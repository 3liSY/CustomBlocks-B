/**
 * HelpCommands.java
 *
 * Responsibility: /cb help (the chest-GUI command browser) and /cb welcome (the
 * quick-start message with clickable first actions) — Group 04. Console gets a plain
 * text command list since it cannot open a chest. Stays under 400 lines (§9.3).
 *
 * Depends on: HelpMenu/HelpTopics (via GuiRouter), Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.gui.chest.GuiRouter;
import com.customblocks.gui.chest.HelpTopics;
import com.customblocks.gui.chest.Nav;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HelpCommands {

    private HelpCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("help").executes(HelpCommands::help));
        root.then(CommandManager.literal("welcome").executes(HelpCommands::welcome));
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            GuiRouter.openFresh(p, Nav.MenuKey.of(Nav.Dest.HELP));
            return 1;
        }
        // Console fallback: a compact text list, one line per category.
        for (HelpTopics.Category c : HelpTopics.CATEGORIES) {
            StringBuilder sb = new StringBuilder(CbFmt.VALUE + c.name() + CbFmt.DIM + ": ");
            for (int i = 0; i < c.topics().size(); i++) {
                if (i > 0) sb.append(CbFmt.FAINT + ", ");
                sb.append(CbFmt.BODY).append(c.topics().get(i).label());
            }
            String line = sb.toString();
            Chat.raw(src, Text.literal(line));
        }
        return 1;
    }

    private static int welcome(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Chat.raw(src, CbFmt.VALUE + CbFmt.BOLD + "Welcome to CustomBlocks!" + CbFmt.RESET + " " + CbFmt.DIM + "Turn any "
                + "image URL into a real, placeable block — here's how to get going:");
        Chat.raw(src, Text.literal(CbFmt.DIM + " ")
                .append(Chat.suggestButton("[Create your first block]", "/cb create ",
                        "Pre-fills /cb create — add an id, a name and an image URL"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[Open the dashboard]", "/cb",
                        "Open the CustomBlocks dashboard"))
                .append(Text.literal(" "))
                .append(Chat.runButton("[Browse help]", "/cb help",
                        "Open the command browser")));
        return 1;
    }


}
