/**
 * CategoryService.java — synchronous category-management logic (Group 11 overhaul).
 *
 * The shared engine behind the unified /cb category command and the CategoryEditMenu tiles.
 * Pure, server-thread operations: rename, merge, delete, colour, description, icon, sort,
 * lock/unlock all, and a read-only info summary. Player-facing wording is returned as an
 * {@link Outcome} (ok + message) so both the command handler and the GUI report identically.
 *
 * No block logic is duplicated: mutation goes through SlotManager / CategoryMetadataStore /
 * LockManager, the same managers the rest of the mod uses.
 *
 * Depends on: SlotManager, SlotData, CategoryMetadataStore, LockManager, TextureStore
 * Called by:  CategoryCommands (the /cb category tree), CategoryEditMenu (anvil submits)
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CategoryService {

    private CategoryService() {} // static-only

    /** Result of a mutation: ok=true → Chat.success(msg), ok=false → Chat.error(msg). */
    public record Outcome(boolean ok, String msg) {}

    private static Outcome ok(String m)  { return new Outcome(true, m); }
    private static Outcome err(String m) { return new Outcome(false, m); }

    /** Colour words the player can use with /cb category color, mapped to their §-codes. */
    private static final Map<String, String> COLOR_CODES = new LinkedHashMap<>();
    static {
        COLOR_CODES.put("none", "");   COLOR_CODES.put("default", "");  COLOR_CODES.put("white", "§f");
        COLOR_CODES.put("green", "§a"); COLOR_CODES.put("aqua", "§b");  COLOR_CODES.put("red", "§c");
        COLOR_CODES.put("pink", "§d");  COLOR_CODES.put("yellow", "§e"); COLOR_CODES.put("gold", "§6");
        COLOR_CODES.put("blue", "§9");  COLOR_CODES.put("darkaqua", "§3"); COLOR_CODES.put("purple", "§5");
        COLOR_CODES.put("darkgreen", "§2"); COLOR_CODES.put("darkred", "§4");
        COLOR_CODES.put("gray", "§8");  COLOR_CODES.put("grey", "§8");
    }

    /** Colour words for command suggestions. */
    public static java.util.Set<String> colorWords() { return COLOR_CODES.keySet(); }

    private static String norm(String cat) {
        return cat == null ? "" : cat.trim().toLowerCase(Locale.ROOT);
    }

    // ── rename ───────────────────────────────────────────────────────────────

    public static Outcome rename(String oldCat, String newCat) {
        String from = norm(oldCat), to = norm(newCat);
        if (to.isEmpty()) return err("New name can't be empty.");
        if (from.equals(to)) return ok("Old and new names are the same — nothing to do.");
        List<SlotData> blocks = SlotManager.byCategory(from);
        if (blocks.isEmpty()) return err("No blocks in category \"" + from + "\". See /cb categories.");
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), to);
        CategoryMetadataStore.renameCategory(from, to);
        return ok("Renamed category \"" + from + "\" → \"" + to + "\" (" + blocks.size() + " block(s) updated).");
    }

    // ── merge ────────────────────────────────────────────────────────────────

    public static Outcome merge(String source, String target) {
        String from = norm(source), to = norm(target);
        if (to.isEmpty()) return err("Target category can't be empty.");
        if (from.equals(to)) return err("Can't merge a category into itself.");
        List<SlotData> blocks = SlotManager.byCategory(from);
        if (blocks.isEmpty()) return err("No blocks in category \"" + from + "\". See /cb categories.");
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), to);
        CategoryMetadataStore.deleteCategory(from);
        return ok("Merged " + blocks.size() + " block(s) from \"" + from + "\" into \"" + to + "\".");
    }

    // ── delete (uncategorize, keep the blocks) ─────────────────────────────────

    public static Outcome delete(String category) {
        String cat = norm(category);
        List<SlotData> blocks = SlotManager.byCategory(cat);
        if (blocks.isEmpty()) return err("No blocks in category \"" + cat + "\". See /cb categories.");
        int count = blocks.size();
        for (SlotData d : blocks) SlotManager.setCategory(d.customId(), "");
        CategoryMetadataStore.deleteCategory(cat);
        return ok("Deleted category \"" + cat + "\" — " + count + " block(s) uncategorized (blocks kept).");
    }

    // ── colour tag ─────────────────────────────────────────────────────────────

    public static Outcome setColor(String category, String colorWord) {
        String cat = norm(category);
        String word = colorWord == null ? "" : colorWord.trim().toLowerCase(Locale.ROOT);
        if (!COLOR_CODES.containsKey(word)) {
            return err("Unknown colour \"" + word + "\". Try: " + String.join(", ", COLOR_CODES.keySet()) + ".");
        }
        CategoryMetadataStore.setColorTag(cat, COLOR_CODES.get(word));
        String shown = word.isEmpty() ? "default" : word;
        return ok("Colour tag for \"" + cat + "\" set to " + COLOR_CODES.get(word) + shown + "§r.");
    }

    // ── description ────────────────────────────────────────────────────────────

    public static Outcome setDescription(String category, String text) {
        String cat = norm(category);
        String desc = text == null ? "" : text.trim();
        CategoryMetadataStore.setDescription(cat, desc);
        return desc.isEmpty()
                ? ok("Cleared the description for \"" + cat + "\".")
                : ok("Description for \"" + cat + "\" set to: §7" + desc);
    }

    // ── icon (display block) ───────────────────────────────────────────────────

    public static Outcome setIcon(String category, String blockId) {
        String cat = norm(category);
        if (blockId == null || blockId.isBlank()) {
            CategoryMetadataStore.clearDisplayBlock(cat);
            return ok("Cleared the icon for \"" + cat + "\" (using default).");
        }
        if (SlotManager.getById(blockId) == null) {
            return err("There's no block called \"" + blockId + "\". Check /cb list.");
        }
        CategoryMetadataStore.setDisplayBlock(cat, blockId);
        return ok("Icon for \"" + cat + "\" set to block §f" + blockId + "§r.");
    }

    // ── sort order ─────────────────────────────────────────────────────────────

    public static Outcome setSort(String category, String mode) {
        String cat = norm(category);
        String m = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (!m.equals("alpha") && !m.equals("custom")) {
            return err("Sort must be \"alpha\" or \"custom\".");
        }
        CategoryMetadataStore.setSortOrder(cat, m);
        return ok("Sort order for \"" + cat + "\" set to " + ("alpha".equals(m) ? "Alphabetical" : "Custom") + ".");
    }

    // ── lock / unlock all ──────────────────────────────────────────────────────

    public static Outcome lockAll(String category, boolean lock) {
        String cat = norm(category);
        List<SlotData> blocks = SlotManager.byCategory(cat);
        if (blocks.isEmpty()) return err("No blocks in category \"" + cat + "\". See /cb categories.");
        int changed = 0;
        for (SlotData d : blocks) {
            boolean was = LockManager.isLocked(d.customId());
            if (lock && !was)  { LockManager.lock(d.customId());   changed++; }
            if (!lock && was)  { LockManager.unlock(d.customId()); changed++; }
        }
        return ok((lock ? "Locked " : "Unlocked ") + changed + " block(s) in \"" + cat + "\".");
    }

    // ── info (read-only summary) ───────────────────────────────────────────────

    public static List<String> info(String category) {
        String cat = norm(category);
        List<SlotData> blocks = SlotManager.byCategory(cat);
        List<String> lines = new ArrayList<>();
        if (blocks.isEmpty()) {
            lines.add("§7Category \"§f" + cat + "§7\" has no blocks. See /cb categories.");
            return lines;
        }
        int locked = 0;
        long texBytes = 0;
        for (SlotData d : blocks) {
            if (LockManager.isLocked(d.customId())) locked++;
            byte[] tex = TextureStore.load(d.index());
            if (tex != null) texBytes += tex.length;
        }
        String color = CategoryMetadataStore.getColorTag(cat);
        String desc = CategoryMetadataStore.getDescription(cat);
        String icon = CategoryMetadataStore.getDisplayBlock(cat);
        String sort = CategoryMetadataStore.getSortOrder(cat);
        lines.add("§6Category: " + (color.isEmpty() ? "§f" : color) + cat);
        lines.add("§7Blocks: §f" + blocks.size() + "  §7Locked: §f" + locked
                + "§7/§f" + (blocks.size() - locked) + " unlocked");
        lines.add("§7Texture total: §f" + humanSize(texBytes));
        lines.add("§7Sort: §f" + ("custom".equals(sort) ? "Custom" : "Alphabetical"));
        lines.add("§7Icon: §f" + (icon == null ? "default" : icon));
        lines.add("§7Description: " + (desc.isEmpty() ? "§8(none)" : "§f" + desc));
        return lines;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
