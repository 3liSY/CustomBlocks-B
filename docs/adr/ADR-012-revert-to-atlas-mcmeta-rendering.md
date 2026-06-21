# ADR-012 — Revert custom blocks to atlas + `.mcmeta` rendering (delete the off-atlas renderer)

- **Status:** Accepted 2026-06-21 (owner decision). **Supersedes ADR-011** (full off-atlas) and the
  hybrid plan in ADR-008. ADR-007 (atlas muffles above 256px) stands and is the reason GIFs cap at 256px.
- **Owner:** non-programmer; has spent months on this and lost trust to repeated "looks done, ships broken."
- **Decided after:** ~10 failed attempts to make the off-atlas renderer crisp. Owner: *"i give up, this is
  never gonna be fixed."* Then chose this revert explicitly.

## Context

Group 14 tried to render placed custom blocks **off the block atlas** — a custom `BlockEntityRenderer`
(`AnimSlotBER`) + `NativeImageBackedTexture` with **mipmaps OFF** — to escape the atlas "muffle."

It never looked right. The placed nyan/nyan1 GIF block stayed **speckled/muffled** through every fix
(black bg, transparent toggle, linear filter, frame timing). Root cause, found 2026-06-21 by tracing
**both** mods end to end:

- **A texture with mipmaps OFF, minified to block size on screen, aliases — that *is* the speckle.**
  Removing mipmaps to dodge the atlas's down-scale traded one artifact (soft) for a worse one (sparkle).
- The **old working mod** (`CustomBlocks/`) had **no custom renderer at all** — verified: zero
  `BlockEntityRenderer`, zero `setFilter`. It rendered GIF blocks the plain vanilla way: a `cube_all`
  block model + a frame-strip PNG + `slot_N.png.mcmeta`, textures kept small (default 128, **max 256**).
  It looked fine because **the atlas builds mipmaps for free** — the exact thing the off-atlas path dropped.
- In *this* mod, the **atlas-animated path was already owner-confirmed working** (Testing Guide §2,
  2026-06-19). Phase 1b/1c regressed it. So this is a return to a confirmed-good state, not a gamble.
- An **animated** sprite costs only **one frame** of atlas space (vanilla uploads sub-frames from the
  `.mcmeta`), so at 256px GIFs do **not** overflow the atlas. ADR-007's overflow concern is about large
  **static** sprites, which is why static also stays modest.

## Decision

**Delete the off-atlas renderer. Render every custom block through the vanilla block atlas + `.mcmeta`,
the way the old mod did and the way this mod already had confirmed working.**

- **GIFs (animated): 256px**, atlas + `.mcmeta` (reuse the existing `AnimationDecoder.ATLAS_MAX_SIZE`).
- **Static blocks:** atlas `cube_all`, at `CustomBlocksConfig.textureSize` (default 256; ADR-007 cap 256
  for crispness — 512 is no longer special now that the off-atlas path is gone).
- **Background:** opaque (black), same as the old mod. The `transparent` toggle is **removed** — it only
  ever controlled the off-atlas draw and has no meaning on a solid atlas cube.
- **Block entity stays for now.** `SlotBlock implements BlockEntityProvider` → `AnimSlotBlockEntity` is
  harmless on the atlas path (the block draws from its blockstate→model regardless). Removing it touches
  block registration and risks existing saved worlds, so it is an **optional later cleanup**, not part of
  this revert.

## Consequences

- **Positive:** returns to a known-good, owner-confirmed render path; deletes ~7 fragile client classes
  and a whole sync feature; GIFs animate everywhere (world/hand/inventory/creative) from one pack file;
  no per-frame GPU upload, no client BE-backfill, no distance sparkle.
- **Trade-off:** caps practical resolution (atlas mipmaps soften very-high-res art — ADR-007). Accepted:
  the owner prefers "looks right and proven" over "max res but broken." If a *static* block looks soft at
  512, lower it to 256 (`/cb config texturesize 256`).
- **Lost feature:** the `transparent` background toggle. Transparent-area GIFs render on the solid cube as
  the baked (black) background, same as the old mod. Re-adding atlas transparency later would need a cutout
  `BlockRenderLayerMap` entry — out of scope, not requested.

## Execution

See **`docs/Finale Fix/Reports/GROUP_14_ATLAS_REVERT_HANDOFF.md`** for the exact file-by-file plan
(delete list, pack-generator flip, registration removals, build-green slices, in-game test checklist).
