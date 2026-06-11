# Group 12 — Export Dashboard & Marketplace

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 20 (External Integrations) verified. Phase 9 (Import/Export) build-verified.
>
> **Objective:** Redesign the entire export system into a robust Export Dashboard chest GUI. Add advanced export formats (litematic, schem, vanilla resource pack). Restore marketplace browsing and block sharing via short codes.
>
> **Source issues:** 17.15 (export system rework), Group H (sharecategory, importcategory, exportblock, importblock, market), Decision §11 (Universal Export Dashboard), Decision §H (litematic + schem + standalone vanilla resource pack)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Export GUI | `/cb export json/txt` (text-only output) | Text-only, no GUI | Full Export Dashboard chest GUI |
| Export formats | JSON, TXT | JSON | JSON, TXT, PNG, CSV, NBT, ZIP, litematic, schem, vanilla resource pack |
| Localhost download links | Not present | Not present | HTTP route generates clickable chat link |
| Share single block | `/cb exportblock <id>` → share code | Missing | Restored |
| Import single block | `/cb importblock <code>` | Missing | Restored |
| Marketplace | `/cb market` — browse shared blocks from others | Missing | Restored |
| Bulk export to vault | Not present | Not present | New: "Vault Sync" bulk action |
| Cloud exports folder | `config/customblocks/cloud_exports/` | Uses `exports/` | Updated to `cloud_exports/` |
| Blueprint item | Not present | Not present | New: physical in-game Blueprint item generated from block |

---

## What this group covers

| Feature | Commands |
|---|---|
| Export dashboard | `/cb export` (opens chest GUI) |
| Export single block | `/cb exportblock <id>` |
| Import single block | `/cb importblock <code>` |
| Bulk export all | From export GUI |
| Export by category | From export GUI |
| Vault sync | From export GUI |
| Marketplace browse | `/cb market` (alias: `/cb marketplace`) |
| Download link | Clickable `[download]` link in chat |

---

## Implementation Requirements

### 1. Export Dashboard Chest GUI

`/cb export` opens the Export Dashboard chest GUI:

**Top row — Bulk Actions:**
| Slot | Action |
|---|---|
| Export All (ZIP) | Exports all blocks to a ZIP file |
| Export Category | Opens category selector, exports that category |
| Vault Sync | Uploads all blocks to the Cloud Vault |

**Middle rows — Block List:**
- Each block in the registry gets a slot.
- Click a block slot → opens Single Block Export sub-menu.

**Single Block Export sub-menu:**
| Option | Description |
|---|---|
| Download PNG | Saves texture PNG to `cloud_exports/` |
| Download JSON | Saves block metadata JSON |
| Download CSV | Saves as CSV row |
| Download NBT | Saves raw `.nbt` data |
| Export Full Block (ZIP) | Texture + JSON + metadata in one ZIP |
| Generate Blueprint Item | Creates a physical Blueprint item in inventory |
| Share Short-Code | Uploads to vault, generates share code in chat |

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

### 5. Marketplace

`/cb market` opens the Marketplace chest GUI:
- Fetches listings from the cloud vault worker.
- Each slot = one shared block from any player. Hover shows: ID, name, uploader, texture preview.
- Click → "Import this block" (creates locally if no ID conflict).
- Filter by category, color, or uploader name.

### 6. Import by Code

`/cb importblock <code>` — downloads a block from the vault by its 6–8 character share code. If a block with the same ID already exists: opens the Import Conflict chest GUI (keep existing / overwrite / rename incoming).

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

## Test G12.1 — Export dashboard opens as chest GUI

```
/cb export
```

**Expected:** Chest GUI opens. Top row has "Export All", "Export Category", "Vault Sync" slots. Middle rows show all registered blocks.

**Pass:** Chest GUI opens with correct layout.
**Fail:** Text-only output, or screen-based UI.

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

## Test G12.8 — Marketplace opens

```
/cb market
```

**Expected:** Chest GUI opens with shared blocks from the vault. If empty (no uploaded blocks yet), shows "No blocks shared yet." message slot.

**Pass:** Marketplace GUI opens.
**Fail:** Command missing or error.

---

## Group 12 Verdict

| Test | Description | Result |
|---|---|---|
| G12.1 | Export dashboard opens as chest GUI | ⬜ |
| G12.2 | Single block JSON export with download link | ⬜ |
| G12.3 | Single block PNG export | ⬜ |
| G12.4 | Bulk export all to ZIP | ⬜ |
| G12.5 | Export category to ZIP | ⬜ |
| G12.6 | Blueprint item generated | ⬜ |
| G12.7 | Share code → import round-trip | ⬜ |
| G12.8 | Marketplace GUI opens | ⬜ |

**Group 12 passes when the Export Dashboard works in-game and all export/import/share paths function correctly.**

If anything shows ❌ — paste:
1. The exact action taken
2. What happened vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g12a
/cb delete g12b
```
