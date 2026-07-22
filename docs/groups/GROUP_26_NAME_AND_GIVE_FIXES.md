# Group 26 - Display Names, ID Resolution, and Texture Mirrors

> Group 26 keeps player-facing block names clean, makes ID lookup forgiving without changing stored identity, and offers an optional human-readable texture mirror that can never affect the real block assets.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_26.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Block display names should look like names, not file fragments. Players also should not lose a valid block merely because they type its ID with different capitalization. Both fixes must leave canonical IDs and slot-keyed texture files unchanged.

For people who browse the server files, the optional named-texture mirror exposes readable PNG copies without becoming a second source of truth. Dedicated-server clients must use the server-synced name data for item/HUD names rather than showing a generic local fallback.

## Ownership

| Owns | Does not own |
| --- | --- |
| Display-name cleanup/migration and case-insensitive ID resolution | Block ID format, creation workflow, and primary texture storage |
| Optional `textures_names` write-only mirror and its manifest | Resource-pack rebuilding or asset delivery: G05 |
| Dedicated-server block/item display-name lookup from client sync | Arabic text rendering/data: G13 |
| Name/give regressions and their limited blast radius | General client prediction behavior: G04 |

## Direction

Player-facing names replace underscores with spaces and use Title Case, whether they come from legacy stored data, new creation, rename, or imported content. Stored IDs and canonical `slot_N` files stay exactly as they are. ID resolution takes an exact match first, then falls back to case-insensitive lookup deterministically; suggestions retain stored casing.

The named-texture mirror is an optional browsing aid. It writes readable copies from canonical textures after their authoritative updates and silently tolerates its own failure. Multiplayer display names read the client cache already synced by the server, with server behavior remaining authoritative and unchanged.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-15 | `NameCase.titleCase` changes underscores to spaces only in display-name paths. | IDs, filenames, and keys are never reformatted by name cleanup. |
| 2026-06-15 | Legacy stored names receive one idempotent boot migration. | Existing bundled/old blocks gain clean names without repeated rewrites. |
| 2026-06-15 | ID lookup is exact first, then case-insensitive with a deterministic tie-break. | Existing exact lookups stay fast and unchanged; player casing is forgiving. |
| 2026-06-15 | Named texture copies are write-only and disabled by default. | The mirror cannot break loading, rendering, packs, or placed blocks. |
| 2026-06-15 | One mirror writer and manifest own every mirror file. | Renames/deletes clean up known files and rebuild restores truth. |
| 2026-06-20 | Dedicated clients use `ClientSlotCache` for display names. | Item and block names reflect server data without common code directly importing a client-only class. |

## Feature Plan

### A. Clean Names and Forgiving IDs

**Player outcome**

Names read naturally, and the same existing block is found whether its ID is typed with the stored capitalization or not.

**Experience**

- `Test_black` displays as `Test Black` across list, item, editor, and imported/created content.
- A player can use `/cb give Te`, `te`, or `TE` for the same stored ID where no exact different ID exists.
- Error messages and tab suggestions retain the actual stored casing rather than concealing it.

**Requirements**

- Update display-name formatting in `NameCase.titleCase` and run one boot migration after data load, before bundled art import.
- Make the migration idempotent and persist only when names actually change.
- `SlotManager.getById`/`hasId` use exact lookup first, then a private case-insensitive search with lowest slot-index tie-break.
- Never lowercase map keys or alter canonical IDs/files as part of resolution or name formatting.

**Boundary**

This work affects player-facing display names and lookup only. It does not rename IDs, move texture assets, or change Arabic block-specific data.

### B. Named-Texture Mirror

**Player outcome**

An administrator can optionally browse a readable copy of block textures by display name without risking real CustomBlocks data.

**Experience**

- `/cb config mirrornames` reports enabled state, file count, and folder path.
- `on` enables and backfills; `off` stops updates but preserves existing browse files; `rebuild` regenerates the folder from canonical truth.
- Named main and face-override copies use sanitized display names, deterministic duplicate suffixes, and clear face suffixes.

**Requirements**

- Store `mirrorNamedTextures` as a default-off configuration field.
- `TextureNameMirror` is the sole owner of `config/customblocks/textures_names/` and `data/mirror_index.json`.
- Sync/removal hooks attach to canonical `TextureStore` and `SlotManager` writer points after authoritative data is committed.
- Write atomically, track `slot -> filename`, and treat every mirror failure as best-effort logged work that cannot fail the original create/retexture/rename/delete action.

**Boundary**

The mod never reads the mirror as a texture source and never rebuilds a pack because it changed. `textures/slot_N.png` remains the only canonical asset path.

### C. Dedicated-Server Display Names

**Player outcome**

On a dedicated server, a player sees the real current name for CustomBlocks in hand, inventory, and HUD instead of `Custom Block`.

**Experience**

- Newly created, renamed, rejoined, and second-client views all use the name last sent by the server.
- Singleplayer behavior remains unchanged.
- A cache that has not arrived yet uses the existing safe fallback without showing raw registry keys.

**Requirements**

- Client-side `SlotBlock`/item name lookup reads `ClientSlotCache` through an environment-safe resolver.
- Server-side name lookup continues to read authoritative `SlotManager` data.
- Common block/item classes must not directly hard-reference a client-only class.
- Name data updates through the existing join and mutation sync route, without inventing a second client name store.

**Boundary**

This is display text only. It does not change server block data, resource-pack content, Arabic letter item names, or general client prediction.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Multiplayer behavior | Environment-safe client lookup follows the established client/server separation and does not make a round trip for a displayed name. |
| G05 | Canonical assets | The mirror consumes canonical texture writes and never replaces asset delivery. |
| G13 | Arabic content | Arabic-specific naming/rendering continues through its own data path while shared title casing stays display-only. |
| G16 | Diagnostics | Mirror failures are contained/logged as optional-file issues and do not look like block failures. |
| G27 | Configuration Screen | G27 may expose the mirror setting/command route without owning mirror file logic. |

## Technical Contract

- `NameCase.titleCase` is a display-name-only formatter; `SlotManager.migrateDisplayNames()` is an idempotent post-load migration.
- `SlotManager.getById` and `hasId` retain exact-map fast paths and only scan case-insensitively after a miss; suggestions remain exact-cased.
- `TextureNameMirror` writes only `textures_names` and its manifest, atomically and best-effort; canonical `slot_N` files are immutable by mirror behavior.
- Canonical texture/save/delete hooks call mirror sync only after their own successful authoritative change and only while enabled.
- Dedicated client item/block names resolve via the synced client cache through an environment-guarded seam; server classes never link client-only code directly.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Mirror import or editing from readable files | It would create a second authoring/source-of-truth path. | Separate asset workflow |
| General client prediction fixes | This Group fixes name lookup only. | G04 and affected feature owner |
| Arabic GUI/search revisions | They are not part of display-name, ID, or mirror work. | G13 and G27 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-15 | Underscores could remain in player-facing Title Case. | Display-name underscores become spaces, while IDs/files remain untouched. |
| 2026-06-15 | Case-insensitive give could be fixed only in its command handler. | Central ID resolution fixes every appropriate lookup consistently. |
| 2026-06-15 | Readable named textures could become an alternate source. | The mirror is write-only, optional, and entirely off the critical path. |
| 2026-06-20 | Client item/block names could read server-only `SlotManager` data. | Dedicated clients read server-synced cache through a safe client seam. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_26.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 26 snapshot](../archive/group-migration-2026-07-18/GROUP_26_NAME_AND_GIVE_FIXES.md)
