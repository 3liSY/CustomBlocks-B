# Group 09 — Backup & Data Safety

**🎯 Active:** §A — A4 Backup Screen + naming display + panic/recover removal (built 2026-07-12, test-now)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | A1-3/A5, E, F, G passed — A4 rebuilt as the Backup Screen; naming display + panic/recover removal built, test-now |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅ · 14/15 passed · A4 Screen + naming + panic/recover built 2026-07-12 |
| **Last tested** | 2026-07-12                      |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> Backup + Trash GUIs are moving to Screens (Group 27 style). §C/§H moved there already.

## 🗺️ Sections

| § | What                                    | State |
| --- | ---------------------------------------- | ----- |
| A | Backup save · list · **Screen** (chat + Screen) | 🎯 A4 Screen + naming + panic/recover |
| E | Load · delete (chat)                     | ✅ 2026-07-12 |
| F | Auto-backup + prune (background)         | ✅ 2026-07-12 |
| G | Config gate + auto-backup status (chat)  | ✅ 2026-07-12 |

~~B~~ moved to Group 16. ~~C~~ moved to Group 27 (§G27.27). ~~D~~ struck, dead scope — no legacy `.dat` path
ever existed. ~~H~~ moved to Group 27 (§G27.28).

🔗 **Related Docs:** [GROUP_09_BACKUP_SAFETY.md](../groups/GROUP_09_BACKUP_SAFETY.md) ·
[GROUP_27_TESTING_GUIDE.md](GROUP_27_TESTING_GUIDE.md) (§C/§H) ·
[GROUP_16_TESTING_GUIDE.md](GROUP_16_TESTING_GUIDE.md) (§B)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g09a BackupTest1` (A, E)
* `/cb create g09b BackupTest2` (E, F)

## A · Backup — save · list · Screen · 🎯 A4 Screen + naming + panic/recover built; A1-3/A5 ✅

> 💡 `/cb backup` (or `/cb backup list`) now opens the red/black Backup Screen; console keeps the chat list.
> 🧰 `/cb create g09a BackupTest1` · a few `/cb backup save …` + one auto-backup for a full list

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A4 | `/cb backup` (or `/cb backup list`) as a player | the Backup Screen opens with left-rail Manual/Auto/All + block-count + size per row | 🎯 | 🎯 |
| A4a | type in search; click **Sort** | list narrows to the match; Sort cycles Newest → Oldest → Size | 🎯 | 🎯 |
| A4b | click **Manual** / **Auto** / **All** tabs | Manual hides `auto-…` backups; Auto shows only them | 🎯 | 🎯 |
| A4c | bottom name field → **+ Create** (blank = auto) | a new backup appears without leaving the screen | 🎯 | 🎯 |
| A4d | select a row → **Restore** → confirm | opaque modal fully hides the list; blocks restore; screen refreshes | 🎯 | 🎯 |
| A4e | select a row → **Delete** → confirm | opaque confirm; row gone after | 🎯 | 🎯 |
| A4f | select a row → **Rename** → type → Rename | row's label + restore-by id update | 🎯 | 🎯 |
| A4g | select an `auto-…` row → **Protect**, run auto-prune | pinned marker shows; NOT pruned past `autoBackupKeepCount` | 🎯 | 🎯 |
| A6 | make an auto-backup, `/cb backup list` in console | label reads `auto · Jul 12, 2:30 PM`; raw `auto-…` id shown dim | 🎯 | 🎯 |
| A7 | `/cb backup panic` | unknown command — nothing runs (subcommand deleted) | 🎯 | 🎯 |
| A8 | `/cb recover` | unknown command — nothing runs (deleted) | 🎯 | 🎯 |
| A1 | `/cb backup save pre-test` | saves named backup | ✅ | ✅ |
| A2 | `/cb backup save` | saves auto-named backup | ✅ | ✅ |
| A3 | `/cb backup save pre-test` again | refused "already exists" | ✅ | ✅ |
| A5 | `/cb backup save my/bad name` | refused invalid name | ✅ | ✅ |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

*(none — §D struck as dead scope 2026-07-12, see group doc §8)*

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

**§E · Load · delete** (Passed 2026-07-12). E1 restore-after-delete, E2 restore-survives-restart, E5
bad-name refusal — all confirmed. (`panic`/`recover` struck same day, see Bugs below.)

**§F · Auto-backup + prune** (Passed 2026-07-12). F1 auto-backup fires on interval no spam, F2 oldest
auto-backups pruned past `autoBackupKeepCount`, F3 manual saves never auto-pruned — all confirmed.

**§G · Config gate + status** (Passed 2026-07-12). G1 `autoBackupEnabled=false` stops firing, G2 list
reflects auto-backup-on status — both confirmed.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| G09-E3 | A7 | `/cb backup panic` — struck (redundant with restore); subcommand + Brigadier definition removed | 🟢 built 2026-07-12, test-now |
| G09-E4 | A8 | `/cb recover` — struck (never needed); top-level command + code removed | 🟢 built 2026-07-12, test-now |
| G09-A4 | A4 | `/cb backup list` rebuilt as the red/black Backup Screen (list · restore · delete · rename · protect · search · tabs · sort · create · opaque confirm) | 🟢 built 2026-07-12, test-now |

**G09-A4 Backup Screen — full spec (locked 2026-07-12):**
- Actions: list, restore, delete, rename a backup.
- Protect toggle per backup (survives auto-prune).
- Size / block-count shown per row.
- Search box + auto/manual tab split.
- In-screen "Create backup" button (manual save without leaving the screen).
- Sort: newest / oldest / size.
- Confirm modal (opaque, per G27 UI_SCREEN_RULES) before restore or delete.
- Style: Group 27 red/black/lime `CbScreenTemplate` standard, same as the already-migrated Backup Screen spec (§G27.27) — this replaces/extends that spec with the fuller action set above.

---

