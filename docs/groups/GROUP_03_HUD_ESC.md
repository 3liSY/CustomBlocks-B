# Group 03 - HUD System and ESC Integration

> Group 03 owns a quiet, useful HUD and any future ESC quick-access experience without turning normal play into a wall of widgets.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_03_Done.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

The HUD should stay quiet until it gives information that matters. It owns the crosshair padlock and macro-recording feedback, and it records the boundary for a future HUD and ESC expansion without reviving the earlier overcrowded widget plan.

The Group also ensures that persistent tool mode feedback does not masquerade as live HUD state. Tool feedback belongs to the hotbar route in G04.

## Ownership

| Owns | Does not own |
| --- | --- |
| HUD overlay behavior, crosshair lock feedback, macro-recording feedback, and future ESC quick access | Tool-mode hotbar feedback: G04 |
| HUD state presentation and placement rules | Bulk command state changes: G07 |
| Future HUD/ESC screen design boundary | Shared screen framework and editor controls: G27 |

## Direction

The HUD defaults to silence. A lock is shown by a small hand-drawn padlock below the crosshair, and macro recording remains visibly unmistakable. A larger widget and ESC quick-access expansion remains a future design area, but it must be re-scoped from the current quiet-HUD direction rather than restoring the older seven-widget catalog.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-15 | Delete the persistent active-tool HUD chip. | Tool mode announces once through the G04 hotbar route instead of lingering in the HUD. |
| 2026-07-15 | Use a hand-drawn 7x9 padlock sprite below the crosshair. | The sprite is horizontally centered, 10px below the crosshair, and never blocks aim. |
| 2026-07-14 | HUD widgets are display-only. | A widget does not capture play input; its related action remains available through the existing command or keybind. |
| 2026-07-14 | Future HUD color work reuses `CbTheme`. | Red, black, and lime stay the shared screen family; do not create a second palette class. |

## Feature Plan

### A. Quiet HUD Feedback

**Player outcome**

Players see only the HUD information that matters at the moment, without a permanent tool-mode chip or aim obstruction.

**Experience**

- A locked custom block shows the 7x9 padlock below the crosshair, not on it.
- Macro recording uses a clear visual banner while recording is active.
- Tool-mode changes are announced once in the hotbar rather than rendered as persistent HUD state.

**Requirements**

- The padlock contains no owner identity; lock ownership remains a `LockManager` concern.
- HUD presentation reads live state rather than treating a persistent player preference as active interaction.
- A bulk lock must provide the same immediate padlock state expected from a single lock operation; G03 owns the widget-push fix regardless of which command (single or bulk) triggers the lock.

**Boundary**

The HUD does not become a control surface during normal play, and it does not own the commands behind its feedback.

### B. Parked QoL Widgets

**Player outcome**

Two future widgets — an incidents ping and a vault-upload progress toast — remain wanted but not urgent.

**Requirements**

- Parked until the owner unparks them; excluded from TG3 progress tracking until then.
- The Buzzer Game score/timer widget and the favorites quick-bar widget are scrapped outright, not parked — do not revive them without a fresh owner decision.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G04 | Tool feedback and incident routes | G04 owns the hotbar tool-mode announcement and shared player-facing incident wording. |
| G07 | Bulk lock state | G07 owns the bulk-lock command; G03 owns pushing the resulting lock state to the HUD so padlocks render consistently. |
| G16 | Incident access | Any future incident indicator routes to the existing incident experience. |
| G27 | Screen framework | G27 owns the shared screen framework and HUD editor surface. |
| G31 | Buzzer state | A future Buzzer display consumes real BuzzerGame state rather than inventing player-visible data. |

## Technical Contract

- `HudRenderMixin` remains the CustomBlocks overlay layer for HUD drawing.
- A future graphical widget system is a sibling to `HudFieldType`, not an extension of its text-brick resolver model.
- `CbTheme` remains the shared red, black, and lime screen palette.
- HUD rendering must never depend on a missing-glyph font icon for the lock indicator.
- Any future ESC mixin identifies its target and injection point before it changes the vanilla game menu.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Incidents ping widget | Parked — owner wants it eventually, not urgent. | G03 |
| Vault-upload progress toast widget | Parked — owner wants it eventually, not urgent. | G03 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-15 | Persistent active-tool HUD chip | One hotbar announcement through G04 when the tool mode changes. |
| 2026-07-14 | Seven-widget catalog as an immediate build scope | Trimmed to two shipped widgets (macro banner, padlock) plus two parked QoL widgets; the rest is scrapped. |
| 2026-07-14 | Clickable widgets during normal play | Widgets remain display-only; commands and keybinds perform actions. |
| 2026-07-14 | Context-aware shortcut list | Removed as unnecessary and confusing. |
| 2026-07-18 | Per-widget op-force overriding a player's own toggle | Scrapped along with the global widget-visibility setting itself; no use case. |
| 2026-07-18 | Shareable HUD layout codes via the G20 vault system | Scrapped; layouts stay local per-player. |
| 2026-07-18 | Spinning 3D block-preview thumbnails on ESC buttons | Scrapped; the plain pause-menu buttons are final. |
| 2026-07-18 | Buzzer Game score/timer and favorites quick-bar widgets | Scrapped outright, not parked. |
| 2026-07-18 | Global widget-visibility server setting (op toggles a widget for the whole server) | Scrapped outright; no real use case. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_03_Done.md) · [All Groups](README.md)

- [G04 Communication](GROUP_04_Communication.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G16 Diagnostics](GROUP_16_DIAGNOSTICS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G31 BuzzerGame](GROUP_31_BUZZERGAME.md)
- [Pre-template Group 03 snapshot](../archive/group-migration-2026-07-18/GROUP_03_HUD_ESC.md)
