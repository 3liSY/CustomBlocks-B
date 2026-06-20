# ADR-005: Render auto-join Arabic words with a BlockEntityRenderer + live in-memory textures, not the resource pack

Date: 2026-06-18
Status: Accepted (proof spike built; pending in-game confirmation of the no-reload path)

## Context
Pass 4 "auto-join" places individual Arabic letter blocks in a row that join into one seamless
connected word. Each placed/broken letter must change the picture on its own block (and re-flow
its neighbours). With the current architecture every block's texture comes from the HTTP resource
pack, so any per-block picture change means:

1. Rebuild the pack ZIP (`ServerPackGenerator.generate`), then
2. on the modded host, `ResourcePackGenerator.regenerate(...)` → **`client.reloadResources()`**
   (`ResourcePackGenerator.java:104`) — a full, multi-second client resource reload (atlas
   re-stitch). Verified in live code.

The vanilla confirm dialog is already auto-suppressed (`ClientCommonNetworkHandlerMixin`
silent-accepts our pack), so the dialog is **not** the blocker. The blocker is the **reload itself**:
doing it on every place/break is unusable, and it is the most likely cause of the dev's recurring
connection resets. The resource-pack route cannot avoid the reload — the block atlas is stitched
globally, with no per-instance update. Confirmed the mod has **no** dynamic-texture or BlockEntity
infrastructure today; all 1028 `SlotBlock`s are pack-textured, and the live GUI preview
(`ArabicPreviewScreen`/`PreviewCube`) is a GUI gizmo of rectangle-fills, not a real world-block path.

## Decision
Render joined-word blocks via a **`BlockEntityRenderer`** that draws the cube faces from a
**live `NativeImageBackedTexture`** registered directly into the `TextureManager` under a dynamic
`Identifier`, **bypassing the resource pack entirely**. This is the same technique vanilla uses for
signs, banners, and player heads. Per-block data (the word, this block's slice/form, colours,
direction) lives in a `BlockEntity` and syncs via the standard block-entity update packet.

Changing a block's picture = build/select a different in-memory texture → **no pack rebuild, no
`reloadResources()`, no dialog, no reset risk, instant.**

A throwaway **proof spike** (`block/ScreenTest*`, `client/render/ScreenTestBlockEntityRenderer`,
ids `customblocks:screen_test`) validates only the mechanism: right-click cycles a per-variant
in-memory bitmap on a real world block with no reload. The real feature replaces the procedural
bitmap with the **sliced `ArabicWordRenderer`** output (the dev-blessed §0 art).

## Rationale
- It is the only path that updates one block's appearance without a global atlas reload.
- It reuses the existing perfect word renderer for the actual pixels — the art stays §0-quality.
- It also makes the feature *better*: instant, per-block independent, no server-wide pack thrash.
- BlockEntity + BER is vanilla-idiomatic and works without touching the Sacred pack/CDN systems
  (Royal Directive §4).

## Consequences
- Introduces the mod's first `BlockEntity` + `BlockEntityRenderer` + client texture cache — a new
  rendering path. Lighting / culling / break-overlay must be handled in the BER (spike uses no-cull
  faces + the passed light; to be refined for the real cube faces + readable back via UV).
- The joined letters become block-entities (a small, bounded count per word) — negligible perf cost
  versus the all-block atlas reload it replaces.
- **Supersedes the ADR-003 `FORM` blockstate plan** and the earlier "slice via pack" idea for the
  *display* mechanism: forms/slices are selected per-block in the BER from live textures, not baked
  pack variants. ADR-003's joining *rules* (which letters connect, RTL, non-connectors) still stand.
- Dynamic textures must be released when a word is broken to avoid leaking GPU textures; the cache is
  keyed so identical (word+colours+slice) tiles are uploaded once and shared.
- This is the template for any future per-block live-texture feature (e.g. animated/GIF blocks in
  the world without a pack reload).

## Amendment — 2026-06-18 (build kickoff decisions)
- **A new dedicated joinable letter block** carries the BlockEntity (id distinct from the 1028
  `SlotBlock`s, like the `screen_test` spike graduated). The existing bundled letter `SlotBlock`s are
  left untouched (static hand-art); auto-join words are placed as this new block from the Arabic
  tool/word flow. This **overrides ADR-003 §1's "no new block class"** — chosen for zero blast radius
  on the pre-registered slots and a clean BlockEntity home for form/facing/colour/word data.
- **Seam colour = each tile keeps its own background colour.** Where two joined letters differ, the
  connecting bar meets at the tile edge (left half = letter A's colour, right half = B's); no
  cross-block colour blending. Matches the per-letter-tile lock and avoids neighbour colour coupling.
- **`FORM` (0 isolated · 1 initial · 2 medial · 3 final) + `FACING`/axis live on the BlockEntity**, not
  a blockstate property — the BER selects the form's live texture per block. The bounded place/break
  re-flow updater stays a sibling of `SlotLighting` (≤2 neighbours, no world scan).
- **Readable back is DEFERRED** to a post-Pass-4 toggle; this build ships the single readable front.
