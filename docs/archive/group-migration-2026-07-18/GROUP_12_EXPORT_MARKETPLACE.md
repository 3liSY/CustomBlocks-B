# Group 12 — Export Dashboard & Marketplace

> 🟣 **importfolder REWORK — logged 2026-06-29 (NO code yet).** `/cb importfolder` today reads `.json`
> only and never applies textures (a matching `.png` next to it is ignored; see `BlockExporter.importFolder`).
> Plan = accept image files (png/jpg/gif/webp) and make a block from each (filename → id, bake the picture),
> and apply textures to `.json` blocks. Full record: **`docs/Information/IMAGE_INPUT_OVERHAUL.md`** (§6 + §9).

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 20 (External Integrations) verified. Phase 9 (Import/Export) build-verified.
>
> **Objective:** Redesign the entire export system into a robust Export Dashboard chest GUI. Add advanced export formats (litematic, schem, vanilla resource pack). Restore marketplace browsing and block sharing via short codes.
>
> **Source issues:** 17.15 (export system rework), Group H (sharecategory, importcategory, exportblock, importblock, market), Decision §11 (Universal Export Dashboard), Decision §H (litematic + schem + standalone vanilla resource pack)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.
>
> ⚠️ **UI medium audit (2026-07-10):** the Export Dashboard and Marketplace are Screen-based, not chest GUI,
> per the mod-wide Screen migration. Note: the Export Dashboard is already **built and confirmed in-game**
> (2026-06-21, `ExportDashboardMenu`) as a chest GUI — owner confirmed 2026-07-10: convert to Screen (default
> applies, no exception). Real rework, marked ⏳ planned. Marketplace (`/cb market`) is unbuilt either way, so
> it can target Screen directly whenever it's written.

---

> ## ⚙️ Implementation status — 2026-06-14 (built, build green, NOT in-game tested)
>
> Scope was set with the developer: the **offline core** is built now; the **online vault** parts and
> the **advanced formats** are deferred and marked partial (revisit at the end).
>
> | Test | State |
> |---|---|
> | G12.1 dashboard GUI | ✅ built (already existed) |
> | G12.2 single JSON + link | ✅ built (now `cloud_exports/`, dashboard tile + link) |
> | G12.3 single PNG + link | ✅ built (PNG link fixed → `cloud_exports/`) |
> | G12.4 Export All → ZIP | ✅ built (`/cb export zip` + tile, `all-<stamp>.zip` + link) |
> | G12.5 category ZIP + link | ✅ built (now a clickable `[download]`) |
> | G12.6 Blueprint item | ✅ built (`/cb exportblock` + `/cb importblock` from hand) |
> | G12.7 share-code round-trip | ⏸️ **partial — deferred** (needs the cloud vault deployed) |
> | G12.8 marketplace | ⏸️ **partial — deferred** (needs the cloud vault deployed) |
> | litematic / .schem / vanilla RP | ⏸️ **partial — deferred** (heavy binary formats) |
>
> **Note on download links:** they resolve from `httpHost` (default `127.0.0.1`) — fine for
> single-player / same machine. Remote friends need a reachable `host:port`.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Export GUI | `/cb export json/txt` (text-only output) | Text-only, no GUI | Full Export Dashboard (Screen-based target; currently built as chest GUI, see audit note) |
| Export formats | JSON, TXT | JSON | JSON, TXT, PNG, CSV, NBT, ZIP, litematic, schem, vanilla resource pack |
| Localhost download links | Not present | Not present | HTTP route generates clickable chat link |
| Share single block | `/cb vault upload <id>` or `/cb export <id> vault` → share code | Missing | Restored for one block |
| Import single block | `/cb importblock <code>` or `/cb vault download <code>` | Missing | Restored via Vault download |
| Marketplace | `/cb market` — browse shared blocks from others | Missing | Restored |
| Bulk export to vault | Not present | Not present | Parked; use one-block Vault share |
| Cloud exports folder | `config/customblocks/cloud_exports/` | Uses `exports/` | Updated to `cloud_exports/` |
| Blueprint item | Not present | Not present | New: physical in-game Blueprint item generated from block |

---

## What this group covers

| Feature | Commands |
|---|---|
| Export dashboard | `/cb export` (opens Screen-based target; currently a chest GUI) |
| Export single block | `/cb exportblock <id>` |
| Import single block | `/cb importblock <code>` |
| Bulk export all | From export GUI |
| Export by category | From export GUI |
| One-block Vault share | `/cb export <id> vault` or the single-block export GUI |
| Marketplace browse | `/cb market` (alias: `/cb marketplace`) |
| Download link | Clickable `[download]` link in chat |

---

## Implementation Requirements

### 1. Export Dashboard — moved to G27

`/cb export` opens the Export Dashboard — full spec moved to `GROUP_27_SCREENS.md` §G27.30 (2026-07-12).
G12 keeps the export formats/routes/Blueprint/storage logic below.

### 2. Advanced Export Formats

| Format | Description | Use case |
|---|---|---|
| `.litematic` | Litematica mod schematic | Map-making, sharing builds |
| `.schem` | WorldEdit schematic | Server building tools |
| Vanilla Resource Pack | ZIP with CustomModelData, no mod required | Sharing with non-modded players |

### 3. Localhost Download Links

The embedded HTTP server (already running on `resourcePackPort`) gains additional routes:
- `GET /export/<id>.<format>` — serves the export file for direct browser download.
- Chat link format: `[download]` → clickable, opens in browser.

Files served from `config/customblocks/cloud_exports/`.

### 4. Blueprint Item

`/cb exportblock <id>` (or from export GUI) can optionally generate a **Blueprint** — a physical item with the block's texture as its icon, block metadata in NBT, and a clickable tooltip. Players can trade or drop Blueprints. `/cb importblock` reads a Blueprint from the player's hand.

### 5. Marketplace — moved to G27

`/cb market` (alias `/cb marketplace`) — full spec moved to `GROUP_27_SCREENS.md` §G27.30 (2026-07-12).

### 6. Import by Code

`/cb importblock <code>` — downloads a block from the vault by its share code. If a block with the same ID already exists and a player ran the command, it opens the Vault conflict client screen; console gets a plain text conflict message.

### 7. Storage

All exports go to `config/customblocks/cloud_exports/`. The `CloudVaultClient` reads/writes this folder.

---

## Setup

```
/cb create g12a ExportTestBlock1 https://i.imgur.com/example.png
/cb create g12b ExportTestBlock2
/cb setcategory g12a exporttest
/cb setcategory g12b exporttest
```

---

## Test G12.1 — Export dashboard opens (currently a chest GUI; target = Screen)

```
/cb export
```

**Expected:** The dashboard opens. Scope selection lets you choose all blocks, categories, or one block; one-block export includes an Upload to Vault tile. Currently opens as a chest GUI in code — migrating to a Screen is tracked but not yet done (see UI medium audit above).

**Pass:** Dashboard opens with correct layout (chest GUI today; Screen once migrated).
**Fail:** Text-only output, or no UI at all.

---

## Test G12.2 — Single block export (JSON)

In the export dashboard, click g12a → "Download JSON".

**Expected:** `Exported g12a → cloud_exports/g12a.json` with a `[download]` link in chat. File exists at `config/customblocks/cloud_exports/g12a.json`.

**Pass:** File created, chat link works.
**Fail:** Error or file missing.

---

## Test G12.3 — Single block export (PNG)

In the export dashboard, click g12a → "Download PNG".

**Expected:** `Exported g12a texture → cloud_exports/g12a.png` with `[download]` link.

**Pass:** PNG file created.
**Fail:** Error or file missing.

---

## Test G12.4 — Bulk export (all to ZIP)

In the export dashboard, click "Export All (ZIP)".

**Expected:** ZIP file created at `cloud_exports/all-YYYYMMDD-HHMMSS.zip` containing all blocks. `[download]` link in chat.

**Pass:** ZIP created with all block data.
**Fail:** Error or ZIP missing.

---

## Test G12.5 — Export category

In the export dashboard, click "Export Category" → select "exporttest".

**Expected:** ZIP with g12a and g12b data. `[download]` link in chat.

**Pass:** ZIP contains 2 blocks.
**Fail:** Error or ZIP missing/incomplete.

---

## Test G12.6 — Generate Blueprint item

In the export dashboard, click g12a → "Generate Blueprint Item".

**Expected:** A Blueprint item is given to the player. Its tooltip shows g12a's name and attributes.

**Pass:** Blueprint item in inventory with correct tooltip.
**Fail:** No item given, or generic item without block data.

---

## Test G12.7 — Share short-code and import

In the export dashboard, click g12a → "Share Short-Code".

**Expected:** `Block "g12a" shared — code: XXXXXX` (6–8 char code).

Note the code. Then:
```
/cb delete g12a
/cb importblock XXXXXX
```

**Expected:** g12a is re-imported from the vault with the same data.

**Pass:** Block imported from share code.
**Fail:** Code not generated, or import fails.

---

## Test G12.8 — Marketplace opens ⏳ planned (blocked — not built)

```
/cb market
```

**Expected (target, Screen-based):** a Screen opens with shared blocks from the vault. If empty (no uploaded blocks yet), shows a "No blocks shared yet." row/tile.

**Pass:** Marketplace Screen opens.
**Fail:** Command missing or error.

---

## Group 12 Verdict

| Test | Description | Result |
|---|---|---|
| G12.1 | Export dashboard opens (chest GUI today; Screen migration not started) | ✅ in-game (2026-06-21) — opens, but layout differs from spec (hand-pick "Bulk Choose" bundle flow, not per-slot click). Spec corrected |
| G12.2 | Single block JSON export with download link | ❌ in-game (2026-06-21) — download link broken (`ERR_CONNECTION_REFUSED`) AND leaks server host. Needs rework |
| G12.3 | Single block PNG export | 🟡 in-game (2026-06-21) — export runs, but same broken/IP-leaking download link as G12.2 |
| G12.4 | Bulk export all to ZIP | 🟡 in-game (2026-06-21) — same download-link issue as G12.2 |
| G12.5 | Export category to ZIP | 🟡 in-game (2026-06-21) — same download-link issue as G12.2 |
| G12.6 | Blueprint item generated | ✅ in-game (2026-06-21) — generates, but value questionable on same server; needs rework/rethink |
| G12.7 | Share code → import round-trip | ⏸️ DEFERRED — needs cloud vault deployed |
| G12.8 | Marketplace Screen opens | ⏸️ DEFERRED — needs cloud vault deployed; ⏳ planned as Screen-based (not built) |

**Group 12 passes when the Export Dashboard works in-game and all export/import/share paths function correctly.**

If anything shows ❌ — paste:
1. The exact action taken
2. What happened vs what was expected
3. Last 20 lines of `latest.log`

---

## Follow-ups (from in-game test 2026-06-21)

- **🔴 CROSS-CUTTING — kill IP/host-exposing download links.** Chat download buttons currently point at
  `http://<serverHost>:<httpPort>/export/<id>` (screenshot: `yoyoo.mcsh.io:8123/export/3lisy`). Two problems:
  (1) it **leaks the server address** — unacceptable for a PUBLIC mod; (2) the route is **unreachable**
  (`ERR_CONNECTION_REFUSED`) so downloads don't even work. **Decision:** remove IP/host from every chat
  download button across the mod and rethink delivery (e.g. write to a known local folder + show the path,
  or in-game delivery, or a proper opt-in public URL via the vault). Affects G12.2–.5 and any other group
  that posts a download link (e.g. G10.5 export PNG). Tracked in SWEEP_INDEX §A.
- **G12.1 spec corrected** — dashboard uses a hand-pick "Bulk Choose" bundle flow (tick blocks → pick
  format), not the per-block-slot-click sub-menu the spec described.
- **G12.6 — Blueprint rework/rethink.** Generates, but a Blueprint that only works on the same server is
  low value. Reconsider its purpose (cross-server trade? offline import?) before polishing.
- **Export system rework (17.15)** still wanted overall once delivery is fixed.

## Cleanup

```
/cb delete g12a
/cb delete g12b
```
