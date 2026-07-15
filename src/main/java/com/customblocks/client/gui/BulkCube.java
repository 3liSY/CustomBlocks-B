/**
 * BulkCube.java — Group 07 (Bulk Operations Hub). CLIENT-ONLY.
 *
 * The Hub's spinning-cube primitive (§G27.22 / §G27.22b: every tile + every Bulk-Actions row shows a "slowly
 * rotating 3-D isometric cube … phase-locked to a shared clock"). Rather than reinvent a 3-D renderer this
 * reuses the proven {@link PreviewCube} (baked-atlas textured quads, depth-off, no z-clip — the same renderer
 * the Studio / Recolor / Shape screens use) and {@link LocalTexturePreview} (reads a block's already-baked
 * {@code slot_N.png} straight off the client pack — no network, works on dedicated servers).
 *
 * Phase-lock: the spin angle comes from the wall clock ({@link #yaw()}), shared by every cube drawn this frame,
 * so ticking / filtering / sorting / scrolling can never reset it — a re-shown tile just re-enters mid-rotation.
 *
 * Cost control: each distinct block owns ONE {@link PreviewCube} (its atlas bakes once, then every frame is just
 * six quads at a new yaw); the instances live in an access-order LRU so a scrolled-away block's GL texture is
 * released. {@link #dispose()} MUST be called from the screen's {@code removed()} (render thread).
 *
 * Depends on: PreviewCube, LocalTexturePreview, DrawContext.
 * Called by: BulkWorkbenchView (grid tiles + info panel), BulkOpsView (rows + result preview).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
final class BulkCube {

    private static final long SPIN_MS = 6000L;   // one full revolution every 6s
    private static final float PITCH = 18.0f;    // fixed slight downward iso tilt (isometric read)
    private static final int CACHE_CAP = 96;     // max distinct block atlases kept alive at once
    private static final int GREY = 0x9AA0A6;     // not-yet-synced blocks show a neutral cube

    /** The shared spin angle this instant (degrees). Every cube drawn this frame reads the same value. */
    static double yaw() { return (System.currentTimeMillis() % SPIN_MS) / (double) SPIN_MS * 360.0; }

    // id → downsampled texture grid; a STABLE ref per id so each atlas bakes exactly once (not per frame).
    private final Map<String, int[]> grids = new HashMap<>();
    private final int[] missing = PreviewCube.solid(GREY);   // shared grid for blocks with no local texture

    // id → its atlas-owning cube; access-order LRU disposes the eldest once past CACHE_CAP.
    private final LinkedHashMap<String, PreviewCube> cubes =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PreviewCube> e) {
                    if (size() > CACHE_CAP) { e.getValue().dispose(); return true; }
                    return false;
                }
            };

    /** Draw block {@code id}'s spinning cube centred at (cx,cy), each face {@code half} px from the centre. */
    void render(DrawContext ctx, String id, int cx, int cy, int half) {
        render(ctx, id, cx, cy, half, yaw());
    }

    /** Same, at an explicit yaw (kept so a hero/preview cube can share or override the shared clock). */
    void render(DrawContext ctx, String id, int cx, int cy, int half, double yaw) {
        if (id == null || half <= 0) return;
        int[] grid = gridFor(id);
        PreviewCube c = cubes.get(id);          // access-order LRU: a get() marks this block most-recent
        if (c == null) { c = new PreviewCube(); cubes.put(id, c); }
        c.render(ctx, grid, cx, cy, half, yaw, PITCH, PreviewCube.AS_IS, 0L);
    }

    private int[] gridFor(String id) {
        int[] g = grids.get(id);
        if (g != null) return g;
        int[] loaded = LocalTexturePreview.load(id);
        g = loaded != null ? loaded : missing;   // cache the miss too so we don't re-read the pack every frame
        grids.put(id, g);
        return g;
    }

    // id → cached average texture hue (0..1), for the Blocks List "Sort ▾ · Color" option (§G27.22).
    private final Map<String, Float> hues = new HashMap<>();

    /** The average hue (0..1) of {@code id}'s texture, cached — greys sort to 0. Used by the Color sort. */
    float hue(String id) {
        if (id == null) return 0f;
        Float cached = hues.get(id);
        if (cached != null) return cached;
        int[] g = gridFor(id);
        long r = 0, gg = 0, b = 0;
        int n = 0;
        for (int px : g) { r += (px >> 16) & 0xFF; gg += (px >> 8) & 0xFF; b += px & 0xFF; n++; }
        float h = n == 0 ? 0f : rgbHue((int) (r / n), (int) (gg / n), (int) (b / n));
        hues.put(id, h);
        return h;
    }

    private static float rgbHue(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        if (d == 0f) return 0f;
        float h;
        if (max == rf) h = ((gf - bf) / d) % 6f;
        else if (max == gf) h = (bf - rf) / d + 2f;
        else h = (rf - gf) / d + 4f;
        h /= 6f;
        return h < 0 ? h + 1f : h;
    }

    /** Release every atlas texture. MUST run on the render thread (the screen's removed()). */
    void dispose() {
        for (PreviewCube c : cubes.values()) c.dispose();
        cubes.clear();
    }
}
