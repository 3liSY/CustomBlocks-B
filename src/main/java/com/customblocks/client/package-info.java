/**
 * Client-side package. CustomBlocksClient is the ClientModInitializer entrypoint.
 * Also holds the client-side ResourcePackGenerator (modded clients generate
 * textures locally instead of downloading the HTTP pack) and HudConfig.
 *
 * Design rule: client code never mutates server state — the server is always
 * authoritative; client-side prediction only.
 */
package com.customblocks.client;
