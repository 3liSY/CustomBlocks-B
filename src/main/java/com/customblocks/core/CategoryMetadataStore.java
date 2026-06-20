/**
 * CategoryMetadataStore.java — per-category metadata (Group 11 overhaul).
 *
 * Stores category-level properties:
 *   - displayBlock:  which block id is the category's icon in the browser
 *   - colorTag:      §-colour code that tints the category name text in the GUI (e.g. "§a")
 *   - description:   free-text shown in the browser header
 *   - sortOrder:     "alpha" (default) or "custom" — if custom, a persisted id list
 *
 * Persists to config/customblocks/data/category_meta.json via atomic write (NFR-13).
 * CategoryDisplayBlockManager is kept as a thin delegate (its API stays stable for callers).
 *
 * Depends on: (none — Gson is bundled with Minecraft).
 * Called by:  CategoryDisplayBlockManager, CategoryEditMenu, CategoryListMenu, CategoryBrowserMenu.
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CategoryMetadataStore {

    private static final String FILE = "config/customblocks/data/category_meta.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Per-category metadata record. */
    public static final class Meta {
        String displayBlock = "";   // block id, "" = none
        String colorTag     = "";   // §-code like "§a", "" = default white
        String description  = "";   // free text
        String sortOrder    = "alpha";  // "alpha" or "custom"
        List<String> customOrder = new ArrayList<>(); // block ids in custom order (only used when sortOrder="custom")

        private Meta() {}

        public String displayBlock()  { return displayBlock; }
        public String colorTag()      { return colorTag; }
        public String description()   { return description; }
        public String sortOrder()     { return sortOrder; }
        public List<String> customOrder() { return customOrder; }
    }

    private static final Map<String, Meta> DATA = new HashMap<>(); // category → metadata

    static { load(); }

    private CategoryMetadataStore() {} // static-only

    private static String key(String category) {
        return category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    }

    private static Meta getOrCreate(String cat) {
        return DATA.computeIfAbsent(key(cat), k -> new Meta());
    }

    // ── Display block ────────────────────────────────────────────────────────

    /** The block id chosen as the category's icon, or null if none set. */
    public static synchronized String getDisplayBlock(String category) {
        Meta m = DATA.get(key(category));
        return m != null && !m.displayBlock.isEmpty() ? m.displayBlock : null;
    }

    public static synchronized void setDisplayBlock(String category, String blockId) {
        getOrCreate(category).displayBlock = blockId == null ? "" : blockId;
        save();
    }

    public static synchronized boolean clearDisplayBlock(String category) {
        Meta m = DATA.get(key(category));
        if (m == null || m.displayBlock.isEmpty()) return false;
        m.displayBlock = "";
        save();
        return true;
    }

    // ── Color tag ────────────────────────────────────────────────────────────

    /** The §-colour code for the category name, or "" for default. */
    public static synchronized String getColorTag(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? m.colorTag : "";
    }

    public static synchronized void setColorTag(String category, String tag) {
        getOrCreate(category).colorTag = tag == null ? "" : tag;
        save();
    }

    // ── Description ──────────────────────────────────────────────────────────

    public static synchronized String getDescription(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? m.description : "";
    }

    public static synchronized void setDescription(String category, String desc) {
        getOrCreate(category).description = desc == null ? "" : desc;
        save();
    }

    // ── Sort order ───────────────────────────────────────────────────────────

    public static synchronized String getSortOrder(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? m.sortOrder : "alpha";
    }

    public static synchronized void setSortOrder(String category, String order) {
        getOrCreate(category).sortOrder = order == null ? "alpha" : order;
        save();
    }

    public static synchronized List<String> getCustomOrder(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? new ArrayList<>(m.customOrder) : new ArrayList<>();
    }

    public static synchronized void setCustomOrder(String category, List<String> ids) {
        Meta meta = getOrCreate(category);
        meta.customOrder = ids == null ? new ArrayList<>() : new ArrayList<>(ids);
        meta.sortOrder = "custom";
        save();
    }

    // ── Rename support ───────────────────────────────────────────────────────

    /** Repoint every display block that referenced oldId to newId (for /cb reid). */
    public static synchronized void renameBlockId(String oldId, String newId) {
        boolean changed = false;
        for (Meta m : DATA.values()) {
            if (m.displayBlock.equals(oldId)) { m.displayBlock = newId; changed = true; }
            int idx = m.customOrder.indexOf(oldId);
            if (idx >= 0) { m.customOrder.set(idx, newId); changed = true; }
        }
        if (changed) save();
    }

    /** Move all metadata from one category key to another (for rename category). */
    public static synchronized void renameCategory(String oldCat, String newCat) {
        Meta m = DATA.remove(key(oldCat));
        if (m != null) {
            DATA.put(key(newCat), m);
            save();
        }
    }

    /** Delete all metadata for a category. */
    public static synchronized void deleteCategory(String category) {
        if (DATA.remove(key(category)) != null) save();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) {
                // Migration: try to import from old display_blocks.json
                migrateOldDisplayBlocks();
                return;
            }
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            for (var e : root.entrySet()) {
                try {
                    Meta m = new Meta();
                    JsonObject o = e.getValue().getAsJsonObject();
                    if (o.has("displayBlock"))  m.displayBlock  = o.get("displayBlock").getAsString();
                    if (o.has("colorTag"))      m.colorTag      = o.get("colorTag").getAsString();
                    if (o.has("description"))   m.description   = o.get("description").getAsString();
                    if (o.has("sortOrder"))     m.sortOrder     = o.get("sortOrder").getAsString();
                    if (o.has("customOrder")) {
                        JsonArray arr = o.getAsJsonArray("customOrder");
                        for (JsonElement el : arr) m.customOrder.add(el.getAsString());
                    }
                    DATA.put(e.getKey(), m);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /** One-time migration from the old flat display_blocks.json (category→blockId). */
    private static void migrateOldDisplayBlocks() {
        try {
            Path old = Path.of("config/customblocks/data/display_blocks.json");
            if (!Files.exists(old)) return;
            JsonObject o = GSON.fromJson(Files.readString(old, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            for (var e : o.entrySet()) {
                try {
                    Meta m = new Meta();
                    m.displayBlock = e.getValue().getAsString();
                    DATA.put(e.getKey(), m);
                } catch (Exception ignored) {}
            }
            if (!DATA.isEmpty()) save(); // write the new format
        } catch (Exception ignored) {}
    }

    private static synchronized void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            for (var e : DATA.entrySet()) {
                Meta m = e.getValue();
                // Skip empty entries
                if (m.displayBlock.isEmpty() && m.colorTag.isEmpty()
                        && m.description.isEmpty() && "alpha".equals(m.sortOrder)) continue;
                JsonObject o = new JsonObject();
                if (!m.displayBlock.isEmpty()) o.addProperty("displayBlock", m.displayBlock);
                if (!m.colorTag.isEmpty())     o.addProperty("colorTag", m.colorTag);
                if (!m.description.isEmpty())  o.addProperty("description", m.description);
                if (!"alpha".equals(m.sortOrder)) {
                    o.addProperty("sortOrder", m.sortOrder);
                    if (!m.customOrder.isEmpty()) {
                        JsonArray arr = new JsonArray();
                        for (String id : m.customOrder) arr.add(id);
                        o.add("customOrder", arr);
                    }
                }
                root.add(e.getKey(), o);
            }
            Path tmp = file.resolveSibling("category_meta.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
