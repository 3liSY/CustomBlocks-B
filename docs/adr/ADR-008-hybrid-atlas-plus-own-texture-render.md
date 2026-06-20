# ADR-008: Hybrid rendering — atlas everywhere + own-texture renderer for the world close-up

Date: 2026-06-19
Status: Accepted

> Builds on **ADR-007** (which still holds for the atlas layer). Decided with the owner on 2026-06-19
> after reviewing in-game-style mockups (`cb_mockups/1_quality_current_vs_pathB.png`,
> `cb_mockups/2_hybrid_vs_full.png`).

## Context

ADR-007 capped texture size at 256px to keep Minecraft's shared block atlas mipmapped. That stopped the
"muffle" but only **traded it for pixelation**: an animated block is one atlas sprite — a vertical strip
`size × (size·frames)` — and `AnimationDecoder.atlasSafeSize` keeps frames by **shrinking per-frame size**.
A 256-frame clip is forced to **32px/frame** (32·256 = 8192 = `MAX_STRIP_PX`). Result: the blocky garbage
the owner reported (matches their screenshot).

Two facts pinned the design:
- **No atlas size wins.** 256px = blocky up close; 512px = atlas overflow → mipmaps off → *every* block
  muffles. The old `CustomBlocks/` mod hit the same wall and also hard-capped at 256 (`MAX_SIZE = 256`) —
  there is no hidden trick to recycle.
- **The own-texture renderer (Path B) is the real crisp fix** — a block drawn from its own
  `NativeImageBackedTexture` (mipmaps off, per-block filter), like a Minecraft map. It was **prototyped and
  proven** as the `screen_test` block (`ScreenTestBlockEntityRenderer`), then shelved. Its hard limit: a
  `BlockEntityRenderer` draws in the **world only** — there is no block entity in a hand/inventory slot.

Owner requirement: **highest quality for all, AND the block keeps animating everywhere as a normal 3D block
item** (hand / hotbar / creative tab / `/cb list`), not a flat 2D icon.

## Decision

**Hybrid, with LOD fallback.**

1. **Atlas/mcmeta path stays the universal layer.** Every animated block keeps its `cube_all` model + a
   regenerated `.mcmeta` strip, so it animates **everywhere** — including a normal **3D block icon** in the
   hand, hotbar, creative tab and `/cb list` (Minecraft renders block items as a 3D cube automatically; no
   2D, no custom item renderer).
2. **Add a per-block own-texture renderer for the placed world block**, reusing the proven `screen_test`
   mechanism: `NativeImageBackedTexture`, mipmaps off, per-block nearest/linear filter, **up to 512px**.
   This layer is what makes the close-up crisp.
3. **LOD fallback.** Far away, off-screen, or when perf is tight, a block shows the **atlas** appearance and
   skips the own-texture draw. Ties into Auto-perf (Group 14 Phase 5). Graceful, never a hard cliff.
4. **Interim atlas-quality fix (ships before the renderer):** invert `AnimationDecoder`'s budget — keep
   per-frame resolution high (atlas-safe 256px) and **sample FRAMES down to fit** `MAX_STRIP_PX`, instead of
   crushing resolution to 32px. Stops the worst pixelation on the atlas layer immediately, and improves the
   inventory icon too.
5. **Full Path B (own-renderer for inventory icons too) is rejected.** A 16px icon looks identical to the
   atlas one, and it would need a custom item renderer for ~1028 slot items — large cost, zero visible gain.

## Rationale

- Delivers crisp **and** animates-everywhere with the least code and risk, by letting each layer do what it
  is already good at (atlas = cheap/shared/everywhere; own-texture = full-res/world).
- Reuses a renderer that is already written and proven (`screen_test`).
- Degrades gracefully under load instead of lagging — the world-only cost is exactly what Auto-perf governs.
- Honest about the engine limit (BER is world-only) rather than fighting it.

## Consequences

- **Two representations from one stored source** (atlas strip + own-texture frames) must stay in sync —
  managed at pack-build + in the texture cache; the source GIF is already stored once (`saveSource`).
- **Per-block world cost** for the own-texture layer → governed by Auto-perf (Phase 5); built in small steps.
- **512px enabled for the own-texture layer only.** The **atlas layer stays ≤256px** — ADR-007 still holds;
  `MAX_TEXTURE_SIZE` and `sanitizeTextureSize` keep the atlas safe.
- **Inventory/hand/creative/list icons are atlas-resolution** (fine at ~16px on screen).
- `screen_test` is **un-shelved** as the basis of the world renderer (it was marked "dropped" in an earlier
  session; this ADR revives it for the production path).
