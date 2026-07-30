/**
 * SettingsBookMenu.java
 *
 * Responsibility: The "Settings Book" -- a single-screen, TABBED chest GUI for every server setting
 * (Group 21, D1). A row of named section tabs sits across the top; clicking a tab swaps the panel
 * below to that section's settings. No page flip, no fold-drawers, no quick-toggle clutter -- you
 * always see which tab you are on (it glows) and every setting is one click away.
 *
 * The active tab id rides in MenuKey.arg, so it survives the in-place refresh after an edit (the title
 * is constant, so switching tabs and editing both refresh without the cursor jumping). Things that
 * already have their own chest (Variant colours, Effects, Discord, AI, Arabic) appear as opener tiles;
 * field wiring is delegated to {@link FieldSlots}.
 *
 * Depends on: ConfigRegistry + ConfigField (the model), FieldSlots (editing) + FieldIcon (rendering),
 *             Icons, BookSfx, GuiRouter/Nav.
 * Called by:  GuiRouter.build (Dest.CONFIG). Replaces the old ConfigMenu.
 */
package com.customblocks.gui.chest;

import com.customblocks.config.ConfigField;
import com.customblocks.config.ConfigRegistry;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SettingsBookMenu {

    private SettingsBookMenu() {} // static-only

    /** Constant title so tab switches + edits refresh the open screen in place (no cursor jump). */
    private static final String TITLE = "Settings";

    /** One top tab: id (also MenuKey.arg), label, icon, and the ordered keys it shows in the panel. */
    private record Section(String id, String label, Item icon, String help, String[] entries) {}

    private static final Section[] SECTIONS = {
        new Section("general", "General", Items.LEVER, "Everyday basics.",
                new String[]{"max_blocks", "texture_quality", "silent_pack", "auto_category"}),
        new Section("appearance", "Appearance", Items.BRUSH, "How blocks look + feedback.",
                // "background_strength" came out with /cb tolerance (G10 §H) — the field no longer exists.
                new String[]{"transparent_background", "background_removal",
                        "variant_colours", "named_texture_mirror", "effects", "edit_hud"}),
        new Section("network", "Network & Cloud", Items.BEACON, "Texture server, cloud + integrations.",
                new String[]{"resource_pack_port", "server_ip", "cloud_sharing", "cloud_url",
                        "cloud_secret", "cloud_test", "discord", "ai"}),
        new Section("backups", "Backups & History", Items.BARREL, "Backups, trash + undo history.",
                new String[]{"auto_backup_interval", "auto_backup_keep", "trash_retention_days",
                        "undo_depth", "history_mode"}),
        new Section("content", "Content", Items.BOOK, "Arabic studio + thresholds.",
                new String[]{"arabic", "bulk_confirm_threshold", "payloads_per_tick"}),
        new Section("system", "System", Items.REDSTONE, "Tools + permissions.",
                new String[]{"tools", "permissions"}),
    };

    public static ChestMenu build(ServerPlayerEntity player, String sectionId) {
        Section sec = find(sectionId);

        ChestMenu m = new ChestMenu(TITLE, 6).fill();
        tabStrip(m, sec);
        panel(m, sec);
        bottomRow(m);
        return m;
    }

    private static Section find(String id) {
        if (id != null && !id.isEmpty()) {
            for (Section s : SECTIONS) if (s.id.equals(id)) return s;
        }
        return SECTIONS[0];
    }

    // ── top row: title + the six tabs ────────────────────────────────────────────
    private static void tabStrip(ChestMenu m, Section active) {
        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());
        m.set(0, Icons.of(Items.ENCHANTED_BOOK, "§6§lSettings",
                "§7Tab: §f" + active.label,
                "§7Click a tab to switch section.",
                "§7Hover any item for a plain-English tip."));
        for (int i = 0; i < SECTIONS.length; i++) {
            Section s = SECTIONS[i];
            boolean on = s.id.equals(active.id);
            String name = (on ? "§a§l" : "§e") + s.label;
            m.set(1 + i,
                    on ? Icons.glint(s.icon, name, "§7" + s.help, "§8You are here")
                       : Icons.of(s.icon, name, "§7" + s.help, "§aClick §7to open"),
                    (p, b, a) -> { if (!on) { BookSfx.turn(p);
                            GuiRouter.repage(p, MenuKey.of(Dest.CONFIG, s.id)); } });
        }
    }

    // ── body: the active section's settings (rows 1-4) ───────────────────────────
    private static void panel(ChestMenu m, Section sec) {
        int idx = 0;
        for (String key : sec.entries) {
            int row = idx / 7;
            if (row > 3) break;                 // stay within rows 1-4
            int slot = 10 + row * 9 + (idx % 7); // cols 1-7; col 0 + col 8 stay framed
            FieldSlots.place(m, slot, ConfigRegistry.byKey(key));
            idx++;
        }
    }

    // ── bottom row: actions (Find / Filter / Reset arrive in phase 5) ────────────
    private static void bottomRow(ChestMenu m) {
        for (int i = 45; i < 54; i++) m.set(i, Icons.accent());
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(47, Icons.of(Items.SPYGLASS, "§eFind a setting", "§8Search (coming in a later build)"));
        m.set(48, Icons.of(Items.HOPPER, "§eFilter", "§8Show only changed / one section (later build)"));
        m.set(50, Icons.of(Items.TNT, "§cReset everything", "§8Restore all defaults (later build)"));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
    }
}
