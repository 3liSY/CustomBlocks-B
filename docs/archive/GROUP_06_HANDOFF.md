# Group 06 — Handoff (updated 2026-06-11)

> **Where we are:** M1 background remover was built, **tested in-game (5 images), found buggy, and
> fixed the same day** — smart black/white fill (kills black-on-black), tolerance ΔE cap 40→22, and
> pixelation (texture 64→128 + `/cb config texturesize`). Build green, jar in mods, **awaiting the
> in-game re-test** → `Reports/GROUP_06_TESTING_GUIDE.md` §1. Next real feature once that passes:
> **M3 hex recolour** (§6); M2 / M4 follow. This file stays the self-contained handoff.

---

## 1. Current state (built, green, in the mods folder — awaiting in-game test)

Jar: `build/libs/customblocks-1.0.0.jar`, copied to `%APPDATA%\.minecraft\mods\` (build 19:08).
Build is green (compile + `verifyMojibake` + `verifySound` + `verifyFileSize`). **Per the Golden
Rule, none of this is DONE until the developer confirms it in-game.**

### Safe fixes the developer is testing now (S1–S5)
- **S1 — Textures restored:** Deleter + Rainbow Rectangle reverted to the **old originals**
  (red jerry-can with white X; vertical rainbow bars).
- **S2 — Omni-Tool texture redrawn:** steel + gold tool head on a rainbow handle, bold outline,
  readable from a distance (one clearly-merged tool).
- **S3 — Lore simplified:** Omni-Tool + Deleter only (short, plain words). Shape/Rainbow lore is
  intentionally **not** rewritten yet — it lands with their real mechanics so it isn't done twice.
- **S4 — Old-system mentions cleaned:** Magic Items menu seed + mapping now use Omni-Tool /
  Rainbow Rectangle / Deleter (no more Lumina Brush / Chisel). Tools tab icon → Omni-Tool.
- **S5 — Creative tabs:** register Blocks→Tools so Tools sits right after Blocks. *(If it still
  looks wrong in-game, that's a Fabric tab-paging quirk — needs a screenshot, don't guess-change.)*

### Quick test checklist for S1–S5
- Creative **CustomBlocks Tools** tab: Omni-Tool icon; Omni-Tool, Rainbow Rectangle, Deleter, then
  the 8 colour/shape markers — all show real textures (no purple/black).
- Hover Omni-Tool + Deleter → short lore.
- `/cb` dashboard → Magic Items → shows Omni-Tool / Rainbow Rectangle / Deleter.
- Tools tab appears right after the CustomBlocks blocks tab.

---

## 2. After testing — the 5 tool mechanics (confirmed order: foundation-first)

These are **real features**, not tiny fixes, because the clean-room rebuild is missing the systems
they sit on. Build them **one at a time**, each its own careful pass, in this order:

| Step | Feature | Depends on |
|---|---|---|
| **M1** | CIE-LAB background remover + 3-mode config (none / background only / background + closed areas), wired into retexture, with lore + a Config-GUI row | — (foundation) |
| **M2** | Triangle tools → right-click fills the image **background with that colour** | M1 |
| **M3** | Square colour-variant + Black-square fallback: square looks for `blockID_<colour>` and swaps (clear hotbar error if missing); black square searches `blockID_black`, else falls back to the original block | M1, the variant concept |
| **M4** | Rainbow Rectangle **per-face URL paint**: right-click a face, paste a URL in chat, only that face changes | per-face texture system (biggest) |

---

## 3. Facts the next session needs (verified in the current code)

- `image/ImageProcessor.java` **only resizes/centres** — no background removal yet (its own comment
  says "BackgroundRemover (3 modes) + ColorReplacer come next"). M1 builds this.
- `core/TextureStore.java` stores **one PNG per block** (`slot_N.png`). No per-face textures.
- `core/SlotData.java` record = `(index, customId, displayName, glow, hardness, soundType,
  noCollision, category)` — **no per-face textures, no colour-variant fields.** M3/M4 will need new
  storage.
- Retexture flow: `command/handlers/CreationCommands.java` → `applyTexture()` downloads on a worker
  thread, calls `ImageProcessor.toBlockPng(raw, textureSize)`, saves via `TextureStore.save`, then
  `ResourcePackServer.updatePack()`. **M1 hooks the background step in here**, after `toBlockPng`.
- Config: `CustomBlocksConfig.java` (atomic save). Add `backgroundMode` here (load/save + a
  `normalize` helper like `didYouMean`). Config GUI row goes in `gui/chest/ConfigMenu.java`; the
  in-game setter goes in `command/handlers/ConfigCommands.java` (mirror the `silentpack` / `hud`
  sub-commands).

### M1 starting notes (algorithm already studied)
- The **old** `ImageProcessor.replaceBackground` (in `CustomBlocks/src/.../ImageProcessor.java`,
  ~line 539) is the reference: BFS flood-fill from **all border pixels** where a pixel matches the
  sampled corner colour by **CIE-LAB ΔE ≤ tolerance**, then a 1-px anti-fringe dilation. Helpers
  `rgbToLab` (~1449) and `deltaE` (~1482) are the LAB math to recycle cleanly. The **old config was
  the buggy part** — reimplement the config/wiring fresh, keep the algorithm.
- **3 modes:** `none` = leave as-is; `background only` = edge-connected flood-fill (above);
  `background + closed areas` = also remove **interior** pixels matching the bg colour (not just
  edge-connected) — i.e. a second pass over all pixels by ΔE, or skip the connectivity check.
- **DECIDED (2026-06-11):** `SlotBlock` renders **opaque** (`cube_all`, no render-layer reg in
  `ServerPackGenerator.cubeAllJson` / `CustomBlocksClient`) — so a removed background is a **flat
  colour, not transparency**. After the black-on-black bug, the developer chose **keep black + smart
  fill** (a near-black subject flips to a **white** fill) over switching to cutout/transparent. No
  render-layer change; backgrounds stay opaque, matching the old mod. (Transparency stays the
  fallback option if smart fill proves insufficient — it would need cutout + per-block layer reg.)

---

## 4. Build / run

```
# JDK 21 is required (machine default is newer and Gradle 8.8 won't run under it)
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
.\gradlew.bat build          # compile + gates
# then copy build/libs/customblocks-1.0.0.jar to %APPDATA%\.minecraft\mods\
```

Texture-generation helper (already used for the shapes/omni): `tools/gen_tool_textures.py`
(Python 3.12 + PIL present).

---

## 5. Task tracker state (updated 2026-06-11)

Status: ☑️ verified in-game · ✅ built/green, awaiting test · 🟡 partial (polish later) · ⏳ not built.

- ☑️ S1–S5 (textures, lore, Magic menu seed, tabs).
- ☑️ Omni-Tool **sneak-click works in the air** (added `use()` override).
- ☑️ `[CB]` prefix unified (black brackets everywhere; `HUD_PREFIX` removed).
- 🟡 ☑️ Item lore humanized for all tools + Squares/Triangles — verified, but PARTIAL (lore polish
  pass later, including dropping the word "marker" → they are **Squares / Triangles**).
- ☑️ Magic Items GUI → double chest (6 rows) + all 11 items seeded.
- ☑️ Tools tab: decision = keep **two** tabs (Fabric has no API to pin a tab's page).
- 🔧 **M1** background remover: CIE-LAB ΔE flood-fill, modes none/edges/closed, run BEFORE resize;
  mode names `NoBgRemove`/`BgRemove`/`BgRemove&More`; **`/cb tolerance 0-100`** + clickable mode
  prompt (0 = off); config-GUI row. Fixed 2026-06-11: smart black/**white** fill, ΔE cap 40→22,
  pixelation (64→**128** + `/cb config texturesize`). **Re-tested in-game 2026-06-11:** ☑️ smart fill
  (dark subject visible) + ☑️ pixelation confirmed; 🐞 light fringe halo on hard edges (Test 2 —
  widen anti-fringe / despeckle); 🐞 near-equal-colour subject still eaten (Test 4 — needs low tol or
  eyedropper); ☑️ photo case **decided: leave alone**. Open items in root `PROGRESS_LOG.md`
  2026-06-11 (late) handoff.
- ⏳ **A** eyedropper · **B** despeckle · **C** preview (background-removal upgrades; toggleable in
  config + `/cb config`; approved, not built).
- ⏳ **M2** triangle → create colour variant · **M3** square → swap variant · **M3 hex** change
  Red/Yellow/Green/Black hex + recolour-existing prompt + item-texture re-tint (see §6 spec). **NEXT.**
- ⏳ **M4** per-face URL paint.
- ⏳ **Texture-Size picker GUI** (developer-requested 2026-06-11): add **512px** (clamp 256→512);
  make `/cb config texturesize` + the config-GUI **Texture Quality** row open a chest sub-menu of
  sizes 16/32/64/128/256/512, each with an **average-MB-per-texture hover**; click sets + saves.
  Full spec in root `PROGRESS_LOG.md` 2026-06-11 (late) handoff. **Ready to build.**
- ⏳ Group 06 step 3 (held-block dynamic glow) — still deferred.

Full per-group testing guide: `Reports/GROUP_06_TESTING_GUIDE.md` (now covers every item with an
implemented/not mark). Session log: `docs/Finale Fix/PROGRESS_LOG.md`.

---

## 6. M2 / M3 colour-variant spec — recovered from the old code (for the clean rebuild)

> Read from old `item/ColorTriangleItem.java` (823 lines) + `item/ColorSquareItem.java` (458 lines).
> Recycle the **behaviour + texture math**, NOT the structure (the old files are huge, NBT-heavy, and
> carry a 16-family/40-alias colour vocabulary we do not need for 4 fixed colours). Build clean.

**Colours (4 fixed):** Red / Yellow / Green / Black. Each has a configurable hex —
old fields `triangleRedHex / triangleYellowHex / triangleGreenHex / triangleBlackHex` in config.
The item name shows the hex, e.g. `Black Square §8[#RRGGBB]`.

**M2 — Triangle = CREATE a colour variant.** Right-click a custom block:
- New block id = `stripColourSuffix(sourceId) + "_" + colourKey` (e.g. `mars` → `mars_black`); the
  source block is untouched. If the variant already exists, just give it (no reprocess).
- New texture = source texture with the **background recoloured to the triangle's hex** — flood-fill
  from the image border (CIE-LAB ΔE, corner-median sample), `edge` vs `full` fill mode, optional
  trapped-hole pass, 1.5× anti-fringe expansion. (Same family as M1's `BackgroundRemover`; reuse it.)
- Copy light/hardness/sound from the source onto the variant; process on a worker thread.
- **Shift+right-click → a confirm GUI first** (old `RecolorJob` / `openRecolorConfirmGui`).

**M3 — Square = SWAP to an existing colour variant.** Right-click a placed custom block:
- Resolve target id: scan id segments for a colour word → replace with the square's colour
  (`obsidian_black` + yellow → `obsidian_yellow`). If that block exists, set the placed block to it.
  If not, fall back to the base id; never auto-create. Clear hotbar error if nothing matches.

**M3 hex — change a colour's hex.** When `triangle<Colour>Hex` changes:
- Re-tint that colour's **Square + Triangle item textures** to the new hex.
- Pop a **GUI prompt**: "recolour already-made blocks of this colour with the new hex?" → if yes,
  batch-recolour every existing `*_<colour>` variant via a **direct hex swap** (old
  `recolourTextureDirectSwap`: replace pixels within ~30 RGB of the old hex with the new hex — no
  flood-fill, so design pixels are safe), then rebuild the pack.

**Prerequisite gap (new mod):** today's Squares/Triangles (`ShapeMarkerItem`) only *announce* a tag;
they don't create/store variants. M2/M3 build the real create/swap + the per-colour hex store first.

**Known old bugs to NOT reintroduce:** batch recolour must force **edge** mode (never player "full"),
broadcast the pack sync **after** the batch finishes, keep the pack debounce short (purple-block
guard). (See `CLAUDE.md` §7 pitfalls.)
