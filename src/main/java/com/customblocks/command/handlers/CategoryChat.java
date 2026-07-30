/**
 * CategoryChat.java — how a category READS in chat (Group 11 §C, Pass 2).
 *
 * Three locked rules live here, in one place, so no handler has to remember them:
 *   • a category name printed in a [CB] line renders in that category's own colour tag, falling
 *     back to the standard value colour when it has none (C11);
 *   • that name is clickable (opens the Category Hub focused on it) and hoverable (count, icon,
 *     colour) (C12);
 *   • a success line carries the obvious follow-up chip — [↩ Undo] after a delete,
 *     [⊞ View Category] after an assignment (C13).
 *
 * The colouring works by DECORATION, not by every message building components by hand: a
 * CategoryService.Outcome is still a plain sentence, and {@link #decorate} scans it for the display
 * name of a real category on word boundaries and swaps that run for a styled chip. One choke point,
 * so a new category message is styled the moment it is written.
 *
 * {@code Uncategorized} is deliberately NOT clickable: it is the system floor, so it renders
 * dim + italic wherever it appears (C16).
 *
 * Depends on: CategoryMembershipStore, CategoryMetadataStore, Chat, CbFmt
 * Called by:  CategoryCommands.report, CategoryDeletePrompt
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.core.CategoryMembershipStore;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.CategoryService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CategoryChat {

    private CategoryChat() {} // static-only

    /** Report a category Outcome: styled sentence, brand, glyph, and its follow-up chip. */
    static int report(ServerCommandSource src, CategoryService.Outcome o) {
        if (o.ok()) Chat.successRich(src, decorate(o.msg()), chip(o.chip()));
        else        Chat.errorRich(src, decorate(CbFmt.BAD + o.msg()));
        return o.ok() ? 1 : 0;
    }

    /** The follow-up chip an Outcome asked for, or null when it wants none (C13). */
    static MutableText chip(CategoryService.Chip c) {
        if (c == null) return null;
        return switch (c.kind()) {
            case "undo" -> Chat.undoButton();
            case "view" -> viewButton(c.arg());
            default -> null;
        };
    }

    /** [⊞ View Category] — opens the Hub focused on that category. */
    static MutableText viewButton(String category) {
        String shown = CategoryMetadataStore.getDisplayName(category);
        return Chat.runButton(CbFmt.CLICK + "[⊞ View Category]", "/cb category open " + shown,
                "Open " + shown + " in the Category Hub");
    }

    /**
     * One category name as a chat chip: its own colour, a click that opens the Hub on it, and a
     * hover naming count / icon / colour (C10 + D3 + D4).
     *
     * The colour is applied as a real {@link TextColor} on the chip's style, not as a §-code inside
     * its text. A legacy code only reaches the colour it names — the Category Hub's custom
     * {@code #RRGGBB} tint had no §-code to be written as, so a hex-coloured category came out in the
     * default colour in chat while the Hub showed it correctly.
     */
    static MutableText name(String category) {
        String key = CategoryMembershipStore.key(category);
        String shown = CategoryMetadataStore.getDisplayName(key);
        if (CategoryMembershipStore.isUncategorized(key)) {
            // The system floor: dim + italic, never clickable — it is not a category anyone made (D8).
            return Text.literal(shown).styled(s -> s
                    .withColor(Formatting.GRAY).withItalic(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(
                            CbFmt.DIM + "The system floor — a block lands here when it has no other category."))));
        }
        TextColor colour = colorOf(key);
        return Text.literal(shown).styled(s -> s
                .withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cb category open " + shown))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverCard(key, shown)))));
    }

    /**
     * A category's colour: its Hub hex tint first, then its §-colour tag, then the standard value
     * colour for a category that has neither.
     */
    private static TextColor colorOf(String key) {
        String hex = CategoryMetadataStore.getColorHex(key);
        if (!hex.isEmpty()) {
            try { return TextColor.fromRgb(Integer.parseInt(hex.substring(1), 16)); }
            catch (NumberFormatException ignored) { /* fall through to the tag */ }
        }
        Formatting f = fromTag(CategoryMetadataStore.getColorTag(key));
        if (f == null) f = fromTag(CbFmt.VALUE);
        return TextColor.fromFormatting(f);
    }

    /** The {@link Formatting} a "§x" tag names, or null when the tag is empty or not a colour. */
    private static Formatting fromTag(String tag) {
        if (tag == null || tag.length() < 2) return null;
        Formatting f = Formatting.byCode(tag.charAt(1));
        return f != null && f.isColor() ? f : null;
    }

    /** The hover card behind a category name: count, icon, colour. */
    private static String hoverCard(String key, String shown) {
        int count = CategoryMembershipStore.blocksIn(key).size();
        String icon = CategoryMetadataStore.getDisplayBlock(key);
        String tag = CategoryMetadataStore.getColorTag(key);
        String hex = CategoryMetadataStore.getColorHex(key);
        StringBuilder sb = new StringBuilder();
        sb.append(tag.isEmpty() ? CbFmt.VALUE : tag).append(shown).append("\n");
        sb.append(CbFmt.DIM).append("Blocks: ").append(CbFmt.BODY).append(count).append("\n");
        sb.append(CbFmt.DIM).append("Icon: ").append(CbFmt.BODY).append(icon == null ? "default" : icon).append("\n");
        sb.append(CbFmt.DIM).append("Colour: ")
          .append(!hex.isEmpty() ? CbFmt.BODY + hex
                  : tag.isEmpty() ? CbFmt.FAINT + "default" : tag + "this colour");
        sb.append("\n").append(CbFmt.CLICK).append("Click to open it in the Category Hub");
        return sb.toString();
    }

    /**
     * Swap every real category name inside {@code msg} for its styled chip.
     *
     * Matching is case-insensitive and bounded: a name only counts when the characters around it are
     * not letters, digits or underscores, so the category {@code food} never lights up inside the
     * block id {@code food_block}. Longest names are tried first, so {@code Arabic Numbers} wins over
     * a category also called {@code Arabic}.
     */
    static MutableText decorate(String msg) {
        List<String> names = knownNames();
        MutableText out = Text.literal("");
        StringBuilder buf = new StringBuilder(CbFmt.BODY);
        String carried = CbFmt.BODY;   // the last §-code seen, re-emitted after a chip so the run keeps its colour
        int i = 0;
        while (i < msg.length()) {
            char c = msg.charAt(i);
            if (c == '§' && i + 1 < msg.length()) {
                carried = msg.substring(i, i + 2);
                buf.append(carried);
                i += 2;
                continue;
            }
            String hit = matchAt(msg, i, names);
            if (hit == null) { buf.append(c); i++; continue; }
            if (buf.length() > 0) { out.append(Text.literal(buf.toString())); buf.setLength(0); }
            out.append(name(hit));
            buf.append(carried);       // whatever colour the sentence was in before the chip
            i += hit.length();
        }
        if (buf.length() > 0) out.append(Text.literal(buf.toString()));
        return out;
    }

    /** The longest known category name starting at {@code i} on a word boundary, or null. */
    private static String matchAt(String msg, int i, List<String> names) {
        char before = i == 0 ? ' ' : msg.charAt(i - 1);
        if (isWordChar(before)) return null;
        for (String n : names) {
            int end = i + n.length();
            if (end > msg.length()) continue;
            if (!msg.regionMatches(true, i, n, 0, n.length())) continue;
            if (end < msg.length() && isWordChar(msg.charAt(end))) continue;
            return n;
        }
        return null;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Every real category's display name, longest first. */
    private static List<String> knownNames() {
        java.util.Set<String> keys = new java.util.TreeSet<>(CategoryMetadataStore.knownCategories());
        keys.addAll(CategoryMembershipStore.keysInUse());
        keys.add(CategoryMembershipStore.UNCATEGORIZED);
        List<String> names = new ArrayList<>();
        for (String k : keys) {
            String n = CategoryMetadataStore.getDisplayName(k);
            if (n != null && !n.isBlank()) names.add(n);
        }
        names.sort(Comparator.comparingInt(String::length).reversed()
                .thenComparing(s -> s.toLowerCase(Locale.ROOT)));
        return names;
    }
}
