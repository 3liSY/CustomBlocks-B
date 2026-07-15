# Group 20 — Cloud Vault & Discord

**🎯 Active:** I, J, A reverted (0/6) · K auto-update built, needs retest (0/6)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | 🎯 3 sections reverted + K auto-update built, need retesting |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/12) |
| **Last tested** | 2026-06-28                      |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ Screen content moved 2026-07-12: VaultConflictScreen spec → `GROUP_27_SCREENS.md` §G27.17.
> UpdateScreen spec (§CS3) → `GROUP_27_SCREENS.md` §G27.23. §K auto-update is actually built
> (`UpdateController.java`, `UpdateScreen.java`, `JarUpdater.java`) — never confirmed in-game, moved to
> Test Now below.

## 🗺️ Sections

| § | What                                                         | State     |
| --- | ------------------------------------------------------------ | --------- |
| I | Vault codes list — `/cb vault codes` (server-wide share log) | 🎯         |
| J | Category Share tile — edit-menu slot → `category share`      | 🎯         |
| A | Vault block share — upload/download/gates + master-switch    | 🎯         |
| C | Backup → cloud sync — `/cb backup save` pushes to R2         | 🧊 parked  |
| B | Conflict screen — clash → 4 resolutions                      | 🧊 parked  |
| G | Vault Hub GUI — `/cb vault` screen                           | 🧊 parked  |
| D | Request signing                                              | ⏳ planned |
| E | Discord events (rich embeds)                                 | ⏳ planned |
| F | Discord customization + `/cb discord`                        | ⏳ planned |
| H | Wave-3 — themes · pings · quiet hours                        | ⏳ planned |
| K | Auto-update — version sync + jar download (§CS3)             | 🎯 built, needs retest |

🔗 **Related Doc:** [GROUP_20_EXTERNAL_INTEGRATIONS.md](../groups/GROUP_20_EXTERNAL_INTEGRATIONS.md)

---

# 🎯 Test now

## K · Auto-update — version sync + jar download · 🎯 built, needs retest

> Built (`UpdateController.java`, `UpdateScreen.java`, `JarUpdater.java`) — never confirmed in-game. On server join, server tells client its mod version; if client is older → mod auto-downloads the new jar from the **server's own HTTP host** (no worker/R2), verifies SHA-256, swaps the old jar out, prompts "Restart to update." Full spec in GROUP_20 §CS3 (decisions AU1–AU10; AU7/AU8 dropped/parked).

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| K1 | Join server with **same version** | No update prompt, normal join. | ➖ | 🎯 |
| K2 | Join server with **newer version**, `autoUpdateEnabled=true` | Update screen appears, jar downloads from server, progress bar fills, "Restart" button shown. | ➖ | 🎯 |
| K3 | After K2: restart game, rejoin same server | Loads new version, no update prompt, clean join. | ➖ | 🎯 |
| K4 | Join server with **newer version**, `autoUpdateEnabled=false` | Warning toast only ("Server has vX, you have vY"), no download attempt. | ➖ | 🎯 |
| K5 | Join server with **older version** (client is newer) | Yellow warning toast only, no downgrade. | ➖ | 🎯 |
| K6 | Corrupt download (hash mismatch) | Abort, error message, old jar untouched. | ➖ | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

## C · Backup → cloud sync · 🧊 parked

> ⏸ **Parked 2026-06-28** — owner doesn't need cloud backups right now. Mod + worker built.
> To un-park: setup R2 (see `cloudflare/SETUP.md`), `/cb config` → **cloud sharing on** + **vault URL** set.

| # | Action                                                           | Expected Result                               | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Cloud sharing **off** → `/cb backup save t1`                     | Backup saves on PC. **No** cloud message.     | 🎯 | 🎯 |
| C2 | Cloud sharing **on**, vault URL **empty** → `/cb backup save t2` | Saves on PC. Doesn't try the cloud.           | 🎯 | 🎯 |
| C3 | Cloud sharing **on**, vault URL set → `/cb backup save t3`       | Saves, "synced to cloud" line with **code**.  | 🎯 | 🎯 |
| C4 | Let an **automatic** backup run with cloud on                    | Only local backup happens.                    | 🎯 | 🎯 |
| C5 | Cloud sharing **on**, vault URL unreachable                      | Saves on PC + gentle "couldn't sync" message. | 🎯 | 🎯 |

## B · Conflict screen · 🧊 parked

> Built, 🎯 not tested — owner wants a visual redesign before in-game testing. Clash → 4 resolutions (Override / Keep Both / Rename Mine / Cancel). Spec `GROUP_27_SCREENS.md §G27.17`.

| § | What it'll do                                                                                   | Spec                           |
| --- | ----------------------------------------------------------------------------------------------- | ------------------------------ |
| D | Request signing — every vault call signed + carries who/where · 🧊 deferred → web dashboard      | GROUP_20                       |
| E | Discord events — create/delete/edit/bulk/backup/vault/error/startup                             | GROUP_20                       |
| F | Discord customization — per-event toggles, templates, colours, identity + `/cb discord` helpers | GROUP_20                       |
| G | Vault Hub GUI — `/cb vault` **client screen** (not chest): Upload · Download · My Codes         | GROUP_20 / GROUP_02 / GROUP_27 |
| H | Wave-3 — Discord theme presets · milestone pings · quiet hours                                  | GROUP_20                       |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **I · Vault codes list** (Passed 2026-06-28). Test history moved to [GROUP_20_CLOUD.md](GROUP_20_CLOUD.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **J · Category Share tile** (Passed 2026-06-28). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **A · Vault block share** (Passed 2026-06-28). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

