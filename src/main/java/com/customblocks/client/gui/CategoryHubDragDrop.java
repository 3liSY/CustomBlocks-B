/**
 * CategoryHubDragDrop.java — Group 27 Category Hub, L10 drag-and-drop state + overlay rendering. CLIENT-ONLY.
 *
 * Pick up a block row, drop it onto a left-list category row to move it there — no popup needed. Kept
 * separate from {@link CategoryHubScreen} (mouse tracking + eased hover border + drop pulse + count "+1"
 * bump are all here) so the screen stays under the 500-line size gate (CLAUDE.md §5.1). The screen still
 * owns the click-a-block-then-Move-button path as a fallback; this only adds the drag gesture on top.
 *
 * Depends on: DrawContext / TextRenderer. Called by: client/gui/CategoryHubScreen.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Environment(EnvType.CLIENT)
final class CategoryHubDragDrop {

    private static final long HOVER_EASE_MS = 150, PULSE_MS = 300, BUMP_MS = 450;

    private String blockId;
    private double startX, startY;
    private boolean active;
    private String hoverKey;
    private long hoverSinceMs;
    private String pulseKey;
    private long pulseUntil;
    private String bumpKey;
    private long bumpUntil;

    /** Call on mouse-down over a block row — arms a potential drag without committing to one yet. */
    void press(String id, double mx, double my) {
        blockId = id; startX = mx; startY = my; active = false; hoverKey = null;
    }

    /** Call from mouseDragged. {@code rowAt} maps a point to the left-list row key under it, or null. */
    boolean drag(double mx, double my, BiFunction<Double, Double, String> rowAt) {
        if (blockId == null) return false;
        if (!active && (Math.abs(mx - startX) > 4 || Math.abs(my - startY) > 4)) active = true;
        if (!active) return false;
        String hover = rowAt.apply(mx, my);
        if (!Objects.equals(hover, hoverKey)) { hoverKey = hover; hoverSinceMs = System.currentTimeMillis(); }
        return true;
    }

    /** Call from mouseReleased. {@code onDrop} fires (blockId, targetKey) only for a real drag-drop. */
    boolean release(BiConsumer<String, String> onDrop) {
        if (blockId == null) return false;
        if (active && hoverKey != null) {
            onDrop.accept(blockId, hoverKey);
            pulseKey = hoverKey; pulseUntil = System.currentTimeMillis() + PULSE_MS;
            bumpKey = hoverKey; bumpUntil = System.currentTimeMillis() + BUMP_MS;
        }
        blockId = null; active = false; hoverKey = null;
        return true;
    }

    boolean isActive() { return active; }
    String draggedId() { return blockId; }

    /** Eased lime border while hovering a valid drop target, and a fading pulse right after a drop. */
    void renderRowOverlay(DrawContext ctx, int x, int y, int w, int h, String rowKey, int accent) {
        long now = System.currentTimeMillis();
        if (active && rowKey.equals(hoverKey)) {
            float t = Math.min(1f, (now - hoverSinceMs) / (float) HOVER_EASE_MS);
            int c = (Math.round(t * 0xFF) << 24) | (accent & 0xFFFFFF);
            ctx.fill(x, y, x + w, y + 1, c);
            ctx.fill(x, y + h - 1, x + w, y + h, c);
            ctx.fill(x, y, x + 1, y + h, c);
            ctx.fill(x + w - 1, y, x + w, y + h, c);
        }
        if (pulseKey != null && rowKey.equals(pulseKey) && now < pulseUntil) {
            float t = (pulseUntil - now) / (float) PULSE_MS;
            ctx.fill(x, y, x + w, y + h, (Math.round(t * 0x55) << 24) | (accent & 0xFFFFFF));
        }
    }

    /** A floating "+1" that rises and fades over a row's block-count text right after a drop lands. */
    void renderBump(DrawContext ctx, TextRenderer tr, int x, int y, String rowKey) {
        long now = System.currentTimeMillis();
        if (bumpKey == null || !rowKey.equals(bumpKey) || now >= bumpUntil) return;
        float t = 1f - (bumpUntil - now) / (float) BUMP_MS;
        int a = Math.round((1f - t) * 0xFF);
        ctx.drawTextWithShadow(tr, Text.literal("§a+1"), x, y - Math.round(t * 6), (a << 24) | 0x40FF00);
    }

    /** A small tag that follows the cursor showing what's being dragged. */
    void renderTag(DrawContext ctx, TextRenderer tr, int mx, int my, String label, int accent) {
        if (!active) return;
        int tw = tr.getWidth(label) + 10;
        ctx.fill(mx + 12, my + 8, mx + 12 + tw, my + 22, 0xEE000000);
        ctx.fill(mx + 12, my + 8, mx + 12 + tw, my + 9, accent);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + label), mx + 16, my + 11, 0xFFFFFFFF);
    }
}
