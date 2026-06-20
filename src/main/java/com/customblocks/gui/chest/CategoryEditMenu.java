/**
 * CategoryEditMenu.java — advanced category editor (Group 11 overhaul).
 *
 * Opened by right-clicking a category in CategoryListMenu. A framed 6-row panel giving
 * full control over a category: display block, rename, merge, lock/unlock all, bulk retexture,
 * stats, delete, color tag, description, sort order, and a placeholder Share tile.
 *
 * All mutating operations delegate to tested commands or direct manager calls — no block
 * logic duplicated. The menu is entirely server-side (vanilla chest GUI, no client mod).
 *
 * Depends on: SlotManager, SlotData, SlotBlock, LockManager, TextureStore,
 *             CategoryMetadataStore, CategoryDisplayBlockManager,
 *             Icons, GuiFx, GuiRouter, Nav, BlockExporter
 * Called by:  GuiRouter (Dest.CATEGORY_EDIT), CategoryListMenu (right-click)
 */
package com.customblocks.gui.chest;

import com.customblocks.block.SlotBlock;
import com.customblocks.core.CategoryMetadataStore;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CategoryEditMenu {

    private CategoryEditMenu() {} // static-only

    /** Available §-colour codes the player can cycle through for the color tag. */
    private static final String[] COLOR_TAGS = {
            "",    // default (white)
            "§a",  // green
            "§b",  // aqua
            "§c",  // red
            "§d",  // light purple
            "§e",  // yellow
            "§6",  // gold
            "§9",  // blue
            "§3",  // dark aqua
            "§5",  // dark purple
            "§2",  // dark green
            "§4",  // dark red
            "§8",  // dark gray
            "§f",  // white (explicit)
    };
    private static final String[] COLOR_NAMES = {
            "Default (white)", "Green", "Aqua", "Red", "Light Purple", "Yellow",
            "Gold", "Blue", "Dark Aqua", "Dark Purple", "Dark Green", "Dark Red",
            "Dark Gray", "White",
    };

    public static ChestMenu build(ServerPlayerEntity player, String category) {
        String cat = category == null ? "" : category.trim();
        List<SlotData> blocks = new ArrayList<>(SlotManager.byCategory(cat));
        blocks.sort(Comparator.comparing(d -> d.displayName().toLowerCase()));

        String colorTag = CategoryMetadataStore.getColorTag(cat);
        String desc = CategoryMetadataStore.getDescription(cat);
        String sortOrder = CategoryMetadataStore.getSortOrder(cat);
        String displayBlock = CategoryMetadataStore.getDisplayBlock(cat);

        // Compute stats
        int total = blocks.size();
        int locked = 0;
        long totalTexSize = 0;
        for (SlotData d : blocks) {
            if (LockManager.isLocked(d.customId())) locked++;
            byte[] tex = TextureStore.load(d.index());
            if (tex != null) totalTexSize += tex.length;
        }
        int unlocked = total - locked;

        // Title uses the color tag
        String titleColor = colorTag.isEmpty() ? "§e" : colorTag;
        ChestMenu m = new ChestMenu("Edit: " + cat, 6);
        m.fill();

        // ── Row 0: Header ────────────────────────────────────────────────────
        m.set(4, Icons.glint(CategoryListMenu.iconFor(cat),
                titleColor + cat,
                "§7" + total + " block" + (total == 1 ? "" : "s"),
                desc.isEmpty() ? "§8No description set" : "§7" + desc));

        // ── Row 1: Main actions ──────────────────────────────────────────────

        // Display Block (slot 10)
        m.set(10, Icons.of(Items.ITEM_FRAME, "§6§lDisplay Block",
                        displayBlock == null ? "§7No icon set (using default)" : "§7Current: §f" + displayBlock,
                        "§a▸ Left-click §7to pick a block",
                        "§c▸ Right-click §7to clear"),
                (p, b, a) -> {
                    if (b == 1) { // right-click = clear
                        CategoryMetadataStore.clearDisplayBlock(cat);
                        GuiFx.click(p);
                        GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                    } else { // left-click = pick from blocks in this category (or all)
                        GuiFx.open(p);
                        GuiRouter.navigate(p, MenuKey.of(Dest.COLOR_PICK, "caticon:" + cat));
                    }
                });

        // Rename (slot 11) — anvil text input
        m.set(11, Icons.of(Items.NAME_TAG, "§e§lRename Category",
                        "§7Change the name of this category.",
                        "§7All blocks in it will be updated.",
                        "§8Opens an anvil to type"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    AnvilPrompt.open(p, "Rename category", new ItemStack(Items.NAME_TAG), cat,
                            text -> {
                                String nn = text.trim().toLowerCase(Locale.ROOT);
                                if (nn.isEmpty() || nn.equals(cat)) {
                                    GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                                    return;
                                }
                                GuiRouter.runAndReopen(p, "category rename " + cat + " " + nn,
                                        MenuKey.of(Dest.CATEGORY_EDIT, nn));
                            },
                            () -> GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat)));
                });

        // Merge Into (slot 12) — anvil text input
        m.set(12, Icons.of(Items.HOPPER, "§d§lMerge Into…",
                        "§7Move all §f" + total + "§7 block(s) into",
                        "§7another category, then delete this one.",
                        "§8Opens an anvil to type"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    AnvilPrompt.open(p, "Merge into… (target category)", new ItemStack(Items.HOPPER), "",
                            text -> {
                                String target = text.trim().toLowerCase(Locale.ROOT);
                                if (target.isEmpty() || target.equals(cat)) {
                                    GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                                    return;
                                }
                                GuiRouter.runAndReopen(p, "category merge " + cat + " " + target,
                                        MenuKey.of(Dest.CATEGORY_LIST));
                            },
                            () -> GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat)));
                });

        // Export (slot 14)
        m.set(14, Icons.of(Items.WRITABLE_BOOK, "§b§lExport",
                        "§7ZIP all " + total + " block(s) in this",
                        "§7category to cloud_exports/."),
                (p, b, a) -> {
                    GuiFx.apply(p);
                    GuiRouter.runCommand(p, "category export " + cat);
                });

        // Share (slot 15) — greyed out / coming soon
        m.set(15, Icons.of(Items.ENDER_PEARL, "§8§lShare §8(coming soon)",
                        "§7Upload this category to the vault",
                        "§7and get a share code.",
                        "§8Vault Worker not deployed yet."),
                (p, b, a) -> GuiFx.deny(p));

        // Lock / Unlock All (slot 16)
        int fLocked = locked;
        int fUnlocked = unlocked;
        m.set(16, Icons.of(Items.TRIPWIRE_HOOK, "§6§lLock / Unlock All",
                        "§7" + fLocked + " locked, " + fUnlocked + " unlocked",
                        "§a▸ Left-click §7to lock all",
                        "§c▸ Right-click §7to unlock all"),
                (p, b, a) -> {
                    boolean doLock = (b != 1); // left = lock, right = unlock
                    for (SlotData d : blocks) {
                        if (doLock) LockManager.lock(d.customId());
                        else LockManager.unlock(d.customId());
                    }
                    GuiFx.apply(p);
                    GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                });

        // ── Row 2: More options ──────────────────────────────────────────────

        // Bulk Retexture (slot 19)
        m.set(19, Icons.of(Items.PAINTING, "§a§lBulk Retexture",
                        "§7Re-apply a URL to all blocks",
                        "§7in this category at once.",
                        "§8Opens a chat prompt"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    GuiRouter.promptCommand(p, "/cb bulkretexture category:" + cat + " ",
                            "paste image URL");
                });

        // Stats (slot 22) — display-only
        String texSizeStr;
        if (totalTexSize < 1024) texSizeStr = totalTexSize + " B";
        else if (totalTexSize < 1024 * 1024) texSizeStr = String.format("%.1f KB", totalTexSize / 1024.0);
        else texSizeStr = String.format("%.1f MB", totalTexSize / (1024.0 * 1024.0));
        m.set(22, Icons.of(Items.BOOK, "§f§lStats",
                "§7Blocks: §f" + total,
                "§7Locked: §f" + locked + " §7/ Unlocked: §f" + unlocked,
                "§7Total texture size: §f" + texSizeStr,
                "§7Sort: §f" + ("custom".equals(sortOrder) ? "Custom order" : "Alphabetical")));

        // Color Tag (slot 20)
        String curColorName = colorTagName(colorTag);
        m.set(20, Icons.of(Items.LIME_DYE, "§e§lColor Tag",
                        "§7Current: " + (colorTag.isEmpty() ? "§fDefault (white)" : colorTag + curColorName),
                        "§7Tints this category's name in the list.",
                        "§a▸ Left-click §7to cycle forward",
                        "§c▸ Right-click §7to cycle back"),
                (p, b, a) -> {
                    int dir = (b == 1) ? -1 : 1;
                    String next = cycleColorTag(colorTag, dir);
                    CategoryMetadataStore.setColorTag(cat, next);
                    GuiFx.click(p);
                    GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                });

        // Description (slot 21) — anvil text input
        m.set(21, Icons.of(Items.PAPER, "§e§lDescription",
                        desc.isEmpty() ? "§7No description set" : "§7\"" + desc + "\"",
                        "§8Opens an anvil to type one"),
                (p, b, a) -> {
                    GuiFx.click(p);
                    AnvilPrompt.open(p, "Category description", new ItemStack(Items.PAPER), desc,
                            text -> GuiRouter.runAndReopen(p, "category desc " + cat + " " + text,
                                    MenuKey.of(Dest.CATEGORY_EDIT, cat)),
                            () -> GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat)));
                });

        // Sort Order (slot 23)
        m.set(23, Icons.of(Items.COMPARATOR, "§e§lSort Order",
                        "§7Current: §f" + ("custom".equals(sortOrder) ? "Custom" : "Alphabetical"),
                        "§a▸ Click §7to toggle"),
                (p, b, a) -> {
                    String next = "alpha".equals(sortOrder) ? "custom" : "alpha";
                    CategoryMetadataStore.setSortOrder(cat, next);
                    GuiFx.click(p);
                    GuiRouter.render(p, MenuKey.of(Dest.CATEGORY_EDIT, cat));
                });

        // ── Row 4: Delete ────────────────────────────────────────────────────

        // Delete Category (slot 40)
        m.set(40, Icons.of(Items.TNT, "§c§lDelete Category",
                        "§7Removes the category label from all",
                        "§7" + total + " block(s). §cDoesn't delete blocks.",
                        "§8Click to confirm."),
                (p, b, a) -> {
                    GuiFx.danger(p);
                    GuiRouter.navigate(p, MenuKey.of(Dest.CATEGORY_DELETE_CONFIRM, cat));
                });

        // ── Footer ───────────────────────────────────────────────────────────
        for (int i = 45; i < 54; i++) m.set(i, Icons.filler());
        m.set(45, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());

        return m;
    }

    /** Build the delete-confirmation sub-menu. */
    public static ChestMenu buildDeleteConfirm(ServerPlayerEntity player, String category) {
        String cat = category == null ? "" : category.trim();
        int count = SlotManager.byCategory(cat).size();

        ChestMenu m = new ChestMenu("Delete category: " + cat + "?", 3).fill();
        m.set(4, Icons.of(Items.TNT, "§c§lDelete \"" + cat + "\"?",
                "§7This will uncategorize §f" + count + "§7 block(s).",
                "§7The blocks themselves are §fnot§7 deleted."));

        m.set(11, Icons.of(Items.LIME_WOOL, "§a§lYes, delete it",
                        "§7Uncategorize all blocks and remove this category."),
                (p, b, a) -> {
                    // Uncategorize all blocks
                    for (SlotData d : SlotManager.byCategory(cat)) {
                        SlotManager.setCategory(d.customId(), "");
                    }
                    // Clean up metadata
                    CategoryMetadataStore.deleteCategory(cat);
                    GuiFx.danger(p);
                    // Go back to the category list
                    MinecraftServer s = p.getServer();
                    if (s != null) {
                        s.execute(() -> {
                            com.customblocks.command.Chat.success(p.getCommandSource(),
                                    "Deleted category \"" + cat + "\" — " + count + " block(s) uncategorized.");
                            GuiRouter.openFresh(p, MenuKey.of(Dest.CATEGORY_LIST));
                        });
                    }
                });

        m.set(15, Icons.of(Items.RED_WOOL, "§c§lNo, go back"),
                (p, b, a) -> GuiRouter.back(p));

        m.set(18, Icons.back(), (p, b, a) -> GuiRouter.back(p));
        m.set(26, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── Color tag helpers ────────────────────────────────────────────────────

    private static String cycleColorTag(String current, int dir) {
        int idx = 0;
        for (int i = 0; i < COLOR_TAGS.length; i++) {
            if (COLOR_TAGS[i].equals(current)) { idx = i; break; }
        }
        int next = ((idx + dir) % COLOR_TAGS.length + COLOR_TAGS.length) % COLOR_TAGS.length;
        return COLOR_TAGS[next];
    }

    private static String colorTagName(String tag) {
        for (int i = 0; i < COLOR_TAGS.length; i++) {
            if (COLOR_TAGS[i].equals(tag)) return COLOR_NAMES[i];
        }
        return "Default";
    }
}
