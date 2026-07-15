# Group 25 — Block Management Extras & Identity Operations

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 3 (Core Commands) and Phase 7 (Tools) build-verified.
>
> **Objective:** Restore all block identity manipulation commands (reid, swapid, swapname, duplicate alias) and all block management extras (custom drops, Block Finder GUI, HD PNG export from editor, tab icon). All operations must be undoable and tab-complete correctly.
>
> **Source issues:** Group K (reid, swapid, swapname, duplicate alias), R2 (DropConfigManager), R4 (BlockFinder GUI + export PNG), Group M (settabicon)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## UI medium audit (2026-07-09)

ReIdMenu (the "change a block's id" GUI, picker + anvil) → **Screen, not built.** Real text field instead
of the anvil-rename workaround. See `docs/UI_MEDIUM_GUIDE.md`.

## 🐞→🟢 FIXED 2026-06-27 (G25-2, awaiting in-game) — `/cb reid` failed on a case-mismatched old id

`/cb reid sfa1 sfa2` errored *"Couldn't change the id…"* when the stored id differed only in case from what
you typed (stored `Sfa1`, typed `sfa1`). The pre-check resolved case-insensitively (`SlotManager.getById`) but
the rename passed the **typed** old id to `SlotManager.reId`, whose `BY_ID.get(oldId)` is **exact** → null.

- **Fix:** `ReIdCommands.reid` now resolves the stored id once (`before.customId()`) and uses it for the lock
  check, the `reId` call, and all messages; a pure case-change of the same id is allowed too. `reId` itself
  kept exact (the command feeds it the exact id). Same family as Group 26 FIX B — this path just missed it.

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
| Set tab icon | `/cb settabicon <url>` (owner = G25, decision C 2026-06-21; removed from G06) |
| Export PNG (editor) | Button in Block Editor chest GUI |

---

## Consolidated ownership (sweep 2026-06-21)

These features were **specced in other group docs but belong here** per SWEEP_INDEX §A/§B. Ownership moved to
G25; the listed docs keep only a cross-ref. (Specs not duplicated — see the source doc for original detail.)

| Feature | Commands | Moved from | Notes |
|---|---|---|---|
| Magic items | `/cb magicitems`, `/cb editmagicitems` | G02 | RESTORE; `editmagicitems` gets a **full revamp** (not a port) — §A |
| Block edit history | `/cb history` | G02 | G02 built the chest-GUI surface; the mutation-log **feature** is owned here (decision A) |
| Favorites | `/cb favorite <id>` (primary), `/cb fav` (alias), `/cb unfavorite <id>` | G17 | Feature owned here. Fully moved out of G17 2026-06-21 (former tests G17.6–G17.8 land here). Currently only `/cb fav` toggle + `/cb favs` exist in code — `favorite`/`unfavorite` not built yet. |
| Recent blocks | `/cb recent` | G17 | Recently-used list (text + chest GUI link). Fully moved out of G17 2026-06-21 (former test G17.12). Not built in code yet. |

> ⚠️ These four need their full test specs written into G25 when they're built (currently only stubs/notes
> elsewhere). Do **not** assume they're tested — they inherit ⬜ until in-game confirmed.

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

Example: `/cb setdrop g25a diamond 3` — block drops 3 diamonds when broken.

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
/cb create g25a DropTest
/cb create g25b IdentityA
/cb create g25c IdentityB
/cb create g25d FinderTest
```

Place `g25d` in at least 3 different locations in the world.

---

## Test G25.1 — Reid changes ID only

```
/cb reid g25b g25b_renamed
```

**Expected:** `Block "g25b" renamed to ID "g25b_renamed". Display name unchanged: "IdentityA".`

`/cb list` — `g25b` gone, `g25b_renamed` with name "IdentityA".

**Pass:** ID changed, name preserved.
**Fail:** Error, or display name also changed.

---

## Test G25.2 — Reid conflict check

```
/cb reid g25c g25d
```

**Expected:** `ID "g25d" is already taken. Choose a different ID.`

**Pass:** Conflict rejected cleanly.
**Fail:** Error/crash or silent swap.

---

## Test G25.3 — Reid is undoable

```
/cb undo
```

**Expected:** `g25b_renamed` reverts to ID `g25b`.

**Pass:** Undo restores original ID.
**Fail:** Undo doesn't affect ID changes.

---

## Test G25.4 — Swap IDs

```
/cb swapid g25b g25c
```

**Expected:**
- `g25b` now has display name "IdentityB".
- `g25c` now has display name "IdentityA".
- IDs are swapped, names stayed with blocks.

**Pass:** IDs swapped, names with blocks.
**Fail:** Error or display names also swapped.

---

## Test G25.5 — Swap IDs is undoable

```
/cb undo
```

**Expected:** `g25b` = "IdentityA", `g25c` = "IdentityB".

**Pass:** Single undo reverts swap.
**Fail:** Undo doesn't work on swaps.

---

## Test G25.6 — Swap names

```
/cb swapname g25b g25c
```

**Expected:** `g25b` displays "IdentityB". `g25c` displays "IdentityA". IDs unchanged.

**Pass:** Names swapped, IDs unchanged.
**Fail:** IDs also swapped.

---

## Test G25.7 — Swap names is undoable

```
/cb undo
```

**Expected:** Names revert — `g25b` = "IdentityA", `g25c` = "IdentityB".

**Pass:** Undo restores names.
**Fail:** Undo doesn't work on name swaps.

---

## Test G25.8 — Duplicate alias

```
/cb duplicate g25b
```

**Expected:** Creates `g25b_copy` with identical attributes.

```
/cb duplicate g25c my_custom_copy
```

**Expected:** Creates `my_custom_copy`.

**Pass:** Both forms work.
**Fail:** Command not found.

---

## Test G25.9 — Set custom drop

```
/cb setdrop g25a diamond 2
```

**Expected:** `Custom drop for "g25a" set to: diamond ×2.`

**Pass:** Drop configured.
**Fail:** Command missing.

---

## Test G25.10 — Custom drop fires on break

Give `g25a`, place it, break it.

**Expected:** 2 diamonds drop.

**Pass:** Correct drops.
**Fail:** No drops or wrong item.

---

## Test G25.11 — Drop persists after restart

Restart server. Break another placed `g25a`.

**Expected:** Still 2 diamonds.

**Pass:** Drop config persisted.
**Fail:** No drops after restart.

---

## Test G25.12 — Clear custom drop

```
/cb cleardrop g25a
```

Place and break `g25a`.

**Expected:** No drops.

**Pass:** Drop removed.
**Fail:** Drops still fire.

---

## Test G25.13 — Drop slot in Block Editor

```
/cb editor g25a
```

**Expected:** "Custom Drop" slot visible in Block Editor GUI. Click → anvil GUI to set item ID and amount.

**Pass:** Slot present, anvil opens.
**Fail:** No drop slot.

---

## Test G25.14 — Block Finder GUI

```
/cb find g25d
```

**Expected:** Chest GUI with ≥3 placement slots. Hover shows coordinates + distance. Total count slot shows total.

**Pass:** GUI with placements found.
**Fail:** Text output only, or empty GUI.

---

## Test G25.15 — Block Finder teleport

Click one placement slot.

**Expected:** Player teleports to that block's location.

**Pass:** Teleport fires.
**Fail:** Nothing on click.

---

## Test G25.16 — Export PNG from Block Editor

```
/cb editor g25a
```

Click "Export PNG" slot.

**Expected:** `Texture exported → cloud_exports/g25a.png` with `[download]` link.

**Pass:** File created, link in chat.
**Fail:** Slot missing or export fails.

---

## Test G25.17 — Set tab icon

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
| G25.1 | Reid changes ID, preserves name | ⬜ |
| G25.2 | Reid rejects conflicting ID | ⬜ |
| G25.3 | Reid is undoable | ⬜ |
| G25.4 | Swap IDs — names stay with blocks | ⬜ |
| G25.5 | Swap IDs is undoable | ⬜ |
| G25.6 | Swap names — IDs unchanged | ⬜ |
| G25.7 | Swap names is undoable | ⬜ |
| G25.8 | Duplicate alias (auto and custom ID) | ⬜ |
| G25.9 | Custom drop configured | ⬜ |
| G25.10 | Custom drop fires on break | ⬜ |
| G25.11 | Drop persists after restart | ⬜ |
| G25.12 | Clear drop removes drops | ⬜ |
| G25.13 | Drop slot in Block Editor GUI | ⬜ |
| G25.14 | Block Finder GUI shows placements | ⬜ |
| G25.15 | Finder teleports to location | ⬜ |
| G25.16 | Export PNG from Block Editor | ⬜ |
| G25.17 | Tab icon updated via URL | ⬜ |

**Group 25 passes when all identity operations and block management extras work in-game.**

If anything shows ❌ — paste:
1. Exact command typed
2. What happened vs expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g25a
/cb delete g25b
/cb delete g25b_renamed
/cb delete g25c
/cb delete g25d
/cb delete g25b_copy
/cb delete my_custom_copy
```
(Break any placed `g25d` instances in the world.)

---

## G25-1 · `#` ("the block I'm looking at") in EVERY block command

> 🔍 diagnosed — do G04-2 first; `BlockRefArgumentType` needs real command tree that G04-2 builds

> *"# should be in all sub subcommands … currently /cb delete # exists which deletes the block ur
> looking at, but i want it for ALL commands that delete retexture, etc, like /cb retexture # (link)"*
> *"and another subidea is i want ! instead of # for held block"*

**Symptom** — `#` is a shortcut meaning *"the custom block my crosshair is on"* (raycast ~10 blocks). `!` is a shortcut meaning *"the custom block in my hand"*.
They work for **one** command only — `/cb delete #`. The developer wants them everywhere a block is targeted,
so `/cb retexture # <link>`, `/cb rename !`, `/cb color #`, `/cb shape !`, etc. all aim at the looked-at or held
block instead of forcing them to type the id.

**Decisions (developer, this session):**
- **Coverage = EVERY block command** that targets an *existing* block — retexture, rename, dupe, reid,
  shape, color, glow/hardness/sound + the other attributes, note, anim, give, lock/unlock, open-GUI,
  face, blueprint, cloud, etc. **Only `/cb create` is excluded** (you're making a new block — nothing to
  look at yet). One consistent rule across the whole `/cb` tree.
- **Arabic auto-join letters included too** — but only via a *verified, no-risk* path (see below). Quote:
  *"second option but i want a verified and optimal way that doesnt oppose and risks or problems."*
- **Edge Cases Locked (developer, this session):**
  - **`#` Range:** 10 blocks (easier to edit ceilings).
  - **`!` Hand Priority:** Checks Main Hand first; if empty, seamlessly falls back to Offhand.
  - **Pierce Entities & Fluids:** `#` raycast pierces right through water, lava, cows, and zombies to hit the block behind them.
  - **Item Frames:** Looking at an Item Frame reads the item inside it. If it's a CustomBlock, `#` targets it!
  - **Spectator / Execute As:** `/execute as @p` and Spectator mode are fully supported.
  - **Collisions:** A block can literally NEVER be named `#` or `!` because Brigadier's `StringArgumentType.word()` strictly forbids it for `/cb create`. Collisions are mathematically impossible.

**Why it's only in delete today** — two real reasons:

1. **`#` doesn't parse as a normal argument.** Every other block command reads its id as
   `StringArgumentType.word()`, and Brigadier's unquoted-word reader **does not allow `#`**
   (`isAllowedInUnquotedString` permits only `0-9 A-Z a-z _ - . +`). `/cb delete #` only works because
   `DeleteCommands` registers `#` as a **separate `literal("#")` branch** (`DeleteCommands.java:47`),
   not as the id arg. So `/cb retexture #` today just errors: `#` is rejected before the handler runs.
2. **The raycast helper is private to delete.** `lookedAtCustom(player)` (`DeleteCommands.java:83`) casts
   the looked-at block to `SlotBlock` and returns its `SlotData`; it lives only in `DeleteCommands` and
   returns `null` for anything that isn't a `SlotBlock` (incl. Arabic auto-join letters).

**The real risk to avoid (why "naive `#` everywhere" is dangerous)** — Arabic **auto-join letter blocks
have NO `SlotData`, NO `customId`, NO texture file.** They're a plain world block + `ArabicLetterBlockEntity`
holding `letter` / `form` / `color` (one of 4 bundled: black/red/green/yellow), drawn live by a BER
(ADR-005, no pack). Almost every `/cb` block command resolves `SlotManager.getById → SlotData → mutate`.
If `#` just handed "the Arabic block" into those pipelines, they'd **NPE or silently corrupt** (there's no
slot to retexture/reid/shape). That mismatch is exactly the "opposition / risk" the developer flagged.

**Fix — one shared resolver + typed targets + opt-in per command (verified, no-crash):**

1. **`BlockRefArgumentType`** (new) replaces `argument("id", word())` everywhere. It accepts a normal id
   **or** `#`, and its tab-complete suggests existing ids **plus `#`** (so the shortcut is discoverable).
   One mechanical swap per command; `/cb delete #`'s existing literal can fold into it.
2. **`BlockTarget.resolve(src, raw)`** (new, central) returns a small typed result:
   - **`SLOT(SlotData)`** — a normal custom block. From `#` raycast hitting a `SlotBlock`, **or** any typed
     id via `getById`. All existing slot commands work **unchanged**.
   - **`ARABIC(BlockPos, ArabicLetterBlockEntity)`** — `#` raycast hit an auto-join `ArabicLetterBlock`.
   - **error states** (not looking at a custom block · console/no player · id not found) → the resolver
     prints one friendly `Chat.error` and the command returns 0. No duplicated error text per command.
3. **Each command opts in to Arabic** — the safety guarantee:
   - **`delete #`** on Arabic → break the world block (it's an ordinary block; `ArabicJoinFlow` already
     re-flows neighbours on break). Reuse the normal break path.
   - **`color #`** on Arabic → call the **existing, proven** `recolorArabicLetter(player, world, pos)` —
     the exact mechanic the colour **Square** already uses (`ShapeToolItem.java:77`). Only the 4 bundled
     colours apply. Zero new render risk.
   - **every other command** (retexture, rename, reid, dupe, shape, glow, note, anim, give, …) → an Arabic
     target gives a **clean, friendly refusal**, e.g. *"That's an Arabic auto-join letter — it has no
     texture/id to retexture. You can delete or recolour it."* **It never reaches the SlotData pipeline,
     so it can't crash or corrupt.** ← this is the "verified, no-risk" path the developer asked for.

So Arabic is accepted as a **target** everywhere (no "that block doesn't exist" confusion), but unsupported
operations **refuse politely instead of mangling data**. New Arabic-capable ops (beyond delete/recolor) can
be added later one at a time, each explicitly — never by accident.

| Fact | Detail |
|---|---|
| `#` today | `DeleteCommands.java:47` — `literal("#")` branch, delete only |
| Raycast | `DeleteCommands.java:83` `lookedAtCustom`, REACH = 6.0, `SlotBlock`→`SlotData`, null for non-slot |
| Why `#` won't ride the id arg | Brigadier unquoted-word excludes `#` (`word()` rejects it) |
| Commands taking an existing-block id | ~20: rename, dupe, retexture, reid, shape×3, note, attributes×5, anim, blueprint, give, face, template, chestgui, cloud, colorimage, imagetool, management(lock/unlock), arabic |
| Excluded | `/cb create` (new id, nothing to look at) |
| Arabic block has no | `SlotData` / `customId` / texture file → can't retexture/reid/shape |
| Proven Arabic recolor path | `ShapeToolItem.recolorArabicLetter(player, world, pos)` (Square tool, `:77`) |
| Coverage decision | **Every** existing-block command (developer) |
| Arabic decision | Include, but only via opt-in safe path (developer: "verified and optimal … no risk") |

**Touches (when built):** new `BlockRefArgumentType` + `BlockTarget` (resolver) · `DeleteCommands`
(fold its `#` literal in) · every handler taking `argument("id", word())` (swap arg type + `getById` →
`BlockTarget.resolve`) · reuse `ShapeToolItem.recolorArabicLetter` + `ArabicJoinFlow` break path.
**Related:** G06-2/G06-3 (delete rail) · G13-20 (Arabic consolidation — more Arabic ops may join later).
**Scope:** Both — raycast + commands are server-side; identical SP & MP.
