/**
 * HudEditorScreen.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the free-floating, brick-based HUD editor. Renders a live preview over the
 * world (the real block you aim at, else a sample), a right-dock brick panel (per-brick
 * show/hide, inspect, delete, reorder), an add-brick palette, master HUD + snap toggles, and
 * the Group 27 frame (title bar, hint lines, bottom action bar, cancel-confirm, save flash,
 * Ctrl+Z/Y/C/V/Enter/Esc). Drag a brick to place it; magnetic snapping + the colour picker +
 * presets + the per-brick inspector are layered on in later steps.
 *
 * Depends on: HudConfig, HudField, HudFieldType, HudRenderer.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=HUD_EDITOR), EscMenuButtons, CbKeybinds.
 */
package com.customblocks.client.gui;

import com.customblocks.client.HudConfig;
import com.customblocks.client.HudRenderer;
import com.customblocks.client.gui.hud.HudBrickInspector;
import com.customblocks.client.gui.hud.HudBrickPalette;
import com.customblocks.client.gui.hud.HudBrickRow;
import com.customblocks.client.gui.hud.HudEditorOverlays;
import com.customblocks.client.gui.hud.HudPresetBrowser;
import com.customblocks.client.hud.HudAnchors;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudFieldType;
import com.customblocks.client.hud.HudHoverSound;
import com.customblocks.client.hud.HudPresetStore;
import com.customblocks.client.hud.HudSnap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HudEditorScreen extends Screen {

    // Group 27 standard colours.
    private static final int BACKDROP = 0x33000000;
    private static final int BAR_BG   = 0xAA000000;
    private static final int GOLD     = 0xFFFFAA00;
    private static final int CYAN      = 0xFF39E0C8;
    private static final int BAR_H    = 42;

    // Right-dock panel geometry.
    private static final int PANEL_W = 184;
    private static final int ROW_H   = 16;

    // Sample context for previewing block-info bricks when not aiming at a custom block.
    private static final HudFieldType.Ctx SAMPLE = new HudFieldType.Ctx(
            true, "example_id", "Example Block", 1,
            "decoration", 7, 2.0f, "stone", "full", false,
            true, 100, 64, -200, 12, 4.5, "north");

    // Snapshot for revert-on-cancel + dirty detection.
    private final List<HudField> orig = HudConfig.snapshotFields();
    private final boolean origVisible = HudConfig.visible;

    // Selection + drag.
    private int selected = -1;
    private boolean dragging = false;
    private int grabDX, grabDY;

    // Snap guide lines (Integer.MIN_VALUE = none), drawn cyan while dragging.
    private int guideX = Integer.MIN_VALUE, guideY = Integer.MIN_VALUE;
    private boolean wasSnapped = false;

    // Add-brick palette overlay.
    private final HudBrickPalette palette = new HudBrickPalette();

    // Panel drag-reorder (⠿ handle) + collapsible see-through panel (Tab).
    private int reorderFrom = -1, reorderTo = -1;
    private boolean panelCollapsed = false;

    // Undo / redo (in-session layout snapshots) + overlays.
    private final Deque<List<HudField>> undoStack = new ArrayDeque<>();
    private final Deque<List<HudField>> redoStack = new ArrayDeque<>();
    private boolean confirmingCancel = false;
    private boolean helpOpen = false;

    // Save flash.
    private boolean saveFlash = false;
    private long    saveFlashEnd = 0;

    private ButtonWidget masterBtn, snapBtn, triggerBtn, soundBtn, volBtn;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        int barY = height - BAR_H;

        // Bottom action bar: [Undo] [Redo] ··· [§aSave] ··· [Copy] [Reset] [Cancel]
        addDrawableChild(ButtonWidget.builder(Text.literal("Undo"), b -> undo())
                .dimensions(8, barY + 11, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Redo"), b -> redo())
                .dimensions(56, barY + 11, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§aSave"), b -> save())
                .dimensions(width / 2 - 44, barY + 11, 88, 20).build());
        int right = width - 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> onCancel())
                .dimensions(right - 66, barY + 11, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> { pushUndo(); HudConfig.resetDefaults(); selected = -1; rebuild(); })
                .dimensions(right - 136, barY + 11, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> copyLayout())
                .dimensions(right - 206, barY + 11, 66, 20).build());

        // [?] help overlay.
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> helpOpen = !helpOpen)
                .dimensions(width - 24, 10, 16, 16).build());

        if (panelCollapsed) return;   // see-through collapse (Tab) — hide panel widgets

        // Panel footer: preset browser · add-brick · master/snap · hover sound · volume.
        int hx = width - PANEL_W, hw = (PANEL_W - 12) / 2;
        int py = BAR_H + 12 + HudConfig.fields.size() * ROW_H + 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("Preset ▾"), b -> { pushUndo(); if (client != null) client.setScreen(new HudPresetBrowser(this)); })
                .dimensions(hx, py, PANEL_W - 8, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add brick"), b -> palette.toggle())
                .dimensions(hx, py + 22, PANEL_W - 8, 18).build());
        masterBtn = ButtonWidget.builder(masterLabel(), b -> { HudConfig.visible = !HudConfig.visible; masterBtn.setMessage(masterLabel()); })
                .dimensions(hx, py + 44, hw, 18).build();
        addDrawableChild(masterBtn);
        snapBtn = ButtonWidget.builder(snapLabel(), b -> { HudConfig.snapEnabled = !HudConfig.snapEnabled; snapBtn.setMessage(snapLabel()); })
                .dimensions(hx + hw + 4, py + 44, hw, 18).build();
        addDrawableChild(snapBtn);

        // Hover-sound row: trigger cycle · sound cycle (preview on pick).
        triggerBtn = ButtonWidget.builder(triggerLabel(), b -> {
            HudConfig.hoverTrigger = (HudConfig.hoverTrigger + 1) % 3;
            triggerBtn.setMessage(triggerLabel());
        }).dimensions(hx, py + 66, hw, 18).build();
        addDrawableChild(triggerBtn);
        soundBtn = ButtonWidget.builder(soundLabel(), b -> {
            HudConfig.hoverSound = HudHoverSound.cycleSound(HudConfig.hoverSound, 1);
            soundBtn.setMessage(soundLabel());
            HudHoverSound.preview();
        }).dimensions(hx + hw + 4, py + 66, hw, 18).build();
        addDrawableChild(soundBtn);

        // Volume: − [value] +
        addDrawableChild(ButtonWidget.builder(Text.literal("Vol −"), b -> { HudConfig.hoverVolume = Math.max(0, HudConfig.hoverVolume - 10); volBtn.setMessage(volLabel()); })
                .dimensions(hx, py + 88, 44, 18).build());
        volBtn = ButtonWidget.builder(volLabel(), b -> {}).dimensions(hx + 48, py + 88, PANEL_W - 8 - 48 - 40, 18).build();
        addDrawableChild(volBtn);
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> { HudConfig.hoverVolume = Math.min(100, HudConfig.hoverVolume + 10); volBtn.setMessage(volLabel()); })
                .dimensions(width - 8 - 36, py + 88, 36, 18).build());
    }

    private Text masterLabel()  { return Text.literal("HUD " + (HudConfig.visible ? "§aOn" : "§cOff")); }
    private Text snapLabel()    { return Text.literal("Snap " + (HudConfig.snapEnabled ? "§aOn" : "§7Off")); }
    private Text triggerLabel() { return Text.literal("Hover: §f" + HudHoverSound.triggerLabel(HudConfig.hoverTrigger)); }
    private Text soundLabel()   { return Text.literal("♪ §f" + HudHoverSound.soundLabel(HudConfig.hoverSound)); }
    private Text volLabel()     { return Text.literal("Vol " + HudConfig.hoverVolume + "%"); }

    /** Re-layout widgets after the brick count changes. */
    private void rebuild() { clearChildren(); init(); }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, BACKDROP);

        // Live preview (sample when not aiming at a custom block).
        HudFieldType.Ctx live = HudRenderer.buildContext(this.client);
        HudFieldType.Ctx preview = live.hasBlock() ? live : SAMPLE;
        HudRenderer.drawAll(ctx, this.client, preview);
        drawSelectionOutline(ctx, preview);

        // Cyan magnetic-snap guide lines while dragging.
        if (dragging) {
            if (guideX != Integer.MIN_VALUE) ctx.fill(guideX, 0, guideX + 1, height, CYAN);
            if (guideY != Integer.MIN_VALUE) ctx.fill(0, guideY, width, guideY + 1, CYAN);
        }

        // Title bar.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lHUD Editor"), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7drag bricks to place · magnetic snap · arrows nudge · shift = free"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Ctrl+Z undo · Ctrl+C copy · Enter save · ? help"), 8, 32, 0xFFFFFFFF);

        // Bottom action bar.
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, BAR_BG);
        ctx.fill(0, barY, width, barY + 1, GOLD);

        drawPanel(ctx, mx, my);
        super.render(ctx, mx, my, delta);   // widgets

        if (palette.isOpen()) palette.draw(ctx, textRenderer, width, PANEL_W, BAR_H, mx, my);

        if (saveFlash) {
            if (System.currentTimeMillis() < saveFlashEnd)
                ctx.fill(width / 2 - 44, barY + 11, width / 2 + 44, barY + 31, 0x4400FF44);
            else saveFlash = false;
        }

        if (helpOpen) HudEditorOverlays.drawHelp(ctx, textRenderer, width, height);
        if (confirmingCancel) HudEditorOverlays.drawConfirm(ctx, textRenderer, width, height);
    }

    private boolean handleConfirmClick(double mx, double my) {
        int cx = width / 2, cy = height / 2;
        if (my >= cy + 6 && my <= cy + 26) {
            if (mx >= cx - 104 && mx <= cx - 8) { discard(); return true; }
            if (mx >= cx + 8 && mx <= cx + 104) { confirmingCancel = false; return true; }
        }
        return true;   // swallow clicks while the dialog is open
    }

    /** Right-dock brick list: ⠿ reorder handle · eye · label · ⚙ · ✕. */
    private void drawPanel(DrawContext ctx, int mx, int my) {
        if (panelCollapsed) {
            ctx.drawText(textRenderer, Text.literal("§8Tab: show panel"), width - 96, BAR_H + 6, 0xFFFFFFFF, false);
            return;
        }
        int px = width - PANEL_W, py = BAR_H + 8;
        ctx.fill(px - 4, py - 4, width, py + HudConfig.fields.size() * ROW_H + 4, 0x88000000);
        for (int i = 0; i < HudConfig.fields.size(); i++) {
            int ry = py + i * ROW_H;
            HudBrickRow.draw(ctx, textRenderer, HudConfig.fields.get(i), px, ry, ROW_H, width,
                    i == selected, i == reorderTo && reorderFrom >= 0, CYAN);
        }
    }

    private void drawSelectionOutline(DrawContext ctx, HudFieldType.Ctx c) {
        if (selected < 0 || selected >= HudConfig.fields.size()) return;
        int[] b = HudRenderer.brickBounds(this.client, HudConfig.fields.get(selected), c, width, height);
        HudEditorOverlays.drawSelection(ctx, b, dragging, CYAN);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (helpOpen) { helpOpen = false; return true; }
        if (confirmingCancel) return handleConfirmClick(mouseX, mouseY);
        if (palette.isOpen()) {
            HudFieldType t = palette.click(mouseX, mouseY, width, PANEL_W, BAR_H);
            if (t != null) {
                pushUndo();
                HudField f = new HudField(t, 8, 8 + HudConfig.fields.size() * 12, HudField.Anchor.TL, 1.0f);
                HudConfig.fields.add(f);
                selected = HudConfig.fields.size() - 1;
                rebuild();
            }
            return true;
        }
        if (handlePanelClick(mouseX, mouseY)) return true;
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Begin dragging a brick if the cursor is over its preview bounds.
        if (button == 0) {
            HudFieldType.Ctx c = previewCtx();
            for (int i = HudConfig.fields.size() - 1; i >= 0; i--) {
                int[] b = HudRenderer.brickBounds(this.client, HudConfig.fields.get(i), c, width, height);
                if (b == null) continue;
                if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                    pushUndo();
                    selected = i; dragging = true;
                    grabDX = (int) Math.round(mouseX) - b[0];
                    grabDY = (int) Math.round(mouseY) - b[1];
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handlePanelClick(double mx, double my) {
        if (panelCollapsed) return false;
        int px = width - PANEL_W, py = BAR_H + 8;
        for (int i = 0; i < HudConfig.fields.size(); i++) {
            int ry = py + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            switch (HudBrickRow.zoneAt(mx, px, width)) {
                case HANDLE -> { reorderFrom = i; reorderTo = i; selected = i; return true; }
                case EYE    -> { pushUndo(); HudConfig.fields.get(i).visible = !HudConfig.fields.get(i).visible; return true; }
                case GEAR   -> {
                    pushUndo(); selected = i;
                    if (client != null) client.setScreen(new HudBrickInspector(HudConfig.fields.get(i), this));
                    return true;
                }
                case DELETE -> {
                    pushUndo();
                    HudConfig.fields.remove(i);
                    if (selected >= HudConfig.fields.size()) selected = HudConfig.fields.size() - 1;
                    rebuild(); return true;
                }
                case BODY   -> { selected = i; return true; }
                default     -> { return false; }
            }
        }
        return false;
    }

    /** Row index under the cursor in the brick panel (clamped to the list). */
    private int rowAt(double my) {
        int py = BAR_H + 8;
        int i = (int) Math.floor((my - py) / ROW_H);
        return Math.max(0, Math.min(HudConfig.fields.size() - 1, i));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (reorderFrom >= 0 && button == 0) { reorderTo = rowAt(mouseY); return true; }
        if (dragging && button == 0 && selected >= 0 && selected < HudConfig.fields.size()) {
            HudField f = HudConfig.fields.get(selected);
            HudFieldType.Ctx c = previewCtx();
            int[] b = HudRenderer.brickBounds(this.client, f, c, width, height);
            int w = b != null ? b[2] : 8, h = b != null ? b[3] : 8;
            int nx = (int) Math.round(mouseX) - grabDX;
            int ny = (int) Math.round(mouseY) - grabDY;

            guideX = Integer.MIN_VALUE; guideY = Integer.MIN_VALUE;
            boolean snapped = false;
            if (HudConfig.snapEnabled && !hasShiftDown()) {
                List<int[]> others = new ArrayList<>();
                for (int i = 0; i < HudConfig.fields.size(); i++) {
                    if (i == selected) continue;
                    int[] ob = HudRenderer.brickBounds(this.client, HudConfig.fields.get(i), c, width, height);
                    if (ob != null) others.add(ob);
                }
                HudSnap.Result r = HudSnap.snap(nx, ny, w, h, width, height, others);
                nx = r.x; ny = r.y; guideX = r.guideX; guideY = r.guideY;
                snapped = r.engaged;
            }
            // Edge-triggered snap tick (only when snapping newly engages).
            if (snapped && !wasSnapped) playTick(1.4f);
            wasSnapped = snapped;

            // Re-anchor to the nearest corner so the offset stays small + holds across resolutions.
            HudAnchors.reanchor(f, nx, ny, w, h, width, height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && reorderFrom >= 0) {
            int to = rowAt(mouseY);
            if (to != reorderFrom && reorderFrom < HudConfig.fields.size()) {
                pushUndo();
                HudField moved = HudConfig.fields.remove(reorderFrom);
                HudConfig.fields.add(Math.max(0, Math.min(HudConfig.fields.size(), to)), moved);
                selected = HudConfig.fields.indexOf(moved);
            }
            reorderFrom = -1; reorderTo = -1;
            return true;
        }
        if (button == 0) { dragging = false; guideX = Integer.MIN_VALUE; guideY = Integer.MIN_VALUE; wasSnapped = false; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void playTick(float pitch) {
        if (client != null)
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private HudFieldType.Ctx previewCtx() {
        HudFieldType.Ctx live = HudRenderer.buildContext(this.client);
        return live.hasBlock() ? live : SAMPLE;
    }

    // ── Keyboard ────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_Z -> { if (shift) redo(); else undo(); return true; }
                case GLFW.GLFW_KEY_Y -> { redo(); return true; }
                case GLFW.GLFW_KEY_C -> { copyLayout(); return true; }
                case GLFW.GLFW_KEY_V -> { pasteLayout(); return true; }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { save(); return true; }
            }
        }
        if (key == GLFW.GLFW_KEY_SLASH) { helpOpen = !helpOpen; return true; }   // ? help
        if (key == GLFW.GLFW_KEY_TAB) { panelCollapsed = !panelCollapsed; rebuild(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { save(); return true; }
        if (selected >= 0 && selected < HudConfig.fields.size()) {
            int step = shift ? 10 : 1;
            HudField f = HudConfig.fields.get(selected);
            int[] b = HudRenderer.brickBounds(this.client, f, previewCtx(), width, height);
            if (b != null) {
                switch (key) {
                    case GLFW.GLFW_KEY_LEFT  -> { HudAnchors.place(f, b[0] - step, b[1], b[2], b[3], width, height); return true; }
                    case GLFW.GLFW_KEY_RIGHT -> { HudAnchors.place(f, b[0] + step, b[1], b[2], b[3], width, height); return true; }
                    case GLFW.GLFW_KEY_UP    -> { HudAnchors.place(f, b[0], b[1] - step, b[2], b[3], width, height); return true; }
                    case GLFW.GLFW_KEY_DOWN  -> { HudAnchors.place(f, b[0], b[1] + step, b[2], b[3], width, height); return true; }
                }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    // ── Undo / redo (in-session layout snapshots) ───────────────────────────
    private void pushUndo() {
        undoStack.push(HudConfig.snapshotFields());
        while (undoStack.size() > 50) undoStack.removeLast();
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.push(HudConfig.snapshotFields());
        HudConfig.setFields(undoStack.pop());
        selected = -1;
        rebuild();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.push(HudConfig.snapshotFields());
        HudConfig.setFields(redoStack.pop());
        selected = -1;
        rebuild();
    }

    // ── Actions ────────────────────────────────────────────────────────────
    private void save() {
        HudConfig.save();
        saveFlash = true;
        saveFlashEnd = System.currentTimeMillis() + 600;
        if (client != null && client.player != null)
            client.player.sendMessage(Text.literal("§a✔ HUD layout saved."), false);
        super.close();
    }

    /** Cancel: confirm first if the layout changed since the editor opened. */
    private void onCancel() {
        if (isDirty()) confirmingCancel = true;
        else discard();
    }

    private boolean isDirty() {
        return HudConfig.visible != origVisible
                || !HudPresetStore.encode(HudConfig.fields).equals(HudPresetStore.encode(orig));
    }

    private void discard() {
        HudConfig.setFields(orig);
        HudConfig.visible = origVisible;
        super.close();
    }

    private void copyLayout() {
        if (client == null) return;
        client.keyboard.setClipboard(HudPresetStore.encode(HudConfig.fields));
        if (client.player != null)
            client.player.sendMessage(Text.literal("§7HUD layout copied to clipboard."), false);
    }

    private void pasteLayout() {
        if (client == null) return;
        List<HudField> list = HudPresetStore.decode(client.keyboard.getClipboard());
        if (list != null) {
            pushUndo();
            HudConfig.fields.clear(); HudConfig.fields.addAll(list); selected = -1; rebuild();
            if (client.player != null) client.player.sendMessage(Text.literal("§aHUD layout pasted."), false);
        } else if (client.player != null) {
            client.player.sendMessage(Text.literal("§cClipboard isn't a valid HUD layout."), false);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    /** ESC: dismiss an open overlay first, otherwise route through cancel-confirm. */
    @Override
    public void close() {
        if (helpOpen) { helpOpen = false; return; }
        if (confirmingCancel) { confirmingCancel = false; return; }
        onCancel();
    }
}
