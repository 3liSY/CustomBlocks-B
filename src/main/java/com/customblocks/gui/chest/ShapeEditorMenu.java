/**
 * ShapeEditorMenu.java — shape picker for one block (Group 08, G08.6). Shows a button per
 * available shape (see {@link com.customblocks.block.BlockShapes}); clicking one DELEGATES to
 * the tested "/cb setshape <id> <shape>" (and the reset button to "/cb clearshape <id>") via
 * GuiRouter.runAndReopen, so locking, undo and the pack rebuild all behave exactly as the
 * commands do. The current shape is marked with an enchant glint. No shape logic lives here.
 *
 * Depends on: ChestMenu/Icons/GuiRouter/Nav, BlockShapes, SlotData/SlotManager.
 * Called by:  GuiRouter (Dest.SHAPE_EDITOR), ChestGuiCommands (/cb shapeeditor <id>).
 */
package com.customblocks.gui.chest;

import com.customblocks.block.BlockShapes;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShapeEditorMenu {

    private ShapeEditorMenu() {} // static-only

    /** Shape name → the vanilla item whose look best stands in for that shape on its button. */
    private static final Map<String, Item> ICON = new LinkedHashMap<>();
    static {
        ICON.put("full",        Items.STONE);
        ICON.put("slab_bottom", Items.SMOOTH_STONE_SLAB);
        ICON.put("slab_top",    Items.SMOOTH_STONE_SLAB);
        ICON.put("carpet",      Items.WHITE_CARPET);
        ICON.put("thin",        Items.GLASS_PANE);
        ICON.put("pane",        Items.GLASS_PANE);
        ICON.put("wall",        Items.COBBLESTONE_WALL);
        ICON.put("pillar",      Items.END_ROD);
        ICON.put("stairs",      Items.STONE_STAIRS);
        ICON.put("cross",       Items.POPPY);
    }

    /** The shape buttons sit in two centred rows of five. */
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        SlotData d = SlotManager.getById(id);
        ChestMenu m = new ChestMenu("CustomBlocks Shapes - ID: " + id, 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 46; i < 53; i++) m.set(i, Icons.accent());

        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block '" + id + "'"));
            m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        String current = d.shape();
        Item preview = SlotManager.itemAt(d.index());
        m.set(4, Icons.of(preview != null ? preview : Items.STONE, "§f§l" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7current shape: §e" + current,
                "§8" + BlockShapes.description(current)));

        String[] names = BlockShapes.names();
        for (int i = 0; i < names.length && i < SLOTS.length; i++) {
            String shape = names[i];
            boolean selected = shape.equals(current);
            Item icon = ICON.getOrDefault(shape, Items.STONE);
            String title = (selected ? "§a§l" : "§e") + shape + (selected ? " §7(current)" : "");
            var stack = selected
                    ? Icons.glint(icon, title, "§7" + BlockShapes.description(shape), "§8Already applied")
                    : Icons.of(icon, title, "§7" + BlockShapes.description(shape), "§7Click to apply");
            m.set(SLOTS[i], stack, (p, b, a) -> {
                if (shape.equals(BlockShapes.DEFAULT)) {
                    GuiRouter.runAndReopen(p, "clearshape " + id, MenuKey.of(Dest.SHAPE_EDITOR, id));
                } else {
                    GuiRouter.runAndReopen(p, "setshape " + id + " " + shape, MenuKey.of(Dest.SHAPE_EDITOR, id));
                }
            });
        }

        m.set(40, Icons.of(Items.BARRIER, "§cReset to full block",
                        "§7Same as /cb clearshape " + id),
                (p, b, a) -> GuiRouter.runAndReopen(p, "clearshape " + id, MenuKey.of(Dest.SHAPE_EDITOR, id)));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
