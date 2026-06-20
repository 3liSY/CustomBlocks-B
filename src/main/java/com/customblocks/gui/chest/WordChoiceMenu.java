/**
 * WordChoiceMenu.java — after the text anvil: one textured word block, or loose letter blocks
 * (Group 13 Area 2). Reads the pending text from ArabicWordSession; the two buttons delegate to
 * ArabicMaker (single → Color Studio, letters → give bundled blocks). Gold "studio" theme.
 *
 * Depends on: ChestMenu, Icons, ArabicWordSession, ArabicMaker, GuiRouter
 * Called by:  GuiRouter (Dest.ARABIC_CHOICE)
 */
package com.customblocks.gui.chest;

import com.customblocks.arabic.ArabicMaker;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WordChoiceMenu {

    private WordChoiceMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ArabicWordSession s = ArabicWordSession.get(player.getUuid());
        if (s == null || s.text == null) {
            GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.ARABIC));
            return new ChestMenu("Arabic Studio", 1);
        }
        String text = s.text;

        ChestMenu m = new ChestMenu("How to make it", 3).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.amber());
        for (int i = 18; i < 27; i++) m.set(i, Icons.amber());
        m.set(4, Icons.glint(Items.WRITABLE_BOOK, "§6§lYou typed: §f" + text,
                "§7Pick how to turn it into block(s)."));

        m.set(11, Icons.glint(Items.PAINTING, "§a§lSingle texture block",
                        "§7Render \"§f" + text + "§7\" as ONE block.",
                        "§7Next: pick background + letter colours.",
                        "§aClick §7→ Color Studio"),
                (p, b, a) -> { GuiFx.click(p); ArabicMaker.chooseSingle(p); });

        m.set(15, Icons.glint(Items.CHEST, "§a§lPlace letter blocks",
                        "§7Get one joinable letter block per Arabic",
                        "§7character. Place them in a row, right-to-left —",
                        "§7they auto-join into the word as you go.",
                        "§aClick §7→ get the letters"),
                (p, b, a) -> { GuiFx.click(p); ArabicMaker.chooseLetters(p); });

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }
}
