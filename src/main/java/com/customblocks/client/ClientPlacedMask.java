/**
 * ClientPlacedMask.java — Group 30 (Guess Mode) · §H Placed Mask Mode. CLIENT-SIDE ONLY.
 *
 * Client cache of "which placed blocks must I see as the bundled '?'", fed by
 * {@link com.customblocks.network.payloads.PlacedMaskPayload}. The server already removed the viewer's OWN
 * masked placements from the feed, so this class needs no owner bookkeeping at all: every position it holds
 * is one this client must hide. That is the whole §H render predicate — viewer identity, decided server-side.
 *
 * It is read from real hot paths (the placed-block renderer runs per block entity per frame, the outline
 * mixin runs per raycast), so the lookup is built for that:
 *   • {@link #EMPTY} short-circuits every call to a single volatile boolean read while nobody is running the
 *     mode — which is the normal state of a server;
 *   • positions live in a fastutil {@code LongOpenHashSet} keyed by dimension id, so a hit is one primitive
 *     hash probe with no boxing;
 *   • the current dimension's set is memoised against the world's {@code RegistryKey}, so walking through a
 *     portal switches sets without a per-call map lookup or a String allocation.
 *
 * Thread safety: written on the network thread, read on the render thread → volatile map swap, same pattern
 * as {@link ClientGuessState} / {@link ClientSlotCache}.
 *
 * Depends on: fastutil, MinecraftClient (current world).
 * Called by:  CustomBlocksClient (populate/clear), GuessDisguise, AnimSlotBER, HudRenderer, the §H mixins.
 */
package com.customblocks.client;

import com.customblocks.network.payloads.PlacedMaskPayload;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class ClientPlacedMask {

    /** dimension id → the packed positions this client must draw as "?". Replaced wholesale, never mutated. */
    private static volatile Map<String, LongSet> STATE = Map.of();

    /** True while nothing anywhere is masked — the one read every hot-path call starts with. */
    private static volatile boolean EMPTY = true;

    /**
     * Memoised "set for the world we are rendering". It carries the STATE it was built from, so a memo is
     * only ever reused while that exact picture is still current — a feed update invalidates it implicitly
     * and no reader can be handed a stale set. RegistryKey instances are interned by the registry, so the
     * compare is a reference hit and the hot path never allocates a dimension-id String.
     */
    private record Memo(Map<String, LongSet> state, RegistryKey<World> key, LongSet set) {}

    private static volatile Memo memo;

    private ClientPlacedMask() {} // static-only

    // ── Feed ─────────────────────────────────────────────────────────────────

    /** Route one payload by its op, so the receiver stays a single line. */
    public static void apply(byte op, Map<String, LongList> dims) {
        switch (op) {
            case PlacedMaskPayload.OP_ADD -> add(dims);
            case PlacedMaskPayload.OP_DEL -> remove(dims);
            default -> full(dims);                          // OP_FULL, and anything unrecognised, replaces
        }
    }

    /** Replace everything (join / toggle / a runner turning the mode off). */
    public static void full(Map<String, LongList> dims) {
        Map<String, LongSet> map = new HashMap<>();
        dims.forEach((dim, list) -> map.put(dim, new LongOpenHashSet(list)));
        swap(map);
    }

    /** One or more positions just became masked for this viewer. */
    public static void add(Map<String, LongList> dims) {
        Map<String, LongSet> map = copy();
        dims.forEach((dim, list) -> map.computeIfAbsent(dim, k -> new LongOpenHashSet()).addAll(list));
        swap(map);
    }

    /** One or more positions stopped being masked (their block was broken). */
    public static void remove(Map<String, LongList> dims) {
        Map<String, LongSet> map = copy();
        dims.forEach((dim, list) -> {
            LongSet set = map.get(dim);
            if (set == null) return;
            set.removeAll(list);
            if (set.isEmpty()) map.remove(dim);
        });
        swap(map);
    }

    /** Drop everything (disconnect) so another server's session never bleeds through. */
    public static void clear() { swap(Map.of()); }

    // ── Lookup ───────────────────────────────────────────────────────────────

    /** Must this client draw the block at {@code pos} in {@code world} as the bundled "?" cube? */
    public static boolean masked(World world, BlockPos pos) {
        if (EMPTY || world == null || pos == null) return false;
        LongSet set = setFor(world);
        return set != null && set.contains(pos.asLong());
    }

    /** Same question for the world the local client is currently in (callers without a world handy). */
    public static boolean maskedHere(BlockPos pos) {
        if (EMPTY || pos == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && masked(mc.world, pos);
    }

    /** Is anything masked at all? Cheapest possible pre-check for a caller doing more work afterwards. */
    public static boolean any() { return !EMPTY; }

    // ── Internals ────────────────────────────────────────────────────────────

    private static LongSet setFor(World world) {
        Map<String, LongSet> state = STATE;
        RegistryKey<World> key = world.getRegistryKey();
        Memo m = memo;
        if (m != null && m.state() == state && m.key() == key) return m.set();
        LongSet set = state.get(key.getValue().toString());
        memo = new Memo(state, key, set);
        return set;
    }

    private static Map<String, LongSet> copy() {
        Map<String, LongSet> map = new HashMap<>();
        STATE.forEach((dim, set) -> map.put(dim, new LongOpenHashSet(set)));
        return map;
    }

    /** Publish a new picture. STATE first, then EMPTY — the memo keys off STATE, so it expires by itself. */
    private static void swap(Map<String, LongSet> map) {
        STATE = Map.copyOf(map);
        EMPTY = map.isEmpty();
    }
}
