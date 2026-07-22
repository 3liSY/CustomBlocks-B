/**
 * SaucedEntities.java — Group 32 (Explosive Tomato) Phase E, E6/E7 shield tint. CLIENT-ONLY.
 *
 * Answers "is this entity currently sauced?" for the shield-tint mixin ({@code ShieldSauceTintMixin}). An entity
 * counts as sauced while it is standing on/in a sauce puddle (the block state is synced, so this needs no packet),
 * and the LOCAL player additionally counts while the E8 screen overlay is running — so a point-blank blocker's
 * shield reddens the instant the blast lands, before the sauce field has finished settling around them.
 *
 * Depends on: SauceRegistry (the common sauce block), SauceScreenOverlay (local-hit state)
 * Called by:  ShieldSauceTintMixin.customblocks$sauceTint
 */
package com.customblocks.client;

import com.customblocks.tomato.SauceRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SaucedEntities {

    private SaucedEntities() {}

    public static boolean isSauced(LivingEntity entity) {
        if (entity == null) return false;
        if (entity == MinecraftClient.getInstance().player && SauceScreenOverlay.active()) return true;
        World world = entity.getWorld();
        if (world == null) return false;
        BlockPos feet = entity.getBlockPos();
        return world.getBlockState(feet).isOf(SauceRegistry.SAUCE)
                || world.getBlockState(feet.down()).isOf(SauceRegistry.SAUCE);
    }
}
