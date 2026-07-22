# Group 07 - Bulk Operations

> Group 07 lets a player make one deliberate change across many custom blocks, with an honest preview, a clear confirmation, and one reversible history step.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_07.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Bulk work must be safe enough for destructive changes and quick enough for ordinary maintenance. Players select concrete blocks, see what will change or be skipped, confirm once, and can undo the whole batch in one standard history action.

This Group owns bulk selection, command routing, batch behavior, confirmation, and history boundaries. It does not own the shared Screen framework, backup retention, bulk recolor rules, block-shape behavior, or shared chat wording.

## Ownership

| Owns | Does not own |
| --- | --- |
| Bulk command behavior, structured scope, skip rules, and confirmation | Screen framework and shared chrome: G27 |
| Bulk Workbench selection and operation flow | Bulk recolor engine and safety policy: G10 |
| One batch history boundary for every reversible operation | Backup storage and retention: G09 |
| `/cb bulk` and no-argument bulk command routes | Shape mutation behavior: G08 |
| `/cb setall` routing through bulk apply behavior | Shared message wording: G04 |

## Direction

The Bulk Workbench is the single player-facing route for browsing, selecting, reviewing, and applying bulk changes. It works from concrete selected IDs, not a second filter language that can drift from server behavior. Chat commands keep compact filter syntax for command use, while the Screen shows the actual per-operation impact before it applies.

Each reversible operation creates one history entry. Locked blocks are protected only where an operation modifies them; read-only or flag operations follow their own explicit rules rather than pretending all actions skip locks.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-10 | Chest bulk menus and the chest block list are replaced by the Bulk Workbench Screen. | There is one maintained player UI instead of parallel chest and Screen paths. |
| 2026-07-11 | The chat confirmation threshold defaults to two selected blocks. | Larger chat batches require a deliberate confirmation. |
| 2026-07-11 | Every reversible bulk operation creates one batch history entry. | Undo and redo treat the full change as one operation, including lock and favorite changes. |
| 2026-07-12 | The Workbench uses explicit selected blocks and per-row include controls. | The removed filter builder, escalation rule, and old 3-by-3 operation picker cannot return. |
| 2026-07-12 | Screen confirmation is always in-screen. | Execute opens a clear modal and never bounces the player into chat. |
| 2026-07-12 | Bulk recolor moves to G10. | G07 may route the action but does not duplicate recolor logic or safety rules. |
| 2026-07-12 | Health tab and reference-audit flow are cut. | No hidden scan feature remains in the Workbench. |
| 2026-07-12 | Bare `/cb setall` opens its own Screen and uses the established backup/apply route. | The shorthand remains safe without creating a parallel mutation engine. |
| 2026-07-18 | The Workbench selects concrete ticked rows client-side and sends an explicit id list. | The old `category:`/`id:`/`name:` filter strings and the AND/OR/NOT filter builder are removed; the server no longer resolves them, so the client must stop emitting them. |
| 2026-07-20 | The natural-language ("ask" / NL) command bar is removed from the Bulk Hub entirely; block targeting (tick + filters, incl. a `Category ▾` filter) is a **Screen** concern owned by [G27 §D/§E](../groups/GROUP_27_SCREENS.md). | G07 owns bulk backend, commands, and services only; the mis-targeting NL parser is gone and is not G07's to rebuild. |
| 2026-07-18 | The bulk-shape command is scrapped, not deferred. | Changing shape across many blocks is dropped from G07 entirely; it does not wait on G08. |
| 2026-07-18 | Every Bulk Workbench and Set All screen — build, layout, and listings — is owned by G27. | G07 owns bulk behavior only; the screens themselves live in G27. |

## Feature Plan

### A. Command Bulk Operations

**Player outcome**

Players can target many blocks from chat with a concise selection expression and receive an honest result before or after the one intended change.

**Experience**

- Chat accepts `all`, category, ID, name, favorite, locked, exact-ID, and explicit-ID-list selection forms.
- A batch above the configured threshold opens a 60-second confirm route before applying.
- A no-match request says so clearly rather than silently acting on nothing.
- Reversible actions create one undo step for the whole batch.
- Console use keeps its non-Screen command behavior.

**Requirements**

- `BulkScope` is the server-side resolver for command selection.
- `BulkConfirm`, `BulkChat`, and `BulkSuggestions` share the same selection count and command result.
- The supported operations are property edit, delete, rename, category move, duplicate, export, lock/unlock, favorite/unfavorite, and re-ID.
- `bulkproperty` remains the property route; a dedicated `bulksound` literal is not required while sound is available there.
- A blank Screen payload is rejected server-side even when a forged request reaches the receiver.

**Boundary**

Chat filter syntax belongs to command parsing. It is not a hidden alternative selection model for the Screen.

### B. Bulk Workbench

**Player outcome**

The player can browse blocks, select the exact rows they intend, choose a bulk action, and understand the result without jumping between chest menus or chat prompts.

**Experience**

- `/cb list` opens the Browse tab for a player; `/cb bulk` opens Bulk Actions.
- Browse supports search, sorting, selection, an inline information panel, and an edit handoff to the Creation Studio.
- Browse participates in G27's shared Filters menu, including the combinable `Animated` filter supplied by G14 data.
- Bulk Actions use selected rows and per-row include checkboxes for the actual scope.
- Each action presents its own before/after or skip information, then a Confirm modal and Result modal.
- Screen history and normal Undo/Redo controls remain available without inverse-command shortcuts.
- Block targeting on the Bulk Hub Screen (tick rows + filters, incl. `Category ▾`) is owned by G27; the old natural-language command bar was removed 2026-07-20. G07 never emits obsolete `category:`/`id:`/`name:` strings to a server path that no longer understands them.

**Requirements**

- `BulkWorkbenchScreen` and its view/model classes read block fields from `ClientSlotCache`.
- `ClientSlotCache` includes the saved animated flag needed by the shared browser filter; G07 does not guess from filenames or textures.
- Per-player locked and favorite state travels in the `BulkSnapshot`, so shared locks are visible without exposing another player's favorites.
- `BulkActionPayload` passes structured arguments to `BulkApply`; user text must never be rebuilt into a space-split command string.
- `BulkOpSpec.skipsLocked()` is the single source for preview and handler lock behavior.
- The Screen refreshes its snapshot after applying an action and preserves the active tab.

**Boundary**

G07 owns the workbench behavior. G27 owns its shared layout pattern, visual system, and cross-screen usability rules.

### C. Safe Batch Rules

**Player outcome**

Protected blocks are not accidentally modified, while valid read-only and flag actions still work as expected.

**Experience**

- Edit, rename, move, re-ID, and delete skip locked blocks and identify the skip.
- Duplicate and export may read locked source blocks.
- Lock/unlock and favorite/unfavorite operate on their own flag state rather than being blocked by it.
- Re-ID preview marks invalid, unchanged, locked, or already-taken results before Confirm.

**Requirements**

- The preview follows the same operation contract as the handler.
- Lock and favorite history preserves the owner identity required for correct undo.
- Bulk deletion calls the G06 deletion rail rather than restoring a separate delete implementation.
- Re-ID keeps structured per-target validation on the server.

**Boundary**

G07 controls batch scope and orchestration; it does not replace G06 deletion, G09 backup, or G28 history internals.

### D. Set All

**Player outcome**

`/cb setall` is a clear whole-library action with a dedicated Screen, a preview, a backup route, and normal batch undo.

**Experience**

- Bare `/cb setall` opens its own Screen.
- Supported settings follow the existing bulk-property capability as the system grows.
- The action previews and confirms before changing every selected block.
- Viewer refresh and undo follow the same contract as the Bulk Workbench.

**Requirements**

- The Screen routes to the shared backup and apply core.
- Set All backups follow the G09 retention contract.
- Category and shape coverage expands only when their underlying mutation contracts are ready.

**Boundary**

Set All is a friendly front door to the bulk engine, not a separate all-block mutation path.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Commands and result wording | G07 supplies structured outcomes; G04 owns shared human-facing wording and affordances. |
| G06 | Bulk deletion | Bulk delete routes through the single Recycle-Bin deletion rail. |
| G08 | Bulk shape | Scrapped — G07 does no bulk shape work. |
| G09 | Set All backup | Set All uses the established backup route and its retention policy. |
| G10 | Bulk recolor | Recolor safety and image changes remain in G10, even when launched from bulk UI. |
| G14 | Animated browser data | G14 supplies the saved animated identity used by the shared `Animated` filter. |
| G27 | Bulk Workbench screens | G27 provides the common Screen pattern and screen checks; G07 provides operation behavior. |
| G28 | Batch history | Each reversible bulk action exposes one operation boundary for undo and redo. |

## Technical Contract

- `BulkScope` resolves chat selection without mutating block state.
- `BulkApply` receives structured payload fields, validates the operation on the server thread, rejects blank Screen scope, and refreshes the current `BulkSnapshot` afterward.
- Chat confirmation and Screen confirmation are separate coherent routes: threshold-gated chat versus always-modal Screen.
- `BulkOpSpec` defines the operation order and lock skip behavior used by both preview and apply paths.
- A bulk action records one history batch. Flag history carries `(id, flag, state, ownerUuid)` rather than assuming flags live on `SlotData`.
- The per-player snapshot carries locked and favorite state needed by the Workbench without broadcasting personal favorites.
- Legacy chest menu commands remain aliases only where needed for existing muscle memory and route into the Screen path.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| World-reach count for affected placed blocks | Needs a bounded loaded-versus-saved world scan design. | G07 |
| Branching history | A roadmap concept that needs a separate history model. | G28 with G07 |
| Dedicated `bulksound` literal | Sound is already supported through `bulkproperty`. | G07 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-10 | Several chest menus and a chest block list formed the bulk flow. | One Bulk Workbench Screen provides Browse and Bulk Actions. |
| 2026-07-12 | The Screen used a filter builder, AND/OR/NOT language, escalation, and a 3-by-3 operation picker. | The player selects concrete rows and can include or exclude each row. |
| 2026-07-12 | A Health tab owned scan and fix behavior. | The tab and its front door are removed. |
| 2026-07-12 | Re-ID used per-block inline text editors. | It uses the current structured pattern and row validation route. |
| 2026-07-12 | Bulk recolor was a G07 feature. | Its engine and safety policy are owned by G10. |
| 2026-07-13 | The Console/NL bar sent `category:`/`id:`/`name:` filter strings and an AND/OR/NOT filter builder to the server. | Removed from the server; the client now ticks concrete rows and sends an explicit id list (client cleanup locked 2026-07-18). |
| 2026-07-12 | Bulk shape was deferred, waiting on the G08 shape mutation contract. | Scrapped outright — no longer part of G07. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_07.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G08 Shapes](GROUP_08_SHAPES.md)
- [G09 Backup and Safety](GROUP_09_BACKUP_SAFETY.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 07 snapshot](../archive/group-migration-2026-07-18/GROUP_07_BULK_OPERATIONS.md)
