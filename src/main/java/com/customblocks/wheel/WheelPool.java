/**
 * WheelPool.java - Group 34 (Wheel of Fortune) item B (item pool).
 *
 * The set of prizes the wheel can land on: every survival-obtainable VANILLA item/block, built ONCE from
 * {@link net.minecraft.registry.Registries#ITEM} and cached (design lock G34 item 7 - never walked per
 * spin). "Survival-obtainable" is approximated as: namespace {@code minecraft}, not AIR, not a spawn egg
 * (creative-only), and not one of a small curated {@link #DENYLIST} of creative/unobtainable blocks
 * (command blocks, barriers, structure/jigsaw blocks, debug tools, ...). Restricting to the {@code minecraft}
 * namespace also drops this mod's own render-only items and every other installed mod's clutter, so a spin
 * only ever names an item both race teams already know.
 *
 * Depends on: Registries.ITEM
 * Called by:  WheelBlockEntity (pickRandom on spin), WheelCommands (list / count)
 */
package com.customblocks.wheel;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WheelPool {

    private WheelPool() {} // static-only

    /** Curated creative-only / unobtainable ids to keep OFF the wheel (tunable). Spawn eggs are excluded
     *  separately by name. All ids are in the {@code minecraft} namespace. */
    private static final Set<String> DENYLIST = Set.of(
            "air", "cave_air", "void_air",
            "command_block", "chain_command_block", "repeating_command_block", "command_block_minecart",
            "barrier", "light", "structure_block", "structure_void", "jigsaw", "debug_stick",
            "knowledge_book", "bedrock", "spawner", "trial_spawner", "vault",
            "budding_amethyst", "reinforced_deepslate", "petrified_oak_slab", "end_portal_frame",
            "infested_stone", "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks",
            "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks", "infested_deepslate",
            "farmland", "dirt_path", "frogspawn");

    /** The cached prize list, built lazily on the first spin/list. */
    private static volatile List<Item> cached;

    /** The prize pool, built once and cached (design lock G34 item 7). Thread-safe lazy init. */
    public static List<Item> items() {
        List<Item> local = cached;
        if (local != null) return local;
        synchronized (WheelPool.class) {
            if (cached == null) cached = build();
            return cached;
        }
    }

    /** Number of prizes on the wheel. */
    public static int size() {
        return items().size();
    }

    /** A random prize from the pool, using the supplied world random. Never AIR. */
    public static Item pickRandom(net.minecraft.util.math.random.Random random) {
        List<Item> pool = items();
        return pool.get(random.nextInt(pool.size()));
    }

    /** Walk {@link Registries#ITEM} once, keeping only survival-obtainable vanilla prizes. */
    private static List<Item> build() {
        List<Item> out = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id = Registries.ITEM.getId(item);
            if (!"minecraft".equals(id.getNamespace())) continue; // vanilla only
            String path = id.getPath();
            if (path.endsWith("_spawn_egg")) continue;            // creative-only
            if (DENYLIST.contains(path)) continue;
            out.add(item);
        }
        return List.copyOf(out);
    }
}
