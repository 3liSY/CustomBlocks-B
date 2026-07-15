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
 * G05-5 (per-client low-res): when the weak-GPU friend has picked a size for this server, each synced
 * texture PNG is downscaled on a worker thread before it is written (keeps the render thread smooth),
 * the diff is taken against the SERVER's 512 fingerprints (via LowResState's sidecar) so shrunk files
 * don't re-download every join, and if a 256px reload STILL drops the pack (atlas overflow) it auto
 * steps down to 128px once. Off / capable clients take the untouched full-size path.
 *
 * Depends on: PackManifest (diff + framing), the four Pack* payloads, MinecraftClient (reload),
 *             LowResState + LowResScaler (G05-5), AnimFrameCache (drop off-atlas anim frames after reload).
 * Called by:  CustomBlocksClient (manifest / file / done receivers + disconnect reset),
 *             LowResClientCommand (applyLowRes).
 */
package com.customblocks.client.packsync;

import com.customblocks.CustomBlocksMod;
import com.customblocks.client.lowres.LowResScaler;
import com.customblocks.client.lowres.LowResState;
import com.customblocks.network.packsync.PackManifest;
import com.customblocks.network.payloads.PackRequestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Environment(EnvType.CLIENT)
public final class ClientPackReceiver {

    /** Options entry that enables our loose pack ("file/<folder name>") — same folder the host path uses. */
    private static final String PACK_ENTRY = "file/CustomBlocks";

    // Client-thread-only state for the current sync:
    private static Map<String, String> targetManifest = new LinkedHashMap<>(); // path -> sha1 the server wants
    private static Map<String, String> localCache;                              // path -> sha1 we currently "have"
    private static final Map<String, byte[][]> chunkBufs = new HashMap<>();      // path -> chunk slots being filled
    private static boolean dirty;                                                // wrote or deleted something this sync

    private static volatile String lastAppliedHash; // aggregate hash of the pack currently applied
    private static final AtomicBoolean reloadInFlight = new AtomicBoolean(false);
    private static volatile boolean reloadAgain = false;
    private static volatile String reloadAgainHash;

    // ── G05-5 low-res state ──────────────────────────────────────────────────
    private static volatile boolean lowActive = false;     // shrink the textures this sync?
    private static volatile int     lowSize   = LowResState.FULL;
    private static volatile String  syncServer;            // address this sync belongs to
    private static volatile boolean trackFolder = false;   // update LowResState folder context on apply?
    private static volatile boolean stepped = false;        // already auto-stepped 256 -> 128 this attempt?
    private static final AtomicInteger pendingWrites = new AtomicInteger(0);
    private static volatile boolean donePendingFinalize = false;
    private static volatile String  donePendingHash;
    private static ExecutorService shrinkExec;

    private ClientPackReceiver() {} // static-only

    private static File packRoot(MinecraftClient client) {
        return new File(client.runDirectory, "resourcepacks/CustomBlocks");
    }

    private static ExecutorService shrink() {
        if (shrinkExec == null) shrinkExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CustomBlocks-LowRes");
            t.setDaemon(true);
            return t;
        });
        return shrinkExec;
    }

    /** Server sent its manifest — diff against disk and ask for exactly what we lack. */
    public static void onManifest(MinecraftClient client, byte[] gz) {
        client.execute(() -> {
            targetManifest = PackManifest.parseManifest(gz);
            String targetHash = PackManifest.aggregateHash(targetManifest);

            // Resolve this client's low-res choice for THIS server (owner: "this server only").
            syncServer  = LowResState.currentServerKey(client);
            int desired = LowResState.sizeFor(syncServer);
            lowActive   = syncServer != null && desired < LowResState.FULL;
            lowSize     = desired;
            boolean folderLow = LowResState.lastFolderSize() < LowResState.FULL;
            trackFolder = lowActive || folderLow;   // only touch LowResState when low-res is in play
            stepped     = false;

            if (targetHash.equals(lastAppliedHash)) {
                // Identical to what we already applied this session — request nothing, no reload.
                send(client, new ArrayList<>());
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Server pack manifest unchanged ({}), nothing to sync.", targetHash);
                return;
            }

            boolean fullPull;
            if (!trackFolder) {
                // Proven full-size path (unchanged): diff the server manifest against the on-disk bytes.
                if (localCache == null) localCache = new HashMap<>(PackManifest.hashFolder(packRoot(client)));
                fullPull = false;
            } else {
                // Low-res in play now (or the folder is currently shrunk). Diff against the SERVER shas we
                // last applied for this exact (server, size); anything else can't be trusted → full re-pull.
                boolean sameCtx = syncServer != null
                        && syncServer.equals(LowResState.lastFolderServer())
                        && LowResState.lastFolderSize() == desired;
                localCache = sameCtx ? new HashMap<>(LowResState.sidecar()) : new HashMap<>();
                fullPull = !sameCtx;
            }

            chunkBufs.clear();
            dirty = false;
            pendingWrites.set(0);
            donePendingFinalize = false;
            List<String> needed = new ArrayList<>();
            for (Map.Entry<String, String> e : targetManifest.entrySet()) {
                if (fullPull || !e.getValue().equals(localCache.get(e.getKey()))) needed.add(e.getKey());
            }
            send(client, needed);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack manifest: {} server files, low={}, requesting {} missing/changed.",
                    targetManifest.size(), lowActive ? lowSize + "px" : "off", needed.size());
        });
    }

    private static void send(MinecraftClient client, List<String> needed) {
        ClientPlayNetworking.send(new PackRequestPayload(PackManifest.gzipLines(needed)));
    }

    /** One file chunk arrived — buffer it; when the last chunk lands, write (and shrink if low-res). */
    public static void onFile(MinecraftClient client, String path, int index, int count, byte[] data) {
        client.execute(() -> {
            int total = Math.max(1, count);
            byte[][] slots = chunkBufs.computeIfAbsent(path, p -> new byte[total][]);
            if (index < 0 || index >= slots.length) return; // malformed — ignore
            slots[index] = data;
            for (byte[] s : slots) if (s == null) return;  // still awaiting chunks
            chunkBufs.remove(path);

            if (lowActive) {
                // Decode + downscale + write on a worker so the render thread never blocks; commit the
                // (client-thread) cache state back on the client thread when the write finishes.
                final int size = lowSize;
                final String serverSha = targetManifest.get(path);
                pendingWrites.incrementAndGet();
                shrink().submit(() -> {
                    byte[] bytes = assemble(slots);
                    byte[] out = LowResScaler.isShrinkable(path) ? LowResScaler.shrink(bytes, size) : bytes;
                    diskWrite(client, path, out);
                    client.execute(() -> {
                        if (serverSha != null) localCache.put(path, serverSha); // diff basis = server 512 sha
                        dirty = true;
                        pendingWrites.decrementAndGet();
                        maybeFinalize(client);
                    });
                });
            } else {
                byte[] bytes = assemble(slots);
                diskWrite(client, path, bytes);
                localCache.put(path, PackManifest.sha1Hex(bytes));
                dirty = true;
            }
        });
    }

    /** Concatenate the buffered chunk slots into one byte[]. */
    private static byte[] assemble(byte[][] slots) {
        int len = 0;
        for (byte[] s : slots) len += (s == null ? 0 : s.length);
        byte[] full = new byte[len];
        int off = 0;
        for (byte[] s : slots) {
            if (s == null) continue;
            System.arraycopy(s, 0, full, off, s.length);
            off += s.length;
        }
        return full;
    }

    private static void diskWrite(MinecraftClient client, String path, byte[] bytes) {
        try {
            File dest = new File(packRoot(client), path);
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(dest.toPath(), bytes);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to write synced pack file {}", path, e);
        }
    }

    /** Server signalled the stream is complete — wait for any low-res writes, then finalise. */
    public static void onDone(MinecraftClient client, String hash) {
        client.execute(() -> {
            if (lowActive && pendingWrites.get() > 0) { // shrink workers still running — finalise when drained
                donePendingFinalize = true;
                donePendingHash = hash;
                return;
            }
            finalizeDone(client, hash);
        });
    }

    /** Called on the client thread whenever a low-res write completes; finalise once all have drained. */
    private static void maybeFinalize(MinecraftClient client) {
        if (donePendingFinalize && pendingWrites.get() == 0) {
            donePendingFinalize = false;
            finalizeDone(client, donePendingHash);
        }
    }

    private static void finalizeDone(MinecraftClient client, String hash) {
        // Remove any local file the server's manifest no longer lists (deleted/renamed slot).
        for (String path : new ArrayList<>(localCache.keySet())) {
            if (!targetManifest.containsKey(path)) {
                File f = new File(packRoot(client), path);
                if (f.delete()) dirty = true;
                localCache.remove(path);
            }
        }
        if (!dirty) {
            lastAppliedHash = hash;
            if (trackFolder && syncServer != null)
                LowResState.commitFolder(syncServer, lowActive ? lowSize : LowResState.FULL, localCache);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack already current (hash {}), no reload.", hash);
            return;
        }
        applyReload(client, hash);
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
                        com.customblocks.client.render.AnimFrameCache.clear();
                        com.customblocks.client.render.StaticFrameCache.clear();
                        // G05-5: a weak GPU that still can't fit the atlas makes MC drop our pack on reload.
                        if (lowActive && !client.options.resourcePacks.contains(PACK_ENTRY)) {
                            CustomBlocksMod.LOGGER.warn("[CustomBlocks] low-res {}px reload dropped the pack (atlas still overflows).", lowSize);
                            handleLowFail(client, lowSize);
                            return;
                        }
                        lastAppliedHash = hash;
                        if (trackFolder && syncServer != null)
                            LowResState.commitFolder(syncServer, lowActive ? lowSize : LowResState.FULL, localCache);
                        if (lowActive) chat(client, "§blow-res " + lowSize + "px applied — blocks should render now.");
                        CustomBlocksMod.LOGGER.info("[CustomBlocks] Synced pack applied (hash {}, low={}).",
                                hash, lowActive ? lowSize + "px" : "off");
                        if (reloadAgain) {
                            reloadAgain = false;
                            applyReload(client, reloadAgainHash);
                        }
                    }))
                    .exceptionally(ex -> {
                        client.execute(() -> {
                            reloadInFlight.set(false);
                            if (lowActive) {
                                CustomBlocksMod.LOGGER.error("[CustomBlocks] low-res {}px reload failed.", lowSize, ex);
                                handleLowFail(client, lowSize);
                            } else {
                                CustomBlocksMod.LOGGER.error("[CustomBlocks] Synced pack reload failed.", ex);
                            }
                        });
                        return null;
                    });
        } else {
            reloadAgain = true;
            reloadAgainHash = hash;
        }
    }

    /** A low-res reload still couldn't fit the atlas — auto step 256 → 128 once, else report exhausted. */
    private static void handleLowFail(MinecraftClient client, int failedSize) {
        if (failedSize > 128 && !stepped) {
            stepped = true;
            chat(client, "§elow-res " + failedSize + "px still too big for this GPU — trying 128px…");
            LowResState.setSize(syncServer, 128);
            forceFullResync(client, 128);
        } else {
            chat(client, "§clow-res 128px still failed — this GPU can't load the CustomBlocks pack.");
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] low-res exhausted (128px failed) — GPU atlas limit.");
        }
    }

    /**
     * Command entrypoint (/cblowres). Re-pull the whole pack from the server at {@code size} (shrinking
     * as it writes when size &lt; FULL). Returns false if no manifest has arrived from this server yet.
     */
    public static boolean applyLowRes(MinecraftClient client, int size) {
        if (targetManifest.isEmpty()) return false;
        client.execute(() -> {
            stepped = false;                 // fresh manual attempt — allow one auto step-down again
            forceFullResync(client, size);
        });
        return true;
    }

    /** Request every file in the current manifest again, applying {@code size} as it writes. */
    private static void forceFullResync(MinecraftClient client, int size) {
        lowActive   = size < LowResState.FULL;
        lowSize     = size;
        syncServer  = LowResState.currentServerKey(client);
        trackFolder = true;                  // a forced re-pull always records the new folder context
        localCache  = new HashMap<>();
        chunkBufs.clear();
        dirty = false;
        pendingWrites.set(0);
        donePendingFinalize = false;
        List<String> all = new ArrayList<>(targetManifest.keySet());
        send(client, all);
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Forcing full re-pull at {} ({} files).",
                lowActive ? size + "px" : "off", all.size());
    }

    private static void chat(MinecraftClient client, String msg) {
        if (client.player != null) client.player.sendMessage(Text.literal("[CustomBlocks] ").append(Text.literal(msg)), false);
    }

    /** Reset per-connection state on disconnect so the next server starts a clean diff. */
    public static void reset() {
        targetManifest = new LinkedHashMap<>();
        localCache = null;
        chunkBufs.clear();
        dirty = false;
        lastAppliedHash = null;
        lowActive = false;
        lowSize = LowResState.FULL;
        syncServer = null;
        trackFolder = false;
        stepped = false;
        pendingWrites.set(0);
        donePendingFinalize = false;
        donePendingHash = null;
    }
}
