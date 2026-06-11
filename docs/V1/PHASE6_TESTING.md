# Phase 6 Testing Tutorial — Undo/Redo + Block Attributes

> **Prerequisite:** Phase 5 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify undo/redo work per-player, and that all block attribute commands
> (setglow, sethardness, setsound, setcollision, setcategory) work correctly.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| UndoManager size | ~1,170 lines — disk-based delta snapshots | ~137 lines — in-memory immutable snapshots only |
| Undo scope | Unknown / likely global | **Per-player by default.** Config: `undoMode = per_player` or `global` |
| Delete undo | May have lost texture on undo | Texture bytes saved with DELETE op — full restore including image |
| `setglow` range | Brigadier rejected values outside 0–15 | Accepts any int, clamps quietly with "capped at 15" note |
| `sethardness` input | Numbers only | **Keywords supported:** `unbreakable`, `instant`, `stone`, plus any float |
| `setcollision` input | Likely `solid`/`passable` only | Accepts many synonyms: `through`, `ghost`, `decor`, `walkthrough`, etc. |
| `setcategory` | Not present in original | **New.** Assign blocks to categories for `/cb search` and GUI filtering |

---

## What Phase 6 covers

| Feature | File |
|---|---|
| Per-player undo/redo stacks | `UndoManager.java` |
| `/cb undo`, `/cb redo` | `HistoryCommands.java` |
| `/cb setglow`, `sethardness`, `setsound`, `setcollision`, `setcategory` | `AttributeCommands.java` |

---

## Setup

Create a test block to use throughout:
```
/cb create p6test GlowTest
/cb give p6test
```
Place it in the world.

---

## Test 6.1 — `/cb setglow`

```
/cb setglow p6test 10
```
**Expected:** `Glow p6test → 10`  
The placed block now emits light. Walk away and look — it should glow in the dark.

Test clamping:
```
/cb setglow p6test 99
```
**Expected:** `Glow p6test → 15 (max 15)` — clamped, no red error.

Test zero:
```
/cb setglow p6test 0
```
**Expected:** `Glow p6test → 0` — block no longer glows.

**Pass:** Glow visually changes on placed block, clamping works.
**Fail:** Block doesn't glow, error thrown for 99, or crash.

---

## Test 6.2 — `/cb sethardness`

```
/cb sethardness p6test unbreakable
```
**Expected:** `p6test → unbreakable`  
Try to break the block in survival mode — it should not break.

```
/cb sethardness p6test instant
```
**Expected:** `p6test → instant break`  
Block should break instantly even without the right tool.

```
/cb sethardness p6test stone
```
**Expected:** `Hardness p6test → 1.5`

```
/cb sethardness p6test 50
```
**Expected:** `Hardness p6test → 50`

**Pass:** All keyword forms work, hardness change is reflected in-game.
**Fail:** Keywords rejected, break behavior doesn't change, or crash.

---

## Test 6.3 — `/cb setsound`

```
/cb setsound p6test wood
```
**Expected:** `Sound p6test → wood`  
Place and break the block — should make wood sounds.

```
/cb setsound p6test metal
```
**Expected:** `Sound p6test → metal`

Test tab-completion: type `/cb setsound p6test ` and press Tab — should suggest the available sound types.

**Pass:** Sound changes on the placed block, tab-completion shows options.
**Fail:** Sound doesn't change, bad-sound error message wrong, or crash.

---

## Test 6.4 — `/cb setcollision`

```
/cb setcollision p6test passable
```
**Expected:** `p6test → passable (walk-through)`  
Walk into the placed block — you should pass through it.

```
/cb setcollision p6test solid
```
**Expected:** `p6test → solid`  
Block is now solid again.

Test synonym:
```
/cb setcollision p6test ghost
```
**Expected:** `p6test → passable (walk-through)` — synonym accepted.

**Pass:** Collision changes in-game, synonyms work.
**Fail:** Block stays solid/passable, synonym rejected, or crash.

---

## Test 6.5 — `/cb setcategory` (new — not in old CustomBlocks)

```
/cb setcategory p6test decorations
```
**Expected:** `Category p6test → decorations`

```
/cb setcategory p6test none
```
**Expected:** `p6test → uncategorized`

**Pass:** Category set and cleared without error.
**Fail:** Command not found, or error on valid input.

---

## Test 6.6 — `/cb undo` (basic)

```
/cb setglow p6test 15
```
Then immediately:
```
/cb undo
```
**Expected:** `Undid glow p6test (X left)` — glow reverts to previous value.

**Pass:** Undo reversed the glow change.
**Fail:** "Nothing to undo", wrong block affected, or crash.

---

## Test 6.7 — `/cb redo`

After test 6.6 (undo just ran):
```
/cb redo
```
**Expected:** `Redid glow p6test (X left)` — glow goes back to 15.

**Pass:** Redo re-applied the change.
**Fail:** "Nothing to redo" immediately after undo, or wrong state.

---

## Test 6.8 — Undo create + delete

```
/cb create p6undo UndoTest
```
Then:
```
/cb undo
```
**Expected:** `Undid create p6undo` — block removed from `/cb list`.

```
/cb redo
```
**Expected:** `Redid create p6undo` — block back in `/cb list`.

**Pass:** Create/delete round-trips cleanly through undo/redo.
**Fail:** Block not removed/restored, list wrong, or crash.

---

## Test 6.9 — Undo delete (texture preserved)

1. Create and texture a block:
   ```
   /cb create p6del DelTest https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png
   ```
   Accept pack prompt. Place the block.

2. Delete it:
   ```
   /cb delete p6del
   ```

3. Undo the delete:
   ```
   /cb undo
   ```
   **Expected:** `Undid delete p6del` — block restored. Give it and place it — texture should still be there.

**Pass:** Deleted textured block fully restored including image.
**Fail:** Block restored but texture gone (purple), or undo fails.

---

## Phase 6 Verdict

| Test | Description | Result |
|---|---|---|
| 6.1 | `setglow` — visual change + clamping | ⬜ |
| 6.2 | `sethardness` — keywords + number, break behavior | ⬜ |
| 6.3 | `setsound` — sound changes, tab-complete works | ⬜ |
| 6.4 | `setcollision` — passable/solid + synonyms | ⬜ |
| 6.5 | `setcategory` — set + clear | ⬜ |
| 6.6 | `undo` — reverses last change | ⬜ |
| 6.7 | `redo` — re-applies undone change | ⬜ |
| 6.8 | Undo create → redo → block back | ⬜ |
| 6.9 | Undo delete → texture preserved | ⬜ |

**When all 9 show ✅ — say "Phase 6 passes" and we move to Phase 7.**

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p6test
/cb delete p6undo
/cb delete p6del
```
