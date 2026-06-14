# 🧪 Group 08 — Shapes & Per-Face Textures — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| ✅ Passed | Shapes · Shape editor GUI · Face textures · Face editor GUI · **X-ray fix** · **Shape preview** | §1–§5 |
| 🟡 Polish later | Face **direction clarity** (→ FaceGuide block + `/cb faceguide`) · `facechangegui` no-arg flow | §4 |
| ⏳ Coming | `bulkshape` · `customtriangle` / `trianglemode` · textured `shapepreview <shape> <id>` | §6 |

---

# 🎯 Test now

## §1 · Shape commands

> 💡 **What it does:** change a block's geometry — slab, carpet, stairs, cross, etc. The shape you
> walk into matches the shape you see, and it survives a restart.

> 🧰 **Before you start:**
> - `/cb create g08a ShapeTest`
> - `/cb retexture g08a <any image url>`
> - Place `g08a` somewhere you can look at it.

**Try these:**

- **① Set a slab** — `/cb setshape g08a slab_bottom`
  - ✅ **Pass:** the placed block becomes a bottom slab; you can stand on the lower half.
- **② Survives restart** — relog or restart the server, look at `g08a`.
  - ✅ **Pass:** still a slab.
- **③ Clear it** — `/cb clearshape g08a`
  - ✅ **Pass:** back to a full cube.
- **④ List shapes** — `/cb shapelist`
  - ✅ **Pass:** chat lists every shape with a description.
- **④b 🔧 X-ray re-test (fixed)** — set a slab/pillar, then look at the ground/blocks around & under it.
  - ✅ **Pass:** no see-through holes into caves/void behind the block.
  - ⚠️ Also glance at a **full** image block — confirm it still looks right (lighting may differ slightly).

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | shape applies to the live block (collision + look) |
| ⬜ | ② | shape persists to disk |
| ⬜ | ③ | clearshape restores a full cube |
| ⬜ | ④ | shapelist shows the choices |

> ↩️ **Undo the test:** `/cb clearshape g08a`  (or `/cb undo`)

---

## §2 · Shape editor GUI

> 💡 **What it does:** pick a shape by clicking instead of typing — every click just runs the
> `setshape` command above.

- **⑤ Open it** — `/cb shapeeditor g08a` *(or `/cb editor g08a` → click the **Shape** tile — same menu)*
  - ✅ **Pass:** a chest menu opens with a button per shape; the current shape glows (enchant glint).
- **⑥ Click a shape** — click **stairs**.
  - ✅ **Pass:** the block becomes stairs and the menu re-opens with **stairs** now glowing.
- **⑦ Reset tile** — click **Reset to full block**.
  - ✅ **Pass:** back to a full cube.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑤ | GUI opens, current shape marked |
| ⬜ | ⑥ | clicking a shape applies it |
| ⬜ | ⑦ | reset tile clears the shape |

> ↩️ **Undo the test:** `/cb clearshape g08a`

---

## §3 · Face textures (commands)

> 💡 **What it does:** put a different image on one face of a block. Faces with no override keep the
> base texture. *(These commands existed since Group 06 as `paintface`/`clearface`; Group 08 just adds
> the `setface` / `clearallfaces` names.)*

- **⑧ Set one face** — `/cb setface g08a up <image url>`
  - ✅ **Pass:** only the top face changes; the rest keep the base texture (accept the pack prompt).
- **⑨ Set a few more** — `/cb setface g08a north <url>` and `/cb setface g08a east <url>`
  - ✅ **Pass:** three faces differ, three stay default.
- **⑩ Clear one** — `/cb clearface g08a up`
  - ✅ **Pass:** the top face goes back to the base texture.
- **⑪ Clear all** — `/cb clearallfaces g08a`
  - ✅ **Pass:** every face is back to the base texture.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑧ | one face overrides independently |
| ⬜ | ⑨ | multiple faces set independently |
| ⬜ | ⑩ | clearface reverts one face |
| ⬜ | ⑪ | clearallfaces reverts every face |

> ↩️ **Undo the test:** `/cb clearallfaces g08a`

---

## §4 · Face editor GUI  🟡 *(works, but layout is being redesigned — see "Polish later")*

> 💡 **What it does:** the click-based version of §3 — one tile per face.
> 🟡 **Known:** the coloured-glass tiles don't clearly say which face is which — a clearer layout +
> a no-arg block-picker flow are parked for a polish pass. Functionality below still works.

- **⑫ Open it** — `/cb facechangegui g08a` *(or `/cb editor g08a` → click the **Faces** tile — same menu)*
  - ✅ **Pass:** a chest menu opens with six face tiles; painted faces glow.
- **⑬ Left-click a face** — click the **Top** tile.
  - ✅ **Pass:** chat fills with `/cb paintface g08a up ` — paste a URL and press enter; that face updates.
- **⑭ Right-click a face** — right-click a painted tile.
  - ✅ **Pass:** that face clears and the menu re-opens with it no longer glowing.
- **⑮ Clear-all tile** — click **Clear all faces**.
  - ✅ **Pass:** every face reverts.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑫ | GUI opens, painted state shown |
| ⬜ | ⑬ | left-click → URL paste → face updates |
| ⬜ | ⑭ | right-click clears one face |
| ⬜ | ⑮ | clear-all tile works |

> ↩️ **Undo the test:** `/cb clearallfaces g08a`

---

## §5 · Shape preview  ✅ *(was broken — fixed and confirmed in-game 2026-06-13)*

> 💡 **What it does:** floats a stand-in block (a vanilla block with the same shape) ~2.5 blocks in
> front of you for 5 seconds so you can see a shape before applying it. Removes itself automatically.
> 🔧 **Fixed:** it showed nothing before because the internal summon ran at your permission level (0 in
> a no-cheats world). It now runs at op level, so it works regardless of cheats.

- **⑯ Preview a slab** — `/cb shapepreview slab_top`
  - ✅ **Pass:** a top-slab-shaped block appears floating in front of you, then vanishes after ~5s.
  - ❌ **Broken if:** nothing appears, or the block stays past ~5s.
- **⑰ Preview a cross** — `/cb shapepreview cross`
  - ✅ **Pass:** an X-shaped (flower-like) preview appears and disappears.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ⑯ | preview appears and auto-removes |
| ⬜ | ⑰ | cross/other shapes preview too |

> ↩️ **Undo the test:** nothing to undo — the preview removes itself. (If one ever lingers:
> `/kill @e[type=block_display]`.)

---

# ⏳ Coming next  *(not built — nothing to test yet)*

| Feature | What it'll do |
|---|---|
| `/cb bulkshape <filter> <shape>` | Apply one shape to many blocks via the Group 07 bulk flow. |
| `/cb customtriangle` · `/cb trianglemode` | Triangular block geometry — needs a design chat (vs the Group 06 colour "Triangle"). |

---

## 🆘 If a test fails, send me
- 🔢 the step number
- 👀 what happened vs what you expected (📸 a screenshot helps)
- 📄 the last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete g08a`
