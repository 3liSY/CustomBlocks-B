/**
 * CategoryAdminBridge.java — Group 27 §G27.6 (Block Creation Studio, Category tab) server-side glue.
 *
 * Handles the studio's CategoryAdminPayload through the EXISTING category rails (no new persistence
 * beyond DefaultCategoryStore):
 *   - create  — register a category as existing (§G27 L11), even at 0 blocks, so it's listed everywhere.
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
import com.customblocks.core.CategoryService;
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

        // §G27 Category Hub: "assign" moves a single block (id in arg) into category c ("" = uncategorize),
        // so it carries a block id, not a category name — handle it before the empty-category guard.
        if ("assign".equals(op)) { assign(src, player, arg, c); return; }

        if (c.isEmpty()) { Chat.error(src, "No category was given."); return; }

        switch (op == null ? "" : op) {
            case "create" -> create(src, player, c);
            case "rename" -> rename(src, player, c, arg);
            case "delete" -> delete(src, player, c);
            case "color"  -> { CategoryMetadataStore.setColorTag(c, arg == null ? "" : arg);
                               CategoryMetadataStore.setColorHex(c, ""); // a swatch pick clears any custom hex
                               Chat.success(src, "Colour updated for category \"" + c + "\".");
                               HudSync.broadcast(player.getServer()); } // NO-REJOIN: all players
            case "colorhex" -> { String hex = CategoryMetadataStore.normalizeHex(arg);
                                 CategoryMetadataStore.setColorHex(c, hex);
                                 if (!hex.isEmpty()) CategoryMetadataStore.setColorTag(c, ""); // custom hex wins over a §-swatch
                                 Chat.success(src, hex.isEmpty()
                                         ? "Custom colour cleared for category \"" + c + "\"."
                                         : "Custom colour " + hex + " set for category \"" + c + "\".");
                                 HudSync.broadcast(player.getServer()); } // NO-REJOIN: all players
            case "default" -> { boolean wasDefault = c.equalsIgnoreCase(DefaultCategoryStore.get());
                                DefaultCategoryStore.set(wasDefault ? "" : c); // §G27 L8: click the current default again to clear it
                                Chat.success(src, wasDefault
                                        ? "\"" + c + "\" is no longer the default category."
                                        : "\"" + c + "\" is now the default category for new blocks.");
                                HudSync.broadcast(player.getServer()); } // NO-REJOIN: all players
            // §G27 Category Hub ops — reuse the shared CategoryService engine (same as /cb category …).
            case "sort"   -> report(src, player, CategoryService.setSort(c, arg));
            case "merge"  -> report(src, player, CategoryService.merge(c, arg)); // c = source, arg = target
            case "lock"   -> report(src, player, CategoryService.lockAll(c, true));
            case "unlock" -> report(src, player, CategoryService.lockAll(c, false));
            default -> Chat.error(src, "Unknown category action.");
        }
    }

    /**
     * Report a CategoryService.Outcome to chat, then live-push the change to every client (NO-REJOIN).
     *
     * Same rails as a typed command: CategoryChat colours the category name, makes it clickable, and
     * hangs the outcome's follow-up chip off the line, so a Hub action and a typed one read alike.
     */
    private static void report(ServerCommandSource src, ServerPlayerEntity player, CategoryService.Outcome o) {
        CategoryChat.report(src, o);
        if (o.ok()) HudSync.broadcast(player.getServer());
    }

    /** Move one block (by id) into category {@code cat} ("" = uncategorize), then live-push (NO-REJOIN). */
    private static void assign(ServerCommandSource src, ServerPlayerEntity player, String blockId, String cat) {
        String id = blockId == null ? "" : blockId.trim();
        if (id.isEmpty()) { Chat.error(src, "No block was given to move."); return; }
        if (SlotManager.getById(id) == null) { Chat.error(src, "There's no block called \"" + id + "\"."); return; }
        SlotManager.setCategory(id, cat);
        Chat.success(src, cat.isEmpty()
                ? "Moved \"" + id + "\" out of its category."
                : "Moved \"" + id + "\" into \"" + cat + "\".");
        HudSync.broadcast(player.getServer()); // NO-REJOIN: block's category shows live for all players
    }

    /** §G27 L11: register {@code c} as a real category (0 blocks, no other metadata) so it's listed everywhere. */
    private static void create(ServerCommandSource src, ServerPlayerEntity player, String c) {
        String id = c.replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (id.isEmpty()) { Chat.error(src, "Give the category a name (letters, numbers or underscores)."); return; }
        CategoryMetadataStore.create(id);
        Chat.success(src, "Created category \"" + id + "\".");
        HudSync.broadcast(player.getServer()); // NO-REJOIN: shows up in every player's hub immediately
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
        HudSync.broadcast(player.getServer()); // NO-REJOIN: category rename shows live for all players
    }

    private static void delete(ServerCommandSource src, ServerPlayerEntity player, String c) {
        List<SlotData> blocks = SlotManager.byCategory(c);
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), "");
        CategoryMetadataStore.deleteCategory(c);
        DefaultCategoryStore.onCategoryGone(c, "");
        Chat.success(src, "Deleted category \"" + c + "\" — " + blocks.size() + " block(s) are now uncategorized.");
        HudSync.broadcast(player.getServer()); // NO-REJOIN: category delete shows live for all players
    }
}
