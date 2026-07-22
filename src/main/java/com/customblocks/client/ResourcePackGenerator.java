/**
 * ResourcePackGenerator.java
 *
 * Responsibility: On a MODDED client, write the resource pack as LOOSE files into
 * {@code <runDir>/resourcepacks/CustomBlocks/} and silently reload, instead of downloading the
 * HTTP pack (Group 05 fix, 2026-06-15 — the integrated single-player server's HTTP push is
 * ignored by a modded client, so the host must generate the pack locally).
 *
 * The file contents come straight from {@link ServerPackGenerator#emit} — the SAME logic the
 * HTTP pack uses — so the local pack can never drift from it. This works for single-player /
 * host, where the client JVM holds the live SlotManager + TextureStore data. (Remote modded
 * friends, whose client JVM has no slot data, are a later step.)
 *
 * Depends on: ServerPackGenerator (emit), MinecraftClient (runDirectory, reloadResources)
 * Called by: CustomBlocksClient (RegenPackPayload receiver)
 */
package com.customblocks.client;

import com.customblocks.CustomBlocksMod;
import com.customblocks.network.ServerPackGenerator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class ResourcePackGenerator {

    /** Options entry that enables our loose pack ("file/<folder name>"). */
    private static final String PACK_ENTRY = "file/CustomBlocks";

    /** Serializes the loose-file write pass so two burst regens can't interleave their writes +
     *  deleteStale on the same folder (SP corrupt-PNG fix, 2026-07-03). */
    private static final Object WRITE_LOCK = new Object();

    /**
     * Serializes the ENTIRE write→apply→reload cycle of one regen, not just the reload (SP corrupt-PNG fix,
     * 2026-07-19). A new regen must NOT run {@link #writeLoosePack} (rewriting the loose folder) while a
     * previous regen's {@code reloadResources()} is still READING that folder — that overlap is the surviving
     * B3 race: {@link #writeAtomic} makes each file swap atomic, but a reload mid-folder-rewrite still reads a
     * {@code slot_N.png} that is being replaced/deleted, which is what surfaced as "Corrupt PNG" on a fast
     * create burst. The old gate sat inside {@link #applyReload} — AFTER the write had already happened — so
     * it only stopped stacked reloads, not the write↔read overlap. While an op holds this flag a fresh request
     * is coalesced into {@link #pendingHash} and re-run by {@link #finishOp} once the op (including its async
     * reload) is fully done, so a write and a reload-read can never run at the same time.
     */
    private static final AtomicBoolean opInFlight = new AtomicBoolean(false);
    private static volatile String pendingHash;       // latest regen requested while an op was running
    private static volatile boolean hasPending = false;
    private static volatile String lastAppliedHash;   // hash of the pack currently applied THIS session

    private ResourcePackGenerator() {} // static-only

    /**
     * (Re)generate the local pack for {@code hash} and silently reload. Reloads are mandatory now, so this
     * only short-circuits on the exact hash we already applied earlier in THIS session (a duplicate payload) —
     * it never carries an applied-hash across a restart or world open. File I/O runs off-thread; the reload is
     * scheduled back on the client thread.
     */
    public static void regenerate(MinecraftClient client, String hash) {
        if (client == null) return;
        // Intra-session dedup only: a repeated identical regen in the same session must not restart the
        // write→reload cycle (that would reload-storm on a burst). A fresh JVM / world open starts with
        // lastAppliedHash == null, so the first regen of any session always writes + reloads.
        if (hash != null && hash.equals(lastAppliedHash)) {
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack already current this session (hash {}), skipping regen.", hash);
            return;
        }
        // Serialize the whole write→reload cycle: if an op is already running, do NOT start a second
        // writeLoosePack that would rewrite the loose folder while the first op's reload is still reading it
        // (the SP corrupt-PNG race). Remember the latest hash instead; finishOp re-runs it once the op ends.
        if (!opInFlight.compareAndSet(false, true)) {
            pendingHash = hash;
            hasPending = true;
            return;
        }
        Thread t = new Thread(() -> {
            try {
                boolean anyChange = writeLoosePack(new File(client.runDirectory, "resourcepacks/CustomBlocks"));
                client.execute(() -> applyReload(client, hash, anyChange));
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to write local resource pack", e);
                client.execute(() -> finishOp(client)); // release the gate + run any coalesced regen
            }
        }, "CustomBlocks-ClientPackGen");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Write every emitted pack file loose under {@code packRoot}, then delete files left behind — but ONLY
     * rewrite a file whose bytes actually changed. Returns true when anything changed on disk (a file was
     * written or a stale file deleted) → {@link #applyReload} must reload; false when disk already matched the
     * emit exactly → nothing to apply. Held under {@link #WRITE_LOCK} so two burst regens run
     * one-after-another instead of racing each other's writes/deleteStale; each changed file lands via
     * {@link #writeAtomic} so a concurrent reloadResources() never reads a half-written PNG (SP corrupt-PNG
     * fix, 2026-07-03).
     */
    private static boolean writeLoosePack(File packRoot) throws Exception {
        synchronized (WRITE_LOCK) {
            Set<String> written = new HashSet<>();
            boolean[] anyChange = {false};
            ServerPackGenerator.emit((path, data) -> {
                String rel = path.replace('\\', '/');
                File dest = new File(packRoot, rel);
                File parent = dest.getParentFile();
                if (parent != null) parent.mkdirs();
                byte[] old = readOrNull(dest);
                boolean changed = old == null || !Arrays.equals(old, data);
                if (changed) { writeAtomic(dest, data); anyChange[0] = true; }
                written.add(rel);
            });
            if (deleteStale(packRoot, packRoot, written)) anyChange[0] = true;
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack synced ({} files, {} changed) to {}.",
                    written.size(), anyChange[0] ? "some" : "none", packRoot.getPath());
            return anyChange[0];
        }
    }

    /** Read a file's bytes, or null if it doesn't exist / can't be read (treated as "new"). */
    private static byte[] readOrNull(File f) {
        try { return f.isFile() ? Files.readAllBytes(f.toPath()) : null; }
        catch (Exception e) { return null; }
    }

    /**
     * Write {@code data} to {@code dest} atomically: fill a sibling {@code .tmp} first, then rename it
     * over the target. A reader (the resource reload) therefore sees either the whole old file or the
     * whole new one — never the truncate-then-fill window of a raw {@code Files.write}, which was
     * leaving readers with 0-byte / partial PNGs during a fast create burst. The {@code .tmp} sits in
     * the same directory as {@code dest} so the move stays on one FileStore and can be atomic.
     */
    private static void writeAtomic(File dest, byte[] data) throws Exception {
        File tmp = new File(dest.getAbsolutePath() + ".tmp");
        Files.write(tmp.toPath(), data);
        try {
            Files.move(tmp.toPath(), dest.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Extremely unlikely (same dir, same volume) — fall back to a plain replace.
            Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Remove any file under {@code root} not produced by this pass (a slot that became empty, an
     * old per-face texture, the stale May-17 leftovers) so the loose folder mirrors the HTTP pack
     * exactly. {@code base} is the pack root used to compute pack-relative paths.
     */
    private static boolean deleteStale(File base, File dir, Set<String> written) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        boolean deleted = false;
        for (File f : files) {
            if (f.isDirectory()) {
                deleted |= deleteStale(base, f, written);
            } else {
                String rel = base.toPath().relativize(f.toPath()).toString().replace('\\', '/');
                if (!written.contains(rel)) deleted |= f.delete();
            }
        }
        return deleted;
    }

    /**
     * Apply the pack for {@code hash}. Reloads are mandatory: any real change to the loose folder takes one
     * guarded full {@code reloadResources()}; an empty change set (disk already matched the emit) just records
     * the hash for this session's dedup. Coalesces a request that arrives mid-reload.
     */
    private static void applyReload(MinecraftClient client, String hash, boolean anyChange) {
        injectPackIfNeeded(client);

        // Nothing actually changed on disk — the pack Minecraft already has is correct; no reload needed.
        if (!anyChange) {
            lastAppliedHash = hash;
            finishOp(client);
            return;
        }

        // A real change: one full reload. The op stays "in flight" across the async reload, so no coalesced
        // regen can start a writeLoosePack that would rewrite the folder this reload is still reading — that
        // overlap was the corrupt-PNG race.
        client.reloadResources()
                .thenRun(() -> client.execute(() -> {
                    try {
                        lastAppliedHash = hash;
                        // Group 14 Phase 1b/1c: the textures just changed on disk — drop the off-atlas
                        // anim AND static caches so placed blocks re-read the fresh image next frame.
                        com.customblocks.client.render.AnimFrameCache.clear();
                        com.customblocks.client.render.StaticFrameCache.clear();
                        CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack applied (hash {}).", hash);
                    } finally {
                        finishOp(client);   // release the gate even if a cache clear throws — never wedge opInFlight
                    }
                }))
                .exceptionally(ex -> {
                    CustomBlocksMod.LOGGER.error("[CustomBlocks] Local pack reload failed.", ex);
                    client.execute(() -> finishOp(client));
                    return null;
                });
    }

    /**
     * End the current regen op: release the single-op gate, then run one coalesced regen if a request arrived
     * while this op was running (latest hash wins). Client thread. Because every regenerate/finishOp call runs
     * on the client thread, the gate release + pending-run is atomic w.r.t. new requests — a write can never be
     * started while this op's reload is (or was) reading the folder.
     */
    private static void finishOp(MinecraftClient client) {
        opInFlight.set(false);
        if (hasPending) {
            hasPending = false;
            regenerate(client, pendingHash);
        }
    }

    /** Add our loose pack to the enabled list (idempotent) so reloadResources() picks it up. */
    private static void injectPackIfNeeded(MinecraftClient client) {
        if (!client.options.resourcePacks.contains(PACK_ENTRY)) {
            client.options.resourcePacks.add(PACK_ENTRY);
            client.options.write();
        }
        client.getResourcePackManager().scanPacks();
    }
}
