# Group 09 — Backup, Data Safety & First-Boot Migration

> **Prerequisite:** Group 01 (Legacy Audit) signed off. Phase 2 (Persistence) build-verified.
>
> **Objective:** Build the unified `/cb backup` system (replacing the old unreliable snapshot system). Restore the trash browser and broken-blocks report. Execute first-boot data migration from old CustomBlocks format. All data paths updated to `config/customblocks/data/`.
>
> **Source issues:** Group C (backup system), Group D (trash & recovery), first-boot MigrationManager (§Server Config Folder Structure in All_Groups.md)
>
> **CRITICAL:** The old snapshot system was completely unreliable (reverted to broken states on restart). This is a **ground-up rebuild** — do NOT patch old code. Design for reliability first.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Backup system | Unreliable snapshots with random restore failures | `BackupManager` stub | Reliable ground-up rebuild with named saves |
| `/cb backup` commands | `backup create/list/restore/delete/expiry` | Not functional | Fully restored |
| Panic mode | `/cb panic` — emergency rollback | Missing | Restored |
| Recover | `/cb recover` — restore from latest backup | Missing | Restored |
| Trash browser | `/cb deletedblocks` — browse recently deleted blocks | Not accessible | Restored as chest GUI |
| Broken blocks report | `/cb showbrokenblocks` — blocks with placement issues or missing textures | Missing | Restored |
| Data paths | `config/customblocks/*.json` (root) | Mixed paths | All normalized to `config/customblocks/data/` |
| Auto-backup | Timed automatic backup every 30 min | Not running | Restored, interval configurable |
| Cloud backup | Sync to CustomBlocks Vault | Missing | Restored (depends on Group 20) |
| First-boot migration | N/A | Old `.gz` files present | MigrationManager converts old format on first boot |

---

## What this group covers

| Feature | Commands |
|---|---|
| Manual backup | `/cb backup save [name]` |
| Backup list | `/cb backup list` |
| Restore backup | `/cb backup restore <name>` |
| Delete backup | `/cb backup delete <name>` |
| Emergency rollback | `/cb backup panic` |
| Auto-backup config | `autoBackupInterval` (default 30 min) |
| Trash browser | `/cb deletedblocks` |
| Trash pin | Pin items to prevent auto-delete |
| Broken blocks | `/cb showbrokenblocks` |
| Recover | `/cb recover` |
| Safety check | `/cb safety` |
| Data migration | Automatic on first boot |
| Storage location | `config/customblocks/backups/` |

---

## Implementation Requirements

### 1. Backup System — Reliability Rules

- Every backup is written atomically: write to a temp file, then rename (same as SlotDataStore).
- Every backup includes: all `SlotData`, all textures (or texture checksums + originals), config snapshot, and a timestamp.
- Backup names: auto-generated as `auto-YYYYMMDD-HHMMSS` for timed backups, or developer-named for manual saves.
- Backups stored in `config/customblocks/backups/`.
- Restoring a backup: write to temp location, verify integrity, then swap — never overwrite live data mid-write.

### 2. `/cb backup save [name]`

Creates a named point-in-time backup. If no name given, auto-generates one.

### 3. `/cb backup restore <name>`

Restores the server to the state captured in that backup.
- Requires `/cb confirm` (always, regardless of `bulkConfirmThreshold`).
- Triggers full pack rebuild after restore.
- Server pauses block modifications during restore.

### 4. `/cb backup panic`

Emergency rollback: immediately restores the most recent backup without a confirmation prompt. Use when something has gone catastrophically wrong.

### 5. Auto-Backup

Config field: `autoBackupInterval` (default 30 min). Timer fires on a daemon thread. Runs silently — no chat message unless configured. Old auto-backups beyond `autoBackupKeepCount` (default 10) are pruned automatically.

### 6. Trash Browser — `/cb deletedblocks`

Opens a chest GUI listing recently deleted blocks (sorted newest first):
- Slots show: block ID, display name, deleted timestamp, texture preview.
- Click a slot → sub-menu: "Restore", "Pin", "Delete permanently".
- Pinned items are never auto-pruned.
- Auto-delete timer: configurable `trashRetentionDays` (default 30).

### 7. Broken Blocks Report — `/cb showbrokenblocks`

Scans all registered blocks and reports:
- Blocks with missing texture files.
- Blocks registered in `SlotData` but with no physical file on disk.
- Placed instances of missing blocks in the world.

Output: chest GUI with Red Wool slots for each broken block. Click → auto-fix options.

### 8. First-Boot Migration (MigrationManager)

Runs once on first boot with the new JAR. Checks for old-format data and converts:

| Migration step | Source | Destination |
|---|---|---|
| Slot data | `data/slots.json.gz` (gzip, `"slots"` key, `"lightLevel"` field) | `data/slots.json` (`"blocks"` key, `"glow"` field) |
| Config | `data/config.json` (root path) | Read from `data/config.json` (path updated in code) |
| Textures | `textures/slot_N.dat` | `textures/slot_N.png` (PNG bytes, just wrong extension) |
| Categories | `data/categories.json` (rich objects + assignment map) | Category string merged into each block's `SlotData` |

After successful migration: old files are deleted or renamed `.migrated`. Migration is idempotent — running again is safe.

### 9. Data Paths — All Classes Must Use

All data files now live in `config/customblocks/data/`. See All_Groups.md §Server Config Folder Structure for the full path constants table. Violating classes must be updated.

---

## Setup

Create some test blocks before testing backup features:
```
/cb create g09a BackupTest1
/cb create g09b BackupTest2
/cb setglow g09a 8
```

---

## Test G09.1 — Manual backup save

```
/cb backup save pre-test
```

**Expected:** `Backup "pre-test" saved. (2 block(s), config, textures)`

Check `config/customblocks/backups/` — a folder or file named `pre-test` exists.

**Pass:** Backup created with confirmation message.
**Fail:** Error, or no backup file created.

---

## Test G09.2 — Backup list

```
/cb backup list
```

**Expected:** List shows at least `pre-test` with creation timestamp, block count, and a `[restore]` button.

**Pass:** Backup appears in list with correct metadata.
**Fail:** List empty, or backup missing.

---

## Test G09.3 — Backup restore

1. Delete a block:
   ```
   /cb delete g09a
   ```
2. Restore:
   ```
   /cb backup restore pre-test
   ```
3. Confirm:
   ```
   /cb confirm
   ```

**Expected:** `Restored from backup "pre-test". 2 block(s) restored.` — `g09a` is back with glow 8.

**Pass:** Block restored correctly with correct attributes.
**Fail:** Block not restored, wrong attributes, or restore crashes.

---

## Test G09.4 — Restore is reliable across restart

1. `/cb backup save restart-test`
2. `/stop` (restart server)
3. `/cb delete g09b`
4. `/cb backup restore restart-test` → `/cb confirm`

**Expected:** `g09b` restored after restart.

**Pass:** Backup survived restart and restores correctly.
**Fail:** Backup missing after restart, or restore fails.

---

## Test G09.5 — Auto-backup fires

Set `autoBackupInterval = 2` (minutes) in config for testing. Wait 2 minutes.

```
/cb backup list
```

**Expected:** An auto-generated backup (named `auto-YYYYMMDD-HHMMSS`) appears in the list.

Restore `autoBackupInterval` to 30 after this test.

**Pass:** Auto-backup created without any manual command.
**Fail:** No auto-backup appears.

---

## Test G09.6 — Trash browser

```
/cb delete g09b
/cb deletedblocks
```

**Expected:** Chest GUI opens. `g09b` appears as a deleted block slot with: ID, name, deletion timestamp. "Restore", "Pin", "Delete permanently" buttons on click.

**Pass:** GUI opens, g09b visible, all action buttons present.
**Fail:** GUI empty, or g09b not listed.

---

## Test G09.7 — Restore from trash

In `/cb deletedblocks`, click `g09b` → click "Restore".

**Expected:** `g09b` is restored to the block registry. GUI closes or refreshes. `/cb list` shows g09b.

**Pass:** Block restored from trash.
**Fail:** Restore button doesn't work.

---

## Test G09.8 — Broken blocks report

Manually delete a texture file: remove `config/customblocks/textures/slot_X.png` for one of the test blocks. Then:

```
/cb showbrokenblocks
```

**Expected:** Chest GUI opens with the affected block shown as a Red Wool slot. Click → "Re-download texture" option available.

**Pass:** Broken block detected and listed.
**Fail:** Broken block not detected, or GUI empty.

---

## Test G09.9 — First-boot migration (if old data present)

*(Skip if no old-format data exists in the server folder.)*

Check: does `config/customblocks/data/slots.json.gz` exist? If yes:

1. Install the new JAR for the first time.
2. Start the server.

**Expected:** Server log shows: `[CustomBlocks] Running first-boot migration…` followed by conversion steps. After startup, `slots.json.gz` is gone, `slots.json` exists with blocks in new format (`"blocks"` key, `"glow"` field).

**Pass:** Migration runs without errors. All old blocks accessible in new format.
**Fail:** Migration errors, or blocks missing after migration.

---

## Group 09 Verdict

| Test | Description | Result |
|---|---|---|
| G09.1 | Manual backup created | ⬜ |
| G09.2 | Backup list shows correct metadata | ⬜ |
| G09.3 | Backup restore works correctly | ⬜ |
| G09.4 | Backup survives server restart | ⬜ |
| G09.5 | Auto-backup fires on schedule | ⬜ |
| G09.6 | Trash browser shows deleted blocks | ⬜ |
| G09.7 | Restore from trash works | ⬜ |
| G09.8 | Broken blocks detected and reported | ⬜ |
| G09.9 | First-boot migration converts old data | ⬜ |

**Group 09 passes when backups are reliable, trash is browsable, broken blocks are detectable, and migration runs cleanly.**

If anything shows ❌ — paste:
1. The exact command or action
2. What happened vs what was expected
3. Full `latest.log` from the affected session start

---

## Cleanup

```
/cb delete g09a
/cb delete g09b
/cb backup delete pre-test
/cb backup delete restart-test
```
