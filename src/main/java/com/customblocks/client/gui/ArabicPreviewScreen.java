/**
 * ArabicPreviewScreen.java — Group 13 §1 live word preview; Group 27 §G27.1 unified frame. CLIENT-ONLY.
 *
 * Pack-free 3D drag-to-rotate preview of a word block (texture fetched once from /tex/<id>, downsampled
 * to a cell grid, drawn via GUI MatrixStack fills). Colour swatches + Create + Back. G27.1 adds the
 * standard frame: 0x33 backdrop, gold title bar + hints, [?] help, bottom action bar
 * [Undo][Redo][Rand]···[§aCreate]···[Copy][Reset][Back], session undo/redo of colours, cancel-confirm.
 * Client only previews/requests; colour + Create go to the server (ArabicPreviewPayload), Back/Esc send
 * GuiBackPayload. Called by CustomBlocksClient (OpenGuiPayload mode=ARABIC_PREVIEW).
 */
package com.customblocks.client.gui;

import com.customblocks.client.gui.panel.CbActionBar;
import com.customblocks.client.gui.panel.CbColorPanel;
import com.customblocks.network.payloads.ArabicPreviewPayload;
import com.customblocks.network.payloads.GuiBackPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;

@Environment(EnvType.CLIENT)
public class ArabicPreviewScreen extends Screen {

    // View controls — defaults + limits for the live tweaks below.
    private static final double DEF_YAW = 28, DEF_PITCH = 16, DEF_SPIN = 0.45;
    private static final int    DEF_HALF = 62;                       // half cube size in px (edge = 2*half)
    private static final double SPIN_MAX = 2.5, SPIN_STEP = 0.15;    // scroll = spin speed
    private static final int    HALF_MIN = 36, HALF_MAX = 120, ZOOM_STEP = 8; // shift+scroll = zoom

    // ── Group 27 standard frame ───────────────────────────────────────────────
    private static final int BAR_BG   = 0xAA000000;
    private static final int GOLD     = 0xFF_FF_AA_00;
    private static final int BAR_H    = 42;

    // Curated palette — mirrors ColorStudioMenu so the two pickers match.
    private static final String[] PALETTE = {
            "#0A0A0A", "#FFFFFF", "#FF0000", "#1E8C1E", "#F0C814", "#2E6FF2",
            "#21C0C0", "#8E3FD0", "#F060A8", "#F08020", "#808080", "#8A5A2B",
    };

    private String pid, texUrl, text;
    private int letterArgb, bgArgb;

    // Downsampled texture: packed 0xAARRGGBB per cell. Rebuilt on each (re)fetch.
    private volatile int[] grid;
    private volatile boolean loading = true, failed;
    private final PreviewCube cube = new PreviewCube(); // shared cube renderer; disposed in removed()

    private double yaw = DEF_YAW, pitch = DEF_PITCH;
    private double spinSpeed = DEF_SPIN;     // idle auto-spin (deg/render delta); scroll adjusts
    private boolean spinning = true;         // click (no drag) pauses/resumes
    private int     half = DEF_HALF;         // shift+scroll zooms
    private boolean dragging, dragged;       // dragged = the press actually moved (rotate), not a click

    // Group 27 frame state.
    private boolean confirmingCancel;
    private CbColorPanel panel; // §G27.7 §B floating colour panel (right side; replaces bottom swatch rows)
    private CbActionBar bar;    // §G27.7 §A4 dockable/hideable action bar (replaces the fixed bottom strip)
    private final CbHelpOverlay help = new CbHelpOverlay("Live Preview", java.util.List.of(
            new CbHelpOverlay.Group("VIEW", java.util.List.of(
                    new CbHelpOverlay.Row("Drag", "Rotate the cube"),
                    new CbHelpOverlay.Row("Scroll", "Spin speed"),
                    new CbHelpOverlay.Row("Shift+Scroll", "Zoom"),
                    new CbHelpOverlay.Row("Click", "Pause / resume spin"),
                    new CbHelpOverlay.Row("R", "Reset view"))),
            new CbHelpOverlay.Group("EDIT", java.util.List.of(
                    new CbHelpOverlay.Row("Ctrl+Z / Y", "Undo / Redo"),
                    new CbHelpOverlay.Row("Ctrl+C / V", "Copy / Paste colours"),
                    new CbHelpOverlay.Row("Ctrl+R", "Randomise colours"),
                    new CbHelpOverlay.Row("Enter", "Create block")))));
    private final int initLetter, initBg;                 // colours when the screen opened (Reset + dirty test)
    private final java.util.Deque<int[]> undo = new java.util.ArrayDeque<>(); // each = {letterArgb, bgArgb}
    private final java.util.Deque<int[]> redo = new java.util.ArrayDeque<>();

    public ArabicPreviewScreen(String pid, String texUrl, int letterArgb, int bgArgb, String text) {
        super(Text.literal("Arabic Preview"));
        set(pid, texUrl, letterArgb, bgArgb, text);
        this.initLetter = letterArgb; this.initBg = bgArgb; // captured once; refresh() must not reset it
    }

    private void set(String pid, String texUrl, int letterArgb, int bgArgb, String text) {
        this.pid = pid; this.texUrl = texUrl; this.text = text;
        this.letterArgb = letterArgb; this.bgArgb = bgArgb;
    }

    /** Server re-rendered the texture (colour change) — refresh in place, keep the rotation. */
    public void refresh(String pid, String texUrl, int letterArgb, int bgArgb, String text) {
        set(pid, texUrl, letterArgb, bgArgb, text);
        fetchAsync();
    }

    @Override
    protected void init() {
        if (confirmingCancel) { addCancelButtons(); return; } // re-init while the discard overlay is up
        // [?] help — top-right of the title bar
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> help.toggle()).dimensions(width - 24, 10, 16, 16).build());
        addDrawableChild(new CbDimSlider(width - 110, 8, 78, 13)); // §A2 backdrop dim
        // §A4 dockable / hideable / smaller action bar (replaces the old fixed full-width button strip).
        if (bar == null) bar = new CbActionBar("arabic", java.util.List.of(
                new CbActionBar.Item("Undo", this::undo, false),
                new CbActionBar.Item("Redo", this::redo, false),
                new CbActionBar.Item("Rand", this::randomize, false),
                new CbActionBar.Item("Create", this::create, true),
                new CbActionBar.Item("Copy", this::copy, false),
                new CbActionBar.Item("Reset", this::reset, false),
                new CbActionBar.Item("Back", this::close, false)));
        bar.init(width, height);
        if (panel == null) panel = new CbColorPanel("arabic", java.util.List.of(
                target("Letter", () -> letterArgb, rgb -> sendColour(0xFF000000 | rgb, bgArgb)),
                target("Background", () -> bgArgb, rgb -> sendColour(letterArgb, 0xFF000000 | rgb))));
        panel.init(width, height);
        if (grid == null) fetchAsync();
    }

    /** Small adapter so the colour panel can drive a letter/background colour without exposing screen state. */
    private CbColorPanel.Target target(String label, java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
        return new CbColorPanel.Target() {
            public String label() { return label; }
            public int get() { return get.getAsInt() & 0xFFFFFF; }
            public void apply(int rgb) { set.accept(rgb & 0xFFFFFF); }
        };
    }

    private void fetchAsync() {
        loading = true; failed = false;
        final String url = texUrl;
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(URI.create(url).toURL());
                if (img == null) { failed = true; loading = false; return; }
                grid = PreviewCube.downsample(img); // shared downsample; a new ref re-bakes the cube
                loading = false;
            } catch (Exception e) {
                failed = true; loading = false;
            }
        }, "CustomBlocks-ArabicPreviewFetch");
        t.setDaemon(true);
        t.start();
    }

    private void create() {
        if (bar != null) bar.flashPrimary(); // brief green flash over Create (Group 27 save feedback)
        if (client != null && client.player != null)
            client.player.sendMessage(Text.literal("§a✔ Creating block '" + pid + "'…"), false);
        ClientPlayNetworking.send(new ArabicPreviewPayload(ArabicPreviewPayload.ACTION_CREATE, letterArgb, bgArgb));
        // The server makes the block and opens the hub chest GUI, which replaces this screen.
    }

    private boolean isDirty() { return letterArgb != initLetter || bgArgb != initBg; }

    /** Back / Esc → confirm if colours changed, else ask the server to reopen the Color Studio. */
    @Override
    public void close() {
        if (isDirty() && !confirmingCancel) { confirmingCancel = true; clearChildren(); init(); return; }
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
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // own backdrop drawn in render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (panel != null) panel.preRender(ctx); // §B4 dropper: capture the frame before any UI is drawn
        ctx.fill(0, 0, width, height, CbScreenPrefs.get().backdrop()); // §A2 persisted dim (world visible)

        // Content (drawn before the bars so the bars give clean edges over any overlap).
        if (grid == null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(failed ? "§cpreview unavailable" : "§8loading preview…"),
                    width / 2, height / 2 - 30, 0xFFFFFFFF);
        } else {
            drawCube(ctx);
        }
        drawStatus(ctx);

        // Title bar strip + gold border.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lLive Preview §7— §f" + text), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7drag rotate · scroll = speed · shift+scroll = zoom · click = pause · R = reset"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Ctrl+Z undo · Ctrl+C copy · Enter create · ? help"), 8, 32, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta); // top-bar widgets ([?], dim slider) on top of the title strip

        // §A4 action bar + §B colour panel (both movable; hidden during the discard confirm).
        if (!confirmingCancel) {
            if (bar != null) bar.render(ctx, width, height, textRenderer, mx, my);
            if (panel != null) panel.render(ctx, width, height, textRenderer, mx, my);
        }
        if (confirmingCancel) renderCancelConfirm(ctx);
        help.render(ctx, width, height, textRenderer, mx, my); // drawn last → always on top; only the red X closes it

        if (!dragging && spinning && !confirmingCancel) { yaw = (yaw + spinSpeed * delta) % 360.0; } // idle spin (click toggles)
    }

    private void renderCancelConfirm(DrawContext ctx) {
        int cx = width / 2, cy = height / 2;
        int pw = 200, ph = 70;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, GOLD);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, 0xFF1A1A1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fDiscard colour changes?"), cx, cy - 18, 0xFFFFFFFF);
    }

    /** Tiny corner readout so the scroll / zoom / pause controls are discoverable. */
    private void drawStatus(DrawContext ctx) {
        String spin = spinning ? Math.round(spinSpeed / SPIN_MAX * 100) + "%" : "paused";
        int zoom = (int) Math.round((half - HALF_MIN) * 100.0 / (HALF_MAX - HALF_MIN));
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8spin §7" + spin + " §8· zoom §7" + zoom + "%"), 8, height - BAR_H - 11, 0xFFFFFFFF);
    }

    // ── 3D cube — delegated to the shared PreviewCube (G27.7 §A1 textured-quad rebuild) ──
    private void drawCube(DrawContext ctx) {
        // §G27.7 §B Option A: block on the left, colour panel on the right → centre the cube in the free area.
        int cx = (panel != null ? panel.x() : width) / 2, cy = height / 2 - 22;
        cube.render(ctx, grid, cx, cy, half, yaw, pitch, PreviewCube.AS_IS, 0L);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (help.mouseClicked(mx, my)) return true;                       // help open → only the red X closes it
        if (confirmingCancel) return super.mouseClicked(mx, my, button);  // only the Yes/No buttons are live
        if (panel != null && panel.isDropperActive()) { panel.mouseClicked(mx, my, button); return true; } // dropper: any click samples
        if (super.mouseClicked(mx, my, button)) return true;              // vanilla top widgets ([?], dim slider)
        if (bar != null && bar.mouseClicked(mx, my, button)) return true; // §A4 action bar (returns false when clicked outside it)
        if (panel != null && panel.mouseClicked(mx, my, button)) return true; // panel UI (returns false when clicked outside it)
        if (button == 0) { dragging = true; dragged = false; return true; } // press: rotate if it moves, else pause
        return false;
    }

    /** A user-initiated colour change — records undo history, then applies + sends. */
    private void sendColour(int letter, int bg) {
        undo.push(new int[]{ letterArgb, bgArgb }); redo.clear();
        applyColour(letter, bg);
    }

    /** Apply a colour pair without touching history (used by undo/redo). */
    private void applyColour(int letter, int bg) {
        letterArgb = letter; bgArgb = bg; // optimistic; server round-trip refreshes the texture
        ClientPlayNetworking.send(new ArabicPreviewPayload(ArabicPreviewPayload.ACTION_COLOUR, letter, bg));
    }

    private void undo() {
        if (undo.isEmpty()) return;
        redo.push(new int[]{ letterArgb, bgArgb });
        int[] s = undo.pop(); applyColour(s[0], s[1]);
    }

    private void redo() {
        if (redo.isEmpty()) return;
        undo.push(new int[]{ letterArgb, bgArgb });
        int[] s = redo.pop(); applyColour(s[0], s[1]);
    }

    private void randomize() {
        java.util.Random r = new java.util.Random();
        int li = r.nextInt(PALETTE.length), bi;
        do { bi = r.nextInt(PALETTE.length); } while (bi == li && PALETTE.length > 1);
        sendColour(argb(PALETTE[li]), argb(PALETTE[bi]));
    }

    private void reset() { sendColour(initLetter, initBg); }

    private void copy() {
        String code = String.format("#%06X/#%06X", letterArgb & 0xFFFFFF, bgArgb & 0xFFFFFF);
        if (client == null) return;
        client.keyboard.setClipboard(code);
        if (client.player != null) client.player.sendMessage(Text.literal("§a✔ Copied colours §7" + code), false);
    }

    private void paste() {
        if (client == null) return;
        try {
            String[] parts = client.keyboard.getClipboard().trim().split("/");
            if (parts.length == 2) sendColour(argb(parts[0]), argb(parts[1]));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (bar != null && bar.mouseDragged(mx, my, button, dx, dy)) return true;     // bar re-dock drag
        if (panel != null && panel.mouseDragged(mx, my, button, dx, dy)) return true; // panel move / reorder
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
        if (bar != null && bar.mouseReleased(mx, my, button)) { dragging = false; return true; }
        if (panel != null && panel.mouseReleased(mx, my, button)) { dragging = false; return true; }
        if (dragging && !dragged) spinning = !spinning; // click without a drag = pause / resume spin
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (panel != null && panel.isPointInside(mx, my)) return true; // don't spin/zoom while over the panel
        if (bar != null && bar.isPointInside(mx, my)) return true;     // …or over the action bar
        if (hasShiftDown()) {
            half = Math.max(HALF_MIN, Math.min(HALF_MAX, half + (int) Math.signum(vAmt) * ZOOM_STEP)); // shift = zoom
        } else {
            spinSpeed = Math.max(0, Math.min(SPIN_MAX, spinSpeed + vAmt * SPIN_STEP));                 // scroll = spin speed
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        if (help.isOpen()) {                                                    // help open → swallow keys
            if (key == GLFW.GLFW_KEY_ESCAPE) help.close();                      // Esc closes the help, not the screen
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH) { help.open(); return true; }           // ? opens help
        if (panel != null && panel.keyPressed(key, scan, mods)) return true;    // panel: fast keys 1–9, hex input, dropper Esc
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
            case GLFW.GLFW_KEY_R -> { // reset view
                yaw = DEF_YAW; pitch = DEF_PITCH; spinSpeed = DEF_SPIN; half = DEF_HALF; spinning = true;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { create(); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (panel != null && panel.charTyped(c, mods)) return true; // panel hex / palette-name entry
        return super.charTyped(c, mods);
    }

    @Override
    public void removed() { if (panel != null) panel.onClosed(); cube.dispose(); super.removed(); }

    private static int argb(String hex) {
        try { return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16); }
        catch (Exception e) { return 0xFF000000; }
    }
}
