# Group 08 - Shape System & Per-Face Textures

## Status

| | |
| --- | --- |
| **Verdict** | All built shape/per-face texture features confirmed in-game, including in-hand/inventory shaped icons. Vanilla block-behavior parity remains parked pending a design session. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-22 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| J | Directional stair placement (facing/half/corners) | Done ✅ | ✏️ |
| A | Shape commands, live hitboxes, and persistence | Done ✅ | - |
| B | Reload-free shape draw | Done ✅ | - |
| D | Full-cube per-face texture commands | Done ✅ | - |
| E | Per-face textures on non-full shapes | Done ✅ | - |
| G | Base shape preview | Done ✅ | - |

**Moved out of this group:**
- §C Shape editor through Block Studio → [Testing_Guide_27.md](Testing_Guide_27.md) §H.
- §F Advanced face editor (live cube selection) → G27. The face-rotation data/commands are built and passing here; only the live-cube screen remains.
- §K Custom Shape Sculptor → parked 💤 (see Archive), revisit as its own design session.

**Original Group:** [GROUP_08_SHAPES.md](../groups/GROUP_08_SHAPES.md)

---

# Active Tests

*No open test rows — §L Vanilla block-behavior parity is parked pending a design session (see Archive).*

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §G Base shape preview — ✅ `2026-06-13`
- §A Shape commands, live hitboxes, and persistence — ✅ `2026-07-20`
- §D Full-cube per-face texture commands — ✅ `2026-07-20`
- §E Per-face textures on non-full shapes — ✅ `2026-07-21`
- §J Directional stair placement (facing/half/corners) — ✅ `2026-07-22`
- §B Reload-free shape draw (incl. in-hand/inventory shaped icons) — ✅ `2026-07-23`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §B/§J block-state shape jar — 💔 `2026-07-20`: crashed the game on boot (block-state count OOM). Reverted to the data-driven shape design. Root cause: `PROGRESS_LOG.md`.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §L Vanilla block-behavior parity — 💤 ✏️ `2026-07-22`: non-stair shapes get no placement rotation or neighbour-connect ([`isDirectional`](../../src/main/java/com/customblocks/block/BlockShapes.java) is stairs-only). Owner wants full vanilla parity — slab→full merge, wall/pane/fence connect, directional placement for all shapes, fence gates, trap/doors, waterlogging, redstone/pressure. Large multi-shape build; needs a design session before scoping. Design detail: G08 group doc.
- §J hotbar/inventory icon look — 💤 ✏️ `2026-07-23`: passing functionally (B6/J confirmed), but owner wants to revisit how the shaped icon looks in the hotbar later.
- Textured shape preview `/cb shapepreview <shape> [id]` — 💤 `2026-07-11`: base preview works; textured id preview deferred.
- §K Custom Shape Sculptor — 💤 `2026-07-20`: idea-stage; needs a bounded geometry/collision/performance design. Revisit as its own session after §B/§J land.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Standalone ShapeEditorScreen ownership — 👎 `2026-07-12`: folded into G27 Block Studio.
- `customtriangle` and `trianglemode` in G08 — 👎 `2026-07-11`: moved to G06 (recolor tools).
- §C Shape editor through Block Studio — 👎 `2026-07-20`: ownership moves to G27 §H.
- §F Advanced face editor — 👎 `2026-07-20`: data/commands built + passing in G08; live-cube screen moves to G27.
- §B block-state `/cb setshape` — 👎 `2026-07-20`: reverted after the boot OOM; replaced by the data-driven design above.
- §J block-state directional placement — 👎 `2026-07-20`: reverted after the boot OOM; replaced by the BlockEntity design above.
- §H FaceGuide helper (`/cb faceguide`) — 👎 `2026-07-21`: removed entirely at owner request.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g08a` and any face-texture test blocks after verification.
- [ ] Keep editor-screen defects linked to G27 while command/logic defects stay in G08.
- [ ] Do not move `customtriangle` or `trianglemode` back into shape scope.
- [x] §D dup command (done 2026-07-20): removed the `paintface` literal; `setface` is the only spec name.

</details>
