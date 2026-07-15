/**
 * Achievement.java
 *
 * Responsibility: Immutable definition of one achievement (its identity, display text,
 * category, and how it unlocks). Pure data — no state, no I/O. The per-player unlock
 * state lives in AchievementStore; the live tracking/notification logic in AchievementManager.
 *
 * Depends on: nothing (plain Java).
 * Called by:  Achievements (the registry), AchievementManager, the future G27 gallery screen.
 */
package com.customblocks.core.onboarding;

public record Achievement(String key, String name, String description,
                          Category category, Counter counter, int target, boolean active) {

    /** Display grouping for the gallery (Group 23 §6). */
    public enum Category { CREATION, TEXTURE, SHARING, COLOR, MASTERY }

    /**
     * Which per-player counter, if any, auto-unlocks this achievement when it reaches {@link #target}.
     * NONE = unlocked manually via AchievementManager.unlock(...) when its feature is wired later.
     */
    public enum Counter { NONE, BLOCKS_CREATED, TEXTURES_APPLIED }

    /** A milestone that the engine evaluates automatically against a counter. */
    public static Achievement milestone(String key, String name, String description,
                                        Category category, Counter counter, int target) {
        return new Achievement(key, name, description, category, counter, target, true);
    }

    /**
     * A feature-gated achievement that ships LOCKED and is unlocked directly when its
     * feature is later confirmed and wired (Group 23 scope decision, 2026-06-22).
     */
    public static Achievement gated(String key, String name, String description, Category category) {
        return new Achievement(key, name, description, category, Counter.NONE, 1, false);
    }
}
