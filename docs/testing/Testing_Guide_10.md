# Group 10 - Color & Image Tools

## Status

| | |
| --- | --- |
| **Verdict** | Six sections are confirmed; the jar A colour engine is built and in the mods folder for testing, while the knob removal and auto detection wait as jar B. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-07-26 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| HA | Colour engine rebuild (§H jar A) | Built 🎯 | - |
| HB | Knob removal + auto detection (§H jar B) | Designed ⏳ | - |
| A | Resize, export PNG, gradient | Done ✅ | - |
| B | No hairline at block edges | Done ✅ | - |
| C | Change a block's background colour | Done ✅ | - |
| D | Fake-transparency grid cleanup | Done ✅ | - |
| F | `/cb colorvariants` families and delete | Done ✅ | - |
| G | Quieter chat, sharper resize | Done ✅ | - |
| E | Colour variants covering the background | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_10_COLOR_IMAGE.md](../groups/GROUP_10_COLOR_IMAGE.md)

---

# Active Tests

## 💡 Setup

- Runnable now: §HA — the jar A build is in the server's mods folder.
- `/cb tolerance` still works in jar A **on purpose**; jar B deletes it (§HB rows).
- §HB waits for jar B; §E is parked.

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

All seven downloaded fine on 2026-07-25. Report and screenshot any that misbehave.

## HA - Colour engine rebuild (§H jar A) - Built 🎯

| | |
| --- | --- |
| **Check** | Same commands as before, cleaner results: no dark rim, thin lines survive, real transparency untouched. |
| **Pass rule** | Every row passes with default settings — no knob changes needed. |
| **Pass mark** | 🎯 built `2026-07-26`, needs in-game run |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HA1 | Create from `<link 2>` | Grid gone, shading kept, no dark rim | ✅ 2026-07-26 | ⏳ |
| HA2 | Create from `<link 3>` | Hair-thin outlines all present | ✅ 2026-07-26 | ⏳ |
| HA3 | Create from `<link 1>` | Untouched, file alpha used | ✅ 2026-07-26 | ⏳ |
| HA4 | Colour variant from `<link 7>` | Blue and the O's hole recoloured, no dark rim | ✅ 2026-07-26 | ⏳ |
| HA5 | Create from `<link 4>` | Grid gone, shading kept | 🎨 Polish | ⏳ |
| HA6 | `/cb setbg <id> red` on the HA2 block | Edges smooth, no bright rim | ✅ 2026-07-26 | ⏳ |
| HA7 | `/cb retextureall`, timed | No hang, not slower than before | ✅ 2026-07-26 | ⏳ |

- 💡 Faint dashed marks on the `<link 4>` bake come from that picture file itself — a clean PNG of the same logo bakes smooth. Confirmed 2026-07-26 by zoomed bake comparison (JPEG block-compression noise sits directly on the true edge pixels; no pixel-level fix exists without eating real edge detail). Jar B's cascade owns the real fix.
- 💡 HA4's recoloured blue background is correct, not a bug — [G10 §H locked decision, 2026-07-25](../groups/GROUP_10_COLOR_IMAGE.md): recolour rails (variants, families, Arabic) always run Auto regardless of the global mode.

## HB - Knob removal + auto detection (§H jar B) - Designed ⏳

| | |
| --- | --- |
| **Check** | Backgrounds come off with no strength setting anywhere, and an unsure result says so instead of damaging the picture. |
| **Pass rule** | Every row passes with no number typed anywhere. |
| **Pass mark** | ⏳ being built — rungs 1-3 of 5 exist, nothing wired in yet |
| **Blocked** | Not testable until the detector replaces the knob; `/cb tolerance` still answers in this jar by design. |

**The knob is gone**

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HB1 | `/cb tolerance`, `/cb tolerance 30` | Unknown command, no tab-complete | ⏳ | ⏳ |
| HB2 | `/cb bgstudio` on any block | No strength button, no percentage | ⏳ | ⏳ |
| HB3 | Cycle `background` in `/cb config` | Auto and Off only | ⏳ | ⏳ |
| HB4 | `/cb config background BgRemove&More` | Accepted, resolves to Auto | ⏳ | ⏳ |
| HB5 | `/cb config background BgSmart` | Resolves to Auto, never suggested | ⏳ | ⏳ |
| HB6 | Junk mode value in config, restart | Comes up Auto, not Off | ⏳ | ⏳ |
| HB7 | Inspect `config/customblocks/` | `block_tolerances.json` gone | ⏳ | ⏳ |
| HB8 | Rename one block, then bulk-rename | Both succeed, no error | ⏳ | ⏳ |

**Auto gets the pictures right**

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HB9 | Create from `<link 1>` | Untouched, file alpha used | ⏳ | ⏳ |
| HB10 | Create from `<link 2>` | Grid gone, shading kept, no dark rim | ⏳ | ⏳ |
| HB11 | Create from `<link 4>` | Same verdict as HB10 | ⏳ | ⏳ |
| HB12 | Create from `<link 3>` | Thin outlines all present | ⏳ | ⏳ |
| HB13 | Colour variant from `<link 7>` | Blue and the O's hole both recoloured | ⏳ | ⏳ |
| HB14 | Create from `<link 6>` | Left alone | ⏳ | ⏳ |
| HB15 | Set Off, inspect edges close up | Still no hairline | ⏳ | ⏳ |

**Rails that must keep working**

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HB16 | Off set, make a colour family | Variants recoloured, not clones | ⏳ | ⏳ |
| HB17 | Off set, create from a link | Background left alone | ⏳ | ⏳ |
| HB18 | Inspect bundled Arabic coloured sets | Recoloured right, no dark rim | ⏳ | ⏳ |
| HB19 | `/cb retextureall`, timed | No hang, not slower than baseline | ⏳ | ⏳ |
| HB20 | Off set, create from a flattened checkerboard | Still flattened to black | ⏳ | ⏳ |
| HB21 | `/cb bgstudio` on a pre-update per-block strength | Opens clean, no error | ⏳ | ⏳ |

**When Auto is unsure**

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| HB22 | Create from a busy edge-to-edge picture | Refuses, bakes unchanged, names `/cb bgpick` | ⏳ | ⏳ |
| HB23 | `/cb bgpick <id> <bg colour>` on the HB22 block | Removes correctly, `/cb undo` reverts | ⏳ | ⏳ |
| HB24 | `/cb bgpick <id> #FF00FF` on a magenta-free image | Says colour absent, no re-bake | ⏳ | ⏳ |
| HB25 | `/cb bgpick` on a block with no stored picture | Honest skip, no guessed re-bake | ⏳ | ⏳ |

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
- §E2 Chrome logo keeps its shading in `BgRemove&More` — ✅ `2026-07-25`
- §F Colour families: create, rebuild, size limits, overwrite warning, delete with confirm, undo, locked members, missing name — ✅ `2026-07-25`
- §G1 Creating from a small source prints only the fetching and result lines — ✅ `2026-07-25`
- §G2 Both blocks crisper, no bright rim and no doubled outline — ✅ `2026-07-25`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

- Owner-set colour hexes — 📜 `2026-07-25`: waiting on the owner's hex list. [G10 §G](../groups/GROUP_10_COLOR_IMAGE.md)

</details>

<details><summary>💤 <b>Parked</b></summary>

- §E3 and §E4 — 💤 `2026-07-25`: both rows set a tolerance number and `/cb tolerance` is being deleted; they return as §HB13 and §HB12.
- §E3 case: the letter O — `<link 7>` — the hole inside the O must fill too.
- §E4 case: Tux — `<link 3>` — hair-thin outlines must survive.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- `/cb dress` — 👎 `2026-06-15`: colour variants and live recolour cover it.
- White outline behind dark art — 👎 `2026-06-29`: reverted, plain black won.
- Advanced `/cb bulkrecolor` Hub — 👎 `2026-07-24`: moved to G27 as the Advanced Recolor Hub; simple hue-shift stays.
- AI background removal — 👎 `2026-07-24`: owner call, never implemented.
- `/cb setbg all` — 👎 `2026-07-25`: owner call; the run broke blocks and server-wide background is not wanted.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete test blocks `g10a`, `d6`, `g1` — rows are done.
- [ ] Delete `e3` and `e4` — §H makes its own blocks.
- [ ] Restore the background on any block left black by the scrapped `/cb setbg all` run.
- [ ] Delete old confirmed-section blocks: `d1`, `d3`, `d5`, `chev`, `test3`, `test5`, `Test6`, `test10`, `big`, zombie.

</details>
