# Group 05 — Silent Resource Pack Delivery

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 4 (Texture Pipeline) build-verified.
>
> **Objective:** Eliminate the vanilla "Would you like to download the resource pack?" dialog entirely. The pack must apply instantly and silently whenever a texture changes. A server config toggle allows owners to restore the dialog if needed.
>
> **Source issues:** 17.1 (no pack prompt dialog), Decision §6 (auto-click mixin), Decision §E (auto-accept mixin + `silentPack` config toggle)
>
> **Rules:** Work through each test in order. Any visible dialog is a failure.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Pack prompt | Silent — no dialog shown to players | Vanilla "Would you like to download" dialog appeared during Phase 4 testing | Fully silent via auto-accept mixin |
| Pack apply trigger | Instant on texture change | Instant (but dialog appeared) | Instant AND silent |
| Config toggle | `silentPack` existed | Not present | Restored — default `true` (silent) |
| Stability | Used vanilla engine | Used vanilla engine | Maintained — mixin only intercepts the accept button, engine unchanged |

---

## What this group covers

| Feature | Area |
|---|---|
| Auto-accept mixin | Intercepts vanilla pack prompt, auto-confirms |
| `silentPack` config | Toggle in `config/customblocks/data/config.json` |
| Pack re-apply on texture change | Triggered by any `/cb retexture` or `/cb create` with URL |
| Multi-player silence | All connected players get silent pack, no dialog for any of them |

---

## Implementation Requirements

### 1. Auto-Accept Mixin

A client-side Mixin on the vanilla pack download confirmation screen (`ConfirmScreen` or equivalent for MC 1.21.1 resource pack prompts):
- When the screen is about to render and `silentPack = true`, auto-click "Yes" before the first render frame.
- The player never sees the dialog — it fires and closes in the same tick.
- Does **not** disable the vanilla pack engine — the pack is still applied correctly through vanilla channels.
- Safe because the mod ships client+server and is pinned to MC 1.21.1.

### 2. `silentPack` Config Toggle

Stored in `config/customblocks/data/config.json`:

| Field | Type | Default | Description |
|---|---|---|---|
| `silentPack` | boolean | true | Auto-accept pack prompt (true = silent, false = shows vanilla dialog) |

Server-forced (`required=true`) packs still show a vanilla prompt regardless of this setting — this is a Minecraft limitation and is documented, not worked around.

### 3. Trigger Points

Pack is pushed silently when:
- A new block is created with a texture URL (`/cb create <id> <name> <url>`).
- An existing block is retextured (`/cb retexture <id> <url>`).
- A bulk import completes.
- `/cb sync` is called manually.

### 4. Debounce

Pack regeneration is debounced (~500ms) — multiple rapid texture changes collapse into one rebuild. This prevents pack thrash during bulk operations.

---

## Setup

Have at least one other player connected, or test in single-player (the mixin fires for the local client too).

```
/cb create g05a SilentPackTest
```

---

## Test G05.1 — No dialog on block create with URL

```
/cb create g05b SilentTest https://i.imgur.com/example.png
```

**Expected:** Block is created and the texture is applied. No "Would you like to download the resource pack?" dialog ever appears. Pack applies silently.

**Pass:** No dialog box seen. Texture loads on the block.
**Fail:** Vanilla download dialog appears.

---

## Test G05.2 — No dialog on retexture

```
/cb retexture g05a https://i.imgur.com/example.png
```

**Expected:** Texture updates silently. No dialog.

**Pass:** No dialog. Texture visible on block.
**Fail:** Dialog appears.

---

## Test G05.3 — Pack applies to all connected players silently

(Have a second player connected for this test, or skip to G05.4 if solo.)

Perform a retexture while the second player is online.

**Expected:** Both players see the updated texture. Neither player sees a dialog.

**Pass:** Both players updated silently.
**Fail:** Second player sees dialog.

---

## Test G05.4 — `silentPack = false` restores dialog

Set `silentPack = false` in `config/customblocks/data/config.json` (or via Config chest GUI, Group 21). Restart/reload.

```
/cb retexture g05a https://i.imgur.com/example2.png
```

**Expected:** The vanilla "Would you like to download the resource pack?" dialog appears.

**Pass:** Dialog appears when `silentPack = false`.
**Fail:** Still silent even with the toggle off.

Restore `silentPack = true` after this test.

---

## Test G05.5 — Debounce collapses rapid retextures

```
/cb retexture g05a https://i.imgur.com/img1.png
/cb retexture g05b https://i.imgur.com/img2.png
```
(run both within 1 second)

**Expected:** One pack rebuild fires (not two). Both textures are present in the single rebuilt pack.

Check `latest.log` — should see one `[CustomBlocks] Rebuilding resource pack…` line, not two.

**Pass:** Single rebuild with both textures.
**Fail:** Two separate pack rebuilds logged.

---

## Test G05.6 — Pack survives server restart

1. Create `g05c` with a texture.
2. Stop and restart the server.
3. Rejoin.

**Expected:** `g05c` texture loads correctly. No dialog. Pack auto-applies silently on join.

**Pass:** Texture loads and pack is silent.
**Fail:** Texture missing, or dialog shown on rejoin.

---

## Group 05 Verdict

| Test | Description | Result |
|---|---|---|
| G05.1 | No dialog on block create with URL | ⬜ |
| G05.2 | No dialog on retexture | ⬜ |
| G05.3 | No dialog for other connected players | ⬜ |
| G05.4 | `silentPack = false` restores dialog | ⬜ |
| G05.5 | Debounce collapses rapid retextures | ⬜ |
| G05.6 | Pack survives server restart silently | ⬜ |

**Group 05 passes when no dialog is ever shown during normal operation, and the config toggle correctly restores the dialog when needed.**

If anything shows ❌ — paste:
1. The exact action taken
2. Whether a dialog appeared (screenshot if possible)
3. Last 20 lines of `latest.log` at the moment of pack push

---

## Cleanup

```
/cb delete g05a
/cb delete g05b
/cb delete g05c
```
