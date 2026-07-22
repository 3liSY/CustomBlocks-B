# Group 16 - Diagnostics and Private Testing

> Group 16 gives the owner a trustworthy place to inspect CustomBlocks health, repair known problems, control feedback, and run private in-game tests without altering the official guides.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_16.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Diagnostics should turn an incident into understandable evidence and a safe next action. The private Testing Center should let its owner test directly in-game, record only results, and later remove the temporary system cleanly without modifying the official documentation or exposing it to other players.

G16 owns diagnostic data/surfaces, broken-block recovery, feedback FX, and the private Testing Center. It displays mutation history but does not own the global history engine; it links to Trash but does not own deletion behavior.

## Ownership

| Owns | Does not own |
| --- | --- |
| Incidents, diagnostic health, debug log, cache/audit/report actions | Shared mutation/undo engine: G28/G25 |
| Broken-block scanner and repair surface | Delete/restore/marker behavior: G06/G09 |
| Feedback FX settings for particles and sounds | General player-experience screenshot feature: G23 |
| Private Testing Center access, result storage, and teardown | Official Testing Guide content and external findings workflow |
| IT Screen and Feedback Screen behavior | Shared Screen system and visual language: G27 |

## Direction

Diagnostics consolidate into one tabbed IT Screen for health, incidents, audit, report, and debug log. Feedback FX stays a separate surface because it also has direct commands. Existing diagnostic capability is migrated, not lost.

`/cb testing` is a private, temporary owner workspace. It is available only when both the configured Minecraft account and configured server match. It shows the real Testing Guides unchanged on the left and stores owner-entered results on the right. When the work ends, stored test data can be destroyed from inside the game; removing command/UI/source integration is a separate deliberate final cleanup.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | Incidents are structured with severity, actor/system, context, and block ID where available. | Diagnostics can show actionable evidence instead of raw error strings only. |
| 2026-06-21 | Broken-block diagnostics belongs to G16. | G09 may link to it but does not own scanner or repair UI. |
| 2026-06-21 | Feedback merges particle and sound controls into one splittable system. | A master toggle controls both, while each channel remains independently configurable. |
| 2026-06-21 | Server-side screenshot capture is dropped. | A headless server does not pretend to have a framebuffer; any future screenshot is G23 scope. |
| 2026-07-09 | IT Chest, audit, report, and debug log migrate to one tabbed IT Screen. | Diagnostic chest menus do not remain as competing end-state UI. |
| 2026-07-09 | Feedback FX becomes its own Screen. | It stays reachable from its direct commands and is not forced into the IT tabs. |
| 2026-07-18 | Testing Center access requires both owner account and configured server. | It is unavailable to every other player, including operators, and unavailable to the owner on another server. |
| 2026-07-18 | Testing Center guide text is read-only and results auto-save server-side for the owner. | The in-game workspace records findings without rewriting official guides. |
| 2026-07-18 | Testing Center teardown removes runtime test data; source removal is a separate final pass. | The running mod does not attempt to delete its own source integration. |

## Feature Plan

### A. IT Diagnostics

**Player outcome**

An administrator can see health, incidents, recent mutations, and repair options without hunting through logs or disconnected menus.

**Experience**

- `/cb diag` and `/cb incidents` open the relevant IT view.
- Health covers TPS, slot capacity, sync/pack state, and memory with honest best-effort values.
- Incidents show newest first with timestamp, actor/system, context, block ID, severity, and detail.
- Mutation history opens the relevant editor where the block remains available.
- Audit, cache inspection/cleanup, report generation, and Debug Log are reachable without losing the current tab/filter context.

**Requirements**

- `IncidentRecorder` writes structured entries atomically and older entries degrade safely when fields are absent.
- Auto-fix may re-download a known texture source, route to editor, restore from backup, or send deletion through the normal Trash path; it never invents a destructive fix.
- Cache clear excludes live textures, sources, generated packs, and backups.
- Debug Log filters `[CustomBlocks]` lines by severity and supports copyable evidence.
- Console commands provide text output rather than opening a Screen.

**Boundary**

G16 displays diagnostic history and repair actions. It does not replace the undo engine, backup contract, or deletion rail.

### B. Broken-Block Recovery

**Player outcome**

A broken/missing-texture block is easy to identify and has a safe fix route or a clear reason why it cannot be fixed automatically.

**Experience**

- `/cb showbrokenblocks` shows a calm empty state when nothing is wrong.
- Entries identify the block, slot/source condition, and available actions.
- Fixes re-bake from saved source when possible; source-less blocks route to retexture or an honest skip.
- Bulk selection persists across pages and deletion goes to recoverable Trash, not hard deletion.

**Requirements**

- Scanner, list, confirm, and repair classes live in the diagnostics package.
- Fix routes use G05/G10 texture work and G06/G09 recoverable deletion as appropriate.
- G09 Safety routes link into diagnostics without duplicating this UI.

**Boundary**

This Group diagnoses and routes a repair. It does not redefine what a valid texture, delete, or restore means.

### C. Feedback FX

**Player outcome**

Players can control visual and sound feedback by category, together or independently, without surprise effects.

**Experience**

- `/cb feedback`, `/cb particles`, and `/cb sounds` reach one Feedback surface.
- A category master toggle changes both particle and sound values; expanded controls change either one.
- Preview plays both channels even if a category is currently disabled.
- Success, error, GUI, selection, bulk completion, and manual pack regeneration have distinct feedback routes.

**Requirements**

- Per-category particle and sound flags persist independently.
- Master control writes both flags instead of creating a third competing state.
- Bulk completion and manual pack regeneration use their dedicated categories rather than duplicate generic success feedback.
- Achievement feedback remains preview-only until an achievement system supplies real triggers.

**Boundary**

Feedback control applies only to actual emitted effects. It does not claim a category is live when no source event exists.

### D. Private Testing Center

**Player outcome**

The owner can test every real Testing Guide in game, see overall and per-guide progress, record professional findings, and export a clean report without exposing the system to other players.

**Experience**

- `/cb testing` opens only for the configured owner account on the configured server; the temporary initial server is `yoyoo.mcsh.io`.
- The dashboard includes completed and unfinished guides, overall/per-guide percentages, test counts, filters, recent activity, and next useful test.
- A guide view keeps the original Testing Guide read-only on the left and owner results on the right.
- Results support pass/fail/blocked/retest, expected versus actual behavior, notes, severity, reproduction, environment, date/time, evidence, follow-up, and edit history.
- Result entries auto-save and reappear for the same owner/server from another computer.
- Report export produces copyable professional findings without mentioning an external AI workflow.

**Requirements**

- Authorization checks both UUID/account and configured server identity for every entry/action, not merely operator level.
- Server-side storage keys results to owner and server, never modifies the official guide Markdown, and uses the same guide/section/test identifiers as the existing docs.
- Progress derives from results without altering the guide's official verdict/status fields.
- The UI follows the G27 Screen system while retaining the intended SrbProjects-style dashboard clarity.

**Boundary**

Only owner results are editable in game. Official guides and external documentation remain separate and read-only here.

### E. Testing Center Teardown

**Player outcome**

When all testing is finished, the temporary private workspace can be removed completely and clearly.

**Experience**

- A plainly named destructive control requires deliberate confirmation.
- It removes private result data, activity/history, evidence references, cached progress, and the runtime access lock.
- The command is unavailable after runtime teardown.
- Final project cleanup removes the temporary command, UI, configuration, assets, and source integration from CustomBlocks-B.

**Requirements**

- Runtime teardown targets only the Testing Center's own persisted data after final confirmation.
- It does not delete official guide documents, unrelated backups, or project source files at runtime.
- Source removal is tracked as an explicit final maintenance task after data teardown.

**Boundary**

Runtime cleanup erases testing data. Repository cleanup erases implementation; they are intentionally separate operations.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Error presentation | G04 routes human-facing errors; G16 stores/views diagnostic incidents. |
| G05 | Pack failures | Pack diagnostics report status and may use normal refresh/rebuild routes without duplicating delivery. |
| G06 | Recoverable delete | Broken-block deletion and auto-fix use the shared Recycle-Bin/Trash contract. |
| G09 | Safety link | G09 Safety may enter G16 diagnostics; G16 does not own backup persistence. |
| G27 | Screen system | G27 supplies IT, Feedback, and Testing Center visual/system patterns. |
| G28 | History | G16 displays history and launches normal undo/redo routes without implementing history storage. |

## Technical Contract

- Incidents are structured, atomically persisted, bounded in retention, and retain actor/system, context, severity, block ID, and error details where known.
- Diagnostic fixes use existing create/retexture, backup, Trash, and editor routes; they do not bypass their safety conditions.
- Particle/sound feedback stores two flags per category, with master toggles writing both.
- Testing Center authorization validates the configured owner identity and server identity on every server-side request.
- Testing Center data is isolated from official guide Markdown and from unrelated server data; guide identifiers are read-only references.
- Auto-save writes only changed owner results and preserves a result-history trail.
- Runtime teardown deletes only the Testing Center data namespace; source removal is never attempted by a running server.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Live achievement feedback | Needs a real achievement event source. | G16 |
| Testing Center implementation | Design is locked; it waits for the temporary in-game build pass. | G16 with G27 |
| Final Testing Center source removal | Happens only after owner confirms all guides/testing are complete. | G16 final cleanup |
| Screenshot feature | Not viable on a headless server and is outside diagnostics scope. | G23 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Incidents are unstructured text and diagnostics are separate small menus. | Incidents are structured and migrate into one IT Screen. |
| 2026-06-21 | G09 owns broken-block scanning. | G16 owns diagnostics; G09 only links to it. |
| 2026-06-21 | Particles and sounds are independent unrelated controls. | Feedback FX combines a master control with per-channel overrides. |
| 2026-07-18 | A Testing Center could edit official guide instructions. | Guides remain read-only; only owner result records are editable. |
| 2026-07-18 | A nuke action removes source files while the mod is running. | Runtime data teardown and source cleanup are separate deliberate steps. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_16.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G09 Backup, Data Safety, and Trash](GROUP_09_BACKUP_SAFETY.md)
- [G23 Player Experience](GROUP_23_PLAYER_EXPERIENCE.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 16 snapshot](../archive/group-migration-2026-07-18/GROUP_16_DIAGNOSTICS.md)
