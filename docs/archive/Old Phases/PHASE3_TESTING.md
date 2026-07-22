# Phase 3 Testing Tutorial — Core Commands

> **Prerequisite:** Phase 2 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify every `/cb` command works correctly end-to-end — create, delete,
> rename, dupe, give, list, reload, undo, redo, setglow, sethardness, setsound,
> setcollision, search, and tab-completion.
>
> **Rules:** Work through each test in order. Stop and report any failure before continuing.

---

## What Phase 3 covers

| Command | Handler file |
|---|---|
| `/cb create`, `delete`, `rename`, `dupe`, `retexture` | `CreationCommands.java` |
| `/cb setglow`, `sethardness`, `setsound`, `setcollision` | `AttributeCommands.java` |
| `/cb undo`, `redo` | `HistoryCommands.java` |
| `/cb give`, `list`, `reload`, `search`, `categories` | `UtilityCommands.java` |
| Tab-completion for block IDs | `BlockSuggestions.java` |

**Not built (Bible Phase 3 scope, skipped):** `DidYouMean.java`, `HelpRegistry.java`, `/cb help`.  
Tests below cover only what exists.

---

## Setup

1. Launch Minecraft with the mod, enter Creative world with cheats ON.
2. You have existing blocks — that is fine. Tests use fresh IDs that won't conflict.

---

## Test 3.1 — `/cb create`

```
/cb create p3test PhaseThree
```

**Expected green message:**
```
Created p3test (slot X)
```

Also test multi-word name with quotes:
```
/cb create p3test2 "Phase Three Block"
```
**Expected:** `Created p3test2 (slot Y)`

**Pass:** Both created, display names correct.  
**Fail:** Red error, or name missing quotes and gets truncated.

---

## Test 3.2 — `/cb list`

```
/cb list
```

**Expected:** Chat shows all your blocks. Both `p3test` and `p3test2` appear with their
display names and slot numbers.

**Pass:** Both new blocks visible in list.  
**Fail:** Command errors or blocks not listed.

---

## Test 3.3 — `/cb give`

```
/cb give p3test
```

**Expected:** A `PhaseThree` item appears in your hotbar. Hover it — tooltip says **PhaseThree**.

**Pass:** Item in hand, correct name on tooltip.  
**Fail:** Error, wrong name, or no item.

---

## Test 3.4 — `/cb rename`

```
/cb rename p3test PhaseThreeRenamed
```

**Expected:**
```
Renamed p3test → "PhaseThreeRenamed"
```

Hover the item in your inventory — tooltip updates to **PhaseThreeRenamed**.

**Pass:** Name updated on item tooltip.  
**Fail:** Error or name unchanged.

> Rename it back: `/cb rename p3test PhaseThree`

---

## Test 3.5 — `/cb dupe`

```
/cb dupe p3test p3testcopy
```

**Expected:**
```
Duplicated p3test → p3testcopy (slot Z)
```

`/cb list` shows both `p3test` and `p3testcopy`.

**Pass:** Copy created with new ID, both appear in list.  
**Fail:** Error or only one block in list.

---

## Test 3.6 — `/cb delete`

```
/cb delete p3testcopy
```

**Expected:**
```
Deleted p3testcopy
```

`/cb list` — `p3testcopy` gone, `p3test` still there.

**Pass:** Correct block removed, other untouched.  
**Fail:** Wrong block deleted, error, or both gone.

---

## Test 3.7 — `/cb undo` and `/cb redo`

Immediately after Test 3.6:

```
/cb undo
```
**Expected:** `p3testcopy` comes back. `/cb list` shows it again.

```
/cb redo
```
**Expected:** `p3testcopy` deleted again. `/cb list` — gone.

**Pass:** Undo restored, redo re-deleted.  
**Fail:** Undo/redo had no effect, or wrong block affected.

---

## Test 3.8 — `/cb setglow`

Place `p3test` in a dark area first (`/time set night` helps).

```
/cb setglow p3test 15
```
**Expected:** Success message. The placed block glows like a torch (light level 15).

```
/cb setglow p3test 0
```
**Expected:** Block stops glowing.

```
/cb setglow p3test 50
```
**Expected:** Capped — message says something like `Set glow of p3test to 15. (15 is Minecraft's brightest — capped)`. **Not an error.**

**Pass:** Glow changes visibly, cap handled cleanly.  
**Fail:** No visible glow change, crash, or unhelpful error on value 50.

---

## Test 3.9 — `/cb sethardness`

Switch to **Survival** for this test (`/gamemode survival`).

```
/cb sethardness p3test unbreakable
```
Try to mine the placed block — **cannot mine it**.

```
/cb sethardness p3test instant
```
Mine the block — **instant break**.

```
/cb sethardness p3test stone
```
Mine it — normal stone speed.

Switch back to Creative: `/gamemode creative`

**Pass:** Hardness changes mining behavior as expected.  
**Fail:** Hardness has no effect, error message, or crash.

---

## Test 3.10 — `/cb setsound`

```
/cb setsound p3test metal
```

Place and break `p3test` — it should **sound like metal**, not stone.
Walk on it — metal footstep sound.

Try an invalid sound:
```
/cb setsound p3test banana
```
**Expected:** Clean error listing valid options. **Not a crash.**

**Pass:** Sound changed to metal, invalid name gives helpful error.  
**Fail:** Sound unchanged, crash, or cryptic error.

---

## Test 3.11 — `/cb setcollision`

Place `p3test` at head-height in a doorway.

```
/cb setcollision p3test passable
```
Walk into it — you pass **through** it. You can still right-click/target it.

```
/cb setcollision p3test solid
```
Walk into it — it blocks you again.

**Pass:** Passable = walk-through, solid = blocks movement.  
**Fail:** Collision unchanged, error, or crash.

---

## Test 3.12 — `/cb reload`

```
/cb reload
```
**Expected:** `Reloaded X block(s) from disk.` — no crash.

**Pass:** Command runs, count matches your block total.  
**Fail:** Error or crash.

---

## Test 3.13 — `/cb search`

```
/cb search p3
```
**Expected:** Finds `p3test` and `p3test2` (both match the partial ID `p3`).

```
/cb search PhaseThree
```
**Expected:** Finds `p3test` by display name.

Each result should have a clickable **[give]** button — click one → item appears in hand.

**Pass:** Partial ID and name search both find correct blocks, [give] button works.  
**Fail:** No results, wrong results, or [give] button crashes.

---

## Test 3.14 — Tab-completion

In chat, type `/cb rename ` (space after rename) and press **Tab**.

**Expected:** A dropdown of your block IDs appears.

Type `/cb setglow ` and press **Tab** — same dropdown.

**Pass:** Tab shows block ID suggestions for commands that take an ID.  
**Fail:** No suggestions appear.

---

## Test 3.15 — Bad ID gives clean error

```
/cb give doesnotexist
```
**Expected:** A red error message like `No block 'doesnotexist'`. **Not a crash.**

```
/cb create p3test PhaseThree
```
(ID already exists)  
**Expected:** `Can't create 'p3test' — id exists or no free slots`. **Not a crash.**

**Pass:** Both give readable errors, no crash.  
**Fail:** Crash or cryptic error text.

---

## Phase 3 Verdict

| Test | Description | Result |
|---|---|---|
| 3.1 | `/cb create` single + multi-word name | ✅ 2026-06-07 |
| 3.2 | `/cb list` shows all blocks | ✅ 2026-06-07 |
| 3.3 | `/cb give` puts item in hand, correct tooltip | ✅ 2026-06-07 |
| 3.4 | `/cb rename` updates item name | ✅ 2026-06-07 |
| 3.5 | `/cb dupe` creates copy | ✅ 2026-06-07 |
| 3.6 | `/cb delete` removes correct block | ✅ 2026-06-07 |
| 3.7 | `/cb undo` + `/cb redo` work | ✅ 2026-06-07 |
| 3.8 | `/cb setglow` changes light, caps at 15 | ✅ 2026-06-07 |
| 3.9 | `/cb sethardness` changes mining speed | ✅ 2026-06-07 |
| 3.10 | `/cb setsound` changes block sounds | ✅ 2026-06-07 |
| 3.11 | `/cb setcollision` passable/solid works | ✅ 2026-06-07 |
| 3.12 | `/cb reload` runs clean | ✅ 2026-06-07 |
| 3.13 | `/cb search` finds by ID + name | ✅ 2026-06-07 |
| 3.14 | Tab-completion suggests block IDs | ✅ 2026-06-07 |
| 3.15 | Unknown ID gives clean error | ✅ 2026-06-07 |

**Phase 3 confirmed ✅ 2026-06-07 — proceed to `docs/PHASE4_TESTING.md`.**

If anything shows ❌ — paste:
1. The exact command you typed
2. What you expected vs what happened
3. Relevant lines from `latest.log`

---

## Cleanup after testing

```
/cb delete p3test
/cb delete p3test2
```
