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
            ResourcePackServer.updatePack();
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
