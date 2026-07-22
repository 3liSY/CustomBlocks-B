/**
 * BuzzerGameWand.java — Group 31 (BuzzerGame) items 1 + E + F (wand-owned session + per-part resize).
 *
 * The one tool that both OWNS a host's BuzzerGame session and sets it up. The session lives on the wand's
 * holder (via {@link BuzzerSessionManager}), not on any placed block. It carries a per-player MODE cycled
 * server-side (no client hooks), so it works for any client:
 *
 *   - Sneak + right-click air              → cycle the mode (Link → Resize → Rotate), shown on the hotbar.
 *   - Right-click air (Link)               → start/own your session and report what's linked.
 *   - Right-click air (Resize)             → report the selected part + its size.
 *   - Right-click air (Rotate)             → flip the turn direction (CW ↔ anti-CW).
 *   - Right-click a buzzer (Link)          → link it into your session.
 *   - Right-click a stand PART (Link)      → link the stand into your session.
 *   - Right-click a stand PART (Resize)    → grow the selected part one ×0.1 step.
 *   - Sneak + RC a stand PART (Resize)     → SELECT that part (base / leg / screen).
 *   - Sneak + left-click a stand PART (Resize) → shrink the selected part one step (cancels the break).
 *   - Right-click a stand PART (Rotate)    → turn the whole stand 45° (yaw).
 *   - Left-click a stand PART (any mode)   → break the whole stand instantly.
 *
 * The stand's parts are server-side INTERACTION entities that track the floating/tilted/scaled visual (I4/I5);
 * the wand routes clicks on them through {@link #onPartInteract} (wired to Fabric's Use/AttackEntityCallback
 * in {@link com.customblocks.CustomBlocksMod}). Because the click IS on a part entity, the selected part is
 * exact — no crosshair-band guessing. No admin gate; wand state is transient per-player and the session dies
 * on host logout.
 *
 * Depends on: BuzzerSession(Manager), SessionState, TimerPart, BuzzerBlockEntity, TimerDisplayBlock(Entity),
 *             TimerDisplayVisual
 * Called by:  BuzzerBlock.onUse (right-click a buzzer), CustomBlocksMod (Use/AttackEntityCallback → part hits),
 *             BuzzerGameRegistry.register()
 */
package com.customblocks.buzzergame;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BuzzerGameWand extends Item {

    /** The jobs the wand can be in. {@code bar} is the short, glanceable action-bar label (I7/F7): a BMP glyph
     *  (unifont-covered so it never tofu-boxes like an astral emoji would) + one word. */
    public enum Mode {
        LINK("Link", "⛓ Link"),        // ⛓
        RESIZE("Resize", "⤢ Resize"),  // ⤢
        ROTATE("Rotate", "⟳ Rotate");  // ⟳

        final String label;
        final String bar;
        Mode(String label, String bar) { this.label = label; this.bar = bar; }
    }

    /** The order sneak+right-click air cycles through (LED-only now — the Digit-style mode was dropped). */
    private static final Mode[] CYCLE = { Mode.LINK, Mode.RESIZE, Mode.ROTATE };

    /** Degrees the stand turns per Rotate-mode click — 45° steps → 8 facings (design lock 2026-07-19). */
    private static final float ROTATE_STEP = 45f;

    /** Per-player wand state (transient — setup-time only). */
    private static final Map<UUID, Mode> MODE = new HashMap<>();
    private static final Map<UUID, Boolean> ROTATE_CW = new HashMap<>();       // true = clockwise (+45°)
    private static final Map<UUID, TimerPart> SELECTED = new HashMap<>();      // Resize-mode selected part

    public BuzzerGameWand(Settings settings) {
        super(settings);
    }

    /** Is this stack the BuzzerGame wand? (Blocks check the held item to route clicks here.) */
    public static boolean isWand(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof BuzzerGameWand;
    }

    private static Mode modeOf(UUID id) { return MODE.getOrDefault(id, Mode.LINK); }
    private static boolean clockwise(UUID id) { return ROTATE_CW.getOrDefault(id, true); }
    private static TimerPart selectedOf(UUID id) { return SELECTED.getOrDefault(id, TimerPart.WHOLE); }

    // ------------------------------------------------------------------ air right-clicks

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (player.isSneaking()) {
                cycleMode(player);
            } else {
                primaryAir(player);
            }
        }
        return TypedActionResult.success(stack);
    }

    private static void cycleMode(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Mode cur = modeOf(id);
        int next = 0;
        for (int i = 0; i < CYCLE.length; i++) {
            if (CYCLE[i] == cur) { next = (i + 1) % CYCLE.length; break; }
        }
        Mode mode = CYCLE[next];
        MODE.put(id, mode);
        Chat.tool(player, mode.bar);                                        // action bar: short, glanceable (F7)
        Chat.toPlayer(player, CbFmt.DIM + "Wand → " + CbFmt.BODY + mode.label // one chat line: the how-to
                + CbFmt.DIM + ": " + hintFor(mode));
    }

    /** Non-sneak right-click air: Link owns the session; Resize reports selection; Rotate flips direction. */
    private static void primaryAir(ServerPlayerEntity player) {
        switch (modeOf(player.getUuid())) {
            case LINK -> {
                BuzzerSession s = BuzzerSessionManager.getOrCreate(player);
                Chat.toolSuccess(player, "Session ready — link a buzzer + a timer stand ("
                        + s.buzzerCount() + " buzzer(s), " + s.displayCount() + " stand(s)).");
            }
            case RESIZE -> {
                TimerPart part = selectedOf(player.getUuid());
                Chat.tool(player, "Resize: selected " + part.label()
                        + " — shift-RC a stand part to pick one, RC to grow, sneak+LC to shrink.");
            }
            case ROTATE -> {
                boolean cw = !clockwise(player.getUuid());
                ROTATE_CW.put(player.getUuid(), cw);
                Chat.tool(player, "Turn direction: " + (cw ? "CLOCKWISE" : "ANTI-CLOCKWISE")
                        + " — right-click a timer stand to turn 45°.");
            }
        }
    }

    private static String hintFor(Mode mode) {
        return switch (mode) {
            case LINK -> "right-click buzzers/stands to link them to your session.";
            case RESIZE -> "shift-RC a stand part to select, RC to grow, sneak+LC to shrink.";
            case ROTATE -> "right-click a stand to turn it 45°; right-click air to flip the direction.";
        };
    }

    // ------------------------------------------------------------------ buzzer right-click (from block.onUse)

    /** Wand-holder right-clicked a buzzer. LINK: link it into the holder's session. */
    public static void onBuzzerClicked(ServerPlayerEntity player, @Nullable BuzzerBlockEntity buzzer) {
        if (buzzer == null) return;
        if (modeOf(player.getUuid()) != Mode.LINK) {
            wrongMode(player, "link a buzzer");
            return;
        }
        BuzzerSession session = BuzzerSessionManager.getOrCreate(player);
        if (!canEditLinks(session)) {
            Chat.toolError(player, "Finish or reset the run before changing links.");
            return;
        }
        detachFromOther(buzzer.getSessionId(), session, buzzer.getBuzzerId(), true);
        session.linkBuzzer(buzzer.getBuzzerId(), buzzer.getPos());
        buzzer.link(session.sessionId());
        Chat.toolSuccess(player, "Linked buzzer to your session (" + session.buzzerCount() + " linked).");
    }

    // ------------------------------------------------------------------ stand PART interaction (I4/I5)

    /**
     * Routed from Fabric's Use/AttackEntityCallback (registered in {@link com.customblocks.CustomBlocksMod})
     * when a player clicks a stand-part INTERACTION entity. {@code attack} = left-click. Returns SUCCESS when
     * the entity is one of ours (consumes the click), else PASS.
     */
    public static ActionResult onPartInteract(PlayerEntity player, World world, Entity entity, boolean attack) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (!TimerHitbox.isHitEntity(entity)) return ActionResult.PASS;
        BlockPos pos = TimerHitbox.hitPos(entity);
        TimerPart part = TimerHitbox.hitPart(entity);
        if (pos == null || part == null) return ActionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof TimerDisplayBlockEntity display)) return ActionResult.PASS;
        if (attack) doPartAttack(sp, world, display);
        else doPartUse(sp, display, part);
        return ActionResult.SUCCESS;
    }

    /** Right-click a stand part: dispatch by mode. In Resize, sneak selects THIS part (exact); RC grows it. */
    private static void doPartUse(ServerPlayerEntity player, TimerDisplayBlockEntity display, TimerPart part) {
        if (!isWand(player.getMainHandStack())) {
            Chat.tool(player, display.isLinked()
                    ? "Timer stand — linked. Hold the BuzzerGame wand to resize / rotate / relink it."
                    : "Timer stand — not linked. Use the BuzzerGame wand (Link mode) to link it.");
            return;
        }
        switch (modeOf(player.getUuid())) {
            case LINK -> linkStand(player, display);
            case RESIZE -> {
                if (player.isSneaking()) selectPartDirect(player, display, part);
                else growSelectedPart(player, display);
            }
            case ROTATE -> rotateStand(player, display);
        }
    }

    /** Left-click a stand part: wand + Resize + sneak shrinks the selected part; anything else breaks the whole
     *  stand instantly (I5 — no slow mining of an invisible voxel). */
    private static void doPartAttack(ServerPlayerEntity player, World world, TimerDisplayBlockEntity display) {
        if (isWand(player.getMainHandStack()) && modeOf(player.getUuid()) == Mode.RESIZE && player.isSneaking()) {
            shrinkSelectedPart(player, display);
            return;
        }
        world.breakBlock(display.getPos(), !player.isCreative(), player);
    }

    private static void linkStand(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        BuzzerSession session = BuzzerSessionManager.getOrCreate(player);
        if (!canEditLinks(session)) {
            Chat.toolError(player, "Finish or reset the run before changing links.");
            return;
        }
        detachFromOther(display.getSessionId(), session, display.getDisplayId(), false);
        session.linkDisplay(display.getDisplayId(), display.getPos());
        display.link(session.sessionId());
        Chat.toolSuccess(player, "Linked timer stand to your session (" + session.displayCount() + " stand(s)).");
    }

    private static void rotateStand(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        boolean cw = clockwise(player.getUuid());
        display.rotate(cw ? ROTATE_STEP : -ROTATE_STEP);
        Chat.tool(player, String.format(Locale.ROOT,
                "Timer stand angle: %.0f° (right-click air to switch to %s)",
                display.getYaw(), cw ? "ANTI-CLOCKWISE" : "CLOCKWISE"));
    }

    // ------------------------------------------------------------------ per-part resize (item F)

    private static void selectPartDirect(ServerPlayerEntity player, TimerDisplayBlockEntity display, TimerPart part) {
        SELECTED.put(player.getUuid(), part);
        Chat.toolSuccess(player, String.format(Locale.ROOT,
                "Selected %s (×%.2f) — RC to grow, sneak+LC to shrink.", part.label(), display.partScale(part)));
    }

    private static void growSelectedPart(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        stepAndReport(player, display, true);
    }

    private static void shrinkSelectedPart(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        stepAndReport(player, display, false);
    }

    private static void stepAndReport(ServerPlayerEntity player, TimerDisplayBlockEntity display, boolean grow) {
        TimerPart part = selectedOf(player.getUuid());
        float before = display.partScale(part);
        float after = display.stepPart(part, grow);
        if (after == before) {
            Chat.tool(player, "The " + part.label() + " is already at the " + (grow ? "max" : "min") + " size.");
        } else {
            Chat.toolSuccess(player, String.format(Locale.ROOT,
                    "%s %s → ×%.2f", grow ? "Grew" : "Shrank", part.label(), after));
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Links may only be changed while the round isn't live (IDLE/ARMED — not mid-run or frozen). */
    private static boolean canEditLinks(BuzzerSession session) {
        SessionState s = session.state();
        return s != SessionState.RUNNING && s != SessionState.RESULTS;
    }

    /** If this object was already linked to a DIFFERENT live session, drop it from that one first. */
    private static void detachFromOther(@Nullable UUID oldSessionId, BuzzerSession target, UUID objectId, boolean buzzer) {
        BuzzerSession old = BuzzerSessionManager.bySession(oldSessionId);
        if (old != null && old != target) {
            if (buzzer) old.unlinkBuzzer(objectId); else old.unlinkDisplay(objectId);
        }
    }

    private static void wrongMode(ServerPlayerEntity player, String action) {
        Chat.toolError(player, "Switch to Link mode to " + action + " (sneak + right-click air to change mode).");
    }
}
