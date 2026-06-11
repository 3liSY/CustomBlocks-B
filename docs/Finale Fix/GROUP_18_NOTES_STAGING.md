# Group 18 — Block Metadata: Notes & Staging Area

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 9 (Import/Export + Notes + Drafts) build-verified.
>
> **Objective:** Rework the note system into a full Book GUI with three tabs. Rename the draft/publish system to the "Staging Area", fix pack-rebuild exclusion behavior, and add clear UX messaging. Both systems are per-block metadata — test them together.
>
> **Source issues:** 17.12 (note UX), P5 (Note rework into Book GUI), 17.14 (draft/publish confusing), P6 (rename to Staging Area, clearer UX)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / changes

| Area | Old behavior | New behavior |
|---|---|---|
| Note storage | `/cb note <id> [text\|clear]` — plain text only | Multi-tab Book GUI: Lore, To-Do, Hover Tooltip |
| Note GUI | None | Chest GUI — `/cb note <id>` |
| Note sharing | None | "Share" button in Note GUI |
| Draft terminology | "draft" / "publish" — confusing | "Staging Area" / "stage" / "release" — old names remain as aliases |
| Staging commands | `/cb draft <id>`, `/cb publish <id>`, `/cb drafts` | `/cb stage <id>`, `/cb release <id>`, `/cb staging` |
| Pack behavior | Staged blocks still triggered pack rebuilds | Staged blocks excluded from pack until released |
| Staging GUI | None | Staging Area chest GUI with explanation banner |
| Status tag | `[draft]` in `/cb list` | `[staging]` (yellow) |
| Resume | `/cb resume <id>` | Preserved — reopens editor for staged block |

---

## What this group covers

| Feature | Commands |
|---|---|
| Note Book GUI | `/cb note <id>` |
| Note lore | Lore tab — multi-line description |
| Note to-do | To-Do tab — checkable items |
| Note tooltip | Hover Tooltip tab — item hover text |
| Legacy note commands | `/cb note <id> <text>` / `/cb note <id> clear` (still work) |
| Stage block | `/cb stage <id>` (alias: `/cb draft <id>`) |
| Release block | `/cb release <id>` (alias: `/cb publish <id>`) |
| View staging | `/cb staging` (alias: `/cb drafts`) |
| Resume | `/cb resume <id>` |
| Note storage | `config/customblocks/data/notes.json` |

---

## Implementation Requirements

### 1. Note Book GUI

`/cb note <id>` opens a chest GUI styled as an open book. Three tabs in the top row:

| Tab | Slot icon | Content |
|---|---|---|
| Lore | Written Book | Freeform description, supports `&` color codes, max 500 chars |
| To-Do | Paper | Checkable task list, up to 20 items |
| Tooltip | Name Tag | Short hover text shown on block item in inventory (max 100 chars) |

Each tab uses anvil GUI inputs for editing. "Save" slot commits changes. "Share" button generates a paste link via cloud vault endpoint.

### 2. Note Legacy Compatibility

`/cb note <id> <text>` — still works. Sets the Lore tab. Shows tip: `"Tip: Use /cb note <id> for the full Notes editor."`

`/cb note <id> clear` — still works. Clears all three tabs.

### 3. Staging Area — Terminology

All old `draft`/`publish`/`drafts` commands remain as aliases. New primary names:

| Old | New (primary) | Alias preserved |
|---|---|---|
| `/cb draft <id>` | `/cb stage <id>` | `/cb draft <id>` ✓ |
| `/cb publish <id>` | `/cb release <id>` | `/cb publish <id>` ✓ |
| `/cb drafts` | `/cb staging` | `/cb drafts` ✓ |

### 4. Pack Behavior Fix

A staged block's texture is **excluded** from pack rebuilds. No rebuild fires when a staged block's texture changes. When `/cb release <id>` is called: block enters the pack and one rebuild fires.

This prevents pack thrash during heavy editing sessions.

### 5. Staging Area Chest GUI

`/cb staging` opens a chest GUI with:
- **Banner slot** (top center): Gold banner. Hover text: "Staged blocks are excluded from resource pack rebuilds. Use this area when making many changes at once to prevent lag."
- **Block slots**: one per staged block. Click → sub-menu: Edit, Release, Delete.
- **"Release All" slot** — releases all staged blocks, one pack rebuild.
- **"Resume" slot** — if a staged block has an in-progress session.

### 6. Messaging

| Action | Message |
|---|---|
| Stage | `"g18a" moved to Staging Area. Changes won't affect the resource pack until you release it.` |
| Release | `"g18a" released. Resource pack updating now.` |
| Release all | `Released N block(s). Resource pack updating once.` |

---

## Setup

```
/cb create g18a NoteTest
/cb create g18b StagingTest1
/cb create g18c StagingTest2
```

---

## Test G18.1 — Note Book GUI opens

```
/cb note g18a
```

**Expected:** Chest GUI opens styled as a book. Three tabs: Lore, To-Do, Hover Tooltip. Save and Share slots visible.

**Pass:** Chest GUI opens with 3 tabs.
**Fail:** Text output only, or old single-text behavior.

---

## Test G18.2 — Lore tab save and read

In Note GUI → Lore tab → Edit → type: `This block is the entrance arch.` → confirm → Save.

`/cb note g18a` — re-open.

**Expected:** Lore tab shows saved text.

**Pass:** Text saved and displayed.
**Fail:** Text lost.

---

## Test G18.3 — To-Do tab

In Note GUI → To-Do tab → Add item → type `Add texture` → Add another → `Set glow level`.

**Expected:** Two unchecked items visible.

Click "Add texture" slot.

**Expected:** Item shows as checked/completed.

**Pass:** Items added and checkable.
**Fail:** Items not added, or click does nothing.

---

## Test G18.4 — Hover Tooltip tab

In Note GUI → Hover Tooltip tab → Edit → type: `Main entrance block.` → Enable → Save.

`/cb give g18a` — hover the item in inventory.

**Expected:** Extra tooltip line: "Main entrance block."

**Pass:** Tooltip visible on item.
**Fail:** No extra tooltip line.

---

## Test G18.5 — Notes persist across restart

Restart server. `/cb note g18a`.

**Expected:** All three tabs retain their data.

**Pass:** Data persisted.
**Fail:** Any tab empty after restart.

---

## Test G18.6 — Legacy note command

```
/cb note g18a This is a legacy note.
```

**Expected:** Lore tab updated. Tip message shown suggesting the GUI.

**Pass:** Lore updated, tip shown.
**Fail:** Command rejected or no update.

---

## Test G18.7 — Clear note

```
/cb note g18a clear
```

`/cb note g18a` → all tabs empty, tooltip disabled.

**Pass:** All data cleared.
**Fail:** Any tab retains data.

---

## Test G18.8 — Stage command

```
/cb stage g18b
```

**Expected:** `"g18b" moved to Staging Area. Changes won't affect the resource pack until you release it.`

**Pass:** Staged, message is clear and conversational.
**Fail:** Error or old "marked as draft" message.

---

## Test G18.9 — Old `draft` alias works

```
/cb draft g18c
```

**Expected:** Same staging behavior. No error.

**Pass:** Alias works.
**Fail:** Command not found.

---

## Test G18.10 — Staging GUI opens with explanation

```
/cb staging
```

**Expected:** Chest GUI with gold banner slot, one slot for g18b, one for g18c, "Release All" slot.

Hover the banner slot → shows the "prevents pack lag" explanation.

**Pass:** GUI opens, explanation on hover.
**Fail:** Text output only, or no explanation.

---

## Test G18.11 — Staged block excluded from pack rebuild

While g18b is staged:

```
/cb retexture g18b https://i.imgur.com/example.png
```

Check `latest.log` — should NOT see `[CustomBlocks] Rebuilding resource pack…`.

**Pass:** No pack rebuild during staging.
**Fail:** Pack rebuilds despite block being staged.

---

## Test G18.12 — Release All triggers one rebuild

In the staging GUI, click "Release All".

**Expected:** `Released 2 block(s). Resource pack updating once.`

`latest.log` — exactly ONE rebuild line.

**Pass:** One rebuild, both blocks released.
**Fail:** Two rebuilds, or no rebuild.

---

## Test G18.13 — `[staging]` tag in list

```
/cb stage g18b
/cb list
```

**Expected:** g18b shows `[staging]` tag (yellow).

**Pass:** `[staging]` tag visible.
**Fail:** `[draft]` (old name) or no tag.

---

## Test G18.14 — Old `publish` alias works

```
/cb publish g18b
```

**Expected:** `"g18b" released.`

**Pass:** Alias works.
**Fail:** Command not found.

---

## Group 18 Verdict

| Test | Description | Result |
|---|---|---|
| G18.1 | Note Book GUI opens with 3 tabs | ⬜ |
| G18.2 | Lore tab saves and reads correctly | ⬜ |
| G18.3 | To-Do items add and check | ⬜ |
| G18.4 | Hover tooltip visible on item | ⬜ |
| G18.5 | All note data persists after restart | ⬜ |
| G18.6 | Legacy note command still works | ⬜ |
| G18.7 | Clear removes all note data | ⬜ |
| G18.8 | `/cb stage` with clear message | ⬜ |
| G18.9 | Old `draft` alias preserved | ⬜ |
| G18.10 | Staging GUI with explanation banner | ⬜ |
| G18.11 | Staged block excluded from pack rebuild | ⬜ |
| G18.12 | Release All triggers exactly one rebuild | ⬜ |
| G18.13 | `[staging]` tag in `/cb list` | ⬜ |
| G18.14 | Old `publish` alias preserved | ⬜ |

**Group 18 passes when notes and staging both work correctly in-game.**

If anything shows ❌ — paste:
1. The exact command or GUI action
2. What appeared vs what was expected
3. Whether a pack rebuild appeared in `latest.log` when not expected

---

## Cleanup

```
/cb release g18b
/cb release g18c
/cb delete g18a
/cb delete g18b
/cb delete g18c
```
