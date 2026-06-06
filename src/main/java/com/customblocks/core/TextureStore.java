/**
 * TextureStore.java
 *
 * Responsibility: The sole reader/writer of per-slot binary texture files
 * (config/customblocks/textures/slot_N.png). The metadata counterpart is SlotDataStore;
 * this handles the heavy PNG bytes so they never bloat slots.json. Atomic writes (NFR-13).
 *
 * Depends on: CustomBlocksMod (LOGGER)
 * Called by:  the retexture command (save), ServerPackGenerator (load), SlotManager (delete).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TextureStore {

    private static final String DIR = "config/customblocks/textures";

    private TextureStore() {} // static-only

    private static Path file(int index) {
        return Path.of(DIR, "slot_" + index + ".png");
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

    public static void delete(int index) {
        try {
            Files.deleteIfExists(file(index));
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }
}
