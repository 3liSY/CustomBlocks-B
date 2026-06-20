/**
 * StudioCategoryPanel.java — Group 27 §G27.6 (Block Creation Studio, "Category" tab). CLIENT-SIDE ONLY.
 *
 * The studio's category manager, drawn into the section panel area. Reads the live category list from
 * ClientSlotCache (synced by HudSync) and lets the player:
 *   - see every category as a chip (tinted by its §-colour tag, ★ marks the default),
 *   - click a chip to assign THIS new block to that category (sets StudioState.category),
 *   - type a name + "Add / Rename" to make a new category (assigned on publish) or rename the selected one,
 *   - recolour, set-as-default or delete the selected category (delete asks first).
 * Rename / colour / default / delete are server actions sent via CategoryAdminPayload; the server applies
 * them through the existing rails and re-syncs, so the chips refresh on their own.
 *
 * All controls are custom-drawn (chips + small buttons + §-colour swatches) with hit-rects computed in
 * {@link #render} and read in {@link #mouseClicked}, matching the studio's other custom pickers. The one
 * text field is owned by the screen and passed in.
 *
 * Depends on: DrawContext / TextRenderer / TextFieldWidget, ClientSlotCache, StudioState,
 *             CategoryAdminPayload, ClientPlayNetworking.
 * Called by: BlockCreationStudioScreen (Category section).
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
public final class StudioCategoryPanel {

    private static final int GOLD = 0xFF_FF_AA_00;
    // The 16 Minecraft formatting colours: { §-tag, swatch argb }.
    private static final String[] COL_TAG = {
            "§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f"};
    private static final int[] COL_ARGB = {
            0xFF000000,0xFF0000AA,0xFF00AA00,0xFF00AAAA,0xFFAA0000,0xFFAA00AA,0xFFFFAA00,0xFFAAAAAA,
            0xFF555555,0xFF5555FF,0xFF55FF55,0xFF55FFFF,0xFFFF5555,0xFFFF55FF,0xFFFFFF55,0xFFFFFFFF};

    private boolean colourMode;     // showing the §-colour swatch row
    private boolean confirmDelete;  // delete is armed (second click confirms)

    // Hit-rects {x,y,w,h}, filled in render().
    private List<int[]> chipRects = new ArrayList<>();
    private List<String> chipCats = new ArrayList<>();
    private int[][] actionRects;    // [rename, colour, default, delete]
    private int[][] colourRects;    // one per COL_ARGB when colourMode

    /** Reset transient sub-modes when the tab is (re)entered. */
    public void reset() { colourMode = false; confirmDelete = false; }

    public void render(DrawContext ctx, TextRenderer tr, int x, int y, StudioState st, TextFieldWidget field, int mx, int my) {
        chipRects = new ArrayList<>();
        chipCats = new ArrayList<>();
        actionRects = null;
        colourRects = null;

        ctx.drawTextWithShadow(tr, Text.literal("§7Category §8(click a chip to assign this block)"), x, y, 0xFFFFFFFF);

        // Chips: every existing category + the pending typed-but-new one, so it shows immediately.
        Set<String> cats = new LinkedHashSet<>(ClientSlotCache.categories());
        if (st.category != null && !st.category.isBlank()) cats.add(st.category.trim().toLowerCase(Locale.ROOT));
        String def = ClientSlotCache.defaultCategory();

        int cx = x, cy = y + 14, rowH = 16, maxX = x + 214;
        for (String cat : cats) {
            String tag = ClientSlotCache.colorTag(cat);
            String label = (tag.isEmpty() ? "§f" : tag) + cat + (cat.equals(def) ? " §6★" : "");
            int w = tr.getWidth(label.replaceAll("§.", "")) + 12;
            if (cx + w > maxX) { cx = x; cy += rowH; }
            boolean sel = cat.equalsIgnoreCase(st.category == null ? "" : st.category.trim());
            boolean hov = mx >= cx && mx < cx + w && my >= cy && my < cy + 14;
            ctx.fill(cx - 1, cy - 1, cx + w + 1, cy + 15, sel ? GOLD : (hov ? 0xFF888888 : 0xFF000000));
            ctx.fill(cx, cy, cx + w, cy + 14, sel ? 0xFF3A2E00 : 0xFF1E1E1E);
            ctx.drawTextWithShadow(tr, Text.literal(label), cx + 6, cy + 3, 0xFFFFFFFF);
            chipRects.add(new int[]{cx, cy, w, 14});
            chipCats.add(cat);
            cx += w + 4;
        }
        if (cats.isEmpty())
            ctx.drawTextWithShadow(tr, Text.literal("§8No categories yet — type a name below and Add."), x, cy, 0xFFFFFFFF);

        // FIXED layout below the chips so it lines up with the screen-owned field at y+88.
        ctx.drawTextWithShadow(tr, Text.literal("§7New category / new name:"), x, y + 74, 0xFFFFFFFF);

        int ay = y + 110; // action row, just under the field (field is at y+88, height 16)
        boolean hasSel = hasSel(st);
        // Add is ALWAYS available (assigns this block to the typed name). Rename/Colour/Default/Delete act
        // on the SELECTED existing category, so they're only enabled when one is selected. Add and Rename
        // are deliberately separate so adding a new category never silently renames the selected one.
        actionRects = new int[5][4];
        String[] labels = {"Add", "Rename", "Colour", "Set Default", confirmDelete ? "§cConfirm delete?" : "Delete"};
        boolean[] on     = {true, hasSel, hasSel, hasSel, hasSel};
        int bx = x;
        for (int i = 0; i < 5; i++) {
            int w = tr.getWidth(labels[i].replaceAll("§.", "")) + 10;
            actionRects[i] = new int[]{bx, ay, w, 16};
            boolean hov = on[i] && mx >= bx && mx < bx + w && my >= ay && my < ay + 16;
            ctx.fill(bx - 1, ay - 1, bx + w + 1, ay + 17, hov ? GOLD : 0xFF000000);
            ctx.fill(bx, ay, bx + w, ay + 16, on[i] ? 0xFF2A2A2A : 0xFF161616);
            ctx.drawTextWithShadow(tr, Text.literal((on[i] ? "§f" : "§8") + labels[i]), bx + 5, ay + 4, 0xFFFFFFFF);
            bx += w + 4;
            if (bx > maxX - 50) { bx = x; ay += 19; }
        }

        // §-colour swatch row when recolouring the selected category.
        if (colourMode && hasSel) {
            int sy = ay + 22;
            ctx.drawTextWithShadow(tr, Text.literal("§7Pick a colour for §f" + st.category + "§7:"), x, sy - 12, 0xFFFFFFFF);
            colourRects = new int[COL_ARGB.length][4];
            for (int i = 0; i < COL_ARGB.length; i++) {
                int swx = x + i * 13, swy = sy;
                colourRects[i] = new int[]{swx, swy, 12, 12};
                boolean hov = mx >= swx && mx < swx + 12 && my >= swy && my < swy + 12;
                ctx.fill(swx - 1, swy - 1, swx + 13, swy + 13, hov ? 0xFFFFFFFF : 0xFF000000);
                ctx.fill(swx, swy, swx + 12, swy + 12, COL_ARGB[i]);
            }
        }
    }

    /** Returns true if the click hit a category control. {@code field} is the screen's new-name field. */
    public boolean mouseClicked(double mx, double my, StudioState st, TextFieldWidget field) {
        // Chips → assign this block to the category.
        for (int i = 0; i < chipRects.size(); i++) {
            if (in(chipRects.get(i), mx, my)) { st.category = chipCats.get(i); confirmDelete = false; return true; }
        }
        // Colour swatches.
        if (colourRects != null) {
            for (int i = 0; i < colourRects.length; i++) {
                if (in(colourRects[i], mx, my)) {
                    send("color", st.category, COL_TAG[i]);
                    colourMode = false;
                    return true;
                }
            }
        }
        // Action buttons: [0]Add [1]Rename [2]Colour [3]Set Default [4]Delete.
        if (actionRects != null) {
            if (in(actionRects[0], mx, my)) { assignTyped(st, field); return true; }
            if (in(actionRects[1], mx, my)) { if (hasSel(st)) renameSelected(st, field); return true; }
            if (in(actionRects[2], mx, my)) { if (hasSel(st)) { colourMode = !colourMode; confirmDelete = false; } return true; }
            if (in(actionRects[3], mx, my)) { if (hasSel(st)) send("default", st.category, ""); return true; }
            if (in(actionRects[4], mx, my)) { // delete (two-click confirm)
                if (!hasSel(st)) return true;
                if (confirmDelete) { send("delete", st.category, ""); st.category = ""; confirmDelete = false; }
                else confirmDelete = true;
                return true;
            }
        }
        return false;
    }

    /** Enter in the new-name field = Add (assign this block to the typed name). Never renames. */
    public void onEnter(StudioState st, TextFieldWidget field) { assignTyped(st, field); }

    /** Assign this block to the typed category name (created on publish if brand new). Never touches others. */
    private void assignTyped(StudioState st, TextFieldWidget field) {
        String typed = sanitize(field);
        if (typed.isEmpty()) return;
        st.category = typed;
        confirmDelete = false;
        if (field != null) field.setText("");
    }

    /** Rename the SELECTED existing category to the typed name (server action). */
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

    private static boolean hasSel(StudioState st) {
        return st.category != null && !st.category.isBlank()
                && ClientSlotCache.categories().contains(st.category.trim().toLowerCase(Locale.ROOT));
    }

    private static void send(String op, String cat, String arg) {
        ClientPlayNetworking.send(new CategoryAdminPayload(op, cat == null ? "" : cat.trim().toLowerCase(Locale.ROOT), arg));
    }

    private static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
