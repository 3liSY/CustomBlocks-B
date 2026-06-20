# 🧪 Group 03 — HUD Overlay & ESC-Menu Buttons — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Partial |
| **Progress** | 🟥🟥 · 0 / 2 passed |
| **Last tested** | — |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | Two easy-to-miss double-checks: **persistence across restart** + **editor Cancel-revert** | §1 |
| ✅ Working | HUD overlay (id + name) · `/cb config hud` toggle · `/cb edithud` drag editor · ESC-menu buttons | §2 |
| 🟡 Polish later | HUD UI polish pass · colour is a fixed 8-preset cycle (no RGB picker) | §3 |

---

# 🎯 Test now

## §1 · Two things worth a double-check

> 💡 **What it does:** the HUD's saved look should survive a restart, and Cancelling the editor
> should throw away unsaved changes.

> 🧰 **Before you start:**
> - `/cb create g03a HudTestBlock`
> - `/cb give g03a`, place it, look at it

**Try these:**

- **① Persistence survives a restart** 🔴
  `/cb edithud` → set Scale to **1.5x** → Save
  - ✅ **Pass:** fully quit to title → relaunch → rejoin → `/cb edithud` still reads **1.5x**.
  - ❌ **Broken if:** it reset to 1.0.

- **② Cancel reverts** 🔴
  `/cb edithud` → change scale/colour, drag the box → click **Cancel** (or Esc) → reopen
  - ✅ **Pass:** everything is back to before.
  - ❌ **Broken if:** unsaved changes stuck.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | scale 1.5 survives a full restart |
| 🟥 | ② | Cancel/Esc discards unsaved editor changes |
| — | **0 / 2** | |

> ↩️ **Undo the test:** `/cb delete g03a`  *(optionally delete the HUD config file to reset the HUD look)*

---

# ✅ Passed — re-check only if something feels off

## ✅ §2 · Confirmed working

| 🔎 Feature | Quick check |
|---|---|
| 👁️ HUD overlay | look at `g03a` → two lines: `g03a` + `HudTestBlock`; hides when you look away |
| 🔀 HUD toggle | `/cb config hud toggle` → `[CB] HUD disabled/enabled`; bare `/cb config hud` shows status |
| ✋ Drag editor | `/cb edithud` → overlay (world stays visible), drag preview + Save → live HUD moves |
| ⎋ ESC buttons | Esc → two gray Command-Block buttons below "Leave Game": **CustomBlocks Menu** (→ dashboard) + **HUD Editor** (→ editor) |
| 🚫 No leak | the two buttons appear only on the pause menu — not title/options |

---

## 🟡 §3 · Polish later

- 🎨 **HUD UI** — works, but a polish pass + revisit is queued before it's "complete".
- 🌈 **Colour** — a fixed 8-preset cycle (White→Yellow→Green→Aqua→Red→Pink→Gold→Gray), not a free RGB picker.
- 🖥️ **HUD look is client-side** — position/scale/colour/opacity live in the client's run dir; the server only drives on/off. (Each player keeps their own look on a dedicated server.)

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs expected (📸 screenshot helps for HUD/editor/ESC visuals)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`
- 💾 For persistence: the contents of the HUD config file

## 🧹 Cleanup
`/cb delete g03a`  *(optionally reset your HUD settings file if you want the defaults back)*
