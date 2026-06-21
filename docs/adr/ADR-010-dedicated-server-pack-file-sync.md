# ADR-010: Dedicated-server pack delivery via file-level sync

Date: 2026-06-20
Status: Accepted

## Context

Group 05's modded-client delivery rebuilds the resource pack **locally** from the client JVM's own
`SlotManager`/`TextureStore` (`RegenPackPayload` → `client/ResourcePackGenerator`). That is only correct
when the client and server share a JVM (single-player / integrated host / LAN). On a **remote dedicated
server** the client's local slot data is stale: blocks created on the server after the client loaded its
own `slots.json` are absent locally, so those slots emit the `empty_slot` model and render as the
magenta/black missing-texture checkerboard. Owner-confirmed symptom: only **newly-created** blocks are
magenta; blocks already in the client's local data render fine. (Root cause proven from the owner's
`latest.log`, 2026-06-20 — see `docs/Finale Fix/GROUP_05_RESOURCE_PACK.md`.)

The old project avoided this by streaming every texture's bytes to each client over its own network
channel — but that code crammed texture+meta+faces+variants+anim into one payload and **mutated client
slot state**, which is where its bugs lived. We did not want to copy it.

## Decision

Sync the pack to modded clients on a dedicated server as a **folder of dumb `(path, bytes)` files**,
built entirely server-side from the existing single source of truth, `ServerPackGenerator.emit()`:

1. Server captures one immutable snapshot (`PackManifest`) — `emit()` once → `path → bytes` → `path → sha1`.
2. On a modded client's join (and after any pack change), the server sends the manifest
   (`PackManifestPayload`, gzipped `path\tsha1` lines).
3. The client diffs the manifest against its on-disk loose pack (`resourcepacks/CustomBlocks`) and
   requests only the missing/changed files (`PackRequestPayload`).
4. The server streams those files (`PackFilePayload`, chunked, capped at 256 KB/player/tick) and ends
   with `PackDonePayload`; the client buffers chunks, writes files, deletes any file the manifest no
   longer lists, and does **one** silent resource reload.

Scope gate: file-sync runs **only** when `server.isDedicated()` and the client `canSend` the payloads
(modded). The integrated host keeps its untouched `RegenPackPayload` local regen; vanilla clients on a
dedicated server keep the HTTP path. The client **only writes files** — it never touches its
`SlotManager`/`TextureStore`, so a host's local data can never be clobbered.

## Rationale

- **Single source of truth, zero drift.** All pack-building logic stays in `emit()`; the sync ships its
  output verbatim. No second pack builder to keep in step.
- **General, no open port.** Works on any dedicated server (not just one host) and needs no reachable
  HTTP port — unlike the vanilla HTTP push, whose `httpHost` problem is a separate concern.
- **Cheap rejoins.** The manifest diff means an unchanged rejoin transfers ~nothing; a single new block
  pushes only that slot's files.
- **No state mutation.** Files-only avoids the old project's slot-clobbering bug class entirely.

## Consequences

- The server holds one in-memory pack snapshot (~37 MB for 1000+ blocks) per refresh; acceptable for a
  server, refreshed on change (debounced).
- The client and server share the single `resourcepacks/CustomBlocks` folder with the integrated-host
  path. They never run at the same time, but **alternating** between single-player and a remote server
  rewrites the folder each switch (a full resync). Accepted for simplicity; revisit with a per-server
  folder if it becomes a problem.
- A pack change while a player has a CustomBlocks GUI open is **not** held back for file-sync the way the
  HTTP/regen push is (the client reload could flicker an open screen). Minor; revisit if observed.
- Capture runs `emit()` off-thread (after the zip build, sequentially), same read pattern the existing
  zip rebuild already uses against `SlotManager`/`TextureStore`.

Untested until confirmed in-game on a real dedicated server (G05.7 + G05.8). Build-green is not done.
