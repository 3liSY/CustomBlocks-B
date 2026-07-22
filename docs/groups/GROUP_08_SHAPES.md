# Group 08 - Shapes and Per-Face Textures

> Group 08 makes a custom block look, collide, rotate, and keep its face-specific textures as the player expects.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_08_Done.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Shapes are real block behavior, not only a model choice. A slab, stair, pane, or pillar must have matching collision, outline, persistence, texture mapping, and placement direction. Per-face textures must keep their intended relative side when the block rotates.

This Group owns shape and face-texture logic plus their commands. It does not own the Block Studio or advanced face-editor screen, image authoring, bulk orchestration, resource-pack transport, or recolor-tool variants.

## Ownership

| Owns | Does not own |
| --- | --- |
| Shape definitions, shape commands, collision, outline, persistence, and placement state | Screen framework and Block Studio surfaces: G27 |
| Per-face texture storage, command behavior, and model mapping | Image authoring and source images: G10 |
| Directional placement and rotated face mapping | Pack generation and distribution infrastructure: G05 |
| Base shape preview | Bulk selection and confirmation: G07 |
| Future custom-shape geometry contract | Recolor-tool variants: G06 |

## Direction

The shape value is a **per-block-definition property stored on `SlotData`**, not a Minecraft block-state property. With ~3100 registered slot blocks, adding shape/facing/half as block-states multiplied to ~3.97M block-states and OOM'd registration on boot (2026-07-20 locked decision), so the block carries only `LIGHT`. Collision and outline read the shape live via dynamic bounds. **§B (reload-free shape draw) is built and reload-free:** the pack emits ONE shape-INDEPENDENT (full-cube) model per static slot, and the client draws the slot's actual shape at chunk-bake time from `BlockShapes` boxes through the batched model wrap (`DirectionalSlotModel`/`SlotModelPlugin`), so `/cb setshape` is a `SlotData` write + client resync with **no pack push and no reload prompt** (a shape change triggers one client world re-mesh, no download). Per-placement orientation is stored on the existing `AnimSlotBlockEntity` (no block-state). §J (directional stairs, facing/half) is **built 2026-07-21** on that same wrap — the placed stair's baked mesh is rotated into the chunk mesh through the Fabric rendering API, NOT a per-frame `BlockEntityRenderer` — so it avoids the confirmed BER "not chunk-batched" FPS risk by construction. §B and §J both draw shape and hitbox from one `BlockShapes.orient` primitive, so they cannot desync.

Each face texture belongs to the block's relative face, like a sticker attached to that face. Rotation therefore remaps face variables as well as geometry. Editor presentation lives in G27, while G08 keeps command and data behavior as the one shared foundation.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-23 | **The client off-atlas texture caches never remember a NEGATIVE derived from an absent/partial pack file, and a resource-reload listener clears them on every reload.** | Kills the shaped/animated item-icon "full cube" leftover (§F3): a false "not animated / no off-atlas texture / no icon fallback" cached while the pack was still being (re)generated is no longer permanent. `AnimFrameCache`/`StaticFrameCache` distinguish TRANSIENT build failures (source absent/partial → retry next frame, self-heal) from STABLE ones (present + parsed but ineligible → cache), and `offatlas_cache_invalidator` — a `SimpleSynchronousResourceReloadListener` in `CustomBlocksClient` — drops both caches plus the temp debug gate on `F3+T` and any foreign `reloadResources()`. Confirmed steady-state static/atlas slots still cache on first touch (no per-frame churn). |
| 2026-07-21 | **§B draws every static slot's shape at runtime through the batched model wrap; the pack emits one canonical, shape-independent model per slot.** | Locks the reload-free path. The pack's block model for a static slot is always the FULL-cube model (`cubeAllJson`/`cubeFacesJson`/`rotatedCubeJson`) and its item model is always the builtin/entity icon, so **no pack byte depends on shape** and `/cb setshape` becomes a `SlotData` write + client resync with no pack push and no reload prompt. The wrap emits geometry from `BlockShapes.boxes`/`stairBoxes` and samples each face's sprite off the wrapped full cube's baked quads, so §E per-face overrides carry over for free and `shape=full` is a literal pass-through. Scope is all 10 shapes including stairs + corners, so the pack stops shipping per-slot corner models. Face quarter-turns (G06 §G) sync to the client via `ClientSlotCache` behind a `CLIENT_FACEROT_RESOLVER` seam, matching `CLIENT_SHAPE_RESOLVER`. A shape change triggers a full client world re-mesh (one brief hitch, no download). |
| 2026-07-21 | **§J per-placement stair facing/half is stored on the BlockEntity and drawn via a batched model wrap, never a `BlockEntityRenderer`.** | Avoids both failure modes at once: no facing/half block-states (so no OOM), and no per-frame BER draw (geometry bakes into the chunk mesh, so no FPS regression on weak hardware). `DirectionalSlotModel`/`SlotModelPlugin` wrap only slots whose current shape is directional; the drawn mesh and the collision box share `BlockShapes.orient`, so they cannot desync. Built 2026-07-21; owner-confirmed 2026-07-22 (TG8 §J). |
| 2026-07-20 | **Shape, facing, and half are NOT block-state properties. Shape is data-driven (SlotData); the pack rebuilds on `/cb setshape`.** | Reverses the 2026-07-12 §B and 2026-07-11 §D block-state decisions. With ~3100 registered slot blocks, `LIGHT(16)×SHAPE(10)×FACING(4)×HALF(2)=1280` states each = **~3.97M block-states**, which OOM'd `SlotManager.registerAll` on boot (game died before any crash report). Only `LIGHT` stays a property (luminance must bake per-state). Per-placement stair orientation is deferred to a BlockEntity redesign (§D). Root cause in `PROGRESS_LOG.md` (2026-07-20). |
| 2026-06-28 | Slot blocks use dynamic bounds and cached shape values. | Collision and outline update live instead of retaining an invisible full cube. |
| 2026-07-11 | Accepted face names are `up`, `down`, `north`, `south`, `east`, and `west`. | Commands, storage, and models use one unambiguous six-face vocabulary. |
| 2026-07-11 | Per-face textures stay attached to the relative face when a block rotates. | Model rotation remaps face texture variables, not only geometry. |
| 2026-07-11 | Directional placement uses horizontal facing and top/bottom half only. | Vertical facing is excluded; stairs and slabs receive vanilla-like placement behavior. |
| 2026-07-12 | Shape editor content belongs in the G27 Block Studio Shape section. | G08 retains logic and commands; a standalone ShapeEditorScreen does not return. |
| 2026-07-12 | Advanced face editing belongs to G27.18's live cube selection design. | The small face-tile fold and legacy chest editor are not the final editor direction. |
| 2026-07-12 | Shape changes use pre-baked variants and a blockstate update. | `/cb setshape` must not push a resource-pack reload prompt. |
| 2026-07-12 | Existing worlds may default legacy blocks to north after orientation states are introduced. | Players may need to replace backwards existing placements; the disruption is accepted. |
| 2026-07-18 | Omni-Tool face rotation turns only the clicked face image in saved 90-degree steps. | Face-image rotation is independent from block placement direction, geometry, and the rotations saved on the other five faces. |

## Feature Plan

### A. Shape Commands and Live Physics

**Player outcome**

Changing a block to a slab, stair, pane, pillar, or another supported shape changes both its visible model and its physical behavior.

**Experience**

- `setshape`, `addshape`, `removeshape`, `clearshape`, `shapelist`, and `shapepreview` use one shape vocabulary.
- Supported shapes include full, bottom/top slab, thin, carpet, wall, pane, stairs, cross, pillar, and custom bounds.
- A player can stand on a slab, move through the open parts of thin/pillar/pane/stairs, and select the actual outline.
- Shape choice persists through restart and can be reversed through the normal history route.

**Requirements**

- `SlotData` persists the selected shape.
- Slot block registration uses dynamic bounds; `BlockShapes` caches each named `VoxelShape` for inexpensive live lookup.
- Dedicated clients resolve shape data from the synced client path when the local server map is absent.
- Shape command tab completion exposes only supported shape names.

**Boundary**

G08 owns shape semantics. The Block Studio is only another front door to the same shape logic.

### B. Reload-Free Shape Models

> ⚠️ **Reverted 2026-07-20 (Scrapped).** The block-state `shape` property this section describes OOM'd registration at ~3100 blocks (see the 2026-07-20 locked decision). `/cb setshape` rebuilds + pushes the pack again. A reload-free approach would need per-slot render without a multiplied block-state (future BlockEntity design).
>
> **2026-07-20 design (discussed, not built):** Every `SlotBlock` already carries a `BlockEntity` (`AnimSlotBlockEntity`, Group 14) and a client renderer (`AnimSlotBER`) that hand-draws the block instead of trusting a baked model — proven in production for animated/off-atlas textures and for Arabic per-placement facing (stored on the BE, synced via the normal BE update packet, zero block-states). Fabric's own docs confirm the design rule this Group hit: blockstates suit "a few hundred states at most"; anything near-infinite (our ~3100 slots × shape/facing combos) is a BlockEntity's job. The reload-free path is: keep shape on `SlotData` (unchanged), but instead of the pack baking a per-shape model, the slot's model emits the shape's box list (`BlockShapes.boxes(shape)` — the exact same coordinates already used for collision/outline) with the slot's live texture. `/cb setshape` becomes a data write + BE resync, no pack rebuild.
>
> **UPDATE 2026-07-21 (from building §J):** §J proved the render vehicle — do §B through the **batched model wrap** (`DirectionalSlotModel`/`SlotModelPlugin`, geometry baked into the chunk mesh), NOT an `AnimSlotBER` hand-draw. A `BlockEntityRenderer` draws every instance as its own call and craters FPS in bulk (~5 vs ~280 FPS in community benchmarks) — the exact GT 730 risk the owner rejected.
>
> **DESIGN LOCKED 2026-07-21 (owner, before build).** Five decisions, all verified against the code first:
> 1. **Scope: all 10 shapes, full §E per-face.** One code path — no split where some shape changes reload and others do not. Stairs and their corners are included, so the pack stops emitting the 4 corner models per stairs slot and `DirectionalSlotModel` emits `BlockShapes.stairBoxes(shape)` directly.
> 2. **Canonical pack model = today's FULL model.** `staticShapeModelJson` stops branching on shape. The wrapped full cube's 6 baked quads already carry the right sprite per face *including* §E overrides, so the wrap samples them instead of resolving texture Identifiers, and `shape=full` stays a byte-identical pass-through.
> 3. **Item model is always the builtin/entity icon.** The old `plainFull` branch made the item JSON shape-dependent, which alone would have kept the reload prompt alive.
> 4. **Face quarter-turns sync to the client.** `FaceRotations` is server-only today (baked into the model JSON as `uv`+`rotation`), so a runtime mesh cannot see it; without a sync, every non-full shape would silently lose G06 §G rotation on a dedicated server.
> 5. **Shape change re-meshes the client world.** With no pack reload nothing invalidates existing chunk sections; a full re-mesh is correct by construction and needs no per-slot placement index.
>
> Enabling facts confirmed in code: `/cb setshape` changes only `models/block/slot_N.json` (textures are keyed by slot, never by shape); the client already receives shape via `ClientSlotCache`/`CLIENT_SHAPE_RESOLVER`; animated and Arabic slots are already shape-agnostic (invisible model + BER) and are out of scope.

**Player outcome**

Changing shape is immediate and does not interrupt the player with a resource-pack reload prompt.

**Experience**

- Changing among full, slab, stairs, pane, cross, and other supported shapes sends an ordinary block update.
- Other players see the changed model through the same normal state update.
- Create and retexture work may do the one-time model bake needed for later fast changes.

**Requirements — SUPERSEDED, do not build as written below**

> The four bullets below describe the reverted 2026-07-20 block-state approach — kept only as a historical record of what NOT to build. Current requirements are the 2026-07-20 BlockEntity design note above this section.

- ~~`ServerPackGenerator` emits all shape model variants for each block when its texture/model is prepared.~~
- ~~The slot block state includes the shape property used to select those variants.~~
- ~~`ShapeCommands.applyShape` changes the stored shape and block state without issuing a resource-pack push.~~
- Existing collision and outline resolution continue to read the selected shape correctly. *(still true — unaffected by either approach)*

**Boundary**

G08 defines which variants are required; G05 owns generated-pack delivery and reload mechanics.

### C. Per-Face Textures

**Player outcome**

Each of the six faces can carry an independent texture, clear back to the base texture, and keep the correct relative side after orientation changes.

**Experience**

- `setface`, `clearface`, and `clearallfaces` work for all six accepted face names.
- An unset face uses the block's base texture.
- Full cubes and non-full shapes use the same face-override rules.
- A rotated block keeps its painted front, top, and other relative faces where the player expects them.
- A face image can keep its own `0`, `90`, `180`, or `270` degree turn without rotating the block or another face.

**Requirements**

- Face assets use `slot_N_face_<face>.png` storage and matching `SlotData` metadata.
- Full-cube and shape-model generation both resolve `TextureStore.hasFace` before falling back to the base texture.
- Shape model elements use face-specific texture references rather than hardcoding `#all`.
- Rotated model states remap face variables consistently with geometry rotation.
- Saved face-image quarter-turns are keyed by block ID and face, persist through restart, and apply to full and shaped models.

**Boundary**

G08 owns face data and model mapping. G10 owns how images are made or prepared before a face command applies them.

### D. Placement Direction and Halves

> ⚠️ **Reverted 2026-07-20 (Scrapped).** The `HORIZONTAL_FACING` + `BLOCK_HALF` block-states this section requires were part of the ~3.97M block-state OOM (see the 2026-07-20 locked decision). Per-placement stair orientation is deferred to a BlockEntity-stored redesign; the block carries only `LIGHT`.
>
> **Built 2026-07-21 (batched BlockEntity path).** `facing`+`half` are stored as plain fields on `AnimSlotBlockEntity` (same discipline as `arabicFacing` — set on placement, written to NBT only when non-default, synced via the standard BE update packet). `SlotBlock.getPlacementState` captures them (vanilla stair rules) and `onPlaced` stamps the BE; `SlotBlock.getOutlineShape`/`getCollisionShape` read them via `world.getBlockEntity(pos)` and feed the already-written `BlockShapes.orient` math. Only `isDirectional(shape)` (stairs today) is touched — every symmetric shape ignores facing/half exactly as before, zero change. Visual rotation is a **batched model wrap** (`DirectionalSlotModel` + `SlotModelPlugin`): the slot's baked stair mesh is rotated into the chunk mesh through the Fabric rendering API using the BE's render data, so there is **no per-frame `BlockEntityRenderer` draw** — this is what avoids the confirmed BER FPS risk. The drawn mesh and the hitbox share one rotation primitive (`BlockShapes.orientPoint`/`orientDir`, the point form of the collision's `rotX180`/`rotY90cw`), so they cannot desync. Only slots whose current shape is directional are wrapped; every other block keeps its untouched vanilla model and render path. Built + owner-confirmed 2026-07-22 (TG8 §J: facing, half, corners, hitbox==visual, persistence, FPS).

**Player outcome**

Stairs face the player, top slabs and upside-down stairs honor the clicked half, and a face-painted block can be placed with a deliberate front.

**Experience**

- Placement writes one horizontal facing and one top/bottom half state.
- Shape hitboxes rotate with stairs and other directional shapes.
- Full blocks may track orientation even when their base texture makes it visually neutral.
- Old placements remain readable, defaulting to north until a player replaces them.

**Requirements — SUPERSEDED, do not build as written below**

> The first three bullets below describe the reverted 2026-07-20 block-state approach — kept only as a historical record of what NOT to build. Current requirements are the 2026-07-20 BlockEntity design note above this section.

- ~~`SlotBlock` carries `HORIZONTAL_FACING` and `BLOCK_HALF` state alongside existing state values.~~
- ~~`BlockShapes` rotates collision and outline geometry from those states.~~
- ~~Pack model variants apply the matching horizontal rotation and face-variable remapping.~~
- Arabic compatibility work may simplify only after native rotation is available and verified. *(still true — unaffected by which approach ships)*

**Boundary**

This is block placement behavior, not an Arabic-only workaround or a new image-editor feature.

### E. Preview and Future Shape Tools

**Player outcome**

Players can quickly understand a shape before applying it and may later receive focused helpers for face direction or sculpting.

**Experience**

- `/cb shapepreview <shape>` shows a temporary base shape and removes it automatically.
- Future sculpting would use a dedicated live editor and one final history action rather than a loose tool mode.

**Requirements**

- The base preview remains independent of a custom texture argument.
- A custom sculptor, if revived, needs a separate geometry/performance design for smoothed visual mesh and collision before implementation.

**Boundary**

The optional textured preview and sculptor are future work; neither may weaken the reliable base preview path.

### F. Defects from the 2026-07-22 MP test pass — F1–F3 fixed, F4 parked

Four defects were found in the 2026-07-22 MP (dedicated-server) test pass. F1 and F2 are fixed and owner-confirmed; F3 had a first fix on 2026-07-22 (pack-routing) and a follow-up fix on 2026-07-23 (a client texture-cache race — see below), and awaits a re-test; F4 is parked as §L. Root causes were verified against source. Test rows live in TG8 (J7/J8/J9, B5, B6).

**F1 — Upside-down stair corners picked the wrong wedge (TG8 J8/J9). Fixed 2026-07-22, owner-confirmed.**

- Symptom: a top-half (upside-down) stair corner drew/collided as the mirror of the correct piece — a mangled corner where two stairs meet on the ceiling.
- Root cause: `StairConnection.compute` chooses `INNER_LEFT`/`INNER_RIGHT`/`OUTER_LEFT`/`OUTER_RIGHT` in the base (NORTH/bottom) frame. `BlockShapes.orient` renders a top half as `x:180 + y:180` = **180° about Z**, which negates X and Y but keeps Z — it preserves front/back (inner vs outer) and mirrors only left↔right. The 2026-07-21 J8 fix verified this for the straight stair only (left/right symmetric); the asymmetric corner wedges (`INNER_LEFT` = SW quadrant vs `INNER_RIGHT` = SE) landed on the wrong side.
- Fix shipped: `StairConnection.topSwap` swaps the LEFT↔RIGHT result of the chosen corner (`INNER_LEFT↔INNER_RIGHT`, `OUTER_LEFT↔OUTER_RIGHT`) when `half == BlockHalf.TOP`; STRAIGHT is symmetric and untouched. Front/back classification and the `isDifferentOrientation` guard are unchanged (Z is preserved). Collision reads the same stored `StairShape` through the same `orient`, so hitbox stays `== visual` automatically.

**F2 — Omni-Tool face rotation did nothing on a shaped block, MP only (TG8 B5). Fixed 2026-07-22, owner-confirmed.**

- Symptom: on a dedicated server, rotating a face of a non-full (shape) block with the Omni-Tool showed no change. Worked in single-player.
- Root cause: `OmniToolItem.rotateFace` (and the undo/redo `FACE_ROTATE` cases in `HistoryCommands`) called `ResourcePackServer.updatePack()` but never `HudSync.broadcast(...)`. A full cube bakes its rotation into the pushed pack model (`FaceModelBuilder.rotatedCubeJson`), so it updated on the pack reload. A §B shaped block reads its rotation only from the synced packed value (`ClientSlotCache.rot` → `SlotGeometryData.resolveFaceRot`), which `rotateFace` never refreshed — so the remote client re-meshed with the stale rotation. SP works because `resolveFaceRot` reads `FaceRotations.packed` directly in-process.
- Fix shipped: `OmniToolItem.rotateFace` and the undo/redo `FACE_ROTATE` restores now call `HudSync.broadcast(...)`, so `ClientSlotCache.rot` changes and `ClientSlotCache.geometryDiffers` triggers the world re-mesh.

**F3 — Plain shaped item icon showed a FULL CUBE on a dedicated server (TG8 B6). Fix built 2026-07-22, MP re-test pending.**

- Symptom: on a dedicated server a plain shaped block's item icon (hotbar, inventory, first-person hand) rendered as a full cube — for **every** shape, not just stairs — while the PLACED block still showed the shape. Single-player showed the shape correctly.
- Root cause: the shaped icon was drawn only by `SlotItemRenderer`, a Fabric `DynamicItemRenderer` that fires **only** for a `builtin/entity` item model. That item model comes from the resource pack, which on a dedicated server is authored by the SERVER; when that routing doesn't reach the client the renderer never fires and the item falls back to its plain baked cube. The placed block was unaffected because its shape is drawn by the pack-independent `DirectionalSlotModel` wrap, which reads the synced `ClientSlotCache` shape. Single-player worked because the client writes its own loose pack. This is the same pack-authoring fragility Group 30 hit and fixed for the guess disguise (`ItemDisguiseMixin`). The earlier "wrong presentation angle" reading was only the SP-visible half of the problem — the real MP failure was that no shape drew at all.
- Fix shipped (pack-independent, mirrors G30): `ShapedItemMixin` hooks `ItemRenderer.renderItem` at HEAD and `ShapedItemIcon.tryRender` draws the shape from the synced `ClientSlotCache` shape — the same `BlockShapes` boxes as the placed mesh and the hitbox — then cancels the vanilla draw. No pack byte is involved, so it can never no-op on a dedicated server. Gated to the plain shaped case: it defers guess (to `ItemDisguiseMixin`) and skips full / cross / animated / painted / rotated slots, so painted/rotated shaped slots keep their atlas cube icon (the standing §B trade). The stair icon's fixed presentation facing (EAST; flip to SOUTH if the step faces away) lives in `ShapedItemIcon`; `SlotItemRenderer`'s shaped branch was removed so there is one authority. The painted-slot gate is cached in `StaticFrameCache.hasPerFace` (cleared on resource reload).

**F3 follow-up 2026-07-23 — the leftover was a client texture-cache race, not the pack routing.** A re-test still showed a full/flat icon for some slots even in **single-player** (the temp `SHAPED-ICON dbg` line logged `remote=false`), which the pack-routing fix above cannot explain. Diagnosis from that line: slot 244 (an animated grid slab) bailed at `tex==null` — both `AnimFrameCache.get` and `StaticFrameCache.getIconFallback` returned null — while a plain slot (678) drew fine. Cause: the integrated server regenerates and serves the resource pack right after world load, and `slot_244.png` was written ~26 s AFTER the icon first rendered. On that first render the png was ABSENT, so `AnimFrameCache.build` returned null and the slot was cached as `NOT_ANIMATED` — a false negative. The shaped-icon path then fell through its `BAIL animated` gate and died at `tex==null`. That negative was only ever cleared by a *following* managed reload (`ClientPackReceiver`/`ResourcePackGenerator` clear the caches in their reload `thenRun`); a manual `F3+T`, a video-settings pack toggle, another mod's `reloadResources()`, or a regen with no following reload left the false negative pinned — a permanent full cube until the next managed reload. The same trap applied to `StaticFrameCache`'s `NOT_OFFATLAS`/`ICON_NOT_READABLE` for static off-atlas and static shaped slots.

- Fix shipped 2026-07-23, two layers. (1) **Anti-poison caching:** `AnimFrameCache` and `StaticFrameCache` now classify a build failure as TRANSIENT (source model/png absent, unreadable, or partially written — pack mid-(re)generation) vs STABLE (present and parsed, but genuinely ineligible — an atlas model, a single static frame, or a grid slot the animated path owns). Only a STABLE verdict is remembered in `NOT_ANIMATED`/`NOT_OFFATLAS`/`ICON_NOT_READABLE`; a TRANSIENT one is not cached, so the slot is retried next frame and self-heals the instant the file lands — no reload required. A present-but-unreadable `.grid.json` is treated as transient too, so an animated slot whose sidecar has not finished writing is never mistaken for static; the `res.isEmpty()` bails cost only a resource lookup (no decode), so steady-state static/atlas slots still cache on first touch with no per-frame churn. (2) **Reload-proof invalidation:** a client `SimpleSynchronousResourceReloadListener` (`offatlas_cache_invalidator`, registered in `CustomBlocksClient`) clears both caches — and the temp `SHAPED-ICON dbg` once-per-slot gate — on EVERY resource reload, so `F3+T` or any foreign `reloadResources()` can no longer leave a stale entry that only the managed paths would have cleared. Together these make the shaped/animated icon independent of pack-regeneration timing and of which code triggered a reload. The temp `SHAPED-ICON dbg` logging in `ShapedItemIcon` stays for this one confirmation pass and can be removed once the owner confirms. Awaits owner re-test with `customblocks-1.0.0.jar`.

**F4 — Vanilla block-behavior parity for non-stair shapes → §L, `Parked 💤 ✏️`.** Today only `stairs` is directional/connecting (`BlockShapes.isDirectional`). Owner scope (slab→full merge, wall/pane/fence connect, directional placement for all shapes, fence gates, trap/doors, waterlogging, redstone/pressure) is a large multi-shape build; needs its own design session before scoping. Recorded in Deferred Scope and TG8 §L.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Variant models and delivery | G08 supplies the needed state/model variants; G05 distributes the generated pack safely. |
| G06 | Tool and face-action boundaries | G06 owns Omni-Tool gestures and action choice; G08 owns saved face transforms and their model mapping. |
| G07 | Bulk shape | A future bulk-shape action uses G07 selection/confirmation and G08 mutation semantics. |
| G10 | Face images | Image creation and preparation happen in G10 before G08 applies a face texture. |
| G13 | Rotation compatibility | Native orientation may replace Arabic-specific facing work only after equivalent behavior is proven. |
| G27 | Shape and face editors | G27 provides Block Studio's Shape section and advanced cube face selection; G08 owns the command/data contract. |
| G28 | History | Shape and face mutations expose one normal reversible operation. |

## Technical Contract

- Shapes are stored per block definition in `SlotData` and applied to placed blocks through slot block state plus live dynamic bounds.
- `BlockShapes` is the single geometry source for collision, outline, preview geometry, and generated shape models.
- The pack emits ONE shape-INDEPENDENT (full-cube) model per static slot; the client draws the slot's current shape at bake time from `BlockShapes` boxes (§B), so `/cb setshape` is a `SlotData` write + client resync with no pack push and no reload prompt (shape is data-driven, not a block-state — see the 2026-07-20 and 2026-07-21 locked decisions).
- Face storage and model variables use exactly `up`, `down`, `north`, `south`, `east`, and `west`.
- Face-image rotation is a per-face quarter-turn transform, separate from placement-facing and shape rotation.
- A non-full model must resolve per-face overrides using the same fallback rule as a full cube.
- Placement records only `LIGHT` (glow) as a block-state. Horizontal facing / top-bottom half are NOT block-state (reverted 2026-07-20 — the extra states OOM'd registration); per-placement stair rotation is stored on `AnimSlotBlockEntity` and drawn via a batched model wrap (§J, built 2026-07-21) — no block-state, no per-frame `BlockEntityRenderer` draw, and the drawn mesh shares `BlockShapes.orient` with the hitbox so they cannot desync.
- G27 editor requests invoke the same G08 command/mutation behavior rather than a separate Screen-only implementation.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Textured `/cb shapepreview <shape> [id]` | Base preview is sufficient; custom-texture display needs a separate rendering path. | G08 |
| Custom Shape Sculptor | Needs a bounded geometry, smoothing, collision, and editing-performance design. | G08 with G06 and G27 |
| Bulk shape | Depends on both final G08 mutation behavior and G07 bulk integration. | G07 with G08 |
| §L Vanilla block-behavior parity | Parked, needs a design session (TG8 §L). Today only `stairs` is directional/connecting (`BlockShapes.isDirectional`); every other shape has no placement rotation or neighbour logic. Owner scope: slab→full merge, wall/pane/fence auto-connect, directional placement for all shapes, fence gates open/close, trap/doors, waterlogging, redstone/pressure behavior. Large multi-shape build — each behavior is its own slice with collision, model, state, and sync work. | G08 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-11 | `customtriangle` and `trianglemode` lived with shapes. | They are recolor-tool variants owned by G06. |
| 2026-07-11 | Face editor would be a small fold inside the Shape section. | Advanced face editing uses G27.18 live cube selection. |
| 2026-07-12 | Shape editing used a standalone ShapeEditorScreen. | It opens G27 Block Studio's Shape section. |
| 2026-07-12 | A shape change rebuilt and pushed the resource pack. | Tried pre-baked variants + a `shape` block-state (§B, built 2026-07-20), but at ~3100 blocks the block-state count OOM'd registration on boot — **reverted the same day**. A shape change rebuilds + pushes the pack again; shape is data-driven on `SlotData`. |
| 2026-07-11 | Directional placement added `HORIZONTAL_FACING` + `BLOCK_HALF` block-states (§D/§J). | Reverted 2026-07-20 (part of the block-state OOM). Per-placement stair orientation is deferred to a BlockEntity-stored design; the block carries only `LIGHT`. |
| 2026-07-11 | FaceGuide swaps the world block for a flattened guide cube. | §H FaceGuide is **removed entirely** (2026-07-21) — `/cb faceguide` and its code are deleted. There is no face-guide feature; face names are documented by `/cb setface` help. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_08_Done.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G13 Arabic](GROUP_13_ARABIC.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 08 snapshot](../archive/group-migration-2026-07-18/GROUP_08_SHAPES.md)
