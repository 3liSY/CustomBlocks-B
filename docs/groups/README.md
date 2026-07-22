# Current Group Documents

Each Group document is the current design and ownership source for one feature area. Start from the [documentation dashboard](../testing/00_DASHBOARD.md), then open the linked Group document and Testing Guide together.

## What Belongs Here

- Purpose, ownership boundaries, and a short Direction section.
- Locked product decisions and detailed Feature Plan specifications.
- Cross-group contracts and enduring technical constraints.
- Deferred or superseded design decisions, kept folded and dated when they remain useful context.

## What Does Not Belong Here

- Verdicts, progress bars, test matrices, test dates, or owner-confirmed results.
- Build diaries, repeated bug boards, or copied historical phase plans.
- A second definition of the testing status system.

Use [GROUP_TEMPLATE.md](GROUP_TEMPLATE.md) for new Groups and when bringing an existing Group document into the current structure. Its linked navigation is part of the template, so a Group remains easy to scan even when its plan is detailed. The linked Testing Guide owns testing and confirmation.

Run `python docs/groups/group_tools.py check <group-document>` after changing a migrated Group. `python docs/groups/group_tools.py audit` lists Groups still awaiting migration. Strict `health` is enabled only after the whole migration is complete.
