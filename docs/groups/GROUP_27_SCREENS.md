# Group 27 — Unified Screen Design System, Upgrades & Block Creation Studio

> 🟣 **DRAG-DROP CREATE — logged 2026-06-29 (NO code yet).** New: drop an image/gif onto the game →
> studio screen with a preview → name/options → Create. Scope = BOTH "drop anywhere, screen auto-opens"
> AND "drop onto the open create screen". Catch: today the studio only sends the server a URL — there is
> no client→server byte-upload channel, so a new chunked upload pipe must be built first. OS drop caught
> via GLFW drop callback. Full record: **`docs/Information/IMAGE_INPUT_OVERHAUL.md`** (§6 + §9).

> **Prerequisites:**
> - Group 13 (Arabic — live preview) confirmed working in-game. `ArabicPreviewScreen` is the reference implementation.
> - `BlockCreationStudioScreen` (G27.6) additionally requires all 5 screen upgrades (G27.1–G27.5) confirmed in-game first.
>
> **Objective:** Establish a unified design language for all CB `Screen` subclasses, implement the shared template, upgrade every existing screen to follow it, build the new `ShapeEditorScreen`, and build the full `BlockCreationStudioScreen` that opens when the player runs `/cb create` with no arguments.
>
> **Rules:** Work screen by screen in order. Stop and confirm in-game before moving to the next screen. Nothing in this group is marked done until tested in-game.

---

# 🛠️ 2026-07-04 corrections + master build order (read this before building anything)

> The rest of this file is still the source of truth for exact layouts/behavior. This section
> fixes the color, adds missing screens, resolves doc conflicts, and gives the build order.
> Do not skip steps. Test each step in-game with the developer before moving to the next
> (CLAUDE.md §2, §4 — nothing is done until he confirms it in-game himself).

## Corrections to the sections below

1. **Exact color scheme locked (2026-07-04):** `#FF0000` red · `#000000` pure black · `#40FF00`
   lime. Every place below this section says "gold," `§6`, or "gold-on-black" — use **red +
   black** instead (title bar accent, borders, primary button). Roles split by job, not one color
   for everything:
   - **Red** = selected / active state (the job gold used to do — selected tab, active toggle,
     focused field border, title bar accent)
   - **Lime** = success feedback ONLY (the "saved!" flash, the green in the original spec's
     `[§aSave]` button flash, achievement-unlock, create-success) — pulls in the full 3-color brand
   - **Black** = background/backdrop everywhere, pure `#000000`, not a softened near-black
   Do not mix these roles — red never means "success," lime never means "selected."
2. **The 5 old plain menu screens are IN SCOPE now.** `MainMenuScreen`, `BlockEditorScreen`,
   `ConfigScreen`, `MacroListScreen`, `ArabicBrowserScreen` (all in
   `src/main/java/com/customblocks/gui/screens/`) are real, working, vanilla-grey-button screens
   that were never part of the original Group 27 plan. Add them to the restyle pass (they follow
   the same red+black CbScreenTemplate frame as every other screen — they're simpler, no 3D cube,
   no undo/redo needed since they're just navigation buttons).
3. **A missing screen gets built: the animated/GIF block editor.** Referenced in the docs as
   "Issue 17.16 — AnimBlockScreen missing." Right now an animated block can only be replaced
   wholesale (retexture), never have its frame order/timing edited. Add a real `AnimBlockScreen`
   (frame list, reorder, per-frame timing, preview) to the plan, built to the same red+black
   standard as everything else.
4. **Screens vs. chest-menus: leave the current split alone.** Don't convert any existing chest-menu
   to a screen, or any existing screen to a chest-menu. The developer picked deliberately which
   GUI type each feature uses — only touch classes that are already `Screen` subclasses.

## What "Group 27" covers (plain terms)

Every full-screen popup the mod opens — the create-a-block screen, the Arabic letter preview, the
recolor tool, the HUD editor, the shape editor, the 5 old menu screens, and the new GIF editor —
should look and feel like one family: same red+black frame, same title bar, same undo/redo/help
buttons in the same place, same keyboard shortcuts. Today they're all different ages and styles.

## Master build order (one step at a time, in-game test after each)

Do not start a step until the previous one is confirmed working in-game.

1. Shared frame + cube renderer fixes (§A) — cube renderer rebuild, help button z-order, oversized
   bottom bar. Touches every 3D screen at once — do this first. Red+black, not the gold in the
   sections below.
2. Floating panel + color system (§B) — the reusable draggable/collapsible panel used for color
   swatches, the recolor tool, the bottom action bar, and the HUD panel.
3. Per-screen fixes: `/cb livecolor` → renamed `/cb recolor` (§G27.11, rename locked, no alias),
   `/cb eyedrop`, `/cb edithud` (full Lego-brick rebuild, §G27.4), `/cb shapeeditor` registration
   bug fix + re-route (§F1/F2).
4. The Masterpiece visual pass (§G27.8) — settings gear menu, movable action bar, refined
   spacing/shadows/hover glow, on every screen built so far. Red+black, not gold.
5. Block Creation Studio (§G27.6 + extended §G27.6.X) — the full one-screen block creator.
   Requires steps 1–4 confirmed first.
5a. Drag-and-drop block creation — new upload channel + drop-catch + preview pop-up, built on the
    Studio. Needs a new client→server byte-upload channel first (today the studio only ever sends
    a URL, never raw file bytes).
6. Studio Edit Mode (§G27.9) — load an existing block into the studio and edit it in place.
7. Studio Paint (§G27.10) — hand-drawn pixel editor inside the studio.
8. Unified Create+Edit / `/cb editor` (§G27.12) — replaces the old chest-based block editor. Fixes
   a confirmed live bug: `/cb editor <id>` currently still opens the old chest editor instead of
   the Studio Edit tab (`GROUP_02_CHEST_GUI.md` test G02.3 failed in-game 2026-06-21).
9. Recolor + Shape folded into the Studio (§G27.11) — retires the standalone recolor/shape screens
   AND the chest `ShapeEditorMenu`.
10. New animated/GIF block editor (`AnimBlockScreen`, correction #3 — accepts both `.gif` and
    pre-sliced `.png`).
11. Vault Hub (command + screen, not command-only) + `VaultConflictScreen` 3D-compare upgrade.
12. Config gap fix — long-text config fields (AI keys, Discord webhook, vault endpoint) get real
    text boxes; the old anvil-paste trick (`AnvilPrompt`) goes away entirely since screens capture
    keyboard input natively.
13. Achievements screen (gallery of unlocked achievements, title+chat `[View]`-link unlock
    notification — not a toast) + onboarding/welcome tutorial screen (first-join popup, book item
    + screen, per the P7 decision). Needs a new `AchievementSyncPayload` first — today
    `AchievementManager.java` has no S2C payload sending unlock state to the client at all.
14. `RecordOverlayStudioScreen` — test Build A (already implemented) in-game, red+black pass, then
    build Build B (`/cb recordoverlay` + Studio-style editor + server-pushed shared layouts,
    designed 2026-06-30, not built).
14a. Drops tab in the Block Studio (`Section.DROPS`) — build once step 2 (floating panel) is done.
14b. `/cb welcome` screen (proper red+black version of the existing clickable-chat-links content,
     `GROUP_04_CHAT.md` G04.10) + marker customization screen (restyle the "deleted block" marker,
     currently a fixed grey ✖ default — both small, self-contained).
14c. Unified chat/hotbar/action-bar style + sound pass — ties chat toasts, hotbar popups, and
     `CbActionBar` into one consistent style + sound cue per message type (`GROUP_04_CHAT.md` §D
     "Screen toasts replace chat"). Do this alongside the red+black repoint of `CbActionBar`
     (correction #1) rather than twice.
14d. `BulkRecolorScreen` — bulk version of the recolor tool (`GROUP_07_BULK_OPERATIONS.md`), reuses
     the recolor-slider pattern from step 9. Entry point stays the bulk-ops chest-menu
     (`BulkHubMenu` tile) — only the recolor step itself is a screen.
15. **Big rebuilds, in this order:** Dashboard (replaces `MainMenu` chest-menu + dead
    `MainMenuScreen`) → Config screen (replaces `ConfigMenu` + `SettingsBookMenu` + their 5
    sub-menus + the Discord Hub editor, folded in as one of its sections) → Macro screen (the only
    GUI macros will ever have — nothing exists today) → Undo/Redo screen (`G28-1`, 3 tabs, real
    persistence, replaces chest `UndoMenu` + `HistoryMenu`). Each built fresh — chest-menu version
    used only as a feature checklist, not ported code — tested in-game before cutover; chest-menu
    equivalents deleted only after the developer confirms the replacement works.
16. **Arabic big section + Colour Family tab**, added inside the Block Creation Studio (not a
    standalone "Arabic Hub" screen) — build after the Studio itself (step 5) and its Edit Mode
    (step 6) are solid, since both are Studio sections that reuse its sidebar/tab pattern. The 6
    old chest menus (`ArabicHubMenu`, `ArabicListMenu`, `ArabicGroupMenu`, `ArabicWordSession`,
    `WordChoiceMenu`, `ColorStudioMenu`) retire once this Studio section is confirmed. Building it
    as a real screen section also fixes the Arabic-Unicode-input bug (`/cb arabic word` can't
    accept typed Unicode via Brigadier, Issue 17.18 — a real text-box widget sidesteps this).
    Wire in `RockwellCondensed.ttf` (already bundled at
    `src/main/resources/assets/customblocks/fonts/`, alongside `arabtype.ttf`, but never used —
    Issue 17.21) for English text-on-block rendering, alongside this work since Word Maker touches
    the same text-render path.

## Cutover rule

Once the developer confirms a new screen works in-game, the matching command (`/cb`,
`/cb gui arabic`, `/cb config`) is switched to open ONLY the new screen, and the old chest-menu
classes + their now-dead command routing are deleted. No dead code left behind.

## Confirmed decisions / resolved conflicts (2026-07-04 audit)

- **Achievements stays a screen**, overriding `GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md` (which says
  chest-menu tab).
- **Config becomes a full screen**, overriding `GROUP_21_CONFIG_GUI.md` (which says stay
  chest-menu). Built fresh, using the current chest Settings Book as a feature/wording reference,
  not ported code. Same "build fresh, take inspiration" approach applies to the Dashboard and
  Arabic section.
- **Arabic reconciled** to one big section inside the Block Creation Studio (not a standalone
  "Arabic Hub" screen) — matches `GROUP_13_ARABIC.md` §G13-22 and `GROUP_13_BUILD_PLAN.md` Pass 5,
  both of which already said "fold into the Studio."
- **`ShapeEditorScreen` vs. chest `ShapeEditorMenu`** — both retire, folded into the Studio's Shape
  section. Already the existing plan (§G27.11/F2), just confirmed.
- **3 of the 5 old screens are dead code today**: `MainMenuScreen`, `BlockEditorScreen`,
  `ArabicBrowserScreen` are never opened by anything in the game right now — only their chest-menu
  equivalents are used. `ConfigScreen` is reachable only via the optional Mod Menu button.
  `MacroListScreen` is unreachable AND has no chest-menu backup — macros currently have zero
  working GUI, only typed commands.
- **Everything else in `gui/chest/` stays chest-menus, untouched**: bulk ops, backup/trash,
  categories, colors (except what folds into the Studio), diagnostics, export, notes. Not now,
  maybe later — don't touch without asking again first.

## Explicitly out of scope (parked, not part of this build)

- **Permissions (Group 22)** — owner paused this group 2026-06-22 with 8 unresolved judgment
  calls; still parked. Screens in this plan check no permissions, open to everyone, same as today.
- **Hologram block-preview** (`/cb preview <url>` + offhand hologram) — has a home as a stub in
  `GROUP_19_DISPLAY.md`. Not part of this plan.
- **Guess Mode** — brainstormed blind/disguise feature, not designed, ties to a future
  chat-command design session (G04-2). Not part of this plan.
- **Custom icon set matching the red+black brand** — a genuinely separate future project. Keep the
  spec's built-in emoji icons (📚 🧱 ✨ 🏁 etc) everywhere in this build; don't block any screen on
  icon art.

## Notes for Fable

- File size limits are enforced by the build (`verifyFileSize`): any `.java` file ≤ 500 lines,
  command handlers ≤ 400, `*Config.java` ≤ 300. Split before you hit the limit, not after.
- `SoundEvents.BLOCK_NOTE_BLOCK_*` needs `.value()`; every other `SoundEvents` constant must NOT
  use `.value()` (build gate `verifySound` checks this).
- JDK 21 required to build (`--no-daemon`, `JAVA_HOME` set to 21 — the machine's default Java is
  different).
- The developer cannot read code. Explain what you built in plain terms and give a short in-game
  test checklist for each step — don't say "done," say "built, ready for you to test."
- **Git discipline:** never commit to `main` directly — branch first. Never commit unless the
  developer asks. Never force-push. List every change in the commit message so the developer knows
  what to test.
- **Testing guide stays in sync:** after each build-order step, add that step's test checklist to
  `docs/testing/GROUP_27_TESTING_GUIDE.md` (template: `docs/testing/TESTING_GUIDE_TEMPLATE.md`)
  before handing back for in-game testing.

---

## 🧊 QoL cross-link — Vault Hub screen (chat output → GUI later)

`/cb vault codes` + the cloud share commands (Group 20) output to **chat** today. Their planned
GUI front-end is the **Vault Hub** — a CB `Screen` in this group's style (GROUP_20 §G / S7 client
screen, Block-Creation-Studio style; supersedes the old "central chest dashboard" wording). Deferred
QoL, no work scheduled — note only. Tracked across four surfaces: **GROUP_20 §G** ↔ **GROUP_04**
(chat) ↔ **GROUP_27** (here, screens) ↔ **GROUP_02** (chest GUI).

---

## Why this group exists

The mod has 4 existing `Screen` subclasses plus a planned 5th (`ShapeEditorScreen`) and a full creation studio (`BlockCreationStudioScreen`). They were built at different times and look inconsistent:

| Inconsistency | Was |
|---|---|
| Backdrop | `0xF0`, `0xC0`, `0x66`, `0x33` — all different |
| Title color | `§6` gold, `§b` cyan, `§e` yellow — mixed |
| Title/hint Y positions | varied by screen |
| Button layout | different orders, different labels |
| Cancel behavior | instant close, or revert snapshot, or `GuiBackPayload` — inconsistent |
| Keyboard shortcuts | `R` and `Enter` on Arabic only |
| Undo/Redo | none on any screen |
| Copy/Paste | none |
| Help | none |
| Save feedback | silent on all screens |

The goal is one shared design language, applied to every screen. New screens are built to the standard from day one. Existing screens are upgraded one at a time.

---

## The CB Screen Design Standard

> **Reference implementation:** `CbScreenTemplate.java` — `client/gui/CbScreenTemplate.java`.
> Every new CB screen must start from or match this template.

### Visual layout

```
╔══════════════════════════════════════════════════════════════╗  ← §6 gold 1px border
║  §6§lScreen Name §7— §fcontext_id                     [?]  ║  ← dark strip title bar
║  §7{screen-specific hint} · {hint} · {hint}                 ║  ← hint line 1
║  §8Ctrl+Z undo · Ctrl+C copy · Enter confirm · ? help       ║  ← hint line 2 (universal)
╠══════════════════════════════════════════════════════════════╣

  [ main content — world visible behind (0x33 backdrop) ]

╠══════════════════════════════════════════════════════════════╣  ← §6 gold 1px border
║  [Undo] [Redo] [Rand]       [§aSave/Apply]       [Copy] [Reset] [Cancel]  ║
╚══════════════════════════════════════════════════════════════╝
```

### Rules

| Rule | Value |
|---|---|
| Backdrop | `0x33000000` — all screens, world always visible |
| Title bar | Dark strip `0xAA000000` + thin `§6` gold bottom border line |
| Title with block | `§6§l{Name} §7— §f{block_id}` |
| Title without block | `§6§l{Name}` (no dash) |
| Hint line 1 | `§7` — screen-specific controls, `·` separator |
| Hint line 2 | `§8` — `Ctrl+Z undo · Ctrl+C copy · Enter confirm · ? help` |
| Bottom action bar | Dark strip `0xAA000000` + thin `§6` gold top border line |
| Button order | `[Undo] [Redo] [Rand] ··· [§aSave] ··· [Copy] [Reset] [Cancel]` |
| Primary button | Always `§a` green text, always centered |
| Cancel w/ unsaved | In-screen overlay confirmation: "Discard changes?" / [Yes, discard] / [Keep editing] |
| Save feedback | Save button flashes green for ~600ms + a **CB toast** (top-right), professional tone — **no chat** (see §G27.13) |
| `[?]` button | Top-right of title bar — click = in-screen shortcut overlay listing all shortcuts |
| Undo history | Persists per block per screen type (disk), not just in-memory |

### Universal keyboard shortcuts (all screens)

| Key | Action |
|---|---|
| `Ctrl+Z` | Undo |
| `Ctrl+Y` / `Ctrl+Shift+Z` | Redo |
| `Ctrl+C` | Copy settings → OS clipboard (silent) + print code in chat |
| `Ctrl+V` | Paste settings from OS clipboard |
| `Ctrl+R` | Randomize (on screens that support it) |
| `Enter` | Primary action (Save / Apply / Create) |
| `Esc` | Cancel (confirmation dialog if unsaved changes exist) |
| `R` | Reset view — 3D-cube screens only |
| `?` | Open shortcut help overlay |

### 3D cube screens

`ArabicPreviewScreen`, `RecolorSliderScreen`, `ShapeEditorScreen`, and `BlockCreationStudioScreen` all use the same `MatrixStack` cell-grid cube renderer. Rules for this sub-family:

- Cube centered in the main content area (or center panel for multi-panel screens)
- Drag-rotate (Y-axis yaw + X-axis pitch)
- Scroll = auto-spin speed (0 to stop)
- Shift+scroll = zoom
- Click (no drag) = pause / resume spin
- `R` = reset yaw / pitch / spin / zoom to defaults
- Corner readout: `§8spin {X}% · zoom {Y}%`
- All 6 faces shaded with ambient + diffuse lighting

---

## Screens in scope

| # | Screen | File | Origin | Upgrade type |
|---|---|---|---|---|
| G27.1 | `ArabicPreviewScreen` | `client/gui/ArabicPreviewScreen.java` | Group 13 §1 | Adopt unified frame + shortcuts + undo |
| G27.2 | `RecolorSliderScreen` | `client/gui/RecolorSliderScreen.java` | Group 10 | Flat 2D box → 3D cube; full unification → **RETIRED, folded into Studio Recolor section (§G27.11)** |
| G27.3 | `EyedropScreen` | `client/gui/EyedropScreen.java` | Group 10 | Minimal; add unified title bar + hint |
| G27.4 | `HudEditorScreen` | `client/gui/HudEditorScreen.java` | — | Full unification + snap-to-corner |
| G27.5 | `ShapeEditorScreen` | `client/gui/ShapeEditorScreen.java` | — | **New** — built to spec → **RETIRED, `/cb shapeeditor` now opens the Studio Shape section (§G27.11)** |
| G27.6 | `BlockCreationStudioScreen` | `client/gui/BlockCreationStudioScreen.java` | — | **New** — full block creation in one screen |
| G27.11 | Studio **Recolor** + **Shape** sections | `client/gui/StudioRecolorPanel.java` (+ studio) | — | **New** — `/cb recolor` (was livecolor) + `/cb shapeeditor` fold into the Studio; standalone screens retired |
| G27.12 | `BlockCreationStudioScreen` (Edit mode) | `client/gui/BlockCreationStudioScreen.java` | — | **New** — Create+Edit tabs; `/cb editor <id>` opens Edit (replaces the retired chest editor) |
| G27.17 | `VaultConflictScreen` | `client/gui/VaultConflictScreen.java` | Group 20 §S2 | **Folded in 2026-06-22** — re-skin to family standard (red). Spec → §G27.17 |
| G27.19 | `VariantRepaintScreen` | `client/gui/VariantRepaintScreen.java` | Group 06 (M3 hex) | **New** — self-contained batch repaint of all `_<colour>` variants after a hex change. Spec → §G27.19 |

Screen-specific upgrade specs are in §§ below. Nothing is built until you test and pass the previous screen.

---

## G27.1 — `ArabicPreviewScreen` upgrade

**Current state:** 3D cube, drag-rotate, zoom, color swatches, Create/Back. Gold title. `0xF0` backdrop. No undo, no shortcuts beyond R+Enter, no `[?]`, no title bar strip, no bottom action bar.

**Upgrades:**
- Backdrop `0xF0` → `0x33`
- Add dark title bar strip + gold border
- Add `[?]` button top-right
- Hint line 2: universal shortcuts
- Bottom action bar strip + gold border
- Button order: `[Undo] [Redo] [Rand] ··· [§aCreate] ··· [Copy] [Reset] [Back]`
  - `[Rand]` = randomize letter+background color combo
  - `[Copy]` = copy current color settings as code string
  - `[Undo]` / `[Redo]` = undo/redo color changes per session (persisted per block)
- Cancel confirmation dialog when clicking Back with un-created changes
- Save feedback: `[§aCreate]` flashes green on success + chat message
- `Ctrl+C` / `Ctrl+V` / `Ctrl+R` / `Ctrl+Z` / `Ctrl+Y` wired

**No changes to:** cube renderer, swatch logic, server payload, `refresh()` flow.

---

## G27.2 — `RecolorSliderScreen` upgrade

**Current state:** flat 2D 128×128 pixel preview box, 3 HSL sliders, `0xC0` backdrop, cyan title. No undo, no shortcuts, no title bar, no bottom bar.

**Upgrades:**
- Backdrop `0xC0` → `0x33`
- Flat 2D preview box → **3D drag-rotate cube** (same renderer as `ArabicPreviewScreen`) with real block texture fetched from `/tex/<id>` — upgrade grid from 32 to 56 cells for sharpness; HSL shift applied per cell live
- Add dark title bar strip + gold border; title: `§6§lLive Recolour §7— §f{id}`
- Sliders move to right panel (beside the cube) — labels + values stay the same
- Add `[?]` button top-right
- Hint line 1: `§7drag to rotate · scroll = spin · shift+scroll = zoom · R = reset`
- Hint line 2: universal shortcuts
- Bottom action bar: `[Undo] [Redo] [Rand] ··· [§aApply] ··· [Copy] [Reset] [Cancel]`
  - `[Rand]` = randomize all 3 sliders
  - `[Undo]` / `[Redo]` = undo/redo slider moves (persisted per block)
  - `[Copy]` = copy H/S/L values as code string
  - `[Reset]` = reset sliders to default (H=0 S=100% L=100%)
- Cancel confirmation dialog if sliders changed from defaults
- `Ctrl+C` / `Ctrl+V` / `Ctrl+R` / `Ctrl+Z` / `Ctrl+Y` / `Enter` wired

---

## G27.3 — `EyedropScreen` upgrade

**Current state:** fully transparent, world visible, custom crosshair, title + hint, no buttons. Minimal by design (must see world to sample it). `0x33` backdrop already correct.

**Upgrades:**
- Add dark title bar strip + gold border; title: `§6§lScreen Eyedrop`
- Hint line 1: `§7click any pixel · Esc = cancel`
- Hint line 2: `§8no keyboard shortcuts — click anywhere to sample`
- No bottom action bar (no buttons — Esc is the only exit)
- `[?]` button top-right (explains: click = sample pixel → prefills chat command; Esc = cancel)
- No undo needed — nothing is committed until player presses Enter in the chat prefill
- Crosshair style: stays as-is (white `+`)

**No changes to:** framebuffer sample logic, ChatScreen prefill flow.

---

## G27.4 — `HudEditorScreen` rebuild — Lego HUD Builder

> **Scope change (2026-06-16, dev-directed):** this is a **full rebuild**, not a light unification.
> `/cb edithud` becomes a **free-floating, brick-based HUD builder** — the in-game HUD is composed of
> independent "bricks" (one info line each), placed anywhere on screen with magnetic snapping, each
> independently styled. Still follows all Group 27 standards (frame, shortcuts, undo, cancel-confirm).
> This **supersedes** the original "snap-to-corner buttons" plan.
>
> **Why:** the current editor is a fixed 2-line (id + name) box with `−/+` click-spam controls and an
> 8-color cycle — looks like raw vanilla, almost no customization. The dev wants a "Lego" HUD: add /
> remove / swap / reorder any info brick, full per-brick styling, magnetic snapping, a real colour
> picker. **QoL first** (dev's stated priority). id + name are just the two default starter bricks.

### Concept — the HUD is a list of bricks

The HUD overlay = an ordered list of **bricks**. Each brick is one info line with its own position,
anchor, and style. Bricks are added from a palette, dragged anywhere (**free-floating**), snapped
magnetically to the screen and to each other, restyled, reordered (z-order), or deleted.

### Brick catalog

| Brick | Data source | Plumbing |
|---|---|---|
| Block ID | `ClientSlotCache` (customId) | already synced |
| Display name | `ClientSlotCache` (displayName) | already synced |
| Slot # | `SlotBlock.getSlotIndex()` | client-side (free) |
| Coordinates (x y z) | looked-at `BlockPos` | client-side (free) |
| World light | `world` light at pos | client-side (free) |
| Distance | player ↔ block | client-side (free) |
| Facing side | `BlockHitResult.getSide()` | client-side (free) |
| Custom text | user-entered string | client-side (free) |
| Header / title | user-entered string (styled) | client-side (free) |
| Divider | static rule line | client-side (free) |
| Category | `SlotData.category` | needs sync expansion |
| Light value (glow) | `SlotData.glow` | needs sync expansion |
| Hardness | `SlotData.hardness` | needs sync expansion |
| Sound type | `SlotData.soundType` | needs sync expansion |
| Shape | `SlotData.shape` | needs sync expansion |
| Solid / passable | `SlotData.noCollision` | needs sync expansion |

**Deferred — no data in `SlotData`:** Author/credit · Source URL · Texture type (gif/video) ·
Resolution. These would need an attribution feature added to block creation — **not in this build**
(dev confirmed: ship without them).

### Per-brick controls

Each brick row in the editor's brick list has: 👁 show/hide · ⠿ drag-reorder (z-order) ·
⚙ inspector · 🗑 delete · type-swap.

Brick **inspector** (⚙), per brick:
- **Size** (own scale 0.5–3.0) · **Colour** (full picker) · **Bold** · **Shadow** ·
  **Prefix label** (e.g. `ID:`) · **Align** L / C / R.
- **Effects** (optional, off by default): **Rainbow** cycle · **Pulse** / glow · **Gradient**.
  Cosmetic only; a brick with no effect renders as a plain solid-colour line.
- **Background override**: a brick may override the global background (own colour + opacity) or turn
  its background off. See *Backgrounds*.

### Free-floating positioning + anchors

- Each brick stores `(offsetX, offsetY, anchor)` where `anchor ∈ {TL, TR, BL, BR, CENTER}`. Position
  resolves relative to the anchor so bricks stay put across resolutions / GUI-scale changes.
- Drag any brick in the live preview to move it. Snapping **sets the anchor automatically** (snap to
  the right edge → anchor becomes the right edge).
- **Shift** while dragging = free move (snap off). **Arrow keys** = nudge selected brick 1px
  (**Shift+arrow** = 10px).

### Magnetic snap — the headline feature (`HudSnap`)

Figma-style smart alignment while dragging:
- Snap candidates: screen edges (with a small gutter margin), screen H/V centre, the rule-of-thirds
  lines, **and every other brick's** left / right / top / bottom / centreX / centreY.
- For the dragged brick's left / right / centreX, snap to the nearest vertical candidate within a
  threshold (~6px); same logic for Y.
- Matching alignment lines draw in **cyan** (`0xFF39E0C8`), extended across the relevant axis while the
  snap holds.
- `[Snap On/Off]` toggle in the panel; **Shift** temporarily disables.

### Full colour picker (`HudColorPicker`)

In-screen popup widget (dev chose the richest option):
- Saturation/Value square (drag) + Hue slider
- Hex input (`#RRGGBB`) + R/G/B numeric readout
- Preset swatch palette row
- **Recent-colours** row (persisted in `HudConfig`)
- **Eyedropper** — "pick from screen" button; reuses `EyedropScreen` (G27.3), returns the sampled
  colour to the picker.

Drives the brick inspector's Colour control (text **and** per-brick background colour); live-updates
the brick in the preview.

### Backgrounds

- A **global** default background (colour + opacity) draws behind every brick that hasn't overridden
  it (default: subtle dark ~40%, matching today's HUD).
- Any brick may **override** with its own background colour + opacity, or turn its background off.
- Bricks float independently, so there is no single shared box — each brick draws its own background
  pad sized to its own text.

### Presets, browsing & sharing

**Built-in presets (3, browsable):**
- **Minimal** — Name only, small, top-left
- **Detailed** — Name + ID + Slot, stacked top-left
- **Builder** — Coords + Light + Distance (build/debug)

**Custom presets:** `[Save as…]` stores the current layout as a named preset. Saving a name that
already exists → in-screen **"overwrite?"** confirm (Yes/No) — never silent data loss.

**Preset browser:** an in-screen picker listing built-ins + saved presets, each shown with a **mini
rendered thumbnail** of its HUD layout (not just a name). Click to load.

**Import / export (both):**
- **Code string** — `[Export]` copies the preset as a compact text code → clipboard + chat;
  `[Import]` pastes a code to add it. For quick sharing (Discord, chat).
- **File** — `.json` written to / read from `config/customblocks/exports/Hud_Presets/` (the `exports/`
  dir already exists per `BlockExporter`; the `Hud_Presets` subfolder is created on first save). For
  backups.
- Round-trip safe: export → import reproduces the exact layout (positions, anchors, styles, effects).

### Brick visibility (when a brick shows in-game)

Smart, by brick family — no per-brick setting needed:
- **Block-info bricks** (ID, Name, Slot, Category, Glow, Hardness, Sound, Shape, Solid) render **only
  while the crosshair is on a custom block** (like today's HUD).
- **World / custom bricks** (Coords, Light, Distance, Facing, Custom text, Header, Divider) render
  **always**.
- A brick hidden via its 👁 toggle never renders, regardless of family.

### Master switch & default layout

- **Master HUD on/off** toggle in the editor panel — hides/shows the whole HUD without deleting
  bricks. Mirrors `CustomBlocksConfig.hudEnabled` / `/cb config hud`.
- **Fresh-install default layout** (dev chose "a bit nicer"): **Name** brick larger on top, **ID**
  brick smaller beneath, top-left. Replaces the old equal-size 2-line stack. Existing users' saved
  configs migrate instead (see *Data + sync changes*).

### Hover sound (look-at feedback)

A sound that plays **once** when the crosshair newly lands on a block (edge-triggered, not every
frame). Configured in the editor panel via a CB-styled dropdown matching other features:
- **Trigger** (3-way): **None** · **Custom blocks only** · **Any block**.
- **Sound** dropdown (17): None, Bamboo, Stone, Wood, Amethyst, Note Pling, Glass, Bell, Wool, Copper,
  Sand, Grass, Nether, Froglight, Sculk, Lodestone, Anvil. (Vanilla `SoundEvents`; bare `SoundEvent`
  per NFR-12.)
- **Volume** slider 0–100%.
- **Preview on pick** — selecting a sound in the dropdown plays it once.
- Persisted in `HudConfig` (`hoverTrigger`, `hoverSound`, `hoverVolume`).

### Keybinds & config

Three client-side, per-player keybinds registered with Fabric `KeyBindingHelper` under a
**"CustomBlocks"** category in vanilla Options → Controls:

| Action | Default key |
|---|---|
| Toggle HUD on/off | `H` |
| Open CustomBlocks Menu | `Right Shift` |
| Open HUD Editor | `Right Ctrl` |

- Single keys only (vanilla Controls cannot store chords). All three keys are **unbound in vanilla by
  default**, so the chosen defaults don't clash.
- **Primary rebind route = vanilla Options → Controls** — free, because these are real `KeyBinding`s.
  No extra UI needed to rebind.
- **Optional convenience routes:** a chat command `/cb config keybind <toggle_hud|menu|editor> <key>`,
  and (optional polish) a Keybinds section in the **client** `ConfigScreen`. ⚠️ Any in-mod rebind UI must
  be a client `Screen` (it has to capture a key press) — **not** the chest-GUI config (`GuiRouter` / `Nav`),
  which can't read the keyboard. There is **no "server keybind"** — Minecraft keybinds are always
  client-local.
- A client tick handler polls `wasPressed()` and runs the action (toggle flips `HudConfig.visible` +
  saves; menu sends `/cb`; editor opens `HudEditorScreen`).

### Layout (Group 27 standard frame)

```
╔══════════════════════════════════════════════════════════════╗
║  §6§lHUD Editor                                        [?]  ║
║  §7drag bricks to place · magnetic snap · arrows nudge · shift = free ║
║  §8Ctrl+Z undo · Ctrl+C copy · Enter save · ? help          ║
╠══════════════════════════════════════════════════════════════╣

   [ live HUD preview — the real block you aim at, else a sample ]
   [ cyan smart-guide lines appear while dragging a brick ]

                          ┌─ BRICK PANEL (right dock) ──┐
                          │ [Preset ▾]                  │
                          │ ⠿ 👁 Name    1.4x ■ ⚙ 🗑    │
                          │ ⠿ 👁 ID      0.8x ■ ⚙ 🗑    │
                          │ ⠿ 👁 Slot    0.6x ■ ⚙ 🗑    │
                          │ [+ Add brick ▾]             │
                          │ Snap [On]   Bg [40%]        │
                          └─────────────────────────────┘
╠══════════════════════════════════════════════════════════════╣
║  [Undo] [Redo]        [§aSave]        [Copy] [Reset] [Cancel] ║
╚══════════════════════════════════════════════════════════════╝
```

### Bottom action bar
`[Undo] [Redo] ··· [§aSave] ··· [Copy] [Reset] [Cancel]` — Group 27 standard.
- `[Reset]` restores the default bricks (id + name, top-left).
- `[Copy]` / `[Paste]` export/import the whole HUD layout as a string (clipboard + chat).
- Cancel confirmation if the layout changed since the editor opened.
- Save flashes green ~600ms + chat message; writes `hud-config-server.json`.

### Editor feedback sound (Royal Directive §2)
Distinct from the *Hover sound* above — this is editor UI feedback: snap tick when a brick snaps ·
soft click on slider/swatch change · confirm chime on Save. Uses `SoundEvents` UI sounds — bare
`SoundEvent` (only `BLOCK_NOTE_BLOCK_*` needs `.value()`, NFR-12).

### Data + sync changes

- **`HudConfig` rewritten.** Was 7 flat fields (x/y/scale/color/bg/showId/showName); becomes box-level
  globals (snap on/off, master scale, default bg, recent colours) + an ordered `List<HudField>`. Old
  saved files **auto-migrate**: existing `hudX/hudY/hudColor/…` map to two bricks (ID + Name) at the
  saved position, so current users see no regression.
- **Sync bug fixed.** `HudSync` joins id+name with ` `, but `ClientSlotCache` splits on the first
  **space** — names with spaces split wrong, one-word names drop the slot entirely. The rebuild moves
  the per-slot payload to a small **structured JSON** object (id, name + the sync-brick fields),
  killing the fragile string split.

### New / changed files (all ≤500-line gate; `HudConfig` ≤300)

| File | New / changed | Purpose |
|---|---|---|
| `client/hud/HudFieldType.java` | new | Brick catalogue enum + per-type label/icon + value resolver(context) |
| `client/hud/HudField.java` | new | One brick instance (type, offset, anchor, style) + JSON |
| `client/HudConfig.java` | rewrite | Box globals + brick list + migration + atomic save (≤300) |
| `client/HudRenderer.java` | rewrite | Resolve anchored positions + render the brick list |
| `client/hud/HudSnap.java` | new | Smart-guide snap math + cyan guide drawing |
| `client/gui/HudEditorScreen.java` | rewrite | Group 27 frame + free-floating drag + panel + sound |
| `client/gui/hud/HudBrickRow.java` | new | One brick-list row widget (toggle / reorder / inspect / delete) |
| `client/gui/hud/HudBrickPalette.java` | new | Add-brick popup |
| `client/gui/hud/HudColorPicker.java` | new | Full colour picker popup (SV square + hue + hex + swatches + recents + eyedrop) |
| `client/gui/hud/HudBrickInspector.java` | new | Per-brick settings popup (size/colour/bold/shadow/prefix/align/effects/bg) |
| `client/gui/hud/HudPresetBrowser.java` | new | Preset picker with mini-thumbnail previews + Save as / Import / Export |
| `client/hud/HudPresetStore.java` | new | Built-in + saved presets; code-string + `.json` import/export (`exports/Hud_Presets/`) |
| `client/hud/HudHoverSound.java` | new | Look-at hover sound: trigger/sound/volume + edge-trigger + preview |
| `client/CbKeybinds.java` | new | Registers the 3 keybinds + tick handler (toggle / menu / editor) |
| `network/HudSync.java` | change | Structured per-slot JSON (fixes NUL/space bug) + sync-brick fields |
| `client/ClientSlotCache.java` | change | Parse structured JSON; expose brick fields |
| `command/handlers/ConfigCommands.java` | change | `/cb config keybind <action> <key>` chat route |
| `gui/screens/ConfigScreen.java` | change (optional) | Optional in-mod Keybinds section — client `Screen` only |

### Build order (within G27.4 — built green at each step, ONE in-game test at the very end per dev)
1. **Data model** — `HudFieldType`, `HudField`, `HudConfig` rewrite (brick list, backgrounds, hover-sound + keybind fields) + migration + nicer default. Build green.
2. **Renderer** — `HudRenderer` rewrite (brick list, anchors, per-brick bg, effects, visibility rule). HUD shows in-game.
3. **Editor UI** — `HudEditorScreen` + brick rows + palette + inspector + Group 27 frame; free-floating drag; collapsible panel; master switch.
4. **Snap engine** — `HudSnap` smart guides + arrow nudge + editor feedback sound.
5. **Colour picker** — `HudColorPicker` + eyedrop (reuse `EyedropScreen`).
6. **Hover sound** — `HudHoverSound` (trigger / sound / volume / preview) + edge-trigger in renderer/mixin.
7. **Keybinds** — `CbKeybinds` (3 keys) + `ConfigScreen` / `ConfigCommands` rebind routes.
8. **Sync bricks** — `HudSync` / `ClientSlotCache` structured JSON (fixes bug) + category/glow/etc bricks.
9. **Presets** — `HudPresetStore` + `HudPresetBrowser` (built-ins, save-as, mini-preview, code + file import/export, overwrite-confirm).
10. **Polish.**

### Build notes / gotchas (read before the build session)
- **`HudSyncPayload` is unchanged** — already `record HudSyncPayload(String indexJson)` with a 1 MB string
  codec, so the structured-JSON switch is purely a content change inside that string (no codec /
  registration change).
- **The separator bug is three-way:** `HudSync` writes ` `, `ClientSlotCache` splits on a space, and
  `HudSyncPayload`'s own doc-comment claims `id:DisplayName`. Structured per-slot JSON removes all three
  ambiguities at once.
- **`HudConfig` ≤300-line gate:** the rewrite adds a brick list + backgrounds + hover-sound + keybind +
  recent-colours fields + migration. If it would exceed 300, split persistence/serialisation into a
  `HudConfigStore` (brick (de)serialise already lives in `HudField`). Keep `HudConfig` lean.
- **Keybinds are real `KeyBinding`s** → vanilla Controls rebinding is free; the `ConfigScreen` Keybinds
  section is optional polish and must be a client `Screen`, never the chest GUI.
- **Build env (CLAUDE.md §6 / memory):** JDK 21 only (PATH `java` is Java 8) — set `JAVA_HOME` to 21 +
  `--no-daemon`. Gates: `verifyMojibake`, `verifySound`, `verifyFileSize`.

---

## G27.5 — `ShapeEditorScreen` (new)

> Spec from the original shapeeditor design discussion (2026-06-16).

**Entry:** `/cb shapeeditor` (no arg) → server opens block-picker chest GUI → player clicks block → `OpenGuiPayload(SHAPE_EDITOR, id)` → client opens `ShapeEditorScreen`. Two modes: Edit Existing (loads saved boxes) or New Shape (starts with one full-cube AABB).

**Layout:**

```
╔══════════════════════════════════════════════════════════════╗
║  §6§lShape Editor §7— §fmy_block                      [?]  ║
║  §7drag to rotate · scroll = spin · click box = select      ║
║  §8Ctrl+Z undo · Ctrl+C copy · Enter save · ? help          ║
╠════════════════╦═══════════════════════╦═════════════════════╣
║  BOX LIST      ║   3D LIVE PREVIEW     ║  COORDINATE EDIT   ║
║  ──────────── ║                       ║  ────────────────   ║
║  □ Box 1  ◄  ║  [block texture cube] ║  X1: [0.000  ▲▼]  ║
║  □ Box 2     ║  [colored wireframe   ║  Y1: [0.000  ▲▼]  ║
║  □ Box 3     ║   cage per AABB]      ║  Z1: [0.000  ▲▼]  ║
║              ║                       ║  X2: [1.000  ▲▼]  ║
║  [+ Add Box] ║  drag=rotate          ║  Y2: [1.000  ▲▼]  ║
║  [- Remove]  ║  scroll=spin          ║  Z2: [1.000  ▲▼]  ║
║              ║  shift+scroll=zoom    ║                    ║
║              ║  click box=select     ║  Snap: [1/16 ▾]   ║
║              ║  R=reset              ║  [Mirror X] [Mirror Z] ║
╠══════════════╩═══════════════════════╩═════════════════════╣
║  PRESETS: [Full] [Slab↓] [Slab↑] [Thin] [Carpet] [Pillar] ║
║           [Pane] [Cross] [Button] [Plate] [+ Save as…]     ║
╠══════════════════════════════════════════════════════════════╣
║  [Undo] [Redo] [Rand]      [§aSave]      [Copy] [Revert] [Cancel] ║
╚══════════════════════════════════════════════════════════════╝
```

**Features:**
- 3D cube with real block texture + colored wireframe AABB box per collision box (distinct color per box)
- Click wireframe box in preview → selects it in the box list
- Left panel: scrollable box list, [+ Add Box], [- Remove]
- Right panel: X1 Y1 Z1 X2 Y2 Z2 with ▲▼ nudge; snap toggle (1/16, 1/8, 1/4, 1/2, free); Mirror X / Z
- Preset bar: Full, Slab↓, Slab↑, Thin, Carpet, Pillar, Pane, Cross, Button, Plate; [+ Save as…] saves client-side named preset
- Undo/redo stack persisted per block
- [Rand] = random preset from the built-in list
- [Copy] = export shape as string → clipboard + chat
- [Revert] = restore last server-saved shape
- Save sends `ShapeEditorPayload` to server; ESC triggers cancel confirmation

---

## G27.6 — `BlockCreationStudioScreen` (new)

> `/cb create` (no args) opens this screen. The existing CLI (`/cb create <id>`, `/cb create <id> <name>`, `/cb create <id> <name> <url>`) is **completely untouched**.

### What changes

| | Old `/cb create` (no args) | New |
|---|---|---|
| Entry | Error / no-op or help text | Opens `BlockCreationStudioScreen` |
| Flow | Multiple commands in sequence | Everything in one screen, never leave |
| Feedback | Terse chat messages | Live 3D preview updates with every change |
| Error | Fails silently or prints error | Sidebar highlights the problem field, scrolls to it |
| Session | Stateless — restart from scratch each time | Remembers last session on close |

### Overall layout

Follows **all Group 27 standards** (`CbScreenTemplate` as base):
- `0x33` backdrop (world visible)
- `§6` gold title bar strip + gold border
- Title: `§6§lBlock Creation Studio`
- Two hint lines
- Bottom action bar
- Cancel confirmation, save feedback, `[?]`, Ctrl+Z/Y/C/V/R/Enter/Esc

```
╔══════════════════════════════════════════════════════════════════════╗
║  §6§lBlock Creation Studio                                     [?]  ║
║  §7{breadcrumb: Main  OR  ‹ Main › Texture › Color}                 ║
║  §8Ctrl+Z undo · Ctrl+C copy config · Enter = create · ? help       ║
╠══════════════════════════╦═══════════════════════════════════════════╣
║  LEFT SIDEBAR            ║   CENTER: 3D LIVE PREVIEW                ║
║  (switches modes)        ║                                          ║
║  [see sub-sections]      ║   [spinning cube — texture + shape]      ║
║                          ║                                          ║
║                          ║   spin X% · zoom Y%                      ║
╠══════════════════════════╩═══════════════════════════════════════════╣
║  [Undo] [Redo]   [§aDraft]   [§aCreate & Publish]   [Copy Config] [Cancel] ║
╚══════════════════════════════════════════════════════════════════════╝
```

### Left Sidebar — Main Mode

> Default state when the studio opens. Five section rows + two utility rows.

```
╔════════════════════════╗
║ [Clear All]            ║  ← clears all fields; resets to blank slate
║ [Start from Template ›]║  ← loads all settings from an existing block (in-screen overlay picker)
╠════════════════════════╣
║ ✔ Identity          ›  ║  ← ✔ = configured  ⬜ = missing/required
║   ID: my_block         ║
║   Name: My Block       ║
╠════════════════════════╣
║ ⬜ Texture          ›  ║
║   (not set)            ║
╠════════════════════════╣
║ ✔ Shape: Full       ›  ║
║                        ║
╠════════════════════════╣
║ ✔ Attributes        ›  ║
║   Glow 0 · Wood        ║
╠════════════════════════╣
║ ✔ Organize          ›  ║
║   No category          ║
╚════════════════════════╝
```

- `✔` = section has valid data. `⬜` = missing required data (pulsing orange on Create attempt).
- Click any row → sidebar switches to that section's panel (breadcrumb updates).
- [Start from Template ›] → opens an in-screen overlay block picker. Pick any existing block → all fields pre-fill from it; ID is cleared (player picks a new one).
- [Clear All] at top — resets all fields + texture; breadcrumb returns to Main.

### Left Sidebar — Identity Panel

**Breadcrumb:** `‹ Main › Identity`

```
╔════════════════════════╗
║ Block ID               ║
║ [my_block____________] ║  ← text input; live validation
║ §a✔ available          ║  ← or §cID already taken
║                        ║
║ Display Name           ║
║ [My Block____________] ║  ← text input
║ §814/64 chars          ║
║                        ║
║ [Auto-ID from name]    ║  ← generates ID from name (snake_case)
║                        ║
║ [Back]                 ║
╚════════════════════════╝
```

- ID validation fires on every keystroke: trims spaces, lowercases, removes invalid chars, checks against existing IDs.
- [Auto-ID from name] converts display name → snake_case ID suggestion.
- Char counter on name field.

### Left Sidebar — Texture Panel

**Breadcrumb:** `‹ Main › Texture`

Tool tabs at top. Active tab highlighted. One tool at a time.

```
╔════════════════════════╗
║ [§aURL] [Color] [AI] [Drop] ║  ← tool tabs
╠════════════════════════╣
║ URL:                   ║  ← active tab = URL
║ [____________________] ║
║ [Load]    §8loading... ║  (or §a✔ loaded / §c✗ failed)
╠════════════════════════╣
║ Per-face: [Off ▾]      ║  ← toggle (Off / On)
║   (when On: 6 URL fields — Top/Bottom/North/South/East/West)
║ Animated: §8auto-detect║
║ Remove BG: [Off]       ║
║                        ║
║ [Back]                 ║
╚════════════════════════╝
```

**Color tab** (full color studio in the sidebar):

```
╔════════════════════════╗
║ [URL] [§aColor] [AI] [Drop] ║
╠════════════════════════╣
║ PALETTE                ║
║ ■ ■ ■ ■ ■ ■ ■ ■       ║  ← 12 curated colors (6×2 grid)
║ ■ ■ ■ ■ ■ ■ ■ ■       ║  ← click to set as base color
╠════════════════════════╣
║ TUNE (HSL)             ║
║ Hue:  ─────o──── +12° ║
║ Sat:  ──o──────  80%  ║
║ Lgt:  ───────o─  110% ║
╠════════════════════════╣
║ Custom: [#FF4400    ]  ║  ← hex input
║ [Eyedrop from screen]  ║  ← opens EyedropScreen; returns color
║                        ║
║ [Back]                 ║
╚════════════════════════╝
```

Center cube updates live as palette / sliders / hex change.

**AI tab:**

```
╔════════════════════════╗
║ [URL] [Color] [§aAI] [Drop] ║
╠════════════════════════╣
║ Describe the texture:  ║
║ [____________________] ║
║ [____________________] ║
║ [Generate]             ║
║ §8(uses AI texture gen)║
║                        ║
║ [Back]                 ║
╚════════════════════════╝
```

**Eyedrop tab:** clicking the tab opens `EyedropScreen`; on color pick, returns to studio with color applied.

### Left Sidebar — Shape Panel

**Breadcrumb:** `‹ Main › Shape`

Center cube shows texture + colored wireframe AABB cage. Same AABB editor design as G27.5, embedded here.

```
╔════════════════════════╗
║ PRESETS                ║
║ [Full] [Slab↓] [Slab↑] ║
║ [Thin] [Carpet][Pillar]║
║ [Pane] [Cross] [Button]║
║ [Plate]                ║
╠════════════════════════╣
║ CUSTOM AABB            ║
║ Box 1 ◄ (selected)     ║
║ Box 2                  ║
║ [+ Add Box] [− Remove] ║
╠════════════════════════╣
║ X1 [0.000 ▲▼]          ║
║ Y1 [0.000 ▲▼]          ║
║ Z1 [0.000 ▲▼]          ║
║ X2 [1.000 ▲▼]          ║
║ Y2 [1.000 ▲▼]          ║
║ Z2 [1.000 ▲▼]          ║
║ Snap: [1/16 ▾]         ║
║ [Mirror X] [Mirror Z]  ║
╠════════════════════════╣
║ [Back]                 ║
╚════════════════════════╝
```

- Preset click → replaces all boxes + updates cube wireframe.
- Coordinate ▲▼ nudge → updates wireframe live.
- Click wireframe box in center preview → selects that box in list.
- Undo/redo tracks shape changes per session.

### Left Sidebar — Attributes Panel

**Breadcrumb:** `‹ Main › Attributes`

```
╔════════════════════════╗
║ Glow                   ║
║ 0 ──────o──────── 15   ║
║ Current: 4             ║
║ (cube glows in preview)║
╠════════════════════════╣
║ Hardness               ║
║ 0 ──o──────────── 10   ║
║ Current: 2.0 (Wood)    ║
╠════════════════════════╣
║ Sound Set              ║
║ [Wood ▾]               ║  ← dropdown
╠════════════════════════╣
║ Has Collision: [On ▾]  ║
║ Transparent:   [Off ▾] ║
║                        ║
║ [Back]                 ║
╚════════════════════════╝
```

Hardness shows named preset labels at common values (Soft 0.5 / Wood 2.0 / Stone 3.0 / Iron 5.0 / Hard 10.0).

### Left Sidebar — Organize Panel

**Breadcrumb:** `‹ Main › Organize`

```
╔════════════════════════╗
║ Category               ║
║ [None ▾]               ║  ← dropdown + [Browse...]
╠════════════════════════╣
║ [★ Mark as Favorite]   ║  ← toggles
║ [📄 Draft mode: Off]   ║
╠════════════════════════╣
║ Notes                  ║
║ [____________________] ║
║ [____________________] ║
╠════════════════════════╣
║ [Save as Blueprint]    ║  ← saves full studio config as named blueprint
║ [Back]                 ║
╚════════════════════════╝
```

### 3D Live Preview (center — always visible)

Same `MatrixStack` cell-grid cube renderer as `ArabicPreviewScreen` (56-cell grid, ambient + diffuse shading):

| State | What shows on cube |
|---|---|
| No texture set | Flat grey / checkerboard with `§8No texture` label |
| URL loading | Spinner overlay — `§8loading texture…` |
| URL failed | Red-tinted cube — `§cURL failed` |
| Texture ready | Real texture, live HSL shifts if Color tab active |
| Shape ≠ full | Cube clipped to shape + colored wireframe AABB cage |
| Glow > 0 | Subtle glow aura effect around cube |
| Animated (GIF) | Texture animates on the cube (live) |

**Camera:** drag rotate · scroll = spin speed · shift+scroll = zoom · click = pause/resume · `R` = reset
**Corner readout:** `§8spin X% · zoom Y%`
**Auto-spin on open.**

### Validation

When [§aCreate & Publish] or [§aDraft] is clicked with missing/invalid fields:

1. Sidebar auto-navigates to the **first problem** section (back to Main → then into the problem panel)
2. The specific field is outlined in red
3. Error text in `§c` appears directly below the field
4. Scrolls to make it visible if needed

Required fields: Block ID (valid + available), Display Name (non-empty). All others optional.

### Bottom Action Bar

```
[Undo] [Redo]   [§aDraft]   [§aCreate & Publish]   [Copy Config] [Cancel]
```

- `[§aDraft]` — creates block as draft, closes studio. Flash green + chat: `§aBlock 'my_block' saved as draft.`
- `[§aCreate & Publish]` — creates block published. Flash green + chat: `§a✔ Block 'my_block' created.`
- `[Copy Config]` — copies full studio state as a string → clipboard + chat
- `[Cancel]` — confirmation dialog if any field is non-default

### Session memory

On close, studio saves current state per-player to disk:
- All field values (ID, name, URL, colors, HSL values, shape, attributes, organize)
- Last texture loaded (grid pixels cached — no re-fetch on reopen)
- Last sidebar mode + panel open

On `/cb create` (no args) reopen, all fields restore. Player can clear with [Clear All] (top of sidebar above Template row).

### G27.6 keyboard shortcuts

All Group 27 universals plus:

| Key | Action |
|---|---|
| `Ctrl+Z` | Undo last change (any field) |
| `Ctrl+Y` / `Ctrl+Shift+Z` | Redo |
| `Ctrl+C` | Copy full studio config code |
| `Ctrl+V` | Paste config code → fills all fields |
| `Ctrl+R` | Randomize active tool (color = random palette; shape = random preset; attributes = random glow) |
| `Enter` | If ID + texture set: Create & Publish |
| `Esc` | Cancel (confirm if dirty) |
| `Tab` | Next input field |
| `R` | Reset cube view |

### G27.6 command change

`CommandRegistrar` / `CreationCommands.java` — add a no-arg branch to `/cb create` that sends `OpenGuiPayload(CREATE_STUDIO)` to the calling player. The existing 1/2/3-arg branches are **untouched**.

---

## Build order

Build and test **one screen at a time**, in this order:

1. **G27.1** `ArabicPreviewScreen` — simplest upgrade; validates the title/bottom bar pattern
2. **G27.2** `RecolorSliderScreen` — validates the 3D cube upgrade path
3. **G27.3** `EyedropScreen` — minimal; validates title bar on a world-visible screen
4. **G27.4** `HudEditorScreen` — validates snap + undo on a non-cube screen
5. **G27.5** `ShapeEditorScreen` — new build; uses everything from G27.1–G27.4
6. **G27.6** `BlockCreationStudioScreen` — new build; requires G27.1–G27.5 all confirmed in-game

---

## All new files

| File | Step | Purpose |
|---|---|---|
| `client/gui/CbScreenTemplate.java` | Design standard | Canonical template all new screens must follow |
| `client/hud/HudFieldType.java` | G27.4 | Brick catalogue enum + value resolver |
| `client/hud/HudField.java` | G27.4 | One brick instance + JSON |
| `client/hud/HudSnap.java` | G27.4 | Magnetic smart-guide snap math + drawing |
| `client/gui/hud/HudBrickRow.java` | G27.4 | Brick-list row widget |
| `client/gui/hud/HudBrickPalette.java` | G27.4 | Add-brick popup |
| `client/gui/hud/HudColorPicker.java` | G27.4 | Full colour picker popup |
| `client/gui/hud/HudBrickInspector.java` | G27.4 | Per-brick settings popup |
| `client/gui/ShapeEditorScreen.java` | G27.5 | Shape editor screen |
| `network/payloads/ShapeEditorPayload.java` | G27.5 | C2S shape data |
| `client/gui/BlockCreationStudioScreen.java` | G27.6 | Full creation studio screen |
| `client/gui/studio/StudioSidebarMain.java` | G27.6 | Main section-row panel |
| `client/gui/studio/StudioSidebarIdentity.java` | G27.6 | Identity sub-panel |
| `client/gui/studio/StudioSidebarTexture.java` | G27.6 | Texture sub-panel (URL/Color/AI/Eyedrop tabs) |
| `client/gui/studio/StudioSidebarShape.java` | G27.6 | Shape sub-panel (presets + AABB editor) |
| `client/gui/studio/StudioSidebarAttributes.java` | G27.6 | Attributes sub-panel |
| `client/gui/studio/StudioSidebarOrganize.java` | G27.6 | Organize sub-panel |
| `network/payloads/CreateStudioPayload.java` | G27.6 | C2S block creation data |
| `core/StudioSession.java` | G27.6 | Per-player session persistence |

---

## G27.7 — Post-Test Corrections & Polish Pass (locked 2026-06-17)

> **Status:** design locked with the dev on 2026-06-17 after the first in-game test of the built G27
> screens (§1–§5). **Nothing here is built yet.** Build in small slices, dev tests each slice before
> the next (CLAUDE.md §2, §4). This section is the authority for the corrections; the testing guide
> gets per-slice test steps only as each slice is built.
>
> **How we got here:** the dev tested all the screens and reported: heavy lag on every 3D screen, the
> background dim is uncontrollable + too transparent, the `[?]` help renders *behind* the block, the
> bottom action bar eats too much space, the block vanishes (mainly while spinning), the Arabic
> letter/background swatches are disorganized, `/cb livecolor` + `/cb shapeeditor` should open a block
> picker when given no id, the recolor controls look plain and want more tools, eyedrop is unclear +
> needs a hide-panel button, the HUD editor feels dark/confusing, and `/cb shapeeditor` is "not a
> registered command."

### Proposed build order (small slices, one in-game test each)

1. **Cube renderer rebuild** (shared) — fixes lag + disappearing block; unblocks the `[?]`-on-top fix. Highest leverage, touches every 3D screen at once. ✅ **dev-confirmed in-game 2026-06-17** (lag gone, vanish fixed, `[?]` now draws on top / §A3).
2. **Shared frame fixes** — **§A6 `[?]` overlay revamp** (red [X], no click-dismiss, auto-size, organised — `CbHelpOverlay`) · dim slider + default (§A2) · dockable+hideable+smaller bottom bar (§A4). *(§A6 built 2026-06-17, awaiting in-game test; A2/A4 not started.)*
3. **No-arg block pickers** — `/cb livecolor` and `/cb shapeeditor` open the picker; fix the duplicate `shapeeditor` registration.
4. **Floating panel + smart-colour system** — the reusable panel primitive (Arabic first, then reused).
5. **Recolor tools** — gradient sliders + the 4 new tune tools.
6. **Eyedrop polish** — intro + hide-panel + dropper/loupe.
7. **HUD editor overhaul** — restyle, guidance, Advanced fold, drag/snap, centered-name fix.

---

### A. Shared across all 3D screens (`PreviewCube` + the copy-pasted frame)

All five cube screens share `client/gui/PreviewCube.java` and a duplicated frame. Fix each root once.

**A1 — Lag (DECISION: proper rebuild).**
Cause: the cube is painted as individual `ctx.fill` rectangles — `GRID=56` → 56×56×6 ≈ **18,800 fills/frame**, redrawn every frame even while only spinning. Shape editor multiplies this per box.
Fix: bake each face into **one image (dynamic texture) drawn as a single textured quad** — 6 quads/frame instead of ~18,800 fills. Re-bake a face only when its pixels change (e.g. recolor slider moved); spinning just redraws the same 6 textured quads. Lives in `PreviewCube` so all screens benefit. (Reject: the quick `GRID 56→28` softening — dev chose the full rebuild.)

**A2 — Background dim (DECISION: slider + sensible dark default).**
Cause: `BACKDROP = 0x33000000` hardcoded (~20% black) on every screen, no control.
Fix: an in-screen **dim slider** from clear → fully black, **default to a solid darker level (~60%, not 0)**, persisted to disk so it sticks across opens. **Exception:** `EyedropScreen` must stay faint (it samples the world) — keep its low default, slider optional there.

**A3 — `[?]` help renders behind the block (DECISION: straight fix).**
Cause: cube faces carry real depth (`m.translate(0,0,half)` pushes them toward the viewer); the help + cancel-confirm overlays are drawn flat at depth 0, so depth-testing puts the cube in front.
Fix: draw the help overlay **and** the cancel-confirm overlay at a high Z (matrix translate to the front) so they always sit on top. Applies to every cube screen.

**A4 — Bottom action bar (DECISION: dockable + hideable + smaller).**
Cause: fixed full-width 42px strip, 7 buttons, hardcoded.
Fix: rework the shared bar into a **movable panel** — dockable to the **left or right** edge as a vertical strip, **plus** a hide/show toggle in its corner that collapses it to a thin sliver. Make the buttons **smaller** to free screen space. Build a hide toggle first, then docking. Reuse the floating-panel primitive (§B) so it behaves like the colour panel.

**A5 — Block disappears (DECISION: fixed by A1, investigate to confirm).**
Reported: vanishes **mainly while spinning** (and "almost everything"). Most likely cause: the matrix-fill geometry crosses the GUI depth-clip range at certain rotations and gets culled. The A1 textured-quad rebuild keeps geometry depth controlled and should remove it. Investigate in-game during A1 to confirm it's gone; if not, clamp the cube's Z extent / disable depth-test around the cube draw.

**A6 — `[?]` help overlay revamp (DECISION, dev-flagged 2026-06-17 in slice-1 test).**
The slice-1 test confirmed the `[?]` overlay now draws **on top** (A3 ✓) — but the overlay itself is poor:
the long shortcut lines **overflow the box** (fixed ~320px panel), it is cramped/unorganised, and **any
click dismisses it** so a stray click makes it vanish. Dev wants: a **red [X] close button top-right**;
the overlay **only closes on the red [X]** (clicks elsewhere are swallowed); fix the overflow; organise it;
"make it actually really good looking."
Fix: one shared `client/gui/CbHelpOverlay.java` — auto-sized panel (measures the widest key + description
column so nothing overflows), gold-bordered dark panel + header bar with the title and a red **[X]**,
key/description rows grouped under **VIEW** / **EDIT** headings, footer hint. Clicks are consumed while
open; only the [X] (or Esc, so the player isn't trapped) closes it. Replaces the copy-pasted `renderHelp`
in the 3 cube screens (Arabic / Recolor / Shape); Eyedrop (§D) and HUD (§E) adopt it in their own slices.
This belongs to **slice 2 (shared frame fixes)**, not slice 1.

---

### B. Floating panel + smart-colour system (new shared primitive)

> Grew out of "make the Arabic colour panel draggable + customizable." The dev confirmed **all of it**.
> Build it **once** as a reusable floating panel and reuse it for the Arabic colours, the recolor
> panel, the bottom action bar (§A4), and the HUD panel — consistent feel everywhere (CLAUDE.md §5).

**Arabic layout (DECISION: Option A — side panel).** Block on the left, colour swatches in a tidy panel on the right (matches the recolor screen). Replaces today's crammed bottom rows that collide with the cube (`ArabicPreviewScreen.drawSwatches`, fixed `height-84`/`height-58`). **And** that panel is the new floating/customizable panel below.

**B1 — Panel movement (DECISION: all).**
- Grab a header handle (⠿) → drag the panel anywhere.
- Magnetic snap to screen edges.
- **Collapse** toggle → shrink to just the header, click to expand.
- **Remember** position + collapsed state per screen, on disk; reopen restores it.

**B2 — Palette customization (DECISION: all).**
- **Add** a colour — type a hex, or grab one with the in-panel dropper (§B4).
- **Remove** swatches you don't use.
- **Reorder** swatches by dragging.
- **Recent colours** row, auto-filled with last picks.
- **Saved palettes** — save a named palette, reuse it on other blocks.

**B3 — Smart-colour extras (DECISION: contrast guard, harmony + tints, fast keys + favorites; NOT pull-from-image).**
- **Contrast guard** — live readout warns when letter vs background is too low-contrast to read from a distance.
- **Harmony + tints** — pick one colour → suggests matching colours (complementary / triad / analogous) **and** a lighter/darker tint-and-shade strip.
- **Fast keys + favorites** — number keys **1–9** apply swatches; a **⭐ favourites** row pinned on top; hover a swatch shows its hex; right-click edits it.
- *(Dropped: pull-colours-from-an-image-URL.)*

**B4 — In-panel eyedrop (DECISION: dropper + loupe).**
A small dropper button on the panel → click any pixel (world, or the block preview itself) → the colour drops into the panel/current slot **without leaving the screen**. Adds a **loupe**: a magnifier that follows the cursor showing the exact pixel + its hex for precise picking. (Reject: the bare "simple dropper"; defer the bigger "full kit" target-toggle/sample-from-block.)

---

### C. `/cb livecolor` (`RecolorSliderScreen`)

**C1 — No-arg opens a block picker (DECISION: yes).**
`ColorPickBlockMenu` already exists and already routes the `"livecolor"` action; `ImageToolCommands.livecolor` just lacks a no-arg branch. Add `/cb livecolor` (no id) → open `COLOR_PICK` with action `livecolor` → pick block → opens the recolor screen.

**C2 — Revamp the controls (DECISION: yes).**
Today's sliders are plain grey bars (`RecolorSliderScreen.Slider.render`). Rebuild as real colour tracks: **hue = rainbow spectrum**, sat/lightness = live gradients, bigger knobs, value chips. Reuse the floating panel (§B) for the slider/tool dock.

**C3 — More tune tools (DECISION: all four).**
- **Temperature + contrast** sliders (on top of hue/sat/light).
- **One-tap filters** — grayscale, sepia, invert, posterize buttons.
- **Brightness curve** — lift shadows / lower highlights separately (not just one lightness slider).
- **Harmony shift** — rotate all colours together toward a target hue while keeping their relationships.
  All applied per-cell live (same `ColorMath` rail the server bakes with); server still bakes the real texture on Apply.

---

### D. `/cb eyedrop` (`EyedropScreen`)

**D1 — Explain it (DECISION: both).**
Add a **first-time popup** on first open ("Click any pixel to grab its colour", never shows again after dismiss) **and** a **permanent short hint line** every time.

**D2 — Hide the top panel (DECISION: yes).**
Add a toggle that hides the title bar / crosshair UI so the whole screen is sample-able (the bar currently covers pixels you may want to pick). The §B4 dropper + loupe work also lands here.

---

### E. `/cb edithud` (`HudEditorScreen`)

> Dev verdict: "dark, disappointing, confusing." It already has presets / inspector / snap / hover
> sounds under the hood — the problem is presentation + discoverability. **DECISION: all four below,
> plus a specific layout bug.**

- **E1 — Restyle** brighter: nicer panel, colours, spacing, buttons so it feels good (not grim/vanilla).
- **E2 — Guidance**: an intro + empty-state — what a "brick" is, what to do first, labels on everything.
- **E3 — Simplify**: keep the main view clean; tuck advanced controls (hover sounds, snap, presets) behind one **"Advanced"** fold.
- **E4 — Drag/snap feel**: clearer preview of where a brick lands.
- **E5 — Centered-name bug (dev-found):** when a display-name brick is centered, a **long name drifts right** instead of staying centered. Cause: the brick is positioned from a left-edge anchor, so variable-width text grows rightward. Fix: **center-anchor** variable-width text bricks so they grow symmetrically about their centre. (`HudAnchors` / `HudRenderer.brickBounds` + the CENTER anchor path.)

---

### F. `/cb shapeeditor` (`ShapeEditorScreen`) — registration bug + entry rework

**F1 — "Not a registered command" (CONFIRMED BUG).**
`shapeeditor` is registered **twice on the same root**:
- `command/handlers/ChestGuiCommands.java:52` → old **chest-menu** shape picker (`Dest.SHAPE_EDITOR`, Group 08).
- `command/handlers/ShapeCommands.java:73` → new **3D client screen** (`GuiMode.SHAPE_EDITOR`, Group 27).
Two literals of the same name collide (order-dependent, unpredictable), and **neither has a no-arg `.executes()`**, so bare `/cb shapeeditor` reads as "unknown/incomplete command." That's the dev's exact symptom.

**F2 — Entry rework (DECISION).**
- **Remove** the old chest-menu `shapeeditor` registration in `ChestGuiCommands` (the Group 08 `Dest.SHAPE_EDITOR` chest route). The **only** `/cb shapeeditor` is the new 3D client screen.
- `/cb shapeeditor` **(no id)** → open the **chest block-picker** (the `ColorPickBlockMenu` / list-GUI pattern, new `"shapeeditor"` action) → player clicks a block → opens the cool **3D `ShapeEditorScreen`**.
- `/cb shapeeditor <id>` → opens the 3D `ShapeEditorScreen` directly (current `ShapeCommands` path).
- Add a `"shapeeditor"` case to `ColorPickBlockMenu` (route + title + verb), or a small shape-specific picker if cleaner.

> **G27.8.F revamp (locked 2026-06-18):** the named-shape picker (10 `BlockShapes`) gets the masterpiece
> look — big stage cube + live **auto-spinning** mini-3D on every shape chip (grouped by family) + a
> locked **"Custom — soon"** teaser chip. The full freeform custom-AABB **carve** editor stays a separate
> future session (new payload + server; see `project_g27_shape_editor_partial`) and is marked **⏳ "soon"**
> in the testing guide. Full revamp spec: **§G27.8** below.

---

## G27.8 — Refined "Masterpiece" Revamp Pass (design locked 2026-06-18)

> **Status:** design locked with the dev on 2026-06-18 after an **interactive, in-game-accurate mockup**
> of the new Live Recolour look was approved. **Nothing here is built yet.** This is the authority for the
> look-and-feel overhaul that lands on top of G27.7. Build the shared kit first, then screen by screen,
> build-green at each step, ONE in-game test per screen (CLAUDE.md §2/§4).
>
> **Visual language:** refined **gold-on-black** — keep the gold identity, add subtle header gradients,
> faux-rounded corners, soft drop shadows, hover glow, consistent spacing. **Motion:** subtle & smooth
> (quick fade/slide on popups, gentle hover lift, smooth knobs); a **Reduced-motion** toggle turns it off.
>
> **How we got here:** the dev reviewed the G27.7 screens and called the bottom button strips + scattered
> controls "old looking / unprofessional," the `[?]` overlay let the screen behind show through, the dim
> slider got in the way, the Arabic colour panel was crammed, the recolour `Temperature` label overlapped
> `TONE TOOLS`, per-brick HUD editing was a scattered popup, and the shape editor was "still partial."

### §G27.8.0 — confirmed root-cause fixes (carry into the revamp)
- **`[?]` shows the screen behind it.** `CbHelpOverlay` swallows clicks but draws no scrim;
  `HudEditorOverlays.drawHelp` closes on *any* click + no scrim. → ONE shared overlay with a full-screen
  dim **scrim** behind it + a red **[X]** (Esc also closes). Used by ALL screens (HUD help folded in).
- **`Temperature` overlaps `TONE TOOLS`.** `RecolorSliderScreen.layoutSliders` puts the tone header at
  `light.y+26` and `RecolorToneTools.layout` puts the first tone-slider label ~1px below it → they
  collide. → fix tone-group spacing; **audit every screen for label collisions**.
- **Collapse target is 9×9 px** (`CbColorPanel` collapse box) → enlarge the hit area + clearer affordance.

### §G27.8.A — Universal move + shared kit
- **Universal move = Shift+Left-drag** on the cube, panels, action bar, and HUD bricks. Added to every
  screen's hint line + the help overlay. (Cube: plain drag rotates, **Shift+drag moves**.)
- **Per-screen layout memory** — where the player drags the cube / panels / bar is remembered **per
  screen**, not globally.
- **Roll `CbActionBar` onto every screen** (Recolor / Shape / HUD still use vanilla strips). Bar is
  **movable + dockable** (Shift+drag grip, snap bottom/left/right, remembered per screen).
- **Cube stage:** soft contact shadow + a faint platform/grid under the cube (premium depth).
- **Quick view-angle buttons** on cube screens: **Front / Iso / Top**.
- **Accent:** **gold only** marks selected/active (no secondary colour).
- **Sounds (Royal Directive §2):** blend all three palettes by interaction — subtle hover **tick**,
  button **click**, slider detents, **amethyst chime** on Apply/Save, **note-block** tone on toggles.
  Master on/off + volume in Settings.
- **Primary stays open** after Apply/Save/Create (green flash, keep iterating; close via Cancel/Esc/Back).
  Toggleable in Settings.
- **First-run hints:** subtle one-time coachmarks pointing at Settings, the move-grip, the mini-toolbar,
  etc. (like the eyedrop intro), persisted-once.
- **Failure UX:** on any fetch / GIF / connection-reset failure → drop to a neutral **grey fallback**
  preview **and** offer a **Retry** (never a crash / purple block).

### §G27.8.B — ⚙ Settings menu (new, global, on every title bar)
Replaces the cramped dim slider with a gear → **Settings** popup (one shared, global state):
- Global **dim** (Off / Dim / Dark presets + slider) — already a global value.
- **Hide world** (solid dark backdrop instead of the live world).
- **UI scale** (all CB panels / cubes / text).
- **Reduced motion** (kill the smooth animations).
- **Master sound** on/off + **volume**.
- **Auto-spin default** + spin speed.
- **Hint lines** on/off · **Tooltips** on/off.
- **Confirm-before-discard** on/off.
- **Snap strength** (HUD magnetic snap).
- **Copy format** (hex / share-code).
- **Compare tools** on/off (Live Recolour split + peek).
- **Reset panel positions** · **Reset settings to defaults**.

### §G27.8.C — `/cb livecolor` (`RecolorSliderScreen`)
- New look + `CbActionBar` (drop the vanilla strip); cube **centered** on the stage shadow/platform.
- HSL + TONE in tidy **grouped, collapsible** sections — **fixes the Temperature / TONE-TOOLS overlap**.
- **Compare:** split cube (orig | new) toggle **AND** hold-to-peek original (both, toggleable in Settings).
- **GIF blocks animate** in the preview with a **play/pause** (freeze a frame to recolour precisely).

### §G27.8.D — `/cb arabic` (`ArabicPreviewScreen`)
- Cube **centered** (not `panel.x()/2`); panel docks right by default; **both cube and panel freely
  movable** (Shift+drag), positions per-screen.
- **Colour panel widened + regrouped**, keep **ALL** features (palette, recents, favourites, harmony,
  contrast guard, saved palettes, hex, eyedropper) in **collapsible sections** — no cram.
- Crammed `+hex / drop / fav / save / load` row → **icon + tiny label** buttons.
- **Palette grab:** extract a palette from an **image URL** or from the **block's own texture** into the
  swatches.

### §G27.8.E — `/cb edithud` (`HudEditorScreen`)
- Replace the stacked `HudBrickInspector` popup with a **floating mini-toolbar** on the selected brick
  (in the live preview): **−/+ size · colour · ⚙ more · ✕ delete** — **plus corner-drag to resize** the
  brick directly. Keep snap/nudge; deep options live behind the ⚙ "more" card.

### §G27.8.F — `/cb shapeeditor` (`ShapeEditorScreen`) — revamp now, carve "soon"
- Revamp the **current named-shape picker** into a masterpiece: big centered stage cube, **live mini-3D
  preview on every shape chip, all auto-spinning**, grouped by family, new look + `CbActionBar`.
- Add a **locked "Custom — soon" chip** teasing freeform carving (visible, disabled).
- **Freeform custom-AABB carving (carve pixels / edit boxes) stays deferred** to its own future session
  (new payload + server) — **marked ⏳ "soon" in the testing guide**. See `project_g27_shape_editor_partial`.

### §G27.8.G — Presets & sharing (across screens)
- Unified preset system: save named looks/layouts, browse a **gallery of live mini-3D thumbnails**,
  **share codes** (copy/paste to a friend). **Search everywhere** (shape picker, preset gallery, block lists).

### §G27.8.H — Block extras
- **Glow** (fullbright) toggle a block can carry, previewed live on the cube.
- **Arabic / RTL UI** option in Settings (flip labels to Arabic + right-to-left).

### Out of scope this pass
- Server chest-GUI menus keep their current styling (revisit later).
- Freeform custom-AABB shape **carving** (separate session — new backend).
- Block FX beyond glow (glint / pulse) — later.

---

## Cross-references

- Template: `src/main/java/com/customblocks/client/gui/CbScreenTemplate.java`
- Tests: `docs/testing/GROUP_27_TESTING_GUIDE.md`
- `/cb edithud` → opens `HudEditorScreen` (G27.4) — command already exists; G27.4 is a full rebuild into the Lego HUD builder (see §G27.4), not just wiring
- Shape panel in G27.6 reuses all design from G27.5 (same AABB editor, same wireframe, embedded in sidebar)
- Color panel in G27.6 inspired by `ArabicPreviewScreen` color studio + `ColorStudioMenu` palette

---

## §G27.6.X — BlockCreationStudioScreen Extended Design (locked 2026-06-18)

> This section extends the original §G27.6 spec with features finalized in the 2026-06-18 design +
> mockup session. The original §G27.6 remains authoritative for the core pipeline (entry, Identity,
> Texture-URL, Shape, Attributes, Organize, session memory, validation, Draft/Create). This section
> adds everything beyond that. Interactive mockup: `docs/mockups/cb_create_studio.html`.

### §G27.6.X.A — Sidebar layout & fold hierarchy

**Quick fold (always visible):**
- `[⟲ Clear]` `[📚 Library]` `[🧱 Material]` quick-start cluster at top.
- Identity row (🏷) + Texture row (🖼) — always expanded.

**Advanced fold (starts CLOSED on first open, remembers state):**
- Single `▾ Advanced — 6 sections` toggle row.
- Inside: Shape (📐), Attributes + Sound (⚙), FX (✨), Behavior (🎮), Lore / Attribution (📖),
  Organize (🗂).

**Section navigation:**
- Clicking any section row navigates into it.
- Sidebar animates from 330px → 520px width when inside a section; returns to 330px on Back.
- Breadcrumb (under title): `‹ Main › Section` (or `‹ Main › Texture › Color`).

**Pins (📌):**
- Each section row has a 📌 pin icon.
- Pinned section: protected from Template / Blueprint / Paste overwrite AND locked from Surprise reroll.
- State persisted per screen in `CbScreenPrefs`.

**Readiness meter (bottom of sidebar, always visible):**
- % progress bar (gold → green at 100%).
- Tick/cross items for each required field: Identity ✓/✗, Texture ✓/✗, Name ✓/✗.
- Clicking a ✗ item navigates directly to that section panel.

### §G27.6.X.B — Identity panel (extended)

Beyond original §G27.6 §③:
- Block ID field shows `cb:` namespace prefix inline.
- Live in-game tooltip preview card updates as name/ID is typed (shows formatted name, `cb:id`, and "Custom block · CustomBlocks-B" footer).
- `[✨ Auto-name]` button (AI-powered name suggestion from texture).

### §G27.6.X.C — Texture panel (redesigned)

Replaces the original §G27.6 §④-§⑥ texture layout:

**Faces scope toggle:**
- `[All faces | Per-face]` — Per-face mode: cube gains clickable faces. Click a face → it highlights gold → any Source or Tool paints only that face. Can paint individual faces from different sources.

**Source group:**
- `[URL]` — image/GIF URL → Load button.
- `[Eyedrop]` — opens EyedropScreen, returns color to studio.
- `[AI ✨]` — style picker (Pixel / Realistic / Cartoon / Arabic ۞) + text prompt + Generate / ⨯4 variations / Edit existing with prompt. ⏳ Generation stub in initial build; stubs toast "coming soon" without blocking.

**Tools group:**
- `[Color]` — 12-swatch palette (right-click ★), HSL sliders, hex input, Eyedrop/pull-palette button.
- `[Gradient]` — two color stops, Linear/Radial/+stop.
- `[Pattern]` — Checker/Bricks/Stripes/Noise chips + Scale slider.
- `[Animate]` — Frames / Scroll mode; Speed slider. ⏳ .mcmeta generation deferred; stub UI present.

**CTM toggle:** Connected textures on/off. ⏳ Resource-pack support deferred; stub toggle present.

### §G27.6.X.D — FX panel (new section, separate from Attributes)

Stackable — any combination can be active simultaneously. Each is a real boolean toggle (rendered as a pill switch in the UI):

| Effect | Behaviour |
|---|---|
| Emissive | Fullbright — renders at max light level, no shadows cast |
| Pulse | Brightness slowly breathes in/out (like beacon) |
| Glint shimmer | Enchantment-style rainbow shimmer overlay |
| Color-cycle | Tint shifts through hue wheel over time |
| Animated tint / pulse-flow | Color animates from texture palette without per-frame work |

FX data stored on `SlotData` (new boolean flags, migration via `SlotManager.loadAll` fallback to false).

### §G27.6.X.E — Behavior panel (new section)

| Field | Values |
|---|---|
| Gravity | Off / On (falls when unsupported, like sand) |
| Bounce | Off / On (entities bounce off top face) |
| Slippery | Off / On (ice-like friction) |
| Step-on effect | None / Damage / Heal / Speed / Particles |

Stored on `SlotData` (new fields, defaulting to Off/None). Behavior test in center stage Test tab validates these in-preview.

### §G27.6.X.F — Lore / Attribution panel

All fields optional, stored on `SlotData`:
- **Author / credit** — auto-filled from player name on first Create; editable; shown on Remix.
- **Lore tooltip** — multi-line flavor text shown in Minecraft item tooltip.
- **Source URL** — link to original texture source.
- **Version / changelog** — e.g. "v1 · first release".

### §G27.6.X.G — Center stage: 3 tabs

**View tab (default):**
- 3D spinning cube with shadow platform + contact grid (§G27.8.A cube stage).
- View-angle strip (vertical icon bar, right edge): Iso / Front / Top + divider + Backdrop / In-world / GIF / Wipe.
- Backdrop cycles: grass / cave / snow / white / black.
- Badge overlay: `§8 No texture` / `§a ready` / `§e <shape>`.
- GIF playback: play/pause + frame scrub (stub in initial build — shows play icon, toasts "GIF playback coming").
- Before/after wipe: drag a split handle to compare original vs current (stub in initial build).

**Test tab:**
- Break/mine test: cube animates a mining crack sequence, plays hardness sound, shows hardness feedback.
- Glow light-spill test: world dims to near-black; block's glow casts a colored halo showing light falloff.
- Day↔night cycle: background toggles between full brightness and night to test emissive.

**Variants tab:**
- Contact-sheet grid of color/shape variations on the current block design.
- Tick individual variant cells to select.
- `[+ Create ticked as a set]` batch-creates all ticked variants as a matched family of blocks (auto-named with suffix).

### §G27.6.X.H — CbActionBar integration

Reuses `CbActionBar` from §G27.8.A:
- Grip (⠿) → Shift+drag to dock bottom / left / right.
- Collapse to sliver (▾/▸).
- Contents: `[Undo] [Redo] [🏁 Checkpoints] ··· [Draft] [Create & Publish] ··· [Share] [Cancel]`.
- Dock position + collapsed state persisted in `CbScreenPrefs`.

### §G27.6.X.I — Overlay catalogue

| Trigger | Overlay | Content |
|---|---|---|
| `[📚 Library]` quick btn | Library | 3 tabs: Templates (load existing block, ID cleared) · Blueprints (saved studio configs) · Import (share code or .json). Search. Gallery of mini-3D thumbnail cards. |
| `[🧱 Material]` quick btn | Material macros | 4 cards: Metal / Glass-Crystal / Neon-Energy / Organic-Stone. One click seeds color + sound + hardness + FX. |
| `[✦ Surprise]` title bar | Surprise | Theme picker + 🎲 Generate. Rerolls all unpinned fields. Pinned sections are immune. |
| `[✨ AI Design]` title bar | AI Design | Text prompt → fills texture + shape + glow + sound + name. `[🖼 Match screenshot]` button. ⏳ Stub in initial build. |
| `[🏁 Checkpoints]` bar | Checkpoints | List of named in-session snapshots. `[+ Save checkpoint]` + restore buttons. Survive within the session, lost on close (not persisted across relog). |
| `[Share]` bar | Share | `[🔡 Share code]` · `[💾 Export to file]` · `[☁ Publish to cloud]` (CloudVault stub) · `[📦 Pack export]` · `[🔗 Promo link]` (stub). |
| `[⚙]` title bar | Settings | Reuse §G27.8.B: dim / hide-world / UI-scale / large-text / reduced-motion / sound / stay-open / confirm-discard / Arabic RTL / set-as-default / reset positions. |
| `[Ctrl+K]` or `[⌘K]` | Command palette | Fuzzy-search field. Results list: every section name, every action (Glow, Mirror X, Share, Behavior, AI Design, …). ↵ to jump. |

### §G27.6.X.J — Attributes panel (deep redesign from §G27.6 §⑧)

- **Glow slider** (0–15) with a visual bar below it that fills gold and glows (`box-shadow`) when level > 0. Color of glow tracks the block's current texture color.
- **Hardness** slider (0–10.0) with 5 material quick-chips: 🌿 Soft / 🪵 Wood / 🪨 Stone / ⚙ Iron / 💎 Hard. Clicking a chip snaps the slider to that material's value.
- **Sound set** — 6-cell icon grid: 🪵 Wood / 🪨 Stone / 🪟 Glass / 🧶 Wool / 🏖 Sand / 🔮 Amethyst. Clicking previews the sound. Separate from the text dropdown in original spec.
- Collision (Solid / Passable) + Transparent (Opaque / On) — two-column layout.

### §G27.6.X.K — Build prerequisites and split order

**Must be confirmed in-game before any G27.6 code is written:**
- G27.1 ArabicPreviewScreen ✅ (pending confirmation)
- G27.2 RecolorSliderScreen ✅ (pending confirmation)
- G27.3 EyedropScreen ✅ (pending confirmation)
- G27.4 HudEditorScreen ✅ (pending confirmation)
- G27.5 ShapeEditorScreen 🟡 (partial, pending confirmation)
- G27.7 corrections batch ✅ (pending confirmation)

**Suggested build order (within G27.6, once prerequisites confirmed):**
1. Entry point: register no-arg `/cb create` → open `BlockCreationStudioScreen`.
2. Screen scaffold: title bar / crumb / hint lines / sidebar container (330px/520px) / CbActionBar.
3. Sidebar Main panel: Quick cluster + Quick fold (Identity row + Texture row) + Advanced fold header.
4. Identity panel: ID field + validation + Auto-ID + name + char count + tooltip preview card.
5. Texture panel: Source + Tools split + Per-face toggle + Color/Gradient/Pattern tool tabs.
6. Shape panel: embed G27.5 named-shape picker (re-render in sidebar context).
7. Attributes panel: glow bar + hardness chips + sound icon grid + collision/transparent.
8. FX panel: 5 boolean toggles with descriptions.
9. Behavior panel: 3 toggles + step-on chip picker.
10. Lore panel + Organize panel.
11. Readiness meter + Pins system.
12. Center stage: View tab (cube + vstrip icons + backdrop swap).
13. Center stage: Test tab (mine + glow + day/night tests).
14. Center stage: Variants tab (contact sheet + Create as set).
15. Library overlay + Material macros overlay.
16. Surprise overlay + Checkpoints overlay.
17. Share overlay + Command palette (Ctrl+K).
18. Validation (red flash on missing ID, sidebar auto-navigate).
19. Draft / Create & Publish pipeline + session memory.
20. Settings overlay (reuse §G27.8.B if already built by then; else stub).
21. Full testing guide §6 run.

**File size discipline:** `BlockCreationStudioScreen.java` will almost certainly need to be split into
at minimum `BlockCreationStudioScreen` (routing + state) + `StudioSidebar` (panel logic) +
`StudioStage` (cube + tabs). Plan the split before writing line 1.

---

## §G27.9 — Studio Edit Mode (load + edit an existing block) (design locked 2026-06-19)

> **Status:** design locked with the dev on 2026-06-19. **Nothing here is built yet.** Build in small
> slices, dev tests each slice before the next (CLAUDE.md §2, §4). This section is the spec; per-slice
> status lives in the testing guide, not here.
>
> **What it adds:** today the studio (`/cb create` → `BlockCreationStudioScreen`) is **create-only** —
> it always calls `SlotManager.create` and makes a NEW block. Edit Mode lets the player **pick an
> existing block and edit all its settings in place** (and optionally save the result as a copy). This
> is **distinct from** the §G27.6.X.I *Library / Templates* overlay, which loads a block but **clears
> the ID** (a clone). Edit Mode keeps the same block and writes back to it.

### §G27.9.0 — Shared foundation: "load an existing block into the studio"

Both Edit Mode and Paint-on-existing (§G27.10) need the studio to be **pre-filled from a real block**.
Build this once:
- **S2C settings sync** — a new payload that sends a block's full `SlotData` (id, name, glow, hardness,
  sound, collision, category, shape, anim) **plus** its current texture bytes to the client, so every
  tab opens pre-filled and the 3D preview shows the real block. (Today the studio only ever starts blank;
  no block→studio sync exists.)
- **Edit-mode `StudioState`** — the existing `StudioState` gains an `editingIndex` / `editingId` (null =
  creating new). The screen title shows `§6§lEditing §f<id>` instead of "new block".

### §G27.9.A — Entry (DECISION: no `/cb edit` command; in-studio landing)

- **There is NO `/cb edit` command.** Editing is reached **inside the studio**.
- **`/cb create`** opens the studio on a new **Landing chooser** (§G27.9.B).
- Pick **Edit existing** → shared block picker (§G27.9.C) → studio opens in Edit Mode on that block.

### §G27.9.B — Landing chooser (new studio front door)

`/cb create` lands here first (DECISION: chooser, not straight-to-create — clearer for a non-coder).
Tiles (DECISION: all of the below):
- **New block** — blank studio, create flow (today's behaviour).
- **Edit existing** — picker → Edit Mode.
- **Paint** — jumps to the Paint tab landing (§G27.10).
- **Continue last session** — reopen the last studio state (session memory already in §G27.6 spec).
- **From template / blueprint** — load a saved blueprint/template (folds in the §G27.6.X.I *Library*
  overlay; this is the clone path — ID cleared).
- **Duplicate a block** — picker → copy with a new id to tweak (the `dupe` rail), original untouched.
- **Recently edited** — quick row of the last few blocks created/edited for fast re-access.

> `/cb paint` skips the chooser and opens straight on the Paint tab (§G27.10).
> **Reconcile at build time:** the chooser is the new front door; the §G27.6.X.A `[⟲ Clear] [📚 Library]`
> quick cluster and §G27.6.X.I Library overlay are reached **from** it rather than duplicated.

### §G27.9.C — Shared block picker (Edit / Paint-existing / Duplicate)

A chest-GUI picker (extend the existing `ColorPickBlockMenu` / list-GUI pattern, or a dedicated picker).
DECISION: all of:
- **Texture thumbnails** — each block's own texture as its icon.
- **Search** box.
- **Filter by category** (categories already on `SlotData`).
- **Sort** — newest / recently edited / A–Z.
- **Favorites pinned** — starred blocks (`FavoritesManager`) float to the top.
- **3D hover preview** — hovering a block shows a small spinning 3D preview (flashiest; build last).

### §G27.9.D — Editable fields & save

- **Pre-fills every tab** from the block: Identity, Texture (shown as current), Shape, Attributes
  (glow / hardness / sound / collision / transparent), Category. (FX / Behavior / Lore become editable
  only once those backends exist — see §G27.6.X.D–F.)
- **ID re-ID (DECISION: editable field + live check).** The Identity ID field is editable with a live
  **available / taken** check. On save, if the ID changed → **in-place re-ID** through the existing
  re-ID rail (`ReIdCommands` / `BulkReidCommands`). Placed blocks survive (they're slot-based).
- **Save options (DECISION: both).**
  - `[Save changes]` — overwrite the same block in place (setters below).
  - `[Save as copy]` — prompts a **new id + name**, duplicates the **full block including the painted
    texture**, leaves the original untouched (the `dupe` rail + edits).
- **Draft (DECISION: keep).** `[Save as draft]` stays alongside (uses `DraftManager`); applies to new
  **and** edited blocks.
- **Locked blocks (DECISION: refuse).** Editing a `/cb lock`ed block is blocked with "unlock first",
  matching `rename` / `retexture` / `delete`. (`LockManager.isLocked`.)
- **Animated blocks (DECISION: settings only).** You may edit an animated block's name / shape /
  attributes / category; the pixel pen is **disabled** for it (see §G27.10) with a clear message.

### §G27.9.E — Server plumbing (new work)

- **S2C** block-settings + texture sync (§G27.9.0) to pre-fill the studio.
- **C2S** "apply edits to an existing block" — distinct from the create-only `CreateStudioPayload`. Routes
  through the **existing setters** (`SlotManager.setShape/setGlow/setHardness/setSoundType/setNoCollision/
  setCategory`, `rename`, re-ID rail) — those already exist; the gap is the payload + a server handler
  (extend `CreationStudioBridge` or a new `StudioEditBridge`). Server stays authoritative; one
  `ResourcePackServer.updatePack()` after the batch.
- **Undo:** record edits via `UndoManager.recordModify` (snapshot before/after), so `/cb undo` reverts.

### §G27.9.F — New files & build order (small slices, ONE in-game test each)

| File | New/changed | Purpose |
|---|---|---|
| `client/gui/studio/StudioState.java` | change | add `editingIndex`/`editingId` + per-tab pre-fill setters |
| `client/gui/StudioLandingPanel.java` | new | the Landing chooser tiles |
| `gui/chest/BlockPickerMenu.java` | new (or extend `ColorPickBlockMenu`) | thumbnail/search/sort/filter/fav picker |
| `network/payloads/BlockSettingsSyncPayload.java` | new | S2C full settings + texture → pre-fill |
| `network/payloads/StudioEditPayload.java` | new | C2S apply-edits-to-existing |
| `command/handlers/StudioEditBridge.java` | new | server handler → setters + re-ID + dupe + undo |

Build order: **1)** load-existing foundation (sync + pre-fill + "Editing <id>" + Landing chooser +
basic picker) → **2)** `[Save changes]` in-place + locked/animated guards → **3)** re-ID + `[Save as
copy]` + Draft → **4)** picker polish (thumbnails / search / filter / sort / favorites / 3D hover).

---

## §G27.10 — Studio Paint (in-studio pixel editor) (design locked 2026-06-19)

> **Status:** design locked with the dev on 2026-06-19. **Nothing here is built yet.** Build in small
> slices, dev tests each (CLAUDE.md §2, §4). Spec only; status → testing guide.
>
> **What it adds:** a real **freehand pixel editor** for block textures, usable on a **new** block or an
> **existing** one. This is **distinct from** the §G27.6.X.C *Color / Gradient / Pattern* tools, which are
> **procedural fills**; Paint is hand-drawing with a pen. It is the "draw your own texture" tool the
> original spec hinted at but never specified. Depends on the §G27.9.0 load-existing foundation.

### §G27.10.A — Entry (DECISION)

- **`/cb paint`** opens the studio on its own **top-level Paint tab** (peer to Identity / Texture / Shape /
  Attributes / Category), which lands on two choices: **[Paint existing block]** · **[Paint new block]**.
- **Paint existing** → shared block picker (§G27.9.C) → loads the block's texture; **full studio**
  available (it enters Edit Mode with the Paint tab open).
- **Paint new** → blank canvas for a brand-new block.
- The Paint tab is also reachable from the Landing chooser **Paint** tile.

### §G27.10.B — Canvas

- **Form (DECISION):** opening Paint launches a **full-screen canvas overlay** (canvas + tool palette +
  color picker, with a small live block preview in a corner).
- **Resolution (DECISION):** new block = **32×32**, upscaled on save through the normal block-png
  pipeline. Existing block = the block's **real stored resolution** (no quality loss when editing
  existing art).
- **Performance (DECISION: efficient build + soft cap/warn).** The canvas MUST be a **baked dynamic
  texture drawn as one quad**, never per-pixel `ctx.fill` (the §A1 lag lesson — a 256/512px grid of fills
  would be catastrophic). For very high-res blocks, **warn or soft-cap** the paint canvas to a sane size.

### §G27.10.C — Tools (DECISION: all)

Pen · Eraser · Bucket fill · Eyedropper · **Line** (drag; snap to 45° on hold) · **Rectangle / box**
(outline or filled) · **Mirror / symmetry** (modes: **Horizontal**, **Vertical**, **4-way**, **Diagonal**)
· **Brush size** (1 / 2 / 3 px) · **Undo / Redo** (per-stroke history, `Ctrl+Z` / `Ctrl+Y`).

### §G27.10.D — Color & transparency

- **Color source (DECISION: full picker).** Reuse the existing `HudColorPicker` — SV square + hue slider +
  hex input + recent colours + preset palette.
- **Transparency (DECISION: yes).** Alpha-aware canvas: the **eraser clears pixels to transparent** (and
  you can paint semi-transparent), so cutout / glass-style blocks can be hand-made. A **checkerboard**
  shows transparency on the canvas.

### §G27.10.E — Canvas navigation (DECISION: all)

**Zoom** (scroll / +/−) · **Pan** (drag — middle-mouse / space) · **Pixel-grid toggle** (faint gridlines)
· **Fit-to-screen** button (reset zoom/pan).

### §G27.10.F — Reference underlay / trace (DECISION: yes)

Show a chosen image faintly under the canvas (onion-skin) with **adjustable opacity** to trace by hand.
Sources (DECISION: all): **a URL**, **another block's texture**, or the **block's own current texture**
(handy for touch-ups / redraws).

### §G27.10.G — Faces, preview & conflicts

- **Per-face (DECISION: toggle).** Single texture (all faces) by default; a toggle switches to **per-face**
  painting (pick a face, paint it). Reuses the §G27.6.X.C per-face scope and `TextureStore.saveFace`.
- **Live preview (DECISION: debounced).** The 3D cube re-textures as you paint, throttled to stay smooth.
- **Texture-source conflict (DECISION: warn).** One working texture. If you paint and then load a URL /
  pick a color in the Texture tab, it **warns before replacing** the paint.

### §G27.10.H — Save / server plumbing (new rail)

- Painting commits to the studio's working texture. On Create / Save → a **new C2S pixel-upload payload**
  carries the painted **PNG bytes** (per-face: one PNG per painted face) → server `TextureStore.save` /
  `saveFace(index, png)` + `ResourcePackServer.updatePack()`.
- **This rail does not exist today** — textures only ever come from a URL download or a solid-colour bake,
  and `CreateStudioPayload` carries no pixel data. This is the single biggest new piece.

### §G27.10.I — New files & build order (small slices, ONE in-game test each)

| File | New/changed | Purpose |
|---|---|---|
| `client/gui/paint/PaintOverlayScreen.java` | new | full-screen canvas overlay (routing + layout) |
| `client/gui/paint/PaintCanvas.java` | new | baked-texture canvas: draw, zoom/pan/grid, alpha |
| `client/gui/paint/PaintTools.java` | new | pen/eraser/fill/eyedrop/line/rect/symmetry/brush |
| `client/gui/paint/PaintHistory.java` | new | per-stroke undo/redo |
| `network/payloads/TexturePixelPayload.java` | new | C2S painted PNG bytes → `TextureStore.save`/`saveFace` |

Build order: **5)** canvas core (overlay + baked canvas + pen/eraser + color picker + pixel-upload
payload, **new block first**) → **6)** fill / eyedrop / line / rect / brush size / undo-redo → **7)**
symmetry modes → **8)** transparency + checkerboard → **9)** zoom / pan / grid / fit → **10)** reference
underlay → **11)** per-face paint → **12)** paint-on-existing (reuse §G27.9.0) + live debounced preview +
source-conflict warn. *(Steps 1–4 are §G27.9; numbering continues here.)*

**File-size discipline (§9.3):** paint splits across the files above; keep each ≤500 lines. The pixel
upload + per-face logic is the riskiest part — build the new-block path end-to-end first, confirm a
hand-drawn block actually saves and renders in-game, then layer existing-block painting on top.

---

## §G27.12 — Block Studio: Unified Create + Edit Mode (`/cb editor`)

> **Status:** design locked with the dev **2026-06-20**. **Nothing built yet.** This is the authority for
> turning the G27.6 creation studio into a dual-mode **Block Studio** that both creates new blocks and
> edits existing ones, and for making `/cb editor <id>` open it — **replacing the retired chest editor**
> (`GROUP_02_CHEST_GUI.md §3` + test G02.3, marked superseded 2026-06-20).
>
> **Depends on:** G27.6 `BlockCreationStudioScreen` built first — this reuses its five panels and 3D
> preview. Build Edit mode as part of / right after G27.6. **Reuse, don't fork** (CLAUDE.md §5).

### Why this exists
The dev wants the old screen-based block editor back — but better, and unified with creation. Rather than
a separate editor screen, the existing Creation Studio gains an **Edit** mode: one screen, one set of
panels, two jobs.

### Decisions (locked 2026-06-20)

**Entry & replace**
- `/cb editor <id>` opens the **Block Studio Edit tab**, block preloaded. (Replaces the chest editor.)
- `/cb editor` (no id) → block picker first (matches the livecolor/shapeeditor no-arg pattern, §C1/§F2).
- `/cb create` (no args) → opens on the **Create tab** (unchanged from G27.6). The 1/2/3-arg `/cb create`
  CLI stays untouched.
- **Only `/cb editor` is rewired this pass;** `/cb gui block <id>` and ESC-menu edit links redirect later.
- Screen renamed **"Block Studio"** (was "Block Creation Studio") to cover both jobs.

**Tabs & state**
- Top tabs `[Create]` · `[Edit]`. Title shows mode: `§6§lBlock Studio §7— §fCreate` / `… §7— §fEdit: <id>`.
- **Two independent states.** Create tab keeps its session memory (G27.6). Edit tab always loads the
  **live** block from disk (source of truth), never the Create session cache.
- Switching tabs loses nothing. Only Esc/close (or switching the edited block) with unsaved work warns.

**Edit-tab panels** — same five as Create (Identity / Texture / Shape / Attributes / Organize),
pre-filled from the block. Primary button flips `[Create & Publish]` → `[Save Changes]`.

**Block selector (Edit tab)** — a built-in block browser/search at the top of the Edit tab. Pick any block
to load it; `/cb editor <id>` just preselects. **Reuses the Studio's existing in-screen overlay picker**
(the "Start from Template" picker, G27.6). Switching the selected block with unsaved live edits →
**"Save changes to `<id>`?"** Save / Discard / Cancel, then loads the new block.

**Live apply (Edit tab only; Create stays preview-until-Create)**
- Edits apply to the **real block in real time**, **reusing the mod's existing live pipeline — do not
  reinvent it.** Audit and mirror the live recolor path (`RecolorSliderScreen` / `/cb livecolor`,
  `ColorMath`) and the chest editor's instant property apply, **including their safeguards**.
- Cheap props (glow, hardness, sound, collision, category, display name) sync instantly.
- Texture / shape ride the **existing debounced** resource-pack-regen path (~500ms debounce +
  `ConfigSyncPayload` broadcast **after** the batch) — the known fix that prevents purple blocks / lag.
- Invalid fields (empty ID/name) **never** live-apply; the block holds its last valid state + the field
  shows the red outline.

**Snapshot / Cancel**
- Opening the Edit tab on a block **snapshots** it. Live edits are real, but **Cancel / Esc restores the
  snapshot** fully (block + placed copies + clients resync). **Save Changes** commits.
- Undo history persists per block (Group 27 standard) and, in Edit mode, reverts the live block too.

**Block ID change → reid (the one Save-only field)**
- The ID field is editable. ID can't live-reid per keystroke, so **ID change applies on Save only.**
- On Save with a changed ID: confirm **"This renames the block and updates N placed copies in the world.
  Continue?"** then run the **existing reid pipeline**. Display-name changes are cheap and live (no reid).

**Locking (reuse `LockManager`)**
- Opening the Edit tab **soft-locks** the block to **ALL edit paths** — the screen AND commands
  (`setglow`, `retexture`, etc.) — so live edits can't collide. A second editor gets "someone is editing
  this block" and opens read-only / waits.
- Lock releases on Save / Cancel / close, and **auto-expires after ~5 min idle or on disconnect/crash**
  so a block is never stuck. Admin force-unlock available.
- A block already **permanently `/cb lock`-ed** opens **read-only** with an "unlock first" notice. (The
  transient edit-session lock is separate from the permanent `/cb lock`.)

**Texture tools in Edit mode**
- Color tab (recolor / HSL / eyedrop) **modifies the block's current texture in place** (like live
  recolor). URL / AI / Drop **replace** the texture wholesale. Matches each tool's existing behavior.

### Known gap (flagged, not dropped)
- Deep-editing **animated/GIF** blocks (frame timing/order) is **retexture-only** here — full frame editing
  belongs to the missing `AnimBlockScreen` (Issue 17.16). Surfaced for a later pass.

### New / changed files (≤500-line gate; reuse G27.6 panels)
| File | New/changed | Purpose |
|---|---|---|
| `client/gui/BlockCreationStudioScreen.java` | change | Add Create/Edit tab switch + mode state (keep ≤500 — split a `StudioEditMode` helper if it grows) |
| `client/gui/studio/StudioEditController.java` | new | Edit-mode: load live block → panels, snapshot, live-apply (reuse existing pipeline), revert, save |
| `client/gui/studio/StudioBlockSelector.java` | new (or reuse template picker) | Edit-tab block browser/search |
| `command/handlers/*` (editor command) | change | `/cb editor <id>` → `OpenGuiPayload(CREATE_STUDIO, mode=EDIT, id)`; no-arg → block picker; tab-complete |
| `core/EditSessionLock.java` | new (or extend `LockManager`) | Transient edit-session soft-lock + auto-expire + force-unlock |
| existing reid pipeline | reuse | Save-time ID change |

### Success criteria (per CLAUDE.md §2 — the Golden Rule)
Nothing here is ✅ DONE until the dev confirms in-game:
- `/cb editor <id>` opens the Block Studio Edit tab, block preloaded, `[Save Changes]` shown.
- Live edits (glow/hardness/sound/collision/category/name) apply in real time; texture/shape apply via the
  debounced path with **no purple blocks**.
- Cancel/Esc restores the block exactly; Save commits.
- Changing the ID prompts the reid confirm and remaps placed copies on Save.
- The block is soft-locked while editing (a command edit to it is refused); the lock auto-releases.
- The Edit-tab selector switches blocks with a Save/Discard prompt.
- `/cb create` Create tab and the CLI are unaffected.

---

## §G27.11 — Recolour + Shape folded INTO the Studio (`/cb recolor`, `/cb shapeeditor`) (design locked 2026-06-20)

> **Status:** design locked with the dev **2026-06-20**. **Nothing built yet.** Spec only; per-slice
> status → testing guide. Build in small slices, dev tests each (CLAUDE.md §2/§4).
>
> **Section-number note:** the Block Studio Unified Create+Edit section was **renumbered
> §G27.10 → §G27.12** (2026-06-20) to clear a duplicate with **Studio Paint** (which keeps §G27.10).
> `GROUP_02_CHEST_GUI.md`'s cross-refs were updated to §G27.12 in the same pass.
>
> **Depends on:** G27.6 `BlockCreationStudioScreen` + §G27.9 Studio Edit Mode foundation (§G27.9.0
> load-existing) built first. This **reuses** the studio's panels/preview and the existing recolour
> bake rail — **reuse, don't fork** (CLAUDE.md §5).

### Why this exists
The dev wants ONE editor. Today recolour (`/cb livecolor` → `RecolorSliderScreen`) and shape
(`/cb shapeeditor` → `ShapeEditorScreen`) are **separate standalone screens**. Folding them into the
Block Studio means one screen, one consistent frame, one place to learn. The studio **already has a
Shape section** (`StudioSections.renderShape`, same 10 `BlockShapes`), so shape needs no new tab — just
re-route the command. Recolour has **no** studio home yet → it becomes a new sidebar section.

### Decisions (locked 2026-06-20)

**Rename — `livecolor` → `recolor` (everywhere; no alias)**
- `/cb livecolor` becomes **`/cb recolor`**. The old name is **removed** — no hidden alias, `/cb livecolor`
  becomes an unknown command. Only `recolor` survives.
- Every user-facing label that said "livecolor" / "Live Recolour" → **"Recolor"** (command help,
  tab-complete, chest-menu buttons, screen/section title). This **supersedes** every `livecolor` mention
  elsewhere in this doc and the GROUP docs (`§C`, `§G27.8.C`, `ColorPickBlockMenu`, `ColorsMenu`,
  `EditorMenu`).

**Shape — no new tab, just re-route (dev: "forget making a new tab, just make /cb shapeeditor open the existing shapes screen")**
- The standalone **`ShapeEditorScreen` is retired.** `/cb shapeeditor <id>` opens the **Studio in Edit
  mode** with the **existing Shape section** focused (the chip picker already in `StudioSections`).
- `/cb shapeeditor` (no id) → existing chest block-picker → pick a block → Studio Edit mode, Shape section.
- No `Shape` duplication: the studio's current Shape section IS the shape editor now. The §G27.8.F
  "masterpiece" shape revamp (mini-3D chips, families, `CbActionBar`), **if/when built, lands on the
  studio Shape section** — there is no longer a standalone screen to revamp.

**Recolour — a new sidebar section, gated on having a texture**
- Add a **Recolor** section to the studio sidebar, placed **right after Texture** (you texture, then
  recolour that texture). New entry in the `Section` enum + `NAV` array.
- **Greyed/locked until a texture exists** — same lock pattern as the **Animation** tab (`ANIM_INDEX`):
  - **Edit mode:** the block always has a texture → Recolor is **live** immediately.
  - **Create mode:** stays **grey** until the player has loaded a texture (URL + id present); once a
    texture is loaded it **unlocks** and recolours the live preview.
  - Locked click → a message ("Load a texture first — then Recolor unlocks"), mirroring the Animation gate.
- The standalone **`RecolorSliderScreen` is retired**; its content **moves into** this section.

**Recolour depth — bring everything, then improve (dev: "everything and even improve and upgrade it")**
- Full parity with the old screen: **HSL** (Hue/Sat/Light) **+ all tone tools** — temperature, contrast,
  shadow/highlight curve, and the one-tap colour filters (`RecolorToneTools`). Nothing dropped.
- **Four upgrades (dev picked all):**
  1. **Before/after split preview** — toggle the 3D cube to show original vs recoloured (split / side-by-side)
     for instant comparison.
  2. **Save/load recolour presets** — name a look ("sunset", "cold steel") and reuse it on any block later.
  3. **Eyedropper colour match** — pick a target colour from anywhere on screen; the tab solves the HSL
     shift that pushes the texture toward it. (Reuse the `EyedropScreen` rail / §B4 dropper.)
  4. **Per-zone tint (shadows / mids / highlights)** — separate tint controls for dark / mid / bright
     regions, on top of the single global shift.

**Apply timing — preview live, bake on Save (dev pick; the purple-block-safe path)**
- While dragging recolour controls the **cube updates live, client-side** (cheap per-cell
  `ColorMath.hslShiftRgb` + `RecolorToneTools.transformRgb`, exactly as `RecolorSliderScreen` does today).
- The **real server texture bake happens once, on `Save changes`** — NOT per drag. One bake through the
  existing recolour rail (the same `ColorMath` path `RecolorApplyPayload` uses). No `~500ms`-debounce
  hammering, **no purple-block flicker** (CLAUDE.md §7 pitfall).
- **Create mode:** the chosen recolour params ride along in the create payload and are baked **at publish**
  (post-create), since there is no block to bake against until then.

**Entry points & chest buttons (dev: route everything into the studio)**
- `/cb recolor <id>` → Studio **Edit mode**, Recolor section focused.
- `/cb recolor` (no id) → existing chest block-picker → pick → Studio Edit mode, Recolor section.
- Chest-menu buttons that opened livecolor/shapeeditor (`ColorsMenu`, `EditorMenu`, `ColorPickBlockMenu`)
  → relabel **"Recolor"** and route into the Studio Edit mode on the matching section.
- The no-arg **chest block-picker stays** as the selector (dev confirmed "they open chest guis"); the
  picker's click target changes from the old standalone screen to the Studio.

### Architecture notes (build-time, not built)
- **File-size gate:** `BlockCreationStudioScreen.java` is **already at the 500-line limit** (§9.3). The
  Recolor section's content MUST live in its **own helper class** — `StudioRecolorPanel` (mirroring
  `StudioAnimPanel` / `StudioCategoryPanel`), hit-tested through `StudioSections` like the other custom
  pickers. Do **not** inline it in the screen.
- **State:** `StudioState` gains recolour fields (hue/sat/light + temp/contrast/shadow/highlight/filter +
  the per-zone tints). They serialise into `toAttrs()` / `saveAttrs()` **and** the `dirty()`/`signature()`
  fingerprint, so a recolour counts as an unsaved change.
- **Server apply:** `CreationStudioBridge` (create) / `StudioEditBridge` (edit, §G27.9.E) bake the recolour
  via the **existing** `ColorMath` rail on Save — reuse, don't add a parallel path.
- **Retire + reroute:** `RecolorSliderScreen` and `ShapeEditorScreen` classes retired. `GuiMode`
  `RECOLOR_SLIDER` / `SHAPE_EDITOR` routing (`CustomBlocksClient`) changes to open `CREATE_STUDIO` in Edit
  mode on the target section (carry a section hint in the open payload). `ImageToolCommands` (livecolor→
  recolor) and `ShapeCommands` (shapeeditor) reroute to the studio; the no-arg chest-picker branches stay.

### Reconciliation with earlier sections
- **Supersedes** the standalone-screen framing of **§G27.2** (Recolor upgrade) and **§G27.5** (Shape
  editor): both screens fold into the Studio. Their visual-upgrade intents (and **§G27.8.C** recolour
  look, **§G27.8.F** shape "masterpiece") carry onto the **Recolor / Shape studio sections** instead of
  standalone screens.
- **Builds on §G27.9 / §G27.12** — Recolour and Shape are **Edit-mode** sections; they need the
  load-existing foundation (§G27.9.0) and the unified studio.

### New / changed files (≤500-line gate; reuse existing rails)
| File | New/changed | Purpose |
|---|---|---|
| `client/gui/StudioRecolorPanel.java` | new | Recolor section content: HSL + tone tools + 4 upgrades; live per-cell preview transform |
| `client/gui/BlockCreationStudioScreen.java` | change | Add `RECOLOR` to `Section`/`NAV` after Texture + the texture-gate (mirror `ANIM_INDEX`); keep ≤500 (delegate to the panel) |
| `client/gui/StudioSections.java` | change | Route the Recolor section's render + click to `StudioRecolorPanel` |
| `client/gui/studio/StudioState.java` | change | Recolour fields + serialise into `toAttrs`/`saveAttrs` + `signature()` |
| `command/handlers/ImageToolCommands.java` | change | `livecolor` → **`recolor`** (no alias); reroute `<id>` to Studio Edit-mode Recolor section |
| `command/handlers/ShapeCommands.java` | change | `/cb shapeeditor <id>` → Studio Edit-mode Shape section (retire standalone open) |
| `gui/chest/ColorsMenu.java` · `EditorMenu.java` · `ColorPickBlockMenu.java` | change | Relabel "Recolor"; route to Studio Edit mode |
| `client/CustomBlocksClient.java` · `gui/GuiMode.java` | change | `RECOLOR_SLIDER`/`SHAPE_EDITOR` open the Studio (Edit mode + section hint) instead of the retired screens |
| `client/gui/RecolorSliderScreen.java` · `client/gui/ShapeEditorScreen.java` | **retire** | Content moved into the Studio sections |
| existing recolour bake rail (`ColorMath`, `CreationStudioBridge`/`StudioEditBridge`) | reuse | Server-side bake on Save / publish |

### Success criteria (per CLAUDE.md §2 — the Golden Rule)
Nothing here is ✅ DONE until the dev confirms in-game:
- `/cb livecolor` is gone (unknown command); `/cb recolor` works; every "Recolor" label reads correctly.
- `/cb recolor <id>` opens the Studio Edit mode on the **Recolor** section; `/cb recolor` (no id) → picker → Recolor.
- The Recolor section is **greyed** in a fresh Create until a texture loads, then unlocks; in Edit it's live at once.
- Recolor has HSL + all tone tools + the 4 upgrades (split preview, presets, eyedropper match, per-zone tint).
- Dragging recolour updates the cube live; the **real texture bakes once on Save** with **no purple blocks**.
- `/cb shapeeditor <id>` opens the Studio Edit mode on the existing **Shape** section (standalone screen gone).
- Chest "Recolor" buttons open the Studio on the right section.

---

## §G27.13 — On-screen toasts replace chat feedback (design locked 2026-06-20)

> **Status:** design locked with the dev **2026-06-20**. **Nothing built yet.** Spec only; per-slice
> status → testing guide. Reuse, don't fork (CLAUDE.md §5).
>
> **Objective:** When the player clicks things **inside a CB client `Screen`**, feedback shows as an
> **on-screen toast (top-right box)** — **never a chat message**. Chat stays clean while a screen is open.

### Why this exists
Clicking inside the screens currently spams **chat**. The dev wants the feedback to pop **on the screen**,
not in chat.

### The root cause (confirmed in code)
There is already a `toast(...)` helper, but it is a **lie** — it sends to chat:

```java
// client/gui/hud/HudPresetBrowser.java
private void toast(String msg) {
    if (client != null && client.player != null)
        client.player.sendMessage(Text.literal(msg), false); // ← false = CHAT, not a toast
}
```

Every "toast" routes through `sendMessage(..., false)` (chat). Same pattern feeds the Studio's
"coming soon" stubs (§G27.6.X `[AI ✨]`, §G27.6.X GIF playback) and all the per-screen "Save … + chat
message" lines (§G27.1, §G27.2, §G27.4, §G27.6, etc.). So "the screens chat at me" is literally true.

### Decisions (locked 2026-06-20)
- **Form:** a **toast — top-right box** (dev pick). Slides in top-right, stacks, auto-fades. Not title,
  not action bar, not in-screen text.
- **Scope of messages:** **everything → toast** (dev pick) — success, info **and** errors. Nothing from a
  CB screen goes to chat while that screen is open.
- **Scope of screens:** **all** CB client screens + their popups —
  `ArabicPreviewScreen`, `RecolorSliderScreen`/Studio Recolor, `EyedropScreen`, `HudEditorScreen`
  (+ `HudPresetBrowser`, `HudColorPicker`, `HudBrickInspector`, …), `ShapeEditorScreen`/Studio Shape,
  `BlockCreationStudioScreen` (+ all `StudioSidebar*`).
- **Reuse, not rewrite:** build ONE real toast and swap the existing `toast(...)` **bodies** to call it —
  the call sites already exist, they just point at chat today.

### How (build-time, not built)
- New `client/gui/CbToast.java` — a real top-right toast. Cleanest route is a vanilla
  `net.minecraft.client.toast.Toast` added via `client.getToastManager().add(...)` (renders top-right,
  works while a screen is open, auto-expires) styled to the CB look (gold border / dark strip, §G27 frame).
  A `CbToast.show(text, kind)` static (kind = success/info/error → colour) is the single entry point.
- **Reroute every screen feedback call** to `CbToast.show(...)`:
  - `HudPresetBrowser.toast(...)` body → `CbToast.show(...)` (delete the `sendMessage` line).
  - Studio "coming soon"/GIF stubs and the Save-flash feedback → `CbToast.show(...)`.
  - Any other `client.player.sendMessage(...)` fired **from a `Screen`** → toast instead.
- **Out of scope (stays chat):** real **server** command replies (`/cb setglow`, bulk results, etc.) —
  those are typed-command feedback, not screen clicks. This section is **only** about feedback raised
  from inside an open `Screen`.

### Open edge (decide at build)
`Ctrl+C` "copy settings" currently **also prints the code string in chat** (§G27 universal shortcut) so
the dev can copy the text. It already lands on the OS clipboard, so the chat echo is redundant — fold it
into a `"Copied ✔"` toast. **Flag for the dev at build time** before removing the chat echo.

### New / changed files (≤500-line gate)
| File | New/changed | Purpose |
|---|---|---|
| `client/gui/CbToast.java` | new | Real top-right CB toast; `show(text, kind)` single entry point |
| `client/gui/hud/HudPresetBrowser.java` | change | `toast(...)` body → `CbToast.show` (drop chat send) |
| `client/gui/BlockCreationStudioScreen.java` + `StudioSidebar*` / `StudioSections` | change | "coming soon"/GIF stubs + Save-flash feedback → `CbToast.show` |
| other `client/gui/*Screen.java` raising `sendMessage` from a screen | change | Route to `CbToast.show` |

### Success criteria (per CLAUDE.md §2 — the Golden Rule)
Nothing here is ✅ DONE until the dev confirms in-game:
- Clicking buttons inside any CB screen shows a **top-right toast**, and **chat stays empty**.
- Success, info, and error feedback all appear as toasts (correct colour per kind).
- Typed server commands (`/cb setglow …`, bulk results) still reply in chat as before (unchanged).
- The Studio "coming soon" stubs and the Save flash now toast instead of chatting.

---

## §G27.14 — HUD: templates section + shape backgrounds (pill default) (design locked 2026-06-20)

> **Status:** design locked with the dev **2026-06-20**. **Nothing built yet.** Spec only; per-slice status
> → testing guide. Extends §G27.4 (Lego HUD). **Purely additive — keep every existing brick, drag, snap,
> inspector, preset and the whole HUD menu untouched** (dev: "make it still lego and dragable… but don't
> touch or ruin current hud menu"). Reuse, don't fork (CLAUDE.md §5).
>
> **Objective:** bring back the old mod's two HUD comforts, rebuilt on the brick system: **(A) background
> shapes** — Pill (default) / Glow box / Plain, like the old `style` switch; and **(B) a Templates section**
> — text lines mixing words with live `{tokens}` (`{name}`, `{id}`, `{light}`…). Each line stays its own
> draggable brick (dev: "each is a different lego", not Name+ID fused into one box).

### Why this exists
The old mod ([HudConfig.java](../../CustomBlocks/src/main/java/com/customblocks/client/HudConfig.java), drawn at
[CustomBlocksClient.java:1155](../../CustomBlocks/src/main/java/com/customblocks/client/CustomBlocksClient.java))
had one styled nameplate with 3 looks — `0 = Pill, 1 = Glow Box, 2 = Plain Text` — plus a "template text"
mode (3 lines of `{name} {id} {light}` placeholders). The -B Lego HUD (§G27.4) dropped both: every brick's
background is a flat filled rectangle ([HudRenderer.drawBackground:140](../src/main/java/com/customblocks/client/HudRenderer.java)),
and `CUSTOM_TEXT` shows literal text only — no tokens ([HudFieldType.resolve](../src/main/java/com/customblocks/client/hud/HudFieldType.java)).
Dev wants both back **as additions** to the brick system, not a replacement.

### Feature A — shape backgrounds (pill / glow box / plain), pill = default
Today the bg is one `ctx.fill` rectangle. Add a background **shape**, mirroring the existing
global-default-or-per-brick-override pattern that already governs bg colour + opacity.

**Decisions (locked 2026-06-20)**
- Shapes the user picks: **Pill** (true rounded capsule + thin accent stripe down the left) · **Glow box**
  (inset fill + accent border on all 4 sides + a brighter top glow strip) · **Plain** (no background — text +
  shadow only; this is the existing `bgOff`). A 4th **Box** = today's flat rectangle, kept so nothing existing
  changes.
- **Default = Pill** for fresh installs, the default layout, and every newly added brick.
- **Accent colour** drives the pill stripe + glow-box border. Global default = the old blue `0xFF5B8DFF`;
  optional per-brick override.
- **Upgrade over old:** the old "pill" was actually square corners — do a **real rounded** capsule.

> **OPEN — flag for dev at build:** existing saved HUDs. To honour "don't ruin", a brick loaded from an
> **old** config (no shape key) defaults to **Box** (flat — looks exactly like today); pill only auto-applies
> to fresh / newly added bricks. Offer a one-click **"apply pill to all"**. → confirm: pill on your *existing*
> bricks too, or only new ones?

**How (build-time, not built)**
- `client/hud/HudField.java` (change): add `BgShape bgShape` (PILL / GLOW_BOX / BOX / PLAIN) + optional
  `accentColor` override + JSON keys (default-safe so old configs load).
- `client/HudConfig.java` (change): global `bgShape` default (PILL) + global `accentColor` (`0x5B8DFF`).
  Mirrors existing `bgColor` / `bgOpacity`.
- `client/HudRenderer.java` → `drawBackground` (change): switch on shape — pill (rounded fill + left stripe),
  glow box (inset + border + top glow), box (today's flat fill, unchanged path), plain (nothing).
- `client/hud/HudBgShapes.java` (new, if needed): rounded-rect / pill / glow draw helpers — keeps
  `HudRenderer` under the 500-line gate.
- `client/gui/hud/HudBrickInspector.java` (change): a **shape picker** control per brick (additive; existing
  controls stay). Global default shape + accent control added wherever the box-level globals UI lives today.
- `client/HudConfigStore.java` (change): migration — missing `bgShape` → BOX (preserve look); fresh/default → PILL.

### Feature B — Templates section (live `{token}` text lines)
Old "template text" lines become **Template bricks**: one line = one brick = independently draggable.

**Decisions (locked 2026-06-20)**
- New brick type **Template** holding a `text` string with `{tokens}`. `resolve()` expands tokens from the
  same per-frame look-at context, **reusing the existing field resolvers** (no fork).
- Token set = the existing bricks: `{name} {id} {slot} {coords} {light} {distance} {facing} {category} {glow}
  {hardness} {sound} {shape} {solid}`, with literal words around them.
- A **Templates** group in the **[+ Add brick ▾]** palette adds a Template brick. The brick **inspector** gets
  a text box + **token-insert chips** (click `{name}` to insert). Existing palette / inspector untouched.
- Visibility: a line containing any **block-info** token follows the block-info rule (shows only while aiming
  a custom block); otherwise it's a world brick (resolver returns null when a needed block token can't resolve).

> **OPEN — flag at build:** Minecraft `§` colour codes inside a template — allow (on top of the brick's base
> colour) or strip? Lean: allow.

**How (build-time, not built)**
- `client/hud/HudFieldType.java` (change): add `TEMPLATE` + a small `{token}` expander (maps `{key}` → the
  matching type's `resolve`).
- `client/gui/hud/HudBrickPalette.java` (change): add the **Templates** group + Template entry.
- `client/gui/hud/HudBrickInspector.java` (change): multiline text + token chips for a Template brick.
- Persistence rides the existing `HudField.text` JSON — no new store work. **Verify** the scope=all sync
  (`network/.../HudSync*`) carries the new shape / accent / template fields.

### New / changed files (≤500-line gate)
| File | New/changed | Purpose |
|---|---|---|
| `client/hud/HudField.java` | change | `bgShape` + accent override; JSON (default-safe) |
| `client/HudConfig.java` | change | global default `bgShape` (PILL) + `accentColor` |
| `client/HudRenderer.java` | change | `drawBackground` switches shape; pill / glow / box / plain |
| `client/hud/HudBgShapes.java` | new (if needed) | rounded-rect / pill / glow helpers (keep renderer < 500) |
| `client/hud/HudFieldType.java` | change | `TEMPLATE` type + `{token}` expander (reuse resolvers) |
| `client/gui/hud/HudBrickInspector.java` | change | per-brick shape picker + template text box + token chips |
| `client/gui/hud/HudBrickPalette.java` | change | "Templates" palette group + entry |
| `client/HudConfigStore.java` | change | migration: old brick → BOX; fresh / new → PILL |
| box-level globals UI (where global bg lives) | change | global default shape + accent colour control |
| `network/…/HudSync*` | verify | new fields ride the scope=all sync |

### Build order (small slices — ONE in-game test each)
1. **Shapes data + render** — fields + JSON + migration → `drawBackground` pill/glow/box/plain → inspector
   shape picker + global default. *Test: default HUD is pill; switch a brick to glow/plain; existing layout unchanged.*
2. **Templates** — `TEMPLATE` type + token expander → palette "Templates" + inspector text/chips.
   *Test: add a `{name} [{id}]` template brick, drag it, it tracks the aimed block live.*

### Success criteria (per CLAUDE.md §2 — the Golden Rule)
Nothing here is ✅ DONE until the dev confirms in-game:
- New / default bricks show the **pill** background; bricks still drag + snap; the existing HUD menu is unchanged.
- The per-brick **shape picker** switches Pill / Glow box / Plain (+ Box) and persists across relog.
- A **Template** brick resolves `{tokens}` live and is its own draggable brick.
- **Old saved HUD configs still load** with their look preserved (no crash, no surprise restyle).

---

## §G27.15 — Animation Tab / `/cb animation` Studio — SCREEN DESIGN (owned here)

> **This is the single home for the Animation tab's SCREEN design.** Folded fully into the screens group
> 2026-06-22 (owner: "fold the screen of animation into the screens group entirely … removed from animation
> group, so everything can be synced and correct"). **`GROUP_14_ANIMATION_VIDEO.md` keeps ONLY the animation
> engine** (render paths, atlas/mcmeta, perf, video import, redstone, the `AnimData` numbers) — it no longer
> owns any screen/UI spec; it points here.
>
> **Status:** 🟡 requirements captured (2026-06-21 interview) + screen spec folded from G14 (2026-06-22).
> **NOT redesigned in detail yet, NOT built.** Owner will revamp/redesign the screen *here*, under the locked
> red+black standard (§G27.6.P P1). Build nothing until a design slice is locked + dev-confirmed, one step at
> a time (CLAUDE.md §2/§4).

### Decisions LOCKED 2026-06-21
- **One umbrella command: `/cb animation`** — opens a **SCREEN GUI hub** (not a chest).
- **`/cb anim` is renamed → `/cb animation`; the old `/cb anim` spelling is HARD-REMOVED** (not kept as an
  alias). Today `/cb anim` lives in `command/handlers/AnimCommands.java`.
- **Standalone `/cb video` + `/cb video extract` are DROPPED** (delete `VideoCommands.java` + its
  `CommandRegistrar` registration). Video import moves *into* the screen instead.
- **Typed chat editors stay** for scripting/quick edits — `/cb animation <id> ticks|fps|original|loop|smoothing|trim`
  — but the **screen is the main hub** (the GUI is the path).
- **`/cb animation` (no id)** → opens **`/cb listgui` filtered to ANIMATED blocks only**; clicking one opens
  that block's animation editor. (Add an animated-only filter to the existing `/cb listgui` chest list.)
- **The hub GROWS the existing studio Animation tab** (`StudioAnimPanel`, the live-preview Speed/Loop/Smooth/Trim
  editor), **not** a brand-new separate screen — to reuse the preview cube / block picker / nav and avoid
  duplication + the 500-line gate. **But this growth is its own future work, not Group 14.**

### Screen design — folded in from GROUP_14 (2026-06-22)
> The Animation tab IS a tab of the Studio (`BlockCreationStudioScreen` / `StudioAnimPanel`). Its design is
> owned here now. Bind everything below to the red+black standard (§G27.6.P P1) when built.

- **A dedicated Animation tab** inside `/cb create`, not buried in Texture. It **lights up once the loaded
  texture is animated**; the Texture tab still owns the source (URL / video / color).
- **FULL redesign required** (owner 2026-06-19: "unorganized trash"). Today `StudioAnimPanel` crams header +
  Speed + Loop + Smooth + Trim into ~118px with no grouping. Revamp into a clean, **grouped, animated-only**
  editor with breathing room (pairs with §G27.6.P P2 cards + P14 spacing).
- **Live-playing preview** — the preview cube cycles frames at the chosen fps / loop / smoothing.
- **Controls** (presentation here; the math/mechanics are engine, in G14):
  - **Speed** shown as both **fps AND ticks**, live cross-update; presets 5 / 10 / 20 / 30 + **match original**.
  - **Loop** buttons **Loop / Bounce / Reverse**.
  - **Smoothing** toggle (ON by default).
  - **Trim** → see the Timeline editor below (replaces the −/+ steppers per §G27.6.P P8).
- **Routing / entry:** `/cb animation <id>` opens the studio on this tab in edit mode (built); `/cb animation`
  (no id) opens an **animated-only** block list → click opens the tab for that block (see "Decisions LOCKED"
  + the `/cb listgui` filter cross-ref). Reuses the studio **edit-load path**.
- **Timeline editor** = the headline UI: filmstrip frame thumbnails, draggable playhead/scrubber (cube
  follows), in/out trim handles, per-frame duration, reorder / delete / duplicate, play/pause/step,
  right-click a frame → static. **This is the same work as §G27.6.P P8** (DaVinci-Resolve-like timeline) —
  build it as that one dedicated slice, with its own mini-spec; do not duplicate.
- **Engine boundary:** how frames actually render on the placed block, atlas/mcmeta regen, performance, video
  import, color-grade/chroma/framing *baking*, redstone behavior → all stay in `GROUP_14_ANIMATION_VIDEO.md`.
  This section only specs what the player sees + touches.

### ⚠️ Open questions (must resolve before design)
- **How the hub relates to the existing studio** (§G27.6 studio / §G27.9 Edit Mode / §G27.12 `/cb editor`):
  a focused "Animation" entry into the *same* unified studio, or a distinct screen? (Lean: same studio.)
- **Create vs edit:** can the hub *create* a new animated block (paste a URL in-screen), or edit existing
  only? (Owner deferred — decide here, not in G14.)
- **Full screen layout:** timeline, live preview, source/import panel, the editors above — to be designed.

### Import-sources wishlist (owner picked ALL — design TBD)
*Already planned/working:* paste a **URL** (gif/webp/mp4); **ffmpeg**-if-present conversion (mp4/webm/mov/avi
→ frames; the G14 §7 Phase-7 vision, clean gif/webp fallback when ffmpeg absent).
*New candidates (all on the wishlist):*
- **Local-file pick** from `config/customblocks/videos/` (screen lists `.mp4` as clickable buttons; needs a
  server→client file-list payload). *(Replaces the dropped `/cb video list`.)*
- **Drag a file onto the game window** (GLFW file-drop callback → `.gif`/`.mp4`). Best UX, low effort.
- **Paste from clipboard (Ctrl+V)** — image/gif from the system clipboard (client-side AWT).
- **In-screen GIF search (Giphy/Tenor)** — keyword → browse → click. Needs a free API key.
- **Spritesheet / frame-folder import** — a sprite grid, or a folder of numbered PNG frames → flipbook.
- **YouTube/social link + pick a time-range** — yt-dlp-if-present; heavier, external dependency.
- **AI-generated animation** — text prompt → moving texture; ties into **Group 15** AI textures.
- **Trim a time-range from any video** — in/out points so only the wanted part imports (pairs with ffmpeg).
- **Webcam / desktop capture** — record live into frames; heavy, niche.

### Flagship-feature wishlist (owner: "a big change, prove this mod strong")
*Owner-selected:*
- **Sound-synced playback ("a real TV")** — attach audio that loops in sync with the picture, distance falloff.
- **Music visualizer / audio-reactive** — block pulses / colour-shifts / swaps frames to note-block music.
- **Ken Burns motion on stills** — pan/zoom/parallax a single image so a static picture feels alive.
- **In-screen frame painting** — pixel-edit / draw on / erase individual frames. **Extends §G27.10 Studio
  Paint to per-frame editing** (reuse its tools).
- **Layered compositing + animated text** — background clip + overlay image + scrolling/typewriter text; a
  mini motion-graphics editor (signs, news tickers, branded screens).
- **Live web-feed screens** — a block shows an image URL that **auto-refreshes** (live webcam, weather radar,
  remote scoreboard PNG, "now playing"). Always current.
- **Preset & template library** — one-click starter looks (fire, water, loading spinner, neon, ticker) + save
  & share your own look as a preset. (Pairs with §G27.8.G presets + Group 20 cloud share.)
*Deprioritized (NOT selected 2026-06-21):*
- **Per-viewer targeted content** (different players see different content on the same block).

### Already covered elsewhere — do NOT re-pitch under this section
- **Video walls / multi-block screens, live-data blocks** (clock / countdown / stats / marquee), **redstone-
  reactive / interactive / triggers / sync channels / auto-emissive** → Group 14 §7 Phases 8–10.
- **Floating holograms / display-entity showcases** → Group 19.
- **Cloud share / marketplace** for animated blocks → Group 20.

### Cross-references
- **Render path** for animated blocks = off-atlas grid (G14 §7, ADR-013/ADR-014). The hub only edits the
  **plain numbers** in `AnimData`; the pack `.mcmeta`/grid sidecar is regenerated deterministically.
- **§G27.10 Studio Paint** — frame painting builds on its pixel editor.
- **§G27.9 Studio Edit Mode / §G27.12 `/cb editor`** — the hub's edit-load reuses this pick-and-edit path.
- **`/cb listgui`** (`command/handlers/ChestGuiCommands.java`) — needs an animated-only filter for the
  `/cb animation` no-id entry.

---

## G27.6.P — Studio Screen Polish Pass (owner decisions via UI, 2026-06-22)

> **Status:** 🟡 DECISIONS LOCKED, NOT BUILT. Owner went through the studio screen control-by-control
> (`BlockCreationStudioScreen` / `StudioSections` / `StudioAnimPanel`) and called it "kinda unprofessional."
> These are the chosen fixes. Nothing here is built yet. Golden rule still applies — none of this is done
> until the owner confirms in-game. Tests live in `Reports/GROUP_27_TESTING_GUIDE.md` (§ "Studio Polish Pass").

> ✅ **Brainstorm complete 2026-06-22** (6 UI question rounds). Theme scope resolved (all screens). Build order
> **deferred by owner** ("document everything in their right group and testing guide first") — see the suggested
> slicing below; pick when build starts. List is now P1–P15.

| # | Decision | Notes / current state |
|---|---|---|
| P1 | **Theme = neon-glow red + black, ALL SCREENS.** Accent `#FF1744` on near-black `#0A0A0A` for selected buttons / headers / borders / titles. | ✅ **SCOPE RESOLVED 2026-06-22:** re-theme the **whole** mod UI (Arabic, recolor, eyedrop, HUD editor, studio) red+black — the **§6 gold standard is RETIRED.** Bigger job: touches every `Screen` + `CbScreenTemplate`. **TODO when built:** update the "CB Screen Design Standard" table at the top of this doc (gold → red). |
| P2 | **Modern dark cards** — each control group in its own bordered card w/ header + divider lines. | **Partly exists:** §G27.6 ④ already specs "each panel sits on a subtle dark card" (built, parked, unconfirmed). This deepens it to per-control-group cards w/ headers in `StudioAnimPanel` etc. |
| P3 | **Hover help on EVERY control, EVERY tab** (Identity, Texture, Shape, Animation, AI, Attributes, Category). | **Currently Attributes-only** (`StudioSections.hint`). Owner: "I want all of them, not only attributes, all tabs." |
| P4 | **Action feedback in-screen, NO chat.** Save/Create → green flash + in-screen confirm. Inline warning next to empty **Block ID / Display Name** (not just on submit). | Matches design standard (line 69: "flash green + CB toast, no chat") + §G27.13 (toasts replace chat). |
| P5 | **Source link line = truncated text + [Copy] button.** Click copies to clipboard + in-screen "copied!" flash (no chat). | Link line itself was the G14 "link always visible" item (display-only now). |
| P6 | **Static background colour: swatches + ✖ clear + ADD a `#hex` / RGB input box.** | GIFs still hide the whole picker (already done — "GIFs render over black"). Static only. |
| P7 | **Preview cube: ADD drag-to-rotate, scroll-to-zoom, pause/play spin.** | Cube auto-spins only now. ❌ Owner **REJECTED** ("those are shit"): lighting/shine, drop-shadow/floor, reset-view button, day/night backdrop. Do **NOT** build those. |
| P8 | **Trim = FULL REWORK → DaVinci-Resolve-like timeline.** Filmstrip of frame thumbnails with **draggable start/end handles**. Replaces the −/+ steppers. Owner: "highly advanced + cool looking… more davinciresolve-like." | Features chosen: **draggable playhead/scrubber** (cube follows it) · **frame # on hover** + highlight · **zoom** the strip · **dim** the cut-off frames (kept visible, greyed) · **transport** (play/pause/step ◀▮▶ + jump start/end) · **In/Out markers** (I/O keys) + **loop the selected range** · **snapping** (handles+playhead snap to frame edges) · **hover magnifier** (enlarged frame popup) · **ruler = BOTH frame# + time** · **edit frames** (drag-reorder / delete / duplicate) · **onion skin** (ghost prev/next on the cube) · **J/K/L shuttle** keys · **frame markers/notes** on the ruler. ⚠️ **With the edit-frames + onion-skin + J/K/L + markers set, P8 IS the full Timeline Editor** (Bucket 2 / Phase 3) — **merge P8 with that plan** and build it as its own dedicated slice, NOT as "polish." Needs its own mini-spec. |
| P9 | **Keep the "— coming soon" placeholder** on unbuilt tabs. | Owner choice (kept clickable w/ message inside). |
| P10 | **Loading = shimmer skeleton.** While a GIF decodes, the cube + controls show a shimmering placeholder until ready (instead of "loading frames…" text). | Currently plain text in `StudioAnimPanel` / playback bar. |
| P11 | **Bottom action bar = strong primary + ghost cancel.** One big filled (red) **Create & Publish / Save** button + a quiet outlined **Cancel**; secondary buttons (Undo/Reset/Copy) lighter weight. | Establishes clear action hierarchy. Applies to all studio modes. |
| P12 | **Restyle the left tab strip.** Red+black card treatment, clearer selected state, keep the green **✔** done-ticks per completed section. | Tabs = Identity/Texture/Shape/Animation/AI/Attributes/Category. |
| P13 | **Restyle text fields.** ID / Name / URL / hex boxes get consistent red+black styling, focus glow, placeholder text. | Currently default vanilla `TextFieldWidget` look. |
| P14 | **Fix cramped spacing.** Rework panel widths + padding so groups have breathing room; nothing squished. | Pairs with P2 cards. |
| P15 | **Consistent icon set.** Use real little icons (copy, undo, play, trash, …) instead of text symbols (− + ✖). | Across the whole studio. |

### G14 follow-ups folded into this pass
The 5 unconfirmed Group 14 studio fixes (re-skin undo, new-GIF-keeps-settings, GIF-hide-bg-picker,
preview cross-fade, source-link-visible) did **not** pass owner test 2026-06-22 → owner moved them here as
"polish the screen later." P4/P5/P6 above subsume the visual ones; the functional ones (re-skin undo,
new-GIF-keeps-settings, cross-fade) stay as **fixes**, not paint. Command rename (`/cb animation`) = ✅
confirmed by owner, stays done.

### Suggested build slicing (NOT locked — owner deferred order)
Deploy + owner-test after EACH slice (bundling hides progress).
- **Slice A — the "looks professional" layer:** P1 theme (all screens) · P2 cards · P14 spacing · P12 tabs ·
  P13 fields · P11 action bar · P4 feedback · P10 shimmer. (Highest visible win, lowest risk.)
- **Slice B — control polish:** P3 hover-help-everywhere · P5 link copy · P6 static `#hex` · P7 cube
  drag/zoom/pause · P15 icons.
- **Slice C — the Timeline Editor (= P8, big, its own mini-spec):** filmstrip + handles + playhead + zoom +
  dim + transport + in/out + loop + snap + ruler(frame+time) + edit-frames + onion-skin + J/K/L + markers.
  This is the Bucket 2 / Phase 3 timeline work — **merge with it**, don't treat as polish.

> Reminder: P1 (all-screens red+black) means updating the "CB Screen Design Standard" table at the top of this
> doc (gold → red) and re-theming every existing screen, not just the studio.

---

## §G27.16 — Onboarding Tutorial + Achievements Gallery (folded from Group 23, design locked 2026-06-22)

> **Status:** 🟡 DESIGN CAPTURED, NOT BUILT. Folded in from **Group 23 (Player Experience)** — owner
> decided these two `Screen`s belong in the Screens group, migrated here with synced + cool features.
> Added by the Group 23 chat; **purely additive — modifies no existing section.** Golden rule applies.
>
> **No duplication:** both screens follow the existing standards already in this doc — do **not** re-spec
> them here. Reuse: **P1** (red+black theme, all screens) · **§87 "3D cube screens"** + `PreviewCube`
> (G27.6.P P7) for spinning previews · **§G27.13** (in-screen toasts, no chat) for feedback · **§G27.9.C /
> lines 1346–1360** (search / filter / favorites / recent) for any list behavior. This section specs ONLY
> what is net-new: the two screens' own layout/flow, plus two universal upgrades (below).
>
> **Group 23 owns the non-screen half** (book, sample blocks, FirstUseHints, TipPool, achievement
> **engine** = tracking + title/chat unlock notify + persistence, dashboard chest tab/tip-slot). See
> `GROUP_23_PLAYER_EXPERIENCE.md`. This section is the **single source of truth for the two screens.**

### §G27.16.A — Tutorial Screen (first-join)

- **Entry:** server-triggered once, on first join, immediately after the Starter Guide book is given —
  only if `tutorial_dismissed` is not set in `players.json` (G23 first-join detection). Sends
  `OpenGuiPayload(TUTORIAL)`.
- **Look (owner-picked 2026-06-22):** red/black per **P1**; **page 1 shows a spinning sample-block cube**
  (reuse `PreviewCube`, §87 rules). `CbScreenTemplate` frame + `[?]`.
- **5 pages**, buttons = `CbButton` Next / Back / Dismiss. Each page has **one command-prefill button**
  (`SUGGEST_COMMAND`, no auto-run):
  1. Welcome — what the mod does
  2. First block — prefill `/cb create `
  3. Dashboard — prefill `/cb`
  4. Tools — prefill `/cb brush` (mention `/cb deleter`)
  5. Help — prefill `/cb help`
- **Dismiss / Esc / finishing page 5** → client sends a dismiss payload; server sets
  `tutorial_dismissed` in `players.json`. **Never reopens** (matches old behaviour; tested G25.4 here).

### §G27.16.B — Achievements Gallery Screen (`/cb achievements`)

- **Entry:** `/cb achievements`, **or** the chest dashboard "Achievements" tab (G23, `MainMenu.java`) →
  `OpenGuiPayload(ACHIEVEMENTS)`.
- **Trophy-wall grid:** unlocked = item icon + name + (hover card) description + **unlock date**; locked =
  grey/locked icon + `???` description. Grouped by the engine's categories (Creation / Texture / Sharing /
  Color / Mastery). Search / filter / sort reuse **§G27.9.C** — not redefined.
- **Progress bar:** "X of Y unlocked" (replaces the old chest bottom-row progress slot from the G23 draft).
- **Data:** client reads achievement state from an engine sync payload (G23 §6/§7 = definitions +
  unlock notify). Screen is display-only; the **engine in Group 23 is authoritative.**
- **Live unlock:** if an achievement unlocks while the gallery is open, it animates in live (see §G27.16.C
  live-sync) — otherwise updates on reopen.

### §G27.16.C — Net-new universal upgrades (from the G23 brainstorm — reconcile before building)

These two were owner-requested for the whole screen system and are **not yet specced elsewhere** — flagged
here so the Screens-group build folds them into the unified pass (don't build them twice):

- **Live cross-screen server→client sync.** Open screens update in real time (block list changes,
  achievement unlocks, edits by others) without close/reopen. Needs a shared "screen state changed"
  payload + per-open-screen listener. Affects gallery, block list, studio Edit-tab, etc.
- **Rich hover cards.** Hover any block / achievement → floating card with a **mini spinning 3D preview**
  (`PreviewCube`) + live stats (glow, shape, category, author). Shared widget, used across screens.

**Already covered / routed — do NOT re-pitch here:** shared theme = **P1** · spinning cubes = **§87 / P7** ·
animations + sound + toasts = **§G27.13 / editor-feedback sounds** · search + filter + favorites + recent =
**§G27.9.C / lines 1346–1360** · **Showcase mode** (auto-rotating block gallery) = already routed to
**Group 19** (floating holograms / display-entity showcases, see "Out of scope this pass" / line ~2001).

### §G27.16 — cross-references

- Engine, book, samples, hints, tips, chest dashboard tab/tip-slot, persistence → `GROUP_23_PLAYER_EXPERIENCE.md`.
- `OpenGuiPayload` needs two new kinds: `TUTORIAL`, `ACHIEVEMENTS` (same pattern as `CREATE_STUDIO` /
  `SHAPE_EDITOR`).
- **Tests:** add to `Reports/GROUP_27_TESTING_GUIDE.md` → § "G27.16 — Onboarding + Achievements screens"
  (covers former G23 tests G25.2/G25.3/G25.4 tutorial + G25.9/G25.11 gallery).

---

## §G27.17 — `VaultConflictScreen` re-skin (folded from Group 20 §S2, design locked 2026-06-22)

> **Folded into the screens group 2026-06-22** (owner via UI: make the cloud-vault conflict screen a real
> member of the screen family, synced to the standard, *before* in-game test). Mirrors how the Animation
> screen (§G27.15) was folded in from Group 14.
>
> **This section is the single source of truth for the SCREEN.** Group 20 keeps the non-screen half —
> `VaultConflict` resolve handler, `VaultConflictPayload` (S2C), `VaultResolvePayload` (C2S), `VaultBlockCodec`,
> `StudioTextureLoader.framesFromPng` / `loadFromPack`, and the server console / no-player text fallback. See
> `GROUP_20_EXTERNAL_INTEGRATIONS.md`.
>
> **State:** `client/gui/VaultConflictScreen.java` already exists and builds green (Group 20 §S2 first cut),
> but it uses ad-hoc chrome and is **not yet tested in-game**. This re-skin rebuilds it to the family
> standard; the owner tests S2 **after** the re-skin lands.

### What it is
Pops when `/cb vault download <code>` restores a block whose id already exists locally. Shows BOTH blocks and
asks the player how to resolve the clash. Console / no-player → server text fallback (Group 20).

### Owner decisions (locked via UI 2026-06-22)

| Topic | Decision |
|---|---|
| Chrome | **Red** (`CbTheme` / P1) — match Studio + the migration target. (Was ad-hoc red/own constants.) |
| Frame | **Full family frame**: backdrop = `CbScreenPrefs.get().backdrop()` (persisted dim), `CbDimSlider` top-right, `[?]` help (`CbHelpOverlay`), `shouldPause()` false, dispose both cubes in `removed()`. |
| Title | `§c§l Block already exists §7— §f<id>` + 2 hint lines (§7 controls · §8 universal). |
| Cubes | Two `PreviewCube`s side by side — YOURS (left, tag "keeps id") / INCOMING (right, tag "from cloud"). **Rotate together** (shared yaw/pitch/spin/zoom; drag = rotate both · scroll = spin · shift+scroll = zoom · click = pause · R = reset). Incoming animates if multi-frame; yours from the active pack. |
| Compare view | **Simple ⇄ Advanced toggle** (segmented pill, top-right of content). Two stat columns under each cube. **Simple** = hide matching rows, show only differing rows highlighted gold. **Advanced** = all rows. **Identical** → one calm "identical — nothing differs" line (kills the scary double "identical"). Stats: glow, hardness, sound, solid, category, shape, frames (idx hidden). |
| id box | `save incoming as` (`CbTextField`) pre-filled with the next free id; live validate (charset `[A-Za-z0-9_.+\-]+`, ≠ incoming id) → free / taken indicator; greys **Keep Both** + **Rename Mine** while invalid. Ignored by Override. |
| Actions | **Fixed decision-button row** (`CbButton` — **not** the dockable `CbActionBar`; these are one-shot decisions, not edit tools). |
| — Override | Danger-red. **Confirm popup first** (in-screen overlay, family style: "Replace your block?" / [Yes, replace] / [Keep mine]). On yes: delete local, incoming takes the id. Undoable via `/cb undo`. |
| — Keep Both | **Primary, highlighted default.** Incoming saved under the typed id; local untouched. |
| — Rename Mine | Neutral. Local re-id'd to the typed id; incoming takes the original id. No delete → no confirm. |
| — Cancel / Esc | Ghost. Nothing changes. No dirty-confirm (it's a decision, not an edit). |
| Feedback | UI clicks + confirm chime + green flash on apply (Studio pattern, Royal Directive §2). Result via **toast** (§G27.13), `[CB]`-branded ✔ / ✖ — not chat. |

### Build notes (next chat)
- **Re-skin** the existing `VaultConflictScreen.java` — don't rewrite from zero; keep the working cube /
  meta-parse / payload logic, swap the chrome onto the family frame.
- **File-size gate:** ≤ 500 lines (current ~315; if frame chrome pushes it over, split a `VaultConflictStats`
  helper).
- `/cb undo` is the supported revert; **redo** of Override / Rename Mine is best-effort (compound batch) —
  keep the existing caveat.
- Tests live in `Reports/GROUP_20_TESTING_GUIDE.md` §2 (currently frozen "redesign pending") — unfreeze once
  the re-skin builds green.

---

## §G27.18 — Screens Group Unification: membership + Wave-4 cool features (brainstorm locked 2026-06-22)

> Owner brainstorm (via UI, 2026-06-22): fold **every** GUI in the package into one documented Screens Group,
> to be **migrated later** with shared "cool" features. **Design / roadmap only — nothing built this pass.**
> Migration runs LATER, screen-by-screen, each tested in-game (phase discipline; max-5-item plans).

### Membership = **Everything GUI** (owner pick)
One documented system covering:
- **Full screens:** `BlockCreationStudioScreen`, `ArabicPreviewScreen`, `EyedropScreen`, `HudEditorScreen`,
  `VaultConflictScreen` (§G27.17), Tutorial + Achievements (§G27.16). (Recolor + Shape already folded into the
  Studio, §G27.11.)
- **HUD overlays:** `HudBrickInspector`, `HudPresetBrowser`, `HudColorPicker` — **in-world, not modal**; they
  adopt theme / sound / toast, **not** the full title-bar frame.
- **Shared widgets (the glue):** `CbActionBar`, `CbColorPanel`, `CbDimSlider`, `CbHelpOverlay`, `CbButton`,
  `CbTextField`, `PreviewCube`, `CbToast`, `CbTheme`.

### Wave-4 features — status (NO duplication; route to existing where already specced)

| Feature | Status | Home |
|---|---|---|
| Red theme everywhere | rollout | P1 / migration target (gold retired screen-by-screen) |
| Persisted dim slider | done · roll out | §G27.7 §A2 (`CbScreenPrefs`, `CbDimSlider`) |
| Dockable/hideable action bar | done · roll out + flip red | §G27.7 §A4 (`CbActionBar`, currently gold) |
| `[?]` help overlay | done · roll out | `CbHelpOverlay` |
| Universal shortcuts | done · roll out | std "Universal keyboard shortcuts" |
| Copy/paste setting-codes | done · roll out | std |
| Per-screen undo/redo | done · roll out | std |
| Cancel-confirm | done · roll out | std |
| **UI scale** | selected | already §G27.8 (~line 1063) |
| **Toasts (success/info/error)** | selected | already §G27.13 |
| **Cross-screen presets + sharing** | exists | already §G27.8.G |
| **Tooltips / rich hover cards** | selected | already §G27.16.C (rich hover cards) |
| **Recent & favourites** | selected | already §G27.9.C / lines 1346–1360 |
| **Open/close animation** | selected | extend §G27.8 "reduced motion" toggle (~line 1064) — add the real transitions |
| **Unified sound theme** | selected | extend §G27.13 / editor-feedback sounds — one open/hover/click/confirm/error set |
| **Multi-OP edit lock** | selected · **NEW** | extend §G27.16.C live cross-screen sync — soft lock + "being edited by X" badge |
| **Shared destructive-confirm** | selected · **NEW** | generalise cancel-confirm + Override confirm into ONE reusable overlay |
| **Screen-switcher hub** | selected · **NEW · FLAGSHIP** | this section (below) |
| **Theme / skin presets** | selected · **NEW** | this section — AMOLED / high-contrast / colourblind-safe palettes, per player |
| **Density toggle** | selected · **NEW** | this section — compact vs roomy |
| **Accent colour picker** | selected · expand | player-facing picker across all CB screens (today: HUD accent §G27.14, P1 red) |

**Parked / dropped:** world-blur toggle — **parked** (blur ≠ dim; dim already exists; easy add later).
"Cross-screen presets" is **not** dropped — it already lives at §G27.8.G.

### Flagship — Screen-switcher hub (NEW)
One hub backend (list of CB screens + Recent / Favourites from §G27.9.C) with **four "doors" (access methods),
built in order** — all stay in the roadmap; only which is built first is open (owner: "want them all";
**lean: grid launcher first**):
1. **Grid launcher** *(lean: build first)* — a key (e.g. Tab) opens a centered grid of all screens, icons +
   live mini-3D thumbnails (`PreviewCube`), and hosts Recent & Favourites. Best foundation; the others plug
   into the same backend.
2. **Top tab strip** — always-visible tabs in the title bar; switch between a few.
3. **Side icon rail** — thin vertical rail docked left/right; hover = name, click = switch.
4. **Radial menu** — hold-key flick pie at the cursor; fastest for power users.

### Net-new specs (brief — full spec when each slice is scheduled)
- **Theme / skin presets:** beyond accent — full palettes (AMOLED black / high-contrast / colourblind-safe),
  per player, persisted in `CbScreenPrefs`.
- **Density toggle:** compact vs roomy layout metric, per player, persisted.
- **Accent picker:** player-chosen accent (red default) applied across all CB screens; persisted; HUD accent
  (§G27.14) reads the same value.
- **Shared destructive-confirm:** one reusable in-screen overlay (title + [confirm] / [cancel]); replaces the
  per-screen copies (cancel-confirm, Override confirm).
- **Multi-OP edit lock:** when an OP opens a block's screen, broadcast a soft lock; other OPs see "being
  edited by <name>" + a disabled / confirm path. Builds on the §G27.16.C live-sync payload.

### Migration rules (unchanged)
Screen-by-screen, in order, **each tested in-game before the next** (phase discipline). Build order TBD when
scheduled. **Nothing here is built until then.**

---

## §G27.19 — `VariantRepaintScreen` — batch repaint of all `_<colour>` variants (design locked 2026-06-26)

> **Status:** design locked with the dev **2026-06-26**. **Nothing built yet** — spec only; per-slice
> status → testing guide. Build in small slices, dev tests each in-game (CLAUDE.md §2/§4).
>
> **Origin:** Group 06 / M3 hex. When `/cb config hex <colour> #NEW` changes a colour and `_<colour>`
> blocks already exist, today a plain **Yes/Info/No chest** (`HexRecolorConfirmMenu`) asks whether to
> repaint them, then silently runs `recolorvariants` using the **global** background settings. The dev
> wants this replaced by a **real, self-contained Group-27 screen** with a bg-mode switcher, a strength
> slider, a 3D before/after preview, scope selection, and per-colour memory.

### Why this exists
The repaint is a **batch background-fill** across every existing variant of one colour — a different
operation from the §G27.11 Studio **HSL/tone** recolour (which retints one block's whole texture). It has
its own controls (which pixels count as background, how loosely), so it gets its own screen rather than
riding the per-block HSL section. The chest confirm gives no control and no preview; the dev was burned by
"click Yes and hope." This screen makes the repaint **visible and tunable before it commits.**

### Decisions (locked 2026-06-26)

**Self-contained — never depends on another tab (dev: "shouldn't rely on editing settings in other tabs for it to work")**
- **Every** control the repaint needs — bg-mode, strength, scope — lives **in this screen**. The player
  never has to open the Settings tab, the global background config, or any other studio section to make
  the repaint work.
- The screen **seeds** its mode + strength from the global background settings on open (sensible default),
  but the player can override them here, and **Apply uses the in-screen values for this batch only** — the
  global config is untouched.
- One **optional** `[Save as default]` writes the chosen mode + strength back to the global background
  settings (the only sync write). The repaint works fully without ever pressing it.

**A standalone screen, reachable from the `/cb create` Studio menu (dev: "standalone screen in the /cb create menu")**
- New standalone client `Screen` — `VariantRepaintScreen` — built on `CbScreenTemplate` (Group-27 frame),
  **not** a chest GUI, **not** a section embedded in `BlockCreationStudioScreen` (keeps the 500-line studio
  screen clean; it's its own operation).
- **Entry points (all open the same screen):**
  1. After `/cb config hex <colour> #NEW` when `_<colour>` blocks exist → opens this screen for that colour
     (replaces the `HexRecolorConfirmMenu` chest).
  2. A **launcher entry in the `/cb create` Studio menu** ("Variant Repaint") → pick a colour → this screen.
  3. `/cb repaint <colour>` command.

**Controls (all four dev-picked extras + the always-in core)**
- **Bg-mode switcher** — radio tiles: **Remove bg** (`edges`) · **+ gaps** (`closed`) · **Smart** (`smart`).
  `none` is excluded (a repaint with no background detection does nothing). Selected tile highlighted.
- **Strength slider** — a real `CbDimSlider`/`CbGradSlider`, 0–100 (the "visual slider bar" extra is the
  native slider). Maps to `backgroundTolerance` exactly as the bake rail already does.
- **3D before/after preview** — two `PreviewCube`s (or one split cube): **before** = a sample variant's
  current texture; **after** = the server-baked result at the chosen mode+strength. Drag rotates both.
- **Preview 1 block** — bakes **one** sample variant on the server at the current mode+strength and streams
  the PNG back to the **after** cube, so the player eyeballs the real result before committing all. (The
  bg-detect flood-fill is server-side Java — preview is a real single bake, **not** a client guess.)
- **Scope** — **All N `_<colour>`** (default) **or** **Pick blocks…** → a scrollable checkbox list of the
  variant ids; only ticked ones repaint. (The "scope: pick blocks" extra.)
- **Per-colour memory** — the screen reopens with the **last mode + strength + scope** used for that colour
  (persisted per player). (The "per-colour memory" extra.)
- **Apply → repaint N** — runs the batch (server) at the in-screen mode+strength over the chosen scope; one
  pack rebuild after; result reported as an **on-screen toast** (§G27.13), not chat.
- Group-27 standard frame: `[?]` help, `Ctrl+Z` undo (= `/cb undo` the batch), `Esc` cancel-confirm if
  changed, save-flash + toast.

### Apply timing — preview one, bake all on Apply (purple-block-safe)
- **Preview** bakes exactly **one** sample variant (server) and shows it; no batch, no global pack reload
  churn.
- **Apply** runs the existing `recolorVariants` batch **once** over the scope, then **one** pack rebuild
  broadcast after (§7 pitfall: never push a reload mid-prompt; keep the debounce short). No per-drag bake.
- Reuses the **existing bake rail** — `BackgroundRemover.recolorBackground` + `recolorVariants` — no parallel
  image path (CLAUDE.md §5).

### Layout (Group-27 standard frame)

```
╔══════════════════════════════════════════════════════════════════════╗  ← family frame (red, §G27.18)
║  §c§lVariant Repaint §7— §fGreen  §8(12 blocks → #10FF01)        [?] ║
║  §7pick mode + strength · Preview tests one · Apply repaints all     ║
║  §8Ctrl+Z undo · Enter = apply · ? help                             ║
╠════════════════╦═══════════════════════════╦═════════════════════════╣
║  BG MODE       ║   BEFORE   │   AFTER       ║  SCOPE                  ║
║  ───────────   ║  ┌──────┐   ┌──────┐       ║  ───────────────        ║
║  ◉ Remove bg   ║  │ cube │   │ cube │       ║  ◉ All 12 green         ║
║  ○ + gaps      ║  │ old  │   │ new  │       ║  ○ Pick blocks…         ║
║  ○ Smart       ║  └──────┘   └──────┘       ║   ☑ mars_green          ║
║                ║   drag = rotate both       ║   ☑ vart_green          ║
║  STRENGTH      ║                            ║   ☐ foo_green           ║
║  ▰▰▰▰▰▱▱▱ 55%  ║   [ Preview 1 block ]      ║   …scrolls              ║
╠════════════════╩═══════════════════════════╩═════════════════════════╣
║  [Undo] [Reset]        [§aApply → repaint 12]        [Save default] [Cancel] ║
╚══════════════════════════════════════════════════════════════════════╝
```

### Architecture (code-grounded; ≤500-line gate; reuse rails)
| File | New/changed | Purpose |
|---|---|---|
| `client/gui/VariantRepaintScreen.java` | **new** | The screen: frame + mode tiles + strength slider + before/after `PreviewCube`s + Apply. Delegate the scope list to keep ≤500. |
| `client/gui/VariantRepaintScope.java` | **new (if needed)** | Scrollable checkbox variant-id list widget (keeps the screen ≤500). |
| `network/payloads/OpenVariantRepaintPayload.java` | **new** | Server→client open: colour, new hex, variant-id list (+ a sample id/texture for the before cube). |
| `network/payloads/VariantPreviewPayload.java` | **new** | Client→server bake-one(mode,tol,sampleId); server→client result PNG → after cube. |
| `network/payloads/VariantRepaintApplyPayload.java` | **new** | Client→server: mode + tol + chosen scope → run the batch. |
| `core/ColorVariantService.java` | **change** | `recolorVariants(...)` gains `mode` + `tol` + optional id-scope params (default to global when absent). Foundation for everything. |
| `command/handlers/HexCommands.java` | **change** | `recolorvariants` accepts mode+tol; `setHex` opens the screen instead of `HexRecolorConfirmMenu`; add `/cb repaint <colour>`. |
| `client/CbScreenPrefs.java` | **change** | Per-colour last mode/tol/scope memory (per player). |
| Studio menu launcher (`BlockCreationStudioScreen` / chest entry) | **change** | "Variant Repaint" door → colour pick → screen. |
| `gui/chest/HexRecolorConfirmMenu.java` | **retire** | The chest confirm is replaced by the screen. |
| `BackgroundRemover.recolorBackground` bake rail | **reuse** | Single-sample preview bake + the Apply batch — no new image path. |

### Build slices (small, build-green, dev tests each — CLAUDE.md §4)
0. **Foundation (invisible):** `mode`+`tol` params on `recolorVariants` + the `recolorvariants` command;
   `/cb repaint <colour>` opens the *existing* chest confirm for now (proves the command path before the
   screen exists). Build green. *(Both the chest and the screen need this — lowest-risk first step.)*
1. **Screen shell:** `VariantRepaintScreen` frame + bg-mode switcher + strength slider + `Apply → repaint
   all` (no preview/scope yet). Replaces `HexRecolorConfirmMenu` after `/cb config hex`. Test in-game.
2. **Before/after preview + Preview-1-block** (server-baked sample → after cube).
3. **Scope — Pick blocks** checkbox list.
4. **Per-colour memory + Save-as-default + toasts + Studio-menu launcher + polish.**

### Reconciliation
- **Distinct from §G27.11** (Studio Recolour): that is per-block **HSL/tone** retint inside the studio; this
  is a **batch background-fill repaint** of every variant of one colour. They do not collide — different
  operation, different screen.
- **Supersedes** `HexRecolorConfirmMenu` (the Yes/Info/No chest, Group 06) once Slice 1 lands.
- Uses the §G27.18 family widgets (`CbScreenTemplate`, `PreviewCube`, `CbDimSlider`, `CbToast`, red theme)
  and follows the standard frame, shortcuts, cancel-confirm, and toast-not-chat (§G27.13) rules.

### Success criteria (CLAUDE.md §2 — the Golden Rule; nothing ✅ until the dev confirms in-game)
- `/cb config hex green #NEW` with `_green` blocks → opens `VariantRepaintScreen` (not the old chest).
- Mode switcher + strength slider change the result; **Preview 1 block** shows the real baked sample on the
  after cube before any batch runs.
- **Apply** repaints the chosen scope (all, or only ticked blocks), one pack rebuild, toast reports the
  count; design pixels untouched (only the detected background changes).
- The screen works **without** opening any other tab/settings; `[Save as default]` is optional.
- Reopening for the same colour restores the last mode + strength + scope.
- `/cb repaint <colour>` and the `/cb create` Studio launcher both open the same screen.

---

## G04-1 (G27 slice) · CbActionBar Restyle + Chat-History Log Screen

> 🔍 designed — **G27-owned deliverables from G04-1; full spec in [GROUP_04_CHAT.md](../groups/GROUP_04_CHAT.md) G04-1**

Two G04-1 items that belong to this doc because they are screens or in-screen components:

**1 · CbActionBar palette restyle** (brainstorm item 5 in G04-1)
Repoint CbActionBar's private colour constants (GOLD/BAR_BG/BTN_BG/PRIMARY) at the shared
G04-1 style spec palette so the in-screen button bar matches chat/hotbar. Pure styling — no behaviour change.

**2 · Chat-history / log screen** (open bucket item in G04-1)
A G27 screen listing recent [CB] lines so the player can scroll back through them. Design feeds the
one style spec. Screen shell + data model to be co-designed with G04-1's incremental delivery slice (e)
and the G27 screen infrastructure.

**Build sequence:** wait for G04-1 slice (a)–(c) to land in G04 first (style spec + colour/glyph +
sound); then these two G27 slices are unblocked.

| Group | Scope | Status |
|---|---|---|
| G27 — Screens | Both — CbActionBar is client; log screen is client | 💬 discuss later (with G04-1 incremental delivery) — not built |

---

## G27-2 · `/cb editor` Rework — Studio Edit-Mode + New "Manage" Tab

> 🔍 designed — **do before G06-7**; the `StudioManagePanel` tab pattern G06-7's Drops tab reuses

> *"/cb editor should open /cb listgui, then choosing a block opens /cb editor <id> which should be completely reworked to open a screen tab in '/cb create' menu."*

### What's broken today

- `/cb editor` (no arg) → **not registered** — errors.
- `/cb editor <id>` → old chest `EditorMenu` (give / glow / hardness / sound / collision / category / rename / retexture / note / delete). Functional but dated; duplicates what the Studio already does.
- `CreationStudioBridge.openStudioEdit(src, id)` **already exists** — opens Studio in edit-mode. The "load a block into the Studio" rail is built; this rework repoints the command onto it.

### What's already in Studio tabs — DO NOT duplicate

| Existing tab | Covers |
|---|---|
| TEXTURE | Retexture |
| SHAPE | Shape |
| ATTRIBUTES | Glow, hardness, sound, collision |
| ANIMATION | Animation/GIF |
| CATEGORY | Category |
| AI | AI texture gen |

### ✅ All decisions (locked 2026-06-24, 2026-06-25)

- **`/cb editor` no-arg** → opens `BLOCK_LIST` (same as `/cb listgui`). Clicking a block in the list → `/cb editor <id>`.
- **`/cb editor <id>`** → `openStudioEdit(src, id)` — Studio screen in edit-mode, landing on the **Manage tab** first.
- **New tab name: "Manage"** — sits in the left tab list alongside TEXTURE/SHAPE/ATTRIBUTES/ANIMATION/AI/CATEGORY/TEXT.
- **Default tab on open: Manage** — when editing a specific block, always land there first.
- **Manage tab actions (not duplicated anywhere):** Give block · Rename · Change ID · Lore/Note · Delete · Background Studio · Color variants · (later waves: history / stats / export / template / test-in-world / lock / favorite / batch-from-this-block).
- **Replace old chest `EditorMenu` fully** — retire `Dest.EDITOR`; repoint all openers (list menus, chat links, other GUIs) to Studio edit-mode.
- **Block list GUI** (`/cb listgui`) needs its own improvements (noted; separate backlog item).
- **All "open Studio for this block" entries consistent** — `/cb animation <id>`, `/cb editor <id>`, and any future single-block door all open the same edit-mode screen, defaulting to their relevant tab.

### Architecture (code-grounded)

- New `Section.MANAGE` enum value in `BlockCreationStudioScreen` + `StudioManagePanel` class (own file, under 500-line gate — mirror `StudioAnimPanel`/`StudioAiPanel`/`StudioCategoryPanel`).
- Buttons delegate to existing `/cb` commands via `GuiRouter.runAndReopen` / `promptCommand` / `confirmCommand` — same pattern as `EditorMenu`. No client-authoritative edits.
- `default → "coming soon"` placeholder branch already exists in `StudioSections` — slots in cleanly.
- Wave delivery: lifecycle (give/rename/ID/lore/delete) first → history+stats → sharing/export → future items.

| Group | Scope | Status |
|---|---|---|
| G27 — Studio/Screens | Both — client screen + server-command delegation (dedicated-safe) | 🔍 designed — all decisions locked 2026-06-25; ready to build |

**Related:** `BlockCreationStudioScreen` / `StudioSections` (extend with `Section.MANAGE`) · `CreationStudioBridge.openStudioEdit` (reuse as entry point) · `EditorMenu` (retire — action list = migration checklist) · `BlockListMenu` (no-arg picker, kept) · G06-7 (Drops tab uses same `Section` pattern — build Manage first) · G13-22 Text tab (same tab-addition infra)
**Touches:** `ChestGuiCommands` (register `/cb editor` no-arg; repoint `/cb editor <id>`) · `BlockCreationStudioScreen` + `StudioSections` (new `Section.MANAGE`) · new `StudioManagePanel` · audit + repoint all `Dest.EDITOR` openers · `BlockListMenu` click → Studio edit-mode
**Verify in-game:** `/cb editor` opens block list; clicking a block opens Studio on Manage tab; all Manage actions (give/rename/delete etc.) work correctly; old chest editor gone; `/cb animation <id>` still opens Studio (on Animation tab, not Manage).


---

## G27-1 · Cooler HUD customization — expand the on-screen block HUD

> 🔍 designed — all decisions locked 2026-06-25; folded from ISSUES.md

### Decisions (locked 2026-06-25)

**First slice — all 4 ship together (none dropped):**
1. **More animations** — typewriter reveal, slide/fade-in on aim, wave, blink, bounce, glitch. Extend `HudField.Effect` enum + `HudRenderer` branches.
2. **More background shapes** — rounded rect, speech-bubble, outline-only, gradient fill. Extend `BgShape` + `HudBgShapes`.
3. **Block-preview brick** — new `HudFieldType` that renders the aimed block's item icon / spinning 3D model in the HUD. Gated on `c.hasBlock()`.
4. **One-click themes** — see "Themes" section below.

**Floating label (decision: toggleable — both modes):**
- **Mode A — aimed only:** label floats above the one block the crosshair hits. Default.
- **Mode B — all visible:** every custom block on screen gets a floating nameplate (like mob health bars).
- A keybind or config toggle switches between modes. Both fully implemented.
- World→screen projection + depth handling (existing anchor-mode extension point on `HudRenderer`).

**Themes (decision: two categories):**
- **Theme = restyle-only skin.** A named bundle of: colour palette (6 named roles: primary / accent / dim / background / text / shadow), font style (bold/italic/shadow), default bg shape, default effect. Applying a theme reskins every brick using those roles — layout (positions, which bricks show) untouched.
- **Full Presets = complete layout replacement.** Total HUD config dump. Existing mechanism, kept separate from themes.
- **Theme browser:** live mini-preview panel — hover a theme and every brick in the preview updates to that skin in real time. Choose → confirm.
- Ship 4–6 polished bundled themes first: neon / minimal / parchment / terminal / cinematic (clean, recording-focused) / high-contrast.

**NEW — Universal floating settings panel (owner addition, 2026-06-25):**
- A persistent, floating, always-accessible settings panel that appears **in every screen** (HUD editor, Studio, bulk menus, etc.) at a fixed but user-moveable position.
- Contains quick controls for cross-cutting settings: HUD dim opacity, showcase cycling speed, overlay brightness, and future global knobs.
- Moveable + configurable (position persists in config). Toggled by a keybind or a pin icon.
- Lives in G27 (screen layer); implemented as a client-only overlay on top of all screens.

### Deeper brainstorm — future slices (NOT v1)

**Light (one field / one render branch each):**
- Conditional bricks — show only when rule holds (glow > 0, unbreakable, drops ≠ self).
- Icon library — small icon beside a value.
- Color-by-value thresholds — user-set rules ("hardness > 8 = red").
- Staggered reveal — bricks cascade-in with per-brick delay.
- Sound on reveal — soft tick via SoundFx.

**Medium:**
- Per-character animation (wave/typewriter/glitch per glyph).
- Distance LOD (far = name only, close = full panel).
- Mini graphs / gauges (radial gauge brick, light-falloff sparkline).
- Smart layout — auto-avoid crosshair / hotbar.
- Custom fonts — register a client font resource for the HUD.

**Flagship (own slices):**
- Comparison mode — sneak-lock block A, aim at block B → HUD shows attribute diff.
- Scan / radar — mini list or radar of all custom blocks in view.
- Interactive HUD — hover brick for tooltip; keybind opens aimed block's editor (G27-2) from the HUD.
- Auto-themes — theme switches by time-of-day or biome.
- Cinematic / photo preset — clean minimal HUD skin for recording (in-frame; counterpart to G29-1 capture-invisible overlay).
- Share HUD layouts — export/import HudConfig (preset sharing + accessibility skins).

### Architecture

- All slice-1 items are **additive per-brick fields** on `HudField` (+ JSON keys) and new branches in `HudRenderer.drawField` / `HudFieldType`. Model already serialises cleanly — incremental delivery.
- Block-preview brick: new `HudFieldType` + item/model render call inside the overlay.
- Floating label: world→screen projection in `HudRenderer`; new anchor mode.
- Themes: `HudTheme` object (palette + style roles); `HudThemeBrowser` screen; theme application loops bricks and sets role-driven fields without touching layout.
- Universal settings panel: client-only overlay class, drawn on top of all screens; keybind toggle; position + state in `HudConfig`.
- Everything stays **per-player client config** (already is). No server cost.

| Group | Scope | Status |
|---|---|---|
| G27 — HUD | Both — client-side render + per-player config (no server cost) | 🔍 designed — all decisions locked 2026-06-25; ship 4-item first slice; floating label, themes, universal panel decided |

**Related:** `HudFieldType` / `HudField` / `HudRenderer` · `HudEditorScreen` · `HudPresetStore` · `HudBgShapes` · G04-1 (visual-voice consistency) · G29-1 (cinematic preset counterpart) · G14-1 (block-preview brick synergy)
**Touches:** `HudField` (+ new per-brick animation/style fields) · `HudField.Effect` + `HudField.BgShape` (new enum values) · `HudFieldType` (block-preview type) · `HudRenderer` (animation timing, bar/icon/model draw, world→screen projection for floating label) · `HudBgShapes` (new bg shapes) · `HudEditorScreen` + inspector (expose new axes) · new `HudTheme` + `HudThemeBrowser` · new universal settings panel overlay · `HudConfigStore` JSON (new keys, back-compat defaults)
**Verify in-game:** each new animation plays in live HUD + editor preview; new bg shapes render; block-preview brick shows the aimed block icon; floating label appears above aimed block (and all visible in mode B); themes restyle all bricks without moving them; full preset overwrites layout; universal panel appears in all screens at its saved position.