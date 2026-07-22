# Group 18 — Block Metadata: Notes

> 🔁 **REVAMP v2 — Note → "Lore" (owner interview 2026-06-21, 2nd pass).** The 3-tab Book GUI below
> (Lore / To-Do / Hover Tooltip) was built (build-green, never in-game confirmed) but the owner found it
> confusing. It is **superseded** by the single-screen **Lore** feature in the next section. Everything from
> "🔒 Locked Decisions" downward is kept for history — **build the REVAMP v2 section, not it.**
>
> ⛔ **STAGING SYSTEM SCRAPPED (sweep 2026-06-21).** Per SWEEP_INDEX §A the entire draft/publish/staging
> system (`draft`/`drafts`/`publish`/`stage`/`release`/`staging`/`resume`) is **removed**. G18 is **notes only**
> now. The staging spec (§3–§6) and tests G18.8–G18.14 are struck below for history; do not build them.
>
> **UI medium audit (2026-07-09):** NotesMenu (Lore GUI) → **Screen, not built.** Real multi-line text
> field(s) instead of an anvil-per-line edit. See `docs/UI_MEDIUM_GUIDE.md`.
>
> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 9 (Notes) build-verified.
>
> **Objective:** Rework the note system into a full Book GUI with three tabs (Lore, To-Do, Hover Tooltip).
>
> **Source issues:** 17.12 (note UX), P5 (Note rework into Book GUI)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## 🔁 REVAMP v2 — Note → "Lore" (owner interview 2026-06-21, 2nd pass)

> **Status: 🟢 BUILT — build-green, awaiting in-game confirm (2026-06-21).** All 5 revamp slices
> (R-S1…R-S5) are built and the jar compiles with all gates passing. The first-pass 3-tab Book GUI has
> been removed. Nothing here is ✅ done until the owner confirms it in-game (CLAUDE.md §2).

### Why revamp

The first-pass note crammed **three** separate things into one menu, and the owner found it confusing:

| First-pass tab | What it was | Where it showed | Problem |
|---|---|---|---|
| Lore (book) | long text | **only inside the menu** | the player never saw it on the block |
| Hover Tooltip | short line | on the item when hovered | the *real* hover text — but a 2nd, separate box |
| To-Do | checklist | menu only | rarely used |

The owner wrote in **Lore** expecting it on hover, but hover read **Tooltip** — two text boxes, only one
showed on the item. The book-and-quill was also clunky. And "lore" is **Minecraft's own word** for the gray
italic lines under an item's name on hover — exactly the thing wanted. So the feature is **renamed and
collapsed to one concept: Lore = the hover lines.**

### Locked decisions (revamp — override the first-pass D1–D10)

| # | Decision |
|---|---|
| R1 | **Rename Note → Lore.** "Lore" = the vanilla term for item hover text; matches what it does. |
| R2 | **`/cb lore <id>` is primary**; **`/cb note <id>` is kept as an alias** (old habit still works). |
| R3 | **Lore = a list of lines** shown on the block item on hover (gray italic, vanilla style). Merges the old Lore + Tooltip into ONE thing. |
| R4 | **Anvil for all typing** (each line ≤ 50 chars, the anvil cap). **The writable book is removed entirely** (reverses first-pass D1). |
| R5 | **Per-line edit:** click a line = edit it in an anvil; right-click = delete it. "+ Add line" opens a fresh anvil. |
| R6 | **One On/Off toggle** for the whole lore. Off hides the item lines even though the text stays saved (first-pass D4 semantics, applied to the whole note). |
| R7 | **To-Do is CUT** (reverses first-pass D2). Removed entirely — menu, data, and code. |
| R8 | **Tabs removed.** Only lore remains, so the menu is a **single screen** (no tab row). |
| R9 | **Share / import KEPT** (first-pass D6/D7 unchanged): the Share button uploads to the cloud vault → posts a share code in chat; `/cb note import <id> <code>` imports with confirm-before-overwrite. Dormant until `vaultEndpoint` is set. |
| R10 | **`&` colour codes kept** (first-pass D3) in lore lines — rendered in the menu and on the item. |
| R11 | **Quick-add + clear kept:** `/cb lore <id> <text>` adds one line; `/cb lore <id> clear` wipes all lines and sets it Off. |
| R12 | **OP only** (first-pass D8 unchanged). |
| R13 | **Entry points:** `/cb lore <id>` + a **Lore** button in the block Editor menu (first-pass D9, renamed). |

### Data model change

- `NoteData` becomes **`List<String> lines` + `boolean enabled`**. The old `lore` / `todo` / `tooltip` /
  `tooltipEnabled` fields are removed.
- **Migration (auto, on first load):** old `tooltip` line + old `lore` (split on newlines) become the new
  `lines`; `enabled` = old `tooltipEnabled` **OR** (the block had any lore). **Old To-Do items are dropped** —
  the only data loss, because To-Do is being removed. Legacy flat `{id:"text"}` notes still fold into one line.
- `notes.json` shape changes to `{ "lines": [...], "enabled": true }` per id. Share JSON uses the same shape;
  old shared codes auto-migrate through the same parse path.

### Menu — single screen (no tabs) — moved to G27

Full screen spec moved to `GROUP_27_SCREENS.md` §G27.33 (2026-07-12). G18 keeps the data model, migration,
and item-hover sync below.

### Slice plan (revamp — build order; careful 1-by-1, owner tests at the end)

| Slice | Scope |
|---|---|
| **R-S1 — Data** | `NoteData` = lines + enabled · migrate old lore/tooltip/todo (To-Do dropped) · `BlockNotesManager` reads/writes/setters · share JSON keeps working |
| **R-S2 — Item hover** | block item shows ALL lore lines (multi-line), synced to other players (`HudSync` / `ClientSlotCache` / `SlotBlock.appendTooltip`) |
| **R-S3 — Menu** | single-screen Lore: line list (add / edit / delete via anvil) + On/Off + Share + Done · **delete the book (`LoreBook`) and all To-Do code** |
| **R-S4 — Commands** | `/cb lore` primary + `/cb note` alias · quick-add one line · `clear` · import kept |
| **R-S5 — Docs** | this section + the testing guide + CHANGELOG + PROGRESS_LOG |

> Each slice ends 🟢 build-green only. ✅ done needs the owner's in-game confirm (CLAUDE.md §2).

### What's cut vs kept

- **Cut:** To-Do (tab, data, code) · the writable book (`LoreBook`) · the tab row · the separate "Lore vs Tooltip" split.
- **Kept:** Share / import (vault) · the On/Off toggle · `&` colours · `/cb lore <id> <text>` quick-add · `clear` · the OP gate · the Editor-menu button · auto-migration of old data.

### Files this will touch (planned)

- **Rewrite:** `core/NoteData.java`, `gui/chest/NotesMenu.java` (→ single screen), `command/handlers/NoteCommands.java`.
- **Edit:** `core/BlockNotesManager.java` (model + migration + `activeLore`), `block/SlotBlock.java`,
  `network/HudSync.java`, `client/ClientSlotCache.java`, `client/CustomBlocksClient.java`,
  `command/CommandRegistrar.java` / `ManagementCommands.java` (`/cb lore` + `/cb note` alias),
  `gui/chest/EditorMenu.java` (button label Note → Lore).
- **Delete:** `gui/chest/LoreBook.java` (book flow gone).

---

## 🔒 Locked Decisions (owner interview 2026-06-21) — ⛔ SUPERSEDED by REVAMP v2 above

> Surveyed the live code first, then a 3-round owner interview. **Nothing built this pass — design only.**
> These decisions override anything below them. Reality corrections are read from code, not guessed.

**Reality corrections (read, not guessed):**
- **Storage is flat today.** `BlockNotesManager` is `Map<String,String>` (id→one text) → `notes.json` flat
  (`core/BlockNotesManager.java`). The 3-field model (Lore / To-Do list / Tooltip+enabled) is **new**; old
  flat notes **auto-migrate into Lore** on first load (safe, non-destructive).
- **Anvil input caps ~50 chars** (`gui/chest/AnvilPrompt.java`, `MAX_LENGTH=50`; vanilla rename box limit).
  500-char Lore is **not possible** via anvil → Lore uses a **writable book** instead.
- **No item tooltip injection exists.** `SlotBlock.SlotItem` only overrides `getName` (no `appendTooltip`).
  Item-hover (G18.4) needs the tooltip text **synced to the client** — names already sync via
  `SlotBlock.CLIENT_NAME_RESOLVER` → ClientSlotCache; the tooltip needs the **same wiring** = heaviest piece.
- **CloudVaultClient.upload() is a stub** (returns null, Phase-14 TODO). Share must **implement** upload
  against the assumed worker contract (mirror `uploadCategory`). Vault endpoint ≠ server host, so no §A leak.
- **Notes are ungated today** (registered on root, no `.requires`). Decision below adds an OP gate.
- **Cleanup flag:** `draft`/`publish`/`drafts` still registered (`ManagementCommands.java:76-88`) — part of
  the **scrapped staging system (§A)**. Remove during this group (dead code).

**Locked decisions:**

| # | Decision |
|---|---|
| D1 | **Lore input = writable book.** Lore tab opens a real book-and-quill (multi-page, handles 500+ chars, fits the "Book GUI" theme). |
| D2 | **To-Do = toggle + right-click delete.** Left-click flips an item done/undone (both ways); right-click removes it. Up to 20 items, added via anvil. |
| D3 | **Color codes honored.** `&a`/`&l`/`&c` etc. render as real color/format in the GUI **and** on the item tooltip (`&`→`§` convert). |
| D4 | **Tooltip = text + explicit on/off toggle.** Off hides the item line even if text is saved; `clear` sets it off. |
| D5 | **Item-hover tooltip is in scope** as its **own slice** (S3) — client sync + `SlotItem.appendTooltip`. |
| D6 | **Share = vault upload + import (full round-trip).** Implement `upload()`; Share posts a share code/link in chat. Import = `/cb note import <id> <code>`. |
| D7 | **Import overwrite = confirm first.** If the target block already has a note, ask before replacing (reuse the `BulkConfirm` hold pattern). |
| D8 | **Permissions = OP only.** Opening/editing/sharing the Notes GUI requires operator. |
| D9 | **Entry points = `/cb note <id>` + a Notes button in the block Editor menu** (`EditorMenu`). |
| D10 | **Pace = one slice at a time**, in-game confirm between each (CLAUDE.md §4). |

---

## 🧱 Slice Plan (build order — confirm each in-game before the next) — ⛔ SUPERSEDED by REVAMP v2

| Slice | Scope | Covers |
|---|---|---|
| **S1 — Lore core** | New structured note model + flat-note **migration** + `notes.json` rework (atomic) · 3-tab chest **shell** (Lore / To-Do / Hover Tooltip + Save) · **Lore tab via writable book** · color-code render · legacy `/cb note <id> <text>` → Lore + tip · `clear` wipes all 3 · OP gate · Notes button in Editor menu · remove dead `draft`/`publish`/`drafts` | G18.1, G18.2, G18.6, G18.7, G18.5(partial) |
| **S2 — To-Do tab** | Add item (anvil) · left-click toggle done/undone · right-click delete · up to 20 · persist | G18.3 |
| **S3 — Tooltip tab + sync** *(heaviest)* | Tooltip text + Enable toggle · **client sync** of tooltip text · `SlotItem.appendTooltip` → line shows on item hover · color codes | G18.4 |
| **S4 — Share + import** | Implement `CloudVaultClient.upload()` · Share button uploads note → share code/link in chat · `/cb note import <id> <code>` with confirm-before-overwrite | (new — not in original G18.1–.7) |

> Persistence across restart (G18.5) is verified continuously, not a standalone slice.
> **Each slice ends `🟢 build-green` only — `✅ done` needs the owner's in-game confirm (CLAUDE.md §2).**

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

## Implementation Requirements — ⛔ SUPERSEDED by REVAMP v2 (kept for history)

### 1. Note Book GUI

`/cb note <id>` opens a chest GUI styled as an open book. Three tabs in the top row:

| Tab | Slot icon | Content |
|---|---|---|
| Lore | Written Book | Freeform description, supports `&` color codes, max 500 chars |
| To-Do | Paper | Checkable task list, up to 20 items |
| Tooltip | Name Tag | Short hover text shown on block item in inventory (max 100 chars) |

**Input methods (per Locked D1/D2/D4):** Lore = **writable book** (book-and-quill, handles 500 chars). To-Do
items + Tooltip text = **anvil** (short, fits the ~50-char cap). "Save" slot commits. "Share" button uploads
the note to the cloud vault (D6) and posts a share code in chat — vault endpoint, not the server host (no §A leak).

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
