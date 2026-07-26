/**
 * BackgroundCommands.java — G10 §C: /cb setbg (alias /cb setbackground), and §H: /cb bgpick.
 *
 *   /cb setbg <id> <value>       — one block.
 *   /cb setbg <scope> <value>    — a chosen selection or every block, using the SAME scope grammar
 *                                  as the bulk commands (all, selected, cat:foo, ...).
 *   /cb bgpick <id> <colour>     — name the BACKGROUND colour of one block's picture, for when the
 *                                  automatic detector said it was not sure. One block only.
 *
 * The two are different questions on purpose. {@code setbg} says what the background should BECOME;
 * {@code bgpick} says what the background IS, which is the one fact the detector was missing. They
 * live together because they take the same colour grammar, the same permission tier and the same
 * undo shape.
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
 * Depends on: BackgroundValue, BackgroundService, BackgroundRemover, ImageProcessor, ColorLibrary,
 *             BulkScope, BulkConfirm, SlotManager, TextureStore,
 *             UndoManager, LockManager, ResourcePackServer, HudSync, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.BackgroundService;
import com.customblocks.core.BackgroundValue;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.TrashManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
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
        root.then(bgpick());
    }

    /**
     * {@code /cb bgpick <id> <colour>} — one block, never a scope. The whole point is that the player
     * is supplying a fact about ONE picture, so there is nothing to broadcast across a selection.
     */
    private static LiteralArgumentBuilder<ServerCommandSource> bgpick() {
        return CommandManager.literal("bgpick")
                .executes(ctx -> bgpickUsage(ctx.getSource()))
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> bgpickUsage(ctx.getSource()))
                        .then(CommandManager.argument("colour", StringArgumentType.greedyString())
                                // Same starter menu as setbg: the colour grammar is shared, so what a
                                // player can type in one they can type in the other.
                                .suggests((c, b) -> {
                                    for (String x : new String[]{"white", "black", "red", "green", "blue", "yellow", "#1A9BFF"}) b.suggest(x);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> bgpick(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "colour").trim()))));
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

    private static int bgpickUsage(ServerCommandSource src) {
        Chat.info(src, "Tell the mod which colour is the background: " + CbFmt.BODY
                + "/cb bgpick <id> <colour|#hex>" + CbFmt.DIM + ". Use it when a picture came out unchanged "
                + "because the mod was not sure what to remove.");
        return 1;
    }

    /**
     * §H escape hatch: re-bake ONE block using a background colour the player named.
     *
     * <p>It re-bakes the block's STORED SOURCE, so it needs no download and costs one bake — and a
     * block with no stored picture gets the same honest skip {@code /cb setbg} already gives rather
     * than a guessed re-bake. The stored background choice is not touched: this changes which pixels
     * were treated as background, not what colour they become.
     */
    private static int bgpick(ServerCommandSource src, String target, String rawColour) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        String hex = ColorLibrary.resolve(rawColour);
        if (hex == null) {
            Chat.error(src, CbFmt.BODY + rawColour + CbFmt.BAD + " isn't a colour. Use a name like "
                    + CbFmt.BODY + "white" + CbFmt.BAD + " or a hex like " + CbFmt.BODY + "#1A9BFF" + CbFmt.BAD + ".");
            return 0;
        }
        final int keyRgb;
        try {
            keyRgb = Integer.parseInt(hex.replace("#", ""), 16) & 0xFFFFFF;
        } catch (Exception e) {
            Chat.error(src, "Couldn't read " + CbFmt.BODY + rawColour + CbFmt.BAD + " as a colour.");
            return 0;
        }

        SlotData d = SlotManager.getById(target);
        if (d == null) {
            TrashManager.TrashEntry trashed = trashEntryFor(target);
            if (trashed != null) {
                Chat.error(src, CbFmt.BODY + target + CbFmt.BAD + " is in the trash (deleted "
                        + trashed.deletedHuman() + "). Restore it with " + CbFmt.BODY + "/cb trash"
                        + CbFmt.BAD + " first.");
                return 0;
            }
            Chat.error(src, "There's no block called " + CbFmt.BODY + target + CbFmt.BAD + ".");
            return 0;
        }
        if (LockManager.isLocked(target)) { Chat.error(src, CbFmt.BODY + target + CbFmt.BAD + " is locked."); return 0; }

        final byte[] source = TextureStore.loadSource(d.index());
        if (source == null || source.length == 0) {
            Chat.error(src, CbFmt.BODY + target + CbFmt.BAD + " has no stored picture to re-bake — give it one with "
                    + CbFmt.BODY + "/cb retexture " + target + " <url>" + CbFmt.BAD + " first.");
            return 0;
        }

        final UUID who = BulkConfirm.actor(src);
        final String id = target;
        Chat.info(src, "Removing the " + CbFmt.VALUE + hex + CbFmt.DIM + " background from " + CbFmt.BODY + id + CbFmt.DIM + "…");

        Thread worker = new Thread(() -> {
            // The stored background decides what the removed area BECOMES; the named colour only says
            // what to remove. A block set to a colour keeps baking onto that colour.
            String stored = d.background();
            Integer fill = (stored != null && BackgroundValue.renderable(stored) && !BackgroundValue.BLACK.equals(stored))
                    ? BackgroundValue.rgb(stored) : null;
            BackgroundRemover.Keyed r = BackgroundRemover.applyNamedKey(source, keyRgb, fill);
            byte[] png = null;
            if (r.ok()) {
                try {
                    png = ImageProcessor.toBlockPng(r.png(), CustomBlocksConfig.textureSize);
                    png = fill != null
                            ? BackgroundRemover.snapBackgroundColor(ImageProcessor.fillBackground(png, fill), BackgroundRemover.AUTO, fill)
                            : BackgroundRemover.snapBackgroundBlack(png, BackgroundRemover.AUTO);
                } catch (Exception e) {
                    png = null; // reported below as a refusal rather than a silent no-op
                }
            }
            final byte[] finalPng = png;
            server.execute(() -> {
                if (finalPng == null) {
                    String why = r.refusal() != null ? r.refusal() : "that picture could not be re-baked";
                    Chat.error(src, "Couldn't use " + CbFmt.BODY + hex + CbFmt.BAD + " as the background of "
                            + CbFmt.BODY + id + CbFmt.BAD + " — " + why + ".");
                    return;
                }
                SlotData before = SlotManager.getById(id);
                if (before == null || LockManager.isLocked(id)) {
                    Chat.error(src, CbFmt.BODY + id + CbFmt.BAD + " changed while that was baking — nothing applied.");
                    return;
                }
                byte[] beforeTex = TextureStore.load(before.index());
                TextureStore.save(before.index(), finalPng);
                // RETEXTURE restores the pixels, which is all bgpick changed — the stored background
                // choice is untouched, so one /cb undo puts the old picture back exactly.
                UndoManager.recordTexture(who, before, beforeTex, finalPng, "bgpick " + hex);
                ResourcePackServer.updatePack();
                ResourcePackServer.syncToAll();
                HudSync.broadcast(server);
                MutableText msg = Text.literal(CbFmt.OK + "Removed the " + CbFmt.VALUE + hex + CbFmt.OK
                                + " background from " + CbFmt.VALUE + id + CbFmt.OK + ".  ")
                        .append(Chat.undoButton());
                Chat.line(src, msg);
            });
        }, "CustomBlocks-BgPick");
        worker.setDaemon(true);
        worker.start();
        return 1;
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
