# Group 17 — Command Regressions

> **Prerequisite:** Phases 0–16 build-verified. Group 01 (Legacy Audit) signed off.
>
> **Objective:** Fix all commands that exist in new CustomBlocks-B but are weaker than their old equivalents. Restore multi-undo/redo, give with amount/player args, dedicated unfavorite, search GUI, delete shorthand, and export path consistency.
>
> **Source issues:** Group N (regression table), Issue 17.13 (fav/favorite alias)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## Locked Decisions & Slice Plan (interview 2026-06-21)

Owner interview locked the scope, semantics, and build order before any code.

**Status (2026-06-21):** Slices 1–3 built, build-green. Slice 1 confirmed in-game; Slices 2–3
awaiting in-game test (owner chose to build the leftovers and test all in one session). Tests
in `Reports/GROUP_17_TESTING_GUIDE.md`.

**Pace:** one slice at a time, with an in-game confirm between each (golden rule). Build order:

| Slice | Covers | Files (primary) | State |
|---|---|---|---|
| **1** | Multi-undo `undo <N>` · `undo all` · `undo clear` (confirm) · multi-redo `redo <N>` · `redo all` | `HistoryCommands`, `UndoManager` | ✅ in-game confirmed |
| **2** | Give `<amount>` (self) · `<player>` (OP-only) | new `GiveCommands` (split out of `UtilityCommands`) | 🟡 built, awaiting test |
| **3** | Delete `#` (looked-at custom block) | new `DeleteCommands` (split out of `CreationCommands`; server raycast) | 🟡 built, awaiting test |
| verify | Search GUI (G17.9) — **already opens `Nav.Dest.SEARCH` chest** — confirm in-game only | — | 🟡 verify in-game |

**Decisions:**
- **Undo/redo (slice 1).** `undo <N>` reverts up to N steps; N over the available count reverts what's there and reports the real number (no error). `undo all` / `redo all` added as shortcuts. Multi-undo chat = **detailed with values**, e.g. `Undid 3 actions:` + a per-step list `glow g17a 12→8`, then `(K left)`. `undo clear` **asks to confirm** (reuse the existing `BulkConfirm` hold), then clears undo **and** redo and reports the count. A BATCH counts as one step. Respects the existing `undoMode` (per-player / global) automatically.
- **Give (slice 2).** Amount range 1–6400 (100 stacks). Overflow → **insert what fits, report `… (Y didn't fit — inventory full)`** (does NOT drop the rest). The `<player>` form is **OP / permission-level-2 only**; self-give stays open to all. Recipient is **notified** (`You received N × Name from <giver>.`) and the giver gets a confirmation. Offline/unknown player → error. Tab-complete: amount samples + online player names.
- **Delete `#` (slice 3).** Deletes the **whole block definition** (= `/cb delete <id>`), resolved by a ~6-block raycast to the looked-at block; **no confirm** — relies on `/cb undo`. **Custom slot blocks only**; Arabic letters / vanilla / air → `The block you're looking at isn't a custom block.`
- **Deferred — NOT in this group's build:**
  - `favorite` (primary, non-toggle) · `unfavorite` (dedicated) · `recent` → **owned by Group 25**; G17 keeps tests G17.6–G17.8 + G17.12 as regression checks only. Current `/cb fav` toggle stays as-is.
  - Export alignment row → folded into the **Issue 17.15 export-rework** session (dedicated discussion). Not touched here.

---

## Regression Table

| Command | Old CB behavior | New CB-B behavior | Gap to fix |
|---|---|---|---|
| `undo` | `/cb undo`, `/cb undo <N>` (undo N steps), `/cb undo clear` | Single undo only | Add multi-undo (`undo <N>`) and `undo clear` |
| `redo` | `/cb redo`, `/cb redo <N>` | Single redo only | Add multi-redo (`redo <N>`) |
| `give` | `/cb give <id> <amount> <player>` | `/cb give <id>` only | Add `<amount>` and `<player>` arguments |
| `favorite` | `favorite` command + dedicated `unfavorite` command | `fav` only (toggle), no `unfavorite` | Restore `favorite` primary + dedicated `unfavorite`, keep `fav` alias — **feature owned by G25**; row is a regression check |
| `search` | Opened a searchable GUI | Text output only | Search GUI (chest-based) |
| `config` | Opened full settings GUI | Reduced/no GUI | Full Config GUI (Group 21, but regression must not block other commands) |
| `delete` | `/cb delete <id>` and `/cb delete #` (delete the block you're looking at) | `/cb delete <id>` only | Add `#` shorthand for targeted block |
| `export` | `exportall`, `exportcategory`, `list export csv` | Different structure | Align with Group 12 export structure |
| `recent` | `/cb recent` — recently used blocks list | Missing | Restored — **feature owned by G25**; row is a regression check |

---

## What this group covers

| Feature | Commands |
|---|---|
| Multi-undo | `/cb undo <N>`, `/cb undo clear` |
| Multi-redo | `/cb redo <N>` |
| Give with args | `/cb give <id> [amount] [player]` |
| Favorite alias | `/cb favorite <id>` (primary), `/cb fav <id>` (alias) — **owned by G25**, regression check here |
| Unfavorite | `/cb unfavorite <id>` (dedicated command) — **owned by G25** |
| Search GUI | `/cb search <query>` → results in chest GUI |
| Delete shorthand | `/cb delete #` |
| Recent blocks | `/cb recent` — **owned by G25**, regression check here |

---

## Implementation Requirements

### 1. Multi-Undo and Undo Clear

`/cb undo` — undoes one action (existing behavior, preserved).
`/cb undo <N>` — undoes the last N actions in one command. Max = `maxUndoDepth` (default 100). If
N exceeds the available steps, undo what's there and report the real number (no error).
`/cb undo all` — undoes the entire stack in one command.
`/cb undo clear` — **asks to confirm** (`Clear N undo steps? [confirm]`, reusing the `BulkConfirm`
hold), then clears the undo **and** redo stacks and reports the count cleared.

Each multi-undo prints a **detailed, value-bearing** summary:
```
Undid 3 actions:
  • glow g17a 12→8
  • glow g17a 8→4
  • glow g17a 4→0
  (2 left)
```
A BATCH (bulk op) counts as one step and renders as `label (N blocks)`. Tab-complete on `/cb undo`
offers `all`, `clear`, and sample numbers. Per-player vs global behavior follows the existing `undoMode`.

### 2. Multi-Redo

`/cb redo` — redoes one action (existing behavior, preserved).
`/cb redo <N>` — redoes the last N undone actions (over-count → redo what's there, report real number).
`/cb redo all` — redoes the entire redo stack. Same detailed summary + `all`/number tab-complete.

### 3. Give with Amount and Player

`/cb give <id>` — gives 1 of the block to the calling player (existing behavior).
`/cb give <id> <amount>` — gives `<amount>` (1–6400) items to the calling player. If the inventory
fills, give only what fits and report the rest: `Gave 12 × GiveTest (52 didn't fit — inventory full).`
`/cb give <id> <amount> <player>` — gives to a specific **online** player. This form is
**OP / permission-level-2 only** (self-give stays open to everyone). Offline/unknown name → error.

Feedback: the giver sees `Gave N × Name to <player>.`; the recipient is notified
`You received N × Name from <giver>.`

Tab-complete: `<amount>` offers samples (1/16/32/64); `<player>` shows online player names.

### 4. Favorite / Unfavorite

> **Ownership (sweep 2026-06-21):** the favorites feature is owned by **G25** (`GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md`).
> G17 keeps tests G17.6–G17.8 as a regression check only; the canonical spec lives in G25.

`/cb favorite <id>` — adds block to favorites. This is the **primary** command name.
`/cb fav <id>` — alias of `favorite` (existing behavior preserved).
`/cb unfavorite <id>` — removes block from favorites (dedicated, does not toggle).
`/cb favs` — lists favorites (existing behavior preserved).

All four forms tab-complete block IDs.

### 5. Search GUI

`/cb search <query>` — finds blocks matching the query (ID, name, category). Results in:
1. If 1 result: opens block editor directly.
2. If 2–27 results: opens a chest GUI with each matching block as a slot. Click → edit or give.
3. If >27 results: paginated chest GUI.
4. If 0 results: chat message "No blocks matched '<query>'."

### 6. Delete Shorthand

`/cb delete #` — resolves the block the player is looking at (server raycast, ~6-block reach) to its
custom id and deletes the **whole block definition** — identical to `/cb delete <id>` (lock check,
snapshot, undoable). Message: `Deleted "g17a" (targeted block). Undo with /cb undo.` **No confirm**
prompt — a mis-aim is recovered with `/cb undo`.

Targets **custom (slot) blocks only**. Aiming at an Arabic letter block, a vanilla block, or air →
`The block you're looking at isn't a custom block.`

`/cb delete <id>` remains unchanged.

### 7. Recent Blocks

> **Ownership (sweep 2026-06-21):** `recent` is owned by **G25**. G17 keeps test G17.12 as a regression check.

`/cb recent` — shows the last 10 blocks the player interacted with (gave, edited, created, or retextured). Output: chest GUI with up to 10 block slots. Click a slot → opens block editor.

Persisted per-player in `config/customblocks/data/players.json`.

---

## Setup

```
/cb create g17a UndoTest
/cb create g17b GiveTest
/cb create g17c SearchTest1
/cb create g17d SearchTest2
/cb setglow g17a 4
/cb setglow g17a 8
/cb setglow g17a 12
```

---

## Test G17.1 — Multi-undo

```
/cb undo 3
```

**Expected:** `Undid 3 action(s): setglow g17a 12→8, setglow g17a 8→4, setglow g17a 4→0`

Verify: `g17a` now has glow 0.

**Pass:** All 3 glow changes undone in one command.
**Fail:** Only one undo fires, or error.

---

## Test G17.2 — Undo clear

```
/cb setglow g17a 5
/cb setglow g17a 10
/cb undo clear
/cb undo
```

**Expected after clear:** `Nothing left to undo.`

**Pass:** Stack cleared — undo fires no action.
**Fail:** Last undo still works after clear.

---

## Test G17.3 — Multi-redo

```
/cb setglow g17a 1
/cb setglow g17a 2
/cb setglow g17a 3
/cb undo 3
/cb redo 3
```

**Expected after redo 3:** g17a has glow 3. `Redid 3 action(s).`

**Pass:** All 3 actions redone.
**Fail:** Only one redo, or error.

---

## Test G17.4 — Give with amount

```
/cb give g17b 5
```

**Expected:** `Gave 5 × GiveTest to [you].`

**Pass:** 5 items in inventory.
**Fail:** Error, or only 1 item given.

---

## Test G17.5 — Give to another player

*(Requires a second online player — skip if solo.)*

```
/cb give g17b 2 <other-player-name>
```

**Expected:** `Gave 2 × GiveTest to <other-player>.`

The other player receives 2 items.

**Pass:** Other player receives items.
**Fail:** Error or items go to wrong player.

---

## Test G17.6 — Favorite as primary command

```
/cb favorite g17a
```

**Expected:** `Added "g17a" to your favorites.` (Not a toggle — adds, doesn't toggle off.)

**Pass:** Block added to favorites.
**Fail:** Command missing, or toggle behavior (removes if already there).

---

## Test G17.7 — Unfavorite dedicated command

```
/cb unfavorite g17a
```

**Expected:** `Removed "g17a" from your favorites.`

Then `/cb favs` — g17a not listed.

**Pass:** Dedicated unfavorite removes correctly.
**Fail:** Command missing, or toggle behavior, or still shows in favs.

---

## Test G17.8 — Fav still works as alias

```
/cb fav g17a
```

**Expected:** Same behavior as `/cb favorite g17a` — adds to favorites.

**Pass:** Alias works.
**Fail:** Command changed behavior or missing.

---

## Test G17.9 — Search returns GUI

```
/cb search SearchTest
```

**Expected:** Chest GUI opens with 2 slots: g17c (SearchTest1) and g17d (SearchTest2). Click a slot → opens block editor for that block.

**Pass:** Chest GUI with 2 results, click-to-edit works.
**Fail:** Text output only, or no results found.

---

## Test G17.10 — Delete shorthand (#)

Place `g17a` block in the world. Look directly at it.

```
/cb delete #
```

**Expected:** `Deleted "g17a" (targeted block).`

**Pass:** Block deleted by crosshair targeting.
**Fail:** Error "unexpected argument #", or wrong block deleted.

---

## Test G17.11 — Delete # on non-custom block

Look at any vanilla block (e.g., dirt).

```
/cb delete #
```

**Expected:** `The block you're looking at isn't a custom block.`

**Pass:** Correct message, no deletion.
**Fail:** Error, or vanilla block affected.

---

## Test G17.12 — Recent blocks

After using give, edit, and create commands in this test session:

```
/cb recent
```

**Expected:** Chest GUI opens with up to 10 recently interacted blocks. Each slot shows the block. Click → opens block editor.

**Pass:** Recent list shows recent blocks, click-to-edit works.
**Fail:** Command missing, empty list, or text output only.

---

## Group 17 Verdict

| Test | Description | Result |
|---|---|---|
| G17.1 | Multi-undo reverts N actions | ⬜ |
| G17.2 | Undo clear empties stack | ⬜ |
| G17.3 | Multi-redo restores N actions | ⬜ |
| G17.4 | Give with amount gives N items | ⬜ |
| G17.5 | Give to another player | ⬜ |
| G17.6 | `favorite` is primary command | ⬜ |
| G17.7 | Dedicated `unfavorite` works | ⬜ |
| G17.8 | `fav` alias still works | ⬜ |
| G17.9 | Search returns chest GUI | ⬜ |
| G17.10 | Delete # removes targeted block | ⬜ |
| G17.11 | Delete # on vanilla block gives clear error | ⬜ |
| G17.12 | Recent blocks chest GUI | ⬜ |

**Group 17 passes when all regression fixes are confirmed working in-game.**

If anything shows ❌ — paste:
1. The exact command typed
2. What happened vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g17b
/cb delete g17c
/cb delete g17d
```
(g17a was deleted via `delete #` in G17.10 — re-create if needed for cleanup)
