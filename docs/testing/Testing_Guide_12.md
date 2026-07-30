# Group 12 - Export & Import

## Status

| | |
| --- | --- |
| **Verdict** | All three sections are built and waiting on in-game testing; Blueprints are deleted. |
| **Progress** | 🟩🟩🟩🟩🟥🟥🟥🟥🟥🟥 40% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Export file commands and result messages | Built 🎯 | - |
| B | `importfolder` image input overhaul | Built 🎯 | - |
| C | Import run recall | Built 🎯 | - |

> Built 2026-07-30 in four checkpoints: export rework, `importfolder` overhaul, run recall, Blueprint removal. Nothing waits on a decision. Every row below is 🎯 — code exists and compiles; none of it is owner-confirmed.

**Original Group:** [GROUP_12_EXPORT_MARKETPLACE.md](../groups/GROUP_12_EXPORT_MARKETPLACE.md) · **Build handoff:** [HANDOFF_G12_BUILD.md](../Information/HANDOFF_G12_BUILD.md)

---

# Active Tests

## 💡 Setup

- Create `g12a` and `g12b`, give each a texture, and assign both to `exporttest`.
- Test from console and from a player, on the host and from a remote client.
- **Command routes only.** Every dashboard/Screen/GUI export surface is G27's (§T). Anything you notice about the export *screen* goes to TG27, not here.
- **Scope reset (2026-07-30):** G12 owns writing export files, reading them back, and `importfolder`. It no longer owns download links, Vault codes, Marketplace, Blueprints, resource packs, or schematics.
- Every artifact now lands in `config/customblocks/cloud_exports/`; the bulk list files used to drop into `exports/`.
- §B test blocks come from image files; keep a small PNG, a JPG/JPEG, a WebP, a name that clashes with an existing block, and a name with spaces/capitals ready.
- §B drops files into `config/customblocks/import/`, with `done/` inside it — not the exports folder. Both export and import are admin-only.
- §B preview buttons: `/cb importfolder confirm` · `cancel` · `fix rename|delete|ignore <file>`. §C recall is `/cb importfolder last`.
- A run of **5+ pictures** takes a G09 safety backup first; smaller runs do not.
- Animated GIF handling inside `importfolder` is **parked** pending its own session — see Parked.

## A - Export file commands and result messages - Built 🎯

| | |
| --- | --- |
| **Check** | Export commands write the correct file to `config/customblocks/cloud_exports/`, name it after the block id, and report count, file, size, and folder. |
| **Pass rule** | Block data file, block PNG, all-blocks zip, category zip, file naming, dated zip names, multi-category payload, result message, console, and permission rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Payload** | Schema v2: one layout, `categories` as an array. No legacy single-category layout is written or read. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Export `g12a` as a data file. | `cloud_exports/g12a.json` exists and contains the block's metadata. | 🎯 | 🎯 |
| A2 | Export `g12a` as PNG. | `cloud_exports/g12a.png` exists and matches the block texture. | 🎯 | 🎯 |
| A3 | Export a block whose display name has spaces and capitals. | The file is named after the **block id**, not the display name — lowercase, no spaces, safe on any system. | 🎯 | 🎯 |
| A4 | Export all blocks to zip. | `all-<date>.zip` contains every block's data file and texture, and the date in the name keeps yesterday's backup from being overwritten. | 🎯 | 🎯 |
| A5 | Run `/cb category export exporttest`. | Category zip contains only that category's blocks plus their metadata and textures, and its name carries the date too. | 🎯 | 🎯 |
| A6 | Put `g12a` in two categories, then export one of them. | Every exported block records **all** categories it belongs to, not just the exported one. One format only — no second legacy layout is written. | 🎯 | 🎯 |
| A7 | Read any export result message. | Message names the block/count, the file name, its size, and the containing folder. No server address appears. | 🎯 | 🎯 |
| A8 | Run every export route from console. | Console gets the same plain-text result; no attempt to open a screen. | 🎯 | ➖ |
| A9 | Try any export route as a non-admin. | Refused with the standard permission message. Export is admin-only. | ➖ | 🎯 |

## B - `importfolder` image input overhaul - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb importfolder` turns a folder of images into blocks in one run, with a preview first, clickable fixes for problem files, and a single undo entry. |
| **Pass rule** | Image formats, naming, preview, inline fix, clash report, slot cap, done-folder move, background mode, progress panel, resume, undo, per-block delete, and permission rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Rules** | The 22 locked rules live in `docs/Information/IMAGE_INPUT_OVERHAUL.md` §8. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Put `chair.png`, `table.jpg`, `lamp.jpeg`, and `rug.webp` in the import folder, then run `/cb importfolder`. | Every supported image is recognised. Nothing is created yet — a preview is shown first. | 🎯 | 🎯 |
| B2 | Read the preview. | Preview lists each file with the block id and display name it will get, the slots this run will use, the slots left after, and every problem file marked. | 🎯 | 🎯 |
| B3 | Confirm the preview. | Only then are blocks created, and only the previewed ones. | 🎯 | 🎯 |
| B4 | Cancel or close the preview. | Nothing is created, nothing is moved, folder untouched. | 🎯 | 🎯 |
| B5 | Import `red_brick.png` and `stone wall.PNG`. | Display names come out `Red Brick` and `Stone Wall` — capital first letter on **every** word, rest lowercase, exactly like `/cb create`. Ids follow the same safe-id rules as `/cb create`. | 🎯 | 🎯 |
| B6 | Import a file whose name clashes with an existing block. | That file gets its own clickable line: rename, delete the file, or ignore it. Nothing is overwritten. | 🎯 | 🎯 |
| B7 | Click rename on a clashing file. | Anvil typing box opens; the typed name is validated the same way `/cb create` validates, then that file imports. | 🎯 | 🎯 |
| B8 | Click delete on a problem file. | Only that file is removed from the import folder; no block is touched. | 🎯 | 🎯 |
| B9 | Click ignore on a problem file. | File stays in place, that line closes, the rest of the run is unaffected. | 🎯 | 🎯 |
| B10 | Put more images in the folder than there are free slots. | The run fills every free slot, then lists the leftovers as clickable lines so they can be retried after freeing slots. Nothing silently vanishes. | 🎯 | 🎯 |
| B11 | Finish a successful run. | Every imported source file moves into a `done` folder **inside** the import folder, so re-running does not reprocess them and no original is destroyed. | 🎯 | 🎯 |
| B12 | Change the background-removal mode, then import. | The run uses the current background mode/tolerance setting — same result as creating those blocks one by one. | 🎯 | 🎯 |
| B13 | Import 30+ images. | The same top-center progress panel the pack sync uses reports the run; the server does not appear frozen or silent. | 🎯 | 🎯 |
| B14 | Run `/cb undo` after an import run. | The whole run reverses as **one** history entry, not one entry per block. | 🎯 | 🎯 |
| B15 | Use the delete button beside a created block in the report. | That single block is removed through the normal delete path — no special-case behaviour. | 🎯 | 🎯 |
| B16 | Import blocks with no category set. | Blocks arrive uncategorised, exactly as `/cb create` leaves them. | 🎯 | 🎯 |
| B17 | Put a block data file next to a same-name image. | Metadata from the data file imports and the image is applied as its texture. | 🎯 | 🎯 |
| B18 | Put a broken or non-image file in the folder. | It is named in the preview as unusable with a clear reason, every good file still imports, and the run never aborts. | 🎯 | 🎯 |
| B19 | Kill the server or disconnect part-way through a big run. | Files already imported sit in `done`; re-running picks up only the remainder, with no duplicates and no lost images. | 🎯 | 🎯 |
| B20 | Run `/cb importfolder` as a non-admin. | Refused with the standard permission message. Import is admin-only. | ➖ | 🎯 |
| B21 | Run `/cb importfolder` with no import folder present. | The folder is created and the message says where to drop images. Nothing errors. | 🎯 | 🎯 |
| B22 | Drop a zip full of images into the import folder and run the command. | The zip is unpacked and its images import like loose files — no manual unzipping. Junk inside the zip is skipped like any other bad file. | 🎯 | 🎯 |
| B23 | Start a run large enough to matter. | A normal G09 backup is taken first, so a bad batch can be walked back even after undo history moves on. | 🎯 | 🎯 |

## C - Import run recall - Built 🎯

| | |
| --- | --- |
| **Check** | The last import run can be reviewed after the chat has scrolled away, without hunting through history. |
| **Pass rule** | Recall after scroll, recall after restart, per-block actions, empty state, and undo agreement rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Command** | `/cb importfolder last`. Saved to `data/last_import.json`, so it survives a restart. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Import a folder, chat for a while, then recall the last run. | The same report comes back: what was created, what was skipped, and why. | 🎯 | 🎯 |
| C2 | Recall the last run after a restart. | The report survives, or it says plainly that it does not — never a blank or wrong answer. | 🎯 | 🎯 |
| C3 | Use the per-block delete button from a recalled report. | Works exactly as it did in the live report, through the normal delete path. | 🎯 | 🎯 |
| C4 | Recall with no import ever run. | Says so plainly. No error, no empty box. | 🎯 | 🎯 |
| C5 | Undo the run, then recall it. | The report shows the run as undone rather than claiming blocks still exist. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none — the `[download]` link regression moved to G20 on 2026-07-30 along with the whole download route)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- Animated GIF handling inside `importfolder` — 💤 `2026-07-30`: GIFs have crashed this server before. Frame caps, size caps, and whether folder import should accept them at all need their own discussion session. Until then `importfolder` decisions cover still images only.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Blueprint item export/import — 👎 `2026-07-30`: Deleted. Vault code sharing (G20 §A) already moves a block between people and is confirmed working, so a same-server-only item added nothing. The item, both commands, the dashboard tile and the help entries are gone.
- Advanced `.litematic`, `.schem`, and NBT formats — 👎 `2026-07-30`: These save *builds*, not block definitions. Wrong feature for this Group; dropped rather than parked.
- Resource-pack export — 👎 `2026-07-30`: Duplicate. G05 already owns resource-pack delivery and it already works automatically.
- Restore blocks from an export zip — 👎 `2026-07-30`: Proposed, then cut the same day. G09 backup/restore is built and confirmed in-game; a second way to get blocks back is clutter. Exports stay a hand-off artifact, not a recovery path.
- Reading old single-category export files — 👎 `2026-07-30`: Died with the restore path above.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g12a`, `g12b`, category `exporttest`, and temporary export files after testing.
- [ ] Empty the import folder and the done folder between §B runs.
- [ ] Keep every export Screen/dashboard finding in G27 §T.
- [ ] Keep download links, Vault codes, and Marketplace findings in G20.
- [ ] Keep category schema/membership meaning in G11; G12 only serialises it.
- [ ] Do not re-add Blueprints, schematics, a second resource-pack path, or a second restore path.
- [ ] Keep backup/restore findings in G09; G12 only asks it for a pre-import safety copy.

</details>
