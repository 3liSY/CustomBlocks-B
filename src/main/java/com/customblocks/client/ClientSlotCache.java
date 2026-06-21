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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
public final class ClientSlotCache {

    /** One slot's synced data. {@code passable} = walk-through (SlotData.noCollision). */
    public record Entry(String id, String name, String category, int glow,
                        float hardness, String sound, String shape, boolean passable) {}

    private static volatile Map<Integer, Entry> INDEX = Collections.emptyMap();
    private static volatile Map<String, String> CAT_COLORS = Collections.emptyMap(); // category → §-colour tag
    private static volatile String DEFAULT_CAT = ""; // studio's default category, "" = none

    private ClientSlotCache() {}

    /** Replace the cache from JSON sent by the server (structured object per slot). */
    public static void populate(String indexJson) {
        try {
            JsonObject root = JsonParser.parseString(indexJson).getAsJsonObject();
            Map<Integer, Entry> map = new HashMap<>();
            Map<String, String> colors = new HashMap<>();
            String defCat = "";
            for (var e : root.entrySet()) {
                String key = e.getKey();
                // Underscore-prefixed keys carry category metadata, not slots (Group 27 §G27.6).
                if (key.startsWith("_")) {
                    if (key.equals("_default") && e.getValue().isJsonPrimitive()) defCat = e.getValue().getAsString();
                    else if (key.equals("_meta") && e.getValue().isJsonObject())
                        for (var m : e.getValue().getAsJsonObject().entrySet()) colors.put(m.getKey(), m.getValue().getAsString());
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
                            str(o, "sound", "stone"), str(o, "shape", "full"), bool(o, "pass", false)));
                } else if (v.isJsonPrimitive()) {
                    // Legacy delimited string fallback (id + NUL-or-space + name).
                    String val = v.getAsString();
                    int sep = val.indexOf((char) 0);
                    if (sep < 0) sep = val.indexOf(' ');
                    if (sep >= 0)
                        map.put(idx, new Entry(val.substring(0, sep), val.substring(sep + 1),
                                "", 0, 1.5f, "stone", "full", false));
                }
            }
            INDEX = Collections.unmodifiableMap(map);
            CAT_COLORS = Collections.unmodifiableMap(colors);
            DEFAULT_CAT = defCat;
        } catch (Exception ignored) {
            INDEX = Collections.emptyMap();
        }
    }

    /** All distinct categories currently in use (from the synced blocks), sorted A→Z. */
    public static Set<String> categories() {
        Set<String> out = new TreeSet<>();
        for (Entry e : INDEX.values()) if (e.category() != null && !e.category().isEmpty()) out.add(e.category());
        return out;
    }

    /** The §-colour tag for a category, or "" if none set. */
    public static String colorTag(String category) {
        String t = CAT_COLORS.get(category);
        return t == null ? "" : t;
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

    public static void clear() { INDEX = Collections.emptyMap(); CAT_COLORS = Collections.emptyMap(); DEFAULT_CAT = ""; }

    private static String  str (JsonObject o, String k, String def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString()  : def; }
    private static int     num (JsonObject o, String k, int def)     { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt()     : def; }
    private static double  dbl (JsonObject o, String k, double def)  { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble()  : def; }
    private static boolean bool(JsonObject o, String k, boolean def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def; }
}
