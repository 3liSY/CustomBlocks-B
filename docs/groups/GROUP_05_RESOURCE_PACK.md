# Group 05 - Resource Pack Delivery

> Group 05 makes custom-block textures reach every player through the correct delivery path, including a server-forced 128 px path that lets the production modpack join with a 2 GB client heap.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_05.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

CustomBlocks textures must apply quietly and reliably on an integrated host, a remote dedicated server, and the separate vanilla-client path. A player should see the current block texture without manually accepting a pack, rejoining after ordinary edits, downloading a larger texture variant than the server selected for that player, or inheriting stale local data.

The Group owns pack generation, transport, integrity, recovery, application, and the CustomBlocks texture-cache lifecycle. It does not own image editing, tool behavior, or the screens that configure those features.

The weak-client release target is concrete: a client running the real production mod list with `-Xmx2G` must be able to complete a clean first join, apply the server-forced 128 px CustomBlocks pack, enter the world, and remain usable. A smaller pack is not considered successful merely because it downloads; the player must reach gameplay without atlas overflow, out-of-memory failure, reload failure, disconnect, or an unbounded CustomBlocks cache.

## Ownership

| Owns | Does not own |
| --- | --- |
| Pack generation, resolution variants, delivery, modded-client sync, silent application, recovery, and delivery performance | Image creation and color editing: G10 |
| Dedicated-server manifest, changed-file transport, resume, integrity, and application acknowledgement | Tool mutation behavior: G06 |
| Client pack resync and CustomBlocks texture-cache cleanup after a mutation or resolution change | Resource-pack settings screens: G27 |
| Pack protocol compatibility gate and delivery diagnostics | General mod-version policy and release naming: G20 |

## Direction

The server is the pack-content and per-player resolution authority. Each published generation has shared full/512, 256, and 128 variants built once on the server. A player downloads only the selected final variant; a forced-128 client never downloads full 512 textures and never performs the resize locally.

Integrated hosts may regenerate their local loose pack directly. Modded clients on dedicated servers receive a bounded manifest, request missing or changed files, stream verified data to temporary files, and reload once. Vanilla clients keep their separate HTTP delivery path. Client pack application never rewrites local server data.

The current client-only low-resolution implementation is migration input, not the target design. It receives full-resolution bytes before shrinking, queues unbounded work, assembles complete files in memory, and can silently return the original full-resolution PNG when scaling fails. The server-authoritative path must replace that behavior and must not share the `/cblowres` command root with a local-only client command.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-15 | Modded integrated hosts use local loose-pack generation and silent reload. | The host does not rely on an HTTP server push to see its own textures. |
| 2026-06-20 | Dedicated modded clients sync pack files by manifest and changed-file requests. | Pack-building logic remains server-side and remote clients never rebuild from stale local slot data. |
| 2026-06-20 | Dedicated initial sync is throttled and large files are chunked. | A large pack cannot flood a shared host connection in one tick. |
| 2026-07-16 | ~~Pixel-only edits may use live texture upload while structural edits keep one safe full reload.~~ | **Reversed 2026-07-19 — see below.** |
| 2026-07-16 | ~~Integrated first-open reload may be skipped only when a saved applied-pack hash matches.~~ | **Reversed 2026-07-19 — see below.** |
| 2026-07-19 | Resource-pack reloads are mandatory: every real pack change applies through one full `reloadResources()`. | The no-reload fast paths are scrapped and removed from code (live in-place GPU swap; per-world pack-hash sidecar skip). A live GPU swap cannot make an edit correct, and the sidecar skip never worked reliably. Intra-session dedup of a repeated identical regen is kept (not a skipped reload); the dedicated no-change rejoin skip is unaffected. |
| 2026-07-18 | Low-resolution mode is server-authoritative and selected per player. | The selected client receives only a prebuilt 128, 256, or full/512 variant; other players keep their own quality. |
| 2026-07-18 | `/cblowres <128|256|512|off> [playerIGN]` owns the resolution workflow. | No IGN changes the sender; changing another player requires operator permission; `512` and `off` both restore full/default quality. |
| 2026-07-18 | Resolution choices persist on the server and support offline names. | A choice survives server and client restarts and already applies on the target's first connection. |
| 2026-07-18 | Forced 128 is a 2 GB-client release gate, not a best-effort visual preference. | The production pack must pass a clean `-Xmx2G` join and gameplay soak before the feature is Done. |
| 2026-07-18 | Variant publication is transactional and fail-safe. | An invalid new texture uses the last-known-good transformed file or, if none exists, a target-sized missing-texture placeholder; it never falls back to a full-size file for a forced-low-res player. |
| 2026-07-18 | Dedicated transfer is receiver-controlled, bounded, resumable, and verified through application. | The server cannot report completion merely because packets were queued; the client acknowledges write, hash verification, and successful application. |
| 2026-07-18 | The server has a public port reachable from outside the LAN; the hybrid HTTP(S)-first plus in-band-fallback transport is locked as the target §E design. | §E moves from "pending owner lock" to a buildable, locked design. |

## Feature Plan

### A. Delivery Paths

**Player outcome**

Every player receives the right pack update for their environment without an unwanted confirmation dialog, stale texture, or accidental client-side pack generation.

**Experience**

- An integrated host sees generated textures through its local loose pack.
- A modded client on a dedicated server receives only files that are missing or changed in its selected resolution variant.
- A vanilla client keeps the HTTP resource-pack path.
- CustomBlocks pack application stays silent for the intended CustomBlocks pack route.

**Requirements**

- `ServerPackGenerator.emit()` remains the logical single source of pack contents.
- One generation operation emits the full/512 source plus 256 and 128 variants; it must not independently build a ZIP and then recapture a different in-memory manifest.
- A dedicated manifest maps normalized pack paths to sizes and hashes and identifies the selected variant and generation.
- A client compares that manifest with `resourcepacks/CustomBlocks`, requests only differences, writes verified files, and reloads once after the completion boundary.
- A server pushes only affected generated files after a block texture or identity mutation, then republishes a complete valid generation.
- Client delivery never mutates `SlotManager` or `TextureStore` state.

**Boundary**

This Group transports and applies the pack; it does not decide what an image should look like or how a tool edits it.

### B. Stable Generation, Update, and Resync

**Player outcome**

Fast edits and item-tool changes reach the actor and other viewers without corrupt files, split generations, duplicate reload storms, or a required rejoin.

**Experience**

- Rapid changes collapse into one controlled generation rather than several competing writes.
- A texture or identity mutation updates the placed block, item icon, and visible client state for affected players.
- A failed regeneration leaves the previous complete pack available instead of publishing a half-written pack.
- A normal texture edit applies through one full reload (mandatory as of 2026-07-19); rapid edits must still collapse into a single reload rather than a reload storm.

**Requirements**

- Build each generation in a staging location, validate every required file and variant, and atomically publish only a complete generation.
- Keep the last-known-good published generation and pin it while an active session still references it.
- Serialize writes and reload requests so a client never reads a partially written PNG.
- Debounce related edits and create one completion boundary for the resulting generation.
- G06 item tools use the same resync contract as command-based mutations.
- Changing an online player's resolution cancels that player's old transfer/application session and immediately starts one session for the newly selected variant.

**Boundary**

This Group owns generation and resync, not the individual tool actions that trigger it.

### C. Server-Authoritative `/cblowres`

**Player outcome**

A weak player or an operator can select a smaller pack that is already active on the next join, without reducing texture quality for anyone else.

**Command contract**

```text
/cblowres <128|256|512|off> [playerIGN]
```

- With no `playerIGN`, the sender changes their own setting. This is allowed for an ordinary player.
- A player may change another IGN only with the server's operator permission. A non-operator attempt is rejected without changing either setting.
- The server console must provide `playerIGN`; it has no player identity to use as a default.
- `128` and `256` select those maximum per-frame texture sizes. `512` and `off` are aliases for removing the low-resolution override and using the full/default variant.
- Names are matched case-insensitively. An offline IGN is accepted as a pending name and is bound/migrated to the player's UUID when identity is known; the last display IGN is retained for administration.
- If the target is online, the old pack session is cancelled and the selected variant is synced and applied immediately. If offline, the setting takes effect before their next manifest is selected.
- The server-selected value wins over any obsolete local setting. The existing client command must be removed or, if retained only as a diagnostic fallback, renamed so it cannot consume the server `/cblowres` root.

**Persistence contract**

- Store resolution choices in an isolated server file such as `config/customblocks/lowres_players.json`; one malformed entry must not discard valid entries.
- Store only active 128/256 overrides. `512`/`off` removes an override rather than creating two representations of full/default quality.
- Each known-player record contains UUID, last known IGN, and resolution. An offline-name record contains a normalized pending IGN and resolution until login supplies a UUID.
- On login, merge a matching pending-name record into the UUID record, update the display IGN after a rename, and resolve duplicate pending/UUID records deterministically without applying two settings.
- Save through a temporary file plus atomic replacement. Keep the last valid file if a new save cannot be completed.
- Load the mapping before player join configuration and manifest selection.
- Reject unknown option values clearly. Never reinterpret a malformed value as a forced 128 or silently change another player.

**Boundary**

This is a server delivery choice, not a client graphics menu. G27 may present it, but it must use this command/state contract rather than create a second authority.

### D. Resolution-Variant Generation and Image Correctness

**Player outcome**

The 128 and 256 variants remain visually and structurally valid for static and animated custom textures, even when one newly supplied image is corrupt.

**Requirements**

- Prebuild and reuse one shared artifact per resolution per published generation; never rescale separately for every joining player.
- Transform every server-generated CustomBlocks texture that can be delivered by this pack: base slot textures, per-face textures, animation grids, and generated tinted tool/item textures.
- Do not blanket-resize unrelated bundled mod assets. Tiny bundled assets that are not generated pack content remain unchanged.
- A static texture fits inside the selected maximum while preserving aspect ratio and a valid PNG encoding.
- An animation is resized per frame cell, then rebuilt with the original grid layout, frame order, timing, and `.mcmeta` semantics. The maximum applies to each frame, not the entire animation sheet.
- Avoid a resampler that holds the source plus several full-image floating-point planes. Decode/process with bounded rows, tiles, or frame cells so a large animation cannot create a several-hundred-megabyte transient allocation.
- Validate dimensions, decoded size, PNG output, metadata consistency, and hashes before publication.
- When a source or transform is invalid, reuse the last-known-good transformed file at the same variant. If no prior file exists, publish a recognizable missing-texture placeholder sized for that variant and log the exact path and reason.
- A forced 128 or 256 generation never substitutes a larger original image after an exception or memory failure.

**Boundary**

This scaling preserves supplied content; artistic editing and crop decisions remain G10 work.

### E. Bounded, Resumable, Verified Dedicated Sync

**Player outcome**

A slow, lossy, interrupted, or memory-constrained client can finish the selected pack without a full restart, false-success message, runaway queue, or connection flood.

**Manifest and session contract**

- Every manifest identifies protocol version, generation/session ID, selected resolution, normalized relative path, byte size, SHA-256, and aggregate pack hash.
- Enforce explicit maximums for compressed and expanded manifest size, file count, path length, chunk length, chunk count, individual file size, and total selected-pack size before allocation.
- Accept only normalized relative paths. Reject absolute paths, `..`, empty/invalid paths, and any resolved target outside the CustomBlocks pack root.
- Exactly one pack session may mutate a client's staging tree. A new generation, resolution change, disconnect, or server switch cancels stale queued work by session ID.

**Transfer and disk contract**

- Stream each file to a `.part` file; do not retain every chunk and then allocate a second complete byte array.
- Resume at verified file/chunk boundaries using a persistent local checkpoint. An interrupted low-resolution first join must not redownload already verified files.
- Hash while streaming, compare the final SHA-256, and atomically rename only a verified file. A short write, disk-full error, permission failure, or hash mismatch is a failure and cannot mark the file complete.
- Perform folder hashing, diff work, and checkpoint I/O away from the client/render thread. Clean obsolete files only after the new generation applies successfully.
- Preflight the pack directory and usable disk space before bulk transfer; preserve or safely remove `.part` files according to whether their session can resume.

**Flow-control and completion contract**

- Replace blind per-tick enqueueing with client-granted byte credit and actual channel writability. Initial design targets are 64 KiB chunks, 256 KiB initial receiver credit, and an adaptive ceiling of 1 MiB in flight per client.
- Control, cancellation, and acknowledgement messages must remain deliverable even while data credit is exhausted.
- The client reports received/written progress, verified files, retryable or terminal failures, reload start, and final application result.
- Server logs may say `applied` only after the client confirms the matching generation successfully reloaded. Queuing a final packet is not completion.
- Retries are bounded, preserve the last-known-good active pack, and report a useful failure reason instead of looping forever.

**Bad-network root-cause record — 2026-07-18**

- The current dedicated sync starts from the play-join event. Pack payloads therefore share one TCP connection and one ordered outbound stream with keepalive, chunk, registry, HUD, and other gameplay packets.
- The current sender may enqueue roughly 5 MiB/s per player regardless of what the socket or client has actually drained. Minecraft 1.21.1 uses a 15-second keepalive interval; when the previous keepalive is still unanswered at the following interval, the server disconnects the client. Large queued pack data can therefore sit ahead of the keepalive and its response path.
- The reported pack is roughly 214 MB. Even with no loss or protocol overhead, 10 Mbit/s takes about 171 seconds to transfer it. At that rate the client drains only about 1.19 MiB/s while the current sender can add 5 MiB/s, leaving roughly 57 MiB queued after 15 seconds. That queue alone takes about 48 seconds to drain, already longer than the approximate 30-second keepalive failure window. Jitter, Wi-Fi loss, retransmission, and reduced TCP congestion windows make the result worse.
- Increasing the keepalive timeout does not remove this queue. TCP is a reliable in-order byte stream, so a small control packet cannot jump ahead of already queued pack bytes. Correctness requires a small bound on outstanding in-band data or a separate transfer connection.
- Reconnect is not recovery today: the server forgets the session, an incomplete file is not checkpointed, the client buffers chunks in memory, and low-resolution first join starts again. A player on intermittent Wi-Fi can repeatedly lose almost-complete work.
- The existing `/pack.zip` HTTP handler is not a resumable alternative. Every successful download is a full `200`, with no byte-range or strong-validator contract, and it uses the JDK server's default executor. With `setExecutor(null)`, one slow download can occupy the server-created request thread and delay unrelated HTTP routes. It also uses a separately reachable plain-HTTP host/port, so NAT, firewall, or port configuration can make it unavailable even when Minecraft itself connects.

**Locked hybrid first-join transport (2026-07-18) — the server has a public port reachable from outside the LAN, so the HTTP(S)-first route below is viable and is the target design.**

1. During Fabric's configuration phase, send a small offer containing protocol, immutable generation, selected resolution, exact artifact bytes, SHA-256, and available transfer routes. Add a configuration task so the client does not enter the world until the matching pack is verified and applied. This removes world/chunk traffic from the download period and gives the player a truthful progress state.
2. If an exact verified artifact is already cached, apply/reuse it and complete configuration immediately. Otherwise prefer a separate HTTP(S) connection for only the selected 128, 256, or full artifact. A slow cosmetic download then cannot bury Minecraft keepalives on the game connection.
3. Make that route genuinely resumable: single byte ranges, `Accept-Ranges: bytes`, `206 Partial Content`, validated `Content-Range`, a strong generation `ETag`, and client `If-Range`. Stream to a `.part` file, persist the verified offset/checkpoint, hash while writing, verify SHA-256, and atomically rename only the exact immutable artifact. If the validator changes, discard incompatible partial bytes and restart only the new generation.
4. Serve slow downloads on a dedicated bounded concurrent executor with explicit per-IP/player and global limits. Keep generations immutable and pinned while resumable sessions reference them; prevent one slow client from blocking pack metadata, jar-update, or another player's transfer. Prefer HTTPS outside a trusted LAN; if plain HTTP is retained, integrity verification remains mandatory and scoped download tokens must not be logged. A reverse proxy, CDN, or object store may serve the same immutable URL/range/hash contract for a public deployment, while the embedded server remains a direct fallback.
5. If the advertised HTTP route is unreachable, blocked, or repeatedly makes no progress, switch once to the in-band configuration fallback. Never run both routes for the same session. The fallback uses 64 KiB chunks, receiver-granted credit, a 256 KiB initial window, a 1 MiB ceiling, and `Channel.isWritable()`; credit is returned only after bytes are safely written/checkpointed.
6. Classify retries. Connection resets, no-progress stalls, `408`, `429`, and appropriate `5xx` responses use capped exponential backoff with jitter and resume. Permanent protocol, authorization, unsafe-path, disk, integrity-after-limit, or incompatible-generation failures stop with one useful reason. A long but progressing transfer uses a no-progress watchdog rather than a short total-request timeout.
7. Show selected variant, bytes complete/total, percentage, current rate, ETA, retry/backoff, and `Resuming from …` while configuration is held. The user may cancel cleanly. The screen must not look like a frozen ordinary join.
8. After bytes verify, apply the pack and send an application acknowledgement for the exact generation. Only that acknowledgement completes the Fabric configuration task. A zero-connectivity client cannot download missing data, but temporary bad Wi-Fi must retain progress across reconnect and restart instead of beginning from zero or claiming false success.

Configuration phase by itself is not the network fix: it still uses the same Minecraft TCP connection. The separate HTTP(S) route avoids game-stream head-of-line blocking; receiver credit and channel writability make the in-band fallback safe. Locked 2026-07-18 — this design supersedes the existing matching-modded-client route.

**Boundary**

The throttle is part of correctness. It may adapt to the receiver, but it may not become an unbounded memory queue to maximize throughput.

### F. Two-GB Application, Cache, and Reload Budget

**Player outcome**

After transfer, the weak client can survive resource application, creative browsing, animations, and later pack changes within the 2 GB target.

**Starting budgets and lifecycle**

- CustomBlocks transport/staging heap outside Minecraft's resource reload should stay at or below 16 MiB per client; receiver in-flight data starts at 256 KiB and never exceeds 1 MiB.
- Bound the static off-atlas/icon cache by estimated native/GPU bytes as well as entry count, with a 64 MiB starting ceiling. Use LRU eviction and close/destroy every evicted native image and texture.
- Bound retained animation grids by decoded native bytes as well as idle time, with a 64 MiB starting ceiling. A 256-frame animation cannot bypass the limit merely because it is one cache entry.
- Clear and close the old CustomBlocks static and animation caches before a full reload begins, not only after reload completes, so old and new resources do not overlap at the peak.
- Clear all G05 native/GPU resources on reload, disconnect, server switch, and client shutdown.
- Treat these as CustomBlocks subsystem ceilings, not proof of total Minecraft memory safety. The production `-Xmx2G` test measures the complete modpack, resource reload, atlas, native images, driver allocations, and gameplay together.

**Reload behavior** (reloads mandatory as of 2026-07-19)

- Every real pack change — pixel-only or structural — applies through one full `reloadResources()`. There is no live in-place GPU swap; that path was scrapped and removed.
- A reload is skipped only when nothing actually changed on disk: a repeated identical regen within the same session (intra-session dedup), or a dedicated rejoin whose diff produced no written/deleted file. Neither skips a reload that an edit requires.
- A world open reloads normally; there is no persisted pack-hash sidecar skip.
- Application failure leaves the previous verified generation recoverable and produces a diagnostic reason.

### G. Compatibility, Diagnostics, and Recovery

**Player outcome**

An incompatible jar is rejected with a useful message before a large transfer, and a genuine 2 GB join failure leaves enough evidence to fix the exact layer that failed.

**Requirements**

- Give releases a unique build/protocol identity. Do not rely on several behaviorally different jars all reporting `1.0.0`.
- Exchange and validate compatibility before registry-dependent configuration and before pack manifest/HTTP selection. A client that supports the old regeneration payload but not the selected manifest protocol must receive a friendly incompatibility response, not silently miss its pack.
- Keep the modded connection path distinct from HTTP port `8080`/`8081` diagnostics; matching modded clients use the Minecraft connection for manifest/file sync, while vanilla pack and jar-update paths may use HTTP.
- Log generation, session, player UUID/IGN, selected resolution, total/compressed bytes, file counts, in-flight credit, elapsed time, retry/resume counts, verification result, reload result, and final application acknowledgement without logging secret tokens.
- Add explicit counters for CustomBlocks heap staging, static cache entries/estimated native and GPU bytes, animation grids/decoded bytes, and resource closures. JVM Native Memory Tracking alone does not account for every third-party or GPU allocation.
- Capture Java Flight Recorder allocation/heap evidence, JVM/native summaries, actual atlas dimensions, and the OpenGL maximum texture size during the production 2 GB acceptance run.
- Obtain the latest failing client's crash report and matching `latest.log`; historical atlas overflow establishes a risk, but does not prove the current failure has the same cause.
- Fix stalls, backpressure, and memory peaks before considering any timeout change. A narrowly active pack-sync grace may be tested under controlled latency/loss; a global timeout increase is not the primary fix.

## Current Implementation Baseline and Migration Risks

This section records the current-code facts the replacement must deliberately remove. It is not the target protocol and must not be copied forward as a shortcut.

### Server generation and sending

- `PackSyncService` currently uses 128 KiB chunks and permits about 256 KiB per player per tick, roughly 5 MiB/s at 20 TPS. That controls enqueue rate but does not use socket/channel writability or receiver-granted credit.
- The sender has no application-level acknowledgement, bounded retry, generation/session ID, or client-applied confirmation. Its completion line currently means chunks and the final message were queued, not that the client wrote, verified, reloaded, or displayed that generation.
- `PackManifest` retains a complete `Map<String, byte[]>`. For the reported roughly 214 MB pack, a refresh can overlap old and new snapshots above roughly 428 MB before maps, ZIP work, decoded images, and network buffers are counted.
- `ResourcePackServer.generate()` catches generation failure internally. The surrounding rebuild can then observe an older ZIP, calculate its hash, and separately recapture a newer manifest. Current build flow also emits pack contents separately for ZIP and manifest, so HTTP and modded delivery can represent different captures.
- The replacement must stage one immutable generation on disk, derive every delivery artifact from it once, and publish the generation/hash/manifests together.

### Client receiving and application

- The current receiver stores chunks as `Map<String, byte[][]>` and then assembles another complete `byte[]` for each file. Low-resolution work is submitted to an unbounded single-thread executor, so network input can outrun resizing and retain large arrays.
- Interrupted low-resolution sync starts over because its applied sidecar is committed only after finalization. Full-resolution diffing can practically reuse disk files, but current folder hashing calls `readAllBytes` and can run on the client thread.
- A disk write can be logged as failed while the caller continues toward success. There is no `.part` stream, atomic verified rename, or post-write hash boundary.
- Resetting state does not cancel already queued workers. Without a generation/session ID, old tasks can mutate files belonging to a later join, resolution, or server.
- Current payload codecs use broad string/byte-array fields and accept counts used directly for allocation; compressed manifest expansion, path length, file count, chunk count, chunk length, individual file size, and total size are not all explicitly bounded.
- Current path normalization is insufficient as a containment check. The replacement must validate a normalized relative path after resolving it under the intended pack root.

### Low-resolution scaling and texture ownership

- `LowResScaler` currently scales PNG files under `/textures/` after the client has received full bytes. It catches ordinary exceptions and returns the original full-resolution bytes; memory exhaustion is not safely converted. Both behaviors violate a forced-128 guarantee.
- The scaler currently bounds an entire animation sheet rather than each frame cell. For example, a 16×16 layout containing 256 frames can be reduced to a 128 px sheet and leave each frame near 8×8 rather than 128×128.
- `ImageResampler` currently retains a source integer array plus four full-image floating-point planes. A 4096×4096 animation sheet can therefore create roughly 400–500 MB of transient allocation during one resize.
- `AnimationDecoder` can produce a 4096×4096 grid with as many as 256 frames, about 64 MB for the raw RGBA grid before resampling overhead.
- `StaticFrameCache` and its icon cache have no byte/entry eviction and retain `NativeImageBackedTexture` objects until resource reload. Raw RGBA alone is about 1 MiB for a 512×512 texture versus 64 KiB for 128×128, before driver overhead.
- `AnimFrameCache` expires idle entries after roughly eight seconds but can retain a full decoded animation grid per entry. Time-only eviction does not bound a collection of large, recently used grids.
- Both caches are currently cleared after `reloadResources()` completes, allowing old CustomBlocks resources to overlap the reload peak. `NativeImage` owns closeable native memory, so eviction/clear must explicitly close it and destroy owned textures.

### Command, compatibility, and connection routing

- The current `/cblowres` root is registered as a client command. Leaving it in place would consume or conflict with the server-authoritative command and must be part of the migration.
- Current registry capacity is derived locally during startup. A `maxSlots` or jar mismatch can reject a client before pack recovery, while all current jar variants may still display `1.0.0`.
- The current version payload is sent after configuration/registry synchronization at join, too late to turn an early incompatibility into a friendly compatibility gate.
- An older dedicated client may understand the regeneration payload but not the manifest payload. The server can then skip HTTP because the client appears modded while dedicated pack sync refuses it, leaving no successful delivery route.
- Matching modded-client manifest/file sync uses the Minecraft connection. HTTP listeners on `8080`/`8081` are relevant to vanilla resource-pack or jar-update paths, not proof that the modded transfer is healthy.
- Current sync begins only in `ServerPlayConnectionEvents.JOIN`, after play configuration. Pack traffic therefore competes with normal world traffic instead of completing in a controlled pre-world configuration task.
- Current `/pack.zip` sends every successful request as a full `200` response and configures `setExecutor(null)`; it has no `Range`/`If-Range`, `206`/`Content-Range`, strong `ETag`, concurrent slow-download isolation, or resumable checkpoint contract.
- There is no implemented keepalive mixin that makes a long pack stall safe merely because an old package description mentions one. Backpressure, worker bounds, memory peaks, and the application handshake remain the primary fixes.
- High latency/loss magnifies TCP head-of-line delays and makes blind enqueueing more dangerous. Controlled impairment tests must cover this without using a global timeout increase as the first remedy.
- Large cosmetic `light=X` missing-model warning volume may amplify log I/O, but the owner excluded that warning cleanup from this join fix.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G06 | Tool mutations | Tool changes use the same generation and client resync contract as command mutations. |
| G10 | Image and color changes | Image edits identify affected generated textures; G05 validates, variants, publishes, and distributes them. |
| G20 | Version/release identity | Releases provide a unique build and protocol identity early enough for G05 compatibility gating. |
| G22 | Dedicated access control | Self-service is open; changing another player's resolution uses G22 operator policy without duplicating pack state. |
| G27 | Settings surfaces | Any resolution setting represents the server-authoritative G05 state without creating a second sync path. |

## Technical Contract

- Published pack state is an immutable generation containing full/512, 256, and 128 artifacts plus bounded manifests. Active sessions pin a generation until completion or cancellation.
- Resolution mapping is loaded before join manifest selection. `512`/`off` removes the override; server-forced 128/256 wins over obsolete local state.
- Generation uses staging, validation, SHA-256 integrity, and atomic publication. Last-known-good files and a target-sized placeholder are the only corrupt-image fallbacks.
- Dedicated sync uses bounded codecs, safe relative paths, receiver credit, session cancellation, `.part` streaming, persistent resume checkpoints, atomic verified commits, and applied-generation acknowledgement.
- Integrated-host regeneration, dedicated modded sync, and vanilla HTTP delivery remain separate routes with one logical content source and no stale-client slot-data assumption.
- CustomBlocks-owned heap, native-image, and GPU caches have explicit byte/entry ceilings and deterministic closure on eviction, reload, disconnect, and shutdown.
- The 2 GB release gate uses Java 21, the actual production mod list and generated pack, a clean client cache, server-forced 128, and enough gameplay/creative/animation activity to expose post-join retention.
- Delivery classes remain within the project file-size gate and keep server and client responsibilities separate.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| A 64 px server variant | The requested compatibility target is 128; validate that path before adding another artifact and test matrix. | G05 |
| Blanket resizing of bundled mod assets | The join pack contract covers server-generated CustomBlocks content; unrelated bundled assets need separate ownership and visual review. | Relevant asset owner |
| Global network-timeout increase | It can conceal a stalled or unbounded sync and affects unrelated connections. | G05 only after controlled evidence |
| Repairing cosmetic `light=X` missing-model warnings | They are not the selected join failure and the owner explicitly excluded them from this fix. | Separate model/log task |
| Full live upload coverage for every texture class | Requires a complete renderer-safe classifier and upload path. | G05 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-15 | Rely on a modded client accepting the HTTP server push. | Integrated hosts regenerate locally; dedicated modded clients use file sync. |
| 2026-06-15 | `silentPack = false` restores a vanilla dialog for CustomBlocks packs. | CustomBlocks pack delivery remains silent on its intended route. |
| 2026-06-20 | A remote client rebuilds from its own `slots.json`. | The server supplies generated pack files through the dedicated sync contract. |
| 2026-07-06 | Generate one lower-resolution pack for every player. | Quality is per player, but variants are shared and prebuilt once per generation. |
| 2026-07-06 | Client-only downscale is the final weak-PC solution. | Server-authoritative variants prevent a forced-128 player from receiving or resizing the full pack. |
| 2026-07-06 | `/cblowres` is a local client command. | The server command owns `/cblowres`; any retained local diagnostic must use a different root. |
| 2026-07-18 | Resize a variant independently for every joining player. | Build full/512, 256, and 128 shared artifacts once and select by player. |
| 2026-07-18 | On a scaling error, send the original full PNG. | Use the last-known-good low-res file or a target-sized missing-texture placeholder. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_05.md) · [All Groups](README.md)

- [G06 Tools](GROUP_06_TOOLS.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G20 External Integrations and Version Delivery](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 05 snapshot](../archive/group-migration-2026-07-18/GROUP_05_RESOURCE_PACK.md)
- [Java Image I/O region and subsampling controls](https://docs.oracle.com/javase/8/docs/technotes/guides/imageio/spec/apps.fm3.html)
- [Java bounded executor queues](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)
- [Netty channel writability](https://netty.io/4.1/api/io/netty/channel/Channel.html?is-external=true)
- [RFC 9113 flow-control model](https://www.rfc-editor.org/info/rfc9113/)
- [Fabric server configuration tasks](https://maven.fabricmc.net/docs/fabric-api-0.102.0%2B1.21/net/fabricmc/fabric/api/networking/v1/FabricServerConfigurationNetworkHandler.html)
- [Fabric server configuration events](https://maven.fabricmc.net/docs/fabric-api-0.115.6%2B1.21.1/net/fabricmc/fabric/api/networking/v1/ServerConfigurationConnectionEvents.html)
- [Minecraft 1.21.1 keepalive constants](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.3/constant-values.html)
- [Minecraft 1.21.1 server common network handler](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.3/net/minecraft/server/network/ServerCommonNetworkHandler.html)
- [TCP reliable in-order byte-stream contract, RFC 9293](https://www.rfc-editor.org/rfc/rfc9293.html)
- [HTTP range, validator, and partial-content contract, RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html)
- [Java 21 embedded HTTP server executor contract](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html)
- [Java 21 HTTP client](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)
- [Google Cloud retry guidance](https://docs.cloud.google.com/storage/docs/retry-strategy)
- [`tc netem` latency, jitter, loss, reorder, and rate emulation](https://man7.org/linux/man-pages/man8/tc-netem.8.html)
- [Java atomic move](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/StandardCopyOption.html)
- [PNGJ row-by-row PNG processing](https://github.com/leonbloy/pngj)
- [Yarn bounded packet codecs](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.1/net/minecraft/network/codec/PacketCodecs.html)
- [Yarn NativeImage lifecycle](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.1/net/minecraft/client/texture/NativeImage.html)
- [Oracle G1 humongous-object guidance](https://docs.oracle.com/en/java/javase/17/gctuning/garbage-first-garbage-collector-tuning.html)
- [Java Flight Recorder troubleshooting](https://docs.oracle.com/en/java/javase/21/troubleshoot/troubleshoot-performance-issues-using-jfr.html)
- [Java `jcmd` and Native Memory Tracking](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jcmd.html)
- [OpenGL texture allocation reference](https://wikis.khronos.org/opengl/GLAPI/glTexImage2D)
- [PNG specification](https://www.w3.org/TR/png-3/)
- [NIST SHA-256 standard](https://csrc.nist.gov/pubs/fips/180-4/upd1/final)
