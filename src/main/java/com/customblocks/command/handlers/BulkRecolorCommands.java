/**
 * BulkRecolorCommands.java — Group 07 §G27.22b (Bulk Operations Hub — Recolor op, server side).
 *
 * The net-new 10th bulk op: batch hue-shift every matched block's texture in place. Follows the project idiom
 * exactly (fast checks + target gather on the server thread → the pixel work on a daemon worker → back to the
 * server thread for ONE pack rebuild + sync + ONE batch undo + chat), mirroring {@link BulkCommands#applyDelete}
 * and the {@code /cb gradient} batch. Locked blocks are skipped (recolor is a {@code skipsLocked} op); textureless
 * blocks are skipped and reported. The transform is a pure hue rotation via {@link ColorMath#hslShift} with
 * saturation/lightness left at 1.0 — the conservative, reversible batch-safe choice (never a destructive
 * sat/light change per the KNOWN_PITFALLS "batch recolor" rule). The whole batch reverts in one {@code /cb undo}.
 *
 * Depends on: SlotManager, SlotData, LockManager, TextureStore, ColorMath, ResourcePackServer, UndoManager,
 *             BulkConfirm, BulkResult, BulkChat, FeedbackFx, IncidentRecorder, Chat.
 * Called by:  BulkApply (the Bulk Operations Hub's Execute).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.FeedbackFx;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.image.ColorMath;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BulkRecolorCommands {

    private BulkRecolorCommands() {} // static-only

    /** Hue-shift every (non-locked, textured) matched block's texture by {@code hueDeg}, as one undo batch. */
    static void applyRecolor(ServerCommandSource src, List<SlotData> blocks, double hueDeg) {
        MinecraftServer server = src.getServer();
        if (server == null) return;

        // Gather the eligible targets + their current textures first (cheap, on the server thread).
        List<SlotData> targets = new ArrayList<>();
        List<byte[]> befores = new ArrayList<>();
        int locked = 0, notex = 0;
        for (SlotData t : blocks) {
            String id = t.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData cur = SlotManager.getById(id);
            if (cur == null) continue;
            byte[] before = TextureStore.load(cur.index());
            if (before == null || before.length == 0) { notex++; continue; }
            targets.add(cur);
            befores.add(before);
        }
        final int lockedN = locked, notexN = notex;
        if (targets.isEmpty()) {
            String why = lockedN > 0 ? " — all " + lockedN + " locked"
                    : (notexN > 0 ? " — none of them have a texture yet" : "");
            BulkResult.record(src, "No blocks recoloured" + why + "."); // X3
            Chat.error(src, "No blocks recoloured" + why + ".");
            return;
        }

        final UUID who = BulkConfirm.actor(src);
        Chat.info(src, "Recolouring " + CbFmt.VALUE + targets.size() + CbFmt.RESET + " block(s) by " + CbFmt.VALUE + (int) Math.round(hueDeg) + "°" + CbFmt.RESET + "…");
        Thread worker = new Thread(() -> {
            try {
                List<byte[]> afters = new ArrayList<>(targets.size());
                for (byte[] before : befores) afters.add(ColorMath.hslShift(before, hueDeg, 1.0, 1.0));
                server.execute(() -> commit(src, server, who, targets, befores, afters, hueDeg, lockedN, notexN));
            } catch (Exception e) {
                IncidentRecorder.record("Bulk recolour failed (" + hueDeg + "°)", "", src.getName(), e);
                server.execute(() -> Chat.error(src, "Couldn't recolour those textures — left unchanged."));
            }
        }, "CustomBlocks-BulkRecolor");
        worker.setDaemon(true);
        worker.start();
    }

    /** Server-thread commit: save every new texture, ONE pack rebuild + sync, ONE batch undo, one chat line. */
    private static void commit(ServerCommandSource src, MinecraftServer server, UUID who,
                               List<SlotData> targets, List<byte[]> befores, List<byte[]> afters,
                               double hueDeg, int lockedN, int notexN) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            SlotData slot = targets.get(i);
            byte[] before = befores.get(i), after = afters.get(i);
            TextureStore.save(slot.index(), after);
            children.add(new UndoManager.Op(UndoManager.Kind.TEXTURE, slot, slot, before, after, "bulk-recolor", null));
            changed.add(slot.customId());
        }
        ResourcePackServer.updatePack();   // ONE debounced rebuild for the whole batch
        ResourcePackServer.syncToAll();
        UndoManager.recordBatch(who, children, "bulk-recolor (" + changed.size() + ")");

        String skipNote = (lockedN > 0 ? " · " + lockedN + " locked skipped" : "")
                + (notexN > 0 ? " · " + notexN + " had no texture" : "");
        BulkResult.record(src, "Recoloured " + changed.size() + " block(s) by "
                + (int) Math.round(hueDeg) + "°" + skipNote + "."); // X3

        String hoverList = CbFmt.DIM + "Recoloured " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + (lockedN > 0 ? "\n\n" + CbFmt.BAD + lockedN + " locked — skipped" : "")
                + (notexN > 0 ? "\n" + CbFmt.FAINT + notexN + " had no texture" : "");
        MutableText msg = Text.literal(CbFmt.OK + "Recoloured ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal(" " + CbFmt.DIM + "by " + CbFmt.VALUE + (int) Math.round(hueDeg) + "°  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (lockedN > 0) msg.append(Text.literal("  " + CbFmt.FAINT + lockedN + " locked"));
        Chat.line(src, msg);
        FeedbackFx.fire(src, "bulk_complete");
    }
}
