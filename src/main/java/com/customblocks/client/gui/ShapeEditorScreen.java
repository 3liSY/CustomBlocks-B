/**
 * ShapeEditorScreen.java — Group 27 §G27.5 (Shape Editor). CLIENT-SIDE ONLY. **PARTIAL** build.
 *
 * A named-shape picker for one block: pick from the 10 built-in {@link BlockShapes} and watch a live
 * 3D shape preview (the block's texture painted onto the shape's boxes via the shared {@link PreviewCube}),
 * then Save to apply it through the existing /cb setshape rail (ShapeEditorPayload → server). Follows the
 * Group 27 frame: dimmable backdrop, red+black title bar + hints (locked palette 2026-07-04), [?] help,
 * movable CbActionBar (§A4), undo/redo, cancel-confirm.
 *
 * NOTE: this is the partial build the dev approved. The full freeform custom-AABB editor (drag box
 * corners, multiple custom boxes, mirror, snap) needs a new shape backend and is a separate future
 * session — see project memory. Here you choose from the named shapes the mod actually supports.
 *
 * Depends on: Screen/DrawContext/ButtonWidget, PreviewCube, BlockShapes, ShapeEditorPayload.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=SHAPE_EDITOR; data = "id|texUrl|currentShape").
 */
package com.customblocks.client.gui;

import com.customblocks.block.BlockShapes;
import com.customblocks.network.payloads.ShapeEditorPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ShapeEditorScreen extends Screen {

    private static final int BAR_BG = CbTheme.BAR_BG, GOLD = CbTheme.ACCENT, BAR_H = 42;
    private static final double DEF_YAW = 28, DEF_PITCH = 16, DEF_SPIN = 0.45;
    private static final int DEF_HALF = 62, HALF_MIN = 36, HALF_MAX = 120, ZOOM_STEP = 8;
    private static final double SPIN_MAX = 2.5, SPIN_STEP = 0.15;

    private final String id, texUrl, originalShape;
    private final String[] shapes = BlockShapes.names();
    private String selected;
    private int[][] chipRects; // laid out in render(), parallel to shapes

    private volatile int[] grid; // texture (or flat fallback); null = still loading
    private volatile boolean loaded;
    private final PreviewCube cube = new PreviewCube(); // owns the baked atlas; disposed in removed()

    private double yaw = DEF_YAW, pitch = DEF_PITCH, spinSpeed = CbScreenPrefs.get().spinDefault;
    private boolean spinning = true, dragging, dragged;
    private int half = DEF_HALF;
    private boolean confirmingCancel;
    private com.customblocks.client.gui.panel.CbActionBar bar; // §A4 movable/dockable action bar
    private final CbSettingsOverlay settings = new CbSettingsOverlay(); // §G27.8.B ⚙ Settings
    private final java.util.Deque<String> undo = new java.util.ArrayDeque<>();
    private final java.util.Deque<String> redo = new java.util.ArrayDeque<>();
    private final CbHelpOverlay help = new CbHelpOverlay("Shape Editor", java.util.List.of(
            new CbHelpOverlay.Group("VIEW", java.util.List.of(
                    new CbHelpOverlay.Row("Click chip", "Pick a shape"),
                    new CbHelpOverlay.Row("Drag", "Rotate the cube"),
                    new CbHelpOverlay.Row("Scroll", "Spin speed"),
                    new CbHelpOverlay.Row("Shift+Scroll", "Zoom"),
                    new CbHelpOverlay.Row("R", "Reset view"))),
            new CbHelpOverlay.Group("EDIT", java.util.List.of(
                    new CbHelpOverlay.Row("Ctrl+Z / Y", "Undo / Redo"),
                    new CbHelpOverlay.Row("Ctrl+C / V", "Copy / Paste shape"),
                    new CbHelpOverlay.Row("Ctrl+R", "Random shape"),
                    new CbHelpOverlay.Row("Enter", "Save shape")))));

    public ShapeEditorScreen(String id, String texUrl, String currentShape) {
        super(Text.literal("Shape Editor"));
        this.id = id;
        this.texUrl = texUrl;
        this.originalShape = (currentShape == null || currentShape.isBlank()) ? "full" : currentShape;
        this.selected = originalShape;
    }

    @Override
    protected void init() {
        if (confirmingCancel) { addCancelButtons(); return; }
        // §A4 movable/dockable action bar (replaces the old fixed full-width button strip).
        if (bar == null) bar = new com.customblocks.client.gui.panel.CbActionBar("shape", java.util.List.of(
                new com.customblocks.client.gui.panel.CbActionBar.Item("Undo", this::undo, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Redo", this::redo, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Rand", this::randomize, false),
                new com.customblocks.client.gui.panel.CbActionBar.Item("Save", this::save, true),
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
        if (!loaded) fetchAsync();
    }

    private void fetchAsync() {
        Thread t = new Thread(() -> {
            try {
                // §F3: prefer the block's already-baked local pack texture (no network, never 403s).
                int[] local = LocalTexturePreview.load(id);
                if (local != null) { grid = local; loaded = true; return; }
                if (texUrl != null && !texUrl.isBlank()) {
                    // Fallback: robust URL fetch (browser UA + Tenor/Giphy resolve) for blocks not baked locally.
                    StudioTextureLoader.Result r = StudioTextureLoader.load(texUrl);
                    if (r != null && r.frames() != null && r.frames().length > 0) { grid = r.frames()[0]; loaded = true; return; }
                }
            } catch (Exception ignored) {}
            grid = PreviewCube.solid(0x9AA0A6); // no texture → neutral grey shape
            loaded = true;
        }, "CustomBlocks-ShapeEditorFetch");
        t.setDaemon(true);
        t.start();
    }

    private void save() {
        if (bar != null) bar.flashPrimary(); // lime "saved" pulse (Group 27 save feedback)
        ClientPlayNetworking.send(new ShapeEditorPayload(id, selected));
        CbToast.success("Shape '" + selected + "' applied to '" + id + "'"); // §G27.13 — toast, not chat
        confirmingCancel = false;
        super.close();
    }

    private boolean isDirty() { return !selected.equals(originalShape); }

    @Override
    public void close() {
        if (isDirty() && CbScreenPrefs.get().confirmDiscard && !confirmingCancel) { // §G27.8.B toggleable
            confirmingCancel = true; clearChildren(); init(); return;
        }
        super.close();
    }

    private void addCancelButtons() {
        int cx = width / 2, cy = height / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("§cYes, discard"), b -> { confirmingCancel = false; super.close(); })
                .dimensions(cx - 96, cy + 2, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Keep editing"), b -> { confirmingCancel = false; clearChildren(); init(); })
                .dimensions(cx + 8, cy + 2, 88, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* world stays visible */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, CbScreenPrefs.get().backdrop()); // §A2 persisted dim (world visible)

        // 3D shape preview (left of the chip panel).
        int cubeCx = width / 2 - 110, cubeCy = height / 2 - 4;
        if (grid == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8loading…"), cubeCx, cubeCy, 0xFFFFFFFF);
        } else {
            cube.renderShape(ctx, grid, cubeCx, cubeCy, half, yaw, pitch, BlockShapes.boxes(selected), PreviewCube.AS_IS, 0L);
        }

        drawChips(ctx, mx, my);
        drawStatus(ctx);

        // Title bar.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Shape Editor", id), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7drag rotate · scroll = spin · click a shape · R = reset"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Ctrl+Z undo · Ctrl+C copy · Enter save · ? help"), 8, 32, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta);

        // §A4 movable action bar (hidden during the discard confirm).
        if (!confirmingCancel && bar != null) bar.render(ctx, width, height, textRenderer, mx, my);
        if (confirmingCancel) renderCancelConfirm(ctx);
        settings.render(ctx, width, height, textRenderer, mx, my); // §G27.8.B ⚙ popup
        help.render(ctx, width, height, textRenderer, mx, my); // drawn last → always on top; only the red X closes it

        if (!dragging && spinning && !confirmingCancel) yaw = (yaw + spinSpeed * delta) % 360.0;
    }

    /** Lay out + draw the 10 shape chips in a 2-column right panel; highlight the selected one. */
    private void drawChips(DrawContext ctx, int mx, int my) {
        int cols = 2, chipW = 118, chipH = 18, gx = 8, gy = 4;
        int x0 = width / 2 + 16, y0 = height / 2 - (((shapes.length + 1) / cols) * (chipH + gy)) / 2;
        chipRects = new int[shapes.length][4];
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7SHAPES"), x0, y0 - 14, 0xFFFFFFFF);
        for (int i = 0; i < shapes.length; i++) {
            int col = i % cols, rowi = i / cols;
            int x = x0 + col * (chipW + gx), y = y0 + rowi * (chipH + gy);
            chipRects[i] = new int[]{ x, y, chipW, chipH };
            boolean sel = shapes[i].equals(selected);
            boolean hover = mx >= x && mx < x + chipW && my >= y && my < y + chipH;
            ctx.fill(x - 1, y - 1, x + chipW + 1, y + chipH + 1, sel ? GOLD : (hover ? 0xFFBBBBBB : 0xFF000000));
            ctx.fill(x, y, x + chipW, y + chipH, sel ? CbTheme.SEL_FILL : 0xFF1A1A1A);
            ctx.drawTextWithShadow(textRenderer, Text.literal((sel ? "§c" : "§f") + shapes[i]), x + 5, y + 5, 0xFFFFFFFF);
        }
        // Selected-shape description under the panel.
        int descY = y0 + (((shapes.length + 1) / cols)) * (chipH + gy) + 4;
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8" + BlockShapes.description(selected)), x0, descY, 0xFFFFFFFF);
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
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fDiscard shape change?"), cx, cy - 18, 0xFFFFFFFF);
    }

    // ── selection history / actions ───────────────────────────────────────────
    private void select(String shape) {
        if (shape == null || shape.equals(selected) || !BlockShapes.isValid(shape)) return;
        undo.push(selected); redo.clear();
        selected = shape;
    }

    private void undo() { if (!undo.isEmpty()) { redo.push(selected); selected = undo.pop(); } }
    private void redo() { if (!redo.isEmpty()) { undo.push(selected); selected = redo.pop(); } }

    private void randomize() {
        String pick = shapes[new java.util.Random().nextInt(shapes.length)];
        select(pick);
    }

    private void reset() { select(originalShape); }

    private void copy() {
        if (client == null) return;
        client.keyboard.setClipboard(selected);
        CbToast.success("Copied shape §7" + selected); // §G27.13 — toast, not chat
    }

    private void paste() {
        if (client == null) return;
        try { select(client.keyboard.getClipboard().trim().toLowerCase(java.util.Locale.ROOT)); }
        catch (Exception ignored) {}
    }

    // ── input ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (help.mouseClicked(mx, my)) return true;                       // help open → only the red X closes it
        if (settings.mouseClicked(mx, my)) return true;                   // settings open → modal
        if (confirmingCancel) return super.mouseClicked(mx, my, button);
        if (super.mouseClicked(mx, my, button)) return true;
        if (bar != null && bar.mouseClicked(mx, my, button)) return true; // §A4 action bar
        if (chipRects != null) {
            for (int i = 0; i < shapes.length; i++) {
                int[] r = chipRects[i];
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) { select(shapes[i]); return true; }
            }
        }
        if (button == 0) { dragging = true; dragged = false; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settings.mouseDragged(mx, my)) return true;                           // settings sliders
        if (bar != null && bar.mouseDragged(mx, my, button, dx, dy)) return true; // bar re-dock drag
        if (dragging) {
            dragged = true;
            yaw = (yaw + dx * 0.6) % 360.0;
            pitch = Math.max(-85, Math.min(85, pitch - dy * 0.6));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (settings.mouseReleased()) { dragging = false; return true; }
        if (bar != null && bar.mouseReleased(mx, my, button)) { dragging = false; return true; }
        if (dragging && !dragged) spinning = !spinning;
        dragging = false;
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
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { save(); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() { cube.dispose(); super.removed(); }
}
