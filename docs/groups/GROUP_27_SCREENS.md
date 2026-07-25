# Group 27 - Unified Screens, Create Studio, and Editing

> Group 27 gives CustomBlocks one calm, capable Screen experience: a consistent visual system, one Studio for creating and editing blocks, and clear Screen handoffs without stealing the underlying feature logic.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_27.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

CustomBlocks has many powerful systems, but their old Screen and chest surfaces were inconsistent, cramped, and often forced players through separate command flows. G27 establishes one interface language and makes the Studio the place where a player creates, loads, changes, previews, and saves a block.

G27 owns interaction design, client layout, Screen state, input, previews, and routing. The feature Groups continue to own their data, server behavior, limits, commands, validation rules, and testing truth. A Screen is never a reason to duplicate a backend.

## Ownership

| Owns | Does not own |
| --- | --- |
| Shared Screen frame, theme, keyboard behavior, modals, toasts, forms, and accessibility settings | Chat wording and command-result policy: G04 |
| Studio landing, Create mode, Edit mode, editing workspace, previews, and Screen routes | Block data, setters, texture/media rules, and resource-pack updates |
| Paint/resize user workflow and client working state | Image processing limits: G10; animation decode/frame rules: G14 |
| Screen versions of the vault conflict, backup, trash, safety, diagnostics, macro, and other migrated surfaces | Vault, backup, diagnostics, macro, and game logic: their owning Groups |
| Text, recolour, shape, animation, HUD, bulk, and configuration Screen presentation | Text, colour, shape, animation, HUD data, bulk, and config backend behavior |
| CategoryHubScreen, the Create-workspace Category tab, and the Export Dashboard Screen | Category data, mutation, tree/membership rules, and export contents: G11 |

## Direction

Every CustomBlocks Screen follows the same red, black, and lime language: near-black workspace, red borders and selected states, clear cards, real text fields, readable hover help, and lime only for a successful primary action. The Screen should feel focused rather than like a grid of unrelated menus. It uses stable layout, smooth 3D preview only where it helps, and a fixed 2D workspace where precision matters.

`/cb create` becomes the Studio front door rather than a pile of disjoint commands. From one landing chooser a player can make a block, load one to edit, enter the Editing workspace, continue a session, start from a template, duplicate, or revisit recent work. The Studio never assumes the client is authoritative: it previews locally and sends one explicit server action for an approved save/apply operation.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-18 | All Screen work uses the unified red/black/lime theme. | The retired gold/cyan/chest visual directions do not return. |
| 2026-06-18 | Screens use consistent cards, spacing, text fields, hover help, action hierarchy, modal discard confirmation, and shortcut help. | New and migrated Screens feel related and do not use anvil/chest text-entry workarounds. |
| 2026-06-19 | `/cb create` opens a Studio landing chooser; there is no separate `/cb edit` command. | Create, edit, duplicate, template, and recent-work flows begin in one place. |
| 2026-06-19 | Existing blocks load into Studio through a full settings-and-texture sync. | Edit Mode pre-fills real data and preview rather than reconstructing a partial clone. |
| 2026-06-19 | Edit Mode supports Save changes, Save as copy, Save as draft, live ReID validation, and a locked-block refusal. | In-place mutation remains deliberate, undoable, and consistent with G25. |
| 2026-07-12 | Recolour and shape are Studio sections, not standalone Screen destinations. | Old recolour/shape routes reroute only after the Studio replacements are ready. |
| 2026-07-15 | The top-level Studio tab is named Editing and contains Paint and Resize modes. | Painting and resizing stay connected without pretending the workspace is only about images. |
| 2026-07-15 | Editing uses a movable, zoomable fixed 2D viewport with a secondary live 3D result preview. | Precision editing never depends on spinning a cube. |
| 2026-07-15 | GIF resize is supported through G14 rules; GIF paint is not part of the current Editing release. | Animation pixel painting cannot silently corrupt frame data. |
| 2026-07-15 | The old Arabic word selection menu moves to Studio Text. | G13 provides the text backend while G27 owns the Unicode-capable Screen flow. |
| 2026-07-18 | Shared block browsers provide an `Animated` filter inside a Filters menu. | GIF and animated WebP blocks can be found without adding a redundant Static filter. |
| 2026-07-18 | `/cb animation` opens the shared block browser with `Animated` already selected; choosing a block opens `/cb create` on that block's Animation tab. | The old animated-only chest picker is replaced by one Screen route. |
| 2026-07-18 | `/cb editor` opens the same modern block browser as `/cb list`; choosing a block opens that block in Studio Edit Mode, the same destination as `/cb editor <id>`. | `/cb listgui` stays removed and no second block-browser route is created. |
| 2026-07-23 | The Backup and Trash Screen surface moves here from G09 (its command/data layer is complete). Scope needs discussion before build. | G09 keeps persistence/restore/retention; G27 owns the Screen. See §F and the Deferred Scope note. |
| 2026-07-20 | The Bulk Operations Hub has no natural-language "ask" / NL command bar; blocks are targeted by ticking rows and narrowing with filters. The Blocks List gains a `Category ▾` filter that combines (AND) with the All/Favorites/Locked/Selected chip. | The mis-targeting NL parser (a phrase could tick every block) is gone; targeting is explicit and cannot silently act on the whole list. |
| 2026-07-25 | CategoryHubScreen, the Create-workspace Category tab, and the Export Dashboard Screen move here from G11. | G11 keeps category data, mutation, and export contents; G27 owns their Screen presentation. See §N. |

## Feature Plan

### A. Shared Screen System

**Player outcome**

Any CustomBlocks Screen is readable on first use, responds predictably, and makes important actions obvious without chat spam.

**Experience**

- Near-black workspace, red active/selection treatment, compact dark cards, stable headers, and generous spacing organize each Screen.
- The title names the current job and block when relevant; every control has concise hover help.
- A strong primary Create/Save/Apply action is visually clear; Cancel is quieter but reachable.
- In-Screen success, error, copy, and save feedback uses toasts or action feedback. Typed commands keep their normal chat responses.
- Unsaved changes use a real Discard or Keep editing confirmation instead of an accidental close.

**Requirements**

- `CbScreenTemplate`, forms, buttons, fields, modal confirmation, shortcut overlay, and toast behavior are reusable primitives rather than copied implementations.
- Use real `CbTextField`-style inputs with labels and stable gaps; avoid anvil/paste workarounds and overlapping controls.
- Support `Ctrl+Z`, `Ctrl+Y`/`Ctrl+Shift+Z`, `Ctrl+C`, `Ctrl+V`, `Enter`, `Esc`, and `?` where meaningful; `R` resets only 3D views and `Ctrl+R` randomizes only when a Screen supports it.
- Provide dim/hide-world, UI-scale, large-text, reduced-motion, sound, and discard-confirmation settings through a single Screen settings surface.
- Notification overload batches/limits visual and audio feedback instead of stacking noise.

**Boundary**

Shared chrome does not turn every Screen into the same layout. Eyedrop sampling stays intentionally minimal, 2D editors remain 2D, and every owner Group keeps its backend messages and validation rules.

### B. Studio Landing, Create, and Edit Mode

**Player outcome**

The player can enter one clear Studio, choose their intent, and create or change a block without losing the current design context.

**Experience**

- Landing offers New block, Edit existing, Editing, Continue last session, From template/blueprint, Duplicate a block, and Recently edited.
- Create mode groups Identity, Texture, Shape, Attributes, Organize, Animation, Text, and related sections around an always-visible live preview.
- The preview reflects loading, missing/failed texture, transparency/background, shape, glow, and animation state with human-readable cues.
- Edit Mode opens an existing block with its current identity, texture, category, shape, animation, and supported attributes pre-filled.
- Edit mode can Save changes, Save as copy, Save as draft, or change ID with a live available/taken check. A locked block explains that it must be unlocked first.
- `/cb editor` opens the same block browser as `/cb list`; after a block is chosen, it opens Studio Edit Mode exactly as `/cb editor <id>` does.

**Requirements**

- A server-to-client payload sends full editable `SlotData` plus current texture bytes into `StudioState`; edit state has an explicit `editingId`/index, never a guessed clone.
- A server-authoritative edit payload uses the existing G05/G08/G10/G11/G25 setters, ReID rail, and one resource-pack update after an approved batch.
- Existing-block edits record one before/after G17 modification record; copy uses the normal duplicate route without overwriting the source.
- Templates/blueprints clear identity for a clone path; Edit Mode preserves identity for an in-place path.
- Session memory may reopen a local unfinished Studio state, but no client-only draft silently becomes a server block.
- `/cb listgui` remains retired; editor selection reuses the current `/cb list` browser instead of restoring the old chest list.

**Discussion later — Discussion ✏️**

The exact picker click behavior, landing tab or section name, any dedicated Manage action list, live-apply versus save-only behavior, and other editor-route extras are intentionally not decided yet.

**Boundary**

G27 coordinates the fields and presentation. It does not reinvent `SlotData`, resource pack writes, lock semantics, duplicate behavior, categories, or image/media conversion.

### C. Editing Workspace: Paint and Resize

**Player outcome**

The player can draw on a texture or resize it before publishing and after creating a block, with clear before/after feedback and shared undo.

**Experience**

- Studio has one top-level Editing tab with peer Paint and Resize modes; `/cb paint` and `/cb resize` open that same workspace on the matching mode.
- The primary editor is a fixed 2D canvas: wheel/controls zoom, drag pans, Fit resets navigation, and an optional pixel grid aids precise work. A small 3D cube shows the result.
- Paint offers pen, eraser, bucket, eyedropper, line with 45-degree snap, rectangle, horizontal/vertical/four-way/diagonal symmetry, brush sizes 1/2/3, alpha-aware colour picker, checkerboard transparency, and a trace underlay from URL, another block, or the current texture.
- Resize shows source/result, numeric dimensions, 64/128/256/512 choices, custom dimensions, linked aspect ratio by default, Keep shape/Crop/Stretch, output estimate, and a deliberate Restore Original action.
- Applying a change keeps the workspace open and places short progress/action feedback in an unobtrusive lower corner.

**Requirements**

- The paint canvas is one baked dynamic texture/quad, never one draw call per pixel; high-resolution sources warn or soft-cap before expensive editing.
- Per-stroke history supports Screen undo/redo; committed existing-block changes record G17 undo/redo as one authoritative modification.
- Static pixel changes use an explicit C2S PNG-bytes path into `TextureStore.save`/`saveFace`; the client refreshes from the server result.
- Per-face painting is explicit and uses existing face storage; loading a URL/colour after paint warns before replacing the one working source.
- GIF resize applies dimensions/fit method to every frame while preserving frame count, order, timing, and loop behavior through G14. GIF frame painting stays disabled with a clear explanation.
- Oversized, invalid, empty, or unsupported media remains preview-only with a clear allowed action; G10/G14 own the actual limits and conversion rules.

**Boundary**

G27 owns the workspace and controls. G10 owns texture/image processing and G14 owns animation mechanics; no second image or GIF engine appears inside a Screen class.

### D. Studio Feature Sections

**Player outcome**

Related creation tools are discoverable inside Studio instead of being scattered across old one-off menus.

**Experience**

- Recolour provides live preview, split comparison, presets, eyedrop match, and per-zone tint inside Studio; world mutation occurs once on Save.
- Shape opens the Studio Shape section, with named shapes first and future advanced carving clearly separated.
- Text uses real Unicode input to create Arabic/text output through the G13 backend; old root-menu selection flows are removed after replacement.
- Animation opens an animated block in Studio through the shared load path, with playback controls/timeline presentation belonging to the UI while G14 controls media mechanics.
- The Studio identity/texture/attributes/category sections use grouped cards, complete-state ticks, sensible validation, source link copy, background/hex controls for static images, and no accidental publish on Enter.

**Requirements**

- `/cb recolor` with an ID loads Edit Mode focused on recolour; without one it uses the shared picker. The retired `livecolor` and standalone shape routes are removed only after the replacements work.
- Studio Text routes the old `/cb arabic word ...` block-selection flow into this Screen and sends text/style/output choices to G13 server logic.
- `/cb animation <id>` uses Studio edit loading. `/cb animation` opens the shared block browser with `Animated` selected, then opens the chosen block in `/cb create` on its Animation tab.
- The old animated-only chest picker is removed only after the shared Screen route works.
- G27 UI may offer existing G10/G14/G13 values and controls, but every server apply call validates through the owning service.

**Boundary**

Recolour algorithms, shapes, Arabic/text rendering, and animated-media storage belong to G10, G08/G13, and G14. Studio does not claim those systems as Screen work expands.

### E. HUD and Bulk Workspaces

**Player outcome**

Complex HUD and bulk work feels editable, inspectable, and reversible rather than a set of raw command flags.

**Experience**

- The HUD Builder uses independent draggable information bricks, anchors, magnetic snapping, per-brick appearance, templates/tokens, backgrounds, presets, and a live in-game result.
- HUD controls include block identity/data bricks, custom text, styling, ordering, hide-off-block behavior, keybinds, and a usable colour picker; old layouts migrate without moving unexpected content.
- Bulk Operations provides Blocks List and Bulk Actions views with search, sort, filter chips plus a `Category ▾` filter that combines with them, block previews, tick selection, clear action scope, result previews, confirmation, progress sweep, and shared history access. There is no natural-language command bar; targeting is tick + filter only.
- Shared block browsers expose `Animated` through a Filters menu. It can combine with Favorites, Locked, category, and other filters while the search box narrows the result further.
- `All` remains the normal unfiltered view; there is no separate Static filter.

**Requirements**

- HUD data uses a structured client cache/sync format for every enabled brick field; layout persistence is atomic and environment-safe.
- HUD draw positions support free placement plus screen/brick magnetic snap guides without layout shifts from dynamic content.
- Bulk Screen actions call G07 services and show their real selection, affected rows, and undo/redo outcome without reimplementing bulk mutation logic.
- Shared browser data includes whether each block is animated, and every Screen that browses custom blocks uses the same filter behavior rather than maintaining separate copies.
- The exact Filters-menu contents and visual layout beyond the approved `Animated` behavior remain open for later discussion.
- Keybinds stay client-owned and configurable through the correct settings route without changing server permissions.

**Boundary**

G03 owns base HUD behavior, G07 owns bulk operations, and G17 owns shared history. G27 owns how a player edits, views, and invokes those systems.

### F. Migrated Screens and Cutover Rules

**Player outcome**

Features such as backup, trash, safety, diagnostics, vault conflict, macros, onboarding, achievements, configuration, and game settings can become polished Screens without losing the rules that already make them safe.

**Experience**

- Vault conflict presents only one import choice at a time with clear local/incoming comparison and normal undo integration.
- Backup, Trash, and Safety give real saved/deleted texture previews, explicit destructive confirmation, and routes to diagnostics rather than hiding safety actions in chest menus.
- **Backup/Trash Screen migration (from G09, 2026-07-23) — Discussion ✏️ before build.** G09's backup engine is rebuilt and complete at the command/data layer (whole-tree deduped snapshots, kind-based retention, verify, preview/contents, granular restore, cloud pull). What remains is the Screen surface: add **multi-select delete** to the Backup Screen, expose the new backup metadata (kind tab/icon, reason/note, size, health dot from `/cb backup verify`, preview/contents/granular actions), build a **Trash Screen** (proposed `GuiMode` 19) mirroring it, and **retire the chest menus** (`BackupMenu`, `BackupConfirmMenu`, `BackupSelection`, `TrashMenu`, `TrashEntryMenu`) plus repoint `SafetyMenu`. Open questions to settle first: exact layout/tabs, whether per-block "restore just this block from backup X" is in-scope now, and the cutover order (Screen accepted before chest removal per the rule below).
- Diagnostics opens one incident detail at a time and provides history without accidentally dumping every incident into one view.
- Tutorial and achievements gallery read G23 flags/data, while macro, config, vault, and game Screens use their owning Group services.

**Requirements**

- Every migrated Screen adopts the shared G27 frame, input, modal, feedback, and text-field standards while preserving the owner Group's server API.
- An old chest/dead route is removed only after its replacement Screen is accepted and its owner verifies the feature flow.
- G27 Screen code never marks backend work complete, bypasses a permission/lock/safety check, or fabricates a result while a request is pending.
- Red/error diagnostic presentation can link to the owner Group's copyable reporting data without calling it an AI report.

**Boundary**

G27 has presentation ownership, not feature ownership. Backup remains G09, vault remains G20, diagnostics/testing remains G16, macros remain G24, onboarding/achievements remain G23, configuration remains G21/G22, and game modes remain their own Groups.

### M. Omni-Tool mode-switch Screen

**Player outcome**

Reopened 2026-07-20. G06 originally scrapped an Omni-Tool mode-selection Screen (2026-07-19) in favor of in-hand sneak+right-click cycling (G06 §F). Owner has reopened the idea: Omni-Tool mode switching (Glow/Hardness/Face/Copy/Delete) gets a Screen, owned here per the normal Screen/logic split — G06 keeps the mode logic and item behavior.

**Experience / Requirements**

Not designed. Open questions for discussion before any build:

- Trigger: replaces the in-hand cycle entirely, or opens alongside it as an alt path?
- Layout: standard G27 red/black/lime frame, list or radial mode picker?
- Does G06 §F (in-hand cycle test rows) stay as a fallback input method or get retired once the Screen ships?

**Boundary**

G27 owns the Screen and presentation only; G06 keeps mode state, switching logic, and item behavior.

### N. Category Screens (from G11)

> Moved from G11 §B/§D (2026-07-25): G11's category commands, records, membership/tree model, migration, and export contents stay in place and unchanged. Only the three Screen surfaces move: `CategoryHubScreen`, the Create-workspace Category tab, and the Export Dashboard Screen replacement.

**Player outcome**

Players browse, create, and edit categories in a proper Screen, and export a category through the same Studio visual language as every other CustomBlocks Screen.

**Experience**

- `CategoryHubScreen` lists categories with counts, icons, browse/edit actions, and category detail; a block row supports category-appropriate give, edit, and remove actions.
- The Create-workspace Category tab uses the main Studio canvas rather than a narrow panel, letting a player select/create a category, set a main category, and edit visible style fields without hiding existing records.
- The Export Dashboard Screen offers the same export result as `/cb category export <category>` without a separate console-only path.
- `/cb categories` opens `CategoryHubScreen` for players; console callers keep a text-only fallback.

**Requirements**

- Browser routes retire the old chest-menu target without breaking the console fallback.
- Screen code receives category data and invokes G11 mutation/export paths rather than reimplementing them.
- Screens read a block's categories as a set from G11's membership store, not the legacy one-word field, which survives only as a derived display shadow for surfaces G27 has not reworked yet and is removed as each one moves across.
- Screen copy and shortcuts make browse, edit, delete, export, and modifier actions discoverable without clutter.

**Boundary**

G27 owns the reusable Screen/Studio presentation. G11 owns what a category means, how its edits are applied, and what an export contains.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Communication and feedback | In-Screen toasts/action feedback follow the shared human style; typed commands retain chat replies. |
| G05 | Resource-pack assets | Studio preview consumes authoritative assets and sends one approved update, never a client-only pack mutation. |
| G07 | Bulk operations | G27 renders selection/action workflow; G07 validates, mutates, and reports bulk data. |
| G08 | Shapes | Studio Shape is the only modern Screen route; G08 owns geometry and shape data. |
| G09 | Backup, trash, and safety | G27 presents safety Screens while G09 controls persistence, restore, and deletion semantics. |
| G10 | Colour, images, and resize | Editing/recolour UI uses G10 limits and processing contracts. |
| G11 | Category Screens | G27 owns CategoryHub, Create-workspace, and Export Dashboard presentation; G11 owns category data, mutation, and export contents. |
| G13 | Arabic and text | Studio Text provides Unicode/UI flow; G13 creates and renders the text/Arabic data. |
| G14 | Animation and GIFs | Studio presents animation/resize controls; G14 owns decoding, frame behavior, and render/performance rules. |
| G16 | Diagnostics and testing | G27 can route private testing/diagnostic Screens, but G16 owns incidents, access control, and results. |
| G17 | Undo/redo | Screen commits record/use the normal shared history; no private UI-only history replaces it. |
| G20 | Vault conflict and hub | G27 renders conflict/vault UI; G20 owns import, cloud policy, and Discord behavior. |
| G21 | Configuration data | G27 presents configuration Screen fields while G21 owns registry-capacity recovery. |
| G22 | Permissions | All Screen actions execute through the same permission policy as commands. |
| G23 | Onboarding and achievements | G23 supplies book, flags, hints, and achievement data; G27 renders tutorial/cinematic/gallery Screens. |
| G24 | Macros | G27 owns MacroList presentation; G24 owns saved macro actions. |
| G25 | Identity operations | Studio Edit/ReID calls G25 validation and migration logic. |
| G30 | Guess Mode | G27 supplies a Screen shell only when requested; G30 owns game rules and settings content. |

## Technical Contract

- Shared Screen primitives use stable layout regions and reusable form/input/modal/toast behavior; visual state must not resize or shift surrounding controls.
- Studio keeps client working state separate from server state; full existing-block load uses a dedicated S2C settings-plus-texture payload and saving uses explicit validated C2S actions.
- Existing-block commit batches setters and records one before/after modification for G17 undo/redo; resource pack update occurs once after the approved batch.
- Paint uses a baked dynamic texture with per-stroke history; pixel uploads are explicit PNG bytes, optionally per face, and never direct client persistence.
- Resize delegates still-image limits to G10 and animated-frame transformations to G14, preserving animation metadata when applicable.
- Red/black/lime theme, reduced motion, scalable text, tooltips, keyboard support, discard protection, and notification aggregation apply to all migrated Screen work unless a minimal task such as Eyedrop requires an intentional exception.
- Screen migrations preserve owner APIs and do not delete old routes until a replacement has been accepted through the corresponding Testing Guide.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| GIF frame painting, layered compositing, animated text, live feeds, sound sync, visualizer, and webcam capture | They need animation/media contracts beyond current static Paint and GIF Resize. | G14 with G27 |
| Full animation timeline, frame markers, onion skin, J/K/L shuttle, and frame editing | It is one substantial dedicated timeline slice, not minor Screen polish. | G14 with G27 |
| Advanced freeform shape carving | Named shape workflow comes first; carving needs a separate safe geometry editor. | G08 with G27 |
| AI design/screenshot matching and studio cloud publication | They require G15/G20 services and their safety/cost rules. | G15/G20 with G27 |
| Premium effects, cinematic welcome extensions, 3D wiki, and admin editor | They are later experience work after core Studio and migration reliability. | G27 with owner Group |
| Custom icon-art project | Functional familiar icons come first; bespoke visual art is separate work. | Future UI art work |
| Backup/Trash Screen migration from G09 (multi-select delete, backup metadata surface, Trash Screen, chest-menu retirement) | Backend is complete in G09, but the Screen scope/layout needs a design discussion before build (see §F). | G27 with G09 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-18 | Screens used mixed gold/cyan/backdrop conventions and inconsistent bars. | All Screen migration follows the red/black/lime standard and shared primitives. |
| 2026-06-19 | Creation and editing were separate command/menu flows, including a possible `/cb edit`. | `/cb create` opens the Studio landing chooser; it contains Edit Mode. |
| 2026-07-12 | Recolour and Shape each had standalone Screen destinations. | They are Studio sections and old routes cut over after replacement acceptance. |
| 2026-07-12 | The old chest editor could remain a parallel editing surface. | `/cb editor <id>` routes to Studio Edit Mode after cutover. |
| 2026-07-15 | Painting and resizing could be disconnected image tools. | One Editing tab contains related Paint and Resize modes in a fixed 2D workspace. |
| 2026-07-15 | GIF Paint could be implied by the general Painting tool. | Current Editing supports GIF Resize only; GIF frame painting is explicitly future work. |
| 2026-07-15 | The Arabic word block-choice menu remained a root UI. | It moves into Studio Text with G13 backend handoff. |
| 2026-07-19 | Omni-Tool mode-selection Screen was scrapped; mode switching stays in-hand only (G06 §F). | Reopened 2026-07-20 — owner wants a mode-switch Screen; see §M. G06 §F in-hand cycling status pending that discussion. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_27.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G08 Shapes and Faces](GROUP_08_SHAPES.md)
- [G09 Backup and Recovery](GROUP_09_BACKUP_SAFETY.md)
- [G10 Colour and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G11 Categories](GROUP_11_CATEGORY.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G17 History, Give, Delete, and Search](GROUP_17_REGRESSIONS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G21 Registry Capacity Sync](GROUP_21_CONFIG_GUI.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G23 Player Experience, Onboarding, and Achievements](GROUP_23_PLAYER_EXPERIENCE.md)
- [G24 Macros](GROUP_24_MACROS.md)
- [G25 Block Management and Identity Operations](GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md)
- [Pre-template Group 27 snapshot](../archive/group-migration-2026-07-18/GROUP_27_SCREENS.md)
