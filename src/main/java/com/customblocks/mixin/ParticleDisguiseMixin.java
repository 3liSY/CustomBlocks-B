/**
 * ParticleDisguiseMixin.java — Group 30 (Guess Mode) · G30 §R Total Blindness. CLIENT-SIDE ONLY.
 *
 * Particle leak plug: the flagged holder must not be able to identify a disguised block by the debris/crack
 * particles it throws — those sample the REAL block texture, so they'd leak the answer even though the block
 * looks like the "?" cube. When the LOCAL player has the block flagged ({@link GuessDisguise#blinds}) both
 * real particle bursts are cancelled on the holder's client and REPLACED with "?"-textured debris
 * ({@link com.customblocks.client.render.MysteryBreakParticle}) so the break still feels responsive without
 * revealing the block (G30 §R: R2):
 *   • {@code addBlockBreakParticles} — the shatter puff at the moment it breaks (state handed in);
 *   • {@code addBlockBreakingParticles} — the little chips that fly while mining (state looked up from pos).
 * The break/mining SOUND leak is a separate follow-up (§R-2). A WATCHER (not flagged) fails the gate → real
 * particles show. Client-only mixin (the ParticleManager is client-only anyway).
 *
 * Registered as a client mixin in customblocks.mixins.json.
 */
package com.customblocks.mixin;

import com.customblocks.client.render.GuessDisguise;
import com.customblocks.client.render.MysteryBreakParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ParticleManager.class)
public abstract class ParticleDisguiseMixin {

    /** The shatter burst when the block breaks — the state is handed in, so gate on it directly. */
    @Inject(
        method = "addBlockBreakParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V",
        at = @At("HEAD"), cancellable = true)
    private void customblocks$guessBlindBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!GuessDisguise.blinds(state)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) MysteryBreakParticle.spawnBreak(mc.world, pos); // "?" debris instead
        ci.cancel();                                                                        // kill the leaking real burst
    }

    /** The chips that fly off while mining — only pos + face given, so look up the state to gate. */
    @Inject(
        method = "addBlockBreakingParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)V",
        at = @At("HEAD"), cancellable = true)
    private void customblocks$guessBlindDiggingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || !GuessDisguise.blinds(mc.world.getBlockState(pos))) return;
        MysteryBreakParticle.spawnDig(mc.world, pos, direction); // one "?" chip instead of the real one
        ci.cancel();
    }
}
