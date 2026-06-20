/**
 * TextureStore.java
 *
 * Responsibility: The sole reader/writer of per-slot binary texture files
 * (config/customblocks/textures/slot_N.png). The metadata counterpart is SlotDataStore;
 * this handles the heavy PNG bytes so they never bloat slots.json. Atomic writes (NFR-13).
 *
 * Also keeps each URL-created block's ORIGINAL downloaded image (config/customblocks/sources/
 * slot_N.src, raw bytes, format-agnostic). The baked slot_N.png is locked to the size it was
 * made at, so re-rendering a block at a HIGHER resolution needs the real source — this store
 * provides it. Blocks made without a single source image (Arabic glyphs, video frames) simply
 * have no source file; callers must tolerate a null loadSource().
 *
 * Depends on: CustomBlocksMod (LOGGER)
 * Called by:  the retexture command (save + saveSource), ServerPackGenerator (load),
 *             SlotManager (delete — removes both texture and source).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TextureStore {

    private static final String DIR = "config/customblocks/textures";
    private static final String SRC_DIR = "config/customblocks/sources";

    private TextureStore() {} // static-only

    /** The six block faces, in vanilla model order (M4 per-face paint). */
    public static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

    private static Path file(int index) {
        return Path.of(DIR, "slot_" + index + ".png");
    }

    private static Path sourceFile(int index) {
        return Path.of(SRC_DIR, "slot_" + index + ".src");
    }

    private static Path faceFile(int index, String face) {
        return Path.of(DIR, "slot_" + index + "_" + face + ".png");
    }

    /** Atomically write the texture for a slot. */
    public static void save(int index, byte[] png) {
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path file = file(index);
            Path tmp = dir.resolve("slot_" + index + ".png.tmp");
            Files.write(tmp, png);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            TextureNameMirror.syncSlot(index); // Group 26 Part C — refresh the named-texture mirror (flag-gated)
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to save texture for slot {}", index, e);
        }
    }

    /** Load a slot's texture bytes, or null if none exists. */
    public static byte[] load(int index) {
        Path file = file(index);
        if (!Files.exists(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to read texture for slot {}", index, e);
            return null;
        }
    }

    public static boolean has(int index) {
        return Files.exists(file(index));
    }

    /** Atomically store a block's ORIGINAL downloaded image (raw bytes, any format). */
    public static void saveSource(int index, byte[] raw) {
        if (raw == null || raw.length == 0) return;
        try {
            Path dir = Path.of(SRC_DIR);
            Files.createDirectories(dir);
            Path file = sourceFile(index);
            Path tmp = dir.resolve("slot_" + index + ".src.tmp");
            Files.write(tmp, raw);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to save source image for slot {}", index, e);
        }
    }

    /** Load a block's original source image bytes, or null if none was stored (e.g. Arabic/video). */
    public static byte[] loadSource(int index) {
        Path file = sourceFile(index);
        if (!Files.exists(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to read source image for slot {}", index, e);
            return null;
        }
    }

    /** True if a re-renderable source image exists for this slot. */
    public static boolean hasSource(int index) {
        return Files.exists(sourceFile(index));
    }

    // ── Per-face overrides (M4) — extra PNGs beside the base; absent file = base shows ──

    /** Atomically write one face's override texture. {@code face} must be in {@link #FACES}. */
    public static void saveFace(int index, String face, byte[] png) {
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path tmp = dir.resolve("slot_" + index + "_" + face + ".png.tmp");
            Files.write(tmp, png);
            Files.move(tmp, faceFile(index, face),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            TextureNameMirror.syncSlot(index); // Group 26 Part C — re-mirror with the new face (flag-gated)
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to save {} face for slot {}", face, index, e);
        }
    }

    /** A face's override bytes, or null when that face just shows the base texture. */
    public static byte[] loadFace(int index, String face) {
        Path file = faceFile(index, face);
        if (!Files.exists(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to read {} face for slot {}", face, index, e);
            return null;
        }
    }

    public static boolean hasFace(int index, String face) {
        return Files.exists(faceFile(index, face));
    }

    /** True when at least one face override exists (the pack then emits a per-face model). */
    public static boolean hasAnyFace(int index) {
        for (String f : FACES) if (hasFace(index, f)) return true;
        return false;
    }

    /** Remove one face override (back to the base texture). True if something was deleted. */
    public static boolean deleteFace(int index, String face) {
        try {
            boolean deleted = Files.deleteIfExists(faceFile(index, face));
            if (deleted) TextureNameMirror.syncSlot(index); // Part C — drop the mirrored face PNG (flag-gated)
            return deleted;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Delete a slot's texture, its source AND all face overrides (every slot-free path). */
    public static void delete(int index) {
        try {
            Files.deleteIfExists(file(index));
        } catch (Exception ignored) {
            // best-effort cleanup
        }
        try {
            Files.deleteIfExists(sourceFile(index));
        } catch (Exception ignored) {
            // best-effort cleanup
        }
        for (String f : FACES) deleteFace(index, f);
        TextureNameMirror.removeSlot(index); // Group 26 Part C — drop this slot's named PNG(s) (flag-gated)
    }
}
