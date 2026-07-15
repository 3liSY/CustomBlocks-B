/**
 * BulkNlBar.java — Group 07 §G07-4 (natural-language command bar). CLIENT-ONLY.
 *
 * The always-visible NL input field + its parse-on-Enter, split out of {@link BulkWorkbenchScreen} for the
 * ≤500-line cap. It owns the field, its text (survives widget rebuilds) and the preview string; on Enter it runs
 * {@link BulkNlParser} and hands a successful parse back to the Screen ({@link BulkWorkbenchScreen#applyNl}),
 * which drops the op + filter into the Console so the normal preview + Execute confirm take over.
 *
 * Depends on: BulkNlParser, BulkWorkbenchView (geometry), CbTextField, CbUiSounds.
 * Called by: BulkWorkbenchScreen (build / focus test / onEnter / preview).
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
final class BulkNlBar {

    private CbTextField field;
    private String text = "";
    private String preview;

    String preview() { return preview; }
    boolean isFocused(Element focused) { return field != null && focused == field; }

    /** Build the input field and hand it to the Screen to add as a child (called every init, all tabs). */
    void build(BulkWorkbenchScreen s, TextRenderer tr, int width) {
        field = new CbTextField(tr, BulkWorkbenchView.nlFieldX(), BulkWorkbenchView.nlFieldY(),
                BulkWorkbenchView.nlFieldW(width), 16, Text.literal("ask…"));
        field.setPlaceholder(Text.literal("§8ask: \"delete all locked\" · \"glow 10 on red\" · \"move all to stone\""));
        field.setMaxLength(120);
        field.setText(text);
        field.setChangedListener(v -> text = v);
        s.addNlField(field);
    }

    /** Enter pressed in the field: parse → apply to the Console (never runs — Execute does), or show the reason. */
    void onEnter(BulkWorkbenchScreen s) {
        BulkNlParser.Nl n = BulkNlParser.parse(text);
        if (!n.ok()) { preview = "§c" + n.error(); CbUiSounds.click(); return; }
        s.applyNl(n);
        preview = "§a→ " + n.desc() + "  §7— review, then Execute";
        CbUiSounds.chime();
    }
}
