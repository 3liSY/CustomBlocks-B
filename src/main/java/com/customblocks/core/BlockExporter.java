/**
 * BlockExporter.java
 *
 * Responsibility: Write the current block list to a human-readable export file
 * (.json or .txt) under config/customblocks/exports/. Atomic write (NFR-13).
 *
 * This is the seed of the full Phase 9 import/export system — for now it just dumps
 * the list of assigned blocks.
 *
 * Depends on: SlotData
 * Called by:  UtilityCommands (the /cb export command + /cb list buttons)
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public final class BlockExporter {

    private static final String DIR = "config/customblocks/exports";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private BlockExporter() {} // static-only

    /** Returns true if {@code format} is a supported export format. */
    public static boolean isSupported(String format) {
        return "json".equalsIgnoreCase(format) || "txt".equalsIgnoreCase(format);
    }

    /**
     * Export all given blocks to a timestamped file. Returns the written file path,
     * or null if the format is unsupported or the write fails.
     */
    public static Path exportAll(String format, Collection<SlotData> blocks) {
        if (!isSupported(format)) return null;
        String fmt = format.toLowerCase();
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path file = dir.resolve("blocks-" + LocalDateTime.now().format(STAMP) + "." + fmt);
            String content = fmt.equals("json") ? toJson(blocks) : toTxt(blocks);
            Path tmp = dir.resolve(file.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toJson(Collection<SlotData> blocks) {
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
}
