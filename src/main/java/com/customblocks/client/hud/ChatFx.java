/**
 * ChatFx.java — GROUP 04 §G04-4 chat click-feedback. CLIENT-SIDE ONLY.
 *
 * A sent chat line physically cannot re-render (vanilla exposes no way to edit a message once sent —
 * the same limit that killed "live-updating chat"). So a chip click is confirmed NOT by changing the
 * line, but by a burst of feedback drawn in the CustomBlocks overlay layer we own (never in vanilla
 * chat). Owner-locked 2026-07-15, the click-feedback quartet:
 *
 *   1. A distinct vanilla sound per action. UNDO / REDO / EDIT / VIEW already sound from the SERVER
 *      command (SoundFx.chip — same whether typed or clicked), so here we only add the ONE the server
 *      can't hook: COPY (a COPY_TO_CLIPBOARD chip runs no command). No new assets → no soundGate risk.
 *   2. A particle burst at the cursor — green sparks for a normal action, red for a destructive one.
 *   3. A screen-edge flash / shake — reuses {@link CbFlash} (green pulse / red for destructive).
 *   4. A floating result toast ("Undone" / "Copied") that drifts up from the click point and fades.
 *
 * Trigger: {@link ChatClickFxMixin} forwards every clicked {@link Style} here (HEAD inject, never
 * cancels), so vanilla still runs the command / copies the text exactly as before. We add feedback;
 * we change nothing about how the click behaves.
 *
 * Depends on: CbFlash, CbTheme. Called by: ChatClickFxMixin (trigger) · CbOverlay (draw).
 */
package com.customblocks.client.hud;

import com.customblocks.client.gui.CbTheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class ChatFx {

    private ChatFx() {} // static-only

    /** The chip actions we recognise from a clicked chip's ClickEvent. */
    private enum Action {
        RUN("Ran", false), UNDO("Undone", false), REDO("Redone", false), EDIT("Edit", false),
        COPY("Copied", false), CONFIRM("Confirmed", false), CANCEL("Cancelled", false),
        DELETE("Deleted", true);

        final String toast;
        final boolean destructive;
        Action(String toast, boolean destructive) { this.toast = toast; this.destructive = destructive; }
    }

    // ── transient effects, bounded so a mash of clicks can never grow unbounded ──
    private record Spark(float x, float y, float vx, float vy, long born, int color) {}
    private record Toast(String text, float x, float y, long born, int color) {}

    private static final long SPARK_MS = 520L;
    private static final long TOAST_MS = 1100L;
    private static final int  MAX_SPARKS = 120;

    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final List<Toast> TOASTS = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════════════════
    //  Trigger — a chip was clicked
    // ══════════════════════════════════════════════════════════════════════════

    /** Called from the click mixin for EVERY clicked style. Only CustomBlocks chips seed feedback. */
    public static void onTextClick(Style style) {
        if (style == null) return;
        ClickEvent ev = style.getClickEvent();
        Action action = classify(ev);
        if (action == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        // Cursor position in GUI-scaled coordinates (the space DrawContext draws in).
        double scaleX = (double) mc.getWindow().getScaledWidth()  / Math.max(1, mc.getWindow().getWidth());
        double scaleY = (double) mc.getWindow().getScaledHeight() / Math.max(1, mc.getWindow().getHeight());
        float sx = (float) (mc.mouse.getX() * scaleX);
        float sy = (float) (mc.mouse.getY() * scaleY);

        int color = action.destructive ? CbTheme.ACCENT : CbTheme.LIME;

        // (3) edge flash / shake — green pulse, or red for a destructive action.
        CbFlash.play(action.destructive);
        // (2) particle burst at the cursor.
        burst(sx, sy, color);
        // (4) floating result toast from the click point.
        seedToast(action.toast, sx, sy, color);
        // (1) the one sound the server can't hook: a COPY chip runs no command.
        if (action == Action.COPY && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, 1.4f, 0.7f));
        }
    }

    /**
     * Map a clicked chip's ClickEvent to an Action, or null if it is not a CustomBlocks chip. A CB chip
     * is a COPY_TO_CLIPBOARD (the ⇪ Copy / share chip) or a RUN_COMMAND of a {@code /cb …} command.
     */
    private static Action classify(ClickEvent ev) {
        if (ev == null) return null;
        if (ev.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) return Action.COPY;
        if (ev.getAction() != ClickEvent.Action.RUN_COMMAND) return null;

        String cmd = ev.getValue() == null ? "" : ev.getValue().trim().toLowerCase(Locale.ROOT);
        if (!cmd.startsWith("/cb ") && !cmd.equals("/cb")) return null;
        String rest = cmd.length() > 4 ? cmd.substring(4).trim() : "";

        if (rest.startsWith("undo"))   return Action.UNDO;
        if (rest.startsWith("redo"))   return Action.REDO;
        if (rest.startsWith("edit"))   return Action.EDIT;
        if (rest.startsWith("cancel")) return Action.CANCEL;
        if (rest.contains("delete") || rest.startsWith("reset") || rest.startsWith("trash")) return Action.DELETE;
        if (rest.startsWith("confirm")) return Action.CONFIRM;
        return Action.RUN;
    }

    private static void burst(float x, float y, int color) {
        java.util.Random r = new java.util.Random();
        int n = 9;
        for (int i = 0; i < n; i++) {
            double ang = (Math.PI * 2 * i / n) + r.nextGaussian() * 0.2;
            float speed = 0.9f + r.nextFloat() * 1.4f;
            float vx = (float) Math.cos(ang) * speed;
            float vy = (float) Math.sin(ang) * speed - 0.6f;   // bias upward
            SPARKS.add(new Spark(x, y, vx, vy, System.currentTimeMillis(), color));
        }
        while (SPARKS.size() > MAX_SPARKS) SPARKS.remove(0);
    }

    private static void seedToast(String text, float x, float y, int color) {
        TOASTS.add(new Toast(text, x, y, System.currentTimeMillis(), color));
        while (TOASTS.size() > 8) TOASTS.remove(0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Draw — called every frame from CbOverlay
    // ══════════════════════════════════════════════════════════════════════════

    public static void render(DrawContext ctx, MinecraftClient mc) {
        long now = System.currentTimeMillis();
        renderSparks(ctx, now);
        renderToasts(ctx, mc, now);
    }

    private static void renderSparks(DrawContext ctx, long now) {
        for (int i = SPARKS.size() - 1; i >= 0; i--) {
            Spark s = SPARKS.get(i);
            long age = now - s.born();
            if (age > SPARK_MS) { SPARKS.remove(i); continue; }
            float t = age / (float) SPARK_MS;                    // 0 → 1
            float fade = 1f - t;
            // simple gravity-less drift; velocity is in px/frame-ish, scaled by age
            float px = s.x() + s.vx() * age * 0.06f;
            float py = s.y() + s.vy() * age * 0.06f + 0.010f * age * t; // slight sag as it ages
            int alpha = (int) (220 * fade) & 0xFF;
            int col = (alpha << 24) | (s.color() & 0x00FFFFFF);
            int sz = fade > 0.5f ? 2 : 1;
            int ix = Math.round(px), iy = Math.round(py);
            ctx.fill(ix, iy, ix + sz, iy + sz, col);
        }
    }

    private static void renderToasts(DrawContext ctx, MinecraftClient mc, long now) {
        if (mc.textRenderer == null) return;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast tst = TOASTS.get(i);
            long age = now - tst.born();
            if (age > TOAST_MS) { TOASTS.remove(i); continue; }
            float t = age / (float) TOAST_MS;                    // 0 → 1
            float fade = t < 0.15f ? (t / 0.15f) : (1f - (t - 0.15f) / 0.85f); // pop-in then fade
            fade = MathHelper.clamp(fade, 0f, 1f);
            int alpha = (int) (255 * fade) & 0xFF;

            int w = mc.textRenderer.getWidth(tst.text());
            int rise = (int) (18 * t);                           // drifts up
            int x = Math.round(tst.x()) + 6;
            int y = Math.round(tst.y()) - 10 - rise;

            // frosted pill: translucent black plate + a thin coloured edge
            int plate = ((int) (170 * fade) & 0xFF) << 24;       // black, fading
            int edge  = (alpha << 24) | (tst.color() & 0x00FFFFFF);
            ctx.fill(x - 3, y - 2, x + w + 3, y + 10, plate);
            ctx.fill(x - 3, y - 2, x + w + 3, y - 1, edge);      // top hairline
            ctx.fill(x - 3, y + 9, x + w + 3, y + 10, edge);     // bottom hairline
            int textCol = (alpha << 24) | (tst.color() & 0x00FFFFFF);
            ctx.drawText(mc.textRenderer, tst.text(), x, y, textCol, true);
        }
    }
}
