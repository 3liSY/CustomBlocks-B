/**
 * PlacedMaskStore.java — Group 30 (Guess Mode) · §H Placed Mask Mode. SERVER source of truth.
 *
 * Responsibility: which players are RUNNING Placed Mask Mode (/cb guess placed) and, per runner, the exact
 * set of block positions they masked while it was on. §H is the INVERSE of §A: §A blinds one flagged holder
 * while watchers keep the truth; §H blinds every watcher while the one runner keeps the truth.
 *
 * Two locked rules from the Group doc drive the shape of this file:
 *   • the mask is recorded PER PLACEMENT, never inferred from block type — so toggling off restores exactly
 *     the blocks that were hidden and nothing else (a block placed before the toggle stays real forever);
 *   • the render predicate is VIEWER IDENTITY only — the runner sees their own placements real, everybody
 *     else sees the bundled "?". Operator/admin permission decides who may RUN the command, never who may
 *     see through a mask ({@link #feedFor} is where that is enforced, on the server).
 *
 * Storage mirrors {@link GuessModeStore}: one JSON file, {@code synchronized} static API, atomic tmp→
 * ATOMIC_MOVE write, static self-load. Positions are packed {@link BlockPos#asLong()} keyed by dimension id,
 * so the same coordinates in the Nether and the Overworld are two different records. Unlike GuessModeStore
 * the mutations here run per BLOCK PLACEMENT, so writes are debounced ({@link #tick}) instead of immediate.
 *
 * config/customblocks/placedmask.json:
 *   { "&lt;uuid&gt;": { "on": bool, "pos": { "minecraft:overworld": [long, long, …] } } }
 *
 * Depends on: (none — standalone, like GuessModeStore)
 * Called by:  PlacedMaskCommands (toggle), SlotBlock (record on place / release on break), GuessSync
 *             (per-viewer feed), PlacedMaskTag (drop tagging), CustomBlocksMod (tick/flush).
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlacedMaskStore {

    private static final String FILE = "config/customblocks/placedmask.json";
    private static final Gson GSON = new Gson();

    /**
     * Group doc §H: "the stored position set is capped, and reaching the cap reports a clear message instead
     * of silently dropping masks". Per RUNNER, so two runners never eat each other's budget.
     */
    public static final int MAX_POSITIONS = 20_000;

    /** How often the debounced writer actually touches disk (server ticks — 5 s). */
    private static final int SAVE_INTERVAL_TICKS = 100;

    /** Runners with the mode currently ON. Toggling off clears their positions, so this set is small. */
    private static final Set<String> ON = new LinkedHashSet<>();

    /** dimension id → (packed BlockPos → owning runner uuid). One lookup answers "who masked this block?". */
    private static final Map<String, Long2ObjectMap<String>> OWNERS = new LinkedHashMap<>();

    /** runner uuid → how many positions they currently own (kept in step with OWNERS so the cap is O(1)). */
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();

    /**
     * The position released by the most recent break, kept for exactly one step. Minecraft removes the block
     * (→ {@code onStateReplaced} → {@link #release}) BEFORE it rolls the drops (→ {@code getDroppedStacks}),
     * so by drop time the live record is already gone. One remembered entry bridges that gap: a drop is
     * tagged only when the dimension AND position match exactly, so a stale value can never mask a wrong item.
     */
    private static String lastDim, lastOwner;
    private static long lastPos;

    private static boolean dirty;
    private static int tickCounter;

    static { load(); }

    private PlacedMaskStore() {} // static-only

    // ── Mode toggle ──────────────────────────────────────────────────────────

    /** Is this player currently RUNNING the mode (so their new placements get recorded)? */
    public static synchronized boolean isRunning(UUID player) {
        return ON.contains(player.toString());
    }

    /** /cb guess placed — flip the mode for one player; returns the new state. Off also unmasks everything. */
    public static synchronized boolean toggle(UUID player) {
        String key = player.toString();
        boolean on = !ON.contains(key);
        if (on) ON.add(key); else { ON.remove(key); clearPositions(key); }
        dirty = true;
        flush();                      // a toggle is rare and meaningful — persist it right away
        return on;
    }

    /** How many positions this runner currently has masked. */
    public static synchronized int count(UUID player) {
        return COUNTS.getOrDefault(player.toString(), 0);
    }

    // ── Per-placement records ────────────────────────────────────────────────

    /**
     * Record one placement the runner just made. Returns false when the runner is not running the mode, is
     * already at {@link #MAX_POSITIONS} (the caller says so plainly — the block simply stays visible), or the
     * position is somehow already masked.
     */
    public static synchronized boolean record(UUID runner, World world, BlockPos pos) {
        String key = runner.toString();
        if (!ON.contains(key)) return false;
        if (COUNTS.getOrDefault(key, 0) >= MAX_POSITIONS) return false;
        Long2ObjectMap<String> dim = OWNERS.computeIfAbsent(dimId(world), k -> new Long2ObjectOpenHashMap<>());
        if (dim.putIfAbsent(pos.asLong(), key) != null) return false;   // already masked by someone
        COUNTS.merge(key, 1, Integer::sum);
        dirty = true;
        return true;
    }

    /** True when this runner has no room left — the caller turns that into one clear chat line. */
    public static synchronized boolean atCap(UUID runner) {
        return COUNTS.getOrDefault(runner.toString(), 0) >= MAX_POSITIONS;
    }

    /**
     * Release the record for a position that stopped existing (broken, exploded, pushed, swept). Returns the
     * owning runner's uuid string when one was actually released, else null. The released entry is remembered
     * for one step so the drop rolled right after can still be tagged (see {@link #ownerForDrop}).
     */
    public static synchronized String release(World world, BlockPos pos) {
        String dimKey = dimId(world);
        Long2ObjectMap<String> dim = OWNERS.get(dimKey);
        if (dim == null) return null;
        String owner = dim.remove(pos.asLong());
        if (owner == null) return null;
        if (dim.isEmpty()) OWNERS.remove(dimKey);
        COUNTS.computeIfPresent(owner, (k, v) -> v <= 1 ? null : v - 1);
        lastDim = dimKey; lastPos = pos.asLong(); lastOwner = owner;
        dirty = true;
        return owner;
    }

    /** Who masked this position — live record first, then the just-released one (drop-time lookup). */
    public static synchronized String ownerForDrop(World world, BlockPos pos) {
        String live = ownerAt(world, pos);
        if (live != null) return live;
        return (lastOwner != null && dimId(world).equals(lastDim) && lastPos == pos.asLong()) ? lastOwner : null;
    }

    /** Who masked this position right now, or null when it is not masked. */
    public static synchronized String ownerAt(World world, BlockPos pos) {
        Long2ObjectMap<String> dim = OWNERS.get(dimId(world));
        return dim == null ? null : dim.get(pos.asLong());
    }

    // ── Broadcast feed ───────────────────────────────────────────────────────

    /**
     * Every masked position the given VIEWER must see as "?" — that is, every runner's positions except the
     * viewer's own. This one method is the whole render contract: a runner is excluded from their own mask
     * HERE, on the server, so no client is ever trusted to hide the answer from itself.
     * Returns dimension id → packed positions (empty map when nothing is masked for them).
     */
    public static synchronized Map<String, LongList> feedFor(UUID viewer) {
        String self = viewer.toString();
        Map<String, LongList> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long2ObjectMap<String>> e : OWNERS.entrySet()) {
            LongList list = new LongArrayList();
            for (Long2ObjectMap.Entry<String> pe : e.getValue().long2ObjectEntrySet())
                if (!self.equals(pe.getValue())) list.add(pe.getLongKey());
            if (!list.isEmpty()) out.put(e.getKey(), list);
        }
        return out;
    }

    /** The dimension id used as this store's per-world key. */
    public static String dimId(World world) {
        return world == null ? "" : world.getRegistryKey().getValue().toString();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    /** Debounced writer — placements mutate this store per block, so disk is touched on a timer, not per edit. */
    public static synchronized void tick() {
        if (++tickCounter < SAVE_INTERVAL_TICKS) return;
        tickCounter = 0;
        flush();
    }

    /** Write now when there is anything to write (server stop, toggle). */
    public static synchronized void flush() {
        if (!dirty) return;
        dirty = false;
        save();
    }

    // ── Internals ────────────────────────────────────────────────────────────

    /** Drop every position a runner owns (their toggle-off "reveal everything" step). */
    private static void clearPositions(String runner) {
        for (Long2ObjectMap<String> dim : OWNERS.values()) {
            Iterator<Long2ObjectMap.Entry<String>> it = dim.long2ObjectEntrySet().iterator();
            while (it.hasNext()) if (runner.equals(it.next().getValue())) it.remove();
        }
        OWNERS.values().removeIf(Long2ObjectMap::isEmpty);
        COUNTS.remove(runner);
        if (runner.equals(lastOwner)) lastOwner = null;
    }

    private static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            for (var e : root.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                String runner = e.getKey();
                try { UUID.fromString(runner); } catch (IllegalArgumentException bad) { continue; }
                JsonObject j = e.getValue().getAsJsonObject();
                if (j.has("on") && j.get("on").getAsBoolean()) ON.add(runner);
                if (!j.has("pos") || !j.get("pos").isJsonObject()) continue;
                for (var de : j.getAsJsonObject("pos").entrySet()) {
                    if (!de.getValue().isJsonArray()) continue;
                    Long2ObjectMap<String> dim = OWNERS.computeIfAbsent(de.getKey(), k -> new Long2ObjectOpenHashMap<>());
                    for (var el : de.getValue().getAsJsonArray()) {
                        if (!el.isJsonPrimitive()) continue;
                        if (dim.putIfAbsent(el.getAsLong(), runner) == null) COUNTS.merge(runner, 1, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            // Re-group the owner index back into per-runner records so the file stays readable by a human.
            Map<String, Map<String, JsonArray>> byRunner = new LinkedHashMap<>();
            for (Map.Entry<String, Long2ObjectMap<String>> e : OWNERS.entrySet())
                for (Long2ObjectMap.Entry<String> pe : e.getValue().long2ObjectEntrySet())
                    byRunner.computeIfAbsent(pe.getValue(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(e.getKey(), k -> new JsonArray()).add(pe.getLongKey());

            JsonObject root = new JsonObject();
            List<String> runners = new ArrayList<>(ON);
            for (String r : byRunner.keySet()) if (!runners.contains(r)) runners.add(r);
            for (String r : runners) {
                JsonObject j = new JsonObject();
                j.addProperty("on", ON.contains(r));
                JsonObject pos = new JsonObject();
                byRunner.getOrDefault(r, Map.of()).forEach(pos::add);
                j.add("pos", pos);
                root.add(r, j);
            }
            Path tmp = file.resolveSibling("placedmask.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
