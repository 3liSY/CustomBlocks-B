/**
 * SoundCommands.java
 *
 * Responsibility: /cb sounds — list per-category event-sound states (any source), and
 * /cb sounds <category> on|off to toggle ONE category's sound only (the sound half of the merged
 * Feedback FX, slice 5). The sounds live in core/SoundFx; the enable flags live in
 * CustomBlocksConfig (soundsEnabled_<category>). The particle half is /cb particles; /cb feedback
 * (slice-5 R2) flips both at once.
 *
 * Depends on: CustomBlocksConfig, SoundFx, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
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

public final class SoundCommands {

    private SoundCommands() {} // static-only

    private static final SuggestionProvider<ServerCommandSource> CATEGORIES =
            (ctx, b) -> CommandSource.suggestMatching(java.util.Arrays.asList(CustomBlocksConfig.FX_CATEGORIES), b);
    private static final SuggestionProvider<ServerCommandSource> ON_OFF =
            (ctx, b) -> CommandSource.suggestMatching(java.util.List.of("on", "off"), b);

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("sounds")
                .executes(SoundCommands::open)
                .then(CommandManager.argument("category", StringArgumentType.word())
                        .suggests(CATEGORIES)
                        .then(CommandManager.argument("state", StringArgumentType.word())
                                .suggests(ON_OFF)
                                .executes(SoundCommands::set))));
    }

    // /cb sounds — players open the Feedback FX board; console gets a text readout.
    private static int open(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.PARTICLES));
            return 1;
        }
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eEvent sounds:"), false);
        for (String c : CustomBlocksConfig.FX_CATEGORIES) {
            boolean on = CustomBlocksConfig.soundsOn(c);
            src.sendFeedback(() -> Text.literal("§7" + c + ": " + (on ? "§aon" : "§7off")), false);
        }
        return 1;
    }

    // /cb sounds <category> on|off — toggles the sound flag only.
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
        CustomBlocksConfig.soundsEnabled.put(cat, on);
        CustomBlocksConfig.save();
        Chat.success(src, "Sound for §f" + cat + " §fis now " + (on ? "§aON" : "§7OFF") + "§f.");
        if (on && src.getEntity() instanceof ServerPlayerEntity p) SoundFx.preview(p, cat);
        return 1;
    }
}
