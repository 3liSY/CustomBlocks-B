/**
 * RetextureConfirmMenu.java — Yes / Info / No confirm shown after a new texture size is picked
 * in TextureSizeMenu, when at least one block already exists.
 *
 * Responsibility: ask whether to re-render existing blocks at the just-chosen size. The middle
 * Info item explains exactly what will happen (count, est. new pack size, source-vs-upscale
 * behaviour, one pack reload). Yes delegates to the tested `/cb retextureall <px>` command (no
 * batch logic duplicated here); No leaves existing blocks untouched. Both end on the Config menu.
 *
 * The target size travels as the MenuKey arg (a px string). Per-texture/pack figures reuse
 * TextureSizeMenu.avgBytes / human so numbers stay consistent with the picker.
 *
 * Depends on: ChestMenu, Icons, GuiRouter, Nav, CustomBlocksConfig, SlotManager, TextureSizeMenu.
 * Called by: GuiRouter.build (Dest.RETEXTURE_CONFIRM).
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RetextureConfirmMenu {

    private RetextureConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        int parsed;
        try { parsed = Integer.parseInt(arg); } catch (Exception e) { parsed = CustomBlocksConfig.textureSize; }
        final int size = parsed;
        final int count = SlotManager.usedSlots();
        final long per = TextureSizeMenu.avgBytes(size);
        final String estPack = TextureSizeMenu.human(per * Math.max(0, count));

        ChestMenu m = new ChestMenu("Retexture existing blocks?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        m.set(13, Icons.of(Items.BOOK, "§e§lWhat this does",
                "§7Re-renders your §f" + count + "§7 existing block(s) at §a" + size + "px§7.",
                "§7Each block is rebuilt from its §foriginal image§7 when one",
                "§7was saved. Blocks with no source (older blocks, Arabic,",
                "§7video) are §fupscaled§7 from their current texture instead.",
                "§7Estimated new pack: §f~" + estPack + " §8(" + count + " × ~" + TextureSizeMenu.human(per) + ")",
                "§7NEW blocks already use " + size + "px regardless of this choice.",
                "§7Players see §fone brief pack reload§7."));

        m.set(11, Icons.glint(Items.LIME_DYE, "§a§lYes — retexture " + count + " block(s)",
                "§7Rebuild them all at §a" + size + "px§7 now.",
                "§8Runs in the background; chat says when it's done."),
                (p, b, a) -> GuiRouter.runAndReopen(p, "retextureall " + size, MenuKey.of(Dest.CONFIG)));

        m.set(15, Icons.of(Items.RED_DYE, "§c§lNo — keep existing blocks",
                "§7Leave already-made blocks as they are.",
                "§7The §a" + size + "px§7 size still applies to NEW blocks."),
                (p, b, a) -> GuiRouter.openFresh(p, MenuKey.of(Dest.CONFIG)));

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
