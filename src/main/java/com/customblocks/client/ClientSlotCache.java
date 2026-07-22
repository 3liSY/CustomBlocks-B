/**
 * ClientSlotCache.java
 *
 * Responsibility: client-side cache of the block index populated from HudSyncPayload on join
 * (and after create/rename/delete). Each slot now carries a structured Entry (id, name +
 * sync-brick fields category/glow/hardness/sound/shape/passable) so the HUD can render those
 * bricks without server access.
 *
 * Group 27 §G27.4: the payload moved from a fragile delimited STRING (id + separator + name,
 * which split wrong on names containing the separator and dropped one-word names) to a
 * structured per-slot JSON object — fixing that bug. populate() still tolerates the old string
 * form for safety.
 *
 * Thread safety: populated on the network thread, read on the render thread; volatile swap.
 * CLIENT-SIDE ONLY.
 *
 * Depends on: Gson, HudSyncPayload.
 * Called by: CustomBlocksClient (populates), HudRenderer (reads).
 */
package com.customblocks.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
public final class ClientSlotCache {

    /** One slot's synced data. {@code passable} = walk-through (SlotData.noCollision).
     *  {@code lore} = the active lore lines (Group 18 REVAMP v2), empty when none/disabled.
     *  {@code arabic} = the Arabic identity tuple "glyph/form/colour" (G13-25 CP3b join-flow
     *  prediction on a remote session), "" for every normal block. */
    /** {@code rot} = G08 §B: the six face quarter-turns packed 2-bits-per-face in Direction order
     *  (FaceRotations.packed / unpack), 0 when nothing is rotated. The runtime shape mesh needs it
     *  because it never reads the pack's model JSON, where the rotations used to live. */
    public record Entry(String id, String name, String category, int glow,
                        float hardness, String sound, String shape, boolean passable, List<String> lore,
                        String arabic, int rot) {}

    private static volatile Map<Integer, Entry> INDEX = Collections.emptyMap();
    private static volatile Map<String, String> CAT_COLORS = Collections.emptyMap(); // category → §-colour tag
    private static volatile Map<String, String> CAT_HEX = Collections.emptyMap();     // category → custom "#RRGGBB" tint
    private static volatile Map<String, String> CAT_DESC = Collections.emptyMap();    // category → description
    private static volatile Map<String, String> CAT_SORT = Collections.emptyMap();    // category → sort mode (non-alpha)
    private static volatile Set<String> ALL_CATS = Collections.emptySet(); // every known category, incl. 0-block ones (§G27 L11)
    private static volatile String DEFAULT_CAT = ""; // studio's default category, "" = none

    private ClientSlotCache() {}

    /** Replace the cache from JSON sent by the server (structured object per slot). */
    public static void populate(String indexJson) {
        try {
            JsonObject root = JsonParser.parseString(indexJson).getAsJsonObject();
            Map<Integer, Entry> map = new HashMap<>();
            Map<String, String> colors = new HashMap<>();
            Map<String, String> hexes = new HashMap<>();
            Map<String, String> descs = new HashMap<>();
            Map<String, String> sorts = new HashMap<>();
            Set<String> allCats = new TreeSet<>();
            String defCat = "";
            for (var e : root.entrySet()) {
                String key = e.getKey();
                // Underscore-prefixed keys carry category metadata, not slots (Group 27 §G27.6).
                if (key.startsWith("_")) {
                    if (key.equals("_default") && e.getValue().isJsonPrimitive()) defCat = e.getValue().getAsString();
                    else if (key.equals("_meta") && e.getValue().isJsonObject())
                        for (var m : e.getValue().getAsJsonObject().entrySet()) colors.put(m.getKey(), m.getValue().getAsString());
                    else if (key.equals("_hex") && e.getValue().isJsonObject())
                        for (var m : e.getValue().getAsJsonObject().entrySet()) hexes.put(m.getKey(), m.getValue().getAsString());
                    else if (key.equals("_desc") && e.getValue().isJsonObject())
                        for (var m : e.getValue().getAsJsonObject().entrySet()) descs.put(m.getKey(), m.getValue().getAsString());
                    else if (key.equals("_sort") && e.getValue().isJsonObject())
                        for (var m : e.getValue().getAsJsonObject().entrySet()) sorts.put(m.getKey(), m.getValue().getAsString());
                    else if (key.equals("_categories") && e.getValue().isJsonArray())
                        for (JsonElement el : e.getValue().getAsJsonArray()) if (el.isJsonPrimitive()) allCats.add(el.getAsString());
                    continue;
                }
                int idx;
                try { idx = Integer.parseInt(key); } catch (NumberFormatException nfe) { continue; }
                JsonElement v = e.getValue();
                if (v.isJsonObject()) {
                    JsonObject o = v.getAsJsonObject();
                    map.put(idx, new Entry(
                            str(o, "id", ""), str(o, "name", ""), str(o, "cat", ""),
                            num(o, "glow", 0), (float) dbl(o, "hard", 1.5),
                            str(o, "sound", "stone"), str(o, "shape", "full"), bool(o, "pass", false),
                            lore(o), str(o, "ar", ""), num(o, "rot", 0)));
                } else if (v.isJsonPrimitive()) {
                    // Legacy delimited string fallback (id + NUL-or-space + name).
                    String val = v.getAsString();
                    int sep = val.indexOf((char) 0);
                    if (sep < 0) sep = val.indexOf(' ');
                    if (sep >= 0)
                        map.put(idx, new Entry(val.substring(0, sep), val.substring(sep + 1),
                                "", 0, 1.5f, "stone", "full", false, List.of(), "", 0));
                }
            }
            // G08 §B — a shape (or face-rotation) change no longer rebuilds and pushes the resource
            // pack, so nothing invalidates the chunk sections that already drew the old geometry. Detect
            // the change here (this cache is the only place the new shape arrives) and re-mesh the world.
            boolean geometryChanged = geometryDiffers(INDEX, map);
            INDEX = Collections.unmodifiableMap(map);
            if (geometryChanged) requestWorldRemesh();
            CAT_COLORS = Collections.unmodifiableMap(colors);
            CAT_HEX = Collections.unmodifiableMap(hexes);
            CAT_DESC = Collections.unmodifiableMap(descs);
            CAT_SORT = Collections.unmodifiableMap(sorts);
            ALL_CATS = Collections.unmodifiableSet(allCats);
            DEFAULT_CAT = defCat;
        } catch (Exception ignored) {
            INDEX = Collections.emptyMap();
        }
    }

    // ── G08 §B — reload-free shape swap ──────────────────────────────────────────────────────────

    /** True when any slot's drawn geometry differs between two index snapshots — a shape swap, a face
     *  rotation, or a slot appearing/disappearing. Ignores name/category/lore churn, which the HUD
     *  re-syncs constantly and which never changes a single quad. */
    private static boolean geometryDiffers(Map<Integer, Entry> before, Map<Integer, Entry> after) {
        if (before.isEmpty()) return false; // first populate (join): the initial bake is already correct
        for (var e : after.entrySet()) {
            Entry old = before.get(e.getKey());
            Entry now = e.getValue();
            if (old == null) continue;                       // new slot: nothing placed with it yet
            if (!old.shape().equals(now.shape())) return true;
            if (old.rot() != now.rot()) return true;
        }
        return false;
    }

    /** Re-mesh every loaded chunk section so placed blocks pick up the new shape without a pack reload.
     *  Hops to the render thread first — populate() runs on the network thread. A shape change is an
     *  admin action, so one brief full re-mesh is the right trade against tracking placements per slot. */
    private static void requestWorldRemesh() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
        });
    }

    /** Every known category — synced blocks' categories UNION explicitly-created 0-block ones (§G27 L11), sorted A→Z. */
    public static Set<String> categories() {
        Set<String> out = new TreeSet<>(ALL_CATS);
        for (Entry e : INDEX.values()) if (e.category() != null && !e.category().isEmpty()) out.add(e.category());
        return out;
    }

    /** The §-colour tag for a category, or "" if none set. */
    public static String colorTag(String category) {
        String t = CAT_COLORS.get(category);
        return t == null ? "" : t;
    }

    /** The custom "#RRGGBB" name tint for a category, or "" if none set (Group 27 Category Hub). */
    public static String colorHex(String category) {
        String h = CAT_HEX.get(category);
        return h == null ? "" : h;
    }

    /** The description for a category, or "" if none set (Group 27 Category Hub). */
    public static String description(String category) {
        String d = CAT_DESC.get(category);
        return d == null ? "" : d;
    }

    /** The sort mode for a category ("alpha" default, or "custom"), Group 27 Category Hub. */
    public static String sortOrder(String category) {
        String s = CAT_SORT.get(category);
        return s == null || s.isEmpty() ? "alpha" : s;
    }

    /** Every synced slot entry (read-only). Group 27 Category Hub counts/lists blocks from this. */
    public static java.util.Collection<Entry> entries() { return INDEX.values(); }

    /** Number of blocks currently in a category. */
    public static int countInCategory(String category) {
        if (category == null) return 0;
        int n = 0;
        for (Entry e : INDEX.values()) if (category.equalsIgnoreCase(e.category())) n++;
        return n;
    }

    /** Block entries in a category, sorted by display name (Group 27 Category Hub block list). */
    public static List<Entry> blocksInCategory(String category) {
        List<Entry> out = new ArrayList<>();
        if (category == null) return out;
        for (Entry e : INDEX.values()) if (category.equalsIgnoreCase(e.category())) out.add(e);
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /** The studio's default category, or "" if none set. */
    public static String defaultCategory() { return DEFAULT_CAT; }

    /** Full entry for a slot, or null if unknown. */
    public static Entry getEntry(int slotIndex) { return INDEX.get(slotIndex); }

    /** Slot index whose synced id matches {@code id} exactly, or null. Linear scan (≤1028, per click). */
    public static Integer indexForId(String id) {
        if (id == null) return null;
        for (Map.Entry<Integer, Entry> e : INDEX.entrySet()) {
            if (id.equals(e.getValue().id())) return e.getKey();
        }
        return null;
    }

    /** Back-compat: {customId, displayName} for the slot, or null. */
    public static String[] get(int slotIndex) {
        Entry e = INDEX.get(slotIndex);
        return e == null ? null : new String[]{ e.id(), e.name() };
    }

    public static void clear() {
        INDEX = Collections.emptyMap(); CAT_COLORS = Collections.emptyMap();
        CAT_DESC = Collections.emptyMap(); CAT_SORT = Collections.emptyMap();
        ALL_CATS = Collections.emptySet(); DEFAULT_CAT = "";
    }

    /** Read the synced lore lines for a slot ("lore" JSON array), or an empty list when absent. */
    private static List<String> lore(JsonObject o) {
        if (!o.has("lore") || !o.get("lore").isJsonArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("lore"))
            if (e.isJsonPrimitive()) { String s = e.getAsString(); if (!s.isEmpty()) out.add(s); }
        return out;
    }

    private static String  str (JsonObject o, String k, String def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString()  : def; }
    private static int     num (JsonObject o, String k, int def)     { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt()     : def; }
    private static double  dbl (JsonObject o, String k, double def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble()  : def; }
    private static boolean bool(JsonObject o, String k, boolean def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def; }
}
