/**
 * BulkOpSpec.java — Group 07 §G07-3 (Bulk Workbench Screen). CLIENT-ONLY.
 *
 * The op table: the nine operations on the Bulk tab's left rail, and for each one its label, the payload op
 * it sends, the params its options pane offers, and whether it skips locked blocks.
 *
 * "Rail op" (this class) and "payload op" (BulkActionPayload) are deliberately different numbers: Lock and
 * Favorite are two rail entries but one server handler (BulkFlagCommands), so both map to payload FLAG and
 * differ only by their p1 (lock|unlock vs favorite|unfavorite).
 *
 * The value lists mirror the chest dashboard's BulkSession + the server's BulkValues, so a value the pane can
 * cycle to is always a value the server accepts.
 *
 * Depends on: SlotBlock (the live sound-type list), BulkActionPayload.
 * Called by: BulkWorkbenchScreen, BulkOpsView, BulkWorkbenchModel.
 */
package com.customblocks.client.gui;

import com.customblocks.block.SlotBlock;
import com.customblocks.network.payloads.BulkActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class BulkOpSpec {

    private BulkOpSpec() {} // static-only

    // Op ids — kept STABLE (Recolor was appended, not inserted) so no existing switch/array index shifts. The
    // left rail's visual order (§G27.22b) is the separate RAIL_ORDER list below.
    static final int OP_PROPERTY = 0;
    static final int OP_RENAME = 1;
    static final int OP_CATEGORY = 2;
    static final int OP_DUPLICATE = 3;
    static final int OP_EXPORT = 4;
    static final int OP_LOCK = 5;
    static final int OP_FAVORITE = 6;
    static final int OP_REID = 7;
    static final int OP_DELETE = 8;
    static final int OP_RECOLOR = 9;    // §G27.22b net-new 10th op
    static final int OP_COUNT = 10;

    /** The top-to-bottom order the 10 ops appear on the §G27.22b LEFT rail (Recolor sits 2nd, after Edit). */
    static final int[] RAIL_ORDER = {
            OP_PROPERTY, OP_RECOLOR, OP_RENAME, OP_CATEGORY, OP_DUPLICATE, OP_REID, OP_LOCK, OP_FAVORITE, OP_EXPORT, OP_DELETE
    };

    /** Rail labels, indexed by op id. Short forms per the §G27.22b mockup ("Move", "Re-ID"). */
    static final String[] RAIL = {
            "Edit", "Rename", "Move", "Duplicate", "Export", "Lock", "Favorite", "Re-ID", "Delete", "Recolor"
    };

    /** Op-header headings, indexed by op id. */
    static final String[] TITLE = {
            "EDIT A SETTING", "RENAME", "MOVE TO CATEGORY", "DUPLICATE", "EXPORT",
            "LOCK / UNLOCK", "FAVORITE", "RE-ID", "DELETE", "RECOLOR"
    };

    /** One-line op-header descriptions (§G27.22b), indexed by op id. */
    static final String[] DESC = {
            "Change one setting (glow / hardness / sound / collision) on every selected block.",
            "Add a prefix/suffix to, or find-and-replace in, each block's display name.",
            "Move every selected block into one category (\"none\" clears it).",
            "Copy each block to <id>_copy (bumps _copy2, _copy3…).",
            "Write the selected blocks to a file in the chosen format.",
            "Lock or unlock the selected blocks (locked blocks skip other ops).",
            "Favorite or unfavorite the selected blocks (per-player).",
            "Re-id each block from a pattern ({n} auto-numbers: planet_{n} → planet_1…).",
            "Delete the blocks and their placed copies — one Undo restores both.",
            "Hue-shift the texture of every selected block (edge mode, one Undo reverts)."
    };

    // Value lists (mirror BulkSession + BulkValues).
    static final String[] PROPERTIES      = {"glow", "hardness", "sound", "collision"};
    static final String[] GLOW_VALUES     = {"0", "4", "8", "12", "15"};
    static final String[] HARDNESS_VALUES = {"instant", "stone", "5", "20", "unbreakable"};
    static final String[] COLLISION_VALUES = {"solid", "passable"};
    static final String[] EXPORT_FORMATS  = {"json", "txt", "csv", "md", "html", "yaml", "png"};
    static final String[] TEXT_MODES      = {"prefix", "suffix", "replace"};
    static final String[] LOCK_MODES      = {"lock", "unlock"};
    static final String[] FAVORITE_MODES  = {"favorite", "unfavorite"};

    /**
     * Which BulkActionPayload op this rail entry sends. The unreachable default is REFRESH, never DELETE:
     * an off-by-one on the rail must not turn into a destructive packet.
     */
    static int payloadOp(int rail) {
        return switch (rail) {
            case OP_PROPERTY  -> BulkActionPayload.PROPERTY;
            case OP_RENAME    -> BulkActionPayload.RENAME;
            case OP_CATEGORY  -> BulkActionPayload.CATEGORY;
            case OP_DUPLICATE -> BulkActionPayload.DUPLICATE;
            case OP_EXPORT    -> BulkActionPayload.EXPORT;
            case OP_LOCK, OP_FAVORITE -> BulkActionPayload.FLAG;
            case OP_REID      -> BulkActionPayload.REID;
            case OP_DELETE    -> BulkActionPayload.DELETE;
            case OP_RECOLOR   -> BulkActionPayload.RECOLOR;
            default           -> BulkActionPayload.REFRESH;
        };
    }

    /**
     * True when the server handler skips locked blocks for this op — so the preview marks them skipped.
     * Duplicate and Export only READ the source, and the flag ops are about flags themselves, so none of
     * the three skip. Getting this wrong would put a false "🔒 skipped" row in front of the player.
     */
    static boolean skipsLocked(int rail) {
        return switch (rail) {
            case OP_PROPERTY, OP_RENAME, OP_CATEGORY, OP_REID, OP_DELETE, OP_RECOLOR -> true;
            default -> false;
        };
    }

    /** Destructive ops get the red confirm styling. */
    static boolean destructive(int rail) { return rail == OP_DELETE; }

    /**
     * Map a snapshot op-key (from BulkSnapshot.openForOp) back to a rail op, so a named bulk* command lands on
     * its own tab. Unknown / bare → OP_PROPERTY, but the caller only shows the op when a key was actually sent.
     */
    static int railForKey(String key) {
        return switch (key == null ? "" : key) {
            case "rename"    -> OP_RENAME;
            case "category"  -> OP_CATEGORY;
            case "duplicate" -> OP_DUPLICATE;
            case "export"    -> OP_EXPORT;
            case "lock"      -> OP_LOCK;
            case "favorite"  -> OP_FAVORITE;
            case "reid"      -> OP_REID;
            case "delete"    -> OP_DELETE;
            case "recolor"   -> OP_RECOLOR;
            default          -> OP_PROPERTY;
        };
    }

    /** No preview column makes sense for Export — it writes a file, it changes no block. */
    static boolean hasPreview(int rail) { return rail != OP_EXPORT; }

    /** The value choices for a property (sound reads the live registry list). */
    static String[] valuesForProperty(String prop) {
        return switch (prop) {
            case "hardness"  -> HARDNESS_VALUES;
            case "sound"     -> SlotBlock.SOUND_TYPES;
            case "collision" -> COLLISION_VALUES;
            default          -> GLOW_VALUES;
        };
    }

    /** A sensible starting value when the property changes (mirrors BulkSession). */
    static String defaultValueForProperty(String prop) {
        return switch (prop) {
            case "hardness"  -> "stone";
            case "sound"     -> SlotBlock.SOUND_TYPES.length > 0 ? SlotBlock.SOUND_TYPES[0] : "stone";
            case "collision" -> "solid";
            default          -> "8";
        };
    }

    /** Cycle {@code current} through {@code options} by {@code dir} (+1 / -1), wrapping. */
    static String cycle(String[] options, String current, int dir) {
        if (options.length == 0) return current;
        int i = 0;
        for (int k = 0; k < options.length; k++) {
            if (options[k].equalsIgnoreCase(current)) { i = k; break; }
        }
        return options[((i + dir) % options.length + options.length) % options.length];
    }

    /** The sentence the confirm modal leads with, e.g. "Delete 3 block(s)?". */
    static String confirmVerb(int rail) {
        return switch (rail) {
            case OP_PROPERTY  -> "Edit";
            case OP_RENAME    -> "Rename";
            case OP_CATEGORY  -> "Move";
            case OP_DUPLICATE -> "Duplicate";
            case OP_EXPORT    -> "Export";
            case OP_LOCK      -> "Change the lock on";
            case OP_FAVORITE  -> "Change the favorite on";
            case OP_REID      -> "Re-ID";
            case OP_DELETE    -> "Delete";
            case OP_RECOLOR   -> "Recolor";
            default           -> "Apply to";
        };
    }
}
