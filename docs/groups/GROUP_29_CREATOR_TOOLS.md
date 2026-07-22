# Group 29 - Creator and Capture Tools

> Group 29 helps a creator frame 9:16 shorts over Minecraft while keeping recording-mode guides out of the actual OBS footage.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_29.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Recording a horizontal Minecraft session for vertical shorts needs a reliable way to see crop, caption, and subject-safe regions while playing. A normal Minecraft HUD would be recorded, defeating the point. The recording guide therefore needs a client-native overlay that is visually separate from the game capture.

G29 owns native capture-safe behavior, recording-mode policy, local/pushed overlay state, layout persistence, commands, and keybinds. G27 owns the Record Overlay Studio Screen layout and editing controls; it does not own the Windows capture boundary or server push behavior.

## Ownership

| Owns | Does not own |
| --- | --- |
| Native transparent overlay window, capture exclusion, display-mode detection, and fail-closed recording behavior | General in-game HUD or cinematic presentation: G03/G27 |
| Overlay command/keybind behavior, local layouts, import/export, and active state | Record Overlay Studio Screen chrome: G27 |
| Server-pushed layout/enabled state and its authorization boundary | Native windows on another player's computer |
| 9:16 crop/caption/subject guide data | OBS, DaVinci Resolve, or external capture software configuration |

## Direction

In borderless/windowed play, recording mode uses a separate transparent, always-on-top, click-through Windows window positioned over Minecraft. The window applies Windows capture exclusion, so the creator sees the complete guide while OBS should not capture it. OBS output is the only success gate. If the capture-safe native window cannot attach, recording mode fails closed rather than showing a guide that might contaminate footage.

True exclusive fullscreen cannot reliably host the native overlay. It receives a deliberately reduced in-game fallback: outline markers only in the side regions that a fixed 9:16 center crop removes. The full guide is available by switching to borderless with the dedicated helper.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-30 | Recording-mode guides use a separate native window, not a Minecraft HUD. | Guides can be visible locally while kept out of OBS game/window capture. |
| 2026-06-30 | Windows `WDA_EXCLUDEFROMCAPTURE` is applied to the native overlay. | Capture-safe mode has an OS-level exclusion request in addition to separate-window isolation. |
| 2026-06-30 | Capture safety is fail-closed. | If the shield/native overlay cannot attach, recording mode stays disabled and explains the fix. |
| 2026-06-30 | Borderless gets the full overlay; true exclusive fullscreen gets crop-discard sidebar markers only. | Full captions/dimming never appear inside footage that will be kept. |
| 2026-06-30 | Built-in guide set includes 9:16 frame, caption margins/box, subject safe-band, and dimmed outside area. | Creators can work for fixed crop or Smart Reframe without guessing. |
| 2026-06-30 | F8 toggles overlay, F9 assists borderless/OBS mode, and F10 opens the Studio. | Recording tools remain available while Minecraft retains normal input. |
| 2026-06-30 | Local editing is the default; owner/admin push can force a shared layout for modded clients and can be reversed. | Server push sends layout data, while every client creates its own local native window. |
| 2026-07-12 | Record Overlay Studio is a G27 Screen. | G29 retains capture, data, layout, and push behavior. |

## Feature Plan

### A. OBS-Safe Shorts Overlay

**Player outcome**

A creator can frame a vertical short while playing and trust that the guide is not baked into the recorded clip.

**Experience**

- The overlay shows the 9:16 center frame, caption-safe margins, a hollow caption box, subject safe-band, and adjustable dimmed cut-off area.
- Fixed center crop can use the precise frame; Smart Reframe can use the subject-safe helper. Both are separately controllable.
- Recording mode remains quiet: no persistent Minecraft chat/action-bar messages that could appear in footage.
- Human errors explain a concrete next step, such as using F9/borderless, reopening the Studio, or restarting Minecraft.

**Requirements**

- Create a transparent always-on-top click-through native window aligned to the Minecraft window, then request `WDA_EXCLUDEFROMCAPTURE` through the Windows integration.
- Detect borderless/windowed versus true-exclusive fullscreen and select the full native overlay or reduced crop-safe fallback automatically.
- Close the native window before showing exclusive fullscreen fallback; never render full overlay features inside the kept 9:16 area on that path.
- Persist enabled state, dim opacity, caption box, safe guides, and last display mode in client configuration.
- Validate real OBS capture output as the acceptance evidence, not only a local screenshot/window check.

**Boundary**

G29 guides framing only. It does not capture video, edit DaVinci projects, modify OBS sources, or use a normal Minecraft HUD as a hidden recording overlay.

### B. Record Overlay Studio and Local Layouts

**Player outcome**

The creator can inspect status, adjust the guide, and save named recording layouts without editing configuration files.

**Experience**

- `/cb recordoverlay` and F10 open the Studio; `on`, `off`, `toggle`, `obs`, and `borderless` provide explicit controls.
- The Screen shows OBS-safe state, display mode, active layout, local versus pushed state, and the last actionable error/fix.
- Built-in guides can be adjusted and local layouts can save, load, list, delete, import, and export as JSON.
- Editor mode may show labels, handles, warnings, and controls; recording mode renders only chosen guides/marks.

**Requirements**

- Persist named local layouts in `config/customblocks/data/shorts-overlay-layouts.json` and remember the last active layout.
- Layout data includes built-in guide choices, opacity, colours, labels, snapping, crop helper preferences, recording visibility, and future custom marks.
- F8/F9/F10 remain editable through Minecraft controls; the Studio exposes their current bindings without owning global keybind registration elsewhere.
- G27 routes Screen edits to G29 layout/state services and never makes editor handles part of recording mode.

**Boundary**

The Studio's canvas/rail/inspector visual design belongs to G27. G29 owns only the data/actions it invokes and the capture-safe renderer that consumes them.

### C. Custom Marks and Shared Push

**Player outcome**

The creator can build a richer personal guide, and an authorized owner can temporarily give every modded player the same recording overlay.

**Experience**

- Custom rectangles, circles, lines, arrows, crosshairs, and freehand marks can be added, selected, moved, resized, styled, labelled, layered, locked, duplicated, and deleted.
- Magnetic snapping supports crop edges, center, safe margins, grid, and other marks; numeric controls provide precision.
- Recording mode renders only selected guide/mark content through the capture-safe native window.
- Push state is visibly distinct from local state. `/cb recordoverlay push off` restores clients to their own local setting.

**Requirements**

- Limit initial custom layouts to ten marks and normalize stored geometry for changing window sizes.
- The server validates owner/admin authorization before sending active layout/enabled payloads to modded clients.
- Each receiving client independently applies the layout to its own native overlay and reports capture-safe inability only to that client.
- Pushed state is saved server-side only after an explicit push; local layouts remain client-owned.

**Boundary**

The server distributes instructions, not native windows. It cannot guarantee another computer's Windows capture APIs, so local fail-closed behavior always wins.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G03 | HUD/keybind environment | Recording guides remain a separate native overlay and do not become a recorded HUD layer. |
| G16 | Diagnostics | Capture/window failures use concise local error details suitable for diagnosis without enabling unsafe recording. |
| G22 | Permissions | Push-to-everyone requires the approved owner/admin authorization route. |
| G27 | Record Overlay Studio | G27 owns Screen chrome and editable-canvas interaction; G29 owns layout state, capture renderer, and push actions. |

## Technical Contract

- Borderless/windowed recording mode renders through a transparent native click-through topmost window aligned with Minecraft and requests `WDA_EXCLUDEFROMCAPTURE`.
- Recording mode fails closed whenever the native overlay/capture shield cannot be applied; Studio editing remains available with a human reason.
- Exclusive fullscreen closes the native overlay and uses only fixed-center-crop-discard sidebar markers, never full guide content in the retained frame.
- Client layouts persist locally in `shorts-overlay-layouts.json`; pushed server state is explicit, reversible, and distributed as data to modded clients.
- Recording mode includes selected guides/marks only; editor-only handles, labels, controls, and warnings cannot render in the native recording overlay.
- OBS recording output is the authoritative verification surface for capture invisibility.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| More creator/capture tools | The shorts overlay needs reliable OBS evidence and complete custom-mark/push behavior first. | G29 |
| Capture support outside Windows | The current native exclusion contract is Windows-specific. | G29 platform work |
| Automatic OBS/DaVinci project setup | It reaches outside the mod's guidance and layout responsibility. | Separate external-tool integration |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-30 | A normal in-game HUD could hide a recording guide. | Recording guidance is a separate native capture-safe window. |
| 2026-06-30 | Full overlay could be shown in exclusive fullscreen. | Exclusive path uses only crop-discard sidebar markers; borderless unlocks full guidance. |
| 2026-07-12 | G29 could define its own Studio screen chrome. | G27 owns the Screen layout; G29 owns behavior and rendering boundary. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_29.md) · [All Groups](README.md)

- [G03 HUD and Escape](GROUP_03_HUD_ESC.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [ADR-015 Shorts Overlay Native Window](../adr/ADR-015-shorts-overlay-native-window.md)
- [Pre-template Group 29 snapshot](../archive/group-migration-2026-07-18/GROUP_29_CREATOR_TOOLS.md)
