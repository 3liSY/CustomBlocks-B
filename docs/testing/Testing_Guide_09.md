# Group 09 - Backup, Data Safety & Trash

## Status

| | |
| --- | --- |
| **Verdict** | Backup commands are built, but dated confirmation and the final trash/backup Screen pass still need clean verification. |
| **Progress** | 🟩🟩🟥🟥🟥🟥🟥🟥🟥🟥 20% |
| **Last tested** | 2026-07-17 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Manual backup save and list | Built 🎯 | - |
| B | Backup load and delete | Built 🎯 | - |
| C | Auto-backup, prune, and config gate | Built 🎯 | - |
| F | Cloud backup handoff to Vault | Planned 📜 | Blocked ‼️ |
| D | Backup Screen polish pass | Built 🎯 | Polish 🎨 |
| E | Trash browser Screen and restore flow | Designed ⏳ | - |
| G | Backup data paths under `config/customblocks/data/` | Planned 📜 | - |

**Original Group:** [GROUP_09_BACKUP_SAFETY.md](../groups/GROUP_09_BACKUP_SAFETY.md)

---

# Active Tests

## 💡 Setup

- Create `g09a` and `g09b`, set at least one attribute, and place both.
- Use a safe test world or temporary server before running restore, panic, trash, or delete rows.
- Keep a file explorer open to `config/customblocks/backups/`.

## A - Manual backup save and list - Built 🎯

| | |
| --- | --- |
| **Check** | Manual saves create safe, named backups and list them with useful metadata. |
| **Pass rule** | Named save, auto-name, duplicate refusal, invalid-name refusal, and list order pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb backup save pre-test`. | Backup is saved with slot data, textures/sources, config snapshot, manifest, and timestamp. | 🎯 | 🎯 |
| A2 | Run `/cb backup save` with no name. | Auto-named backup is created with a timestamp-style name. | 🎯 | 🎯 |
| A3 | Save `pre-test` again. | Existing backup is not overwritten and the command explains the duplicate. | 🎯 | 🎯 |
| A4 | Run `/cb backup save my/bad name`. | Name is refused and no backup folder/file is created. | 🎯 | 🎯 |
| A5 | Run `/cb backup list`. | Backups show newest first with timestamp, block count, and action affordances. | 🎯 | 🎯 |

## B - Backup load and delete - Built 🎯

| | |
| --- | --- |
| **Check** | Loading a backup protects current data first, restores reliably, and never overwrites live data mid-write. |
| **Pass rule** | Load confirm, cancel, safety copy, delete, and restart restore pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Delete `g09a`, then run `/cb backup load pre-test`. | Command warns about replacing live data and waits for confirmation. | 🎯 | 🎯 |
| B2 | Confirm the load. | `g09a` returns with its saved attributes and a pre-restore safety backup is created. | 🎯 | 🎯 |
| B3 | Start another load and cancel. | No live data changes. | 🎯 | 🎯 |
| B4 | Delete a backup with `/cb backup delete <name>`. | The backup is removed and live blocks are untouched. | 🎯 | 🎯 |
| B5 | Save, restart, delete a test block, then load the saved backup. | Backup survives restart and restores correctly. | 🎯 | 🎯 |

## C - Auto-backup, prune, and config gate - Built 🎯

| | |
| --- | --- |
| **Check** | Auto-backups run on schedule, prune only old auto entries, and expose safe config controls. |
| **Pass rule** | Interval, keep count, disable, config gate, and config tile rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Set auto-backup interval to a short value and restart. | An `auto-YYYYMMDD-HHMMSS` backup appears without manual command. | 🎯 | 🎯 |
| C2 | Set keep count low and wait for several auto-backups. | Only the newest auto-backups are pruned; manual saves remain. | 🎯 | 🎯 |
| C3 | Set interval to `0` and wait. | No new auto-backups are created. | 🎯 | 🎯 |
| C4 | Open server config from `/cb config`. | A confirmation gate appears before the config screen. | 🎯 | 🎯 |
| C5 | Use the Auto-Backup config tile. | Left-click changes interval, right-click changes keep count, and the value persists. | 🎯 | 🎯 |

## D - Backup Screen polish pass - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb backup` with no args opens a clean Screen for create, load, select, and delete-selected. |
| **Pass rule** | Open, create, right-click load, multi-select delete, refresh, and console fallback pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb backup` as a player. | Backup Screen opens with newest backups first. | 🎯 | 🎯 |
| D2 | Create a backup from the Screen. | Suggested name can be accepted/edited, backup saves, and list refreshes. | 🎯 | 🎯 |
| D3 | Load a backup from the Screen. | Confirm screen appears; Yes loads safely and No changes nothing. | 🎯 | 🎯 |
| D4 | Select multiple backups and delete selected. | Only selected backups are deleted; live data is untouched. | 🎯 | 🎯 |
| D5 | Run `/cb backup` from console. | Console receives text output instead of trying to open a Screen. | ➖ | 🎯 |

## E - Trash browser Screen and restore flow - Designed ⏳

| | |
| --- | --- |
| **Check** | Trash is recoverable, visible through a Screen, and aligned with the G06 recycle-bin marker system. |
| **Pass rule** | Delete capture, restore, id collision, pin, delete forever, marker heal, and retention rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Final trash Screen is not built; base trash behavior overlaps the G06 recycle-bin system. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Delete `g09t` and open `/cb trash`. | Trash Screen lists the deleted block with saved data and deletion time. | 🎯 | 🎯 |
| E2 | Restore `g09t`. | Block returns with saved texture/attributes and markers heal where applicable. | 🎯 | 🎯 |
| E3 | Try to restore when the same id already exists. | Restore is refused cleanly and does not overwrite the live block. | 🎯 | 🎯 |
| E4 | Pin a trash entry. | Entry is marked as protected from retention pruning. | 🎯 | 🎯 |
| E5 | Delete forever. | Entry is permanently removed and live blocks are untouched. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

- §F Cloud backup handoff to Vault — 📜 `2026-07-12`: Depends on Group 20.
- §G Backup data paths under `config/customblocks/data/` — 📜 `2026-07-12`: Needs verification across all storage classes.

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Broken Blocks Report in TG9 — 👎 `2026-07-12`: Moved to Group 16.
- First-boot migration rows — 👎 `2026-07-12`: Dead scope until a real legacy-data case appears.
- Chest-menu backup/trash target — 👎 `2026-07-12`: Replaced by Screen target.
- `/cb recover` and `/cb backup panic` — 👎 `2026-07-19`: Redundant with `/cb backup load <newest>` through the same confirm-gated safe-restore rail; never implemented, doc previously claimed them Built by mistake.
- `/cb backup restore` alias — 👎 `2026-07-19`: Duplicate literal for `/cb backup load`; removed from `BackupCommands`.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g09a`, `g09b`, `g09t`, and temporary backups after testing.
- [ ] Reset auto-backup interval and keep count to owner defaults after testing.
- [ ] Move any broken-block repair finding to G16.
- [ ] Do not re-add first-boot migration unless a real legacy-data case is provided.

</details>
