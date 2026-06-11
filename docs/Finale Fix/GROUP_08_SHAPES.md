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
