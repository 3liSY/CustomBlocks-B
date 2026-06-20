# Group 13 — Arabic System — Build Plan (surgical, test between passes)

> Companion to `GROUP_13_ARABIC.md` (the in-game test spec) and `ADR-003`.
> Golden Rule: nothing here is DONE until the developer confirms it in-game.
> Built in ordered passes; stop and hand back for a test after each.

## Locked decisions (from the developer, 2026-06-15)

- **Build everything**, surgically, testing between passes.
- **Fonts bundled** in the JAR, auto-extracted on boot: `arabtype.ttf` (Arabic) + `Rockwell-Condensed
  Bold.ttf` (English). Script auto-detected per text. Private-server use → bundling is fine.
- **Input redesign** via the existing `AnvilPrompt` (no Brigadier non-ASCII problem). `/cb arabic word
  <id> <name>` → anvil → chest with **both** buttons (Give the blocks / Auto-place a row). `/cb arabic
  text <color>` → anvil → text block. Word/text blocks render in the same style.
- **Auto-joining** = a **new dedicated joinable letter block** with a BlockEntity + live in-memory
  texture (**ADR-005**; supersedes the ADR-003 FORM-blockstate/pack plan). Forms correct on first
  attempt (isolated = hand-art, connected = font-shaped/restyled), **per-form PNG override** for polish.
  Direction stored per-block (facing/axis); set at placement or by the OmniTool Arabic Direction mode.
- **Textures hybrid**: hand-drawn PNGs for isolated letters + extra forms + numbers; engine draws
  connected forms + custom words in the matching style (white glyph + black stroke + colour bg).
- **Colour = background**, glyph white. Default white-on-black. Red/Green/Yellow live-linked to the
  marker hex via the existing `ColorVariantService`; full control (29-colour + arbitrary hex) on top.
- **Stroke**: black outline, thickness measured from the sample PNGs.
- **Scope**: 28 letters + extra forms (hamza, ta_marbuta, alef variants, maqsura, waw/ya hamza) +
  Eastern numbers ٠–٩ + Western numbers 0–9.
- **Browser**: tabbed chest GUI (Letters / Numbers / Words / Recent / Colors / + user-creatable
  sections), mirroring the `/cb category` system; click → give; colour rail sets grab-colour.

## Reuse map (do NOT rebuild these)

| Need | Reuse |
|---|---|
| Live render state per placement | `SlotBlock.LIGHT` pattern + `getPlacementState` (ADR-002) |
| Neighbour re-evaluation, bounded | `block/SlotLighting` (template for the form updater) |
| Free-text input in chest UI | `gui/chest/AnvilPrompt` |
| Colour variants + live hex relink | `core/ColorVariantService` (`createVariant` / `recolorVariants` / `swapPlaced`) |
| Background recolour / pixel swap | `image/BackgroundRemover.recolorBackground`, `image/ColorReplacer` |
| Per-block live texture (no reload) | `block/ScreenTest*` + `client/render/ScreenTestBlockEntityRenderer` (ADR-005 spike → graduate) |
| Chest GUI framework + nav | `gui/chest/` `ChestMenu`/`GuiRouter`/`Nav`/`Layout`/`Icons` |
| Creatable/customizable sections | `core/CategoryService` + `gui/chest/CategoryBrowserMenu`/`CategoryEditMenu` |

## Assets

- Source art: `Desktop/Random Docs/arabic_numbers_png/` — BLACK/RED/GREEN/YELLOW × 56 (28 letters +
  extra forms + Eastern a0–a9 + Western num_0–9), all 256² RGBA, white glyph + black stroke + colour bg.
- Font files: `arabtype.ttf` (in `run/config/customblocks/`), `Rockwell-Condensed Bold.ttf`
  (`Desktop/rockwell-condensed_1_65vhx/Rockwell-Condensed/Rockwell-Condensed Bold/`).
- Name reconciliation: `ArabicLetterMap` dhal/tah/dhah/nun ↔ art thal/ta2/tha2/noon (mapping table).

---

## Pass 1 — Fonts bundled + script routing  *(low risk)*
Embed `arabtype.ttf` + `RockwellCondensed.ttf` at `assets/customblocks/fonts/`; auto-extract to
`config/customblocks/fonts/` on first boot (log line; graceful disable if absent). `ArabicWordRenderer`
loads arabtype for Arabic; a new path uses Rockwell for Latin; choose by scanning the text's script.
- Touches: `arabic/ArabicWordRenderer` (font load + script routing), mod init (extract-on-boot),
  resources (2 ttf). **No shared-block changes.**
- Tests: **G13.1**, **G13.2**, **G13.9**.

## Pass 2 — Asset import pipeline (hand-art blocks) + override loader
Bundle the PNG art; import letters/extra-forms/numbers as blocks straight from the art (isolated form,
white default). Wire the per-form/per-block **drop-in override** folder (`config/customblocks/arabic/`).
Plug colour variants into `ColorVariantService` (background = colour). Reconcile asset↔letter names.
- Touches: `arabic/ArabicLetterMap` (+ extra forms + numbers + name map), `arabic/ArabicBlockRegistry`
  (import from PNG, override loader), `command/handlers/ArabicCommands` (import/list scope), resources (art).
- Test: import → blocks exist with correct hand-art textures; override PNG replaces one on reload;
  red/green/yellow variants via the existing colour tools.

## Pass 3 — Anvil word/text flow  *(reuses AnvilPrompt)*
`/cb arabic word <id> <name>` → anvil → typed/pasted Arabic → chest with **Give blocks** + **Auto-place
a row**. `/cb arabic text <color>` → anvil → single text block (font-drawn, same style).
- Touches: `command/handlers/ArabicCommands`, a small `gui/chest` choice menu, `ArabicBlockRegistry`
  (word render + place-row macro). **No shared-block changes.**
- Tests: **G13.3**, **G13.4**.

## Pass 4 — Auto-joining (live BlockEntity texture) + direction  *(biggest)*
> **Display per [ADR-005](../../adr/ADR-005-auto-join-dynamic-texture-rendering.md), NOT the old
> ADR-003 FORM-blockstate/pack-variant plan.** Forms + facing live on a **letter BlockEntity** drawn
> from a **live in-memory texture** (no pack rebuild / reload). Auto-join uses a **NEW dedicated
> joinable letter block** (the `screen_test` spike graduated), leaving the 1028 `SlotBlock`s untouched.
> Each tile keeps its **own** background colour at the seam. ADR-003's joining *rules* still apply.
> Char→art mapping VERIFIED: ح=`ha`, ه=`ha2`, ط=`ta2`. Build in sub-steps, test between each:

- **4a — Joining brain.** New `arabic/ArabicJoining` (full RTL table + the non-connectors
  ا أ إ آ د ذ ر ز و ؤ ة ى ء, right-only; numbers never join) + the letter BlockEntity storing letter +
  computed form. Proof: a letter shows the correct form alone vs beside neighbours (no render yet).
- **4b — Live render.** Real `ArabicLetterBlockEntity` + a `BlockEntityRenderer` (graduate the
  `screen_test` renderer); isolated = bundled hand-art PNG exactly, connected = arabtype-drawn at bumped
  stroke; texture cache keyed (letter+form+colour), released on break. Proof: 4 forms draw on the block.
  Tests **G13.5–G13.7**.
- **4c — Re-flow.** Bounded place/break updater (sibling of `block/SlotLighting`, ≤2 neighbours, no
  world scan). Proof: place/break re-shapes the line instantly. Test **G13.8** (no diagonal join).
- **4d — Direction + facing.** Per-block `FACING`/axis on the BlockEntity; word stands like a sign,
  multiple words coexist + re-flow. (Readable back = deferred, see ADR-005 amendment.)
- **4e — OmniTool O5.** `OmniToolState.Mode.ARABIC` + `AttackBlockCallback` (left-click captures, not
  breaks): right-click start + left-click target → re-flow that line + remember direction/face for next.
- Touches: new `block/ArabicLetterBlock` + `*BlockEntity`, new `client/render/ArabicLetter*Renderer`,
  new `arabic/ArabicJoining`, new join updater, `core/SlotData`-style letter marker, `CustomBlocksConfig`
  (+ colour-join toggle), `command/handlers/ArabicCommands`, reuse `arabic/ArabicWordRenderer` (pixels).
  **Does NOT touch `SlotBlock`, `ServerPackGenerator`, or `TextureStore`** (the ADR-003 plan did).
- Tests: **G13.5**, **G13.6**, **G13.7**, **G13.8**.

## Pass 5 — Tabbed browser GUI (creatable, like /cb category)
Replace `gui/screens/ArabicBrowserScreen` with a tabbed chest GUI (Letters / Numbers / Words / Recent /
Colors + user-creatable sections) on the `CategoryService` model; colour rail sets grab-colour.
- Touches: new `gui/chest/ArabicBrowserMenu` (+ tabs), `command/handlers/ArabicCommands`/`GuiCommands`
  (`/cb gui arabic` → chest), remove the screen.
- Test: **G13.10**.

---

## File-size gate watch (§9.3)
`ArabicCommands` ≤ 400 (command handler); every other `.java` ≤ 500; `CustomBlocksConfig` ≤ 300 — split
before exceeding. New logic (join updater, form rendering, browser tabs) goes in their own files, not
bolted onto existing ones.

## Out of scope / deferred
Marketplace/vault sharing of Arabic packs; ligatures beyond standard isolated/initial/medial/final;
diacritics/harakat stacking; bidi mixing rules beyond per-run script detection.
