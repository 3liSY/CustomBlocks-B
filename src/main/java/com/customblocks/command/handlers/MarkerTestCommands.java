/**
 * MarkerTestCommands.java — G06-14 slice 1 TEMP test command.
 *
 * /cb spawnmarker <name> — places a Deleted marker on the face you're aiming at and stamps it with
 * <name>, so you can confirm the floating tag + look-HUD read "Deleted: <name>" and that it breaks
 * instantly with no drop. This is a throwaway harness for slice 1 ONLY — REMOVE once the real delete
 * paths create markers (slice 2+).
 *
 * Depends on: DeletedMarkerRegistry, DeletedMarkerBlockEntity, Chat
 * Called by:  CommandRegistrar (temporary)
 */
package com.customblocks.command.handlers;

import com.customblocks.block.DeletedMarkerBlockEntity;
import com.customblocks.block.DeletedMarkerRegistry;
import com.customblocks.command.Chat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class MarkerTestCommands {

    private MarkerTestCommands() {} // static-only

    /** How far the crosshair reaches to find a face to place against (blocks). */
    private static final double REACH = 6.0;

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("spawnmarker")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> spawn(ctx, StringArgumentType.getString(ctx, "name")))));
    }

    private static int spawn(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Only a player can use /cb spawnmarker — run it in-game while looking at a block.");
            return 0;
        }
        World world = p.getWorld();
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1.0f).multiply(REACH));
        BlockHitResult hit = world.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) {
            Chat.error(src, "Look at a block within 6 blocks, then run /cb spawnmarker <name>.");
            return 0;
        }
        BlockPos place = hit.getBlockPos().offset(hit.getSide());
        if (!world.getBlockState(place).isReplaceable()) {
            Chat.error(src, "No room to place a marker there — aim at an open face.");
            return 0;
        }
        world.setBlockState(place, DeletedMarkerRegistry.BLOCK.getDefaultState());
        String display = name.trim();
        if (world.getBlockEntity(place) instanceof DeletedMarkerBlockEntity be) {
            String slug = display.toLowerCase().replaceAll("[^a-z0-9]+", "_");
            be.setData(slug, display);
            be.sync();
        }
        Chat.success(src, "Placed a Deleted marker for \"" + display + "\". Look at it (tag + HUD), then break it.");
        return 1;
    }
}
