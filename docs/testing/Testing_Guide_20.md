# Group 20 - Cloud Vault, Discord & External Integrations

## Status

| | |
| --- | --- |
| **Verdict** | Block vault sharing is confirmed; backup R2 setup, conflict screen, Discord, Vault Hub, and signing remain open. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-06-28 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| C | Backup cloud sync to R2 | Built 🎯 | Blocked ‼️ |
| D | Vault conflict import screen handoff | Designed ⏳ | Discussion ✏️ |
| E | Discord event embeds | Designed ⏳ | Discussion ✏️ |
| F | Discord customization and chat helpers | Designed ⏳ | Discussion ✏️ |
| A | Block vault share and master gate | Done ✅ | - |
| B | Vault code history and category share tile | Done ✅ | - |
| G | Vault Hub Screen | Designed ⏳ | Parked 💤 |
| H | Request signing, identity, and owner control plane | Planned 📜 | Parked 💤 |
| I | Wave-3 Block Network extras | Planned 📜 | Parked 💤 |
| J | Auto-update handoff to G32 | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_20_EXTERNAL_INTEGRATIONS.md](../groups/GROUP_20_EXTERNAL_INTEGRATIONS.md)

---

# Active Tests

## 💡 Setup

- Use a server with internet access and `vaultEndpoint` configured.
- Test `cloudShareEnabled=true` and `cloudShareEnabled=false`.
- Use OP account for upload/share paths; use non-OP for permission checks.
- Keep one static block and one animated block available.
- Worker-side changes require owner Cloudflare/R2 deploy evidence; a mod jar alone cannot confirm them.

## A - Block vault share and master gate - Done ✅

| | |
| --- | --- |
| **Check** | Whole-block cloud share uploads/downloads static and animated blocks, while the master switch gates every cloud path. |
| **Pass rule** | Static, animated, OP upload, download, import alias, export tile, master-off, category/note gate, conflict-free import, and share effects rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Upload a static block through `/cb export <id> vault` or the Vault tile. | A code is produced with a click-to-copy style message and share feedback. | ✅ | ✅ |
| A2 | Download/import that code on a clean id. | Block data and texture restore correctly. | ✅ | ✅ |
| A3 | Upload and download an animated block. | Texture grid and animation sidecar survive the round trip. | ✅ | ✅ |
| A4 | Try upload as non-OP. | Upload is denied; downloading by code remains allowed if that is still the chosen model. | ✅ | ✅ |
| A5 | Set `cloudShareEnabled=false`, then try block upload/download. | The exact disabled message appears and no network request is made. | ✅ | ✅ |
| A6 | With master off, try category share/import, lore share/import, and conflict re-fetch. | All cloud paths are gated by the same master switch. | ✅ | ✅ |
| A7 | Use command aliases and dashboard tiles for vault share/import. | Every entry point routes to the same working path. | ✅ | ✅ |
| A8 | Cause a normal no-conflict import. | Import completes without opening a conflict screen. | ✅ | ✅ |

## B - Vault code history and category share tile - Done ✅

| | |
| --- | --- |
| **Check** | Generated vault codes are remembered server-wide, and category edit share is no longer a dead tile. |
| **Pass rule** | Code logging, newest order, restart, kind/label, category tile, disabled/unconfigured, and stale-stub rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Upload a block, category, or note. | `/cb vault codes` records kind, label, code, and newest order. | ✅ | ✅ |
| B2 | Restart server and run `/cb vault codes`. | History persists from `vault_codes.json`. | ✅ | ✅ |
| B3 | Generate many entries. | History stays bounded to the configured newest entries without corrupting old rows. | ✅ | ✅ |
| B4 | Open Category Edit and press Share. | Tile runs category share instead of showing a dead placeholder. | ✅ | ✅ |
| B5 | Press category Share while cloud is disabled or unconfigured. | Existing command gates show the correct message; the tile itself does not bypass them. | ✅ | ✅ |
| B6 | Verify old generic upload/download stubs are gone. | No dead stub path is callable from commands or tiles. | ✅ | ✅ |

## C - Backup cloud sync to R2 - Built 🎯

| | |
| --- | --- |
| **Check** | Manual backup save should sync large zip backups directly to R2 without breaking local backup success. |
| **Pass rule** | Owner R2 setup, local success, cloud success, cloud failure, size wall, code history, and auto-backup skip rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Owner must finish R2 bucket, lifecycle, token, worker vars, and redeploy before this can be confirmed. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Configure R2 bucket `cb-backups`, lifecycle, token, and worker vars, then redeploy worker. | Worker accepts backup signing requests. | 🎯 | 🎯 |
| C2 | Run `/cb backup save` with cloud enabled. | Local backup succeeds first, then message appends cloud sync success. | 🎯 | 🎯 |
| C3 | Save a large backup over KV limits. | Upload uses presigned direct-to-R2 PUT and does not hit KV size walls. | 🎯 | 🎯 |
| C4 | Break worker/R2 intentionally and save backup. | Local save still succeeds; warning names the cloud failure cause. | 🎯 | 🎯 |
| C5 | Run `/cb vault codes`. | Backup sync logs a code/history entry with kind and label. | 🎯 | 🎯 |
| C6 | Trigger auto-backup or safety backup. | It does not sync unless the manual backup path is used. | 🎯 | 🎯 |

## D - Vault conflict import screen handoff - Designed ⏳

| | |
| --- | --- |
| **Check** | When an imported block id already exists, G20 server logic must open the G27 conflict screen and apply the chosen result safely. |
| **Pass rule** | Open packet, preview bytes, override, keep both, rename mine, cancel, console fallback, undo, and conflict re-fetch gate rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen look/behavior belongs to G27; G20 owns packets, codec, result handling, and undo recording. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Import a code whose id already exists as a player. | G27 conflict Screen opens with local and incoming previews. | ⏳ | ⏳ |
| D2 | Choose Override. | Incoming block replaces local id and `/cb undo` restores the previous state. | ⏳ | ⏳ |
| D3 | Choose Keep Both with a typed id. | Incoming block imports under the typed valid id; local block remains. | ⏳ | ⏳ |
| D4 | Choose Rename Mine. | Local block moves to typed id and incoming takes the original id. | ⏳ | ⏳ |
| D5 | Cancel or press Esc. | Nothing changes. | ⏳ | ⏳ |
| D6 | Trigger conflict from console/no player. | Plain text conflict message appears instead of trying to open a client screen. | ⏳ | ⏳ |
| D7 | Set master switch off before conflict re-fetch/action. | Network action is blocked by the same disabled message. | ⏳ | ⏳ |

## E - Discord event embeds - Designed ⏳

| | |
| --- | --- |
| **Check** | Discord notifications should be real embeds with attached block media, batching, player identity, and clean silence when blank. |
| **Pass rule** | Create, delete, edit, bulk, backup, vault, error, startup, media attach, batching, role ping, and blank-webhook rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Current Discord path is not the full event system. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Set a webhook and create a block. | Discord receives a CustomBlocks embed with player, id, name, and texture media when available. | ⏳ | ⏳ |
| E2 | Delete, edit/retexture, and run a bulk op. | Each event uses the correct color/template and batches rapid events into one summary with spoiler details. | ⏳ | ⏳ |
| E3 | Save backup and upload/download through vault. | Backup and vault events post with useful codes/status. | ⏳ | ⏳ |
| E4 | Trigger an incident/error. | Error embed can ping configured role and contains the human-readable message. | ⏳ | ⏳ |
| E5 | Start server with startup enabled. | Startup event posts once per boot. | ⏳ | ⏳ |
| E6 | Clear webhook URL. | All Discord paths skip silently with no log spam. | ⏳ | ⏳ |
| E7 | Test PNG and GIF block media. | Discord receives actual attached media, not a localhost URL that cannot render. | ⏳ | ⏳ |

## F - Discord customization and chat helpers - Designed ⏳

| | |
| --- | --- |
| **Check** | Admins can tune Discord defaults by chat commands now, while the full GUI editor belongs to G21. |
| **Pass rule** | Show, reset, placeholders, preview, test, toggle, template, color, identity, role ping, theme, quiet hours, and defaults rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | G20 owns data model and chat helpers; G21 owns the eventual GUI editor. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb discord show`. | Every toggle, color, template, identity, ping, batch, and quiet-hours value is listed clearly. | ⏳ | ⏳ |
| F2 | Run `/cb discord placeholders`. | Available placeholders are explained without jargon. | ⏳ | ⏳ |
| F3 | Run `/cb discord preview <event>`. | A fake sample embed posts without needing a real block action. | ⏳ | ⏳ |
| F4 | Toggle one event off and trigger it. | That event stays silent while other enabled events still post. | ⏳ | ⏳ |
| F5 | Change template, color, username, avatar, or per-event role ping. | Future embeds use the saved values. | ⏳ | ⏳ |
| F6 | Run `/cb discord reset`. | Shipped professional defaults restore. | ⏳ | ⏳ |
| F7 | Configure quiet hours or a theme preset if included in this slice. | Output follows the chosen schedule/theme. | ⏳ | ⏳ |

## G - Vault Hub Screen - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb vault` should become a client Screen for Upload, Download, My Codes, Recent, and Settings. |
| **Pass rule** | Open, upload, download, my codes, recent, settings, disabled state, and G27 layout rows pass after the screen is built. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Parked QoL; chat commands remain the current usable path. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Run `/cb vault`. | Vault Hub Screen opens with Upload, Download, My Codes, Recent, and Settings areas. | ⏳ | ⏳ |
| G2 | Upload from the Screen. | Same block vault path as commands runs and logs history. | ⏳ | ⏳ |
| G3 | Download from the Screen. | Same import/conflict behavior as commands runs. | ⏳ | ⏳ |
| G4 | Open My Codes. | Local/server code history is browsable and copyable. | ⏳ | ⏳ |
| G5 | Turn cloud off. | Screen shows disabled state without hitting network. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Block vault share — ✅ `2026-06-27`
- §A Master cloud gate — ✅ `2026-06-28`
- §B Vault code history — ✅ `2026-06-28`
- §B Category share tile — ✅ `2026-06-28`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §G Vault Hub Screen — 💤 `2026-06-27`: Chat commands remain usable until QoL screen work resumes.
- §H Request signing, identity, and owner control plane — 💤 `2026-06-27`: Future owner web dashboard/control plane track.
- §I Wave-3 Block Network extras — 💤 `2026-06-21`: Marketplace, cross-server library, community, external creation, and web extras are later phases.
- §J Auto-update handoff to G32 — 💤 `2026-07-11`: Server-source jar update flow belongs to G32.
- Dedicated `/block` worker route — 💤 `2026-06-21`: Reuse category zip unless it proves limiting.
- In-game Discord GUI editor — 💤 `2026-06-21`: G21 owns it.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Separate `/cb vault undo` stack — 👎 `2026-06-22`: Use existing `/cb undo` and `/cb redo`.
- Ship-blank cloud model — 👎 `2026-06-21`: Owner ships their vault on by default.
- Periodic auto-update check — 👎 `2026-07-11`: Join-only detection belongs to G32.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove temporary test block codes if they expose private data.
- [ ] Delete temporary local import blocks after round-trip tests.
- [ ] Keep worker secrets and R2 keys out of TG notes.
- [ ] Keep conflict Screen findings in G27.
- [ ] Keep Discord GUI editor findings in G21.

</details>
