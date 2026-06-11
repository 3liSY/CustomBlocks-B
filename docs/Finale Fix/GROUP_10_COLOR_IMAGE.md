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
| dress | Apply color overlay to existing texture | Missing | Restored |
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
| Dress (color overlay) | `/cb dress <id> <hex-color> <intensity>` |
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

### 1. Dress (Color Overlay)

`/cb dress <id> <hex-color> <intensity>` — blends a solid color on top of the existing texture.
- `<intensity>` = 0.0–1.0 (0 = no change, 1 = full color replacement).
- Operates on the server-side PNG. Undoable (stores pre-dress PNG).
- Example: `/cb dress myblock #FF0000 0.3` — slight red tint.

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

`/cb exportpng <id>` — saves the block's current texture to `config/customblocks/cloud_exports/<id>.png`. Also accessible via a clickable chat link that serves the file over localhost HTTP.

### 6. Resize

`/cb resize <id> <64|128|256>` — resamples the texture to the target resolution. Default texture size is 256px (Decision §L). Server operators can lower this.

### 7. AI Background Removal

Runs as an async background task (does not freeze the server). Uses `rembg`-style segmentation to detect the subject and remove the background. Falls back to `corners` mode if AI processing fails or times out.

Accessible via the `ai` mode in `/cb bgstudio`.

### 8. Live Recolor Slider

In the Color Tools chest GUI: a color slider that shows the block texture updating in real-time as the hue/saturation/brightness values change. Click "Apply" to commit.

### 9. Screen Eyedrop

In the Color Tools chest GUI: a "Pick from screen" button. When clicked:
- The GUI closes temporarily.
- The cursor changes to an eyedrop cursor.
- Player clicks any pixel on their screen.
- The sampled color is returned to the Color Tools GUI.

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
| G10.1 | Dress applies color overlay | ⬜ |
| G10.2 | Dress is undoable | ⬜ |
| G10.3 | Gradient creates interpolated blocks | ⬜ |
| G10.4 | Background removal (corners mode) works | ⬜ |
| G10.5 | Export PNG saves file with chat link | ⬜ |
| G10.6 | Resize resamples texture | ⬜ |
| G10.7 | Palette save, list, and load work | ⬜ |
| G10.8 | Color Variants panel creates new blocks | ⬜ |

**Group 10 passes when all color and image tools work in-game.**

If anything shows ❌ — paste:
1. The exact command and texture URL
2. What the texture looked like vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g10a
/cb delete g10b
/cb delete gradient_1
/cb delete gradient_2
/cb delete gradient_3
/cb delete gradient_4
```
