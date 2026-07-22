# Group 17 - Command Regressions

## Status

| | |
| --- | --- |
| **Verdict** | Owned command regressions are confirmed; only the search Screen migration remains as a G27 handoff. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟥🟥 80% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Search command route and G27 handoff | Built 🎯 | Discussion ✏️ |
| B | Multi-undo and redo | Done ✅ | - |
| C | Give amount and player args | Done ✅ | - |
| D | Delete targeted custom block with `#` | Done ✅ | - |
| E | Favorites and recent ownership | Planned 📜 | Scrapped 👎 |
| F | Export structure alignment ownership | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_17_REGRESSIONS.md](../groups/GROUP_17_REGRESSIONS.md)

---

# Active Tests

## 💡 Setup

- Create disposable ids: `g17a`, `g17b`, `g17c`, `g17d`.
- Keep one placed `g17a` or `g17b` block for `delete #`.
- Use a second online player only for the give-to-player row.
- These tests are regression retests; confirmed rows stay archived until a fresh failure appears.

## A - Search command route and G27 handoff - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb search <query>` must not regress to text-only output, while its final Screen migration is tracked outside G17. |
| **Pass rule** | Results route, click-to-edit, zero-results message, and G27 handoff rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen conversion belongs to G27; G17 only guards the command regression. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Create `g17c SearchTest1` and `g17d SearchTest2`, then run `/cb search SearchTest`. | A results UI opens with both matches; it is not plain text output. | 🎯 | 🎯 |
| A2 | Click one result. | The selected block editor opens for that block. | 🎯 | 🎯 |
| A3 | Search for a string with no matches. | Chat gives a clear no-results message and does not open an empty screen. | 🎯 | 🎯 |
| A4 | Compare current UI to the G27 Screen target. | Any chest-to-Screen mismatch is recorded as G27 work, not a G17 command failure. | 🎯 | 🎯 |

## B - Multi-undo and redo - Done ✅

| | |
| --- | --- |
| **Check** | `/cb undo` and `/cb redo` support multi-step, all, and clear flows without losing detail. |
| **Pass rule** | Multi-undo, multi-redo, all, over-count, clear-confirm, and batch rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Run `/cb setglow g17a 4`, `8`, `12`, then `/cb undo 3`. | Three changes undo in one command with value-bearing summary and glow returns to 0. | ✅ | ✅ |
| B2 | Run `/cb redo 3`. | Three changes redo in one command and glow returns to 12. | ✅ | ✅ |
| B3 | Use `/cb undo all` and `/cb redo all`. | Each command handles the whole available stack and reports real counts. | ✅ | ✅ |
| B4 | Run `/cb undo 99` with fewer than 99 steps. | It undoes what exists and reports the actual number, not an error. | ✅ | ✅ |
| B5 | Run `/cb undo clear`, then confirm. | Undo and redo stacks clear only after confirmation. | ✅ | ✅ |

## C - Give amount and player args - Done ✅

| | |
| --- | --- |
| **Check** | `/cb give` supports self amount, online recipient, OP gate, overflow safety, and clear errors. |
| **Pass rule** | Self, amount, tab-complete, overflow, recipient, non-OP, and offline-player rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb give g17b`. | One item is given to the caller. | ✅ | ✅ |
| C2 | Run `/cb give g17b 5`. | Five items are given to the caller. | ✅ | ✅ |
| C3 | Tab-complete after `/cb give g17b `. | Useful amount suggestions appear, such as 1, 16, 32, and 64. | ✅ | ✅ |
| C4 | Fill inventory nearly full, then run `/cb give g17b 6400`. | Only the amount that fits is inserted; overflow is reported and no items drop. | ✅ | ✅ |
| C5 | As OP, run `/cb give g17b 2 <online-player>`. | Recipient receives two items and both players get clear messages. | ✅ | ✅ |
| C6 | As non-OP, try giving to another player. | Self-give remains available, but giving to others is denied. | ✅ | ✅ |
| C7 | Try an offline player name. | Error says the player must be online. | ✅ | ✅ |

## D - Delete targeted custom block with `#` - Done ✅

| | |
| --- | --- |
| **Check** | `/cb delete #` deletes the looked-at custom block definition and stays undoable. |
| **Pass rule** | Target delete, undo, vanilla/air rejection, Arabic rejection, and normal id delete rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Place `g17b`, look at it, then run `/cb delete #`. | The targeted custom block definition is deleted and the message says it is undoable. | ✅ | ✅ |
| D2 | Run `/cb undo`. | The deleted block definition returns with texture and data intact. | ✅ | ✅ |
| D3 | Look at a vanilla block or air, then run `/cb delete #`. | Clear message says the target is not a custom block and nothing is deleted. | ✅ | ✅ |
| D4 | Aim at an Arabic letter block, then run `/cb delete #`. | It is rejected as not a normal custom slot block. | ✅ | ✅ |
| D5 | Run `/cb delete g17c`. | Normal id deletion still works unchanged. | ✅ | ✅ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §B Multi-undo and redo — ✅ `2026-06-21`
- §C Give amount and player args — ✅ `2026-06-21`
- §D Delete targeted custom block with `#` — ✅ `2026-06-21`
- §A Search command route — ✅ `2026-06-21`

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

- §E Favorites and recent ownership — 👎 `2026-06-21`: Moved to G25.
- §F Export structure alignment ownership — 👎 `2026-06-21`: Moved out of G17.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g17a`, `g17b`, `g17c`, and `g17d` if they still exist.
- [ ] Clear any undo/redo stack created only for this test.
- [ ] Keep search Screen notes in G27, not in G17.

</details>
