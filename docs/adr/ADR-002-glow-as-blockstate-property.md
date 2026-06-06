# ADR-002: Store block glow in a BlockState property, not a luminance lambda

Date: 2026-06-04
Status: Accepted

## Context
Blocks need configurable light emission (FR-04-1, 0–15) that can change at runtime via
`/cb setglow`. The obvious approach — `AbstractBlock.Settings.luminance(state -> liveValue)`
reading the slot's current glow — does **not** work.

Verified against the Minecraft 1.21.1 bytecode: `AbstractBlock.AbstractBlockState` has a
`private final int luminance` field that is computed **once** in the state's constructor
(`settings.luminance.applyAsInt(state)`), and `getLuminance()` simply returns that field.
Block states are constructed at **registration**, which happens before any slot data is
loaded or any glow is set — so the lambda is sampled once, returns 0, and is frozen at 0
forever. Mutating `SlotData` later has no effect on the cached value.

(The old project shipped exactly this dead lambda plus a `triggerGlowUpdate` that scanned
`chunk.getBlockEntityPositions()`. `SlotBlock` has no block entity, so that scan matched
nothing — its glow was effectively broken too.)

## Decision
Model light as a real **BlockState property**: `SlotBlock.LIGHT = IntProperty.of("light", 0, 15)`.
- `luminance(state -> state.get(LIGHT))` bakes each of the 16 states to its own correct value.
- `getPlacementState` applies the slot's configured glow (`SlotData.glow`) to new placements.
- `SlotData.glow` persists the configured default; the placed block's light is saved per-position
  in the chunk (so it survives reloads independently of slot data).
- `block/SlotLighting` rewrites the `LIGHT` state of already-placed instances near players when
  `setglow` runs (bounded scan: loaded chunks within a small radius, non-empty sections only).

## Rationale
- This is the vanilla-idiomatic approach (`LightBlock` uses a `LEVEL` IntProperty the same way).
- Luminance is correct because it is baked **per state** from immutable property data, not from
  external mutable state.
- No mixin required, so it works on dedicated servers and all clients.

## Consequences
- Each `SlotBlock` now has 16 states. The resource pack is unaffected: the generator's `""`
  catch-all variant matches every state (verified in the `BlockStatesLoader` bytecode — an empty
  variant key is an empty predicate that matches all states), so no missing-model / purple blocks.
- Changing glow on copies in unloaded chunks doesn't update them until they're revisited; this is
  acceptable because the per-position state is preserved and new placements use the configured glow.
- The same pattern (state property + `getPlacementState`) is the template for future per-block
  attributes that must affect rendering/behaviour live.
