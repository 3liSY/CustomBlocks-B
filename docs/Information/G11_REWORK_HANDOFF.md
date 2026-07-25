# G11 Category Rework — Build Handoff

> **CONSUMED 2026-07-25 — historical.** This build ran; §A is `Built 🎯` and the jar is green.
> Do not build from this file again. Current truth is
> [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md) and
> [Testing_Guide_11.md](../testing/Testing_Guide_11.md); what actually landed, and the four places
> the build departed from the plan below, are recorded in [PROGRESS_LOG.md](PROGRESS_LOG.md).
>
> Known inaccuracies in the plan below, kept as written: `/cb setcategory` lives in
> `AttributeCommands.java`, not `BulkCategoryCommands`/`SetAllCommands`; the package is
> `com.customblocks`, not `com.stormy.customblocks`.

Source of truth for every decision: [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md) §B
(Locked Decisions + Feature Plan §B). Test plan: [Testing_Guide_11.md](../testing/Testing_Guide_11.md)
§A, rows A1–A11. This doc is a build-order handoff, not a spec — if it disagrees with the group
doc, the group doc wins.

Scope reminder: commands only. Chest menus (`CategoryEditMenu`, `CategoryListMenu`,
`CategoryBrowserMenu`) stay on the current single-category view untouched — do not touch them.
G27 owns the future Screen surface.

## Current state (verified 2026-07-25)

- `SlotData.category` is one plain lowercase `String` (`""` = uncategorized). `SlotManager.categories()`
  scans every block for it; there is no category record independent of blocks.
- `CategoryMetadataStore` (`core/CategoryMetadataStore.java`) already stores per-category
  displayBlock/colorTag/colorHex/description/sortOrder, keyed by lowercase string, with an
  `exists` flag so an explicitly-created empty category persists. This is the closest existing
  thing to a "category record" — extend it, don't replace it.
- `CategoryService` (`core/CategoryService.java`) has the current rename/merge/delete/color/desc/
  icon/sort/lockAll/info logic, all single-category.
- `CategoryCommands` (`command/handlers/CategoryCommands.java`) registers `/cb category <action>`.
  `/cb setcategory` and bulk `/cb bulkcategory` live in `BulkCategoryCommands.java` /
  `SetAllCommands.java` — both call `SlotManager.setCategory(id, cat)`, which **replaces**.
- `BulkConfirm` (`command/handlers/BulkConfirm.java`) is the existing confirm-hold mechanism —
  reuse it for the block-destroying delete modes, do not invent a new one.
- `UndoManager` (`core/UndoManager.java`) has `Kind.MODIFY`/`Kind.DELETE`/`Kind.BATCH` and
  `recordBatch(actor, List<Op>, label)` — reuse this for delete-mode undo; do not add a new Kind
  unless a mutation genuinely doesn't fit MODIFY/DELETE.
- Startup hook: `CustomBlocksMod.onInitialize()` calls `SlotManager.loadAll()` at line 101 — the
  first-load conversion must run right after that line, once `SlotManager` is populated.

## Build order

### Step 1 — `CategoryMembershipStore` (new file, `core/CategoryMembershipStore.java`)

The new multi-membership layer. Model it on `CategoryMetadataStore`'s persistence style (Gson,
atomic write to `config/customblocks/data/`, static `Map` + `load()`/`save()`).

- Storage: `Map<String blockId, LinkedHashSet<String categoryKey>>`. `LinkedHashSet` preserves
  join order — needed later if any ordering ever matters, and it's free.
- File: `config/customblocks/data/category_membership.json`.
- `Uncategorized` is a **built-in key** that always exists (bootstrap it in `CategoryMetadataStore`
  too — see Step 2) and can never be deleted.
- API to build:
  - `Set<String> of(String blockId)` — a block with no entry returns `{"uncategorized"}`, never
    an empty set.
  - `void add(String blockId, String catKey)` — adds `catKey`. If the block's current set is
    exactly `{"uncategorized"}` and `catKey` isn't `"uncategorized"`, drop `"uncategorized"`
    first (this is an implementation detail, not a separately-locked decision — the point is a
    block is never sitting in a real category *and* `Uncategorized` at the same time).
  - `void remove(String blockId, String catKey)` — removes `catKey`. If the resulting set is
    empty, add `"uncategorized"` back. Refuse (return false / no-op) if `catKey` is
    `"uncategorized"` itself — it can't be removed as a "last category" because it always *is*
    the last resort (TG11 A2).
  - `void renameKey(String oldKey, String newKey)` — move every block's `oldKey` membership to
    `newKey` (used by `/cb category rename`).
  - `void mergeInto(String fromKey, String toKey)` — for every block with `fromKey`: add
    `toKey`, remove `fromKey`. Because it's a `Set`, a block already in both just dedups for
    free (TG11 A6) — no special-case needed.
  - `List<String> blocksIn(String catKey)` — every block id with `catKey` in its set (any
    membership, not just "main" — there is no main). Used by `give`, `info`, `filter`.
  - `List<String> exclusiveBlocksIn(String catKey)` — blocks where `of(id)` is exactly
    `{catKey}` (size 1). Used by the exclusive-blocks delete mode.
  - `void deleteKeyEverywhere(String catKey)` — for every block with `catKey`, `remove(id,
    catKey)` (which itself handles the fall-to-Uncategorized case). This is `/cb category
    delete`'s "category only" mode.
- First-load conversion — a public `static void runFirstLoadConversionIfNeeded()`:
  - No-op if the store's JSON file already exists (means it's already been through this once).
  - Otherwise: iterate `SlotManager.assignedSlots()`; for each block whose legacy
    `d.category()` is non-empty, `add(d.customId(), norm(d.category()))`; also call
    `CategoryMetadataStore.create(cat)` so the category record exists even if metadata was
    never set. If nothing was found, still write an empty (or Uncategorized-only) file so the
    "already ran" check above works on the next boot.
  - Call this from `CustomBlocksMod.onInitialize()` right after `SlotManager.loadAll()` (after
    line 101 in the current file). No backup, no confirm, no report — that gating was dropped
    because the owner confirmed no category data is currently in use (G11 Locked Decisions,
    2026-07-25 "no migration phase" entry). Log one line either way (`LOGGER.info`) so a future
    debug session can see whether it ran and what it found.

### Step 2 — `CategoryMetadataStore` additions (existing file)

- Add a `displayName` field to `Meta` (the typed-as-entered name; the map key stays the
  lowercase normalized key, unchanged).
- Bootstrap `Uncategorized` at class init: if `DATA` has no `"uncategorized"` entry after
  `load()`, create one with `exists = true` and `displayName = "Uncategorized"`.
- `create(String typedName)`: normalize to a key, check `DATA.containsKey(key)` — if present
  AND the existing entry's `displayName` differs from the new typed name (case/spelling), this
  is the **key-collision** case (G11 Locked Decisions, 2026-07-25): reject and return/throw
  something `CategoryService` can turn into an error message naming the existing display name.
  If the key doesn't exist yet, create it with the typed `displayName` stored as-is (no more
  forced lowercase display — G11 Locked Decisions, "stores the display name as typed").
- Guard every delete/rename path against `key(cat).equals("uncategorized")` — that record can
  never be deleted or renamed away.

### Step 3 — `CategoryService` rewrite (existing file)

Rework method-by-method against TG11 §A rows. Keep the `Outcome` pattern.

- **`rename`**: on top of existing `CategoryMetadataStore.renameCategory`, also call
  `CategoryMembershipStore.renameKey(from, to)`. Keep the existing "old and new the same" /
  "empty" guards. Do **not** add key-freezing — the owner explicitly declined that; rename
  still changes identity (already documented as a known caveat for exporters).
- **`merge`**: replace the `SlotManager.setCategory` loop with
  `CategoryMembershipStore.mergeInto(from, to)`, then `CategoryMetadataStore.deleteCategory(from)`.
  No dedup handling needed in this method — the store does it (TG11 A6).
- **`delete`** — now three modes, not one. Suggest a `String mode` parameter
  (`"category"` / `"exclusive"` / `"move"`) or three separate public methods
  (`deleteCategoryOnly`, `deleteExclusiveBlocks`, `deleteMoveFirst`) — whichever keeps
  `CategoryCommands` cleanest; check how other 3-choice commands in this codebase expose
  sub-verbs before picking (e.g. how `TrashCommands` or `DeletionService` structure multi-mode
  actions) rather than guessing a new shape.
  - **category only**: `CategoryMembershipStore.deleteKeyEverywhere(cat)` +
    `CategoryMetadataStore.deleteCategory(cat)`. No confirm needed (mirrors today's `delete`,
    TG11 A3) — nothing destructive happens to a block.
  - **exclusive blocks**: `CategoryMembershipStore.exclusiveBlocksIn(cat)`, then delete each via
    the existing block-deletion path (`DeletionService.deleteCore` — check its signature) behind
    `BulkConfirm.request(...)`, batched into one `UndoManager.recordBatch` (TG11 A4). This is the
    one path that can destroy blocks — it must go through confirm and be undoable, no exceptions.
  - **move first**: takes a target category, runs `CategoryMembershipStore.mergeInto` semantics
    for the blocks in `cat` (add target, remove `cat`) before removing the category record
    (TG11 A5). No block destruction, so no confirm needed unless you judge otherwise once you
    see the wired-up command shape.
- **`give`**: change `SlotManager.byCategory(cat)` to `CategoryMembershipStore.blocksIn(cat)`
  (any membership counts, TG11 A10 — there's no main to prefer).
- **`info`**: add a `list` variant. Default: existing count/locked/texture/sort/icon/description
  lines, but block count comes from `CategoryMembershipStore.blocksIn(cat).size()`. With the
  list flag: also append the block ids (TG11 A11).
- **new `create(String typedName)`**: thin wrapper over `CategoryMetadataStore.create`,
  surfacing the key-collision rejection as an `Outcome` error.
- **new `filter` methods**: one for the category listing (modes: alphabetical, newest-to-oldest,
  oldest-to-newest, most-blocks-first, grouped-by-colour-tag, hide-empty — TG11 A8's category
  half) and one for blocks inside a category (alphabetical, newest-to-oldest, oldest-to-newest
  only — TG11 A8's block half). "Newest/oldest" needs a per-block or per-category creation
  timestamp; check whether `SlotData` or its creation path already has one before adding a new
  field — don't duplicate `AnimData`-style bookkeeping if a timestamp already exists somewhere
  reachable. Default with no mode argument is alphabetical for both (TG11 §A, filter-default
  decision).

### Step 4 — command wiring (`CategoryCommands.java`, `BulkCategoryCommands.java`, `SetAllCommands.java`)

- **`/cb setcategory <blockId> <category>...`**: change from `SlotManager.setCategory` (replace)
  to a loop of `CategoryMembershipStore.add(blockId, norm(cat))` over every listed category
  (TG11 A1). No main-category concept — do not add one.
- **new `/cb category set <blockId> <category>`** and **`/cb category remove <blockId>
  <category>`**: thin wrappers calling `CategoryMembershipStore.add`/`.remove` for one
  membership each.
- **`/cb category create <name>`**: wire to `CategoryService.create`.
- **`/cb category delete <name>`**: needs a mode argument or three sub-literals per the shape
  chosen in Step 3.
- **`/cb category filter <name> <mode>`**: new sub-command, category-listing modes.
- **`/cb category info <name> [list]`**: extend the existing `info` literal with an optional
  trailing `list` argument.
- **tab-complete**: the existing `suggestCategories` helper in `CategoryCommands.java` already
  suggests from `SlotManager.categories()` — repoint it at
  `CategoryMetadataStore.knownCategories()` (or a merged view) so a category with 0 blocks (or
  one that only exists via `CategoryMembershipStore`) still tab-completes (TG11 A9). Apply the
  same suggester to every new argument that takes a category name.
- **`BulkCategoryCommands.applyCategory`**: decide whether bulk `/cb bulkcategory` should also
  become additive (consistent with `/cb setcategory`) or stay a replace — this wasn't asked
  about explicitly and the two commands doing different things by default would be confusing.
  Flag it back to the owner rather than guessing; it's a fast yes/no, not a redesign.

### Step 5 — wire the first-load conversion

Add the `CategoryMembershipStore.runFirstLoadConversionIfNeeded()` call in
`CustomBlocksMod.onInitialize()` immediately after `SlotManager.loadAll()` (current line 101).

### Step 6 — compile, then hand back for in-game testing

- `JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'` (system default `java` is 1.8;
  do not build with it), then `.\gradlew.bat compileJava --no-daemon -q`.
- Do **not** touch `docs/` beyond what's already locked in `GROUP_11_CATEGORY.md` and
  `Testing_Guide_11.md` unless something built turns out to contradict a locked decision — if
  that happens, stop and flag it rather than silently reconciling docs to match the code.
- Once it compiles, this is `Built 🎯` — do not mark `Done ✅` or fill in any SP/MP result cell
  in TG11 §A. Only the owner's in-game confirmation does that (per
  `docs/testing/extra/Glossary.md` §2 and §9.3, and repo-wide rule in `CLAUDE.md`).
- After code changes: run `python docs/testing/extra/testing_tools.py health` and
  `python docs/groups/group_tools.py check docs/groups/GROUP_11_CATEGORY.md` — both must pass
  before handing back. Update TG11's Status row (`Built 🎯`, `Blocked` line removed) once real
  code exists, but leave every A1–A11 result cell as `🎯` (target), not a date, until tested.

## Explicitly out of scope for this build

- Chest menus (`CategoryEditMenu`/`CategoryListMenu`/`CategoryBrowserMenu`) — untouched, per
  G11's "commands only until G27 Screens exist" decision.
- Category export/import carrying multi-membership data — parked `Discussion ✏️` in TG12 §A /
  TG20 §K, not G11's to design or build.
- Nesting, hidden/locked/permission gates, sounds/particles/accent/badge/auto-add, templates,
  main category — all scrapped in the G11 Locked Decisions / Superseded tables. Do not
  reintroduce any of them "while you're in there."
