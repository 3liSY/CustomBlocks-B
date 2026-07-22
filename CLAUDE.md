# CustomBlocks-B Agent Protocol

## Priority

1. Follow system and user instructions first.
2. Follow this protocol for workspace work.
3. For documentation work, [the glossary](docs/testing/extra/Glossary.md) is the only authority for statuses, symbols, filenames, and testing-guide structure.

## Start Here

- Read this file at the start of every workspace task.
- When touching `docs/`, read the glossary before changing, summarizing, classifying, or generating project documentation.
- Read the relevant Group document and canonical Testing Guide together before changing feature status.
- Treat source code as evidence of `Built`, never evidence of `Done`.

## Documentation Contract

Each active group has exactly two current documents:

- `docs/groups/GROUP_XX_*.md`: feature decisions, technical scope, and code truth.
- `docs/testing/Testing_Guide_XX.md`: verdict, test rows, results, and folded history.

Group documents follow `docs/groups/GROUP_TEMPLATE.md`. They use Purpose, Ownership, Direction, Locked Decisions, Feature Plan, cross-group contracts, and enduring technical constraints. Keep the template's top navigation intact. They do not use Testing Guide verdicts, progress bars, test dates, test rows, or owner-confirmation claims.

Before editing a current Group document, read its Group document, canonical Testing Guide, and `docs/groups/GROUP_TEMPLATE.md`. Preserve every still-relevant decision and specification when migrating it; do not replace detail with a vague summary or create a parallel copy. After editing a Group document, run:

```powershell
python docs/groups/group_tools.py check <group-document>
```

Do not report a Group-document repair complete unless this check passes. During the one-time migration, use `python docs/groups/group_tools.py audit` to see the remaining documents; use `health` only when every Group has been migrated.

The dashboard is a generated navigation view. It links to current documents but never overrides them. Archives preserve history but never define current behavior.

Do not create copied `Done`, `Scrapped`, plan, notes, or status documents beside a current guide. Keep confirmed, regression, planned, parked, and scrapped history folded inside the one guide.

## Historical Phase Boundary

`docs/archive/` and `docs/Information/` preserve old phase material. They are historical reference, not part of the current Group/Testing Guide system.

- Do not apply the current glossary, status template, filename lifecycle, or testing-guide rules to historical phase files.
- Do not treat historical phase statuses, decisions, or ownership maps as current truth.
- When an old record is useful as evidence, preserve its wording and date, then record the current decision in the relevant Group document or Testing Guide.
- Keep historical files clearly labelled and linked through their historical indexes; do not mix them into the current dashboard.

## Testing-Guide Lifecycle

- Active or partially complete guide: `Testing_Guide_XX.md`.
- Fully confirmed guide only: `Testing_Guide_XX_Done.md`.
- Fully scrapped guide only: `Testing_Guide_XX_Scrapped.md`.
- A suffix is a rename of the one guide, never a duplicate copy.
- `_Done.md` requires `100%`, no active test rows, and every in-scope section confirmed. Parked or scrapped future ideas may remain folded in the guide.
- `_Scrapped.md` requires no active implementation or test work.
- A filename, Verdict, Progress, Sections table, and archive must agree. Surface conflicts; never silently choose one version.

## Documentation Edits

- Use only the statuses and flags defined by the glossary.
- Keep the Verdict to one useful sentence. Keep all other status fields literal and short.
- A passed test cell is exactly `✅ YYYY-MM-DD`; use `-` when an environment cannot run a row.
- Keep technical causes, design detail, and implementation notes in the Group document or `PROGRESS_LOG.md`, not in the Testing Guide.
- Do not put a glossary, legend, AI instructions, or old status system inside a Testing Guide.
- Update every affected canonical link and regenerate the dashboard after a filename or status change.
- After changing a Testing Guide, run `python docs/testing/extra/testing_tools.py health`. Do not report the document repair complete while it has warnings.

## Conflict Handling

- Preserve evidence and dates.
- When code and docs disagree, update the document only where source proof is clear; otherwise mark the item for discussion.
- When filenames and contents disagree, the content wins until the guide is repaired and renamed.
- Never delete an untracked or historical document without first confirming it is a duplicate or moving it into the archive.

## Engineering Rules

- Inspect the existing code and follow its established patterns before editing.
- Keep changes scoped. Do not refactor unrelated code or undo user work.
- Use exact imports and official documentation when needed; do not guess APIs or add dependencies without approval.
- Slot data is immutable. `SlotManager` owns slot assignments and `SlotDataStore` owns slot disk I/O.
- Packet work returns to the server or client executor before changing game state.
- For Mixins, identify the target method and injection point before editing.
- Keep UI readable: opaque modals, no overlap or clipping, and left-side screen tabs.

## Verification

- Run focused checks that match the change.
- A passing build proves compilation only.
- Record owner game results in the canonical Testing Guide, then move a fully confirmed section into its folded archive.
- Never claim a feature is done without owner in-game confirmation.
