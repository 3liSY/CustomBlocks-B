# Phase 11 Testing Tutorial — HUD Overlay

> **Prerequisite:** Phase 10 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify the HUD overlay renders correctly when looking at a custom block.
>
> **Note on Phase 11 scope:** `/cb edithud` toggle was already verified in Phase 10 (test 10.10).
> The Bible spec also calls for HudEditorScreen (drag editor), AnimBlockScreen, and DevConsoleScreen
> — none of these exist on disk. Deferred to Phase 17 (issues 17.4, 17.16, 17.17).

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| HUD system | Full HUD overlay + drag-to-reposition editor via `edithud` | HUD overlay via `HudRenderer.java` + `HudSyncPayload`. Toggle only — no editor (Phase 17 17.4). |
| HUD data source | Server-pushed to client on join | `HudSyncPayload` populates `ClientSlotCache` on join. `HudRenderer` reads cache. |
| HUD editor | `/cb edithud` opened a drag editor screen | `/cb edithud` toggles on/off only (Phase 17 17.4). |
| AnimBlockScreen | Animation/GIF editor GUI | Not built (Phase 17 17.16). |
| DevConsoleScreen | In-game debug log viewer | Not built (Phase 17 17.17). |

---

## What Phase 11 covers (built)

| Component | File | Status |
|---|---|---|
| HUD overlay renderer | `client/HudRenderer.java` | Built |
| HUD config (visible/x/y) | `client/HudConfig.java` | Built |
| Client slot cache (data source) | `client/ClientSlotCache.java` | Built |
| HUD sync on join | `HudSyncPayload` registered in `CustomBlocksClient.java` | Built |
| HUD toggle | `/cb edithud` | Built — tested in Phase 10 (10.10) |

---

## Setup

```
/cb create p11test HudTestBlock
/cb setglow p11test 8
```

Make sure the HUD is **enabled** (run `/cb edithud` once if it was disabled):
```
/cb edithud
```
Chat should say `HUD overlay enabled.`

---

## Test 11.1 — HUD shows block info when looking at custom block

Give yourself the block:
```
/cb give p11test
```

Place the block somewhere in front of you.

**Aim your crosshair directly at the placed p11test block.**

**Expected:** A small overlay appears on-screen (top-left or configured position) showing:
- Block ID: `p11test`
- Display name: `HudTestBlock`

Move crosshair away from the block — overlay disappears or becomes blank.

**Pass:** HUD renders ID + name when aimed at custom block, clears when aimed away.
**Fail:** Nothing renders, or HUD shows wrong block info.

---

## Test 11.2 — HUD does not show for non-custom blocks

Aim crosshair at any vanilla block (dirt, stone, etc.).

**Expected:** No HUD overlay (or overlay is blank/hidden). The HUD only activates for custom blocks.

**Pass:** No overlay for vanilla blocks.
**Fail:** HUD fires for every block.

---

## Test 11.3 — HUD persists across relog

Log out and back in (or `/reload` if on singleplayer).

Place the crosshair on p11test.

**Expected:** HUD still shows `p11test` / `HudTestBlock` — data survived from `HudSyncPayload` on relog.

**Pass:** HUD still functional after relog, data re-synced from server.
**Fail:** Overlay blank after relog.

---

## Test 11.4 — HUD respects toggle state

```
/cb edithud
```

Chat: `HUD overlay disabled.`

Aim at p11test — **no overlay** should appear.

```
/cb edithud
```

Chat: `HUD overlay enabled.`

Aim at p11test — overlay returns.

**Pass:** Toggle state respected by renderer.
**Fail:** Overlay shows even when disabled, or disappears even when enabled.

---

## Phase 11 Verdict

| Test | Description | Result |
|---|---|---|
| 11.1 | HUD shows block ID + name on custom block | ⏳ PENDING REBUILD |
| 11.2 | HUD does not fire on vanilla blocks | ⏳ PENDING REBUILD |
| 11.3 | HUD data persists across relog | ⏳ PENDING REBUILD |
| 11.4 | Toggle state respected | ⏳ PENDING REBUILD |

**Phase 11 PENDING — HUD bug fixed in code but not yet rebuilt + tested in-game.**

**Two bugs were found and fixed (code changes made, build required):**

| Bug | Root cause | Fix |
|---|---|---|
| HUD never renders | `editHud()` toggled server config but never notified the client. `HudConfig.visible` only set at init — if `hudEnabled` was ever saved as `false`, renderer stayed blind forever. | New `HudStatePayload` — server sends `enabled` bool to client on toggle. Client sets `HudConfig.visible`. |
| New blocks invisible in HUD | `ClientSlotCache` only populated on player join, never refreshed mid-session. | New `HudSync.sendTo()` — called after every create/delete/rename/dupe. Also uses Gson for safe JSON (was fragile string concatenation). |

**Files changed:**
- New: `network/payloads/HudStatePayload.java`
- New: `network/HudSync.java`
- Edited: `CustomBlocksMod.java` (register payload, use HudSync on join)
- Edited: `client/CustomBlocksClient.java` (receive HudStatePayload)
- Edited: `command/handlers/GuiCommands.java` (send HudStatePayload on toggle)
- Edited: `command/handlers/CreationCommands.java` (syncHud after mutations)

**Action required:** `.\gradlew.bat build` then relaunch, then run tests 11.1–11.4.

> **Missing from Phase 11 spec:**
> - `HudEditorScreen` (drag editor) → Phase 17 issue 17.4
> - `AnimBlockScreen` (animation editor) → Phase 17 issue 17.16
> - `DevConsoleScreen` (debug viewer) → Phase 17 issue 17.17

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p11test
```
