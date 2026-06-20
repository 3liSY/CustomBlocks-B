/**
 * MirrorCommands.java
 *
 * Responsibility: the `/cb config mirrornames` subtree — status / on / off / rebuild for the optional
 * named-texture mirror (Group 26 Part C). Kept as its OWN handler rather than added to ConfigCommands
 * (374/400 lines — one more block would bust the verifyFileSize gate; a domain-split handler is the §5
 * rule anyway). Brigadier merges this "config" literal into the others, so /cb config still works.
 *
 * All the real work lives in core/TextureNameMirror — this is just the chat surface.
 *
 * Depends on: CustomBlocksConfig, Chat, TextureNameMirror
 * Called by:  CommandRegistrar.register()
 */
package com.customblocks.command.handlers;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.Chat;
import com.customblocks.core.TextureNameMirror;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class MirrorCommands {

    private MirrorCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        // /cb config mirrornames [on|off|rebuild] — the named-texture mirror (Group 26 Part C).
        root.then(CommandManager.literal("config")
                .then(CommandManager.literal("mirrornames")
                        .executes(MirrorCommands::status)
                        .then(CommandManager.literal("on").executes(MirrorCommands::on))
                        .then(CommandManager.literal("off").executes(MirrorCommands::off))
                        .then(CommandManager.literal("rebuild").executes(MirrorCommands::rebuild))));
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        boolean on = CustomBlocksConfig.mirrorNamedTextures;
        Chat.info(ctx.getSource(), "Named-texture mirror: " + (on ? "§aON" : "§cOFF")
                + " §7· §f" + TextureNameMirror.fileCount() + "§7 file(s)"
                + " §8(/cb config mirrornames on · off · rebuild)");
        Chat.info(ctx.getSource(), "§7Folder: §f" + TextureNameMirror.folderPath());
        return 1;
    }

    private static int on(CommandContext<ServerCommandSource> ctx) {
        CustomBlocksConfig.mirrorNamedTextures = true;
        CustomBlocksConfig.save();
        int files = TextureNameMirror.rebuildAll(); // backfill every existing block
        Chat.success(ctx.getSource(), "Named-texture mirror §aon§r — wrote §f" + files
                + "§r file(s) to textures_names/. Browse your blocks by name there. "
                + "§7(slot_N.png stays the real texture — this is just a readable copy.)");
        return 1;
    }

    private static int off(CommandContext<ServerCommandSource> ctx) {
        CustomBlocksConfig.mirrorNamedTextures = false;
        CustomBlocksConfig.save();
        Chat.success(ctx.getSource(), "Named-texture mirror §coff§r. Existing files are left in place; "
                + "no new ones are written. §7(/cb config mirrornames on re-enables and refreshes them.)");
        return 1;
    }

    private static int rebuild(CommandContext<ServerCommandSource> ctx) {
        int files = TextureNameMirror.rebuildAll(); // wipe + regenerate from truth
        Chat.success(ctx.getSource(), "Rebuilt the named-texture mirror from scratch — §f" + files + "§r file(s)"
                + (CustomBlocksConfig.mirrorNamedTextures ? "."
                        : " §7(note: the mirror is currently OFF, so it won't auto-update until you turn it on)."));
        return 1;
    }
}
