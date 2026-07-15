/**
 * TempRetextureBackup.java — GROUP_14 §S TEMPORARY (remove when the logo re-source job is done).
 *
 * Per-block snapshot store behind /cb tempretexture. Before the batch overwrites a block's texture with a
 * sourced HD logo, this captures that block's TRUE original (baked texture + stored source image + source
 * link) to disk so /cb tempretexture undo <id> can put exactly that block back — independent of the global
 * /cb undo stack (which only walks the last action). The snapshot is written ONCE per block: re-running
 * /cb tempretexture all never clobbers the original, so undo always returns the real starting block.
 *
 * Re-source only ever rewrites texture+source+url (and is gated to STATIC blocks by the command), so those
 * three are the whole of what undo must restore — faces and animation are never touched here.
 *
 * Files (all under config/customblocks/tempretexture/): slot_<i>.snap = marker carrying which parts existed;
 * slot_<i>.tex.png / .src.bin / .url.txt = the saved originals (written only when they existed). Atomic
 * write-temp + rename everywhere (CLAUDE.md §7).
 *
 * Depends on: TextureStore (the slot I/O choke point). Called by: TempRetextureCommands.
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class TempRetextureBackup {

    private TempRetextureBackup() {} // static-only

    private static final Path DIR = Path.of("config/customblocks/tempretexture");

    private static Path marker(int i) { return DIR.resolve("slot_" + i + ".snap"); }
    private static Path texFile(int i) { return DIR.resolve("slot_" + i + ".tex.png"); }
    private static Path srcFile(int i) { return DIR.resolve("slot_" + i + ".src.bin"); }
    private static Path urlFile(int i) { return DIR.resolve("slot_" + i + ".url.txt"); }

    /** True once this block has a saved original (so we never re-snapshot over it). */
    public static boolean has(int index) {
        return Files.exists(marker(index));
    }

    /**
     * Capture this block's current texture+source+link as its restore point — but only the FIRST time, so
     * repeated /cb tempretexture all runs keep pointing undo at the true original. No-op if already saved.
     */
    public static void snapshot(int index) {
        if (has(index)) return;
        try {
            Files.createDirectories(DIR);
            byte[] tex = TextureStore.load(index);
            byte[] src = TextureStore.loadSource(index);
            String url = TextureStore.loadUrl(index);
            boolean hadTex = tex != null && tex.length > 0;
            boolean hadSrc = src != null && src.length > 0;
            boolean hadUrl = url != null && !url.isBlank();
            if (hadTex) write(texFile(index), tex);
            if (hadSrc) write(srcFile(index), src);
            if (hadUrl) write(urlFile(index), url.getBytes(StandardCharsets.UTF_8));
            // Marker LAST so a crash mid-write never leaves a half-snapshot that undo would trust.
            write(marker(index), ("tex=" + hadTex + ";src=" + hadSrc + ";url=" + hadUrl).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] tempretexture: failed to snapshot slot {}", index, e);
        }
    }

    /** Restore this block to its captured original. False if there is no snapshot for it. */
    public static boolean restore(int index) {
        if (!has(index)) return false;
        try {
            String flags = new String(Files.readAllBytes(marker(index)), StandardCharsets.UTF_8);
            boolean hadTex = flags.contains("tex=true");
            boolean hadSrc = flags.contains("src=true");
            boolean hadUrl = flags.contains("url=true");

            if (hadTex) TextureStore.save(index, Files.readAllBytes(texFile(index)));
            else TextureStore.delete(index); // original had no texture (rare) → full clear

            if (hadSrc) TextureStore.saveSource(index, Files.readAllBytes(srcFile(index)));
            else TextureStore.deleteSource(index); // strip the logo's source so re-bake matches the original

            if (hadUrl) TextureStore.saveUrl(index, new String(Files.readAllBytes(urlFile(index)), StandardCharsets.UTF_8));
            else TextureStore.deleteUrl(index);
            return true;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] tempretexture: failed to restore slot {}", index, e);
            return false;
        }
    }

    /** Every slot index that currently has a snapshot (drives /cb tempretexture undo all). */
    public static List<Integer> snapshotted() {
        List<Integer> out = new ArrayList<>();
        if (!Files.isDirectory(DIR)) return out;
        try (Stream<Path> files = Files.list(DIR)) {
            files.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith("slot_") && n.endsWith(".snap"))
                    .forEach(n -> {
                        try { out.add(Integer.parseInt(n.substring(5, n.length() - 5))); }
                        catch (NumberFormatException ignored) {}
                    });
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] tempretexture: failed to list snapshots", e);
        }
        return out;
    }

    /** Drop all snapshots (the job-done cleanup). Returns how many blocks were cleared. */
    public static int clearAll() {
        if (!Files.isDirectory(DIR)) return 0;
        int blocks = snapshotted().size();
        try (Stream<Path> files = Files.list(DIR)) {
            files.forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            Files.deleteIfExists(DIR);
        } catch (IOException e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] tempretexture: failed to clear snapshots", e);
        }
        return blocks;
    }

    private static void write(Path file, byte[] bytes) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
