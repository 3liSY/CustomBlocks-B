# Group 33 - CB Control Center

## Status

| | |
| --- | --- |
| **Verdict** | The CB Control Center is fully planned and parked; SRBProjects, presence, revocation, red-X enforcement, live updates, and emergency controls still need building. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-07-18 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | SRBProjects dashboard and automatic discovery | Planned 📜 | Parked 💤 |
| B | Person, server, and CB-copy blocking | Planned 📜 | Parked 💤 |
| C | Complete CB refusal and red-X state | Planned 📜 | Parked 💤 |
| D | Live updates, saved restrictions, and outages | Planned 📜 | Parked 💤 |
| E | Confirmed emergency CB pause | Planned 📜 | Parked 💤 |
| F | Security and honest control limits | Planned 📜 | Parked 💤 |
| G | Later integrations and organization extras | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_33_CB_CONTROL_CENTER.md](../groups/GROUP_33_CB_CONTROL_CENTER.md)

---

# Active Tests

## 💡 Setup

- Resume only when Group 33 leaves parked scope.
- Use one SRBProjects test build, two official CB client copies for one Minecraft account, a second Minecraft account, two official CB servers, and one singleplayer world.
- Keep one client/server online and one offline so live and waiting behavior can be compared.
- Provide a safe way to simulate the owner service becoming unavailable without stopping Minecraft.
- Use temporary owner reasons and remove all G33 test restrictions during Cleanup.

## A - SRBProjects dashboard and automatic discovery - Planned 📜

| | |
| --- | --- |
| **Check** | SRBProjects presents one organized, truthful view of official CB clients, people, and servers after their jars actually start. |
| **Pass rule** | First launch, offline retention, duplicate-copy grouping, server presence, new-user notification, and unknown-value rows pass twice. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Start Minecraft with official CB for a Minecraft account the panel has never seen and do not join a server. | The person appears immediately, is marked online, and produces one clickable new-user notification in SRBProjects. | 📜 | 📜 |
| A2 | Close that Minecraft client. | The person remains in the list as offline with the last-seen time; the panel does not claim the jar still exists. | 📜 | 📜 |
| A3 | Start official CB for the same Minecraft account from a second CB copy. | The main list keeps one person row and shows a duplicate mark/count that opens both CB-copy entries. | 📜 | 📜 |
| A4 | Start and stop an official CB server. | The server appears while running, shows its connected official-CB users, then remains as an offline remembered server after shutdown. | 📜 | 📜 |
| A5 | Inspect a client/server that has not reported a server name or another optional fact. | The missing value displays `-`; no invented value, IP identity, or stale online state appears. | 📜 | 📜 |
| A6 | Place a CB jar in a stopped test profile without ever starting Minecraft/server. | No entry appears and the panel never claims it can detect an unrun jar sitting in `mods`. | 📜 | 📜 |

## B - Person, server, and CB-copy blocking - Planned 📜

| | |
| --- | --- |
| **Check** | The owner can block exactly one account, server, or CB copy permanently or temporarily with a required reason. |
| **Pass rule** | Scope, duration, required reason, account-follow, expiry, and unblock rows pass without one scope leaking into another. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Try to confirm a block with an empty reason. | SRBProjects refuses to apply it and clearly asks for the required reason. | 📜 | 📜 |
| B2 | Block one Minecraft account permanently with a reason while two CB copies for that account are online. | Both copies lose CB, and another account remains unaffected. | 📜 | 📜 |
| B3 | Block one person only on server A while the same person also uses server B. | CB is unavailable on server A and remains normally usable on server B and in singleplayer. | 📜 | 📜 |
| B4 | Block only one of two known CB copies for the same account. | The selected copy loses CB and the other copy remains allowed. | 📜 | 📜 |
| B5 | Apply a temporary block with a chosen end time. | The restriction stays active until that time, expires once, and does not return after reconnect or restart. | 📜 | 📜 |
| B6 | Add a new official CB copy after its Minecraft account already has an account-wide block. | The new copy receives the existing account block as soon as it connects. | 📜 | 📜 |
| B7 | Unblock an online person, then unblock an offline person. | The online person regains CB live; the offline person receives the unblock on their next successful connection. | 📜 | 📜 |

## C - Complete CB refusal and red-X state - Planned 📜

| | |
| --- | --- |
| **Check** | A blocked person remains in Minecraft but cannot use any CB path and sees one consistent red-X state. |
| **Pass rule** | Commands, menus, tools, edits, block interaction, packet refusal, red-X visuals, reason timing, and ordinary-Minecraft rows pass twice. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Join an official CB server while account-blocked. | The player stays on the server, receives no join spam, and can use ordinary Minecraft normally. | 📜 | 📜 |
| C2 | While blocked, try CB commands, menus, tools, creation, editing, and CB block interaction. | Every CB path refuses to act, changes no data, and shows the owner's reason only when CB is attempted. | 📜 | 📜 |
| C3 | Look at placed CB blocks and inspect CB items in inventory, hotbar, and hand while blocked. | Every CB block/item view uses the same red X instead of its normal CB appearance. | 📜 | 📜 |
| C4 | Send a CB action through a changed or direct client path instead of using the visible controls. | The official server rejects it; hiding a screen/button is not the only protection. | 📜 | 📜 |
| C5 | Remove the block while the player is online. | Normal CB visuals and actions return live without restarting Minecraft or rejoining. | 📜 | 📜 |
| C6 | Block the official client, open singleplayer, and attempt CB. | CB stays unavailable, uses red-X visuals, and shows the same reason while ordinary singleplayer remains usable. | 📜 | 📜 |

## D - Live updates, saved restrictions, and outages - Planned 📜

| | |
| --- | --- |
| **Check** | Online changes arrive immediately, offline changes wait visibly, and a service outage does not stop ordinary CB use. |
| **Pass rule** | Live block/unblock, offline saved block, queued change, newest-change-wins, outage, and reconnect rows pass twice. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Block and unblock a person already online on an official server. | Each change applies live and SRBProjects shows that the server/client received the latest decision. | 📜 | 📜 |
| D2 | Block an official client, close it, disconnect the machine from the service, then open its singleplayer world. | The last known block remains active; going offline does not clear it. | 📜 | 📜 |
| D3 | Make a new block or unblock while the target is offline. | SRBProjects shows that the change is waiting and applies it when the target next connects. | 📜 | 📜 |
| D4 | Send block, unblock, then block rapidly while one target reconnects late. | Only the newest decision wins; an older delayed message cannot reverse it. | 📜 | 📜 |
| D5 | Make the owner service unavailable while an unrestricted client and server are running. | CustomBlocks keeps working normally; Minecraft does not freeze, stop, or require the panel. | 📜 | 📜 |
| D6 | Restore the owner service after a short outage. | Clients/servers reconnect quietly, apply only missing changes, and do not duplicate new-user notifications or reasons. | 📜 | 📜 |

## E - Confirmed emergency CB pause - Planned 📜

| | |
| --- | --- |
| **Check** | One carefully confirmed emergency action can pause CB on servers only or servers plus singleplayer, with manual or timed restore. |
| **Pass rule** | Confirmation, both scopes, required reason, manual restore, timed restore, restart survival, and personal-block isolation rows pass twice. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Open the emergency action and inspect it without confirming. | It offers servers only or servers plus singleplayer, requires a reason, explains the effect, and keeps Cancel available. | 📜 | 📜 |
| E2 | Confirm servers-only emergency mode. | CB becomes unavailable on every connected official CB server while official-client singleplayer remains usable. | 📜 | 📜 |
| E3 | Confirm servers-plus-singleplayer emergency mode. | Both official server CB and official-client singleplayer CB use the red-X/refusal state. | 📜 | 📜 |
| E4 | Choose manual restore, then restore CB. | Emergency state remains until the owner restores it, then connected targets regain CB live. | 📜 | 📜 |
| E5 | Choose an automatic restore time and restart SRBProjects before it arrives. | The timer survives the restart, fires once at the chosen time, and restores the selected scope. | 📜 | 📜 |
| E6 | Keep one person individually blocked, enable emergency mode, then restore the emergency. | General CB returns, but the person's separate block remains active. | 📜 | 📜 |
| E7 | Use either emergency scope while one target is offline. | The target shows waiting state and receives the newest pause/restore truth on reconnect. | 📜 | 📜 |

## F - Security and honest control limits - Planned 📜

| | |
| --- | --- |
| **Check** | G33 controls official CustomBlocks without pretending to control computers, hidden jars, hostile forks, or private data. |
| **Pass rule** | Data boundary, owner-secret, server authority, control-service isolation, update trust, and truthful-limit rows pass review and multiplayer checks. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Inspect every fact sent by the official client and server. | Only approved CB/game facts are sent; private files, other mods, and device contents are absent. | 📜 | 📜 |
| F2 | Inspect person and CB-copy identity storage. | Minecraft account identifies the person, a separate random identity identifies the CB copy, and raw IP is not the primary identity. | 📜 | 📜 |
| F3 | Inspect the distributed jar and client requests. | No owner password, reusable admin token, cloud secret, or owner-control power exists in the public jar. | 📜 | 📜 |
| F4 | Try blocked CB actions from a client that ignores its visible blocked screen. | The official server still rejects all multiplayer CB changes. | 📜 | 📜 |
| F5 | Inspect the service endpoints used by G33. | The existing pack/export HTTP server and open test Cloud Vault are not used as the control service. | 📜 | 📜 |
| F6 | Test any future official update route used by G33. | It accepts only an owner-controlled signed release and never trusts an arbitrary joined server's replacement jar. | 📜 | 📜 |
| F7 | Review all owner-facing claims. | The panel says it controls connected official CB; it never promises detection of unrun jars or tamper-proof private singleplayer. | 📜 | 📜 |

## G - Later integrations and organization extras - Planned 📜

| | |
| --- | --- |
| **Check** | Parked additions stay out of the first build without being forgotten or mixed into core acceptance. |
| **Pass rule** | Each parked idea remains separately named and cannot silently become a first-build dependency. |
| **Pass mark** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Review first-build scope for Discord. | Discord alerts, `discord.gg/stormygang` appeals, and quick controls remain parked. | 📜 | 📜 |
| G2 | Review first-build access flow. | Normal CB requires no password; optional per-person invite codes remain parked. | 📜 | 📜 |
| G3 | Review first-build organization. | Custom labels/colors, private notes, and advanced search remain parked QoL. | 📜 | 📜 |
| G4 | Review first-build records. | Required control reasons/history remain; recording every CB action remains parked. | 📜 | 📜 |
| G5 | Review first-build owner surfaces. | SRBProjects is the only active surface; private website/mobile access remains parked. | 📜 | 📜 |
| G6 | Review blocked-person controls. | Per-feature switches remain absent; the active rule is normal CB or blocked CB. | 📜 | 📜 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §§A-F CB Control Center core — 💤 `2026-07-18`: Fully discussed and intentionally parked for later implementation.
- §G Discord, website, invite codes, labels/search/notes, detailed activity, and per-feature controls — 💤 `2026-07-18`: Later additions outside the first build.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Raw-IP/device control and detecting a jar that has never run — 👎 `2026-07-18`: Impossible or outside CB-only scope.
- Minecraft server `max slots` control — 👎 `2026-07-18`: Owner removed it from this idea.
- One required shared password for all CB users — 👎 `2026-07-18`: Too annoying and too easy to share.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove all temporary G33 test blocks and emergency states.
- [ ] Restore every test account, CB copy, and server to normal CB access.
- [ ] Delete temporary owner reasons and test-only presence records.
- [ ] Stop any temporary outage simulator or test service.

</details>
