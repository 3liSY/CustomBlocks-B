# Group 27 — Unified Screen Design System, Upgrades & Block Creation Studio

> **Prerequisites:**
> - Group 13 (Arabic — live preview) confirmed working in-game. `ArabicPreviewScreen` is the reference implementation.
> - `BlockCreationStudioScreen` (G27.6) additionally requires all 5 screen upgrades (G27.1–G27.5) confirmed in-game first.
>
> **Objective:** Establish a unified design language for all CB `Screen` subclasses, implement the shared template, upgrade every existing screen to follow it, build the new `ShapeEditorScreen`, and build the full `BlockCreationStudioScreen` that opens when the player runs `/cb create` with no arguments.
>
> **Rules:** Work screen by screen in order. Stop and confirm in-game before moving to the next screen. Nothing in this group is marked done until tested in-game.

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
- Tests: `docs/Finale Fix/Reports/GROUP_27_TESTING_GUIDE.md`
- `/cb edithud` → opens `HudEditorScreen` (G27.4) — command already exists; G27.4 is a full rebuild into the Lego HUD builder (see §G27.4), not just wiring
- Shape panel in G27.6 reuses all design from G27.5 (same AABB editor, same wireframe, embedded in sidebar)
- Color panel in G27.6 inspired by `ArabicPreviewScreen` color studio + `ColorStudioMenu` palette

---

## §G27.6.X — BlockCreationStudioScreen Extended Design (locked 2026-06-18)

> This section extends the original §G27.6 spec with features finalized in the 2026-06-18 design +
> mockup session. The original §G27.6 remains authoritative for the core pipeline (entry, Identity,
> Texture-URL, Shape, Attributes, Organize, session memory, validation, Draft/Create). This section
> adds everything beyond that. Interactive mockup: `docs/Finale Fix/mockups/cb_create_studio.html`.

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
