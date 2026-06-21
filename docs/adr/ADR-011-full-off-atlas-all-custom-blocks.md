# ADR-011: Full off-atlas rendering for ALL custom blocks ("no atlas, forever")

Date: 2026-06-20
Status: Accepted (plan agreed with the owner; no code written yet)

> Supersedes the parts of **ADR-008** that kept the atlas as a universal layer and rejected a full own-texture
> path. Builds on ADR-008 §2 (the 512px `NativeImageBackedTexture` mechanism) and its Context (the engine
> facts about the atlas + mipmaps still hold).

## Context

ADR-008 chose a **Hybrid**: keep Minecraft's shared block atlas as the universal layer (so blocks animate
everywhere, including the 3D hand/inventory icon) and add an own-texture `BlockEntityRenderer` only for the
placed world block. It explicitly **rejected** a full own-texture path for the inventory icon ("a 16px icon
looks identical").

Two things forced a re-decision:

1. **The half-built off-atlas pass regressed the world (code-confirmed).** Static + animated off-atlas blocks
   emit an INVISIBLE pack model (no `"parent"`), so the **only** thing that can draw them is `AnimSlotBER`,
   which draws **only blocks that carry a client `AnimSlotBlockEntity`**. Blocks placed by an older jar have
   no saved BlockEntity → nothing draws them → **fully invisible** (singleplayer + server). Backgrounds went
   **see-through** because the draw uses an alpha-tested cutout layer (`getEntityCutoutNoCull`) where the old
   atlas cube was solid (alpha → black). And the **GIF "muffle" survived** because an animated block's
   hand/inventory ICON still goes through the atlas (`cubeAllJson`).

2. **The owner is done with the atlas.** The atlas's mipmap pre-blur is the muffle; no atlas size escapes it
   (ADR-007). A web search (2026-06-20) confirmed there is **no replacement product**: the off-atlas
   own-texture renderer (`NativeImageBackedTexture` + `BlockEntityRenderer`/`DynamicItemRenderer`) **is** the
   technique every image / picture-frame mod uses (OnlinePictureFrame, ImageFrame, Minecraft maps). The idea
   was right; the implementation was left half-finished.

**Owner's success bar (for now):** (1) every placed block VISIBLE again, (2) the GIF muffle GONE for good.
The owner is not actively using the blocks right now — they just want to see them and end the muffle.

## Decision

**Full off-atlas for ALL custom blocks. Nothing of ours touches the atlas for its visual** — static and
animated, **placed world block AND hand/inventory/creative icon.** The atlas hybrid + LOD fallback of ADR-008
is dropped.

Locked parameters:

- **512px** stored cap (owner's choice).
- **Mipmaps OFF** — this is what permanently removes the muffle. (Non-negotiable for the owner's goal.)
- **Smooth / linear sampling** — the owner's images are mostly smooth high-quality photos; at 512px,
  pixel-art / logos still read crisp.
- **Background: black by default**, with a **`transparent` toggle** exposed in **`/cb config` AND the config
  GUI**. (The off-atlas draw must use an opaque path so transparent image areas render black by default.)
- **Keep the auto-join Arabic architecture as-is** (do not convert it to slots).

**Sharpness tradeoff — Option A now, Option B later:**

- **Option A (chosen first):** mipmaps off everywhere. Razor-sharp up close; fine photo detail can "sparkle"
  slightly at DISTANCE. Simplest, safest, guaranteed no muffle.
- **Option B (deferred polish, no deadline):** generate full-res down-scaled mip levels **from the 512px
  source image** so distant blocks smooth out. This is **NOT** the old atlas muffle — that came from the
  atlas pre-shrinking the image to a tiny tile before mipmapping; here the base stays full-res. Built only if
  the Option-A distance sparkle bothers the owner in-game.

## Build order (each step tested in-game before the next; nothing ✅ until the owner confirms)

1. **Every placed block visible again, off-atlas.** Ensure old + new placed blocks all draw via the off-atlas
   renderer (close the missing-`AnimSlotBlockEntity` gap that made old blocks invisible). Un-breaks the world
   without returning to the atlas.
2. **Black backgrounds + transparent toggle.** Opaque draw (black default); add `transparent` to `/cb config`
   and the config GUI.
3. **Kill the GIF muffle for good.** Route the animated block's hand/inventory ICON through the same
   off-atlas renderer (the last thing still on the atlas).
4. **Sharpness/quality pass.** Lock 512px + linear + mipmaps-off (Option A). Option B only on request.

Shaped (slab/stairs/cross) and per-face painted blocks staying on the atlas = a small optional follow-up,
later — they render correctly, just not yet off-atlas.

## Rationale

- The owner explicitly rejects the atlas; the hybrid kept it as the universal layer, so it can't meet the
  "no muffle anywhere" bar. Routing the icon off-atlas too is the only way the muffle fully dies.
- It's not a new mechanism — it extends ADR-008 §2's proven 512px own-texture path to the icon, plus a fix so
  old placed blocks aren't invisible. Lower conceptual risk than it looks; the risk is the BlockEntity-gap
  fix, which Step 1 isolates and tests on its own.
- Small, ordered, in-game-verified steps match the project's discipline and the owner's hard-won caution
  (5 prior failed attempts).

## Consequences

- **Per-block world + icon render cost** instead of shared atlas batching. Acceptable: the owner isn't placing
  many right now; Auto-perf (Group 14 Phase 5) can govern it later if needed.
- **Old placed blocks must be healed** so they all get the draw hook — the crux of Step 1.
- **ADR-008's LOD fallback / "icon stays on atlas" is void.** If perf ever demands an atlas fallback, that's a
  fresh decision.
- **ADR-007 (atlas ≤256px cap)** still bounds anything that *does* stay on the atlas (shaped / per-face until
  the follow-up), but is irrelevant to the off-atlas path, which is full-res 512px.
