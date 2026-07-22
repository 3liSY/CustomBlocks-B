/**
 * BulkWorkbenchScreen.java — Group 07 (Bulk Operations Hub). CLIENT-ONLY.
 *
 * The one Screen replacing the chest bulk menus + block list: two LEFT-rail tabs (TABS_ON_LEFT) over one shared
 * target set — BLOCKS LIST (§G27.22 cube-tile grid) and BULK ACTIONS (§G27.22b op rail + include-checkbox rows +
 * RESULT-PREVIEW panel). It holds no authoritative state: every Execute ships a {@link BulkActionPayload}; the
 * server runs the real handler and answers with a snapshot that {@link #refresh(String)} re-renders in place.
 *
 * Depends on: BulkWorkbenchView, BulkOpsView, BulkCube, BulkWorkbenchModel, BulkOpSpec, BulkDraw, BulkAction.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=BULK_WORKBENCH → open or refresh).
 */
package com.customblocks.client.gui;

import com.customblocks.client.ClientSlotCache;
import com.customblocks.network.payloads.BulkActionPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_X;
import static com.customblocks.client.gui.BulkWorkbenchView.CONTENT_Y;
import static com.customblocks.client.gui.BulkWorkbenchView.contentBottom;

@Environment(EnvType.CLIENT)
public final class BulkWorkbenchScreen extends Screen {

    static final int TAB_BROWSE = 0, TAB_BULK = 1;
    private static final long SWEEP_MS = 900L;            // §G27.22b Execute sweep duration

    private final BulkWorkbenchView view = new BulkWorkbenchView();
    final BulkOpsView ops = new BulkOpsView();
    private final BulkCube cube = new BulkCube();          // shared spinning-cube renderer (disposed in removed())

    int tab = TAB_BROWSE;
    boolean pick;                                            // Group 12 hand-off: Blocks List, with "Use these"
    private final Set<String> ticked = new LinkedHashSet<>(); // shared target set, both tabs (TG B1)
    private final Set<String> excluded = new HashSet<>();     // §G27.22b per-row include-off (dropped from N/Execute)
    private Set<String> locked = Set.of();
    private Set<String> fav = Set.of();
    private String detailId;

    // Bulk Actions intent — never authoritative, re-validated server-side. Package-visible for BulkConsoleInput + NL bar.
    int opIndex = BulkOpSpec.OP_PROPERTY;
    String property = "glow", value = "8";
    String textMode = "prefix";
    String exportFormat = "json";
    String lockMode = "lock", favMode = "favorite";
    int recolorHue = 0;                                      // §G27.22b Recolor hue (degrees)
    // The tab has no filter builder and the server only ever receives an explicit id list (§G07-B rip, 2026-07-20).
    String browseCategory;  // §G07-B2: Blocks List Category ▾ filter, combines with browseFilter (AND)

    boolean confirmOpen;
    private boolean draggingScroll;   // A2: a list scrollbar thumb is being dragged
    private boolean draggingHue;      // Recolor hue slider is being dragged
    private String resultMsg;         // X3: last op's result, shown as an in-screen dismissable modal
    private boolean selAllOpen;       // Blocks List "Select ▾" dropdown is open (§G27.22)
    private String browseFilter = "all";   // §G27.22 filter chip: all | fav | locked | selected
    private String browseSort = "name";    // §G27.22 Sort ▾: name | id | newest | color
    private boolean sortOpen;              // §G27.22 Sort ▾ dropdown is open
    boolean categoryOpen;                  // §G07-B2 Category ▾ dropdown is open
    private boolean executing;             // §G27.22b Execute sweep is animating
    private long executeStart;
    private int executeN;

    // §G07-4 history + toast, and the Bulk-actions companion — split to hold the ≤500-line cap.
    private final BulkHistoryOverlay hist = new BulkHistoryOverlay();
    private final BulkConsoleInput con = new BulkConsoleInput(this);

    // Text-field contents live here (not the widgets): init() rebuilds the widgets on op change without wiping input.
    String search = "", textA = "", textB = "";

    // Recomputed once per frame in render() (Bulk Actions tab only).
    private List<BulkWorkbenchModel.PreviewRow> previewCache = List.of();
    private Map<String, BulkWorkbenchModel.PreviewRow> previewById = Map.of();
    private List<String> scopeCache = List.of();

    public BulkWorkbenchScreen(String data) {
        super(Text.literal("Bulk Operations Hub"));
        parse(data);
    }

    /**
     * True when this snapshot is a real open request (a named tab), false for the blank-tab live-refresh an
     * Apply broadcasts to every player (§G07-4 MP rule) — so a bystander's op never pops a closed Hub open.
     */
    public static boolean wantsOpen(String data) {
        try {
            JsonObject root = JsonParser.parseString(data == null ? "{}" : data).getAsJsonObject();
            return root.has("tab") && !root.get("tab").getAsString().isBlank();
        } catch (Exception e) {
            return true;
        }
    }

    /** Re-render in place from a fresh server snapshot (keeps the screen and the targets). */
    public void refresh(String data) {
        parse(data);
        ticked.removeIf(id -> BulkWorkbenchModel.byId(id) == null);
        excluded.removeIf(id -> !ticked.contains(id));
        if (detailId != null && BulkWorkbenchModel.byId(detailId) == null) detailId = null;
        confirmOpen = false;
        if (client != null) rebuild();
    }

    private void parse(String data) {
        try {
            JsonObject root = JsonParser.parseString(data == null ? "{}" : data).getAsJsonObject();
            locked = ids(root, "locked");
            fav = ids(root, "fav");
            String t = root.has("tab") ? root.get("tab").getAsString() : "";
            if (!t.isBlank()) {
                pick = "pick".equals(t);
                if ("bulk".equals(t)) {                       // bare /cb bulk → Bulk Actions on the default op
                    tab = TAB_BULK;
                } else if (t.startsWith("bulk:")) {           // named bulk* → its own op
                    tab = TAB_BULK; opIndex = BulkOpSpec.railForKey(t.substring(5));
                } else {
                    tab = TAB_BROWSE;
                }
            }
            if (root.has("result")) {                          // X3: relay the last op's outcome into an in-screen modal
                String r = root.get("result").getAsString();
                if (!r.isBlank()) resultMsg = r;
            }
            hist.parse(root);                                   // §G07-4 toast + undo/redo history stacks
        } catch (Exception ignored) {
            locked = Set.of();
            fav = Set.of();
        }
    }

    private static Set<String> ids(JsonObject root, String key) {
        Set<String> out = new HashSet<>();
        if (root.has(key) && root.get(key).isJsonArray()) {
            JsonArray a = root.getAsJsonArray(key);
            for (JsonElement e : a) if (e.isJsonPrimitive()) out.add(e.getAsString());
        }
        return out;
    }

    // ── state read by the views ──────────────────────────────────────────────
    int tab() { return tab; }
    boolean pickMode() { return pick; }
    Set<String> ticked() { return ticked; }
    Set<String> excluded() { return excluded; }
    Set<String> locked() { return locked; }
    Set<String> fav() { return fav; }
    String detailId() { return detailId; }
    String searchText() { return search; }
    BulkWorkbenchView view() { return view; }
    BulkCube cube() { return cube; }
    int opIndex() { return opIndex; }
    String property() { return property; }
    String value() { return value; }
    String textMode() { return textMode; }
    String textA() { return textA; }
    String textB() { return textB; }
    String exportFormat() { return exportFormat; }
    String lockMode() { return lockMode; }
    String favMode() { return favMode; }
    int recolorHue() { return recolorHue; }
    boolean selAllOpen() { return selAllOpen; }
    String browseFilter() { return browseFilter; }
    String browseSort() { return browseSort; }
    boolean sortOpen() { return sortOpen; }
    boolean categoryOpen() { return categoryOpen; }
    boolean histOpen() { return hist.histOpen(); }
    int undoHistSize() { return hist.undoSize(); }
    String browseCategory() { return browseCategory; }
    List<BulkWorkbenchModel.PreviewRow> preview() { return previewCache; }
    BulkWorkbenchModel.PreviewRow previewFor(String id) { return previewById.get(id); }
    List<String> scopeIds() { return scopeCache; }
    void clearExcluded() { excluded.clear(); }
    int executeCount() { return executeN; }
    float executeProgress() {
        if (!executing) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - executeStart) / (float) SWEEP_MS));
    }

    boolean canApply() {
        if (scopeCache.isEmpty()) return false;
        return switch (opIndex) {
            case BulkOpSpec.OP_RENAME, BulkOpSpec.OP_CATEGORY, BulkOpSpec.OP_REID -> !textA.isBlank();
            case BulkOpSpec.OP_RECOLOR -> recolorHue > 0;   // 0° is a no-op the server refuses
            default -> true;
        };
    }

    // ── layout ───────────────────────────────────────────────────────────────
    void rebuild() { clearChildren(); init(); }

    @Override
    protected void init() {
        field(width - 190, 12, 150, 16, "§8search…", 48, search, v -> { search = v; view.resetScroll(); ops.resetScrolls(); });
        if (tab != TAB_BULK) return;            // only Bulk Actions has op controls

        switch (opIndex) {
            case BulkOpSpec.OP_RENAME -> {
                String aHint = switch (textMode) { case "prefix" -> "e.g. red_"; case "suffix" -> "e.g. _v2"; default -> "find this"; };
                field(BulkOpsView.ctlX(1), BulkOpsView.ctlY(), BulkOpsView.ctlW(), 16, "§8" + aHint, 64, textA, v -> textA = v);
                if ("replace".equals(textMode))
                    field(BulkOpsView.ctlX(2), BulkOpsView.ctlY(), BulkOpsView.ctlW(), 16, "§8replace with", 64, textB, v -> textB = v);
            }
            case BulkOpSpec.OP_CATEGORY ->
                    field(BulkOpsView.ctlX(1), BulkOpsView.ctlY(), BulkOpsView.ctlW(), 16, "§8category name", 32, textA, v -> textA = v);
            case BulkOpSpec.OP_REID ->
                    field(BulkOpsView.ctlX(1), BulkOpsView.ctlY(), BulkOpsView.ctlW() + 40, 16, "§8e.g. planet_{n}", 48, textA, v -> textA = v);
            default -> { }
        }
    }

    private void field(int x, int y, int w, int h, String placeholder, int max, String initial,
                       java.util.function.Consumer<String> sink) {
        CbTextField f = new CbTextField(textRenderer, x, y, w, h, Text.literal(placeholder));
        f.setPlaceholder(Text.literal(placeholder));
        f.setMaxLength(max);
        f.setText(initial);
        f.setChangedListener(sink);
        addDrawableChild(f);
    }

    // ── render ───────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* black drawn in render */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (tab == TAB_BULK) recompute();
        view.renderChrome(ctx, textRenderer, mx, my, this, width, height);
        switch (tab) {
            case TAB_BULK -> ops.render(ctx, textRenderer, mx, my, this, width, height);
            default       -> view.renderBrowse(ctx, textRenderer, mx, my, this, width, height);
        }
        super.render(ctx, mx, my, delta);
        if (executing && tab == TAB_BULK) {
            ops.renderSweep(ctx, textRenderer, this, width, height);
            if (System.currentTimeMillis() - executeStart > SWEEP_MS + 500) executing = false;
        }
        if (confirmOpen && tab == TAB_BULK) ops.renderModal(ctx, textRenderer, mx, my, this, width, height);
        if (resultMsg != null) ops.renderResultModal(ctx, textRenderer, resultMsg, mx, my, width, height);
        hist.render(ctx, textRenderer, mx, my, width, height);   // §G07-4 history overlay + toast (topmost)
    }

    /** One pass per frame (Bulk Actions): the included scope (ticked − excluded) and its live old→new preview. */
    private void recompute() {
        List<String> scope = new ArrayList<>();
        for (String id : ticked) if (!excluded.contains(id)) scope.add(id);   // locked kept in → preview marks skip
        scopeCache = scope;

        previewCache = opIndex == BulkOpSpec.OP_REID
                ? BulkWorkbenchModel.reidPreview(scope, textA, locked)
                : BulkWorkbenchModel.preview(opIndex, BulkAction.p1(this), BulkAction.p2(this), BulkAction.p3(this), scope, locked, fav);
        Map<String, BulkWorkbenchModel.PreviewRow> byId = new HashMap<>();
        for (BulkWorkbenchModel.PreviewRow r : previewCache) byId.put(r.id(), r);
        previewById = byId;
    }

    private void startExecute() {
        executeN = BulkOpText.impact(this)[0];
        executeStart = System.currentTimeMillis();
        executing = true;
        BulkAction.send(this);
    }

    // ── input ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (resultMsg != null) {   // X3 result modal is topmost — dismiss on OK, swallow everything else
            if (BulkDraw.in(mx, my, ops.rResultOk)) { CbUiSounds.click(); resultMsg = null; }
            return true;
        }
        if (confirmOpen) return modalClick(mx, my);
        if (hist.click(mx, my, view.rHistory)) return true;   // §G07-4 history overlay + toast (topmost)
        if (super.mouseClicked(mx, my, button)) return true;

        if (BulkDraw.in(mx, my, view.rClose)) { close(); return true; }
        for (int i = 0; i < view.rTabs.length; i++) {
            if (BulkDraw.in(mx, my, view.rTabs[i]) && (!pick || i == TAB_BROWSE)) { switchTab(i); return true; }
        }

        // A2: grab the list scrollbar thumb — the drag then continues in mouseDragged.
        int[] sb = tab == TAB_BROWSE ? view.rListScroll : ops.rListScroll;
        if (sb != null && BulkDraw.in(mx, my, sb)) {
            draggingScroll = true;
            if (tab == TAB_BROWSE) view.scrollToMouse(my); else ops.scrollToMouse(my);
            return true;
        }

        return switch (tab) {
            case TAB_BULK -> bulkClick(mx, my, button);
            default       -> browseClick(mx, my, button);
        };
    }

    private boolean modalClick(double mx, double my) {
        if (BulkDraw.in(mx, my, ops.rModalConfirm)) { CbUiSounds.chime(); confirmOpen = false; startExecute(); return true; }
        if (BulkDraw.in(mx, my, ops.rModalCancel)) { CbUiSounds.click(); confirmOpen = false; return true; }
        return true; // the modal is modal — nothing behind it is clickable
    }

    private boolean browseClick(double mx, double my, int button) {
        // The Sort ▾ menu overlays everything — resolve it first (§G27.22).
        if (sortOpen) {
            if (setSort(mx, my)) return true;
            sortOpen = false; // a click elsewhere closes it, then still lands below
        }
        // The Category ▾ menu (§G07-B) overlays too — resolve it next.
        if (categoryOpen) {
            if (con.pickCategory(mx, my)) return true;
            categoryOpen = false;
        }
        // The "Select ▾" dropdown (4 options) overlays the grid — resolve it next.
        if (selAllOpen) {
            if (BulkDraw.in(mx, my, view.rSelScreen)) { CbUiSounds.click(); ticked.addAll(view.pageIds()); selAllOpen = false; return true; }
            if (BulkDraw.in(mx, my, view.rSelMatch))  { CbUiSounds.click(); ticked.addAll(view.allMatchingIds()); selAllOpen = false; return true; }
            if (BulkDraw.in(mx, my, view.rSelLocked)) { CbUiSounds.click(); addMatching(locked); selAllOpen = false; return true; }
            if (BulkDraw.in(mx, my, view.rSelFav))    { CbUiSounds.click(); addMatching(fav); selAllOpen = false; return true; }
            if (BulkDraw.in(mx, my, view.rSelAll))    { CbUiSounds.click(); selAllOpen = false; return true; }
            selAllOpen = false; // a click elsewhere closes the dropdown, then still lands below
        }
        // Left info panel controls (right-click a tile opens it; ✖ or right-click empty dismisses).
        if (detailId != null) {
            if (BulkDraw.in(mx, my, view.rInfoClose)) { CbUiSounds.click(); detailId = null; return true; }
            if (BulkDraw.in(mx, my, view.rInfoTick)) { CbUiSounds.tick(); if (!ticked.remove(detailId)) ticked.add(detailId); return true; }
            if (BulkDraw.in(mx, my, view.rInfoEditor)) { CbUiSounds.click(); ClientPlayNetworking.send(new BulkActionPayload(BulkActionPayload.OPEN_EDITOR, "", detailId, "", "")); return true; }
            if (BulkDraw.in(mx, my, view.rInfoPanel)) return true;   // swallow stray clicks on the panel (grid tiles sit behind it)
        }
        // Filter chips.
        if (BulkDraw.in(mx, my, view.rChipAll))    { setFilter("all"); return true; }
        if (BulkDraw.in(mx, my, view.rChipFav))    { setFilter("fav"); return true; }
        if (BulkDraw.in(mx, my, view.rChipLocked)) { setFilter("locked"); return true; }
        if (BulkDraw.in(mx, my, view.rChipSel))    { setFilter("selected"); return true; }
        if (view.rChipCategory != null && BulkDraw.in(mx, my, view.rChipCategory)) {
            CbUiSounds.click(); categoryOpen = !categoryOpen; sortOpen = false; selAllOpen = false; return true;
        }
        if (BulkDraw.in(mx, my, view.rSort)) { CbUiSounds.click(); sortOpen = !sortOpen; categoryOpen = false; return true; }

        if (BulkDraw.in(mx, my, view.rSelAll) && !view.allMatchingIds().isEmpty()) { CbUiSounds.click(); selAllOpen = !selAllOpen; return true; }
        if (BulkDraw.in(mx, my, view.rClearSel)) { CbUiSounds.click(); ticked.clear(); return true; }
        if (pick && BulkDraw.in(mx, my, view.rUseThese) && !ticked.isEmpty()) {
            CbUiSounds.chime();
            ClientPlayNetworking.send(new BulkActionPayload(BulkActionPayload.PICK_DONE, BulkAction.scopeExpr(this), "", "", ""));
            return true;
        }
        boolean hit = rowClick(mx, my, button, view.rowRects, view.rowIds, true);
        if (!hit && button == 1 && detailId != null) { detailId = null; return true; } // right-click empty space closes the panel
        return hit;
    }

    private boolean setSort(double mx, double my) {
        if (BulkDraw.in(mx, my, view.rSortName))   { browseSort = "name"; }
        else if (BulkDraw.in(mx, my, view.rSortId)) { browseSort = "id"; }
        else if (BulkDraw.in(mx, my, view.rSortNewest)) { browseSort = "newest"; }
        else if (BulkDraw.in(mx, my, view.rSortColor)) { browseSort = "color"; }
        else return false;
        CbUiSounds.click(); sortOpen = false; view.resetScroll();
        return true;
    }

    private void setFilter(String f) { CbUiSounds.click(); browseFilter = f; view.resetScroll(); }

    /** Add every currently-matching id that is in {@code flagSet} (Select ▾ → Locked only / Favorites). */
    private void addMatching(Set<String> flagSet) {
        for (String id : view.allMatchingIds()) if (flagSet.contains(id)) ticked.add(id);
    }

    private boolean bulkClick(double mx, double my, int button) {
        if (button == 1) return true;   // §G27.22b: right-click is disabled on this tab (no context menu)
        for (int op = 0; op < BulkOpSpec.OP_COUNT; op++) {
            if (ops.railRects[op] != null && BulkDraw.in(mx, my, ops.railRects[op])) { con.setOp(op); return true; }
        }
        if (BulkDraw.in(mx, my, ops.rOptA)) { con.cycleA(1); return true; }
        if (BulkDraw.in(mx, my, ops.rOptB)) { con.cycleB(1); return true; }
        if (ops.rHue != null && BulkDraw.in(mx, my, ops.rHue)) { draggingHue = true; setHueFromMouse(mx); return true; }
        if (BulkDraw.in(mx, my, ops.rUndo)) { CbUiSounds.click(); ClientPlayNetworking.send(new BulkActionPayload(BulkActionPayload.UNDO, "", "", "", "")); return true; }
        if (BulkDraw.in(mx, my, ops.rRedo)) { CbUiSounds.click(); ClientPlayNetworking.send(new BulkActionPayload(BulkActionPayload.REDO, "", "", "", "")); return true; }
        if (BulkDraw.in(mx, my, ops.rApply)) {
            if (canApply()) { CbUiSounds.click(); confirmOpen = true; }   // Execute always opens the confirm (TG B12)
            return true;
        }
        // include-checkbox rows.
        for (int i = 0; i < ops.rowRects.size(); i++) {
            if (BulkDraw.in(mx, my, ops.rowRects.get(i))) {
                String id = ops.rowIds.get(i);
                if (!excluded.remove(id)) excluded.add(id);
                CbUiSounds.tick();
                return true;
            }
        }
        return false;
    }

    private void setHueFromMouse(double mx) {
        int[] r = ops.rHue;
        if (r == null) return;
        recolorHue = BulkDraw.clamp((int) Math.round((mx - r[0]) / (double) r[2] * 360.0), 0, 360);
    }

    private boolean rowClick(double mx, double my, int button, List<int[]> rects, List<String> rowIds, boolean allowDetail) {
        for (int i = 0; i < rects.size(); i++) {
            if (!BulkDraw.in(mx, my, rects.get(i))) continue;
            String id = rowIds.get(i);
            if (button == 1) {
                if (!allowDetail) return true;
                detailId = id.equals(detailId) ? null : id;
            } else if (!ticked.remove(id)) {
                ticked.add(id);
            }
            CbUiSounds.tick();
            return true;
        }
        return false;
    }

    private void switchTab(int t) {
        if (tab == t) return;
        tab = t;
        confirmOpen = false;
        CbUiSounds.click();
        rebuild();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (confirmOpen || resultMsg != null) return true;
        int step = vAmt > 0 ? -1 : 1;
        if (tab == TAB_BROWSE) {
            if (BulkDraw.in(mx, my, CONTENT_X, CONTENT_Y, BulkWorkbenchView.contentW(width), contentBottom(height) - CONTENT_Y)) {
                view.scrollBy(step);
                return true;
            }
        } else {
            int top = ops.curListTop;
            if (BulkDraw.in(mx, my, CONTENT_X, top, BulkWorkbenchView.contentW(width), BulkOpsView.listBottom(height) - top)) {
                ops.scrollList(step);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScroll) {
            if (tab == TAB_BROWSE) view.scrollToMouse(my); else ops.scrollToMouse(my);
            return true;
        }
        if (draggingHue) { setHueFromMouse(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingScroll = false;
        draggingHue = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (resultMsg != null) {   // X3 result modal — dismiss on Esc/Enter
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) resultMsg = null;
            return true;
        }
        if (confirmOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { confirmOpen = false; return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { confirmOpen = false; startExecute(); return true; }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !(getFocused() instanceof CbTextField)) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() { cube.dispose(); super.removed(); }   // §G27.22: release the cube atlases (render thread)

    @Override
    public void close() { if (client != null) client.setScreen(null); }

    @Override
    public boolean shouldPause() { return false; }
}
