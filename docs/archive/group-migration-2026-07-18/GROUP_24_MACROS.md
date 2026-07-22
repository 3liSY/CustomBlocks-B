# Group 24 — Macros

> ⛔ **SCRIPTS SCRAPPED (sweep 2026-06-21).** Per SWEEP_INDEX §A, `script`/`scriptgui`/`run` are **removed** —
> the macro system covers these needs. §3–§4 and tests G26.6–G26.9 are struck below; do not build them.
> 🔧 **Toggle renamed:** the macro on/off toggle is now `/cb macro on` / `/cb macro off` (the bare `/cb on` /
> `/cb off` collided with the G03 HUD toggle — decision D, 2026-06-21).
>
> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 13 (Macros) build-verified.
>
> ⚠️ **UI medium audit (2026-07-10):** Screen-migration decision locked mod-wide — `MacroListScreen` STAYS
> Screen-based. Earlier "must be chest-based" direction below was backwards vs. the mod-wide Screen
> migration and is reverted. Do not convert it to a chest GUI.
>
> **Objective:** Improve the existing macro recorder/player system (Screen-based GUI, step labels, single-undo batch).
>
> **Source issues:** existing Phase 13 macro system improvements needed
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / improves

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Macro record | `/cb macro record <name>` | Working | Preserved |
| Macro play | `/cb macro play <name>` | Working | Preserved |
| Macro GUI | `/cb gui macros` — `MacroListScreen` (screen-based) | Screen-based | Keep Screen-based |
| Macro on/off | `/cb macro on` / `/cb macro off` — toggle macro system | Missing | Restored (renamed from `/cb on`/`/cb off`) |
| ~~Script GUI / `script` / `run`~~ | ~~extended macro with conditions/loops~~ | ⛔ **SCRAPPED (SWEEP_INDEX §A)** | — |

---

## What this group covers

| Feature | Commands |
|---|---|
| Macro record | `/cb macro record <name>` |
| Macro add step | `/cb macro add <cmd>` |
| Macro stop | `/cb macro stop` |
| Macro play | `/cb macro play <name>` |
| Macro list | `/cb macro list` |
| Macro delete | `/cb macro delete <name>` |
| Macro GUI | `/cb gui macros` → `MacroListScreen` (Screen-based) |
| Macro toggle | `/cb macro on` / `/cb macro off` |
| ~~Script GUI / run~~ | ⛔ **SCRAPPED** — `scriptgui`/`script` removed (§A) |

---

## Implementation Requirements

### 1. Macro System (Existing — Improvements)

The Phase 13 macro system is functional. Improvements needed:
- **Macro GUI stays Screen-based**: `MacroListScreen` is kept as-is (Screen, not chest).
- **Macro step names**: Each step in the macro should have a descriptive label (auto-generated from the command).
- **Macro undo**: Running a macro pushes one undo entry for the entire batch.

### 2. `/cb macro on` and `/cb macro off`

Global toggle for the macro system (renamed from bare `/cb on`/`/cb off` — collided with G03 HUD toggle):
- `/cb macro on` — enables all macro recording and playback (default state).
- `/cb macro off` — disables all macro recording and playback. Running `/cb macro play` while off shows: `"Macro system is disabled. Use /cb macro on to re-enable."`

### 3–4. ~~Script GUI / persistence~~ — ⛔ SCRAPPED

`scriptgui`/`script`/`run` (conditional/loop "extended macro") **removed** per SWEEP_INDEX §A. Plain macros
(record → add → play, single-undo batch) cover the use case. Original script spec deleted.

### 5. Macro GUI (Screen-Based) — moved to G27

`/cb gui macros` opens `MacroListScreen` — full spec moved to `GROUP_27_SCREENS.md` §G27.24 (2026-07-12,
screen-content consolidation). G24 owns the macro record/play/list/delete *logic* (§1 above); the screen
chrome is G27's.

---

## Setup

```
/cb create g26a MacroTest
/cb create g26b MacroTarget
```

---

## Test G26.1 — Macro record and play (existing)

```
/cb macro record test-macro
/cb macro add /cb setglow g26a 8
/cb macro add /cb setglow g26b 4
/cb macro stop
/cb macro play test-macro
```

**Expected:** g26a has glow 8, g26b has glow 4.

**Pass:** Macro plays both steps correctly.
**Fail:** Steps not executed, or only one step fired.

---

## Test G26.2 — Macro is a single undo entry

```
/cb undo
```

**Expected:** Both glow changes reverted in one undo. `Undid macro "test-macro" (2 steps).`

**Pass:** Single undo reverts the whole macro.
**Fail:** Multiple undos required.

---

## Test G26.3 — Macro GUI is Screen-based

```
/cb gui macros
```

**Expected:** `MacroListScreen` opens. "test-macro" visible as a row. Sub-menu on click: Play, Edit, Delete, Info. "Record New" control at top.

**Pass:** Screen opens, all elements present.
**Fail:** Chest GUI opens instead, or elements missing.

---

## Test G26.4 — `/cb macro off` disables macros

```
/cb macro off
/cb macro play test-macro
```

**Expected:** `"Macro system is disabled. Use /cb macro on to re-enable."`

**Pass:** Macro system disabled, correct message.
**Fail:** Macro runs despite `/cb macro off`.

---

## Test G26.5 — `/cb macro on` re-enables macros

```
/cb macro on
/cb macro play test-macro
```

**Expected:** Macro plays again.

**Pass:** Re-enabled correctly.
**Fail:** Still disabled after `/cb macro on`.

---

## Tests G26.6–G26.9 — ⛔ SCRAPPED (scripts removed)

Script GUI / add-step / run / saved-script tests are **removed** per SWEEP_INDEX §A (`scriptgui`/`script`/`run`
scrapped). Skip. G24 scope = macros only.

---

## Group 24 Verdict

| Test | Description | Result |
|---|---|---|
| G26.1 | Macro record and play | ⬜ |
| G26.2 | Macro is single undo entry | ⬜ |
| G26.3 | Macro GUI is Screen-based | ⬜ |
| G26.4 | `/cb macro off` disables macros | ⬜ |
| G26.5 | `/cb macro on` re-enables macros | ⬜ |
| ~~G26.6–G26.9~~ | ~~script GUI / run~~ | ⛔ SCRAPPED — scripts removed (§A) |

**Group 24 passes when macros work correctly in-game with the Screen-based GUI.**

If anything shows ❌ — paste:
1. The exact commands typed
2. Which steps executed vs expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g26a
/cb delete g26b
/cb macro delete test-macro
```
