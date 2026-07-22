# Group 28 - Undo System Rework

## Status

| | |
| --- | --- |
| **Verdict** | Undo command behavior is usable, but the visual undo/history UI is broken and needs the designed full rework. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-06-25 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Current undo GUI defects | Built 🎯 | Regression 💔 |
| B | Shared undo describe helper | Designed ⏳ | - |
| C | Bulk drill-in and child revert | Designed ⏳ | - |
| D | Persistent undo store | Designed ⏳ | - |
| E | UndoHistoryScreen handoff to G27 | Designed ⏳ | - |
| F | Old UndoMenu and HistoryMenu | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_28_CREATE_STUDIO.md](../groups/GROUP_28_CREATE_STUDIO.md)

---

# Active Tests

## 💡 Setup

- Create at least five test blocks.
- Run one single edit, one bulk edit, one bulk delete, one undo, and one redo.
- Compare `/cb undo` command output against the visual undo/history UI.
- G28 owns undo state/logic; G27 owns the final Screen chrome.

## A - Current undo GUI defects - Built 🎯

| | |
| --- | --- |
| **Check** | Existing undo/history GUI defects are real and should be reproduced before replacement. |
| **Pass rule** | Batch label, block id, friendly action, redo mirror, audit batch, lore count, and functional revert rows are documented. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Current UI is known defective; this section records the regression target for the rework. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Create a bulk operation and open the undo GUI. | Current GUI likely shows `?` or weak batch identity; capture exact behavior. | 💔 | 💔 |
| A2 | Compare GUI label to `/cb undo` command text. | Command text is the reference; GUI should be identified where it drifts. | 💔 | 💔 |
| A3 | Open history/audit view after a batch. | Current view may show `×N` as if it were a block id; capture exact behavior. | 💔 | 💔 |
| A4 | Open redo side after undoing a batch. | Redo presentation should show the same defects if still duplicated. | 💔 | 💔 |
| A5 | Click the bulk row to undo. | Determine whether function works even though presentation is bad. | 🎯 | 🎯 |

## B - Shared undo describe helper - Designed ⏳

| | |
| --- | --- |
| **Check** | Command reports, undo screen, redo screen, and audit history should share one batch-aware label/description helper. |
| **Pass rule** | Single edit, every attribute label, batch count, block preview, audit row, command output, and localization-safe rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | View a single glow/hardness/sound/collision/category edit. | Friendly label is correct everywhere. | ⏳ | ⏳ |
| B2 | View a bulk edit. | Row shows action, block count, and useful first-block/summary detail. | ⏳ | ⏳ |
| B3 | Compare `/cb undo` text and Screen row. | Same helper produces matching facts. | ⏳ | ⏳ |
| B4 | View audit history. | Audit row no longer has copied stale label logic. | ⏳ | ⏳ |
| B5 | View redo side. | Redo uses the same helper and stays consistent. | ⏳ | ⏳ |

## C - Bulk drill-in and child revert - Designed ⏳

| | |
| --- | --- |
| **Check** | Bulk steps can be inspected and reverted safely without corrupting redo history. |
| **Pass rule** | Drill-in, child list, whole-batch undo, single-child remove, batch shrink, redo, confirm threshold, cancel, and locked/missing rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Click a bulk entry in the future screen. | It expands to show every affected block with useful details. | ⏳ | ⏳ |
| C2 | Choose Undo whole step. | All children revert as one operation. | ⏳ | ⏳ |
| C3 | Choose one child entry to remove/revert. | Only that child reverts and the batch shrinks from N to N-1. | ⏳ | ⏳ |
| C4 | Redo after child removal. | Redo stack remains coherent and does not resurrect unwanted children. | ⏳ | ⏳ |
| C5 | Trigger a large revert above threshold. | Confirm dialog appears and cancel leaves everything unchanged. | ⏳ | ⏳ |

## D - Persistent undo store - Designed ⏳

| | |
| --- | --- |
| **Check** | Undo/redo history survives restart with a capped rolling per-player buffer. |
| **Pass rule** | Save, load, reload, disconnect, cap trim, per-player isolation, atomic write, and corrupt-file recovery rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Make several edits, restart server, then open history. | Undo entries survive restart. | ⏳ | ⏳ |
| D2 | Run `/cb reload` if applicable. | History reloads without losing current entries. | ⏳ | ⏳ |
| D3 | Exceed configured max steps. | Oldest entries trim FIFO and newest entries remain. | ⏳ | ⏳ |
| D4 | Test two players. | Each player sees the correct per-player stack. | ⏳ | ⏳ |
| D5 | Corrupt one undo JSON file in a test environment. | Error is recoverable and does not crash the server. | ⏳ | ⏳ |

## E - UndoHistoryScreen handoff to G27 - Designed ⏳

| | |
| --- | --- |
| **Check** | The final UI should be a G27 Screen with Undo, Redo, and Audit tabs. |
| **Pass rule** | Open, tabs, search/filter, drill-in, preview, rollback, confirm modal, keyboard, and route cutover rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Run `/cb undogui`, `/cb redogui`, and `/cb history`. | They open the G27 UndoHistoryScreen tabs after cutover. | ⏳ | ⏳ |
| E2 | Filter audit by player/date/block. | Audit tab narrows results without breaking undo/redo tabs. | ⏳ | ⏳ |
| E3 | Click one history entry and use rollback if admin. | Rollback action is clear, confirmed if needed, and records history safely. | ⏳ | ⏳ |
| E4 | Use keyboard shortcuts. | Screen follows G27 standard for close/help/navigation. | ⏳ | ⏳ |
| E5 | Confirm old chest routes. | Old UndoMenu/HistoryMenu are no longer the active route after owner confirms. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

- §A Current undo GUI defects — 💔 `2026-06-25`: Bulk/history visual rows are wrong and need full rework.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §F Old UndoMenu and HistoryMenu — 👎 `2026-07-12`: Replaced by planned G27 UndoHistoryScreen after cutover.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete undo test blocks.
- [ ] Reset undo config caps changed for stress tests.
- [ ] Remove intentionally corrupted undo-store fixtures.
- [ ] Keep screen chrome defects linked to G27.

</details>
