/**
 * BulkCategoryCommands.java
 *
 * Bulk "move to category" (Group 07): reassign the category of every matched block at once, the
 * batch counterpart of /cb setcategory. Kept in its own handler so BulkCommands stays under the
 * 400-line gate (§9.3); shares the bulk plumbing (BulkScope filters, BulkConfirm guard for big/all
 * batches, BulkChat hover list, one UndoManager batch entry).
 *
 *   /cb bulkcategory <filter> <category>   — set the category (or "none" to clear)
 *   /cb bulkcategory                        — opens the dashboard pre-set to Move mode
 *
 * Category is metadata only — no texture or resource-pack rebuild (mirrors the single setter).
 * Locked blocks are skipped. Mutation goes through SlotManager (the single source of truth).
 *
 * Depends on: BulkScope, BulkConfirm, BulkChat, SlotManager, LockManager, UndoManager, Chat
 * Called by:  CommandRegistrar, BulkActionMenu (applyCategoryFromGui)
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BulkCategoryCommands {

    private BulkCategoryCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkcategory")
                .executes(ctx -> openBuilder(ctx.getSource()))
                .then(CommandManager.argument("args", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.CATEGORY_ARGS)
                        .executes(ctx -> bulkCategory(ctx.getSource(), StringArgumentType.getString(ctx, "args")))));
    }

    /** Open the two-step builder pre-set to the "Move to category" operation. */
    private static int openBuilder(ServerCommandSource src) {
        return BulkCommands.openOpBuilder(src, "category");
    }

    /** Close the dashboard, then run the assembled move (confirm fires in chat if big/all). */
    public static void applyCategoryFromGui(ServerPlayerEntity player, String filter, String category) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkCategory(player.getCommandSource(), (filter + " " + category).trim());
        });
    }

    // ── /cb bulkcategory <filter> <category> ──────────────────────────────────

    private static int bulkCategory(ServerCommandSource src, String args) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 2) { usage(src); return 0; }
        String catRaw = parts[parts.length - 1];
        String scope  = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
        String cat    = normalize(catRaw);

        List<SlotData> blocks = BulkScope.resolve(scope, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + scope); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(scope) || blocks.size() > threshold;

        Runnable action = () -> applyCategory(src, blocks, cat);
        if (needConfirm) {
            String what = cat.isEmpty() ? "clear the category of" : "move to \"" + cat + "\"";
            BulkConfirm.request(src, action, "move " + blocks.size() + " block(s)");
            String hoverList = "§7Will " + what + ":\n§f" + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§fMove ", "§f (" + what + ")?  ", blocks.size(), hoverList,
                    "§a§l[✔ Confirm]", "§c§l[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    /** Set the category on every (non-locked) matched block, as one undo batch. */
    private static void applyCategory(ServerCommandSource src, List<SlotData> blocks, String cat) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        int locked = 0;
        for (SlotData target : blocks) {
            String id = target.customId();
            if (LockManager.isLocked(id)) { locked++; continue; }
            SlotData before = SlotManager.getById(id);
            if (before == null) continue;
            SlotData after = SlotManager.setCategory(id, cat);
            if (after == null) continue;
            children.add(new UndoManager.Op(UndoManager.Kind.MODIFY, before, after, null, "bulk category"));
            changed.add(id);
        }
        if (changed.isEmpty()) {
            Chat.error(src, "No blocks moved" + (locked > 0 ? " — all " + locked + " matched are locked" : "") + ".");
            return;
        }
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-category (" + changed.size() + ")");

        String verb = cat.isEmpty() ? "Cleared the category of " : "Moved ";
        String tail = cat.isEmpty() ? "" : " §ato §b" + cat;
        String hoverList = "§7" + (cat.isEmpty() ? "Cleared" : "Moved to " + cat) + " for "
                + changed.size() + " block(s):\n§f" + BulkChat.columns(changed)
                + (locked > 0 ? "\n\n§c" + locked + " locked — skipped" : "");
        MutableText msg = Text.literal("§a" + verb)
                .append(Chat.hover("§e§n" + changed.size() + " block" + (changed.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal(tail))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Revert this whole batch §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        if (locked > 0) msg.append(Text.literal("  §8" + locked + " locked"));
        Chat.line(src, msg);
    }

    /** "none"/"clear"/"uncategorized" → "" (clear); otherwise lowercase (mirrors /cb setcategory). */
    private static String normalize(String raw) {
        String c = raw.trim();
        if (c.equalsIgnoreCase("none") || c.equalsIgnoreCase("clear") || c.equalsIgnoreCase("uncategorized")) {
            return "";
        }
        return c.toLowerCase(Locale.ROOT);
    }

    private static void usage(ServerCommandSource src) {
        Chat.error(src, "Usage: /cb bulkcategory <filter> <category>   (use 'none' to clear)");
        Chat.info(src, "Filters: all · category:<name> · id:<prefix> · name:<text> · favorite:yes|no · locked:yes|no");
    }
}
