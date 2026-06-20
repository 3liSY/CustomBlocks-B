/**
 * ArabicGroupMenu.java — one Arabic group as an aligned grid + colour rail (Group 13 Area 2a revamp).
 *
 * Shows a single group (Arabic Letters / Arabic Numbers / English Numbers), one glyph per slot in a
 * tidy 8-wide grid (cols 1-8), with a 4-colour rail down the left column (black/red/green/yellow).
 * Clicking a rail colour re-renders the SAME screen showing that variant (cursor stays put). Clicking
 * a glyph opens the existing CategoryBlockMenu (give / edit / remove) — no block logic is duplicated.
 * "Give every block here" reuses /cb category give.
 *
 * arg = "<category>|<color>" (e.g. "Arabic Letters|red"). Opened from ArabicListMenu.
 * Depends on: ArabicArt, SlotManager, SlotBlock, Icons, GuiFx, GuiRouter/Nav.
 * Called by:  GuiRouter (Dest.ARABIC_GROUP).
 */
package com.customblocks.gui.chest;

import com.customblocks.arabic.ArabicArt;
import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Locale;

public final class ArabicGroupMenu {

    private ArabicGroupMenu() {} // static-only

    /** Left-column rail slots (top four rows) and the 8-wide glyph grid (cols 1-8, rows 0-4). */
    private static final int[] RAIL_SLOTS = {0, 9, 18, 27};

    public static ChestMenu build(ServerPlayerEntity player, String arg) {
        String cat = "Arabic Letters";
        String color = "black";
        if (arg != null && !arg.isEmpty()) {
            int bar = arg.indexOf('|');
            if (bar >= 0) { cat = arg.substring(0, bar); color = arg.substring(bar + 1); }
            else cat = arg;
        }
        if (!ArabicArt.isColor(color)) color = "black";
        final String fcat = cat, fcolor = color;

        ChestMenu m = new ChestMenu(fcat, 6).fill();

        // ── Colour rail (left column) ──
        for (int i = 0; i < ArabicArt.COLORS.length && i < RAIL_SLOTS.length; i++) {
            String col = ArabicArt.COLORS[i];
            m.set(RAIL_SLOTS[i], colourTile(col, col.equals(fcolor)),
                    (p, b, a) -> { GuiFx.click(p); GuiRouter.render(p, MenuKey.of(Dest.ARABIC_GROUP, fcat + "|" + col)); });
        }
        m.set(36, Icons.of(Items.ITEM_FRAME, "§e§lColour rail",
                "§7Showing the §f" + cap(fcolor) + "§7 variant.",
                "§8Click a colour on the left to switch."));

        // ── Glyph grid (cols 1-8, rows 0-4 = 40 slots; every group fits on one page) ──
        List<ArabicArt.Glyph> glyphs = ArabicArt.ALL.stream()
                .filter(g -> ArabicArt.category(g).equals(fcat)).toList();
        int[] grid = gridSlots();
        for (int i = 0; i < glyphs.size() && i < grid.length; i++) {
            ArabicArt.Glyph g = glyphs.get(i);
            String id = ArabicArt.blockId(g, fcolor);
            SlotData d = SlotManager.getById(id);
            if (d == null) {
                m.set(grid[i], Icons.of(Items.BARRIER, "§8" + ArabicArt.displayName(g, fcolor),
                                "§7Not imported yet.", "§aClick §7→ import the bundled set"),
                        (p, b, a) -> { GuiFx.click(p); GuiRouter.runCommand(p, "arabic import"); });
            } else {
                SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
                Item disp = item != null ? item : Items.STONE;
                final String fid = d.customId();
                m.set(grid[i], Icons.of(disp, "§f" + d.displayName(),
                                "§7id: §f" + d.customId(),
                                "§7Click to manage §8(give / edit / remove)"),
                        (p, b, a) -> { GuiFx.click(p); GuiRouter.navigate(p, MenuKey.of(Dest.CATEGORY_BLOCK, fid + " " + fcat)); });
            }
        }

        // ── Footer ──
        for (int i = 45; i < 54; i++) m.set(i, Icons.filler());
        m.set(45, Icons.back(), (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(49, Icons.glint(Items.HOPPER, "§a§lGive every block here",
                        "§7One of every block in §f" + fcat + "§7,",
                        "§7all four colours.",
                        "§aClick §7→ give all"),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.runCommand(p, "category give " + fcat); });
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** The 8-wide glyph grid: cols 1-8, rows 0-4 (40 slots). */
    private static int[] gridSlots() {
        int[] s = new int[40];
        int k = 0;
        for (int r = 0; r < 5; r++)
            for (int c = 1; c <= 8; c++)
                s[k++] = r * 9 + c;
        return s;
    }

    /** A coloured-concrete rail selector; the showing colour glows. */
    private static ItemStack colourTile(String color, boolean selected) {
        Item item = switch (color) {
            case "red"    -> Items.RED_CONCRETE;
            case "green"  -> Items.GREEN_CONCRETE;
            case "yellow" -> Items.YELLOW_CONCRETE;
            default       -> Items.BLACK_CONCRETE;
        };
        return selected
                ? Icons.glint(item, "§a§l" + cap(color) + " §8(showing)", "§7This variant is in view.")
                : Icons.of(item, "§f" + cap(color), "§aClick §7→ show this colour");
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
