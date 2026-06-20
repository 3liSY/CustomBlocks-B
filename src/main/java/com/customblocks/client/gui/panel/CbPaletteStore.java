/**
 * CbPaletteStore.java — Group 27 §G27.7 §B persisted palette + panel layout. CLIENT-ONLY.
 *
 * Responsibility: the on-disk state behind the reusable floating colour panel (CbColorPanel): the
 * editable working swatches, a recents row, a pinned favourites row, named saved palettes you can
 * reuse on other blocks, and each screen's remembered panel position + collapsed state (§B1). One
 * shared instance, loaded lazily, saved atomically (temp + ATOMIC_MOVE, NFR-13) so a mid-save crash
 * cannot corrupt it. All colours are stored as 0xRRGGBB ints.
 *
 * Depends on: Gson.
 * Called by: client/gui/panel/CbColorPanel (read + mutate), the host screens via the panel.
 */
package com.customblocks.client.gui.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class CbPaletteStore {

    private static final Path FILE = Path.of("config/customblocks/data/palettes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CbPaletteStore INSTANCE;

    public static final int RECENT_MAX = 12;
    public static final int FAV_MAX     = 9;   // matches fast keys 1–9
    public static final int SWATCH_MAX  = 24;

    /** Per-screen remembered panel placement. */
    public static final class Layout {
        public int x = -1, y = -1;   // -1 = "not placed yet" → panel picks a sensible default
        public boolean collapsed = false;
    }

    // Serialized fields (Gson reads/writes these directly).
    public List<Integer> swatches  = defaultSwatches();
    public List<Integer> recents   = new ArrayList<>();
    public List<Integer> favorites = new ArrayList<>();
    public Map<String, List<Integer>> saved   = new LinkedHashMap<>();
    public Map<String, Layout>        layouts = new LinkedHashMap<>();

    public static CbPaletteStore get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    // ── Working swatches (the main grid) ───────────────────────────────────────
    public void addSwatch(int rgb) {
        int c = rgb & 0xFFFFFF;
        swatches.remove((Integer) c);
        swatches.add(c);
        while (swatches.size() > SWATCH_MAX) swatches.remove(0);
        save();
    }

    public void removeSwatch(int index) {
        if (index >= 0 && index < swatches.size()) { swatches.remove(index); save(); }
    }

    public void moveSwatch(int from, int to) {
        if (from < 0 || from >= swatches.size() || to < 0 || to >= swatches.size() || from == to) return;
        swatches.add(to, swatches.remove(from));
        save();
    }

    public void setSwatch(int index, int rgb) {
        if (index >= 0 && index < swatches.size()) { swatches.set(index, rgb & 0xFFFFFF); save(); }
    }

    // ── Recents (auto-filled with last picks) ───────────────────────────────────
    public void pushRecent(int rgb) {
        int c = rgb & 0xFFFFFF;
        recents.remove((Integer) c);
        recents.add(0, c);
        while (recents.size() > RECENT_MAX) recents.remove(recents.size() - 1);
        save();
    }

    // ── Favourites (pinned ⭐ row) ──────────────────────────────────────────────
    public boolean isFavorite(int rgb) { return favorites.contains(rgb & 0xFFFFFF); }

    public void toggleFavorite(int rgb) {
        int c = rgb & 0xFFFFFF;
        if (!favorites.remove((Integer) c)) {
            favorites.add(c);
            while (favorites.size() > FAV_MAX) favorites.remove(0);
        }
        save();
    }

    // ── Named saved palettes (reuse on other blocks) ────────────────────────────
    public void savePalette(String name) {
        if (name == null || name.isBlank()) return;
        saved.put(name.trim(), new ArrayList<>(swatches));
        save();
    }

    /** Replace the working swatches with a saved palette; returns false if the name is unknown. */
    public boolean loadPalette(String name) {
        List<Integer> p = saved.get(name);
        if (p == null) return false;
        swatches = new ArrayList<>(p);
        save();
        return true;
    }

    public void deletePalette(String name) { if (saved.remove(name) != null) save(); }

    public List<String> paletteNames() { return new ArrayList<>(saved.keySet()); }

    // ── Per-screen layout (§B1 remember position + collapsed) ───────────────────
    public Layout layout(String screenId) {
        return layouts.computeIfAbsent(screenId, k -> new Layout());
    }

    public void saveLayout(String screenId, int x, int y, boolean collapsed) {
        Layout l = layout(screenId);
        l.x = x; l.y = y; l.collapsed = collapsed;
        save();
    }

    // ── Persistence ─────────────────────────────────────────────────────────────
    private static List<Integer> defaultSwatches() {
        // Mirrors ArabicPreviewScreen / ColorStudioMenu so the panel opens with familiar colours.
        int[] def = {
                0x0A0A0A, 0xFFFFFF, 0xFF0000, 0x1E8C1E, 0xF0C814, 0x2E6FF2,
                0x21C0C0, 0x8E3FD0, 0xF060A8, 0xF08020, 0x808080, 0x8A5A2B,
        };
        List<Integer> out = new ArrayList<>(def.length);
        for (int c : def) out.add(c);
        return out;
    }

    private static CbPaletteStore load() {
        try {
            if (Files.exists(FILE)) {
                CbPaletteStore p = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), CbPaletteStore.class);
                if (p != null) { p.normalize(); return p; }
            }
        } catch (Exception ignored) {}
        return new CbPaletteStore();
    }

    /** Guard against a hand-edited or partial file leaving null collections. */
    private void normalize() {
        if (swatches  == null || swatches.isEmpty()) swatches = defaultSwatches();
        if (recents   == null) recents   = new ArrayList<>();
        if (favorites == null) favorites = new ArrayList<>();
        if (saved     == null) saved     = new LinkedHashMap<>();
        if (layouts   == null) layouts   = new LinkedHashMap<>();
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("palettes.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {}
    }
}
