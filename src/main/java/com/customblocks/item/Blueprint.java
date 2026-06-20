/**
 * Blueprint.java
 *
 * Responsibility: build and read a "Blueprint" — a physical, tradeable item that carries one
 * block's full schema-v1 recipe inside its CUSTOM_DATA component. A player can hand a Blueprint
 * to a friend (or keep it) and recreate the block later with /cb importblock (held in hand).
 *
 * Design choice (developer-approved): a plain renamed PAPER carrying the recipe — no per-block
 * model/texture registration. Reliable over fancy. The recipe is the JSON from BlockExporter,
 * so import reuses the exact same schema as the file/category import paths (one source of truth).
 *
 * Depends on: SlotData, BlockExporter
 * Called by:  BlueprintCommands
 */
package com.customblocks.item;

import com.customblocks.core.BlockExporter;
import com.customblocks.core.SlotData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class Blueprint {

    private Blueprint() {} // static-only

    /** NBT key under CUSTOM_DATA that holds the block's schema-v1 JSON. */
    private static final String KEY = "cb_blueprint";

    /** Build a Blueprint item for {@code d}: a named PAPER with the recipe inside and a readable tooltip. */
    public static ItemStack create(SlotData d) {
        ItemStack s = new ItemStack(Items.PAPER);

        NbtCompound nbt = new NbtCompound();
        nbt.putString(KEY, BlockExporter.toJson(d));
        s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        s.set(DataComponentTypes.CUSTOM_NAME,
                line("§b§lBlueprint: §f" + d.displayName()));

        List<Text> lore = new ArrayList<>();
        lore.add(line("§7id: §f" + d.customId()));
        lore.add(line("§7glow: §f" + d.glow() + "   §7hardness: §f" + d.hardness()));
        lore.add(line("§7sound: §f" + d.soundType()));
        if (!d.category().isEmpty()) lore.add(line("§7category: §f" + d.category()));
        lore.add(line("§8Hold this and run §7/cb importblock"));
        s.set(DataComponentTypes.LORE, new LoreComponent(lore));

        s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    /** True if {@code s} is a Blueprint (carries a recipe). */
    public static boolean is(ItemStack s) {
        return json(s) != null;
    }

    /** The schema-v1 JSON carried by {@code s}, or null if it isn't a Blueprint. */
    public static String json(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        NbtComponent c = s.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return null;
        NbtCompound nbt = c.copyNbt();
        return nbt.contains(KEY) ? nbt.getString(KEY) : null;
    }

    private static Text line(String s) {
        return Text.literal(s).styled(st -> st.withItalic(false));
    }
}
