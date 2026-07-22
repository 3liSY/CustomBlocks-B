# Group 13 - Arabic Backend

## Status

| | |
| --- | --- |
| **Verdict** | Legacy Arabic behavior was confirmed, but the real-SlotBlock rebuild has conflicting source status and needs a clean checkpoint retest. |
| **Progress** | 🟩🟩🟩🟩🟥🟥🟥🟥🟥🟥 40% |
| **Last tested** | 2026-07-04 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| C | Join/reflow instantness after break | Built 🎯 | Regression 💔 |
| F | Type-a-word auto-build backend | Planned 📜 | Blocked ‼️ |
| B | Real SlotBlock Arabic rearchitecture checkpoints | Built 🎯 | Discussion ✏️ |
| D | Arabic tool and attribute parity | Designed ⏳ | Discussion ✏️ |
| E | Text Blocks backend | Designed ⏳ | - |
| G | `/cb arabic` command UX revamp | Planned 📜 | - |
| A | Legacy live-render Arabic behavior | Done ✅ | - |

**Original Group:** [GROUP_13_ARABIC.md](../groups/GROUP_13_ARABIC.md)

---

# Active Tests

## 💡 Setup

- Use the newest jar on a dedicated server for rearchitecture checks.
- Create a short Arabic word row with at least three letters.
- Keep Square, Triangle, Deleter, and attribute commands available.
- Test the G27 text-creation screen in TG27, not here.

## B - Real SlotBlock Arabic rearchitecture checkpoints - Built 🎯

| | |
| --- | --- |
| **Check** | Arabic letters/numbers behave as real SlotBlocks with Arabic metadata, not as a separate live-render block system. |
| **Pass rule** | CP1 through CP5 pass twice in MP, and the source status conflict is resolved in the group doc. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Group doc still contains conflicting status language: checkpoint rows say built/tested, while a later table says designed/nothing built. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Open creative/search for Arabic letters and numbers. | Real Arabic slots are listed from the repurposed Arabic Letters tab; old live-render `arabic_letter` entries are gone. | 🎯 | 🎯 |
| B2 | Place isolated, initial, medial, and final letter slots. | Art is font-baked correctly, full-bright, and not using the wrong old bundled hand-art for letters. | 🎯 | 🎯 |
| B3 | Place a word row and walk behind it. | Back face reads the same word correctly from behind. | 🎯 | 🎯 |
| B4 | Place mixed black/red/green/yellow letters. | All four pre-baked colors use config hexes and join across colors. | 🎯 | 🎯 |
| B5 | Restart after the old system removal. | Old `customblocks:arabic_letter` NBT system and static number/letter blocks do not return. | 🎯 | 🎯 |

## C - Join/reflow instantness after break - Built 🎯

| | |
| --- | --- |
| **Check** | Breaking a letter updates neighboring forms immediately, not at server-paced delay. |
| **Pass rule** | Place, break, reflow, back mirror, and mixed-color rows pass twice in MP with no visible wait. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Build a 5-letter Arabic row quickly. | Forms join instantly while placing. | 🎯 | 🎯 |
| C2 | Break the middle letter. | Both sides reflow immediately with no visible delayed correction. | 🎯 | 🎯 |
| C3 | Re-add a letter into the gap. | Whole run re-pairs correctly, including back-face partners. | 🎯 | 🎯 |
| C4 | Repeat on a remote server with ping. | Client prediction and server reconciliation match with no flicker. | ➖ | 🎯 |

## D - Arabic tool and attribute parity - Designed ⏳

| | |
| --- | --- |
| **Check** | Arabic real slots must obey the same tool and attribute paths as normal SlotBlocks. |
| **Pass rule** | Deleter, Square, Triangle, glow, hardness, sound, collision, undo, and MP viewer refresh pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Shared block contract and real-slot rebuild must be reconciled before marking parity done. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Use the Deleter on an Arabic letter slot. | It uses the same delete/recycle behavior as normal blocks and is undoable. | 🎯 | 🎯 |
| D2 | Use a Square on an Arabic letter slot. | It swaps to the matching color while preserving facing, joins, and back partner. | 🎯 | 🎯 |
| D3 | Use a Triangle/custom color path on a letter. | If supported, it creates a joining sibling variant; if not supported yet, it answers honestly. | 🎯 | 🎯 |
| D4 | Run `/cb setglow <arabicId> 12`. | Letter lights like a normal block and the value survives restart. | 🎯 | 🎯 |
| D5 | Test hardness, sound, and collision attributes. | Arabic slot honors the same attribute values as normal SlotBlocks. | 🎯 | 🎯 |

## E - Text Blocks backend - Designed ⏳

| | |
| --- | --- |
| **Check** | Rebranded Text Blocks store editable text data and render Arabic/Latin phrase blocks for the G27 screen. |
| **Pass rule** | Create, edit, font, style, multiline, restart, and G27 handoff rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | G27 owns the front-end screen; G13 owns only backend/render/create services. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Create a text block through the G27 text screen. | G13 backend creates the block with stored `TextData`. | 🎯 | 🎯 |
| E2 | Reopen and edit that text block. | Existing text/style data loads and saves without creating a duplicate system. | 🎯 | 🎯 |
| E3 | Use Arabic and Latin text. | Arabic uses `arabtype.ttf`; Latin uses the chosen non-Arabic font path. | 🎯 | 🎯 |
| E4 | Restart and inspect the block. | Text data persists and renders identically. | 🎯 | 🎯 |

## F - Type-a-word auto-build backend - Planned 📜

| | |
| --- | --- |
| **Check** | Typed words should place connected real-slot Arabic rows on the ground with one-step undo. |
| **Pass rule** | Command, screen trigger, RTL placement, spaces, digits, collision stop, and undo rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Depends on the real SlotBlock Arabic rebuild landing cleanly. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb arabic build <word>`. | Connected RTL row appears in front of the player using real Arabic slots. | 🎯 | 🎯 |
| F2 | Build a word with a space. | Space becomes a gap and the next word starts fresh. | 🎯 | 🎯 |
| F3 | Build a word with digits. | Player chooses Eastern or Western digits; digits sit inline and do not join. | 🎯 | 🎯 |
| F4 | Aim into an occupied area. | Build stops before collision and reports how many blocks were placed. | 🎯 | 🎯 |
| F5 | Undo the word build. | Last built word is removed as one saved undo step. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Legacy placement flash and Square recolor — ✅ `2026-06-20`
- §A Legacy static-letter retirement Build B — ✅ `2026-06-20`
- §A Legacy no-reload preview path — ✅ `2026-06-18`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §C Join/reflow instantness after break — 💔 `2026-07-04`: Reflow works but feels server-paced after break.

</details>

<details><summary>📜 <b>Planned</b></summary>

- §G `/cb arabic` command UX revamp — 📜 `2026-07-04`: Deferred to a separate owner discussion.

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Hide/manage bundled letters — 👎 `2026-07-02`: Cancelled by static-block retirement.
- G13-owned text creation screen — 👎 `2026-07-17`: Moved to G27.
- Old live-render `ArabicLetterBlock` system — 👎 `2026-07-04`: Replaced by real SlotBlocks.
- `/cb arabic text` as a separate command surface — 👎 `2026-06-16`: Folded into text/word creation direction.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Resolve the G13-25 source status conflict before using this TG for final acceptance.
- [ ] Delete temporary Arabic letter/word rows after testing.
- [ ] Keep all screen-layout findings in TG27.
- [ ] Keep old-system behavior only as history, not active target behavior.

</details>
