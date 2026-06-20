/**
 * HudSnap.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: Figma-style magnetic snapping for a dragged brick. Given the dragged
 * brick's screen rect plus every other brick's rect, it snaps the brick's left / centreX /
 * right to the nearest vertical candidate (screen edges with a gutter, screen centre, the
 * rule-of-thirds lines, and other bricks' left / centre / right) within a pixel threshold,
 * and the same for the Y axis. Returns the adjusted position plus the matched guide-line
 * coordinates so the editor can draw them in cyan. Pure math — no drawing, no MC types.
 *
 * Depends on: nothing.
 * Called by: HudEditorScreen (during drag); the editor draws the returned guides + Shift
 *            disables snapping entirely.
 */
package com.customblocks.client.hud;

import java.util.ArrayList;
import java.util.List;

public final class HudSnap {

    private HudSnap() {}

    /** Default snap pull distance in pixels. */
    public static final int THRESHOLD = 6;
    /** Margin kept from the screen edges when snapping to them. */
    public static final int GUTTER = 4;

    /** Snap outcome: adjusted top-left + the guide line(s) that engaged (Integer.MIN_VALUE = none). */
    public static final class Result {
        public final int x, y;
        public final int guideX, guideY;   // screen coords of the engaged vertical / horizontal guide
        public final boolean engaged;      // true if either axis snapped this call
        Result(int x, int y, int guideX, int guideY) {
            this.x = x; this.y = y; this.guideX = guideX; this.guideY = guideY;
            this.engaged = guideX != Integer.MIN_VALUE || guideY != Integer.MIN_VALUE;
        }
    }

    /**
     * Snap a dragged brick.
     *
     * @param x,y    proposed top-left of the dragged brick (screen px)
     * @param w,h    its size
     * @param screenW,screenH window size
     * @param others bounds {x,y,w,h} of every OTHER brick (already filtered to visible)
     */
    public static Result snap(int x, int y, int w, int h, int screenW, int screenH, List<int[]> others) {
        List<Integer> candX = new ArrayList<>();
        List<Integer> candY = new ArrayList<>();

        // Screen guides.
        candX.add(GUTTER);                 candX.add(screenW - GUTTER);   candX.add(screenW / 2);
        candX.add(screenW / 3);            candX.add(2 * screenW / 3);
        candY.add(GUTTER);                 candY.add(screenH - GUTTER);   candY.add(screenH / 2);
        candY.add(screenH / 3);            candY.add(2 * screenH / 3);

        // Other bricks' edges + centres.
        for (int[] b : others) {
            candX.add(b[0]); candX.add(b[0] + b[2] / 2); candX.add(b[0] + b[2]);
            candY.add(b[1]); candY.add(b[1] + b[3] / 2); candY.add(b[1] + b[3]);
        }

        // X axis: try the brick's left / centre / right against every candidate.
        int[] sx = bestSnap(new int[]{ x, x + w / 2, x + w }, candX);
        int newX = sx[0] != Integer.MIN_VALUE ? x + sx[0] : x;   // sx[0] = delta, sx[1] = guide line
        int guideX = sx[0] != Integer.MIN_VALUE ? sx[1] : Integer.MIN_VALUE;

        int[] sy = bestSnap(new int[]{ y, y + h / 2, y + h }, candY);
        int newY = sy[0] != Integer.MIN_VALUE ? y + sy[0] : y;
        int guideY = sy[0] != Integer.MIN_VALUE ? sy[1] : Integer.MIN_VALUE;

        return new Result(newX, newY, guideX, guideY);
    }

    /** Returns {delta, guideLine} for the best snap within THRESHOLD, or {MIN_VALUE, MIN_VALUE}. */
    private static int[] bestSnap(int[] points, List<Integer> candidates) {
        int bestDelta = Integer.MIN_VALUE, bestAbs = THRESHOLD + 1, guide = Integer.MIN_VALUE;
        for (int p : points) {
            for (int cand : candidates) {
                int d = cand - p;
                int ad = Math.abs(d);
                if (ad < bestAbs) { bestAbs = ad; bestDelta = d; guide = cand; }
            }
        }
        return bestAbs <= THRESHOLD ? new int[]{ bestDelta, guide } : new int[]{ Integer.MIN_VALUE, Integer.MIN_VALUE };
    }
}
