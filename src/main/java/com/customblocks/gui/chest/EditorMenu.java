/**
 * EditorMenu.java — single-block editor (Group 02, G02.3). Shows the block preview plus its
 * current attributes, and exposes give / glow / hardness / sound / collision / category /
 * rename / retexture / note / delete actions. Every mutating action DELEGATES to the
 * existing, tested /cb commands (via GuiRouter.runAndReopen / promptCommand / confirmCommand)
 * so locking, undo recording, lighting and chat feedback all behave exactly as the commands do.
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class EditorMenu {

    private EditorMenu() {} // static-only

    private static final float[] HARDNESS = {-1f, 0f, 0.5f, 1f, 1.5f, 2f, 4f, 6f, 10f};

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        SlotData d = SlotManager.getById(id);
        ChestMenu m = new ChestMenu("CustomBlocks Editor - ID: " + id, 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 46; i < 53; i++) m.set(i, Icons.accent());

        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block '" + id + "'"));
            m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        Item icon = SlotManager.itemAt(d.index());
        m.set(4, Icons.of(icon != null ? icon : Items.STONE, "§f§l" + d.displayName(),
                "§7id: §f" + d.customId(),
                "§7slot: §f" + d.index(),
                "§7glow: §f" + d.glow(),
                "§7hardness: §f" + hardnessLabel(d.hardness()),
                "§7sound: §f" + d.soundType(),
                "§7collision: §f" + (d.noCollision() ? "passable" : "solid"),
                "§7category: §f" + (d.category().isEmpty() ? "none" : d.category())));

        m.set(19, Icons.of(Items.HOPPER, "§aGive block", "§7Put this block in your inventory"),
                (p, b, a) -> GuiRouter.runAndReopen(p, "give " + id, MenuKey.of(Dest.EDITOR, id)));

        m.set(20, Icons.of(Items.GLOWSTONE_DUST, "§eGlow: §f" + d.glow(),
                        "§7Left-click §8+1", "§7Right-click §8-1"),
                (p, b, a) -> {
                    int next = Math.max(0, Math.min(15, d.glow() + (b == 1 ? -1 : 1)));
                    GuiRouter.runAndReopen(p, "setglow " + id + " " + next, MenuKey.of(Dest.EDITOR, id));
                });

        m.set(21, Icons.of(Items.IRON_PICKAXE, "§eHardness: §f" + hardnessLabel(d.hardness()),
                        "§7Left-click §8next", "§7Right-click §8previous"),
                (p, b, a) -> {
                    float next = cycleHardness(d.hardness(), b == 1 ? -1 : 1);
                    GuiRouter.runAndReopen(p, "sethardness " + id + " " + hardnessArg(next), MenuKey.of(Dest.EDITOR, id));
                });

        m.set(22, Icons.of(Items.NOTE_BLOCK, "§eSound: §f" + d.soundType(),
                        "§7Left-click §8next", "§7Right-click §8previous"),
                (p, b, a) -> {
                    String next = cycleSound(d.soundType(), b == 1 ? -1 : 1);
                    GuiRouter.runAndReopen(p, "setsound " + id + " " + next, MenuKey.of(Dest.EDITOR, id));
                });

        m.set(23, Icons.of(Items.SLIME_BLOCK, "§eCollision: §f" + (d.noCollision() ? "passable" : "solid"),
                        "§7Click to toggle"),
                (p, b, a) -> GuiRouter.runAndReopen(p,
                        "setcollision " + id + " " + (d.noCollision() ? "solid" : "passable"),
                        MenuKey.of(Dest.EDITOR, id)));

        m.set(24, Icons.of(Items.BOOKSHELF, "§eCategory: §f" + (d.category().isEmpty() ? "none" : d.category()),
                        "§7Left-click §8next", "§7Right-click §8previous"),
                (p, b, a) -> {
                    String next = cycleCategory(d.category(), b == 1 ? -1 : 1);
                    GuiRouter.runAndReopen(p, "setcategory " + id + " " + next, MenuKey.of(Dest.EDITOR, id));
                });

        m.set(25, Icons.of(Items.STONECUTTER, "§eShape", "§7Slab, stairs, carpet, cross…",
                        "§7Click to open the shape picker"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.SHAPE_EDITOR, id)));

        m.set(26, Icons.of(Items.ITEM_FRAME, "§eFaces", "§7A different texture per side",
                        "§7Click to open the face editor"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.FACE_EDITOR, id)));

        m.set(28, Icons.of(Items.ANVIL, "§eChange ID", "§7Give this block a new id",
                        "§7Texture & placed blocks stay — only the id changes"),
                (p, b, a) -> ReIdMenu.openAnvil(p, id, () -> GuiRouter.render(p, MenuKey.of(Dest.EDITOR, id))));

        m.set(29, Icons.of(Items.NAME_TAG, "§eRename", "§7Set a new display name"),
                (p, b, a) -> GuiRouter.promptCommand(p, "/cb rename " + id + " ", "rename " + id));

        m.set(30, Icons.of(Items.PAINTING, "§eRetexture", "§7Set a texture from a URL"),
                (p, b, a) -> GuiRouter.promptCommand(p, "/cb retexture " + id + " ", "retexture " + id));

        m.set(31, Icons.of(Items.WRITABLE_BOOK, "§eNote", "§7Attach a note to this block"),
                (p, b, a) -> GuiRouter.promptCommand(p, "/cb note " + id + " ", "note " + id));

        m.set(32, Icons.of(Items.LIME_DYE, "§eColour Variants", "§7Lighter / darker / complementary…",
                        "§7Spin off new colour-shifted blocks"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_VARIANTS, id)));

        m.set(33, Icons.of(Items.TNT, "§c§lDelete", "§7Remove this block", "§cClick, then confirm in chat"),
                (p, b, a) -> GuiRouter.confirmCommand(p, "/cb delete " + id, "§c[Confirm DELETE " + id + "]"));

        m.set(34, Icons.of(Items.SHEARS, "§eBackground Studio", "§7Remove / change this block's background",
                        "§7corners · enclosed · smart (offline)"),
                (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.BGSTUDIO, id)));

        m.set(35, Icons.of(Items.SPYGLASS, "§eLive Recolour", "§7Drag hue/sat/bright with a live preview",
                        "§8Opens a screen — needs the mod client-side"),
                (p, b, a) -> GuiRouter.runCommand(p, "livecolor " + id));

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static String hardnessLabel(float v) {
        if (v < 0) return "unbreakable";
        if (v == 0) return "instant";
        return trim(v);
    }

    private static String hardnessArg(float v) {
        return v < 0 ? "-1" : trim(v);
    }

    private static String trim(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static float cycleHardness(float current, int dir) {
        int idx = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < HARDNESS.length; i++) {
            float diff = Math.abs(HARDNESS[i] - current);
            if (diff < best) { best = diff; idx = i; }
        }
        int next = (idx + dir + HARDNESS.length) % HARDNESS.length;
        return HARDNESS[next];
    }

    private static String cycleSound(String current, int dir) {
        String[] arr = SlotBlock.SOUND_TYPES;
        int idx = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(current)) { idx = i; break; }
        }
        int next = (idx + dir + arr.length) % arr.length;
        return arr[next];
    }

    private static String cycleCategory(String current, int dir) {
        List<String> cats = new ArrayList<>();
        cats.add("none");
        cats.addAll(SlotManager.categories());
        String cur = (current == null || current.isEmpty()) ? "none" : current;
        int idx = 0;
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i).equalsIgnoreCase(cur)) { idx = i; break; }
        }
        int next = (idx + dir + cats.size()) % cats.size();
        return cats.get(next);
    }
}
