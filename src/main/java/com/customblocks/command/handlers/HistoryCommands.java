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
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
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
        report(src, "Undid", steps, UndoManager.undoSize(who));
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
        report(src, "Redid", steps, UndoManager.redoSize(who));
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
        MutableText msg = Text.literal(Chat.PREFIX + "§eClear all §f" + n + "§e undo step"
                        + (n == 1 ? "" : "s") + "?§r This can't be undone. ")
                .append(confirmButton())
                .append(Text.literal(" §7or §f/cb cancel"));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    /** Undo exactly one step for the player; returns false if there was nothing to undo. */
    public static boolean undoOnce(ServerPlayerEntity player) {
        UndoManager.Op op = UndoManager.undo(player.getUuid());
        if (op == null) return false;
        applyInverse(player.getCommandSource(), op);
        return true;
    }

    /** Redo exactly one step for the player; returns false if there was nothing to redo. */
    public static boolean redoOnce(ServerPlayerEntity player) {
        UndoManager.Op op = UndoManager.redo(player.getUuid());
        if (op == null) return false;
        applyForward(player.getCommandSource(), op);
        return true;
    }

    /** Print a one-line result for a single step, or a bulleted list with values for many. */
    private static void report(ServerCommandSource src, String verb, List<String> steps, int left) {
        if (steps.isEmpty()) { // shouldn't happen (guarded above), but stay safe
            Chat.info(src, verb.equals("Undid") ? "Nothing to undo" : "Nothing to redo");
            return;
        }
        if (steps.size() == 1) {
            Chat.success(src, verb + " " + steps.get(0) + " §7(" + left + " left)");
            return;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§a" + verb + " " + steps.size() + " actions:"), false);
        for (String s : steps) {
            src.sendFeedback(() -> Text.literal("§7  • §f" + s), false);
        }
        src.sendFeedback(() -> Text.literal("§7  (" + left + " left)"), false);
    }

    /** Reverse an op (undo direction). */
    private static void applyInverse(ServerCommandSource src, UndoManager.Op op) {
        switch (op.kind()) {
            case CREATE -> { // block was created → remove it again
                SlotManager.removeSilently(op.after().customId());
                ResourcePackServer.updatePack();
            }
            case DELETE -> { // block was deleted → bring it back (data + texture)
                SlotManager.restoreSnapshot(op.before());
                if (op.texture() != null) TextureStore.save(op.before().index(), op.texture());
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.before());
            case SHAPE -> { // restore the old shape, then rebuild the pack so the model reverts too
                restoreMeta(src, op.before());
                ResourcePackServer.updatePack();
            }
            case TEXTURE -> { // pixels were replaced → put the old texture back + rebuild the pack
                TextureStore.save(op.before().index(), op.texture());
                ResourcePackServer.updatePack();
            }
            case REID -> // id was changed old→new → change it back new→old (reId is its own inverse)
                    SlotManager.reId(op.after().customId(), op.before().customId());
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
            case DELETE -> { // re-delete the block
                SlotManager.removeSilently(op.before().customId());
                ResourcePackServer.updatePack();
            }
            case MODIFY -> restoreMeta(src, op.after());
            case SHAPE -> { // re-apply the new shape + rebuild the pack
                restoreMeta(src, op.after());
                ResourcePackServer.updatePack();
            }
            case TEXTURE -> { // re-apply the new texture + rebuild the pack
                TextureStore.save(op.after().index(), op.textureAfter());
                ResourcePackServer.updatePack();
            }
            case REID -> // re-apply the id change old→new
                    SlotManager.reId(op.before().customId(), op.after().customId());
            case BATCH -> { // re-apply every child of the bulk op as a single step
                if (op.children() != null) {
                    for (UndoManager.Op child : op.children()) applyForward(src, child);
                }
            }
        }
    }

    /** Restore a metadata snapshot, then refresh placed-block lighting (glow may differ). */
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
            return "reid " + from + "§7→§r " + to;
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
        if (a.glow() != b.glow()) return a.glow() + "§7→§r" + b.glow();
        if (a.hardness() != b.hardness()) return fmtH(a.hardness()) + "§7→§r" + fmtH(b.hardness());
        if (!eq(a.soundType(), b.soundType())) return a.soundType() + "§7→§r" + b.soundType();
        if (a.noCollision() != b.noCollision()) return solid(a.noCollision()) + "§7→§r" + solid(b.noCollision());
        if (!eq(a.category(), b.category())) return cat(a.category()) + "§7→§r" + cat(b.category());
        if (!eq(a.shape(), b.shape())) return a.shape() + "§7→§r" + b.shape();
        if (!eq(a.displayName(), b.displayName())) return "\"" + a.displayName() + "\"§7→§r\"" + b.displayName() + "\"";
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
        return Text.literal("§a[confirm]").styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cb confirm"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Clear your undo history"))));
    }
}
