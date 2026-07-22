# Group 08 — Shape System & Per-Face Textures

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 6 (Attributes) build-verified.
>
> **Objective:** Restore the full block shape system (slab, thin, carpet, wall, pane, stairs, cross, etc.) and per-face texture support (different image per face). Both must be accessible via commands and a Screen. The implementation must be clean — no junk or leftover broken behavior from the old version.
>
> **Source issues:** Group B (setshape, addshape, removeshape, clearshape, shapeeditor, shapelist, shapepreview, facechangegui, setface, clearface, clearallfaces, bulkshape) — `customtriangle`/`trianglemode` moved to `GROUP_06_TOOLS.md` 2026-07-11 (misfiled; they're recolor-tool features, not shapes)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.
>
> ⚠️ **UI medium audit (2026-07-10), updated 2026-07-11:** Shape editor is Screen-based — folded into the
> G27 Block Studio Shape section (§4 below) and confirmed **built** 2026-07-11 (`/cb shapeeditor` opens
> it for real; needs-retest, see G08.6). Face editor's target changed 2026-07-11: no longer folds into the
> Shape section — see §5 for the reconciled call (`GROUP_27_SCREENS.md §G27.18`), still not built.

---

## 🐞→🟢 FIXED 2026-06-28 (awaiting in-game) — shape hitbox was a FULL cube; pillar/thin/stairs/pane pushed the player

`/cb setshape` rendered the right model but the **collision + selection box stayed a full cube** — the player
was pushed out of pillar/thin/stairs/pane and couldn't stand in the open parts (slab only *looked* ok: you
stand on top of a full cube).

- **Root cause (the real one):** Minecraft **caches** a block's collision/outline VoxelShape once at
  registration. Our shape is per-slot in `SlotData` (assigned later), not in the block state, so the cache
  froze every slot at "full" and `SlotBlock.getOutlineShape`/`getCollisionShape` were **never re-consulted**.
  (An earlier round added a client shape-seam `CLIENT_SHAPE_RESOLVER`/`resolveShape` for the dedicated client —
  correct + needed, but it had no effect while the cache bypassed those methods.)
- **Fix:** `.dynamicBounds()` on the slot block settings (`SlotManager.registerAll`) disables the shape cache,
  so the shape is read **live** every query. `BlockShapes` now caches each shape's VoxelShape per-name
  (`OUTLINE_CACHE`) so live evaluation stays cheap. Together with `resolveShape` (synced shape on a remote
  client) and the G06 sweep (HudSync on `/cb setshape`), the hitbox is correct AND live for every player.
- **Test:** TG §A (A1 stand-on-slab, A2 persist, A3 clear, A5 no-holes; + walk-through cross, no-push on
  pillar/thin/stairs/pane) on a **dedicated server**.

---

## 🔒 Locked 2026-07-12 (design only, not built) — G08-A1, setshape RP-reload prompt

Owner MP test 2026-07-11: A1 passes (block becomes the shape, hitbox correct — see FIXED 2026-06-28 above)
but `/cb setshape` pops the resource-pack reload screen. Owner wants the shape to apply instantly
client-side with no reload prompt.

**Root cause:** collision/outline is already live (`SlotBlock` reads shape straight off `SlotData`, no
reload needed — that's why A2/A3/A5/A6 already pass). The reload is purely the **visual model**:
`ServerPackGenerator` regenerates the block's model JSON for the new shape and `ResourcePackServer` pushes
it as a new pack, which is what triggers the reload screen.

**Fix direction (confirmed against Fabric docs — this is the vanilla-native pattern, same as
stairs/slabs):** bake **all 10 shape model variants** into the block's pack **once**, at create/retexture
time (same texture already in the pack, one model JSON per shape). Add a `shape` **BlockState property**
that selects among the pre-baked models. `/cb setshape` then only writes the BlockState — the model is
already shipped to the client, so it's a normal block-update packet, not a resourcepack push. Zero reload,
ever, after the one-time bake.

- **Cost:** pack generation does ~10x the model work at block-creation/retexture time (once), not on every
  `setshape` call.
- **Scope:** touches `ServerPackGenerator` (bake all variants instead of current shape only), block
  registration (add the `shape` BlockState property), `ShapeCommands.applyShape` (write state, not
  regenerate+push pack).

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Block shapes | slab, thin, carpet, wall, pane, stairs, cross, full | Full block only | All shapes restored |
| Shape editor GUI | `/cb shapeeditor <id>` opened chest GUI | Present in Phase 10 screen version | Screen-based (folded into G27 Block Studio) |
| Shape list | `/cb shapelist` | Missing | Restored |
| Shape preview | `/cb shapepreview <shape>` — shows shape in world | Missing | Restored |
| Per-face textures | Different image on each face (up/down/north/south/east/west) | Single texture on all faces | Restored |
| Face editor GUI | `/cb facechangegui <id>` | Missing | Restored |
| Set face | `/cb setface <id> <face> <url>` | Missing | Restored |
| Clear face | `/cb clearface <id> <face>` | Missing | Restored |
| Clear all faces | `/cb clearallfaces <id>` | Missing | Restored |
| Bulk shape | `/cb bulkshape <filter> <shape>` | Missing | Restored (depends on Group 07) |

---

## What this group covers

| Feature | Commands |
|---|---|
| Set shape | `/cb setshape <id> <shape>` |
| Add shape | `/cb addshape <id> <shape>` |
| Remove shape | `/cb removeshape <id> <shape>` |
| Clear shape | `/cb clearshape <id>` |
| Shape editor | `/cb shapeeditor` — **screen folded into Block Studio Shape section (G27 §G27.11, decision E)**; standalone `ShapeEditorScreen` retired. G08 owns the shape *logic/commands*; G27 owns the editor *screen* |
| Shape list | `/cb shapelist` |
| Shape preview | `/cb shapepreview <shape>` |
| Set face texture | `/cb setface <id> <face> <url>` |
| Clear face texture | `/cb clearface <id> <face>` |
| Clear all faces | `/cb clearallfaces <id>` |
| Face editor GUI | `/cb facechangegui <id>` |

---

## Implementation Requirements

### 1. Available Shapes

| Shape name | Description |
|---|---|
| `full` | Default full block (1×1×1) |
| `slab_bottom` | Bottom half-slab |
| `slab_top` | Top half-slab |
| `thin` | Thin vertical panel (like glass pane, no frame) |
| `carpet` | 1/16-height ground layer |
| `wall` | Wall post shape |
| `pane` | Thin vertical panel with frame |
| `stairs` | Stair shape (bottom-front quarter missing) |
| `cross` | X cross (like flower) |
| `pillar` | Tall thin pillar |
| `custom` | Custom AABB bounding box (set via shape editor) |

Tab-complete for shape names on all shape commands.

### 2. Shape Persistence

Shape is stored in `SlotData`. On shape change:
- Update the block's AABB collision/outline shape.
- Trigger pack rebuild (models need updating).
- Action is undoable.

### 3. Per-Face Textures

Each block can have a different texture on each of 6 faces: `up`, `down`, `north`, `south`, `east`, `west`
(`TextureStore.FACES` — the actual accepted names; **not** "top"/"bottom", fixed 2026-07-11).

- If a face has no specific texture, it uses the block's default texture.
- Face textures stored alongside the main texture in `config/customblocks/textures/slot_N_face_up.png` etc.
- Face texture data stored in `SlotData`.

> ⚠️ **Known gap, confirmed 2026-07-11 — planned, not built:** non-full shapes (stairs/slab/pillar/etc)
> currently **ignore per-face textures entirely** — `ServerPackGenerator.shapeModelJson`/`element()` hardcode
> the base texture on every face of every box, unlike the full-cube path (`cubeFacesJson`) which already
> checks `TextureStore.hasFace` per face. Confirmed real gap (checked in code, not a regression — shape
> models were simply never written to look at face overrides). **Planned fix (small, low-risk, mirrors
> existing cube code):** in `shapeModelJson`, build the `textures` object the same way `cubeFacesJson`
> does (`hasFace(index, face) ? base+"_"+face : base"` per face name); in `element(int[] b)`, replace the
> hardcoded `"#all"` texture ref with `"#" + face` per face. No new systems needed. Not yet implemented —
> build when this group is picked up.

### 4. Shape Editor — folded into Block Studio (G27)

> **Ownership (sweep 2026-06-21, decision E):** the standalone `ShapeEditorScreen` is **retired**.
> `/cb shapeeditor` now opens the **Block Studio Shape section**. G08 keeps the shape *commands/logic*
> (`setshape`, `clearshape`, `bulkshape`, the RP-reload fix at G08-A1 above); the editor *screen* — full
> spec, historical chest-GUI reference, and the still-open `GuiRouter` migration/dedupe bug — **moved to
> `GROUP_27_SCREENS.md` §G27.11** (2026-07-12, screen-content consolidation).

### 5. Face Editor — target is G27.18, not the Shape-section fold

> **Re-decided 2026-07-11 (reconciling 3 conflicting plans):** the 2026-07-09 "fold FaceEditorMenu into
> the Shape section" idea below is **superseded**. The real target is `GROUP_27_SCREENS.md §G27.18`
> ("Advanced Per-Face Customization") — click-to-select faces on the live 3D cube in the Studio, with
> multi-select, copy, broken-link detection, per-face sound/light/physics. `§G27.10.G` (creation-time
> per-face toggle in the Texture panel) is the near-term slice of the same feature, not a separate one.
> None of the three are built yet; `/cb facechangegui` still runs the old `FaceEditorMenu` chat-prefill
> chest GUI in the meantime.

**Current-in-code design (chest GUI, still live):** `/cb facechangegui <id>` opens a chest GUI with:
- 6 face slots representing the block's 6 sides.
- Left-click pre-fills a `/cb paintface <id> <face> ` chat command for the URL; right-click clears that face.
- Current face texture shown via an enchant glint on painted slots.
- "Clear all faces" action slot.

**Superseded target (small fold, do not build):** ~~the Block Studio Shape section gets 6 face tiles with
drag-and-drop image support~~ — replaced by G27.18's cube click-to-select design.

### 6. Shape Preview

`/cb shapepreview <shape>` — spawns a temporary ghost block at eye level showing the shape for 5 seconds, then disappears.

---

## Setup

```
/cb create g08a ShapeTest
/cb retexture g08a https://i.imgur.com/example.png
```

Place `g08a` somewhere accessible.

---

## Test G08.1 — Set shape (slab)

```
/cb setshape g08a slab_bottom
```

**Expected:** Block in world changes to bottom-slab shape. `Block g08a shape set to slab_bottom.`

**Pass:** Block renders as bottom slab. Collision matches slab shape.
**Fail:** Block unchanged, error, or wrong collision.

---

## Test G08.2 — Shape persists after restart

Restart the server (or relog).

**Expected:** `g08a` still renders as `slab_bottom`.

**Pass:** Shape persisted.
**Fail:** Reverted to full block.

---

## Test G08.3 — Clear shape

```
/cb clearshape g08a
```

**Expected:** `g08a` returns to full block shape.

**Pass:** Block renders as full block.
**Fail:** Shape not cleared.

---

## Test G08.4 — Shape list

```
/cb shapelist
```

**Expected:** Chat shows all available shape names with descriptions.

**Pass:** All shapes listed.
**Fail:** Command missing or empty list.

---

## Test G08.5 — Shape preview

```
/cb shapepreview slab_top
```

**Expected:** A ghost block appears at eye level showing a top-slab shape. Disappears after ~5 seconds.

**Pass:** Preview appears and disappears.
**Fail:** Nothing appears, or ghost block persists.

---

## Test G08.6 — Shape editor Screen 🎯 needs retest (verified built 2026-07-11)

```
/cb shapeeditor g08a
```

**Expected (target, Screen-based):** the Block Studio Shape section (G27 §G27.11) opens with shape-selector
tiles. Clicking "stairs" changes the block to stair shape.

**Pass:** Screen opens, shape selection works.
**Fail:** Screen missing, selections don't apply, or command still opens the legacy chest GUI.

---

## Test G08.7 — Set face texture

```
/cb setface g08a up https://i.imgur.com/top_texture.png
```

**Expected:** Top face of `g08a` gets a different texture. Other faces unchanged. Pack rebuilds.

**Pass:** Top face shows different texture.
**Fail:** Error, or all faces changed.

---

## Test G08.8 — Clear single face

```
/cb clearface g08a up
```

**Expected:** Top face reverts to the block's default texture.

**Pass:** Top face matches default texture.
**Fail:** Face still shows the per-face texture.

---

## Test G08.9 — Set multiple faces

```
/cb setface g08a up https://i.imgur.com/top.png
/cb setface g08a down https://i.imgur.com/bottom.png
/cb setface g08a north https://i.imgur.com/north.png
```

**Expected:** Three faces have unique textures. Three others use default.

**Pass:** Correct faces show their unique textures.
**Fail:** Any face shows wrong texture.

---

## Test G08.10 — Clear all faces

```
/cb clearallfaces g08a
```

**Expected:** All faces revert to the default texture. `Cleared all face overrides for g08a.`

**Pass:** All faces show default texture.
**Fail:** Some face overrides remain.

---

## Test G08.11 — Face editor Screen ⏳ planned (target changed 2026-07-11 → `GROUP_27_SCREENS.md §G27.18`; not built, `/cb facechangegui` still opens the legacy chest GUI)

```
/cb facechangegui g08a
```

**Expected (target, Screen-based):** the Block Studio Shape section shows 6 face tiles. Clicking a face
tile opens drag-and-drop image input (or a URL input). After providing an image/URL, that face updates.

**Pass:** Screen opens, face image/URL input works.
**Fail:** Screen missing, face tiles don't accept input, or command still opens the legacy chest GUI.

---

## Group 08 Verdict

| Test | Description | Result |
|---|---|---|
| G08.1 | Set shape to slab | ⬜ |
| G08.2 | Shape persists after restart | ⬜ |
| G08.3 | Clear shape restores full block | ⬜ |
| G08.4 | Shape list shows all shapes | ⬜ |
| G08.5 | Shape preview appears and disappears | ⬜ |
| G08.6 | Shape editor GUI works | ⬜ |
| G08.7 | Set single face texture | ⬜ |
| G08.8 | Clear single face reverts to default | ⬜ |
| G08.9 | Multiple faces set independently | ⬜ |
| G08.10 | Clear all faces restores defaults | ⬜ |
| G08.11 | Face editor GUI opens URL input | ⬜ |

**Group 08 passes when shapes and per-face textures both work in-game.**

If anything shows ❌ — paste:
1. The exact command typed
2. What the block looked like vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g08a
```

---

## 💡 Future idea — Custom Shape Sculptor *(currently just an idea — not planned, not built)*

> Captured 2026-06-13. A direction to explore **after** Group 08 is confirmed working in-game.
> Nothing here is committed; it may become its own group later.

**The wish:** a tool (likely an Omni-Tool mode) to "curve/shape any pixel of any custom block however I
like" — freeform shaping instead of the fixed preset shapes.

**The honest constraint:** Minecraft block models can't do **true curves**. A block model is
axis-aligned cuboid "elements" only, with rotation limited to 22.5° steps on one axis. So "curving"
isn't possible — but **freeform shaping out of voxels/boxes is**, and the foundation already exists:
`BlockShapes` already turns a list of boxes into both the model elements *and* the collision union, so a
sculptor just makes that list **data-driven** instead of hardcoded.

**Two possible flavors:**
- **Voxel sculptor** — treat the block as an 8×8×8 grid; each cell on/off. Add/carve cells to build any
  blocky shape. Needs greedy-merging of cells into larger boxes for render performance.
- **Box editor** — define a handful of arbitrary boxes (from/to + optional 22.5° tilt). Fewer pieces;
  can fake "slanted" looks. Closer to Blockbench.

**How the tool could work in-world:** `useOnBlock` already exposes the clicked face (`getSide()`) and the
exact hit point (`getHitPos()`), so a `SCULPT` Omni-Tool mode could add a voxel where you right-click and
carve the one you point at — live model + collision rebuild (debounced), one undo step per click.

**Constraints to respect if we build it:** render perf (cap resolution at 8³, greedy-merge boxes);
it edits the block *type* (all placed copies change, like shapes today); store the voxel mask on
`SlotData` (compact bitset) with undo; v1 paints the base texture on every box (per-box texturing is a
much bigger job — defer). The Group 08 `custom` AABB shape is the small seed of this.

**Direction locked 2026-07-11 (still not built):** go straight for real smooth-feeling geometry, not the
boxy voxel fallback — NoCubes-style: a mesh generator turns the on/off cell grid into both a smoothed
visual AND a smoothed `VoxelShape` collision (proven approach, NoCubes ships this on 1.21). This is a
real performance cost (NoCubes' own docs call the per-block mesh regen "wasteful") and a bigger build than
the plain box-union shapes elsewhere in G08 — accepted tradeoff, owner's call.

**Tool workflow (locked 2026-07-11):** new Omni-Tool "Sculpt Mode" → right-click a placed block → opens a
live 3D sculpting screen (blown-up grid of cells) → click/drag to add/remove single cells (Minecraft
place/break metaphor) → live NoCubes-style smoothing preview while you work → Apply commits one undo step
for the whole session (not per-cell).

**Cool-factor additions locked 2026-07-11:**
- Sphere vs cube brush shape (round carves in one click vs blocky)
- Mirror/symmetry mode (carve one side, other side auto-mirrors)
- Per-block resolution choice (default 8×8×8; pick finer for a showpiece block, cost stays local to that block)
- Multi-step undo history while sculpting, separate from the single Apply-time undo
- Toggle: raw blocky grid view vs smoothed preview, so you can carve precisely then check the final look

**Status:** direction + tool design locked, still idea-stage — no code written. Revisit after Group 08
passes in-game; write a proper group spec before building.

---

## 🧭 Planned — Face direction helper ("FaceGuide") *(idea, agreed — not built yet)*

> Captured 2026-06-13. Replaces the confusing coloured-tile face editor as the way to learn "which
> side is which." The face *editor* (paint/clear) stays; this is purely about **showing directions**.

**Part 1 — a built-in FaceGuide block.** On load the mod auto-seeds one custom block (like it already
seeds the built-in tools) with **N / E / S / W / UP / DOWN** painted on its six faces, so each face
plainly states which world direction it is. It's a normal, placeable, usable custom block. The letter
textures are **drawn in-code** (no download — offline-safe), using per-face textures (already supported).

**Part 2 — inspect a placed block in place.** A toggle command (e.g. `/cb faceguide`): look at a custom
block and run it → that block **temporarily** swaps to the FaceGuide appearance where it sits → run again
(or look away) → it swaps back. No pack rebuild / reload prompt — it's a live block swap. Chat walks the
player through it. **Safety:** the swap stores the original block and auto-restores after ~30s and on
relog/disconnect, so a block can never get stuck looking like the guide.

**Naming note:** call it `FaceGuide` / `/cb faceguide`, **not** "ShapePreview" — `/cb shapepreview`
already exists for shapes, so reusing that name would confuse.

**Locked 2026-07-11:**
- **Visibility:** the swap is a real world block change, visible to every nearby player, not just the
  one who ran the command — simplest to build, accepted tradeoff (anyone near the block during the ~30s
  preview will see it flip to labels).
- **Non-cube shapes:** if the target block has a shape (stairs/slab/etc), the preview temporarily
  flattens it to a full cube so all 6 direction labels are visible at once, then restores the real shape
  when the preview ends — same restore mechanism as the texture swap.

**Status:** agreed direction, not built. Build as a Group 08 slice after the current work; front-end only
where possible (per-face textures + a temp block-swap with restore).

---

## 🧊 PARKED — Textured shape preview: `/cb shapepreview <shape> [id]` *(base works; `[id]` parked 2026-07-11)*

> ⚠️ **Status: base `/cb shapepreview <shape>` (vanilla stand-in) works and passed in-game — unaffected,
> keep using it.** The optional **`[id]`** argument (preview wearing a custom block's texture, no pack
> reload) is **parked indefinitely** as of 2026-07-11 — owner call: not worth building right now, revisit
> only if it becomes worth it later. Design kept below for reference if picked back up.

> Captured 2026-06-13. Extends the working `/cb shapepreview <shape>` (which floats a vanilla stand-in
> block) so you can preview a shape **wearing one of your custom block's textures**, with **no pack
> rebuild / reload prompt**.

**The trick:** the custom block's texture is already in the loaded pack as `customblocks:slot_N`. A
`block_display` can show that block's model **transformed** (scale + translate) into a shape's box —
and `summon` takes the transformation as NBT, so no code-side entity API and no rebuild. For each box in
`BlockShapes.boxes(shape)` (the same source the collision/model use), summon one transformed copy of
`slot_N`; auto-remove after 5s, exactly like the current preview.

- Single-box shapes (slab/carpet/thin/wall/pillar): one transformed display — exact.
- Multi-box (stairs/pane): one display per box.
- `full`: the block at scale 1. `cross`: billboard — can't be made by transforming a cube; fall back to
  the vanilla stand-in or just show `full` (decision when built).

---

## 🧭 §G08.8 — Directional Placement (Rotation & Halves)

> **Origin:** Owner brainstorm 2026-07-08 — *"when placing blocks edited with shapeeditor to stairs, its direction is fixed, and so does normal slotblocks, needs big discussion about their logic."*
> 
> **Phase:** G08 (Shapes). **Depends on:** `ServerPackGenerator` (G05).
> 
> **The Problem:** Right now, `SlotBlock` is completely agnostic to rotation. If you place a block with a "stairs" shape, or a full block with a TV screen texture on the front face, it just drops facing North. It behaves like a static monument because the game doesn't record which way you were looking.

### The Solution: Full Vanilla Logic for Custom Blocks

We are injecting full vanilla placement logic into **every** custom block. 
When a player places a custom block, the server will now track:
1. **Horizontal Facing:** (North, South, East, West) based on where the player is looking.
2. **Block Half:** (Top, Bottom) based on whether the player clicked the top or bottom half of a block (used for upside-down stairs and top-slabs).

Because every custom block shares the exact same core class (`SlotBlock`), this logic applies across the board:
- **Shapes:** Stairs will finally face you. Clicking the top of a block places upside-down stairs or top-slabs.
- **Per-face Textures:** A full block with a "Front" face texture will automatically rotate to face the player when placed.
- **Normal full blocks:** Even a plain red block will technically track rotation, it just won't look any different.

### Decisions & Impact

| # | Decision |
|---|---|
| DP1 | **Horizontal Facing + Halves (Vanilla-Parity):** We are adding `Properties.HORIZONTAL_FACING` (4 states) and `Properties.BLOCK_HALF` (2 states) to `SlotBlock`. Combined with the existing 16 light levels, this gives each custom block 128 possible states. This is standard for Minecraft (redstone wire has 1,296) and is completely safe for performance. We are skipping vertical facing (Up/Down) to keep it sane. |
| DP2 | **Dynamic Hitboxes:** The hardcoded hitboxes in `BlockShapes.java` will be rewritten to read the block's `FACING` and `HALF` state. If a stair block is facing East, its invisible physical steps will rotate East so players don't bump into air. |
| DP3 | **Resource Pack Rotation:** `ServerPackGenerator` (in Group 05) will be updated. Instead of generating 1 static model state per block, it will generate 4 rotational states (using `"y": 90`, `"y": 180`, etc.) based on the block's `FACING`. |
| DP4 | **The "Old World" Migration Rule:** This introduces a hard disruption for existing servers. Because old custom blocks didn't have a `FACING` property, when this update drops, Minecraft will default them to North. **Owner confirmed: This disruption is accepted.** It's worth it for the feature, and players will just need to break and replace any backwards stairs in their builds. |
| DP5 | **Clean up Arabic (Bonus):** Currently, Arabic letters (Group 13) use a complex invisible workaround to face the right way because `SlotBlock` didn't support rotation. By giving all blocks native rotation, we can eventually delete the Arabic workaround and make the code much cleaner. |
| DP6 | **Painted faces rotate with the block (confirmed 2026-07-11):** per-face textures are "sticker-glued" — a face painted while the block was one way keeps showing on the same relative side after the block is rotated (e.g. a TV-screen face always faces the direction you place the block, matching G13's existing "faces the player" behavior for letters). `ServerPackGenerator`'s 4 rotational model states (DP3) must remap which texture variable each face uses per rotation, not just rotate geometry — not yet designed, needs a build pass when this group is picked up. |

**Status:** idea, brainstormed. Build alongside / after FaceGuide.
