# Group 03 — HUD System & ESC Integration

> **Prerequisite:** Group 02 (Chest GUI) verified in-game.
>
> **Objective:** Restore the HUD overlay system (block ID + name when looking at a custom block), build the full drag-to-reposition HUD editor with customization, and inject CustomBlocks buttons into the ESC/pause menu.
>
> **Source issues:** 17.4 (HUD config system), 17.6 (HUD overlay absent), 17.7 (ESC menu integration)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| HUD overlay | Displayed block ID + display name when crosshair on custom block | Missing entirely | Fully restored |
| HUD config | `/cb config hud` and `/cb edithud` with drag-to-reposition | `/cb edithud` toggle-only stub | Full drag-to-reposition editor with Color, Scale, Position, Background Opacity |
| ESC menu | Two custom buttons in pause menu | Missing | Two standard gray buttons: "CustomBlocks Menu" and "HUD Editor" |
| HUD persistence | HUD position/settings survived restart | Not applicable | Persists to `config/customblocks/data/hud-config-server.json` |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| HUD overlay | Crosshair-targeted block ID + name rendering |
| HUD toggle | `/cb config hud toggle` (or via Config chest GUI) |
| HUD editor | `/cb edithud` → drag-to-reposition chest GUI |
| HUD customization | Color, Scale, Position, Background Opacity |
| ESC menu | Two injected buttons below "Leave Game" |
| HUD persistence | `config/customblocks/data/hud-config-server.json` |

---

## Implementation Requirements

### 1. HUD Overlay

When the player's crosshair is on a custom block:
- Renders block ID and display name in the top-left of the screen (default position).
- Respects the configured position, scale, color, and background opacity.
- Disappears immediately when crosshair moves off a custom block.
- Implemented via `HudRenderMixin` injecting into `InGameHud`.
- Client-side only — server sends the block index via `HudSyncPayload` on join.

### 2. HUD Config System

`/cb config hud` opens the HUD config section within the Config chest GUI (Group 21).

Configurable fields stored in `hud-config-server.json`:

| Field | Type | Default | Description |
|---|---|---|---|
| `hudEnabled` | boolean | true | Show/hide HUD overlay |
| `hudX` | int | 5 | X position (pixels from left) |
| `hudY` | int | 5 | Y position (pixels from top) |
| `hudScale` | float | 1.0 | Text scale multiplier |
| `hudColor` | hex string | `#FFFFFF` | Text color |
| `hudBgOpacity` | float | 0.4 | Background rectangle opacity (0 = none) |
| `hudShowId` | boolean | true | Show block ID in HUD |
| `hudShowName` | boolean | true | Show display name in HUD |

### 3. HUD Editor

`/cb edithud` opens a drag-to-reposition HUD editor overlay:
- Player can click and drag the HUD element to any screen position.
- Buttons to increase/decrease scale, change color (preset palette), adjust bg opacity.
- "Save" button commits changes to `hud-config-server.json`.
- "Reset" button restores defaults.
- Changes are visible in real-time while editing.

### 4. ESC Menu Integration

Two buttons injected into the vanilla pause/ESC menu, positioned equally below the "Leave Game" button:
- **"CustomBlocks Menu"** — opens the main dashboard chest GUI.
- **"HUD Editor"** — opens `/cb edithud` directly.
- Both buttons use Command Block texture as icon (as decided).
- Standard gray Minecraft button style.
- Implemented via Mixin on the `GameMenuScreen`.

### 5. HUD Sync

On player join: server sends `HudSyncPayload` containing the full block ID→name map. Client caches this in `ClientSlotCache`. HUD reads from this cache — no server round-trip per frame.

---

## Setup

```
/cb create g03a HudTestBlock
/cb retexture g03a https://i.imgur.com/example.png
```

Place `g03a` somewhere visible and stand close enough to look at it.

---

## Test G03.1 — HUD overlay appears

Look directly at the placed `g03a` block.

**Expected:** HUD renders in the top-left showing:
```
g03a
HudTestBlock
```

**Pass:** Both block ID and display name appear on screen.
**Fail:** Nothing renders, or only one field shows.

---

## Test G03.2 — HUD disappears when looking away

Look away from `g03a` to any non-custom block or the sky.

**Expected:** HUD overlay disappears immediately.

**Pass:** HUD gone when not targeting a custom block.
**Fail:** HUD stays on screen permanently.

---

## Test G03.3 — HUD toggle off

```
/cb config hud toggle
```
(or toggle via Config chest GUI)

**Expected:** `[CustomBlocks] HUD disabled.` Look at `g03a` — no HUD appears.

**Pass:** HUD hidden after toggle.
**Fail:** HUD still shows after toggle.

---

## Test G03.4 — HUD toggle on

```
/cb config hud toggle
```

**Expected:** `[CustomBlocks] HUD enabled.` Look at `g03a` — HUD reappears.

**Pass:** HUD visible again.
**Fail:** HUD stays hidden.

---

## Test G03.5 — HUD settings persist across restart

1. Set HUD scale to 1.5 via `/cb edithud` and save.
2. Run `/stop` (or relog).
3. Rejoin world and look at `g03a`.

**Expected:** HUD renders at the larger scale (1.5×), not the default.

**Pass:** Scale persisted after restart.
**Fail:** Scale reset to default after restart.

---

## Test G03.6 — HUD editor opens

```
/cb edithud
```

**Expected:** A drag-to-reposition editor overlay opens. The HUD element is visible and draggable. Buttons for scale, color, opacity, Save, and Reset are present.

**Pass:** Editor opens with all controls.
**Fail:** Command missing, toggle-only behavior, or screen-based editor.

---

## Test G03.7 — HUD drag reposition

In `/cb edithud`, click and drag the HUD element to the bottom-right of the screen. Click "Save".

**Expected:** HUD element moves to the new position. After saving and closing, the HUD renders at the new position when looking at `g03a`.

**Pass:** Position saved and renders correctly.
**Fail:** Drag doesn't work, or position resets after closing editor.

---

## Test G03.8 — ESC menu buttons present

Press **ESC** in-game to open the pause menu.

**Expected:** Two gray buttons appear below "Leave Game":
1. "CustomBlocks Menu" with Command Block icon
2. "HUD Editor" with Command Block icon

Both are evenly spaced and not overlapping vanilla buttons.

**Pass:** Both buttons visible and positioned correctly.
**Fail:** Buttons missing, overlapping vanilla buttons, or wrong icons.

---

## Test G03.9 — ESC menu "CustomBlocks Menu" button

Press **ESC** and click "CustomBlocks Menu".

**Expected:** Pause menu closes and main dashboard chest GUI opens.

**Pass:** Chest GUI opens correctly.
**Fail:** Nothing happens, error, or wrong GUI opens.

---

## Test G03.10 — ESC menu "HUD Editor" button

Press **ESC** and click "HUD Editor".

**Expected:** Pause menu closes and the HUD drag-to-reposition editor opens.

**Pass:** HUD editor opens correctly.
**Fail:** Nothing happens, or wrong screen opens.

---

## Group 03 Verdict

| Test | Description | Result |
|---|---|---|
| G03.1 | HUD overlay appears on custom block | ⬜ |
| G03.2 | HUD disappears when not targeting custom block | ⬜ |
| G03.3 | HUD toggle off | ⬜ |
| G03.4 | HUD toggle on | ⬜ |
| G03.5 | HUD settings persist across restart | ⬜ |
| G03.6 | HUD editor opens with full controls | ⬜ |
| G03.7 | Drag reposition saves correctly | ⬜ |
| G03.8 | ESC menu shows two CB buttons | ⬜ |
| G03.9 | ESC "CustomBlocks Menu" opens dashboard | ⬜ |
| G03.10 | ESC "HUD Editor" opens editor | ⬜ |

**Group 03 passes when all HUD and ESC features are confirmed working in-game by the developer.**

If anything shows ❌ — paste:
1. The exact action taken
2. What appeared vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g03a
```
