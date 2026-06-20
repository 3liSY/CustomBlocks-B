/**
 * ArabicListMenu.java — the Arabic Browser LANDING screen (Group 13 Area 2a, revamp 2026-06-16).
 *
 * Instead of dumping all 224 colour variants in one flat scroll, this is now a small group picker:
 * one tile per marked group (Arabic Letters / Arabic Numbers / English Numbers / English Letters —
 * coming soon). Click a group → ArabicGroupMenu opens an aligned grid of just that group with a
 * colour rail to switch the black/red/green/yellow variant. Mirrors the bulk Step1→Step2 flow.
 *
 * Opened by /cb arabic list (and the hub's Browse tile). Depends on: SlotManager, Icons, GuiFx,
 * GuiRouter/Nav. Called by: GuiRouter (Dest.ARABIC_LIST).
 */
package com.customblocks.gui.chest;

import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ArabicListMenu {

    private ArabicListMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        ChestMenu m = new ChestMenu("Arabic Browser", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.amber());
        for (int i = 45; i < 54; i++) m.set(i, Icons.amber());

        m.set(4, Icons.glint(Items.WRITABLE_BOOK, "§6§lArabic Browser",
                "§7Pick a group to browse.",
                "§7Each glyph comes in §fx4 colours§7 — switch",
                "§7them with the colour rail inside."));

        // Three bundled groups + a coming-soon placeholder, evenly spaced on the middle row.
        group(m, 19, "Arabic Letters",  Items.BOOK,    "§7Letters + extra forms.");
        group(m, 21, "Arabic Numbers",  Items.CLOCK,   "§7Eastern ٠-٩.");
        group(m, 23, "English Numbers", Items.COMPASS, "§70-9 (Western).");
        m.set(25, Icons.of(Items.GRAY_DYE, "§8§lEnglish Letters",
                "§8Coming soon — not bundled yet.",
                "§7Use §fMake a Word§7 for English text for now."));

        m.set(45, Icons.back(), (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** One group tile — shows the bundled count and opens that group's grid (default black). */
    private static void group(ChestMenu m, int slot, String cat, Item icon, String note) {
        int n = SlotManager.byCategory(cat).size();
        m.set(slot, Icons.glint(icon, "§6§l" + cat, note,
                        "§7" + n + " block(s) bundled.",
                        "§aClick §7→ browse this group"),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.navigate(p, MenuKey.of(Dest.ARABIC_GROUP, cat + "|black")); });
    }
}
