# Group 24 - Macros

## Status

| | |
| --- | --- |
| **Verdict** | Macro toggle is confirmed; record/play, single-undo, and G27 screen handoff still need clean current coverage. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-07-10 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Macro record and play logic | Built 🎯 | Discussion ✏️ |
| C | Macro single-undo batch | Built 🎯 | Discussion ✏️ |
| D | MacroListScreen handoff to G27 | Designed ⏳ | Discussion ✏️ |
| B | Macro on/off toggle | Done ✅ | - |
| E | Script and run commands | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_24_MACROS.md](../groups/GROUP_24_MACROS.md)

---

# Active Tests

## 💡 Setup

- Create `g24a` and `g24b` as disposable macro targets.
- Use `/cb macro` commands for logic tests.
- Screen layout and polish for `/cb gui macros` belong to G27; G24 checks command/data behavior.

## A - Macro record and play logic - Built 🎯

| | |
| --- | --- |
| **Check** | Recording, adding steps, stopping, listing, playing, and deleting macros works without reviving scripts. |
| **Pass rule** | Record, add, stop, play, list, delete, missing macro, bad step, and restart rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs fresh owner retest after template cleanup. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb macro record test-macro`. | Recording starts and names the macro. | 🎯 | 🎯 |
| A2 | Add `/cb setglow g24a 8` and `/cb setglow g24b 4`. | Both steps are stored in order with readable step labels. | 🎯 | 🎯 |
| A3 | Run `/cb macro stop`. | Recording stops cleanly. | 🎯 | 🎯 |
| A4 | Run `/cb macro play test-macro`. | Both glow changes apply in order. | 🎯 | 🎯 |
| A5 | Run `/cb macro list`. | Macro appears with useful step count/summary. | 🎯 | 🎯 |
| A6 | Play a missing macro name. | Error is clear and no action runs. | 🎯 | 🎯 |
| A7 | Restart server, then play the macro. | Saved macro still exists and plays. | 🎯 | 🎯 |
| A8 | Run `/cb macro delete test-macro`. | Macro is removed and no longer plays. | 🎯 | 🎯 |

## B - Macro on/off toggle - Done ✅

| | |
| --- | --- |
| **Check** | `/cb macro on` and `/cb macro off` control recording/playback without colliding with HUD toggles. |
| **Pass rule** | Off, play denied, record denied if applicable, on, play allowed, persistence, and message rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Run `/cb macro off`. | Macro system disables globally. | ✅ | ✅ |
| B2 | Run `/cb macro play test-macro` while disabled. | Message says `Macro system is disabled. Use /cb macro on to re-enable.` | ✅ | ✅ |
| B3 | Try recording while disabled. | Recording is denied or clearly follows the same disabled rule. | ✅ | ✅ |
| B4 | Run `/cb macro on`, then play again. | Macro runs normally. | ✅ | ✅ |
| B5 | Confirm `/cb on` and `/cb off` still belong to HUD behavior if present. | Macro toggle does not collide with G03 HUD commands. | ✅ | ✅ |

## C - Macro single-undo batch - Built 🎯

| | |
| --- | --- |
| **Check** | Playing a macro records one undo batch for the whole macro, not one undo per step. |
| **Pass rule** | Play, undo once, redo once, mixed command, failure rollback/report, and history label rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs current undo-stack evidence with G17 multi-undo behavior. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Play a macro with two glow changes. | Both changes apply. | 🎯 | 🎯 |
| C2 | Run `/cb undo` once. | Both macro changes revert in one undo entry. | 🎯 | 🎯 |
| C3 | Run `/cb redo` once. | Both macro changes reapply in one redo entry. | 🎯 | 🎯 |
| C4 | Inspect undo summary. | It names the macro and step count clearly. | 🎯 | 🎯 |
| C5 | Include one bad step in a test macro. | Failure is reported clearly and undo history is not corrupted. | 🎯 | 🎯 |

## D - MacroListScreen handoff to G27 - Designed ⏳

| | |
| --- | --- |
| **Check** | Macro GUI stays Screen-based and uses the G27 screen standard. |
| **Pass rule** | Open, list, play, edit, delete, info, record-new, empty state, and navigation rows pass in TG27. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen chrome and layout are G27; G24 owns the macro behavior behind it. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb gui macros`. | MacroListScreen opens, not a chest conversion. | ⏳ | ⏳ |
| D2 | Click a macro row. | Play, Edit, Delete, and Info actions are available. | ⏳ | ⏳ |
| D3 | Use Record New. | It starts the same macro-record logic tested in A. | ⏳ | ⏳ |
| D4 | Delete from the Screen. | It deletes the same saved macro data tested in A. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §B Macro on/off toggle — ✅ `2026-07-10`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §E Script and run commands — 👎 `2026-06-21`: Removed in favor of plain macros.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g24a` and `g24b`.
- [ ] Delete `test-macro`.
- [ ] Keep MacroListScreen layout issues in G27.
- [ ] Do not restore `script`, `scriptgui`, or `run`.

</details>
