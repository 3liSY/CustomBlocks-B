# Group 08 — Shapes & Per-Face Textures

**🎯 Active:** A1 reload-prompt (1) · A8 gif-slab render (1) · G rotation spec locked, not built · F reverted (0/2)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                       |
| --------------- | ------------------------------------- |
| **Verdict**     | ✅ B/C/E fully passed, folded to Archive — A has 2 open findings (A1 RP-reload, A8 gif-slab render), F still needs retest |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅⚠️⚠️⏳⏳ · 79% (15/19 MP passed, 2 findings) |
| **Last tested** | 2026-07-12 (MP)                       |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                 | State      |
| --- | ------------------------------------ | ---------- |
| A | Shape commands (set · clear · X-ray) | 🎯 A1, A8 open |
| B | Shape editor Screen                  | ✅ 2026-07-12 |
| C | Face textures (commands + on shapes) | ✅ 2026-07-12 |
| D | Face editor Screen (`facechangegui`) | 🧊 parked → `GROUP_27_SCREENS.md §G27.18` |
| E | `bulkshape`                          | ✅ 2026-07-12 |
| F | Shape preview                        | 🎯 needs retest |
| G | Directional placement (Rotation)     | ⏳ spec locked 2026-07-12, not built |

🔗 **Related Doc:** [GROUP_08_SHAPES.md](../groups/GROUP_08_SHAPES.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g08a ShapeTest` · `/cb retexture g08a <url>` for A
* `g08a` placed for B and C

## A · Shape commands · 🎯 A1, A8 open

> 💡 Change a block's geometry.
> 🧰 `/cb create g08a ShapeTest` · `/cb retexture g08a <url>`

| # | Action                                            | Expected Result                            | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb setshape g08a slab_bottom`                   | block becomes bottom slab; can stand on it | ➖ | ⚠️ |
| A2 | relog or restart                                  | still a slab                               | ➖ | ✅ |
| A3 | `/cb clearshape g08a`                             | back to full cube                          | ➖ | ✅ |
| A5 | set slab/pillar, look at ground around it         | no X-ray holes into caves/void             | ➖ | ✅ |
| A6 | set pillar/thin/stairs/pane, walk into open parts | no push-out; selection box hugs shape      | ➖ | ✅ |
| A8 | `/cb setshape` a slab on a **gif/animated-texture** block, place, stand on it | hitbox/collision is slab (confirmed — feels like slab), but **visually still renders full cube** | 🎯 | ⚠️ |

> ⚠️ A1 (MP): passes, but `setshape` pops the resource-pack reload screen. Owner wants the shape to
> update instantly client-side with no reload prompt — tracked as bug below.

> ⚠️ A8 (new 2026-07-12): confirmed systemic — happens on **every** gif/animated-texture block, not one-off.
> Animated blocks use a separate model-bake path that skips the shape bake — hitbox says slab, render
> says full cube. Static-texture blocks unaffected (see passed B/C/E). Tracked as bug below.

> **A4 and A7 removed:** A4 shapelist was folded into GUI rework. A7 cross collision is accepted partial.

---

## G · Directional Placement (Rotation & Halves) · ⏳ spec locked 2026-07-12, not built

> 💡 Full vanilla rotation (Horizontal Facing + Halves) for every custom block. Stairs face the player;
> clicking the top of a block places upside-down stairs. Per-face textures face the player. Full spec
> DP1-DP6 locked in `GROUP_08_SHAPES.md` §G08.8 — nothing left to design, build it.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Place a block with `stairs` shape | Block rotates to face the player | ⏳ | ⏳ |
| G2 | Click the top half of a block while holding `stairs` | Places upside-down stairs | ⏳ | ⏳ |
| G3 | Place a full block with a front-face texture | Texture rotates to face the player | ⏳ | ⏳ |
| G4 | Walk into the empty space of rotated stairs | No collision; invisible hitboxes rotate perfectly | ⏳ | ⏳ |

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **F · Shape preview** (Passed 2026-06-13). Test history moved to [GROUP_08_SHAPES.md](GROUP_08_SHAPES.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **B · Shape editor Screen** (Passed 2026-07-12, MP). B1/B2/B3 all confirmed — click-based shape picker works, refreshes highlight correctly, `full` is just a grid tile (no separate reset button, corrected 2026-07-11).
- **C · Face textures** (Passed 2026-07-12, MP). C1-C6 all confirmed — per-face texture commands work on shaped blocks and survive shape changes.
- **E · Bulk shape** (Passed 2026-07-12, MP). E1-E4 all confirmed — bulk apply, confirm-gate, undo, and locked-skip all work.

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| G08-A1 | A1 | `setshape` pops RP-reload screen — fix: bake all shape models once, swap via BlockState property | 🔒 spec locked, see [GROUP_08_SHAPES.md](../groups/GROUP_08_SHAPES.md) |
| G08-A8 | A8 | **all** gif/animated-texture blocks: shape hitbox correct (slab) but visual still full cube — animated model bake path skips shape bake | ✅ 2026-07-12: build a full fix (not a workaround) — deep-search the animated model-bake path, google Fabric 1.21.1 animated-texture + shape/model precedent if needed, fix the root cause |
| G08-G  | §G  | Directional Placement (rotation + halves) for every custom block — full spec DP1-DP6 already locked in `GROUP_08_SHAPES.md` §G08.8 | ⏳ spec locked, no rotation code found in source — not built yet |

---

