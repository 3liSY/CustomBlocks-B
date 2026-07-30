# Group 07 - Bulk Operations

## Status

| | |
| --- | --- |
| **Verdict** | The two backend items are confirmed; re-id has just moved out of chat into the Workbench and needs an in-game check. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟥🟥🟥🟥 66% |
| **Last tested** | 2026-07-20 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| D | Re-ID is Workbench-only | Built 🎯 | - |
| A | Bulk undo/redo batch recording | Done ✅ | - |
| C | Chat bulk command backend | Done ✅ | - |

> Bulk Hub **Screen** targeting (search, filter chips, Category ▾, tick-to-select) moved to [G27 §D](Testing_Guide_27.md) — it is a screen test, not G07 backend.

**Original Group:** [GROUP_07_BULK_OPERATIONS.md](../groups/GROUP_07_BULK_OPERATIONS.md)

---

# Active Tests

The Bulk Operations Hub **Screen** (opening it, targeting blocks, search, filter chips, the new **Category ▾** filter, tick-to-select) is a screen test and lives in **[G27 §D — Bulk Operations Hub screens](Testing_Guide_27.md)**. Test it there.

## D - Re-ID is Workbench-only - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb bulkreid` opens the Workbench, and the old typed form says where it went. |
| **Pass rule** | D1 through D4 pass in SP and MP. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb bulkreid` with nothing after it. | Workbench opens on Re-ID. | 🎯 | 🎯 |
| D2 | Run `/cb bulkreid all prefix test_`. | A message pointing back to `/cb bulkreid`, no ids changed. | 🎯 | 🎯 |
| D3 | Re-id a few blocks from the Workbench and apply. | Ids change, textures and placed blocks unchanged. | 🎯 | 🎯 |
| D4 | Run `/cb undo` after D3. | All those ids go back in one step. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Bulk undo/redo batch recording — ✅ `2026-07-20`
- §C Chat bulk command backend — ✅ `2026-07-15`

</details>

<details><summary>💔 <b>Regression</b></summary>

- Hub Console/NL bar sent dead filter strings — 💔 `2026-07-15`. First rebuilt to concrete-tick `2026-07-20`, but the NL parser still mis-targeted (a phrase like "glow 10 all red" ticked **all** blocks, ignoring "red"). Resolved `2026-07-20` by **ripping the NL command bar entirely** and adding a Category ▾ filter. Screen targeting now lives in [G27 §D](Testing_Guide_27.md).

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- Branching undo — 💤 `2026-07-12`

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Chest bulk menus and chest block list — 👎 `2026-07-10`
- Health tab and real scan — 👎 `2026-07-12`
- 3x3 op-picker grid — 👎 `2026-07-12`
- AND/OR/NOT filter builder and escalation — 👎 `2026-07-12` (client code fully ripped `2026-07-20`)
- Natural-language (NL) command bar + `BulkNlParser`/`BulkNlBar` — 👎 `2026-07-20` (mis-targeted; ripped entirely, replaced by tick + Category ▾ filter, now G27 §D)
- Bulk recolor ownership in G07 — 👎 `2026-07-12` (advanced Hub screen moved to G27 §O `2026-07-24`; live `/cb bulkrecolor` hue-shift op stays here in G07, unrelated to the Hub — see G27 §O note)
- Bulk shape command — 👎 `2026-07-18` (scrapped outright; no longer waiting on G08)
- Dedicated `bulksound` literal — 👎 `2026-07-18` (sound already works through `bulkproperty`)
- `/cb bulkreid` chat argument forms (prefix/suffix/replace) — 👎 `2026-07-27` (owner call; the Workbench shows what it will change first, chat could not)

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete all throwaway bulk test blocks after running the rows.
- [ ] Keep bulk Workbench and Set All screen verification in G27.
- [ ] Do not restore deleted chest bulk menus, the old filter-builder language, or the ripped NL command bar.
- [ ] Move any advanced bulk-recolor-Hub findings to G27 §O instead of reopening it here; findings about the live `/cb bulkrecolor` hue-shift op stay in G07.

</details>
