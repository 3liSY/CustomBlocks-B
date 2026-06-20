/**
 * ColorPickBlockMenu.java — Group 10 (Coloring), shared "pick a block" step.
 *
 * Responsibility: a lightweight paginated grid of every assigned block used when a colour tool
 * needs a target but the player didn't name one — e.g. "/cb bgstudio" with no id, or the Coloring
 * hub's Background Studio / Colour Variants / Live Recolour tiles. Clicking a block hands off to
 * that tool for the chosen id. The action it routes to rides on the MenuKey arg so pagination keeps
 * it. No block logic here — it only navigates.
 *
 * Actions (MenuKey.arg): "bgstudio" → Background Studio · "variants" → Colour Variants panel ·
 *                        "livecolor" → live recolour slider · "shapeeditor" → 3D Shape Editor
 *                        (both client screens, opened via command).
 *
 * Depends on: ChestMenu, Icons, Layout, GuiRouter/Nav, SlotManager/SlotData/SlotBlock.
 * Called by:  GuiRouter.build (Dest.COLOR_PICK); opened from ImageToolCommands + ColorsMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ColorPickBlockMenu {

    private ColorPickBlockMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player, String action, int page) {
        String act = (action == null || action.isEmpty()) ? "bgstudio" : action;

        List<SlotData> all = new ArrayList<>(SlotManager.assignedSlots());
        all.sort(Comparator.comparingInt(SlotData::index));

        int per = Layout.PER_PAGE;
        int maxPage = all.isEmpty() ? 0 : (all.size() - 1) / per;
        int p = Math.max(0, Math.min(page, maxPage));

        ChestMenu m = new ChestMenu("Pick a block — " + title(act), 6);
        if (all.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No blocks yet",
                    "§8Create one with /cb create <id> <image-url>"));
        }

        int start = p * per;
        for (int i = 0; i < per; i++) {
            int gi = start + i;
            if (gi >= all.size()) break;
            SlotData d = all.get(gi);
            SlotBlock.SlotItem item = SlotManager.itemAt(d.index());
            Item display = item != null ? item : Items.STONE;
            m.set(i, Icons.of(display, "§f" + d.displayName(),
                            "§7id: §f" + d.customId(),
                            "§7category: §f" + (d.category().isEmpty() ? "none" : d.category()),
                            "§aClick §7→ " + verb(act)),
                    (pl, b, a) -> route(pl, act, d.customId()));
        }

        Layout.pagedFooter(m, p, maxPage, Dest.COLOR_PICK, act, all.size());
        return m;
    }

    /** Open the chosen tool for {@code id}. */
    private static void route(ServerPlayerEntity p, String action, String id) {
        if (action.startsWith("caticon:")) {
            String cat = action.substring(8);
            CategoryMetadataStore.setDisplayBlock(cat, id);
            GuiFx.apply(p);
            GuiRouter.back(p); // back to CategoryEditMenu
            return;
        }
        switch (action) {
            case "variants"    -> GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_VARIANTS, id));
            case "livecolor"   -> GuiRouter.runCommand(p, "livecolor " + id);
            case "shapeeditor" -> GuiRouter.runCommand(p, "shapeeditor " + id); // G27 §F2 → 3D Shape Editor
            default            -> GuiRouter.navigate(p, MenuKey.of(Dest.BGSTUDIO, id));
        }
    }

    private static String title(String action) {
        if (action.startsWith("caticon:")) return "Pick icon for " + action.substring(8);
        return switch (action) {
            case "variants"    -> "Colour Variants";
            case "livecolor"   -> "Live Recolour";
            case "shapeeditor" -> "Shape Editor";
            default            -> "Background Studio";
        };
    }

    private static String verb(String action) {
        if (action.startsWith("caticon:")) return "set as category icon";
        return switch (action) {
            case "variants"    -> "open its colour variants";
            case "livecolor"   -> "open the live recolour slider";
            case "shapeeditor" -> "open the 3D shape editor";
            default            -> "open the Background Studio";
        };
    }
}
