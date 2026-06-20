/**
 * FontAssets.java
 *
 * Responsibility: On boot, extract the JAR-bundled font files to the config folder so the
 * renderer can load them as ordinary files. Bundled at assets/customblocks/fonts/ in the JAR:
 *   - arabtype.ttf          -> config/customblocks/arabtype.ttf          (Arabic shaping)
 *   - RockwellCondensed.ttf -> config/customblocks/fonts/RockwellCondensed.ttf  (English text)
 *
 * Only writes a file if it is not already present, so a developer-supplied override survives.
 * If a bundled resource is missing, logs a clear warning and continues (Arabic/English text
 * rendering then falls back to a system font, never crashing the boot).
 *
 * Depends on: CustomBlocksMod.LOGGER, standard Java NIO
 * Called by:  CustomBlocksMod.onInitialize (once, early)
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FontAssets {

    private FontAssets() {}

    /** Extract both bundled fonts to their config destinations if absent. */
    public static void extractAll() {
        extract("/assets/customblocks/fonts/arabtype.ttf",
                Path.of("config/customblocks/arabtype.ttf"));
        extract("/assets/customblocks/fonts/RockwellCondensed.ttf",
                Path.of("config/customblocks/fonts/RockwellCondensed.ttf"));
    }

    /** Copy one bundled resource to dest, only if dest does not exist yet. */
    private static void extract(String resource, Path dest) {
        try {
            if (Files.exists(dest)) {
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Font already present, keeping {}.", dest);
                return;
            }
            try (InputStream in = FontAssets.class.getResourceAsStream(resource)) {
                if (in == null) {
                    CustomBlocksMod.LOGGER.warn(
                            "[CustomBlocks] Bundled font {} missing from JAR — text rendering will " +
                            "fall back to a system font.", resource);
                    return;
                }
                Files.createDirectories(dest.getParent());
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                CustomBlocksMod.LOGGER.info("[CustomBlocks] Extracted {} to {}.",
                        dest.getFileName(), dest);
            }
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Failed to extract font {}: {}",
                    resource, e.getMessage());
        }
    }
}
