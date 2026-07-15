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

import com.customblocks.command.CbFmt;
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

    /** Max frames kept (locked design). Longer clips are even-sampled down to this so every frame is real. */
    public static final int MAX_FRAMES = 256;
    /**
     * Per-frame cell ceiling for an animated clip. Frames are packed into a square GRID texture rendered
     * OFF the block atlas (its own GL texture, no mipmap), so a clip keeps ALL its frames at this size —
     * no atlas strip-height limit, no frame-dropping to fit one tall column. The real cell may be smaller
     * (see {@link #gridCell}) so the whole grid stays within {@link #GRID_BUDGET_PX}.
     */
    public static final int OFFATLAS_MAX_SIZE = 512;
    /**
     * Max side (px) of the packed grid texture — caps GPU memory for one animated block (a 4096² RGBA
     * texture is 64 MB). Both cols·cell and rows·cell are kept ≤ this. Well under the GL 16384 hard limit.
     */
    private static final int GRID_BUDGET_PX = 4096;
    /** Clamp absurd source frame dimensions so one giant frame can't exhaust the heap. */
    private static final int MAX_FRAME_DIM = 4096;
    /** Hard wall-clock budget for one decode; a pathological GIF aborts cleanly past this. */
    private static final long TIMEOUT_MS = 20_000;
    /** Stop collecting if free heap drops below this, rather than risk an OOM. */
    private static final long MIN_FREE_HEAP_BYTES = 64L * 1024 * 1024;

    /** The decode result: the strip PNG, per-frame times (ticks AND real ms), frame count, transparency, warning. */
    public record Decoded(byte[] stripPng, List<Integer> frameTimes, List<Integer> frameTimesMs,
                          int frameCount, boolean transparency, String warning) {}

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
     * Decode {@code raw} into a square-cell GRID PNG ({@code cols×rows} cells, packed left→right, top→bottom)
     * holding EVERY frame, plus per-frame display times. Returns null if the data isn't a readable multi-frame
     * image (caller falls back to the static path). Runs OFF the server thread. Frames are bicubic-scaled to a
     * cell size chosen so the whole grid fits {@link #GRID_BUDGET_PX}. The grid renders off the block atlas
     * (its own GL texture), so it keeps full speed + all frames with no atlas muffle. {@code size} is the
     * desired per-frame size (16..512); {@link #gridCell} may shrink it for very long clips.
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

            // Keep EVERY frame (the off-atlas grid has no strip-height limit), only even-sampling clips
            // longer than the locked MAX_FRAMES so memory stays bounded. We still composite EVERY frame in
            // order (disposal correctness) and only snapshot the kept ones.
            int frameCap = MAX_FRAMES;

            Set<Integer> keep = null;
            String warning = null;
            if (total > frameCap) {
                keep = evenSampleIndices(total, frameCap);
                warning = CbFmt.VALUE + "That clip had " + total + " frames — kept " + frameCap
                        + " (the max) at full speed. " + CbFmt.DIM + "Use /cb animation to fine-tune.";
                CustomBlocksMod.LOGGER.warn("[CustomBlocks] Animation: {} frames sampled to {}.", total, frameCap);
            }

            // Per-frame cell size: as large as the requested size allows while the packed grid stays within
            // GRID_BUDGET_PX. Short clip → big crisp cells; long clip → smaller cells, but ALL frames kept.
            int expected = (total > 0) ? Math.min(total, MAX_FRAMES) : MAX_FRAMES;
            int effSize = gridCell(expected, size);

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

            // centiseconds → ticks (1 tick = 50ms = 5cs, min 1) for the legacy mcmeta + studio fps, AND
            // centiseconds → real milliseconds (1cs = 10ms, min 10) for the off-atlas real-clock renderer.
            // Done after the loop so a kept frame's folded-in skipped time is included → true original speed.
            List<Integer> ticks = new ArrayList<>(keptCs.size());
            List<Integer> ms = new ArrayList<>(keptCs.size());
            for (int cs : keptCs) {
                ticks.add(Math.max(1, (int) Math.round(cs / 5.0)));
                ms.add(Math.max(10, cs * 10));
            }

            byte[] strip = buildGrid(kept, effSize);
            return new Decoded(strip, ticks, ms, kept.size(), transparency, warning);
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
     * Columns in the packed grid for {@code n} frames — a square-ish layout (⌈√n⌉ columns). SHARED by the
     * decoder (packs the PNG), ServerPackGenerator (writes the grid sidecar), and the client AnimFrameCache
     * (reads cells), so all three agree on the layout. Frame {@code f} lives at col {@code f % cols}, row
     * {@code f / cols}.
     */
    public static int gridCols(int n) {
        if (n <= 1) return 1;
        return (int) Math.ceil(Math.sqrt(n));
    }

    /**
     * Per-frame cell px for {@code n} frames at the requested size, shrunk if needed so neither grid side
     * (cols·cell, rows·cell) exceeds {@link #GRID_BUDGET_PX}. Floored at 16. A short clip keeps the full
     * requested size; only a long clip (many cells) is scaled down to stay within the GPU-memory budget.
     */
    public static int gridCell(int n, int requested) {
        int cols = gridCols(n);
        int rows = (n + cols - 1) / cols;
        int maxDim = Math.max(1, Math.max(cols, rows));
        int cell = Math.min(requested, GRID_BUDGET_PX / maxDim);
        return Math.max(16, cell);
    }

    /** Pack the square frames into one GRID PNG (cols×rows cells, left→right, top→bottom). */
    private static byte[] buildGrid(List<BufferedImage> frames, int cell) throws Exception {
        int n = frames.size();
        int cols = gridCols(n);
        int rows = (n + cols - 1) / cols;
        BufferedImage grid = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = grid.createGraphics();
        for (int f = 0; f < n; f++) {
            g.drawImage(frames.get(f), (f % cols) * cell, (f / cols) * cell, null);
        }
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(grid, "PNG", baos);
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
