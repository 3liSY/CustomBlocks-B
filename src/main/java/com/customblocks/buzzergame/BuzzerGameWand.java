/**
 * BuzzerGameWand.java — Group 31 (BuzzerGame) item 1 (central wand).
 *
 * The one admin tool for setting up a BuzzerGame, replacing the old separate Link Wand + Resize Tool
 * (design lock 2026-07-10). It carries a per-player MODE that you cycle through, and each mode changes
 * what right-clicking a block does — all server-side (no client input hooks), so it works for any client:
 *
 *   - Sneak + right-click air        → cycle the mode (Link → Resize → Rotate → Digit-style), on the action bar.
 *   - Right-click air (no sneak)      → flip the current mode's direction (Resize BIGGER ↔ SMALLER,
 *                                        Rotate CLOCKWISE ↔ ANTI-CLOCKWISE; Link/Digit-style have none).
 *   - Right-click a block             → apply the current mode:
 *        LINK   : click an admin panel to select it, then click buzzers / timer stands to link them.
 *        RESIZE : click a timer stand to nudge its size one notch in the current direction.
 *        ROTATE : click a timer stand to turn it 5° in the current direction.
 *        DIGITS : click a timer stand to flip its digits between LED 7-segment and plain text.
 *
 * All state is transient per-player (setup time only), like the tools it replaces.
 *
 * Depends on: AdminPanelBlockEntity, BuzzerBlockEntity, TimerDisplayBlockEntity, PanelSession, SessionState,
 *             TimerDisplayVisual
 * Called by:  AdminPanelBlock.onUse (panel), BuzzerBlock.onUse (buzzer), TimerDisplayBlock.onUse (stand),
 *             BuzzerGameRegistry.register()
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;
import com.customblocks.command.CbFmt;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    /** The jobs the wand can be in. */
    public enum Mode {
        LINK("Link"),
        RESIZE("Resize"),
        ROTATE("Rotate"),
        DIGITS("Digit style");

        final String label;
        Mode(String label) { this.label = label; }
    }

    /** The order sneak+right-click air cycles through. */
    private static final Mode[] CYCLE = { Mode.LINK, Mode.RESIZE, Mode.ROTATE, Mode.DIGITS };

    /** Degrees the stand turns per Rotate-mode click. */
    private static final float ROTATE_STEP = 5f;

    /** Per-player wand state (transient — setup-time only, like the old tools). */
    private static final Map<UUID, Mode> MODE = new HashMap<>();
    private static final Map<UUID, Boolean> RESIZE_GROW = new HashMap<>();   // true = bigger
    private static final Map<UUID, Boolean> ROTATE_CW = new HashMap<>();     // true = clockwise (+5°)
    private static final Map<UUID, BlockPos> SELECTED_PANEL = new HashMap<>();

    public BuzzerGameWand(Settings settings) {
        super(settings);
    }

    /** Is this stack the BuzzerGame wand? (Blocks check the held item to route clicks here.) */
    public static boolean isWand(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof BuzzerGameWand;
    }

    private static Mode modeOf(UUID id) {
        return MODE.getOrDefault(id, Mode.LINK);
    }

    private static boolean growing(UUID id) {
        return RESIZE_GROW.getOrDefault(id, true);
    }

    private static boolean clockwise(UUID id) {
        return ROTATE_CW.getOrDefault(id, true);
    }

    // ------------------------------------------------------------------ air right-clicks

    /**
     * Right-click air: sneaking cycles the mode; otherwise flips the current mode's direction. Right-clicks
     * on a block are handled by that block's onUse (see the onXxxClicked methods) and never reach here.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (player.isSneaking()) {
                cycleMode(player);
            } else {
                flipDirection(player);
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
        Chat.tool(player, "Wand mode: " + mode.label + "  —  " + hintFor(mode));
    }

    /** Non-sneak right-click air: flip the direction of whatever mode we're in (Resize / Rotate have one). */
    private static void flipDirection(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        switch (modeOf(id)) {
            case RESIZE -> {
                boolean grow = !growing(id);
                RESIZE_GROW.put(id, grow);
                Chat.tool(player, "Resize direction: " + (grow ? "BIGGER" : "SMALLER")
                        + "  —  right-click a timer stand to apply.");
            }
            case ROTATE -> {
                boolean cw = !clockwise(id);
                ROTATE_CW.put(id, cw);
                Chat.tool(player, "Turn direction: " + (cw ? "CLOCKWISE" : "ANTI-CLOCKWISE")
                        + "  —  right-click a timer stand to turn 5°.");
            }
            default -> Chat.tool(player, hintFor(modeOf(id)));
        }
    }

    private static String hintFor(Mode mode) {
        return switch (mode) {
            case LINK -> "right-click a panel, then buzzers/stands to link them.";
            case RESIZE -> "right-click a stand to resize; right-click air to flip bigger/smaller.";
            case ROTATE -> "right-click a stand to turn it 5°; right-click air to flip the turn direction.";
            case DIGITS -> "right-click a stand to flip its digits between LED and plain text.";
        };
    }

    // ------------------------------------------------------------------ block apply (called from onUse)

    /** Wand-holder right-clicked an admin panel. LINK: select it. Other modes: nudge them to Link mode. */
    public static void onPanelClicked(ServerPlayerEntity player, BlockPos panelPos) {
        if (!requireOp(player)) return;
        if (modeOf(player.getUuid()) != Mode.LINK) {
            wrongMode(player, "select a panel");
            return;
        }
        SELECTED_PANEL.put(player.getUuid(), panelPos.toImmutable());
        Chat.toPlayer(player, CbFmt.OK + "Panel selected. Now right-click buzzers / timer stands to link them to it.");
    }

    /** Wand-holder right-clicked a buzzer. LINK: link it to the selected panel. */
    public static void onBuzzerClicked(ServerPlayerEntity player, World world, @Nullable BuzzerBlockEntity buzzer) {
        if (buzzer == null || !requireOp(player)) return;
        if (modeOf(player.getUuid()) != Mode.LINK) {
            wrongMode(player, "link a buzzer");
            return;
        }
        AdminPanelBlockEntity panel = selectedPanel(player, world);
        if (panel == null) return;
        PanelSession session = panel.session();
        if (session.state() != SessionState.IDLE) {
            Chat.toPlayer(player, CbFmt.BAD + "Reset the round to Idle before changing its links.");
            return;
        }
        session.linkBuzzer(buzzer.getBuzzerId());
        buzzer.link(panel.getPos(), session.sessionId());
        panel.markDirty();
        Chat.toPlayer(player, CbFmt.OK + "Linked buzzer to panel (" + session.buzzerCount() + " linked).");
    }

    /** Wand-holder right-clicked a timer stand — dispatch by the current mode (LINK links it, RESIZE resizes). */
    public static void onStandClicked(ServerPlayerEntity player, World world, @Nullable TimerDisplayBlockEntity display) {
        if (display == null || !requireOp(player)) return;
        switch (modeOf(player.getUuid())) {
            case LINK -> linkStand(player, world, display);
            case RESIZE -> resizeStand(player, display);
            case ROTATE -> rotateStand(player, display);
            case DIGITS -> toggleDigits(player, display);
        }
    }

    private static void toggleDigits(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        display.toggleDigitStyle();
        boolean led = display.isLed();
        Chat.tool(player, "Timer digits:", led ? "LED 7-segment" : "Plain text");
    }

    private static void linkStand(ServerPlayerEntity player, World world, TimerDisplayBlockEntity display) {
        AdminPanelBlockEntity panel = selectedPanel(player, world);
        if (panel == null) return;
        PanelSession session = panel.session();
        if (session.state() != SessionState.IDLE) {
            Chat.toPlayer(player, CbFmt.BAD + "Reset the round to Idle before changing its links.");
            return;
        }
        session.linkScreen(display.getDisplayId());
        display.link(panel.getPos(), session.sessionId());
        panel.markDirty();
        Chat.toPlayer(player, CbFmt.OK + "Linked timer stand to panel (" + session.screenCount() + " stand(s)).");
    }

    private static void resizeStand(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        boolean grow = growing(player.getUuid());
        float before = display.getScale();
        float target = TimerDisplayVisual.clampScale(before + (grow ? TimerDisplayVisual.SCALE_STEP : -TimerDisplayVisual.SCALE_STEP));
        display.setScale(target);
        if (target == before) {
            Chat.tool(player, grow ? "Already at the max size." : "Already at the min size.");
        } else {
            Chat.tool(player, String.format(Locale.ROOT, "Timer stand size: %.2f×   (right-click air to switch to %s)",
                    target, grow ? "SMALLER" : "BIGGER"));
        }
    }

    private static void rotateStand(ServerPlayerEntity player, TimerDisplayBlockEntity display) {
        boolean cw = clockwise(player.getUuid());
        display.rotate(cw ? ROTATE_STEP : -ROTATE_STEP);
        Chat.tool(player, String.format(Locale.ROOT,
                "Timer stand angle: %.0f°   (right-click air to switch to %s)",
                display.getYaw(), cw ? "ANTI-CLOCKWISE" : "CLOCKWISE"));
    }

    // ------------------------------------------------------------------ helpers

    /** The panel the player selected with the wand, if it's still there; else nag them to pick one. */
    private static @Nullable AdminPanelBlockEntity selectedPanel(ServerPlayerEntity player, World world) {
        BlockPos pos = SELECTED_PANEL.get(player.getUuid());
        if (pos == null) {
            Chat.toPlayer(player, CbFmt.BAD + "Select an admin panel first — right-click one with the wand (Link mode).");
            return null;
        }
        if (!(world.getBlockEntity(pos) instanceof AdminPanelBlockEntity panel)) {
            SELECTED_PANEL.remove(player.getUuid());
            Chat.toPlayer(player, CbFmt.BAD + "That panel is gone — select an admin panel again.");
            return null;
        }
        return panel;
    }

    private static boolean requireOp(ServerPlayerEntity player) {
        if (player.hasPermissionLevel(2)) return true;
        Chat.toolError(player, "Only operators can use the BuzzerGame wand.");
        return false;
    }

    private static void wrongMode(ServerPlayerEntity player, String action) {
        Chat.toolError(player, "Switch to Link mode to " + action + " (sneak + right-click air to change mode).");
    }
}
