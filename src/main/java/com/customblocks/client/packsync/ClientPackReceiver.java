/**
 * ClientPackReceiver.java
 *
 * Responsibility: On a MODDED client connected to a DEDICATED server, receive the server's pack manifest,
 * diff it against the loose pack on disk (resourcepacks/CustomBlocks), request only the missing/changed
 * files, stream each file to a {@code .part} temp, verify it, atomically commit it, and — once every
 * requested file is verified — do ONE silent resource reload and acknowledge application (Group 05 §G).
 *
 * §G — bounded, resumable, verified:
 *   • SESSION — every manifest opens a session id; PackFile/PackDone for any other id are ignored, so a
 *     superseded transfer (resolution change / new generation) can't write into the current one (§G5/§G12).
 *   • .part STREAMING — chunks append straight to a {@code .part} file (staging stays ~one chunk, well under
 *     16 MiB); completed files are committed with an atomic rename, so an interrupt leaves the previous pack
 *     intact and completed files aren't re-downloaded on reconnect (§G1/§G3/§G6).
 *   • FLOW CONTROL — every received chunk is acked so the server may send more, bounding in-flight bytes
 *     without a false timeout (§G4).
 *   • VERIFY + RETRY — each committed file's SHA-256 must match the manifest; a mismatch re-requests just
 *     that file, up to {@link PackSyncProtocol#MAX_FILE_RETRIES} (§G7).
 *   • BOUNDS + PATH SAFETY — chunk/size/count/path are validated before any write; nothing can land outside
 *     the pack root (§G8/§G9). A disk preflight rejects a transfer that wouldn't fit (§G14).
 *   • APPLY ACK — the server is told applied ONLY after the reload actually completes (§G10/§G11).
 *
 * Writes FILES ONLY — never SlotManager/TextureStore. All state is on the client thread (receivers hop via
 * client.execute). Resolution is server-authoritative (§F): the client writes exactly what it receives.
 *
 * Depends on: PackManifest, PackSyncProtocol, the Pack* payloads, MinecraftClient (reload),
 *             AnimFrameCache/StaticFrameCache (drop off-atlas frames after reload).
 * Called by:  CustomBlocksClient (manifest/file/done receivers + disconnect reset).
 */
package com.customblocks.client.packsync;

import com.customblocks.CustomBlocksMod;
import com.customblocks.network.packsync.PackManifest;
import com.customblocks.network.packsync.PackSyncProtocol;
import com.customblocks.network.payloads.PackAckPayload;
import com.customblocks.network.payloads.PackAppliedPayload;
import com.customblocks.network.payloads.PackRequestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class ClientPackReceiver {

    /** Options entry that enables our loose pack ("file/<folder name>") — same folder the host path uses. */
    private static final String PACK_ENTRY = "file/CustomBlocks";

    // Client-thread-only state for the current session:
    private static long activeSession = -1;                                     // manifest id we're serving
    private static Map<String, PackManifest.Entry> target = new LinkedHashMap<>(); // path -> expected sha+size
    private static Map<String, String> localCache;                              // path -> sha256 we currently have
    private static final Set<String> failed = new HashSet<>();                  // files that failed SHA this round
    private static final Map<String, Integer> retries = new HashMap<>();        // path -> re-request count
    private static Part current;                                                // the single in-progress .part
    private static boolean dirty;                                               // wrote or deleted something
    private static boolean aborted;                                             // fatal error — ignore rest, don't apply

    private static volatile String lastAppliedHash; // aggregate hash of the pack currently applied
    private static final AtomicBoolean reloadInFlight = new AtomicBoolean(false);
    private static volatile boolean reloadAgain = false;
    private static volatile String reloadAgainHash;
    private static volatile long reloadAgainSession;

    private ClientPackReceiver() {} // static-only

    /** One file being streamed to disk: its open {@code .part} stream + cursor, committed on the last chunk. */
    private static final class Part {
        final String path;
        final File dest;
        final File partFile;
        final OutputStream out;
        int nextIndex;
        long written;
        Part(String path, File dest, File partFile, OutputStream out) {
            this.path = path; this.dest = dest; this.partFile = partFile; this.out = out;
        }
    }

    private static File packRoot(MinecraftClient client) {
        return new File(client.runDirectory, "resourcepacks/CustomBlocks");
    }

    // ── manifest → diff → request ─────────────────────────────────────────────────

    /** Server opened a session — parse+diff its manifest, preflight disk, and ask for exactly what we lack. */
    public static void onManifest(MinecraftClient client, long session, byte[] gz) {
        client.execute(() -> {
            File root = packRoot(client);
            Map<String, PackManifest.Entry> entries;
            try {
                entries = PackManifest.parseEntries(gz); // bounded inflate + per-row validation (§G8/§G9)
            } catch (Exception bad) {
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Rejected malformed pack manifest: {}", bad.toString());
                return;
            }
            // A new session supersedes anything in flight — reset per-session state and drop abandoned parts.
            activeSession = session;
            closeCurrentDiscard();
            failed.clear();
            retries.clear();
            dirty = false;
            aborted = false;
            target = entries;
            sweepParts(root);

            Map<String, String> targetHashes = new LinkedHashMap<>();
            long totalBytes = 0;
            for (Map.Entry<String, PackManifest.Entry> e : entries.entrySet()) {
                targetHashes.put(e.getKey(), e.getValue().sha());
                totalBytes += e.getValue().size();
            }
            if (totalBytes > PackSyncProtocol.MAX_TOTAL_BYTES) {
                aborted = true;
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack manifest declares {} MB (> {} MB cap) — refusing.",
                        totalBytes / (1024 * 1024), PackSyncProtocol.MAX_TOTAL_BYTES / (1024 * 1024));
                SyncProgressOverlay.error("Server pack is too large to sync");
                return;
            }
            String targetHash = PackManifest.aggregateHash(targetHashes);

            if (targetHash.equals(lastAppliedHash)) {
                sendRequest(session, new ArrayList<>()); // identical to what we applied — server ends the round
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Server pack manifest unchanged ({}), nothing to sync.", targetHash);
                return;
            }
            if (localCache == null) localCache = new HashMap<>(PackManifest.hashFolder(root));

            List<String> needed = new ArrayList<>();
            long neededBytes = 0;
            for (Map.Entry<String, PackManifest.Entry> e : entries.entrySet()) {
                if (e.getValue().sha().equals(localCache.get(e.getKey()))) continue; // unchanged — skip
                needed.add(e.getKey());
                neededBytes += e.getValue().size();
            }

            if (!preflightDisk(root, neededBytes)) {
                aborted = true;
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Not enough free disk for a {} MB pack sync at {} — aborting before transfer.",
                        neededBytes / (1024 * 1024), root);
                SyncProgressOverlay.error("Not enough free disk for the texture sync");
                return; // §G14: reject before any bulk transfer, previous pack untouched
            }

            // §E12: show the join progress panel only when there is a real transfer (an exact-cache/no-op
            // join stays silent). The resumed offset = bytes already on disk we did NOT re-download (§G3).
            if (!needed.isEmpty()) SyncProgressOverlay.beginSync(neededBytes, totalBytes - neededBytes);

            sendRequest(session, needed);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack manifest (session {}): {} server files, requesting {} missing/changed ({} KB).",
                    session, entries.size(), needed.size(), neededBytes / 1024);
        });
    }

    /** True when the pack root's filesystem has room for {@code neededBytes} plus the safety margin. */
    private static boolean preflightDisk(File root, long neededBytes) {
        try {
            root.mkdirs();
            long usable = root.getUsableSpace(); // 0 = unknown → don't false-reject
            return usable <= 0 || usable >= neededBytes + PackSyncProtocol.DISK_MARGIN;
        } catch (Exception e) {
            return true; // can't tell → let the transfer try
        }
    }

    private static void sendRequest(long session, List<String> needed) {
        ClientPlayNetworking.send(new PackRequestPayload(session, PackManifest.gzipLines(needed)));
    }

    // ── file chunks → .part → verify → commit ─────────────────────────────────────

    /** One file chunk arrived — ack it for flow control, then append it to the file's {@code .part}. */
    public static void onFile(MinecraftClient client, long session, String path, int index, int count, byte[] data) {
        client.execute(() -> {
            if (session != activeSession || aborted || data == null) return; // stale/dead/empty-packet
            ack(session, data.length); // flow control counts wire bytes, regardless of what we do with them

            if (!PackSyncProtocol.safeRelPath(path)) return;                    // §G9
            PackManifest.Entry entry = target.get(path);
            if (entry == null) return;                                          // not a file we asked to track
            if (data.length > PackSyncProtocol.MAX_CHUNK_BYTES) { failFile(path, "oversized chunk"); return; } // §G8
            int wantCount = (int) Math.max(1L, (entry.size() + PackSyncProtocol.CHUNK_BYTES - 1) / PackSyncProtocol.CHUNK_BYTES);
            if (count != wantCount) { failFile(path, "chunk count " + count + " != " + wantCount); return; }
            if (index < 0 || index >= count) return;

            if (index == 0) startPart(client, path);                            // (re)open on the first chunk
            Part ps = current;
            if (ps == null || !ps.path.equals(path)) return;                    // couldn't open / different file
            if (index != ps.nextIndex) return;                                  // out-of-order or duplicate — drop
            if (ps.written + data.length > entry.size()) { failFile(path, "byte overflow"); return; } // §G8

            try {
                ps.out.write(data);
            } catch (Exception e) {
                fatal("disk write failed for " + path, e);                      // §G6
                return;
            }
            ps.written += data.length;
            ps.nextIndex++;
            SyncProgressOverlay.onChunk(data.length);                            // §E12 progress
            if (ps.nextIndex == count) commit(ps, entry);                       // last chunk → verify + commit
        });
    }

    /** Open a fresh {@code .part} for {@code path}, discarding any previous in-progress file. */
    private static void startPart(MinecraftClient client, String path) {
        closeCurrentDiscard();
        File dest = PackSyncProtocol.resolveInside(packRoot(client), path);     // §G9 — provably inside the root
        if (dest == null) { CustomBlocksMod.LOGGER.warn("[CustomBlocks] Refused unsafe pack path {}", path); return; }
        File part = new File(dest.getParentFile(), dest.getName() + PackManifest.PART_SUFFIX);
        try {
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            OutputStream out = new BufferedOutputStream(new java.io.FileOutputStream(part, false));
            current = new Part(path, dest, part, out);
        } catch (Exception e) {
            fatal("could not open " + part, e);                                 // §G6
        }
    }

    /** Last chunk landed — verify size + SHA-256, then atomically commit the {@code .part} or fail it (§G7). */
    private static void commit(Part ps, PackManifest.Entry entry) {
        try { ps.out.close(); } catch (Exception e) { fatal("close failed for " + ps.path, e); return; }
        String got;
        try { got = PackManifest.sha256File(ps.partFile); }
        catch (Exception e) { failFile(ps.path, "hash read failed"); return; }
        if (ps.written != entry.size() || !got.equalsIgnoreCase(entry.sha())) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Integrity check failed for {} (size {}/{}, sha {}) — will retry.",
                    ps.path, ps.written, entry.size(), got);
            deleteQuietly(ps.partFile);
            failed.add(ps.path);
            current = null;
            return;
        }
        try {
            commitAtomic(ps.partFile, ps.dest);
        } catch (Exception e) {
            fatal("commit (rename) failed for " + ps.path, e);                  // §G6
            return;
        }
        localCache.put(ps.path, got);
        failed.remove(ps.path);
        dirty = true;
        current = null;
    }

    private static void commitAtomic(File part, File dest) throws Exception {
        try {
            Files.move(part.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception atomicUnsupported) {
            Files.move(part.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ── done → retry or apply ─────────────────────────────────────────────────────

    /** Server finished a round — retry any failed file, else delete stale, reload, and ack application. */
    public static void onDone(MinecraftClient client, long session, String hash) {
        client.execute(() -> {
            if (session != activeSession || aborted) return;
            if (current != null) { deleteQuietly(current.partFile); failed.add(current.path); current = null; } // incomplete

            if (!failed.isEmpty()) {
                List<String> again = new ArrayList<>();
                int maxN = 0;
                for (String p : failed) {
                    int n = retries.merge(p, 1, Integer::sum);
                    if (n > PackSyncProtocol.MAX_FILE_RETRIES) {
                        aborted = true;
                        CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack file {} failed verification {} times — aborting; previous pack kept.", p, n);
                        SyncProgressOverlay.error("Textures kept failing the integrity check");
                        sendApplied(session, hash, false);                       // §G7/§G11 — not applied
                        return;
                    }
                    again.add(p);
                    maxN = Math.max(maxN, n);
                }
                failed.clear();
                SyncProgressOverlay.onRetryRound(maxN);                           // §E12 — surface retry/resume
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Re-requesting {} pack file(s) that failed verification.", again.size());
                sendRequest(session, again);                                     // same session, another round
                return;
            }

            // Every requested file verified — remove files the manifest no longer lists.
            for (String path : new ArrayList<>(localCache.keySet())) {
                if (!target.containsKey(path)) {
                    File f = PackSyncProtocol.resolveInside(packRoot(client), path);
                    if (f != null && f.delete()) dirty = true;
                    localCache.remove(path);
                }
            }
            if (!dirty) {
                lastAppliedHash = hash;
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack already current (hash {}), no reload.", hash);
                SyncProgressOverlay.done();                                       // no-op unless a panel was showing
                sendApplied(session, hash, true);                                // §G10 — confirm even a no-op
                return;
            }
            SyncProgressOverlay.applying();                                       // §E12 — bytes in, reload running
            applyReload(client, hash, session);                                  // real change → one reload
        });
    }

    /** Enable the pack if needed, then one guarded silent reload; ack the result only when it completes. */
    private static void applyReload(MinecraftClient client, String hash, long session) {
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
                        if (!client.options.resourcePacks.contains(PACK_ENTRY)) {
                            // Minecraft dropped our pack on reload (e.g. the atlas still overflows on a weak GPU).
                            // Resolution is server-authoritative now — the owner sets a smaller /cblowres. We can
                            // only report it; there is no client-side re-shrink to fall back on. Not applied (§G11).
                            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Reload dropped the CustomBlocks pack — the client could not load it.");
                            SyncProgressOverlay.error("Client couldn't load the pack (ask an op to lower /cblowres)");
                            sendApplied(session, hash, false);
                            return;
                        }
                        lastAppliedHash = hash;
                        CustomBlocksMod.LOGGER.info("[CustomBlocks] Synced pack applied (hash {}).", hash);
                        SyncProgressOverlay.done();                              // §E12
                        sendApplied(session, hash, true);                        // §G10
                        if (reloadAgain) {
                            reloadAgain = false;
                            applyReload(client, reloadAgainHash, reloadAgainSession);
                        }
                    }))
                    .exceptionally(ex -> {
                        client.execute(() -> {
                            reloadInFlight.set(false);
                            CustomBlocksMod.LOGGER.error("[CustomBlocks] Synced pack reload failed.", ex);
                            SyncProgressOverlay.error("Applying the textures failed");
                            sendApplied(session, hash, false);                   // §G11
                        });
                        return null;
                    });
        } else {
            reloadAgain = true;
            reloadAgainHash = hash;
            reloadAgainSession = session;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private static void ack(long session, int bytes) {
        if (bytes > 0) ClientPlayNetworking.send(new PackAckPayload(session, bytes));
    }

    private static void sendApplied(long session, String hash, boolean ok) {
        try { ClientPlayNetworking.send(new PackAppliedPayload(session, hash, ok)); }
        catch (Exception ignored) { /* connection already gone — server cleans up on disconnect */ }
    }

    /** A file arrived wrong — drop its {@code .part} and mark it for a bounded retry at Done (§G7). */
    private static void failFile(String path, String why) {
        if (current != null && current.path.equals(path)) closeCurrentDiscard();
        failed.add(path);
        CustomBlocksMod.LOGGER.warn("[CustomBlocks] Pack file {} rejected ({}) — will retry.", path, why);
    }

    /** An unrecoverable local error — stop touching disk, keep the previous pack, don't report applied (§G6). */
    private static void fatal(String why, Throwable e) {
        aborted = true;
        closeCurrentDiscard();
        SyncProgressOverlay.error("Couldn't write textures to disk");
        CustomBlocksMod.LOGGER.error("[CustomBlocks] Pack sync aborted: {} — previous pack kept.", why, e);
    }

    /** Close the in-progress stream and delete its partial {@code .part} (nothing half-written survives). */
    private static void closeCurrentDiscard() {
        if (current == null) return;
        try { current.out.close(); } catch (Exception ignored) {}
        deleteQuietly(current.partFile);
        current = null;
    }

    private static void deleteQuietly(File f) {
        try { if (f != null) Files.deleteIfExists(f.toPath()); } catch (Exception ignored) {}
    }

    /** Delete every abandoned {@code .part} under the pack root (interrupted prior session, §G3 cleanup). */
    private static void sweepParts(File root) {
        if (root == null || !root.isDirectory()) return;
        sweepWalk(root);
    }

    private static void sweepWalk(File dir) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File f : kids) {
            if (f.isDirectory()) sweepWalk(f);
            else if (f.getName().endsWith(PackManifest.PART_SUFFIX)) deleteQuietly(f);
        }
    }

    /** Reset per-connection state on disconnect so the next server starts a clean diff (§G12). */
    public static void reset() {
        closeCurrentDiscard();
        activeSession = -1;
        target = new LinkedHashMap<>();
        localCache = null;
        failed.clear();
        retries.clear();
        dirty = false;
        aborted = false;
        lastAppliedHash = null;
        SyncProgressOverlay.hide(); // §E12 — a lost connection shows the vanilla screen, not a stuck panel
    }
}
