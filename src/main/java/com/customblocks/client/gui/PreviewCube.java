/**
 * PreviewCube.java — shared 3D texture-cube renderer for Group 27 screens. CLIENT-SIDE ONLY.
 *
 * G27.7 §A1 rebuild: instead of painting each face as a GRID×GRID grid of {@code ctx.fill} rects
 * (~6·GRID² ≈ 18,800 fills/frame — the lag the dev reported), the texture is baked ONCE into a single
 * dynamic atlas (GRID wide × GRID·6 tall, one shaded band per face) and each face is drawn as ONE
 * textured quad via {@code ctx.drawTexture} — 6 quads/frame, re-baked only when the pixels change.
 *
 * Why this also fixes the other two reported bugs:
 *  - §A3 ([?] / overlays drew BEHIND the cube): {@code ctx.drawTexture} renders IMMEDIATELY in 1.21.1
 *    (Tessellator), while {@code ctx.fill}/text are DEFERRED (flushed at the end of the screen render).
 *    We flush the backdrop first ({@code ctx.draw()}), draw the cube immediately, and let every later
 *    bar/overlay/text flush on top — so overlays are always in front, for free.
 *  - §A5 (block vanished while spinning): the cube is drawn with the depth test OFF, so rotated faces
 *    can't be clipped against the GUI's z=0 plane; back-to-front painter's order handles occlusion.
 *
 * Stateful: ONE instance per screen (owns the atlas texture). The screen owns yaw/pitch/zoom/spin and
 * passes its grid + a {@code version} key each frame; call {@link #dispose()} when the screen closes.
 *
 * Depends on: DrawContext / MatrixStack / RotationAxis, NativeImage(+BackedTexture) / TextureManager,
 * RenderSystem, BufferedImage (downsample only).
 * Called by: ArabicPreviewScreen (G27.1), RecolorSliderScreen (G27.2), ShapeEditorScreen (G27.5).
 */
package com.customblocks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.awt.image.BufferedImage;

@Environment(EnvType.CLIENT)
public final class PreviewCube {

    public static final int GRID = 56;          // cells per face side (sharp; texture is baked once, not per-frame)
    private static final float AMBIENT = 0.55f;  // base face brightness so no face goes black
    private static final float DIFFUSE = 0.45f;  // how much the light direction brightens a face
    private static final double[] LIGHT = norm(-0.35, -0.55, 0.78); // upper-left, toward viewer

    private static final double[][] FACE_N = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0},
    };
    /** Fixed per-face brightness baked into the atlas (face f occupies atlas band [f·GRID, (f+1)·GRID)). */
    private static final float[] FACE_BRI = faceBrightness();

    /** Per-cell colour transform: maps a base 0xAARRGGBB cell to an output 0xAARRGGBB before shading. */
    @FunctionalInterface
    public interface CellColor { int apply(int argb); }

    /** Identity transform — draw the texture as-is. */
    public static final CellColor AS_IS = argb -> argb;

    // ── baked atlas state (one per screen instance) ───────────────────────────
    private NativeImageBackedTexture tex;   // GRID × GRID*6, six shaded bands
    private Identifier texId;
    private int[] bakedGrid;                 // identity of the grid last baked (reference compare)
    private long bakedVersion = Long.MIN_VALUE; // caller's pixel-version last baked
    private boolean disposed;

    public PreviewCube() {}

    /**
     * Draw the full cube. {@code grid} is GRID·GRID packed 0xAARRGGBB. {@code transform} recolours each
     * cell (e.g. live HSL) before the bake; {@code version} is any value the caller bumps when those
     * output pixels change (pass 0 for a static texture — a new {@code grid} reference also re-bakes).
     */
    public void render(DrawContext ctx, int[] grid, int cx, int cy, int half,
                       double yaw, double pitch, CellColor transform, long version) {
        if (grid == null || disposed) return;
        ensureBaked(grid, transform, version);
        beginCube(ctx);
        drawCube(ctx, cx, cy, half, yaw, pitch);
        endCube();
    }

    /**
     * Draw a named-shape silhouette: each pixel-box ({x1,y1,z1,x2,y2,z2} in 0..16) in {@code boxes} is a
     * textured sub-cuboid sharing the cube rotation. Null/empty = a full cube. Same bake rules as
     * {@link #render}.
     */
    public void renderShape(DrawContext ctx, int[] grid, int cx, int cy, int half,
                            double yaw, double pitch, int[][] boxes, CellColor transform, long version) {
        if (grid == null || disposed) return;
        ensureBaked(grid, transform, version);
        int[][] bs = (boxes == null || boxes.length == 0) ? new int[][]{{0, 0, 0, 16, 16, 16}} : boxes;
        beginCube(ctx);
        for (int[] b : bs) drawBox(ctx, cx, cy, half, yaw, pitch, b);
        endCube();
    }

    /** Release the GL texture. Call from the screen's {@code removed()} (render thread). */
    public void dispose() {
        disposed = true;
        if (texId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texId);
            texId = null; tex = null;
        }
    }

    // ── bake ──────────────────────────────────────────────────────────────────
    private void ensureBaked(int[] grid, CellColor transform, long version) {
        if (tex == null) {
            tex = new NativeImageBackedTexture(new NativeImage(GRID, GRID * 6, false));
            texId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("cb_preview_cube", tex);
        }
        if (grid == bakedGrid && version == bakedVersion) return; // pixels unchanged → reuse the upload
        NativeImage img = tex.getImage();
        if (img == null) return;
        for (int f = 0; f < 6; f++) {
            float bri = FACE_BRI[f];
            int bandY = f * GRID;
            for (int gy = 0; gy < GRID; gy++) {
                for (int gx = 0; gx < GRID; gx++) {
                    int out = transform.apply(grid[gy * GRID + gx]);
                    int a = out >>> 24;
                    // NativeImage packs little-endian ABGR (see EyedropScreen.getColor); alpha 0 = see-through.
                    img.setColor(gx, bandY + gy, a == 0 ? 0 : packAbgr(out, a, bri));
                }
            }
        }
        tex.upload();
        bakedGrid = grid;
        bakedVersion = version;
    }

    // ── immediate-mode draw wrapper (see header for why) ──────────────────────
    private void beginCube(DrawContext ctx) {
        ctx.draw();                       // flush the deferred backdrop so the immediate cube sits ABOVE it
        RenderSystem.enableBlend();       // honour transparent padding cells
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();  // rotated faces must never be z-clipped against the GUI plane
    }

    private void endCube() {
        RenderSystem.enableDepthTest();
    }

    /** Six face quads, painted back-to-front. */
    private void drawCube(DrawContext ctx, int cx, int cy, int half, double yaw, double pitch) {
        for (int f : faceOrder(pitch, yaw)) {
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(cx, cy, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) pitch));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yaw));
            applyFaceRotation(m, f);
            m.translate(0, 0, half);
            drawBand(ctx, f, half, half);
            m.pop();
        }
    }

    /** One pixel-box as a textured cuboid, faces painted back-to-front. */
    private void drawBox(DrawContext ctx, int cx, int cy, int half, double yaw, double pitch, int[] b) {
        // local space: full cube spans [-half, half]. x,z: 0px→-half, 16px→+half.
        // y is screen-down, so block-bottom (0px) → +half, block-top (16px) → -half.
        double x0 = (b[0] / 8.0 - 1.0) * half, x1 = (b[3] / 8.0 - 1.0) * half;
        double z0 = (b[2] / 8.0 - 1.0) * half, z1 = (b[5] / 8.0 - 1.0) * half;
        double y0 = (1.0 - b[1] / 8.0) * half, y1 = (1.0 - b[4] / 8.0) * half;
        double ccx = (x0 + x1) / 2, ccy = (y0 + y1) / 2, ccz = (z0 + z1) / 2;
        double hX = Math.abs(x1 - x0) / 2, hY = Math.abs(y1 - y0) / 2, hZ = Math.abs(z1 - z0) / 2;

        for (int f : faceOrder(pitch, yaw)) {
            double dist = (f <= 1) ? hZ : (f <= 3) ? hX : hY; // centre→face along the normal
            double pw   = (f == 2 || f == 3) ? hZ : hX;        // in-plane half width
            double ph   = (f >= 4) ? hZ : hY;                  // in-plane half height
            MatrixStack m = ctx.getMatrices();
            m.push();
            m.translate(cx, cy, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) pitch));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yaw));
            m.translate((float) ccx, (float) ccy, (float) ccz);
            applyFaceRotation(m, f);
            m.translate(0, 0, (float) dist);
            drawBand(ctx, f, (int) Math.round(pw), (int) Math.round(ph));
            m.pop();
        }
    }

    /** Draw face {@code f}'s atlas band as one quad spanning local [-halfW,halfW]×[-halfH,halfH]. */
    private void drawBand(DrawContext ctx, int f, int halfW, int halfH) {
        if (halfW <= 0 || halfH <= 0) return;
        // drawTexture(id, x, y, width, height, u, v, regionW, regionH, texW, texH) — scales the band to the quad.
        ctx.drawTexture(texId, -halfW, -halfH, halfW * 2, halfH * 2,
                0f, (float) (f * GRID), GRID, GRID, GRID, GRID * 6);
    }

    /** Faces sorted back-to-front by their rotated-normal depth (z grows toward the viewer). */
    private static Integer[] faceOrder(double pitch, double yaw) {
        double pr = Math.toRadians(pitch), yr = Math.toRadians(yaw);
        Integer[] order = {0, 1, 2, 3, 4, 5};
        double[] depth = new double[6];
        for (int f = 0; f < 6; f++) depth[f] = rotate(pr, yr, FACE_N[f])[2];
        java.util.Arrays.sort(order, (a, b) -> Double.compare(depth[a], depth[b]));
        return order;
    }

    /** Orient the drawing plane (local +Z) to point along face f's outward normal. */
    private static void applyFaceRotation(MatrixStack m, int f) {
        switch (f) {
            case 1 -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180)); // back
            case 2 -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));  // right (+X)
            case 3 -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90)); // left  (-X)
            case 4 -> m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90)); // top   (+Y)
            case 5 -> m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));  // bottom(-Y)
            default -> { }                                                      // front (+Z)
        }
    }

    // ── static helpers (texture prep — unchanged) ─────────────────────────────
    /** A flat solid-colour grid (used when a shape preview has no texture to fetch). */
    public static int[] solid(int rgb) {
        int[] g = new int[GRID * GRID];
        java.util.Arrays.fill(g, 0xFF000000 | (rgb & 0xFFFFFF));
        return g;
    }

    /**
     * Composite a packed-0xAARRGGBB grid over an opaque background colour: every transparent or
     * semi-transparent cell takes (some of) {@code bgArgb}, output is fully opaque. Mirrors the
     * server-side {@code ImageProcessor.fillBackground} so the studio preview matches the baked block.
     */
    public static int[] compositeOver(int[] src, int bgArgb) {
        int[] out = new int[GRID * GRID];
        int br = (bgArgb >> 16) & 0xFF, bg = (bgArgb >> 8) & 0xFF, bb = bgArgb & 0xFF;
        for (int i = 0; i < out.length; i++) {
            int p = (src == null) ? 0 : src[i];
            int a = p >>> 24;
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            int or = (r * a + br * (255 - a)) / 255;
            int og = (g * a + bg * (255 - a)) / 255;
            int ob = (b * a + bb * (255 - a)) / 255;
            out[i] = 0xFF000000 | (or << 16) | (og << 8) | ob;
        }
        return out;
    }

    /** Area-average downsample of a source image to GRID·GRID packed 0xAARRGGBB (alpha-weighted RGB). */
    public static int[] downsample(BufferedImage img) {
        int[] g = new int[GRID * GRID];
        int w = img.getWidth(), h = img.getHeight();
        for (int gy = 0; gy < GRID; gy++) {
            for (int gx = 0; gx < GRID; gx++) {
                int sx0 = gx * w / GRID, sx1 = Math.max(sx0 + 1, (gx + 1) * w / GRID);
                int sy0 = gy * h / GRID, sy1 = Math.max(sy0 + 1, (gy + 1) * h / GRID);
                long aSum = 0, r = 0, gg = 0, b = 0, cnt = 0;
                for (int yy = sy0; yy < sy1 && yy < h; yy++) {
                    for (int xx = sx0; xx < sx1 && xx < w; xx++) {
                        int p = img.getRGB(xx, yy);
                        int pa = p >>> 24;
                        aSum += pa; cnt++;
                        r  += ((p >> 16) & 0xFF) * pa;
                        gg += ((p >> 8) & 0xFF) * pa;
                        b  += (p & 0xFF) * pa;
                    }
                }
                int outA = (int) (aSum / Math.max(1, cnt));
                int or = aSum == 0 ? 0 : (int) (r / aSum);
                int og = aSum == 0 ? 0 : (int) (gg / aSum);
                int ob = aSum == 0 ? 0 : (int) (b / aSum);
                g[gy * GRID + gx] = (outA << 24) | (or << 16) | (og << 8) | ob;
            }
        }
        return g;
    }

    // ── small maths ───────────────────────────────────────────────────────────
    /** Pack a 0xAARRGGBB cell, shaded by {@code bri}, into NativeImage's little-endian ABGR with alpha {@code a}. */
    private static int packAbgr(int argb, int a, float bri) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * bri));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * bri));
        int b = Math.min(255, Math.round((argb & 0xFF) * bri));
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static float[] faceBrightness() {
        float[] bri = new float[6];
        for (int f = 0; f < 6; f++)
            bri[f] = AMBIENT + DIFFUSE * (float) Math.max(0, dot(FACE_N[f], LIGHT));
        return bri;
    }

    /** Rotate a vector by yaw (about Y) then pitch (about X) — matches the matrix multiply order. */
    private static double[] rotate(double pitch, double yaw, double[] v) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double x1 = v[0] * cy + v[2] * sy, z1 = -v[0] * sy + v[2] * cy, y1 = v[1];
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double y2 = y1 * cp - z1 * sp, z2 = y1 * sp + z1 * cp;
        return new double[]{ x1, y2, z2 };
    }

    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static double[] norm(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[]{ x / l, y / l, z / l };
    }
}
