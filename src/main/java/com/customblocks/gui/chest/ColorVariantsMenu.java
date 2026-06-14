/**
 * ColorVariantsMenu.java — Group 10 (Colour Variants panel).
 *
 * Responsibility: show a block plus a row of algorithmic colour swatches (lighter, darker, vivid,
 * muted, complementary, two split-complements). Each swatch's lore previews the resulting average
 * colour; clicking one bakes that HSL-shifted texture into a NEW block via ColorToolService
 * (one CREATE undo per click) and stays open so several can be made in a row.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, ColorToolService, ColorMath, TextureStore, SlotManager.
 * Called by:  GuiRouter.build (Dest.COLOR_VARIANTS); opened from EditorMenu's "Colour Variants" tile.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ColorToolService;
import com.customblocks.core.ColorToolService.Variant;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.image.ColorMath;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class ColorVariantsMenu {

    private ColorVariantsMenu() {} // static-only

    /** Item used for each swatch tile (the real preview is the block you create). */
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16};

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        ChestMenu m = new ChestMenu("Colour Variants - " + id, 5).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 36; i < 45; i++) m.set(i, Icons.accent());

        SlotData d = SlotManager.getById(id);
        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block '" + id + "'"));
            m.set(36, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(44, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        int avg = averageColor(d.index());
        Item icon = SlotManager.itemAt(d.index());
        m.set(4, Icons.of(icon != null ? icon : Items.PAINTING, "§f§l" + d.displayName(),
                "§7id: §f" + id,
                "§7average colour: §f" + (avg >= 0 ? hex(avg) : "§8unknown"),
                "§8Click a swatch to spin off a new block."));

        var list = ColorToolService.VARIANTS;
        for (int i = 0; i < list.size() && i < SLOTS.length; i++) {
            final Variant v = list.get(i);
            String resultHex = avg >= 0 ? hex(ColorMath.hslShiftRgb(avg, v.hueDeg(), v.satFactor(), v.lightFactor())) : "§8?";
            m.set(SLOTS[i], Icons.of(swatchItem(i), "§e" + v.label(),
                            "§7→ roughly §f" + resultHex,
                            "§7Creates §f" + id + "_" + v.key(),
                            "§aClick §7→ make this variant"),
                    (p, b, a) -> {
                        ColorToolService.createVariant(p, id, v.key());
                        GuiRouter.render(p, MenuKey.of(Dest.COLOR_VARIANTS, id));
                    });
        }

        m.set(36, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(44, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** A spread of differently coloured wools so the swatch row reads as a palette at a glance. */
    private static Item swatchItem(int i) {
        Item[] wools = {Items.WHITE_WOOL, Items.GRAY_WOOL, Items.RED_WOOL, Items.LIGHT_BLUE_WOOL,
                Items.PURPLE_WOOL, Items.ORANGE_WOOL, Items.LIME_WOOL};
        return wools[i % wools.length];
    }

    private static int averageColor(int index) {
        try {
            byte[] tex = TextureStore.load(index);
            if (tex == null || tex.length == 0) return -1;
            return ColorMath.averageColor(tex);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }
}
