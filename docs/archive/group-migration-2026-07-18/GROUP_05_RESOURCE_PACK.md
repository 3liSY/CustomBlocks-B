# Group 05 — Silent Resource Pack Delivery

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 4 (Texture Pipeline) build-verified.
>
> **Objective:** Eliminate the vanilla "Would you like to download the resource pack?" dialog entirely. The pack must apply instantly and silently whenever a texture changes. A server config toggle allows owners to restore the dialog if needed.
>
> **Source issues:** 17.1 (no pack prompt dialog), Decision §6 (auto-click mixin), Decision §E (auto-accept mixin + `silentPack` config toggle)
>
> **Rules:** Work through each test in order. Any visible dialog is a failure.

---

## UI medium audit (2026-07-09)

TextureSizeMenu + RetextureConfirmMenu (folded in) → **Screen, not built.** See `docs/UI_MEDIUM_GUIDE.md`.

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

This group's own design — [`client/package-info.java`](../src/main/java/com/customblocks/client/package-info.java) —
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

> ⚠️ **Synced 2026-07-10:** this table was blank/stale, orphaned from the real pass history. Actual status
> lives in `docs/testing/GROUP_05_TESTING_GUIDE_DONE.md` — **✅ 12/13 pass** (Last tested 2026-07-03), only
> B3 (SP-only fast create/retexture burst → double reload + Corrupt PNG) still open.

| Test | Description | Result |
|---|---|---|
| G05.1 | No dialog on block create with URL | ✅ (see TG_DONE) |
| G05.2 | No dialog on retexture | ✅ (see TG_DONE) |
| G05.3 | No dialog for other connected players | ✅ (see TG_DONE) |
| G05.4 | ~~`silentPack = false` restores dialog~~ — retired 2026-06-15 | ➖ |
| G05.5 | Debounce collapses rapid retextures | ✅ (see TG_DONE) |
| G05.6 | Pack survives server restart silently | ✅ (see TG_DONE) |
| G05.7 | Remote/dedicated: new block renders on modded client (not magenta) | ✅ (see TG_DONE) |
| G05.8 | Rejoin a dedicated server is near-instant (manifest diff) | ✅ (see TG_DONE) — B3 open (SP-only reload burst) |

**Group 05 passes when no dialog is ever shown during normal operation, and the config toggle correctly restores the dialog when needed.** Currently 12/13 — only B3 open.

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

---

## G05-1 · "Needs rejoin" — tools skip the client resync (+ deeper corruption)

> 🔍 **umbrella** — roots: slot-guard (→ G06-2/04) + resync broadcast (→ here) + slot data corruption (→ G06-2/3)
>
> 🟢 **Root A (resync) BUILT 2026-06-26 — build-green, awaiting in-game test.** Implemented as `HudSync.broadcast(server)`
> (HUD/identity to **all** clients) wired into `DeleterItem`, `ColorVariantService`, `ColorToolService`, and the
> create/delete commands (was actor-only), **+** a texture convergence guard in `ResourcePackGenerator` (integrated
> host: after each reload, re-run once toward the live pack hash so a dedup gate can't strand a new texture). Test =
> `GROUP_06_TESTING_GUIDE.md` **§M**. Root B (slot-data corruption) was the separate slice-1 `FreedSlots` guard.

> *"some stuff dont work unless rejoin, and they are really broken, such as the deleting issues and
> when switching colors with squares so many problems occur"* … *(after rejoin)* *"No, still broken"*

**Symptom** — After using the item tools (red Deleter, color Squares/Triangles), the result doesn't
show correctly until the player **rejoins** — and the developer confirms **some of it stays broken even
after a rejoin**. Two layers stacked on top of each other.

**Why — TWO roots:**

**Root A — tools skip the live client resync (the "needs rejoin" layer).** Every **command** handler
calls `HudSync.sendTo(player)` after a mutation — that's what refreshes the client's `ClientSlotCache`
(block names / HUD / tooltip data). But **no item tool does** (verified: zero `HudSync` usage anywhere
in `item/`). `DeleterItem` only calls `ResourcePackServer.updatePack()` ([line 49](CustomBlocks-B/src/main/java/com/customblocks/item/DeleterItem.java#L49)); `ColorVariantService`
(Square swap / Triangle create) only `updatePack` + give. So after a tool edit the client's cache is
**stale**, and the pack reload is slow/unreliable on a dedicated server — a rejoin re-pushes HudSync +
the full pack, so it "fixes" the **view**. Affected tools: **Deleter, Square, Triangle, Omni, Chisel,
Lumina, Rainbow** (all of `item/`). *Secondary gap:* even the command path's `HudSync.sendTo(actor)`
only resyncs the **actor**, not other online players — so in MP everyone else stays stale too.

**Root B — the data is genuinely corrupted (the "still broken after rejoin" layer).** Rejoin reloads
slot data from the server; if that data is actually wrong, rejoin reloads the **same wrong data**. This
is the **slot-recycling / unclean-delete corruption already tracked in G06-2 + G06-3** (delete frees a
slot without guarding it → next create reuses it → placed blocks wear the wrong skin / collide). That's
why a rejoin doesn't heal it — same root as G06-2/G06-3, surfacing here.

### Developer's input (UI 2026-06-23)

- **Rejoin does NOT fully fix it** → confirms Root B (real corruption), not just a view-staleness gap.
- "Scan everything" → the resync gap is **system-wide across every item tool**, not one tool.

### Brainstorm (fix direction)

1. **Centralize the after-edit resync.** One shared `AfterEdit.broadcast(server)` (HudSync to **all**
   online players + `updatePack`) that **every** mutation path calls — commands AND tools. Today each
   command re-implements `HudSync.sendTo(actor)` and tools forget entirely; a single choke-point means
   no path can skip it and MP viewers all refresh. Ideally fire it from a post-mutation hook in
   `SlotManager` so it's structurally impossible to miss.
2. **Make the pack reload actually land live on dedicated clients** (the G06-1/G10-1 propagation thread)
   so textures update without a rejoin.
3. **Root B is G06-2 + G06-3** — fix the slot-guard + clean delete there; this item just documents that
   their corruption is what survives a rejoin.

| Group | Scope | Status |
|---|---|---|
| G05 — Sync/Pack | Both — view-staleness bites on dedicated; corruption (Root B) hits both | 🔍 diagnosed — Root A is a clear gap; Root B = G06-2/G06-3 |

> ✋ **In-game re-confirmed 2026-06-25** (testing G06 §L L3 + §K row 5, after the slot-reuse-guard slice 1 shipped):
> a freshly **created** block shows its **name + HUD but no texture** until a **rejoin**; a custom-hex variant's
> **HUD is absent** when aimed at until a **rejoin**. Pure Root A (view-staleness / pack reload too slow), exactly
> as diagnosed. **This is the next code step** = the shared `AfterEdit.broadcast` resync-to-all + a fast texture
> push so creates/deletes/tool-edits land **fresh with no rejoin** (also closes G06-2 slice 2).

**Related:** **G06-2 / G06-3** (the corruption that survives rejoin — Root B) · **G06-1 / G10-1** (pack/config not reaching dedicated clients live) · `HudSync` (the resync every command calls and every tool skips)
**Touches:** new `AfterEdit.broadcast` choke-point (HudSync-to-all + `updatePack`) · every `item/` tool (`DeleterItem`, `ShapeToolItem`, `OmniToolItem`, `ChiselItem`, `LuminaBrushItem`, `RainbowRectangleItem`) · `ColorVariantService` / `ColorToolService` · `SlotManager` (post-mutation hook) · command handlers (route through the shared resync)
**Verify in-game:** after the resync fix, what (if anything) is STILL wrong post-rejoin → that residue is pure G06-2/G06-3.

---

## G05-2 · Delete-then-create scrambles the new block's NAME + HUD (shows the old block)

> 🔍 diagnosed — name/HUD surface of the same G06-2+04 slot-reuse root; fixed by same slot-guard + `AfterEdit.broadcast`

> *"when deleting a block with the command or deleter tool idk which one i used, then creating new
> block, the newly created block gets their name messed up and their hud messed up and conflicted and
> replaced with an old deleted block."*

**Symptom** — Delete a block, then create a new one. The new block's **display name** (on the item and
on the placed block) and its **HUD** read wrong: scrambled, "conflicted," or showing the **old deleted
block's** identity instead of the new one. Seen on the **dedicated server**. Developer ~90% sure the
delete was via the **Deleter tool** (confirmed in code = the worst path); rejoin "probably still broken"
(→ points at the deeper data collision, not just a stale display — **needs in-game verify**).

**This is the NAME/HUD facet of the G06-2/G06-3 slot-reuse root** + the G05-1 resync gap — the
previously un-itemized "much worse stuff" G06-3 said the developer would specify later. Same disease,
new symptom surface.

**Why — a block's whole identity is just its slot index.** A placed or held block carries **only**
`slot_N` (the registry block). Its name/HUD/texture are a pure lookup on that index — there is no
per-block name stored on the placement:

| Layer | How the name/HUD is resolved | Source |
|---|---|---|
| Placed block & held item name | `SlotBlock.resolveName(slotIndex, slotKey, fallback)` | `SlotBlock.java:64` / `:221` / `:237` |
| Server JVM / singleplayer | `SlotManager.getBySlot("slot_N")` → live, correct | `SlotBlock.java:65` |
| **Dedicated client** (SlotManager empty) | `CLIENT_NAME_RESOLVER` → `ClientSlotCache.getEntry(idx)`, keyed by **slot index** | `SlotBlock.java:56,67` · `ClientSlotCache.java:111` |
| HUD overlay | reads the aimed `SlotBlock`'s index → same `ClientSlotCache` entry | `HudRenderer.java:76-83` |

So whatever slot `N` currently maps to **is** the block, everywhere. Now the two roots collide on that index:

**Root A — slot index reuse (DATA collision; this is the "still broken after rejoin" half).**
`SlotManager.delete()` frees `slot_N` but does **not** guard it (no `RetiredSlots`; that set is
Arabic-only — `SlotManager.java:113`). `nextFreeSlotIndex()` then returns the **lowest free** index, so
the next create grabs **`slot_N` again** (`SlotManager.java:319`). The new block B is now `slot_N` — and
any still-placed copies of the deleted block A (also `slot_N`) **instantly become B**: same name, same
HUD, same texture. There is no longer any way to tell A's old placements from B; they are the same
registry block. Rejoin reloads the same collided data from disk → **stays wrong** (matches developer's
"probably still broken"). *Creating without deleting first does NOT collide (a fresh index is handed
out) — same finding as G06-3.*

**Root B — the client never re-learns the mapping (DISPLAY staleness; the "needs rejoin" half).** On a
dedicated server the name/HUD come from `ClientSlotCache`, which only updates when a `HudSyncPayload`
arrives. But:

| Delete path | Sends HudSync? | Effect |
|---|---|---|
| **Deleter tool** (`DeleterItem.act`) | ❌ **none** (verified — only `updatePack()`, `DeleterItem.java:40-52`) | Client cache still maps `slot_N` → old block A. |
| `/cb delete` command | ⚠️ actor only (`DeleteCommands.java:108`) | Only the deleter's own client updates; other players stay stale. |
| `/cb create` | ⚠️ actor only (`syncHud(src)`, `CreationCommands.java:157`) | Same — MP viewers don't refresh. |

So after a **tool** delete the client's `slot_N` still says "A"; when B reuses `slot_N`, the new block
reads back as **A** (or flickers A↔B as partial syncs/pack reloads land). That's the "name messed up /
shows the old deleted block."

**Net:** Root A makes B and A's placements physically share one slot; Root B means the client may even
show the *old* name on top of that. Either way the new block's identity is not its own.

### Fix direction (brainstorm — NOT built)

Shares both existing fix rails; nothing here is new architecture:

1. **Guard the freed slot against instant reuse** (the core, shared with **G06-2 #3 / G06-3 #1**): a
   light **reuse-only guard** so `nextFreeSlotIndex` skips a just-freed index while fresh ones remain.
   **Not** `RetiredSlots` (that air-cleans placements → would vanish the developer's placed blocks).
   This alone stops B from ever landing on A's index → no name/HUD collision.
2. **Resync after every edit, to everyone** (shared with **G05-1 #1**): one `AfterEdit.broadcast`
   choke-point (HudSync to **all** online players + `updatePack`) that **tools** call too — so the
   Deleter and create both refresh the cache, on the actor and on other players.
3. **Verify in-game whether anything is STILL wrong after rejoin** once #1 lands — if yes, that residue
   is pure Root A corruption already on disk (may need a one-time integrity sweep); if no, it was Root B
   display staleness. (Developer's "probably still broken" makes #1 the load-bearing fix.)

| Fact | Detail |
|---|---|
| Block identity | placement carries only `slot_N`; name/HUD = lookup on that index (`SlotBlock.resolveName`) |
| Dedicated name source | `ClientSlotCache` keyed by slot index, fed only by `HudSyncPayload` |
| `delete()` slot guard | **none** — frees `slot_N` raw (`SlotManager.java:113`); `RetiredSlots` is Arabic-only |
| Reuse rule | `nextFreeSlotIndex` returns lowest free → reuses just-freed index (`SlotManager.java:319`) |
| Deleter tool resync | **zero** HudSync (verified, `DeleterItem.java:40-52`) |
| Command resync | actor-only, both delete (`:108`) and create (`syncHud`, `:157`) |
| Rejoin | reloads same data → collided identity persists (needs in-game confirm) |
| Create-without-delete | does NOT collide (fresh index) — corruption needs a prior delete |

| Group | Scope | Status |
|---|---|---|
| G05 — Sync/Pack | Both — slot collision is environment-independent; client-cache staleness bites on dedicated | 🔍 diagnosed — two roots (slot-reuse guard + resync-to-all); rejoin-persistence to verify |

**Related:** **G06-2 / G06-3** (same slot-reuse root — the guard fix is shared) · **G05-1** (same resync
gap — the `AfterEdit.broadcast` choke-point is shared) · **G06-5** (variant naming — different, name is
*stored* there; here the name is collateral of the index collision)
**Touches:** `SlotManager.delete` / `nextFreeSlotIndex` (+ reuse-only guard) · new `AfterEdit.broadcast`
(HudSync-to-all + `updatePack`) · `DeleterItem.act` (call it) · `DeleteCommands` / `CreationCommands`
(route through it) · `ClientSlotCache` / `HudSync` (unchanged — they're correct once fed) · `SlotBlock.resolveName` (the index→name seam, for reference)
**Verify in-game:** (1) does the new block show the OLD name immediately, and (2) does a full rejoin heal
it → tells us Root B (display) vs Root A (data) share.

---

## G05-3 · Random face-swap — concurrent client pack writes corrupt the loose pack

> 🔍 diagnosed — standalone; fix is 2 changes in 1 file; high priority (corrupts visible textures in-game)

> *"RANDOMLY when creating or bundling or whatever blocks, their texture in game sometimes get messed up,
> and a face of that block just get swapped randomly to another random block for no reason even tho i did
> nothing... NO I DONT KNOW WHEN IT HAPPENS OR WHY OR HOW TO EVEN REPLICATE, i need ur absolute help on
> this cz its very stupid and can ruin a LOT"*

**Symptom** — On the modded host client (single-player / LAN host), a block randomly shows a completely
wrong texture on one face — looks like a different block's texture, on a face that was never explicitly
painted. Happens intermittently during rapid operations (create multiple blocks, color variants, retexture
quickly). Cannot be reproduced on demand. No in-game error or log warning.

**Root cause confirmed from code inspection, 2026-06-25.** No fix exists yet.

### Root cause — three interlocking races in `client/ResourcePackGenerator.java`

`ResourcePackGenerator.regenerate()` spawns a **new background thread for every `RegenPackPayload`** with
**no guard** against concurrent writes. When rapid operations fire multiple `updatePack()` calls in quick
succession, two (or more) threads run `writeLoosePack()` simultaneously on the same
`resourcepacks/CustomBlocks/` directory.

**Race 1 — model file overwrite.** Both T1 and T2 write `assets/customblocks/models/block/slot_N.json`
for the same slot. T1 may have seen `hasAnyFace(N) = true` at snapshot time and written a per-face model;
T2 may have seen `hasAnyFace(N) = false` and overwritten it with a cube_all model — or vice versa.
Whichever thread writes last wins. The surviving model may not match the face texture files on disk.

**Race 2 — `deleteStale()` cross-deletion.** Each thread calls `deleteStale(packRoot, packRoot, written)`
at the end of its pass. T1's `written` set does NOT include face texture files that only T2 wrote (T1's
pack snapshot didn't have that face override). T1's `deleteStale()` **deletes those face files** while
T2's per-face model still references them. On the next `reloadResources()`, the model's face reference
points to a now-missing file — Minecraft substitutes a fallback, which can be another slot's texture or
the wrong base texture entirely.

**Race 3 — non-atomic `Files.write()`.** The current write is `Files.write(dest.toPath(), data)` — NOT
atomic (truncates the file to 0 bytes FIRST, then writes the new bytes). If Minecraft's
`reloadResources()` reads a texture file mid-write (during the truncate-then-write window), it gets an
empty or partial PNG → broken texture.

**Why it looks like "another block's texture":** The (model, texture file) pair ends up inconsistent
across the two racing writes. A per-face model may reference a face file whose bytes are stale from a
previous operation, or the cube_all model may use a base texture that was partially overwritten — both
surface as the wrong block's appearance on one face.

### Fix (designed, NOT built — 2 changes, 1 file)

Both changes go in `client/ResourcePackGenerator.java`:

**Fix 1 — `writeInFlight` guard.** Add a new `AtomicBoolean writeInFlight`. In `regenerate()`, CAS it
before spawning the thread — same pattern as the existing `reloadInFlight` guard for reloads. If a write
is already running, coalesce the incoming hash as `pendingHash` / `hasPending = true` and return. When
the write finishes (in the `finally` block that fires `applyReload()`), `writeInFlight` clears on the
client thread; `applyReload()` already drives the `hasPending` → `regenerate(client, pendingHash)` cycle.
Result: only one `writeLoosePack()` runs at a time; all subsequent requests queue as pending and execute
sequentially after the current write+reload cycle.

**Fix 2 — atomic writes in `writeLoosePack()`.** Replace:
```java
Files.write(dest.toPath(), data);
```
with:
```java
File tmp = new File(dest.getParentFile(), dest.getName() + ".tmp");
Files.write(tmp.toPath(), data);
Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
```
Mirrors the pattern `TextureStore.saveFace()` already uses. Eliminates the 0-byte truncation window that
`reloadResources()` could read mid-write. Requires adding `import java.nio.file.StandardCopyOption`.

### Facts

| Fact | Detail |
|---|---|
| Affected path | `client/ResourcePackGenerator.java` — modded host only (singleplayer + LAN host) |
| Trigger | Any rapid sequence of `ResourcePackServer.updatePack()` → multiple `RegenPackPayload` → multiple `regenerate()` → concurrent threads |
| Existing guard | `reloadInFlight` guards `reloadResources()` BUT NOT the file-write phase — write and reload are separate steps |
| `Files.write()` atomicity | NOT atomic: truncates first (0-byte window), then writes content |
| `TextureStore.saveFace()` | Already uses atomic tmp + ATOMIC_MOVE — fix mirrors that |
| Fix scope | ~10 lines changed, 1 import added, 1 file only |
| Dedicated server path | Unaffected — dedicated server uses `ResourcePackServer` (HTTP ZIP); `ResourcePackGenerator` is modded-client-only |

| Group | Scope | Status |
|---|---|---|
| G05 — Sync/Pack | Client-only (modded host) | 🔍 diagnosed — root cause confirmed from code 2026-06-25; fix designed, NOT built |

**Related:** `client/ResourcePackGenerator.java` (the broken file) · `ServerPackGenerator.emit()` (called by both threads — read-only, not the problem) · `TextureStore.saveFace()` (already uses the correct atomic write pattern — mirror it) · G05-1 / G05-2 (different client-sync roots, same G05 home)
**Touches:** `client/ResourcePackGenerator` — new `writeInFlight AtomicBoolean` · `regenerate()` guard (CAS + pending coalesce) · `writeLoosePack()` atomic file writes (tmp + ATOMIC_MOVE) · `import java.nio.file.StandardCopyOption`
**Verify in-game:** (1) create 5+ blocks rapidly — no face shows a wrong texture on any of them; (2) `/cb retexture` 3 blocks within 1 second — all update to their correct texture; (3) check `latest.log` — only ever ONE `CustomBlocks-ClientPackGen` thread active at a time between pack writes.

---

## G05-5 · Per-client low-res texture mode — weak-GPU "resource reload failed" (atlas overflow)

> 🟡 **feature, spec-locked by owner, NOT built.** Standalone; all changes client-side; server + the 512px pack for every other player stay untouched. Test = `GROUP_05_TESTING_GUIDE.md` **§E**.

> *"my friend is getting resource reload failed for customblocks mod"* … *"i want them 512 tho and my other friends are good with it, can it be a fix for him alone"*

**Symptom** — One friend on a weak GPU (**NVIDIA GeForce GT 730**) gets **"resource reload failed"** joining the dedicated server; his client then drops **all** resource packs, so every custom block is magenta/gone. Every other player (and the owner) loads the same pack fine. Owner wants to keep **512px for everyone else**.

### Root cause (proven from his `latest.log`, 2026-07-06)

Block-atlas overflow, not a corrupt file:
```
Caught error loading resourcepacks, removing all selected resourcepacks
Caused by: net.minecraft.class_148: Stitching
Caused by: net.minecraft.class_1054: Unable to fit: customblocks:block/slot_1973 - size: 512x512 - Maybe try a lower resolution resourcepack?
```
Config `textureSize=512`, ~1600 blocks. The **static** (on-atlas `cube_all`) slot textures stitch onto `minecraft:textures/atlas/blocks.png`; their combined area exceeds the friend's GPU `GL_MAX_TEXTURE_SIZE` → the stitcher throws → MC removes every pack. Owner's client GPU is larger (fits it); the Docker server never renders an atlas. Anomaly seen: `slot_1281` is 4088×3796 (one rogue oversized static texture). Client-only tweaks (Mipmap 0, force dedicated GPU, Sodium reinstall) were tried and **all failed** — it's a hardware ceiling. Animated GIF / Arabic blocks render **off**-atlas (`invisibleBlockModelJson`) so they are NOT part of the stitch crash, but their large sheets still eat his limited VRAM.

### Chosen fix (owner-locked, NOT built) — client-side downscale, per client, manual

The weak client does the shrinking; the server and `emit()` single-source-of-truth are untouched, so no other player is affected.

1. **Trigger:** client command **`/cblowres [256|128|off]`** — its OWN root, NOT a `/cb` subcommand. Friend is **not** an op → it MUST be a client-side command (Fabric `ClientCommandManager`, no permission), separate from the op-gated server `/cb` tree. ⚠️ **Collision lesson (fixed 2026-07-07):** it was first built as `/cblowres`. Registering ANY client-side child under the `cb` literal makes Fabric's client dispatcher OWN the whole `/cb` root and reject EVERY other `/cb …` server command client-side with "Incorrect argument for command at position 3" *before it reaches the server* (Brigadier finds no client match → `dispatcherUnknownArgument`, which Fabric does not forward on). Moved to its own `/cblowres` root — the server `/cb` tree is left completely untouched.
2. **Size:** friend picks **256** (try first) or **128** (if 256 still overflows the GT 730). `off` = full 512.
3. **Apply instantly:** on any change → wipe the local loose pack, **re-download the full pack from the server**, downscale each PNG to the chosen size, then one silent `reloadResources()`. No local full-res cache (re-pull each change — his bottleneck is GPU, not bandwidth).
4. **Scope of shrink:** **all** block textures to the chosen size — static/on-atlas (THE crash fix) **and** off-atlas GIF + Arabic sheets (VRAM relief so his card doesn't OOM later).
5. **Persist** the choice to a client config; re-apply on join. Smooth/area-average downscale.

### Facts

| Fact | Detail |
|---|---|
| Trigger of crash | on-atlas static `cube_all` textures overflow the client GPU's max atlas size (`class_1054: Unable to fit`) |
| Environment | dedicated server, remote **modded** client — the `ClientPackReceiver` sync path (G05-4) |
| Why friend-only | atlas cap = client `GL_MAX_TEXTURE_SIZE` (hardware); owner GPU bigger, server headless |
| Off-atlas blocks | animated / Arabic use `invisibleBlockModelJson` → not in the crash, but consume VRAM |
| Client tweaks | Mipmap 0 / force GPU / Sodium reinstall — all tried, all failed (hardware wall) |
| Global fallback (rejected) | `/cb config texturesize 256` + `/cb retextureall 256` fixes it but drops 512 for everyone |
| Server-side per-client pack (rejected) | heavier, touches the working sync pipeline |

| Group | Scope | Status |
|---|---|---|
| G05 — Sync/Pack | Client-only (weak-GPU remote modded client); opt-in | 🟡 spec-locked by owner 2026-07-06, NOT built |

**Related:** `client/packsync/ClientPackReceiver` (where the downscale + diff-fix live) · `ServerPackGenerator.emit()` / `network/packsync/PackManifest` (512 source of truth — unchanged) · G05-4 (the sync path this rides on) · `ImageProcessor` (reuse its scaling if client-safe) · the global `RetextureAllCommands` / `texturesize` levers (the rejected everyone-drops-to-256 fallback)
**Touches:** new client config field (`lowres` size / off) + persistence · new client command `/cblowres [256|128|off]` (`ClientCommandManager`, no op) · `ClientPackReceiver.writeFile` (downscale PNGs incl. off-atlas sheets when on) · `ClientPackReceiver` diff (track the **server** manifest sha1, not the on-disk hash — else every shrunk file mismatches the 512 manifest and re-downloads each join) · on size/off change: force full re-pull + reload · a downscale helper
**Verify in-game:** (1) GT-730 friend runs `/cblowres 256`, rejoins → NO "resource reload failed", blocks render; (2) if 256 still fails, `/cblowres 128` loads; (3) `/cblowres off` re-pulls full 512 + reloads; (4) other players + owner still see full 512; (5) setting survives relaunch; (6) non-op friend can run the command with no permission error.

---

## G05-6 · Live in-place texture swap — kill the full reload on a pixel-only edit ⭐ (the "reload freezes every edit" pain) · **PHASE 1 BUILT 2026-07-16 — awaiting owner confirm · PHASE 2 DEFERRED (API confirmed, spec below)**

### Locked decisions (owner, 2026-07-16 — build BOTH phases together when green-lit)

- **B3 scope:** live-upload fixes Corrupt PNG for **retexture-only bursts on existing blocks** (the realistic
  real-world case). A burst that includes `/cb create` (a brand-new block) is still a structural edit → still
  takes the old full-reload path → still carries B3's old (rare) risk. This is NOT claimed fixed and is not
  in scope here; document as a known remaining edge once built.
- **Live-upload failure (Phase 2 sprite upload fails mid-edit):** fall back silently to the existing full
  `reloadResources()` path — no player-facing message, no chat spam. Log ONE line server-side
  (`LOGGER.warn`, include the slot id) so it's traceable in the log if it happens a lot. Never crash.
- **Multiplayer scope:** live pixel push broadcasts to **every connected client**, not just the editor —
  everyone standing nearby sees the block update instantly, same as the editor does. Needs the update to ride
  the existing per-slot sync payload (or a new lightweight one) to all players, not just a local-client texture
  poke.
- **Undo interaction:** `/cb undo` on a retexture that was live-swapped must **live-revert instantly** using
  the same live-upload path in reverse (previous slot bytes), not a full reload — consistency with how the
  edit itself felt.
- **Build order:** G05-6 ships **before** G05-7 — bigger win, touches the riskier atlas code, get it stable
  first. Build **incrementally**: Phase 1 (off-atlas + icons) verified working with no regression to existing
  §A/§B/§C/§E/§F behavior before starting Phase 2 (atlas sprite upload), even though owner approved both
  phases in one pass.

> Owner's #1 pain: every create/edit/retexture triggers `client.reloadResources()`, freezing the game
> for seconds to rebuild **everything** (all models, full block-atlas re-stitch, sounds, shaders, lang) —
> when only ONE block's pixels changed. This entry designs a fast path that pushes the new pixels straight
> to the GPU and skips the reload for pixel-only edits. Structural edits (new block, shape/model change,
> add/remove face) still take one real reload — rare and acceptable.

### Root cause (why it freezes)

`client/ResourcePackGenerator.java` funnels **every** edit path through one chokepoint:
`regenerate()` → `writeLoosePack()` (rewrites the whole loose pack from `ServerPackGenerator.emit`) →
`applyReload()` → **`client.reloadResources()`** (`ResourcePackGenerator.java:135`; mirrored in
`client/packsync/ClientPackReceiver.java:256`). `reloadResources()` is the nuclear option — it re-runs
every reload listener: model bake, block-atlas stitch, sound load, shader recompile, lang. Cost is
independent of how little changed, so a 1-pixel recolour pays the full multi-second stall.

The regen does **not** track which slot changed — it always rebuilds all files, then reloads all. But the
write pass (`writeLoosePack`) is exactly where the change set is knowable: diff each new file's bytes
against the copy already on disk before overwriting.

### Two render paths (decides difficulty) — CORRECTED 2026-07-15 after code sweep

Custom blocks split into two GPU paths. **Verified against `ServerPackGenerator.emit` (`:100-165`) — an
earlier draft of this doc was wrong** and claimed the common static block renders off-atlas; it does NOT.
Which model each slot gets (source of truth):

| Slot kind | World block model | World render | Item icon |
|---|---|---|---|
| Animated (grid) | invisible (no `parent`) | **off-atlas** (`AnimSlotBER` + `AnimFrameCache`) | off-atlas (`builtin/entity` → `SlotItemRenderer`) |
| Arabic letter/number | invisible (no `parent`) | **off-atlas** (`AnimSlotBER` + `StaticFrameCache`) | off-atlas (`builtin/entity`) |
| **Common full static** (`/cb create`) | `cube_all` (**has `parent`**) | **ATLAS** — vanilla bakes it | off-atlas (`builtin/entity` → `StaticFrameCache.getIconFallback`) |
| Non-full shape / per-face / cross | shape / `cube` model (has `parent`) | **ATLAS** | atlas (normal baked icon) |

The off-atlas gate is `StaticFrameCache.isOffAtlasModel` = `!model.has("parent")`; `cubeAllJson` sets
`"parent":"minecraft:block/cube_all"` (`ServerPackGenerator.java:244`), so the common static block's WORLD
render is **atlas**, not off-atlas. Only its ITEM icon is off-atlas.

**What Phase 1 (off-atlas re-upload) therefore covers:** live world updates for **animated + Arabic** blocks,
and live ITEM-icon updates for **every** block (all icons route through the off-atlas caches). **What it does
NOT cover:** the WORLD appearance of the common static block, per-face, and shape blocks — those are atlas and
need **Phase 2**. So Phase 1 is still a real win (all icons + all animated/Arabic world blocks, plus it kills
the reload for those edits), but it is NOT "most day-to-day edits" for the common block's in-world look.

### Chosen design (NOT built) — a change classifier + two upload paths, phased

**Shared foundation — change classifier (in `writeLoosePack`).** Before overwriting each emitted file,
compare new bytes to the on-disk file. Build two sets: `pixelChangedSlots` (a `slot_N.png` whose bytes
changed while its `.json` model / `.grid` sidecar did NOT) and a `structuralChanged` flag (ANY model /
blockstate / lang / new / deleted file). Pass the result to `applyReload`.

**`applyReload` decision:**
- `structuralChanged == false` **AND** every `pixelChangedSlot` is off-atlas (Phase 1) / any (Phase 2)
  → **skip `client.reloadResources()`**; live-upload each changed slot instead. Instant, no stall.
- otherwise → current full reload path, unchanged (safe fallback).

**Phase 1 — off-atlas live upload (low risk, do first).** Covers animated + Arabic WORLD blocks and ALL item
icons (see the corrected table above). For each pixel-changed off-atlas slot: read the freshly-written
`slot_N.png` from the loose folder on disk (NOT via `ResourceManager` — it isn't reloaded; verified both
caches read via `rm.getResource`). API confirmed against **yarn 1.21.1** (and by the mod's own working code):
- Static (`StaticFrameCache`): it currently builds a `NativeImageBackedTexture` then **discards the ref**,
  keeping only the `Identifier` (`StaticFrameCache.java:102-105`). Keep the texture object, then
  `getImage()` → overwrite pixels (`setColor`) from the fresh PNG → `upload()` — the exact pattern
  `AnimFrameCache.blitCell`+`upload()` already uses (`AnimFrameCache.java:138-128`). (`setImage(NativeImage)`
  also exists in 1.21.1 if dimensions change → new image.)
- Animated (`AnimFrameCache`): the `Slot` already holds `frameTex` (`NativeImageBackedTexture`) + the RAM
  `grid`; replace the `grid` from the fresh PNG (targeted rebuild), next `prepare()` re-blits + uploads.

**Phase 2 — atlas sprite live upload (harder; required for the COMMON static block, per-face, shapes).**
API corrected for **yarn 1.21.1** (an earlier draft mis-cited a later version):
- Atlas: `mc.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)` → `SpriteAtlasTexture`.
- Sprite: `atlas.getSprite(Identifier.of("customblocks","block/slot_N"))`. Atlas pixel origin =
  **`Sprite.getX()` / `Sprite.getY()`** (public on `Sprite`, NOT on `SpriteContents`; `getContents()` +
  `getAtlasId()` also public).
- Upload: `SpriteContents.upload(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images)`
  — **package-private in 1.21.1**, so it needs an **accessor/invoker Mixin** (there is also a public
  `upload(int x, int y)` used for animation). **No `GpuTexture` parameter** in 1.21.1 (that param is the
  1.21.5+ render rewrite). The `NativeImage[]` must supply **one image per atlas mip level** (block atlas is
  built with mipmaps) — regenerate the mip chain for the new pixels or MC mis-samples at distance.
- Risks: the sprite must already exist in the current atlas (a brand-NEW block still needs one structural
  reload to get stitched); mip count must match; bind/flush handled by the upload call.

> **Phase 2 — DEFERRED (owner, 2026-07-16): build only AFTER Phase 1 passes in-game.** Reason: the atlas
> GL/mip upload cannot be verified at build-time and a wrong mip chain smears the *shared* block atlas for
> every player, so it is gated behind Phase 1's live confirmation. **API re-confirmed via `javap` against
> this project's yarn 1.21.1 (2026-07-16) — ready to build, no re-derivation needed:**
> - `net.minecraft.client.texture.SpriteContents#upload(int,int,int,int,net.minecraft.client.texture.NativeImage[])`
>   — **package-private** → needs an `@Invoker` mixin on `SpriteContents` (pattern: `mixin/ScreenInvoker.java`).
> - `SpriteContents#mipmapLevelsImages` (the `NativeImage[]`) — **package-private field** → `@Accessor` mixin,
>   OR rebuild the chain yourself with the **public** `SpriteContents#generateMipmaps(int levels)`.
> - `Sprite#getX()` / `Sprite#getY()` / `Sprite#getContents()` — all **public** (atlas pixel origin + contents).
> - `SpriteAtlasTexture#getSprite(Identifier)` + `SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE`; atlas via
>   `BakedModelManager#getAtlas(Identifier)` (`mc.getBakedModelManager()`).
> - Wire into the SAME classifier already built: when `structuralChanged==false` and a pixel-changed slot is an
>   ATLAS world block (common static / per-face / shape), route it to the atlas-upload path instead of falling
>   back to a full reload. Off-atlas slots keep the Phase 1 path unchanged.

**Rejected alternative — selective resource reload.** Forge has `ISelectiveResourceReloadListener` +
`Predicate<IResourceType>` to reload only the TEXTURES type and skip models/sounds/lang. **Fabric has NO
built-in equivalent** (confirmed — it's a Forge-only config), and even on Forge it still re-stitches the
whole atlas. Direct per-texture GPU upload is strictly better on Fabric and is the standard live-edit
approach (same as the community "live texture editor" mods). Not pursuing selective reload.

### Bonus — this obsoletes open bug B3

B3 (fast SP create/retexture burst → multiple reloads + `Corrupt PNG`) is a **reload-time** race: a
`reloadResources()` reads a `slot_N.png` mid-write. A pixel-only edit that skips `reloadResources()`
entirely removes the race for the common case, shrinking B3's surface to structural-edit bursts only.

### The "reload on first join" — log analysis (2026-07-15, ~400 logs)

Owner also reports a reload when joining. Traced from `.minecraft/logs`. It is **two separate reloads**, only one join-related:

1. **Boot reload (once at launch).** MC's normal startup reload of ALL enabled packs — `file/CustomBlocks`
   is listed alongside every other pack (Clean Connected Glass, Dramatic Skys, all mods). Latest session:
   the ONLY reload was this one (`03:15:06`), then joined `yoyoo.mcsh.io` at `03:16:23` with NO extra reload.
   **Not the mod's doing** — every resource pack triggers it; not removable without breaking other packs.
2. **Join reload (conditional).** `ClientPackReceiver.finalizeDone` (`:238`) reloads ONLY when the server's
   manifest differs from what's on disk (`dirty == true`). If nothing changed → `"Pack already current
   (hash …), no reload."` and it is skipped — already optimized. A join reload therefore means the client
   genuinely downloaded new/changed files (blocks created/edited server-side since last visit).

**Log evidence:** recent sessions (07-08 → 07-15) mostly show `alreadyCurrent` hits and no join reload;
join reloads correlate with real server-side pack changes, not every join.

**What G05-6 can remove:** a first-EVER join (missing hundreds of files + models/blockstates) still needs
one real reload — structural, unavoidable. A later join where only **textures** changed can route through
G05-6's live-upload fast path instead of `reloadResources()` (same classifier: `dirty` files that are all
pixel-only → live-upload; any structural → reload). Boot reload stays (vanilla, out of scope).

### Facts

| Fact | Value |
|---|---|
| MC / loader | 1.21.1 Fabric |
| Chokepoint | `client/ResourcePackGenerator.java` `regenerate → writeLoosePack → applyReload → reloadResources` (`:135`) + `ClientPackReceiver.java:256` (join sync; gate at `:238`) |
| Boot reload | vanilla MC startup reload of all packs — out of scope, not removable |
| Join reload | `ClientPackReceiver.finalizeDone` only when `dirty`; skipped when hash matches. First-ever join = unavoidable; texture-only later joins = G05-6 candidate |
| Coverage (Phase 1) | live WORLD update for animated + Arabic blocks; live ICON update for ALL blocks |
| Coverage (Phase 2) | live WORLD update for the common static block, per-face, shapes (atlas sprites) |
| Off-atlas API (Phase 1) | keep `NativeImageBackedTexture` ref → `getImage()` + `setColor` + `.upload()` (proven in-code, `AnimFrameCache.blitCell`); `setImage(NativeImage)` if size changes. 1.21.1 ✓ |
| Atlas API (Phase 2) | `mc.getBakedModelManager().getAtlas(BLOCK_ATLAS_TEXTURE).getSprite(id)`; origin = `Sprite.getX()/getY()`; `SpriteContents.upload(x,y,skipPx,skipRows,NativeImage[] mips)` — **package-private → accessor Mixin**; mips = one per atlas level; **no GpuTexture in 1.21.1** |
| Structural edits | still take one full `reloadResources()` — unavoidable, rare |
| Reload state | 🟢 **Phase 1 BUILT** 2026-07-16 (off-atlas world = animated + Arabic; all item icons; `/cb undo` live-reverts via same classifier; MP = each client applies its own streamed change) — compiles clean, awaiting owner in-game confirm. **Phase 2 DEFERRED** (atlas sprite upload) until Phase 1 passes — API confirmed above. B3 edge: `/cb create` bursts still take the old full-reload path (known, not claimed fixed). |

**Related:** `client/ResourcePackGenerator.java` (chokepoint — add classifier + fast path) · `client/render/StaticFrameCache.java` (discards its tex ref today — `:102-105`; must keep it) + `AnimFrameCache.java` (`Slot` already holds `frameTex`+`grid`) · `client/render/AnimSlotBER.java` (draws off-atlas, unchanged) · `network/ServerPackGenerator.java:100-165` (model selection — the source proving which slots are atlas vs off-atlas) · B3 (this shrinks its surface) · G05-3 (atomic loose-writer — the classifier reads the same on-disk files)
**Touches (when built):** `writeLoosePack` (byte-diff classifier, per-slot + structural flag) · `applyReload` (skip-reload decision + live-upload dispatch) · `StaticFrameCache` (keep the `NativeImageBackedTexture` ref + `reupload(slot)` reading the PNG from the loose folder) · `AnimFrameCache` (targeted `grid` replace per slot) · **Phase 2 only:** a `SpriteContents.upload` accessor/invoker Mixin + block-atlas sprite lookup + mip-chain rebuild
**Verify in-game (when built):** (1) **Phase 1** — edit an ANIMATED or ARABIC block's texture → placed blocks update **instantly, no freeze, no reload line**; (2) **Phase 1** — edit ANY block → its hand/inventory ICON updates instantly; (3) rapid burst of pixel edits → each lands, no stall, no `Corrupt PNG` (B3); (4) create a NEW block or change a SHAPE → still one silent reload (structural, expected); (5) **Phase 2** — edit a COMMON static / per-face / shape block → its placed-in-world atlas sprite updates live, no re-stitch stall; (6) MP: owner edit still propagates to other players correctly.

---

## G05-7 · Skip the redundant first-join reload (singleplayer/integrated) — persist the applied hash · **BUILT 2026-07-16 — awaiting owner confirm**

### Locked decisions (owner, 2026-07-16)

- **Hash file scope changed to PER-WORLD (safer), not the global `resourcepacks/CustomBlocks/.cbpackhash`
  originally sketched below.** Store it inside the world's own save folder (e.g.
  `saves/<world>/customblocks/.cbpackhash` for SP, or the equivalent per-level data folder) — a
  fresh/different world always starts with no hash and forces its first reload exactly like today; only a
  world already opened with this exact pack skips it. Rewrite the "Chosen design" section below's file path
  accordingly when building.
- **Trust model:** never trust the hash file blindly. On ANY doubt — missing, corrupted/unreadable, mismatched
  pack folder (empty or `pack.mcmeta` absent), or a mismatched mod version (see next bullet) — force a full
  reload rather than risk stale/missing textures. The existing "safety gate" idea below already covers the
  empty-folder case; extend it to also treat a corrupt/unreadable hash file as "no hash" (force reload), not
  as an error.
- **Write path must be crash-safe:** the sidecar write itself must never be able to corrupt or crash — write
  to a temp file then atomic-rename over the real one (same pattern as the mod's existing atomic loose-writer
  from G05-3), so a mid-write crash/power-loss leaves either the old valid file or nothing, never a half-written
  one that could be misread as valid.
- **Mod version invalidation:** bundle the mod's version string into what's checked, alongside the pack hash.
  If the mod jar was updated since the hash was written, force one reload even if the pack-content hash still
  matches — treat a mod update the same as any other real pack change.
- **Build order:** after G05-6 is stable (see its locked decisions above).

> Owner's "first boot reload" pain. On EVERY session's first world-open (singleplayer / LAN host), the game
> reloads once even when nothing changed since last session — a wasted reload, because Minecraft already
> loaded that exact pack at boot. This entry removes it, safely, by remembering on disk which pack was last
> applied. (The vanilla boot reload itself is out of scope — see the log analysis under G05-6; it's normal
> MC startup and not removable.)

### Root cause (proven from code + logs)

Two reloads happen at the start of a singleplayer session:
1. **Boot** — MC loads all packs including `file/CustomBlocks` (it's in `options.txt`). Vanilla, unavoidable.
2. **First world-open** — the integrated server fires `RegenPackPayload` → `CustomBlocksClient` receiver →
   `ResourcePackGenerator.regenerate(client, hash)`. Its skip guard is
   `if (hash != null && hash.equals(lastAppliedHash))` (`ResourcePackGenerator.java:56`), but
   **`lastAppliedHash` is an in-memory `static volatile` that is `null` at every launch** (`:46`). So the
   guard never matches on the first open of a session → it rewrites the loose pack + calls
   `reloadResources()` — even though the on-disk pack MC just loaded at boot is byte-identical.

The **dedicated-server path already avoids this**: `ClientPackReceiver.finalizeDone` diffs against the
on-disk `localCache` and logs `"Pack already current (hash …), no reload."` (`:238-243`). The integrated
path simply has no equivalent disk memory.

### As built (2026-07-16) — per-world sidecar, seeded before the guard

- Sidecar path is **PER-WORLD**: `<worldSave>/customblocks/.cbpackhash`, via
  `client.getServer().getSavePath(WorldSavePath.ROOT).resolve("customblocks/.cbpackhash")`. A remote/dedicated
  connection has no local `getServer()` → `sidecarFile` returns null → no seed, no write (the MP manifest-diff
  path handles that case), so this change is SP/integrated-host only, exactly as scoped.
- Two lines: `hash\n<modVersion>\n`, written **atomically** (temp + `ATOMIC_MOVE` rename) — the same crash-safe
  pattern as `writeAtomic`.
- `regenerate` calls `seedForWorld(client)` **before** the `hash.equals(lastAppliedHash)` guard. `seedForWorld`
  re-seeds on a world switch (tracks `seededWorld` path; a different world first **forgets** the old hash so a
  stale value can never wrongly skip).
- **Never trust the hash blindly** (`readSidecarHash`): returns the stored hash only when the sidecar exists,
  `pack.mcmeta` is still on disk (`packLooksValid`), the stored mod version equals the running one, and the
  hash is non-empty — ANY failure → null → normal write+reload.
- `writeSidecar` runs at all three apply outcomes (no-change / live-swap / full-reload completion), best-effort
  (a write failure is logged + swallowed; worst case = one extra reload next open).
- **deleteStale is a non-issue as built**: the sidecar lives in the world save folder, *outside* the
  `resourcepacks/CustomBlocks/` tree `deleteStale` walks — it can never be swept. (The original draft's
  "exclude `.cbpackhash` from delete" note only applied to the rejected in-pack location.)

### Why it's safe (nothing lost, no missing textures)

The reload is skipped **only** when the requested hash equals the hash of the pack already written to disk,
which is exactly the pack Minecraft stitched into its atlas at boot. If the world's data changed since last
session (any create/edit/retexture), the hash differs → full reload runs as today → new textures load. A
missing/emptied pack folder also forces the reload via the safety gate.

### Facts

| Fact | Value |
|---|---|
| Trigger | `RegenPackPayload` → `ResourcePackGenerator.regenerate` on integrated-server world-open |
| Bug | `lastAppliedHash` in-memory only (`ResourcePackGenerator.java:46`), null each launch → guard at `:56` never matches first-open |
| Precedent | dedicated path already skips via on-disk diff (`ClientPackReceiver.finalizeDone:238`) |
| Fix | persist applied hash + mod version to **per-world** `<worldSave>/customblocks/.cbpackhash` (atomic); seed `lastAppliedHash` from it per world-open, validated |
| Out of scope | the vanilla boot reload (all packs, MC startup) — not removable; remote/dedicated joins (no local world save → MP manifest-diff path unchanged) |
| Reload state | 🟢 **BUILT** 2026-07-16 (per-world atomic sidecar, mod-version-bundled, never-trust-blindly, deleteStale-immune by location) — compiles clean, awaiting owner in-game confirm |

**Related:** `client/ResourcePackGenerator.java` (`regenerate` guard + `applyReload` completion) · `client/packsync/ClientPackReceiver.java:238` (the dedicated path this mirrors) · `network/payloads/RegenPackPayload` (the trigger) · G05-6 (same file; independent change) · deleteStale / writeAtomic (sidecar must survive `deleteStale` — exclude `.cbpackhash` from the delete pass)
**Touches (when built):** `regenerate` (seed `lastAppliedHash` from sidecar on first call) · `applyReload` completion (write sidecar) · `writeLoosePack`/`deleteStale` (never delete `.cbpackhash`) · a small read/write helper
**Verify in-game (when built):** (1) launch, open the same SP world with NO changes since last session → NO reload overlay, blocks all present + correct; (2) create/edit a block, relaunch, open world → ONE reload (data changed, expected), edit is visible; (3) delete the `.cbpackhash` (or the loose folder) → next open forces a full reload, nothing missing after; (4) dedicated server join → unchanged behaviour (still uses the manifest-diff path).
