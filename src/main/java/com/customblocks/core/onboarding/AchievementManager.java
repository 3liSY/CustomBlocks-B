/**
 * AchievementManager.java
 *
 * Responsibility: The public face of the achievement engine. Command handlers call the
 * record/unlock methods when something happens; this class bumps the right counter, decides
 * which achievements that crossed, persists via AchievementStore, and fires the unlock
 * notification (action-bar line + chat message with a clickable [View]). Read methods feed the
 * dashboard tab and the G27 gallery screen.
 *
 * Nothing here opens a screen or touches the chest GUI — callers pass a ServerPlayerEntity and
 * this stays server-side. The [View] link simply runs /cb achievements.
 *
 * Depends on: Achievement, Achievements, AchievementStore, Minecraft text API.
 * Called by:  (future wiring) the create/retexture/feature handlers; the dashboard + gallery for reads.
 */
package com.customblocks.core.onboarding;

import com.customblocks.command.Chat;

import com.customblocks.command.CbFmt;
import com.customblocks.core.onboarding.Achievement.Counter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.UUID;

public final class AchievementManager {

    private AchievementManager() {}

    // ---- recording (called when something happens in-game) ---------------------------

    /** Record that the player created a block; unlocks any creation milestone just crossed. */
    public static void recordBlockCreated(ServerPlayerEntity player) {
        int total = AchievementStore.bumpCounter(player.getUuid(), Counter.BLOCKS_CREATED, 1);
        evaluateCounter(player, Counter.BLOCKS_CREATED, total);
    }

    /** Record that the player applied a URL texture; unlocks first_texture on the first one. */
    public static void recordTextureApplied(ServerPlayerEntity player) {
        int total = AchievementStore.bumpCounter(player.getUuid(), Counter.TEXTURES_APPLIED, 1);
        evaluateCounter(player, Counter.TEXTURES_APPLIED, total);
    }

    /**
     * Directly unlock a feature-gated achievement (e.g. ai_texture) when its feature is wired.
     * No-op if the key is unknown or already unlocked.
     */
    public static void unlock(ServerPlayerEntity player, String key) {
        Achievement def = Achievements.byKey(key);
        if (def == null) return;
        award(player, def);
    }

    /** Re-check every counter-driven milestone for a player (e.g. after a server restart import). */
    private static void evaluateCounter(ServerPlayerEntity player, Counter counter, int value) {
        for (Achievement a : Achievements.all()) {
            if (!a.active() || a.counter() != counter) continue;
            if (value >= a.target()) award(player, a);
        }
    }

    /** Mark unlocked (idempotent) and notify only if it was newly unlocked. */
    private static void award(ServerPlayerEntity player, Achievement def) {
        boolean isNew = AchievementStore.markUnlocked(player.getUuid(), def.key(), System.currentTimeMillis());
        if (isNew) notifyUnlock(player, def);
    }

    // ---- reads (for the dashboard tab + the G27 gallery) -----------------------------

    public static boolean isUnlocked(UUID uuid, String key) { return AchievementStore.isUnlocked(uuid, key); }
    public static long unlockedAt(UUID uuid, String key)    { return AchievementStore.unlockedAt(uuid, key); }
    public static int  unlockedCount(UUID uuid)             { return AchievementStore.unlockedCount(uuid); }
    public static int  total()                              { return Achievements.count(); }

    /** Current value of a progress counter (e.g. blocks created) — for the "3/10" text. */
    public static int counterValue(UUID uuid, Counter counter) { return AchievementStore.counter(uuid, counter); }

    /** "X of Y unlocked" string for the dashboard progress slot (Group 23 §9 / G25.11). */
    public static String progressLine(UUID uuid) {
        return unlockedCount(uuid) + " of " + total() + " achievements unlocked";
    }

    // ---- notification ----------------------------------------------------------------

    private static void notifyUnlock(ServerPlayerEntity player, Achievement def) {
        // Above the hotbar (action bar): "Achievement Unlocked: <Name>".
        Chat.tool(player, "Achievement Unlocked:", def.name());
        // Chat line with a clickable [View] that opens the achievements GUI.
        MutableText view = Text.literal(" " + CbFmt.OK + "[View]").styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cb achievements"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Open your achievements"))));
        Chat.toPlayer(player,
                Text.literal(CbFmt.DIM + "You unlocked " + CbFmt.BODY + "\"" + def.name() + "\"" + CbFmt.DIM + "!").append(view));
    }
}
