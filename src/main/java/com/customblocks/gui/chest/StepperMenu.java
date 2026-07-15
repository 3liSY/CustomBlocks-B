/**
 * StepperMenu.java
 *
 * Responsibility: A small "number stepper" chest for editing an INT setting without typing in an anvil
 * (Group 21, spec section 5 helpers). Shows the current value with -big / -1 / +1 / +big buttons plus
 * jump-to-min / jump-to-max and reset-to-default. Each press applies live + saves (via ConfigApply)
 * and refreshes this chest in place; Back / Done returns to the Settings Book.
 *
 * Opened with arg = the field key. Back / Done returns via the nav stack to whatever opened the
 * stepper (the book or a sub-chest). Restart-required fields show the warning here.
 *
 * Depends on: ConfigField, ConfigRegistry, ConfigApply, Icons, GuiRouter/Nav.
 * Called by:  GuiRouter.build (Dest.STEPPER); opened from SettingsBookMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.config.ConfigField;
import com.customblocks.config.ConfigRegistry;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class StepperMenu {

    private StepperMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        final String key = arg == null ? "" : arg;
        ConfigField f = ConfigRegistry.byKey(key);

        ChestMenu m = new ChestMenu(titleFor(f), 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        if (f == null || f.type != ConfigField.Type.INT) {
            m.set(13, Icons.of(Items.BARRIER, "§cThis setting can't be stepped"));
            m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        int cur = f.asInt();
        int big = f.step > 1 ? f.step : 10;

        m.set(13, Icons.glint(Items.PAPER, "§a§l" + f.name + " §f= §e" + cur,
                "§7" + f.help,
                "§7Range §f" + f.min + " §7- §f" + f.max,
                f.restart ? "§cNeeds a server restart to take effect" : "§8Applies live",
                f.isDefault() ? "§7Default value" : "§b✦ Changed from default §7(" + f.defaultRaw + ")"));

        m.set(10, stepBtn(Items.RED_CONCRETE, "§c-" + big), (p, b, a) -> step(p, f, -big, key));
        m.set(11, stepBtn(Items.PINK_CONCRETE, "§c-1"), (p, b, a) -> step(p, f, -1, key));
        m.set(15, stepBtn(Items.LIME_CONCRETE, "§a+1"), (p, b, a) -> step(p, f, 1, key));
        m.set(16, stepBtn(Items.GREEN_CONCRETE, "§a+" + big), (p, b, a) -> step(p, f, big, key));

        m.set(9,  Icons.of(Items.LEVER, "§7Jump to min §f" + f.min),
                (p, b, a) -> set(p, f, f.min, key));
        m.set(17, Icons.of(Items.LEVER, "§7Jump to max §f" + f.max),
                (p, b, a) -> set(p, f, f.max, key));

        m.set(4, Icons.of(Items.STRUCTURE_VOID, "§eReset to default §7(" + f.defaultRaw + ")"),
                (p, b, a) -> set(p, f, parseInt(f.defaultRaw, cur), key));

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(22, Icons.of(Items.LIME_CONCRETE, "§a§lDone", "§7Back to the Settings Book"),
                (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static void step(ServerPlayerEntity p, ConfigField f, int delta, String arg) {
        set(p, f, f.asInt() + delta, arg);
    }

    private static void set(ServerPlayerEntity p, ConfigField f, int value, String arg) {
        int v = Math.max(f.min, Math.min(f.max, value));
        ConfigApply.setAndSave(f, String.valueOf(v));
        BookSfx.step(p);
        GuiRouter.repage(p, MenuKey.of(Dest.STEPPER, arg)); // refresh in place (same title + rows)
    }

    private static ItemStack stepBtn(Item item, String label) {
        return Icons.of(item, "§l" + label);
    }

    private static String titleFor(ConfigField f) {
        return "Set: " + (f == null ? "value" : f.name);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
