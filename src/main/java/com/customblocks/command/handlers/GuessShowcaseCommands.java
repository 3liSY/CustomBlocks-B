/**
 * GuessShowcaseCommands.java — Group 30 (Guess Mode) · G30-8b Showcase. OP / level-2 (inherited).
 *
 * The {@code showcase} subtree under {@code /cb guess} — spawn / delete a floating end-crystal-style display:
 *
 *   /cb guess showcase spawn            → spawn a Showcase one block above your feet, showing the bundled "?"
 *   /cb guess showcase spawn <blockid>  → spawn one showing that block's picture
 *   /cb guess showcase delete           → remove the Showcase you're looking at (else the nearest one nearby)
 *
 * Shift-right-clicking a Showcase also removes it (see {@link com.customblocks.block.GuessShowcaseBlock}).
 * The display's look/feel (spin / orbit / size / glow / particles) is tuned on the Showcase tab of
 * {@code /cb guess settings}; only which picture it shows is set here (per display, on its BlockEntity).
 *
 * Depends on: GuessShowcaseRegistry, GuessShowcaseBlockEntity, SlotManager, Chat, BlockSuggestions.
 * Called by:  GuessCommands.register (attaches {@link #node()} under the guess literal).
 */
package com.customblocks.command.handlers;

import com.customblocks.block.GuessShowcaseBlock;
import com.customblocks.block.GuessShowcaseBlockEntity;
import com.customblocks.block.GuessShowcaseRegistry;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class GuessShowcaseCommands {

    private GuessShowcaseCommands() {} // static-only

    /** Blocks within this radius are searched when {@code delete} can't find one directly in the crosshair. */
    private static final int DELETE_RADIUS = 6;

    /** The {@code showcase} literal node, attached under {@code /cb guess} by GuessCommands (op-gated there). */
    public static LiteralArgumentBuilder<ServerCommandSource> node() {
        return CommandManager.literal("showcase")
                .then(CommandManager.literal("spawn")
                        .executes(ctx -> spawn(ctx, null))
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(BlockSuggestions.IDS)
                                .executes(ctx -> spawn(ctx, StringArgumentType.getString(ctx, "id")))))
                .then(CommandManager.literal("delete")
                        .executes(GuessShowcaseCommands::delete));
    }

    /** Spawn a Showcase one block above the op's feet. {@code id} null → the bundled "?" fallback. */
    private static int spawn(CommandContext<ServerCommandSource> ctx, String id) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity op)) {
            Chat.error(src, "Run /cb guess showcase spawn as a player — it spawns where you stand.");
            return 0;
        }
        ServerWorld world = op.getServerWorld();

        // Resolve which picture the display shows.
        int slot = GuessShowcaseBlockEntity.NO_SLOT;
        String label = "the \"?\" cube";
        if (id == null) {
            Chat.info(src, "No block specified — the Showcase will show the \"?\" cube.");
        } else {
            SlotData d = SlotManager.getById(id);
            if (d == null) {
                Chat.error(src, "There's no block called \"" + id + "\". Check /cb list for the right id.");
                return 0;
            }
            slot = d.index();
            label = "\"" + d.displayName() + "\" (" + d.customId() + ")";
        }

        // One block above the feet (WorldEdit "//up 1"); skip up one more if that spot isn't free.
        BlockPos pos = op.getBlockPos().up(1);
        if (!world.getBlockState(pos).isReplaceable()) pos = pos.up(1);
        if (!world.getBlockState(pos).isReplaceable()) {
            Chat.error(src, "No free space above you to spawn a Showcase — move somewhere more open.");
            return 0;
        }

        world.setBlockState(pos, GuessShowcaseRegistry.BLOCK.getDefaultState());
        if (world.getBlockEntity(pos) instanceof GuessShowcaseBlockEntity be) be.setShownSlot(slot);
        Chat.success(src, "Showcase spawned showing " + label + ". Tune it on /cb guess settings → Showcase; "
                + "sneak + right-click it (or /cb guess showcase delete) to remove it.");
        return 1;
    }

    /** Remove the Showcase in the op's crosshair, else the nearest one within {@value #DELETE_RADIUS} blocks. */
    private static int delete(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayerEntity op)) {
            Chat.error(src, "Run /cb guess showcase delete as a player.");
            return 0;
        }
        ServerWorld world = op.getServerWorld();

        // 1) Looking straight at one?
        HitResult hr = op.raycast(8.0, 1.0f, false);
        if (hr instanceof BlockHitResult bhr && isShowcase(world, bhr.getBlockPos())) {
            world.removeBlock(bhr.getBlockPos(), false);
            Chat.success(src, "Showcase removed.");
            return 1;
        }

        // 2) Otherwise the nearest one nearby.
        BlockPos origin = op.getBlockPos();
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -DELETE_RADIUS; dx <= DELETE_RADIUS; dx++)
            for (int dy = -DELETE_RADIUS; dy <= DELETE_RADIUS; dy++)
                for (int dz = -DELETE_RADIUS; dz <= DELETE_RADIUS; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (!isShowcase(world, p)) continue;
                    double sq = p.getSquaredDistance(origin);
                    if (sq < bestSq) { bestSq = sq; best = p; }
                }
        if (best == null) {
            Chat.error(src, "No Showcase nearby to delete. Look at one, or stand within " + DELETE_RADIUS + " blocks.");
            return 0;
        }
        world.removeBlock(best, false);
        Chat.success(src, "Showcase removed.");
        return 1;
    }

    private static boolean isShowcase(ServerWorld world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        return s.getBlock() instanceof GuessShowcaseBlock;
    }
}
