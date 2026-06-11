/**
 * HexRecolorConfirmMenu.java — Yes / Info / No confirm shown right after a colour's hex was
 * changed with /cb config hex, when blocks of that colour already exist (Group 06 / M3 hex).
 *
 * Responsibility: ask whether to repaint the existing "*_<colour>" blocks from the old hex to
 * the new one. The middle Info item explains the direct pixel swap (no flood fill — design
 * pixels are safe) and that players see one brief pack reload. Yes delegates to the tested
 * `/cb recolorvariants <colour> <oldhex>` command (no batch logic duplicated here); No keeps
 * the blocks as they are — only NEW variants and the item art use the new hex.
 *
 * The MenuKey arg carries "colourKey:oldHex" (e.g. "red:#EE3333").
 *
 * Depends on: ChestMenu, Icons, GuiRouter, Nav, ColorVariantService.
 * Called by:  GuiRouter.build (Dest.HEX_RECOLOR_CONFIRM); opened from ConfigCommands.setHex.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ColorVariantService;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HexRecolorConfirmMenu {

    private HexRecolorConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        int sep = arg.indexOf(':');
        final String colour = sep > 0 ? arg.substring(0, sep) : arg;
        final String oldHex = sep > 0 ? arg.substring(sep + 1) : "";
        final String newHex = ColorVariantService.hexFor(colour);
        final int count = ColorVariantService.variantCount(colour);
        final String name = ColorVariantService.capitalize(colour);

        ChestMenu m = new ChestMenu("Recolour existing blocks?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        // The pack rebuild for the new hex is DEFERRED while this prompt is open (the item-art
        // re-tint would otherwise push a pack reload mid-prompt). Yes → the repaint batch ends
        // with the one rebuild; any other way out (No / X / ESC) rebuilds here on close.
        final boolean[] choseYes = {false};
        m.onClose(p -> { if (!choseYes[0]) ResourcePackServer.updatePack(); });

        m.set(13, Icons.of(Items.BOOK, "§e§lWhat this does",
                "§7Repaints your §f" + count + "§7 existing \"_" + colour + "\" block(s)",
                "§7from §f" + oldHex + "§7 to §f" + newHex + "§7.",
                "§7Only pixels close to the old colour change — the",
                "§7design itself is untouched (no flood fill).",
                "§7NEW " + colour + " variants already use " + newHex + " regardless.",
                "§7Players see §fone brief pack reload§7."));

        m.set(11, Icons.glint(Items.LIME_DYE, "§a§lYes — repaint " + count + " block(s)",
                "§7Swap §f" + oldHex + " §7→ §f" + newHex + "§7 on them now.",
                "§8Runs in the background; chat says when it's done."),
                (p, b, a) -> {
                    choseYes[0] = true; // the batch does the single pack rebuild when it ends
                    // Strip the '#' (belt-and-braces with the greedyString oldhex arg). BUG 1.
                    GuiRouter.runCommand(p, "recolorvariants " + colour + " " + oldHex.replace("#", ""));
                });

        m.set(15, Icons.of(Items.RED_DYE, "§c§lNo — keep them as they are",
                "§7Existing " + name + " blocks stay " + oldHex + "§7.",
                "§7New variants + the item art still use §f" + newHex + "§7."),
                (p, b, a) -> p.closeHandledScreen());

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
