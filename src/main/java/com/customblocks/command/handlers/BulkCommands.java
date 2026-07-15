/**
 * BulkCommands.java
 *
 * Responsibility: the chat-side bulk operations over many blocks at once (Group 07): edit a setting
 * (glow / hardness / sound / collision), delete, and rename. /cb confirm and /cb cancel run or discard a
 * pending big batch (they also back the clickable chat buttons). Every no-arg form opens the Bulk Workbench
 * Screen instead (§G07-3) — the chest Hub → Select → Action → Confirm menus were deleted with it.
 *
 * Filters are resolved by {@link BulkScope}. A batch over CustomBlocksConfig.bulkConfirmThreshold
 * blocks (or the "all" filter) is held pending until /cb confirm (60s window, then auto-expires).
 * The whole batch records ONE undo entry via UndoManager.recordBatch, so a single /cb undo reverts
 * it. Value parsing lives in BulkValues to keep this handler under 400 (§9.3).
 *
 * The Workbench does NOT come through here — it calls the applyX cores below via {@link BulkApply}, with
 * structured arguments and no chat confirm guard (it runs its own in-screen modal).
 *
 * All mutation goes through SlotManager (the single source of truth); locked blocks are skipped.
 * Registered into the /cb tree by CommandRegistrar.
 *
 * Depends on: BulkScope, BulkValues, BulkSnapshot, SlotManager, LockManager, UndoManager, SlotLighting,
 *             DeletionService, HudSync, ResourcePackServer, Chat
 * Called by:  CommandRegistrar, BulkApply (the Screen)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.SlotLighting;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.DeletionService;
import com.customblocks.core.FeedbackFx;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkCommands {

    private BulkCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb bulk (§G07-1) and its two muscle-memory aliases open the Bulk Workbench on its Bulk tab.
        root.then(CommandManager.literal("bulk").executes(ctx -> openWorkbench(ctx.getSource())));
        root.then(CommandManager.literal("bulkgui").executes(ctx -> openWorkbench(ctx.getSource())));
        root.then(CommandManager.literal("bulkhub").executes(ctx -> openWorkbench(ctx.getSource())));

        root.then(CommandManager.literal("bulkproperty")
                .executes(ctx -> openOp(ctx.getSource(), "property"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.PROPERTY_ARGS)
                        .executes(ctx -> bulkProperty(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
        root.then(CommandManager.literal("bulkdelete")
                .executes(ctx -> openOp(ctx.getSource(), "delete"))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> bulkDelete(ctx.getSource(), StringArgumentType.getString(ctx, "filter")))));
        root.then(CommandManager.literal("bulkrename")
                .executes(ctx -> openOp(ctx.getSource(), "rename"))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.RENAME_ARGS)
                        .executes(ctx -> bulkRename(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));

        root.then(CommandManager.literal("confirm").executes(ctx -> BulkConfirm.confirm(ctx.getSource())));
        root.then(CommandManager.literal("cancel").executes(ctx -> BulkConfirm.cancel(ctx.getSource())));
    }

    /**
     * Every no-arg bulk command opens the Bulk Workbench on its Bulk tab (§G07-3). The four chest menus this
     * used to walk the player through — Hub → Select → Action → Confirm — are gone; the Screen shows the op
     * rail, the block list, the options and the preview all at once. Shared with the other bulk handlers.
     */
    static int openWorkbench(ServerCommandSource src) {
        return BulkSnapshot.openFor(src, BulkSnapshot.TAB_BULK);
    }

    /**
     * A NAMED bulk* command (e.g. /cb bulkdelete with no filter) opens the Console straight on its own op,
     * skipping the op-picker grid that bare /cb bulk shows (§G07-4 A5a). Shared by every bulk* handler.
     */
    static int openOp(ServerCommandSource src, String opKey) {
        return BulkSnapshot.openForOp(src, opKey);
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
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + scope); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(scope) || blocks.size() > threshold;

        Runnable action = () -> applyProperty(src, blocks, prop, pv);
        if (needConfirm) {
            String summary = "bulkproperty " + prop + "=" + pv.display + " on " + blocks.size() + " block(s)";
            BulkConfirm.request(src, action, summary);
            String hoverList = CbFmt.DIM + "Would set " + CbFmt.VALUE + prop + CbFmt.DIM + "=" + CbFmt.VALUE + pv.display + CbFmt.DIM + " on:\n" + CbFmt.BODY
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Apply to ", CbFmt.BODY + "?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply the validated property to every (non-locked) matched block as one undo batch. */
    static void applyProperty(ServerCommandSource src, List<SlotData> blocks, String prop, BulkValues.Parsed pv) {
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
            BulkResult.record(src, "No blocks changed" + (locked > 0 ? " — all " + locked + " locked" : "") + "."); // X3
            Chat.error(src, "No blocks changed" + (locked > 0 ? " — all " + locked + " matched block(s) are locked" : "") + ".");
            return;
        }
        // One undo entry for the whole batch (G07.2): a single /cb undo reverts everything.
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-edit " + prop + "=" + pv.display);
        BulkResult.record(src, "Set " + prop + "=" + pv.display + " on " + changed.size() + " block(s)"
                + (locked > 0 ? " · " + locked + " locked skipped" : "")); // X3

        // One tidy line: count + the change, full id list on hover, one-click undo. No flooding.
        String hoverList = CbFmt.DIM + "Changed " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n" + CbFmt.BAD + locked + " locked — skipped" : "");
        MutableText msg = Text.literal(CbFmt.OK + "Set " + CbFmt.VALUE + prop + CbFmt.OK + "=" + CbFmt.VALUE + pv.display + CbFmt.OK + " on ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (locked > 0) msg.append(Text.literal("  " + CbFmt.FAINT + locked + " locked"));
        Chat.line(src, msg);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: bulk property change shows live for all players (one push)
        FeedbackFx.fire(src, "bulk_complete");
    }


    // ── Bulk delete (dashboard "Delete" mode — destructive; one undo restores the batch) ──

    /** Resolve the filter and either delete now (small) or hold for /cb confirm (big / all). */
    private static int bulkDelete(ServerCommandSource src, String filter) {
        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        Runnable action = () -> applyDelete(src, blocks);
        if (needConfirm) {
            BulkConfirm.request(src, action, "delete " + blocks.size() + " block(s)");
            String hoverList = CbFmt.BAD + "Will delete " + blocks.size() + " block(s):\n" + CbFmt.BODY
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BAD + CbFmt.BOLD + "Delete ", CbFmt.BAD + "? ", blocks.size(), hoverList,
                    CbFmt.DANGER + CbFmt.BOLD + "[✔ DELETE]", CbFmt.OK + CbFmt.BOLD + "[✖ Keep]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Delete every (non-locked) matched block, recording the whole batch as ONE undo entry. */
    static void applyDelete(ServerCommandSource src, List<SlotData> blocks) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            // Slice 3: same rail as /cb delete — frees the slot AND swaps placed copies to
            // "Deleted: <name>" markers (no purple blocks). Returns the texture for undo.
            byte[] texture = DeletionService.deleteCore(src.getServer(), before);
            children.add(new UndoManager.Op(UndoManager.Kind.DELETE, before, null, texture, "delete"));
            deleted.add(id);
        }
        if (deleted.isEmpty()) {
            BulkResult.record(src, "No blocks deleted" + (locked > 0 ? " — all " + locked + " locked" : "") + "."); // X3
            Chat.error(src, "No blocks deleted" + (locked > 0 ? " — all " + locked + " matched are locked" : "") + ".");
            return;
        }
        ResourcePackServer.updatePack(); // ONE debounced rebuild frees the deleted slots' textures
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-delete (" + deleted.size() + ")");
        BulkResult.record(src, "Deleted " + deleted.size() + " block(s)"
                + (locked > 0 ? " · " + locked + " locked skipped" : "")); // X3
        HudSync.broadcast(src.getServer()); // clear the deleted identities live for everyone (no rejoin)

        String hoverList = CbFmt.DIM + "Deleted " + deleted.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(deleted)
                + (locked > 0 ? "\n\n" + CbFmt.BAD + locked + " locked — skipped" : "");
        MutableText msg = Text.literal(CbFmt.BAD + "Deleted ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + deleted.size() + " block" + (deleted.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (locked > 0) msg.append(Text.literal("  " + CbFmt.FAINT + locked + " locked"));
        Chat.line(src, msg);
        FeedbackFx.fire(src, "bulk_complete");
    }

    // ── Bulk rename (prefix / suffix / replace — display name only, no pack rebuild) ──

    /**
     * Where the mode keyword starts in a {@code <ids…> prefix|suffix|replace <text>} command line.
     *
     * The scope is an id LIST now (Group 04 §A7), so it can run to several tokens — the mode keyword is
     * what ends it. Returns the index of the first prefix/suffix/replace token, or -1 if there is none.
     * Scanning from 1 keeps at least one token for the scope, so a bare mode is still a usage error.
     *
     * A block whose id is literally "prefix"/"suffix"/"replace" would end the list early; comma- or
     * quote-separate the ids to disambiguate. Shared with BulkReidCommands — identical grammar.
     */
    static int modeIndex(String[] tokens) {
        for (int i = 1; i < tokens.length; i++) {
            String t = tokens[i].toLowerCase(Locale.ROOT);
            if (t.equals("prefix") || t.equals("suffix") || t.equals("replace")) return i;
        }
        return -1;
    }

    /** /cb bulkrename &lt;ids…&gt; prefix &lt;text&gt; | suffix &lt;text&gt; | replace &lt;old&gt; &lt;new&gt; */
    private static int bulkRename(ServerCommandSource src, String args) {
        String[] t = args.trim().split("\\s+");
        int m = modeIndex(t);
        if (m < 1 || t.length < m + 2) { usageRename(src); return 0; }
        String filter = String.join(" ", Arrays.copyOf(t, m));
        String mode = t[m].toLowerCase(Locale.ROOT);
        String a = "";
        String b = "";
        switch (mode) {
            case "prefix", "suffix" -> a = String.join(" ", Arrays.copyOfRange(t, m + 1, t.length));
            case "replace" -> {
                if (t.length < m + 3) { usageRename(src); return 0; }
                a = t[m + 1];
                b = t[m + 2];
            }
            default -> { usageRename(src); return 0; }
        }

        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        final String fmode = mode, fa = a, fb = b;
        Runnable action = () -> applyRename(src, blocks, fmode, fa, fb);
        if (needConfirm) {
            BulkConfirm.request(src, action, "rename " + blocks.size() + " block(s)");
            String what = renameWhat(mode, a, b);
            String hoverList = CbFmt.DIM + what + " on:\n" + CbFmt.BODY + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, CbFmt.BODY + "Rename ", CbFmt.BODY + " (" + what + ")?  ", blocks.size(), hoverList,
                    CbFmt.OK + CbFmt.BOLD + "[✔ Confirm]", CbFmt.BAD + CbFmt.BOLD + "[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Apply prefix/suffix/replace to every (non-locked) matched display name, as one undo batch. */
    static void applyRename(ServerCommandSource src, List<SlotData> blocks, String mode, String a, String b) {
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
            BulkResult.record(src, "No names changed" + (locked > 0 ? " — " + locked + " locked" : " — nothing matched") + "."); // X3
            Chat.error(src, "No names changed" + (locked > 0 ? " — " + locked + " locked, " : " ") + "or the text didn't match.");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-rename (" + changed.size() + ")");
        BulkResult.record(src, "Renamed " + changed.size() + " block(s)"
                + (locked > 0 ? " · " + locked + " locked skipped" : "")); // X3
        String hoverList = CbFmt.DIM + "Renamed " + changed.size() + " block(s):\n" + CbFmt.BODY + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n" + CbFmt.BAD + locked + " locked — skipped" : "");
        MutableText msg = Text.literal(CbFmt.OK + "Renamed ")
                .append(Chat.hover(CbFmt.VALUE + CbFmt.UNDER + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + CbFmt.RESET, hoverList))
                .append(Text.literal("  "))
                // G04-UNDO-DIALECT: runButton prepends its own ▶, so a "[↩ Undo]" label rendered "▶ [↩ Undo]".
                .append(Chat.undoButton())
                .append(Text.literal(" " + CbFmt.OK + "✔"));
        if (locked > 0) msg.append(Text.literal("  " + CbFmt.FAINT + locked + " locked"));
        Chat.line(src, msg);
        HudSync.broadcast(src.getServer()); // NO-REJOIN: bulk rename shows live for all players (one push)
        FeedbackFx.fire(src, "bulk_complete");
    }

    private static String renameWhat(String mode, String a, String b) {
        return switch (mode) {
            case "prefix" -> "add prefix \"" + a + "\"";
            case "suffix" -> "add suffix \"" + a + "\"";
            default       -> "replace \"" + a + "\" → \"" + b + "\"";
        };
    }

    static void usageRename(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkrename <ids...> prefix <text> | suffix <text> | replace <old> <new>");
        Chat.info(src, BulkChat.SCOPE_HELP);
    }

    private static void usageProperty(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkproperty <ids...> <glow|hardness|sound|collision> <value>");
        Chat.info(src, "Or run /cb bulk with no arguments to pick blocks and settings in the Bulk Operations Hub.");
    }
}
