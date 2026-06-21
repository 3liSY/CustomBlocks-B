/**
 * Pack-sync layer (Group 05, remote/dedicated fix). On a DEDICATED server a modded client
 * can't build the pack from its own local slot data, so the server ships the pack as a folder
 * of files: PackManifest snapshots the pack (path -> bytes -> sha1) from
 * {@link com.customblocks.network.ServerPackGenerator#emit}; PackSyncService diffs against a
 * client's manifest, queues only the missing/changed files, and streams them throttled.
 *
 * Server-side only. The matching client buffer lives in com.customblocks.client.packsync.
 * The integrated-host path (RegenPackPayload local regen) is untouched and never uses this.
 */
package com.customblocks.network.packsync;
