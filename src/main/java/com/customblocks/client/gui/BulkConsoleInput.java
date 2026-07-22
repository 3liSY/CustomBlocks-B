/**
 * BulkConsoleInput.java — Group 07 (Bulk Operations Hub — Console tab input actions). CLIENT-ONLY.
 *
 * Split out of {@link BulkWorkbenchScreen} for the ≤500-line cap. Owns the Console's op-selection and
 * option-cycling actions (the rail/grid op pick, the two value cyclers). All state lives on the Screen
 * (package-private fields); this class only mutates it via a stored ref.
 *
 * Depends on: BulkWorkbenchScreen (state), BulkOpSpec, CbUiSounds.
 * Called by: BulkWorkbenchScreen (bulkClick op-pick / option-cycle).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
     * §G07-B Blocks List Category ▾ pick: resolve a click in the open dropdown ("" = All categories).
     * Lives here (not the Screen) only to keep BulkWorkbenchScreen under the 500-line cap. Returns true if
     * the click hit an option.
     */
    boolean pickCategory(double mx, double my) {
        BulkWorkbenchView v = s.view();
        for (int i = 0; i < v.rCatOpts.size(); i++) {
            if (BulkDraw.in(mx, my, v.rCatOpts.get(i))) {
                s.browseCategory = v.catOptValues.get(i);
                CbUiSounds.click();
                s.categoryOpen = false;
                v.resetScroll();
                return true;
            }
        }
        return false;
    }
}
