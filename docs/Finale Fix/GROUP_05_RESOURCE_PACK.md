# Group 05 — Silent Resource Pack Delivery

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 4 (Texture Pipeline) build-verified.
>
> **Objective:** Eliminate the vanilla "Would you like to download the resource pack?" dialog entirely. The pack must apply instantly and silently whenever a texture changes. A server config toggle allows owners to restore the dialog if needed.
>
> **Source issues:** 17.1 (no pack prompt dialog), Decision §6 (auto-click mixin), Decision §E (auto-accept mixin + `silentPack` config toggle)
>
> **Rules:** Work through each test in order. Any visible dialog is a failure.

---

## 🟡 Status — 2026-06-20 — REMOTE/dedicated servers: newly-created blocks render magenta (root cause found, fix designed, NOT built)

> Supersedes the 2026-06-15 "FIXED on modded clients (host)" scope. That fix covers only the
> **integrated host** (client JVM == server JVM). On a **remote dedicated server** (e.g. yoyoo.mcsh.io)
> a modded client cannot see blocks **created after it loaded its own local data** — they show the
> magenta/black missing-texture checkerboard. Blocks already in the client's local `slots.json` render
> fine, so **only newly-created blocks are magenta** (owner-confirmed symptom).

### Root cause (proven from the owner's client `latest.log`, 2026-06-20)

The modded client builds its pack from its **own local `slots.json`**, not the server's data.

- `CustomBlocksMod.onInitialize()` → `SlotManager.loadAll()` (common entrypoint, runs on the client too)
  loads the client's local `slots.json` unconditionally. Log: `16:22:49 Loaded 1014 saved custom block(s)`
  — **before** connecting anywhere.
- `16:24:48 Connecting to yoyoo.mcsh.io` → server sends `RegenPackPayload` → client runs
  `ResourcePackGenerator.regenerate()` → `16:24:56 Local pack written (4081 files)` → `16:25:05 applied`.
- That local build reads `SlotManager.assignedSlots()` + `TextureStore` from the **client JVM** — its stale
  local copy. Server-created blocks (frmf1) are absent → those slots emit the `empty_slot` model → magenta.
- The `JsonParseException: No key pack_format` at log line 465 is unrelated noise (a malformed third-party
  pack, "Texture-Packs.com"); CustomBlocks still loaded + applied. Not the cause.
- The download worked — `/cb create` only creates the block AFTER decode; the block exists + is named.

### Why the OLD mod didn't have this

The old mod streamed every texture's bytes to each client over the mod's network channel (drip-feed +
chunking), so the client built its pack from the **server's** textures — no port, worked on any server.
CustomBlocks-B replaced that with "modded client rebuilds from its OWN local data", which is only correct
when client and server share a JVM (singleplayer/host). On a remote server it uses stale local data.

### Chosen fix — file-level pack sync over the mod channel (NOT a copy of the old code)

Owner decision 2026-06-20: build it **from scratch, simpler**. The old `NetworkManager`/per-slot drip-feed
is **not** to be copied (it crammed texture+meta+faces+variants+anim into one payload and mutated client
slot state — that's where its bugs lived).

**Design C — sync the pack as a folder of files.** The unit is a dumb `(path, bytes)`; all pack-building
logic stays server-side in the existing `ServerPackGenerator.emit()` (single source of truth, zero drift).

1. **Server** computes a manifest `{path → sha1}` from `emit()`.
2. **Modded client joins a DEDICATED server** → server sends the manifest → client diffs it against its
   on-disk loose pack (`resourcepacks/CustomBlocks`) → requests only missing/changed files → server streams
   those (chunked when > ~512 KB) → "done" sentinel → one silent reload. Unchanged rejoin ≈ free.
3. **Block created/changed** → server pushes just the changed `slot_N.*` files + "done".
4. **Client writes files only** — never mutates its `SlotManager`/`TextureStore`, so the host's local
   `slots.json` can never be clobbered.
5. **Initial join is throttled** — cap ≤ ~256 KB/player/tick so a large first sync (≈37 MB for 1000+ blocks)
   never lags the connection on shared hosting.

**Scope:** keep the existing local-regen (`RegenPackPayload`) for the **integrated host**
(`!serverInstance.isDedicated()`) — verified, free, untouched. File-sync runs **only** when
`serverInstance.isDedicated()`. Vanilla clients on a dedicated server keep the HTTP path (separate concern).

General by design — fixes every remote/dedicated server, not just yoyoo; needs no open HTTP port.

**Planned new classes (each ≤ 500-line gate):** `network/packsync/PackManifest` (hash emit() + folder),
`network/packsync/PackSyncService` (server per-player queue + tick throttle + send/diff), client
`client/packsync/ClientPackReceiver` (buffer chunks → write files → silent reload), and small payloads
(`PackManifestPayload` S2C, `PackFilePayload` S2C, `PackRequestPayload` C2S, `PackDonePayload` S2C).

**Tests:** G05.7 + G05.8 below. **Status: designed, awaiting build + in-game confirm.**

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

## Test G05.7 — REMOTE/dedicated server: newly-created block shows on a modded client ⭐ (the frmf1 bug)

Requires a **real dedicated server** (or `runServer`), not single-player. Join it with the mod installed.

1. While connected to the dedicated server, run:
   ```
   /cb create g05remote RemoteTest https://i.imgur.com/example.png
   ```
2. Place the block and look at it.

**Expected:** the block shows its real texture — NOT the magenta/black checkerboard.
Check `latest.log` (client) for the file-sync applying, and no `customblocks:block/placeholder` for this slot.

**Pass:** new block renders its texture on the remote server.
**Fail:** new block is magenta (the pre-fix bug).

---

## Test G05.8 — Rejoin a dedicated server is near-instant (manifest diff, no re-download)

1. After G05.7, fully disconnect and rejoin the same dedicated server (no blocks changed meanwhile).
2. Watch `latest.log`.

**Expected:** the client recognises it already has every pack file (manifest matches) and transfers
~nothing — no full re-sync. All existing blocks still render.

**Pass:** rejoin is fast, no large file transfer, textures intact.
**Fail:** full ~37MB re-sync on every rejoin, or textures missing after rejoin.

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
| G05.7 | Remote/dedicated: new block renders on modded client (not magenta) | ⬜ |
| G05.8 | Rejoin a dedicated server is near-instant (manifest diff) | ⬜ |

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
