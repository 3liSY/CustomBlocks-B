# Group 04 - Messages & Command Communication

> Group 04 owns the clear, human language CustomBlocks gives players in chat, on the hotbar, and when the mod disconnects them.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/TESTING_GUIDE_04.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Players should always understand what CustomBlocks did, what went wrong, and what they can do next. Chat, hotbar feedback, interactive chips, command guidance, and CustomBlocks-owned disconnect screens need one predictable voice and visual meaning.

The Group protects the difference between a useful explanation and an invented answer. Every button must perform its displayed real action, every error must be honest, and technical detail must support the explanation instead of replacing it.

## Ownership

| Owns | Does not own |
| --- | --- |
| `/cb` chat messages, command suggestions, clickable chat chips, hotbar tool feedback, and CustomBlocks kick screens | In-screen action-bar layout and editor controls: G27 |
| Human error wording and incident links | Incident storage and recording: G16 |
| Message routes and color meaning | Template feature behavior: G12 |

## Direction

CustomBlocks communication should be calm, direct, and useful. Chat explains command outcomes, the hotbar responds to hand actions, and a CustomBlocks kick screen gives the player a human explanation, a fix, and expandable technical context. No route pretends an action worked, hides a real command behind a vague chip, or gives a made-up suggestion.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| - | Chat is for command outcomes; the hotbar is for hand actions. | One action has one clear route, with no duplicate or misplaced feedback. |
| - | Clickable chips use the displayed real `/cb` command. | No hidden token commands, expiring click targets, or generic View actions. |
| - | Every CustomBlocks-caused kick uses one human screen. | Future kick paths attach to the same screen instead of inventing new disconnect wording. |
| - | The report control is named `Copy Report`. | The copied report is pasteable, includes useful technical context, and never mentions AI. |

## Feature Plan

### A. Message Routes

**Player outcome**

Players receive one clear answer in the correct place, with a stable color meaning.

**Experience**

- Command responses go to chat and begin with `[CB]`.
- Hand actions go to the hotbar without the `[CB]` prefix.
- Green means a tool action worked, red means it failed, light gray is a neutral fact, and reported values use yellow.
- A command result never appears as a hotbar message; a hand-action result never becomes a chat response.
- The same hotbar line inside 20 ticks is swallowed, while a different line appears immediately.
- Player-facing headlines never expose internal exceptions, class names, or raw implementation detail.

**Requirements**

- Incident messages retain their real incident code and provide a Details action for `/cb incidents <code>`.
- Screen feedback uses Group 04 wording even when G27 owns the surrounding controls.

**Boundary**

G27 owns editor and screen action-bar controls. Group 04 owns only the message text passed into those surfaces.

### B. Command Guidance

**Player outcome**

Players get useful help for real commands without accidental command execution.

**Experience**

- The registered `/cb` command tree is captured after registration.
- DidYouMean and help read that capture, so newly registered commands do not need a second hand-maintained list.
- Suggestions are clickable but never run by themselves.
- Renamed commands may offer a suggestion-only alias that preserves the remaining typed command.
- Clearly unrelated input receives an honest unknown-command answer rather than a fabricated suggestion.

**Requirements**

- Typo correction is always "smart" (confident hits only) and is not configurable (owner-locked 2026-07-18). The old `smart`/`always`/`off` config value, its `/cb config didyoumean` command, and its Settings-GUI entry were removed; there is no way to weaken or disable suggestions.

**Boundary**

This Group guides a player toward a real command; it does not invent command behavior or own the feature reached by that command.

### C. Interactive Chips and Rename

**Player outcome**

Clickable chat controls do exactly what their label promises, and no-op actions say so clearly.

**Experience**

- Run and Edit chips use their displayed real `/cb` command.
- Undo and Redo use the real `/cb undo` and `/cb redo` commands.
- Copy appears only for a specific real value, such as a share code or diagnostic value.
- The incident Details chip runs `/cb incidents <code>`.
- `/cb rename <id> <current name>` says the block already has that name, changes nothing, and creates no undo entry.

**Requirements**

- A chip cannot depend on a hidden token, a generic copy action, a separate View action, or an expiring target.

**Boundary**

The feature behind a chip remains owned by its Group; Group 04 owns the message and real command route.

### D. CustomBlocks Kick Screen

**Player outcome**

A player disconnected by CustomBlocks understands what happened, why, and how to recover.

**Experience**

- Every screen presents What happened, Why, and How to fix it in that order.
- The title is red, explanation white, why yellow, fix green, controls light blue, and technical text gray.
- Technical Details expands the raw error and diagnostic context without replacing the human explanation.
- `Copy Report` copies the human explanation and technical context in a pasteable form.

**Requirements**

| Cause | Screen behavior | Preview route |
| --- | --- | --- |
| Registry or block-limit desync | The unified screen explains the mismatch. | `/cb debug kick registry` |
| Resource-pack apply failure | The unified screen has a prepared explanation shape. | `/cb debug kick resourcepack` |
| Future generic CustomBlocks kick | The unified screen has a prepared explanation shape. | `/cb debug kick generic` |

- Debug routes preview a cause without forcing a real disconnect.
- A future CustomBlocks kick attaches to this same screen and receives the matching Testing Guide coverage before it is treated as unified.

**Boundary**

This Group owns CustomBlocks-caused disconnect wording and the screen contract. It does not claim vanilla or unrelated server disconnects.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G16 | Incident details | Group 04 routes players to real incident codes; G16 stores and presents incident records. |
| G27 | Screen feedback | G27 owns screen controls and action-bar layout; Group 04 supplies clear player-facing wording. |
| G12 | Template behavior | Group 04 does not describe template behavior as deleted or scrapped. |

## Technical Contract

- `Chat`, `CbFmt`, and `HotbarSpeaker` provide the message route and formatting behavior.
- `CommandRegistrar`, `CommandTree`, and `DidYouMean` provide real-command guidance.
- Chat helpers and their command call sites provide interactive chips.
- `CbKickScreen`, kick helpers, and client mixins provide the CustomBlocks kick surface.
- `CreationCommands.rename` owns the unchanged-name no-op behavior.
- The Group never converts source inspection into an owner-confirmed claim; confirmation remains in TG4.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| New CustomBlocks kick causes | Each cause needs a real trigger path and matching TG coverage before it is added. | G04 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| - | Voice mode toggle | One professional tone remains. |
| - | Generic ordinary-message Copy chip | Copy appears only for a specific real value. |
| - | Separate View chip | The honest Edit path replaces it. |
| - | Read-only chat direction | Interactive chat remains. |
| - | Rich hover card | Dropped as unnecessary. |
| 2026-07-18 | Configurable typo-correction modes (`smart`/`always`/`off`) | Always smart, not editable; the config value, command, and GUI entry were removed. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/TESTING_GUIDE_04.md) · [All Groups](README.md)

- [G16 Diagnostics](GROUP_16_DIAGNOSTICS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
