# Group 25 - Block Management Extras & Identity Operations

## Status

| | |
| --- | --- |
| **Verdict** | ReID command core is confirmed; the case fix, ReID Screen, universal target shortcuts, and extras remain open. |
| **Progress** | 🟩🟩🟥🟥🟥🟥🟥🟥🟥🟥 20% |
| **Last tested** | 2026-06-27 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| D | Universal block targets `#` and `!` | Designed ⏳ | Blocked ‼️ |
| B | ReID case-mismatch fix | Built 🎯 | Discussion ✏️ |
| C | ReID Screen and editor entry | Designed ⏳ | Discussion ✏️ |
| H | Moved management extras ownership | Planned 📜 | Discussion ✏️ |
| E | Swap, duplicate, and identity extras | Planned 📜 | - |
| F | Custom drops | Planned 📜 | - |
| G | Block Finder, editor PNG export, and tab icon | Planned 📜 | - |
| A | ReID command core | Done ✅ | - |

**Original Group:** [GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md](../groups/GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md)

---

# Active Tests

## 💡 Setup

- Create `g25a`, `g25b`, `g25c`, and `g25d`.
- Place `g25d` in several locations for finder tests.
- Use locked/favorite/lore state on one block when testing identity migration.
- Screen layout for ReID belongs to G27; G25 owns the identity operation behavior behind it.

## A - ReID command core - Done ✅

| | |
| --- | --- |
| **Check** | `/cb reid <old-id> <new-id>` changes id only, migrates state, and stays undoable. |
| **Pass rule** | Locked guard, rename, slot, texture, favorite, lock, lore, undo, redo, and conflict rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Lock `g25b`, then run `/cb reid g25b x`. | Locked block refuses reid until unlocked. | ✅ | ✅ |
| A2 | Unlock and run `/cb reid g25b g25b_new`. | Id changes while display name, slot, texture, and attributes remain with the block. | ✅ | ✅ |
| A3 | Check favorites, locks, and lore/notes after reid. | State follows the new id. | ✅ | ✅ |
| A4 | Run `/cb undo`, then `/cb redo`. | Id change reverses and reapplies cleanly. | ✅ | ✅ |
| A5 | Try reid to an existing id. | Conflict is rejected without damaging either block. | ✅ | ✅ |

## B - ReID case-mismatch fix - Built 🎯

| | |
| --- | --- |
| **Check** | ReID resolves the stored id once so typed-case mismatches do not fail the command. |
| **Pass rule** | Case mismatch, pure case change, lock check, messages, undo, and conflict rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Fix was built after diagnosis and still needs owner confirmation. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Store a block as `Sfa1`, then run `/cb reid sfa1 sfa2`. | Command succeeds by using the resolved stored id. | 🎯 | 🎯 |
| B2 | Try a pure case change if allowed by id rules. | Behavior is clear and does not claim the old id is missing. | 🎯 | 🎯 |
| B3 | Run the same test while the block is locked. | Lock refusal checks the resolved stored block. | 🎯 | 🎯 |
| B4 | Run `/cb undo` and `/cb redo`. | Case-mismatch reid participates in normal undo/redo. | 🎯 | 🎯 |
| B5 | Try reid into an existing id with different case. | Conflict behavior remains safe and human-readable. | 🎯 | 🎯 |

## C - ReID Screen and editor entry - Designed ⏳

| | |
| --- | --- |
| **Check** | ReID should be available from a proper Screen flow with real text input and editor integration. |
| **Pass rule** | No-arg picker, single-id open, editor button, invalid id, locked id, cancel, undo, and G27 layout rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | ReIdMenu was reclassified as a Screen and is not built in the current design. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb reid`. | ReID Screen opens to pick a block, then enter a new id. | ⏳ | ⏳ |
| C2 | Run `/cb reid g25b`. | Screen opens directly for `g25b`. | ⏳ | ⏳ |
| C3 | Click Change ID from `/cb editor g25b`. | Same Screen opens and returns to editor on cancel. | ⏳ | ⏳ |
| C4 | Enter an invalid, taken, or locked id. | Button disables or refuses with a clear message before damage. | ⏳ | ⏳ |
| C5 | Complete a Screen reid and undo it. | It runs the same core command behavior as A. | ⏳ | ⏳ |

## D - Universal block targets `#` and `!` - Designed ⏳

| | |
| --- | --- |
| **Check** | Existing-block commands should accept `#` for looked-at block and `!` for held block through one safe typed resolver. |
| **Pass rule** | Parser, tab-complete, range, held priority, fluids/entities, item frames, spectator/execute, every command family, Arabic safe path, and create exclusion rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Depends on the command tree/refactor work noted as G04-2 before mass adoption. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Type `/cb retexture # <url>` while looking at a slot block. | `#` resolves to that block id and command behaves like the id was typed. | ⏳ | ⏳ |
| D2 | Hold a custom block in main hand and run a command with `!`. | `!` resolves to main hand block; if main hand is empty it falls back to offhand. | ⏳ | ⏳ |
| D3 | Aim through water, lava, or an entity at a custom block. | `#` pierces to the block behind and uses 10-block reach. | ⏳ | ⏳ |
| D4 | Look at an item frame containing a custom block. | `#` resolves the item inside the frame. | ⏳ | ⏳ |
| D5 | Use `/execute as` or spectator mode with `#` where possible. | Resolver behaves consistently or gives a clear no-player/no-target message. | ⏳ | ⏳ |
| D6 | Run supported commands across rename, retexture, give, lock, unlock, shape, face, note, color, and cloud flows. | Every existing-block command uses the same resolver and errors. | ⏳ | ⏳ |
| D7 | Try `/cb create # Name` or `/cb create ! Name`. | Create remains excluded because it makes a new id. | ⏳ | ⏳ |
| D8 | Look at an Arabic auto-join letter and run delete. | Arabic block deletes through the safe world-block path. | ⏳ | ⏳ |
| D9 | Look at an Arabic auto-join letter and run color/recolor. | Arabic recolor uses the proven Square recolor path. | ⏳ | ⏳ |
| D10 | Look at an Arabic letter and run unsupported slot-data commands. | Command refuses politely and never reaches `SlotData` mutation code. | ⏳ | ⏳ |

## E - Swap, duplicate, and identity extras - Planned 📜

| | |
| --- | --- |
| **Check** | Identity operations beyond ReID must be atomic, undoable, and tab-complete correctly. |
| **Pass rule** | `swapid`, `swapname`, duplicate alias, collisions, undo, redo, and state migration rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Run `/cb swapid g25b g25c`. | Ids swap while block data stays with each original block. | 📜 | 📜 |
| E2 | Undo the swap. | One undo restores both ids. | 📜 | 📜 |
| E3 | Run `/cb swapname g25b g25c`. | Display names swap while ids and attributes remain. | 📜 | 📜 |
| E4 | Run `/cb duplicate g25b` and `/cb duplicate g25c custom_copy`. | Alias matches `/cb dupe` behavior with auto and custom ids. | 📜 | 📜 |

## F - Custom drops - Planned 📜

| | |
| --- | --- |
| **Check** | Custom block drops can be configured, cleared, persisted, and edited from the block editor. |
| **Pass rule** | Set, break, amount, invalid item, clear, undo, restart, editor slot, and default-no-drop rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb setdrop g25a diamond 2`. | Drop config is saved and message is clear. | 📜 | 📜 |
| F2 | Place and break `g25a`. | Two diamonds drop. | 📜 | 📜 |
| F3 | Restart and break another `g25a`. | Drop persists. | 📜 | 📜 |
| F4 | Run `/cb cleardrop g25a`. | Block returns to no custom drop. | 📜 | 📜 |
| F5 | Open editor and use Custom Drop slot. | Same drop config can be edited from UI. | 📜 | 📜 |

## G - Block Finder, editor PNG export, and tab icon - Planned 📜

| | |
| --- | --- |
| **Check** | Extra management surfaces should locate placed blocks, export textures from editor, and set creative tab icon. |
| **Pass rule** | Find, teleport, pagination, refresh, permissions, export PNG, download link, tab icon, restart, and invalid URL rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Place `g25d` several times, then run `/cb find g25d`. | Finder lists placements with coordinates, dimension, and distance. | 📜 | 📜 |
| G2 | Click one finder result. | Admin teleports to the placement. | 📜 | 📜 |
| G3 | Use refresh and pagination. | Results update without losing count. | 📜 | 📜 |
| G4 | Click Export PNG in block editor. | Texture exports to `cloud_exports` and posts a sender-only download link. | 📜 | 📜 |
| G5 | Run `/cb settabicon <url>`. | Custom Blocks creative tab icon updates and persists after restart. | 📜 | 📜 |

## H - Moved management extras ownership - Planned 📜

| | |
| --- | --- |
| **Check** | Features moved from other groups must not be considered tested just because ownership moved. |
| **Pass rule** | Favorites, recent, block edit history, magic items, and editor revamp rows get full specs before implementation. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs full owner walkthrough before writing detailed test rows. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Define `/cb favorite`, `/cb fav`, `/cb unfavorite`, and `/cb favs` behavior. | Primary command, alias behavior, and non-toggle unfavorite are clear. | 📜 | 📜 |
| H2 | Define `/cb recent`. | Recently-used list behavior and UI owner are clear. | 📜 | 📜 |
| H3 | Define `/cb history`. | Mutation-log feature ownership and display route are clear. | 📜 | 📜 |
| H4 | Define `/cb magicitems` and `/cb editmagicitems`. | Full revamp scope is documented before build. | 📜 | 📜 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A ReID command core — ✅ `2026-06-13`

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

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g25a`, `g25b`, `g25b_new`, `g25c`, and `g25d`.
- [ ] Remove placed finder fixtures.
- [ ] Clear any custom drops and tab-icon test files.
- [ ] Keep ReID Screen layout issues in G27.

</details>
