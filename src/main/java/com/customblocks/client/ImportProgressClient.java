/**
 * ImportProgressClient.java — CLIENT-ONLY. Group 12 §B rule 18.
 *
 * Receives {@link ImportProgressPayload} and pushes it into the shared top-center panel, so a folder
 * import reports itself with the same look a pack sync does — the owner's call, over inventing a second
 * progress widget.
 *
 * WHY IT IS ITS OWN ENTRYPOINT: {@code CustomBlocksClient} sits exactly on the §9.3 500-line cap, so any
 * line added there fails the build. Rather than move somebody else's receivers around it — or refactor the
 * pack-sync client path this reports through, which the G12 handoff rules out — Group 12 registers its one
 * receiver from here, listed as a second "client" entrypoint in fabric.mod.json. Nothing existing moved.
 *
 * Depends on: ImportProgressPayload, SyncProgressOverlay (G05's panel)
 * Called by:  Fabric loader (client entrypoint)
 */
package com.customblocks.client;

import com.customblocks.client.packsync.SyncProgressOverlay;
import com.customblocks.network.payloads.ImportProgressPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class ImportProgressClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The render hop matters: the payload lands on a netty thread, and the overlay's state is read by
        // the HUD on the client main thread.
        ClientPlayNetworking.registerGlobalReceiver(ImportProgressPayload.ID, (payload, context) ->
                context.client().execute(() -> SyncProgressOverlay.importProgress(
                        payload.done(), payload.total(), payload.label())));

        // Leaving a server must not strand an import panel on screen for the next one.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SyncProgressOverlay.hide());
    }
}
