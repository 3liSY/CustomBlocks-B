/**
 * CustomColorToolItem.java
 *
 * Responsibility: The custom-colour magic tools behind /cb customcolor — one registered
 * Square + Triangle pair whose colour rides in item NBT ("cb_rgb"), so ONE item id serves
 * every hex. Behaviour mirrors ShapeToolItem with a "hex_rrggbb" colour key: the Triangle
 * creates a "*_hex_rrggbb" variant of the clicked block, the Square swaps a placed block to
 * an existing one. Inspired by the old project's ColorSquare/ColorTriangle items (NBT colour
 * + tinted icon); the code is new. The icon texture is white and tinted client-side from the
 * same NBT, so each pair visibly wears its colour.
 *
 * Depends on: SlotBlock/SlotData/SlotManager, ColorVariantService, ColorLibrary, Chat
 * Called by:  ToolItems (registration), HexCommands + CustomColorMenu via givePair
 */
package com.customblocks.item;

import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.ColorVariantService;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.List;
import java.util.Locale;

public class CustomColorToolItem extends Item {

    /** NBT int key carrying the tool's 0xRRGGBB colour (also read by the client icon tint). */
    public static final String NBT_RGB = "cb_rgb";

    private final String shape; // "Square" or "Triangle"

    public CustomColorToolItem(Settings settings, String shape) {
        super(settings);
        this.shape = shape;
    }

    /** The stack's colour, or -1 when it has none (e.g. pulled bare from the registry). */
    public static int rgbOf(ItemStack stack) {
        if (stack == null) return -1;
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return -1;
        NbtCompound nbt = custom.copyNbt();
        return nbt.contains(NBT_RGB) ? nbt.getInt(NBT_RGB) & 0xFFFFFF : -1;
    }

    /** Build one coloured tool stack: NBT colour + coloured name + lore + glint. */
    public static ItemStack createStack(Item item, int rgb, String shape) {
        rgb &= 0xFFFFFF;
        String hex = String.format(Locale.ROOT, "#%06X", rgb);
        String label = ColorLibrary.nameForHex(hex) != null ? ColorLibrary.nameForHex(hex) : hex;
        ItemStack stack = new ItemStack(item, 1);

        NbtCompound nbt = new NbtCompound();
        nbt.putInt(NBT_RGB, rgb);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        final int nameRgb = rgb;
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(label).styled(s -> s.withColor(nameRgb).withBold(true).withItalic(false))
                        .append(Text.literal(" " + shape + " ").styled(s -> s.withColor(0xFFFFFF).withBold(false).withItalic(false)))
                        .append(Text.literal("[" + hex + "]").styled(s -> s.withColor(0x888888).withBold(false).withItalic(false))));
        List<Text> lore = "Triangle".equals(shape)
                ? List.of(line("§7Right-click a custom block to create its " + hex + " variant."),
                          line("§7The image's background is recoloured; the design stays."),
                          line("§8Made by /cb customcolor — the colour is in the item."))
                : List.of(line("§7Right-click a placed custom block to swap it to its " + hex + " variant."),
                          line("§7Swaps only — create the variant with the matching Triangle."),
                          line("§8Made by /cb customcolor — the colour is in the item."));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static Text line(String s) {
        return Text.literal(s).styled(st -> st.withItalic(false));
    }

    /** Hand the player the Square + Triangle pair (drops at their feet if the inventory is full). */
    public static void givePair(ServerPlayerEntity player, int rgb) {
        player.giveItemStack(createStack(ToolItems.CUSTOM_SQUARE, rgb, "Square"));
        player.giveItemStack(createStack(ToolItems.CUSTOM_TRIANGLE, rgb, "Triangle"));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld().getBlockState(ctx.getBlockPos()).getBlock() instanceof SlotBlock slot)) {
            return ActionResult.PASS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS; // client swings instantly; server does the work
        }
        SlotData d = SlotManager.getBySlot(slot.getSlotKey());
        if (d == null) {
            return ActionResult.SUCCESS; // unassigned slot block; nothing to act on
        }
        int rgb = rgbOf(ctx.getStack());
        if (rgb < 0) {
            Chat.tool(player, "§cThis tool has no colour — get a pair with /cb customcolor.");
            return ActionResult.SUCCESS;
        }
        String key = ColorVariantService.keyForRgb(rgb);
        if ("Triangle".equals(shape)) {
            ColorVariantService.createVariant(player, d.customId(), key);
        } else {
            ColorVariantService.swapPlaced(player, ctx.getWorld(), ctx.getBlockPos(), d, key);
        }
        return ActionResult.SUCCESS;
    }
}
