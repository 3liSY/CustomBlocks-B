/**
 * BlockExporter.java
 *
 * Responsibility: Write/read block definitions for the Group 12 import/export system.
 *   - exportAll  — timestamped bulk list for readability/backup (not round-trip importable),
 *                  in json/txt/csv/md/html/yaml
 *   - exportOne  — per-block schema-v2 JSON in cloud_exports/<id>.json (importable by importFolder)
 *   - exportPng / exportAllPng — write the baked block texture(s) as usable .png image files
 *   - importFolder — scan a directory for per-block JSONs and create any missing blocks
 * All writes use atomic temp-rename (NFR-13).
 *
 * G12 rework (2026-07-30 Locked Decisions):
 *   • EVERY artifact lands in ONE place, {@link CbPaths#CLOUD_EXPORTS} — the bulk list files used to
 *     drop into exports/ instead, so a result message could not name a single honest folder.
 *   • Every file is named from the block ID, never the display name (safe on any filesystem), and every
 *     ZIP name carries the date + time so a new bundle can never silently replace an older one.
 *   • Schema v2 records the FULL category membership set ({@link CategoryMembershipStore#of}) as
 *     {@code categories}, replacing the single legacy {@code category} word. There is exactly ONE
 *     layout: no parallel legacy shape is written, and nothing here reads the old single-category one.
 *   • Nothing reports success unseen — {@link #wrote} re-reads the artifact off disk first.
 *
 * Depends on: SlotData, SlotManager, TextureStore, CbPaths, CategoryMembershipStore, CategoryMetadataStore
 * Called by:  UtilityCommands, CategoryCommands, BulkExportCommands, CategoryVaultCommands
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class BlockExporter {

    /** The ONE artifact folder (G12) — resolved from CbPaths, never a hardcoded literal. */
    private static final Path DIR = CbPaths.CLOUD_EXPORTS;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    /** The relative folder path a result message shows the owner, so they can find it in a file panel.
     *  Derived from the real path, so the message can never name a folder we do not write to. */
    public static final String FOLDER_LABEL = CbPaths.label(CbPaths.CLOUD_EXPORTS);
    /** Schema version stamped into a per-block payload. v2 = full {@code categories} set, no legacy word. */
    private static final int SCHEMA = 2;

    private BlockExporter() {}

    /**
     * Validate a just-written artifact and return it, or null if it is missing / empty — so no caller
     * can report a success it did not actually put on disk (G12 §A: "contents are validated before
     * success is reported"). Every export method funnels its return through this.
     */
    private static Path wrote(Path file) {
        try {
            return (Files.isRegularFile(file) && Files.size(file) > 0) ? file : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Byte size of an artifact, or 0 when it can't be read — for the "(3.4 MB)" part of a result line. */
    public static long sizeOf(Path file) {
        try {
            return file == null ? 0L : Files.size(file);
        } catch (Exception e) {
            return 0L;
        }
    }

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
            case "json"           -> { ext = "json"; content = BlockExportFormats.bulkJson(GSON, blocks); }
            case "txt"            -> { ext = "txt";  content = BlockExportFormats.txt(blocks); }
            case "csv"            -> { ext = "csv";  content = BlockExportFormats.csv(blocks); }
            case "md", "markdown" -> { ext = "md";   content = BlockExportFormats.markdown(blocks); }
            case "html"           -> { ext = "html"; content = BlockExportFormats.html(blocks); }
            case "yaml", "yml"    -> { ext = "yml";  content = BlockExportFormats.yaml(blocks); }
            default               -> { return null; }
        }
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve("blocks-" + LocalDateTime.now().format(STAMP) + "." + ext);
            atomicWrite(file, content);
            return wrote(file);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Export one block's baked texture PNG to cloud_exports/&lt;id&gt;.png — named from the block ID, so a
     * display name with spaces or capitals can never produce an awkward or unsafe file name (TG12 A3).
     * Null if it has no texture or the write fails.
     */
    public static Path exportPng(SlotData d) {
        byte[] png = TextureStore.load(d.index());
        if (png == null || png.length == 0) return null;
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(fileStem(d) + ".png");
            atomicWriteBytes(file, png);
            return wrote(file);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The file-name stem for one block: its custom ID, lower-cased with every character that is not
     * {@code a-z 0-9 _ -} folded to an underscore.
     *
     * The ID is already the safe handle — this is the belt-and-braces guarantee behind TG12 A3 that NO
     * export file name can ever be derived from {@link SlotData#displayName()}, whatever a future id rule
     * allows through. Everything that names a file (PNG, per-block JSON, ZIP entries) goes through here,
     * so the JSON, the PNG and the ZIP entry for one block always agree.
     */
    private static String fileStem(SlotData d) {
        String id = d.customId() == null ? "" : d.customId().trim().toLowerCase(Locale.ROOT);
        String safe = id.replaceAll("[^a-z0-9_-]", "_");
        return safe.isEmpty() ? "block-" + d.index() : safe;
    }

    /** Outcome of a bulk PNG export: where it went, how many wrote, how many had no texture. */
    public record PngBatch(Path dir, int written, int skipped) {}

    /** Export every given block's baked texture PNG into cloud_exports/textures-&lt;stamp&gt;/. Null only on directory failure. */
    public static PngBatch exportAllPng(Collection<SlotData> blocks) {
        Path dir = DIR.resolve("textures-" + LocalDateTime.now().format(STAMP));
        int written = 0, skipped = 0;
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            return null;
        }
        for (SlotData d : blocks) {
            byte[] png = TextureStore.load(d.index());
            if (png == null || png.length == 0) { skipped++; continue; }
            try {
                atomicWriteBytes(dir.resolve(fileStem(d) + ".png"), png);
                written++;
            } catch (Exception e) { skipped++; }
        }
        return new PngBatch(dir, written, skipped);
    }

    /**
     * Export one block to cloud_exports/&lt;id&gt;.json (schema v2 — importable by importFolder/importJson).
     * Lands in cloud_exports/ so the HTTP server can serve it as a [download] link.
     * Returns the written path, or null on failure.
     */
    public static Path exportOne(SlotData d) {
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(fileStem(d) + ".json");
            atomicWrite(file, toBlockJson(d));
            return wrote(file);
        } catch (Exception e) {
            return null;
        }
    }

    // toJson(SlotData) is GONE with the Blueprint item (G12, 2026-07-30): it existed only to stuff a
    // block's recipe into that paper item's NBT, and nothing else ever called it.

    /**
     * Apply a block file's metadata onto a block that ALREADY exists — the folder-import path (TG12 B17:
     * a data file next to a same-name picture contributes the attributes while the picture becomes the
     * texture). Reuses the one {@link #applyFields} reader, so a metadata field can never mean one thing
     * to a ZIP import and another to a folder import. Silently does nothing for an unreadable payload:
     * the block and its texture are already good, and the run must not abort over one bad sidecar (rule 12).
     *
     * Returns true when something was actually applied, so a caller can say so honestly.
     */
    public static boolean applyMetadata(SlotData d, String json) {
        if (d == null || json == null || json.isBlank()) return false;
        try {
            JsonObject o = GSON.fromJson(json, JsonObject.class);
            if (o == null) return false;
            applyFields(d, o);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Bundle EVERY given block into one ZIP at cloud_exports/all-YYYYMMDD-HHMMSS.zip
     * (each block contributes &lt;id&gt;.json and, when present, &lt;id&gt;.png). The "Export All"
     * counterpart of exportCategoryZip. Returns the written path, or null on failure. Atomic.
     */
    public static Path exportAllZip(Collection<SlotData> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        return writeZip("all-" + LocalDateTime.now().format(STAMP) + ".zip", blocks);
    }

    // importJson(String) is GONE with the Blueprint item (G12, 2026-07-30): its only caller was
    // /cb importblock reading a held Blueprint's NBT. Single-block import still has two real paths —
    // importFolder (a file on disk) and the G20 vault download — so this was a third, now unreachable one.

    /**
     * Bundle every block in a category into one ZIP at cloud_exports/&lt;category&gt;-YYYYMMDD-HHMMSS.zip.
     * Each block contributes &lt;id&gt;.json (schema v2, importable) and, when present, &lt;id&gt;.png
     * (the baked texture). Returns the written path, or null on failure. Atomic temp-rename.
     *
     * The name carries the TIME as well as the day (G12, 2026-07-30) — it used to stamp the day only,
     * so exporting the same category twice in one day quietly replaced the earlier bundle. A backup that
     * can overwrite yesterday's is not a backup; this matches exportAllZip exactly.
     */
    public static Path exportCategoryZip(String category, Collection<SlotData> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        String safe = (category == null || category.isBlank()) ? "uncategorized"
                : category.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return writeZip(safe + "-" + LocalDateTime.now().format(STAMP) + ".zip", blocks);
    }

    /**
     * The one ZIP writer behind exportAllZip and exportCategoryZip: each block contributes
     * &lt;id&gt;.json plus, when it has one, &lt;id&gt;.png. Built in a .tmp sibling and atomically
     * renamed, then validated, so a half-written bundle is never reported as a success.
     */
    private static Path writeZip(String fileName, Collection<SlotData> blocks) {
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(fileName);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp);
                 ZipOutputStream zip = new ZipOutputStream(os)) {
                for (SlotData d : blocks) {
                    String stem = fileStem(d);
                    zip.putNextEntry(new ZipEntry(stem + ".json"));
                    zip.write(toBlockJson(d).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                    byte[] png = TextureStore.load(d.index());
                    if (png != null && png.length > 0) {
                        zip.putNextEntry(new ZipEntry(stem + ".png"));
                        zip.write(png);
                        zip.closeEntry();
                    }
                }
            }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return wrote(file);
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

    /**
     * Apply optional attribute fields from a per-block JSON onto an already-created slot.
     *
     * Categories arrive as the schema-v2 {@code categories} ARRAY and are added one membership at a
     * time through {@link CategoryMembershipStore#add} — the real G11 model — so a block that was
     * exported from three categories comes back in all three. The old single {@code category} word is
     * not read: G12 keeps exactly one layout, and a reader for the retired one would be the "dupe" the
     * owner ruled out.
     *
     * Called after {@link SlotManager#create} has already returned, so this thread holds NEITHER
     * monitor — which is what keeps it clear of the documented SlotManager↔CategoryMembershipStore
     * lock-order hazard (see the LOCK ORDER banner in CategoryMembershipStore).
     */
    private static void applyFields(SlotData d, JsonObject o) {
        String id = d.customId();
        if (o.has("glow"))        SlotManager.setGlow(id, o.get("glow").getAsInt());
        if (o.has("hardness"))    SlotManager.setHardness(id, o.get("hardness").getAsFloat());
        if (o.has("soundType"))   SlotManager.setSoundType(id, o.get("soundType").getAsString());
        if (o.has("noCollision")) SlotManager.setNoCollision(id, o.get("noCollision").getAsBoolean());
        if (o.has("categories") && o.get("categories").isJsonArray()) {
            for (JsonElement el : o.getAsJsonArray("categories")) {
                try {
                    String key = el.getAsString();
                    if (key != null && !CategoryMembershipStore.isUncategorized(key)) {
                        CategoryMembershipStore.add(id, key);
                    }
                } catch (Exception ignored) {} // one unreadable entry must not lose the rest
            }
        }
    }

    // ── Serialisation ────────────────────────────────────────────────────────
    //
    // HOW a block reads in each format lives in BlockExportFormats — split out when the schema-v2
    // rework pushed this file past the §9.3 500-line cap. This file owns the ARTIFACT (where it goes,
    // what it is called, whether it landed); that one owns the text inside it.

    private static String toBlockJson(SlotData d) {
        return BlockExportFormats.blockJson(GSON, SCHEMA, d);
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
