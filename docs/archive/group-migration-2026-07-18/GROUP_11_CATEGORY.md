# Group 11 — Category System

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 8 (Color Ecosystem + Categories) build-verified.
>
> **Objective:** Restore the full category system: Screen-based browser (`CategoryHubScreen`, see audit note
> below), per-category give-all, display blocks (category icons), category export/import/share by code,
> auto-categorize, and category display block management.
>
> **Source issues:** Group G (blocks, blockscat/blockscategory, blockadd, givecategory, givedisplayblock, exportcategory, sharecategory, importcategory), R5 (AutoCategorizeManager, CategoryDisplayBlockManager)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## UI medium audit (2026-07-09)

Confirmed via code: `CategoryHubScreen` (Group 27) already does everything CategoryListMenu +
CategoryBrowserMenu + CategoryEditMenu do (list, rename, merge, move, colour, lock, description, blocks)
— it was just never wired to replace them, same drift pattern as the G10 mix-up. Decision: **retire the
old chest menus**, `/cb categories` should open CategoryHubScreen instead (code work not done yet).
CategoryBlockMenu's 3 actions (Give/Edit/Remove) fold into the Hub's BLOCKS section. ExportDashboardMenu →
**Screen**, not built, unrelated to the Hub overlap — its own 2-phase scope→format flow.

## Locked Rework Direction (2026-06-30)

Status: **first cramped `/cb create` Category tab sample rejected in-game; wide workspace sample built next; needs polishing after in-game test**.

The current shipped G11 chest/category system remains the verified baseline. The next G11 direction is a full replacement category system, first prototyped as:

- `docs/mockups/category_create_tab_v2.html` — accepted direction for the `/cb create` Category tab.
- `docs/mockups/category_system_rework.html` — earlier exploration; rejected as too compact/scattered, kept only as reference.

Ownership:

- **G11 owns** category data, category rules, multi-category assignment, category tree behavior, category commands, migration, category browser/manager, delete behavior, and category import/export metadata.
- **G27/Create Studio owns** the host screen surface where this appears as the left-side **Category** tab inside `/cb create`.
- **G12/G20 cross-link** only for export/import/cloud transport. The category model and style payload still belong to G11.

Locked decisions:

- Blocks may belong to **multiple categories**.
- Each block has exactly **one main category** for the visible badge/icon display.
- Categories may start **empty** or may be created while adding a block.
- Category creation must exist in both **commands and GUI**.
- `/cb create` gets a left-side **Category** tab. The tab must blend Minecraft-style preview/slots with a modern advanced editor.
- The old/current categories must migrate automatically into real category records. No existing category or block assignment may disappear.
- Subcategories are supported as an **unlimited tree**, but the UI should warn that more than two sub-levels is not recommended.
- Parent category views include blocks from child categories.
- The player may choose either a guided wizard or a full advanced editor.
- Right-click, shift-click, and modifier-click shortcuts are part of the design and must be shown in item/category lore.
- Only the main category badge is visible by default. Extra memberships are for browsing/filtering unless a later setting says otherwise.
- Category customization should be advanced, in-game, and exportable: name/key, parent, icon source, custom block icon, vanilla item icon, color/accent, badge text, badge color/style, description, tile/template style, sort order, hidden/locked state, permissions, sounds, particles, auto-add rules, and import/export payload.
- Category templates are in scope.
- Reordering is in scope.
- Category export/import must include category style/customization, tree data, assignments, and blocks where applicable.
- Category delete must present separate clear modes:
  - Delete category only; blocks survive, blocks with no remaining category become uncategorized.
  - Delete category plus only blocks that belong exclusively to that category/tree.
  - Delete category plus all blocks shown in that category tree, even if shared elsewhere.
  - Move blocks to another category, then delete the category.

Implementation gate:

- Do **not** replace the current in-game category system from this mockup alone.
- The first narrow in-game sample was rejected as cramped and visually bad.
- The replacement sample is now a wider `/cb create` Category workspace (`StudioCategoryWorkspacePanel`) that uses the main canvas instead of the skinny left panel.
- `/cb category` and `/cb categories` are still the old flows; replacing those belongs to the next G11 slice before full migration can be considered coherent.
- After owner tests the wide sample in game, polish layout/wording/shortcuts/delete flow before full migration.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Category browser GUI | Chest GUI listing all categories with block counts | `CategoryBrowserScreen` (screen-based) | Retire chest menus, route to `CategoryHubScreen` |
| `/cb blocks` | Listed all blocks in all categories | Missing | Restored |
| `/cb blockscat <name>` | Listed blocks in a specific category | `CategoryBrowserScreen` (screen) | Restored via `CategoryHubScreen` (Screen, not chest) |
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

> **Command names (final — UPDATED on sweep 2026-06-21):** the cryptic old names were dropped, then in the
> Round-2 cleanup the scattered category verbs were **folded into one unified `/cb category <action>`**
> command (verified in code: `CategoryCommands.java` registers only `literal("category")`). The standalone
> `renamecategory` / `mergecategory` / `givecategory` / `exportcategory` / `sharecategory` / `importcategory` /
> `categorydesc` are **GONE**. `/cb categories` remains the browser GUI; adding to a category still uses
> `/cb setcategory`. Test bodies below show the old forms historically (they passed pre-unification) — the
> shipped equivalents are the `/cb category …` subcommands.

| Feature | Command (as shipped) |
|---|---|
| Category overview GUI | `/cb categories` (left-click → browse · right-click → CategoryEditMenu) |
| Flat block list GUI | `/cb blockslist` (alias of `/cb listgui`) |
| Add to category | `/cb setcategory <id> <category>` |
| Give all in category | `/cb category give <category>` |
| Set category icon (display block) | CategoryEditMenu → Display Block picker tile (commands `setdisplayblock`/`cleardisplayblock` removed) |
| Rename category | `/cb category rename <old> <new>` (also the Rename tile) |
| Merge categories | `/cb category merge <source> <dest>` (also the Merge tile) |
| Category description | `/cb category desc <category> <text…>` (also the Description tile) |
| Export (dashboard GUI) | `/cb export` (player → Export Dashboard chest GUI; console → text) |
| Export category | `/cb category export <category>` |
| Share category | `/cb category share <category>` |
| Import category by code | `/cb category import <code>` |
| Auto-categorize | create-time hint on `/cb create` only (standalone `/cb autocategorize` removed) |

---

## Implementation Requirements

### 1. Category Browser (`CategoryHubScreen`) — moved to G27

`/cb blockscat <name>` and `/cb blocks` open `CategoryHubScreen` — full spec moved to
`GROUP_27_SCREENS.md` §G27.29 (2026-07-12). G11 keeps the give/export/display-block/import commands below.

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

## Test G11.1 — Category browser opens as `CategoryHubScreen` ⏳ (blocked — Screen not built)

> ⚠️ **UPDATED 2026-07-10:** category browser moved to `CategoryHubScreen` Screen (audit note above),
> replacing the old chest menus. Expected/Pass/Fail flipped; the old chest browser is now the FAIL. The
> 2026-06-14 ✅ in Verdict predates this decision — retest needed once the Screen ships.

```
/cb categories
```
…then click the **testcat** tile.

**Expected:** `CategoryHubScreen` opens titled "testcat (3 blocks)". Three rows visible for g11a, g11b, g11c. Top has the category icon + "Set icon", "Give All", "Export", "Share" controls.

**Pass:** Screen opens with all 3 blocks and controls.
**Fail:** Old chest GUI opens, or blocks missing.

---

## Test G11.2 — Block row click opens sub-menu

In `CategoryHubScreen`, click the g11a row.

**Expected:** Sub-menu opens with: "Give", "Edit" (→ block editor), "Remove from category".

**Pass:** Sub-menu appears with all 3 options.
**Fail:** Nothing happens, or sub-menu missing options.

---

## Test G11.3 — `/cb categories` category list ⏳ (blocked — Screen not built)

> ⚠️ **UPDATED 2026-07-10:** predates the Screen decision — retest once `CategoryHubScreen` ships.

```
/cb categories
```

**Expected:** `CategoryHubScreen` opens with all categories listed. "testcat" shows with a "3 blocks" count. Clicking "testcat" → opens the category browser for testcat. (Console still prints a text list.)

**Pass:** All categories visible, clicking works.
**Fail:** Old chest GUI opens, text-only output in-game, or wrong navigation.

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

> ⚠️ **Synced 2026-07-10:** `docs/testing/GROUP_11_TESTING_GUIDE.md` reverted sections C/D/E to
> needs-testing on 2026-07-05 ("stale, needs retest") — this Verdict table's blanket ✅ rows below were
> never updated to reflect that revert, plus G11.1/.3 now target `CategoryHubScreen` Screen, not chest
> (see tests above). Treat rows here as historical, not current status.

| Test | Description | Result |
|---|---|---|
| G11.1 | Category browser opens as `CategoryHubScreen` (was: chest GUI) | ⏳ stale ✅ (2026-06-14) — predates 2026-07-10 Screen decision, retest once built |
| G11.2 | Block row click opens sub-menu | ✅ in-game (2026-06-14) |
| G11.3 | `/cb categories` category list | ⏳ stale ✅ (2026-06-14) — predates 2026-07-10 Screen decision, retest once built |
| G11.4 | Give category gives all items | ✅ in-game (2026-06-14) |
| G11.5 | `setcategory` adds block to category | ✅ in-game (2026-06-14) |
| G11.6 | Export category creates ZIP | 🟡 ZIP write ✅ (2026-06-14); **`[download]` link host-leaks + unreachable — same cross-cutting fix as G12 (§A), `getZipUrl()`** |
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

## Follow-ups (sweep 2026-06-21)

- **Command names updated** — scattered category verbs unified into `/cb category <action>` (verified: code
  registers only `literal("category")`). Covers table corrected; test bodies left as historical old forms.
- **🔴 G11.6 export link** — category ZIP posts a `[download]` link via `ResourcePackServer.getZipUrl()` —
  same host-leak/unreachable bug as G12.2–.5 and G10.5. Fix together; remove IP/host (SWEEP_INDEX §A).
- **Known gap (carried)** — CategoryEditMenu "Bulk Retexture" tile points at `/cb bulkretexture`, which was
  never built (no such literal in code). Either build it or remove the tile.
- **Ownership** — G11 owns `categories`, `category <action>`, `setcategory`, `blockslist`, CategoryEditMenu,
  display blocks, auto-categorize hint. Export/share ZIP delivery shares G12's download-link fix.

## Cleanup

```
/cb delete g11a
/cb delete g11b
/cb delete g11c
/cb delete g11d
/cb delete g11e
/cb delete g11f
```
