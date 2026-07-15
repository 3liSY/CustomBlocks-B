# Group 06 — Tools, Dynamic Glow & Creative Tab

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 7 (Tools) build-verified.
>
> **Objective:** Merge the Lumina Brush and Chisel into a single configurable "Omni-Tool". Add held-block dynamic glow emission. Audit and clean up the dedicated tools creative tab. Restore tool-give shortcuts. Finalize tool consolidation per locked decisions.
>
> **Source issues:** 17.8 (held block dynamic glow), 17.9 (dedicated creative tools tab), 17.10 (Chisel + Lumina unification), Group F (tool-give shortcuts), Q8 (tool consolidation), Decision §7, §8, §9, §C
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## UI medium audit (2026-07-09)

RecolorConfirmMenu, HexRecolorConfirmMenu, OmniMenu → **keep chest GUI**. Owner: design is poor, flag for
a `GUI_DESIGN_GUIDE.md` polish pass — applies to every chest GUI kept across this whole audit, not just
these.

HexColorsMenu ("Variant Colours" hex editor), CustomColorMenu ("Color Studio") → **Screen — not built**.
Both currently fake real text-entry with an anvil-rename prompt; owner wants a proper Screen text
field(+ live colour preview) instead.

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Lumina Brush | Cycled glow 0→4→8→12→15 | Working physical item | Merged into Omni-Tool as "Glow Mode" |
| Amethyst Chisel | Cycled hardness presets | Working physical item | Merged into Omni-Tool as "Hardness Mode" |
| Omni-Tool | Existed as configurable multi-mode tool | Missing (two separate tools) | Rebuilt: one item, hot-swappable modes via Shift+RClick |
| Deleter | Right-click to remove block instantly | Working physical item | Kept as separate item (by decision) |
| Tool-give shortcuts | `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle` | Missing — had to use `/give @s customblocks:…` | Restored as `/cb brush` → gives Omni-Tool, etc. |
| Held block glow | Torch-like light emitted when holding glowing block | Missing | Networked client-side mixin: server packet + client light render |
| Creative tools tab | "CustomBlocks Tools" tab existed | Two tabs (Blocks + Tools) | Single "CustomBlocks Tools" tab: Omni-Tool, Deleter, Square, Triangle. NO blocks in tools tab. Sortable via config. |
| Golden Hexagon | Admin tool (physical item) | Framework only | Merged into Omni-Tool as "Admin Mode" — not a separate item |
| Rainbow Rectangle | Area tool (physical item) | Framework only | Merged into Omni-Tool as "Area Mode" |
| Diamond Triangle (Wand) | Eyedrop/wand tool | Framework only | Merged into Omni-Tool as "Eyedrop Mode" |
| Tab icon | `/cb settabicon <url>` | Missing | **Owned by G25** (decision C 2026-06-21) — not a G06 tool |
| Custom-hex tools | `/cb customtriangle <#RRGGBB>` — gave a Square + Triangle tinted to a custom hex instead of the fixed presets | Missing | **Moved here from G08 2026-07-11** (was misfiled as a "triangular block shape" — it's actually a recolor-tool variant, no block-shape feature exists in the legacy code) — not built |
| Triangle fill mode | `/cb trianglemode edge\|full` — edge-only vs whole-face paint fill for the Triangle recolor tool | Missing | **Moved here from G08 2026-07-11** (was misfiled as "triangle placement mode") — not built |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Omni-Tool | Physical item — Shift+RClick opens mode config chest GUI |
| Omni-Tool modes | Glow, Hardness, Delete, Paint, Area, Eyedrop, Admin |
| Held block glow | Dynamic light emitted from hand when holding glowing custom block |
| Creative tools tab | Clean tab: Omni-Tool, Deleter, Square, Triangle only |
| Tool-give shortcuts | `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle`, `/cb rectangle`, `/cb hexagon` |
| Mode visual feedback | Omni-Tool changes texture/color per active mode |

---

## Implementation Requirements

### 1. Omni-Tool — Physical Item

- Single item: `customblocks:omni_tool`.
- **Right Click on a block** → performs the active mode's action.
- **Shift + Right Click on a block** → opens the Omni-Tool Config chest GUI.
- The item texture changes dynamically (via model predicates) to reflect the active mode.
- Mode is stored per-player in `config/customblocks/data/magic_items.json`.

**Modes and their behavior:**

| Mode | Icon color hint | Right-click behavior |
|---|---|---|
| Glow | Yellow/gold | Cycle glow: 0→4→8→12→15→0 (Sneak = reverse) |
| Hardness | Gray/iron | Cycle hardness presets: instant→soft→stone→hard→tough→unbreakable |
| Delete | Red | Remove placed custom block instantly (no drop), even unbreakable |
| Paint | Blue | Apply the last-used URL as texture to the clicked block |
| Area | Green | Mark area corners for bulk operations |
| Eyedrop | Purple | Copy texture/attributes from the clicked block to clipboard |
| Admin | Orange | Open admin panel for the clicked block |

### 2. Omni-Tool Config Chest GUI

Opened with Shift+RClick. Contains:
- Mode selector slots (one per mode, click to switch).
- "Configure active mode" slot → opens mode-specific sub-settings.
- "Cycling behavior" slot → customize which values cycle for Glow and Hardness modes.
- "Save default" slot → save current mode as default for this player.

### 3. Held Block Dynamic Glow

Implementation: **Networked Client-Side Mixin** (Decision §7):
1. Server detects when a player is holding a custom block item with `glow > 0`.
2. Server sends a small packet to all nearby clients: "Player X is holding a block with light level Y."
3. Clients render a dynamic light source at the player's hand position using a Mixin.
4. Same effect when placing — light level jumps from hand position to block position.
5. Zero server TPS lag (light rendered client-side only).

### 4. Creative Tools Tab

Tab name: "CustomBlocks Tools"
Contents: Omni-Tool, Deleter Tool, Green Square Marker, Yellow Triangle Marker. Nothing else.
Sort mode configurable in config: `toolTabSort` — `default` (fixed order) or `alphabetical`.

### 5. Tool-Give Shortcuts

All shortcuts give the correct item:

| Command | Item given |
|---|---|
| `/cb brush` | Omni-Tool (Glow mode pre-selected) |
| `/cb chisel` | Omni-Tool (Hardness mode pre-selected) |
| `/cb deleter` | Deleter Tool |
| `/cb square` | Green Square Marker |
| `/cb triangle` | Yellow Triangle Marker |
| `/cb rectangle` | Omni-Tool (Area mode pre-selected) |
| `/cb hexagon` | Omni-Tool (Admin mode pre-selected) |

---

## Setup

```
/cb create g06a GlowBlock
/cb setglow g06a 12
/cb give g06a
```

Place `g06a` nearby. Pick up the block item in your inventory.

---

## Test G06.1 — Omni-Tool give

```
/cb brush
```

**Expected:** Omni-Tool given with Glow mode active. Item name shows current mode (e.g., "Omni-Tool [Glow Mode]").

**Pass:** Item given with correct name and mode.
**Fail:** Wrong item given, error, or missing command.

---

## Test G06.2 — Omni-Tool right-click cycles glow

Hold the Omni-Tool in Glow mode. Right-click on the placed `g06a`.

**Expected:** Glow cycles: `0 → 4 → 8 → 12 → 15 → 0`. Chat or action bar shows current glow level after each click.

**Pass:** Glow increments correctly on each right-click.
**Fail:** Nothing happens, wrong increment, or error.

---

## Test G06.3 — Omni-Tool config chest GUI opens

Hold Omni-Tool. Shift+Right-click on any custom block.

**Expected:** A chest GUI opens with mode selector slots, cycling config, and save default slot.

**Pass:** Chest GUI opens with correct contents.
**Fail:** Nothing happens or error.

---

## Test G06.4 — Omni-Tool mode switch in GUI

In the Omni-Tool config GUI, click the "Hardness" mode slot.

**Expected:** Active mode changes to Hardness. Item texture/name updates to reflect "Hardness Mode". Close GUI and right-click a block — hardness cycles.

**Pass:** Mode switch works, item reflects new mode, right-click cycles hardness.
**Fail:** Mode doesn't change, or right-click still does old mode behavior.

---

## Test G06.5 — Deleter give and use

```
/cb deleter
```

Hold the Deleter. Right-click on the placed `g06a`.

**Expected:** `g06a` is removed instantly. No block drop. Removal is undoable (`/cb undo` restores it).

**Pass:** Block removed instantly. Undo works.
**Fail:** Block not removed, or drop appears.

---

## Test G06.6 — Held block dynamic glow

Hold the `g06a` item (glow = 12) in your main hand. Stand in a dark area (night or underground).

**Expected:** A light level 12 glow radiates from your hand position, illuminating nearby blocks. Other players nearby also see the light.

**Pass:** Dynamic light visible from hand. Other players see it too.
**Fail:** No light emitted from hand.

---

## Test G06.7 — Glow on placement

> **REWORDED 2026-06-21:** two separate things were conflated. (a) The placed block emitting
> **ground light** is normal block lighting and **works**. (b) A smooth **hand→placed transition**
> only matters once held-block glow (G06.6) works — and G06.6 is currently broken, so the transition
> half is N/A until G06.6 is fixed.

Hold the `g06a` item. Place it on a wall in a dark area.

**Expected:** The placed block lights its surroundings (ground light). Once G06.6 works, the light
should also transition smoothly from hand to placed position.

**Pass:** Placed-block ground light works. (Hand→place transition gated on G06.6.)
**Fail:** Placed block emits no light at all.

---

## Test G06.8 — Creative tools tab contents

> **CORRECTED 2026-06-21 (developer):** "exactly 4 items" was wrong. The tab houses MORE than 4
> tool items and that is **intended/correct**. The real check is: it's the tools tab, contains the
> tool items, and does NOT contain custom blocks.

Open creative inventory. Navigate to "CustomBlocks Tools" tab.

**Expected:** Tab contains the tool items (Omni-Tool, Deleter, Square, Triangle, and the other tool
items — more than 4 is fine). No custom blocks in this tab.

**Pass:** Tools tab present with tool items; no custom blocks mixed in.
**Fail:** Custom blocks present in the tools tab, or tab missing.

---

## Test G06.9 — Tool-give shortcuts all work

```
/cb chisel
/cb square
/cb triangle
/cb rectangle
/cb hexagon
```

**Expected:** Each gives the correct item with the correct pre-selected mode.

- `/cb chisel` → Omni-Tool [Hardness Mode]
- `/cb square` → Green Square Marker
- `/cb triangle` → Yellow Triangle Marker
- `/cb rectangle` → Omni-Tool [Area Mode]
- `/cb hexagon` → Omni-Tool [Admin Mode]

**Pass:** All 5 commands give correct items with correct modes.
**Fail:** Any command missing, wrong item, or wrong mode.

---

## Group 06 Verdict

> ⚠️ **Synced 2026-07-10:** this table is orphaned — `docs/testing/GROUP_06_TESTING_GUIDE.md` moved to a
> lettered section scheme (A–O, CV, etc.) with current status **0% (0/79 passed)**, all 🟥, and does not
> map 1:1 to these G06.x rows. Treat the TG as current source of truth; rows below are historical. G06.5 in
> particular is stale — see **§G06-2** further down this doc, which diagnoses the Deleter as broken
> (leftover ghost blocks, texture bleed) contradicting the ✅ below.

| Test | Description | Result |
|---|---|---|
| G06.1 | Omni-Tool given via `/cb brush` | ✅ in-game (2026-06-21) — whole Omni-Tool mechanism to be reworked later |
| G06.2 | Right-click cycles glow | ✅ in-game (2026-06-21) — Omni rework pending |
| G06.3 | Config chest GUI opens via Shift+RClick | ✅ in-game (2026-06-21) — Omni rework pending |
| G06.4 | Mode switch in GUI works | ✅ in-game (2026-06-21) — Omni rework pending |
| G06.5 | Deleter removes block instantly, undoable | ⚠️ stale ✅ (2026-06-21) — **contradicted by §G06-2 below**, which diagnoses the Deleter as broken (leftover ghost blocks, texture bleed onto wrong block); treat as 🔍 diagnosed, not built, until reconciled |
| G06.6 | Held block emits dynamic glow | ❌ in-game (2026-06-21) — NO glow from hand; feature not working |
| G06.7 | ~~Glow transitions on placement~~ → placed block emits ground light | ✅ in-game (2026-06-21) — placed-block ground light works; the hand→place *transition* depends on G06.6 (broken). Spec reworded |
| G06.8 | Creative tools tab contents | ✅ in-game (2026-06-21) — spec corrected: tab houses MORE than 4 items, which is intended/correct |
| G06.9 | All tool-give shortcuts work | ✅ in-game (2026-06-21) — work; shortcuts to be reworked later |

**Group 06 passes when the developer confirms all tools work, glow emits from hand, and the creative tab is clean.**

If anything shows ❌ — paste:
1. The exact command or action
2. What happened vs what was expected
3. Last 20 lines of `latest.log`

---

## Colour Squares — 2026-06-20 additions

The colour **Squares** (the M3 colour-swap tools, `item/ShapeToolItem.java`) gained two changes this date.
Tests → `GROUP_06_TESTING_GUIDE.md` §J.

1. **Squares recolour placed auto-join Arabic letters.** Previously a Square only matched `SlotBlock` and
   did nothing on a letter. Now a Square on an `ArabicLetterBlock` recolours it to the Square's colour —
   **colour only**: it sets the letter's per-block colour and syncs, never touching the blockstate or the
   join flow, so FACING / form / neighbour joins are untouched. Green/Yellow/Red/Black map 1:1 to the
   bundled letter colours; instant, no pack reload. Full design → `GROUP_13_ARABIC.md` → **O11**.
2. **Cleaner swap wording.** The swap feedback now reads `Swapped to <DisplayName>` (clean name, not the
   raw id) and the same-colour case reads `Already <DisplayName>`. Plus all hotbar popups dropped the
   `[CB]` tag (owner request) — that part lives in Group 04 (`Chat.tool`).

---

## Follow-ups (from in-game test 2026-06-21)

8 of 9 tests pass; G06.6 fails. Open work:

- **G06.6 — held-block hand glow BROKEN (build fix).** Holding a glowing block emits no light from
  hand. Networked client-side mixin (§3) not working. Real bug — needs investigation + fix.
- **Omni-Tool full mechanism rework (G06.1–.4, .9).** Tool works, but developer wants the entire
  Omni-Tool mechanism + the tool-give shortcuts redesigned. Scope/design TBD — later.
- **G06.5 — Deleter polish.** Works + undoable, but: (a) weak/absent action-bar + chat feedback,
  (b) resource-pack reload after delete fires too slowly. Improve feedback + speed up the RP refresh.
- **G06.7 spec reworded; G06.8 spec corrected** (tab may house >4 items — intended).

## Cleanup

```
/cb delete g06a
```

---

## G06-1 · Instant Square swap feedback (locked 2026-07-15)

> 🔍 diagnosed — implementation not built. This is owned by Group 06 and tested in TG6 §M.

> *"the action bar after recoloring a block with a square is delayed, should be instant like the recoloring"*

**Scope locked with the owner:** this is a timing/prediction fix for the Square tools, not a Group 04
wording-unification change. It applies to every current and future tool that implements `ColorSwapTool`:
fixed Green/Yellow/Red/Black Squares and custom-hex Squares. It covers normal custom blocks and Arabic
SlotBlocks, in singleplayer/LAN and dedicated multiplayer.

**Current source behavior**

- `ClientSwapPredictor` already paints a valid target block on the client in the same tick as the click.
  Remote clients use the synced `ClientSlotCache`; local/LAN sessions use the in-process `SlotManager`.
- `ColorVariantService.swapPlaced` changes the authoritative server block first, then sends the existing
  hotbar line through `Chat`: `Swapped to <display name>`.
- When the block is already that colour, the server sends the existing line `Already <display name>.`.
- Both lines currently wait for the server round trip. The predictor also returns early for the already-
  correct target, so that line cannot appear immediately today.

**Locked result**

1. A successful Square swap shows the existing `Swapped to <display name>` line at the exact same moment
   as the client-side colour change.
2. A Square used on a block that is already that colour shows the existing `Already <display name>.` line
   at that same moment.
3. Each result appears **once only**. The later matching server confirmation is consumed silently and must
   not duplicate the line.
4. Existing wording, colours, and message meaning stay unchanged. Do not add new error behavior or invent
   failure messages as part of this timing fix.
5. The server remains authoritative for the real block swap; client prediction only removes the visible wait
   and the confirmation reconciles silently.

**Implementation direction**

- Extend the existing `ClientSwapPredictor` path to produce the matching hotbar result for both target cases.
- Resolve the display name from `ClientSlotCache.Entry.name()` on remote sessions and `SlotData.displayName()`
  on local/LAN sessions, so the client uses the exact current server wording.
- Add one prediction/confirmation dedupe gate keyed to the predicted Square result, so the server's later
  identical `Chat` line is not rendered a second time.
- Keep `Chat` as the wording source of truth. Group 04's hotbar unification contract is not changed by this
  slice.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | All Square tools; SP/LAN + dedicated MP; normal + Arabic SlotBlocks | 🔍 diagnosed — not built |

**Touches:** `ColorSwapTool` · `ShapeToolItem` · `CustomColorToolItem` · `ClientSwapPredictor` ·
`ClientSlotCache` · `ColorVariantService.swapPlaced` · existing `Chat` hotbar route.
**Cross-reference:** Group 04 owns the shared hotbar wording contract; Group 13 may verify Arabic-letter
coverage, but neither owns this timing fix.

---

## G06-2 · Deleter doesn't delete properly + buggy (MP)

> 🔍 diagnosed — **build first**; fix with G06-3 (same slot-reuse root); G05-2 + G06-1 share the same `AfterEdit.broadcast` fix

> *"deleter tool doesn't delete the block entirely and it is really really buggy currently"*

**Wanted** (developer, = old mod behavior): right-click wipes the block's **entire definition**, but
the placed block **stays** in the world as a "custom block" showing the **broken black/purple
missing-texture** look. Not vanish, not turn into another block. Also: add the same delete as a
**Delete mode on the Omni-Tool** (today only the standalone red Deleter exists).

**Symptom** — Doesn't delete cleanly. Leftover blank ghosts, ghosts that later change to a different
block's texture, sometimes nothing happens / many clicks, sometimes wrong block. Tested on MP.
Developer confirms it **misbehaves only — no hard crash / kick / freeze**, so the "many clicks / wrong
block" are misbehavior under the same root causes below (verify at build), not a separate crash bug.

**Why** — The new Deleter does step 1 of 3 the old mod did, and adds a corruption bug:

| Step | Old mod | New mod (`DeleterItem.act`) |
|---|---|---|
| Wipe definition (texture/name/settings) | ✅ | ✅ |
| **Refresh the clicked block** → instantly shows black/purple broken | ✅ `setBlockState(pos, …, FORCE_STATE)` | ❌ never touches the world block (ignores `pos`) |
| **Fast client sync** to all players | ✅ broadcast `SlotUpdatePayload("remove", …)` | ❌ only the slow `ResourcePackServer.updatePack()` |

Plus **slot recycling corruption:** `delete()` frees the slot but doesn't guard it. The next
created block reuses that slot number (`nextFreeSlotIndex`), so the leftover ghost wears the new
block's skin. A guard exists (`RetiredSlots`) but only the Arabic batch-retire uses it — the
Deleter doesn't.

**Why MP looks worst** — no block refresh + only the slow pack re-download means the clicked block
keeps its old look until the client re-pulls the pack (the known "RP reload too slow" G06.5
follow-up). Looks like nothing happened, or happens late, or shows stale.

**Fix approach** — Mirror the old mod, in `DeleterItem.act` (and a new Omni-Tool Delete mode that
calls the same path):

1. After wiping the definition, **refresh the clicked block** so it instantly shows the black/purple broken texture.
2. Push a **fast remove-sync** to all clients, not just the slow pack rebuild.
3. **Guard the freed slot** so it isn't instantly reused (the corruption source — see G06-3). **Do NOT route through `RetiredSlots`:** that set also **air-swaps placed copies to nothing** (`ArabicLetterRetirement` turns a placed `slot_N` into air), which would make the developer's placed blocks **vanish** — the opposite of "keep it as a broken block." Use a lighter **reuse-only guard** (skip the index in `nextFreeSlotIndex`, no air-clean).
4. Add **Delete mode** to `OmniToolState.Mode` + the `OmniToolItem` action switch, reusing the same delete.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — MP confirmed (refresh/recycle root causes hit SP too) | 🔍 diagnosed — not built |

**Related:** G06-1 (action-bar timing) · G06.5 follow-up (weak feedback + slow RP reload) · known "placed blocks changing texture" bug
**Touches:** `DeleterItem.act` · `OmniToolItem` + `OmniToolState.Mode` · `SlotManager.delete` / `nextFreeSlotIndex` / `RetiredSlots` · resource-pack sync path
**Reference:** old mod `CustomBlocks/…/GuiManager.executeDeleterDelete` (the 3-step delete to copy)

### ✅ Decisions (locked via UI 2026-06-26) — "improved Option 2", shared `removed` block

> ⛔ **SUPERSEDED 2026-06-27 → see [G06-14](#g06-14--unified-recycle-bin-deletion-system-replaces-the-removed-system).**
> The whole `(Removed)` mechanism below (shared `RemovedBlock` swapped by a chunk-scanner, permanent
> `DeletedSlots` no-reuse, `RemovedPlacements` in-session undo journal) is being **ripped out and
> replaced** by the unified Recycle-Bin system. Reasons it's being scrapped: the delete paths drifted
> (bulk delete never swaps → leaves broken purple slot blocks); the chunk-scanner re-scans the whole
> world forever; deleted slot numbers leak permanently; markers carry no identity and undo only works
> in-session. Kept below **for history only** — do not build against it.

### ✅ Decisions (locked via UI 2026-06-26) — "improved Option 2", shared `removed` block  *(historical)*

> ⚠️ **Supersedes** the older "Wanted" note above (*"keep the placement as a broken black/purple
> missing-texture block; do NOT turn it into another block"*) **and** the weaker reuse-only guard in
> §L. Owner re-chose in a fresh decision UI 2026-06-26: a leftover that stays a `slot_N` block (broken
> or guarded) can still bleed identity; a leftover tied to **no slot** never can. This covers
> **G06-2 + G06-3 + G05-2** with one mechanism.

On delete (red Deleter **and** `/cb delete` — both already route through `SlotManager.delete`):

1. **Swap every placed copy → one shared `RemovedBlock`** (`customblocks:removed`): neutral grey
   texture, name **"(Removed)"**, drops nothing, breaks instantly. It is **not** a `SlotBlock`, so it
   is tied to no slot index → it can never inherit an old *or* a future block's skin/name. This is the
   "turn into another block" the old note forbade — owner reversed that on purpose (cleaner root fix).
2. **Never reuse the freed slot index.** Replace the reuse-as-last-resort guard with a permanent
   retire: a deleted index goes into a new persisted **`DeletedSlots`** set; `nextFreeSlotIndex`
   **skips it forever** (migrate existing `FreedSlots` entries in on boot). Trade-off accepted by
   owner: 1028 slots, a few hundred used — ample headroom; a future "reclaim" cmd can free indices
   once a world scan proves no copies remain.
3. **Instant, no-rejoin refresh** (keeps §M's `HudSync.broadcast`): on delete, immediately sweep
   currently-loaded chunks and swap matching `slot_N` → `RemovedBlock`. **Far / unloaded** copies swap
   the moment their chunk next loads (owner accepted swap-on-load 2026-06-26).
4. **Undo of a delete un-retires the index** (drop it from `DeletedSlots`) so a restored block isn't
   eaten by the sweeper, **AND restores the greyed placements** (✅ confirmed in-game 2026-06-26).
   *Updated 2026-06-26:* the original "placed copies don't auto-return" limitation is **removed** —
   the sweeper now journals each swapped position (`RemovedPlacements`), and undo flips every still-
   `(Removed)` tracked position back to its `slot_N` block (inheriting the slot's glow). `/cb redo`
   re-retires + re-sweeps. In-memory journal → works within a session (undo history is itself per-
   session); a copy swapped on far-chunk-load AFTER the undo is the only one not caught (rare).

**New mechanism** — mirrors the proven Arabic cleanup, but swaps to `RemovedBlock` instead of air:

| New file | Role | Mirrors |
|---|---|---|
| `block/RemovedBlock.java` (+ static assets: blockstate / model / `removed.png` / lang `(Removed)`) | the shared leftover block | `arabic_letter` (shipped static block) |
| `core/DeletedSlots.java` (persisted int-set) | indices retired by delete; never reused; swept to `removed` | `RetiredSlots` |
| `block/DeletedPlacementSweeper.java` | chunk-load scan + tick-time `slot_N → removed` swap (budgeted) | `ArabicLetterRetirement` |
| `block/RemovedPlacements.java` (in-memory journal, added 2026-06-26) | remembers WHERE each copy was greyed so `/cb undo` can flip it back to `slot_N` | (none — closes the undo gap) |

**Wire-in** — `SlotManager.delete` adds the index to `DeletedSlots` (was `FreedSlots`) + kicks the
loaded-chunk sweep; `nextFreeSlotIndex` skips `DeletedSlots`; `DeletedPlacementSweeper.init()` in
`CustomBlocksMod.onInitialize`; undo path drops the index from `DeletedSlots`.

**Still open after this build (not in scope here):** Omni-Tool **Delete mode** (G06-2 fix #4) ·
recolor **pack-thrash debounce** (G06-3 fix #4, ties G06-1). Tracked, not built this pass.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | G06-2 + G06-3 + G05-2 core (shared `removed` block + no-reuse) | ✅ §O #1 (instant grey) + #8 (undo-restore) + #10 (restart) confirmed in-game 2026-06-26; #2/#3/#4/#5/#6/#7/#9 left to test |

---

## G06-3 · Delete-then-create scrambles placed blocks (worst with a color Square)

> 🛠️ building 2026-06-26 — same root as G06-2; covered by the shared-`removed`-block + no-reuse
> decision under **G06-2 → ✅ Decisions (locked 2026-06-26)**. Fixes #1–#3 below land via that one
> mechanism; fix #4 (pack-thrash debounce) stays open.

> *"when deleting a block then creating a new one after, so many conflicts happen … blocks flicker
> between newly created and already created, and much much worse stuff happen that ruined my video
> recordings today.. im sad"*

**Symptom** — Delete a block, then create a new one right after, and placed blocks start **flashing
back and forth** between the old block and the just-created block (oscillating, won't settle). The
developer reports it's worst — maybe only — **when touching the block with a color Square**; a block
left alone (never recolored) tends to stay put. Plus "much worse stuff merged together" (developer
overwhelmed, will itemize the rest later). Ruined a recording session. Seen on the **dedicated
server**; treat as **Both** — the cause is server-side logic, not environment-specific (developer
asked us not to assume SP is safe).

**This is the same root as G06-2** (slot recycling), seen from the player's side as world corruption.

**Why** — Placed blocks in the world, and the color Square's swap targets, are keyed by **slot
*index* (`slot_N`)**, not by the block's name. So:

| Step | What happens to the slot index |
|---|---|
| Delete block A (say `slot_5`) | `delete()` frees `slot_5` but **doesn't guard it** (`RetiredSlots` is Arabic-only). |
| Create block B | `nextFreeSlotIndex()` hands back the lowest free index = **`slot_5` again**; B's texture bakes onto `slot_5.png`. |
| Result | Block A (still placed as `slot_5`) and block B now **share one texture** → A wears B's skin. |

**Why the color Square makes it flash** (confirmed in code):

- A **Square** → `ColorVariantService.swapPlaced` sets the placed block to the **variant's slot index**. After a reuse, those variant slots are scrambled, so the swap lands on the wrong / colliding texture.
- A **Triangle / variant-create** → `createVariant` also calls `SlotManager.create` (line 132), so it **pulls more reused indices**, each firing its own `ResourcePackServer.updatePack()` → repeated pack reloads across the whole world = the visible **back-and-forth flashing**.

**Why an untouched block "stays put"** — with no Square swap and no new create reusing its index, a
placed block keeps its own slot and renders fine. The corruption needs a **reuse** to happen.

**Confirmed: creating *without* deleting first does NOT collide** — `nextFreeSlotIndex` returns a
fresh, never-used index when nothing was freed. So the conflict genuinely requires a **delete (or
Arabic retire) first** (answers the developer's "not sure if it happens normally" — it doesn't).

**Fix** — Shares G06-2's core; the extra pieces are about the Square/variant path and pack-thrash:

1. **Stop instant index reuse** (the core, shared with G06-2) — a **reuse-only guard**: `nextFreeSlotIndex` skips a just-freed index while fresh ones remain. **Not** `RetiredSlots` — that air-cleans placements (would make placed blocks vanish; see G06-2 fix #3).
2. **Refresh the deleted block** to the broken black/purple look (G06-2 fix #1) so a freed slot reads as "empty," never silently inherited by the next create.
3. **Make the Square/variant path collision-safe** — once reuse is guarded, a freshly-created variant can't land on an index an existing placed block is using.
4. **Tame the recolor pack-thrash** (ties to G06-1) — batch/debounce the `updatePack()` storm from per-click variant creation so the world doesn't reload-flash on every color.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — dedicated confirmed; cause is environment-independent | 🔍 diagnosed — not built |

**Related:** **G06-2** (same slot-recycling root — fix them together) · **G06-1** (recolor pack-thrash / lag) · known "placed blocks changing texture" bug
**Touches:** `SlotManager.delete` / `nextFreeSlotIndex` / `create` (+ new reuse-only guard) · `RetiredSlots` (keep its air-clean for Arabic; new guard stays separate) · `ColorVariantService.createVariant` / `swapPlaced` · `DeleterItem.act` (placed-block refresh) · `ResourcePackServer.updatePack` (debounce)
**Note:** "much worse stuff" is partly un-itemized — developer will specify more after this pass; log new sub-symptoms here as they're named.

---

## G06-5 · Color-variant names compound wrong ("Block Yellow (Green)")

> 🔍 diagnosed — build with G06-4 (shared `nearestName` resolver + migration pass); one of the most "ready" items in C4

> *"when creating a green block from a yellow block it becomes 'block yellow (green)' wtf?"*

**Symptom** — Make a green variant from a block that's already a yellow one and the new block's name
becomes **"Block Yellow (Green)"** — the old color is left in, the new color tacked on, and the format
(parentheses) isn't what the developer wants.

**Why** — Variant creation builds the display name as **`src.displayName() + " (" + label + ")"`**
([ColorVariantService.java:132](CustomBlocks-B/src/main/java/com/customblocks/core/ColorVariantService.java#L132); same pattern in `ColorToolService.createVariant`). The **ID** is built with
`stripColourSuffix(sourceId)` so it drops the old color — but the **display name has no equivalent
strip**, so a yellow source's full name ("…Yellow") is carried straight into the green variant's name.
ID and name use different logic; the name is the buggy half.

### Developer's decision (UI 2026-06-23)

- **Format = "Mars Green"** — base name, space, color word. No parentheses. *(developer: take the old
  mod as inspiration, don't copy verbatim.)*
- **Auto-clean existing** wrongly-named blocks, not just new ones.

### Old-mod inspiration (reference only)

The old mod already solved this: **`ColorNames`** — 16 canonical color families + 40+ aliases +
`resolveFamily(word)` — used "for stripping existing color segments from block IDs **and display
names**." The fix mirrors that idea (clean rewrite, not a copy).

### Brainstorm (fix direction)

1. **Add a display-name color strip** (parallel to the ID's `stripColourSuffix`): a small color-word
   set (the 4 fixed + the `ColorLibrary` names/aliases) that removes a trailing/embedded color word
   from the base name. New name = **`cleanBase + " " + Capitalized(color)`** → "Mars Green".
2. **Apply in every variant path** — `ColorVariantService.createVariant` (fixed 4),
   `CustomColorToolItem` / hex variants (use the exact `ColorLibrary` name, else the **nearest** palette name
   via the new `nearestName` resolver — never the raw `#hex`), and leave the HSL/gradient labels ("Lighter",
   "Vivid", "Gradient 2/5") alone — those aren't colors, don't strip them.
3. **One-time migration** to re-derive existing variant names to the clean scheme — same mechanism as
   the Group 26 `migrateDisplayNames` pass (idempotent, only rewrites what changed).

### Hex-name fallback (locked via UI 2026-06-24)

- **Nearest named colour** — a nameless custom hex is named by its **closest** palette colour ("dark red",
  "maroon", etc.), NOT the raw `#hex`. Owner: *"if its dark red or maroon … name it the nearest name like that."*
- **Build a new nearest-colour resolver** — `ColorLibrary.nameForHex` is **exact-match only** today (returns
  null otherwise) and there is **no** distance resolver. Add `ColorLibrary.nearestName(hex)` = the palette
  entry with the smallest RGB distance. *(Checked the old mod for the referenced `ColorNames`/`resolveFamily`
  — **not present** under that name; nothing to recycle, so this is a clean small build.)*
- **Open:** palette coverage — the 29 `ColorLibrary` dyes may not include "maroon"/"dark red" exactly; either
  nearest-of-29 is good enough, or enrich the palette with a few dark/muted names. Confirm at build.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — server-side naming | 🔍 diagnosed — format + migration + **nearest-name fallback** all chosen; palette coverage open |

**Related:** **G06-4** (the colored-tool hex system) · **G10-1** (`ColorLibrary` names) · Group 26 `migrateDisplayNames` (the migration pattern)
**Touches:** new display-name color-strip helper · new `ColorLibrary.nearestName(hex)` resolver · `ColorVariantService.createVariant` / `ColorToolService.createVariant` (use it) · `CustomColorToolItem` naming · a `migrateVariantNames` one-time pass in `SlotManager`

---

## G06-4 · Hex-Change System Rework — Colour Tools + Variant Repaint

> 🔍 designed — full rework; build with G10-1 (shares re-bake-from-source engine); do G10-1 first
>
> **2026-07-10:** owner reconfirmed building this now, alongside G10-1 — independent of G07-2 (bulk
> recolor hub), which is parked. **Scope confirmed: core fix + BOTH "go beyond" extras** (tool
> rename/lore on NBT, and the gradient/tone-mix tool) — not deferring the extras to a later pass.

> *"current hex changing system of colored 'squares and triangles' is horrible: when changing hex of a set of colored tools, their item colors don't change to the hex, and when choosing to repaint existing blocks to the new hex it doesn't do anything, and the lore hex stays the same"*

### Symptom

`/cb config hex <colour> <#RRGGBB>` changes one of the four fixed tool colors (red/yellow/green/black), but: (3a) the **tool's item colour doesn't update**, (3b) **repainting existing blocks does nothing**, (3c) the **name/lore hex stays the old value**. Developer: "horrible", wants a **full rework and upgrade**. Seen on the dedicated server; repaint tried on the 4 fixed-color variants.

### Two distinct roots

**Root A — hex never reaches the client (3a + 3c).** The four hexes live ONLY in server-side `CustomBlocksConfig` (`triangleRedHex`/Yellow/Green/Black). Nothing syncs them to clients — no payload carries them (verified 2026-06-24: no `ConfigSyncPayload` class; real S2C payloads are `HudSyncPayload` / `TransparentBgPayload` / etc., none carrying hexes). `ShapeToolItem.getName` runs client-side and reads the client's own local config → shows the old hex forever on dedicated. (Singleplayer: shared JVM, name would update — matches "seen on dedicated.")

**Root B — repaint is a brittle pixel match (3b).** `/cb recolorvariants` → `ColorVariantService.recolorVariants` → `ColorReplacer.swapColor(png, oldRgb, newRgb, 30)` only repaints pixels within 30/channel of the exact old hex. Silently does nothing when baked background is not a flat block of that exact colour, or when variant was baked at a different past hex.

### ✅ All decisions (locked 2026-06-23, 2026-06-24, 2026-06-25)

- **Full rework** — not a patch.
- **Live-tint the fixed-4 icons:** red/yellow/green/black Square+Triangle work like `CustomColorToolItem` — white icon tinted client-side from the synced hex, so item colour AND name update instantly when hex changes. No pack reload, dedicated-safe. Unifies all colour tools on one pattern.
- **Sync hex to clients:** new small S2C payload (mirror `TransparentBgPayload` / `HudSyncPayload` pattern), pushed on join + on every `/cb config hex`. Fixes the name/lore-stale issue at the root.
- **Robust re-bake repaint:** repaint each `_<colour>` variant by re-rendering from the stored source with the new hex (`createVariant` / `applyBgRemoval` path), falling back to `swapColor` only when no source exists. Catches every variant regardless of what hex it was baked at, and handles non-flat backgrounds.
- **Custom-hex variants included (2026-06-25):** `*_hex_rrggbb` variants (from `/cb customcolor`) fold into the rework. Auto-repaint on hex change covers ALL variant types, not just the 4 fixed suffixes. Nothing goes stale.
- **Auto-repaint on hex change:** `HEX_RECOLOR_CONFIRM` GUI's Yes runs the robust re-bake, so changing a colour offers one-click clean update of all variants. Retire (or repoint) the brittle standalone `/cb recolorvariants`.
- **"Go beyond" extras (2026-06-25 scope lock):**
  - **(1) Name + describe each colour tool** — rename + custom lore per tool, self-documenting hotbar. Stored on tool NBT (same place as `cb_rgb`).
  - **(2) Gradient / tone-mix tool** — a tool that blends two colours or applies a gradient (extends `ColorReplacer.tint` / `RecolorToneTools` to a 2-stop blend).
- **Candidate list (not built this round):** free-hex on the fixed-4 tools, saved favourite colour sets.

### Rework direction

1. New S2C hex-sync payload — mirror `TransparentBgPayload`; push on join + `/cb config hex`.
2. `ShapeToolItem` → white art, live client-tint from synced hex (mirror `CustomColorToolItem`).
3. `ColorVariantService.recolorVariants` → re-bake-from-source for ALL variant types (fixed-4 + custom-hex).
4. `HexCommands.setHex` / `HEX_RECOLOR_CONFIRM` → auto robust re-bake.
5. Retire `ServerPackGenerator.addTintedShapeItems` (icons go live-tinted; no longer pack-baked).
6. Tool NBT: rename + lore storage per tool.
7. Gradient/tone-mix tool: 2-stop blend in `ColorReplacer` engine.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — repaint hits both; name/lore/icon bites on dedicated | 🔍 designed — all decisions locked 2026-06-25; ready to build after G10-1 |

**Related:** G10-1 (same re-bake-from-source engine — build together) · G06-5 (nearestName resolver — apply in variant paths) · G07-2 (bulk recolor hub consumes this engine) · `CustomColorToolItem` (live client-tint pattern to copy) · `ColorVariantService` (4-colour variant flow)
**Touches:** new hex-sync S2C payload · `ShapeToolItem` (client-tinted icon + synced name) · client item-tint renderer (mirror `CustomColorToolItem`) · `ColorVariantService.recolorVariants` (re-bake from source, all variant types) · `HexCommands.setHex` / `HEX_RECOLOR_CONFIRM` (auto repaint) · `ServerPackGenerator.addTintedShapeItems` (retire) · tool NBT (name/lore) · gradient/tone-mix tool (new)
**Verify in-game:** tool icon colour + lore hex update instantly on `/cb config hex` (dedicated server); repaint catches all variant types (fixed-4 + custom-hex); no more stale `_hex_rrggbb` variants; gradient tool applies 2-colour blend correctly.

---

## G06-7 · Block Drops Customization — Per-Block Drop Control

> 🟡 **SELF-default baseline BUILT + confirmed in-game 2026-07-09** (via Group 30 §R3, Guess Mode testing —
> `getDroppedStacks` added to `SlotBlock`, every custom block now drops its own item on a survival break).
> The rest of this spec (NONE mode, per-block config, Drops screen tab, XP, tool-tier gating) is still
> 🔍 designed, not built — Drops screen is a Studio Section tab alongside Manage (G27-2); build after
> G27-2 sets the tab pattern; do NOT merge with G27-2's tab.

> *"add a way to customize what custom blocks drop when broken in minecraft"*

### Current state (superseded 2026-07-09 — see baseline note above)

As of 2026-06-25: custom blocks dropped **nothing** when broken in survival mode, no `getDroppedStacks`
override existed on `SlotBlock`. That gap is now closed (SELF-default, see above); everything below this
line is still the **target end-state** for the full configurable system (NONE mode, per-block picker, XP,
tool-tier) — not yet built.

### ✅ All decisions (locked 2026-06-24, 2026-06-25)

**Drop modes per block:**
- **SELF** (default) — drops the block's own `SlotItem`. Migration: all existing blocks default to SELF on first load (idempotent).
- **NONE** — drops nothing.
- **CUSTOM** — roll the configured loot entries.

**Loot entry system (CUSTOM mode) — full depth v1:**
- Each entry: item + count range + chance (0–100%).
- Silk-touch → drop SELF override.
- Fortune → multiply counts (configurable per block: uniform-bonus or ore-formula).
- Weighted loot pools (pick-N-weighted), multiple pools (one guaranteed + one bonus), per-pool roll count.
- XP on break (override `getExperienceDrops`, emit XP-orb range).
- Tool-tier gating — require minimum tier (wood→netherite) to drop anything.
- Survival rule: "always drop" (regardless of tool/gamemode) or "respect tool" (silk-touch + fortune + tool-tier). Full set in v1.

**What triggers drops:**
- Survival break → drops ✓
- Creative instant-break → drops ✓
- `/cb delete` = admin (removes block definition entirely) → NO drops.

**Safety cap (smart, not arbitrary):**
- No hard cap unless the configuration would cause a server crash risk.
- The system calculates max possible items from the configured entries × max rolls × max counts.
- If the calculated max exceeds a safe threshold → show a warning in the Drops screen ("This config could drop ~N items — may cause lag. Reduce or accept the risk.").
- Developer can continue past the warning (their choice).
- No silent hard blocks.

**Item picker (in the Drops screen):**
- Search bar (live filter as you type) + category tabs (food / tools / blocks / etc.) — both active simultaneously, search overrides categories.
- Custom blocks included in the picker (your own /cb create blocks appear as selectable drop items).
- Advanced features: more cool filtering/sorting (to be designed during build session).

**Drops screen home:** a dedicated `Section.DROPS` tab in `/cb create` Studio (alongside Manage, Texture, etc.). Optional — default state shows the block's current drops (default = SELF). Synced via `SlotData.drops`. NOT merged with G27-2's Manage tab — own tab, own purpose.

**Command path:** `/cb drops <id> …` for terminal/command use (always exists alongside the GUI).

**Bulk op:** a bulk "set drops" op in the G07-2 framework (reuse, don't rebuild bulk). This block as template source.

### Open items (finalise during build)

- Exact safe-threshold calculation for the warning (needs testing with real server perf).
- Advanced item picker features (filter/sort options — design during build session).
- Tab name: "Drops" (provisional).

| Group | Scope | Status |
|---|---|---|
| G06 — Tools (block behavior) | Both — server-side drop logic, identical SP & MP; screen is a G27 Studio surface | 🔍 designed — all decisions locked 2026-06-25; ready to build after G27-2 |

**Related:** G27-2 (Studio tab pattern — Drops tab uses same `Section` infra; build Manage first) · G07-2 (bulk framework — bulk-drops op reuses it) · `SlotData` attribute pattern (glow/hardness — mirror for `drops`) · `SlotBlock.getDroppedStacks` (override site) · `ColorLibrary`/registry search (item picker pattern)
**Touches:** `SlotData` (+`drops` / `DropData` / `DropEntry`) · `SlotManager` (setter + default migration to SELF) · `SlotBlock.getDroppedStacks` + `getExperienceDrops` (emit configured drops, honor tool/fortune/silk) · new `Section.DROPS` tab + `StudioDropsPanel` · new `/cb drops` command · optional bulk-drops op (G07-2) · `SlotDataStore` (persist drops)
**Verify in-game:** break block in survival → drops configured item(s); creative break → also drops; /cb delete → no drops; SELF default means block item drops on fresh blocks; fortune/silk-touch respected in "respect tool" mode; Drops screen and /cb drops command show the same value (synced); smart cap warns on dangerous configs.


---

## G06-6 · Per-face actions — Omni-Tool "Face" mode (rotate / mirror / copy / glow)

> 🔍 designed — all decisions locked 2026-06-25; folded from ISSUES.md

### Decisions (locked 2026-06-25)

- **Scope: per-FACE texture only.** No whole-block rotation (furnace-style FACING block-state on 1028+ blocks) — per-face is what was wanted. Whole-block rotation is explicitly OUT of this FX; flag as a separate future slice if needed.
- **Every custom block automatic** — all SlotBlocks' faces are actionable, no opt-in flag.
- **New "Face" mode on the Omni-Tool** (sneak+right-click to cycle modes, reusing the proven mode-switch infra from G06-2 Delete mode).
- **Input scheme — scroll-wheel (recommended; open for final tuning):** scroll up/down cycles the active action (Rotate → Mirror → Copy → UV Zoom → …); action bar shows current action; right-click applies to the hit face; sneak+right-click opens a small per-face sub-menu for heavier picks (copy-to-one vs copy-to-all, etc.). Owner was unsure — this is the recommended scheme, confirm during build.
- **Face actions (locked):**
  - **Rotate 90°** — each right-click increments the face's `rotation` field (0/90/180/270). Atlas-native, no image rewrite, cheapest action.
  - **Mirror / flip** — flip the face texture image at bake (no native UV-mirror field in vanilla models; bake-time PNG flip per face).
  - **Copy face → one chosen face OR all 6 faces** — BOTH modes available: right-click target = paste to one face; a separate button/action = paste to all 6. Uses existing `TextureStore.saveFace`.
  - **Per-face glow / see-through** — flagged as a **later, separate slice** (whole-block luminance / cutout today; true per-face emissive/cutout is a heavier render feature). NOT in v1.
  - **"+ more cool stuff" open bucket** (see brainstorm below).
- **After-edit resync:** face change = pack rebuild; uses the shared G06-1/G05-1 resync path so it lands live without rejoin.
- **Undo:** every face transform registers an undo step (G28-1 shared helper — nothing ships un-undoable).

### Brainstorm — locked v1 + future bucket

**Cheap (native model fields, atlas-safe — candidates for v1 or v1.x):**
- Per-face zoom / pan / crop — set the face's `uv` array to show a region of the image (zoom, pan, tile). Native, no image rewrite.
- Rotate-all / mirror-all — apply transform to all 6 faces at once.
- Swap two faces — exchange textures (+ transforms) of face A and face B.
- uvlock toggle — rotate with/without `uvlock` (texture spins vs stays fixed).
- Random shuffle — randomise all 6 faces' rotations for an organic non-repeating look.

**Medium (reuse other engines on one face):**
- Per-face tint / recolor — run the G10-1/G06-4 recolor engine on a single face PNG.
- Per-face image from URL — paint a different source image onto one face (extends `paintface`, driven by crosshair hit side).
- Copy a face from ANOTHER block — eyedropper: sample block X's face, stamp onto block Y's face.
- Per-face border / frame overlay — composite a frame onto a face at bake.
- Face presets — save a full 6-face config as a template, apply to other blocks.

**Heavy (own later slices):**
- Per-face glow (emissive) — needs emissive texture layer.
- Per-face see-through — needs per-face cutout; ties to G10-1 render-layer work.
- Per-face animation — GIF on one face only; needs per-face frame path (far future).

### Architecture

1. Wire `BlockHitResult.getSide()` into the Omni-Tool's action so Face mode knows which of the 6 faces was clicked.
2. Extend per-face data with a small transform record per face: `rotation` (0/90/180/270) + `mirrorH`/`mirrorV`. Persist in `SlotData` / face store; feed into `cubeFacesJson`.
   - Rotate → set model face `rotation` field (no image change).
   - Mirror → bake-time flip of the face PNG.
   - Copy → `TextureStore.saveFace` to target face(s).
3. New `OmniToolState.Mode.FACE` + scroll-wheel action cycling + action-bar readout.
4. Pack-rebuild on change → shared resync path.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools (Omni-Tool + face system) | Both — server-side face data + pack; rotate is atlas-native | 🔍 designed — all decisions locked 2026-06-25; input scheme pending final confirm during build |

**Related:** existing per-face system (`FaceCommands` / `FaceEditorMenu` / `TextureStore` faces / `cubeFacesJson`) · `OmniToolItem` + `OmniToolState.Mode` · G06-2 (Delete mode — same mode-extension pattern) · G10-1 (per-face see-through = later slice) · G06-1/G05-1 (after-edit resync) · G28-1 (undo step)
**Touches:** `OmniToolItem` + `OmniToolState.Mode` (new Face mode) · `BlockHitResult.getSide()` wiring · `SlotData` / face store (+ transform record: rotation + mirror) · `ServerPackGenerator.cubeFacesJson` (emit per-face `rotation`; bake mirror) · `TextureStore` (face copy to one or all) · after-edit resync path
**Verify in-game:** right-click a face → texture rotates 90° each click; persists across rejoin; all players see it after live resync (no rejoin needed); mirror/copy work per face and copy-to-all works; old configs unaffected.

---

## G06-13 · Background-removal clean-up — rim + corners + keep the shadow

> 🔍 reported 2026-06-26 (owner, TEST-button image) — **not built**; investigate before coding (don't guess)

> *"i set it to bgremove, the insides are better but the white background didn't fully turn into black
> perfectly, and shadow should still be cool and visible"*

**Context** — A red **button-on-white** image (with a soft drop-shadow). Switching from closed/smart to
**`BgRemove` (edges)** correctly **stopped the white letters being eaten ✅** (closed/smart matched the
white letters to the white background; edges only removes the *connected outer* background, so the letters,
walled in by the red ring, survive).

**Two issues left** (`BackgroundRemover.process`, EDGES path):
1. **White doesn't fully go black.** A pale rim survives *(verified by owner)*. Likely cause *(to confirm on
   the real image)*: the soft **drop-shadow** ring is grey = outside the tight white ΔE tolerance, so the edge
   flood **stops at the shadow**, leaving a thin near-white band trapped between shadow and button unremoved.
2. **Corners show through to the floor** *(verified by owner)*. Likely cause: non-square source →
   `ImageProcessor` pads with transparency; the padding renders see-through, not black.

**Constraint** — owner wants the **drop-shadow kept visible** (the 3-D look). So we must clean the
rim/corners to black **without** eating the mid-grey shadow.

**Fix direction (confirm during build):**
- After the edge flood, **snap leftover near-WHITE → black** (mirror of the existing near-black
  `snapBackgroundColor` / `SNAP_MAX`, but toward white). Near-white only, so the mid-grey shadow is untouched.
- **Flatten transparent padding to the fill** so corners aren't see-through.
- Re-check the shadow survives; tune the near-white threshold so it cleans the rim but not the shadow's lightest edge.

**Owner-provided (06-26, verified):** `/cb tolerance` = **20 / 100** · source = iStock "test" button
(`gm996396804`, button-on-white). At strength 20 the ΔE cap is only ~4.4 of 22 (code: `MAX_DELTA_E`), i.e.
**very tight** — consistent with the pale rim (only near-exact-white gets blackened, the shadow's light
falloff doesn't).
**Still needed before code:** the **actual PNG file** (the link above is the watermarked stock *page*, not the
asset) so the fix is tested on real pixels, not guessed.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools / bg-removal | image pipeline (server-side bake) | 🔍 reported — tolerance 20 + URL in hand; **need the PNG file**, then investigate (not built) |

**Related:** §H "low-contrast eat" (parked) · §I eyedropper/despeckle (the broader fix) · `BlockToleranceStore` (per-block strength)
**Touches:** `image/BackgroundRemover` (near-white snap + padding flatten) · `image/ImageProcessor` (padding fill)
**Verify in-game:** red-button-on-white → background fully black, corners not see-through, drop-shadow still visible; white letters stay intact.

---

## G06-14 · Unified Recycle-Bin deletion system (replaces the `(Removed)` system)

> 💬 designed 2026-06-27 (owner interview) — **NOT built.** One clean, correct deletion system for
> **every** delete path. Supersedes the `(Removed)` mechanism under [G06-2](#g06-2--deleter-doesnt-delete-properly--buggy-mp) and
> resolves **G06-2 + G06-3 + G05-2**; the trash side is **[G09-2](GROUP_09_BACKUP_SAFETY.md)**.

> *"i want a new and safe and correct system FOR ALL rather than the current, i just dont like it and
> its kinda bad … leave a clear mark and dont use slotblocks and use a new system for /cb trash."*

### Why the current system is being scrapped (not patched)

Investigated 2026-06-27. The `(Removed)` system is bad in four concrete ways:

1. **The delete paths drifted apart.** Single `/cb delete` and the red Deleter call the placement
   swap; **bulk delete does not** ([BulkCommands.applyDelete](../../src/main/java/com/customblocks/command/handlers/BulkCommands.java) never calls
   `DeletedPlacementSweeper.onDeleted` or `HudSync.broadcast`). So bulk-deleted copies lose their
   texture but stay `slot_N` → they show as the **broken purple/placeholder block**, never the marker.
   *(This is the bug the owner hit.)*
2. **The chunk-scanner never stops.** As long as one block was ever deleted, `onChunkLoad` runs a
   full block-by-block scan of **every chunk that loads, forever** — a permanent tax that gets worse
   with each delete, and currently scans for out-of-range ghost indices that can never match.
3. **Slot numbers leak permanently.** `DeletedSlots` retires a deleted index forever; the fixed slot
   pool only shrinks.
4. **Markers carry no identity + undo is fragile.** The grey `(Removed)` block is tied to no slot, so
   undo needs an in-memory `RemovedPlacements` journal that dies on restart, and you can't tell which
   block a marker used to be.

**Removed when G06-14 ships:** `RemovedBlock`, `RemovedPlacements`, `DeletedPlacementSweeper` (replaced
by a name-keyed marker resolver), the permanent-retire behaviour of `DeletedSlots`, `FreedSlots`, and
the per-path delete logic duplicated across `DeleteCommands` / `SlotBlock.cbDelete` / `BulkCommands`.

### The new system — "Recycle Bin" (locked 2026-06-27)

Owner-chosen behaviour. Mental model = the desktop Recycle Bin.

| # | Decision | Owner answer |
|---|---|---|
| 1 | Placed copies on delete | **Leave a clear marker** — never a broken slot block |
| 2 | Marker label | Reads **`Deleted: <blockname>`** |
| 3 | Marker is breakable | **Yes** — instant, drops nothing |
| 4 | Restore behaviour | Re-creates the block **and** flips every marker back into it — **even after a restart** |
| 5 | Restore slot number | Takes **any free slot number** as its new one; synced + recognised everywhere |
| 6 | When a slot number frees | **Reserved while in Trash; freed only on Empty** *(assistant-decided safe default — owner was unsure; recycle-bin model avoids the far-chunk corruption)* |

**Flow:**

- **Delete** (any method) → the block's definition + texture + attributes move to **Trash**
  (recoverable). Every placed copy becomes a **`Deleted: <name>` marker** — instantly in loaded
  chunks, on chunk-load for far ones. The block's slot number is **reserved** (kept out of reuse)
  while it sits in Trash, so far copies stay safe and restorable.
- **Restore** (from `/cb trash`) → the block is re-created on a **fresh free slot number**, textures +
  attributes re-applied, and every `Deleted: <name>` marker resolves **by name** back into the real
  block (works after restart because Trash is on disk — no in-memory journal needed).
- **Empty** (permanent) → the block is gone for good; its reserved slot number is **freed for reuse**;
  that block's remaining markers convert to a generic, un-healable `(Deleted)` marker (so a later
  same-name create doesn't accidentally revive them). This is the only point a number is freed → no
  silent leak, no surprise corruption.

### The marker block (identity-carrying, not a slot block)

- One dedicated block `customblocks:deleted_marker` with a tiny **BlockEntity** holding the deleted
  block's `customId` + `displayName` (so the nameplate reads `Deleted: <name>`). **Not** a `SlotBlock`
  → tied to no slot index, can never inherit another block's skin.
- Grey neutral texture (shipped static asset, never the generated pack). Instant break, drops nothing.
- **Auto-heal rule:** when a marker's chunk loads, if a **live block** with the same `customId` exists,
  the marker converts to it (this is what makes Restore land everywhere, even after restart). A generic
  (emptied) marker has no `customId` → never heals.

### Locked visuals & labels (owner interview 2026-06-27)

| Aspect | Decision |
|---|---|
| Default texture | **Grey block + faint dark ✖** on each face — shipped static PNG (generated, not the live pack). |
| Placed marker label | Shows **both** an always-visible floating tag **and** the look-HUD, text `Deleted: <name>` in **red**. |
| Held item (creative give only) | Generic name **"Deleted Marker"** in red — markers drop nothing, so this is only reachable via give/creative. |
| Break | Instant, drops nothing (already locked). |

> 🧰 **Deferred QoL → [G06-15] in-game marker customization screen.** Owner wants to restyle the
> marker however they like from an in-game GUI. **Out of scope for the G06-14 build** — ship the
> grey-✖ default now, build the screen later. **Do not forget.**

### One shared delete rail (all paths funnel through it)

A single `DeletionService` method does every per-block step so the paths can't drift again:
snapshot → move definition to Trash → convert placed copies to markers (loaded now + far on load) →
reserve the slot. Callers add the batched finish (one pack rebuild, one `HudSync.broadcast`, one undo
entry). Routed paths: `/cb delete <id>` / `#`, the red **Deleter** tool (`SlotBlock.cbDelete`),
**bulk delete** (`BulkCommands`), the GUI delete (`BulkConfirmMenu`), and the broken-block cleanup
(`BrokenConfirmMenu`).

### New / changed / removed

| Action | File | Role |
|---|---|---|
| **New** | `block/DeletedMarkerBlock.java` + `DeletedMarkerBlockEntity.java` (+ static assets, lang `Deleted: %s`) | identity-carrying marker |
| **New** | `core/DeletionService.java` | the one shared delete rail |
| **New** | `block/MarkerResolver.java` | name-keyed convert (delete→marker, restore→block) on tick + chunk-load, budgeted |
| **New** | `core/TrashSlots.java` (persisted) | slot numbers reserved while their block is in Trash; freed on Empty |
| **Change** | `command/handlers/TrashCommands.java` + trash GUI | Restore = fresh slot + heal markers by name; Empty = free slot + tombstone markers |
| **Change** | `DeleteCommands` / `SlotBlock.cbDelete` / `BulkCommands` / `BulkConfirmMenu` / `BrokenConfirmMenu` | route through `DeletionService` |
| **Remove** | `RemovedBlock`, `RemovedPlacements`, `DeletedPlacementSweeper`, `FreedSlots`, `DeletedSlots` no-reuse | the old `(Removed)` mechanism |

### Build slices (small, testable — owner tests each before the next)

1. **Marker block + BE** — register `deleted_marker`, nameplate `Deleted: <name>`, instant break, no drop. *(Test: place one via a temp command, confirm name + break.)* — ✅ **CONFIRMED in-game 2026-06-27** (`/cb spawnmarker`; grey + red ✖; tag + look-HUD; no drop; held name "Deleted Marker"). ⚠️ **Pending tweak for slice 2's jar:** held item name is red, look-HUD is green — owner wants the **item name green to match the HUD**; recolor `DeletedMarkerItem` red→green in the next build (don't rebuild solely for this).
2. **Shared `DeletionService` + route `/cb delete` and the Deleter** through it → copies become markers; old `(Removed)` swap retired. *(Test: delete a placed block → markers appear.)* — ✅ **CONFIRMED in-game 2026-06-27** (D2.1–D2.7). Item name red→green folded in. One bug surfaced — `/cb undo` of a delete needed a rejoin to refresh the name/HUD → **fixed same day, see 2a below.**
   - **2a. NO-REJOIN sweep** *(the undo-rejoin fix + the same lag everywhere)* — 🟢 **BUILT 2026-06-27, awaiting in-game.** `/cb undo`+`/cb redo` now `HudSync.broadcast` after their steps; the same live-refresh added to all attribute setters (glow/hardness/sound/collision/category/shape) across **commands, tools (Lumina/Chisel/Omni), menus, and bulk ops**; and the older actor-only paths (reid/anim/notes/category-admin/studio/vault) upgraded to refresh **all** players (owner's "everyone online" choice). NO-REJOIN principle banner written into `HudSync` (grep tag `NO-REJOIN`). TG **§D-fix sweep** (D2.5 + DF1–DF9).
3. **Route bulk delete + GUI delete + broken-cleanup** through the same rail. *(Test: bulk delete → markers everywhere, no purple blocks, other players update without rejoin.)* — bulk **delete** is the only delete path still on the old route; this slice moves it to `DeletionService` (+ folds in any remaining no-rejoin gap for deletes).
4. **Trash reserve/restore** — reserve slot while in Trash; Restore re-creates on a fresh slot + heals markers by name (loaded + on-load). *(Test: delete → restore → markers turn back; restart → still restores.)*
5. **Empty frees the slot + tombstones markers**; remove the forever chunk-scanner and the permanent no-reuse leak. *(Test: empty → number reusable; emptied markers don't revive on same-name create.)*

| Group | Scope | Status |
|---|---|---|
| G06 — Tools (deletion) + G09-2 (trash) | Both SP & MP — server-authoritative; replaces G06-2/G06-3/G05-2 `(Removed)` mechanism | 🟡 **slices 1+2/5 ✅ in-game**; **slice 2a NO-REJOIN sweep 🟢 build-green 2026-06-27** (awaiting in-game); 3 slices left |

**Related:** [G06-2](#g06-2--deleter-doesnt-delete-properly--buggy-mp) / [G06-3](#g06-3--delete-then-create-scrambles-placed-blocks-worst-with-a-color-square) / G05-2 (the bugs this resolves) · [G09-2 trash](GROUP_09_BACKUP_SAFETY.md) · G07-3 bulk delete (now shares the rail)
**Touches:** new `DeletionService` · new `DeletedMarkerBlock` (+BE) · new `MarkerResolver` · new `TrashSlots` · `TrashCommands` + trash GUI · `DeleteCommands` / `SlotBlock.cbDelete` / `BulkCommands` / `BulkConfirmMenu` / `BrokenConfirmMenu` (route through rail) · `SlotManager.delete` / `nextFreeSlotIndex` (reserve not retire) · **removes** `RemovedBlock` / `RemovedPlacements` / `DeletedPlacementSweeper` / `FreedSlots`
**Verify in-game:** every delete method leaves `Deleted: <name>` markers (no purple blocks); markers break instantly; Restore brings the block back on a new number + flips markers everywhere (incl. after restart); Empty frees the number and emptied markers don't revive; no per-chunk lag while exploring after deletes.
