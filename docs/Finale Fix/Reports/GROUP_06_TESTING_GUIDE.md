# 🧪 Group 06 — Tools, Colour Variants & Per-Face Paint — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| 🎯 **TEST NOW** | Round-2 fixes + **M4 per-face paint** — clean Red Triangle · reload waits for ANY open GUI · paint one face from a URL | §A |
| 🎯 Re-confirm | Hex setters (`/cb config hex`) · Config-GUI hex editor + Color Studio | §B · §C |
| ✅ Passed 2026-06-11 | Square swap + marker purge (8/8) · Triangle variants (6/6) · Retexture-all + source store (6/6) | §D · §E · §F |
| ✅ Working | Omni-Tool · Deleter · Magic Items GUI · `[CB]` prefix · bg-removal + pixelation · Texture-Size picker | §G |
| 🟡 Polish later | hotbar wording · bg fringe / low-contrast (parked) · chat flooding · partial GUIs · lore | §H |
| ⏳ Coming | eyedropper · despeckle · preview · held-block glow · `.dat` migration (parked) | §I |

---

# 🎯 Test now

## §A · Round-2 fixes + M4 per-face paint

> 💡 **What it does:** the Red Triangle art is redrawn clean, pack reloads now wait while you're
> inside ANY CustomBlocks menu/anvil, and the Rainbow Rectangle paints single block faces from a URL.

> 🧰 **Before you start:**
> - a textured custom block placed
> - the Rainbow Rectangle + colour tools from `/cb` → Magic Items

**Try these:**

- **① Red Triangle looks right** — look at the Red Triangle (+ a changed-hex colour's tools)
  - ✅ **Pass:** clean, symmetric triangle like the yellow one (fill · lighter highlight · dark outline). Changed-hex tools keep the style; Color Studio pairs use the clean shape too.
- **② Reload waits for ANY open GUI** — `/cb config` → Variant Colours → edit red's hex in the anvil, **even with no `_red` blocks placed**
  - ✅ **Pass:** chat confirms, you land back in the editor, and **no reload happens while a CustomBlocks menu/anvil is open**. Close it → reload arrives a moment later.
  - ❌ **Broken if:** the rebuild log fires the same second (the old bug).
- **③ Rectangle paints one face** — Rainbow Rectangle → right-click the **north** face
  - ✅ **Pass:** chat pre-fills `/cb paintface <id> north ` → paste a URL → after one reload **only** that face changes; the other five keep the base.
- **④ More faces + direct command** — paint `up` with another URL; also `/cb paintface <id> east <url>`
  - ✅ **Pass:** each face holds its own image; the rest stay base.
- **⑤ Clear a face / all** — `/cb clearface <id> north` then `/cb clearface <id> all`
  - ✅ **Pass:** north → base, then every painted face resets; "No painted faces" when none left.
- **⑥ Area selector moved to sneak** — **sneak + right-click** two blocks with the Rectangle
  - ✅ **Pass:** corner 1 / corner 2 messages as before (Omni-Tool Area mode unaffected).
- **⑦ Errors bounce cleanly** — `paintface <id> sideways <url>` → "pick a face" · bad URL → "couldn't get a texture", face unchanged · `paintface nope north <url>` → "no block called".
- **⑧ Survives restart** — restart with painted faces present
  - ✅ **Pass:** every painted face stays (files in `config/customblocks/textures/slot_N_<face>.png`); `/cb clearface` still works.
- **⑨ Deleting cleans up** — delete a block with painted faces, then make a new one
  - ✅ **Pass:** no leftover face art bleeds onto other blocks.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | Red Triangle art clean; changed-hex tools keep the style |
| ⬜ | ② | reload waits for ANY open GUI (the re-fix) |
| ⬜ | ③ | Rectangle paints one face only |
| ⬜ | ④ | multiple faces + direct command |
| ⬜ | ⑤ | clearface (one / all) resets |
| ⬜ | ⑥ | sneak area-selector unaffected |
| ⬜ | ⑦ | bad face / URL / id → clean errors |
| ⬜ | ⑧ | painted faces survive a restart |
| ⬜ | ⑨ | deleting leaves no face leftovers |

---

## §B · Hex setters — `/cb config hex`  *(re-confirm)*

> 💡 **What it does:** `/cb config hex red #FF8800` makes "red" MEAN orange — for new variants, item
> art, item names, and (if you say Yes) your existing red blocks.

> 🧰 **Before you start:** a placed `vart_red`, a `_green` variant, the Squares/Triangles in inventory.

**Try these:**

- **① Status line** — `/cb config hex`
  - ✅ **Pass:** all four on one line — red `#EE3333` · yellow `#F0C814` · green `#1E8C1E` · black `#0A0A0A`.
- **② Change → confirm pops** — `/cb config hex red #FF8800`
  - ✅ **Pass:** "Red `#EE3333` → `#FF8800`…", then (because `_red` blocks exist) the **Yes / Info / No** chest opens.
- **③ Yes repaints** — click **Yes** → "Recoloured N block(s)"; after reload the placed `vart_red` background is **orange**, design intact.
- **④ Art + names follow** — Red Square + Red Triangle read `… [#FF8800]` and the icons are orange-tinted.
- **⑤ New variants use it** — Red Triangle on a different block → orange background.
- **⑥ Bad input bounces** — `… red banana` → "use #RRGGBB" · `… purple #112233` → "pick red/yellow/green/black". Nothing changes.
- **⑦ No keeps blocks** — `… green #00FFAA` → **No** → existing greens unchanged; new ones + art use `#00FFAA`.
- **⑧ Survives restart** — `/cb config hex` still shows your custom hexes (saved to `config.json`).

> ↩️ **Undo the test:** `/cb config hex red #EE3333` · `/cb config hex green #1E8C1E` (default art returns automatically).

---

## §C · Config-GUI hex editor + Color Studio  *(re-confirm)*

> 💡 **What it does:** edit the four hexes from the Config GUI (dyes → anvil), the pack reload waits
> for the recolour prompt, and `/cb customcolor` is a Color Studio handing out magic tool pairs in
> 29 presets or any hex.

> 🧰 **Before you start:** ≥1 `*_red` variant placed + a textured block to try the custom tools on.

**Try these:**

- **① Config → Variant Colours** — `/cb config` → glowing **Variant Colours** (red dye) → sub-GUI with four dyes, each showing its hex, default, and `_colour` block count.
- **② Edit a hex** — click **Red dye** → anvil "Set Red hex" → `#FF8800`, take output → same flow as the command (confirm; Yes/Info/No if `_red` placed; else back to the editor showing the new hex).
- **③ Bad input + cancel** — `banana` → "use #RRGGBB", back to editor · **ESC** → straight back, nothing changed.
- **④ Reload waits for the confirm** — with `_red` blocks: `/cb config hex red #22AAFF` → the Yes/Info/No opens **without** a reload first; reload only after you answer.
- **⑤ Color Studio opens** — `/cb customcolor` → 6-row studio, 29 colours as dyes (hex + RGB) + a glowing **Custom Hex…** button.
- **⑥ Preset → pair** — click **Purple** → "Gave you the Purple Square + Triangle (#7700CC)", two glinting purple-tinted tools; studio stays open.
- **⑦ Custom hex → pair** — **Custom Hex…** → `#FF1493` → a pair lands in inventory. `lavender` works; `banana` errors and returns.
- **⑧ Custom tools work** — Triangle on `foo` → `foo_hex_ff1493` (design intact); Square swaps the placed original; no variant → "make one with the Triangle first".
- **⑨ Direct form** — `/cb customcolor pink` · `#00CED1` · `banana` → pink pair · teal pair · error with suggestions.
- **⑩ Survives restart** — tools keep colour/name/tint + still work; the hex variant block + texture survive; `/cb config hex` keeps your edits.

---

# ✅ Passed — kept for re-test reference

## ✅ §D · Square swap (M3) + marker purge — 8/8 passed 2026-06-11

> Confirmed in-game 2026-06-11.  🟡 one PARTIAL: hotbar wording (works, reads rough — queued in §H).
> **What changed:** right-click a placed block with a **Square** → it changes IN PLACE to that
> colour's variant (made earlier by a Triangle). Squares never create; Black Square with no `_black`
> → back to base. Swapping is its own undo.

> 🧰 Setup: `vart` + `vart_red` + `vart_black` (Red/Black Triangles on a placed `vart`); grab all four Squares.

- ☑️ **1** Red Square on placed `vart` → `vart_red`, "Swapped to vart_red", no new item *(broken if old "tagged at (x,y,z)")*
- ☑️ **2** Black Square → `vart_black`
- ☑️ **3** Green Square (no variant) → "create one with the Green Triangle first"; `/cb list` shows no `vart_green`
- ☑️ **4** delete `vart_black`, Black Square → swaps back to base `vart`
- ☑️ **5** Red Square on a `vart_red` → "already vart_red", no change
- ☑️ **6** `/cb setglow vart_red 12`, swap a dark block to it → lights up at 12
- ☑️ **7** Square lore rewritten ("swap to its variant…"), no "tagging" lines
- ☑️ **8** Magic Items label = "Squares & Triangles — Triangles create colour variants, Squares swap them"

## ✅ §E · Triangle colour variants (M2) — 6/6 passed 2026-06-11

> Confirmed 2026-06-11. Right-click a placed block with a **Triangle** → creates a COPY with its
> background recoloured (`<id>_red`…); original untouched; the copy lands in inventory.

> 🧰 Setup: `/cb create vart "hi" <url>`, place it, grab a Red Triangle.

- ☑️ **1** Red Triangle on `vart` → `vart_red` item, same design red background, original unchanged
- ☑️ **2** sneak+right-click → Yes/Info/No chest; Yes creates, No does nothing
- ☑️ **3** same Triangle again → instant "already exists" + another item, no duplicate block
- ☑️ **4** Red Triangle on placed `vart_black` → `vart_red` (suffix swapped, not `vart_black_red`)
- ☑️ **5** Triangle on an untextured block → friendly error, nothing created
- ☑️ **6** `/cb undo` right after → variant removed

## ✅ §F · Retexture-all + source store — 6/6 passed 2026-06-11

> Confirmed 2026-06-11. A source store keeps each block's original image
> (`config/customblocks/sources/slot_N.src`) so it can be re-rendered **sharper**; a confirm GUI +
> `/cb retextureall [16-512]` batch-resize all existing blocks.

- ☑️ **1** `/cb config texturesize` → pick a size (≥1 block) → Yes/Info/No opens; Info shows count + est. pack size
- ☑️ **2** Yes → "complete — N re-rendered, N upscaled, N skipped"; placed blocks change resolution after one reload
- ☑️ **3** No → existing blocks unchanged, new size still saved for new textures
- ☑️ **4** 0 blocks → no confirm, straight back to Config
- ☑️ **5** new block (has source) genuinely **sharpens** going up; old block only **upscales** *(correct, not a bug)*
- ☑️ **6** `/cb retextureall 128` runs the same batch directly

> ⚠️ Known limits: pre-source blocks can't sharpen · Arabic/video blocks have no single source · delete-undo restores texture but not source.

## ✅ §G · Already working — re-test only if something feels off

| 🔎 Feature | Quick check |
|---|---|
| 🛠️ Omni-Tool (Glow/Hardness/Area) | `/cb omni` → right-click a block cycles the mode's effect |
| 🌬️ Sneak-click in the air | hold Omni-Tool, sneak+right-click nothing → config GUI opens |
| 🗑️ Deleter | `/cb deleter` → right-click wipes a block; `/cb undo` restores |
| 🟥 Squares | swap a placed block to its colour variant (§D) |
| 🎒 Magic Items GUI | `/cb` → Magic Items → double chest, all items, colour grid |
| 🏷️ `[CB]` prefix | chat + hotbar popup look identical |
| ✂️ Background Removal | smart black/white fill + tolerance cap (re-tested 2026-06-11) |
| 🖼️ Texture-Size picker | Config → Texture Quality OR `/cb config texturesize` → 6 sizes (16–512), hover MB, click sets |

---

## 🟡 §H · Polishing later  *(known, not bugs)*

- 🗨️ **Hotbar wording** (Square swap errors, tool feedback) — works, reads rough; **PARTIAL**, queued with chat-formatting polish.
- ✂️ **Bg removal fringe** (thin light halo on hard edges) + **low-contrast eat** (high strength) — **parked**; eyedropper/despeckle (§I) are the real fix.
- 💬 **Chat flooding** — still happening; PARTIAL, fix later.
- 🪟 **Partial GUIs** (Group 02 chests, Omni-Tool config GUI…) — final polish pass queued.
- 📜 **Item lore** — general tone pass queued ("marker"/"tagging" already purged).

---

## ⏳ §I · Coming next  *(not built — nothing to test)*

| | Feature | What it'll do |
|:--:|---|---|
| A | Eyedropper | click a block to strip that exact colour |
| B | Despeckle | auto-wipe leftover confetti specks |
| C | Preview | pick from a few strength results |
| — | Held-block glow | dynamic light on a held block (deferred) |
| — | Old `.dat` migration | 770 old high-res blocks → new scheme; **parked** (keep native 256, delete 20 junk test blocks) |

---

## 🆘 If a test fails, send me
- 🔢 the step number · 👀 what happened vs expected (📸 helps) · 📄 last ~20 lines of `.minecraft/logs/latest.log`

## 🧹 Cleanup
`/cb delete vart` · `/cb delete vart_red` · `/cb delete vart_black`
