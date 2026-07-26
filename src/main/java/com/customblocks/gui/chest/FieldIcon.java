/**
 * FieldIcon.java
 *
 * Responsibility: Render ONE {@link com.customblocks.config.ConfigField} as a decorated chest item
 * for the Settings Book (Group 21, spec section 4 "living items"). The item, its name + current
 * value, a plain-English help tooltip (always shown, D10), the enchant shimmer on editable slots,
 * and the small star marker when a value differs from its shipped default all come from here, so the
 * menu code never hand-builds a setting's item.
 *
 * Pure rendering -- reads the field, returns an ItemStack. No side effects, no navigation.
 *
 * Depends on: ConfigField, Icons, Minecraft Items.
 * Called by:  SettingsBookMenu (and the sub-chests in later phases).
 */
package com.customblocks.gui.chest;

import com.customblocks.config.ConfigField;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class FieldIcon {

    private FieldIcon() {} // static-only

    /** The star shown beside a setting whose value differs from its shipped default. */
    public static final String CHANGED_MARK = " §b✦";

    /** Build the living item for a setting, ready to drop into a ChestMenu slot. */
    public static ItemStack of(ConfigField f) {
        String name = nameLine(f);
        List<String> lore = new ArrayList<>();
        wrap(f.help, lore);                 // plain-English explanation (always, D10)
        lore.add(" ");
        actionHint(f, lore);                // what a click does
        if (f.restart) lore.add("§cNeeds a server restart");
        if (f.hasValue() && !f.isDefault()) lore.add("§b✦ Changed from default");

        Item item = itemFor(f);
        ItemStack stack = Icons.ofCoded(item, name, lore.toArray(new String[0]));
        if (shimmer(f)) stack.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    // ── name + value ──────────────────────────────────────────────────────────
    private static String nameLine(ConfigField f) {
        switch (f.type) {
            case COMING_SOON:
                return "§8§l" + f.name + " §8(soon)";
            case ACTION:
            case GROUP:
                return "§e§l" + f.name;
            case ENUM:
                return "§a§l" + f.name + " §f→ " + valueText(f) + changed(f);
            default: // BOOL / INT / STRING / HEX
                return "§a§l" + f.name + " §f= " + valueText(f) + changed(f);
        }
    }

    private static String changed(ConfigField f) {
        return (f.hasValue() && !f.isDefault()) ? CHANGED_MARK : "";
    }

    private static String valueText(ConfigField f) {
        switch (f.type) {
            case BOOL:   return f.asBool() ? "§aON" : "§cOFF";
            case ENUM:   return "§e" + prettyEnum(f);
            case INT:    return "§a" + f.get();
            case HEX:    return "§f" + f.get();
            case STRING: {
                String v = f.get();
                return (v == null || v.isEmpty()) ? "§8(not set)" : "§f" + trim(v, 28);
            }
            default:     return "";
        }
    }

    /** Friendlier text for a few enums; otherwise the raw token. */
    private static String prettyEnum(ConfigField f) {
        String v = f.get();
        switch (f.key) {
            case "texture_quality":      return v + "px";
            case "history_mode":         return "per_player".equals(v) ? "per-player" : "server-wide";
            case "auto_backup_interval": return "0".equals(v) ? "off" : "every " + v + " min";
            case "background_removal":
                switch (v) { case "auto": return "automatic"; default: return "off"; }
            default: return v;
        }
    }

    // ── tooltip helpers ─────────────────────────────────────────────────────────
    private static void actionHint(ConfigField f, List<String> lore) {
        switch (f.type) {
            case BOOL:        lore.add("§aClick §7to toggle"); break;
            case ENUM:        lore.add("§aClick §7to cycle"); break;
            case INT:         lore.add("§aClick §7to change the number"); break;
            case HEX:         lore.add("§aClick §7to pick a colour"); break;
            case STRING:      lore.add("§aClick §7to edit"); break;
            case ACTION:      lore.add("§aClick §7to open"); break;
            case GROUP:       lore.add("§aClick §7to open"); break;
            case COMING_SOON: lore.add("§8Not available yet"); break;
        }
    }

    /** Editable values, action buttons and sub-chest openers shimmer; read-only / soon stay plain. */
    private static boolean shimmer(ConfigField f) {
        return f.type != ConfigField.Type.COMING_SOON
                && (f.editable() || f.type == ConfigField.Type.ACTION || f.type == ConfigField.Type.GROUP);
    }

    // ── item choice (living items) ───────────────────────────────────────────────
    private static Item itemFor(ConfigField f) {
        // key-specific flavour first, then a sensible per-type default.
        switch (f.key) {
            case "edit_hud":              return Items.GLOWSTONE;
            case "cloud_test":            return Items.ENDER_EYE;
            case "discord_test":          return Items.PAPER;
            case "variant_colours":       return Items.BRUSH;
            case "effects":               return Items.FIREWORK_ROCKET;
            case "ai":                    return Items.AMETHYST_CLUSTER;
            case "discord":               return Items.WRITABLE_BOOK;
            case "arabic":                return Items.BOOK;
            case "advanced":              return Items.COMPARATOR;
            case "max_blocks":            return Items.CHEST;
            case "texture_quality":       return Items.PAINTING;
            case "resource_pack_port":    return Items.BEACON;
            case "server_ip":             return Items.COMPASS;
            case "cloud_url":             return Items.ENDER_PEARL;
            case "cloud_sharing":         return f.asBool() ? Items.ENDER_EYE : Items.ENDER_PEARL;
            case "background_strength":   return Items.GLASS_BOTTLE;
            case "background_removal":    return Items.BLACK_DYE;
            case "history_mode":          return Items.REPEATER;
            case "auto_backup_interval":  return Items.CLOCK;
            case "auto_backup_keep":      return Items.BARREL;
            case "trash_retention_days":  return Items.COMPOSTER;
            case "undo_depth":            return Items.CLOCK;
            case "bulk_confirm_threshold":return Items.HOPPER;
            case "ai_texture_style":      return Items.AMETHYST_SHARD;
            case "discord_webhook":       return Items.PAPER;
            case "variant_red":           return Items.RED_DYE;
            case "variant_yellow":        return Items.YELLOW_DYE;
            case "variant_green":         return Items.GREEN_DYE;
            case "variant_black":         return Items.BLACK_DYE;
            case "arabic_default_bg":     return Items.GRAY_DYE;
            case "arabic_default_letter": return Items.WHITE_DYE;
            default: break;
        }
        switch (f.type) {
            case BOOL:        return f.asBool() ? Items.LIME_DYE : Items.GRAY_DYE;
            case ENUM:        return Items.COMPARATOR;
            case INT:         return Items.PAPER;
            case STRING:      return Items.PAPER;
            case HEX:         return Items.WHITE_DYE;
            case ACTION:      return Items.LEVER;
            case GROUP:       return Items.CHEST;
            case COMING_SOON: return Items.GRAY_DYE;
            default:          return Items.PAPER;
        }
    }

    // ── tiny text utils ───────────────────────────────────────────────────────
    private static void wrap(String text, List<String> out) {
        if (text == null || text.isEmpty()) return;
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > 38) {
                out.add("§7" + line);
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add("§7" + line);
    }

    private static String trim(String v, int max) {
        return v.length() <= max ? v : v.substring(0, max - 3) + "...";
    }
}
