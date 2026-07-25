/**
 * ColorFamilyOps.java — the engine behind /cb colorvariants (G10-CV). Split out of
 * ColorVariantCommands so the Brigadier layer stays thin (and under the 400-line handler gate)
 * while the family logic lives here under the 500-line class cap. Holds three forms:
 *
 *   • createFamily(id, name, link)  — NEW family from one image link (slice 1).
 *   • rebuildFamily(id)             — regenerate an EXISTING block's 3 colours from its stored
 *                                     image, base untouched (slice 2, piece 1).
 *   • beginOverride(...)            — re-run create on an EXISTING family → /cb confirm →
 *                                     overwrite the picture/colours, KEEP glow/hardness/sound/
 *                                     collision/category/favourite + names (slice 2, piece 2).
 *
 * Reuse, do NOT rewrite: the download-off-thread idiom from CreationCommands.createWithTexture;
 * the recolour rail (recolorBackground → toBlockPng → fillBackground) + rgb/label/variantId from
 * core/ColorVariantService; the batch-undo pattern (UndoManager.recordBatch); BulkConfirm for the
 * /cb confirm hold; TEXTURE undo ops (carry old+new bytes) so an overwrite is one /cb undo.
 *
 * Depends on: SlotManager, SlotData, TextureStore, UndoManager, ColorVariantService, ColorLibrary,
 *             BackgroundRemover, ImageProcessor, ImageDownloader, ResourcePackServer, HudSync,
 *             CategoryCommands, BulkConfirm, CustomBlocksConfig, IncidentRecorder, Chat.
 * Called by:  ColorVariantCommands (the Brigadier tree).
 */
package com.customblocks.command;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.command.handlers.BulkConfirm;
import com.customblocks.command.handlers.CategoryCommands;
import com.customblocks.core.ColorLibrary;
import com.customblocks.core.ColorVariantService;
import com.customblocks.core.IncidentRecorder;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.image.BackgroundRemover;
import com.customblocks.image.ImageDownloader;
import com.customblocks.image.ImageLimits;
import com.customblocks.image.ImageProcessor;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ColorFamilyOps {

    private ColorFamilyOps() {} // static-only

    /** The default colour set a family is built with (the bare id IS the black/normal one). */
    static final String[] DEFAULT_COLOURS = {"red", "green", "yellow"};

    /** The acting player's UUID, or null for console/command-block (those aren't undoable). */
    private static UUID actor(ServerCommandSource src) {
        return src.getEntity() instanceof ServerPlayerEntity p ? p.getUuid() : null;
    }

    // ── /cb colorvariants <id> <name> <link> — build a new colour family (or overwrite if it exists) ──
    public static int createFamily(ServerCommandSource src, String id, String name, String link) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        if (!ImageDownloader.isHttpUrl(link)) {
            Chat.error(src, "That doesn't look like a web link, so nothing was created. Use a direct "
                    + "http/https image URL (right-click the image → Copy Image Address).");
            return 0;
        }
        // If any target id already exists, this is an OVERWRITE → hold it behind /cb confirm (slice 2 piece 2).
        List<String> clash = takenTargets(id);
        if (!clash.isEmpty()) {
            return beginOverride(src, id, name, link, clash);
        }

        final UUID who = actor(src);
        final String baseMode = CustomBlocksConfig.backgroundMode;       // base = normal upload (snap-to-black)
        final int baseTol = CustomBlocksConfig.backgroundTolerance;
        final int colourTol = baseTol > 0 ? baseTol : 30;               // 0 tol → no recolour at all (createVariant rule)
        final int size = CustomBlocksConfig.textureSize;
        Chat.info(src, "Fetching the image for the \"" + id + "\" colour family before creating…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageLimits.downloadColorFamilySource(link);
                byte[] basePng = bakeBase(raw, baseMode, baseTol, size);
                Map<String, byte[]> colourPngs = recolourSet(raw, baseMode, colourTol, size);
                server.execute(() -> commit(src, server, who, id, name, link, raw, basePng, colourPngs));
            } catch (Exception e) {
                String code = IncidentRecorder.record("Colour-family create failed for \"" + id + "\" (url: " + link + ")",
                        id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get an image from that URL, so the family was NOT created.", code));
            }
        }, "CustomBlocks-ColorFamily");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    /** Server-thread commit: claim slots, save textures, ONE pack rebuild, batch undo, hand over. */
    private static void commit(ServerCommandSource src, MinecraftServer server, UUID who, String id,
                               String name, String link, byte[] raw, byte[] basePng,
                               Map<String, byte[]> colourPngs) {
        // Re-check on the server thread — an id could have been taken while downloading.
        List<String> clash = takenTargets(id);
        if (!clash.isEmpty()) {
            Chat.error(src, "These ids were taken while the image downloaded: " + CbFmt.BODY + String.join(", ", clash)
                    + CbFmt.BAD + ". Nothing was created.");
            return;
        }

        SlotData base = SlotManager.create(id, name);
        if (base == null) {
            Chat.error(src, "Couldn't create \"" + id + "\" — the id was taken or every slot is in use. "
                    + "Nothing was created.");
            return;
        }
        TextureStore.save(base.index(), basePng);
        TextureStore.saveSource(base.index(), raw);
        TextureStore.saveUrl(base.index(), link);

        List<UndoManager.Op> children = new ArrayList<>();
        children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, base, null, "create"));

        List<Integer> giveIndices = new ArrayList<>();
        giveIndices.add(base.index());
        StringBuilder made = new StringBuilder(CbFmt.BODY + id); // base shown white
        boolean slotsRanOut = false;

        for (Map.Entry<String, byte[]> e : colourPngs.entrySet()) {
            String key = e.getKey();
            String vid = ColorVariantService.variantId(id, key); // <id>_<colour>
            String variantName = ColorLibrary.stripTrailingColorName(name) + " " + ColorVariantService.labelFor(key);
            SlotData c = SlotManager.create(vid, variantName);
            if (c == null) { slotsRanOut = true; break; } // pool exhausted — keep what we made
            TextureStore.save(c.index(), e.getValue());
            TextureStore.saveSource(c.index(), e.getValue()); // variant source is its baked colour PNG, not the raw upload
            children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, c, null, "create"));
            giveIndices.add(c.index());
            made.append(CbFmt.DIM + ", ").append(colourCode(key)).append(vid);
        }

        // ONE pack rebuild + broadcast AFTER the whole batch (§7), then resync every client's HUD (G05-1).
        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        HudSync.broadcast(server);

        UndoManager.recordBatch(who, children, "colorvariants " + id + " (" + children.size() + ")");
        giveAll(src, giveIndices);
        CategoryCommands.suggestOnCreate(src, base); // Group 11: one-click category hint (opt-out via config)

        Chat.success(src, "Created colour family " + CbFmt.BODY + id + CbFmt.RESET + ": " + made + CbFmt.RESET + ". " + CbFmt.DIM + "One " + CbFmt.BODY + "/cb undo" + CbFmt.DIM + " removes them all.");
        if (slotsRanOut) {
            Chat.error(src, "Ran out of free slots before finishing — only the ids above were made. "
                    + "Free some slots and re-run for the rest.");
        }
    }

    // ── /cb colorvariants <id> — rebuild an EXISTING block's colour family from its stored image ──
    // Base is left untouched (no link = no re-bake). The 3 colours are regenerated from the base's own
    // stored source image, keeping the base's name. If a colour already exists it is NOT clobbered here —
    // overwriting an existing family is the override form (re-run with a link, behind /cb confirm).
    public static int rebuildFamily(ServerCommandSource src, String id) {
        MinecraftServer server = src.getServer();
        if (server == null) return 0;

        SlotData base = SlotManager.getById(id);
        if (base == null) {
            Chat.error(src, "There's no block called " + CbFmt.BODY + id + CbFmt.BAD + ". To make a NEW family use: "
                    + CbFmt.BODY + "/cb colorvariants <id> <name> <link>" + CbFmt.BAD + ".");
            return 0;
        }
        // Don't silently overwrite existing colours — that's the override form (re-run with a link).
        List<String> existing = existingColours(id);
        if (!existing.isEmpty()) {
            Chat.error(src, "This family already has: " + CbFmt.BODY + String.join(", ", existing) + CbFmt.BAD + ". To overwrite it, "
                    + "re-run with the image link: " + CbFmt.BODY + "/cb colorvariants " + id + " <name> <link>" + CbFmt.BAD + " (asks for /cb confirm).");
            return 0;
        }

        final int index = base.index();
        final String name = base.displayName();
        final String baseMode = CustomBlocksConfig.backgroundMode;
        final int baseTol = CustomBlocksConfig.backgroundTolerance;
        final int colourTol = baseTol > 0 ? baseTol : 30;   // 0 tol → no recolour (createVariant rule)
        final int size = CustomBlocksConfig.textureSize;
        final UUID who = actor(src);

        Chat.info(src, "Rebuilding the colour family for \"" + id + "\" from its stored image…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = TextureStore.loadSource(index);
                if (raw == null) {
                    server.execute(() -> Chat.error(src, "\"" + id + "\" has no stored image to recolour from "
                            + "(it wasn't made from an image link). Nothing was changed."));
                    return;
                }
                // The size gate lives on the DOWNLOAD in the create/overwrite forms, so a stored source
                // reaches this rail unchecked: it may predate these limits, or have been saved by plain
                // /cb create (looser cap, no pixel limit). Re-check before baking three textures from it.
                try {
                    ImageLimits.requireColorFamilySource(raw);
                } catch (Exception tooBig) {
                    server.execute(() -> Chat.error(src, tooBig.getMessage() + " Retexture " + CbFmt.BODY + id
                            + CbFmt.BAD + " with a smaller image first, then rebuild its colours. Nothing was changed."));
                    return;
                }
                Map<String, byte[]> colourPngs = recolourSet(raw, baseMode, colourTol, size);
                server.execute(() -> commitRebuild(src, server, who, id, name, raw, colourPngs));
            } catch (Exception e) {
                String code = IncidentRecorder.record("Colour-family rebuild failed for \"" + id + "\"", id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't rebuild the colour family.", code));
            }
        }, "CustomBlocks-ColorFamilyRebuild");
        worker.setDaemon(true);
        worker.start();
        return 1;
    }

    /** Server-thread commit for a rebuild: create the missing colours, ONE pack rebuild, batch undo, hand over. */
    private static void commitRebuild(ServerCommandSource src, MinecraftServer server, UUID who, String id,
                                      String name, byte[] raw, Map<String, byte[]> colourPngs) {
        if (!SlotManager.hasId(id)) {
            Chat.error(src, "\"" + id + "\" was removed while rebuilding. Nothing was changed.");
            return;
        }
        // A colour could have been created while we recoloured — don't clobber it.
        List<String> existing = existingColours(id);
        if (!existing.isEmpty()) {
            Chat.error(src, "These were created while rebuilding: " + CbFmt.BODY + String.join(", ", existing)
                    + CbFmt.BAD + ". Nothing was changed.");
            return;
        }

        List<UndoManager.Op> children = new ArrayList<>();
        List<Integer> giveIndices = new ArrayList<>();
        StringBuilder made = new StringBuilder();
        boolean slotsRanOut = false;

        for (Map.Entry<String, byte[]> e : colourPngs.entrySet()) {
            String key = e.getKey();
            String vid = ColorVariantService.variantId(id, key);
            String variantName = ColorLibrary.stripTrailingColorName(name) + " " + ColorVariantService.labelFor(key);
            SlotData c = SlotManager.create(vid, variantName);
            if (c == null) { slotsRanOut = true; break; } // pool exhausted — keep what we made
            TextureStore.save(c.index(), e.getValue());
            TextureStore.saveSource(c.index(), e.getValue()); // variant source is its baked colour PNG, not the raw upload
            children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, c, null, "create"));
            giveIndices.add(c.index());
            if (made.length() > 0) made.append(CbFmt.DIM + ", ");
            made.append(colourCode(key)).append(vid);
        }

        if (children.isEmpty()) {
            Chat.error(src, "Couldn't create any colours — every slot may be in use. Nothing was changed.");
            return;
        }

        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        HudSync.broadcast(server);

        UndoManager.recordBatch(who, children, "colorvariants rebuild " + id + " (" + children.size() + ")");
        giveAll(src, giveIndices);

        Chat.success(src, "Rebuilt colour family for " + CbFmt.BODY + id + CbFmt.RESET + ": " + made + CbFmt.RESET + ". " + CbFmt.DIM + "One " + CbFmt.BODY + "/cb undo" + CbFmt.DIM + " "
                + "removes the new colours (" + CbFmt.BODY + id + CbFmt.DIM + " itself is untouched).");
        if (slotsRanOut) {
            Chat.error(src, "Ran out of free slots before finishing — only the colours above were made. "
                    + "Free some slots and re-run for the rest.");
        }
    }

    // ── override: re-run create on an existing family → /cb confirm → overwrite the picture/colours ──
    // Holds via BulkConfirm so a fat overwrite never fires by accident. Overwrites only the TEXTURE of
    // targets that already exist (slot metadata — glow/hardness/sound/collision/category/favourite + the
    // display name — is left untouched); any family member that's missing is created fresh. One /cb undo
    // reverts the whole thing (TEXTURE children restore the old pixels, CREATE children remove new ones).
    private static int beginOverride(ServerCommandSource src, String id, String name, String link, List<String> clash) {
        BulkConfirm.request(src, () -> runOverwrite(src, id, name, link), "overwrite the " + id + " colour family");
        Chat.info(src, CbFmt.VALUE + id + CbFmt.DIM + " already exists (" + String.join(", ", clash) + "). Type " + CbFmt.BODY + "/cb confirm" + CbFmt.DIM + " to "
                + "OVERWRITE the picture/colours from the new link — keeps glow, hardness, sound, collision, "
                + "category, favourite and names. " + CbFmt.BODY + "/cb cancel" + CbFmt.DIM + " to stop.");
        return 1;
    }

    /** Confirmed override: download + recolour off-thread, then overwrite on the server thread. */
    private static void runOverwrite(ServerCommandSource src, String id, String name, String link) {
        MinecraftServer server = src.getServer();
        if (server == null) return;
        final UUID who = actor(src);
        final String baseMode = CustomBlocksConfig.backgroundMode;
        final int baseTol = CustomBlocksConfig.backgroundTolerance;
        final int colourTol = baseTol > 0 ? baseTol : 30;
        final int size = CustomBlocksConfig.textureSize;
        Chat.info(src, "Fetching the new image to overwrite the \"" + id + "\" colour family…");

        Thread worker = new Thread(() -> {
            try {
                byte[] raw = ImageLimits.downloadColorFamilySource(link);
                byte[] basePng = bakeBase(raw, baseMode, baseTol, size);
                Map<String, byte[]> colourPngs = recolourSet(raw, baseMode, colourTol, size);
                server.execute(() -> commitOverwrite(src, server, who, id, name, link, raw, basePng, colourPngs));
            } catch (Exception e) {
                String code = IncidentRecorder.record("Colour-family overwrite failed for \"" + id + "\" (url: " + link + ")",
                        id, src.getName(), e);
                server.execute(() -> Chat.incidentError(src, "Couldn't get an image from that URL, so NOTHING was changed.", code));
            }
        }, "CustomBlocks-ColorFamilyOverwrite");
        worker.setDaemon(true);
        worker.start();
    }

    /** Server-thread overwrite: re-save each family member's texture (keep metadata), create any missing. */
    private static void commitOverwrite(ServerCommandSource src, MinecraftServer server, UUID who, String id,
                                        String name, String link, byte[] raw, byte[] basePng,
                                        Map<String, byte[]> colourPngs) {
        // target id → its new png + the fresh-create name (used only if the target is missing)
        Map<String, byte[]> pngs = new LinkedHashMap<>();
        Map<String, String> freshName = new LinkedHashMap<>();
        pngs.put(id, basePng);
        freshName.put(id, name);
        for (Map.Entry<String, byte[]> e : colourPngs.entrySet()) {
            String vid = ColorVariantService.variantId(id, e.getKey());
            pngs.put(vid, e.getValue());
            freshName.put(vid, ColorLibrary.stripTrailingColorName(name) + " " + ColorVariantService.labelFor(e.getKey()));
        }

        List<UndoManager.Op> children = new ArrayList<>();
        List<Integer> giveIndices = new ArrayList<>();
        StringBuilder over = new StringBuilder();   // overwritten ids
        StringBuilder made = new StringBuilder();   // freshly-created ids
        boolean slotsRanOut = false;

        for (Map.Entry<String, byte[]> e : pngs.entrySet()) {
            String tid = e.getKey();
            byte[] png = e.getValue();
            boolean isBase = tid.equals(id);
            SlotData existing = SlotManager.getById(tid);
            if (existing != null) {
                // Overwrite TEXTURE only — slot metadata (glow/hardness/sound/collision/category/favourite/name) kept.
                byte[] before = TextureStore.load(existing.index());
                TextureStore.save(existing.index(), png);
                TextureStore.saveSource(existing.index(), isBase ? raw : png);
                if (isBase) TextureStore.saveUrl(existing.index(), link);
                if (before != null) {
                    children.add(new UndoManager.Op(UndoManager.Kind.TEXTURE, existing, existing, before, png, "recolor", null));
                }
                if (over.length() > 0) over.append(CbFmt.DIM + ", ");
                over.append(isBase ? CbFmt.BODY : colourCode(colourKeyOf(id, tid))).append(tid);
            } else {
                // Missing member → create it fresh (CREATE op, given to the player).
                SlotData c = SlotManager.create(tid, freshName.get(tid));
                if (c == null) { slotsRanOut = true; break; }
                TextureStore.save(c.index(), png);
                TextureStore.saveSource(c.index(), isBase ? raw : png);
                if (isBase) TextureStore.saveUrl(c.index(), link);
                children.add(new UndoManager.Op(UndoManager.Kind.CREATE, null, c, null, "create"));
                giveIndices.add(c.index());
                if (made.length() > 0) made.append(CbFmt.DIM + ", ");
                made.append(isBase ? CbFmt.BODY : colourCode(colourKeyOf(id, tid))).append(tid);
            }
        }

        if (children.isEmpty()) {
            Chat.error(src, "Nothing to overwrite — the family may have been removed. Nothing was changed.");
            return;
        }

        ResourcePackServer.updatePack();
        ResourcePackServer.syncToAll();
        HudSync.broadcast(server);

        UndoManager.recordBatch(who, children, "colorvariants overwrite " + id + " (" + children.size() + ")");
        giveAll(src, giveIndices);

        StringBuilder msg = new StringBuilder("Overwrote colour family " + CbFmt.BODY + id + CbFmt.RESET);
        if (over.length() > 0) msg.append(" — repainted ").append(over).append(CbFmt.RESET);
        if (made.length() > 0) msg.append("; added ").append(made).append(CbFmt.RESET);
        msg.append(". " + CbFmt.DIM + "One " + CbFmt.BODY + "/cb undo" + CbFmt.DIM + " puts the old pictures back.");
        Chat.success(src, msg.toString());
        if (slotsRanOut) {
            Chat.error(src, "Ran out of free slots before adding every missing colour — repaints above were kept.");
        }
    }

    // ── shared image rails (one copy for create / rebuild / overwrite) ──

    /** Base / black = the normal upload: strip bg to opaque black, square it, re-snap black. */
    private static byte[] bakeBase(byte[] raw, String baseMode, int baseTol, int size) throws Exception {
        byte[] png = BackgroundRemover.apply(raw, baseMode, baseTol);
        png = ImageProcessor.toBlockPng(png, size);
        return BackgroundRemover.snapBackgroundBlack(png, baseMode, baseTol);
    }

    /** The 3 colours from the SAME source, recoloured to each configured hex (recolour → square → fill). */
    private static Map<String, byte[]> recolourSet(byte[] raw, String baseMode, int colourTol, int size) throws Exception {
        Map<String, byte[]> colourPngs = new LinkedHashMap<>();
        for (String key : DEFAULT_COLOURS) {
            int rgb = ColorVariantService.rgbFor(key);
            byte[] recoloured = BackgroundRemover.recolorBackground(raw, baseMode, colourTol, rgb);
            byte[] png = ImageProcessor.toBlockPng(recoloured, size);
            png = ImageProcessor.fillBackground(png, rgb);
            colourPngs.put(key, png);
        }
        return colourPngs;
    }

    // ── small helpers ──

    /** Which of the family's 3 colour ids (NOT the base) already exist. */
    private static List<String> existingColours(String id) {
        List<String> existing = new ArrayList<>();
        for (String key : DEFAULT_COLOURS) {
            String vid = ColorVariantService.variantId(id, key);
            if (SlotManager.hasId(vid)) existing.add(vid);
        }
        return existing;
    }

    /** Which of the family's target ids (base + the 3 colours) already exist. Package-visible so
     *  ColorFamilyDelete resolves family membership the same way the clash check does. */
    static List<String> takenTargets(String id) {
        List<String> taken = new ArrayList<>();
        if (SlotManager.hasId(id)) taken.add(id);
        taken.addAll(existingColours(id));
        return taken;
    }

    /** The colour key ("red"/"green"/"yellow") a variant id maps to under this base, or "" for the base. */
    private static String colourKeyOf(String id, String tid) {
        for (String key : DEFAULT_COLOURS) {
            if (ColorVariantService.variantId(id, key).equals(tid)) return key;
        }
        return "";
    }

    /** Hand the player every block in the list (drops at their feet if the inventory is full). */
    private static void giveAll(ServerCommandSource src, List<Integer> indices) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return;
        for (int index : indices) {
            var item = SlotManager.itemAt(index);
            if (item != null) p.giveItemStack(new ItemStack(item));
        }
    }

    /** The §-colour code a variant's name renders in (chat/HUD) — matches the Triangle colours. */
    private static String colourCode(String key) {
        return switch (key) {
            case "red"    -> CbFmt.BAD;
            case "green"  -> CbFmt.OK;
            case "yellow" -> CbFmt.VALUE;
            default       -> CbFmt.BODY;
        };
    }
}
