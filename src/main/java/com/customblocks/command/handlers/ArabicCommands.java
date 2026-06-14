/**
 * ArabicCommands.java
 *
 * Responsibility: /cb arabic subcommands — import (28 letters), letter, word, list.
 * Stays under 400 lines (§9.3).
 *
 * Depends on: ArabicLetterMap, ArabicBlockRegistry, SlotManager, Chat
 * Called by: CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.arabic.ArabicBlockRegistry;
import com.customblocks.arabic.ArabicLetterMap;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Optional;

public final class ArabicCommands {

    private ArabicCommands() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        var arabic = CommandManager.literal("arabic");

        // /cb arabic import — all 28 letters, white on black
        arabic.then(CommandManager.literal("import")
                .executes(ctx -> importAll(ctx)));

        // /cb arabic letter <name> — single letter
        arabic.then(CommandManager.literal("letter")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> {
                            String typed = b.getRemaining().toLowerCase(java.util.Locale.ROOT);
                            for (ArabicLetterMap.Letter l : ArabicLetterMap.ALL) {
                                if (l.nameEn().toLowerCase(java.util.Locale.ROOT).startsWith(typed)) b.suggest(l.nameEn());
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> importLetter(ctx,
                                StringArgumentType.getString(ctx, "name")))));

        // /cb arabic word <text> <id> <displayName>
        arabic.then(CommandManager.literal("word")
                .then(CommandManager.argument("text", StringArgumentType.word())
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .then(CommandManager.argument("displayName",
                                        StringArgumentType.greedyString())
                                        .executes(ctx -> importWord(ctx,
                                                StringArgumentType.getString(ctx, "text"),
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "displayName")))))));

        // /cb arabic list
        arabic.then(CommandManager.literal("list").executes(ArabicCommands::listLetters));

        root.then(arabic);
    }

    private static int importAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Chat.info(src, "Importing 28 Arabic letter blocks...");
        ArabicBlockRegistry.ImportResult r = ArabicBlockRegistry.importAll(WHITE, BLACK);
        if (r.created() > 0) Chat.success(src, "Created §e" + r.created() + "§r block(s).");
        if (r.skipped() > 0) Chat.info(src,    "Skipped §e" + r.skipped() + "§r (already exist).");
        if (r.failed()  > 0) Chat.error(src,   "Failed §c"  + r.failed()  + "§r — check log.");
        if (r.created() == 0 && r.skipped() == 28) Chat.info(src, "All 28 letters already imported.");
        return 1;
    }

    private static int importLetter(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        Optional<ArabicLetterMap.Letter> opt = ArabicLetterMap.byName(name);
        if (opt.isEmpty()) {
            Chat.error(src, "Unknown letter '§e" + name + "§r'. Valid names: alef, ba, ta, ... ya");
            return 0;
        }
        boolean ok = ArabicBlockRegistry.importLetter(opt.get(), WHITE, BLACK);
        if (ok) Chat.success(src, "Letter §e" + name + "§r ready. Give: /cb give " + opt.get().blockId());
        else    Chat.error(src, "Failed to import §e" + name + "§r. Is a font configured?");
        return ok ? 1 : 0;
    }

    private static int importWord(CommandContext<ServerCommandSource> ctx,
                                  String text, String id, String displayName) {
        ServerCommandSource src = ctx.getSource();
        String error = ArabicBlockRegistry.importWord(text, id, displayName, WHITE, BLACK);
        if (error != null) { Chat.error(src, "Failed: " + error); return 0; }
        Chat.success(src, "Arabic word block §e" + id + "§r created. Give: /cb give " + id);
        return 1;
    }

    private static int listLetters(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        long present = ArabicLetterMap.ALL.stream()
                .filter(l -> SlotManager.getById(l.blockId()) != null).count();
        src.sendFeedback(() -> Text.literal(Chat.PREFIX + "§eArabic: §f"
                + present + "/28 letters imported"), false);
        for (ArabicLetterMap.Letter l : ArabicLetterMap.ALL) {
            boolean has = SlotManager.getById(l.blockId()) != null;
            String tag  = has ? "§a[give]" : "§8[not imported]";
            src.sendFeedback(() -> Text.literal(
                    "§7 " + l.nameEn() + " §f(" + l.nameAr() + ") " + tag), false);
        }
        return 1;
    }
}
