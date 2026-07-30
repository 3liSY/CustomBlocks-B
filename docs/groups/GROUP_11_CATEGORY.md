# Group 11 - Categories

> Group 11 gives creators a clear way to organize, browse, customize, export, and later share collections of CustomBlocks.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_11_Done.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Categories should work as a durable collection model, not just a single label. Players need a reliable baseline for assigning, giving, renaming, combining, and decorating categories today, plus a planned path to multiple, equal memberships and rich exportable metadata. Categories are flat: there is no parent/child nesting and no membership is a stored "main".

This Group owns category records, assignments, commands, migration rules, and category-specific behavior. It does not own shared Screen layout, export/download transport, or Vault connectivity. Bulk retexture is not a category feature at all and no category surface offers it.

## Ownership

| Owns | Does not own |
| --- | --- |
| Category data, assignment, commands, customization, and first-load conversion | Shared Screen framework and Create Studio host surface: G27 |
| Current category actions and future multi-category model | ZIP/download transport: G12 |
| Category export/import metadata | Vault sharing and remote import transport: G20 |
| Create-time category hint and category display/icon behavior | Any bulk retexture flow: scrapped 2026-07-25, no owner |
| Category delete semantics | Generic block selection and bulk operations: G07 |
| Category data and mutation services consumed by a Screen | CategoryHubScreen, the Create-workspace Category tab, and the Export Dashboard Screen: G27 |

## Direction

The existing unified `/cb category <action>` commands and `/cb setcategory` remain the reliable baseline. Player browsing, category creation, and advanced editing happen through G27 Screens (`CategoryHubScreen` and the Create-workspace Category tab); G11 supplies the data and mutation paths those Screens call.

The target model permits multiple, equal category memberships; there is no main category, no nesting, and no stored badge priority between memberships. Existing assignments must migrate without loss. Category customization and export metadata remain category data even when G27 provides the Screen or G12/G20 provide transport.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-14 | Scattered category verbs are replaced by `/cb category <action>`. | Rename, merge, delete, color, icon, sort, lock, give, export, share, import, info, list, and edit use one command family. (`description` was in this list until `2026-07-28`, when it was removed outright.) |
| 2026-06-14 | Auto-categorize is a create-time hint only. | The standalone `/cb autocategorize` command does not return. |
| 2026-07-25 | Blocks may have multiple categories; none is a stored "main". | Browsing/filtering can use any or all memberships; no membership carries a badge priority over another. |
| 2026-07-25 | A category delete may destroy blocks, but only behind `/cb confirm` and as one undoable batch. | The block-destroying delete modes are in scope; a bare `delete` keeps today's uncategorize-only behavior. |
| 2026-07-25 | A category stores the display name as typed plus a lowercase key used for matching. | `Arabic Letters` displays as typed while `arabic letters` still resolves it; names stop being forced lowercase. |
| 2026-07-25 | Hidden, locked, and permission-gated categories are scrapped. | Every category stays visible and editable for everyone; no visibility or permission field enters the category record. |
| 2026-07-25 | Per-category sounds, particles, accent colour, badges, and auto-add rules are scrapped. | The category record keeps only display name, key, icon, colour tag, and order; nothing joins a category unasked. (Description left the record `2026-07-28`; the parent field never existed — categories are flat.) |
| 2026-06-30 | Categories may be empty. | A category exists as its own record and does not vanish when its last block leaves. |
| 2026-07-25 | Categories are flat; there is no parent/child nesting. | No category has a parent field; every category is top-level and none can contain another. |
| 2026-07-25 | Delete offers exactly three modes: category only, exclusively-owned blocks, or move blocks first. | "Exclusively-owned" means a block with no other membership, decided from multi-membership data, not from a tree. |
| 2026-07-25 | A category is created explicitly by command and implicitly by assigning a block to an unknown name. | `/cb category create` makes an empty category; today's assign-and-it-appears habit keeps working. |
| 2026-07-25 | `/cb setcategory <blockId> <category>...` takes several categories at once; `/cb category set` and `/cb category remove` act on one membership. | Multi-membership is typed in one command, and a single add or removal has its own explicit verb. |
| 2026-07-25 | Every category argument tab-completes to existing category names. | Names are picked from a list rather than retyped, so a typo cannot silently create a category. |
| 2026-07-25 | Today's `sort` becomes `filter`, applying to the category listing and to a category's blocks with a separate mode set for each. | Category modes are alphabetical, newest-to-oldest, oldest-to-newest, most blocks first, grouped by colour tag, and hide-empty; count, colour, and emptiness cannot apply to blocks. |
| 2026-07-25 | `/cb setcategory` adds memberships instead of replacing them. | Assigning a block somewhere new never silently removes it from where it already is; clearing a membership needs `/cb category remove`. |
| 2026-07-25 | Every block always belongs to at least one category; the built-in `Uncategorized` category is the default and cannot be deleted or removed as a block's last membership. | A block can never end up with zero categories. `/cb category remove` on a block's last real category leaves it in `Uncategorized` instead of erroring. |
| 2026-07-25 | Two typed names that normalize to the same key is a rejected creation, not a silent merge. | `/cb category create` (and implicit creation) errors and names the existing category holding that key; the caller must pick a genuinely different name. |
| 2026-07-25 | The default `filter` order, with no mode given, is alphabetical for both categories and blocks inside a category. | Matches today's `/cb category list` behavior; nothing changes for existing habits until a mode is explicitly requested. |
| 2026-07-25 | `/cb category info <name>` shows the block count by default and a full block listing when asked. | `/cb category info <name>` gives count/icon/colour; `/cb category info <name> list` (or an equivalent explicit argument) adds the block names. |
| 2026-07-25 | Merging a category into another that a block already belongs to just drops the duplicate membership. | Merge is a set union; no error or special-cased report for blocks that were already in both. |
| 2026-07-25 | Category commands (create, rename, merge, delete, and the rest) stay open to any player, with no new permission check. | Categories remain a creative-organization tool, not an admin-gated one, matching today's behavior. |
| 2026-07-25 | `/cb category delete <name>` removes only that one membership from every affected block; other memberships are untouched. | A block with no other membership left falls to `Uncategorized`, consistent with the 3-mode delete and the remove-to-Uncategorized rule. |
| 2026-07-25 | Category templates are scrapped. | No command copies one category's setup onto another; each category is configured directly. |
| 2026-07-25 | There is no migration phase; a first-load conversion runs only if a legacy category word is found and is a no-op otherwise. | Owner confirmed no category data is in use, so the backup/confirm/report gating is dropped while stray assignments are still converted rather than lost. |
| 2026-06-30 | Category creation exists in commands and GUI. | The Create workspace is a real creation path, not a read-only browser. |
| 2026-07-12 | Category export includes category style, tree, assignment, and relevant block metadata. | A category share/import is more than a list of names. |
| 2026-07-25 | Every category Screen (`CategoryHubScreen`, the Create-workspace Category tab, and the Export Dashboard) moves to G27. | G11 keeps category data, mutation, and export contents; G27 owns their Screen presentation. See [G27 §N](GROUP_27_SCREENS.md). |
| 2026-07-25 | Bulk retexture is removed from the category editor and is not deferred to any group. | `CategoryEditMenu` no longer shows the tile and no `bulkretexture` route is planned. |
| 2026-07-25 | The rework is exposed through commands only until the G27 category Screens exist. | Existing chest category menus keep the current single-category view; no new model work is spent on them. |
| 2026-07-25 | The legacy one-word `SlotData.category` field stays, as a derived display shadow of the membership set. | Every membership change restamps it with the alphabetically-first real membership by typed name, or `""` when the block sits in `Uncategorized` alone. It is computed, never chosen, and no read path treats it as truth, so it is not a stored main. It keeps the not-yet-reworked surfaces (HUD, Arabic chest menus, exports, blueprint lore, Bulk Workbench, Category Hub) readable and is deleted when G27 moves them onto the membership store. |
| 2026-07-25 | `/cb bulkcategory` becomes additive, matching `/cb setcategory`. | The same word means "add" in both; `none` still clears every membership, so re-filing in bulk is an explicit clear-then-add. |
| 2026-07-26 | Delete has ONE form, `/cb category delete <name>`, and asks in chat. | Supersedes the three-mode delete. Running it posts a clickable prompt with two choices: keep the blocks (drop the category only) or delete the blocks that live nowhere else. No `exclusive` / `category` / `move` mode words exist. |
| 2026-07-26 | A block-destroying delete records TWO undo entries, not one. | One entry restores the category record and its memberships; a second, separate entry restores the wiped blocks. `/cb undo` can take back the blocks without being forced to also take back the category. |
| 2026-07-26 | The delete "move blocks first" mode is replaced by `combine`. | Emptying a category into another is `/cb category combine <a> into <b>`, which already deletes `<a>`. |
| 2026-07-26 | `merge` is renamed `combine <a> into <b>`, with the `into` connector required. | The old `merge a b` gave no clue which category survived; the connector makes the direction unmissable. |
| 2026-07-26 | Category names are never quoted. | Quotes are stripped on read and are never printed back in chat. `/cb category give Arabic Numbers` works as typed. Fixes the bug where the literal `"` became part of the lookup key. |
| 2026-07-26 | A mode word always comes BEFORE the category name, never after. | The name is the last, greedy argument on every verb, so an unquoted multi-word name can never be confused with a mode: `/cb category filter newest Arabic Numbers`, `/cb category info list Arabic Numbers`. |
| 2026-07-26 | The sort/filter word `alpha` is renamed `alphabetically`. | Applies to `/cb category sort`, the category mode set, and the in-category block mode set. |
| 2026-07-26 | `/cb category delete` is undoable in its safe form too. | Dropping a category without touching blocks still records an undo entry that restores the record and every membership it held. |
| 2026-07-26 | The delete prompt states both counts in its sentence and hover-lists the doomed ids. | One `[CB]` line naming how many blocks are inside and how many are exclusive, then the two buttons; the destructive button's hover names every block that would die. |
| 2026-07-26 | Delete asks only when something is actually at risk. | With no exclusive blocks, nothing can be destroyed, so the category is dropped immediately and reported — no prompt. |
| 2026-07-26 | Clicking the destructive button IS the confirmation; `/cb confirm` is not involved. | The count and the hover list are shown before the click, and the two undo entries cover a mistake. Supersedes the 2026-07-25 "only behind `/cb confirm`" rule for this path. |
| 2026-07-26 | The two undo entries are pushed blocks-last so the first `/cb undo` restores blocks. | Labels read `Deleted N blocks (<name>)` and `Deleted category <name>`; the blocks entry is newest because it is the one worth panicking about. |
| 2026-07-26 | A category name printed in chat renders in that category's own colour tag. | Falls back to the standard value colour when the category has no tag. Applies to every `[CB]` line that names a category. |
| 2026-07-26 | A category name in chat is clickable and hoverable. | Click opens the Category Hub focused on that category; hover shows count, icon, and colour. (The description line left the hover card `2026-07-28`.) |
| 2026-07-26 | Category success messages carry a follow-up button for the obvious next step. | e.g. `[Undo]` after a delete, `[View Category]` after a set. |
| 2026-07-26 | Tab-complete shows a block count that is NOT inserted when accepted. | The suggestion text is the bare name, so accepting types only `Arabic Numbers`; the count rides as the suggestion's tooltip so an empty category is still spottable in the list. |
| 2026-07-26 | `Uncategorized` stays visible everywhere but is styled as a system floor. | Dim/italic in listings, tab-complete, and the Hub so it never reads as a category the owner made. |
| 2026-07-26 | The category manager is the existing `CategoryHubScreen`, extended — not a second screen — and it reaches full parity with the commands plus bulk selection and a search box. | Create, rename, delete (same two-choice prompt as a dialog), combine, colour, icon, sort, give, export/share, membership drag-drop, multi-select acting on many blocks at once, and type-to-filter over the block grid. (Description was in this parity list until `2026-07-28`, when the feature was removed everywhere.) Commands become the fallback path, not the main one. **The screen work itself belongs to G27 and is tested there ([TG27 §U](../testing/Testing_Guide_27.md)); G11 keeps only the data, records, and mutation paths it calls.** |
| 2026-07-27 | A suggestion position offers ONE kind of thing at a time. | Mode words while what is typed can still become one, category names once it cannot. Brigadier merges every branch into one alphabetical list, so a mode literal beside a name argument produced `alphabetically, Arabic Numbers, food, newest` with nothing marking which was which (TG11 A6). The mode word is now parsed out of the single greedy argument instead of being a literal. |
| 2026-07-27 | The two delete buttons carry a single-use token, not the category name. | A scrolled-back chat line can never fire a delete against a category that changed underneath it, and no typeable "destroy this category" command sits next to `delete`. The token expires after two minutes. |
| 2026-07-27 | A category name clicked in chat opens the Hub through `/cb category open <name>`. | One real command behind the click, so the same jump works typed, and the Hub receives the category key to focus on. |
| 2026-07-27 | A category NAME never carries a formatting code; colour lives only in the record. | `§x` and the `&x` spelling are stripped from a typed name on create, rename, and assignment, and the reply says a colour code was dropped and names `/cb category color`. Left in, `&` printed raw and `§` would repaint the rest of the chat line from inside a name. |
| 2026-07-27 | A category name's colour is applied as a real text colour, not a §-code inside the text. | The Hub's custom `#RRGGBB` tint has no §-code to be written as, so a hex-coloured category used to read default-coloured in chat while the Hub showed it right. Order: hex, then colour tag, then the standard value colour. |
| 2026-07-27 | Category listings render names as the same chip every other line uses. | `info`, `filter`, and `list` output is coloured, clickable, and hoverable; a Hub action reports through the same rails as a typed command. |
| 2026-07-27 | The Hub lists `Uncategorized` even at 0 blocks. | It is the floor a block lands on by itself, so it is real when empty; it reads `Uncategorized` in grey, never a lower-case bucket label. |
| 2026-07-28 | Category descriptions are removed root-and-branch. | Not reworded, not moved to a screen: the `desc` verb, the stored `description` field, the `_desc` sync key, the chat hover line, the `info` line, the chest-editor tile and its anvil prompt, the category-list lore line, and the Hub's field + Save button + read-back are all deleted. A description in an existing `category_meta.json` is not read and drops on the next save. Nothing inherits the feature — it is scrapped, not deferred. |
| 2026-07-28 | `rename` uses the same connector grammar as `combine`: `/cb category rename <old> into <new>`. | Rename is the one verb with TWO names, and only one command argument may swallow spaces, so the old `rename <old> <new>` made a multi-word name untypeable on either side. One greedy argument split on a required `into` lets both sides be multi-word and unquoted. `into` is deliberately shared with `combine` — one connector to learn, not two. |
| 2026-07-28 | Past the `into` connector, rename suggests nothing. | The new name does not exist yet; offering the existing categories there would only invite the collision error. Before the connector it suggests real names, and once one is typed it offers `<name> into`. |
| 2026-07-28 | A category name's colour applies EVERYWHERE the name is printed, not only when it is the line's subject. | Includes mid-sentence occurrences and every listing line. One rule, so the same category never reads two different colours in two lines. |
| 2026-07-28 | A check that needs a screen open is tested in G27, not G11. | TG11 is chat and typing only. A chat chip that opens the Hub is judged in TG11 on *appearing and firing on the right category*; how the Hub then looks and behaves is [TG27 §U](../testing/Testing_Guide_27.md). The screen halves of TG11's old §D moved to TG27 §U9-§U11 and leave no marks in TG11. |

## Feature Plan

### A. Current Category Baseline

**Player outcome**

Players can organize blocks, work with a category, and maintain its basic details from one coherent command vocabulary.

**Experience**

- `/cb setcategory <id> <category>` assigns a block.
- `/cb category give`, `rename <old> into <new>`, `combine <a> into <b>`, `color`, `icon`, `sort`, `lock`, `unlock`, `delete`, `info`, `list`, and `edit` provide the supported category actions. (`desc` was removed `2026-07-28`; `merge` became `combine` `2026-07-26`.)
- `/cb categories` is the player category entry point; console callers receive text rather than a Screen attempt.
- A display block, color tag, and ordering make categories recognizable.
- Category give reports inventory overflow honestly.
- `/cb category give <category>` hands out every block that has that category as any membership, since no membership is a stored main.
- `/cb category info <name>` gives a count by default and a full block listing when explicitly asked.

**Requirements**

- Category records and assignments persist under the established data-path contract.
- Rename and merge update affected assignments consistently; a merge that would duplicate a membership just drops the duplicate.
- Category commands stay open to any player; no permission gate is added around create, rename, merge, or delete.
- Display-block selection uses the category editing flow rather than removed standalone set/clear commands.
- Create-time hints can suggest a category but never silently force one on the player.
- No category surface offers a bulk retexture action; the tile is removed, not hidden behind a flag.

**Boundary**

This baseline stays usable while the broader category model is built. It does not claim that a missing UI action exists.

### B. Multi-Category Model and Delete Safety

**Player outcome**

Creators can build useful flat category collections and share blocks across them without losing an existing block or deleting more than they intended.

**Experience**

- A block can belong to multiple categories with no main/badge priority between them.
- Categories are flat: no category has a parent, and none can contain another.
- Categories can carry a typed display name plus a matching key, icon, colour tag, and order.
- A `filter` replaces today's `sort` and applies both to the category listing and to the blocks inside a category, with its own mode set for each.
- Category-listing modes: alphabetical, newest-to-oldest, oldest-to-newest, most blocks first, grouped by colour tag, and hide-empty.
- Block-listing modes inside a category: alphabetical, newest-to-oldest, and oldest-to-newest.
- `/cb setcategory <blockId> <category>...` accepts several categories at once; `/cb category set` and `/cb category remove` handle one membership at a time.
- Every category argument tab-completes to the existing category names.
- Delete presents three separate choices: remove the category only, remove only its exclusively-owned blocks, or move its blocks elsewhere first.
- Every block always carries at least one category; the built-in `Uncategorized` category is where a block lands when its last real membership is removed, and it cannot itself be deleted.
- `/cb category delete <name>` removes only that membership from each affected block, leaving any other memberships untouched.

**Requirements**

- A first-load conversion turns any leftover legacy category word into a real record; with none present it does nothing and is not a gate on the rest of the model.
- Delete confirms its chosen mode and never treats shared blocks as exclusive by mistake.
- Local export/import carries **every** membership per block, settled 2026-07-30 with G12; one format only, no legacy single-category layout alongside it. Remote share payloads (G20 §K) still need the same decision applied.
- No membership carries a stored priority; a listing, icon, or badge picks from the category being browsed rather than reading a "main" field.

**Boundary**

This is a category data-model evolution, not a blanket change to every block-management surface. It reaches players through commands until the G27 category Screens exist; the current chest category menus keep the single-category view.

### C. Export, Share, and Import

**Player outcome**

Creators can export a complete category locally, then later share or import it through a safe remote route.

**Experience**

- `/cb category export <category>` produces a ZIP with category metadata, assignments, and required assets.
- The G27 Export Dashboard Screen calls this same export route rather than reimplementing it.
- `/cb category share` and `import` become available only when the G20 Vault path is ready.
- Import handles existing IDs through a deliberate conflict-resolution flow.

**Requirements**

- Local ZIP creation is separate from download-link presentation and remote upload.
- Links do not leak host/IP information or pretend to be reachable when they are not.
- Remote import validates the incoming category payload before merging it into local records.

**Boundary**

G11 defines category contents. G27 owns the Export Dashboard Screen, G12 owns the local export file and folder import, and G20 owns download links and all remote share/import transport.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G07 | Bulk category action | G07 supplies selection/confirmation; G11 applies consistent category assignment rules. |
| G10 | Retexture ownership | Category surfaces never offer bulk retexture; single-block retexture stays a G10 image path reached from block editing. |
| G12 | Local export | G11 builds category contents; G12 serialises every membership into the file. Download presentation is G20's as of 2026-07-30. |
| G20 | Vault share/import | G20 transports validated category artifacts; G11 owns their schema and local merge rules. |
| G27 | CategoryHub, Create-workspace, and Export Dashboard Screens | G27 owns their Screen/Studio presentation; G11 supplies category data, mutations, and export contents. |
| G28 | History | Category edits expose clear reversible actions where the history contract supports them. |

## Technical Contract

- G11 is the source of category record, membership, display, and assignment semantics. Categories are flat; no record carries a parent, and no membership is a stored main.
- Current command registration uses `/cb category <action>` plus `/cb setcategory`; removed scattered verbs and `/cb autocategorize` are not reintroduced.
- Category UI routes call G11 services/commands and preserve a text-only console fallback.
- A first-load conversion creates durable records from any leftover legacy category assignment; it is not a prerequisite for the multi-membership model, which stands on the record model alone.
- Membership is a set per block in its own store; `SlotData.category` survives only as a derived display shadow of that set and is never read back as truth. Both stores fold a typed name to its key the same way — lower-case, with spaces, hyphens, and underscores treated alike — so a name that resolves in one resolves in the other, and a separator swap is a key collision rather than a second category.
- A membership change is its own undo step carrying both whole sets, because a snapshot pair could only restore the display shadow.
- A delete operation states its exact scope and distinguishes uncategorizing, exclusive deletion, and move-then-delete.
- Category listing order comes from a stored per-category creation time; block order inside a category uses the slot index, which is already monotonic per creation because deleted indices are permanently reserved.
- Category export serializes validated category metadata with required block references/assets; remote upload/download remains outside the local schema layer.
- Category storage follows the shared `config/customblocks/data/` path convention.
- The category record has no description field, and `category_meta.json` has no `description` key. An old file keeping one is read past and rewritten without it (`2026-07-28`).
- A verb taking two category names uses the shared ` into ` connector and one greedy argument; only one argument in a command may swallow spaces, so two name-shaped arguments can never both be multi-word.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Vault share/import | Requires the G20 remote service and a conflict-resolution route. | G20 with G11 |
| Multi-category routing in the existing chest menus | Commands come first; the chest menus keep the single-category view until G27 Screens exist. | G11 with G27 |
| Remote category share payload carrying multi-memberships | Local export settled 2026-07-30 (all memberships, one format); the Vault payload still needs designing. | G20 with G11 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-14 | Individual category commands had separate top-level names. | One `/cb category <action>` command family is used. |
| 2026-06-14 | `/cb autocategorize` was a standalone command. | Categorization suggestion is a create-time hint only. |
| 2026-06-30 | A cramped narrow Category tab was the proposed Create experience. | The wider workspace uses the main Studio canvas. |
| 2026-07-09 | Old chest category menus were the intended browser. | `CategoryHubScreen` is the intended player browser. |
| 2026-07-25 | Delete offered exactly three modes, named by a trailing mode word: `category`, `exclusive`, `move <target>`. | One `/cb category delete <name>` that asks in chat with two clickable choices. Moving blocks out first is `combine <a> into <b>`. No mode words. |
| 2026-07-25 | The block-destroying delete sat behind `/cb confirm` as one undo batch. | The button click is the confirmation, and it writes two separate undo entries instead of one. |
| 2026-07-25 | `merge <source> <target>`. | `combine <a> into <b>`, with the `into` connector required so the surviving category is unmistakable. |
| 2026-07-25 | A multi-word category name was typed quoted, and mode words followed the name. | Names are never quoted; the mode word comes first so the name can stay the last greedy argument. |
| 2026-07-10 | The wider Create Studio Category workspace was tracked as a G11 decision. | Screen ownership moved to G27; see [G27 §N](GROUP_27_SCREENS.md). |
| 2026-07-25 | G11 tracked CategoryHubScreen, the Create-workspace Category tab, and the Export Dashboard as its own Screen work. | All three moved to G27; G11 keeps only category data, mutation, and export contents. |
| 2026-07-25 | A category Bulk Retexture tile was deferred to G10 with G07 for a future supported flow. | The tile is removed from `CategoryEditMenu` and the idea is scrapped, not deferred. |
| 2026-07-25 | Category records were to carry hidden/locked state and permissions. | Owner rejected them as clutter; visibility and permission gating are out of the category model entirely. |
| 2026-07-25 | Category records were to carry sounds, particles, an accent colour, a per-block badge, and auto-add rules. | All five rejected as clutter; the existing icon and colour tag are the category's visual identity and membership is always deliberate. |
| 2026-06-30 | The tree was unlimited in depth, with the UI warning past two levels. | Superseded same day: a two-level hard cap. |
| 2026-07-25 | Nesting became a two-level hard cap, a parent listing named its sub-categories, and delete refused while children existed. | All dropped same day: categories are flat, no record carries a parent, and nothing in the model refers to a tree. |
| 2026-06-30 | Delete had a fourth mode that removed every block shown in the tree. | Dropped: there is no tree for a tree-wide delete to reach. |
| 2026-06-30 | A gated migration had to create records for existing assignments before the model could expand. | Owner confirmed no category data is in use; a no-op-unless-needed first-load conversion replaces the migration phase and gates nothing. |
| 2026-06-30 | Templates and reordering would let a creator repeat a category style. | Templates are scrapped; reordering becomes a `filter` with named listing modes. |
| 2026-06-14 | `/cb setcategory <id> <category>` replaced whatever category a block was in. | It adds a membership instead; re-filing a block now needs an explicit removal or replace verb. |
| 2026-07-12 | G11 assumed category export would carry tree, memberships, and customization data. | Settled 2026-07-30 for local export: every membership travels, in one single format. Remote payloads remain G20's to design. |
| 2026-06-14 | An empty string meant "uncategorized"; no category record existed for it. | A real `Uncategorized` category record is the default and cannot be deleted; a block is never in a bare empty state. |
| 2026-06-30 | A block had one main category chosen by position, promoted automatically when removed. | Dropped same day: no membership is a stored main; browsing/badges pick from context, not a saved priority. |
| 2026-06-14 | A category carried a free-text description, set by `/cb category desc <name> <text>` and printed in `info`, the chat hover card, the chest menus, and the Hub. | Removed outright `2026-07-28`. The field, the command, the sync key, and every surface that printed it are deleted; a stored description is dropped on the record's next save. Not deferred to any group. |
| 2026-06-14 | `rename <old> <new>` took two single-word arguments. | `rename <old> into <new>`: one greedy argument split on the required `into`, so both names can be multi-word and unquoted — the same grammar as `combine`. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_11_Done.md) · [All Groups](README.md)

- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G12 Export & Import](GROUP_12_EXPORT_MARKETPLACE.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 11 snapshot](../archive/group-migration-2026-07-18/GROUP_11_CATEGORY.md)
