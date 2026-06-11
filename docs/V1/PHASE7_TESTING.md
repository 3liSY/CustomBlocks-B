# Phase 7 Testing Tutorial — Tools / Items

> **Prerequisite:** Phase 6 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify the three hand tools work correctly — Lumina Brush (glow cycling),
> Chisel (hardness cycling), and Deleter (definition wipe + undo).
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Number of tools built | 7 tools planned | **3 built this phase** — Lumina Brush, Chisel, Deleter. Color tools (ColorSquare, ColorTriangle, GoldenHexagon, RectangleTool) come in a later phase. |
| Client-side skip delay | Tool use had a noticeable delay before acting | **Fixed.** Client returns SUCCESS immediately (arm swings), server does the work. Zero perceived delay. |
| Chisel role | Cycled glow AND handled shape editing | Shape editing removed (shape system not built yet). Chisel cycles hardness only. |
| Deleter behavior | Physical block was broken | **Physical block stays.** Only the slot definition + texture is wiped. Placed copy reverts to unassigned appearance. Fully undoable. |
| Undo support for tool use | Unknown | All 3 tools push to UndoManager — every tool action is `/cb undo`-able. |
| Deleter + locked blocks | Unknown | Deleter checks `LockManager` — locked blocks cannot be deleted by the tool. |
| Creative tab | One tab | **Two tabs:** "CustomBlocks" (created blocks) + "CustomBlocks Tools" (hand tools). |

---

## What Phase 7 covers

| Tool | Item ID | Action |
|---|---|---|
| Lumina Brush | `customblocks:lumina_brush` | Right-click block → cycle glow up (0→4→8→12→15→0). Shift+right-click → cycle down. |
| Chisel | `customblocks:chisel` | Right-click block → cycle hardness (instant→soft→stone→hard→tough→unbreakable→…). Shift = backward. |
| Deleter | `customblocks:deleter` | Right-click block → wipe slot definition + texture. Physical block stays, reverts to unassigned. Undoable. |

---

## Setup

Get all three tools from the **CustomBlocks Tools** creative tab (search for it in the creative inventory), or:
```
/give @s customblocks:lumina_brush
/give @s customblocks:chisel
/give @s customblocks:deleter
```

Create a test block:
```
/cb create p7test ToolTest
/cb give p7test
```
Place it in the world.

---

## Test 7.1 — Creative tab shows tools

Open the creative inventory. Find the **CustomBlocks Tools** tab.

**Expected:** Tab exists and contains Lumina Brush, Chisel, and Deleter items.

**Pass:** All 3 tools visible in the tab.
**Fail:** Tab missing, or items not in it.

---

## Test 7.2 — Lumina Brush: glow cycling forward

Hold the **Lumina Brush** and right-click the placed `p7test` block.

**Expected each click:** Glow steps through `0 → 4 → 8 → 12 → 15 → 0 → …`  
Chat shows: `p7test glow → 4` (then 8, 12, 15, 0 on each click).  
At glow > 0 the placed block visually emits light.

**Pass:** Glow cycles forward, light emitted in-game, chat message correct.
**Fail:** No chat message, glow doesn't change, or block doesn't light up.

---

## Test 7.3 — Lumina Brush: glow cycling backward (sneak)

Hold the brush, **sneak + right-click** the block.

**Expected:** Glow cycles in reverse: `0 → 15 → 12 → 8 → 4 → 0 → …`

**Pass:** Backward cycle works.
**Fail:** Sneaking has no effect.

---

## Test 7.4 — Lumina Brush: change is undoable

Set glow to 8 with the brush, then:
```
/cb undo
```
**Expected:** `Undid glow p7test` — glow reverts.

**Pass:** Undo works after brush use.
**Fail:** Nothing to undo, or wrong block affected.

---

## Test 7.5 — Chisel: hardness cycling forward

Hold the **Chisel** and right-click `p7test`.

**Expected each click:** Cycles through `instant → soft → stone → hard → tough → unbreakable → instant → …`  
Chat shows: `p7test hardness → instant` (then soft, stone, etc.)

Switch to survival mode to verify: at `instant` the block breaks immediately; at `unbreakable` it won't break at all.

**Pass:** Hardness changes, labels correct, in-game break behavior matches.
**Fail:** No cycle, wrong labels, or break behavior unchanged.

---

## Test 7.6 — Chisel: hardness cycling backward (sneak)

Sneak + right-click with the Chisel.

**Expected:** Cycles backward through the presets.

**Pass:** Backward cycle works.
**Fail:** Sneaking has no effect.

---

## Test 7.7 — Deleter: wipes definition, block stays physical

Hold the **Deleter** and right-click `p7test`.

**Expected:**
- Chat: `Deleted p7test`
- `/cb list` — `p7test` is gone
- The physical block in the world is **still there** but now shows the unassigned/purple appearance
- The slot is freed for reuse

**Pass:** Definition wiped, physical block stays, list updated.
**Fail:** Physical block also disappears, or definition not removed.

---

## Test 7.8 — Deleter: undo restores full definition

Immediately after 7.7:
```
/cb undo
```
**Expected:** `Undid delete p7test` — block reappears in `/cb list` with its name restored.

If `p7test` had a texture, it should return too.

**Pass:** Full definition restored via undo.
**Fail:** Nothing to undo, or block restored without name/texture.

---

## Test 7.9 — Deleter: locked block cannot be deleted

Lock the block first:
```
/cb create p7lock LockTest
/cb give p7lock
```
Place it, then lock it:
```
/cb lock p7lock
```
Now right-click it with the Deleter.

**Expected:** Chat error: `'p7lock' is locked — /cb unlock p7lock first`  
Block definition is NOT wiped.

Cleanup:
```
/cb unlock p7lock
/cb delete p7lock
```

**Pass:** Locked block protected from Deleter.
**Fail:** Deleter wipes a locked block.

---

## Test 7.10 — No tool delay (instant arm swing)

Right-click any custom block with any of the 3 tools rapidly (several clicks).

**Expected:** Arm swings instantly with no stutter or delay between clicks. Effect applied immediately.

**Pass:** Zero perceived delay.
**Fail:** Noticeable lag between swing and effect.

---

## Phase 7 Verdict

| Test | Description | Result |
|---|---|---|
| 7.1 | Tools in CustomBlocks Tools creative tab | ⬜ |
| 7.2 | Lumina Brush cycles glow forward | ⬜ |
| 7.3 | Lumina Brush cycles backward with sneak | ⬜ |
| 7.4 | Brush change is undoable | ⬜ |
| 7.5 | Chisel cycles hardness forward + in-game effect | ⬜ |
| 7.6 | Chisel cycles backward with sneak | ⬜ |
| 7.7 | Deleter wipes definition, physical block stays | ⬜ |
| 7.8 | Deleter undo restores full definition | ⬜ |
| 7.9 | Deleter blocked on locked blocks | ⬜ |
| 7.10 | No delay on any tool use | ⬜ |

**When all 10 show ✅ — say "Phase 7 passes" and we move to Phase 8.**

If anything shows ❌ — paste:
1. The exact action you took
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p7test
```
(p7lock cleaned up in test 7.9 already)
