/**
 * StudioSections.java — Block Creation Studio section renderer + picker hit-testing. CLIENT-SIDE ONLY.
 *
 * Split out of {@link BlockCreationStudioScreen} (which was at the 500-line gate, §9.3) so the screen
 * keeps the view/preview/nav/lifecycle and this class owns the CONTENT of each left-hand section: it
 * draws the custom pickers (palette swatches, shape chips, hardness chips, the Category panel and the
 * Group 14 Animation tab) and answers the matching clicks. Behaviour is identical to the old in-screen
 * drawSection/hitPicker — only relocated.
 *
 * Stateful like the other studio panels: the hit-rects are computed in {@link #render} and read in
 * {@link #mouseClicked}. It owns the Category + Animation sub-panels.
 *
 * Depends on: DrawContext / TextRenderer / TextFieldWidget, BlockShapes, StudioState,
 *             StudioCategoryPanel, StudioAnimPanel. Called by: BlockCreationStudioScreen.
 */
package com.customblocks.client.gui;

import com.customblocks.block.BlockShapes;
import com.customblocks.client.gui.BlockCreationStudioScreen.Section;
import com.customblocks.client.gui.studio.StudioState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class StudioSections {

    private static final int GOLD = 0xFF_FF_AA_00;

    private static final int[] PALETTE = {0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFFFFFFF, 0xFF212121};
    private static final String[] HARD_LABEL = {"Soft", "Wood", "Stone", "Iron", "Hard"};
    private static final float[]  HARD_VALUE = {0.5f, 2.0f, 3.0f, 5.0f, 10.0f};

    private final StudioCategoryPanel catPanel = new StudioCategoryPanel();
    private final StudioAnimPanel animPanel = new StudioAnimPanel();
    private final StudioAiPanel aiPanel = new StudioAiPanel();

    private int[][] shapeRects, swatchRects, hardRects; // hit-test rects for the custom pickers
    private String hoverHint = "";

    /** Reset the Category sub-panel's transient state when the Category tab is (re)entered. */
    public void resetCategory() { catPanel.reset(); }

    /** Regenerate (↻) on the AI tab — roll a different look for the current prompt. */
    public void aiRegenerate(StudioState st) { aiPanel.regenerate(st); }

    /** Enter in the category name field = Add (delegates to the Category panel). */
    public void categoryEnter(StudioState st, TextFieldWidget catField) { catPanel.onEnter(st, catField); }

    /** The live-preview frame the Animation tab wants shown right now (frame-swap playback). */
    public int[] currentFrame(StudioState st) { return animPanel.currentFrame(st); }

    /**
     * Draw the active section's labels + custom pickers (text fields/buttons are drawn by the screen's
     * super.render). Returns the one-line hover hint for the control under the mouse ("" = none).
     */
    public String render(Section section, DrawContext ctx, TextRenderer tr, int x, int y,
                         StudioState st, TextFieldWidget catField, int mx, int my, String navLabel) {
        shapeRects = swatchRects = hardRects = null;
        hoverHint = "";
        switch (section) {
            case IDENTITY -> {
                ctx.drawTextWithShadow(tr, Text.literal("§7Block ID §8(required)"), x, y, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, Text.literal("§7Display Name §8(required)"), x, y + 34, 0xFFFFFFFF);
            }
            case TEXTURE -> renderTexture(ctx, tr, x, y, st, mx, my);
            case ANIMATION -> animPanel.render(ctx, tr, x, y, st, mx, my);
            case SHAPE -> renderShape(ctx, tr, x, y, st, mx, my);
            case ATTRIBUTES -> renderAttributes(ctx, tr, x, y, st, mx, my);
            case AI -> aiPanel.render(ctx, tr, x, y, st, mx, my);
            case CATEGORY -> catPanel.render(ctx, tr, x, y, st, catField, mx, my);
            default -> {
                ctx.drawTextWithShadow(tr, Text.literal("§e" + navLabel + " — coming soon"), x, y, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, Text.literal("§8Needs new block data; its own session."), x, y + 14, 0xFFFFFFFF);
            }
        }
        return hoverHint;
    }

    private void renderTexture(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal("§7Texture URL §8(optional)"), x, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7Background colour §8(fills behind the image):"), x, y + 54, 0xFFFFFFFF);
        // PALETTE swatches + one "✖" clear swatch at the end (index == PALETTE.length).
        swatchRects = new int[PALETTE.length + 1][4];
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = x + i * 22, sy = y + 66;
            swatchRects[i] = new int[]{sx, sy, 18, 18};
            boolean sel = st.hasBg && st.bgArgb == (0xFF000000 | (PALETTE[i] & 0xFFFFFF));
            boolean hov = mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18;
            ctx.fill(sx - 1, sy - 1, sx + 19, sy + 19, sel ? 0xFFFFFFFF : (hov ? 0xFFBBBBBB : 0xFF000000));
            ctx.fill(sx, sy, sx + 18, sy + 18, PALETTE[i]);
        }
        int cxs = x + PALETTE.length * 22, cys = y + 66; // clear swatch
        swatchRects[PALETTE.length] = new int[]{cxs, cys, 18, 18};
        boolean chov = mx >= cxs && mx < cxs + 18 && my >= cys && my < cys + 18;
        ctx.fill(cxs - 1, cys - 1, cxs + 19, cys + 19, chov ? 0xFFBBBBBB : 0xFF000000);
        ctx.fill(cxs, cys, cxs + 18, cys + 18, 0xFF2A2A2A);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(st.hasBg ? "§c✖" : "§8✖"), cxs + 9, cys + 5, 0xFFFFFFFF);
    }

    private void renderShape(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal("§7Shape"), x, y, 0xFFFFFFFF);
        String[] shapes = BlockShapes.names();
        shapeRects = new int[shapes.length][4];
        for (int i = 0; i < shapes.length; i++) {
            int col = i % 2, row = i / 2;
            int sx = x + col * 104, sy = y + 14 + row * 20;
            shapeRects[i] = new int[]{sx, sy, 100, 18};
            boolean sel = shapes[i].equals(st.shape);
            boolean hov = mx >= sx && mx < sx + 100 && my >= sy && my < sy + 18;
            ctx.fill(sx - 1, sy - 1, sx + 101, sy + 19, sel ? GOLD : (hov ? 0xFFBBBBBB : 0xFF000000));
            ctx.fill(sx, sy, sx + 100, sy + 18, sel ? 0xFF3A2E00 : 0xFF1A1A1A);
            ctx.drawTextWithShadow(tr, Text.literal((sel ? "§e" : "§f") + shapes[i]), sx + 4, sy + 5, 0xFFFFFFFF);
        }
    }

    private void renderAttributes(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        hint(mx, my, x, y, 40, "Glow", "Light the block emits (0 = none, 15 = torch-bright).");
        hint(mx, my, x, y + 38, 60, "Hardness", "How long the block takes to break.");
        hint(mx, my, x, y + 74, 40, "Sound", "Footstep / break / place sound family.");
        hint(mx, my, x, y + 100, 60, "Collision", "Solid = blocks movement · Passable = walk through.");
        ctx.drawTextWithShadow(tr, Text.literal("§7Glow"), x, y, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + st.glow), x + 50, y + 19, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7Hardness"), x, y + 38, 0xFFFFFFFF);
        hardRects = new int[HARD_LABEL.length][4];
        for (int i = 0; i < HARD_LABEL.length; i++) {
            int sx = x + i * 42, sy = y + 50;
            hardRects[i] = new int[]{sx, sy, 40, 18};
            boolean sel = st.hardnessSet && st.hardness == HARD_VALUE[i];
            ctx.fill(sx - 1, sy - 1, sx + 41, sy + 19, sel ? GOLD : 0xFF000000);
            ctx.fill(sx, sy, sx + 40, sy + 18, sel ? 0xFF3A2E00 : 0xFF1A1A1A);
            ctx.drawCenteredTextWithShadow(tr, Text.literal((sel ? "§e" : "§f") + HARD_LABEL[i]), sx + 20, sy + 5, 0xFFFFFFFF);
        }
        ctx.drawTextWithShadow(tr, Text.literal("§7Sound"), x, y + 74, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("§f" + st.sound), x + 80, y + 89, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal("§7Collision"), x, y + 100, 0xFFFFFFFF);
    }

    /** Hit-test the custom-drawn pickers for the active section. */
    public boolean mouseClicked(Section section, double mx, double my, StudioState st, TextFieldWidget catField) {
        if (section == Section.SHAPE && shapeRects != null) {
            String[] shapes = BlockShapes.names();
            for (int i = 0; i < shapeRects.length; i++)
                if (in(shapeRects[i], mx, my)) { st.shape = shapes[i]; return true; }
        }
        if (section == Section.TEXTURE && swatchRects != null) {
            for (int i = 0; i < swatchRects.length; i++)
                if (in(swatchRects[i], mx, my)) {
                    if (i == PALETTE.length) st.clearBg(); else st.pickBg(PALETTE[i]);
                    return true;
                }
        }
        if (section == Section.ATTRIBUTES && hardRects != null) {
            for (int i = 0; i < hardRects.length; i++)
                if (in(hardRects[i], mx, my)) { st.hardness = HARD_VALUE[i]; st.hardnessSet = true; return true; }
        }
        if (section == Section.ANIMATION) return animPanel.mouseClicked(mx, my, st);
        if (section == Section.CATEGORY) return catPanel.mouseClicked(mx, my, st, catField);
        return false;
    }

    /** One-line hover hint shown at the bottom of the panel for the labelled control under the mouse. */
    private void hint(int mx, int my, int x, int y, int w, String label, String text) {
        if (mx >= x && mx < x + w && my >= y - 2 && my < y + 10) hoverHint = "§e" + label + " §7— " + text;
    }

    private static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
