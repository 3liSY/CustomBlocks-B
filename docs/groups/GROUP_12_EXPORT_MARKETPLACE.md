# Group 12 - Export and Marketplace

> Group 12 turns CustomBlocks into useful local files, portable Blueprint items, and later safe Vault/Marketplace artifacts without exposing a server address or losing block data.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_12.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Exports must be honest: a locally written JSON, PNG, or ZIP is useful even when a remote browser download is unavailable. Portable items, Vault sharing, Marketplace browsing, folder imports, and heavyweight formats each need their own explicit contract rather than being presented as one finished export button.

This Group owns export contents, Blueprint behavior, folder-import requirements, and Marketplace/export routes. It does not own the common Screen framework, resource-pack HTTP delivery, Vault deployment, category schema, or image processing.

## Ownership

| Owns | Does not own |
| --- | --- |
| Local block/category/all-block export artifacts and format selection | Export and Marketplace Screen framework: G27 |
| Blueprint item creation and hand import | Vault deployment, share codes, and remote transport: G20 |
| `importfolder` image/JSON import rules | Category schema and metadata meaning: G11 |
| Download-link safety requirements for export surfaces | Resource-pack delivery infrastructure: G05 |
| Marketplace data presentation and import handoff | Image decode, bake, and validation: G10 |

## Direction

Local export goes first to `config/customblocks/cloud_exports/` and reports the actual saved artifact. Chat must never leak a server host/IP or offer an unreachable remote download as though it worked. A host-local link is only acceptable when visibly local; remote distribution belongs to an opt-in Vault URL or another deliberate delivery route.

The Export Dashboard and Marketplace are G27 Screens that call G12 export/import services. Same-server Blueprint transfer is useful as an in-game handoff, but it is not a substitute for portable Vault sharing across servers.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | A host-leaking or unreachable `[download]` link is unacceptable. | Export reports a reliable local artifact until a safe remote route exists. |
| 2026-06-21 | Blueprint is a same-server item handoff, not proof of cross-server portability. | Its value is reviewed separately from Vault sharing. |
| 2026-06-29 | `importfolder` accepts image files and applies matching textures to JSON imports. | Image-only and JSON-plus-image folder inputs have a real target behavior. |
| 2026-07-10 | Export Dashboard and Marketplace use Screens. | The current chest dashboard is a baseline only; Marketplace targets Screen directly. |
| 2026-07-12 | Export Dashboard uses a hand-picked Bulk Choose flow for multi-block bundles. | It does not return to the incorrect per-slot-click spec. |
| 2026-07-12 | Vault share/import and Marketplace wait for the G20 deployment path. | Local export remains independent and no fake remote fallback is added. |
| 2026-07-12 | Advanced binary and vanilla-pack formats remain parked. | They are not advertised as available until their real serializers and validation exist. |

## Feature Plan

### A. Local Export

**Player outcome**

Creators can export one block, a hand-picked set, a category, or all blocks to clear local artifacts.

**Experience**

- `/cb export` opens the dashboard for players and provides text output for console.
- Single-block JSON and PNG exports preserve the correct metadata or texture.
- All-block and category export create ZIP bundles with their needed data.
- The dashboard supports scope first, then format, including the hand-picked Bulk Choose path.
- The saved path is reported honestly when no safe remote browser route is available.

**Requirements**

- Artifacts live under `config/customblocks/cloud_exports/`.
- JSON, PNG, and ZIP contents are validated before being presented as successful.
- Category exports use the G11 schema and include needed metadata, assets, and assignments.
- Any download affordance must be local-only with clear wording or a verified opt-in remote URL; it must not reveal a server address.

**Boundary**

G12 creates export artifacts and owns their scope. G27 owns the screen layout; G20 owns public/Vault delivery.

### B. Blueprint Handoff

**Player outcome**

A player can hand another player a block Blueprint on the same server and import it safely from their inventory.

**Experience**

- An export action can create a Blueprint item with a texture icon and block metadata.
- The item can be dropped or traded and survives a restart.
- `/cb importblock` handles the held Blueprint and refuses duplicate IDs without overwriting a live block.

**Requirements**

- Blueprint NBT holds the data needed for same-server import.
- Item import validates the record before creation and uses the normal conflict path.
- Blueprint behavior is described as same-server until Vault sharing proves remote portability.

**Boundary**

Blueprints are in-game artifacts. They do not bypass G20 remote import or conflict handling.

### C. Vault and Marketplace

**Player outcome**

When Vault is ready, a creator can share a block by code and browse/import public shared entries through a proper Marketplace Screen.

**Experience**

- A single-block share returns a short code.
- Importing a code restores metadata and texture through a conflict-safe path.
- Player conflicts open the appropriate Screen; console conflicts receive clear text.
- Marketplace shows empty, unavailable, error, browse, preview, search/filter, and import states honestly.

**Requirements**

- G20 performs authenticated remote upload/download and reports availability.
- Marketplace import calls the same conflict-safe import service as a typed code.
- No bulk-to-Vault export is implied until its workload and artifact design are agreed.

**Boundary**

G12 owns local artifact interpretation and Marketplace behavior. G20 owns the remote service and deployment.

### D. Folder Import and Future Formats

**Player outcome**

Creators can import a practical folder of image and metadata files without manually rebuilding every texture.

**Experience**

- PNG, JPG, GIF, and WebP files can become blocks using filename-derived IDs.
- A JSON record paired with a same-name PNG applies both metadata and texture.
- Bad files are skipped with a concise report while valid entries continue.
- Later litematic, schem, vanilla resource-pack, and NBT formats appear only once their format-specific behavior is complete.

**Requirements**

- Folder import reuses G10 image decoding and source safeguards.
- Filename-to-ID mapping validates collisions and unsafe names before mutation.
- JSON and image pairing has explicit precedence rather than silently ignoring a texture.
- Heavy binary format work requires real serializers, size limits, and test fixtures.

**Boundary**

Folder import is an input workflow, not a substitute for remote Vault sharing or a catch-all import parser.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Delivery routes | Export download behavior does not interfere with or expose resource-pack delivery infrastructure. |
| G10 | Images | Folder image imports use G10 decoding/baking rules. |
| G11 | Category export | G11 supplies category schema; G12 serializes the selected category artifact. |
| G20 | Vault | G20 hosts remote upload/download; G12 applies local import/export and conflict behavior. |
| G27 | Dashboard and Marketplace | G27 supplies Screen presentation; G12 provides routes, scopes, and actions. |

## Technical Contract

- Local export paths are under `config/customblocks/cloud_exports/`.
- Export services validate an artifact before reporting success and distinguish local file creation from browser delivery.
- Same-server Blueprint import validates NBT and applies duplicate-ID protection.
- Vault code import and Marketplace import use one conflict-safe local import rail.
- `importfolder` recognizes supported images and JSON-plus-matching-image pairs, reports bad entries, and continues valid ones.
- Screens call G12 export/import routes; console routes stay textual.
- No client-visible export control may emit a raw server host/IP in chat.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Vault share/import and Marketplace | Requires G20 deployment and safe remote service behavior. | G20 with G12 |
| Advanced litematic, schem, vanilla-pack, and NBT formats | Needs dedicated serializers, validation, and fixture coverage. | G12 |
| Bulk Vault export | Needs a separate workload, conflict, and artifact design. | G12 with G20 |
| Richer Blueprint purpose | Requires a reason beyond same-server handoff. | G12 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Chat `[download]` links could expose a server host and still count as a finished export. | Local artifact output is primary until a safe verified route exists. |
| 2026-07-10 | Export Dashboard remains a chest GUI. | It migrates to the G27 Screen system. |
| 2026-07-12 | The Dashboard picks each export from a per-slot block menu. | Multi-block export uses the hand-picked Bulk Choose flow. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_12.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G11 Categories](GROUP_11_CATEGORY.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Image Input Overhaul](../Information/IMAGE_INPUT_OVERHAUL.md)
- [Pre-template Group 12 snapshot](../archive/group-migration-2026-07-18/GROUP_12_EXPORT_MARKETPLACE.md)
