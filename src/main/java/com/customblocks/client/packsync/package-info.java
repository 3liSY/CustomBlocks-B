/**
 * Client side of the dedicated-server pack sync (Group 05, remote/dedicated fix). ClientPackReceiver
 * takes the server's manifest, diffs it against the loose pack on disk, requests only the
 * missing/changed files, buffers the streamed chunks, writes them, removes stale files, and does
 * one silent resource reload. It ONLY writes files — it never touches SlotManager/TextureStore, so a
 * host's local data can't be clobbered. Active only when connected to a dedicated server; the
 * integrated-host path (com.customblocks.client.ResourcePackGenerator) is untouched.
 */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
package com.customblocks.client.packsync;
