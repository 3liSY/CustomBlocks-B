/**
 * BackgroundCommands.java — G10 §C: /cb setbg (alias /cb setbackground).
 *
 *   /cb setbg <id> <value>       — one block.
 *   /cb setbg <scope> <value>    — a chosen selection or every block, using the SAME scope grammar
 *                                  as the bulk commands (all, selected, cat:foo, ...).
 *
 * {@code <value>} is black, a 29-colour library name, or a free hex — one grammar, one stored
 * value, whichever route wrote it (GROUP_10 §B: "single, selected, all-block, Studio, and tool
 * routes write the same SlotData.background value").
 *
 * "transparent" parses but is refused with an honest message: slot blocks are not registered on
 * the cutout render layer yet, so storing it would bake alpha the world renders wrong. That
 * registration is Group 14's half of the §B boundary — see GROUP_14 and Testing_Guide_14.
 *
 * Reuse, do NOT rewrite: target resolution is BulkScope (the bulk grammar), the big-batch hold is
 * BulkConfirm, the re-bake is BackgroundService, and the undo shape is a RETEXTURE op — the one
 * Kind that already restores BOTH the slot snapshot (which now carries background) and the pixels.
 *
 * Depends on: BackgroundValue, BackgroundService, BulkScope, BulkConfirm, SlotManager, TextureStore,
 *             UndoManager, LockManager, ResourcePackServer, HudSync, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BackgroundService;
import com.customblocks.core.BackgroundValue;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.TrashManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BackgroundCommands {

    private BackgroundCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(command("setbg"));
        root.then(command("setbackground")); // alias — identical tree
    }

    private static LiteralArgumentBuilder<ServerCommandSource> command(String name) {
        return CommandManager.literal(name)
                .executes(ctx -> usage(ctx.getSource()))
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> usage(ctx.getSource()))
                        .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                // ColorLibrary.suggest() is a search helper (caps at 3 hits), so the fixed
                                // starter list here is the full menu a player sees before typing anything.
                                .suggests((c, b) -> {
                                    b.suggest(BackgroundValue.BLACK);
                                    for (String s : new String[]{"white", "red", "green", "blue", "yellow", "#1A9BFF"}) b.suggest(s);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setBackground(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "value").trim()))));
    }

    private static int usage(ServerCommandSource src) {
        Chat.info(src, "Set a block's background: " + CbFmt.BODY + "/cb setbg <id> <black|colour|#hex>" + CbFmt.DIM
                + ". Works on a selection or everything too, e.g. " + CbFmt.BODY + "/cb setbg all black" + CbFmt.DIM + ".");
        return 1;
    }

    private static int setBackground(ServerCommandSource src, String target, String rawValue) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        String value = BackgroundValue.normalize(rawValue);
        if (value == null) {
            Chat.error(src, "\"" + rawValue + "\" isn't a background. Use " + CbFmt.BODY + "black" + CbFmt.BAD + ", a colour "
                    + "name like " + CbFmt.BODY + "red" + CbFmt.BAD + ", or a hex like " + CbFmt.BODY + "#1A9BFF" + CbFmt.BAD + ".");
            return 0;
        }
        if (!BackgroundValue.renderable(value)) {
            Chat.error(src, "Transparent backgrounds aren't ready yet — slot blocks still render on the solid "
                    + "layer, so a see-through block would come out wrong. Use " + CbFmt.BODY + "black" + CbFmt.BAD
                    + " or a colour for now (Group 14 owns the cutout render layer this needs).");
            return 0;
        }

        // One block by id, otherwise the bulk scope grammar — no second background system.
        List<SlotData> targets = new ArrayList<>();
        SlotData single = SlotManager.getById(target);
        if (single != null) {
            targets.add(single);
        } else {
            targets.addAll(BulkScope.resolve(target, BulkConfirm.actor(src)));
            if (targets.isEmpty()) {
                // G10 §F: a deleted-but-recoverable id must not dead-end on "no match" — the block is sitting
                // in the trash and the player only needs to be told where it went (TG10 C9).
                TrashManager.TrashEntry trashed = trashEntryFor(target);
                if (trashed != null) {
                    Chat.error(src, CbFmt.BODY + target + CbFmt.BAD + " is in the trash (deleted "
                            + trashed.deletedHuman() + "). Open " + CbFmt.BODY + "/cb trash" + CbFmt.BAD
                            + ", pick it and press Restore, then set its background.");
                    return 0;
                }
                Chat.error(src, "No block or selection matched " + CbFmt.BODY + target + CbFmt.BAD + ".");
                return 0;
            }
        }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(target) || targets.size() > threshold;
        Runnable action = () -> run(src, server, targets, value);
        if (needConfirm) {
            String summary = "set background " + BackgroundValue.label(value) + " on " + targets.size() + " block(s)";
            BulkConfirm.request(src, action, summary);
            String hoverList = CbFmt.DIM + "Would set background " + CbFmt.VALUE + BackgroundValue.label(value)
                    + CbFmt.DIM + " on:\n" + CbFmt.BODY + BulkChat.columns(BulkChat.ids(targets));
            BulkChat.confirm(src, CbFmt.BODY + "Re-bake ", CbFmt.BODY + "?  ", targets.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /**
     * The newest trash entry whose id matches {@code target}, or null. Id matching is case-insensitive to
     * mirror {@link SlotManager#getById}, so "Test10" finds the trashed "test10" the same way a live block
     * would be found. Only consulted after every live route has already missed.
     */
    private static TrashManager.TrashEntry trashEntryFor(String target) {
        if (target == null || target.isBlank()) return null;
        for (TrashManager.TrashEntry e : TrashManager.list()) { // newest first
            if (target.equalsIgnoreCase(e.customId())) return e;
        }
        return null;
    }

    /** Re-bake off the server thread (image work), then commit every changed block in one go. */
    private static void run(ServerCommandSource src, MinecraftServer server, List<SlotData> targets, String value) {
        UUID who = BulkConfirm.actor(src);
        List<SlotData> work = new ArrayList<>();
        List<String> locked = new ArrayList<>();
        for (SlotData d : targets) {
            if (LockManager.isLocked(d.customId())) locked.add(d.customId()); else work.add(d);
        }
        if (work.isEmpty()) {
            Chat.error(src, "Nothing changed — every matched block is locked.");
            return;
        }
        if (work.size() > 1) Chat.info(src, "Re-baking " + work.size() + " block(s) onto " + BackgroundValue.label(value) + "…");

        Thread worker = new Thread(() -> {
            Map<String, byte[]> baked = new LinkedHashMap<>();
            List<String> noSource = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (SlotData d : work) {
                BackgroundService.Result r = BackgroundService.rebake(d.index(), value);
                if (r.ok()) baked.put(d.customId(), r.png());
                else if (r.skip() == BackgroundService.Skip.NO_SOURCE) noSource.add(d.customId());
                else failed.add(d.customId());
            }
            server.execute(() -> commit(src, server, who, value, baked, noSource, failed, locked));
        }, "CustomBlocks-Background");
        worker.setDaemon(true);
        worker.start();
    }

    /** Server thread: store the value + pixels, ONE pack rebuild, ONE undo entry, honest skip report. */
    private static void commit(ServerCommandSource src, MinecraftServer server, UUID who, String value,
                               Map<String, byte[]> baked, List<String> noSource, List<String> failed,
                               List<String> locked) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();

        for (Map.Entry<String, byte[]> e : baked.entrySet()) {
            String id = e.getKey();
            SlotData before = SlotManager.getById(id);
            if (before == null || LockManager.isLocked(id)) continue; // changed while we were baking
            byte[] beforeTex = TextureStore.load(before.index());
            SlotData after = SlotManager.setBackground(id, value);
            if (after == null) continue;
            TextureStore.save(after.index(), e.getValue());
            // RETEXTURE restores the slot snapshot (which now carries background) AND the pixels,
            // so one /cb undo puts back both the choice and the picture.
            children.add(new UndoManager.Op(UndoManager.Kind.RETEXTURE, before, after,
                    beforeTex, e.getValue(), "background", null));
            changed.add(id);
        }

        if (changed.isEmpty()) {
            Chat.error(src, "No backgrounds changed." + skipNote(noSource, failed, locked));
            return;
        }

        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        HudSync.broadcast(server);
        UndoManager.recordBatch(who, children, "background " + BackgroundValue.label(value) + " (" + changed.size() + ")");

        String hoverList = CbFmt.DIM + "Background " + BackgroundValue.label(value) + " on " + changed.size()
                + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed);
        MutableText msg = Text.literal(CbFmt.OK + "Background " + CbFmt.VALUE + BackgroundValue.label(value) + CbFmt.OK + " on ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                .append(Chat.undoButton());
        Chat.line(src, msg);

        String note = skipNote(noSource, failed, locked);
        if (!note.isEmpty()) Chat.info(src, note.trim());
    }

    /** Say exactly what was skipped and why — never silently drop a block from a batch. */
    private static String skipNote(List<String> noSource, List<String> failed, List<String> locked) {
        StringBuilder sb = new StringBuilder();
        if (!noSource.isEmpty()) {
            sb.append(" ").append(noSource.size()).append(" skipped with no stored picture to re-bake (")
              .append(String.join(", ", noSource)).append(") — retexture them from a link first.");
        }
        if (!failed.isEmpty()) {
            sb.append(" ").append(failed.size()).append(" failed to re-bake (").append(String.join(", ", failed)).append(").");
        }
        if (!locked.isEmpty()) {
            sb.append(" ").append(locked.size()).append(" locked and left alone (").append(String.join(", ", locked)).append(").");
        }
        return sb.toString();
    }
}
