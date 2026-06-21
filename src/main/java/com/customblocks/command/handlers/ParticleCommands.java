/**
 * ParticleCommands.java
 *
 * Responsibility: /cb particles — open the Particle FX board (player) or list states (console),
 * and /cb particles <category> on|off to toggle one category's particle effect. The effects live
 * in core/ParticleFx; the enable flags live in CustomBlocksConfig (particlesEnabled_<category>).
 * Group 16, slice 4.
 *
 * Depends on: CustomBlocksConfig, ParticleFx, GuiRouter, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.ParticleFx;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

public final class ParticleCommands {

    private ParticleCommands() {} // static-only

    private static final SuggestionProvider<ServerCommandSource> CATEGORIES =
            (ctx, b) -> CommandSource.suggestMatching(java.util.Arrays.asList(CustomBlocksConfig.FX_CATEGORIES), b);
    private static final SuggestionProvider<ServerCommandSource> ON_OFF =
            (ctx, b) -> CommandSource.suggestMatching(java.util.List.of("on", "off"), b);

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("particles")
                .executes(ParticleCommands::open)
                .then(CommandManager.argument("category", StringArgumentType.word())
                        .suggests(CATEGORIES)
                        .then(CommandManager.argument("state", StringArgumentType.word())
                                .suggests(ON_OFF)
                                .executes(ParticleCommands::set))));
    }

    // /cb particles — players get the board; console gets a text readout.
    private static int open(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.PARTICLES));
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eParticle FX:"), false);
        for (String c : CustomBlocksConfig.FX_CATEGORIES) {
            boolean on = CustomBlocksConfig.particlesOn(c);
            src.sendFeedback(() -> Text.literal("§7" + c + ": " + (on ? "§aon" : "§7off")), false);
        }
        return 1;
    }

    // /cb particles <category> on|off
    private static int set(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String cat = StringArgumentType.getString(ctx, "category").toLowerCase(Locale.ROOT);
        String state = StringArgumentType.getString(ctx, "state").toLowerCase(Locale.ROOT);
        if (!CustomBlocksConfig.isFxCategory(cat)) {
            Chat.error(src, "Unknown category '" + cat + "'. Options: "
                    + String.join(", ", CustomBlocksConfig.FX_CATEGORIES));
            return 0;
        }
        boolean on;
        if (state.equals("on")) on = true;
        else if (state.equals("off")) on = false;
        else { Chat.error(src, "Use §fon §cor §foff§c."); return 0; }
        CustomBlocksConfig.particlesEnabled.put(cat, on);
        CustomBlocksConfig.save();
        Chat.success(src, "Particles for §f" + cat + " §fare now " + (on ? "§aON" : "§7OFF") + "§f.");
        if (on && src.getEntity() instanceof ServerPlayerEntity p) ParticleFx.preview(p, cat);
        return 1;
    }
}
