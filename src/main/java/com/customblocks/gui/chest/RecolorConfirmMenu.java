/**
 * RecolorConfirmMenu.java — Yes / Info / No confirm shown when a Triangle is sneak+right-clicked
 * on a custom block (Group 06 / M2).
 *
 * Responsibility: preview exactly what the colour-variant creation will do (new block id, the
 * configured hex the background turns into, that the source block is untouched) before doing it.
 * Yes delegates to ColorVariantService.createVariant (no logic duplicated here); No just closes.
 *
 * The MenuKey arg carries "colourKey:sourceId" (e.g. "black:mars").
 *
 * Depends on: ChestMenu, Icons, GuiRouter, Nav, ColorVariantService, SlotManager, TextureStore.
 * Called by:  GuiRouter.build (Dest.RECOLOR_CONFIRM); opened from ShapeToolItem (Triangle).
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ColorVariantService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RecolorConfirmMenu {

    private RecolorConfirmMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        String[] parts = (arg == null ? "" : arg).split(":", 2);
        final String colour = parts.length == 2 ? parts[0] : "black";
        final String sourceId = parts.length == 2 ? parts[1] : "";
        final SlotData src = SlotManager.getById(sourceId);
        final String hex = ColorVariantService.hexFor(colour);
        final String colourName = ColorVariantService.capitalize(colour);

        ChestMenu m = new ChestMenu("Create " + colourName + " variant?", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 18; i < 27; i++) m.set(i, Icons.accent());

        if (src == null) { // block vanished between click and open
            m.set(13, Icons.of(Items.BARRIER, "§c§lBlock not found",
                    "§7\"§f" + sourceId + "§7\" doesn't exist any more."));
            m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        final String vid = ColorVariantService.variantId(sourceId, colour);
        final boolean exists = SlotManager.getById(vid) != null;
        final boolean hasSource = TextureStore.hasSource(src.index());

        m.set(13, Icons.of(Items.BOOK, "§e§lWhat this does",
                "§7Makes a new block §f" + vid + "§7 from §f" + sourceId + "§7.",
                "§7Its image background is recoloured to §f" + hex + "§7;",
                "§7the design itself stays. §f" + sourceId + "§7 is untouched.",
                "§7Light, hardness and sound are copied over.",
                hasSource ? "§8Rebuilt from the block's original image."
                          : "§8Recoloured from the block's current texture.",
                exists ? "§e\"" + vid + "\" already exists — you'll just get one."
                       : "§8Players see one brief pack reload."));

        m.set(11, Icons.glint(Items.LIME_DYE, exists
                        ? "§a§lYes — give me \"" + vid + "\""
                        : "§a§lYes — create \"" + vid + "\"",
                "§7Background becomes §f" + hex + "§7. Runs in the background;",
                "§8chat says when it's done."),
                (p, b, a) -> {
                    MinecraftServer s = p.getServer();
                    if (s == null) return;
                    s.execute(() -> {
                        p.closeHandledScreen();
                        ColorVariantService.createVariant(p, sourceId, colour);
                    });
                });

        m.set(15, Icons.of(Items.RED_DYE, "§c§lNo — do nothing",
                "§7Keep things exactly as they are."),
                (p, b, a) -> p.closeHandledScreen());

        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
