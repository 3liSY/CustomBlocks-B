/**
 * TipPool.java
 *
 * Responsibility: Supply a rotating helpful tip for the dashboard "Tip" slot (Group 23 §9 /
 * G25.12). Early-game tips are shown first; once a player has unlocked a few achievements the
 * pool widens to advanced tips. Rotation is per-player and advances each time the dashboard
 * asks for the next tip — kept in memory only (a tip that resets on restart is harmless).
 *
 * Depends on: AchievementManager (to widen the pool by progress).
 * Called by:  (future wiring) MainMenu when rendering the Tip slot.
 */
package com.customblocks.core.onboarding;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TipPool {

    private TipPool() {}

    /** Shown from the start — basics every new player needs. */
    private static final List<String> BASIC = List.of(
            "Create a block with /cb create <id> <name>, then texture it with /cb retexture <id> <url>.",
            "Open this dashboard any time with /cb.",
            "Give yourself a block with /cb give <id>, or grab it from the Custom Blocks creative tab.",
            "Grab your tools from the Custom Tools creative tab — the Omni-Tool does glow, shape and more.",
            "Stuck? /cb help lists every command.");

    /** Unlocked once the player has some achievements — deeper features. */
    private static final List<String> ADVANCED = List.of(
            "Hold a glowing block to emit dynamic light.",
            "Share a block to the Cloud Vault with /cb share, and import others' blocks from the vault.",
            "Make a colour gradient or save a palette from the colour tools.",
            "Bulk-edit many blocks at once with the /cb bulk commands.",
            "Change a block's shape (slab, stairs, and more) from the Shape editor.");

    /** After this many unlocked achievements, advanced tips join the rotation. */
    private static final int ADVANCED_AT = 3;

    /** uuid -> next index into the (possibly widened) pool. In-memory only. */
    private static final Map<String, Integer> CURSOR = new ConcurrentHashMap<>();

    /** Return the next tip for this player and advance their rotation cursor. */
    public static String next(UUID uuid) {
        List<String> pool = poolFor(uuid);
        String key = uuid.toString();
        int i = CURSOR.getOrDefault(key, 0);
        String tip = pool.get(Math.floorMod(i, pool.size()));
        CURSOR.put(key, i + 1);
        return tip;
    }

    private static List<String> poolFor(UUID uuid) {
        if (AchievementManager.unlockedCount(uuid) >= ADVANCED_AT) {
            java.util.ArrayList<String> all = new java.util.ArrayList<>(BASIC);
            all.addAll(ADVANCED);
            return all;
        }
        return BASIC;
    }
}
