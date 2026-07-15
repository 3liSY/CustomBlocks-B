# Group 24 — Macros

**🎯 Active:** A (0/14)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                     |
| --------------- | ----------------------------------- |
| **Verdict**     | 🎯 7 rows need testing               |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/14)       |
| **Last tested** | —                                    |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-10): §C previously said "convert `MacroListScreen` to a chest GUI" — backwards
> vs. the mod-wide Screen migration. Reverted: `MacroListScreen` STAYS Screen-based; §C is polish on the
> existing Screen, not a medium swap.

## 🗺️ Sections

| § | What                                                     | State      |
| --- | ----------------------------------------------------------- | ---------- |
| A | Base macro system (record/add/play/list/delete/cancel)   | 🎯 test-now |
| B | Macro on/off toggle (`/cb macro on`/`off`)               | ⏳ not built |
| C | Macro GUI polish (stays `MacroListScreen`, Screen-based) | ⏳ not built |
| D | Step labels + single-undo batch                          | ⏳ not built |

🔗 **Related Doc:** [GROUP_24_MACROS.md](../groups/GROUP_24_MACROS.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g26a MacroTest`
* `/cb create g26b MacroTarget`

**A · Base macro system (built, never confirmed under the new TG system)**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb macro record foo` | "Recording macro 'foo'..." | 🎯 | 🎯 |
| A2 | `/cb macro add setglow g26a 5` (a couple steps) | "Added: /setglow g26a 5" each time | 🎯 | 🎯 |
| A3 | `/cb macro stop` | "Macro 'foo' saved. Play with /cb macro play foo" | 🎯 | 🎯 |
| A4 | `/cb macro play foo` | Steps run in order; "Done: N/N succeeded" | 🎯 | 🎯 |
| A5 | `/cb macro list` | Lists saved macros with clickable `[play]`/`[delete]` | 🎯 | 🎯 |
| A6 | `/cb macro cancel` mid-recording | "Recording cancelled." — macro not saved | 🎯 | 🎯 |
| A7 | `/cb macro delete foo` | "Deleted macro 'foo'." | 🎯 | 🎯 |

> Verified in source 2026-07-04: `MacroCommands.java` + `MacroManager.java` implement all seven
> subcommands (record/add/stop/cancel/play/list/delete). This is the old Phase-13 macro system —
> real, working code — but no session in `PROGRESS_LOG.md` shows it confirmed in-game under this
> group. TG previously showed "⏳ planned, 0/0" as if nothing existed — wrong, corrected here.

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(none yet)*

</details>

<details><summary>⏳ <b>Planned / Not built</b> (click to open)</summary>

| § | What                                                                                       | Spec |
| --- | ------------------------------------------------------------------------------------------- | ---- |
| B | `/cb macro on` / `/cb macro off` — global toggle; `play` while off should say "Macro system is disabled." Confirmed absent from `MacroCommands.java` (no such literal). | GROUP_24_MACROS.md §2 |
| C | `/cb gui macros` polish on the existing `MacroListScreen` (Play/Edit/Delete/Info sub-menu, Record New + Enable/Disable controls). Stays Screen-based — not a chest GUI conversion. | GROUP_27_SCREENS.md §G27.24 (moved 2026-07-12) |
| D | Descriptive step labels (auto-generated from command) + single combined undo entry per macro playback. No evidence in `MacroManager.java`/`MacroCommands.play()` — playback just executes steps sequentially, no undo wrapping. | GROUP_24_MACROS.md §1 |
| — | `script`/`scriptgui`/`run` (conditional/loop macros) — ⛔ **scrapped**, do not build (SWEEP_INDEX §A) | — |

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ------------------ | ------ |
|        |          |                     |        |

---

