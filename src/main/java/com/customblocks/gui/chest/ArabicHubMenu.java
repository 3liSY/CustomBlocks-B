/**
 * ArabicHubMenu.java — the Arabic "studio" landing GUI (Group 13 Pass 5 + Area 2d premium pass).
 *
 * A single gold-themed chest hub: Browse (the marked list), Make a Word/Text (all-in-GUI maker —
 * no chat handoff), Default Colours (edits the saved start colours), and Import/refresh. Deliberately
 * thin — it routes into ArabicListMenu / ArabicMaker / ColorStudioMenu rather than duplicating logic.
 *
 * Depends on: SlotManager, Icons, GuiFx, GuiRouter/Nav, ArabicMaker, ArabicWordSession, CustomBlocksConfig
 * Called by:  GuiRouter (Dest.ARABIC); opened by /cb arabic.
 */
package com.customblocks.gui.chest;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.arabic.ArabicMaker;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ArabicHubMenu {

    private ArabicHubMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        // Landing screen: drop any leftover maker state + throwaway preview block.
        ArabicMaker.clearPreview(player);
        ArabicWordSession.clear(player.getUuid());

        ChestMenu m = new ChestMenu("Arabic Studio", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.amber());
        for (int i = 45; i < 54; i++) m.set(i, Icons.amber());

        int bundled = SlotManager.byCategory("Arabic Letters").size()
                    + SlotManager.byCategory("Arabic Numbers").size()
                    + SlotManager.byCategory("English Numbers").size();

        m.set(4, Icons.glint(Items.WRITABLE_BOOK, "§6§lArabic Studio",
                "§7Browse, build and colour Arabic blocks.",
                "§7Letters & numbers come bundled; words and",
                "§7coloured text render on demand."));

        // Browse — the marked, paginated list (Area 2a).
        m.set(20, Icons.glint(Items.BOOK, "§b§lBrowse all blocks",
                        "§7" + bundled + " bundled block(s): letters, Arabic",
                        "§7numbers, English numbers — all marked.",
                        "§aClick §7→ open the browser"),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.navigate(p, MenuKey.of(Dest.ARABIC_LIST)); });

        // Make a Word / Text — fully in-GUI (Name → ID → Text → colours). No chat.
        m.set(22, Icons.glint(Items.NAME_TAG, "§a§lMake a Word / Text",
                        "§7Name it, give it an id, type the text,",
                        "§7then pick colours in the Color Studio.",
                        "§7Works for Arabic §8or§7 English.",
                        "§aClick §7→ start"),
                (p, b, a) -> { GuiFx.open(p); ArabicMaker.startGuiMaker(p); });

        // Default colours — pre-fills the Color Studio (Area 2d).
        m.set(24, Icons.glint(Items.BRUSH, "§d§lDefault Colours",
                        "§7Letter: §f" + CustomBlocksConfig.arabicDefaultLetterHex,
                        "§7Background: §f" + CustomBlocksConfig.arabicDefaultBgHex,
                        "§aClick §7→ edit the start colours"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    ArabicWordSession s = ArabicWordSession.start(p.getUuid());
                    s.defaultsMode = true;
                    s.bgArgb     = ArabicMaker.argb(CustomBlocksConfig.arabicDefaultBgHex);
                    s.letterArgb = ArabicMaker.argb(CustomBlocksConfig.arabicDefaultLetterHex);
                    GuiRouter.navigate(p, MenuKey.of(Dest.ARABIC_COLOR, "defaults"));
                });

        // Maintenance.
        m.set(40, Icons.of(Items.HOPPER, "§e§lImport / refresh bundled set",
                        "§7Make sure all 224 bundled blocks exist",
                        "§7(letters + numbers, x4 colours). Safe to re-run."),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.runCommand(p, "arabic import"); });

        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
