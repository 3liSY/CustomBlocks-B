# Group 29 — Creator / Capture Tools

> **UI medium audit (2026-07-09):** RecordOverlayStudioScreen confirmed right medium (Screen), flagged
> for a design/UX polish pass.

> **Revived 2026-06-25.** Previously a retired pointer to ISSUES.md G29-1.
> This doc now owns the Shorts Framing Overlay spec — G29-1 folded here by owner decision.
## G29-1 · Shorts Framing Overlay — capture-invisible 9:16 recording guide (Group 29, NEW)

> 🟡 Build A implemented 2026-06-30 — client-only Windows subsystem (`WDA_EXCLUDEFROMCAPTURE`); awaiting owner in-game + OBS confirmation
> 🟢 Build B slice 1 implemented 2026-06-30 — `/cb recordoverlay`, F10 Studio, built-in guide editor, and local layout commands; custom marks + multiplayer push still pending

> *"completely merge the group into the issues.md"* (owner, 2026-06-24) — Group 29 is now folded **in full**
> into this entry. The old `GROUP_29_CREATOR_TOOLS.md` is **retired to a one-line pointer**; THIS is the home.

**✅ FULL RECORD (not a stub).** Group 29 held only this one spec, so it was merged here in its entirety and
the standalone Group doc retired. This is the **deliberate exception** to the Folding Protocol above (which
normally pushes detail out to a Group doc): for G29-1 the detail lives here. *(Group 29 = "Creator/Capture
Tools," a container for recording aids; if more such tools appear later they can be added as further FX rows.)*

### Objective

Give the developer on-screen tools that make recording vertical (9:16) shorts in OBS reliable — **without those
tools ever appearing in the recording.** The developer records mainly Minecraft, exports 16:9 from OBS, and
converts to vertical in DaVinci Resolve.

### Background

- Developer records gameplay in **OBS Studio**, full-screen Minecraft.
- Final product is **vertical shorts (9:16)**; the source is **16:9**.
- Problem: while recording there's no way to see where the 9:16 crop edges, the caption zone, or the safe area
  will land. Framing is guesswork.
- Hard constraint: any recording-mode guide must be **visible to the developer but invisible to OBS**, so it
  never contaminates the footage. Screenshot/screen-share hiding is a Windows capture-shield bonus, not the
  owner success gate for Build B.
- Developer is not a programmer (`CLAUDE.md §3`): keep it plain, keep it reliable, one feature verified in-game
  before piling on more.

**Want** — An on-screen guide showing where the vertical-short (9:16) crop edges, caption zone, and safe area
land while recording in OBS — **visible to the developer, invisible to the recording.** Drawn as a separate,
always-on-top, click-through OS window over Minecraft, flagged `WDA_EXCLUDEFROMCAPTURE` so OBS should not
capture it. (Anything Minecraft draws in its own frame is part of what OBS records, so an in-game HUD can't be
hidden — hence a separate window.)

**Status: Build A implemented 2026-06-30, not owner-confirmed yet.** The locked design is now wired as a
client-only Windows overlay path plus an exclusive-fullscreen fallback. It still needs the in-game and OBS
recording check below before it can be marked done.

### Why a separate window (the core constraint)

Anything Minecraft draws into its own frame is part of the picture OBS game-captures — a literal in-game HUD
**cannot** be hidden from the recording. Therefore the guide is drawn by a separate, transparent,
always-on-top, click-through window that floats exactly over the Minecraft window. Because OBS game-captures
the *Minecraft* window, the floating window is not part of that capture. As a second layer of safety, the
window is flagged so the OS excludes it from capture paths that respect the Windows capture shield. Build B's
required pass/fail check is OBS output.

### Feature log (Group 29 container)

| # | Feature | Status |
|---|---------|--------|
| 29.1 | Shorts Framing Overlay — toggleable 9:16 guide, capture-invisible | Build A implemented, pending in-game + OBS confirm |
| 29.1-B | Record Overlay Studio — advanced draggable/custom shape editor + owner/admin push-to-everyone | Build B slice 1 implemented; custom marks + push pending |
| 29.2 | *(reserved for next creator-tool idea)* | — |

### Build B design lock — Record Overlay Studio (owner interview 2026-06-30)

**Status:** Build B slice 1 implemented 2026-06-30. `/cb recordoverlay`, F10 Studio, built-in guide editing,
OBS/borderless controls, and local layout save/load/export/import are wired. The full draggable custom mark
editor and owner/admin push-to-everyone sync remain pending.

#### Build B slice 1 implementation notes (2026-06-30)

- Added `RECORD_OVERLAY` GUI/action mode over the existing `OpenGuiPayload` path so the server command can trigger
  client-local overlay actions without storing recording preferences on the server.
- Added `/cb recordoverlay`, `studio`, `on|off|toggle`, `obs on|off|toggle`, `borderless`, and local
  `layout save|load|delete|list|export|import` commands.
- Added F10 keybind (`Open Record Overlay Studio`) alongside F8 overlay and F9 borderless.
- Added the first `RecordOverlayStudioScreen`: left layer rail, center 16:9 preview, right inspector sliders for
  built-in crop/safe/caption/subject/dim guides, bottom overlay/OBS/borderless/save/reset controls.
- Added local named layout persistence in `config/customblocks/data/shorts-overlay-layouts.json`.
- Registered `/cb recordoverlay push everyone|off` as an OP-only placeholder that reports the multiplayer sync slice
  is still pending.

#### Command and entry points

- **Primary command:** `/cb recordoverlay`
- `/cb recordoverlay` opens the Studio screen.
- `/cb recordoverlay on|off|toggle` controls the active overlay.
- `/cb recordoverlay studio` also opens the Studio screen.
- `/cb recordoverlay obs on|off|toggle` controls OBS-safe recording mode.
- `/cb recordoverlay borderless` toggles the capture-friendly borderless path.
- `/cb recordoverlay layout save <name>`, `load <name>`, `delete <name>`, `list`, `export <name>`,
  and `import` manage the owner's custom layouts.
- `/cb recordoverlay push everyone` pushes the current active layout and enabled state to every modded client
  on the server. This is an owner/admin action; other players do not need to accept it first.
- `/cb recordoverlay push off` clears the forced/shared overlay state and returns clients to their own local
  setting.
- No short global aliases are locked yet. Keep the command explicit and polish names later after in-game feel.

#### Keybinds

- `F8` — overlay on/off quick toggle.
- `F9` — OBS/borderless recording-mode helper.
- `F10` — open Record Overlay Studio.
- All keybinds must remain editable through Minecraft Controls. Build B should also show/edit the current
  bindings inside the Studio so the owner does not have to dig through menus.
- A "hide everything now" action is allowed as an advanced keybind/button, but the first visible controls stay
  simple: overlay, OBS mode, Studio.

#### Studio screen shape, custom marks, snapping, built-in guides — moved to G27

The Studio editor's screen chrome (left-rail layers, center canvas, right inspector, bottom strip; custom-mark
controls; snapping/precision behavior; the built-in-guide toggle list) is fully specced in
`GROUP_27_SCREENS.md` §G27.25 (2026-07-12, screen-content consolidation). This doc keeps the OBS-safety,
push-to-everyone, and layout-storage backend below, which aren't screen chrome.

#### Recording mode vs editor mode

- **Editor mode** is a normal Minecraft GUI. It can show handles, labels, action buttons, warnings, and helper UI.
  It is not meant to be recorded.
- **Recording mode** must draw only the owner's chosen guide lines/marks in the native capture-excluded overlay.
  These lines appear to the player but should not appear in OBS.
- Recording mode should avoid actionbar/chat spam. If a visible Minecraft HUD/actionbar is used, OBS can capture
  it. Normal toggle confirmations should be quiet or extremely minimal.
- If a human-readable error is needed, show it in the Studio/status panel first; only use chat/actionbar when the
  owner explicitly runs a command or when the overlay cannot safely start.

#### OBS capture safety

- The Build B promise is **OBS invisibility**. Screenshot hiding can remain a diagnostic bonus from Windows, but
  owner success is measured by OBS output.
- Recording mode must use the native capture-excluded window path whenever the overlay is supposed to be hidden
  from OBS. Do not implement the main recording overlay as a Minecraft HUD layer.
- If Windows capture protection cannot be applied, fail closed for recording mode: do not silently show a
  record overlay that OBS may capture.
- Error messages must be human and actionable, for example:
  - "Record Overlay cannot use the OBS-safe window yet. Press F9 to switch to borderless, then try again."
  - "Windows capture shield did not attach. Restart Minecraft or reopen Record Overlay Studio."
  - "Exclusive fullscreen blocks the full overlay. Use F9/borderless for OBS-safe recording mode."
  - "This feature needs the Windows capture shield on this computer. The editor still works, but recording mode is disabled."
- The Studio should include a clear status readout: `OBS-safe: on/off`, current display mode, active layout, pushed
  or local, and last error/fix.

#### Multiplayer and push-to-everyone

- Local editing remains the default: the owner edits their own layouts first.
- Owner/admin can choose **Push to everyone**. When pushed, the active layout and enabled state are sent to all
  modded clients and their overlay turns on without each player accepting it first.
- The server cannot draw one capture-excluded OS window for everyone. Each modded client must receive the pushed
  layout and create its own local native overlay window.
- Pushed overlay state should be explicit in the Studio: "local only" vs "pushed to everyone."
- Pushing requires permission/op. Non-owner players should not be able to force overlays onto everyone.
- A pushed overlay should be reversible: `/cb recordoverlay push off` and a Studio button must remove it.
- If a client cannot apply OBS-safe mode, that client receives the human-readable failure reason locally.

#### Layout storage

- Owner-created layouts only; no forced canned presets for Build B.
- Remember last active layout automatically.
- Save local layouts in client config.
- Server-pushed layout state saves server-side only when explicitly pushed.
- Import/export layouts as JSON so layouts can be backed up or shared later.
- Layouts should include custom marks, built-in guide toggles, opacity, colors, labels, snapping preferences,
  fixed/moving helper settings, and recording-mode visibility flags.

#### Build B slice order

1. **B1 — Data model + commands:** `/cb recordoverlay`, layout JSON, local/global state, command parsing, status
   messages, permission model for push.
2. **B2 — Studio editor shell:** full screen with layers, canvas, inspector, global toggles, save/load.
3. **B3 — Drag/resize/snapping:** Lego-style editing, magnetic guides, numeric controls, max 10 custom marks.
4. **B4 — Native recording renderer:** render all selected built-in guides and custom marks into the capture-safe
   native overlay; editor handles never render in recording mode.
5. **B5 — Push-to-everyone:** server payloads for active pushed layout + enabled state; local client native
   overlay activation; permissions and push-off.
6. **B6 — Diagnostics/polish:** OBS-safe status, human error/fix messages, quiet recording mode, import/export,
   owner in-game checklist.

### Locked decisions (full)

- **DECISION — Toggle:** A configurable mod keybind turns the overlay on/off mid-game. Code lives in
  CustomBlocks-B (this item), so it "belongs" to the mod even though it renders as its own OS window.
- **DECISION — Capture invisibility:** The overlay window is flagged `WDA_EXCLUDEFROMCAPTURE` via
  `SetWindowDisplayAffinity` (user32.dll, called from Java through JNA). Effect: the developer's eyes see it;
  OBS should not. Screenshot/screen-share hiding may also happen on Windows, but the owner gate is OBS. The
  window is also click-through (mouse and keyboard pass through to Minecraft).
- **DECISION — Display-mode handling (auto-detect):** The mod reads whether Minecraft is running borderless or
  true-exclusive fullscreen and picks the render path automatically:
  - **Borderless windowed → full overlay** (floating window): 9:16 frame outline, caption-safe margins,
    caption box, subject safe-band, and the dim slider. This is the complete, intended experience.
  - **True fullscreen / F11 (exclusive) → degraded in-frame fallback:** a floating window cannot reliably draw
    over exclusive fullscreen, so the mod instead draws an **outline-only** marker inside the cropped-away
    sidebars of the 16:9 frame (the left/right regions discarded when cropping to 9:16). The developer sees it;
    it vanishes when the footage is cropped to 9:16. **No** dimming, caption box, or caption margins in this
    mode (those sit inside the kept 9:16 zone and would show in the short).
- **DECISION — Bundled borderless toggle:** Because borderless unlocks the full feature (and is better for OBS
  anyway — cleaner game-capture, instant alt-tab, no minimize-on-focus-loss), the mod ships a one-key
  **borderless fullscreen toggle** so the developer can flip Minecraft into borderless without digging menus.
- **DECISION — Guide elements (full / borderless mode):**
  1. **9:16 center frame outline** — bright rectangle marking the vertical crop.
  2. **Caption-safe margins** — inner margins showing where platform UI / captions typically cover, so
     important content is not placed there.
  3. **Caption box** — a developer-positioned, resizable box marking where captions will sit. Rendered
     **hollow** (outline + a small "CAPTIONS — keep clear" tag) so the developer can see the gameplay behind it
     and judge whether the background is clean enough for readable captions. Position/size persist in config.
  4. **Subject safe-band** — a slightly wider "keep your subject inside here" band. Makes the overlay useful
     whether DaVinci uses a fixed center crop **or** Smart Reframe (auto-tracking, where the crop moves). See
     open question below.
  5. **Dim cut-off area** — the region outside the 9:16 frame is darkened so the keep-zone stands out.
     **Adjustable opacity slider** in config (semi-transparent by intent, so the developer can still see action
     happening outside the frame).
- **DECISION — Persistence:** All overlay settings (keybinds, dim opacity, caption-box position/size, last
  display mode) save to config and restore on next launch.

### Fixed crop vs moving crop — resolved for Build B

How DaVinci converts 16:9 → 9:16 changes how exact the guide can be:

- **Smart Reframe (auto-tracking):** the crop box moves to follow the action, so a static on-screen guide
  cannot be pixel-exact. The **subject safe-band** is the right aid here ("keep the subject in this box").
- **Fixed center crop:** the crop is always the middle 9:16 strip, so the **frame outline** is pixel-exact.

**Build B decision:** support both. Fixed center crop, moving/Smart-Reframe helper, and subject-safe helper are
all toggleable in Studio, by keybind/command where useful, and in saved layouts. The owner can turn either one
on or off without needing to know the editing-app term first.

### Build A implementation (2026-06-30)

- New client-only subsystem: `src/main/java/com/customblocks/client/capture/`.
- `F8` toggles the Shorts overlay on/off in-game (`key.customblocks.toggle_shorts_overlay`).
- `F9` toggles the Windows borderless helper (`key.customblocks.toggle_shorts_borderless`) so the full native
  overlay can sit above Minecraft without using exclusive fullscreen.
- Borderless/windowed path uses a separate transparent Swing `JWindow`, click-through and always-on-top, with
  `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` applied through JNA/user32.dll.
- Exclusive fullscreen path closes the native overlay and draws only crop-discard sidebar markers inside
  Minecraft. Those are visible while playing but should vanish from the final 9:16 center crop.
- Settings persist in `config/customblocks/data/shorts-overlay-client.json`: enabled state, dim opacity,
  caption-box position/size, safe margins, subject safe-band, and last mode.
- ADR: `docs/adr/ADR-015-shorts-overlay-native-window.md`.

| Group | Scope | Status |
|---|---|---|
| G29 — Creator/Capture Tools | **Client-only** (Windows 11 capture-exclusion); off the server entirely | 🟡 Build A implemented 2026-06-30; awaiting in-game + OBS confirm |
| G29-1 Build B | **Client + optional server push state** (`/cb recordoverlay`, Studio editor, pushed layouts) | 📝 design locked 2026-06-30; NOT built |

**Related:** **G27-1** (the IN-frame "cinematic HUD preset" — the *opposite* approach: shown IN the recording,
vs this one hidden FROM it; both kept, see Register row 22) · `CLAUDE.md §2` (Golden Rule) / §5 (client never
mutates server)
**Touches:** `client/capture/` subsystem · JNA → `user32.dll` (`SetWindowDisplayAffinity`) · GLFW window-state
read (borderless vs exclusive) · client keybinds · config persistence · new `/cb recordoverlay` command ·
Record Overlay Studio screen · layout JSON import/export · optional server payloads for push-to-everyone ·
permissions for owner/admin push · ADR-015 native-window / capture-exclusion approach
**Verify in-game (Build A):** overlay toggles with the keybind; borderless shows all guides over Minecraft;
**confirmed ABSENT from a real OBS recording**; borderless toggle flips Minecraft to borderless; F11-exclusive
sidebar fallback shows + vanishes on a 9:16 crop; settings persist across restart.
**Verify after Build B:** `/cb recordoverlay` opens Studio; owner creates/edits up to 10 custom marks; snapping,
opacity, shape, labels, fixed/moving helpers, recording mode, layout save/load/import/export, and push-to-everyone
all pass the testing guide; OBS still does not capture recording-mode marks.

---
