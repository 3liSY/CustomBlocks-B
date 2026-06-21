# Group 24 — Macros

> ⛔ **SCRIPTS SCRAPPED (sweep 2026-06-21).** Per SWEEP_INDEX §A, `script`/`scriptgui`/`run` are **removed** —
> the macro system covers these needs. §3–§4 and tests G26.6–G26.9 are struck below; do not build them.
> 🔧 **Toggle renamed:** the macro on/off toggle is now `/cb macro on` / `/cb macro off` (the bare `/cb on` /
> `/cb off` collided with the G03 HUD toggle — decision D, 2026-06-21).
>
> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 13 (Macros) build-verified.
>
> **Objective:** Improve the existing macro recorder/player system (chest GUI, step labels, single-undo batch).
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
| Macro GUI | `/cb gui macros` — `MacroListScreen` (screen-based) | Screen-based | Chest GUI version |
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
| Macro GUI | `/cb gui macros` → chest GUI |
| Macro toggle | `/cb macro on` / `/cb macro off` |
| ~~Script GUI / run~~ | ⛔ **SCRAPPED** — `scriptgui`/`script` removed (§A) |

---

## Implementation Requirements

### 1. Macro System (Existing — Improvements)

The Phase 13 macro system is functional. Improvements needed:
- **Macro GUI → chest GUI**: `MacroListScreen` must be chest-based (not screen-based).
- **Macro step names**: Each step in the macro should have a descriptive label (auto-generated from the command).
- **Macro undo**: Running a macro pushes one undo entry for the entire batch.

### 2. `/cb macro on` and `/cb macro off`

Global toggle for the macro system (renamed from bare `/cb on`/`/cb off` — collided with G03 HUD toggle):
- `/cb macro on` — enables all macro recording and playback (default state).
- `/cb macro off` — disables all macro recording and playback. Running `/cb macro play` while off shows: `"Macro system is disabled. Use /cb macro on to re-enable."`

### 3–4. ~~Script GUI / persistence~~ — ⛔ SCRAPPED

`scriptgui`/`script`/`run` (conditional/loop "extended macro") **removed** per SWEEP_INDEX §A. Plain macros
(record → add → play, single-undo batch) cover the use case. Original script spec deleted.

### 5. Macro GUI (Chest-Based)

`/cb gui macros` opens a chest GUI:
- Each slot = one saved macro/script.
- Click → sub-menu: Play, Edit (re-record / edit steps), Delete, Info.
- "Record New" slot at top.
- "Enable/Disable" toggle slot.

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

## Test G26.3 — Macro GUI is chest-based

```
/cb gui macros
```

**Expected:** Chest GUI opens. "test-macro" visible as a slot. Sub-menu on click: Play, Edit, Delete, Info. "Record New" slot at top.

**Pass:** Chest GUI, all elements present.
**Fail:** Screen-based MacroListScreen opens.

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
| G26.3 | Macro GUI is chest-based | ⬜ |
| G26.4 | `/cb macro off` disables macros | ⬜ |
| G26.5 | `/cb macro on` re-enables macros | ⬜ |
| ~~G26.6–G26.9~~ | ~~script GUI / run~~ | ⛔ SCRAPPED — scripts removed (§A) |

**Group 24 passes when macros work correctly in-game with a chest GUI.**

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
