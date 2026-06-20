# Group 07 — Bulk Operations

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 01 (Legacy Audit) signed off.
>
> **Objective:** Restore all bulk operations — delete, rename, re-ID, property change, export, move, duplicate, lock/unlock, favorite/unfavorite, shape/sound/recolor — with both command and chest GUI access.
>
> **Source issues:** Group A (bulkdelete, bulkrename, bulkreid, bulkproperty, bulkexport, bulkmove, bulkduplicate, bulklock, bulkunlock, bulkfavorite, bulkunfavorite, bulkshape, bulksound, bulkblockadd, bulkrecolor)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing. Bulk operations affecting more than 10 blocks must ask for confirmation before proceeding (`bulkConfirmThreshold = 10` from config).

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Bulk delete | `/cb bulkdelete <category>` or regex filter | Missing | Restored |
| Bulk rename | Rename all matching blocks with prefix/suffix | Missing | Restored |
| Bulk re-ID | Reassign IDs by pattern | Missing | Restored |
| Bulk property | Set glow/hardness/sound/collision on many blocks at once | Missing | Restored |
| Bulk export | Export all blocks in a category/filter to ZIP | Missing | Restored |
| Bulk move | Move all blocks in a category to another | Missing | Restored |
| Bulk duplicate | Clone a set of blocks with new IDs | Missing | Restored |
| Bulk lock/unlock | Lock or unlock entire categories | Missing | Restored |
| Bulk favorite/unfavorite | Favorite all blocks matching a filter | Missing | Restored |
| Bulk shape | Apply a shape to many blocks | Missing | Restored (requires Group 08) |
| Bulk sound | Set sound group on many blocks | Missing | Restored |
| Bulk recolor | Recolor entire categories (edge mode ONLY — never full mode) | Missing | Restored |
| Bulk GUI | `/cb bulkgui` — chest GUI for all bulk operations | Missing | Restored |
| Confirmation guard | "Confirm?" prompt when N > 10 | Missing | Restored (configurable via `bulkConfirmThreshold`) |

---

## What this group covers

| Feature | Commands |
|---|---|
| Bulk delete | `/cb bulkdelete <filter>` |
| Bulk rename | `/cb bulkrename <filter> <prefix/suffix/pattern>` |
| Bulk re-ID | `/cb bulkreid <filter> <pattern>` |
| Bulk property | `/cb bulkproperty <filter> <property> <value>` |
| Bulk export | `/cb bulkexport <filter>` |
| Bulk move | `/cb bulkmove <filter> <target-category>` |
| Bulk duplicate | `/cb bulkduplicate <filter> <id-prefix>` |
| Bulk lock/unlock | `/cb bulklock <filter>` / `/cb bulkunlock <filter>` |
| Bulk favorite/unfavorite | `/cb bulkfavorite <filter>` / `/cb bulkunfavorite <filter>` |
| Bulk shape | `/cb bulkshape <filter> <shape>` |
| Bulk sound | `/cb bulksound <filter> <sound>` |
| Bulk recolor | `/cb bulkrecolor <filter>` (edge mode only) |
| Bulk GUI | `/cb bulkgui` |
| Config | `bulkConfirmThreshold` (default 10) |

---

## Implementation Requirements

### 1. Filter Syntax

All bulk commands accept a filter argument that selects which blocks to operate on. The
resolver lives in `core/BulkScope.java` (ported + upgraded from the old mod's `BulkScope`,
adapted to the new immutable `SlotData` record + `SlotManager` API).

**Core filters (required by this group):**

| Filter | Meaning |
|---|---|
| `category:<name>` | All blocks in that category (case-insensitive exact match) |
| `id:<prefix>` | All blocks whose ID starts with the prefix *(new — old mod only did exact id)* |
| `name:<substring>` | All blocks whose display name contains the substring |
| `all` | All blocks (requires confirmation even if ≤ 10) |

**Bonus filters (recycled from the old mod's resolver, kept as free extras):**

| Filter | Meaning |
|---|---|
| `name:<prefix>*` | Display name *starts with* the prefix (wildcard form of `name:`) |
| `favorite:yes` / `favorite:no` | Favorited / not favorited by the running player |
| `locked:yes` / `locked:no` | Locked / unlocked blocks |
| `<id1>,<id2>,...` | Explicit comma-separated id list |
| `<id>` | A single block by exact id |

Example: `/cb bulkdelete category:test` — deletes all blocks in the "test" category.

### 2. Confirmation Guard

If a bulk operation would affect more than `bulkConfirmThreshold` (default 10) blocks:
- Chat shows: `This will affect N blocks. Type /cb confirm to proceed, or /cb cancel to abort.`
- No action taken until `/cb confirm` is run (or 60 seconds pass — auto-cancel).

If `N <= bulkConfirmThreshold`: executes immediately.

### 3. Undo for Bulk Operations

All bulk operations push a single undo entry describing the entire batch. One `/cb undo` reverts the full batch.

### 4. Bulk Recolor — Edge Mode Only

`/cb bulkrecolor` runs color resampling in **edge mode** only (Decision §7, pitfall note in CLAUDE.md). Full-mode recolor is blocked to prevent design destruction. This is hardcoded — no config option to enable full mode on bulk.

### 5. Bulk GUI

`/cb bulkgui` opens a chest GUI with:
- Filter builder slots (select category, ID prefix, etc.)
- Operation selector slots (one per bulk operation)
- Preview slot: shows "N blocks selected" before executing
- Execute slot: runs the operation (triggers confirmation if needed)

---

## Setup

```
/cb create g07a BulkTest1
/cb create g07b BulkTest2
/cb create g07c BulkTest3
/cb setcategory g07a bulktest
/cb setcategory g07b bulktest
/cb setcategory g07c bulktest
```

---

## Test G07.1 — Bulk property (glow on category)

```
/cb bulkproperty category:bulktest glow 8
```

**Expected:** `Set glow=8 on 3 block(s): g07a, g07b, g07c.`

Verify: `/cb list` — all three blocks show glow 8.

**Pass:** All 3 blocks updated.
**Fail:** Error, or only some blocks updated.

---

## Test G07.2 — Bulk property is undoable

```
/cb undo
```

**Expected:** All 3 blocks revert to glow 0. Single undo entry for the entire batch.

**Pass:** All 3 blocks reverted with one undo.
**Fail:** Undo only reverts one block, or multiple undos required.

---

## Test G07.3 — Confirmation guard fires at threshold

```
/cb bulkproperty all glow 0
```

(If more than 10 blocks exist on the server)

**Expected:** `This will affect N blocks. Type /cb confirm to proceed, or /cb cancel to abort.`

**Pass:** Confirmation prompt appears. Operation does not execute immediately.
**Fail:** Operation runs without confirmation.

---

## Test G07.4 — Confirm executes bulk

After G07.3's prompt:
```
/cb confirm
```

**Expected:** Operation executes. `Set glow=0 on N block(s).`

**Pass:** Executes after confirmation.
**Fail:** Confirm does nothing, or operation already ran before confirm.

---

## Test G07.5 — Bulk rename

```
/cb bulkrename category:bulktest prefix:[TEST]
```

**Expected:** `Renamed 3 block(s): names now start with "[TEST]".`

Verify: `/cb list` — g07a shows "[TEST]BulkTest1", etc.

**Pass:** All names prefixed correctly.
**Fail:** Error or names unchanged.

---

## Test G07.6 — Bulk duplicate

```
/cb bulkduplicate category:bulktest copy_
```

**Expected:** 3 new blocks created: `copy_g07a`, `copy_g07b`, `copy_g07c` with identical attributes.

**Pass:** 3 clones created with correct IDs and attributes.
**Fail:** Error, or fewer clones created.

---

## Test G07.7 — Bulk lock and unlock

```
/cb bulklock category:bulktest
```

**Expected:** `Locked 3 block(s) in category "bulktest".`

Try: `/cb setglow g07a 15` → should be blocked.

```
/cb bulkunlock category:bulktest
```

Try: `/cb setglow g07a 15` → should succeed.

**Pass:** Lock blocks edits; unlock restores editability.
**Fail:** Lock doesn't block edits, or unlock doesn't work.

---

## Test G07.8 — Bulk export

```
/cb bulkexport category:bulktest
```

**Expected:** `Exported 3 block(s) → bulktest-YYYYMMDD-HHMMSS.zip`

Check `config/customblocks/cloud_exports/` — ZIP file present.

**Pass:** ZIP created with all 3 blocks' data.
**Fail:** Error, or file not found.

---

## Test G07.9 — Bulk GUI opens

```
/cb bulkgui
```

**Expected:** Chest GUI opens with filter builder, operation selector, preview, and execute slots.

**Pass:** GUI opens with all sections.
**Fail:** Command missing or GUI empty.

---

## Implementation status

> **Slice 1 — built, NOT verified.** Awaiting the developer's in-game test (the golden rule:
> nothing is ✅ until tested in-game). Build is green (compile + 3 gates); jar staged at
> `.minecraft/mods/customblocks-1.0.0.jar`.

**Slice 1 ships (covers G07.1–G07.4):**

| Piece | Where |
|---|---|
| Filter resolver (core + bonus filters above) | `core/BulkScope.java` (new) |
| `/cb bulkproperty <filter> <glow\|hardness\|sound\|collision> <value>` | `command/handlers/BulkCommands.java` (new) |
| Confirm guard — `/cb confirm` / `/cb cancel`, 60s auto-expire, fires when N > threshold or `all` | `BulkCommands` pending-action map |
| `bulkConfirmThreshold` config (default 10) | `CustomBlocksConfig.java` |
| **Batch undo** — whole batch = ONE undo entry (`/cb undo` reverts all) | `UndoManager.Kind.BATCH` + `recordBatch()`, applied in `HistoryCommands` |

**Slice 2 adds (GUI + chat QOL — awaiting test):**

| Piece | Where |
|---|---|
| `bulkproperty` is now **mainly a GUI** — `/cb bulkproperty` (no args) / `/cb bulkgui` / `/cb bulk` / dashboard tile open a click-driven builder | `gui/chest/BulkPropertyMenu.java` + `BulkFilterMenu.java` + `BulkSession.java` |
| Click-to-pick filter (All / Favorited / Locked / per-category), property + value cycling, live "N matches" count, Apply | `BulkPropertyMenu` (thin front-end → runs the tested command) |
| **Cleaner chat** — bulk result is one line, full id list on **hover**, clickable `[↩ Undo]` | `BulkCommands` + new `Chat` rich helpers (`runButton`/`suggestButton`/`hover`/`line`) |
| Clickable confirm — `[✔ Confirm]` / `[✖ Cancel]` buttons instead of typing | `BulkCommands.sendConfirmPrompt` |

Notes:
- Locked blocks are **skipped** in a bulk op (reported in the result line), not errored.
- `bulkproperty` properties: `glow`/`light`, `hardness`, `sound`, `collision` (mirror the single-block setters).
- GUI filters cover the no-typing cases; `id:`/`name:` prefix filters stay on the chat command (need typed text).

**Not yet built (later slices):** `bulkdelete / bulkrename / bulkreid / bulkexport / bulkmove /
bulkduplicate / bulklock / bulkunlock / bulkfavorite / bulkunfavorite / bulksound / bulkrecolor`
(edge-mode only) and `/cb bulkgui` (tests G07.5–G07.9). `bulkshape` is out — needs Group 08.

---

## Group 07 Verdict

| Test | Description | Result |
|---|---|---|
| G07.1 | Bulk property sets glow on category | ⬜ |
| G07.2 | Bulk operation is undoable as single entry | ⬜ |
| G07.3 | Confirmation guard fires at threshold | ⬜ |
| G07.4 | `/cb confirm` executes the operation | ⬜ |
| G07.5 | Bulk rename adds prefix | ⬜ |
| G07.6 | Bulk duplicate creates clones | ⬜ |
| G07.7 | Bulk lock and unlock | ⬜ |
| G07.8 | Bulk export creates ZIP | ⬜ |
| G07.9 | Bulk GUI opens | ⬜ |

**Group 07 passes when all bulk operations work, confirmations fire correctly, and bulk GUI is functional.**

If anything shows ❌ — paste:
1. The exact command typed
2. How many blocks were affected vs expected
3. Last 20 lines of `latest.log`

---

## 💡 Parked idea — `/cb setall <setting> <value>` shorthand (revisit later)

> **Status:** idea captured **2026-06-20**, **marked WILL-REVISIT — not scheduled, not built.** Kept here
> so it isn't lost. Dev asked to park it for now.

**The idea:** a friendly one-liner — `/cb setall glow 15` — that sets a setting on **every custom block in
the mod** at once.

**Why it's mostly already here:** this is `/cb bulkproperty all <setting> <value>` (Test G07.1/G07.3). The
`all` filter already means *every custom block*, and that path already has the **confirm guard** and the
**single-entry batch undo** the dev wants. So `setall` would be a **thin alias** over the existing tested
engine — reuse, don't fork (CLAUDE.md §5).

**What's decided (for whenever it's revisited):**
- **Build as an alias** of `bulkproperty all …` → inherits confirm + one-shot `/cb undo` for free.
- **Auto-backup before EVERY setall** (dev pick) — snapshot all block data first (route through the
  Group 09 backup rail), so a bad `setall` is always recoverable beyond undo.

**Still open (decide on revisit):**
- **Which settings** `setall` accepts. `bulkproperty` today covers **glow, hardness, sound, collision**;
  the dev said "all settings in the mod" (would also want **category**, maybe **shape** via Group 08).
  Coverage was **left undecided** when parking.
- Backup retention (keep-all vs prune to last N) if auto-backup-every-time piles up files.

---

## Cleanup

```
/cb bulkdelete category:bulktest
/cb delete copy_g07a
/cb delete copy_g07b
/cb delete copy_g07c
```
