# Group 12 - Export & Import

> Group 12 writes CustomBlocks out to local files and turns dropped image files into blocks. Everything that travels over the internet belongs to G20, every screen belongs to G27, and recovering lost blocks belongs to G09.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_12.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Two jobs, both local, both command-driven and admin-only: write a block, a category, or the whole set to a file on disk, and turn a folder of dropped images into blocks.

Export is a hand-off artifact, not a recovery route — G09 already owns getting lost blocks back, and G12 must not grow a second one.

This Group does not own screens, download links, cloud sharing, backups, resource packs, category meaning, or image decoding.

## Ownership

| Owns | Does not own |
| --- | --- |
| Local block/category/all-block export artifacts and their layout | Every export/import Screen and dashboard: G27 |
| `/cb importfolder` — image and data-file input rules | Download links and any internet delivery: G20 |
| The last import run's report | Vault codes, Marketplace, cross-server sharing: G20 |
| Export/import result messages on command routes | Backups, restore, and recovering lost blocks: G09 |
| Filename → block id/display-name mapping for imports | Category schema and membership meaning: G11 |
| | Resource-pack delivery: G05 · Image decode, bake, background removal: G10 |

## Direction

Export writes to `config/customblocks/cloud_exports/` and says exactly what it wrote: count, file name, size, folder. No server address is ever printed on a command route.

Import is one command, `/cb importfolder`, run deliberately — not a folder watcher. It previews before it commits, fixes problems inline, and registers as a single undo entry.

The 2026-07-30 scope reset removed four things that were never really this Group's: the download link (internet transport, G20), Vault and Marketplace (G20, and Vault is already built), Blueprints (redundant now that Vault codes work — deleted, not maintained), and resource packs plus schematics (G05 owns one, the others save builds not blocks).

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-29 | `importfolder` accepts image files and applies matching textures to data-file imports. | Image-only and data-plus-image folder inputs have a real target behavior. |
| 2026-07-30 | G12 is command routes only. | Any dashboard, Screen, or GUI export surface is G27's, including a future `importfolder` screen. |
| 2026-07-30 | Download links and all internet delivery leave G12 for G20. | The unreachable-link regression is G20's problem; G12 reports the local file honestly and stops there. |
| 2026-07-30 | Blueprint items are removed entirely. | G20 §A Vault codes already move blocks between people and are confirmed working. The Blueprint commands and item get deleted. |
| 2026-07-30 | Resource-pack export and schematic/NBT formats are dropped from G12. | G05 already owns resource packs; schematics save builds, not block definitions. |
| 2026-07-30 | Exports record **all** categories per block, and only one export layout exists. | No legacy single-category layout is written or maintained alongside it. |
| 2026-07-30 | Export result messages state count, file name, size, and folder. | The owner can find the artifact in the host file panel without guessing. |
| 2026-07-30 | `importfolder` previews before it commits. | Nothing is created until the preview is confirmed, and problems are fixable from inside the preview. |
| 2026-07-30 | Imported display names use capital-first-letter-per-word, matching `/cb create`. | `red_brick.png` becomes `Red Brick`. |
| 2026-07-30 | An import run is one undo entry. | `/cb undo` reverses the whole run; individual blocks use the normal delete path, not a special one. |
| 2026-07-30 | Imported source files move to a done folder. | Re-running does not reprocess them and no original image is destroyed. |
| 2026-07-30 | Animated GIF handling in `importfolder` is parked for its own session. | Folder-import decisions currently cover still images only. |
| 2026-07-30 | Export files are named after the block id; zip names carry the date. | Safe on every filesystem, and a new backup never overwrites yesterday's. |
| 2026-07-30 | Import uses a dedicated import folder with `done` inside it, not the exports folder. | Files waiting to come in are never mixed with files that went out. |
| 2026-07-30 | Export and `importfolder` are admin-only. | Both write server files and create blocks in bulk. |
| 2026-07-30 | Import progress reuses the pack-sync top-center progress panel. | One progress look across the mod; needs a server→client progress message. |
| 2026-07-30 | An interrupted run resumes from the `done` folder. | Re-running handles only the remainder — no duplicates, no lost images, no separate resume state to persist. |
| 2026-07-30 | G12 does **not** get its own restore path. | G09 backup/restore is built and confirmed. Exports are a hand-off artifact, not a recovery route — a second way back is clutter. |
| 2026-07-30 | A zip dropped in the import folder is unpacked and imported like loose files. | No manual unzipping before an import. |
| 2026-07-30 | A sizeable import run asks G09 for a normal backup first. | Safety beyond undo, with no backup code of G12's own. Whether "auto-backup before risky operations" becomes a general G09 rule is G09's call. |

## Feature Plan

### A. Local Export

**Player outcome**

Creators can export one block, a category, or every block to a clear local file.

**Experience**

- Single block to a data file or to a PNG, named after the block id.
- All blocks to one zip; one category to one zip; both names carry the date.
- Every result message names count, file, size, and folder.
- Console gets the same plain text a player gets.

**Requirements**

- Artifacts live under `config/customblocks/cloud_exports/`.
- File names come from the block id, never the display name — no spaces, no case surprises, safe on any filesystem.
- Zip names carry the date so a new backup never silently replaces an older one.
- Contents are validated before success is reported.
- Category exports carry every category a block belongs to, using the G11 membership model.
- Exactly one export layout exists — no parallel legacy format.
- Admin-only.
- No command route may print a server host or IP.

**Boundary**

G12 creates the artifact and owns its layout. G27 owns any screen that triggers it. G20 owns delivering it to anyone off the machine.

### B. Folder Import

**Player outcome**

Drop a pile of images in a folder, run one command, get blocks — with a look before anything happens and an easy fix for anything wrong.

**Experience**

- `/cb importfolder` scans the folder and shows a preview: each file, the id and display name it will get, slots used, slots remaining, and every problem marked.
- Confirm creates the blocks; cancel changes nothing.
- Problem files get their own clickable line — rename (anvil box), delete the file, or ignore.
- Name clashes never overwrite.
- More images than free slots: fill what fits, list the leftovers as retryable lines.
- Progress is reported during long runs through the same top-center panel the pack sync uses.
- Imported source files move to a `done` folder inside the import folder, which is also what lets an interrupted run resume — the next run simply sees less work left.
- Blocks arrive uncategorised, as `/cb create` leaves them.
- The whole run is one undo entry; the report also offers a normal delete per created block.

**Requirements**

- A dedicated import folder, created on first run with a message saying where to drop images. Not the exports folder.
- A zip dropped in that folder is unpacked and treated as loose images.
- A sizeable run asks G09 for a normal backup before it starts.
- Supported still-image types plus data-file-and-image pairing.
- Naming follows `/cb create` rules exactly: safe ids, capital-first-letter-per-word display names.
- Admin-only.
- Progress needs a server→client message to drive the shared panel; that plumbing is part of this job.
- Background removal uses the current configured mode and tolerance — no special import-only behaviour.
- Image decode and bake reuse G10.
- No file type causes the run to abort; unusable files are named with a reason and skipped.

**Boundary**

G12 owns the command, the chat report, and the anvil rename. A full preview *Screen* is G27's if it is ever wanted.

### C. Import Run Recall

**Player outcome**

The last import can be reviewed after chat has scrolled away.

**Experience**

- One command brings back the last run's report: created, skipped, and why.
- The per-block delete buttons still work from the recalled report.
- Nothing run yet, or an already-undone run, is stated plainly rather than guessed at.

**Requirements**

- It is the same report object §B produced, kept — not a second reporting system.
- If the report does not survive a restart, it says so instead of showing a stale one.

**Boundary**

A record of what an import did. Not a history browser, not a backup.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Resource packs | G05 owns resource-pack delivery outright; G12 does not export one. |
| G10 | Images | Folder import uses G10 decoding, baking, and the configured background mode. |
| G11 | Categories | G11 supplies the membership model; G12 serialises every membership into the artifact. |
| G20 | Internet | G20 owns download links, Vault codes, and Marketplace. G12 hands it finished local artifacts and nothing else. |
| G27 | Screens | G27 owns the export dashboard (§T) and any future import screen; G12 supplies the routes and services behind them. |
| G05 | Progress panel | The top-center progress panel built for pack sync is reused for import runs; G05 owns the panel itself, G12 only feeds it counters. |
| G09 | Backup and recovery | G09 owns backups, restore, and getting lost blocks back. G12 has no restore path of its own and only asks G09 for a safety copy before a sizeable import. |

## Technical Contract

- Local export paths are under `config/customblocks/cloud_exports/`; artifacts are named from the block id, and zips carry the date.
- Import reads a dedicated import folder, with `done/` inside it; it is created on first run if missing.
- Export services validate an artifact before reporting success, and the message states count, file, size, and folder.
- One export layout only; it carries full category membership per block.
- `importfolder` previews, then commits; it never creates before confirmation.
- Import runs record a single batch undo entry through the existing undo system.
- Imported source files are moved to `done/`, never deleted; that move is also the resume mechanism after an interrupted run.
- Import naming reuses the `/cb create` id and display-name rules.
- Long runs drive the shared top-center progress panel over a server→client message.
- Export and `importfolder` are admin-gated.
- A zip in the import folder is unpacked into the same still-image path; a sizeable run triggers a G09 backup first.
- No command route emits a raw server host/IP.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Animated GIF handling in folder import | Needs its own session: frame caps, size caps, crash history on this server. | G12 |
| `importfolder` preview as a full Screen | Chat report plus anvil rename is enough for now. | G27 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Chat `[download]` links could expose a server host and still count as a finished export. | The whole download route left G12 for G20 on 2026-07-30. |
| 2026-06-21 | Blueprint is a same-server item handoff worth keeping. | Removed entirely on 2026-07-30; Vault codes do the job. |
| 2026-07-10 | Export Dashboard remains a chest GUI, migrating to G27 Screens. | G12 stopped owning any screen on 2026-07-30; G27 §T owns it. |
| 2026-07-12 | Export Dashboard uses a hand-picked Bulk Choose flow. | Screen-flow decision; it moved to G27 with the dashboard. |
| 2026-07-12 | Vault share/import and Marketplace wait in G12 for the G20 path. | Both are G20's outright. Vault block sharing is already built and confirmed there. |
| 2026-07-12 | Advanced binary and vanilla-pack formats remain parked in G12. | Dropped on 2026-07-30: G05 owns resource packs, and schematics save builds rather than blocks. |
| 2026-07-25 | Whether category exports carry multi-membership was undecided. | Settled 2026-07-30: they carry every membership, in one single format. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_12.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G11 Categories](GROUP_11_CATEGORY.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Image Input Overhaul](../Information/IMAGE_INPUT_OVERHAUL.md)
- [Pre-template Group 12 snapshot](../archive/group-migration-2026-07-18/GROUP_12_EXPORT_MARKETPLACE.md)
