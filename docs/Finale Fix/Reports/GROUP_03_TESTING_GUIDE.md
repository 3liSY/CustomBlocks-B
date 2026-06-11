# Group 03 — HUD Overlay & ESC-Menu Buttons · Testing Guide

*One green build proves it compiles — nothing is done until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ confirmed  ·  🟡 polish later  ·  ⏳ not built

---

## Where Group 03 stands

> ☑️ **Working (confirmed in-game)** — HUD overlay (id + name), `/cb config hud` toggle, `/cb edithud`
> drag editor, the two ESC-menu buttons
> 🎯 **Double-check** — persistence across restart + editor Cancel-revert (easy to miss) → §1
> 🟡 **Polish later** — HUD UI polish + a proper revisit; colour is a fixed 8-preset cycle (no RGB picker)

---

## 🎯 §1 · Two things worth a double-check

**Setup:** `/cb create g03a HudTestBlock` · `/cb give g03a`, place it, look at it.

**1 — Persistence survives a restart**
```
/cb edithud      → set Scale to 1.5x → Save
```
Fully quit to title, relaunch, rejoin → `/cb edithud` still reads **1.5x**
(`config/customblocks/data/hud-config-server.json` → `"hudScale": 1.5`). *Broken if:* it reset to 1.0.

**2 — Cancel reverts** — `/cb edithud`, change scale/colour/drag the box, click **Cancel** (or Esc),
reopen → everything back to before. *Broken if:* unsaved changes stuck.

| ✓ | Step |
|---|---|
| ⬜ | 1 — scale 1.5 survives a full restart |
| ⬜ | 2 — Cancel/Esc discards unsaved editor changes |

---

## ☑️ §2 · Already working — re-test only if something feels off

| Feature | Quick check |
|---|---|
| HUD overlay | look at `g03a` → two lines: `g03a` + `HudTestBlock`; hides when you look away |
| HUD toggle | `/cb config hud toggle` → `[CB] HUD disabled/enabled`; bare `/cb config hud` shows status |
| Drag editor | `/cb edithud` → overlay (world stays visible), drag preview + Save → live HUD moves |
| ESC buttons | Esc → two gray Command-Block-icon buttons below "Leave Game": **CustomBlocks Menu** (→ dashboard) + **HUD Editor** (→ editor) |
| No leak | the two buttons appear only on the pause menu, not title/options |

---

## 🟡 §3 · Polishing later (known, not bugs)
- **HUD UI** — works, but a polish pass + revisit is queued before it's "complete".
- **Colour** — fixed 8-preset cycle (White→Yellow→Green→Aqua→Red→Pink→Gold→Gray), not a free RGB picker.
- **HUD look is client-side** — position/scale/colour/opacity live in the client's run dir; the server
  only drives on/off. (Each player keeps their own look on a dedicated server.)

---

## If a test fails
Send: the step number, expected vs actual (screenshot for HUD/editor/ESC visuals), the relevant
`logs/latest.log` lines (esp. any `Exception`/mixin error naming `ScreenInvoker`, `HudEditorScreen`,
`EscMenuButtons`), and — for persistence — the contents of `hud-config-server.json`.

## Cleanup
```
/cb delete g03a
```
*(Optionally delete `config/customblocks/data/hud-config-server.json` to reset HUD settings.)*
