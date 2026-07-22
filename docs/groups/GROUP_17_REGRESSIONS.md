# Group 17 - Command Regressions

> Group 17 keeps everyday CustomBlocks commands at least as capable, safe, and clear as their trusted older behavior.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_17.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

This Group guards a focused set of command regressions: multi-step history, safe item giving, targeted delete, and a usable search route. It prevents old convenience from quietly disappearing while keeping ownership boundaries clear.

## Ownership

| Owns | Does not own |
| --- | --- |
| Multi-undo, multi-redo, and history-clear command semantics | History persistence/engine: G28 |
| `/cb give` amount and recipient behavior | Favorites and recent-block features: G25 |
| `/cb delete #` targeted delete command behavior | Delete/Trash rail: G06/G09 |
| Search command route and no-results behavior | Search Screen design/migration: G27 |

## Direction

Commands remain compact but expressive. Multi-step undo/redo gives value-bearing feedback, recipient giving is permission-gated and inventory-safe, and targeted delete uses the normal recoverable deletion path. Search must remain a results UI for matches, while G27 owns its final Screen form.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | `undo <N>`, `undo all`, `redo <N>`, and `redo all` operate on available history without over-count errors. | Multi-step command history remains useful and reports the real result count. |
| 2026-06-21 | `undo clear` confirms and clears undo plus redo stacks. | A destructive history reset cannot happen by accident. |
| 2026-06-21 | `/cb give <id> [amount] [player]` supports 1-6400 items. | Overflow reports what did not fit and never drops surprise items. |
| 2026-06-21 | Giving to another online player requires permission level 2. | Self-give remains open while cross-player delivery stays controlled. |
| 2026-06-21 | `/cb delete #` resolves the looked-at normal custom slot block and relies on undo rather than a confirmation. | It shares normal delete safeguards and rejects air, vanilla, and non-normal targets clearly. |
| 2026-06-21 | Favorites/recent and export alignment are not G17 scope. | G25 and G12/G10 own their own feature evolution. |
| 2026-07-10 | Search moves to a G27 Screen. | G17 preserves command routing and no-results behavior, not chest/Screen layout. |

## Feature Plan

### A. Multi-Step History Commands

**Player outcome**

Players can undo or redo several actions, all available actions, or deliberately clear history with useful detail.

**Experience**

- `undo <N>` and `redo <N>` act on up to the requested available steps.
- `undo all` and `redo all` handle the full relevant stack.
- Each summary includes changed values and remaining count where useful.
- A batch remains one history step.
- `undo clear` presents the normal confirmation flow and clears both stacks only after confirmation.

**Requirements**

- Command behavior honors the existing per-player/global history mode.
- Tab completion offers numeric examples, `all`, and `clear` where valid.
- Over-count requests report the actual count instead of an error.

**Boundary**

G17 owns command semantics and regression coverage, not the internal history storage model.

### B. Safe Give Command

**Player outcome**

Players can receive the requested number of items, and authorized operators can send items to another online player without accidental item drops or vague feedback.

**Experience**

- Self-give supports one item or an amount from 1 through 6400.
- Only the portion that fits enters inventory; the remainder is reported.
- Authorized recipient giving informs both sender and receiver.
- Offline or unknown recipients produce a clear error.

**Requirements**

- Recipient form requires permission level 2; self-give does not.
- Suggestions include useful amounts and online player names.
- Item identity/name resolution follows the normal custom-block giving path.

**Boundary**

Giving does not create a second item or inventory system.

### C. Targeted Delete

**Player outcome**

Looking at a normal custom block and typing `/cb delete #` deletes that block definition through the usual recoverable route.

**Experience**

- Server raycast resolves the target within the supported interaction reach.
- The result says which block was deleted and points to normal undo.
- Air, vanilla blocks, and unsupported targets give a plain rejection without changing the world.

**Requirements**

- `#` resolves to the same deletion service as `/cb delete <id>`.
- Lock checks, snapshots, recycle behavior, and history use the shared delete contract.
- A bad target cannot delete a neighboring or arbitrary block.

**Boundary**

G17 owns shorthand routing only. G06/G09 own deletion, recovery, and Trash behavior.

### D. Search Route

**Player outcome**

Searching finds matching blocks through a useful results surface and avoids opening an empty UI when no blocks match.

**Experience**

- Matches open the current results UI and can hand off to block editing.
- Zero matches return a clear chat result.
- Final Screen layout, search interaction, and migration are handled by G27.

**Requirements**

- Command parsing and result selection remain stable across UI migration.
- Console behavior stays textual where a player Screen cannot open.

**Boundary**

The Group guards command capability; it does not own visual Search UI design.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G06 | Targeted delete | `delete #` routes to the one deletion service. |
| G09 | Recoverable data | Targeted delete retains normal Trash/recovery behavior. |
| G25 | Favorites and recent | G17 does not reopen these moved command areas. |
| G27 | Search Screen | G27 changes presentation without breaking G17 search routing. |
| G28 | History engine | G17 commands call the shared history model and respect its configured mode. |

## Technical Contract

- Multi-undo/redo execute through the existing history engine and treat one batch as one step.
- History clear uses the normal confirmation holder and clears both undo and redo only after approval.
- Give validates item ID, amount, recipient availability, permission, and inventory capacity before reporting the result.
- Targeted delete uses a bounded server-side raycast and then the normal deletion service.
- Search produces either a valid results route or a clear no-match result; G27 may replace the UI without changing this contract.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Final Search Screen migration | Screen behavior/design belongs to G27. | G27 |
| Favorites and recent commands | Ownership moved from G17. | G25 |
| Export command alignment | Requires the separate export work. | G12 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Undo/redo act on one step only. | Multi-step, all, and confirmed clear flows are supported. |
| 2026-06-21 | `give` supports only one item to self. | Amount and authorized recipient forms are supported. |
| 2026-06-21 | Favorites/recent remain G17 regressions. | They are G25 ownership. |
| 2026-07-10 | Search chest UI is G17's final target. | G27 owns Screen migration; G17 keeps command routing. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_17.md) · [All Groups](README.md)

- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G09 Backup, Data Safety, and Trash](GROUP_09_BACKUP_SAFETY.md)
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
- [G25 Block Management Extras](GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 17 snapshot](../archive/group-migration-2026-07-18/GROUP_17_REGRESSIONS.md)
