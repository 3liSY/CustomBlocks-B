/**
 * GuessSettingsScreen.java — Group 30 (Guess Mode) §10 / G30-11 unified /cb guess screen. CLIENT-SIDE ONLY.
 *
 * The shared "Guess Settings" control panel — one tabbed screen. Persistent tab bar: Pose · Buzz · Sound ·
 * Look · Showcase. Two tabs are LIVE:
 *   • Pose (G30-4): six arm sliders (up/down · in/out · twist, per arm) + a live 3D player-dummy preview +
 *     presets/reset; drags preview locally, release/preset pushes the whole pose to the server (GuessPosePayload).
 *   • Showcase (G30-8b): two sliders (core spin · size) + a Glow toggle, with a live in-screen spinning
 *     preview of the "?" display; a change pushes the shared showcase tuning to the server
 *     (GuessShowcasePayload) so every placed Showcase in the world updates live too. (Outer-orbit + particles
 *     were removed with the outer layer — §S: S1/S7.)
 * Buzz / Sound / Look stay greyed "coming soon" placeholders. Every slider on both tabs supports inline number
 * entry (click a value → type it), scroll / ↑↓ nudge, and right-click-to-reset (CbGradSlider).
 *
 * Depends on: ClientGuessState (synced pose + showcase tuning + preview override), GuessPoseStore /
 *   GuessShowcaseStore (defaults + presets), GuessPosePayload / GuessShowcasePayload, CbGradSlider, CbButton,
 *   CbTheme, InventoryScreen.drawEntity.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=GUESS_SETTINGS).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientGuessState;
import com.customblocks.client.render.AnimClock;
import com.customblocks.client.render.GuessDisguise;
import com.customblocks.client.render.GuessShowcaseBER;
import com.customblocks.core.GuessPoseStore;
import com.customblocks.core.GuessShowcaseStore;
import com.customblocks.network.payloads.GuessPosePayload;
import com.customblocks.network.payloads.GuessShowcasePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class GuessSettingsScreen extends Screen {

    private static final int BAR_H = 30;
    private static final int SLIDER_X = 24, SLIDER_W = 150, SLIDER_TOP = 92, SLIDER_STEP = 28;

    private static final int POSE = 0, SHOWCASE = 4; // indices into TABS of the two LIVE tabs

    // Pose tab — six arm sliders (degrees): [0..2] right up/down·in/out·twist, [3..5] left.
    private final CbGradSlider[] poseSliders = new CbGradSlider[6];
    // Showcase tab — two tuning sliders: core spin, size. (The Glow toggle was scrapped — G30 §S: S6.)
    private final CbGradSlider[] showSliders = new CbGradSlider[2];

    private int tab = POSE;
    private int dragging = -1; // slider index being dragged, or -1

    private static final String[] TABS = {"Pose", "Buzz", "Sound", "Look", "Showcase"};
    private static final int TAB_X = 8, TAB_Y = BAR_H + 4, TAB_W = 62, TAB_GAP = 4;
    private String notice = "";  // transient "coming soon" line shown when a greyed tab is clicked
    private long noticeEnd;

    public GuessSettingsScreen() {
        super(Text.literal("Guess Settings"));
        String[] labels = {"Right arm — up/down", "Right arm — in/out", "Right arm — twist",
                           "Left arm — up/down",  "Left arm — in/out",  "Left arm — twist"};
        double[] deg = currentDegrees();
        for (int i = 0; i < 6; i++) {
            poseSliders[i] = new CbGradSlider(labels[i], -180, 180, deg[i], CbGradSlider.PLAIN);
            poseSliders[i].decimals = true; // angles are fine values (e.g. -28.6°)
        }
        showSliders[0] = new CbGradSlider("Core spin (°/s)", GuessShowcaseStore.SPEED_MIN, GuessShowcaseStore.SPEED_MAX, ClientGuessState.showcaseInner(), CbGradSlider.PLAIN);
        showSliders[1] = new CbGradSlider("Size",            GuessShowcaseStore.SIZE_MIN,  GuessShowcaseStore.SIZE_MAX,  ClientGuessState.showcaseSize(),  CbGradSlider.PLAIN);
        showSliders[1].decimals = true; // size is a fine value (e.g. 1.2×)
    }

    /** The current SAVED pose (from the synced state), in degrees, in slider order. */
    private static double[] currentDegrees() {
        return new double[]{
                Math.toDegrees(ClientGuessState.poseRightPitch()), Math.toDegrees(ClientGuessState.poseRightYaw()),
                Math.toDegrees(ClientGuessState.poseRightRoll()),  Math.toDegrees(ClientGuessState.poseLeftPitch()),
                Math.toDegrees(ClientGuessState.poseLeftYaw()),    Math.toDegrees(ClientGuessState.poseLeftRoll())};
    }

    /** The slider set for the active tab. */
    private CbGradSlider[] active() { return tab == SHOWCASE ? showSliders : poseSliders; }

    @Override
    protected void init() {
        for (int i = 0; i < poseSliders.length; i++) poseSliders[i].set(SLIDER_X, SLIDER_TOP + i * SLIDER_STEP, SLIDER_W);
        for (int i = 0; i < showSliders.length; i++) showSliders[i].set(SLIDER_X, SLIDER_TOP + i * SLIDER_STEP, SLIDER_W);

        // Persistent tab bar. Pose + Showcase are LIVE (click to switch); Buzz/Sound/Look are greyed, disabled
        // placeholders whose click/hover shows a "coming soon" hint (handled in mouseClicked/render).
        for (int i = 0; i < TABS.length; i++) {
            boolean live = isLive(i);
            int x = TAB_X + i * (TAB_W + TAB_GAP);
            final int idx = i;
            addDrawableChild(CbButton.tab(Text.literal(TABS[i]), x, TAB_Y, TAB_W, 16, i == tab, live,
                    live ? b -> switchTab(idx) : b -> {}));
        }

        int fy = height - 30, bw = 74;
        if (tab == SHOWCASE) {
            addDrawableChild(CbButton.ghost(Text.literal("Reset"), SLIDER_X, fy, bw, 20, b -> resetShowcase()));
        } else {
            addDrawableChild(CbButton.normal(Text.literal("Cradle"),  SLIDER_X,            fy, bw, 20, b -> applyPreset("cradle")));
            addDrawableChild(CbButton.normal(Text.literal("Present"), SLIDER_X + bw + 6,   fy, bw, 20, b -> applyPreset("present")));
            addDrawableChild(CbButton.normal(Text.literal("Low"),     SLIDER_X + (bw+6)*2, fy, bw, 20, b -> applyPreset("low")));
            addDrawableChild(CbButton.ghost(Text.literal("Reset"),    SLIDER_X + (bw+6)*3, fy, bw, 20, b -> resetPose()));
        }
        addDrawableChild(CbButton.primary(Text.literal("Done"), width - 90, fy, 74, 20, b -> close()));

        if (tab == POSE) pushPreview(); else ClientGuessState.clearPosePreview();
    }

    private static boolean isLive(int i) { return i == POSE || i == SHOWCASE; }

    private void switchTab(int i) {
        if (i == tab) return;
        commitOpenFields();
        tab = i;
        clearChildren();
        init();
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xE6000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta); // background + tab/footer buttons

        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Guess Settings — " + TABS[tab]), 8, 10, 0xFFFFFFFF);

        for (CbGradSlider s : active()) s.render(ctx, textRenderer, tab == SHOWCASE ? "" : "°");
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7drag · scroll or ↑↓ to nudge · click a value to type it · right-click a slider to reset"),
                SLIDER_X, SLIDER_TOP - 22, 0xFFFFFFFF);

        if (tab == SHOWCASE) drawShowcasePreview(ctx); else drawPreview(ctx);

        int hov = placeholderTabAt(mx, my);
        if (hov >= 0) drawTip(ctx, TABS[hov] + " — coming soon", mx, my);
        if (!notice.isEmpty() && System.currentTimeMillis() < noticeEnd)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), width / 2, height - 46, 0xFFFFFFFF);
    }

    /** The index of a GREYED placeholder tab under the cursor, or -1. Pose + Showcase are real live buttons. */
    private int placeholderTabAt(double mx, double my) {
        if (my < TAB_Y || my > TAB_Y + 16) return -1;
        for (int i = 0; i < TABS.length; i++) {
            if (isLive(i)) continue;
            int x = TAB_X + i * (TAB_W + TAB_GAP);
            if (mx >= x && mx <= x + TAB_W) return i;
        }
        return -1;
    }

    private void comingSoon(String tab) {
        notice = "§7" + tab + " — coming soon";
        noticeEnd = System.currentTimeMillis() + 2200;
    }

    private void drawTip(DrawContext ctx, String text, double mx, double my) {
        int w = textRenderer.getWidth(text), x = (int) mx + 8, y = (int) my - 14;
        ctx.fill(x - 3, y - 3, x + w + 3, y + 11, 0xF0000000);
        ctx.fill(x - 3, y - 3, x + w + 3, y - 2, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7" + text), x, y, 0xFFFFFFFF);
    }

    /** Right-side panel on the Pose tab: the live player-dummy holding whatever's in hand, posed live. */
    private void drawPreview(DrawContext ctx) {
        int px1 = (int) (width * 0.52f), py1 = BAR_H + 40, px2 = width - 28, py2 = height - 44;
        ctx.fill(px1 - 1, py1 - 1, px2 + 1, py2 + 1, 0xFF000000);
        ctx.fill(px1, py1, px2, py2, 0xFF141414);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Live preview"), px1 + 6, py1 + 4, 0xFFFFFFFF);

        LivingEntity dummy = client == null ? null : client.player;
        if (dummy == null) return;
        int size = Math.min((py2 - py1) / 2, 80);
        float cx = (px1 + px2) / 2f, cy = (py1 + py2) / 2f + size * 0.35f;
        InventoryScreen.drawEntity(ctx, px1 + 6, py1 + 14, px2 - 6, py2 - 6, size, 0.0f, cx, cy, dummy);
    }

    /**
     * Right-side panel on the Showcase tab: a live in-screen preview of the "?" display, spinning + sized
     * per the CURRENT slider values (updates as you drag), so you don't have to spawn one to tune it.
     * Mirrors the Pose tab's dummy. Renders the shared BER core cube into a GUI 3D transform (drawEntity-style:
     * z-offset, negative-z scale, gui depth lighting) and flushes with {@code ctx.draw()}.
     */
    private void drawShowcasePreview(DrawContext ctx) {
        int px1 = (int) (width * 0.52f), py1 = BAR_H + 40, px2 = width - 28, py2 = height - 44;
        ctx.fill(px1 - 1, py1 - 1, px2 + 1, py2 + 1, 0xFF000000);
        ctx.fill(px1, py1, px2, py2, 0xFF141414);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Live preview — the \"?\" display"), px1 + 6, py1 + 4, 0xFFFFFFFF);
        if (client == null) return;

        float inner = (float) showSliders[0].value;
        float size  = (float) showSliders[1].value;
        float yaw = ((AnimClock.nowMs() % 3_600_000L) / 1000f * inner) % 360f; // live turntable at the current speed
        Identifier tex = GuessDisguise.textureForLook(ClientGuessState.NO_LOOK);  // the bundled "?" placeholder

        int cx = (px1 + px2) / 2, cy = (py1 + py2) / 2 + 8;
        float scalePx = Math.min(px2 - px1, py2 - py1) * 0.6f; // fit the cube to the panel

        ctx.enableScissor(px1, py1 + 14, px2, py2);
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 50);
        ms.scale(scalePx, scalePx, -scalePx);                              // drawEntity-style: flip Z into GUI space
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18f));        // tilt down so the cube reads as 3D
        DiffuseLighting.enableGuiDepthLighting();
        VertexConsumerProvider.Immediate imm = ctx.getVertexConsumers();
        GuessShowcaseBER.renderCore(ms, imm, tex, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, size, yaw);
        ctx.draw();                                                        // flush the 3D cube
        ms.pop();
        ctx.disableScissor();

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8spawn one in-world with /cb guess showcase spawn"),
                px1 + 6, py2 - 12, 0xFFFFFFFF);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        CbGradSlider[] s = active();
        if (button == 0) {
            for (int i = 0; i < s.length; i++)
                if (s[i].hitChip(mx, my)) { beginEdit(i); return true; }
            commitOpenFields();
            int ph = placeholderTabAt(mx, my);
            if (ph >= 0) { comingSoon(TABS[ph]); return true; }
            for (int i = 0; i < s.length; i++)
                if (s[i].hit(mx, my)) { dragging = i; s[i].setFromX(mx); pushPreview(); return true; }
        } else if (button == 1) {
            for (int i = 0; i < s.length; i++)
                if (s[i].hit(mx, my) || s[i].hitChip(mx, my)) { resetAxis(i); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) { active()[dragging].setFromX(mx); pushPreview(); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0) { commit(); dragging = -1; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        CbGradSlider[] s = active();
        for (int i = 0; i < s.length; i++) {
            if (s[i].hit(mx, my) || s[i].hitChip(mx, my)) {
                s[i].nudge(v > 0 ? 1 : -1);
                pushPreview(); commit();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    // ── Keyboard (inline number entry) ──────────────────────────────────────────
    @Override
    public boolean charTyped(char chr, int modifiers) {
        int e = editingSlider();
        if (e >= 0 && active()[e].charTyped(chr)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        int e = editingSlider();
        if (e >= 0) {
            CbGradSlider sl = active()[e];
            if (key == 256) { sl.keyPressed(key); return true; } // ESC cancels the edit, doesn't close the screen
            boolean arrow = (key == 265 || key == 264);
            if (sl.keyPressed(key)) {
                pushPreview();
                if (arrow || !sl.editing) commit();
                return true;
            }
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    // ── Inline-edit helpers (operate on the active tab's sliders) ────────────────
    private int editingSlider() { CbGradSlider[] s = active(); for (int i = 0; i < s.length; i++) if (s[i].editing) return i; return -1; }

    private void beginEdit(int i) { commitOpenFields(); active()[i].beginEdit(); }

    private void commitOpenFields() {
        CbGradSlider[] s = active();
        boolean any = false;
        for (CbGradSlider sl : s) if (sl.editing) { sl.commitEdit(); any = true; }
        if (any) { pushPreview(); commit(); }
    }

    private void resetAxis(int i) {
        if (tab == SHOWCASE) active()[i].value = showcaseDefault(i);
        else { float[] d = GuessPoseStore.presetDegrees("cradle"); if (d != null) active()[i].value = d[i]; }
        active()[i].cancelEdit();
        pushPreview(); commit();
    }

    private static double showcaseDefault(int i) {
        return i == 0 ? GuessShowcaseStore.DEF_INNER : GuessShowcaseStore.DEF_SIZE;
    }

    // ── Apply / commit ──────────────────────────────────────────────────────────

    /** Pose tab only: push the working pose into the live-preview override so the dummy shows it now. */
    private void pushPreview() {
        if (tab != POSE || client == null || client.player == null) return;
        ClientGuessState.setPosePreview(client.player.getUuid(), rad(0), rad(1), rad(2), rad(3), rad(4), rad(5));
    }

    private float rad(int i) { return (float) Math.toRadians(poseSliders[i].value); }

    /** Push the active tab's values to the server (op-checked there) → save + sync to everyone. */
    private void commit() {
        if (tab == SHOWCASE) {
            ClientPlayNetworking.send(GuessShowcasePayload.of((float) showSliders[0].value, (float) showSliders[1].value));
        } else {
            ClientPlayNetworking.send(GuessPosePayload.of(rad(0), rad(1), rad(2), rad(3), rad(4), rad(5)));
        }
    }

    private void applyPreset(String name) {
        float[] d = GuessPoseStore.presetDegrees(name);
        if (d == null) return;
        for (int i = 0; i < 6; i++) poseSliders[i].value = d[i];
        pushPreview(); commit();
    }

    private void resetPose() {
        float[] d = GuessPoseStore.presetDegrees("cradle");
        if (d != null) for (int i = 0; i < 6; i++) poseSliders[i].value = d[i];
        pushPreview(); commit();
    }

    private void resetShowcase() {
        showSliders[0].value = GuessShowcaseStore.DEF_INNER;
        showSliders[1].value = GuessShowcaseStore.DEF_SIZE;
        commit();
    }

    @Override
    public void removed() {
        commitOpenFields();
        ClientGuessState.clearPosePreview();
        super.removed();
    }

    @Override
    public boolean shouldPause() { return false; }
}
