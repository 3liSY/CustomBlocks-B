/**
 * CategoryHubModel.java — Group 27 Category Hub, client-side data assembly. CLIENT-ONLY.
 *
 * Pure (no rendering): turns the synced {@link ClientSlotCache} into the hub's category rows (display
 * name, colour swatch, block count, default marker) and per-category block lists, plus the shared
 * 16-colour swatch palette and the §-tag → ARGB mapping the name tint uses. Kept separate so
 * {@link CategoryHubScreen} stays under the 500-line size gate (CLAUDE.md §5.1).
 *
 * Depends on: ClientSlotCache.
 * Called by: client/gui/CategoryHubScreen.
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class CategoryHubModel {

    private CategoryHubModel() {} // static-only

    /** Uncategorized pseudo-category key (blocks whose category is ""). */
    public static final String UNCATEGORIZED = "";

    /** One list row: real category key (""=uncategorized), display name, swatch colour, §-tag, custom hex, count, default star. */
    public record Row(String key, String name, int colorArgb, String colorTag, String colorHex, int count, boolean isDefault) {}

    // The 16 Minecraft colour codes as swatches (matches the studio palette), plus a "clear" handled by the screen.
    public static final String[] TAGS = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f" };
    public static final int[] ARGB = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF };

    /** ARGB for a §-colour tag (name tint / swatch), white for none/unknown. */
    public static int argbForTag(String tag) {
        if (tag == null) return 0xFFFFFFFF;
        for (int i = 0; i < TAGS.length; i++) if (TAGS[i].equals(tag)) return ARGB[i];
        return 0xFFFFFFFF;
    }

    /** Parse "#RRGGBB" → opaque ARGB, or 0 when not a valid 6-digit hex. */
    public static int argbForHex(String hex) {
        if (hex == null) return 0;
        String h = hex.trim().replace("#", "");
        if (!h.matches("(?i)[0-9a-f]{6}")) return 0;
        try { return 0xFF000000 | Integer.parseInt(h, 16); } catch (NumberFormatException e) { return 0; }
    }

    /** The category name's tint ARGB: custom hex wins, else the §-tag, else white. */
    public static int nameArgb(String tag, String hex) {
        int h = argbForHex(hex);
        return h != 0 ? h : argbForTag(tag);
    }

    /** Build the category name as coloured Text: RGB style for a custom hex, else the §-tag prefix. */
    public static net.minecraft.text.MutableText coloredName(String name, String tag, String hex) {
        int h = argbForHex(hex);
        if (h != 0)
            return net.minecraft.text.Text.literal(name).setStyle(
                    net.minecraft.text.Style.EMPTY.withColor(net.minecraft.text.TextColor.fromRgb(h & 0xFFFFFF)));
        return net.minecraft.text.Text.literal((tag == null || tag.isEmpty() ? "§f" : tag) + name);
    }

    /** Build the category rows from the synced cache, filtered by a (case-insensitive) search substring. */
    public static List<Row> rows(String search) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String def = ClientSlotCache.defaultCategory();
        List<Row> out = new ArrayList<>();
        for (String cat : ClientSlotCache.categories()) {
            String name = titleCase(cat);
            if (!q.isEmpty() && !cat.toLowerCase(Locale.ROOT).contains(q) && !name.toLowerCase(Locale.ROOT).contains(q))
                continue;
            String tag = ClientSlotCache.colorTag(cat);
            String hex = ClientSlotCache.colorHex(cat);
            out.add(new Row(cat, name, nameArgb(tag, hex), tag, hex, ClientSlotCache.countInCategory(cat),
                    cat.equalsIgnoreCase(def)));
        }
        // Uncategorized bucket last, only when it has blocks and matches the search.
        int un = uncategorizedCount();
        if (un > 0 && (q.isEmpty() || "uncategorized".contains(q)))
            out.add(new Row(UNCATEGORIZED, "(uncategorized)", 0xFF777777, "", "", un, false));
        return out;
    }

    /** Blocks in a category (or the uncategorized bucket for key ""), sorted by display name. */
    public static List<ClientSlotCache.Entry> blocks(String key) {
        if (UNCATEGORIZED.equals(key)) {
            List<ClientSlotCache.Entry> out = new ArrayList<>();
            for (ClientSlotCache.Entry e : ClientSlotCache.entries())
                if (e.category() == null || e.category().isEmpty()) out.add(e);
            out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            return out;
        }
        return ClientSlotCache.blocksInCategory(key);
    }

    public static int totalBlocks() { return ClientSlotCache.entries().size(); }

    public static int uncategorizedCount() {
        int n = 0;
        for (ClientSlotCache.Entry e : ClientSlotCache.entries())
            if (e.category() == null || e.category().isEmpty()) n++;
        return n;
    }

    /** underscore_or_dashed id → "Title Case" display name. */
    public static String titleCase(String id) {
        if (id == null || id.isBlank()) return "Uncategorized";
        String[] parts = id.trim().replace('-', '_').split("_+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.length() > 1 ? p.substring(1) : "");
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    /** Normalise typed text into a category id (lowercase, spaces→_, drop other punctuation). */
    public static String normalizeId(String typed) {
        return typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
    }
}
