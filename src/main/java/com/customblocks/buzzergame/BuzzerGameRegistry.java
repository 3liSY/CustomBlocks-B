/**
 * BuzzerGameRegistry.java — Group 31 (BuzzerGame) Phase 1 item 1.
 *
 * Registers the buzzer block, its BlockItem, and its BlockEntityType under {@code customblocks:buzzer}
 * (absorbed into CB-B's main namespace — NOT a separate buzzergame: namespace, per the G31 design lock).
 * Because the item is a real registered {@link net.minecraft.item.Item} (not a slot-system SlotItem),
 * vanilla {@code /give @s customblocks:buzzer} works out of the box; {@code /cb buzzergame give buzzer}
 * and the dedicated BuzzerGame creative tab are the two convenience paths on top of it.
 *
 * Mirrors {@link com.customblocks.block.DeletedMarkerRegistry}. Call {@link #register()} once for the
 * block/item/BE, then {@link #registerTab()} for the creative tab, both from CustomBlocksMod.onInitialize.
 * Later G31 phases add the admin panel + timer screen blocks here alongside the buzzer.
 *
 * Depends on: BuzzerBlock, BuzzerBlockEntity, CustomBlocksMod
 * Called by:  CustomBlocksMod.onInitialize (register + registerTab)
 */
package com.customblocks.buzzergame;

import com.customblocks.CustomBlocksMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BuzzerGameRegistry {

    private BuzzerGameRegistry() {} // static-only

    /** The dedicated "BuzzerGame" creative tab (registered after the CustomBlocks + Tools tabs). */
    public static final RegistryKey<ItemGroup> BUZZERGAME_TAB =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(CustomBlocksMod.MOD_ID, "buzzergame"));

    public static BuzzerBlock BLOCK;
    public static BlockItem ITEM;
    public static BlockEntityType<BuzzerBlockEntity> BLOCK_ENTITY;

    public static AdminPanelBlock ADMIN_PANEL;
    public static BlockItem ADMIN_PANEL_ITEM;
    public static BlockEntityType<AdminPanelBlockEntity> ADMIN_PANEL_ENTITY;

    public static TimerDisplayBlock TIMER_DISPLAY;
    public static BlockItem TIMER_DISPLAY_ITEM;
    public static BlockEntityType<TimerDisplayBlockEntity> TIMER_DISPLAY_ENTITY;

    /** The central BuzzerGame wand — link + resize + more, mode-cycled (op-only). Replaces the old link wand + resize tool. */
    public static BuzzerGameWand WAND;

    /** Register the buzzer + admin panel blocks, their BlockItems, and BlockEntityTypes (main namespace). */
    public static void register() {
        Identifier buzzerId = Identifier.of(CustomBlocksMod.MOD_ID, "buzzer");
        BLOCK = new BuzzerBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.BRIGHT_RED)
                .strength(1.5f, 6.0f)
                .sounds(BlockSoundGroup.METAL)
                .nonOpaque());
        Registry.register(Registries.BLOCK, buzzerId, BLOCK);
        ITEM = new BlockItem(BLOCK, new Item.Settings());
        Registry.register(Registries.ITEM, buzzerId, ITEM);
        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, buzzerId,
                FabricBlockEntityTypeBuilder.create(BuzzerBlockEntity::new, BLOCK).build());

        Identifier panelId = Identifier.of(CustomBlocksMod.MOD_ID, "admin_panel");
        ADMIN_PANEL = new AdminPanelBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .strength(2.0f, 6.0f)
                .sounds(BlockSoundGroup.METAL));
        Registry.register(Registries.BLOCK, panelId, ADMIN_PANEL);
        ADMIN_PANEL_ITEM = new BlockItem(ADMIN_PANEL, new Item.Settings());
        Registry.register(Registries.ITEM, panelId, ADMIN_PANEL_ITEM);
        ADMIN_PANEL_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, panelId,
                FabricBlockEntityTypeBuilder.create(AdminPanelBlockEntity::new, ADMIN_PANEL).build());

        Identifier displayId = Identifier.of(CustomBlocksMod.MOD_ID, "timer_display");
        TIMER_DISPLAY = new TimerDisplayBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .strength(2.0f, 6.0f)
                .sounds(BlockSoundGroup.METAL)
                .nonOpaque());
        Registry.register(Registries.BLOCK, displayId, TIMER_DISPLAY);
        TIMER_DISPLAY_ITEM = new BlockItem(TIMER_DISPLAY, new Item.Settings());
        Registry.register(Registries.ITEM, displayId, TIMER_DISPLAY_ITEM);
        TIMER_DISPLAY_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, displayId,
                FabricBlockEntityTypeBuilder.create(TimerDisplayBlockEntity::new, TIMER_DISPLAY).build());

        Identifier wandId = Identifier.of(CustomBlocksMod.MOD_ID, "buzzergame_wand");
        WAND = new BuzzerGameWand(new Item.Settings().maxCount(1));
        Registry.register(Registries.ITEM, wandId, WAND);

        CustomBlocksMod.LOGGER.info("[CustomBlocks] G31 Phase 1: registered 'buzzer' + 'admin_panel' blocks + BlockEntities + 'buzzergame_wand'.");
    }

    /** Register the dedicated BuzzerGame creative tab (buzzer + admin panel; more blocks later). */
    public static void registerTab() {
        Registry.register(Registries.ITEM_GROUP, BUZZERGAME_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.buzzergame"))
                        .icon(() -> new ItemStack(ITEM))
                        .entries((displayContext, entries) -> {
                            entries.add(ITEM);
                            entries.add(ADMIN_PANEL_ITEM);
                            entries.add(TIMER_DISPLAY_ITEM);
                            entries.add(WAND);
                        })
                        .build());
    }
}
