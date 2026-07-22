# Group 21 - Registry Capacity Sync

> Group 21 prevents an outdated client registry size from producing Fabric's raw CustomBlocks registry kick when it joins a server with more slots.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_21.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

`max_blocks` determines how many CustomBlocks block and item registry entries a client creates during launch. When its value is lower than the server's value, Fabric rejects the connection before normal play-time synchronization can occur. The raw message is technical and gives the player no route back.

G21 owns the client-side recovery path for that one registry-capacity mismatch. It raises the local setting safely, explains the required full restart, and preserves the fact that a client with equal or greater capacity must remain untouched. It does not own the in-game configuration interface.

## Ownership

| Owns | Does not own |
| --- | --- |
| `max_blocks` mismatch detection during config-phase registry synchronization | `/cb config`, Settings Book layout, and general setting editing: G27 |
| Atomic, raise-only local repair and the restart-required message | Normal play-phase configuration synchronization |
| The client mixin and helper that prevent the raw missing-slot disconnect | New registries, block metadata, and resource-pack delivery |
| Narrow audit of registry-size-related join failures | Generic config payload cleanup or a speculative server handshake |

## Direction

When the server advertises more `customblocks:slot_N` entries than the client's frozen launch registry, the client intercepts the registry-remap failure. It learns the required capacity, raises only the local `max_blocks` value, and tells the player to fully restart Minecraft before rejoining.

The repair is deliberately small and idempotent. A client that already has enough slots joins normally, sees no prompt, and keeps its higher value. Settings changed by the owner through a future Screen are a different concern from recovering another player's under-sized launch registry.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-26 | Only `max_blocks` creates a data-sized CustomBlocks registry surface. | The repair stays focused instead of synchronizing unrelated settings. |
| 2026-06-26 | The client repairs itself automatically with an atomic raise-only write. | Players never manually edit a file, and the client can never lower its capacity. |
| 2026-06-26 | The shipped default stays modest. | Client memory/registry cost remains reasonable; an affected client needs one restart. |
| 2026-06-27 | Config-phase self-heal is the complete solution. | A server handshake is not built because normal payloads arrive after the kick. |
| 2026-06-27 | Mismatch detection compares against the live registry, not the just-updated config value. | A same-session retry cannot leak the raw error while registries still await restart. |
| 2026-07-12 | Settings Book and Discord editor work belong to G27 and G21. | G21 remains a networking/mixin backend document. |

## Feature Plan

### A. Launch-Time Capacity Repair

**Player outcome**

A player with too few CustomBlocks slots sees a clear restart instruction instead of a cryptic unknown-registry disconnect.

**Experience**

- The message names the server capacity, the player's old value, and the fact that the setting was updated.
- One full Minecraft restart is requested plainly; the player can then rejoin without editing a file.
- Retrying before restart still avoids the raw registry error, even though the running registry cannot gain slots.

**Requirements**

- A config-phase client mixin reads the highest remote `customblocks:slot_N` entry during the registry-remap failure.
- `MaxSlotsHealer.ensureAtLeast(int needed)` is the only write path for this repair.
- The write reuses the normal atomic config-store behavior and clamps capacity within `1..8192`.
- The restart prompt is one-shot per needed value and logs enough context to diagnose a mismatch without repeated noise.

**Boundary**

This area repairs a client that is below server capacity. It does not add registry entries at runtime, replace normal setting synchronization, or change a client that is already equal to or above server capacity.

### B. Capacity Safety Rules

**Player outcome**

Joining a server with fewer slots than the client remains uneventful and safe.

**Experience**

- A higher-capacity client joins without a warning, file write, or value reduction.
- An equal-capacity client joins without any repair UI.
- Normal servers with no mismatch never see the recovery experience.

**Requirements**

- The healer is idempotent: `local >= needed` is silent no-op behavior.
- The comparison uses live registered slot capacity because config changes cannot alter a running registry.
- The code path stays isolated from `SlotManager`, which has no safe room for additional logic.

**Boundary**

Capacity safety does not decide the server's configured maximum; it only ensures a joining client can reach that maximum after restart.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Registered block assets | G21 only heals client registry count; it does not build or serve assets. |
| G13 | Arabic registry | The fixed Arabic registry is outside `max_blocks` capacity and must not enter the repair calculation. |
| G16 | Diagnostics | Join failures and healing logs use the diagnostic conventions without exposing a raw Fabric error to players. |
| G27 | Settings Screen | G27 edits local settings and shows normal restart warnings; G21 handles automatic recovery while joining another server. |

## Technical Contract

- `SlotManager.registerAll(int max)` creates the flat `slot_0` through `slot_{max-1}` block and item registrations once during mod initialization.
- Minecraft registries are frozen after launch, and the mismatch happens in Fabric's configuration phase before the existing play-phase join hooks can send a payload.
- The client registry must be greater than or equal to the server registry; extra client slots are harmless, so repair is permanently raise-only.
- `MaxSlotsHealer` owns atomic config writes and restart-pending state; the config-phase mixin is its only required caller.
- The repair derives needed capacity from the live remote registry entries and compares it with the live local registry, not mutable in-memory config.
- `ConfigSyncPayload` may be considered later for unrelated play-time synchronization, but is not a solution to this launch-time kick.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| General restart-class config payload | It is unrelated cleanup and cannot repair the config-phase mismatch. | Future configuration backend work |
| Additional registry-capacity repair | No other data-sized CustomBlocks registry surface is currently known. | G21 after a new audit |
| Settings Book, setting search, live apply, and Discord editor | These are interaction and UI concerns, not launch-time registry recovery. | G27 and G21 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-26 | A server handshake and the client healer would cooperate. | The self-heal mixin is the only reliable path because the server cannot send a normal payload before the kick. |
| 2026-06-27 | The local config value could prove a same-session retry was safe. | The live registry is authoritative until a full restart recreates it. |
| 2026-07-12 | G21 owned the Settings Book specification. | G27 owns the Screen; G21 retains only registry-capacity recovery. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_21.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 21 snapshot](../archive/group-migration-2026-07-18/GROUP_21_CONFIG_GUI.md)
