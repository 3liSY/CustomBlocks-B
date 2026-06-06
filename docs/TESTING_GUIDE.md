# 🧪 CustomBlocks — Testing Guide

> Living document. Each phase adds its own test checklist. **Only test what's been
> built.** Per the golden rule (CLAUDE.md §2), a feature is DONE only after you test it
> in-game and confirm it works — a green build is not enough.

**Status legend:** ✅ pass · ❌ fail · ⏳ not built yet · ⬜ not tested

---

## 0. How to run the mod for testing

All commands run from the project root in PowerShell (`c:\Users\POTATO\Desktop\Code\CustomBlocks`).

| Command | What it does |
|---|---|
| `.\gradlew.bat build` | Compiles + runs all 3 gates. Should end with **BUILD SUCCESSFUL**. |
| `.\gradlew.bat runClient` | Launches a **dev Minecraft client** with the mod loaded. |
| `.\gradlew.bat runServer` | Launches a **dev dedicated server** (optional; needs EULA accept). |

**Where to read the result:** log lines appear in the **terminal** where you ran the
command, and are also saved to **`run\logs\latest.log`**. To check, open that file and
press **Ctrl+F** for `[CustomBlocks]`.

**If something fails:** copy the red error text (or the last ~30 lines of the terminal)
and send it to me. Don't try to fix it yourself.

---

## 1. PHASE 0 — Foundation  ← **TEST THIS NOW**

**Goal:** the mod loads in Minecraft 1.21.1 with no errors and prints its startup lines.
There are **no blocks, items, or commands yet** — that is expected and correct.

### Test 0.1 — Build passes
1. Run `.\gradlew.bat build`
2. **Expect:** ends with `BUILD SUCCESSFUL`, and you see `verifyFileSize`,
   `verifyMojibake`, `verifySound` in the task list with no FAILED.
- Result: ⬜

### Test 0.2 — Client launches with mod loaded  ⭐ the important one
1. Run `.\gradlew.bat runClient` (first launch downloads assets — give it a minute).
2. **Expect:** the Minecraft title screen appears, no crash.
3. In the terminal (or `run\logs\latest.log`, Ctrl+F `[CustomBlocks]`) you should see **all three**:
   - `[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 0 — foundation)`
   - `[CustomBlocks] Hello World — mod loaded successfully.`
   - `[CustomBlocks] Client initializing (Phase 0 — foundation).`
- Result: ⬜

### Test 0.3 — Nothing unexpected registered (sanity)
1. From the title screen → **Singleplayer** → create a Creative world.
2. Open the creative inventory / search.
3. **Expect:** **no** CustomBlocks blocks or items exist yet. (Correct for Phase 0.)
   If the game runs and you can move around with no crash, that's a pass.
- Result: ⬜

### Test 0.4 — Clean shutdown
1. Quit the game normally (or close the window).
2. **Expect:** no crash report, terminal returns to a prompt.
- Result: ⬜

### Phase 0 verdict
- [x] Confirmed working in-game (2026-06-03). ✅

---

## 1b. PHASE 1 — Block Slot System  ← **TEST THIS NOW**

**Goal:** the game starts with **1028 blocks registered** and does not crash.

> ⚠️ **Expected and normal in Phase 1:** the slot blocks have **no textures yet**, so
> they render as the **purple/black "missing texture" cube**, and the log will show
> **many "missing model" / "missing texture" warnings** for `slot_0`…`slot_1027`.
> These are *warnings, not errors*. Real textures arrive in **Phase 4**. Don't worry.

### Test 1.1 — Registration log line  ⭐ the important one
1. Run `.\gradlew.bat runClient`.
2. In the terminal (or `run\logs\latest.log`, Ctrl+F `[CustomBlocks]`) look for:
   - `[CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 1 — block slot system)`
   - `[CustomBlocks] Config loaded: maxSlots=1028, httpPort=8123`  *(or "Created default config")*
   - `[CustomBlocks] Registered 1028 slot blocks (slot_0 to slot_1027).`
3. **Expect:** game reaches the title screen, **no crash**.
- Result: ⬜

### Test 1.2 — Config file was created
1. Check that the file **`run\config\customblocks\config.json`** now exists.
2. Open it — it should contain `"maxSlots": 1028` and `"httpPort": 8123`.
- Result: ⬜

### Test 1.3 — Creative tab exists with the slot blocks
1. Singleplayer → create a **Creative** world.
2. Open the creative inventory and find the **"CustomBlocks"** tab (bookshelf icon).
3. **Expect:** the tab is full of slot items (they'll look untextured — that's fine).
- Result: ⬜

### Test 1.4 — A slot block can be placed and broken
1. Grab any slot item, place it in the world (it'll be a purple/black cube).
2. Break it. **Expect:** places and breaks with no crash.
- Result: ⬜

### Phase 1 verdict
- [x] Confirmed working in-game (2026-06-03). ✅ (Now defaults to **800** slots.)

---

## 1c. BATCH 2 — Manage blocks (create / persist / commands)  ← **TEST THIS NOW**

**Goal:** create, name, duplicate, give, and delete custom blocks with `/cb`, and have
them **survive a restart**. Blocks are still untextured (purple) — textures are Batch 3.

> Setup: launch with `.\gradlew.bat runClient`, make a **Creative** world with **cheats ON**
> (so commands work). The startup log should now say `Registered 800 slot blocks`.

### Test 2.1 — Create + list + give + place
1. `/cb create test1` → expect `§aCreated test1 (slot 0)…`.
2. `/cb list` → expect it shows `test1 (slot 0, "test1")`.
3. `/cb give test1` → a block item appears in your inventory; **place it** (purple cube,
   named "test1" on hover).
- Result: ⬜

### Test 2.2 — Rename + duplicate + delete
1. `/cb rename test1 Cool Block` → hover the placed/held item, name updates to "Cool Block".
2. `/cb dupe test1 test2` → expect `Duplicated test1 → test2 (slot 1)`.
3. `/cb list` → shows **both** test1 and test2.
4. `/cb delete test2` → then `/cb list` shows only test1.
- Result: ⬜

### Test 2.3 — Persistence across restart  ⭐ the important one
1. With `test1` still created, **fully close Minecraft** (not just leave the world).
2. Confirm the file **`run\config\customblocks\slots.json`** exists and contains `test1`.
3. Relaunch `.\gradlew.bat runClient`, re-enter a world, run `/cb list`.
4. **Expect:** `test1` is **still there**.
- Result: ⬜

### Test 2.4 — Reload
1. `/cb reload` → expect `Reloaded N block(s) from disk.` with no crash.
- Result: ⬜

### Test 2.5 — Clickable export (added after Batch 2)
1. With at least one block created, run `/cb list`.
2. At the bottom, click the green **[.json]** (or **[.txt]**) button.
3. **Expect:** a message `Exported N block(s) → …\blocks-<timestamp>.json [copy path]`.
4. Click **[copy path]**, paste into File Explorer → the export file opens / exists with your blocks.
   (Or run `/cb export txt` directly.)
- Result: ⬜

### Batch 2 verdict
- [x] Core confirmed working in-game (2026-06-03). ✅  ·  Export (2.5): ⬜ quick-check next launch.
- Next: **Batch 3 (textures 🎨)**.

---

## 1d. BATCH 3 — Textures 🎨 (Phase 4)  ← **TEST THIS NOW**

**Goal:** paste an image URL and the block shows that image — for everyone — and it
survives a restart.

> Setup: `.\gradlew.bat runClient`, Creative world, **cheats ON**. You'll need a **direct
> image link** ending in `.png` or `.jpg` (PNG/JPG/GIF; **WebP not supported yet**). Tip:
> right-click an image in your browser → **Copy Image Address**.

### Test 3.1 — HTTP server starts
1. After the world loads, check the log for:
   `[CustomBlocks] Resource-pack HTTP server live on port 8123` (or a fallback port).
- Result: ⬜

### Test 3.2 — Texture a block  ⭐ the main event
Two ways — both should work:

**One-shot (old format you're used to):**
1. `/cb create cat Cat https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png`
   - `cat` = id, `Cat` = display name, then the URL. Multi-word names need quotes: `"My Cat"`.
2. You should see `Created cat (slot 0).` then `Downloading texture for cat...` then `Textured cat!`.

**Two-step (retexture an existing block):**
1. `/cb create test1`
2. `/cb retexture test1 <paste a direct .png/.jpg URL>` — press **Tab** after `retexture ` and it lists your block ids.

Then for either:
3. A **resource-pack prompt** appears ("CustomBlocks textures") → click **Yes / Proceed / Download**.
4. `/cb give cat` (or `test1`), then **place the block**.
5. **Expect:** the block (and its item) now show your image. 🎉
- Result: ⬜

### Test 3.2b — Bad URL is handled cleanly
1. `/cb create bad Bad data:image/png;base64,AAAA` (a `data:` link, not a web link).
2. **Expect:** a clear error `That URL isn't a web link…` and **no** block named `bad` is created
   (`/cb list` does not show it). No cryptic "invalid URI scheme" message.
- Result: ⬜

### Test 3.3 — Everyone-ready / persistence
1. Break and replace it — texture stays.
2. **Fully close Minecraft**, relaunch, re-enter the world, `/cb give test1`, place it.
3. **Expect:** the texture is **still there** after restart.
- Result: ⬜

### Expected rough edges (NOT bugs)
- You get a **pack prompt each time you retexture** — that's the vanilla mechanism; just
  click Yes. (Auto-apply is a later polish.)
- **Multiplayer only:** other players see the texture, but the block's hover-name and the
  creative tab contents need client sync (Phase 5). Singleplayer is fully fine.
- A bad/blocked URL or a WebP link gives a clear error — try a direct `.png`/`.jpg`.

### Batch 3 verdict
- [ ] Block shows your image + survives restart → **tell me "Batch 3 works"** and I'll
  commit it to `dev`, then we move to **Batch 4 (attributes & tools)**.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1e. BATCH 4 — Attributes (Phase 6, slice 1: glow 💡)  ← **TEST THIS NOW**

**Goal:** make a custom block emit light (0–15), persist it, and have it light the world.

> Setup: `.\gradlew.bat runClient`, Creative world, **cheats ON**. Make sure it's **night**
> or a dark area so glow is visible (`/time set night`).

### Test 4.1 — Set glow + place
1. `/cb create lamp Lamp` (textured or not — glow works either way).
2. `/cb setglow lamp 15` → expect `Set glow of lamp to 15…`. Press **Tab** after `setglow `
   to confirm `lamp` is suggested.
3. `/cb give lamp`, **place the block** in a dark spot.
4. **Expect:** the block lights up the area like a torch/glowstone (level 15).
- Result: ⬜

### Test 4.2 — Level 0 = no light
1. `/cb setglow lamp 0`, break and re-place the block.
2. **Expect:** it no longer emits light.
- Result: ⬜

### Test 4.3 — Glow survives a restart
1. `/cb setglow lamp 12`, confirm `run\config\customblocks\slots.json` shows `"glow": 12`.
2. **Fully close Minecraft**, relaunch, re-enter, `/cb give lamp`, place it.
3. **Expect:** it still glows after restart.
- Result: ⬜

### Test 4.4 — Change glow on an already-placed block
1. Place `lamp`, then run `/cb setglow lamp 7`.
2. **Expect:** the block already in the world updates its light **immediately** (you don't
   have to break/replace it), as long as you're within a few chunks of it.
- Result: ⬜

### Test 4.5 — Glow caps at 15 (no error)
1. `/cb setglow lamp 50`.
2. **Expect:** a green message `Set glow of lamp to 15. (15 is Minecraft's brightest — capped)`
   — NOT a red error. (15 is the engine's hard limit; nothing can emit more.)
- Result: ⬜

### Test 4.6 — Hardness 💪 (`/cb sethardness`)  — test in **Survival**
> ⚠️ In **Creative** every positive-hardness block breaks **instantly**, so 1, 20, 50 all
> feel identical there. To feel the difference you must be in **Survival** (`/gamemode survival`).
> Only `unbreakable` (-1) is visibly different in Creative.

1. `/cb create rock Rock` then `/cb give rock`, place a few. Switch to Survival.
2. `/cb sethardness rock unbreakable` → break one: **can't mine it**. (Words work now — press
   Tab after `sethardness rock ` to see `unbreakable / instant / stone / 5 / 20 / 50`.)
3. `/cb sethardness rock instant` → **instant** break (like grass).
4. `/cb sethardness rock 50` → **very slow** to mine.
5. `/cb sethardness rock stone` (or `1.5`) → back to normal stone speed.
6. Confirm the message no longer calls 50 "vanilla stone".
- Result: ⬜

### Test 4.8 — Custom sound 🔊 (`/cb setsound`)
> ⚠️ **Relaunch a fresh `runClient` first** — new block code (the sound override) only loads on
> a fresh launch, not a hot-swap. Use a **distinct** sound like `metal` or `glass` (not `dirt`).

1. `/cb setsound rock metal` → press Tab after `setsound rock ` to see the sound list.
2. **Decisive check:** open `run\config\customblocks\slots.json` — the `rock` entry should now
   show `"sound": "metal"`. (This proves the command + save path.)
3. Place and break `rock`, and walk on it → it should **sound like metal**.
4. An invalid name like `/cb setsound rock banana` → clear error listing valid options.
- Result: ⬜
- If step 2 shows `"sound":"metal"` but step 3 still sounds like stone → tell me (it's a
  client-refresh issue, not the data). If step 2 does **not** show it → also tell me (save bug).

### Test 4.7 — Hardness survives restart
1. `/cb sethardness rock -1`, confirm `run\config\customblocks\slots.json` shows `"hardness": -1.0`.
2. Restart, `/cb give rock`, place it → still unbreakable.
- Result: ⬜

### Expected behavior (NOT a bug)
- Glow lives in the block's **state**, so each placed block remembers its own light and it
  survives world reloads. `setglow` updates placed copies that are **loaded near a player**;
  copies in unloaded/faraway chunks keep their old light until you revisit them (then a new
  placement or a `setglow` while nearby refreshes them).
- Hardness is read **live** every break attempt, so `sethardness` affects all placed copies
  (loaded or not) immediately — no need to replace them.

### Batch 4 (attributes) verdict
- [ ] Glow + glow-cap + hardness all work → **tell me "works"** and I'll commit, then
  continue Batch 4 (custom sounds, then undo/redo and tools).
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1f. BATCH 5+6 — Undo/Redo ↩️ + Collision 🚶 (Phase 6)  ← **TEST THIS NOW**

All `/cb undo` and `/cb redo` tests must be run **as a player** (not the server console).

### Test 6.1 — Undo a create
1. `/cb create ghosttest Ghost`
2. `/cb undo` → message `[CB] Undid create ghosttest`.
3. `/cb list` → `ghosttest` is gone.
- Result: ⬜

### Test 6.2 — Redo brings it back
1. After 6.1, `/cb redo` → `[CB] Redid create ghosttest`.
2. `/cb list` → `ghosttest` is back.
- Result: ⬜

### Test 6.3 — Undo an attribute change
1. `/cb setglow ghosttest 15`, then `/cb setglow ghosttest 0`.
2. `/cb undo` → glow goes back to 15 (place the block to confirm it's bright again).
3. `/cb undo` again → glow back to 0.
- Result: ⬜

### Test 6.4 — Undo a delete restores the texture
1. Texture a block (`/cb create pic Pic https://…`), confirm it shows the image.
2. `/cb delete pic`, then `/cb undo`.
3. `/cb give pic`, place it → the **image texture is back** (accept the pack prompt if asked).
- Result: ⬜

### Test 6.5 — Per-player isolation ⭐ the important one (needs 2 players, or skip)
1. Player A: `/cb create a_block`. Player B: `/cb create b_block`.
2. Player B: `/cb undo` → removes **b_block** only; `a_block` is untouched.
- Result: ⬜  (⏳ if you can't get a 2nd player — singleplayer still proves the rest)

### Test 6.6 — Collision: passable = walk-through
1. `/cb create wall Wall`, `/cb give wall`, place a block at head height in a doorway.
2. `/cb setcollision wall passable` → walk into it: you pass **through** it.
3. You can still **break** it (outline is intact) and still **target** it.
4. `/cb setcollision wall solid` → it blocks you again.
- Result: ⬜

### Test 6.7 — Collision survives restart + is undoable
1. `/cb setcollision wall passable`; check `slots.json` shows `"noCollision": true`.
2. Restart → still passable. `/cb undo` (as the same player) → back to solid.
- Result: ⬜

### Batch 5+6 verdict
- [ ] Undo/redo + collision work → **tell me "works"** and I'll commit, then start Phase 7 (the 7 tools).
- [ ] Something broke → paste the command + the error / last log lines.

> **Known limit (not a bug):** retexture is not undoable yet (later slice). In **multiplayer**,
> collision/hardness/sound are read from server state; a vanilla client predicts movement
> locally, so passable blocks are fully reliable in singleplayer — multiplayer client sync is
> a later networking slice.

---

## 1g. BATCH 7 — Tools 🛠️ (Phase 7: Lumina Brush, Chisel, Deleter)  ← **TEST THIS NOW**

Open the creative inventory → there's a new **"CustomBlocks Tools"** tab with 3 items
(glowstone-dust brush, amethyst chisel, red-barrier deleter). Grab them and a custom block.

### Test 7.1 — Lumina Brush cycles glow
1. Place a custom block. Hold the **Lumina Brush**, right-click the block repeatedly.
2. **Expect:** action-bar `[CB] <id> glow → 4 / 8 / 12 / 15 / 0 …` and the block visibly
   brightens each click. **Sneak + right-click** steps it back down.
- Result: ⬜

### Test 7.2 — Chisel cycles hardness
1. Hold the **Chisel**, right-click a custom block a few times.
2. **Expect:** action-bar `[CB] <id> hardness → soft / stone / hard / tough / unbreakable / instant`.
3. Confirm it took effect: in **Survival**, an "unbreakable" block can't be mined; "instant" breaks at once.
- Result: ⬜

### Test 7.3 — Deleter removes placed blocks (even unbreakable)  ⭐ (re-test after fix)
1. Chisel a block to **unbreakable**, place a few. Hold the **Deleter**, right-click them.
2. **Expect:** each **actually disappears** (with break particles, no drop) and shows a
   **readable aqua** `[CB] Removed <id>` on the hotbar. If you right-click empty air/nothing,
   you get **no** "Removed" message.
3. `/cb give <id>` still works — only the placed blocks were removed, not the definition.
- Result: ⬜

> **Note:** the **Lumina Brush** + **Chisel** are being merged into one **GUI "Block Editor"**
> in Phase 10 (your call). For now they still work by click-cycling (7.1–7.2); their icons are
> placeholders until then. No need to judge them yet.

### Test 7.4 — Tools feel instant (no delay)
1. Rapidly right-click with each tool. **Expect:** the arm swings and the effect happens with
   no noticeable lag (the old client-skip bug is avoided by design).
- Result: ⬜

### Test 7.5 — Tool edits are undoable
1. Lumina-Brush a block up to glow 15, then `/cb undo` → glow steps back.
2. Chisel a block to unbreakable, then `/cb undo` → hardness reverts.
- Result: ⬜  (Deleter's block-removal is not slot-undo; that's `/cb`-level history only.)

### Batch 7 (tools) verdict
- [ ] All 3 tools work + feel instant → **tell me "works"** and I'll commit, then start Phase 8
  (color ecosystem), which unlocks the remaining color tools.
- [ ] Something broke → paste what you did + the error / last log lines.

---

## 1h. BATCH 8 — Categories 🗂️ + Search 🔎 (Phases 8 + 9)  ← **TEST THIS NOW**

### Test 8.1 — Tag a block with a category
1. `/cb create rock Rock`, `/cb create grass Grass`.
2. `/cb setcategory rock nature`, `/cb setcategory grass nature`.
3. **Expect:** `[CB] Category rock → nature ✔`.
4. `/cb list` → each line shows its `[nature]` tag.
- Result: ⬜

### Test 8.2 — List categories
1. `/cb categories` → shows `nature (2)` with a clickable **[list]** button.
2. Click **[list]** (or run `/cb search nature`) → both blocks appear.
- Result: ⬜

### Test 8.3 — Search by id / name / category
1. `/cb search roc` → finds `rock` (id match).
2. `/cb search nature` → finds both (category match).
3. Each hit has a clickable **[give]** button that puts the block in your hand.
- Result: ⬜

### Test 8.4 — Clear a category + persistence
1. `/cb setcategory rock none` → `[CB] rock → uncategorized ✔`; `/cb list` shows no tag on rock.
2. Check `run\config\customblocks\slots.json`: `grass` has `"category": "nature"`, `rock` has none.
3. Restart → categories survive. `/cb undo` (as the player who set it) reverts a category change.
- Result: ⬜

### Batch 8 (categories + search) verdict
- [ ] Categories + search work → **tell me "works"** and I'll commit Batches 3–8 to `dev`.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1i. BATCH 9 — Undo scope toggle (`/cb config`)  ← **TEST THIS NOW**

### Test 9.1 — Default is server-wide
1. `/cb config` → shows `Undo mode: server-wide`.
- Result: ⬜

### Test 9.2 — Cycle the mode
1. `/cb config undomode` → `[CB] Undo mode → per-player (history cleared) ✔`.
2. `/cb config undomode` again → back to `server-wide`.
3. `/cb config undomode perplayer` and `/cb config undomode serverwide` set it directly.
- Result: ⬜

### Test 9.3 — Server-wide actually shares (needs 2 players, or skip)
1. Set `/cb config undomode serverwide`. Player A: `/cb create shared_a`.
2. Player B: `/cb undo` → removes `shared_a` (server-wide history is shared).
3. Switch to `perplayer`: now B's `/cb undo` only affects B's own edits.
- Result: ⬜  (⏳ if solo — 9.1/9.2 still prove the toggle + persistence)

### Test 9.4 — Survives restart
1. Set a mode, restart, `/cb config` → it's still the mode you set (saved in `config.json`).
- Result: ⬜

### Batch 9 verdict
- [ ] Deleter fix + readable HUD text + `/cb config undomode` all work → **tell me "works"**
  and I'll commit Batches 3–9 to `dev`, then continue phases in order.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1j. BATCH 10 — Phase 9: per-block export, importfolder, templates  ← **TEST THIS NOW**

> Setup: `.\gradlew.bat runClient`, Creative world, cheats ON.
> Have a couple of blocks created from Batch 2 tests (or create fresh ones).

### Test 10.1 — Per-block export
1. `/cb create stone Stone` (or use an existing block).
2. `/cb export stone` → expect the export menu: `[to Config] [to Vault] [Download]` buttons.
3. Click `[to Config]` → expect `Saved stone → exports/stone.json` (no file path, no copy button).
4. Open `run\config\customblocks\exports\stone.json` — verify it has `"schema": 1`, `"id": "stone"`,
   `"displayName"`, `"glow"`, `"hardness"`, `"soundType"`.
- Result: ⬜

### Test 10.2 — importfolder round-trip
1. Export a block: `/cb export stone` (creates `exports/stone.json`).
2. Delete the block: `/cb delete stone` → confirm gone from `/cb list`.
3. `/cb importfolder` → expect `Imported 1 block(s): stone`.
4. `/cb list` → `stone` is back with all its attributes.
- Result: ⬜

### Test 10.3 — importfolder skips duplicates
1. Ensure `stone.json` is in the exports folder and `stone` still exists.
2. `/cb importfolder` → expect `Skipped 1 (already exist): stone` (no duplicate, no crash).
- Result: ⬜

### Test 10.4 — importfolder with custom path
1. Copy `stone.json` somewhere else (e.g., `run\` folder).
2. `/cb importfolder run` → imports from that directory instead.
- Result: ⬜

### Test 10.5 — Template save + apply
1. Create two blocks: `/cb create base Base`, `/cb create copy Copy`.
2. Tune `base`: `/cb setglow base 8`, `/cb sethardness base 5`, `/cb setcategory base nature`.
3. `/cb template save tough_nature base` → `Saved template "tough_nature" from base`.
4. `/cb template list` → shows `tough_nature [apply]`.
5. `/cb template apply tough_nature copy` → `Applied template "tough_nature" → copy`.
6. Check `copy` inherited glow 8, hardness 5, category nature (via `/cb list` or `/cb search nature`).
- Result: ⬜

### Test 10.6 — Template is undoable
1. After Test 10.5, run `/cb undo` → `copy` should revert to its original glow/hardness/category.
- Result: ⬜

### Test 10.7 — Template survives restart
1. With `tough_nature` saved, fully close + relaunch Minecraft.
2. `/cb template list` → `tough_nature` still there.
- Result: ⬜

### Batch 10 verdict
- [ ] All export/import/template operations work → continue to Batch 11 tests below.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1k. BATCH 11 — UX polish: Deleter visual, export menu, template overhaul

> Setup: same world from Batch 10. Have a couple of blocks created and at least one placed.

### Test 11.1 — Deleter shows purple/black placeholder (not transparent)
1. Place a custom block in the world.
2. Hold the Deleter tool, right-click the placed block.
3. **Expect:** the block stays in the world but changes to a **purple/black checkerboard**
   (Minecraft's "missing texture" pattern) — NOT invisible or transparent like before.
4. `/cb undo` → block definition is restored, texture reappears.
- Result: ⬜

### Test 11.2 — `/cb export` shows action menu
1. `/cb export` (no arguments) → expect a line with `[.json] [.txt] [to Vault]` buttons and
   a hint `Per-block: /cb export <id>`.
2. Click `[to Vault]` → expect "Vault sync is coming in Phase 14. Stay tuned!" (stub).
3. Click `[.json]` → expect "Exported X block(s) → blocks-*.json [to Vault]" — no file path, no copy button.
- Result: ⬜

### Test 11.3 — Per-block export menu
1. `/cb export stone` → expect: `[to Config] [to Vault] [Download]` buttons.
2. Click `[to Config]` → expect `Saved stone → exports/stone.json`.
3. Click `[to Vault]` → expect Phase 14 stub message.
4. Click `[Download]` → expect a clickable `[open link]` that opens `http://localhost:8123/export/stone`
   in your browser, downloading `stone.json`.
- Result: ⬜

### Test 11.4 — Template list shows attributes
1. `/cb template save mypreset stone` (assumes `stone` has some attributes set).
2. `/cb template list` → expect the line shows attributes like `glow:X hard:Y stone` inline,
   plus `[apply →]` and `[x]` buttons.
- Result: ⬜

### Test 11.5 — Template save/apply confirmations show attributes
1. `/cb template save glowy stone` (with `stone` having glow > 0) →
   confirm message shows the glow/hardness/sound values, e.g. `(glow:8 hard:1.5 stone)`.
2. `/cb template apply glowy copy` → confirm message shows the applied attribute set.
- Result: ⬜

### Test 11.6 — Template delete
1. `/cb template delete glowy` → expect `Deleted template "glowy"`.
2. `/cb template list` → `glowy` no longer appears.
3. Press Tab after `/cb template delete ` → only surviving templates are suggested.
- Result: ⬜

### Test 11.7 — Template empty-list explanation
1. Delete all templates so none remain.
2. `/cb template list` → expect a multi-line explanation of what templates are and how to create one.
- Result: ⬜

### Batch 11 verdict
- [ ] All Batch 11 tests pass → continue to Batch 12 tests below.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1l. BATCH 12 — Phase 9 completion: Lock, Notes, Favorites, Drafts

> Setup: same world. Have at least two custom blocks created (`/cb create stone`, `/cb create heart`).

### Test 12.1 — Lock prevents modification
1. `/cb lock stone` → expect success + "Locked stone"
2. Try `/cb rename stone Stone2` → expect error "stone is locked"
3. Try `/cb setglow stone 8` → expect error "stone is locked"
4. Try `/cb delete stone` → expect error "stone is locked"
5. Hold Deleter tool, right-click the placed stone block → expect error on HUD "stone is locked"
6. `/cb unlock stone` → success. Then `/cb rename stone Stone2` → works.
- Result: ⬜

### Test 12.2 — `/cb locked` list
1. Lock two blocks: `/cb lock heart`, `/cb lock stone`
2. `/cb locked` → shows both with `[unlock]` buttons
3. Click `[unlock]` on one → it disappears from the list on next `/cb locked`
- Result: ⬜

### Test 12.3 — Notes: set, show, clear
1. `/cb note stone` → "No note for stone" + hint
2. `/cb note stone This is my main block` → success (multi-word, no quotes)
3. `/cb note stone` → shows "This is my main block"
4. `/cb note stone clear` → success
5. `/cb note stone` → back to "No note"
- Result: ⬜

### Test 12.4 — Note cleaned up on block deletion
1. `/cb note heart My heart block` → set note
2. `/cb delete heart` → deletes block
3. `/cb create heart Heart` → recreate
4. `/cb note heart` → "No note for heart" (old note was cleaned up)
- Result: ⬜

### Test 12.5 — Favorites toggle and list
1. `/cb fav stone` → "Added stone to favorites ★"
2. `/cb fav stone` → "Removed stone from favorites"
3. `/cb fav stone` and `/cb fav heart` → add both
4. `/cb favs` → lists both with `[give]` and `[unfav]` buttons
5. Click `[give]` → receives the block item
6. Click `[unfav]` → removes from list
- Result: ⬜

### Test 12.6 — Draft mark and publish
1. `/cb draft stone` → "stone marked as draft"
2. `/cb drafts` → shows stone with `[publish]` button
3. `/cb list` → stone shows `[draft]` tag
4. Click `[publish]` or `/cb publish stone` → success
5. `/cb list` → `[draft]` tag gone
6. `/cb draft stone` again, then `/cb delete stone` → block deleted, check `/cb drafts` → stone no longer listed
- Result: ⬜

### Test 12.7 — Status tags in `/cb list`
1. Lock `heart`, mark `stone` as draft
2. `/cb list` → heart shows `[locked]` in red, stone shows `[draft]` in grey
3. `/cb search stone` → also shows `[draft]` tag
- Result: ⬜

### Batch 12 verdict
- [ ] All Batch 12 tests pass → **tell me "works"** and I'll commit Batches 3–12 to `dev`.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 1n. BATCH 13 — Macros, Arabic, GUI, HUD, Diagnostics, Cloud/Discord, Onboarding

> Setup: `.\gradlew.bat runClient`, Creative world, cheats ON.
> Build must show **BUILD SUCCESSFUL** before starting these tests.

---

### Test 13.1 — Macro: record and play

1. Start recording: `/cb macro record mymacro` → expect `[CB] Recording macro "mymacro". Use /cb macro add <cmd> to add steps.`
2. Add two steps:
   - `/cb macro add /cb create tmp1 Temp1`
   - `/cb macro add /cb create tmp2 Temp2`
3. Stop: `/cb macro stop` → expect `[CB] Saved macro "mymacro" (2 steps).`
4. Delete the blocks if they got created: `/cb delete tmp1`, `/cb delete tmp2`.
5. Play: `/cb macro play mymacro` → expect both `tmp1` and `tmp2` to be created. `/cb list` confirms them.
6. `/cb macro list` → shows `mymacro` (2 steps) with a `[play]` button.
7. `/cb macro delete mymacro` → expect `[CB] Deleted macro "mymacro".`; `/cb macro list` shows nothing.
- Result: ⬜

### Test 13.2 — Macro: cancel mid-recording

1. `/cb macro record canceltest` → start.
2. `/cb macro add /cb create willnotexist Ghost`
3. `/cb macro cancel` → expect `[CB] Recording cancelled.`
4. Check `/cb list` confirms no `willnotexist` block exists.
- Result: ⬜

### Test 13.3 — Arabic: import all letters

> Note: placing `arabtype.ttf` in `run\config\customblocks\` gives best results; the system
> `SansSerif` fallback still works but may produce less accurate Arabic shaping.

1. `/cb arabic import` → expect a result like `[CB] Arabic import done: 28 created, 0 skipped, 0 failed.`
2. Open the Creative inventory → **CustomBlocks** tab. You should see 28 new blocks named
   `arabic_alef`, `arabic_ba`, … `arabic_ya`.
3. Accept the resource pack prompt if it appears.
4. Place a few of the letter blocks — each should show the Arabic letter as a texture image.
- Result: ⬜

### Test 13.4 — Arabic: import single letter

1. `/cb arabic letter alef` → expect `[CB] Created arabic_alef` (or `already exists` if imported above).
- Result: ⬜

### Test 13.5 — Arabic: render a word block

1. `/cb arabic word hello Hello مرحبا` → expect `[CB] Created arabic word block "hello".`
2. `/cb give hello`, place it → the block should show the word as a texture.
- Result: ⬜

### Test 13.6 — GUI: main menu opens

1. `/cb gui` → expect the **CustomBlocks Main Menu** screen to open in-game
   (dark overlay with buttons: Block List, Categories, Templates, Macros, Arabic Letters, Diagnostics, Close).
2. Click **Close** → screen closes with no crash.
- Result: ⬜

### Test 13.7 — GUI: block editor opens

1. Make sure you have a block created (e.g. `/cb create testblock Test`).
2. `/cb gui block testblock` → expect the **Block Editor** screen to open with `testblock` as the title.
3. Click **Set Glow** → the ChatScreen should open pre-filled with `/cb setglow testblock `.
4. Click **← Back** → returns to previous screen or closes.
- Result: ⬜

### Test 13.8 — GUI: config screen

1. `/cb gui config` → expect the **Config** screen to open showing live values:
   `undoMode`, `hudEnabled`, `textureSize`, `maxUndoDepth`, `httpHost`.
2. Click a button (e.g. **Toggle HUD**) → value flips; no crash.
- Result: ⬜

### Test 13.9 — HUD: shows block info

1. Create and place a custom block: `/cb create hud1 "My HUD Block"`, `/cb give hud1`, place it.
2. Look directly at the placed block with your crosshair.
3. **Expect:** a small overlay in the top-left corner shows `hud1 "My HUD Block"`.
4. Look away from the block → the overlay disappears.
- Result: ⬜

### Test 13.10 — HUD: toggle

1. `/cb edithud` → expect `[CB] HUD disabled.` and the overlay is gone when looking at the block.
2. `/cb edithud` again → `[CB] HUD enabled.` and the overlay reappears.
- Result: ⬜

### Test 13.11 — Diagnostics: `/cb diag`

1. `/cb diag` → expect a multi-line snapshot including at least:
   `Slots: X / Y`, `Heap: Z MB`, `Players online: N`.
2. No crash. Values look reasonable.
- Result: ⬜

### Test 13.12 — Diagnostics: incidents log

1. `/cb incidents` → `[CB] No incidents recorded.` (unless something crashed earlier).
2. `/cb incidents clear` → `[CB] Incident log cleared.`
- Result: ⬜

### Test 13.13 — Cloud/Discord stubs

1. `/cb vault upload testblock` → expect a stub message:
   `Vault sync is coming in Phase 14. Set vaultEndpoint in config to enable.`
2. `/cb vault download ABCD` → same stub message.
3. `/cb discord test` → expect `Discord webhook not configured. Set discordWebhookUrl in config.`
4. `/cb discord status` → shows current `discordWebhookUrl` (empty).
- Result: ⬜

### Test 13.14 — Onboarding: first join welcome

1. After joining a world, check `run\config\customblocks\players.json` — your UUID should be listed.
2. Re-join (close and reopen the world) → **no** second welcome message fires.
3. If you can log in with a second account/alt, it should receive the welcome message on first join.
- Result: ⬜

### Test 13.15 — AI stubs

1. `/cb ai parse make a glowing red block` → expect AI is disabled until `aiApiKey` is set.
2. `/cb ai texture red glowing rock` → stub message referencing `aiTextureEnabled`.
- Result: ⬜

### Batch 13 verdict
- [ ] All Batch 13 tests pass → **tell me "Batch 13 works"** and I'll commit Batches 3–13 to `dev`.
- [ ] Something broke → paste the command + the error / last log lines.

---

## 2. Optional — confirm the safety gates really work

You can trust these (I tested them), but if you want to see them yourself:

- **File-size gate:** temporarily nothing to do — it's proven to fail a >500-line file
  and pass otherwise. Leave it; it guards every build automatically.
- **Mojibake gate:** if a file ever gets corrupted text, `.\gradlew.bat build` fails with
  a `MOJIBAKE DETECTED` banner pointing at the file+line. See `docs\MOJIBAKE_SHIELD.md`.

---

## 3. Future phases — tests activate as we build them

Derived from the Engineering Bible §8 milestones. **All ⏳ until that phase is built** —
do not test these yet; there's nothing there.

| Phase | What you'll test | Status |
|---|---|---|
| 1 — Block Slot System | Server/world starts with **1028 blocks** registered, no registry errors. | build ✅ · in-game ⬜ (see §1b) |
| 2 — Persistence | Create a block → restart → still there. (Backups/snapshots come later.) | build ✅ · in-game ⬜ (§1c) |
| 3 — Core Commands | create/delete/list/rename/dupe/give in Batch 2; undo/redo → Batch 4, retexture → Batch 3. | partial (§1c) |
| 4 — Texture Pipeline | `/cb retexture <id> <url>` changes the block's look for everyone (HTTP pack). | build ✅ · in-game ⬜ (see §1d) |
| 5 — Client Architecture | Optional now (single HTTP-pack path covers all clients). Adds multiplayer name/tab sync + auto-apply + keep-alive. | deferred (optional) |
| 6 — Undo/Redo + Attributes | Player A's undo doesn't touch Player B's blocks. Shapes & custom sounds work. | glow ✅ · hardness+sounds: build ✅ · in-game ⬜ (§1e) · undo/redo ⏳ |
| 7 — Tools / Items | All **7 tools** work on custom blocks, with **instant** response (no delay). | ⏳ |
| 8 — Color + Categories | Blocks auto-categorize, color detection works, **batch recolor doesn't destroy designs**. | ⏳ |
| 9 — Import/Export + Search | Export → delete → import = **identical** block. Search is instant. Templates work. | ⏳ |
| 10 — GUI | Full create/edit workflow via menus, no typing. **No GUI file over 500 lines.** | build ✅ · in-game ⬜ (§1n 13.6–13.8) |
| 11 — HUD | `/cb edithud` toggles overlay; HUD shows block id+name when looking at a custom block. | build ✅ · in-game ⬜ (§1n 13.9–13.10) |
| 12 — Arabic | Import 28 letters + word blocks; font fallback chain; RTL shaping. | build ✅ · in-game ⬜ (§1n 13.3–13.5) |
| 13 — AI + Video + Macros | AI parses commands (stub); macros record & replay; video (jcodec dep pending). | build ✅ · in-game ⬜ (§1n 13.1–13.2, 13.15) |
| 14 — Cloud + Discord | Share a block on Server A → import on Server B via code. Discord notification fires. | build ✅ (stubs) · in-game ⬜ (§1n 13.13) |
| 15 — Diagnostics | Diagnostics collect system state; incidents are logged with context; onboarding. | build ✅ · in-game ⬜ (§1n 13.11–13.12, 13.14) |
| 16 — Release | 1028-block stress test, 5+ players, invalid URLs handled gracefully, ship the JAR. | ⏳ |

---

## 4. How to report a test result to me

For each thing you test, tell me:
1. **What you did** (e.g. "ran runClient, made a creative world").
2. **What you expected** vs **what happened**.
3. If it broke: the **error text** or a **screenshot**.

That's all I need to mark it ✅ or chase the bug.
