/**
 * NotesMenu.java — the Lore GUI (Group 18 REVAMP v2). A single-screen server-side chest (no tabs): the
 * block's lore lines listed in a grid, a + Add line button, an On/Off switch, Share, and Close. Each line
 * is typed in an anvil (≤ {@link NoteData#MAX_LINE_LEN} chars); left-click a line edits it, right-click
 * deletes it. Everything saves live through BlockNotesManager — there is no Save button.
 *
 * & colour codes in a line render as colours both here and on the item (decision R10). When the switch is
 * On, every line shows under the item name on hover; when Off the lines stay saved but are hidden.
 *
 * Depends on: ChestMenu, Icons, GuiRouter/Nav, AnvilPrompt, BlockNotesManager, NoteData, SlotManager, HudSync
 * Called by:  GuiRouter.build (Dest.NOTES); opened by /cb lore <id> (alias /cb note <id>) and the Editor menu.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.NoteData;
import com.customblocks.command.handlers.NoteCommands;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.network.HudSync;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class NotesMenu {

    private NotesMenu() {} // static-only

    /** Inner content slots (rows 1-4, columns 1-7) for the lore lines — 28 slots = NoteData.MAX_LINES. */
    private static final int[] LINE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
    };

    public static ChestMenu build(ServerPlayerEntity player, String id) {
        ChestMenu m = new ChestMenu("Lore — " + id, 6).fill();

        SlotData d = SlotManager.getById(id);
        if (d == null) {
            m.set(22, Icons.of(Items.BARRIER, "§cNo block \"" + id + "\""));
            m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
            m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
            return m;
        }

        NoteData note = BlockNotesManager.get(id);
        List<String> lines = note.lines();

        for (int i = 0; i < 9; i++) m.set(i, Icons.accent());

        // ── Line list ────────────────────────────────────────────────────────────
        if (lines.isEmpty()) {
            m.set(22, Icons.of(Items.PAPER, "§7No lore yet",
                    "§8These lines show under the item's", "§8name when you hover it.",
                    "", "§aClick §f+ Add line§a below to start."));
        } else {
            for (int i = 0; i < lines.size() && i < LINE_SLOTS.length; i++) {
                final int idx = i;
                String text = lines.get(i);
                m.set(LINE_SLOTS[i], Icons.ofCoded(Items.PAPER, text,
                                "§8line " + (i + 1) + " / " + lines.size(),
                                "§7Left-click: §eedit", "§7Right-click: §cdelete"),
                        (p, b, a) -> {
                            if (b == 1) { BlockNotesManager.removeLine(id, idx); resync(p); reopen(p, id); }
                            else AnvilPrompt.open(p, "Edit line", new ItemStack(Items.PAPER), text,
                                    val -> { BlockNotesManager.editLine(id, idx, val); resync(p); reopen(p, id); },
                                    () -> reopen(p, id));
                        });
            }
        }

        // ── Footer ─────────────────────────────────────────────────────────────────
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));

        boolean full = lines.size() >= NoteData.MAX_LINES;
        ItemStack add = full
                ? Icons.of(Items.BARRIER, "§cList full", "§7Maximum " + NoteData.MAX_LINES + " lines")
                : Icons.of(Items.EMERALD, "§a§l+ Add line",
                        "§7" + lines.size() + " / " + NoteData.MAX_LINES + " used",
                        "§7Click → type a line", "§8Supports §f&§8 colour codes");
        m.set(47, add, (p, b, a) -> {
            if (full) return;
            AnvilPrompt.open(p, "New lore line", new ItemStack(Items.PAPER), "",
                    val -> { BlockNotesManager.addLine(id, val); resync(p); reopen(p, id); },
                    () -> reopen(p, id));
        });

        m.set(49, toggleIcon(note), (p, b, a) -> {
            if (lines.isEmpty()) return;                 // nothing to show/hide yet
            BlockNotesManager.setEnabled(id, !note.enabled());
            resync(p);
            reopen(p, id);
        });

        m.set(51, Icons.of(Items.ENDER_PEARL, "§b§lShare",
                        "§7Upload this lore to the vault and", "§7get a share code in chat.",
                        "§8Import it elsewhere with", "§8/cb note import <id> <code>"),
                (p, b, a) -> { p.closeHandledScreen(); NoteCommands.share(p, id); });

        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** The On/Off switch icon — reflects whether the lines currently show on the item. */
    private static ItemStack toggleIcon(NoteData note) {
        if (note.lines().isEmpty())
            return Icons.of(Items.GRAY_DYE, "§8Off", "§7Add a line first.");
        return note.enabled()
                ? Icons.glint(Items.LIME_DYE, "§a§lOn", "§7Lines show on the item.", "§7Click to turn §cOff")
                : Icons.of(Items.GRAY_DYE, "§c§lOff", "§7Lines are hidden (still saved).", "§7Click to turn §aOn");
    }

    /** Reopen the menu after an anvil prompt or a mutation (the chest may have been closed). */
    private static void reopen(ServerPlayerEntity p, String id) {
        GuiRouter.render(p, MenuKey.of(Dest.NOTES, id));
    }

    /** Broadcast the changed lore so the item hover updates live for EVERY player (was actor-only). */
    private static void resync(ServerPlayerEntity p) {
        HudSync.broadcast(p.getServer()); // NO-REJOIN: lore change shows live for all players
    }
}
