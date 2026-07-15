/**
 * ServerPackGenerator.java
 *
 * Responsibility: Build the resource-pack ZIP (in a temp file, then atomic-move) that
 * gives every assigned slot its texture + model, and gives every empty slot a shared
 * invisible model (so Minecraft never logs "missing model" for the unused slots).
 *
 * pack_format 34 = Minecraft 1.21.1. Recycled/condensed from the old project.
 *
 * The build logic lives in emit(PackSink) — the single source of truth for pack contents.
 * generate(File) zips it for the HTTP server; the modded client (ResourcePackGenerator)
 * writes the same files loose, so the local pack can never drift from the HTTP pack.
 *
 * Depends on: SlotManager, TextureStore, CustomBlocksConfig
 * Called by:  ResourcePackServer (build thread, zip) and client ResourcePackGenerator (loose).
 */
package com.customblocks.network;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.CustomBlocksMod;
import com.customblocks.core.AnimData;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.image.ColorReplacer;
import com.customblocks.image.ImageProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ServerPackGenerator {

    private static final Gson GSON = new Gson();
    private static final int PACK_FORMAT = 34; // MC 1.21.1
    private static final String MOD_ID = CustomBlocksMod.MOD_ID;

    private ServerPackGenerator() {} // static-only

    /**
     * Receives one pack file at a time. Two backends use the SAME build logic in {@link #emit}:
     * the server writes a ZIP (this file), the modded client writes loose files
     * (ResourcePackGenerator) — so the local pack can never drift from the HTTP pack.
     */
    @FunctionalInterface
    public interface PackSink {
        /** @param path pack-relative path (e.g. "assets/customblocks/textures/block/slot_0.png"). */
        void put(String path, byte[] data) throws Exception;
    }

    /** Build the pack ZIP to {@code outputFile} (atomic). */
    public static void generate(File outputFile) {
        try {
            long start = System.currentTimeMillis();
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack zip emit start (assigned={}, maxSlots={}, textureSize={}).",
                    SlotManager.usedSlots(), CustomBlocksConfig.maxSlots, CustomBlocksConfig.textureSize);
            if (outputFile.getParentFile() != null) outputFile.getParentFile().mkdirs();
            File tmp = new File(outputFile.getAbsolutePath() + ".tmp");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp))) {
                emit((path, data) -> {
                    zos.putNextEntry(new ZipEntry(path));
                    zos.write(data);
                    zos.closeEntry();
                });
            }
            Files.move(tmp.toPath(), outputFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            CustomBlocksMod.LOGGER.info("[CustomBlocks] Pack zip emit done ({} KB, {} ms).",
                    outputFile.length() / 1024, System.currentTimeMillis() - start);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Failed to generate resource pack", e);
        }
    }

    /**
     * Emit every pack file (pack.mcmeta, per-slot textures/models/blockstates, empty-slot
     * placeholders, re-tinted item art) through {@code sink}. The single source of truth for
     * pack contents; the server zips it, the client writes it loose. Duplicate paths are skipped
     * so one bad entry can't abort the build.
     */
    public static void emit(PackSink sink) throws Exception {
        Set<String> written = new HashSet<>();
        // pack.mcmeta
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", "CustomBlocks");
        JsonObject meta = new JsonObject();
        meta.add("pack", pack);
        put(sink, written, "pack.mcmeta", GSON.toJson(meta).getBytes(StandardCharsets.UTF_8));

        // Assigned slots → real (or placeholder) texture + cube_all model.
        Set<Integer> assigned = new HashSet<>();
        for (SlotData d : SlotManager.assignedSlots()) {
            int i = d.index();
            assigned.add(i);
            String key = "slot_" + i;
            AnimData anim = SlotManager.animFor(i);
            String shape = SlotManager.shapeFor(i);
            byte[] tex = TextureStore.load(i);
            if (tex == null || tex.length == 0) tex = PLACEHOLDER_PNG;
            // G05-5 atlas guard: a STATIC block texture must never exceed textureSize on a side. A correctly
            // baked texture already is ≤ textureSize; this only rescues a legacy/oversized store file so one
            // bad slot can't bloat the atlas/VRAM for every player. Animated slots are skipped — their base is
            // a deliberate off-atlas frame GRID meant to be larger than a single tile.
            boolean animatedGrid = anim.isAnimated() && TextureStore.has(i);
            if (!animatedGrid) tex = clampStaticTexture(tex);
            put(sink, written, tex(key), tex);
            put(sink, written, blockstate(key), blockstateJson(MOD_ID + ":block/" + key));
            // Model selection, in priority order:
            //   animated (G14) → plain cube_all + a sidecar .mcmeta so the strip plays EVERYWHERE
            //                    (world/hand/inventory/creative tab) — wins over shape/face so a
            //                    batch resize or per-face paint can never flatten the strip;
            //   else non-full shape (G08) → a generated shape model (uses the base texture);
            //   else M4 per-face overrides → per-face cube; else the plain cube_all.
            if (animatedGrid) {
                // Group 14: the PLACED animated block AND its ITEM icon both render OFF the block atlas (their
                // own GL texture — all frames, full speed, no atlas muffle). AnimSlotBER paints the world block;
                // the builtin/entity item model routes the icon to SlotItemRenderer. The block model is INVISIBLE
                // so the atlas never stitches the (large) frame grid. A grid sidecar carries the layout + playback;
                // a LEGACY vertical strip (baked before the grid change) still ships its .mcmeta so old animated
                // blocks keep working until they're re-created.
                put(sink, written, blockModel(key), invisibleBlockModelJson());
                put(sink, written, itemModel(key), builtinEntityItemJson());
                int cols = stripCols(tex, anim.frameCount());
                if (cols >= 2) put(sink, written, gridSidecar(key), gridJsonBytes(anim, cols));
                else           put(sink, written, tex(key) + ".mcmeta", mcmetaBytes(anim));
            } else if (!com.customblocks.block.BlockShapes.isFull(shape)) {
                // G08 §3: a non-full shape honors per-face overrides like the full cube; fixes face→setshape "art lost". cross (1 texture) skips.
                if (!com.customblocks.block.BlockShapes.isCross(shape)) writeFacePngs(sink, written, i, key);
                put(sink, written, blockModel(key), shapeModelJson(shape, i, key));
            } else if (d.isArabic()) {
                // G13-25 CP2: an Arabic letter/number slot renders OFF-ATLAS like an animated block —
                // AnimSlotBER draws its faces (glyph facing + readable back via the BlockEntity), which
                // an atlas cube_all cannot do. Invisible block model + builtin/entity item icon
                // (SlotItemRenderer picks it up through StaticFrameCache automatically).
                put(sink, written, blockModel(key), invisibleBlockModelJson());
                put(sink, written, itemModel(key), builtinEntityItemJson());
            } else if (TextureStore.hasAnyFace(i)) {
                writeFacePngs(sink, written, i, key);
                put(sink, written, blockModel(key), cubeFacesJson(i, key));
            } else {
                // Group 14 ADR-012: a STATIC, full-shape, single-texture block — the common /cb create block
                // — renders through the vanilla atlas as a plain cube_all. WORLD model unchanged (keeps chunk-
                // mesh batching — no per-instance GL texture for the common case).
                put(sink, written, blockModel(key), cubeAllJson(key));
                // Group 30 (Guess Mode): route the ITEM icon (hand/inventory/frame/dropped) through
                // SlotItemRenderer so a flagged holder sees the "?" cube there too. SlotItemRenderer falls back
                // to StaticFrameCache.getIconFallback (real slot_N.png, atlas gate bypassed) when not flagged —
                // visually the same cube_all icon this replaces. Placed-in-world rendering is untouched (still
                // the atlas model above); this only changes how the ITEM is drawn.
                put(sink, written, itemModel(key), builtinEntityItemJson());
            }
            // Default item model for the remaining atlas paths (shape / per-face — their normal baked icon,
            // still out of scope for item-level guess disguise). put() skips this as a duplicate when a branch
            // above already wrote the item model (animated / arabic / plain-cube → builtin).
            put(sink, written, itemModel(key), itemJson(MOD_ID + ":block/" + key));
        }

        // Empty slots → one shared placeholder model (purple/black missing-texture look).
        int max = CustomBlocksConfig.maxSlots;
        boolean emptyModel = false;
        for (int i = 0; i < max; i++) {
            if (assigned.contains(i)) continue;
            String key = "slot_" + i;
            if (!emptyModel) {
                put(sink, written, "assets/" + MOD_ID + "/models/block/empty_slot.json", emptyModelJson());
                emptyModel = true;
            }
            put(sink, written, blockstate(key), blockstateJson(MOD_ID + ":block/empty_slot"));
            put(sink, written, itemModel(key), itemJson(MOD_ID + ":block/empty_slot"));
        }

        // M3 hex — when a colour's hex was changed from the shipped default, override
        // that colour's Square/Triangle item art with a re-tinted copy of the bundled art.
        addTintedShapeItems(sink, written);
    }

    /** Re-tinted Square/Triangle item textures for every colour whose hex left its default. */
    private static void addTintedShapeItems(PackSink sink, Set<String> written) throws Exception {
        String[][] colours = {
                {"red",    CustomBlocksConfig.triangleRedHex,    CustomBlocksConfig.TRIANGLE_RED_DEFAULT},
                {"yellow", CustomBlocksConfig.triangleYellowHex, CustomBlocksConfig.TRIANGLE_YELLOW_DEFAULT},
                {"green",  CustomBlocksConfig.triangleGreenHex,  CustomBlocksConfig.TRIANGLE_GREEN_DEFAULT},
                {"black",  CustomBlocksConfig.triangleBlackHex,  CustomBlocksConfig.TRIANGLE_BLACK_DEFAULT},
        };
        for (String[] c : colours) {
            if (c[1].equalsIgnoreCase(c[2])) {
                CustomBlocksMod.LOGGER.info("[CustomBlocks] G06-C tint SKIP {} — still default {}", c[0], c[2]);
                continue; // still the default → bundled art is right
            }
            int rgb = Integer.parseInt(c[1].substring(1), 16);
            for (String shape : new String[]{"square", "triangle"}) {
                String rel = "assets/" + MOD_ID + "/textures/item/" + c[0] + "_" + shape + ".png";
                try (var in = ServerPackGenerator.class.getResourceAsStream("/" + rel)) {
                    if (in == null) {
                        CustomBlocksMod.LOGGER.warn("[CustomBlocks] G06-C tint ART MISSING {} — tool icon can't re-tint", rel);
                        continue; // bundled art missing — keep whatever the client has
                    }
                    int baseRgb = Integer.parseInt(c[2].substring(1), 16); // bundled art's fill = the shipped default
                    put(sink, written, rel, ColorReplacer.tint(in.readAllBytes(), rgb, baseRgb));
                    CustomBlocksMod.LOGGER.info("[CustomBlocks] G06-C tint {} -> {}", rel, c[1]);
                } catch (Exception e) {
                    CustomBlocksMod.LOGGER.warn("[CustomBlocks] Item re-tint failed for {}", rel, e);
                }
            }
        }
    }

    // ── Path helpers ─────────────────────────────────────────────────────────
    private static String tex(String key)        { return "assets/" + MOD_ID + "/textures/block/" + key + ".png"; }
    private static String blockstate(String key) { return "assets/" + MOD_ID + "/blockstates/" + key + ".json"; }
    private static String blockModel(String key) { return "assets/" + MOD_ID + "/models/block/" + key + ".json"; }
    private static String itemModel(String key)  { return "assets/" + MOD_ID + "/models/item/" + key + ".json"; }

    // ── JSON helpers ─────────────────────────────────────────────────────────
    private static byte[] blockstateJson(String modelRef) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelRef);
        JsonObject variants = new JsonObject();
        variants.add("", variant);
        JsonObject bs = new JsonObject();
        bs.add("variants", variants);
        return GSON.toJson(bs).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Group 14 ADR-012: a plain {@code cube_all} block model textured with the slot's strip. For animated
     * slots a sidecar {@code .mcmeta} drives the strip; the atlas builds mipmaps for free, so the placed
     * block is crisp (this is the revert from the off-atlas renderer — see ADR-012).
     */
    private static byte[] cubeAllJson(String key) {
        JsonObject tex = new JsonObject();
        tex.addProperty("all", MOD_ID + ":block/" + key);
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/cube_all");
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Invisible world model for an animated slot: no {@code "parent"} and no elements, so the block atlas
     * never stitches the (large) frame grid and the block draws nothing on its own — {@link
     * com.customblocks.client.render.AnimSlotBER} paints it off-atlas. {@code particle} points at a vanilla
     * atlas texture so break particles still show and the loader logs no missing-texture warning. The absent
     * {@code "parent"} is also the client's off-atlas marker (StaticFrameCache/AnimFrameCache).
     */
    private static byte[] invisibleBlockModelJson() {
        JsonObject tex = new JsonObject();
        tex.addProperty("particle", "minecraft:block/stone");
        JsonObject m = new JsonObject();
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Item model for an off-atlas slot: {@code builtin/entity} routes the icon to the Fabric DynamicItemRenderer
     * ({@link com.customblocks.client.render.SlotItemRenderer}). The display transforms match the off-atlas cube
     * to vanilla block-item sizing in the GUI / hand / ground (same values as the Arabic letter item model).
     */
    private static byte[] builtinEntityItemJson() {
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:builtin/entity");
        JsonObject display = new JsonObject();
        display.add("gui",                   disp(30, 225, 0,  0, 0, 0,   0.625));
        display.add("ground",                disp(0, 0, 0,     0, 3, 0,   0.25));
        display.add("fixed",                 disp(0, 0, 0,     0, 0, 0,   0.5));
        display.add("thirdperson_righthand", disp(75, 45, 0,   0, 2.5, 0, 0.375));
        display.add("thirdperson_lefthand",  disp(75, 45, 0,   0, 2.5, 0, 0.375));
        display.add("firstperson_righthand", disp(0, 45, 0,    0, 0, 0,   0.4));
        display.add("firstperson_lefthand",  disp(0, 225, 0,   0, 0, 0,   0.4));
        m.add("display", display);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject disp(double rx, double ry, double rz,
                                   double tx, double ty, double tz, double scale) {
        JsonObject o = new JsonObject();
        o.add("rotation", arr3(rx, ry, rz));
        o.add("translation", arr3(tx, ty, tz));
        o.add("scale", arr3(scale, scale, scale));
        return o;
    }

    private static JsonArray arr3(double a, double b, double c) {
        JsonArray j = new JsonArray();
        j.add(a); j.add(b); j.add(c);
        return j;
    }

    /** Pack-relative path of the grid layout sidecar for an animated slot ({@code slot_N.grid.json}). */
    private static String gridSidecar(String key) {
        return "assets/" + MOD_ID + "/textures/block/" + key + ".grid.json";
    }

    /**
     * The grid sidecar JSON: total frame {@code count}, grid {@code cols}, a {@code "timebase":"ms"} marker,
     * default {@code frametime} (ms), and the ordered {index,time(ms)} playback (honoring loop/trim/speed).
     * Times are in MILLISECONDS (ADR-014 Step 2) so the off-atlas renderer plays at the clip's true source
     * fps instead of the 20-tps tick ceiling. The client (AnimFrameCache) derives each cell's UV from cols +
     * the image width, then plays this sequence on a millisecond wall-clock. A sidecar written before this
     * change has no {@code timebase} → the client reads its times as ticks (×50) so it still plays correctly
     * until the pack is rebuilt.
     */
    private static byte[] gridJsonBytes(AnimData a, int cols) {
        JsonObject o = new JsonObject();
        o.addProperty("count", a.frameCount());
        o.addProperty("cols", cols);
        o.addProperty("timebase", "ms");
        o.addProperty("frametime", a.baseFrametimeMs());
        JsonArray frames = new JsonArray();
        for (int[] f : a.playbackMs()) {
            JsonObject fo = new JsonObject();
            fo.addProperty("index", f[0]);
            fo.addProperty("time", f[1]);
            frames.add(fo);
        }
        o.add("frames", frames);
        return GSON.toJson(o).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Grid columns for a stored animated texture: {@code 1} = a LEGACY vertical strip (one column, N rows),
     * {@code ≥2} = a packed grid. Detected from the PNG's own IHDR dimensions: a vertical strip is exactly
     * {@code frameCount}× as tall as it is wide; anything else is a grid (cols = ⌈√count⌉, matching the decoder).
     */
    private static int stripCols(byte[] png, int frameCount) {
        if (frameCount <= 1) return 1;
        int w = pngDim(png, 16), h = pngDim(png, 20);   // IHDR: width @16, height @20 (big-endian)
        if (w <= 0 || h <= 0) return 1;                  // unreadable dims → treat as legacy (safe)
        if ((long) w * frameCount == h) return 1;        // exact N-tall single column → legacy vertical strip
        return com.customblocks.image.AnimationDecoder.gridCols(frameCount);
    }

    /** Read a big-endian 4-byte int from a PNG at {@code off} (16 = width, 20 = height in the IHDR chunk). */
    private static int pngDim(byte[] b, int off) {
        if (b == null || b.length < off + 4) return -1;
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    /** Write each painted face's PNG for a slot (shared by the shaped + full-cube per-face model paths). */
    private static void writeFacePngs(PackSink sink, Set<String> written, int index, String key) throws Exception {
        for (String face : TextureStore.FACES) {
            byte[] ft = TextureStore.loadFace(index, face);
            if (ft != null && ft.length > 0) put(sink, written, tex(key + "_" + face), clampStaticTexture(ft));
        }
    }

    /** M4 — full cube model: painted faces point at their override, the rest at the base. */
    private static byte[] cubeFacesJson(int index, String key) {
        String base = MOD_ID + ":block/" + key;
        JsonObject tex = new JsonObject();
        tex.addProperty("particle", base);
        for (String face : TextureStore.FACES) {
            tex.addProperty(face, TextureStore.hasFace(index, face) ? base + "_" + face : base);
        }
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/cube");
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Model for a non-full shape (G08). "cross" = vanilla cross billboard; every other shape is built from
     * BlockShapes' boxes as elements. Faces default to the base (#all); a PAINTED face points at its own var
     * (mirrors {@link #cubeFacesJson}) so per-face art survives a shape change (G08 §3).
     */
    private static byte[] shapeModelJson(String shape, int index, String key) {
        String base = MOD_ID + ":block/" + key;
        JsonObject m = new JsonObject();
        JsonObject tex = new JsonObject();
        tex.addProperty("particle", base);

        if (com.customblocks.block.BlockShapes.isCross(shape)) {
            m.addProperty("parent", "minecraft:block/cross");
            tex.addProperty("cross", base);
            m.add("textures", tex);
            return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
        }

        m.addProperty("parent", "minecraft:block/block");
        tex.addProperty("all", base);
        for (String face : TextureStore.FACES) { // G08 §3: a texture var per painted face (like cubeFacesJson)
            if (TextureStore.hasFace(index, face)) tex.addProperty(face, base + "_" + face);
        }
        m.add("textures", tex);
        JsonArray elements = new JsonArray();
        int[][] boxes = com.customblocks.block.BlockShapes.boxes(shape);
        if (boxes != null) {
            for (int[] b : boxes) elements.add(element(index, b));
        }
        m.add("elements", elements);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /** One model element (box): each face uses its #&lt;face&gt; override var when painted, else #all. Auto-UV. */
    private static com.google.gson.JsonObject element(int index, int[] b) {
        JsonObject el = new JsonObject();
        JsonArray from = new JsonArray();
        from.add(b[0]); from.add(b[1]); from.add(b[2]);
        JsonArray to = new JsonArray();
        to.add(b[3]); to.add(b[4]); to.add(b[5]);
        el.add("from", from);
        el.add("to", to);
        JsonObject faces = new JsonObject();
        for (String face : new String[]{"down", "up", "north", "south", "west", "east"}) {
            JsonObject f = new JsonObject();
            f.addProperty("texture", TextureStore.hasFace(index, face) ? "#" + face : "#all");
            faces.add(face, f);
        }
        el.add("faces", faces);
        return el;
    }

    /**
     * Build the {@code slot_N.png.mcmeta} for an animated slot, deterministically, from the stored
     * numbers (AnimData). Loop / Bounce / Reverse are expressed purely as the frame-INDEX order of
     * the {@code frames} array — no pixel duplication. The top-level {@code frametime} is the default;
     * each {index,time} entry overrides it per displayed frame. This is regenerated on every build,
     * so per-frame timing can never be "lost on save" (the old project's bug).
     */
    private static byte[] mcmetaBytes(AnimData a) {
        JsonObject animation = new JsonObject();
        animation.addProperty("interpolate", a.interpolate());
        animation.addProperty("frametime", a.baseFrametime());
        JsonArray frames = new JsonArray();
        for (int[] f : a.playback()) {
            JsonObject fo = new JsonObject();
            fo.addProperty("index", f[0]);
            fo.addProperty("time", f[1]);
            frames.add(fo);
        }
        animation.add("frames", frames);
        JsonObject root = new JsonObject();
        root.add("animation", animation);
        return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] itemJson(String parent) {
        JsonObject m = new JsonObject();
        m.addProperty("parent", parent);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] emptyModelJson() {
        // References a texture that is intentionally absent from the pack.
        // Minecraft renders the standard purple/black missing-texture checkerboard,
        // which is the correct placeholder appearance for deleted or unassigned slots.
        JsonObject tex = new JsonObject();
        tex.addProperty("all", MOD_ID + ":block/placeholder");
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/cube_all");
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    private static void put(PackSink sink, Set<String> written, String path, byte[] data) throws Exception {
        if (!written.add(path)) return; // skip duplicates so one bad entry can't abort the build
        sink.put(path, data);
    }

    /**
     * G05-5 atlas guard: downscale {@code png} to a {@link CustomBlocksConfig#textureSize}-square texture
     * when it exceeds that size on either side; otherwise return it untouched (the common case — one cheap
     * IHDR read, no decode). Stops a legacy or imported block texture stored larger than the configured size
     * from bloating the block atlas / VRAM for every player. Never throws: on a resize failure it logs and
     * ships the original rather than aborting the whole pack build.
     */
    private static byte[] clampStaticTexture(byte[] png) {
        int max = CustomBlocksConfig.textureSize;
        int w = ImageProcessor.pngWidth(png), h = ImageProcessor.pngHeight(png);
        if (w <= max && h <= max) return png; // within budget (or unreadable → leave it) — no work
        try {
            byte[] fixed = ImageProcessor.toBlockPng(png, max);
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] Downscaled oversized block texture {}x{} -> {}px on emit (atlas guard).", w, h, max);
            return fixed;
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.error("[CustomBlocks] Could not downscale oversized texture ({}x{}); shipping as-is.", w, h, e);
            return png;
        }
    }

    /** A 1x1 PNG used as a placeholder texture for untextured slots. */
    private static final byte[] PLACEHOLDER_PNG = {
        (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
        0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x02,0x00,0x00,0x00,(byte)0x90,0x77,0x53,(byte)0xDE,
        0x00,0x00,0x00,0x0C,0x49,0x44,0x41,0x54,0x08,(byte)0xD7,0x63,(byte)0xF8,(byte)0x0F,(byte)0xF0,
        0x00,0x00,0x00,0x02,0x00,0x01,(byte)0xE2,0x21,(byte)0xBC,0x33,0x00,0x00,0x00,0x00,
        0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82
    };
}
