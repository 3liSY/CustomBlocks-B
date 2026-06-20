/**
 * ChestGuiCommands.java
 *
 * Registers the server-side chest GUI commands (Group 02): the /cb dashboard, the block
 * list, the per-block editor, the visual undo/redo and history menus, the magic-items
 * menus, and the resource-pack control commands (pause/resume/sync/unsuppress). All chest
 * menus are opened through GuiRouter; nothing here re-implements block mutation logic.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.BulkSession;
import com.customblocks.gui.chest.Nav;
import com.customblocks.gui.chest.Nav.Dest;
import com.customblocks.gui.chest.Nav.MenuKey;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

public final class ChestGuiCommands {

    private ChestGuiCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // Bare /cb (and aliases) open the dashboard.
        root.executes(ChestGuiCommands::dashboard);
        root.then(CommandManager.literal("menu").executes(ChestGuiCommands::dashboard));
        root.then(CommandManager.literal("dashboard").executes(ChestGuiCommands::dashboard));
        root.then(CommandManager.literal("admingui").executes(ChestGuiCommands::dashboard));

        // /cb gui — alias for the dashboard.
        // (The old "/cb gui block <id>" path was removed; use "/cb editor <id>" for that.)
        root.then(CommandManager.literal("gui").executes(ChestGuiCommands::dashboard));

        // /cb editor <id>
        root.then(CommandManager.literal("editor")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> editor(ctx, StringArgumentType.getString(ctx, "id")))));

        // /cb shapeeditor moved to ShapeCommands (G27 §F2) — it now opens the 3D Shape Editor screen
        // (no-id → chest block-picker → screen; <id> → screen). Removing the old colliding literal here
        // fixes the "not a registered command" bug. The Group 08 chest ShapeEditorMenu (Dest.SHAPE_EDITOR)
        // is still reachable from EditorMenu.

        // /cb facechangegui <id> — Group 08 per-face texture GUI
        root.then(CommandManager.literal("facechangegui")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> openFor(ctx, Dest.FACE_EDITOR, StringArgumentType.getString(ctx, "id")))));

        // Direct menu entry points.
        root.then(CommandManager.literal("listgui").executes(open(Dest.BLOCK_LIST)));
        root.then(CommandManager.literal("blockslist").executes(open(Dest.BLOCK_LIST))); // clear alias for listgui
        root.then(CommandManager.literal("undogui").executes(open(Dest.UNDO)));
        root.then(CommandManager.literal("redogui").executes(open(Dest.REDO)));
        root.then(CommandManager.literal("history").executes(open(Dest.HISTORY)));
        root.then(CommandManager.literal("magicitems").executes(open(Dest.MAGIC)));
        root.then(CommandManager.literal("editmagicitems").executes(open(Dest.MAGIC_EDIT)));

        // Resource-pack controls.
        root.then(CommandManager.literal("rp")
                .then(CommandManager.literal("pause").executes(ChestGuiCommands::rpPause))
                .then(CommandManager.literal("resume").executes(ChestGuiCommands::rpResume)));
        root.then(CommandManager.literal("sync").executes(ChestGuiCommands::sync));
        root.then(CommandManager.literal("unsuppress").executes(ChestGuiCommands::unsuppress));
    }

    private static Command<ServerCommandSource> open(Dest dest) {
        return ctx -> {
            ServerCommandSource src = ctx.getSource();
            if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
                Chat.error(src, "Open this menu as a player");
                return 0;
            }
            // A plain /cb listgui is normal browsing — clear any abandoned pick-mode flags so the
            // confirm tile isn't stuck on "use these for bulk/export" from an earlier, closed flow.
            if (dest == Dest.BLOCK_LIST) {
                BulkSession s = BulkSession.get(p.getUuid());
                s.listPickForBulk = false;
                s.listPickForExport = false;
            }
            GuiRouter.openFresh(p, MenuKey.of(dest));
            return 1;
        };
    }

    /** Public entry used by the /cb alias so the bare command opens the dashboard. */
    public static int openDashboard(CommandContext<ServerCommandSource> ctx) {
        return dashboard(ctx);
    }

    private static int dashboard(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Open the menu as a player");
            return 0;
        }
        GuiRouter.openRoot(p);
        return 1;
    }

    private static int editor(CommandContext<ServerCommandSource> ctx, String id) {
        return openFor(ctx, Dest.EDITOR, id);
    }

    /** Open a per-block GUI (editor / shape editor / face editor) at {@code id}, with Back going home. */
    private static int openFor(CommandContext<ServerCommandSource> ctx, Dest dest, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Open this menu as a player");
            return 0;
        }
        if (SlotManager.getById(id) == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        Nav.reset(p.getUuid(), MenuKey.of(Dest.MAIN));
        GuiRouter.navigate(p, MenuKey.of(dest, id));
        return 1;
    }

    private static int rpPause(CommandContext<ServerCommandSource> ctx) {
        ResourcePackServer.pause();
        Chat.info(ctx.getSource(), "Resource-pack rebuilds paused. Block edits won't be pushed to clients until you run /cb rp resume.");
        return 1;
    }

    private static int rpResume(CommandContext<ServerCommandSource> ctx) {
        ResourcePackServer.resume();
        Chat.success(ctx.getSource(), "Resource-pack rebuilds resumed — pushing the latest pack to clients now.");
        return 1;
    }

    private static int sync(CommandContext<ServerCommandSource> ctx) {
        int n = ResourcePackServer.syncToAll();
        Chat.success(ctx.getSource(), "Force-syncing the resource pack to " + n + " client(s).");
        return 1;
    }

    private static int unsuppress(CommandContext<ServerCommandSource> ctx) {
        ResourcePackServer.unsuppress();
        Chat.success(ctx.getSource(),
                "Resource-pack prompt re-enabled. New joins (and /cb sync) will again show the server resource-pack download notification that a 'do not show again' choice had silenced.");
        return 1;
    }
}
