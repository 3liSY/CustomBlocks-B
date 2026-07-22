/**
 * HistoryCommands.java
 *
 * Responsibility: The history subcommands — `/cb undo` and `/cb redo`, plus the Group 17
 * multi-step forms: `undo <N>` / `undo all` / `undo clear`, and `redo <N>` / `redo all`.
 * Each is per-player (FR-06-2): it reads that player's own stack from UndoManager and applies
 * the reverse (undo) or forward (redo) change through SlotManager's non-recording restore
 * primitives. `undo clear` is held behind the shared BulkConfirm flow (run /cb confirm to apply).
 * Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * UndoManager stores only immutable SlotData snapshots; all state mutation and the
 * resource-pack / lighting refresh live here, so UndoManager stays free of server types.
 *
 * NO-REJOIN (see HudSync's NO-REJOIN PRINCIPLE banner; owner 2026-06-27): undo/redo rebuild the
 * pack (texture live) + re-place blocks via NOTIFY_ALL (placements live) AND now broadcast the slot
 * cache once after applying steps (undoCore / redoCore / undoOnce / redoOnce) so a restored block's
 * NAME / look-HUD refreshes live for every player — no rejoin (was TG §D-bug, fixed 2026-06-27).
 */
package com.customblocks.command.handlers;

import com.customblocks.core.SoundFx;

import com.customblocks.command.CbFmt;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.FaceRotations;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.core.WidgetSync;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HistoryCommands {

    private HistoryCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("undo")
                .executes(ctx -> undoCore(ctx, 1))
                .then(CommandManager.literal("all").executes(ctx -> undoCore(ctx, Integer.MAX_VALUE)))
                .then(CommandManager.literal("clear").executes(HistoryCommands::undoClear))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .suggests(HistoryCommands::countSuggestions)
                        .executes(ctx -> undoCore(ctx, IntegerArgumentType.getInteger(ctx, "count")))));

        root.then(CommandManager.literal("redo")
                .executes(ctx -> redoCore(ctx, 1))
                .then(CommandManager.literal("all").executes(ctx -> redoCore(ctx, Integer.MAX_VALUE)))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .suggests(HistoryCommands::countSuggestions)
                        .executes(ctx -> redoCore(ctx, IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            countSuggestions(CommandContext<ServerCommandSource> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        b.suggest(5);
        b.suggest(10);
        b.suggest(25);
        return b.buildFuture();
    }

    /** Undo up to {@code requested} steps in one command (1, N, or all). */
    private static int undoCore(CommandContext<ServerCommandSource> ctx, int requested) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb undo as a player");
            return 0;
        }
        UUID who = player.getUuid();
        if (UndoManager.undoSize(who) == 0) {
            Chat.info(src, "Nothing to undo");
            return 0;
        }
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < requested; i++) {
            UndoManager.Op op = UndoManager.undo(who);
            if (op == null) break;
            applyInverse(src, op);
            steps.add(describe(op, true));
        }
        // NO-REJOIN (TG §D-bug fix): refresh every client's slot cache once, after all steps, so a
        // restored block's name / look-HUD updates live instead of needing a rejoin (covers undo of
        // delete, rename, reid, recreate — any op that changes a HUD field). One broadcast per command.
        HudSync.broadcast(src.getServer());
        SoundFx.chip(player, SoundFx.Chip.UNDO);   // G04-4 cue — same whether typed or chip-clicked
        // G04 A4: an undo's own line carries the way back. A delete's line offers ↩ Undo; the undo's
        // line then offers ↪ Redo, so the round trip is always one click away and never a typed command.
        report(src, "Undid", steps, UndoManager.undoSize(who), "undo", Chat.redoButton());
        return 1;
    }

    /** Redo up to {@code requested} steps in one command (1, N, or all). */
    private static int redoCore(CommandContext<ServerCommandSource> ctx, int requested) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb redo as a player");
            return 0;
        }
        UUID who = player.getUuid();
        if (UndoManager.redoSize(who) == 0) {
            Chat.info(src, "Nothing to redo");
            return 0;
        }
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < requested; i++) {
            UndoManager.Op op = UndoManager.redo(who);
            if (op == null) break;
            applyForward(src, op);
            steps.add(describe(op, false));
        }
        // NO-REJOIN (TG §D-bug fix): same as undoCore — one broadcast after the redo steps.
        HudSync.broadcast(src.getServer());
        SoundFx.chip(player, SoundFx.Chip.REDO);   // G04-4 cue
        // G04-UNDO-DIALECT: the redo line carries ↩ Undo, the mirror of the undo line's ↪ Redo. It used to
        // pass null here, so a redo dead-ended and the only way back was typing the command.
        report(src, "Redid", steps, UndoManager.redoSize(who), "redo", Chat.undoButton());
        return 1;
    }

    /** `/cb undo clear` — confirm (BulkConfirm hold), then wipe undo + redo and report the count. */
    private static int undoClear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb undo clear as a player");
            return 0;
        }
        UUID who = player.getUuid();
        int n = UndoManager.undoSize(who);
        if (n == 0) {
            Chat.info(src, "Nothing to clear — your undo history is already empty.");
            return 0;
        }
        BulkConfirm.request(src, () -> {
            int cleared = UndoManager.clearHistory(who);
            Chat.success(src, "Cleared " + cleared + " undo step" + (cleared == 1 ? "" : "s") + ".");
        }, "clear undo history");
        MutableText msg = Text.literal(CbFmt.VALUE + "Clear all " + CbFmt.BODY + n + CbFmt.VALUE + " undo step"
                        + (n == 1 ? "" : "s") + "?" + CbFmt.RESET + " This can't be undone. ")
                .append(confirmButton())
                .append(Text.literal(" " + CbFmt.DIM + "or " + CbFmt.BODY + "/cb cancel"));
        Chat.line(src, msg);
        return 1;
    }

    /** Undo exactly one step for the player; returns false if there was nothing to undo. */
    public static boolean undoOnce(ServerPlayerEntity player) {
        UndoManager.Op op = UndoManager.undo(player.getUuid());
        if (op == null) return false;
        applyInverse(player.getCommandSource(), op);
        HudSync.broadcast(player.getServer()); // NO-REJOIN: refresh all clients' HUD live (TG §D-bug fix)
        return true;
    }

    /** Redo exactly one step for the player; returns false if there was nothing to redo. */
    public static boolean redoOnce(ServerPlayerEntity player) {
        UndoManager.Op op = UndoManager.redo(player.getUuid());
        if (op == null) return false;
        applyForward(player.getCommandSource(), op);
        HudSync.broadcast(player.getServer()); // NO-REJOIN: refresh all clients' HUD live (TG §D-bug fix)
        return true;
    }

    /**
     * Print a one-line result for a single step, or a bulleted list with values for many.
     *
     * {@code chip} rides the closing line (the success line when there is one step, the remainder line
     * when there are many) so a multi-step undo is as clickable as a single one. Pass null for no chip.
     *
     * G04-COUNT: the remainder used to read "(0 left)" / "(1 left)" — it never named WHICH stack, and a
     * bare "(0 left)" read like a failure code on an operation that had just succeeded. It now names the
     * stack ("· 1 more to undo"), and at zero it says nothing at all: an empty stack is not news.
     */
    private static void report(ServerCommandSource src, String verb, List<String> steps, int left,
                               String stack, MutableText chip) {
        if (steps.isEmpty()) { // shouldn't happen (guarded above), but stay safe
            Chat.info(src, verb.equals("Undid") ? "Nothing to undo" : "Nothing to redo");
            return;
        }
        String remainder = remainder(left, stack);
        if (steps.size() == 1) {
            Chat.successWith(src, verb + " " + steps.get(0) + remainder, chip);
            return;
        }
        Chat.raw(src, CbFmt.OK + verb + " " + steps.size() + " actions:");
        for (String s : steps) {
            Chat.raw(src, Text.literal(CbFmt.DIM + "  • " + CbFmt.BODY + s));
        }
        MutableText tail = Text.literal(CbFmt.DIM + "  " + (remainder.isEmpty() ? "" : remainder.trim()));
        if (chip != null) tail.append(Text.literal("  ")).append(chip);
        Chat.raw(src, tail);
    }

    /** " · N more to undo" — or "" when the stack is empty (nothing left is not worth a line). */
    private static String remainder(int left, String stack) {
        if (left <= 0) return "";
        return " " + CbFmt.DIM + "· " + left + " more to " + stack;
    }

    /** Reverse an op (undo direction). */
    private static void applyInverse(ServerCommandSource src, UndoManager.Op op) {
        switch (op.kind()) {
            case CREATE -> { // block was created → remove it again, but KEEP its texture on disk so a
                             // later /cb redo can restore the pixels (CREATE ops carry no texture bytes).
                SlotManager.removeSilentlyKeepTexture(op.after().customId());
                ResourcePackServer.updatePack();
            }
            case DELETE -> { // block was deleted → bring it back (data + texture + greyed placements)
                SlotManager.restoreSnapshot(op.before());
                if (op.texture() != null) TextureStore.save(op.before().index(), op.texture());
                // Turn the placed copies the sweeper marked back into the real block (G06-2 undo completion).
                com.customblocks.block.RemovedPlacements.restore(src.getServer(), op.before().index());
                com.customblocks.core.MarkerResolver.forget(op.before().index()); // no longer deleted
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.before());
            case SHAPE -> { // G08 §B: shape is data-only and no pack byte depends on it — restoring it
                // must NOT push a pack, or undo would prompt a reload the change itself never did.
                restoreMeta(src, op.before());
                HudSync.broadcast(src.getServer()); // carries the restored shape → client re-mesh
            }
            case TEXTURE -> { // pixels were replaced → put the old texture back + rebuild the pack
                TextureStore.save(op.before().index(), op.texture());
                ResourcePackServer.updatePack();
            }
            case RETEXTURE -> { // /cb retexture → restore the old slot (incl. AnimData) + old pixels, rebuild
                SlotManager.restoreSnapshot(op.before());
                if (op.texture() != null) TextureStore.save(op.before().index(), op.texture());
                ResourcePackServer.updatePack();
            }
            case REID -> // id was changed old→new → change it back new→old (reId is its own inverse)
                    SlotManager.reId(op.after().customId(), op.before().customId());
            case FLAG -> setFlag(src, op.flag(), !op.flag().on()); // flag was set to `on` → put it back to !on
            case FACE_ROTATE -> { FaceRotations.set(op.faceRot().index(), op.faceRot().face(), op.faceRot().oldQ()); ResourcePackServer.updatePack(); HudSync.broadcast(src.getServer()); } // restore pre-rotate turn + rebuild model; F2: sync packed rot so §B shaped blocks re-mesh on MP
            case BATCH -> { // revert every child of the bulk op as a single step
                if (op.children() != null) {
                    for (UndoManager.Op child : op.children()) applyInverse(src, child);
                }
            }
        }
    }

    /** Re-apply an op (redo direction). */
    private static void applyForward(ServerCommandSource src, UndoManager.Op op) {
        switch (op.kind()) {
            case CREATE -> { // re-create the block
                SlotManager.restoreSnapshot(op.after());
                ResourcePackServer.updatePack();
            }
            case DELETE -> { // re-delete the block → retire its slot + re-grey the placed copies (round-trip)
                SlotManager.removeSilently(op.before().customId());
                com.customblocks.core.DeletedSlots.add(op.before().index());
                com.customblocks.block.DeletedPlacementSweeper.onDeleted(src.getServer(), op.before().index());
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.after());
            case SHAPE -> { // G08 §B — data-only, no pack push (see the undo branch above).
                restoreMeta(src, op.after());
                HudSync.broadcast(src.getServer()); // carries the re-applied shape → client re-mesh
            }
            case TEXTURE -> { // re-apply the new texture + rebuild the pack
                TextureStore.save(op.after().index(), op.textureAfter());
                ResourcePackServer.updatePack();
            }
            case RETEXTURE -> { // re-apply the new slot (incl. AnimData) + new pixels, rebuild
                SlotManager.restoreSnapshot(op.after());
                if (op.textureAfter() != null) TextureStore.save(op.after().index(), op.textureAfter());
                ResourcePackServer.updatePack();
            }
            case REID -> // re-apply the id change old→new
                    SlotManager.reId(op.before().customId(), op.after().customId());
            case FLAG -> setFlag(src, op.flag(), op.flag().on()); // re-apply the flip the op recorded
            case FACE_ROTATE -> { FaceRotations.set(op.faceRot().index(), op.faceRot().face(), op.faceRot().newQ()); ResourcePackServer.updatePack(); HudSync.broadcast(src.getServer()); } // re-apply the rotate + rebuild model; F2: sync packed rot so §B shaped blocks re-mesh on MP
            case BATCH -> { // re-apply every child of the bulk op as a single step
                if (op.children() != null) {
                    for (UndoManager.Op child : op.children()) applyForward(src, child);
                }
            }
        }
    }

    /**
     * Apply a lock/favorite flag through its owning manager — the FLAG kind's equivalent of restoreSnapshot.
     *
     * A lock is a server-wide fact, so every client's padlock widget is re-pushed (same rail as
     * ManagementCommands). A favorite is per-player, so it hits {@code flag.owner()}'s list — NOT the
     * clicker's. Those differ under undoMode=global, where B can undo A's favorite: the right behaviour is
     * to unfavorite it for A, who favorited it. The owner may be offline; the json write still lands, only
     * the live widget push is skipped.
     */
    private static void setFlag(ServerCommandSource src, UndoManager.Flag flag, boolean on) {
        if (flag == null) return;
        if ("lock".equals(flag.which())) {
            if (on) LockManager.lock(flag.id()); else LockManager.unlock(flag.id());
            WidgetSync.pushAll(src.getServer());
            return;
        }
        if (flag.owner() == null) return; // a favorite with no owner is unrestorable — record() prevents it
        if (on) FavoritesManager.add(flag.owner(), flag.id());
        else FavoritesManager.remove(flag.owner(), flag.id());
        ServerPlayerEntity owner = src.getServer().getPlayerManager().getPlayer(flag.owner());
        if (owner != null) WidgetSync.push(owner);
    }

    /** Restore a metadata snapshot, then refresh placed-block lighting (glow may differ). Shape is
     *  data-driven (SlotData) + read live by the block, so a SHAPE undo/redo only needs the snapshot
     *  restore here plus a pack rebuild at the call site (G08 revert 2026-07-20). */
    private static void restoreMeta(ServerCommandSource src, SlotData d) {
        SlotManager.restoreSnapshot(d);
        SlotLighting.applyToPlaced(src.getServer(), d.index(), d.glow());
    }

    /**
     * One step's human description. When {@code undo} is true the value diff reads
     * current→restored (e.g. glow 12→8); on redo it reads restored→reapplied (8→12).
     */
    private static String describe(UndoManager.Op op, boolean undo) {
        if (op.kind() == UndoManager.Kind.BATCH) {
            int n = op.children() == null ? 0 : op.children().size();
            return op.label() + " (" + n + " block" + (n == 1 ? "" : "s") + ")";
        }
        if (op.kind() == UndoManager.Kind.REID) {
            String from = undo ? op.after().customId() : op.before().customId();
            String to = undo ? op.before().customId() : op.after().customId();
            return "reid " + from + CbFmt.DIM + "→" + CbFmt.RESET + " " + to;
        }
        if (op.kind() == UndoManager.Kind.FLAG) { // carries no snapshot — the id is on the payload
            return op.label() + " " + (op.flag() == null ? "?" : op.flag().id());
        }
        if (op.kind() == UndoManager.Kind.FACE_ROTATE) { // no snapshot — index+face live on the payload
            UndoManager.FaceRot fr = op.faceRot();
            if (fr == null) return op.label() + " ?";
            return "rotate " + fr.face() + " " + ((undo ? fr.oldQ() : fr.newQ()) * 90) + "°";
        }
        SlotData ref = op.before() != null ? op.before() : op.after();
        String id = ref != null ? ref.customId() : "?";
        if ((op.kind() == UndoManager.Kind.MODIFY || op.kind() == UndoManager.Kind.SHAPE)
                && op.before() != null && op.after() != null) {
            String d = undo ? diff(op.after(), op.before()) : diff(op.before(), op.after());
            return op.label() + " " + id + (d.isEmpty() ? "" : " " + d);
        }
        return op.label() + " " + id;
    }

    /** Format the single attribute that differs between two snapshots as "from→to" (or "" if none). */
    private static String diff(SlotData a, SlotData b) {
        if (a.glow() != b.glow()) return a.glow() + CbFmt.DIM + "→" + CbFmt.RESET + b.glow();
        if (a.hardness() != b.hardness()) return fmtH(a.hardness()) + CbFmt.DIM + "→" + CbFmt.RESET + fmtH(b.hardness());
        if (!eq(a.soundType(), b.soundType())) return a.soundType() + CbFmt.DIM + "→" + CbFmt.RESET + b.soundType();
        if (a.noCollision() != b.noCollision()) return solid(a.noCollision()) + CbFmt.DIM + "→" + CbFmt.RESET + solid(b.noCollision());
        if (!eq(a.category(), b.category())) return cat(a.category()) + CbFmt.DIM + "→" + CbFmt.RESET + cat(b.category());
        if (!eq(a.shape(), b.shape())) return a.shape() + CbFmt.DIM + "→" + CbFmt.RESET + b.shape();
        if (!eq(a.displayName(), b.displayName())) return "\"" + a.displayName() + "\"" + CbFmt.DIM + "→" + CbFmt.RESET + "\"" + b.displayName() + "\"";
        return "";
    }

    private static boolean eq(String x, String y) {
        return x == null ? y == null : x.equals(y);
    }

    private static String fmtH(float h) {
        return h == Math.rint(h) ? String.valueOf((int) h) : String.valueOf(h);
    }

    private static String solid(boolean noCollision) {
        return noCollision ? "passable" : "solid";
    }

    private static String cat(String c) {
        return c == null || c.isBlank() ? "(none)" : c;
    }

    private static MutableText confirmButton() {
        return Text.literal(CbFmt.OK + "[confirm]").styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cb confirm"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Clear your undo history"))));
    }
}
