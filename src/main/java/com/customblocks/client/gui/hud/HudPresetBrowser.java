/**
 * HudPresetBrowser.java — GROUP 27 §G27.4 (Lego HUD Builder). CLIENT-SIDE ONLY.
 *
 * Responsibility: the preset picker. Lists the 3 built-ins + saved presets, each with a mini
 * rendered thumbnail of its layout; click to load it into the editor. [Save as…] stores the
 * current layout (name clash → overwrite-confirm), [Import] loads a code string from the
 * clipboard, [Export] copies the current layout as a code string AND writes a .json backup.
 *
 * Depends on: HudConfig, HudField, HudPresetStore.
 * Called by: HudEditorScreen ([Preset ▾]).
 */
package com.customblocks.client.gui.hud;

import com.customblocks.client.HudConfig;
import com.customblocks.client.hud.HudField;
import com.customblocks.client.hud.HudPresetStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HudPresetBrowser extends Screen {

    private static final int GOLD = 0xFFFFAA00;
    private static final int PW = 300, PH = 230;
    private static final int ROW_H = 34, THUMB_W = 56, THUMB_H = 28;
    private static final int REF_W = 320, REF_H = 180;   // reference resolution for thumbnails

    private enum Mode { LIST, NAME, CONFIRM }

    private final Screen parent;
    private final List<HudPresetStore.Preset> presets = new ArrayList<>();
    private Mode mode = Mode.LIST;
    private int scroll = 0;
    private int px, py, listTop, listBottom;
    private TextFieldWidget nameField;
    private String pendingName = "";

    public HudPresetBrowser(Screen parent) {
        super(Text.literal("HUD Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        listTop = py + 28;
        listBottom = py + PH - 30;
        reloadList();

        int by = py + PH - 24, bw = (PW - 20) / 4;
        if (mode == Mode.LIST) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Save as…"), b -> { mode = Mode.NAME; pendingName = ""; rebuild(); })
                    .dimensions(px + 8, by, bw, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Import"), b -> doImport())
                    .dimensions(px + 8 + bw + 2, by, bw, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Export"), b -> doExport())
                    .dimensions(px + 8 + (bw + 2) * 2, by, bw, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> back())
                    .dimensions(px + 8 + (bw + 2) * 3, by, bw, 18).build());
        } else if (mode == Mode.NAME) {
            nameField = new TextFieldWidget(textRenderer, px + 8, py + 40, PW - 16, 18, Text.literal("name"));
            nameField.setMaxLength(32);
            nameField.setText(pendingName);
            addDrawableChild(nameField);
            setInitialFocus(nameField);
            addDrawableChild(ButtonWidget.builder(Text.literal("§aSave"), b -> trySave())
                    .dimensions(px + 8, by, 80, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> { mode = Mode.LIST; rebuild(); })
                    .dimensions(px + PW - 88, by, 80, 18).build());
        } else { // CONFIRM
            addDrawableChild(ButtonWidget.builder(Text.literal("§aYes, overwrite"), b -> { commitSave(pendingName); mode = Mode.LIST; rebuild(); })
                    .dimensions(px + 8, by, 130, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("No"), b -> { mode = Mode.NAME; rebuild(); })
                    .dimensions(px + PW - 88, by, 80, 18).build());
        }
    }

    private void reloadList() {
        presets.clear();
        presets.addAll(HudPresetStore.builtins());
        presets.addAll(HudPresetStore.saved());
    }

    private void rebuild() { clearChildren(); init(); }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (parent != null) parent.render(ctx, mx, my, 0);
        ctx.fill(0, 0, width, height, 0x66000000);
        ctx.fill(px - 1, py - 1, px + PW + 1, py + PH + 1, GOLD);
        ctx.fill(px, py, px + PW, py + PH, 0xF0101010);

        if (mode == Mode.LIST) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lHUD Presets"), px + 8, py + 9, 0xFFFFFFFF);
            ctx.enableScissor(px, listTop, px + PW, listBottom);
            int y = listTop - scroll;
            for (int i = 0; i < presets.size(); i++) {
                HudPresetStore.Preset p = presets.get(i);
                int ry = y + i * ROW_H;
                boolean hover = mx >= px + 4 && mx <= px + PW - 4 && my >= ry && my < ry + ROW_H - 2 && my >= listTop && my < listBottom;
                if (hover) ctx.fill(px + 4, ry, px + PW - 4, ry + ROW_H - 2, 0x33FFFFFF);
                drawThumb(ctx, px + 8, ry + 2, p.fields());
                ctx.drawTextWithShadow(textRenderer, Text.literal("§f" + p.name()), px + 8 + THUMB_W + 8, ry + 6, 0xFFFFFFFF);
                ctx.drawText(textRenderer, Text.literal(p.builtin() ? "§8built-in" : "§7custom"), px + 8 + THUMB_W + 8, ry + 18, 0xFFFFFFFF, false);
            }
            ctx.disableScissor();
        } else if (mode == Mode.NAME) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lSave preset as…"), px + 8, py + 9, 0xFFFFFFFF);
            ctx.drawText(textRenderer, Text.literal("§7Name this layout, then Save."), px + 8, py + 24, 0xFFFFFFFF, false);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§6§lOverwrite?"), px + 8, py + 9, 0xFFFFFFFF);
            ctx.drawText(textRenderer, Text.literal("§eA preset named '" + pendingName + "' exists."), px + 8, py + 40, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, Text.literal("§7Overwrite it?"), px + 8, py + 54, 0xFFFFFFFF, false);
        }

        super.render(ctx, mx, my, delta);
    }

    /** Mini layout thumbnail: one colour bar per visible brick at its scaled-down position. */
    private void drawThumb(DrawContext ctx, int x, int y, List<HudField> fields) {
        ctx.fill(x, y, x + THUMB_W, y + THUMB_H, 0xFF202020);
        ctx.fill(x, y, x + THUMB_W, y + 1, 0xFF000000);
        int barW = 40, barH = 8;   // reference-space brick footprint
        for (HudField f : fields) {
            if (!f.visible) continue;
            int rx, ry;
            switch (f.anchor) {
                case TR     -> { rx = REF_W - f.offsetX - barW; ry = f.offsetY; }
                case BL     -> { rx = f.offsetX;                ry = REF_H - f.offsetY - barH; }
                case BR     -> { rx = REF_W - f.offsetX - barW; ry = REF_H - f.offsetY - barH; }
                case CENTER -> { rx = REF_W / 2 + f.offsetX - barW / 2; ry = REF_H / 2 + f.offsetY - barH / 2; }
                default     -> { rx = f.offsetX;                ry = f.offsetY; }
            }
            int tx = x + Math.round(rx / (float) REF_W * THUMB_W);
            int ty = y + Math.round(ry / (float) REF_H * THUMB_H);
            int tw = Math.max(3, Math.round(barW / (float) REF_W * THUMB_W));
            int th = Math.max(2, Math.round(barH / (float) REF_H * THUMB_H));
            tx = Math.max(x, Math.min(x + THUMB_W - tw, tx));
            ty = Math.max(y, Math.min(y + THUMB_H - th, ty));
            ctx.fill(tx, ty, tx + tw, ty + th, 0xFF000000 | (f.color & 0xFFFFFF));
        }
    }

    // ── Input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (mode == Mode.LIST && my >= listTop && my < listBottom) {
            int idx = (int) Math.floor((my - (listTop - scroll)) / ROW_H);
            if (idx >= 0 && idx < presets.size()) { applyPreset(presets.get(idx)); back(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        if (mode == Mode.LIST) {
            int content = presets.size() * ROW_H;
            int view = listBottom - listTop;
            int max = Math.max(0, content - view);
            scroll = Math.max(0, Math.min(max, scroll - (int) (vert * 18)));
            return true;
        }
        return super.mouseScrolled(mx, my, horiz, vert);
    }

    // ── Actions ────────────────────────────────────────────────────────────
    private void applyPreset(HudPresetStore.Preset p) {
        HudConfig.fields.clear();
        HudConfig.fields.addAll(HudPresetStore.copyOf(p.fields()));
        toast("§aLoaded preset '" + p.name() + "'.");
    }

    private void trySave() {
        String name = nameField != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) { toast("§cEnter a name first."); return; }
        pendingName = name;
        if (HudPresetStore.exists(name)) { mode = Mode.CONFIRM; rebuild(); }
        else { commitSave(name); mode = Mode.LIST; rebuild(); }
    }

    private void commitSave(String name) {
        boolean ok = HudPresetStore.save(name, HudConfig.fields);
        toast(ok ? "§aSaved preset '" + name + "'." : "§cCouldn't save preset.");
        reloadList();
    }

    private void doImport() {
        if (client == null) return;
        List<HudField> list = HudPresetStore.decode(client.keyboard.getClipboard());
        if (list == null) { toast("§cClipboard isn't a valid HUD code."); return; }
        HudConfig.fields.clear();
        HudConfig.fields.addAll(list);
        toast("§aImported HUD layout from clipboard.");
        back();
    }

    private void doExport() {
        if (client == null) return;
        String code = HudPresetStore.encode(HudConfig.fields);
        client.keyboard.setClipboard(code);
        boolean file = HudPresetStore.save("Exported", HudConfig.fields);
        toast(file ? "§aExported: code copied + saved Exported.json." : "§aExport code copied to clipboard.");
        reloadList();
    }

    private void toast(String msg) {
        if (client != null && client.player != null) client.player.sendMessage(Text.literal(msg), false);
    }

    private void back() { if (client != null) client.setScreen(parent); }

    @Override
    public void close() { if (mode != Mode.LIST) { mode = Mode.LIST; rebuild(); } else back(); }

    @Override
    public boolean shouldPause() { return false; }
}
