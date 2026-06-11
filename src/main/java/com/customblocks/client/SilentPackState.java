/**
 * SilentPackState.java
 *
 * Responsibility: Client-side flag for the silent-pack auto-accept (Group 05). Read by
 * the ClientCommonNetworkHandler mixin to decide whether to swallow the vanilla
 * resource-pack prompt.
 *
 * Default is FALSE (vanilla prompt) on purpose: the client only goes silent after OUR
 * server sends a SilentPackPayload(true). On disconnect it resets to false, so joining a
 * different server never auto-accepts that server's packs.
 *
 * Depends on: nothing
 * Called by: CustomBlocksClient (sets), ClientCommonNetworkHandlerMixin (reads)
 */
package com.customblocks.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SilentPackState {

    private static volatile boolean silent = false;

    private SilentPackState() {} // static-only

    public static boolean isSilent() { return silent; }

    public static void set(boolean value) { silent = value; }
}
