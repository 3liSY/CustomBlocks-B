/**
 * BlockExporter.java
 *
 * Responsibility: Write/read block definitions for the Phase 9 import/export system.
 *   - exportAll  — timestamped bulk list for readability/backup (not round-trip importable),
 *                  in json/txt/csv/md/html/yaml
 *   - exportOne  — per-block schema-v1 JSON in exports/<id>.json (importable by importFolder)
 *   - exportPng / exportAllPng — write the baked block texture(s) as usable .png image files
 *   - importFolder — scan a directory for per-block JSONs and create any missing blocks
 * All writes use atomic temp-rename (NFR-13).
 *
 * Depends on: SlotData, SlotManager, TextureStore
 * Called by:  UtilityCommands, BulkExportCommands
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BlockExporter {

    private static final String DIR = "config/customblocks/exports";
    private static final String CLOUD_DIR = "config/customblocks/cloud_exports";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BlockExporter() {}

    /** Returns true if {@code format} is a supported TEXT bulk export format (png is separate — see exportPng). */
    public static boolean isSupported(String format) {
        if (format == null) return false;
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "json", "txt", "csv", "md", "markdown", "html", "yaml", "yml" -> true;
            default -> false;
        };
    }

    /**
     * Export all given blocks to a timestamped bulk file. Returns the written path,
     * or null on unsupported format / write failure.
     */
    public static Path exportAll(String format, Collection<SlotData> blocks) {
        if (!isSupported(format)) return null;
        String ext;
        String content;
        switch (format.toLowerCase(Locale.ROOT)) {
            case "json"           -> { ext = "json"; content = toBulkJson(blocks); }
            case "txt"            -> { ext = "txt";  content = toTxt(blocks); }
            case "csv"            -> { ext = "csv";  content = toCsv(blocks); }
            case "md", "markdown" -> { ext = "md";   content = toMarkdown(blocks); }
            case "html"           -> { ext = "html"; content = toHtml(blocks); }
            case "yaml", "yml"    -> { ext = "yml";  content = toYaml(blocks); }
            default               -> { return null; }
        }
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("blocks-" + LocalDateTime.now().format(STAMP) + "." + ext);
            atomicWrite(file, content);
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /** Export one block's baked texture PNG to cloud_exports/&lt;id&gt;.png. Null if it has no texture or the write fails. */
    public static Path exportPng(SlotData d) {
        byte[] png = TextureStore.load(d.index());
        if (png == null || png.length == 0) return null;
        try {
            Path dir = Path.of(CLOUD_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(d.customId() + ".png");
            atomicWriteBytes(file, png);
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /** Outcome of a bulk PNG export: where it went, how many wrote, how many had no texture. */
    public record PngBatch(Path dir, int written, int skipped) {}

    /** Export every given block's baked texture PNG into exports/textures-&lt;stamp&gt;/. Null only on directory failure. */
    public static PngBatch exportAllPng(Collection<SlotData> blocks) {
        Path dir = Path.of(DIR, "textures-" + LocalDateTime.now().format(STAMP));
        int written = 0, skipped = 0;
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            return null;
        }
        for (SlotData d : blocks) {
            byte[] png = TextureStore.load(d.index());
            if (png == null || png.length == 0) { skipped++; continue; }
            try { atomicWriteBytes(dir.resolve(d.customId() + ".png"), png); written++; }
            catch (Exception e) { skipped++; }
        }
        return new PngBatch(dir, written, skipped);
    }

    /**
     * Export one block to cloud_exports/<id>.json (schema v1 — importable by importFolder/importJson).
     * Lands in cloud_exports/ so the HTTP server can serve it as a [download] link.
     * Returns the written path, or null on failure.
     */
    public static Path exportOne(SlotData d) {
        try {
            Path dir = Path.of(CLOUD_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(d.customId() + ".json");
            atomicWrite(file, toBlockJson(d));
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /** Public access to one block's schema-v1 JSON (used by the Blueprint item). */
    public static String toJson(SlotData d) {
        return toBlockJson(d);
    }

    /**
     * Bundle EVERY given block into one ZIP at cloud_exports/all-YYYYMMDD-HHMMSS.zip
     * (each block contributes &lt;id&gt;.json and, when present, &lt;id&gt;.png). The "Export All"
     * counterpart of exportCategoryZip. Returns the written path, or null on failure. Atomic.
     */
    public static Path exportAllZip(Collection<SlotData> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        try {
            Path dir = Path.of(CLOUD_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("all-" + LocalDateTime.now().format(STAMP) + ".zip");
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp);
                 ZipOutputStream zip = new ZipOutputStream(os)) {
                for (SlotData d : blocks) {
                    zip.putNextEntry(new ZipEntry(d.customId() + ".json"));
                    zip.write(toBlockJson(d).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                    byte[] png = TextureStore.load(d.index());
                    if (png != null && png.length > 0) {
                        zip.putNextEntry(new ZipEntry(d.customId() + ".png"));
                        zip.write(png);
                        zip.closeEntry();
                    }
                }
            }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recreate one block from a schema-v1 JSON string (as carried by a Blueprint item or a
     * single &lt;id&gt;.json). Creates the block if its id is free; reports it skipped if the id
     * already exists; reports failed on a malformed payload or no free slot. Never throws.
     */
    public static ImportResult importJson(String json) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();
        try {
            JsonObject o = GSON.fromJson(json, JsonObject.class);
            if (o == null || !o.has("id")) { failed.add("not a block blueprint"); return new ImportResult(created, skipped, failed); }
            String id = o.get("id").getAsString();
            if (id.isBlank()) { failed.add("blank id"); return new ImportResult(created, skipped, failed); }
            if (SlotManager.hasId(id)) { skipped.add(id); return new ImportResult(created, skipped, failed); }
            String name = o.has("displayName") ? o.get("displayName").getAsString() : id;
            SlotData d = SlotManager.create(id, name);
            if (d == null) { failed.add(id + " (no free slot)"); return new ImportResult(created, skipped, failed); }
            applyFields(d, o);
            created.add(id);
        } catch (Exception e) {
            failed.add("bad blueprint (" + e.getMessage() + ")");
        }
        return new ImportResult(created, skipped, failed);
    }

    /**
     * Bundle every block in a category into one ZIP at cloud_exports/&lt;category&gt;-YYYYMMDD.zip.
     * Each block contributes &lt;id&gt;.json (schema v1, importable) and, when present, &lt;id&gt;.png
     * (the baked texture). Returns the written path, or null on failure. Atomic temp-rename.
     */
    public static Path exportCategoryZip(String category, Collection<SlotData> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        String safe = (category == null || category.isBlank()) ? "uncategorized"
                : category.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        try {
            Path dir = Path.of(CLOUD_DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve(safe + "-" + LocalDateTime.now().format(DAY) + ".zip");
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp);
                 ZipOutputStream zip = new ZipOutputStream(os)) {
                for (SlotData d : blocks) {
                    zip.putNextEntry(new ZipEntry(d.customId() + ".json"));
                    zip.write(toBlockJson(d).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                    byte[] png = TextureStore.load(d.index());
                    if (png != null && png.length > 0) {
                        zip.putNextEntry(new ZipEntry(d.customId() + ".png"));
                        zip.write(png);
                        zip.closeEntry();
                    }
                }
            }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * Import a category ZIP (as produced by exportCategoryZip / downloaded from the vault):
     * extract its per-block JSONs into a fresh imports/ folder and create any missing blocks.
     * Entry names are reduced to their file name (zip-slip safe). Textures bundled in the ZIP are
     * NOT yet re-applied — like importFolder, this restores block definitions only.
     * Never throws — always returns a result.
     */
    public static ImportResult importCategoryZip(byte[] zipBytes) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0) {
            failed.add("empty download");
            return new ImportResult(created, skipped, failed);
        }
        try {
            Path dir = Path.of("config/customblocks/imports", "import-" + LocalDateTime.now().format(STAMP));
            Files.createDirectories(dir);
            try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry e;
                while ((e = zin.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    String name = Path.of(e.getName()).getFileName().toString(); // strip any path (zip-slip safe)
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (!lower.endsWith(".json") && !lower.endsWith(".png")) continue;
                    Files.write(dir.resolve(name), zin.readAllBytes());
                    zin.closeEntry();
                }
            }
            return importFolder(dir);
        } catch (Exception ex) {
            failed.add("unzip error: " + ex.getMessage());
            return new ImportResult(created, skipped, failed);
        }
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

    private static String toCsv(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder("id,name,slot,glow,hardness,sound,collision,category").append(nl);
        for (SlotData d : blocks)
            sb.append(csv(d.customId())).append(',')
              .append(csv(d.displayName())).append(',')
              .append(d.index()).append(',')
              .append(d.glow()).append(',')
              .append(d.hardness()).append(',')
              .append(csv(d.soundType())).append(',')
              .append(!d.noCollision()).append(',')
              .append(csv(d.category())).append(nl);
        return sb.toString();
    }

    private static String toMarkdown(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("# CustomBlocks — ").append(blocks.size()).append(" block(s)").append(nl).append(nl);
        sb.append("| ID | Name | Slot | Glow | Hardness | Sound | Collision | Category |").append(nl);
        sb.append("|----|------|------|------|----------|-------|-----------|----------|").append(nl);
        for (SlotData d : blocks)
            sb.append("| `").append(md(d.customId())).append("` | ")
              .append(md(d.displayName())).append(" | ")
              .append(d.index()).append(" | ")
              .append(d.glow()).append(" | ")
              .append(d.hardness()).append(" | ")
              .append(md(d.soundType())).append(" | ")
              .append(d.noCollision() ? "no" : "yes").append(" | ")
              .append(md(d.category().isEmpty() ? "—" : d.category())).append(" |").append(nl);
        return sb.toString();
    }

    private static String toHtml(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>").append(nl).append("<html lang=\"en\"><head><meta charset=\"UTF-8\">").append(nl)
          .append("<title>CustomBlocks — Block List</title>").append(nl)
          .append("<style>body{font-family:system-ui,Arial,sans-serif;margin:2rem;background:#1b1b1f;color:#e8e8ea}")
          .append("h1{font-weight:500}table{border-collapse:collapse;width:100%}")
          .append("th,td{border:1px solid #3a3a40;padding:6px 10px;text-align:left}")
          .append("th{background:#26262b}tr:nth-child(even){background:#222227}code{color:#7fd1ff}</style>").append(nl)
          .append("</head><body>").append(nl)
          .append("<h1>CustomBlocks — ").append(blocks.size()).append(" block(s)</h1>").append(nl)
          .append("<table><thead><tr><th>ID</th><th>Name</th><th>Slot</th><th>Glow</th><th>Hardness</th>")
          .append("<th>Sound</th><th>Collision</th><th>Category</th></tr></thead><tbody>").append(nl);
        for (SlotData d : blocks)
            sb.append("<tr><td><code>").append(html(d.customId())).append("</code></td><td>")
              .append(html(d.displayName())).append("</td><td>").append(d.index()).append("</td><td>")
              .append(d.glow()).append("</td><td>").append(d.hardness()).append("</td><td>")
              .append(html(d.soundType())).append("</td><td>").append(d.noCollision() ? "no" : "yes")
              .append("</td><td>").append(html(d.category().isEmpty() ? "—" : d.category()))
              .append("</td></tr>").append(nl);
        sb.append("</tbody></table></body></html>").append(nl);
        return sb.toString();
    }

    private static String toYaml(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder("blocks:").append(nl);
        for (SlotData d : blocks)
            sb.append("  - id: ").append(yaml(d.customId())).append(nl)
              .append("    name: ").append(yaml(d.displayName())).append(nl)
              .append("    slot: ").append(d.index()).append(nl)
              .append("    glow: ").append(d.glow()).append(nl)
              .append("    hardness: ").append(d.hardness()).append(nl)
              .append("    sound: ").append(yaml(d.soundType())).append(nl)
              .append("    collision: ").append(!d.noCollision()).append(nl)
              .append("    category: ").append(yaml(d.category())).append(nl);
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        return (s.contains(",") || s.contains("\"") || s.contains("\n"))
                ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String md(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    private static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String yaml(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ") + "\"";
    }

    private static void atomicWrite(Path file, String content) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void atomicWriteBytes(Path file, byte[] bytes) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
