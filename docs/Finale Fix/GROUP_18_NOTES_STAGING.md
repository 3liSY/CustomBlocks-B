# Group 18 — Block Metadata: Notes

> ⛔ **STAGING SYSTEM SCRAPPED (sweep 2026-06-21).** Per SWEEP_INDEX §A the entire draft/publish/staging
> system (`draft`/`drafts`/`publish`/`stage`/`release`/`staging`/`resume`) is **removed**. G18 is **notes only**
> now. The staging spec (§3–§6) and tests G18.8–G18.14 are struck below for history; do not build them.
>
> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 9 (Notes) build-verified.
>
> **Objective:** Rework the note system into a full Book GUI with three tabs (Lore, To-Do, Hover Tooltip).
>
> **Source issues:** 17.12 (note UX), P5 (Note rework into Book GUI)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / changes

| Area | Old behavior | New behavior |
|---|---|---|
| Note storage | `/cb note <id> [text\|clear]` — plain text only | Multi-tab Book GUI: Lore, To-Do, Hover Tooltip |
| Note GUI | None | Chest GUI — `/cb note <id>` |
| Note sharing | None | "Share" button in Note GUI |
| ~~Draft/staging system~~ | ~~draft/publish/stage/release/staging/resume~~ | ⛔ **SCRAPPED — removed entirely (SWEEP_INDEX §A)** |

---

## What this group covers

| Feature | Commands |
|---|---|
| Note Book GUI | `/cb note <id>` |
| Note lore | Lore tab — multi-line description |
| Note to-do | To-Do tab — checkable items |
| Note tooltip | Hover Tooltip tab — item hover text |
| Legacy note commands | `/cb note <id> <text>` / `/cb note <id> clear` (still work) |
| Note storage | `config/customblocks/data/notes.json` |
| ~~Staging commands~~ | ⛔ **SCRAPPED** — `stage`/`release`/`staging`/`draft`/`publish`/`drafts`/`resume` removed |

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

### 3–6. ~~Staging Area~~ — ⛔ SCRAPPED

The entire staging system (terminology, pack-behavior exclusion, staging chest GUI, messaging) is
**removed** per SWEEP_INDEX §A. Macros + the existing pack debounce cover the "avoid pack thrash" need.
No `stage`/`release`/`staging`/`draft`/`publish`/`drafts`/`resume` commands. Original spec deleted.

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

## Tests G18.8–G18.14 — ⛔ SCRAPPED (staging removed)

All staging tests (`stage`/`draft`/`staging`/`release`/`publish`, pack-exclusion, `[staging]` tag) are
**removed** per SWEEP_INDEX §A. Skip. G18 scope = notes (G18.1–G18.7) only.

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
| ~~G18.8–G18.14~~ | ~~staging tests~~ | ⛔ SCRAPPED — staging system removed (§A) |

**Group 18 passes when the notes Book GUI works in-game (G18.1–G18.7).**

If anything shows ❌ — paste:
1. The exact command or GUI action
2. What appeared vs what was expected
3. Whether a pack rebuild appeared in `latest.log` when not expected

---

## Cleanup

```
/cb delete g18a
/cb delete g18b
/cb delete g18c
```
