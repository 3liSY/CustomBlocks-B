/**
 * BuzzerPanelScreen.java — Group 31 item 4 (admin panel Screen). CLIENT-SIDE ONLY.
 *
 * The branded red/black control panel opened by right-clicking a BuzzerGame admin panel (op-only). It is a
 * thin view over the server session: it renders the snapshot string it was handed and, on every button,
 * sends a {@link BuzzerPanelActionPayload} back; the server applies it and returns a fresh snapshot, which
 * arrives as another OpenGuiPayload and calls {@link #refresh(String)} — so the screen never holds
 * authoritative state (CLAUDE.md §5.8). Layout:
 *   - Control row : Start · Stop · Reset (click-again to confirm on a live round) · Reveal (lit at Results).
 *   - Settings    : Mode · Format · Countdown · False-start · Target (cycle or type-exact) — greyed off Idle.
 *   - Link list   : Buzzers ▸ N → expand → per-buzzer click-again unlink.
 *
 * Snapshot format (see PanelSession.guiSnapshot):
 *   pos|state|mode|format|target|countdown|falseStart|buzzers|screens|canReveal|idle
 *
 * Depends on: CbTheme, BuzzerPanelActionPayload, ClientPlayNetworking, Screen widgets
 * Called by:  CustomBlocksClient (OpenGuiPayload mode=BUZZER_PANEL → open or refresh)
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.BuzzerPanelActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class BuzzerPanelScreen extends Screen {

    private static final int BACKDROP = 0x33000000;
    private static final int BAR_H = 42;
    private static final int PANEL_W = 320;

    // parsed snapshot
    private long pos;
    private String state = "", mode = "", format = "", target = "", countdown = "", falseStart = "";
    private int buzzers, screens;
    private boolean canReveal, idle;

    // client-only interaction state
    private boolean resetArmed;
    private boolean showLinks;
    private boolean typingTarget;
    private int armedUnlink = -1;
    private TextFieldWidget targetField;

    public BuzzerPanelScreen(String data) {
        super(Text.literal("BuzzerGame Panel"));
        parse(data);
    }

    /** Re-render in place from a fresh server snapshot (keeps the screen open). */
    public void refresh(String data) {
        parse(data);
        resetArmed = false;
        armedUnlink = -1;
        if (client != null) rebuild();
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    // ------------------------------------------------------------------ parse

    private void parse(String data) {
        String[] f = data == null ? new String[0] : data.split("\\|", -1);
        pos        = parseLong(get(f, 0));
        state      = get(f, 1);
        mode       = get(f, 2);
        format     = get(f, 3);
        target     = get(f, 4);
        countdown  = get(f, 5);
        falseStart = get(f, 6);
        buzzers    = parseInt(get(f, 7));
        screens    = parseInt(get(f, 8));
        canReveal  = "1".equals(get(f, 9));
        idle       = "1".equals(get(f, 10));
    }

    private static String get(String[] a, int i) { return i < a.length ? a[i] : ""; }
    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
    private static int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - PANEL_W / 2;
        int gap = 6, h = 20;
        int y = 92;

        // Control row — Start · Stop · Reset · Reveal
        int c4 = (PANEL_W - 3 * gap) / 4;
        ButtonWidget start = add("Start", left, y, c4, h, b -> send(BuzzerPanelActionPayload.START, 0));
        start.active = idle;
        if (!idle) start.setTooltip(Tooltip.of(Text.literal("Reset to Idle to start a new round.")));
        add("Stop", left + (c4 + gap), y, c4, h, b -> send(BuzzerPanelActionPayload.STOP, 0));
        add(resetArmed ? "§eConfirm?" : "Reset", left + 2 * (c4 + gap), y, c4, h, b -> onReset());
        ButtonWidget reveal = add("Reveal", left + 3 * (c4 + gap), y, c4, h, b -> send(BuzzerPanelActionPayload.REVEAL, 0));
        reveal.active = canReveal;
        if (!canReveal) reveal.setTooltip(Tooltip.of(Text.literal("Reveal lights up once everyone has buzzed.")));

        // Settings — greyed off Idle
        int c2 = (PANEL_W - gap) / 2;
        y += h + gap;
        ButtonWidget modeB = add("Mode: " + mode, left, y, c2, h, b -> send(BuzzerPanelActionPayload.MODE, 0));
        ButtonWidget fmtB = add("Format: " + format, left + (c2 + gap), y, c2, h, b -> send(BuzzerPanelActionPayload.FORMAT, 0));
        y += h + gap;
        ButtonWidget cdB = add("Countdown: " + countdown, left, y, c2, h, b -> send(BuzzerPanelActionPayload.COUNTDOWN, 0));
        ButtonWidget fsB = add("False-start: " + falseStart, left + (c2 + gap), y, c2, h, b -> send(BuzzerPanelActionPayload.FALSESTART, 0));
        y += h + gap;
        ButtonWidget tgt = add("Target: " + target, left, y, c2, h, b -> send(BuzzerPanelActionPayload.TARGET_PRESET, 0));
        for (ButtonWidget s : new ButtonWidget[]{modeB, fmtB, cdB, fsB, tgt}) {
            s.active = idle;
            if (!idle) s.setTooltip(Tooltip.of(Text.literal("Already running — stop or reset first.")));
        }
        if (!typingTarget) {
            ButtonWidget typeBtn = add("Type exact…", left + (c2 + gap), y, c2, h, b -> { typingTarget = true; rebuild(); });
            typeBtn.active = idle;
        } else {
            targetField = new TextFieldWidget(textRenderer, left + (c2 + gap), y, c2 - 26, h, Text.literal("seconds"));
            targetField.setText(target.replace("s", ""));
            addDrawableChild(targetField);
            add("OK", left + (c2 + gap) + c2 - 24, y, 24, h, b -> submitTarget());
            setInitialFocus(targetField);
        }

        // Link list
        y += h + gap + 4;
        add((showLinks ? "Buzzers ▾ " : "Buzzers ▸ ") + buzzers, left, y, c2, h,
                b -> { showLinks = !showLinks; armedUnlink = -1; rebuild(); });
        ButtonWidget scr = add("Screens: " + screens, left + (c2 + gap), y, c2, h, b -> {});
        scr.active = false; // info only
        if (showLinks) {
            y += h + gap;
            for (int i = 1; i <= buzzers; i++) {
                final int idx = i;
                String lbl = (armedUnlink == idx) ? "§eUnlink #" + idx + "?  " : "Buzzer #" + idx + "  §7(click to unlink)";
                ButtonWidget bb = add(lbl, left, y, PANEL_W, h, b -> onUnlink(idx));
                bb.active = idle;
                if (!idle) bb.setTooltip(Tooltip.of(Text.literal("Unlink only while Idle.")));
                y += h + 2;
            }
        }

        // Close
        add("Close", cx - 40, height - BAR_H + 11, 80, h, b -> close());
    }

    private ButtonWidget add(String text, int x, int y, int w, int h, ButtonWidget.PressAction a) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(text), a).dimensions(x, y, w, h).build();
        addDrawableChild(b);
        return b;
    }

    // ------------------------------------------------------------------ actions

    private void send(int action, double arg) {
        ClientPlayNetworking.send(new BuzzerPanelActionPayload(pos, action, arg));
        // The server replies with a fresh snapshot (OpenGuiPayload) → refresh() rebuilds the screen.
    }

    private void onReset() {
        if (idle || "Finished".equalsIgnoreCase(state)) {
            send(BuzzerPanelActionPayload.RESET, 0);           // nothing to lose → instant
        } else if (!resetArmed) {
            resetArmed = true;
            rebuild();                                          // arm: relabel "Confirm?"
        } else {
            resetArmed = false;
            send(BuzzerPanelActionPayload.RESET, 0);            // confirmed
        }
    }

    private void onUnlink(int idx) {
        if (armedUnlink != idx) {
            armedUnlink = idx;
            rebuild();
        } else {
            armedUnlink = -1;
            send(BuzzerPanelActionPayload.UNLINK, idx);
        }
    }

    private void submitTarget() {
        try {
            double v = Double.parseDouble(targetField.getText().trim());
            typingTarget = false;
            send(BuzzerPanelActionPayload.TARGET_SET, v);
        } catch (NumberFormatException ignored) {
            // leave the field open for a correct value
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (typingTarget && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            submitTarget();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // world stays visible; our dim is drawn in render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, BACKDROP);

        // title bar
        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("BuzzerGame Panel", state), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7op-only · settings apply only while Idle"), 8, 24, 0xFFFFFFFF);

        // status readout
        int left = width / 2 - PANEL_W / 2;
        ctx.drawTextWithShadow(textRenderer, Text.literal("§fMode §7" + mode + "  §f· Format §7" + format), left, 52, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§fTarget §7" + target + "  §f· Countdown §7" + countdown + "  §f· False-start §7" + falseStart), left, 64, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§fLinked §7" + buzzers + " buzzer(s), " + screens + " screen(s)"), left, 76, 0xFFFFFFFF);

        // bottom bar
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, CbTheme.BAR_BG);
        ctx.fill(0, barY, width, barY + 1, CbTheme.ACCENT);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
