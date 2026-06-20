# 🧪 Group 14 — Animation, Video & Display Blocks — Testing

> 🟢 Build green = compiles + gates pass. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar` — already rebuilt + installed; restart Minecraft to load it.

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Partial |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟥🟥🟥🟥 · Part A confirmed · Phase 2 built, pending in-game · muffle + earth 🟡 deferred to Phase 1b |
| **Last tested** | 2026-06-19 (§2 confirmed) |
| **Jar** | 1.0.0 — rebuilt + installed 2026-06-20 (Fix 4 Texture-tab landing). Muffle + earth deferred to Phase 1b. |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **Bug fixes (2026-06-20):** **`/cb anim`** lists **animated blocks only** + click lands on the Animation tab; the **Animation tab is redesigned** (grouped Speed/Loop/Smoothing/Trim + playback bar). *(Crispness + earth speed → 🟡 deferred to Phase 1b renderer.)* | §0 |
| 🎯 **TEST NOW** | **Phase 2 — Animation tab + live preview + edit-EVERYTHING.** A GIF block has a dedicated **Animation** tab in `/cb create` that **plays live**; `/cb anim <id>` opens that block as a **full studio** — rename its id, swap its picture/GIF, change colour/shape/glow/sound/etc + animation — then **Save changes** | §1 |
| ✅ Confirmed 2026-06-19 | GIF/WebP animated blocks · studio "Load texture" · animates everywhere | §2 |
| 🟡 Polish later | loading a NEW GIF resets its animation settings (adjust + Save again); animated block's background can't be re-filled without a new image; a re-skin isn't undoable; smoothing shows as frame-swap in the preview | §3 |
| ⏳ Not built | Phase **1b** hybrid renderer (the FULL crisp fix — §0 is the interim) · Phases 3–10 (timeline, playback polish, auto-perf, grading, video import, walls, live-data, redstone) | §4 |

> ⚠️ **Phase 1b redesigned from scratch (2026-06-20).** All previous "hybrid" / `screen_test` / LOD-fallback
> attempts are retired — they patched the atlas pipeline which cannot be made crisp. New approach: own-texture
> `BlockEntityRenderer` (no atlas for placed blocks), proven by Minecraft maps + Slideshow mod. See §5 for the
> full build plan including what to delete first. The muffle + choppiness fixes are NOT in the current jar —
> they land when Phase 1b is built and confirmed in-game.
>
> 🎯 **Phase 1b — Slices A + B + C are BUILT + installed (2026-06-20).** Placed animated blocks are now drawn
> **off-atlas** (own texture, mipmaps OFF) — the actual crispness fix. **PROVEN root cause:** the baked strip
> PNGs in your game files are crisp; Minecraft's block atlas was adding the muffle. Test in **§5 → Slice B/C**:
> restart MC, look at an animated block close-up — is it finally sharp, and does it animate? ⚠️ If an animated
> block shows **invisible**, the renderer couldn't read its strip — report it. (Static blocks may still look
> muffled if the atlas is overflowing — that's a separate, smaller follow-up.)

---

# 🎯 Test now

## §0 · The bug fixes (round 2 — built + installed 2026-06-20) — test together

> 💡 **What this proves:** the round-1 follow-ups — GIF speed, the WebP muffle, the calmer tab — plus a note when
> you `/cb anim` a non-animated block. `/cb anim` list already passed (✅ below).
>
> ⚠️ **Re-create your test blocks first.** The picture is baked when the block is made, so `earth`/`nyan` from
> round 1 are still the old bake. `/cb delete earth` · `/cb delete nyan`, then re-create below.

### Fix 1 — Earth crispness + speed — 🟡 PARTIAL, deferred 2026-06-20

- **Status:** owner-decided to **come back later**. The atlas path can't be made fully crisp/smooth (vanilla
  mipmaps); the real fix is the own-texture world renderer (**Phase 1b**, not built). Earth on the atlas is
  interim: 32 frames at 256px, plays the clip's real speed. Not a test target this round.

### Fix 1b — the WebP muffle (nyan-style) — 🟡 PARTIAL, deferred 2026-06-20

- **Status:** owner-decided to **come back later**. Root cause confirmed = the vanilla **block atlas + mipmaps**;
  no atlas-side setting escapes it (capping at 256 helped but did not remove it). The crisp fix is the
  own-texture renderer (**Phase 1b**) — proven by the `screen_test` block. Not a test target this round.

### Fix 2 — `/cb anim` animated-only list ✅ PASSED 2026-06-20

- `/cb anim` (no id) → "Animated Blocks" list, click → that block's Animation tab. Confirmed by owner.

### Fix 3 — the Animation tab is calm + plain-language (v2)

- **③ Open it** 🔴 — `/cb anim earth`.
  - ✅ **Pass:** no fps/ticks jargon. Just **Speed** (Slower · **Normal** · Faster), **Loop** (Forward · Bounce ·
    Reverse), **Smooth motion** (On/Off), **Trim**, and a moving playback bar. Feels uncluttered.
- **④ Controls work** 🔴 — **Normal** = real speed; **Slower/Faster** nudge it; Loop modes + Trim react live.

### Fix 4 — a note when the block isn't animated 🎯 RE-TEST (tab polish) 2026-06-20

- `/cb anim <non-animated id>` → warns + opens the studio. **Polish applied 2026-06-20:** it now lands
  **straight on the Texture tab** (was Identity) and the note is reworded ("isn't animated yet — opened the
  studio on the Texture tab. Paste a GIF/WebP and hit Load texture to animate it.").
  - ✅ **Pass:** the studio opens on the **Texture** tab and the chat note matches.

> ↩️ **Undo the test:** `/cb delete earth` · `/cb delete nyan`

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟡 | ① | Earth crisp + speed — **deferred** (needs Phase 1b renderer) |
| 🟡 | ② | animated WebP muffle — **deferred** (needs Phase 1b renderer) |
| ✅ | — | `/cb anim` animated-only list + correct click |
| 🟥 | ③④ | Animation tab is calm/plain and controls work |
| 🟥 | — | non-animated `/cb anim` now opens on the **Texture** tab (re-test) |
| — | **renderer work** | crispness + smoothness deferred to the own-texture renderer (Phase 1b) |

---

## §1 · Phase 2 — the Animation tab, live preview & editing existing blocks (full editor)

> 💡 **What this proves:** animation is now **edited in a real GUI with a live-playing preview**, and
> `/cb anim <id>` re-opens any animated block as a **full studio** — you can change *everything*: rename
> the id, swap the picture/GIF, recolour, reshape, retune the animation — all from one screen.

### Part A — make a new animated block in the studio

- **① Open the studio** 🔴 — `/cb create` (no args).
- **② Load a GIF** 🟡 — **Texture** tab → paste a GIF link → **Load texture**. Badge reads **"✔ animated"**.
  `https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif`
- **③ The Animation tab unlocks** 🔴 — it was greyed before loading; now it's white. Click it.
  - ✅ **Pass:** the preview cube **plays the animation live** (frames cycle, not a still image).
- **④ The controls change the preview live** 🔴 —
  - **Speed**: click `5` then `30` → the preview visibly slows / speeds up. `Original` = the clip's own timing.
  - **Loop**: `Bounce` → plays forward-then-back; `Reverse` → plays backward.
  - **Smooth**: On / Off (preview swaps frames either way — the real blend is in-world; see §3).
  - **Trim**: Start/End `−`/`+` shorten the played range; `Full` resets to all frames.
- **⑤ Publish** 🟡 — give it an **id** + **name** (Identity tab) → **Create & Publish** → place it → it animates.

### Part B — edit EVERYTHING with `/cb anim <id>`

> Place the test block from ⑤ (or `g14anim`) in the world first, so you can confirm the placed copy
> survives a rename + updates after each save.

- **⑥ Re-open it** 🔴 — `/cb anim <id>`. The studio opens **on the Animation tab**, that block loaded,
  the preview **playing its real frames**.
- **⑦ Tune the animation + save** 🔴 — set a new Speed/Loop/Trim → **Save changes** (button says "Save
  changes", not "Create & Publish").
  - ✅ **Pass:** chat says "Saved changes to …"; the placed block updates after the pack reloads; re-open
    → the controls show the values you just saved.
- **⑧ Rename the id (safe)** 🔴 — **Identity** tab → change the **id** to something new → **Save changes**.
  - ✅ **Pass:** chat confirms; the **block you placed in the world does NOT vanish** (it keeps its look);
    `/cb anim <new-id>` opens it; the old id is gone from `/cb list`.
- **⑨ Swap the picture** 🔴 — re-open it → **Texture** tab → paste a **different** image link → **Load
  texture** → **Save changes**.
  - ✅ **Pass:** the placed block shows the **new image**. (A static link makes it a still block; a GIF link
    makes it animate — and back again.)
- **⑩ Change colour/shape too** 🟡 — set a background colour / a shape / glow → **Save changes** → they all
  apply together.
- **⑪ Picker** 🟡 — `/cb anim` (no id) → the **block list** opens so you can pick one.

> ↩️ **Undo the test:** `/cb delete <your test id / new-id>` · `/cb delete g14anim`

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ③ | Animation tab unlocks + preview plays live |
| 🟥 | ④ | Speed / Loop / Smooth / Trim change the live preview |
| 🟥 | ⑥⑦ | `/cb anim <id>` opens an existing block; tuning persists (survives re-open) |
| 🟥 | ⑧ | **rename the id — placed blocks survive, nothing breaks** |
| 🟥 | ⑨ | **swap the picture/GIF on an existing block** (static↔animated) |
| 🟥 | ⑩ | colour / shape / glow change in the same save |
| 🟥 | ⑪ | `/cb anim` (no id) opens the block list |
| — | **0 / 7** | |

---

# ✅ Confirmed

## §2 · Part A — animated blocks — confirmed 2026-06-19

> You confirmed **"they work."** Pack animation, so a block animates **everywhere** (world, hand, inventory,
> creative tab, `/cb list`). Phase 2 (§1) replaces the old `/cb anim` chat card with the GUI, but the
> underlying animated-block behaviour below is unchanged.

**Re-check anytime:**

- **① Studio GIF** — `/cb create` → **Texture** → paste a GIF link → **Load texture** → badge "✔ animated".
- **② Animates everywhere** — hand · hotbar · CustomBlocks creative tab · `/cb list`.
- **③ Loop modes** — set Bounce then Reverse (now via the Animation tab) → plays accordingly.
- **④ Survives a restart** — set a speed/loop, quit + reload → still animates the same.
- **⑤ WebP + share links** — animated WebP plays if frames are exposed; Tenor/Giphy page links resolve.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | studio "Load texture" works on GIF/CDN links |
| ✅ | ② | animates everywhere (hand/inventory/creative/list) |
| ✅ | ③ | loop / bounce / reverse |
| ✅ | ④ | persists after restart |
| ✅ | ⑤ | WebP decodes; page links resolve |
| — | **5 / 5** | |

---

# 🟡 Polish later

- **Loading a NEW GIF resets its animation settings** — a fresh clip has its own frame count + timing, so it
  comes in at default speed/loop. Adjust on the Animation tab and **Save changes** again to set them.
- **An animated block's background can't be re-filled on its own** — to change the colour behind an animated
  block you load a new image (the bg applies during that re-bake). Static blocks re-fill from the colour alone.
- **A picture swap (re-skin) isn't undoable** — `/cb undo` doesn't revert a texture change (same as
  `/cb retexture`). Settings-only edits without a rename are still undoable.
- **Smoothing in the preview is a frame-swap** — the real cross-fade between frames happens in the world
  (Minecraft's `.mcmeta` interpolation), so the studio preview just swaps frames. Functionally correct.

---

# ⏳ Not built

| Phase | What it'll do |
|:--:|---|
| ~~1a~~ | ✅ built 2026-06-20 — decoder invert (interim crisp). See §0 Fix 1. |
| **1b** | **Own-texture renderer — REDESIGNED FROM SCRATCH (2026-06-20).** See §5 below for full plan + deletion steps. |
| ~~2-redesign~~ | ✅ built 2026-06-20 — animated-only `/cb anim` list + Animation tab redesign. See §0 Fix 2 + 3. |
| 3 | **Timeline editor** — per-frame thumbnails, drag-trim, reorder/delete/duplicate, frame→static |
| 4 | **Playback polish** — de-sync, seamless-loop crossfade, speed ramp, play-once |
| 5 | **Auto performance** config — Always / Distance / Auto |
| 6 | **Color grading + chroma-key + smart framing** |
| 7 | **Universal video import** — ffmpeg-if-present, GIF/WebP fallback |
| 8 | **Video wall** — one synced screen engine, N×M, fit modes |
| 9 | **Live data blocks** — clock / date / countdown / live stats / marquee |
| 10 | **Redstone-reactive for ALL blocks** + interactive + triggers + sync channels + auto-emissive |

> Phase 3 builds on Phase 2's edit-load (it rewrites the same frame-strips), so confirm §1 in-game first.

---

---

# ⏳ Phase 1b — Own-Texture Renderer (redesigned from scratch 2026-06-20)

> **Why this exists:** muffling and choppy animation were never fixed because every previous attempt
> patched the atlas pipeline. The atlas applies mipmaps to all sprites — no size, .mcmeta, or blur
> setting can stop that. The only fix is to not use the atlas for placed animated blocks.
>
> **Proof this approach works:** Minecraft's own maps use this exact path (`NativeImageBackedTexture`,
> mipmaps OFF, registered straight to TextureManager). The Slideshow mod (1.21, 1M+ downloads) uses
> the same concept for GIFs on placed blocks — production-confirmed crisp and smooth.

---

## Step 0 — Delete the old prototype code FIRST

Before writing one line of Phase 1b code, delete these 5 files. They are the `screen_test` cluster:
unconfirmed prototype code that was never proven in-game, now superseded.

```
src/main/java/com/customblocks/block/ScreenTestBlock.java
src/main/java/com/customblocks/block/ScreenTestBlockEntity.java
src/main/java/com/customblocks/block/ScreenTestRegistry.java
src/main/java/com/customblocks/client/render/ScreenTestBlockEntityRenderer.java
src/main/java/com/customblocks/client/render/ScreenTestImages.java
```

Also remove their registrations from `CustomBlocksMod.java` and `CustomBlocksClient.java`
(search for `ScreenTest` — every reference goes). Run `.\gradlew.bat build` and confirm gates still pass
before touching anything else.

---

## Step 1 — What gets built (3 new files)

| File | Package | Role |
|---|---|---|
| `AnimSlotBlockEntity.java` | `block/` | Holds `slotIndex`. No server sync. One BlockEntityType for all 1028 slots. |
| `AnimFrameCache.java` | `client/render/` | Client singleton. `slotIndex → NativeImageBackedTexture`. One texture per slot (not per frame). Frame advance = update NativeImage pixels + `texture.upload()`. `setFilter(true, false)` = linear, NO mipmap. 512px. Reuses `AnimationDecoder` + `AnimData` unchanged. Clears on resource reload. |
| `AnimSlotBER.java` | `client/render/` | BlockEntityRenderer. Draws 6 faces via `RenderLayer.getEntityCutoutNoCull(textureId)`. Frame timing: `(tick + partialTick) * 50` ms → current frame index. Smooth at 60fps, NOT tied to 20 ticks/s. Respects `AnimData.playback()` for loop/bounce/reverse. |

**What stays completely unchanged:** `AnimationDecoder`, `AnimData`, all studio/command code, all atlas
code for inventory/hand (`.mcmeta` stays — inventory animation still works).

**One model change:** placed animated block model JSON → `"parent": "minecraft:block/air"` (transparent).
Item model JSON is separate and unchanged (hand/inventory still show the atlas texture).

---

## Step 2 — Build order (small slices, confirm each in-game)

**Slice A — delete + scaffold:**
1. Delete the 5 screen_test files (Step 0).
2. Create `AnimSlotBlockEntity` (just the class + registration — no rendering yet).
3. Build green → confirm in-game that animated blocks still place/break normally.

**Slice B — cache + renderer:**
4. Create `AnimFrameCache` (decode from source, upload NativeImageBackedTexture).
5. Create `AnimSlotBER` (6-face render, frame 0 only first — no animation yet).
6. Wire transparent placed model for one test block.
7. Build green → confirm in-game: does the placed block show the image crisp? No muffle?

**Slice C — animation:**
8. Add ms-based frame timing to `AnimSlotBER`.
9. Build green → confirm in-game: does it animate smoothly? No choppiness?

**Slice D — full:**
10. Wire to all animated slots. Test loop/bounce/reverse. Test restart survival.

---

## §5 · Phase 1b — Test checklist (fill in after each slice)

> Run these ONLY after the relevant slice is built and the jar is installed.

**Slice A — safe plumbing 🎯 TEST NOW (built + installed 2026-06-20)**

> 💡 **What this proves:** every slot block now carries a (data-less) BlockEntity — the hook the world
> renderer needs. **No visual change yet** (atlas model still draws everything). This test only confirms
> the BlockEntity-on-every-block change did NOT break normal placement/breaking. Use any animated **or**
> static custom block.

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | A1 | A custom block **places** normally (animated or static) |
| 🟥 | A2 | It **breaks** normally and drops itself |
| 🟥 | A3 | It still **looks the same** as before (atlas texture, animates if it was animated) |
| 🟥 | A4 | Place several, **relog / reload the world** → all still there, intact, no errors in `latest.log` |

> ⚠️ If a block fails to place, vanishes, or the log shows a BlockEntity error → stop and report (Slice A is
> the risky change; this is exactly what it isolates).

**Slice B — crispness**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | Placed animated block shows image — no muffle, no speckle, sharp at close range |
| 🟥 | ② | Inventory / hand still show the block normally (atlas still works for those) |
| 🟥 | ③ | Block is still solid (you can walk on it, break it, select it) |

**Slice C — smoothness**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ④ | Animation plays smoothly — no steppy/choppy frames |
| 🟥 | ⑤ | Speed setting actually changes playback speed |
| 🟥 | ⑥ | Loop / Bounce / Reverse all work correctly |

**Slice D — completeness**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ⑦ | Works on all animated blocks, not just the one test block |
| 🟥 | ⑧ | Survives a server restart (no broken block after reload) |
| 🟥 | ⑨ | Multiple different animated blocks in the same world — all play correctly |

---

## 🆘 If a test fails

- 🔢 Step number (e.g. ③)
- 👀 What happened vs expected (📸 a screenshot helps) — and the **link** you used
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup

```
/cb delete g14anim
/cb delete g14webp
/cb delete g14awebp
```
