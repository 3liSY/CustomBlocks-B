/**
 * VaultBlockCodec.java
 *
 * Responsibility: Pack ONE block into a self-contained ZIP for the Cloud Vault, and unpack a
 * downloaded ZIP back into a live block — restoring its definition AND its textures (Group 20, S1).
 *
 * The category share path (BlockExporter.importCategoryZip) restores block *definitions* only and
 * omits animation, so it can't faithfully round-trip a textured/animated block. This codec carries
 * everything needed to recreate a block exactly:
 *   block.json      — the slot's full JSON (reuses SlotDataStore's slots.json shape, incl. anim)
 *   texture.png     — the baked texture (the frame grid for animated blocks)
 *   source.src      — the original downloaded image for static blocks, when one was kept
 *   face_<f>.png    — any per-face override textures (M4), one per present face
 *
 * On download the source slot's index is ignored: a fresh slot is allocated and all fields,
 * animation and textures are applied to it, then the caller regenerates the pack. The animation
 * .mcmeta / grid sidecar is NOT carried — ServerPackGenerator rebuilds it from AnimData at pack
 * build, exactly as for a locally-created animated block.
 *
 * Depends on: SlotManager, SlotData, SlotDataStore, TextureStore, AnimData, BlockExporter.ImportResult
 * Called by:  CloudCommands (/cb vault upload|download)
 */
package com.customblocks.cloud;

import com.customblocks.core.AnimData;
import com.customblocks.core.BlockExporter.ImportResult;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotDataStore;
import com.customblocks.core.SlotManager;
import com.customblocks.core.TextureStore;
import com.customblocks.image.AnimationDecoder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class VaultBlockCodec {

    private VaultBlockCodec() {} // static-only

    private static final Gson GSON = new Gson();
    private static final String JSON_ENTRY = "block.json";
    private static final String TEX_ENTRY = "texture.png";
    private static final String SRC_ENTRY = "source.src";
    private static final String FACE_PREFIX = "face_";
    private static final int VAULT_MAX_BYTES = 5 * 1024 * 1024; // cloudflare/worker.js MAX_BYTES
    private static final int[] SHARE_CELLS = {512, 256, 128, 64, 32, 16};
    private static final ThreadLocal<String> LAST_PACK_ERROR = ThreadLocal.withInitial(() -> "");

    /**
     * Pack one block into an in-memory ZIP. Returns the bytes, or null if the block has no baked
     * texture (nothing worth sharing) or on any failure. Never throws.
     */
    public static byte[] pack(SlotData d) {
        LAST_PACK_ERROR.set("");
        if (d == null) return null;
        byte[] texture = TextureStore.load(d.index());
        if (texture == null || texture.length == 0) {
            fail("does it have a texture yet?");
            return null;
        }
        try {
            if (d.isAnimated()) {
                return packAnimated(d, texture);
            }

            byte[] zip = zipBlock(d, texture, TextureStore.loadSource(d.index()));
            if (fitsVault(zip)) return zip;
            zip = zipBlock(d, texture, null);
            if (fitsVault(zip)) return zip;
            fail("share package is larger than the 5 MB vault limit.");
            return null;
        } catch (Exception e) {
            fail("pack error (" + e.getMessage() + ")");
            return null;
        }
    }

    /** Human reason for the most recent {@link #pack(SlotData)} failure on this thread. */
    public static String lastPackError() {
        String msg = LAST_PACK_ERROR.get();
        return (msg == null || msg.isBlank()) ? "does it have a texture yet?" : msg;
    }

    private static void fail(String msg) {
        LAST_PACK_ERROR.set(msg == null || msg.isBlank() ? "unknown packaging error." : msg);
    }

    /**
     * Animated blocks can exceed the test worker's 5 MB cap. The original GIF/WebP source is useful
     * for local re-rendering, but the downloaded block only needs texture.png + AnimData, so omit the
     * source and downscale the share texture only when the full animated grid still does not fit.
     */
    private static byte[] packAnimated(SlotData d, byte[] texture) throws Exception {
        byte[] zip = zipBlock(d, texture, null);
        if (fitsVault(zip)) return zip;

        int frames = Math.max(2, d.anim().frameCount());
        for (int cell : SHARE_CELLS) {
            byte[] downsized = rebuildAnimatedGrid(texture, frames, cell);
            if (downsized == null) continue;
            zip = zipBlock(d, downsized, null);
            if (fitsVault(zip)) return zip;
        }

        fail("animated share package is larger than the 5 MB vault limit, even after downscaling.");
        return null;
    }

    private static byte[] zipBlock(SlotData d, byte[] texture, byte[] source) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            putEntry(zip, JSON_ENTRY, GSON.toJson(SlotDataStore.toJsonObject(d)).getBytes(StandardCharsets.UTF_8));
            putEntry(zip, TEX_ENTRY, texture);
            if (source != null && source.length > 0) putEntry(zip, SRC_ENTRY, source);
            for (String face : TextureStore.FACES) {
                byte[] fp = TextureStore.loadFace(d.index(), face);
                if (fp != null && fp.length > 0) putEntry(zip, FACE_PREFIX + face + ".png", fp);
            }
        }
        return bos.toByteArray();
    }

    private static boolean fitsVault(byte[] zip) {
        return zip != null && zip.length > 0 && zip.length <= VAULT_MAX_BYTES;
    }

    /**
     * Rebuild an animated texture as a smaller grid. The frame count and timing stay in block.json;
     * ServerPackGenerator will regenerate the matching grid sidecar after import.
     */
    private static byte[] rebuildAnimatedGrid(byte[] texture, int frameCount, int targetCell) {
        if (texture == null || texture.length == 0 || frameCount <= 1) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(texture));
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            boolean vertical = (long) w * frameCount == h;
            int srcCols = vertical ? 1 : AnimationDecoder.gridCols(frameCount);
            int srcRows = (frameCount + srcCols - 1) / srcCols;
            int srcCell = Math.min(w / srcCols, h / srcRows);
            if (srcCell <= 0 || targetCell >= srcCell) return null;

            int outCols = AnimationDecoder.gridCols(frameCount);
            int outRows = (frameCount + outCols - 1) / outCols;
            BufferedImage out = new BufferedImage(outCols * targetCell, outRows * targetCell, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            hints(g);
            for (int i = 0; i < frameCount; i++) {
                int sx = (i % srcCols) * srcCell;
                int sy = (i / srcCols) * srcCell;
                if (sx + srcCell > w || sy + srcCell > h) {
                    g.dispose();
                    return null;
                }
                int dx = (i % outCols) * targetCell;
                int dy = (i / outCols) * targetCell;
                g.drawImage(img.getSubimage(sx, sy, srcCell, srcCell), dx, dy, targetCell, targetCell, null);
            }
            g.dispose();
            return toPng(out);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unpack a downloaded block ZIP and create the block if its id is free. Reports it skipped if
     * the id already exists (S1 — the S2 conflict GUI handles that case), or failed on a malformed
     * payload / no free slot. Caller must regenerate the pack afterwards. Never throws.
     */
    public static ImportResult unpack(byte[] zipBytes) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0) {
            failed.add("empty download");
            return new ImportResult(created, skipped, failed);
        }
        try {
            Map<String, byte[]> entries = readEntries(zipBytes);
            JsonObject o = readJson(entries, failed);
            if (o == null) return new ImportResult(created, skipped, failed);
            String id = o.get("customId").getAsString();
            if (id == null || id.isBlank()) { failed.add("blank id"); return new ImportResult(created, skipped, failed); }
            if (SlotManager.hasId(id)) { skipped.add(id); return new ImportResult(created, skipped, failed); }
            importBlock(o, id, entries, created, failed);
        } catch (Exception ex) {
            failed.add("unpack error (" + ex.getMessage() + ")");
        }
        return new ImportResult(created, skipped, failed);
    }

    /**
     * Import the downloaded block under a CHOSEN id, ignoring the id stored in block.json (Group 20 S2
     * conflict resolution). Override re-imports under the freed original id; Keep Both under a new typed
     * id; Rename Mine under the original id after the local block has been re-id'd out of the way. The
     * caller must have ensured {@code forcedId} is free — if it is somehow taken, the block is reported
     * failed and never overwritten. The caller regenerates the pack afterwards. Never throws.
     */
    public static ImportResult unpackAs(byte[] zipBytes, String forcedId) {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed  = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0) { failed.add("empty download"); return new ImportResult(created, skipped, failed); }
        if (forcedId == null || forcedId.isBlank()) { failed.add("blank id"); return new ImportResult(created, skipped, failed); }
        try {
            Map<String, byte[]> entries = readEntries(zipBytes);
            JsonObject o = readJson(entries, failed);
            if (o == null) return new ImportResult(created, skipped, failed);
            if (SlotManager.hasId(forcedId)) { failed.add(forcedId + " (already taken)"); return new ImportResult(created, skipped, failed); }
            importBlock(o, forcedId, entries, created, failed);
        } catch (Exception ex) {
            failed.add("unpack error (" + ex.getMessage() + ")");
        }
        return new ImportResult(created, skipped, failed);
    }

    /** Parse block.json from the entries; records a failure and returns null when missing / malformed. */
    private static JsonObject readJson(Map<String, byte[]> entries, List<String> failed) {
        byte[] jsonBytes = entries.get(JSON_ENTRY);
        if (jsonBytes == null) { failed.add("not a block share (no block.json)"); return null; }
        JsonObject o = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        if (o == null || !o.has("customId")) { failed.add("malformed block.json"); return null; }
        return o;
    }

    /** Allocate a fresh slot under {@code id}, apply every attribute, and write the textures. */
    private static void importBlock(JsonObject o, String id, Map<String, byte[]> entries,
                                    List<String> created, List<String> failed) {
        String name = o.has("displayName") ? o.get("displayName").getAsString() : id;
        SlotData d = SlotManager.create(id, name);
        if (d == null) { failed.add(id + " (no free slot)"); return; }
        int index = d.index();

        // Attributes (every field SlotDataStore writes — keys match toJsonObject).
        if (o.has("glow"))        SlotManager.setGlow(id, o.get("glow").getAsInt());
        if (o.has("hardness"))    SlotManager.setHardness(id, o.get("hardness").getAsFloat());
        if (o.has("sound"))       SlotManager.setSoundType(id, o.get("sound").getAsString());
        if (o.has("noCollision")) SlotManager.setNoCollision(id, o.get("noCollision").getAsBoolean());
        if (o.has("category"))    SlotManager.setCategory(id, o.get("category").getAsString());
        if (o.has("shape"))       SlotManager.setShape(id, o.get("shape").getAsString());
        if (o.has("anim") && o.get("anim").isJsonObject()) {
            AnimData anim = SlotDataStore.animFromJson(o.getAsJsonObject("anim"));
            if (anim.isAnimated()) SlotManager.setAnim(id, anim);
        }

        // Textures: base (grid for animated), then any source image + per-face overrides.
        TextureStore.save(index, entries.get(TEX_ENTRY));
        byte[] src = entries.get(SRC_ENTRY);
        if (src != null && src.length > 0) TextureStore.saveSource(index, src);
        for (String face : TextureStore.FACES) {
            byte[] fp = entries.get(FACE_PREFIX + face + ".png");
            if (fp != null && fp.length > 0) TextureStore.saveFace(index, face, fp);
        }

        created.add(id);
    }

    // ── S2 conflict screen support ────────────────────────────────────────────

    /**
     * Lightweight read of a downloaded block ZIP for the S2 conflict screen — its id, display name, a
     * compact stats string, frame count, and a small preview PNG — WITHOUT importing it or touching
     * SlotManager. The preview is a vertical 64px frame strip the client slices back into cube grids.
     */
    public record Peek(String id, String name, String stats, int frameCount, byte[] preview) {}

    /** Read incoming block metadata + build a tiny preview. Returns null on any failure. Never throws. */
    public static Peek peek(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) return null;
        try {
            Map<String, byte[]> entries = readEntries(zipBytes);
            byte[] jsonBytes = entries.get(JSON_ENTRY);
            if (jsonBytes == null) return null;
            JsonObject o = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (o == null || !o.has("customId")) return null;
            String id = o.get("customId").getAsString();
            String name = o.has("displayName") ? o.get("displayName").getAsString() : id;
            int frames = frameCountOf(o);
            return new Peek(id, name, statsOf(o), frames, buildPreview(entries.get(TEX_ENTRY), frames));
        } catch (Exception e) {
            return null;
        }
    }

    /** Compact "key=val;…" stats from a live slot (the conflict screen's "yours" side). Carries the slot index. */
    public static String statsOf(SlotData d) {
        return stats(d.glow(), d.hardness(), d.soundType(), !d.noCollision(),
                d.category() == null ? "" : d.category(), d.shape(),
                d.isAnimated() ? d.anim().frameCount() : 1, d.index());
    }

    /** Compact "key=val;…" stats from a downloaded block.json (the "incoming" side). Mirror of {@link #statsOf(SlotData)}. */
    public static String statsOf(JsonObject o) {
        int glow = o.has("glow") ? o.get("glow").getAsInt() : 0;
        float hard = o.has("hardness") ? o.get("hardness").getAsFloat() : SlotData.DEFAULT_HARDNESS;
        String sound = o.has("sound") ? o.get("sound").getAsString() : SlotData.DEFAULT_SOUND;
        boolean solid = !(o.has("noCollision") && o.get("noCollision").getAsBoolean());
        String cat = o.has("category") ? o.get("category").getAsString() : "";
        String shape = o.has("shape") ? o.get("shape").getAsString() : SlotData.DEFAULT_SHAPE;
        return stats(glow, hard, sound, solid, cat, shape, frameCountOf(o), -1);
    }

    private static String stats(int glow, float hard, String sound, boolean solid,
                                String cat, String shape, int frames, int idx) {
        String h = (hard == Math.rint(hard)) ? String.valueOf((int) hard) : String.valueOf(hard);
        StringBuilder sb = new StringBuilder()
                .append("glow=").append(glow)
                .append(";hard=").append(h)
                .append(";sound=").append(sound)
                .append(";solid=").append(solid)
                .append(";cat=").append(cat == null ? "" : cat)
                .append(";shape=").append(shape)
                .append(";frames=").append(Math.max(1, frames));
        if (idx >= 0) sb.append(";idx=").append(idx);
        return sb.toString();
    }

    private static int frameCountOf(JsonObject o) {
        if (o.has("anim") && o.get("anim").isJsonObject()) {
            JsonObject a = o.getAsJsonObject("anim");
            if (a.has("frameCount")) return Math.max(1, a.get("frameCount").getAsInt());
        }
        return 1;
    }

    private static final int PREVIEW_CELL = 64; // per-frame px in the shipped preview (downsamples to GRID anyway)

    /**
     * Downsample the baked texture into a small vertical preview strip (PREVIEW_CELL wide, ·frameCount
     * tall) so the conflict screen can show the incoming block live without shipping a full-res strip
     * over a single packet. Slices the source either as a packed cell-grid (current format) or a legacy
     * vertical strip — the same auto-detect the client uses to read it back. Null on any failure.
     */
    private static byte[] buildPreview(byte[] tex, int frameCount) {
        if (tex == null || tex.length == 0) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(tex));
            if (img == null) return null;
            int n = Math.max(1, frameCount);
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0) return null;
            if (n <= 1) return toPng(square(img, PREVIEW_CELL));
            boolean vertical = (long) w * n == h;
            int cols = vertical ? 1 : AnimationDecoder.gridCols(n);
            int cell = w / cols;
            if (cell <= 0) return null;
            BufferedImage strip = new BufferedImage(PREVIEW_CELL, PREVIEW_CELL * n, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = strip.createGraphics();
            hints(g);
            for (int i = 0; i < n; i++) {
                int sx = (i % cols) * cell, sy = (i / cols) * cell;
                if (sx + cell > w || sy + cell > h) break;
                g.drawImage(img.getSubimage(sx, sy, cell, cell), 0, i * PREVIEW_CELL, PREVIEW_CELL, PREVIEW_CELL, null);
            }
            g.dispose();
            return toPng(strip);
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage square(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        hints(g);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static byte[] toPng(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    /** Read every ZIP entry into memory, keyed by its bare file name (zip-slip safe). */
    private static Map<String, byte[]> readEntries(byte[] zipBytes) throws Exception {
        Map<String, byte[]> out = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = java.nio.file.Path.of(e.getName()).getFileName().toString().toLowerCase(Locale.ROOT);
                out.put(name, zin.readAllBytes());
                zin.closeEntry();
            }
        }
        return out;
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
