/**
 * SlotBlock.java
 *
 * Responsibility: The generic Minecraft Block that backs one of the pre-registered
 * slots. It holds only its slot index/key; its name (and later its texture, shape,
 * attributes) come from the SlotData assigned to it in SlotManager.
 *
 * Phase 1 (foundation): minimal — registration + display name only. Sounds, shapes,
 * glow, drops, and Arabic joining are added in their phases. Keep this file small.
 *
 * Depends on: SlotData, SlotManager
 * Called by:  SlotManager.registerAll() (registration), the game (rendering/naming)
 */
package com.customblocks.block;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SlotBlock extends Block implements BlockEntityProvider {

    /**
     * Light emission as a real block-state property (0..15). Minecraft bakes a state's
     * luminance ONCE at construction (getLuminance() returns a final field), so dynamic
     * glow MUST live in the state — a luminance lambda reading mutable data is frozen at 0.
     * The model is identical for every value; the pack's "" catch-all variant covers all 16.
     */
    public static final IntProperty LIGHT = IntProperty.of("light", 0, 15);

    private final int slotIndex;
    private final String slotKey;

    public SlotBlock(int slotIndex, Settings settings) {
        super(settings);
        this.slotIndex = slotIndex;
        this.slotKey = "slot_" + slotIndex;
        setDefaultState(getStateManager().getDefaultState().with(LIGHT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    /** New placements inherit the block's configured glow (from SlotData via SlotManager). */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(LIGHT, SlotManager.glowFor(slotIndex));
    }

    public int getSlotIndex() { return slotIndex; }
    public String getSlotKey() { return slotKey; }

    /**
     * Group 14 / Phase 1b: every slot block carries a (data-less) BlockEntity so the client
     * BlockEntityRenderer can draw placed ANIMATED blocks off-atlas (crisp, no mipmap muffle).
     * Static blocks keep their normal atlas model — the renderer simply skips them. The BE holds
     * no state of its own (its slot index is read from this block), so it adds no chunk-save cost
     * beyond the entity's existence and never ticks. Render type stays MODEL (unchanged) until the
     * animated-block model is switched to transparent in a later slice.
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AnimSlotBlockEntity(pos, state);
    }

    /**
     * Live break hardness: read from the slot's SlotData each attempt (hardness isn't baked
     * like luminance, so the override is read every time). Negative = unbreakable, 0 = instant.
     *
     * This is Minecraft's EXACT vanilla formula (AbstractBlockState.calcBlockBreakingDelta),
     * with our live per-block hardness swapped in for the baked value. Lower hardness mines
     * faster, higher mines slower, exactly like vanilla. (The old project used a custom
     * formula that divided by 100 unless speed > 1 — but our blocks aren't in any tool tag,
     * so speed is always 1.0 and it always took the slow ÷100 branch. That was the bug.)
     */
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        SlotData d = SlotManager.getBySlot(slotKey);
        float hardness = d != null ? d.hardness() : SlotData.DEFAULT_HARDNESS;
        if (hardness < 0) return 0f;  // unbreakable (vanilla returns 0 for hardness -1)
        if (hardness == 0) return 1f; // instant (also avoids divide-by-zero)
        int divisor = player.canHarvest(state) ? 30 : 100;
        return player.getBlockBreakingSpeed(state) / hardness / (float) divisor;
    }

    /** Valid keys for /cb setsound — also used for tab suggestions + validation. */
    public static final String[] SOUND_TYPES = {
            "stone", "wood", "grass", "metal", "glass", "sand", "wool", "gravel", "snow",
            "dirt", "coral", "bamboo", "nether_brick", "ice", "honey", "bone", "slime"
    };

    /** Map a sound-type key to a vanilla BlockSoundGroup (unknown → stone). */
    public static BlockSoundGroup soundGroupFor(String type) {
        return switch (type == null ? "" : type) {
            case "wood"         -> BlockSoundGroup.WOOD;
            case "grass"        -> BlockSoundGroup.GRASS;
            case "metal"        -> BlockSoundGroup.METAL;
            case "glass"        -> BlockSoundGroup.GLASS;
            case "sand"         -> BlockSoundGroup.SAND;
            case "wool"         -> BlockSoundGroup.WOOL;
            case "gravel"       -> BlockSoundGroup.GRAVEL;
            case "snow"         -> BlockSoundGroup.SNOW;
            case "dirt"         -> BlockSoundGroup.ROOTED_DIRT;
            case "coral"        -> BlockSoundGroup.WET_GRASS;
            case "bamboo"       -> BlockSoundGroup.BAMBOO;
            case "nether_brick" -> BlockSoundGroup.NETHER_BRICKS;
            case "ice"          -> BlockSoundGroup.GLASS;
            case "honey"        -> BlockSoundGroup.HONEY;
            case "bone"         -> BlockSoundGroup.BONE;
            case "slime"        -> BlockSoundGroup.SLIME;
            default             -> BlockSoundGroup.STONE;
        };
    }

    /** Live sound group: read from SlotData each time, so placed blocks update immediately. */
    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        SlotData d = SlotManager.getBySlot(slotKey);
        return soundGroupFor(d != null ? d.soundType() : SlotData.DEFAULT_SOUND);
    }

    /**
     * Live outline (selection box): matches the block's configured shape, read from SlotData each
     * call so a /cb setshape updates already-placed blocks immediately. (The visible model comes
     * from the resource pack; this is the server-side targeting/highlight box.)
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        SlotData d = SlotManager.getBySlot(slotKey);
        return BlockShapes.outline(d != null ? d.shape() : SlotData.DEFAULT_SHAPE);
    }

    /**
     * Live collision: a "passable" block (noCollision) has an empty collision box, so entities
     * walk through it. Otherwise the collision matches the configured shape (cross is walk-through
     * like a plant). Read from SlotData each call, so placed blocks update immediately.
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        SlotData d = SlotManager.getBySlot(slotKey);
        if (d != null && d.noCollision()) return VoxelShapes.empty();
        return BlockShapes.collision(d != null ? d.shape() : SlotData.DEFAULT_SHAPE);
    }

    @Override
    public MutableText getName() {
        SlotData d = SlotManager.getBySlot(slotKey);
        String name = (d != null) ? d.displayName() : null;
        return Text.literal(name != null ? name : "Custom Block " + slotIndex);
    }

    /** The matching BlockItem for a slot, named from the same SlotData. */
    public static class SlotItem extends BlockItem {
        private final String slotKey;

        public SlotItem(SlotBlock block, Item.Settings settings) {
            super(block, settings);
            this.slotKey = block.getSlotKey();
        }

        @Override
        public Text getName(ItemStack stack) {
            SlotData d = SlotManager.getBySlot(slotKey);
            String name = (d != null) ? d.displayName() : null;
            return Text.literal(name != null ? name : "Custom Block");
        }
    }
}
