# Progress Log

One entry per work session. Newest at the top. See the Engineering Bible §9.2.

---

## 2026-06-11 (HANDOFF → START GROUP 07 — Bulk Operations) — read this first

Developer wrapped the bug-fix session (below) and called it: **start Group 07 in a fresh convo.**

### State right now (trust this)
- **Build green** (compile + 3 gates). Jar in `.minecraft\mods\customblocks-1.0.0.jar` (**12:46**).
- **BUG 1 fixed & kept** — `/cb config hex … #RRGGBB` (with `#`) works in chat + Config GUI.
- **BUG 2 (silent pack) REVERTED** — do NOT re-apply the join-delay. Developer is running with
  `/cb config silentpack OFF` so the resource-pack screen shows on every edit (that's what makes
  edits visibly apply on their current data). See the entry below for the full why.
- **Nothing committed** (git has only the 2 old commits; all work is working-tree). Don't commit
  unless the developer asks.

### ⚠️ Environment is contaminated — do NOT chase texture rendering this session
The developer has been swapping old/new jars on one world, so `config/customblocks/` mixes old
data (root `slot_N.png`, `.dat` files, 20 MB `slots_old.json`) with new (`textures/slot_N.png`,
3.5 KB `slots.json`). Also an OLD **client-side** pack `.minecraft/resourcepacks/CustomBlocks`
(888 old textures, same `customblocks` namespace) is still enabled and overrides the new server
pack. **Result: block textures render wrong/old — this is NOT a code bug** (the new pipeline is
correct: `slots.json` clean, served zip has the right textures, paths read only from
`textures/slot_N.png`). **Resolution = the deferred `.dat` migration.** Until then, any visual
texture test is unreliable. Group 07 was chosen specifically because it avoids this.

### Group 07 — Bulk Operations (the task)
- Spec: `docs/Finale Fix/GROUP_07_BULK_OPERATIONS.md`. Tests: G07.1–G07.9 (verified by command
  output + `/cb list`, not visuals — contamination-safe).
- Scope: `bulkdelete / bulkrename / bulkreid / bulkproperty / bulkexport / bulkmove /
  bulkduplicate / bulklock / bulkunlock / bulkfavorite / bulkunfavorite / bulksound /
  bulkrecolor`, plus `/cb bulkgui` and the `bulkConfirmThreshold` (default 10) confirm guard.
- **Build only what's safe now:** SKIP `bulkshape` (needs Group 08, not built). `bulkrecolor`
  must be **edge-mode ONLY** — hardcoded, no full-mode option (CLAUDE.md pitfall: batch full-mode
  recolor destroys designs). Each bulk op = ONE undo entry for the whole batch.
- Filters: `category:<name>` · `id:<prefix>` · `name:<substring>` · `all` (always confirms).
- Architecture (CLAUDE.md §5): new command handlers under `command/handlers/` (≤400 lines, split
  if needed), registered via `CommandRegistrar`; all slot writes through `SlotManager` /
  `SlotDataStore`; GUI as a standalone `gui/chest/` menu. Check what already exists first
  (LockManager, FavoritesManager, BlockExporter, category system are already in the tree).

### First steps for the next session
1. Read this entry + CLAUDE.md. Read `GROUP_07_BULK_OPERATIONS.md` fully.
2. Grep for any existing `bulk*` handlers/commands before writing (avoid dupes).
3. Propose a SMALL first slice (≤5 items — e.g. the filter parser + `bulkproperty` + confirm
   guard + undo) and get the developer's OK before coding. Hand back for in-game test (G07.1–G07.4).
4. Keep `silentpack OFF` in mind: the developer accepts the pack screen on edits for now.

---

## 2026-06-11 (BUG 1 FIXED ✅ · BUG 2 silent-pack change REVERTED ⏪) — final jar 12:46

Build green (compile + 3 gates). Final jar → `.minecraft\mods` **12:46**.

### Test results (developer, in-game)
- **BUG 1 — DONE ✅.** `/cb config hex red #FF0000` (with `#`) works in chat AND the Config GUI.
  Kept. (greedyString args + `#`-strip in the GUI dispatches.)
- **BUG 2 — REVERTED ⏪ at developer's request. DO NOT re-apply without rethinking.**
  - The 250ms-delay fix DID make rejoin silent (proved via an `[SP]` diagnostic build:
    `silent=true` at the pack prompt). BUT making silent *reliable* broke the developer's
    edit workflow: with the pack screen suppressed, edits (hex change / new block / any edit)
    showed **no dialog and no visible change** — because on the current CONTAMINATED data the
    silent auto-accept doesn't visibly reload, while the manual "Yes" path did.
  - Reverted `CustomBlocksMod` JOIN hook back to immediate `sendToPlayer` and removed the added
    `ResourcePackServer.sendToPlayerDelayed`. Now == the round-2 state where edits worked.
  - **Workaround given to developer:** `/cb config silentpack off` → forces the resource-pack
    screen back on every pack send; accept it and the edit shows.
  - **Real root issue is the contamination** (see below), not the silent timing. Revisit silent
    pack only AFTER the `.dat` migration, on clean data — and confirm the silent auto-accept
    actually triggers a visible client reload before relying on it.
- Diagnostic `[SP]` logs were added to root-cause, then stripped.

### NOT a bug — environment contamination (texture rendering)
Developer saw a newly-created block (`tesp`, slot 22) render as an OLD-version texture. Root-caused
to **two CustomBlocks packs colliding**, NOT mod code:
- New mod is correct: `slots.json` clean (slot 22 = tesp), served `customblocks_pack.zip` contains
  the correct new `slot_22.png` (9259 B), `httpHost 127.0.0.1:8123` reachable. `TextureStore` +
  `ServerPackGenerator` only ever read `config/customblocks/textures/slot_N.png`.
- An OLD **client-side** pack `.minecraft/resourcepacks/CustomBlocks` (888 old slot textures, same
  `customblocks` namespace + paths) is still enabled and collides. Disabling it breaks ALL blocks
  (old + new) on the current mixed data — so it's left as-is.
- Cause: jar-swapping mixed old data (root `slot_N.png`, `.dat` files, 20 MB `slots_old.json`) with
  new data. **Resolution deferred to the `.dat` migration** (see [[project_customblocks_dat_migration]]).
  Texture rendering can't be cleanly tested until then. Developer agreed to leave it.

### 🐞 BUG 1 — `#` in hex args (`/cb config hex`, `recolorvariants`) → Brigadier parse error
- Root cause confirmed: `StringArgumentType.word()` rejects `#`. Both affected args are the
  FINAL argument, so switched them to `greedyString()` (safe).
- `HexCommands.register`: `value` arg of `config hex` and `oldhex` arg of `recolorvariants`
  → `word()` → `greedyString()`.
- Belt-and-braces: the GUI dispatch strings now strip `#` before running the command —
  `HexColorsMenu.applyHex` (`norm.replace("#","")`) and the `HexRecolorConfirmMenu` Yes button
  (`oldHex.replace("#","")`). `normalizeHexColor` already tolerates `#RRGGBB` and `RRGGBB`.

### 🐞 BUG 2 — silentpack ON, rejoin still shows the vanilla "download pack?" dialog
- Verified the join hook ALREADY sent `SilentPackPayload` before the pack (the round-2 jar had
  this and still failed) → plain send-order is insufficient. The client's flag receiver
  double-defers `SilentPackState.set` onto its main thread, so a same-tick race on rejoin lets
  the pack screen be built before the flag is true.
- Fix (the diagnosis's explicit alternative): **delay the pack send ~250ms (~5 ticks) after the
  payload.** New `ResourcePackServer.sendToPlayerDelayed(player, delayMs)` (mirrors the existing
  `onGuiClosed` DEBOUNCER→server-thread pattern, no-ops if the player left). `CustomBlocksMod`
  JOIN hook now calls it instead of `sendToPlayer`.

### Files touched
`command/handlers/HexCommands.java`, `gui/chest/HexColorsMenu.java`,
`gui/chest/HexRecolorConfirmMenu.java`, `network/ResourcePackServer.java`, `CustomBlocksMod.java`.

### Next
Developer runs testing guide §0d (① — ⑨) + §0c④ re-check + confirm the Yes-repaint actually
repaints (it was masked by BUG 1's parse error). §0d pass → **Group 06 done → start Group 07**
(`docs/Finale Fix/`).

---

## 2026-06-11 (HANDOFF — session ended mid-test) — 2 NEW BUGS reported, diagnosed, NOT fixed

Developer tested the round-2 jar (07:10) and hit two new problems. **Fix these FIRST in the
next session, before anything else.**

### 🐞 BUG 1 — `/cb config hex red #FF0000` → "Expected whitespace to end one argument, but found trailing data … #FF0000<--[HERE]"
- Happened when editing red's hex through the Config GUI (Variant Colours → anvil).
- **Diagnosis (confident):** Brigadier's `StringArgumentType.word()` only accepts
  `0-9 A-Z a-z _ - . +` — the `#` character is ILLEGAL in a `word()` argument. The anvil flow
  (`HexColorsMenu.applyHex`) normalizes input to canonical `#RRGGBB` and dispatches
  `cb config hex red #FF0000` via `executeWithPrefix` → parse error in chat, nothing applied.
- **Same landmine in TWO more places:**
  - `HexRecolorConfirmMenu` Yes button runs `recolorvariants <colour> <oldhex>` where oldhex
    carries a leading `#` — `oldhex` is also a `word()` arg → same parse error.
  - Any player typing a hex WITH `#` by hand (the §0b guide literally tells them to).
- **Fix:** in `HexCommands.register`, change the `value` arg of `config hex` AND the `oldhex`
  arg of `recolorvariants` from `StringArgumentType.word()` to `greedyString()` (both are the
  final argument, so greedy is safe). `normalizeHexColor` already tolerates both `#FF0000`
  and `FF0000`, so no other change needed. Belt-and-braces: strip the `#` in
  `HexColorsMenu.applyHex` and the Yes button's dispatched command strings.
- Files: `command/handlers/HexCommands.java`, `gui/chest/HexColorsMenu.java`,
  `gui/chest/HexRecolorConfirmMenu.java`.

### 🐞 BUG 2 — silent pack ON, but rejoin still shows the vanilla "download resource pack?" Yes/No dialog
- Developer expectation (correct): with `/cb config silentpack` ON, rejoin should apply the
  pack silently — no dialog.
- **Diagnosis (likely, verify in code):** an ORDER problem on join. `CustomBlocksMod`'s
  join hook calls `ResourcePackServer.sendToPlayer(player)` right away, but the client only
  auto-accepts when its `SilentPackState` flag is true — and that flag is set by
  `SilentPackPayload`, which (a) is reset to false on every disconnect by design, and (b) may
  arrive AFTER the ResourcePackSend packet on rejoin → the mixin sees flag=false → vanilla
  dialog shows.
- **Fix direction:** on PLAY-join, send `SilentPackPayload(silentPack)` FIRST, then the pack
  packet (or delay the pack send a few ticks after the payload). Check
  `CustomBlocksMod` (join hook, ~line 97), `client/SilentPackState`,
  `mixin/ClientCommonNetworkHandlerMixin`, `network/payloads/SilentPackPayload`.
- Also note: vanilla still always prompts for server-FORCED packs; ours is not forced — the
  silent path is the mod's own auto-accept, so ordering is the whole story.

### State of the rest (unchanged from the entry below)
- Round-2 fixes + M4 are built, green, jar copied 07:10 — **testing guide §0d NOT yet run**
  (developer hit BUG 1 immediately). After both bugs are fixed: rerun §0d ① — ⑨, plus §0c④
  re-check (reload-waits-for-GUI), plus confirm the Yes-repaint actually repaints (it may
  have been silently broken by BUG 1's parse error on `recolorvariants`).
- Group 06 after that: core mechanics all built (M1-M4). Optional backlog: eyedropper,
  despeckle, preview, held-block glow (deferred), §4 lore/chat polish. **Developer said:
  when Group 06 is done → continue to Group 07** (`docs/Finale Fix/` has the group docs).

---

## 2026-06-11 (round-2 fixes + M4) — red art redrawn · reload waits for GUIs · per-face paint (build green, jar copied, NOT tested)

### Developer's test results (previous slice)
- ☑️ §0c passes ("everything else passes") — Config-GUI hex editor, anvil editing, Color Studio.
- 🐞 **Red Triangle texture "broken"** — diagnosed: the BUNDLED red art itself was lumpy/deformed
  (old-project file); yellow/green/black are clean. Pack tint output was verified clean offline.
- 🐞 **Pack-rebuild timing "still not working"** — diagnosed from `latest.log` (06:45:10): hex
  change with **0 variants** takes the no-confirm branch → instant rebuild → resource reload
  lands while the player is still inside the Variant Colours GUI. Last session's fix only
  covered the confirm-GUI case.

### Done (code written; build green — compile + 3 gates; jar → `.minecraft\mods` 07:10)
- **`red_triangle.png` regenerated** from the clean yellow art (HSB transfer to `#EE3333`):
  symmetric shape, highlight + outline preserved. `custom_square/triangle.png` white bases
  regenerated from the YELLOW art too (were derived from the lumpy red).
- **`ColorReplacer.tint` → HSB transfer** (target hue; sat = target×pixel; brightness scaled):
  re-tinted overrides keep highlight/fill/outline contrast instead of flattening.
- **Deferred reload safety (recycled old-project behaviour):** `ResourcePackServer.sendToPlayer`
  now HOLDS the push for any player inside a CustomBlocks chest menu or anvil prompt
  (`PENDING_SENDS`); `CbChestHandler.onClosed` + `AnvilPrompt.onClosed` call `onGuiClosed`,
  which delivers the held push ~300ms later if no other CB screen replaced it. Covers EVERY
  flow (confirm GUI, Variant Colours, Color Studio, anvils) — not just the confirm GUI.
- **M4 — per-face URL paint** (Group 06's last tool mechanic):
  - `TextureStore`: per-face overrides `slot_N_<face>.png` (FACES const, saveFace/loadFace/
    hasFace/hasAnyFace/deleteFace; `delete()` wipes faces too — no bleed into reused slots).
  - `ServerPackGenerator`: blocks with overrides emit a `minecraft:block/cube` model — painted
    faces point at their texture, the rest (+particle) at the base. No SlotData change needed;
    faces persist as plain files.
  - **New `command/handlers/FaceCommands.java`** — `/cb paintface <id> <face> <url>` (same
    download/bg-clean/resize pipeline as retexture, ONE rebuild after) + `/cb clearface
    <id> <face|all>`. Registered in CommandRegistrar.
  - **`RainbowRectangleItem`** — right-click a FACE → chat opens pre-filled
    `/cb paintface <id> <face> ` (ChatPrefillPayload); **area corner-marking moved to
    sneak + right-click** (Omni Area mode untouched). Lore rewritten.

### TEST IN-GAME (developer) — guide §0d (9 tests)

### Files touched
`assets .../red_triangle.png`, `custom_square.png`, `custom_triangle.png` (regenerated),
`image/ColorReplacer.java`, `network/ResourcePackServer.java`, `network/ServerPackGenerator.java`,
`gui/chest/CbChestHandler.java`, `gui/chest/AnvilPrompt.java`, `core/TextureStore.java`,
`command/handlers/FaceCommands.java` (new), `command/CommandRegistrar.java`,
`item/RainbowRectangleItem.java`, `Reports/GROUP_06_TESTING_GUIDE.md` (§0d).

### Group 06 remaining after this
M4 was the last core mechanic. Still open in the tracker: A eyedropper · B despeckle ·
C preview (bg-removal upgrades, approved) · held-block glow (deferred) · §4 lore/chat polish.
Group 07 starts once the developer calls Group 06 done.

---

## 2026-06-11 (M3 hex fixes + customcolor) — Config-GUI hex editor · rebuild-waits-for-GUI · Color Studio (build green, jar copied, NOT tested)

### Developer's §0b test findings (this session's work order)
1. `/cb config hex` had **no Config-GUI slot** — wire it in, sub-GUI with a dye per tool, edit in anvils.
2. **Bug:** after a hex change the pack rebuild fired immediately, WHILE the recolour-confirm GUI was opening — rebuild must wait until that GUI is closed.
3. **Remake `/cb customcolor` from scratch** on the old mod's Color Studio idea (no args → GUI of ready-made colours → magic tool pairs).

### Done (code written; build green — compile + 3 gates, 24s; jar → `.minecraft\mods`)
- **New `gui/chest/AnvilPrompt.java`** — reusable server-side anvil text input (rename the seed
  item, take the output to submit; ESC = cancel). Recycled from the old project's proven
  AnvilPromptManager, rebuilt callback-based.
- **New `gui/chest/HexColorsMenu.java`** (`Nav.Dest.HEX_COLORS`) — the ONLY GUI place hexes are
  edited: four dyes (red/yellow/green/black) showing current hex, default, variant count; click →
  anvil prompt → valid input runs the tested `/cb config hex` command. ConfigMenu got a glowing
  **Variant Colours** entry (slot 32) that opens it.
- **Rebuild-timing fix** — `ChestMenu.onClose(callback)` + `CbChestHandler.onClosed` now report
  screen close once. `HexCommands.setHex` no longer rebuilds the pack before opening the confirm;
  `HexRecolorConfirmMenu`: **Yes** → the repaint batch's single rebuild covers everything ·
  **No/X/ESC** → one rebuild on close (item re-tint). No-variant/console path rebuilds immediately.
  Restart with the prompt open is safe — the pack rebuilds at every SERVER_STARTED.
- **`/cb customcolor` rebuilt** (old mod read for inspiration; code new):
  - **New `core/ColorLibrary.java`** — 29 preset colours + aliases + name/hex resolution
    (data recycled from the old ColorLibrary).
  - **New `item/CustomColorToolItem.java`** + `custom_square`/`custom_triangle` registered in
    ToolItems — ONE item id per shape, the colour rides in NBT (`cb_rgb`); Triangle creates a
    `*_hex_rrggbb` variant, Square swaps to an existing one (same flows as ShapeToolItem).
  - **`ColorVariantService`** generalised for `hex_rrggbb` keys: `keyForRgb/isHexKey/rgbForKey/
    labelFor`, `stripColourSuffix` also strips `_hex_xxxxxx`.
  - **New `gui/chest/CustomColorMenu.java`** (`Nav.Dest.CUSTOM_COLOR`) — the Color Studio: 29
    dyes (click → pair, studio stays open) + **Custom Hex…** anvil button (hex or colour name).
  - **`/cb customcolor [colour]`** registered in HexCommands — bare opens the studio; an arg
    (hex or name, aliases included) gives the pair directly, with did-you-mean on bad input.
  - **Icons:** new white `custom_square/triangle` textures (generated from the red art) tinted
    client-side from the stack NBT (`ColorProviderRegistry` in CustomBlocksClient) — every pair
    visibly wears its colour. Lang entries added.

### Persistence (audited, needs in-game confirm)
Hexes → config.json (atomic save/load) · tool colour/name/lore → item NBT in the inventory ·
hex variants → SlotDataStore + TextureStore like any block · pack → rebuilt every boot.

### TEST IN-GAME (developer) — guide §0c (10 tests) + re-run §0b ②③⑦ (timing changed)

### Files touched
`gui/chest/AnvilPrompt.java` (new), `gui/chest/HexColorsMenu.java` (new),
`gui/chest/CustomColorMenu.java` (new), `core/ColorLibrary.java` (new),
`item/CustomColorToolItem.java` (new), `gui/chest/ChestMenu.java`, `gui/chest/CbChestHandler.java`,
`gui/chest/ConfigMenu.java`, `gui/chest/HexRecolorConfirmMenu.java`, `gui/chest/Nav.java`,
`gui/chest/GuiRouter.java`, `command/handlers/HexCommands.java`, `core/ColorVariantService.java`,
`item/ToolItems.java`, `client/CustomBlocksClient.java`, assets (2 textures, 2 models, lang),
`Reports/GROUP_06_TESTING_GUIDE.md` (§0c).

---

## 2026-06-11 (M3 hex) — colour setters + item re-tint + variant repaint (build green, NOT tested)

### First: M3 swap verified ✅ + hotbar wording flagged
- Developer confirmed all 8 §0 guide tests in-game (swap, fallback, errors, glow, lore, GUI label).
- 🟡 **PARTIAL: hotbar message wording** — rough, queued with the chat/GUI polish (guide §4).

### Done (code written; build green — compile + 3 gates, 24s; jar → `.minecraft\mods`)
Built **M3 hex** from `Reports/GROUP_06_HANDOFF.md` §6 — the four variant colours are configurable:
- **New `command/handlers/HexCommands.java`** — `/cb config hex` (status), `/cb config hex
  <colour>` (one), `/cb config hex <colour> <#RRGGBB>` (set: normalize/validate → save → pack
  rebuild re-tints item art → if `*_<colour>` blocks exist, open the confirm GUI; console gets a
  command hint instead). Plus `/cb recolorvariants <colour> <oldhex>` — the repaint batch entry.
  Started inside ConfigCommands but that hit 413 lines → **the 400-line gate fired** → split out
  (the no-monolith rule doing its job).
- **New `image/ColorReplacer.java`** — `swapColor` (direct per-pixel replace within ~30/channel
  of the old hex, NO flood fill → design pixels safe, the old project's batch pitfall avoided)
  and `tint` (target hex scaled by pixel brightness — outlines survive) for item art.
- **`ColorVariantService`** — `variantCount(colour)` + `recolorVariants(player, colour, oldRgb)`:
  off-thread batch over every `*_<colour>` block texture, ONE `updatePack()` AFTER the batch
  (CLAUDE.md §7), report "N recoloured / N skipped(+incidents)".
- **`ServerPackGenerator.addTintedShapeItems`** — when a colour's hex ≠ shipped default, the pack
  carries re-tinted Square/Triangle item textures (bundled art loaded from the jar, tinted,
  written at the same asset path → overrides the mod art client-side). Default hex → no override.
- **`CustomBlocksConfig`** — `TRIANGLE_*_DEFAULT` constants (fields now initialize from them).
- **New `gui/chest/HexRecolorConfirmMenu`** (Yes/Info/No; arg `colour:oldHex`; Yes runs the
  tested `/cb recolorvariants`, No closes) + `Nav.Dest.HEX_RECOLOR_CONFIRM` + `GuiRouter` case.
- **`ShapeToolItem.getName`** — names now show the live hex: "Red Square §8[#EE3333]" (spec).

### TEST IN-GAME (developer) — guide §0b (8 tests)
Status line · set red `#FF8800` with `_red` blocks placed → confirm opens · Yes repaints (design
intact) · item names show hex + art re-tinted · new variants use the new hex · bad hex/colour
errors · No keeps blocks · hexes survive restart.

### Files touched
`command/handlers/HexCommands.java` (new), `image/ColorReplacer.java` (new),
`gui/chest/HexRecolorConfirmMenu.java` (new), `core/ColorVariantService.java`,
`network/ServerPackGenerator.java`, `CustomBlocksConfig.java`, `item/ShapeToolItem.java`,
`gui/chest/Nav.java`, `gui/chest/GuiRouter.java`, `command/CommandRegistrar.java`,
`command/handlers/ConfigCommands.java` (hex moved out).

---

## 2026-06-11 (M3 swap) — Square = swap to colour variant (build green, jar copied, NOT tested)

### Done (code written; build green — compile + 3 gates, 23s; jar → `.minecraft\mods`)
Built **M3 Square swap** from `Reports/GROUP_06_HANDOFF.md` §6 — squares' old placeholder
tag/marking is GONE (developer flagged it this session):
- **`ColorVariantService.swapPlaced(player, world, pos, current, colourKey)`** — resolve
  `variantId(currentId, colour)` (strip-suffix + add, so `vart_red` + black → `vart_black`);
  exists → `world.setBlockState` to that slot's block, carrying the target's glow in the
  `SlotBlock.LIGHT` state (mirrors `getPlacementState`); **never creates**. Black Square with
  no `_black` → falls back to the BASE id. Same block → "already" message. No variant at all →
  hotbar error "create one with the <Colour> Triangle first". Null block guard → /cb reload hint.
- **`ShapeMarkerItem`** — square branch now calls `swapPlaced` (tag message deleted); square
  lore rewritten (swap wording, Black Square fallback line; no more "tagging"/"mark").
- **DEVIATION from the presented plan:** `/cb undo` does NOT cover swaps. `UndoManager` stores
  only SlotData edits (deliberately free of world/position types) and a swap edits no SlotData.
  Swap is self-reversing (Square of the original colour / Black Square → base). Wiring world
  edits into undo = its own slice + ADR if the developer wants it.

### TEST IN-GAME (developer) — guide §0 (7 tests)
Red Square on placed `vart` → becomes `vart_red`, "Swapped to" chat · Black Square → `vart_black`
· Green Square (no variant) → error, nothing created · delete `vart_black` then Black Square →
back to base `vart` · same colour twice → "already" · swapped block carries variant glow ·
square lore rewritten.

### Also: full marker/tagging purge (same session, developer-requested; rebuilt green, jar re-copied)
- **`ShapeMarkerItem.java` RENAMED → `ShapeToolItem.java`** (class + constructor + header).
- Every "marker"/"tagging" reference removed from src: `ToolItems` (list type + comments),
  `CustomBlocksMod` (tab comment), `MagicItemsManager` (seed comment), `MagicMenu` (header
  comment + slot comment + **user-visible GUI label**: "Marker Shapes / Tag your custom blocks
  by colour" → "Squares & Triangles / Triangles create colour variants — Squares swap them"),
  `RecolorConfirmMenu` + `ColorVariantService` (called-by comments), `ToolCommands` (comment),
  `item/package-info.java` (rewritten to current tool reality — old project's 7-tool list was stale).
- Grep proof: zero `marker|Marker|tagged|tagging` matches left in `src/main`. (Unrelated "tag"
  uses — `[CB]` brand tag in Chat.java, Minecraft tool-tag comment, category list tag — kept.)

### Files touched
`core/ColorVariantService.java`, `item/ShapeToolItem.java` (renamed from `ShapeMarkerItem.java`),
`item/ToolItems.java`, `item/package-info.java`, `CustomBlocksMod.java`,
`core/MagicItemsManager.java`, `gui/chest/MagicMenu.java`, `gui/chest/RecolorConfirmMenu.java`,
`command/handlers/ToolCommands.java`, `Reports/GROUP_06_TESTING_GUIDE.md` (new §0).

---

## 2026-06-11 (verification) — M2 + Retexture-all PASSED in-game ✅; Squares old-tag issue → M3 next

### Verified in-game ✅ DONE (developer, 2026-06-11)
- **M2 Triangle colour variants** — all 6 guide tests passed: variant create (design kept, bg
  recoloured, source untouched), sneak Yes/Info/No confirm, already-exists instant give, suffix
  swap (`vart_black` + red → `vart_red`), untextured-block error, `/cb undo` removes variant.
- **Retexture-all** — all 6 guide tests passed: confirm GUI + Info numbers, Yes batch (progress +
  complete chat, blocks visibly change), No keeps existing blocks, 0-block skip, source-sharpen vs
  upscale-only difference confirmed, direct `/cb retextureall <px>`.
- `Reports/GROUP_06_TESTING_GUIDE.md` updated: §1 + §2 marked passed; squares issue flagged.

### Reported issue (developer)
- **Squares still do the old tag/marking** (`[CB] <Colour> Square tagged …`, `ShapeMarkerItem`
  square branch). Expected — M3 not built — but developer wants it fixed next.

### Next steps, in order
1. **M3 — Square swap** (`Reports/GROUP_06_HANDOFF.md` §6): right-click placed block → swap the
   PLACED block in place to its existing `<id>_<colour>` variant; Black Square falls back to base
   id when no `_black` exists; **never auto-create**; clear hotbar error when no variant; reuse
   `ColorVariantService` id math (`stripColourSuffix`); record undo; replace square tag message +
   "tagging" lore. Plan presented to developer — awaiting go-ahead.
2. **M3 hex** — config hex setters + Square/Triangle item re-tint + "recolour existing blocks?"
   batch prompt (direct hex swap, batch must force edge mode — CLAUDE.md §7).
3. Rest of guide §4/§5 backlog (eyedropper, despeckle, preview, per-face, lore/chat polish).

---

## 2026-06-11 (later still) — M2: Triangle creates colour variants (build green, NOT tested)

### Done (code written; build green — compile + 3 gates, 22s; jar copied to `.minecraft\mods` 03:58)
Built **M2 from `Reports/GROUP_06_HANDOFF.md` §6** — Triangles now CREATE colour variants:
- **Config:** 4 new fields `triangleRedHex #EE3333` / `triangleYellowHex #F0C814` / `triangleGreenHex
  #1E8C1E` / `triangleBlackHex #0A0A0A` (old project's defaults), validated by a new
  `normalizeHexColor` (accepts missing `#`, bad value → keep old). Load + atomic save wired.
- **`BackgroundRemover`:** `apply()` body extracted into a shared `process(…, Integer forcedFill)`;
  new `recolorBackground(input, mode, tolerance, fillRgb)` = same M1 detection (flood-fill, ΔE,
  anti-fringe) but paints the background the GIVEN hex instead of smart black/white. Mode `none` is
  promoted to `edges` (a recolour with no detection would do nothing). Smart-fill brightness scan
  skipped when the fill is forced.
- **New `core/ColorVariantService`** — the M2 brain: `variantId` (`stripColourSuffix(src) + "_" +
  colour`, so `mars_black` + red → `mars_red`), `createVariant(player, sourceId, colour)`:
  variant exists → just hand the item over; source untextured → friendly error, nothing claimed;
  else claim slot, copy glow/hardness/sound/category in ONE `restoreSnapshot` write, record undo,
  then worker thread recolours (stored original source if present, else baked PNG → same fallback
  as retextureall) → `toBlockPng` at current textureSize → save (+ copy the `.src` to the variant
  so it stays re-renderable) → ONE pack rebuild → give item + chat. Failure → incident + chat
  (block stays, textureless, deletable).
- **`ShapeMarkerItem`:** Triangle right-click = create variant now; **sneak+right-click = new
  Yes/Info/No `RecolorConfirmMenu`** (arg `colour:sourceId`; Info shows new id, hex, source-vs-baked
  note, already-exists note). Squares keep the tag message until M3. Triangle lore rewritten.
- **Wiring:** `Nav.Dest.RECOLOR_CONFIRM` + `GuiRouter.build` case.

### NOT done (next slices, in spec order)
- **M3** — Square = swap placed block to an existing colour variant (never auto-create, hotbar error).
- **M3 hex** — `/cb config` hex setters + item-texture re-tint + "recolour existing blocks?" batch
  prompt (batch must force edge mode — CLAUDE.md §7).

### TEST IN-GAME (developer)
1. Give a textured block + a Red Triangle. Right-click the placed block → chat "Creating …", then
   "created — background recoloured to #EE3333"; new block lands in inventory; place it: same design,
   red background. Source block unchanged.
2. Sneak+right-click with a triangle → confirm GUI opens; Info reads right; Yes creates (chat + item);
   No/✕ does nothing.
3. Same triangle on the SAME block again → "already exists — here's one" + item, instantly.
4. Red triangle on `<id>_black` → makes/gives `<id>_red` (suffix swapped, not stacked).
5. Triangle on an untextured block → friendly error, no block created (check /cb list).
6. Undo: /cb undo after a variant create should remove the variant.

### Files touched
`CustomBlocksConfig.java`, `image/BackgroundRemover.java`, `core/ColorVariantService.java` (new),
`item/ShapeMarkerItem.java`, `gui/chest/RecolorConfirmMenu.java` (new), `gui/chest/Nav.java`,
`gui/chest/GuiRouter.java`.

---

## 2026-06-11 (later) — Texture-Size picker sub-GUI (✅ verified in-game)

### Done (code written; build green — compile + 3 gates, 22s; jar copied to `.minecraft\mods`)
Built the texture-size sub-GUI from the handoff spec below. Four parts + one extra fix:
- **512 now allowed** — raised the cap 256→512 in **three** spots: the `texturesize` command arg
  (`ConfigCommands`), the config-file loader clamp (`CustomBlocksConfig.load`, line ~127 — the
  handoff named only two spots; this loader clamp would have silently snapped 512 back to 256),
  and the field doc comment. Suggestions now offer 16/32/64/128/256/512.
- **New `gui/chest/TextureSizeMenu.java`** — a 3-row chest listing all six sizes (slots 10–15).
  Current size glows. Each item's hover shows **~per-texture size + ~whole-pack size** (per-texture
  × live `maxSlots`, e.g. "~80 KB per texture · ~64 MB pack if all 800 textured") plus a look hint.
  Clicking delegates to the tested `/cb config texturesize <px>` command then reopens Config.
- **Wired** via `Nav.Dest.TEXTURE_SIZE` + `GuiRouter.build` switch case.
- **Opens** from the Config chest **Texture Quality** row (slot 20, now glowing + clickable) and from
  `/cb config texturesize` with **no argument** (`openTextureSizeMenu`; console still prints status).
  `/cb config texturesize <px>` with a number still sets directly as a shortcut.

### MB numbers are measured, not guessed
Re-encoded real images at 16–512px (32-bit RGBA PNG, high-quality downscale) and cross-checked
against the developer's 20 on-disk textures (all 64px, 0.2–11 KB) — the model lands in range.
Typical mid-values used: 16≈1 KB · 32≈2 KB · 64≈6 KB · 128≈20 KB · 256≈80 KB · 512≈0.3 MB.
(Real images vary: a flat logo packs larger than a smooth photo at the same size.)

### Verified in-game ✅ DONE
- Developer confirmed the texture-size picker works: menu opens from the Texture Quality row AND the
  bare `/cb config texturesize`, hover shows per-texture + pack MB, clicking a size sets/saves and
  returns to Config, 512 selectable. ✅

### 🟡 PARTIAL / backlog (developer-requested this session; NOT built — deferred to save a session)
- **Retexture-confirm sub-sub-GUI.** After picking a size in `TextureSizeMenu`, open a 3-button
  confirm: **Yes** (retexture existing blocks to the new size) · **Info** (middle, read-only) · **No**
  (keep existing as-is). Info-item brainstorm: "§eRetexture N existing blocks?" where N =
  `SlotManager.usedSlots()`; est. new pack size = N × per-texture-at-new-px; "players see one brief
  pack reload"; "only already-created blocks change; sources are re-rendered".
  **✅ FEASIBILITY CHECKED — source is NOT stored.** Per-block disk = only the baked `slot_N.png`
  (`TextureStore`), at the size it was made. No URL, no raw image, no cache; `applyTexture`
  (`CreationCommands`) downloads → processes → discards the URL (kept only in an error incident).
  `SlotData` has no source field. Consequences for the feature:
    - Smaller/equal target size → clean (downscale the baked PNG).
    - **Larger target → only upscales** the old pixels; no real sharpening (we lack the original).
  To enable TRUE higher-res re-render: add a **source store** — in `applyTexture`, also write the raw
  downloaded bytes to `config/customblocks/sources/slot_N.<ext>`; then retexture-all re-runs
  `ImageProcessor.toBlockPng` at any size from the real source. Cost: ~1 source image of disk/block;
  only helps blocks created AFTER that change (existing blocks have no source to recover).
  **DECISION NEEDED from developer:** (a) honest down/equal-only retexture now, or (b) add source
  store first so the feature can truly sharpen going forward.
  **➡ UPDATE (this session): chose (b). SOURCE STORE BUILT (build green, NOT tested).**
  - `TextureStore` now also keeps each URL block's ORIGINAL image at `config/customblocks/sources/
    slot_N.src` (raw bytes): new `saveSource` / `loadSource` / `hasSource`; `delete(index)` now wipes
    texture AND source (single choke point — every slot-free path in `SlotManager` already routes here).
  - `CreationCommands.applyTexture` calls `TextureStore.saveSource(index, raw)` after a successful
    texture. So every block textured/retextured FROM NOW ON is re-renderable at any size.
  - A **huge NOTE block** above `CreationCommands.retexture()` documents the exact retexture-all batch
    recipe (loadSource → re-run BackgroundRemover + toBlockPng at new size → one pack rebuild at end).
  - Caveats: blocks made BEFORE this update have no source (upscale-only); Arabic/video blocks never
    have a single source; undo of a delete restores the baked texture but not the source.
  - STILL NOT BUILT: the Yes/Info/No confirm sub-sub-GUI + the batch job itself. That's the next slice.
  **➡ UPDATE 2 (this session): retexture-confirm GUI + batch BUILT (build green, NOT tested).**
  - New `gui/chest/RetextureConfirmMenu.java` — 3-row Yes / Info / No. Opens after picking a size in
    `TextureSizeMenu` **only when** `usedSlots() > 0` (else straight back to Config). Info item shows
    block count, est. new pack size (reuses `TextureSizeMenu.avgBytes`/`human`), source-vs-upscale note.
  - New command `/cb retextureall [16-512]` in `CreationCommands` + `retextureAll(server, size, src)`
    batch: off-thread, per slot loads the stored source → re-runs BackgroundRemover + toBlockPng at the
    new size; no source → upscales the baked PNG; ONE `ResourcePackServer.updatePack()` after the batch.
    Reports "N re-rendered, N upscaled, N skipped"; logs an incident if any skipped.
  - Wiring: `Nav.Dest.RETEXTURE_CONFIRM`, `GuiRouter.build` case + new `runThenNavigate` helper,
    `TextureSizeMenu` click now branches (confirm vs back-to-config), `TextureSizeMenu.avgBytes`/`human`
    made package-private for reuse.
  - **TEST IN-GAME (developer):** with ≥1 existing block, pick a size → confirm opens; Info reads right;
    Yes → chat "Retexturing N…" then "complete — …", blocks change at new size; No → blocks unchanged,
    size still set for new blocks; with 0 blocks, picking a size skips the confirm. Try going UP in size
    on a block made THIS session (has source → real sharpen) vs an OLD block (upscale only).

### Next steps, in order
1. Pick the next issue with the developer (texture GUI is done; M3 is big and was deferred).
2. Retexture-confirm sub-sub-GUI (above) — once the source-storage question is answered.
3. Queued **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6) — the big colour-variant feature.
4. Open bg bugs (Test 2 fringe, Test 4 low-contrast) stay parked unless the developer reopens them.
5. 🔶 **BACKLOG — old `.dat` → new `.png` migration** (noted 2026-06-11, not yet planned/built). The live
   **➡ DEVELOPER DECISIONS (2026-06-11, via UI):** migration STAYS PARKED for now. When built: old blocks
   keep their **native 256** (NO stretch to 512 — zero detail gained, pack would balloon ~60→230 MB).
   Default textureSize for new blocks = **256**, developer adjusts in-game via the size menu as wanted.
   The 20 junk test blocks (wrecked by a 64→512 upscale: blur + rainbow static on busy images, e.g.
   slot_2 "PNG Taat" — no sources existed, all hit the upscale fallback) are to be **deleted entirely**.
   config dir has **770 old `slot_N.dat` files** (real blocks) but the new `slots.json` knows only **20 junk
   test blocks**. Findings from inspection:
   - `.dat` bytes ARE real PNGs (old version's extension); `.dat` are **256×256 RGBA** (some 128²) vs new
     `.png` at 64². So `.dat` are HIGH-RES — effectively free re-render sources for 770 blocks.
   - A block renders in the new pack ONLY if its index is in new `slots.json` (`assignedSlots()`) **AND**
     `slot_N.png` exists ([ServerPackGenerator] cube_all). So migration = **TWO jobs:** (A) copy/rename
     `slot_N.dat`→`slot_N.png` (trivial, no transcode), (B) convert OLD metadata (`slots.bak1.json` 144 KB /
     `slots_old.json` 20 MB) → new `SlotData` schema `(index, customId, displayName, glow, hardness,
     soundType, noCollision, category)`. Texture-only copy = orphan, never shown.
   - Gotchas: index collision (junk tests 0–19 overlap real old 0–19 → wipe junk, import wholesale or offset);
     per-face `slot_N_north.dat` + variants `slot_N_var0.dat` have no home (new = cube_all, M3 not built) →
     flatten to single `slot_N.dat`, variants dropped for now.
   - Sizing the `.dat`: DOWN-size = clean/sharp; UP past native 256 = upscale-only (no new detail). ⚠ `.dat`
     are already-baked (bg removed, square) → size them with a **plain downscale**, NOT the full
     `retextureAll` pipeline (BackgroundRemover + toBlockPng would double-process). `loadSource()` reads
     `slot_N.src` (none exist yet) → wire `.dat` as a source fallback.

### Files touched
`gui/chest/TextureSizeMenu.java` (new), `gui/chest/Nav.java`, `gui/chest/GuiRouter.java`,
`gui/chest/ConfigMenu.java`, `command/handlers/ConfigCommands.java`, `CustomBlocksConfig.java`.

---

## 2026-06-11 (late) — 🟢 HANDOFF (read this first): M1 re-test results + texture-size sub-GUI request

> Continue in a fresh conversation. Self-contained. Supersedes the earlier "M1 bg fixes" entry below
> (those fixes are now in the jar AND in-game tested — results here).

### In-game re-test (new jar: smart fill + tolerance ΔE cap 22 + textureSize 128)
Developer ran the 5 substitute images + the pixelation test:

1. ✅ **Test 1 — dark subject FIXED.** Black "K" renders on a light/white fill, clearly visible — no
   more black-on-black. Smart fill works. *(Fill looks faintly lilac in shade = world lighting on
   white, not a bug. Can verify the raw PNG is pure white if it ever matters.)*
2. 🐞 **Test 2 — light fringe halo remains.** White bg removed→black, red "88" correct, but a thin
   **light outline** rings the red digits — anti-alias fringe the 1-px dilation didn't catch.
   Developer's instinct (raise tolerance) is **right, partly:** fringeTol = tol+6, so higher tol
   eats more of the ring (try 45–55). Proper fix = widen the anti-fringe past 1 px, or queued **B
   despeckle**. Lives in `image/BackgroundRemover.java` (`FRINGE_EXTRA` + Stage-2 dilation). NOT a blocker.
3. 🐞 **Test 4 — low-contrast still eaten.** Substitute (gray `CCCCCC` bg + near-white `EEEEEE` "6")
   at **tol 55** → all-black; the 6 read as background. Cause: those greys are only ~ΔE 12 apart and
   tol 55 now maps to ΔE 12.1 — still ≥ the subject gap. The cap fix (40→22) DOES help the *original*
   bug (a ~ΔE 13 subject now survives at 55) but this substitute is lower-contrast and sits on the
   line. Fix for near-equal colours = **lower tol (25–30)** or the queued **A eyedropper**. The
   unmapped **"9 9"** image (near-white digits, clean result) confirms the fix works at real contrast.
   **OPEN DECISION:** (a) accept as the documented colour-distance limit, or (b) build eyedropper sooner.
4. ✅ **DECISION — Test 5 photo: leave it alone.** Developer agrees bg removal isn't for complex
   photos. No work on the photo case; eyedropper/despeckle stay optional/later.
5. ✅ **Pixelation — big win.** "sharpness difference is huge." 128px >> 64px, confirmed in-game.

### ▶️ NEXT FEATURE (developer wants this) — Texture-Size picker sub-GUI · NOT built
Ready to build (clear spec). Three parts:
1. **Add a 512px option** — raise the max from 256 → **512** in two places:
   `CustomBlocksConfig.textureSize` clamp `(16,256)`→`(16,512)` and `ConfigCommands` arg
   `integer(16,256)`→`(16,512)`. Mind pack size (that's what the MB hover is for).
2. **Open a sub-GUI** instead of a chat line: clicking the **Texture Quality** row in the config
   chest GUI (`gui/chest/ConfigMenu.java` slot 20) AND running `/cb config texturesize` (no arg)
   open a new chest menu listing every size — **16 / 32 / 64 / 128 / 256 / 512** — as clickable
   items. Click one → set `textureSize` + `save()` + return to the config GUI. Follow the existing
   chest-menu pattern (`ConfigMenu`, Magic Items menu) and wire via `GuiRouter` / `Nav`. New file
   e.g. `gui/chest/TextureSizeMenu.java`.
3. **Hover lore on each size = average texture size in MB**, so the cost is visible before choosing.
   Starting estimates (measure one real 256px export, scale by area ∝ px², then refine):
   | px | ~MB / block texture |
   |----|----|
   | 16  | <0.001 |
   | 32  | ~0.002 |
   | 64  | ~0.008 |
   | 128 | ~0.03 |
   | 256 | ~0.10 |
   | 512 | ~0.40 |
   (Could also show pack total ≈ per-texture × assigned blocks.)

### Still queued (unchanged)
- **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6) — next real feature once bg is signed off.
- A eyedropper · B despeckle · C preview · M2 · M4.

### Verified (Golden Rule)
✅ Smart fill (Test 1) + pixelation — developer-confirmed in-game. Open: Test 2 fringe, Test 4
low-contrast. Decided: Test 5 photo = leave. Texture-size GUI = not built.

### Build / run
`$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'` → `.\gradlew.bat build` → copy
`build\libs\customblocks-1.0.0.jar` to `%APPDATA%\.minecraft\mods\`.

---

## 2026-06-11 (cont.) — M1 bg fixes: tolerance + smart fill + pixelation (build green, NOT tested)

### Decision (developer, this session)
- Black-on-black fork → **keep black, smart fill** (NOT transparency). Honors the earlier
  "opaque black like the old version" call; no cutout/render changes. A near-black subject now
  gets a contrasting **WHITE** fill so it can't vanish into the black background.

### Done (code written; build green — compile + 3 gates, 8s; jar copied to `.minecraft\mods`)
- `image/BackgroundRemover.java`:
  - **Tolerance (bug #2):** `MAX_DELTA_E` 40 → **22**, so even strength 100 can't eat a
    low-contrast subject (Test 4's near-white "6", subject only ~ΔE 13 from its gray bg).
  - **Smart fill (bug #1):** after the bg mask, the mean foreground HSV-"value" (max RGB channel)
    picks the fill — `< 64` (near-black subject) → WHITE, else BLACK. Saturated-but-bright
    subjects (pure red = value 255) stay on black; only genuinely dark subjects flip. Partial-
    alpha edge pixels now composite against the chosen fill, not always black.
  - `snapBackgroundBlack` bails when the corners aren't black (i.e. a white-fill block), so the
    near-black snap can't blacken a dark subject.
- **Pixelation:** `CustomBlocksConfig.textureSize` default 64 → **128**; new `/cb config
  texturesize <16-256>` setter (status + set, saves config — overwrites a config.json pinned at
  64); ConfigMenu quality-row hint now points at the command.

### Verified in-game
- Nothing new — code + build only. **Golden Rule: NOT done until the developer confirms in-game.**

### Next steps, in order
1. **Re-run the 5 test images.** Expected: Test 1 (black "K") now shows on a WHITE fill, not
   black-on-black; Test 4 ("6") survives at high strength; Tests 2/5 unchanged.
2. **Pixelation gotcha:** the existing `config.json` is pinned at `textureSize: 64`, so the new
   128 default does NOT apply to this install on its own. Run `/cb config texturesize 128` once
   (or edit config.json), then **retexture** a block — texture size only affects new textures.
3. If the 5 tests pass in-game → build **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6).
   Held until then.
4. Then A (eyedropper) / B (despeckle) for photo/confetti; M2, M4 after.

### Files touched
`image/BackgroundRemover.java`, `CustomBlocksConfig.java`,
`command/handlers/ConfigCommands.java`, `gui/chest/ConfigMenu.java`.

---

## 2026-06-11 — 🟢 HANDOFF (read this first): background-removal test results + next steps

> Long session ending; continue in a fresh conversation. Everything below is self-contained.

### State in one paragraph
Groups 02–06 features are built; most are developer-verified in-game. **M1 background removal is in the
jar but FAILED in-game testing** — real bugs diagnosed below (not yet fixed). Before anything else, the
next session must pick the **black-fill-vs-transparency** design fork, apply the bg fixes, and fix the
global pixelation. **M3 hex recolour** (the next real feature, spec in `Reports/GROUP_06_HANDOFF.md` §6)
is queued *behind* that. No code was changed this session — the jar = the version with the bugs below.

### ✅ Verified in-game (developer-confirmed)
- Group 04 (chat tone, `/cb help`, DidYouMean, `/cb welcome`, incidents) · Group 05 (silent pack) ·
  Group 03 core (HUD overlay, `/cb config hud`, `/cb edithud`, ESC buttons).
- Group 06: Omni-Tool + **air sneak-click**, Deleter, the 8 Squares/Triangles (tag-on-click),
  **Magic Items double-chest GUI**, unified black-bracket **`[CB]` prefix**.

### 🐞 Built but FAILING — background removal (M1 + `/cb tolerance`), tested 2026-06-11
Ran the 5 test images. Findings (root causes located, **fixes NOT applied**):

1. **Dark subject vanishes — Test 1 (black "K" on white → all-black block).** Not deletion: the white
   bg is painted **black** and the subject is also black → black-on-black, invisible. **Core flaw of
   "paint bg black": any dark subject disappears.**
   → **DECIDE FIRST:** switch removed background to **transparent (cutout render)** instead of black.
   This is the see-through option declined earlier — the black-on-black failure makes it the right call.
   Needs: `"render_type":"cutout"` in `ServerPackGenerator.cubeAllJson` **and** register every SlotBlock
   on the cutout layer client-side (Fabric `BlockRenderLayerMap`). Watch light-leak/culling glitches.
   (Weaker alt: keep black but choose a contrasting fill by subject brightness.)

2. **Tolerance too aggressive — Test 4 (gray bg + near-white "6" at tol 55 → all-black).** Map is
   `tol/100 * MAX_DELTA_E(40)`, so tol 55 = ΔE 22, but subject-to-bg was only ~ΔE 13 → the 6 read as
   background and got eaten. → **Fix:** lower `MAX_DELTA_E` in `BackgroundRemover` from 40 to ~20–22.
   Document that genuinely low-contrast images can't be cleanly separated by colour distance.

3. **Photo breaks down + modes identical — Test 5.** Colour flood-fill can't isolate a photo (no
   uniform bg); edge vs closed match because a photo has ~no enclosed same-colour pockets. → Not a
   tweak — this is the documented limit; needs the queued **eyedropper (A)/despeckle (B)/AI**.

4. **Test 2 (the two 8s) is CORRECT, keep it.** Enclosed mode blacks the loop-holes; edge-only leaves
   them red (enclosed bg, not edge-connected). That's the intended mode difference, not a bug.

### 🐞 Global quality bug — severe pixelation (ALL generated blocks)
Textures are **64px** (`CustomBlocksConfig.textureSize = 64`); 512→64 downscale + MC block rendering =
chunky. → **Fix:** bump `textureSize` to **128 or 256** + add an in-game setter (the `/cb config` row is
read-only and existing `config.json` is pinned at 64). Mind pack size at 256.

### 🟡 Partials backlog (developer-requested this session)
- **Chat flooding** — still happening; **keep PARTIAL**, fix later.
- **All "partial" GUIs** (Group 02 chest GUIs, Omni-Tool config GUI, etc.) — **revisit soon**: new
  mechanics, add new stuff into the GUIs, final polish. Treat as one dedicated backlog item.
- **Lore wording** — drop "marker"; they are **Squares / Triangles** (do in the lore polish pass).
- **Chat formatting** — dedicated polish pass queued.

### ▶️ Next steps, in order
1. Decide **black fill vs transparency** (fork in bug #1) — gates the bg fixes.
2. Apply bg fixes: tolerance scale (`MAX_DELTA_E`), the chosen fill/transparency → re-run the 5 tests.
3. Fix **pixelation** (textureSize 128/256 + setter).
4. Build **M3 hex recolour** (`GROUP_06_HANDOFF.md` §6) — the next real feature.
5. Then A (eyedropper) / B (despeckle) for photo/confetti; M2, M4 after.

### Key files
- `image/BackgroundRemover.java` — algorithm; `MAX_DELTA_E` lives here.
- `command/handlers/CreationCommands.java` — `applyTexture` (remover before `toBlockPng`, snap after).
- `command/handlers/ConfigCommands.java` — `/cb tolerance`, `/cb config background`.
- `CustomBlocksConfig.java` — `backgroundMode`, `backgroundTolerance`, `textureSize`.
- `network/ServerPackGenerator.java` — block-model gen (the `render_type` for the transparency fork).
- Specs/status: `Reports/GROUP_06_HANDOFF.md` (§6 M2/M3), `Reports/GROUP_06_TESTING_GUIDE.md`,
  `Reports/_TESTING_GUIDE_TEMPLATE.md` (format standard for all guides).

### Build / run
`$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'` → `.\gradlew.bat build` → copy
`build\libs\customblocks-1.0.0.jar` to `%APPDATA%\.minecraft\mods\`. Golden Rule: nothing's done until
the developer confirms in-game.

---

## 2026-06-10 — Finale Fix Group 06: Omni air-click, prefix unify, lore pass, Magic GUI (✅ verified in-game)

### Done (code written this session)
Four handoff items from `GROUP_06_HANDOFF.md`. 3 of 4 coded; 1 blocked on a developer decision.

**1. Omni-Tool sneak-click works in the air** — `item/OmniToolItem.java`: added a `use(World, PlayerEntity, Hand)`
override (the air / non-block right-click hook). Sneaking opens the mode-switch GUI in the air and on any
non-custom block; a plain air-click passes through. `useOnBlock` still handles custom blocks and returns
SUCCESS, so `use()` never double-fires there. Mirrors the file's existing client-SUCCESS / server-does-work
pattern; reuses the same `GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.OMNI))` call already present.

**3a. [CB] prefix unified** (developer chose: black-bracket chat format everywhere) — `command/Chat.java`:
deleted `HUD_PREFIX`; `Chat.tool()` (action bar) now uses `PREFIX`. `gui/chest/CbChestHandler.java:78`
repointed `Chat.HUD_PREFIX` -> `Chat.PREFIX`. Grep confirms no other `HUD_PREFIX` / hand-built `[CB]` refs.
Note: black brackets are dim on the dark hotbar bar — the developer's accepted trade for one identical look.

**3b. Lore humanized for every item** — rewrote `appendTooltip` in `OmniToolItem`, `DeleterItem`,
`RainbowRectangleItem`, `ShapeMarkerItem` (the shared class driving all 8 markers); and ADDED `appendTooltip`
(plus Item/ItemStack/Text imports) to `LuminaBrushItem` and `ChiselItem`, which previously had none.

**4. Magic Items GUI -> double chest** — rewrote `gui/chest/MagicMenu.java` as a 6-row menu: light-blue
framed border, the 3 hand tools (Omni / Rainbow / Deleter) on the top interior row, the 8 colour markers in
a 4-colour grid (each colour's Square directly above its Triangle), a Nether-Star header, a "Marker Shapes"
label, and back / edit-toggle / close on the bottom row. Enabled items glint; disabled are dimmed. Items now
resolve by id from the item registry (`Registries.ITEM.get`) so adding a seed item only needs a slot mapping.
Seeded the 8 markers into `core/MagicItemsManager.java` (so they get enable/disable persistence).

### Task 2 (resolved): keep two separate tabs — NO code change
**Tools creative tab on Page 5 (wanted Page 2 next to Blocks).** Verified: both tabs already register
back-to-back in `CustomBlocksMod` (the only lever Fabric gives); both source trees share mod id `customblocks`
(so no stray old jar is co-loading — both tabs are from the one mod). Fabric's official 1.21 docs expose NO
API to pin a custom tab to a page or force it adjacent to another tab — a clean two-tab "Page 2" fix is not
supported. Options offered: merge tools into the Blocks tab (one tab, reliably adjacent) vs keep two tabs.
**Developer chose: keep two separate tabs.** No tab code changed; placement stays as Fabric orders it in the
137-mod pack. Per the handoff, did NOT guess-change the tab code.

### Verified (static only)
- File-size gate: all touched files well under §9.3 (MagicMenu ~135/500, OmniToolItem ~135/500, others small).
- API cross-check: `use()` signature taken from the old project's `LuminaBrushItem` (1.21.1
  `TypedActionResult<ItemStack> use(World, PlayerEntity, Hand)`); ChestMenu/Icons/GuiRouter/Nav APIs read
  from source before use; `MagicMenu.build(player, edit, page)` signature kept (GuiRouter calls it unchanged).

### Verified in-game (2026-06-10) — ✅ DONE
- `gradlew build` green (compile + all 3 gates, 27s); jar copied to `.minecraft\mods\`.
- Developer confirmed all 4: Omni air sneak-click opens the GUI; [CB] identical in chat + hotbar;
  new lore on every tool/marker; `/cb` -> Magic Items double-chest with all 11 items.

### Marked PARTIAL (revisit later)
- **Item lore** = partial (polish later), same convention as the GUIs. A dedicated lore pass later
  for tone consistency + per-item flavour; Shape/Rainbow lore to tighten once M2–M4 mechanics land.

### Next
- Continue queued mechanics, foundation-first: **M1** — CIE-LAB background remover + 3-mode config
  (see `docs/Finale Fix/Reports/GROUP_06_HANDOFF.md` §2–3).

---

## 2026-06-10 — Group 03: HUD overlay, HUD editor & ESC-menu buttons (written, NOT yet built/tested)

### Done (code written this session)
Implemented Group 03 (source issues 17.4 HUD config, 17.6 HUD overlay, 17.7 ESC menu). 5 new files,
3 rewrites, 4 edits.

**New (5):**
- `client/gui/HudEditorScreen.java` — Lunar-style drag-to-reposition overlay editor. World stays
  visible (`renderBackground` no-op, `shouldPause()` false); live preview; buttons for Scale ±,
  Color cycle, BG opacity ±, ID/Name toggles, Reset, Save, Cancel. Drag via mouseClicked/Dragged/
  Released with clamping; Cancel/Esc reverts to the snapshot taken on open.
- `client/gui/EscMenuButtons.java` — on `ScreenEvents.AFTER_INIT`, if the screen is `GameMenuScreen`,
  adds two `CbIconButton`s below the lowest vanilla button: "CustomBlocks Menu" (closes menu, runs
  `/cb`) and "HUD Editor" (opens `HudEditorScreen`).
- `client/gui/CbIconButton.java` — `ButtonWidget` that also draws a Command Block item icon.
- `mixin/ScreenInvoker.java` — `@Invoker` exposing Screen's protected `addDrawableChild` so the ESC
  buttons render + receive clicks like vanilla. Registered in `customblocks.mixins.json` (client).

**Rewritten (3):**
- `client/HudConfig.java` — full client-side config + atomic persistence to
  `config/customblocks/data/hud-config-server.json` (visible/x/y/scale/color/bgOpacity/showId/
  showName), palette cycle, clamps, load()/save()/resetDefaults()/syncFromConfig(). 170 lines (≤300).
- `client/HudRenderer.java` — split into `render(ctx)` (live, from mixin) + shared `draw(...)` honoring
  position/scale/color/bg/flags (used by the editor preview) + `boxSize(...)` for hit-testing.
- `command/handlers/GuiCommands.java` — `/cb edithud` now sends `OpenGuiPayload(HUD_EDITOR)` instead
  of toggling.
- `client/CustomBlocksClient.java` — `HudConfig.load()` on init; `HUD_EDITOR` case in the OpenGui
  switch; `HudStatePayload` receiver now sets `HudConfig.visible` + `save()`; registers
  `ScreenEvents.AFTER_INIT → EscMenuButtons`.

**Edited (4):**
- `network/HudSync.java` — separator bug fix: `id + ' ' + name` → `id + '\u0000' + name` to match
  `ClientSlotCache`'s NUL split (was producing blank/garbled HUD name lines).
- `gui/GuiMode.java` — added `HUD_EDITOR(6)`.
- `command/handlers/ConfigCommands.java` — added `/cb config hud` (status / toggle / on / off);
  resolves the dangling `/cb config hud on|off` that `ConfigScreen` already referenced.
- `src/main/resources/customblocks.mixins.json` — registered `ScreenInvoker` under `client`.

### Verified (static only)
- File-size gate: HudConfig 170/300, HudEditorScreen 205/500, ConfigCommands 113/400, all others
  well under §9.3 limits.
- Cross-checked APIs against real source: `ClientSlotCache.get/populate`, `SlotBlock.getSlotIndex`,
  `CustomBlocksConfig.hudEnabled/save`, `Chat.info/success`, `OpenGuiPayload`/`HudStatePayload`
  shapes, `CustomBlocksMod` S2C registrations (HUD_EDITOR reuses the existing OpenGuiPayload).
- Testing guide written: `docs/Finale Fix/Reports/GROUP_03_TESTING_GUIDE.md` (G03.1–G03.10).

### NOT verified — read before testing
- **Not compiled and not run.** The sandbox cannot build: no network, no cached Gradle 8.8
  distribution, and only JDK 25 present (project needs Temurin JDK 21). Compile errors remain
  possible until `gradlew build` runs on the dev PC.
- ESC buttons use `ScreenEvents.AFTER_INIT` + a `ScreenInvoker` invoker mixin (not a hand-patched
  `GameMenuScreen.init` mixin); behaviour must be confirmed in-game.
- HUD look settings persist client-side; only on/off is server-driven.
- Per the Golden Rule, none of this is DONE until confirmed in-game.

### Next session
- Run `gradlew build` on the dev PC; fix any compile errors.
- Test G03.1–G03.10 in-game per the Group 03 testing guide.

---

## 2026-06-09 — Group 02: Chest GUI Core Infrastructure (written, NOT yet built/tested)

### Done (code written this session)
Replaced the screen-based GUI with a server-side chest-GUI system. 15 new files + 5 edits.

**New — `gui/chest/` framework + menus (12):**
- `Icons.java` — stained-glass fillers + item-icon builder (name/lore/glint via DataComponentTypes).
- `Nav.java` — per-player back-stack: `Dest` enum (MAIN, BLOCK_LIST, EDITOR, UNDO, REDO, HISTORY, MAGIC, MAGIC_EDIT) + `MenuKey(dest, arg, page)` record.
- `ChestMenu.java` + `CbChestHandler.java` — generic 1–6 row chest container + click router (mirrors the proven old `CbScreenHandler`).
- `GuiRouter.java` — open/navigate/repage/back/render + command delegation (`runCommand`, `runAndReopen`, `promptCommand`, `confirmCommand`) via `executeWithPrefix`.
- `Layout.java` — shared paginated footer (back / prev / page / next / close).
- `MainMenu`, `BlockListMenu`, `EditorMenu`, `UndoMenu`, `HistoryMenu`, `MagicMenu`.

**New — core + command (3):**
- `core/MutationLog.java` — in-memory audit log (actor / action / blockId / time) for the history GUI.
- `core/MagicItemsManager.java` — magic-item registry + enabled-state persistence.
- `command/handlers/ChestGuiCommands.java` — registers `/cb`, `menu`, `dashboard`, `admingui`, `gui [block <id>]`, `editor <id>`, `listgui`, `undogui`, `redogui`, `history`, `magicitems`, `editmagicitems`, `rp pause|resume`, `sync`, `unsuppress`.

**Edited (5):**
- `CommandRegistrar` — register `ChestGuiCommands` after `GuiCommands`.
- `GuiCommands` — removed the old screen `gui`/`admingui` registrations (kept `edithud`).
- `UndoManager` — added `undoStack(uuid)`/`redoStack(uuid)` reads + a `MutationLog` hook in `push()`.
- `HistoryCommands` — added public `undoOnce`/`redoOnce` for the visual undo/redo menus.
- `ResourcePackServer` — added `pause`/`resume`/`isPaused`/`syncToAll`/`unsuppress`; `updatePack()` no-ops while paused, `sendToPlayer()` no-ops while suppressed.

Every mutating editor action DELEGATES to the existing tested `/cb` commands (give/setglow/sethardness/setsound/setcollision/setcategory/rename/retexture/note/delete) so locking, undo recording, lighting and chat feedback behave identically. Command arg formats verified against `AttributeCommands`.

### Verified (static only)
- File-size gate: all files within §9.3 limits (largest handler `ChestGuiCommands` 156 / 400).
- Mojibake gate (exact build.gradle patterns) + sound gate: 0 hits.
- Command-literal collision scan: no duplicate literals introduced.
- API cross-check against real source: `SlotManager`, `SlotData`, `SlotBlock`, `ToolItems`, `Chat`, `executeWithPrefix`, `DataComponentTypes`, `ClickEvent`/`HoverEvent` ctor style — all confirmed.
- Covers tests G02.1–G02.11.

### NOT verified — read before testing
- **Not compiled and not run.** The sandbox cannot build (no Gradle/JDK here), so compile errors remain possible until `gradlew build` is run on the dev PC.
- Known non-blocking javac warnings only (not errors): `GuiCommands` has a pre-existing duplicate `ServerPlayerEntity` import, and its `openGui` method is now unused/dead.
- Minor spec deviation: `/cb history` shows the paginated log but does NOT yet implement the optional shift-click "filter by player" from §6.
- Per the Golden Rule, none of this is DONE until confirmed in-game.

### Next session
- Run `gradlew build` on the dev PC; fix any compile errors.
- Test G02.1–G02.11 in-game.

---

## 2026-06-07 — Phase 16: the 32K startup-warning investigation (CORRECTED)

### What was seen
Client log (`.minecraft/logs/latest.log`) showed ~32,000 lines of:
`Exception loading blockstate definition: 'customblocks:blockstates/slot_N.json'
missing model for variant: 'customblocks:slot_N#light=K'`, for N = 0..2047, K = 0..15.

### Root cause (confirmed, not guessed)
- The active config `…/.minecraft/config/customblocks/config.json` has `maxSlots: 2048`, so the
  mod registers **2048 blocks** (slot_0 … slot_2047).
- `SlotBlock` carries `IntProperty LIGHT (0..15)` — dynamic glow MUST live in the blockstate
  because luminance is frozen at block construction. That is **16 block-states per block**.
- 2048 × 16 = **32,768 states**, each needing a model.
- The game resolves models at **client launch / world load — BEFORE joining any server.** At that
  moment only vanilla + the **mod jar** are loaded. The mod jar ships **zero** blockstate/model
  files (`src/main/resources/assets/customblocks/blockstates/` is empty). So every state logs
  "missing model." That is the 32K.
- The HTTP resource pack (`ServerPackGenerator`) only loads **after a player joins**, so it can
  never silence launch-time warnings, no matter what it contains.

### Decision
- These are **harmless WARN noise** — no crash, blocks still render once textures arrive. The
  developer chose to **accept them as expected** for now.
- The previous commit (`P0 Fix: eliminate 32K console warnings`) was based on a wrong assumption
  (that the HTTP pack was the source) and was **reverted in full**. That commit had also deleted
  the empty-slot loop, which legitimately suppresses post-join warnings — restoring it.

### Correction of the earlier misdiagnosis
- WRONG: "empty-string `\"\"` variant fails for LIGHT blocks." The `\"\"` catch-all is valid and
  matches all states; it was not the cause. Restored.
- WRONG: "writing empty-slot JSONs floods the log." Those JSONs *suppress* post-join warnings;
  removing them removed a safety net. Restored.

### Future option (not built — needs its own phase)
Eliminate launch warnings forever by giving the **mod itself** a model for every registered slot
at launch — e.g. a Fabric always-enabled runtime resource pack that synthesizes one
blockstate + cube_all model per slot in memory (count auto-matches `maxSlots`). Textures still
come from the HTTP pack for assigned slots; unassigned just show the missing-texture checkerboard,
silently. To discuss before building.

---

## 2026-06-05 — Phase 13/16 final push: Video, GUI screens, Mod Menu (Batch 14)

### Done (build-verified after `gradlew build` downloads jcodec + modmenu)
- **VideoDecoder** (Phase 13) — jcodec 0.2.5 MP4 frame extractor. `extractFrameAsPng(File, int)`
  seeks to a frame, converts via AWTUtil, scales via ImageProcessor. Throws cleanly on bad
  frames. jcodec + jcodec-javase 0.2.5 uncommented in build.gradle.
- **VideoCommands** — `/cb video list` enumerates `.mp4` files in `config/customblocks/videos/`.
  `/cb video extract <file> <id> <frame>` extracts frame N as the texture for block `<id>`.
  Runs on a daemon thread (never blocks tick). Registered in CommandRegistrar.
- **MacroListScreen** (Phase 10 GUI) — dedicated GUI screen for macro management opened via
  `/cb gui macros` or Main Menu → "Macros". Buttons: View All, Record New, Play, Delete, Add
  Step, Stop Recording.
- **ArabicBrowserScreen** (Phase 10 GUI) — dedicated GUI screen for the Arabic system opened
  via `/cb gui arabic` or Main Menu → "Arabic Letters". Buttons: Import All, Single Letter,
  Word Block, List.
- **Mod Menu integration** (Phase 16) — `ModMenuApiImpl` opens ConfigScreen when the ModMenu
  "Config" button is clicked. `modmenu:11.0.2` added as compile-only dep; terraformersmc maven
  repo added; `modmenu` entrypoint added to fabric.mod.json.
- **GuiCommands** — added `/cb gui macros` and `/cb gui arabic` routes.
- **CustomBlocksClient** — routed `MACRO_LIST` and `ARABIC_BROWSER` modes to their screens.
- **MainMenuScreen** — "Macros" and "Arabic Letters" buttons now open dedicated GUI screens
  instead of running list commands.
- All 3 build gates pass; jcodec + modmenu IDE errors are "missing classpath" warnings that
  clear after `.\gradlew.bat build` downloads the new dependencies.

### Pending developer action
- Run `.\gradlew.bat build` — this downloads jcodec + modmenu-api (first time will be slow).
- Test TESTING_GUIDE §1j–§1n (Batches 10–14). On "works": commit to `dev`, merge to `main`.
- Add a 64×64 PNG as `src/main/resources/pack.png` for the Mod Menu mod icon (optional).
- Place `.mp4` video files in `run/config/customblocks/videos/` to test video extraction.
- Phase 16 distribution: `.\gradlew.bat build` → `build/libs/customblocks-1.0.0.jar`.

### Architecture status
All 16 phases are now code-complete (stubs where full implementation requires external services
or developer-provided files). The only remaining hard blockers before v1.0.0 ship are in-game
confirmation of Batches 10–14 and the stress/multiplayer tests from Phase 16.

---

## 2026-06-05 — Phases 10–15 big push (Batch 13)

### Done (build-verified, NOT yet in-game confirmed)
- **MacroManager** — record/replay named command sequences. `/cb macro record <name>`,
  `add <cmd>`, `stop`, `cancel`, `play <name>`, `list`, `delete <name>`. Persists to
  `config/customblocks/macros/<name>.json`.
- **Arabic system** — `ArabicLetterMap` (28 letters), `ArabicWordRenderer` (Java2D PNG,
  loads `arabtype.ttf` from `config/customblocks/arabtype.ttf` or system fallback),
  `ArabicBlockRegistry`. Commands: `/cb arabic import`, `letter <name>`, `word <text> <id> <name>`,
  `list`.
- **Diagnostics** — `DiagnosticsHelper` + `IncidentRecorder`. `/cb diag` shows system snapshot
  (slots, heap, TPS, pack size). `/cb incidents` shows the incident log.
- **Cloud + Discord stubs** — `/cb vault` and `/cb discord` give setup instructions and fire
  when `vaultEndpoint` / `discordWebhookUrl` are set in config.json.
- **AI stubs** — `AiCommandParser` + `AiTextureGenerator` return null until `aiApiKey` is set.
- **GUI system (Phase 10)** — `GuiMode`, `GuiEngine`, `GuiState`, `OpenGuiPayload`.
  Screens: `MainMenuScreen` (7-button hub), `BlockEditorScreen` (per-block attributes),
  `ConfigScreen` (live config values). `/cb gui`, `/cb gui block <id>`, `/cb gui config`,
  `/cb admingui`, `/cb edithud` (toggle HUD). Screens open client-side via the server packet.
- **HUD overlay (Phase 11)** — `HudSyncPayload` syncs block index to client on join.
  `ClientSlotCache` caches it. `HudRenderMixin` injects into InGameHud to show
  `§e<id> §7"<name>"` when looking at a custom block. Toggle with `/cb edithud`.
- **OnboardingManager** — detects first-time players, sends a clickable welcome message once.
- **Config fields added** — `hudEnabled`, `aiApiKey`, `aiTextureEnabled`, `vaultEndpoint`,
  `discordWebhookUrl`.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Pending developer action
- Place `arabtype.ttf` in `config/customblocks/arabtype.ttf` for proper Arabic letter rendering
  (system fallback works but may lack full Arabic shaping on some servers).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12) PLUS new §1n (Batch 13).
- On "works": commit all Batches 3–13 to `dev`.
- Remaining: Phase 13 video (jcodec dep needed), Phase 14 Cloud (needs Cloudflare Worker URL),
  Phase 16 polish (Mod Menu, README finalization).

---

## 2026-06-05 — Phase 9 completion: Lock, Notes, Favorites, Drafts (Batch 12)

### Done (build-verified, NOT yet in-game confirmed)
- **LockManager** — `config/customblocks/locks.json`; atomic persist. `/cb lock <id>`,
  `/cb unlock <id>`, `/cb locked` (list with `[unlock]` buttons). Lock checks wired into
  every mutation point: `delete`, `rename`, `retexture`, `setglow`, `sethardness`,
  `setsound`, `setcollision`, `setcategory`, `template apply`, and the Deleter tool.
- **BlockNotesManager** — `config/customblocks/notes.json`. `/cb note <id>` (show),
  `/cb note <id> <text>` (set, greedy string), `/cb note <id> clear` (remove).
  Notes auto-cleaned when the block is deleted.
- **FavoritesManager** — per-player bookmarks in `config/customblocks/favorites.json`.
  `/cb fav <id>` toggles (add/remove). `/cb favs` lists with `[give]` + `[unfav]` buttons.
- **DraftManager** — `config/customblocks/drafts.json`. `/cb draft <id>` marks as draft,
  `/cb publish <id>` removes the mark, `/cb drafts` lists with `[publish]` buttons.
- **`/cb list` and `/cb search`** now show `§c[locked]` and `§8[draft]` status tags inline.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12). On "works": commit Batches 3–12 to `dev`.
- Discuss next step: color ecosystem, remaining Phase 9 items, or Phase 10 GUI planning.

---

## 2026-06-05 — UX polish: Deleter visual, export menu, template overhaul (Batch 11)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter visual fixed:** after deleting a block definition, the placed block now shows the
  purple/black "missing texture" checkerboard (Minecraft's standard placeholder) instead of
  going fully transparent/invisible. Fixed in `ServerPackGenerator.emptyModelJson()` — empty
  slot model changed from no-geometry invisible to `cube_all` referencing an absent texture.
- **`/cb export` redesigned:** bare `/cb export` now shows an action menu with
  `[.json] [.txt] [to Vault]` buttons. `/cb export <id>` shows `[to Config] [to Vault] [Download]`
  buttons. Per-block export no longer shows file paths or copy-path buttons. Vault stubs cleanly
  to "Phase 14". New download action writes the JSON and gives an HTTP link
  (`http://host:port/export/<id>`) served by a new `/export/` route in `ResourcePackServer`.
- **`/cb template` overhauled:**
  - List shows each template's attributes inline: `glow:X hard:Y sound:Z [passable] [cat]`.
  - Empty list explains what templates are and how to use them.
  - Save/apply confirmations now show the captured/applied attribute set.
  - New `/cb template delete <name>` subcommand.
  - Tab suggestions for template names on `apply` and `delete` arguments.
- Build green; all 3 gates pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10) + §1k (Batch 11). On "works": commit Batches 3–11.
- Continue with the next phase (color ecosystem or LockManager/BlockNotes — developer's call).

---

## 2026-06-05 — Phase 9 completion + Deleter redesign (Batch 10)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter redesigned:** right-click now deletes the block's entire definition (slot data +
  texture + pack rebuild + undo record), not just the placed instance. Placed block stays,
  reverts to unassigned-slot appearance. Matches `/cb delete` semantics.
- **Per-block export:** `/cb export <id>` → `exports/<id>.json` (schema v1: all attributes,
  importable). Existing `/cb export json|txt` bulk export unchanged.
- **importfolder:** `/cb importfolder [path]` scans a folder for per-block JSONs, creates
  missing blocks, skips existing ids, reports results. Triggers pack rebuild on any creation.
  Defaults to `exports/` when no path given.
- **Templates:** `TemplateManager` + `TemplateCommands`. `/cb template save/apply/list`.
  Apply is undoable. Templates stored as `config/customblocks/templates/*.json`.
- Build green; all 3 gates (verifyFileSize, verifyMojibake, verifySound) pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10). On "works": commit Batches 3–10 to `dev`.
- Continue with the next phase in order (LockManager / BlockNotes, or color ecosystem).

---

## 2026-06-04 — Test feedback fixes (Batch 9): Deleter, HUD prefix, undo scope

### From dev's in-game testing
- **Deleter:** said "Removed" even when it didn't delete. Fixed: now uses `World.breakBlock`
  (reliable sync + particles, ignores hardness so it still kills unbreakable blocks) and only
  confirms when the boolean says a block was actually removed.
- **HUD prefix invisible:** tool action-bar messages used the chat `[CB]` prefix whose `§0`
  black brackets vanish on the dark HUD. Added `Chat.HUD_PREFIX` (aqua) for tool feedback.
- **Undo scope:** added `/cb config undomode` cycling **server-wide** (new default) ↔
  **per-player**; `/cb config` shows it. `UndoManager` now keys its stacks by mode (shared
  GLOBAL_KEY vs per-UUID). Switching scope clears history. Persisted as `undoMode`.

### Parked for Phase 10 (dev's call — do NOT start Phase 10 yet)
- **Lumina Brush + Chisel → merge into ONE GUI "Block Editor" tool.** Left untouched for now;
  their placeholder icons (glowstone dust / amethyst shard) get finalized then too. The dev
  wants to continue phases one-by-one, not jump to the GUI system early.

### Next
- Dev re-test: Deleter actually removes + readable HUD text; `/cb config undomode` toggle
  (TESTING_GUIDE §1g + §1i). On "works": commit Batches 3–9 to `dev`. Then continue in order.

---

## 2026-06-04 — Phases 8+9 warped (Batch 8): Categories & Search

### Done (build-verified, NOT yet in-game)
- **Categories (Phase 8):** new immutable `SlotData.category` field (persisted, undoable).
  `/cb setcategory <id> <name>` (`none` clears). `/cb categories` lists categories + counts
  with clickable [list]. Pattern-identical to glow/sound — same setter/undo/persist path.
- **Search (Phase 9):** `/cb search <query>` matches id / display name / category
  (case-insensitive), each hit clickable [give]. Pure reads via `SlotManager.search/byCategory/
  categories`. `/cb list` now shows a `[category]` tag.
- Chose single-category-per-block (minimal, clean) over the old multi-category CategoryManager;
  can expand later if the GUI needs it. Build green; all 3 gates pass.

### Why this cluster
- Categories + Search are the two most tightly-connected metadata phases and carry zero image
  risk, so warping them was safe. The color ecosystem (recolor pipeline + the 4 color tools)
  is deferred — it's the one area the old project had real bugs (BFS recolor), so it gets its
  own careful batch.

### Next
- Dev batch-test Phases 6–9 (TESTING_GUIDE §1f–§1h). On "works": commit Batches 3–8 to `dev`.
- Then either color ecosystem (Phase 8 color) or templates/import-export (Phase 9 rest).

---

## 2026-06-04 — Phase 7 tools (Batch 7): Lumina Brush, Chisel, Deleter

### Done (build-verified, NOT yet in-game)
- **3 hand tools** in a new `item/` package, listed in a new "CustomBlocks Tools" creative tab:
  - **Lumina Brush** → cycles a clicked block's glow (sneak = backward); reuses setGlow path,
    refreshes placed copies, undoable.
  - **Chisel** → cycles hardness presets instant/soft/stone/hard/tough/unbreakable (sneak =
    backward); undoable.
  - **Deleter** → instantly removes a placed custom block (no drop), even unbreakable ones.
- **`CustomToolItem` base** gates work on `ServerPlayerEntity` (NOT `world.isClient`) so there's
  no client-side skip delay (Bible §9.6 pitfall). Tool models reuse vanilla item textures.
- Build green; all 3 gates pass.

### Deferred (need subsystems not built yet)
- Color Square/Triangle, Rectangle, Golden Hexagon → require the color-replace + region
  pipeline; these belong with Phase 8 (color ecosystem). Building them now would mean writing
  big machinery blind. Not done — flagged for Phase 8.

### Next
- Dev batch-test Phases 6–7 (TESTING_GUIDE §1f + §1g). On "works": commit Batches 3–7 to `dev`.
- Then Phase 8 (color ecosystem) — unlocks the remaining color tools.

---

## 2026-06-04 — Phase 6 undo/redo (Batch 5) + setcollision

### Done (build-verified, NOT yet in-game)
- **Undo/redo (per-player).** New `core/UndoManager` — clean ~150-line per-player stacks of
  immutable `SlotData` snapshots (the old one was 1,170 lines of disk-delta machinery; not
  ported). New `command/handlers/HistoryCommands` registers `/cb undo` + `/cb redo`.
  - Recording wired into create / delete / rename / dupe / setglow / sethardness / setsound.
  - `SlotManager.restoreSnapshot` + `removeSilently` = non-recording restore primitives so
    undo never re-triggers itself. Delete-undo restores the texture bytes too.
  - Config: `maxUndoDepth` (default 25). Per-player isolation via UUID-keyed stacks.
- **setcollision** (walk-through blocks): `/cb setcollision <id> <solid|passable>`. New
  immutable `SlotData.noCollision` field (persisted, undoable). `SlotBlock.getCollisionShape`
  returns an empty box live when passable — entities pass through, outline stays breakable.

### Next
- Dev in-game test: undo/redo per-player + collision toggle (TESTING_GUIDE §6); 2-player
  isolation if possible. Then the 7 tools (Phase 7) as their own focused, tested batch.

---

## 2026-06-04 — Bug fixes: mining formula, [CB] chat format (sound path verified)

### Fixed
- **Hardness mining was wrong (Bug 1 + 2).** The ported formula divided by 100 unless tool
  speed > 1 — but our blocks aren't in any tool tag, so speed is always 1.0 and it ALWAYS
  took the slow ÷100 branch (default 1.5 ≈ 7.5s by hand → felt unbreakable). Replaced with
  Minecraft's EXACT vanilla formula `getBlockBreakingSpeed / hardness / (canHarvest?30:100)`
  using live hardness. Now low = fast, high = slow, -1 = unbreakable, default 1.5 = stone-like.
- **Default hardness:** verified `DEFAULT_HARDNESS = 1.5` (no -1 regression). The "unbreakable
  default" was the broken formula, now fixed.
- **Chat bloat (Bug 4):** added `command/Chat` with the old project's `§0§l[§b§lCB§0§l]§r`
  prefix + ✔/✖ glyphs. Converted Creation/Attribute/Utility command messages to short
  branded lines (dropped the multi-line explanations).

### Verified, NOT changed
- **setsound (Bug 3):** traced the full path — command → `SlotManager.setSoundType` (updates
  BY_ID + BY_SLOT) → `saveAll` writes `"sound"` → `SlotBlock.getSoundGroup` reads it live
  (confirmed `state.getSoundGroup()` delegates live to the block, no caching). The code is
  correct end-to-end; no defect found. Likely the client wasn't relaunched after the build, or
  a subtle sound. Isolation test added to TESTING_GUIDE (check slots.json + a fresh runClient).

### Next
- Dev re-test in Survival: mining speed scales correctly; `[CB]` messages are concise; setsound
  isolation test. Then commit + continue Batch 4 (undo/redo).

---

## 2026-06-04 — Hardness words + message fix + Batch 4 slice 3 (sounds)

### Findings explained (from dev testing)
- "Hardness 1/20/50 all feel the same" = **Creative mode** breaks any positive-hardness block
  instantly; only `-1` (unbreakable) is honored there. Hardness break-times only differ in
  **Survival**. Not a bug — explained to the dev.
- The "(vanilla stone = 1.5)" hint was appended to every value (looked like it called 50
  "vanilla stone"). Fixed the wording.

### Done (build-verified, NOT yet in-game)
- **sethardness word values:** accepts `unbreakable` / `instant` / `stone` (with tab
  suggestions) as well as numbers. Clearer message + a Survival-vs-Creative note.
- **Sounds (Phase 6, slice 3):** `/cb setsound <id> <type>` (17 vanilla groups). `SlotData.soundType`
  (immutable, default "stone", persisted); read live in `SlotBlock.getSoundGroup`
  (+ shared `SlotBlock.soundGroupFor`/`SOUND_TYPES` for the command's validation/suggestions).

### Next
- Dev in-game test: TESTING_GUIDE §1e — hardness words **in Survival**, setsound on
  break/step. On "works": commit Batch 3 + glow + hardness + sounds to `dev`, then undo/redo.

---

## 2026-06-04 — Glow confirmed ✅ + glow cap + Batch 4 slice 2 (hardness)

### Done
- **Glow confirmed working in-game** by the developer ("done it perfectly"). ✅
  The BlockState-property approach (ADR-002) actually emits light.

### Done (build-verified, NOT yet in-game)
- **Side quest — setglow > 15:** Minecraft caps block light at 15 (4-bit, hard engine limit).
  `setglow` now accepts ANY number and caps at 15 with a friendly message instead of a red
  Brigadier rejection. There is no way to emit more than 15 — explained to the developer.
- **Hardness (Phase 6, slice 2):** `/cb sethardness <id> <value>` (negative = unbreakable,
  0 = instant, 1.5 = vanilla stone). `SlotData.hardness` (immutable, default 1.5, persisted);
  read **live** in `SlotBlock.calcBlockBreakingDelta` (ported formula) — no state property
  needed because hardness, unlike luminance, is queried live each break attempt.

### Next
- Dev in-game test: TESTING_GUIDE §1e (glow cap message + hardness: unbreakable / instant /
  slow). On "works": commit Batch 3 + glow + hardness to `dev`, then continue Batch 4
  (custom sounds, then undo/redo and tools).

---

## 2026-06-04 — Batch 3 polish + Batch 4 slice 1 (glow) build-verified

### Done (build-verified, NOT yet in-game)
- **Old-format create:** `/cb create <id> <name> <url>` makes + textures in one command
  (name is now a quoted string so a URL can follow it; `<id>` and `<id> <name>` still work).
- **Bug fix (from screenshot):** the failing URL was a `data:` URI, which `HttpClient`
  rejects ("invalid URI scheme data"). Now non-http(s) URLs are caught **before** the block
  is created → clear message, no half-made block, no "already exists" cascade.
  (`ImageDownloader.isHttpUrl` + pre-create check.)
- **Tab auto-complete** (`BlockSuggestions.IDS`) for delete/rename/dupe/retexture/setglow/give.
- **Glow attribute (Phase 6, slice 1):** `/cb setglow <id> <0-15>` via a new
  `AttributeCommands` handler; `SlotData.glow` (immutable, 0–15, persisted) is the
  configured default for new placements.

### Debug — glow was NOT working (fixed)
- Root cause (verified in the 1.21.1 bytecode): `AbstractBlockState.getLuminance()` returns
  a `final int` field computed ONCE at state construction. A `.luminance(state -> liveValue)`
  lambda is therefore frozen at registration (value 0, before any glow is set) — so dynamic
  glow via a lambda can NEVER work. The old project had the same dead lambda + a
  `triggerGlowUpdate` that scanned `getBlockEntityPositions()` (SlotBlock has no block entity),
  so its glow was effectively broken too.
- Fix (vanilla-idiomatic, like `LightBlock`): light is now a real **block-state property**
  `LIGHT (0..15)` on `SlotBlock`. `luminance` reads `state.get(LIGHT)` → baked per state →
  actually emits. `getPlacementState` applies the slot's configured glow to new placements;
  `block/SlotLighting` rewrites the LIGHT state of already-placed instances near players.
- Pack-safe: the generator's `""` catch-all variant matches all 16 states (verified in the
  `BlockStatesLoader` bytecode — empty key = empty predicate = match-all), so no purple blocks.

### Decisions
- Attribute setters live in their own `AttributeCommands` handler (design rule #5),
  not in CreationCommands — keeps each handler focused and under the size cap.
- Glow stored in the block STATE (not just SlotData): a placed block's light is saved in the
  chunk, survives reload independently, and is correct per-instance. See `docs/adr/ADR-002`.

### Next
- Dev in-game test: TESTING_GUIDE §1d (texture, incl. one-shot create + bad-URL §3.2b) and
  §1e (glow). On "works": commit Batch 3 + glow to `dev`, then continue Batch 4
  (hardness, sounds, undo/redo, tools).

---

## 2026-06-03 — Batch 3 (Phase 4: textures) build-verified

### Done (build-verified, NOT yet in-game)
- `image/ImageDownloader` (browser headers) + `image/ImageProcessor` (bicubic square PNG).
- `core/TextureStore` — per-slot PNG files (atomic); textures stay out of slots.json.
- `network/ServerPackGenerator` — pack_format 34; textured cube_all for created blocks;
  one shared invisible model for empty slots (kills the missing-model warnings).
- `network/ResourcePackServer` — embedded HTTP `/pack.zip`, SHA-1, ResourcePackSendS2CPacket
  to ALL clients (single pack path; no modded/vanilla split).
- `/cb retexture <id> <url>` — download/decode off the server thread, then rebuild + push.
- Mod wiring: HTTP start/build on SERVER_STARTED, stop on SERVER_STOPPED, send pack on join,
  creative tab → created-blocks-only. Config: textureSize=64, httpHost=127.0.0.1.

### Design note (de-risking)
- Chose a single HTTP-pack path for ALL clients to avoid the old PACK2 dual-system conflict.
  Client-side generation (Phase 5) is now optional. Textures work everywhere via the pack;
  multiplayer block NAMES + creative-tab contents still need client sync (Phase 5).

### Known rough edge
- Each retexture re-prompts the player to accept the pack (vanilla mechanism). Smoothing
  this (client auto-apply) is a Phase 5 nicety.

### Next
- Dev in-game test (TESTING_GUIDE §1d): create -> retexture <url> -> accept pack prompt ->
  see the textured block; survives a restart. Then commit Batch 3 to dev.

---

## 2026-06-03 — Phase 1 CONFIRMED ✅ + Batch 2 (Phases 2-3 core)

### Context
- Developer asked to batch related phases (16 small steps was overwhelming) and to lower
  maxSlots to **800** for faster load. New plan = 6 testable batches; this is Batch 2.

### Done (build-verified, NOT yet in-game)
- Phase 1 confirmed working in-game ("Phase 1 works"). ✅
- `maxSlots` default → 800.
- `SlotDataStore` — atomic JSON persistence (sole slot disk-I/O class, rule #4).
- `SlotManager` — create/delete/rename/dupe + loadAll/saveAll/reload; orphan guard on load.
- `/cb` command tree: `CommandRegistrar` + `CreationCommands` (create/delete/rename/dupe)
  + `UtilityCommands` (list/give/reload). LANG1 avoided by construction (clean literal tree).
- Wired `CustomBlocksMod`: loadAll after registerAll, register commands, saveAll on stop.
- `gradlew build` PASSED, all gates green; no file over its size limit.

### Deferred (on purpose, to later batches)
- Undo/redo → batched with attributes (Batch 4 / Phase 6).
- retexture → needs the image pipeline (Batch 3 / Phase 4).
- Creative tab still lists all 800 slots; will switch to "only created blocks" in Batch 3.

### Next
- Developer in-game test of Batch 2 (TESTING_GUIDE §1c): create/list/give/place/rename/
  dupe/delete + survives a restart. Then Batch 3 = textures.

---

## 2026-06-03 — Phase 0 CONFIRMED ✅ + Phase 1 start

### Done
- **Phase 0 confirmed working in-game by the developer** ("Phase 0 works"). Mod loads in
  MC 1.21.1, the three `[CustomBlocks]` startup lines appear, no crash. Phase 0 is DONE.

### Now starting — Phase 1: Block Slot System
- Goal (Bible milestone): the game starts with **1028 blocks registered**, no errors.
- Scope: `CustomBlocksConfig` (minimal: maxSlots, httpPort), immutable `SlotData` + `.update()`,
  `SlotBlock`, `SlotManager` (register the pool + BlockItems + a creative tab), wire into
  `CustomBlocksMod` with a clear "Registered N slot blocks" log line.
- Deferred to their own phases: `SlotDataStore` (Phase 2 persistence), `BlockFinder` (Phase 3).
- Expectation: slot blocks render as the **missing-texture (purple/black) cube** until Phase 4.

### Build-verified (NOT yet in-game)
- Implemented `CustomBlocksConfig` (62 lines, atomic save), `SlotData` (immutable record
  + `.update()`), `SlotBlock` + `SlotBlock.SlotItem` (minimal), `SlotManager.registerAll()`,
  and wired `CustomBlocksMod` to register the pool + a "CustomBlocks" creative tab.
- `gradlew build` PASSED — compiles, all gates green. No file exceeds its size limit.
- Recycled the proven 1.21.1 registration API from the old project (`Identifier.of`,
  `Registry.register(Registries.BLOCK/ITEM, …)`, `FabricItemGroup.builder()`).
- **Awaiting developer in-game test** (see docs/TESTING_GUIDE.md §Phase 1): startup log
  should read "Registered 1028 slot blocks", game must not crash, creative tab present.
  Expect many "missing model" warnings + purple blocks — normal until Phase 4.

---

## 2026-06-03 (cont.) — Phase 0: hardening & protocol

### Done
- Wrote `CLAUDE.md` — the operating protocol (golden rule, who the developer is, phase
  discipline, architecture rules, known pitfalls, git/doc protocol, forbidden behaviors).
- Added `verifyFileSize` build gate (§9.3): fails the build if any `.java` exceeds its
  limit (500 general / 400 command handlers / 300 `*Config.java`). package-info exempt.
  This is new — the old project relied on an unenforced rule and grew 9,400-line files.
- Added `.editorconfig` (UTF-8, LF, 4-space) for encoding/format consistency (NFR-11).
- Added GitHub Actions CI (`.github/workflows/build.yml`): JDK 21 + `gradlew build`
  (which runs all three gates) + JAR artifact. Remote is `github.com/3liSY/CustomBlocks`.

### Note on "spot the bugs"
- The new project has no runtime bugs to hunt yet (≈3 code files, builds clean). The real
  bugs live in the old project; the strategy is **prevention** (gates above + the pitfalls
  table in CLAUDE.md §7) and **fix-on-port** as each phase is rebuilt.

### Verified
- `gradlew build` **PASSED** — all three gates run (`verifyFileSize`, `verifyMojibake`,
  `verifySound`).
- `verifyFileSize` proven to actually fire: a throwaway 501-line file made the build
  FAIL with exit 1 ("exceeds class limit of 500"); file then deleted, build green again.
  (A gate that's never seen to fail is false confidence — so it was tested both ways.)

### Next Session
- Phase 1 — Block Slot System (unchanged from the entry below).

---

## 2026-06-03 — Phase 0: Foundation (fresh start)

### Done
- Scaffolded a fresh project in the repo root, recycling the old project's working
  build infrastructure (Fabric Loom 1.7.4, Gradle 8.8 wrapper, MC 1.21.1, Java 21).
- `build.gradle`: minimal Phase-0 dependency set + `verifyMojibake` and `verifySound`
  gates wired into `build`. jcodec / SpotBugs / permissions are commented out, to be
  enabled in their phases (13 / 15 / when needed).
- `fabric.mod.json`, `customblocks.mixins.json` (empty mixin list), minimal `en_us.json`.
- Full package structure laid down as `package-info.java` for all 21 packages.
- `CustomBlocksMod` + `CustomBlocksClient` entrypoints — log a Hello-World line on load.
- Ported `TextSanitizer.java` verbatim (the mojibake repair dictionary).
- Docs: `CHANGELOG.md`, this log, `docs/adr/ADR-001` + template, copied
  `docs/MOJIBAKE_SHIELD.md`, and `LICENSE` / `LICENSE-ar`.

### Decision (flagged to developer)
- Instead of ~90 empty class stubs, the package structure is documented via
  `package-info.java` per package, with real classes added per phase. This matches
  the Bible's own "start minimal, expand per-phase" philosophy. Can switch to full
  stubs if the developer prefers.

### Verified
- `gradlew build` **PASSED** (25s): compileJava, verifyMojibake, and verifySound all
  green; produced `build/libs/customblocks-1.0.0.jar` (2.3 MB, fabric-api bundled).
- Required installing Temurin JDK 21 (machine had only JDK 26, too new for Gradle 8.8)
  and pinning the Gradle daemon to it via `~/.gradle/gradle.properties`.
- **NOT yet verified in-game** — per CLAUDE.md, only a developer in-game test counts
  as DONE. The build passing proves it compiles, not that it works in Minecraft.
  Next: run `gradlew runClient` and confirm the "[CustomBlocks] Hello World — mod
  loaded successfully." line appears in the log with no errors.

### Next Session
- Phase 1 — Block Slot System: `SlotBlock`, immutable `SlotData` + `.update()`,
  `SlotManager` (register 1028 blocks), `SlotDataStore`, `BlockFinder`, minimal
  `CustomBlocksConfig` (httpPort, maxSlots). Milestone: server starts with 1028
  blocks in the registry, no errors.
