# Group 05 — Silent Resource Pack Delivery

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 4 (Texture Pipeline) build-verified.
>
> **Objective:** Eliminate the vanilla "Would you like to download the resource pack?" dialog entirely. The pack must apply instantly and silently whenever a texture changes. A server config toggle allows owners to restore the dialog if needed.
>
> **Source issues:** 17.1 (no pack prompt dialog), Decision §6 (auto-click mixin), Decision §E (auto-accept mixin + `silentPack` config toggle)
>
> **Rules:** Work through each test in order. Any visible dialog is a failure.

---

## ✅ Status — 2026-06-15 (later 2) — FIXED on modded clients (host), confirmed in-game

> The modded client now generates the pack **locally** and silently reloads instead of relying on the
> ignored HTTP push. Developer confirmed textures load with no dialog (single-player / host). The
> vanilla-friend HTTP path (remote `httpHost`) remains a separate later step. History kept below for
> context.
>
> **What shipped:** `ServerPackGenerator.emit(PackSink)` (one source of truth for pack contents) +
> new `client/ResourcePackGenerator` (writes loose `resourcepacks/CustomBlocks/` + silent reload) +
> `RegenPackPayload` + `ResourcePackServer.sendToPlayer` modded/vanilla branch (modded = local regen,
> HTTP push skipped; vanilla = HTTP download). See `PROGRESS_LOG.md` 2026-06-15 (later 2).

### (historical) 🔴 Status — 2026-06-15 — delivery was BROKEN on modded clients (root cause found)

> ⚠️ This supersedes the original "fully silent via auto-accept mixin" claim in the tables below. As
> built, **the HTTP server-push did not deliver textures to a modded client** (the host included) —
> now resolved by the local generator above.

### What actually happens (proven from the developer's logs + files, 2026-06-15)

- **Server side now works.** The join race is fixed: a player who joins before the async build finishes
  is queued (`AWAITING_FIRST_PACK`) and the pack is pushed when the build completes. Logs show two real
  sends — `Sent resource pack to 3liSY` on join and on edit. **The server is doing its job.**
- **The client never applies the push.** Whole session: no resource reload, no download, no dialog. The
  `server-resource-packs` download cache has nothing newer than Jan 2. The integrated single-player
  server's pushed pack is simply ignored by this client.
- **The HTTP pack is valid** (`pack_format 34`, correct asset layout) — the delivery path is the fault,
  not the pack.
- **Visible textures are stale** — they come from an old local pack at `resourcepacks/CustomBlocks`
  (dated May 17, from the OLD mod); the new mod never updates it, so edits never appear.

### Root cause

This group's own design — [`client/package-info.java`](../../src/main/java/com/customblocks/client/package-info.java) —
states *modded clients generate the pack **locally** instead of downloading the HTTP pack*. That class,
**`ResourcePackGenerator`, was never built in CustomBlocks-B.** So:

- A **modded client** (the host, and modded friends) has no local generation path **and** ignores the
  HTTP push it was never meant to depend on → no textures.
- The HTTP push was only ever the path for **vanilla** clients.

### Intended delivery model (to restore)

| Client type | How it should get textures |
|---|---|
| **Modded** (host + modded friends) | **Local generation** — write `resourcepacks/CustomBlocks` + silent reload. *Missing — must be built.* |
| **Vanilla** friends | **HTTP server-push** (existing) — works on a real server once `httpHost` is the server's reachable IP. |

### Planned fix (NOT built yet)

1. Build the client-side `ResourcePackGenerator` (recycle the old project's proven version, split to fit
   the 500-line gate; pull texture bytes from the existing `/tex/<id>` HTTP route or a sync), then
   trigger a silent client resource reload.
2. For modded clients, **skip the self HTTP push** so the local pack is the single source (avoids the
   old "two-pack" conflict).
3. Keep the HTTP push for vanilla clients; fix `httpHost` for remote delivery separately.

### Already built + deployed on 2026-06-15 (necessary, not sufficient)

- **Server** `ResourcePackServer`: `AWAITING_FIRST_PACK` join-queue, cleared in `start()`/`forget()`;
  per-send logs (`Sent resource pack to …`, `Join before pack ready — … queued`).
- **Client** `ClientCommonNetworkHandlerMixin`: recognises our pack by its `"CustomBlocks textures"`
  label and silent-accepts it regardless of flag timing. Correct, but only takes effect once a modded
  client actually engages the pack — which needs the generator above.

Tracked in `Reports/GROUP_05_TESTING_GUIDE.md` §3. **Test G05.4 is retired** (the `silentPack`-off
"restores dialog" check no longer applies — goal is always-silent for our pack).

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

## Test G05.4 — ~~`silentPack = false` restores dialog~~ — **RETIRED 2026-06-15**

Retired by the join-silent fix (see the 2026-06-15 update above). Our pack is now **always silent**;
`silentPack = false` no longer restores the vanilla dialog for it. No action — skip this test.

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
| G05.4 | ~~`silentPack = false` restores dialog~~ — retired 2026-06-15 | ➖ |
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
