# Group 06 — Tools, Dynamic Glow & Creative Tab

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 7 (Tools) build-verified.
>
> **Objective:** Merge the Lumina Brush and Chisel into a single configurable "Omni-Tool". Add held-block dynamic glow emission. Audit and clean up the dedicated tools creative tab. Restore tool-give shortcuts. Finalize tool consolidation per locked decisions.
>
> **Source issues:** 17.8 (held block dynamic glow), 17.9 (dedicated creative tools tab), 17.10 (Chisel + Lumina unification), Group F (tool-give shortcuts), Q8 (tool consolidation), Decision §7, §8, §9, §C
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## 🧭 Build Ledger — what's left in Group 06

> ⚠️ **2026-07-16: audited against the code first — this ledger was stale.** Biggest correction:
> **G06-4 and G06-5 are already built** (hex sync, robust re-bake, "Mars Green" names, nearest-of-29).
> The "no payload syncs hexes (verified 2026-06-24)" note under Root A below is wrong — don't rebuild it.
> Live test status is in the testing guide.

| TG § | Spec ID | What | Status (2026-07-16) | Blocked by |
|---|---|---|---|---|
| H | G06-14 | Recycle-Bin delete | 🟢 slices 3-5 BUILT (slice 3 was already coded; built 4-5). Restore reuses ORIGINAL slot # (deviates from decision #5 — see TG §H). Pending in-game | — |
| G | G06-4 | Hex system rework | ✅ **CONFIRMED in-game 2026-07-16** (TG6 §G core rows) — hex-sync + robust re-bake. Extras NOT built: fixed-4 live-tint (needs white art+model; pack-baked path works), free-text anvil rename/lore (→ moved to G27 §G27.20) | — |
| G | G06-5 | Variant names ("Mars Green") | ✅ **CONFIRMED in-game 2026-07-16** (TG6 §G core rows) — `stripTrailingColorName` + `nearestName` of 29 | — |
| B | G06-1 | Instant Square swap feedback | ✅ **CONFIRMED in-game 2026-07-16** (TG6 §B, B1-B5/B7) — client overlay in `ClientSwapPredictor` (action-bar overwrite = dedupe) | — |
| A | G06-6 | Per-face "Face" mode (Omni-Tool) | ⏳ not built — large (own session) | — |
| A | G06-18 | Rainbow Rectangle — wrong job built | ✅ **CONFIRMED in-game 2026-07-16** (TG6 §A, A1-A7) — area/corner removed; per-face paint only (Area also dropped from Omni). Undo/redo gap found this pass → tracked as G28-1, not this spec | — |
| D | G06-17 | Glow — hand-glow unbuilt + placement lag | ✅ Sym2 (placement lag) **CONFIRMED in-game 2026-07-16** (TG6 §F1, `CLIENT_GLOW_RESOLVER`). ⏳ Sym1 (hand/dropped light) not built — own session | — |
| E | G06-16 | Omni-Tool & tools rework | 🟡 Area drop BUILT (G06-18). ⏳ modes (Delete/Paint/Eyedrop/Admin) + config Screen not built. **Unblocked** — G27 Screen framework now exists | — |
| F | G06-13 | Bg-removal rim/corners/shadow | 🔍 reported | needs a fresh repro (old asset stale 2+ weeks) |
| G | G06-19 | Hex-change full pack-rebuild kicks players (TG6 G9) | 🔍 diagnosed 2026-07-16 — design locked (targeted rebuild + cooldown + client patience) | needs discuss-before-build pass, then code |
| ~~J~~ | ~~G06-7~~ | ~~Block drops~~ | **MOVED to Group 27 / TG27** 2026-07-16 | owned by G27 now, not G06 |
| — | G06-2 | Deleter diagnosis | 🔍 diagnosed | fix ships via G06-14 · **archive this section once G06-14 slices 3-5 are fully in-game confirmed** (locked 2026-07-16) |
| — | G06-3 | Delete-then-create scramble | 🔍 diagnosed | fix ships via G06-14 · **archive alongside G06-2** once G06-14 is fully confirmed |

🔗 Historical / superseded content (old numbered tests, the pre-G06-14 `(Removed)` mechanism, old follow-ups)
lives in the collapsed **Archive (historical)** section at the bottom of this doc — not deleted, just out of the way.

---

## UI medium audit (2026-07-09)

RecolorConfirmMenu, HexRecolorConfirmMenu, OmniMenu → **keep chest GUI**. Owner: design is poor, flag for
a `GUI_DESIGN_GUIDE.md` polish pass — applies to every chest GUI kept across this whole audit, not just
these.

HexColorsMenu ("Variant Colours" hex editor), CustomColorMenu ("Color Studio") → **Screen — not built.**
Both currently fake real text-entry with an anvil-rename prompt; owner wants a proper Screen text
field(+ live colour preview) instead. **Migrated to Group 27 2026-07-16** — see
`GROUP_27_SCREENS.md` §G27.20 (scope locked, full design still open) and §G27.21 (RP-reload-waits-for-
open-screen spinoff from TG6 §G14). TG6 §G11-13/§G16-17/§G22 no longer test these as G06 chest flows.

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Lumina Brush | Cycled glow 0→4→8→12→15 | Working physical item | Merged into Omni-Tool as "Glow Mode" |
| Amethyst Chisel | Cycled hardness presets | Working physical item | Merged into Omni-Tool as "Hardness Mode" |
| Omni-Tool | Existed as configurable multi-mode tool | Missing (two separate tools) | Rebuilt: one item, hot-swappable modes via Shift+RClick |
| Deleter | Right-click to remove block instantly | Working physical item | Kept as separate item (by decision) |
| Tool-give shortcuts | `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle` | Missing — had to use `/give @s customblocks:…` | Restored as `/cb brush` → gives Omni-Tool, etc. |
| Held block glow | Torch-like light emitted when holding glowing block | Missing | Networked client-side mixin planned, **never built — diagnosed 2026-07-16 as G06-17**, fix direction is now fully client-side (no packet needed), scope = held + dropped items |
| Creative tools tab | "CustomBlocks Tools" tab existed | Two tabs (Blocks + Tools) | Single "CustomBlocks Tools" tab: Omni-Tool, Deleter, Square, Triangle. NO blocks in tools tab. Sortable via config. |
| Golden Hexagon | Admin tool (physical item) | Framework only | Merged into Omni-Tool as "Admin Mode" — not a separate item |
| Rainbow Rectangle | Area tool (physical item) | Framework only | **Correction 2026-07-16 (G06-18): this "Area tool → Omni-Tool Area Mode" mapping was wrong.** Rainbow Rectangle's real job is per-face paint on one block (already the standalone tool, TG §A) — it was never meant to be an Area/corner-select tool. Area is **dropped entirely**, not merged into Omni-Tool as anything. |
| Diamond Triangle (Wand) | Eyedrop/wand tool | Framework only | Merged into Omni-Tool as "Eyedrop Mode" |
| Tab icon | `/cb settabicon <url>` | Missing | **Owned by G25** (decision C 2026-06-21) — not a G06 tool |
| Custom-hex tools | `/cb customtriangle <#RRGGBB>` — gave a Square + Triangle tinted to a custom hex instead of the fixed presets | Missing | **Moved here from G08 2026-07-11** (was misfiled as a "triangular block shape" — it's actually a recolor-tool variant, no block-shape feature exists in the legacy code) — not built |
| Triangle fill mode | `/cb trianglemode edge\|full` — edge-only vs whole-face paint fill for the Triangle recolor tool | Missing | **Moved here from G08 2026-07-11** (was misfiled as "triangle placement mode") — not built |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Omni-Tool | Physical item — Shift+RClick opens mode config chest GUI |
| Omni-Tool modes | ~~Glow, Hardness, Delete, Paint, Area, Eyedrop, Admin~~ → **locked 2026-07-16 (G06-16): `Glow, Hardness, Delete, Face, Paint, Eyedrop, Admin`** (Area dropped, Face added — see G06-16/G06-18) |
| Held block glow | Dynamic light emitted from hand when holding glowing custom block — **and dropped-on-ground items** (scope locked 2026-07-16, see G06-17) |
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
| ~~Area~~ | ~~Green~~ | ~~Mark area corners for bulk operations~~ — **dropped entirely, locked 2026-07-16 (G06-16/G06-18)**: this was a misunderstanding, no real job, never built as a mode. **Face** (per-face rotate/mirror/copy, spec `G06-6`) takes this row's place in the locked mode list instead. |
| Eyedrop | Purple | Copy texture/attributes from the clicked block to clipboard |
| Admin | Orange | Open admin panel for the clicked block |

### 2. Omni-Tool Config Chest GUI

Opened with Shift+RClick. Contains:
- Mode selector slots (one per mode, click to switch).
- "Configure active mode" slot → opens mode-specific sub-settings.
- "Cycling behavior" slot → customize which values cycle for Glow and Hardness modes.
- ~~"Save default" slot → save current mode as default for this player.~~ **Dropped, locked 2026-07-16
  (G06-16)** — not carried into the new Screen version.

> ⚠️ This whole chest-GUI is being replaced by a Screen (G06-16) — the list above is the OLD baseline;
> the new Screen keeps only mode selector + cycling behavior, see G06-16 below.

### 3. Held Block Dynamic Glow

Implementation: **Networked Client-Side Mixin** (Decision §7):
1. Server detects when a player is holding a custom block item with `glow > 0`.
2. Server sends a small packet to all nearby clients: "Player X is holding a block with light level Y."
3. Clients render a dynamic light source at the player's hand position using a Mixin.
4. Same effect when placing — light level jumps from hand position to block position.
5. Zero server TPS lag (light rendered client-side only).

> ⚠️ **This mechanism is currently broken — see live spec [G06-17](#g06-17--held-block-glow--broken-hand-glow--placement-lag) below.**

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

> ⚠️ **The whole Omni-Tool mechanism above (sections 1, 2, 4, 5) is under a full rework demand — see live
> spec [G06-16](#g06-16--omni-tool--tools-rework-umbrella) below.** Kept here as the original baseline spec /
> reference for what already exists.

---

## Colour Squares — 2026-06-20 additions

The colour **Squares** (the M3 colour-swap tools, `item/ShapeToolItem.java`) gained two changes this date.
Tests → `GROUP_06_TESTING_GUIDE.md` §B.

1. **Squares recolour placed auto-join Arabic letters.** Previously a Square only matched `SlotBlock` and
   did nothing on a letter. Now a Square on an `ArabicLetterBlock` recolours it to the Square's colour —
   **colour only**: it sets the letter's per-block colour and syncs, never touching the blockstate or the
   join flow, so FACING / form / neighbour joins are untouched. Green/Yellow/Red/Black map 1:1 to the
   bundled letter colours; instant, no pack reload. Full design → `GROUP_13_ARABIC.md` → **O11**.
2. **Cleaner swap wording.** The swap feedback now reads `Swapped to <DisplayName>` (clean name, not the
   raw id) and the same-colour case reads `Already <DisplayName>`. Plus all hotbar popups dropped the
   `[CB]` tag (owner request) — that part lives in Group 04 (`Chat.tool`).

---

## G06-1 · Instant Square swap feedback (locked 2026-07-15)

> 🔍 diagnosed — implementation not built. This is owned by Group 06 and tested in TG6 §B.

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
> ⛔ **The original fix mechanism designed for this (a shared `(Removed)` block) was superseded 2026-06-27
> by [G06-14](#g06-14--unified-recycle-bin-deletion-system-replaces-the-removed-system).** The diagnosis
> below is still accurate; the fix now ships via G06-14's Recycle-Bin rail. Historical decision text moved
> to the Archive section at the bottom of this doc.

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
keeps its old look until the client re-pulls the pack (the known "RP reload too slow" follow-up).
Looks like nothing happened, or happens late, or shows stale.

**Fix approach** — now ships through G06-14's shared `DeletionService` rail rather than a bespoke fix
here: refresh the clicked block instantly, fast remove-sync to all clients, and slot-index reuse is
guarded by G06-14's Trash reservation (not the old reuse-only guard). Omni-Tool Delete mode still to add.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — MP confirmed (refresh/recycle root causes hit SP too) | 🔍 diagnosed — fix ships via G06-14 |

**Related:** G06-1 (action-bar timing) · G06-14 (the fix rail) · known "placed blocks changing texture" bug
**Touches:** `DeleterItem.act` · `OmniToolItem` + `OmniToolState.Mode` · `SlotManager.delete` / `nextFreeSlotIndex` · G06-14's `DeletionService`
**Reference:** old mod `CustomBlocks/…/GuiManager.executeDeleterDelete` (the 3-step delete to copy)

---

## G06-3 · Delete-then-create scrambles placed blocks (worst with a color Square)

> 🔍 diagnosed — same root as G06-2; fix ships via [G06-14](#g06-14--unified-recycle-bin-deletion-system-replaces-the-removed-system)'s
> Recycle-Bin rail (slot reservation replaces the old reuse-only guard this section originally proposed).

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

**Fix** — shares G06-2's core fix rail (G06-14); the extra pieces below are specific to the Square/variant
path and pack-thrash, and are not yet folded into a build slice:

1. ~~Stop instant index reuse~~ — now covered by G06-14's Trash slot reservation.
2. ~~Refresh the deleted block~~ — now covered by G06-14's marker swap.
3. **Make the Square/variant path collision-safe** — once reuse is guarded, a freshly-created variant can't land on an index an existing placed block is using. *(verify once G06-14 lands)*
4. **Tame the recolor pack-thrash** (ties to G06-1) — batch/debounce the `updatePack()` storm from per-click variant creation so the world doesn't reload-flash on every color. *(still open)*

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — dedicated confirmed; cause is environment-independent | 🔍 diagnosed — core fix ships via G06-14; items #3/#4 still open |

**Related:** **G06-2** (same slot-recycling root) · **G06-1** (recolor pack-thrash / lag) · **G06-14** (the fix rail) · known "placed blocks changing texture" bug
**Touches:** `ColorVariantService.createVariant` / `swapPlaced` (collision-safety once slots are reserved) · `ResourcePackServer.updatePack` (debounce)
**Note:** "much worse stuff" is partly un-itemized — developer will specify more after this pass; log new sub-symptoms here as they're named.

---

## G06-5 · Color-variant names compound wrong ("Block Yellow (Green)")

> ✅ **BUILT (found 2026-07-16).** Fixed: names now read "Mars Green" (base + colour word); nameless hexes take
> the nearest of 29 palette colours. The analysis below is kept for history — the fix already ships.

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

> ✅ **CORE BUILT (found 2026-07-16).** The hex now syncs to clients, tool names/lore update live, and
> repaint re-bakes each variant from its source (no more brittle pixel match). The analysis below is kept
> for history. **Still not built:** the two "go beyond" extras — live-tint the fixed-4 tool icons (the
> pack-baked path already works) and free-text anvil rename/lore. A gradient/tone subsystem also exists.

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
  - **(1) Name + describe each colour tool** — rename + custom lore per tool, self-documenting hotbar. Stored on tool NBT (same place as `cb_rgb`). **Locked 2026-07-16: free-text, anvil-style rename** — player types any name/lore they want, same UX as a vanilla anvil rename, not locked to auto-generated presets.
  - **(2) Gradient / tone-mix tool** — a tool that blends two colours or applies a gradient (extends `ColorReplacer.tint` / `RecolorToneTools` to a 2-stop blend). **UX undecided (2026-07-16)** — owner unsure whether this is a new physical tool item or a panel inside the Colour Studio GUI; decide during build by looking at existing tool-item patterns.
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

## G06-7 · Block Drops Customization — **MOVED to Group 27** (2026-07-16)

> Full spec + all locked decisions relocated to [GROUP_27_SCREENS.md](GROUP_27_SCREENS.md) /
> [Group_27_Testing_Guide.md](../testing/Group_27_Testing_Guide.md) — it lives as a `Section.DROPS`
> Studio tab under `/cb create`, owned entirely by Group 27 now. SELF-default baseline (confirmed in-game
> 2026-07-09 via Group 30 §R3) already shipped and stays shipped; the rest of the system (NONE/CUSTOM
> modes, loot entries, XP, tool-tier gating, the Drops screen) builds under G27's ownership.
>
> **G06 no longer tracks this feature.** See Group 27's docs for current status. *(Note: the actual spec
> body — drop modes, loot entries, safety cap, item picker, etc. — still needs to be physically copied into
> Group 27's doc; this move only updates G06's ownership/tracking. Do that copy before building.)*

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

## G06-18 · Rainbow Rectangle — wrong job built (Area/corner-select doesn't belong)

> 🔍 diagnosed 2026-07-16 (owner) — not yet fixed.

> Owner: *"it should repaint faces not area, it was incorrectly understood."*

**Symptom** — Rainbow Rectangle (TG §A, the existing per-face paint tool) currently includes an
Area/corner-select mode: sneak+right-click two blocks marks "Corner 1 set… / Area selected …" for a bulk
multi-block operation (TG §A row A4). **This was a misunderstanding of the tool's actual job.** The real
job is simple: paint one face of **one block** at a time from an image URL (already the core of A1–A3,
A5–A7). Area/corner-select serves no real purpose here and should not exist.

**Fix:** remove the Area/corner-select behavior entirely. Rainbow Rectangle becomes purely a single-block,
per-face paint tool — right-click a face → paste URL → that face updates. No corner-marking, no bulk/area
target.

**Downstream effect:** this is also why **Area was dropped from G06-16's Omni-Tool mode list** — Area's
only conceivable job (per-face repaint) is Rainbow Rectangle's job, not a separate Omni-Tool mode.

**TG update needed:** row A4 ("Sneak + right-click two blocks → Corner 1 set… then Area selected …") tests
behavior that should no longer exist — remove or replace once this fix ships.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — client tool + server-side paint | 🔍 diagnosed — not built |

**Related:** TG §A (Per-face, tests this tool) · G06-16 (Area dropped from Omni-Tool for the same reason) · G06-6 (Face mode — the Omni-Tool's actual per-face system, different tool)
**Touches:** the Rainbow Rectangle tool item (name TBD — verify actual class, likely under `item/`) — remove corner-select/area state, keep single-face paint-from-URL.
**Verify in-game:** right-click any single face → texture updates on that face only; sneak+right-click no longer starts a corner-select flow; existing A1–A3/A5–A7 behavior unaffected.

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
> **every** delete path. Supersedes the `(Removed)` mechanism previously designed under G06-2 (moved to
> Archive below) and resolves **G06-2 + G06-3 + G05-2**; the trash side is **[G09-2](GROUP_09_BACKUP_SAFETY.md)**.

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
| 7 | Reserved slot number in Trash GUI | **Shown** *(locked 2026-07-16)* — each trashed block's entry displays its reserved slot number, not hidden |

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
   - **2a. NO-REJOIN sweep** *(the undo-rejoin fix + the same lag everywhere)* — 🟢 **BUILT 2026-06-27, awaiting in-game.** `/cb undo`+`/cb redo` now `HudSync.broadcast` after their steps; the same live-refresh added to all attribute setters (glow/hardness/sound/collision/category/shape) across **commands, tools (Lumina/Chisel/Omni), menus, and bulk ops**; and the older actor-only paths (reid/anim/notes/category-admin/studio/vault) upgraded to refresh **all** players (owner's "everyone online" choice). NO-REJOIN principle banner written into `HudSync` (grep tag `NO-REJOIN`). TG **§I** (live resync).
3. **Route bulk delete + GUI delete + broken-cleanup** through the same rail. *(Test: bulk delete → markers everywhere, no purple blocks, other players update without rejoin.)* — bulk **delete** is the only delete path still on the old route; this slice moves it to `DeletionService` (+ folds in any remaining no-rejoin gap for deletes).
4. **Trash reserve/restore** — reserve slot while in Trash; Restore re-creates on a fresh slot + heals markers by name (loaded + on-load). *(Test: delete → restore → markers turn back; restart → still restores.)*
5. **Empty frees the slot + tombstones markers**; remove the forever chunk-scanner and the permanent no-reuse leak. *(Test: empty → number reusable; emptied markers don't revive on same-name create.)*

| Group | Scope | Status |
|---|---|---|
| G06 — Tools (deletion) + G09-2 (trash) | Both SP & MP — server-authoritative; replaces G06-2/G06-3/G05-2 `(Removed)` mechanism | 🟡 **slices 1+2/5 ✅ in-game**; **slice 2a NO-REJOIN sweep 🟢 build-green 2026-06-27** (awaiting in-game); 3 slices left |

**Related:** [G06-2](#g06-2--deleter-doesnt-delete-properly--buggy-mp) / [G06-3](#g06-3--delete-then-create-scrambles-placed-blocks-worst-with-a-color-square) / G05-2 (the bugs this resolves) · [G09-2 trash](GROUP_09_BACKUP_SAFETY.md) · G07-3 bulk delete (now shares the rail)
**Touches:** new `DeletionService` · new `DeletedMarkerBlock` (+BE) · new `MarkerResolver` · new `TrashSlots` · `TrashCommands` + trash GUI · `DeleteCommands` / `SlotBlock.cbDelete` / `BulkCommands` / `BulkConfirmMenu` / `BrokenConfirmMenu` (route through rail) · `SlotManager.delete` / `nextFreeSlotIndex` (reserve not retire) · **removes** `RemovedBlock` / `RemovedPlacements` / `DeletedPlacementSweeper` / `FreedSlots`
**Verify in-game:** every delete method leaves `Deleted: <name>` markers (no purple blocks); markers break instantly; Restore brings the block back on a new number + flips markers everywhere (incl. after restart); Empty frees the number and emptied markers don't revive; no per-chunk lag while exploring after deletes.

---

## G06-16 · Omni-Tool & tools rework (umbrella)

> 🟡 **mode list + config-UI scope locked 2026-07-16 (owner interview).** Merges 4 previously-loose topics
> under one ID (TG §E): the dedicated creative tools tab, Chisel + Lumina unification, tool-give shortcuts,
> and tool consolidation. **Blocked** — see Build order below.

> From the 2026-06-21 in-game test follow-ups: *"Tool works, but developer wants the entire Omni-Tool
> mechanism + the tool-give shortcuts redesigned."*

**What exists today** — the baseline Omni-Tool spec (modes, config GUI, tools tab, give-shortcuts) is
already documented above under **Implementation Requirements** (sections 1, 2, 4, 5) and was confirmed
working in-game 2026-06-21 (old tests G06.1–.4, .8, .9 — see Archive). That baseline is not broken; the
owner wants it **redesigned**.

### ✅ Locked decisions (owner interview 2026-07-16)

**Mode list — final:** `Glow, Hardness, Delete, Face, Paint, Eyedrop, Admin`.
- **Delete** = G06-2/G06-14's Recycle-Bin delete, added as a mode (standalone red Deleter item stays too — not folded away).
- **Face** = G06-6's per-face rotate/mirror/copy, added as a mode.
- **Area is dropped entirely.** Its only real job (per-face repaint) was a misunderstanding of Rainbow
  Rectangle's actual spec (see **G06-18**) — Area's corner-select/bulk-apply concept serves no purpose and
  is not being rebuilt as an Omni-Tool mode.
- **Paint / Eyedrop / Admin** keep their original Implementation Requirements §1 behavior — not redesigned this pass.

**Tool consolidation — locked:** standalone tools **stay separate physical items**. Deleter, colour Squares,
and Triangles do **not** fold into Omni-Tool modes (Delete mode exists on the Omni-Tool as an *additional*
way to delete, alongside — not replacing — the standalone Deleter).

**Config UI — becomes a Screen, not a chest GUI.** The Shift+RClick config menu (Implementation
Requirements §2) moves off the chest-GUI pattern flagged by the UI medium audit and becomes a proper
Screen, built against **Group 27's Screen pattern** (the same infra G27 uses for its own Studio screens).
**Locked 2026-07-16 — Screen keeps the "Cycling behavior" customization** (picking which glow/hardness
values are in the cycle) but **drops "Save default"** (no longer a feature) — mode selector + cycling
config only, nothing else from the original §2 chest-GUI content.

**Build order — this feature is BLOCKED.** The Omni-Tool item, its modes, and its give-shortcuts **stay
owned by G06** (not moved to G27, unlike Drops) — but the whole rework **cannot start** until G27 has a
working Screen pattern to build the config UI against. Check G27's status before starting; if no reusable
Screen pattern exists yet, that becomes a prerequisite step, not a G06 task.

**Creative tab + give-shortcuts — stay in G06 regardless.** Whatever happens to Omni-Tool's config UI, the
"CustomBlocks Tools" creative tab and the `/cb brush` / `/cb chisel` / etc. give-shortcuts are owned by G06
and are not affected by the Screen-move. Their own redesign (if any) is still open — not yet discussed
beyond "they exist and give the right items" (Implementation Requirements §4–5, confirmed working).

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — client item + server-side give commands | 🟡 mode list + config-UI scope locked; **blocked on G27 Screen pattern** |

**Related:** Implementation Requirements §1–5 (the existing baseline this extends) · G06-2/G06-14 (Delete
mode source) · G06-6 (Face mode source) · G06-18 (Rainbow Rectangle — why Area was dropped) · Group 27
(Screen pattern this is blocked on)
**Touches:** `OmniToolItem` + `OmniToolState.Mode` (add DELETE, FACE; remove AREA) · new Omni-Tool config
Screen (replaces the chest-GUI `OmniMenu`) · tools tab / give-shortcut commands unaffected.

---

## G06-19 · Hex-change full pack-rebuild kicks players (TG6 §G9)

> 🔍 diagnosed 2026-07-16 (server-log repro + code trace) — not built.

> Owner (TG6 test G9): a single `/cb config hex green #00FFAA` → No caused a kick/lag-out, and after
> restart some blocks stayed on the old colour (some `#00ffaa`, some not) — desync between clients.

### Confirmed from server log (`Desktop\latest.log`, 2026-07-16 run)

```
05:35:12  Pack zip emit start (assigned=1718, maxSlots=3000, textureSize=512)
05:35:12  Pack zip emit done (216790 KB, 4785 ms)
05:35:12  Pack manifest ready (9450 files...) → Sent pack manifest to 3liSY
05:35:17  Pack zip emit start ... (SECOND full rebuild, 5s later)
05:35:17  Pack zip emit done (216790 KB, 4785 ms) → Sent pack manifest to 3liSY again
05:35:49  3liSY lost connection: Disconnected   (client-side timeout, no server exception)
```

One hex-tint change triggered **two** full 216MB zip rebuilds + two full 9450-file manifest resends,
~5s apart. Client choked downloading/applying two full packs back to back, disconnected ~32s later.
Post-restart colour desync (`#00ffaa` on some blocks, not others) is the client having cached a
half-applied pack version from the interrupted second sync.

### Root-caused (code trace, 2026-07-16)

- **Full-rebuild-on-any-change is the real design today, not a bug.** `ResourcePackServer.rebuild()`
  ([ResourcePackServer.java:366](../../src/main/java/com/customblocks/core/ResourcePackServer.java#L366))
  always calls `ServerPackGenerator.generate()` for the **entire** zip (all assigned slots up to
  `maxSlots`), then `sendToAll()` + `PackSyncService.refresh()` resend the **full** manifest to
  **every** online player — regardless of whether one hex tint or a thousand slots changed. There is no
  partial "just these retinted textures" rebuild path anywhere in `ResourcePackServer` or
  `ServerPackGenerator`. `PackSyncService` already diffs file-by-file for *what a client actually
  downloads*, but the zip build and the manifest itself are always full-pack.
- **The doubled rebuild is not explained by the hex command path.** Traced every branch —
  `HexCommands.setHex` (single `updatePack()` call when no existing blocks use that colour), the
  `HexRecolorConfirmMenu` Yes/No flow (`onClose` calls `updatePack()` exactly once, `choseYes`-guarded
  so Yes's `/cb recolorvariants` can't double-fire it), and `ColorVariantService.recolorVariants`
  (also one call). `updatePack()` itself CAS-guards (`rebuildScheduled`) against double-scheduling
  within its 500ms debounce window — but the two rebuilds in the log are **5 seconds apart**, well
  outside that window, so the debounce never even saw the second trigger. Something else fired a
  second `updatePack()` independently; static code trace can't pin down what without a live repro.

### Owner-scoped fix (2026-07-16) — hex-tint path only, not every rebuild trigger

Owner explicitly narrowed scope: only fix the hex-change rebuild storm, not every `updatePack()` caller
(create/delete/recolorvariants/trash restore are out of scope here — no evidence they have the same
problem).

1. **Targeted rebuild** — skip regenerating zip entries for slots/blocks that don't use the changed hex
   colour at all; only touch the affected variant textures instead of all 3000 slots.
2. **Safety-net cooldown** — add a longer cooldown on top of the existing 500ms debounce (block a repeat
   full-rebuild within N seconds of the last one), since the observed double-fire happened outside the
   debounce window and the actual second-trigger cause is still unconfirmed.
3. **Client-side patience** — add a loading/patience indicator during pack sync instead of letting the
   client hard-timeout/disconnect while a legitimately-slow sync is still in progress.

**Second-rebuild trigger — deliberately not chased this round.** Owner chose not to add debug logging or
do a live repro to catch it; noted here as a known open question for whoever picks up G06-19 next —
correlate `Server thread` vs any other thread firing `updatePack()` within the same few seconds, if it
recurs.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools / resource pack sync | Both — server-side rebuild + client-side sync | 🔍 diagnosed 2026-07-16 — design locked, not built |

**Related:** TG6 §G (hex system, G06-4) · TG6 §G9 (the failing test) · `ResourcePackServer` / `ServerPackGenerator` / `PackSyncService`
**Touches:** `ResourcePackServer.rebuild()` (targeted rebuild + cooldown) · `ServerPackGenerator.generate()` (skip unaffected slots) · client pack-sync handling (loading indicator, avoid timeout-disconnect)
**Verify in-game:** repeat TG6 G9 (`/cb config hex green #00FFAA` → No) on a dedicated server → no disconnect, no post-restart colour desync; rebuild only touches slots using the changed hex.

---

## G06-17 · Held-block glow — unbuilt hand-glow + placement lag

> 🔍 **diagnosed 2026-07-16** (code investigation + web research, both symptoms root-caused — no repro
> needed, confirmed still-current by owner). Merges the hand-glow feature (old test G06.6, confirmed
> failing 2026-06-21) with the placement-lag bug (`G06-GLOW-LAG`, found 2026-07-10) under one ID (TG §D).

### Symptom 1 — hand glow does nothing: it was never built

Implementation Requirements §3 specs a "networked client-side mixin" (server detects the held glowing
item, sends a packet, client renders light via mixin). **Confirmed by code search: none of it exists.**
Checked every class in `mixin/` (11 files: `AbstractBlockStateMixin`, `BipedArmPoseMixin`,
`ChatClickFxMixin`, `ClientCommonNetworkHandlerMixin`, `DisconnectedScreenMixin`, `HudRenderMixin`,
`ItemDisguiseMixin`, `ParticleDisguiseMixin`, `RegistrySyncHealMixin`, `ScreenInvoker`,
`package-info`) — none touch light or rendering. Grepped the whole codebase for any held-light payload or
resolver — nothing. **This is not a bug, it's unimplemented.** The 2026-06-21 test confirmed the symptom;
the code confirms why.

**Fix direction (researched 2026-07-16):** the modern, standard way to do this in Fabric 1.21 is **entirely
client-side** — no server packet needed at all. Reference implementation: **LambDynamicLights** (the de
facto mod for this, MIT-licensed, source on GitHub) tracks light-emitting items the player holds/wears/
drops and updates lighting locally using the vanilla `minecraft:light` block trick (place an invisible
client-side-only light block at the source position via a client-side `setBlockState`, move it as the
source moves, remove it when gone) — this has been the standard technique since the `minecraft:light`
block was added in snapshot 21w13a (≈1.17). No custom lighting-engine mixin required.

Since the client can already resolve a held item's configured glow value locally (mirroring the
`CLIENT_NAME_RESOLVER`/`CLIENT_SOUND_RESOLVER` pattern already built for name/sound — see Symptom 2's fix,
which needs the equivalent `CLIENT_GLOW_RESOLVER` anyway), **no new S2C payload is needed at all** — this
simplifies the original §3 spec, which assumed a server-push packet. Client already knows its own held
item's glow once the resolver exists; it can place/move/remove the light block purely client-side, every
tick, with zero server involvement and zero TPS cost (matches §3's "Zero server TPS lag" goal, just via a
simpler mechanism than originally spec'd).

**Scope locked 2026-07-16 — held AND dropped items both glow.** Matching LambDynamicLights' full behavior
(not held-only, as the original §3 draft implied): a glowing custom block also lights the area while it
sits as a dropped `ItemEntity` on the ground, not just while held. Same client-side light-block mechanism —
track dropped-item-entity positions in render range too, not just the local player's held-item slot.

### Symptom 2 — placed-block glow appears late: real bug, root-caused

[SlotBlock.java:208-210](../../src/main/java/com/customblocks/block/SlotBlock.java#L208-L210):
```java
public BlockState getPlacementState(ItemPlacementContext ctx) {
    return getDefaultState().with(LIGHT, SlotManager.glowFor(slotIndex));
}
```
`SlotManager.glowFor` ([SlotManager.java:415-418](../../src/main/java/com/customblocks/core/SlotManager.java#L415-L418))
reads the in-process `SlotManager.BY_SLOT` map directly, with no dedicated-client fallback. This is the
**exact same client/server data-split gap** already fixed for name (`CLIENT_NAME_RESOLVER`) and sound
(`CLIENT_SOUND_RESOLVER`) on `SlotBlock` — but glow never got the equivalent resolver.

**Why this causes a visible delay:** on a dedicated server, Minecraft's client predicts a block placement
locally (calls `getPlacementState` client-side for the preview) before the server's authoritative packet
arrives. On the client, `SlotManager` is empty (dedicated clients never populate it — same reason
name/sound needed resolvers), so `glowFor` returns 0 → the client's predicted placement is briefly unlit.
The server computes the correct state and sends its update; only once that packet lands does the client
swap to the correctly-lit state. That gap between client-predicted (unlit) and server-confirmed (lit) reads
as "light appears late, well after the block is placed" — exactly the reported symptom. Singleplayer/host
never shows this (shared JVM, `glowFor` is always correct even in the client-side predicted call), which
matches the report surfacing specifically in MP/dedicated testing (G07, 2026-07-10).

**Fix:** add `SlotBlock.CLIENT_GLOW_RESOLVER` (an `IntFunction<Integer>`, same shape as
`CLIENT_SOUND_RESOLVER`), synced the same way (`ClientSlotCache` / `HudSync`), and branch
`getPlacementState` on `CLIENT_REMOTE_SESSION` the same way `resolveSound` already does — falling back to
the resolver when the local `SlotManager` has nothing. This directly mirrors existing, working code; no new
pattern invented.

**Shared groundwork:** the `CLIENT_GLOW_RESOLVER` built for Symptom 2 is exactly what Symptom 1's
client-side light-block tracker needs to read a held item's glow value on a dedicated client — build
Symptom 2's resolver first, Symptom 1 reuses it.

| Group | Scope | Status |
|---|---|---|
| G06 — Tools | Both — Symptom 1 client-only; Symptom 2 hits dedicated MP (SP/host unaffected) | 🔍 diagnosed 2026-07-16 — both root-caused, ready to build |

**Related:** Implementation Requirements §3 (the original hand-glow spec — simplified by this diagnosis,
no server payload needed) · `SlotBlock.CLIENT_NAME_RESOLVER` / `CLIENT_SOUND_RESOLVER` (the pattern
Symptom 2's fix mirrors exactly) · `SlotLighting` (the retroactive `/cb setglow` updater — unaffected,
already correct)
**Touches:** new `SlotBlock.CLIENT_GLOW_RESOLVER` + wiring in `getPlacementState` (Symptom 2) · new
client-only light-block tracker mixin/system reading the held item's glow via the same resolver (Symptom 1)
· `ClientSlotCache` / `HudSync` (carry glow, same as name/sound)
**Verify in-game:** place a glowing block on a dedicated server → light appears at the same moment as the
block, not late; hold a glowing block item in the dark → light radiates from hand instantly, visible to
other nearby players, moves as the player moves, clears when the item is put away.

---

<details><summary>🗄️ <b>Archive (historical, superseded — do not build against)</b></summary>

### Historical — original numbered tests (G06.1–G06.9) + Setup + Verdict

Superseded by `docs/testing/Group_06_Testing_Guide.md`'s lettered sections (A–J). Orphaned since
2026-07-10 — did not map 1:1 to the TG even before the letter/merge cleanup.

**Setup**

```
/cb create g06a GlowBlock
/cb setglow g06a 12
/cb give g06a
```

Place `g06a` nearby. Pick up the block item in your inventory.

**Test G06.1 — Omni-Tool give**

```
/cb brush
```

Expected: Omni-Tool given with Glow mode active. Item name shows current mode (e.g., "Omni-Tool [Glow Mode]").

**Test G06.2 — Omni-Tool right-click cycles glow**

Hold the Omni-Tool in Glow mode. Right-click on the placed `g06a`. Expected: Glow cycles `0 → 4 → 8 → 12 → 15 → 0`, chat/action bar shows current level.

**Test G06.3 — Omni-Tool config chest GUI opens**

Hold Omni-Tool. Shift+Right-click on any custom block. Expected: chest GUI with mode selector slots, cycling config, save default slot.

**Test G06.4 — Omni-Tool mode switch in GUI**

Click the "Hardness" mode slot. Expected: active mode changes, item reflects it, right-click cycles hardness.

**Test G06.5 — Deleter give and use**

```
/cb deleter
```

Right-click the placed `g06a`. Expected: removed instantly, no drop, `/cb undo` restores it.

**Test G06.6 — Held block dynamic glow**

Hold `g06a` (glow = 12) in a dark area. Expected: light level 12 radiates from hand, visible to other players.

**Test G06.7 — Glow on placement**

> REWORDED 2026-06-21: placed-block ground light is normal block lighting and works; the hand→placed
> transition only matters once G06.6 works (it doesn't).

**Test G06.8 — Creative tools tab contents**

> CORRECTED 2026-06-21: "exactly 4 items" was wrong — the tab houses MORE than 4 tool items, intended. Real
> check: it's the tools tab, contains tool items, no custom blocks.

**Test G06.9 — Tool-give shortcuts all work**

```
/cb chisel
/cb square
/cb triangle
/cb rectangle
/cb hexagon
```

Expected: each gives the correct item + pre-selected mode.

**Group 06 Verdict (historical)**

| Test | Description | Result |
|---|---|---|
| G06.1 | Omni-Tool given via `/cb brush` | ✅ in-game (2026-06-21) — whole mechanism reworked later (G06-16) |
| G06.2 | Right-click cycles glow | ✅ in-game (2026-06-21) — rework pending (G06-16) |
| G06.3 | Config chest GUI opens via Shift+RClick | ✅ in-game (2026-06-21) — rework pending (G06-16) |
| G06.4 | Mode switch in GUI works | ✅ in-game (2026-06-21) — rework pending (G06-16) |
| G06.5 | Deleter removes block instantly, undoable | ⚠️ stale ✅ — contradicted by G06-2's diagnosis (leftover ghosts, texture bleed); treat as 🔍 diagnosed until reconciled via G06-14 |
| G06.6 | Held block emits dynamic glow | ❌ in-game (2026-06-21) — NO glow from hand. Folded into **G06-17**. |
| G06.7 | Placed block emits ground light | ✅ in-game (2026-06-21) — works; transition depends on G06.6/G06-17 |
| G06.8 | Creative tools tab contents | ✅ in-game (2026-06-21) — spec corrected, folded into G06-16 |
| G06.9 | All tool-give shortcuts work | ✅ in-game (2026-06-21) — work; rework pending (G06-16) |

### Historical — Follow-ups (2026-06-21 in-game test)

8 of 9 tests pass; G06.6 fails.

- **G06.6 — held-block hand glow BROKEN.** → superseded by **G06-17**.
- **Omni-Tool full mechanism rework (G06.1–.4, .9).** → superseded by **G06-16**.
- **G06.5 — Deleter polish** (weak feedback, slow RP reload). → superseded by **G06-2** / **G06-14**.
- **G06.7 spec reworded; G06.8 spec corrected** (tab may house >4 items — intended, folded into G06-16).

### Superseded — the original `(Removed)` delete mechanism (pre-G06-14)

> ⛔ Superseded 2026-06-27 by **G06-14**. `RemovedBlock`, `DeletedPlacementSweeper`, `RemovedPlacements`,
> and `DeletedSlots`' permanent-retire behaviour were all ripped out and replaced. **Do not build against this.**

**✅ Decisions (locked via UI 2026-06-26) — "improved Option 2", shared `removed` block**

On delete (red Deleter **and** `/cb delete` — both already route through `SlotManager.delete`):

1. **Swap every placed copy → one shared `RemovedBlock`** (`customblocks:removed`): neutral grey
   texture, name "(Removed)", drops nothing, breaks instantly. Not a `SlotBlock` → tied to no slot index.
2. **Never reuse the freed slot index.** A deleted index went into a persisted `DeletedSlots` set;
   `nextFreeSlotIndex` skipped it forever.
3. **Instant, no-rejoin refresh** — on delete, immediately sweep loaded chunks and swap matching
   `slot_N` → `RemovedBlock`. Far/unloaded copies swapped on next chunk load.
4. **Undo of a delete un-retired the index** and restored the greyed placements via an in-memory
   journal (`RemovedPlacements`).

Mechanism mirrored the Arabic cleanup: `block/RemovedBlock.java`, `core/DeletedSlots.java`,
`block/DeletedPlacementSweeper.java`, `block/RemovedPlacements.java`.

**Why it was scrapped (not patched), per G06-14:**

1. The delete paths drifted — bulk delete never called the sweep, showed broken purple blocks.
2. The chunk-scanner never stopped — full block-by-block scan of every loaded chunk, forever.
3. Slot numbers leaked permanently — no reclaim mechanism.
4. Markers carried no identity — undo needed a fragile in-memory journal that died on restart.

</details>

---
