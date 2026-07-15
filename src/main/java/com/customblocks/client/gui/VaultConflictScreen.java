/**
 * VaultConflictScreen.java — Group 20 §S2 (Cloud Vault conflict screen). CLIENT-SIDE ONLY.
 *
 * Opened when {@code /cb vault download <code>} restores a block whose id already exists locally. Shows
 * BOTH blocks as side-by-side spinning 3D cubes — YOURS (left) vs INCOMING (right) — with their stats and
 * a Simple⇄Advanced toggle, an editable id box pre-filled with the next free id, and four actions:
 *   Override     — incoming takes the id, the local block is deleted (undoable).
 *   Keep Both    — incoming imported under the typed id; local untouched.
 *   Rename Mine  — local re-id'd to the typed id; incoming takes the original id.
 *   Cancel / ESC — nothing.
 * The player only states intent: each button sends a {@link VaultResolvePayload}; the server re-fetches the
 * ZIP by code, re-validates, applies, rebuilds the pack once, and records to UndoManager (§5.8 authoritative).
 *
 * Incoming frames ride the open packet as a tiny preview strip ({@link StudioTextureLoader#framesFromPng});
 * the local block's frames are read back from the active resource pack ({@code idx=} in the meta). Null frames
 * on either side fall back to a grey cube.
 *
 * Depends on: Screen/DrawContext/TextFieldWidget, PreviewCube, StudioTextureLoader, CbButton, CbTextField,
 *             CbTheme, ClientPlayNetworking, VaultResolvePayload.
 * Called by: CustomBlocksClient (VaultConflictPayload receiver).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.VaultResolvePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class VaultConflictScreen extends Screen {

    private static final int BACKDROP = 0x33000000;
    private static final int BAR_BG = CbTheme.BAR_BG, ACCENT = CbTheme.ACCENT, BAR_H = 42;
    private static final int GREY = 0x9AA0A6;               // no-texture cube fill
    private static final int DIFF = 0xFFFFD24A, SAME = 0xFFB0B0B0; // stat colours (changed / unchanged)
    private static final String ID_CHARS = "[A-Za-z0-9_.+\\-]+";
    /** Stat keys shown under each cube, in order. {@code idx} is internal (frame read) — not displayed. */
    private static final String[] KEYS = {"glow", "hard", "sound", "solid", "cat", "shape", "frames"};

    private final String code, incomingId;
    private final PreviewCube youCube = new PreviewCube();
    private final PreviewCube inCube  = new PreviewCube();
    private final int[] grey = PreviewCube.solid(GREY); // stable ref → no-texture cube bakes once, not per frame
    private final int[][] youFrames, inFrames;
    private Map<String, String> youStats = new LinkedHashMap<>(), inStats = new LinkedHashMap<>();
    private String suggest = "";

    private CbTextField idBox;
    private CbButton keepBothBtn, renameBtn;
    private int idBoxY;
    private boolean advanced = false;     // false = Simple (only differing attrs); true = full list
    private boolean idValid = true;
    private String idError = "";

    // ── view (both cubes share one rotation) ──────────────────────────────────
    private static final double DEF_YAW = 28, DEF_PITCH = 16, DEF_SPIN = 0.45;
    private static final int DEF_HALF = 44, HALF_MIN = 30, HALF_MAX = 80, ZOOM_STEP = 6;
    private static final double SPIN_MAX = 2.5, SPIN_STEP = 0.15;
    private double yaw = DEF_YAW, pitch = DEF_PITCH, spin = DEF_SPIN;
    private int half = DEF_HALF;
    private boolean spinning = true, dragging, dragged;

    public VaultConflictScreen(String code, String incomingId, String meta, byte[] preview) {
        super(Text.literal("Resolve Block Conflict"));
        this.code = code;
        this.incomingId = incomingId == null ? "" : incomingId;
        parseMeta(meta == null ? "" : meta);
        // Incoming frames ride the packet; local frames come from the active pack (idx in the "you" stats).
        inFrames = StudioTextureLoader.framesFromPng(preview, intStat(inStats, "frames", 1));
        int idx = intStat(youStats, "idx", -1);
        youFrames = idx >= 0 ? StudioTextureLoader.loadFromPack(idx, intStat(youStats, "frames", 1)) : null;
    }

    // ── meta parsing ───────────────────────────────────────────────────────────
    /** meta = "you=<kv>|in=<kv>|suggest=<freeId>"; each kv = "k=v;k=v;…". */
    private void parseMeta(String meta) {
        for (String tok : meta.split("\\|")) {
            int eq = tok.indexOf('=');
            if (eq < 0) continue;
            String key = tok.substring(0, eq), val = tok.substring(eq + 1);
            switch (key) {
                case "you" -> youStats = parseKv(val);
                case "in"  -> inStats = parseKv(val);
                case "suggest" -> suggest = val;
            }
        }
    }

    private static Map<String, String> parseKv(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : s.split(";")) {
            if (part.isEmpty()) continue;
            int eq = part.indexOf('=');
            if (eq < 0) m.put(part, "");
            else m.put(part.substring(0, eq), part.substring(eq + 1));
        }
        return m;
    }

    private static int intStat(Map<String, String> m, String k, int def) {
        try { return Integer.parseInt(m.getOrDefault(k, "").trim()); }
        catch (Exception e) { return def; }
    }

    // ── init ────────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        int by = height - BAR_H + 11;

        idBoxY = height - BAR_H - 28;
        int boxW = 130, boxX = width / 2 - boxW / 2;
        idBox = new CbTextField(textRenderer, boxX, idBoxY, boxW, 14, Text.literal("new id"));
        idBox.setMaxLength(64);
        idBox.setText(suggest);
        idBox.setChangedListener(t -> validateId());
        addDrawableChild(idBox);

        addDrawableChild(CbButton.normal(Text.literal(advanced ? "View: Advanced" : "View: Simple"),
                width - 122, BAR_H + 6, 98, 16, b -> { advanced = !advanced; rebuild(); }));

        int ow = 86, kw = 92, rw = 104, cw = 70, gap = 6;
        int x = width / 2 - (ow + kw + rw + cw + gap * 3) / 2;
        addDrawableChild(CbButton.normal(Text.literal("Override"), x, by, ow, 20, b -> send(VaultResolvePayload.OVERRIDE)));
        x += ow + gap;
        keepBothBtn = CbButton.primary(Text.literal("Keep Both"), x, by, kw, 20, b -> send(VaultResolvePayload.KEEP_BOTH));
        addDrawableChild(keepBothBtn);
        x += kw + gap;
        renameBtn = CbButton.normal(Text.literal("Rename Mine"), x, by, rw, 20, b -> send(VaultResolvePayload.RENAME_MINE));
        addDrawableChild(renameBtn);
        x += rw + gap;
        addDrawableChild(CbButton.ghost(Text.literal("Cancel"), x, by, cw, 20, b -> close()));

        validateId();
    }

    private void rebuild() { clearChildren(); init(); }

    /** Client-side validation only (charset + not the original id). The server re-checks "taken" (the client
     *  SlotManager is empty on a dedicated server) and errors back if it loses the race. */
    private void validateId() {
        String id = idBox == null ? "" : idBox.getText().trim();
        if (id.isEmpty())               { idValid = false; idError = "Type a new id."; }
        else if (!id.matches(ID_CHARS)) { idValid = false; idError = "Only letters, numbers, _ . + -"; }
        else if (id.equals(incomingId)) { idValid = false; idError = "Pick a different id than " + incomingId; }
        else                            { idValid = true;  idError = ""; }
    }

    private void send(String action) {
        // Keep Both / Rename Mine need a valid typed id; Override ignores it.
        if (!VaultResolvePayload.OVERRIDE.equals(action) && !idValid) return;
        ClientPlayNetworking.send(new VaultResolvePayload(code, action, idBox == null ? "" : idBox.getText().trim()));
        super.close();
    }

    // ── render ────────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* world stays visible */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, BACKDROP);

        int leftCx = width / 4 + 6, rightCx = width * 3 / 4 - 6;
        int cubeCy = BAR_H + 24 + half;
        int yf = frameOf(youFrames), inf = frameOf(inFrames);
        youCube.render(ctx, gridFor(youFrames, yf), leftCx, cubeCy, half, yaw, pitch, PreviewCube.AS_IS, yf);
        inCube.render(ctx, gridFor(inFrames, inf), rightCx, cubeCy, half, yaw, pitch, PreviewCube.AS_IS, inf);

        // Labels + stats (deferred → drawn on top of the immediate cubes).
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lYOURS"), leftCx, BAR_H + 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§c§lINCOMING"), rightCx, BAR_H + 10, 0xFFFFFFFF);
        int statTop = cubeCy + half + 8;
        renderStats(ctx, leftCx, statTop, true);
        renderStats(ctx, rightCx, statTop, false);

        // id box label + validation hint.
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7New id (Keep Both / Rename Mine):"),
                width / 2, idBoxY - 12, 0xFFFFFFFF);
        if (!idError.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§c" + idError), width / 2, idBoxY + 18, 0xFFFFFFFF);

        // Title bar.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Resolve Block Conflict", incomingId), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Same id exists · drag to rotate · scroll = spin · R = reset"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8Override replaces · Keep Both adds · Rename Mine frees the id · Esc cancels"), 8, 32, 0xFFFFFFFF);

        // Bottom action bar.
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, BAR_BG);
        ctx.fill(0, barY, width, barY + 1, ACCENT);

        // Keep Both / Rename Mine track the typed id's validity; Override is always allowed.
        if (keepBothBtn != null) keepBothBtn.active = idValid;
        if (renameBtn != null) renameBtn.active = idValid;

        super.render(ctx, mx, my, delta);

        if (!dragging && spinning) yaw = (yaw + spin * delta) % 360.0;
    }

    /** One column of stats under a cube. Simple mode shows only attrs that differ; Advanced shows all. */
    private void renderStats(DrawContext ctx, int cx, int top, boolean youSide) {
        int y = top, shown = 0;
        for (String k : KEYS) {
            String yv = youStats.getOrDefault(k, ""), iv = inStats.getOrDefault(k, "");
            boolean differs = !yv.equals(iv);
            if (!advanced && !differs) continue;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(fmt(k, youSide ? yv : iv)),
                    cx, y, differs ? DIFF : SAME);
            y += 11;
            shown++;
        }
        if (shown == 0) // Simple mode, nothing differs
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7identical"), cx, y, SAME);
    }

    private static String fmt(String k, String v) {
        return switch (k) {
            case "glow"   -> "Glow " + def(v, "0");
            case "hard"   -> "Hardness " + def(v, "?");
            case "sound"  -> cap(def(v, "stone"));
            case "solid"  -> "true".equals(v) ? "Solid" : "Passable";
            case "cat"    -> "Cat: " + (v.isEmpty() ? "—" : v);
            case "shape"  -> "Shape: " + def(v, "full");
            case "frames" -> ("1".equals(v) || v.isEmpty()) ? "Static" : "Animated " + v + "f";
            default       -> k + " " + v;
        };
    }

    private static String def(String v, String d) { return v == null || v.isEmpty() ? d : v; }
    private static String cap(String v) { return v.isEmpty() ? v : Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase(Locale.ROOT); }

    private int frameOf(int[][] f) {
        if (f == null || f.length <= 1) return 0;
        return (int) ((System.currentTimeMillis() / 120L) % f.length);
    }

    private int[] gridFor(int[][] frames, int idx) {
        if (frames == null || frames.length == 0) return grey;
        int[] g = frames[idx % frames.length];
        return g == null ? grey : g;
    }

    // ── input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0) { dragging = true; dragged = false; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
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
        if (dragging && !dragged) spinning = !spinning;
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (hasShiftDown()) half = Math.max(HALF_MIN, Math.min(HALF_MAX, half + (int) Math.signum(vAmt) * ZOOM_STEP));
        else spin = Math.max(0, Math.min(SPIN_MAX, spin + vAmt * SPIN_STEP));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = getFocused() instanceof TextFieldWidget;
        if (key == GLFW.GLFW_KEY_R && !typing) {
            yaw = DEF_YAW; pitch = DEF_PITCH; spin = DEF_SPIN; half = DEF_HALF; spinning = true;
            return true;
        }
        return super.keyPressed(key, scan, mods); // ESC → close() (nothing happens)
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() {
        youCube.dispose();
        inCube.dispose();
        super.removed();
    }
}
