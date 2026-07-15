# Phase 10 Testing Tutorial — GUI System

> **Prerequisite:** Phase 9 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify that all built GUI screens open, render, and function correctly.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.
>
> **Phase 17 note:** The developer has confirmed this entire screen-based GUI system will
> be replaced with a chest-based GUI in Phase 17. These tests verify the current system
> works — not that it's the final design.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| GUI engine | `GuiManager.java` (~9,400 lines) — chest inventory-based GUIs. `CbScreenHandler.java` handled chest slots. | **Minecraft Screen classes.** `GuiEngine.java` is a small drawing utility (~50 lines). No chest GUIs. |
| Block editor | `/cb editor <id>` — opened a chest GUI editor | `/cb gui block <id>` — opens `BlockEditorScreen`. `/cb editor` shortcut missing (Phase 17 17.5). |
| HUD toggle | `/cb edithud` — opened a drag-to-reposition HUD layout editor screen | `/cb edithud` — toggles HUD on/off only. No layout editor (Phase 17 17.4). |
| Config GUI | Full settings GUI with all config options editable | `ConfigScreen` — limited, text-only display |
| Main menu | `GuiManager.openWelcomeGui()` — chest-based navigation | `MainMenuScreen` — button list. Runs commands when clicked. |
| Missing screens | — | `TexturePickerScreen`, `ShapeEditorScreen`, `ColorsHubScreen`, `CategoryBrowserScreen`, `SnapshotManagerScreen`, `UndoPickerScreen`, `ImportConflictScreen`, `AdminScreen` — all unbuilt |
| `AnvilPromptManager` | Text input via anvil rename flow | Not built |
| `ColorLibrary` / `ColorPickerHelper` | Color picker widgets | Not built |

**Correct command names (important):**

| What you want | Correct command | Wrong command (will fail) |
|---|---|---|
| Open main menu | `/cb gui` | — |
| Open block editor | `/cb gui block <id>` | `/cb gui editor <id>` ❌ |
| Open config screen | `/cb gui config` | — |
| Open macro list | `/cb gui macros` | — |
| Open Arabic browser | `/cb gui arabic` | — |
| Toggle HUD | `/cb edithud` | `/cb config hud` ❌ |

---

## What Phase 10 covers (built)

| Screen | Command | File |
|---|---|---|
| Main Menu | `/cb gui` | `MainMenuScreen.java` |
| Block Editor | `/cb gui block <id>` | `BlockEditorScreen.java` |
| Config Screen | `/cb gui config` | `ConfigScreen.java` |
| Macro List | `/cb gui macros` | `MacroListScreen.java` |
| Arabic Browser | `/cb gui arabic` | `ArabicBrowserScreen.java` |
| HUD toggle | `/cb edithud` | `GuiCommands.java` |

---

## Setup

```
/cb create p10test GuiTest
```

---

## Test 10.1 — Main menu opens

```
/cb gui
```

**Expected:** Full-screen dark overlay with gold "CustomBlocks" title and buttons:
Block List, Categories, Templates, Macros, Arabic Letters, Diagnostics, Close [Esc].

Press Esc — screen closes.

**Pass:** Screen opens, all buttons visible, Esc closes it.
**Fail:** Nothing opens, crash, or screen stays open after Esc.

---

## Test 10.2 — Main menu buttons run commands

Click **Block List** in the main menu.

**Expected:** Screen closes and `/cb list` output appears in chat.

Click **Categories**.

**Expected:** `/cb categories` output in chat.

**Pass:** Buttons execute commands and close the screen.
**Fail:** Buttons do nothing, or screen stays open.

---

## Test 10.3 — Block editor opens via `/cb gui block`

```
/cb gui block p10test
```

**Expected:** Block editor screen opens — title "Block Editor — p10test". Shows buttons: Retexture, Set Glow, Set Hardness, Set Sound, Set Collision, Set Category, Add Note, Give to Me, Rename, Delete Block, Close.

Press Esc — screen closes.

**Pass:** Editor opens with p10test loaded, all buttons visible.
**Fail:** Nothing opens, wrong block loaded, or crash.

---

## Test 10.4 — Block editor: pre-fill command buttons

In the block editor for p10test, click **Set Glow...**.

**Expected:** Screen closes, chat box opens pre-filled with `/cb setglow p10test `.

Press Esc without typing — nothing happens.

**Pass:** Chat pre-fills with the correct command stub.
**Fail:** Command runs immediately (not pre-fill), or wrong command.

---

## Test 10.5 — Block editor: run command buttons

In `/cb gui block p10test`, click **Give to Me**.

**Expected:** Screen closes, `/cb give p10test` runs, block added to inventory.

**Pass:** Give executes directly.
**Fail:** Nothing happens, or wrong command runs.

---

## Test 10.6 — Block editor: invalid ID rejected

```
/cb gui block doesnotexist
```

**Expected:** Red error in chat: `No block 'doesnotexist'`. No screen opens.

**Pass:** Server validates ID before sending payload to client.
**Fail:** Screen opens with blank/broken state.

---

## Test 10.7 — Config screen opens

```
/cb gui config
```

**Expected:** Config screen opens.

Press Esc — closes.

**Pass:** Screen opens and closes cleanly.
**Fail:** Crash or nothing.

---

## Test 10.8 — Macro list screen

```
/cb gui macros
```

**Expected:** Macro list screen opens.

Press Esc — closes.

**Pass:** Opens and closes.
**Fail:** Crash or nothing.

---

## Test 10.9 — Arabic browser screen

```
/cb gui arabic
```

**Expected:** Arabic browser screen opens.

Press Esc — closes.

**Pass:** Opens and closes.
**Fail:** Crash or nothing.

---

## Test 10.10 — HUD toggle via `/cb edithud`

```
/cb edithud
```

**Expected:** `HUD overlay disabled.` (or enabled, depending on current state). Chat confirms the toggle.

```
/cb edithud
```

**Expected:** State reverses.

**Pass:** Toggle works both directions, confirmed in chat.
**Fail:** No message, or state doesn't change.

---

## Phase 10 Verdict

| Test | Description | Result |
|---|---|---|
| 10.1 | Main menu opens + Esc closes | ✅ |
| 10.2 | Main menu buttons execute commands | ✅ |
| 10.3 | Block editor opens via `/cb gui block <id>` | ✅ |
| 10.4 | Pre-fill buttons open chat with command stub | ✅ |
| 10.5 | Run buttons execute directly | ✅ |
| 10.6 | Invalid ID rejected before screen opens | ✅ |
| 10.7 | Config screen opens | ✅ |
| 10.8 | Macro list screen opens | ✅ |
| 10.9 | Arabic browser screen opens | ✅ |
| 10.10 | `/cb edithud` toggles HUD | ✅ |

**Phase 10 ✅ — confirmed in-game. Missing screens deferred to Phase 17.**

> **Reminder:** This GUI system is fully replaced in Phase 17 (Issue 17.3).
> Missing screens (TexturePicker, ShapeEditor, ColorsHub, etc.) are Phase 17 scope.

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p10test
```
