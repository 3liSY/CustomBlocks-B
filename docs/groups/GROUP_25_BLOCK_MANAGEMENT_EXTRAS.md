# Group 25 - Block Management and Identity Operations

> Group 25 makes advanced block management safe and direct: rename or swap identity, target the block in front of or held by a player, and manage block-specific extras without losing attached data.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_25.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Block IDs, names, attachments, and world placements are connected data. Management actions need to preserve that connection, refuse unsafe collisions, and participate in the same undo history as normal editing. Players also need a faster way to point at or hold a block instead of constantly retyping its ID.

G25 owns those identity operations and focused management extras. Its Screen entries route through G27; its history behavior consumes G17; its special Arabic target handling stays safely separated from normal `SlotData` operations.

## Ownership

| Owns | Does not own |
| --- | --- |
| ReID, swap ID/name, duplicate alias, identity-state migration, and universal block references | Shared undo/redo implementation: G17 |
| Typed `#` looked-at and `!` held block resolution | Arabic block data/rendering: G13 |
| Custom drops, finder data/actions, tab icon configuration, and management-only exports | Block editor and ReID Screen presentation: G27 |
| Favourites, recent blocks, edit-history requirements, and magic-item handoff | The original command/asset behavior each referenced feature owns |

## Direction

Identity operations are deliberate, atomic changes. ReID changes an ID without losing the block's name, asset, lock/favourite/lore state, or placed references. Swaps preserve the block data that belongs to each original block. All mutable operations have one understandable history entry.

Existing-block commands gain one safe target language: `#` for the custom block a player is looking at and `!` for the custom block in hand. The resolver understands ordinary slots and Arabic auto-join letters as different types, so an unsupported Arabic action explains itself rather than falling into an unsafe slot-data path.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | ReID, swap ID, swap name, and duplicate are atomic, undoable identity operations. | Identity changes preserve or exchange only the data their operation promises. |
| 2026-06-21 | `/cb duplicate` is the full alias for `/cb dupe`. | Both automatic copy IDs and a chosen new ID follow one duplicate behavior. |
| 2026-06-21 | Every existing-block command can accept `#` or `!`; create cannot. | Block targeting is consistent without pretending a new block already exists. |
| 2026-06-21 | `#` uses ten-block vision through fluids/entities and can resolve an item-frame block; `!` prefers main hand then offhand. | Target shortcuts match how players actually aim and hold blocks. |
| 2026-06-21 | Arabic targets are typed and opt-in per action. | Arabic letters may be deleted/recoloured through safe existing paths; unsupported operations refuse cleanly. |
| 2026-06-27 | ReID resolves the stored ID once before mutation. | Typed case differences cannot make a valid stored ID fail halfway through ReID. |
| 2026-07-09 | ReID and management UI are Screens. | G27 provides real text inputs and editor surfaces, not anvil/chest workarounds. |

## Feature Plan

### A. Identity Operations

**Player outcome**

An operator can change or exchange an identity without accidentally losing the block behind it.

**Experience**

- `/cb reid <old-id> <new-id>` changes only the ID and reports a clear taken/missing/locked reason when it cannot proceed.
- `/cb swapid` exchanges IDs while names, textures, attributes, and attached state remain with their original block data.
- `/cb swapname` exchanges display names only.
- `/cb duplicate <id> [new-id]` follows `/cb dupe`, producing a collision-free copy name when one is not supplied.

**Requirements**

- Resolve case-insensitive lookup to the exact stored ID before lock checks, mutation, and messages.
- Migrate slot, texture, placements, favourite, lock, lore/note, and comparable attached state during ReID.
- Reject collisions before mutation and create one G17-compatible undo/redo record for each atomic operation.
- ReID Screen routes invoke the same server operation as commands and return safely to the originating editor.

**Boundary**

Identity operations do not invent new block content or change visual editor layout. G27 owns the Screen and each feature owner remains responsible for validating its own stored fields.

### B. Universal Block References

**Player outcome**

A player can use `#` for a nearby looked-at CustomBlock or `!` for a held CustomBlock in every command that acts on an existing block.

**Experience**

- Suggestions include normal IDs and `#`, making the shortcut discoverable.
- `#` reaches ten blocks, passes through fluids/entities, and can read a CustomBlock contained by an item frame.
- `!` uses main hand first and automatically falls back to offhand.
- Console, spectator, execute-as, empty-hand, and no-target situations produce one useful target error.
- Arabic letters are recognized as a target rather than misidentified as a missing block.

**Requirements**

- `BlockRefArgumentType` accepts a normal ID, `#`, or `!` where a command expects an existing block.
- `BlockTarget.resolve` returns typed `SLOT`, `ARABIC`, or friendly error results; commands opt into Arabic behavior explicitly.
- Slot commands receive unchanged `SlotData` after resolution; Arabic delete uses the normal world break/join-flow path and Arabic recolour reuses `ShapeToolItem.recolorArabicLetter`.
- Unsupported Arabic actions stop before reaching `SlotData` code and say the letter can only be deleted or recoloured.

**Boundary**

`/cb create` stays excluded. `#` and `!` are not valid ordinary IDs, and G25 does not broaden Arabic operations beyond their explicitly safe paths.

### C. Drops and Management Surfaces

**Player outcome**

An operator can configure a custom drop, find placed blocks, export a block texture from its editor, and set a persistent creative-tab icon.

**Experience**

- `/cb setdrop <id> <item-id> [amount]` and `cleardrop` set or remove the intentional break drop.
- Finder results show loaded placements with location, dimension, and distance; an authorized operator can refresh, page, and teleport.
- An editor export produces the same private download route as `/cb exportpng <id>`.
- `/cb settabicon <url>` updates a saved icon that returns after restart.

**Requirements**

- Persist custom drops under `config/customblocks/data/drop_config.json`; no configured custom drop means no custom drop.
- Drop changes validate item IDs/amounts, are undoable, and use the same data through command and G27 editor routes.
- Scan loaded chunks asynchronously, with explicit result count/pagination and admin-gated teleport.
- Save exported PNGs under `cloud_exports` with sender-only download links; store the tab image at `textures/tab_icon.png` after safe image retrieval/validation.

**Boundary**

G25 provides management behavior and data. G27 owns editor/finder/ReID Screen layout, while G10 remains the primary PNG export feature owner.

### D. Consolidated Management Requirements

**Player outcome**

Related management actions eventually live in one clearly owned area rather than disappearing between old Groups.

**Experience**

- Favourites have explicit favourite, toggle alias, unfavourite, and list behavior rather than only an ambiguous toggle.
- Recent blocks expose useful recently used content without silently becoming a second dashboard system.
- Block edit history and magic items have their scope written before a large implementation begins.

**Requirements**

- Define full interaction and test contracts before building favourites, recent, mutation log, or magic-item revamp work.
- Reuse G17 history where an action is undoable; do not create competing edit records.
- Route any future Screen surface through G27 and respect G22 permission policy.

**Boundary**

This is ownership consolidation, not permission to assume the features exist or have passed testing. Their detailed build work stays deferred until an owner walkthrough sets the contract.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G06 | Tools and delete | Universal targeting complements normal tools; delete routes retain their existing safety behavior. |
| G10 | PNG export | Editor export calls the same protected export/download behavior as G10. |
| G13 | Arabic letters | Typed targets never treat Arabic letter entities as `SlotData`; only approved delete/recolour paths run. |
| G17 | Undo/redo and search | Atomic identity changes and macro/history consumers share the normal history model. |
| G22 | Permissions | Finder teleport and management actions apply the approved command tiers. |
| G27 | Screen framework | ReID, editor, and finder interfaces call G25 services rather than duplicating data logic. |

## Technical Contract

- ReID resolves the exact stored ID once, then applies lock checks, state migration, mutation, and messages against that exact key.
- `BlockRefArgumentType` and `BlockTarget.resolve` are the single parsing/resolution path for existing-block command targets.
- `BlockTarget` separates normal `SlotData` from Arabic world-block targets and returns user-facing errors centrally.
- `#` supports a ten-block raycast through fluids/entities and item-frame content; `!` resolves main hand before offhand.
- Identity operations record one shared undo/redo entry and must migrate referenced attached state atomically.
- Custom drops, finder data, export artifacts, and tab icon files use stable persisted locations; client Screen code never mutates them directly.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Favourites, recent blocks, edit history, and magic-item revamp | They need full interaction/test contracts, not just an ownership move. | G25 with G17/G27 |
| Additional Arabic-target actions | Each must be independently proved safe for Arabic letter data. | G25 with G13 |
| Finder support for unloaded chunks | Current finder contract is loaded-chunk scanning; persistent world indexing is separate work. | G25 future slice |
| ReID/editor/finder visual polish | The behavior is here, but visual interaction belongs to the unified Screen system. | G27 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Only `/cb delete #` accepted a looked-at block. | Every existing-block command uses the central typed target resolver. |
| 2026-06-21 | Arabic letters could flow through ordinary slot-ID mutation paths. | Arabic targets are explicitly typed and only approved actions may run. |
| 2026-06-27 | ReID could mutate using the casing typed by the player. | ReID uses the resolved stored ID, fixing case-mismatch failure. |
| 2026-07-09 | ReID input could use an anvil or chest interaction. | G27 provides a proper Screen with real text input. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_25.md) · [All Groups](README.md)

- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G10 Colour and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G17 History, Give, Delete, and Search](GROUP_17_REGRESSIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 25 snapshot](../archive/group-migration-2026-07-18/GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md)
