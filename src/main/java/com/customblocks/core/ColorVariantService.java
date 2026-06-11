/**
 * ColorVariantService.java
 *
 * Responsibility: Create and swap coloured variants of existing custom blocks (Group 06).
 * M2 — a Triangle calls createVariant: the source block's texture has its background
 * recoloured to the triangle's configured hex, the result is saved into a new slot
 * (id = stripColourSuffix(sourceId) + "_" + colourKey), and light/hardness/sound/category
 * are copied from the source. If the variant already exists the player is just handed one
 * (no reprocess). Image work runs on a worker thread; ONE pack rebuild at the end.
 * M3 — a Square calls swapPlaced: the clicked PLACED block changes in place to that
 * colour's EXISTING variant. Never creates; Black Square falls back to the base block.
 * Customcolor — the custom tools use the same flows with keys of the form "hex_rrggbb"
 * (variant ids end in "_hex_rrggbb"; the colour comes from the key, not the config).
 *
 * Recoloured from which input: the stored ORIGINAL source image when one exists
 * (TextureStore.loadSource), else the baked slot PNG — same fallback rule as retextureAll.
 *
 * Depends on: SlotManager, SlotData, TextureStore, UndoManager, IncidentRecorder,
 *             BackgroundRemover (recolorBackground), ImageProcessor, ResourcePackServer,
 *             CustomBlocksConfig (triangle*Hex, textureSize, backgroundTolerance), Chat
 * Called by:  item/ShapeToolItem (Triangle create, Square swap),
 *             gui/chest/RecolorConfirmMenu (Yes button).
 */
package com.customblocks.core;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ColorReplacer;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;

public final class ColorVariantService {

    private ColorVariantService() {} // static-only

    /** The four fixed variant colours (M2 spec) — also the id suffixes. */
    public static final String[] COLOUR_KEYS = {"red", "yellow", "green", "black"};

    /** The configured "#RRGGBB" for a colour key (canonical form, validated on config load). */
    public static String hexFor(String colourKey) {
        return switch (colourKey == null ? "" : colourKey.toLowerCase(Locale.ROOT)) {
            case "red"    -> CustomBlocksConfig.triangleRedHex;
            case "yellow" -> CustomBlocksConfig.triangleYellowHex;
            case "green"  -> CustomBlocksConfig.triangleGreenHex;
            case "black"  -> CustomBlocksConfig.triangleBlackHex;
            default       -> "#000000";
        };
    }

    /** The configured colour as a packed 0xRRGGBB int. */
    public static int rgbFor(String colourKey) {
        return Integer.parseInt(hexFor(colourKey).substring(1), 16);
    }

    // ── Custom-colour keys (customcolor tools) — suffix form "hex_rrggbb" ──────

    /** The id-suffix key for an arbitrary colour, e.g. 0xFF8800 → "hex_ff8800". */
    public static String keyForRgb(int rgb) {
        return String.format(Locale.ROOT, "hex_%06x", rgb & 0xFFFFFF);
    }

    /** True when {@code key} is a custom-colour key rather than one of the four fixed ones. */
    public static boolean isHexKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).matches("hex_[0-9a-f]{6}");
    }

    /** A key's colour as 0xRRGGBB: "hex_ff8800" → 0xFF8800; fixed keys read the config. */
    public static int rgbForKey(String key) {
        return isHexKey(key) ? Integer.parseInt(key.substring(4), 16) : rgbFor(key);
    }

    /** Human label for a key: "red" → "Red", "hex_ff8800" → "#FF8800". */
    public static String labelFor(String key) {
        return isHexKey(key) ? "#" + key.substring(4).toUpperCase(Locale.ROOT) : capitalize(key);
    }

    /** "mars_black" / "mars_hex_ff8800" → "mars"; ids without a colour suffix are unchanged. */
    public static String stripColourSuffix(String id) {
        for (String c : COLOUR_KEYS) {
            if (id.endsWith("_" + c)) return id.substring(0, id.length() - c.length() - 1);
        }
        return id.replaceFirst("(?i)_hex_[0-9a-f]{6}$", "");
    }

    /** The variant id a colour produces for a source block, e.g. ("mars", "black") → "mars_black". */
    public static String variantId(String sourceId, String colourKey) {
        return stripColourSuffix(sourceId) + "_" + colourKey.toLowerCase(Locale.ROOT);
    }

    /**
     * Create (or hand over) the {@code colourKey} variant of {@code sourceId} for {@code player}.
     * Fast checks + slot claim happen on the calling (server) thread; the image recolour runs on
     * a worker thread, then hops back for ONE pack rebuild + the item handover.
     */
    public static void createVariant(ServerPlayerEntity player, String sourceId, String colourKey) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        SlotData src = SlotManager.getById(sourceId);
        if (src == null) {
            Chat.tool(player, "§cThere's no block called \"" + sourceId + "\" any more.");
            return;
        }
        String key = colourKey.toLowerCase(Locale.ROOT);
        String vid = variantId(sourceId, key);

        // Already exists (also covers "red triangle on mars_red") → just hand one over.
        SlotData existing = SlotManager.getById(vid);
        if (existing != null) {
            give(player, existing.index());
            Chat.tool(player, "§7\"§f" + vid + "§7\" already exists — here's one.");
            return;
        }

        // Pick the recolour input BEFORE claiming a slot, so a textureless source costs nothing.
        byte[] sourceImage = TextureStore.loadSource(src.index()); // original image when stored
        byte[] input = sourceImage != null ? sourceImage : TextureStore.load(src.index());
        if (input == null || input.length == 0) {
            Chat.tool(player, "§c\"" + sourceId + "\" has no texture yet — give it one first "
                    + "(§f/cb retexture " + sourceId + " <url>§c).");
            return;
        }

        SlotData created = SlotManager.create(vid, src.displayName() + " (" + labelFor(key) + ")");
        if (created == null) {
            Chat.tool(player, "§cCouldn't create \"" + vid + "\" — every slot is in use.");
            return;
        }
        // Copy the source's feel onto the variant (one snapshot write instead of four setters).
        SlotData copied = new SlotData(created.index(), created.customId(), created.displayName(),
                src.glow(), src.hardness(), src.soundType(), src.noCollision(), src.category());
        SlotManager.restoreSnapshot(copied);
        UndoManager.recordCreate(player.getUuid(), copied);

        final int index = copied.index();
        final int rgb = rgbForKey(key);
        final String hex = String.format(Locale.ROOT, "#%06X", rgb);
        final String mode = CustomBlocksConfig.backgroundMode; // none → edges inside recolorBackground
        final int tol = CustomBlocksConfig.backgroundTolerance > 0
                ? CustomBlocksConfig.backgroundTolerance : 30;  // 0 would mean "no recolour at all"
        Chat.tool(player, "§7Creating §f" + vid + " §7with a " + hex + " background…");

        Thread worker = new Thread(() -> {
            try {
                byte[] recoloured = BackgroundRemover.recolorBackground(input, mode, tol, rgb);
                byte[] png = ImageProcessor.toBlockPng(recoloured, CustomBlocksConfig.textureSize);
                TextureStore.save(index, png);
                // Variants stay re-renderable at other sizes when the source had an original.
                if (sourceImage != null) TextureStore.saveSource(index, sourceImage);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    give(player, index);
                    Chat.tool(player, "§a\"" + vid + "\" created §7— background recoloured to " + hex + ".");
                });
            } catch (Exception e) {
                IncidentRecorder.record("Colour-variant recolour failed for \"" + vid + "\" (from \""
                        + sourceId + "\", by " + player.getName().getString() + ")", e);
                server.execute(() -> Chat.tool(player, "§cRecolouring \"" + vid + "\" failed — the block "
                        + "exists but kept no texture. Retexture it, or /cb delete " + vid + "."));
            }
        }, "CustomBlocks-ColorVariant");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Swap the placed block at {@code pos} to its {@code colourKey} variant (Group 06 / M3).
     * Squares call this. Only swaps to blocks that ALREADY exist — Triangles create them.
     * Black Square with no "_black" variant falls back to the base block. The slot registry
     * never changes — only the placed block — so the swap is reversed by clicking with a
     * Square of the original colour (no UndoManager entry; there is no SlotData edit to record).
     */
    public static void swapPlaced(ServerPlayerEntity player, World world, BlockPos pos,
                                  SlotData current, String colourKey) {
        String key = colourKey.toLowerCase(Locale.ROOT);
        String base = stripColourSuffix(current.customId());
        SlotData target = SlotManager.getById(variantId(current.customId(), key));
        if (target == null && "black".equals(key)) {
            target = SlotManager.getById(base); // Black Square fallback: no _black → base block
        }
        if (target == null) {
            Chat.tool(player, "§cNo " + labelFor(key) + " variant of \"" + base + "\" exists — "
                    + "create one with the " + labelFor(key) + " Triangle first.");
            return;
        }
        if (target.index() == current.index()) {
            Chat.tool(player, "§7This block is already §f" + target.customId() + "§7.");
            return;
        }
        SlotBlock block = SlotManager.blockAt(target.index());
        if (block == null) { // registry hole shouldn't happen — but never let a click crash
            Chat.tool(player, "§c\"" + target.customId() + "\" has no registered block — try /cb reload.");
            return;
        }
        // Mirror SlotBlock.getPlacementState: the target's configured glow rides in the state.
        world.setBlockState(pos, block.getDefaultState()
                .with(SlotBlock.LIGHT, SlotManager.glowFor(target.index())));
        Chat.tool(player, "§7Swapped to §f" + target.customId() + "§7.");
    }

    /** How many existing blocks are {@code colourKey} variants (id ends in "_<colour>"). */
    public static int variantCount(String colourKey) {
        String suffix = "_" + colourKey.toLowerCase(Locale.ROOT);
        int n = 0;
        for (SlotData d : SlotManager.assignedSlots()) {
            if (d.customId().endsWith(suffix)) n++;
        }
        return n;
    }

    /**
     * M3 hex — after a colour's hex changed: repaint every existing "*_<colourKey>" block's
     * texture from {@code oldRgb} to the CURRENT configured hex. Direct per-pixel swap
     * (ColorReplacer, ~30/channel tolerance, NO flood fill) so design pixels are safe — the
     * old project's batch-recolour pitfall (CLAUDE.md §7). Off-thread; ONE pack rebuild and
     * the chat report come after the whole batch.
     */
    public static void recolorVariants(ServerPlayerEntity player, String colourKey, int oldRgb) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String key = colourKey.toLowerCase(Locale.ROOT);
        int newRgb = rgbFor(key);
        java.util.List<SlotData> variants = new java.util.ArrayList<>();
        String suffix = "_" + key;
        for (SlotData d : SlotManager.assignedSlots()) {
            if (d.customId().endsWith(suffix)) variants.add(d);
        }
        if (variants.isEmpty()) {
            Chat.tool(player, "§7No \"_" + key + "\" blocks exist — nothing to recolour.");
            return;
        }
        Chat.tool(player, "§7Recolouring §f" + variants.size() + " §7" + key + " variant(s) to "
                + hexFor(key) + "…");
        Thread worker = new Thread(() -> {
            int done = 0, skipped = 0;
            for (SlotData d : variants) {
                try {
                    byte[] png = TextureStore.load(d.index());
                    if (png == null || png.length == 0) { skipped++; continue; }
                    TextureStore.save(d.index(), ColorReplacer.swapColor(png, oldRgb, newRgb, 30));
                    done++;
                } catch (Exception e) {
                    skipped++;
                    IncidentRecorder.record("Hex recolour failed for \"" + d.customId() + "\"", e);
                }
            }
            final int fDone = done, fSkipped = skipped;
            server.execute(() -> {
                ResourcePackServer.updatePack(); // ONE rebuild, broadcast AFTER the batch (§7)
                Chat.tool(player, "§aRecoloured " + fDone + " block(s)"
                        + (fSkipped > 0 ? " §7(" + fSkipped + " skipped — see incidents)" : "§7."));
            });
        }, "CustomBlocks-HexRecolor");
        worker.setDaemon(true);
        worker.start();
    }

    /** Hand the player the variant's block item (drops at their feet if the inventory is full). */
    private static void give(ServerPlayerEntity player, int index) {
        var item = SlotManager.itemAt(index);
        if (item != null) player.giveItemStack(new ItemStack(item));
    }

    /** "black" → "Black" (display only). */
    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
