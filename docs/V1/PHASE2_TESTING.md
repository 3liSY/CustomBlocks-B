# Phase 2 Testing Tutorial — Persistence & Safety

> **Prerequisite:** Phase 1 ✅ confirmed. You have Minecraft installed with the mod.
>
> **Goal:** Verify that block data saves to disk correctly, survives a full restart,
> and that the mod handles missing or corrupted save files without crashing.
>
> **Rules:** Work through each test in order. Stop and report any failure before continuing.

---

## What Phase 2 covers (and what's not built yet)

| Feature | Status |
|---|---|
| Block data saves to disk on every change | ✅ Built — `SlotDataStore.java` |
| All blocks reload on restart | ✅ Built — `SlotManager.loadAll()` |
| Atomic writes (no corruption mid-save) | ✅ Built — temp file + rename |
| Graceful handling of corrupt/missing save file | ✅ Built — logs error, starts empty |
| BackupManager (automatic backups) | ❌ Not yet built |
| SnapshotManager (named full-state snapshots) | ❌ Not yet built |
| TrashManager (soft-delete recycle bin) | ❌ Not yet built |

Tests below only cover the 4 built items.

---

## Setup

1. Launch Minecraft with the mod loaded.
2. Join a **Singleplayer Creative world with cheats ON**.
3. Confirm the log line `[CustomBlocks] Loaded 34 saved custom block(s).` appeared at startup.

---

## Test 2.1 — Block saves to disk immediately after creation

**Commands:**
```
/cb create p2test PhaseTwo
```

**Immediately** (without restarting) open File Explorer and navigate to:
```
C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\
```

Open `slots.json`. Find the entry for `p2test`. It should look like:
```json
{
  "index": <some number>,
  "customId": "p2test",
  "displayName": "PhaseTwo",
  "glow": 0,
  "hardness": 1.5,
  "sound": "stone"
}
```

**Pass:** Entry exists in `slots.json` right after the `/cb create` command, no restart needed.  
**Fail:** Entry missing, or file doesn't exist, or file looks corrupted.

---

## Test 2.2 — Block survives a full restart ⭐ Most important

1. With `p2test` created (from Test 2.1), **fully close Minecraft** — quit to desktop.
2. Relaunch Minecraft and re-enter the same world.
3. Check the startup log line:
   ```
   [CustomBlocks] Loaded 35 saved custom block(s).
   ```
   (Should be 35 now — your 34 old blocks plus `p2test`.)
4. Run `/cb list` → find `p2test` in the list with display name **PhaseTwo**.
5. Run `/cb give p2test` → item appears in hand with correct name on tooltip.

**Pass:** Block present after restart, count incremented, all data intact.  
**Fail:** Block missing, count wrong, or data changed.

---

## Test 2.3 — All fields persist (not just name)

1. Set some attributes on `p2test`:
   ```
   /cb setglow p2test 10
   /cb sethardness p2test 5
   ```
2. Open `slots.json` again. The `p2test` entry should show:
   ```json
   "glow": 10,
   "hardness": 5.0
   ```
3. **Fully close Minecraft**, relaunch, re-enter the world.
4. Run `/cb list` — hover over `p2test` or run `/cb give p2test` and check the block.
5. Confirm glow and hardness values are still 10 and 5 after restart.

**Pass:** Both values survived restart.  
**Fail:** Values reset to defaults (0 glow, 1.5 hardness).

> **Setup:** `/cb setglow` and `/cb sethardness` are Batch 4 commands. If they give an error
> here, skip this test — those commands aren't being tested until later.
> Test 2.2 alone proves persistence. Mark this ⏳ and return after Batch 4.

---

## Test 2.4 — Atomic write (no `.tmp` garbage file left behind)

After any save operation (creating/renaming a block), check the config folder:
```
C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\
```

**Expected:** Only `slots.json` — no `slots.json.tmp` file sitting there.

The `.tmp` file is created during the write and immediately renamed to `slots.json`.
If the write completes normally, no `.tmp` remains.

**Pass:** No `.tmp` file visible after the save.  
**Fail:** A `slots.json.tmp` file is stuck there (would indicate a crashed mid-write).

---

## Test 2.5 — Missing save file handled gracefully

**This tests what happens if `slots.json` is deleted before the mod loads.**

> ⚠️ This test deletes your save file temporarily. Your real blocks will be gone for
> one launch. They come back when you restore the file in step 4. Follow the steps carefully.

1. Close Minecraft completely.
2. In File Explorer, **rename** (don't delete) `slots.json` to `slots.json.bak`:
   ```
   C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\slots.json
   → rename to: slots.json.bak
   ```
3. Launch Minecraft and enter a world.
4. Check the log — you should see:
   ```
   [CustomBlocks] Loaded 0 saved custom block(s).
   ```
   and **no crash**. The mod starts clean with zero blocks.
5. Close Minecraft again.
6. **Rename `slots.json.bak` back to `slots.json`** to restore your blocks.
7. Relaunch — should load your 35 blocks again normally.

**Pass:** Mod starts clean with 0 blocks, no crash. Blocks restore when file is put back.  
**Fail:** Crash on startup when file is missing.

---

## Test 2.6 — Corrupt save file handled gracefully

**This tests what happens if `slots.json` gets corrupted (invalid JSON).**

1. Close Minecraft completely.
2. Open `slots.json` in Notepad. Delete the first `{` character and save.
   The file is now invalid JSON.
3. Launch Minecraft and enter a world.
4. Check the log — you should see an error like:
   ```
   [CustomBlocks] Failed to load slot data (starting empty)
   ```
   followed by `[CustomBlocks] Loaded 0 saved custom block(s).` — and **no crash**.
5. Close Minecraft.
6. In Notepad, put the `{` back at the start of `slots.json` and save it.
7. Relaunch — should load your blocks normally again.

**Pass:** Corrupt file → error log + clean start + no crash. File fixed → normal load.  
**Fail:** Crash on startup with corrupt file.

---

## Phase 2 Verdict

| Test | Description | Result |
|---|---|---|
| 2.1 | Block saves to `slots.json` immediately | ✅ 2026-06-07 |
| 2.2 | Block survives full restart | ✅ 2026-06-07 |
| 2.3 | All fields (glow, hardness) persist | ✅ 2026-06-07 |
| 2.4 | No `.tmp` file left after save | ✅ 2026-06-07 |
| 2.5 | Missing file → clean start, no crash | ✅ 2026-06-07 |
| 2.6 | Corrupt file → error log, no crash | ✅ 2026-06-07 |

**Phase 2 confirmed ✅ 2026-06-07 — proceed to `docs/PHASE3_TESTING.md`.**

If anything shows ❌ — paste:
1. The exact steps you followed
2. What you expected vs what happened
3. The relevant lines from `latest.log`

---

## What's not built yet (Phase 2 remainder)

These 3 features are in the Engineering Bible Phase 2 scope but were not implemented:

| Feature | What it does |
|---|---|
| `BackupManager` | Auto-saves a full backup of `slots.json` on a timer (e.g. every 30 min) |
| `SnapshotManager` | Named full-state snapshots you can restore with `/cb snapshot restore <name>` |
| `TrashManager` | Deleted blocks go to a recycle bin, restorable with `/cb trash restore <id>` |

These are not blockers for Phase 3. Tell me if you want them built before continuing,
or we can proceed and add them later.
