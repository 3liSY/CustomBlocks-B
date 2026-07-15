/**
 * ConfigApply.java
 *
 * Responsibility: The single apply/save path for Settings-Book edits (Group 21, spec section 6).
 * Toggling a boolean, cycling an enum, or committing a typed/stepped value all go through here so the
 * book never duplicates save or live-sync logic.
 *
 * Where a tested `/cb …` command already exists for a change (because it also broadcasts a live update
 * to clients -- silent pack, transparent background, undo-mode history clear, auto-backup timer, etc.),
 * we route through that command via {@link GuiRouter#runAndReopen} rather than re-implementing the
 * side effect. Everything else is a plain field set + atomic {@code CustomBlocksConfig.save()} (those
 * values are read at use-time, no broadcast needed).
 *
 * Depends on: ConfigField, CustomBlocksConfig, BackgroundRemover, GuiRouter/Nav.
 * Called by:  SettingsBookMenu (toggles/cycles/anvil) and StepperMenu (numbers).
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.config.ConfigField;
import com.customblocks.image.BackgroundRemover;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ConfigApply {

    private ConfigApply() {} // static-only

    private static final String[] TEX_SIZES = {"16", "32", "64", "128", "256", "512"};

    /** Toggle a BOOL or cycle an ENUM, then save + refresh the menu the player is in. */
    public static void change(ServerPlayerEntity p, ConfigField f) {
        if (f.type == ConfigField.Type.BOOL) BookSfx.toggle(p, !f.asBool()); else BookSfx.step(p);
        String cmd = commandFor(f);
        if (cmd != null) { GuiRouter.runAndReopenCurrent(p, cmd); return; }
        String next = (f.type == ConfigField.Type.BOOL) ? String.valueOf(!f.asBool()) : nextEnum(f);
        f.set(next);
        CustomBlocksConfig.save();
        GuiRouter.refresh(p);
    }

    /** Commit a typed/stepped raw value (INT/STRING/HEX), save, then refresh the current menu. */
    public static void apply(ServerPlayerEntity p, ConfigField f, String raw) {
        f.set(raw);
        CustomBlocksConfig.save();
        BookSfx.save(p);
        GuiRouter.refresh(p);
    }

    /** Set + save only (used by the stepper, which then refreshes itself). */
    public static void setAndSave(ConfigField f, String raw) {
        f.set(raw);
        CustomBlocksConfig.save();
    }

    private static String nextEnum(ConfigField f) {
        String[] vals = f.enumValues;
        if (vals == null || vals.length == 0) return f.get();
        String cur = f.get();
        for (int i = 0; i < vals.length; i++) if (vals[i].equals(cur)) return vals[(i + 1) % vals.length];
        return vals[0];
    }

    /** The tested /cb subcommand that toggles/cycles this field (so its live broadcast runs), else null. */
    private static String commandFor(ConfigField f) {
        switch (f.key) {
            case "silent_pack":            return "config silentpack toggle";
            case "transparent_background": return "config transparent toggle";
            case "named_texture_mirror":   return "config mirrornames " + (f.asBool() ? "off" : "on");
            case "typo_correction":        return "config didyoumean cycle";
            case "history_mode":           return "config undomode";
            case "auto_backup_interval":   return "config autobackup interval";
            case "background_removal":     return "config background "
                    + BackgroundRemover.commandArg(BackgroundRemover.next(CustomBlocksConfig.backgroundMode));
            case "texture_quality":        return "config texturesize " + nextTexSize();
            default:                        return null;
        }
    }

    private static String nextTexSize() {
        String cur = String.valueOf(CustomBlocksConfig.textureSize);
        for (int i = 0; i < TEX_SIZES.length; i++) if (TEX_SIZES[i].equals(cur)) return TEX_SIZES[(i + 1) % TEX_SIZES.length];
        return "256";
    }
}
