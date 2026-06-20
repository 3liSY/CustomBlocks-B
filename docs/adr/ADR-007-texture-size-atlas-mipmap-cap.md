# ADR-007: Cap texture size at 256px (power of two) to keep the block atlas mipmapped

Date: 2026-06-19
Status: Accepted

## Context

Custom block textures (static and animated) are stitched into Minecraft's **single block
atlas** (`minecraft:textures/atlas/blocks.png`). The mod let the owner pick a texture size and
allowed up to **512px**. With 512px selected, blocks (and especially animated ones) looked
**"muffled"** — soft and aliased — in the world, hand, and inventory.

We first misdiagnosed this as the scale **filter** (bicubic softening pixel-art) and built a
per-block Sharp/Smooth "Style" toggle (Group 14 "Phase 1"). It did not fix the real problem.

The real cause, confirmed from the in-game atlas log (`Created: 16384x8192x0` — the trailing `0`
is the mip-level count, i.e. **mipmaps OFF**):

- The atlas has a hard size limit (MC's `GL_MAX_TEXTURE_SIZE`, **16384px**).
- An animated block is **one** sprite that is a vertical frame-strip: `size × (size·frameCount)`.
  At 512px a 44-frame clip is `512 × 22528` — far past the 16384 limit.
- When a sprite (or the whole stitched atlas) exceeds the limit, Minecraft **downscales the atlas
  and disables mipmaps for every block**. Without mipmaps, every minified (distant) block aliases
  into shimmering noise — the "muffled" look, on *all* blocks, not just the big one.

The old project never hit this: it hard-capped texture size at **256** and enforced power-of-two
sizes, keeping the atlas within budget and mipmapped. We confirmed the same in-game: at 256px the
blocks are crisp everywhere.

## Decision

1. **Cap `textureSize` at 256px** (`CustomBlocksConfig.MAX_TEXTURE_SIZE = 256`). The size picker
   (command range, chest GUI) only offers 16 / 32 / 64 / 128 / 256. 512 is removed.
2. **Snap any requested size to a power of two** via `CustomBlocksConfig.sanitizeTextureSize` —
   applied on config load *and* on `/cb config texturesize <px>`. Non-power-of-two sizes can break
   mipmap generation even under the cap, so all sizes are floored to a power of two (16..256).
3. **Bound the animated strip height** in `AnimationDecoder` (`MAX_STRIP_PX = 8192`). A clip long
   enough that `size·frameCount` would exceed the budget is rendered at a smaller per-frame size
   (`atlasSafeSize`, the largest power of two that still fits), so its strip can never overflow the
   atlas. 8192 = half the 16384 atlas max, leaving room for every other block sprite.
4. **Revert the Sharp/Smooth "Style" toggle.** It was built to fix the muffling, which it did not;
   the cap is the fix. `AnimData.sharp`, its persistence, the `/cb anim … style` command, and the
   `decode(raw, size, sharp)` overload are removed. The `/cb anim` card's Smooth On/Off row (which
   toggles `interpolate`, the genuine frame-blend control) is restored.

## Rationale

- The cap addresses the **measured** cause (atlas overflow → mipmaps off), not a guessed one.
- Power-of-two is the standard requirement for clean mipmap chains; enforcing it removes a second,
  subtler way the atlas can lose mipmapping.
- Reducing *per-frame size* for very long clips (rather than dropping frames) keeps motion smooth
  and only the rare long clip loses resolution — short clips (≤ 32 frames at 256px) are untouched.
- A per-block filter toggle is the wrong layer for an atlas-wide problem and added user-facing
  jargon for no real benefit once the cap is in place.

## Consequences

- **256px is the maximum.** Blocks cannot be sharper than 256px per face. In Minecraft's atlas this
  is already the practical ceiling; going higher trades *every* block's distance clarity for one
  block's close-up detail — a bad trade.
- **Existing 512px blocks stay soft until re-created/retextured** at 256. Statics: `/cb retexture`;
  animated blocks must be re-created (animated slots are excluded from `retexture-all`).
- **Very long animations auto-reduce per-frame resolution** (e.g. a 44-frame clip renders at 128px).
  The player gets a one-line chat note when this happens.
- A future Sharp/Smooth (nearest vs bicubic) choice can return as a proper option inside the Group 14
  **Phase 2 Animation tab GUI** if desired — but as a content-quality preference, not a muffling fix.
