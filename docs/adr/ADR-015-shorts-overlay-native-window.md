# ADR-015 - Shorts overlay uses a native capture-excluded window

Date: 2026-06-30
Status: Accepted for Build A, pending owner in-game + OBS confirmation

## Context

G29-1 needs a 9:16 Shorts framing guide that the player can see while recording Minecraft, but OBS must not
capture. Screenshot and screen-share hiding can be a useful Windows capture-shield bonus, but the owner success
gate is OBS output. Anything drawn by Minecraft's HUD renderer becomes part of the game frame and can be
captured, so a normal in-game overlay cannot satisfy the core requirement.

The owner also records on Windows 11 and wants this as a client-only creator tool, with no server state and no
effect on other players.

## Decision

Build the full guide as a separate transparent native window over Minecraft in borderless/windowed mode.

Implementation details:

- Use a Swing `JWindow` for the visual layer.
- Keep it always-on-top, non-focusable, and click-through with Win32 extended styles.
- Call `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` through JNA/user32.dll.
- Track the Minecraft native window bounds and resize/reposition the overlay to match.
- Provide `F8` for overlay enable/disable and `F9` for the borderless helper.
- Persist local client settings under `config/customblocks/data/shorts-overlay-client.json`.
- In true/exclusive fullscreen, close the native overlay and draw only sidebar fallback markers that should be
  cropped away by a center 9:16 export.

## Consequences

Good:

- The full guide can be visible to the player while staying outside the captured Minecraft frame.
- `WDA_EXCLUDEFROMCAPTURE` gives an OS-level capture-shield layer; OBS is the required verification target.
- The feature remains client-only and does not touch server data.
- The fallback gives some framing signal in exclusive fullscreen without drawing inside the final 9:16 keep-zone.

Tradeoffs:

- The complete experience is Windows-only.
- OBS confirmation is mandatory; compile success does not prove capture exclusion.
- Exclusive fullscreen cannot support the full native overlay reliably, so that path is intentionally degraded.
- The overlay uses AWT/Swing plus JNA, so the dependencies are included in the mod jar.

## Verification

Owner test rows live in `docs/testing/TESTING_GUIDE_29.md` section A.

The feature is not complete until:

- `F8` toggles the full guide.
- `F9` enables the borderless path and keeps the guide aligned.
- OBS preview/recording does not show the guide.
- Screenshot hiding may be checked as a bonus, but it is not the owner pass/fail gate.
- `F11` exclusive fullscreen shows only sidebar fallback markers, and those disappear after a 9:16 center crop.
- Settings persist after restart.
