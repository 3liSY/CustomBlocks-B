/**
 * BuzzerBlock.java — Group 31 (BuzzerGame) Phase 1 items 1 + 3 + 5-6.
 *
 * The game-show buzzer players press. Right-click behaviour, in order:
 *   1. Holding the BuzzerGame wand (Link mode) → link this buzzer to the selected admin panel.
 *   2. Linked + a round is live → route the press into the panel's {@link PanelSession#onBuzz} (records
 *      the stopped time / reaction, or a false start), with a chime on accept / a low note on reject.
 *   3. Linked but no round running → a gentle "no round" nudge.
 *   4. Not linked → the standalone item-1 demo press (log + honk + action-bar), so a lone buzzer still works.
 * On break, a linked buzzer auto-unlinks itself from its panel's session and warns nearby hosts.
 * Server is authoritative; the dome always pops via the {@code pressed} state + a scheduled release.
 *
 * Depends on: BuzzerBlockEntity, AdminPanelBlockEntity, PanelSession, BuzzerGameWand, RoundBroadcast, CustomBlocksMod
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
            BuzzerGameWand.onBuzzerClicked(serverPlayer, world, buzzer);
            return ActionResult.SUCCESS;
        }

        // 2/3) Linked to a live panel → route the press into its session.
        AdminPanelBlockEntity panel = linkedPanel(world, buzzer);
        if (panel != null) {
            PanelSession session = panel.session();
            pressDome(serverWorld, pos, state);
            if (session.isBuzzAccepting()) {
                PanelSession.Result r = session.onBuzz(buzzer.getBuzzerId(), serverPlayer.getName().getString());
                panel.markDirty();
                if (r.ok()) playPress(serverWorld, pos); else playReject(serverWorld, pos);
                if (r.ok()) Chat.toolSuccess(serverPlayer, r.message()); else Chat.toolError(serverPlayer, r.message());
            } else {
                playReject(serverWorld, pos);
                Chat.tool(serverPlayer, "No round running on this buzzer's panel yet.");
            }
            return ActionResult.SUCCESS;
        }

        // 4) Unlinked → the standalone demo press (item 1).
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
     * Auto-unlink a linked buzzer from its panel's session when it's broken; warn nearby hosts. Must read
     * this buzzer's BlockEntity BEFORE super (AbstractBlock.onStateReplaced removes the BE on block change).
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof BuzzerBlockEntity buzzer && buzzer.isLinked()
                && world.getBlockEntity(buzzer.getPanelPos()) instanceof AdminPanelBlockEntity panel
                && buzzer.getSessionId() != null && buzzer.getSessionId().equals(panel.session().sessionId())) {
            panel.session().unlinkBuzzer(buzzer.getBuzzerId());
            panel.markDirty();
            if (world instanceof ServerWorld serverWorld) {
                RoundBroadcast.warnNear(serverWorld, buzzer.getPanelPos(), "A linked buzzer was removed.");
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    /** The admin panel this buzzer is linked to, only if the link is still valid (matching session id). */
    private static @Nullable AdminPanelBlockEntity linkedPanel(World world, @Nullable BuzzerBlockEntity buzzer) {
        if (buzzer == null || !buzzer.isLinked()) return null;
        if (world.getBlockEntity(buzzer.getPanelPos()) instanceof AdminPanelBlockEntity panel
                && buzzer.getSessionId() != null && buzzer.getSessionId().equals(panel.session().sessionId())) {
            return panel;
        }
        return null;
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

    /** Reject feedback: a low note + a puff of smoke (too-early / no-round press). */
    private static void playReject(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.9f, 0.8f);
        Vec3d c = Vec3d.ofCenter(pos).add(0, 0.2, 0);
        world.spawnParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.15, 0.15, 0.15, 0.0);
    }
}
