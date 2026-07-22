# Group 06 - Tools and Block Interaction

> Group 06 owns the physical tools that change CustomBlocks, their immediate player feedback, and the shared deletion and glow behavior behind them.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_06.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Tools should feel immediate and predictable: a Square recolors on the same moment its feedback appears, the Omni-Tool has a small, intentional set of modes (Glow, Hardness, Delete, Face, Copy), deletion is recoverable, and configured glow appears cleanly on placed blocks.

This Group owns tool interaction and the data contracts it needs. It does not own shared message wording, editor screens, image creation, pack transport, or undo history.

## Ownership

| Owns | Does not own |
| --- | --- |
| Square, Triangle, Rectangle, Deleter, and Omni-Tool interaction behavior | Shared message wording and kick presentation: G04 |
| Omni-Tool Face-mode rotation gesture | Face-image transform storage and model mapping: G08 |
| Immediate local prediction for Square results | Pack generation and delivery: G05 |
| Whole Recycle-Bin: deletion rail, markers, Trash surface, restore, and empty | Arabic-letter behavior beyond tool coverage: G13 |
| Immediate placed-block glow prediction | Tool configuration and all editor/config screens: G27 |
| Omni Copy grab/paste of all block stats | Per-block drops and marker-customization screens: G27 |
| Creative tools tab and tool-give shortcuts | Undo and redo history: G28 |

## Direction

Standalone tools remain clear physical items. The Omni-Tool adds an alternate route for selected operations but does not replace the Deleter, Squares, or Triangles. Tool actions keep the server authoritative while clients predict only the visual result that is already safe to show.

The deletion path is one shared Recycle-Bin system, not separate rules for commands, tools, bulk actions, and menus. Tool screens use the Group 27 Screen pattern, while Group 06 retains ownership of the behavior those screens configure.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-27 | Every delete route uses one Recycle-Bin rail and an identity-carrying marker. | Deletion, restore, and empty cannot drift between tools, commands, bulk actions, or menus. |
| 2026-07-15 | Square feedback appears with the local recolor and the matching server message is consumed once. | A player sees one immediate result without changing G04 wording. |
| 2026-07-18 | Omni-Tool modes are Glow, Hardness, Delete, Face, and Copy. | Paint and Admin are scrapped; the old Eyedrop becomes Copy. |
| 2026-07-18 | Omni Delete works exactly like the standalone red Deleter: instant, no confirm, recoverable in trash. | One delete behavior, no extra confirm gate on the multi-tool. |
| 2026-07-18 | Omni Copy grabs a clicked block's four feel-stats (glow, hardness, sound, walk-through/collision), then the next click pastes them onto another block. | While Copy holds stats, the hotbar keeps showing a paste prompt until the paste happens or Copy mode is left. Look/textures, shape, name, and category are NOT copied. |
| 2026-07-18 | Omni Face mode does 90-degree clockwise rotation of the clicked face only; no other Face actions ship. | The block and shape stay still; each face remembers its own rotation and every copy of the block ID updates. The old Face-action backlog is dropped. |
| 2026-07-19 | Omni modes are switched by sneak + right-click, cycling in fixed order Glow → Hardness → Face → Copy → Delete (Delete last so it is not hit by accident). | Plain right-click always performs the current mode's action; sneak + right-click steps to the next mode. |
| 2026-07-19 | Each mode switch shows a hotbar line naming the new mode plus a small click sound. | The player always knows the current mode without opening the Screen. |
| 2026-07-19 | Rotating a face that is already painted spins the painted image along with the face. | Rotate acts on whatever is drawn on that face; no special-casing of painted vs base faces. |
| 2026-07-19 | G06 owns the entire Recycle-Bin: delete, restore, and empty. | G09 keeps only full-server safety backups and has no per-block trash role; restore/empty do not hand off mid-flow. |
| 2026-07-19 | The Omni-Tool remembers its mode per player across logout and restart. | All five modes persist in `OmniToolState`, the same way Glow/Hardness already do. |
| 2026-07-19 | Face rotation works on every block shape, not just full cubes. | G08 maps the rotated face image onto whatever faces the shape (slab, stairs, etc.) exposes. |
| 2026-07-19 | Omni Copy copies the sound as a plain reference. | Paste sets the target's sound to the source value with no support check; if it plays for the source it plays for the target. |
| 2026-07-16 | Standalone Deleter, Squares, and Triangles stay as physical items. | Omni-Tool Delete is an additional route, not a replacement. |
| 2026-07-19 | The Omni-Tool mode-selection Screen is dropped entirely — no Screen anywhere. | Everything is in-hand: sneak + right-click cycles modes; in Glow/Hardness mode plain right-click cycles the value. This supersedes the 2026-07-18 "Screen owned by G27" decision. |
| 2026-07-16 | Rainbow Rectangle (standalone) paints faces on one block. | Area/corner selection is not part of this tool. |
| 2026-07-18 | Held and dropped item hand-glow is dropped. | Only placed-block glow is supported; there is no held/dropped item light tracker. |
| 2026-07-18 | Renaming/editing the CB tool items is dropped as unnecessary. | Tool names and looks stay as they are. |
| 2026-07-18 | Per-block drops customization and the deleted-marker customization screen move to G27. | G06 does not build those screens. |
| 2026-07-19 | A slot assigned to a live block must never sit in the permanent deleted set. | The sweeper never turns a placement whose slot is live into a marker, and a boot self-heal scrubs any stale live index off the deleted set — a live custom block can never be tombstoned by stale deletion data. |

## Feature Plan

### A. Fast Recolor Tools

**Player outcome**

A recolor tool changes a valid target and gives one matching hotbar result in the same moment.

**Experience**

- Fixed and custom-hex Squares cover ordinary custom blocks and Arabic SlotBlocks.
- A successful recolor shows `Swapped to <display name>` immediately.
- Reusing the same color shows `Already <display name>` immediately.
- The later server confirmation does not repeat the line.
- Triangle and Rectangle variants retain their distinct paint behavior, including per-face paint where selected.

**Requirements**

- `ClientSwapPredictor` predicts only the accepted local visual result.
- `ColorSwapTool`, `ShapeToolItem`, and `CustomColorToolItem` use the same prediction and dedupe contract.
- `ClientSlotCache` supplies the remote display name; local sessions use the local slot data.
- `ColorVariantService.swapPlaced` remains the server-authoritative mutation path.
- Group 04 remains the source for wording, colors, and shared message rules.

**Boundary**

This work removes perceived latency for normal tool use. It does not invent an error state where the existing operation has no real failure case.

### B. Omni-Tool and Creative Access

**Player outcome**

Players have one configurable multi-mode tool while keeping the familiar standalone tools for direct jobs.

**Experience**

- Omni modes are Glow, Hardness, Delete, Face, and Copy — five modes, no more.
- Sneak + right-click cycles modes in fixed order Glow → Hardness → Face → Copy → Delete (Delete last); each switch shows a hotbar line naming the mode plus a small click sound.
- Plain right-click performs the current mode's action; in Glow/Hardness mode that click cycles the value. There is no mode Screen — everything is in-hand.
- The tools creative tab holds tools, not custom blocks.
- Tool-give shortcuts give the intended item and starting mode.
- Deleter, Squares, and Triangles stay available as separate items.

**The five modes**

- **Glow** — cycle the block's glow value (built).
- **Hardness** — cycle the block's hardness (built).
- **Delete** — instant delete, no confirm, exactly like the standalone red Deleter; recoverable in trash via the shared deletion rail.
- **Face** — rotate the clicked face image 90 degrees clockwise per click (`0 -> 90 -> 180 -> 270 -> 0`); nothing else.
- **Copy** — click a block to grab its four feel-stats only (glow, hardness, sound, walk-through/collision); the next click pastes them onto another block. Look/textures, shape, name, and category are NOT copied. While Copy holds stats, the hotbar keeps a persistent paste prompt until the paste happens or the player leaves Copy mode.

Paint and Admin were scrapped (2026-07-18). Eyedrop was renamed Copy and widened from one value to all stats.

**Requirements**

- `OmniToolItem` and `OmniToolState.Mode` carry the five-mode list (today they carry only Glow + Hardness — Delete/Face/Copy are unbuilt).
- Mode switching is in-hand via sneak + right-click cycling; there is no mode Screen to build.
- The old `OmniMenu` chest-GUI route is retired outright (no Screen replaces it).
- Tool-tab contents and `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle`, `/cb rectangle`, and `/cb hexagon` stay aligned with the final mode/item mapping.

**Face mode direction**

- Face mode edits the image on the clicked face; it does not rotate the block or its shape.
- Each normal right-click turns only that face image 90 degrees clockwise: `0 -> 90 -> 180 -> 270 -> 0`.
- If the face is already painted (§C per-face paint), rotate spins the painted image with it — rotation acts on whatever is drawn on the face, painted or base.
- Every face remembers its own rotation independently.
- Face mode works on every shape (slab, stairs, and other G08 shapes), not only full cubes; G08 maps the rotated image onto the faces that shape exposes.
- A face edit changes the custom block ID, so every placed copy of that block shows the same result.
- Online players receive the updated face immediately without rejoining or accepting another resource-pack prompt.
- Rotation is the ONLY Face action. The earlier backlog (mirror, copy/paste face, swap, reset, clone, zoom, fit styles, per-face tint, etc.) is dropped; any advanced per-face image editing, if ever revived, belongs to the G27 Editing workspace, not this tool.

**Boundary**

G06 owns tool actions and shortcuts. G08 owns face-image storage and model mapping. G27 owns the screen framework, the Omni Screen, and screen design language.

### C. Recycle-Bin Deletion

**Player outcome**

Deleting a block leaves a clear recoverable marker instead of a broken slot block, and every delete method behaves the same way.

**Experience**

- A deleted placed copy becomes a `Deleted: <name>` marker with a neutral static appearance.
- Markers break instantly and drop nothing.
- Restore re-creates the block and resolves its markers, including markers encountered after a restart.
- Empty permanently releases the reserved slot and leaves a generic marker that cannot revive accidentally.
- The Trash surface shows the reserved slot number for each recoverable item.

**Requirements**

- `DeletionService` is the only per-block deletion rail for commands, the Deleter, bulk delete, GUI delete, and broken-block cleanup.
- `DeletedMarkerBlock` and its block entity retain the deleted block identity and display name without becoming a `SlotBlock`.
- `MarkerResolver` converts loaded markers immediately and resolves distant markers on chunk load with a bounded budget.
- `TrashSlots` persists slot reservation until Empty; slot reuse never corrupts distant placements.
- Restore, Empty, pack refresh, HUD refresh, and G28 undo/redo use the same shared state rather than per-path journals.

**Boundary**

G06 owns the entire recycle bin — deletion rail, markers, Trash surface, restore, and empty. G09 owns only separate full-server safety backups; G28 owns command-history behavior.

### D. Glow Behavior

**Player outcome**

A configured glow value appears the moment a block is placed, with no dark-to-bright flash.

**Experience**

- Dedicated clients predict the correct glow value on placement rather than briefly placing an unlit block.

**Requirements**

- `SlotBlock.CLIENT_GLOW_RESOLVER` follows the established client name/sound resolver pattern.
- `ClientSlotCache` and HUD synchronization provide the glow value for dedicated clients.
- `SlotBlock.getPlacementState` uses the client resolver when the local slot map is unavailable.
- Existing placed-block retroactive glow updates remain compatible with this path.

**Dropped (2026-07-18)**

- Held and dropped configured-item hand-glow is dropped. There is no client light tracker for items in hand or on the ground — only placed blocks emit light.

**Boundary**

Placed-block glow is server-authoritative state; the client prediction only removes the placement flash.

### E. Color and Image Tool Follow-ups

**Player outcome**

Color-tool variants remain understandable and image cleanup has a focused path when a real input asset is available.

**Experience**

- Custom hex Squares and Triangles behave as tool variants, not block-shape features.
- Triangle fill behavior remains a tool choice.
- Background removal preserves intentional shadows while removing unwanted pale edges and transparent padding.

**Requirements**

- Custom-hex tool variants stay grouped with tool behavior.
- Background-removal work waits for the actual source PNG so thresholds are tuned against real pixels.
- The image pipeline may snap remaining near-white background to black and flatten transparent padding only if the source confirms that approach preserves the intended shadow.

**Boundary**

G06 owns how a tool invokes the outcome. G10 owns the broader image and color authoring workflow.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Tool feedback wording | G06 triggers the immediate result; G04 supplies the exact shared message text and presentation rules. |
| G05 | Texture and identity resync | Tool mutations use the standard pack and client refresh contract without creating a parallel delivery path. |
| G08 | Face-image transforms | G06 owns the tool gesture; G08 stores each face's transform and maps it onto full and shaped block models. |
| G09 | Full-server backups | G06 owns the whole recycle bin (delete, restore, empty); G09 owns only separate full-server safety backups and has no per-block trash role. |
| G10 | Image and color authoring | G10 provides image/color work; G06 applies tool-specific variants and action behavior. |
| G13 | Arabic recolor coverage | Square recolor coverage preserves Arabic letter form and join behavior. |
| G27 | Screens | G27 builds and owns the remaining screens: image/color editor surfaces, per-block drops, and the deleted-marker customization screen. G06 supplies only the behavior behind them. The Omni-Tool has no Screen at all (dropped 2026-07-19 — all in-hand). |
| G28 | Undo and redo | G06 mutations expose one consistent operation boundary for history to reverse or repeat. |

## Technical Contract

- The server remains authoritative for actual block mutation, deletion, restore, slot allocation, and pack changes.
- `ClientSwapPredictor` may show only the immediate Square result and must dedupe a matching later confirmation.
- `Chat` remains the shared wording source; G06 must not duplicate or fork message content.
- Every delete entry point routes through `DeletionService`; marker identity is persisted and does not depend on an in-memory placement journal.
- A recoverable deleted block reserves its slot. Empty releases it and makes remaining generic markers non-healable.
- A placement becomes a `Deleted:` marker only when its slot index is retired **and** not currently a live block (`shouldMark` in `DeletedPlacementSweeper`). A boot reconcile (`DeletedSlots.reconcileLive`) removes any live slot wrongly left in the deleted set, so a live block's placements are never tombstoned by stale data (legacy delete+restore damage self-repairs on restart).
- Dedicated-client glow resolves from synced client data for the placement prediction only; there is no held/dropped item light tracker.
- Omni mode is switched by sneak + right-click cycling `OmniToolState.Mode` in fixed order (Glow → Hardness → Face → Copy → Delete); each switch emits a hotbar line and a click sound. Plain right-click runs the current mode; there is no mode Screen.
- Omni Delete routes through the shared `DeletionService`; Omni Copy reads only the four feel-stats (glow, hardness, sound, walk-through/collision) and re-applies them to the paste target through the same setters the editors use — never look, shape, name, or category. Sound is copied as a plain reference with no target-support check.
- `OmniToolState` persists the active mode per player across logout and restart, for all five modes.
- Face rotation is stored per block ID and per face as a quarter-turn value; it rotates whatever is drawn on the face (painted or base), updates every copy, persists through restart, and remains one reversible operation.
- Other screen code uses the G27 pattern; `OmniToolItem` and tool commands retain the behavior contract in this Group and carry no Screen.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Marker appearance customization Screen | The safe default marker and deletion rail come first. Moved out of G06 (2026-07-18). | G27 |
| Background-removal rim and padding adjustment | Needs the actual source PNG and a fresh visual reproduction. | G06 with G10 |
| Wider pack rebuild changes beyond the hex path | Requires a separate, evidence-led delivery investigation. | G05 |
| Further tools-tab redesign | The final contents and shortcut behavior must settle before visual polish. | G06 with G27 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-27 | Delete paths used a `(Removed)` block, permanent slot retirement, and separate per-path logic. | One Recycle-Bin rail uses identity markers, persisted reservation, restore, and Empty. |
| 2026-07-16 | Omni-Tool included Area mode and Rainbow Rectangle was treated as an area tool. | Area is removed; Rainbow Rectangle is per-face paint and Face is the Omni mode. |
| 2026-07-16 | Omni configuration stayed a chest GUI with a save-default setting. | Dropped: no chest GUI and no Screen. Modes cycle in-hand via sneak + right-click; values cycle on plain right-click. |
| 2026-07-18 | Omni mode-selection Screen was to be built and owned by G27. | Dropped entirely (2026-07-19); the Omni-Tool has no Screen — all in-hand. |
| 2026-07-16 | Omni modes were Glow, Hardness, Delete, Face, Paint, Eyedrop, and Admin (seven). | Five modes: Glow, Hardness, Delete, Face, Copy. Paint + Admin scrapped; Eyedrop became Copy (four feel-stats only). |
| 2026-07-16 | Face mode kept a large backlog of image actions (mirror, swap, clone, zoom, tint…). | Face mode is rotate-only; the backlog is dropped. |
| 2026-07-16 | Dynamic held/dropped item glow used a client-only light tracker. | Item hand-glow is dropped; only placed-block glow remains. |
| 2026-07-16 | Block drops and hex/name editing belonged to the tools document. | Those screens and authoring flows belong to G27. |
| 2026-07-18 | Renaming/editing the CB tool items was on the table. | Dropped as unnecessary. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_06.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G09 Backup and Safety](GROUP_09_BACKUP_SAFETY.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G13 Arabic](GROUP_13_ARABIC.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 06 snapshot](../archive/group-migration-2026-07-18/GROUP_06_TOOLS.md)
