/**
 * SubChestMenu.java
 *
 * Responsibility: A generic sub-chest for a Settings-Book category that opens its own small chest
 * rather than folding inline -- AI, Discord, Arabic and Advanced (Group 21, spec section 3). It simply
 * renders every {@link ConfigField} in the category through {@link FieldSlots} (skipping the opener
 * tile itself), so all editing reuses the same path as the main book. Variant colours and Effects use
 * their own existing menus (HexColorsMenu / Feedback FX) instead of this one.
 *
 * Opened with arg = the category id. Back returns to the book via the nav stack; edits refresh this
 * chest in place (constant title per category).
 *
 * Depends on: ConfigRegistry + ConfigField, FieldSlots, Icons, GuiRouter.
 * Called by:  GuiRouter.build (Dest.SUBCFG); opened from FieldSlots group clicks.
 */
package com.customblocks.gui.chest;

import com.customblocks.config.ConfigField;
import com.customblocks.config.ConfigRegistry;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SubChestMenu {

    private SubChestMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String cat) {
        ConfigField head = ConfigRegistry.byKey(cat);
        String title = head != null ? head.name : cat;

        ChestMenu m = new ChestMenu(title, 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());
        m.set(4, Icons.of(Items.ENCHANTED_BOOK, "§6§l" + title,
                head != null ? "§7" + head.help : "§7Category settings"));

        int slot = 19; // start mid-chest for a tidy centred row
        for (ConfigField f : ConfigRegistry.inCategory(cat)) {
            if (cat.equals(f.key)) continue; // skip the opener tile itself
            if (slot > 43) break;
            FieldSlots.place(m, slot++, f);
        }

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
