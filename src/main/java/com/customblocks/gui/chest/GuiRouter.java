/**
 * GuiRouter.java
 *
 * Central entry point for opening chest menus and walking the navigation back-stack.
 * All menu opening is deferred to the server thread (server.execute) so it never runs
 * re-entrantly inside a slot-click handler. Mutating actions delegate to the existing,
 * tested /cb commands via runCommand / runAndReopen rather than re-implementing logic.
 */
package com.customblocks.gui.chest;

import com.customblocks.command.Chat;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

public final class GuiRouter {

    private GuiRouter() {} // static-only

    /** Open the dashboard, starting a fresh navigation session. */
    public static void openRoot(ServerPlayerEntity player) {
        Nav.reset(player.getUuid(), MenuKey.of(Dest.MAIN));
        render(player, MenuKey.of(Dest.MAIN));
    }

    /** Open a menu directly as a fresh single-entry session. */
    public static void openFresh(ServerPlayerEntity player, MenuKey key) {
        Nav.reset(player.getUuid(), key);
        render(player, key);
    }

    /** Navigate deeper: push the destination onto the stack and open it. */
    public static void navigate(ServerPlayerEntity player, MenuKey key) {
        Nav.push(player.getUuid(), key);
        render(player, key);
    }

    /** Change only the current menu (e.g. a new page) without growing the stack. */
    public static void repage(ServerPlayerEntity player, MenuKey key) {
        Nav.replaceTop(player.getUuid(), key);
        render(player, key);
    }

    /** Go back one level. A menu opened directly (e.g. /cb bulkgui) has nothing beneath —
     *  Back then goes home to the Main Menu instead of closing; the ✖ Close button closes. */
    public static void back(ServerPlayerEntity player) {
        MenuKey prev = Nav.popToPrevious(player.getUuid());
        if (prev == null) {
            Nav.reset(player.getUuid(), MenuKey.of(Dest.MAIN));
            render(player, MenuKey.of(Dest.MAIN));
            return;
        }
        render(player, prev);
    }

    /** Build the menu for a key and show it on the server thread (in place when possible). */
    public static void render(ServerPlayerEntity player, MenuKey key) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> show(player, build(player, key)));
    }

    /**
     * Show a freshly built menu. If the player already has one of our chest menus open with
     * the SAME size and title (e.g. a toggle or page change within the same screen), refresh
     * it in place so the mouse cursor does not snap back to the centre. Otherwise open it as
     * a new screen (navigating to a different menu).
     */
    private static void show(ServerPlayerEntity player, ChestMenu menu) {
        if (menu == null) return;
        if (player.currentScreenHandler instanceof CbChestHandler h
                && !h.isDisposed()
                && h.menuRows() == menu.rows()
                && h.menuTitle().equals(menu.title())) {
            h.refreshWith(menu);
        } else {
            menu.open(player);
        }
    }

    /** Close the chest, then run a /cb subcommand as the player (for non-chest features). */
    public static void runCommand(ServerPlayerEntity player, String sub) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            s.getCommandManager().executeWithPrefix(player.getCommandSource(), "cb " + sub);
        });
    }

    /** Run a /cb subcommand as the player, then push+open a new menu (deeper navigation). */
    public static void runThenNavigate(ServerPlayerEntity player, String sub, MenuKey key) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            s.getCommandManager().executeWithPrefix(player.getCommandSource(), "cb " + sub);
            Nav.push(player.getUuid(), key);
            show(player, build(player, key));
        });
    }

    /** Run a /cb subcommand as the player, then rebuild and reopen the given menu in place. */
    public static void runAndReopen(ServerPlayerEntity player, String sub, MenuKey key) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            s.getCommandManager().executeWithPrefix(player.getCommandSource(), "cb " + sub);
            show(player, build(player, key));
        });
    }

    /** Close the chest and post a one-click chat line that fills the player's chat box with a command. */
    public static void typeInChat(ServerPlayerEntity player, String command) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            player.sendMessage(Text.literal(Chat.PREFIX + "§7Click to type: ")
                    .append(Text.literal("§e" + command).styled(st -> st
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("§7Puts it in your chat box — edit or press Enter to run"))))), false);
        });
    }

    /** Close the chest and send a clickable chat prompt that pre-fills a command (text input). */
    public static void promptCommand(ServerPlayerEntity player, String suggest, String label) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            player.sendMessage(Text.literal(Chat.PREFIX + "§fClick to continue: ")
                    .append(Text.literal("§e[" + label + "]").styled(st -> st
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(suggest))))), false);
        });
    }

    /** Close the chest and send a clickable chat confirmation that runs a command on click. */
    public static void confirmCommand(ServerPlayerEntity player, String run, String label) {
        MinecraftServer s = player.getServer();
        if (s == null) return;
        s.execute(() -> {
            player.closeHandledScreen();
            player.sendMessage(Text.literal(Chat.PREFIX + "§fConfirm: ")
                    .append(Text.literal(label).styled(st -> st
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, run))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(run))))), false);
        });
    }

    private static ChestMenu build(ServerPlayerEntity player, MenuKey key) {
        return switch (key.dest()) {
            case MAIN -> MainMenu.build(player);
            case BLOCK_LIST -> BlockListMenu.build(player, key.arg(), key.page());
            case EDITOR -> EditorMenu.build(player, key.arg());
            case REID -> ReIdMenu.build(player, key.page());
            case UNDO -> UndoMenu.build(player, false, key.page());
            case REDO -> UndoMenu.build(player, true, key.page());
            case HISTORY -> HistoryMenu.build(player, key.page());
            case MAGIC -> MagicMenu.build(player, false, key.page());
            case MAGIC_EDIT -> MagicMenu.build(player, true, key.page());
            case CONFIG -> ConfigMenu.build(player);
            case TEXTURE_SIZE -> TextureSizeMenu.build(player);
            case RETEXTURE_CONFIRM -> RetextureConfirmMenu.build(player, key.arg());
            case RECOLOR_CONFIRM -> RecolorConfirmMenu.build(player, key.arg());
            case HEX_RECOLOR_CONFIRM -> HexRecolorConfirmMenu.build(player, key.arg());
            case HEX_COLORS -> HexColorsMenu.build(player);
            case CUSTOM_COLOR -> CustomColorMenu.build(player);
            case DIAG -> DiagMenu.build(player);
            case SEARCH -> SearchMenu.build(player, key.arg(), key.page());
            case HELP -> HelpMenu.build(player, key.arg(), key.page());
            case OMNI -> OmniMenu.build(player);
            case BULK_HUB -> BulkHubMenu.build(player);
            case BULK_CONFIRM -> BulkConfirmMenu.build(player);
            case BULK_SELECT -> BulkSelectMenu.build(player);
            case BULK_ACTION -> BulkActionMenu.build(player);
            case SHAPE_EDITOR -> ShapeEditorMenu.build(player, key.arg());
            case FACE_EDITOR -> FaceEditorMenu.build(player, key.arg());
            case BACKUP_LIST -> BackupMenu.build(player, key.page());
            case BACKUP_CONFIRM -> BackupConfirmMenu.build(player, key.arg());
            case CONFIG_CONFIRM -> ConfigWarnMenu.build(player);
            case TRASH_LIST -> TrashMenu.build(player, key.page());
            case TRASH_ENTRY -> TrashEntryMenu.build(player, key.arg());
            case BROKEN_LIST -> BrokenBlocksMenu.build(player, key.page());
            case BROKEN_CONFIRM -> BrokenConfirmMenu.build(player, key.arg());
            case SAFETY -> SafetyMenu.build(player);
            case BGSTUDIO -> BgStudioMenu.build(player, key.arg());
            case COLOR_VARIANTS -> ColorVariantsMenu.build(player, key.arg());
            case COLORS -> ColorsMenu.build(player);
            case PALETTE_LIST -> PaletteMenu.build(player, key.page());
            case GRADIENT_PICKER -> GradientPickerMenu.build(player);
            case COLOR_PICK -> ColorPickBlockMenu.build(player, key.arg(), key.page());
            case CATEGORY_LIST -> CategoryListMenu.build(player, key.page());
            case CATEGORY_BROWSE -> CategoryBrowserMenu.build(player, key.arg(), key.page());
            case CATEGORY_BLOCK -> CategoryBlockMenu.build(player, key.arg());
            case CATEGORY_EDIT -> CategoryEditMenu.build(player, key.arg());
            case CATEGORY_DELETE_CONFIRM -> CategoryEditMenu.buildDeleteConfirm(player, key.arg());
            case EXPORT_DASHBOARD -> ExportDashboardMenu.build(player, key.arg(), key.page());
            case ARABIC -> ArabicHubMenu.build(player);
            case ARABIC_LIST -> ArabicListMenu.build(player);
            case ARABIC_GROUP -> ArabicGroupMenu.build(player, key.arg());
            case ARABIC_CHOICE -> WordChoiceMenu.build(player);
            case ARABIC_COLOR -> ColorStudioMenu.build(player, key.arg());
            case ANIM_LIST -> AnimListMenu.build(player, key.page());
        };
    }
}
