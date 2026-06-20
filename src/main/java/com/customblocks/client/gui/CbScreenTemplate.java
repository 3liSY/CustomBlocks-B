/**
 * CbScreenTemplate.java — GROUP 27 DESIGN STANDARD. CLIENT-SIDE ONLY.
 *
 * THIS IS A TEMPLATE, NOT A REAL SCREEN. Copy this file, rename it, and fill in the
 * TODOs. Do not register or reference this class anywhere in the mod.
 *
 * Every CB Screen must follow this standard:
 *   - Backdrop: 0x33000000 (world always visible behind the screen)
 *   - Title bar: dark strip at top with thin §6-gold bottom border
 *   - Title format: "§6§lScreen Name §7— §fcontext" (no context = just "§6§lScreen Name")
 *   - Hint line 1: §7 screen-specific controls, · separator
 *   - Hint line 2: §8 universal shortcuts (always the same)
 *   - Bottom action bar: dark strip with thin §6-gold top border
 *   - Button order: [Undo] [Redo] [Rand] ··· [§aPrimary] ··· [Copy] [Reset] [Cancel]
 *   - Primary button: §a green text, always centered
 *   - Cancel with unsaved changes: in-screen confirmation overlay
 *   - Save feedback: primary button flashes green + professional chat message
 *   - [?] button: top-right of title bar → in-screen shortcut overlay
 *   - Undo history: persists per block per screen type (disk)
 *
 * Universal keyboard shortcuts (implement all that apply):
 *   Ctrl+Z         → undo
 *   Ctrl+Y         → redo
 *   Ctrl+Shift+Z   → redo (alternate)
 *   Ctrl+C         → copy settings to OS clipboard (silent) + print code in chat
 *   Ctrl+V         → paste settings from OS clipboard
 *   Ctrl+R         → randomize (if screen supports it)
 *   Enter          → primary action (save / apply / create)
 *   Esc            → cancel (confirm dialog if unsaved changes)
 *   R              → reset view (3D-cube screens only)
 *   ? (key)        → open shortcut help overlay
 *
 * Authority: GROUP_27_SCREENS.md. See also ArabicPreviewScreen for the proven 3D cube
 * renderer pattern (MatrixStack cell-grid, drag-rotate, zoom, spin, face shading).
 *
 * Depends on: Screen, DrawContext, ButtonWidget, ClientPlayNetworking
 * Called by: CustomBlocksClient (OpenGuiPayload mode=TODO_YOUR_MODE)
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class CbScreenTemplate extends Screen {

    // ── Standard colours ─────────────────────────────────────────────────────
    /** World-behind dim — applied over the full screen so world stays visible. */
    private static final int BACKDROP   = 0x33000000;
    /** Title bar + bottom action bar background. */
    private static final int BAR_BG     = 0xAA000000;
    /** §6 gold as an ARGB int — used for the 1-pixel border lines. */
    private static final int GOLD       = 0xFF_FF_AA_00;
    /** Height of the title bar and bottom bar in pixels. */
    private static final int BAR_H      = 42;

    // ── State ─────────────────────────────────────────────────────────────────
    /** TODO: add your screen's fields here. */
    private boolean dirty = false;          // true once the player has made a change
    private boolean confirmingCancel = false; // true while the "Discard changes?" overlay is open
    private boolean saveFlash = false;      // true during the brief green button flash
    private long    saveFlashEnd = 0;

    // ── Buttons whose labels change at runtime ────────────────────────────────
    private ButtonWidget primaryBtn;   // the §aSave / §aApply / §aCreate button

    // TODO: replace "TODO_SCREEN_NAME" with your screen's display name
    private static final String SCREEN_NAME = "TODO_SCREEN_NAME";
    // TODO: set to the block ID or empty string if this screen has no block context
    private final String contextId;

    public CbScreenTemplate(String contextId) {
        super(Text.literal(SCREEN_NAME));
        this.contextId = contextId;
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        int cx = width / 2;
        int barY = height - BAR_H;   // top of bottom action bar

        // Undo / Redo / Rand (left group)
        addDrawableChild(ButtonWidget.builder(Text.literal("Undo"),  b -> undo())
                .dimensions(8, barY + 11, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Redo"),  b -> redo())
                .dimensions(56, barY + 11, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Rand"),  b -> randomize())
                .dimensions(104, barY + 11, 44, 20).build());

        // Primary action (center, §a green)
        primaryBtn = ButtonWidget.builder(Text.literal("§aSave"), b -> primary())  // TODO: label
                .dimensions(cx - 44, barY + 11, 88, 20).build();
        addDrawableChild(primaryBtn);

        // Copy / Reset / Cancel (right group)
        int right = width - 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> cancel())
                .dimensions(right - 66, barY + 11, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"),  b -> reset())
                .dimensions(right - 136, barY + 11, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Copy"),   b -> copy())
                .dimensions(right - 206, barY + 11, 66, 20).build());

        // [?] help button — top-right of title bar
        addDrawableChild(ButtonWidget.builder(Text.literal("?"), b -> openHelp())
                .dimensions(width - 24, 10, 16, 16).build());

        // TODO: add your screen's content widgets here
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Intentionally empty — world stays visible; we draw our own backdrop in render().
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // 1. World-behind dim
        ctx.fill(0, 0, width, height, BACKDROP);

        // 2. Title bar
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, GOLD);   // gold bottom border

        String title = contextId.isEmpty()
                ? "§6§l" + SCREEN_NAME
                : "§6§l" + SCREEN_NAME + " §7— §f" + contextId;
        ctx.drawTextWithShadow(textRenderer, Text.literal(title), 8, 10, 0xFFFFFFFF);
        // Hint line 1 — TODO: replace with your screen's specific controls
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7TODO: drag to rotate · scroll = spin · R = reset"),
                8, 22, 0xFFFFFFFF);
        // Hint line 2 — universal shortcuts (do not change)
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Ctrl+Z undo · Ctrl+C copy · Enter confirm · ? help"),
                8, 32, 0xFFFFFFFF);

        // 3. Bottom action bar
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, BAR_BG);
        ctx.fill(0, barY, width, barY + 1, GOLD);      // gold top border

        // 4. Main content — TODO: draw your screen's content here
        super.render(ctx, mx, my, delta);               // draws widgets

        // 5. Save flash — briefly tint the primary button area green
        if (saveFlash) {
            if (System.currentTimeMillis() < saveFlashEnd) {
                ctx.fill(width / 2 - 44, barY + 11, width / 2 + 44, barY + 31, 0x4400FF44);
            } else {
                saveFlash = false;
            }
        }

        // 6. Cancel confirmation overlay
        if (confirmingCancel) {
            renderCancelConfirm(ctx, mx, my);
        }
    }

    /** Renders the "Discard changes?" in-screen overlay. */
    private void renderCancelConfirm(DrawContext ctx, int mx, int my) {
        int cx = width / 2, cy = height / 2;
        int pw = 200, ph = 70;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, GOLD);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, 0xFF1A1A1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fDiscard changes?"), cx, cy - 18, 0xFFFFFFFF);
        // Buttons are added/removed dynamically — see cancel() and discardConfirmed()
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    /** TODO: implement your primary action (save to server, close, etc.). */
    private void primary() {
        // TODO: send payload to server
        saveFlash = true;
        saveFlashEnd = System.currentTimeMillis() + 600;
        // TODO: send chat message on success (professional, friendly tone)
        // ChatHelper.send(player, "§a✔ Saved: " + contextId);
        dirty = false;
        super.close();
    }

    private void cancel() {
        if (dirty) {
            confirmingCancel = true;
            addCancelOverlayButtons();
        } else {
            doClose();
        }
    }

    private void addCancelOverlayButtons() {
        int cx = width / 2, cy = height / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("§cYes, discard"), b -> discardConfirmed())
                .dimensions(cx - 96, cy + 2, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Keep editing"), b -> keepEditing())
                .dimensions(cx + 8, cy + 2, 88, 20).build());
    }

    private void discardConfirmed() {
        dirty = false;
        doClose();
    }

    private void keepEditing() {
        confirmingCancel = false;
        clearChildren();
        init(); // re-add normal widgets
    }

    /** Override to send GuiBackPayload or do other cleanup before closing. */
    protected void doClose() {
        super.close();
    }

    @Override
    public void close() {
        cancel(); // always route through cancel so confirmation fires if dirty
    }

    /** TODO: implement undo. */
    private void undo()       { /* pop from per-block undo stack */ }
    /** TODO: implement redo. */
    private void redo()       { /* push back onto per-block undo stack */ }
    /** TODO: implement randomize if applicable, or remove the button. */
    private void randomize()  { dirty = true; }
    /** TODO: implement copy settings to clipboard + chat. */
    private void copy()       { /* encode state → clipboard + ChatHelper */ }
    /** TODO: implement reset to defaults. */
    private void reset()      { dirty = true; }

    private void openHelp() {
        // TODO: implement in-screen shortcut overlay listing all keys for this screen
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl  = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT)   != 0;

        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_Z -> { if (shift) { redo(); } else { undo(); } return true; }
                case GLFW.GLFW_KEY_Y -> { redo();       return true; }
                case GLFW.GLFW_KEY_C -> { copy();       return true; }
                case GLFW.GLFW_KEY_V -> { paste();      return true; }
                case GLFW.GLFW_KEY_R -> { randomize();  return true; }
            }
        }
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { primary(); return true; }
            case GLFW.GLFW_KEY_R                              -> { resetView(); return true; } // 3D screens only
            case GLFW.GLFW_KEY_SLASH                          -> { openHelp();  return true; } // ? key
        }
        return super.keyPressed(key, scan, mods);
    }

    /** TODO: implement paste from clipboard. */
    private void paste()     { dirty = true; }
    /** TODO: implement reset view (3D-cube screens only; delete this for non-cube screens). */
    private void resetView() { }

    @Override
    public boolean shouldPause() { return false; } // keep the world ticking behind the screen
}
