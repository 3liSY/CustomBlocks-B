/**
 * PaletteMenu.java — Group 10 (per-player colour palettes).
 *
 * Responsibility: show the player's working colour set (left-click a swatch to drop it) and their
 * saved palettes (left-click to load, right-click to delete). Actions delegate to the tested
 * /cb palette commands so persistence + chat feedback behave identically. Colours are shown as the
 * nearest wool for a visual cue, with the exact hex in the name.
 *
 * Layout (6 rows):
 *   Row 1 (0-8):   accent frame + header tile.
 *   Row 2 (9-17):  ＋ Add colour (anvil) · Save as… (anvil) · Clear + spacers.
 *   Row 3 (18-26): working-set swatches (up to 9).
 *   Row 4-5 (27-44): saved palettes (paged, 18 per page).
 *   Row 6 (45-53): accent frame + back/page/close.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, AnvilPrompt, PlayerPaletteManager, ColorLibrary.
 * Called by:  GuiRouter.build (Dest.PALETTE_LIST); opened from /cb palette and the Colours hub.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ColorLibrary;
import com.customblocks.core.PlayerPaletteManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class PaletteMenu {

    private PaletteMenu() {} // static-only

    private static final int SAVED_PER_PAGE = 18; // slots 27..44

    public static ChestMenu build(ServerPlayerEntity player, int page) {
        ChestMenu m = new ChestMenu("Colour Palette", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        List<String> work = PlayerPaletteManager.working(player.getUuid());

        // Row 1 — header.
        m.set(4, Icons.of(Items.PAINTING, "§6§lColour Palette",
                "§7Working set: §f" + work.size() + "/" + PlayerPaletteManager.WORKING_MAX,
                "§8Your reusable colour kit. These swatches show up",
                "§8as quick-picks in the Background Studio fill picker,",
                "§8Gradient endpoints and Custom Colour."));

        // Row 2 — action buttons.
        m.set(10, Icons.of(Items.LIME_DYE, "§a＋ Add colour",
                        "§7Opens an anvil — type a name or hex.",
                        "§8e.g. red, #FF5500"),
                (p, b, a) -> AnvilPrompt.open(p, "Add colour (name or #hex)", new ItemStack(Items.LIME_DYE), "",
                        text -> {
                            String hex = ColorLibrary.resolve(text.trim());
                            if (hex != null) {
                                GuiRouter.runAndReopen(p, "palette add " + text.trim(), MenuKey.of(Dest.PALETTE_LIST).withPage(page));
                            } else {
                                GuiRouter.render(p, MenuKey.of(Dest.PALETTE_LIST).withPage(page));
                            }
                        },
                        () -> GuiRouter.render(p, MenuKey.of(Dest.PALETTE_LIST).withPage(page))));

        m.set(12, Icons.of(Items.WRITABLE_BOOK, "§eSave current as…",
                        "§7Snapshot the working set under a name"),
                (p, b, a) -> AnvilPrompt.open(p, "Palette name", new ItemStack(Items.NAME_TAG), "",
                        text -> GuiRouter.runAndReopen(p, "palette save " + text.trim(), MenuKey.of(Dest.PALETTE_LIST)),
                        () -> GuiRouter.render(p, MenuKey.of(Dest.PALETTE_LIST))));

        m.set(14, Icons.of(Items.BARRIER, "§cClear working set", "§7Remove every working colour"),
                (p, b, a) -> GuiRouter.runAndReopen(p, "palette clear", MenuKey.of(Dest.PALETTE_LIST)));

        // Row 3 — working-set swatches (slots 18..26).
        for (int i = 0; i < work.size() && i < 9; i++) {
            final String hex = work.get(i);
            m.set(18 + i, Icons.of(Swatch.woolFor(hex), "§f" + hex,
                            "§7In your working set.",
                            "§cLeft-click §7→ remove"),
                    (p, b, a) -> {
                        PlayerPaletteManager.workingRemove(p.getUuid(), hex);
                        GuiRouter.render(p, MenuKey.of(Dest.PALETTE_LIST).withPage(page));
                    });
        }
        if (work.isEmpty()) {
            m.set(22, Icons.of(Items.GRAY_DYE, "§7Working set empty",
                    "§8Add colours with §a＋ Add colour§8 above,",
                    "§8the eyedrop, or /cb palette add <colour>."));
        }

        // Rows 4-5 — saved palettes (slots 27..44, paged).
        List<String> names = PlayerPaletteManager.names(player.getUuid());
        int from = Math.max(0, page) * SAVED_PER_PAGE;
        for (int i = 0; i < SAVED_PER_PAGE && from + i < names.size(); i++) {
            final String name = names.get(from + i);
            List<String> cols = PlayerPaletteManager.get(player.getUuid(), name);
            int n = cols == null ? 0 : cols.size();
            m.set(27 + i, Icons.of(Items.WRITTEN_BOOK, "§e" + name,
                            "§7" + n + " colour(s)",
                            "§aLeft-click §7→ load into working set",
                            "§cRight-click §7→ delete"),
                    (p, b, a) -> {
                        if (b == 1) GuiRouter.runAndReopen(p, "palette delete " + name, MenuKey.of(Dest.PALETTE_LIST).withPage(page));
                        else GuiRouter.runAndReopen(p, "palette load " + name, MenuKey.of(Dest.PALETTE_LIST).withPage(page));
                    });
        }

        // Paging for saved palettes.
        if (page > 0) m.set(45, Icons.of(Items.SPECTRAL_ARROW, "§ePrevious page"),
                (p, b, a) -> GuiRouter.repage(p, MenuKey.of(Dest.PALETTE_LIST).withPage(page - 1)));
        else m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        if (from + SAVED_PER_PAGE < names.size())
            m.set(52, Icons.of(Items.SPECTRAL_ARROW, "§eNext page"),
                    (p, b, a) -> GuiRouter.repage(p, MenuKey.of(Dest.PALETTE_LIST).withPage(page + 1)));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
