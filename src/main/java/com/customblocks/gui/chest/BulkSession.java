/**
 * BulkSession.java
 *
 * Per-player state for the Bulk GUI builder (Group 07): which filter, which property, which
 * value the player is assembling. The GUI is a thin front-end — when the player clicks Apply,
 * the builder turns this state into a `/cb bulkproperty <filter> <prop> <value>` command and
 * runs it through the already-tested command backend (GuiRouter.runCommand). Nothing here
 * mutates blocks; it only remembers the in-progress selection between menu clicks.
 *
 * Called by: BulkSelectMenu, BulkActionMenu, BulkHubMenu
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BulkSession {

    /** The properties the bulk builder can set, in cycle order. */
    public static final String[] PROPERTIES = {"glow", "hardness", "sound", "collision"};

    private static final String[] GLOW_VALUES     = {"0", "4", "8", "12", "15"};
    private static final String[] HARDNESS_VALUES = {"instant", "stone", "5", "20", "unbreakable"};
    private static final String[] COLLISION_VALUES = {"solid", "passable"};

    private static final Map<UUID, BulkSession> SESSIONS = new ConcurrentHashMap<>();

    /** Which operation the dashboard runs: "property" / "delete" / "rename" / "lock" / "favorite". */
    public String op = "property";
    /** Selection filter (BulkScope syntax), e.g. "all" or "category:stone". */
    public String filter = "all";
    /** The chosen property name (one of PROPERTIES). */
    public String property = "glow";
    /** The chosen value for the current property. */
    public String value = "8";
    /** Rename sub-mode: "prefix" / "suffix" / "replace". */
    public String renameMode = "prefix";
    /** Rename text A — the prefix/suffix text, or the "find" text in replace mode. */
    public String renameA = "";
    /** Rename text B — the replacement (replace mode only). */
    public String renameB = "";
    /** Flag direction: true = lock/favorite (the "on" sense), false = unlock/unfavorite. */
    public boolean flagOn = true;
    /** Target category for the "Move to category" op ("" = clear). */
    public String category = "";
    /** Export format for the "Export" op: "json" or "txt". */
    public String exportFormat = "json";

    // ---- Two-step bulk GUI: a shared Step-1 selection feeds a per-op Step-2 action (any bulk op). ----

    /** Step-1 filter kinds, in cycle order. */
    public static final String[] SEL_FILTER_KINDS = {"all", "category", "favorite", "locked", "name", "id"};
    /** Every export format the export action GUI offers (kept complete on purpose). */
    public static final String[] EXPORT_FORMATS = {"json", "txt", "png", "csv", "md", "html", "yaml"};

    /** Step-1 selection mode: "" = nothing yet · "filter" = a rule (kind/value) · "picked" = ticked ids. */
    public String selMode = "";
    /** Step-1 filter kind (one of SEL_FILTER_KINDS) when in "filter" mode. */
    public String selFilterKind = "all";
    /** Typed value for the category / name / id kinds (those need text). */
    public String selFilterValue = "";
    /** True only while the block list is being used to hand-pick blocks for the current bulk op. */
    public boolean listPickForBulk = false;

    private static final String[] OPS =
            {"property", "delete", "rename", "category", "duplicate", "export", "lock", "favorite"};
    private static final String[] RENAME_MODES = {"prefix", "suffix", "replace"};

    private BulkSession() {}

    /** Cycle the dashboard operation forward (dir +1) or back (dir -1). */
    public void cycleOp(int dir) { op = cycle(OPS, op, dir); }

    /** Cycle the rename sub-mode (prefix → suffix → replace). */
    public void cycleRenameMode(int dir) { renameMode = cycle(RENAME_MODES, renameMode, dir); }

    /** Flip the flag direction (lock⇄unlock or favorite⇄unfavorite). */
    public void toggleFlag() { flagOn = !flagOn; }

    // ---- Two-step bulk GUI helpers (shared by every op) ----

    /** Wipe the Step-1 selection so the builder opens at "None selected currently". */
    public void resetSelection() {
        selMode = "";
        selFilterKind = "all";
        selFilterValue = "";
        listPickForBulk = false;
    }

    /** Cycle the Step-1 filter kind and switch to filter mode. */
    public void cycleSelFilterKind(int dir) {
        selFilterKind = cycle(SEL_FILTER_KINDS, selFilterKind, dir);
        selMode = "filter";
    }

    /** True when the current filter kind needs a typed value (category / name / id). */
    public boolean selFilterNeedsValue() {
        return "category".equals(selFilterKind) || "name".equals(selFilterKind) || "id".equals(selFilterKind);
    }

    /** Cycle the export format across all offered formats. */
    public void cycleExportFormat(int dir) { exportFormat = cycle(EXPORT_FORMATS, exportFormat, dir); }

    /** The BulkScope expression for the current Step-1 selection ("" = nothing selected). */
    public String selScopeExpr(UUID uuid) {
        if ("picked".equals(selMode)) return ListSelection.joined(uuid);
        if ("filter".equals(selMode)) {
            return switch (selFilterKind) {
                case "all"      -> "all";
                case "favorite" -> "favorite:yes";
                case "locked"   -> "locked:yes";
                case "category" -> "category:" + selFilterValue;
                case "name"     -> "name:" + selFilterValue;
                case "id"       -> "id:" + selFilterValue;
                default         -> "";
            };
        }
        return "";
    }

    /** True when Step 1 has a usable selection (lights the Next button). */
    public boolean selHasSelection(UUID uuid) {
        if ("picked".equals(selMode)) return ListSelection.size(uuid) > 0;
        if ("filter".equals(selMode)) return !selFilterNeedsValue() || !selFilterValue.isBlank();
        return false;
    }

    /** Plain-words label of the current Step-1 selection for menu headers. */
    public String selLabel(UUID uuid) {
        if ("picked".equals(selMode)) {
            int n = ListSelection.size(uuid);
            return n == 0 ? "None selected currently" : "Picked blocks: " + n;
        }
        if ("filter".equals(selMode)) {
            return switch (selFilterKind) {
                case "all"      -> "All blocks";
                case "favorite" -> "Favorited";
                case "locked"   -> "Locked";
                case "category" -> selFilterValue.isBlank() ? "Category: (type a name)" : "Category: " + selFilterValue;
                case "name"     -> selFilterValue.isBlank() ? "Name contains: (type text)" : "Name contains: " + selFilterValue;
                case "id"       -> selFilterValue.isBlank() ? "ID starts: (type text)" : "ID starts: " + selFilterValue;
                default         -> "None selected currently";
            };
        }
        return "None selected currently";
    }

    /** Human label for a Step-1 filter kind. */
    public static String selKindLabel(String kind) {
        return switch (kind) {
            case "all"      -> "All blocks";
            case "category" -> "Category";
            case "favorite" -> "Favorited";
            case "locked"   -> "Locked";
            case "name"     -> "Name contains";
            case "id"       -> "ID starts";
            default         -> kind;
        };
    }

    /** The BulkFlagCommands op string for the current flag operation + direction. */
    public String flagCommandOp() {
        if ("favorite".equals(op)) return flagOn ? "favorite" : "unfavorite";
        return flagOn ? "lock" : "unlock";
    }

    /** Human-friendly label for an operation. */
    public static String prettyOp(String op) {
        return switch (op) {
            case "delete"    -> "Delete blocks";
            case "rename"    -> "Rename blocks";
            case "category"  -> "Move to category";
            case "duplicate" -> "Duplicate blocks";
            case "export"    -> "Export blocks";
            case "lock"      -> "Lock blocks";
            case "favorite"  -> "Favorite blocks";
            default          -> "Edit a setting";
        };
    }

    public static BulkSession get(UUID player) {
        return SESSIONS.computeIfAbsent(player, k -> new BulkSession());
    }

    /** The value choices available for the current property (sound = the live sound-type list). */
    public String[] valuesForProperty() {
        return switch (property) {
            case "glow"      -> GLOW_VALUES;
            case "hardness"  -> HARDNESS_VALUES;
            case "sound"     -> SlotBlock.SOUND_TYPES;
            case "collision" -> COLLISION_VALUES;
            default          -> GLOW_VALUES;
        };
    }

    /** A sensible default value when the property changes. */
    public String defaultValueForProperty() {
        return switch (property) {
            case "glow"      -> "8";
            case "hardness"  -> "stone";
            case "sound"     -> SlotBlock.SOUND_TYPES.length > 0 ? SlotBlock.SOUND_TYPES[0] : "stone";
            case "collision" -> "solid";
            default          -> "0";
        };
    }

    /** Cycle the property forward (dir +1) or back (dir -1) and reset the value to its default. */
    public void cycleProperty(int dir) {
        property = cycle(PROPERTIES, property, dir);
        value = defaultValueForProperty();
    }

    /** Cycle the value of the current property forward (dir +1) or back (dir -1). */
    public void cycleValue(int dir) {
        value = cycle(valuesForProperty(), value, dir);
    }

    private static String cycle(String[] options, String current, int dir) {
        if (options.length == 0) return current;
        int i = 0;
        for (int k = 0; k < options.length; k++) {
            if (options[k].equalsIgnoreCase(current)) { i = k; break; }
        }
        int next = ((i + dir) % options.length + options.length) % options.length;
        return options[next];
    }
}
