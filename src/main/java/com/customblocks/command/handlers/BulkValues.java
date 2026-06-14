/**
 * BulkValues.java
 *
 * Value parsing + validation for the Bulk Dashboard's "edit a setting" mode (Group 07).
 * Split out of BulkCommands to keep that handler under the 400-line gate (§9.3). Mirrors the
 * single-block setter rules in AttributeCommands so a bulk edit accepts exactly what /cb setX does.
 *
 * Called by: BulkCommands.
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Locale;

public final class BulkValues {

    private BulkValues() {} // static-only

    /** Validated value for one property; only the field matching the property is meaningful. */
    public static final class Parsed {
        public int intVal;
        public float floatVal;
        public String strVal;
        public boolean boolVal;
        public String display;
    }

    /** Parse + validate the value for a property. Prints an error and returns null if invalid. */
    public static Parsed parse(ServerCommandSource src, String prop, String value) {
        Parsed pv = new Parsed();
        switch (prop) {
            case "glow", "light" -> {
                try {
                    pv.intVal = Math.max(0, Math.min(15, Integer.parseInt(value.trim())));
                    pv.display = String.valueOf(pv.intVal);
                } catch (NumberFormatException e) {
                    Chat.error(src, "Glow must be a whole number 0-15.");
                    return null;
                }
            }
            case "hardness" -> {
                Float h = parseHardness(value);
                if (h == null) { Chat.error(src, "Hardness must be a number, or: unbreakable, instant, stone."); return null; }
                pv.floatVal = Math.max(-1.0f, Math.min(100.0f, h));
                pv.display = trim(pv.floatVal);
            }
            case "sound" -> {
                String soundKey = value.trim().toLowerCase(Locale.ROOT);
                boolean valid = false;
                for (String s : SlotBlock.SOUND_TYPES) if (s.equals(soundKey)) { valid = true; break; }
                if (!valid) { Chat.error(src, "Unknown sound '" + value + "'. Options: " + String.join(", ", SlotBlock.SOUND_TYPES)); return null; }
                pv.strVal = soundKey;
                pv.display = soundKey;
            }
            case "collision" -> {
                Boolean passable = parseCollision(value);
                if (passable == null) { Chat.error(src, "Collision must be: solid or passable."); return null; }
                pv.boolVal = passable;
                pv.display = passable ? "passable" : "solid";
            }
            default -> {
                Chat.error(src, "Unknown setting '" + prop + "'. Options: glow, hardness, sound, collision.");
                return null;
            }
        }
        return pv;
    }

    /** Parse a hardness value: a number, or a friendly keyword. Returns null if unrecognized. */
    private static Float parseHardness(String raw) {
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "unbreakable": case "unbreak": case "indestructible": case "bedrock":
                return -1.0f;
            case "instant": case "instabreak": case "instant-break": case "soft":
                return 0.0f;
            case "stone": case "default": case "normal":
                return 1.5f;
            default:
                try { return Float.parseFloat(raw.trim()); }
                catch (NumberFormatException e) { return null; }
        }
    }

    /** Parse a collision mode. Returns true (passable), false (solid), or null if unrecognized. */
    private static Boolean parseCollision(String raw) {
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "passable": case "through": case "walkthrough": case "walk": case "none":
            case "off": case "no": case "false": case "ghost": case "decor":
                return Boolean.TRUE;
            case "solid": case "on": case "yes": case "true": case "normal": case "block":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    /** Drop a trailing ".0" so 50.0 shows as "50" but 1.5 stays "1.5". */
    private static String trim(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }
}
