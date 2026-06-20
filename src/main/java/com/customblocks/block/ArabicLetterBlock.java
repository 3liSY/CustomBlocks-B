/**
 * ArabicLetterBlock.java — Group 13 / Pass 4 (real feature). The dedicated joinable letter block.
 *
 * A single registered block (customblocks:arabic_letter); the actual letter is per-instance data on
 * its ArabicLetterBlockEntity, read from the placed ItemStack. Distinct from the 1028 SlotBlocks and
 * the 224 static bundled letter blocks — those are untouched (ADR-005 amendment 2026-06-18).
 *
 *   FACING blockstate  : set at placement so the glyph faces the player (a sign). The word runs
 *                        along the horizontal axis perpendicular to FACING; only blocks sharing the
 *                        same FACING join, so two words pointing different ways coexist (4d).
 *   onPlaced           : copy the letter from the stack onto the BlockEntity, then re-flow (4c).
 *   onStateReplaced    : on break, re-flow the ≤2 neighbours so the line re-shapes.
 *
 * The block renders INVISIBLE (no flat-black base model) — the glyph is drawn entirely by
 * ArabicLetterBlockEntityRenderer from a live in-memory texture (no resource pack, no reload).
 * INVISIBLE avoids the black flash on placement, before the BlockEntity syncs its letter to the client.
 *
 * Depends on: ArabicLetterBlockEntity, ArabicLetterRegistry, ArabicJoinFlow
 * Called by:  ArabicLetterRegistry.register() (registration), the game (placement / break / render)
 */
package com.customblocks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class ArabicLetterBlock extends Block implements BlockEntityProvider {

    /** Horizontal facing of the glyph (toward the reader). Word axis is perpendicular to this. */
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    /** Custom-data key carrying the letter codepoint on the held/placed ItemStack. */
    private static final String NBT_LETTER = "ArabicLetter";
    /** Custom-data key carrying the bundled colour name (black/red/green/yellow). */
    private static final String NBT_COLOR = "ArabicColor";
    /** Custom-data key carrying a fixed form (0..3) for searchable decoration variants; absent/-1 = auto-join. */
    private static final String NBT_FORM = "ArabicForm";

    public ArabicLetterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** No flat-black base model — the glyph is 100% the block-entity renderer. Kills the placement flash. */
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    /**
     * Front faces the player (furnace convention): FACING points back toward the placer.
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // 1) Forgiving join (v2 — ALL colours): orient so an adjacent auto-join letter lands on our word
        //    axis (perpendicular to FACING) — a straight row connects whichever way it is built, and any
        //    colour now joins any colour (the black-only boundary is gone).
        Direction f = joinFacing(ctx.getWorld(), ctx.getBlockPos(), ctx.getHorizontalPlayerFacing());
        if (f != null) return getDefaultState().with(FACING, f);

        // 2) Fall back to the furnace convention (glyph faces the placer).
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    /**
     * Facing to use so an adjacent auto-join letter (any colour) ends up on our word axis. If that
     * neighbour's own facing is already perpendicular to the touch direction we copy it (continue its
     * word); else we pick the perpendicular facing nearest the placer so the touch direction becomes the
     * word axis. Null when there is no auto-join neighbour.
     */
    @Nullable
    private static Direction joinFacing(World world, BlockPos pos, Direction playerFacing) {
        if (world == null) return null;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos np = pos.offset(d);
            if (!(world.getBlockState(np).getBlock() instanceof ArabicLetterBlock)) continue;
            if (!(world.getBlockEntity(np) instanceof ArabicLetterBlockEntity nbe)) continue;
            if (nbe.lockedForm() >= 0) continue;            // fixed-form decoration never drives joining
            BlockState ns = world.getBlockState(np);
            Direction nf = ns.contains(FACING) ? ns.get(FACING) : null;
            if (nf != null && nf.getAxis() != d.getAxis()) return nf; // already on its word line — match it
            return perpendicularToward(d, playerFacing);    // re-orient so d becomes our word axis
        }
        return null;
    }

    /** A horizontal facing perpendicular to {@code d}, preferring the one that faces the placer. */
    private static Direction perpendicularToward(Direction d, Direction playerFacing) {
        Direction a = (d.getAxis() == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;
        Direction b = a.getOpposite();
        Direction want = playerFacing.getOpposite();
        return (want == a || want == b) ? want : a;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ArabicLetterBlockEntity(pos, state);
    }

    /**
     * Middle-click (pick block): vanilla's default returns a bare arabic_letter item with no NBT, which
     * renders blank and shows the raw "block.customblocks.arabic_letter" key. Instead read this block's
     * BlockEntity and hand back a properly-stamped stack (letter + colour + form) so the picked item
     * looks right and re-joins when placed.
     */
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof ArabicLetterBlockEntity be && be.letter() != 0) {
            return stackFor(be.letter(), be.color(), be.lockedForm(), 1);
        }
        return super.getPickStack(world, pos, state);
    }

    /** Copy the letter from the stack onto the BlockEntity, then re-flow this block + neighbours. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;
        char letter = letterOf(stack);
        if (world.getBlockEntity(pos) instanceof ArabicLetterBlockEntity be) {
            be.setLetter(letter);
            be.setColor(colorOf(stack));
            be.setLockedForm(lockedFormOf(stack));
        }
        ArabicJoinFlow.onPlace(world, pos);
    }

    /** On break: the BlockEntity here is already gone, so just re-flow the two in-axis neighbours. */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        boolean removed = !state.isOf(newState.getBlock());
        super.onStateReplaced(state, world, pos, newState, moved);
        if (removed && !world.isClient) {
            ArabicJoinFlow.onBreak(world, pos, state.get(FACING));
        }
    }

    // ── ItemStack <-> letter helpers ─────────────────────────────────────────

    /** Build a placeable stack of {@code count} AUTO-JOIN blocks pre-set to {@code letter} (black). */
    public static ItemStack stackFor(char letter, int count) {
        return stackFor(letter, "black", -1, count);
    }

    /**
     * Build a placeable stack.
     * @param letter     the Arabic letter char
     * @param color      bundled colour name (black/red/green/yellow)
     * @param lockedForm -1 = auto-join (form follows neighbours); 0..3 = fixed searchable decoration form
     * @param count      stack size
     */
    public static ItemStack stackFor(char letter, String color, int lockedForm, int count) {
        ItemStack stack = new ItemStack(ArabicLetterRegistry.ITEM, count);
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(NBT_LETTER, letter);
        nbt.putString(NBT_COLOR, (color == null || color.isEmpty()) ? "black" : color);
        if (lockedForm >= 0 && lockedForm <= 3) nbt.putInt(NBT_FORM, lockedForm);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        // The display name is NOT baked here — ArabicLetterItem.getName computes it live from this NBT
        // (ADR-006), so editing a form label re-labels every stack with no reload.
        return stack;
    }

    /** Read the letter codepoint stored on a stack (0 if none). */
    public static char letterOf(ItemStack stack) {
        NbtCompound nbt = customData(stack);
        return (nbt != null && nbt.contains(NBT_LETTER)) ? (char) nbt.getInt(NBT_LETTER) : 0;
    }

    /** Read the bundled colour name stored on a stack ("black" if none). */
    public static String colorOf(ItemStack stack) {
        NbtCompound nbt = customData(stack);
        return (nbt != null && nbt.contains(NBT_COLOR)) ? nbt.getString(NBT_COLOR) : "black";
    }

    /** Read the fixed form (0..3) stored on a stack, or -1 for auto-join. */
    public static int lockedFormOf(ItemStack stack) {
        NbtCompound nbt = customData(stack);
        return (nbt != null && nbt.contains(NBT_FORM)) ? nbt.getInt(NBT_FORM) : -1;
    }

    private static NbtCompound customData(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? null : data.copyNbt();
    }
}
