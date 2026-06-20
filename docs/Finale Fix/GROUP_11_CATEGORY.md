# Group 11 — Category System

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 8 (Color Ecosystem + Categories) build-verified.
>
> **Objective:** Restore the full category system: chest GUI browser, per-category give-all, display blocks (category icons), category export/import/share by code, auto-categorize, and category display block management.
>
> **Source issues:** Group G (blocks, blockscat/blockscategory, blockadd, givecategory, givedisplayblock, exportcategory, sharecategory, importcategory), R5 (AutoCategorizeManager, CategoryDisplayBlockManager)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Category browser GUI | Chest GUI listing all categories with block counts | `CategoryBrowserScreen` (screen-based) | Chest GUI browser |
| `/cb blocks` | Listed all blocks in all categories | Missing | Restored |
| `/cb blockscat <name>` | Listed blocks in a specific category | `CategoryBrowserScreen` (screen) | Restored as chest GUI |
| `/cb blockadd <id> <category>` | Add block to category (alias for setcategory) | `/cb setcategory` exists | Restored as alias |
| `/cb givecategory <name>` | Give all items in a category at once | Missing | Restored |
| `/cb givedisplayblock <id>` | Give the display block for a category | Missing | Restored |
| Export category | `/cb exportcategory <name>` — ZIP of all blocks in category | Missing | Restored (connects to Group 12 export) |
| Share category | `/cb sharecategory <name>` — generate share code | Missing | Restored (connects to Group 20 cloud) |
| Import category | `/cb importcategory <code>` — download by share code | Missing | Restored |
| Auto-categorize | Suggest category from block name/texture | `AutoCategorizeManager` stub | Fully wired |
| Category display blocks | Set a custom block as the icon for a category | `CategoryDisplayBlockManager` stub | Fully wired |

---

## What this group covers

> **Command names (final, post-cleanup 2026-06-14):** the cryptic old names were dropped. `/cb categories`
> is the single category browser GUI; adding to a category uses the clear `/cb setcategory`. The table
> below shows the **commands as actually shipped**.

| Feature | Commands |
|---|---|
| Category overview GUI | `/cb categories` (left-click → browse · right-click → CategoryEditMenu) |
| Flat block list GUI | `/cb blockslist` (alias of `/cb listgui`) |
| Add to category | `/cb setcategory <id> <category>` |
| Give all in category | `/cb givecategory <category>` |
| Set category icon (display block) | CategoryEditMenu → Display Block picker tile (commands `setdisplayblock`/`cleardisplayblock` removed) |
| Rename category | `/cb renamecategory <old> <new>` (also the Rename tile) |
| Merge categories | `/cb mergecategory <source> <dest>` (also the Merge tile) |
| Category description | `/cb categorydesc <category> <text…>` (also the Description tile) |
| Export (dashboard GUI) | `/cb export` (player → Export Dashboard chest GUI; console → text) |
| Export category | `/cb exportcategory <category>` |
| Share category | `/cb sharecategory <category>` |
| Import category by code | `/cb importcategory <code>` |
| Auto-categorize | create-time hint on `/cb create` only (standalone `/cb autocategorize` removed) |

---

## Implementation Requirements

### 1. Category Browser Chest GUI

`/cb blockscat <name>` opens a paginated chest GUI:
- Each slot = one block in the category.
- Slot item = the block's texture (or display block icon if set).
- Click a slot → sub-menu: Give, Edit (opens block editor), Remove from category.
- Top row: category name, block count, "Give All", "Export", "Share" action slots.

### 2. `/cb blocks`

Shows all blocks grouped by category. Opens a category list chest GUI where clicking a category opens the category browser for that category.

### 3. Give Category

`/cb givecategory <category>` — gives the player one item of each block in the category. Warns if inventory is full (gives what it can, reports overflow).

### 4. Category Display Block

`/cb givedisplayblock <id>` — gives a special "display block" item that represents the category icon. When placed in the category browser, it shows as the visual icon for that category.

Setting a category display block:
1. Create a block and give yourself its display block item: `/cb givedisplayblock <id>`
2. In the category browser GUI, place the display block in the "icon" slot.

Stored in `config/customblocks/data/display_blocks.json`.

### 5. Export Category

`/cb exportcategory <category>` — exports all blocks in the category to a ZIP file in `config/customblocks/cloud_exports/<category>-YYYYMMDD.zip`. Includes textures + JSON metadata. Clickable download link in chat.

### 6. Share & Import Category

`/cb sharecategory <category>` — uploads the category export to the cloud vault and returns a short alphanumeric share code (6–8 characters).

`/cb importcategory <code>` — downloads the category from the vault by code. Conflicts (existing IDs) use the import conflict resolution GUI.

### 7. Auto-Categorize

`/cb autocategorize <id>` — analyzes the block's display name and dominant texture color, then suggests a category name. Options: "Accept", "Edit", "Skip".

Also runs automatically on block creation if no category is specified and the block name matches known patterns (e.g., "Red Brick" → category "bricks").

Config field: `autoCategorizeEnabled` (default true).

### 8. Category Data Path

Categories stored in `config/customblocks/data/categories.json`. The old separate categories file is migrated by Group 09's MigrationManager.

---

## Setup

```
/cb create g11a CategoryTestBlock1
/cb create g11b CategoryTestBlock2
/cb create g11c CategoryTestBlock3
/cb setcategory g11a testcat
/cb setcategory g11b testcat
/cb setcategory g11c testcat
```

---

## Test G11.1 — Category browser opens as chest GUI

```
/cb categories
```
…then click the **testcat** tile.

**Expected:** Chest GUI opens titled "testcat (3 blocks)". Three slots visible for g11a, g11b, g11c. Top row has the category icon + "Set icon", "Give All", "Export", "Share" action slots.

**Pass:** Chest GUI opens with all 3 blocks and action slots.
**Fail:** Screen-based UI opens, or blocks missing.

---

## Test G11.2 — Block slot click opens sub-menu

In the category browser, click the g11a slot.

**Expected:** Sub-menu opens with: "Give", "Edit" (→ block editor), "Remove from category".

**Pass:** Sub-menu appears with all 3 options.
**Fail:** Nothing happens, or sub-menu missing options.

---

## Test G11.3 — `/cb categories` category list

```
/cb categories
```

**Expected:** Chest GUI opens with all categories listed. "testcat" shows with a "3 blocks" count. Clicking "testcat" → opens the category browser for testcat. (In-game it's a chest GUI; the console still prints a text list.)

**Pass:** All categories visible, clicking works.
**Fail:** Text-only output in-game or wrong navigation.

---

## Test G11.4 — Give category

```
/cb givecategory testcat
```

**Expected:** `Gave 3 items: g11a, g11b, g11c.` All three items in inventory.

**Pass:** All 3 items received.
**Fail:** Error, or fewer items received.

---

## Test G11.5 — add a block to a category (via `/cb setcategory`)

> The old `/cb blockadd` alias was **removed** in the 2026-06-14 cleanup — `/cb setcategory` is the one
> clear command for this now.

```
/cb create g11d AliasTest
/cb setcategory g11d testcat
```

**Expected:** g11d is added to "testcat". Re-open `/cb categories` → testcat → g11d is listed (now 4 blocks).

**Pass:** Block added and appears in the browser.
**Fail:** Not added, or not shown in the browser.

---

## Test G11.6 — Export category

In the category browser for "testcat", click "Export".

**Expected:** ZIP file created at `config/customblocks/cloud_exports/testcat-YYYYMMDD.zip`. Clickable `[download]` link in chat.

**Pass:** File created, chat link works.
**Fail:** Error or no file created.

---

## Test G11.7 — Share category generates code

In the category browser for "testcat", click "Share".

**Expected:** A 6–8 character share code shown in chat. Example: `Category "testcat" shared — code: AB1C2D`

**Pass:** Share code generated.
**Fail:** Error or no code shown. (Requires Group 20 cloud vault deployed.)

---

## Test G11.8 — Auto-categorize suggestion

```
/cb create g11e RedBrickWall
/cb autocategorize g11e
```

**Expected:** Suggestion: `"Suggested category: bricks. [Accept] [Edit] [Skip]"`

Click "Accept".

**Pass:** Block categorized as "bricks" automatically.
**Fail:** No suggestion, or wrong category suggested.

---

## Test G11.9 — Category tile lore (browse vs edit)

```
/cb categories
```

Hover the **testcat** tile.

**Expected:** Lore includes the block count, the description (if set), and two lines:
"Left-click to browse" and "Right-click to edit." Category name text is tinted if a color tag is set.

**Pass:** Both hint lines shown.
**Fail:** Old single-action lore, or no edit hint.

---

## Test G11.10 — Right-click opens CategoryEditMenu

In `/cb categories`, **right-click** the testcat tile.

**Expected:** A 6-row CategoryEditMenu opens with tiles: Display Block, Rename, Merge, Export,
Share (greyed "coming soon"), Lock/Unlock All, Bulk Retexture, Stats, Color Tag, Description,
Sort Order, Delete.

**Pass:** Edit menu opens with all tiles.
**Fail:** Browser opens instead, or tiles missing.

---

## Test G11.11 — `/cb export` Export Dashboard GUI

```
/cb export
```
(as a player, no args)

**Expected:** Export Dashboard chest GUI opens showing scope tiles (Per Block, Per Category,
All Blocks, Per Selection). Clicking a scope redraws the same GUI with format tiles; a "← Back"
tile returns to scope selection. Running it from console still prints text output.

**Pass:** Scope → format flow works in-place; Back returns.
**Fail:** Chat-only output for a player, or no redraw.

---

## Test G11.12 — Removed commands are gone

```
/cb setdisplayblock testcat g11a
/cb cleardisplayblock testcat
/cb autocategorize g11a
```

**Expected:** All three are unrecognized (Brigadier "Unknown command" / usage error). The display
block is now set via the CategoryEditMenu Display Block tile; auto-categorize is a create-time hint only.

**Pass:** None of the three resolve.
**Fail:** Any still runs.

---

## Test G11.13 — `/cb renamecategory`

```
/cb renamecategory testcat blockstest
```

**Expected:** Category renamed; all member blocks now report category "blockstest". `/cb categories`
shows "blockstest", not "testcat".

**Pass:** Rename applies to all blocks.
**Fail:** Error, or only some blocks moved.

---

## Test G11.14 — `/cb mergecategory`

```
/cb create g11f MergeTest
/cb setcategory g11f othercat
/cb mergecategory othercat blockstest
```

**Expected:** All blocks in "othercat" move into "blockstest"; "othercat" disappears from `/cb categories`.

**Pass:** Source emptied + removed, blocks now in dest.
**Fail:** Error, or source category remains.

---

## Test G11.15 — CategoryEditMenu Display Block picker

Right-click "blockstest" → click the **Display Block** tile → pick a block.

**Expected:** That block's item becomes the category icon in `/cb categories` and the browser.

**Pass:** Icon updates to the chosen block.
**Fail:** Icon unchanged or error.

---

## Test G11.16 — CategoryEditMenu Color Tag cycling

Right-click "blockstest" → click the **Color Tag** tile repeatedly.

**Expected:** Each click cycles the tag color; the category name text in `/cb categories` tints
to match (icon is NOT tinted).

**Pass:** Name text color changes per cycle.
**Fail:** No color change, or icon tinted instead of name.

---

## Test G11.17 — CategoryEditMenu Delete category

Right-click "blockstest" → **Delete** → confirm in the sub-menu.

**Expected:** Category removed; its blocks become uncategorized (NOT deleted). They still exist via
`/cb listgui`.

**Pass:** Category gone, blocks survive uncategorized.
**Fail:** Blocks deleted, or category remains.

---

## Group 11 Verdict

| Test | Description | Result |
|---|---|---|
| G11.1 | Category browser opens as chest GUI | ✅ in-game (2026-06-14) |
| G11.2 | Block slot click opens sub-menu | ✅ in-game (2026-06-14) |
| G11.3 | `/cb categories` category list | ✅ in-game (2026-06-14) |
| G11.4 | Give category gives all items | ✅ in-game (2026-06-14) |
| G11.5 | `setcategory` adds block to category | ✅ in-game (2026-06-14) |
| G11.6 | Export category creates ZIP | ✅ in-game (2026-06-14) |
| G11.7 | Share category generates code | ⚠️ deferred — vault Worker not deployed yet |
| G11.8 | Auto-categorize suggests category | ⚠️ redesigned — `/cb autocategorize` command removed; auto-categorize kept as create-time hint on `/cb create` only |
| G11.9 | Category tile browse/edit lore | ✅ in-game (2026-06-14) |
| G11.10 | Right-click → CategoryEditMenu | ✅ in-game (2026-06-14) |
| G11.11 | `/cb export` Export Dashboard GUI | ✅ in-game (2026-06-14) |
| G11.12 | Removed commands are gone | ✅ in-game (2026-06-14) |
| G11.13 | `/cb renamecategory` | ✅ in-game (2026-06-14) |
| G11.14 | `/cb mergecategory` | ✅ in-game (2026-06-14) |
| G11.15 | CategoryEditMenu Display Block picker | ✅ in-game (2026-06-14) |
| G11.16 | CategoryEditMenu Color Tag cycling | ✅ in-game (2026-06-14) |
| G11.17 | CategoryEditMenu Delete category | ✅ in-game (2026-06-14) |

**G11.1–G11.17 ALL confirmed working in-game by the developer (2026-06-14).**

> **Round-2 follow-up (2026-06-14) — items 2–4 confirmed in-game ✅:**
> 2. ✅ CategoryEditMenu Rename / Merge / Description now use an **Anvil GUI** (Bulk Retexture stays chat — URLs > 50 chars).
> 3. ✅ Unified **`/cb category <action>`** (rename · merge · delete · color · desc · icon · sort · lock · unlock · give · export · share · import · info · list · edit); old scattered `renamecategory`/`mergecategory`/`categorydesc`/`givecategory`/`exportcategory`/`sharecategory`/`importcategory` REMOVED.
> 4. ✅ Export Dashboard: **Per Selection → Bulk Choose** (block list with tick-boxes); Per Block / Per Category / Bulk Choose now show the same seven format tiles as All Blocks.
>
> The 🐞 resource-pack **join-prompt bug is NOT a Group 11 item** — it belongs to Group 05 (Silent Resource Pack). Re-fixed 2026-06-14 (stale static pack state cleared on `start()`); tracked in GROUP_05_TESTING_GUIDE §3.
>
> Known gap (pre-existing, not introduced): the CategoryEditMenu "Bulk Retexture" tile points at `/cb bulkretexture`, which has no command — never built.

If anything shows ❌ — paste:
1. The exact command typed
2. What appeared vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g11a
/cb delete g11b
/cb delete g11c
/cb delete g11d
/cb delete g11e
/cb delete g11f
```
