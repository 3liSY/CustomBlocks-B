/**
 * StudioCategoryWorkspacePanel.java - wide G11-3 Category Forge sample for /cb create.
 *
 * This replaces the cramped first sample with a real workspace-sized tab. It still does not migrate
 * the category data model: publishing sends the current single StudioState.category until G11 gets
 * its full category-record/multi-membership slice.
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import com.customblocks.client.gui.studio.StudioState;
import com.customblocks.network.payloads.CategoryAdminPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class StudioCategoryWorkspacePanel {

    private enum Flow { BASIC, STYLE, RULES, SHORTCUTS, DELETE }
    private record Row(String id, String name, int depth, int color, String badge, String meta, int count) {}

    private static final String SEC = "\u00a7";
    private static final int ACCENT = CbTheme.ACCENT;
    private static final int CARD = 0xAA101010;
    private static final int CARD_2 = 0xCC151515;
    private static final int LINE = 0xFF303030;
    private static final int MAX_ROWS = 4;

    private static final String[] FLOW_LABELS = {"Basic", "Style", "Rules", "Keys", "Delete"};
    private static final String[] DELETE_LABELS = {"Category only", "Exclusive blocks", "All shown", "Move first"};
    private static final String[] COL_TAG = {
            SEC + "0", SEC + "1", SEC + "2", SEC + "3", SEC + "4", SEC + "5", SEC + "6", SEC + "7",
            SEC + "8", SEC + "9", SEC + "a", SEC + "b", SEC + "c", SEC + "d", SEC + "e", SEC + "f"};
    private static final int[] COL_ARGB = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF};

    private Flow flow = Flow.BASIC;
    private boolean confirmDelete;
    private int deleteMode;

    private List<int[]> rowRects = new ArrayList<>();
    private List<Row> rowData = new ArrayList<>();
    private int[][] actionRects;
    private int[][] flowRects;
    private int[][] colorRects;
    private int[][] deleteRects;

    public void reset() { confirmDelete = false; }

    public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                       StudioState st, TextFieldWidget field, int mx, int my) {
        rowRects = new ArrayList<>();
        rowData = rows(st);
        actionRects = flowRects = colorRects = deleteRects = null;

        int panelW = Math.max(500, w);
        int treeW = Math.min(238, Math.max(214, panelW / 3));
        int previewW = Math.min(170, Math.max(150, panelW / 4));
        int gap = 10;
        int editorW = panelW - treeW - previewW - gap * 2;
        if (editorW < 190) {
            previewW = 0;
            editorW = panelW - treeW - gap;
        }

        Row main = mainRow(st);
        drawHeader(ctx, tr, x, y, panelW, st, main);

        int bodyY = y + 34;
        drawTree(ctx, tr, x, bodyY, treeW, st, mx, my);
        drawEditor(ctx, tr, x + treeW + gap, bodyY, editorW, main, st, mx, my);
        if (previewW > 0) drawPreview(ctx, tr, x + panelW - previewW, bodyY, previewW, main, st);
    }

    public boolean mouseClicked(double mx, double my, StudioState st, TextFieldWidget field) {
        for (int i = 0; i < rowRects.size(); i++) {
            if (in(rowRects.get(i), mx, my)) {
                st.category = rowData.get(i).id();
                confirmDelete = false;
                return true;
            }
        }
        if (flowRects != null) {
            for (int i = 0; i < flowRects.length; i++) {
                if (in(flowRects[i], mx, my)) {
                    flow = Flow.values()[i];
                    confirmDelete = false;
                    return true;
                }
            }
        }
        if (actionRects != null) {
            if (in(actionRects[0], mx, my)) { assignTyped(st, field); return true; }
            if (in(actionRects[1], mx, my)) { if (hasExisting(st)) renameSelected(st, field); return true; }
            if (in(actionRects[2], mx, my)) { if (hasMain(st)) send("default", st.category, ""); return true; }
        }
        if (colorRects != null) {
            for (int i = 0; i < colorRects.length; i++) {
                if (in(colorRects[i], mx, my)) {
                    if (hasMain(st)) send("color", st.category, COL_TAG[i]);
                    return true;
                }
            }
        }
        if (deleteRects != null) {
            for (int i = 0; i < DELETE_LABELS.length; i++) {
                if (in(deleteRects[i], mx, my)) { deleteMode = i; confirmDelete = false; return true; }
            }
            int confirm = DELETE_LABELS.length;
            if (deleteRects.length > confirm && in(deleteRects[confirm], mx, my)) {
                if (!hasExisting(st)) return true;
                if (confirmDelete) {
                    send("delete", st.category, "");
                    st.category = "";
                    confirmDelete = false;
                } else {
                    confirmDelete = true;
                }
                return true;
            }
        }
        return false;
    }

    public void onEnter(StudioState st, TextFieldWidget field) { assignTyped(st, field); }

    private void drawHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, StudioState st, Row main) {
        ctx.fill(x, y, x + w, y + 27, 0x99101010);
        ctx.fill(x, y, x + w, y + 1, ACCENT);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "c" + SEC + "lCategory Forge " + SEC + "7- in-game sample"), x + 8, y + 6, 0xFFFFFFFF);
        String selected = main == null ? "none" : main.name() + " / " + main.badge();
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8selected main: " + SEC + "7" + fit(selected, tr, w - 230)), x + 214, y + 7, 0xFFFFFFFF);
    }

    private void drawTree(DrawContext ctx, TextRenderer tr, int x, int y, int w, StudioState st, int mx, int my) {
        drawCard(ctx, x, y, w, 224);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "fCategory Tree"), x + 9, y + 8, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8Pick the visible main badge"), x + 9, y + 20, 0xFFFFFFFF);

        int ry = y + 38;
        for (int i = 0; i < Math.min(MAX_ROWS, rowData.size()); i++) {
            Row r = rowData.get(i);
            int rx = x + 8 + r.depth() * 14;
            int rw = w - 16 - r.depth() * 14;
            boolean selected = r.id().equalsIgnoreCase(st.category == null ? "" : st.category.trim());
            boolean hover = mx >= rx && mx < rx + rw && my >= ry && my < ry + 23;
            ctx.fill(rx - 1, ry - 1, rx + rw + 1, ry + 24, selected ? ACCENT : (hover ? 0xFF777777 : 0xFF000000));
            ctx.fill(rx, ry, rx + rw, ry + 23, selected ? 0xFF2A1014 : 0xFF191919);
            ctx.fill(rx + 5, ry + 5, rx + 19, ry + 19, r.color());
            ctx.drawTextWithShadow(tr, Text.literal((selected ? SEC + "f" : SEC + "7") + fit(r.name(), tr, rw - 74)), rx + 26, ry + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + r.meta()), rx + 26, ry + 14, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + r.count()), rx + rw - 18, ry + 8, 0xFFFFFFFF);
            rowRects.add(new int[]{rx, ry, rw, 23});
            ry += 27;
        }

        ctx.drawTextWithShadow(tr, Text.literal(SEC + "7New category / rename"), x + 9, y + 159, 0xFFFFFFFF);
        drawActions(ctx, tr, x + 8, y + 194, w - 16, st, mx, my);
    }

    private void drawActions(DrawContext ctx, TextRenderer tr, int x, int y, int w, StudioState st, int mx, int my) {
        String[] labels = {"Add", "Rename", "Default"};
        boolean[] enabled = {true, hasExisting(st), hasMain(st)};
        actionRects = new int[labels.length][4];
        int bw = (w - 8) / 3;
        for (int i = 0; i < labels.length; i++) {
            actionRects[i] = new int[]{x + i * (bw + 4), y, bw, 18};
            drawButton(ctx, tr, labels[i], actionRects[i], enabled[i], in(actionRects[i], mx, my), false);
        }
    }

    private void drawEditor(DrawContext ctx, TextRenderer tr, int x, int y, int w, Row main, StudioState st, int mx, int my) {
        drawCard(ctx, x, y, w, 224);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "fAdvanced Editor"), x + 9, y + 8, 0xFFFFFFFF);
        drawFlowTabs(ctx, tr, x + 8, y + 27, w - 16, mx, my);
        ctx.fill(x + 8, y + 51, x + w - 8, y + 212, 0x66101010);
        ctx.fill(x + 8, y + 51, x + w - 8, y + 52, LINE);

        int cy = y + 58;
        switch (flow) {
            case BASIC -> {
                drawLine(ctx, tr, x, cy, "Key", main == null ? "none selected" : main.id(), w);
                drawLine(ctx, tr, x, cy + 17, "Parent", main == null ? "root" : main.meta(), w);
                drawLine(ctx, tr, x, cy + 34, "Badge", main == null ? "NONE" : main.badge() + " visible", w);
                drawLine(ctx, tr, x, cy + 51, "Also in", "Building, Walls (preview)", w);
                drawLine(ctx, tr, x, cy + 68, "Create", "empty or from current block", w);
            }
            case STYLE -> drawStyle(ctx, tr, x, cy, w, mx, my);
            case RULES -> {
                drawLine(ctx, tr, x, cy, "Auto-add", "stone, wall, brick, trim", w);
                drawLine(ctx, tr, x, cy + 17, "Depth", "unlimited; warn after 2", w);
                drawLine(ctx, tr, x, cy + 34, "Visibility", "browse + create", w);
                drawLine(ctx, tr, x, cy + 51, "Permissions", "admins edit, players browse", w);
            }
            case SHORTCUTS -> {
                drawLine(ctx, tr, x, cy, "Left", "select/open category", w);
                drawLine(ctx, tr, x, cy + 17, "Right", "quick action menu", w);
                drawLine(ctx, tr, x, cy + 34, "Shift", "set visible main badge", w);
                drawLine(ctx, tr, x, cy + 51, "Ctrl", "toggle extra membership", w);
            }
            case DELETE -> drawDelete(ctx, tr, x, cy, w, st, mx, my);
        }
    }

    private void drawFlowTabs(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
        flowRects = new int[FLOW_LABELS.length][4];
        int bw = Math.max(34, (w - 12) / FLOW_LABELS.length);
        for (int i = 0; i < FLOW_LABELS.length; i++) {
            flowRects[i] = new int[]{x + i * (bw + 3), y, bw, 17};
            drawButton(ctx, tr, FLOW_LABELS[i], flowRects[i], true, in(flowRects[i], mx, my), flow == Flow.values()[i]);
        }
    }

    private void drawStyle(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8Accent / badge colour"), x + 10, y + 4, 0xFFFFFFFF);
        colorRects = new int[COL_ARGB.length][4];
        int cols = Math.max(8, Math.min(16, (w - 24) / 14));
        for (int i = 0; i < COL_ARGB.length; i++) {
            int sx = x + 10 + (i % cols) * 14;
            int sy = y + 20 + (i / cols) * 15;
            colorRects[i] = new int[]{sx, sy, 12, 12};
            ctx.fill(sx - 1, sy - 1, sx + 13, sy + 13, in(colorRects[i], mx, my) ? 0xFFFFFFFF : 0xFF000000);
            ctx.fill(sx, sy, sx + 12, sy + 12, COL_ARGB[i]);
        }
        drawLine(ctx, tr, x, y + 54, "Icon", "current block / custom / vanilla", w);
        drawLine(ctx, tr, x, y + 71, "Template", "Blockbench clean", w);
    }

    private void drawDelete(DrawContext ctx, TextRenderer tr, int x, int y, int w, StudioState st, int mx, int my) {
        deleteRects = new int[DELETE_LABELS.length + 1][4];
        int bw = (w - 30) / 2;
        for (int i = 0; i < DELETE_LABELS.length; i++) {
            int rx = x + 10 + (i % 2) * (bw + 6);
            int ry = y + 4 + (i / 2) * 24;
            deleteRects[i] = new int[]{rx, ry, bw, 18};
            drawButton(ctx, tr, DELETE_LABELS[i], deleteRects[i], true, in(deleteRects[i], mx, my), deleteMode == i);
        }
        int confirm = DELETE_LABELS.length;
        deleteRects[confirm] = new int[]{x + 10, y + 60, Math.min(166, w - 24), 18};
        String label = confirmDelete ? "Confirm category-only delete" : "Delete selected category";
        drawButton(ctx, tr, label, deleteRects[confirm], hasExisting(st), in(deleteRects[confirm], mx, my), false);
        if (deleteMode != 0) drawLine(ctx, tr, x, y + 77, "Now", "mode preview only", w);
    }

    private void drawPreview(DrawContext ctx, TextRenderer tr, int x, int y, int w, Row main, StudioState st) {
        drawCard(ctx, x, y, w, 224);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "fBlock Preview"), x + 9, y + 8, 0xFFFFFFFF);
        int c = main == null ? 0xFF777777 : main.color();
        ctx.fill(x + 16, y + 34, x + 72, y + 90, 0xFF1A1A1A);
        ctx.fill(x + 25, y + 43, x + 63, y + 81, st.hasBg ? st.bgArgb : c);
        int badgeY = y + 104;
        ctx.fill(x + 14, badgeY, x + w - 14, badgeY + 22, c);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(SEC + "0" + (main == null ? "NONE" : main.badge())), x + w / 2, badgeY + 7, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8main badge only"), x + 16, badgeY + 30, 0xFFFFFFFF);
        drawChip(ctx, tr, x + 14, y + 154, "building", 0xFFFFAA00);
        drawChip(ctx, tr, x + 14, y + 176, "walls", 0xFFFF8844);
    }

    private void drawChip(DrawContext ctx, TextRenderer tr, int x, int y, String label, int color) {
        ctx.fill(x, y, x + 108, y + 17, 0xFF191919);
        ctx.fill(x, y, x + 4, y + 17, color);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "7" + fit(label, tr, 86)), x + 9, y + 5, 0xFFFFFFFF);
    }

    private void drawLine(DrawContext ctx, TextRenderer tr, int x, int y, String label, String value, int w) {
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + label + ": " + SEC + "7" + fit(value, tr, w - 82)), x + 10, y + 4, 0xFFFFFFFF);
    }

    private void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, LINE);
        ctx.fill(x, y, x + w, y + h, CARD);
        ctx.fill(x, y, x + w, y + 1, 0x44FF1744);
    }

    private void drawButton(DrawContext ctx, TextRenderer tr, String label, int[] r, boolean enabled, boolean hover, boolean selected) {
        int border = selected ? ACCENT : (hover && enabled ? 0xFFBBBBBB : 0xFF000000);
        int bg = !enabled ? 0xFF151515 : (selected ? 0xFF2A1014 : CARD_2);
        ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, border);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        ctx.drawCenteredTextWithShadow(tr, Text.literal((enabled ? SEC + "f" : SEC + "8") + fit(label, tr, r[2] - 6)),
                r[0] + r[2] / 2, r[1] + 5, 0xFFFFFFFF);
    }

    private List<Row> rows(StudioState st) {
        List<Row> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String selected = mainId(st);
        add(out, seen, "building", "Building", 0, 0xFFFFAA00, "BUILD", "root", 12);
        add(out, seen, "walls", "Walls", 1, 0xFFFF8844, "WALL", "Building", 7);
        add(out, seen, selected, title(selected), selected.equals("building") || selected.equals("walls") ? 0 : 2,
                colorFor(selected, 0xFFAAB5C1), badge(selected), "Building / Walls", 3);
        for (String cat : ClientSlotCache.categories()) {
            if (out.size() >= MAX_ROWS) break;
            add(out, seen, cat, title(cat), 0, colorFor(cat, 0xFF888888), badge(cat), "existing", 1);
        }
        if (out.size() < MAX_ROWS) add(out, seen, "empty_ideas", "Empty Ideas", 0, 0xFF77A7FF, "IDEA", "empty", 0);
        return out;
    }

    private void add(List<Row> rows, Set<String> seen, String id, String name, int depth, int color, String badge, String meta, int count) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || seen.contains(key)) return;
        seen.add(key);
        rows.add(new Row(key, name, depth, color, badge, meta, count));
    }

    private Row mainRow(StudioState st) {
        String id = mainId(st);
        for (Row r : rowData) if (r.id().equalsIgnoreCase(id)) return r;
        return new Row(id, title(id), 0, colorFor(id, 0xFFAAB5C1), badge(id), "root", 0);
    }

    private String mainId(StudioState st) {
        if (st.category != null && !st.category.isBlank()) return st.category.trim().toLowerCase(Locale.ROOT);
        String def = ClientSlotCache.defaultCategory();
        return def == null || def.isBlank() ? "stone_walls" : def.trim().toLowerCase(Locale.ROOT);
    }

    private int colorFor(String cat, int fallback) {
        String tag = ClientSlotCache.colorTag(cat);
        for (int i = 0; i < COL_TAG.length; i++) if (COL_TAG[i].equals(tag)) return COL_ARGB[i];
        return fallback;
    }

    private static String title(String id) {
        if (id == null || id.isBlank()) return "Uncategorized";
        String[] parts = id.trim().replace('-', '_').split("_+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(p.charAt(0))).append(p.length() > 1 ? p.substring(1) : "");
        }
        return out.isEmpty() ? id : out.toString();
    }

    private static String badge(String id) {
        String clean = (id == null || id.isBlank()) ? "CAT" : id.replaceAll("[^a-zA-Z0-9]", "");
        return clean.length() <= 5 ? clean.toUpperCase(Locale.ROOT) : clean.substring(0, 5).toUpperCase(Locale.ROOT);
    }

    private void assignTyped(StudioState st, TextFieldWidget field) {
        String typed = sanitize(field);
        if (typed.isEmpty()) return;
        st.category = typed;
        confirmDelete = false;
        if (field != null) field.setText("");
    }

    private void renameSelected(StudioState st, TextFieldWidget field) {
        String typed = sanitize(field);
        if (typed.isEmpty() || st.category.trim().equalsIgnoreCase(typed)) return;
        send("rename", st.category, typed);
        st.category = typed;
        confirmDelete = false;
        if (field != null) field.setText("");
    }

    private static String sanitize(TextFieldWidget field) {
        return field == null ? "" : field.getText().trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
    }

    private static boolean hasMain(StudioState st) {
        return st.category != null && !st.category.isBlank();
    }

    private static boolean hasExisting(StudioState st) {
        return hasMain(st) && ClientSlotCache.categories().contains(st.category.trim().toLowerCase(Locale.ROOT));
    }

    private static String fit(String s, TextRenderer tr, int maxPx) {
        if (s == null) s = "";
        if (tr.getWidth(s) <= maxPx) return s;
        String out = s;
        while (!out.isEmpty() && tr.getWidth(out + "...") > maxPx) out = out.substring(0, out.length() - 1);
        return out.isEmpty() ? "..." : out + "...";
    }

    private static void send(String op, String cat, String arg) {
        ClientPlayNetworking.send(new CategoryAdminPayload(op, cat == null ? "" : cat.trim().toLowerCase(Locale.ROOT), arg));
    }

    private static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
