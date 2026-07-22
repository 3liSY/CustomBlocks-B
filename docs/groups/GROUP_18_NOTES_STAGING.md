# Group 18 - Block Lore

> Group 18 gives a CustomBlock one clear editable Lore feature: saved lines that appear on its item hover when enabled.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_18.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Block metadata should be understandable at a glance. Lore is the vanilla-style item-hover text a creator expects, not a confusing collection of separate note, tooltip, and to-do concepts. It remains editable, can be hidden without loss, and later can be shared safely through Vault.

G18 owns Lore data, migration, item hover synchronization, commands, and import semantics. G27 owns the final Lore Screen.

## Ownership

| Owns | Does not own |
| --- | --- |
| Lore lines, enabled state, migration, persistence, and hover synchronization | Lore Screen layout and text-editing UX: G27 |
| `/cb lore` command and `/cb note` compatibility alias | Vault transport and remote service: G20 |
| Lore share/import data format and overwrite confirmation | General item name/tooltip infrastructure beyond Lore data |
| Editor Lore entry-point behavior | Retired staging/draft workflow |

## Direction

Each block has one Lore record: an ordered list of lines plus an enabled flag. Enabled Lore appears beneath the item name in Minecraft-style formatting; disabling it hides the lines without deleting them. `/cb lore` is the primary command and `/cb note` remains a compatibility alias.

The final editor is a real G27 Screen with proper text fields. It calls the same data/command services, rather than rebuilding Lore logic in UI code.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | Note is renamed to Lore. | One concept replaces separate note, tooltip, and hover fields. |
| 2026-06-21 | Lore stores multiple ordered lines and one enabled flag. | Item hover can show useful multi-line metadata without a second tooltip feature. |
| 2026-06-21 | `/cb lore` is primary and `/cb note` is an alias. | Existing habit works while new documentation is clear. |
| 2026-06-21 | `&` formatting codes render in editor and item hover. | Existing colored/formatted Lore remains expressive. |
| 2026-06-21 | Lore editing is operator-only. | Block metadata cannot be edited by ordinary players. |
| 2026-06-21 | Quick-add and clear remain command shortcuts. | `/cb lore <id> <text>` adds one line; `clear` removes lines and disables Lore. |
| 2026-06-21 | Share/import retains confirmation before overwrite. | A remote code cannot silently replace a block's existing Lore. |
| 2026-07-09 | Lore uses a G27 Screen with real text fields. | Anvil-per-line and the old book/chest GUI are not the end state. |
| 2026-07-12 | To-Do, writable book, tabs, and staging/draft behavior are removed. | No retired data/menu/command path remains active. |

## Feature Plan

### A. Lore Data and Migration

**Player outcome**

Existing notes become useful Lore without losing readable text, while intentionally removed To-Do data does not reappear as confusing lines.

**Experience**

- A block stores ordered Lore lines and whether they are visible.
- Existing flat notes migrate to one Lore line.
- Older tooltip and multi-line Lore merge into a sensible line list.
- Existing To-Do items are intentionally dropped because the feature is removed.
- Malformed old data leaves the server running and creates an actionable diagnostics finding.

**Requirements**

- `notes.json` uses a `{ lines, enabled }` record per block.
- Migration is idempotent and atomic.
- Legacy shared Lore codes pass through the same parser/migration route.
- Clear removes lines and disables the record without leaving orphan data.

**Boundary**

This migration preserves Lore text where possible. It does not recreate To-Do or staging features.

### B. Item Hover and Commands

**Player outcome**

Lore appears correctly on every recipient's item hover and is easy for an operator to add, edit, hide, or clear.

**Experience**

- All enabled lines render in order beneath the block item name.
- Formatting codes render as formatting, not raw clutter.
- Toggling off hides Lore while retaining the saved lines.
- `/cb lore <id>`, quick-add, and clear work with `/cb note` as a compatibility alias.
- The Editor Lore button opens the same Lore surface.

**Requirements**

- Lore data synchronizes through the normal client cache/tooltip route for dedicated clients.
- `SlotBlock.appendTooltip` reads the current synced Lore without client-side server-map assumptions.
- Commands validate block ID and operator permission before mutation.
- A missing block cannot create orphan Lore data.

**Boundary**

G18 controls Lore lines and visibility only; it does not take over block naming or unrelated item tooltip behavior.

### C. Lore Screen and Sharing

**Player outcome**

Operators can edit lines naturally in a Screen and later share/import a Lore record without exposing a server address or overwriting data accidentally.

**Experience**

- The G27 Lore Screen lists, adds, edits, deletes, and toggles lines with real text input.
- Share has an honest unavailable state until Vault is configured.
- Import creates or updates Lore only after the required overwrite confirmation.
- Old shared data upgrades into the current line model on import.

**Requirements**

- The Screen calls G18 data services and preserves navigation/unsaved-state behavior according to G27 rules.
- Vault codes use the remote Vault endpoint, never a Minecraft server host/IP.
- Invalid codes leave existing Lore unchanged and report a human-readable result.

**Boundary**

G18 owns Lore content format. G20 owns remote Vault transport and credentials.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Human feedback | Lore errors and confirmations use shared clear message rules. |
| G16 | Diagnostics | Malformed migration/import can create diagnostic evidence without crashing. |
| G20 | Vault | G20 transports Lore codes; G18 validates and applies the Lore payload. |
| G27 | Lore Screen | G27 hosts editing; G18 remains the data/command source of truth. |
| G28 | History | Lore mutation exposes normal reversible boundaries where supported. |

## Technical Contract

- `NoteData` is an ordered list of lines plus an enabled flag, persisted atomically under the shared data directory.
- Legacy flat/lore/tooltip records parse through one migration path; To-Do fields are intentionally discarded.
- Server Lore state synchronizes to clients before `SlotBlock.appendTooltip` renders it.
- `/cb lore` and `/cb note` invoke the same validated operator-gated handlers.
- Share/import uses the current Lore JSON shape, confirms overwrite, and never exposes a server host/IP.
- G27 controls only presentation and calls G18 services for every mutation.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Final Lore Screen | G27 owns its Screen implementation. | G27 with G18 |
| Lore share/import | Requires configured and verified Vault transport. | G20 with G18 |
| Richer Lore editing options | Needs a stable Screen/editor model first. | G18 with G27 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Notes, a separate tooltip, and To-Do tabs coexist. | One Lore line list supplies item hover text. |
| 2026-06-21 | Lore uses a writable book and tabbed chest flow. | The end state is a single G27 Screen. |
| 2026-06-21 | Staging/draft commands are part of G18. | They are removed entirely. |
| 2026-07-09 | An anvil-per-line flow is the intended editor. | Proper Screen text fields are the intended editor. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_18.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 18 snapshot](../archive/group-migration-2026-07-18/GROUP_18_NOTES_STAGING.md)
