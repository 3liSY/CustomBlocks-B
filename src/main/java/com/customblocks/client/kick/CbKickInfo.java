/**
 * CbKickInfo.java — GROUP 04 §G04-5 unified kick data. CLIENT-SIDE ONLY.
 *
 * ONE shape for every CustomBlocks-caused kick or connection failure (owner-locked 2026-07-15). The
 * player always sees a human explanation first, in three parts — What happened, Why, How to fix it —
 * never a raw exception, registry count or Java class name. The deeper evidence rides along in
 * {@link #rawError} / {@link #technicalDetails} for the Technical Details expander and the Copy Report
 * button; it never becomes the headline.
 *
 * This record is the single source of truth the kick Screen renders and the report is built from, so a
 * new cause is just a new factory in {@link CbKick} — the layout, colours and report never change.
 *
 * Depends on: nothing (plain data + a Text/report builder). Used by: CbKick, CbKickScreen.
 */
package com.customblocks.client.kick;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

@Environment(EnvType.CLIENT)
public record CbKickInfo(
        String title,            // the red headline, e.g. "CustomBlocks could not connect"
        String whatHappened,     // plain explanation (white)
        String why,              // likely cause in normal language (yellow)
        String howToFix,         // concrete steps (green)
        String errorCode,        // short pasteable code, or "" if none
        String rawError,         // the original raw MC/CB error (gray, under Technical Details)
        String technicalDetails, // deeper diagnostic (gray, under Technical Details)
        String likelyCause,      // one-line cause for the report
        String recommendedFix,   // one-line fix for the report
        String serverType,       // dedicated / integrated / realm …
        String connectionPhase,  // login / configuration / play …
        String recentContext     // what the player was doing
) {

    // ── The §G04-5 colour contract (semantic, not decoration) ──────────────────
    public static final int TITLE = 0xFFFF0000; // red
    public static final int WHAT  = 0xFFFFFFFF; // white
    public static final int WHY   = 0xFFFFE04D; // yellow
    public static final int FIX   = 0xFF40FF00; // green
    public static final int CTRL  = 0xFF55D0FF; // light blue (Details + Copy Report)
    public static final int RAW   = 0xFFA0A0A0; // gray (raw log + technical text)

    private static Text colored(String s, int rgb, boolean bold) {
        Style st = Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0x00FFFFFF)).withBold(bold);
        return Text.literal(s).setStyle(st);
    }

    /** The coloured disconnect reason Text — the fallback vanilla would show if our Screen never opened. */
    public MutableText reason() {
        MutableText t = Text.empty().append(colored(title, TITLE, true));
        t.append(Text.literal("\n\n")).append(colored("What happened:", WHAT, true))
                .append(Text.literal("\n")).append(colored(whatHappened, WHAT, false));
        t.append(Text.literal("\n\n")).append(colored("Why:", WHY, true))
                .append(Text.literal("\n")).append(colored(why, WHY, false));
        t.append(Text.literal("\n\n")).append(colored("How to fix it:", FIX, true))
                .append(Text.literal("\n")).append(colored(howToFix, FIX, false));
        return t;
    }

    /**
     * The complete, pasteable Copy Report. Named report, NEVER "AI report", and it mentions no AI: it is
     * a plain bug-report body a player can drop into any tracker or helper conversation. It works from
     * the kick screen even while the player cannot join, because every field is captured up front.
     */
    public String report() {
        StringBuilder b = new StringBuilder();
        b.append("=== CustomBlocks connection report ===\n\n");
        b.append(title).append('\n');
        if (!errorCode.isEmpty()) b.append("Error code: ").append(errorCode).append('\n');
        b.append('\n');
        b.append("What happened: ").append(whatHappened).append('\n');
        b.append("Why: ").append(why).append('\n');
        b.append("How to fix it: ").append(howToFix).append('\n');
        b.append('\n');
        b.append("Likely cause: ").append(likelyCause).append('\n');
        b.append("Recommended fix: ").append(recommendedFix).append('\n');
        b.append("Connection phase: ").append(connectionPhase).append('\n');
        b.append("Server type: ").append(serverType).append('\n');
        b.append("Recent action: ").append(recentContext).append('\n');
        b.append('\n');
        b.append("Minecraft version: ").append(CbKick.mcVersion()).append('\n');
        b.append("CustomBlocks version: ").append(CbKick.cbVersion()).append('\n');
        b.append('\n');
        b.append("--- Technical details ---\n");
        b.append(technicalDetails).append('\n');
        b.append("--- Raw error ---\n");
        b.append(rawError).append('\n');
        return b.toString();
    }
}
