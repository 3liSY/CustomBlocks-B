/**
 * ColorVariantCommands.java — the Brigadier tree for /cb colorvariants (alias /cb variants), G10-CV.
 * Thin command layer only: it parses the args and delegates to {@link com.customblocks.command.ColorFamilyOps}
 * (which holds the family logic under the 500-line class cap, keeping this handler under the 400-line gate).
 *
 *   /cb colorvariants <id> <name> <link>   — NEW colour family from one image link: the bare <id> (normal/
 *                                            black upload) plus <id>_red / _green / _yellow. If the family
 *                                            already exists, this becomes an OVERWRITE behind /cb confirm.
 *   /cb colorvariants <id>                 — rebuild an EXISTING block's 3 colours from its stored image.
 *   /cb colorvariants delete <id>          — delete the whole family (base + whichever colours exist),
 *                                            behind /cb confirm; one /cb undo restores it.
 *
 * Depends on: ColorFamilyOps, Chat.
 * Called by:  CommandRegistrar.
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.command.Chat;
import com.customblocks.command.ColorFamilyDelete;
import com.customblocks.command.ColorFamilyOps;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class ColorVariantCommands {

    private ColorVariantCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(familyCommand("colorvariants"));
        root.then(familyCommand("variants")); // alias — same tree (G10-CV "locked")
    }

    /** Build the subcommand tree under one literal name, so the alias gets an identical tree. */
    private static LiteralArgumentBuilder<ServerCommandSource> familyCommand(String name) {
        return CommandManager.literal(name)
                .executes(ctx -> usage(ctx.getSource()))
                // "delete" sits BEFORE the <id> argument so `/cb colorvariants delete <id>` parses as the
                // delete sub-form rather than as a family whose id happens to be "delete".
                .then(CommandManager.literal("delete")
                        .executes(ctx -> needDeleteId(ctx.getSource()))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> ColorFamilyDelete.deleteFamily(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")))))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        // /cb colorvariants <id> — rebuild an existing block's colour family from its stored image.
                        .executes(ctx -> ColorFamilyOps.rebuildFamily(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id")))
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                // /cb colorvariants <id> <name> — missing the link.
                                .executes(ctx -> needLink(ctx.getSource()))
                                .then(CommandManager.argument("link", StringArgumentType.greedyString())
                                        .executes(ctx -> ColorFamilyOps.createFamily(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "link").trim())))));
    }

    private static int usage(ServerCommandSource src) {
        Chat.info(src, "Make a colour family from one image: " + CbFmt.BODY + "/cb colorvariants <id> <name> <link>" + CbFmt.DIM + ". "
                + "It builds " + CbFmt.BODY + "<id>" + CbFmt.DIM + " plus " + CbFmt.BAD + "<id>_red" + CbFmt.DIM + ", " + CbFmt.OK + "<id>_green" + CbFmt.DIM + ", " + CbFmt.VALUE + "<id>_yellow" + CbFmt.DIM + ".");
        return 1;
    }

    private static int needDeleteId(ServerCommandSource src) {
        Chat.error(src, "Say which family to delete: " + CbFmt.BODY + "/cb colorvariants delete <id>" + CbFmt.BAD + ".");
        return 0;
    }

    private static int needLink(ServerCommandSource src) {
        Chat.error(src, "That form needs an image link too: /cb colorvariants <id> <name> <link>.");
        return 0;
    }
}
