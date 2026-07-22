# Group 33 - CB Control Center

> Group 33 gives the CustomBlocks owner one private SRBProjects control center for seeing official CB use and controlling CB access without controlling Minecraft or the user's computer.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_33.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

CustomBlocks needs one organized owner-only place outside Minecraft where official CB clients and servers can be seen and CB access can be controlled. The first surface is a dedicated area inside the SRBProjects desktop app. It is not a website, Discord bot, server-hosting panel, device-management tool, or replacement for Minecraft administration.

G33 owns the official CB control network, its SRBProjects dashboard, client/server presence, CB-only revocation, live policy delivery, and emergency CB shutdown. The system must stay honest about its reach: it can control official CB behavior after the jar runs and connects, but it cannot detect a jar that has never run, make Java code unchangeable, or control an altered copy in somebody's private singleplayer world.

## Ownership

| Owns | Does not own |
| --- | --- |
| SRBProjects CB Control Center navigation, people/server lists, notifications, and action flows | In-game CustomBlocks Screen framework: G27 |
| Official client/server registration, presence, and stable CB-copy identity | IP tracking, device administration, private files, other mods, or server-host operating-system control |
| Account-, server-, and CB-copy-level revocation of CustomBlocks | Ordinary local command roles and per-command permission tiers: G22 |
| Red-X blocked presentation and required human reason when blocked CB is attempted | Shared message/kick presentation rules: G04 |
| Live block/unblock delivery, saved last-known restrictions, and outage behavior | General cloud sharing, Discord transport, and official update delivery: G20 |
| Confirmed emergency CB pause for servers only or servers plus singleplayer | Testing Center workflow and diagnostics evidence: G16 |

## Direction

The SRBProjects control center opens to an organized list of every official CB user the system has seen. A user first appears when official CB actually starts, receives a new-user notification, and remains listed while offline until the owner removes the record. The list shows the Minecraft identity, online state, CB version, current or last server, last-seen time, and any active CB restriction. One person stays one row; a duplicate mark reveals their other known CB copies.

A blocked player remains on the Minecraft server and can continue ordinary Minecraft. CustomBlocks itself becomes unusable: commands, menus, tools, creation, editing, and CB interaction are refused, while CB blocks and items appear as a red X. The owner's required reason appears only when the player attempts CB. Unblocking restores CB immediately for connected users and on the next successful launch for offline users.

The official server is the final authority on multiplayer CB actions. Singleplayer follows the last restriction received by the official client, including while temporarily offline, but local singleplayer enforcement cannot be promised against a deliberately altered jar. If the control service is unavailable, CustomBlocks still starts and ordinary users keep playing; existing saved restrictions remain, and new owner changes wait until the affected official client or server reconnects.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-18 | The first and only active owner surface is a private area inside the SRBProjects desktop app. | No website or Discord control surface is required for the first build. |
| 2026-07-18 | Official CB clients report presence as soon as Minecraft starts; official CB servers report presence as soon as the server starts. | Discovery begins only after the jar runs; an untouched jar sitting in `mods` cannot be detected. |
| 2026-07-18 | Users join and use official CB normally without a password or manual approval. | Invite/password access remains optional future scope rather than friction for every user. |
| 2026-07-18 | The people list retains known offline users and keeps one row per Minecraft account. | Multiple CB copies appear behind a duplicate mark instead of cluttering the main list. |
| 2026-07-18 | A newly seen CB user creates an SRBProjects notification. | Discord notifications are not part of the first build. |
| 2026-07-18 | Revocation controls CustomBlocks only. | It never controls Minecraft generally, the computer, files, IP, other mods, or server hosting. |
| 2026-07-18 | Blocks may target one server, one CB copy, or the whole Minecraft account, and may be permanent or end at a chosen time. | The owner chooses the exact reach and duration of each block. |
| 2026-07-18 | No warning is required before blocking; every kick or block requires an owner-written reason. | The reason is saved with the action and shown only when the affected player tries CB. |
| 2026-07-18 | A blocked player stays on the server while all CB actions are refused. | CB blocks and inventory/hotbar items render as a red X for that player; normal Minecraft stays available. |
| 2026-07-18 | Blocking and unblocking update live for connected servers and clients. | Offline copies receive the latest decision when they next connect; existing known blocks remain saved while offline. |
| 2026-07-18 | SRBProjects has one emergency CB-off action with a clear confirmation and required reason. | The owner chooses servers only or servers plus singleplayer, then chooses manual restore or an automatic restore time. |
| 2026-07-18 | The mod must not stop working merely because the panel or service is unavailable. | Availability failures delay new remote changes but do not break ordinary CB startup or gameplay. |
| 2026-07-18 | Official server rules cannot be overridden through the supported CB client or local server settings. | Every multiplayer CB action is checked by the official server; altered or unrelated copies remain outside the official network guarantee. |

## Feature Plan

### A. SRBProjects Control Center

**Player outcome**

The owner opens one calm, organized desktop view and immediately sees who and what is using official CustomBlocks.

**Experience**

- The main navigation separates people, servers, active restrictions, and notifications rather than placing every action in one large page.
- The people list shows Minecraft name, online/offline, CB version, current or last server, last seen, and active restriction.
- One person appears once. A duplicate mark and count open the other known CB copies and their version/server/last-seen details.
- A first-seen person creates a clickable SRBProjects notification that opens that person's page.
- A person page keeps current facts and control actions separate so dangerous controls do not dominate the overview.

**Requirements**

- The panel is owner-only and does not expose a public administration page.
- Empty values display `-`; stale status is clearly different from currently online.
- Every destructive action uses a focused confirmation with its target and exact effect.
- The panel must show whether the latest change reached each affected client/server or is waiting for reconnection.

**Boundary**

Custom labels, label colors, private notes, advanced search across notes/labels, and detailed activity browsing are later quality-of-life work.

### B. Official CB Presence and Identity

**Player outcome**

Official CB users and servers appear automatically after CB starts, without a separate enrollment chore.

**Experience**

- A client appears when Minecraft starts with official CB, before it joins a server.
- A server appears when its official CB jar starts and shows the CB users currently connected to it.
- Closing Minecraft or stopping a server changes it to offline while preserving the remembered entry and last-seen time.
- Reinstalling or using another computer can produce another CB-copy entry under the same Minecraft account.

**Requirements**

- Use Minecraft account identity for person-wide decisions and a separate random CB-copy identity for one-copy decisions.
- Never use raw IP address as the person's identity and never inspect or report other mods or private files.
- Report only the CB/game facts needed by this Group, with clear first-run disclosure that official CB connects to its owner service.
- Do not claim a jar still exists after it goes offline; the dashboard only knows when that copy was last seen.

**Boundary**

The system cannot discover a jar that has never run, a machine with no network path, an old build that predates G33, or an altered copy that removes its connection.

### C. Person, Server, and Copy Revocation

**Player outcome**

The owner can stop one person's CB use at the right level without removing them from Minecraft.

**Experience**

- Block scope offers whole Minecraft account, one chosen server, or one chosen CB copy.
- Duration offers permanent or an owner-selected end time.
- Every block requires a reason; no warning is forced before the owner confirms it.
- A person-wide block follows the same Minecraft account onto other known or newly seen official CB copies.
- Unblock is available from the same restriction record and takes effect live where possible.

**Requirements**

- Store the target, scope, reason, start, optional end, current state, and delivery state for every restriction.
- Server-specific and copy-specific blocks must not accidentally become account-wide.
- Expired temporary blocks restore access once and cannot silently return after reconnect/restart.
- Owner changes are ordered so an older delayed message cannot overwrite a newer block or unblock.

**Boundary**

G33 does not offer individual CB-feature switches. A person is allowed to use CB normally or CB is unavailable at the selected scope.

### D. Complete CB Refusal and Red-X State

**Player outcome**

A blocked person understands that CB is unavailable while the rest of Minecraft remains usable.

**Experience**

- CB commands, menus, tools, creation, editing, block interaction, and client-only CB entry points refuse to operate.
- Existing CB blocks, held items, inventory items, and hotbar items display a red X instead of their normal CB appearance.
- The block reason appears only when the person attempts to use CB; joining or opening Minecraft does not spam them.
- Removing the block restores the ordinary CB visuals and controls without a restart when the user is online.

**Requirements**

- Multiplayer checks happen on the server for every CB mutation/action path; hiding client buttons alone is never treated as enforcement.
- Client rendering uses one shared blocked-state source so world blocks and item views cannot disagree.
- Rejected client packets/actions change no server data and produce the same human reason rather than an error or stack trace.
- Singleplayer keeps the last known official restriction locally so simply disconnecting from the internet does not clear an existing block.

**Boundary**

An owner of a computer can alter local Java code and bypass client-only singleplayer checks. That cannot bypass an official server's server-side checks and is not represented as a guarantee G33 can make.

### E. Live Delivery and Safe Outages

**Player outcome**

Blocks, unblocks, and emergency changes feel immediate without making CB depend on the panel being online every second.

**Experience**

- Connected clients and servers receive changes live and the panel shows when each target has applied them.
- Offline targets show that the change is waiting, then apply the newest decision after reconnect.
- If the owner service becomes unavailable, normal CB users continue using CB and known blocked users remain blocked by their last saved state.
- Reconnection quietly catches up without duplicate notifications or repeated reason messages.

**Requirements**

- Clients and servers connect outward to the owner service; G33 must not expose the existing pack/export HTTP server as a control endpoint.
- Save last-known restrictions and a strictly increasing change version so reconnect/retry is safe.
- Keep connection, retry, and storage work off Minecraft's main game thread.
- No reusable owner-control secret may be shipped inside the public jar.

**Boundary**

An action cannot reach a device or server while it is genuinely offline. G33 promises visible waiting state and eventual application, not impossible instant delivery to disconnected targets.

### F. Emergency CB Pause

**Player outcome**

The owner can temporarily stop CB during a serious problem without stopping Minecraft servers or locking ordinary gameplay.

**Experience**

- One emergency action offers two scopes: all official CB servers, or all official CB servers plus official-client singleplayer.
- The confirmation states exactly who will lose CB, requires the owner's reason, and keeps Cancel visually safe.
- The owner chooses manual restore or sets an automatic restore time.
- Affected users receive the same red-X/refusal behavior and see the emergency reason only when they try CB.

**Requirements**

- Emergency state is separate from personal restrictions so restoring the emergency does not unblock individually blocked people.
- Automatic restore fires once, survives owner-app restart, and cannot be undone by an older delayed message.
- The panel always shows active emergency scope, reason, start time, and restore rule.
- Restoring CB updates connected targets live and leaves offline targets queued for their next connection.

**Boundary**

Emergency pause disables CustomBlocks only. It never shuts down Minecraft, restarts servers, changes server capacity, or controls the host machine.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Human feedback | Block/refusal text uses G04's unified human message rules; G33 owns the reason and when it appears. |
| G16 | Diagnostics | Connection, delivery, and rejected-action failures produce useful owner evidence without exposing private device data. |
| G20 | External service and official updates | G20 may provide hardened cloud/Discord/update transport; G33 owns control policy and never trusts an arbitrary server-supplied replacement jar. |
| G22 | Local permissions | G22 decides which allowed users may run specific commands; G33 first decides whether that person may use CB at all. |
| G27 | In-game Screens | G27 screens respect G33 refusal state but do not own or duplicate the SRBProjects panel. |

## Technical Contract

- Official multiplayer enforcement is server authoritative and covers commands, payloads, tools, menus, block interaction, and every mutation service.
- Person-wide identity uses the authenticated Minecraft account; a separate random installation identity handles one-copy targeting. Raw IP is never the primary identity.
- The distributed jar contains no owner password, reusable administration token, cloud secret, or power that a client can present as owner authority.
- Every owner action is authenticated outside the public jar, ordered, recorded with its reason, and acknowledged by each online target.
- Existing restrictions are saved locally/server-side for outage continuity; unavailable control service delays new changes but never blocks ordinary CB startup for unrestricted users.
- Singleplayer restriction is an official-client behavior, not tamper-proof device control. Documentation and UI must state that limit plainly.
- The current unauthenticated pack/export HTTP service and minimal open Cloud Vault are not reused as the G33 control service.
- Official update delivery must be pinned to an owner-controlled signed release path before remote update controls are considered.
- No G33 code reads another mod's files, scans a person's mods folder, controls the host OS, or reports private device contents.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Discord alerts, appeals, and quick actions through `discord.gg/stormygang` | The owner chose SRBProjects-only focus for the first build. | G33 with G20 transport |
| Private website/mobile panel | The owner does not want to build or pay for a website yet. | G33 future surface |
| Optional invite/password mode | Requiring every user to enter a password would be annoying; private-server/test access can return later with separate per-person codes. | G33 access extension |
| Custom labels, colors, multiple labels, private notes, and advanced search | Useful organization polish was intentionally parked until the core control system exists. | G33 QoL |
| Detailed recording of every CB action | The owner parked full activity capture; core control decisions still retain their required reason/history. | G33 audit extension |
| Per-feature CB switches for individual people | The owner chose one simple allowed-or-blocked CB state. | G33 only if owner reopens it |
| Server-host controls, player-capacity changes, remote files, and device administration | They are outside CustomBlocks and outside the safe scope of this system. | Separate owner-managed infrastructure, not G33 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-18 | Device IP could identify and control every machine with CB in its mods folder. | A person is identified by Minecraft account and a CB copy by a random identity after CB runs; IP/private-device control is excluded. |
| 2026-07-18 | The owner could make the public jar literally unchangeable. | Official servers enforce official CB rules; altered local copies cannot be made impossible and remain outside the guarantee. |
| 2026-07-18 | A password could be required for everybody before CB works. | Normal use stays open; optional private invite codes are parked. |
| 2026-07-18 | Remote global settings could include default Minecraft server max slots. | Server player capacity was scrapped and is outside CB control. |
| 2026-07-18 | Discord and a website could ship with the first control panel. | SRBProjects is the only active surface; Discord and web are parked. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_33.md) · [All Groups](README.md)

- [G04 Messages and Command Communication](GROUP_04_Communication.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
