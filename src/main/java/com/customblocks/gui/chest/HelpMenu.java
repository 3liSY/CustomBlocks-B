/**
 * HelpMenu.java — the /cb help command browser (Group 04). Two views driven by the
 * MenuKey arg: "" = the category overview, "<category>" = that category's command list.
 * Clicking a command closes the chest and pre-fills the command in the player's chat
 * input via ChatPrefillPayload (only the fixed part — placeholders like <id> are
 * stripped so the player just types the arguments).
 */
package com.customblocks.gui.chest;

import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.network.payloads.ChatPrefillPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class HelpMenu {

    private HelpMenu() {} // static-only

    /** Overview slots for the category buttons (centred rows of a 6-row chest). */
    private static final int[] CATEGORY_SLOTS = {19, 20, 21, 22, 23, 24, 25, 30, 31, 32};

    public static ChestMenu build(ServerPlayerEntity player, String category, int page) {
        HelpTopics.Category cat = category == null || category.isEmpty()
                ? null : HelpTopics.byName(category);
        return cat == null ? buildOverview() : buildCategory(cat, page);
    }

    // ── Category overview ────────────────────────────────────────────────────

    private static ChestMenu buildOverview() {
        ChestMenu m = new ChestMenu("CustomBlocks Help", 6).fill();
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());

        m.set(4, Icons.glint(Items.KNOWLEDGE_BOOK, "§b§lCommand Browser",
                "§7Every /cb command, organized by topic.",
                "§7Open a category, then click a command",
                "§7to pre-fill it in your chat."));

        List<HelpTopics.Category> cats = HelpTopics.CATEGORIES;
        for (int i = 0; i < cats.size() && i < CATEGORY_SLOTS.length; i++) {
            HelpTopics.Category c = cats.get(i);
            m.set(CATEGORY_SLOTS[i], Icons.of(c.icon(), "§e§l" + c.name(),
                            "§7" + c.desc(),
                            "§8" + c.topics().size() + " command(s)",
                            "§aClick §7→ browse"),
                    (p, b, a) -> GuiRouter.navigate(p, MenuKey.of(Dest.HELP, c.name())));
        }

        m.set(49, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── One category's command list ──────────────────────────────────────────

    private static ChestMenu buildCategory(HelpTopics.Category cat, int page) {
        ChestMenu m = new ChestMenu("Help — " + cat.name(), 6).fill();
        List<HelpTopics.Topic> topics = cat.topics();

        int maxPage = Math.max(0, (topics.size() - 1) / Layout.PER_PAGE);
        int p0 = Math.min(Math.max(0, page), maxPage);
        int start = p0 * Layout.PER_PAGE;
        int end = Math.min(topics.size(), start + Layout.PER_PAGE);

        for (int i = start; i < end; i++) {
            HelpTopics.Topic t = topics.get(i);
            m.set(i - start, Icons.of(Items.PAPER, "§e" + t.suggest(),
                            "§7" + t.desc(),
                            "§aClick §7→ pre-fill in chat"),
                    (pl, b, a) -> prefill(pl, t.suggest()));
        }

        Layout.pagedFooter(m, p0, maxPage, Dest.HELP, cat.name(), topics.size());
        return m;
    }

    /**
     * Close the chest and open the player's chat with the fixed part of the command
     * already typed. "/cb create <id> <name> <url>" pre-fills "/cb create " — the
     * placeholder tokens are stripped so the player only types real arguments.
     */
    private static void prefill(ServerPlayerEntity player, String suggest) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        int ph = suggest.indexOf(" <");
        String text = ph < 0 ? suggest : suggest.substring(0, ph) + " ";
        s.execute(() -> {
            player.closeHandledScreen();
            ServerPlayNetworking.send(player, new ChatPrefillPayload(text));
        });
    }
}
