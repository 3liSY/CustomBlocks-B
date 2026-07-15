/**
 * BulkConsoleInput.java — Group 07 (Bulk Operations Hub — Console tab input actions). CLIENT-ONLY.
 *
 * Split out of {@link BulkWorkbenchScreen} for the ≤500-line cap. Owns the Console's op-selection and
 * option-cycling actions (the rail/grid op pick, the two value cyclers) plus the NL-bar hand-off: it drops a
 * parsed {@link BulkNlParser.Nl} into the Console's op + filter state so the normal preview + Execute confirm
 * take over. All state lives on the Screen (package-private fields); this class only mutates it via a stored ref.
 *
 * Depends on: BulkWorkbenchScreen (state), BulkOpSpec, BulkNlParser, CbUiSounds.
 * Called by: BulkWorkbenchScreen (bulkClick op-pick / option-cycle, applyNl delegate).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
final class BulkConsoleInput {

    private final BulkWorkbenchScreen s;

    BulkConsoleInput(BulkWorkbenchScreen s) { this.s = s; }

    /** Rail op pick: switch the active op (no-op if already selected). */
    void setOp(int i) {
        if (s.opIndex == i) return;
        s.opIndex = i;
        CbUiSounds.click();
        s.ops.resetScrolls();
        s.rebuild();
    }

    /** Primary option cycler (meaning depends on the active op). */
    void cycleA(int dir) {
        CbUiSounds.click();
        switch (s.opIndex) {
            case BulkOpSpec.OP_PROPERTY -> {
                s.property = BulkOpSpec.cycle(BulkOpSpec.PROPERTIES, s.property, dir);
                s.value = BulkOpSpec.defaultValueForProperty(s.property);
            }
            case BulkOpSpec.OP_RENAME -> {
                s.textMode = BulkOpSpec.cycle(BulkOpSpec.TEXT_MODES, s.textMode, dir);
                s.rebuild();   // replace-mode adds a second text field
            }
            case BulkOpSpec.OP_EXPORT   -> s.exportFormat = BulkOpSpec.cycle(BulkOpSpec.EXPORT_FORMATS, s.exportFormat, dir);
            case BulkOpSpec.OP_LOCK     -> s.lockMode = BulkOpSpec.cycle(BulkOpSpec.LOCK_MODES, s.lockMode, dir);
            case BulkOpSpec.OP_FAVORITE -> s.favMode = BulkOpSpec.cycle(BulkOpSpec.FAVORITE_MODES, s.favMode, dir);
            default -> { }
        }
    }

    /** Secondary option cycler — only the property value has one. */
    void cycleB(int dir) {
        if (s.opIndex != BulkOpSpec.OP_PROPERTY) return;
        CbUiSounds.click();
        s.value = BulkOpSpec.cycle(BulkOpSpec.valuesForProperty(s.property), s.value, dir);
    }

    /**
     * NL bar hand-off (§G27.22b model): drop the parsed op + control values into the Bulk Actions tab AND select
     * the blocks the phrase matched (into the shared target set), so the include-checkbox rows + preview populate.
     * Nothing runs — the player still reviews the rows and clicks Execute.
     */
    void applyNl(BulkNlParser.Nl n) {
        s.tab = BulkWorkbenchScreen.TAB_BULK; s.pick = false;
        s.opIndex = n.opIndex();
        switch (s.opIndex) {
            case BulkOpSpec.OP_PROPERTY -> { s.property = n.property(); s.value = n.value(); }
            case BulkOpSpec.OP_CATEGORY -> s.textA = n.value();
            case BulkOpSpec.OP_RENAME   -> { s.textMode = n.textMode(); s.textA = n.value(); }
            case BulkOpSpec.OP_LOCK     -> s.lockMode = n.value();
            case BulkOpSpec.OP_FAVORITE -> s.favMode = n.value();
            case BulkOpSpec.OP_EXPORT   -> s.exportFormat = n.value();
            default -> { }
        }
        // Evaluate the phrase's single filter and tick exactly those blocks.
        s.fb().setSingle(n.filterKind(), n.filterValue(), n.filterNeg());
        List<ClientSlotCache.Entry> matches =
                BulkWorkbenchModel.matchingBool(s.fb().conds(), s.fb().conns(), s.locked(), s.fav());
        s.ticked().clear();
        for (ClientSlotCache.Entry e : matches) s.ticked().add(e.id());
        s.clearExcluded();
        s.search = "";
        s.rebuild();
    }
}
