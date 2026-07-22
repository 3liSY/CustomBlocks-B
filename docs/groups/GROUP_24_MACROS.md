# Group 24 - Macros

> Group 24 lets operators record and replay repeatable CustomBlocks actions as a simple saved macro, with one understandable undo for the whole run.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_24.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Macros are a lightweight shortcut for repeating known CustomBlocks work. They should be simple to record, clear to review, safe to undo as one intended action, and easy to switch off when an operator needs to stop automation.

G24 owns macro data and execution. G27 owns the MacroList Screen so macros fit the unified interface. It does not grow into a separate programming or scripting system.

## Ownership

| Owns | Does not own |
| --- | --- |
| Macro record, add-step, stop, play, list, delete, persistence, and global toggle | MacroList Screen visual layout: G27 |
| Readable stored step labels and macro-run undo batching | Individual command behavior within each recorded step |
| Disabled-state messaging and toggle semantics | G03 HUD on/off commands |
| Plain macros only | Conditions, loops, scripts, script GUI, and `run` commands |

## Direction

A macro remains a named ordered list of normal CustomBlocks actions. An operator can record or add steps, stop, inspect the saved macro, play it, and remove it. Each macro run is treated as one user intention in history rather than a confusing chain of separate undo entries.

`/cb macro on` and `/cb macro off` are the only global controls. The old bare on/off route is reserved for HUD behavior. The macro interface remains a Screen managed by G27, while the server retains authority over stored macro data and execution.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | Macros remain plain recorded/addable command sequences. | Conditions, loops, scripts, `scriptgui`, and `run` are not built. |
| 2026-06-21 | Macro controls are `/cb macro on` and `/cb macro off`. | G03 HUD controls keep bare `/cb on` and `/cb off` without a command collision. |
| 2026-06-21 | Playing one macro creates one undo entry. | One `/cb undo` reverses the intended macro batch. |
| 2026-07-10 | Macro management stays Screen-based. | G27 owns `MacroListScreen`; no chest conversion is introduced. |

## Feature Plan

### A. Macro Lifecycle

**Player outcome**

An operator can save a repeatable sequence and understand exactly which macro is being run or changed.

**Experience**

- `/cb macro record <name>`, `add`, `stop`, `play`, `list`, and `delete` cover the full lifecycle.
- Each stored step has a readable label generated from its command, not an unexplained raw internal record.
- Missing macro names and invalid steps fail clearly before unexpected world changes occur.
- Saved macros survive the expected server persistence cycle.

**Requirements**

- Store named macros as ordered command/action records with stable readable summaries.
- Validate a macro name and each step before it becomes executable.
- Preserve ordering exactly during replay and report the step that cannot run.
- Keep deletion deliberate and remove the saved macro from subsequent list/play results.

**Boundary**

Macros coordinate existing commands. They do not introduce an alternate command language, conditional logic, loops, or arbitrary code execution.

### B. Toggle and Safe Batch History

**Player outcome**

An operator can pause macro activity globally and undo a completed macro in one clear step.

**Experience**

- While disabled, recording and playback follow one obvious disabled rule and playback says `Macro system is disabled. Use /cb macro on to re-enable.`
- Re-enabling restores ordinary macro behavior without changing recorded content.
- A successful macro run appears as one history item that names the macro and its step count.

**Requirements**

- The global toggle is persisted/available consistently across recording and playback paths.
- Macro execution records one G17-compatible undo batch, with matching redo behavior.
- A failed step reports its cause and must not corrupt prior history or create a misleading successful batch.
- Bare `/cb on` and `/cb off` never mutate macro state.

**Boundary**

Macro history is a consumer of the shared undo system. G17 owns the underlying multi-undo behavior and history policy.

### C. G27 MacroList Screen Handoff

**Player outcome**

The macro list feels like the rest of CustomBlocks while exposing the same server-owned macro actions.

**Experience**

- `/cb gui macros` opens the G27 MacroList Screen.
- A macro row can expose Play, Edit, Delete, and Info, with a clear Record New route and empty state.
- Screen actions produce exactly the same saved data and validation outcomes as macro commands.

**Requirements**

- G27 routes every Screen action to G24 services; it does not duplicate macro persistence or execute client-trusted steps.
- Macro list/detail data includes the name, step count, readable labels, and availability needed by the Screen.
- Screen deletion/recording remains subject to the same permission and disabled-state checks as commands.

**Boundary**

G24 does not define Screen chrome or navigation standards. Those remain G27 responsibilities.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G03 | HUD controls | Macro on/off remains namespaced under `/cb macro` and cannot collide with HUD controls. |
| G16 | Diagnostics | A failed macro step reports a readable command/step cause without noisy server errors. |
| G17 | Undo/redo | One successful macro run is represented by one shared history batch. |
| G22 | Permissions | Macro commands and Screen actions follow their final permission tier. |
| G27 | MacroList Screen | G27 owns visual UI; G24 owns every stored action and validation rule. |

## Technical Contract

- A macro is a persisted named ordered sequence with generated readable step labels.
- Playback evaluates server-side commands/actions in order and uses one shared undo/redo batch per successful run.
- Macro enablement is checked before recording or playback and never shares the bare HUD toggle route.
- G27 receives macro metadata and invokes G24 server services; client UI does not own macro execution or persistence.
- Script, `scriptgui`, and `run` command registrations remain absent by design.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Conditional or looping scripts | The approved product is intentionally a simple macro recorder/player. | Separate automation design |
| Arbitrary command/script execution | It needs a new safety and permission model and is not a macro extension. | Separate automation design |
| Macro sharing/import | It needs a safe transport and permission contract beyond local macro usefulness. | G24 with G20 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Bare `/cb on` and `/cb off` could toggle macros. | Macro controls are namespaced to avoid the HUD collision. |
| 2026-06-21 | Script GUI, script steps, and run commands could extend macros. | Those features are scrapped; plain macros are the complete scope. |
| 2026-07-10 | Macro management could become a chest interface. | It remains a G27 Screen. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_24.md) · [All Groups](README.md)

- [G03 HUD and Escape](GROUP_03_HUD_ESC.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G17 History, Give, Delete, and Search](GROUP_17_REGRESSIONS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 24 snapshot](../archive/group-migration-2026-07-18/GROUP_24_MACROS.md)
