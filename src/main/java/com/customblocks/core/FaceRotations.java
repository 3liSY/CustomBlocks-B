/**
 * FaceRotations.java
 *
 * Responsibility: The sole reader/writer of per-face quarter-turn rotations (Group 06 §G, the
 * Omni-Tool Face mode). Keyed by slot INDEX (like the per-face texture files in {@link TextureStore})
 * then by face name ("down".."east"); the value is a quarter-turn 1..3 (a face at 0 is never stored —
 * absent means 0). Rotation is rendered natively by the vanilla model face {@code "rotation"} property
 * (see {@link com.customblocks.network.ServerPackGenerator}); no pixels are ever mangled.
 *
 * Persisted to config/customblocks/data/face_rotations.json via atomic write (NFR-13) so rotations
 * survive a restart. A retired/reused slot is cleared via {@link #clear(int)} from
 * {@link TextureStore#delete(int)} so it can never inherit stale rotations.
 *
 * Depends on: Gson (standalone otherwise)
 * Called by:  OmniToolItem (rotate), ServerPackGenerator (read on model build), HistoryCommands
 *             (undo/redo restore), TextureStore (clear on delete).
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class FaceRotations {

    private static final String FILE = "config/customblocks/data/face_rotations.json";
    private static final Gson GSON = new Gson();
    // slot index → (face name → quarter-turn 1..3). A face at 0 is never stored (absent = 0).
    private static final Map<Integer, Map<String, Integer>> ROT = new HashMap<>();

    static { load(); }

    private FaceRotations() {} // static-only

    /** Current quarter-turn (0..3) of one face; 0 when the face has no stored rotation. */
    public static synchronized int get(int index, String face) {
        Map<String, Integer> m = ROT.get(index);
        if (m == null) return 0;
        Integer q = m.get(face);
        return q == null ? 0 : q;
    }

    /** Advance one face clockwise: newQ = (old+1)%4. Stores (or clears at 0), persists, returns newQ. */
    public static synchronized int rotateCw(int index, String face) {
        int newQ = (get(index, face) + 1) % 4;
        set(index, face, newQ);
        return newQ;
    }

    /** Set one face to an absolute quarter-turn. 0 removes the entry (absent = 0). Persists. */
    public static synchronized void set(int index, String face, int q) {
        q = ((q % 4) + 4) % 4;
        Map<String, Integer> m = ROT.computeIfAbsent(index, k -> new HashMap<>());
        if (q == 0) m.remove(face); else m.put(face, q);
        if (m.isEmpty()) ROT.remove(index);
        save();
    }

    // ── G08 §B — packed form for the client sync ─────────────────────────────────────────────────
    // Rotations used to live only in the pack's model JSON (uv + rotation, FaceModelBuilder.element).
    // §B draws non-full shapes at runtime on the CLIENT, which never sees that JSON, so the six
    // quarter-turns ride along on the HudSync slot index as one small int (2 bits per face) and are
    // read back through SlotBlock.resolveFaceRot. Order is vanilla Direction order, so the client can
    // index it straight off a Direction with no name lookup.

    /** The six faces in {@link net.minecraft.util.math.Direction} order — the packing order. */
    public static final String[] PACK_ORDER = { "down", "up", "north", "south", "west", "east" };

    /** All six quarter-turns as one int, 2 bits per face in {@link #PACK_ORDER}. 0 = nothing rotated. */
    public static synchronized int packed(int index) {
        int out = 0;
        for (int i = 0; i < PACK_ORDER.length; i++) out |= (get(index, PACK_ORDER[i]) & 3) << (i * 2);
        return out;
    }

    /** One face's quarter-turn (0..3) back out of {@link #packed}. {@code dirId} is Direction#getId. */
    public static int unpack(int packed, int dirId) {
        if (dirId < 0 || dirId >= PACK_ORDER.length) return 0;
        return (packed >> (dirId * 2)) & 3;
    }

    /** True when at least one face of this slot carries a non-zero rotation. */
    public static synchronized boolean hasAny(int index) {
        Map<String, Integer> m = ROT.get(index);
        return m != null && !m.isEmpty();
    }

    /** Drop every face rotation for a slot (called when the slot's textures are deleted). */
    public static synchronized void clear(int index) {
        if (ROT.remove(index) != null) save();
    }

    // -------------------------------------------------------------------------

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (String idx : o.keySet()) {
                JsonObject faces = o.getAsJsonObject(idx);
                if (faces == null) continue;
                Map<String, Integer> m = new HashMap<>();
                for (String face : faces.keySet()) {
                    int q = ((faces.get(face).getAsInt() % 4) + 4) % 4;
                    if (q != 0) m.put(face, q);
                }
                if (!m.isEmpty()) ROT.put(Integer.parseInt(idx), m);
            }
        } catch (Exception ignored) { /* best-effort load — a bad file just means no rotations */ }
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            for (Map.Entry<Integer, Map<String, Integer>> e : ROT.entrySet()) {
                JsonObject faces = new JsonObject();
                for (Map.Entry<String, Integer> fe : e.getValue().entrySet()) faces.addProperty(fe.getKey(), fe.getValue());
                o.add(String.valueOf(e.getKey()), faces);
            }
            Path tmp = file.resolveSibling("face_rotations.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) { /* best-effort persist */ }
    }
}
