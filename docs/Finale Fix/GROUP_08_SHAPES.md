# Group 08 — Shape System & Per-Face Textures

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 6 (Attributes) build-verified.
>
> **Objective:** Restore the full block shape system (slab, thin, carpet, wall, pane, stairs, cross, etc.) and per-face texture support (different image per face). Both must be accessible via commands and chest GUI. The implementation must be clean — no junk or leftover broken behavior from the old version.
>
> **Source issues:** Group B (setshape, addshape, removeshape, clearshape, shapeeditor, shapelist, shapepreview, facechangegui, setface, clearface, clearallfaces, bulkshape, customtriangle, trianglemode)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Block shapes | slab, thin, carpet, wall, pane, stairs, cross, full | Full block only | All shapes restored |
| Shape editor GUI | `/cb shapeeditor <id>` opened chest GUI | Present in Phase 10 screen version | Chest GUI version |
| Shape list | `/cb shapelist` | Missing | Restored |
| Shape preview | `/cb shapepreview <shape>` — shows shape in world | Missing | Restored |
| Per-face textures | Different image on each face (top/bottom/north/south/east/west) | Single texture on all faces | Restored |
| Face editor GUI | `/cb facechangegui <id>` | Missing | Restored |
| Set face | `/cb setface <id> <face> <url>` | Missing | Restored |
| Clear face | `/cb clearface <id> <face>` | Missing | Restored |
| Clear all faces | `/cb clearallfaces <id>` | Missing | Restored |
| Custom triangle | `/cb customtriangle` — create triangular shaped block | Missing | Restored |
| Triangle mode | `/cb trianglemode` — tool mode for placing triangles | Missing | Restored |
| Bulk shape | `/cb bulkshape <filter> <shape>` | Missing | Restored (depends on Group 07) |

---

## What this group covers

| Feature | Commands |
|---|---|
| Set shape | `/cb setshape <id> <shape>` |
| Add shape | `/cb addshape <id> <shape>` |
| Remove shape | `/cb removeshape <id> <shape>` |
| Clear shape | `/cb clearshape <id>` |
| Shape editor | `/cb shapeeditor <id>` |
| Shape list | `/cb shapelist` |
| Shape preview | `/cb shapepreview <shape>` |
| Set face texture | `/cb setface <id> <face> <url>` |
| Clear face texture | `/cb clearface <id> <face>` |
| Clear all faces | `/cb clearallfaces <id>` |
| Face editor GUI | `/cb facechangegui <id>` |
| Custom triangle | `/cb customtriangle <id> <name>` |
| Triangle mode | `/cb trianglemode` |

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

Each block can have a different texture on each of 6 faces: `top`, `bottom`, `north`, `south`, `east`, `west`.

- If a face has no specific texture, it uses the block's default texture.
- Face textures stored alongside the main texture in `config/customblocks/textures/slot_N_face_top.png` etc.
- Face texture data stored in `SlotData`.

### 4. Shape Editor Chest GUI

`/cb shapeeditor <id>` opens a chest GUI with:
- Shape selector slots (one per available shape, with visual icon).
- For `custom` shape: bounding box min/max coordinate sliders (6 slots).
- Preview: current shape shown in item tooltip.
- Apply and Cancel slots.

### 5. Face Editor Chest GUI

`/cb facechangegui <id>` opens a chest GUI with:
- 6 face slots representing the block's 6 sides.
- Click a face slot → opens a URL input (anvil GUI).
- Current face texture shown in slot item texture.
- "Clear face" and "Clear all faces" action slots.

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

## Test G08.6 — Shape editor chest GUI

```
/cb shapeeditor g08a
```

**Expected:** Chest GUI opens with shape selector slots. Clicking "stairs" changes the block to stair shape.

**Pass:** GUI opens, shape selection works.
**Fail:** GUI missing, or selections don't apply.

---

## Test G08.7 — Set face texture

```
/cb setface g08a top https://i.imgur.com/top_texture.png
```

**Expected:** Top face of `g08a` gets a different texture. Other faces unchanged. Pack rebuilds.

**Pass:** Top face shows different texture.
**Fail:** Error, or all faces changed.

---

## Test G08.8 — Clear single face

```
/cb clearface g08a top
```

**Expected:** Top face reverts to the block's default texture.

**Pass:** Top face matches default texture.
**Fail:** Face still shows the per-face texture.

---

## Test G08.9 — Set multiple faces

```
/cb setface g08a top https://i.imgur.com/top.png
/cb setface g08a bottom https://i.imgur.com/bottom.png
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

## Test G08.11 — Face editor chest GUI

```
/cb facechangegui g08a
```

**Expected:** Chest GUI with 6 face slots. Clicking a face slot opens an anvil input for a URL. After entering a URL, that face updates.

**Pass:** GUI opens, face URL input works.
**Fail:** GUI missing or face slots don't open URL input.

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

**Status:** idea only. Revisit after Group 08 passes in-game; if pursued, write a proper group spec first.

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

**Status:** agreed direction, not built. Build as a Group 08 slice after the current work; front-end only
where possible (per-face textures + a temp block-swap with restore).

---

## 🎨 PARTIAL — Textured shape preview: `/cb shapepreview <shape> [id]` *(base works; `[id]` deferred)*

> ⚠️ **Status: PARTIAL.** `/cb shapepreview <shape>` (vanilla stand-in) works and passed in-game. The
> optional **`[id]`** argument — preview the shape wearing a custom block's texture with no pack reload —
> is **not built yet**; come back and implement it correctly/perfectly later (developer's call,
> 2026-06-13). Design below.

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

**Status:** idea, brainstormed. Build alongside / after FaceGuide.
