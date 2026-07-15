/**
 * BulkApply.java — Group 07 §G07-3 (Bulk Workbench Screen — server side).
 *
 * The nine structured entry points the Workbench's Apply button reaches. Each takes the filter and every
 * argument as its OWN string, resolves the scope through BulkScope, and hands the resolved block list to the
 * op's real handler core.
 *
 * ⚠️ Why this class exists at all: the chest dashboard's old `apply*FromGui` helpers glued their arguments
 * back into a command line (`scope + " " + mode + " " + a`) and let the handler re-split it on whitespace.
 * Any argument with a space in it — a category literally named "red stone", a rename prefix of "old " —
 * was silently mis-parsed or dropped. Every path through here passes arguments as arguments. (Those
 * helpers were deleted with the chest menus; this is the only GUI door left.)
 *
 * Two other deliberate differences from the chat path:
 *   • No confirm guard. The Screen runs its own in-screen confirm modal before it ever sends the packet, so
 *     bouncing the player to a chat [✔ Confirm] would be a second gate on an already-confirmed action.
 *   • Nothing closes the player's screen. The Workbench stays open and refreshes in place.
 * The result chat line (with its [↩ Undo] button) is still printed — that IS the undo affordance.
 *
 * Depends on: BulkScope, BulkConfirm (actor), BulkValues, and each Bulk*Commands' handler core.
 * Called by:  BulkNet (the Bulk Workbench Screen).
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Locale;

public final class BulkApply {

    private BulkApply() {} // static-only

    /**
     * Resolve a filter to blocks, reporting an empty match. Null means "stop, nothing to do".
     *
     * ⚠️ The blank check is load-bearing. {@code BulkScope.isAll("")} is true, so a blank scope resolves to
     * EVERY block — and unlike the chat path, nothing here re-asks for confirmation (the Screen already showed
     * its modal). A dropped or empty scope on a DELETE would therefore wipe the whole mod silently. The Screen
     * greys out Apply on an empty selection, but that is a client-side guard on a packet any client can forge,
     * so the server refuses a blank scope outright. Escalating to "all" is still allowed — it arrives as the
     * explicit expression "all", which the player confirmed by name and count in the modal.
     */
    private static List<SlotData> resolve(ServerCommandSource src, String filter) {
        if (filter == null || filter.isBlank()) {
            Chat.error(src, "No blocks selected — tick some blocks first.");
            return null;
        }
        List<SlotData> blocks = BulkScope.resolve(filter, BulkConfirm.actor(src));
        if (blocks.isEmpty()) {
            Chat.error(src, "No blocks matched: " + filter);
            return null;
        }
        return blocks;
    }

    /** Edit a setting — glow / hardness / sound / collision. Validates the value before touching a block. */
    public static void property(ServerCommandSource src, String filter, String prop, String value) {
        String property = prop.toLowerCase(Locale.ROOT);
        BulkValues.Parsed pv = BulkValues.parse(src, property, value); // prints its own error
        if (pv == null) return;
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkCommands.applyProperty(src, blocks, property, pv);
    }

    /** Rename display names by prefix / suffix / find-and-replace. */
    public static void rename(ServerCommandSource src, String filter, String mode, String a, String b) {
        String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        if (!isTextMode(m)) { BulkCommands.usageRename(src); return; }
        if (!hasText(src, m, a)) return;
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkCommands.applyRename(src, blocks, m, a, b == null ? "" : b);
    }

    /** Re-id blocks by prefix / suffix / find-and-replace (slots and textures are untouched). */
    public static void reid(ServerCommandSource src, String filter, String mode, String a, String b) {
        String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        if (!isTextMode(m)) { BulkReidCommands.usage(src); return; }
        if (!hasText(src, m, a)) return;
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkReidCommands.applyReid(src, blocks, m, a, b == null ? "" : b);
    }

    /**
     * Re-id by an EXPLICIT per-block map from the B9 hybrid editor: {@code mapping} is "old1=new1,old2=new2,…".
     * Each new id was chosen by hand, so instead of a pattern transform we hand the parsed pairs straight to the
     * handler core, which re-checks locked / invalid / taken / batch-clash at apply time (the preview never wins).
     */
    public static void reidMap(ServerCommandSource src, String mapping) {
        if (mapping == null || mapping.isBlank()) { Chat.error(src, "No id changes to apply."); return; }
        List<String[]> pairs = new java.util.ArrayList<>();
        for (String part : mapping.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0 || eq >= part.length() - 1) continue;
            String oldId = part.substring(0, eq).trim();
            String newId = part.substring(eq + 1).trim();
            if (!oldId.isEmpty() && !newId.isEmpty()) pairs.add(new String[]{oldId, newId});
        }
        if (pairs.isEmpty()) { Chat.error(src, "No id changes to apply."); return; }
        BulkReidCommands.applyReidExplicit(src, pairs);
    }

    /** Move every matched block to a category ("none" / "" clears it). */
    public static void category(ServerCommandSource src, String filter, String category) {
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkCategoryCommands.applyCategory(src, blocks, BulkCategoryCommands.normalize(category));
    }

    /** Duplicate every matched block into "&lt;id&gt;_copy" (then _copy2, _copy3…). */
    public static void duplicate(ServerCommandSource src, String filter) {
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkDuplicateCommands.applyDuplicate(src, blocks);
    }

    /** Write the matched blocks out in one of the seven formats. Read-only; no undo entry. */
    public static void export(ServerCommandSource src, String filter, String format) {
        String f = format == null || format.isBlank() ? "json" : format.toLowerCase(Locale.ROOT);
        if (!BulkExportCommands.isExportFormat(f)) {
            Chat.error(src, "Unknown export format \"" + format + "\".");
            return;
        }
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkExportCommands.writeExport(src, blocks, f);
    }

    /**
     * Flip lock / unlock / favorite / unfavorite. Each is its own inverse, so no undo entry is recorded.
     * This one resolves its own scope inside BulkFlagCommands (favorites need the player), so it repeats the
     * blank-scope refusal rather than going through {@link #resolve}.
     */
    public static void flag(ServerCommandSource src, String op, String filter) {
        String o = op == null ? "" : op.toLowerCase(Locale.ROOT);
        if (filter == null || filter.isBlank()) {
            Chat.error(src, "No blocks selected — tick some blocks first.");
            return;
        }
        switch (o) {
            case "lock", "unlock", "favorite", "unfavorite" -> BulkFlagCommands.run(src, o, filter);
            default -> Chat.error(src, "Unknown flag op \"" + op + "\".");
        }
    }

    /** Delete every matched block; placed copies become "Deleted: …" markers. One undo restores the batch. */
    public static void delete(ServerCommandSource src, String filter) {
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkCommands.applyDelete(src, blocks);
    }

    /**
     * Batch hue-shift every matched block's texture (§G27.22b Recolor). {@code hueArg} is the shift in degrees;
     * a 0° (or 360°) shift is refused rather than churning the pack for a no-op. The transform is hue-only
     * (saturation/lightness untouched) — the reversible, batch-safe path (KNOWN_PITFALLS). One undo reverts all.
     */
    public static void recolor(ServerCommandSource src, String filter, String hueArg) {
        double raw;
        try { raw = Double.parseDouble(hueArg == null ? "0" : hueArg.trim()); }
        catch (NumberFormatException e) { raw = 0; }
        double hueDeg = ((raw % 360) + 360) % 360;
        if (hueDeg == 0) { Chat.error(src, "Pick a hue shift first — 0° changes nothing."); return; }
        List<SlotData> blocks = resolve(src, filter);
        if (blocks == null) return;
        BulkRecolorCommands.applyRecolor(src, blocks, hueDeg);
    }

    private static boolean isTextMode(String m) {
        return m.equals("prefix") || m.equals("suffix") || m.equals("replace");
    }

    /** All three text modes need a non-empty text A ("replace" may have an empty B — that deletes a substring). */
    private static boolean hasText(ServerCommandSource src, String mode, String a) {
        if (a != null && !a.isEmpty()) return true;
        Chat.error(src, "Type the text to " + mode + " first.");
        return false;
    }
}
