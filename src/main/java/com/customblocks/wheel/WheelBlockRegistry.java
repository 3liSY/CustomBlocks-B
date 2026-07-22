/**
 * WheelBlockRegistry.java - Group 34 (Wheel of Fortune) v2 items A + F.
 *
 * Registers the wheel's hidden centre anchor block, its BlockItem, and its BlockEntityType under
 * {@code customblocks:wheel_hub}, plus a dedicated "Wheel of Fortune" creative tab. Because the item is a
 * real registered {@link net.minecraft.item.Item}, {@code /give @s customblocks:wheel_hub} works out of the
 * box; {@code /cb wheel} and the creative tab are convenience paths on top of it. Mirrors
 * {@link com.customblocks.buzzergame.BuzzerGameRegistry}.
 *
 * It also holds the SINGLETON pointer (design lock: one wheel per server). The wheel that is currently
 * standing claims it on placement, or on its first tick after a restart; placing a second wheel takes the
 * first one down rather than doubling up.
 *
 * Depends on: WheelBlock, WheelBlockEntity, CustomBlocksMod
 * Called by:  CustomBlocksMod.onInitialize (register + registerTab), WheelBlock / WheelBlockEntity (singleton)
 */
package com.customblocks.wheel;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class WheelBlockRegistry {

    private WheelBlockRegistry() {} // static-only

    /** The dedicated "Wheel of Fortune" creative tab. */
    public static final RegistryKey<ItemGroup> WHEEL_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(CustomBlocksMod.MOD_ID, "wheel"));

    public static WheelBlock BLOCK;
    public static BlockItem ITEM;
    public static BlockEntityType<WheelBlockEntity> BLOCK_ENTITY;

    /** Where the one live wheel is. In-memory only — a restart re-claims it from the wheel's first tick. */
    private static @Nullable BlockPos activePos;
    private static @Nullable RegistryKey<World> activeWorld;

    /** Register the anchor block, its BlockItem, and its BlockEntityType (main namespace). */
    public static void register() {
        Identifier hubId = Identifier.of(CustomBlocksMod.MOD_ID, "wheel_hub");
        BLOCK = new WheelBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.GOLD)
                .strength(0.2f, 6.0f)   // the anchor is invisible; keep it a one-hit removal like the timer stand
                .sounds(BlockSoundGroup.METAL)
                .noCollision()
                .nonOpaque());
        Registry.register(Registries.BLOCK, hubId, BLOCK);
        ITEM = new BlockItem(BLOCK, new net.minecraft.item.Item.Settings());
        Registry.register(Registries.ITEM, hubId, ITEM);
        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, hubId,
                FabricBlockEntityTypeBuilder.create(WheelBlockEntity::new, BLOCK).build());

        CustomBlocksMod.LOGGER.info("[CustomBlocks] G34: registered 'wheel_hub' anchor block + BlockEntity.");
    }

    /** Register the dedicated Wheel of Fortune creative tab (just the wheel item). */
    public static void registerTab() {
        Registry.register(Registries.ITEM_GROUP, WHEEL_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.wheel"))
                        .icon(() -> new ItemStack(ITEM))
                        .entries((displayContext, entries) -> entries.add(ITEM))
                        .build());
    }

    // ------------------------------------------------------------------ singleton pointer

    /** Mark this wheel as THE wheel (on placement, or on its first tick after a restart). */
    public static void claim(ServerWorld world, BlockPos pos) {
        activeWorld = world.getRegistryKey();
        activePos = pos.toImmutable();
    }

    /** Drop the pointer when this wheel comes down. A different wheel's break must not clear it. */
    public static void release(ServerWorld world, BlockPos pos) {
        if (pos.equals(activePos) && world.getRegistryKey().equals(activeWorld)) {
            activePos = null;
            activeWorld = null;
        }
    }

    /**
     * Take down whatever wheel is standing somewhere else (any dimension), so {@code keep} becomes the only
     * one. Returns true if an old wheel was actually removed.
     */
    public static boolean removeOther(ServerWorld world, BlockPos keep) {
        if (activePos == null || activeWorld == null) return false;
        if (activePos.equals(keep) && world.getRegistryKey().equals(activeWorld)) return false;
        BlockPos old = activePos;
        ServerWorld target = world.getServer().getWorld(activeWorld);
        activePos = null;
        activeWorld = null;
        if (target == null || !target.getBlockState(old).isOf(BLOCK)) return false;
        target.removeBlock(old, false); // onStateReplaced despawns that wheel's display entities
        return true;
    }
}
