/**
 * IdReferenceRegistry.java — Group 07 §G07-4 (Library Health scan + reference-map audit).
 *
 * A LIVE, on-demand audit of the block library — NOT a persisted push-model registry. Every scan reads the
 * current truth (SlotManager + the id-keyed stores) and reports issues, so it can never drift out of sync and
 * needs no new save file, no per-subsystem write-site wiring, and no boot-time backfill. (A persisted push
 * registry would start empty on existing installs until each subsystem next wrote, so it would need a live
 * backfill scan anyway — this IS that scan, kept as the source of truth.)
 *
 * Buckets (§G07-4 locked — issue-count only, no severity tiers):
 *   • no-texture   — an assigned block with no baked texture (report-only; a texture needs a source image).
 *   • dangling-ref — an id-keyed reference (lock, category icon) pointing at a block that no longer exists.
 *   • duplicates   — two blocks whose ids collide case-insensitively (report-only; which to keep is a human call).
 *   • misconfigured — an invalid shape name, or a glow outside 0..15.
 *
 * Fix-all (one Execute, §G07-4) clears the dangling refs and repairs the misconfigured blocks; the SlotData
 * repairs record ONE undo batch so a single /cb undo reverts them. no-texture / duplicates are left for the
 * owner (not safely auto-fixable).
 *
 * Depends on: SlotData, SlotManager, LockManager, CategoryDisplayBlockManager, TextureStore, UndoManager,
 *             BlockShapes, ResourcePackServer.
 * Called by:  BulkNet (HEALTH_SCAN / HEALTH_FIX), BulkSnapshot (report JSON for the Health tab).
 */
package com.customblocks.core;

import com.customblocks.block.BlockShapes;
import com.customblocks.network.ResourcePackServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class IdReferenceRegistry {

    private IdReferenceRegistry() {} // static-only

    /** One health scan's findings (each list is a human-readable line; the client shows counts + samples). */
    public record Report(List<String> noTexture, List<String> dangling, List<String> duplicates, List<String> misconfig) {
        public int issueCount() { return noTexture.size() + dangling.size() + duplicates.size() + misconfig.size(); }
        public int fixableCount() { return dangling.size() + misconfig.size(); }
    }

    // ── Scan (pure read, no mutation) ─────────────────────────────────────────
    public static Report scan() {
        List<String> noTexture = new ArrayList<>();
        List<String> dangling = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        List<String> misconfig = new ArrayList<>();

        // Per-block: missing texture, invalid shape, out-of-range glow; plus a case-insensitive id-collision tally.
        Map<String, Integer> lowerCounts = new HashMap<>();
        for (SlotData d : SlotManager.assignedSlots()) {
            String id = d.customId();
            if (!TextureStore.has(d.index())) noTexture.add(id);
            if (!BlockShapes.isValid(d.shape())) misconfig.add(id + " — bad shape \"" + d.shape() + "\"");
            if (d.glow() < 0 || d.glow() > 15) misconfig.add(id + " — glow " + d.glow() + " (0..15)");
            lowerCounts.merge(id.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : lowerCounts.entrySet())
            if (e.getValue() > 1) duplicates.add(e.getKey() + " ×" + e.getValue());

        // Dangling refs: a lock or a category icon pointing at an id that no longer exists.
        for (String id : LockManager.list())
            if (!SlotManager.hasId(id)) dangling.add("lock → " + id);
        for (String cat : SlotManager.categories()) {
            String icon = CategoryDisplayBlockManager.get(cat);
            if (icon != null && !icon.isBlank() && !SlotManager.hasId(icon)) dangling.add("category \"" + cat + "\" icon → " + icon);
        }

        return new Report(noTexture, dangling, duplicates, misconfig);
    }

    public static String scanJson() { return toJson(scan()); }

    public static String toJson(Report r) {
        JsonObject o = new JsonObject();
        o.add("noTexture", arr(r.noTexture()));
        o.add("dangling", arr(r.dangling()));
        o.add("duplicates", arr(r.duplicates()));
        o.add("misconfig", arr(r.misconfig()));
        o.addProperty("issues", r.issueCount());
        o.addProperty("fixable", r.fixableCount());
        return o.toString();
    }

    private static JsonArray arr(List<String> xs) {
        JsonArray a = new JsonArray();
        for (String x : xs) a.add(x);
        return a;
    }

    // ── Fix-all (§G07-4 — one Execute, one undo for the SlotData repairs) ──────
    /** Clear dangling refs + repair misconfigured blocks. Returns how many issues were fixed. */
    public static int fixAll(ServerCommandSource src) {
        int fixed = 0;

        // Dangling refs → remove the stale pointer (nothing to undo — the target block is already gone).
        for (String id : new ArrayList<>(LockManager.list()))
            if (!SlotManager.hasId(id) && LockManager.unlock(id)) fixed++;
        for (String cat : new ArrayList<>(SlotManager.categories())) {
            String icon = CategoryDisplayBlockManager.get(cat);
            if (icon != null && !icon.isBlank() && !SlotManager.hasId(icon) && CategoryDisplayBlockManager.clear(cat)) fixed++;
        }

        // Misconfigured SlotData → repair as ONE undo batch (invalid shape → full; out-of-range glow → clamp).
        List<UndoManager.Op> children = new ArrayList<>();
        boolean shapeChanged = false;
        for (SlotData d : new ArrayList<>(SlotManager.assignedSlots())) {
            String id = d.customId();
            if (!BlockShapes.isValid(d.shape())) {
                SlotData before = SlotManager.getById(id);
                SlotData after = SlotManager.setShape(id, SlotData.DEFAULT_SHAPE);
                if (before != null && after != null) { children.add(new UndoManager.Op(UndoManager.Kind.SHAPE, before, after, null, "health-fix shape")); fixed++; shapeChanged = true; }
            } else if (d.glow() < 0 || d.glow() > 15) {
                SlotData before = SlotManager.getById(id);
                SlotData after = SlotManager.setGlow(id, Math.max(0, Math.min(15, d.glow())));
                if (before != null && after != null) { children.add(new UndoManager.Op(UndoManager.Kind.MODIFY, before, after, null, "health-fix glow")); fixed++; }
            }
        }
        if (!children.isEmpty() && src.getEntity() instanceof ServerPlayerEntity p)
            UndoManager.recordBatch(p.getUuid(), children, "health fix-all (" + children.size() + ")");
        if (shapeChanged) ResourcePackServer.updatePack(); // shapes changed models → one debounced rebuild

        return fixed;
    }
}
