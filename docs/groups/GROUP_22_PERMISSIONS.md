# Group 22 - Permissions

> Group 22 gives each `/cb` action a clear permission rule, with LuckPerms-aware nodes when available and understandable vanilla-operator fallback everywhere else.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_22.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

CustomBlocks needs permissions that work for a public server without making ordinary commands disappear from chat suggestions. Players should be able to browse and use allowed commands, while changes, bulk work, backups, and diagnostics have predictable server-owner control.

G22 owns the shared permission engine, permission-node naming, vanilla fallback tiers, and command mapping. It is deliberately paused until the remaining command classification choices are resolved; the stored design is ready to resume without reopening decisions that are already settled.

## Ownership

| Owns | Does not own |
| --- | --- |
| Permission checks, nodes, tier mapping, and clean denial behavior | Command behavior and feature-specific validation in each owning Group |
| Fabric Permissions API integration and vanilla OP fallback | Permissions Screen layout and setting editing: G27 |
| `use`, `edit`, and `admin` fallback configuration fields | Server member/rank management inside LuckPerms |
| Resource-pack control command access rules | Resource-pack build and delivery behavior: G05 |

## Direction

Every top-level `/cb` action receives one permission node in the form `customblocks.command.<name>` and one fallback tier. The API checks a node where a permissions provider exists, otherwise it uses configurable vanilla OP levels: `use=0`, `edit=2`, and `admin=4` by default.

A denied command stays visible and tab-completable. Its execution performs a shared check and responds with exactly `You don't have permission to use this command.` This prevents a normal authorization decision from looking like an unknown command or a server error.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-22 | Build a small permission slice before wiring every command. | The first implementation proves `list` as use, `create` as edit, and `backup` as admin. |
| 2026-06-22 | Use one node per top-level subcommand. | Permissions remain discoverable and do not require a separate custom syntax. |
| 2026-06-22 | Use Fabric Permissions API when available, with vanilla OP fallback. | LuckPerms-style nodes and standalone servers share one permission call site. |
| 2026-06-22 | Default fallback tiers are use 0, edit 2, and admin 4. | Server owners can choose more open or restricted fallback values without changing nodes. |
| 2026-06-22 | Denial happens inside command execution. | Commands stay visible/tab-completable and show the agreed human message. |
| 2026-06-22 | Vanilla OP fallback is the first required test environment. | LuckPerms-specific checks wait until a server has it installed. |
| 2026-07-12 | Permissions settings use the G27 Screen framework. | G22 owns the fields and behavior, not a competing settings UI. |

## Feature Plan

### A. Shared Permission Engine

**Player outcome**

Players receive a short clear refusal only when they attempt an action they are not allowed to use.

**Experience**

- Command discovery and tab completion remain available to every player.
- Denied use produces one friendly message, with no stack trace or vanilla unknown-command response.
- Allowed commands continue into their existing validation and error messaging normally.

**Requirements**

- Restore the `fabric-permissions-api` dependency and centralize permission evaluation in a reusable helper.
- The helper accepts the concrete command node and fallback tier, then chooses provider evaluation or vanilla OP level.
- Avoid copying fragile authorization checks through dozens of inline command handlers.
- Permission denial is normal control flow and must not create noisy logs.

**Boundary**

The engine decides whether a command may begin. It does not replace command-specific input, ownership, lock, or safety validation.

### B. Command Tiers and Fallback Configuration

**Player outcome**

Server owners can understand who may browse, edit, or administer CustomBlocks and adjust the three fallback levels for their server.

**Experience**

- Use covers viewing, browsing, help, search, and other non-mutating player actions.
- Edit covers creation and intentional content changes.
- Admin covers bulk operations, server configuration, backup/recovery, diagnostics, and disruptive resource-pack controls.

**Requirements**

- Store `permissionTierUse`, `permissionTierEdit`, and `permissionTierAdmin` as validated vanilla OP levels.
- Give every registered top-level command exactly one node and one tier before mass wiring begins.
- Keep the final command map in this Group and the executable coverage in its Testing Guide.
- G27 exposes the three fields in its Permissions settings area without becoming the source of permission policy.

**Boundary**

Tiers are fallback policy, not a separate rank system. LuckPerms or another provider remains responsible for granting/revoking named permissions.

### C. Incremental Rollout

**Player outcome**

The permission system arrives without a large, hard-to-debug change across every command at once.

**Experience**

- The first slice shows the full use/edit/admin contrast using familiar commands.
- Server owners can test fallback levels before integrating a permission provider.
- Resource-pack pause, resume, and sync are protected as administrative controls when wired.

**Requirements**

- First wire `list`, `create`, and `backup`; validate denial, access, tier override, and clean logs.
- Then apply the approved map systematically to remaining command families.
- With LuckPerms installed, a named permission can grant the exact command without unintentionally granting unrelated admin access.
- `/cb rp pause`, `/cb rp resume`, and `/cb sync` use the admin tier; sync retains its three-second warning before a pack push.

**Boundary**

The full rollout waits for the unresolved command classifications below. No broad permission rewrite should guess those policy decisions.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Resource-pack controls | Pause, resume, and sync are admin-tier actions; their pack behavior remains owned by G05. |
| G09 | Backup and recovery | Backup/recovery commands use the admin tier while G09 retains their safety behavior. |
| G12 | Export and import | Final export/import tier selection must preserve G12 and G20 workflows. |
| G13 | Arabic blocks | Final Arabic command tier selection follows its editing/use behavior without changing Arabic data rules. |
| G15 | AI textures | Final AI tier selection protects cost/safety without changing G15 generation behavior. |
| G16 | Diagnostics | Diagnostic, audit, and feedback administration commands use the approved owner tier. |
| G17 | History and favourites | Undo/redo and favourite command classifications must match their actual mutation behavior. |
| G27 | Settings Screen | G27 presents G22's three fallback fields and never duplicates node evaluation. |

## Technical Contract

- Nodes use `customblocks.command.<subcommand>` consistently for every top-level command.
- A single shared helper invokes Fabric Permissions API when present and vanilla OP fallback when it is not.
- Default fallback values are `use=0`, `edit=2`, and `admin=4`; configuration validates permitted vanilla OP levels.
- Permission checks run inside execution, so `.requires()` does not hide an unavailable command from completion/discovery.
- The command map is finalized before broad wiring, and each command has one tier rather than layered accidental checks.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Final tier for lock/unlock | It still needs the owner to choose admin or edit. | G22 |
| Final tier for favourites | It still needs the owner to choose use or edit. | G22 with G17 |
| Final tier for undo/redo and note/lore | Their player-facing policy remains undecided. | G22 with G17/G18 |
| Final tier for AI, export/import, feedback controls, and Arabic | These cost, safety, and editing boundaries need an explicit owner decision. | G22 with G12/G13/G15/G16 |
| LuckPerms verification environment | The first rollout validates vanilla fallback; provider testing waits for a server installation. | G22 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-22 | Commands could use only raw OP checks or inconsistent unrestricted access. | Every mapped command uses one named node and fallback tier. |
| 2026-06-22 | Denied commands could be hidden with `.requires()`. | Commands remain visible and issue the agreed in-execution message. |
| 2026-07-12 | Permissions fields belonged to the old Group 21 configuration UI. | G27 owns their Screen presentation. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_22.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G09 Backup and Recovery](GROUP_09_BACKUP_SAFETY.md)
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G15 AI Textures](GROUP_15_AI_TEXTURES.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G17 History, Give, Delete, and Search](GROUP_17_REGRESSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 22 snapshot](../archive/group-migration-2026-07-18/GROUP_22_PERMISSIONS.md)
