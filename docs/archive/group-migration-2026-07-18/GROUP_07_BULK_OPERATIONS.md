# Group 07 — Bulk Operations

> **Objective:** do one thing to many blocks at once — safely, undoably, and from one professional Screen.
>
> **Rules:** chat batches over `bulkConfirmThreshold` (default 10) must confirm before running; the Screen
> confirms every Execute in its own modal. Bulk recolor is **hue-only** — `ColorMath.hslShift(png, hue, 1.0, 1.0)`,
> saturation/lightness held at 1.0. (The CLAUDE.md "edge mode, never full mode" pitfall has **no API to bind to**:
> grep proves there is no edge/full recolor mode in this codebase — `"full"` is only a block *shape*. Hue-only is
> the reversible, batch-safe reading of that rule.) Locked blocks are skipped by the six ops that *modify* them,
> and never silently — see §4.

> **Rewritten 2026-07-10.** The previous version of this doc carried a "What this group restores" table
> claiming every op was *Missing*, nine chest-era numbered tests (G07.1–.9), and a §5 describing a
> four-menu chest GUI. All three were stale — the ops are built, and the chest flow is now deleted.
> Pass/fail state lives in `GROUP_07_TESTING_GUIDE.md`, not here (see [[feedback_verify_tg_against_source]]).
>
> **§G07-3 BUILT 2026-07-10** (all 3 slices, build-green, **not in-game**). Two long-standing doc errors were
> corrected against source in the same pass: `bulkduplicate` has no `<id-prefix>` argument, and only 6 of the
> 10 ops skip locked blocks.
>
> **🔄 HUB REBUILT 2026-07-12 to the locked mockups (§G27.22 + §G27.22b)** — full `./gradlew build` green,
> **not in-game**. The Hub is now **2 tabs** (Extra/Health cut) with **10 ops** (net-new **Recolor**). This
> rebuild *removed* three earlier features: the 3×3 op-picker grid, the AND/OR/NOT **filter builder** +
> **escalation**, and the per-block **Re-ID box editor**. Anything below describing those is historical — see
> "§G07-5 Hub rebuild" for what actually ships.
>
> **Golden Rule:** 🟢 built ≠ done. Nothing below is ✅ until confirmed in-game.

> ✅ **2026-07-15 (MP run) — the CHAT half of bulk is confirmed working.** `/cb bulkdelete a b c` deletes exactly
> those three with no second confirm (TG7 B6 / TG4 A7), and `/cb bulkdelete category:red` answers **"No blocks
> matched"** instead of silently matching nothing (TG7 B8 / TG4 A7b). The dead filter language really is gone
> from `BulkScope`.
>
> ❗ **The Hub half is now the ONLY thing left of G07-HUB-FILTER** — the Console tab + NL bar still *build*
> `category:`/`id:`/`name:`/`favorite:`/`locked:` strings and send them to a server that stopped resolving them.
> Fix = strip the filter builder from the Hub; the Console picks by tickbox.
>
> 🟡 **G07-BULK-UNDO — BUILT 2026-07-15** (one pass with G04-UNDO-DIALECT), awaiting in-game confirm on
> **G04 §G**. Bulk lock/unlock/favorite recorded **nothing** in `UndoManager` (so `/cb undo` skipped them and
> undid whatever came *before*), and their `▶ [undo]` chip ran the **inverse command** rather than `/cb undo`.
> Now: every bulk op records one batch — flags via the new `UndoManager.Kind.FLAG`, which carries
> `(id, lock|favorite, on, ownerUuid)` because lock/favorite live in `LockManager`/`FavoritesManager`, **not**
> on `SlotData`, so the snapshot-restore path every other Kind uses cannot reach them. Only `↩ Undo` / `↪ Redo`
> survive as affordances. See `GROUP_04_Communication.md`.

---

## Where this group actually stands

The **backend was already built and green**; the **front-end has been replaced twice**. **Ten** bulk operations
run through tested command handlers, a shared filter resolver, a confirm guard, and single-entry batch undo.
As of 2026-07-10 the four chest menus and the chest block list are **deleted**, and one Screen replaces them
(§G07-3); as of 2026-07-12 that Screen is rebuilt to §G27.22/§G27.22b (§G07-5). 🟢 build-green, **not in-game**.

### Built — backend, verified against source

| Op | Command | Handler | Undo | Skips 🔒? |
|---|---|---|---|---|
| Edit a setting (glow/hardness/sound/collision) | `/cb bulkproperty <filter> <prop> <value>` | `BulkCommands` | one batch entry | yes |
| Delete | `/cb bulkdelete <filter>` | `BulkCommands` | one batch entry | yes |
| Rename (prefix/suffix/replace) | `/cb bulkrename <filter> prefix\|suffix\|replace …` | `BulkCommands` | one batch entry | yes |
| Move to category | `/cb bulkcategory <filter> <category>` | `BulkCategoryCommands` | one batch entry | yes |
| Duplicate | `/cb bulkduplicate <filter>` | `BulkDuplicateCommands` | one batch entry | **no** |
| Export | `/cb bulkexport <filter> <format>` | `BulkExportCommands` | n/a (file write) | **no** |
| Lock / Unlock | `/cb bulklock <filter>` · `/cb bulkunlock <filter>` | `BulkFlagCommands` | ✅ one batch of `Kind.FLAG` | **no** |
| Favorite / Unfavorite | `/cb bulkfavorite <filter>` · `/cb bulkunfavorite <filter>` | `BulkFlagCommands` | ✅ same — the batch remembers the owner | **no** |
| Re-ID | `/cb bulkreid <filter> <mode> <text>` | `BulkReidCommands` | one batch entry | yes |

> **Two corrections to this table (2026-07-10, read from source):** `bulkduplicate` takes **no** `<id-prefix>`
> argument — every copy is named `<id>_copy`, then `_copy2`, `_copy3`. And "locked blocks are always skipped"
> was never true of all nine: Duplicate, Export and the flag ops only *read* the source block, so they act on
> locked blocks normally. The Screen's preview now mirrors the real per-op rule rather than a uniform one.

**Shared engine:** `core/BulkScope.java` (filter resolver, §1) · `bulkConfirmThreshold` config ·
`/cb confirm` / `/cb cancel` with a 60s window (`BulkConfirm`) · clickable `[✔ Confirm]` / `[✖ Cancel]`
chat buttons with a hover id list (`BulkChat`) · tab-completion (`BulkSuggestions`).

### Not built (verified against source 2026-07-12)

- `bulksound` as its own literal (today only reachable via `bulkproperty … sound …`).
- **`/cb bulkrecolor` (Bulk Recolor Hub)** — **MOVED to Group 10** (2026-07-12): see `GROUP_10_COLOR_IMAGE.md` §G10-BRH. It is a bulk front-door whose engines live in G10 (background) + G06 (re-bake); no longer tracked in G07.
- `bulkshape` — ❗ blocked on Group 08.
> **⚠️ Re-verified against source 2026-07-12 (2nd pass): the Console-tab rows previously listed here as "not
> built" were STALE — they ARE built (build-green, matching §G07-4 Slice 2/3 and the Testing Guide's "retest"
> state). Confirmed in source: the 3×3 op-picker (`BulkOpsView.renderGrid`; bare `/cb bulk` →
> `BulkSnapshot.openFor("bulk")` → `opGrid`; a named `bulk*` → `openForOp` → straight to its tab, skips the
> grid), the Dir-2 top toolbar (`BulkOpsView` — 2 rows, inline `old→new`, no preview pane), the B3 rename
> redesign (per-mode labels + `BulkOpText.example`, all 3 modes), the B9/B9a Re-ID hybrid (`renderReidEditor`
> — per-block boxes + Fill-all + live per-row marks), the B12 confirm modal (`renderModal` — opaque, "Deploy N
> blocks?", id list, always-opens), and quoted multi-id + AND/OR/NOT filters (`BulkScope`). Don't re-build any
> of these. What is GENUINELY still unbuilt:**

> **🟢 Built 2026-07-12 (Slice 1) — build-green, NOT in-game:** **A2** scrollbar drag (`BulkDraw.vscroll` grab
> band + `BulkWorkbenchScreen.mouseDragged`/`scrollToMouse` on both the Catalog and Console lists) · **S4**
> sound seam (`SlotBlock.CLIENT_SOUND_RESOLVER`/`resolveSound` + `CustomBlocksClient` install — sound now
> applies on a dedicated client, mirroring the shape/name/lore seams; no core/HudSync change, `sound` was
> already synced) · in-screen **Undo/Redo** buttons (`BulkActionPayload.UNDO/REDO` → `BulkNet` →
> `HistoryCommands.undoOnce/redoOnce` → `pushAll`) · confirm modal verb renamed **Deploy → Confirm** (owner
> 2026-07-12), B12 mockup shown to owner. **X3** dual feedback — new `BulkResult` relay + a `BulkSnapshot`
> `"result"` field + an in-screen **Result** modal (`BulkOpsView.renderResultModal`); every apply core records
> its outcome, so it shows on-screen AND stays in chat (a Hub open clears any stale chat-path result first).
> Verify: `GROUP_07_TESTING_GUIDE.md` §B23, §B24, §S4 · screen rows → `GROUP_27_TESTING_GUIDE.md` §T.

Still genuinely unbuilt:

- ~~**A1/A4 screen-chrome** (§G27.22)~~ — 🟢 **REBUILT 2026-07-12** (build-green) to the locked §G27.22 mockup: rotating-cube tiles + name + lime `id:`, filter chips, Sort ▾, **LEFT** info panel on right-click, `showing X of N`, Select ▾ 4-option, footer History, centered dimmed History popup. (The earlier same-day grid — right drawer, 2-option Select-all — is superseded and gone.) Verify the §G27.22 checklist in `GROUP_27_SCREENS.md`.
- ~~**`/cb setall` → own Screen**~~ — 🟢 **BUILT 2026-07-12** (owner-approved): `client/gui/SetAllScreen` (GuiMode 18) + `SetAllActionPayload`; bare `/cb setall` opens it, routes to the same tested backup+apply core. Verify TG §S6.
- ~~**Health tab real scan**~~ — ⛔ **CUT 2026-07-12 (owner).** Built that morning, then the whole Extra/Library-Health tab was removed. `BulkHealthView` + `command/handlers/BulkHealth` **deleted**; payload `HEALTH_SCAN`/`HEALTH_FIX` (14/15) and the `BulkSnapshot` health embed stripped. Do not re-add.
- ~~**NL command bar**~~ — 🟢 **BUILT 2026-07-12** (owner-approved: heuristic v1, no AI): always-visible top bar → `client/gui/BulkNlParser`. **Reworked 2026-07-12** for §G27.22b — a parsed phrase now lands on the op **and ticks the blocks it matched** (the old escalation path is gone); Execute still confirms. Verify `GROUP_27_TESTING_GUIDE.md` §T23.
- ~~**`IdReferenceRegistry` / reference-map audit**~~ — ⛔ **ORPHANED 2026-07-12.** It was Health-only (grep-proven), and Health is cut, so `core/IdReferenceRegistry.java` is now **dead code still on disk** — left there pending owner OK to delete (it is a `core/` class → CORE_BLAST_RADIUS). Build is green with it unused. If reference-auditing is ever wanted again, it must get a new front-door.
- **Impact/world-reach preview** (`… P placed`) — ⏸ **deferred**: the `P placed` count needs a world-scan design (loaded-vs-saved chunks, perf) that isn't locked. The rest of the impact line (`N change · M skip · K problem`) already ships.
- ~~**In-screen history panel**~~ — 🟢 **BUILT 2026-07-12**: chrome **History** button → overlay with jump-back-N + bottom-right toast. See below.
- **Branching undo** — roadmap only, not scheduled this pass.

### Deleted (2026-07-10, with the §G07-3 build)

`BulkHubMenu` · `BulkSelectMenu` · `BulkActionMenu` · `BulkConfirmMenu` · `BlockListMenu` · `BulkStyle`,
their five `Nav.Dest` entries and `GuiRouter` cases, the `/cb listgui` and `/cb blockslist` literals, the
`listgui` help topic, and all six `apply*FromGui` helpers. Owner was explicit: **delete the chest path, do
not leave both mechanisms coexisting.** `ListSelection` survives (Group 12's chest Export Dashboard reads
it); `BulkSession` survives but was pruned to the single field that outlived the menus,
`listPickForExport`.

---

## Implementation Requirements

### 1. Filter syntax

Every bulk command takes a filter selecting which blocks to operate on. Resolver: `core/BulkScope.java`
— a pure read over `SlotManager` / `LockManager` / `FavoritesManager`, no mutation.

| Filter | Meaning |
|---|---|
| `all` | All blocks (always confirms, even at ≤ threshold) |
| `category:<name>` | All blocks in that category (case-insensitive exact match) |
| `id:<prefix>` | All blocks whose ID starts with the prefix |
| `name:<substring>` | All blocks whose display name contains the substring |
| `name:<prefix>*` | Display name *starts with* the prefix |
| `favorite:yes` / `favorite:no` | Favorited / not, **for the running player** |
| `locked:yes` / `locked:no` | Locked / unlocked |
| `<id1>,<id2>,…` | Explicit comma-separated id list |
| `<id>` | A single block by exact id |

### 2. Confirmation guard

> 🔒 **`bulkConfirmThreshold` locked 2026-07-11: default changes 10 → 2, mod-wide** (see §G07-4 big-batch
> safety). 🟢 **BUILT Slice 1 2026-07-11** — `CustomBlocksConfig.bulkConfirmThreshold=2`, `ConfigRegistry`
> default synced; every bulk op (chat + Hub) now gates at 2+.

Over `bulkConfirmThreshold` (now 2), the **chat** path prints a clickable confirm line with the block
count and a hover id list; nothing runs until `[✔ Confirm]` is clicked or `/cb confirm` typed. The window is
60s (`BulkConfirm.WINDOW_MS`) — it does not proactively announce expiry, it only replies "expired" if
`/cb confirm` arrives late.

The **Screen** path does not use chat at all: Apply always opens an in-screen modal (§G07-3), regardless of
count. Two doors, each coherent.

### 3. Undo

**Every** op pushes a **single** undo entry for the whole batch — one `/cb undo` reverts everything.
Lock/unlock/favorite/unfavorite were the exception until 2026-07-15 ("each is its own inverse"); that was the
bug, not the design — they recorded nothing, so `/cb undo` reached silently past them. They now record a batch
of `Kind.FLAG` children like everything else.

### 4. Locked blocks

Never **modified** by a bulk op, ever. They're skipped and **reported** — in the result line for the chat
path, and marked `locked` in the live preview *before* Apply for the Screen path.

The five modifying ops skip them: **Edit · Rename · Move · Re-ID · Delete**. The other four do **not**, and
shouldn't: **Duplicate** and **Export** only read the source block, and **Lock/Unlock** and
**Favorite/Unfavorite** are about those flags themselves. `BulkOpSpec.skipsLocked()` is the single source of
truth for this, and `BulkWorkbenchModel.preview()` reads it — so the preview can't promise a skip the handler
won't perform, or hide one it will.

### 5. Bulk recolor — MOVED to Group 10

The `/cb bulkrecolor` Bulk Recolor Hub now lives in `GROUP_10_COLOR_IMAGE.md` §G10-BRH (2026-07-12). The
edge-mode-only safety rule travels with it (full-mode recolor stays blocked to prevent design destruction).

---

## §G07-3 · Bulk Workbench Screen

> 🟢 **BUILT 2026-07-10 — build-green, NOT confirmed in-game.** Replaced the four chest menus **and**
> `BlockListMenu`. Verify against `GROUP_07_TESTING_GUIDE.md` §B/§C + `GROUP_27_TESTING_GUIDE.md` §T before this becomes ✅.

One `Screen` (`GuiMode.BULK_WORKBENCH` = 16), two tabs over one shared selection.

### Browse tab — what `/cb list` opens

Searchable, scrollable list of every assigned block. **Left-click ticks** (multi-select), **right-click**
swaps the right pane to that block's inline detail with an "Open full editor" button. `/cb list` from a
**non-player source** (server console) still prints the old chat list.

> **As built:** the detail pane shows id, category, glow, hardness, sound, shape, collision, locked, favorite
> and lore — everything `ClientSlotCache` actually holds. The design said "texture"; the client has no baked
> image for a block, so that was dropped rather than faked. "Open full editor" opens the Creation Studio in
> edit mode (`CreationStudioBridge.openStudioEdit`).

### Bulk tab — 3-pane workbench

Everything visible at once, no page-flipping:

```
┌─ Bulk Operations ───────────────────────────────┐
│ OPS      │ 🔍 search____  │ EDIT A SETTING     │
│ ▸Edit    │ ☑ stone_a  glow8│ Property: [Glow  ▾]│
│  Rename  │ ☑ stone_b  glow0│ Value:    [ 8 ─●─ ]│
│  Move    │ ☐ brick_c  glow4│                    │
│  Duplic. │ ☑ wood_d   glow0│ PREVIEW            │
│  Export  │ ☐ glass_e 🔒lockd│ stone_a  0 → 8      │
│  Lock    │ ☐ sand_f   glow8│ stone_b  0 → 8      │
│  Favorite│                  │ wood_d   0 → 8      │
│  Re-ID   │ [filter▾] [all] │ 🔒 glass_e skipped   │
│  Delete  │ [sel all page]   │                    │
├──────────┴─────────────────┴────────────────────┤
│ 3 blocks · 1 skipped      [Cancel] [ Apply ]  │
└─────────────────────────────────────────────────┘
```

**Ten ops** — the chest hub's eight, plus **Re-ID** (whose backend `BulkReidCommands.applyReid` was written for
exactly this and left unwired for weeks), plus **Recolor** (net-new 2026-07-12, §G27.22b).

**Execute always opens an in-screen confirm modal** naming the count. Never bounces to chat, never closes the Screen.

**Selection model — ⚠️ SUPERSEDED 2026-07-12 (§G07-5).** The historical model below is gone: the in-tab
`[filter ▾]` builder and the **"apply to all N" escalation** were **removed** with the §G27.22b rebuild.
*Current model:* you pick blocks on **Blocks List** (filter chips · search · Sort ▾ · Select ▾ 4-option) or by
typing a phrase in the **NL bar**; those ticked blocks become one row each on **Bulk Actions**, where a per-row
**include checkbox** is the only scope control. Scope sent = ticked − excluded; the server still skips locked.

<details><summary>Historical (pre-2026-07-12) selection model — no longer built</summary>

`[filter ▾]` narrowed *which blocks the list showed*; the ticked rows were the scope. When the filter matched
more than the player ticked, an **"apply to all N"** button escalated the scope to the filter expression itself.
It was deliberately **withheld while a search was typed** — escalation targeted the filter's matches, not the
list on screen, and one click must never widen a Delete onto blocks the player cannot see.

</details>

### Architecture (as built)

- **Data:** the Screen reads `ClientSlotCache` for id/name/category/glow/hardness/sound/shape/lore. The two
  things that cache lacks — `locked` and `favorite` — ride a small per-player snapshot instead:
  `{"tab":…, "locked":[ids], "fav":[ids]}`, sent in `OpenGuiPayload.data` on open and re-sent after every
  Apply (`BulkSnapshot`).
  - ⚠️ **The design's CORE_BLAST_RADIUS change was NOT needed and was NOT made.** It called for adding
    `locked` to `HudSync.buildIndexJson` plus a new per-player `FavoritesSyncPayload`, touching
    `HudSyncPayload` / `HudSync` / `ClientSlotCache.Entry` / `HudRenderer`. The snapshot (the pattern
    `BuzzerPanelScreen` already uses) gets the same data with **zero core files touched**, and player A's
    favorites can never leak onto player B's screen by construction, because the blob is per-player rather
    than one broadcast for everyone. Owner chose this route 2026-07-10.
  - A blank `tab` in the snapshot means "keep the player on whatever tab they're on" — that's what an Apply
    sends, so a bulk op can't yank the Screen back to Browse. A named tab switches to it, which is how
    `/cb bulk` lands on the workbench even when Browse is already open.
- **Execution:** typed `BulkActionPayload {op, scope, p1, p2, p3}` → `BulkNet` receiver, modelled on
  `BuzzerPanelNet`: validate the op, hop to the server thread, call `BulkApply` (which calls the existing
  handler cores with **structured arguments**), push a fresh snapshot so the open Screen refreshes in place.
  Three params, not the designed two — `replace` needs mode + find + replace-with. `scope` uses a widened
  64 KiB string codec because a hand-ticked selection travels as an explicit id list.
- ⚠️ **Bug found and fixed on the way through** (`BulkApply`): the old `apply*FromGui` helpers glued their
  arguments back into a command line (`filter + " " + mode + " " + a`) and let the handler re-split it on
  whitespace, so `category:my cat`, a category named "red stone", or any rename text with a space was
  silently mis-parsed. Every Screen path now passes arguments as arguments.
- ⚠️ **Blank scope is refused server-side.** `BulkScope.isAll("")` is true, and the Screen path has no chat
  confirm guard (it ran its own modal), so an empty-scope DELETE would have wiped every block. `BulkApply`
  rejects a blank scope outright. (Escalation is gone as of §G07-5, so the Screen now only ever sends an
  explicit ticked-id list — the refusal stays as the server-side guard against a forged packet.)
- **Class split** (current, all ≤500): `BulkWorkbenchScreen` (state/input/routing, 500) · `BulkWorkbenchView`
  (chrome + Blocks List) · `BulkOpsView` (Bulk Actions: op rail · header · rows · foot · modals · sweep) ·
  `BulkResultPanel` (right RESULT-PREVIEW) · `BulkCube` (shared spinning cube) · `BulkWorkbenchModel`
  (preview + Re-ID template) · `BulkOpSpec` (op table + `RAIL_ORDER`) · `BulkOpText` · `BulkAction` ·
  `BulkConsoleInput` · `BulkNlBar`/`BulkNlParser` · `BulkHistoryOverlay` · `BulkDraw` (shared primitives).
  `BulkReidState` was **deleted** (the per-block Re-ID box editor it backed is gone).
- **The preview mirrors each handler's real skip rules**, not a uniform one: only Edit/Rename/Move/Re-ID/
  Delete skip locked blocks, and Re-ID additionally reproduces the handler's `unchanged` / `invalid id` /
  `id taken` skips before Apply.

### Routing changes

| Command / entry point | Before | After |
|---|---|---|
| `/cb list` (player) | chat list | Screen, Browse tab |
| `/cb list` (console) | chat list | chat list *(unchanged)* |
| `/cb listgui`, `/cb blockslist` | chest `BlockListMenu` | **deleted** |
| `/cb bulkgui`, `/cb bulkhub` | chest `BulkHubMenu` | Screen, Bulk tab |
| `/cb bulk` | did not exist | Screen, Bulk tab *(absorbs §G07-1)* |
| every `bulk*` command with **no args** | chest `BulkSelectMenu` builder | Screen, Bulk tab |
| MainMenu chest → "Block List" tile | chest `BlockListMenu` | Screen, Browse tab |
| MainMenu chest → "Bulk Operations" tile | chest `BulkHubMenu` | Screen, Bulk tab |
| G12 Export Dashboard → "Bulk Choose" | chest `BlockListMenu` | Screen, pick mode → `PICK_DONE` |

A `bulk` topic was added to `HelpTopics` (and the dead `listgui` topic removed) so `/cb help` lists it and
G04-2's did-you-mean knows it.

**Verify in-game:** `GROUP_07_TESTING_GUIDE.md` §B–§S · Hub screens → `GROUP_27_TESTING_GUIDE.md` §T.

---

## §G07-1 · `/cb bulk` alias → open the Bulk Workbench

> 🟢 **built, absorbed into §G07-3** (2026-07-10). The Screen rewrite had to register a bulk literal anyway,
> so `bulk` shipped as part of that routing table rather than as a standalone one-line change. `bulkgui` and
> `bulkhub` are kept as aliases — muscle memory, plus existing help topics and links point at them.
> The `bulk` topic was added to `HelpTopics`. Not ✅ until §C2 passes in-game.

**Note (scope decision, still binding):** developer chose **alias only** — the hub's op list was not to be
expanded on the back of the alias. §G07-3 adds Re-ID on its own merits; recolor and background remain
tracked separately (`GROUP_10_COLOR_IMAGE.md` §G10-BRH, G10-1).

**Related:** §G07-3 · G10-1 (bulk background — a future hub op) · G04-2 (discoverable + typo-correctable)

---

## ✅ `/cb setall <setting> <value>` shorthand — confirmed 2026-07-12, build this batch

> **Status:** idea captured 2026-06-20, spec fully locked 2026-07-11, **owner confirmed build 2026-07-12.**

**The idea:** a friendly one-liner — `/cb setall glow 15` — that sets a setting on **every custom block in
the mod** at once.

**Why it's mostly already here:** this is `/cb bulkproperty all <setting> <value>`. The `all` filter already
means *every custom block*, and that path already has the **confirm guard** and the **single-entry batch
undo** the dev wants. So `setall` would be a **thin alias** over the existing tested engine — reuse, don't
fork (CLAUDE.md §5).

**What's decided (for whenever it's revisited):**
- **Build as an alias** of `bulkproperty all …` → inherits confirm + one-shot `/cb undo` for free.
- **Auto-backup before EVERY setall** (dev pick) — snapshot all block data first (route through the
  Group 09 backup rail), so a bad `setall` is always recoverable beyond undo.

**Coverage 🔒 locked 2026-07-11:** `setall` accepts **every** setting `bulkproperty` could ever set — glow,
hardness, sound, collision, **plus category and shape** once Group 08 lands. "Set anything" is the bar, not
just today's 4.

**Backup retention 🔒 locked 2026-07-11:** prune to **last 3** setall backups, oldest auto-deleted.

---

## §G07-4 · Post-test polish & rework backlog (owner SP run 2026-07-10)

> Owner ran §A–§C solo. Backend + routing land; 5 rows fail/rework and the whole Screen wants a polish pass.
> Pass/fail per row: `GROUP_07_TESTING_GUIDE.md`.
>
> **🟢 BUILD STATUS (owner authorized the full rebuild, running slices 1→4 straight through, 2026-07-11):**
> - **Slice 1 (server/config):** threshold 10→2 · duplicate `_copy` bump · MP live-refresh · MP lock re-check
>   (already server-side) · C6 verified. ✅ build-green.
> - **Slice 2 (Dir-2 shell):** rename → **Bulk Operations Hub** · Control Room labels · tabs on LEFT · Dir-2
>   layout (top toolbar · inline `old→new` · big list · dropped preview pane) · X1 opaque modal · X2/B17
>   scissor no-overlap · X4 scrollbars · X5 tickable detail · A1 (icon + un-clip id + chip; **spin = static
>   block icon, true 3D-spin deferred**) · B11 padlock sprite · B1a "Showing: all ▾" · B4 "Move category" ·
>   capitalized labels. Health tab = shell (scan → Slice 4). ✅ build-green, NOT in-game.
> - **Slice 3 (op reworks):** B3 rename · B9/B9a per-block re-ID boxes + Fill-all · A5a/D quoted multi-id + 3×3
>   op-hub grid · AND/OR/NOT filter builder (core/BulkScope boolean engine, mirrored client-side) · impact line
>   `N change · M skip · K problem`. ✅ build-green, NOT in-game. **Screen split** to hold the ≤500 cap:
>   `BulkOpText` · `BulkFilterBuilder` · `BulkAction` · `BulkReidState`. World-reach **P** deferred to Slice 4.
> - **Slice 4 (heavy backend):** IdReferenceRegistry (+ world-reach P) · re-ID re-point + undo remap · in-screen
>   undo/redo + history + toast · X3 dual feedback · Health scan + fix-all · NL command bar · re-ID auto-number
>   patterns. ✅ confirmed 2026-07-12: build this batch (all of Slice 4, no partial pick).
>
> ⚠️ **The four slices above are HISTORY, not the current build.** The §G07-5 rebuild (2026-07-12, §G27.22/b)
> removed several things they shipped — the Dir-2 top toolbar, the filter builder, escalation, the 3×3 op grid,
> the per-block Re-ID boxes (`BulkReidState`), and the whole Health tab (so Slice 4's IdReferenceRegistry front
> door is gone too). Read **§G07-5** for what actually ships today.
> - **A5a MP fix:** ✅ confirmed 2026-07-12: build. Player B must see A's lock as a real lock icon (shared
>   state) and must never see A's personal favorite star — favorites are per-player.

### 💔 Reworks (fail)

- **B3 — Rename `replace` mode** not understood / not behaving as expected. The prefix/suffix/replace UX +
  live preview need a redesign so `find → replace-with` reads clearly and the preview shows real before/after.
- **B9 / B9a — Re-ID flow is wrong (owner confirmed direction, wants harder brainstorm).** Ticking a few blocks
  reveals an **inline text box next to each block's id**; edit **and tick each id individually** before Apply,
  not one shared mode+text. The `id taken` / `unchanged` / `invalid id` marks belong on each inline editor.
  **Locked 2026-07-10:** **hybrid** — one shared field bulk-fills every per-block box (prefix/suffix), then you
  edit individual boxes and tick each; per-row `id taken`/`invalid`/`unchanged` marks beside each box. Owner: build the sketch.
  ⛔ **SUPERSEDED by §G27.22b (2026-07-12).** The per-block box editor was built, then removed. Re-ID is now a
  single **pattern template** (`planet_{n}` → `planet_1`, `planet_2`…); each targeted block gets a normal row whose
  NEW value carries the same `id taken` / `no change` / `invalid id` / `will skip (locked)` marks. Still sent as an
  explicit `REID_MAP` so the server re-checks every pair. `BulkReidState.java` deleted.
- **B3 — Rename `replace`.** Owner: "kinda confusing." Fix sketch (artifact) = clearer labels ("find this
  text…" / "…replace it with"), an **inline example** of the transform, and a **live preview as you type** that
  names rows that won't match. Owner tweaks 2026-07-10: (a) refine the **label wording**, (b) **highlight the
  matched substring** inside each id in the preview, (c) give **prefix/suffix modes the same live-example**
  treatment. Otherwise the direction is accepted.
  - **🔒 Spec locked 2026-07-11 (design only, not built):** clear-label + match-highlight + live-example
    treatment applies to **all 3 rename modes** (prefix/suffix/replace), and gets a visual polish pass — not
    just a functional fix. Owner: "make it cooler looking."
- **B17 — text overlaps boxes/buttons in every screen** (does not pass). Root cause is the shared layout, not
  one op. See cross-screen bug X2.
- **C6 — RESOLVED (2026-07-12).** `bulk` lives inside the "Blocks" help category page (`HelpTopics.java:37`),
  not the top-level overview chest. Confirmed correct, not a bug.

### 🔀 Moved 2026-07-12 — G07-HUB, Blocks List screen-chrome

Owner MP test 2026-07-11 flagged A1 (dead middle space, small icons, wrong rail labels). Redesigned
tab-by-tab via mockup and **fully relocated to `GROUP_27_SCREENS.md` §G27.22** (design locked 2026-07-12) —
it's the screen's chrome/layout, not bulk-ops logic. Per owner (2026-07-12) the **entire Blocks List screen
spec AND its test checklist moved to §G27.22**; `GROUP_07_TESTING_GUIDE.md` §A was removed. The bulk-op
*logic* (10 ops, undo, confirm) stays in this doc.

The **Bulk Actions tab** (second tab) screen-spec is likewise locked in **`GROUP_27_SCREENS.md` §G27.22b**
(design v8, 2026-07-12): op picker rail, per-row include checkbox, right RESULT-PREVIEW panel, right-click
disabled, Execute sweep. Bulk-op behaviour still lives here.
**Both tabs are now BUILT to those specs (2026-07-12, build-green, not in-game)** — see §G07-5 below.

> 🧪 **Tests:** Hub **screens** → `GROUP_27_TESTING_GUIDE.md` §T. Bulk-op **behaviour** → `GROUP_07_TESTING_GUIDE.md` §B.

---

## §G07-5 — Hub rebuilt to §G27.22 / §G27.22b (2026-07-12, 🟢 build-green, NOT in-game)

Owner authorized building every remaining phase straight through, with one batch in-game test at the end.
Full `./gradlew build` (incl. `tgGate`) is green; every file is under its cap.

**Removed outright** (superseded by the locked mockups — do not re-add without a new lock):
- The **Extra / Library-Health** tab (`BulkHealthView`, `command/handlers/BulkHealth`, payload 14/15, snapshot
  embed). `core/IdReferenceRegistry` is left orphaned on disk pending owner OK to delete.
- The **3×3 op-picker landing grid** — the 10 ops are permanently visible on the left rail, so a landing grid is
  redundant. Bare `/cb bulk` now opens Bulk Actions on the default op; a named `bulk*` still jumps to its own op.
- The **AND/OR/NOT filter builder** + **"apply to all N" escalation** (see the superseded selection model above).
- The **per-block Re-ID box editor** (`BulkReidState`, deleted). Re-ID is now one **pattern template** with
  `{n}`/`{nn}` auto-numbering (`planet_{n}` → `planet_1`, `planet_2`…). Locked blocks are skipped and do not
  consume a number. It still ships as an explicit `REID_MAP` (`old=new,…`) so the server re-checks every pair.

**Net-new: Recolor (10th op).**
- Client: left-rail op + hue slider + live result swatch; `canApply` refuses 0°.
- Wire: payload `RECOLOR = 14` → `BulkNet` → `BulkApply.recolor(scope, hue)` → **`BulkRecolorCommands`** (new).
- Server core: gathers non-locked, *textured* targets → hue-shifts each off-thread via
  `ColorMath.hslShift(png, hue, 1.0, 1.0)` → back on the server thread for **ONE** `ResourcePackServer.updatePack()`
  + `syncToAll()` + **ONE** `UndoManager.recordBatch` of `TEXTURE` children. Mirrors `/cb gradient`. One `/cb undo`
  reverts the whole batch (`HistoryCommands` already reverses `TEXTURE` children inside a `BATCH`).
- Skips reported, never silent: locked → `will skip`; no-texture → counted + named in the result line.

**Op ids stay STABLE.** Recolor was **appended** as id `9` (not inserted), and `BulkOpSpec.RAIL_ORDER` drives the
rail's visual order (Edit · Recolor · Rename · Move · Duplicate · Re-ID · Lock · Favorite · Export · Delete). No
existing `OP_*` constant, switch, or array index shifted. `OP_COUNT = 10`.

**Locked-skip is now 6 of 10:** Edit · Rename · Move · Re-ID · Delete · **Recolor**. Duplicate/Export/Lock/Favorite
do not skip (the first two only *read* the source; the flag ops are about flags themselves).

**Execute flow:** Execute → opaque confirm (`<Op> N blocks?` + body, **no** backdrop override) → a red **L→R sweep**
across the rows with an `applying c/N` count-up, ending `✓ <op> applied on N`; the server snapshot then refreshes
the Hub in place (for every player who has it open). One-step Undo/Redo sit in the footer.

### 🔒 Resolved 2026-07-11 (was: needs discussion)

- **A5a / chat `bulklock` UX (owner confirmed all 3 + wants more brainstorm).**
  1. **Quoted multi-id form:** `/cb bulklock "id1" "id2" …`, as many as wanted; drop/replace the unclear `id:`
     filter.
  2. **Bulk-op HUB** on the bare `/cb bulk` (and a bare `/cb bulklock` with nothing) — pick which op to start
     with, instead of always landing on Edit.
  3. **Named command overrides the hub:** `/cb bulklock` jumps straight to the **Lock tab**.
  **Hub shape locked 2026-07-10: Variant A — a 3×3 tile grid** of the 9 ops; click a tile → its tab.
  ⛔ **Direction 2 (the 3×3 grid) is CANCELLED as of §G07-5 (2026-07-12)** — §G27.22b puts all **10** ops
  permanently on the LEFT rail, which makes a separate landing grid redundant. Bare `/cb bulk` now opens Bulk
  Actions on the default op. Directions 1 (quoted multi-id, `BulkScope.java:105`) and 3 (named `bulk*` jumps
  straight to its op, `BulkCommands.java`) are **built** and unaffected.
- **B12 — confirm box.** Owner forgot the exact gripe — **wants a visual mockup from us first** to react to.
  Produce a visualization of the in-screen confirm modal (count + id list + Cancel/Confirm) next.
- **C4 / C4a / C5 — RESOLVED to a doc fix (2026-07-10).** Verified in source: **not bugs.** MainMenu slot 19
  "Block List" → `BulkSnapshot.openFromChest(TAB_BROWSE)`, slot 31 "Bulk Operations" → `TAB_BULK`, Export
  Dashboard slot 16 "Bulk Choose" → `TAB_PICK`. Owner's trouble was only finding them — TG rows reworded with
  the `/cb menu` / `/cb export` path; **retest**.

### 🛠️ Polish (rows pass)

- **A1 — full revamp.** (a) **category name beside the id** (red chip). (b) the "dots" owner flagged =
  the **`…` truncation ellipsis** on clipped ids (e.g. `arabic_a7_iso_…`) — **stop clipping, show the full id**
  (this is the X2 no-clip rule applied to Browse rows). (c) **spinning block preview** per row. (d) **remove the
  long bottom bar** that lists blocks / "apply to all N" to reclaim vertical space — make room for taller preview
  rows **without** compacting everything else. Rework the list + its inner options. A2–A6 pass but polish.
- **B1 — Bulk tab revamp.** **Tabs must always sit on the LEFT** — nothing across the top — for **both** Bulk
  and Browse.
- **B1a** — `all ▾` control with the arrow is unclear to a normal player; clearer label.
- **B4 / B4a** — label "Move" → "Move **category**".
- **B11** — red locked square → a **hand-drawn pixel padlock sprite** (locked 2026-07-10). ⚠️ Cannot be a
  🔒 glyph — MC's font has no supplementary-plane coverage (repo pitfall); draw it with pixel fills in `BulkDraw`.
- **B1a** — filter label → **`Showing: all ▾`** (locked 2026-07-10), so a new player reads it as a filter.
- **B1 — bulk-tab revamp = DEEPER LAYOUT REDESIGN, Direction 2 (locked 2026-07-11).** Options collapse into a
  **top toolbar**; the separate preview pane is removed and each block row shows its **`old → new` inline**; the
  block **list gets most of the screen** (better for big batches). Constraints stay: tabs on the LEFT rail, no
  overlap, opaque modals. Bigger rebuild than Dir 1 (which kept today's 3 panes) — owner chose the roomy one.
- Labels want to look cooler; **capitalize the first word** of every label (7th screenshot).

### 🧠 Edge-case rulings (owner, 2026-07-10)

- **Re-ID — clash between two boxes in one batch:** detect when two per-block boxes are typed the **same new
  id** (not just collisions vs existing ids) → **mark both red, grey out Apply** until fixed.
- **Re-ID — a block already PLACED in the world:** **re-point** the placed blocks to the new id seamlessly
  (they keep working), rather than turning them into "Deleted:" markers.
- **MP — player B has the Bulk screen open when player A applies:** **push a fresh snapshot to every player**
  with the screen open (live-refresh), not only the one who applied. 🟢 **BUILT Slice 1** — `BulkSnapshot.pushAll`
  re-sends a per-player (own favorites), blank-tab snapshot to all online players; the client treats a blank tab
  as refresh-only (`BulkWorkbenchScreen.wantsOpen`) so a closed Hub is never popped open by a bystander's op.
- **Re-ID undo (with the new re-point):** ONE undo **fully reverses** — restores the old id **and re-points the
  placed world blocks back** (symmetric with the re-point-on-apply behaviour). `UndoManager` `Kind.REID` must
  carry the placed-block remap so the batch can walk it back.
- **Duplicate of an existing copy:** detect a trailing `_copy` and **bump the number** → `stone_copy` becomes
  `stone_copy2` (not `stone_copy_copy`). 🟢 **BUILT Slice 1** — `copyBase()` strips a trailing `_copy`/`_copyN`
  (regex anchored, so `red_copyright` is untouched) before `uniqueId()` re-appends and bumps.
- **MP lock race:** the server **re-checks the lock at apply time** (source of truth) and skips a block locked
  after the preview was built, reporting it — the preview does not win. ✅ **Already satisfied** — all 5 modifying
  handlers read `LockManager.isLocked` live in their apply loop; the client preview never gates the skip. No code.

### 🆕 New ask — in-screen Undo / Redo (owner 2026-07-11)

Add **Undo and Redo buttons inside the Bulk Workbench Screen** (not just `/cb undo` in chat). They drive the
same `UndoManager` stack; after firing, push a fresh snapshot so the open screen refreshes in place (and, per
the MP rule above, to every player with the screen open). Redo pairs with undo on the existing stack.

### 🏷️ Naming (owner 2026-07-11)

- **Screen renamed: "Bulk Workbench" → "Bulk Operations Hub"** everywhere (title, help, docs, `GuiMode` comment).
- **Label set = "Control Room" (locked 2026-07-11):**

  | Element | New label |
  |---|---|
  | tab 1 (was Browse) | **Blocks List** (superseded "Catalog" — final name per §G27.22, 2026-07-12) |
  | tab 2 (was Bulk) | **Bulk Actions** (superseded "Console" — final name per §G27.22b, 2026-07-12) |
  | ~~tab 3~~ | ~~Health~~ — **REMOVED 2026-07-12** (Extra/Library-Health tab cut; Hub is 2 tabs only) |
  | rail (was OPS) | **Commands** |
  | preview (was PREVIEW) | **Result** |
  | scope (was "scope: N ticked") | **Targets: N** |
  | apply button (was Apply) | **Execute** |
  | confirm modal verb | **Confirm** — title "&lt;verb&gt; N blocks?", button "Confirm"/"Confirm delete" (renamed from Deploy, owner 2026-07-12) |

- **Two tabs now (2026-07-12):** Blocks List · Bulk Actions (the old 3rd "Health" tab was cut). Plus an
  **always-visible natural-language command bar pinned at the top** of the Hub (sits above the Dir-2 top
  toolbar). NL bar → parsed ops + preview → Execute.

### ✨ Bold features approved (owner 2026-07-11)

- ~~**Library Health scan**~~ — **CUT 2026-07-12 (owner).** The Extra/Library-Health tab is removed entirely;
  the Hub is 2 tabs (Blocks List + Bulk Actions). Do not re-add. (Was: an inspector dashboard scoring the
  library into 4 issue buckets with a one-click fix-all.)
- **Natural-language command bar** — type intent ("make my red blocks glow and rename them crimson") → parsed
  into bulk ops **with a preview** before running. Leans on the existing `StudioAiPanel` / `AiCommands`.
  **🔒 Locked 2026-07-11:** pinned top bar, parses **on Enter** (not live-as-you-type) — shows parsed ops +
  preview, doesn't run until Execute is clicked.
- **Branching undo history (git-style)** — ON THE ROADMAP (not first pass): undo + a new op forks a branch;
  switch between timelines on a visual tree. Supersedes the linear in-screen history panel if built.
- Parked (cool but heavy, revisit): live world ghost-preview, interactive turntable gallery, shareable macro
  replay, Automations/conditional-rules.

### 🚀 Power features & architecture (owner-approved 2026-07-11 — fold into the Dir-2 rebuild)

- **Full reference-map audit — 🔒 scoped 2026-07-11 (owner gave CORE_BLAST_RADIUS permission).** An id is held
  by many subsystems: placed world blocks, favorites, categories, notes (G18), animations (G14),
  showcase/holograms (G19), templates, undo history. Re-ID and Delete walk the registry so **nothing dangles**.
  - **Home:** new standalone class `IdReferenceRegistry` — NOT added to `SlotManager` directly, to keep the
    core-file diff small.
  - **Population:** **push model** — each subsystem (notes, animations, showcase, templates, undo, favorites,
    categories) calls `registry.register(id, source)` whenever it saves a reference to a block id. Always
    current; touches each of those ~7 files once to add the call.
  - **Persistence:** registry **saves its own file** (incremental, updated on each register/unregister) —
    not memory-only, not a rebuild-every-boot scan. New file format, `SlotDataStore`-adjacent I/O; get the
    format + save path right before wiring subsystems in.
  - Still not built — this is the locked design to build against once picked up.
- **Impact + world-reach preview:** before confirm show `N change · M skip (locked) · K invalid · **P placed
  blocks in world affected**`. The world-reach count is the safety signal for Delete / Re-ID.
- **Filter combinators:** multi-condition scope (`category:red AND unlocked`), not just today's single-kind
  filter. **🔒 Locked 2026-07-11: full boolean support from the start — AND + OR + NOT**, not AND-only.
- **In-screen history panel:** multi-step undo/redo list with batch labels ("Re-ID 5", "Delete 12"), jump back
  N in one click. Redo invalidates (with a visible warning) when a new op is applied after an undo.
  **🔒 Locked 2026-07-11:** keeps **every** change made in the Hub this session (unlimited, not capped) —
  cleared on close/relog. Each undo/redo fires a **bottom-right in-screen toast** ("Undid Delete 12" /
  "Redid Re-ID 5"), dismissed by click or auto-fades after a few seconds — no chat line.
- **Re-ID patterns + live collision:** auto-number `planet_{n}`; colour each per-block box in real time against
  existing ids + reserved + the other boxes (superset of the batch-clash rule).
- **Big-batch safety (IN this pass, "make it cool"):** progress bar + **cancel mid-batch** (unwinds what ran) +
  **type-to-confirm** for huge/destructive batches; one debounced pack rebuild at the end.
  - **🔒 Threshold locked 2026-07-11: N = 2, mod-wide.** 🟢 **BUILT Slice 1.** `bulkConfirmThreshold=2` (was 10)
    for **every** bulk op, not just recolor/big-batch — Edit/Rename/Move/Duplicate/Delete/Re-ID/Lock/Favorite all
    gate the confirm at 2+ blocks now.
- **MP griefing / audit:** **deferred to Group 22 (permissions)** — who may bulk-delete/re-id, plus an audit
  log. Not built in G07.

### 🐛 Cross-screen bugs (fix for ALL screens, not just G07)

> **X1 + X2 + tabs-on-left are now mod-wide RULES** (owner-approved 2026-07-10) — added to `CLAUDE.md`
> `<UI_SCREEN_RULES>`: opaque modals, no text overlap/clipping, tabs on the left. Every Screen must comply.
> **B12** was not a real defect — owner just didn't understand the row; it only checks that the confirm box
> opens on *every* Apply (1 or 20 blocks). Owner ruled **2026-07-10: always confirm** on every Apply (1 or 20) — matches current behaviour, no change.

- **X1 — popups render the content behind them** (4th & 8th screenshots). Known bug. A modal must fully occlude
  what's behind it. Fix globally.
- **X2 — labels/headings overlap controls and get clipped** (3rd/6th/7th screenshots — `arabic_a1_iso_…`,
  `Select all on pa…`). New global rule: **no text may run over another element or be truncated**.
- **X3 — lock/conflict feedback** ("No names changed — 1 locked…", 8th screenshot). Owner ruling 2026-07-10:
  **BOTH** — show an **in-screen modal dismissed with an X** (explained in-screen) **and** keep the chat line as
  a log.
- **X4 — no vertical scrollbar/slider** on the side of any list (Browse + all Bulk tabs); can't page through all
  blocks cleanly.
- **X5 — right detail preview** doesn't let you tick / act on its blocks easily.

### 🔀 Cross-group — NOT Group 07

- **Glow placement light-lag:** placing a block with `glow > 0` shows its light **late, after the block is
  placed**. The recolor "squares latency/slowness" was already fixed — mirror that fix. **File under the
  glow/lighting group + its TG**, not here. **FILED 2026-07-10 -> Group 06** ("Tools, Dynamic Glow & Creative
  Tab") as bug **G06-GLOW-LAG**. Research: `SlotLighting.java` already force-updates light for *already-placed*
  blocks; the lag is on the **new-placement** path (`SlotBlock.getPlacementState`), which likely skips the same
  client light re-check. Mirror the already-fixed recolor "squares latency" fix. Repro pending under G06.

---

## Follow-ups now resolved

The 2026-06-21 in-game session left three "scope TBD" reworks open. §G07-3 closes all three:

- **bulkproperty complete rework** → the Bulk tab's Edit-a-setting pane, with a live `old → new` preview.
- **Confirm guard rework** → in-screen confirm modal (Screen path); the chat guard is unchanged.
- **Bulk GUI revamp** → the whole chest→Screen conversion.

---

## Cleanup (after testing)

```
/cb bulkdelete category:bulktest
/cb delete copy_g07a
/cb delete copy_g07b
/cb delete copy_g07c
```
