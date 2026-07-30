/**
 * ImportProgress.java — drive the shared top-center progress panel from a folder-import run (G12 §B rule 18).
 *
 * A 40-image import spends real seconds decoding pictures, and before this the owner saw nothing at all
 * between the confirmation and the result — indistinguishable from a frozen server (TG12 B13). This sends
 * the counter the client panel draws.
 *
 * Nothing here is required for a run to work: a vanilla client, or a player who logged out mid-run, simply
 * receives nothing. Every send is guarded by {@code canSend}, so an import never fails because a client
 * could not be told about it.
 *
 * Depends on: ImportProgressPayload, ServerPlayNetworking
 * Called by:  core/ImportService
 */
package com.customblocks.network;

import com.customblocks.network.payloads.ImportProgressPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ImportProgress {

    private ImportProgress() {} // static-only

    /** Show the panel at 0 of {@code total}. */
    public static void begin(ServerPlayerEntity player, int total) {
        send(player, 0, Math.max(1, total), "Importing pictures");
    }

    /** Advance to {@code done} of {@code total}. */
    public static void step(ServerPlayerEntity player, int done, int total, String label) {
        send(player, done, Math.max(1, total), label == null ? "Importing pictures" : label);
    }

    /** Finish: a brief closing line, then the panel hides itself. */
    public static void done(ServerPlayerEntity player, int made) {
        send(player, 1, 1, made + (made == 1 ? " block imported" : " blocks imported"));
    }

    /** Drop the panel immediately (a cancelled or failed run leaves nothing hanging on screen). */
    public static void hide(ServerPlayerEntity player) {
        send(player, 0, 0, "");
    }

    private static void send(ServerPlayerEntity player, int done, int total, String label) {
        if (player == null) return; // console has no panel; the chat report is its whole answer
        try {
            if (!ServerPlayNetworking.canSend(player, ImportProgressPayload.ID)) return; // vanilla client
            ServerPlayNetworking.send(player, new ImportProgressPayload(done, total, label));
        } catch (Exception ignored) {
            // Never let a progress packet break an import.
        }
    }
}
