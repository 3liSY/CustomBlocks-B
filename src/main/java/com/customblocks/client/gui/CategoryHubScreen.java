/**
 * CategoryHubScreen.java — Group 27 Category Hub (golden screen). CLIENT-ONLY.
 *
 * One red+black full-screen manager for categories. Left = a searchable, scrollable category list
 * (swatch, name tinted by its §-tag OR custom hex, live count, ★ default, + an "(uncategorized)"
 * bucket). Right = the selected category's detail, laid out in evenly-spaced sections so labels never
 * overlap their controls:
 *   RENAME  — the name box is pre-filled with the current name; edit it and press Rename (L5).
 *   ORGANISE— Merge / Move open a {@link CbPopupPicker} of existing categories, no blind typing (L6/L10).
 *   COLOUR  — 16 §-swatches + a custom "#RRGGBB" hex field (L4); one tint wins (server clears the other).
 *   OPTIONS — ★ Default is a toggle (click again to clear, L8) · Sort · Lock all · Unlock all.
 *   BLOCKS  — searchable; drag a row onto a left-list category to move it (or pick it then Move it, L10).
 * Every mutation is a {@link CategoryAdminPayload} to the authoritative server; the server broadcasts a
 * fresh HudSync so the hub refreshes live (NO-REJOIN).
 *
 * This class owns state + input + actions; all drawing (and the hit-rects the mouse handlers read back)
 * lives in {@link CategoryHubView}, which the screen static-imports its layout constants from so the
 * fields positioned in init() stay aligned with the labels drawn there.
 *
 * Depends on: CategoryHubView, CategoryHubModel, CategoryHubDragDrop, ClientSlotCache, CategoryAdminPayload,
 *             CbTheme, CbTextField, CbButton, CbHelpOverlay, CbPopupPicker, CbToast.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=CATEGORY_HUB), BlockCreationStudioScreen.
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import com.customblocks.network.payloads.CategoryAdminPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.customblocks.client.gui.CategoryHubView.BAR_H;
import static com.customblocks.client.gui.CategoryHubView.LW;
import static com.customblocks.client.gui.CategoryHubView.LX;
import static com.customblocks.client.gui.CategoryHubView.NEW_BTN_W;
import static com.customblocks.client.gui.CategoryHubView.NEW_Y_OFF;
import static com.customblocks.client.gui.CategoryHubView.Y_BLK;
import static com.customblocks.client.gui.CategoryHubView.Y_HEX;
import static com.customblocks.client.gui.CategoryHubView.Y_RENAME;

@Environment(EnvType.CLIENT)
public class CategoryHubScreen extends Screen {

    private final Screen parent;

    private String sel; // selected category key: null = none, "" = uncategorized, else a real category id
    private String pickedBlock;
    private boolean confirmDelete;
    private final CategoryHubDragDrop drag = new CategoryHubDragDrop(); // L10: drag a block onto a category row
    private final CategoryHubView view = new CategoryHubView();         // all drawing + hit-rect geometry

    private CbTextField searchField, nameField, hexField, blockSearchField, newField;

    private final CbHelpOverlay help = new CbHelpOverlay("Category Hub", List.of(
            new CbHelpOverlay.Group("BROWSE", List.of(
                    new CbHelpOverlay.Row("Click", "Select a category"),
                    new CbHelpOverlay.Row("Search", "Filter the list (or the blocks inside one)"),
                    new CbHelpOverlay.Row("Scroll", "Scroll list / blocks"))),
            new CbHelpOverlay.Group("EDIT", List.of(
                    new CbHelpOverlay.Row("Rename", "Edit the pre-filled name"),
                    new CbHelpOverlay.Row("Drag a block", "Drop it on a category to move it there"),
                    new CbHelpOverlay.Row("Merge / Move", "Or pick a target from the popup"),
                    new CbHelpOverlay.Row("Swatch / #hex", "Set the name colour"),
                    new CbHelpOverlay.Row("★ Default", "Toggle (click again clears)")))));
    private final CbPopupPicker picker = new CbPopupPicker();

    public CategoryHubScreen() { this(null); }
    public CategoryHubScreen(Screen parent) { super(Text.literal("Category Hub")); this.parent = parent; }

    /**
     * Open focused on one category (G11 C12): a category name clicked in chat lands here with its
     * key already selected, so the detail pane is showing the thing that was clicked. A blank or
     * unknown key just opens the hub with nothing selected.
     */
    public CategoryHubScreen(Screen parent, String focusKey) {
        this(parent);
        if (focusKey != null && !focusKey.isBlank()) this.sel = focusKey;
    }

    @Override
    protected void init() {
        searchField = new CbTextField(textRenderer, width - 190, 12, 150, 16, Text.literal("search"));
        searchField.setPlaceholder(Text.literal("§8search…"));
        searchField.setMaxLength(32);
        addDrawableChild(searchField);

        int rx = LX + LW + 10, rw = width - (LX + LW + 10) - 8;
        nameField = new CbTextField(textRenderer, rx, BAR_H + Y_RENAME, rw - 148, 16, Text.literal("name"));
        nameField.setPlaceholder(Text.literal("§8category name"));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);

        hexField = new CbTextField(textRenderer, rx, BAR_H + Y_HEX, 76, 14, Text.literal("hex"));
        hexField.setPlaceholder(Text.literal("§8#RRGGBB"));
        hexField.setMaxLength(7);
        addDrawableChild(hexField);

        blockSearchField = new CbTextField(textRenderer, rx + rw - 120, BAR_H + Y_BLK - 3, 120, 14, Text.literal("block search"));
        blockSearchField.setPlaceholder(Text.literal("§8search blocks…"));
        blockSearchField.setMaxLength(32);
        addDrawableChild(blockSearchField);

        // New-category input at the bottom of the left rail (always visible); its "+ New" button is drawn by the view.
        newField = new CbTextField(textRenderer, LX, height - NEW_Y_OFF, LW - NEW_BTN_W - 4, 16, Text.literal("new category"));
        newField.setPlaceholder(Text.literal("§8new category name…"));
        newField.setMaxLength(32);
        addDrawableChild(newField);

        addDrawableChild(CbButton.normal(Text.literal("?"), width - 24, 12, 16, 16, b -> help.toggle()));
        addDrawableChild(CbButton.normal(Text.literal("Close"), width - 70, height - 24, 62, 18, b -> close()));
        if (sel != null) syncFieldsToSelection();
        updateFieldVisibility(); // detail fields hidden until a category is selected (no overlap on the overview)
    }

    /** Detail-pane fields belong to the selected state only — hidden on the overview so they never draw over it. */
    private void updateFieldVisibility() {
        boolean detail = sel != null;
        if (nameField != null) nameField.visible = detail;
        if (hexField != null) hexField.visible = detail;
        if (blockSearchField != null) blockSearchField.visible = detail;
    }

    // ── render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* full black drawn in render */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        view.render(ctx, mx, my, this, textRenderer, width, height); // bar + list + detail + footer

        super.render(ctx, mx, my, delta); // fields + [?] + Close
        help.render(ctx, width, height, textRenderer, mx, my);
        picker.render(ctx, width, height, textRenderer, mx, my);

        // L10 drag-drop: a small tag follows the cursor while a block is being dragged.
        if (drag.isActive() && drag.draggedId() != null)
            drag.renderTag(ctx, textRenderer, mx, my, blockName(drag.draggedId()), CbTheme.ACCENT);
    }

    // ── package-private accessors — CategoryHubView reads live state while rendering ────────────────
    String sel() { return sel; }
    String pickedBlock() { return pickedBlock; }
    boolean confirmDelete() { return confirmDelete; }
    CategoryHubDragDrop drag() { return drag; }
    String searchText() { return fieldText(searchField); }
    String nameText() { return fieldText(nameField); }
    String blockSearchText() { return fieldText(blockSearchField); }
    String newText() { return fieldText(newField); }

    // ── input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (picker.mouseClicked(mx, my)) return true;
        if (help.mouseClicked(mx, my)) return true;
        if (super.mouseClicked(mx, my, button)) return true; // fields + [?] + Close

        for (int i = 0; i < view.rowRects.size(); i++) {
            if (in(mx, my, view.rowRects.get(i))) { select(view.rowKeys.get(i)); return true; }
        }
        if (in(mx, my, view.rNew)) { createPending(); return true; }

        if (sel != null) {
            String key = realKey(sel);
            boolean real = !CategoryHubModel.UNCATEGORIZED.equals(sel);
            if (real && in(mx, my, view.rRename)) { doRename(key); return true; }
            if (real && in(mx, my, view.rDelete)) { doDelete(key); return true; }
            if (in(mx, my, view.rMerge)) { openMergePicker(); return true; }
            if (in(mx, my, view.rMove) && pickedBlock != null) { openMovePicker(); return true; }
            if (real && in(mx, my, view.rDefault)) { send("default", key, ""); return true; }
            if (real && in(mx, my, view.rSort))   { send("sort", key, "custom".equalsIgnoreCase(ClientSlotCache.sortOrder(key)) ? "alpha" : "custom"); return true; }
            if (real && in(mx, my, view.rLock))   { send("lock", key, ""); return true; }
            if (real && in(mx, my, view.rUnlock)) { send("unlock", key, ""); return true; }
            if (real && in(mx, my, view.rClear))  { send("color", key, ""); return true; }
            if (real && in(mx, my, view.rSetHex)) { doSetHex(key); return true; }
            if (real) for (int i = 0; i < view.swatchRects.length; i++)
                if (in(mx, my, view.swatchRects[i])) { send("color", key, CategoryHubModel.TAGS[i]); return true; }
            for (int i = 0; i < view.blockRects.size(); i++)
                if (in(mx, my, view.blockRects.get(i))) {
                    pickedBlock = view.blockIds.get(i);
                    drag.press(pickedBlock, mx, my); // arm a possible drag; click-then-Move still works if no drag
                    return true;
                }
        }
        return false;
    }

    /** L10 drag-drop: once the mouse actually moves, track which left-list row it's hovering. */
    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag.drag(mx, my, this::hoveredRowKey)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    /** L10 drag-drop: releasing over a category row assigns the dragged block there; releasing anywhere
     *  else just cancels the drag (the picked block stays picked, so the click-then-Move path still works). */
    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag.release((blockId, targetCat) -> { send("assign", targetCat, blockId); pickedBlock = null; })) return true;
        return super.mouseReleased(mx, my, button);
    }

    private String hoveredRowKey(double mx, double my) {
        for (int i = 0; i < view.rowRects.size(); i++) if (in(mx, my, view.rowRects.get(i))) return view.rowKeys.get(i);
        return null;
    }

    private String blockName(String id) {
        for (ClientSlotCache.Entry e : ClientSlotCache.entries()) if (e.id().equals(id)) return e.name();
        return id;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (picker.mouseScrolled(vAmt)) return true;
        int step = (int) -Math.signum(vAmt);
        if (mx < LX + LW + 5) view.scrollList(step);
        else view.scrollBlocks(step);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (picker.isOpen()) { if (key == GLFW.GLFW_KEY_ESCAPE) picker.close(); return true; }
        if (help.isOpen()) { if (key == GLFW.GLFW_KEY_ESCAPE) help.close(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && !(getFocused() instanceof CbTextField)) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // ── actions ──────────────────────────────────────────────────────────────
    private void select(String key) {
        sel = key; confirmDelete = false; pickedBlock = null; view.resetBlockScroll();
        syncFieldsToSelection();
        updateFieldVisibility();
    }

    /** Pre-fill the name box with the current name (L5 inline rename) and the hex box with the saved tint. */
    private void syncFieldsToSelection() {
        if (sel == null) return;
        boolean real = !CategoryHubModel.UNCATEGORIZED.equals(sel);
        if (nameField != null) nameField.setText(real ? CategoryHubModel.titleCase(sel) : "");
        if (hexField != null) hexField.setText(ClientSlotCache.colorHex(realKey(sel)));
    }

    private void createPending() {
        String id = CategoryHubModel.normalizeId(fieldText(newField));
        if (id.isEmpty()) { CbToast.info("Type a category name first."); return; }
        send("create", id, "");
        if (newField != null) newField.setText("");
        select(id);
        CbToast.info("Created '" + id + "' — drag a block onto it, or pick one and press Move.");
    }

    private void doRename(String key) {
        String to = CategoryHubModel.normalizeId(fieldText(nameField));
        if (to.isEmpty() || to.equals(key)) { CbToast.info("Edit the name first."); return; }
        send("rename", key, to); select(to);
    }

    private void doDelete(String key) {
        if (!confirmDelete) { confirmDelete = true; return; }
        send("delete", key, ""); confirmDelete = false;
        // L9: the blocks are NOT lost — the server keeps them, now uncategorized. Jump to that bucket so they
        // stay visible instead of leaving a blank pane (which read as "the blocks disappeared").
        boolean hadBlocks = !CategoryHubModel.blocks(key).isEmpty();
        select(hadBlocks ? CategoryHubModel.UNCATEGORIZED : null);
        if (hadBlocks) CbToast.info("Deleted — its blocks are safe here in (uncategorized).");
    }

    private void doSetHex(String key) {
        String hex = com.customblocks.core.CategoryMetadataStore.normalizeHex(fieldText(hexField));
        if (hex.isEmpty()) { CbToast.info("Enter a hex like #40FF00."); return; }
        send("colorhex", key, hex);
    }

    /** Merge: choose an existing category to fold this one into (or the picked-block move target). */
    private void openMergePicker() {
        String key = realKey(sel);
        List<CbPopupPicker.Item> items = pickerItems(key);
        String cand = CategoryHubModel.normalizeId(fieldText(nameField));
        String candLabel = cand.equals(key) ? "" : CategoryHubModel.titleCase(cand);
        picker.open(CategoryHubModel.UNCATEGORIZED.equals(sel) ? "Empty into…" : "Merge into…", items,
                cand.equals(key) ? "" : cand, candLabel, target -> {
            if (CategoryHubModel.UNCATEGORIZED.equals(sel)) gatherUncategorizedInto(target);
            else { send("merge", key, target); select(target); }
        });
    }

    private void openMovePicker() {
        if (pickedBlock == null) return;
        List<CbPopupPicker.Item> items = pickerItems(null);
        String cand = CategoryHubModel.normalizeId(fieldText(nameField));
        picker.open("Move '" + fit(pickedBlock, 40) + "' into…", items, cand, CategoryHubModel.titleCase(cand), target -> {
            send("assign", target, pickedBlock);
            pickedBlock = null;
        });
    }

    /** Assign every block in the uncategorized bucket to {@code target} (client-side loop, server-authoritative). */
    private void gatherUncategorizedInto(String target) {
        for (ClientSlotCache.Entry e : CategoryHubModel.blocks(CategoryHubModel.UNCATEGORIZED)) send("assign", target, e.id());
        select(target);
    }

    /** Existing categories as picker items (optionally excluding {@code exclude}), swatch-tinted. */
    private List<CbPopupPicker.Item> pickerItems(String exclude) {
        List<CbPopupPicker.Item> items = new ArrayList<>();
        for (CategoryHubModel.Row r : CategoryHubModel.rows("")) {
            if (CategoryHubModel.UNCATEGORIZED.equals(r.key())) continue;
            if (exclude != null && r.key().equalsIgnoreCase(exclude)) continue;
            items.add(new CbPopupPicker.Item(r.key(), r.name(), r.colorArgb(), r.count()));
        }
        return items;
    }

    private void send(String op, String cat, String arg) {
        ClientPlayNetworking.send(new CategoryAdminPayload(op, cat == null ? "" : cat, arg == null ? "" : arg));
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private String realKey(String s) { return s == null ? "" : s; }
    private String fieldText(CbTextField f) { return f == null ? "" : f.getText(); }

    /** Trim to fit maxPx (used for the Move-picker title; the drawing copy lives in CategoryHubView). */
    private String fit(String s, int maxPx) {
        if (s == null) s = "";
        if (textRenderer.getWidth(s) <= maxPx) return s;
        while (!s.isEmpty() && textRenderer.getWidth(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static boolean in(double mx, double my, int[] r) { return r != null && in(mx, my, r[0], r[1], r[2], r[3]); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return false; }
}
