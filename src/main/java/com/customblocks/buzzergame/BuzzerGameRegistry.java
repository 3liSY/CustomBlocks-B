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
 * blocks/items/BEs + the wand, then {@link #registerTab()} for the creative tab, both from
 * CustomBlocksMod.onInitialize. Registers the buzzer, the timer stand, and the session-owning wand — the
 * old admin panel block was scrapped for the wand-owned session (G31 redesign 2026-07-18).
 *
 * Depends on: BuzzerBlock, BuzzerBlockEntity, TimerDisplayBlock, TimerDisplayBlockEntity, BuzzerGameWand, CustomBlocksMod
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

    public static TimerDisplayBlock TIMER_DISPLAY;
    public static BlockItem TIMER_DISPLAY_ITEM;
    public static BlockEntityType<TimerDisplayBlockEntity> TIMER_DISPLAY_ENTITY;

    /** The central BuzzerGame wand — link + resize + more, mode-cycled (op-only). Replaces the old link wand + resize tool. */
    public static BuzzerGameWand WAND;

    /** Render-only part items (item F): each is the ItemStack a stand-part ITEM_DISPLAY renders. Not in any
     *  tab, not obtainable — they exist purely so the base/leg/screen can be shown + resized independently. */
    public static Item TIMER_PART_BASE;
    public static Item TIMER_PART_LEG;
    public static Item TIMER_PART_SCREEN;

    /** Render-only label items (item E, rev 2026-07-19): the two fixed Arabic words الهدف / النتيجة, each a flat
     *  green quad rendered by an ITEM_DISPLAY glued to the screen. Replaces the old customblocks:timer_label
     *  bitmap font (I1) so the labels never fight MC bidi / the 256px atlas page / a PUA-codepoint match again. */
    public static Item TIMER_LABEL_TARGET;
    public static Item TIMER_LABEL_RESULT;

    /** Register the buzzer + timer stand blocks, their BlockItems, and BlockEntityTypes (main namespace). */
    public static void register() {
        Identifier buzzerId = Identifier.of(CustomBlocksMod.MOD_ID, "buzzer");
        BLOCK = new BuzzerBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.BRIGHT_RED)
                .strength(0.2f, 6.0f) // near-instant survival break (G31 §C); resistance kept
                .sounds(BlockSoundGroup.METAL)
                .nonOpaque());
        Registry.register(Registries.BLOCK, buzzerId, BLOCK);
        ITEM = new BlockItem(BLOCK, new Item.Settings());
        Registry.register(Registries.ITEM, buzzerId, ITEM);
        BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, buzzerId,
                FabricBlockEntityTypeBuilder.create(BuzzerBlockEntity::new, BLOCK).build());

        Identifier displayId = Identifier.of(CustomBlocksMod.MOD_ID, "timer_display");
        TIMER_DISPLAY = new TimerDisplayBlock(AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .strength(0.05f, 6.0f) // true one-hit break (I5): the block is INVISIBLE so there's no crack
                                       // overlay to sell a 0.2 mine — drop hardness to read as instant like the buzzer
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

        // Render-only stand-part items (item F) — no tab, no recipe; only ever rendered by display entities.
        TIMER_PART_BASE = registerPart("timer_part_base");
        TIMER_PART_LEG = registerPart("timer_part_leg");
        TIMER_PART_SCREEN = registerPart("timer_part_screen");

        // Render-only Arabic-label items (item E, rev 2026-07-19) — the two screen words as flat quads.
        TIMER_LABEL_TARGET = registerPart("timer_label_target");
        TIMER_LABEL_RESULT = registerPart("timer_label_result");

        CustomBlocksMod.LOGGER.info("[CustomBlocks] G31: registered 'buzzer' + 'timer_display' blocks + BlockEntities + 'buzzergame_wand' + stand parts.");
    }

    /** Register one render-only stand-part item under {@code customblocks:<name>}. */
    private static Item registerPart(String name) {
        Item item = new Item(new Item.Settings());
        Registry.register(Registries.ITEM, Identifier.of(CustomBlocksMod.MOD_ID, name), item);
        return item;
    }

    /** Register the dedicated BuzzerGame creative tab (buzzer + timer stand + wand). */
    public static void registerTab() {
        Registry.register(Registries.ITEM_GROUP, BUZZERGAME_TAB,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.customblocks.buzzergame"))
                        .icon(() -> new ItemStack(ITEM))
                        .entries((displayContext, entries) -> {
                            entries.add(ITEM);
                            entries.add(TIMER_DISPLAY_ITEM);
                            entries.add(WAND);
                        })
                        .build());
    }
}
