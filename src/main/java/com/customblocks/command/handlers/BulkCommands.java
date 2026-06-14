/**
 * BulkCommands.java
 *
 * Responsibility: bulk operations over many blocks at once (Group 07), driven entirely by the
 * Bulk Dashboard GUI (/cb bulkgui). Modes: edit a setting (glow / hardness / sound / collision)
 * and delete. The dashboard's Apply/Delete buttons call applyFromGui / applyDeleteFromGui here.
 * /cb confirm and /cb cancel run or discard a pending big batch (they also back the clickable
 * chat buttons).
 *
 * Filters are resolved by {@link BulkScope}. A batch over CustomBlocksConfig.bulkConfirmThreshold
 * blocks (or the "all" filter) is held pending until /cb confirm (60s window, then auto-expires).
 * The whole batch records ONE undo entry via UndoManager.recordBatch, so a single /cb undo reverts
 * it (Group 07 test G07.2). Value parsing lives in BulkValues to keep this handler under 400 (§9.3).
 *
 * All mutation goes through SlotManager (the single source of truth); locked blocks are skipped.
 * Registered into the /cb tree by CommandRegistrar.
 *
 * Depends on: BulkScope, BulkValues, SlotManager, LockManager, UndoManager, SlotLighting,
 *             TextureStore, BlockNotesManager, ResourcePackServer, Chat
 * Called by:  CommandRegistrar, BulkActionMenu
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkCommands {

    private BulkCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb bulkgui and /cb bulkhub open the Bulk Hub (the front door to every op).
        root.then(CommandManager.literal("bulkgui").executes(ctx -> openHub(ctx.getSource())));
        root.then(CommandManager.literal("bulkhub").executes(ctx -> openHub(ctx.getSource())));

        root.then(CommandManager.literal("bulkproperty")
                .executes(ctx -> openBuilder(ctx.getSource()))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.PROPERTY_ARGS)
                        .executes(ctx -> bulkProperty(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
        root.then(CommandManager.literal("bulkdelete")
                .executes(ctx -> openDeleteBuilder(ctx.getSource()))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> bulkDelete(ctx.getSource(), StringArgumentType.getString(ctx, "filter")))));
        root.then(CommandManager.literal("bulkrename")
                .executes(ctx -> openOpBuilder(ctx.getSource(), "rename"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.RENAME_ARGS)
                        .executes(ctx -> bulkRename(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));

        root.then(CommandManager.literal("confirm").executes(ctx -> BulkConfirm.confirm(ctx.getSource())));
        root.then(CommandManager.literal("cancel").executes(ctx -> BulkConfirm.cancel(ctx.getSource())));
    }

    /** Open the Bulk Hub (the operation picker) for a player; console can't drive the GUI. */
    private static int openHub(ServerCommandSource src) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            com.customblocks.gui.chest.GuiFx.open(p);
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.BULK_HUB));
            return 1;
        }
        Chat.error(src, "Open the bulk hub in-game with /cb bulkgui.");
        return 0;
    }

    /** Open the two-step builder for /cb bulkproperty with no args. */
    private static int openBuilder(ServerCommandSource src) { return openOpBuilder(src, "property"); }

    /** Open the two-step builder for /cb bulkdelete with no args. */
    private static int openDeleteBuilder(ServerCommandSource src) { return openOpBuilder(src, "delete"); }

    /** Open the op's Step-1 selection builder fresh ("None selected currently"). */
    static int openOpBuilder(ServerCommandSource src, String op) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            BulkSession s = BulkSession.get(p.getUuid());
            s.op = op;
            s.resetSelection();
            com.customblocks.gui.chest.GuiFx.open(p);
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.BULK_SELECT));
            return 1;
        }
        Chat.error(src, "Open the bulk builder in-game with /cb bulkgui.");
        return 0;
    }

    /**
     * Apply a bulk edit assembled in the dashboard: close the menu, then run the validated
     * batch through the same path the chat command used (confirm-guard + batch-undo intact).
     */
    public static void applyFromGui(ServerPlayerEntity player, String filter, String property, String value) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkProperty(player.getCommandSource(), (filter + " " + property + " " + value).trim());
        });
    }

    /** Run a rename assembled in the dashboard (prefix/suffix use text A; replace uses A → B). */
    public static void applyRenameFromGui(ServerPlayerEntity player, String filter, String mode, String a, String b) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        String args = "replace".equals(mode)
                ? filter + " replace " + a + " " + b
                : filter + " " + mode + " " + a;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkRename(player.getCommandSource(), args.trim());
        });
    }

    // ── /cb bulkproperty <filter> <property> <value> ──────────────────────────

    private static int bulkProperty(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 3) { usageProperty(src); return 0; }
        String value = parts[parts.length - 1];
        String prop  = parts[parts.length - 2].toLowerCase(Locale.ROOT);
        String scope = String.join(" ", Arrays.copyOf(parts, parts.length - 2));

        // Validate property + value ONCE up front — fail before touching any block.
        BulkValues.Parsed pv = BulkValues.parse(src, prop, value);
        if (pv == null) return 0;

        List<SlotData> blocks = BulkScope.resolve(scope, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + scope); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(scope) || blocks.size() > threshold;

        Runnable action = () -> applyProperty(src, blocks, prop, pv);
        if (needConfirm) {
            String summary = "bulkproperty " + prop + "=" + pv.display + " on " + blocks.size() + " block(s)";
            BulkConfirm.request(src, action, summary);
            String hoverList = "§7Would set §b" + prop + "§7=§b" + pv.display + "§7 on:\n§f"
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§fApply to ", "§f?  ", blocks.size(), hoverList,
                    "§a§l[✔ Confirm]", "§c§l[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply the validated property to every (non-locked) matched block as one undo batch. */
    private static void applyProperty(ServerCommandSource src, List<SlotData> blocks, String prop, BulkValues.Parsed pv) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            SlotData after = switch (prop) {
                case "glow", "light" -> SlotManager.setGlow(id, pv.intVal);
                case "hardness"      -> SlotManager.setHardness(id, pv.floatVal);
                case "sound"         -> SlotManager.setSoundType(id, pv.strVal);
                case "collision"     -> SlotManager.setNoCollision(id, pv.boolVal);
                default              -> null;
            };
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.MODIFY, before, after, null, "bulk " + prop));
            changed.add(id);
            // Glow is the only property that needs an already-placed-block refresh (mirrors /cb setglow).
            if (prop.equals("glow") || prop.equals("light")) {
                SlotLighting.applyToPlaced(src.getServer(), after.index(), pv.intVal);
            }
        }
        if (changed.isEmpty()) {
            Chat.error(src, "No blocks changed" + (locked > 0 ? " — all " + locked + " matched block(s) are locked" : "") + ".");
            return;
        }
        // One undo entry for the whole batch (G07.2): a single /cb undo reverts everything.
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-edit " + prop + "=" + pv.display);

        // One tidy line: count + the change, full id list on hover, one-click undo. No flooding.
        String hoverList = "§7Changed " + changed.size() + " block(s):\n§f" + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n§c" + locked + " locked — skipped" : "");
        MutableText msg = Text.literal("§aSet §b" + prop + "§a=§b" + pv.display + "§a on ")
                .append(Chat.hover("§e§n" + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Revert this whole batch §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        if (locked > 0) msg.append(Text.literal("  §8" + locked + " locked"));
        Chat.line(src, msg);
    }


    // ── Bulk delete (dashboard "Delete" mode — destructive; one undo restores the batch) ──

    /** Close the dashboard, then delete the assembled filter (confirm fires in chat if big). */
    public static void applyDeleteFromGui(ServerPlayerEntity player, String filter) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkDelete(player.getCommandSource(), filter);
        });
    }

    /** Resolve the filter and either delete now (small) or hold for /cb confirm (big / all). */
    private static int bulkDelete(ServerCommandSource src, String filter) {
        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        Runnable action = () -> applyDelete(src, blocks);
        if (needConfirm) {
            BulkConfirm.request(src, action, "delete " + blocks.size() + " block(s)");
            String hoverList = "§cWill delete " + blocks.size() + " block(s):\n§f"
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§c§lDelete ", "§c? ", blocks.size(), hoverList,
                    "§4§l[✔ DELETE]", "§a§l[✖ Keep]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Delete every (non-locked) matched block, recording the whole batch as ONE undo entry. */
    private static void applyDelete(ServerCommandSource src, List<SlotData> blocks) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            byte[] texture = TextureStore.load(before.index());  // capture BEFORE delete so undo restores it
            SlotManager.delete(id);
            BlockNotesManager.onBlockDeleted(id);                // clean up any orphaned note
            children.add(new UndoManager.Op(UndoManager.Kind.DELETE, before, null, texture, "delete"));
            deleted.add(id);
        }
        if (deleted.isEmpty()) {
            Chat.error(src, "No blocks deleted" + (locked > 0 ? " — all " + locked + " matched are locked" : "") + ".");
            return;
        }
        ResourcePackServer.updatePack(); // ONE debounced rebuild frees the deleted slots' textures
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-delete (" + deleted.size() + ")");

        String hoverList = "§7Deleted " + deleted.size() + " block(s):\n§f" + BulkChat.columns(deleted)
                + (locked > 0 ? "\n\n§c" + locked + " locked — skipped" : "");
        MutableText msg = Text.literal("§cDeleted ")
                .append(Chat.hover("§e§n" + deleted.size() + " block" + (deleted.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Restore them all §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        if (locked > 0) msg.append(Text.literal("  §8" + locked + " locked"));
        Chat.line(src, msg);
    }

    // ── Bulk rename (prefix / suffix / replace — display name only, no pack rebuild) ──

    /** /cb bulkrename &lt;filter&gt; prefix &lt;text&gt; | suffix &lt;text&gt; | replace &lt;old&gt; &lt;new&gt; */
    private static int bulkRename(ServerCommandSource src, String args) {
        String[] t = args.trim().split("\\s+");
        if (t.length < 3) { usageRename(src); return 0; }
        String filter = t[0];
        String mode = t[1].toLowerCase(Locale.ROOT);
        String a = "";
        String b = "";
        switch (mode) {
            case "prefix", "suffix" -> a = String.join(" ", Arrays.copyOfRange(t, 2, t.length));
            case "replace" -> {
                if (t.length < 4) { usageRename(src); return 0; }
                a = t[2];
                b = t[3];
            }
            default -> { usageRename(src); return 0; }
        }

        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        final String fmode = mode, fa = a, fb = b;
        Runnable action = () -> applyRename(src, blocks, fmode, fa, fb);
        if (needConfirm) {
            BulkConfirm.request(src, action, "rename " + blocks.size() + " block(s)");
            String what = renameWhat(mode, a, b);
            String hoverList = "§7" + what + " on:\n§f" + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§fRename ", "§f (" + what + ")?  ", blocks.size(), hoverList,
                    "§a§l[✔ Confirm]", "§c§l[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply prefix/suffix/replace to every (non-locked) matched display name, as one undo batch. */
    private static void applyRename(ServerCommandSource src, List<SlotData> blocks, String mode, String a, String b) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            String newName = switch (mode) {
                case "prefix"  -> a + before.displayName();
                case "suffix"  -> before.displayName() + a;
                case "replace" -> before.displayName().replace(a, b);
                default        -> before.displayName();
            };
            if (newName.equals(before.displayName())) continue; // nothing to change
            SlotData after = SlotManager.rename(id, newName);
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.MODIFY, before, after, null, "bulk rename"));
            changed.add(id);
        }
        if (changed.isEmpty()) {
            Chat.error(src, "No names changed" + (locked > 0 ? " — " + locked + " locked, " : " ") + "or the text didn't match.");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-rename (" + changed.size() + ")");
        String hoverList = "§7Renamed " + changed.size() + " block(s):\n§f" + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n§c" + locked + " locked — skipped" : "");
        MutableText msg = Text.literal("§aRenamed ")
                .append(Chat.hover("§e§n" + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Revert this whole batch §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        if (locked > 0) msg.append(Text.literal("  §8" + locked + " locked"));
        Chat.line(src, msg);
    }

    private static String renameWhat(String mode, String a, String b) {
        return switch (mode) {
            case "prefix" -> "add prefix \"" + a + "\"";
            case "suffix" -> "add suffix \"" + a + "\"";
            default       -> "replace \"" + a + "\" → \"" + b + "\"";
        };
    }

    private static void usageRename(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkrename <filter> prefix <text> | suffix <text> | replace <old> <new>");
        Chat.info(src, "Filters: all · category:<name> · id:<prefix> · name:<text> · favorite:yes|no · locked:yes|no");
    }

    /** Internal usage fallback — the dashboard normally supplies valid args. */
    private static void usageProperty(ServerCommandSource src) {
        Chat.error(src, "Open the bulk dashboard in-game with /cb bulkgui.");
    }
}
