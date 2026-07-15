/**
 * RecolorSliderScreen.java — Group 10 live recolour; Group 27 §G27.2 unified frame + 3D cube. CLIENT-ONLY.
 *
 * Drag Hue / Saturation / Lightness and watch a live 3D drag-to-rotate cube of the block's texture
 * recolour in real time, then Apply to commit. The cube is the shared {@link PreviewCube}; the HSL
 * shift is applied per cell live (same ColorMath the server bakes with). Apply sends RecolorApplyPayload
 * — the SERVER bakes the real texture; the client only previews (CLAUDE.md §5.8).
 *
 * G27.2 adds: dimmable backdrop, red+black title bar + hints (locked palette 2026-07-04), [?] help,
 * sliders in a right panel, the movable/dockable CbActionBar (§A4)
 * [Undo][Redo][Rand]···[Apply]···[Copy][Reset][Cancel], session undo/redo of slider moves,
 * cancel-confirm. Called by CustomBlocksClient (OpenGuiPayload mode=RECOLOR_SLIDER).
 */
package com.customblocks.client.gui;

import com.customblocks.image.ColorMath;
import com.customblocks.network.payloads.GuiBackPayload;
import com.customblocks.network.payloads.RecolorApplyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class RecolorSliderScreen extends Screen {

    private static final int BAR_BG = CbTheme.BAR_BG, GOLD = CbTheme.ACCENT, BAR_H = 42;
    private static final double DEF_YAW = 28, DEF_PITCH = 16, DEF_SPIN = 0.45;
    private static final int DEF_HALF = 62, HALF_MIN = 36, HALF_MAX = 120, ZOOM_STEP = 8;
    private static final double SPIN_MAX = 2.5, SPIN_STEP = 0.15;

    private final String id, texUrl;
    private volatile int[] grid;     // PreviewCube.GRID² packed 0xAARRGGBB; null = not loaded
    private volatile boolean failed;
    private final PreviewCube cube = new PreviewCube(); // owns the baked atlas; disposed in removed()

    private final CbGradSlider hue   = new CbGradSlider("Hue",        -180, 180,   0, CbGradSlider.HUE);
    private final CbGradSlider sat   = new CbGradSlider("Saturation",    0, 200, 100, CbGradSlider.SAT);
    private final CbGradSlider light = new CbGradSlider("Lightness",     0, 200, 100, CbGradSlider.LIGHT);
    private final RecolorToneTools tone = new RecolorToneTools(this::pushUndo); // §C3 temperature/contrast/curve/filters
    private CbGradSlider dragSlider;

    // Camera + frame state.
    private double yaw = DEF_YAW, pitch = DEF_PITCH, spinSpeed = CbScreenPrefs.get().spinDefault;
    private boolean spinning = true, cubeDragging, dragged;
    private int half = DEF_HALF;
    private boolean confirmingCancel;
    private com.customblocks.client.gui.panel.CbActionBar bar; // §A4 movable/dockable action bar
    private final CbSettingsOverlay settings = new CbSettingsOverlay(); // §G27.8.B ⚙ Settings
    private final java.util.Deque<double[]> undo = new java.util.ArrayDeque<>(); // {hue,sat,light}
    private final java.util.Deque<double[]> redo = new java.util.ArrayDeque<>();
    private final CbHelpOverlay help = new CbHelpOverlay("Live Recolour", java.util.List.of(
            new CbHelpOverlay.Group("VIEW", java.util.List.of(
                    new CbHelpOverlay.Row("Drag", "Rotate the cube"),
                    new CbHelpOverlay.Row("Scroll", "Spin speed"),
                    new CbHelpOverlay.Row("Shift+Scroll", "Zoom"),
                    new CbHelpOverlay.Row("R", "Reset view"))),
            new CbHelpOverlay.Group("EDIT", java.util.List.of(
                    new CbHelpOverlay.Row("Ctrl+Z / Y", "Undo / Redo"),
                    new CbHelpOverlay.Row("Ctrl+C / V", "Copy / Paste H/S/L"),
                    new CbHelpOverlay.Row("Ctrl+R", "Randomise sliders"),
                    new CbHelpOverlay.Row("Enter", "Apply recolour")))));

    public RecolorSliderScreen(String id, String texUrl) {
        super(Text.literal("Live Recolour"));
        this.id = id;
        this.texUrl = texUrl;
    }

    @Override
    protected void init() {
        if (confirmingCancel) { addCancelButtons(); return; }
        layoutSliders();
        // §A4 movable/dockable action bar (replaces the old fixed full-width button strip).
        if (bar == null) bar = new com.customblocks.client.gui.panel.CbActionBar("recolor", java.util.List.of(
                new com.customblocks.client.gui.panel.CbActionBar.Item("Undo", this::undo, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Redo", this::redo, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Rand", this::randomize, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Apply", this::apply, true),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Copy", this::copy, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Reset", this::reset, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Cancel", this::close, false)));
        bar.init(width, height);
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> help.toggle()).dimensions(width - 24, 10, 16, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("⚙"), b -> settings.toggle()).dimensions(width - 44, 10, 16, 16).build()); // §G27.8.B
        // §G27.8.A quick view angles
        addDrawableChild(ButtonWidget.builder(Text.literal("Front"), b -> { yaw = 0; pitch = 0; }).dimensions(8, BAR_H + 6, 38, 14).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Iso"), b -> { yaw = DEF_YAW; pitch = DEF_PITCH; }).dimensions(48, BAR_H + 6, 30, 14).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Top"), b -> { yaw = 45; pitch = 85; }).dimensions(80, BAR_H + 6, 30, 14).build());
        if (grid == null && !failed) fetchAsync();
    }

    private void layoutSliders() {
        int x = width / 2 + 24, w = Math.min(170, width / 2 - 70), y = 60;
        hue.set(x, y, w);
        sat.set(x, y + 28, w);
        light.set(x, y + 56, w);
        tone.layout(x, light.y + 26, w); // §C3 tone tools stacked under H/S/L
    }

    private void fetchAsync() {
        Thread t = new Thread(() -> {
            // §F3: prefer the block's already-baked local pack texture (no network, never 403s).
            int[] local = LocalTexturePreview.load(id);
            if (local != null) { grid = local; return; }
            // Fallback: robust URL fetch (browser UA + Tenor/Giphy resolve) for blocks not baked locally.
            StudioTextureLoader.Result r = StudioTextureLoader.load(texUrl);
            if (r == null || r.frames() == null || r.frames().length == 0) { failed = true; return; }
            grid = r.frames()[0];
        }, "CustomBlocks-RecolorFetch");
        t.setDaemon(true);
        t.start();
    }

    private void apply() {
        if (bar != null) bar.flashPrimary(); // lime "saved" pulse (Group 27 save feedback)
        ClientPlayNetworking.send(new RecolorApplyPayload(
                id, (float) hue.value, (float) (sat.value / 100.0), (float) (light.value / 100.0),
                tone.temp(), tone.contrast(), tone.shadow(), tone.highlight(), tone.filter()));
        CbToast.success("Recolour applied to '" + id + "'"); // §G27.13 — toast, not chat
        ClientPlayNetworking.send(new GuiBackPayload());
        super.close();
    }

    private boolean isDirty() {
        return hue.value != 0 || sat.value != 100 || light.value != 100 || !tone.isIdentity();
    }

    /** Cancel / Esc — confirm if sliders moved, else ask the server to reopen the menu. */
    @Override
    public void close() {
        if (isDirty() && CbScreenPrefs.get().confirmDiscard && !confirmingCancel) { // §G27.8.B toggleable
            confirmingCancel = true; clearChildren(); init(); return;
        }
        ClientPlayNetworking.send(new GuiBackPayload());
        super.close();
    }

    private void addCancelButtons() {
        int cx = width / 2, cy = height / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("§cYes, discard"), b -> {
            ClientPlayNetworking.send(new GuiBackPayload()); confirmingCancel = false; super.close();
        }).dimensions(cx - 96, cy + 2, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Keep editing"), b -> {
            confirmingCancel = false; clearChildren(); init();
        }).dimensions(cx + 8, cy + 2, 88, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* world stays visible */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, CbScreenPrefs.get().backdrop()); // §A2 persisted dim (world visible)

        // 3D cube (left of the slider panel).
        int cubeCx = width / 2 - 80, cubeCy = height / 2 - 4;
        if (grid == null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(failed ? "§7(preview unavailable — Apply still works)" : "§8loading preview…"),
                    cubeCx, cubeCy, 0xFFFFFFFF);
        } else {
            double dh = hue.value, ds = sat.value / 100.0, dl = light.value / 100.0;
            // version bumps whenever the HSL or tone output changes → the atlas re-bakes (else it just redraws).
            long version = (Math.round(dh * 100) * 1_000_003L) + (Math.round(ds * 10000) * 1009L)
                    + Math.round(dl * 10000) + tone.version() * 1_000_000_007L;
            cube.render(ctx, grid, cubeCx, cubeCy, half, yaw, pitch,
                    argb -> (argb & 0xFF000000) | (tone.transformRgb(ColorMath.hslShiftRgb(argb & 0xFFFFFF, dh, ds, dl)) & 0xFFFFFF),
                    version);
        }

        // Sliders + tone tools (right panel).
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7HSL"), hue.x, hue.y - 26, 0xFFFFFFFF);
        hue.render(ctx, textRenderer, "°");
        sat.render(ctx, textRenderer, "%");
        light.render(ctx, textRenderer, "%");
        tone.render(ctx, textRenderer, mx, my);
        drawStatus(ctx);

        // Title bar.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Live Recolour", id), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7drag rotate · scroll = spin · shift+scroll = zoom · R = reset"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Ctrl+Z undo · Ctrl+C copy · Enter apply · ? help"), 8, 32, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta);

        // §A4 movable action bar (hidden during the discard confirm).
        if (!confirmingCancel && bar != null) bar.render(ctx, width, height, textRenderer, mx, my);
        if (confirmingCancel) renderCancelConfirm(ctx);
        settings.render(ctx, width, height, textRenderer, mx, my); // §G27.8.B ⚙ popup
        help.render(ctx, width, height, textRenderer, mx, my); // drawn last → always on top; only the red X closes it

        if (!cubeDragging && spinning && !confirmingCancel) yaw = (yaw + spinSpeed * delta) % 360.0;
    }

    private void drawStatus(DrawContext ctx) {
        String spin = spinning ? Math.round(spinSpeed / SPIN_MAX * 100) + "%" : "paused";
        int zoom = (int) Math.round((half - HALF_MIN) * 100.0 / (HALF_MAX - HALF_MIN));
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8spin §7" + spin + " §8· zoom §7" + zoom + "%"), 8, height - 14, 0xFFFFFFFF);
    }

    private void renderCancelConfirm(DrawContext ctx) {
        int cx = width / 2, cy = height / 2, pw = 200, ph = 70;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, GOLD);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, CbTheme.DIALOG_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fDiscard slider changes?"), cx, cy - 18, 0xFFFFFFFF);
    }

    // ── history / actions ─────────────────────────────────────────────────────
    private double[] snapshot() {
        double[] t = tone.snapshot();
        return new double[]{ hue.value, sat.value, light.value, t[0], t[1], t[2], t[3], t[4] };
    }
    private void restore(double[] s) {
        hue.value = s[0]; sat.value = s[1]; light.value = s[2];
        tone.restore(new double[]{ s[3], s[4], s[5], s[6], s[7] });
    }
    private void pushUndo() { undo.push(snapshot()); redo.clear(); }

    private void undo() { if (!undo.isEmpty()) { redo.push(snapshot()); restore(undo.pop()); } }
    private void redo() { if (!redo.isEmpty()) { undo.push(snapshot()); restore(redo.pop()); } }

    private void randomize() {
        pushUndo();
        java.util.Random r = new java.util.Random();
        hue.value = r.nextInt(361) - 180;
        sat.value = r.nextInt(201);
        light.value = 50 + r.nextInt(101); // keep it visible (50–150%)
    }

    private void reset() { pushUndo(); hue.value = 0; sat.value = 100; light.value = 100; tone.reset(); }

    private void copy() {
        String code = (int) Math.round(hue.value) + "/" + (int) Math.round(sat.value) + "/" + (int) Math.round(light.value);
        if (client == null) return;
        client.keyboard.setClipboard(code);
        CbToast.success("Copied H/S/L §7" + code); // §G27.13 — code is on the clipboard; no chat echo
    }

    private void paste() {
        if (client == null) return;
        try {
            String[] p = client.keyboard.getClipboard().trim().split("/");
            if (p.length == 3) {
                pushUndo();
                hue.value   = clamp(Double.parseDouble(p[0]), hue.min, hue.max);
                sat.value   = clamp(Double.parseDouble(p[1]), sat.min, sat.max);
                light.value = clamp(Double.parseDouble(p[2]), light.min, light.max);
            }
        } catch (Exception ignored) {}
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ── input ───────────────────────────────────────────────────────────────--
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (help.mouseClicked(mx, my)) return true;                       // help open → only the red X closes it
        if (settings.mouseClicked(mx, my)) return true;                   // settings open → modal
        if (confirmingCancel) return super.mouseClicked(mx, my, button);
        if (super.mouseClicked(mx, my, button)) return true;
        if (bar != null && bar.mouseClicked(mx, my, button)) return true; // §A4 action bar
        for (CbGradSlider s : new CbGradSlider[]{ hue, sat, light }) {
            if (s.hit(mx, my)) { pushUndo(); dragSlider = s; s.setFromX(mx); return true; }
        }
        if (tone.mouseClicked(mx, my, button)) return true; // §C3 tone sliders + filter buttons
        if (button == 0) { cubeDragging = true; dragged = false; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settings.mouseDragged(mx, my)) return true;                           // settings sliders
        if (bar != null && bar.mouseDragged(mx, my, button, dx, dy)) return true; // bar re-dock drag
        if (dragSlider != null) { dragSlider.setFromX(mx); return true; }
        if (tone.mouseDragged(mx)) return true;
        if (cubeDragging) {
            dragged = true;
            yaw = (yaw + dx * 0.6) % 360.0;
            pitch = Math.max(-85, Math.min(85, pitch - dy * 0.6));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (settings.mouseReleased()) { cubeDragging = false; return true; }
        if (bar != null && bar.mouseReleased(mx, my, button)) { cubeDragging = false; return true; }
        if (cubeDragging && !dragged) spinning = !spinning;
        dragSlider = null; cubeDragging = false; tone.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (bar != null && bar.isPointInside(mx, my)) return true; // don't spin/zoom over the action bar
        if (hasShiftDown())
            half = Math.max(HALF_MIN, Math.min(HALF_MAX, half + (int) Math.signum(vAmt) * ZOOM_STEP));
        else
            spinSpeed = Math.max(0, Math.min(SPIN_MAX, spinSpeed + vAmt * SPIN_STEP));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0, shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        if (help.isOpen()) {                                                    // help open → swallow keys
            if (key == GLFW.GLFW_KEY_ESCAPE) help.close();                      // Esc closes the help, not the screen
            return true;
        }
        if (settings.keyPressed(key)) return true;                              // settings open → Esc closes, keys swallowed
        if (key == GLFW.GLFW_KEY_SLASH) { help.open(); return true; }           // ? opens help
        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_Z -> { if (shift) redo(); else undo(); return true; }
                case GLFW.GLFW_KEY_Y -> { redo();      return true; }
                case GLFW.GLFW_KEY_C -> { copy();      return true; }
                case GLFW.GLFW_KEY_V -> { paste();     return true; }
                case GLFW.GLFW_KEY_R -> { randomize(); return true; }
            }
        }
        switch (key) {
            case GLFW.GLFW_KEY_R -> { yaw = DEF_YAW; pitch = DEF_PITCH; spinSpeed = CbScreenPrefs.get().spinDefault; half = DEF_HALF; spinning = true; return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { apply(); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() { cube.dispose(); super.removed(); }
}
