/**
 * BulkConfirm.java
 *
 * The shared "hold a big batch until /cb confirm" mechanism for all bulk ops (Group 07). One
 * pending action per actor, with a 60s expiry. Split out of BulkCommands so every op handler
 * (property / delete / rename / future ones) reuses one confirm flow and each file stays under
 * the 400-line gate (§9.3). /cb confirm and /cb cancel run or discard the held action.
 *
 * Called by: BulkCommands (and future bulk handlers).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulkConfirm {

    private BulkConfirm() {} // static-only

    /** How long a pending batch waits for /cb confirm before it auto-expires. */
    private static final long WINDOW_MS = 60_000L;

    /** Stand-in key for the console / command blocks (no player UUID). */
    private static final UUID CONSOLE = new UUID(0L, 0L);

    private record Pending(Runnable action, long expiresAt, String summary) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    public static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    private static UUID key(ServerCommandSource src) {
        UUID u = actor(src);
        return u != null ? u : CONSOLE;
    }

    /** Hold {@code action} until the actor runs /cb confirm (replaces any earlier pending). */
    public static void request(ServerCommandSource src, Runnable action, String summary) {
        PENDING.put(key(src), new Pending(action, System.currentTimeMillis() + WINDOW_MS, summary));
    }

    /** /cb confirm — run the held action if it hasn't expired. */
    public static int confirm(ServerCommandSource src) {
        Pending p = PENDING.remove(key(src));
        if (p == null) { Chat.info(src, "Nothing to confirm."); return 0; }
        if (System.currentTimeMillis() > p.expiresAt()) {
            Chat.info(src, "That bulk confirmation expired — run it again.");
            return 0;
        }
        p.action().run();
        return 1;
    }

    /** /cb cancel — discard the held action. */
    public static int cancel(ServerCommandSource src) {
        Pending p = PENDING.remove(key(src));
        Chat.info(src, p == null ? "Nothing to cancel." : "Cancelled: " + p.summary() + ".");
        return 1;
    }
}
