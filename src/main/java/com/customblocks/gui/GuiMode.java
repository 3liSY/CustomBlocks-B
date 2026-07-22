/**
 * GuiMode.java
 *
 * Responsibility: Enum of all CustomBlocks in-game GUI screens. The int id is
 * what travels in OpenGuiPayload so the client knows which screen to open.
 *
 * Depends on: (none)
 * Called by: GuiCommands (chooses mode), CustomBlocksClient (dispatches to screen)
 */
package com.customblocks.gui;

public enum GuiMode {
    MAIN_MENU(0),
    BLOCK_EDITOR(1),
    CONFIG(2),
    TEMPLATE_LIST(3),
    ARABIC_BROWSER(4),
    MACRO_LIST(5),
    HUD_EDITOR(6),
    RECOLOR_SLIDER(7),  // Group 10 — live recolour slider (client screen)
    EYEDROP(8),         // Group 10 — sample a colour from the Minecraft screen (client screen)
    ARABIC_PREVIEW(9),  // Group 13 — live Arabic word preview, 3D rotatable block (client screen)
    SHAPE_EDITOR(10),   // Group 27 §G27.5 — named-shape picker, 3D shape preview (client screen)
    CREATE_STUDIO(11),  // Group 27 §G27.6 — block creation studio, /cb create no-args (client screen)
    RECORD_OVERLAY(12), // Group 29 Build B - Record Overlay Studio/actions (client-local)
    CATEGORY_HUB(13),   // Group 27 Category Hub — full red+black category manager (client screen)
    GUESS_SETTINGS(14), // Group 30 §10 / G30-4 — shared Guess Settings screen (Pose tab: sliders + live dummy)
    // id 15 retired — the Group 31 BuzzerGame admin panel Screen was scrapped for the wand-owned session (2026-07-18)
    BULK_WORKBENCH(16), // Group 07 — Bulk Operations Hub (Catalog · Console · Health tabs; replaces the chest bulk flow)
    BACKUP_SCREEN(17),  // Group 09 §G09-A4 — Backup Screen (list · restore · delete · rename · protect; replaces the chest backup menu)
    SETALL_SCREEN(18);  // Group 07 — Set All Screen (/cb setall on a dedicated screen: setting + value → apply to every block)

    public final int id;

    GuiMode(int id) { this.id = id; }

    public static GuiMode fromId(int id) {
        for (GuiMode m : values()) if (m.id == id) return m;
        return MAIN_MENU;
    }
}
