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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class ResourcePackGenerator {

    /** Options entry that enables our loose pack ("file/<folder name>"). */
    private static final String PACK_ENTRY = "file/CustomBlocks";

    /** Guards against stacking reloadResources() calls; a second request rides the pendingHash. */
    private static final AtomicBoolean reloadInFlight = new AtomicBoolean(false);
    private static volatile String pendingHash;       // a regen requested while a reload was running
    private static volatile boolean hasPending = false;
    private static volatile String lastAppliedHash;   // hash of the pack currently applied

    private ResourcePackGenerator() {} // static-only

    /**
     * (Re)generate the local pack for {@code hash} and silently reload. No-op if that exact pack is
     * already applied. File I/O runs off-thread; the reload is scheduled back on the client thread.
     */
    public static void regenerate(MinecraftClient client, String hash) {
        if (client == null) return;
        if (hash != null && hash.equals(lastAppliedHash)) {
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack already current (hash {}), skipping regen.", hash);
            return;
        }
        Thread t = new Thread(() -> {
            try {
                writeLoosePack(new File(client.runDirectory, "resourcepacks/CustomBlocks"));
                client.execute(() -> applyReload(client, hash));
            } catch (Exception e) {
                CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to write local resource pack", e);
            }
        }, "CustomBlocks-ClientPackGen");
        t.setDaemon(true);
        t.start();
    }

    /** Write every emitted pack file loose under {@code packRoot}, then delete files left behind. */
    private static void writeLoosePack(File packRoot) throws Exception {
        Set<String> written = new HashSet<>();
        ServerPackGenerator.emit((path, data) -> {
            File dest = new File(packRoot, path);
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(dest.toPath(), data);
            written.add(path.replace('\\', '/'));
        });
        deleteStale(packRoot, packRoot, written);
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack written ({} files) to {}.",
                written.size(), packRoot.getPath());
    }

    /**
     * Remove any file under {@code root} not produced by this pass (a slot that became empty, an
     * old per-face texture, the stale May-17 leftovers) so the loose folder mirrors the HTTP pack
     * exactly. {@code base} is the pack root used to compute pack-relative paths.
     */
    private static void deleteStale(File base, File dir, Set<String> written) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteStale(base, f, written);
            } else {
                String rel = base.toPath().relativize(f.toPath()).toString().replace('\\', '/');
                if (!written.contains(rel)) f.delete();
            }
        }
    }

    /** Enable the pack if needed, then run one guarded silent reload; coalesce a request mid-reload. */
    private static void applyReload(MinecraftClient client, String hash) {
        injectPackIfNeeded(client);
        if (reloadInFlight.compareAndSet(false, true)) {
            client.reloadResources()
                    .thenRun(() -> client.execute(() -> {
                        reloadInFlight.set(false);
                        lastAppliedHash = hash;
                        // Group 14 Phase 1b: the strips just changed on disk — drop the off-atlas anim
                        // textures so placed animated blocks re-read the fresh strip on the next frame.
                        com.customblocks.client.render.AnimFrameCache.clear();
                        CustomBlocksMod.LOGGER.info("[CustomBlocks] Local pack applied (hash {}).", hash);
                        if (hasPending) {
                            hasPending = false;
                            regenerate(client, pendingHash);
                        }
                    }))
                    .exceptionally(ex -> {
                        client.execute(() -> reloadInFlight.set(false));
                        CustomBlocksMod.LOGGER.error("[CustomBlocks] Local pack reload failed.", ex);
                        return null;
                    });
        } else {
            // A reload is already running — remember the latest hash and regen once it finishes.
            pendingHash = hash;
            hasPending = true;
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
