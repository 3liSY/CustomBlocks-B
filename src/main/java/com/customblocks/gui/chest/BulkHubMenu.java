/**
 * BulkHubMenu.java — the Bulk Operations hub (Group 07 finale).
 *
 * The front door for bulk work: one tile per operation, each colour-coded by op (blue edit ·
 * red delete · yellow rename · cyan category · white duplicate · gold export · green lock ·
 * magenta favorite — see docs/GUI_DESIGN_GUIDE.md §1). Clicking a tile sets BulkSession.op,
 * clears any old selection, and opens the op's two-step builder (Step 1 choose blocks → Step 2
 * options), so Back returns here. A glint marks the op currently remembered.
 *
 * Opened by: /cb bulkgui, /cb bulkhub, MainMenu's Bulk tile.
 */
package com.customblocks.gui.chest;

import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BulkHubMenu {

    private BulkHubMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        BulkSession s = BulkSession.get(player.getUuid());
        ChestMenu m = new ChestMenu("§3Bulk Operations", 6).fill()
                .frame(Icons.accent(), Icons.of(Items.BLUE_STAINED_GLASS_PANE, " "));

        if (s.prePicked) {
            int picked = ListSelection.size(player.getUuid());
            m.set(4, Icons.glint(Items.COMMAND_BLOCK, "§b§lBulk Operations",
                    "§aActing on your §e" + picked + " §apicked block(s).",
                    "§8Pick an action → confirm. No re-choosing blocks.",
                    "§7Back returns to your list."));
        } else {
            m.set(4, Icons.glint(Items.COMMAND_BLOCK, "§b§lBulk Operations",
                    "§7Do one thing to many blocks at once.",
                    "§7Blocks on the server: §f" + SlotManager.usedSlots() + "§7/§f" + SlotManager.getMaxSlots(),
                    "§8Pick an op → choose blocks → confirm."));
        }

        // Two tidy rows of operation tiles. Each opens that op's two-step builder.
        opTile(m, 20, s, "property",  Items.COMPARATOR, "§b§lEdit a setting",
                "§7Change glow, hardness, sound or collision.");
        opTile(m, 21, s, "rename",    Items.NAME_TAG, "§e§lRename",
                "§7Add a prefix/suffix, or find-and-replace.");
        opTile(m, 22, s, "category",  Items.CHEST, "§3§lMove to category",
                "§7Re-file matched blocks under a category.");
        opTile(m, 23, s, "duplicate", Items.BOOK, "§f§lDuplicate",
                "§7Copy matched blocks (texture + settings).");
        opTile(m, 29, s, "export",    Items.MAP, "§6§lExport",
                "§7Write matched blocks to a file (7 formats).");
        opTile(m, 30, s, "lock",      Items.TRIPWIRE_HOOK, "§a§lLock / Unlock",
                "§7Protect blocks from edits, or release them.");
        opTile(m, 31, s, "favorite",  Items.NETHER_STAR, "§d§lFavorite",
                "§7Bookmark blocks for yourself (per-player).");
        opTile(m, 32, s, "delete",    Items.TNT, "§c§lDelete",
                "§7Permanently remove matched blocks. §8/cb undo restores.");

        m.set(45, Icons.back(), (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    /** Operations that need an options screen first (the rest go straight to the confirm). */
    private static boolean isParamOp(String op) {
        return "property".equals(op) || "rename".equals(op) || "category".equals(op) || "export".equals(op);
    }

    /** One operation tile — glints if it's the remembered op; opens the op's next step.
     *  Pre-picked (from /cb listgui): skip Step 1 → options (param ops) or straight to confirm. */
    private static void opTile(ChestMenu m, int slot, BulkSession s, String op, Item icon,
                               String name, String desc) {
        boolean current = op.equals(s.op);
        String hint = s.prePicked ? "§eClick §7→ confirm on your picks"
                : (current ? "§a✔ last used — §eclick to start" : "§eClick §7to start");
        ItemStack stack = current
                ? Icons.glint(icon, name, "§7" + desc, "", hint)
                : Icons.of(icon, name, "§7" + desc, "", hint);
        m.set(slot, stack, (p, b, a) -> { GuiFx.select(p);
                BulkSession ss = BulkSession.get(p.getUuid());
                ss.op = op;
                if (ss.prePicked) {
                    // Keep the hand-picked selection (selMode "picked") — skip Step 1.
                    GuiRouter.navigate(p, MenuKey.of(isParamOp(op) ? Dest.BULK_ACTION : Dest.BULK_CONFIRM));
                } else {
                    ss.resetSelection();
                    GuiRouter.navigate(p, MenuKey.of(Dest.BULK_SELECT));
                } });
    }
}
