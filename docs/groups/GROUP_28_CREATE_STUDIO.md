# Group 28 - Undo and History Rework

> Group 28 makes every undo, redo, and history entry honestly describe what it will change, including large bulk operations and face-texture edits.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_28.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Undo must be safe enough for a player to trust before they click it. The existing command description is more accurate than the old GUI, which duplicates stale formatting logic and makes a bulk operation look like an unknown block or a single tiny change. G28 removes that split-brain behavior.

G28 owns undo/redo records, descriptions, persistent history, bulk child semantics, and the missing per-face texture history. G27 owns the eventual Undo History Screen presentation, but it consumes G28's single accurate data model.

## Ownership

| Owns | Does not own |
| --- | --- |
| Undo/redo stack records, batch children, shared descriptions, and persistent storage | Screen layout, chrome, and interaction widgets: G27 |
| Whole-batch and one-child bulk revert semantics | Bulk operation selection/mutation rules: G07 |
| Face paint/clear before-after history capture | Face texture authoring behavior: G06/G08 |
| Audit facts and history query data | Permission decisions for rollback/history access: G22 |

## Direction

One `UndoDescribe` helper produces the same friendly action, block summary, block count, and child detail for commands, undo, redo, and audit history. A bulk step is visibly a batch with its affected blocks, never `?` or a fake block ID such as `x5`.

History survives restart as a capped per-player rolling buffer. A player can revert a bulk step as one intentional action, or deliberately remove exactly one child from the batch. Arbitrary partial selection is not allowed because it would make redo state unreliable.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-23 | Bulk history needs drill-in to every affected block. | A count is summary information, not a substitute for inspectable children. |
| 2026-06-25 | Command, undo, redo, and audit use one description helper. | Action labels/counts cannot drift through copied GUI formatting. |
| 2026-06-25 | A bulk step supports Undo whole step or reverting one chosen child only. | Whole-batch safety is preserved and limited child removal cannot corrupt redo. |
| 2026-06-25 | History persists per player with atomic JSON and a configurable FIFO cap of 200 steps by default. | Restart/reload does not erase usable history or create unbounded data. |
| 2026-06-25 | Large reverts use a confirmation threshold. | Risky batch actions are explicit and cancel leaves state unchanged. |
| 2026-07-12 | The replacement UI is one G27 Undo History Screen with Undo, Redo, and Audit tabs. | Old UndoMenu/HistoryMenu retire only after this Screen is accepted. |
| 2026-07-16 | Paintface, clearface, and future per-face texture changes must record normal undo/redo entries. | Per-face work cannot become a permanent change outside the common history system. |

## Feature Plan

### A. One Honest Description Model

**Player outcome**

Undo, redo, command output, and audit history describe the same action in the same plain language.

**Experience**

- A single glow/hardness/sound/collision/category action uses a friendly action name everywhere.
- A bulk row names the action, affected-block count, and useful representative detail instead of `?` or `xN` as a block.
- Hover/detail can list the first affected blocks and make the full child list available when needed.
- Redo mirrors undo accurately rather than maintaining another presentation path.

**Requirements**

- Introduce `UndoDescribe` as the only description/label/count source for commands, undo, redo, and audit views.
- Replace copied `friendlyAction`, block-ID, and batch-count maps in old callers.
- Model a batch through its children, never through null before/after placeholders or a synthetic block ID.
- Keep action labels aligned with the actual stored operation names, not outdated command spelling.

**Boundary**

Description is presentation data derived from real history. It does not reinterpret or alter the mutation the underlying operation recorded.

### B. Bulk Inspection and Safe Revert

**Player outcome**

Before undoing a large change, a player can see exactly which blocks it will affect and choose a safe supported outcome.

**Experience**

- Selecting a batch opens its child list with block identity and relevant before/after context.
- Undo whole step reverses all children as the normal recommended action.
- A player may choose one child to revert/remove; the batch changes from N children to N-1 and remaining child history remains coherent.
- A large revert shows a clear confirmation; Cancel changes nothing.

**Requirements**

- Batch inverse/redo operations remain ordered and atomic for whole-step paths.
- Child-only revert updates the batch/redo state through one deliberate code path; arbitrary multi-child selection is forbidden.
- Missing, locked, or invalid child state produces a useful outcome without corrupting the remaining stack.
- Command routes and future G27 Screen routes use the same G28 operation APIs.

**Boundary**

G28 does not redefine G07 bulk scope or bypass locks/permissions. It records and safely reverses the operations those Groups approve.

### C. Persistent History and Face Texture Coverage

**Player outcome**

Undo history remains available after restart and includes all intended texture-changing actions, including per-face edits.

**Experience**

- Each player sees their own recent undo/redo history after reconnect/reload.
- When the configured cap is reached, only oldest history is trimmed; the newest work remains.
- Painting or clearing one face can be undone/redone through the normal `/cb undo`/`redo` flow.

**Requirements**

- `UndoStore` writes compact per-player JSON files atomically and recovers safely from a bad/corrupt file.
- Push/load/disconnect/reload behavior maintains the configured rolling `undoMaxSteps` buffer.
- `FaceCommands` capture complete before/after per-face texture state for paintface, clearface, and future face transforms before calling the normal `UndoManager` path.
- Config includes `undoMaxSteps` and `undoConfirmThreshold` with validated defaults.

**Boundary**

Persistence stores history, not a second block database. G06/G08 still own face command semantics and G09 remains responsible for server backup/recovery.

### D. Undo History Screen Handoff

**Player outcome**

One understandable Screen replaces separate fragile undo and history chest menus.

**Experience**

- `/cb undogui`, `/cb redogui`, and `/cb history` lead to Undo, Redo, or Audit tabs of the same G27 Screen after cutover.
- Rows show real action, count, block detail, and drill-in; Audit can filter relevant facts without changing undo stacks.
- Confirmations, search/filter, preview, keyboard behavior, and error feedback follow the G27 standard.

**Requirements**

- G27 consumes G28 description and operation APIs rather than producing client-side action summaries.
- Audit rollback uses clearly authorized G28 services and records safe history as required.
- Old menus remain reachable only until the replacement has passed the owner acceptance route.

**Boundary**

G27 provides the Screen. It cannot declare or repair history semantics on its own, and G28 does not create another parallel UI.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G06 | Face tools | Every face-texture mutation supplies before/after state to normal undo history. |
| G07 | Bulk operations | G07 submits batch children; G28 describes and reverses them without changing bulk selection rules. |
| G08 | Shapes/faces | Per-face data follows the same safe undo contract as other texture state. |
| G09 | Backup safety | Persistent undo is short rolling history, not a replacement for durable backups. |
| G17 | Shared history consumers | Macro, Studio, ReID, and other mutation flows use this corrected common undo/redo foundation. |
| G22 | Permissions | Rollback/history Screen actions respect final authorization policy. |
| G27 | Undo History Screen | G27 renders the three tabs; G28 supplies authoritative rows, children, actions, and confirmations. |

## Technical Contract

- `UndoDescribe` is the sole source for action labels, block summaries, counts, and child details across command, undo, redo, and audit surfaces.
- Batch operations store/recover children as the authoritative affected records; UI never infers a fake block from null batch before/after fields.
- Whole-batch revert and single-child removal are distinct supported operations with redo-safe stack transitions; arbitrary partial batches are rejected.
- `UndoStore` persists atomic compact JSON per player, trims FIFO at `undoMaxSteps`, isolates players, and handles corrupt files without server failure.
- Face paint/clear history stores before/after texture state and uses the normal `UndoManager` record path.
- G27 Screen code requests server-authoritative descriptions/details/actions and does not mutate history directly.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Arbitrary multi-child partial bulk undo | It would make redo and batch consistency unsafe. | G28 only after a new stack design |
| Long-term audit archive beyond rolling undo data | It needs separate storage/privacy/retention policy. | Future audit work |
| Automatic cross-server undo synchronization | Undo remains local server state and is not a cloud transport feature. | G20/G28 future work |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-23 | Old GUI copies could independently format actions and batches. | One shared helper defines every history description. |
| 2026-06-25 | A bulk entry could only show a weak count or fake block identity. | It exposes true children and supports a safe drill-in. |
| 2026-06-25 | Undo history was memory-only. | It is a capped atomic per-player rolling store. |
| 2026-07-12 | Undo and History remain separate chest menus. | G27 provides one three-tab Screen after cutover. |
| 2026-07-16 | Per-face texture changes could bypass undo entirely. | Every face paint/clear operation records normal history. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_28.md) · [All Groups](README.md)

- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G08 Shapes and Faces](GROUP_08_SHAPES.md)
- [G09 Backup and Recovery](GROUP_09_BACKUP_SAFETY.md)
- [G17 History, Give, Delete, and Search](GROUP_17_REGRESSIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 28 snapshot](../archive/group-migration-2026-07-18/GROUP_28_CREATE_STUDIO.md)
