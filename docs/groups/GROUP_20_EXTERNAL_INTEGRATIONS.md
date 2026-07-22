# Group 20 - Cloud Vault, Discord, and External Integrations

> Group 20 lets a server share whole CustomBlocks by code and connect its chosen events to Discord without exposing a confusing or unsafe public integration surface.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_20.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Cloud Vault gives a player a simple way to move a complete custom block, including its texture and animation data, through a share code. It must be optional, friendly when unavailable, and safe when a downloaded block conflicts with local content.

Discord is the server's notification connection. G20 owns the events, configuration model, chat controls, and delivery behavior; it deliberately does not own the large configuration Screen. The owner-controlled Cloudflare worker is part of this product boundary, so worker capabilities and mod behavior stay explicit.

## Ownership

| Owns | Does not own |
| --- | --- |
| Whole-block vault upload, download, share codes, local code history, and the cloud master gate | Category export/import structure: G12 |
| Import-conflict data, server actions, and normal undo/redo recording | Conflict Screen layout and controls: G27 |
| Discord event model, defaults, templates, chat helpers, batching, and webhook delivery | Discord configuration Screen: G21 |
| Cloud request identity, worker contract, and graceful network failure behavior | Server backups and local recovery: G09 |
| Future vault transport of block-adjacent configurations | Asset conversion and animation data production: G05 and G14 |

## Direction

Vault sharing stays centered on a whole-block payload and a short code. Uploads are operator-only; code downloads are available to players. A single `cloudShareEnabled` switch gates every cloud path before any request is made. Missing configuration or an unavailable worker is quiet where the feature is optional, while an action the player deliberately started gets a human-readable reason.

Discord starts with a professional default and useful chat commands. The data model is designed so G21 can add a full editor without reimplementing event delivery. Large community, marketplace, remote-control, and external-creation ideas remain separated from the dependable share-and-notify core.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | Vault shares one complete static or animated block using the existing category-style zip payload. | The payload contains `SlotData`, required texture files, and animation sidecar data without a second block-storage format. |
| 2026-06-21 | Upload is operator-only; download by code is available to players. | Sharing cannot become an open upload endpoint by accident. |
| 2026-06-21 | `cloudShareEnabled` gates every cloud request. | Block, category, note, and conflict follow-up paths all stop before networking when disabled. |
| 2026-06-21 | A player import conflict opens a dedicated Screen; console imports use plain text. | An existing local block is never silently overwritten and client-only UI is never sent to a console. |
| 2026-06-21 | Conflict actions use the normal `/cb undo` and `/cb redo` history. | Vault imports follow the same multi-level undo rules as other edits. |
| 2026-06-21 | Discord is fully configurable through data and chat helpers before its GUI exists. | G21 can add the editor on top of one stable configuration model. |
| 2026-06-21 | The worker enforces request signing, limits, identity policy, and a remote kill switch. | A shipped jar is treated as public; the design aims for abuse resistance, never secret-in-jar security. |
| 2026-06-27 | `/cb vault` is a Screen route rather than a chest dashboard. | G27 provides the interface while G20 provides every vault action. |

## Feature Plan

### A. Block Vault and Share Codes

**Player outcome**

An operator can upload a complete block and receive a code; a player can import that code without losing animation or block data.

**Experience**

- Upload and download use the same share code flow from commands and connected Screen routes.
- Successful sharing gives immediate, readable feedback and retains the generated code in local history.
- Disabled or unconfigured cloud sharing explains the action could not run without implying the block itself failed.
- Backup cloud sync preserves a successful local backup even when the remote upload cannot finish.

**Requirements**

- Package one block as `SlotData` plus source texture files and the `grid.json` sidecar when animated.
- Store bounded code history with kind, label, code, and newest-first order in `vault_codes.json`.
- Use `vaultEndpoint` as the live endpoint field and retain the single `cloudShareEnabled` gate for all cloud entry points.
- Keep payload limits and cloud backup behavior compatible with KV share storage and the separate R2 backup route.

**Boundary**

G20 moves block payloads. G12 owns category export/import, G09 owns local backup/recovery, and G14 owns animation rendering and authoring.

### B. Import Conflict Resolution

**Player outcome**

When an imported ID already exists, the player can see the choice and deliberately replace, preserve, rename, or cancel it.

**Experience**

- The Screen compares local and incoming content, with a simple differences view and an advanced full-property view.
- Its editable ID field starts with a valid free suggestion and disables an invalid action with a clear reason.
- Override, Keep Both, Rename Mine, and Cancel are the only outcomes; each successful mutation gives normal share feedback and a `/cb give <id>` hint.

**Requirements**

- Server packets supply the incoming content and safe preview bytes; G27 renders the side-by-side live previews.
- Every successful conflict action records one normal undo/redo edit.
- A console or other non-player source receives a clear text conflict response instead of a client packet.
- Conflict re-fetch and result handling must pass through the master cloud gate.

**Boundary**

G20 owns the import codec, validation, and mutation. G27 owns visual layout, preview rendering, and interaction controls.

### C. Discord Events and Chat Controls

**Player outcome**

An administrator can connect a webhook, receive useful event embeds, and tune the integration without editing files by hand.

**Experience**

- Events cover create, delete, edit, bulk work, backups, vault work, incidents, and server startup.
- Block media is attached as its real PNG or GIF, never a localhost-only link.
- Rapid matching events become one summary with optional spoiler detail rather than webhook spam.
- `/cb discord show`, `reset`, `placeholders`, and `preview <event>` make configuration understandable before the G21 editor exists.

**Requirements**

- Persist per-event enablement, template, colour, role ping, webhook identity, batch window, quiet hours, and media choices in one data model.
- Support placeholders for block, actor, action, count, code, error, time, and server context.
- Ship a Professional default with a blank webhook URL; blank configuration produces no log spam.
- Use the uploader head only as a Discord-reachable author icon and attach media through multipart delivery.

**Boundary**

G20 owns delivery and configuration behavior. G21 owns the full visual editor; generic outbound webhooks are a future platform feature, not part of the Discord core.

### D. Worker Safety and Owner Controls

**Player outcome**

Cloud actions remain predictable under normal outages and can be limited or stopped by the service owner without a mod update.

**Experience**

- A deliberate failed action identifies a useful cause, such as an unreachable worker, without exposing secrets.
- Service policy may allow, deny, limit, or stop requests for a player or server.
- Local actions remain usable when optional remote delivery is unavailable.

**Requirements**

- Sign requests with HMAC plus timestamp/replay protection, knowing a public jar cannot hold a truly secret key.
- Send player UUID and server identity on vault requests for worker-side policy decisions.
- Apply size limits, rate limits, WAF/custom-domain protection, and a remote kill switch at the worker boundary.
- Store normal share payloads in KV and large backup payloads in R2 with the configured lifecycle policy.

**Boundary**

This Group defines the mod-to-worker contract. The owner deploys and operates the Cloudflare resources; a local jar change cannot claim that worker controls exist.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Asset delivery | Vault transports ready source assets; it does not rebuild resource packs. |
| G09 | Backup safety | A local backup completes before optional cloud sync; remote failure never invalidates the local copy. |
| G12 | Category export | Both routes can share the existing zip conventions without becoming duplicate feature owners. |
| G14 | Animation data | Animated vault payloads preserve the grid texture and `grid.json` sidecar needed by the renderer. |
| G16 | Diagnostics | Cloud and Discord incidents use understandable diagnostic reporting without leaking sensitive configuration. |
| G21 | Configuration Screen | G21 reads/writes the G20 Discord data model and does not duplicate webhook delivery. |
| G27 | Vault and conflict Screens | G27 displays and routes actions; G20 validates requests and changes stored block data. |

## Technical Contract

- Vault imports and uploads use an existing category-style archive containing one block's `SlotData`, texture files, and optional animation sidecar.
- `cloudShareEnabled` is checked before every network request; `vaultEndpoint` is the live endpoint configuration field.
- Conflict mutation is server-authoritative and creates a normal `UndoManager` entry, never a private vault-only history.
- Screen packets contain only the content and preview data required for a one-time conflict decision; the client cannot choose an invalid or unauthorized mutation.
- Discord media is multipart-attached from the available source file, so remote Discord clients can render it.
- Signing, rate limits, size caps, R2 lifecycle, identity policy, and remote stop controls are verified and enforced by the owner-operated worker.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Marketplace browser, likes, trends, profiles, and social features | They need moderation, search, and a broader public product boundary. | Future Cloud Network work |
| Web gallery, QR links, versioning, forks, private codes, and offline queue | They extend sharing after the single-block flow is dependable. | Future Cloud Network work |
| Content moderation, remote announcements, telemetry, and private admin dashboard | These are owner-platform safety features, not normal in-game sharing. | Owner platform work |
| External creation from Discord, browser, REST, Twitch, or generic webhooks | Inbound public integrations need separate authentication and abuse rules. | Future external integrations work |
| Discord setup wizard and full Discord Hub | The stable data model comes first; the editor belongs to G21. | G21 |
| Pack/multi-block sharing, vouchers, expiry/pin UI, and download holograms | One-block sharing and safe import behavior are the current foundation. | G20 future slice |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | A download conflict could use a chest GUI and an automatic suffix. | G27 provides a Screen with an editable valid ID and four deliberate actions. |
| 2026-06-21 | Vault conflicts would have a separate undo stack. | Every resolved import uses normal `/cb undo` and `/cb redo`. |
| 2026-06-21 | Discord could rely on localhost texture URLs. | The source PNG or GIF is attached to the webhook. |
| 2026-06-27 | `/cb vault` would be a chest dashboard. | It routes to the G27 Screen framework. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_20.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G09 Backup and Recovery](GROUP_09_BACKUP_SAFETY.md)
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G21 Configuration Screen](GROUP_21_CONFIG_GUI.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 20 snapshot](../archive/group-migration-2026-07-18/GROUP_20_EXTERNAL_INTEGRATIONS.md)
