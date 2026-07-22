/**
 * FaceModelBuilder.java
 *
 * Responsibility: Build the block-model JSON for the per-face / shaped / rotated slots — the model
 * variants that carry per-face texture overrides (M4 paint), non-full geometry (G08 shapes), and
 * per-face quarter-turn rotation (G06 §G Face mode). Split out of {@link ServerPackGenerator} to keep
 * that file under the §9.3 monolith cap; the model shapes are all pure {@code (index,key)→byte[]}
 * builders with no pack-sink state, so they live cleanly on their own.
 *
 * Depends on: TextureStore (per-face overrides), FaceRotations (quarter-turns), BlockShapes (geometry).
 * Called by:  ServerPackGenerator.emit (model dispatch).
 */
package com.customblocks.network;

import com.customblocks.CustomBlocksMod;
import com.customblocks.core.FaceRotations;
import com.customblocks.core.TextureStore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public final class FaceModelBuilder {

    private static final Gson GSON = new Gson();
    private static final String MOD_ID = CustomBlocksMod.MOD_ID;

    private FaceModelBuilder() {} // static-only

    /** M4 — full cube model: painted faces point at their override, the rest at the base. */
    public static byte[] cubeFacesJson(int index, String key) {
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

    // G08 §B (2026-07-21): shapeModelJson + stairModelJson were deleted here. The pack no longer emits a
    // per-shape or per-corner model — a static slot always ships the FULL-cube form and the client draws
    // the real shape from BlockShapes at bake time (SlotShapeMesh), which is what makes a shape change
    // reload-free. Per-face overrides and quarter-turns still ride on the cube models below; the runtime
    // mesh samples this model's baked quads for its sprites and reads the turns off the synced value.

    /**
     * G06 §G: a FULL cube with at least one rotated face. {@code minecraft:block/cube} has fixed
     * elements with no per-face slot for a rotation, so a rotated full cube is emitted as an
     * explicit single-element cube (parent {@code block}), reusing {@link #element} for the per-face
     * texture + rotation. Textures map mirrors {@link #cubeFacesJson} (particle+all→base, painted
     * faces→base_"_"face). Paint-only-no-rotation still uses the plain {@link #cubeFacesJson}.
     */
    public static byte[] rotatedCubeJson(int index, String key) {
        String base = MOD_ID + ":block/" + key;
        JsonObject tex = new JsonObject();
        tex.addProperty("particle", base);
        tex.addProperty("all", base);
        for (String face : TextureStore.FACES) {
            if (TextureStore.hasFace(index, face)) tex.addProperty(face, base + "_" + face);
        }
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/block");
        m.add("textures", tex);
        JsonArray elements = new JsonArray();
        elements.add(element(index, new int[]{0, 0, 0, 16, 16, 16}));
        m.add("elements", elements);
        return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * One model element (box): each face uses its #&lt;face&gt; override var when painted, else #all.
     * Auto-UV normally; a face with a stored quarter-turn (G06 §G Face mode) gets an explicit
     * {@code uv:[0,0,16,16]} + {@code rotation} (MC requires a uv present to rotate). A face at 0
     * emits neither, so an un-rotated block's JSON is byte-identical to before.
     */
    private static JsonObject element(int index, int[] b) {
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
            int q = FaceRotations.get(index, face);
            if (q > 0) {
                JsonArray uv = new JsonArray(); uv.add(0); uv.add(0); uv.add(16); uv.add(16);
                f.add("uv", uv);
                f.addProperty("rotation", q * 90);
            }
            faces.add(face, f);
        }
        el.add("faces", faces);
        return el;
    }
}
