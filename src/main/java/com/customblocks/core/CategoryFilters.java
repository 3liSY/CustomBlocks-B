/**
 * CategoryFilters.java — ordering for `/cb category filter` (Group 11 §B).
 *
 * G11 replaced the old `sort` verb with `filter`, which has TWO separate mode sets: one for the
 * category listing, one for the blocks inside a category. Count, colour and emptiness are
 * category-only concepts, so they are deliberately absent from the block set rather than silently
 * ignored — asking for them on blocks is an error the caller reports.
 *
 * Default with no mode given is alphabetical for both (G11 Locked Decisions), matching what
 * `/cb category list` already did — nothing changes until a mode is asked for.
 *
 * Split out of CategoryService so that file stays about mutation and this one stays about order.
 *
 * Depends on: CategoryMembershipStore, CategoryMetadataStore, SlotData, SlotManager
 * Called by:  CategoryService
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class CategoryFilters {

    private CategoryFilters() {} // static-only

    /** Category-listing modes (G11: alphabetical, newest, oldest, most blocks, by colour, hide-empty). */
    public static final List<String> CATEGORY_MODES =
            List.of("alpha", "newest", "oldest", "most", "color", "hideempty");

    /** Block-listing modes inside a category — no count/colour/emptiness, those aren't block traits. */
    public static final List<String> BLOCK_MODES = List.of("alpha", "newest", "oldest");

    /** Normalize a typed mode; "" (nothing typed) means the alphabetical default. */
    public static String mode(String raw) {
        String m = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return m.isEmpty() ? "alpha" : m;
    }

    // ── Category listing ─────────────────────────────────────────────────────

    /**
     * Every known category key in {@code mode} order. Returns null for an unknown mode so the
     * caller can name the valid ones rather than quietly falling back to alphabetical.
     *
     * "Known" is the union of metadata records and keys actually in use, so an empty
     * explicitly-created category still lists (G11: categories may be empty) — except under
     * {@code hideempty}, which is the mode for exactly that.
     */
    public static List<String> categories(String mode) {
        String m = mode(mode);
        if (!CATEGORY_MODES.contains(m)) return null;

        Set<String> all = new TreeSet<>(CategoryMetadataStore.knownCategories());
        all.addAll(CategoryMembershipStore.keysInUse());
        List<String> out = new ArrayList<>(all);

        switch (m) {
            case "newest" -> out.sort(Comparator
                    .comparingLong((String k) -> CategoryMetadataStore.getCreatedAt(k)).reversed()
                    .thenComparing(k -> k));
            case "oldest" -> out.sort(Comparator
                    .comparingLong((String k) -> CategoryMetadataStore.getCreatedAt(k))
                    .thenComparing(k -> k));
            case "most" -> out.sort(Comparator
                    .comparingInt((String k) -> CategoryMembershipStore.blocksIn(k).size()).reversed()
                    .thenComparing(k -> k));
            // Grouped by colour tag: same-coloured categories sit together, alphabetical within a
            // group, and the untagged ones fall to the end rather than leading under "".
            case "color" -> out.sort(Comparator
                    .comparing((String k) -> {
                        String tag = CategoryMetadataStore.getColorTag(k);
                        return tag.isEmpty() ? "￿" : tag;
                    })
                    .thenComparing(k -> k));
            case "hideempty" -> {
                out.removeIf(k -> CategoryMembershipStore.blocksIn(k).isEmpty());
                out.sort(Comparator.naturalOrder());
            }
            default -> out.sort(Comparator.naturalOrder()); // alpha
        }
        return out;
    }

    // ── Blocks inside a category ─────────────────────────────────────────────

    /**
     * The blocks holding {@code category}, in {@code mode} order. Returns null for an unknown mode.
     *
     * Newest/oldest read the slot INDEX, not a timestamp: a block has never carried a creation
     * time, and indices are handed out lowest-free-first while deleted ones are permanently
     * reserved (DeletedSlots), so a higher index IS a later creation. Adding a timestamp field to
     * SlotData would mean a new column on every block and eight back-compat constructors for a
     * value the index already tells us.
     */
    public static List<SlotData> blocks(String category, String mode) {
        String m = mode(mode);
        if (!BLOCK_MODES.contains(m)) return null;
        List<SlotData> out = new ArrayList<>(CategoryMembershipStore.blocksIn(category));
        switch (m) {
            case "newest" -> out.sort(Comparator.comparingInt(SlotData::index).reversed());
            case "oldest" -> out.sort(Comparator.comparingInt(SlotData::index));
            default -> out.sort(Comparator.comparing(SlotData::customId, String.CASE_INSENSITIVE_ORDER));
        }
        return out;
    }
}
