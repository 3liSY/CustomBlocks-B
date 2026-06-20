# 🧪 Group 27 — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar` — rebuild + reinstall before testing if unsure.

**Legend:** 🎯 test now · ✅ confirmed working · 🟡 partial · ⏳ not built yet

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🔴 Not tested · 🟡 Partial · 🟢 All pass |
| **Progress** | 🟥🟥🟥🟥🟥 · 0 / 5 passed |
| **Last tested** | — |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | Screen | Open with | Status |
|:--:|---|---|---|
| 🎯 | **Block Creation Studio** | `/cb create` | **5 new fixes/upgrades — test this jar (§ below)** |
| ✅ | Arabic preview | `/cb arabic` | works · polish later (G27.8) |
| ✅ | Live recolour | `/cb livecolor <id>` | works · polish later (G27.8) · **to be renamed `/cb recolor` + folded into Studio (§G27.11, ⏳)** |
| ✅ | Eyedrop | `/cb eyedrop` | works · polish later (G27.8) |
| ✅ | HUD editor | `/cb edithud` | works · polish later (G27.8) |
| 🟡 | Shape editor | `/cb shapeeditor <id>` | named-shape picker works · carve later (G27.8) · **to be folded into Studio Shape section (§G27.11, ⏳)** |
| ⏳ | **Studio Edit Mode** | `/cb create` → Edit existing | not built |
| ⏳ | **Studio Paint** | `/cb paint` | not built |
| ⏳ | **Studio Recolor + Shape fold-in** | `/cb recolor`, `/cb shapeeditor` | not built (§G27.11) |
| ⏳ | **Screen toasts (no chat)** | every CB screen | not built (§G27.13) |

> ✅ The earlier studio build (opens · Identity · Texture URL · Shape · Attributes · category text · Create carries everything · `[?]`/dim/cancel · greyed FX/Behavior/Lore) you **already confirmed in-game**. **Don't re-test those.** This guide now lists only the **5 new fixes/upgrades** in this jar.

---

## 🎯 TEST NOW — Studio: the 5 new fixes (`/cb create`)

> Open with **`/cb create`**. The old `/cb create <id> <name> <url>` still works unchanged.
> Clean up after with `/cb delete <id>`.

### Do these in order

- **① Enter never publishes** 🔴 — open the studio, click into **any** field, type, press **Enter**
  - ✅ the block is **NOT** created. Enter just confirms the field. Only the **Create & Publish** button makes the block.
  - ❌ pressing Enter still creates/publishes a block.

- **② Hex box not blocked** 🟡 — Texture tab → look at the `#RRGGBB` field + **Use hex** button
  - ✅ the button sits **beside** the field, not on top of it; you can read/click both. Type `#33AAFF` + **Use hex** → cube background turns that colour.
  - ❌ the button overlaps the text box.

- **③ Background colour (behind the image)** 🔴 — Texture tab → **Load** an image with transparent areas (e.g. a logo PNG) → then click a colour swatch (or `#hex`)
  - ✅ the image **stays**; the colour fills **behind** its transparent parts (badge **image + background**). The little **✖** swatch (end of the row) clears the background. Picking a colour with **no** image = a solid-colour block (badge **background set**).
  - ❌ picking a colour **wipes** the image (old behaviour).
  - ➕ Create & Publish → the real block matches (background shows behind the logo).

- **④ UI upgrades** 🟡 — look around while filling sections
  - ✅ a left tab shows a green **✔** once that section has a real value (Identity done, Texture done, …).
  - ✅ each panel sits on a subtle dark **card**; hovering **Glow / Hardness / Sound / Collision** shows a one-line **hint** at the bottom of the panel.

- **⑤ Category tab** 🔴 (was "Organize") — click the **Category** tab
  - ✅ every existing category shows as a **chip** (★ marks the default, colour-tagged ones are tinted).
  - ✅ **click a chip** → assigns THIS block to it (chip goes gold).
  - ✅ type a name + **Add** (or Enter) → new chip, this block assigned to it (created on Publish).
  - ✅ select an existing category, then with a name typed click **Rename** → it renames server-wide (its blocks move); **Colour** → pick a colour, the chip tints; **Set Default** → ★ moves to it; **Delete** → asks **"Confirm delete?"**, second click removes it (its blocks go uncategorized).
  - ❌ any button does the wrong category, or Add silently renames an existing one.

### 📋 Scorecard

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | Enter never publishes (button-only create) |
| 🟥 | ② | hex field + button no longer overlap |
| 🟥 | ③ | **background colour fills behind the image** (✖ clears; colour-only = solid) |
| 🟥 | ④ | tab ✔ ticks · panel cards · attribute hover hints |
| 🟥 | ⑤ | **Category tab: assign · add · rename · colour · default · delete** |
| — | **0 / 5** | |

> 💡 ③ and ⑤ are the big ones. For ⑤, the rename/colour/default/delete act on **real server categories** (they affect other blocks too) — that's intended.

---

## ⏳ WHEN BUILT — Recolor + Shape fold-in (§G27.11)

> Not built yet. These are the tests for when it lands. Open with `/cb recolor` / `/cb shapeeditor`.
> Needs the Studio (`/cb create`) + Edit Mode foundation first. Clean up with `/cb delete <id>`.

### Do these in order

- **① `livecolor` is gone** ⏳ — type `/cb livecolor someblock`
  - ✅ unknown command (no hidden alias). `/cb recolor someblock` works instead.
  - ❌ `/cb livecolor` still runs.

- **② `/cb recolor <id>` opens the Studio** ⏳ — `/cb recolor <existing block>`
  - ✅ the **Block Studio** opens in **Edit** mode with the **Recolor** section focused — HSL sliders + tone tools, not the old standalone screen.
  - ❌ the old standalone recolour screen opens, or an error.

- **③ `/cb recolor` (no id) → picker** ⏳ — `/cb recolor`
  - ✅ a chest block-picker opens; clicking a block opens the Studio Recolor section for it.

- **④ Recolor is greyed until there's a texture** ⏳ — `/cb create` → click the **Recolor** tab on a blank new block
  - ✅ Recolor is **greyed/locked** with a "load a texture first" message. Load a URL + set an id → Recolor **unlocks**.
  - ✅ In **Edit** mode (existing block) Recolor is live straight away.

- **⑤ Live preview, bake on Save** ⏳ — in Edit mode, drag Hue/Sat/Light + tone tools
  - ✅ the 3D cube recolours **live** as you drag. The **placed block in the world does NOT change yet.**
  - ✅ click **Save changes** → the real block re-textures **once**, **no purple flash**.
  - ❌ the world block flickers purple / re-bakes on every drag.

- **⑥ The 4 upgrades** ⏳ — in the Recolor section
  - ✅ **Before/after split** preview toggle · **Save/Load preset** · **Eyedropper match** · **Per-zone tint** (shadows/mids/highlights) all present and working.

- **⑦ `/cb shapeeditor <id>` opens the Studio Shape section** ⏳ — `/cb shapeeditor <existing block>`
  - ✅ the **Studio** opens in Edit mode on the existing **Shape** section (chip picker). The old standalone shape screen is gone.
  - ❌ the old standalone shape editor opens.

- **⑧ Chest "Recolor" buttons** ⏳ — open a block's chest editor / colours menu
  - ✅ the button reads **"Recolor"** (not "Live Recolour") and opens the Studio on the Recolor section.

### 📋 Scorecard (§G27.11)

| ✓ | # | Proves |
|:--:|:--:|---|
| ⏳ | ① | `/cb livecolor` retired, `/cb recolor` works |
| ⏳ | ② | `/cb recolor <id>` → Studio Edit, Recolor section |
| ⏳ | ③ | no-arg recolor → picker → Studio |
| ⏳ | ④ | Recolor greyed until texture (Create) · live in Edit |
| ⏳ | ⑤ | **live preview, single bake on Save, no purple** |
| ⏳ | ⑥ | split preview · presets · eyedropper match · per-zone tint |
| ⏳ | ⑦ | `/cb shapeeditor <id>` → Studio Shape section |
| ⏳ | ⑧ | chest buttons relabelled + routed |
| — | **0 / 8** | |

---

## ⏳ WHEN BUILT — Screen toasts replace chat (§G27.13)

> Not built yet. Tests for when it lands. Goal: clicking inside a CB screen pops a **top-right toast**,
> chat stays empty. Open any screen (`/cb create`, `/cb edithud`, `/cb arabic`, …) to test.

### Do these in order

- **① Click feedback is a toast, not chat** ⏳ — open `/cb create`, do something with feedback (e.g. a "coming soon" button, Save, copy)
  - ✅ a **box pops top-right**; **chat stays empty**.
  - ❌ the message shows up in chat.

- **② Errors toast too** ⏳ — trigger a failure in a screen (e.g. HUD preset → Save with an empty name → "Enter a name first")
  - ✅ the error appears as a **red-tinted toast** top-right, not chat.

- **③ Works on every screen** ⏳ — repeat in `/cb edithud` (preset load/save), `/cb arabic`, `/cb eyedrop`
  - ✅ each screen's click feedback toasts; none of them write chat.

- **④ Typed commands still use chat** ⏳ — close screens, run `/cb setglow <id> 8` in chat
  - ✅ the **command** still replies in chat as before (only *screen* clicks moved to toasts).
  - ❌ command replies vanished or moved to a toast.

### 📋 Scorecard (§G27.13)

| ✓ | # | Proves |
|:--:|:--:|---|
| ⏳ | ① | screen clicks toast top-right, chat stays empty |
| ⏳ | ② | errors toast too (correct colour) |
| ⏳ | ③ | all CB screens covered |
| ⏳ | ④ | typed server commands still reply in chat |
| — | **0 / 4** | |

---

## ⏳ Not built

- **Recolor + Shape fold-in (§G27.11)** — rename `/cb livecolor` → `/cb recolor`; fold recolour into a new
  Studio sidebar section (HSL + tone + split-preview / presets / eyedropper-match / per-zone tint, greyed
  until a texture exists, live preview + bake-on-Save); `/cb shapeeditor` opens the Studio's existing Shape
  section; retire both standalone screens; relabel + reroute the chest buttons. *(Tests above.)*
- **G27.8 polish pass** — the gold-on-black "masterpiece" restyle + movable bars + Settings for **all five** older screens.
- **Studio — more** — FX (emissive/pulse/glint), Behavior (gravity/bounce/slippery), Lore/attribution *(all need new block data)*; the big overlays (Library/Material/Surprise/Share/Settings/Ctrl+K), session memory, undo/redo.
- **Studio Edit Mode** — `/cb create` → **Edit existing**: pick a block and edit **all** its settings in place — re-ID, Save changes / Save as copy, draft, locked-block guard.
- **Studio Paint** — `/cb paint`: a full-screen **pixel editor** to hand-draw a block's texture (pen / eraser / fill / eyedrop / line / rect / mirror-symmetry / brush size / undo) with a full colour picker, transparency, zoom/pan/grid, and a trace underlay.
- **Shape carve** — freeform custom-box shape editor (its own backend session).
- **Screen toasts (§G27.13)** — real top-right `CbToast` replaces every in-screen chat message; reroute the
  fake `toast()` (HudPresetBrowser), the Studio "coming soon"/GIF stubs, and all Save-flash feedback. Typed
  server commands stay in chat. *(Tests above.)*

---

## 🆘 If a test fails

- 🔢 Step number (e.g. ⑧)
- 👀 What happened vs expected (📸 helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup

```
/cb delete mega
/cb delete spine
```
