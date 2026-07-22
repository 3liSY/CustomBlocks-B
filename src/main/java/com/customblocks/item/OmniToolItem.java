/**
 * OmniToolItem.java
 *
 * The Omni-Tool (Group 06) — one item that carries five modes, cycled in-hand (no Screen):
 *   Glow → Hardness → Face → Copy → Delete → back to Glow.
 *
 *   Right-click a custom block  → run the active mode's action.
 *   Sneak + right-click anywhere → CYCLE to the next mode (a hotbar line + a click sound each step).
 *
 * The active mode is stored per-player in OmniToolState (persists across restarts); the Copy buffer
 * is a session-only clipboard there too. The Face/Delete/Copy modes reuse existing subsystems —
 * Face rides the vanilla model-rotation store (FaceRotations), Delete calls the shared CbBlock.cbDelete
 * (same as the red Deleter), Copy moves the four feel-stats through SlotManager.
 *
 * Depends on: OmniToolState, SlotBlock/SlotData/SlotManager, SlotLighting, FaceRotations, UndoManager,
 *             ResourcePackServer, HudSync, LockManager, Chat
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.FaceRotations;
import com.customblocks.core.LockManager;
import com.customblocks.core.OmniToolState;
import com.customblocks.core.OmniToolState.Mode;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class OmniToolItem extends Item {

    private static final int[] GLOW_STEPS = {0, 4, 8, 12, 15};
    private static final float[] HARD_STEPS = {0f, 0.5f, 1.5f, 5f, 20f, -1f};
    private static final String[] HARD_LABELS = {"instant", "soft", "stone", "hard", "tough", "unbreakable"};

    /**
     * §I3 standing paste-prompt. Shown the moment a feel is grabbed AND kept alive by {@link #tickCopyPrompts}
     * so it never fades while Copy mode holds a clipboard. Grab and ticker send the EXACT same string so the
     * prompt refreshes seamlessly instead of flickering between two wordings.
     */
    private static final String COPY_PROMPT = "Feel copied — click a block to paste";

    /**
     * How often the copy-prompt ticker re-sends {@link #COPY_PROMPT} (server ticks). A hotbar line stays at
     * full opacity for ~40 ticks then fades; re-sending every 30 ticks (1.5s) refreshes the dwell before it
     * fades, and 30 &gt; the {@code HotbarSpeaker} 20-tick swallow window so the identical re-send still lands.
     */
    private static final int COPY_PROMPT_REFRESH_TICKS = 30;
    private static int copyPromptClock = 0;

    public OmniToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        var block = ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock();
        boolean ours = block instanceof SlotBlock;
        if (!ours) {
            return ActionResult.PASS; // not our block — act like an empty hand (joinable letters included)
        }
        // Client returns SUCCESS so the arm swings with no delay; the server does the work.
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        // F1/F4: sneak + right-click CYCLES the mode in-hand (no Screen). This fires when aimed at a
        // custom block; use() covers the air / non-custom-block case, so cycling works anywhere.
        if (player.isSneaking()) {
            cycleMode(player, ctx.getWorld(), ctx.getStack());
            return ActionResult.SUCCESS;
        }
        SlotBlock slot = (SlotBlock) block;
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to edit
        }
        Mode mode = OmniToolState.getMode(player.getUuid());
        // F2 (owner-locked 2026-07-15): a locked block is edited by nothing — gate every mode EXCEPT Copy,
        // which may READ a locked source (§I4); Copy's own paste-onto-locked refusal is handled in doCopy.
        boolean locked = LockManager.isLocked(d.customId());
        if (locked && mode != Mode.COPY) {
            Chat.lockedTool(player, d.customId());
            return ActionResult.SUCCESS;
        }
        switch (mode) {
            case GLOW     -> cycleGlow(player, d);
            case HARDNESS -> cycleHardness(player, d);
            case FACE     -> rotateFace(player, d, ctx.getSide().getName());
            case DELETE   -> slot.cbDelete(player, ctx.getWorld(), ctx.getBlockPos()); // §H — same rail as the red Deleter
            case COPY     -> doCopy(player, d, locked);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * The sneak-click "cycle mode" gesture must also work in the air (and on any non-custom block),
     * not only when aimed at a custom block. useOnBlock already handles the sneak-click on a custom
     * block (returns SUCCESS, so this never double-fires there); this covers air + non-custom blocks.
     * A plain (non-sneak) air-click has nothing to act on, so it passes through.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) {
            return TypedActionResult.pass(stack); // normal air-click: no block to edit
        }
        // Client swings instantly; the server cycles the mode (mirrors useOnBlock).
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack);
        }
        cycleMode(player, world, stack);
        return TypedActionResult.success(stack);
    }

    // ── Mode cycle ────────────────────────────────────────────────────────────

    /** Step to the next mode, live-rename the held tool, show a hotbar line + a small click sound (F1/F2). */
    private static void cycleMode(ServerPlayerEntity player, World world, ItemStack stack) {
        Mode next = OmniToolState.getMode(player.getUuid()).next();
        OmniToolState.setMode(player.getUuid(), next);
        applyName(stack, next); // live item-name update
        Chat.toolSuccess(player, "Omni-Tool", next.label);
        world.playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundCategory.PLAYERS, 0.6f, 1.4f);
    }

    // ── Mode actions ─────────────────────────────────────────────────────────

    private static void cycleGlow(ServerPlayerEntity player, SlotData d) {
        int next = GLOW_STEPS[0];
        for (int s : GLOW_STEPS) { if (s > d.glow()) { next = s; break; } }
        SlotData u = SlotManager.setGlow(d.customId(), next);
        if (u == null) return;
        UndoManager.recordModify(player.getUuid(), d, u, "glow");
        SlotLighting.applyToPlaced(player.getServer(), d.index(), next);
        Chat.toolSuccess(player, "Glow set", String.valueOf(next));
    }

    private static void cycleHardness(ServerPlayerEntity player, SlotData d) {
        int i = hardIndex(d.hardness());
        int n = (i + 1) % HARD_STEPS.length;
        SlotData u = SlotManager.setHardness(d.customId(), HARD_STEPS[n]);
        if (u == null) return;
        UndoManager.recordModify(player.getUuid(), d, u, "hardness");
        Chat.toolSuccess(player, "Hardness set", HARD_LABELS[n]);
    }

    /**
     * §G Face mode: turn the clicked face's image 90° clockwise (the geometry never rotates). The turn
     * lives in {@link FaceRotations} (model-native rotation), is undo/redo-recorded, and the pack is
     * pushed so every placed copy updates live — the same rail as /cb setface.
     */
    private static void rotateFace(ServerPlayerEntity p, SlotData d, String face) {
        int oldQ = FaceRotations.get(d.index(), face);
        int newQ = FaceRotations.rotateCw(d.index(), face);
        UndoManager.recordFaceRotate(p.getUuid(), d.index(), face, oldQ, newQ);
        ResourcePackServer.updatePack(); // model changed → clients re-sync live (like setface)
        // F2 (2026-07-22): a full cube bakes rotation into the pushed pack model, but a §B shaped block
        // reads its rotation only from the synced packed value (ClientSlotCache.rot → resolveFaceRot),
        // which the pack push never refreshes — so on a dedicated server the shaped block re-meshes with
        // the stale rotation. Broadcast the HUD sync (it carries the packed rot) so ClientSlotCache.rot
        // changes and geometryDiffers() triggers the world re-mesh. SP already works (reads in-process).
        HudSync.broadcast(p.getServer());
        Chat.toolSuccess(p, "Face " + face, (newQ * 90) + "°");
    }

    /**
     * §I Copy mode: a repeatable grab→paste loop for the four feel-stats (glow, hardness, sound,
     * walk-through) — NOT look/shape/name/category (§I5). First click with an empty clipboard GRABS
     * (reading a locked source is allowed, §I4); each later click PASTES onto the clicked block in one
     * undo step (§I2), refusing a locked target (§I4). The clipboard stays until the player leaves Copy
     * mode (§I3), so paste repeats.
     */
    private static void doCopy(ServerPlayerEntity p, SlotData d, boolean targetLocked) {
        OmniToolState.Feel held = OmniToolState.getCopy(p.getUuid());
        if (held == null) { // GRAB — a locked source may be read (§I4)
            OmniToolState.setCopy(p.getUuid(), new OmniToolState.Feel(d.glow(), d.hardness(), d.soundType(), d.noCollision()));
            Chat.toolSuccess(p, COPY_PROMPT); // §I3: the ticker keeps this exact line alive until Copy mode ends
            return;
        }
        if (targetLocked) { Chat.lockedTool(p, d.customId()); return; } // paste refused on a locked target (§I4)
        SlotData before = d;
        SlotManager.setGlow(d.customId(), held.glow());
        SlotManager.setHardness(d.customId(), held.hardness());
        SlotManager.setSoundType(d.customId(), held.soundType());
        SlotData after = SlotManager.setNoCollision(d.customId(), held.noCollision()); // last setter → fresh snapshot
        if (after == null) return; // block vanished mid-paste
        UndoManager.recordModify(p.getUuid(), before, after, "copy"); // ONE undo step (§I2)
        SlotLighting.applyToPlaced(p.getServer(), d.index(), held.glow()); // glow shows live on placed copies
        HudSync.broadcast(p.getServer()); // feel-stats HUD updates live, no rejoin
        Chat.toolSuccess(p, "Pasted feel onto", d.customId()); // buffer STAYS (§I3 — repeatable)
    }

    private static int hardIndex(float h) {
        for (int i = 0; i < HARD_STEPS.length; i++) if (Float.compare(HARD_STEPS[i], h) == 0) return i;
        return 2; // "stone" — sensible default for an off-preset value
    }

    // ── §I3 persistent paste-prompt ticker ───────────────────────────────────

    /**
     * §I3 fix: the Copy-mode paste prompt must STAY until the player leaves Copy mode, but a hotbar line
     * fades on its own after ~3s. So while a player holds the Omni-Tool in Copy mode with a live clipboard,
     * re-send the standing {@link #COPY_PROMPT} every {@link #COPY_PROMPT_REFRESH_TICKS} ticks — refreshing
     * the dwell before it fades. Leaving Copy mode clears the clipboard ({@code OmniToolState.setMode}), so
     * the loop stops and the last line fades away naturally. Registered on END_SERVER_TICK by the mod init.
     */
    public static void tickCopyPrompts(net.minecraft.server.MinecraftServer server) {
        if (++copyPromptClock < COPY_PROMPT_REFRESH_TICKS) return;
        copyPromptClock = 0;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (OmniToolState.getMode(p.getUuid()) != Mode.COPY) continue; // only Copy mode
            if (OmniToolState.getCopy(p.getUuid()) == null) continue;      // nothing grabbed yet — no prompt
            if (!isHoldingOmni(p)) continue;                              // tool must be in hand to see it
            Chat.toolSuccess(p, COPY_PROMPT);
        }
    }

    /** True if the player holds the Omni-Tool in either hand (so a hotbar prompt is worth showing). */
    private static boolean isHoldingOmni(ServerPlayerEntity p) {
        return p.getMainHandStack().getItem() instanceof OmniToolItem
                || p.getOffHandStack().getItem() instanceof OmniToolItem;
    }

    // ── Display helpers (mode shown in the item's name) ──────────────────────

    /** Set the stack's name to reflect the given mode, e.g. "Omni-Tool [Glow Mode]". */
    public static void applyName(ItemStack stack, Mode mode) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(CbFmt.CLICK + "Omni-Tool " + mode.color + "[" + mode.label + " Mode]")
                .styled(s -> s.withItalic(false)));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip,
                              net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.literal(CbFmt.DIM + "Five tools in one hand.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.DIM + "Right-click a custom block to apply the current mode.").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.DIM + "Sneak + right-click anywhere to cycle:").styled(s -> s.withItalic(false)));
        tooltip.add(Text.literal(CbFmt.VALUE + "Glow " + CbFmt.FAINT + "· " + CbFmt.DIM + "Hardness "
                + CbFmt.FAINT + "· " + CbFmt.VALUE + "Face " + CbFmt.FAINT + "· " + CbFmt.VALUE + "Copy "
                + CbFmt.FAINT + "· " + CbFmt.BAD + "Delete").styled(s -> s.withItalic(false)));
    }
}
