# Group 29 — Creator / Capture Tools

**🎯 Active:** A (0/12) · B1 (0/32)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                                 |
| --------------- | ----------------------------------------------- |
| **Verdict**     | 🎯 test-now (built/pending owner confirm in OBS) |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/44)        |
| **Last tested** | —                                               |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ Screen content moved 2026-07-12: RecordOverlayStudioScreen chrome (layers rail/canvas/inspector,
> custom-mark controls, snapping, built-in-guide list) → `GROUP_27_SCREENS.md` §G27.25. G29 keeps
> OBS-capture, push-to-everyone, and layout-storage backend. B6–B13 test rows exercise the screen.

## 🗺️ Sections

| §   | What                                                                             | State      |
| --- | -------------------------------------------------------------------------------- | ---------- |
| A   | Shorts framing overlay — capture-invisible 9:16 recording guide                  | 🎯 test-now |
| B1  | `/cb recordoverlay` slice 1 — Studio shell, built-in guide editor, local layouts | 🎯 test-now |
| B2+ | Advanced custom marks + push-to-everyone                                         | ⏳ planned  |

🔗 **Related Doc:** [GROUP_29_CREATOR_TOOLS.md](../groups/GROUP_29_CREATOR_TOOLS.md)

---

# 🎯 Test now

### 🧰 Setup Required
* Windows 11, Minecraft running in windowed or borderless mode (fullscreen/exclusive needed for A5)
* OBS (or equivalent capture/preview software) open and previewing or recording, for the capture-invisibility checks (A4, B14)
* Owner/admin permission on the account for the push-to-everyone checks (B15); a non-permitted account for B16

> **A · Shorts framing overlay**
| # | Action                                                                         | Expected Result                                                                    | SP | MP |
| --- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- | --- | --- |
| A1 | Start Minecraft on Windows 11 in windowed or borderless mode, then press `F8`. | Transparent Shorts guide appears over Minecraft. Pressing `F8` hides it.           | 🎯 | 🎯 |
| A2 | With the overlay visible, click and play normally through it.                  | Mouse and keyboard still control Minecraft; no focus stolen.                       | 🎯 | 🎯 |
| A3 | Press `F9` from normal windowed/fullscreen setup.                              | Switches to borderless capture-friendly path; full guide stays aligned.            | 🎯 | 🎯 |
| A4 | Record or preview in OBS while the `F8` overlay is visible.                    | Overlay visible to you but **absent** from the OBS output.                         | 🎯 | 🎯 |
| A5 | Press Minecraft `F11` into true fullscreen/exclusive, enable overlay.          | Full native overlay is gone; only simple in-frame sidebar fallback markers appear. | 🎯 | 🎯 |
| A6 | Restart Minecraft after leaving the overlay enabled or disabled.               | `shorts-overlay-client.json` persists enabled state and guide settings.            | 🎯 | 🎯 |

> **B slice 1 · Record Overlay Studio** (`/cb recordoverlay`)
| #   | Action                                                                                        | Expected Result                                             | SP | MP |
| --- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------- | --- | --- |
| B1  | Run `/cb recordoverlay`.                                                                      | Record Overlay Studio opens.                                | 🎯 | 🎯 |
| B2  | Press `F10`.                                                                                  | Record Overlay Studio opens.                                | 🎯 | 🎯 |
| B3  | Run `/cb recordoverlay on`, `off`, `toggle`.                                                  | Active overlay state changes exactly as requested.          | 🎯 | 🎯 |
| B4  | Run `/cb recordoverlay obs on`, `off`, `toggle`.                                              | OBS mode changes state.                                     | 🎯 | 🎯 |
| B5  | Run `/cb recordoverlay borderless`.                                                           | Enters/leaves capture-friendly borderless path.             | 🎯 | 🎯 |
| B6  | In Studio, click left-rail layers: `9:16 Crop`, `Safe Area`, etc.                             | Click selects; click again toggles guide on/off in preview. | 🎯 | 🎯 |
| B7  | Select `Safe Area`, move side/top/bottom sliders.                                             | Preview updates immediately; native overlay matches.        | 🎯 | 🎯 |
| B8  | Select `Caption Zone`, move X/Y/W/H sliders.                                                  | Caption box updates and persists.                           | 🎯 | 🎯 |
| B9  | Select `Subject Box`, move W/H sliders.                                                       | Subject guide updates and persists.                         | 🎯 | 🎯 |
| B10 | Select `Dim Sidebars`, move opacity.                                                          | Side dim opacity changes.                                   | 🎯 | 🎯 |
| B11 | Press `Save`, close Studio, restart Minecraft, reopen Studio.                                 | `shorts-overlay-client.json` keeps toggles.                 | 🎯 | 🎯 |
| B12 | `/cb recordoverlay layout save test1`, change a guide, `/cb recordoverlay layout load test1`. | Saved layout returns.                                       | 🎯 | 🎯 |
| B13 | `/cb recordoverlay layout list`, `export`, `delete`, `import`.                                | Commands function as expected.                              | 🎯 | 🎯 |
| B14 | While OBS preview/recording is active, close Studio and enable overlay/OBS mode.              | Guides visible to player but absent from OBS output.        | 🎯 | 🎯 |
| B15 | Run `/cb recordoverlay push everyone` as OP/admin.                                            | Command reports placeholder (not built yet).                | 🎯 | 🎯 |
| B16 | Try `/cb recordoverlay push everyone` without permission.                                     | Denied by permission gating.                                | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

## B0 — Owner design lock and slice status

| ID    | Decision                                                   | Status in this build                 |
| ----- | ---------------------------------------------------------- | ------------------------------------ |
| B0.1  | Main command: `/cb recordoverlay`                          | ✅ built                              |
| B0.2  | Main screen: Record Overlay Studio GUI                     | 🟡 slice 1 built                      |
| B0.3  | Main promise: guides visible to player, hidden from OBS    | 🟡 pending OBS test                   |
| B0.4  | Lego-style drag/resize plus numeric controls               | 🟡 sliders built; drag/resize pending |
| B0.5  | Max custom marks: 10 per layout                            | ⏳ pending                            |
| B0.6  | Owner-created layouts only                                 | 🟡 local built                        |
| B0.7  | Owner/admin push active layout to everyone                 | ⏳ pending                            |
| B0.8  | Each client creates its own local capture-excluded overlay | 🟡 built                              |
| B0.9  | Dim outside crop owner-controlled                          | ✅ built                              |
| B0.10 | Fixed vs moving crop                                       | ⏳ fixed only                         |

## ⏳ Future Build B slices

| Future ID | Area                   | Not built yet                                              |
| --------- | ---------------------- | ---------------------------------------------------------- |
| BF1       | Custom marks           | box, circle/ellipse, line, arrow, crosshair, freehand      |
| BF2       | Custom mark editing    | drag/resize handles, X/Y/W/H, labels, color, opacity, etc. |
| BF3       | Max-count rule         | 10 custom marks per layout                                 |
| BF4       | Snapping               | crop edges, center lines, safe margins                     |
| BF5       | Extra guides           | thirds, center lines, moving/Smart-Reframe helper          |
| BF6       | Studio keybind editing | inside Studio                                              |
| BF7       | Push-to-everyone       | real owner sync                                            |
| BF8       | Push failure handling  | per-client local failure messages                          |

## Human error wording examples

| Situation              | Good message                                                                                                                             |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Exclusive fullscreen   | "Record Overlay cannot use the OBS-safe window in exclusive fullscreen. Press F9 for borderless, then try again."                        |
| Windows capture shield | "Windows capture shield did not attach. Reopen Record Overlay Studio or restart Minecraft, then test OBS again."                         |
| Non-owner tries push   | "Only the owner/admin can push Record Overlay to everyone."                                                                              |
| Max marks reached      | "This layout already has 10 marks. Delete or duplicate less before adding another."                                                      |
| Push local failure     | "Record Overlay layout received, but OBS-safe mode is unavailable on this client. The editor still works; recording overlay stayed off." |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(No sections fully passed yet)*

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

