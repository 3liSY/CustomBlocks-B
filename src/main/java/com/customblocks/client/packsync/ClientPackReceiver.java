/**
 * ClientPackReceiver.java
 *
 * Responsibility: On a MODDED client connected to a DEDICATED server, receive the server's pack
 * manifest, diff it against the loose pack already on disk (resourcepacks/CustomBlocks), request
 * only the files that are missing or changed, buffer the streamed chunks, write them, delete any
 * stale local file the manifest no longer lists, and do ONE silent resource reload (Group 05,
 * remote/dedicated fix — fixes server-created blocks rendering magenta).
 *
 * Writes FILES ONLY — never SlotManager/TextureStore — so a host's own local data is never touched.
 * All state is handled on the client thread (network receivers hop via client.execute); the per-tick
 * server throttle keeps each batch of writes tiny, so there is no render hitch.
 *
 * Depends on: PackManifest (diff + framing), the four Pack* payloads, MinecraftClient (reload),
 *             AnimFrameCache (drop off-atlas anim frames after a reload).
 * Called by:  CustomBlocksClient (manifest / file / done receivers + disconnect reset).
 */
package com.customblocks.client.packsync;

import com.customblocks.CustomBlocksMod;
import com.customblocks.network.packsync.PackManifest;
import com.customblocks.network.payloads.PackRequestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class ClientPackReceiver {

    /** Options entry that enables our loose pack ("file/<folder name>") — same folder the host path uses. */
    private static final String PACK_ENTRY = "file/CustomBlocks";

    // Client-thread-only state for the current sync:
    private static Map<String, String> targetManifest = new LinkedHashMap<>(); // path -> sha1 the server wants
    private static Map<String, String> localCache;                              // path -> sha1 we currently have on disk
    private static final Map<String, byte[][]> chunkBufs = new HashMap<>();      // path -> chunk slots being filled
    private static boolean dirty;                                                // wrote or deleted something this sync

    private static volatile String lastAppliedHash; // aggregate hash of the pack currently applied
    private static final AtomicBoolean reloadInFlight = new AtomicBoolean(false);
    private static volatile boolean reloadAgain = false;
    private static volatile String reloadAgainHash;

    private ClientPackReceiver() {} // static-only

    private static File packRoot(MinecraftClient client) {
        return new File(client.runDirectory, "resourcepacks/CustomBlocks");
    }

    /** Server sent its manifest — diff against disk and ask for exactly what we lack. */
    public static void onManifest(MinecraftClient client, byte[] gz) {
        client.execute(() -> {
            targetManifest = PackManifest.parseManifest(gz);
            String targetHash = PackManifest.aggregateHash(targetManifest);
            if (targetHash.equals(lastAppliedHash)) {
                // Identical to what we already applied this session (e.g. a change that didn't touch
                // our files, or a duplicate manifest) — request nothing, no reload.
                send(client, new ArrayList<>());
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Server pack manifest unchanged ({}), nothing to sync.", targetHash);
                return;
            }
            if (localCache == null) localCache = new HashMap<>(PackManifest.hashFolder(packRoot(client)));
            chunkBufs.clear();
            dirty = false;
            List<String> needed = new ArrayList<>();
            for (Map.Entry<String, String> e : targetManifest.entrySet()) {
                if (!e.getValue().equals(localCache.get(e.getKey()))) needed.add(e.getKey());
            }
            send(client, needed);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack manifest: {} server files, requesting {} missing/changed.",
                    targetManifest.size(), needed.size());
        });
    }

    private static void send(MinecraftClient client, List<String> needed) {
        ClientPlayNetworking.send(new PackRequestPayload(PackManifest.gzipLines(needed)));
    }

    /** One file chunk arrived — buffer it; when the last chunk lands, write the file. */
    public static void onFile(MinecraftClient client, String path, int index, int count, byte[] data) {
        client.execute(() -> {
            int total = Math.max(1, count);
            byte[][] slots = chunkBufs.computeIfAbsent(path, p -> new byte[total][]);
            if (index < 0 || index >= slots.length) return; // malformed — ignore
            slots[index] = data;
            for (byte[] s : slots) if (s == null) return; // still awaiting chunks
            writeFile(client, path, slots);
            chunkBufs.remove(path);
        });
    }

    private static void writeFile(MinecraftClient client, String path, byte[][] slots) {
        int len = 0;
        for (byte[] s : slots) len += (s == null ? 0 : s.length);
        byte[] full = new byte[len];
        int off = 0;
        for (byte[] s : slots) {
            if (s == null) continue;
            System.arraycopy(s, 0, full, off, s.length);
            off += s.length;
        }
        try {
            File dest = new File(packRoot(client), path);
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(dest.toPath(), full);
            localCache.put(path, PackManifest.sha1Hex(full));
            dirty = true;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to write synced pack file {}", path, e);
        }
    }

    /** Server signalled the stream is complete — drop stale files, then one silent reload. */
    public static void onDone(MinecraftClient client, String hash) {
        client.execute(() -> {
            // Remove any local file the server's manifest no longer lists (deleted/renamed slot).
            for (String path : new ArrayList<>(localCache.keySet())) {
                if (!targetManifest.containsKey(path)) {
                    File f = new File(packRoot(client), path);
                    if (f.delete()) { dirty = true; }
                    localCache.remove(path);
                }
            }
            if (!dirty) {
                lastAppliedHash = hash;
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack already current (hash {}), no reload.", hash);
                return;
            }
            applyReload(client, hash);
        });
    }

    /** Enable the pack if needed, then one guarded silent reload; coalesce a request mid-reload. */
    private static void applyReload(MinecraftClient client, String hash) {
        if (!client.options.resourcePacks.contains(PACK_ENTRY)) {
            client.options.resourcePacks.add(PACK_ENTRY);
            client.options.write();
        }
        client.getResourcePackManager().scanPacks();
        if (reloadInFlight.compareAndSet(false, true)) {
            client.reloadResources()
                    .thenRun(() -> client.execute(() -> {
                        reloadInFlight.set(false);
                        lastAppliedHash = hash;
                        com.customblocks.client.render.AnimFrameCache.clear();
                        com.customblocks.client.render.StaticFrameCache.clear();
                        CustomBlocksMod.LOGGER.info("[CustomBlocks] Synced pack applied (hash {}).", hash);
                        if (reloadAgain) {
                            reloadAgain = false;
                            applyReload(client, reloadAgainHash);
                        }
                    }))
                    .exceptionally(ex -> {
                        client.execute(() -> reloadInFlight.set(false));
                        CustomBlocksMod.LOGGER.error("[CustomBlocks] Synced pack reload failed.", ex);
                        return null;
                    });
        } else {
            reloadAgain = true;
            reloadAgainHash = hash;
        }
    }

    /** Reset per-connection state on disconnect so the next server starts a clean diff. */
    public static void reset() {
        targetManifest = new LinkedHashMap<>();
        localCache = null;
        chunkBufs.clear();
        dirty = false;
        lastAppliedHash = null;
    }
}
