# HANDOFF — Group 12 build session (Export & Import)

> Written `2026-07-30` at the end of a discuss-only design session. Every decision below is
> owner-locked. **Do not re-litigate them** — build them.
>
> Canonical docs: [GROUP_12_EXPORT_MARKETPLACE.md](../groups/GROUP_12_EXPORT_MARKETPLACE.md) ·
> [Testing_Guide_12.md](../testing/Testing_Guide_12.md) · folder-import spec in
> [IMAGE_INPUT_OVERHAUL.md](IMAGE_INPUT_OVERHAUL.md) §8.

---

## 0. Read first

1. `docs/groups/GROUP_12_EXPORT_MARKETPLACE.md` — Locked Decisions table is the contract.
2. `docs/testing/Testing_Guide_12.md` — §A (9 rows), §B (23 rows), §C (5 rows) are the acceptance list.
3. `docs/Information/IMAGE_INPUT_OVERHAUL.md` §8 — 22 numbered `importfolder` rules.
4. `THE_ROYAL_DIRECTIVE.md` — §3 read before code, §8 git checkpoints, §13 checklist, §14 security.

**Build environment:** repo is `CustomBlocks-B` (the clean rebuild — never edit `CustomBlocks/`,
that is reference-only). Needs **JDK 21**; the `java` on PATH is Java 8. Set `JAVA_HOME` and build
with `--no-daemon`.

**Owner comms rule:** no code, no file paths, no class names in chat. Explain in plain language.
Decisions go through the question UI, not prose.

---

## 1. Scope of this jar

Three jobs, one jar, in this order. Each is independently testable; do not start the next until the
previous compiles green.

| Job | Section | State today |
| --- | --- | --- |
| 1. Export rework | TG12 §A | Built, needs a correctness pass |
| 2. `importfolder` image overhaul | TG12 §B | Only a JSON-only stub exists |
| 3. Import run recall | TG12 §C | Nothing |
| 4. Blueprint removal | — | Delete existing code |

Owner has **not** picked the order between them beyond "one by one". Job 1 first is recommended
because Job 2's report reuses its message style, and Job 4 is a pure deletion that can go anytime.

---

## 2. Job 1 — Export rework (TG12 §A)

**Touch:** `core/BlockExporter.java`, `command/handlers/UtilityCommands.java`,
`command/handlers/CategoryCommands.java`, `command/Chat.java` / `CbFmt` for the message.

### What changes

1. **File names come from the block id, never the display name.** Lowercase, no spaces.
   `exportOne`/`exportPng` already key off `d.customId()` — verify nothing downstream re-derives a
   name from `displayName()`.
2. **Zip names carry the date.** `exportAllZip` already stamps `all-YYYYMMDD-HHMMSS.zip`; make
   `exportCategoryZip` match. A new backup must never silently replace an older one.
3. **Exports carry EVERY category per block.** Today the payload reads the legacy single
   `SlotData.category` shadow field. Read `core/CategoryMembershipStore.java` instead — the real
   model, a set per block. **One format only**; do not write a legacy layout alongside it, and do
   not add a reader for the old one (owner: "only 1 shall live, no dupes").
4. **Result message states count, file name, size, folder.** Owner picked the fullest variant:
   ```
   [CB] ✔ Exported 12 blocks
   [CB] File: all-2026-07-30.zip  (3.4 MB)
   [CB] Folder: config/customblocks/cloud_exports/
   ```
   Keep the `[CB]` prefix and `✔`/`✖` glyphs — owner brand, never strip.
5. **Admin-gate every export route.** `UtilityCommands` currently registers `/cb export` with **no
   `.requires(...)` at all**. House style is `.requires(s -> s.hasPermissionLevel(2))`.
6. **Console parity.** Console gets the same plain text, never a screen attempt.

### Do NOT change

- The format list. Owner said keep all of `json/txt/csv/md/html/yaml/png/zip`.
- The `cloud_exports/` location.
- The `[download]` link behaviour or wording — **that route now belongs to G20 §L** and is
  awaiting a separate owner decision. Leave it exactly as-is. Do not "fix" it on a guess.

---

## 3. Job 2 — `importfolder` image overhaul (TG12 §B, 23 rows)

**Today:** `UtilityCommands.importFolderCmd` → `BlockExporter.importFolder(Path)` reads block JSON
only, from `config/customblocks/exports`, and never applies a texture. That is the whole feature.

**Reference implementation worth reading:** the old mod at
`Coding/CustomBlocks/src/main/java/com/customblocks/command/CustomBlockCommand.java`,
`cmdImportFolder` (~line 3978). It already did: 8 image extensions, animated GIF handling, size cap,
background removal, pad-to-square, slot cap, skipped/failed lists, off-thread work with a
`server.execute` hop for mutation. **Read it before writing anything** — owner explicitly said "it
was good before but needs to be improved". Reuse its shape; do not reinvent.

### The 22 locked rules

Full text in `IMAGE_INPUT_OVERHAUL.md` §8. Summary:

| # | Rule |
| --- | --- |
| 1 | Deliberate command, not a folder watcher |
| 2 | **Preview before commit** — files, ids, display names, slots used, slots left, problems marked |
| 3 | Problem files get per-file clickable lines: **rename** (anvil), **delete file**, **ignore** |
| 4 | Never overwrite on a name clash |
| 5 | Naming = `/cb create` rules; display name is **Capital On Every Word** (`red_brick.png` → `Red Brick`) |
| 6 | Background removal uses the **current configured mode/tolerance**, no import-only special case |
| 7 | Slot cap: fill what fits, list leftovers as retryable clickable lines |
| 8 | Imported source files move to `done/` **inside** the import folder; never deleted |
| 9 | Blocks arrive **uncategorised** |
| 10 | **One** batch undo entry per run; per-block removal uses the normal delete path |
| 11 | Progress reported during long runs |
| 12 | No file type aborts the run |
| 13 | **Animated GIFs are PARKED** — still images only this session |
| 14 | A preview *Screen* is G27's, not this job |
| 15 | **Dedicated import folder**, not `exports/`; created on first run with a "drop images here" message |
| 16 | Admin-only |
| 17 | A **zip dropped in the import folder is unpacked** and imported like loose files |
| 18 | Progress reuses the pack-sync top-center panel |
| 19 | Interrupted run **resumes by itself** via `done/` — no separate resume state |
| 20 | A sizeable run asks **G09** for a normal backup first |
| 21 | Last run's report is recallable (that is Job 3) |
| 22 | **No restore path in G12** |

### Anchors

- **Import folder path:** add a constant to `core/CbPaths.java` (`ROOT = config/customblocks`,
  siblings `DATA`, `BACKUPS`, `TRASH`, `TEXTURES`, `SOURCES`, `UPDATES`). Do not hardcode a literal
  anywhere else — that file's header says so explicitly.
- **Anvil rename:** `gui/chest/ReIdMenu.openAnvil(ServerPlayerEntity, String id, Runnable onCancel)`
  is the existing pattern. Reuse it rather than building a second anvil flow.
- **Clickable chat:** `command/Chat.java` → `runButton`, `suggestButton`, `copyButton`, `undoButton`.
- **Image work:** `image/ImageProcessor.toBlockPng(byte[], int)`, `dimensions`, `fillBackground`;
  background pipeline is the `image/Bg*.java` family driven by
  `CustomBlocksConfig.backgroundMode` (registered in `config/ConfigRegistry.java` ~line 119).
- **Batch undo:** `core/UndoManager.recordBatch(UUID player, List<Op> children, String label)`.
  Already exists for G07 bulk ops. **Do not build a new undo system** — the owner asked "it should
  just register as can be undoable, not that hard", and this is exactly that.
- **Pre-run backup:** `core/BackupManager.save(name, blocks, Kind, reason, note)`. Use the normal
  path; G12 writes **no** backup code of its own.
- **Progress panel:** `client/packsync/SyncProgressOverlay.java` (CLIENT-ONLY, G05, top-center,
  red+black `CbTheme`), fed by `client/packsync/ClientPackReceiver` and driven server-side from
  `network/packsync/PackSyncService`. **This is the panel the owner meant** — not a boss bar.
  Reusing it needs a new server→client progress payload. That plumbing is part of this job.
  Owner chose "reuse it" over "make it general", so keep the change minimal and additive; do not
  refactor the pack-sync path it reports on.
- **Threading:** heavy decode off-thread, mutation back on the server thread. The old mod's
  `thread(() -> { … server.execute(() -> { … }); })` shape is correct and proven.
- **Permission:** `.requires(s -> s.hasPermissionLevel(2))`.

### Careful

- `SlotManager.setCategory` / `createNoSave` dual-write into the membership store, and there is a
  **documented lock-order hazard** around it (see the banner in `CategoryMembershipStore` /
  the G11 log entry). Bulk creation must not shadow inside the store's monitor.
- Slot exhaustion mid-run must not half-create a block.

---

## 4. Job 3 — Import run recall (TG12 §C, 5 rows)

Keep the report object Job 2 produces and let one command bring it back after chat scrolls.

- Same report, **not** a second reporting system.
- Per-block delete buttons still live from the recalled report.
- "Nothing run yet" and "already undone" are stated plainly, never guessed.
- If the report does not survive a restart, **say so** rather than showing a stale one. Persisting
  it is optional; lying about it is not.

---

## 5. Job 4 — Delete Blueprints

Owner: *"blueprint item does nothing and useless, needs nuking"* → **Remove it completely.**

Rationale: G20 §A Vault code sharing is **already built and confirmed in-game**, so a same-server-only
item adds nothing.

**Delete:** `item/Blueprint.java`, `command/handlers/BlueprintCommands.java`, the
`/cb exportblock` + `/cb importblock` registrations, the Blueprint tile in
`gui/chest/ExportDashboardMenu.java`, and any help entry in `gui/chest/HelpTopics.java`.

Do **not** delete `ExportDashboardMenu` itself — the export screen belongs to G27 §T, not to this job.

---

## 6. Hard boundaries — do not touch

| Thing | Owner |
| --- | --- |
| Any export/import **Screen** or dashboard, incl. a folder-import preview screen | **G27** §T |
| `[download]` link, its host-leak regression, Vault codes, Marketplace | **G20** §L / §A / §I |
| Backups, restore, recovering lost blocks | **G09** |
| Category schema and what a membership means | **G11** |
| Resource-pack delivery | **G05** |
| Image decode/bake/background algorithms | **G10** |

Scrapped, do not resurrect: Blueprints · `.litematic`/`.schem`/NBT · resource-pack export from G12 ·
a G12 restore path · reading old single-category export files.

Parked, needs its own session: animated GIFs in folder import · auto-backup before *all* risky bulk
ops (that one is G09's call, noted in TG09).

---

## 7. Definition of done

- [ ] Jar builds green on JDK 21, `--no-daemon`.
- [ ] TG12 §A rows A1–A9, §B rows B1–B23, §C rows C1–C5 are reachable by an owner test.
- [ ] No `[CB]` message anywhere prints a raw server host or IP.
- [ ] `/cb undo` reverses a whole import run as one entry.
- [ ] Blueprint commands are gone from tab-complete and help.
- [ ] Git checkpoint per job (Royal Directive §8), not one giant commit.
- [ ] Same pass as every build: update the **testing guide** (status marks + template v4 ordering
      🎯→✅→🟡→⏳) and **PROGRESS_LOG**. Status marks go in the TESTING GUIDE only — the group spec
      stays clean. Testing guide stays LEAN; bug/cause/fix narrative goes to PROGRESS_LOG.
- [ ] Do not mark anything ✅ — only the owner confirms in-game.

---

## 8. Owner working style

- Plain language only in chat. No code, no paths, no class names.
- Batch open questions through the question UI with plain-language options.
- Preview before bulk: show 1–2 samples and get approval before generating a full batch.
- Never install or run anything from the internet.
- Build the jar only when told.
