/**
 * ColorsMenu.java — Group 10, the Coloring hub (opened by /cb coloring, alias /cb colors).
 *
 * Responsibility: the one front door to every colour tool. Block-targeted tools (Background Studio,
 * Colour Variants, Live Recolour) route through a block picker first; the rest open directly. Pure
 * navigation — no colour logic lives here. Laid out as a framed 4-over-3 tile grid so it reads as a
 * proper hub, since this is the main menu the developer reaches all colouring from.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav.
 * Called by:  GuiRouter.build (Dest.COLORS); opened from /cb coloring and /cb colors.
 */
package com.customblocks.gui.chest;

import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ColorsMenu {

    private ColorsMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Coloring", 6).fill();
        // Framed top + bottom rows for a polished hub feel.
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.BRUSH, "§b§lColoring",
                "§7Everything for colouring your blocks —",
                "§7backgrounds, palettes, gradients, variants.",
                "§8Block tools ask you to pick a block first."));

        // ── Row 2: the four main tools (slots 10/12/14/16) ──────────────────────
        m.set(10, Icons.of(Items.SHEARS, "§e§lBackground Studio",
                        "§7Cut or recolour a block's background.",
                        "§7Pick a mode + strength + fill, preview, apply.",
                        "§aClick §7→ pick a block"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_PICK, "bgstudio")));

        m.set(12, Icons.of(Items.PAINTING, "§e§lPalette",
                        "§7Your saved colours + working swatches.",
                        "§8Used as quick-picks in the fill picker,",
                        "§8gradient endpoints and Custom Colour.",
                        "§aClick §7→ open your palette"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.PALETTE_LIST)));

        m.set(14, Icons.of(Items.PRISMARINE_CRYSTALS, "§e§lGradient Builder",
                        "§7Make a smooth run of colour-step blocks",
                        "§7between two colours (or two blocks).",
                        "§aClick §7→ open the builder"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.GRADIENT_PICKER)));

        m.set(16, Icons.of(Items.MAGENTA_DYE, "§e§lCustom Colour",
                        "§7Ready-made colours + a custom hex,",
                        "§7handed to you as magic Square/Triangle tools.",
                        "§aClick §7→ open the colour studio"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.CUSTOM_COLOR)));

        // ── Row 4: the per-block extras (slots 29/31/33) ────────────────────────
        m.set(29, Icons.of(Items.LIGHT_BLUE_DYE, "§eColour Variants",
                        "§7Spin off lighter/darker/vivid/complement",
                        "§7copies of a block in one click.",
                        "§aClick §7→ pick a block"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_PICK, "variants")));

        m.set(31, Icons.of(Items.FIREWORK_STAR, "§eLive Recolour",
                        "§7Drag Hue / Saturation / Lightness with a",
                        "§7live preview, then apply. §8(client screen)",
                        "§aClick §7→ pick a block"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_PICK, "livecolor")));

        m.set(33, Icons.of(Items.SPYGLASS, "§eScreen Eyedrop",
                        "§7Click any pixel of your screen to grab",
                        "§7its colour into your palette. §8(client screen)",
                        "§aClick §7→ start the eyedrop"),
                (p, b, a) -> GuiRouter.runCommand(p, "eyedrop"));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(49, Icons.of(Items.BOOK, "§7How blocks get colour",
                "§8Make a block from an image, then use these",
                "§8tools to clean its background or shift its colour."));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
