# Group 29 - Creator / Capture Tools

## Status

| | |
| --- | --- |
| **Verdict** | Native overlay and first Studio slice are built, but OBS invisibility, custom marks, and multiplayer push remain open. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-06-30 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | OBS-safe native shorts overlay | Built 🎯 | Discussion ✏️ |
| B | Record Overlay Studio slice 1 | Built 🎯 | Discussion ✏️ |
| C | Custom marks, snapping, and recording renderer | Designed ⏳ | Discussion ✏️ |
| D | Push-to-everyone overlay sync | Designed ⏳ | Discussion ✏️ |
| E | G27 Studio chrome handoff | Designed ⏳ | Discussion ✏️ |
| F | Future creator tools | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_29_CREATOR_TOOLS.md](../groups/GROUP_29_CREATOR_TOOLS.md)

---

# Active Tests

## 💡 Setup

- Test on Windows with OBS Studio.
- Record a short OBS clip for every recording-mode row.
- Use borderless and exclusive fullscreen paths.
- Remember: the owner success gate is OBS invisibility, not just seeing the overlay locally.

## A - OBS-safe native shorts overlay - Built 🎯

| | |
| --- | --- |
| **Check** | The 9:16 guide overlay is visible to the player but absent from real OBS output. |
| **Pass rule** | F8, borderless, capture exclusion, OBS recording, F11 fallback, crop vanish, persistence, and failure-message rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but not owner-confirmed with OBS. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Press F8 in-game. | Shorts overlay toggles on/off. | 🎯 | ➖ |
| A2 | Press F9 or use borderless helper. | Minecraft switches to capture-friendly borderless mode. | 🎯 | ➖ |
| A3 | In borderless mode, enable overlay. | Full guide appears over Minecraft: 9:16 frame, caption area, subject safe-band, and dim cut-off area. | 🎯 | ➖ |
| A4 | Record with OBS game/window capture. | Overlay is absent from the recording while still visible to the owner. | 🎯 | ➖ |
| A5 | Use true exclusive fullscreen/F11. | Native overlay closes and the fallback draws only crop-discard sidebar markers. | 🎯 | ➖ |
| A6 | Crop recording to 9:16. | Exclusive fallback markers vanish from the final vertical crop. | 🎯 | ➖ |
| A7 | Restart Minecraft. | Enabled state and overlay settings persist. | 🎯 | ➖ |
| A8 | Force capture shield failure if possible. | Recording mode fails closed with a human-readable fix, not a captured overlay. | 🎯 | ➖ |

## B - Record Overlay Studio slice 1 - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb recordoverlay`, F10, built-in guide controls, and local layout management are wired. |
| **Pass rule** | Command open, toggles, OBS mode, borderless, F10, local layout save/load/list/delete/export/import, and status rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but not owner-confirmed in game. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Run `/cb recordoverlay`. | Record Overlay Studio opens. | 🎯 | 🎯 |
| B2 | Use `/cb recordoverlay on`, `off`, and `toggle`. | Overlay state changes clearly. | 🎯 | 🎯 |
| B3 | Use `/cb recordoverlay obs on/off/toggle`. | OBS-safe mode toggles and status updates. | 🎯 | 🎯 |
| B4 | Use `/cb recordoverlay borderless`. | Borderless helper runs from command. | 🎯 | 🎯 |
| B5 | Press F10. | Studio opens from keybind. | 🎯 | 🎯 |
| B6 | Adjust built-in guide controls in the Studio. | Preview and overlay settings update. | 🎯 | 🎯 |
| B7 | Save, load, list, delete, export, and import a layout. | Local JSON layout workflow works and survives restart. | 🎯 | 🎯 |
| B8 | Check Studio status panel. | It shows OBS-safe state, display mode, active layout, local/pushed state, and last error/fix. | 🎯 | 🎯 |

## C - Custom marks, snapping, and recording renderer - Designed ⏳

| | |
| --- | --- |
| **Check** | The advanced editor should support custom marks and render them through the capture-excluded native overlay. |
| **Pass rule** | Add, drag, resize, shape, label, color, opacity, layer, lock, duplicate, delete, snap, numeric edit, render, and OBS rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Custom marks and native recording renderer for all marks are pending. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Add rectangle, circle, line, arrow, crosshair, and freehand marks. | Each mark appears in editor and can be selected. | ⏳ | ⏳ |
| C2 | Drag/resize marks with mouse and numeric fields. | Position/size update accurately and save normalized. | ⏳ | ⏳ |
| C3 | Toggle snapping and drag near guides. | Snaps to center, crop edges, safe margins, grid, and other marks. | ⏳ | ⏳ |
| C4 | Edit color, opacity, stroke, fill, label, visibility, lock, and layer order. | Settings apply in editor and persist. | ⏳ | ⏳ |
| C5 | Enter recording mode. | Only chosen guide/mark lines render; editor handles do not render. | ⏳ | ⏳ |
| C6 | Record with OBS. | Custom marks are absent from OBS output when recording mode is OBS-safe. | ⏳ | ⏳ |

## D - Push-to-everyone overlay sync - Designed ⏳

| | |
| --- | --- |
| **Check** | Owner/admin can push the active layout and enabled state to every modded client. |
| **Pass rule** | Permission, push, client apply, client failure, pushed status, push off, local restore, and persistence rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Server payloads and client application are pending. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Non-admin tries `/cb recordoverlay push everyone`. | Command is denied cleanly. | ➖ | ⏳ |
| D2 | Owner/admin pushes current layout to everyone. | All modded clients receive layout and enable overlay locally. | ➖ | ⏳ |
| D3 | One client cannot apply OBS-safe mode. | That client receives local human-readable failure reason. | ➖ | ⏳ |
| D4 | Studio shows pushed state. | UI distinguishes local-only from pushed-to-everyone. | ➖ | ⏳ |
| D5 | Run `/cb recordoverlay push off`. | Forced overlay clears and clients return to local settings. | ➖ | ⏳ |

## E - G27 Studio chrome handoff - Designed ⏳

| | |
| --- | --- |
| **Check** | Record Overlay Studio screen chrome follows the G27 left-rail/canvas/inspector/bottom-strip design. |
| **Pass rule** | Layout, layers, canvas, inspector, bottom strip, guide toggles, handles, edit-only helpers, and red/black/lime styling rows pass in G27. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen chrome spec is owned in G27; G29 owns capture safety and data behavior. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Open Studio and inspect layout. | Left layers, center 16:9 canvas, right inspector, and bottom global strip match G27 spec. | ⏳ | ⏳ |
| E2 | Switch from editor to recording mode. | Editor handles disappear from recording mode. | ⏳ | ⏳ |
| E3 | Use import/export and save/load buttons. | They drive G29 layout storage without screen chrome drift. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §F Future creator tools — 💤 `2026-06-30`: Reserved for the next creator/capture tool idea after overlay work is stable.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove temporary overlay layouts after testing.
- [ ] Turn off pushed overlay state after MP tests.
- [ ] Keep OBS clips/evidence paths private if they contain personal footage.
- [ ] Keep screen chrome issues linked to G27.

</details>
