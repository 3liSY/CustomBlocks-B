# Group 02 - Retired Chest GUI Core

> Group 02 preserves the retirement boundary for the old chest GUI system so discontinued menus do not quietly return.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_02_Scrapped.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

The old chest-GUI plan is retired. Group 02 records the boundary between that earlier system and its screen-based replacements, so future work does not rebuild outdated menus or delete compatibility code too early.

It is a retirement and cutover document, not a place to create new chest GUI features.

## Ownership

| Owns | Does not own |
| --- | --- |
| The retirement boundary for the old Main Menu, Block Editor, Block List, Undo/Redo browser, and History UI | Their screen-based replacements: G27 |
| The compatibility rule for old chest GUI base code during cutover | Admin resource-pack commands and permissions: G22 |

## Direction

New rich interfaces belong to their owning screen Groups. The old chest GUI base remains only while a live replacement still depends on it; it is not a reason to restore retired menus or create a parallel chest workflow.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-12 | Chest GUI feature work is retired. | Do not rebuild the old dashboard, editor, block list, undo/redo browser, or history UI. |
| 2026-07-12 | Screen replacements belong to G27. | New screen design and cutover work stays out of G02. |
| 2026-07-12 | Admin resource-pack commands belong to G22. | `/cb rp pause`, `/cb rp resume`, `/cb sync`, and `/cb unsuppress` are not G02 work. |

## Feature Plan

### A. Replacement Boundary

**Player outcome**

Players are sent to the current screen workflow rather than an abandoned chest menu.

**Experience**

- The Main Menu, Block Editor, Block List, Undo/Redo browser, and History Log do not receive new chest-GUI work.
- Their replacements are designed and delivered through G27.

**Requirements**

- Do not add a second route that reopens a retired chest menu after a replacement exists.
- Treat an old menu only as a feature checklist during a G27 replacement, never as code to extend.

**Boundary**

Group 02 does not own the replacement screen layout, controls, or player-facing behavior.

### B. Compatibility and Deletion

**Player outcome**

Cutover does not break an existing route while replacements are still being completed.

**Experience**

- No player-facing workflow is removed before its working screen replacement is ready.

**Requirements**

- Old base code such as `ChestMenu.java` remains until every interface that depends on it has a complete screen replacement.
- Remove dead chest code only as part of the replacement cutover, not as unrelated cleanup.

**Boundary**

Keeping temporary base code does not reopen the retired chest-GUI scope.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G27 | Screen replacement | G27 owns all replacement screens and decides when a retired menu can cut over. |
| G22 | Admin resource-pack commands | G22 owns permissions and the remaining admin command behavior. |

## Technical Contract

- `ChestMenu.java` and related base code cannot be deleted until no live replacement depends on them.
- Retired menus are not extended, restyled, or revived as a shortcut around unfinished screen work.
- A cutover removes old command routing only after its replacement is complete and its owner approves the transition.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

*(none)*

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-12 | Restore the old chest GUI system. | Use the owning screen replacement in G27 or G22. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_02_Scrapped.md) · [All Groups](README.md)

- [G27 Screens](GROUP_27_SCREENS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
