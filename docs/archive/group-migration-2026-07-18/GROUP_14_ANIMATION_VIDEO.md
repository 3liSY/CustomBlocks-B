# Group 14 — Animation, Video & Display Blocks

## UI medium audit (2026-07-09)

AnimListMenu (animated-blocks-only picker behind `/cb animation`) → **fold into the Block Creation Studio
Screen, not built.** One continuous flow instead of a chest hop before the Studio opens.

> 🟣 **IMAGE-INPUT OVERHAUL — investigation logged 2026-06-29 (NO code yet).** This group owns the
> "turn any link/file into a usable image" engine. The plan — paste ANY link works, stop the imgur
> `text/html` bounce (proven root cause, not AVIF), bundle WebP (have) + HEIC decoders, and bake in a
> sandboxed AVIF decoder (Cloudflare as interim) — plus all decisions and open questions, is in
> **`docs/Information/IMAGE_INPUT_OVERHAUL.md`**. Read that before touching ImageDownloader /
> LinkResolver / ImageProcessor / AnimationDecoder.

> 🟢 **CURRENT DIRECTION — 2026-06-21 (THIS IS AUTHORITATIVE — every banner below it is history).**
> In-game testing found **two bugs** (retexture garbles a block; animated blocks aren't the source's
> quality/speed) and we agreed a **3-step plan** to fix them properly. The render path stays **off-atlas
> grid** (ADR-013) and gets upgraded. **Full precise record — bugs, root causes, the whole brainstorm,
> every option, the decisions, the plan, and the memory math — is in § 7 at the bottom of this file**
> ("Animated Block Overhaul (2026-06-21)") and formally in **`ADR-014`**. Test plan: the matching new
> section in `Reports/GROUP_14_TESTING_GUIDE.md`. **Status: diagnosed + decided, NO code written yet.**

> 🟢 **RENDER PATH LOCKED 2026-06-21 — OFF-ATLAS GRID (ADR-013/ADR-014).** An atlas-revert was briefly
> planned (ADR-012) but **abandoned without ever shipping**: the off-atlas renderer is the owner-blessed
> design and it *does* render in-game (the two bugs above were found running **on** it — proof the blocks
> draw off-atlas). The atlas-revert handoff doc has been **removed**; ADR-012 stays only as a superseded
> record (ADR-013 cites its speckle diagnosis). The at-distance speckle that revert worried about is
> handled by **Step 3's mipmaps** (§7.4), not by leaving off-atlas.

> 🧹 **SCREEN + COMMAND CONSOLIDATION → GROUP 27 §G27.15 (commands 2026-06-21; full SCREEN DESIGN folded in
> 2026-06-22).** The Animation tab/screen now lives **entirely** in the screens group — its layout, controls,
> live preview, routing, and the timeline editor are owned by **`GROUP_27_SCREENS.md` §G27.15** (the single,
> canonical home; see its "Screen design" subsection), bound to the red+black standard. Commands also collapse
> there: all animation/video commands → one **`/cb animation`** screen hub (not chest); `/cb anim` **renamed →
> `/cb animation`** (old spelling **hard-removed**); standalone `/cb video` / `/cb video extract` **dropped**
> (import moves into the screen); typed editors (`ticks|fps|original|loop|smoothing|trim`) **still work**;
> `/cb animation` (no id) opens **`/cb listgui` filtered to animated blocks only**.
>
> **G14 owns ONLY the animation ENGINE** — the off-atlas render overhaul (§7), data (`AnimData` numbers),
> performance, video import, redstone behavior. **No screen/UI spec lives here** — it all points to §G27.15.

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
> **Render quality — UPDATED 2026-06-20 (owner): FULL OFF-ATLAS, "no atlas forever" — supersedes the Hybrid
> note above and ADR-008's "reject full Path B / hybrid LOD" (see ADR-011).** Every custom block (static +
> animated, **placed AND hand/inventory icon**) renders off-atlas; nothing of ours touches the atlas. The
> "owner decision pending (placed vs hand/inventory)" in §4 Phase 1c is **resolved → no-atlas-anywhere.**
> Locked knobs: **512px**, **mipmaps OFF** (kills the muffle for good), **smooth/linear sampling** (clean
> photos; pixel-art still crisp at 512). **Black background by default + a `transparent` toggle in
> `/cb config` and the config GUI.** **Option A** (accept a tiny distance "sparkle") ships first; **Option B**
> (full-res mip down-scales from the 512px image — NOT the old atlas muffle) is a deferred polish, no
> deadline. The 4-step build order + the code-confirmed invisible-block diagnosis live in
> `Reports/GROUP_14_TESTING_GUIDE.md` §6 and `PROGRESS_LOG.md` (2026-06-20).
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

**The Animation tab → SCREEN DESIGN MOVED to GROUP_27 §G27.15 (2026-06-22)**
> The Animation tab's UI — layout, grouping, control presentation, live-playing preview, the full redesign
> ("unorganized trash"), routing, and the timeline editor — is now owned by the **screens group**:
> **`GROUP_27_SCREENS.md §G27.15`**, bound to the unified red+black standard. It is **removed from this
> group**; GROUP_14 keeps only the animation **engine + data**. Do not re-spec the screen here.

Engine/data facts the screen relies on (kept here):
- The tab lights up once the loaded texture is animated; the **source** (URL / video / color) stays owned by
  the Texture tab.
- The studio **EDIT-LOAD path** (open an existing block with full state) is reused by every later engine phase
  (see Phase 2). *(Built — works.)*
- Live preview is **client-side only**; numbers persist in `AnimData` (see "Locked decisions" above).
- Command routing (`/cb anim` → `/cb animation` rename; the animated-only list entry) is tracked in
  GROUP_27 §G27.15.

**Engine behaviors the controls drive (kept here; the CONTROLS/UI live in GROUP_27 §G27.15)**
- Speed is stored as both **fps and ticks** (the screen shows both, cross-updating); presets 5/10/20/30 +
  match-original are a screen affordance.
- Loop **Loop / Bounce / Reverse** is done by mcmeta frame-**index** ordering — **no pixel duplication**.
- Smoothing = **interpolate**, ON by default, per-block toggle.
- **The muffling/smoothness fix is a clean own-texture renderer (ADR-008, redesigned 2026-06-20).** All
  previous hybrid/LOD-fallback designs retired — they patched the atlas pipeline which can never be made
  crisp. New approach: placed animated blocks use a `BlockEntityRenderer` with `NativeImageBackedTexture`
  (mipmaps OFF, 512px, ms-based frame timing) — the same path Minecraft maps use. Atlas stays for
  inventory/hand only (fine at those sizes). Placed block model = transparent. No atlas involvement for
  placed visual. Proven by Minecraft's own MapRenderer + Slideshow mod (production mod, 1.21, GIFs on
  blocks, confirmed crisp). **Pre-build deletion: the `screen_test` cluster (5 files)** — never confirmed
  in-game, superseded by this cleaner design.
- **Color grading** (brightness / contrast / saturation / hue + tint), **chroma-key** (pick a color →
  transparent), **smart framing** (re-crop / pan / zoom) all **bake into the strip** via re-decode from the
  stored source (one uniform transform across frames). The editing **UI** for these lives in G27 §G27.15.
- **Playback polish** mechanics — random start offset (de-sync, Path-B), seamless-loop crossfade, speed
  ramp/easing, play-once / hold-last-frame.

> **Timeline editor UI** (filmstrip thumbnails, draggable playhead, in/out trim handles, per-frame duration,
> reorder/delete/duplicate, transport, frame→static) → **moved to GROUP_27 §G27.15** (= the P8 Timeline
> Editor in §G27.6.P). Engine note kept here: frame mutations rewrite the stored strip; numbers stay in `AnimData`.

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
| **1c. Static own-texture renderer (off-atlas) — owner-requested 2026-06-20** | **The static-block half of 1b. This is the fix for "created blocks look blurry/low-quality even at 512px."** Today `AnimSlotBER` early-returns for static slots (`AnimSlotBER.java` ~line 47: *"static slot — its normal atlas model already rendered it"*), so a STATIC custom block still renders through Minecraft's shared block atlas → mipmap muffle. **Owner confirmed (2026-06-20) the block is blurry at BOTH 256px and 512px, in singleplayer AND on the server** → proves it is the atlas, not the multiplayer pack-sync. The placed animated blocks are already crisp via 1b; static blocks were left out ("a separate, smaller follow-up"). **Build:** a static texture cache (one `NativeImageBackedTexture` per static slot, loaded **once** from `slot_N.png`, **no** per-frame upload — the only difference from `AnimFrameCache`), `setFilter(nearest/linear, mipmap OFF)`, up to 512px; `AnimSlotBER` draws static slots from it (6 faces, honouring shape/face-overrides/glow); static placed model → transparent (same pattern as 1b). After this, NO custom block (static, animated, Arabic) uses the atlas for its placed visual. | B | Reuses 1b's `AnimSlotBlockEntity` (already on every slot block via Slice A) + the same `RenderLayer.getEntityCutoutNoCull` draw. See ADR-008 (Path B) — this completes it for static blocks. **⚠️ Owner decision pending (placed vs hand/inventory):** a `BlockEntityRenderer` only draws the *placed* world block, so hand/inventory/creative icons would still come from the atlas (soft). To remove the atlas *everywhere* also add a custom item renderer (`BuiltinModelItemRenderer`, the path shields/banners/chests use). **(1)** placed-only = proven + smaller; **(2)** placed + item renderer = atlas gone everywhere, more scope. Owner leans toward no-atlas-anywhere; confirm before building. |
| **2. Animation tab redesign + animated-only list + routing** | The headline. **Full redesign** of the Animation tab (owner: "unorganized trash") — grouped, clean, **animated-blocks-only**; live-playing preview; speed/loop/smoothing/trim; **studio edit-load path**. `/cb anim` (no id) → a list of **animated blocks ONLY**, click → studio **Animation tab** for that block (today it opens the full list + wrong click — open fix). | A | **Screen layout/redesign owned by GROUP_27 §G27.15** — this row = the edit-load wiring + the animated-only `/cb listgui` filter whose click calls `CreationStudioBridge.openStudioEdit(id)` (engine/command). The edit-load path is reused by **every** later phase. |
| **3. Timeline frame editor** *(UI → GROUP_27 §G27.15 / §G27.6.P P8)* | Filmstrip thumbnails, per-frame duration, drag trim handles, reorder/delete/duplicate, play/pause/step, frame→static. | A | **Timeline UI owned by the screens group**; this row = the engine side — frame mutations rewrite the stored strip; numbers stay in `AnimData`. |
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

## 5b. Source recovery — fixing blurry OLD blocks (re-sourcing plan)

> Added 2026-06-27 from the `/cb sourcelist` follow-up. **DESIGN / PLAN ONLY — not built.**
> Separate from the render slices: this is about *up-close softness* (small original images), NOT the
> atlas muffle (fixed by Slice 1b/1c) or the distance sparkle (Slice 2 mipmaps — ⏸ deferred as polish).

**The scare that started this:** `/cb sourcelist` reported only ~12 of 1018 blocks "auto-ready" (have a
saved `.src` original) → owner feared the rest were lost in the `.dat → .png` migration.

**What is actually true (verified on disk 2026-06-27):**
- Old `customblocks_data/textures/slot_N.dat` files are **byte-identical** to the live
  `config/customblocks/textures/slot_N.png` (md5 match). The migration was a pure rename. **Nothing was
  lost; the old folder has no extra sources** (no `sources/` dir in it).
- Separate `.src` originals are a **newer feature** (`TextureStore`). Old blocks predate it → never had
  one. The ~12 auto-ready are the post-feature blocks. Expected, **not damage**.

**Real target is NOT ~1000 blocks — it's ~242.** Of 1039 blocks:

| group | count | action |
|---|---|---|
| `_green` / `_yellow` / `_red` recolor variants **with a base** | ~680 | regenerate by recolor from base — no source needed (`ColorVariantService`) |
| standalone bases ≥512px | 28 | already sharp |
| no-texture (Arabic glyph / video) | 10 | generated, vector-sharp — never re-source |
| **bases ≤256px needing a bigger picture** | **~242** | the only real work |

**OWNER RULING 2026-06-27 — `_black` blocks count as BASES, not auto-derivable variants.** Black recolor
is degenerate (can't be cleanly re-derived from a base the way green/yellow/red can), so each `_black`
block needs **its own source**. → the 79 `_black` blocks are in the re-source target set, NOT the
auto-regenerate set. (Decomposition counts above will shift once `_black` is excluded from the variant
collapse and re-tallied at build time.)

**Fix options for the ~242 (owner decides at build time):**
1. **Local AI upscale** (Real-ESRGAN / Upscayl) — batch 256→1024, no internet, scriptable; de-blurs every
   base incl. non-findable ones; invents no true detail.
2. **Name-search HD download** — most bases are logos/brands (apple, bmw, barca, xbox…), trivially found
   in HD; semi-manual pick per block.

Either way: writes only new `sources/*.src`, **never** touches `textures/` or the old folder; then re-run
recolor so variants follow. `retextureall` takes a safety backup first (Slice 1d).

---

## 5d. Source Wall — temporary review-and-fix workflow for the ~242 (💬 plan, not built · 2026-06-28)

> Owner-chosen path for §5b **option 2** (name-search HD download), decided 2026-06-28. **Not built.**
> Owner's words: "see all the blocks in front of me in the server, fix them ASAP, easiest way."

**The loop (4 steps):**
1. **Lay them out — `/cb sourcewall` (TEMPORARY command).** Places every blurry black-bg / `_black` base
   (the in-scope set ≈ 203, see §5b tally) in a **labelled grid** in the world so the owner sees the whole
   catalogue at once. Each placed block carries a floating name tag = its `customId`, so the owner knows
   exactly which id to retexture. **Registry-safe** — it only *places* blocks; it edits no `SlotData`,
   `slots.json`, `textures/`, or `sources/`.
2. **Find links (assistant side, off-server).** Claude web-searches an HD source per name-matchable block
   (logos / brands / flags / clubs → Wikimedia / official) and produces a **manifest**: `id → candidate URL
   + confidence`. Personal/test names (ali, jalal, sports_planet, billiardo…) have no web match → marked
   "owner supplies / skip", **never guessed**.
3. **Approve + fix (owner, in-world).** Owner walks the wall, eyeballs each candidate, and for the good ones
   runs the already-built **`/cb retexture <id> <url>`** (live update; `/cb undo` per block). One block at a
   time — no blind batch.
4. **Variants follow.** Once a base is sharp, its `_red/_yellow/_green` regenerate by recolor (Triangle).
   `_black` are bases (own source) — owner ruling §5b.

**HARD REQUIREMENTS (owner, 2026-06-28) — must be 100% precise:**
- **Revert / teardown.** `/cb sourcewall` must **clean up after itself** — a paired teardown (e.g.
  `/cb sourcewall clear`) removes **only** the blocks it placed and restores the area. It must record the
  exact positions it placed so teardown never touches anything the owner built. The wall is scaffolding,
  never a permanent world edit.
- **Self-delete.** The command is **temporary**. Once the ~242 re-source job is finished, the
  `/cb sourcewall` command + handler are **removed from the codebase** — not left as dead surface. Track
  this as an explicit closeout task.
- **Safety.** Places into empty / a fresh flat area only; never overwrites existing blocks; teardown is
  exact (placed-set only); touches no slot/texture/source data.

**Status:** 🟢 **BUILT 2026-06-28 (build-green, gates pass) — awaiting in-game confirm.** Not ✅ until the owner
runs it. **Rev 2 (2026-06-28):** first in-game run placed only 197 — the black-only border gate dropped all
**48 transparent-bg bases** (sambosa, whatsapp, tesla…), which render black in-game so the owner counts them as
"black blocks". `borderIsBlack` → `borderRendersBlack` now also accepts a **>80%-transparent** border; offline
scan of 339 bases = 199 black + 48 transparent + 82 coloured-bg + 10 no-texture, so the wall should now show
**≈247 bases** + gifs. The **edge-black recolour bug** (GROUP_10 §G10-3) is a **separate** known issue to fix
**after** the wall job — do not lose it.

**Files (TEMP — delete at closeout):** `core/SourceWall.java` (scope scan + black-border detect + teardown-
record persistence) · `command/handlers/SourceWallCommands.java` (`/cb sourcewall [clear]`, OP-only) ·
`CommandRegistrar` registration line. Scope (rev 2 2026-06-28) = **black-bg + transparent-bg + `_black`** bases
**+ all gifs**; skips colour variants (`_red/_green/_yellow/_hex_`) + Arabic glyphs (`arabic*`) + coloured/photo
borders (flags etc.). **No baked-size gate** — server bakes at 512 so baked width ≠ source quality (the v1 ≤256
gate wrongly placed only the 10 Arabic glyphs); the v2 black-only gate wrongly dropped 48 transparent bases.
Chat reports placed count + skip breakdown. Grid 20-wide / 2-spacing / starts 2 ahead; skips non-empty cells;
each block gets an invisible armor-stand id label; placed positions + label UUIDs saved to
`config/customblocks/sourcewall.json`; `clear` removes only blocks still `instanceof SlotBlock` + those UUIDs.

**Tests:** `GROUP_14_TESTING_GUIDE.md` §R (R1–R7, 🟥 0/7 — R7 = transparent bases now placed).

---

## 5c. In-mod resize quality — Lanczos + light sharpen + small-source warning (Slice 1f / "Option B")

> Built 2026-06-27. **The automatic, every-user, every-image lever** — runs inside the mod on every
> `/cb create` and `/cb retexture(all)`, no setup, no tools, works on a dedicated server.
> Sits next to §5b (which is the *manual* owner-side re-source of the ~242 old bases).

**Why this and not in-mod AI.** Owner asked: can every user get the high-quality result automatically,
fast, like `retextureall`? We deep-searched embedding an AI upscaler (Real-ESRGAN/ESRGAN) inside the mod
via ONNX Runtime / DJL. Rejected:
- **Size** — ONNX Runtime's published Java jar is ~260 MB (bundles Win/Mac/Linux native libs) + a ~64 MB
  model. Our whole mod is small; this bloats it 10–100×.
- **Speed/server** — real ESRGAN ×4 on **CPU** is seconds per image and a dedicated server has no GPU →
  baking many blocks would lag. Only tiny models are fast, and they give a smaller bump.
- **Quality ceiling** — the winning `5_HD_PLUS_AI` pipeline = **HD download + AI**. The mod cannot
  auto-find an HD version of a picture, so in-mod AI could only do the AI-on-your-pic half = the *mushy*
  variant #4, never #5. Half of #5 is a human/web step the mod can't automate.

So the chosen lever improves the **resize maths itself** — better for everyone, for free.

**What it does.**
1. **Lanczos-3 resampler** (pure Java, no dependency) replaces Java2D **bicubic** in
   `ImageProcessor.toBlockPng`. Resamples in **premultiplied alpha** so transparent logos get **no dark /
   coloured halo** around the edges. Separable (horizontal then vertical pass); edge-clamped.
2. **Light unsharp mask** applied **only when enlarging** (source smaller than the block) — restores the
   crisp edges bicubic smears. Skipped when shrinking (Lanczos is already crisp there; sharpening a
   down-scale just adds aliasing).
   - **Anti-ringing clamp (fix, 2026-06-28).** Lanczos-3 has *negative lobes*: at a hard bright/dark edge
     (e.g. a moon's lit limb against the already-blacked background) the weighted sum overshoots **past the
     brightest real pixel → clamps to pure white (255)**, painting a thin **white ring hugging the subject**.
     The unsharp mask then amplifies it. This is generated **after** background removal, so it survives
     **any** tolerance/mode — it is *not* leftover background. Proven offline: source brightest pixel 247,
     pipeline output 255; plain bilinear stays 247. **Fix:** in each resize pass, clamp every output pixel
     to the **min/max of the real source samples that fed it** (samples with weight > 1e-6); and clamp each
     sharpened pixel back into its own **3×3 neighbourhood**. Overshoot (the ring) is removed; in-range
     micro-contrast (the sharpness) is kept. Re-verified offline: full pipeline max back to 247, ring gone.
   - **Anti-fringe peel (fix, 2026-06-28) — *second, distinct* white ring.** Separate from the clamp above.
     When a photo subject is **feathered onto a white page** (e.g. a stock moon on white), the soft
     anti-aliased edge leaves a pale-grey **halo ring** that pure colour-keying can't separate from a pale
     subject limb (low tolerance → ring stays; high tolerance → it chews the limb). Lives in
     `BackgroundRemover` (Stage 2), *after* the flood-fill marks the background. **Fix = gradient-shape gate,
     not colour:** peel an edge pixel only where the image is a **gradient descending toward the background**
     (this pixel is closer to the bg colour than the neighbour just inward, by ≥ `PEEL_MARGIN`). It climbs a
     feather ring-by-ring and **stops dead at the flat subject body** however pale — a flat light-grey logo
     edge is *not* a descending gradient, so it keeps its 1px anti-alias. Bounded by `PEEL_CAP` (never shave a
     pixel already clearly subject) and `FRINGE_PASSES` (max rim depth in px). **Tolerance-independent** — works
     the same at tol 20 and tol 50. Tunables (locked after over-peel test 06-28): `PEEL_CAP=45`,
     `PEEL_MARGIN=1.5`, `FRINGE_PASSES=8`. (Pushing `PEEL_MARGIN→1.0`/`FRINGE_PASSES→12` started eating the
     moon limb — reverted.) Offline-proven on 4 cases: moon (ring gone, body/limbs intact), crisp dark logo
     (~0.5% change), flat light-grey block (~7% ≈ 1px rim), thin 2–3px features (survive).
   - ⚠️ **STILL OPEN — "white blob beside the moon" at low tolerance.** A *different* defect, **not yet
     fixed.** At low tolerance the flood-fill is stricter, so a pocket of near-white background that is
     separated from the frame edge (or sits just over the colour step) can be left **unremoved as a white
     blob**. Not reproducible on the dreamstime moon sample at tol 20 (background comes out fully clean) —
     needs the owner's exact source image / blob location to repro and fix. Tracked, do not assume the peel
     above covers it.
   - ⚠️ **Recolour twin — "edge-black" (2026-06-28, NOT fixed).** The *same* flood-connectivity limit hits
     the **colour-variant** (Triangle) path: when a Triangle recolours a base, a black pocket walled off from
     the frame edge — or a black that's off-tolerance — stays **black** instead of taking the new colour
     (owner's `iafc_green` screenshot). Full root-cause + fix options in **GROUP_10_COLOR_IMAGE.md §G10-3**.
     Fix scheduled **after** the §5d Source-Wall job.
3. **Small-source warning** — when the picture you give is smaller than the block size, chat prints a
   non-blocking heads-up ("that picture is only 128×128px… it was enlarged, may look soft; use ≥512px").
   It does **not** stop the create — just tells you before you wonder why it's soft.

**Honest limits (no overselling).**
- This is **not AI**. It adds **no new detail** — only crisper edges and far less mush than bicubic. Tiny
  text from a 128px source still won't become readable. For true detail: §5b re-source, or feed a big
  picture / the `5_HD_PLUS_AI` result as the source.
- It runs at **bake time** only (create / retexture / retextureall). Already-baked blocks change only when
  re-baked from their saved original.

**Files.** `image/ImageResampler.java` (new — Lanczos + unsharp; + anti-ringing clamp 2026-06-28), `image/ImageProcessor.java`
(`toBlockPng` now calls the resampler; `smallSourceNote`/`dimensions` helpers), `command/handlers/
CreationCommands.java` (emits the small-source heads-up on create + retexture). Tests → `GROUP_14_TESTING_GUIDE.md` §Q.

---

## 6. Superseded from v1

- **Chest-GUI anim editor (old P1)** → replaced by the **screen-based Animation tab** in `/cb create`.
- **`/cb anim` chat card** → replaced by **opening the GUI** (the card was a Part-A stopgap; dev-confirmed
  working, now retired by Phase 2).
- **jcodec Video Studio chest GUI (old Q6)** → replaced by **ffmpeg-if-present universal import** feeding
  the same packer (Phase 7), edited in the Animation tab.
- **Live-BlockEntity-only animation** → rejected for base animation (wouldn't animate in inventories);
  the BlockEntity path is used **only** where independence is required (Path B).

---

# 7. Animated Block Overhaul — bugs, brainstorm & plan (2026-06-21)

> **Why this section exists:** the owner tested the off-atlas GRID build (ADR-013) in-game and found two
> real problems. We diagnosed both from the code, then brainstormed fixes and the owner chose a direction.
> This is the **complete, precise record** so the work survives a chat change. **No code is written yet —
> this is diagnosis + decisions only.** Formal decision record: **`ADR-014`**. Test plan: the
> "§7 — Overhaul" section in `Reports/GROUP_14_TESTING_GUIDE.md`.
>
> **Prior build state (unchanged, from the previous session):** off-atlas GRID renderer (ADR-013) —
> builds green, NOT in-game confirmed. Safety checkpoint commit `6aecd74`.

## 7.1 The two bugs (root-caused in code)

### Bug A — `/cb retexture <id> <gif>` garbles the block (data desync)

- **Symptom (owner screenshot 2026-06-21):** after `/cb retexture say <gif>`, the block shows scrambled
  face fragments smeared across the cube — a scattered mess, not the picture.
- **Root cause — a data desync, NOT a render glitch.** Exact chain:
  1. `/cb retexture` runs `CreationCommands.retexture()` → `applyTexture()`
     (`CreationCommands.java:266`). `applyTexture` treats **every** URL as a plain static image:
     `BackgroundRemover.apply` → `ImageProcessor.toBlockPng(..)` (`ImageProcessor.java:29`, which does
     `ImageIO.read` = first GIF frame only) → `TextureStore.save(index, oneSquarePng)`.
  2. It does **two** wrong things vs. create: (a) it never detects an animated GIF — `/cb create` does,
     via `AnimCommands.maybeCreateAnimated()` called at `CreationCommands.java:170`; `applyTexture` has no
     such call. (b) It **never touches `AnimData`**.
  3. So if "say" was already an **animated** block, the texture file is now ONE static square but the slot
     is still flagged `animated, N frames`.
  4. On the next pack rebuild, `ServerPackGenerator.emit()` takes the animated branch because
     `anim.isAnimated() && TextureStore.has(i)` is still true (`ServerPackGenerator.java:112`). It calls
     `stripCols(singleSquare, N)` (`ServerPackGenerator.java:297`): `(long)w*N == h`? For a 1-cell square
     `w*N != w`, so it returns `gridCols(N)` (e.g. 16 for 256) and writes a grid sidecar claiming the
     1-face image is a 16×16 grid of N frames.
  5. Client `AnimFrameCache.build()` (`AnimFrameCache.java:126`) reads `count=N`, `cols=gridCols(N)`,
     `cell = w/cols` (tiny), then `Slot.uv(frame)` (`AnimFrameCache.java:99`) samples N little cells out of
     a single face → garbage smear. **= the screenshot.**
- **Also a latent bug even if "say" was static:** retexturing a block with a GIF should make it animate
  (like create does) — today it just bakes frame 0 as a still. So retexture can't turn a block animated.

### Bug B — animated blocks (e.g. `wardenn`, 256 frames) aren't the source's quality or speed

Two **separate** hard caps, both real, both **different** from the at-distance "speckle" (that one is the
mipmaps-OFF aliasing flagged as ADR-013's open risk):

- **Quality capped at 256 px/frame.** `gridCell(n, requested)` (`AnimationDecoder.java:277`) shrinks the
  cell so `cols·cell ≤ GRID_BUDGET_PX` (4096, `AnimationDecoder.java:67`). For 256 frames `cols = 16`
  (`gridCols`, line 267), so `cell ≤ 4096/16 = 256`. If the source GIF frames are bigger than 256 px they
  get bicubic-downscaled to 256 → softer / less detail than the original. The shrink is **forced** by the
  "all frames live in one GPU texture" design — it is a symptom, not a choice.
- **Speed capped at 20 fps.** Frame timing is in **whole Minecraft ticks** (1 tick = 50 ms). The decoder
  rounds each GIF delay `round(cs/5)` with a floor of 1 (`AnimationDecoder.java:212`); `AnimData.timeFor`
  floors at 1 tick (`AnimData.java:115`); the client advances frames on tick boundaries
  (`AnimFrameCache.Slot.currentFrame`, `AnimFrameCache.java:82`). Any GIF faster than 20 fps (≤4 cs/frame)
  collapses to 20 fps → plays **slower** than the web original.

## 7.2 The brainstorm — every option considered

**Axis A — where frames live (the VRAM problem).** Today: ALL frames packed into one grid texture, all
resident on the GPU → cost = frames × resolution (that's what forces the 256 px shrink).

| Option | What | Trade |
|---|---|---|
| Current grid (today) | all frames in one GPU texture | simple draw, zero per-frame CPU; **VRAM = frames×res** (forces shrink) |
| **(1) Current-frame-only upload** | keep frames in RAM, upload only the frame showing now to one small GPU texture (the "video texture" trick; same path MC maps + dynamic GUIs use) | **VRAM ≈ 1 frame/id**; costs one small upload per displayed frame + RAM holds frames |
| (2) GL array texture (`GL_TEXTURE_2D_ARRAY`) | frames as layers, not a grid | cleaner UVs, allows **per-layer mipmaps**; VRAM unchanged (all resident) |
| (3) BC7/DXT compression | compress the resident texture (every game does this) | **4× less VRAM**; minor, usually-invisible quality cost; still resident |
| (4) Texture pool | only **on-screen** animated blocks hold a GPU texture, from a fixed reusable pool | **VRAM capped by how many you SEE**, not how many exist; the part that makes it safe at scale |
| (5) RAM frames compressed + LRU evict | hold frames compressed in system RAM, drop ids with no loaded blocks | bounds **system RAM** to the visible/loaded set |

**Axis B — speed/timing.** (a) milliseconds-from-ticks (simple, still tick-quantized jitter, auto-pauses);
(b) **real-world clock with freeze-on-pause** (true speed, >20 fps, smooth through tick lag).

**Axis C — quality target.** Owner's words: *"the same as the blocks'"* → animated frames render at the
**same resolution as static blocks** (the `/cb config texturesize` setting, up to 512), **not** a separate
animation cap. This becomes affordable once only one frame is resident (Axis A option 1).

**Axis D — retexture robustness.** (i) **spot-fix**: make retexture animation-aware (GIF → rebuild grid +
set anim; static → save + **clear** anim) — mirrors create; (ii) **+ fail-safe client guard**:
`AnimFrameCache.build` rejects a texture whose real dimensions don't match the claimed `cols×cell` and
falls back to frame 0 / static instead of rendering garbage — immunizes against **any** future desync from
**any** path; (iii) **+ structural atomic write**: route every texture write through one method that
updates texture + anim info together so they can't drift.

**Bonus ideas raised:** mipmaps on the resident frame to **kill the distance speckle** (ADR-013's open
risk); **raise the 256-frame cap** once VRAM is pool-bounded (longer clips keep every frame; costs RAM,
not VRAM); **LOD** = update far blocks less often; **fully pause off-screen** blocks (stop advancing +
uploading frames you can't see).

## 7.3 Decisions (owner, 2026-06-21)

1. **Retexture fix — smallest first.** Do the **spot-fix only** now (Axis D-i). If in-game testing shows
   it's not enough, **escalate**: add the fail-safe guard (D-ii), then the atomic write (D-iii). *(Owner:
   "i wanna try the smaller first, if doesnt work we go deeper.")*
2. **Speed — real-world clock, freeze on pause** (Axis B-b).
3. **Quality/VRAM — all three levers** (Axis A: pool **+** current-frame-only **+** RAM compression), with
   per-frame resolution = the block texture-size setting (Axis C). VRAM capped by what's on screen.
4. **Bonuses — all four:** mipmaps (fix the distance speckle), raise the 256-frame cap, far-block LOD,
   fully pause off-screen blocks.

## 7.4 The plan — 3 steps, each tested in-game before the next

> One step at a time. Owner only has to place a block and say if it looks right. Nothing is ✅ until the
> owner confirms in-game (Golden Rule). "Likely files" are estimates to orient the next chat — confirm
> against the code before editing.

**Step 1 — Retexture spot-fix (smallest; escalate only if needed).**

> ✅ **GROUNDED 2026-06-21 (read before coding):** the studio "Save changes" rail **already does this fix** —
> `StudioReskin.apply` detects an animated source → fresh `AnimData`, static → `AnimData.NONE`
> (`StudioReskin.java:60-78,107`). Bug A lives **only** in the `/cb retexture` chat rail: `applyTexture`
> (`CreationCommands.java:266`) treats every URL as a plain static image. Confirmed `applyTexture` has **one
> caller** — `retexture()` (`CreationCommands.java:256`); its "shared with create-with-url" header comment is
> **stale** (create uses `createWithTexture`, which already calls `maybeCreateAnimated`). So Step 1 = mirror
> `StudioReskin`'s animated/static branch into `applyTexture`; **zero blast radius into create.** A GIF onto a
> previously-static block **becomes animated** (owner-confirmed 2026-06-21), same as create.

- Make `applyTexture` (retexture rail) animation-aware, mirroring create:
  - new URL is an animated GIF → decode to the grid + `SlotManager.setAnim(id, AnimData.ofDecoded(...))`
    (reuse the `AnimCommands.maybeCreateAnimated` logic, but for an **existing** slot — likely a new
    `AnimCommands.reskinAnimated(...)` / shared helper).
  - new URL is static → save the static PNG **and clear** the animation:
    `SlotManager.setAnim(id, AnimData.NONE)` (verified: `AnimData.NONE` has frameCount 0 → `isAnimated()`
    false; `SlotManager.setAnim` exists at `SlotManager.java:237`). This is what stops the desync.
- **Escalation (only if Step 1 fails in-game):** add the fail-safe guard in `AnimFrameCache.build`
  (reject mismatched texture vs `cols×cell` → render frame 0 / static); then the atomic texture+anim write.
- Likely files: `CreationCommands.java`, `AnimCommands.java`, (escalation) `AnimFrameCache.java`,
  `TextureStore.java` / `SlotManager.java`.

**Step 2 — Real-world-clock timing (true speed, >20 fps, smooth; freeze on pause).**
- Drive playback off wall-clock elapsed time (accumulated, frozen while the game is paused) instead of
  `worldTime` ticks. Carry per-frame times in **milliseconds** in the grid sidecar so sub-tick / >20 fps
  is expressible.
- Likely files: `AnimFrameCache.java` (`Slot.currentFrame` + timing fields), `AnimSlotBER.java` (pass real
  time), `SlotItemRenderer.java` (icon), `ServerPackGenerator.java` (sidecar ms field),
  `AnimationDecoder.java` / `AnimData.java` (keep finer-grained times).

**Step 3 — Quality pass (the big rewrite): pool + one-frame-on-GPU + full resolution + mipmaps + RAM
compression, then the remaining bonuses.**
- Client render path: keep decoded frames in **system RAM** (compressed); a **fixed pool** of reusable
  GPU textures sized to the max cell; assign a pool texture only to animated blocks **on screen**; upload
  the current frame when shared playback advances; **mipmaps ON** for that resident frame (kills the
  distance speckle); **LRU-evict** RAM frames for ids with no loaded blocks; **far-block LOD** (lower
  update rate) and **off-screen pause** (no advance/upload).
- Decoder/quality: per-frame cell = the block `textureSize` (drop the `gridCell` shrink for the
  pool/streaming path); **raise `MAX_FRAMES`** (256) now that VRAM is pool-bounded.
- Likely files: `AnimFrameCache.java` (pool + streaming + mipmaps + eviction — likely splits into ≥2
  classes to stay under the 500-line gate), `AnimSlotBER.java`, `SlotItemRenderer.java`,
  `AnimationDecoder.java`, `ServerPackGenerator.java`, `CustomBlocksConfig` (LOD/perf knobs).
- This is a real rewrite of the client render path → **expect it split across slices**, each built green +
  tested, per the §9.3 file-size gates.

## 7.5 Memory math (per **distinct** animated block id; placed copies of one id share a texture)

| per-frame | grid texture (today, all-resident) | VRAM today | VRAM with pool + current-frame-only |
|---|---|---|---|
| 256 px (current cap) | 4096² | 64 MB | ~1 frame, bounded by on-screen pool |
| 384 px | 6144² | 144 MB | ~1 frame, bounded by on-screen pool |
| 512 px (= "same as blocks") | 8192² | 256 MB | ~1 frame, bounded by on-screen pool |

So today "10 different 512 px animated blocks ≈ 2.5 GB GPU" — which is *why* the code shrinks to 256.
With the Step-3 pool, total VRAM is bounded by how many animated blocks are **on screen**, not how many
exist; system RAM holds the frames (compressed, LRU-evicted). That is what makes "same as the blocks"
affordable.

## 7.6 Verified-technique notes (so nothing here is a gamble)

- **One frame on GPU + per-frame upload** = how video playback and MC's own map renderer work
  (`NativeImageBackedTexture` + `upload()` / `glTexSubImage2D`). Standard, production-proven.
- **GL array textures**, **BC7/DXT (S3TC/BPTC) compression**, **mipmaps for minification aliasing**, and
  **texture pooling** are all standard, widely-supported desktop-GL techniques.
- **Wall-clock cosmetic animation** (freeze on pause) is the normal way to decouple animation speed from
  tick rate.
- None of this changes the server model: the pack still ships the image + sidecar; all of this is
  **client render** (works on a dedicated server too).

---

## 8. Image-Input Engine — Layer 1: the link-fixer

> Part of the **Image-Input Overhaul** (full record: `docs/Information/IMAGE_INPUT_OVERHAUL.md`).
> This is **Layer 1 of 3** — "paste ANY link and it works." Layer 2 (HEIC) and Layer 3 (real AVIF)
> are still queued with open decisions; see the master doc. **Status / checkmarks live in
> `Reports/GROUP_14_TESTING_GUIDE.md` §T. This spec stays clean (plan only).**

**The job.** A pasted link should turn into a block whether it's a direct "Copy Image Address" link,
a share link, or a plain page link — with the mod doing any resolving itself, invisibly.

**What it does (two parts):**

1. **Stop the webpage bounce (the proven root cause).** The download request no longer says it
   accepts `text/html`. Sites like imgur read `text/html` as "a browser is visiting" and send back
   their **webpage** instead of the **image** — that was the old "Couldn't find a direct image on
   that page" failure on every direct imgur link. The request also stops asking for `image/avif`
   (which we can't decode yet, and which made sites send avif instead of jpg/png). The trailing
   `*/*;q=0.8` is kept so a genuine **page** link still returns HTML for the page-reader below.

2. **Per-site shortcuts + the page-reader.** `LinkResolver.directImageUrl(url)` rewrites a handful
   of known links straight to the direct image with pure string rules (no network), and the existing
   `og:image` page-reader (now able to run, since pages return HTML again) handles everything else.
   - **Deterministic rewrites (safe, no guess):** Google Images wrapper (`imgurl=`), Google Drive
     share → `uc?export=download`, Dropbox → `dl.dropboxusercontent.com`, Twitter/X media
     (`pbs.twimg.com` → `?format=…&name=large`), GitHub `/blob/` → `raw.githubusercontent.com`.
   - **Via the page-reader (`og:image`/`twitter:image`):** imgur pages, Discord, Reddit, Pinterest,
     Tenor, Giphy, Steam, Flickr, DeviantArt, Tumblr, Wikimedia/Wikipedia, plus any other site that
     advertises a preview image. **Login-walled sites (Instagram, Facebook) may still fail** — their
     wall, not our bug.
   - Rewrites fire **only** when the transform is well-established; we never guess a rewrite that
     could break a link that already worked.

**Files touched:**
- `image/ImageDownloader.java` — the `Accept` header (drop `text/html` + `avif`); the download path
  now rewrites a known share link up-front and re-normalizes a resolved media URL.
- `image/LinkResolver.java` — new `directImageUrl(url)` + helpers (`hostOf`, `queryParam`, `driveId`).

**Not in this layer (queued):** HEIC decoder (Layer 2), real AVIF (Layer 3 — bake-in vs Cloudflare,
undecided), `/cb importfolder` rework (Group 12), drag-and-drop create (Group 27).

---

## G10-1 (G14 slice) · Cutout Render Layer for `background` Attribute

> 🔍 designed — **G10-owned feature; full spec in GROUP_10_COLOR_IMAGE.md (G10-1 section). G14 owns only the render-layer registration.**

The unified `background` attribute (transparent / black / colour) requires registering slot blocks on the **`cutout` render layer** client-side. This is the G14 deliverable that makes atlas blocks capable of transparency at all.

**Why it lives here:** every render-path change touches G14 (animation / off-atlas). The cutout registration is a client-only, one-call change in `CustomBlocksClient` — small footprint, but it must exist before `background = transparent` shows any visible effect.

**G14 work item:** add `BlockRenderLayerMap.putSimple(SlotBlock.INSTANCE, RenderLayer.getCutout())` (or the equivalent registry call for Fabric 1.21.1) in `CustomBlocksClient`'s `onInitializeClient`. One call, one line. Verify in-game: transparent blocks are actually see-through; existing solid blocks are unaffected (black default baked correctly).

**Full spec (decisions, touches, verify):** → GROUP_10_COLOR_IMAGE.md G10-1 section.


---

## G14-1 · Showcase / cycler block — rapidly cycle through custom-block textures (NEW)

> 🔍 designed — all decisions locked 2026-06-25; folded from ISSUES.md

### Decisions (locked 2026-06-25)

**Form: normal SlotBlock with `ShowcaseData` on `SlotData`.** Not a new BlockEntity, not a new block type. A showcase block is just any SlotBlock with `ShowcaseData` set (sibling of `AnimData`). This makes it ride `ClientSlotCache` + `AnimClock` wall-clock for free — every client renders the same block at the same instant, no server sync needed. HUD/tools/pack all work unchanged.

**Both "dedicated showcase block" and "toggle on any block" collapse to one mechanism:** the dedicated block is a SlotBlock created with `ShowcaseData` set; the toggle is flipping `ShowcaseData` on an existing block. Same field. True per-placement independence (same id, different config per copy) is intentionally OUT — it needs a BlockEntity and breaks the sync guarantee.

**Source / filter:** configurable per block; default = ALL custom blocks. Reuse proven `BulkScope` filter expressions (all / category:… / id:… / name:… / favorite / locked / id-list). Resolve client-side from `ClientSlotCache`.

**Behavior (all locked):**
- Adjustable **speed** (slow → strobe).
- **Random order** — time-seeded shuffle so "random + sync-all" still agrees across clients (deterministic from wall-clock bucket).
- **Smooth fade / crossfade** — within each period, crossfade outgoing→incoming slot texture by alpha; draw both cubes, fade one out/in.
- **Sync-all** — wall-clock buckets (`AnimClock.nowMs()`), already the animation sync mechanism; free.
- **Animated members (show animation playing):** when the cycled slot is itself an animated block, play its full animation while it is the "active" slot in the cycle. (Uses the same per-frame path as `AnimSlotBER`.)
- **Cycles in inventory too:** the showcase block's item icon in hotbar/inventory also cycles through its filtered pool. Non-trivial — item icon render hook needed — but wanted.

**Easter egg — `/cb create showcase`:** opens the Studio pre-filled with `ShowcaseData` defaults (filter=ALL, speed=medium, random order, fade on) + full configurability (filter picker, speed slider, order toggle, fade toggle, all exposed). Owner wants "full customizability and control and advanced" — not a one-click dump. This is the primary creation flow for showcase blocks.

**"+ more cool stuff" open bucket (NOT v1, NOT built):**
- Per-block tint/recolor while cycling.
- Speed ramp / pulse (ease in/out).
- "Slot machine" ease-out stop effect.
- Reactive speed (faster when player nearby / aimed at / redstone signal).
- Shuffle on redstone pulse.
- Show the cycling block's NAME on the HUD (ties G27-1 block-preview brick).

### Architecture

1. **Render:** extend `AnimSlotBER`. Reads `ShowcaseData` → picks the slot to show this frame (`index = (nowMs / periodMs)` for sequential; deterministic shuffle for random). Crossfade: draw both cube textures, interpolate alpha over `nowMs % periodMs`.
2. **`ShowcaseData` on `SlotData`:** fields = filter expression (BulkScope string) · speed (ms/period) · order (SEQUENTIAL / RANDOM) · fade (bool). Persist alongside `AnimData`.
3. **Filter resolution:** resolve the BulkScope expression client-side from `ClientSlotCache` (has id/category/favorite per slot) → produces a slot-index list; cache until `ClientSlotCache` updates.
4. **Sync-all:** `AnimClock.nowMs()` already used by animations — reuse; showcase clock ticks identically on every client.
5. **Pack model:** transparent off-atlas model (like animated slots) so BER draws it; `ServerPackGenerator` emits this for any slot with `ShowcaseData`.
6. **Command / Studio toggle:** `/cb showcase <id> [filter] [speed]` sets `ShowcaseData`; the Studio `ANIMATION` tab (or a new showcase section) exposes the config. `/cb create showcase` opens Studio pre-filled.
7. **Inventory icon cycling:** item icon render hook (client-side) reads `ShowcaseData` from `ClientSlotCache` and picks the current frame. Requires a custom item renderer override.

### Open items (confirm during build)

- Performance cap: strobe speed + many showcase blocks + crossfade = render cost; cap the rate / fade cost, confirm in-game stays smooth with many on screen.
- Filter source: confirm `ClientSlotCache` carries enough (id/category/favorite) to resolve every filter client-side, or restrict showcase filters to what's synced.
- Inventory icon cycling: confirm the Fabric item renderer API supports per-tick custom icons without breaking other item rendering.
- Studio tab: decide whether showcase config lives in the existing ANIMATION tab (as a sub-mode) or a new Section (discuss during build).

| Group | Scope | Status |
|---|---|---|
| G14 — Render (off-atlas/animation) | Both — client-side render, wall-clock synced; no server sync, no data risk | 🔍 designed — all decisions locked 2026-06-25; perf cap + inventory icon API + Studio tab placement = confirm during build |

**Related:** `AnimSlotBER` / `AnimFrameCache` / `StaticFrameCache` / `AnimClock` · `AnimData` (per-block mode pattern) · `BulkScope` (filter expressions) · `ClientSlotCache` (client-side filter resolution) · G27-1 (HUD block-preview / name overlay synergy) · G27-2 (`/cb create` entry point pattern)
**Touches:** `SlotData` (+ `ShowcaseData` field) · `AnimSlotBER` (cycle + crossfade draw) · `SlotManager` (setter) · client filter resolver over `ClientSlotCache` · `/cb showcase` command + `/cb create showcase` Studio pre-fill · `ServerPackGenerator` (transparent off-atlas model for showcase slots) · `AnimClock` (reused) · item renderer override (inventory cycling)
**Verify in-game:** placed showcase cycles through filtered set at set speed; random + sync-all agree across clients (same block same frame); fade looks smooth; toggle on a normal block makes it cycle; `/cb create showcase` opens Studio pre-filled; animated block shows animation while active in cycle; inventory icon cycles in hotbar.