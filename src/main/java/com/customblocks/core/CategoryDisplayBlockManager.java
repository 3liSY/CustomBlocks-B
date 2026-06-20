/**
 * CategoryDisplayBlockManager.java — Group 11: which block's texture represents a category.
 *
 * Thin delegate over CategoryMetadataStore: keeps the same public API so all existing callers
 * (CategoryListMenu, CategoryBrowserMenu, SlotManager.renameId hooks) continue to compile
 * unchanged. The actual persistence lives in CategoryMetadataStore now.
 *
 * Depends on: CategoryMetadataStore
 * Called by:  gui/chest/{CategoryListMenu, CategoryBrowserMenu}, core/SlotManager (renameId).
 */
package com.customblocks.core;

public final class CategoryDisplayBlockManager {

    private CategoryDisplayBlockManager() {} // static-only

    /** The block id chosen as {@code category}'s icon, or null if none is set. */
    public static String get(String category) {
        return CategoryMetadataStore.getDisplayBlock(category);
    }

    /** Set the icon block for a category. */
    public static void set(String category, String blockId) {
        CategoryMetadataStore.setDisplayBlock(category, blockId);
    }

    /** Remove a category's icon (falls back to the generic icon). Returns false if none was set. */
    public static boolean clear(String category) {
        return CategoryMetadataStore.clearDisplayBlock(category);
    }

    /** Repoint every icon that referenced {@code oldId} to {@code newId} (for /cb reid). */
    public static void renameId(String oldId, String newId) {
        CategoryMetadataStore.renameBlockId(oldId, newId);
    }
}
