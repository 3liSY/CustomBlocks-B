/**
 * ConfigCommands.java
 *
 * Responsibility: In-game config tweaks via chat (NOT a GUI). Currently `/cb config undomode`
 * cycles the undo scope between server-wide and per-player; `/cb config` shows the current
 * value. Registered into the /cb tree by CommandRegistrar. Stays under 400 lines (§9.3).
 *
 * Switching undo scope clears the existing history (the old stacks live in a different
 * keyspace), so the change is predictable rather than leaving half-orphaned entries.
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.core.AutoBackup;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.SilentPackPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ConfigCommands {

    private ConfigCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("config")
                .executes(ConfigCommands::show)
                .then(CommandManager.literal("undomode")
                        .executes(ConfigCommands::cycleUndoMode)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("serverwide");
                                    b.suggest("perplayer");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setUndoMode(ctx, StringArgumentType.getString(ctx, "value"))))));

        // /cb config hud [toggle|on|off] — HUD visibility (Group 03). Brigadier merges this
        // second "config" literal into the one above, keeping /cb config (show) intact.
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("hud")
                        .executes(ConfigCommands::hudStatus)
                        .then(CommandManager.literal("toggle").executes(ctx -> setHud(ctx, !CustomBlocksConfig.hudEnabled)))
                        .then(CommandManager.literal("on").executes(ctx -> setHud(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> setHud(ctx, false)))));

        // /cb config didyoumean [smart|always|off] — typo-correction mode (Group 04).
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("didyoumean")
                        .executes(ConfigCommands::didYouMeanStatus)
                        .then(CommandManager.literal("smart").executes(ctx -> setDidYouMean(ctx, "smart")))
                        .then(CommandManager.literal("always").executes(ctx -> setDidYouMean(ctx, "always")))
                        .then(CommandManager.literal("off").executes(ctx -> setDidYouMean(ctx, "off")))
                        .then(CommandManager.literal("cycle").executes(ConfigCommands::cycleDidYouMean))));

        // /cb config silentpack [toggle|on|off] — silent resource-pack delivery (Group 05).
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("silentpack")
                        .executes(ConfigCommands::silentPackStatus)
                        .then(CommandManager.literal("toggle").executes(ctx -> setSilentPack(ctx, !CustomBlocksConfig.silentPack)))
                        .then(CommandManager.literal("on").executes(ctx -> setSilentPack(ctx, true)))
                        .then(CommandManager.literal("off").executes(ctx -> setSilentPack(ctx, false)))));

        // /cb config background [NoBgRemove|BgRemove|BgRemove&More] — strip image backgrounds to
        // black on (re)texture (M1). A greedy arg is used so "BgRemove&More" (with the &) parses.
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("background")
                        .executes(ConfigCommands::backgroundStatus)
                        .then(CommandManager.argument("mode", StringArgumentType.greedyString())
                                .suggests((c, b) -> {
                                    b.suggest("NoBgRemove");
                                    b.suggest("BgRemove");
                                    b.suggest("BgRemove&More");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> setBackground(ctx, StringArgumentType.getString(ctx, "mode"))))));

        // /cb tolerance <0-100> — background-removal strength (M1). 0 = off; >0 prompts the player
        // (clickable) to choose Background only vs Background + enclosed areas.
        root.then(CommandManager.literal("tolerance")
                .executes(ConfigCommands::toleranceStatus)
                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setTolerance(ctx, IntegerArgumentType.getInteger(ctx, "value")))));

        // /cb config texturesize [16-512] — block texture resolution (pixelation fix). Higher =
        // sharper but a larger resource pack. Applies to FUTURE (re)textures; existing blocks need
        // a retexture to change. With no argument, opens the size-picker chest GUI for a player.
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("texturesize")
                        .executes(ConfigCommands::openTextureSizeMenu)
                        .then(CommandManager.argument("px", IntegerArgumentType.integer(16, CustomBlocksConfig.MAX_TEXTURE_SIZE))
                                .suggests((c, b) -> { b.suggest(16); b.suggest(32); b.suggest(64); b.suggest(128); b.suggest(256); b.suggest(512); return b.buildFuture(); })
                                .executes(ctx -> setTextureSize(ctx, IntegerArgumentType.getInteger(ctx, "px"))))));

        // /cb config autobackup [interval [min] | keep [count]] — timed auto-backup (Group 09 / Slice 3).
        // No value after interval/keep cycles through presets (used by the Config GUI tile).
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("autobackup")
                        .executes(ConfigCommands::autoBackupStatus)
                        .then(CommandManager.literal("interval")
                                .executes(ConfigCommands::cycleAutoInterval)
                                .then(CommandManager.argument("min", IntegerArgumentType.integer(0, 10080))
                                        .executes(ctx -> setAutoInterval(ctx, IntegerArgumentType.getInteger(ctx, "min")))))
                        .then(CommandManager.literal("keep")
                                .executes(ConfigCommands::cycleAutoKeep)
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(0, 1000))
                                        .executes(ctx -> setAutoKeep(ctx, IntegerArgumentType.getInteger(ctx, "count")))))));

        // /cb config hex + /cb recolorvariants (M3 hex) live in HexCommands (400-line limit).
    }

    private static int show(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            // Gate the config screen behind a Yes/No confirm (it changes live behaviour).
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.CONFIG_CONFIRM));
            return 1;
        }
        Chat.info(src, "Undo mode: §f" + label(CustomBlocksConfig.undoMode)
                + " §8(/cb config undomode to switch)");
        return 1;
    }

    // ── Auto-backup (Group 09 / Slice 3) ─────────────────────────────────────

    private static final int[] INTERVAL_PRESETS = {0, 5, 15, 30, 60, 120, 360};
    private static final int[] KEEP_PRESETS     = {3, 5, 10, 20, 50};

    private static int autoBackupStatus(CommandContext<ServerCommandSource> ctx) {
        int iv = CustomBlocksConfig.autoBackupInterval;
        Chat.info(ctx.getSource(), "Auto-backup: " + (iv <= 0 ? "§cOFF" : "§aevery " + iv + " min")
                + " §7· keep §f" + CustomBlocksConfig.autoBackupKeepCount
                + " §8(/cb config autobackup interval <min> · keep <count>)");
        return 1;
    }

    private static int cycleAutoInterval(CommandContext<ServerCommandSource> ctx) {
        return setAutoInterval(ctx, nextPreset(INTERVAL_PRESETS, CustomBlocksConfig.autoBackupInterval));
    }

    private static int setAutoInterval(CommandContext<ServerCommandSource> ctx, int min) {
        CustomBlocksConfig.autoBackupInterval = Math.max(0, Math.min(10080, min));
        CustomBlocksConfig.save();
        AutoBackup.applyConfigChange(); // adopt the new interval immediately
        int iv = CustomBlocksConfig.autoBackupInterval;
        Chat.success(ctx.getSource(), iv <= 0
                ? "Auto-backup turned off."
                : "Auto-backup → every " + iv + " min.");
        return 1;
    }

    private static int cycleAutoKeep(CommandContext<ServerCommandSource> ctx) {
        return setAutoKeep(ctx, nextPreset(KEEP_PRESETS, CustomBlocksConfig.autoBackupKeepCount));
    }

    private static int setAutoKeep(CommandContext<ServerCommandSource> ctx, int count) {
        CustomBlocksConfig.autoBackupKeepCount = Math.max(0, Math.min(1000, count));
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), "Auto-backup keep count → " + CustomBlocksConfig.autoBackupKeepCount
                + ". Older auto-backups beyond that are pruned.");
        return 1;
    }

    /** Next value in {@code presets} after {@code current} (wraps); if {@code current} isn't a preset,
     *  snap to the first preset greater than it, else the first preset. */
    private static int nextPreset(int[] presets, int current) {
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == current) return presets[(i + 1) % presets.length];
        }
        for (int v : presets) if (v > current) return v;
        return presets[0];
    }

    private static int cycleUndoMode(CommandContext<ServerCommandSource> ctx) {
        String next = "global".equals(CustomBlocksConfig.undoMode) ? "per_player" : "global";
        return apply(ctx, next);
    }

    private static int setUndoMode(CommandContext<ServerCommandSource> ctx, String raw) {
        String mode;
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "serverwide": case "server": case "global": case "shared":
                mode = "global"; break;
            case "perplayer": case "per_player": case "player": case "isolated":
                mode = "per_player"; break;
            default:
                Chat.error(ctx.getSource(), "Use: serverwide or perplayer");
                return 0;
        }
        return apply(ctx, mode);
    }

    private static int apply(CommandContext<ServerCommandSource> ctx, String mode) {
        CustomBlocksConfig.undoMode = mode;
        CustomBlocksConfig.save();
        UndoManager.clearAll(); // switching scope invalidates the old stacks
        Chat.success(ctx.getSource(), "Undo mode → " + label(mode) + " §7(history cleared)");
        return 1;
    }

    private static int hudStatus(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "HUD: " + (CustomBlocksConfig.hudEnabled ? "§aON" : "§cOFF")
                + " §8(/cb config hud toggle · /cb edithud to customize)");
        return 1;
    }

    private static int setHud(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        CustomBlocksConfig.hudEnabled = enabled;
        CustomBlocksConfig.save();
        if (ctx.getSource().getEntity() instanceof ServerPlayerEntity p) {
            ServerPlayNetworking.send(p, new HudStatePayload(enabled));
        }
        Chat.success(ctx.getSource(), "HUD " + (enabled ? "enabled" : "disabled") + ".");
        return 1;
    }

    private static int didYouMeanStatus(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Typo correction (Did-you-mean) is set to §f"
                + CustomBlocksConfig.didYouMean + "§7. Options: smart, always, off.");
        return 1;
    }

    private static int setDidYouMean(CommandContext<ServerCommandSource> ctx, String mode) {
        CustomBlocksConfig.didYouMean = mode;
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), switch (mode) {
            case "off"    -> "Typo correction turned off — unknown commands show a plain message.";
            case "always" -> "Typo correction set to \"always\" — the closest match is always suggested.";
            default        -> "Typo correction set to \"smart\" — suggestions only when confident.";
        });
        return 1;
    }

    private static int cycleDidYouMean(CommandContext<ServerCommandSource> ctx) {
        String next = switch (CustomBlocksConfig.didYouMean) {
            case "smart"  -> "always";
            case "always" -> "off";
            default        -> "smart";
        };
        return setDidYouMean(ctx, next);
    }

    private static int silentPackStatus(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Silent resource pack: " + (CustomBlocksConfig.silentPack ? "§aON" : "§cOFF")
                + " §8(/cb config silentpack toggle)");
        return 1;
    }

    private static int setSilentPack(CommandContext<ServerCommandSource> ctx, boolean enabled) {
        CustomBlocksConfig.silentPack = enabled;
        CustomBlocksConfig.save();
        // Push the new preference to every online client so it takes effect immediately.
        MinecraftServer s = ctx.getSource().getServer();
        if (s != null) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, new SilentPackPayload(enabled));
            }
        }
        Chat.success(ctx.getSource(), enabled
                ? "Silent pack on — players get texture updates with no download dialog."
                : "Silent pack off — players will see the vanilla resource-pack dialog.");
        return 1;
    }

    /** No-arg /cb config texturesize: open the size-picker GUI for a player; print status to console. */
    private static int openTextureSizeMenu(CommandContext<ServerCommandSource> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.TEXTURE_SIZE));
            return 1;
        }
        return textureSizeStatus(ctx);
    }

    private static int textureSizeStatus(CommandContext<ServerCommandSource> ctx) {
        Chat.info(ctx.getSource(), "Texture size: §e" + CustomBlocksConfig.textureSize
                + "px §8(/cb config texturesize 16-" + CustomBlocksConfig.MAX_TEXTURE_SIZE
                + " · higher = sharper, bigger pack)");
        return 1;
    }

    private static int setTextureSize(CommandContext<ServerCommandSource> ctx, int px) {
        int size = CustomBlocksConfig.sanitizeTextureSize(px); // pow2; ≤256 is atlas-safe, 512 needs the own-texture renderer
        CustomBlocksConfig.textureSize = size;
        CustomBlocksConfig.save();
        String note;
        if (px != size) {
            note = " §7(snapped from " + px + "px — sizes are powers of two up to "
                    + CustomBlocksConfig.MAX_TEXTURE_SIZE + "px).";
        } else if (size > 256) {
            note = " §e(heads up: " + size + "px is past the atlas-safe 256px — current blocks render via the"
                    + " atlas and may soften at this size until the own-texture renderer ships. See ADR-008.)";
        } else {
            note = " §7(" + size + "px is atlas-safe and crisp).";
        }
        Chat.success(ctx.getSource(), "Texture size → §e" + size + "px§r. "
                + "Re-create or retexture a block to see it at the new resolution" + note);
        return 1;
    }

    private static int backgroundStatus(CommandContext<ServerCommandSource> ctx) {
        String mode = CustomBlocksConfig.backgroundMode;
        Chat.info(ctx.getSource(), "Background removal: §f" + BackgroundRemover.displayName(mode)
                + " §8(arg: " + BackgroundRemover.commandArg(mode)
                + " · options: NoBgRemove · BgRemove · BgRemove&More)");
        return 1;
    }

    private static int setBackground(CommandContext<ServerCommandSource> ctx, String raw) {
        String mode = BackgroundRemover.fromArg(raw);
        if (mode == null) {
            Chat.error(ctx.getSource(), "Use: NoBgRemove, BgRemove, or BgRemove&More");
            return 0;
        }
        CustomBlocksConfig.backgroundMode = mode;
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), "Background removal → §f" + BackgroundRemover.displayName(mode)
                + "§a. " + switch (mode) {
                    case "edges"  -> "New textures get their edge background painted black.";
                    case "closed" -> "Edge + enclosed background areas painted black.";
                    default        -> "Textures are used exactly as downloaded.";
                });
        return 1;
    }

    private static int toleranceStatus(CommandContext<ServerCommandSource> ctx) {
        int t = CustomBlocksConfig.backgroundTolerance;
        String mode = CustomBlocksConfig.backgroundMode;
        Chat.info(ctx.getSource(), "Background strength: §f" + t + "§7/100 §8("
                + ("none".equals(mode) ? "off" : BackgroundRemover.displayName(mode))
                + ") §7— /cb tolerance <0-100>");
        return 1;
    }

    private static int setTolerance(CommandContext<ServerCommandSource> ctx, int value) {
        ServerCommandSource src = ctx.getSource();
        if (value <= 0) {
            CustomBlocksConfig.backgroundTolerance = 0;
            CustomBlocksConfig.backgroundMode = "none";
            CustomBlocksConfig.save();
            Chat.success(src, "Background removal turned §coff§a §7(strength 0). Textures are used exactly as downloaded.");
            return 1;
        }
        CustomBlocksConfig.backgroundTolerance = value;
        CustomBlocksConfig.save();
        // Ask (clickable) which reach to apply the new strength at.
        MutableText msg = Text.literal(Chat.PREFIX + "§fStrip strength set to §e" + value
                        + "§7/100. Choose how far it reaches:  ")
                .append(modeButton("§a§l[ Background Only ]", "/cb config background BgRemove",
                        "Remove only the outer background connected to the image edges"))
                .append(Text.literal("  "))
                .append(modeButton("§b§l[ Background + Enclosed Areas ]", "/cb config background BgRemove&More",
                        "Also remove enclosed background pockets trapped inside the image"));
        src.sendFeedback(() -> msg, false);
        return 1;
    }

    /** A clickable chat button that runs a command when clicked. */
    private static MutableText modeButton(String label, String command, String hover) {
        return Text.literal(label).styled(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    private static String label(String mode) {
        return "per_player".equals(mode) ? "per-player" : "server-wide";
    }
}
