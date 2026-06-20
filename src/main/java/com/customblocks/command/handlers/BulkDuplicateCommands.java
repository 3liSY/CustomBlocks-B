/**
 * BulkDuplicateCommands.java
 *
 * Bulk duplicate (Group 07): copy every matched block into a new block, texture + attributes and
 * all (SlotManager.dupe clones everything). Each copy gets a unique id derived from the source —
 * "<id>_copy", then "_copy2", "_copy3"… so a batch never collides. The whole batch records ONE
 * UndoManager entry (CREATE children), so a single /cb undo removes every copy; ONE pack rebuild
 * runs after the batch. Big/all batches confirm in chat first.
 *
 *   /cb bulkduplicate <filter>     /cb bulkduplicate            (no args → dashboard, Duplicate mode)
 *
 * Locked sources are fine to copy (duplicating reads, never edits, the original). Slot exhaustion
 * is reported as a skipped count. Kept in its own handler to stay under the 400-line gate (§9.3).
 *
 * Depends on: BulkScope, BulkConfirm, BulkChat, SlotManager, UndoManager, ResourcePackServer, Chat
 * Called by:  CommandRegistrar, BulkActionMenu (applyDuplicateFromGui)
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BulkDuplicateCommands {

    private BulkDuplicateCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("bulkduplicate")
                .executes(ctx -> openBuilder(ctx.getSource()))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .suggests(BulkSuggestions.FILTER_ONLY)
                        .executes(ctx -> bulkDuplicate(ctx.getSource(), StringArgumentType.getString(ctx, "filter"), false))));
    }

    private static int openBuilder(ServerCommandSource src) {
        return BulkCommands.openOpBuilder(src, "duplicate");
    }

    /** Close the dashboard, then duplicate the assembled filter (confirm fires in chat if big). */
    public static void applyDuplicateFromGui(ServerPlayerEntity player, String filter) {
        net.minecraft.server.MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            bulkDuplicate(player.getCommandSource(), filter, true);
        });
    }

    private static int bulkDuplicate(ServerCommandSource src, String filter, boolean force) {
        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) { Chat.error(src, "No blocks matched filter: " + filter); return 0; }

        int threshold = Math.max(1, CustomBlocksConfig.bulkConfirmThreshold);
        boolean needConfirm = BulkScope.isAll(filter) || blocks.size() > threshold;

        Runnable action = () -> applyDuplicate(src, blocks);
        if (needConfirm && !force) {
            BulkConfirm.request(src, action, "duplicate " + blocks.size() + " block(s)");
            String hoverList = "§7Will copy " + blocks.size() + " block(s):\n§f"
                    + BulkChat.columns(BulkChat.ids(blocks));
            BulkChat.confirm(src, "§fDuplicate ", "§f?  ", blocks.size(), hoverList,
                    "§a§l[✔ Confirm]", "§c§l[✖ Cancel]");
            return 1;
        }
        action.run();
        return 1;
    }

    private static void applyDuplicate(ServerCommandSource src, List<SlotData> blocks) {
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> made = new ArrayList<>();
        int failed = 0;
        for (SlotData target : blocks) {
            String newId = uniqueId(target.customId() + "_copy");
            if (newId == null) { failed++; continue; }
            SlotData d = SlotManager.dupe(target.customId(), newId);
            if (d == null) { failed++; continue; } // no free slot / id race
            children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, d, null, "duplicate"));
            made.add(newId);
        }
        if (made.isEmpty()) {
            Chat.error(src, "Nothing duplicated" + (failed > 0 ? " — no free slots for " + failed + " block(s)" : "") + ".");
            return;
        }
        ResourcePackServer.updatePack(); // ONE rebuild — the copies' textures were just written
        UndoManager.recordBatch(BulkConfirm.actor(src), children, "bulk-duplicate (" + made.size() + ")");

        String hoverList = "§7Created " + made.size() + " copy(ies):\n§f" + BulkChat.columns(made)
                + (failed > 0 ? "\n\n§c" + failed + " skipped — no free slot" : "");
        MutableText msg = Text.literal("§aDuplicated into ")
                .append(Chat.hover("§e§n" + made.size() + " new block" + (made.size() == 1 ? "" : "s") + "§r", hoverList))
                .append(Text.literal("  "))
                .append(Chat.runButton("§e[↩ Undo]", "/cb undo", "§7Remove every copy §8(/cb undo)"))
                .append(Text.literal(" §a✔"));
        if (failed > 0) msg.append(Text.literal("  §8" + failed + " skipped"));
        Chat.line(src, msg);
    }

    /** "<base>", then "<base>2", "<base>3"… — the first id not already in use, or null if none free. */
    private static String uniqueId(String base) {
        if (!SlotManager.hasId(base)) return base;
        for (int i = 2; i <= 999; i++) {
            String candidate = base + i;
            if (!SlotManager.hasId(candidate)) return candidate;
        }
        return null;
    }
}
