# Group 34 - Wheel of Fortune

> Owns a referee-run fortune-wheel spectacle block for team-race YouTube content.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_34.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Content creator wants a fortune-wheel block for live videos: two teams race to obtain whatever item or block the wheel lands on, referee spins and calls the round, best of 5 (score tracked manually, not by the mod). This Group owns the standalone block, its spin logic, result display, and commands. It is a spectacle system, not a data-editing tool.

## Ownership

| Owns | Does not own |
| --- | --- |
| Wheel block + block entity, center-arrow spin interaction, spin/result logic, item pool caching, ring/arrow/popup display entities, `/cb wheel` commands | CB slot/texture editing (owned by the `gui/chest/*Menu.java` system) — wheel is not user-recolorable |
| `wheel/` package: `WheelBlockRegistry`, `WheelBlock`, `WheelBlockEntity`, `WheelPool`, `WheelRing`, `WheelDisplayVisual`, `WheelFx`, `WheelCommands` | Score tracking across rounds — referee tracks manually, mod does not persist match state |

## Direction

A large **vertical** fortune wheel (~20 blocks across) that faces whoever placed it, built entirely from display entities around a center anchor. A **Minecraft-style arrow** sits at the center pivot. On right-click, the mod draws a fresh ring of **50 item icons** (randomly re-rolled from the full survival pool each spin) over **50 tapered pie wedges, each a unique fully-saturated colour meeting at the hub**, then spins the arrow ~8s with **real deceleration** and lands it honestly on one visible icon — that icon **is** the winner. The landed prize floats **above the wheel's rim**, with its name banner directly above it. One wheel per server (singleton). Pool is every survival-obtainable vanilla item/block, cached once.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-22 | Standalone `wheel/` package, not routed through `gui/chest/*Menu.java` | Wheel logic must not depend on CB slot/chest-GUI editing code |
| 2026-07-22 | Block+entity uses plain `BlockWithEntity`/`BlockEntity` pattern (like `GuessShowcaseBlock`), not `SlotBlock`/`CbBlock` | Wheel is not user-recolorable via CB slot system |
| 2026-07-22 | One wheel only, singleton per server | No multi-wheel linking/ID logic needed |
| 2026-07-22 | Pool = survival-obtainable vanilla items/blocks only, auto-pulled from `Registries.ITEM`, cached once (not walked per spin) | Exclusion filter for creative-only/unobtainable items (command blocks, barriers, spawn eggs, etc.), computed once and cached |
| 2026-07-22 | Show-only: the mod never gives the landed item to anyone | Referee reads the result; teams obtain it themselves. No give/target logic |
| 2026-07-22 | No in-mod score / round tracking | Referee tracks match score manually outside the mod |
| 2026-07-23 | **Wheel is VERTICAL, ~20 blocks across, faces the placer** | Upright wheel-of-fortune, flat face turned toward whoever placed the item. Superseded the flat wool disc |
| 2026-07-23 | **Wheel body = display entities, not real blocks** | Ring/wedges/icons/arrow are all display entities around a center anchor. No physical block wall; clean instant removal. Superseded the real-wool-disc decision |
| 2026-07-23 | **Center anchor placed by a wheel item; no visible center block** | `/cb wheel` gives a wheel item; where it is placed = the wheel center; wheel spawns around it; breaking it removes the whole wheel. A Minecraft-style arrow occupies the center pivot |
| 2026-07-23 | **Spin trigger = right-click the wheel or the center arrow** | Either target spins it. (Optional `/cb wheel spin` backup not required for v1) |
| 2026-07-23 | **Each spin re-rolls 50 icons from the full pool, arrow lands honestly on one visible icon** | Per spin: pick 50 random items from the ~1300 pool, draw their icons over the coloured wedges, spin arrow, land on ONE real visible icon = winner. Every item can appear (rerolled each spin). Arrow always points at the item it won. Superseded pre-picked-then-fake-animation |
| 2026-07-23 | **Spin feel = ~8s with real deceleration/easing, honest random landing** | Fast loops → coast → settle on a truly random visible slice; no instant stop, no rigged pre-pick. Superseded the fast-cycle-then-instant-stop decision |
| 2026-07-23 | **Spin sound = whir loop + peg-clacks that space out as it slows; win sound = bell ding + fanfare + firework + level-up** | Layered carnival audio during the 8s; big multi-layer payoff on land |
| 2026-07-23 | **Win popup = giant slow-spinning 3D icon + glowing name banner + pop-in scale + fireworks ring, holds until next spin** | Payoff pushed via the NBT display technique; persists so teams keep seeing the target until the next spin overwrites it |
| 2026-07-23 | **Slice count = 50 everywhere** (wedges and per-spin icons) | Replaced the 100 cap: 50 gives chunky readable wedges and one unique colour each. All items still reachable via the per-spin reroll |
| 2026-07-23 | **Palette = 50 unique full-saturation colours, hues stepped by the golden angle (~137.5°)** | One colour per slice, never repeated; neighbours always land far apart on the colour wheel so no seam ever muddies |
| 2026-07-24 | **Wheel face = ONE baked texture on a single quad, never assembled from quads in game** | The disc is `assets/customblocks/textures/block/wheel_face.png`, generated by `tools/gen_wheel_face.py`, worn by one ITEM_DISPLAY. Every attempt to build the wedges out of rectangles in game stair-stepped, because hiding a rectangle's overhang needs a guaranteed paint order and Minecraft does not give one for translucent display quads. A raster assigns every texel to exactly one slice, so no seam can step, tear or flicker. Superseded both quad builds |
| 2026-07-24 | **The face is double-sided, and its back is MIRRORED** | The back face's UVs are U-flipped (`[16,0,0,16]`). The icon ring's positions are physical, so a viewer behind the wheel sees them mirrored — the face must mirror with them or wedge and icon stop lining up |
| 2026-07-23 | **Prize popup floats ABOVE the wheel rim; its name banner sits directly above the icon, close** | Clears the arrow and the disc entirely (the old 2-blocks-out-front placement still overlapped them), and the name reads as a label on the prize instead of drifting far below it |

## Feature Plan

### A. Wheel Structure (vertical display-entity wheel)

**Player outcome**

Player sees a large vertical fortune wheel (~20 blocks across) facing them, with coloured carnival wedges and a Minecraft-style arrow at the center.

**Experience**

- Placing the wheel item spawns the whole wheel (wedges + arrow) around that point, facing the placer.
- Built from display entities only — no physical block wall.
- Breaking the center removes every display entity cleanly.

**Requirements**

- Block + block entity using plain `BlockWithEntity`/`BlockEntity` pattern; the placed block is the hidden center anchor.
- Wheel faces the placer's yaw at placement time.
- Server-side only guard (`world.isClient` check) on all spin/result logic.

**Boundary**

Does not support user recoloring or CB slot/texture assignment.

### B. Spin Logic + Ring Reroll

**Player outcome**

Referee right-clicks the wheel (or the arrow); it re-rolls 50 items onto the ring and spins the arrow ~8s to an honest random landing.

**Experience**

- Each spin: pick 50 random items from the full pool, draw their icons over the coloured wedges.
- Arrow spins fast then decelerates and settles on one visible icon = the winner.
- Whir + peg-clack audio that spaces out as it slows.

**Requirements**

- `WheelBlockRegistry` tracks the singleton wheel instance.
- `WheelPool` supplies the cached full pool; `WheelRing` picks the per-spin 50 and computes wedge/icon layout.
- Landing slice is chosen truly at random from the 50 visible; the arrow's final angle matches that slice.
- Ignore a right-click while a spin is already running.

**Boundary**

Does not track match score or round history — referee tracks that manually.

### C. Result Display (floating prize popup)

**Player outcome**

After the arrow lands, the winning item blows up above the wheel: giant slow-spinning icon, glowing name banner, pop-in animation, fireworks ring.

**Experience**

- ITEM_DISPLAY giant icon, slow 3D rotation, floating clear above the rim.
- TEXT_DISPLAY name banner with glow, directly above the icon.
- Pop-in scale animation + fireworks/particle ring on land.
- Holds until the next spin overwrites it.

**Requirements**

- `WheelDisplayVisual` drives the popup entities via the NBT-push technique.
- Popup names the exact item the arrow landed on.

**Boundary**

Does not persist the result after the next spin overwrites it; the mod never gives the item.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| - | - | - |

## Technical Contract

- Reuse `buzzergame/` package shape: own registry, block+entity, command handler, NBT-driven display visual class.
- Block+entity pattern follows `GuessShowcaseBlock`/`GuessShowcaseBlockEntity`, not the `SlotBlock`/`CbBlock` system.
- All spin/result state changes guarded by `world.isClient` server-side check.
- Item pool computed once from `Registries.ITEM` with a survival-obtainable filter, cached — never walked per spin.
- Per-spin ring = 50 items sampled from the cached pool (no duplicates within a spin); wheel/arrow/popup are display entities only.
- The face is one ITEM_DISPLAY of `customblocks:wheel_face`, a render-only item (same pattern as the G31 stand parts) whose model is a flat double-sided quad. `item_display: "none"` renders the raw model, and `ItemRenderer.renderItem` applies `translate(-0.5, -0.5, -0.5)`, so the model's `[0,16]` box maps to `[-0.5,+0.5]` blocks — the disc comes out centred on the anchor.
- Texture and Java must agree on two numbers: `FACE_TEX_PX` (1024) and `FACE_MARGIN_PX` (8), duplicated in `tools/gen_wheel_face.py`. The disc's baked radius is `TEX/2 - MARGIN`, and `FACE_SCALE = 2*OUTER_R * TEX / (TEX - 2*MARGIN)` inverts exactly that to land the painted rim on `OUTER_R`. Change one, change the other, re-run the generator.
- The generator paints a slice with `floor(angle / SLICE_DEG + 0.5) mod SLICES` — the same rule `WheelRing.sliceAt` uses to name the winner — so the colour under the arrow is the slice the code awards, by construction. It colours pixels OUTSIDE the disc too and varies only alpha, so a half-covered rim texel blends the right colour instead of fading to black.
- Display entities default to `width`/`height` `0`, which sets `ignoreCameraFrustum` — the 20-block face is never frustum-culled despite sitting on a 1-block entity.
- Why not quads: a rectangle cannot be clipped along a ray, so any strip build must rely on the next slice being painted exactly on top of its overhang. Minecraft will not promise that — the strips are translucent TEXT_DISPLAY backgrounds 0.002 blocks apart, and that ordering does not survive translucent rendering. Rejected in game three times (2026-07-24). Do not rebuild the face from quads.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Multi-wheel / linked wheels | Current content need is one wheel only | G34, if a future format needs it |
| In-mod score tracking across rounds | Referee tracks manually by design | G34, if automation is later requested |
| Dedicated carnival-lever model on the rim | v2 uses the center arrow as the spin control | G34 polish |
| `/cb wheel spin` command backup | Right-click is the primary trigger; command is optional | G34, if off-camera control is wanted |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Superseded decision | Replaced by |
| --- | --- | --- |
| 2026-07-22 | Wheel body = real vanilla wool disc, flat, placed by hub and removed on break | 2026-07-23: vertical display-entity wheel, ~20 across, faces placer |
| 2026-07-22 | Lever mounted on the wheel; hub block IS the spin control | 2026-07-23: no center block; Minecraft-style center arrow, right-click wheel/arrow to spin |
| 2026-07-22 | Spin feel: fast cycling then sudden stop, no deceleration; winner picked server-side up front, animation fakes it | 2026-07-23: ~8s real deceleration, honest random landing on a visible icon |
| 2026-07-22 | Reel = single ITEM_DISPLAY icon + TEXT_DISPLAY name floating above hub | 2026-07-23: 50-icon ring + giant animated prize popup |
| 2026-07-23 | Icon cap = 100 visible per spin | 2026-07-23: 50 slices everywhere — chunky readable wedges, one unique colour each |
| 2026-07-23 | Ring look = alternating coloured concrete-block wedges (10-colour cycle) behind the icons | 2026-07-23: 50 unique full-saturation TEXT_DISPLAY colour quads, tapered into true pie sectors meeting at the hub |
| 2026-07-23 | Wedges = TEXT_DISPLAY colour quads, tapered radial bands per slice | 2026-07-24: one baked texture on a single quad — bands left a stair-step at every band boundary |
| 2026-07-24 | Wedges = per-slice strip stacks, each slice's overhang sheared off by the next slice's exact edge | 2026-07-24: one baked texture on a single quad — the shear needs a paint order Minecraft does not guarantee for translucent quads |
| 2026-07-23 | Prize popup floats 2 blocks out in front of the hub, name banner below it | 2026-07-23: popup rides above the wheel rim, name banner directly above the icon |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_34.md) · [All Groups](README.md)

- `buzzergame/` package — structural reference for registry + entity/block + commands + NBT display visual.
- `GuessShowcaseBlock`/`GuessShowcaseBlockEntity` — block+entity pattern reference.
- `TimerDisplayVisual.pushText` — NBT-push technique reference for result display.
