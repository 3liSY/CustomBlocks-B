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

| Feature | Commands |
|---|---|
| Block list with categories | `/cb blocks` |
| Category browser | `/cb blockscat <name>` (alias: `/cb blockscategory`) |
| Add to category | `/cb blockadd <id> <category>` |
| Give all in category | `/cb givecategory <category>` |
| Set category display block | `/cb givedisplayblock <id>` |
| Export category | `/cb exportcategory <category>` |
| Share category | `/cb sharecategory <category>` |
| Import category by code | `/cb importcategory <code>` |
| Auto-categorize | `/cb autocategorize <id>` (or auto on create) |
| Category list | `/cb categories` |

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
/cb blockscat testcat
```

**Expected:** Chest GUI opens titled "testcat (3 blocks)". Three slots visible for g11a, g11b, g11c. Top row has "Give All", "Export", "Share" action slots.

**Pass:** Chest GUI opens with all 3 blocks and action slots.
**Fail:** Screen-based UI opens, or blocks missing.

---

## Test G11.2 — Block slot click opens sub-menu

In the category browser, click the g11a slot.

**Expected:** Sub-menu opens with: "Give", "Edit" (→ block editor), "Remove from category".

**Pass:** Sub-menu appears with all 3 options.
**Fail:** Nothing happens, or sub-menu missing options.

---

## Test G11.3 — `/cb blocks` category list

```
/cb blocks
```

**Expected:** Chest GUI opens with all categories listed. "testcat" shows with count "(3)". Clicking "testcat" → opens category browser for testcat.

**Pass:** All categories visible, clicking works.
**Fail:** Text-only output or wrong navigation.

---

## Test G11.4 — Give category

```
/cb givecategory testcat
```

**Expected:** `Gave 3 items: g11a, g11b, g11c.` All three items in inventory.

**Pass:** All 3 items received.
**Fail:** Error, or fewer items received.

---

## Test G11.5 — blockadd alias

```
/cb create g11d AliasTest
/cb blockadd g11d testcat
```

**Expected:** `g11d added to category "testcat".` Same behavior as `/cb setcategory`.

**Pass:** Block added to category.
**Fail:** Command missing.

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

## Group 11 Verdict

| Test | Description | Result |
|---|---|---|
| G11.1 | Category browser opens as chest GUI | ⬜ |
| G11.2 | Block slot click opens sub-menu | ⬜ |
| G11.3 | `/cb blocks` shows category list | ⬜ |
| G11.4 | Give category gives all items | ⬜ |
| G11.5 | blockadd is alias for setcategory | ⬜ |
| G11.6 | Export category creates ZIP | ⬜ |
| G11.7 | Share category generates code | ⬜ |
| G11.8 | Auto-categorize suggests category | ⬜ |

**Group 11 passes when all category features work correctly in-game.**

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
```
