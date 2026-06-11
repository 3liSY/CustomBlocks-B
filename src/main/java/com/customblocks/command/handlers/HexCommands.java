/**
 * HexCommands.java
 *
 * Responsibility: The colour-variant hex commands (Group 06 / M3 hex), split out of
 * ConfigCommands to respect the 400-line handler limit (§9.3):
 *   /cb config hex                      — show all four configured hexes
 *   /cb config hex <colour>             — show one
 *   /cb config hex <colour> <#RRGGBB>   — set it: saves, re-tints the Square/Triangle item
 *                                         art (pack rebuild), and offers to repaint existing
 *                                         "*_<colour>" blocks via a Yes/Info/No confirm
 *   /cb recolorvariants <colour> <oldhex> — the repaint batch (the confirm's Yes runs this)
 *   /cb customcolor [colour]             — no arg: the Color Studio GUI (preset colours →
 *                                          magic tool pairs); with a hex/name: give directly
 *
 * Depends on: CustomBlocksConfig, ColorVariantService, ColorLibrary, CustomColorToolItem,
 *             ResourcePackServer, GuiRouter/Nav, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.ColorVariantService;
import com.customblocks.item.CustomColorToolItem;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;

public final class HexCommands {

    private HexCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb config hex [colour] [#RRGGBB] — Brigadier merges this "config" literal into the
        // one ConfigCommands registers, keeping the rest of /cb config intact.
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("hex")
                        .executes(HexCommands::hexStatus)
                        .then(CommandManager.argument("colour", StringArgumentType.word())
                                .suggests((c, b) -> { for (String k : ColorVariantService.COLOUR_KEYS) b.suggest(k); return b.buildFuture(); })
                                .executes(ctx -> hexStatusOne(ctx, StringArgumentType.getString(ctx, "colour")))
                                // greedyString (not word): word() rejects '#', so "#RRGGBB" hex
                                // input would never parse. Final arg → greedy is safe (BUG 1).
                                .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                        .executes(ctx -> setHex(ctx, StringArgumentType.getString(ctx, "colour"),
                                                StringArgumentType.getString(ctx, "value")))))));

        // /cb customcolor [colour] — bare: open the Color Studio; with an arg: give the
        // Square + Triangle pair for that hex or colour name directly.
        root.then(CommandManager.literal("customcolor")
                .executes(HexCommands::openStudio)
                .then(CommandManager.argument("colour", StringArgumentType.greedyString())
                        .suggests((c, b) -> { for (var lc : ColorLibrary.ALL) b.suggest(lc.name().toLowerCase(Locale.ROOT).replace(" ", "")); return b.buildFuture(); })
                        .executes(ctx -> giveCustom(ctx, StringArgumentType.getString(ctx, "colour")))));

        // /cb recolorvariants <colour> <oldhex> — repaint existing "*_<colour>" blocks from the
        // old hex to the current one (the hex-confirm GUI's Yes button runs exactly this).
        root.then(CommandManager.literal("recolorvariants")
                .then(CommandManager.argument("colour", StringArgumentType.word())
                        .suggests((c, b) -> { for (String k : ColorVariantService.COLOUR_KEYS) b.suggest(k); return b.buildFuture(); })
                        // greedyString (not word): oldhex carries a leading '#', which word()
                        // rejects. Final arg → greedy is safe (BUG 1).
                        .then(CommandManager.argument("oldhex", StringArgumentType.greedyString())
                                .executes(ctx -> recolorVariants(ctx, StringArgumentType.getString(ctx, "colour"),
                                        StringArgumentType.getString(ctx, "oldhex"))))));
    }

    private static boolean validColour(String key) {
        for (String k : ColorVariantService.COLOUR_KEYS) if (k.equals(key)) return true;
        return false;
    }

    /** /cb customcolor — open the Color Studio chest GUI (players only). */
    private static int openStudio(CommandContext<ServerCommandSource> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(ctx.getSource(), "Players only — the Color Studio is a GUI.");
            return 0;
        }
        GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CUSTOM_COLOR));
        return 1;
    }

    /** /cb customcolor <colour> — resolve a hex or colour name and give the tool pair. */
    private static int giveCustom(CommandContext<ServerCommandSource> ctx, String colourRaw) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(ctx.getSource(), "Players only — tools go into your inventory.");
            return 0;
        }
        String hex = ColorLibrary.resolve(colourRaw);
        if (hex == null) {
            var similar = ColorLibrary.suggest(colourRaw);
            Chat.error(ctx.getSource(), "\"" + colourRaw + "\" isn't a colour — use #RRGGBB or a "
                    + "name" + (similar.isEmpty() ? "" : " (did you mean " + String.join(", ", similar) + "?)")
                    + ". /cb customcolor shows them all.");
            return 0;
        }
        CustomColorToolItem.givePair(p, Integer.parseInt(hex.substring(1), 16));
        String name = ColorLibrary.nameForHex(hex);
        Chat.success(ctx.getSource(), "Gave you the " + (name != null ? name : hex)
                + " Square + Triangle §7(" + hex + ")§a.");
        return 1;
    }

    private static int hexStatus(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Variant colours: §cred " + CustomBlocksConfig.triangleRedHex
                + " §eyellow " + CustomBlocksConfig.triangleYellowHex
                + " §agreen " + CustomBlocksConfig.triangleGreenHex
                + " §8black " + CustomBlocksConfig.triangleBlackHex
                + " §8(/cb config hex <colour> <#RRGGBB>)");
        return 1;
    }

    private static int hexStatusOne(CommandContext<ServerCommandSource> ctx, String colourRaw) {
        String key = colourRaw.toLowerCase(Locale.ROOT);
        if (!validColour(key)) {
            Chat.error(ctx.getSource(), "Pick a colour: red, yellow, green, or black.");
            return 0;
        }
        Chat.info(ctx.getSource(), ColorVariantService.capitalize(key) + ": §f"
                + ColorVariantService.hexFor(key) + " §8(/cb config hex " + key + " <#RRGGBB>)");
        return 1;
    }

    private static int setHex(CommandContext<ServerCommandSource> ctx, String colourRaw, String valueRaw) {
        String key = colourRaw.toLowerCase(Locale.ROOT);
        if (!validColour(key)) {
            Chat.error(ctx.getSource(), "Pick a colour: red, yellow, green, or black.");
            return 0;
        }
        String norm = CustomBlocksConfig.normalizeHexColor(valueRaw, null);
        if (norm == null) {
            Chat.error(ctx.getSource(), "That's not a colour code — use #RRGGBB (e.g. #FF8800).");
            return 0;
        }
        String old = ColorVariantService.hexFor(key);
        if (norm.equals(old)) {
            Chat.info(ctx.getSource(), ColorVariantService.capitalize(key) + " is already " + norm + ".");
            return 1;
        }
        switch (key) {
            case "red"    -> CustomBlocksConfig.triangleRedHex = norm;
            case "yellow" -> CustomBlocksConfig.triangleYellowHex = norm;
            case "green"  -> CustomBlocksConfig.triangleGreenHex = norm;
            case "black"  -> CustomBlocksConfig.triangleBlackHex = norm;
        }
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), ColorVariantService.capitalize(key) + " §f" + old + " §a→ §f"
                + norm + "§a. New variants + item art use it now.");
        int n = ColorVariantService.variantCount(key);
        if (n > 0 && ctx.getSource().getEntity() instanceof ServerPlayerEntity p) {
            // The pack rebuild WAITS for this confirm GUI: Yes → the batch's single rebuild
            // covers the item re-tint too; No/close → the menu's onClose rebuilds (§7 —
            // never push a pack reload at a player while they're inside the prompt).
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.HEX_RECOLOR_CONFIRM, key + ":" + old));
            return 1;
        }
        ResourcePackServer.updatePack(); // no prompt to wait for — re-tint the item art now
        if (n > 0) {
            Chat.info(ctx.getSource(), n + " existing \"_" + key + "\" block(s) still wear " + old
                    + " — /cb recolorvariants " + key + " " + old + " repaints them.");
        }
        return 1;
    }

    private static int recolorVariants(CommandContext<ServerCommandSource> ctx, String colourRaw, String oldRaw) {
        String key = colourRaw.toLowerCase(Locale.ROOT);
        if (!validColour(key)) {
            Chat.error(ctx.getSource(), "Pick a colour: red, yellow, green, or black.");
            return 0;
        }
        String oldHex = CustomBlocksConfig.normalizeHexColor(oldRaw, null);
        if (oldHex == null) {
            Chat.error(ctx.getSource(), "The old colour must be #RRGGBB (it's what gets replaced).");
            return 0;
        }
        if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(ctx.getSource(), "Players only — progress is reported in chat.");
            return 0;
        }
        ColorVariantService.recolorVariants(p, key, Integer.parseInt(oldHex.substring(1), 16));
        return 1;
    }
}
