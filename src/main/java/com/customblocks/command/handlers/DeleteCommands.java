/**
 * DeleteCommands.java
 *
 * Responsibility: Block deletion — /cb delete <id> and /cb delete # (the custom block the
 * player is looking at, ~6-block raycast). Split out of CreationCommands (Group 17 slice 3)
 * so both stay under the 400-line command-handler cap (§9.3). All deletion goes through one
 * shared rail: lock check → texture snapshot → SlotManager.delete → undo record → HUD sync.
 *
 * Depends on: SlotData, SlotManager, TextureStore, UndoManager, LockManager, BlockNotesManager,
 *             ResourcePackServer, HudSync, SlotBlock, Chat, BlockSuggestions
 * Called by:  CommandRegistrar
 */
package com.customblocks.command.handlers;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.UUID;

public final class DeleteCommands {

    private DeleteCommands() {} // static-only

    /** How far the crosshair reaches for /cb delete # (blocks). */
    private static final double REACH = 6.0;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("delete")
                .then(CommandManager.literal("#").executes(DeleteCommands::deleteLooked))
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(BlockSuggestions.IDS)
                        .executes(ctx -> deleteById(ctx, StringArgumentType.getString(ctx, "id")))));
    }

    /** /cb delete <id> — delete by name (unchanged behaviour). */
    private static int deleteById(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        SlotData before = SlotManager.getById(id);
        if (before == null) {
            Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
            return 0;
        }
        return deleteCore(src, before, false);
    }

    /** /cb delete # — delete the custom block the player is looking at (no confirm; undoable). */
    private static int deleteLooked(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Only a player can use /cb delete # — run it in-game while looking at a block.");
            return 0;
        }
        SlotData before = lookedAtCustom(p);
        if (before == null) {
            Chat.error(src, "The block you're looking at isn't a custom block.");
            return 0;
        }
        return deleteCore(src, before, true);
    }

    /**
     * Raycast ~REACH blocks from the player's eyes and return the custom block's SlotData, or null
     * for air, a vanilla block, or an Arabic-letter block (those aren't SlotBlocks).
     */
    private static SlotData lookedAtCustom(ServerPlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1.0f).multiply(REACH));
        BlockHitResult hit = p.getWorld().raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        if (!(p.getWorld().getBlockState(hit.getBlockPos()).getBlock() instanceof SlotBlock sb)) return null;
        return SlotManager.getBySlot(sb.getSlotKey());
    }

    /** Shared deletion rail for both forms. */
    private static int deleteCore(ServerCommandSource src, SlotData before, boolean targeted) {
        String id = before.customId();
        if (LockManager.isLocked(id)) {
            Chat.error(src, "\"" + id + "\" is locked. Use /cb unlock " + id + " to edit it.");
            return 0;
        }
        byte[] texture = TextureStore.load(before.index()); // snapshot so undo can restore pixels
        SlotManager.delete(id);
        BlockNotesManager.onBlockDeleted(id); // clean up orphaned note if any
        ResourcePackServer.updatePack();      // free the slot's texture from the pack
        UndoManager.recordDelete(actor(src), before, texture);
        Chat.success(src, targeted
                ? "Deleted \"" + id + "\" (targeted block). Undo with /cb undo."
                : "Block \"" + id + "\" deleted. You can undo this with /cb undo.");
        if (src.getEntity() instanceof ServerPlayerEntity p) HudSync.sendTo(p);
        return 1;
    }

    /** The acting player's UUID, or null for console/command-block (not undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }
}
