# Group 18 — Lore (Block Notes Revamp)

**🎯 Active:** B, C, D, E, F, G (0/12)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                   |
| --------------- | --------------------------------- |
| **Verdict**     | 🎯 6 sections need retesting       |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/12) |
| **Last tested** | 2026-06-21                        |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): NotesMenu (Lore GUI) → Screen, not built. See `GROUP_18_NOTES_STAGING.md`.
> Screen spec moved 2026-07-12 → `GROUP_27_SCREENS.md` §G27.33.

## 🗺️ Sections

| § | What                                              | State    |
| --- | ------------------------------------------------- | -------- |
| B | Add lines, see them in the screen                 | 🎯        |
| C | Edit + delete a line                              | 🎯        |
| D | On/Off toggle shows/hides lore on item            | 🎯        |
| E | Colour codes (`&a`, `&l`, any order)              | 🎯        |
| F | Alias (`/cb note`) · quick-add · `/cb lore clear` | 🎯        |
| G | Survives server restart                           | 🎯        |
| A | Share & Import via cloud vault                    | 🧊 parked |

🔗 **Related Doc:** [GROUP_18_NOTES_STAGING.md](../groups/GROUP_18_NOTES_STAGING.md)

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

**A · Share & Import — 🧊 parked until `vaultEndpoint` set**
"the cloud vault isn't set up yet" = correct behavior today, not a bug.

| # | Action                                    | Expected Result                                        | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb lore g18a` → **Share**               | chat shows share code + `[copy code]`                  | ⏳ | ⏳ |
| A2 | `/cb note import g18b <code>`             | g18b's lore matches g18a's exactly                     | ⏳ | ⏳ |
| A3 | import onto a block that already has lore | prompted to `/cb confirm`; `/cb cancel` keeps old lore | ⏳ | ⏳ |

</details>

<details><summary>🗄️ <b>History — superseded approaches</b> (click to open)</summary>

- **First-pass 3-tab Book GUI (G18.1–G18.7):** Lore via book-and-quill, To-Do tab, Hover Tooltip tab — 🎯 not tested, never in-game confirmed. Superseded 2026-06-21 by single-screen Lore revamp.
- **Staging system (G18.8–G18.14):** `draft`/`publish`/`stage`/`release`/`staging`/`resume` — scrapped 2026-06-21 via SWEEP_INDEX §A. Macros + pack debounce cover the need.
- Full spec + locked decisions → `groups/GROUP_18_NOTES_STAGING.md`.

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **B · Add lines** (Passed 2026-06-21). Test history moved to [GROUP_18_NOTES_STAGING.md](GROUP_18_NOTES_STAGING.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · Edit + delete** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · On/Off toggle** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · Colour codes** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **F · Alias / quick-add / clear** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **G · Survives restart** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

