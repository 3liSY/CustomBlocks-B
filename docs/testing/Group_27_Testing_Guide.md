# Group 27 — Screens, Studio & HUD

**🎯 Active:** **T (0/25 — Bulk Ops Hub screens BUILT 2026-07-12, never in-game; carries G07-HUB + G07-X3)** · L (8/28) · **L-v6 rebuild (0/26 — design locked 2026-07-05, not built)** · Fixes (2/12) · C (4/10) · K (6/14) · J reverted (0/2) · M (a built 0/2, not in-game; spec → §G27.20, not built) · N (15/15) · O (11/11) · P (15/15) · Q (0/0 — spec locked 2026-07-12, not built)
**📦 Jar:** `customblocks-1.0.0.jar` (2026-07-05, **UI-kit Batch 2** — categories real on creation · L10 drag+click · block search · F3 GIF grid · K7 immediate-fill · L9 delete auto-select). *Build-green only (compile + all gates); NOT dev-confirmed in-game.*

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | 🎯 test-now (Batch 2 rebuilt L6, L9–L12, F2, F3, K7 — retest)                     |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅🟡🟡🟡🟡🟡🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · ~31% MP pass (20/64 slots) confirmed; Batch 2 rebuilt the 7 open items, awaiting retest |
| **Last tested** | 2026-07-05 — owner MP re-swept L/F/K. ✅ L1–L5, L7, L8, F1, F4, K1–K6 · 🟡 L6, L9, F3, F5, F6 · ⚠️ L10–L12, F2, K7 were broken → **Batch 2 rebuilt L6/L9/L10/L11/L12/F2/F3/K7** (build-green, not yet retested). F5/F6/K5/K6 still open (need design discussion). |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): HudEditorScreen/ShapeEditorScreen/BlockCreationStudioScreen/CategoryHubScreen confirmed right medium, flagged for design/UX polish pass. See `GROUP_27_SCREENS.md`.

## 🗺️ Sections

| § | What                                                                | State     |
| --- | ------------------------------------------------------------------- | --------- |
| T | **Bulk Operations Hub screens** (§G27.22 Blocks List + §G27.22b Bulk Actions + §T-d op behaviour, moved from G07 §B) | 🎯 MP retest + 6 discuss items |
| L | **Category Hub** — full red+black category manager (`/cb category`) | 🎯 test-now |
| Fixes | Screen-sweep: help opacity (K7) · collapse arrow (K4) · preview loaders · label overlaps → §M | 🎯 test-now |
| C | Shape folds into the Studio Shape section (§G27.11; Recolor half planned) | 🎯 test-now |
| K | Master-order step 1 — locked red+black frame + movable action bar   | 🎯 test-now |
| J | Earlier studio + 5 screens (arabic · recolor · eyedrop · edithud)   | 🎯         |
| A | Studio — 5 fixes (Enter · hex · bg-behind-image · UI ✔ · Category)  | 🧊 parked  |
| B | HUD templates + shape backgrounds                                   | 🧊 parked  |
| D | Screen toasts replace chat                                          | ⏳ planned |
| E | Studio Polish Pass (red+black, P1–P15)                              | ⏳ planned |
| F | Onboarding + Achievements screens                                   | ⏳ planned |
| G | `/cb animation` hub / timeline editor                               | ⏳ planned |
| H | red+black restyle of the 5 older screens + Settings gear            | ⏳ planned |
| I | Studio more — Edit Mode · Editing (Paint + Resize) · FX/Behavior/Lore tabs · carve | ⏳ planned |
| M | **Text-overlap fix + field-layout standard** (all screens) — step a built (§G27.20a); d+e prevention next | 🎯 test-now |
| N | **Advanced Per-Face Customization** — design LOCKED 2026-07-12, shell spec'd (§G27.11g + §G27.18) | ⏳ planned |
| O | **Studio UI Overhaul & 3D Design Editor** — design LOCKED 2026-07-12, shell spec'd (§G27.11h + §G27.19); Editing behavior = §G27.10 | ⏳ planned |
| P | **Settings Book Screen** — real spec is §G27.27 (owner-locked); screen design LOCKED 2026-07-12, §G27.11i (§G27.21 is superseded, ideas-only) | ⏳ planned |
| Q | **Backup + Trash + Safety hub Screens** (folded from Group 09, spec locked 2026-07-12, §G27.27–29) | ⏳ planned |
| R | **Diagnostics Screen** — `⊙ Details` opens ONE incident, not all of them (folded from Group 04 §C, 2026-07-15) | ⏳ planned |

🔗 **Related Doc:** [GROUP_27_SCREENS.md](../groups/GROUP_27_SCREENS.md)

---

# 🎯 Test now

### 🧰 Setup Required
* A world with a few categories that have blocks (use `/cb setcategory` or the studio); one 2nd player for the live-push checks (§L)
* Any custom block id (see `/cb list`); one animated (GIF) block for C4 (§C)
* Any word block for arabic preview; any custom block for the rest (§K)
* `/cb create`; a logo PNG with transparent areas (§A)
* `/cb edithud` (§B)

## T · Bulk Operations Hub screens (§G27.22 / §G27.22b) · 🎯 0/25

> 💡 `/cb list` opens the **Bulk Operations Hub** — two LEFT-rail tabs: **Blocks List** (spinning-cube grid) and **Bulk Actions** (10 ops + include-checkbox rows + RESULT PREVIEW).
> ⚠️ **Screen spec: `GROUP_27_SCREENS.md` §G27.22 + §G27.22b** — both locked 2026-07-12 and **now BUILT**. §T-d (below) folds in the per-op behaviour rows that used to live in `GROUP_07_TESTING_GUIDE.md` §B — it's the same screen, testing it split in two never made sense.
> 🧰 ~15 blocks, at least one **locked**, one **★ favorite**, and a couple placed in the world; a 2nd player for MP.

### T-a · Blocks List tab (§G27.22)

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| T1 | `/cb list` | opens on **Blocks List**; LEFT rail has exactly **2 tabs** (no Extra/Health); grid shows ~30 tiles, each a rotating 3-D cube + display name + **`id:` in lime**; no dead middle space | 🎯 | 🎯 |
| T2 | tick a tile · change a filter chip · change Sort ▾ · scroll | cube rotation is smooth and **never resets/jumps** (phase-locked to one shared clock) | 🎯 | 🎯 |
| T3 | left-click a tile | red border + red tint + lime **✓** badge + a subtle red pulse; the green **`N selected`** pill in the title bar follows | 🎯 | 🎯 |
| T4 | right-click a tile, then right-click another | an **info panel opens on the LEFT** (hero cube, name, id, category, live status) with **Target/Untarget** + **Open editor**; the 2nd right-click swaps it in; ✖ or right-click empty closes | 🎯 | 🎯 |
| T5 | click each filter chip: **All · ★ Favorites · Locked · Selected** | the grid narrows to that set; the active chip is red | 🎯 | 🎯 |
| T6 | **Sort ▾** → Name · ID · Newest · Color | the grid reorders correctly (Newest = most recently made first; Color groups by texture hue) | 🎯 | 🎯 |
| T7 | drag the scrollbar thumb · wheel over the grid | both scroll; the **`showing X of N`** counter under the grid updates | 🎯 | 🎯 |
| T8 | footer **Select ▾** → each of the 4 options · then **Clear selection** | *All on screen* / *All matching search* / *Locked only* / *Favorites* each tick the right set; Clear empties and the button greys out when nothing is selected | 🎯 | 🎯 |
| T9 | ★ favorite + locked blocks in the grid | ★ gold top-left, hand-drawn **pixel padlock** bottom-left (NOT a missing-glyph box); MP: a 2nd player never sees player 1's ★ | 🎯 | 🎯 |

### T-b · Bulk Actions tab (§G27.22b)

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| T10 | open the **Bulk Actions** tab | LEFT rail = 2 tabs + a red `— ACTIONS —` separator + the **10 ops** (Edit · Recolor · Rename · Move · Duplicate · Re-ID · Lock · Favorite · Export · Delete), each with a pixel icon; **Delete** is muted-red | 🎯 | 🎯 |
| T11 | click through every op | the active op goes red; the header swaps its icon + name + description + inline controls; the rows and right panel re-render — with **no cube-spin reset** | 🎯 | 🎯 |
| T12 | read a row | **include ☑** far-left, **OLD cube** in a fixed cell, name + old value, a **red →**, **NEW cube**, then the **new value in lime** with an `after` sublabel | 🎯 | 🎯 |
| T13 | tick a **locked** block and pick Edit/Rename/Move/Re-ID/Delete/Recolor | that row is greyed, shows the **pixel padlock**, reads **`will skip`**, and is excluded from `affects N` — but on the **Lock** op it is included normally | 🎯 | 🎯 |
| T14 | un-tick a row's **include checkbox**, then re-tick | un-tick dims the row (grey stripe) and drops it from **`affects N of T`**, the **⚡ Execute on N** count, and the right **SUMMARY** + green bar; re-tick restores all three | 🎯 | 🎯 |
| T15 | watch the right **RESULT PREVIEW** while switching ops / toggling checkboxes | header + a sample block's **before → after** rotating cubes with captions, the `old → new` line, and a SUMMARY (`✓ change N` / `skip`) with a **green proportion bar** — all update live | 🎯 | 🎯 |
| T16 | **right-click anywhere** on this tab | **nothing happens** — no context menu, no info panel (that gesture is Blocks-List only) | 🎯 | ➖ |
| T17 | click every op and read each row's text | name/old-value and new-value **never overlap a spinning cube** (fixed cell); nothing is clipped mid-word | 🎯 | 🎯 |
| T18 | **⚡ Execute** → **Cancel**, then Execute → **Confirm** | Cancel closes with no change; Confirm runs a red **sweep bar** left→right across the rows with an `applying c/N` count-up, ending **`✓ <op> applied on N`** | 🎯 | 🎯 |

### T-c · Shared Hub chrome

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| T19 | look at the title bar | **"Bulk Operations Hub"** bold red with **no subtitle**; under it `N blocks` in grey **and a separate green-bordered `N selected` pill** (two distinct counts, not one run-on string) | 🎯 | 🎯 |
| T20 | click every op with Rename Mode = `replace` (the widest layout) | no label overlaps a button/field; nothing is clipped or ellipsized | 🎯 | 🎯 |
| T21 | **Execute** with 1 selected, then with 20 | the **confirm dialog opens both times** (titled "&lt;op&gt; N blocks?"), is **fully opaque** (nothing bleeds through), and never bounces to chat or closes the Hub | 🎯 | 🎯 |
| T22 | click **History** (footer, bottom-LEFT), then click a step a few back | the popup opens **centered, on top, over a DIMMED backdrop** (the screen stays faintly visible — the one owner-approved `OPAQUE_MODALS` exception; the box itself is solid); a step row **jumps back N at once**; ✖ or a click outside closes it | 🎯 | 🎯 |
| T23 | in the always-visible **NL bar** (top), type `delete all locked` (or `glow 10 on red`) + Enter | it lands on the matching op **and ticks the blocks it matched**, with live rows + preview; the old "press Enter to parse" helper text is **gone**; **nothing runs** until Execute; a bad phrase shows a "couldn't parse" hint and runs nothing | 🎯 | 🎯 |
| T24 | Execute an op that changes nothing (Edit on only-locked blocks) — **G07-X3** | a **Result** modal pops **in-screen** ("No blocks changed — all 1 locked"); dismiss with OK/Esc; the same line is **also** still in chat (dual feedback — both, not either) | 🎯 | 🎯 |
| T25 | run 2–3 ops, then Undo/Redo | a bottom-right **toast** flashes the step ("Undid Delete (12)" / "Redid …") and auto-fades; no chat spam from the toast | 🎯 | 🎯 |

### T-d · Bulk-op behaviour (moved from G07 §B, tested SP 2026-07-13)

> 💡 What each of the ten ops actually **does** to the blocks, and that one undo reverts it. Moved here from `GROUP_07_TESTING_GUIDE.md` — it's the Bulk Actions tab, same screen as T-b.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| Bd1 | tick on Blocks List, switch to Bulk Actions | ticks carry over as one row per block; `affects N of T` + the Execute count match | ✅ | 🎯 |
| Bd2 | **Edit** glow 0 → 8 | glow updates; one undo reverts | ⚠️ | 🎯 |
| Bd3 | **Rename** → prefix, suffix, replace | live colour example; rows preview → Execute → one undo | ❔ | 🎯 |
| Bd4 | **Move** → type a category | blocks move; `none` clears; one undo | 🟡 | 🎯 |
| Bd4a | **Move** → category with a space (`red stone`) | one category "red stone", not cut short | 🟡 | 🎯 |
| Bd5 | **Duplicate** | copy appears with `_copy` suffix; one undo | ✅ | 🎯 |
| Bd6 | **Export** 7 formats | each format writes correctly | 💔 | 🎯 |
| Bd7 | **Lock/Unlock** | locked blocks skip the six ops that modify them | ✅ | 🎯 |
| Bd8 | **Favorite/Unfavorite** (2 players) | 2nd player's favorites unaffected | ✅ | 🎯 |
| Bd9 | **Re-ID** → type a pattern with `{n}` | each row's NEW value shows the numbered id | ❔ | 🎯 |
| Bd9a | **Re-ID** pattern that hits an existing id / a no-op / an illegal id | that row reads `id taken` / `no change` / `invalid id`; locked reads `will skip` | ❔ | 🎯 |
| Bd10 | **Delete** | blocks removed + world markers become "Deleted: …"; one undo restores both | ✅ | 🎯 |
| BdR | **Recolor** → drag the hue slider, Execute | every selected block's texture hue-shifts live; one undo reverts the whole batch | 💔 | 🎯 |
| BdRa | **Recolor** with the hue left at **0°** | Execute is greyed out (0° is a no-op) | ✅ | 🎯 |
| Bd11 | tick a **locked** block → Edit/Rename/Move/Re-ID/Delete/Recolor | it is skipped and **named in the result** | ✅ | 🎯 |
| Bd18 | player A applies (lock / ★ / delete) while player B has the Hub open | B's Hub live-refreshes without reopening | ➖ | 🎯 |
| Bd21 | duplicate a block | copy appears with texture + settings | ✅ | 🎯 |
| Bd22 | duplicate a block whose id ends `_copy` (e.g. `stone_copy`) | new id is `stone_copy2`, **not** `stone_copy_copy` | ⚠️ | 🎯 |
| Bd23 | run an op, then **↶ Undo** (then **↷ Redo**) in the footer | Undo reverts that whole bulk op in one step; Redo re-applies | 🟡 | 🎯 |

## L · Category Hub · 🎯 0/14

> 💡 `/cb category` (or `/cb category list`) now opens the **Category Hub** — one red+black screen replacing the chest browser. Left = searchable category list; right = the selected category's blocks + settings. Every change is server-authoritative and shows live (no rejoin).
> ⚠️ **L6/L9/L10/L11/L11b/L12 are being replaced by the v6 rebuild — see §L-v6 below.** These original rows stay for history; the rebuild rows (L13–L25) are the ones to test once built.
> 🧰 A world with a few categories that have blocks (use `/cb setcategory` or the studio); one 2nd player for the live-push checks.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| L1 | `/cb category` | opens the **Category Hub** client screen (bold-red title, black bg) — NOT a chest GUI | 🎯 | ✅ |
| L2 | look at the left list | every category: colour swatch, name tinted by its colour, live block count, ★ on the default; an **(uncategorized)** row if any blocks have no category | 🎯 | ✅ |
| L3 | click a category | right pane shows its name, block count, settings row, and the blocks inside it | 🎯 | ✅ |
| L4 | click a colour swatch (right pane) | the category name recolours immediately in the list — and for a 2nd player with no rejoin | 🎯 | ✅ |
| L4b | type `#RRGGBB` in the **hex** box → **Set #** | the name takes that exact custom colour (list + header + swatch preview); a swatch pick clears the hex, hex wins over a swatch | 🎯 | ✅ |
| L5 | the **Name** box is pre-filled with the current name → edit letters → **Rename** | category renamed everywhere (blocks + metadata); the hub reselects the new name | 🎯 | ✅ |
| L6 | **Merge into… ▾** → pick a target from the popup (or "+ Create") | all blocks move into the target; the source category disappears; the popup now lists **every** category incl. 0-block ones | 🎯 | 🎯 *(Batch 2: categories are real on creation, so the picker shows them all — retest)* |
| L7 | **Delete** (click once = "Confirm?", click again) | category removed; its blocks are kept (now uncategorized) | 🎯 | ✅ |
| L8 | **Set default** (click again to clear) · **Sort** · **Lock all** · **Unlock all** | chat confirms each; ★ toggles on THIS row and clicking again clears it; lock/unlock affects the blocks | 🎯 | ✅ |
| L9 | select a category with blocks → **Delete** (confirm) | the hub jumps to the **(uncategorized)** bucket showing the freed blocks (NOT a blank pane — the blocks were never lost) | 🎯 | 🎯 *(Batch 2: delete now auto-selects uncategorized — retest; was the "blocks disappear" report)* |
| L10 | **drag** a block row onto a category in the left list — OR click a block → **Move ▾** → pick a target | drag: valid targets glow lime, drop pulses the row + a "+1" count bump; click-then-Move still works too | 🎯 | 🎯 *(Batch 2: full drag-and-drop + click revamp — retest)* |
| L11 | type in the top-right **search** · **+ New category** (type a name first) · scroll wheel · **Close** | +New makes a **real** category immediately (shows at 0 blocks in list + popups); search filters; wheel scrolls; Close returns | 🎯 | 🎯 *(Batch 2: new categories are server-real, no more lost pending rows — retest)* |
| L11b | select a category → type in the **BLOCKS search** box (above the block list) | the block list filters to names/ids matching what you type; empty search shows all | 🎯 | 🎯 *(Batch 2 new — owner request "search for blocks inside a category")* |
| L12 | select the **(uncategorized)** bucket → **Empty into… ▾** / drag a block / Move a picked block | its blocks pour into the chosen category; the target picker lists every real category | 🎯 | 🎯 *(Batch 2: same categories-real fix — retest)* |

## L-v6 · Category Hub REBUILD · 🎯 building (design locked 2026-07-05)

> 💡 The whole hub is being **rebuilt** on the v5 "Red Ops" mockup (`docs/mockups/category_hub_rework_v5.html`).
> These rows replace the failing L6/L9/L10/L11/L12 + F2/F3/K7 behaviour — those get **folded into this rebuild**,
> not shipped separately. Spec: `GROUP_27_SCREENS.md` §G27-3 "🎨 v6 FULL REDESIGN". Rows are ⏳ until built.
> 🧰 A world with a few categories (some empty), some loose/uncategorized blocks, one GIF/animated block, a 2nd player.
> 🔨 Progress: (1) file split `CategoryHubScreen`→`CategoryHubView` done, behaviour-identical, owner confirmed "looks same" 2026-07-05. (2) Overview landing zone built 2026-07-05 (L13) — awaiting SP confirm.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| L13 | `/cb category` (open with NOTHING selected) | opens on a **HUB OVERVIEW** — no category selected; right pane = centred dashboard: marker box + "SELECT A CATEGORY" + four stat pills with **cyan** live values (`CATEGORIES` · `BLOCKS` · `DEFAULT` · `LOOSE`) + sublines. New-category input sits **bottom-left of the rail** with a `+ New` button. Does NOT auto-dive. **Detail boxes (name/hex/desc/block-search) are HIDDEN until a category is selected** — no text overlapping the overview (the reported bug) | 🎯 | ⏳ |
| L14 | look at the left list with some loose blocks | the **(uncategorized) inbox** is pinned at the **TOP**, greyed/distinct; it only appears when loose blocks exist | ⏳ | ⏳ |
| L15 | select a category → look at a block row | row shows a **small thumbnail** + the **name only**; a tiny per-row button toggles the internal id on/off; toggle is **ephemeral** — resets to name-only on scroll or hub reopen (not a persisted global setting) | ⏳ | ⏳ |
| L16 | click the tiny **🔍** on a block row (try a normal AND a GIF block) · then open a 2nd popover on another row while the first is still open | a **preview popover** opens beside the row showing the block big; the GIF one **animates** (not blank) — F3; **multiple popovers can stay open at once** (no auto-close) | ⏳ | ⏳ |
| L17 | tick several blocks (checkboxes) → **Move selected to…** → pick a target · also try bulk **Lock/Unlock** and bulk **Delete** on a multi-selection | all ticked blocks move/lock/unlock/delete together (v6 builds all three bulk actions, not move-only) — try a **locked** block too: it moves fine (lock does NOT block category moves) | ⏳ | ⏳ |
| L18 | click one block → **Move to…** popup → pick a target · then also try **dragging** a row onto a left category | pick→Move is the reliable path and works; drag also works as a bonus | ⏳ | ⏳ |
| L19 | on a category → **Move all blocks to…** → pick a target | all blocks move into the target; **this category stays** (now empty) — it is NOT deleted | ⏳ | ⏳ |
| L20 | on a category → **Empty to loose** | all its blocks go straight to the (uncategorized) inbox with no target picker | ⏳ | ⏳ |
| L21 | on a category → **Delete** | a **confirm popup** appears ("Delete X? Its N blocks move to (uncategorized). [Delete] [Cancel]"); after Delete the blocks survive in the inbox | ⏳ | ⏳ |
| L22 | **Set category icon** (pick a block) — check the pre-pick default too · **Hide from browser** toggle · **Duplicate category** | before picking, icon shows a plain colour swatch (no silent auto-pick); after picking, icon shows as the category's thumbnail in the list; hidden category vanishes from the Studio block-picker browser but **stays visible in this hub** always; duplicate makes a copy of name+settings only (0 blocks, `_copy`/`_copy2`… suffix) | ⏳ | ⏳ |
| L23 | inside a category: **Sort** (A–Z / Z–A / colour / **newest** — uses a real `createdAt` stamp, not slot order) · **Select-all** · **Bulk rename** (prefix / find-replace) | sort reorders the block list correctly incl. newest; select-all ticks every block; bulk rename rewrites **both name AND internal id** together for every block in one action | ⏳ | ⏳ |
| L24 | click a single block → **Rename** · **Delete** (confirm, mentions Trash/`/cb undo`) · **Lock/Unlock** · **Duplicate** (`_copy` suffix) | each acts on that one block — all four are backend-supported (`SlotManager.rename`/`.dupe`, `DeletionService.delete`, `LockManager`), just newly wired to the hub | ⏳ | ⏳ |
| L25 | do any action (move / rename / delete / bulk rename) | a soft UI **click** on buttons + a **lime success chime** on completion (subtle, not spammy); after a bulk action, an **Undo** button appears (fires `/cb undo`) and a **Redo** button undoes the undo | ⏳ | ⏳ |

## Fixes · Screen-sweep · 🎯 0/6

> 💡 Batch-1 fixes from the last test round (K4/K7 + "preview unavailable" + label overlaps).

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | on any framed screen, collapse the action bar (corner toggle) | the collapsed tab is now a **solid red** handle with a bold, larger arrow — easy to spot (was a tiny dim arrow) — K4 | 🎯 | ✅ |
| F2 | open the **[?]** help on recolor / arabic / shape / **Category Hub** | the popup is **fully opaque** — no text from behind bleeds through it — K7 · *(Batch 2: scrim now drawn as an immediate textured quad (`CbImmediateFill`), the PreviewCube trick — retest on a fresh client jar)* | 🎯 | 🎯 |
| F3 | `/cb recolor <id>` / `/cb shapeeditor <id>` on a real block **and a GIF/animated block** | the 3D cube preview **loads** for both — normal AND animated (reads the block's baked grid, crops frame 0) · *(Batch 2: reads the `slot_N.grid.json` sidecar for GIFs — retest)* | 🎯 | 🎯 |
| F4 | arabic preview + `/cb shapeeditor <id>` on a block with a texture | the cube shows the real texture (not the grey fallback / not "unavailable") | 🎯 | ✅ |
| F5 | `/cb recolor` → look at the **TONE TOOLS** heading | "TONE TOOLS" sits clear ABOVE the "Temperature" slider label — no more garbled overlap · *(owner: partial — still looks bad, wants revamp)* | 🎯 | 🟡 |
| F6 | `/cb create` → **Identity** tab | the "Block ID" / "Display Name" labels sit clearly above their boxes (not touching the top border) · *(owner: partial — wants improvement)* | 🎯 | 🟡 |

## M · Text-overlap fix · 🎯 0/2

> 💡 Step a: `CbTextField` now insets + centers its text so hints/values sit inside the box instead of riding up onto the label (fixes every field on every screen at once).

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| M1 | `/cb create` → **Texture** tab, then **AI** tab | the "https://… image url" / "Describe the block…" hint sits **centered inside** its box; the label above is fully clear of the box border (no cram, no clip) | 🎯 | 🎯 |
| M2 | `/cb category` → select a category → look at **Name** + **Description** boxes | the current text sits inside each box; the box top does not cut through the label above it | 🎯 | 🎯 |

## C · Shape folds into the Studio Shape section · 🎯 0/5

> 💡 `/cb shapeeditor` now opens the Block Creation Studio on its Shape tab (standalone shape screen retired); the Recolor half of the §G27.11 fold is still planned.
> 🧰 Any custom block id (see `/cb list`); one animated (GIF) block for C4.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | `/cb shapeeditor <id>` | opens the Block Creation Studio (not the old shape screen); the **Shape** tab is focused; the block's current shape chip is selected | 🎯 | ✅ |
| C2 | Pick a different shape chip → **Save changes** | the real block takes the new shape (place it / look at it); no crash | 🎯 | ✅ |
| C3 | `/cb shapeeditor` (no id) → click a block in the chest picker | the same studio opens on the Shape tab for the clicked block (picker GUI itself → revamp + search, see BUG-G27-303) | 🎯 | ✅ |
| C4 | `/cb shapeeditor <animatedId>` | studio opens on **Shape** (not Animation); the cube still animates the block | 🎯 | ✅ |
| C5 | Save a shape change, 2nd player looks (MP) / relog (SP) | new shape shows for the other player with no rejoin; persists on relog | 🎯 | 🎯 |

## K · Red+black frame + movable action bar · 🎯 0/7

> 💡 Every full-screen CB popup uses the locked palette: **#FF0000 red** borders/titles, **pure black** bars/panels, **#40FF00 lime** only for the "saved" flash; Recolor + HUD editors swap their bottom strips for Arabic's movable bar.
> 🧰 Any word block for arabic preview; any custom block for the rest.

| # | Action                                                              | Expected Result                                                                                   | SP | MP |
| --- | --- | --- | --- | --- |
| K1 | Open each: arabic preview · `/cb recolor <id>` · `/cb eyedrop` · `/cb edithud` · `/cb create` | every title is **bold red** + red border lines; bars/panels pure black — **no gold/yellow anywhere** | 🎯 | ✅ |
| K2 | `/cb recolor <id>` → look at the bottom of the screen             | no full-width button strip; instead the small floating bar [Undo][Redo][Rand][Apply][Copy][Reset][Cancel] | 🎯 | ✅ |
| K3 | Drag the bar's ⠿ grip toward the left edge, then relog & reopen    | bar docks left as a vertical strip; after relog it's still docked left                            | 🎯 | ✅ |
| K4 | Click the bar's `_` toggle, then the sliver arrow                  | bar collapses to a thin tab; clicking the tab brings it back                                      | 🎯 | ✅ |
| K5 | Same bar checks on `/cb edithud`        | movable bar present; buttons all work (Save/Apply/Cancel/Undo…) · *(owner: passes, but `/cb edithud` needs a full look-revamp → edithud v2 slice)* | 🎯 | ✅ |
| K6 | In recolor move a slider → click **Apply**                       | Apply button flashes **lime** briefly; recolour applies like before · *(owner: passes, but wants the top-right toast rectangle + lime revamped → toast slice)* | 🎯 | ✅ |
| K7 | Press `?` on arabic / recolor                      | help popup: red border, red bold headings, fully opaque (no bleed-through), only the red X (or Esc) closes it · *(Batch 2: same `CbImmediateFill` fix as F2 — retest)* | 🎯 | 🎯 |

## A · Studio — 5 new fixes · 🧊 0/5 (Parked)

> 💡 Built, awaiting in-game. Parked by owner 2026-06-21 — resume when you pick this up.
> 🧰 `/cb create`; a logo PNG with transparent areas.

| # | Action                                               | Expected Result                                                                       | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Open studio → click a field → type → press **Enter** | block NOT created; Enter only confirms the field                                      | 🎯 | 🎯 |
| A2 | Texture tab → `#RRGGBB` field + **Use hex**          | button sits beside the field; cube bg turns that colour                               | 🎯 | 🎯 |
| A3 | Load a transparent-area PNG → click a colour swatch  | image stays, colour fills **behind** transparent parts                                | 🎯 | 🎯 |
| A4 | Fill sections, look around                           | left tab green ✔ when a section has a real value; panels on dark cards; hover → hints | 🎯 | 🎯 |
| A5 | Click **Category** tab                               | categories as chips; click chip → assigns (gold); acts on real server categories      | 🎯 | 🎯 |

## B · HUD templates + shape backgrounds · 🧊 0/5 (Parked)

> 💡 Extends the Lego HUD. **Additive** — existing bricks + editor menu untouched.
> 🧰 `/cb edithud`.

| # | Action                                                         | Expected Result                                                                         | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | `/cb edithud` → add a fresh brick (or `[Reset]`)               | brick bg = rounded **pill** + thin blue stripe; ⚙ inspector has a shape control         | 🎯 | 🎯 |
| B2 | Select a brick → ⚙ → **Shape**                                 | cycles Pill / Glow box / Box / Plain; **Accent ■** sets stripe colour; relog → persists | 🎯 | 🎯 |
| B3 | Load a HUD saved before this jar                               | loads, no crash; old bricks show pill look; positions/styles unchanged                  | 🎯 | 🎯 |
| B4 | `[+ Add brick]` → **Template** → text `{name} [{id}]` → aim    | shows live name/id; off a custom block, lines hide                                      | 🎯 | 🎯 |
| B5 | Template brick ⚙ → click `{light}` / `{coords}` / `{hardness}` | token inserts into text box and resolves in-game                                        | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | What it'll do                                                       | Spec             |
| --- | ------------------------------------------------------------------- | ---------------- |
| C | Recolor half of the fold — `/cb recolor` HSL/tone folds into the Studio (rename + Shape done) | SCREENS §G27.11  |
| D | Real top-right `CbToast` replaces in-screen chat messages           | SCREENS §G27.13  |
| E | Studio Polish Pass — red+black theme, cards, hover-help             | SCREENS §G27.6.P |
| F | Onboarding tutorial + Achievements gallery screens                  | SCREENS §G27.16  |
| G | `/cb animation` hub / timeline editor                               | SCREENS §G27.15  |
| H | red+black restyle + movable bars + Settings for older screens       | SCREENS §G27.8   |
| I | Studio more — Edit Mode · Editing (Paint + Resize) · FX/Behavior/Lore tabs | SCREENS §G27.9–10 |
| M | Text-overlap fix + `CbForm` field-layout standard + `verifyScreenFields` gate | SCREENS §G27.20  |
| N | Advanced Per-Face Customization — 3D polygon clicker, directional light, GIFs | SCREENS §G27.18  |
| O | Studio UI Overhaul — 4-Pillar Tabs + Editing Workspace                 | SCREENS §G27.19  |
| P | Enterprise Config GUI — 6 Tabs, Search, Visual Pickers, Backups, Golden Pulse | SCREENS §G27.21  |
| R | **Diagnostics Screen** — one incident, opened by code, + a history log tab   | SCREENS §G27.31  |

**§R · Diagnostics Screen** — folded from Group 04 §C on 2026-07-15. The chest GUI it replaces dumps **every**
incident at once and spams chat; that was never a chat bug, it was a missing screen.

| #  | Action                                     | Expected Result                                                                 | SP | MP |
| -- | ------------------------------------------ | ------------------------------------------------------------------------------- | -- | -- |
| R1 | Trigger an error (`/cb retexture <id> https://bad.invalid/x.png`), click **⊙ Details** | a Screen opens showing **only that one incident** — what broke, when, the command, the stack behind a fold, and a copy button | 🟥 | 🟥 |
| R2 | Watch chat while R1 runs                   | chat keeps **one** plain-English line + the pasteable code, and nothing else. No dump, no spam — the screen IS the output | 🟥 | 🟥 |
| R3 | Open the **History** tab                   | every recorded incident, newest first, each row opening into the R1 detail view | 🟥 | 🟥 |
| R4 | Check the tab rail                         | tabs on the **LEFT** (mod-wide rule), red+black frame like every other screen    | 🟥 | 🟥 |

> 💡 Group 16 keeps `IncidentRecorder` — it records exactly as it does today. Group 27 owns only the screen.

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

| § | What it was                                                | Spec             | Tested / Outcome          |
| --- | ------------------------------------------------------------ | ---------------- | ------------------------- |
| A | Studio — 5 fixes (Enter · hex · bg-behind-image · UI ✔ · Category) | SCREENS §G27.1–4 | 🧊 parked (dev paused)   |
| B | HUD templates + shape backgrounds                          | SCREENS §G27.14  | 🧊 parked (dev paused)   |
| J | Earlier studio + 5 screens (arabic · recolor · eyedrop · edithud) | SCREENS          | ✅ 2026-06-15 (reverted) |

</details>

🔗 **Related Doc:** [GROUP_27_SCREENS.md](../groups/GROUP_27_SCREENS.md)

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| G07-HUB | §T (T1–T25) | Bulk Operations Hub — both tabs rebuilt to the locked mockups (§G27.22 rotating-cube grid + left info panel + chips + Sort/Select ▾ + centered dimmed History popup; §G27.22b 10-op LEFT rail incl. Recolor + include-checkbox rows + RESULT PREVIEW + Execute sweep) | 🟢 built 2026-07-12, retest in one batch |
| Bd2-glow | Bd2 | **Edit → glow** control doesn't exist on the row/panel — not a bug, it was never built | Open |
| Bd3-rename | Bd3 | Rename op only supports prefix/suffix/replace fragments; owner wants a full-name-rename mode AND a per-block small textbox mode (not force-bulk-to-one-name) — needs a design discussion, not a quick fix | ❔ discuss |
| Bd4-polish | Bd4/Bd4a | Move functionally passes but panel/flow is rough — owner wants to talk through polish | ❔ discuss |
| Bd-popups | T (general) | Owner: **all** bulk-op popup menus (Move target picker, etc.) are bad — needs a design discussion across the board, not per-row fixes | ❔ discuss |
| Bd6-export | Bd6 | Export panel reads **`affects 0 of 3`** even with blocks ticked (see owner screenshot) — Execute count not wired to the ticked set | Open |
| Bd9-reid | Bd9/Bd9a | Owner: the `{n}` pattern box ("Pattern (n) numbers") is unusable UX — wants a small textbox **beside each block row** to bulk re-ID directly, not one shared pattern field | ❔ discuss |
| BdR-recolor | BdR | Recolor: block cubes don't update live as the hue slider moves (see owner screenshot) — needs major rework, not a small fix | Open |
| Bd22-copy | Bd22 | Functionally correct (`_copy` → `_copy2`), but the **Result Preview text** shows `_copy_copy` instead of the real `_copy2` outcome — display-only bug | Open |
| Bd23-feedback | Bd23 | Undo/Redo functionally passes but the toast + feedback quality needs improvement | 🛠️ polish wanted |
| G07-X3 | T24 | conflict/no-op feedback needs **both** an in-screen Result modal AND the chat log (not one or the other) | 🎯 built, retest |
| C1/C4 | C1/C4 | Stairs (and other multi-box shapes) render disjointed in the studio 3D cube | Open |
| C1 | C1 | Link line under the block: not clickable + hard to see — wants clickable + clearer (scope: ALL screens, shared helper) | Open |
| C3 | C3 | Block-picker chest GUI needs a full revamp + a search button (reuse the bulkdelete search) — do inside the Recolor-fold slice | Open |
| K1 | K1 | Rename half-done — "Live Recolour" still shown in 6 user-facing labels (recolor screen ×3, picker title, ColorsMenu + EditorMenu buttons) | Open |
| L6 | L6 | Merge/Move popup didn't list every category (owner MP) | **Batch 2 FIXED — retest.** Root: the picker came from `ClientSlotCache.categories()`, which only listed categories with ≥1 block. Fix: categories are now server-real on creation (`exists` flag + `_categories` sync), so the picker lists all of them incl. 0-block ones. |
| L9 | L9 | "Deleting a category makes the inside blocks disappear" (owner MP) | **Batch 2 FIXED — retest.** Blocks were never lost (server kept them uncategorized); the pane just went blank. Fix: `doDelete()` now auto-selects the `(uncategorized)` bucket so the freed blocks stay visible. |
| L10 | L10 | "Move picked block" flow is very bad, needs full revamp (owner MP) | **Batch 2 FIXED — retest.** Full revamp: drag a block row onto a left-list category (eased lime border + drop pulse + "+1" count bump), OR the old click-then-Move popup path. New `CategoryHubDragDrop` helper. |
| L11/new-cat | L11 + "new category button" + "hard to put blocks in a new category" | Search confusing; "+ New category" needs revamp; hard to fill a new category (owner MP) | **Batch 2 FIXED — retest.** Root was the same as L6 (new categories were client-only `pendingNew`, never sent to server). Fix: "+ New category" sends a `create` op → the category is real immediately and accepts drag/Move drops. `pendingNew` removed. |
| L11b/search-in-cat | L11b (new) | Owner wants to search for blocks **inside** a selected category's block list | **Batch 2 BUILT — retest.** New search box above the BLOCKS panel filters that category's block list by name/id. |
| L12 | L12 | Uncategorized bucket / "Empty into…" needs a full revamp (owner MP) | **Batch 2 FIXED — retest.** Same categories-real fix — the "Empty into…" target picker now lists every real category; drag-drop works from the bucket too. |
| F2/K7 | F2/K7 | Help popup still shows text from behind, 4th time — on a FRESH client jar (owner MP) | **Batch 2 FIXED — retest.** Root (agent-confirmed): `ctx.draw()` does NOT keep earlier deferred text behind a later deferred `ctx.fill`. Fix: scrim drawn as an IMMEDIATE textured quad via new `CbImmediateFill` (the same trick `PreviewCube` uses) in both `CbHelpOverlay` and `CbPopupPicker`. |
| F3-gif | F3 | Recolor/shape preview works for normal blocks but not GIF/animated (owner MP) | **Batch 2 FIXED — retest.** Root: animated blocks bake as a roughly-square frame GRID (not a tall strip), so the `h > w` guess never fired. Fix: `LocalTexturePreview` reads the `slot_N.grid.json` sidecar and crops cell 0. |
| F5 | F5 | Recolor TONE TOOLS panel still looks bad (owner: partial) | **Design LOCKED 2026-07-12** — full v2 panel spec'd (`GROUP_27_SCREENS.md` §G27.11c: card frame, 3D cube tie-in + compare slider, gradient tracks, scrubber values, presets+history, hex+apply). Not built. |
| F6 | F6 | create Identity labels still look bad (owner: partial) | **Design LOCKED 2026-07-12** — full v2 spec'd (`GROUP_27_SCREENS.md` §G27.11d: 3 equal peer sections id/name/link, live id-availability check, auto-slug from name, new Original Image Link field with thumbnail + copy/open). Not built. |
| K5 | K5 | `/cb edithud` passes but looks plain/bad — full revamp wanted | **Design LOCKED 2026-07-12** — full v2 spec'd (`GROUP_27_SCREENS.md` §G27.11e: canvas-first layout, floating contextual toolbar w/ clamp rule, layers rail with collapse + global settings strip, Compact mode). Not built. |
| K6 | K6 | Top-right toast rectangle + lime — appearance revamp wanted | **Design LOCKED 2026-07-12** — full v2 spec'd (`GROUP_27_SCREENS.md` §G27.11f: severity-coded cards, pinned/progress toast types, duplicate/group collapsing, per-toast actions, settings panel). Not built. |

---

## [G27.18] Advanced Per-Face Customization (Planned)

| ID | Group | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| N1 | UI/UX | Open `/cb create`. Left-click a face on the spinning preview cube. Verify it gets a highly visible glowing outline. | ⏳ Planned | ⏳ | ⏳ |
| N2 | UI/UX | Drag the cube. Verify the block rotates but no faces are accidentally selected. | ⏳ Planned | ⏳ | ⏳ |
| N3 | UI/UX | Right-click the cube. Verify spinning pauses/unpauses. | ⏳ Planned | ⏳ | ⏳ |
| N4 | UI/UX | Multi-select: Click Top, then click North. Verify both have glowing outlines. Change texture. Verify both faces update. | ⏳ Planned | ⏳ | ⏳ |
| N5 | UI/UX | Copy properties: Select a fully configured face, then click a blank face. Verify the blank face inherits all properties. | ⏳ Planned | ⏳ | ⏳ |
| N6 | UI/UX | Complex shape: Create a Stair block. Verify you can click individual exposed polygon surfaces on the step. | ⏳ Planned | ⏳ | ⏳ |
| N7 | Physics | Apply 6 distinct GIFs to 6 faces. Verify they play flawlessly and fully synchronized. | ⏳ Planned | ⏳ | ⏳ |
| N8 | Physics | Set Top face to Glass (transparent). Verify you can see the inside of the block, and the inside walls mirror the outside textures. | ⏳ Planned | ⏳ | ⏳ |
| N9 | Physics | Hollow Box: Set North face to "Passable". Verify you can walk straight through the North face into the block, but get blocked by the South face. | ⏳ Planned | ⏳ | ⏳ |
| N10 | Physics | Directional Light: Set East face to Glow 15. Verify the floor to the East lights up. Break block, verify light goes away. | ⏳ Planned | ⏳ | ⏳ |
| N11 | Sound | Set Top to Glass, Sides to Wood. Verify walking on top sounds like glass, touching sides sounds like wood. Break block, verify mixed sound plays. | ⏳ Planned | ⏳ | ⏳ |
| N12 | UI/UX | Broken Link: Apply a 404 URL. Verify face flashes red in the Studio UI and auto-selects for fixing. | ⏳ Planned | ⏳ | ⏳ |
| N13 | Menus | Inventory: Verify the held item displays a standard 3D isometric view with the customized faces. | ⏳ Planned | ⏳ | ⏳ |
| N14 | Menus | Hub: Open `/cb hub`. Verify the block renders correctly as a 3D item and the menu does not lag. | ⏳ Planned | ⏳ | ⏳ |
| N15 | Cmds | Commands: Verify `/cb retexture` still only applies to the entire block as a fallback. | ⏳ Planned | ⏳ | ⏳ |

## [G27.19] Studio UI Overhaul & 3D Design Editor (Planned)

| ID | Group | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| O1 | UI | Open Studio. Verify 10 tabs are condensed into 4: Identity, Design, Shape, Behavior. | ⏳ Planned | ⏳ | ⏳ |
| O2 | UI | In the **Editing** tab, verify the two internal modes are exactly **Paint** and **Resize**; Animation remains a separate top-level tab. | ⏳ Planned | ⏳ | ⏳ |
| O3 | Resize | Resize Workspace: load a source, verify width/height fields, 64/128/256/512 presets, Custom size, and the linked-aspect default. | ⏳ Planned | ⏳ | ⏳ |
| O4 | Resize | Change the fit method to Keep shape, Crop, and Stretch. Verify the before/after preview changes to match each choice. | ⏳ Planned | ⏳ | ⏳ |
| O5 | Paint | Editing Paint Workspace: verify the camera stays on a movable flat 2D canvas with pan, zoom, and Fit to Screen. | ⏳ Planned | ⏳ | ⏳ |
| O6 | Paint | Glass Eraser: Paint a solid color. Select Glass Eraser and wipe. Verify a genuine transparent hole is punched. | ⏳ Planned | ⏳ | ⏳ |
| O7 | Paint | Synchronized Mirroring: Multi-select Top + North. Draw on Top. Verify brush strokes mirror instantly onto North. | ⏳ Planned | ⏳ | ⏳ |
| O8 | Keys | Shortcuts: Verify `[ ]` changes brush size, `Ctrl+Z` undoes, and holding `Spacebar` allows camera panning. | ⏳ Planned | ⏳ | ⏳ |
| O9 | Anim | Animation Workspace: Load a GIF. Open Film tab. Verify a massive horizontal timeline spans the bottom screen. | ⏳ Planned | ⏳ | ⏳ |
| O10 | Paint/Anim | Verify a still-image Paint reference underlay can be shown with adjustable opacity. GIF frame painting remains unavailable in this release. | ⏳ Planned | ⏳ | ⏳ |

## [G27.10] Studio Editing Workspace (Planned)

> **Scope:** one Editing tab inside Block Studio, with Paint and Resize modes. These rows are planned until
> the owner confirms each slice in-game. The screen spec is `GROUP_27_SCREENS.md` §G27.10; texture-size
> mechanics remain Group 10-owned and GIF mechanics remain Group 14-owned.

| ID | Area | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| E1 | Entry | Run `/cb create`, choose **Editing**, and verify one Editing tab opens with exactly **Paint** and **Resize** mode choices. | ⏳ Planned | ⏳ | ⏳ |
| E2 | Entry | Run `/cb paint` and verify it opens the same Editing tab with Paint selected; no duplicate Paint screen appears. | ⏳ Planned | ⏳ | ⏳ |
| E3 | Entry | Run `/cb resize` and verify it opens the same Editing tab with Resize selected; no duplicate Resize screen appears. | ⏳ Planned | ⏳ | ⏳ |
| E4 | Create | In a new block flow, load a still image and verify Paint and Resize can be used before publishing. | ⏳ Planned | ⏳ | ⏳ |
| E5 | Existing | Open an existing block in Edit Mode and verify its stored texture and dimensions are pre-filled without quality loss. | ⏳ Planned | ⏳ | ⏳ |
| E6 | Navigation | Switch between Paint and Resize after making a working change. Verify the working result, mode state, and preview are retained. | ⏳ Planned | ⏳ | ⏳ |
| E7 | Canvas | In Paint, verify the primary surface is a stable flat 2D viewport. Drag to pan, zoom in/out, and use Fit to Screen; texture data must not change from navigation. | ⏳ Planned | ⏳ | ⏳ |
| E8 | Paint | Use pen, eraser, fill, eyedropper, line, rectangle, symmetry, and brush size. Verify each changes only the intended working texture area. | ⏳ Planned | ⏳ | ⏳ |
| E9 | Paint | Paint transparency and erase pixels. Verify transparent pixels show on the checkerboard and remain transparent in the live preview. | ⏳ Planned | ⏳ | ⏳ |
| E10 | GIF boundary | Load a GIF in Editing. Verify Paint clearly reports that GIF painting is unavailable, while Resize remains available. | ⏳ Planned | ⏳ | ⏳ |
| E11 | Resize | Verify current width/height, editable numeric fields, 64/128/256/512 presets, Custom size, allowed maximum, and output-size estimate are visible. | ⏳ Planned | ⏳ | ⏳ |
| E12 | Resize | Change width with proportions linked by default. Verify height follows; unlock proportions and verify width and height can then be edited independently. | ⏳ Planned | ⏳ | ⏳ |
| E13 | Resize | Test Keep shape, Crop, and Stretch on the same still image. Verify the before/after preview shows fit-without-distortion, edge removal, and distortion respectively. | ⏳ Planned | ⏳ | ⏳ |
| E14 | Resize | Resize a still image, click Apply, and verify the actual new or existing block uses the requested dimensions and fit method. The Editing tab stays open. | ⏳ Planned | ⏳ | ⏳ |
| E15 | GIF Resize | Resize a GIF and verify every frame uses the same dimensions and fit method; no frame is skipped, duplicated, or left at the old size. | ⏳ Planned | ⏳ | ⏳ |
| E16 | GIF preservation | Before and after GIF Resize, compare frame count, frame order, per-frame timing, and loop behavior. Verify all remain unchanged. | ⏳ Planned | ⏳ | ⏳ |
| E17 | Preview | Use the before/after view for Paint, still Resize, and GIF Resize. Verify the original and current result are clearly distinguishable and update after each working change. | ⏳ Planned | ⏳ | ⏳ |
| E18 | Original | Make Paint and Resize changes, then use **Restore Original** in the lower corner. Confirm the deliberate prompt and verify the original texture and dimensions return. | ⏳ Planned | ⏳ | ⏳ |
| E19 | History | Use on-screen Undo/Redo after Paint and Resize. Then use `/cb undo` and `/cb redo`; verify both command paths operate on the same shared history. | ⏳ Planned | ⏳ | ⏳ |
| E20 | Validation | Enter invalid, empty, oversized, and unsupported resize values. Verify no server change occurs and the screen gives a human explanation and a valid next action. | ⏳ Planned | ⏳ | ⏳ |
| E21 | Multiplayer | Apply a Paint or Resize change on a server, then have a second player view the block. Verify the applied result syncs without a rejoin and no unfinished working preview leaks to the other player. | ⏳ Planned | ⏳ | ⏳ |

## [G27.21] Enterprise Config GUI Redesign (Planned)

| ID | Group | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| P1 | UI | Run `/cb admin`. Verify UI blurs world background (Glassmorphism), scales to 80%, and plays dark red background animation. | ⏳ Planned | ⏳ | ⏳ |
| P2 | T1 | Server Status: Verify it accurately identifies SP vs MP, and Capacity bar displays Used/Max slots. | ⏳ Planned | ⏳ | ⏳ |
| P3 | T1 | Capacity Colors: Force Used slots > 86%. Verify Capacity progress bar dynamically turns Red. | ⏳ Planned | ⏳ | ⏳ |
| P4 | T2 | General: Toggle Typo Correction to On. Open Advanced Dictionary, verify custom synonyms can be added/saved. | ⏳ Planned | ⏳ | ⏳ |
| P5 | T3 | Backups: Open Backups tab. Verify a scrollable "Live Backup Manager" list exists with a red Restore button. | ⏳ Planned | ⏳ | ⏳ |
| P6 | T3 | History: Click "Purge History Cache". Verify double-confirm prompt appears, and Anvil clank sound plays. | ⏳ Planned | ⏳ | ⏳ |
| P7 | T4 | Visuals: Open Variant Colours. Verify a massive visual RGB Color Picker (photoshop-style) opens instead of a text field. | ⏳ Planned | ⏳ | ⏳ |
| P8 | T5 | Vault: Click "Login to Vault". Verify an in-game OAuth-style browser window successfully opens and authenticates. | ⏳ Planned | ⏳ | ⏳ |
| P9 | T5 | Webhooks: Paste a Discord URL and click "Test Webhook". Verify the customized test payload correctly sends. | ⏳ Planned | ⏳ | ⏳ |
| P10 | T6 | Developer: Open Dev Tab (Admin only). Verify live RAM usage chart updates in real-time. | ⏳ Planned | ⏳ | ⏳ |
| P11 | Cmds | Aliases: Verify `/cb config`, `/cb settings`, and `/cb admin` all open the exact same screen. | ⏳ Planned | ⏳ | ⏳ |
| P12 | Core | Global Settings: Open any screen. Click the `⚙` gear. Verify "Accent Color" and "UI Volume Slider" apply universally. | ⏳ Planned | ⏳ | ⏳ |
| P13 | Core | Search: Type in Search Bar. Verify UI jumps to the result and draws a glowing golden border around the field. | ⏳ Planned | ⏳ | ⏳ |
| P14 | Core | Unsaved: Alter `maxSlots`. Verify Apply button turns orange. Press `ESC`. Verify red "Discard or Save?" popup triggers. | ⏳ Planned | ⏳ | ⏳ |
| P15 | Sec | True Privacy: De-op a player. Verify they only have access to Tab 1. Tabs 2-6 show Padlocks and actual values read `Hidden`. | ⏳ Planned | ⏳ | ⏳ |

## [G27.27] Undo/Redo & History Log Screens (Migrated from Group 02)

| ID | Group | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| R1 | UI | Run `/cb undogui`. Verify a Screen interface opens showing the player's personal undo stack. | ⏳ Planned | ⏳ | ⏳ |
| R2 | UI | Run `/cb history`. Verify a Screen interface opens showing the mutation log. | ⏳ Planned | ⏳ | ⏳ |
| R3 | Cmds | In `/cb history`, verify advanced dropdowns can filter by Player, Date, and Block Type. | ⏳ Planned | ⏳ | ⏳ |
| R4 | Sec | As an OP, select a row in `/cb history`. Verify the [Rollback] button appears and successfully reverts that specific edit. | ⏳ Planned | ⏳ | ⏳ |

## [G27.32] Ultimate Premium UI Features (Planned)

| ID | Group | Test Case | Status | SP | MP |
|---|---|---|---|---|---|
| S1 | UI | Trigger a success event. Verify a Modern Toast slides in from the top-right with a popping green checkmark micro-animation. | ⏳ Planned | ⏳ | ⏳ |
| S2 | UI | Toast Expiry: Wait for a toast to expire. Verify it physically detaches and falls off the bottom of the screen with gravity. | ⏳ Planned | ⏳ | ⏳ |
| S3 | Physics | Toast Swatting: Flick your mouse cursor at a falling toast. Verify you can physically 'swat' it off the screen edge. | ⏳ Planned | ⏳ | ⏳ |
| S4 | UI | Overload: Trigger 15 events in 1 millisecond. Verify a single "OVERLOAD" mega-toast appears, a 1-second subtle red vignette shader triggers on screen, and a bass-boosted sound plays instead of 15 overlapping sounds. | ⏳ Planned | ⏳ | ⏳ |
| S5 | UX | Wiki Tome: Run `/cb help`. Verify a hyper-realistic 3D book opens, and you can physically drag pages to turn them. | ⏳ Planned | ⏳ | ⏳ |
| S6 | UX | Embedded Playgrounds: Find a slider inside a Wiki text paragraph. Drag it. Verify the 3D block next to the text updates live. | ⏳ Planned | ⏳ | ⏳ |
| S7 | MP | Google Docs Editor: Two admins open the same Wiki page to edit. Verify both admins can see each other's live glowing cursors typing. | ⏳ Planned | ⏳ | ⏳ |
| S8 | UX | Admin Media: Drag a `.png` file from your actual Windows Desktop directly onto the Minecraft window. Verify the image embeds instantly into the Wiki editor. | ⏳ Planned | ⏳ | ⏳ |
| S9 | Video | First Join Welcome: Join as a new player. Verify a 10-second cinematic hype video aggressively force-opens and cannot be skipped. | ⏳ Planned | ⏳ | ⏳ |
| S10 | Combat | Welcome Invincibility: Get attacked by a zombie while the 10-second cinematic is playing. Verify you are 100% invincible and take no damage until it finishes. | ⏳ Planned | ⏳ | ⏳ |
