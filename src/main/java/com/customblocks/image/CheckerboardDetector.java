/**
 * CheckerboardDetector.java — G10-6 fix (v3 + speck-cleanup).
 *
 * Responsibility: recognise a "flattened transparency-checkerboard" source — an OPAQUE image whose
 * background is the editor's transparency checkerboard baked into pixels (no alpha), the kind that
 * stock sites (rawpixel `image_png_800`, pngtree `ourlarge`, citypng) hand out as a *preview*
 * instead of a real transparent PNG — and clean that checkerboard to solid BLACK so it bakes like a
 * normal block instead of rendering the checkerboard in-world.
 *
 * Light AND dark grids: the grid is drawn in the source site's own theme, so it arrives either as the
 * classic near-white tone pair or, on dark-themed sites, as a near-black one (citypng measures 25 vs 40).
 * detect() therefore tries each brightness regime in turn. Only the brightness gate differs between them
 * — neutrality, the two-tone border split, the L* gap and the periodic switch run are applied unchanged
 * inside whichever regime is being tested, so a normal image still cannot match either way.
 *
 * Why v3 (not the old edge-flood): the earlier fix flooded the checkerboard inward from the image
 * border, so checker "pockets" walled off from the border by the subject (e.g. a gap between glyphs)
 * could not be reached and stayed in the bake. v3 is connectivity-free: it builds a neutral, near-tone
 * MASK over the whole image, finds connected components, and kills any component that genuinely carries
 * BOTH checker tones (a real 2-tone checker patch) — border patches AND trapped pockets alike — while a
 * single-tone subject white (text, planet body) survives. A short feather then peels residual checker
 * bleed off the subject edge without darkening the subject itself (a bright-neutral GUARD protects white
 * text edges and a planet limb). detect() stays the strict gate so this never touches a normal image.
 *
 * Why the final speck-cleanup: webp/jpeg compression tints some checker pixels just off-neutral
 * (e.g. chroma 11 > the mask's 10), so they escape the mask and survive as tiny light dots scattered on
 * the black. After v3 the legitimate subject is one large connected mass, so any *small isolated* blob
 * left on the background is residue: cleanup paints black every small light-neutral blob (and any ≤4px
 * speck) that is not the main subject. Removes the dot fuzz; cannot touch text/arrow/planet (all part of
 * the one big blob). Whole pass is gated behind detect() — it only runs on a confirmed checkerboard.
 *
 * Step 8 (2026-07-28) writes the cleaned grid TRANSPARENT as well as black. Painting it black was the
 * whole fix while the only job was to stop rendering a checkerboard in-world; once background removal
 * runs afterwards it is actively harmful, because the removal is told nothing and must rediscover the
 * background from colour — and then floods from the flattened grid straight into any BLACK ARTWORK
 * touching it (measured: the owner's football lost whole dark panels). This class already KNOWS which
 * pixels were grid, so it hands that over as authored transparency and rung 1 decides on a fact. The
 * RGB stays black, so anything reading colour alone sees exactly what it saw before. Only the grid is
 * declared — the two cleanup steps also paint black, but they scrub grain and specks, some of it inside
 * the artwork, and declaring those punched holes through it.
 *
 * Called by: BackgroundRemover.process (flattenToBlack, before the mode-gated flood).
 *            isFlattened() has no caller since G10 §G removed the "that was a flattened preview image"
 *            chat notice (owner, 2026-07-25); the detection it exposes is unchanged and still available.
 */
package com.customblocks.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class CheckerboardDetector {

    private CheckerboardDetector() {} // static-only

    // ── detection thresholds (validated offline on the rawpixel + pngtree previews) ──
    private static final int    LIGHT_MIN         = 190;  // a "light" pixel has max channel ≥ this
    /** A "dark" pixel has max channel ≤ this. Dark-themed stock sites (citypng) ship their transparency
     *  grid as two near-black greys instead of the classic near-white pair; measured tones are 25 and 40,
     *  so the ceiling sits well above them while staying far below any mid-tone image content. */
    private static final int    DARK_MAX          = 96;
    private static final int    NEUTRAL_DETECT    = 18;   // detection: max-min ≤ this = near-neutral
    private static final double BORDER_LIGHT_FRAC = 0.85; // ≥ this fraction of the border ring must be in-regime
    private static final double TONE_MIN_FRAC     = 0.20; // each of the two tones ≥ this fraction of light border pixels
    private static final double DL_MIN            = 2.0;  // the two tones must differ by L* in [DL_MIN, DL_MAX]
    private static final double DL_MAX            = 22.0; // distinct but close (a true checker is two near-white greys)
    private static final int    MIN_ALT           = 4;    // ≥ this many tone switches along a border line = periodic grid
    /** The two tone peaks must be at least this many luminance buckets apart. A lossy source smears one
     *  tone over its neighbouring buckets, so without this the "second biggest bucket" is that smear
     *  rather than the other tone, and the L* gap test then sees two near-identical greys. */
    private static final int    MIN_BUCKET_SEP    = 2;
    private static final int    OPAQUE_ALPHA      = 250;  // alpha ≥ this = opaque

    // ── v3 flatten params (validated offline: share_real.webp, uranus_real.png) ──
    private static final int    NEUTRAL_V3   = 10;    // mask chroma gate: max-min ≤ this
    private static final double MASK_TOL_DE  = 14.0;  // mask ΔE: pixel within this of either tone
    /** Verdict ΔE — how close a pixel must actually be to a checker tone before it is removed as grid.
     *  {@link #MASK_TOL_DE} is deliberately generous because it builds a CANDIDATE set and has to hold
     *  a lossy grid together; as a verdict it is far too loose. Measured on the owner's football, the
     *  ball's white limb reads 200–217 against grid tones of 235 and 255, which is 12 ΔE away — inside
     *  the candidate bar, plainly not the backdrop — and the ball baked with bites out of its edge. */
    private static final double KILL_TOL_DE  = 6.0;
    private static final int    KILL_MIN_TONE = 8;    // a killed component needs ≥ this many px of EACH tone
    private static final double KILL_MIN_FRAC = 0.10; // …and the minority tone ≥ this fraction of the component
    private static final int    FEATHER_R    = 3;     // feather radius (px) out from killed pixels
    /** At or below this subject coverage a boundary pixel is grid, not a thin edge — and dividing by it
     *  would amplify the little colour that is there into a bright fringe. A majority statement, the
     *  same one rung 5's snap makes: a pixel the solve measures as more grid than artwork IS grid.
     *  At 0.06 the pixels between kept a fraction of the grid's own tone and drew the soft pale fringe
     *  the owner reported round the ball and the subscribe bell. It can only reach pixels the solve has
     *  already explained as a mixture of this grid tone and this artwork. */
    private static final double COVER_FLOOR  = 0.50;

    private static final int    BLACK        = 0xFF000000;

    /** Two background tones (CIE-LAB) of a detected checkerboard. */
    private record Tones(double[] a, double[] b) {}

    /** True if {@code input} is a flattened transparency-checkerboard preview (used to warn the player). */
    public static boolean isFlattened(byte[] input) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return false;
            BufferedImage img = toArgb(src);
            return detect(img, img.getWidth(), img.getHeight()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * If {@code input} is a flattened checkerboard, return PNG bytes with the checkerboard painted
     * opaque BLACK; otherwise return {@code input} unchanged. Never throws — a failure leaves the
     * image untouched so it can never break a retexture.
     */
    public static byte[] flattenToBlack(byte[] input) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) return input;
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage img = toArgb(src);
            Tones tones = detect(img, w, h);
            if (tones == null) return input; // not a checkerboard → leave it for the normal pipeline

            int n = w * h;
            int[] argb = img.getRGB(0, 0, w, h, null, 0, w);

            boolean[] kill = new boolean[n];
            CheckerLattice.Fit fit = computeKill(argb, w, h, n, tones, kill); // steps 2–3
            boolean declare = coversBorder(kill, w, h);          // may this be handed on as fact?
            // Asked BEFORE the silhouette repair on purpose: coversBorder answers "was the grid found
            // at all", which is a question about the colour verdict. The repair below spares grid that
            // is the artwork's own colour, and near a subject running off the picture edge that is some
            // of the border ring — letting it move the answer would mean a well-found grid stopped
            // being declared because the ball touches the frame.
            CheckerSilhouette.resolve(kill, fit, argb, w, h, tones.a(), tones.b());
            if (declare) CheckerLattice.strandEnclosed(kill, fit, w, h);
            feather(argb, w, h, n, kill, tones, declare);        // steps 4–5
            for (int i = 0; i < n; i++) if (kill[i]) argb[i] = BLACK; // step 6
            CheckerScrub.run(argb, w, h, n, kill, declare);      // steps 7a–7b: residue, confined to the grid
            if (declare) for (int i = 0; i < n; i++) if (kill[i]) argb[i] = BLACK & 0x00FFFFFF; // step 8

            img.setRGB(0, 0, w, h, argb, 0, w);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            return input;
        }
    }

    // ── v3 steps 2–3: mask → connected components → kill genuine 2-tone patches ──────────────────────
    private static CheckerLattice.Fit computeKill(int[] argb, int w, int h, int n, Tones t,
                                                  boolean[] kill) {
        // step 2: neutral, near-tone mask
        boolean[] mask = new boolean[n];
        for (int i = 0; i < n; i++) {
            int p = argb[i];
            if (((p >>> 24) & 0xFF) < OPAQUE_ALPHA) continue;
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > NEUTRAL_V3) continue;
            double[] lab = labOf(p);
            if (Math.min(de(lab, t.a()), de(lab, t.b())) <= MASK_TOL_DE) mask[i] = true;
        }
        // step 3: 4-connected components; kill any that carries both tones
        boolean[] seen = new boolean[n];
        int[] comp = new int[n]; // reused per component as a BFS queue + member list
        for (int s = 0; s < n; s++) {
            if (!mask[s] || seen[s]) continue;
            int head = 0, tail = 0;
            comp[tail++] = s; seen[s] = true;
            while (head < tail) {
                int idx = comp[head++], x = idx % w, y = idx / w;
                if (x + 1 < w && mask[idx + 1] && !seen[idx + 1]) { seen[idx + 1] = true; comp[tail++] = idx + 1; }
                if (x > 0     && mask[idx - 1] && !seen[idx - 1]) { seen[idx - 1] = true; comp[tail++] = idx - 1; }
                if (y + 1 < h && mask[idx + w] && !seen[idx + w]) { seen[idx + w] = true; comp[tail++] = idx + w; }
                if (y > 0     && mask[idx - w] && !seen[idx - w]) { seen[idx - w] = true; comp[tail++] = idx - w; }
            }
            int nA = 0, nB = 0;
            for (int k = 0; k < tail; k++) {
                double[] lab = labOf(argb[comp[k]]);
                if (de(lab, t.a()) <= de(lab, t.b())) nA++; else nB++;
            }
            if (nA >= KILL_MIN_TONE && nB >= KILL_MIN_TONE && Math.min(nA, nB) >= KILL_MIN_FRAC * tail) {
                for (int k = 0; k < tail; k++) kill[comp[k]] = true;
            }
        }
        // A killed pixel must itself BE one of the two tones, not merely near enough to have joined the
        // candidate mask. Everything above reasons about components, and a component is only as honest
        // as its loosest member.
        for (int i = 0; i < n; i++) {
            if (!kill[i]) continue;
            double[] lab = labOf(argb[i]);
            if (Math.min(de(lab, t.a()), de(lab, t.b())) > KILL_TOL_DE) kill[i] = false;
        }
        // The colour verdict is a candidate list, not a decision: artwork that shares a tone with the
        // grid lands in the same component. CheckerLattice keeps only what is geometrically a grid.
        return CheckerLattice.confine(kill, mask, argb, w, h, n, t.a(), t.b());
    }

    /**
     * May the grid this pass found be handed on as authored transparency?
     *
     * <p>Only when it accounts for the border ring. detect() fires on a border that is ≥85% grid, so on
     * a source whose grid was properly identified the kill covers about that much of the ring as well.
     * A lossy source can smear the tones far enough that detect() still recognises the pattern while the
     * per-pixel mask catches almost none of it — measured on the chrome JPEG, where the kill reaches a
     * few percent. Declaring that as the file's own transparency states a near-empty background as FACT,
     * and rung 1 rightly believes facts: the picture came back with 1.6% removed instead of 81%.
     *
     * <p>Where this returns false the pass still blacks the checkerboard out, which is all it ever did
     * before the hand-off existed, and the colour rungs decide the background as they did then.
     */
    private static boolean coversBorder(boolean[] kill, int w, int h) {
        long ring = 0, killed = 0;
        for (int x = 0; x < w; x++) {
            ring += 2;
            if (kill[x]) killed++;
            if (kill[(h - 1) * w + x]) killed++;
        }
        for (int y = 1; y < h - 1; y++) {
            ring += 2;
            if (kill[y * w]) killed++;
            if (kill[y * w + w - 1]) killed++;
        }
        return ring > 0 && killed >= ring * BORDER_LIGHT_FRAC;
    }

    // ── v3 steps 4–5: multi-source BFS from kill pixels, then peel checker bleed off subject edge ────
    private static void feather(int[] argb, int w, int h, int n, boolean[] kill, Tones t, boolean declare) {
        int[] dist = new int[n]; Arrays.fill(dist, Integer.MAX_VALUE);
        int[] near = new int[n]; // nearest kill pixel's ORIG argb (local checker tone)
        // Presence is its own flag, never a reserved colour. A packed opaque WHITE pixel is 0xFFFFFFFF,
        // which as a signed int IS -1, so a -1 sentinel collides with the commonest grid tone there is:
        // measured 2026-07-29, 63180 of the share button's 143912 grid pixels and 44080 of the subscribe
        // banner's 95060 are exactly that value. Every boundary pixel whose nearest grid square was pure
        // white therefore read as "no grid tone carried here", the solve was skipped, and the mixture
        // baked opaque — 484 pale pixels tracing the share arrow, which is the outline the owner reported.
        boolean[] hasNear = new boolean[n];
        int[] q = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) if (kill[i]) { dist[i] = 0; near[i] = argb[i]; hasNear[i] = true; q[tail++] = i; }
        while (head < tail) {
            int idx = q[head++];
            if (dist[idx] >= FEATHER_R) continue;
            int x = idx % w, y = idx / w, nd = dist[idx] + 1, nc = near[idx];
            if (x + 1 < w && dist[idx + 1] > nd) { dist[idx + 1] = nd; near[idx + 1] = nc; hasNear[idx + 1] = true; q[tail++] = idx + 1; }
            if (x > 0     && dist[idx - 1] > nd) { dist[idx - 1] = nd; near[idx - 1] = nc; hasNear[idx - 1] = true; q[tail++] = idx - 1; }
            if (y + 1 < h && dist[idx + w] > nd) { dist[idx + w] = nd; near[idx + w] = nc; hasNear[idx + w] = true; q[tail++] = idx + w; }
            if (y > 0     && dist[idx - w] > nd) { dist[idx - w] = nd; near[idx - w] = nc; hasNear[idx - w] = true; q[tail++] = idx - w; }
        }
        // The artwork's colour at each feather pixel, propagated the same way the grid's tone is: from
        // the NEAREST unmixed piece of artwork, not from an average of everything nearby. Averaging a
        // window that straddles two parts of the artwork — the share arrow's white face and its red
        // outline — invents a pink that is in neither, and the solve then cannot explain the pixel and
        // declines, which is what left pale steps along that outline (measured 2026-07-28).
        int[] art = new int[n];
        boolean[] hasArt = new boolean[n]; // same reason as hasNear: white artwork packs to the old -1
        head = 0; tail = 0;
        int[] adist = new int[n]; Arrays.fill(adist, Integer.MAX_VALUE);
        for (int i = 0; i < n; i++) {
            if (!kill[i] && dist[i] > FEATHER_R) { adist[i] = 0; art[i] = argb[i]; hasArt[i] = true; q[tail++] = i; }
        }
        while (head < tail) {
            int idx = q[head++];
            if (adist[idx] >= FEATHER_R) continue;
            int x = idx % w, y = idx / w, nd = adist[idx] + 1, nc = art[idx];
            if (x + 1 < w && adist[idx + 1] > nd) { adist[idx + 1] = nd; art[idx + 1] = nc; hasArt[idx + 1] = true; q[tail++] = idx + 1; }
            if (x > 0     && adist[idx - 1] > nd) { adist[idx - 1] = nd; art[idx - 1] = nc; hasArt[idx - 1] = true; q[tail++] = idx - 1; }
            if (y + 1 < h && adist[idx + w] > nd) { adist[idx + w] = nd; art[idx + w] = nc; hasArt[idx + w] = true; q[tail++] = idx + w; }
            if (y > 0     && adist[idx - w] > nd) { adist[idx - w] = nd; art[idx - w] = nc; hasArt[idx - w] = true; q[tail++] = idx - w; }
        }

        for (int i = 0; i < n; i++) {
            if (kill[i] || dist[i] > FEATHER_R || !hasNear[i]) continue;
            int p = argb[i];
            // Coverage is SOLVED, not guessed from a colour distance. The old ramp read "how far is this
            // pixel from the grid's tone" as "how much subject is in it", and those are not the same
            // question: a red button half covered by a white grid sits a long way from white, so the ramp
            // called it fully covered, left it opaque, and divided the grid's colour back out of it —
            // which lightens it. That is the pale outline the owner reported round the share button and
            // the subscribe banner (measured 2026-07-28: boundary pixels baking as EEDDDD and E4A1A1
            // against a DB1313 button). The matting solve answers the coverage question directly, from
            // the grid tone on one side and the artwork's own colour on the other.
            double[] bg = EdgeMix.linear(near[i]);
            double[] fg = hasArt[i] ? EdgeMix.linear(art[i]) : null;
            // No artwork nearby to interpolate against, or artwork the same colour as the grid — a white
            // caption on a white grid tone. Both are the case the old brightness GUARD was written for,
            // and both are answered here by the solve declining rather than by a brightness constant.
            if (fg == null || !EdgeMix.separated(bg, fg)) continue;
            double a = EdgeMix.coverage(p, bg, fg);
            if (a < 0 || !EdgeMix.explains(p, bg, fg, a)) continue; // not a mixture of these two
            if (!declare) {
                // The grid was not identified well enough to be stated as fact, so there is no alpha
                // channel to write coverage into: peel as before and leave the edge to the colour rungs,
                // which run their own unmixing over whatever they decide.
                argb[i] = 0xFF000000
                        | (EdgeMix.encode(LinearBlend.toLinear(p >> 16) - (1 - a) * bg[0]) << 16)
                        | (EdgeMix.encode(LinearBlend.toLinear(p >> 8)  - (1 - a) * bg[1]) << 8)
                        |  EdgeMix.encode(LinearBlend.toLinear(p)       - (1 - a) * bg[2]);
                continue;
            }
            if (a < COVER_FLOOR) { kill[i] = true; continue; } // all backdrop — it belongs to the grid
            // Coverage, not darkening. `a` is how much of this boundary pixel the subject actually
            // occupies; the rest is grid showing through. Subtracting the grid's colour and leaving the
            // pixel OPAQUE — what this did until 2026-07-28 — writes the premultiplied value at full
            // strength, which composites as a dark rim on every soft edge (measured: a ring round the
            // owner's football, speckles along the share arrow's outline). Dividing the coverage back
            // out and recording it in the alpha channel is the matting equation, and rung 1 carries the
            // raster through untouched, so the edge lands in the bake as the fraction it really is.
            argb[i] = (clampByte(a * 255) << 24) | (EdgeMix.recover(p, bg, a) & 0xFFFFFF);
        }
    }


    private static int clampByte(double v) { int i = (int) Math.round(v); return i < 0 ? 0 : (i > 255 ? 255 : i); }

    // ── detection ────────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the two background tones if {@code img} is a flattened 2-tone neutral checkerboard, else null.
     *
     * <p>A preview grid is drawn in whatever theme its site uses, so the same pattern arrives either as the
     * classic near-white pair or as a near-black one. Both are tried; everything that makes the test strict
     * — neutrality, two tones that each hold a real share of the border, a small L* gap, and a periodic run
     * of switches — is applied identically inside each regime, so widening the brightness gate does not
     * loosen the signature. Light is tried first because it is by far the common case.
     */
    private static Tones detect(BufferedImage img, int w, int h) {
        if (w < 16 || h < 16) return null;
        if (hasRealTransparency(img, w, h)) return null; // a genuine transparent PNG → leave it alone
        Tones light = detectRegime(img, w, h, false);
        return light != null ? light : detectRegime(img, w, h, true);
    }

    /** One brightness regime's pass of the checkerboard signature ({@code dark} = near-black tone pair). */
    private static Tones detectRegime(BufferedImage img, int w, int h, boolean dark) {
        // Border ring: how much of it is in-regime neutral, and which luminance buckets dominate?
        long[] hist = new long[64]; // luminance / 4
        long lightCount = 0, borderCount = 0;
        for (int x = 0; x < w; x++) { borderCount += 2; lightCount += tallyBorder(img.getRGB(x, 0), hist, dark); lightCount += tallyBorder(img.getRGB(x, h - 1), hist, dark); }
        for (int y = 1; y < h - 1; y++) { borderCount += 2; lightCount += tallyBorder(img.getRGB(0, y), hist, dark); lightCount += tallyBorder(img.getRGB(w - 1, y), hist, dark); }
        if (lightCount < borderCount * BORDER_LIGHT_FRAC) return null;

        // Two tone peaks. The second peak must sit at least MIN_BUCKET_SEP buckets away from the first:
        // a lossy source (JPEG thumbnail, webp) smears each tone across its neighbouring buckets, so the
        // plain "two biggest buckets" pick would otherwise return one tone and its own ringing.
        int b1 = -1;
        for (int i = 0; i < 64; i++) if (b1 < 0 || hist[i] > hist[b1]) b1 = i;
        int b2 = -1;
        for (int i = 0; i < 64; i++) {
            if (Math.abs(i - b1) < MIN_BUCKET_SEP) continue;
            if (b2 < 0 || hist[i] > hist[b2]) b2 = i;
        }
        if (b1 < 0 || b2 < 0 || hist[b1] == 0 || hist[b2] == 0) return null;

        int c1 = b1 * 4, c2 = b2 * 4; // bucket centre luminances

        // Tone share is measured over ALL in-regime border mass, each pixel assigned to whichever peak it
        // is nearer — the same split the periodicity test uses. Counting only the two peak buckets would
        // reject a genuine checkerboard whose tones are smeared by lossy compression (a Google-image JPEG
        // thumbnail of the same PNG spreads one tone over four buckets and drops it under the threshold).
        long nearC1 = 0, nearC2 = 0;
        for (int i = 0; i < 64; i++) {
            if (hist[i] == 0) continue;
            int lum = i * 4;
            if (Math.abs(lum - c1) <= Math.abs(lum - c2)) nearC1 += hist[i]; else nearC2 += hist[i];
        }
        if (nearC1 < lightCount * TONE_MIN_FRAC || nearC2 < lightCount * TONE_MIN_FRAC) return null;
        double l1 = labOf(0xFF000000 | gray(c1))[0];
        double l2 = labOf(0xFF000000 | gray(c2))[0];
        double gap = Math.abs(l1 - l2);
        if (gap < DL_MIN || gap > DL_MAX) return null;

        // Periodicity: a true checkerboard switches tone repeatedly along a border line; two solid
        // halves (a normal image) do not. BOTH axes must switch, because a checkerboard is periodic in
        // two dimensions — accepting either axis alone also accepts plain STRIPES, and striped artwork
        // in these tones is a design a player may genuinely want (it was flattened as a false positive
        // in both brightness regimes while this took the better axis).
        int altTop = alternations(img, w, h, true, c1, c2, dark);
        int altLeft = alternations(img, w, h, false, c1, c2, dark);
        if (Math.min(altTop, altLeft) < MIN_ALT) return null;

        return new Tones(meanToneLab(img, w, h, b1, dark), meanToneLab(img, w, h, b2, dark));
    }

    /** Neutral (near-grey) AND inside the requested brightness regime — the gate every detection step shares. */
    private static boolean inRegime(int r, int g, int b, boolean dark) {
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        if (mx - mn > NEUTRAL_DETECT) return false;
        return dark ? mx <= DARK_MAX : mx >= LIGHT_MIN;
    }

    /** Count this border pixel into the luminance histogram if it is in-regime; returns 1 if it is, else 0. */
    private static long tallyBorder(int argb, long[] hist, boolean dark) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (!inRegime(r, g, b, dark)) return 0;
        hist[luminance(r, g, b) / 4]++;
        return 1;
    }

    /** Tone switches along the top row (horizontal=true) or left column, classifying each in-regime pixel by nearest tone. */
    private static int alternations(BufferedImage img, int w, int h, boolean horizontal, int c1, int c2, boolean dark) {
        int n = horizontal ? w : h, switches = 0, prev = 0;
        for (int i = 0; i < n; i++) {
            int argb = horizontal ? img.getRGB(i, 0) : img.getRGB(0, i);
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            if (!inRegime(r, g, b, dark)) { prev = 0; continue; }
            int lum = luminance(r, g, b);
            int cls = Math.abs(lum - c1) <= Math.abs(lum - c2) ? 1 : 2;
            if (prev != 0 && cls != prev) switches++;
            prev = cls;
        }
        return switches;
    }

    /** Mean RGB (as LAB) of the in-regime border pixels whose luminance bucket is within ±1 of {@code bucket}. */
    private static double[] meanToneLab(BufferedImage img, int w, int h, int bucket, boolean dark) {
        long sr = 0, sg = 0, sb = 0, n = 0;
        for (int x = 0; x < w; x++) { long[] a = sampleTone(img.getRGB(x, 0), bucket, dark); sr += a[0]; sg += a[1]; sb += a[2]; n += a[3];
                                      long[] c = sampleTone(img.getRGB(x, h - 1), bucket, dark); sr += c[0]; sg += c[1]; sb += c[2]; n += c[3]; }
        for (int y = 1; y < h - 1; y++) { long[] a = sampleTone(img.getRGB(0, y), bucket, dark); sr += a[0]; sg += a[1]; sb += a[2]; n += a[3];
                                          long[] c = sampleTone(img.getRGB(w - 1, y), bucket, dark); sr += c[0]; sg += c[1]; sb += c[2]; n += c[3]; }
        if (n == 0) return labOf(0xFF000000 | gray(bucket * 4));
        return labOf(0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n));
    }

    private static long[] sampleTone(int argb, int bucket, boolean dark) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (!inRegime(r, g, b, dark)) return new long[]{0, 0, 0, 0};
        if (Math.abs(luminance(r, g, b) / 4 - bucket) > 1) return new long[]{0, 0, 0, 0};
        return new long[]{r, g, b, 1};
    }

    /** Sample a grid; true if a meaningful fraction of pixels are non-opaque (a real transparent image). */
    private static boolean hasRealTransparency(BufferedImage img, int w, int h) {
        int stepX = Math.max(1, w / 50), stepY = Math.max(1, h / 50);
        long total = 0, trans = 0;
        for (int x = 0; x < w; x += stepX) {
            for (int y = 0; y < h; y += stepY) {
                total++;
                if (((img.getRGB(x, y) >>> 24) & 0xFF) < OPAQUE_ALPHA) trans++;
            }
        }
        return total > 0 && (double) trans / total > 0.01;
    }

    private static int luminance(int r, int g, int b) { return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b); }
    private static int gray(int v) { int c = Math.min(255, Math.max(0, v)); return (c << 16) | (c << 8) | c; }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    // ── CIE-LAB (sRGB → XYZ → L*a*b*, D65) + Euclidean ΔE — same formula as BackgroundRemover ───
    /** Package-visible for CheckerLattice, which asks the same colour questions. */
    static double[] labOf(int argb) {
        double rF = ((argb >> 16) & 0xFF) / 255.0;
        double gF = ((argb >> 8) & 0xFF) / 255.0;
        double bF = (argb & 0xFF) / 255.0;
        rF = (rF > 0.04045) ? Math.pow((rF + 0.055) / 1.055, 2.4) : (rF / 12.92);
        gF = (gF > 0.04045) ? Math.pow((gF + 0.055) / 1.055, 2.4) : (gF / 12.92);
        bF = (bF > 0.04045) ? Math.pow((bF + 0.055) / 1.055, 2.4) : (bF / 12.92);
        rF *= 100.0; gF *= 100.0; bF *= 100.0;
        double x = rF * 0.4124 + gF * 0.3576 + bF * 0.1805;
        double y = rF * 0.2126 + gF * 0.7152 + bF * 0.0722;
        double z = rF * 0.0193 + gF * 0.1192 + bF * 0.9505;
        x /= 95.047; y /= 100.000; z /= 108.883;
        x = (x > 0.008856) ? Math.cbrt(x) : (7.787 * x) + (16.0 / 116.0);
        y = (y > 0.008856) ? Math.cbrt(y) : (7.787 * y) + (16.0 / 116.0);
        z = (z > 0.008856) ? Math.cbrt(z) : (7.787 * z) + (16.0 / 116.0);
        return new double[]{(116.0 * y) - 16.0, 500.0 * (x - y), 200.0 * (y - z)};
    }

    static double de(double[] a, double[] b) {
        return Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
    }
}
