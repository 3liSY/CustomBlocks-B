/**
 * BlockExportFormats.java — how a block list is written in each readable format (Group 12 §A).
 *
 * Split out of {@link BlockExporter} when the schema-v2 rework pushed that file past the §9.3 500-line
 * cap. Pure text building: it decides how a block READS in json/txt/csv/md/html/yaml and nothing else —
 * no disk, no paths, no naming, no validation. Those stay in BlockExporter, which owns the artifact.
 *
 * All six formats now record EVERY category a block belongs to, read from {@link CategoryMembershipStore}
 * (the real G11 set) rather than the legacy one-word {@link SlotData#category()} shadow. The shadow only
 * ever held the alphabetically-first membership, so a block in three categories used to export as if it
 * were in one — and the bulk JSON recorded no category at all.
 *
 * Depends on: SlotData, CategoryMembershipStore, CategoryMetadataStore, Gson
 * Called by:  BlockExporter
 */
package com.customblocks.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

final class BlockExportFormats {

    private BlockExportFormats() {} // static-only

    /**
     * Every category key a block belongs to, sorted so two exports of the same block match byte for byte.
     * Never empty — G11 guarantees at least the {@code uncategorized} floor.
     */
    static List<String> categoryKeys(SlotData d) {
        return new ArrayList<>(new TreeSet<>(CategoryMembershipStore.of(d.customId())));
    }

    /** The same memberships written the way a person reads them ("Walls, Arabic Letters"). */
    static String categoryNames(SlotData d) {
        List<String> shown = new ArrayList<>();
        for (String k : categoryKeys(d)) shown.add(CategoryMetadataStore.getDisplayName(k));
        return String.join(", ", shown);
    }

    /** The round-trippable per-block payload: schema, attributes, and the full membership array. */
    static String blockJson(Gson gson, int schema, SlotData d) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", schema);
        o.addProperty("id", d.customId());
        o.addProperty("displayName", d.displayName());
        o.addProperty("glow", d.glow());
        o.addProperty("hardness", d.hardness());
        o.addProperty("soundType", d.soundType());
        if (d.noCollision()) o.addProperty("noCollision", true);
        o.add("categories", keyArray(d)); // ALWAYS present — every block holds at least one membership
        return gson.toJson(o);
    }

    static String bulkJson(Gson gson, Collection<SlotData> blocks) {
        JsonObject root = new JsonObject();
        root.addProperty("exported_at", LocalDateTime.now().toString());
        root.addProperty("count", blocks.size());
        JsonArray arr = new JsonArray();
        for (SlotData d : blocks) {
            JsonObject o = new JsonObject();
            o.addProperty("index", d.index());
            o.addProperty("customId", d.customId());
            o.addProperty("displayName", d.displayName());
            o.add("categories", keyArray(d)); // G12: a list export records memberships too, not none
            arr.add(o);
        }
        root.add("blocks", arr);
        return gson.toJson(root);
    }

    private static JsonArray keyArray(SlotData d) {
        JsonArray cats = new JsonArray();
        for (String k : categoryKeys(d)) cats.add(k);
        return cats;
    }

    static String txt(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("CustomBlocks export — ").append(LocalDateTime.now()).append(nl);
        sb.append(blocks.size()).append(" block(s)").append(nl);
        sb.append("------------------------------------------------").append(nl);
        for (SlotData d : blocks) {
            sb.append(d.customId())
              .append("  (slot ").append(d.index()).append(")  \"")
              .append(d.displayName()).append("\"  [").append(categoryNames(d)).append(']').append(nl);
        }
        return sb.toString();
    }

    static String csv(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder("id,name,slot,glow,hardness,sound,collision,categories").append(nl);
        for (SlotData d : blocks)
            sb.append(csvCell(d.customId())).append(',')
              .append(csvCell(d.displayName())).append(',')
              .append(d.index()).append(',')
              .append(d.glow()).append(',')
              .append(d.hardness()).append(',')
              .append(csvCell(d.soundType())).append(',')
              .append(!d.noCollision()).append(',')
              .append(csvCell(categoryNames(d))).append(nl);
        return sb.toString();
    }

    static String markdown(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("# CustomBlocks — ").append(blocks.size()).append(" block(s)").append(nl).append(nl);
        sb.append("| ID | Name | Slot | Glow | Hardness | Sound | Collision | Categories |").append(nl);
        sb.append("|----|------|------|------|----------|-------|-----------|------------|").append(nl);
        for (SlotData d : blocks)
            sb.append("| `").append(md(d.customId())).append("` | ")
              .append(md(d.displayName())).append(" | ")
              .append(d.index()).append(" | ")
              .append(d.glow()).append(" | ")
              .append(d.hardness()).append(" | ")
              .append(md(d.soundType())).append(" | ")
              .append(d.noCollision() ? "no" : "yes").append(" | ")
              .append(md(categoryNames(d))).append(" |").append(nl);
        return sb.toString();
    }

    static String html(Collection<SlotData> blocks) {
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
          .append("<th>Sound</th><th>Collision</th><th>Categories</th></tr></thead><tbody>").append(nl);
        for (SlotData d : blocks)
            sb.append("<tr><td><code>").append(htmlCell(d.customId())).append("</code></td><td>")
              .append(htmlCell(d.displayName())).append("</td><td>").append(d.index()).append("</td><td>")
              .append(d.glow()).append("</td><td>").append(d.hardness()).append("</td><td>")
              .append(htmlCell(d.soundType())).append("</td><td>").append(d.noCollision() ? "no" : "yes")
              .append("</td><td>").append(htmlCell(categoryNames(d)))
              .append("</td></tr>").append(nl);
        sb.append("</tbody></table></body></html>").append(nl);
        return sb.toString();
    }

    static String yaml(Collection<SlotData> blocks) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder("blocks:").append(nl);
        for (SlotData d : blocks)
            sb.append("  - id: ").append(yamlCell(d.customId())).append(nl)
              .append("    name: ").append(yamlCell(d.displayName())).append(nl)
              .append("    slot: ").append(d.index()).append(nl)
              .append("    glow: ").append(d.glow()).append(nl)
              .append("    hardness: ").append(d.hardness()).append(nl)
              .append("    sound: ").append(yamlCell(d.soundType())).append(nl)
              .append("    collision: ").append(!d.noCollision()).append(nl)
              .append("    categories: ").append(yamlCell(categoryNames(d))).append(nl);
        return sb.toString();
    }

    // ── per-format escaping ──────────────────────────────────────────────────

    private static String csvCell(String s) {
        if (s == null) return "";
        return (s.contains(",") || s.contains("\"") || s.contains("\n"))
                ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static String md(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    private static String htmlCell(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String yamlCell(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ") + "\"";
    }
}
