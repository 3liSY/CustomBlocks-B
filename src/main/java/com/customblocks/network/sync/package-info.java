/**
 * Sync helpers. TextureQueue debounces rapid block changes (e.g. bulk imports)
 * into a single pack rebuild after a short window, preventing clients from being
 * flooded with redundant resource-pack downloads. Keep the debounce window short
 * (~500ms) — a long window causes seconds of purple blocks (§9.6).
 */
package com.customblocks.network.sync;
