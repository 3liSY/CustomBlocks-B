/**
 * CategoryAdminBridge.java — Group 27 §G27.6 (Block Creation Studio, Category tab) server-side glue.
 *
 * Handles the studio's CategoryAdminPayload through the EXISTING category rails (no new persistence
 * beyond DefaultCategoryStore):
 *   - rename  — move every block in the category to a new name + carry its metadata + default pointer.
 *   - delete  — uncategorize every block in the category + drop its metadata + clear the default.
 *   - color   — set the category's §-colour tag (CategoryMetadataStore).
 *   - default — set this category as the studio's default (DefaultCategoryStore).
 * Re-syncs the player's HUD cache afterwards so the studio chips refresh. Server-authoritative.
 *
 * Depends on: SlotManager, CategoryMetadataStore, DefaultCategoryStore, HudSync, Chat.
 * Called by:  CustomBlocksMod (CategoryAdminPayload receiver).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.DefaultCategoryStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.HudSync;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Locale;

public final class CategoryAdminBridge {

    private CategoryAdminBridge() {} // static-only

    public static void handle(ServerPlayerEntity player, String op, String cat, String arg) {
        ServerCommandSource src = player.getCommandSource();
        String c = cat == null ? "" : cat.trim().toLowerCase(Locale.ROOT);
        if (c.isEmpty()) { Chat.error(src, "No category was given."); return; }

        switch (op == null ? "" : op) {
            case "rename" -> rename(src, player, c, arg);
            case "delete" -> delete(src, player, c);
            case "color"  -> { CategoryMetadataStore.setColorTag(c, arg == null ? "" : arg);
                               Chat.success(src, "Colour updated for category \"" + c + "\".");
                               HudSync.sendTo(player); }
            case "default" -> { DefaultCategoryStore.set(c);
                                Chat.success(src, "\"" + c + "\" is now the default category for new blocks.");
                                HudSync.sendTo(player); }
            default -> Chat.error(src, "Unknown category action.");
        }
    }

    private static void rename(ServerCommandSource src, ServerPlayerEntity player, String c, String arg) {
        String to = arg == null ? "" : arg.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (to.isEmpty()) { Chat.error(src, "Give the category a new name (letters, numbers or underscores)."); return; }
        if (to.equals(c)) return;
        List<SlotData> blocks = SlotManager.byCategory(c);
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), to);
        CategoryMetadataStore.renameCategory(c, to);
        DefaultCategoryStore.onCategoryGone(c, to);
        Chat.success(src, "Renamed category \"" + c + "\" to \"" + to + "\" (" + blocks.size() + " block(s)).");
        HudSync.sendTo(player);
    }

    private static void delete(ServerCommandSource src, ServerPlayerEntity player, String c) {
        List<SlotData> blocks = SlotManager.byCategory(c);
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), "");
        CategoryMetadataStore.deleteCategory(c);
        DefaultCategoryStore.onCategoryGone(c, "");
        Chat.success(src, "Deleted category \"" + c + "\" — " + blocks.size() + " block(s) are now uncategorized.");
        HudSync.sendTo(player);
    }
}
