# Group 10 - Color & Image Tools

## Status

| | |
| --- | --- |
| **Verdict** | Core image commands are confirmed; background routes and the advanced `/cb bulkrecolor` Hub need later discussion. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-06-29 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| D | Checkerboard-source flattening and speck cleanup | Built 🎯 | Discussion ✏️ |
| E | Colour-variant edge/off-edge black cleanup | Built 🎯 | Discussion ✏️ |
| B | Background Studio, palette, and coloring engine flows | Built 🎯 | Polish 🎨 |
| G | Advanced `/cb bulkrecolor` Hub | Planned 📜 | Discussion ✏️ |
| C | Unified `background` attribute | Designed ⏳ | Discussion ✏️ |
| F | `/cb colorvariants` family command and future GUI | Designed ⏳ | - |
| A | Resize, export PNG, and gradient command basics | Done ✅ | - |
| H | AI background removal | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_10_COLOR_IMAGE.md](../groups/GROUP_10_COLOR_IMAGE.md)

---

# Active Tests

## 💡 Setup

- Create `g10a` and `g10b` from real image URLs with visibly different colors.
- Keep one flattened checkerboard source image and one normal transparent PNG available.
- Cross-check screen-only failures in G27 when the UI is live recolor, eyedrop, or the unified Coloring Screen.

## B - Background Studio, palette, and coloring engine flows - Built 🎯

| | |
| --- | --- |
| **Check** | G10 engines still perform background removal, fill color, tolerance, palette, and color-variant operations while final screens move through G27. |
| **Pass rule** | Background, fill color, tolerance, palette persistence, variant creation, and undo rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Apply Background Studio background-only with black fill. | Background becomes black and `/cb undo` restores. | 🎯 | 🎯 |
| B2 | Change fill color to red or a hex value and apply. | Removed background bakes to that color. | 🎯 | 🎯 |
| B3 | Run `/cb tolerance 60` and `/cb tolerance 60 g10a`. | Global and per-block tolerance behave separately and persist. | 🎯 | 🎯 |
| B4 | Save, load, and delete a palette. | Palette state persists and appears as a reusable color source. | 🎯 | 🎯 |
| B5 | Create a color variant from the panel/command path. | Variant is generated from the source and is undoable. | 🎯 | 🎯 |

## C - Unified `background` attribute - Designed ⏳

| | |
| --- | --- |
| **Check** | Background becomes one stored per-block attribute: transparent, black, or color. |
| **Pass rule** | Single, selected, all-block, command, Studio, and tool surfaces write the same value and render correctly on full and shaped blocks after discussion is locked. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Exact commands, Screen controls, selection flow, tool behavior, migration mechanics, edge cases, limits, confirmation, cancel/rollback, and undo remain Discussion ✏️ for later. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Set a block background to transparent. | Atlas and off-atlas blocks become actually see-through where alpha exists. | 🎯 | 🎯 |
| C2 | Set a block background to black. | Texture composites onto solid black and records that value on the block. | 🎯 | 🎯 |
| C3 | Set a block background to a color from the 29-dye palette or hex. | Texture composites onto that color and survives restart. | 🎯 | 🎯 |
| C4 | Apply the same background value to a shaped block. | Slab/stair/cross visuals do not show broken interior-face artifacts. | 🎯 | 🎯 |
| C5 | Bulk-change background on a small selection. | Each selected block re-bakes, pack rebuilds once safely, and no-source blocks are skipped with a warning. | 🎯 | 🎯 |
| C6 | Apply one background choice to all blocks through the future approved route. | Every eligible block receives the same stored background value; skips and failures are reported honestly. | ⏳ | ⏳ |
| C7 | Use the future approved background tool on one block. | The tool writes the same stored background value as command, Studio, selected, and all-block routes. | ⏳ | ⏳ |

## D - Checkerboard-source flattening and speck cleanup - Built 🎯

| | |
| --- | --- |
| **Check** | Opaque transparency-checkerboard preview images are detected and flattened to black before normal background processing. |
| **Pass rule** | Owner checkerboard images, color variants, and normal transparent PNG controls pass twice in-game. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Create/retexture from a flattened checkerboard source. | Checkerboard pixels become black; subject remains intact. | 🎯 | 🎯 |
| D2 | Make color variants from that source. | Tinted checkerboard does not survive into red/green/yellow variants. | 🎯 | 🎯 |
| D3 | Use a real transparent PNG. | It remains unaffected by checkerboard flattening. | 🎯 | 🎯 |
| D4 | Inspect tiny edge specks around the subject. | Speck cleanup removes small checker dots without eating real design pixels. | 🎯 | 🎯 |

## E - Colour-variant edge/off-edge black cleanup - Built 🎯

| | |
| --- | --- |
| **Check** | Color variants do not leave avoidable black bands or trapped background when the source can be safely cleaned. |
| **Pass rule** | Non-square padding fix is still confirmed, and remaining trapped-pocket candidates are classified honestly. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Create a variant from a non-square source. | Transparent padding is filled with the variant color instead of black bands. | 🎯 | 🎯 |
| E2 | Test an image with a disconnected black pocket. | Result is classified as source content, closed-mode candidate, or true bug with screenshot evidence. | 🎯 | 🎯 |
| E3 | Retest the owner edge-black screenshot source if available. | The exact old failure either stays fixed or records what still remains. | 🎯 | 🎯 |

## F - `/cb colorvariants` family command and future GUI - Designed ⏳

| | |
| --- | --- |
| **Check** | One source can create or refresh a full color family safely, with one undo and clear summary. |
| **Pass rule** | Fresh create, id-only regen, delete, overwrite guard, extra colors, and size limits pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Command slice exists; GUI/screen builder is still design-discuss. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb colorvariants logo MyLogo <link>`. | `logo`, `logo_red`, `logo_green`, and `logo_yellow` exist with correct names/colors. | 🎯 | 🎯 |
| F2 | Edit `logo`, then run `/cb colorvariants logo`. | Existing color variants rebuild from the base block's stored texture. | 🎯 | 🎯 |
| F3 | Run `/cb colorvariants delete logo` and confirm. | Whole family is removed and one undo restores it. | 🎯 | 🎯 |
| F4 | Try an oversized image. | Command fails before slot or texture state mutates. | 🎯 | 🎯 |
| F5 | Open the future family GUI when built. | GUI/screen supplies source, color set, preview, and create flow without duplicating command logic. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Resize, export PNG, and gradient command basics — ✅ `2026-06-15`
- §E Non-square color-variant padding fix — ✅ `2026-06-29`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

- §G Advanced `/cb bulkrecolor` Hub — 📜 `2026-07-18`: A separate advanced Hub is locked; selection, operations, layout, arguments, safety, progress/cancel behavior, undo, and `/cb bulk` integration need later Discussion ✏️.

</details>

<details><summary>💤 <b>Parked</b></summary>

- §H AI background removal — 💤 `2026-07-09`: Tied to G15 and gated separately.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- `/cb dress` — 👎 `2026-06-15`: Replaced by Color Variants and live recolor.
- Whole-bg white keyline direction — 👎 `2026-06-29`: Reverted and superseded by plain-black fill.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g10a`, `g10b`, color-family blocks, and gradient blocks after testing.
- [ ] Keep screen-only findings in G27.
- [ ] Keep AI background-removal findings in G15 unless they affect the shared image engine.
- [ ] Do not move Bulk Recolor Hub back to G07.

</details>
