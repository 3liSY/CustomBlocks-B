# Group 09 - Backup, Data Safety, and Trash

> Group 09 protects a server's CustomBlocks data before risky changes and provides a calm, recoverable path back when something goes wrong.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_09.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Backups must be trustworthy: complete, atomically written, clearly named, safe to restore after a restart, and impossible to apply halfway over live data. Trash must let the owner review recoverable deleted blocks without reimplementing the G06 deletion system.

This Group owns backup lifecycle, safety controls, automatic backup policy, and the backup/trash surfaces. It does not own the shared deletion rail, broken-block diagnostics, cloud transport, or common Screen framework.

## Ownership

| Owns | Does not own |
| --- | --- |
| Manual backup, load, delete, list, auto-backup, and prune behavior | Shared Recycle-Bin deletion and marker behavior: G06 |
| Backup file integrity and the backup data layout | Broken-block diagnostics and repair: G16 |
| Backup and Trash user flows | Common Screen framework and visual system: G27 |
| Backup safety boundary for Set All and other callers | Cloud/Vault transport: G20 |
| Configured backup interval and retention rules | Bulk selection and mutation behavior: G07 |

## Direction

Every restore starts from a complete, verified backup and protects the current state with a safety copy before replacing it. Write and restore operations are atomic, serialized, and explicit about destructive consequences. Player Screens are calm front doors to the same command contracts; server-console use stays textual.

Trash is the recovery view over G06's Recycle-Bin records. It shows what is recoverable and routes Restore, pin, and Empty through that shared system rather than maintaining an independent deleted-block store.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-27 | Trash is the recovery half of G06's Recycle-Bin system. | Restore, marker healing, slot reservation, and Empty use one shared contract. |
| 2026-07-09 | Backup, Trash, and Safety use Screens rather than chest menus. | Their player experience follows the G27 Screen pattern. |
| 2026-07-12 | A destructive backup load always asks for confirmation and creates a pre-restore safety copy. | Live data is never overwritten casually or mid-write. |
| 2026-07-12 | Broken-block reporting belongs to G16. | G09 may link to diagnostics but does not own scanner or repair code. |
| 2026-07-12 | First-boot migration is removed until a real legacy-data case exists. | No speculative migration format or code is kept alive. |
| 2026-07-12 | Automatic backups prune only automatic entries. | Manual named backups remain intact while scheduled retention stays bounded. |

## Feature Plan

### A. Reliable Backup Lifecycle

**Player outcome**

An owner can save a named point in time, find it later, load it safely, recover from the latest copy, or remove only the backup they intend.

**Experience**

- `/cb backup save [name]` creates a named save or a clear timestamp-style name.
- `/cb backup list` shows recent backups with useful metadata, newest first.
- `/cb backup load <name>` shows the consequence, asks for confirmation, and refreshes the recovered game data.
- `/cb backup delete <name>` affects backup data only, never live blocks.
- `/cb backup` opens the Backup Screen for a player and returns text from a server console.

**Requirements**

- Each backup includes block data, textures or source metadata, configuration snapshot, manifest, and timestamp.
- Writes use a temporary location followed by an atomic rename.
- Loading verifies the chosen backup before swapping it into place.
- Restore serializes with block mutations and requests the required pack/client refresh after success.
- Duplicate and invalid names are refused before any file is created.

**Boundary**

G09 guarantees a reliable data boundary. It does not define how a particular tool or block mutation works.

### B. Automation

**Player outcome**

The owner gets quiet scheduled protection without hand-running backups, on top of the normal confirm-gated load flow in §A.

**Experience**

- Auto-backup uses timestamp-style names and runs quietly on the configured schedule.
- Setting the interval to zero disables scheduling.
- The configured keep count removes only older automatic entries.
- Backup configuration is protected by the server-config confirmation gate.

**Requirements**

- Auto-backup scheduling is bounded and does not block normal server work.
- The default interval and keep count are persisted configuration, not hardcoded one-off behavior.
- Pruning distinguishes automatic saves from manual names and from pinned recovery records.
- A server restart does not make valid backups unrecoverable.

**Boundary**

Automation runs the same §A save path on a schedule; it is not a shortcut around permission, confirmation, or pack-consistency rules.

### C. Trash and Recycle-Bin Recovery

**Player outcome**

A deleted block is visible, recoverable, and clearly distinguishable from one that has been permanently emptied.

**Experience**

- `/cb trash` opens a Screen with deletion time and the saved block record.
- Restore returns the block through the G06 Recycle-Bin contract and heals eligible markers.
- An ID collision refuses restore cleanly rather than overwriting a live definition.
- Pin protects an entry from retention pruning.
- Empty permanently removes the recovery record without touching unrelated live blocks.

**Requirements**

- Trash reads the persisted records created by G06 deletion rather than duplicating them.
- Restore and Empty call the shared G06 services for marker and slot behavior.
- The Trash Screen keeps the current entry selected after a safe refresh where practical.
- Screens use a real item/texture preview route where that data is available, never a misleading placeholder.

**Boundary**

G09 presents recovery. G06 remains the authority for delete conversion, marker identity, slot reservation, and world healing.

### D. Data Layout and External Handoff

**Player outcome**

Backup and runtime data have clear local homes, and future remote backup can be added without weakening local restore safety.

**Experience**

- Runtime data uses the established `config/customblocks/data/` layout.
- Backups live under `config/customblocks/backups/`.
- A future Vault handoff keeps local backups as the primary recovery source.

**Requirements**

- Storage classes use the central data-path constants rather than inventing root-level paths.
- Any cloud export is a copy/handoff operation; it cannot silently replace the local backup lifecycle.
- File names and manifests remain valid on the supported server filesystem.

**Boundary**

G09 owns local safety. G20 owns remote integration and credentials.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G06 | Recycle-Bin and Trash | G09 displays and requests recovery; G06 performs deletion, restore, Empty, marker healing, and slot handling. |
| G07 | Set All safety | G07 routes Set All through G09's backup/apply safety boundary. |
| G16 | Diagnostics | Broken-block findings and repairs route to G16, while safety surfaces may link there. |
| G20 | Vault handoff | A remote copy uses the reliable local backup artifact and cannot bypass it. |
| G27 | Screens | Backup and Trash use the shared Screen framework while keeping G09 command/data behavior. |
| G05 | Pack refresh after load | A successful load uses the normal generated-pack delivery and client refresh route. |

## Technical Contract

- Backup artifacts are written to a temporary target, verified, and atomically promoted to their final name.
- A backup contains data required to reconstruct block definitions and their related configuration/assets without reading partially live state.
- Load makes a current-state safety copy, verifies the target artifact, serializes mutations, swaps state safely, and starts the normal pack refresh path.
- Automatic retention only prunes automatic backups; manual and pinned records are not swept by that rule.
- Trash data is a view of persisted Recycle-Bin records, not a second deletion database.
- Console command paths do not attempt to open Screens.
- Storage paths remain under `config/customblocks/data/` and `config/customblocks/backups/` through shared path constants.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Vault/cloud backup handoff | Depends on the G20 remote integration contract and credentials flow. | G20 with G09 |
| Further Trash browsing polish | The core recovery and G06 alignment come before optional presentation additions. | G09 with G27 |
| New legacy-data migration | No real legacy format is present in this codebase. | New group after evidence exists |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-27 | Trash was a separate deleted-block store with old slot behavior. | It is the recovery surface for G06's unified Recycle-Bin rail. |
| 2026-07-09 | Backup and Trash were chest-menu targets. | They use the shared G27 Screen pattern. |
| 2026-07-12 | G09 owned broken-block scanning and repair. | Diagnostics belongs to G16. |
| 2026-07-12 | A first-boot migration converted speculative gzip or `.dat` formats. | The feature is removed until a real legacy format is found. |
| 2026-07-19 | `/cb recover` and `/cb backup panic` were planned no-confirm/shortcut emergency routes. | Scrapped as redundant with `/cb backup load <newest>` through the existing confirm-gated safe-restore rail; never implemented. |
| 2026-07-19 | `/cb backup restore` was a hidden alias for `/cb backup load`. | Removed as a duplicate literal; `load` is the only verb. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_09.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G16 Diagnostics](GROUP_16_DIAGNOSTICS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 09 snapshot](../archive/group-migration-2026-07-18/GROUP_09_BACKUP_SAFETY.md)
