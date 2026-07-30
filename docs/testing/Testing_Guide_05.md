# Group 05 - Silent Resource Pack Delivery

## Status

| | |
| --- | --- |
| **Verdict** | Slow-internet joins, weak-PC textures, safe delivery, 2 GB stability, and old-version warnings are all built and awaiting play-testing (§D fix awaiting retest). |
| **Progress** | 🟩🟩🟩🟩🟥🟥🟥🟥🟥🟥 40% |
| **Last tested** | 2026-07-19 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| E | Slow / dropping internet on first join | Built 🎯 | - |
| F | Smaller textures for weak PCs (`/cblowres`) | Built 🎯 | - |
| G | Safe, resumable texture delivery to players | Built 🎯 | - |
| H | Stays stable on a 2 GB-memory PC | Built 🎯 | - |
| I | Old-version warning and per-join logs | Built 🎯 | - |
| D | Fast SP create/retexture burst stability | Built 🎯 | Regression 💔 |
| C | Tool edit client resync after pack updates | Done ✅ | - |
| J | Host/integrated silent local pack generation | Done ✅ | - |
| K | Remote dedicated modded-client pack sync | Done ✅ | - |
| L | Dedicated manifest diff rejoin | Done ✅ | - |

**Original Group:** [GROUP_05_RESOURCE_PACK.md](../groups/GROUP_05_RESOURCE_PACK.md)

---

# Active Tests

## 💡 Owner quick-check — the part that matters to you

This whole group exists for one thing: a weak-PC friend can join and play. These are the only checks you need to see with your own eyes. Everything after this (§D–§I) is fine-grained detail that proves the code is safe — a helper can run those, or they simply stay pending; you don't have to grind through them.

| The thing you care about | Result |
| --- | --- |
| A weak-PC player (your friend) can join the server and load its textures. | ✅ `2026-07-19` |
| That player can run `/cblowres 128` and switch to smaller textures on the spot, no rejoin. | ✅ `2026-07-19` |
| Blocks and animated blocks still look correct after the switch. | 🎯 not checked yet |

## 💡 Setup (for whoever runs the detailed §D–§I tests)

- Use Java 21 and the real production client/server mod list, configs, registry size, and generated CustomBlocks content.
- For §F, §G, §H, and §I, use a real dedicated server, one normal client, and one client launched with exactly `-Xmx2G`.
- Start the 2 GB acceptance run with no `resourcepacks/CustomBlocks` folder, no low-res sidecar/checkpoint, and no reusable client pack cache.
- Keep the latest client crash report and matching `latest.log`; preserve server logs from join start through application acknowledgement.
- Record selected resolution, manifest generation/session, selected-pack bytes/files, retry/resume counts, client heap, CustomBlocks cache counters, resource reload result, atlas dimensions, and OpenGL maximum texture size.
- Use controlled latency, loss, interruption, disk-full/write-failure, malformed-input, corrupt-PNG, and incompatible-jar cases only in disposable test copies.
- Use a real direct image URL for texture rows.
- Use SP/integrated world for §D.
- Use a real dedicated server for §K, §L, §D, §F, §G, §H, and §I.

> **§A and §B were scrapped 2026-07-19 — reloads are mandatory.** The no-reload fast paths were removed from
> code; see the Scrapped archive below. Active testing starts at §C.

## D - Fast SP create/retexture burst stability - Built 🎯

| | |
| --- | --- |
| **What this proves** | Making several textures very fast in your own world doesn't corrupt them or force a rejoin. (This one had a bug where fast edits could break a texture; a fix is in, waiting on your retest.) |
| **Counts as passed when** | Two fast bursts work twice — no broken texture, no forced rejoin. |
| **Passed** | ✅ `YYYY-MM-DD` |

**✅ You can test:** D1, D2 — both in your own singleplayer world, no friend or tools needed. This is the one with a bug-fix waiting on your retest.

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | In singleplayer, quickly create and re-texture two blocks using image links. | All textures show up, nothing looks broken, and you don't need to rejoin. | 🎯 | ➖ |
| D2 | Do the same fast burst again after restarting Minecraft. | Stable again — no leftover broken texture from before. | 🎯 | ➖ |

## E - Slow / dropping internet on first join - Built 🎯

| | |
| --- | --- |
| **What this proves** | A player on slow or unreliable internet still gets all the textures before entering the world, sees a clear progress bar the whole time, and never a stuck-looking join. If their internet drops, they keep what already downloaded and continue from there — they don't start over. |
| **Counts as passed when** | The already-downloaded, slow-download, dropped-and-resumed, retry, progress-bar, restart, and finish rows all work twice. |
| **Passed** | ✅ `YYYY-MM-DD` |

**✅ You can test:** E1, E5, E14 (join twice; change a texture then reconnect; pull the network cable mid-join).
**🔧 Needs a helper/tools (park):** E2, E3, E7, E9, E11, E12, E13 — all need a way to throttle or fake a bad connection.
**🚫 Skip:** E4, E6, E8, E10 — a separate web-download method that was never built.

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Join when this player already has the correct textures from before. | They get straight in — nothing is downloaded again. | ➖ | 🎯 |
| E2 | On a fresh weak-PC (128) client, join on a slow, patchy connection. | The progress bar keeps moving, the player stays connected, the textures finish and load, and they get into the world. | ➖ | 🎯 |
| E3 | Join on an even worse connection, then cut the internet after most of it has downloaded; reconnect. | It picks up where it left off instead of downloading everything again. | ➖ | 🎯 |
| E4 | (Separate web-download method — never built.) | — | ➖ | 🚫 |
| E5 | Change the textures on the server while a player's half-finished download exists, then have them reconnect. | It throws away the half-done download and cleanly gets the correct new textures — no mixing old and new. | ➖ | 🎯 |
| E6 | (Separate web-download method — never built.) | — | ➖ | 🚫 |
| E7 | Watch a player during a slow download. | The download stays gentle and never lags them off the server (no false disconnect). | ➖ | 🎯 |
| E8 | (Separate web-download method — never built.) | — | ➖ | 🚫 |
| E9 | Force drops, freezes, and repeated errors during a download. | Small hiccups quietly retry on their own; a real problem stops once with a clear message — never an endless retry loop. | ➖ | 🎯 |
| E10 | (Separate web-download method — never built.) | — | ➖ | 🚫 |
| E11 | Restart the player's game mid-download, then restart the server. | The download resumes where it was, not from the start (unless the textures were changed on purpose). | ➖ | 🎯 |
| E12 | Watch a full slow join, including a hiccup, from the player's screen. | The bar shows size, percent, speed, time left, and how much was already saved; cancelling (Esc → Disconnect) exits cleanly and never looks frozen. | ➖ | 🎯 |
| E13 | Let everything download but block the final load, then allow it. | The server doesn't report "done" until the player's game has actually finished loading the textures. | ➖ | 🎯 |
| E14 | Cut the player's internet while textures are still missing, then bring it back. | They get a clear "connection problem" message, keep the partial download, and resume — it never fakes success or restarts from zero. | ➖ | 🎯 |

## F - Smaller textures for weak PCs (`/cblowres`) - Built 🎯

| | |
| --- | --- |
| **What this proves** | `/cblowres` lets a player switch to smaller textures (128 or 256) so a weak computer can cope, or back to full with `off`. Operators can set it for someone else. The server remembers each player's choice — even across restarts — and applies it without needing a rejoin. |
| **Counts as passed when** | The set-your-own, operator-sets-others, non-operator-blocked, console, offline-remembered, restart, back-to-full, two-players, texture-quality, and bad-input rows all work twice. |
| **Passed** | ✅ `YYYY-MM-DD` |

**✅ You can test (most of §F):** F1–F9, F11–F14, F17, F18 — just commands, a friend, and looking at the blocks (F13/F14 need you to feed in a deliberately broken image).
**🔧 Needs a helper/tools (park):** F10, F15, F16 — need to watch network traffic or measure memory.

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | As a normal (non-operator) player, type `/cblowres 128`. | Your textures switch to the smaller set right away — no operator rank needed. | ➖ | ✅ `2026-07-19` |
| F2 | As an operator, type `/cblowres 256 <online player>`. | Only that player switches to 256, right away. | ➖ | 🎯 |
| F3 | As a normal player, try `/cblowres 128 <someone else>`. | You're blocked with a clear message; nobody's textures change. | ➖ | 🎯 |
| F4 | From the server console, type it with no name, then with a name. | The no-name version tells you the console must name a player; the named version works. | ➖ | 🎯 |
| F5 | Set 128 for a player who is offline (type their name in any capitalization), restart the server, then let them join. | They come in on 128 — the choice was remembered. | ➖ | 🎯 |
| F6 | Restart both the 128 player's game and the server, then rejoin. | Still 128 — the server remembers it, not the player's PC. | ➖ | 🎯 |
| F7 | Type `/cblowres 512`, then `/cblowres off`. | Both put the player back on the full-quality textures. | ➖ | 🎯 |
| F8 | A player has an old leftover "small textures" setting on their own PC, but the server chose something different. | The server's choice wins; the old PC-side setting is ignored. | ➖ | 🎯 |
| F9 | Two fresh players join at the same time — one forced to 128, one on full. | Each gets only their own textures; the full-quality player is not shrunk. | ➖ | 🎯 |
| F10 | Check what a forced-128 player actually downloaded. | Only the small 128 textures came down — never the full-size ones. | ➖ | 🎯 |
| F11 | Make wide, tall, square, per-side, and colored textures, then look at the 128 and 256 versions. | All look right and keep their shape — nothing stretched or squashed. | 🎯 | 🎯 |
| F12 | Make an animated texture with several frames and custom timing, then view the 128 and 256 versions. | Every frame is shrunk correctly and the animation still plays at the right speed. | 🎯 | 🎯 |
| F13 | Save a good texture, then feed in a broken replacement for it. | It keeps the last good version and notes the broken one in the log — no full-size one slips through. | 🎯 | 🎯 |
| F14 | Add a brand-new broken texture that has no earlier good copy. | It shows a clear "missing texture" placeholder at the right size; the pack still works. | 🎯 | 🎯 |
| F15 | Push a very large image or animation through 128 shrinking. | It finishes without a memory spike. | 🎯 | 🎯 |
| F16 | Trigger the same texture build while several 128 and 256 players connect. | The server builds each size once and shares it — not once per player. | ➖ | 🎯 |
| F17 | Set a choice for an offline name, then have that player join after a name or capitalization change. | The two records merge into one, keeping their current name and single chosen size. | ➖ | 🎯 |
| F18 | Type `/cblowres` with a missing, unknown, or nonsense value. | It explains the correct usage and changes nothing. | ➖ | 🎯 |

## G - Safe, resumable texture delivery to players - Built 🎯

| | |
| --- | --- |
| **What this proves** | When the server sends textures to a player, it does it safely: it never floods them off the server, it resumes after an internet drop, it checks nothing arrived broken (and re-fetches if it did), it can't be tricked into writing bad files onto their PC, and it only counts the job as done once the player's game has actually loaded the textures. |
| **Counts as passed when** | The clean-join, only-changed, resume, slow-connection, mid-change, disk-error, corruption, bad-data, unsafe-file, done-only-when-loaded, load-failure, disconnect, low-disk, and many-players rows all work twice. |
| **Passed** | ✅ `YYYY-MM-DD` |

This is the delivery engine behind §E — mostly a helper's job.
**✅ You can test:** G2, G10 (rejoin, change one texture, rejoin again; watch the server log say "applied" after a normal join).
**🔧 Needs a helper/tools (park):** G1, G3–G9, G11–G15 — need to interrupt or throttle the connection, corrupt a file, fill a disk, or feed the server bad data.

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | A player joins fresh and downloads the textures. | The download stays light — it never floods or lags them off the server. | ➖ | 🎯 |
| G2 | A player rejoins with the same textures, then you change one texture and they rejoin again. | The first rejoin downloads nothing; the second downloads only the one changed file. | ➖ | 🎯 |
| G3 | Cut the internet halfway through a big first download, then reconnect. | It continues from where it stopped — finished files are not downloaded again. | ➖ | 🎯 |
| G4 | Put a player on a slow, unreliable connection while others are playing. | Their download stays controlled, chat and commands stay responsive, and they finish without lagging the server. | ➖ | 🎯 |
| G5 | Change a player's texture size while their download is still running. | The old download is cancelled and can't overwrite the new one. | ➖ | 🎯 |
| G6 | Make the disk full or unwritable during a download. | A clear failure message; textures aren't marked done, and the previous working textures still work. | ➖ | 🎯 |
| G7 | Corrupt one downloaded file, then allow a clean retry. | It catches the bad file, re-downloads just that one, and only keeps it once it's correct. | ➖ | 🎯 |
| G8 | Feed the server oversized or garbage data. | Every size limit is enforced and the bad data is rejected without crashing. | ➖ | 🎯 |
| G9 | Feed the server sneaky file paths that try to escape the textures folder. | Nothing can be written outside the CustomBlocks folder; the bad paths are rejected. | ➖ | 🎯 |
| G10 | Watch a normal download from start to finish. | The server only reports "done" after the player's game finishes loading — never just because the last file was sent. | ➖ | 🎯 |
| G11 | Make the player's game fail to load the textures after everything arrives, then reconnect. | The server records the failure, keeps the previous working textures, and doesn't falsely report success. | ➖ | 🎯 |
| G12 | Disconnect, switch servers, or shut down mid-download. | Everything closes cleanly and no leftover download writes anything afterwards. | ➖ | 🎯 |
| G13 | Interrupt a texture rebuild partway through, then have someone else join. | No half-built textures go out; new joiners get the last complete version. | ➖ | 🎯 |
| G14 | Start a download with too little free disk space, then another with plenty. | The first is refused up front with a clear reason; the second works normally. | ➖ | 🎯 |
| G15 | Download to several normal and slow players at once. | Each is handled fairly, the server stays responsive, and its memory use stays under control. | ➖ | 🎯 |

## H - Stays stable on a 2 GB-memory PC - Built 🎯

| | |
| --- | --- |
| **What this proves** | The real modpack can join and keep playing on a low-memory (2 GB) computer forced to small textures, without running out of memory or crashing — even after browsing lots of blocks and animated blocks. Textures don't pile up in memory forever; old ones are dropped and freed. |
| **Counts as passed when** | Two clean first joins, plus the browse-blocks, browse-animations, size-switch, reconnect, long-play, and memory-review rows all pass with no crash, out-of-memory, or disconnect. |
| **Passed** | ✅ `YYYY-MM-DD` |

**✅ You can test (roughly):** H5, H6 (switch texture sizes then change a block; disconnect, switch servers, reconnect, quit — game shouldn't crash or slow down).
**🔧 Needs a helper/tools (park):** H1, H2, H3, H4, H7, H8 — need the real 2 GB test PC, a long play session, or a memory readout to confirm "stays capped".

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Launch the real modpack on a 2 GB PC, wipe any old CustomBlocks textures, force 128 on the server, and join. | Textures load, the player gets in, and there's no crash or out-of-memory error. | ➖ | 🎯 |
| H2 | Do H1 again from a clean start and compare. | Same result — it doesn't secretly rely on leftover cached data. | ➖ | 🎯 |
| H3 | After joining, browse all CustomBlocks blocks and item icons, showing lots of them over and over. | Memory used by textures stays capped; old ones are dropped and freed instead of piling up. | ➖ | 🎯 |
| H4 | View and cycle through the largest animated blocks for a while. | Animation memory stays capped and unused ones are freed. | ➖ | 🎯 |
| H5 | With lots loaded, switch 128 → 256 → off and change a block. | Old texture memory is freed before each reload; old and new don't stack up. | ➖ | 🎯 |
| H6 | Disconnect, switch servers, reconnect, and quit after all that. | Memory returns to normal; nothing stays held from the old connection. | ➖ | 🎯 |
| H7 | Keep playing normally for a long session — moving, loading chunks, using the inventory, viewing animated blocks. | Stays connected and playable within 2 GB; memory levels off instead of climbing forever. | ➖ | 🎯 |
| H8 | Review the memory and graphics readings from the run. | They explain the memory use with no unexplained texture buildup. | ➖ | 🎯 |

## I - Old-version warning and per-join logs - Built 🎯

| | |
| --- | --- |
| **What this proves** | A player on an outdated version gets a clear "your version is out of date" message instead of broken or missing textures. The server's saved list of player texture choices survives a damaged file, and each join is written to the log so problems can be traced. |
| **Counts as passed when** | The matching-version, old-version, different-build, damaged-file, interrupted-save, and join-log rows pass. |
| **Passed** | ✅ `YYYY-MM-DD` |

**✅ You can test:** I1, I4, I6 (join with the current version; put a few broken lines in `lowres_players.json` then restart; do a couple joins and read the log).
**🔧 Needs a helper/tools (park):** I2, I3, I5, I7, I8, I9 — need an old version of the mod, memory tools, or the friend's crashing PC.

| # | Do this | You should see | SP | MP |
| --- | --- | --- | --- | --- |
| I1 | Join with a matching, up-to-date version. | Accepted normally; textures sync as usual. | ➖ | 🎯 |
| I2 | Join with an older version that doesn't understand the new texture sync. | A clear "out of date" message up front — the player doesn't just end up with missing textures. | ➖ | 🎯 |
| I3 | Join with another mismatched version. | The message and log name the actual versions on each side, not a vague identical "1.0.0". | ➖ | 🎯 |
| I4 | Put some broken entries among good ones in the player texture-choice file (`lowres_players.json`), then restart. | Good entries load, broken ones are skipped with a specific warning, and the whole file is not thrown away. | ➖ | 🎯 |
| I5 | Interrupt the server while it's saving that file, then restart. | The previous good file is still readable; no player silently gets the wrong texture size. | ➖ | 🎯 |
| I6 | Do a normal, a resumed, and a failed join while keeping the logs. | Each join is logged with player, texture size, files, amount, time, whether it retried, and whether it succeeded — and no passwords or secrets. | ➖ | 🎯 |
| I7 | Compare the game's built-in memory tools with CustomBlocks' own texture-memory counters during heavy use. | CustomBlocks' counters clearly show what it's holding and freeing. | ➖ | 🎯 |
| I8 | Reproduce the friend's current crash once on the latest version and keep the crash and log files. | It points to the actual current cause, not an old one. | ➖ | 🎯 |
| I9 | Check the two web ports and the in-game texture sync separately during a normal join. | Textures sync over the game connection; the web ports (8080/8081) only serve the basic pack download. | ➖ | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §C Tool edit client resync after pack updates — ✅ `2026-07-19`
- §J Host/integrated silent local pack generation — ✅ `2026-06-15`
- §K Remote dedicated modded-client pack sync — ✅ `2026-07-03`
- §L Dedicated manifest diff rejoin — ✅ `2026-07-03`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §D Fast SP create/retexture burst stability — 💔 `2026-07-03`: rapid reloads could expose a corrupt-PNG race. Fixed in code 2026-07-19 by serializing the write→reload cycle; awaiting owner D1/D2 retest. Root cause: `PROGRESS_LOG.md`.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §A Live in-place texture swap without full reload — 👎 `2026-07-19`: reloads turned out to be mandatory — a GPU-only swap that skips the reload pipeline is not correct, so the live-swap path was removed. Every real texture edit now takes one reload.
- §B Skip redundant integrated first-open reload — 👎 `2026-07-19`: unreliable and unnecessary once reloads are mandatory; the per-world skip was removed and a world open always reloads once. Same-session dedup (kept) and the §L no-change rejoin skip (confirmed) are separate and untouched.
- `silentPack = false` restoring the dialog — 👎 `2026-06-15`: CustomBlocks packs are intended to stay silent.
- Global texture-size downgrade for everyone — 👎 `2026-07-06`: Rejected because it lowers quality for players who do not need it.
- Client-only downscale as the final weak-PC solution — 👎 `2026-07-18`: Replaced because it downloads full-resolution content before resizing and cannot guarantee the forced-low-res memory path.
- Per-player resizing during every join — 👎 `2026-07-18`: Replaced by shared full/512, 256, and 128 artifacts built once per published generation.
- Original full-size PNG as a low-res scaling fallback — 👎 `2026-07-18`: Replaced by last-known-good low-res output or a target-sized missing-texture placeholder.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] §D corrupt-PNG race — fixed in code 2026-07-19; leave open until owner D1/D2 retest passes. Detail: `PROGRESS_LOG.md`.
- [ ] Delete `g05a`, `g05b`, `g05c`, and disposable resolution/transport test blocks after verification.
- [ ] Keep SP reload-burst evidence attached to G05-D2-CORRUPT-PNG until fixed.
- [ ] Preserve the latest 2 GB client crash/log pair and the final passing JFR/log/counter bundle with the release evidence.
- [ ] Remove protocol-harness files, traffic shaping, disk-failure simulation, and corrupt test input after their rows pass.
- [ ] Do not treat vanilla startup pack reload as a CustomBlocks failure.
- [ ] Cross-link tool resync failures to G06 when the broken trigger is an item tool.

</details>
