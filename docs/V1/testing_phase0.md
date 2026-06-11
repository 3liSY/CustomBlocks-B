# PHASE 0 TESTING GUIDE

Phase 0 tests that the mod **initializes and loads correctly**. Nothing fancy yet.

You'll test 4 things. Each takes 1 minute.

---

## BEFORE YOU START

- JAR is in `.minecraft/mods/`
- Minecraft ready to launch
- 5 minutes total

---

## TEST 1: MOD INITIALIZES

**What:** Launch game and create world. Check mod doesn't crash.

**How:**
1. Open Minecraft
2. Click "Singleplayer"
3. Click "Create New World"
4. Wait for world to load (no loading screen = done)

**Success if:** No crash, world loads normally

**Fail if:** Game crashes or hangs

**Mark:** [ ] Pass [ ] Fail

---

## TEST 2: CONFIG FILE EXISTS

**What:** Verify mod created its config file with correct settings.

**How:**
1. Close Minecraft
2. Go to folder: `.minecraft/config/customblocks/`
3. Open `config.json` with Notepad
4. Look for these 11 fields inside:
   - maxSlots
   - httpPort
   - textureSize
   - httpHost
   - maxUndoDepth
   - undoMode
   - hudEnabled
   - aiApiKey
   - aiTextureEnabled
   - vaultEndpoint
   - discordWebhookUrl

**Success if:** File exists, all 11 fields present

**Fail if:** File missing or fields missing

**Mark:** [ ] Pass [ ] Fail

---

## TEST 3: RELOAD COMMAND WORKS

**What:** Verify mod doesn't crash when you reload it in-game.

**How:**
1. Launch Minecraft again, load your world
2. Press T to open chat
3. Type: `/reload`
4. Wait 2 seconds for reload to finish
5. Check: do you see chat message? No crash?

**Success if:** Reload completes, no crash

**Fail if:** Crash or disconnect to main menu

**Mark:** [ ] Pass [ ] Fail

---

## TEST 4: NO EARLY FEATURES

**What:** Verify Phase 0 isn't loading Phase 1+ features yet.

**How:**
1. Still in-game, press T for chat
2. Type: `/give @s customblocks:slot_0`
3. Press Enter

**Success if:** Command fails (block doesn't exist yet)

**Fail if:** Block appears in inventory (shouldn't exist at Phase 0)

**Mark:** [ ] Pass [ ] Fail

---

## SUMMARY

All 4 tests pass = **Phase 0 works**

If any fail, report which test + what happened.

