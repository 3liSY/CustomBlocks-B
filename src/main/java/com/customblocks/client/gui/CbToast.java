/**
 * CbToast.java — Group 27 §G27.13 real top-right toast. CLIENT-SIDE ONLY.
 *
 * Responsibility: the ONE on-screen feedback box for every CB client Screen. Replaces the old
 * "toast" helpers that secretly sent chat messages (sendMessage(..., false)). A vanilla
 * {@link Toast} added via the ToastManager, so it renders top-right, stacks, auto-fades and works
 * while a screen is open. Styled to the locked red+black palette; the accent edge tells the kind:
 * lime = success (only success — locked palette rule), red = error, grey = info.
 *
 * Single entry point: {@code CbToast.show(text, Kind)}. Server command replies stay in chat —
 * this is only for feedback raised from inside an open Screen (§G27.13 scope).
 *
 * Depends on: ToastManager/Toast, CbTheme.
 * Called by: all Group 27 screens + their popups.
 */
package com.customblocks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class CbToast implements Toast {

    public enum Kind { SUCCESS, INFO, ERROR }

    private static final long LIFE_MS = 3000;
    private static final int PAD = 8, MIN_W = 140, MAX_W = 230, LINE_H = 11;

    private final String text;
    private final Kind kind;
    private java.util.List<net.minecraft.text.OrderedText> lines;
    private int w = MIN_W, h = 24;

    private CbToast(String text, Kind kind) {
        this.text = text;
        this.kind = kind;
    }

    /** The single entry point — queue a toast (safe to call from any screen code on the client thread). */
    public static void show(String text, Kind kind) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.getToastManager().add(new CbToast(text, kind));
    }

    public static void success(String text) { show(text, Kind.SUCCESS); }
    public static void info(String text)    { show(text, Kind.INFO); }
    public static void error(String text)   { show(text, Kind.ERROR); }

    private void layout(MinecraftClient mc) {
        if (lines != null) return;
        int textW = Math.max(MIN_W - 2 * PAD, Math.min(MAX_W - 2 * PAD, mc.textRenderer.getWidth(text)));
        lines = mc.textRenderer.wrapLines(Text.literal(text), textW);
        int widest = 0;
        for (net.minecraft.text.OrderedText l : lines) widest = Math.max(widest, mc.textRenderer.getWidth(l));
        w = Math.max(MIN_W, widest + 2 * PAD);
        h = Math.max(24, lines.size() * LINE_H + 2 * PAD - 2);
    }

    @Override
    public int getWidth() { return w; }

    @Override
    public int getHeight() { return h; }

    @Override
    public Visibility draw(DrawContext ctx, ToastManager manager, long startTime) {
        MinecraftClient mc = manager.getClient();
        layout(mc);
        int edge = switch (kind) {
            case SUCCESS -> CbTheme.LIME;    // lime = success ONLY (locked palette)
            case ERROR   -> CbTheme.ACCENT;  // red
            case INFO    -> 0xFF777777;      // neutral grey
        };
        // Black card + kind-coloured border + thicker left edge.
        ctx.fill(0, 0, w, h, 0xF2000000);
        ctx.fill(0, 0, w, 1, edge);
        ctx.fill(0, h - 1, w, h, edge);
        ctx.fill(w - 1, 0, w, h, edge);
        ctx.fill(0, 0, 2, h, edge);
        int y = PAD - 1;
        for (net.minecraft.text.OrderedText l : lines) {
            ctx.drawTextWithShadow(mc.textRenderer, l, PAD, y, 0xFFFFFFFF);
            y += LINE_H;
        }
        return startTime >= LIFE_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
