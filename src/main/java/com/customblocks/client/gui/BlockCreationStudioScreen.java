/**
 * BlockCreationStudioScreen.java — Group 27 §G27.6 (Block Creation Studio). CLIENT-SIDE ONLY.
 *
 * Opened by bare /cb create. One-screen block maker: left section sidebar (Identity / Texture / Shape /
 * Attributes / Category — plus FX/Behaviour/Lore "soon"), a live 3D preview cube, a bottom action bar.
 * "Create & Publish" (button only) sends ONE CreateStudioPayload; the server creates the block via the
 * existing CreationCommands rail + SlotManager setters (client never mutates server state — §5.8).
 *
 * Depends on: Screen/DrawContext/TextFieldWidget/ButtonWidget, PreviewCube, StudioCategoryWorkspacePanel,
 *             BlockShapes, CbHelpOverlay, CbSettingsOverlay, CbScreenPrefs, StudioState, CreateStudioPayload.
 * Called by: CustomBlocksClient (OpenGuiPayload mode=CREATE_STUDIO).
 */
package com.customblocks.client.gui;

import com.customblocks.block.BlockShapes;
import com.customblocks.client.gui.studio.StudioState;
import com.customblocks.core.AnimData;
import com.customblocks.network.payloads.CreateStudioPayload;
import com.customblocks.network.payloads.StudioSavePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public class BlockCreationStudioScreen extends Screen {

    // Package-private so StudioSections can render each section's content. ANIMATION (Group 14 Phase 2)
    // sits after Texture and is enabled only once an animated clip is loaded.
    enum Section { IDENTITY, TEXTURE, ANIMATION, SHAPE, ATTRIBUTES, AI, CATEGORY, FX, BEHAVIOR, LORE }
    private static final String[] NAV = {"Identity", "Texture", "Animation", "Shape", "Attributes", "AI", "Category", "FX", "Behavior", "Lore"};
    private static final int LAST_REAL = 6;  // indices > this are "coming soon"
    private static final int ANIM_INDEX = 2; // the Animation tab — enabled only when a clip is loaded

    private static final int BAR_BG = CbTheme.BAR_BG, ACCENT = CbTheme.ACCENT, BAR_H = 42;
    private static final int NAV_X = 8, NAV_W = 94, NAV_BH = 18, NAV_GAP = 2;
    private static final int PX = 112, PW = 214; // section panel area
    private static final double DEF_YAW = 28, DEF_PITCH = 16, DEF_SPIN = 0.45;
    private static final int DEF_HALF = 54, HALF_MIN = 34, HALF_MAX = 110, ZOOM_STEP = 8;
    private static final double SPIN_MAX = 2.5, SPIN_STEP = 0.15;

    private static final String[] SOUNDS = {"stone", "wood", "grass", "metal", "glass", "sand", "wool", "gravel", "snow",
            "dirt", "coral", "bamboo", "nether_brick", "ice", "honey", "bone", "slime"};

    private final StudioState st = new StudioState();
    private Section section = Section.IDENTITY;

    private TextFieldWidget idField, nameField, urlField, hexField, catField, aiField;

    private final PreviewCube cube = new PreviewCube();
    private final StudioSections sections = new StudioSections();
    private final CbSettingsOverlay settings = new CbSettingsOverlay(); // §G27.8.B ⚙ (replaces the dim slider)

    // Cached composite preview grid so the cube only re-bakes when texture/background change.
    private int[] dispGrid, dispRaw;
    private boolean dispHasBg, dispTex;
    private int dispBg;
    private long dispVersion;
    private String hoverHint = ""; // bottom-of-panel attribute hover help
    private double yaw = DEF_YAW, pitch = DEF_PITCH, spinSpeed = CbScreenPrefs.get().spinDefault;
    private boolean spinning = true, dragging, dragged;
    private int half = DEF_HALF;
    private boolean confirmingCancel;
    private long saveFlashEnd;
    private String notice = "";    // on-screen banner (replaces chat warnings, e.g. missing id/name on Create)
    private long noticeEnd;
    private boolean lastAnimated; // tracks the animated flag so the nav rebuilds when a clip finishes loading

    private final CbHelpOverlay help = new CbHelpOverlay("Block Creation Studio", java.util.List.of(
            new CbHelpOverlay.Group("STUDIO", java.util.List.of(
                    new CbHelpOverlay.Row("Left tabs", "Identity / Texture / Shape / Attributes / Category"),
                    new CbHelpOverlay.Row("Create & Publish", "Make the block (button only)"),
                    new CbHelpOverlay.Row("Enter", "Confirm the field you're typing in"),
                    new CbHelpOverlay.Row("Esc", "Cancel (asks if unsaved)"))),
            new CbHelpOverlay.Group("VIEW", java.util.List.of(
                    new CbHelpOverlay.Row("Drag", "Rotate the cube"),
                    new CbHelpOverlay.Row("Scroll", "Spin speed · Shift+Scroll zoom"),
                    new CbHelpOverlay.Row("R", "Reset view")))));

    public BlockCreationStudioScreen() {
        super(Text.literal("Block Creation Studio"));
        st.grid = PreviewCube.solid(0x9AA0A6); // neutral cube until a texture/colour is set
    }

    /**
     * Edit mode (Group 14 Phase 2) — open the studio on an EXISTING block with its state loaded. The
     * attrs string is the same "key=value;…" form CreationStudioBridge.editAttrs builds. The frame grids
     * are read back from the resource pack on a daemon thread so the preview plays without re-downloading.
     */
    public BlockCreationStudioScreen(int index, String id, String name, String attrs, String url) {
        this();
        st.editMode = true;
        st.editIndex = index;
        st.id = id == null ? "" : id;
        st.editOrigId = st.id; // remember the open-time id so a rename still resolves server-side
        st.name = name == null ? "" : name;
        st.sourceLink = url == null ? "" : url; // the link this block was made from — shown read-only, not re-applied
        StudioEditLoad.apply(st, attrs);
        st.markBaseline(); // record the loaded state so "dirty" means the player actually changed something
        // Animated → Animation tab; not animated → Texture tab (load a GIF there), not Identity (Fix 4, 2026-06-20).
        section = st.isAnimated() ? Section.ANIMATION : Section.TEXTURE;
        if ("shape".equals(st.openSection)) section = Section.SHAPE; // §G27.11 — /cb shapeeditor opens on Shape
        StudioEditLoad.loadFrames(st);
    }

    /** AI mode (Group 15) — open the studio on the AI tab with an optional pre-filled prompt. The panel's
     *  debounce fires the first generation ~0.8s after open when the prompt is non-empty (/cb ai <prompt>). */
    public BlockCreationStudioScreen(String aiPrompt) {
        this();
        st.aiPrompt = aiPrompt == null ? "" : aiPrompt;
        section = Section.AI;
    }

    @Override
    protected void init() {
        if (confirmingCancel) { addCancelButtons(); return; }
        buildNav();
        buildBottomBar();
        buildSection();
        lastAnimated = st.isAnimated(); // nav was just built for this state; watch for it changing
    }

    private void buildNav() {
        int y0 = BAR_H + 8;
        for (int i = 0; i < NAV.length; i++) {
            final int idx = i;
            boolean real = i <= LAST_REAL;
            boolean enabled = real && (i != ANIM_INDEX || st.isAnimated()); // Animation locks until a clip loads
            String tick = (real && sectionDone(i)) ? " §a✔" : "";
            addDrawableChild(CbButton.tab(Text.literal(NAV[i] + tick),
                    NAV_X, y0 + i * (NAV_BH + NAV_GAP), NAV_W, NAV_BH, i == section.ordinal(), enabled, b -> navClick(idx)));
        }
    }

    private void buildBottomBar() {
        int by = height - BAR_H + 11, cx = width / 2, right = width - 8;
        addDrawableChild(CbButton.primary(Text.literal(st.editMode ? "Save changes" : "Create & Publish"), cx - 80, by, 160, 20, b -> create()));
        addDrawableChild(CbButton.ghost(Text.literal("Cancel"), right - 66, by, 66, 20, b -> close()));
        addDrawableChild(CbButton.normal(Text.literal("?"), width - 24, 10, 16, 16, b -> help.toggle()));
        addDrawableChild(CbButton.normal(Text.literal("⚙"), width - 44, 10, 16, 16, b -> settings.toggle()));
    }

    private void buildSection() {
        int y = BAR_H + 30;
        switch (section) {
            case IDENTITY -> {
                // §overlap-fix: fields dropped 4px so the "Block ID"/"Display Name" labels (drawn at y and
                // y+34 by StudioSections) sit clearly ABOVE their boxes instead of touching the top border.
                idField = field(PX, y + 12, st.id, 32, "block id", t -> st.id = t);
                nameField = field(PX, y + 46, st.name, 48, "display name", t -> st.name = t);
                addDrawableChild(idField);
                addDrawableChild(nameField);
                addDrawableChild(CbButton.normal(Text.literal("Auto-ID from name"), PX, y + 74, 150, 18, b -> autoId()));
            }
            case TEXTURE -> {
                urlField = field(PX, y + 8, st.url, 512, "https://… image url", t -> { st.url = t; });
                addDrawableChild(urlField);
                addDrawableChild(CbButton.normal(Text.literal("Load texture"), PX, y + 30, 110, 18, b -> loadTexture()));
                // Hex/background row — hidden once a GIF is loaded: animated blocks don't use a background
                // colour (owner: "remove bg colouring entirely for gifs"). A SHORT field + button beside it.
                if (!st.isAnimated()) {
                    hexField = new CbTextField(textRenderer, PX, y + 96, 96, 16, Text.literal("#RRGGBB"));
                    hexField.setMaxLength(7);
                    hexField.setPlaceholder(Text.literal("§8#RRGGBB"));
                    addDrawableChild(hexField);
                    addDrawableChild(CbButton.normal(Text.literal("Use hex"), PX + 102, y + 96, 58, 16, b -> useHex()));
                }
            }
            case ATTRIBUTES -> {
                addDrawableChild(CbButton.normal(Text.literal("−"), PX, y + 14, 20, 18, b -> { st.glow = Math.max(0, st.glow - 1); }));
                addDrawableChild(CbButton.normal(Text.literal("+"), PX + 80, y + 14, 20, 18, b -> { st.glow = Math.min(15, st.glow + 1); }));
                addDrawableChild(CbButton.normal(Text.literal("◀"), PX, y + 84, 20, 18, b -> cycleSound(-1)));
                addDrawableChild(CbButton.normal(Text.literal("▶"), PX + 140, y + 84, 20, 18, b -> cycleSound(1)));
                addDrawableChild(CbButton.normal(Text.literal(st.passable ? "Passable" : "Solid"), PX, y + 112, 100, 18, b -> { st.passable = !st.passable; rebuild(); }));
            }
            case AI -> {
                aiField = field(PX, y + 8, st.aiPrompt, 200, "describe the block… e.g. glowing red crystal",
                        t -> st.aiPrompt = t);
                addDrawableChild(aiField);
                addDrawableChild(CbButton.normal(Text.literal("↻ Regenerate"), PX, y + 30, 110, 18, b -> sections.aiRegenerate(st)));
            }
            case CATEGORY -> {
                // The chips/actions are custom-drawn by StudioSections; the screen only owns the name field,
                // placed at a fixed offset the panel draws its label/actions around.
                catField = field(PX + 8, y + 172, "", 32, "new category / new name", t -> {});
                addDrawableChild(catField);
                sections.resetCategory();
                // Group 27 Category Hub — jump to the full red+black category manager (returns here on close).
                addDrawableChild(CbButton.normal(Text.literal("Open Full Category Hub"), width - 210, BAR_H + 6, 200, 18,
                        b -> { if (client != null) client.setScreen(new CategoryHubScreen(this)); }));
            }
            default -> { /* SHAPE/ANIMATION = custom pickers; FX/BEHAVIOR/LORE = note only */ }
        }
    }

    private TextFieldWidget field(int x, int y, String value, int max, String hint, java.util.function.Consumer<String> sink) {
        CbTextField f = new CbTextField(textRenderer, x, y, 200, 16, Text.literal(hint));
        f.setMaxLength(max);
        f.setText(value);
        f.setPlaceholder(Text.literal("§8" + hint));
        f.setChangedListener(sink);
        return f;
    }

    private void rebuild() { clearChildren(); init(); }

    private void navClick(int idx) {
        if (idx > LAST_REAL) { message("§7" + NAV[idx] + " needs new block data — coming soon."); return; }
        if (idx == ANIM_INDEX && !st.isAnimated()) {
            message("§7Load a GIF or WebP in the Texture tab first — then the Animation tab unlocks.");
            return;
        }
        section = Section.values()[idx];
        rebuild();
    }

    private void autoId() {
        st.id = st.name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (idField != null) idField.setText(st.id);
    }

    private void loadTexture() {
        String url = st.url == null ? "" : st.url.trim();
        if (url.isEmpty()) { st.loadState = "none"; return; }
        st.loadState = "loading";
        // Keep the player's Loop + Smooth choices when they swap in a different clip (Bucket 1 slice 2): a
        // new clip brings its OWN frame count + real speed, so speed + trim reset, but loop/smooth are taste.
        final AnimData prev = st.anim;
        // StudioTextureLoader uses the same fetch as /cb create (browser UA + Tenor/Giphy resolve),
        // so links that worked from the command now work here too (fixes the "URL failed" bug).
        Thread t = new Thread(() -> {
            StudioTextureLoader.Result r = StudioTextureLoader.load(url);
            if (r != null) {
                st.grid = r.frames()[0];
                if (r.animated()) {
                    st.frames = r.frames();
                    AnimData fresh = r.anim(); // new clip: its own real speed (match-original) + full trim
                    if (prev != null && prev.isAnimated())
                        fresh = fresh.withLoopMode(prev.loopMode()).withInterpolate(prev.interpolate());
                    st.anim = fresh;
                    st.hasBg = false; // GIFs don't use a background colour (owner: remove bg for gifs)
                } else {
                    st.frames = null; // keep all frames only when really animated
                    st.anim = r.anim();
                }
                st.textureLoaded = true;
                st.loadState = r.animated() ? "okanim" : "ok";
            } else st.loadState = "fail";
        }, "CustomBlocks-StudioFetch");
        t.setDaemon(true);
        t.start();
    }

    private void useHex() {
        if (hexField == null) return;
        try {
            int rgb = Integer.parseInt(hexField.getText().trim().replace("#", ""), 16);
            st.pickBg(0xFF000000 | rgb);
        } catch (Exception e) { message("§cThat's not a valid #RRGGBB colour."); }
    }

    /** Enter inside a focused field: the Category tab adds/renames; every other field just blurs. */
    private void onFieldEnter(TextFieldWidget f) {
        if (section == Section.CATEGORY && f == catField) sections.categoryEnter(st, catField);
        else setFocused(null);
    }

    /** The grid shown on the cube, cached so it only re-bakes on change (bg+image composite / bg-only / image / grey). */
    private int[] displayGrid() {
        boolean tex = st.textureLoaded && st.grid != null;
        if (st.grid == dispRaw && st.hasBg == dispHasBg && st.bgArgb == dispBg && tex == dispTex && dispGrid != null)
            return dispGrid;
        int[] disp;
        boolean bg = st.hasBg && !st.isAnimated(); // GIFs ignore the background colour (preview matches the block)
        if (bg && tex)            disp = PreviewCube.compositeOver(st.grid, st.bgArgb);
        else if (bg)              disp = PreviewCube.solid(st.bgArgb);
        else                      disp = st.grid;
        dispRaw = st.grid; dispHasBg = st.hasBg; dispBg = st.bgArgb; dispTex = tex; dispGrid = disp;
        dispVersion++;
        return disp;
    }

    /** A section is "done" (✔) once it carries a meaningful, non-default value (for the nav ticks). */
    private boolean sectionDone(int idx) {
        return switch (idx) {
            case 0 -> !st.id.isBlank() && !st.name.isBlank();
            case 1 -> st.textureLoaded || st.hasBg;
            case 2 -> st.isAnimated();
            case 3 -> !st.shape.equals("full");
            case 4 -> st.hardnessSet || st.glow != 0 || st.passable || !st.sound.equals("stone");
            case 5 -> st.textureLoaded; // AI tab: a generated texture is loaded
            case 6 -> !st.category.isBlank();
            default -> false;
        };
    }

    private void cycleSound(int dir) {
        int i = 0;
        for (int k = 0; k < SOUNDS.length; k++) if (SOUNDS[k].equals(st.sound)) { i = k; break; }
        st.sound = SOUNDS[(i + dir + SOUNDS.length) % SOUNDS.length];
    }

    private void create() {
        if (st.editMode) { saveEdit(); return; }
        if (st.id == null || st.id.trim().isEmpty()) { notice("§cGive it an id first (short, lowercase) — see the Identity tab."); section = Section.IDENTITY; rebuild(); return; }
        if (st.name == null || st.name.trim().isEmpty()) { notice("§cGive it a display name first — see the Identity tab."); section = Section.IDENTITY; rebuild(); return; }
        saveFlashEnd = System.currentTimeMillis() + 600;
        String url = st.url == null ? "" : st.url.trim();
        ClientPlayNetworking.send(new CreateStudioPayload(st.id.trim(), st.name.trim(), url, st.toAttrs()));
        message("§7⏳ Creating §f" + st.id.trim() + "§7… watch chat for the result.");
        confirmingCancel = false;
        super.close();
    }

    /** Edit mode: send the whole block — id (maybe renamed), name, a new image url (or ""), and the
     *  settings + animation knobs — to the server ("Save changes"). The studio edits everything. */
    private void saveEdit() {
        saveFlashEnd = System.currentTimeMillis() + 600;
        String newUrl = st.url == null ? "" : st.url.trim();
        ClientPlayNetworking.send(new StudioSavePayload(st.editOrigId, st.id.trim(), st.name.trim(), newUrl, st.saveAttrs()));
        message("§7⏳ Saving §f" + st.id.trim() + "§7…");
        st.markBaseline(); // closing right after a save shouldn't re-prompt "discard changes?"
        confirmingCancel = false;
        super.close();
    }

    @Override
    public void close() {
        if (st.dirty() && CbScreenPrefs.get().confirmDiscard && !confirmingCancel) { // §G27.8.B toggleable
            confirmingCancel = true; clearChildren(); init(); return;
        }
        super.close();
    }

    private void addCancelButtons() {
        int cx = width / 2, cy = height / 2;
        addDrawableChild(CbButton.primary(Text.literal("Yes, discard"), cx - 96, cy + 2, 88, 20, b -> { confirmingCancel = false; super.close(); }));
        addDrawableChild(CbButton.ghost(Text.literal("Keep editing"), cx + 8, cy + 2, 88, 20, b -> { confirmingCancel = false; clearChildren(); init(); }));
    }

    private void message(String s) {
        CbToast.info(s); // §G27.13 — toast, not chat
    }

    /** Show an on-screen banner for ~4s (used instead of chat for in-studio notices, e.g. missing id/name). */
    private void notice(String s) { notice = s; noticeEnd = System.currentTimeMillis() + 4000; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) { /* world stays visible */ }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, CbScreenPrefs.get().backdrop());

        // A clip just finished loading (or unloaded) on the worker thread → rebuild so the Animation tab
        // lights up / greys out. Fires once per transition (lastAnimated is updated in init()).
        if (!confirmingCancel && st.isAnimated() != lastAnimated) { rebuild(); }

        if (section != Section.CATEGORY) {
        int cubeCx = PX + PW + (width - (PX + PW)) / 2;
        int cubeCy = height / 2 - 4;
        if (st.isAnimated()) st.grid = sections.currentFrame(st); // live playback: swap the displayed frame
        cube.renderShape(ctx, displayGrid(), cubeCx, cubeCy, half, yaw, pitch, BlockShapes.boxes(st.shape), PreviewCube.AS_IS, dispVersion);
        String badge = switch (st.loadState) {
            case "loading" -> "§8loading texture…";
            case "aigen"   -> "§b✦ generating… (AI)";
            case "aifail"  -> "§ccouldn't generate — try rephrasing";
            case "ok"      -> "§a✔ " + (st.textureLoaded ? (st.hasBg ? "image + background" : "texture loaded") : "background set");
            case "okanim"  -> "§a✔ animated — it'll play once placed";
            case "fail"    -> "§cURL failed — check the link";
            default        -> "§8grey = no texture yet";
        };
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(badge), cubeCx, cubeCy + half + 16, 0xFFFFFFFF);
        // Keep the image/GIF link on-screen at all times (owner request) — under the preview, every tab. Use
        // the url being typed if any, else the stored source link (edit mode) so EXISTING blocks show it too.
        String link = st.url != null && !st.url.isBlank() ? st.url.trim()
                : (st.sourceLink == null ? "" : st.sourceLink.trim());
        if (!link.isEmpty()) {
            String shown = link.length() > 60 ? link.substring(0, 57) + "…" : link;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8link: §7" + shown), cubeCx, cubeCy + half + 28, 0xFFFFFFFF);
        }

        // Panel card behind the active section (UI polish) — dark cards with a neon-red top hairline.
        }
        ctx.fill(NAV_X - 4, BAR_H + 6, NAV_X + NAV_W + 4, height - BAR_H - 6, CbTheme.CARD);
        int panelRight = section == Section.CATEGORY ? width - 8 : PX + PW - 4;
        ctx.fill(PX - 8, BAR_H + 26, panelRight, height - BAR_H - 10, CbTheme.CARD);
        ctx.fill(PX - 8, BAR_H + 26, panelRight, BAR_H + 27, CbTheme.CARD_EDGE);

        drawNavMarker(ctx);
        hoverHint = sections.render(section, ctx, textRenderer, PX, BAR_H + 30, st, catField, mx, my, NAV[section.ordinal()], width, height);
        if (!hoverHint.isEmpty())
            ctx.drawTextWithShadow(textRenderer, Text.literal(hoverHint), PX, height - BAR_H - 16, 0xFFFFFFFF);

        // Title bar + breadcrumb.
        ctx.fill(0, 0, width, BAR_H, BAR_BG);
        ctx.fill(0, BAR_H - 1, width, BAR_H, ACCENT);
        ctx.drawTextWithShadow(textRenderer, CbTheme.title("Block Creation Studio", st.id.isBlank() ? "new block" : st.id), 8, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7‹ " + NAV[section.ordinal()] + " · pick a section on the left · drag to rotate"), 8, 22, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8Create with the button below · Esc = cancel · ? help"), 8, 32, 0xFFFFFFFF);

        // Bottom action bar.
        int barY = height - BAR_H;
        ctx.fill(0, barY, width, height, BAR_BG);
        ctx.fill(0, barY, width, barY + 1, ACCENT);

        super.render(ctx, mx, my, delta);

        if (!notice.isEmpty() && System.currentTimeMillis() < noticeEnd)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(notice), width / 2, barY - 14, 0xFFFFFFFF);
        if (System.currentTimeMillis() < saveFlashEnd)
            ctx.fill(width / 2 - 80, barY + 11, width / 2 + 80, barY + 31, CbTheme.FLASH_OK);
        if (confirmingCancel) renderCancelConfirm(ctx);
        settings.render(ctx, width, height, textRenderer, mx, my); // §G27.8.B ⚙ popup
        help.render(ctx, width, height, textRenderer, mx, my);

        if (!dragging && spinning && !confirmingCancel && !help.isOpen())
            yaw = (yaw + spinSpeed * delta) % 360.0;
    }

    private void drawNavMarker(DrawContext ctx) {
        int y0 = BAR_H + 8, i = section.ordinal();
        ctx.fill(NAV_X - 3, y0 + i * (NAV_BH + NAV_GAP), NAV_X - 1, y0 + i * (NAV_BH + NAV_GAP) + NAV_BH, ACCENT);
    }

    private void renderCancelConfirm(DrawContext ctx) {
        int cx = width / 2, cy = height / 2, pw = 220, ph = 70;
        ctx.fill(cx - pw / 2 - 1, cy - ph / 2 - 1, cx + pw / 2 + 1, cy + ph / 2 + 1, ACCENT);
        ctx.fill(cx - pw / 2, cy - ph / 2, cx + pw / 2, cy + ph / 2, CbTheme.DIALOG_BG);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fDiscard this block?"), cx, cy - 18, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (help.mouseClicked(mx, my)) return true;
        if (settings.mouseClicked(mx, my)) return true; // settings open → modal
        if (confirmingCancel) return super.mouseClicked(mx, my, button);
        if (super.mouseClicked(mx, my, button)) return true;
        if (sections.mouseClicked(section, mx, my, st, catField)) return true;
        if (button == 0) { dragging = true; dragged = false; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settings.mouseDragged(mx, my)) return true; // settings sliders
        if (dragging) { dragged = true; yaw = (yaw + dx * 0.6) % 360.0; pitch = Math.max(-85, Math.min(85, pitch - dy * 0.6)); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (settings.mouseReleased()) { dragging = false; return true; }
        if (dragging && !dragged) spinning = !spinning;
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (hasShiftDown()) half = Math.max(HALF_MIN, Math.min(HALF_MAX, half + (int) Math.signum(vAmt) * ZOOM_STEP));
        else spinSpeed = Math.max(0, Math.min(SPIN_MAX, spinSpeed + vAmt * SPIN_STEP));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (help.isOpen()) { if (key == GLFW.GLFW_KEY_ESCAPE) help.close(); return true; }
        if (settings.keyPressed(key)) return true; // settings open → Esc closes, keys swallowed
        boolean typing = getFocused() instanceof TextFieldWidget;
        if (key == GLFW.GLFW_KEY_SLASH && !typing) { help.open(); return true; }
        // Enter NEVER publishes — only the "Create & Publish" button does. Enter just confirms the
        // focused field (the Category tab uses it to add a category chip; elsewhere it blurs the field).
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (typing) { onFieldEnter((TextFieldWidget) getFocused()); return true; }
            return true;
        }
        if (key == GLFW.GLFW_KEY_R && !typing) { yaw = DEF_YAW; pitch = DEF_PITCH; spinSpeed = CbScreenPrefs.get().spinDefault; half = DEF_HALF; spinning = true; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() { cube.dispose(); super.removed(); }
}
