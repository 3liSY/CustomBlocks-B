/**
 * BlockExporter.java
 *
 * Responsibility: Write/read block definitions for the Phase 9 import/export system.
 *   - exportAll  — timestamped bulk JSON/TXT for readability/backup (not round-trip importable)
 *   - exportOne  — per-block schema-v1 JSON in exports/<id>.json (importable by importFolder)
 *   - importFolder — scan a directory for per-block JSONs and create any missing blocks
 * All writes use atomic temp-rename (NFR-13).
 *
 * Depends on: SlotData, SlotManager
 * Called by:  UtilityCommands
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BlockExporter {

    private static final String DIR = "config/customblocks/exports";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private BlockExporter() {}

    /** Returns true if {@code format} is a supported bulk export format. */
    public static boolean isSupported(String format) {
        return "json".equalsIgnoreCase(format) || "txt".equalsIgnoreCase(format);
    }

    /**
     * Export all given blocks to a timestamped bulk file. Returns the written path,
     * or null on unsupported format / write failure.
     */
    public static Path exportAll(String format, Collection<SlotData> blocks) {
        if (!isSupported(format)) return null;
        String fmt = format.toLowerCase();
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("blocks-" + LocalDateTime.now().format(STAMP) + "." + fmt);
            atomicWrite(file, fmt.equals("json") ? toBulkJson(blocks) : toTxt(blocks));
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Export one block to exports/<id>.json (schema v1 — importable by importFolder).
     * Returns the written path, or null on failure.
     */
    public static Path exportOne(SlotData d) {
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(d.customId() + ".json");
            atomicWrite(file, toBlockJson(d));
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /** Result of importing a folder: three categorised lists of block ids. */
    public record ImportResult(List<String> created, List<String> skipped, List<String> failed) {}

    /**
     * Scan {@code folder} for *.json files that match the per-block schema (must have an
     * "id" field). Creates any blocks whose id does not already exist; skips existing ones;
     * reports malformed files or no-free-slot failures. Never throws — always returns a result.
     */
    public static ImportResult importFolder(Path folder) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();
        try {
            if (!Files.isDirectory(folder)) {
                failed.add("not a directory: " + folder);
                return new ImportResult(created, skipped, failed);
            }
            try (var stream = Files.list(folder)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                    try {
                        JsonObject o = GSON.fromJson(
                                Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
                        // Bulk export files lack an "id" field — silently skip them.
                        if (o == null || !o.has("id")) return;
                        String id = o.get("id").getAsString();
                        if (id.isBlank()) { failed.add(file.getFileName() + " (blank id)"); return; }
                        if (SlotManager.hasId(id))  { skipped.add(id); return; }
                        String name = o.has("displayName") ? o.get("displayName").getAsString() : id;
                        SlotData d = SlotManager.create(id, name);
                        if (d == null) { failed.add(id + " (no free slot)"); return; }
                        applyFields(d, o);
                        created.add(id);
                    } catch (Exception e) {
                        failed.add(file.getFileName() + " (" + e.getMessage() + ")");
                    }
                });
            }
        } catch (IOException e) {
            failed.add("scan error: " + e.getMessage());
        }
        return new ImportResult(created, skipped, failed);
    }

    /** Apply optional attribute fields from a per-block JSON onto an already-created slot. */
    private static void applyFields(SlotData d, JsonObject o) {
        String id = d.customId();
        if (o.has("glow"))        SlotManager.setGlow(id, o.get("glow").getAsInt());
        if (o.has("hardness"))    SlotManager.setHardness(id, o.get("hardness").getAsFloat());
        if (o.has("soundType"))   SlotManager.setSoundType(id, o.get("soundType").getAsString());
        if (o.has("noCollision")) SlotManager.setNoCollision(id, o.get("noCollision").getAsBoolean());
        if (o.has("category"))    SlotManager.setCategory(id, o.get("category").getAsString());
    }

    // ── Serialisation ────────────────────────────────────────────────────────

    private static String toBlockJson(SlotData d) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", 1);
        o.addProperty("id", d.customId());
        o.addProperty("displayName", d.displayName());
        o.addProperty("glow", d.glow());
        o.addProperty("hardness", d.hardness());
        o.addProperty("soundType", d.soundType());
        if (d.noCollision()) o.addProperty("noCollision", true);
        if (!d.category().isEmpty()) o.addProperty("category", d.category());
        return GSON.toJson(o);
    }

    private static String toBulkJson(Collection<SlotData> blocks) {
        JsonObject root = new JsonObject();
        root.addProperty("exported_at", LocalDateTime.now().toString());
        root.addProperty("count", blocks.size());
        JsonArray arr = new JsonArray();
        for (SlotData d : blocks) {
            JsonObject o = new JsonObject();
            o.addProperty("index", d.index());
            o.addProperty("customId", d.customId());
            o.addProperty("displayName", d.displayName());
            arr.add(o);
        }
        root.add("blocks", arr);
        return GSON.toJson(root);
    }

    private static String toTxt(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("CustomBlocks export — ").append(LocalDateTime.now()).append(nl);
        sb.append(blocks.size()).append(" block(s)").append(nl);
        sb.append("------------------------------------------------").append(nl);
        for (SlotData d : blocks) {
            sb.append(d.customId())
              .append("  (slot ").append(d.index()).append(")  \"")
              .append(d.displayName()).append("\"").append(nl);
        }
        return sb.toString();
    }

    private static void atomicWrite(Path file, String content) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
