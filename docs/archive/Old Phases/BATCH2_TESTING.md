# Batch 2 Testing Tutorial — Create, Manage, Persist Blocks

> **Prerequisite:** Phase 1 ✅ confirmed. You have a Creative world open with cheats ON.
>
> **Goal:** Create blocks with `/cb`, name them, rename them, duplicate them, delete them,
> and confirm they survive a full Minecraft restart.
>
> **Rules:** Work through each test in order. If one fails, stop and report it.
> Do not mark a test ✅ until you personally saw it work.

---

## Setup

1. Open Minecraft, join (or create) a **Singleplayer Creative world with cheats ON**.
2. Open chat with **T** — all commands start with `/cb`.
3. You already have 34 saved blocks from previous sessions. That is fine — these
   tests create new ones with fresh names that won't conflict.

---

## Test 2.1 — Create a block

**Command:**
```
/cb create test1 TestBlock
```

**What each part means:**
- `test1` = the internal ID you use in other commands (one word, no spaces)
- `TestBlock` = the display name shown in-game (multi-word names need quotes: `"My Block"`)

**Expected chat message:**
```
Created test1 (slot X)
```
where X is some slot number (doesn't matter which one).

**Also check:**
- Press **E** → open Creative inventory → find the **CustomBlocks** tab.
- `TestBlock` should now appear in it as a purple/black cube item.

**Pass:** Message appeared, item in tab.  
**Fail:** Red error message, or item not in tab.

---

## Test 2.2 — List your blocks

**Command:**
```
/cb list
```

**Expected:** Chat shows all your created blocks. You'll see your existing 34 blocks
plus the new `test1`. Each line shows the ID, display name, and slot number.

**Pass:** `test1` appears in the list.  
**Fail:** Command errors, or `test1` is missing.

---

## Test 2.3 — Give yourself a block item

**Command:**
```
/cb give test1
```

**Expected:** A `TestBlock` item appears in your hand / hotbar.

**Also check:** Hover over it — the tooltip should say **TestBlock** (the display name),
not `slot_X` or something generic.

**Pass:** Item in hand, tooltip shows correct name.  
**Fail:** Error, or wrong name in tooltip.

---

## Test 2.4 — Place and interact with the block

1. With the `TestBlock` item in hand, **place it** in the world.
   - It will be a purple/black checkerboard cube — no texture yet, that is correct.
2. Look directly at it — the block's name should appear on screen (HUD overlay).
3. **Don't break it yet** — you'll need it for the next test.

**Pass:** Block placed, name visible on HUD.  
**Fail:** Crash, or no HUD overlay.

---

## Test 2.5 — Rename a block

**Command:**
```
/cb rename test1 NewName
```

**Expected:**
```
Renamed test1 → "NewName"
```

**Check:** Hover over the block item in your inventory — the tooltip should now say
**NewName**. The placed block in the world should also update its HUD text.

**Pass:** Name changed on item tooltip and HUD.  
**Fail:** Error, or name did not update.

> Rename it back when done so later tests are consistent:
> ```
> /cb rename test1 TestBlock
> ```

---

## Test 2.6 — Duplicate a block

**Command:**
```
/cb dupe test1 test1copy
```

**Expected:**
```
Duplicated test1 → test1copy (slot Y)
```

**Check:** `/cb list` → both `test1` and `test1copy` appear.

**Pass:** Both blocks in the list.  
**Fail:** Error, or only one block in list.

---

## Test 2.7 — Delete a block

**Command:**
```
/cb delete test1copy
```

**Expected:**
```
Deleted test1copy
```

**Check:** `/cb list` → `test1copy` is gone. `test1` still there.

**Pass:** `test1copy` removed, `test1` untouched.  
**Fail:** Error, or wrong block deleted, or both gone.

---

## Test 2.8 — Undo a delete ⭐

**Immediately after Test 2.7:**
```
/cb undo
```

**Expected:**
```
[CB] Undid delete test1copy
```

**Check:** `/cb list` → `test1copy` is back.

**Pass:** Block restored by undo.  
**Fail:** Error, or block not restored.

> After confirming, delete `test1copy` again to clean up:
> ```
> /cb delete test1copy
> ```

---

## Test 2.9 — Persistence across restart ⭐ Most important

**This proves your blocks survive a server/game restart.**

1. Confirm `test1` exists: `/cb list` shows it.
2. Check the file exists:
   ```
   C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\slots.json
   ```
   Open it — find an entry for `test1` with its display name and slot number.
3. **Fully close Minecraft** (quit to desktop, not just leave the world).
4. Relaunch Minecraft and re-enter the same world.
5. Run `/cb list` → `test1` is still there with the correct name.

**Pass:** `test1` survived the restart with its name intact.  
**Fail:** Block missing after restart, or name changed.

---

## Test 2.10 — Reload command

**Command:**
```
/cb reload
```

**Expected:**
```
Reloaded X block(s) from disk.
```
where X matches your block count.

**Pass:** Command ran, no crash, number looks correct.  
**Fail:** Error or crash.

---

## Test 2.11 — Bad ID is handled cleanly

**Try creating a block with an ID that already exists:**
```
/cb create test1 Duplicate
```

**Expected:** A red error message like:
```
Can't create 'test1' — id exists or no free slots
```
No duplicate block created. `/cb list` still shows only one `test1`.

**Pass:** Clean error, no duplicate.  
**Fail:** Crash, or two blocks with the same ID appear.

---

## Test 2.12 — Tab-completion works

In chat, type `/cb rename ` (with a space after rename) and press **Tab**.

**Expected:** A dropdown list of your block IDs appears as suggestions.

**Pass:** Suggestions appear.  
**Fail:** No suggestions, or game crashes.

---

## Batch 2 Verdict

Mark each result:

| Test | Description | Result |
|---|---|---|
| 2.1 | `/cb create` makes a block | ⬜ |
| 2.2 | `/cb list` shows all blocks | ⬜ |
| 2.3 | `/cb give` puts item in hand with correct name | ⬜ |
| 2.4 | Block places in world, HUD shows name | ⬜ |
| 2.5 | `/cb rename` updates tooltip + HUD | ⬜ |
| 2.6 | `/cb dupe` creates a copy | ⬜ |
| 2.7 | `/cb delete` removes block | ⬜ |
| 2.8 | `/cb undo` restores deleted block | ⬜ |
| 2.9 | Block survives full restart | ⬜ |
| 2.10 | `/cb reload` runs clean | ⬜ |
| 2.11 | Duplicate ID gives clean error | ⬜ |
| 2.12 | Tab-completion lists block IDs | ⬜ |

**When all 12 show ✅ — tell me "Batch 2 passes" and we move to Batch 3 (textures).**

If anything shows ❌ — paste:
1. The exact command you typed
2. What you expected vs what happened
3. The chat message or last 20 lines of `latest.log`

---

## Quick Cleanup After Testing

When done, clean up the test blocks so they don't clutter your real block list:
```
/cb delete test1
```
(You may already have deleted `test1copy` in Test 2.7.)
