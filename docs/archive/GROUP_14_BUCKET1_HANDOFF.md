# Group 14 — Bucket 1 fix handoff (resume here)

> Written 2026-06-21 mid-session (owner usage running out). This is a **complete cold-start brief** so a
> fresh chat can pick up the remaining Group 14 work without re-deriving anything. Read this top-to-bottom,
> then read the files it points at before writing code.

---

## 0. The one rule that overrides everything

**Nothing is ✅ DONE until the owner runs it in-game and confirms.** Build-green ≠ done. The owner is a
non-programmer, burned by AIs that marked untested work "done." Never commit unless asked. Max 5 items per
plan. See `CLAUDE.md`.

Owner's instruction for this work (verbatim intent): *"fix everything left but one by one and correctly,
then i test all at once."* → Build every Bucket 1 slice, each compiling green, **no incremental in-game
tests** — the owner does **one** in-game test at the very end. So: be conservative, get each slice right,
and be honest in the final report about which slices are logic (high confidence) vs GL render (needs their
eyes — I can't see their game).

Build command: `.\gradlew.bat build` (Temurin JDK 21). Gates: `verifyMojibake`, `verifySound`,
`verifyFileSize` (≤500 `.java`, ≤400 command handler, ≤300 `*Config.java`; `count > limit` fails, so 400
exactly passes).

---

## 1. What is already ✅ owner-confirmed (do NOT redo)

- **ADR-014 Step 1** — `/cb retexture` spot-fix (anim-aware, no garble).
- **ADR-014 Step 2** — real-clock millisecond timing (true source fps, >20fps, freezes on pause).
- **Bonus** — `/cb retexture` undo/redo (`UndoManager.Kind.RETEXTURE`).
- **Owner already migrated old animated blocks** (they re-retextured the old "say" block etc.). Do **not**
  build any auto-migration — explicitly out of scope.

Confirmed in: `PROGRESS_LOG.md`, `Reports/GROUP_14_TESTING_GUIDE.md` §7,
memory `project_group14_atlas_revert.md`.

---

## 2. Scope decided this session: **BUCKET 1 ONLY**

Owner picked "Bucket 1 only" via AskUserQuestion. Bucket 2 (the 8 unbuilt roadmap phases — timeline editor,
video wall, live data blocks, redstone-reactive, ffmpeg import, color-grade/chroma-key, etc.) is **NOT in
scope now.** Do not start any Phase 3–10 feature.

**Bucket 1 = these slices (build all, green each, owner tests once at end):**

| # | Slice | Risk | Notes |
|---|-------|------|-------|
| 1 | **Studio re-skin undo** (`StudioReskin`) | LOW | Mirror the already-built `/cb retexture` undo. Real gap. |
| 2 | **Polish: new-GIF resets anim settings on load** | LOW–MED | GUI logic. |
| 3 | **Polish: animated-block bg can't re-fill alone** (needs re-load) | LOW–MED | GUI/state logic. |
| 4 | **Polish: preview smoothing is frame-swap not cross-fade** | MED | GUI render. |
| 5 | **Step 3 render rewrite** (the big one — see §3) | **HIGH (GL)** | Coupled; delivers S3-1..S3-7. |

Recommended build order: **1 → 2 → 3 → 4 → 5** (safe logic first, GL rewrite last). Keep each green.

---

## 3. Step 3 render rewrite — the critical technical understanding

ADR-014 Step 3 (`docs/adr/ADR-014-animated-block-overhaul.md` §Decisions 3–4) lists these levers:
mipmaps ON, per-frame = block `texturesize` sharpness, texture **pool** (only on-screen ids hold a GPU
texture), **current-frame-only upload** (1 frame resident/id), RAM frames compressed + LRU, **raise the
256-frame cap**, far-block LOD, off-screen pause.

**KEY INSIGHT (do not miss this): these are NOT independent toggles. They are one coupled rewrite.**

Current design (today): each animated slot uploads its **whole frame GRID** as ONE GL texture, **mipmaps
OFF**, and the renderer samples the current cell via a UV rectangle.

Why the levers are coupled:
- **Mipmaps can't go ON with the grid** — mipmap minification averages neighbouring texels, so at distance
  it bleeds *across cell boundaries* (that's exactly why vanilla's atlas muffles big sprites). Half-texel UV
  inset only stops level-0 bleed, not deeper mip levels.
- **Whole-grid upload caps quality + frame count** — the grid is bounded to `GRID_BUDGET_PX = 4096`
  (`AnimationDecoder.java:67`) so it fits GPU max texture size; that's what forces the 256-frame cap and the
  256px/frame `OFFATLAS_MAX_SIZE` shrink (`AnimationDecoder.gridCell`, line 282).
- **The fix that unlocks all of them = current-frame-only upload:** keep the decoded grid in **RAM**
  (`NativeImage`, can be large, never whole-uploaded to GPU), and upload **only the current cell** into a
  small per-slot GPU texture when the frame changes. Once each resident frame is its own small texture:
  - mipmaps are safe (no neighbour cells to bleed) → kills the distance speckle (S3-1),
  - each frame can be full block `texturesize` (no 4096 grid-budget shrink) → sharpness (S3-2),
  - GPU holds 1 frame/slot → VRAM bounded (S3-3),
  - the on-disk grid may exceed 4096 (it's only a CPU `NativeImage` + PNG now) → raise the 256 cap (S3-5),
  - add an eviction pass (slot not rendered in N seconds → free its GPU texture + RAM) → the **pool** + LRU
    (S3-3/S3-4) and **off-screen pause** (S3-7) fall out naturally,
  - far-block LOD (S3-6) = pick a smaller mip / skip upload when far.

So **build current-frame-only upload FIRST inside Step 3**; the rest layer on it. Don't try mipmaps on the
existing grid — it won't look right.

### The one genuinely risky bit: mipmap generation (blind GL)
`NativeImageBackedTexture` does **not** generate mipmaps (uploads level 0 only). `setFilter(_, true)` alone
with no mip levels = incomplete texture = often renders black/white. To get real mipmaps you must allocate
mip levels and upload each. Use the **same proven MC primitives SpriteContents uses**:
- `net.minecraft.client.texture.MipmapHelper.generateMipmaps(NativeImage[] , int mipLevel)` to build the chain,
- `TextureUtil.prepareImage(glId, maxLevel, w, h)` to allocate, then upload each level.
- Verify exact 1.21.1 signatures in the decompiled sources before relying on them.

This is the part I **cannot verify** without the owner's game. The ADR explicitly calls these
"verified techniques only" (MC maps / video playback use per-frame `NativeImageBackedTexture` upload;
mipmaps via `MipmapHelper`). Build it, but flag it clearly in the final report as "compiles only — your
eyes needed."

### File-size gate plan for Step 3
`AnimFrameCache.java` is currently 256 lines and will grow. ADR-014 §Plan says it **splits** to stay under
500. Suggested split: new class `AnimFrameTexture` (per-slot: RAM grid `NativeImage` + the one mipmapped GPU
cell texture + lastFrameIndex + lastRenderedMs + upload-on-frame-change). `AnimFrameCache` becomes the
registry/pool (`get(slot)`, eviction pass). Keep both under 500.

---

## 4. Exact files + anchors (all read this session — current state)

**Render (client) — `src/main/java/com/customblocks/client/render/`:**
- `AnimFrameCache.java` (256 ln) — the rewrite target. Today: `Slot` holds ONE grid texture + order/times in
  **ms**; `currentFrame(nowMs)`; `uv(frame)` returns the cell rect (half-texel inset); `build(n)` reads
  `slot_N.png` + `slot_N.grid.json` (or legacy `.png.mcmeta`, cols=1), composites over black unless
  `OffAtlasBgState.isTransparent()`, registers `NativeImageBackedTexture` with `setFilter(true,false)`
  (mipmaps OFF). `clear()` on reload destroys textures. `CACHE`/`NOT_ANIMATED` maps.
- `AnimSlotBER.java` (108 ln) — placed-block BER. Animated path: `s.uv(s.currentFrame(AnimClock.nowMs()))`
  → `getEntityCutoutNoCull(s.textureId)` → `drawCube(...)`. Static path via `StaticFrameCache`. `drawCube` +
  `face` + `vert` helpers shared with the item renderer. Faces at z=0.999.
- `SlotItemRenderer.java` (62 ln) — item icon, same animated/static logic, reuses `AnimSlotBER.drawCube`.
- `AnimClock.java` — monotonic ms wall-clock (`nanoTime`, freezes on `mc.isPaused()`). `nowMs()`.
- `StaticFrameCache.java` (132 ln) — static off-atlas twin (one full texture, mipmaps OFF). Off-atlas gate =
  model JSON has no `"parent"`. Useful pattern reference for the rewrite.
- `OffAtlasImage.java` (51 ln) — `compositeOverBlack(NativeImage)` (premultiply, force alpha 255).
- `OffAtlasBgState.java`, `SlotBeBackfill.java` — bg-transparent flag + backfill (relevant to polish #3).

**Decode / pack (server side):**
- `AnimationDecoder.java` (403 ln) — `MAX_FRAMES=256` (line 55), `OFFATLAS_MAX_SIZE=512` (line 62),
  `GRID_BUDGET_PX=4096` (line 67). `decode(raw,size)` builds the grid PNG + returns `Decoded(stripPng,
  frameTimes[ticks], frameTimesMs[ms], frameCount, transparency, warning)`. `gridCols(n)=⌈√n⌉`,
  `gridCell(n,requested)` shrinks to fit 4096. **For S3 sharpness/cap:** raise/relax `GRID_BUDGET_PX` and the
  cell cap so the on-disk grid carries full-res frames (client no longer whole-uploads it). Beware bigger
  pack PNG / download size (ADR-accepted trade-off).
- `ServerPackGenerator.java` (~435 ln) — writes `slot_N.png` + grid sidecar (`gridJsonBytes`,
  `"timebase":"ms"`, `frametime=baseFrametimeMs`, iterates `playbackMs()`); legacy `mcmetaBytes` (ticks).
  Emits invisible block model for animated slots so only the BER paints them.
- `AnimData.java` (205 ln) — immutable record; carries `frameMs`; `timeMsFor`, `baseFrametimeMs`,
  `playbackMs`, `order()`. `TICK_MS=50`, `FALLBACK_MS=100`.

**Undo (for slice 1):**
- `UndoManager.java` — `Kind.RETEXTURE` + `recordRetexture(player, before, after, beforeTex, afterTex)`
  already exist. Add the studio re-skin recording the same way.
- `HistoryCommands.java` — has RETEXTURE inverse/forward (restoreSnapshot + TextureStore.save +
  `ResourcePackServer.updatePack()`). Studio re-skin undo should reuse this path.
- `command/handlers/RetextureAllCommands.java` — split out of `CreationCommands` for the 400-line gate
  (pattern to follow if a handler gets close to 400).

**Studio re-skin (slice 1 target) — find + read these before coding:**
- `StudioReskin.java` (client GUI re-skin / "Save changes") — currently records **no undo**. (Path not yet
  opened this session — `grep -r StudioReskin src` to locate; it's referenced in docs as the open undo gap.)
- The studio save likely calls a server command/path; the undo must be recorded **server-side** (server is
  authoritative) like the `/cb retexture` undo, not client-side.

**Polish targets (slices 2–4) — locate the studio anim/settings screen:**
- Look under `src/main/java/com/customblocks/client/gui/` (`StudioEditLoad.java` was touched in Step 2;
  the anim settings + preview screen lives near it). `grep` for the anim settings load + the preview render
  loop (frame-swap) to find slices 2 and 4. Slice 3 (bg re-fill) relates to `OffAtlasBgState` /
  `SlotBeBackfill` + the bg-color command path.

---

## 5. Known caveats / honesty flags to carry forward

- Step 2 owner report ("works better than before") covered **speed + undo**. Two sub-tests were **not
  itemized** by the owner and remain un-confirmed: **S2-3** (Esc pause freezes the animation) and **S2-4**
  (long `wardenn` clip). If anything looks off there, re-flag — don't assume passed.
- The `/cb retexture` undo restores pixels + anim flag but **not** the stored source bytes (obscure, no
  visible bug today). The new studio undo will have the same limit unless explicitly extended.
- Studio edit→save historically risked dropping ms timing — verify the save path preserves `frameMs` when
  touching slice 1.

---

## 6. Suggested first action in the fresh chat

1. Re-read `PROGRESS_LOG.md` top entry + this file.
2. `grep -rn "StudioReskin" src` and read it + its server save path.
3. Build **slice 1 (studio re-skin undo)** first — lowest risk, mirrors existing RETEXTURE undo. Green it.
4. Then slices 2→3→4 (polish), then slice 5 (Step 3 render rewrite, current-frame-upload first).
5. Update `PROGRESS_LOG.md` + `Reports/GROUP_14_TESTING_GUIDE.md` per slice (mark "built green, awaiting
   owner test" — NOT done).
6. Final report: list every change; clearly separate "logic, high confidence" from "GL render, needs your
   in-game eyes." Then the owner runs **one** test.

Do not mark anything ✅ until the owner confirms in-game.
