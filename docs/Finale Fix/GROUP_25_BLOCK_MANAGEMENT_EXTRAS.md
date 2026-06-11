# Group 25 — Block Management Extras & Identity Operations

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 3 (Core Commands) and Phase 7 (Tools) build-verified.
>
> **Objective:** Restore all block identity manipulation commands (reid, swapid, swapname, duplicate alias) and all block management extras (custom drops, Block Finder GUI, HD PNG export from editor, tab icon). All operations must be undoable and tab-complete correctly.
>
> **Source issues:** Group K (reid, swapid, swapname, duplicate alias), R2 (DropConfigManager), R4 (BlockFinder GUI + export PNG), Group M (settabicon)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

### Identity Operations

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| `/cb reid` | Rename block ID without changing display name | Missing | Restored |
| `/cb swapid` | Swap IDs of two blocks | Missing | Restored |
| `/cb swapname` | Swap display names of two blocks | Missing | Restored |
| `/cb duplicate` | Alias for `/cb dupe` | Missing (only `/cb dupe`) | Restored |

### Block Management Extras

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| Custom drops | `DropConfigManager` — blocks drop custom items | `DropConfigManager` stub | Fully wired |
| Block Finder | `BlockFinder` — find placed instances in world | Missing | Chest GUI |
| PNG export from editor | Not in editor GUI | Group 10 has `/cb exportpng` cmd | Also as button in Block Editor GUI |
| `/cb settabicon` | Set custom creative tab icon from URL | Missing | Restored |
| Drop config slot | Not in editor | Not in editor | Added to Block Editor chest GUI |
| Drop persistence | `config/customblocks/data/drop_config.json` | Not present | Restored |

---

## What this group covers

| Feature | Commands |
|---|---|
| Re-ID | `/cb reid <old-id> <new-id>` |
| Swap IDs | `/cb swapid <id1> <id2>` |
| Swap names | `/cb swapname <id1> <id2>` |
| Duplicate alias | `/cb duplicate <id> [new-id]` |
| Set custom drop | `/cb setdrop <id> <item-id> [amount]` |
| Clear custom drop | `/cb cleardrop <id>` |
| Block Finder | `/cb find <id>` |
| Set tab icon | `/cb settabicon <url>` |
| Export PNG (editor) | Button in Block Editor chest GUI |

---

## Implementation Requirements

### 1. `/cb reid <old-id> <new-id>`

Changes block ID without touching display name, texture, or attributes. `<new-id>` must not already exist. All placed world instances update automatically. Undoable.

Errors: "ID 'newid' is already taken." / "No block with ID 'oldid'."

### 2. `/cb swapid <id1> <id2>`

Swaps IDs of two blocks. Display names, textures, and attributes stay with their original blocks. World placements update. Undoable as single atomic action.

### 3. `/cb swapname <id1> <id2>`

Swaps display names only. IDs and all attributes unchanged. Undoable.

### 4. `/cb duplicate <id> [new-id]`

Alias for `/cb dupe`. Same behavior. Auto-generates `<id>_copy` if no `[new-id]` given (increments to `_copy_2` etc. on collision).

### 5. Custom Drop System

`/cb setdrop <id> <minecraft-item-id> [amount]` — configures the block's drop on break.

Example: `/cb setdrop g28a diamond 3` — block drops 3 diamonds when broken.

Default (no custom drop set): block drops nothing.

Custom drops override vanilla drop behavior. Undoable.

`/cb cleardrop <id>` — removes custom drop, block drops nothing again.

**Drop config slot in Block Editor:** In the Block Editor chest GUI (Group 02), a "Custom Drop" slot shows the current drop item (empty glass if none). Click → anvil GUI to set item ID and amount.

**Persistence:** `config/customblocks/data/drop_config.json`

### 6. Block Finder GUI

`/cb find <id>` — scans all loaded chunks async. Opens a chest GUI:
- Each slot = one placed instance. Hover: coordinates (X, Y, Z), dimension, distance from player.
- Click → teleport to that location (admin/OP 4 permission required).
- "Refresh" slot re-scans. "Total count" slot shows total found.
- Results paginated in groups of 27.

### 7. Export PNG from Block Editor

Block Editor chest GUI gets an "Export PNG" slot. Click → saves `config/customblocks/cloud_exports/<id>.png` and provides a `[download]` chat link. Same as `/cb exportpng <id>` from Group 10, accessible directly from the editor.

### 8. `/cb settabicon`

`/cb settabicon <url>` — downloads the image and uses it as the icon for the "CustomBlocks" blocks creative tab. Stored at `config/customblocks/textures/tab_icon.png`. Reapplies on restart.

---

## Setup

```
/cb create g28a DropTest
/cb create g28b IdentityA
/cb create g28c IdentityB
/cb create g28d FinderTest
```

Place `g28d` in at least 3 different locations in the world.

---

## Test G28.1 — Reid changes ID only

```
/cb reid g28b g28b_renamed
```

**Expected:** `Block "g28b" renamed to ID "g28b_renamed". Display name unchanged: "IdentityA".`

`/cb list` — `g28b` gone, `g28b_renamed` with name "IdentityA".

**Pass:** ID changed, name preserved.
**Fail:** Error, or display name also changed.

---

## Test G28.2 — Reid conflict check

```
/cb reid g28c g28d
```

**Expected:** `ID "g28d" is already taken. Choose a different ID.`

**Pass:** Conflict rejected cleanly.
**Fail:** Error/crash or silent swap.

---

## Test G28.3 — Reid is undoable

```
/cb undo
```

**Expected:** `g28b_renamed` reverts to ID `g28b`.

**Pass:** Undo restores original ID.
**Fail:** Undo doesn't affect ID changes.

---

## Test G28.4 — Swap IDs

```
/cb swapid g28b g28c
```

**Expected:**
- `g28b` now has display name "IdentityB".
- `g28c` now has display name "IdentityA".
- IDs are swapped, names stayed with blocks.

**Pass:** IDs swapped, names with blocks.
**Fail:** Error or display names also swapped.

---

## Test G28.5 — Swap IDs is undoable

```
/cb undo
```

**Expected:** `g28b` = "IdentityA", `g28c` = "IdentityB".

**Pass:** Single undo reverts swap.
**Fail:** Undo doesn't work on swaps.

---

## Test G28.6 — Swap names

```
/cb swapname g28b g28c
```

**Expected:** `g28b` displays "IdentityB". `g28c` displays "IdentityA". IDs unchanged.

**Pass:** Names swapped, IDs unchanged.
**Fail:** IDs also swapped.

---

## Test G28.7 — Swap names is undoable

```
/cb undo
```

**Expected:** Names revert — `g28b` = "IdentityA", `g28c` = "IdentityB".

**Pass:** Undo restores names.
**Fail:** Undo doesn't work on name swaps.

---

## Test G28.8 — Duplicate alias

```
/cb duplicate g28b
```

**Expected:** Creates `g28b_copy` with identical attributes.

```
/cb duplicate g28c my_custom_copy
```

**Expected:** Creates `my_custom_copy`.

**Pass:** Both forms work.
**Fail:** Command not found.

---

## Test G28.9 — Set custom drop

```
/cb setdrop g28a diamond 2
```

**Expected:** `Custom drop for "g28a" set to: diamond ×2.`

**Pass:** Drop configured.
**Fail:** Command missing.

---

## Test G28.10 — Custom drop fires on break

Give `g28a`, place it, break it.

**Expected:** 2 diamonds drop.

**Pass:** Correct drops.
**Fail:** No drops or wrong item.

---

## Test G28.11 — Drop persists after restart

Restart server. Break another placed `g28a`.

**Expected:** Still 2 diamonds.

**Pass:** Drop config persisted.
**Fail:** No drops after restart.

---

## Test G28.12 — Clear custom drop

```
/cb cleardrop g28a
```

Place and break `g28a`.

**Expected:** No drops.

**Pass:** Drop removed.
**Fail:** Drops still fire.

---

## Test G28.13 — Drop slot in Block Editor

```
/cb editor g28a
```

**Expected:** "Custom Drop" slot visible in Block Editor GUI. Click → anvil GUI to set item ID and amount.

**Pass:** Slot present, anvil opens.
**Fail:** No drop slot.

---

## Test G28.14 — Block Finder GUI

```
/cb find g28d
```

**Expected:** Chest GUI with ≥3 placement slots. Hover shows coordinates + distance. Total count slot shows total.

**Pass:** GUI with placements found.
**Fail:** Text output only, or empty GUI.

---

## Test G28.15 — Block Finder teleport

Click one placement slot.

**Expected:** Player teleports to that block's location.

**Pass:** Teleport fires.
**Fail:** Nothing on click.

---

## Test G28.16 — Export PNG from Block Editor

```
/cb editor g28a
```

Click "Export PNG" slot.

**Expected:** `Texture exported → cloud_exports/g28a.png` with `[download]` link.

**Pass:** File created, link in chat.
**Fail:** Slot missing or export fails.

---

## Test G28.17 — Set tab icon

```
/cb settabicon https://i.imgur.com/example.png
```

**Expected:** `Creative tab icon updated.` CustomBlocks creative tab shows custom icon.

**Pass:** Tab icon changed.
**Fail:** Command missing or icon unchanged.

---

## Group 25 Verdict

| Test | Description | Result |
|---|---|---|
| G28.1 | Reid changes ID, preserves name | ⬜ |
| G28.2 | Reid rejects conflicting ID | ⬜ |
| G28.3 | Reid is undoable | ⬜ |
| G28.4 | Swap IDs — names stay with blocks | ⬜ |
| G28.5 | Swap IDs is undoable | ⬜ |
| G28.6 | Swap names — IDs unchanged | ⬜ |
| G28.7 | Swap names is undoable | ⬜ |
| G28.8 | Duplicate alias (auto and custom ID) | ⬜ |
| G28.9 | Custom drop configured | ⬜ |
| G28.10 | Custom drop fires on break | ⬜ |
| G28.11 | Drop persists after restart | ⬜ |
| G28.12 | Clear drop removes drops | ⬜ |
| G28.13 | Drop slot in Block Editor GUI | ⬜ |
| G28.14 | Block Finder GUI shows placements | ⬜ |
| G28.15 | Finder teleports to location | ⬜ |
| G28.16 | Export PNG from Block Editor | ⬜ |
| G28.17 | Tab icon updated via URL | ⬜ |

**Group 25 passes when all identity operations and block management extras work in-game.**

If anything shows ❌ — paste:
1. Exact command typed
2. What happened vs expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g28a
/cb delete g28b
/cb delete g28b_renamed
/cb delete g28c
/cb delete g28d
/cb delete g28b_copy
/cb delete my_custom_copy
```
(Break any placed `g28d` instances in the world.)
