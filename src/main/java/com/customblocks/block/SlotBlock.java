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

import com.customblocks.command.Chat;
import com.customblocks.core.BlockNotesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.LoreFormat;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;

public class SlotBlock extends Block implements BlockEntityProvider, CbBlock {

    /**
     * Group 26 / FIX D — client name seam. On a DEDICATED server the client's SlotManager has no
     * slot data, so getName()/SlotItem.getName() would fall back to "Custom Block". The real name
     * is in the client-only {@code ClientSlotCache} (synced by HudSync on join + after mutations).
     * Common code must not import a client-only class (ADR-009), so the client entrypoint installs
     * this resolver: slotIndex -> display name, or null if unknown. It stays null on a server JVM,
     * and is never consulted in singleplayer (SlotManager is shared in-process there).
     */
    public static volatile IntFunction<String> CLIENT_NAME_RESOLVER = null;

    /**
     * G05-1 / M2 — true while this client is connected to a REMOTE server (dedicated, or a LAN guest).
     * There the local SlotManager holds THIS PC's own stale block list, not the server's, so name/lore
     * resolution must skip it and read the synced ClientSlotCache instead. False on a server JVM, in
     * singleplayer, and on a LAN host (their in-process SlotManager IS the live truth). Set by the
     * client entrypoint on join, reset on disconnect.
     */
    public static volatile boolean CLIENT_REMOTE_SESSION = false;

    /**
     * Resolve a slot's display name. On a server JVM / singleplayer / LAN host the live SlotManager is
     * authoritative. On a remote client ({@link #CLIENT_REMOTE_SESSION}) the local SlotManager is stale,
     * so it is skipped and the synced ClientSlotCache (via {@link #CLIENT_NAME_RESOLVER}) is the only
     * truth, falling back to {@code fallback} on a cache miss. Non-remote behaviour is unchanged.
     */
    public static String resolveName(int slotIndex, String slotKey, String fallback) {
        if (!CLIENT_REMOTE_SESSION) {
            SlotData d = SlotManager.getBySlot(slotKey);
            if (d != null && d.displayName() != null) return d.displayName();
        }
        IntFunction<String> resolver = CLIENT_NAME_RESOLVER;
        if (resolver != null) {
            String cached = resolver.apply(slotIndex);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        return fallback;
    }

    /**
     * Group 30 (Guess Mode) — client name-blank seam. When the LOCAL player is in guess mode, their own
     * client shows a blank/mystery name for a held custom block (or every custom block, in "all" scope)
     * instead of the real name — so they can't read the answer, while everyone else still sees the real
     * name (blinding is purely local, no server sync). Common code can't import a client class (ADR-009),
     * so the client entrypoint installs this: (held stack, slot index) → the blank text, or null to use
     * the real name. Stays null on a server JVM.
     */
    public interface NameDisguise { String blankFor(ItemStack stack, int slotIndex); }

    public static volatile NameDisguise CLIENT_NAME_DISGUISE = null;

    /**
     * Group 18 (REVAMP v2) — client lore seam. Mirrors {@link #CLIENT_NAME_RESOLVER}: on a dedicated
     * server the client has no BlockNotesManager data, so the active lore lines are synced into the
     * client-only ClientSlotCache (via HudSync) and read back through this resolver. Null on a server
     * JVM; never consulted in singleplayer (BlockNotesManager is shared in-process there).
     */
    public static volatile IntFunction<List<String>> CLIENT_LORE_RESOLVER = null;

    /**
     * The block's active lore lines for a slot's item (empty when none/disabled). Server JVM and
     * singleplayer read BlockNotesManager directly (keyed by the block's customId); a dedicated
     * client (empty SlotManager) falls back to the synced cache via {@link #CLIENT_LORE_RESOLVER}.
     */
    public static List<String> resolveLore(int slotIndex, String slotKey) {
        if (!CLIENT_REMOTE_SESSION) {
            SlotData d = SlotManager.getBySlot(slotKey);
            if (d != null) return BlockNotesManager.activeLore(d.customId());
        }
        IntFunction<List<String>> resolver = CLIENT_LORE_RESOLVER;
        List<String> lore = resolver == null ? null : resolver.apply(slotIndex);
        return lore == null ? List.of() : lore;
    }

    // G08 §B/§J — the SHAPE + FACE-ROTATION client seams live in SlotGeometryData (split out for the
    // no-monolith limit; they travel together because §B builds both the collision boxes and the drawn
    // mesh from them). resolveShape below stays here as the name every caller already uses.

    /** @see SlotGeometryData#resolveShape */
    public static String resolveShape(int slotIndex, String slotKey) {
        return SlotGeometryData.resolveShape(slotIndex, slotKey);
    }

    /**
     * S4 client-sound seam — same idea as {@link SlotGeometryData#CLIENT_SHAPE_RESOLVER}. Footstep/break/place sounds are
     * played CLIENT-side from the block's sound group; on a DEDICATED server the client's SlotManager is empty,
     * so {@link #getSoundGroup} fell back to stone and a custom sound (single /cb setsound, or bulk/setall
     * sound) never took effect for the player. The synced sound type lives in the client-only ClientSlotCache
     * ("sound" field, HudSync); the client entrypoint installs this resolver (slotIndex -> sound key, or null).
     * Null on a server JVM, never consulted in singleplayer/LAN host (their in-process SlotManager is truth).
     */
    public static volatile IntFunction<String> CLIENT_SOUND_RESOLVER = null;

    /**
     * Resolve a slot's sound-type key. Server JVM / singleplayer / LAN host read the live SlotManager; a remote
     * client ({@link #CLIENT_REMOTE_SESSION}) skips its stale SlotManager and reads the synced sound via
     * {@link #CLIENT_SOUND_RESOLVER}, falling back to {@link SlotData#DEFAULT_SOUND}.
     */
    public static String resolveSound(int slotIndex, String slotKey) {
        if (!CLIENT_REMOTE_SESSION) {
            SlotData d = SlotManager.getBySlot(slotKey);
            if (d != null) return d.soundType();
        }
        IntFunction<String> resolver = CLIENT_SOUND_RESOLVER;
        if (resolver != null) {
            String cached = resolver.apply(slotIndex);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        return SlotData.DEFAULT_SOUND;
    }

    /**
     * G06-17 client-glow seam — same idea as {@link #CLIENT_SOUND_RESOLVER}. The placed-block light level
     * (LIGHT state) is computed client-side in {@link #getPlacementState} when the client predicts a
     * placement; on a DEDICATED server the client's SlotManager is empty, so glowFor returned 0 and the
     * predicted block was briefly unlit until the server's authoritative state landed ("light appears
     * late"). The synced glow already rides in ClientSlotCache ("glow" field, HudSync); the client
     * entrypoint installs this resolver (slotIndex -> glow 0..15, or null). Null on a server JVM.
     */
    public static volatile IntFunction<Integer> CLIENT_GLOW_RESOLVER = null;

    /**
     * Resolve a slot's glow level (0..15). Server JVM / singleplayer / LAN host read the live SlotManager;
     * a remote client ({@link #CLIENT_REMOTE_SESSION}) skips its stale SlotManager and reads the synced
     * glow via {@link #CLIENT_GLOW_RESOLVER}, falling back to 0.
     */
    public static int resolveGlow(int slotIndex, String slotKey) {
        if (!CLIENT_REMOTE_SESSION) {
            SlotData d = SlotManager.getBySlot(slotKey);
            if (d != null) return d.glow();
        }
        IntFunction<Integer> resolver = CLIENT_GLOW_RESOLVER;
        if (resolver != null) {
            Integer cached = resolver.apply(slotIndex);
            if (cached != null) return Math.max(0, Math.min(15, cached));
        }
        return 0;
    }

    /**
     * Light emission as a real block-state property (0..15). Minecraft bakes a state's
     * luminance ONCE at construction (getLuminance() returns a final field), so dynamic
     * glow MUST live in the state — a luminance lambda reading mutable data is frozen at 0.
     * The model is identical for every value; the pack's "" catch-all variant covers all 16.
     */
    public static final IntProperty LIGHT = IntProperty.of("light", 0, 15);

    // G08 revert (2026-07-20 — OOM incident): shape/facing/half are NOT block-state properties.
    // With ~3100 registered slot blocks, LIGHT(16) × SHAPE(10) × FACING(4) × HALF(2) = 1280 states
    // each = ~3.97 MILLION block-states at registration → OutOfMemoryError in SlotManager.registerAll
    // (the game died on boot before any crash report could write). Shape is data-driven again: it lives
    // on SlotData, collision/outline read it live (getOutlineShape/getCollisionShape), and the rendered
    // model is the single baked model the pack emits for the slot's current shape — /cb setshape rebuilds
    // + pushes the pack (ShapeCommands / HistoryCommands). Only LIGHT stays a property (luminance must be
    // baked per-state). Per-placement stair rotation (§J) is dropped with the properties; see
    // docs/groups Group 08 for the redesign path (BlockEntity-stored orientation).

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

    /** New placements inherit the block's configured glow (G06-17: resolveGlow so a remote client's
     *  predicted placement is lit from the synced cache, not left dark until the server packet lands).
     *  Shape is data-driven (SlotData) — not a placement state — so nothing shape-related is set here. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // G08 §J — a directional shape (stairs) captures its facing + clicked half for onPlaced to stamp on
        // the BlockEntity (there is NO facing/half block-state — the 2026-07-20 OOM revert). Non-directional
        // shapes are a no-op inside the helper and stay exactly as before.
        DirectionalPlacement.capture(ctx, resolveShape(slotIndex, slotKey));
        return getDefaultState().with(LIGHT, resolveGlow(slotIndex, slotKey));
    }

    public int getSlotIndex() { return slotIndex; }
    public String getSlotKey() { return slotKey; }

    /**
     * G13-19 — Deleter contract for a slot block: wipe the slot DEFINITION (texture/name/settings)
     * but leave the placed copy in the world as a broken/empty block (the developer may retexture or
     * recreate and keep the placement). Fully undoable via /cb undo. {@code world}/{@code pos} are
     * unused here — the definition is keyed by slot, not by this one placement. Runs through the one
     * shared rail (G06-14 slice 2): {@link com.customblocks.core.DeletionService#delete}.
     */
    @Override
    public boolean cbDelete(ServerPlayerEntity player, World world, BlockPos pos) {
        SlotData d = SlotManager.getBySlot(slotKey);
        if (d == null) return false; // unassigned slot — nothing to delete
        if (LockManager.isLocked(d.customId())) {
            Chat.lockedTool(player, d.customId());
            return false;
        }
        // All the work (texture snapshot → delete → undo → resync → placed copies become "Deleted:
        // <name>" markers) runs through the one shared rail (G06-14 slice 2).
        com.customblocks.core.DeletionService.delete(player.getServer(), d, player.getUuid());
        Chat.tool(player, "Deleted " + d.customId());
        return true;
    }

    /**
     * Group 14 / Phase 1b: every slot block carries a BlockEntity so the client BlockEntityRenderer
     * can draw placed off-atlas blocks (crisp, no mipmap muffle). Atlas-rendered blocks keep their
     * normal model — the renderer simply skips them. For a normal block the BE holds no state (its
     * slot index is read from this block) and never ticks; a placed ARABIC LETTER additionally
     * carries its glyph facing + back-face mirror partner there (G13-25 CP2 — data-only design,
     * never a blockstate property).
     */
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AnimSlotBlockEntity(pos, state);
    }

    /**
     * G13-25 CP2+CP3(+CP3b) — Arabic auto-join hook, DATA-GATED: for the overwhelming majority of
     * slot blocks (no ArabicMeta) this is a no-op after one map lookup. For a placed letter the
     * flow stamps the glyph facing on the BlockEntity and re-flows the word run by swapping
     * sibling form-slots. Runs on BOTH sides (CP3b): the server authoritatively, the client as an
     * instant prediction of the same result (the old system's dual-side pattern + ADR-009).
     * Nested calls from the flow's own swaps are gated out inside ArabicSlotJoinFlow (inFlow()).
     */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        // G08 §J — stamp the directional orientation captured in getPlacementState onto the BlockEntity
        // (data-only, no block-state); server-authoritative + predicting client. No-op for non-directional.
        DirectionalPlacement.stamp(world, pos, placer, resolveShape(slotIndex, slotKey));
        // Session-aware gate (CP3b): a remote client's own SlotManager is stale — the flow's
        // helper consults the synced cache there, so the prediction actually fires.
        if (com.customblocks.arabic.ArabicSlotJoinFlow.isArabicSlot(world, slotIndex, slotKey)) {
            com.customblocks.arabic.ArabicSlotJoinFlow.onPlaced(world, pos, placer);
        }
    }

    /** G13-25 CP3(+CP3b) — when a placed letter is removed (break / Deleter sweep / swap), re-flow
     *  the neighbouring word runs on both sides. Data-gated + guarded exactly like {@link #onPlaced}. */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        boolean removed = !state.isOf(newState.getBlock());
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!removed) return;
        if (com.customblocks.arabic.ArabicSlotJoinFlow.isArabicSlot(world, slotIndex, slotKey)) {
            com.customblocks.arabic.ArabicSlotJoinFlow.onBreak(world, pos);
        }
    }

    /**
     * G08 §J corners — when a neighbour changes, a stairs-shaped block recomputes its connection shape
     * (inner/outer corner or straight) from its neighbours, exactly like vanilla stairs. Server-side only:
     * the recompute writes the shape to the BlockEntity, which syncs to clients (and the collision/outline
     * read it live). {@link StairConnection#refresh} self-gates — it is a cheap no-op for the ~99% of slot
     * blocks that aren't stairs, and for stairs it only writes (and re-meshes) when the shape actually changed.
     */
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) StairConnection.refresh(world, pos);
    }

    /**
     * Group 30 (§R: R3) — custom blocks drop their own item when broken. Slot blocks are registered
     * without a loot table, so vanilla resolves an EMPTY drop list and breaking one yields nothing.
     * We override the drop directly (code, not a per-slot loot JSON — there are 1028 slots) to return
     * this slot's item. Only the survival break path calls this (creative + piston/replace go through
     * other rails), so it behaves exactly like a normal block. For a guess-mode holder the dropped item
     * ENTITY still renders as the "?" cube (client-side disguise via the item mixins), so the drop keeps
     * the disguise — nothing about the real block leaks through it.
     */
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, net.minecraft.loot.context.LootContextParameterSet.Builder builder) {
        Item item = asItem();
        return item == net.minecraft.item.Items.AIR ? List.of() : List.of(new ItemStack(item));
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

    /**
     * Live sound group. Read through {@link #resolveSound} (not SlotManager directly) so it also works on a
     * DEDICATED client, whose SlotManager is empty — footstep/break/place sounds play client-side, so without
     * this seam a custom sound (single /cb setsound, or bulk/setall sound) never took effect there (S4).
     */
    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return soundGroupFor(resolveSound(slotIndex, slotKey));
    }

    /**
     * Live outline (selection box): matches the block's configured shape, read from SlotData each
     * call so a /cb setshape updates already-placed blocks immediately. (The visible model comes
     * from the resource pack; this is the server-side targeting/highlight box.)
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // resolveShape reads the synced shape on a dedicated client (its SlotManager is empty) so the
        // selection box matches the shape for everyone — no more full-cube box on a carpet (G08 MP fix).
        String shape = resolveShape(slotIndex, slotKey);
        // G08 §J — a directional shape rotates its box to the placed facing/half (read from the BE), so the
        // selection box tracks the same rotation the client draws (both go through BlockShapes.orient).
        if (BlockShapes.isDirectional(shape)) {
            SlotOrientation o = DirectionalPlacement.at(world, pos);
            if (o != null) return BlockShapes.stairOutline(o.shape(), o.facing(), o.half());
        }
        return BlockShapes.outline(shape);
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
        // resolveShape so a dedicated client predicts the right collision box (carpet/slab/…), matching the
        // server, instead of a full cube (G08 MP fix). Passable is server-authoritative (d above).
        String shape = resolveShape(slotIndex, slotKey);
        // G08 §J — directional collision rotates with the placement (BE facing/half), so you walk into the
        // stair exactly where you see it. Same BlockShapes.orient path as the outline + the drawn mesh.
        if (BlockShapes.isDirectional(shape)) {
            SlotOrientation o = DirectionalPlacement.at(world, pos);
            if (o != null) return BlockShapes.stairCollision(o.shape(), o.facing(), o.half());
        }
        return BlockShapes.collision(shape);
    }

    @Override
    public MutableText getName() {
        return Text.literal(resolveName(slotIndex, slotKey, "Custom Block " + slotIndex));
    }

    /** The matching BlockItem for a slot, named from the same SlotData (or synced cache on a server). */
    public static class SlotItem extends BlockItem {
        private final String slotKey;
        private final int slotIndex;

        public SlotItem(SlotBlock block, Item.Settings settings) {
            super(block, settings);
            this.slotKey = block.getSlotKey();
            this.slotIndex = block.getSlotIndex();
        }

        @Override
        public Text getName(ItemStack stack) {
            // Group 30 — a guess-mode holder sees a blank/mystery name for this stack (local only).
            NameDisguise nd = CLIENT_NAME_DISGUISE;
            if (nd != null) {
                String blank = nd.blankFor(stack, slotIndex);
                if (blank != null) return Text.literal(blank);
            }
            return Text.literal(SlotBlock.resolveName(slotIndex, slotKey, "Custom Block"));
        }

        /** Group 18 (REVAMP v2) — show the block's lore lines (when enabled) under the item name. */
        @Override
        public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
            for (String line : SlotBlock.resolveLore(slotIndex, slotKey)) {
                if (line == null || line.isEmpty()) continue;
                // Default vanilla lore look = gray italic; & codes override. Parsed so a colour keeps
                // bold/italic (modern semantics) — '&l' works anywhere, not only after a colour.
                tooltip.add(LoreFormat.parse(line, Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
            }
        }
    }
}
