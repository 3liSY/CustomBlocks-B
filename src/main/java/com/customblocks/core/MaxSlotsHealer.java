/**
 * MaxSlotsHealer.java
 *
 * Group 21 / §9 (D13) — the ONE shared, raise-only, idempotent healer for the cross-client
 * `max_blocks` (config key `maxSlots`) desync. Both sync callers funnel through
 * {@link #ensureAtLeast(int)} so there is a single write path: no race, no double write, no
 * double restart-prompt (§9.6).
 *
 *   Caller A (Phase A): client config-phase mixin (RegistrySyncHealMixin) — derives the server's
 *                       slot count from the registry-sync map and heals before the raw kick.
 *   Caller B (Phase B): server handshake (ConfigSyncPayload receiver) — same call, exact number.
 *
 * Heals by RAISING the local config only (client >= server is always safe; only client < server
 * kicks). The block registry is frozen at launch, so the new value takes effect on the NEXT
 * restart — that one restart is unavoidable (§9.2), not a failure.
 *
 * Depends on: CustomBlocksConfig (the maxSlots field), CustomBlocksConfigStore (atomic save)
 * Called by:  RegistrySyncHealMixin (Phase A); ConfigSyncPayload receiver (Phase B)
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksConfigStore;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MaxSlotsHealer {

    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlocks");

    /** Set once a heal writes a higher value; true means a full client restart is pending. */
    private static volatile boolean restartPending = false;

    private MaxSlotsHealer() {} // static-only

    /**
     * Raise-only, idempotent. If the local maxSlots is already >= needed, does nothing and
     * returns false. Otherwise atomically raises the on-disk config to `needed` (clamped to the
     * registered range 1..8192, matching CustomBlocksConfigStore) and flags a restart.
     *
     * @return true if it changed the config (a restart is now required)
     */
    public static synchronized boolean ensureAtLeast(int needed) {
        int target = clamp(needed, 1, 8192);
        if (CustomBlocksConfig.maxSlots >= target) {
            return false; // idempotent no-op — the other caller already healed, or we were fine
        }
        int previous = CustomBlocksConfig.maxSlots;
        CustomBlocksConfig.maxSlots = target;
        CustomBlocksConfigStore.save(); // atomic temp + rename
        restartPending = true;
        LOGGER.info("[CustomBlocks] max_blocks self-heal: raised maxSlots {} -> {} (restart required)",
                previous, target);
        return true;
    }

    /** True once a heal has written a higher value this session (restart needed to take effect). */
    public static boolean isRestartPending() {
        return restartPending;
    }

    /**
     * The single friendly wording shown on both heal paths (§9.6). Plain English, both numbers.
     *
     * @param serverNeeds the server's max_blocks (what we raised to)
     * @param youWere     the client's previous value
     */
    public static Text restartScreenText(int serverNeeds, int youWere) {
        return Text.literal(
                "This server uses " + serverNeeds + " custom blocks; your game was set to " + youWere
                        + ".\nWe've updated your setting - fully restart Minecraft, then rejoin.");
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
