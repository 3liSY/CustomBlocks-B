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
        String displayName  = "";   // G11: the name AS TYPED; the map key stays the normalized form
        String displayBlock = "";   // block id, "" = none
        String colorTag     = "";   // §-code like "§a", "" = default white
        String colorHex     = "";   // Group 27 Category Hub custom hex "#RRGGBB", "" = use colorTag/default
        String description  = "";   // free text
        String sortOrder    = "alpha";  // "alpha" or "custom"
        List<String> customOrder = new ArrayList<>(); // block ids in custom order (only used when sortOrder="custom")
        boolean exists = false; // §G27 L11: true once explicitly created — keeps the category listed at 0 blocks
        long createdAt = 0L;    // G11 filter: epoch millis, for newest-to-oldest / oldest-to-newest

        private Meta() {}

        public String displayName()   { return displayName; }
        public String displayBlock()  { return displayBlock; }
        public String colorTag()      { return colorTag; }
        public String colorHex()      { return colorHex; }
        public String description()   { return description; }
        public String sortOrder()     { return sortOrder; }
        public List<String> customOrder() { return customOrder; }
        public boolean exists()       { return exists; }
        public long createdAt()       { return createdAt; }
    }

    private static final Map<String, Meta> DATA = new HashMap<>(); // category key → metadata

    static { load(); bootstrapUncategorized(); }

    private CategoryMetadataStore() {} // static-only

    /** The shared normalizer — one definition, in {@link CategoryMembershipStore#key}. */
    private static String key(String category) {
        return CategoryMembershipStore.key(category);
    }

    private static Meta getOrCreate(String cat) {
        String k = key(cat);
        Meta m = DATA.computeIfAbsent(k, x -> new Meta());
        if (m.displayName.isEmpty()) m.displayName = cat == null ? k : cat.trim();
        if (m.createdAt == 0L) m.createdAt = System.currentTimeMillis();
        return m;
    }

    /**
     * G11: {@code Uncategorized} is a built-in record that always exists and can never be deleted
     * or renamed — it is where a block lands when its last real membership goes.
     */
    private static void bootstrapUncategorized() {
        Meta m = DATA.computeIfAbsent(CategoryMembershipStore.UNCATEGORIZED, k -> new Meta());
        m.exists = true;
        if (m.displayName.isEmpty()) m.displayName = "Uncategorized";
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

    /** The custom "#RRGGBB" hex tint for the category name (Group 27 Category Hub), or "" for none. */
    public static synchronized String getColorHex(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? m.colorHex : "";
    }

    /** Set (or clear, with "") the custom hex tint. Normalised to "#RRGGBB"; invalid input clears it. */
    public static synchronized void setColorHex(String category, String hex) {
        getOrCreate(category).colorHex = normalizeHex(hex);
        save();
    }

    /** "#RRGGBB" (upper-case) if {@code hex} is a valid 6-digit hex (with/without #), else "". */
    public static String normalizeHex(String hex) {
        if (hex == null) return "";
        String h = hex.trim().replace("#", "");
        if (!h.matches("(?i)[0-9a-f]{6}")) return "";
        return "#" + h.toUpperCase(Locale.ROOT);
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

    // ── Display name / creation time (Group 11) ──────────────────────────────

    /**
     * The category's name AS TYPED ("Arabic Letters"), for every player-facing surface. Falls back
     * to the normalized key for a record that predates display names, so nothing renders blank.
     */
    public static synchronized String getDisplayName(String category) {
        String k = key(category);
        Meta m = DATA.get(k);
        return m != null && !m.displayName.isEmpty() ? m.displayName : k;
    }

    /** Epoch millis this category record was first created (0 when unknown — pre-G11 records). */
    public static synchronized long getCreatedAt(String category) {
        Meta m = DATA.get(key(category));
        return m != null ? m.createdAt : 0L;
    }

    /** True when a record already exists under {@code category}'s key. */
    public static synchronized boolean keyExists(String category) {
        return DATA.containsKey(key(category));
    }

    /**
     * The existing display name that {@code typedName} would collide with, or null when it is free
     * or is simply a different CASING of the same category.
     *
     * This is the line between the two behaviours the locked decisions ask for: typing
     * "arabic letters" at "Arabic Letters" resolves (case only), while typing "arabic-letters"
     * collides — it reads as a different name but lands on the same key, so it must be refused
     * rather than silently filed into a category the player didn't name.
     */
    public static synchronized String collisionFor(String typedName) {
        String typed = typedName == null ? "" : typedName.trim();
        Meta m = DATA.get(key(typed));
        if (m == null) return null;
        String shown = m.displayName.isEmpty() ? key(typed) : m.displayName;
        return typed.equalsIgnoreCase(shown) ? null : shown;
    }

    // ── Existence (Group 27 L11: categories are real the moment you create them) ─────

    /**
     * Register {@code category} as an existing category, even with 0 blocks and no other metadata.
     * Idempotent and unchecked — the pre-G11 callers (the Category Hub admin bridge, the first-load
     * conversion) create a record for a name that is already resolved. Use {@link #createChecked}
     * for a player typing a NEW name, which must be able to fail.
     */
    public static synchronized void create(String category) {
        getOrCreate(category).exists = true;
        save();
    }

    /**
     * Create a brand-new category from a typed name, rejecting a key collision (TG11 A7).
     *
     * A name that only differs in case from an existing one RESOLVES to it — that is the whole
     * point of the display-name/key split ("arabic letters" finds "Arabic Letters"). A name that
     * lands on the same key any OTHER way — a hyphen for a space, doubled spacing — is a genuine
     * collision: two visibly different names cannot share one category, so it is rejected and the
     * caller is told which existing category owns that key.
     *
     * @return null on success, or the EXISTING display name that blocked it.
     */
    public static synchronized String createChecked(String typedName) {
        String typed = typedName == null ? "" : typedName.trim();
        String k = key(typed);
        Meta existing = DATA.get(k);
        if (existing != null) {
            String shown = existing.displayName.isEmpty() ? k : existing.displayName;
            return shown; // both the "already exists" and the collision case — the caller words it
        }
        Meta m = getOrCreate(typed);
        m.exists = true;
        save();
        return null;
    }

    /** Every category key that has metadata OR was explicitly {@link #create}d — NOT block-membership. */
    public static synchronized java.util.Set<String> knownCategories() {
        return new java.util.HashSet<>(DATA.keySet());
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

    /**
     * Move all metadata from one category key to another (for rename category), re-stamping the
     * display name to the newly typed one. Refuses to rename the built-in {@code Uncategorized}
     * record away — it is the floor every block falls to and must keep its key.
     */
    public static synchronized void renameCategory(String oldCat, String newCat) {
        if (CategoryMembershipStore.isUncategorized(oldCat)) return;
        if (CategoryMembershipStore.isUncategorized(newCat)) return; // nothing may take the floor's key
        Meta m = DATA.remove(key(oldCat));
        if (m != null) {
            m.displayName = newCat == null ? key(newCat) : newCat.trim();
            DATA.put(key(newCat), m);
            save();
        }
    }

    /** Delete all metadata for a category. The built-in {@code Uncategorized} record is never deletable. */
    public static synchronized void deleteCategory(String category) {
        if (CategoryMembershipStore.isUncategorized(category)) return;
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
                    if (o.has("displayName"))   m.displayName   = o.get("displayName").getAsString();
                    if (o.has("createdAt"))     m.createdAt     = o.get("createdAt").getAsLong();
                    if (o.has("displayBlock"))  m.displayBlock  = o.get("displayBlock").getAsString();
                    if (o.has("colorTag"))      m.colorTag      = o.get("colorTag").getAsString();
                    if (o.has("colorHex"))      m.colorHex      = o.get("colorHex").getAsString();
                    if (o.has("description"))   m.description   = o.get("description").getAsString();
                    if (o.has("sortOrder"))     m.sortOrder     = o.get("sortOrder").getAsString();
                    if (o.has("exists"))        m.exists        = o.get("exists").getAsBoolean();
                    if (o.has("customOrder")) {
                        JsonArray arr = o.getAsJsonArray("customOrder");
                        for (JsonElement el : arr) m.customOrder.add(el.getAsString());
                    }
                    // Re-key through the shared normalizer: a pre-G11 file was keyed on plain
                    // lower-case, so a stored "my-cat" has to fold to "my cat" or its metadata
                    // would orphan the moment anything looked it up.
                    String k = key(e.getKey());
                    if (m.displayName.isEmpty()) m.displayName = e.getKey();
                    DATA.put(k, m);
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
                // Skip empty entries (unless explicitly created — L11: an empty category must still persist)
                if (!m.exists && m.displayBlock.isEmpty() && m.colorTag.isEmpty() && m.colorHex.isEmpty()
                        && m.description.isEmpty() && "alpha".equals(m.sortOrder)) continue;
                JsonObject o = new JsonObject();
                if (m.exists) o.addProperty("exists", true);
                if (!m.displayName.isEmpty()) o.addProperty("displayName", m.displayName);
                if (m.createdAt > 0L)         o.addProperty("createdAt", m.createdAt);
                if (!m.displayBlock.isEmpty()) o.addProperty("displayBlock", m.displayBlock);
                if (!m.colorTag.isEmpty())     o.addProperty("colorTag", m.colorTag);
                if (!m.colorHex.isEmpty())     o.addProperty("colorHex", m.colorHex);
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
