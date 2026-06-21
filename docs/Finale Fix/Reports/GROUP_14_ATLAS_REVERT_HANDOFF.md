# Group 14 — Atlas Revert Handoff (the GIF-muffle fix)

> **For the next chat. Read this first, then `ADR-012`.** This is the agreed fix for the placed-GIF
> "muffle/speckle" that ~10 off-atlas attempts never solved. Decision: **delete the off-atlas renderer,
> render through the vanilla atlas + `.mcmeta` (the old mod's proven way). GIFs at 256px.**
>
> 🚦 **Golden rule (CLAUDE.md):** nothing is ✅ until the owner confirms in-game. Build-green = compiles +
> gates only. Owner is a non-programmer who's been burned — small slices, confirm each before the next.

---

## Why (one paragraph)

The off-atlas renderer uploaded textures with **mipmaps OFF**; a sharp texture minified to block size
with no mipmaps **aliases** — that *is* the speckle. The atlas builds mipmaps for free, which is why the
old mod (no custom renderer, `cube_all` + `.mcmeta`, ≤256px) looked fine, and why **this** mod's atlas
path was already owner-confirmed (Testing Guide §2). Animated sprites cost only one frame of atlas space,
so 256px GIFs don't overflow. Full reasoning: `docs/adr/ADR-012-revert-to-atlas-mcmeta-rendering.md`.

---

## Exact reference map (verified 2026-06-21 — line numbers may drift, grep to confirm)

### A. Delete these 7 client files (the off-atlas renderer)

```
src/main/java/com/customblocks/client/render/AnimSlotBER.java
src/main/java/com/customblocks/client/render/AnimFrameCache.java
src/main/java/com/customblocks/client/render/StaticFrameCache.java
src/main/java/com/customblocks/client/render/SlotItemRenderer.java
src/main/java/com/customblocks/client/render/OffAtlasImage.java
src/main/java/com/customblocks/client/render/OffAtlasBgState.java
src/main/java/com/customblocks/client/render/SlotBeBackfill.java
```

**Keep** (separate Arabic feature — do NOT touch): `ArabicLetterBlockEntityRenderer`,
`ArabicLetterItemRenderer`, `ArabicPrewarm`.

**Keep** (harmless on atlas; removing risks existing worlds — optional later cleanup, NOT now):
`block/AnimSlotBlockEntity.java`, `block/AnimSlotRegistry.java`, and `SlotBlock`'s `BlockEntityProvider`.
Their header comments mention `AnimSlotBER`; that's cosmetic — leave or tidy, your call.

### B. Delete the `transparent` toggle (off-atlas-only feature)

```
src/main/java/com/customblocks/network/payloads/TransparentBgPayload.java
src/main/java/com/customblocks/command/handlers/RenderConfigCommands.java
```
Then remove every reference:
- `command/handlers/ConfigCommands.java:82` → `RenderConfigCommands.register(root);`
- `gui/chest/ConfigMenu.java:44` + the slot-18 "Block Background" tile it builds
- `CustomBlocksConfig.java:61` → `public static volatile boolean transparentBackground`
- `CustomBlocksConfigStore.java:58` (load) and `:104` (save)
- `CustomBlocksMod.java:92-93` (payload S2C registration) and `:248` (JOIN send)
- `CustomBlocksClient.java:138-145` (receiver + DISCONNECT reset)

### C. `CustomBlocksClient.java` — remove off-atlas registrations

- lines **192-194** — `BlockEntityRendererFactories.register(AnimSlotRegistry.BLOCK_ENTITY, AnimSlotBER::new)`
- line **200** — `SlotBeBackfill.register();`
- lines **205-211** — the `SlotItemRenderer` loop over slot items
- lines **138-145** — `TransparentBgPayload` receiver (covered in B)
- **Keep** the Arabic renderer registrations (213-221).

### D. Remove the cache-clear calls to the deleted caches

- `client/packsync/ClientPackReceiver.java:156-157` — `AnimFrameCache.clear(); StaticFrameCache.clear();`
  (and the header note on line 15)
- `client/ResourcePackGenerator.java:110-111` — same two `.clear()` calls

### E. `network/ServerPackGenerator.java` — flip the pack back to the atlas

In `emit(...)` the per-slot model selection (~lines 110-140):

- **Animated branch** (`anim.isAnimated() && TextureStore.has(i)`, ~112-120):
  - block model → **`cubeAllJson(key)`** (was `invisibleBlockModelJson`)
  - **keep** the `.mcmeta` sidecar line (`tex(key)+".mcmeta", mcmetaBytes(anim)`) — this is what animates it
  - **remove** the `builtinEntityItemJson()` item-model line → the default item model at the end of the
    loop (`itemJson(MOD_ID+":block/"+key)`) now handles the icon
- **Static full-cube else branch** (~129-137):
  - block model → **`cubeAllJson(key)`** (was `invisibleBlockModelJson`)
  - **remove** the `builtinEntityItemJson()` line → default item model handles it
- **Re-add the helper** (it was deleted in Step 3):
  ```java
  private static byte[] cubeAllJson(String key) {
      JsonObject tex = new JsonObject();
      tex.addProperty("all", MOD_ID + ":block/" + key);
      JsonObject m = new JsonObject();
      m.addProperty("parent", "minecraft:block/cube_all");
      m.add("textures", tex);
      return GSON.toJson(m).getBytes(StandardCharsets.UTF_8);
  }
  ```
- **Delete now-unused helpers:** `invisibleBlockModelJson`, `builtinEntityItemJson`,
  `BUILTIN_ENTITY_ITEM_JSON` (grep first — confirm no other caller).
- **Leave unchanged:** shaped (`shapeModelJson`) and per-face (`cubeFacesJson`) branches — already atlas.

### F. Texture size — GIFs at 256

Mostly already wired:
- `image/AnimationDecoder.java` → `ATLAS_MAX_SIZE = 256` (exists).
- `command/handlers/AnimCommands.java:108-109` → already caps: `min(ATLAS_MAX_SIZE, textureSize)`.
- **Verify** the studio create path also caps animated strips: grep callers of `AnimationDecoder.decode`
  and ensure the animated/atlas one passes `Math.min(AnimationDecoder.ATLAS_MAX_SIZE, textureSize)`
  (check `BlockCreationStudioScreen` / the create command). Static keeps `CustomBlocksConfig.textureSize`.

---

## Build order (small slices — build green + owner-confirm each)

> ⚠️ Existing animated/static blocks were baked with the off-atlas (invisible) model. They need a **pack
> regen** to pick up the new `cube_all` model: rejoin / restart, or re-create one test block. Tell the owner.

**Slice 1 — pack flip (E + re-add `cubeAllJson`).** Smallest change that restores visuals. Build green.
*Owner test:* a GIF block placed in the world **animates and is crisp (no speckle)**; hand/inventory/creative
all show it animating. This is the moment the muffle should be gone.

**Slice 2 — delete the 7 renderer files (A) + remove registrations (C) + clear-calls (D).** Build green.
*Owner test:* same as Slice 1 still true; blocks place/break normally; no errors in `latest.log`.

**Slice 3 — remove the `transparent` toggle (B).** Build green.
*Owner test:* `/cb config` chest GUI no longer shows the Block Background tile; `/cb config transparent`
is gone; nothing else changed.

**Slice 4 — verify the 256 GIF cap (F).** Build green.
*Owner test:* a freshly created GIF is 256px and crisp; `/cb config texturesize 256` static block crisp.

(Optional, later — not this pass: remove `AnimSlotBlockEntity`/`AnimSlotRegistry` + `BlockEntityProvider`
from `SlotBlock`. Touches block registration; test existing worlds carefully if you do.)

---

## In-game test checklist (owner)

| ✓ | Proves |
|:--:|---|
| ⬜ | Placed GIF block (nyan/nyan1) **animates and is crisp** — no speckle/muffle, close AND at distance |
| ⬜ | Same GIF in **hand / hotbar / inventory / creative / `/cb list`** animates |
| ⬜ | A **static** `/cb create` block is crisp and solid (walk on it, break it, particles right) |
| ⬜ | **Server == singleplayer** — looks identical in both |
| ⬜ | **Shaped / per-face** blocks still render correctly (must NOT go invisible — they stayed on the atlas) |
| ⬜ | **Glow** (`/cb setglow`) still works |
| ⬜ | Relog / reload world → everything still there, no `latest.log` errors |

If a test fails: step + what happened vs expected + the link used + last ~20 lines of `latest.log`.

---

## Gotchas

- **Render layer:** `cube_all` draws on the SOLID layer. Textures are baked opaque (black bg), so this is
  correct and matches the old mod. Transparent-area GIFs will show the baked black bg — expected (see ADR-012).
- **`put(sink, written, …)` skips duplicates** — that's why removing the `builtinEntityItemJson` line lets
  the loop-end default item model take over cleanly. Don't add a second item-model write.
- **Don't delete the Arabic renderers.** They share the `client/render` package but are a different feature.
- Build: `.\gradlew.bat build` (Temurin JDK 21). Gates: `verifyFileSize` (≤500/.java, ≤400 cmd, ≤300 *Config),
  `verifyMojibake`, `verifySound`.
