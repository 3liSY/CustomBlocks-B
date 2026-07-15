/**
 * ColorVariantNameMigration.java
 *
 * Responsibility: G06-5 one-time, idempotent clean-up of colour-variant blocks created BEFORE the
 * G06-5 naming rules. Two fixes, both no-ops once applied:
 *   - NAME: "Vart (Green)" -> "Vart Green", "A4 Black (#FF1493)" -> "A4 Magenta" (drop the brackets,
 *     turn a "#hex" parenthetical into its nearest preset name, and strip a prior colour word so the
 *     name doesn't compound). Only touches a parenthetical that IS a colour word or "#hex" -- a name
 *     like an Arabic letter block "Alef (...)" is left exactly as it is.
 *   - ID: legacy "..._hex_ff1493" -> "..._magenta" (the nearest preset's slug). Collisions (two old
 *     hexes that round to the same name) keep BOTH blocks via a "_2"/"_3" suffix.
 *
 * Runs once right after SlotManager.loadAll() + migrateDisplayNames(), through the existing safe APIs
 * (SlotManager.reId migrates every id-keyed reference; rename re-mirrors the texture name). reId is
 * its own inverse, so a mistaken id move is reversible with /cb reId.
 *
 * Depends on: SlotManager, SlotData, ColorLibrary.
 * Called by:  CustomBlocksMod (boot, once).
 */
package com.customblocks.core;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorVariantNameMigration {

    private ColorVariantNameMigration() {} // static-only

    /** "<base> (<inner>)" -- greedy base so the LAST parenthetical is taken as the colour token. */
    private static final Pattern PAREN = Pattern.compile("^(.*) \\((.+)\\)$");
    /** A legacy variant id ending in a raw hex colour. */
    private static final Pattern HEX_ID = Pattern.compile("(?i)^(.*)_hex_([0-9a-f]{6})$");

    /** How many blocks the run changed, for the boot log. */
    public record Result(int names, int ids) {
        public boolean any() { return names > 0 || ids > 0; }
    }

    /** Run the one-time clean-up. Idempotent: already-clean blocks re-derive to themselves. */
    public static synchronized Result migrate() {
        int names = 0, ids = 0;
        for (SlotData d : new ArrayList<>(SlotManager.assignedSlots())) {
            // ID first, so the rename below targets the final id.
            String newId = cleanId(d.customId());
            if (!newId.equals(d.customId())) {
                String free = newId;
                for (int n = 2; SlotManager.hasId(free); n++) free = newId + "_" + n;
                SlotData moved = SlotManager.reId(d.customId(), free);
                if (moved != null) { ids++; d = moved; }
            }
            String newName = cleanName(d.displayName());
            if (!newName.equals(d.displayName()) && SlotManager.rename(d.customId(), newName) != null) {
                names++;
            }
        }
        return new Result(names, ids);
    }

    /** "A4 Black (#FF1493)" -> "A4 Magenta"; a non-colour parenthetical is left unchanged. */
    static String cleanName(String name) {
        if (name == null) return null;
        Matcher m = PAREN.matcher(name.strip());
        if (!m.matches()) return name;
        String colour = colourFromToken(m.group(2).strip());
        if (colour == null) return name; // not a colour-variant name (e.g. an Arabic block) -> leave it
        String base = ColorLibrary.stripTrailingColorName(m.group(1).strip());
        return base + " " + colour;
    }

    /** "mars_hex_ff1493" -> "mars_magenta"; anything else returned unchanged. */
    static String cleanId(String id) {
        if (id == null) return null;
        Matcher m = HEX_ID.matcher(id);
        if (!m.matches()) return id;
        int rgb = Integer.parseInt(m.group(2), 16);
        return m.group(1) + "_" + ColorLibrary.slugOf(ColorLibrary.nearestName(rgb));
    }

    /** A parenthetical token -> a preset colour name, or null when it isn't a colour. */
    private static String colourFromToken(String token) {
        String hex = token.startsWith("#") ? token.substring(1) : token;
        if (hex.matches("(?i)[0-9a-f]{6}")) return ColorLibrary.nearestName(Integer.parseInt(hex, 16));
        return ColorLibrary.nameForSlug(ColorLibrary.slugOf(token)); // a colour word -> canonical, else null
    }
}
