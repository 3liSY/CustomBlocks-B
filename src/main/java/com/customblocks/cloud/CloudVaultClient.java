/**
 * CloudVaultClient.java
 *
 * Responsibility: Upload/download block definitions to/from the Cloudflare Block Vault.
 * Configured via CustomBlocksConfig.vaultEndpoint.
 *
 * Phase 14 stub — methods return null/false until the Cloudflare Worker is set up
 * and vaultEndpoint is filled in config.json.
 *
 * Depends on: CustomBlocksConfig, SlotData
 * Called by: CloudCommands
 */
package com.customblocks.cloud;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.SlotData;

public final class CloudVaultClient {

    private CloudVaultClient() {}

    /** Upload a block definition. Returns a share code, or null on failure. */
    public static String upload(SlotData data, byte[] texture) {
        if (!isConfigured()) return null;
        // TODO Phase 14: POST to vaultEndpoint/upload with JSON + texture
        return null;
    }

    /** Download a block definition by share code. Returns the JSON string, or null. */
    public static String download(String shareCode) {
        if (!isConfigured()) return null;
        // TODO Phase 14: GET vaultEndpoint/block/<shareCode>
        return null;
    }

    public static boolean isConfigured() {
        return !CustomBlocksConfig.vaultEndpoint.isEmpty();
    }
}
