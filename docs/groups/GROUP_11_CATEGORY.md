# Group 11 - Categories

> Group 11 gives creators a clear way to organize, browse, customize, export, and later share collections of CustomBlocks.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_11.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Categories should work as a durable collection model, not just a single label. Players need a reliable baseline for assigning, giving, renaming, merging, describing, and decorating categories today, plus a planned path to multiple memberships, a main visible category, trees, and rich exportable metadata.

This Group owns category records, assignments, commands, migration rules, and category-specific behavior. It does not own shared Screen layout, export/download transport, Vault connectivity, or a made-up bulk retexture command.

## Ownership

| Owns | Does not own |
| --- | --- |
| Category data, assignment, commands, tree rules, customization, and migration | Shared Screen framework and Create Studio host surface: G27 |
| Current category actions and future multi-category model | ZIP/download transport: G12 |
| Category export/import metadata | Vault sharing and remote import transport: G20 |
| Create-time category hint and category display/icon behavior | A separate `bulkretexture` feature: G10 with G07 if approved |
| Category delete semantics | Generic block selection and bulk operations: G07 |

## Direction

The existing unified `/cb category <action>` commands and `/cb setcategory` remain the reliable baseline. Player browsing moves to `CategoryHubScreen`, and the wider Category workspace inside `/cb create` becomes the future host for creation and advanced editing. A cramped side panel is not an acceptable substitute.

The target model permits multiple category memberships with exactly one main category for the visible badge/icon. Existing assignments must migrate without loss. Category customization, hierarchy, and export metadata remain category data even when another group provides the Screen or transport.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-14 | Scattered category verbs are replaced by `/cb category <action>`. | Rename, merge, delete, color, description, icon, sort, lock, give, export, share, import, info, list, and edit use one command family. |
| 2026-06-14 | Auto-categorize is a create-time hint only. | The standalone `/cb autocategorize` command does not return. |
| 2026-06-30 | Blocks may have multiple categories and one main category. | Browsing/filtering can use all memberships while the main category supplies the default badge/icon. |
| 2026-06-30 | Categories may be empty and form an unlimited tree. | The UI warns beyond two nesting levels but data does not impose an artificial cap. |
| 2026-06-30 | Existing categories and assignments migrate without loss. | The new model cannot discard a category, a block assignment, or a style record. |
| 2026-06-30 | Category creation exists in commands and GUI. | The Create workspace is a real creation path, not a read-only browser. |
| 2026-07-09 | Category browsing moves from old chest menus to `CategoryHubScreen`. | Browser/detail/edit routing must not keep competing player UI paths. |
| 2026-07-10 | The wider Create Studio Category workspace replaces the rejected cramped sample. | Category editing uses adequate canvas space before the full migration proceeds. |
| 2026-07-12 | Category export includes category style, tree, assignment, and relevant block metadata. | A category share/import is more than a list of names. |

## Feature Plan

### A. Current Category Baseline

**Player outcome**

Players can organize blocks, work with a category, and maintain its basic details from one coherent command vocabulary.

**Experience**

- `/cb setcategory <id> <category>` assigns a block.
- `/cb category give`, `rename`, `merge`, `desc`, `color`, `sort`, `lock`, `unlock`, `delete`, `info`, `list`, and `edit` provide the supported category actions.
- `/cb categories` is the player category entry point; console callers receive text rather than a Screen attempt.
- A display block, color tag, description, and ordering make categories recognizable.
- Category give reports inventory overflow honestly.

**Requirements**

- Category records and assignments persist under the established data-path contract.
- Rename and merge update affected assignments consistently.
- Display-block selection uses the category editing flow rather than removed standalone set/clear commands.
- Create-time hints can suggest a category but never silently force one on the player.
- The Category Bulk Retexture tile is removed or disabled until it points to a real supported flow.

**Boundary**

This baseline stays usable while the broader category model is built. It does not claim that a missing UI action exists.

### B. CategoryHub and Create Workspace

**Player outcome**

Players can browse categories and blocks in a proper Screen, then create or edit categories from a spacious workspace inside `/cb create`.

**Experience**

- `CategoryHubScreen` lists categories with counts, icons, browse/edit actions, and category detail.
- A block row supports category-appropriate give, edit, and remove actions.
- The Create Category workspace uses the main canvas rather than a narrow panel.
- A player can select/create a category, set a main category, and edit visible style fields without hiding existing records.

**Requirements**

- Browser routes retire the old chest-menu target without breaking console fallback.
- G27 Screen code receives category data and invokes G11 mutation paths rather than reimplementing them.
- The workspace maintains a stable difference between current baseline records and the new multi-category model during migration.
- Screen copy and shortcuts make browse, edit, delete, and modifier actions discoverable without clutter.

**Boundary**

G27 owns the reusable screen/Studio presentation. G11 owns what a category means and how its edits are applied.

### C. Multi-Category Model and Safety

**Player outcome**

Creators can build useful category trees and shared collections without losing an existing block or deleting more than they intended.

**Experience**

- A block can belong to multiple categories while showing one main category by default.
- Parent views include child-category blocks.
- Categories can carry name/key, parent, icon, accent, badge, description, style, order, hidden/locked state, permissions, sounds, particles, and auto-add rules.
- Templates and reordering help creators repeat a category style.
- Delete presents separate choices: remove category only, remove only exclusively-owned blocks, remove all shown tree blocks, or move blocks first.

**Requirements**

- Category migration creates real records for current assignments before expanding the model.
- Delete confirms its chosen mode and never treats shared blocks as exclusive by mistake.
- Export/import carries tree, memberships, main category, and permitted customization data.
- Extra memberships remain browse/filter data unless a later explicit setting expands visible badges.

**Boundary**

This is a category data-model evolution, not a blanket change to every block-management surface.

### D. Export, Share, and Import

**Player outcome**

Creators can export a complete category locally, then later share or import it through a safe remote route.

**Experience**

- `/cb category export <category>` produces a ZIP with category metadata, assignments, and required assets.
- A player flow offers the same export result as the command route.
- `/cb category share` and `import` become available only when the G20 Vault path is ready.
- Import handles existing IDs through a deliberate conflict-resolution flow.

**Requirements**

- Local ZIP creation is separate from download-link presentation and remote upload.
- Links do not leak host/IP information or pretend to be reachable when they are not.
- Remote import validates the incoming category payload before merging it into local records.

**Boundary**

G11 defines category contents. G12 owns export/download experience and G20 owns remote share/import transport.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G07 | Bulk category action | G07 supplies selection/confirmation; G11 applies consistent category assignment rules. |
| G10 | Bulk Retexture decision | No category UI advertises a missing retexture command; any future supported flow uses G10 image logic. |
| G12 | Local export | G11 builds category contents; G12 owns safe download/share presentation. |
| G20 | Vault share/import | G20 transports validated category artifacts; G11 owns their schema and local merge rules. |
| G27 | Hub and Create workspace | G27 supplies Screen/Studio presentation; G11 supplies category data and mutations. |
| G28 | History | Category edits expose clear reversible actions where the history contract supports them. |

## Technical Contract

- G11 is the source of category record, membership, main-category, parent-tree, style, and assignment semantics.
- Current command registration uses `/cb category <action>` plus `/cb setcategory`; removed scattered verbs and `/cb autocategorize` are not reintroduced.
- Category UI routes call G11 services/commands and preserve a text-only console fallback.
- Migration must create durable records from existing category assignments before enabling richer memberships and parent trees.
- A delete operation states its exact scope and distinguishes uncategorizing, exclusive deletion, tree deletion, and move-then-delete.
- Category export serializes validated category metadata with required block references/assets; remote upload/download remains outside the local schema layer.
- Category storage follows the shared `config/customblocks/data/` path convention.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Vault share/import | Requires the G20 remote service and a conflict-resolution route. | G20 with G11 |
| Complete multi-category migration | Needs owner review of the wide workspace before replacing current category routing. | G11 with G27 |
| Category templates and advanced style effects | Depend on the stable new category record model. | G11 |
| Bulk Retexture tile | No real command or shared backend exists yet. | G10 with G07 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-14 | Individual category commands had separate top-level names. | One `/cb category <action>` command family is used. |
| 2026-06-14 | `/cb autocategorize` was a standalone command. | Categorization suggestion is a create-time hint only. |
| 2026-06-30 | A cramped narrow Category tab was the proposed Create experience. | The wider workspace uses the main Studio canvas. |
| 2026-07-09 | Old chest category menus were the intended browser. | `CategoryHubScreen` is the intended player browser. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_11.md) · [All Groups](README.md)

- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 11 snapshot](../archive/group-migration-2026-07-18/GROUP_11_CATEGORY.md)
