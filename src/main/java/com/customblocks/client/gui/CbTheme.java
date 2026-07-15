/**
 * CbTheme.java — GROUP 27 colour palette (locked 2026-07-04, correction #1). CLIENT-SIDE ONLY.
 *
 * Single source of truth for the red+black brand. Exact locked colours:
 *   #FF0000 red  — selected / active state (selected tab, active toggle, focused border, title accent)
 *   #000000 black — background/backdrop everywhere (pure black, never a softened near-black)
 *   #40FF00 lime — SUCCESS feedback ONLY (save flash, create-success) — never "selected"
 * Roles never mix: red never means success, lime never means selected.
 *
 * All Group 27 frame screens + shared primitives read from here so a global tweak is one edit.
 *
 * Constants + tiny Text builders for the exact-red bold titles (legacy §c is #FF5555, not the
 * locked #FF0000, so titles are built with a real RGB style instead of a § code).
 *
 * Depends on: minecraft Text/Style. Called by: client GUI screens + panels.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

@Environment(EnvType.CLIENT)
public final class CbTheme {

    private CbTheme() {}

    // ── Chrome (bars, panels, dialogs) — pure-black base ─────────────────────
    /** Title bar + bottom action bar background. */
    public static final int BAR_BG    = 0xE6000000;
    /** Section panel / card fill behind grouped controls. */
    public static final int CARD      = 0x88000000;
    /** Near-opaque black for floating panels / popups (world must not bleed through). */
    public static final int PANEL_BG  = 0xF0000000;
    /** Subtle red hairline edge for the active card. */
    public static final int CARD_EDGE = 0x44FF0000;
    /** Modal dialog background (discard-changes overlay) — pure black. */
    public static final int DIALOG_BG = 0xFF000000;

    // ── Accent (the locked #FF0000 red — selected / active ONLY) ────────────
    /** Primary red — accent lines, borders, selected marker. */
    public static final int ACCENT     = 0xFFFF0000;
    /** Dimmer red for inactive / secondary accents. */
    public static final int ACCENT_DIM = 0xFF7A0000;
    /** Translucent red glow fill (selection highlight, soft underlays). */
    public static final int GLOW       = 0x55FF0000;
    /** Dark-red fill behind a selected chip/tab (pairs with an ACCENT border). */
    public static final int SEL_FILL   = 0xFF2A0000;

    // ── Success (the locked #40FF00 lime — success feedback ONLY) ───────────
    public static final int LIME      = 0xFF40FF00;
    /** Translucent lime "saved!" flash over the primary button. */
    public static final int FLASH_OK  = 0x5540FF00;

    // ── Text ─────────────────────────────────────────────────────────────────
    public static final int TEXT      = 0xFFFFFFFF;
    public static final int TEXT_DIM  = 0xFFB0B0B0;

    private static final Style RED_BOLD =
            Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)).withBold(true);

    /** Bold exact-#FF0000 text (titles, section headings). */
    public static MutableText red(String s) {
        return Text.literal(s).setStyle(RED_BOLD);
    }

    /** Standard screen title: bold red name, no context. */
    public static MutableText title(String name) {
        return Text.empty().append(red(name));
    }

    /** Standard screen title: bold red name + "§7— §f{context}". */
    public static MutableText title(String name, String context) {
        return Text.empty().append(red(name)).append(Text.literal(" §7— §f" + context));
    }
}
