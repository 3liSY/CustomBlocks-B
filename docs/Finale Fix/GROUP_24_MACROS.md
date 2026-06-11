# Group 24 — Macros & Scripts

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 13 (Macros) build-verified.
>
> **Objective:** Improve the existing macro recorder/player system and restore the extended Script GUI (`/cb scriptgui`) — an advanced macro system with conditional logic and loops.
>
> **Source issues:** Group M partial (script/scriptgui — "extended macro system with GUI"), existing Phase 13 macro system improvements needed
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / improves

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Macro record | `/cb macro record <name>` | Working | Preserved |
| Macro play | `/cb macro play <name>` | Working | Preserved |
| Macro GUI | `/cb gui macros` — `MacroListScreen` (screen-based) | Screen-based | Chest GUI version |
| Script GUI | `/cb scriptgui` — extended macro with conditions/loops | Missing | Restored |
| `/cb script <name>` | Run a script by name | Missing | Restored |
| Macro on/off | `/cb on` / `/cb off` — toggle macro system | Missing | Restored |
| Script persistence | `config/customblocks/macros/<name>.json` | Works for macros | Extended for scripts |

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
| Script GUI | `/cb scriptgui` |
| Script run | `/cb script <name>` |
| Macro toggle | `/cb on` / `/cb off` |

---

## Implementation Requirements

### 1. Macro System (Existing — Improvements)

The Phase 13 macro system is functional. Improvements needed:
- **Macro GUI → chest GUI**: `MacroListScreen` must be chest-based (not screen-based).
- **Macro step names**: Each step in the macro should have a descriptive label (auto-generated from the command).
- **Macro undo**: Running a macro pushes one undo entry for the entire batch.

### 2. `/cb on` and `/cb off`

Global toggle for the macro system:
- `/cb on` — enables all macro recording and playback (default state).
- `/cb off` — disables all macro recording and playback. Running `/cb macro play` while off shows: `"Macro system is disabled. Use /cb on to re-enable."`

### 3. Script GUI — Extended Macro System

`/cb scriptgui` opens a chest GUI script editor with:
- **Step list** (left 4 columns): list of steps (each slot = one step). Steps can be:
  - `/cb` command
  - `wait <ticks>` — pause before next step
  - `if <condition> <cmd>` — conditional (e.g., `if block:g05a exists /cb give g05a`)
  - `loop <N>` / `end_loop` — repeat a block of steps N times
- **Add step** slot → opens anvil GUI to type a step
- **Delete step** slot → click a step then delete
- **Run** slot → executes the script immediately
- **Save** slot → saves script to `config/customblocks/macros/<name>.json`
- **Clear all** slot

### 4. Script Persistence

Scripts saved to `config/customblocks/macros/<name>.json` in the same format as macros, but with extended step types (`wait`, `if`, `loop`).

`/cb script <name>` runs a saved script.

### 5. Macro GUI (Chest-Based)

`/cb gui macros` opens a chest GUI:
- Each slot = one saved macro/script.
- Click → sub-menu: Play, Edit (opens scriptgui with this macro), Delete, Info.
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

## Test G26.4 — `/cb off` disables macros

```
/cb off
/cb macro play test-macro
```

**Expected:** `"Macro system is disabled. Use /cb on to re-enable."`

**Pass:** Macro system disabled, correct message.
**Fail:** Macro runs despite `/cb off`.

---

## Test G26.5 — `/cb on` re-enables macros

```
/cb on
/cb macro play test-macro
```

**Expected:** Macro plays again.

**Pass:** Re-enabled correctly.
**Fail:** Still disabled after `/cb on`.

---

## Test G26.6 — Script GUI opens

```
/cb scriptgui
```

**Expected:** Chest GUI opens with step list area, add step, delete step, run, save, and clear all slots.

**Pass:** Script GUI opens.
**Fail:** Command missing, error, or screen-based UI.

---

## Test G26.7 — Add steps to script

In the Script GUI:
1. Click "Add step" → anvil opens → type `/cb setglow g26a 12` → confirm.
2. Click "Add step" → type `wait 20` → confirm.
3. Click "Add step" → type `/cb setglow g26b 6` → confirm.

**Expected:** Three step slots visible in the step list area.

**Pass:** All 3 steps appear as slots.
**Fail:** Steps not added, or step list empty.

---

## Test G26.8 — Run script from GUI

In the Script GUI, click "Run".

**Expected:** g26a glow sets to 12. Waits 1 second (20 ticks). g26b glow sets to 6.

**Pass:** Script executes steps in order with delay.
**Fail:** Error, wrong order, or delay not respected.

---

## Test G26.9 — Save and run saved script

In the Script GUI, click "Save". Enter name "test-script" in anvil.

```
/cb script test-script
```

**Expected:** Script runs again — g26a glow 12, wait, g26b glow 6.

**Pass:** Script runs correctly from command.
**Fail:** Command missing, or script not found.

---

## Group 24 Verdict

| Test | Description | Result |
|---|---|---|
| G26.1 | Macro record and play | ⬜ |
| G26.2 | Macro is single undo entry | ⬜ |
| G26.3 | Macro GUI is chest-based | ⬜ |
| G26.4 | `/cb off` disables macros | ⬜ |
| G26.5 | `/cb on` re-enables macros | ⬜ |
| G26.6 | Script GUI opens | ⬜ |
| G26.7 | Steps added to script | ⬜ |
| G26.8 | Script runs steps in order with delay | ⬜ |
| G26.9 | Saved script runs via `/cb script` | ⬜ |

**Group 24 passes when macros and scripts both work correctly in-game with chest GUIs.**

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
(Delete test-script via `/cb gui macros`)
