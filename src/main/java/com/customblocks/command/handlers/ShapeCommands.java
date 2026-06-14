/**
 * ShapeCommands.java
 *
 * Responsibility: The block-shape subcommands (Group 08, slice 1 — commands):
 *   /cb setshape <id> <shape>   — change a block's shape (full, slab_bottom, slab_top, carpet,
 *                                 thin, pane, wall, pillar, stairs, cross — see BlockShapes)
 *   /cb clearshape <id>         — reset a block to a full cube
 *   /cb shapelist               — list every available shape + description
 *   /cb shapepreview <shape>    — float a temporary stand-in block (auto-removed after 5s) in
 *                                 front of you so you can see a shape before applying it
 *
 * Shape lives on SlotData; SlotBlock reads it live for collision/outline, and ServerPackGenerator
 * emits the matching model — so the change is one pack rebuild away from being visible. Undoable
 * via the dedicated SHAPE undo kind (which rebuilds the pack on undo/redo). Locked blocks refuse.
 *
 * Depends on: SlotManager/SlotData, BlockShapes, LockManager, UndoManager, ResourcePackServer, Chat
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.block.BlockShapes;
import com.customblocks.command.Chat;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.UndoManager;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.UUID;

public final class ShapeCommands {

    private ShapeCommands() {} // static-only

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("setshape")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .then(CommandManager.argument("shape", StringArgumentType.word())
                                .suggests((ctx, b) -> CommandSource.suggestMatching(BlockShapes.names(), b))
                                .executes(ctx -> applyShape(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "shape"))))));

        root.then(CommandManager.literal("clearshape")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> applyShape(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"), BlockShapes.DEFAULT))));

        root.then(CommandManager.literal("shapelist").executes(ctx -> shapeList(ctx.getSource())));

        root.then(CommandManager.literal("shapepreview")
                .then(CommandManager.argument("shape", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(BlockShapes.names(), b))
                        .executes(ctx -> shapePreview(ctx.getSource(),
                                StringArgumentType.getString(ctx, "shape")))));
    }

    /** Change one block's shape (clearshape passes "full"). Validates, applies, undo + pack rebuild. */
    private static int applyShape(ServerCommandSource src, String id, String shapeRaw) {
        String shape = shapeRaw == null ? "" : shapeRaw.trim().toLowerCase(Locale.ROOT);
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        if (!BlockShapes.isValid(shape)) {
            Chat.error(src, "Unknown shape \"" + shape + "\". See /cb shapelist for the choices.");
            return 0;
        }
        if (before.shape().equals(shape)) {
            Chat.info(src, "\"" + id + "\" is already " + shape + " — nothing to change.");
            return 1;
        }

        SlotData after = SlotManager.setShape(id, shape);
        if (after == null) { Chat.error(src, "Couldn't set the shape of \"" + id + "\"."); return 0; }
        UndoManager.recordShape(actor(src), before, after);
        ResourcePackServer.updatePack(); // the model changed — rebuild so the new shape shows

        if (shape.equals(BlockShapes.DEFAULT)) {
            Chat.success(src, "Cleared the shape of \"" + id + "\" — back to a full block. Undo with /cb undo.");
        } else {
            Chat.success(src, "Set the shape of \"" + id + "\" to §e" + shape + "§r. Undo with /cb undo.");
        }
        return 1;
    }

    private static int shapeList(ServerCommandSource src) {
        Chat.info(src, "Available shapes (use /cb setshape <id> <shape>):");
        for (String name : BlockShapes.names()) {
            src.sendFeedback(() -> Text.literal("  §e" + name + " §7- " + BlockShapes.description(name)), false);
        }
        return 1;
    }

    /**
     * Spawn a temporary block_display showing {@code shape}, ~2.5 blocks in front of the player at
     * eye level, then auto-remove it after 5 seconds. Built by orchestrating the vanilla summon/kill
     * commands as the player (so the NBT parser does the work — the mod has no entity code), each
     * tagged uniquely so the kill only removes this preview. Player + op only (summon needs level 2).
     */
    private static int shapePreview(ServerCommandSource src, String shapeRaw) {
        String shape = shapeRaw == null ? "" : shapeRaw.trim().toLowerCase(Locale.ROOT);
        if (!BlockShapes.isValid(shape)) {
            Chat.error(src, "Unknown shape \"" + shape + "\". See /cb shapelist for the choices.");
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
            Chat.error(src, "Run /cb shapepreview as a player — it shows the shape in front of you.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        // A point ~2.5 blocks ahead at eye level, shifted by half a block so the 1×1 model centres on it.
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d at = eye.add(look.multiply(2.5)).subtract(0.5, 0.5, 0.5);
        String pos = fmt(at.x) + " " + fmt(at.y) + " " + fmt(at.z);
        String tag = "cb_preview_" + System.nanoTime();

        // Run the vanilla summon/kill at op level (4) so they work even in a no-cheats world / for a
        // non-op player, and silently so only our own friendly message shows.
        ServerCommandSource silent = src.withLevel(4).withSilent();
        String summon = "summon minecraft:block_display " + pos + " {block_state:" + blockStateNbt(shape)
                + ",Tags:[\"" + tag + "\"],brightness:{block:15,sky:15}}";
        server.getCommandManager().executeWithPrefix(silent, summon);
        Chat.info(src, "Previewing §e" + shape + "§r for 5 seconds…");

        // Remove it after 5s — daemon thread (the mod's timed-work idiom), hop back to the server thread.
        String kill = "kill @e[type=minecraft:block_display,tag=" + tag + "]";
        Thread cleaner = new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) { return; }
            server.execute(() -> server.getCommandManager().executeWithPrefix(silent, kill));
        }, "CustomBlocks-ShapePreview");
        cleaner.setDaemon(true);
        cleaner.start();
        return 1;
    }

    /** A vanilla stand-in block whose own model shows the shape, as a block_state NBT object. */
    private static String blockStateNbt(String shape) {
        return switch (shape) {
            case "slab_bottom" -> "{Name:\"minecraft:smooth_stone_slab\",Properties:{type:\"bottom\"}}";
            case "slab_top"    -> "{Name:\"minecraft:smooth_stone_slab\",Properties:{type:\"top\"}}";
            case "carpet"      -> "{Name:\"minecraft:white_carpet\"}";
            case "thin", "pane" -> "{Name:\"minecraft:glass_pane\"}";
            case "wall"        -> "{Name:\"minecraft:cobblestone_wall\"}";
            case "pillar"      -> "{Name:\"minecraft:end_rod\"}";
            case "stairs"      -> "{Name:\"minecraft:stone_stairs\"}";
            case "cross"       -> "{Name:\"minecraft:poppy\"}";
            default            -> "{Name:\"minecraft:stone\"}"; // full
        };
    }

    /** Format a coordinate with a dot decimal (never a locale comma — the summon parser needs dots). */
    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }
}
