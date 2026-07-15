# Group 03 — HUD System & ESC Integration

> **Prerequisite:** Group 27 HUD Builder foundation.
>
> **Objective:** Expand the multi-widget HUD system and replace the vanilla ESC menu with a full Quick-Access Panel.
>
> ⚠️ **UI medium audit (2026-07-10) / Scope Trim (2026-07-12) / Interaction pass (2026-07-14):** The original spec for 11+ widgets was heavily trimmed to eliminate bloat. Scrapped features include Director Mode, Auto-Profile Swap, Achievement Celebrations, and several minor widgets. On 2026-07-14 every widget got a locked click behaviour, the colour theme was locked, and **Context-aware shortcuts were cut entirely**. The final locked-in scope is explicitly detailed below.

> ✅ **2026-07-15 — GROUP 03 CLOSED / DONE.** A0 (silent HUD) + A3 (7×9 padlock) + A2 (macro banner) all
> passed MP. The active-tool chip deletion below shipped and is confirmed. One inherited defect stays open,
> handed to the bulk owner: **G03-BULKLOCK-HUD** — `/cb bulklock` never calls `WidgetSync.pushAll`, so bulk-lock
> padlocks don't appear until another push happens (single `/cb lock` does push). One line in `BulkFlagCommands`;
> tracked in Group 07 now. See TG3.
>
> 🔒 **2026-07-15 (MP run) — the HUD is now TWO widgets, not three.**
>
> **The active-tool chip is deleted.** Owner: the Omni-Tool "is kinda mid and won't be used" — it does not earn
> a permanent screen widget. Drop `ACTIVE_TOOL` from `HudWidgetType`, `drawActiveTool` from `HudWidgetRenderer`,
> and the `tool` field from `WidgetSync`. The tool still announces its mode: on the **hotbar**, once, on switch
> — that is Group 04 §F's job now, not the HUD's.
>
> **Why it had to go either way (TG3 A0):** `WidgetSync` sent `tool = OmniToolState.getMode(uuid)`, which is a
> **persistent per-player mode**, not "is holding the tool". Once you ever set GLOW, the chip rendered forever,
> on an otherwise-quiet HUD. Deleting the widget is less code than gating it, and it closes A0 and A1 together.
>
> **Padlock, owner-approved at 1:1:** a hand-drawn **7×9 pixel sprite** in CB red (highlight column on the lit
> side, darker right/bottom edge), **centred under the crosshair — 10px BELOW it, horizontally centred**
> (owner-locked 2026-07-15; this supersedes the earlier "10px to the right"). It must never overlap the
> crosshair itself — below it, not on it, so aim stays clear. Not the 🔒
> font glyph — same missing-glyph risk G27 T9 already flagged, which is why the Bulk grid draws its own. Locked
> is locked: the sprite carries **no owner identity**, so `LockManager` stays the flat set it is today.

---

## G03-2 · Full HUD/ESC rewrite — multi-widget system + quick-access panel

> 🔍 designed — brainstorm + scope trim locked 2026-07-12; **interaction/colour pass locked 2026-07-14**; **NOT built**.

### Locked decisions (Final Scope)

**1. Multi-widget HUD — DISPLAY-ONLY (clicking dropped 2026-07-14).** Independent widgets, each own on/off +
position. **HUD widgets are NOT clickable in-game** — Minecraft grabs the mouse while playing, so there is no
cursor, and a "hold a key to free the cursor" scheme was rejected (*"no one is gonna remember to click a
hotkey"*, owner). Every widget's *action* is reached through the **existing command it already maps to** — no
new interaction code, widgets only render state. The catalog is locked to these 7 widgets:

| Widget | Display behaviour | Action (via existing command — NOT a widget click) |
|---|---|---|
| **Active-tool chip** | live Omni-Tool mode icon + colour | change tool via the existing tool keybind/command |
| **Macro-recording banner** | pulses/flashes aggressively (red/white) while `/cb macro record` is active, cannot be missed | `/cb macro stop` |
| **Locked-block crosshair icon** | **7×9 padlock sprite 10px BELOW the crosshair, horizontally centred (never ON it)** so it never blocks aim; clearly reads "locked" | `/cb unlock <id>` (owner/locker/op) |
| **Buzzer Game score/timer** | on 0:00 it turns grey and calmly displays "Time's Up!" | — (display only) |
| **Incidents ping** | icon lights up, plays a subtle 'blip'/chime | `/cb incidents <code>` — the same jump the G04 chat error-link uses |
| **Vault-upload progress toast** | on failure: flashes red, plays a failure sound, auto-fades after a few seconds | existing vault cancel command |
| **Favorites quick-bar** | strip of exactly 3 icons (Previous, Current, Next) | existing favourites cycle command/keybind |

**2. ESC menu becomes a real quick-access panel** — this IS clickable (a cursor exists inside a Screen). Built via a **`GameMenuScreen` mixin, owner pre-approved 2026-07-14** (satisfies the CLAUDE.md Mixin Checkmark):
- **Live mini-previews** — Buttons show a fully 3D rendered thumbnail that spins. **Cost cap (locked 2026-07-14): spin only the on-screen buttons and cap simultaneous spinners (~12); off-screen thumbnails stay static until scrolled to.** Keeps "constantly spinning" visually true without tanking FPS. (The cheaper "spin only hovered/selected" option was explicitly rejected.)
- **Shareable HUD layouts** — export a widget layout as a share code. **Reuse the existing Group 20 vault-code system**, not a bespoke format — one code format mod-wide, no new parsing/storage path.
- **Op-forced widgets** — an op can pin a widget as force-visible for everyone. **Gate: vanilla op only** (no dedicated permission node). **Conflict rule: op-force WINS** over the player's own per-widget on/off toggle — a forced widget cannot be hidden locally.
- **Force-switch from chat** — clicking the **View** chip in chat (G04 §G04-4) runs the item's screen command, which makes this panel jump/re-scroll to that item **even if it is already open on something else**.

**3. Colour theme (locked 2026-07-14)**:
- **One shared default theme for all widgets: red / black / lime** — the same theme as the existing Screen GUIs. **`CbTheme.java` already IS this palette** (`ACCENT 0xFFFF0000`, `LIME 0xFF40FF00`, black panels) — straight reuse, **no new palette class**. Colour is a mod-wide look, not a per-widget meaning.
- Per-widget colour customisation exists, but is **organised, not a raw RGB mess**: a row of curated **swatches** for quick picks, a **themed** picker constrained to colours that harmonise with the HUD palette, and an **"advanced" full picker one click deeper** for anyone who insists. Every widget ships a good-looking default so most players never need to open it.
- **Widget on/off + position + colour config lives as a new "Widgets" tab inside the existing `HudEditorScreen`** (reuses its drag/position code), not a separate screen.

**4. Data Stubbing Strategy**:
- For any of the 7 widgets whose backend system (e.g. Vault, Buzzer Game) is not fully built yet, build **client-side stubs (fake placeholder data)** so the visual HUD can be built and tested without waiting for other groups.
- **Stubbed widgets are DEV/TESTING-ONLY** (behind a dev flag) and carry a subtle **"demo data" marker** (small icon/tint). **Normal players never see fake data** — a widget goes live for players only when its real backend lands, at which point the marker is removed.

**5. SCRAPPED Features (Do Not Build)**:
- **Context-aware shortcuts** — *cut entirely 2026-07-14.* The ESC panel does **not** show a dynamic state-driven shortcut list. Owner: *"unnecessary and confusing."* (This was inside the 2026-07-12 locked scope; it is now removed.)
- Director Mode hotkey
- Auto-profile-swap
- Achievement celebrations
- Recent-blocks strip
- Disguise reminder (Guess Mode)
- Undo-stack counter
- Team/shared scoreboard

**6. Inherited from Group 04 (2026-07-14):** the *live-updating chat line* idea (a message that rewrites itself in place — progress %, countdown) was rejected in Group 04 on technical grounds (vanilla has no API to edit an already-sent chat message). **That use case belongs here instead: progress/countdown lives in a HUD widget** in the shared CB overlay layer.

**7. Architecture (locked 2026-07-14): a NEW `HudWidget` system alongside `HudFieldType`, not an extension of it.** The existing `HudFieldType` is Group 27's catalogue of **text bricks** with string resolvers — the 7 new widgets are graphical/stateful (banner, padlock, progress bar, icon strip) and do not fit a string-resolver API. Build a sibling `HudWidgetType` with its own render + layout so the shipped G27 text-brick HUD stays untouched. Both draw through the existing `HudRenderMixin` **CB overlay layer** — the same owner-controlled layer G04's animated flourishes use (never a vanilla-chat rewrite).

---

### Cross-references
- Cross-links: Group 12/20 (vault share — **the HUD-layout share code reuses the G20 code system**), Group 16 (incidents ping → Diagnostics GUI jump), Group 17 (favorites bar), Group 22 (op-forced widgets), Group 31 (Buzzer score widget), **Group 04 §G04-4 (the View chip force-switches this ESC panel; the incidents-ping jump reuses the same clickable-error-link path)**.

| Group | Scope | Status |
|---|---|---|
| G03 — HUD & ESC | Client-heavy (widget render/drag/layout, display-only) + server sync + dev-only stubs | 🔍 designed — trimmed 2026-07-12, interaction + colour + architecture locked 2026-07-14, **not built**. |

**Touches:** a **new `HudWidgetType`** sibling system (NOT `HudFieldType`, which stays G27 text-only) · `HudRenderMixin` (the CB overlay layer the widgets + G04 flourishes share) · a "Widgets" tab in the existing `HudEditorScreen` · `CbTheme` (reused as-is, red/black/lime) · `GameMenuScreen` mixin (ESC panel, pre-approved; capped-spin 3D thumbnails) · Group 20 vault-code system (layout share) · Group 16 Diagnostics + `/cb incidents` (incidents-ping jump). **No HUD-click dispatch — widgets are display-only.**
**Verify in-game (when built):** each widget renders its state correctly; its action works through the mapped command; the padlock sits beside the crosshair without blocking aim; an op-forced widget cannot be hidden by a player; a saved layout round-trips through a Group 20 vault code; stub widgets appear only under the dev flag.
