# Group 18 - Block Lore Notes

## Status

| | |
| --- | --- |
| **Verdict** | Lore data and commands are partly built, but the Screen version and fresh owner confirmation are still missing. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Lore data model and migration | Built 🎯 | Discussion ✏️ |
| B | Item hover lore sync | Built 🎯 | Discussion ✏️ |
| C | `/cb lore` and `/cb note` alias commands | Built 🎯 | Discussion ✏️ |
| D | Lore share and import | Designed ⏳ | Discussion ✏️ |
| E | Lore Screen handoff to G27 | Designed ⏳ | Discussion ✏️ |
| F | Staging and draft system | Planned 📜 | Scrapped 👎 |
| G | First-pass 3-tab notes GUI | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_18_NOTES_STAGING.md](../groups/GROUP_18_NOTES_STAGING.md)

---

# Active Tests

## 💡 Setup

- Create a disposable block: `/cb create g18a LoreTest`.
- Use `/cb give g18a` when checking hover lore.
- Keep one old flat note or old notes backup if migration needs to be tested.
- Screen layout itself is G27; this guide tests G18 data, commands, migration, and item hover behavior.

## A - Lore data model and migration - Built 🎯

| | |
| --- | --- |
| **Check** | Notes collapse into one Lore concept: saved lines plus an enabled flag, with old data migrated safely. |
| **Pass rule** | New save shape, old flat note, old lore/tooltip, old To-Do removal, restart, and malformed-data rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs fresh owner confirmation after the old stale notes rows were retired. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Add multiple lore lines to `g18a`, then inspect stored data after save. | `notes.json` stores `lines` and `enabled`, not old separate lore/todo/tooltip fields. | 🎯 | 🎯 |
| A2 | Load a legacy flat `{id:"text"}` note. | It migrates into one lore line and remains enabled. | 🎯 | 🎯 |
| A3 | Load old data with tooltip plus multi-line lore. | Tooltip and lore merge into the new line list without duplicating nonsense. | 🎯 | 🎯 |
| A4 | Load old To-Do data. | To-Do items are dropped intentionally and do not reappear as lore lines. | 🎯 | 🎯 |
| A5 | Restart the server after edits. | Lines and enabled state persist exactly. | 🎯 | 🎯 |
| A6 | Load malformed notes data. | The mod keeps running and reports a human-readable diagnostics issue. | 🎯 | 🎯 |

## B - Item hover lore sync - Built 🎯

| | |
| --- | --- |
| **Check** | Lore lines appear on the custom block item hover using Minecraft-style item lore. |
| **Pass rule** | Single line, multi-line, color codes, off toggle, other-player sync, restart, and clear rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Requires client sync evidence from the current jar. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Add one lore line, then run `/cb give g18a` and hover the item. | The hover shows the lore line under the item name. | 🎯 | 🎯 |
| B2 | Add three lines and hover again. | All lines appear in order without merging into one unreadable line. | 🎯 | 🎯 |
| B3 | Add lore containing `&a`, `&l`, and `&c`. | Color/format codes render in the menu and hover, with no raw `&` clutter unless escaped by design. | 🎯 | 🎯 |
| B4 | Toggle lore off. | Hover lore hides, but the saved lines remain available when toggled on again. | 🎯 | 🎯 |
| B5 | Have a second player receive or view the item. | The second client sees the same lore lines after sync. | 🎯 | 🎯 |
| B6 | Restart and hover the item again. | Lore still appears correctly after reload. | 🎯 | 🎯 |
| B7 | Clear lore. | Hover lines disappear and saved line list becomes empty. | 🎯 | 🎯 |

## C - `/cb lore` and `/cb note` alias commands - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb lore` is the primary command, while `/cb note` stays as a compatibility alias. |
| **Pass rule** | Open, quick-add, clear, alias, OP gate, bad id, and editor button rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Open/editor-button rows depend on the final G27 Screen surface. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb lore g18a`. | The lore editor opens or routes to the G27 Lore Screen when that is built. | 🎯 | 🎯 |
| C2 | Run `/cb lore g18a Main entrance block`. | One line is added and appears in hover lore. | 🎯 | 🎯 |
| C3 | Run `/cb lore g18a clear`. | All lore lines clear and lore is disabled. | 🎯 | 🎯 |
| C4 | Run `/cb note g18a Alias line`. | Alias still works and writes to the same Lore data. | 🎯 | 🎯 |
| C5 | Try lore commands as a non-OP account. | Editing is denied cleanly if OP-only is still the locked permission model. | 🎯 | 🎯 |
| C6 | Use a missing id. | Error names the missing block and does not create orphan note data. | 🎯 | 🎯 |
| C7 | Click the Lore button from the block editor. | It opens the same Lore surface as `/cb lore g18a`. | 🎯 | 🎯 |

## D - Lore share and import - Designed ⏳

| | |
| --- | --- |
| **Check** | Share/import keeps the new Lore shape and protects existing notes before overwrite. |
| **Pass rule** | Vault disabled, share, import, overwrite confirm, old-code migration, and bad-code rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Vault upload endpoint must be configured and verified. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Press Share with no vault endpoint configured. | A clear disabled-state message appears and no data is lost. | ⏳ | ⏳ |
| D2 | Share a lore note with multiple lines. | A share code/link is produced and references the vault, not the Minecraft server host. | ⏳ | ⏳ |
| D3 | Import the code into a blank block. | Lines, enabled state, and color codes match the shared note. | ⏳ | ⏳ |
| D4 | Import into a block that already has lore. | A confirm-before-overwrite prompt appears. | ⏳ | ⏳ |
| D5 | Import an old note code. | Old shape migrates through the same parser into new Lore lines. | ⏳ | ⏳ |
| D6 | Import a bad code. | Failure is human-readable and leaves existing lore unchanged. | ⏳ | ⏳ |

## E - Lore Screen handoff to G27 - Designed ⏳

| | |
| --- | --- |
| **Check** | The user-facing editor must be a real Screen, not the retired 3-tab notes GUI. |
| **Pass rule** | G27 Screen open, line editing, real text field, reorder if supported, toggle, share, and navigation rows pass in TG27. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | G27 owns the screen build; G18 owns the saved Lore behavior behind it. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Open Lore from command or editor button after G27 build. | A real Screen opens, not the old book/chest/tab design. | ⏳ | ⏳ |
| E2 | Add and edit lore text from the Screen. | Text entry feels like a proper editor, not an anvil-per-line workaround unless explicitly chosen later. | ⏳ | ⏳ |
| E3 | Delete one line. | Only that line is removed and undo/result notes can identify the action. | ⏳ | ⏳ |
| E4 | Toggle lore on/off. | Hover output changes immediately while saved lines remain. | ⏳ | ⏳ |
| E5 | Use Back/Done/Cancel navigation. | Unsaved or auto-saved behavior is clear and consistent with G27 rules. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

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

- §F Staging and draft system — 👎 `2026-06-21`: Removed from G18 scope.
- §G First-pass 3-tab notes GUI — 👎 `2026-06-21`: Superseded by Lore revamp and G27 Screen direction.
- To-Do notes feature — 👎 `2026-06-21`: Cut from the Lore model.
- Writable book Lore editor — 👎 `2026-07-09`: Superseded by the Screen direction.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g18a` after testing.
- [ ] Remove temporary legacy notes fixtures after migration evidence is saved.
- [ ] Keep Lore Screen issues in G27.
- [ ] Do not restore staging, draft, publish, or To-Do tests.

</details>
