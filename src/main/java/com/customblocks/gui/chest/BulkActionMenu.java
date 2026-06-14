/**
 * BulkActionMenu.java — Step 2 of every two-step bulk op: review, set the op's controls, confirm.
 *
 * One screen, op-driven (BulkSession.op). The Step-1 selection supplies the scope; this screen adds
 * only the op-specific controls and the apply button:
 *   🔍 Review — the exact blocks the selection resolves to (click to refresh).
 *   Controls — edit: setting + value · rename: mode + text(s) · category: target · export: format ·
 *              lock/favorite: direction · delete/duplicate: none.
 *   ✔ Apply  — delegates to the SAME tested command paths the chat commands use (confirm-guard +
 *              batch-undo live there), passing the Step-1 scope expression as the filter.
 *
 * Carries no mutation logic of its own. Back returns to Step 1.
 *
 * Reached from: BulkSelectMenu's Next → tile.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.handlers.BulkCategoryCommands;
import com.customblocks.command.handlers.BulkCommands;
import com.customblocks.command.handlers.BulkDuplicateCommands;
import com.customblocks.command.handlers.BulkExportCommands;
import com.customblocks.command.handlers.BulkFlagCommands;
import com.customblocks.core.BulkScope;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BulkActionMenu {

    private BulkActionMenu() {} // static-only

    public static ChestMenu build(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BulkSession s = BulkSession.get(uuid);
        String op = s.op;

        String expr = s.selScopeExpr(uuid);
        boolean hasSel = s.selHasSelection(uuid) && !expr.isBlank();
        List<SlotData> matches = hasSel ? BulkScope.resolve(expr, uuid) : List.of();
        int count = matches.size();

        boolean delete    = "delete".equals(op);
        boolean rename    = "rename".equals(op);
        boolean category  = "category".equals(op);
        boolean duplicate = "duplicate".equals(op);
        boolean export    = "export".equals(op);
        boolean lock      = "lock".equals(op);
        boolean favorite  = "favorite".equals(op);
        boolean flag      = lock || favorite;
        boolean edit      = !delete && !rename && !category && !duplicate && !export && !flag;

        ChestMenu m = new ChestMenu(BulkStyle.titleColor(op) + "Bulk " + BulkSession.prettyOp(op)
                        + " — Step 2: Options", 6).fill()
                .frame(Icons.of(BulkStyle.framePane(op), " "), Icons.of(BulkStyle.frameCorner(op), " "));

        m.set(4, Icons.glint(BulkStyle.opIcon(op), BulkStyle.headerColor(op) + "§l"
                        + BulkSession.prettyOp(op) + " · Step 2 — Options",
                "§7Review, set the details, confirm.",
                "§7Selected: §f" + s.selLabel(uuid) + " §8(" + count + ")"));

        // 🔍 Review — the exact blocks.
        m.set(20, review(s, uuid, matches),
                (p, b, a) -> { GuiFx.click(p); GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });

        // » connectors (drawn when there are controls to the right of Review)
        ItemStack link = Icons.of(Items.LIGHT_GRAY_STAINED_GLASS_PANE, "§8»");
        if (!delete && !duplicate) m.set(21, link);

        // ── op-specific controls ──
        if (edit) {
            m.set(22, chooser(Items.COMPARATOR, "§a§lSetting", BulkSession.PROPERTIES, s.property,
                            "§7What to change on each block."),
                    (p, b, a) -> { GuiFx.click(p);
                            BulkSession.get(p.getUuid()).cycleProperty(b == 1 ? -1 : 1);
                            GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });
            m.set(23, link);
            m.set(24, chooser(valueIcon(s.property), "§d§lValue", s.valuesForProperty(), s.value,
                            "§7The new §f" + s.property + "§7 value."),
                    (p, b, a) -> { GuiFx.click(p);
                            BulkSession.get(p.getUuid()).cycleValue(b == 1 ? -1 : 1);
                            GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });
        } else if (rename) {
            boolean replace = "replace".equals(s.renameMode);
            m.set(22, chooser(Items.NAME_TAG, "§a§lMode", new String[]{"prefix", "suffix", "replace"}, s.renameMode,
                            "§7prefix = front · suffix = end · replace = swap"),
                    (p, b, a) -> { GuiFx.click(p);
                            BulkSession.get(p.getUuid()).cycleRenameMode(b == 1 ? -1 : 1);
                            GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });
            m.set(23, link);
            m.set(24, Icons.of(Items.WRITABLE_BOOK, "§d§l" + (replace ? "Find: " : "Text: ")
                            + (s.renameA.isEmpty() ? "§8(click to type)" : "§f" + s.renameA),
                            "§7" + (replace ? "Text to find in each name." : "Text to add to each name."),
                            "", "§eClick §7to type it"),
                    (p, b, a) -> { GuiFx.click(p); openText(p, replace ? "Find text" : "Text to add", s.renameA, true); });
            if (replace) {
                m.set(29, Icons.of(Items.WRITABLE_BOOK, "§d§lReplace with: "
                                + (s.renameB.isEmpty() ? "§8(click to type)" : "§f" + s.renameB),
                                "§7Text to put in its place.", "", "§eClick §7to type it"),
                        (p, b, a) -> { GuiFx.click(p); openText(p, "Replace with", s.renameB, false); });
            }
        } else if (category) {
            String existing = String.join(", ", SlotManager.categories());
            if (existing.length() > 40) existing = existing.substring(0, 40) + "…";
            m.set(22, Icons.of(Items.CHEST, "§3§lCategory: "
                            + (s.category.isEmpty() ? "§8(click to type)" : "§f" + s.category),
                            "§7Move matched blocks into this category.",
                            existing.isEmpty() ? "§8No categories yet." : "§8Existing: " + existing,
                            "§8Type §fnone§8 to clear the category.",
                            "", "§eClick §7to type it"),
                    (p, b, a) -> { GuiFx.click(p); openCategory(p, s.category); });
        } else if (export) {
            m.set(22, chooser(Items.WRITABLE_BOOK, "§6§lFormat — §f" + s.exportFormat,
                            BulkSession.EXPORT_FORMATS, s.exportFormat, "§7File type for the export."),
                    (p, b, a) -> { GuiFx.click(p);
                            BulkSession.get(p.getUuid()).cycleExportFormat(b == 1 ? -1 : 1);
                            GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });
        } else if (flag) {
            String[] dirs = lock ? new String[]{"lock", "unlock"} : new String[]{"favorite", "unfavorite"};
            m.set(22, chooser(lock ? Items.TRIPWIRE_HOOK : Items.NETHER_STAR, "§a§lDirection", dirs, s.flagCommandOp(),
                            lock ? "§7lock = protect · unlock = release"
                                 : "§7favorite = bookmark · unfavorite = remove"),
                    (p, b, a) -> { GuiFx.click(p);
                            BulkSession.get(p.getUuid()).toggleFlag();
                            GuiRouter.repage(p, MenuKey.of(Dest.BULK_ACTION)); });
        }

        // ── apply button ──
        m.set(40, applyTile(s, op, count, delete, rename, category, duplicate, export, flag, lock),
                applyClick(op, count, s));

        m.set(45, Icons.back(), (p, b, a) -> { GuiFx.click(p); GuiRouter.back(p); });
        m.set(53, Icons.close(), (p, b, a) -> p.closeHandledScreen());
        return m;
    }

    // ── apply tile + handler ──────────────────────────────────────────────────

    private static ItemStack applyTile(BulkSession s, String op, int count, boolean delete, boolean rename,
                                       boolean category, boolean duplicate, boolean export, boolean flag, boolean lock) {
        if (count == 0) return Icons.of(Items.GRAY_DYE, "§8Nothing to do", "§7Go back and pick blocks.");
        if (rename) {
            boolean replace = "replace".equals(s.renameMode);
            boolean ready = !s.renameA.isEmpty() && (!replace || !s.renameB.isEmpty());
            if (!ready) return Icons.of(Items.GRAY_DYE, "§8Type the text first",
                    "§7Set the " + (replace ? "find + replace" : s.renameMode) + " text above.");
            return Icons.glint(Items.LIME_DYE, "§a§l✔ Rename " + count + " block(s)",
                    "§7" + renameDesc(s) + " on §e" + count + " §7block(s).",
                    "§8Large batches confirm in chat.");
        }
        if (category) {
            if (s.category.isEmpty()) return Icons.of(Items.GRAY_DYE, "§8Pick a category first",
                    "§7Click the Category tile (or type 'none' to clear).");
            boolean clear = "none".equalsIgnoreCase(s.category);
            return Icons.glint(Items.LIME_DYE, "§a§l✔ Move " + count + " block(s)",
                    "§7" + (clear ? "Clear the category of" : "Move to §b" + s.category + "§7,")
                            + " §e" + count + " §7block(s).",
                    "§8Large batches confirm in chat.");
        }
        if (delete) return Icons.glint(Items.TNT, "§4§l⚠ Delete " + count + " block(s)",
                "§cPermanently delete §e" + count + " §cblock(s).",
                "§7Locked blocks are skipped. §8/cb undo restores them.",
                "§8Large batches confirm in chat.");
        if (duplicate) return Icons.glint(Items.BOOK, "§a§l✔ Duplicate " + count + " block(s)",
                "§7Make a textured copy of §e" + count + " §7block(s).",
                "§8Copies are named <id>_copy. §8/cb undo removes them.");
        if (export) return Icons.glint(Items.LIME_CONCRETE, "§a§l✔ Export " + count + " block(s)",
                "§7Write §e" + count + " §7block(s) as §6." + s.exportFormat + "§7.",
                "§8Saved in config/customblocks/exports/. Read-only — no undo needed.");
        if (flag) {
            String cmdOp = s.flagCommandOp();
            return Icons.glint(lock ? Items.TRIPWIRE_HOOK : Items.NETHER_STAR,
                    "§a§l✔ " + capitalize(cmdOp) + " " + count + " block(s)",
                    "§7" + cmdOp + " §e" + count + " §7block(s).",
                    "§8Self-inverse — the opposite is the undo. A §f[undo]§8 appears in chat.");
        }
        return Icons.glint(Items.LIME_DYE, "§a§l✔ Apply to " + count + " block(s)",
                "§7Set §b" + s.property + "§7=§b" + s.value + "§7 on §e" + count + " §7block(s).",
                "§8Large batches confirm in chat.");
    }

    private static ChestMenu.Click applyClick(String op, int count, BulkSession s) {
        return (p, b, a) -> {
            BulkSession ss = BulkSession.get(p.getUuid());
            String scope = ss.selScopeExpr(p.getUuid());
            if (count == 0 || scope.isBlank()) { GuiFx.deny(p); return; }
            switch (op) {
                case "delete" -> { GuiFx.danger(p); BulkCommands.applyDeleteFromGui(p, scope); }
                case "rename" -> {
                    boolean replace = "replace".equals(ss.renameMode);
                    if (ss.renameA.isEmpty() || (replace && ss.renameB.isEmpty())) { GuiFx.deny(p); return; }
                    GuiFx.apply(p);
                    BulkCommands.applyRenameFromGui(p, scope, ss.renameMode, ss.renameA, ss.renameB);
                }
                case "category" -> {
                    if (ss.category.isEmpty()) { GuiFx.deny(p); return; }
                    GuiFx.apply(p);
                    BulkCategoryCommands.applyCategoryFromGui(p, scope, ss.category);
                }
                case "duplicate" -> { GuiFx.apply(p); BulkDuplicateCommands.applyDuplicateFromGui(p, scope); }
                case "export"    -> { GuiFx.apply(p); BulkExportCommands.applyExportFromGui(p, scope, ss.exportFormat); }
                case "lock", "favorite" -> { GuiFx.apply(p); BulkFlagCommands.applyFlagFromGui(p, ss.flagCommandOp(), scope); }
                default -> { GuiFx.apply(p); BulkCommands.applyFromGui(p, scope, ss.property, ss.value); }
            }
        };
    }

    // ── tiles / helpers ───────────────────────────────────────────────────────

    /** The Review tile: the exact ids the selection resolves to (first 8, then a count). */
    private static ItemStack review(BulkSession s, UUID uuid, List<SlotData> matches) {
        List<String> lore = new ArrayList<>();
        lore.add("§7The exact blocks this will touch.");
        lore.add("§7From: §f" + s.selLabel(uuid));
        lore.add("");
        if (matches.isEmpty()) {
            lore.add("§8(none — go back and pick)");
        } else {
            int shown = Math.min(8, matches.size());
            for (int i = 0; i < shown; i++) lore.add("§8• §f" + matches.get(i).customId());
            if (matches.size() > shown) lore.add("§8…and " + (matches.size() - shown) + " more");
        }
        lore.add("");
        lore.add("§eClick §7to refresh");
        return Icons.of(Items.SPYGLASS, "§b§l🔍 Review — §e" + matches.size() + " §b§lblock(s)",
                lore.toArray(new String[0]));
    }

    /** A radio-style chooser tile: each option on its own line, the current one highlighted. */
    private static ItemStack chooser(Item icon, String name, String[] opts, String current, String... head) {
        List<String> lore = new ArrayList<>();
        for (String h : head) lore.add(h);
        if (head.length > 0) lore.add("");
        if (opts.length <= 7) {
            for (String o : opts) lore.add(o.equalsIgnoreCase(current) ? "§e▸ §f§l" + o : "§8• " + o);
        } else {
            int at = 0;
            for (int i = 0; i < opts.length; i++) if (opts[i].equalsIgnoreCase(current)) at = i;
            String prev = opts[(at - 1 + opts.length) % opts.length];
            String next = opts[(at + 1) % opts.length];
            lore.add("§8" + prev + " §7← §e▸ §f§l" + current + " §7→ §8" + next);
            lore.add("§8choice " + (at + 1) + " of " + opts.length);
        }
        lore.add("");
        lore.add("§eLeft§7/§eright-click §7to cycle");
        return Icons.of(icon, name, lore.toArray(new String[0]));
    }

    private static Item valueIcon(String property) {
        return switch (property) {
            case "glow"      -> Items.GLOWSTONE;
            case "hardness"  -> Items.IRON_PICKAXE;
            case "sound"     -> Items.NOTE_BLOCK;
            case "collision" -> Items.IRON_BARS;
            default          -> Items.PAPER;
        };
    }

    private static String renameDesc(BulkSession s) {
        return switch (s.renameMode) {
            case "prefix" -> "add prefix \"" + s.renameA + "\"";
            case "suffix" -> "add suffix \"" + s.renameA + "\"";
            default       -> "replace \"" + s.renameA + "\" → \"" + s.renameB + "\"";
        };
    }

    private static String capitalize(String v) {
        return v.isEmpty() ? v : Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    /** Anvil for a rename text; stores it and re-shows Step 2 (keeps the Step1→Step2 stack). */
    private static void openText(ServerPlayerEntity player, String title, String current, boolean isFindOrText) {
        AnvilPrompt.open(player, title, new ItemStack(Items.NAME_TAG), current,
                text -> {
                    BulkSession s = BulkSession.get(player.getUuid());
                    if (isFindOrText) s.renameA = text == null ? "" : text;
                    else s.renameB = text == null ? "" : text;
                    GuiRouter.render(player, MenuKey.of(Dest.BULK_ACTION));
                },
                () -> GuiRouter.render(player, MenuKey.of(Dest.BULK_ACTION)));
    }

    /** Anvil for the target category; stores it and re-shows Step 2. */
    private static void openCategory(ServerPlayerEntity player, String current) {
        AnvilPrompt.open(player, "Category (none = clear)", new ItemStack(Items.CHEST), current,
                text -> {
                    BulkSession.get(player.getUuid()).category = text == null ? "" : text.trim();
                    GuiRouter.render(player, MenuKey.of(Dest.BULK_ACTION));
                },
                () -> GuiRouter.render(player, MenuKey.of(Dest.BULK_ACTION)));
    }
}
