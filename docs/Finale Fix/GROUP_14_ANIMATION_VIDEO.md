# Group 14 — Animation, Video & Display Blocks

> **Status today (2026-06-19):** Part A — animated blocks via `/cb create <id> <name> <gif/webp-url>`,
> the studio "Load texture" preview, and the clickable `/cb anim` card — is **built and dev-confirmed
> in-game** ("they work"). Phase 2 (full studio editor) is built, pending in-game.
>
> **Render quality — LOCKED 2026-06-19 (owner, via mockups): HYBRID rendering (ADR-008).** The
> "soft/muffled" finding ran deeper than first thought, and **no atlas size wins**: capping at 256px
> (ADR-007) only traded muffle for **pixelation** (256px = blocky close up, 512px = atlas-overflow muffle),
> and the old `CustomBlocks/` mod hit the same wall (it also hard-capped at 256 — no trick to recycle). The
> fix is **Hybrid**: keep the **atlas/`.mcmeta`** path as the universal layer so every block keeps animating
> **everywhere as a normal 3D block item** (hand / hotbar / creative / `/cb list`), **and** add a per-block
> **own-texture renderer** (`NativeImageBackedTexture`, mipmaps off, **up to 512px**, the proven
> `screen_test` mechanism) for the **placed world block close-up** — full-res, crisp at any distance — with
> an **LOD fallback** to the atlas when far / off-screen / perf-tight. The owner reviewed in-game-style
> mockups and chose Hybrid over Full-Path-B (a 16px inventory icon looks identical, so a custom item
> renderer for ~1028 items is rejected). **Interim before the renderer lands:** invert `AnimationDecoder`'s
> budget so the atlas keeps high per-frame resolution and **samples frames down to fit** (no more 32px
> crush). See **ADR-008** (and ADR-007, which still bounds the atlas layer). The old "Sharp/Smooth Style
> toggle" was a misdiagnosis and is removed.
>
> **This is the v2 revamp.** It **supersedes** the original chest-GUI plan (old "P1 — AnimBlockScreen
> chest GUI" / "Q6 — Video Studio chest GUI" / jcodec). Those were replaced by the screen-based studio
> + pack-`.mcmeta` design and the TwelveMonkeys/ffmpeg source path. This doc lays out the full
> **Display Block** vision and its phased build order, decided with the owner on **2026-06-19**.
>
> **Golden Rule:** nothing here is ✅ until the owner runs it in-game. Build-green = it compiles, nothing more.
>
> **Status / checkmarks live in `Reports/GROUP_14_TESTING_GUIDE.md`. This spec stays clean (plan only).**

---

## 1. What this group is now

Group 14 grew from "restore the GIF block editor" into the mod's **Display Block platform**: any block
can show a **moving picture** (GIF / WebP / video), a **multi-block screen**, or **live data** (a clock,
a countdown, server stats), and **react** to redstone or a right-click. The animation tab inside
`/cb create` is the workshop where all of it is built and edited.

It is **one program, built in small testable phases** (§4) — not one drop. Each phase is useful on its
own and is verified in-game before the next begins.

---

## 2. The spine — two rendering paths (read this first)

Every feature below uses **one of two paths**. The path is not a preference — it is dictated by what the
feature needs, and it decides cost + where the block animates.

| | **Path A — pack `.mcmeta`** | **Path B — client renderer / BlockEntity** |
|---|---|---|
| How | A vertical frame-strip `slot_N.png` + a regenerated `.mcmeta` sidecar; Minecraft's atlas animates it. | The mod draws the block itself each frame (like the Arabic letters / ScreenTest block). |
| Animates in | **Everywhere** — world, hand, inventory, creative tab, item frames, `/cb list`. | **World only** (no block entity exists in a hand/inventory slot). |
| Cost | **Cheap + shared** — 1000 copies of the same block ≈ the cost of one (one atlas sprite). Cost scales with the number of **distinct** animated textures, not placed blocks. | **Per-block** — each rendered block/screen has its own cost. **This is what Auto-perf protects.** |
| Sync | All copies are **globally in lockstep**; you can't pause or offset one. | Each block is **independent** — can de-sync, gate, react, show different content. |
| Used for | Base animation, speed/loop/smoothing, color grading, chroma-key, framing, the timeline editor. | De-sync, video walls, live-data blocks, interactive/redstone reactive, trigger playback, auto-emissive. |

**Consequence to remember:** "lag from many animated blocks" is mostly a **Path-B** concern. Plain
mcmeta animation (Path A) does **not** get cheaper or more expensive with block count — so **Auto-perf
(Phase 5) governs Path-B features**, not the base animation.

---

## 3. Locked decisions (2026-06-19, with the owner)

**Architecture (kept from v1, still non-negotiable)**
- Animation persists as **plain numbers** in `slots.json` (frameCount, per-frame times, speed override,
  loop, interpolate, trim, framing, transparency, …) — **never** a baked mcmeta string. The
  `.mcmeta` is **regenerated deterministically at pack-build**. (Designs out the old "timing lost on
  save" bug.)
- The source GIF/WebP/video is stored **once** (`TextureStore.saveSource`); edits that only change
  numbers **never re-download** — they regenerate the tiny mcmeta + **one** pack rebuild. Edits that
  change pixels (sharpness, grade, chroma, crop) **re-decode from the stored source**, never the network.
- Animated slots are **excluded from `retexture-all`** so a batch resize can't flatten the strip.
- **256 frames max, full resolution.** Longer clips are **even-sampled down to 256**, then a human warning.
- Live preview is **client-side only** — tweaks update the preview instantly; the real block + pack
  rebuild happen **once** on Save. No live pack churn.

**The Animation tab (the owner's core ask)**
- A dedicated **Animation tab** inside `/cb create` (not buried in the Texture tab). It lights up once the
  loaded texture is animated; the Texture tab still owns the **source** (URL / video / color).
- The studio gains an **EDIT-LOAD path**: it can open an **existing** block with its full state loaded.
- **`/cb anim` is no longer a chat command card.** Routing:
  - `/cb anim <id>` → opens the studio on the **Animation tab in edit mode** for that block. *(Built — works.)*
  - `/cb anim` (no id) → opens a list of **animated blocks ONLY**; clicking one opens the studio **Animation
    tab editing that block**. **OPEN FIX (owner-reported 2026-06-19):** today `openList` opens the **full**
    block list (every block) and a click toggles a bulk ✔ / opens the chest editor — neither is animated-only
    and neither lands on the Animation tab. Fix = a dedicated animated-only chest list whose click calls
    `CreationStudioBridge.openStudioEdit(id)` (the `/cb anim <id>` wiring already exists).
  - (Typed shortcuts like `/cb anim <id> fps 10` may remain for scripting, but the **GUI is the path**.)
- **The Animation tab gets a FULL redesign (owner-reported 2026-06-19: "unorganized trash").** The current
  `StudioAnimPanel` crams header + Speed + Loop + Smooth + Trim into ~118px with no grouping or timeline.
  Revamp it into a clean, grouped, **animated-blocks-only** editor (see "Look + controls" below + the Phase 3
  timeline). Scope details settled in Phase 2/3 build.
- The preview cube **plays the animation live** (cycles frames at the chosen fps / loop / smoothing).

**Look + controls**
- Speed shown as **both fps AND ticks**, live cross-update; presets 5/10/20/30 + **match original**.
- Loop modes **Loop / Bounce / Reverse** via mcmeta frame-**index** ordering (no pixel duplication).
- Smoothing (interpolate) **ON by default**, per-block toggle.
- **The muffling/smoothness fix is a clean own-texture renderer (ADR-008, redesigned 2026-06-20).** All
  previous hybrid/LOD-fallback designs retired — they patched the atlas pipeline which can never be made
  crisp. New approach: placed animated blocks use a `BlockEntityRenderer` with `NativeImageBackedTexture`
  (mipmaps OFF, 512px, ms-based frame timing) — the same path Minecraft maps use. Atlas stays for
  inventory/hand only (fine at those sizes). Placed block model = transparent. No atlas involvement for
  placed visual. Proven by Minecraft's own MapRenderer + Slideshow mod (production mod, 1.21, GIFs on
  blocks, confirmed crisp). **Pre-build deletion: the `screen_test` cluster (5 files)** — never confirmed
  in-game, superseded by this cleaner design.
- **Timeline editor**: per-frame thumbnails, draggable playhead, in/out trim handles, per-frame duration,
  reorder / delete / duplicate, play/pause/step, right-click a frame → static block.
- **Color grading** (brightness / contrast / saturation / hue + tint), **chroma-key** (pick a color →
  transparent), **smart framing** (re-crop / pan / zoom instead of the forced centered square).
- **Playback polish**: random start offset (de-sync), seamless-loop crossfade, speed ramp/easing,
  play-once / hold-last-frame.

**Performance (owner: "think smarter")**
- A config control with **three modes**: **Always** · **Distance** (pick a radius) · **Auto**.
  **Auto** = play the **nearest** Path-B blocks up to a **frame budget** scaled by server TPS + loaded
  count, **freeze off-screen** blocks, and drop **far** blocks to lower fps (LOD). Set once, never lag.
- Applies to **Path-B** features (walls, live data, de-sync, interactive). Base mcmeta animation is unaffected.

**Video + big features**
- **Universal video import**: **ffmpeg-if-present** (mp4/webm/mov/avi → frames); clean **GIF/WebP fallback**
  when ffmpeg isn't installed. Never hard-breaks.
- **Video wall** (Path B): one **logical screen** drives all tiles via UV mapping → perfect sync, ~1/Nth
  the cost, **resize without re-slicing**. Any **N×M**, **auto-remap** on move/resize, **fit modes**
  (stretch / letterbox / crop). *(Corners / multi-surface deferred — not chosen for v1.)*
- **Live data blocks** (Path B, "smart sign"): real-world **clock**, **date/calendar**, **countdown/countup**,
  **live stats** (players online, TPS, in-game day, scoreboard), **scrolling marquee**. All styleable
  (font, color, format, background, glow).
- **Sync channels** (Path B): tag blocks to a named channel so they play in **lockstep** (the opposite of
  de-sync — needed for screens/clocks).
- **Auto-emissive glow** (Path B): bright pixels of each frame emit light, in time with the picture.
- **Trigger playback** (Path B): play on right-click / redstone / proximity / day-night instead of always-on.
- **Interactive** (Path B): right-click to pause / scrub / next clip.

**Redstone-reactive for ALL blocks (owner: "must be implemented … next steps?")**
- A general **redstone reaction** attribute on **every** custom block (not only animated ones), surfaced in
  the **Attributes** tab. When powered, the block applies a chosen effect (e.g. glow toggle · play/pause
  animation · swap to an alternate texture · solid↔passable · emit light). **Server-authoritative** (§5.8).
- Independent of the animation work — can be pulled earlier (it's its own clean phase, §4 Phase 10 / "early track").

**Not gating yet:** premium/license gating was raised but **deferred** — build free for now, gate later if/when the cloud licensing exists.

---

## 4. Phased build order

> Small, ordered, each verified in-game before the next. Phase = the "small step." Letters in **[A]/[B]**
> mark the rendering path (§2).

| Phase | Goal | Path | Notes |
|---|---|:--:|---|
| **1. Atlas muffling cap** (interim) | Cap texture size + bound the animated strip height so the atlas keeps mipmaps. Stops overflow, but 256px is blocky close up — an interim guard, not the real fix. | A | `CustomBlocksConfig.MAX_TEXTURE_SIZE`/`sanitizeTextureSize`; `AnimationDecoder.MAX_STRIP_PX`/`atlasSafeSize`. See ADR-007. (The earlier Sharp/Smooth toggle was a misdiagnosis — reverted.) |
| **1a. Decoder budget INVERT** (interim quality) | Stop the 32px crush: keep per-frame resolution high and **sample frames down** to fit `MAX_STRIP_PX`, instead of shrinking per-frame size. Immediate atlas-layer quality bump (and a better inventory icon) before the renderer lands. | A | Invert `AnimationDecoder.atlasSafeSize` logic. Small, contained. See ADR-008 §interim. |
| **1b. Own-texture renderer — REDESIGNED FROM SCRATCH (2026-06-20)** | **Atlas is NOT used for placed animated blocks.** Placed model = transparent (no atlas texture). A fresh 3-file system handles all placed-block rendering: `AnimSlotBlockEntity` (holds slotIndex), `AnimFrameCache` (client singleton — one `NativeImageBackedTexture` per animated slot, `setFilter(linear, mipmapOFF)`, 512px, updated per frame via `texture.upload()`), `AnimSlotBER` (BlockEntityRenderer — 6 faces via `RenderLayer.getEntityCutoutNoCull`, ms-based frame timing for 60fps smoothness, full `AnimData` loop/bounce/reverse support). Inventory/hand still use the atlas (fine at small sizes). **Pre-build: delete the `screen_test` cluster** (5 files — unproven prototype). Proven approach: same path as Minecraft maps + Slideshow mod (production-confirmed crisp). | B | Old `screen_test` / "hybrid" / LOD-fallback designs are **retired** — they were never confirmed in-game and the complexity was never needed. Start clean. |
| **2. Animation tab redesign + animated-only list + routing** | The headline. **Full redesign** of the Animation tab (owner: "unorganized trash") — grouped, clean, **animated-blocks-only**; live-playing preview; speed/loop/smoothing/trim; **studio edit-load path**. `/cb anim` (no id) → a list of **animated blocks ONLY**, click → studio **Animation tab** for that block (today it opens the full list + wrong click — open fix). | A | `ANIMATION` section in `BlockCreationStudioScreen` (exists; redesign it); animated-only chest list whose click calls `CreationStudioBridge.openStudioEdit(id)`. The edit-load path is reused by **every** later phase. |
| **3. Timeline frame editor** | Filmstrip thumbnails, per-frame duration, drag trim handles, reorder/delete/duplicate, play/pause/step, frame→static. | A | Frame mutations rewrite the stored strip; numbers stay in `AnimData`. |
| **4. Playback polish** | De-sync random offset, seamless-loop crossfade, speed ramp/easing, play-once / hold-last-frame. | A/B | Ramp + crossfade are Path-A (mcmeta/strip). **De-sync + play-once need Path-B** (a per-block client offset) → world-only; flagged honestly. |
| **5. Auto performance mode + config** | Config: Always / Distance(radius) / **Auto** (TPS+count budget, off-screen freeze, far-fps LOD). | B | The Path-B governor. Built before walls/live-data so they ship already-protected. |
| **6. Color grading + chroma-key + smart framing** | Brightness/contrast/saturation/hue + tint; color→transparent; re-crop/pan/zoom. | A | All bake into the strip via re-decode from the stored source; one uniform transform across frames. |
| **7. Universal video import** | ffmpeg-if-present → mp4/webm/mov/avi to frames; GIF/WebP fallback. | A | A source-layer add ahead of `AnimationDecoder`; detect ffmpeg, slice, hand frames to the existing packer. |
| **8. Video wall** | One synced **logical screen**, UV-mapped tiles, any N×M, auto-remap, fit modes. | B | New screen/multiblock + BlockEntity; counts as **one** Auto-perf budget unit. |
| **9. Live data blocks** | Clock / date / countdown / live stats / marquee; styleable. | B | Render data→texture on a tick; reuses the screen renderer from Phase 8. |
| **10. Redstone-reactive (all blocks) + interactive + triggers + channels + auto-emissive** | The Behavior rail: powered→effect on every block; right-click control; trigger playback; sync channels; emissive glow. | B | New `SlotData` behavior fields + server neighbor-update hook. **Redstone-reactive can be pulled early as its own track** — it doesn't depend on the animation work. |

**Dependency notes**
- Phase 2's **edit-load path** unlocks 3, 6, 8, 9, 10 (they all need to open an existing block).
- Phase 5 (**Auto-perf**) should precede 8 + 9 so the heavy Path-B features arrive protected.
- Phase 10's **redstone-for-all** is independent — if the owner wants it sooner, it slots in after Phase 1 without disturbing the rest.

---

## 5. Honest reality checks (so nothing surprises the owner)

- **Inventory animation is Path-A only.** Anything that needs a client renderer (de-sync, walls, live
  data, interactive) animates **in the world but not in a hand/inventory icon**. That's a hard engine
  limit, not a bug.
- **You can't pause a vanilla animated texture per-block.** Real pause/gate/de-sync require Path B.
- **ffmpeg is an external program.** Universal video only works where ffmpeg is installed; without it we
  fall back to GIF/WebP cleanly. (No silent failure, no auto-download.)
- **Animated WebP** decodes only if the bundled reader exposes its frames; otherwise it lands as a clean
  static block. The "muffled/speckled" look was the **block atlas + mipmaps** — no atlas setting, size, or
  `.mcmeta` trick can fix it. The real fix (ADR-008, redesigned 2026-06-20) is to **not use the atlas at
  all for placed animated blocks**: placed model = transparent, all placed visual = own-texture BER with
  mipmaps OFF. Inventory/hand stay on atlas (fine at small sizes).
- **Every phase is "done" only on the owner's in-game confirmation** — build-green proves compile + gates, nothing more.

---

## 6. Superseded from v1

- **Chest-GUI anim editor (old P1)** → replaced by the **screen-based Animation tab** in `/cb create`.
- **`/cb anim` chat card** → replaced by **opening the GUI** (the card was a Part-A stopgap; dev-confirmed
  working, now retired by Phase 2).
- **jcodec Video Studio chest GUI (old Q6)** → replaced by **ffmpeg-if-present universal import** feeding
  the same packer (Phase 7), edited in the Animation tab.
- **Live-BlockEntity-only animation** → rejected for base animation (wouldn't animate in inventories);
  the BlockEntity path is used **only** where independence is required (Path B).
