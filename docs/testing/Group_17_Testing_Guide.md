# Group 17 — Command Regressions

**🎯 Active:** B, C, D, E (0/8)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                |
| --------------- | ------------------------------ |
| **Verdict**     | 🎯 4 sections need retesting    |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/8) |
| **Last tested** | 2026-06-21                     |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-10): Search results UI (§C) is Screen-based per the mod-wide Screen migration
> (`GROUP_17_REGRESSIONS.md` audit note). It's already built/confirmed as a chest GUI — owner confirmed
> Screen spec moved 2026-07-12 → `GROUP_27_SCREENS.md` §G27.32.
> 2026-07-10: convert to Screen (default applies, no exception). Real rework, scheduled as ⏳ planned.

## 🗺️ Sections

| § | What                                            | State          |
| --- | ----------------------------------------------- | -------------- |
| B | Multi-undo · undo all · undo clear · multi-redo | 🎯              |
| C | Search GUI                                      | 🎯              |
| D | `give <amount>` · `give <amount> <player>`      | 🎯              |
| E | `delete #` (targeted custom block)              | 🎯              |
| A | fav · unfavorite · recent · export alignment    | 🧊 parked (G25) |

🔗 **Related Doc:** [GROUP_17_REGRESSIONS.md](../groups/GROUP_17_REGRESSIONS.md)

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | What                                               | Owner           |
| --- | -------------------------------------------------- | --------------- |
| A | `favorite` · `unfavorite` · `fav` alias · `recent` | **Group 25**    |
| A | Export alignment                                   | **Issue 17.15** |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **B · Multi-undo / redo** (Passed 2026-06-21). Test history moved to [GROUP_17_REGRESSIONS.md](GROUP_17_REGRESSIONS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · Search results UI** (Passed 2026-06-21, chest GUI today; target = Screen, migration not started). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · give with args** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · delete #** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

