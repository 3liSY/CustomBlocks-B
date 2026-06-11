# Phase 8 Testing Tutorial — Color Ecosystem + Categories

> **Prerequisite:** Phase 7 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify what was actually built from the Phase 8 Bible spec. Most of Phase 8
> was NOT implemented — see the scope table below.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Category system | Full `CategoryManager` + `Category` objects. Categories had icons, display blocks, auto-categorize by dominant color, export/import/share by code | **String tags only.** No Category objects, no display blocks, no icons, no auto-categorize |
| Color detection | `ColorDetection.java` — extracted dominant color from any block's texture automatically | **Not built** |
| Color names | `ColorNames.java` — mapped hex colors to human names ("crimson", "slate", etc.) | **Not built** |
| Player palettes | `PlayerPaletteManager.java` — each player stored a personal color palette | **Not built** |
| Auto-categorize | `AutoCategorizeManager.java` — assigned category based on dominant texture color | **Not built** |
| Category display blocks | `CategoryDisplayBlockManager.java` — special block showing a category's icon in-world | **Not built** |
| Bulk recolor | Batch recolor with edge-mode safety, hex change wizard | **Not built** |
| BulkScope | `BulkScope.java` — scoped batch operations (by category, by query, by list) | **Not built** |
| Search | Opened a GUI search picker | Text output in chat only |

**What IS built from Phase 8:** basic string-based category assignment + search that includes category matching.

---

## What Phase 8 covers (built portion only)

| Feature | Implementation |
|---|---|
| Assign a block to a category | `setcategory` in `AttributeCommands.java` (tested in Phase 6) |
| List all categories in use | `categories` in `UtilityCommands.java` |
| Search by category name | `search <query>` in `UtilityCommands.java` (matches id, name, or category) |
| Tab-complete suggests existing categories | `setcategory` tab-complete pulls from `SlotManager.categories()` |

---

## Setup

Create a few test blocks to give categories to:
```
/cb create p8red RedBlock
/cb create p8blue BlueBlock
/cb create p8red2 RedBlock2
```

---

## Test 8.1 — Assign categories

```
/cb setcategory p8red reds
/cb setcategory p8red2 reds
/cb setcategory p8blue blues
```

**Expected:** Each confirms with `Category <id> → <cat>`.

**Pass:** All three accepted without error.
**Fail:** Command not found, or error on valid input.

---

## Test 8.2 — `/cb categories` lists all categories

```
/cb categories
```

**Expected:**
```
2 categor(ies):
 - blues (1) [list]
 - reds (2) [list]
```
Clicking `[list]` runs `/cb search <category>` and shows matching blocks.

**Pass:** Both categories appear with correct block counts. `[list]` button works.
**Fail:** Categories missing, wrong counts, or button broken.

---

## Test 8.3 — Search finds blocks by category

```
/cb search reds
```

**Expected:** `p8red` and `p8red2` both appear in results.

```
/cb search blues
```

**Expected:** `p8blue` appears.

**Pass:** Category search returns correct blocks.
**Fail:** Search returns nothing or wrong blocks.

---

## Test 8.4 — Search also matches ID and display name

```
/cb search red
```

**Expected:** All blocks whose ID or display name or category contains "red" — should return `p8red`, `p8red2`, `p8blue` (if any have "red" in name), and anything in the "reds" category.

**Pass:** Search is case-insensitive and searches all three fields.
**Fail:** Only matches exact ID.

---

## Test 8.5 — Tab-complete suggests existing categories in `setcategory`

Type (don't press Enter):
```
/cb setcategory p8red 
```
Press **Tab** after the space.

**Expected:** Suggestions include `reds` and `blues` (the categories already in use), plus `none`.

**Pass:** Existing categories appear as suggestions.
**Fail:** No suggestions, or only `none`.

---

## Test 8.6 — Clear category with `none`

```
/cb setcategory p8blue none
```

**Expected:** `p8blue → uncategorized`

Then:
```
/cb categories
```

**Expected:** `blues` is gone (no blocks in it). Only `reds` remains.

**Pass:** Category cleared, and empty category disappears from the list.
**Fail:** `blues` still shows with 0 blocks, or error on `none`.

---

## Phase 8 Verdict

| Test | Description | Result |
|---|---|---|
| 8.1 | `setcategory` assigns categories | ✅ |
| 8.2 | `categories` lists all with counts + working [list] button | ✅ |
| 8.3 | `search` finds blocks by category | ✅ |
| 8.4 | `search` also matches ID and display name | ✅ |
| 8.5 | `setcategory` tab-complete suggests existing categories | ✅ |
| 8.6 | `setcategory none` clears, empty categories disappear | ✅ |

**When all 6 show ✅ — say "Phase 8 passes" and we move to Phase 9.**

> **Note:** The unbuilt Phase 8 features (ColorDetection, ColorNames, PlayerPaletteManager,
> AutoCategorize, CategoryDisplayBlock, BulkScope, hex change wizard) are logged in
> `PHASE17_RESTORATION.md` Group E and Group G for later restoration discussion.

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p8red
/cb delete p8red2
/cb delete p8blue
```
