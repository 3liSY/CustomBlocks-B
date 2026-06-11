# Group 01 — Legacy Feature Audit

> **Prerequisite:** Phases 0–16 build-verified. Old `CustomBlockCommand.java` accessible at `CustomBlocks/src/main/java/com/customblocks/command/CustomBlockCommand.java`.
>
> **Objective:** Produce a complete, authoritative gap analysis between old CustomBlocks and new CustomBlocks-B before any restoration work begins. Nothing in Groups 02–30 may be marked in-progress until this audit report is signed off by the developer.
>
> **Source issues:** 17.11
>
> **Rules:** This group produces a document, not runnable code. The output of this audit feeds directly into the implementation priority of every other group. Do not skip or abbreviate it.

---

## What this group does

This is a prerequisite investigation group. No feature is built here — the output is a signed-off audit table that confirms the full scope of restoration work and locks implementation priority.

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Command surface | ~150 subcommands in monolithic `CustomBlockCommand.java` | ~35 subcommands split across handlers | Catalogue every gap: Missing / Partial / Broken / Replaced-by-inferior |
| GUI surface | Monolithic `GuiManager.java` (10,324 lines) | 11 screen classes + `GuiEngine` | Identify all GUI workflows that exist in old but not new |
| Systems | BackupManager, SnapshotManager, DropConfigManager, etc. | Subset rebuilt cleanly | Identify systems not yet ported |
| Config | ~80 fields in old `config.json` | ~15 fields in new `config.json` | Identify every dropped or missing config field |

---

## What this group covers

| Audit Area | Source |
|---|---|
| Full old `/cb` subcommand surface | `CustomBlockCommand.java` (6,784 lines) |
| GUI workflows | `GuiManager.java` (10,324 lines) |
| Manager/system classes | All `*Manager.java` in old project |
| Config fields | Old `config.json` |
| Known missing examples | `/cb snapshots`, `/cb voice`, `/cb find`, `/cb recent`, etc. |

---

## Implementation Requirements

### 1. Audit Scope

Compare current new CB-B implementation against old CustomBlocks. For each item, assign a status:

| Status | Meaning |
|---|---|
| `EXISTS` | Fully present and working in new CB-B |
| `MISSING` | Was in old CB, not in new CB-B at all |
| `PARTIAL` | Partially ported — some sub-features missing |
| `BROKEN` | Present in new CB-B but does not work correctly |
| `REPLACED` | Old feature replaced by a new equivalent |
| `SCRAPPED` | Intentionally dropped (e.g. Voice Modes) |

### 2. Full old subcommand reference

The complete old `/cb` subcommand surface (from `CustomBlockCommand.java`):

```
_internal_setglobalbg achievements add addshape ai apply arabic audit backup bgstudio
blockadd blocks blockscat blockscategory brush bulk bulkblockadd bulkcolor bulkdelete
bulkduplicate bulkexport bulkfavorite bulkgui bulklock bulkmove bulkproperty bulkrecolor
bulkreid bulkrename bulkshape bulksound bulkunfavorite bulkunlock cache cb chisel clear
clearallfaces clearface clearshape colors config confirm create customblock customcolor
customtriangle delete deletedblocks deleter diagnostics dress dupe duplicate edge edithud
editmagicitems editor expiry export exportall exportblock exportcategory exportpng
facechangegui favorite find full give givecategory givedisplayblock gradient gui help
hexagon history hologram import importblock importcategory importfolder list listgui lock
macro magicitems market marketplace menu note off on palette panic particles pause recent
record recover rectangle redo redogui reid reload remove removeshape rename reset resize
resourcepack restore resume retexture rp run safety save screenshot script scriptgui search
setcollision setface setglow sethardness setshape setsound settabicon settings shapeeditor
shapelist shapepreview sharecategory show showbrokenblocks showcase snapshots sounds square
stop swapid swapname sync template text tolerance triangle trianglemode undo undogui
unfavorite unlock unsuppress voice welcome
```

### 3. Known scrapped features (do not restore)

| Feature | Reason |
|---|---|
| `/cb voice` | Voice Modes intentionally scrapped (Decision §I) — use single polished tone |
| `/cb snapshots` | Merged into `/cb backup` system (Decision §C) |

### 4. Audit output format

The audit must produce a table with these columns:

```
| Command/Feature | Old CB Status | New CB-B Status | Restoration Group | Priority |
```

Priority levels: **P0** (blocks testing), **P1** (core UX), **P2** (quality of life), **P3** (advanced/optional).

### 5. Restoration policy

Any legacy feature that provided meaningful functionality must be evaluated. The goal is not parity — it is recovering functionality unintentionally lost. Edge cases and broken old-CB behavior should be rebuilt correctly, not copied.

---

## Output Required

- **Audit table** — every old command/feature with status + restoration group reference.
- **Priority list** — top 10 items by developer-assigned priority.
- **Scope confirmation** — developer signs off that Groups 02–30 cover everything.

---

## Test G01.1 — Audit table completeness

Walk through every subcommand in the reference list above. For each one, assign a status. The audit table must have an entry for every item in the list — no blanks.

**Expected:** Table with 150+ rows, all statuses filled, no "TBD" entries.

**Pass:** Developer reviews and agrees the table is complete.
**Fail:** Rows missing, statuses left blank, or developer finds a gap not in the table.

---

## Test G01.2 — GUI workflow completeness

List every GUI screen/workflow from old `GuiManager.java`. Mark each as EXISTS / MISSING / PARTIAL in new CB-B.

**Expected:** All old GUI workflows accounted for with a clear status and restoration group.

**Pass:** Developer confirms no GUI workflow was accidentally omitted.
**Fail:** Developer names a workflow that has no entry in the audit.

---

## Test G01.3 — Config field completeness

Compare old `config.json` field list against new `CustomBlocksConfig.java`. Every old field must have a status: EXISTS / DROPPED / RENAMED / MERGED.

**Expected:** All config fields accounted for. Only `voiceMode` is intentionally dropped.

**Pass:** Developer confirms config coverage is complete.
**Fail:** Config fields found in old that have no status entry.

---

## Test G01.4 — Developer sign-off

Developer reviews the full audit output and confirms:
1. No restoration group is missing a critical feature.
2. Priority assignments are correct.
3. Groups 02–30 collectively cover everything marked MISSING, PARTIAL, or BROKEN.

**Pass:** Developer explicitly says "audit approved" or equivalent.
**Fail:** Developer finds gaps, or sign-off is not given.

---

## Group 01 Verdict

| Test | Description | Result |
|---|---|---|
| G01.1 | Audit table — all 150+ commands accounted for | ⬜ |
| G01.2 | GUI workflow gaps identified | ⬜ |
| G01.3 | Config field coverage complete | ⬜ |
| G01.4 | Developer sign-off granted | ⬜ |

**Group 01 passes only when the developer explicitly approves the audit output. No other group may begin implementation until G01.4 is ✅.**

If anything shows ❌ — paste:
1. The specific gap found
2. Which old feature/command was missed
3. Suggested restoration group assignment
