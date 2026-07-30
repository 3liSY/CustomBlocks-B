/**
 * ImportService.java — commit a confirmed folder-import run (Group 12 §B, rules 6, 8-11, 18-20).
 *
 * The scan says what WOULD happen ({@link ImportScan}); this is the half that actually happens, and only
 * after the owner confirmed the preview. It is the old mod's proven shape — heavy picture work on a
 * background thread, every mutation hopped back onto the server thread — rebuilt on this project's rails.
 *
 * Rules this file owns:
 *   6  · background removal uses the CONFIGURED mode, through the same BackgroundRemover →
 *        ImageProcessor → snap pipeline `/cb create` runs. There is no import-only special case, which is
 *        why an imported block looks identical to the same picture created one at a time (B12).
 *   8  · each source file is MOVED into done/ the moment its block exists — never deleted.
 *   9  · a picture-only import arrives uncategorised, exactly as `/cb create` leaves a block. Only an
 *        explicit data file can put it in a category, and then only the categories that file names.
 *   10 · the whole run is ONE batch undo entry through the existing UndoManager — no new undo system.
 *   11 · progress is reported while it works, so a long run never looks frozen.
 *   18 · that progress drives the shared pack-sync panel via {@link ImportProgress}.
 *   19 · resume is free: because rule 8 moves each file as it lands, an interrupted run leaves exactly
 *        the unfinished files behind and the next run sees only those. No resume state is stored.
 *   20 · a run of {@link #BACKUP_THRESHOLD}+ files asks G09 for an ordinary safety backup FIRST, and
 *        aborts untouched if that backup fails. G12 writes no backup code of its own.
 *
 * Slot exhaustion cannot half-create a block: the slot is claimed first, and only a claimed slot gets
 * pixels; an entry that finds no slot is reported as a leftover with nothing written.
 *
 * Depends on: ImportScan, ImportEntry, ImportReport, SlotManager, TextureStore, UndoManager,
 *             BackupManager (G09), BackgroundRemover/ImageProcessor (G10), ResourcePackServer, ImportProgress
 * Called by:  command/handlers/ImportFolderCommands
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ImportProgress;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ImportService {

    private ImportService() {} // static-only

    /** From this many files up, the run takes a G09 safety backup before it starts (owner, 2026-07-30). */
    public static final int BACKUP_THRESHOLD = 5;
    /** Backup name prefix, so these are recognisable in /cb backup list and prunable as a family. */
    private static final String BACKUP_PREFIX = "pre-import";
    /** How many pictures are decoded before hopping to the server thread to create them. Bounds memory:
     *  only this many decoded textures are ever held at once, however large the run. */
    private static final int CHUNK = 8;
    /** Longest we wait for one chunk's server-thread work before giving up on the run. */
    private static final long CHUNK_TIMEOUT_SECONDS = 60L;

    /** What a finished run produced — handed to the caller on the SERVER thread to report. */
    public record Outcome(List<ImportReport.Made> made, List<ImportReport.Missed> missed, List<String> leftover) {}

    /**
     * Run the import. Call on the SERVER thread; returns immediately and does its work in the background,
     * calling {@code onDone} back ON the server thread when finished.
     *
     * {@code entries} must already be the confirmed, ready ones — this method creates every entry it is
     * given and does not re-judge them.
     */
    public static void commit(MinecraftServer server, ServerPlayerEntity player, String actorName,
                              UUID undoOwner, List<ImportEntry> entries, Consumer<Outcome> onDone) {
        if (server == null || entries == null || entries.isEmpty()) {
            if (onDone != null) onDone.accept(new Outcome(List.of(), List.of(), List.of()));
            return;
        }
        final List<ImportEntry> work = List.copyOf(entries);
        final int total = work.size();

        // Rule 20: flush slot state on the server thread (we are on it) so the snapshot is current.
        final boolean wantBackup = total >= BACKUP_THRESHOLD;
        if (wantBackup) SlotManager.saveAll();
        final int blockCount = SlotManager.assignedSlots().size();
        final String backupName = BackupManager.timestampName(BACKUP_PREFIX);

        ImportProgress.begin(player, total);

        Thread worker = new Thread(() -> {
            // ── the G09 safety copy, before anything is created ──────────────────────
            if (wantBackup) {
                try {
                    BackupManager.save(backupName, blockCount, BackupManager.Kind.SAFETY, "pre-import", null);
                } catch (Exception e) {
                    IncidentRecorder.record("Backup-before-import failed (" + total + " file(s))", null, actorName, e);
                    server.execute(() -> {
                        ImportProgress.hide(player);
                        if (onDone != null) onDone.accept(new Outcome(List.of(),
                                List.of(new ImportReport.Missed("(the whole run)",
                                        "the safety backup failed, so nothing was imported")),
                                List.of()));
                    });
                    return;
                }
            }

            List<ImportReport.Made> made = Collections.synchronizedList(new ArrayList<>());
            List<ImportReport.Missed> missed = Collections.synchronizedList(new ArrayList<>());
            List<String> leftover = Collections.synchronizedList(new ArrayList<>());
            List<UndoManager.Op> children = Collections.synchronizedList(new ArrayList<>());

            List<Prepared> chunk = new ArrayList<>(CHUNK);
            int seen = 0;
            for (ImportEntry e : work) {
                seen++;
                Prepared p = prepare(e, missed);
                if (p != null) chunk.add(p);
                // Packets go out from the server thread, never this worker (rule: packet work returns to
                // the server executor before it touches game state or the network). Reports files
                // FINISHED, so the bar never reads 100% while blocks are still being made — the closing
                // panel comes from ImportProgress.done alone.
                final int at = seen - 1;
                final String label = e.fileName();
                server.execute(() -> ImportProgress.step(player, at, total, label));
                if (chunk.size() >= CHUNK) {
                    if (!applyChunk(server, chunk, made, missed, leftover, children)) break;
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) applyChunk(server, chunk, made, missed, leftover, children);

            server.execute(() -> {
                if (!made.isEmpty()) {
                    ResourcePackServer.updatePack();  // one rebuild for the whole run
                    HudSync.broadcast(server);        // the new blocks appear without a rejoin (G05-1)
                    // Rule 10: ONE undo entry for the run, through the batch undo G07 already uses.
                    UndoManager.recordBatch(undoOwner, children, "import");
                }
                ImportProgress.done(player, made.size());
                if (onDone != null) onDone.accept(new Outcome(new ArrayList<>(made), new ArrayList<>(missed),
                        new ArrayList<>(leftover)));
            });
        }, "CustomBlocks-ImportFolder");
        worker.setDaemon(true);
        worker.start();
    }

    /** One entry with its texture already baked, waiting for a slot. */
    private record Prepared(ImportEntry entry, byte[] png, byte[] source, String metaJson) {}

    /**
     * Decode + bake one entry's picture off the server thread, through the SAME pipeline `/cb create`
     * uses so the configured background mode applies with no import-only behaviour (rule 6). Returns null
     * and records the reason when the file cannot be used — one bad file never stops the run (rule 12).
     */
    private static Prepared prepare(ImportEntry e, List<ImportReport.Missed> missed) {
        String metaJson = null;
        if (e.data() != null) {
            try {
                metaJson = Files.readString(e.data(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                metaJson = null; // the picture is still perfectly importable without its sidecar
            }
        }
        if (e.image() == null) {
            // A data file with no picture: a definition-only import, no texture to bake.
            return new Prepared(e, null, null, metaJson);
        }
        byte[] raw = ImportScan.read(e.image());
        if (raw == null) {
            missed.add(new ImportReport.Missed(e.fileName(), "the file could not be read"));
            return null;
        }
        try {
            byte[] cleaned = BackgroundRemover.applyReporting(raw, CustomBlocksConfig.backgroundMode).png();
            byte[] png = ImageProcessor.toBlockPng(cleaned, CustomBlocksConfig.textureSize);
            png = BackgroundRemover.snapBackgroundBlack(png, CustomBlocksConfig.backgroundMode);
            return new Prepared(e, png, raw, metaJson);
        } catch (Exception ex) {
            missed.add(new ImportReport.Missed(e.fileName(), "the picture could not be prepared"));
            return null;
        }
    }

    /**
     * Create one chunk's blocks ON the server thread and wait for it, so at most {@link #CHUNK} baked
     * textures are ever in memory at once. Returns false if the hop timed out (the server is shutting
     * down or badly stalled), which stops the run rather than queueing work nobody will run.
     */
    private static boolean applyChunk(MinecraftServer server, List<Prepared> chunk,
                                      List<ImportReport.Made> made, List<ImportReport.Missed> missed,
                                      List<String> leftover, List<UndoManager.Op> children) {
        final List<Prepared> batch = List.copyOf(chunk);
        CountDownLatch done = new CountDownLatch(1);
        server.execute(() -> {
            try {
                for (Prepared p : batch) create(p, made, missed, leftover, children);
            } finally {
                done.countDown();
            }
        });
        try {
            return done.await(CHUNK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Create ONE block. Server thread only.
     *
     * Order is deliberate: claim the slot first, and only then write pixels and move the file. A run that
     * exhausts the pool therefore leaves nothing half-made — the entry becomes a leftover with its source
     * file untouched, so re-running after freeing slots picks it up (rule 7 + rule 19).
     */
    private static void create(Prepared p, List<ImportReport.Made> made, List<ImportReport.Missed> missed,
                               List<String> leftover, List<UndoManager.Op> children) {
        ImportEntry e = p.entry();
        if (SlotManager.hasId(e.id())) {                       // taken since the preview — never overwrite (rule 4)
            missed.add(new ImportReport.Missed(e.fileName(), "a block called \"" + e.id() + "\" appeared in the meantime"));
            return;
        }
        SlotData d = SlotManager.create(e.id(), e.name());
        if (d == null) {
            leftover.add(e.fileName());                        // no free slot — nothing written
            return;
        }
        if (p.png() != null) {
            TextureStore.save(d.index(), p.png());
            TextureStore.saveSource(d.index(), p.source());    // keep the original for later re-bakes
        }
        // Rule 9: nothing here sets a category. Only a data file that NAMES categories can add any.
        if (p.metaJson() != null) BlockExporter.applyMetadata(d, p.metaJson());

        SlotData after = SlotManager.getById(e.id());
        children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, after == null ? d : after, null, "import"));
        made.add(new ImportReport.Made(e.id(), e.name(), e.fileName()));

        // Rule 8: the sources move into done/ only now the block really exists — which is also what makes
        // an interrupted run resume by itself (rule 19).
        if (e.image() != null) ImportScan.move(e.image(), CbPaths.IMPORT_DONE);
        if (e.data() != null)  ImportScan.move(e.data(), CbPaths.IMPORT_DONE);
    }
}
