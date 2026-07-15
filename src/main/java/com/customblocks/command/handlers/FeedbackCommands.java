/**
 * FeedbackCommands.java
 *
 * Responsibility: /cb feedback — open the merged Feedback FX board (player) or list states
 * (console), and /cb feedback <category> on|off to toggle BOTH the particle and the sound for one
 * category at once (the "master" switch). The split switches are /cb particles (FX only) and
 * /cb sounds (sound only); all three open the same board. Group 16, slice 5.
 *
 * Depends on: CustomBlocksConfig, ParticleFx, SoundFx, GuiRouter, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.ParticleFx;
import com.customblocks.core.SoundFx;
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

public final class FeedbackCommands {

    private FeedbackCommands() {} // static-only

    private static final SuggestionProvider<ServerCommandSource> CATEGORIES =
            (ctx, b) -> CommandSource.suggestMatching(java.util.Arrays.asList(CustomBlocksConfig.FX_CATEGORIES), b);
    private static final SuggestionProvider<ServerCommandSource> ON_OFF =
            (ctx, b) -> CommandSource.suggestMatching(java.util.List.of("on", "off"), b);

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("feedback")
                .executes(FeedbackCommands::open)
                .then(CommandManager.argument("category", StringArgumentType.word())
                        .suggests(CATEGORIES)
                        .then(CommandManager.argument("state", StringArgumentType.word())
                                .suggests(ON_OFF)
                                .executes(FeedbackCommands::set))));
    }

    // /cb feedback — players open the board; console gets a combined particle/sound readout.
    private static int open(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.PARTICLES));
            return 1;
        }
        Chat.raw(src, CbFmt.VALUE + "Feedback FX (particle / sound):");
        for (String c : CustomBlocksConfig.FX_CATEGORIES) {
            boolean pOn = CustomBlocksConfig.particlesOn(c);
            boolean sOn = CustomBlocksConfig.soundsOn(c);
            Chat.raw(src, Text.literal(CbFmt.DIM + c + ": particle " + (pOn ? CbFmt.OK + "on" : CbFmt.DIM + "off")
                    + " " + CbFmt.DIM + "/ sound " + (sOn ? CbFmt.OK + "on" : CbFmt.DIM + "off")));
        }
        return 1;
    }

    // /cb feedback <category> on|off — sets BOTH the particle and the sound flag.
    private static int set(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String cat = StringArgumentType.getString(ctx, "category").toLowerCase(Locale.ROOT);
        String state = StringArgumentType.getString(ctx, "state").toLowerCase(Locale.ROOT);
        if (!CustomBlocksConfig.isFxCategory(cat)) {
            Chat.error(src, "Unknown category \"" + cat + "\". Options: "
                    + String.join(", ", CustomBlocksConfig.FX_CATEGORIES));
            return 0;
        }
        boolean on;
        if (state.equals("on")) on = true;
        else if (state.equals("off")) on = false;
        else { Chat.error(src, "Use " + CbFmt.BODY + "on " + CbFmt.BAD + "or " + CbFmt.BODY + "off" + CbFmt.BAD + "."); return 0; }
        CustomBlocksConfig.particlesEnabled.put(cat, on);
        CustomBlocksConfig.soundsEnabled.put(cat, on);
        CustomBlocksConfig.save();
        Chat.success(src, "Feedback (particle + sound) for " + CbFmt.BODY + cat + " " + CbFmt.BODY + "is now "
                + (on ? CbFmt.OK + "ON" : CbFmt.DIM + "OFF") + CbFmt.BODY + ".");
        if (on && src.getEntity() instanceof ServerPlayerEntity p) {
            ParticleFx.preview(p, cat);
            SoundFx.preview(p, cat);
        }
        return 1;
    }
}
