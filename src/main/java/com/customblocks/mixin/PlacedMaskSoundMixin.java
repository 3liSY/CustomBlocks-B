/**
 * PlacedMaskSoundMixin.java — Group 30 (Guess Mode) · §H Placed Mask Mode. CLIENT-SIDE ONLY.
 *
 * Break-sound leak plug. A watcher who breaks a masked block would hear the REAL block's material — glass,
 * wool, metal — and read the answer straight off it, even though the block looks like the "?" cube. That
 * sound is not played where the block lives: both the breaker and every observer funnel through the same
 * client call, {@code WorldRenderer.processWorldEvent} with {@link net.minecraft.world.WorldEvents#BLOCK_BROKEN}
 * (the breaker gets it locally from {@code Block.onBreak} → {@code ClientWorld.syncWorldEvent}; everyone else
 * gets it as a world-event packet). Hooking that one method covers both.
 *
 * When the position is masked FOR THIS VIEWER the vanilla effect is cancelled and replaced with the neutral
 * mystery pair — a plain stone break sound (the "?" cube has no material of its own) plus the "?" debris the
 * §A particle plug already uses — so breaking still feels responsive and nothing leaks. A viewer with no mask
 * on that block fails the gate immediately and hears the real thing.
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.ClientPlacedMask;
import com.customblocks.client.render.MysteryBreakParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class PlacedMaskSoundMixin {

    @Inject(method = "processWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V", at = @At("HEAD"), cancellable = true)
    private void customblocks$placedMaskBreakEffect(int eventId, BlockPos pos, int data, CallbackInfo ci) {
        if (eventId != WorldEvents.BLOCK_BROKEN || !ClientPlacedMask.any()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc == null ? null : mc.world;
        if (world == null || !ClientPlacedMask.masked(world, pos)) return;
        // Vanilla's own break-effect volume/pitch shape, with the "?" cube's neutral stone material.
        BlockSoundGroup group = BlockSoundGroup.STONE;
        world.playSoundAtBlockCenter(pos, group.getBreakSound(), SoundCategory.BLOCKS,
                (group.getVolume() + 1.0f) / 2.0f, group.getPitch() * 0.8f, false);
        MysteryBreakParticle.spawnBreak(world, pos);   // "?" debris instead of the real block's texture
        ci.cancel();
    }
}
