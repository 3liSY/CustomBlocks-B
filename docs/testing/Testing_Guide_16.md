# Group 16 - Diagnostics, IT Screen & Private Testing Center

## Status

| | |
| --- | --- |
| **Verdict** | Diagnostics has strong historical coverage, but Debug Log retest, screen migration, and the private Testing Center are still open. |
| **Progress** | 🟩🟩🟩🟩🟩🟥🟥🟥🟥🟥 50% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Broken-block diagnostics | Built 🎯 | - |
| B | Advanced Debug Log viewer | Built 🎯 | Discussion ✏️ |
| C | Current IT Chest diagnostics and admin tools | Built 🎯 | Discussion ✏️ |
| D | Feedback FX board | Built 🎯 | Discussion ✏️ |
| E | Private Testing Center `/cb testing` | Designed ⏳ | Discussion ✏️ |
| F | Unified IT Screen migration | Designed ⏳ | Discussion ✏️ |
| G | Achievement live trigger | Planned 📜 | Parked 💤 |
| H | Screenshot command | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_16_DIAGNOSTICS.md](../groups/GROUP_16_DIAGNOSTICS.md)

---

# Active Tests

## 💡 Setup

- Use the owner account on the configured test server.
- Current private server target: `yoyoo.mcsh.io`.
- Keep one disposable block id available, such as `g16_diag`.
- Keep `logs/latest.log` openable outside game for evidence comparison.
- Treat older chest confirmations as history; current rows must be retested against the latest design.

## A - Broken-block diagnostics - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb showbrokenblocks` finds missing-texture blocks and routes fixes through diagnostics, not through G09 ownership. |
| **Pass rule** | Empty state, broken listing, fix selected, single fix, select all, delete-to-trash, and safety-dashboard link pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb showbrokenblocks` with no broken blocks. | A clean empty state appears with count 0 and no confusing warning. | 🎯 | 🎯 |
| A2 | Create or force one block with a missing baked texture, then reopen. | The broken block appears with id, slot/source details, and a clear red issue state. | 🎯 | 🎯 |
| A3 | Tick one or more entries and choose Fix selected. | Blocks with saved source re-bake; blocks without source are skipped with a clear reason. | 🎯 | 🎯 |
| A4 | Right-click one broken entry. | Saved-source blocks fix immediately; source-less blocks prefill or route to retexture. | 🎯 | 🎯 |
| A5 | Use Select all and Clear selection across pages. | Selection count updates correctly and does not forget off-page selections. | 🎯 | 🎯 |
| A6 | Delete selected, then confirm. | Blocks go to trash/restorable flow, not hard deletion. | 🎯 | 🎯 |
| A7 | Open from the Safety dashboard link. | G09 links into this diagnostics surface without owning the scanner UI. | 🎯 | 🎯 |

## B - Advanced Debug Log viewer - Built 🎯

| | |
| --- | --- |
| **Check** | The in-game Debug Log shows `[CustomBlocks]` lines with severity filtering and copyable reports. |
| **Pass rule** | Open, summary, filter, severity render, copy line, copy filtered, and forced-error rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but needs one fresh owner test round after the template cleanup. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Open `/cb diag`, then choose Debug Log. | A log view opens with newest `[CustomBlocks]` lines first. | 🎯 | 🎯 |
| B2 | Hover the summary tile. | Lore shows error, warning, info, shown, and total counts; it glints when issues exist. | 🎯 | 🎯 |
| B3 | Cycle the severity filter. | All, Issues, Errors, Warnings, and Info each show only matching lines with correct counts. | 🎯 | 🎯 |
| B4 | Inspect line tiles. | Info uses a calm item, warnings and errors are visually stronger, and hover shows the raw line. | 🎯 | 🎯 |
| B5 | Click one log line and use the chat copy button. | Clipboard receives the full raw line exactly. | 🎯 | 🎯 |
| B6 | Use Copy filtered. | Clipboard receives every currently filtered line, one per row. | 🎯 | 🎯 |
| B7 | Force a failed texture URL, then filter to Errors. | The new failure appears near the top with a useful reason. | 🎯 | 🎯 |

## C - Current IT Chest diagnostics and admin tools - Built 🎯

| | |
| --- | --- |
| **Check** | Existing diagnostics commands still work while the future Screen rewrite is designed. |
| **Pass rule** | Dashboard, incidents, audit, cache, report, confirm/cancel, and clear rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Later Screen migration may replace chest presentation, so evidence should say which UI was tested. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb diag`. | Diagnostics opens with health, incidents, mutation history, and controls. | 🎯 | 🎯 |
| C2 | Cause a bad image download. | A structured incident records time, actor/system, context, block id when available, and error. | 🎯 | 🎯 |
| C3 | Click a failed-texture incident with a stored URL. | It retries from the stored URL or opens the right editor when no fix exists. | 🎯 | 🎯 |
| C4 | Run `/cb audit` and `/cb audit <player>`. | Mutation history appears and player filtering works. | 🎯 | 🎯 |
| C5 | Run `/cb cache` and `/cb cache clear`. | Cache stats are readable and cleanup avoids live textures, pack files, sources, and backups. | 🎯 | 🎯 |
| C6 | Generate a diagnostics report. | A report file is written and a download/copy link is shown. | 🎯 | 🎯 |
| C7 | Use `/cb confirm` and `/cb cancel` with and without a pending bulk action. | Pending actions confirm/cancel; empty state wording is clear and human. | 🎯 | 🎯 |

## D - Feedback FX board - Built 🎯

| | |
| --- | --- |
| **Check** | Particle and sound feedback are one splittable system with clear per-category controls. |
| **Pass rule** | Open, master toggle, expand, particle-only, sound-only, preview, persistence, and bulk-complete rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Live achievement firing is intentionally parked until an achievement system exists. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Open `/cb feedback`, `/cb particles`, and `/cb sounds`. | All routes reach the same feedback board. | 🎯 | 🎯 |
| D2 | Toggle a category master tile. | Both particle and sound flags change together and persist. | 🎯 | 🎯 |
| D3 | Expand one category. | Separate FX and Sound tiles appear without colliding with footer controls. | 🎯 | 🎯 |
| D4 | Disable only sound for error, then trigger an error. | Error particles still show, but the error sound is silent. | 🎯 | 🎯 |
| D5 | Disable only particles for success, then trigger success. | Success sound still plays, but particles are silent. | 🎯 | 🎯 |
| D6 | Right-click preview a category. | Preview plays both particle and sound even if that category is disabled. | 🎯 | 🎯 |
| D7 | Complete a bulk action. | `bulk_complete` uses the special feedback, not the plain success cue. | 🎯 | 🎯 |

## E - Private Testing Center `/cb testing` - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb testing` should be a private owner-only testing workspace linked to the real Testing Guides and owner result notes. |
| **Pass rule** | Access lock, guide list, progress, split guide/result view, auto-save, report export, server sync, and teardown rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design is locked enough to document, but UI/build work still needs implementation. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Owner account runs `/cb testing` on `yoyoo.mcsh.io`. | Private dashboard opens and shows every TG, including completed ones. | ⏳ | ⏳ |
| E2 | Different account runs `/cb testing` on the same server. | Access is denied cleanly, even if the account is operator. | ⏳ | ⏳ |
| E3 | Owner account runs `/cb testing` on a different server. | Access is denied unless that server is configured. | ⏳ | ⏳ |
| E4 | Inspect the dashboard. | It shows overall percentage, per-guide percentage, done, active, failed, blocked, needs-retest, latest activity, and next useful test. | ⏳ | ⏳ |
| E5 | Open one guide. | Left side shows the original TG read-only; right side shows editable owner results for the selected row. | ⏳ | ⏳ |
| E6 | Edit a result note and wait. | Notes auto-save without needing a final Save button. | ⏳ | ⏳ |
| E7 | Add expected/actual, severity, reproduction, server/version, date/time, evidence, follow-up, and history. | Result panel stores professional test evidence without changing the guide text. | ⏳ | ⏳ |
| E8 | Use the report/export action. | A clean copyable report is produced without mentioning external AI workflow. | ⏳ | ⏳ |
| E9 | Join from another computer as the same account/server. | Stored results and progress reappear because they are server-side and owner-bound. | ⏳ | ⏳ |
| E10 | Use the final nuke/delete control after confirming twice. | Private testing data, result history, progress cache, evidence references, and access lock are removed. | ⏳ | ⏳ |

## F - Unified IT Screen migration - Designed ⏳

| | |
| --- | --- |
| **Check** | Diagnostics chest surfaces should become a unified tabbed Screen where appropriate. |
| **Pass rule** | Health, incidents, audit, report, debug log, feedback separation, navigation, and old command routing rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Requires the G27 screen system and careful migration from existing chest menus. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Open diagnostics after the migration. | Health, incidents, audit, report, and debug log are tabs in one IT Screen. | ⏳ | ⏳ |
| F2 | Navigate from `/cb incidents`, `/cb audit`, `/cb report`, and Debug Log. | Commands route to the correct tab without separate disconnected menus. | ⏳ | ⏳ |
| F3 | Open Feedback FX. | Feedback remains its own surface because it is reached by its own commands too. | ⏳ | ⏳ |
| F4 | Trigger incidents and refresh. | Screen data refreshes without losing selected tab or filter state. | ⏳ | ⏳ |
| F5 | Compare to the old chest workflow. | No diagnostic capability disappears during the migration. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §C IT Chest dashboard — ✅ `2026-06-21`
- §C Structured incident auto-fix — ✅ `2026-06-21`
- §C Audit, cache, and report commands — ✅ `2026-06-21`
- §D Feedback FX board — ✅ `2026-06-21`
- §D Bulk-complete feedback — ✅ `2026-06-21`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §G Achievement live trigger — 💤 `2026-06-21`: Waits for a real achievement system; preview stays testable.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §H Screenshot command — 👎 `2026-06-21`: Server-side screenshot capture is impractical on dedicated servers.
- `rp_regenerate` feedback category — 👎 `2026-06-21`: Manual pack reload feedback was not useful enough to keep.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g16_diag` and any broken-block fixtures.
- [ ] Clear temporary incidents after evidence is captured.
- [ ] Remove generated diagnostic reports that contain sensitive paths.
- [ ] Keep `/cb testing` data private to the configured owner/server pair.
- [ ] When the private Testing Center is retired, remove its command, UI, config, assets, stored data, and docs links.

</details>
