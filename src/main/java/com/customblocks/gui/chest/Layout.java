/**
 * Layout.java
 *
 * Shared bottom-row navigation (back / prev / page indicator / next / close) for the
 * 6-row paginated chest menus (block list, undo, redo, history). Content occupies slots
 * 0..44; the footer occupies slots 45..53.
 */
package com.customblocks.gui.chest;

import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;

public final class Layout {

    private Layout() {} // static-only

    /** Content slots per page on a 6-row menu (slots 0..44). */
    public static final int PER_PAGE = 45;

    /** Render the standard footer row with back, pagination and close controls. */
    public static void pagedFooter(ChestMenu m, int page, int maxPage, Dest dest, String arg, int total) {
        for (int i = 45; i < 54; i++) m.set(i, Icons.filler());
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        if (page > 0) {
            m.set(48, Icons.of(Items.ARROW, "§ePrevious page", "§8Page " + page + " / " + (maxPage + 1)),
                    (p, b, a) -> GuiRouter.repage(p, new MenuKey(dest, arg, page - 1)));
        }
        m.set(49, Icons.of(Items.PAPER, "§7Page §f" + (page + 1) + " §7/ §f" + (maxPage + 1),
                "§8" + total + " item(s)"));
        if (page < maxPage) {
            m.set(50, Icons.of(Items.ARROW, "§eNext page", "§8Page " + (page + 2) + " / " + (maxPage + 1)),
                    (p, b, a) -> GuiRouter.repage(p, new MenuKey(dest, arg, page + 1)));
        }
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
    }
}
