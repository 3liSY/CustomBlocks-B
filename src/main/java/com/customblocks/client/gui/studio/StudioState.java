/**
 * StudioState.java — Group 27 §G27.6 (Block Creation Studio). CLIENT-SIDE ONLY.
 *
 * Plain holder for everything the studio is gathering for one new block: identity, texture source
 * (URL or a solid base colour), shape, attributes and category. The screen reads/writes these; on
 * "Create & Publish" the screen serialises the attributes via {@link #toAttrs()} into the
 * CreateStudioPayload, and the server applies them through the existing SlotManager setters.
 *
 * Only the fields that map to a real server rail live here (shape / glow / hardness / sound /
 * collision / category / colour). FX / Behaviour / Lore need new SlotData fields and are NOT here yet.
 *
 * Depends on: (none). Called by: BlockCreationStudioScreen.
 */
package com.customblocks.client.gui.studio;

import com.customblocks.core.AnimData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StudioState {

    // Identity
    public String id = "";
    public String name = "";

    // Texture source — an image URL and/or a background colour. The colour fills BEHIND the image's
    // transparent pixels (or, with no image, becomes the whole block). They are NOT mutually exclusive.
    public String url = "";
    public boolean hasBg = false;        // true once the player picks a background colour
    public int bgArgb = 0xFFCCCCCC;      // the chosen background colour (always treated opaque)

    // Shape (one of BlockShapes.names()); "full" = default cube.
    public String shape = "full";

    // Attributes (each maps to a SlotManager setter on the server).
    public int glow = 0;                 // 0..15; 0 = no glow
    public float hardness = 1.5f;        // only sent when hardnessSet
    public boolean hardnessSet = false;
    public String sound = "stone";       // one of SlotBlock.SOUND_TYPES
    public boolean passable = false;     // true = entities walk through

    // Organize
    public String category = "";

    // AI (Group 15) — the prompt typed in the AI tab; the field's listener keeps this live so the
    // StudioAiPanel can read it each frame for debounced generation. /cb ai <prompt> pre-fills it.
    public String aiPrompt = "";

    // ── Live preview texture (set by the screen's loader thread) ────────────────
    public volatile int[] grid;          // null until first texture/colour set
    public volatile String loadState = "none"; // none | loading | ok | fail | aigen | aifail
    public volatile boolean textureLoaded;

    // ── Animation (Group 14 Phase 2) ────────────────────────────────────────────
    // The loader thread decodes ALL frames into preview grids and the animation's plain numbers; the
    // Animation tab edits those numbers (AnimData is immutable — every change returns a new snapshot).
    public volatile int[][] frames;      // per-frame preview grids; null = not animated / not loaded yet
    public AnimData anim = AnimData.NONE;// client-side animation numbers (speed/loop/smoothing/trim)
    public volatile boolean animLoading; // edit mode: the existing block's strip is being read from the pack

    // ── Edit mode (Group 14 Phase 2 — open an existing block to change it) ───────
    public int editIndex = -1;           // >=0 when editing an existing block (its slot index)
    public boolean editMode;             // true = the studio is editing, not creating ("Save changes")
    public String editOrigId = "";       // the id the block had on open — the server lookup key, even after a rename
    public String baseline = "";         // signature captured right after edit-load, so dirty() = "changed"

    /** True once the player has entered/changed anything worth a discard-confirm. */
    public boolean dirty() {
        if (editMode) return !signature().equals(baseline); // editing: dirty == differs from what we loaded
        return !id.isBlank() || !name.isBlank() || !url.isBlank() || hasBg
                || !shape.equals("full") || glow != 0 || hardnessSet || passable
                || !category.isBlank();
    }

    /** A compact fingerprint of every editable value, used to detect unsaved edit-mode changes.
     *  Includes id (a rename) and url (a new image) so changing only those still counts as dirty. */
    public String signature() {
        return id + "|" + name + "|" + url + "|" + toAttrs() + "|" + animCsv();
    }

    /** Capture the current state as the "no unsaved changes" baseline (called after an edit-load). */
    public void markBaseline() {
        baseline = signature();
    }

    /** True when the loaded clip is a real multi-frame animation (numbers only — the preview frames may
     *  still be loading in edit mode; {@link #frames} gates only the live playback, not the controls). */
    public boolean isAnimated() {
        return anim != null && anim.isAnimated();
    }

    /** Edit-mode "Save changes" body: the standard attrs plus the animation knobs (when animated). */
    public String saveAttrs() {
        return isAnimated() ? toAttrs() + ";anim=" + animCsv() : toAttrs();
    }

    /** Pick a background colour — the image (if any) stays, the colour fills behind transparent pixels. */
    public void pickBg(int argb) {
        hasBg = true;
        bgArgb = 0xFF000000 | (argb & 0xFFFFFF);
        loadState = "ok";
    }

    /** Drop the background colour (back to the image alone, or grey when there's no image). */
    public void clearBg() {
        hasBg = false;
        if (!textureLoaded) loadState = "none";
    }

    /** The animation's plain numbers as a compact CSV — carried in payloads + the edit-mode signature. */
    public String animCsv() {
        if (anim == null || !anim.isAnimated()) return "";
        return anim.frameCount() + "," + anim.uniformTicks() + "," + anim.loopMode() + ","
                + (anim.interpolate() ? 1 : 0) + "," + anim.trimStart() + "," + anim.trimEnd();
    }

    /**
     * Encode the non-identity settings as a compact {@code key=value;…} string carried in the
     * payload's 4th field. The server (CreationStudioBridge) parses it. Order-independent; missing
     * keys fall back to defaults. hardness is left empty when the player never set it (keep default).
     */
    public String toAttrs() {
        return "shape=" + shape
                + ";glow=" + glow
                + ";hardness=" + (hardnessSet ? hardness : "")
                + ";sound=" + sound
                + ";passable=" + (passable ? 1 : 0)
                + ";category=" + category.trim()
                + ";color=" + (hasBg ? (0xFF000000 | (bgArgb & 0xFFFFFF)) : "none");
    }
}
