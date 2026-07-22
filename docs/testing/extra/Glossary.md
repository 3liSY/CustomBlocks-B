# CustomBlocks-B Documentation Glossary

**Authority:** canonical and mandatory for every CustomBlocks-B document, testing guide, dashboard, and AI agent.

This file defines the words, status labels, symbols, and documentation rules used by the project. It is a rulebook, not a suggestion. A document must not invent a competing meaning, status, emoji, or lifecycle.

## 1. Authority Rules

1. This glossary is the only source of truth for project status and documentation terminology.
2. `CLAUDE.md` and `AGENTS.md` enforce this glossary for AI behavior. They do not create a second status system.
3. A testing guide records actions, evidence, and results. It does not repeat this glossary, add a legend, or contain AI instructions.
4. A group document records design and technical detail. A testing guide records how to verify the result in-game.
5. If two documents disagree, keep the factual evidence, mark the disagreement `Discussion ✏️`, and resolve the source document before changing the status.
6. Historical documents may contain old terminology, but active documents must use this glossary.

## Scope Boundary

This glossary governs the current documentation system only: canonical Group documents, canonical Testing Guides, the testing dashboard, this template, and agent documentation rules.

It does not retroactively change material in `docs/archive/` or `docs/Information/`. Those folders preserve earlier phase records with their original terminology and status language. They can provide dated history, but never override current Group or Testing Guide truth.

## 2. Status Labels

Every feature, slice, issue, or testable item has **exactly one** status.
Every status label is one word plus its symbol.

| Status | Symbol | Use it when | Do not use it when |
| --- | --- | --- | --- |
| **Planned** | 📜 | The idea exists, but the design is not fully locked. | A complete design already exists. |
| **Designed** | ⏳ | The behavior and design are locked, but working code does not exist. | Code exists, even if it has not been tested. |
| **Built** | 🎯 | Code exists and needs real Minecraft verification. | You only know that compilation succeeded. |
| **Done** | ✅ | The owner tested the required behavior in Minecraft and accepted it. | It is only locally built, assumed correct, or tested by code inspection alone. |

The exact one-word status label is required in section tables, section headers, archives, dashboards, and summaries. The symbol alone is allowed only in SP/MP result cells where the column meaning is already clear.

### Status rules

- Compilation, a passing Gradle build, or a successful code review never creates `Done ✅`.
- A partially working build normally remains `Built 🎯` and may receive `Polish 🎨` or `Discussion ✏️`.
- A design that is still being decided remains `Planned 📜`, even if a developer has started experimenting.
- A confirmed feature that later breaks may keep its factual status while carrying `Regression 💔` until the fix is re-confirmed.
- Never use `Passed`, `Partial`, `Not tested`, `Build green`, `build-green`, `Test now`, or the old status field name as a status.

## 3. Optional Flags

Flags add important context. They do not replace the status.

| Flag | Symbol | Meaning |
| --- | --- | --- |
| **Regression** | 💔 | Behavior that previously worked or was confirmed has been broken by a later change. |
| **Blocked** | ‼️ | Testing or progress cannot continue because a named dependency or decision is unavailable. |
| **Parked** | 💤 | Work is intentionally paused by the owner and is not currently active. |
| **Discussion** | ✏️ | The design, scope, ownership, or expected result is not settled. |
| **Polish** | 🎨 | The behavior works enough to evaluate, but appearance, wording, flow, or feel needs refinement. |
| **Scrapped** | 👎 | The item was intentionally rejected, removed, or replaced. Keep its history; do not silently erase it. |

### Flag rules

- An item may have zero or more flags, but every flag must have a short reason or linked evidence.
- `Blocked ‼️` and `Parked 💤` are different: blocked means progress cannot proceed; parked means the owner chose not to proceed now.
- `Discussion ✏️` stops design or implementation decisions until the owner resolves it.
- `Regression 💔` remains visible until the repaired behavior is tested and confirmed again.
- `Scrapped 👎` is terminal for that item. A replacement gets a new item identity and its own status.
- Do not create new flag emojis without updating this glossary first.

## 4. Status Transitions

The normal path is:

`Planned 📜` -> `Designed ⏳` -> `Built 🎯` -> `Done ✅`

Testing-guide section order:

`Built 🎯` -> flagged items -> `Designed ⏳` -> `Planned 📜` -> `Done ✅` -> `Parked 💤` -> `Scrapped 👎`

Allowed corrections:

- `Designed ⏳` -> `Planned 📜` when the design is reopened.
- `Built 🎯` -> `Designed ⏳` when the code is removed and the design remains.
- `Done ✅` -> `Built 🎯` with `Regression 💔` when a later change breaks it.
- Any active item -> `Scrapped 👎` when the owner rejects or removes it.
- Any status may carry `Blocked ‼️`, `Parked 💤`, `Discussion ✏️`, or `Polish 🎨` when the flag is truthful.

Never move an item forward merely to make a dashboard look better. Status follows evidence.

## 5. Testing Terms

| Term | Exact meaning |
| --- | --- |
| **In-game confirmation** | The owner tested the behavior inside Minecraft using the required setup and accepted the result. |
| **Evidence** | A dated observation, screenshot, copied log, command result, or other record that supports a status. |
| **Test case** | One repeatable action and its expected in-game result. |
| **Test row** | A single test case in a testing guide. |
| **Test section** | A group of related test rows belonging to one feature slice. |
| **SP** | Singleplayer or solo-world test. |
| **MP** | Dedicated server or multiplayer test. |
| **N/A** | The test cannot run in that environment. Use `➖`; it is not a status and does not count as a failed test. |
| **Regression** | A previously confirmed behavior that no longer works after a later change. Use the `Regression 💔` flag. |
| **Active test** | A test that still needs a real action. It is an activity label, not a seventh status. |
| **Progress percentage** | Confirmed runnable test slots divided by total runnable test slots, calculated from the actual test rows. In a TG status block it is shown only as a 10-block emoji bar plus an integer percentage. |

## 6. Project Document Terms

| Term | Exact meaning |
| --- | --- |
| **Idea** | A user request or possibility that has not yet been fully designed. Usually `Planned 📜`. |
| **Feature** | A user-visible capability with a defined purpose and expected behavior. |
| **Slice** | A separately buildable and testable part of a feature. |
| **Group** | The ownership area that contains the feature design and technical scope, such as G27. |
| **Testing Guide (TG)** | The clean, executable verification document for one group. |
| **Group document** | The design and implementation record owned by a group. It may contain technical details; the TG should not. |
| **Issue** | A uniquely identified problem, decision, or work item. It must have an owner group and stable ID. |
| **Finding** | A factual observation from testing. It is not automatically a bug; classify it as a bug, `Polish 🎨`, `Discussion ✏️`, or accepted behavior. |
| **Owner** | The project owner who decides scope, accepts behavior, and confirms in-game results. |
| **Canonical** | The one document or field that owns a fact. Other documents link to it instead of redefining it. |
| **History** | A dated record of what happened. History is never rewritten to hide an earlier state. |

## 7. Document Ownership

| Information | Belongs in |
| --- | --- |
| Definitions, statuses, symbols, and document rules | This glossary |
| AI behavior and enforcement | `CLAUDE.md` and `AGENTS.md`, pointing back here |
| Feature design and technical decisions | The relevant group document in `docs/groups/` |
| Repeatable in-game actions and expected results | The relevant TG in `docs/testing/` |
| Detailed findings, root causes, and implementation notes | `PROGRESS_LOG.md` or the relevant group document |
| Cross-group IDs and ownership | The canonical ID map |
| Percentages and summaries | Generated dashboard data, based on TG data |
| Historical or retired material | The approved archive location |

No document may become a second glossary by copying the status definitions or emoji legend.

## 8. Canonical Files and Dashboard

Each group has one current Group document and one current Testing Guide. Those two documents own current truth.

| Item | Rule |
| --- | --- |
| Group document | Lives in `docs/groups/` and owns final feature decisions, ownership, boundaries, and technical truth. |
| Testing Guide | Lives in `docs/testing/` and owns verdict, test rows, results, progress, and folded history. |
| Dashboard | Lives at `docs/testing/Dashboard.md`, is generated from canonical guides, and links to current documents only. It never overrides them. |
| Archive | Preserves history. It cannot override an active Group document or Testing Guide. |

### Testing Guide filenames

- `Testing_Guide_XX.md` is the one active guide.
- `Testing_Guide_XX_Done.md` is allowed only when the guide is fully confirmed: `100%`, no active tests, and every in-scope section is `Done ✅`. Parked or scrapped future ideas may remain folded in the guide.
- `Testing_Guide_XX_Scrapped.md` is allowed only when the entire guide is intentionally scrapped and has no active work.
- A status suffix renames the one guide. It never creates a second copy.
- A filename, Verdict, Progress, Sections table, and folded archive must agree. When they disagree, preserve evidence and repair the conflict before using the status in a dashboard.

### Dashboard rules

- The dashboard is a clickable index, not a second feature specification.
- It may show only the group, Verdict, progress, last-tested date, status, and links to the canonical Testing Guide and Group document.
- Status sections are folded by default. The current `Continue` row is the only expanded work summary.
- Refresh it with `python docs/testing/extra/testing_tools.py dashboard` after a guide rename, status change, or link change.

## 9. Testing Guide Rules

Every active TG must:

1. Use the exact four statuses and exact optional flags from this glossary.
2. Give each feature slice one status and clearly separate optional flags.
3. Record the actual SP and MP result for every runnable test row.
4. Use `➖` only when an environment genuinely cannot run that row.
5. Keep the banner short and professional. Do not include glossary reminders, legends, AI instructions, root causes, or implementation deep-dives.
6. Put confirmed section history in the guide's history area according to the current TG template.
7. Never claim `Done ✅` without owner confirmation and evidence.

### Archive folding rule

Every TG archive uses the same fold order:

1. `✅ Confirmed`
2. `💔 Regression`
3. `📜 Planned`
4. `💤 Parked`
5. `👎 Scrapped`

When a test or section is fully passed, the archive bullet uses a short feature name, then the exact passed marker:

```md
- §A Feature name — ✅ `YYYY-MM-DD`
```

The passed marker itself is only `✅ ` plus the backticked date. Do not write `Passed`, `Done on`, `confirmed in-game`, or extra status prose after it.

In the `## Sections` table, confirmed work uses only `Done ✅` in the `Status` column. Do not put dates, archive markers, or archive prose in `## Sections`.

Use one archive bullet per folded item. Put technical details in the group document or progress log, not inside the archive marker.

### Status block rule

Every TG must use this exact four-row status block:

| | |
| --- | --- |
| **Verdict** | Editing tab is built; resize controls still need implementation and in-game testing. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-07-17 |
| **Jar** | `customblocks-1.0.0.jar` |

- `Verdict` is one line max and is the only descriptive status summary. It must say what is done and what remains without jargon.
- `Progress` is one line max and contains exactly 10 bar emojis plus one integer percentage. Use `🟩` for confirmed progress and `🟥` for remaining progress. The number of green blocks is the percentage floor divided by 10; `100%` uses 10 green blocks. Do not add counts, prose, parentheses, or explanations.
- `Last tested` is one line max and contains only the date in `YYYY-MM-DD` format.
- `Jar` is one line max and contains only the jar file name in backticks.
- The sections table must use exactly four columns: `§`, `Feature`, `Status`, `Flags`. Keep optional flags out of the `Status` cell.
- Use `💡` for setup and check lines. Do not use the old toolbox callout in TGs.

## 10. AI Enforcement

Before an AI reads, writes, edits, classifies, or summarizes any project status or testing document, it must read this file.

An AI must:

- use only the exact statuses and flags defined here;
- preserve factual evidence when repairing terminology;
- stop and surface a conflict instead of silently choosing between contradictory documents;
- keep glossary rules out of individual TGs;
- update the canonical source and any generated views consistently;
- never mark code as done from compilation alone;
- never invent a status because the available choices feel incomplete.

## 11. Migration Rules

When repairing older documents:

1. Preserve the original date and factual history.
2. Translate old labels only when the evidence clearly determines the new status.
3. Replace `Not Tested` with `Built 🎯` when code exists.
4. Replace `Passed` with `Done ✅` only when owner confirmation exists; otherwise use `Built 🎯`.
5. Replace `Partial` with the truthful status plus `Polish 🎨`, `Discussion ✏️`, or `Blocked ‼️` as appropriate.
6. Replace old color/status symbols with the exact symbols in this glossary.
7. If the evidence is contradictory, keep the item visible and use `Discussion ✏️` until resolved.
8. Remove copied legends and glossary reminder lines from TGs after the meanings are enforced centrally.

This migration is a controlled repair. It must not erase history, hide regressions, or convert uncertainty into a false success.
