/**
 * ColorStudioMenu.java — pick BACKGROUND + LETTER colours for an Arabic/Latin word block
 * (Group 13 Area 2c/2d). A curated 12-swatch palette plus a custom #hex anvil for each channel,
 * the selected swatch glows, and a live nearest-colour preview tile. The black outline always
 * stays. Two modes (key.arg):
 *   • "word"     — colours feed ArabicMaker.finalizeWord; a "Render preview" button makes the
 *                  true glyph block to look at. Create → make the block.
 *   • "defaults" — edits the saved CustomBlocksConfig.arabicDefault* colours. Save → config.
 *
 * Reuses the HexColorsMenu pattern (swatch → AnvilPrompt → validated hex). No render/slot logic
 * is duplicated; creation/preview delegate to ArabicMaker.
 *
 * Depends on: ChestMenu, Icons, GuiFx, GuiRouter/Nav, AnvilPrompt, ArabicWordSession, ArabicMaker,
 *             CustomBlocksConfig, Chat
 * Called by:  GuiRouter (Dest.ARABIC_COLOR)
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.arabic.ArabicMaker;
import com.customblocks.command.Chat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ColorStudioMenu {

    private ColorStudioMenu() {} // static-only

    /** One preset: a display name, its #hex, and the concrete block shown as the swatch. */
    private record Sw(String name, String hex, Item item) {}

    private static final Sw[] PALETTE = {
            new Sw("Black",  "#0A0A0A", Items.BLACK_CONCRETE),
            new Sw("White",  "#FFFFFF", Items.WHITE_CONCRETE),
            new Sw("Red",    "#FF0000", Items.RED_CONCRETE),
            new Sw("Green",  "#1E8C1E", Items.GREEN_CONCRETE),
            new Sw("Yellow", "#F0C814", Items.YELLOW_CONCRETE),
            new Sw("Blue",   "#2E6FF2", Items.BLUE_CONCRETE),
            new Sw("Cyan",   "#21C0C0", Items.CYAN_CONCRETE),
            new Sw("Purple", "#8E3FD0", Items.PURPLE_CONCRETE),
            new Sw("Pink",   "#F060A8", Items.PINK_CONCRETE),
            new Sw("Orange", "#F08020", Items.ORANGE_CONCRETE),
            new Sw("Gray",   "#808080", Items.GRAY_CONCRETE),
            new Sw("Brown",  "#8A5A2B", Items.BROWN_CONCRETE),
    };

    // Aligned 6x2 swatch grid per channel (cols 2-7), with the channel header on col 0 and the
    // custom #hex tile on col 8 flanking each band — reads like a palette, not scattered.
    private static final int[] BG_SLOTS  = {11,12,13,14,15,16, 20,21,22,23,24,25};
    private static final int[] LET_SLOTS = {29,30,31,32,33,34, 38,39,40,41,42,43};

    public static ChestMenu build(ServerPlayerEntity player, String mode) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null) { GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.ARABIC)); return new ChestMenu("Arabic Studio", 1); }
        boolean defaults = "defaults".equals(mode);

        ChestMenu m = new ChestMenu(defaults ? "Default Colours" : "Color Studio", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.amber());
        String sub = defaults ? "§7Set the colours new word blocks start on."
                              : "§7Colour \"§f" + s.text + "§7\" — outline stays black.";
        m.set(4, Icons.glint(Items.BRUSH, "§6§l" + (defaults ? "Default Colours" : "Color Studio"), sub,
                "§7Pick a swatch or type a custom §f#hex§7."));

        // ── Background channel ──
        m.set(9, Icons.glint(nearest(s.bgArgb), "§6§lBackground §f= " + ArabicMaker.hex(s.bgArgb),
                "§7The block's background fill."));
        placeSwatches(m, BG_SLOTS, s.bgArgb, mode, true);
        m.set(17, Icons.of(Items.NAME_TAG, "§e§lCustom background §f#hex",
                        "§7Type any §f#RRGGBB §7background.",
                        "§aClick §7→ anvil"),
                (p, b, a) -> { GuiFx.click(p); openHex(p, true, mode); });

        // ── Letter channel ──
        m.set(27, Icons.glint(nearest(s.letterArgb), "§f§lLetter §f= " + ArabicMaker.hex(s.letterArgb),
                "§7The glyph colour (black outline stays)."));
        placeSwatches(m, LET_SLOTS, s.letterArgb, mode, false);
        m.set(35, Icons.of(Items.NAME_TAG, "§e§lCustom letter §f#hex",
                        "§7Type any §f#RRGGBB §7letter colour.",
                        "§aClick §7→ anvil"),
                (p, b, a) -> { GuiFx.click(p); openHex(p, false, mode); });

        // ── Footer ──
        m.set(45, Icons.back(), (p, b, a) -> { ArabicMaker.clearPreview(p); GuiRouter.back(p); });
        m.set(53, Icons.close(), (p, b, a) -> { ArabicMaker.clearPreview(p); p.closeHandledScreen(); });

        if (defaults) {
            m.set(49, Icons.glint(Items.LIME_CONCRETE, "§a§lSave defaults",
                            "§7Background: §f" + ArabicMaker.hex(s.bgArgb),
                            "§7Letter: §f" + ArabicMaker.hex(s.letterArgb),
                            "§aClick §7→ save"),
                    (p, b, a) -> saveDefaults(p, s));
        } else {
            m.set(46, Icons.glint(nearest(s.bgArgb), "§b§lPreview §8(swatch)",
                    "§7Letter §f" + ArabicMaker.hex(s.letterArgb) + " §7on §f" + ArabicMaker.hex(s.bgArgb),
                    "§8Instant colour preview (nearest block)."));
            m.set(47, Icons.of(Items.SPYGLASS, "§d§lRender preview",
                            "§7Make the REAL glyph block to look at",
                            "§7(reused + cleaned up automatically).",
                            "§aClick §7→ render"),
                    (p, b, a) -> { GuiFx.click(p); ArabicMaker.renderPreview(p); });
            m.set(49, Icons.glint(Items.LIME_CONCRETE, "§a§lCreate block",
                            "§7Name: §f" + s.name + " §7· id: §f" + s.id,
                            "§7Letter §f" + ArabicMaker.hex(s.letterArgb) + " §7on §f" + ArabicMaker.hex(s.bgArgb),
                            "§aClick §7→ create + give"),
                    (p, b, a) -> { GuiFx.click(p); ArabicMaker.finalizeWord(p); });
        }
        return m;
    }

    /** Place a palette row for one channel; the selected colour glows. */
    private static void placeSwatches(ChestMenu m, int[] slots, int current, String mode, boolean bg) {
        for (int i = 0; i < PALETTE.length && i < slots.length; i++) {
            Sw sw = PALETTE[i];
            int argb = ArabicMaker.argb(sw.hex());
            boolean sel = argb == current;
            ItemStack icon = sel
                    ? Icons.glint(sw.item(), "§a§l" + sw.name() + " §8(selected)", "§7" + sw.hex())
                    : Icons.of(sw.item(), "§f" + sw.name(), "§7" + sw.hex(), "§aClick §7→ use");
            m.set(slots[i], icon, (p, b, a) -> {
                GuiFx.click(p);
                ArabicWordSession s = ArabicWordSession.get(p.getUuid());
                if (s == null) return;
                if (bg) s.bgArgb = argb; else s.letterArgb = argb;
                GuiRouter.render(p, Nav.MenuKey.of(Nav.Dest.ARABIC_COLOR, mode));
            });
        }
    }

    /** Anvil to type a custom #hex for the bg/letter channel; valid input updates + reopens. */
    private static void openHex(ServerPlayerEntity player, boolean bg, String mode) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null) return;
        String cur = ArabicMaker.hex(bg ? s.bgArgb : s.letterArgb);
        AnvilPrompt.open(player, "Type #RRGGBB", new ItemStack(Items.NAME_TAG), cur,
                text -> {
                    String norm = CustomBlocksConfig.normalizeHexColor(text, null);
                    if (norm == null) Chat.error(player.getCommandSource(), "Not a colour — use #RRGGBB (e.g. #FF8800).");
                    else {
                        ArabicWordSession ss = ArabicWordSession.get(player.getUuid());
                        if (ss != null) { if (bg) ss.bgArgb = ArabicMaker.argb(norm); else ss.letterArgb = ArabicMaker.argb(norm); }
                    }
                    GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.ARABIC_COLOR, mode));
                },
                () -> GuiRouter.render(player, Nav.MenuKey.of(Nav.Dest.ARABIC_COLOR, mode)));
    }

    private static void saveDefaults(ServerPlayerEntity player, ArabicWordSession s) {
        CustomBlocksConfig.arabicDefaultBgHex     = ArabicMaker.hex(s.bgArgb);
        CustomBlocksConfig.arabicDefaultLetterHex = ArabicMaker.hex(s.letterArgb);
        CustomBlocksConfig.save();
        Chat.success(player.getCommandSource(), "Saved default colours: letter §f"
                + CustomBlocksConfig.arabicDefaultLetterHex + " §7on §f" + CustomBlocksConfig.arabicDefaultBgHex + "§r.");
        ArabicWordSession.clear(player.getUuid());
        GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.ARABIC));
    }

    /** The palette swatch block closest to an ARGB colour — gives any hex a coloured preview tile. */
    private static Item nearest(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        Item best = Items.WHITE_CONCRETE; long bestD = Long.MAX_VALUE;
        for (Sw sw : PALETTE) {
            int a = ArabicMaker.argb(sw.hex());
            int dr = r - ((a >> 16) & 0xFF), dg = g - ((a >> 8) & 0xFF), db = b - (a & 0xFF);
            long d = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (d < bestD) { bestD = d; best = sw.item(); }
        }
        return best;
    }
}
