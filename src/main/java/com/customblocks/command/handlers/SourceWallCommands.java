/**
 * SourceWallCommands.java — GROUP_14 §5d TEMPORARY command (remove when the ~242 re-source job is done).
 *
 * /cb sourcewall        — place every in-scope blurry black-bg / "_black" BASE block in a labelled grid in
 *                         the world in front of you (each block carries a floating tag = its customId), so
 *                         you can SEE the whole re-source backlog at once and fix them with /cb retexture.
 * /cb sourcewall clear  — tear the wall down: removes ONLY the blocks/labels this command placed (recorded
 *                         to disk), never anything you built yourself.
 *
 * Safety (owner ruling 2026-06-28): places into empty/replaceable cells only; records exact positions +
 * label UUIDs so teardown is exact; edits no SlotData / textures / sources (placing blocks ≠ editing slots).
 * This whole command + core/SourceWall are deleted once the re-source job is finished (closeout task).
 *
 * Depends on: SourceWall, SlotManager, SlotBlock, Chat
 * Called by:  CommandRegistrar (temporary)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.SourceWall;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SourceWallCommands {

    private SourceWallCommands() {} // static-only

    private static final int WIDTH = 20; // columns before wrapping to the next row
    private static final int GAP = 2;    // blocks between cells (room for the floating label)
    private static final int AHEAD = 2;  // how far in front of the player the grid starts

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(CommandManager.literal("sourcewall")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> place(ctx.getSource()))
                .then(CommandManager.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    // ── place ────────────────────────────────────────────────────────────────────────────────────────

    private static int place(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) {
            Chat.error(src, "Run /cb sourcewall in-game — it builds the grid in front of you.");
            return 0;
        }
        if (SourceWall.recordExists()) {
            Chat.error(src, "A source wall is already up. Run /cb sourcewall clear before making a new one.");
            return 0;
        }
        MinecraftServer server = src.getServer();
        List<SlotData> snapshot = new ArrayList<>(SlotManager.assignedSlots());
        if (snapshot.isEmpty()) { Chat.error(src, "You have no blocks yet — nothing to put on a wall."); return 0; }
        Chat.info(src, "Scanning " + snapshot.size() + " block(s) for the re-source wall…");

        Thread worker = new Thread(() -> {
            SourceWall.ScopeResult res = SourceWall.scope(snapshot); // heavy: reads textures, off-thread
            if (server == null) return;
            server.execute(() -> build(src, p, res)); // mutate world back on the server thread
        }, "CustomBlocks-SourceWall");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    private static void build(ServerCommandSource src, ServerPlayerEntity p, SourceWall.ScopeResult res) {
        List<SourceWall.Target> scope = res.targets();
        if (scope.isEmpty()) {
            Chat.error(src, "No in-scope blocks found (black/transparent bases or gifs). Skipped "
                    + res.skippedVariant() + " variants, " + res.skippedArabic() + " arabic, "
                    + res.skippedNonBlack() + " coloured-bg bases.");
            return;
        }
        ServerWorld world = (ServerWorld) p.getWorld();
        Direction facing = p.getHorizontalFacing();
        Direction right = facing.rotateYClockwise();
        BlockPos origin = p.getBlockPos().offset(facing, AHEAD);

        SourceWall.Placed rec = new SourceWall.Placed();
        rec.dim = world.getRegistryKey().getValue().toString();
        int placed = 0, skipped = 0;

        for (int i = 0; i < scope.size(); i++) {
            SourceWall.Target t = scope.get(i);
            SlotBlock block = SlotManager.blockAt(t.index());
            if (block == null) { skipped++; continue; }
            int col = i % WIDTH, row = i / WIDTH;
            BlockPos pos = origin.offset(right, col * GAP).offset(facing, row * GAP);
            if (!world.getBlockState(pos).isReplaceable()) { skipped++; continue; } // never overwrite

            world.setBlockState(pos, block.getDefaultState()
                    .with(SlotBlock.LIGHT, SlotManager.glowFor(t.index())));
            rec.blocks.add(new int[]{pos.getX(), pos.getY(), pos.getZ()});
            spawnLabel(world, pos, t.id(), rec);
            placed++;
        }

        boolean saved = SourceWall.saveRecord(rec);
        Chat.success(src, "Source wall up — placed " + CbFmt.BODY + placed + CbFmt.RESET + " of " + scope.size()
                + " (" + CbFmt.BODY + res.blackBases() + CbFmt.RESET + " black/transparent bases + " + CbFmt.BODY + res.gifs() + CbFmt.RESET + " gifs)"
                + (skipped > 0 ? " " + CbFmt.DIM + "(" + skipped + " had no room)" : "") + ".");
        Chat.line(src, Text.literal(CbFmt.DIM + "Off-wall: " + CbFmt.BODY + res.skippedVariant() + CbFmt.DIM + " colour variants, " + CbFmt.BODY
                + res.skippedArabic() + CbFmt.DIM + " arabic, " + CbFmt.BODY + res.skippedNonBlack() + CbFmt.DIM + " coloured-bg bases."));
        Chat.line(src, Text.literal(CbFmt.DIM + "Each tag = id. Fix one: " + CbFmt.BODY + "/cb retexture <id> <url>" + CbFmt.DIM + ". Remove: " + CbFmt.BODY + "/cb sourcewall clear"));
        if (!saved) {
            Chat.error(src, "Couldn't save the teardown record — /cb sourcewall clear may miss some blocks; "
                    + "break any leftovers by hand if that happens.");
        }
    }

    /** Floating id label = an invisible, weightless, invulnerable armor stand with a visible custom name. */
    private static void spawnLabel(ServerWorld world, BlockPos pos, String id, SourceWall.Placed rec) {
        try {
            ArmorStandEntity stand = new ArmorStandEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            stand.setInvisible(true);
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setCustomName(Text.literal(id));
            stand.setCustomNameVisible(true);
            if (world.spawnEntity(stand)) rec.stands.add(stand.getUuidAsString());
        } catch (Exception ignored) {
            // a missing label is cosmetic — the block is still placed and recorded
        }
    }

    // ── clear ────────────────────────────────────────────────────────────────────────────────────────

    private static int clear(ServerCommandSource src) {
        SourceWall.Placed rec = SourceWall.loadRecord();
        if (rec == null || rec.blocks == null) {
            Chat.error(src, "No source wall recorded — nothing to clear.");
            return 0;
        }
        ServerWorld world = resolveWorld(src, rec.dim);
        if (world == null) { Chat.error(src, "Couldn't find the wall's world to clear it."); return 0; }

        int removedBlocks = 0;
        for (int[] xyz : rec.blocks) {
            BlockPos pos = new BlockPos(xyz[0], xyz[1], xyz[2]);
            // Only remove a cell that is STILL one of our slot blocks — never touch what the owner built.
            if (world.getBlockState(pos).getBlock() instanceof SlotBlock) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                removedBlocks++;
            }
        }
        int removedLabels = 0;
        if (rec.stands != null) {
            for (String u : rec.stands) {
                try {
                    Entity e = world.getEntity(UUID.fromString(u));
                    if (e instanceof ArmorStandEntity) { e.discard(); removedLabels++; }
                } catch (Exception ignored) {
                    // bad/old uuid — skip
                }
            }
        }
        SourceWall.deleteRecord();
        Chat.success(src, "Source wall cleared — removed " + CbFmt.BODY + removedBlocks + CbFmt.RESET + " block(s) + " + CbFmt.BODY
                + removedLabels + CbFmt.RESET + " label(s). Your own builds were left alone.");
        return 1;
    }

    private static ServerWorld resolveWorld(ServerCommandSource src, String dim) {
        MinecraftServer server = src.getServer();
        if (server != null && dim != null) {
            for (ServerWorld w : server.getWorlds()) {
                if (w.getRegistryKey().getValue().toString().equals(dim)) return w;
            }
        }
        if (src.getEntity() instanceof ServerPlayerEntity p) return (ServerWorld) p.getWorld();
        return server != null ? server.getOverworld() : null;
    }
}
