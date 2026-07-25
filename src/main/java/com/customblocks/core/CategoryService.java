/**
 * CategoryService.java — synchronous category-management logic (Group 11 overhaul).
 *
 * The shared engine behind the unified /cb category command and the CategoryEditMenu tiles.
 * Pure, server-thread operations: create, rename, merge, the three delete modes, membership
 * add/remove, colour, description, icon, sort, lock/unlock all, filters, and a read-only info
 * summary. Player-facing wording is returned as an {@link Outcome} (ok + message) so both the
 * command handler and the GUI report identically.
 *
 * G11 rework: a block's categories are a SET in {@link CategoryMembershipStore}, not a word on the
 * block, and none of them is a "main". Every read below asks that store, never SlotManager's
 * legacy one-word field. Category identity (typed display name, colour, icon, description, order,
 * creation time) stays in {@link CategoryMetadataStore}.
 *
 * No block logic is duplicated: mutation goes through CategoryMembershipStore / CategoryMetadataStore /
 * DeletionService / LockManager, the same managers the rest of the mod uses.
 *
 * Depends on: CategoryMembershipStore, CategoryMetadataStore, CategoryFilters, SlotManager,
 *             SlotData, DeletionService, UndoManager, LockManager, TextureStore
 * Called by:  CategoryCommands (the /cb category tree), CategoryEditMenu (anvil submits)
 */
package com.customblocks.core;

import com.customblocks.command.CbFmt;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CategoryService {

    private CategoryService() {} // static-only

    /** Result of a mutation: ok=true → Chat.success(msg), ok=false → Chat.error(msg). */
    public record Outcome(boolean ok, String msg) {}

    private static Outcome ok(String m)  { return new Outcome(true, m); }
    private static Outcome err(String m) { return new Outcome(false, m); }

    // The colour-word palette moved to CbFmt (G04-3) — no §-code is defined outside that file now.

    /** Colour words for command suggestions. */
    public static java.util.Set<String> colorWords() { return CbFmt.categoryColorWords(); }

    /** The shared category-name normalizer (display as typed, match on the folded key). */
    private static String norm(String cat) {
        return CategoryMembershipStore.key(cat);
    }

    /** How a category should be shown to a player: its typed name, e.g. "Arabic Letters". */
    private static String shown(String cat) {
        return CategoryMetadataStore.getDisplayName(cat);
    }

    // ── create ───────────────────────────────────────────────────────────────

    /**
     * Make an empty category from a typed name. Rejects a key collision by naming the category
     * that already owns the key, rather than silently merging into it (TG11 A7).
     */
    public static Outcome create(String typedName) {
        String typed = typedName == null ? "" : typedName.trim();
        if (typed.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(typed)) {
            return err("\"Uncategorized\" already exists — it's the built-in category every block falls back to.");
        }
        String clash = CategoryMetadataStore.createChecked(typed);
        if (clash != null) {
            return clash.equalsIgnoreCase(typed)
                    ? err("Category \"" + clash + "\" already exists.")
                    : err("\"" + typed + "\" would collide with the existing category \"" + clash
                            + "\" — they'd share the same name internally. Pick a different name.");
        }
        return ok("Created empty category \"" + typed + "\". Add blocks with /cb setcategory <id> " + typed + ".");
    }

    // ── membership (one block, one category) ──────────────────────────────────

    /**
     * Add ONE membership. Assigning to an unknown name creates that category on the spot (G11:
     * today's assign-and-it-appears habit keeps working), but a name that would collide with an
     * existing key is refused — a typo must never file a block into a category nobody named.
     */
    public static Outcome addMembership(UUID actor, String blockId, String typedCategory) {
        SlotData d = SlotManager.getById(blockId);
        if (d == null) return err("There's no block called \"" + blockId + "\". Check /cb list.");
        String typed = typedCategory == null ? "" : typedCategory.trim();
        if (typed.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(typed)) {
            return err("Nothing is put INTO \"Uncategorized\" — a block lands there on its own when its "
                    + "last category is removed. Use /cb category remove " + d.customId() + " <category>.");
        }
        String clash = CategoryMetadataStore.collisionFor(typed);
        if (clash != null) {
            return err("\"" + typed + "\" collides with the existing category \"" + clash
                    + "\" — use that exact name, or pick a different one.");
        }
        CategoryMetadataStore.create(typed); // implicit creation; no-op when it already exists
        List<String> before = new ArrayList<>(CategoryMembershipStore.of(d.customId()));
        boolean changed = CategoryMembershipStore.add(d.customId(), typed);
        if (!changed) return ok("\"" + d.customId() + "\" is already in " + shown(typed) + " — nothing to do.");
        UndoManager.recordMembership(actor, d.customId(), before,
                CategoryMembershipStore.of(d.customId()), "category");
        return ok("Added \"" + d.customId() + "\" to " + shown(typed) + ".");
    }

    /**
     * Remove ONE membership. Taking away a block's last real category drops it into
     * {@code Uncategorized} rather than erroring — a block is never in nothing (TG11 A2).
     */
    public static Outcome removeMembership(UUID actor, String blockId, String typedCategory) {
        SlotData d = SlotManager.getById(blockId);
        if (d == null) return err("There's no block called \"" + blockId + "\". Check /cb list.");
        String cat = norm(typedCategory);
        if (cat.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(cat)) {
            return err("\"Uncategorized\" can't be removed — it's where a block sits when it has no other category.");
        }
        if (!CategoryMembershipStore.has(d.customId(), cat)) {
            return err("\"" + d.customId() + "\" isn't in " + shown(cat) + ".");
        }
        List<String> before = new ArrayList<>(CategoryMembershipStore.of(d.customId()));
        CategoryMembershipStore.remove(d.customId(), cat);
        UndoManager.recordMembership(actor, d.customId(), before,
                CategoryMembershipStore.of(d.customId()), "category");
        boolean floored = CategoryMembershipStore.of(d.customId()).contains(CategoryMembershipStore.UNCATEGORIZED);
        return ok("Removed \"" + d.customId() + "\" from " + shown(cat) + "."
                + (floored ? " It has no other category, so it's now Uncategorized." : ""));
    }

    // ── rename ───────────────────────────────────────────────────────────────

    /**
     * Rename a category: its record and every block's membership move to the new key.
     *
     * No key-freezing — the owner declined it, so a rename genuinely changes the category's
     * identity, and anything holding the old key (an exported ZIP, a saved filter) will not find
     * it afterwards. That is the accepted trade for a rename actually renaming.
     */
    public static Outcome rename(String oldCat, String newCat) {
        String from = norm(oldCat);
        String typed = newCat == null ? "" : newCat.trim();
        String to = norm(typed);
        if (to.isEmpty()) return err("New name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(from)) return err("\"Uncategorized\" can't be renamed.");
        if (CategoryMembershipStore.isUncategorized(to))   return err("\"Uncategorized\" is reserved — pick another name.");
        if (from.equals(to)) return ok("Old and new names are the same — nothing to do.");
        if (!exists(from)) return err("There's no category \"" + oldCat + "\". See /cb categories.");
        String clash = CategoryMetadataStore.collisionFor(typed);
        if (clash != null) {
            return err("\"" + typed + "\" collides with the existing category \"" + clash
                    + "\" — merge into it with /cb category merge instead.");
        }
        int moved = CategoryMembershipStore.blocksIn(from).size();
        CategoryMetadataStore.renameCategory(from, typed);
        CategoryMembershipStore.renameKey(from, to);
        return ok("Renamed \"" + oldCat + "\" → \"" + typed + "\" (" + moved + " block(s) updated).");
    }

    // ── merge ────────────────────────────────────────────────────────────────

    /**
     * Fold {@code source} into {@code target}: every block gains the target and loses the source,
     * then the source record goes. A block already in both simply keeps one membership — it's a
     * set union, so there is nothing to report and nothing to special-case (TG11 A6).
     */
    public static Outcome merge(String source, String target) {
        String from = norm(source), to = norm(target);
        if (to.isEmpty()) return err("Target category can't be empty.");
        if (from.equals(to)) return err("Can't merge a category into itself.");
        if (CategoryMembershipStore.isUncategorized(from)) return err("\"Uncategorized\" can't be merged away.");
        if (CategoryMembershipStore.isUncategorized(to))   return err("Nothing merges INTO \"Uncategorized\" — use /cb category delete instead.");
        if (!exists(from)) return err("There's no category \"" + source + "\". See /cb categories.");
        if (!exists(to))   return err("There's no category \"" + target + "\". Create it first with /cb category create.");
        int moved = CategoryMembershipStore.mergeInto(from, to);
        CategoryMetadataStore.deleteCategory(from);
        return ok("Merged " + moved + " block(s) from " + shown(from) + " into " + shown(to) + ".");
    }

    // ── delete — three modes (G11: exactly three, no more) ────────────────────

    /**
     * Mode 1 — category only. Strips this one membership from every block and drops the record.
     * Other memberships are untouched; a block left with none falls to {@code Uncategorized}.
     * Nothing is destroyed, so this needs no confirmation (TG11 A3).
     */
    public static Outcome deleteCategoryOnly(String category) {
        String cat = norm(category);
        Outcome guard = deletable(cat);
        if (guard != null) return guard;
        String name = shown(cat);
        int touched = CategoryMembershipStore.deleteKeyEverywhere(cat);
        CategoryMetadataStore.deleteCategory(cat);
        return ok("Deleted category " + name + " — " + touched + " block(s) kept, just no longer in it.");
    }

    /**
     * Mode 3 — move first. Blocks move to {@code targetCategory} before the source record goes, so
     * nothing lands in {@code Uncategorized} by accident (TG11 A5). Same union rule as merge; the
     * difference is purely that the caller names the destination as part of a delete.
     */
    public static Outcome deleteMoveFirst(String category, String targetCategory) {
        String cat = norm(category), to = norm(targetCategory);
        Outcome guard = deletable(cat);
        if (guard != null) return guard;
        if (to.isEmpty()) return err("Name the category to move the blocks to.");
        if (cat.equals(to)) return err("That's the same category — pick somewhere else to move the blocks.");
        if (CategoryMembershipStore.isUncategorized(to)) {
            return err("To leave the blocks uncategorized, use the category-only delete instead.");
        }
        if (!exists(to)) return err("There's no category \"" + targetCategory + "\". Create it first with /cb category create.");
        String name = shown(cat);
        int moved = CategoryMembershipStore.mergeInto(cat, to);
        CategoryMetadataStore.deleteCategory(cat);
        return ok("Moved " + moved + " block(s) from " + name + " to " + shown(to) + ", then deleted " + name + ".");
    }

    /**
     * Mode 2 — exclusive blocks. DESTROYS every block whose only category is this one, then drops
     * the record. Blocks that also live somewhere else are left alone and merely lose this
     * membership.
     *
     * The one category path that can destroy blocks, so it goes down the shared delete rail
     * (DeletionService, which also sweeps placed copies to "Deleted: <name>" markers) and records
     * the whole thing as ONE undo batch. The caller is responsible for holding it behind
     * /cb confirm first — see {@link #exclusiveBlocksIn} for the preview list.
     */
    public static Outcome deleteExclusiveBlocks(MinecraftServer server, UUID actor, String category) {
        String cat = norm(category);
        Outcome guard = deletable(cat);
        if (guard != null) return guard;
        if (server == null) return err("No server context — run this in-game.");

        String name = shown(cat);
        List<UndoManager.Op> children = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int locked = 0;
        for (SlotData d : CategoryMembershipStore.exclusiveBlocksIn(cat)) {
            if (LockManager.isLocked(d.customId())) { locked++; continue; }
            byte[] texture = DeletionService.deleteCore(server, d);
            children.add(new UndoManager.Op(UndoManager.Kind.DELETE, d, null, texture, "delete"));
            deleted.add(d.customId());
        }
        int kept = CategoryMembershipStore.deleteKeyEverywhere(cat); // whatever survived loses the membership
        CategoryMetadataStore.deleteCategory(cat);
        if (!deleted.isEmpty()) {
            ResourcePackServer.updatePack();                       // ONE rebuild frees every deleted slot
            UndoManager.recordBatch(actor, children, "category-delete (" + deleted.size() + ")");
        }
        String msg = "Deleted category " + name + " and " + deleted.size() + " block(s) that were only in it.";
        if (kept > 0)   msg += " " + kept + " shared block(s) kept.";
        if (locked > 0) msg += " " + locked + " locked block(s) kept.";
        return ok(msg);
    }

    /** The blocks a mode-2 delete would destroy — for the confirmation preview. */
    public static List<SlotData> exclusiveBlocksIn(String category) {
        return CategoryMembershipStore.exclusiveBlocksIn(norm(category));
    }

    /** Shared delete guard: the floor is never deletable and an unknown name is a typo, not a no-op. */
    private static Outcome deletable(String cat) {
        if (cat.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(cat)) {
            return err("\"Uncategorized\" can't be deleted — it's the fallback every block needs.");
        }
        if (!exists(cat)) return err("There's no category \"" + cat + "\". See /cb categories.");
        return null;
    }

    /** True when a category has a record OR at least one block — an empty category is still real. */
    private static boolean exists(String cat) {
        return CategoryMetadataStore.keyExists(cat) || !CategoryMembershipStore.blocksIn(cat).isEmpty();
    }

    // ── filter (replaces the old sort verb) ──────────────────────────────────

    /** Category-listing lines in {@code mode} order, or null when the mode isn't one of ours. */
    public static List<String> filterCategories(String mode) {
        List<String> keys = CategoryFilters.categories(mode);
        if (keys == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add(CbFmt.HEAD + "Categories (" + keys.size() + ") " + CbFmt.DIM + "· " + CategoryFilters.mode(mode));
        for (String k : keys) {
            String color = CategoryMetadataStore.getColorTag(k);
            lines.add(CbFmt.DIM + " • " + (color.isEmpty() ? CbFmt.BODY : color) + shown(k)
                    + CbFmt.DIM + "  (" + CategoryMembershipStore.blocksIn(k).size() + ")");
        }
        return lines;
    }

    /** Block-listing lines for one category in {@code mode} order, or null for an unknown mode. */
    public static List<String> filterBlocks(String category, String mode) {
        String cat = norm(category);
        List<SlotData> blocks = CategoryFilters.blocks(cat, mode);
        if (blocks == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add(CbFmt.HEAD + shown(cat) + CbFmt.DIM + " · " + blocks.size() + " block(s) · " + CategoryFilters.mode(mode));
        for (SlotData d : blocks) lines.add(CbFmt.DIM + " • " + CbFmt.BODY + d.customId());
        return lines;
    }

    /** The valid mode words, for tab-completion and error text. */
    public static List<String> categoryModes() { return CategoryFilters.CATEGORY_MODES; }
    public static List<String> blockModes()    { return CategoryFilters.BLOCK_MODES; }

    // ── colour tag ─────────────────────────────────────────────────────────────

    public static Outcome setColor(String category, String colorWord) {
        String cat = norm(category);
        String word = colorWord == null ? "" : colorWord.trim().toLowerCase(Locale.ROOT);
        if (!CbFmt.isCategoryColor(word)) {
            return err("Unknown colour \"" + word + "\". Try: " + String.join(", ", CbFmt.categoryColorWords()) + ".");
        }
        CategoryMetadataStore.setColorTag(cat, CbFmt.categoryColor(word));
        String display = word.isEmpty() ? "default" : word;
        return ok("Colour tag for " + shown(cat) + " set to " + CbFmt.categoryColor(word) + display + CbFmt.RESET + ".");
    }

    // ── description ────────────────────────────────────────────────────────────

    public static Outcome setDescription(String category, String text) {
        String cat = norm(category);
        String desc = text == null ? "" : text.trim();
        CategoryMetadataStore.setDescription(cat, desc);
        return desc.isEmpty()
                ? ok("Cleared the description for " + shown(cat) + ".")
                : ok("Description for " + shown(cat) + " set to: " + CbFmt.DIM + desc);
    }

    // ── icon (display block) ───────────────────────────────────────────────────

    public static Outcome setIcon(String category, String blockId) {
        String cat = norm(category);
        if (blockId == null || blockId.isBlank()) {
            CategoryMetadataStore.clearDisplayBlock(cat);
            return ok("Cleared the icon for " + shown(cat) + " (using default).");
        }
        if (SlotManager.getById(blockId) == null) {
            return err("There's no block called \"" + blockId + "\". Check /cb list.");
        }
        CategoryMetadataStore.setDisplayBlock(cat, blockId);
        return ok("Icon for " + shown(cat) + " set to block " + CbFmt.BODY + blockId + CbFmt.RESET + ".");
    }

    // ── sort order (the GUI's alpha/custom block order — NOT the filter verb) ──

    public static Outcome setSort(String category, String mode) {
        String cat = norm(category);
        String m = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (!m.equals("alpha") && !m.equals("custom")) {
            return err("Sort must be \"alpha\" or \"custom\".");
        }
        CategoryMetadataStore.setSortOrder(cat, m);
        return ok("Sort order for " + shown(cat) + " set to " + ("alpha".equals(m) ? "Alphabetical" : "Custom") + ".");
    }

    // ── lock / unlock all ──────────────────────────────────────────────────────

    public static Outcome lockAll(String category, boolean lock) {
        String cat = norm(category);
        List<SlotData> blocks = CategoryMembershipStore.blocksIn(cat);
        if (blocks.isEmpty()) return err("No blocks in " + shown(cat) + ". See /cb categories.");
        int changed = 0;
        for (SlotData d : blocks) {
            boolean was = LockManager.isLocked(d.customId());
            if (lock && !was)  { LockManager.lock(d.customId());   changed++; }
            if (!lock && was)  { LockManager.unlock(d.customId()); changed++; }
        }
        return ok((lock ? "Locked " : "Unlocked ") + changed + " block(s) in " + shown(cat) + ".");
    }

    // ── info (read-only summary) ───────────────────────────────────────────────

    /**
     * Category summary. {@code listBlocks} appends every block id (TG11 A11) — the count alone is
     * the default so a 200-block category doesn't flood chat unasked.
     */
    public static List<String> info(String category, boolean listBlocks) {
        String cat = norm(category);
        List<SlotData> blocks = CategoryMembershipStore.blocksIn(cat);
        List<String> lines = new ArrayList<>();
        if (blocks.isEmpty() && !CategoryMetadataStore.keyExists(cat)) {
            lines.add(CbFmt.DIM + "There's no category \"" + CbFmt.BODY + category + CbFmt.DIM + "\". See /cb categories.");
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
        lines.add(CbFmt.HEAD + "Category: " + (color.isEmpty() ? CbFmt.BODY : color) + shown(cat));
        lines.add(CbFmt.DIM + "Blocks: " + CbFmt.BODY + blocks.size() + "  " + CbFmt.DIM + "Locked: " + CbFmt.BODY + locked
                + CbFmt.DIM + "/" + CbFmt.BODY + (blocks.size() - locked) + " unlocked");
        lines.add(CbFmt.DIM + "Texture total: " + CbFmt.BODY + humanSize(texBytes));
        lines.add(CbFmt.DIM + "Sort: " + CbFmt.BODY + ("custom".equals(sort) ? "Custom" : "Alphabetical"));
        lines.add(CbFmt.DIM + "Icon: " + CbFmt.BODY + (icon == null ? "default" : icon));
        lines.add(CbFmt.DIM + "Description: " + (desc.isEmpty() ? CbFmt.FAINT + "(none)" : CbFmt.BODY + desc));
        if (listBlocks) {
            if (blocks.isEmpty()) {
                lines.add(CbFmt.FAINT + "(no blocks yet)");
            } else {
                List<String> ids = new ArrayList<>();
                for (SlotData d : blocks) ids.add(d.customId());
                lines.add(CbFmt.DIM + "Blocks: " + CbFmt.BODY + String.join(", ", ids));
            }
        }
        return lines;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
