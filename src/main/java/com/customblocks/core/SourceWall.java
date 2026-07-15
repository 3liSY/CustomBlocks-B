/**
 * SourceWall.java — GROUP_14 §5d TEMPORARY re-source helper (remove when the ~242 job is done).
 *
 * Responsibility: the pure (no-world) half of the "Source Wall" review tool.
 *   1. scope()  — classify a block snapshot into the owner's in-scope target set: blurry (≤256px) BASE
 *                 blocks whose background is black OR whose id ends "_black" — skipping animated blocks
 *                 and colour variants (_red/_green/_yellow/_hex_, which regenerate by recolour). Safe to
 *                 run OFF the server thread (it only reads texture files via TextureStore, like SourceAudit).
 *   2. record persistence — save/load/delete the exact set of block positions + label-entity UUIDs the
 *                 wall placed, so `/cb sourcewall clear` can tear down ONLY what it made, even across a
 *                 restart. Atomic write (temp + move), never throws on read.
 *
 * Why temporary: this is scaffolding for the one-off re-source of the old small-source bases (§5b/§5d).
 * Once that job is finished, this class + SourceWallCommands are deleted (owner ruling 2026-06-28).
 *
 * Depends on: SlotData, TextureStore, Gson (bundled with Minecraft)
 * Called by:  SourceWallCommands (/cb sourcewall, /cb sourcewall clear)
 */
package com.customblocks.core;

import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SourceWall {

    private SourceWall() {} // static-only

    /** Colour-variant id suffixes — these regenerate from their base by recolour, so they are OUT of scope. */
    private static final String[] VARIANT_SUFFIXES = {"_red", "_green", "_yellow"};

    /** The teardown record file — exactly what the wall placed, so clear removes only that. */
    private static final Path RECORD = Path.of("config/customblocks/sourcewall.json");

    private static final Gson GSON = new Gson();

    /** One block on the wall. {@code size} = baked px width; {@code blackId} = id ends "_black"; {@code gif} = animated. */
    public record Target(int index, String id, int size, boolean blackId, boolean gif) {}

    /** scope() outcome: the wall set (black-bg bases + all gifs) + why everything else was skipped. */
    public record ScopeResult(List<Target> targets, int total, int gifs, int blackBases,
                              int skippedVariant, int skippedArabic, int skippedNonBlack) {}

    /** The teardown record: which world + which block positions + which label-entity UUIDs the wall made. */
    public static final class Placed {
        public String dim = "";
        public List<int[]> blocks = new ArrayList<>();  // each = {x, y, z}
        public List<String> stands = new ArrayList<>(); // label armor-stand UUIDs
    }

    // ── scope ────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Classify a snapshot into the wall set: every black-bg / "_black" BASE block (the re-source work) PLUS
     * every animated/GIF block (owner wants all gifs shown too, 2026-06-28). Skips colour variants
     * (_red/_green/_yellow/_hex_ — they regenerate by recolour) and Arabic glyphs (vector-sharp, never
     * re-sourced). Does NOT gate on baked size — server textures bake at 512, so baked width says nothing
     * about whether the SOURCE was small. Reads texture files only — safe OFF the server thread once
     * {@code blocks} has been snapshotted on it. Never throws.
     */
    public static ScopeResult scope(List<SlotData> blocks) {
        List<Target> out = new ArrayList<>();
        int gifs = 0, variants = 0, arabic = 0, nonBlack = 0;
        for (SlotData d : blocks) {
            String id = d.customId();
            if (id == null) continue;
            String low = id.toLowerCase(Locale.ROOT);
            int idx = d.index();
            if (d.isAnimated()) {                              // show ALL gifs
                out.add(new Target(idx, id, TextureStore.bakedWidth(idx), low.endsWith("_black"), true));
                gifs++;
                continue;
            }
            if (low.startsWith("arabic")) { arabic++; continue; }   // vector glyphs, already sharp (§5b)
            if (isColourVariant(low)) { variants++; continue; }     // _red/_green/_yellow/_hex_ regen by recolour
            boolean blackId = low.endsWith("_black");
            if (blackId || borderRendersBlack(idx)) {
                out.add(new Target(idx, id, TextureStore.bakedWidth(idx), blackId, false));
            } else {
                nonBlack++;                                         // coloured bg (flags/photos) → not a "black block", handled separately
            }
        }
        int blackBases = out.size() - gifs;
        // bases first (the actual work), gifs after; within each group blurriest (smallest) first.
        out.sort(Comparator.comparing(Target::gif).thenComparingInt(Target::size).thenComparing(Target::id));
        return new ScopeResult(out, blocks.size(), gifs, blackBases, variants, arabic, nonBlack);
    }

    private static boolean isColourVariant(String low) {
        if (low.contains("_hex_")) return true;
        for (String s : VARIANT_SUFFIXES) if (low.endsWith(s)) return true;
        return false;
    }

    /**
     * True when the texture's 1px outer ring RENDERS BLACK on a block face in-game: either a mostly-opaque
     * near-black border (a literal black background) OR a mostly-transparent border (Minecraft paints
     * transparent block faces black, so e.g. the samosa-on-transparent base still looks like a "black block").
     * Coloured/photo borders (flags, full-bleed photos) are excluded. Conservative: needs >80% of the sampled
     * ring to be transparent, or >80% of the opaque ring pixels to be near-black.
     */
    static boolean borderRendersBlack(int index) {
        try {
            byte[] png = TextureStore.load(index);
            if (png == null || png.length == 0) return false;
            BufferedImage im = ImageIO.read(new ByteArrayInputStream(png));
            if (im == null) return false;
            int w = im.getWidth(), h = im.getHeight();
            if (w < 2 || h < 2) return false;
            int stepX = Math.max(1, w / 64), stepY = Math.max(1, h / 64);
            int[] tally = new int[3]; // {black, opaque, transparent}
            for (int x = 0; x < w; x += stepX) {
                tallyBorderPixel(im, x, 0, tally);          // top + bottom rows
                tallyBorderPixel(im, x, h - 1, tally);
            }
            for (int y = 0; y < h; y += stepY) {
                tallyBorderPixel(im, 0, y, tally);          // left + right columns
                tallyBorderPixel(im, w - 1, y, tally);
            }
            int black = tally[0], opaque = tally[1], trans = tally[2];
            int total = opaque + trans;
            if (total == 0) return false;
            if ((double) trans / total > 0.80) return true;          // transparent bg → renders black on the face
            return opaque > 0 && (double) black / opaque > 0.80;     // literal black bg
        } catch (Exception e) {
            return false;
        }
    }

    /** Count one sampled border pixel into {@code tally} = {black, opaque, transparent}: transparent if α≤200,
     * else opaque (and black too if near-black). */
    private static void tallyBorderPixel(BufferedImage im, int x, int y, int[] tally) {
        int argb = im.getRGB(x, y);
        if (((argb >>> 24) & 0xFF) <= 200) { tally[2]++; return; } // transparent
        tally[1]++;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (Math.max(r, Math.max(g, b)) < 45) tally[0]++;
    }

    // ── teardown record ──────────────────────────────────────────────────────────────────────────────

    public static boolean recordExists() {
        return Files.exists(RECORD);
    }

    /** Atomic write of the teardown record. Returns false on failure (caller should warn). */
    public static boolean saveRecord(Placed p) {
        try {
            Files.createDirectories(RECORD.getParent());
            Path tmp = RECORD.resolveSibling(RECORD.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(p), StandardCharsets.UTF_8);
            Files.move(tmp, RECORD, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Load the teardown record, or null if none / unreadable. Never throws. */
    public static Placed loadRecord() {
        try {
            if (!Files.exists(RECORD)) return null;
            return GSON.fromJson(Files.readString(RECORD, StandardCharsets.UTF_8), Placed.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void deleteRecord() {
        try {
            Files.deleteIfExists(RECORD);
        } catch (Exception ignored) {
        }
    }
}
