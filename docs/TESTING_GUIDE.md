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
| 4 — Texture Pipeline | `/cb retexture <id> <url>` changes the block's look **for everyone** within ~3s. | ⏳ |
| 5 — Client Architecture | **Vanilla AND modded** clients both see textures. No purple/black blocks. No disconnect on big packs. | ⏳ |
| 6 — Undo/Redo + Attributes | Player A's undo doesn't touch Player B's blocks. Shapes & custom sounds work. | ⏳ |
| 7 — Tools / Items | All **7 tools** work on custom blocks, with **instant** response (no delay). | ⏳ |
| 8 — Color + Categories | Blocks auto-categorize, color detection works, **batch recolor doesn't destroy designs**. | ⏳ |
| 9 — Import/Export + Search | Export → delete → import = **identical** block. Search is instant. Templates work. | ⏳ |
| 10 — GUI | Full create/edit workflow via menus, no typing. **No GUI file over 500 lines.** | ⏳ |
| 11 — HUD | `/cb edithud` opens the drag editor; HUD shows block info when you look at one. | ⏳ |
| 12 — Arabic | Import letters → browse in GUI → place → **auto-join** swaps to the right form. | ⏳ |
| 13 — AI + Video + Macros | AI parses "make a glowing red block"; video frames extract; macros record & replay. | ⏳ |
| 14 — Cloud + Discord | Share a block on Server A → import on Server B via code. Discord notification fires. | ⏳ |
| 15 — Diagnostics | Diagnostics collect system state; incidents are logged with context. | ⏳ |
| 16 — Release | 1028-block stress test, 5+ players, invalid URLs handled gracefully, ship the JAR. | ⏳ |

---

## 4. How to report a test result to me

For each thing you test, tell me:
1. **What you did** (e.g. "ran runClient, made a creative world").
2. **What you expected** vs **what happened**.
3. If it broke: the **error text** or a **screenshot**.

That's all I need to mark it ✅ or chase the bug.
