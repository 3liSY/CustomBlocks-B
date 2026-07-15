/**
 * SourceAudit.java
 *
 * Responsibility: a READ-ONLY survey of every block's "re-make material" — for each block, does it
 * have a saved original image on disk (.src → re-bakeable sharper with NO download), only a saved
 * web link (.url → re-makeable only by re-downloading), or nothing (the owner must supply a bigger
 * picture)? Sorts all blocks into those three buckets and writes a plain-English .txt report to
 * config/customblocks/exports/. Mutates nothing, downloads nothing, deletes nothing.
 *
 * Background: most old blocks look soft because their SOURCE images were small, not because of any
 * render bug or file damage (see Group 14 handoff). A block can only be made sharper by re-baking it
 * from a bigger source — so the owner first needs to SEE which blocks even have a source to re-bake
 * from. This produces that list.
 *
 * Depends on: SlotData, TextureStore
 * Called by:  SourceListCommands (/cb sourcelist)
 */
package com.customblocks.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SourceAudit {

    private static final String DIR = "config/customblocks/exports";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SourceAudit() {} // static-only

    /** Which re-make material a block has, best (AUTO_READY) to none (NEEDS_IMAGE). */
    public enum Bucket { AUTO_READY, LINK_ONLY, NEEDS_IMAGE }

    /** One block's audit row. {@code size} = baked px width (0 = unknown); {@code url} only for LINK_ONLY. */
    public record Row(int slot, String id, String name, int size, boolean animated, Bucket bucket, String url) {}

    /** Outcome of a report run: where it was written (null on write failure) + the bucket tallies. */
    public record Result(Path file, int total, int autoReady, int linkOnly, int needsImage, int animated) {}

    /**
     * Classify a snapshot of blocks. Reads only the texture/source/url files via TextureStore — safe to
     * call off the server thread once {@code blocks} has been snapshotted on it. Never throws.
     */
    public static List<Row> audit(List<SlotData> blocks) {
        List<Row> rows = new ArrayList<>();
        for (SlotData d : blocks) {
            int idx = d.index();
            int size = TextureStore.bakedWidth(idx);
            String url = TextureStore.loadUrl(idx);
            // A saved original image wins (no download needed); else a saved link; else nothing.
            Bucket b = TextureStore.hasSource(idx) ? Bucket.AUTO_READY
                    : (url != null ? Bucket.LINK_ONLY : Bucket.NEEDS_IMAGE);
            rows.add(new Row(idx, d.customId(), d.displayName(), size, d.isAnimated(), b, url));
        }
        return rows;
    }

    /** Audit + write the .txt report. Returns the tallies + the file path (file == null only on write failure). */
    public static Result writeReport(List<SlotData> blocks) {
        List<Row> rows = audit(blocks);
        int auto = 0, link = 0, need = 0, anim = 0;
        for (Row r : rows) {
            switch (r.bucket()) {
                case AUTO_READY -> auto++;
                case LINK_ONLY -> link++;
                case NEEDS_IMAGE -> need++;
            }
            if (r.animated()) anim++;
        }
        Path file = write(rows, auto, link, need, anim);
        return new Result(file, rows.size(), auto, link, need, anim);
    }

    private static Path write(List<Row> rows, int auto, int link, int need, int anim) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("CustomBlocks - block source report").append(nl);
        sb.append(LocalDateTime.now()).append(nl);
        sb.append("============================================================").append(nl).append(nl);
        sb.append("WHAT THIS IS: which of your blocks can be re-made sharper, and how.").append(nl);
        sb.append("A block is only as sharp as the picture it was made from. Re-making at 512").append(nl);
        sb.append("only adds sharpness when a bigger / clearer picture is available.").append(nl).append(nl);
        sb.append("Totals: ").append(rows.size()).append(" block(s)  |  ")
          .append(auto).append(" auto-ready  |  ").append(link).append(" link-only  |  ")
          .append(need).append(" need a picture from you  |  ").append(anim).append(" animated").append(nl).append(nl);

        appendBucket(sb, nl, rows, Bucket.AUTO_READY, false,
                "AUTO-READY - a saved picture is on disk; re-bakeable at 512 with NO download.");
        appendBucket(sb, nl, rows, Bucket.LINK_ONLY, true,
                "LINK-ONLY - only a saved web link; re-makeable only by re-downloading "
                        + "(helps only if that link's image is big).");
        appendBucket(sb, nl, rows, Bucket.NEEDS_IMAGE, false,
                "NEED A PICTURE FROM YOU - no saved picture or link; supply a bigger image to sharpen it.");
        try {
            Path dir = Path.of(DIR);
            Files.createDirectories(dir);
            Path f = dir.resolve("block-sources-" + LocalDateTime.now().format(STAMP) + ".txt");
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static void appendBucket(StringBuilder sb, String nl, List<Row> rows, Bucket b,
                                     boolean showUrl, String title) {
        long n = rows.stream().filter(r -> r.bucket() == b).count();
        sb.append("------------------------------------------------------------").append(nl);
        sb.append(title).append("  (").append(n).append(")").append(nl);
        sb.append("------------------------------------------------------------").append(nl);
        for (Row r : rows) {
            if (r.bucket() != b) continue;
            sb.append("  ").append(r.id())
              .append("  (slot ").append(r.slot()).append(", ")
              .append(r.size() > 0 ? r.size() + "px" : "size?")
              .append(r.animated() ? ", GIF" : "")
              .append(")  \"").append(r.name()).append("\"").append(nl);
            if (showUrl && r.url() != null) sb.append("      ").append(r.url()).append(nl);
        }
        sb.append(nl);
    }
}
