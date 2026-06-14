/**
 * ImageToolCommands.java — Group 10 (Coloring), background + colour-hub entry points.
 *
 *   /cb coloring                 — open the Coloring hub (palette, background studio, gradient,
 *                                  custom colour, variants, live recolour, eyedrop). /cb colors = alias.
 *   /cb bgstudio                 — pick a block, then open its Background Studio.
 *   /cb bgstudio <id>            — open the Background Studio for <id> directly.
 *   /cb tolerance <value>        — set the GLOBAL default background-removal strength (0-100).
 *   /cb tolerance <value> <id>   — set just <id>'s strength (persisted per-block; global untouched)
 *                                  and re-apply its current mode now.
 *   /cb livecolor <id>           — open the live recolour slider (client screen) for <id>.
 *   /cb eyedrop                  — open the screen eyedrop (client screen) to pick a colour.
 *
 * Per-block strengths live in BlockToleranceStore; the global default in CustomBlocksConfig. The two
 * client screens are reached by sending an OpenGuiPayload; everything else delegates to the tested
 * GUIs / ColorToolService. Separate handler from ColorImageCommands so each stays under the 400-line
 * gate (§9.3).
 *
 * Depends on: ColorToolService, BgStudioSession, BlockToleranceStore, CustomBlocksConfig, SlotManager,
 *             SlotData, TextureStore, GuiRouter/Nav, GuiMode, OpenGuiPayload, ResourcePackServer,
 *             ServerPlayNetworking, Chat, BlockSuggestions.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockToleranceStore;
import com.customblocks.core.ColorToolService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.gui.GuiMode;
import com.customblocks.gui.chest.BgStudioSession;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ImageToolCommands {

    private ImageToolCommands() {} // static-only

    /** Delimiter between the block id and the texture URL in the RECOLOR_SLIDER payload data. */
    public static final String DATA_SEP = "|";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb bgstudio [id] — no id opens a block picker; an id goes straight to the studio.
        root.then(CommandManager.literal("bgstudio")
                .executes(ImageToolCommands::bgstudioPick)
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> bgstudio(ctx, StringArgumentType.getString(ctx, "id")))));

        // /cb tolerance <value> [id] — value first; with no id it sets the global default.
        root.then(CommandManager.literal("tolerance")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> toleranceGlobal(ctx, IntegerArgumentType.getInteger(ctx, "value")))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> tolerancePerBlock(ctx,
                                        IntegerArgumentType.getInteger(ctx, "value"),
                                        StringArgumentType.getString(ctx, "id"))))));

        // /cb coloring — the hub; /cb colors stays as a familiar alias.
        root.then(CommandManager.literal("coloring").executes(ImageToolCommands::colors));
        root.then(CommandManager.literal("colors").executes(ImageToolCommands::colors));

        root.then(CommandManager.literal("livecolor")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> livecolor(ctx, StringArgumentType.getString(ctx, "id")))));

        root.then(CommandManager.literal("eyedrop").executes(ImageToolCommands::eyedrop));
    }

    private static ServerPlayerEntity player(CommandContext<ServerCommandSource> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
    }

    // ── /cb bgstudio ───────────────────────────────────────────────────────────

    private static int bgstudioPick(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can open the Background Studio."); return 0; }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.COLOR_PICK, "bgstudio"));
        return 1;
    }

    private static int bgstudio(CommandContext<ServerCommandSource> ctx, String id) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can open the Background Studio."); return 0; }
        if (SlotManager.getById(id) == null) {
            Chat.error(ctx.getSource(), "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.BGSTUDIO, id));
        return 1;
    }

    // ── /cb tolerance ──────────────────────────────────────────────────────────

    /** No id: set the server-wide default strength used by future background removals. */
    private static int toleranceGlobal(CommandContext<ServerCommandSource> ctx, int value) {
        CustomBlocksConfig.backgroundTolerance = Math.max(0, Math.min(100, value));
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), "Global background tolerance set to §e" + CustomBlocksConfig.backgroundTolerance
                + "%§r. §7Used by new removals and any block without its own setting.");
        return 1;
    }

    /** With an id: store the override for that block only (global stays put) and re-apply it now. */
    private static int tolerancePerBlock(CommandContext<ServerCommandSource> ctx, int value, String id) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can set a block's tolerance."); return 0; }
        if (SlotManager.getById(id) == null) {
            Chat.error(ctx.getSource(), "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        int v = Math.max(0, Math.min(100, value));
        BlockToleranceStore.set(id, v);
        BgStudioSession.setTol(p.getUuid(), id, v);
        BgStudioSession.State s = BgStudioSession.get(p.getUuid(), id);
        Chat.success(ctx.getSource(), "Set \"" + id + "\" tolerance to §e" + v + "%§r §7(global default stays "
                + CustomBlocksConfig.backgroundTolerance + "%). Applying…");
        ColorToolService.applyBgRemoval(p, id, s.mode, v);
        return 1;
    }

    // ── /cb coloring (alias /cb colors) ────────────────────────────────────────

    private static int colors(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can open the Coloring hub."); return 0; }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.COLORS));
        return 1;
    }

    // ── /cb livecolor + /cb eyedrop (client screens) ───────────────────────────

    private static int livecolor(CommandContext<ServerCommandSource> ctx, String id) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can open the live recolour slider."); return 0; }
        SlotData d = SlotManager.getById(id);
        if (d == null) {
            Chat.error(ctx.getSource(), "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        byte[] tex = TextureStore.load(d.index());
        if (tex == null || tex.length == 0) {
            Chat.error(ctx.getSource(), "\"" + id + "\" has no texture to recolour yet.");
            return 0;
        }
        // data = "<id>|<texture url>" so the client can fetch the current pixels to preview.
        String data = id + DATA_SEP + ResourcePackServer.getTexUrl(id);
        ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.RECOLOR_SLIDER.id, data));
        return 1;
    }

    private static int eyedrop(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = player(ctx);
        if (p == null) { Chat.error(ctx.getSource(), "Only a player can use the eyedrop."); return 0; }
        ServerPlayNetworking.send(p, new OpenGuiPayload(GuiMode.EYEDROP.id, ""));
        return 1;
    }
}
