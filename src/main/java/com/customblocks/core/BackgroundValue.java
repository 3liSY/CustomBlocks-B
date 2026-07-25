/**
 * BackgroundValue.java — G10 §C: the ONE canonical form of a block's stored background choice.
 *
 * A block's background is one of three things, and every surface (command, Studio, bulk, tool)
 * writes the same string so there is only ever one background system:
 *
 *   "black"        — the default safe fill; the texture is composited onto solid black.
 *   "#RRGGBB"      — composited onto that colour (29-colour library name or free hex, both
 *                    resolved to canonical hex here so storage never holds two spellings of red).
 *   "transparent"  — source alpha is kept and the block renders see-through.
 *
 * NOT YET RENDERABLE — "transparent" is a valid stored value and parses here, but slot blocks
 * are not registered on the cutout render layer, so an atlas block with alpha does not render
 * see-through today. That registration is Group 14's piece of the contract (GROUP_10 §B
 * "Boundary": G10 owns the stored value and re-bake, G14 owns the client cutout-layer
 * registration). Until it lands, {@link #renderable} is false for transparent and the command
 * surfaces refuse it with an honest message rather than storing a value that bakes wrong.
 *
 * Depends on: ColorLibrary (name/hex resolution).
 * Called by:  SlotData (normalization), BackgroundService (re-bake), BackgroundCommands.
 */
package com.customblocks.core;

import java.util.Locale;

public final class BackgroundValue {

    private BackgroundValue() {} // static-only

    /** Composited onto solid black — the default for every block that has never been set. */
    public static final String BLACK = "black";
    /** Source alpha kept. Parses and stores, but see the class note: not renderable yet. */
    public static final String TRANSPARENT = "transparent";
    /** What a block with no stored choice behaves as (and what legacy slots.json entries load as). */
    public static final String DEFAULT = BLACK;

    /**
     * Canonicalize any typed/stored value: {@code "black"}, {@code "transparent"}, or {@code "#RRGGBB"}.
     * Returns null when the input is not a background value at all, so callers can show a usage error.
     * A null/blank input normalizes to {@link #DEFAULT} — that is the "unset" case, not a typo.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT;
        String low = raw.trim().toLowerCase(Locale.ROOT);
        if (low.equals(BLACK)) return BLACK;
        if (low.equals(TRANSPARENT) || low.equals("clear") || low.equals("alpha")) return TRANSPARENT;
        return ColorLibrary.resolve(low); // name or hex → "#RRGGBB"; null when unparseable
    }

    /** True when the value keeps source alpha instead of compositing onto a fill. */
    public static boolean isTransparent(String value) {
        return TRANSPARENT.equals(value);
    }

    /**
     * True when this background can actually be baked AND rendered correctly right now.
     * Transparent is false until Group 14 registers slot blocks on the cutout layer.
     */
    public static boolean renderable(String value) {
        return !isTransparent(value);
    }

    /**
     * The 0xRRGGBB fill this background composites onto, or -1 for transparent (nothing to fill).
     * Black is 0x000000, matching the fill BackgroundRemover has always used.
     */
    public static int rgb(String value) {
        if (value == null || isTransparent(value)) return -1;
        if (BLACK.equals(value)) return 0x000000;
        try {
            return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0x000000; // a corrupt stored value bakes as the safe default rather than throwing
        }
    }

    /** Player-facing name: "Black", "Transparent", a library colour name, or the raw hex. */
    public static String label(String value) {
        if (value == null) return "Black";
        if (BLACK.equals(value)) return "Black";
        if (isTransparent(value)) return "Transparent";
        String named = ColorLibrary.nameForHex(value);
        return named != null ? named : value;
    }
}
