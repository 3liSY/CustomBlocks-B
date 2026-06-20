# Group 29 — Creator / Capture Tools

> **New feature group (not a legacy restoration).** Groups 01–28 restore/fix block
> features. Group 29 is a fresh container for **content-creation / recording aids** —
> tools that help the developer record and edit Minecraft footage, starting with a
> shorts-framing overlay. Features land here as `§29.N`.

---

## Objective

Give the developer on-screen tools that make recording vertical (9:16) shorts in OBS
reliable — without those tools ever appearing in the recording. The developer records
mainly Minecraft, exports 16:9 from OBS, and converts to vertical in DaVinci Resolve.

---

## Background

- Developer records gameplay in **OBS Studio**, full-screen Minecraft.
- Final product is **vertical shorts (9:16)**; the source is **16:9**.
- Problem: while recording there is no way to see where the 9:16 crop edges, the
  caption zone, or the safe area will land. Framing is guesswork.
- Hard constraint: any framing guide must be **visible to the developer but invisible
  to OBS** (and to screenshots / screen-share), so it never contaminates the footage.
- Developer is not a programmer (see root `CLAUDE.md §3`): keep it plain, keep it
  reliable, one feature verified in-game before piling on more.

---

## Feature Log

| # | Feature | Status |
|---|---------|--------|
| 29.1 | Shorts Framing Overlay — toggleable 9:16 guide, capture-invisible | Spec locked, pending build + in-game confirm |
| 29.2 | *(reserved for next creator-tool idea)* | — |

---

## §29.1 — Shorts Framing Overlay

A keybind-toggled overlay that draws the vertical-short framing guides on the
developer's screen while recording. It is rendered as a **separate desktop window**,
not inside Minecraft's frame, so OBS never captures it.

### Why a separate window (the core constraint)

Anything Minecraft draws into its own frame is part of the picture OBS game-captures —
a literal in-game HUD **cannot** be hidden from the recording. Therefore the guide is
drawn by a separate, transparent, always-on-top, click-through window that floats
exactly over the Minecraft window. Because OBS game-captures the *Minecraft* window,
the floating window is not part of that capture. As a second layer of safety, the
window is flagged so the OS excludes it from all screen capture and screenshots.

### Locked decisions

- **DECISION — Toggle:** A configurable mod keybind turns the overlay on/off mid-game.
  Code lives in the CustomBlocks-B mod (this group), so it "belongs" to the mod even
  though it renders as its own OS window.

- **DECISION — Capture invisibility:** The overlay window is flagged
  `WDA_EXCLUDEFROMCAPTURE` via `SetWindowDisplayAffinity` (user32.dll, called from Java
  through JNA). Effect: the developer's eyes see it; OBS, screenshots, and screen-share
  do not. Windows 11 supports this fully. The window is also click-through (mouse and
  keyboard pass through to Minecraft).

- **DECISION — Display-mode handling (auto-detect):** The mod reads whether Minecraft
  is running borderless or true-exclusive fullscreen and picks the render path
  automatically:
  - **Borderless windowed → full overlay** (floating window): 9:16 frame outline,
    caption-safe margins, caption box, subject safe-band, and the dim slider. This is
    the complete, intended experience.
  - **True fullscreen / F11 (exclusive) → degraded in-frame fallback:** a floating
    window cannot reliably draw over exclusive fullscreen, so the mod instead draws an
    **outline-only** marker inside the cropped-away sidebars of the 16:9 frame (the
    left/right regions that get discarded when cropping to 9:16). The developer sees
    it; it vanishes when the footage is cropped to 9:16. **No** dimming, caption box,
    or caption margins in this mode (those sit inside the kept 9:16 zone and would show
    in the short).

- **DECISION — Bundled borderless toggle:** Because borderless unlocks the full
  feature (and is better for OBS anyway — cleaner game-capture, instant alt-tab, no
  minimize-on-focus-loss), the mod ships a one-key **borderless fullscreen toggle** so
  the developer can flip Minecraft into borderless without digging through menus.

- **DECISION — Guide elements (full / borderless mode):**
  1. **9:16 center frame outline** — bright rectangle marking the vertical crop.
  2. **Caption-safe margins** — inner margins showing where platform UI / captions
     typically cover, so important content is not placed there.
  3. **Caption box** — a developer-positioned, resizable box marking where captions
     will sit. Rendered **hollow** (outline + a small "CAPTIONS — keep clear" tag) so
     the developer can see the gameplay behind it and judge whether the background is
     clean enough for readable captions. Position and size persist in config.
  4. **Subject safe-band** — a slightly wider "keep your subject inside here" band.
     This makes the overlay useful whether DaVinci uses a fixed center crop **or**
     Smart Reframe (auto-tracking, where the crop moves). See open question below.
  5. **Dim cut-off area** — the region outside the 9:16 frame is darkened so the
     keep-zone stands out. **Adjustable opacity slider** in config (semi-transparent by
     intent, so the developer can still see action happening outside the frame).

- **DECISION — Persistence:** All overlay settings (keybinds, dim opacity, caption-box
  position/size, last display mode) save to config and restore on next launch.

### Open question — needs developer check (DaVinci)

How DaVinci converts 16:9 → 9:16 changes how exact the guide can be:

- **Smart Reframe (auto-tracking):** the crop box moves to follow the action, so a
  static on-screen guide cannot be pixel-exact. The **subject safe-band** is the right
  aid here ("keep the subject in this box").
- **Fixed center crop:** the crop is always the middle 9:16 strip, so the frame outline
  is pixel-exact.

**How to check (developer):** open a finished short in DaVinci, play it, and watch the
framing. If the view pans/follows the action → Smart Reframe. If it stays locked dead
center → fixed crop. Menu confirm: select the clip → Inspector (top-right) → look for a
**Smart Reframe** block with an *Analyze* button.

The build is designed to work either way (frame outline + safe-band both present); once
the developer confirms which DaVinci uses, the non-applicable element can be hidden.

### Technical approach (implementation notes, not yet built)

- New client-only subsystem (e.g. `client/capture/`), kept off the server entirely.
- Separate AWT/Swing or LWJGL transparent window, always-on-top, click-through
  (`WS_EX_LAYERED | WS_EX_TRANSPARENT`), capture-excluded via JNA → user32.dll.
- Auto-detect borderless vs exclusive via the GLFW window state Minecraft already holds.
- Respect the root architecture rules (`CLAUDE.md §5`): client never mutates server
  state; every new class gets a header comment; file-size gates apply; an ADR is added
  for the native-window / capture-exclusion approach (non-obvious decision).

### Success criteria (per root `CLAUDE.md §2` — the Golden Rule)

Nothing here is ✅ DONE until the developer confirms in-game. Specifically:

- Overlay toggles on/off with the keybind.
- In borderless: 9:16 outline, caption margins, caption box, safe-band, and dim slider
  all render correctly over Minecraft.
- The overlay is **confirmed absent** from an actual OBS recording and from a screenshot.
- Borderless toggle keybind flips Minecraft into borderless fullscreen.
- In F11 exclusive: the sidebar outline fallback shows and disappears on 9:16 crop.
- Settings persist across a restart.

---

## Status

**Spec locked for §29.1 (2026-06-20). Not built.** Awaiting developer go-ahead to
implement, plus the DaVinci check above. Group 29 reserved for further creator/capture
tools as `§29.2+`.
