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

## UI medium audit (2026-07-09) — locked 2026-07-12

Whole group → **Screens**: BackupMenu (+ BackupConfirmMenu folded in), TrashMenu (+ TrashEntryMenu folded
in, real deleted-block texture preview via `SlotItemRenderer` instead of the missing-texture placeholder
icon), and SafetyMenu as the hub Screen (tiles: Backup / Trash / Broken-Blocks — the last tile links out
to G16's diagnostics screen, code no longer lives here).

Built to the **Group 27 `CbScreenTemplate` standard** (`GROUP_27_SCREENS.md`): locked red `#FF0000` /
black `#000000` / lime `#40FF00` palette (red = selected/active, lime = success only, never mixed), dark
title-bar strip + gold-slot-now-red 1px border, dual hint lines (context + universal shortcuts), `[?]`
help button, bottom action bar, opaque confirm modals (no bleed-through), save flashes lime + CB toast
(no chat). This **overrides** G27's older 2026-07-04 "leave backup/trash as chest-menus" note — the
2026-07-09 TG9 audit is the newer decision.

Mockup approved 2026-07-12 (owner review, overlap bug fixed — each panel now `display:none` unless
active, bounded-height scroll container).

**BrokenBlocksMenu / BrokenConfirmMenu / BrokenBlockScanner move fully to Group 16** as part of this
rebuild — see §7 below and `GROUP_16_DIAGNOSTICS.md` (already documents G16 ownership; the physical
package move + command repoint happens in this build).

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Backup system | Unreliable snapshots with random restore failures | `BackupManager` stub | Reliable ground-up rebuild with named saves |
| `/cb backup` commands | `backup create/list/restore/delete/expiry` | Not functional | Fully restored |
| Panic mode | `/cb panic` — emergency rollback | Missing | Restored |
| Recover | `/cb recover` — restore from latest backup | Missing | Restored |
| Trash browser | `/cb deletedblocks` — browse recently deleted blocks | Not accessible | Restored as Screen (TrashMenu) |
| ~~Broken blocks report~~ | `/cb showbrokenblocks` | **MOVED → G16** (diagnostics, decision B 2026-06-21) | — |
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
| ~~Broken blocks~~ | **MOVED → G16** (`/cb showbrokenblocks` is diagnostics) |
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

Opens `TrashMenu` (Screen, not built — see audit note above) listing recently deleted blocks (sorted newest first):
- Rows show: block ID, display name, deleted timestamp, texture preview.
- Click a row → sub-menu: "Restore", "Pin", "Delete permanently".
- Pinned items are never auto-pruned.
- Auto-delete timer: configurable `trashRetentionDays` (default 30).

> 🔁 **2026-06-27 — Trash is now the recovery half of the unified Recycle-Bin deletion system.**
> Behaviour is owned by **[G06-14](GROUP_06_TOOLS.md#g06-14--unified-recycle-bin-deletion-system-replaces-the-removed-system)**
> (spec there). Key changes vs the old trash:
> - **Delete** moves the block here AND turns its placed copies into `Deleted: <name>` markers. The
>   block's slot number is **reserved** (kept out of reuse) while it sits in Trash.
> - **Restore** re-creates the block on **any free slot number** (not necessarily the old one) and the
>   `Deleted: <name>` markers in the world resolve **by name** back into it — even after a restart.
> - **Delete permanently (Empty)** frees the reserved slot number for reuse and tombstones that block's
>   leftover markers (a generic `(Deleted)` marker that won't revive on a later same-name create).
>
> This replaces the old behaviour where Restore made a brand-new slot and left the placed copies grey
> and orphaned. The old `(Removed)` block / `DeletedSlots` no-reuse / chunk-scanner are removed.

### 7. ~~Broken Blocks Report~~ — MOVED → G16 (physical move locked 2026-07-12)

`/cb showbrokenblocks` is owned by **G16** (diagnostics) as of decision B (2026-06-21) — it scans for
missing textures / broken registrations and feeds the IT Chest auto-fix flow. Spec + test live in
`GROUP_16_DIAGNOSTICS.md` / `GROUP_16_TESTING_GUIDE.md` §E.

Ownership moved 2026-06-21 but the code (`core/BrokenBlockScanner`, `gui/chest/BrokenBlocksMenu`,
`gui/chest/BrokenConfirmMenu`, related command handlers) never physically relocated out of G09's
package. This Screen rebuild is doing that move now: files → a diagnostics package, `/cb
showbrokenblocks` command registration repointed, TG9's old §B rows struck (no rows kept here — see
`GROUP_09_TESTING_GUIDE.md`). SafetyMenu's Broken-Blocks tile keeps a link/tile that opens G16's screen;
it no longer owns any of the underlying code.

### 8. ~~First-Boot Migration (MigrationManager)~~ — STRUCK, dead scope (2026-07-12)

Verified against source: `core/TextureStore.java` has only ever read/written `slot_N.png` — no `.dat`
handling, no gzip branch, no migration code path anywhere in the tree. It was never `.dat`; there is no
legacy `slots.json.gz` / `categories.json` format expected in this codebase. No `MigrationManager` class
exists, and none is being built. Removed from TG9's Planned/Parked list. If a real legacy-data scenario
ever surfaces, it gets a fresh spec + session — not this one.

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

## Test G09.6 — Trash browser ⏳ (blocked — Screen not built)

```
/cb delete g09b
/cb deletedblocks
```

**Expected:** `TrashMenu` Screen opens. `g09b` appears as a deleted block row with: ID, name, deletion timestamp. "Restore", "Pin", "Delete permanently" buttons on click.

**Pass:** Screen opens, g09b visible, all action buttons present.
**Fail:** Empty, g09b not listed, or old chest GUI opens instead.

---

## Test G09.7 — Restore from trash

In `/cb deletedblocks`, click `g09b` → click "Restore".

**Expected:** `g09b` is restored to the block registry. GUI closes or refreshes. `/cb list` shows g09b.

**Pass:** Block restored from trash.
**Fail:** Restore button doesn't work.

---

## Test G09.8 — ⛔ MOVED → G16

The broken-blocks report test (`/cb showbrokenblocks`) now lives in `GROUP_16_DIAGNOSTICS.md` (decision B).
Skip here.

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
| ~~G09.8~~ | ~~Broken blocks detected and reported~~ | ⛔ MOVED → G16 (decision B) |
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
