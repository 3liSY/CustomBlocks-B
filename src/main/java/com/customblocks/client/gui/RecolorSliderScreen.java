/**
 * RecolorSliderScreen.java — Group 10 (live recolour slider). CLIENT-SIDE ONLY.
 *
 * Responsibility: drag Hue / Saturation / Lightness and watch a live preview of the block's
 * texture update in real time, then click Apply to commit. The preview is built client-side from
 * the block's current texture (fetched once over the mod's HTTP server, downsampled to a small
 * cell grid so re-shading every frame is cheap); Apply sends a RecolorApplyPayload and the SERVER
 * bakes the real texture — the client never mutates server state (CLAUDE.md §5.8).
 *
 * Depends on: Screen/DrawContext/ButtonWidget, ColorMath (same HSL maths as the server commit),
 *             RecolorApplyPayload, ClientPlayNetworking.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=RECOLOR_SLIDER).
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;

@Environment(EnvType.CLIENT)
public class RecolorSliderScreen extends Screen {

    private static final int GRID = 32;   // preview resolution (cells per side)
    private static final int BOX  = 128;  // preview box size in px

    private final String id;
    private final String texUrl;

    // Downsampled source: packed 0xAARRGGBB per cell (alpha 0 = transparent padding).
    private volatile int[] grid;
    private volatile boolean failed;

    private final Slider hue   = new Slider("Hue",        -180, 180,   0);
    private final Slider sat   = new Slider("Saturation",    0, 200, 100);
    private final Slider light = new Slider("Lightness",     0, 200, 100);
    private Slider dragging;

    public RecolorSliderScreen(String id, String texUrl) {
        super(Text.literal("Live Recolour"));
        this.id = id;
        this.texUrl = texUrl;
    }

    @Override
    protected void init() {
        layoutSliders();
        int cx = width / 2;
        int row = height - 36;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> { hue.value = 0; sat.value = 100; light.value = 100; })
                .dimensions(cx - 154, row, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§aApply"), b -> apply())
                .dimensions(cx - 40, row, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(cx + 74, row, 80, 20).build());
        if (grid == null && !failed) fetchAsync();
    }

    private void layoutSliders() {
        int cx = width / 2;
        int w = 220, x = cx - w / 2, y = height / 2 + 4;
        hue.set(x, y, w);
        sat.set(x, y + 34, w);
        light.set(x, y + 68, w);
    }

    private void fetchAsync() {
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(URI.create(texUrl).toURL());
                if (img == null) { failed = true; return; }
                int[] g = new int[GRID * GRID];
                int w = img.getWidth(), h = img.getHeight();
                for (int gy = 0; gy < GRID; gy++) {
                    for (int gx = 0; gx < GRID; gx++) {
                        int sx = Math.min(w - 1, gx * w / GRID);
                        int sy = Math.min(h - 1, gy * h / GRID);
                        g[gy * GRID + gx] = img.getRGB(sx, sy); // 0xAARRGGBB
                    }
                }
                grid = g;
            } catch (Exception e) {
                failed = true;
            }
        }, "CustomBlocks-RecolorFetch");
        t.setDaemon(true);
        t.start();
    }

    private void apply() {
        ClientPlayNetworking.send(new RecolorApplyPayload(
                id, (float) hue.value, (float) (sat.value / 100.0), (float) (light.value / 100.0)));
        close();
    }

    /** Cancel / Esc / Apply all route through here — ask the server to reopen the menu we came from. */
    @Override
    public void close() {
        ClientPlayNetworking.send(new GuiBackPayload());
        super.close();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // keep the world visible; we add our own dim in render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xC0101010);
        super.render(ctx, mx, my, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§lLive Recolour §7— " + id), width / 2, 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Drag the bars · the preview updates live · Apply to keep"), width / 2, 28, 0xFFFFFFFF);

        drawPreview(ctx);
        hue.render(ctx, valueLabel(hue, "°"));
        sat.render(ctx, valueLabel(sat, "%"));
        light.render(ctx, valueLabel(light, "%"));
    }

    private void drawPreview(DrawContext ctx) {
        int bx = (width - BOX) / 2, by = height / 2 - 8 - BOX;
        ctx.fill(bx - 2, by - 2, bx + BOX + 2, by + BOX + 2, 0xFF555555); // frame
        int[] g = grid;
        if (g == null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(failed ? "§7(preview unavailable — Apply still works)" : "§8loading preview…"),
                    width / 2, by + BOX / 2, 0xFFFFFFFF);
            return;
        }
        int cell = BOX / GRID;
        double dh = hue.value, ds = sat.value / 100.0, dl = light.value / 100.0;
        for (int gy = 0; gy < GRID; gy++) {
            for (int gx = 0; gx < GRID; gx++) {
                int argb = g[gy * GRID + gx];
                int a = argb >>> 24;
                int px = bx + gx * cell, py = by + gy * cell;
                if (a == 0) { // checker behind transparent padding
                    ctx.fill(px, py, px + cell, py + cell, ((gx + gy) % 2 == 0) ? 0xFF2A2A2A : 0xFF1F1F1F);
                    continue;
                }
                int shifted = ColorMath.hslShiftRgb(argb & 0xFFFFFF, dh, ds, dl);
                ctx.fill(px, py, px + cell, py + cell, 0xFF000000 | shifted);
            }
        }
    }

    private String valueLabel(Slider s, String unit) {
        return s.label + ": " + (int) Math.round(s.value) + unit;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        for (Slider s : new Slider[]{hue, sat, light}) {
            if (s.hit(mx, my)) { dragging = s; s.setFromX(mx); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { dragging.setFromX(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    /** A simple horizontal drag bar. */
    private final class Slider {
        final String label;
        final double min, max;
        double value;
        int x, y, w;
        Slider(String label, double min, double max, double value) {
            this.label = label; this.min = min; this.max = max; this.value = value;
        }
        void set(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }
        boolean hit(double mx, double my) { return mx >= x && mx <= x + w && my >= y - 2 && my <= y + 12; }
        void setFromX(double mx) {
            double f = Math.max(0, Math.min(1, (mx - x) / w));
            value = min + f * (max - min);
        }
        void render(DrawContext ctx, String text) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§f" + text), x, y - 12, 0xFFFFFFFF);
            ctx.fill(x, y + 3, x + w, y + 7, 0xFF333333);                 // track
            double f = (value - min) / (max - min);
            int knob = x + (int) Math.round(f * w);
            ctx.fill(knob - 2, y - 1, knob + 2, y + 11, 0xFFEEEEEE);      // knob
        }
    }
}
