/**
 * BgStudioMenu.java — Group 10 (Background Studio).
 *
 * Responsibility: per-block background-removal GUI. Pick a mode (Keep / Remove / Remove+gaps /
 * Smart auto), nudge the strength, pick a fill colour for the removed area — either via the anvil
 * or one-click from your Palette working set — then Apply. Apply is undoable, so the flow is
 * "apply → look at the block in-world → ↩ Undo if it's wrong" (the Undo tile sits right here).
 * No image logic is duplicated — Apply delegates to the tested ColorToolService.applyBgRemoval.
 *
 * Depends on: ChestMenu, Icons, Swatch, GuiRouter/Nav, AnvilPrompt, BgStudioSession,
 *             PlayerPaletteManager, BackgroundRemover, ColorToolService, ColorLibrary, SlotManager.
 * Called by:  GuiRouter.build (Dest.BGSTUDIO); opened from the block picker, EditorMenu and /cb bgstudio.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.ColorLibrary;
import com.customblocks.core.ColorToolService;
import com.customblocks.core.PlayerPaletteManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.image.BackgroundRemover;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class BgStudioMenu {

    private BgStudioMenu() {} // static-only

    private static final int[] PALETTE_SLOTS = {30, 31, 32, 33};

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        ChestMenu m = new ChestMenu("Background Studio - " + id, 5).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 36; i < 45; i++) m.set(i, Icons.accent());

        SlotData d = SlotManager.getById(id);
        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block \"" + id + "\""));
            m.set(36, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(44, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        BgStudioSession.State s = BgStudioSession.get(player.getUuid(), id);
        Item icon = SlotManager.itemAt(d.index());
        m.set(4, Icons.of(icon != null ? icon : Items.PAINTING, "§f§l" + d.displayName(),
                "§7id: §f" + id,
                "§7mode: §f" + BackgroundRemover.displayName(s.mode),
                "§7fill: §f" + s.fillColor,
                "§8Pick a mode, then Apply."));

        modeTile(m, 11, id, s, BackgroundRemover.NONE, Items.GLASS,  "§fKeep background",
                "§7Show the original image — no cut.");
        modeTile(m, 15, id, s, BackgroundRemover.AUTO, Items.SHEARS, "§eRemove background",
                "§7Works it out from the picture itself.",
                "§8Nothing to tune. If it cannot tell,",
                "§8it leaves the picture alone and says so.");

        // ↩ Undo — the "preview" is: apply, look at the block in-world, undo here if it's wrong.
        m.set(27, Icons.of(Items.LEVER, "§e↩ Undo last change",
                        "§7Reverts the last apply on any block.",
                        "§8Same as /cb undo."),
                (p, b, a) -> GuiRouter.runCommand(p, "undo"));

        // Fill-colour picker — what colour the removed background becomes.
        m.set(28, Icons.of(Swatch.woolFor(s.fillColor), "§eFill colour: §f" + s.fillColor,
                        "§7Removed area is painted this colour.",
                        "§aLeft-click §7→ type a new colour",
                        "§cRight-click §7→ reset to black"),
                (p, b, a) -> {
                    if (b == 1) { // right-click → reset
                        BgStudioSession.setFillColor(p.getUuid(), id, "#000000");
                        GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id));
                    } else { // left-click → anvil
                        AnvilPrompt.open(p, "Fill colour (name or #hex)", new ItemStack(Items.LIME_DYE),
                                s.fillColor,
                                text -> {
                                    String hex = ColorLibrary.resolve(text.trim());
                                    if (hex != null) BgStudioSession.setFillColor(p.getUuid(), id, hex);
                                    GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id));
                                },
                                () -> GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id)));
                    }
                });

        // Palette quick-fills — one-click fill colours from the player's working set (shared source).
        List<String> pal = PlayerPaletteManager.working(player.getUuid());
        if (pal.isEmpty()) {
            m.set(31, Icons.of(Items.GRAY_DYE, "§7No palette colours yet",
                    "§8Add colours in /cb coloring → Palette,",
                    "§8then quick-pick them here as fills."));
        } else {
            for (int i = 0; i < PALETTE_SLOTS.length && i < pal.size(); i++) {
                final String hex = pal.get(i);
                m.set(PALETTE_SLOTS[i], Icons.of(Swatch.woolFor(hex), "§fFill: " + hex,
                                "§7From your palette.",
                                "§aClick §7→ use as the fill colour"),
                        (p, b, a) -> {
                            BgStudioSession.setFillColor(p.getUuid(), id, hex);
                            GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id));
                        });
            }
        }

        // Apply — bake the chosen mode + fill.
        m.set(40, Icons.of(Items.EMERALD_BLOCK, "§a§lApply",
                        "§7Run §f" + BackgroundRemover.displayName(s.mode),
                        "§7fill: §f" + s.fillColor + " §7on \"" + id + "\".",
                        "§8Then look in-world — ↩ Undo here if it's wrong."),
                (p, b, a) -> {
                    BgStudioSession.State cur = BgStudioSession.get(p.getUuid(), id);
                    ColorToolService.applyBgRemoval(p, id, cur.mode, BgStudioSession.fillColorRgb(cur));
                    GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id));
                });

        m.set(36, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(44, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    private static void modeTile(ChestMenu m, int slot, String id, BgStudioSession.State s,
                                 String mode, Item item, String name, String... lore) {
        boolean selected = mode.equals(s.mode);
        String[] full = new String[lore.length + 1];
        System.arraycopy(lore, 0, full, 0, lore.length);
        full[lore.length] = selected ? "§aSelected" : "§eClick §7→ select";
        var stack = selected ? Icons.glint(item, name + " §a✔", full) : Icons.of(item, name, full);
        m.set(slot, stack, (p, b, a) -> {
            BgStudioSession.setMode(p.getUuid(), id, mode);
            GuiRouter.render(p, MenuKey.of(Dest.BGSTUDIO, id));
        });
    }
}
