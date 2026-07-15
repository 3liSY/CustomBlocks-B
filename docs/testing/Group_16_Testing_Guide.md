# Group 16 — Diagnostics & IT Chest

**🎯 Active:** A, B, C, D, E (0/10)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                |
| --------------- | ------------------------------ |
| **Verdict**     | 🎯 5 rows need testing         |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/10) |
| **Last tested** | 2026-06-21 (owner)             |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): ItChestMenu/AuditMenu/ReportMenu/DebugLogMenu → one tabbed IT Screen. FeedbackMenu → separate Screen. Neither built. See `GROUP_16_DIAGNOSTICS.md`.
>
> 🔀 **The incidents SCREEN is not ours (2026-07-15).** `⊙ Details` → a Screen showing **one** incident (plus a
> History tab) is **Group 27 §R / §G27.31**, folded there from Group 04 §C. **Group 16 keeps `IncidentRecorder`
> and `/cb incidents` unchanged** — we record, Group 27 renders. Do not build an incidents GUI here.

## 🗺️ Sections

| § | What                                                             | State |
| --- | ---------------------------------------------------------------- | ----- |
| A | IT Chest dashboard · incident log · mutation row · clear/refresh | 🎯     |
| B | `/cb confirm` · `/cb cancel` wording                             | 🎯     |
| C | Feedback FX (merged particle + sound) R1–R4                      | 🎯     |
| D | Debug Log viewer — severity/filter/copy                          | 🎯     |
| E | `/cb showbrokenblocks` — list · fix · select · delete            | 🎯     |
| F | Private Testing Center — owner-only temporary workspace           | ⏳     |

🔗 **Related Doc:** [GROUP_16_DIAGNOSTICS.md](../groups/GROUP_16_DIAGNOSTICS.md) *(Assuming standard naming)*

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

- `achievement` FX live trigger — blocked on owner's achievement system (no ETA). Preview works today.
- `error_code_search` — Type a 3-character code (e.g. `E-45`) into the Incidents UI search bar to jump to the raw exception.

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **A · IT Chest dashboard** (Passed 2026-06-21). Test history moved to [GROUP_16_DIAGNOSTICS.md](GROUP_16_DIAGNOSTICS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **B · `/cb confirm` / `/cb cancel`** (Passed verified). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · Feedback FX (particle + sound)** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · Debug Log viewer** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · `/cb showbrokenblocks`** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

## F - Private Testing Center (owner-only, temporary)

This section covers the private `/cb testing` workspace described in
`GROUP_16_DIAGNOSTICS.md`. It is for the owner's account on the configured server only. The
initial configured server is `yoyoo.mcsh.io` and must remain changeable.

The guide instructions are read-only and must appear exactly as written. The owner records only
the test result in the panel beside the guide. Results auto-save to the selected server and must
still be available to the same owner from another computer on that server.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | On `yoyoo.mcsh.io`, the owner runs `/cb testing`. | The private Testing Center opens. | ➖ | ⏳ |
| F2 | A different account on `yoyoo.mcsh.io` runs `/cb testing`. | The command is denied and no Testing Center content is exposed. | ➖ | ⏳ |
| F3 | The owner runs `/cb testing` on a server other than the configured server. | The command is denied and the private workspace does not open. | ➖ | ⏳ |
| F4 | Open the dashboard. | All existing Testing Guides are visible, including completed guides, with overall and per-guide percentages, counts, statuses, and recent activity. | ➖ | ⏳ |
| F5 | Open a guide from the dashboard. | The guide appears exactly as written; its instructions and ordering are unchanged and protected. | ➖ | ⏳ |
| F6 | Select a test inside a guide. | The guide remains visible on the left and the owner's editable results panel appears beside it. | ➖ | ⏳ |
| F7 | Record Passed, Failed, Blocked, and Needs Retest results with detailed notes. | Each result supports expected/actual text, severity, reproduction steps, environment details, follow-up, evidence references, and history. | ➖ | ⏳ |
| F8 | Enter and edit a result, then leave and reopen the workspace. | Changes auto-save without a separate Save action and are restored exactly. | ➖ | ⏳ |
| F9 | Join the same server from another computer using the owner's account. | The same results, history, progress, and evidence references are available. | ➖ | ⏳ |
| F10 | Use the dashboard filters and next-test controls. | The owner can quickly find unfinished, failed, blocked, needs-retest, and recent work, then open the next useful test. | ➖ | ⏳ |
| F11 | Use the final delete/nuke control and then run `/cb testing`. | The private results, history, evidence references, progress cache, and access lock are removed; the command no longer opens the workspace. | ➖ | ⏳ |

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---
