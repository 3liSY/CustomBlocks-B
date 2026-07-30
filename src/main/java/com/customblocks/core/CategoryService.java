/**
 * CategoryService.java — synchronous category-management logic (Group 11 overhaul).
 *
 * The shared engine behind the unified /cb category command and the CategoryEditMenu tiles.
 * Pure, server-thread operations: create, rename, merge, the three delete modes, membership
 * add/remove, colour, icon, sort, lock/unlock all, filters, and a read-only info summary.
 * Player-facing wording is returned as an {@link Outcome} (ok + message) so both the command
 * handler and the GUI report identically.
 *
 * G11 rework: a block's categories are a SET in {@link CategoryMembershipStore}, not a word on the
 * block, and none of them is a "main". Every read below asks that store, never SlotManager's
 * legacy one-word field. Category identity (typed display name, colour, icon, order, creation
 * time) stays in {@link CategoryMetadataStore}.
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

    /**
     * Result of a mutation: ok=true → Chat.success(msg), ok=false → Chat.error(msg).
     *
     * {@code chip} is the follow-up button the success line should carry (G11 C13) — an [↩ Undo]
     * after anything destructive, a [⊞ View Category] after an assignment. Null means no chip.
     */
    public record Outcome(boolean ok, String msg, Chip chip) {}

    /** A follow-up chip request: {@code kind} is "undo" or "view"; {@code arg} is its category. */
    public record Chip(String kind, String arg) {}

    private static Outcome ok(String m)  { return new Outcome(true, m, null); }
    private static Outcome err(String m) { return new Outcome(false, m, null); }

    /** A success that asks for the [↩ Undo] chip. */
    private static Outcome okUndo(String m) { return new Outcome(true, m, new Chip("undo", "")); }

    /** A success that asks for the [⊞ View Category] chip, pointed at {@code cat}. */
    private static Outcome okView(String m, String cat) { return new Outcome(true, m, new Chip("view", cat)); }

    // The colour-word palette moved to CbFmt (G04-3) — no §-code is defined outside that file now.

    /** Colour words for command suggestions. */
    public static java.util.Set<String> colorWords() { return CbFmt.categoryColorWords(); }

    /** The shared category-name normalizer (display as typed, match on the folded key). */
    private static String norm(String cat) {
        return CategoryMembershipStore.key(cat);
    }

    /**
     * A typed name as the player meant it: quotes stripped, casing kept. Use this — never a bare
     * {@code trim()} — anywhere a typed name is stored or echoed, so a quote someone typed out of
     * old habit is never baked into a record or printed back (G11 2026-07-26).
     */
    private static String typed(String cat) {
        return CategoryMembershipStore.unquote(cat);
    }

    /**
     * The tail sentence a name that carried a colour code earns (TG11 C10).
     *
     * {@link CategoryMembershipStore#unquote} drops the code, so the name is clean by the time it is
     * stored — but silently eating half of what someone typed reads as a bug. This says what happened
     * and points at the field that actually holds a colour, so the next attempt lands.
     */
    private static String codeHint(String raw, String cleaned) {
        if (!CategoryMembershipStore.hasCodes(raw == null ? "" : raw.replace("\"", ""))) return "";
        return " Colour codes aren't part of a name — set one with /cb category color <colour> " + cleaned + ".";
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
        String typed = typed(typedName);
        if (typed.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(typed)) {
            return err("Uncategorized already exists — it's the built-in category every block falls back to.");
        }
        String clash = CategoryMetadataStore.createChecked(typed);
        if (clash != null) {
            return clash.equalsIgnoreCase(typed)
                    ? err("Category " + clash + " already exists.")
                    : err(typed + " would collide with the existing category " + clash
                            + " — they'd share the same name internally. Pick a different name.");
        }
        return okView("Created empty category " + typed + ". Add blocks with /cb setcategory <id> "
                + typed + "." + codeHint(typedName, typed), typed);
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
        String typed = typed(typedCategory);
        if (typed.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(typed)) {
            return err("Nothing is put INTO Uncategorized — a block lands there on its own when its "
                    + "last category is removed. Use /cb category remove " + d.customId() + " <category>.");
        }
        String clash = CategoryMetadataStore.collisionFor(typed);
        if (clash != null) {
            return err(typed + " collides with the existing category " + clash
                    + " — use that exact name, or pick a different one.");
        }
        CategoryMetadataStore.create(typed); // implicit creation; no-op when it already exists
        List<String> before = new ArrayList<>(CategoryMembershipStore.of(d.customId()));
        boolean changed = CategoryMembershipStore.add(d.customId(), typed);
        if (!changed) return ok("\"" + d.customId() + "\" is already in " + shown(typed) + " — nothing to do.");
        UndoManager.recordMembership(actor, d.customId(), before,
                CategoryMembershipStore.of(d.customId()), "category");
        return okView("Added \"" + d.customId() + "\" to " + shown(typed) + ".", typed);
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
            return err("Uncategorized can't be removed — it's where a block sits when it has no other category.");
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
        String typed = typed(newCat);
        String to = norm(typed);
        if (to.isEmpty()) return err("New name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(from)) return err("Uncategorized can't be renamed.");
        if (CategoryMembershipStore.isUncategorized(to))   return err("Uncategorized is reserved — pick another name.");
        if (from.equals(to)) return ok("Old and new names are the same — nothing to do.");
        if (!exists(from)) return err("There's no category " + typed(oldCat) + ". See /cb categories.");
        String clash = CategoryMetadataStore.collisionFor(typed);
        if (clash != null) {
            return err(typed + " collides with the existing category " + clash
                    + " — fold it in with /cb category combine instead.");
        }
        String was = shown(from);
        int moved = CategoryMembershipStore.blocksIn(from).size();
        CategoryMetadataStore.renameCategory(from, typed);
        CategoryMembershipStore.renameKey(from, to);
        return okView("Renamed " + was + " → " + typed + " (" + moved + " block(s) updated)."
                + codeHint(newCat, typed), typed);
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
        if (CategoryMembershipStore.isUncategorized(from)) return err("Uncategorized can't be merged away.");
        if (CategoryMembershipStore.isUncategorized(to))   return err("Nothing merges INTO Uncategorized — use /cb category delete instead.");
        if (!exists(from)) return err("There's no category " + typed(source) + ". See /cb categories.");
        if (!exists(to))   return err("There's no category " + typed(target) + ". Create it first with /cb category create.");
        int moved = CategoryMembershipStore.mergeInto(from, to);
        CategoryMetadataStore.deleteCategory(from);
        return ok("Merged " + moved + " block(s) from " + shown(from) + " into " + shown(to) + ".");
    }

    /**
     * {@code /cb category combine <a> into <b>} — the player-facing name for a merge, and the only
     * one left (G11 2026-07-26: {@code merge} is retired from the command tree; the Hub bridge still
     * calls {@link #merge} under the hood). The {@code into} connector is required by the command
     * shape, so which category survives is never a guess.
     *
     * Undoable as one batch: every moved block's whole membership set plus the source record, so a
     * mistaken combine is one {@code /cb undo} away rather than a manual re-file.
     */
    public static Outcome combine(UUID actor, String source, String target) {
        String from = norm(source), to = norm(target);
        if (from.isEmpty()) return err("Name the category to combine.");
        if (to.isEmpty()) return err("Name the category to combine INTO — /cb category combine <a> into <b>.");
        if (from.equals(to)) return err("Can't combine a category into itself.");
        if (CategoryMembershipStore.isUncategorized(from)) return err("Uncategorized can't be combined away.");
        if (CategoryMembershipStore.isUncategorized(to)) {
            return err("Nothing combines INTO Uncategorized — use /cb category delete instead.");
        }
        if (!exists(from)) return err("There's no category " + typed(source) + ". See /cb categories.");
        if (!exists(to))   return err("There's no category " + typed(target) + ". Create it first with /cb category create.");

        String name = shown(from), into = shown(to);
        String record = CategoryMetadataStore.snapshot(from);
        List<SlotData> members = CategoryMembershipStore.blocksIn(from);
        List<List<String>> before = new ArrayList<>();
        for (SlotData d : members) before.add(new ArrayList<>(CategoryMembershipStore.of(d.customId())));

        int moved = CategoryMembershipStore.mergeInto(from, to);
        CategoryMetadataStore.deleteCategory(from);

        List<UndoManager.Op> children = new ArrayList<>();
        UndoManager.Op rec = UndoManager.categoryRecordOp(from, record, "", "category-combine");
        if (rec != null) children.add(rec);
        for (int i = 0; i < members.size(); i++) {
            String id = members.get(i).customId();
            UndoManager.Op op = UndoManager.membershipOp(id, before.get(i),
                    CategoryMembershipStore.of(id), "category-combine");
            if (op != null) children.add(op);
        }
        UndoManager.recordBatch(actor, children, "Combined " + name + " into " + into);

        return okUndo("Combined " + moved + " block(s) from " + name + " into " + into
                + " — " + name + " is gone.");
    }

    // ── delete — three modes (G11: exactly three, no more) ────────────────────

    /**
     * Mode 1 — category only. Strips this one membership from every block and drops the record.
     * Other memberships are untouched; a block left with none falls to {@code Uncategorized}.
     * Nothing is destroyed, so this needs no confirmation (TG11 A3).
     *
     * It IS undoable (G11 2026-07-26): one batch entry restoring the record — colour, icon,
     * description, sort and all — plus every membership it stripped. Behaviour here was always
     * right; it just used to record nothing, so /cb undo skipped straight past it (TG11 A3 finding).
     */
    public static Outcome deleteCategoryOnly(UUID actor, String category) {
        String cat = norm(category);
        Outcome guard = deletable(cat);
        if (guard != null) return guard;
        String name = shown(cat);

        // Snapshot BEFORE the wipe: the record itself, then each affected block's WHOLE membership
        // set (an undo has to restore the set, not just re-add this one key — see UndoManager.Membership).
        String record = CategoryMetadataStore.snapshot(cat);
        List<SlotData> members = CategoryMembershipStore.blocksIn(cat);
        List<List<String>> before = new ArrayList<>();
        for (SlotData d : members) before.add(new ArrayList<>(CategoryMembershipStore.of(d.customId())));

        int touched = CategoryMembershipStore.deleteKeyEverywhere(cat);
        CategoryMetadataStore.deleteCategory(cat);

        // The record op goes FIRST so that on undo the category exists again before any membership
        // is restored — restoring one re-stamps the legacy display shadow from its display name.
        List<UndoManager.Op> children = new ArrayList<>();
        UndoManager.Op rec = UndoManager.categoryRecordOp(cat, record, "", "category-delete");
        if (rec != null) children.add(rec);
        for (int i = 0; i < members.size(); i++) {
            String id = members.get(i).customId();
            UndoManager.Op op = UndoManager.membershipOp(id, before.get(i),
                    CategoryMembershipStore.of(id), "category-delete");
            if (op != null) children.add(op);
        }
        UndoManager.recordBatch(actor, children, "Deleted category " + name);

        return okUndo("Deleted category " + name + " — " + touched + " block(s) kept, just no longer in it.");
    }

    /**
     * The destructive half of the delete prompt (G11 C7): DESTROYS every block whose only category
     * is this one, then drops the record. Blocks that also live somewhere else are left alone and
     * merely lose this membership.
     *
     * The one category path that can destroy blocks, so it goes down the shared delete rail
     * (DeletionService, which also sweeps placed copies to "Deleted: <name>" markers). It records
     * TWO undo entries, not one (G11 2026-07-26): the category entry is pushed FIRST and the blocks
     * entry LAST, so the newest — the first {@code /cb undo} — is the one worth panicking about.
     * The click on the danger button IS the confirmation; {@code /cb confirm} is not involved.
     */
    public static Outcome deleteExclusiveBlocks(MinecraftServer server, UUID actor, String category) {
        String cat = norm(category);
        Outcome guard = deletable(cat);
        if (guard != null) return guard;
        if (server == null) return err("No server context — run this in-game.");

        String name = shown(cat);

        // Snapshot BEFORE anything moves: the record, and every member's whole membership set —
        // including the blocks about to die, so the category undo puts their memberships back too.
        String record = CategoryMetadataStore.snapshot(cat);
        List<SlotData> members = CategoryMembershipStore.blocksIn(cat);
        List<List<String>> before = new ArrayList<>();
        for (SlotData d : members) before.add(new ArrayList<>(CategoryMembershipStore.of(d.customId())));

        List<UndoManager.Op> blockOps = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int locked = 0;
        for (SlotData d : CategoryMembershipStore.exclusiveBlocksIn(cat)) {
            if (LockManager.isLocked(d.customId())) { locked++; continue; }
            byte[] texture = DeletionService.deleteCore(server, d);
            blockOps.add(new UndoManager.Op(UndoManager.Kind.DELETE, d, null, texture, "delete"));
            deleted.add(d.customId());
        }
        int kept = CategoryMembershipStore.deleteKeyEverywhere(cat); // whatever survived loses the membership
        CategoryMetadataStore.deleteCategory(cat);

        // Entry 1 — the category itself (record + every membership it held).
        List<UndoManager.Op> catOps = new ArrayList<>();
        UndoManager.Op rec = UndoManager.categoryRecordOp(cat, record, "", "category-delete");
        if (rec != null) catOps.add(rec);
        for (int i = 0; i < members.size(); i++) {
            String id = members.get(i).customId();
            UndoManager.Op op = UndoManager.membershipOp(id, before.get(i),
                    CategoryMembershipStore.of(id), "category-delete");
            if (op != null) catOps.add(op);
        }
        UndoManager.recordBatch(actor, catOps, "Deleted category " + name);

        // Entry 2 — the blocks. Pushed last, so it is the first thing /cb undo takes back. Its label
        // is pre-formatted because HistoryDescribe leaves a label that already carries a "(…)" alone.
        if (!deleted.isEmpty()) {
            ResourcePackServer.updatePack();                       // ONE rebuild frees every deleted slot
            UndoManager.recordBatch(actor, blockOps, "Deleted " + deleted.size()
                    + " block" + (deleted.size() == 1 ? "" : "s") + " (" + name + ")");
        }

        String msg = "Deleted category " + name + " and " + deleted.size() + " block(s) that were only in it.";
        if (kept > 0)   msg += " " + kept + " shared block(s) kept.";
        if (locked > 0) msg += " " + locked + " locked block(s) kept.";
        return okUndo(msg);
    }

    /** The blocks a mode-2 delete would destroy — for the confirmation preview. */
    public static List<SlotData> exclusiveBlocksIn(String category) {
        return CategoryMembershipStore.exclusiveBlocksIn(norm(category));
    }

    /** Shared delete guard: the floor is never deletable and an unknown name is a typo, not a no-op. */
    private static Outcome deletable(String cat) {
        if (cat.isEmpty()) return err("Category name can't be empty.");
        if (CategoryMembershipStore.isUncategorized(cat)) {
            return err("Uncategorized can't be deleted — it's the fallback every block needs.");
        }
        if (!exists(cat)) return err("There's no category " + cat + ". See /cb categories.");
        return null;
    }

    /** True when a category has a record OR at least one block — an empty category is still real. */
    private static boolean exists(String cat) {
        return CategoryMetadataStore.keyExists(cat) || !CategoryMembershipStore.blocksIn(cat).isEmpty();
    }

    // ── listings ─────────────────────────────────────────────────────────────
    // The read-only category/block listings and the info summary live in CategoryReport (§9.3
    // class gate). This file owns mutation; that one owns how a category READS.

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

    // ── sort order (the GUI's alphabetical/custom block order — NOT the filter verb) ──

    /**
     * Set a category's stored block order. The word players type is {@code alphabetically}
     * (G11 2026-07-26 — {@code alpha} is retired from every surface); the STORED value stays
     * {@code "alpha"}, which is what the Category Hub, the chest edit menu and the HUD sync already
     * read and write. {@code alpha} is still accepted silently so those GUI callers keep working.
     */
    public static Outcome setSort(String category, String mode) {
        String cat = norm(category);
        String m = CategoryFilters.sortWord(mode);
        if (m.isEmpty()) return err("Sort must be alphabetically or custom.");
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

}
