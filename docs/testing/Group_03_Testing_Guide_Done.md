# Group 03 — HUD Overlay

**🎯 Active:** A HUD widgets — now **2** widgets, not 3  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                              |
| --------------- | ----------------------------- |
| **Verdict**     | ✅ DONE — passed MP 2026-07-15. A0 (silent HUD) + A3 (padlock) confirmed in-game. Group closed; G03-BULKLOCK-HUD handed to the bulk owner |
| **Progress**    | ✅✅✅ · 100% (3/3)             |
| **Last tested** | 2026-07-15 (MP)               |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                  | State       |
| --- | ------------------------------------- | ----------- |
| A | HUD widgets (display-only, 2 widgets) | ✅ done |

🔗 **Related Doc:** [GROUP_03_HUD_ESC.md](../groups/GROUP_03_HUD_ESC.md) · **ADR:** [ADR-017](../adr/ADR-017-cb-overlay-layer-not-chat-rewrite.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g03a HudTest`

## A · HUD widgets · 🎯 test-now

> 💡 Both widgets are **display-only** — they render state and are never clicked; each widget's action stays its existing command.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A0 | Set an Omni-Tool mode (e.g. GLOW), put the tool away, play normally for a minute | HUD is silent and empty — **nothing bottom-left, ever again**, nothing top-right, no blips, no ESC panel | ✅ | ✅ |
| A3 | `/cb lock g03a`, aim at that block | a **7×9 hand-drawn pixel padlock** in CB red — **10px BELOW the crosshair, horizontally centred**, never on it. Clears on `/cb unlock` | ✅ | ✅ |
| A2 | `/cb macro record m1`, then `/cb macro stop` | banner pulses red/white while recording; clears the instant recording stops | ✅ | ✅ |

> 🔒 A3's sprite is owner-approved: 7×9, CB red, highlight column on the lit side, darker right/bottom edge,
> ownerless (locked is locked). It is **drawn pixel-by-pixel, not a font glyph** — the 🔒 glyph is a
> missing-glyph risk on any pack whose font lacks it (same reason the G27 Bulk grid draws its own).
> 💡 A0 was the active-tool chip that never cleared; the widget is now deleted, which IS the fix.

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **Persistence + Cancel-revert** (Passed 2026-06-28). Superseded by Group 27.
- **HUD overlay + ESC buttons** (Confirmed). Superseded by Group 27.

</details>

<details><summary>🗑️ <b>Deleted Tests</b> (click to open)</summary>

All cut after an MP test run. Not deferred; do not re-add. See ADR-017.

- **A1 Active-tool chip** — cut 2026-07-15 (owner: "the Omni-Tool is kinda mid and won't be used" — it doesn't
  earn a permanent screen widget). The chip is gone; the tool now speaks on the hotbar, once, on mode switch
  (Group 04 §F). This cut is also what makes A0 pass.
- **ESC Panel Dynamic Shortcuts** — feature cut entirely.
- **A4 Buzzer timer** — stub-backed, no backend ever existed; only renderable behind a launch flag.
- **A5 Incidents ping** — the corner ping + blip. `IncidentRecorder` is untouched and still records; read it with `/cb incidents`.
- **A6 Vault upload toast** — stub-backed, same as the Buzzer.
- **A7 Favourites strip** — confusing, cut.
- **B1/B2 ESC quick-access panel** — the whole panel, its thumbnails, and its force-switch.
- **B3 Op-force widget pin** — `/cb edithud force` is gone.
- **B4 HUD-layout share code** — never built; cut rather than carried.
- **C1 Widgets tab** — no per-player widget layout exists any more; geometry lives in `HudWidgetType`.
- **C2 Per-widget colour picker** — colour is a mod-wide look, not a per-widget knob.
- **C3 Dev-stub gating** — `-Dcustomblocks.devStubs=true` no longer exists.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| G03-BULKLOCK-HUD | A3 | `/cb bulklock` never calls `WidgetSync.pushAll`, so a bulk lock's padlocks don't appear until something else pushes — single `/cb lock` does push. Surfaced 2026-07-15 while building the G04 undo pass (undoing a bulk lock *does* push, which makes the gap visible). One line in `BulkFlagCommands`. | ⚠️ |

---
