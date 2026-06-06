/**
 * ArabicWordRenderer.java
 *
 * Responsibility: Render Arabic (or any Unicode) text onto a square PNG image using
 * Java2D. Outputs PNG bytes suitable for storage in TextureStore.
 *
 * Font lookup order:
 *   1. config/customblocks/arabtype.ttf  (user-supplied)
 *   2. First .ttf in config/customblocks/fonts/
 *   3. System SansSerif fallback (may lack full Arabic shaping)
 *
 * Depends on: CustomBlocksConfig (for textureSize), standard Java AWT + ImageIO
 * Called by: ArabicBlockRegistry
 */
package com.customblocks.arabic;

import com.customblocks.CustomBlocksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;

public final class ArabicWordRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Arabic");
    private static Font FONT = null;

    static {
        System.setProperty("java.awt.headless", "true");
        FONT = loadFont();
    }

    private ArabicWordRenderer() {}

    /**
     * Render text onto a square image of textureSize. Colors are ARGB ints.
     * Returns PNG bytes, or null on failure.
     */
    public static byte[] render(String text, int textColor, int bgColor) {
        try {
            int size = CustomBlocksConfig.textureSize;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background
            g.setColor(new Color(bgColor, true));
            g.fillRect(0, 0, size, size);

            // Font sizing: fill ~55% of the square
            Font base = (FONT != null) ? FONT : new Font("SansSerif", Font.PLAIN, 12);
            Font drawn = base.deriveFont((float)(size * 0.55));

            // AttributedString for RTL / Arabic shaping
            AttributedString as = new AttributedString(text);
            as.addAttribute(TextAttribute.FONT,          drawn);
            as.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
            as.addAttribute(TextAttribute.FOREGROUND,    new Color(textColor, true));

            FontMetrics fm = g.getFontMetrics(drawn);
            int x = Math.max(0, (size - fm.stringWidth(text)) / 2);
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(as.getIterator(), x, y);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOG.error("[CustomBlocks/Arabic] Render failed for '{}': {}", text, e.getMessage());
            return null;
        }
    }

    private static Font loadFont() {
        // 1. arabtype.ttf beside config folder
        Path p1 = Path.of("config/customblocks/arabtype.ttf");
        if (Files.exists(p1)) {
            try (InputStream in = Files.newInputStream(p1)) {
                LOG.info("[CustomBlocks/Arabic] Loaded arabtype.ttf from {}", p1);
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
            } catch (Exception e) {
                LOG.warn("[CustomBlocks/Arabic] Failed to load arabtype.ttf: {}", e.getMessage());
            }
        }
        // 2. Any .ttf in config/customblocks/fonts/
        Path fontDir = Path.of("config/customblocks/fonts");
        if (Files.isDirectory(fontDir)) {
            try {
                var found = Files.list(fontDir).filter(p -> p.toString().endsWith(".ttf")).findFirst();
                if (found.isPresent()) {
                    try (InputStream in = Files.newInputStream(found.get())) {
                        LOG.info("[CustomBlocks/Arabic] Loaded font from {}", found.get());
                        return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(24f);
                    }
                }
            } catch (Exception e) {
                LOG.warn("[CustomBlocks/Arabic] Font dir scan failed: {}", e.getMessage());
            }
        }
        // 3. System fallback
        LOG.info("[CustomBlocks/Arabic] No Arabic font found — using system SansSerif. " +
                 "Place arabtype.ttf in config/customblocks/ for proper Arabic shaping.");
        return new Font("SansSerif", Font.PLAIN, 24);
    }
}
