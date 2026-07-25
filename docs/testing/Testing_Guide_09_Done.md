# Group 09 - Backup, Data Safety & Trash

## Status

| | |
| --- | --- |
| **Verdict** | All shipped sections (A, B, C, G, H, I) confirmed in-game. Cloud backup (old §F) dropped — backups are local-only now. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-23 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Manual backup save and list | Done ✅ | - |
| B | Backup load and delete | Done ✅ | - |
| C | Auto-backup interval, keep count, and prune | Done ✅ | - |
| G | Full-tree deduped snapshot and data paths | Done ✅ | - |
| H | Naming, kind, reason, and retention | Done ✅ | - |
| I | Integrity: verify and boot guard | Done ✅ | - |

**Original Group:** [GROUP_09_BACKUP_SAFETY.md](../groups/GROUP_09_BACKUP_SAFETY.md)

---

# Active Tests

*(none — every shipped section is confirmed and archived below. Cloud backup was cut, not tested.)*

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Manual backup save and list — ✅ `2026-07-23`
- §B Backup load and delete — ✅ `2026-07-23`
- §C Auto-backup interval, keep count, and prune — ✅ `2026-07-23`
- §G Full-tree deduped snapshot and data paths — ✅ `2026-07-23`
- §H Naming, kind, reason, and retention — ✅ `2026-07-23`
- §I Integrity: verify and boot guard — ✅ `2026-07-23`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

- Backup + Trash **Screens**, full scope — 📜 `2026-07-23`: **all** backup subcommands (save, list, load, contents/browse, delete) move to one Screen GUI at **G27 / TG27**; console commands become thin triggers into it. Discuss layout with owner before building.
- Auto-backup config gate (old §C4-C6: `autoBackupTime`, `autoBackupBudgetMB`, `/cb config` gate) — 📜 `2026-07-23`: moving to **G27 / TG27** as config-screen controls; engine itself already confirmed (§C above).
- Verify fix-guidance text (§I) — 📜 `2026-07-23`: `/cb backup verify` output (I2/I3) needs to append what to actually do about each problem it finds (e.g. missing pool file → "restore from an earlier backup" or "delete and re-save"), not just name it. Code TODO.

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Cloud backup upload + pull (old §F) — 👎 `2026-07-23`: needed Cloudflare R2, which requires a billing card the owner declined. Backup upload, `/cb backup pull`, and the ZIP export/import that fed them were removed from the mod; backups are local-only. Category/note sharing (KV, no card) is unaffected and still works. If cloud backup is ever wanted, restore from git and stand up R2 per `cloudflare/SETUP.md`.
- Broken Blocks Report in TG9 — 👎 `2026-07-12`: Moved to Group 16.
- First-boot migration rows — 👎 `2026-07-12`: Dead scope until a real legacy-data case appears.
- Chest-menu backup/trash target — 👎 `2026-07-12`: Replaced by Screen target.
- `/cb recover` and `/cb backup panic` — 👎 `2026-07-19`: Redundant with `/cb backup load <newest>` through the same confirm-gated safe-restore rail.
- `/cb backup restore` alias — 👎 `2026-07-19`: Duplicate literal for `/cb backup load`.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g09a`, `g09b`, `g09t`, and temporary backups after testing.
- [ ] Reset `autoBackupInterval`, `autoBackupKeepCount`, `autoBackupTime`, `autoBackupBudgetMB`, and `safetyKeepCount` to owner defaults after testing.
- [ ] Move any broken-block repair finding to G16.

</details>
