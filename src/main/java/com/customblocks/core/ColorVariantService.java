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

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.block.SlotBlock;
import com.customblocks.command.Chat;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
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

    // ── Custom-colour keys (customcolor tools) — name-slug form, e.g. "magenta" (G06-5) ──────
    // A custom colour snaps to its nearest preset: the key is that preset's slug, so the id is
    // hex-free ("a4_magenta", not "a4_hex_ff1493") and one bucket exists per colour name per block.
    // Legacy "hex_rrggbb" keys/ids from before G06-5 are still parsed until step 3 migrates them.

    /** The id-suffix key for an arbitrary colour = the slug of its nearest preset: 0xFF1493 → "magenta". */
    public static String keyForRgb(int rgb) {
        return ColorLibrary.slugOf(ColorLibrary.nearestName(rgb));
    }

    /** True when {@code key} is a LEGACY "hex_rrggbb" key (pre-G06-5). */
    public static boolean isHexKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).matches("hex_[0-9a-f]{6}");
    }

    /** A key's colour as 0xRRGGBB: fixed keys read the config; preset slugs read the library; legacy hex parses. */
    public static int rgbForKey(String key) {
        if (isHexKey(key)) return Integer.parseInt(key.substring(4), 16); // legacy "hex_rrggbb"
        for (String c : COLOUR_KEYS) if (c.equals(key)) return rgbFor(key); // 4 fixed → config hex
        String hex = ColorLibrary.hexForSlug(key); // the other 25 presets → library hex
        if (hex != null) return Integer.parseInt(hex.substring(1), 16);
        return rgbFor(key); // unknown key → old fallback
    }

    /** Human label for a key: "red" → "Red", "light_gray" → "Light Gray", legacy "hex_ff8800" → nearest preset. */
    public static String labelFor(String key) {
        String name = ColorLibrary.nameForSlug(key);
        if (name != null) return name;
        if (isHexKey(key)) return ColorLibrary.nearestName(rgbForKey(key)); // legacy
        return capitalize(key);
    }

    /** "mars_black" / "mars_light_gray" / legacy "mars_hex_ff8800" → "mars"; longest colour suffix wins. */
    public static String stripColourSuffix(String id) {
        if (id == null) return null;
        String low = id.toLowerCase(Locale.ROOT);
        String best = null;
        for (ColorLibrary.LibColor c : ColorLibrary.ALL) {
            String suf = "_" + ColorLibrary.slugOf(c.name());
            if (low.endsWith(suf) && (best == null || suf.length() > best.length())) best = suf;
        }
        if (best != null) return id.substring(0, id.length() - best.length());
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
            Chat.toolError(player, "There's no block called \"" + sourceId + "\" any more.");
            return;
        }
        if (LockManager.isLocked(sourceId)) {
            Chat.lockedTool(player, sourceId); return;
        }
        String key = colourKey.toLowerCase(Locale.ROOT);
        String vid = variantId(sourceId, key);

        // Already exists (also covers "red triangle on mars_red") → just hand one over.
        SlotData existing = SlotManager.getById(vid);
        if (existing != null) {
            give(player, existing.index());
            Chat.tool(player, "\"" + vid + "\" already exists — here's one.");
            return;
        }

        // Pick the recolour input BEFORE claiming a slot, so a textureless source costs nothing.
        byte[] sourceImage = TextureStore.loadSource(src.index()); // original image when stored
        byte[] input = sourceImage != null ? sourceImage : TextureStore.load(src.index());
        if (input == null || input.length == 0) {
            Chat.toolError(player, "\"" + sourceId + "\" has no texture yet — give it one first "
                    + "(/cb retexture " + sourceId + " <url>).");
            return;
        }

        // G06-5: "Vart Green" not "Vart (Green)"; strip a prior colour word so a hex variant of
        // "A4 Black" reads "A4 Magenta", not "A4 Black (#FF1493)".
        String variantName = ColorLibrary.stripTrailingColorName(src.displayName()) + " " + labelFor(key);
        SlotData created = SlotManager.create(vid, variantName);
        if (created == null) {
            Chat.toolError(player, "Couldn't create \"" + vid + "\" — every slot is in use.");
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
        Chat.tool(player, "Creating " + vid + " with a " + hex + " background…");

        Thread worker = new Thread(() -> {
            try {
                byte[] recoloured = BackgroundRemover.recolorBackground(input, mode, tol, rgb);
                byte[] png = ImageProcessor.toBlockPng(recoloured, CustomBlocksConfig.textureSize);
                // A non-square source leaves transparent padding above/below after toBlockPng squares it;
                // that padding renders BLACK on the atlas's solid layer (black bands on a landscape logo
                // like youtube). Fill the padding with the variant colour so the bands match the bg. Opaque
                // logo art is drawn over the fill, so only transparent padding changes (CLAUDE.md §7).
                png = ImageProcessor.fillBackground(png, rgb);
                TextureStore.save(index, png);
                // Variants stay re-renderable at other sizes when the source had an original.
                if (sourceImage != null) TextureStore.saveSource(index, sourceImage);
                server.execute(() -> {
                    ResourcePackServer.updatePack();
                    HudSync.broadcast(server); // resync every client's slot cache → new variant has a HUD live (G05-1)
                    give(player, index);
                    Chat.toolSuccess(player, "\"" + vid + "\" created — background recoloured to " + hex + ".");
                });
            } catch (Exception e) {
                IncidentRecorder.record("Colour-variant recolour failed for \"" + vid + "\" (from \""
                        + sourceId + "\")", vid, player.getName().getString(), e);
                server.execute(() -> Chat.toolError(player, "Recolouring \"" + vid + "\" failed — the block "
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
            Chat.toolError(player, "No " + labelFor(key) + " variant of \"" + base + "\" exists — "
                    + "create one with the " + labelFor(key) + " Triangle first.");
            return;
        }
        if (target.index() == current.index()) {
            Chat.tool(player, "Already " + target.displayName() + ".");
            return;
        }
        SlotBlock block = SlotManager.blockAt(target.index());
        if (block == null) { // registry hole shouldn't happen — but never let a click crash
            Chat.toolError(player, "\"" + target.customId() + "\" has no registered block — try /cb reload.");
            return;
        }
        // G13-25 CP4: a placed Arabic letter's facing + back-mirror live on its BlockEntity, which
        // the swap below replaces — capture them first so a colour swap can't knock the letter out
        // of its word (facing is re-stamped and the run re-flowed right after the swap).
        net.minecraft.util.math.Direction arabicFacing = null;
        int arabicBack = -1;
        if (target.isArabic()
                && world.getBlockEntity(pos) instanceof com.customblocks.block.AnimSlotBlockEntity abe) {
            arabicFacing = abe.arabicFacing();
            arabicBack = abe.arabicBackSlot();
        }
        // Mirror SlotBlock.getPlacementState: the target's configured glow rides in the state.
        world.setBlockState(pos, block.getDefaultState()
                .with(SlotBlock.LIGHT, SlotManager.glowFor(target.index())));
        if (arabicFacing != null
                && world.getBlockEntity(pos) instanceof com.customblocks.block.AnimSlotBlockEntity fresh) {
            boolean changed = fresh.setArabicFacing(arabicFacing);
            changed |= fresh.setArabicBackSlot(arabicBack);
            if (changed) fresh.sync();
            com.customblocks.arabic.ArabicSlotJoinFlow.reflowAfterSwap(world, pos);
        }
        Chat.toolSuccess(player, "Swapped to", target.displayName());
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
     * M3 hex — after a colour's hex changed: repaint every existing "*_<colourKey>" block to the
     * CURRENT configured hex.
     *
     * Two reliable paths, neither of which needs to know the OLD hex:
     *   • variant has a stored ORIGINAL source → REGENERATE the bg from it at the new hex (the exact
     *     path {@link #createVariant} used to make it). Cleanest — no compounding artifacts.
     *   • no stored source (older variant) → re-detect the painted bg ON THE BAKED block PNG and
     *     repaint it. The block was made by painting its bg, so its corners ARE that fill colour;
     *     {@code recolorBackground} samples the corners and floods that region to the new hex.
     * The old per-pixel swap-by-{@code oldRgb} is gone: it only worked when the config's *previous*
     * value happened to equal the block's actual painted bg, so older variants looked unchanged.
     * {@code oldRgb} is kept in the signature for callers but no longer needed. Design pixels stay
     * safe — `recolorBackground` only fills the detected background (CLAUDE.md §7). Off-thread; ONE
     * pack rebuild + the chat report after the whole batch.
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
            Chat.tool(player, "No \"_" + key + "\" blocks exist — nothing to recolour.");
            return;
        }
        final String mode = CustomBlocksConfig.backgroundMode; // none → edges inside recolorBackground
        final int tol = CustomBlocksConfig.backgroundTolerance > 0
                ? CustomBlocksConfig.backgroundTolerance : 30; // 0 would mean "no recolour at all"
        Chat.tool(player, "Recolouring " + variants.size() + " " + key + " variant(s) to "
                + hexFor(key) + "…");
        com.customblocks.CustomBlocksMod.LOGGER.info("[CustomBlocks] G06-C recolorVariants START: key={} oldRgb=#{} newRgb=#{} variantsFound={}",
                key, String.format(Locale.ROOT, "%06X", oldRgb), String.format(Locale.ROOT, "%06X", newRgb), variants.size());
        Thread worker = new Thread(() -> {
            int regen = 0, repainted = 0, skipped = 0;
            for (SlotData d : variants) {
                try {
                    byte[] source = TextureStore.loadSource(d.index()); // the original upload, pre-recolour
                    if (source != null && source.length > 0) {
                        // Clean regenerate to the new hex — same recolour path createVariant used.
                        byte[] recoloured = BackgroundRemover.recolorBackground(source, mode, tol, newRgb);
                        byte[] png = ImageProcessor.toBlockPng(recoloured, CustomBlocksConfig.textureSize);
                        // Same non-square padding fix as createVariant: fill the transparent top/bottom
                        // padding with the variant colour so a landscape logo shows colour bands, not black.
                        png = ImageProcessor.fillBackground(png, newRgb);
                        TextureStore.save(d.index(), png);
                        regen++;
                        continue;
                    }
                    // No stored source (older variant, made before sources were kept) → re-detect the
                    // painted background ON THE BAKED block PNG and repaint it. The block was MADE by
                    // painting its bg, so its corners ARE that fill colour: recolorBackground samples
                    // the corners and floods the same region to the new hex — it never needs to know
                    // the old hex. This is what fixes the older blocks the swap-by-old-hex path missed.
                    byte[] png = TextureStore.load(d.index());
                    if (png == null || png.length == 0) { skipped++; continue; }
                    byte[] out = BackgroundRemover.recolorBackground(png, mode, tol, newRgb);
                    TextureStore.save(d.index(), out); // already block-sized — no re-resize
                    repainted++;
                } catch (Exception e) {
                    skipped++;
                    IncidentRecorder.record("Hex recolour failed for \"" + d.customId() + "\"",
                            d.customId(), player.getName().getString(), e);
                }
            }
            final int fRegen = regen, fRepainted = repainted, fSkipped = skipped;
            server.execute(() -> {
                com.customblocks.CustomBlocksMod.LOGGER.info("[CustomBlocks] G06-C recolorVariants DONE: regenerated(from-source)={} repainted(baked-bg-detect)={} skipped(no-texture)={} -> updatePack()",
                        fRegen, fRepainted, fSkipped);
                ResourcePackServer.updatePack(); // ONE rebuild, broadcast AFTER the batch (§7)
                int total = fRegen + fRepainted;
                Chat.toolSuccess(player, "Recoloured " + total + " block(s)"
                        + (fSkipped > 0 ? " (" + fSkipped + " skipped — no texture yet, retexture them)" : "."));
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
