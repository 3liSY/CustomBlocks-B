# Phase 1 Testing Tutorial — Block Slot System

> **Goal:** Verify the mod loads, registers all slot blocks, creates its config file,
> shows the creative tab, and lets you place/break a block. No textures yet.
>
> **Rules:** Work through each test in order. If one fails, stop and report it before
> moving forward. Do not skip a test.

---

## Before You Start — One-Time Setup

### 1. Build the mod
Open PowerShell in the project folder:
```
.\gradlew.bat build
```
You must see **BUILD SUCCESSFUL** at the end.  
If it says BUILD FAILED — stop and report the red error text.

### 2. Check your config file
The file at:
```
C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\config.json
```
contains `"maxSlots": 2048`. That means the mod will register **2048 slot blocks**.  
All tests below use 2048 as the expected number. If you ever change this value,
update the expected numbers accordingly.

---

## Launch the Mod

Run the Minecraft client with the mod loaded. Since you're using a real Minecraft
install (not `runClient`), load the mod jar into your mods folder and launch
normally.

When the title screen appears — that is a pass on its own. No crash = good.

---

## Test 1.1 — Registration log line ⭐ Most important

**What to check:** The mod printed its startup lines in the log.

1. After the title screen loads, open the log file:
   ```
   C:\Users\66664\AppData\Roaming\.minecraft\logs\latest.log
   ```
2. Press **Ctrl+F** and search for `[CustomBlocks]`.
3. You should find all three of these lines (in order):
   ```
   [CustomBlocks] Initializing CustomBlocks v1.0.0 (Phase 10+)
   [CustomBlocks] Config loaded: maxSlots=2048, httpPort=8123, ...
   [CustomBlocks] Registered 2048 slot blocks (slot_0 to slot_2047).
   ```

**Pass:** All 3 lines found, numbers match your config.  
**Fail:** Any line missing, or a crash before the title screen.

> ⚠️ You will also see thousands of lines like:
> `missing model for variant: 'customblocks:slot_3#light=7'`
>
> **This is expected and harmless.** These warnings fire at launch because the mod
> registers 2048 blocks but the textures only load after you join a server. They
> do not cause crashes. Ignore them for Phase 1 testing.

---

## Test 1.2 — Config file was created

**What to check:** The mod wrote its config file to disk.

1. Open File Explorer and navigate to:
   ```
   C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\
   ```
2. You should see `config.json` in that folder.
3. Open it. It should contain at minimum:
   ```json
   {
     "maxSlots": 2048,
     "httpPort": 8123,
     "textureSize": 64,
     "httpHost": "127.0.0.1"
   }
   ```

**Pass:** File exists, contains those fields.  
**Fail:** File missing, or file is empty / corrupted.

---

## Test 1.3 — Creative tab appears in inventory

**What to check:** The mod added its own tab to the creative inventory.

1. In Minecraft, go to **Singleplayer → Create New World**.
   - Game Mode: **Creative**
   - Cheats: **Allowed** (you'll need them for later tests)
2. Once in the world, press **E** to open your inventory.
3. Click through the tabs at the top until you find one labeled **"CustomBlocks"**
   (it has a bookshelf icon).

**Pass:** The tab exists.  
**Fail:** No tab, or the game crashed while creating the world.

> At this stage the tab will be **empty** — that is correct and expected.  
> Blocks only appear in the tab after you use `/cb create` to make one (Batch 2).

---

## Test 1.4 — Check the second creative tab: tools

**What to check:** The mod also registered a tools tab.

1. Still in the creative inventory, look for a second CustomBlocks tab called
   **"CustomBlocks Tools"** (glowstone dust icon or similar).
2. Open it — it should contain 3 tool items:
   - **Lumina Brush** (cycles glow)
   - **Chisel** (cycles hardness)
   - **Deleter** (removes placed blocks)

**Pass:** Tools tab exists with 3 items.  
**Fail:** Tab missing, or fewer than 3 items.

---

## Test 1.5 — Place and break a slot block

**What to check:** You can physically place and break a slot block with no crash.

> You need a block item first. Open the F3 debug screen for its slot number, OR use
> the `/cb create` command from Batch 2. For this test, use the command:

1. With cheats on, type in chat:
   ```
   /cb create test1 TestBlock
   ```
   You should see a green message confirming it was created.

2. Open the **CustomBlocks** creative tab — `TestBlock` should now appear there.

3. Take the item and **place it** somewhere in the world.
   - It will look like a **purple/black checkerboard cube** — that is correct, no texture yet.

4. **Break it** with your hand (Creative mode = instant break).

**Pass:** Block placed, block broken, no crash, no error in chat.  
**Fail:** Crash, red error in chat, or block refuses to place/break.

---

## Test 1.6 — Log line after joining

**What to check:** The HTTP resource pack server started when you entered the world.

1. After entering the world (from Test 1.5), check the log again:
   ```
   C:\Users\66664\AppData\Roaming\.minecraft\logs\latest.log
   ```
2. Search for `[CustomBlocks]` and look for:
   ```
   [CustomBlocks] Resource-pack HTTP server live on port 8123
   [CustomBlocks] Loaded X saved custom block(s).
   ```
   (After Test 1.5 created `test1`, the second line should show at least `1`.)

**Pass:** HTTP server line found, no "Could not open an HTTP port" error.  
**Fail:** Error line saying the port failed, or line missing entirely.

---

## Phase 1 Verdict

Mark each result below:

| Test | Description | Result |
|---|---|---|
| 1.1 | Registration log lines appear | ✅ 2026-06-07 |
| 1.2 | Config file created on disk | ✅ 2026-06-07 |
| 1.3 | CustomBlocks creative tab exists | ✅ 2026-06-07 |
| 1.4 | CustomBlocks Tools tab with 3 items | ✅ 2026-06-07 |
| 1.5 | Slot block can be placed and broken | ✅ 2026-06-07 |
| 1.6 | HTTP server startup line in log | ✅ 2026-06-07 |

**Phase 1 confirmed ✅ 2026-06-07 — proceed to `docs/BATCH2_TESTING.md`.**

If anything shows ❌ — paste:
1. The exact command you ran (if any)
2. What you expected vs what happened
3. The last 20 lines of `latest.log` at the point of failure

---

## Quick Reference — Log File Location

| What | Where |
|---|---|
| Log file | `C:\Users\66664\AppData\Roaming\.minecraft\logs\latest.log` |
| Config folder | `C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\` |
| Saved blocks | `C:\Users\66664\AppData\Roaming\.minecraft\config\customblocks\slots.json` |

---

## What the warnings look like (normal, not a bug)

You will see a wall of text like this in the log:
```
[Worker-Main-2/WARN]: Exception loading blockstate definition:
'customblocks:blockstates/slot_850.json' missing model for variant:
'customblocks:slot_850#light=7'
```
There will be roughly **32,768 of these** (2048 slots × 16 light levels).  
They are **WARN level, not ERROR level.** The game does not crash. Blocks still work.  
These fire at launch because the texture pack only loads after joining a server,
not at the main menu. This is a known limitation — accepted for now.
