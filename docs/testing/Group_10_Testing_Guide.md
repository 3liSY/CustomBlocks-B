# Group 10 — Color & Image Tools

**🎯 Active:** CVS2 only (CVS1/3/4 passed) · G (0/3) · CV/CV-R/CV-O/A-D/E/F/H rebuilt + verified in source (0/25)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | ✅ CVS1/3/4 pass, CVS2 open (test links added). Rest awaiting in-game test — one gap: colorvariants delete not built (CV3). |
| **Progress**    | ✅✅✅🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 9% (3/32 passed) |
| **Last tested** | 2026-07-12                      |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> Live recolor slider + eyedrop + the Coloring-Screen merge live in `GROUP_27_SCREENS.md` §G27.2/§G27.3.

## 🗺️ Sections

| §    | What                                                    | State |
| ---- | ------------------------------------------------------- | ----- |
| CV-S | `/cb colorvariants` crash-safety guard                  | 🎯 CVS2 open, rest ✅ 2026-07-12 |
| G    | Recolour leaves trapped/off-edge black (edge-black bug) | 🎯 not tested |
| BG-X | Investigation: photo bg-removal leaves ragged white blob | ❔ discuss |
| CV   | `/cb colorvariants` colour-family CREATE form           | 🎯 not tested |
| CV-R | `/cb colorvariants <id>` — rebuild family               | 🎯 not tested |
| CV-O | `/cb colorvariants` — overwrite family                  | 🎯 not tested |
| A    | `/cb resize` · `/cb exportpng`                          | 🎯 not tested |
| B    | `/cb gradient` · Gradient Builder                       | 🎯 not tested |
| C    | `/cb bgstudio` · fill colour                            | 🎯 not tested |
| D    | Colour Variants panel                                   | 🎯 not tested |
| E    | `/cb palette` · `/cb coloring` hub                      | 🎯 not tested |
| F    | Live recolour slider · screen eyedrop                   | 🎯 not tested |
| H    | Non-square logo variant: padding bands                  | 🎯 not tested |

🔗 **Related Doc:** [GROUP_10_COLOR_IMAGE.md](../groups/GROUP_10_COLOR_IMAGE.md)

---

# 🎯 Test now

## CV-S · Colorvariants crash-safety · 🎯 CVS2 open, rest ✅ 2026-07-12

> 💡 Rejects too-large colour-family sources before commit and adds pack ZIP log lines to prevent silent timeouts.
> 🧰 `/cb colorvariants fam MyFam <link>`

| # | Action                                                                                         | Expected Result                                                                            | SP | MP |
| --- | --- | --- | --- | --- |
| CVS2 | Try an oversized direct image (>12 MB or >4096px side) with `/cb colorvariants big Big <link>` | friendly "too large" error; **no slots created**, no pack rebuild, no timeout — test links: [learningcontainer 15MB](https://www.learningcontainer.com/wp-content/uploads/2020/07/Large-Sample-Image-download-for-Testing.jpg) · [examplefile 150MB](https://www.examplefile.com/image/jpg/150-mb-jpg) | 🎯 | 🎯 |
| CVS1 | Existing normal family: `/cb colorvariants fam MyFam <small-link>` → `/cb confirm`             | family overwrites normally; no disconnect; server log shows `Pack zip emit start` / `done` | ✅ | ✅ |
| CVS3 | After CVS1, run `/cb undo`, then redo/overwrite again with another small image                 | old pictures restore, second overwrite works                                               | ✅ | ✅ |
| CVS4 | Check server + PC logs after CVS1-CVS3                                                         | no Java client crash report; no server log ending only at `Rebuilding resource pack…`      | ✅ | ✅ |

> ⚠️ CVS4 log-check covered CVS1/CVS3 only — once CVS2 (oversized image) is run, re-check logs for that
> case too (should show the "too large" refusal, no partial rebuild attempt).

## G · Recolour edge-black bug · 🎯 not tested

> 💡 Fixes the solid black band left on the edge when recolouring HD sources with black backgrounds.
> 🧰 Triangle-recolour a logo/subject base (e.g. `iafc` → green)

| # | Action                                                                                  | Expected Result                                                                       | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Triangle-recolour a logo/subject base, place it, look at the subject edge               | new colour reaches the subject edge — **no black band/ring** left hugging the subject | 🎯 | 🎯 |
| G2 | On that same recoloured block, look at any **intended black design** inside the subject | design black is **kept** (no colour bleeding into the logo's dark parts)              | 🎯 | 🎯 |
| G3 | Check the **original** base block you recoloured                                        | unchanged — the recolour made a **new** slot                                          | 🎯 | 🎯 |

## BG-X · Investigation — photo bg-removal leaves ragged white blob · ❔ discuss

> Not a bug row, no code change made yet.

`Cat` block from a Wikipedia photo link came out with a ragged white blob + black speckle around it; a clean
transparent-PNG source (`samosa`) looked fine same session. Cause: background removal is colour-flood based
— it handles a clean transparent PNG fine, but a photo with a shadowed/non-uniform background defeats the
flood (parts survive as a white blob, some speckle falls within tolerance and goes black). Same root cause
as the GROUP_14 "white blob beside the moon" finding.

**Try now, no code:** `/cb config background BgSmart` (keeps only the largest subject) · adjust
`/cb config backgroundtolerance` · or use a clean transparent-PNG source instead of a photo.

**Real fix** (non-colour subject isolation) is a bigger job — discuss before building.

---

## CV · `/cb colorvariants` colour-family CREATE form · 🎯 not tested
> 💡 One image link → a colour family: bare `<id>` (black) + `<id>_red/_green/_yellow`. Alias `/cb variants`.
> 🧰 none, works from a fresh link.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| CV1 | `/cb colorvariants logo MyLogo <link>` | creates `logo` + `logo_red/_green/_yellow`, each coloured correctly | 🎯 | 🎯 |
| CV2 | check chat/HUD names | base shows "MyLogo", variants show "MyLogo Red/Green/Yellow" in their colour | 🎯 | 🎯 |
| CV3 | `/cb colorvariants delete logo` | ⚠️ **not built** — replies "not built yet, use `/cb delete` + `/cb undo`", not a real delete | 🎯 | 🎯 |
| CV4 | place `logo`, hit with a Red Square (swap tool) | becomes `logo_red` (compat via `ColorVariantService.swapPlaced`) | 🎯 | 🎯 |

## CV-R · `/cb colorvariants <id>` — rebuild family · 🎯 not tested
> 💡 `<id>` already exists → rebuilds its 3 colours from the block's own stored texture, keeps display name.
> 🧰 an existing block, e.g. from CV1.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| CVR1 | edit `logo`'s texture (`/cb retexture logo <link>`), then `/cb colorvariants logo` | 3 colours rebuild from the new texture; name unchanged | 🎯 | 🎯 |
| CVR2 | `/cb colorvariants doesnotexist` | error telling you to pass a display name + link | 🎯 | 🎯 |

## CV-O · `/cb colorvariants` — overwrite family · 🎯 not tested
> 💡 Re-running the CREATE form on an existing family holds on `/cb confirm` before overwriting.
> 🧰 an existing family, e.g. from CV1.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| CVO1 | `/cb colorvariants logo MyLogo <new-link>` on an existing family → `/cb confirm` | prompt lists ids to overwrite; on confirm, texture/colour updates but glow/hardness/sound/category/favorite survive | 🎯 | 🎯 |
| CVO2 | lock `logo_red`, then re-run CVO1 | `logo_red` skipped, others proceed, summary line names the skip | 🎯 | 🎯 |

## A · `/cb resize` · `/cb exportpng` · 🎯 not tested
> 💡 Resize resamples a block's texture; exportpng writes it to `cloud_exports/<id>.png`.
> 🧰 a textured block.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb resize <id> 128` | "texture resized to 128×128"; visibly sharper than 64px | 🎯 | 🎯 |
| A2 | `/cb resize <id> 256` then `64` | resamples down cleanly, no crash | 🎯 | 🎯 |
| A3 | `/cb exportpng <id>` | "Texture exported" + file appears at `config/customblocks/cloud_exports/<id>.png` | 🎯 | 🎯 |
| A4 | click the `[download]` chat link from A3 | ⚠️ **known bug** — link leaks the server host and is unreachable; expect it to fail, that's the documented state not a new find | 🎯 | 🎯 |

## B · `/cb gradient` · Gradient Builder · 🎯 not tested
> 💡 Creates N intermediate colour-blended blocks between two existing blocks' colours, CIE-Lab interpolated.
> 🧰 two textured blocks of different colours.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | `/cb gradient <id1> <id2> 4` | 4 blocks created (`gradient_1`..`gradient_4`), smooth colour steps | 🎯 | 🎯 |
| B2 | `/cb undo` right after | whole gradient batch reverts in one step | 🎯 | 🎯 |

## C · `/cb bgstudio` · fill colour · 🎯 not tested
> 💡 Chest-GUI background removal: corners / flood / none / ai modes, fill colour default black.
> 🧰 `/cb tolerance <id> 30` then a textured block.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | `/cb bgstudio <id>` → Corners mode → Apply | background pixels removed/filled; subject only | 🎯 | 🎯 |
| C2 | same, on a dark-subject source | ⚠️ if a black band/ring shows on the edge, that's the known G section bug — note it, don't re-report | 🎯 | 🎯 |
| C3 | a flattened-checkerboard source (fake-transparent export) | comes out solid black, no checker/speckle | 🎯 | 🎯 |

## D · Colour Variants panel (chest GUI) · 🎯 not tested
> 💡 From the Block Editor, "Color Variants" slot — 4-6 algorithmic swatches (lighter/darker/complementary), separate from the `/cb colorvariants` command family above.
> 🧰 `/cb editor <id>`.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | open editor → click "Color Variants" | panel shows 4-6 swatches | 🎯 | 🎯 |
| D2 | click a swatch | new block created using that variant's texture | 🎯 | 🎯 |

## E · `/cb palette` · `/cb coloring` hub · 🎯 not tested
> 💡 Per-player saved colour palettes, reachable from chat command or the Colors hub GUI.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | `/cb palette save orange-theme` → `/cb palette list` | "orange-theme" saved and shown | 🎯 | 🎯 |
| E2 | `/cb palette load orange-theme` | loads into the colour picker | 🎯 | 🎯 |
| E3 | `/cb colors` | Colors hub chest GUI opens | 🎯 | 🎯 |

## F · Live recolour slider · screen eyedrop · 🎯 not tested
> 💡 Real Screens (not chest GUIs) — drag a slider to preview live recolour; eyedrop samples any monitor pixel.
> 🧰 `/cb recolor <id>`.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | `/cb recolor <id>`, drag slider | block updates live as you drag; Esc/cancel returns to the previous menu | 🎯 | 🎯 |
| F2 | open the eyedrop from the recolour screen, click a pixel elsewhere on screen | that exact colour is picked | 🎯 | 🎯 |

## H · Non-square logo variant: padding bands · 🎯 not tested
> 💡 Non-square sources used to show black padding bands on colour variants — fixed 2026-06-29 by filling the transparent pad with the variant colour.
> 🧰 a non-square logo (e.g. 960×673).

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | make a colour variant of a non-square logo | no black bands top/bottom — padding matches the variant colour | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | What it'll do                                                                                        |
| --- | ---------------------------------------------------------------------------------------------------- |
| A | `jar` CLI export (with zip) — bundle the RP + blocks into a `.jar` mod                               |
| F | `customcolor` UI hex picker + brightness slider                                                      |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(none — CV/CV-R/CV-O/A-F/H rebuilt with fresh test-row tables above, 2026-07-12, verified against current source. Old passes here were stale pre-07-03-cutoff and are superseded, not restored.)*

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

