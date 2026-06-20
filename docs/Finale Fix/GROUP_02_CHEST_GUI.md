# Group 02 — Chest GUI Core Infrastructure

> **Prerequisite:** Group 01 (Legacy Audit) signed off. Phases 0–16 build-verified.
>
> **Objective:** Replace all screen-based interfaces with a chest-based GUI system. Restore the original stained-glass visual theme (modernized). All GUI commands, navigation, back-stack, and editor access must work correctly.
>
> **Source issues:** 17.3, 17.5, Group L (listgui, menu, undogui, redogui, history, magicitems, editmagicitems, editor, rp pause/resume, sync, unsuppress), Group N (search GUI, config GUI regressions)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing. No screen-based GUI is acceptable — every GUI must be chest-based.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Main menu GUI | Chest GUI with stained glass slots | Screen-based main menu (Block List / Categories / Templates / etc.) | Chest GUI restored, modernized visual theme |
| `/cb menu` / `/cb dashboard` | Opened chest GUI | Did not exist | Restored as aliases for `/cb` |
| `/cb editor <id>` | Opened block editor chest GUI directly | Missing (only `/cb gui block <id>` existed) | ~~Restored as chest shortcut~~ → **SUPERSEDED 2026-06-20: opens the screen-based Block Studio Edit tab (§G27.12)** |
| Block editor | Chest GUI | Screen-based BlockEditorScreen | ~~Chest GUI~~ → **Block Studio screen, Edit mode (§G27.12)** — dev "replace" decision 2026-06-20 |
| Undo browser | `/cb undogui` opened chest GUI | Not present | Restored |
| History | `/cb history` showed mutation log | Missing | Restored |
| rp pause/resume | `/cb rp pause` / `/cb rp resume` | Missing | Restored |
| sync | `/cb sync` force-synced pack to all clients | Missing | Restored |
| Navigation | Back-stack via chest slot | Screen back button | Chest-based back-stack |
| Visual theme | Stained glass style | Plain screen buttons | Stained glass, polished/modernized |

---

## What this group covers

| Feature | Commands |
|---|---|
| Main menu | `/cb`, `/cb menu`, `/cb dashboard`, `/cb gui` |
| Block editor | `/cb editor <id>`, `/cb gui block <id>` |
| GUI list | `/cb listgui` |
| Undo/redo browser | `/cb undogui`, `/cb redogui` |
| History log | `/cb history` |
| Magic items | `/cb magicitems`, `/cb editmagicitems` |
| RP control | `/cb rp pause`, `/cb rp resume`, `/cb sync` |
| Suppression | `/cb unsuppress` |
| Navigation | Back-stack across all chest GUIs |

---

## Implementation Requirements

### 1. Chest GUI Framework

All GUIs use chest-based containers (1–6 rows). No screen-based interfaces remain anywhere in the mod.

- Visual theme: stained glass slots for navigation/decoration, item slots for actions.
- Color coding: green = confirm/create, red = delete/cancel, yellow = edit/modify, gray = navigate/info.
- Every chest GUI has a back slot (arrow left in top-left or bottom-left corner) that returns to the previous GUI in the back-stack.

### 2. Access commands

`/cb` → opens main dashboard chest GUI.
`/cb menu` and `/cb dashboard` are aliases for `/cb`.
`/cb gui` is also an alias.

### 3. Block editor access

> **SUPERSEDED — 2026-06-20 (developer "replace" decision).** The block editor is no longer
> chest-based. `/cb editor <id>` now opens the **screen-based Block Studio in Edit mode**
> (full spec: `GROUP_27_SCREENS.md §G27.12`). This subsection is kept for history; the chest
> editor it describes is retired. Only `/cb editor` is rewired this pass — the `/cb gui block <id>`
> and ESC-menu redirects are deferred to a later pass.

`/cb editor <id>` opens the **Block Studio Edit tab** for the block (see §G27.12). This is the primary shortcut.
`/cb gui block <id>` remains valid (still routes the old path until redirected later).
Tab-complete for `<id>` works on both forms.

### 4. GUI navigation back-stack

The back-stack is serialized to `GuiState` and survives accidental GUI close/reopen. The back slot always returns to the exact previous state (same page, same scroll position if applicable).

### 5. Undo/redo browser

`/cb undogui` — chest GUI listing the player's undo history. Each slot = one action. Click to undo to that point.
`/cb redogui` — same for redo stack.

### 6. History log

`/cb history` — opens a chest GUI showing the server mutation log (who edited which block, when). Paginated. Filter by player (shift-click slot).

### 7. Magic items

`/cb magicitems` — chest GUI listing all magic items (special admin tools).
`/cb editmagicitems` — edit mode for magic item configuration.

### 8. Resource pack control

`/cb rp pause` — pauses automatic pack regeneration (for batch operations).
`/cb rp resume` — resumes pack regeneration and triggers one rebuild.
`/cb sync` — force-pushes the current pack to all connected clients immediately.
`/cb unsuppress` — re-enables pack notifications after a suppression.

### 9. Config path

All GUI-related server-side data persists to `config/customblocks/data/` (see All_Groups.md §Server Config Folder Structure).

---

## Setup

```
/cb create g02a ChestGUITest
/cb create g02b EditorTest
```

---

## Test G02.1 — Main menu opens as chest GUI

```
/cb
```

**Expected:** A chest GUI opens (not a screen/overlay). Title shows something like "CustomBlocks Dashboard". Stained glass decoration visible. Slots for: Block List, Categories, Templates, Macros, Arabic, Diagnostics, Config, Close.

**Pass:** Chest GUI opens with correct slots and decoration.
**Fail:** A screen-based UI opens instead of a chest GUI.

---

## Test G02.2 — Menu aliases all work

```
/cb menu
/cb dashboard
/cb gui
```

**Expected:** All three open the same chest GUI as `/cb`.

**Pass:** All aliases open identical chest GUI.
**Fail:** Any alias fails or opens a different screen.

---

## Test G02.3 — Block editor via `/cb editor`

```
/cb editor g02a
```

> **UPDATED 2026-06-20 (replace decision):** the editor is now screen-based — the Block Studio
> Edit tab (§G27.12). Expected/Pass/Fail flipped; the old chest editor is the FAIL now.

**Expected:** The **Block Studio** screen opens in **Edit** mode, pre-loaded with `g02a` — Identity / Texture / Shape / Attributes / Organize panels + 3D live preview, primary button `[Save Changes]`, block soft-locked while open.

**Pass:** Screen-based Block Studio opens in Edit mode for `g02a`.
**Fail:** Error, or the old chest editor opens.

---

## Test G02.4 — Block editor via `/cb gui block`

```
/cb gui block g02b
```

**Expected:** Same block editor chest GUI for `g02b`.

**Pass:** Opens correctly.
**Fail:** Command not found, or wrong block shown.

---

## Test G02.5 — Tab-complete for editor

Type `/cb editor ` (with trailing space) and press Tab.

**Expected:** Tab-complete offers `g02a`, `g02b` (and any other created blocks).

**Pass:** Tab-complete works.
**Fail:** No suggestions.

---

## Test G02.6 — Navigation back-stack

1. `/cb` → opens main menu
2. Click "Block List" slot → opens block list GUI
3. Click a block → opens block editor GUI
4. Click back slot

**Expected:** Returns to block list GUI. Click back again → returns to main menu.

**Pass:** Back-stack navigates correctly through all levels.
**Fail:** Back slot closes GUI entirely, or navigates to wrong screen.

---

## Test G02.7 — Undo browser

```
/cb setglow g02a 8
/cb setglow g02a 4
/cb undogui
```

**Expected:** Chest GUI opens showing at least 2 undo entries. Each slot shows the action (e.g., "setglow g02a 8 → 4"). Clicking a slot undoes to that point.

**Pass:** GUI opens, entries visible, click undoes correctly.
**Fail:** GUI opens empty, or undo from GUI doesn't work.

---

## Test G02.8 — History log

```
/cb history
```

**Expected:** Chest GUI showing mutation log entries. Each entry shows: timestamp, player name, action, block ID.

**Pass:** Log opens with at least the glow changes from G02.7.
**Fail:** Empty log, or command missing.

---

## Test G02.9 — RP pause and resume

```
/cb rp pause
/cb create g02c PauseTest
```

**Expected after pause:** `[CustomBlocks] Pack regeneration paused.` Block is created but no pack rebuild fires.

```
/cb rp resume
```

**Expected:** `[CustomBlocks] Pack regeneration resumed — rebuilding now.` Pack rebuilds once.

**Pass:** Pause/resume cycle works without double-rebuild.
**Fail:** Pack rebuilds during pause, or resume doesn't trigger rebuild.

---

## Test G02.10 — Sync forces pack push

```
/cb sync
```

**Expected:** `[CustomBlocks] Force-syncing resource pack to N client(s).` All connected clients re-download the pack.

**Pass:** Message shows correct client count. Pack re-applies on client.
**Fail:** Command missing, or no pack push observed.

---

## Test G02.11 — listgui

```
/cb listgui
```

**Expected:** Lists all available chest GUI commands with clickable `[open]` buttons.

**Pass:** List appears with correct GUI names and working buttons.
**Fail:** Command missing or empty list.

---

## Group 02 Verdict

| Test | Description | Result |
|---|---|---|
| G02.1 | Main menu opens as chest GUI | ⬜ |
| G02.2 | All menu aliases work | ⬜ |
| G02.3 | `/cb editor <id>` opens **Block Studio screen, Edit mode** (§G27.12) | ⬜ |
| G02.4 | `/cb gui block <id>` works | ⬜ |
| G02.5 | Tab-complete for editor | ⬜ |
| G02.6 | Back-stack navigates correctly | ⬜ |
| G02.7 | Undo browser GUI works | ⬜ |
| G02.8 | History log GUI opens | ⬜ |
| G02.9 | RP pause and resume | ⬜ |
| G02.10 | Sync force-pushes pack | ⬜ |
| G02.11 | listgui shows all GUIs | ⬜ |

**Group 02 passes only when the developer confirms all chest GUIs open, navigate, and function correctly in-game.**

If anything shows ❌ — paste:
1. The exact command typed
2. What opened (chest vs screen) and what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g02a
/cb delete g02b
/cb delete g02c
```
