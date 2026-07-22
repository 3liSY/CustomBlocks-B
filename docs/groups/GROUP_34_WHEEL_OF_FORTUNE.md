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

A large **vertical** fortune wheel (~20 blocks across) that faces whoever placed it, built entirely from display entities around a center anchor. A **Minecraft-style arrow** sits at the center pivot. On right-click, the mod draws a fresh ring of **100 item icons** (randomly re-rolled from the full survival pool each spin) over alternating coloured carnival wedges, then spins the arrow ~8s with **real deceleration** and lands it honestly on one visible icon — that icon **is** the winner. A cool center popup (giant slow-spinning 3D icon, glowing name banner, pop-in scale, fireworks) shows the landed item and holds until the next spin. One wheel per server (singleton). Pool is every survival-obtainable vanilla item/block, cached once.

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
| 2026-07-23 | **Each spin re-rolls 100 icons from the full pool, arrow lands honestly on one visible icon** | Per spin: pick 100 random items from the ~1300 pool, draw their icons over alternating coloured wedges, spin arrow, land on ONE real visible icon = winner. Every item can appear (rerolled each spin). Arrow always points at the item it won. Superseded pre-picked-then-fake-animation |
| 2026-07-23 | **Spin feel = ~8s with real deceleration/easing, honest random landing** | Fast loops → coast → settle on a truly random visible slice; no instant stop, no rigged pre-pick. Superseded the fast-cycle-then-instant-stop decision |
| 2026-07-23 | **Icon cap = 100 visible per spin (perf)** | Full 1300 icons would overlap into mush on a 20-block ring and overload a 137-mod server; 100 evenly-spaced icons stay readable and light. All items still reachable via per-spin reroll |
| 2026-07-23 | **Ring look = alternating coloured carnival wedges behind the icons** | Classic fortune-wheel wedge backdrop; icons sit on the wedges |
| 2026-07-23 | **Spin sound = whir loop + peg-clacks that space out as it slows; win sound = bell ding + fanfare + firework + level-up** | Layered carnival audio during the 8s; big multi-layer payoff on land |
| 2026-07-23 | **Win popup = giant slow-spinning 3D icon + glowing name banner + pop-in scale + fireworks ring, holds until next spin** | Center payoff pushed via the NBT display technique; persists so teams keep seeing the target until the next spin overwrites it |

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

Referee right-clicks the wheel (or the arrow); it re-rolls 100 items onto the ring and spins the arrow ~8s to an honest random landing.

**Experience**

- Each spin: pick 100 random items from the full pool, draw their icons over alternating coloured wedges.
- Arrow spins fast then decelerates and settles on one visible icon = the winner.
- Whir + peg-clack audio that spaces out as it slows.

**Requirements**

- `WheelBlockRegistry` tracks the singleton wheel instance.
- `WheelPool` supplies the cached full pool; `WheelRing` picks the per-spin 100 and computes wedge/icon layout.
- Landing slice is chosen truly at random from the 100 visible; the arrow's final angle matches that slice.
- Ignore a right-click while a spin is already running.

**Boundary**

Does not track match score or round history — referee tracks that manually.

### C. Result Display (cool center popup)

**Player outcome**

After the arrow lands, the winning item blows up in the center: giant slow-spinning icon, glowing name banner, pop-in animation, fireworks ring.

**Experience**

- ITEM_DISPLAY giant icon, slow 3D rotation.
- TEXT_DISPLAY name banner with glow under it.
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
- Per-spin ring = 100 items sampled from the cached pool (no duplicates within a spin); wheel/arrow/popup are display entities only.

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
| 2026-07-22 | Reel = single ITEM_DISPLAY icon + TEXT_DISPLAY name floating above hub | 2026-07-23: 100-icon ring + giant animated center popup |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_34.md) · [All Groups](README.md)

- `buzzergame/` package — structural reference for registry + entity/block + commands + NBT display visual.
- `GuessShowcaseBlock`/`GuessShowcaseBlockEntity` — block+entity pattern reference.
- `TimerDisplayVisual.pushText` — NBT-push technique reference for result display.
