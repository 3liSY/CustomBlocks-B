# Group 10 - Color & Image Tools

## Status

| | |
| --- | --- |
| **Verdict** | The colour engine and the busy-source row are confirmed; the pale outline round the artwork is root-caused to four faults, all rebuilt in jar F and waiting on an in-game retest. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-07-29 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| HD | Fake-preview specks and rims (§H jar D) | Built 🎯 | Regression 💔 |
| HC | Same-colour artwork (§H jar C) | Built 🎯 | Regression 💔 |
| HA | Colour engine rebuild (§H jar A) | Done ✅ | Regression 💔 |
| A | Resize, export PNG, gradient | Done ✅ | - |
| B | No hairline at block edges | Done ✅ | - |
| C | Change a block's background colour | Done ✅ | - |
| D | Fake-transparency grid cleanup | Done ✅ | - |
| F | `/cb colorvariants` families and delete | Done ✅ | - |
| G | Quieter chat, sharper resize | Done ✅ | - |
| HB | Knob removal + auto detection (§H jar B) | Done ✅ | - |
| E | Colour variants covering the background | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_10_COLOR_IMAGE.md](../groups/GROUP_10_COLOR_IMAGE.md)

---

# Active Tests

## 💡 Setup

- Background mode config: `config/customblocks/config.json`, key `backgroundMode`.
- Off is spelled `nobackground`, not `none`.
- `/cb tolerance` is gone; no strength setting anywhere.
- §E is parked.

**Test pictures** — referenced below as `<link 1>`, `<link 2>`, and so on.

| # | Link | Why this one |
| --- | --- | --- |
| 1 | [transparent PNG](https://commons.wikimedia.org/wiki/Special:FilePath/PNG_transparency_demonstration_1.png) | Real alpha, must stay untouched |
| 2 | [chrome logo](https://www.citypng.com/public/uploads/preview/chevrolet-logo-emblem-png-image-701751694713268nqpyixtxkk.png) | Dark checker grid behind shiny chrome |
| 3 | [Tux](https://commons.wikimedia.org/wiki/Special:FilePath/Tux.png) | Hair-thin outlines |
| 4 | [blurry JPEG copy](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTSe6HDsghzAZLcsTvwpErU0SlbVKOOBNYDfFPCUOPCBzEOiIG8QkQ3pC8&s=10) | Link 2 re-compressed |
| 5 | [oversized table](https://upload.wikimedia.org/wikipedia/commons/3/39/Periodic_table_large.png) | Too big on purpose |
| 6 | [one-way stripes](https://upload.wikimedia.org/wikipedia/commons/a/ad/Narrow_9_stripes.png) | Must not read as a checker grid |
| 7 | [letter O on blue](https://thumbs.dreamstime.com/b/letter-o-lettering-design-blue-color-background-o-letter-blue-background-268745593.jpg) | Enclosed hole for the fill to reach |
| 8 | [table, 2000px](https://commons.wikimedia.org/wiki/Special:FilePath/Periodic_table_large.png?width=2000) | Link 5 under the size cap, for HB15 |
| 9 | [football](https://www.citypng.com/public/uploads/preview/black-white-classic-soccer-ball-hd-png-7040816948787759thtmzauog.png) | Fake checker preview, black panels |
| 10 | [penguin](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTKQx3AGxCdlFu_yFxVd5JV2DMSdzXsZY1JTKvHVykDNA&s=10) | White body on white |
| 11 | [Jupiter](https://i.pinimg.com/736x/43/53/c2/4353c255d2544a1c3587ed9573989fc3.jpg) | Cropped subject, background in corners |
| 12 | [share button](https://image.similarpng.com/file/similarpng/very-thumbnail/2020/11/Share-icon-with-red-color-on-transparent-background-PNG.png) | Fake checker preview |
| 13 | [subscribe banner](https://png.pngtree.com/png-vector/20211001/ourmid/pngtree-subscribe-in-arabic-with-bell-icon-png-image_3966312.png) | Fake checker preview, Arabic |

Links 1-8 downloaded fine on 2026-07-25; links 9-13 on 2026-07-28. Report and screenshot any that misbehave.

## HC - Same-colour artwork (§H jar C, refixed jar F) - Built 🎯

| | |
| --- | --- |
| **Check** | Artwork that shares its colour with its own background bakes whole. |
| **Pass rule** | No holes, no eaten panels, no kept backdrop. |
| **Pass mark** | Stripes confirmed; the other five rebuilt in jar F and awaiting a retest |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HC1 💔 | Colour variant from `<link 9>` (football) | Ball whole, no ring of backdrop round it | 🎯 | 🎯 |
| HC2 💔 | Colour variant from `<link 10>` (penguin) | Belly stays white, no pale outline | 🎯 | 🎯 |
| HC3 💔 | Colour variant from `<link 11>` (Jupiter) | Backdrop removed, no pale outline | 🎯 | 🎯 |
| HC4 💔 | Colour variant from `<link 12>` and `<link 13>` | Button and banner clean, no pale outline | 🎯 | 🎯 |
| HC5 💔 | Re-check `<link 2>` and `<link 4>` (chrome) | `<link 4>` shows no dark outline | 🎯 | 🎯 |
| HC6 | Re-check `<link 6>` (stripes) | Still left alone | 🎯 | ✅ 2026-07-28 |

## HD - Fake-preview specks and rims (§H jar D, refixed jar F) - Built 🎯

| | |
| --- | --- |
| **Check** | Small light details inside artwork survive, and no edge bakes as a dark rim. |
| **Pass rule** | No black specks in white shapes, no see-through dots in filled areas, no ring on any edge. |
| **Pass mark** | Subscribe dots and the golden pair confirmed; the four edge rows rebuilt in jar F and awaiting a retest |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HD1 | Colour variant from `<link 13>` (subscribe) | ش keeps its three white dots | 🎯 | ✅ 2026-07-28 |
| HD2 💔 | Colour variant from `<link 12>` (share) | Arrow clean inside and no pale outline | 🎯 | 🎯 |
| HD3 💔 | Colour variant from `<link 9>` (football) | No see-through dots in white panels | 🎯 | 🎯 |
| HD4 💔 | Same three blocks, edges close up | No dark ring, no pale ring | 🎯 | 🎯 |
| HD5 💔 | Re-check `<link 2>` and `<link 4>` (chrome) | `<link 4>` shows no dark outline | 🎯 | 🎯 |
| HD6 | Re-check `<link 1>` and `<link 3>` | Untouched, thin outlines intact | 🎯 | ✅ 2026-07-28 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Resize, export PNG and gradient commands — ✅ `2026-06-15`
- §B No hairline around block edges close up — ✅ `2026-07-25`
- §C Single-block background: black, named colour, hex, shaped block, restart, undo, honest skip — ✅ `2026-07-25`
- §C9 `/cb setbg` on a trashed block names the trash and `/cb trash restore` — ✅ `2026-07-25`
- §D Checker grids cleaned to black on sharp and blurry sources, real alpha untouched, no speckles — ✅ `2026-07-25`
- §D6 One-way striped artwork left alone by checker cleanup — ✅ `2026-07-25`
- §E1 Tall and wide pictures pad with the variant colour, no black bars — ✅ `2026-07-25`
- §E2 Chrome logo keeps its shading through background removal — ✅ `2026-07-25`
- §F Colour families: create, rebuild, size limits, overwrite warning, delete with confirm, undo, locked members, missing name — ✅ `2026-07-25`
- §G1 Creating from a small source prints only the fetching and result lines — ✅ `2026-07-25`
- §G2 Both blocks crisper, no bright rim and no doubled outline — ✅ `2026-07-25`
- §HA1 Create from the chrome logo, grid gone with no dark rim — ✅ `2026-07-26`
- §HA2 Create from Tux, hair-thin outlines all present — ✅ `2026-07-26`
- §HA3 Create from a transparent PNG, untouched — ✅ `2026-07-26`
- §HA5 Create from a re-compressed JPEG copy, grid gone and shading kept — ✅ `2026-07-27`
- §HA6 `/cb setbg` on a baked block, smooth edges — ✅ `2026-07-26`
- §HA7 `/cb retextureall` timed, no hang — ✅ `2026-07-26`
- §HA8 Create from striped artwork, left alone — ✅ `2026-07-26`
- §HB1 `/cb tolerance` fully removed, no tab-complete — ✅ `2026-07-26`
- §HB2 `/cb config` cycles Auto/NoBackground only — ✅ `2026-07-26`
- §HB3 Legacy mode spelling rejected in chat — ✅ `2026-07-26`
- §HB4 `BgSmart` rejected in chat — ✅ `2026-07-26`
- §HB5 Junk config value fails startup with a named error — ✅ `2026-07-26`
- §HB6 Legacy `none` config value renames to `nobackground` on start — ✅ `2026-07-26`
- §HB7 `block_tolerances.json` confirmed gone — ✅ `2026-07-26`
- §HB8 `/cb rename` and `/cb bulkreid` succeed with no error — ✅ `2026-07-26`
- §HB9 Off set, create from `<link 3>`, no see-through hairline along the edge — ✅ `2026-07-27`
- §HB10 Off set, colour family recolours variants, not clones — ✅ `2026-07-26`
- §HB11 Off set, create leaves background alone — ✅ `2026-07-26`
- §HB14 Off set, flattened checkerboard still cleans to black — ✅ `2026-07-26`
- §HB15 Busy picture under the size cap bakes with no refusal line — ✅ `2026-07-29`
- §HA4 Colour variant from the letter O, accepted with a known soft regression — ✅ `2026-07-29`
- §HA9 Chrome logo, Tux and the chrome JPEG unchanged by the HA4 fix — ✅ `2026-07-29`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §HA4 Colour variant from `<link 7>` — 💔 `2026-07-28`: owner reports dots still round the O. Clean-plate unit added the same day takes the baked strong-blue count 30 → 13 offline, with six of the seven goldens byte-identical and the chrome logo moved 32 px. The 13 survivors measure 5.0-37.8 ΔE00 from the local backdrop, so they are not background residue at any bar — they are half-backdrop edge pixels of a deliberately blurred glyph, reachable only by unmixing. Two deeper-unmix attempts (a plate-keyed spill pass, then a ramp-followed band) each regressed the chrome goldens and raised the blue count, and were reverted. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)
- §HA4 earlier round — 💔 `2026-07-27`, refixed same day. Two blue blobs sat on the O's inner edge; measured offline as 57 px and 43 px slivers of the counter, pinched off it by a 2-3 px seam, plus watermark-veiled pixels 6.3 ΔE from the key. Fix pairs pockets across hairline seams and recognises background under a translucent white veil. Golden-diffed: Tux, the chrome logo, the transparent PNG, the periodic table and the stripes are byte-identical; the blurry chrome JPEG moves 616 px with no visible change. A third fix then took the watermark's own ink, which measured 81 % of the way to white and was never background at all: an isolated, under-floor, lighter-and-paler island is absorbed, and isolated means touching no subject anywhere. Strong blue on the letter O falls 296 px → 44 px. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)
- §HC2 Colour variant from `<link 10>` (penguin) — 💔 `2026-07-29`: confirmed on 2026-07-28, broken again by the jar E edge rebuild. A white outline sat outside the bird, the same pale rim HC1, HC3, HC4 and HD2 show — one fault in the edge solve, not four. Fourth round on this defect. Root-caused to four separate faults and rebuilt in jar F; leftover backdrop measured share 618 → 0, penguin 475 → 9, Jupiter 160 → 54, chrome 169 → 14, subscribe 179 → 131. Subscribe is the weak one and is not root-caused. Still needs an in-game retest. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)
- §HC1 Colour variant from `<link 9>` (football) — 💔 `2026-07-29`: background mode is Auto and the source is a fake-checker preview. The ball baked inside a ring of its own backdrop one cell wide, root-caused to the grid silhouette repair and rebuilt in jar F: leftover backdrop 318 → 194, of which 168 are white ball panels against white grid cells with no colour difference to separate. That 168 is a ceiling, not a bug — colour cannot decide it, only an alpha-carrying source can. Still needs an in-game retest. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)
- §HB15 Create from `<link 5>` — 🟡 `2026-07-27`: refusal came from the §F size limit (6000x3300 over the 4096px cap), not from Auto being unsure. Superseded — the refusal path itself is gone, so the row now checks that a busy picture bakes silently. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)

</details>

<details><summary>📜 <b>Planned</b></summary>

- Gradient backgrounds never reach rung 2 — 📜 `2026-07-27`: measured on `<link 7>`, whose background fades from `15,116,186` at the top-left to `31,133,197` at the bottom-right, 6.5 ΔE apart. `flatBorderTone` needs all four sides within one JND of a single tone: top and left hit 99.9 %, bottom and right hit 0.0 %, so rung 2 declines and the picture falls to the measuring rungs. Smooth gradients are ordinary in stock art, so this is a whole class of source that can never be decided by a fact. Own session; three approaches already measured and rejected (bilinear surface fit 35 % vs 85 % coverage; neighbour-relative flood 75 %; running local colour 74.9 %). [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)
- Owner-set colour hexes — 📜 `2026-07-25`: waiting on the owner's hex list. [G10 §G](../groups/GROUP_10_COLOR_IMAGE.md)
- A light subject sharing the grid's own tone — 🎯 `2026-07-28`: built in jar E and folded into HC1/HD3. [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md)

</details>

<details><summary>💤 <b>Parked</b></summary>

- §E3 and §E4 — 💤 `2026-07-25`: both rows set a tolerance number and `/cb tolerance` is being deleted; they return folded into §HA4/§H picture-accuracy testing.
- §E3 case: the letter O — `<link 7>` — the hole inside the O must fill too.
- §E4 case: Tux — `<link 3>` — hair-thin outlines must survive.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- `/cb dress` — 👎 `2026-06-15`: colour variants and live recolour cover it.
- White outline behind dark art — 👎 `2026-06-29`: reverted, plain black won.
- Advanced `/cb bulkrecolor` Hub — 👎 `2026-07-24`: moved to G27 as the Advanced Recolor Hub; simple hue-shift stays.
- AI background removal — 👎 `2026-07-24`: owner call, never implemented.
- `/cb setbg all` — 👎 `2026-07-25`: owner call; the run broke blocks and server-wide background is not wanted.
- `/cb bgstudio` no-strength check — 👎 `2026-07-26`: owner call; passed SP `2026-07-26` but owner does not use GUIs, moving to G27's screen-GUI migration with the others.
- `/cb bgstudio` on pre-update per-block strength — 👎 `2026-07-26`: owner call; same G27 screen-GUI migration.
- `/cb bgpick` and the unsure-refusal line — 👎 `2026-07-28`: owner call. Detection must be automatic, so a command whose only job is to apologise for detection failing is a setting in disguise. An undecidable picture now bakes unchanged and says nothing. Retires HB16, HB17 and HB18.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete test blocks `g10a`, `d6`, `g1` — rows are done.
- [ ] Delete `e3` and `e4` — §H makes its own blocks.
- [ ] Restore the background on any block left black by the scrapped `/cb setbg all` run.
- [ ] Delete old confirmed-section blocks: `d1`, `d3`, `d5`, `chev`, `test3`, `test5`, `Test6`, `test10`, `big`, zombie.

</details>
