/**
 * TemplateManager.java
 *
 * Responsibility: Persist and apply named block-attribute presets (templates) under
 * config/customblocks/templates/. A template captures glow, hardness, soundType,
 * noCollision, and category — not the texture, id, or slot index. Applying a template
 * stamps those attributes onto any existing block. All writes are atomic (NFR-13).
 *
 * Depends on: SlotData, SlotManager
 * Called by:  TemplateCommands
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class TemplateManager {

    private static final String DIR = "config/customblocks/templates";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TemplateManager() {}

    public record Template(String name, int glow, float hardness, String soundType,
                           boolean noCollision, String category) {}

    /**
     * Save a block's attribute preset under {@code templateName}. Overwrites any existing
     * template with the same name. Returns true on success.
     */
    public static boolean save(String templateName, SlotData d) {
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            JsonObject o = new JsonObject();
            o.addProperty("schema", 1);
            o.addProperty("name", templateName);
            o.addProperty("glow", d.glow());
            o.addProperty("hardness", d.hardness());
            o.addProperty("soundType", d.soundType());
            if (d.noCollision()) o.addProperty("noCollision", true);
            if (!d.category().isEmpty()) o.addProperty("category", d.category());
            atomicWrite(dir.resolve(sanitize(templateName) + ".json"), GSON.toJson(o));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Load a template by name. Returns null if missing or unreadable. */
    public static Template load(String templateName) {
        try {
            Path file = Path.of(DIR).resolve(sanitize(templateName) + ".json");
            if (!Files.exists(file)) return null;
            JsonObject o = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null || !o.has("name")) return null;
            return new Template(
                    o.get("name").getAsString(),
                    o.has("glow")        ? o.get("glow").getAsInt()         : 0,
                    o.has("hardness")    ? o.get("hardness").getAsFloat()   : SlotData.DEFAULT_HARDNESS,
                    o.has("soundType")   ? o.get("soundType").getAsString() : SlotData.DEFAULT_SOUND,
                    o.has("noCollision") && o.get("noCollision").getAsBoolean(),
                    o.has("category")    ? o.get("category").getAsString()  : SlotData.DEFAULT_CATEGORY
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Apply a template's attributes to an existing block. Returns the updated SlotData,
     * or null if either the template or the block doesn't exist.
     */
    public static SlotData apply(String templateName, String blockId) {
        Template t = load(templateName);
        if (t == null) return null;
        if (SlotManager.getById(blockId) == null) return null;
        SlotManager.setGlow(blockId, t.glow());
        SlotManager.setHardness(blockId, t.hardness());
        SlotManager.setSoundType(blockId, t.soundType());
        SlotManager.setNoCollision(blockId, t.noCollision());
        SlotManager.setCategory(blockId, t.category());
        return SlotManager.getById(blockId);
    }

    /** All saved template names, sorted alphabetically. */
    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try {
            Path dir = Path.of(DIR);
            if (!Files.isDirectory(dir)) return names;
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    String fname = p.getFileName().toString();
                    names.add(fname.substring(0, fname.length() - 5));
                });
            }
            names.sort(String::compareTo);
        } catch (Exception ignored) {}
        return names;
    }

    public static boolean exists(String templateName) {
        return Files.exists(Path.of(DIR).resolve(sanitize(templateName) + ".json"));
    }

    public static boolean delete(String templateName) {
        try {
            return Files.deleteIfExists(Path.of(DIR).resolve(sanitize(templateName) + ".json"));
        } catch (Exception e) {
            return false;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static void atomicWrite(Path file, String content) throws Exception {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
