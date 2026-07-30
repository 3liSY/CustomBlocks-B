/**
 * CategoryReport.java — the read-only category listings (Group 11).
 *
 * Split out of CategoryService under the 500-line class gate (§9.3). Nothing here mutates: it turns
 * category records and memberships into the chat lines behind `/cb category list <mode>`,
 * `/cb category filter [mode] <name>` and `/cb category info [list] <name>`.
 *
 * {@code Uncategorized} is rendered dim + italic wherever it appears (G11 C16) — it is the system
 * floor every block falls back to, not a category the owner made.
 *
 * Depends on: CategoryFilters, CategoryMembershipStore, CategoryMetadataStore, LockManager,
 *             TextureStore, SlotData, CbFmt
 * Called by:  CategoryCommands, CategoryMemberCommands
 */
package com.customblocks.core;

import com.customblocks.command.CbFmt;

import java.util.ArrayList;
import java.util.List;

public final class CategoryReport {

    private CategoryReport() {} // static-only

    /** How a category is shown to a player: its typed name, e.g. "Arabic Letters". */
    private static String shown(String cat) {
        return CategoryMetadataStore.getDisplayName(cat);
    }

    /** The style a category's name is printed in — its colour tag, or the system-floor style. */
    private static String style(String key) {
        if (CategoryMembershipStore.isUncategorized(key)) return CbFmt.DIM + CbFmt.ITALIC;
        String tag = CategoryMetadataStore.getColorTag(key);
        return tag.isEmpty() ? CbFmt.BODY : tag;
    }

    /** Category-listing lines in {@code mode} order, or null when the mode isn't one of ours. */
    public static List<String> filterCategories(String mode) {
        List<String> keys = CategoryFilters.categories(mode);
        if (keys == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add(CbFmt.HEAD + "Categories (" + keys.size() + ") " + CbFmt.DIM + "· " + CategoryFilters.mode(mode));
        for (String k : keys) {
            lines.add(CbFmt.DIM + " • " + style(k) + shown(k)
                    + CbFmt.RESET + CbFmt.DIM + "  (" + CategoryMembershipStore.blocksIn(k).size() + ")");
        }
        return lines;
    }

    /** Block-listing lines for one category in {@code mode} order, or null for an unknown mode. */
    public static List<String> filterBlocks(String category, String mode) {
        String cat = CategoryMembershipStore.key(category);
        List<SlotData> blocks = CategoryFilters.blocks(cat, mode);
        if (blocks == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add(CbFmt.HEAD + shown(cat) + CbFmt.DIM + " · " + blocks.size() + " block(s) · " + CategoryFilters.mode(mode));
        for (SlotData d : blocks) lines.add(CbFmt.DIM + " • " + CbFmt.BODY + d.customId());
        return lines;
    }

    /**
     * Category summary. {@code listBlocks} appends every block id (TG11 A8) — the count alone is
     * the default so a 200-block category doesn't flood chat unasked.
     */
    public static List<String> info(String category, boolean listBlocks) {
        String cat = CategoryMembershipStore.key(category);
        List<SlotData> blocks = CategoryMembershipStore.blocksIn(cat);
        List<String> lines = new ArrayList<>();
        if (blocks.isEmpty() && !CategoryMetadataStore.keyExists(cat)) {
            lines.add(CbFmt.DIM + "There's no category " + CbFmt.BODY
                    + CategoryMembershipStore.unquote(category) + CbFmt.DIM + ". See /cb categories.");
            return lines;
        }
        int locked = 0;
        long texBytes = 0;
        for (SlotData d : blocks) {
            if (LockManager.isLocked(d.customId())) locked++;
            byte[] tex = TextureStore.load(d.index());
            if (tex != null) texBytes += tex.length;
        }
        String icon = CategoryMetadataStore.getDisplayBlock(cat);
        String sort = CategoryMetadataStore.getSortOrder(cat);
        lines.add(CbFmt.HEAD + "Category: " + style(cat) + shown(cat));
        lines.add(CbFmt.DIM + "Blocks: " + CbFmt.BODY + blocks.size() + "  " + CbFmt.DIM + "Locked: " + CbFmt.BODY + locked
                + CbFmt.DIM + "/" + CbFmt.BODY + (blocks.size() - locked) + " unlocked");
        lines.add(CbFmt.DIM + "Texture total: " + CbFmt.BODY + humanSize(texBytes));
        lines.add(CbFmt.DIM + "Sort: " + CbFmt.BODY + ("custom".equals(sort) ? "Custom" : "Alphabetical"));
        lines.add(CbFmt.DIM + "Icon: " + CbFmt.BODY + (icon == null ? "default" : icon));
        if (listBlocks) {
            if (blocks.isEmpty()) {
                lines.add(CbFmt.FAINT + "(no blocks yet)");
            } else {
                List<String> ids = new ArrayList<>();
                for (SlotData d : blocks) ids.add(d.customId());
                lines.add(CbFmt.DIM + "Blocks: " + CbFmt.BODY + String.join(", ", ids));
            }
        }
        return lines;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
