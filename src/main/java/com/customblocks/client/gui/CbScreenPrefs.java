/**
 * CbScreenPrefs.java — persisted client-side UI prefs for Group 27 screens. CLIENT-ONLY.
 *
 * G27.7 §A2: holds the player's chosen backdrop dim (0..255 black alpha, default ~60%) so screens read as
 * a solid dark panel, persisted across opens. G27.7 §A4: also holds each screen's action-bar frame state
 * (dock side + hidden) so the dockable/hideable bottom bar remembers where the player put it.
 *
 * Single shared instance, loaded lazily from config/customblocks/data/screen-prefs.json, saved atomically
 * (temp + ATOMIC_MOVE, NFR-13). Serialised whole-object via Gson; the old {"dimAlpha":N} files still load
 * (extra fields simply default).
 * Depends on: Gson.
 * Called by: the Group 27 cube screens (backdrop() + dim slider) and CbActionBar (bar() dock/hidden).
 */
package com.customblocks.client.gui;

import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class CbScreenPrefs {

    private static final Path FILE = Path.of("config/customblocks/data/screen-prefs.json");
    private static final Gson GSON = new Gson();
    private static CbScreenPrefs INSTANCE;

    /** Backdrop black alpha, 0 (clear) .. 255 (solid). Default ~60%. */
    public int dimAlpha = 0x99;

    /** Per-screen action-bar frame state (§A4). */
    public Map<String, Bar> bars = new LinkedHashMap<>();

    /** §D1: whether the eyedrop first-time intro popup has been dismissed (never shows again after). */
    public boolean eyedropIntroSeen = false;

    // ── §G27.8.B global Settings (gear popup) ─────────────────────────────────
    /** Solid dark backdrop instead of the live world. */
    public boolean hideWorld = false;
    /** Master CB UI sounds on/off + volume 0..100. */
    public boolean soundOn = true;
    public int soundVolume = 60;
    /** Default cube auto-spin speed applied when a cube screen opens (0 = still, max 2.5). */
    public double spinDefault = 0.45;
    /** Kill the subtle animations (save-flash pulse etc.). */
    public boolean reducedMotion = false;
    /** Ask "Discard changes?" when cancelling with unsaved changes. */
    public boolean confirmDiscard = true;

    /** Dock side + hidden state of one screen's action bar. dock: 0 = bottom, 1 = left, 2 = right. */
    public static final class Bar {
        public int dock = 0;
        public boolean hidden = false;
    }

    public static CbScreenPrefs get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    /** 0xAARRGGBB backdrop fill using the chosen dim over black (solid when Hide-world is on). */
    public int backdrop() { return hideWorld ? 0xFF000000 : clampAlpha() << 24; }

    public double dim01() { return clampAlpha() / 255.0; }

    /** Set from a 0..1 slider value and persist. */
    public void setDim01(double v) {
        dimAlpha = (int) Math.round(Math.max(0, Math.min(1, v)) * 255);
        save();
    }

    public Bar bar(String screenId) { return bars.computeIfAbsent(screenId, k -> new Bar()); }

    public void saveBar(String screenId, int dock, boolean hidden) {
        Bar b = bar(screenId);
        b.dock = dock; b.hidden = hidden;
        save();
    }

    public void markEyedropIntroSeen() { eyedropIntroSeen = true; save(); }

    /** Persist after a settings-popup change (one write per interaction end). */
    public void saveSettings() { save(); }

    /** §G27.8.B "Reset panel positions" — forget every screen's bar dock/hidden state. */
    public void resetBars() { bars.clear(); save(); }

    /** §G27.8.B "Reset settings to defaults" — settings only; bar positions are a separate reset. */
    public void resetSettings() {
        dimAlpha = 0x99; hideWorld = false; soundOn = true; soundVolume = 60;
        spinDefault = 0.45; reducedMotion = false; confirmDiscard = true;
        save();
    }

    private int clampAlpha() { return Math.max(0, Math.min(255, dimAlpha)); }

    private static CbScreenPrefs load() {
        try {
            if (Files.exists(FILE)) {
                CbScreenPrefs p = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), CbScreenPrefs.class);
                if (p != null) { if (p.bars == null) p.bars = new LinkedHashMap<>(); return p; }
            }
        } catch (Exception ignored) {}
        return new CbScreenPrefs();
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("screen-prefs.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {}
    }
}
