/**
 * AdminPanelBlock.java — Group 31 (BuzzerGame) Phase 1 item 2.
 *
 * The host's control block for a BuzzerGame round. Right-clicking it (operators only — design lock:
 * op/admin-panel-permission for all control actions; regular players can only press buzzers) opens the
 * branded BuzzerGame admin Screen (item 4) via {@link OpenGuiPayload}; the wand instead selects it for
 * linking. Start/Stop/Reset/Reveal also stay available as remote {@code /cb buzzergame ...} commands.
 *
 * BlockWithEntity so an {@link AdminPanelBlockEntity} can own the {@link PanelSession}; render forced to
 * MODEL (BlockWithEntity defaults to INVISIBLE).
 *
 * Depends on: AdminPanelBlockEntity, PanelSession
 * Called by:  BuzzerGameRegistry.register() (registration), the game (placement / right-click readout)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;

import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class AdminPanelBlock extends BlockWithEntity {

    public static final MapCodec<AdminPanelBlock> CODEC = createCodec(AdminPanelBlock::new);

    public AdminPanelBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AdminPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return validateTicker(type, BuzzerGameRegistry.ADMIN_PANEL_ENTITY, AdminPanelBlockEntity::serverTick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        if (!serverPlayer.hasPermissionLevel(2)) {
            Chat.toolError(serverPlayer, "Only operators can use the admin panel.");
            return ActionResult.SUCCESS;
        }
        // BuzzerGame wand in hand → select this panel for linking (handled by BuzzerGameWand), not a readout.
        if (BuzzerGameWand.isWand(serverPlayer.getMainHandStack())) {
            BuzzerGameWand.onPanelClicked(serverPlayer, pos);
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof AdminPanelBlockEntity panel) {
            // Open the branded admin Screen (item 4). Vanilla clients without the mod ignore this payload;
            // start/stop/reset/reveal stay available as remote commands for them.
            ServerPlayNetworking.send(serverPlayer,
                    new OpenGuiPayload(GuiMode.BUZZER_PANEL.id, PanelGui.guiSnapshot(panel.session(), pos.asLong())));
        }
        return ActionResult.SUCCESS;
    }
}
