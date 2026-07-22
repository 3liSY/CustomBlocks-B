/**
 * BuzzerBlock.java — Group 31 (BuzzerGame) Phase 1 items 1 + 3 + 5-6.
 *
 * The game-show buzzer players press. Right-click behaviour, in order:
 *   1. Holding the BuzzerGame wand (Link mode) → link this buzzer to the host's wand session.
 *   2. Linked + a round is live → route the press into the host {@link BuzzerSession#onBuzz} (records
 *      the stopped time / reaction, or a false start), with a chime on accept / a low note on reject.
 *   3. Linked but no round running → a gentle "no round" nudge.
 *   4. Not linked (or the host's session is gone) → the standalone demo press (log + honk + action-bar),
 *      so a lone buzzer still works; a stale link (host logged off) is cleared lazily here.
 * On break, a linked buzzer auto-unlinks itself from its host session and warns nearby players.
 * Server is authoritative; the dome always pops via the {@code pressed} state + a scheduled release.
 *
 * Depends on: BuzzerBlockEntity, BuzzerSession, BuzzerSessionManager, BuzzerGameWand, RoundBroadcast, CustomBlocksMod
 * Called by:  BuzzerGameRegistry.register(), the game (placement / press / tick / break)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;

import com.customblocks.CustomBlocksMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class BuzzerBlock extends BlockWithEntity {

    /** True while the dome is pressed down; auto-cleared by the scheduled tick ~1s later. */
    public static final BooleanProperty PRESSED = BooleanProperty.of("pressed");

    public static final MapCodec<BuzzerBlock> CODEC = createCodec(BuzzerBlock::new);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

    /** Ticks the pressed dome stays down before auto-releasing (20 ticks = 1s). */
    private static final int RELEASE_DELAY_TICKS = 20;

    public BuzzerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(PRESSED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PRESSED);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // BlockWithEntity defaults to INVISIBLE — force normal model render.
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BuzzerBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        BuzzerBlockEntity buzzer = world.getBlockEntity(pos) instanceof BuzzerBlockEntity b ? b : null;

        // 1) BuzzerGame wand in hand → linking, not buzzing.
        ItemStack main = serverPlayer.getMainHandStack();
        if (BuzzerGameWand.isWand(main)) {
            BuzzerGameWand.onBuzzerClicked(serverPlayer, buzzer);
            return ActionResult.SUCCESS;
        }

        // 2/3) Linked to a live host session → route the press into the solo stopwatch cycle.
        BuzzerSession session = buzzer != null ? BuzzerSessionManager.bySession(buzzer.getSessionId()) : null;
        if (session != null) {
            pressDome(serverWorld, pos, state);
            BuzzerSession.BuzzOutcome out = session.onBuzz(buzzer.getBuzzerId(), serverPlayer.getName().getString());
            switch (out.phase()) {
                case START -> { fxStart(serverWorld, pos); Chat.toolSuccess(serverPlayer, out.message()); }
                case STOP  -> { fxStop(serverWorld, pos);  Chat.toolSuccess(serverPlayer, out.message()); }
                case RESET -> { fxReset(serverWorld, pos); Chat.tool(serverPlayer, out.message()); }
                case IGNORED -> { playReject(serverWorld, pos); Chat.toolError(serverPlayer, out.message()); }
            }
            return ActionResult.SUCCESS;
        }
        // A link whose host has logged off resolves to no session — clear it so it reads as unlinked.
        if (buzzer != null && buzzer.isLinked()) buzzer.clearLink();

        // 4) Unlinked → the standalone demo press.
        pressDome(serverWorld, pos, state);
        playPress(serverWorld, pos);
        CustomBlocksMod.LOGGER.info("[CustomBlocks] Buzzer pressed at {} by {} (id={})",
                pos, serverPlayer.getName().getString(), buzzer != null ? buzzer.getBuzzerId() : "?");
        Chat.tool(serverPlayer, "[BuzzerGame] Buzz!");
        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(PRESSED)) {
            world.setBlockState(pos, state.with(PRESSED, false));
        }
    }

    /**
     * Auto-unlink a linked buzzer from its host session when it's broken; warn nearby players. Must read
     * this buzzer's BlockEntity BEFORE super (AbstractBlock.onStateReplaced removes the BE on block change).
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof BuzzerBlockEntity buzzer && buzzer.isLinked()) {
            BuzzerSession session = BuzzerSessionManager.bySession(buzzer.getSessionId());
            if (session != null) {
                session.unlinkBuzzer(buzzer.getBuzzerId());
                if (world instanceof ServerWorld serverWorld) {
                    RoundBroadcast.warnNear(serverWorld, pos, "A linked buzzer was removed.");
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private void pressDome(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(PRESSED, true));
        world.scheduleBlockTick(pos, this, RELEASE_DELAY_TICKS);
    }

    /** Press feedback: a bright chime + a small enchant-particle pop above the dome. */
    private static void playPress(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.15f);
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.2, 0);
        world.spawnParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 18, 0.25, 0.25, 0.25, 0.0);
    }

    // ------------------------------------------------------------------ §G press FX — 3 distinct combos
    // Vanilla sounds + particles only (true custom particles need a client mod). Note-block SoundEvents
    // use .value(); every other SoundEvents constant is a bare SoundEvent. Tunable at the call site.

    /** Press 1 (START): a bright rising three-layer cue — pling + bell + a trident-return swell — and a
     *  green happy-villager burst (I8). */
    private static void fxStart(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.35, 0);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 1.0f, 1.5f);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 0.5f, 2.0f);
        world.playSound(null, pos, SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.BLOCKS, 0.6f, 1.5f); // rising swell
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 22, 0.30, 0.30, 0.30, 0.02);
    }

    /** Press 2 (STOP): a hard "locked" combo — basedrum + hat snap + a low anvil clunk — and a bigger crit
     *  burst, clearly different from the start (I8). */
    private static void fxStop(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.35, 0);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundCategory.BLOCKS, 1.0f, 0.9f);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.9f, 1.7f);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.35f, 1.4f); // metallic clunk, low vol
        world.spawnParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 34, 0.40, 0.40, 0.40, 0.15);
    }

    /** Press 3 (RESET): a soft but clearly-audible combo — a hat tick + an item-pickup blip at full volume —
     *  and a cloud puff. Replaces the near-inaudible lone button click (I8: G3 was barely hearable). */
    private static void fxReset(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.30, 0);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.9f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.2f); // audible blip @ vol 1.0
        world.spawnParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 10, 0.18, 0.12, 0.18, 0.0);
    }

    /** Reject feedback: a low note + a puff of smoke (too-early / no-round press). */
    private static void playReject(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.9f, 0.8f);
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.2, 0);
        world.spawnParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.15, 0.15, 0.15, 0.0);
    }
}
