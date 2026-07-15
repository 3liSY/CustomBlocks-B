/**
 * Achievements.java
 *
 * Responsibility: The registry of all achievement DEFINITIONS (Group 23 §6). Ordered for
 * display; indexed by key for lookup. The 5 milestones are ACTIVE (auto-evaluated against a
 * per-player counter); the rest ship LOCKED and feature-gated, unlocked later as each feature
 * is confirmed (scope decision 2026-06-22).
 *
 * Depends on: Achievement.
 * Called by:  AchievementManager (evaluation), AchievementStore (validation), G27 gallery (display).
 */
package com.customblocks.core.onboarding;

import com.customblocks.core.onboarding.Achievement.Category;
import com.customblocks.core.onboarding.Achievement.Counter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Achievements {

    private Achievements() {}

    /** Definition order = gallery display order. Milestones first, then feature-gated stubs. */
    private static final List<Achievement> ALL = List.of(
            // --- Creation milestones (ACTIVE) — driven by the BLOCKS_CREATED counter ---
            Achievement.milestone("first_block",   "First Block",   "Create your first custom block.",
                    Category.CREATION, Counter.BLOCKS_CREATED, 1),
            Achievement.milestone("ten_blocks",    "Getting Started", "Create 10 custom blocks.",
                    Category.CREATION, Counter.BLOCKS_CREATED, 10),
            Achievement.milestone("fifty_blocks",  "Block Builder",  "Create 50 custom blocks.",
                    Category.CREATION, Counter.BLOCKS_CREATED, 50),
            Achievement.milestone("hundred_blocks","Block Master",   "Create 100 custom blocks.",
                    Category.CREATION, Counter.BLOCKS_CREATED, 100),

            // --- Texture milestone (ACTIVE) — driven by the TEXTURES_APPLIED counter ---
            Achievement.milestone("first_texture", "First Texture", "Apply a texture from a URL.",
                    Category.TEXTURE, Counter.TEXTURES_APPLIED, 1),

            // --- Feature-gated stubs (LOCKED until their feature is wired) ---
            Achievement.gated("ai_texture",        "AI Artist",       "Generate a texture with AI.",            Category.TEXTURE),
            Achievement.gated("animated_block",    "Living Block",    "Create a block from an animated GIF.",   Category.TEXTURE),
            Achievement.gated("first_share",       "Sharer",          "Share a block to the Cloud Vault.",      Category.SHARING),
            Achievement.gated("marketplace_import","Collector",       "Import a block from the marketplace.",   Category.SHARING),
            Achievement.gated("category_share",    "Curator",         "Share an entire category.",              Category.SHARING),
            Achievement.gated("gradient_created",  "Gradient Maker",  "Create a gradient.",                     Category.COLOR),
            Achievement.gated("palette_saved",     "Palette Keeper",  "Save a colour palette.",                 Category.COLOR),
            Achievement.gated("bg_removal",        "Clean Cut",       "Remove a texture's background.",         Category.COLOR),
            Achievement.gated("all_shapes",        "Shapeshifter",    "Use every block shape.",                 Category.MASTERY),
            Achievement.gated("all_tools",         "Tool Master",     "Use every Omni-Tool mode.",              Category.MASTERY),
            Achievement.gated("bulk_master",       "Bulk Master",     "Run a bulk operation on 50+ blocks.",    Category.MASTERY)
    );

    private static final Map<String, Achievement> BY_KEY = index();

    private static Map<String, Achievement> index() {
        Map<String, Achievement> m = new LinkedHashMap<>();
        for (Achievement a : ALL) m.put(a.key(), a);
        return m;
    }

    /** All achievements in display order (immutable). */
    public static List<Achievement> all() { return ALL; }

    /** Total number of achievements — the denominator for "X of Y unlocked". */
    public static int count() { return ALL.size(); }

    /** Look up a definition by key, or null if the key is unknown. */
    public static Achievement byKey(String key) { return BY_KEY.get(key); }

    /** True if {@code key} names a real achievement definition. */
    public static boolean exists(String key) { return BY_KEY.containsKey(key); }
}
