/**
 * ArabicBlockRegistry.java
 *
 * Responsibility: Create and manage the Arabic letter/word block definitions.
 * Each letter gets a CustomBlocks slot with a Java2D-rendered texture.
 * importAll() creates all 28 letters in one call; individual letters can be
 * imported or re-textured on demand.
 *
 * Persists a registry of created IDs to config/customblocks/arabic_registry.json.
 *
 * Depends on: ArabicLetterMap, ArabicWordRenderer, SlotManager, TextureStore,
 *             ResourcePackServer
 * Called by: ArabicCommands
 */
package com.customblocks.arabic;

import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.network.ResourcePackServer;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ArabicBlockRegistry {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Arabic");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR  = Path.of("config/customblocks");
    private static final Path FILE = DIR.resolve("arabic_registry.json");

    private ArabicBlockRegistry() {}

    /**
     * Create the bundled NUMBER blocks (Eastern A0-A9 + Western E0-E9, x4 colors = 80) from the JAR
     * PNGs, skipping any that already exist. Saves slot data ONCE and (when {@code rebuild}) rebuilds
     * the pack once. Idempotent: a second call with everything present does no work. Boot passes
     * rebuild=false (the SERVER_STARTED handler builds the pack); the /cb arabic import command true.
     *
     * The 144 LETTER blocks are NO LONGER created here — the auto-join system (customblocks:arabic_letter,
     * zero-slot NBT variants) replaced them. Numbers never join, so they stay as static slot blocks.
     */
    public static ImportResult importArt(boolean rebuild) {
        int created = 0, skipped = 0, failed = 0;
        boolean anyNew = false;
        for (ArabicArt.Glyph g : ArabicArt.ALL) {
            if (g.group() == ArabicArt.Group.LETTER) continue; // letters retired → auto-join only
            for (String color : ArabicArt.COLORS) {
                String id = ArabicArt.blockId(g, color);
                if (SlotManager.getById(id) != null) { skipped++; continue; }
                byte[] png = readResource(ArabicArt.resource(g, color));
                if (png == null) {
                    failed++;
                    LOG.warn("[CustomBlocks/Arabic] Bundled art missing: {}", ArabicArt.resource(g, color));
                    continue;
                }
                SlotData d = SlotManager.createNoSave(id, ArabicArt.displayName(g, color), ArabicArt.category(g));
                if (d == null) { failed++; continue; } // no free slot
                TextureStore.save(d.index(), png);
                created++;
                anyNew = true;
            }
        }
        if (anyNew) {
            SlotManager.saveAll();
            if (rebuild) ResourcePackServer.updatePack();
        }
        LOG.info("[CustomBlocks/Arabic] Art import: {} created, {} skipped, {} failed.",
                created, skipped, failed);
        return new ImportResult(created, skipped, failed);
    }

    /** Read a bundled JAR resource fully, or null if absent. */
    private static byte[] readResource(String path) {
        try (InputStream in = ArabicBlockRegistry.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (Exception e) {
            LOG.warn("[CustomBlocks/Arabic] Failed reading bundled art {}: {}", path, e.getMessage());
            return null;
        }
    }

    /** Import all 28 base letters. Returns a result summary. */
    public static ImportResult importAll(int textColor, int bgColor) {
        int created = 0, skipped = 0, failed = 0;
        boolean anyNew = false;
        for (ArabicLetterMap.Letter letter : ArabicLetterMap.ALL) {
            if (SlotManager.getById(letter.blockId()) != null) { skipped++; continue; }
            if (importLetter(letter, textColor, bgColor)) { created++; anyNew = true; }
            else failed++;
        }
        if (anyNew) ResourcePackServer.updatePack();
        saveRegistry();
        return new ImportResult(created, skipped, failed);
    }

    /** Import or re-texture a single letter. Returns true on success. */
    public static boolean importLetter(ArabicLetterMap.Letter letter, int textColor, int bgColor) {
        try {
            byte[] tex = ArabicWordRenderer.render(letter.asString(), textColor, bgColor);
            if (tex == null) return false;
            SlotData existing = SlotManager.getById(letter.blockId());
            if (existing != null) {
                TextureStore.save(existing.index(), tex);
                ResourcePackServer.updatePack();
                return true;
            }
            String display = letter.nameEn() + " (" + letter.nameAr() + ")";
            SlotData created = SlotManager.create(letter.blockId(), display);
            if (created == null) return false;
            TextureStore.save(created.index(), tex);
            return true;
        } catch (Exception e) {
            LOG.error("[CustomBlocks/Arabic] Failed to import letter {}", letter.blockId(), e);
            return false;
        }
    }

    /** Render custom Arabic text and create/update a block. Returns null on success or an error string. */
    public static String importWord(String text, String id, String displayName, int textColor, int bgColor) {
        return importWord(text, id, displayName, textColor, bgColor, true);
    }

    /**
     * As {@link #importWord(String, String, String, int, int)}, but {@code rebuildPack == false} skips
     * the resource-pack rebuild. Used by the live preview: the throwaway texture is served pack-free
     * over the {@code /tex/<id>} endpoint (straight from TextureStore), so the pack never needs the slot
     * — no rebuild, no client pack prompt.
     */
    public static String importWord(String text, String id, String displayName, int textColor, int bgColor, boolean rebuildPack) {
        try {
            byte[] tex = ArabicWordRenderer.render(text, textColor, bgColor);
            if (tex == null) return "Renderer failed — check font config";
            SlotData existing = SlotManager.getById(id);
            if (existing == null) {
                SlotData created = SlotManager.create(id, displayName);
                if (created == null) return "No free slots available";
                TextureStore.save(created.index(), tex);
            } else {
                TextureStore.save(existing.index(), tex);
            }
            if (rebuildPack) ResourcePackServer.updatePack();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static void saveRegistry() {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (ArabicLetterMap.Letter l : ArabicLetterMap.ALL) {
                if (SlotManager.getById(l.blockId()) != null) arr.add(l.blockId());
            }
            root.add("blocks", arr);
            Path tmp = DIR.resolve("arabic_registry.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks/Arabic] Failed to save registry", e);
        }
    }

    public record ImportResult(int created, int skipped, int failed) {}
}
