# Group 20 — External Integrations: Cloud Vault, Discord & the Block Network

> **UI medium audit (2026-07-09):** VaultConflictScreen confirmed right medium (Screen), flagged for a
> design/UX polish pass.

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 11 (category share) + Group 18 (note share) prove the vault HTTP plumbing works. Internet access on the server. **Group 21 (Config GUI) is NOT a hard prerequisite** — every setting works from `config/customblocks/data/config.json` today (the original doc's "G21 required" claim was wrong).
>
> **Objective:** Finish the real gap — **whole-block** cloud share (upload/download by code) — and turn `DiscordWebhook` from a plain-text stub into a **fully customizable** notification system. Then grow both into a phased "Block Network": marketplace, cross-server library, community, external creation, and an owner control plane. **All of it sits on the owner-controlled Cloudflare worker, hardened against abuse because this is a public-released mod.**
>
> **Source:** Phase 14 (Cloud Vault + Discord stubs), Decision §G (vault deployed), Decision §N (Discord blank by default), the **owner brainstorm interview 2026-06-21** (4 UI rounds + 2 feature waves = 32 features greenlit), and the **owner UI implementation interview 2026-06-21** (5 rounds — see **🎨 UI Decisions** below, which nails down exactly how every surface looks + behaves).
>
> **Rules:** Build **one slice at a time**, in order. Owner confirms each in-game before the next (CLAUDE.md §2/§4). **Nothing is ✅ until the owner confirms in-game.** Worker-side pieces are the **owner's deploy** — a green mod build never marks them done.

---

## 🧭 Owner triage — "next critical fixes" review (2026-06-27)

> Owner walked the 10-item review. Verdicts verified in code where flagged. **This section overrides earlier wording it touches** (e.g. the Vault Hub form-factor).

**✅ Confirmed real bugs — fix:**
- **Master switch leak (was #1) — ✅ FIXED + confirmed in-game 2026-06-28 (G20 TG A7–A10).** `cloudShareEnabled=false` previously gated **only** the block vault (`CloudCommands.cloudReady`). These paths checked only `CloudVaultClient.isConfigured()` → they hit the network with the master switch off:
  - `/cb category share` → `CategoryCommands.shareCategory` (gate at `isConfigured` :259)
  - `/cb category import` → `CategoryCommands.importCategory` (:299)
  - lore Share button → `NoteCommands.share` (:128)
  - `/cb lore import` → `NoteCommands.importNote` (:161)
  - conflict-screen re-fetch → `VaultConflict.resolve` (:89) — reached only after a gated download, but re-hits the network when a player clicks an action, so it's gated too.

  **Locked fix plan (owner, 2026-06-27):** gate **all 5** paths, **both directions** (share *and* import). **Inline** check in each method (no shared helper) — insert the `cloudShareEnabled` check **above** each `isConfigured()` line, copying `CloudCommands.cloudReady` (:175). Message matches the block vault **exactly** (red error): `"Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled)."` Switch-on behavior unchanged. After: `gradlew build` (compile + gates), then owner tests in-game. (Owner believed this was patched manually; it is not in the repo.) In-game test rows: G20 TG §A A7–A10.
- **Backup → cloud sync (was #4) — ⏸ PARKED by owner 2026-06-28 (not needed now); build-green, backups moved KV→R2, ready to un-park.** `/cb backup save` + the GUI save zip the saved backup and sync it; the code logs in `/cb vault codes`. **Gated + best-effort:** fires only when `cloudShareEnabled` AND `vaultEndpoint` is set; a cloud failure reports but **never** fails the local save. Auto-backups + pre-retexture safety copies do **not** sync (they bypass `startSave`). **Size-wall discovery (owner in-game "not working", diagnosed 2026-06-28):** real backups are **40–150+ MB** — past KV's **25 MiB** value cap (40 MiB POST → 413) **and** past the worker's **100 MB** request-body limit. So the bytes must not pass through the worker. **Fix = presigned direct-to-R2:** the worker ([cloudflare/worker.js](../../cloudflare/worker.js)) now signs a one-time R2 URL (AWS SigV4, Web Crypto) — `POST /backup` → `{code,url}`, `GET /backup/<code>` → 302 to a presigned GET; the mod ([CloudVaultClient.uploadBackup](../../src/main/java/com/customblocks/cloud/CloudVaultClient.java)) does a **two-step** POST-then-PUT, uploading the zip straight to R2 (single object ≤5 GB, no size cap). 3-month expiry is now an **R2 bucket lifecycle rule** (90 d), not a worker TTL. category/note unchanged on KV. **Owner setup required before testing:** enable R2 → bucket `cb-backups` → lifecycle 90 d → API token → 4 worker vars (`R2_ACCOUNT_ID`/`R2_BUCKET`/`R2_ACCESS_KEY_ID`/`R2_SECRET_ACCESS_KEY`) → redeploy worker (steps in [cloudflare/SETUP.md](../../cloudflare/SETUP.md)). Still **NOT confirmed**. G20 TG §C (C1–C5).

**🟢 Verified non-issue — no action:**
- **Raw HTTP links (was #2).** `[download]` links (`category export`, `report generate`, etc.) build from `httpHost` (default `127.0.0.1`) and post via `sendFeedback(…, false)` → **sender-only, never broadcast**. No host/IP leaks to other players; at worst shows the OP-runner the IP they already connected to. Dropped.

**🧊 Reclassified QoL / deferred (owner — not critical now):**
- **Request signing + control plane (was #3) → future owner web dashboard.** S4 (HMAC/identity) + the D13 control plane deferred; owner wants them surfaced as a **web admin dashboard** (roadmap OD1) later, not a release blocker.
- **Vault Hub (was #6) → S7, now a CLIENT SCREEN, not a chest.** Owner: the Hub becomes a **screen GUI** (Block-Creation-Studio style), superseding the "central chest dashboard" wording in **U11 / S7**. QoL, deferred.
- **Bulk / all-block vault upload (was #7).** All-block Vault tile routes around bulk sync because bulk upload isn't implemented. QoL, deferred.
- **Permissions system / per-command nodes (was #9).** Code uses scattered OP checks instead of permission nodes + fallback tiers. QoL, deferred.

**✅ Confirmed in-game 2026-06-28:**
- **#5 vault code history → `/cb vault codes`.** New `VaultHistory` store (`config/customblocks/data/vault_codes.json`, atomic write, newest 500). One **server-wide** shared list; records on **upload only**. Block + category + note uploads — and backup sync (#4) — all log a code. Schema is open (kind + label) so new share types log without a format change. *(Named `codes`, not `mine`, because the owner chose one shared list, not per-player.)* Owner confirmed I1–I5 incl. restart-persist. G20 TG §I ✅ 5/5.
- **#8 category-edit Share tile.** `CategoryEditMenu` slot 15 was greyed "coming soon" → now runs `category share <cat>` (mirrors the already-working `CategoryBrowserMenu` Share tile). The command's own gates handle disabled/unconfigured. Owner confirmed J1–J2. G20 TG §J ✅ 2/2.
- **#10 dead `CloudVaultClient.upload/download` stubs removed.** The two Phase-14 stubs (always returned null, zero callers) + the now-unused `SlotData` import are deleted, header updated. No behavior change; the 2026-06-28 jar ran in-game with no regression.

> 🧊 **QoL — chat now, GUI later (cross-linked).** `/cb vault codes` + the cloud share commands are **chat-message** output for now. A chest/screen GUI front-end is the **Vault Hub** (#6 / §G / S7 client screen) — deferred QoL, no work scheduled. Tracked across all four surfaces: **GROUP_20 §G** (here) ↔ **GROUP_04** (chat messages) ↔ **GROUP_27** (screens) ↔ **GROUP_02** (chest GUI).

---

## 📡 Deployed infrastructure (real, keep)

| Thing | Value |
|---|---|
| Worker | `cb-cloud-vault` |
| KV namespace ID | `ee49b7a0d7584b539b1c986bdf8428ee` |
| R2 bucket (backups) | `cb-backups` — ⏳ pending owner setup (worker vars `R2_ACCOUNT_ID`/`R2_BUCKET`/`R2_ACCESS_KEY_ID`/`R2_SECRET_ACCESS_KEY`) |
| Endpoint | `cb-cloud-vault.cbbblocksvault.workers.dev` |
| Doc config keys | `cloudShareEnabled`, `cloudShareUrl` |
| **Code config key (reality)** | **`vaultEndpoint`** (the only field that exists today) |

---

## 🔒 Locked Decisions (owner interview 2026-06-21)

> Surveyed the live code first, then a multi-round UI interview + two feature-brainstorm waves.
> **Nothing built this pass — design only.** These decisions override anything below them.

### Reality corrections (read from code, not guessed)

- **Config now has `vaultEndpoint`, `discordWebhookUrl`, and `cloudShareEnabled`.** `cloudShareUrl` / `cloudPackSecret` / `discordNotify*` **do not exist yet**.
- **Vault HTTP plumbing is PROVEN.** `CloudVaultClient.uploadCategory` / `downloadCategory` (G11) and `uploadNote` / `downloadNote` (G18) already hit the worker fine, off-thread, with graceful null-on-failure. The generic **block** methods `upload(SlotData,texture)` / `download(code)` are still stubs, but block share now reuses the category ZIP route through `VaultBlockCodec`.
- **`/cb vault upload|download` are wired for block share.** `/cb export <id> vault`, `/cb importblock <code>`, and the single-block Export Dashboard Vault tile delegate to that same path as of 2026-06-27. `/cb discord test` + `status` **work**, but `DiscordWebhook.post()` sends **plain text only** — no embeds, no event hooks, no toggles. No event ever fires today.
- **`/cb backup`, `/cb exportblock`, `/cb category export` already exist** (BackupCommands, BlueprintCommands, CategoryCommands) — backup-sync + block packaging can lean on them.
- **The original doc's tests were mis-numbered `G21.x`** (stale, same bug G19's doc had → renumbered to **G20.x** here).

### Core decisions (rounds 1–4)

| # | Decision |
|---|---|
| D1 | **Scope = both** Cloud Vault whole-block share **and** Discord, full. |
| D2 | **Vault shares static AND animated blocks.** Animated = grid PNG + `grid.json` sidecar (G14) packed into the payload. |
| D3 | **Cloud storage = reuse the existing `/category` zip route** for the block payload (1-block zip: `SlotData` JSON + texture(s) + sidecar). **No new storage route needed** → zero new worker route just to store a block. *(Alternative considered + parked: a dedicated `/block` + `/block/<code>` route — cleaner naming, but needs a new worker deploy. Revisit only if the zip path proves limiting.)* |
| D4 | **Conflict on download** (incoming block id already exists locally): **warn the player it exists + a clickable "override" + a multi-level undo/redo stack.** Professional polish — never a silent overwrite. `/cb vault undo` / `/cb vault redo` walk a stack of snapshots (not just last). *(May later unify with the planned G25 block-edit history; for now G20 owns its own stack.)* |
| D5 | **Discord events wired — ALL of them:** block **created**, **deleted**, **edited/retextured**, **bulk** op, **backup** saved, **vault** upload/download, **error** (incident), **server/mod startup**. |
| D6 | **Discord fully customizable "from 0":** per-event **on/off**; **editable message templates** with placeholders (`{id} {name} {player} {time} {field} {count} {action} {code} {message} {server}`); **embed look** (color / title / which fields); **webhook identity** (username + avatar) + **@role ping on error**; **+ extras owner asked to keep:** footer/timestamp toggle, **block-texture shown as the embed image/thumbnail**, **batch/rate-limit** so Discord isn't spammed by bulk ops, **per-event test**. |
| D7 | **Discord editing surface:** **chat commands now** (`/cb discord …`) **+ a shipped sensible default config** (agreed below). The full **in-game GUI editor lands in G21** — G20 builds the data model so G21 only adds the screen. Owner wants GUI **and** chat **and** good defaults; defaults ship first. |
| D8 | **`/cb backup save` auto-syncs to the cloud** (uploads after local save) whenever `cloudShareEnabled`. Message appends `(synced to cloud)`. |
| D9 | **Master switch `cloudShareEnabled`** added. Off → every vault command says "Cloud sharing is disabled," nothing hits the network. |
| D10 | **Security = all 4 layers** (see §🛡️). HARD TRUTH documented: a public jar's URL + secret are extractable; goal is **abuse-resistance, not true secrecy**. |
| D11 | **Ship model: owner's vault, cloud ON by default.** Uploads OP-only + all protections on. Owner can throttle/kill the worker **server-side instantly**, no mod update (mod fails to graceful silence). *(Alternatives considered + parked: ship blank / opt-in-off-by-default.)* |
| D12 | **Upload = OP only. Download by code = any player.** *(Alternative considered + parked: a `vaultUploadPermission = op\|all\|off` config — not chosen; OP-only is the abuse-minimal default.)* |
| D13 | **Owner control plane ("full control over each player + server, to my end"):** the mod sends a **signed identity** (player **UUID** + **server id**) on every vault request; the **worker** (owner-controlled) enforces **per-player + per-server allow/deny lists, per-entity quotas, ban, and a remote kill switch** — all without a mod update. |
| D14 | **Graceful silence preserved:** no `vaultEndpoint` / no `discordWebhookUrl` → all code paths skip silently, **zero errors in `latest.log`**. |

### Owner-approved defaults (flag if wrong)
- Cloud **ON** by default · uploads **OP-only** · downloads open by code.
- Discord: all events **ON** once a webhook is set · username **"CustomBlocks"** · embed texture **on** for create/edit/vault · footer timestamp **on** · batch window **5s** · error role ping **blank** (admin sets).
- Backup cloud-sync **on** when `cloudShareEnabled`.
- Share-code TTL **30 days** (pin to keep permanent — G20.5).

---

## 🎨 UI Decisions — owner implementation interview (2026-06-21, 5 rounds)

> This nails down **exactly how every G20 surface looks + behaves.** Grounded by reading the live
> code first (`CloudCommands`, `DiscordWebhook`, `CloudVaultClient`, `UndoManager`,
> `CategoryCommands` share pattern). **Where anything here conflicts with the older decisions
> above, this section wins.** Standing owner rule restated: **everything professional, in GUIs.**

| # | Decision |
|---|---|
| **U1 — Brand split** | **In-game chat** keeps the `[CB]` prefix + `✔/✖` glyphs (matches the locked branding rule + existing `CategoryCommands` share messages). **Discord** posts use the webhook's own identity (username **`CustomBlocks`**) — **no `[CB]` prefix** there. "Depending where" = chat vs Discord. |
| **U2 — Embed media = attach the real file** | The block PNG lives on the **localhost** pack server, which Discord **cannot** reach. So the mod **attaches the block's actual source file to the webhook POST** (multipart) — Discord then hosts + renders it. **Whatever was uploaded** (static **PNG** *or* animated **GIF**), not PNG-only. Zero public URL needed. |
| **U3 — Conflict = a client screen** *(revised 2026-06-22: was a chest GUI)* | On download, if the incoming id **already exists locally** AND a **player** ran it, open a dedicated **client screen** (matches the Block Creation Studio style). Both blocks shown as **side-by-side spinning 3D cubes** (`PreviewCube`) — an animated incoming **animates in its cube**; per-face blocks show the front face. The incoming texture/frames ride **inside the open-screen packet** (PNG bytes) and are painted live (`NativeImageBackedTexture`) → **no pack rebuild to preview**. One **editable id box**, pre-filled with the next free id (`g20a2`), used by the actions; **live-validated** (empty / taken / illegal → that button greys + red hint). Four actions: **✅ Override** (incoming takes the id, local deleted) · **➕ Keep Both** (incoming imported under the **typed** box id; local untouched) · **✏️ Rename Mine** (local block renamed to the box id; incoming takes the original id) · **❌ Cancel / ESC** (nothing). On success → share-style **sound + particle + on-screen confirm + a `/cb give <id>` chat hint**. Never a silent overwrite. **Console / no player = a plain text message** (the screen needs a client). *(Note: old "Keep Both auto-suffix g20a2" is now the editable pre-fill; old "Rename Incoming" folded into the typed Keep Both; the 4th action is now "Rename Mine".)* |
| **U4 — Conflict screen: Simple ⇄ Advanced toggle** | A view switch on the conflict screen. **Simple** = show **only the differing** attributes (yellow, `old→new`, e.g. `glow 6→0`, `sound stone→glass`). **Advanced** = the **full** attribute list for both blocks, differences highlighted. |
| **U5 — Undo = the existing `/cb undo`** | **No separate `/cb vault undo`.** Override / Keep Both / Rename record a normal edit onto the existing `UndoManager`, reverted by the existing **`/cb undo` / `/cb redo`** — multi-level, depth 100, already honors the `undoMode` config (per-player **or** server-wide). Memory-only (clears on restart/reload) **like every other edit in the mod** — consistent, not a regression. **This supersedes D4's "own stack" wording.** |
| **U6 — Backup sync-fail names the cause** | Local save OK but cloud sync fails → a **soft, human-readable** warning line that **names why**, e.g. `⚠ Couldn't sync to cloud: worker unreachable — check your internet or vaultEndpoint.` Success path still appends `(synced to cloud)` (D8). |
| **U7 — Discord batching = summary + spoiler** | Rapid same-type events inside the 5s window (D6) collapse into **ONE summary embed** (`📦 Bulk create — 20 blocks by {player}`). The full per-event list is hidden behind a **Discord spoiler tag** `||…||` — click to reveal. (Embeds can't truly collapse; spoiler is the real "click to see all" in one message.) |
| **U8 — Startup ping default ON** | The "server online" Discord event fires on every boot, **default ON**, silenceable with `/cb discord toggle startup off`. |
| **U9 — Discord chat-editing helpers** | No Discord GUI in G20 (full editor = **G21**, U12). G20 core ships these chat helpers: `/cb discord show` (print every toggle/color/template/identity), `/cb discord reset` (restore shipped defaults), `/cb discord placeholders` (list all `{…}` placeholders), `/cb discord preview <event>` (send a **fake sample** embed to tune look without a real trigger). Plus **Help-GUI entries + a dashboard chest tile** for vault & discord. |
| **U10 — Cheap-now polish folded into core** | (a) **Share juice** — cloud-whoosh **sound + particle burst + on-screen title** (`☁ Uploaded! aB3xK`) on a successful share (honors the per-event sound/particle category toggles). (b) **`/cb vault mine`** — **local** history of every code *you* generated (browsable in the Vault Hub) so a code is never lost. (c) **Player-head embed icon** — the uploader's Minecraft skin face as the Discord embed author icon (public head-render URL, Discord-reachable). (d) **Per-event role ping** — extend the error-only ping so **any** event can ping a role. |
| **U11 — Vault Hub GUI (build now)** | `/cb vault` opens a **client Screen** (superseded 2026-06-27 — no longer a chest dashboard, see §S7 below): **Upload · Download · My Codes · Recent · Settings** sections. Everything reachable by clicking (the "all GUIs" rule). `/cb vault mine` lives here as **My Codes**. |
| **U12 — Discord config GUI stays G21** | Keeps G20 small + testable fast (CLAUDE.md §4). G20 ships chat + defaults + the data model; **G21 adds the full Discord Hub editor screen** on top of that model. |
| **U13 — Wave-3 prioritized into G20 (own slices, after core)** | **Conflict "Rename incoming"** (folded into U3) · **Embed theme presets** (one click sets all Discord colors + wording: **Minimal / Colorful / Professional**) · **Milestone pings** (celebratory `🎉 100th block created!` embed) · **Quiet hours** (no Discord posts between set hours). |
| **U14 — Parked to later roadmap** (captured, not built in G20) | **Block Voucher** gift item · **Pack / multi-block share** · **Expiry + pin GUI** · **Download preview hologram** · **Discord Hub GUI** (→G21) · **Discord setup wizard** · **`/cb vault info <code>`** code-peek · **self-updating live status message** · **web gallery + QR** per code. |

---

## 🛡️ Security — the hard truth + the 4 layers (owner picked ALL four)

> **This is a public mod.** Anyone can decompile the jar or sniff their own traffic. **Any URL or secret shipped in the mod is extractable.** There is no way to truly hide the Cloudflare endpoint. The honest goal is *"assume the link leaks; make abuse pointless and cheap to absorb."* Cloudflare is well-suited to this. (Weak option explicitly rejected: base64/obfuscating the URL in the jar — defeated in minutes.)

| Layer | What | Where |
|---|---|---|
| **1. Request signing** | Mod signs every request: **HMAC(secret + body + timestamp)**; worker rejects forged or replayed (old-timestamp) requests. This is the doc's `cloudPackSecret`. Rotatable. | mod sends · worker verifies |
| **2. Rate limiting** | Cloudflare WAF rate-limit rule **+** in-worker per-IP KV counter. Caps spam + upload-flood DoS. | worker / CF |
| **3. Size cap + TTL** | **Shares** (category/note) ≤5 MB in KV, **permanent (no TTL — a library)**. **Backups** go to **R2** (no size cap; single object ≤5 GB) and auto-expire after **3 months** via an R2 bucket **lifecycle rule** (90 d). KV's 25 MiB cap + the worker's 100 MB body limit are why backups bypass both — owner R2 setup pending. | worker + R2 |
| **4. WAF + hidden front** | Custom domain in front of the raw `*.workers.dev` host; **require the mod's custom header**; Bot Fight Mode / managed WAF; free L3/L4 DDoS absorption. | CF dashboard |

**Reassurance for the owner:** you own the worker. If it's ever hammered you can **throttle, ban, or kill it server-side instantly** — the mod just fails to graceful silence (D14). "My vault + protection" is achievable that way.

---

## 🌟 Full feature roadmap — every greenlit item, phased

> Owner greenlit **all 32** brainstormed features (wave 1 + wave 2) + the core decisions. Organized as a **phased roadmap** (owner's choice) so nothing is lost and nothing is falsely "done." Build **G20 core first**, slice by slice; later phases are their own groups. **Phase letters are adjustable** — the point is each feature is captured exactly once.

### 🧱 Phase **G20 — Core** (build now)
*(the rounds 1–4 decisions, mod-side)*
1. **Vault block upload/download** — static + animated, reuse `/category` zip path (D2, D3).
2. **Conflict + multi-level undo/redo** — warn, clickable override, `/cb vault undo|redo` stack (D4).
3. **Backup → cloud auto-sync** — `/cb backup save` uploads when enabled (D8).
4. **Master switch `cloudShareEnabled`** (D9).
5. **Request signing + identity headers** — HMAC + secret + timestamp + UUID + server-id (D10/D13, mod side).
6. **OP-only upload / open download** (D12).
7. **Discord events — all 8** — create, delete, edit, bulk, backup, vault, error, startup (D5).
8. **Discord embeds + block-texture in embed** (D6, "embed shows block texture").
9. **Discord full customization** — per-event toggle, message templates, embed look, identity + role ping, footer/timestamp, batch/rate-limit, per-event test (D6).
10. **Discord chat editing + shipped defaults** (D7).
11. **Graceful silence** (D14).

### 🏪 Phase **G20.5 — Marketplace & reliability**
| Tag | Feature | What it does |
|---|---|---|
| M1 | **In-game Cloud Browser GUI** | Chest GUI: search / trending / newest / "mine", thumbnail previews, 1-click download. Makes the vault a browsable store. *(headline)* *(Distinct from G20's **Vault Hub** (U11) — the Hub manages YOUR upload/download/codes; M1 browses OTHERS' shared blocks / the store. The Hub's "Download" + "My Codes" tiles later grow into M1.)* |
| M2 | **Web gallery + QR/short-link** | Worker serves an HTML page per code (texture preview + "use code X") + QR / short link. Share outside the game. |
| M3 | **Likes + downloads + trending** | KV counters per code; powers the Trending tab. Social proof. |
| M4 | **Versioning + remix/fork** | Re-upload = new version; fork someone's block, edit, re-share with an **attribution chain** crediting the original author. |
| X1 | **Cross-server personal library** | Your uploads + favorites follow your UUID onto ANY server running the mod. `/cb vault mine`. Retention hook. |
| X2 | **Offline queue + retry + cache** | Cloud down → queue upload, auto-retry, never lose a share; cache downloads for instant re-grab. |
| X3 | **Private / password / pin** | Unlisted codes, password-protected codes, pin-permanent (skip TTL). |
| X4 | **Server backup → cloud → restore** | Push the whole server's block set to cloud, restore from one code on a fresh server. Extends D8. |
| DP3 | **Auto-post code + web link** | Sharing a block auto-drops its code + gallery link in Discord (pairs with M2). |

### 👥 Phase **G30 — Community & discovery**
| Tag | Feature | What it does |
|---|---|---|
| CM1 | **Creator profiles + follow + feed** | Public profile per creator; follow people; "new from follows" feed. |
| CM2 | **Comments + ratings** | Reviews on shared blocks (needs moderation, see CS1). |
| CM3 | **Achievements + leaderboards** | Badges ("100 downloads", "first fork"); global + per-server boards. |
| DP4 | **Weekly digest + live status embed** | Auto digest (top creators / new-block count) + one self-updating Discord message with live server stats. |
| DS1 | **NL + image search** | "find a glowing blue crystal" or upload a pic → similar blocks. Vector / perceptual search (Workers AI). |
| DS2 | **Auto-tagging + alt-text** | Vision model tags every upload (neon/medieval/meme) → better browsing + accessibility. |
| DS3 | **Duplicate/similar warning + dedup** | On upload: "looks like block X" (perceptual hash); dedups storage. |
| DS4 | **AI texture cleanup/upscale** | Workers AI sharpens/upscales on upload. **Ties the parked G15 AI work.** |

### 🎯 Phase **G31 — External creation & integrations**
| Tag | Feature | What it does |
|---|---|---|
| DP2 | **Create block FROM Discord** | Post an image URL in a channel → block appears in-game. Two-way bridge (worker interaction/bot endpoint). Heaviest. |
| FC1 | **"Send to Minecraft" browser extension** | Right-click any web image → lands in your game via the queue. Kills creation friction. |
| FC2 | **Generic inbound webhook / REST API** | Any service / third-party tool POSTs → block created. Dev ecosystem on the vault. |
| FC3 | **Block Voucher item** | A share code as a giftable in-game item another player redeems. Trading. |
| FC4 | **Twitch redeem → create live** | Viewers spend channel points → block spawns on stream (EventSub). Streamer bait. |
| CS4 | **Generic outbound webhooks** | Any event → any URL (Slack/Zapier/custom), not just Discord. Generalizes the Discord plumbing to N destinations. |

### 🛡️ Phase **G32 — Owner platform & safety**
| Tag | Feature | What it does |
|---|---|---|
| CS1 | **Content moderation + report + queue** | **Near-mandatory for a public image vault.** In-game report button, worker review/approval queue, optional auto-screen (hash blocklist or AI), mod approve/reject (Discord buttons). Protects the owner from NSFW/illegal uploads. |
| CS2 | **Remote announcement / MOTD push** | Owner pushes a message via the worker → shows in-game on ALL servers. Fleet-wide, no mod update. |
| CS3 | **Auto-update — version sync + jar download** | **Far beyond a passive checker.** On server join, server tells client its mod version; if client is older → mod auto-downloads the new jar from the Cloudflare worker (or server HTTP host), swaps the old jar out, prompts "Restart to update", player restarts + rejoins → done. Zero manual hunting. Also fires a periodic background check + admin alert in chat/Discord. Full spec below (§CS3). |
| OD1 | **Private web admin dashboard** | Usage graphs, moderation queue, ban list, MOTD editor, kill switch, remote feature flags. The "customized to my end" endgame. |
| OD2 | **Opt-in telemetry + remote error reporting** | Sentry-style: crashes/errors phone home (opt-in, anonymized) → owner fixes before players report. |
| OD3 | **Per-server tiers/quotas + global bad-hash blocklist + audit log** | Deep moderation/control. |
| OD4 | **`/cb cloud status` + friendly rate-limit msgs + CDN signed URLs** | Health (latency/quota/worker health), polite "slow down" messages, CDN-cached signed texture URLs (fast, cheap, anti-hotlink). |
| CM4 | **Owner-curated featured packs** | Owner pushes "Staff Picks" / seasonal event packs from the worker (surfaces in the M1 browser). |

### ☁️ Worker track — **owner's Cloudflare deploys** (parallel to all phases)
> I can write these, but **they deploy to the owner's worker, not the mod jar.** A green mod build never marks them done.

| Tag | Worker work | Supports |
|---|---|---|
| W1 | HMAC signature verify + replay window | Security L1, all vault calls |
| W2 | Per-IP rate limiting (CF rule + KV counter) | Security L2 |
| W3 | Upload size cap + share-code TTL auto-expire | Security L3 |
| W4 | WAF / Bot Fight + custom-domain front + required header | Security L4 |
| W5 | Control plane: per-player + per-server allow/deny, quotas, ban, remote kill | D13 |
| W6 | Storage: reuse `/category` (or `/block`) + perceptual-hash dedup | G20 core, DS3 |
| W7 | list/search/trending routes · web-gallery HTML · version store · profile store · likes counters | G20.5, G30 |
| W8 | moderation queue + review API · MOTD · telemetry sink · admin dashboard | G32 |
| W9 | **Version + download endpoint** — `GET /version` returns `{version, sha256, downloadUrl, changelog}`; `GET /download/latest` serves the jar (or R2 signed URL). Owner uploads new jars via dashboard or `PUT /version` (authed). | CS3 (G32) |

---

## 🎚️ Shipped default Discord config (owner approves → becomes the default)

> Owner: "agree a good default here, that becomes the shipped default." Draft below — **flag any wording/color to change.**

- **Events:** all ON when a webhook is set (**startup included** — U8).
- **Theme:** `Professional` preset (U13) — sets the colors + templates below; per-field edits override it.
- **Identity:** username `CustomBlocks` · avatar blank · **per-event role ping blank** (U10d; admin sets, applies to any event not just error).
- **Embed media:** the block's **actual file attached** (PNG **or** GIF — U2) · author icon = **uploader's player head** (U10c) · on for create / edit / vault.
- **Footer timestamp:** on · **batch:** 5s window → **one summary embed + spoiler list** (U7) · **quiet hours:** off (blank) by default (U13).
- **Per-event color + template:**

| Event | Color | Default template |
|---|---|---|
| create | `#57F287` green | `🟢 Block Created — {id} ({name}) by {player}` |
| delete | `#ED4245` red | `🔴 Block Deleted — {id} by {player}` |
| edit | `#5865F2` blurple | `✏️ Block Edited — {id} ({field}) by {player}` |
| bulk | `#FEE75C` gold | `📦 Bulk {action} — {count} blocks by {player}` |
| backup | `#1ABC9C` teal | `💾 Backup Saved — {name} ({count} blocks)` |
| vault | `#9B59B6` purple | `☁️ Vault {action} — {id} → code {code}` |
| error | `#992D22` dark red | `⚠️ Error — {message}` |
| startup | `#95A5A6` grey | `🚀 CustomBlocks online — {count} blocks · {server}` |

---

## ⚙️ Config fields to add

| Field | Default | Purpose |
|---|---|---|
| `cloudShareEnabled` | `true` | Master switch (D9). |
| `vaultEndpoint` | (your url, D11) | Worker base (already exists; `cloudShareUrl` is the doc alias). |
| `cloudPackSecret` | (shipped secret) | HMAC signing key (D10 L1; extractable — see hard truth). |
| `cloudBackupSync` | `true` | Auto-sync backups (D8). |
| `discordWebhookUrl` | `""` | Already exists. Blank by default → silence. |
| `discordNotify<Event>` | `true` | Per-event on/off ×8 (D6). |
| `discordMsg<Event>` | (defaults above) | Per-event template (D6). |
| `discordColor<Event>` | (defaults above) | Per-event embed color (D6). |
| `discordWebhookUsername` | `CustomBlocks` | Webhook identity (D6). |
| `discordWebhookAvatar` | `""` | Webhook avatar (D6). |
| `discordErrorRoleId` | `""` | @role ping on error (D6). |
| `discordEmbedTexture` | `true` | Attach the block's real file (PNG/GIF) to the embed (D6, **U2**). |
| `discordTimestampFooter` | `true` | Footer timestamp (D6). |
| `discordBatchSeconds` | `5` | Collapse rapid events → summary embed + spoiler list (D6, **U7**). |
| `discordRolePing<Event>` | `""` | Optional @role ping **per event**, not just error (**U10d**). `discordErrorRoleId` stays as the error alias. |
| `discordTheme` | `professional` | Embed preset (`minimal`/`colorful`/`professional`) that bulk-sets colors+templates; per-field edits win (**U13**). |
| `discordQuietStart` | `""` | Quiet-hours start `HH:mm`; blank = always post (**U13**). |
| `discordQuietEnd` | `""` | Quiet-hours end `HH:mm` (**U13**). |
| `discordMilestones` | `true` | Celebratory Nth-block pings (**U13**). |
| `vaultShareEffects` | `true` | Sound + particle + on-screen title on a successful share (**U10a**); honors the sound/particle category toggles. |

> **New data file:** `config/customblocks/data/vault_codes.json` — owned by a new `VaultHistoryManager` (local "my codes" history, **U10b**). Follows the `config/customblocks/data/` path convention.

---

## 🧱 G20 core — build slices (one at a time, in-game confirm between each)

| Slice | Builds | Notes |
|---|---|---|
| **S1** | Vault block upload/download core **+ share juice** | reuse `/category` zip (json + texture(s) + sidecar); gate on `cloudShareEnabled`; OP-gate upload; static **and** animated. Upload msg keeps `[CB]` brand + a **`[click to copy]`** button (trailing `/cb vault download` hint removed, 2026-06-22). Sound + particle + on-screen title on success (U1, U10a). |
| **S2** | Conflict resolve (screen spec → `GROUP_27_SCREENS.md` §G27.17) | On id clash (player present) open the screen — full look/buttons in G27.17. Each action records to the **existing `UndoManager`** → plain `/cb undo` / `/cb redo`; success juice + `/cb give` hint. **Console = plain text message.** (U3, U4, U5) |
| **S3** | Backup → cloud sync | `/cb backup save` uploads when enabled → `(synced to cloud)`; **sync-fail = soft warning naming the cause** (U6). |
| **S4** | Request signing + identity | HMAC + secret + timestamp + UUID + server-id header (mod side of security; **needs worker W1/W5 to fully verify**). |
| **S5** | Discord events + embeds | wire all 8 events; embed builder; **attach the real file (PNG/GIF)** (U2); **player-head author icon** (U10c); **summary+spoiler batching** (U7); **startup default-on** (U8). |
| **S6** | Discord customization + chat helpers + defaults | per-event toggle/template/color/identity/**per-event role-ping** + `/cb discord show\|reset\|placeholders\|preview\|test\|toggle\|msg\|color` chat commands + shipped defaults + Help-GUI/dashboard entries (U9, U10d). |
| **S7** | **Vault Hub GUI** | `/cb vault` **client Screen** (not chest — superseded 2026-06-27): **Upload · Download · My Codes · Recent · Settings**; `/cb vault mine` (local `vault_codes.json` history) lives here as **My Codes** (U11, U10b). |
| **S8** | **Wave-3 extras** | **Embed theme presets** (Minimal/Colorful/Professional) · **Milestone pings** · **Quiet hours** (U13). *(Conflict "Rename incoming" already folded into S2.)* |

*(Parked to later: Block Voucher · pack share · expiry+pin GUI · download hologram · **Discord Hub editor GUI → G21** · setup wizard · `/cb vault info` · live-status message · web gallery+QR — U14. Marketplace browser, cross-server, community, external creation, owner dashboard remain their later phases.)*

---

## 🧩 S2 — Conflict Screen — build pieces (screen spec moved to G27)

> The screen itself (look, buttons, behavior, owner decisions) is **fully specced in
> `GROUP_27_SCREENS.md` §G27.17** (folded 2026-06-22, re-confirmed as single source of truth 2026-07-12) —
> don't duplicate here. G20 keeps the non-screen build pieces:

1. **Open-screen packet** (S2C): code + your-stats + incoming-stats + incoming frame bytes + animated flag.
2. **Result packet** (C2S): code + action enum (`OVERRIDE` / `KEEP_BOTH` / `RENAME_MINE`) + typed id.
3. **`VaultBlockCodec`** additions — `peek(zip)` (read incoming attrs + frame bytes without importing) + `unpackAs(zip, forcedId)` (import under a chosen id).
4. **Server result handler** — maps action → delete / reid / import + rebuild + undo record + juice.
5. **Wire** the conflict branch in `vaultDownload` (player + clash → send open packet; else text) and **register** both packets.

---

## ⛔ Parked / not-now (captured so nothing is lost)

| Item | Note |
|---|---|
| Dedicated `/block` worker route | Reuse `/category` instead (D3). Revisit only if zip path limits. |
| Ship-blank / opt-in-off ship models | Not chosen (D11) — owner ships their vault, ON. |
| `vaultUploadPermission` config | Not chosen (D12) — OP-only is fixed. |
| Separate `/cb vault undo` stack | **Dropped (U5).** Override rides the existing `UndoManager` → plain `/cb undo`/`/cb redo`. D4's own-stack wording superseded. |
| In-game Discord **GUI editor** (Discord Hub) | Deferred to **G21** (D7, U12); G20 ships chat helpers + defaults + the data model the G21 screen sits on. |
| Block Voucher · pack share · expiry+pin GUI · download hologram · setup wizard · `/cb vault info` · live-status msg · web gallery+QR | **Captured, not built in G20 (U14).** Later phases (G20.5 / G30 / G31). |
| AI texture upscale (DS4) | Ties parked **G15** AI work — coordinate when G15 unparks. |

## 🔮 Brainstorm wave 3 — FUTURE (owner: "could brainstorm later, mark that")
Not yet brainstormed. Candidate focus areas offered, awaiting the owner's pick: **monetization** · **anti-grief** · **accessibility** · **performance**. Plus open space for more. *Revisit on request.*

---

## 🔄 §CS3 — Auto-Update: Version Sync + Jar Download

> **Origin:** Owner brainstorm 2026-07-08 — *"I am desperate for a way to join a server that has a newer version of CustomBlocks while having an older one locally, where it auto downloads itself and I just rejoin and it fixes itself, no brain damage."*
>
> **Phase:** G32 (Owner platform & safety). **Depends on:** the existing embedded resource-pack HTTP host (`ResourcePackServer`, deployed). **No Cloudflare worker, no R2** — the server is the single download source (owner decision 2026-07-11).
>
> **Hard constraint:** Java locks `.jar` files in memory at startup. You **cannot** hot-swap a running mod. A game restart is the one unavoidable step — but everything else (detecting, downloading, swapping) is fully automatic.
>
> ⚠️ **This §CS3 was rewritten 2026-07-11 to a server-source-only design.** Dropped from the original brainstorm: Cloudflare worker/R2 jar-hosting, the `autoUpdateSource` fallback chain (only one source now — the server itself), the AU7 periodic background check (detection is join-only), and the AU8 changelog (parked). See the decision table for the current locked spec.

### The problem

Server owner updates CustomBlocks on their server. Players still have the old jar in their `mods/` folder. Right now:
- Fabric's handshake may kick them with a cryptic "mismatched mod channel" error, OR
- They connect but the mod breaks silently because the server sends packets/data the old client doesn't understand.

Either way the player has to **manually** find the new jar, download it, put it in their mods folder, and restart. Brain damage.

### The solution — zero-effort auto-update flow

**Step-by-step (player's perspective):**

1. Player launches Minecraft with CustomBlocks v1.3 in their `mods/` folder.
2. Player clicks "Connect" to a server running CustomBlocks v1.5.
3. During the Fabric network handshake, the server sends its mod version (`1.5.0`) to the client.
4. Client detects mismatch: `1.3.0 < 1.5.0`.
5. **Instead of just getting kicked**, the mod:
   - Shows an in-game screen: *"Server has CustomBlocks v1.5 — you have v1.3. Downloading update…"*
   - Downloads the new jar from the **server's own HTTP host** (`GET /download/latest`) — the same embedded `ResourcePackServer` that already serves the resource pack. No cloud, no worker.
   - Verifies the download (SHA-256 hash from the server's `/version` endpoint).
   - Saves the new jar to a staging folder (`config/customblocks/updates/`).
   - Swaps: renames the old jar in `mods/` to `.disabled`, moves the new jar in.
   - Shows: *"✅ CustomBlocks updated to v1.5! Restart your game to apply."* with a **[Restart Now]** button (calls `MinecraftClient.getInstance().scheduleStop()` → clean shutdown, player relaunches the launcher) and a **[Later]** button.
6. Player restarts, rejoins the server. Done. No hunting, no brain damage.

**Step-by-step (server owner's perspective):**

1. Owner builds a new jar, drops it in the server's `mods/` folder, restarts the server.
2. On boot the server SHA-256's its own `mods/customblocks-*.jar` and serves it over its existing HTTP host — **no separate upload step, no dashboard**.
3. Every player who connects with an older version gets the auto-update flow above. Zero support tickets.

### Decisions

| # | Decision |
|---|---|
| AU1 | **Single download source = the server's own HTTP host.** The client downloads the jar from `GET /download/latest` on the same embedded `ResourcePackServer` (`http://<httpHost>:<port>/download/latest`) that already serves the resource pack — the client is already reachable to that port because it pulls the pack ZIP from it. **No Cloudflare worker, no R2, no `autoUpdateSource` field** (dropped 2026-07-11). Works fully offline on a LAN. |
| AU2 | **Version comparison = semver.** Mod embeds its version from `fabric.mod.json` (read via `FabricLoader`). Server sends its version in a custom S→C handshake packet on JOIN. Client compares. Only auto-updates when client is **older** (a newer client on an older server → warning toast only, see AU9). |
| AU3 | **Integrity = SHA-256 hash.** The server's `GET /version` endpoint returns `{version, sha256, downloadUrl}`. After download, client verifies the hash before touching the `mods/` folder. Hash mismatch → abort + error message, never replace with a corrupt file. |
| AU4 | **Jar swap = safe rename dance.** Old jar renamed to `customblocks-old-v1.3.0.jar.disabled` (Fabric ignores `.disabled`). New jar placed as `customblocks-1.5.0.jar`. On next boot Fabric loads the new one. If the new jar fails to load → player can manually delete it and rename `.disabled` back. |
| AU5 | **Player consent = soft.** The update screen explains what's happening and gives **[Restart Now]** (calls `MinecraftClient.getInstance().scheduleStop()` → clean quit to desktop; player relaunches the launcher, Fabric loads the new jar at JVM start) / **[Later]**. Never force a restart. If they pick Later, the download is still staged — next launch picks it up. |
| AU6 | **Server-side config: `autoUpdateEnabled`** (default `true`). Server owners can disable this entirely if they don't want clients auto-updating (e.g., locked modpacks). When off, a version-mismatch just shows a warning toast with the version numbers — no download attempt. Sent to the client inside the handshake packet. |
| AU7 | ~~Periodic background check.~~ **DROPPED 2026-07-11.** Detection is join-only (the handshake packet). No startup/interval polling, no OP background alert, no `update_available` Discord event. (The Discord event system §E/F is unbuilt anyway.) |
| AU8 | ~~Changelog in the update screen.~~ **PARKED 2026-07-11.** The update screen shows the version numbers only; no changelog string is fetched or displayed. Revisit once there's a place for the owner to author changelog text. |
| AU9 | **Newer-client-on-older-server = warning only.** If a player with v1.5 joins a server running v1.3, **no auto-downgrade** (dangerous — they might play on multiple servers). Just a yellow warning toast: *"This server runs an older version of CustomBlocks (v1.3). Some features may not work."* |
| AU10 | **Security — the server is the trusted source.** The jar comes from the server's own `mods/` folder over its own HTTP host; the SHA-256 in the `/version` response is the integrity proof. No third-party URLs, no Modrinth/CurseForge redirect, no cloud middleman. This is a **trusted environment** — the player already trusts this server enough to run its resource pack from the same host. |

### Config fields (added to `CustomBlocksConfig`)

| Field | Default | Side | Purpose |
|---|---|---|---|
| `autoUpdateEnabled` | `true` | server | Master switch. Off → mismatch shows a warning toast only, no download. |

*(`autoUpdateSource` and `autoUpdateCheckInterval` removed 2026-07-11 — single source, join-only detection.)*

### Network packets

| Packet | Dir | Payload | When |
|---|---|---|---|
| `VersionHandshakePayload` | S→C | `serverModVersion` (string), `downloadUrl` (string), `sha256` (string), `autoUpdateEnabled` (bool) | On `ServerPlayConnectionEvents.JOIN`, alongside the other join payloads (SilentPack, TransparentBg, etc.). |
| *(no C→S needed)* | — | — | Client handles everything locally — download, swap, prompt. Server doesn't need to know. |

### Server endpoints (added to `ResourcePackServer`)

| Route | Method | Auth | Response |
|---|---|---|---|
| `/version` | GET | none (LAN-trusted) | `{"version": "1.5.0", "sha256": "abc123...", "downloadUrl": "http://<host>:<port>/download/latest"}` |
| `/download/latest` | GET | none (LAN-trusted) | The jar bytes, served straight from the server's own `mods/customblocks-*.jar`. |

### Implementation classes (estimated)

| Class | Responsibility | Size |
|---|---|---|
| `update/ServerJarInfo` | Server-side: on boot, locate `mods/customblocks-*.jar`, SHA-256 it, cache `{version, sha256, file}`. Backs the two HTTP contexts. | ~90 lines |
| `update/VersionCompare` | Pure semver string comparison (older / same / newer). | ~50 lines |
| `update/JarUpdater` | Client-side: downloads jar from server, verifies SHA-256, performs the rename-swap dance in the client's `mods/`. | ~130 lines |
| `update/UpdateScreen` | Client screen shown on version mismatch — **full spec in `GROUP_27_SCREENS.md` §G27.23** (2026-07-12). | ~150 lines |
| `network/VersionHandshakePayload` | S→C packet with server's mod version + download info + `autoUpdateEnabled`. | ~45 lines |
| Seam in `CustomBlocksMod` | Register the handshake payload; send it in the JOIN handler. | ~10 lines |

### Server HTTP host = the only download source (AU1)

The mod already runs an embedded HTTP server for resource-pack delivery (`ResourcePackServer`, `httpHost`/`httpPort`, default `127.0.0.1:8123`). §K reuses it directly — two new contexts, one port, already reachable by any client that can pull the resource pack:
- `GET /version` → the `ServerJarInfo` metadata JSON.
- `GET /download/latest` → the jar bytes from the server's own `mods/` folder.
- Fully offline / LAN-friendly. No cloud setup, no worker, no R2.

### Test rows (future — added to TG20 §K when built)

| # | Action | Expected Result | SP | MP |
|---|---|---|---|---|
| K1 | Join server with **same version** | No update prompt, normal join. | ➖ | 🟥 |
| K2 | Join server with **newer version**, `autoUpdateEnabled=true` | Update screen appears, jar downloads from server, progress bar fills, "Restart" button shown. | ➖ | 🟥 |
| K3 | After K2: restart game, rejoin same server | Loads new version, no update prompt, clean join. | ➖ | 🟥 |
| K4 | Join server with **newer version**, `autoUpdateEnabled=false` | Warning toast only ("Server has vX, you have vY"), no download attempt. | ➖ | 🟥 |
| K5 | Join server with **older version** (client is newer) | Yellow warning toast only, no downgrade. | ➖ | 🟥 |
| K6 | Corrupt download (hash mismatch) | Abort, error message, old jar untouched. | ➖ | 🟥 |

*(Old K5 "worker fallback" removed — single source. Old K8 "periodic check" removed — join-only detection. 2026-07-11.)*

---

## ✅ G20 core verdict (full slice tests live in `Reports/GROUP_20_TESTING_GUIDE.md`)

| Slice | Proves | Result |
|---|---|---|
| S1 | Block upload→code, download→restore (static + animated), gating, OP-gate, share juice | ✅ 6/6 passed 2026-06-27; command/GUI aliases wired 2026-06-27 |
| S2 | Conflict **screen** (3D cubes; Override / Keep Both (typed) / Rename Mine / Cancel) + Simple⇄Advanced diff + plain `/cb undo` reverts + success juice | ⏳ not built |
| S3 | Backup syncs to cloud (R2 direct); sync-fail names the cause | 🟡 built; backups→R2, awaiting owner R2 setup + in-game test |
| S4 | Signed requests + identity (needs worker verify) | ⏳ not built |
| S5 | All 8 events fire as embeds; real file (PNG/GIF) attached; player-head icon; summary+spoiler batch; silence when blank | ⏳ not built |
| S6 | Per-event toggles/templates/colors/identity/per-event role-ping + chat helpers (show/reset/placeholders/preview) + defaults | ⏳ not built |
| S7 | Vault Hub GUI (Upload/Download/My Codes/Recent/Settings) + `/cb vault mine` history | ⏳ not built |
| S8 | Embed theme presets + milestone pings + quiet hours | ⏳ not built |

**Group 20 (core) passes when block share + Discord both work in-game.** Worker-side hardening + control plane = owner's separate deploy track (W1–W8), confirmed on the owner's Cloudflare, not by a mod build.
