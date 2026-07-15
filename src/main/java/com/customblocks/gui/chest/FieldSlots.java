/**
 * FieldSlots.java
 *
 * Responsibility: Place ONE {@link ConfigField} into a chest slot AND wire its click to the right
 * editor/navigation, so the Settings Book, the sub-chests and any future config screen all edit
 * settings through one consistent path (Group 21 -- "one consistent pattern, no per-setting special
 * casing" in the menus). Rendering comes from {@link FieldIcon}; applying from {@link ConfigApply}.
 *
 * Click routing by type:
 *   BOOL / ENUM -> toggle / cycle in place (ConfigApply.change)
 *   INT         -> open the number stepper
 *   STRING/HEX  -> open the anvil text prompt
 *   ACTION      -> run the reused command (Edit HUD, cloud test, Discord test)
 *   GROUP       -> open the matching sub-chest (Variant colours -> HexColorsMenu, Effects -> Feedback
 *                  FX board, AI/Discord/Arabic/Advanced -> SubChestMenu)
 *   COMING_SOON -> inert
 *
 * Depends on: ConfigField, ConfigApply, FieldIcon, AnvilPrompt, Icons, GuiRouter/Nav.
 * Called by:  SettingsBookMenu, SubChestMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.config.ConfigField;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FieldSlots {

    private FieldSlots() {} // static-only

    /** Render the field and, if it is interactive, wire its click. No-op for a null field. */
    public static void place(ChestMenu m, int slot, ConfigField f) {
        if (f == null) return;
        ChestMenu.Click click = clickFor(f);
        if (click != null) m.set(slot, FieldIcon.of(f), click);
        else m.set(slot, FieldIcon.of(f)); // read-only / coming-soon
    }

    private static ChestMenu.Click clickFor(ConfigField f) {
        switch (f.type) {
            case BOOL:
            case ENUM:   return (p, b, a) -> ConfigApply.change(p, f);
            case INT:    return (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.STEPPER, f.key));
            case STRING:
            case HEX:    return (p, b, a) -> editText(p, f);
            case ACTION: return actionClick(f);
            case GROUP:  return groupClick(f);
            default:     return null; // COMING_SOON
        }
    }

    private static ChestMenu.Click actionClick(ConfigField f) {
        switch (f.key) {
            case "edit_hud":     return (p, b, a) -> { BookSfx.turn(p); GuiRouter.runCommand(p, "edithud"); };
            case "cloud_test":   return (p, b, a) -> GuiRouter.runCommand(p, "vault");
            case "discord_test": return (p, b, a) -> GuiRouter.runCommand(p, "discord test");
            default:             return null;
        }
    }

    private static ChestMenu.Click groupClick(ConfigField f) {
        switch (f.key) {
            case "variant_colours": return (p, b, a) -> { BookSfx.turn(p); GuiRouter.navigate(p, MenuKey.of(Dest.HEX_COLORS)); };
            case "effects":         return (p, b, a) -> { BookSfx.turn(p); GuiRouter.navigate(p, MenuKey.of(Dest.PARTICLES, "")); };
            case "ai":
            case "discord":
            case "arabic":
            case "advanced":        return (p, b, a) -> { BookSfx.turn(p); GuiRouter.navigate(p, MenuKey.of(Dest.SUBCFG, f.key)); };
            default:                return null;
        }
    }

    /** Free-text / hex editing via the shared anvil; commit applies + refreshes the current menu. */
    private static void editText(ServerPlayerEntity player, ConfigField f) {
        ItemStack seed = Icons.of(Items.NAME_TAG, "§f" + f.name);
        AnvilPrompt.open(player, "Set " + f.name, seed, f.get(),
                text -> ConfigApply.apply(player, f, text),
                () -> GuiRouter.refresh(player));
    }
}
