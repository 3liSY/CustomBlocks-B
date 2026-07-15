/**
 * UpdateController.java
 *
 * Responsibility: Client-side brain for §K auto-update (GROUP_20 §CS3). Handles the server's
 * VersionHandshakePayload on join: reads the client's own version (fabric.mod.json), compares it to
 * the server's (AU2), and routes:
 *   • same version            → nothing (K1).
 *   • client NEWER than server → yellow warning chat line, never downgrade (AU9 / K5).
 *   • client OLDER, auto-update OFF on the server → warning chat line, no download (AU6 / K4).
 *   • client OLDER, auto-update ON, download available → open {@link UpdateScreen} (K2).
 *   • client OLDER, auto-update ON, but the server has no jar to serve → informational chat line.
 *
 * One-shot per connection (guarded + reset on disconnect) so a resend can't double-prompt.
 * House style is chat feedback (the codebase has no toast helper); §CS3 calls these "toasts".
 * CLIENT-SIDE ONLY.
 *
 * Depends on: FabricLoader (own version), MinecraftClient, VersionCompare, UpdateScreen
 * Called by: CustomBlocksClient (VersionHandshakePayload receiver + DISCONNECT reset)
 */
package com.customblocks.client.update;

import com.customblocks.CustomBlocksMod;
import com.customblocks.network.payloads.VersionHandshakePayload;
import com.customblocks.update.VersionCompare;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class UpdateController {

    private static volatile boolean handledThisConnection = false;

    private UpdateController() {} // static-only

    /** Reset the one-shot guard so the next server connection can prompt again. Call on disconnect. */
    public static void reset() {
        handledThisConnection = false;
        JarUpdater.reset();
    }

    /** Handle a version handshake. Safe to call off-thread — hops to the client thread itself. */
    public static void onHandshake(MinecraftClient client, VersionHandshakePayload p) {
        client.execute(() -> handle(client, p));
    }

    private static void handle(MinecraftClient client, VersionHandshakePayload p) {
        if (handledThisConnection) return;
        handledThisConnection = true;

        String own = ownVersion();
        String server = p.serverVersion();
        if (own == null || server == null || server.isBlank()) return; // can't compare — stay silent

        int cmp = VersionCompare.compare(own, server);
        if (cmp == 0) return; // K1 — same version, nothing to do

        if (cmp > 0) {
            // K5 / AU9 — client is newer. Never downgrade; just warn.
            chat(client, "§e[CustomBlocks] This server runs an older version (v" + server
                    + "). You have v" + own + " — some features may differ.");
            return;
        }

        // client is OLDER than the server.
        if (!p.autoUpdateEnabled()) {
            // K4 / AU6 — server disabled auto-update. Warn only.
            chat(client, "§e[CustomBlocks] Server has v" + server + ", you have v" + own
                    + ". Auto-update is off on this server — update manually to match.");
            return;
        }
        if (p.downloadUrl() == null || p.downloadUrl().isBlank()
                || p.sha256() == null || p.sha256().isBlank()) {
            // Auto-update on, but the server has no packaged jar to serve (e.g. dev host).
            chat(client, "§e[CustomBlocks] Server has v" + server + ", you have v" + own
                    + ". No download is available from this server — update manually.");
            return;
        }

        // K2 — older client, auto-update on, jar available → run the update flow.
        CustomBlocksMod.LOGGER.info("[CustomBlocks] §K: client v{} < server v{} — opening update screen.", own, server);
        client.setScreen(new UpdateScreen(server, own, p.downloadUrl(), p.sha256()));
    }

    private static String ownVersion() {
        return FabricLoader.getInstance()
                .getModContainer(CustomBlocksMod.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    private static void chat(MinecraftClient client, String msg) {
        if (client.player != null) client.player.sendMessage(Text.literal(msg), false);
    }
}
