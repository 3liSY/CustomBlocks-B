# Phase 5 Testing Tutorial — Client Architecture

> **Prerequisite:** Phase 4 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify the client-side pipeline — HUD overlay, GUI screens via commands,
> and client initialization — all work correctly.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What Phase 5 covers

| Feature | File |
|---|---|
| Client mod initialization | `CustomBlocksClient.java` |
| HUD overlay (block ID + name when looking at custom block) | `HudRenderer.java` |
| HUD position + visibility config | `HudConfig.java` |
| Client slot cache (populated on join, used by HUD) | `ClientSlotCache.java` |
| GUI screens opened via server→client packets | `OpenGuiPayload.java` |
| HUD data synced from server on join | `HudSyncPayload.java` |

---

## Test 5.1 — Client initializes (log check)

Open `%AppData%\.minecraft\logs\latest.log` (or check the console if running `runClient`).

Search for:
```
[CustomBlocks] Client initializing (Phase 10/11 — GUI + HUD).
```

**Pass:** Line appears near the top of the log (shortly after Fabric loads).
**Fail:** Line missing — client entrypoint not registered in `fabric.mod.json`.

---

## Test 5.2 — HUD overlay: block info when looking at custom block

1. You need at least one placed custom block in the world. Use one from Phase 4, or:
   ```
   /cb create p5hud HudTest
   /cb give p5hud
   ```
   Place it.

2. Look directly at the placed block with your crosshair.

**Expected:** In the **top-left corner** of the screen, you see:
```
p5hud "HudTest"
```
(Yellow ID, grey quoted name)

**Pass:** Overlay appears when looking at the block, disappears when looking away.
**Fail:** Nothing appears, or appears even when not looking at a custom block.

---

## Test 5.3 — HUD shows correct data for multiple blocks

Place two different custom blocks next to each other. Look at each one in turn.

**Expected:** HUD text changes to match whichever block the crosshair is on.

**Pass:** Two different IDs/names shown correctly, no mix-up.
**Fail:** Wrong block info shown, or HUD stays on first block when looking at second.

---

## Test 5.4 — HUD toggle off/on

```
/cb config hud false
```
Look at a placed custom block.

**Expected:** HUD overlay is **gone** — nothing in top-left.

Then:
```
/cb config hud true
```
Look at the block again.

**Expected:** HUD overlay returns.

**Pass:** Toggle works both directions.
**Fail:** Toggle has no effect, or crashes.

---

## Test 5.5 — GUI: Main Menu screen

```
/cb gui
```

**Expected:** A GUI screen opens in-game — the CustomBlocks main menu.

Press `Esc` to close.

**Pass:** Screen opens and closes cleanly.
**Fail:** Nothing opens, crash, or error in chat.

---

## Test 5.6 — GUI: Macros screen

```
/cb gui macros
```

**Expected:** The macro management screen opens.

Press `Esc` to close.

**Pass:** Screen opens and closes.
**Fail:** Nothing, wrong screen, or crash.

---

## Test 5.7 — GUI: Arabic browser screen

```
/cb gui arabic
```

**Expected:** The Arabic letter browser screen opens.

Press `Esc` to close.

**Pass:** Screen opens and closes.
**Fail:** Nothing, wrong screen, or crash.

---

## Test 5.8 — GUI: Block editor screen

```
/cb create p5edit EditTest
/cb gui editor p5edit
```

**Expected:** The block editor screen opens with `p5edit` loaded.

Press `Esc` to close.

**Pass:** Editor opens with correct block.
**Fail:** Nothing, generic screen, wrong block, or crash.

---

## Phase 5 Verdict

| Test | Description | Result |
|---|---|---|
| 5.1 | Client init log message present | ⬜ |
| 5.2 | HUD shows ID + name when looking at custom block | ⬜ |
| 5.3 | HUD updates correctly between two different blocks | ⬜ |
| 5.4 | HUD toggle off/on via `/cb config hud` | ⬜ |
| 5.5 | `/cb gui` opens main menu screen | ⬜ |
| 5.6 | `/cb gui macros` opens macro screen | ⬜ |
| 5.7 | `/cb gui arabic` opens Arabic browser | ⬜ |
| 5.8 | `/cb gui editor <id>` opens block editor | ⬜ |

**When all 8 show ✅ — say "Phase 5 passes" and we move to Phase 6.**

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p5hud
/cb delete p5edit
```
