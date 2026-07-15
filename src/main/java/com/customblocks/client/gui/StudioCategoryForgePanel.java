/**
 * StudioCategoryForgePanel.java - G11-3 Category Forge sample for the /cb create Category tab.
 *
 * This is intentionally a small in-game slice of the locked mockup, not the final category data
 * migration. The player can preview the richer tree/main-badge/editor shape while publishing still
 * uses the existing single category rail through StudioState.category.
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
public final class StudioCategoryForgePanel {

    private enum Flow { BASIC, STYLE, RULES, SHORTCUTS, DELETE }
    private record Row(String id, String name, int depth, int color, String badge, String meta, int count) {}

    private static final int GOLD = CbTheme.ACCENT;
    private static final int WIDTH = 214;
    private static final int MAX_ROWS = 4;
    private static final String SEC = "\u00a7";

    private static final String[] COL_TAG = {
            SEC + "0", SEC + "1", SEC + "2", SEC + "3", SEC + "4", SEC + "5", SEC + "6", SEC + "7",
            SEC + "8", SEC + "9", SEC + "a", SEC + "b", SEC + "c", SEC + "d", SEC + "e", SEC + "f"};
    private static final int[] COL_ARGB = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF};
    private static final String[] FLOW_LABELS = {"Basic", "Style", "Rules", "Keys", "Delete"};
    private static final String[] DELETE_LABELS = {"Category only", "Exclusive blocks", "All shown", "Move first"};

    private Flow flow = Flow.BASIC;
    private boolean confirmDelete;
    private int deleteMode;

    private List<int[]> rowRects = new ArrayList<>();
    private List<Row> rowData = new ArrayList<>();
    private int[][] actionRects;
    private int[][] flowRects;
    private int[][] colourRects;
    private int[][] deleteRects;

    /** Reset transient sub-modes when the tab is (re)entered. */
    public void reset() { confirmDelete = false; }

    public void render(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, TextFieldWidget field, int mx, int my) {
        rowRects = new ArrayList<>();
        rowData = rows(st);
        actionRects = null;
        flowRects = null;
        colourRects = null;
        deleteRects = null;

        Row main = mainRow(st);
        drawHeader(ctx, tr, x, y);
        drawBlockSample(ctx, tr, x, y + 22, st, main);
        drawTree(ctx, tr, x, y + 70, st, mx, my);

        ctx.drawTextWithShadow(tr, Text.literal(SEC + "7New category / rename:"), x, y + 162, 0xFFFFFFFF);
        drawActions(ctx, tr, x, y + 194, st, mx, my);
        drawFlowTabs(ctx, tr, x, y + 218, mx, my);
        drawFlow(ctx, tr, x, y + 242, st, main, mx, my);
    }

    /** Returns true if the click hit a category control. {@code field} is the screen's new-name field. */
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
            if (in(actionRects[3], mx, my)) { st.category = mainRow(st).id(); return true; }
        }
        if (colourRects != null) {
            for (int i = 0; i < colourRects.length; i++) {
                if (in(colourRects[i], mx, my)) {
                    if (hasMain(st)) send("color", st.category, COL_TAG[i]);
                    return true;
                }
            }
        }
        if (deleteRects != null) {
            for (int i = 0; i < DELETE_LABELS.length; i++) {
                if (in(deleteRects[i], mx, my)) { deleteMode = i; confirmDelete = false; return true; }
            }
            if (deleteRects.length > DELETE_LABELS.length && in(deleteRects[DELETE_LABELS.length], mx, my)) {
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

    /** Enter in the new-name field = Add (assign this block to the typed name). Never renames. */
    public void onEnter(StudioState st, TextFieldWidget field) { assignTyped(st, field); }

    private void drawHeader(DrawContext ctx, TextRenderer tr, int x, int y) {
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "c" + SEC + "lCategory Forge " + SEC + "8G11-3 sample"), x, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "7Tree + one visible badge; full model waits."), x, y + 11, 0xFFFFFFFF);
    }

    private void drawBlockSample(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, Row main) {
        String id = st.id == null || st.id.isBlank() ? "stone_trim" : st.id;
        String name = st.name == null || st.name.isBlank() ? "Stone Trim" : st.name;
        int badgeColor = main == null ? 0xFF444444 : main.color();
        ctx.fill(x, y, x + WIDTH, y + 39, 0xAA101010);
        ctx.fill(x, y, x + WIDTH, y + 1, 0x55444444);
        ctx.fill(x + 6, y + 6, x + 34, y + 34, 0xFF1A1A1A);
        ctx.fill(x + 10, y + 10, x + 30, y + 30, st.hasBg ? st.bgArgb : badgeColor);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "f" + fit(name, tr, 92)), x + 40, y + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + fit(id, tr, 92)), x + 40, y + 17, 0xFFFFFFFF);
        int bx = x + 148;
        ctx.fill(bx, y + 7, x + WIDTH - 6, y + 24, badgeColor);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(SEC + "0" + (main == null ? "NONE" : main.badge())), bx + (WIDTH - 154) / 2, y + 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8also: building, walls"), x + 40, y + 29, 0xFFFFFFFF);
    }

    private void drawTree(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "7Category tree / picker"), x, y, 0xFFFFFFFF);
        int ry = y + 14;
        for (int i = 0; i < Math.min(MAX_ROWS, rowData.size()); i++) {
            Row r = rowData.get(i);
            int rx = x + r.depth() * 16;
            int rw = WIDTH - r.depth() * 16;
            boolean selected = r.id().equalsIgnoreCase(st.category == null ? "" : st.category.trim());
            boolean hover = mx >= rx && mx < rx + rw && my >= ry && my < ry + 18;
            ctx.fill(rx - 1, ry - 1, rx + rw + 1, ry + 19, selected ? GOLD : (hover ? 0xFF777777 : 0xFF000000));
            ctx.fill(rx, ry, rx + rw, ry + 18, selected ? 0xFF2A1014 : 0xFF191919);
            ctx.fill(rx + 3, ry + 3, rx + 14, ry + 15, r.color());
            String line = (selected ? SEC + "f" : SEC + "7") + fit(r.name(), tr, rw - 82);
            ctx.drawTextWithShadow(tr, Text.literal(line), rx + 20, ry + 5, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + r.count()), rx + rw - 18, ry + 5, 0xFFFFFFFF);
            rowRects.add(new int[]{rx, ry, rw, 18});
            ry += 21;
        }
    }

    private void drawActions(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        String[] labels = {"Add", "Rename", "Default", "Use Main"};
        boolean[] enabled = {true, hasExisting(st), hasMain(st), true};
        actionRects = new int[labels.length][4];
        int bx = x;
        for (int i = 0; i < labels.length; i++) {
            int w = Math.max(42, tr.getWidth(labels[i]) + 10);
            actionRects[i] = new int[]{bx, y, w, 16};
            drawButton(ctx, tr, labels[i], actionRects[i], enabled[i], in(actionRects[i], mx, my), false);
            bx += w + 4;
        }
    }

    private void drawFlowTabs(DrawContext ctx, TextRenderer tr, int x, int y, int mx, int my) {
        flowRects = new int[FLOW_LABELS.length][4];
        int bx = x;
        for (int i = 0; i < FLOW_LABELS.length; i++) {
            int w = i == 3 ? 34 : 40;
            flowRects[i] = new int[]{bx, y, w, 16};
            drawButton(ctx, tr, FLOW_LABELS[i], flowRects[i], true, in(flowRects[i], mx, my), flow == Flow.values()[i]);
            bx += w + 3;
        }
    }

    private void drawFlow(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, Row main, int mx, int my) {
        ctx.fill(x, y, x + WIDTH, y + 88, 0x66101010);
        ctx.fill(x, y, x + WIDTH, y + 1, 0x55444444);
        switch (flow) {
            case BASIC -> {
                drawLine(ctx, tr, x, y, "Name/key", main == null ? "none selected" : main.id());
                drawLine(ctx, tr, x, y + 15, "Parent", main == null ? "root" : main.meta());
                drawLine(ctx, tr, x, y + 30, "Badge", main == null ? "NONE" : main.badge() + " only visible badge");
                drawLine(ctx, tr, x, y + 45, "Members", "main + building + walls sample");
                ctx.drawTextWithShadow(tr, Text.literal(SEC + "8Enter in the field adds a category."), x + 6, y + 66, 0xFFFFFFFF);
            }
            case STYLE -> {
                ctx.drawTextWithShadow(tr, Text.literal(SEC + "7Accent / badge colour"), x + 6, y + 7, 0xFFFFFFFF);
                colourRects = new int[COL_ARGB.length][4];
                for (int i = 0; i < COL_ARGB.length; i++) {
                    int sx = x + 6 + i * 13, sy = y + 22;
                    colourRects[i] = new int[]{sx, sy, 12, 12};
                    ctx.fill(sx - 1, sy - 1, sx + 13, sy + 13, in(colourRects[i], mx, my) ? 0xFFFFFFFF : 0xFF000000);
                    ctx.fill(sx, sy, sx + 12, sy + 12, COL_ARGB[i]);
                }
                drawLine(ctx, tr, x, y + 43, "Icon", "current block / vanilla / custom");
                drawLine(ctx, tr, x, y + 58, "Template", "Blockbench clean");
            }
            case RULES -> {
                drawLine(ctx, tr, x, y, "Auto-add", "stone, wall, brick, trim");
                drawLine(ctx, tr, x, y + 15, "Depth", "2 OK; deeper warns");
                drawLine(ctx, tr, x, y + 30, "Visibility", "shown in create + browser");
                drawLine(ctx, tr, x, y + 45, "Permissions", "admins edit, players browse");
            }
            case SHORTCUTS -> {
                drawLine(ctx, tr, x, y, "Left", "open / select");
                drawLine(ctx, tr, x, y + 15, "Right", "quick actions");
                drawLine(ctx, tr, x, y + 30, "Shift", "set visible main badge");
                drawLine(ctx, tr, x, y + 45, "Ctrl", "toggle extra membership");
                ctx.drawTextWithShadow(tr, Text.literal(SEC + "8Lore sample for the final picker."), x + 6, y + 66, 0xFFFFFFFF);
            }
            case DELETE -> drawDelete(ctx, tr, x, y, st, mx, my);
        }
    }

    private void drawDelete(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, int mx, int my) {
        deleteRects = new int[DELETE_LABELS.length + 1][4];
        for (int i = 0; i < DELETE_LABELS.length; i++) {
            int col = i % 2, row = i / 2;
            int rx = x + 6 + col * 101, ry = y + 7 + row * 22;
            deleteRects[i] = new int[]{rx, ry, 96, 18};
            drawButton(ctx, tr, DELETE_LABELS[i], deleteRects[i], true, in(deleteRects[i], mx, my), deleteMode == i);
        }
        int confirmIndex = DELETE_LABELS.length;
        deleteRects[confirmIndex] = new int[]{x + 6, y + 58, 150, 18};
        String label = confirmDelete ? "Confirm category-only delete" : "Delete selected category";
        drawButton(ctx, tr, label, deleteRects[confirmIndex], hasExisting(st), in(deleteRects[confirmIndex], mx, my), false);
        if (deleteMode != 0)
            ctx.drawTextWithShadow(tr, Text.literal(SEC + "8Mode preview only in this sample."), x + 6, y + 78, 0xFFFFFFFF);
    }

    private void drawLine(DrawContext ctx, TextRenderer tr, int x, int y, String label, String value) {
        ctx.drawTextWithShadow(tr, Text.literal(SEC + "8" + label + ": " + SEC + "7" + fit(value, tr, 142)), x + 6, y + 7, 0xFFFFFFFF);
    }

    private void drawButton(DrawContext ctx, TextRenderer tr, String label, int[] r, boolean enabled, boolean hover, boolean selected) {
        int border = selected ? GOLD : (hover && enabled ? 0xFFBBBBBB : 0xFF000000);
        int bg = !enabled ? 0xFF151515 : (selected ? 0xFF2A1014 : 0xFF242424);
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
            add(out, seen, cat, title(cat), 0, colorFor(cat, 0xFF777777), badge(cat), "existing", 1);
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
