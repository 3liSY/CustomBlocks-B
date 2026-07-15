# Group 10 — Color & Image Tools

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 8 (Color Ecosystem) build-verified.
>
> **Objective:** Restore and improve all color and image manipulation tools: dress (color overlay), gradient, bgstudio (background removal), palette management, exportpng, resize. Add AI-powered background removal, a live recolor slider, and a screen eyedrop color picker.
>
> **Source issues:** Group E (dress, gradient, colors, customcolor, palette, bgstudio, tolerance, exportpng, resize), Q2 (Smart AI background removal), Q3 (Live Recolor & Screen Eyedrop), R5 (ColorVariantService)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| ~~dress~~ | Apply color overlay to existing texture | Missing | **DROPPED** (removed in code — redundant with Colour Variants + live recolour) |
| gradient | Generate blocks between two colors | Missing | Restored |
| bgstudio | Background removal GUI (3 modes: corners, flood-fill, none) | Missing | Restored + improved |
| tolerance | Set background removal tolerance | Missing | Restored |
| palette | Per-player favorite color palettes | `PlayerPaletteManager` stub | Fully wired to GUI |
| exportpng | Export a block's texture as a PNG file | Missing | Restored |
| resize | Resize a block's texture (64px → 128px → 256px) | Missing | Restored |
| AI background removal | None | None | New: `rembg`-style AI segmentation, falls back to classic modes |
| Live recolor | None | None | New: real-time preview slider showing block updating as you drag |
| Screen eyedrop | None | None | New: sample any pixel from monitor to pick a color |
| ColorVariantService | Generated algorithmic variants | `ColorVariantService` stub | Fully wired to Color Tools GUI |
| Background removal modes | Corners Only, Corners + Enclosed, None | Missing | Restored |
| Color distance method | YCbCr | YCbCr (old default) | Switched to CIE Lab (perceptually accurate, per Decision §M) |

---

## What this group covers

| Feature | Commands |
|---|---|
| ~~Dress (color overlay)~~ | **DROPPED** — `/cb dress` removed in code |
| Gradient generator | `/cb gradient <id1> <id2> <steps>` |
| Background removal | `/cb bgstudio <id>` |
| BG tolerance | `/cb tolerance <id> <value>` |
| Palette management | `/cb palette save/load/list/delete <name>` |
| Export PNG | `/cb exportpng <id>` |
| Resize texture | `/cb resize <id> <64|128|256>` |
| Color variants | In Color Tools chest GUI |
| Live recolor | In Color Tools chest GUI |
| Screen eyedrop | In Color Tools chest GUI |
| Colors hub | `/cb colors` |
| Custom color | `/cb customcolor <id> <hex>` |

---

## Implementation Requirements

### 1. ~~Dress (Color Overlay)~~ — DROPPED

`/cb dress` was **removed in code** (`ColorImageCommands.java`: "redundant with Colour Variants").
Color overlay use-cases are covered by **Color Variants** (§10) and **live recolour** (G27). Tests
G10.1 / G10.2 retired.

### 2. Gradient Generator

`/cb gradient <id1> <id2> <steps>` — creates `<steps>` intermediate blocks between the colors of `id1` and `id2`.
- Auto-generates IDs: `gradient_1`, `gradient_2`, … `gradient_N`.
- Uses CIE Lab color interpolation for perceptual accuracy.
- All generated blocks are undoable as a batch.

### 3. Background Removal Studio

`/cb bgstudio <id>` opens the Background Removal chest GUI with 4 modes:

| Mode | Description |
|---|---|
| `corners` | Sample corner pixels as background color, remove matching pixels |
| `flood` | Corners + flood-fill enclosed matching areas |
| `none` | No removal — manual paint only |
| `ai` | AI segmentation (rembg-style) — best results, requires processing time |

`/cb tolerance <id> <0–100>` — set color distance threshold for corner/flood modes. Color distance uses **CIE Lab** (not old YCbCr).

### 4. Palette Management

`/cb palette save <name>` — saves the current color selection as a named palette.
`/cb palette load <name>` — loads a saved palette into the color picker.
`/cb palette list` — lists saved palettes.
`/cb palette delete <name>` — removes a palette.

Persisted per-player. Palettes accessible in all color GUI contexts.

### 5. Export PNG

`/cb exportpng <id>` — saves the block's current texture to `config/customblocks/cloud_exports/<id>.png`. Also posts a clickable chat link served over the mod's HTTP server.

> 🔴 **CROSS-CUTTING (SWEEP_INDEX §A):** the `[download]` link here uses `ResourcePackServer.getPngUrl()` —
> the **same host-leaking + unreachable mechanism flagged in G12** (`http://<host>:<port>/...`). The local
> file write works; the link does not, and it exposes the server address. **Remove IP/host from this link**
> and rethink delivery alongside the G12 fix. The verdict ✅ below predates this decision (2026-06-14) and
> covers only the file write, not the link.

### 6. Resize

`/cb resize <id> <64|128|256>` — resamples the texture to the target resolution. Default texture size is 256px (Decision §L). Server operators can lower this.

### 7. AI Background Removal

Runs as an async background task (does not freeze the server). Uses `rembg`-style segmentation to detect the subject and remove the background. Falls back to `corners` mode if AI processing fails or times out.

Accessible via the `ai` mode in `/cb bgstudio`.

### 8. Live Recolor Slider · 9. Screen Eyedrop · UI medium / Coloring-Screen merge — moved to G27

`/cb recolor <id>` (live recolor) and the screen eyedrop flow are real Screens
(`RecolorSliderScreen`/`EyedropScreen`) — full spec, upgrade plan, and the "everything merges into ONE
Coloring Screen" target (BgStudio/ColorVariants/Colors-hub/Palette/GradientPicker/block-picker) all live in
`GROUP_27_SCREENS.md` §G27.2/§G27.3 (2026-07-12, screen-content consolidation). G10 owns the *logic*
behind each — gradient generation (§2), bgstudio modes (§3), palette persistence (§4), color-variant
algorithms (§10) — none of that moved.

### 10. ColorVariantService

From the Block Editor chest GUI, a "Color Variants" slot opens the Color Variants panel:
- Generates 4–6 algorithmic color variations of the block (lighter, darker, complementary, split-complementary).
- Each variant shown as a preview slot.
- Click a variant → creates a new block using that texture.

---

## Setup

```
/cb create g10a ColorTestBase https://i.imgur.com/example.png
/cb create g10b ColorTestTarget https://i.imgur.com/example2.png
```

---

## Test G10.1 — Dress (color overlay)

```
/cb dress g10a #FF5500 0.4
```

**Expected:** `Applied color overlay #FF5500 (40%) to "g10a".` Block texture has an orange tint. Original texture still visible beneath the overlay.

**Pass:** Texture updated with tint.
**Fail:** Error, no change, or original texture completely replaced.

---

## Test G10.2 — Dress is undoable

```
/cb undo
```

**Expected:** Texture reverts to original (pre-dress).

**Pass:** Undo restores original.
**Fail:** Undo doesn't affect texture.

---

## Test G10.3 — Gradient generation

```
/cb gradient g10a g10b 4
```

**Expected:** 4 intermediate blocks created: `gradient_1` through `gradient_4`. Each one a smooth color step between `g10a` and `g10b`.

**Pass:** 4 blocks created with visually interpolated textures.
**Fail:** Error, wrong count, or no visible gradient.

---

## Test G10.4 — Background removal (corners mode)

```
/cb tolerance g10a 30
/cb bgstudio g10a
```

In the GUI, select "Corners" mode and click "Apply".

**Expected:** Corner-colored background pixels removed (transparent). Block shows subject only.

**Pass:** Background removed in corners mode.
**Fail:** GUI missing, no change applied, or full texture removed.

---

## Test G10.5 — Export PNG

```
/cb exportpng g10a
```

**Expected:** `Texture exported → cloud_exports/g10a.png` with a clickable `[download]` chat link.

Check `config/customblocks/cloud_exports/g10a.png` exists.

**Pass:** File created, clickable link in chat.
**Fail:** Error or file missing.

---

## Test G10.6 — Resize texture

```
/cb resize g10a 128
```

**Expected:** `g10a texture resized to 128×128.` Texture quality visibly higher than 64px.

**Pass:** Texture resampled to 128px.
**Fail:** Error or no change.

---

## Test G10.7 — Palette save and load

```
/cb palette save orange-theme
```

**Expected:** Current color saved as "orange-theme".

```
/cb palette list
```

Shows "orange-theme".

```
/cb palette load orange-theme
```

**Expected:** Palette loaded into color picker.

**Pass:** Save, list, and load all work.
**Fail:** Any step fails.

---

## Test G10.8 — Color Variants panel

Open `/cb editor g10a` → click "Color Variants" slot.

**Expected:** Panel shows 4–6 variant swatches (lighter, darker, complementary, etc.). Clicking one creates a new block.

**Pass:** Variants appear, clicking one creates a block.
**Fail:** Panel missing, or no blocks created on click.

---

## Group 10 Verdict

| Test | Description | Result |
|---|---|---|
| G10.1 | Dress applies color overlay | ❌ removed (replaced by Variants + live recolour) |
| G10.2 | Dress is undoable | ❌ removed |
| G10.3 | Gradient creates interpolated blocks | ✅ in-game (2026-06-14) |
| G10.4 | Background removal (corners mode) works | ⏳ stale ✅ (2026-06-14) — tested via `BgStudioMenu` chest GUI, which the 2026-07-09 UI medium audit reclassified as an unbuilt Screen (merging into one Coloring Screen); needs retest once Screen ships |
| G10.5 | Export PNG saves file with chat link | 🟡 file write ✅ (2026-06-14); **`[download]` link host-leaks + unreachable — same cross-cutting fix as G12 (§A)** |
| G10.6 | Resize resamples texture | ✅ in-game (2026-06-14) |
| G10.7 | Palette save, list, and load work | ⏳ stale ✅ (2026-06-14) — tested via `PaletteMenu` chest GUI, reclassified unbuilt Screen by 2026-07-09 audit; needs retest once Screen ships |
| G10.8 | Color Variants panel creates new blocks | ⏳ stale ✅ (2026-06-14) — tested via `ColorVariantsMenu` chest GUI, reclassified unbuilt Screen by 2026-07-09 audit; needs retest once Screen ships |

**Group 10 + the Coloring redesign confirmed working in-game by the developer (2026-06-14).** Remaining
polish landed on top: client screens (live recolour, eyedrop) return to the previous menu on cancel/Esc.

If anything shows ❌ — paste:
1. The exact command and texture URL
2. What the texture looked like vs what was expected
3. Last 20 lines of `latest.log`

---

## Follow-ups (sweep 2026-06-21)

- **`dress` DROPPED** — removed in code; G10.1/G10.2 retired. Body + tables updated.
- **🔴 G10.5 download link** — shares the cross-cutting host-leak/unreachable bug with G12.2–.5.
  Remove IP/host from the chat `[download]` link and rethink delivery (SWEEP_INDEX §A). Re-test after fix.
- **Ownership** — G10 owns `colors`, `customcolor`, `gradient`, `palette`, `bgstudio`, `exportpng`, `resize`,
  Color Variants, live recolour, eyedrop. The `ai` bg-removal mode ties into G15 (AI, parked) — leave gated.

## Cleanup

```
/cb delete g10a
/cb delete g10b
/cb delete gradient_1
/cb delete gradient_2
/cb delete gradient_3
/cb delete gradient_4
```

---

## G10-1 · Unified `background` Attribute — Transparency + Background Color

> 🔍 designed — **G10-1 and G10-1 are ONE feature**; build together as a single `background` attribute + cutout render layer. Render-layer registration (G14 slice) cross-referenced in GROUP_14_ANIMATION_VIDEO.md.

> *G10-1: "transparent mode doesnt make the blocks transparent actually, needs some serious talk about the logic"*
> *G10-1: "i wanna change all bgs color and need ur help to bulk do that, i also wanna control them like others and triangle" … "all + per selection filter"*

### What this is

Two originally-separate issues unified into one feature (owner 2026-06-24): G10-1 (transparency) and G10-1 (background color control) are the SAME axis — `background = {transparent | black | colour}`. Transparency is not a separate switch; it is one choice of background.

### Why the current state is broken

**G10-1 root — two layers of "no":**
1. The transparent toggle only feeds the off-atlas cache (`AnimSlotBER` / `StaticFrameCache`). After ADR-012 reverted static blocks to the atlas, this path went dead for normal blocks. The toggle has no effect on the most common block type.
2. No `BlockRenderLayerMap` call exists in the mod — every atlas block renders on the **solid** layer, so alpha pixels show opaque/black regardless. `.nonOpaque()` (set in `SlotManager.registerAll`) does not fix this; it controls culling/light, not alpha.

**G10-1 root — background is baked, not stored:**
`ColorToolService.applyBgRemoval` bakes the fill colour into the PNG at creation time. `SlotData` has no `background` field. No value to bulk-set, no lore to show, no clean "change it again later."

### ✅ All decisions (locked 2026-06-23, 2026-06-24, 2026-06-25)

- **ONE attribute:** `background = {transparent | black | colour}` stored on `SlotData`. Not a separate toggle.
- **3 choices:** see-through · solid black · a colour (29-dye palette from `ColorLibrary` + free hex).
- **Default = black** — applied automatically when an image's background is removed / is checkered.
- **Shaped blocks included:** full-cube AND shaped blocks (slabs, stairs, crosses) support the background attribute at launch. Interior-face / culling quirks verified in-game during build.
- **Migration:** `background = black` for all existing blocks with no setting. Existing blocks that had `transparent ON` auto-migrate to `background = transparent`. Silent migration (no confirm prompt).
- **UX — simple toggle/switcher:** single block or bulk selection — no step-by-step guide. Just the control in Studio, command, and bulk.
- **All four surfaces synced** — command · Studio control · bulk op · background tool item — all write the same `SlotData.background` value.
- **Render path (G14 slice):** register slot blocks on the **cutout** render layer (`BlockRenderLayerMap` in `CustomBlocksClient`). Cutout = binary alpha (matches locked meaning) and keeps atlas mipmaps → crispness ADR-012 protected is preserved. This is the single change that makes atlas blocks capable of transparency.
- **Bake the value into the pack texture per block:** `background = black` → composite onto black at bake time; `background = transparent` → keep alpha, cutout shows the world through; `background = colour` → composite onto that colour. All three render correctly with no client toggle race.

### ✅ Already wired (verified 2026-06-24)

The GLOBAL transparent bool is already synced to clients via the existing `TransparentBgPayload` (sent on join + on `/cb config transparent`). Global propagation works today. What is missing: (1) the render layer registration, (2) the per-block `background` field on `SlotData`, (3) the four synced surfaces. The per-block design extends the existing payload — does not build sync from scratch.

**What already exists (G10 Background Studio):** `BgStudioMenu` (chest GUI) + `BgStudioSession` (per-player state: removal mode, tolerance, fill colour — default black) + `ImageToolCommands` + `/cb tolerance` + `BackgroundRemover` + `ColorToolService.applyBgRemoval` + `ColorLibrary`. Fill-colour-at-apply-time already works per block. G10-1's gap = fill not stored on `SlotData`, no bulk, not unified with G10-1.

### Open items (verify in-game when building)

- Shaped-block interior faces — slab/stair/cross models may reveal hollow interiors or odd culling when cutout; verify and handle.
- ~~No-source blocks — repaint-in-place or skip with a warning?~~ **Locked 2026-07-10: skip + chat
  warning.** Simpler, never produces a worse-looking result than what's already there.
- Bulk heaviness caps — **still open, unresolved.** Owner parked the whole bulk-recolor hub (G07-2)
  2026-07-10 rather than pick a confirm threshold, and G10-1's own bulk-background op shares that
  same "this is a heavy re-bake, not a light property flip" question. Until it's picked, treat G10-1's
  bulk-background surface as using the standard `bulkConfirmThreshold` (10) like every other bulk op —
  revisit if that proves too heavy in practice.

> **2026-07-10:** owner confirmed building G10-1 now, as its own standalone fix — independent of
> G07-2 (the bulk recolor hub), which is parked. G10-1's own lighter "bulk op" surface (one of the
> four synced surfaces below) is in scope; the heavy 11-tile recolor hub is not.

| Group | Scope | Status |
|---|---|---|
| G10 — Color & Image Tools (primary) | Both — server re-bake + pack; cutout render is client | 🔍 designed — all decisions locked 2026-06-25; ready to build |
| G14 — Render (render-layer slice) | Client — `BlockRenderLayerMap` registration only | 🔍 designed — cross-ref in GROUP_14 |

**Related:** ADR-008 / ADR-012 (off-atlas vs atlas revert) · G08 (shapes — atlas render layer) · G07-2 (bulk recolor hub that surfaces background bulk) · `ColorLibrary` / `/cb coloring` (dye palette to reuse)
**Touches:** `CustomBlocksClient` (new `BlockRenderLayerMap` cutout registration — G14 work) · `SlotData` (+`background` field) · `ServerPackGenerator` (per-block bake uses stored bg) · `ColorToolService` (re-bake from stored value) · `OffAtlasBgState` / `StaticFrameCache` / `AnimFrameCache` (read per-block flag) · new bulk-bg op in G07 framework · bg color picker GUI (reuse `ColorLibrary`) · `RenderConfigCommands` + `ConfigMenu` (wording update) · `TransparentBgPayload` (extend for per-block)
**Verify in-game:** cutout layer makes transparent blocks actually see-through; black default looks correct on existing blocks; shaped-block cutout has no visual interior-face artifacts; bulk background re-bakes and rebuilds pack correctly; all four surfaces (command/Studio/bulk/tool) write the same value.

---

## G10-3 · BUG — colour-variant recolour leaves trapped / off-edge black (the "edge-black" screenshot, 2026-06-28)

> ⚠️ New, **not built.** Found by owner: HD-retextured `iafc` (black-bg base) → made a **green** variant with
> the Triangle → the green block keeps a **solid black band on one edge** that did not turn green (owner
> screenshot, 2026-06-28). **Documented now; fix scheduled AFTER the §5d Source-Wall re-source job** (GROUP_14)
> per owner: "we will come back to it after the wall fix finishes, keep it in mind."
>
> ✅ **UPDATE 2026-06-29 — candidate #1 (non-square source) FIXED + in-game confirmed.** A non-square logo
> (e.g. `youtube` 960×673) showed **thick black bands top/bottom** on a colour variant. Cause was not the
> ring bug: `ImageProcessor.toBlockPng` squares the image with **transparent padding**, which renders black
> on the solid atlas layer. Fix = fill that padding with the variant colour via `ImageProcessor.fillBackground`
> after `toBlockPng` in `ColorVariantService.createVariant` + `recolorVariants` (from-source branch). Distinct
> from the §G Stage 2e ring-absorb. Candidates #2/#3 (off-tolerance / walled pocket) remain open.

**Root cause (from code — fact, not guess).** `BackgroundRemover.recolorBackground` always runs **EDGES** mode:
it maps `none → EDGES` and **never** uses CLOSED (`image/BackgroundRemover.java:101–149`), called by
`core/ColorVariantService.createVariant` (`:171`) and `recolorVariants` (`:282`). EDGES = a BFS flood seeded
from the **border** pixels. A pixel takes the new colour only if it is **both** (a) within ΔE tolerance of the
corner-sampled background **and** (b) flood-connected to the frame border. So:
- a black pocket **walled off** from the border by the logo → never reached → **stays black**;
- a black region that is **off-tolerance** (a different black / sits over a colour step) → not matched → **stays black**.

This is the **black twin** of the still-open "white blob beside the moon" (GROUP_14 §5c) — the same
flood-connectivity limitation, different fill colour.

**Why a *right-edge band* specifically — 3 candidates (need the owner's actual file to pick one):**
1. **Non-square source** — the uploaded `iafc` picture has a black bar baked into the image (content, not flat
   bg matching the corners) → recolour correctly leaves it. Fix = a clean/square source, **no code**.
2. **Off-tolerance black** — the band is a slightly different black than the sampled corners → raise
   `backgroundtolerance`, or fix the source.
3. **Disconnected pocket** — the logo walls the band off from the seeded border → needs CLOSED-mode recolour.

**Unbuilt fix options (owner decides when we return):**
- **CLOSED-mode recolour** — let `recolorBackground` also fill enclosed / disconnected bg-colour pockets
  (Stage 1b already does this for *removal*; it is **not** wired for *recolour*). Catches #3.
  **Risk:** can over-recolour intended-black **design** pixels — needs in-game check (CLAUDE.md §7: batch
  recolour must stay edge-safe).
- **Raise tolerance** — catches #2; risk of eating dark subject edges.
- **Clean / square source** — fixes #1; no code.

**Fast diagnosis available:** owner sends the baked `iafc_green` PNG (or the `iafc` source) → confirm pocket
vs off-tolerance vs non-square in minutes, then pick the fix.

**Status:** 🟡 partial — candidate #1 (non-square padding bands) ✅ fixed + in-game confirmed 2026-06-29 (see
banner above); candidates #2/#3 still **documented, not fixed** — revisit **after** the §5d wall job finishes.
**Files:** `image/BackgroundRemover.java` (`recolorBackground` / `process`) · `core/ColorVariantService.java`
(`createVariant` / `recolorVariants`).
**Related:** GROUP_14 §5c "white blob" still-open (twin) · GROUP_06 (Triangle/Square recolour surfaces it) ·
GROUP_10_TESTING_GUIDE.md §G · CLAUDE.md §7.

---

## G10-4 · DESIGN — smart thin white KEYLINE replaces the whole-bg white flip (owner ruling, 2026-06-28)

> ⛔ **REVERTED 2026-06-29 — superseded by plain-black fill (§G10-7 below / PROGRESS_LOG keyline-revert).** In-game the
> keyline flooded thin strokes into white blobs and hid solid-dark subjects black-on-black. Owner ruling: content is
> composed on **black backgrounds only** → keyline AND white-flip removed; `BackgroundRemover.process` now always paints
> plain black. The design below is kept for history; the `KEYLINE_*` / `EDGE_DARK_VALUE` constants no longer exist.
>
> 🟢 **Built, build-green, NOT yet in-game confirmed.** Surfaced in **G07 C2** testing: a dark logo / penguin
> on `BgRemove` came back with a **big white rectangle** behind it (Tux turned white-on-white). Owner ruling:
> *"make a white OUTLINE ONLY AROUND THE THING AND MAKE IT SMART, not a big white rectangle … thin but quality
> needs upping … if the image has black, it does this thin thing, if it doesnt, just upload without it."*

**Old behaviour (removed).** `BackgroundRemover.process` smart path flipped the **entire background** to white
whenever the whole subject was near-black **or** ≥45% of the silhouette was dark (`outlineDark`/`EDGE_DARK_FRACTION`).
On a dark subject that meant a full white card — the rejected look.

**New behaviour.** Background is **always BLACK**. A subject that would vanish on black instead gets a **thin
white keyline** that hugs **only its dark edges**:
1. During the existing silhouette scan, every subject pixel that is **on the edge** (touches background) **and
   dark** (HSV value < `EDGE_DARK_VALUE` = 64) is recorded as a **keyline seed** (`darkEdge` mask).
2. **Conditional ("if the image has black"):** the keyline is built **only** when the whole subject is near-black
   **or** at least `KEYLINE_MIN_EDGE_FRAC` (4%) of the silhouette is dark. Otherwise → no keyline, plain black.
   A bright logo with no dark outline goes on black exactly as before.
3. **Thin + quality:** width = `KEYLINE_FRACTION` (1.2% of the image's short side), floor `KEYLINE_MIN_PX` (2 px).
   `BgMask.dilateInto` stamps a **Euclidean disk** per seed → even, **rounded** width (not blocky Manhattan
   growth) that downscales to a clean anti-aliased outline. Owner picked **thin** over medium; "quality up" =
   the disk dilation + the line living at native res through the Lanczos downscale, vs the rough mock.
4. Only the **dark** parts of the silhouette get a line — bright art (white belly, yellow feet, pure-red logo)
   reads on black already and stays lineless. Per-pixel, so one logo can be part-lined, part-plain.

**Scope guard.** Keyline is the **smart-fill (retexture) path only** (`forcedFill == null`). The colour-variant /
BgStudio custom-fill path (`forcedFill` set) is **untouched** — it still paints the chosen colour everywhere,
no keyline. Mode `none` still returns the image unchanged.

**Constants (`BackgroundRemover.java`).** `EDGE_DARK_VALUE` 64 · `KEYLINE_MIN_EDGE_FRAC` 0.04 ·
`KEYLINE_FRACTION` 0.012 · `KEYLINE_MIN_PX` 2. Disk dilation = `BgMask.dilateInto(seed, isBg, w, h, radius)`.

**Known edge case (note, not a bug).** Where the dark subject **touches the image border** there is no background
ring on that side, so no keyline there — the silhouette meets the black at the frame. Rare; flag if owner hits it.

**Status:** 🟢 built + build-green; **awaiting owner in-game confirm** (re-test G07 C2). **Files:**
`image/BackgroundRemover.java` (`process`, smart path) · `image/BgMask.java` (`dilateInto`).
**Related:** G07-TG C2 (where it surfaced) · GROUP_14 small-source blur (the circle-blur in the same screenshot
was a 72 px sample upscaled 7×, **not** this) · CLAUDE.md §2 (build-green ≠ done).

---

## G10-6 · BUG — flattened transparency-checkerboard source bakes into the block instead of black (2026-06-29)

> 🟢 **Built + build-green, NOT yet in-game confirmed (offline-validated on the owner's two real images).**
> Found by owner: `/cb retexture` on two "transparent PNG" links produced blocks showing the **grey/white
> transparency-checkerboard** (and, on colour variants, a **red/yellow/green tinted** checkerboard) instead of the
> expected **solid black** background (owner screenshots: `Share` variants + `Uranus`, 2026-06-29). Owner config at
> report: **`bgremove` (EDGES)**; owner ruling: **"fix it for all modes, unsure which one it was"** → the fix is
> **mode-independent**. Fix direction chosen: **C (auto-handle + warning)**.
>
> Repro URLs:
> - `uranus` ← rawpixel `image_png_800/…neptune…isolat….png`
> - `share`  ← pngtree `…/ourlarge/…share-button….webp`

**Root cause (verified offline — fact, not guess).** Neither URL returns a real transparent image. Both are
**flattened preview exports**: the image editor's transparency-checkerboard is rendered into **opaque** pixels,
no alpha channel.
- `uranus_src.png` → `Format24bppRgb` (**no alpha**); background is an opaque **2-tone checkerboard** of
  `(255,255,255)` and `(238,238,238)`.
- `share_src.webp` → PIL mode **`RGB`** (`has_alpha False`); opaque checkerboard **+ a baked "pngtree" watermark**.

The mod's `BackgroundRemover` is **colour-flood based** (`process` → BFS flood seeded from the corner colour,
CIE-LAB ΔE ≤ `tol`; `image/BackgroundRemover.java:118–159`). A checkerboard defeats it two ways:
1. **Two tones, one seed.** `sampleCornerBackground` takes the **median** of the corner 3×3s → picks ONE tone.
   Clearing the whole checkerboard needs **both** tones within ΔE `tol` of that single seed.
2. **4-connectivity break.** `DIRS` is 4-neighbour only. In a checkerboard, same-tone squares touch only
   **diagonally**; every square's 4-neighbours are the OTHER tone. If the second tone is outside `tol` the flood
   **cannot cross** from one square to the next and dies at the border — the checkerboard survives, bakes into the
   PNG, and renders in-game. `snapBackgroundBlack` then can't help (it only snaps **near-black**, not light grey).

For these two files the two tones are **ΔE ≈ 5.9** apart — just under the default `tolerance=30` ΔE of **6.6**
(`MAX_DELTA_E=22`). So at the **code defaults with an active mode** they *mostly* clear; the fact that the owner
still sees a full checkerboard means the failing instance is running **`backgroundMode = none`** (the code
default in `CustomBlocksConfig.java:102` → no removal at all) **or** a **tolerance below ~27**. ⚠️ **Need from
owner:** `/cb config` background **mode** + **tolerance** to pin which. Note a higher-contrast checkerboard
(e.g. `255`/`204`, ΔE ≈ 18) would survive at **any safe tolerance** — so this is a real robustness gap, not just
a config slip.

**This is the implementation gap of an already-locked decision.** G10-1 (line 326) rules: *"Default = black —
applied automatically when an image's background is removed / **is checkered**."* The "is checkered → black" half
is **not implemented**.

**Twin of:** G10-3 BUG + GROUP_14 §5c "white blob" — the same **flood-connectivity** limitation (a region the
colour flood can't traverse survives), here a 2-tone opaque checkerboard rather than a walled pocket.

**The fix (built — option C, mode-independent).** New self-contained class **`image/CheckerboardDetector.java`**:
- **`flattenToBlack(byte[])`** — detect a flattened checkerboard; if found, edge-flood from **both** tones and
  paint it **opaque black**; else return the bytes unchanged. Hooked as the **first line of
  `BackgroundRemover.process`** (one line), so it runs through `apply` / `recolorBackground` for **every** caller
  (retexture, create, variants, BgStudio) and in **every mode — including `none`** (a checkerboard is never real
  content; implements the G10-1 "is checkered → black" rule). Idempotent with the existing flood that follows.
- **`isFlattened(byte[])`** — same detection, used by `CreationCommands` (create + retexture) to chat the
  **`FLAT_CHECKER_NOTE`** heads-up ("that was a flattened preview … grab the real transparent PNG") = option A's
  warning. This is option **C**.

**How it avoids eating the subject (the hard part).** The checkerboard is **perfectly neutral** (R=G=B, max-min=0)
while a real subject's brights carry **chroma** (the `uranus` limb's near-white is cyan-tinted, max-min ≥ 15). So
the flood gates on a **strict neutrality** check (`max-min ≤ 6`) **plus** ΔE ≤ 12 to either tone, **plus**
edge-connectivity — the planet's white cloud band and the `SHARE` white text/arrow are interior (not border-
connected) **and** tinted, so they survive. Detection is strict to avoid false positives in `none` mode: opaque
(no real alpha) **+** ≥85% light-neutral border **+** exactly 2 dominant luminance tones (each ≥20%, L* gap
2–22) **+** periodic alternation (≥4 tone switches along a border line) = a real checkerboard, not a normal image.

**Offline validation (the proof, since in-game can't be run here).** Ran the exact algorithm + constants on the
owner's two real downloads: `uranus` → planet **fully intact**, checkerboard → black; `share` → red button + white
"SHARE" text + arrow **intact**, checkerboard **and** the baked "pngtree" watermark → black. Both subjects
chroma-protected; corners verified `(0,0,0)`. (Validation script + before/after PNGs were in the session scratchpad.)

**Status:** 🟢 built + **build-green** (monolithGate/Mojibake/Sound pass) + **offline-validated on both real
images**; **awaiting owner in-game confirm** per CLAUDE.md §2 (build-green ≠ done).
**Files:** **NEW** `image/CheckerboardDetector.java` · `image/BackgroundRemover.java` (`process` — +1 line hook) ·
`command/handlers/CreationCommands.java` (`+FLAT_CHECKER_NOTE`, `applyTexture` + `createWithTexture` warning).
**In-game test:** re-run the two repro `/cb retexture` commands → expect **solid black** bg (no checkerboard) +
the heads-up chat line; check a normal transparent-PNG retexture is **unaffected**; check a colour variant of a
checkerboard source comes out solid (not checkered).
**Related:** G10-1 ("is checkered → black" decision, now implemented) · G10-3 BUG (connectivity twin) · GROUP_14
§5c · CLAUDE.md §7. **Note:** the pre-fix root-cause line refs above point at the pre-edit file; the live hook is
the first line of `process`.

---

### G10-6 · UPDATE — edge-flood → **v3 (connectivity-free) + speck-cleanup** (2026-06-29)

> 🟢 **Built + build-green + offline-validated on the actual compiled Java (JDK21). Awaiting in-game confirm.**
> Supersedes the **edge-flood** `flattenToBlack` described above. `detect()` (the strict gate), the public API
> (`flattenToBlack` / `isFlattened`), and the `BackgroundRemover` / `CreationCommands` hooks are **unchanged**.

**Why the edge-flood wasn't enough.** Owner reported, after the edge-flood build, **tiny dots still ringing the
subject** on the black result. Two residues the border-seeded flood can't clear:
1. **Trapped pockets** — checker squares walled off from the image border by the subject (e.g. a gap between
   glyphs) are never reached by an edge BFS, so they bake in.
2. **Off-neutral dots** — webp/jpeg compression tints some checker pixels just past the neutrality gate
   (e.g. chroma 11 > 10), so they survive as scattered light specks on the black.

**v3 algorithm (replaces the `flattenToBlack` body; no flood, no border seeding).**
1. **detect() → toneA, toneB** — unchanged strict gate (real `share` tones ≈ 254/231, `uranus` ≈ 254/238).
2. **Mask** — pixel = opaque **AND** chroma (max−min) ≤ `NEUTRAL_V3` (10) **AND** ΔE ≤ `MASK_TOL_DE` (14) to
   either tone.
3. **Connected components over the mask (4-conn); kill any that genuinely carries BOTH tones** (≥ 8 px of each
   **and** the minority tone ≥ 10 % of the component). Kills border checker **and trapped pockets** alike, since
   it never depends on reaching the border; a single-tone subject white (text, planet body) is **not** bi-tonal,
   so it survives.
4. **Feather** — multi-source BFS out to radius `FEATHER_R` (3) from killed pixels, each carrying the kill
   pixel's original local tone; non-kill pixels in range get the checker bleed peeled by an alpha ramp
   (`RAMP_LO` 6 → `RAMP_HI` 34 ΔE). A **GUARD** keeps any bright-neutral pixel (max > 235 AND chroma < 12) so
   white text edges and the planet limb aren't darkened.
5. **Kill → opaque black.**
6. **Edge-grain cleanup (the dot fix that matters most).** Most dots aren't *isolated* — they are light checker
   fuzz **8-connected to the subject blob** at its outer edge, so a "separate-blob" check can't see them. Take
   connected components of **light-neutral** pixels (max ≥ `SPECK_LIGHT` 170, chroma ≤ `SPECK_CH` 16); kill any
   component that **touches the black exterior AND is ≤ `EDGE_MAX` (200) px**. Measured on the two reals: edge-grain
   components were `share` ≤ 56 px / `uranus` ≤ 79 px, while the subject's light mass is far larger — the `uranus`
   white planet **body is 7 404 px** (and *does* touch black at a limb gap), so the size cap protects it; the
   `SHARE` letters are 2 028–3 308 px **and** never touch black; interior cloud highlights don't touch black either.
   The clean gap (≤ 79 grain vs ≥ 7 404 body) makes the cap safe here.
7. **Speck-cleanup** — mops up any *fully isolated* residue: paint black every non-subject ink blob (8-conn, ink =
   max channel > 24) that is **≤ `SPECK_NOISE` (8) px** (any colour — sub-pixel at block res) **or** **≤ `SPECK_MAX`
   (64) px and ≥ `SPECK_FRAC` (0.5) light-neutral**. The single largest blob (the subject) is always protected, so
   text/arrow/planet can never be touched.

**Offline validation — on the real compiled Java, not just a python model.** Compiled `CheckerboardDetector.java`
with Temurin JDK 21 and ran `flattenToBlack` on the owner's two downloads via `ImageIO` (the mod's own decode
path). **Isolated** residue blobs before cleanup `share` 33 / `uranus` 129 → **0 / 0**. **Edge-grain** (light
components touching black, ≤ 200 px) before `share` 41 / `uranus` 151 → **0 / 0** after step 6 — *and* the `uranus`
planet body (the 7 404 px light component that touches black) is **kept**. Subject stays one mass (`share`
≈ 111 k px, `uranus` ≈ 426 k px); red button + white "SHARE"/arrow + planet limb/cloud detail intact. Verified by
amplified-×4 crops of the whole perimeter (edge ring is clean red→black, no white speckle). (Script + amplified
PNGs in the session scratchpad; `Desktop\cmp_*_dotfix.png`.) Note: the baked **pngtree watermark** (grey-on-red,
interior) is *not* a checker dot and is out of scope — it's a colour-based `BackgroundRemover` matter.

**Status:** 🟢 built + **build-green** (monolithGate/Mojibake/Sound pass; class **400 lines** < 500 gate) +
**offline-validated on the compiled Java** + **deployed** to `.minecraft\mods` + `Desktop\MODS\mods` (old jars
backed up `*.bak_pre_v3edge`); **awaiting owner in-game confirm** (CLAUDE.md §2: build-green ≠ done).
**Files:** `image/CheckerboardDetector.java` (**`flattenToBlack` body replaced** with v3 + speck-cleanup; `detect`
+ LAB/ΔE helpers kept; old edge-flood `seed`/`isBackground` removed). **No change** to `BackgroundRemover.java`
or `CreationCommands.java` (hooks already in place).
**In-game test:** same two repro `/cb retexture` commands → expect **solid black** bg with **no dots/speckle**
around the subject; normal transparent-PNG retexture unaffected; colour variant of a checker source comes out
solid (not checkered). See `GROUP_10_TESTING_GUIDE.md` §G.

---

## G10-CV · `/cb colorvariants` — batch colour-set command (DESIGNED 2026-06-29, NOT built)

> 🔍 **Designed — owner interview complete (2026-06-29). NOT built.** Owner asked for a one-shot command that
> turns one image link into a whole red/green/yellow/black block family, the way the colour Triangles recolour a
> background. Per CLAUDE.md §2 nothing here is done until built + confirmed in-game. Build is split Phase 1 (lean)
> / Phase 2 (heavy).

### What it is
`/cb colorvariants` creates a colour FAMILY of a block from a single image link (or from an existing block's own
texture): a base/black block plus red, green and yellow versions, generated the same way the colour Triangles
recolour a background. One download, one pack rebuild, the whole batch is one `/cb undo`.

### Entry points — command + GUI (owner decision 2026-06-29: BOTH)
The family must be reachable two ways that share **ONE backend rail** (the slice-1 `ColorVariantCommands`
create path), so the command and the GUI can never drift:
- **Command** — ✅ built + confirmed (slice 1, 2026-06-29): `/cb colorvariants <id> <name> <link>` (+ `/cb variants`).
- **Chest GUI / screen** — 🔍 DESIGN-DISCUSS first, NOT built. A "Colour Family" builder reached from the
  Coloring hub (`/cb coloring`) and the Block dashboard: supply a source (image link or an existing block),
  choose the colour set (default red/green/yellow, or extra colours / `all`), preview, Create. Reuse the
  existing chest-GUI primitives (`gui/chest/*`, the Gradient Builder + Colour Variants panel §D patterns) —
  the GUI only **gathers inputs then calls the command rail**, no second backend. This **supersedes** the bare
  "confirm/preview chest GUI" bullet under Phase 2 below.
  ⚠️ Open design questions to settle before any coding: where it lives (hub tile vs Create-Studio tab vs
  standalone screen), how the image is supplied in-GUI (link anvil vs block picker vs both), and whether the
  preview shows all 4 swatches before commit.

### Naming (locked)
- **black / normal = the bare `<id>`** (no suffix) — this IS the normal upload.
- **colours = `<id>_red`, `<id>_green`, `<id>_yellow`** — `ColorVariantService.variantId` suffix scheme.
- Fully compatible with the Square/Triangle swap tools: a Black Square on a coloured variant already falls back
  to the bare `<id>` (`ColorVariantService.swapPlaced`), so the family is live-swappable in-world with no new
  code. ⚠️ Known coexistence: the Black **Triangle** still creates `<id>_black`; that legacy `_black` is a minor
  duplicate of the bare-id black — flagged for a future unify, NOT fixed here. `delete` removes a stray `_black` too.

### Command forms (locked — "two shapes + retexture-first")
- `/cb colorvariants <id> <displayname> <link>` — create a NEW family of 4 from the link. `<displayname>` is a
  quoted string (mirrors `/cb create`), `<link>` is greedy. The display name is asked for / used ONLY when the
  block is new.
- `/cb colorvariants <id>` — `<id>` already exists → rebuild its 3 colours from the block's OWN stored texture
  (source if present, else the baked PNG), keeping its current display name. If `<id>` does NOT exist → error
  telling the player to pass a display name + link.
- **Re-image an existing family:** `/cb retexture <id> <link>` then `/cb colorvariants <id>`.
- `/cb colorvariants delete <id>` — delete the WHOLE family: `<id>` + `<id>_red/_green/_yellow` (+ a legacy
  `<id>_black` if one exists). Held behind `/cb confirm`; the prompt lists exactly what will be deleted.
- **Optional extra colours:** a trailing colour list or the word `all` extends the set beyond the 4 — e.g.
  `/cb colorvariants <id> <name> <link> blue,gold,purple` or `… all` (the full 29-colour `ColorLibrary`).
  Default (no list) = red/green/yellow + bare-id black. Extra colours use `ColorLibrary` slugs + free hex.
- alias: **`/cb variants`** for every form above.

### Source & override rules (locked)
- The 3 colours are ALWAYS derived from the base `<id>`'s normal (black) texture. So `<id>` itself is re-baked
  ONLY when a link is supplied; the id-only form leaves `<id>` untouched and just (re)generates the 3 colours.
- One download is reused for all colours. **2026-07-01 safety update:** the base stores the original upload, while
  each coloured variant stores its own baked colour PNG as its source. This keeps variants resize/repaint-safe
  without duplicating one large raw upload 4x on disk.
- **Override:** if any target id already exists, the command holds on `/cb confirm`, the prompt lists which ids
  will be overwritten, and on confirm it overwrites the TEXTURE/COLOUR ONLY — it KEEPS each block's glow,
  hardness, sound, collision, category and favorite state (one snapshot copy, like `ColorVariantService`).

### Crash-safety follow-up (2026-07-01 — built, awaiting owner test)
Owner report/logs: sometimes `/cb colorvariants` / overwrite ends in a timeout. The supplied server
`latest.log` completes two family overwrites (`tea`, `netflix`), then for `sun` stops after:
`[CustomBlocks-PackBuilder/INFO]: [CustomBlocks] Rebuilding resource pack…`. The PC log shows the client stayed
alive and disconnected with `Timed out`, with no new client crash report. That points at the server/resource-pack
pipeline stalling or being killed after the command succeeded, not a Brigadier parse crash.

Patch made:
- `image/ImageLimits.java` added a colour-family source guard: max 12 MB download body, max 4096 px on a side,
  max 12 MP. Oversized images fail before any slots/textures are changed.
- `ImageDownloader` now streams image responses with a byte cap instead of accepting unbounded bodies into memory.
- `ColorFamilyOps` uses the guarded colour-family download path for create and overwrite.
- Variant source storage changed as described above: base keeps the original upload; colour variants keep their
  baked colour PNG, avoiding 4x duplication of a huge raw source.
- `ServerPackGenerator.generate` now logs `Pack zip emit start` and `Pack zip emit done` with assigned count,
  maxSlots, texture size, pack KB, and elapsed ms. If the server still dies, the next log should identify whether
  it stopped before or during ZIP emission.

Separate log finding, not fixed here: the PC client registered `maxSlots=1500`, while the server registered
`maxSlots=1400`. That explains the client warnings for `slot_1400` through `slot_1499` missing models after pack
reloads. It did not produce the final timeout in this log, but the configs should be aligned before judging pack
reload noise.

### Safety (locked)
- The whole batch (create-family, override-family, or delete-family) is ONE `/cb undo` — `UndoManager.recordBatch`.
- Locked target blocks are SKIPPED (left untouched); the command continues with the rest and reports which were
  skipped (`LockManager.isLocked`).
- A per-colour summary line at the end: which ids were created, which overwritten, which skipped.

### Cross-system sync (owner requirement — "synced to all cmds system")
Wire the feature into EVERY relevant shared system, consistent with the rest of the mod:
- **Advancements/achievements** — count the family toward stats + a new "Colour Master" achievement on the first
  completed family (`AchievementManager`).
- **Sounds + particles** — a completion burst on family finish, gated by the per-event category toggles
  (`bulk_complete`).
- **Coloured names** — each variant's display name renders in its own colour in chat + HUD (§-codes).
- **HUD** — `HudSync.broadcast` after the batch so every new block has a live HUD.
- **Resource pack** — exactly ONE `ResourcePackServer.updatePack()` + `syncToAll()` after the whole batch (§7).
- **Undo / Incidents / Locks** — batch undo; `IncidentRecorder` on any failure; locks respected.
- **Categories** — same create-time category hint as `/cb create` (`CategoryCommands.suggestOnCreate`); no forced
  grouping (owner declined auto-group).
- **DidYouMean / Help** — register the command + alias so typos suggest it and `/cb help` lists it.
- **Config** — honours `backgroundMode`, `backgroundTolerance`, `textureSize`, `silentPack`; the 4 colours read
  `triangle{Red,Green,Yellow}Hex`; extra colours read `ColorLibrary`.
- **Vault/Marketplace** — share-as-code (Phase 2).

### Phase split (locked — "lean Phase 1, heavy stuff Phase 2")
**Phase 1 (lean — ship + test first):** the command (create / override / delete forms + `/cb variants` alias),
hand-the-player-all-blocks, optional extra colours / `all`, completion sound+particles, coloured names, the
"Colour Master" achievement, batch undo, skip-locked, per-colour summary, and the FULL cross-system sync above.

**Phase 2 (heavy — after Phase 1 is green in-game):** poster PNG (`/cb colorvariants poster <id>` → one grid
image to `cloud_exports/`, reusing `/cb exportpng` delivery — mind the G10.5 host-leak link caveat), share-as-code
(`/cb colorvariants share <id>` → one Cloud Vault code that recreates the family), hologram preview before commit
(BLOCKED on the `/cb preview` hologram system existing first), the **Colour Family chest GUI / screen** (now a
co-equal entry point — see "Entry points — command + GUI" above; design-discuss before building), and a
`/cb colorvariants list` family dashboard. Animated-GIF colour variants also live in Phase 2 (heavier).

### Inherited limitation
The recolour path is `ColorVariantService` → `BackgroundRemover.recolorBackground`, so this command inherits the
open **G10-3** trapped/off-edge-black bug on some images. It improves automatically when G10-3 is fixed.

### Reuse map (build from these — do NOT rewrite)
- `core/ColorVariantService` — `createVariant` recolour rail, `variantId` / `stripColourSuffix` naming,
  `recolorVariants` config-hex resync (the family auto-repaints when `triangleRedHex` etc. change later).
- `command/handlers/CreationCommands.createWithTexture` — the download-off-thread / create-only-if-download-ok pattern.
- `command/handlers/BulkConfirm` — `/cb confirm` / `/cb cancel`, 60s window.
- `image/BackgroundRemover` + `image/ImageProcessor` (`toBlockPng`, `fillBackground`) + `image/CheckerboardDetector`.
- `core/TextureStore`, `core/SlotManager`, `core/UndoManager`, `core/LockManager`, `network/ResourcePackServer`,
  `network/HudSync`, `core/onboarding/AchievementManager`, `command/Chat`, `gui/ColorLibrary`.

### Files (planned)
- **NEW** `command/handlers/ColorVariantCommands.java` (keeps `ColorImageCommands.java` — 315 lines — under the
  400-line handler gate). Registered in `command/CommandRegistrar`.
- Likely a small `core/ColorVariantService` addition for a "build the whole family from one source" rail (the
  existing `createVariant` is per-colour from an existing block; the from-link/batch rail is new but reuses the
  same recolour internals).

### In-game tests (add to GROUP_10_TESTING_GUIDE.md when built — Phase 1)
- Fresh: `/cb colorvariants logo MyLogo <link>` → `logo` + `logo_red/_green/_yellow` exist, coloured correctly.
- Names: each shows its colour in chat/HUD; base "MyLogo", variants "MyLogo Red/Green/Yellow".
- Id-only regen: edit `logo`, run `/cb colorvariants logo` → colours rebuilt from its texture, name kept.
- Override: re-run on an existing family → `/cb confirm` lists ids → settings (glow etc.) survive.
- Delete: `/cb colorvariants delete logo` → `/cb confirm` → whole family gone; one `/cb undo` restores it.
- Skip-locked: lock `logo_red`, re-run → it's skipped, others proceed, summary names it.
- Extra colours: `… <link> all` → full palette family; `… blue,gold` → just those.
- Swap-tool compat: place `logo`, hit with a Red Square → becomes `logo_red`.
- Juice: completion sound + particles fire; "Colour Master" unlocks on first family.

**Status:** 🔍 designed, NOT built. **Related:** GROUP_06 (Triangle/Square recolour + swap) · G10-3 (inherited
edge-black) · G10.5 (poster-PNG download-link caveat) · GROUP_15 (hologram/AI parked) · the colour-styles idea below.

---

## G10-Styles · Colour STYLES for all creation paths (IDEA 2026-06-29 — documented, not scheduled)

> 💡 **Idea captured (2026-06-29), home = Group 10 (owner decision). NOT scheduled, NOT built.** During the
> `/cb colorvariants` interview the owner saw four alternate colouring LOOKS and ruled they should NOT live in
> colorvariants — instead become a shared option across ALL block-creation commands.

### What it is
A `style=` modifier (or chest-GUI choice) that changes HOW colour/texture is applied at creation time, available
to every creation path — `/cb create`, `/cb retexture`, `/cb colorvariants`, `/cb arabic word`, and future ones.
The default stays today's behaviour (background recolour / snap-to-black). The styles:
- **Background recolour** (default) — background becomes the colour, subject untouched (`BackgroundRemover.recolorBackground`).
- **Tint / dye** — the SUBJECT itself is dyed the colour, shading preserved. Reuses `image/ColorReplacer.tint`
  (already used for tool art). Cheapest add.
- **Duotone** — shadows → colour, highlights → white; a poster/risograph look. New `ColorMath` function.
- **Smart palette** — read the image's dominant colour (`ColorMath.averageColor`) and auto-build its
  complementary / analogous / lighter / darker set. This is the unbuilt Group 10 §10 "ColorVariantService panel" design.
- **Shade spectrum** — N shades of ONE colour, light→dark; reuses `ColorMath.labLerp` (the `/cb gradient` maths).

### Why it's cross-cutting
The owner wants the SAME look options everywhere a block gets a texture, not bolted onto one command. So it
belongs as a shared service (e.g. a `CreationStyle` enum + one apply point in the image pipeline) that each
creation command passes through — including `/cb arabic word` (style a generated word texture) and the studio.

### Status & sequencing
💡 idea only — NOT scheduled, NOT built. Slots AFTER `/cb colorvariants` Phase 1/2 and likely alongside a broader
creation-pipeline pass. Cross-refs: `/cb colorvariants` (above), Group 10 §10 (ColorVariantService panel),
`/cb arabic word` (creation path), `image/ColorReplacer`, `image/ColorMath`, `image/BackgroundRemover`.

---

## G10-BRH · Bulk Recolor Hub (`/cb bulkrecolor`) — MOVED here from Group 07 (2026-07-12)

> **Provenance:** this was `§G07-2` in `GROUP_07_BULK_OPERATIONS.md`. Owner moved it to Group 10 on
> 2026-07-12 — it is a bulk *front-door* whose engines all live here (G10-1 `background` + the merged
> Coloring Screen) and in Group 06 (re-bake). It is not Bulk-Ops-logic, so it leaves G07 entirely.
>
> 🧊 **PARKED — spec fully locked, nothing built.** Gated on **G10-1 (`background` engine)** + **G06-4
> (re-bake engine)**; build both first — this hub is the front-door on top of them. Per CLAUDE.md §2
> nothing here is done until built + confirmed in-game.

### What this is

Do one recolor/tone/background op to *many* blocks at once. The bulk framework to host it is ready
(`BulkScope` + `BulkConfirm` + one-undo batch + the `RetextureAll` pattern). WHAT "recolor" means is
decided (11 tiles below). No bulk recolor/background op exists anywhere today (verified: zero
`bulk*recolor` / `bulk*background` / `bulk*color` literals).

### ✅ Decisions (locked 2026-06-24 / 06-25)

**GUI: slider screen** — reuse the merged **Coloring Screen** (Group 10), not a new screen.

**11 locked tiles:**

| # | Tile | Engine |
|---|---|---|
| 1 | **Tint to colour** | bulk-push toward dye/hex; reuse `ColorVariantService` / `ColorReplacer.tint` |
| 2 | **Tone** (HSL: lighter / darker / vivid) | reuse `RecolorToneTools` |
| 3 | **Background** (transparent / black / colour) | G10-1 engine (`SlotData.background`), surfaced here |
| 4 | **Palette-swap (A→B)** | find colourA → make colourB across selection; `ColorReplacer.swapColor` per block |
| 5 | **Saved presets + live preview** | save a recipe; preview on sample blocks before committing |
| 6 | **Desaturate / Greyscale** | strip all colour from selected blocks |
| 7 | **Invert colours** | flip every colour to its negative |
| 8 | **Hue shift** | rotate hue by N degrees via slider |
| 9 | **Gradient across selection** | colourA→colourB across block positions; direction picker |
| 10 | **Colour temperature shift** | warmer / cooler via slider |
| 11 | **Saturate / Vivify** | boost saturation without changing hue, via slider |

**Candidate (not this round):** per-block randomised tint.

**Heavy TEXTURE batch:** re-bake each matched PNG, one debounced pack rebuild at the end, one undo step.
Model on `RetextureAll`; reuse the G10-1/G06-4 re-bake-from-source engine.

**Edge-mode only (safety, moved from G07 §5):** colour resampling runs in **edge mode** only. Full-mode
recolor stays blocked to prevent design destruction — hardcoded, no config option enables it on a bulk op.

**UX — confirm + progress + preview + cancel:**
- Confirm dialog over threshold: "This will re-bake N blocks — proceed?".
- Progress bar while re-baking (not a black screen).
- Live sample preview BEFORE confirm: 3-4 sample blocks already recoloured.
- Cancel mid-op: stops after current block, auto-undoes already-baked blocks.
- Completion feedback: sound + chat ("Re-baked 87 blocks — done").

### 🔒 Design locked 2026-07-11 — "Recoloring Hub"

Mockup: https://claude.ai/code/artifact/377729aa-4faa-4a59-b2a7-7138a4b10a2f (Recoloring Hub tab = build target).

- **Layout = Tiered Console shape.** Primary row (5): Hue, Saturation, Lightness, Tint Overlay, Preset Swap.
  Secondary row (6): Gradient Map, Random/Block, Per-Face, Pattern/Noise, Live Preview, Reset.
- **Theme = dark red/black**, matching the Bulk Operations Hub — not teal/aqua.
- **Confirm threshold N = 2** — almost every bulk recolor job gates the confirm dialog.
- **Confirm dialog = "Rebake Confirmation"**, in-hub chrome, live block + chunk count, type-to-confirm
  ("DEPLOY") gate before Deploy unlocks.

### Open items (both resolved)

- ~~Threshold N~~ **Locked: N = 2.**
- ~~Gradient direction~~ **Locked:** full picker (left→right / top→bottom / centre).

**Related:** G10-1 (background engine — tile 3) · G06-4 (re-bake engine + colour tools) · `RetextureAll` ·
`ColorLibrary` (29-dye palette) · `RecolorSliderScreen` · `BulkScope` / `BulkConfirm` (the G07 framework it
calls). **Verify in-game:** all 11 tiles function; confirm fires over threshold; cancel mid-batch undoes;
completion sound + chat fires; sliders update preview.
