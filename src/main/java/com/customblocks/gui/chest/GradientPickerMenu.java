/**
 * GradientPickerMenu.java — Group 10 (Gradient Builder GUI).
 *
 * Responsibility: let the player pick two endpoint colours (by typing a hex/name via anvil OR by
 * picking a block from the block list whose average colour is used), choose the number of steps
 * (1-16), see a wool-preview of the interpolated swatches, and click Create to bake them into
 * real blocks. Delegates to the tested gradient logic in ColorImageCommands / ColorMath.
 *
 * Layout (5 rows):
 *   Row 1 (0-8):   accent frame + title.
 *   Row 2 (9-17):  Colour A tile (10) · preview swatches (12-14) · Colour B tile (16).
 *   Row 3 (18-26): Steps −/display/+ (21-23) · Create (25).
 *   Row 4 (27-35): empty filler.
 *   Row 5 (36-44): accent frame + back/close.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, AnvilPrompt, GradientSession, ColorLibrary,
 *             ColorMath, SlotManager, TextureStore, ColorImageCommands, CustomBlocksConfig.
 * Called by:  GuiRouter.build (Dest.GRADIENT_PICKER); opened from ColorsMenu and /cb gradient (no args).
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.PlayerPaletteManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.image.ColorMath;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class GradientPickerMenu {

    private GradientPickerMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Gradient Builder", 5).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 36; i < 45; i++) m.set(i, Icons.accent());

        GradientSession.State s = GradientSession.get(player.getUuid());

        m.set(4, Icons.of(Items.BRUSH, "§6§lGradient Builder",
                "§7Pick two colours, set the step count,",
                "§7and create a smooth gradient of blocks."));

        // Colour A tile.
        String aLabel = s.colorA != null ? "§f" + s.colorA : "§8not set";
        String aExtra = s.blockA != null ? "§7from block: §f" + s.blockA : "§7Type a hex or pick a block";
        Item aWool = s.colorA != null ? woolFor(s.colorA) : Items.LIGHT_GRAY_STAINED_GLASS_PANE;
        m.set(10, Icons.of(aWool, "§eColour A: " + aLabel, aExtra,
                        "§aLeft-click §7→ type colour",
                        "§eRight-click §7→ pick from a block"),
                (p, b, a) -> {
                    if (b == 1) { // right-click → pick from block list
                        GuiRouter.promptCommand(p, "/cb gradientpick a ", "gradientpick a <blockId>");
                    } else { // left-click → anvil
                        AnvilPrompt.open(p, "Colour A (name or #hex)", new ItemStack(Items.RED_DYE), "",
                                text -> {
                                    String hex = ColorLibrary.resolve(text.trim());
                                    if (hex != null) GradientSession.setColorA(p.getUuid(), hex, null);
                                    GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER));
                                },
                                () -> GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER)));
                    }
                });

        // Colour B tile.
        String bLabel = s.colorB != null ? "§f" + s.colorB : "§8not set";
        String bExtra = s.blockB != null ? "§7from block: §f" + s.blockB : "§7Type a hex or pick a block";
        Item bWool = s.colorB != null ? woolFor(s.colorB) : Items.LIGHT_GRAY_STAINED_GLASS_PANE;
        m.set(16, Icons.of(bWool, "§eColour B: " + bLabel, bExtra,
                        "§aLeft-click §7→ type colour",
                        "§eRight-click §7→ pick from a block"),
                (p, b, a) -> {
                    if (b == 1) { // right-click → pick from block list
                        GuiRouter.promptCommand(p, "/cb gradientpick b ", "gradientpick b <blockId>");
                    } else { // left-click → anvil
                        AnvilPrompt.open(p, "Colour B (name or #hex)", new ItemStack(Items.BLUE_DYE), "",
                                text -> {
                                    String hex = ColorLibrary.resolve(text.trim());
                                    if (hex != null) GradientSession.setColorB(p.getUuid(), hex, null);
                                    GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER));
                                },
                                () -> GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER)));
                    }
                });

        // Preview swatches — show interpolated colours between A and B as nearest-wool.
        if (s.colorA != null && s.colorB != null) {
            int rgbA = parseHex(s.colorA);
            int rgbB = parseHex(s.colorB);
            int previewCount = Math.min(s.steps, 5); // show up to 5 preview swatches
            int[] previewSlots = {12, 13, 14, 11, 15}; // centre first
            for (int i = 0; i < previewCount; i++) {
                double t = (double) (i + 1) / (previewCount + 1);
                int lerped = ColorMath.labLerp(rgbA, rgbB, t);
                m.set(previewSlots[i], Icons.of(woolFor(hex(lerped)), "§7Step " + (i + 1),
                        "§f" + hex(lerped)));
            }
        }

        // Steps controls.
        m.set(21, Icons.of(Items.RED_DYE, "§c− step", "§7Current: " + s.steps),
                (p, b, a) -> {
                    GradientSession.setSteps(p.getUuid(), s.steps - 1);
                    GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER));
                });
        m.set(22, Icons.glint(Items.COMPARATOR, "§eSteps: §f" + s.steps,
                "§7Number of gradient blocks to create."));
        m.set(23, Icons.of(Items.LIME_DYE, "§a+ step", "§7Current: " + s.steps),
                (p, b, a) -> {
                    GradientSession.setSteps(p.getUuid(), s.steps + 1);
                    GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER));
                });

        // Create button.
        boolean ready = s.colorA != null && s.colorB != null;
        if (ready) {
            m.set(25, Icons.of(Items.EMERALD_BLOCK, "§a§lCreate gradient",
                            "§7" + s.steps + " block(s) from",
                            "§f" + s.colorA + " §7→ §f" + s.colorB,
                            "§8Undoable with /cb undo."),
                    (p, b, a) -> {
                        // Build gradient from the two hex colours.
                        p.closeHandledScreen();
                        createGradientFromHex(p, s.colorA, s.colorB, s.steps);
                    });
        } else {
            m.set(25, Icons.of(Items.GRAY_DYE, "§7Create gradient",
                    "§8Pick both colours first."));
        }

        // Palette quick-pick endpoints (shared source) — fill the empty row 4.
        List<String> pal = PlayerPaletteManager.working(player.getUuid());
        if (pal.isEmpty()) {
            m.set(31, Icons.of(Items.GRAY_DYE, "§7No palette colours yet",
                    "§8Add colours in /cb coloring → Palette to",
                    "§8quick-pick gradient endpoints here."));
        } else {
            m.set(27, Icons.of(Items.PAINTING, "§ePalette colours",
                    "§aLeft-click §7a swatch → set Colour A",
                    "§eRight-click §7a swatch → set Colour B"));
            int[] slots = {28, 29, 30, 31, 32, 33, 34};
            for (int i = 0; i < slots.length && i < pal.size(); i++) {
                final String hx = pal.get(i);
                m.set(slots[i], Icons.of(woolFor(hx), "§f" + hx,
                                "§aLeft §7→ A §8| §eRight §7→ B"),
                        (p, b, a) -> {
                            if (b == 1) GradientSession.setColorB(p.getUuid(), hx, null);
                            else GradientSession.setColorA(p.getUuid(), hx, null);
                            GuiRouter.render(p, MenuKey.of(Dest.GRADIENT_PICKER));
                        });
            }
        }

        m.set(36, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(44, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /**
     * Create gradient blocks from two hex colours. Reuses the exact same logic as the existing
     * /cb gradient command but takes raw colours instead of block IDs.
     */
    static void createGradientFromHex(ServerPlayerEntity player, String hexA, String hexB, int steps) {
        var server = player.getServer();
        if (server == null) return;
        int rgbA = parseHex(hexA);
        int rgbB = parseHex(hexB);
        int size = CustomBlocksConfig.textureSize;
        int n = Math.max(1, Math.min(16, steps));
        com.customblocks.command.Chat.info(player.getCommandSource(), "Generating §e" + n + "§r gradient block(s)…");
        Thread worker = new Thread(() -> {
            try {
                java.util.List<byte[]> pngs = new java.util.ArrayList<>();
                for (int k = 1; k <= n; k++) {
                    double tStep = (double) k / (n + 1);
                    pngs.add(ColorMath.solidPng(ColorMath.labLerp(rgbA, rgbB, tStep), size));
                }
                server.execute(() -> {
                    java.util.List<com.customblocks.core.UndoManager.Op> children = new java.util.ArrayList<>();
                    java.util.List<String> made = new java.util.ArrayList<>();
                    int next = 1;
                    for (byte[] png : pngs) {
                        String gid;
                        do { gid = "gradient_" + (next++); } while (SlotManager.hasId(gid));
                        SlotData created = SlotManager.create(gid, "Gradient " + gid.substring("gradient_".length()));
                        if (created == null) break; // slot pool exhausted
                        TextureStore.save(created.index(), png);
                        children.add(new com.customblocks.core.UndoManager.Op(
                                com.customblocks.core.UndoManager.Kind.CREATE, null, created, null, "create"));
                        made.add(gid);
                    }
                    com.customblocks.network.ResourcePackServer.updatePack();
                    com.customblocks.network.ResourcePackServer.syncToAll();
                    if (children.isEmpty()) {
                        com.customblocks.command.Chat.error(player.getCommandSource(),
                                "Couldn't create gradient blocks — every slot is in use.");
                        return;
                    }
                    com.customblocks.core.UndoManager.recordBatch(player.getUuid(), children,
                            "gradient (" + children.size() + ")");
                    com.customblocks.command.Chat.success(player.getCommandSource(),
                            "Created §a" + made.size() + "§r gradient block(s): §f"
                                    + String.join(", ", made) + "§r. §7One /cb undo removes them all.");
                });
            } catch (Exception e) {
                com.customblocks.core.IncidentRecorder.record("Gradient picker failed", e);
                server.execute(() -> com.customblocks.command.Chat.error(player.getCommandSource(),
                        "Couldn't build that gradient."));
            }
        }, "CustomBlocks-GradientPicker");
        worker.setDaemon(true);
        worker.start();
    }

    private static int parseHex(String hex) {
        return Swatch.parseHex(hex, 0x808080);
    }

    private static String hex(int rgb) {
        return Swatch.hex(rgb);
    }

    /** Nearest of the 16 dye-wools to a "#RRGGBB" string, for a visual swatch cue. */
    private static Item woolFor(String hex) {
        return Swatch.woolFor(hex);
    }
}
