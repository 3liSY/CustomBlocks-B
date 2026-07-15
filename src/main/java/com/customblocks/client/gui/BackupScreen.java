/**
 * BackupScreen.java — Group 09 §G09-A4 (Backup Screen). CLIENT-ONLY.
 *
 * The Screen that replaces the chest backup menu and the /cb backup list chat text. Group 27
 * CbScreenTemplate standard (red #FF0000 / black / lime #40FF00): left-rail tabs (Manual · Auto · All),
 * a search box, a sort cycle (Newest · Oldest · Size), a scrollable list with per-row block-count + size
 * + a pinned marker, an in-screen "Create backup" field, and an opaque confirm modal before restore or
 * delete (UI_SCREEN_RULES: no bleed-through).
 *
 * It holds NO authoritative state — every action ships a {@link BackupActionPayload}; the server runs the
 * real BackupCommands rail and answers with a fresh OpenGuiPayload(BACKUP_SCREEN) that calls
 * {@link #refresh(String)}, so the list re-renders in place and the Screen never closes.
 *
 * Depends on: BackupActionPayload, CbTheme, CbTextField, Gson. Called by: CustomBlocksClient
 * (OpenGuiPayload mode=BACKUP_SCREEN → open or refresh).
 */
package com.customblocks.client.gui;

import com.customblocks.network.payloads.BackupActionPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class BackupScreen extends Screen {

    /** One backup row as sent by BackupManager.screenJson(). */
    private record Row(String name, String label, String when, int blocks, String size, boolean auto, boolean prot) {}

    private static final int BAR_H   = 42;   // title + bottom bar height
    private static final int LEFT_W  = 90;   // left tab rail width
    private static final int ROW_H   = 26;   // two text lines per row

    private static final int TAB_MANUAL = 0, TAB_AUTO = 1, TAB_ALL = 2;
    private static final int SORT_NEW = 0, SORT_OLD = 1, SORT_SIZE = 2;
    private static final String[] SORT_LABEL = {"Newest", "Oldest", "Size"};

    private static final int MODAL_NONE = 0, MODAL_RESTORE = 1, MODAL_DELETE = 2, MODAL_RENAME = 3;

    private final List<Row> rows = new ArrayList<>();
    private int tab = TAB_MANUAL;
    private int sort = SORT_NEW;
    private int scroll;                 // first visible row index
    private String selected;            // selected backup's raw id, or null
    private int modal = MODAL_NONE;

    private String search = "";         // survives widget rebuilds
    private String createName = "";
    private CbTextField searchField;
    private CbTextField createField;
    private CbTextField renameField;

    // Selected-row action buttons (disabled when nothing is selected).
    private ButtonWidget restoreBtn, renameBtn, protectBtn, deleteBtn;

    public BackupScreen(String data) {
        super(Text.literal("Backups"));
        parse(data);
    }

    /** Re-render in place from a fresh server list (keeps the screen, tab, search and selection). */
    public void refresh(String data) {
        parse(data);
        if (selected != null && rows.stream().noneMatch(r -> r.name.equals(selected))) selected = null;
        modal = MODAL_NONE;
        if (client != null) reinit();
    }

    private void parse(String data) {
        rows.clear();
        try {
            JsonObject root = JsonParser.parseString(data == null ? "{}" : data).getAsJsonObject();
            JsonArray arr = root.has("backups") ? root.getAsJsonArray("backups") : new JsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                rows.add(new Row(
                        str(o, "name"), str(o, "label"), str(o, "when"),
                        o.has("blocks") ? o.get("blocks").getAsInt() : -1,
                        str(o, "size"),
                        o.has("auto") && o.get("auto").getAsBoolean(),
                        o.has("prot") && o.get("prot").getAsBoolean()));
            }
        } catch (Exception ignored) { /* leave rows empty on a malformed snapshot */ }
    }

    private static String str(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsString() : ""; }

    private void reinit() { clearChildren(); init(); }

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        int barY = height - BAR_H;

        // Left-rail tabs (TABS_ON_LEFT). Active tab label is red.
        addTab("Manual", TAB_MANUAL, BAR_H + 8);
        addTab("Auto",   TAB_AUTO,   BAR_H + 32);
        addTab("All",    TAB_ALL,    BAR_H + 56);

        // Search + sort across the top of the content area.
        int contentX = LEFT_W + 10;
        int sortW = 92;
        searchField = new CbTextField(textRenderer, contentX, BAR_H + 8, width - sortW - contentX - 12, 16,
                Text.literal("search"));
        searchField.setMaxLength(48);
        searchField.setText(search);
        searchField.setChangedListener(s -> { search = s; scroll = 0; });
        addDrawableChild(searchField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Sort: " + SORT_LABEL[sort]), b -> cycleSort())
                .dimensions(width - sortW - 8, BAR_H + 6, sortW, 18).build());

        // Bottom bar: create field + Create (left), selected-row actions (right), Close (far right).
        createField = new CbTextField(textRenderer, contentX, barY + 13, 120, 16, Text.literal("new name (optional)"));
        createField.setMaxLength(48);
        createField.setText(createName);
        createField.setChangedListener(s -> createName = s);
        addDrawableChild(createField);
        addDrawableChild(ButtonWidget.builder(Text.literal("§a+ Create"), b -> doCreate())
                .dimensions(contentX + 126, barY + 11, 64, 20).build());

        int rx = width - 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(rx - 56, barY + 11, 56, 20).build());
        deleteBtn = actionBtn("§cDelete", rx - 56 - 4 - 62, barY, this::askDelete);
        protectBtn = actionBtn("Protect", rx - 56 - 4 - 62 - 4 - 70, barY, this::doProtect);
        renameBtn = actionBtn("Rename", rx - 56 - 4 - 62 - 4 - 70 - 4 - 62, barY, this::askRename);
        restoreBtn = actionBtn("Restore", rx - 56 - 4 - 62 - 4 - 70 - 4 - 62 - 4 - 62, barY, this::askRestore);
        updateActionButtons();

        // A modal owns input while open — build only its buttons, disable the rest.
        if (modal != MODAL_NONE) buildModal();
    }

    private void addTab(String name, int which, int y) {
        boolean active = tab == which;
        addDrawableChild(ButtonWidget.builder(Text.literal(active ? "§c" + name : name), b -> { tab = which; scroll = 0; reinit(); })
                .dimensions(8, y, LEFT_W - 16, 20).build());
    }

    private ButtonWidget actionBtn(String label, int x, int barY, Runnable onClick) {
        int w = label.contains("Protect") ? 70 : 62;
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> onClick.run())
                .dimensions(x, barY + 11, w, 20).build();
        addDrawableChild(b);
        return b;
    }

    private void updateActionButtons() {
        boolean has = selected != null && modal == MODAL_NONE;
        for (ButtonWidget b : new ButtonWidget[]{restoreBtn, renameBtn, protectBtn, deleteBtn}) {
            if (b != null) b.active = has;
        }
        if (protectBtn != null) {
            Row r = selectedRow();
            protectBtn.setMessage(Text.literal(r != null && r.prot ? "Unprotect" : "Protect"));
        }
    }

    // ── Filtering / sorting ────────────────────────────────────────────────────
    private List<Row> visibleRows() {
        List<Row> out = new ArrayList<>();
        String q = search.trim().toLowerCase(Locale.ROOT);
        for (Row r : rows) {
            if (tab == TAB_MANUAL && r.auto) continue;
            if (tab == TAB_AUTO && !r.auto) continue;
            if (!q.isEmpty() && !(r.name.toLowerCase(Locale.ROOT).contains(q)
                    || r.label.toLowerCase(Locale.ROOT).contains(q))) continue;
            out.add(r);
        }
        // rows arrive newest-first; Newest keeps that, Oldest reverses, Size sorts by parsed byte size desc.
        if (sort == SORT_OLD) java.util.Collections.reverse(out);
        else if (sort == SORT_SIZE) out.sort((a, b) -> Long.compare(sizeBytes(b.size), sizeBytes(a.size)));
        return out;
    }

    /** Parse "12.3 KB" back to an approximate byte count for size sorting. */
    private static long sizeBytes(String s) {
        try {
            String t = s.trim();
            int sp = t.indexOf(' ');
            if (sp < 0) return 0;
            double n = Double.parseDouble(t.substring(0, sp));
            String unit = t.substring(sp + 1).toUpperCase(Locale.ROOT);
            return switch (unit) {
                case "GB" -> (long) (n * 1024 * 1024 * 1024);
                case "MB" -> (long) (n * 1024 * 1024);
                case "KB" -> (long) (n * 1024);
                default -> (long) n;
            };
        } catch (Exception e) { return 0; }
    }

    private Row selectedRow() {
        if (selected == null) return null;
        for (Row r : rows) if (r.name.equals(selected)) return r;
        return null;
    }

    private int listTop()    { return BAR_H + 30; }
    private int listBottom() { return height - BAR_H - 6; }
    private int perPage()    { return Math.max(1, (listBottom() - listTop()) / ROW_H); }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* own backdrop below */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x33000000);                       // world dim
        ctx.fill(0, BAR_H, LEFT_W, height - BAR_H, CbTheme.BAR_BG);      // left rail
        ctx.fill(LEFT_W, BAR_H, LEFT_W + 1, height - BAR_H, CbTheme.ACCENT_DIM);

        // Title bar
        ctx.fill(0, 0, width, BAR_H, CbTheme.BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, CbTheme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Backups", tabName() + " · " + rows.size() + " total"),
                8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7Click a row to select · buttons below act on it · Create adds one now"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Restore/Delete ask to confirm · pinned backups survive auto-prune · Esc close"), 8, 32, 0xFFFFFFFF);

        // List
        List<Row> vis = visibleRows();
        if (scroll > Math.max(0, vis.size() - perPage())) scroll = Math.max(0, vis.size() - perPage());
        renderList(ctx, vis, mx, my);

        // Bottom bar
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, CbTheme.BAR_BG);
        ctx.fill(0, barY, width, barY + 1, CbTheme.ACCENT);

        super.render(ctx, mx, my, delta);   // widgets (tabs, fields, buttons)
        updateActionButtons();

        if (modal != MODAL_NONE) renderModal(ctx, mx, my);
    }

    private void renderList(DrawContext ctx, List<Row> vis, int mx, int my) {
        int x0 = LEFT_W + 8, x1 = width - 8, top = listTop();
        if (vis.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§8No backups here yet."), x0 + 4, top + 8, 0xFFFFFFFF);
            return;
        }
        int end = Math.min(vis.size(), scroll + perPage());
        for (int i = scroll; i < end; i++) {
            Row r = vis.get(i);
            int y = top + (i - scroll) * ROW_H;
            boolean sel = r.name.equals(selected);
            boolean hover = mx >= x0 && mx <= x1 && my >= y && my < y + ROW_H - 2 && modal == MODAL_NONE;
            ctx.fill(x0, y, x1, y + ROW_H - 2, sel ? CbTheme.SEL_FILL : (hover ? 0x22FFFFFF : CbTheme.CARD));
            if (sel) {
                ctx.fill(x0, y, x1, y + 1, CbTheme.ACCENT);
                ctx.fill(x0, y + ROW_H - 3, x1, y + ROW_H - 2, CbTheme.ACCENT);
            }
            String pin = r.prot ? " §7[§fpinned§7]" : "";
            ctx.drawTextWithShadow(textRenderer, Text.literal("§f" + r.label + pin), x0 + 6, y + 3, 0xFFFFFFFF);
            String meta = "§8" + r.name + " §7· " + r.when + " §7· " + blocksText(r) + " §7· " + r.size;
            ctx.drawTextWithShadow(textRenderer, Text.literal(meta), x0 + 6, y + 14, 0xFFFFFFFF);
        }
        // Scrollbar hint
        if (vis.size() > perPage()) {
            int trackH = listBottom() - top;
            int thumbH = Math.max(12, trackH * perPage() / vis.size());
            int thumbY = top + (trackH - thumbH) * scroll / Math.max(1, vis.size() - perPage());
            ctx.fill(x1 - 2, top, x1, listBottom(), 0x33FFFFFF);
            ctx.fill(x1 - 2, thumbY, x1, thumbY + thumbH, CbTheme.ACCENT);
        }
    }

    private static String blocksText(Row r) { return r.blocks >= 0 ? r.blocks + " blk" : "? blk"; }
    private String tabName() { return tab == TAB_MANUAL ? "Manual" : tab == TAB_AUTO ? "Auto" : "All"; }

    // ── Modal (opaque, per UI_SCREEN_RULES) ────────────────────────────────────
    private void renderModal(DrawContext ctx, int mx, int my) {
        int cx = width / 2, cy = height / 2;
        int pw = 300, ph = modal == MODAL_RENAME ? 116 : 96;
        ctx.fill(0, 0, width, height, 0x66000000);                                   // dim the screen further
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, CbTheme.ACCENT);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, CbTheme.DIALOG_BG); // fully opaque fill
        Row r = selectedRow();
        String tgt = r != null ? r.label : (selected == null ? "?" : selected);
        String title = switch (modal) {
            case MODAL_RESTORE -> "§fRestore this backup?";
            case MODAL_DELETE  -> "§cDelete this backup?";
            default            -> "§fRename backup";
        };
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(title), cx, cy - ph / 2 + 8, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + tgt), cx, cy - ph / 2 + 22, 0xFFFFFFFF);
        if (modal == MODAL_RESTORE)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8Your current blocks are saved first."), cx, cy - ph / 2 + 34, 0xFFFFFFFF);
        if (modal == MODAL_DELETE)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8This can't be undone."), cx, cy - ph / 2 + 34, 0xFFFFFFFF);
    }

    private void buildModal() {
        // Disable everything behind the modal, then add its two buttons (+ the rename field).
        for (var child : new ArrayList<>(children())) {
            if (child instanceof ButtonWidget bw) bw.active = false;
            if (child instanceof CbTextField tf) tf.active = false;
        }
        int cx = width / 2, cy = height / 2;
        int ph = modal == MODAL_RENAME ? 116 : 96;
        int by = cy + ph / 2 - 28;
        if (modal == MODAL_RENAME) {
            renameField = new CbTextField(textRenderer, cx - 130, cy - 8, 260, 16, Text.literal("new name"));
            renameField.setMaxLength(48);
            Row r = selectedRow();
            renameField.setText(r != null && !r.auto ? r.name : "");
            addDrawableChild(renameField);
            setInitialFocus(renameField);
        }
        String okLabel = switch (modal) {
            case MODAL_RESTORE -> "§aRestore";
            case MODAL_DELETE  -> "§cDelete";
            default            -> "§aRename";
        };
        addDrawableChild(ButtonWidget.builder(Text.literal(okLabel), b -> confirmModal())
                .dimensions(cx - 130, by, 126, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> cancelModal())
                .dimensions(cx + 4, by, 126, 20).build());
    }

    // ── Actions ─────────────────────────────────────────────────────────────
    private void cycleSort() { sort = (sort + 1) % 3; reinit(); }

    private void doCreate() {
        send(BackupActionPayload.ACTION_CREATE, createName.trim(), "");
        createName = "";
        if (createField != null) createField.setText("");
    }

    private void askRestore() { if (selected != null) { modal = MODAL_RESTORE; reinit(); } }
    private void askDelete()  { if (selected != null) { modal = MODAL_DELETE;  reinit(); } }
    private void askRename()  { if (selected != null) { modal = MODAL_RENAME;  reinit(); } }
    private void doProtect()  { if (selected != null) send(BackupActionPayload.ACTION_PROTECT, selected, ""); }

    private void confirmModal() {
        switch (modal) {
            case MODAL_RESTORE -> send(BackupActionPayload.ACTION_RESTORE, selected, "");
            case MODAL_DELETE  -> send(BackupActionPayload.ACTION_DELETE, selected, "");
            case MODAL_RENAME  -> {
                String nn = renameField != null ? renameField.getText().trim() : "";
                if (nn.isEmpty()) return; // keep the modal open until a name is typed
                send(BackupActionPayload.ACTION_RENAME, selected, nn);
            }
            default -> { }
        }
        modal = MODAL_NONE;   // server refresh re-opens with the new state
        reinit();
    }

    private void cancelModal() { modal = MODAL_NONE; reinit(); }

    private void send(String action, String name, String arg) {
        ClientPlayNetworking.send(new BackupActionPayload(action, name == null ? "" : name, arg == null ? "" : arg));
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (modal == MODAL_NONE && button == 0) {
            List<Row> vis = visibleRows();
            int x0 = LEFT_W + 8, x1 = width - 8, top = listTop();
            int end = Math.min(vis.size(), scroll + perPage());
            for (int i = scroll; i < end; i++) {
                int y = top + (i - scroll) * ROW_H;
                if (mx >= x0 && mx <= x1 && my >= y && my < y + ROW_H - 2) {
                    String name = vis.get(i).name;
                    selected = name.equals(selected) ? null : name;
                    updateActionButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (modal == MODAL_NONE) {
            int max = Math.max(0, visibleRows().size() - perPage());
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vAmount)));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (modal != MODAL_NONE) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { cancelModal(); return true; }
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
                    && !(modal == MODAL_RENAME && (renameField == null || renameField.getText().trim().isEmpty()))) {
                confirmModal(); return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }
}
