/**
 * StudioEditLoad.java — Block Creation Studio edit-mode loader. CLIENT-SIDE ONLY.
 *
 * Split out of {@link BlockCreationStudioScreen} (§9.3 500-line gate) so the screen keeps the
 * view/nav/lifecycle and this owns parsing an EXISTING block's state into the {@link StudioState}:
 *   - {@link #apply}      — parse the "key=value;…" attrs string (shape/glow/.../category + animation)
 *                           that CreationStudioBridge.editAttrs builds, writing it into the state.
 *   - {@link #loadFrames} — read the block's frame grids back from the active resource pack on a daemon
 *                           thread, so the preview plays without re-downloading.
 *
 * Behaviour is identical to the old in-screen applyEditAttrs/parseAnim/loadEditFrames — only relocated.
 *
 * Depends on: StudioState, AnimData, StudioTextureLoader. Called by: BlockCreationStudioScreen (edit ctor).
 */
package com.customblocks.client.gui;

import com.customblocks.client.gui.studio.StudioState;
import com.customblocks.core.AnimData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class StudioEditLoad {

    private StudioEditLoad() {} // static-only

    /** Parse the edit-load attrs string into the StudioState (shape/glow/.../category + animation numbers). */
    public static void apply(StudioState st, String attrs) {
        Map<String, String> a = new HashMap<>();
        if (attrs != null) for (String pair : attrs.split(";")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) a.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        st.shape = a.getOrDefault("shape", "full");
        try { st.glow = Integer.parseInt(a.getOrDefault("glow", "0")); } catch (NumberFormatException ignored) {}
        String hardness = a.get("hardness");
        if (hardness != null && !hardness.isBlank()) {
            try { st.hardness = Float.parseFloat(hardness); st.hardnessSet = true; } catch (NumberFormatException ignored) {}
        }
        st.sound = a.getOrDefault("sound", "stone");
        st.passable = "1".equals(a.get("passable"));
        st.category = a.getOrDefault("category", "");
        String color = a.getOrDefault("color", "none");
        if (!color.equals("none")) try { st.pickBg(Integer.parseInt(color)); } catch (NumberFormatException ignored) {}
        st.anim = parseAnim(a.get("anim"), a.get("animtimes"));
    }

    /** Rebuild an AnimData from the edit-load CSV "frameCount,uniformTicks,loop,interp,trimS,trimE" + times. */
    private static AnimData parseAnim(String csv, String timesCsv) {
        if (csv == null || csv.isBlank()) return AnimData.NONE;
        String[] p = csv.split(",");
        if (p.length < 6) return AnimData.NONE;
        try {
            int fc = Integer.parseInt(p[0]), ut = Integer.parseInt(p[1]);
            int ts = Integer.parseInt(p[4]), te = Integer.parseInt(p[5]);
            List<Integer> times = new ArrayList<>();
            if (timesCsv != null && !timesCsv.isBlank())
                for (String t : timesCsv.split("_")) try { times.add(Integer.parseInt(t)); } catch (NumberFormatException ignored) {}
            return new AnimData(fc, ut, p[2], "1".equals(p[3]), ts, te, false, times);
        } catch (NumberFormatException e) {
            return AnimData.NONE;
        }
    }

    /** Read the block's frame grids back from the resource pack (off-thread) so the preview plays. */
    public static void loadFrames(StudioState st) {
        if (st.editIndex < 0) return;
        int fc = st.anim != null ? st.anim.frameCount() : 1;
        st.animLoading = st.isAnimated();
        Thread t = new Thread(() -> {
            int[][] frames = StudioTextureLoader.loadFromPack(st.editIndex, Math.max(1, fc));
            if (frames != null && frames.length > 0) {
                st.grid = frames[0];
                st.textureLoaded = true;
                st.loadState = "ok";
                if (st.isAnimated() && frames.length > 1) st.frames = frames;
            }
            st.animLoading = false;
        }, "CustomBlocks-StudioEditLoad");
        t.setDaemon(true);
        t.start();
    }
}
