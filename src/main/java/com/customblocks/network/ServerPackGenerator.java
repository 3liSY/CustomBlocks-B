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
            byte[] tex = TextureStore.load(i);
            if (tex == null || tex.length == 0) tex = PLACEHOLDER_PNG;
            put(sink, written, tex(key), tex);
            put(sink, written, blockstate(key), blockstateJson(MOD_ID + ":block/" + key));
            // Model selection, in priority order:
            //   animated (G14) → plain cube_all + a sidecar .mcmeta so the strip plays EVERYWHERE
            //                    (world/hand/inventory/creative tab) — wins over shape/face so a
            //                    batch resize or per-face paint can never flatten the strip;
            //   else non-full shape (G08) → a generated shape model (uses the base texture);
            //   else M4 per-face overrides → per-face cube; else the plain cube_all.
            AnimData anim = SlotManager.animFor(i);
            String shape = SlotManager.shapeFor(i);
            if (anim.isAnimated() && TextureStore.has(i)) {
                // Group 14 Phase 1b: the PLACED animated block is drawn off-atlas by AnimSlotBER (crisp,
                // no atlas mipmap muffle), so its world model is INVISIBLE (barrier-style — particle only).
                // The strip + .mcmeta still ship so the INVENTORY/HAND icon animates via the atlas; its
                // item model points straight at the strip (decoupled from the now-invisible block model so
                // the icon still shows). Never emit a frame spec against the 1×1 placeholder.
                put(sink, written, blockModel(key), invisibleBlockModelJson(key));
                put(sink, written, tex(key) + ".mcmeta", mcmetaBytes(anim)); // slot_N.png.mcmeta
                put(sink, written, itemModel(key), cubeAllJson(key)); // decoupled item → atlas-animated icon
            } else if (!com.customblocks.block.BlockShapes.isFull(shape)) {
                put(sink, written, blockModel(key), shapeModelJson(shape, key));
            } else if (TextureStore.hasAnyFace(i)) {
                for (String face : TextureStore.FACES) {
                    byte[] ft = TextureStore.loadFace(i, face);
                    if (ft != null && ft.length > 0) put(sink, written, tex(key + "_" + face), ft);
                }
                put(sink, written, blockModel(key), cubeFacesJson(i, key));
            } else {
                put(sink, written, blockModel(key), cubeAllJson(key));
            }
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
            if (c[1].equalsIgnoreCase(c[2])) continue; // still the default → bundled art is right
            int rgb = Integer.parseInt(c[1].substring(1), 16);
            for (String shape : new String[]{"square", "triangle"}) {
                String rel = "assets/" + MOD_ID + "/textures/item/" + c[0] + "_" + shape + ".png";
                try (var in = ServerPackGenerator.class.getResourceAsStream("/" + rel)) {
                    if (in == null) continue; // bundled art missing — keep whatever the client has
                    put(sink, written, rel, ColorReplacer.tint(in.readAllBytes(), rgb));
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
     * Group 14 Phase 1b: an invisible block model (vanilla barrier pattern — a "particle" texture but no
     * geometry), so a placed animated block draws NOTHING from the pack and AnimSlotBER paints it off-atlas.
     * Particle is the block's strip so break/step particles stay themed.
     */
    private static byte[] invisibleBlockModelJson(String key) {
        JsonObject tex = new JsonObject();
        tex.addProperty("particle", MOD_ID + ":block/" + key);
        JsonObject m = new JsonObject();
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] cubeAllJson(String key) {
        JsonObject tex = new JsonObject();
        tex.addProperty("all", MOD_ID + ":block/" + key);
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/cube_all");
        m.add("textures", tex);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
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
     * Model for a non-full shape (G08). "cross" uses the vanilla cross billboard; every other
     * shape is built from BlockShapes' boxes as model elements, textured with the base on all faces.
     * Geometry comes from the SAME BlockShapes boxes the collision uses, so look matches feel.
     */
    private static byte[] shapeModelJson(String shape, String key) {
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
        m.add("textures", tex);
        JsonArray elements = new JsonArray();
        int[][] boxes = com.customblocks.block.BlockShapes.boxes(shape);
        if (boxes != null) {
            for (int[] b : boxes) elements.add(element(b));
        }
        m.add("elements", elements);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /** One model element (box) with the base texture on all six faces, auto-UV. */
    private static com.google.gson.JsonObject element(int[] b) {
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
            f.addProperty("texture", "#all");
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

    /** A 1x1 PNG used as a placeholder texture for untextured slots. */
    private static final byte[] PLACEHOLDER_PNG = {
        (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
        0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x02,0x00,0x00,0x00,(byte)0x90,0x77,0x53,(byte)0xDE,
        0x00,0x00,0x00,0x0C,0x49,0x44,0x41,0x54,0x08,(byte)0xD7,0x63,(byte)0xF8,(byte)0x0F,(byte)0xF0,
        0x00,0x00,0x00,0x02,0x00,0x01,(byte)0xE2,0x21,(byte)0xBC,0x33,0x00,0x00,0x00,0x00,
        0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82
    };
}
