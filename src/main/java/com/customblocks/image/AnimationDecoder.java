/**
 * AnimationDecoder.java
 *
 * Responsibility: Decode a multi-frame image (animated GIF, and — once a frame-aware ImageIO
 * plugin like TwelveMonkeys is on the classpath — animated WebP) into ONE vertical frame-strip PNG
 * plus the per-frame display times in game ticks. Format-agnostic: it drives the generic
 * {@link ImageReader} multi-frame API, so whatever ImageIO can enumerate frames for animates; a
 * single-frame source (PNG/JPEG/static WebP/APNG-first-frame) is simply "not animated".
 *
 * GIF correctness (recycled ALGORITHM from the old project, rewritten clean — never its bugs):
 *   • composites every frame onto a running canvas, honoring per-frame disposal
 *     (none / doNotDispose / restoreToBackground / restoreToPrevious) and frame offsets;
 *   • reads each frame's real delay (centiseconds → ticks, 1 tick = 50ms = 5cs, min 1 tick);
 *   • crops each composited frame to a CENTERED SQUARE, then scales to textureSize (bicubic) so
 *     every frame is the same square size — fixing the old "frames not normalized" bug.
 *
 * Anti-bug guarantees vs. the old version:
 *   • per-frame timing is RETURNED as plain numbers (never baked into a string) — the caller
 *     persists them in AnimData and the .mcmeta is regenerated deterministically at pack-build;
 *   • frame boundaries are strictly textureSize-based (the strip is size × size·frames), never
 *     inferred from pixel content;
 *   • frames beyond the atlas budget are even-sampled down (two-pass so compositing stays correct)
 *     while keeping FULL per-frame resolution, with a human-readable warning (ADR-008 invert);
 *   • heap pre-check, frame-dimension clamp, and a hard time budget guard against OOM/runaway GIFs.
 *
 * Depends on: ImageIO (+ optional frame-aware plugin). Pure Java, no native code.
 * Called by:  AnimCommands (create-animated + re-decode), later the Part B studio.
 */
package com.customblocks.image;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class AnimationDecoder {

    private AnimationDecoder() {} // static-only

    /** Max frames kept in a strip (locked design). Longer clips are even-sampled down to this. */
    public static final int MAX_FRAMES = 256;
    /**
     * Max HEIGHT (px) one animated strip may occupy in Minecraft's block atlas. The strip is a single
     * (size × size·frames) sprite; a sprite taller than the atlas budget forces Minecraft to downscale
     * the WHOLE block atlas and disable mipmaps for every block — the "muffled"/aliased look. A clip with
     * more frames than fit is even-sampled to fewer frames at FULL per-frame resolution (see {@link
     * #atlasFrameCap}), so every frame stays crisp and every other block stays sharp. 8192 = half of
     * MC's 16384 atlas max, leaving room for all the other block sprites. See ADR-007 + ADR-008.
     */
    private static final int MAX_STRIP_PX = 8192;
    /**
     * Cap an ATLAS caller should apply to the per-frame size. The shared block atlas muffles (mipmaps on)
     * above 256px (ADR-007); 512px is reserved for the own-texture renderer (ADR-008). Atlas callers pass
     * {@code Math.min(ATLAS_MAX_SIZE, textureSize)} so a config set to 512 can't overflow the atlas (the
     * speckled/muffled look). {@link #decode} itself does NOT force this — the own-texture path needs 512.
     */
    public static final int ATLAS_MAX_SIZE = 256;
    /** Clamp absurd source frame dimensions so one giant frame can't exhaust the heap. */
    private static final int MAX_FRAME_DIM = 4096;
    /** Hard wall-clock budget for one decode; a pathological GIF aborts cleanly past this. */
    private static final long TIMEOUT_MS = 20_000;
    /** Stop collecting if free heap drops below this, rather than risk an OOM. */
    private static final long MIN_FREE_HEAP_BYTES = 64L * 1024 * 1024;

    /** The decode result: the strip PNG, per-frame times (ticks), frame count, transparency, warning. */
    public record Decoded(byte[] stripPng, List<Integer> frameTimes, int frameCount,
                          boolean transparency, String warning) {}

    private record FrameMeta(int delayCsecs, int disposal, int offsetX, int offsetY) {}

    /**
     * True when {@code raw} holds more than one frame (an actual animation). Cheap-ish: it asks the
     * reader for the frame count. Any failure → treated as NOT animated (the caller then takes the
     * normal static path), so this can never throw the create flow off the rails.
     */
    public static boolean isAnimated(byte[] raw) {
        if (raw == null || raw.length == 0) return false;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
            if (iis == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, false, false);
                return reader.getNumImages(true) > 1;
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decode {@code raw} into a square vertical strip at {@code size}px per frame. Returns null if the
     * data isn't a readable multi-frame image (caller falls back to the static path). The caller runs this
     * OFF the server thread. Frames are scaled with high-quality bicubic + antialias. A clip with more
     * frames than fit the atlas is even-sampled to fewer frames at FULL resolution (see {@link
     * #atlasFrameCap}) so its strip can never overflow the block atlas (which would disable mipmaps for
     * every block — the "muffled" look). ADR-008 interim: hold resolution, drop frames — no 32px crush.
     */
    public static Decoded decode(byte[] raw, int size) {
        // Honor the requested size up to 512 — the own-texture renderer path (ScreenTest) wants full res.
        // ATLAS callers pass min(ATLAS_MAX_SIZE, …) so their strip stays atlas-safe; this method doesn't force it.
        size = Math.max(16, Math.min(CustomBlocksConfig.MAX_TEXTURE_SIZE, size));
        long start = System.currentTimeMillis();
        ImageReader reader = null;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
            if (iis == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return null;
            reader = readers.next();
            reader.setInput(iis, false, false); // not seek-forward-only + read metadata (delays/disposal)

            int total = safeNumImages(reader);                 // -1 if the reader won't say
            BufferedImage first = reader.read(0);
            if (first == null) return null;
            int canvasW = Math.min(first.getWidth(), MAX_FRAME_DIM);
            int canvasH = Math.min(first.getHeight(), MAX_FRAME_DIM);
            if (canvasW <= 0 || canvasH <= 0) return null;

            // How many frames fit the atlas at FULL per-frame resolution. The strip is (size × size·frames);
            // keeping its height ≤ MAX_STRIP_PX preserves mipmaps for every block. ADR-008 interim fix: hold
            // resolution and even-sample FRAMES down to this cap — instead of crushing per-frame size to 32px.
            int frameCap = atlasFrameCap(size);

            // Which source frames to KEEP. When the source has more than frameCap we even-sample, but we
            // still composite EVERY frame in order (disposal correctness) and only snapshot the kept ones —
            // so memory stays bounded to <= frameCap snapshots.
            Set<Integer> keep = null;
            String warning = null;
            if (total > frameCap) {
                keep = evenSampleIndices(total, frameCap);
                warning = "§eThat clip had " + total + " frames — kept " + frameCap
                        + " at full resolution so it stays crisp. §7Use /cb anim to fine-tune.";
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Animation: {} frames sampled to {} (full-res).", total, frameCap);
            }

            int effSize = size; // FULL per-frame resolution — no shrink (the ADR-008 invert)

            BufferedImage composite = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = composite.createGraphics();
            BufferedImage previous = null; // for disposal=3 (restoreToPrevious)

            List<BufferedImage> kept = new ArrayList<>();
            List<Integer> keptCs = new ArrayList<>(); // per-kept-frame display time in centiseconds (see below)
            int lastKept = -1;                        // index into keptCs of the most recent kept frame
            int limit = total > 0 ? total : MAX_FRAMES * 8; // unknown count → read until a frame fails
            boolean transparency = false;

            for (int i = 0; i < limit; i++) {
                if (System.currentTimeMillis() - start > TIMEOUT_MS) {
                    CustomBlocksMod.LOGGER.warn("[CustomBlocks] Animation decode hit the time budget at frame {}.", i);
                    break;
                }
                if (lowHeap()) {
                    CustomBlocksMod.LOGGER.warn("[CustomBlocks] Animation decode stopped early (low heap) at frame {}.", i);
                    break;
                }
                BufferedImage frame;
                FrameMeta fm;
                try {
                    frame = reader.read(i);
                    fm = parseGifMeta(reader.getImageMetadata(i));
                } catch (Exception e) {
                    break; // ran past the last real frame
                }
                if (frame == null) break;

                if (fm.disposal() == 3) previous = copy(composite);

                int dx = clamp(fm.offsetX(), 0, canvasW - 1);
                int dy = clamp(fm.offsetY(), 0, canvasH - 1);
                gc.setComposite(AlphaComposite.SrcOver);
                gc.drawImage(frame, dx, dy, null);

                int cs = fm.delayCsecs() <= 0 ? 10 : fm.delayCsecs();
                boolean wanted = (keep == null) || keep.contains(i);
                if (wanted) {
                    BufferedImage square = cropScaleSquare(composite, effSize);
                    if (!transparency) transparency = hasAlpha(square);
                    kept.add(square);
                    keptCs.add(cs);
                    lastKept = keptCs.size() - 1;
                    if (keep == null && kept.size() >= frameCap) break; // unknown-count safety cap
                } else if (lastKept >= 0) {
                    // Sampled-OUT frame: fold its display time into the kept frame it follows, so the
                    // animation keeps its ORIGINAL total duration (dropping frames must NOT speed it up).
                    keptCs.set(lastKept, keptCs.get(lastKept) + cs);
                }

                applyDisposalForNext(gc, fm, frame, composite, previous, canvasW, canvasH, dx, dy);
            }
            gc.dispose();

            if (kept.isEmpty()) return null;
            if (kept.size() == 1) return null; // a single frame is a static block, not an animation

            // centiseconds → ticks (1 tick = 50ms = 5cs), min 1. Done after the loop so a kept frame's
            // folded-in skipped time is included → the clip plays at its true original speed.
            List<Integer> ticks = new ArrayList<>(keptCs.size());
            for (int cs : keptCs) ticks.add(Math.max(1, (int) Math.round(cs / 5.0)));

            byte[] strip = buildStrip(kept, effSize);
            return new Decoded(strip, ticks, kept.size(), transparency, warning);
        } catch (OutOfMemoryError oom) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Out of memory decoding animation.", oom);
            return null;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Animation decode failed.", e);
            return null;
        } finally {
            if (reader != null) try { reader.dispose(); } catch (Exception ignored) {}
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int safeNumImages(ImageReader reader) {
        try {
            return reader.getNumImages(true);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Pick {@code want} evenly-spaced indices spanning [0, total-1] (always includes first + last). */
    private static Set<Integer> evenSampleIndices(int total, int want) {
        Set<Integer> out = new HashSet<>();
        for (int k = 0; k < want; k++) {
            out.add((int) Math.round((double) k * (total - 1) / (want - 1)));
        }
        return out;
    }

    /** Crop the canvas to a centered square, then scale to size×size with high-quality bicubic + antialias. */
    private static BufferedImage cropScaleSquare(BufferedImage canvas, int size) {
        int w = canvas.getWidth(), h = canvas.getHeight();
        int side = Math.min(w, h);
        int sx = (w - side) / 2, sy = (h - side) / 2;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(canvas, 0, 0, size, size, sx, sy, sx + side, sy + side, null);
        g.dispose();
        return out;
    }

    /**
     * The most frames a strip can hold at FULL per-frame {@code size} without overflowing the block atlas.
     * The strip is (size × size·frames); keeping its height ≤ {@link #MAX_STRIP_PX} preserves mipmaps for
     * every block (a taller sprite forces Minecraft to downscale the whole atlas — the "muffle"). Never more
     * than {@link #MAX_FRAMES} (the locked design max); floored at 1. This is the ADR-008 interim invert:
     * hold per-frame resolution and drop frames, instead of the old shrink-the-frame logic that crushed long
     * clips to 32px. {@code size} is 16..512 (decode clamps it), so this returns 16..256 (16 frames at 512px,
     * 32 at the atlas-safe 256px). A short clip (≤ this many frames) keeps every frame.
     */
    private static int atlasFrameCap(int size) {
        int fit = MAX_STRIP_PX / Math.max(16, size);
        return Math.max(1, Math.min(MAX_FRAMES, fit));
    }

    /** Stack the square frames into one vertical strip (size wide, size·N tall). */
    private static byte[] buildStrip(List<BufferedImage> frames, int size) throws Exception {
        BufferedImage strip = new BufferedImage(size, size * frames.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = strip.createGraphics();
        for (int i = 0; i < frames.size(); i++) g.drawImage(frames.get(i), 0, i * size, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(strip, "PNG", baos);
        return baos.toByteArray();
    }

    /** Clear/restore the running composite per the CURRENT frame's disposal, ready for the next. */
    private static void applyDisposalForNext(Graphics2D gc, FrameMeta fm, BufferedImage frame,
                                             BufferedImage composite, BufferedImage previous,
                                             int canvasW, int canvasH, int dx, int dy) {
        switch (fm.disposal()) {
            case 2 -> { // restoreToBackgroundColor — clear this frame's rectangle
                gc.setComposite(AlphaComposite.Clear);
                int rw = Math.min(frame.getWidth(), canvasW - dx);
                int rh = Math.min(frame.getHeight(), canvasH - dy);
                gc.fillRect(dx, dy, Math.max(0, rw), Math.max(0, rh));
                gc.setComposite(AlphaComposite.SrcOver);
            }
            case 3 -> { // restoreToPrevious — revert to the pre-frame snapshot
                if (previous != null) {
                    gc.setComposite(AlphaComposite.Src);
                    gc.drawImage(previous, 0, 0, null);
                    gc.setComposite(AlphaComposite.SrcOver);
                }
            }
            default -> { /* 0 (none) / 1 (doNotDispose) — leave the composite as-is */ }
        }
    }

    private static boolean lowHeap() {
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory() + (rt.maxMemory() - rt.totalMemory());
        return free < MIN_FREE_HEAP_BYTES;
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    /** True if any pixel is less than fully opaque (so the block should be a see-through cutout). */
    private static boolean hasAlpha(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int stepX = Math.max(1, w / 32), stepY = Math.max(1, h / 32); // sample a grid; full scan is wasteful
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                if ((img.getRGB(x, y) >>> 24) < 255) return true;
            }
        }
        return false;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Read GIF per-frame delay/disposal/offset from the standard javax_imageio_gif_image_1.0 tree.
     * For non-GIF formats (no such nodes) it returns sane defaults (delay 10cs, no disposal, no
     * offset) so those frames composite as plain full-frame draws.
     */
    private static FrameMeta parseGifMeta(IIOMetadata meta) {
        int delay = 10, disposal = 0, offX = 0, offY = 0;
        if (meta == null) return new FrameMeta(delay, disposal, offX, offY);
        try {
            for (String fmt : meta.getMetadataFormatNames()) {
                org.w3c.dom.Node root = meta.getAsTree(fmt);
                if (root == null) continue;
                java.util.Deque<org.w3c.dom.Node> stack = new java.util.ArrayDeque<>();
                stack.push(root);
                while (!stack.isEmpty()) {
                    org.w3c.dom.Node n = stack.pop();
                    org.w3c.dom.NamedNodeMap attrs = n.getAttributes();
                    if (attrs != null) {
                        if ("GraphicControlExtension".equals(n.getNodeName())) {
                            org.w3c.dom.Node d  = attrs.getNamedItem("delayTime");
                            org.w3c.dom.Node dm = attrs.getNamedItem("disposalMethod");
                            if (d != null) try { delay = Integer.parseInt(d.getNodeValue()); } catch (NumberFormatException ignored) {}
                            if (dm != null) disposal = switch (dm.getNodeValue()) {
                                case "doNotDispose" -> 1;
                                case "restoreToBackgroundColor" -> 2;
                                case "restoreToPrevious" -> 3;
                                default -> 0;
                            };
                        } else if ("ImageDescriptor".equals(n.getNodeName())) {
                            org.w3c.dom.Node x = attrs.getNamedItem("imageLeftPosition");
                            org.w3c.dom.Node y = attrs.getNamedItem("imageTopPosition");
                            if (x != null) try { offX = Integer.parseInt(x.getNodeValue()); } catch (NumberFormatException ignored) {}
                            if (y != null) try { offY = Integer.parseInt(y.getNodeValue()); } catch (NumberFormatException ignored) {}
                        }
                    }
                    org.w3c.dom.NodeList kids = n.getChildNodes();
                    for (int i = 0; i < kids.getLength(); i++) stack.push(kids.item(i));
                }
            }
        } catch (RuntimeException ignored) {}
        return new FrameMeta(delay, disposal, offX, offY);
    }
}
