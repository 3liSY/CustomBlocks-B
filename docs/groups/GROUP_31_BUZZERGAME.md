# Group 31 - BuzzerGame

> Group 31 owns a filmable buzzer/stopwatch minigame: a pressable buzzer block and a physical timer stand whose screen shows an Arabic target and a live result, hosted from a wand with no GUI.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_31.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

BuzzerGame is a YouTube-style reflex game built out of CustomBlocks pieces. A host places a buzzer and a timer stand, links them with a wand, sets a target time, and a player tries to start and stop the buzzer as close to that target as possible. The screen speaks Arabic to the host's audience: a target line and a live result line.

The Group exists so the minigame reuses the mod's real strengths — custom blocks, server-side display entities, and the bundled Arabic renderer — instead of being a bolt-on. It owns only the game's blocks, wand, session, and screen; it borrows the Arabic font and any future cloud sync from the Groups that own those.

## Ownership

| Owns | Does not own |
| --- | --- |
| The `customblocks:buzzer` block, the `customblocks:timer_display` stand, the `customblocks:buzzergame_wand`, and the `/cb buzzergame` command tree | Arabic glyph shaping and the bundled `arabtype` font pipeline: G13 |
| The in-memory host session, buzzer press routing, and the solo stopwatch round logic | Player-facing chat and hotbar routing helpers: G04 |
| The timer stand's server-side display entities and the on-screen Arabic/LED rendering, resize, and rotate | A future global scoreboard sync endpoint: G20 |
| The buzzer state a future HUD indicator would read | HUD overlay surface: G03 |

## Direction

One buzzer, one screen, one host. The host runs `start` with a target in seconds; the screen shows the target and result lines in RTL — number on the left, Arabic word on the right (`<target> : الهدف`, `0.00 : النتيجة`). The player presses the buzzer to start the result climbing, presses again to freeze it, presses a third time to clear back to `0.00` and try again. The number reads like a stopwatch — hundredths of a second, brand-green on a black screen. The stand is resizable part-by-part with the wand, and everything is instant: a press must register with no perceptible delay, the way recoloring a block is instant.

Multiplayer ranking (Duel and Party), reaction-race timing, and a dramatic host-run reveal are intentionally out of the current build and parked as a later quality-of-life pass.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-18 | Solo stopwatch is the only live game mode. | Duel, Party, reaction-race, false-start rules, and ranked reveal are parked; the session is always solo and those paths stay dormant/unreachable. |
| 2026-07-18 | `start` requires a target value (`5`, `5s`, `5.5`, `5.5s`), range 0.5–60s. | Bare `start` is rejected with a clear error; the value becomes the on-screen `الهدف`. |
| 2026-07-18 | The buzzer press cycle is start → freeze → clear. | Press 1 starts `النتيجة` counting from `0.00`, press 2 freezes it, press 3 resets to `0.00` and re-arms. No countdown, no auto-stop. |
| 2026-07-18 | The `reveal` command and the action-bar timer are removed; `stop` is removed. | The physical screen is the only readout; `reset` returns the screen to a single idle `0.00`. |
| 2026-07-18 | Screen Arabic labels are baked to white bitmap-font glyphs (Path B), not drawn as MC text. | The pre-shaped-constant approach failed in-game (I1): MC re-runs bidi on every line, reversing the visual-order glyphs and garbling the number. The two fixed words `الهدف` / `النتيجة` are baked once via Java2D `TextLayout` (correct shaping, same engine as `ArabicWordRenderer`) into a `customblocks:timer_label` bitmap font; PUA codepoints are bidi-neutral and white glyphs still tint with `textcolor`. Numbers stay in the `customblocks:led` bitmap font. Supersedes the pre-shaped `arabtype` text approach. |
| 2026-07-18 | One timer stand per session. | Multi-screen sync is dropped from the current scope. |
| 2026-07-18 | The stand is resized part-by-part with the wand. | Base, leg, screen, and whole-stand each resize independently; the model is split into separate display entities so a lower part growing lifts the parts above it. |
| 2026-07-18 | Text colour and text size are per-stand and fully customizable. | `/cb buzzergame textcolor <#hex>` (default `#15FF00`) and `/cb buzzergame textsize <n>` (up to 30×) act on the stand under the crosshair. |
| 2026-07-18 | The session is wand-owned and in-memory. | Using the wand starts/owns a host session; it is never persisted and dies on host logout, unlinking every linked block. |
| 2026-07-19 | Baked label PNGs must stay ≤256 px in both dimensions. | MC's font-atlas page is 256×256; a wider glyph is dropped and renders as a tofu box (the 351/360 px labels failed, I1). Re-bake smaller. |
| 2026-07-19 | Screen text layout is RTL: number on the left, Arabic word on the right (`5.00 : الهدف`). | Reads correctly right-to-left for an Arabic viewer; supersedes the word-left mockup. |
| 2026-07-19 | The screen keeps its −22.5° tilt; text is glued with full 3D tilt-correct placement. | Offset along the true tilted face-normal (with a vertical component) and separate the two lines along the tilted local-up axis, not world-Y. Fixes the persistent float-off (I2). |
| 2026-07-19 | Per-part hitboxes are server-side **interaction entities** that track each rendered part. Three parts: **neck, base, screen**. Each box re-fits its visual on every resize step (tracks the visual exactly, may overlap neighbors). | The invisible block's voxel can't leave its cell, so it never matched the floating/tilted/scaled visual; interaction entities (custom size, follow the parts) make select/rotate/link land on the part you see (I4). |
| 2026-07-19 | Resize is **per-part independent** (screen ×2 grows only the screen), step **×0.1** per wand action, range **×0.1 – ×5.0** per part. | Delivers the owner's "size neck only / base only" ask; fine step for dialing, wide bounds for freedom. |
| 2026-07-19 | Breaking = attack (left-click) any visible part → the whole stand breaks instantly. | Routed through the part interaction entities, not by mining the invisible voxel; fixes the "slow/bugged" break feel (I5/C2). |
| 2026-07-19 | Cooler LED screen: green bloom + faint "ghost 8" unlit segments behind the lit digits + a blinking colon (~1 Hz). **No scanlines** — plain dark glass (owner picked option C). | Authentic LED-clock look; bloom = a dim backing `TEXT_DISPLAY` layer, colon blinks on the tick. Scanline glass dropped to keep digits crisp. |
| 2026-07-19 | Redraw `led_digits.png` to clean 7-segment glyphs — current "5" is malformed (top bar stops short, cols 3–16 instead of full 3–20; other digits also inconsistent). | Owner saw the "5" missing pixels top/bottom; the baked sheet's bars aren't full-width. Rebuild all 10 as uniform 7-segment. |
| 2026-07-19 | The Arabic screen label is green-tinted to match the LED (baked PNG is white → recolor to green on bake). | One theme color; word + digits read as one lit display. |
| 2026-07-19 | Default placed stand size is ×1.3, and the owner can change it in-game via a trailing `setdefault`: `/cb buzzergame size <scale> setdefault` persists that scale as the spawn default; the plain form stays a one-off. Widen the numeric `size` arg bound from ×0.4–3.0 to **×0.1–5.0** to match the resize range. | Easy no-file default knob; hangs off the existing `size` subcommand (BuzzerGameCommands.java:81). |
| 2026-07-19 | The plain-text digit style is dropped — LED only. | The wand's Digit-style mode and the LED↔plain toggle (test E9) are removed; wand modes become Link / Resize / Rotate. |
| 2026-07-19 | The one-frame place flicker (C5/I6) is accepted as a minor regressed QoL item. | Server-side display entities render one default frame before the transform syncs; not worth blocking the pass. Tracked, not fixed now. |
| 2026-07-19 | Wand **Rotate** mode = yaw only, 45° steps (8 facings), whole stand. | Simple snappy decor placement; no pitch (keeps the screen upright at its fixed −22.5°). |
| 2026-07-19 | `/cb buzzergame textcolor` and `textsize` stay, acting on the **LED digits** only (label stays locked green). `textsize` scales digits inside the screen (ghost-8 scales with them), separate from part resize. | Per-stand non-green screens + digit scaling still wanted even with LED-only. |
| 2026-07-19 | **Both** label words — `الهدف` (target) and `النتيجة` (result) — get the same re-bake: ≤256px, green-tinted, RTL. | Consistent; the result screen (shown while counting) must not stay broken. |
| 2026-07-19 | Ghost-8 uses a dim shade of the current `textcolor` (red digits → faint red ghosts). | Cohesive display at any custom color. |
| 2026-07-19 | `textsize` auto-caps so digits never overflow the glass — clamps to the screen face regardless of the 30× arg. | Always looks clean; no digits spilling past the screen. |
| 2026-07-19 | Wand part-select on overlapping boxes picks the part **nearest the crosshair** surface. | Intuitive — you select what you're aiming at. |
| 2026-07-19 | **Visual look approved by owner** (flat in-world mockup): deepslate base + neck, red-trim head, lime accent, green LED `5.00` with bloom + blinking colon, green RTL `الهدف` label, plain dark glass. The whole third-pass design is now locked; nothing left to decide, ready for a build session. | Signed off from PIL mockups (true −22.5° tilt + real bloom only render in-game); this is the target the build reproduces. |

## Open Issues & Fixes (2026-07-19 second test pass)

The 2026-07-18 I1–I8 batch was retested in-game 2026-07-19. **§G (all press FX) confirmed; §A/§B still
confirmed; D1/D2, E5–E8, F5 confirmed.** The screen, hitbox, break, and flicker fixes did **not** hold and
are re-root-caused below into the third fix pass (design agreed with the owner 2026-07-19). Per-row status
lives in the [Testing Guide](../testing/Testing_Guide_31.md); this table is the design record.

| ID | Area | Symptom (2026-07-19) | Root cause | Third-pass fix |
| --- | --- | --- | --- | --- |
| I1 | E screen | Label still a tofu rectangle; LED numbers fine | Baked label PNGs are 351/360 px wide — wider than MC's 256 px font-atlas page → glyph dropped | Re-bake both words ≤256 px; RTL layout (number left, word right); shared baseline with the LED line |
| I2 | E screen | Screen text still floats off the tilted glass | The forward offset is XZ-only and ignores the −22.5° tilt's vertical component; line separation uses world-Y not the tilted up-axis | Full 3D glue: offset along the true face-normal, separate lines along the tilted local-up |
| I4 | C/F hitbox | Can't select/resize one part, or reliably click/rotate the stand | The invisible block's voxel is clamped to its cell and can't match the floating/tilted/scaled display entities | Spawn a server-side **interaction entity** per part (base/leg/screen) that tracks the rendered part; wand select/rotate/link raycast those |
| I5 | C feel | Stand break still reads slow | Crosshair lands on the floating visual, not the small voxel (strength is already 0.05) | Break via the part interaction entities: attack any part → whole stand breaks |
| I6 | C feel | One-frame place flicker persists | Display entity shows a default frame before the transform syncs | Accepted minor (regressed QoL); not fixed this pass |
| — | E screen | — (new, agreed polish) | — | Cooler LED: bloom + ghost-8 segments + blinking colon + scanline glass; drop the plain-text digit style; default size ×1.3 |
| I3 | D flow | D3–D8 unreadable | Blocked by the broken screen (I1/I2), not a logic fault | Re-verify once the screen renders |
| I7 | F wand | Hotbar label still wants a rework | — | Short glanceable label (Polish) |
| I8 | G FX | — | — | ✅ confirmed 2026-07-19 |

## Feature Plan

### A. Buzzer block and press detection

**Player outcome**

A player can obtain, place, and press a game-show buzzer, and every press is reliably attributed to the presser with no perceptible delay.

**Experience**

- The buzzer is a half-dome game-show block (red dome, black base) with a 1×1 footprint; the dome visibly pops on press and auto-releases.
- Pressing gives immediate audio/visual feedback; spam-clicking is debounced so a press is never double-counted.
- A press must register instantly, matching the responsiveness of the block-recolor tools.

**Requirements**

- The buzzer is a real registered block/item in the `customblocks` namespace (obtainable via `/give`, the creative tab, and `/cb buzzergame give buzzer`).
- Anyone can place it — no operator gate.
- Break near-instant so filming setups are quick to rearrange.
- Break particles use a real particle texture, not the missing-texture magenta.

**Boundary**

The buzzer carries no redstone in/out and owns no round logic — it only routes a press into the host session.

### B. Wand-owned session

**Player outcome**

A host holds one wand, starts a session by using it, and links a buzzer and a timer stand into that session; the session cleans itself up when the host leaves.

**Experience**

- Right-clicking the wand in the air starts/owns the caller's session and reports what is linked.
- Sneak + right-click air cycles the wand mode (Link, Resize, Rotate).
- In Link mode, clicking a buzzer or a stand links it into the caller's session.
- If a linked block is broken, or the host logs off, everything unlinks and nearby players are warned.

**Requirements**

- The session lives in memory keyed by the host's UUID; multiple hosts run fully independent sessions.
- No admin gate — anyone can host.
- Links store the block position so logout/break cleanup can reach the real blocks.

**Boundary**

There is no placed control block and no persistence; disconnect is the only cleanup trigger.

### C. Block fixes and instant feel

**Player outcome**

Placing and breaking both blocks feels immediate, and breaking them shows correct particles.

**Experience**

- Both blocks break near-instantly.
- The timer stand's display entities appear the moment it is placed, with no one-tick flicker.
- Break particles match each block's material instead of showing magenta/black.

**Requirements**

- Add a `particle` texture to the buzzer and timer-stand models.
- Lower block strength so survival breaking is near-instant.
- Spawn the stand's display entities on placement rather than on the first block-entity tick.

**Boundary**

This area is purely feel/rendering fixes; it changes no game logic.

### D. Solo stopwatch flow

**Player outcome**

A host arms a target and a player uses one buzzer to try to hit it.

**Experience**

- `/cb buzzergame start <value>` arms the round: the screen shows `<target> : الهدف` and `0.00 : النتيجة` (RTL, number left / word right).
- Buzzer press 1 starts `النتيجة` climbing live; press 2 freezes it; press 3 clears to `0.00` and re-arms.
- `/cb buzzergame reset` clears the target and returns the screen to a single idle `0.00`.
- Each press has a distinct sound and particle: a start cue, a sharp stop cue, and a soft reset cue.

**Requirements**

- `start` requires a value in 0.5–60s (`N`, `Ns`, decimals); reject bare or out-of-range input with a `[CB]` error.
- An `ARMED` session state sits between `start` and the first press (target shown, clock idle); the clock only advances while running.
- The result reads in hundredths, `0.00` under ten seconds and `00.00` at ten seconds or more.
- Command feedback uses the G04 chat/hotbar contract (`Chat.success`/`error`/`info`, hotbar `Chat.tool*`).

**Boundary**

No countdown, no multiplayer scoring, no reveal — those are parked (see Deferred Scope).

### E. Physical timer stand and screen

**Player outcome**

The stand shows a readable, brand-styled Arabic target and a live result, and the host can recolor and resize the text.

**Experience**

- The stand is a real placeable block that is itself invisible; its look is server-side display entities, so it stays freely resizable and rotatable.
- Both faces are readable (player side and camera side).
- Arabic labels are cursive and correct; the numbers use the glowing LED clock font; `النتيجة` is the larger line.
- `/cb buzzergame textcolor <#hex>` recolors the LED digits (default `#15FF00`; the ghost-8 follows a dim shade of it — the Arabic label stays locked green); `/cb buzzergame textsize <n>` scales the digits up to 30× (auto-capped to the glass); both target the stand under the crosshair.

**Requirements**

- Arabic labels are the two fixed words `الهدف` and `النتيجة`, both re-baked ≤256px, green-tinted, to bitmap-font glyphs (Path B, via Java2D `TextLayout`) and shown as bidi-neutral PUA glyphs — MC cannot be trusted to render Arabic text without reversing/garbling it (I1).
- Numbers render in the `customblocks:led` bitmap font (digits and dot only), redrawn to clean uniform 7-segment glyphs, with a green bloom + faint ghost-8 unlit segments and a blinking (~1 Hz) colon for an authentic LED-clock look; plain dark glass, no scanlines.
- The digit/label panels are anchored onto the tilted screen face (full 3D tilt-correct glue) and scale with the screen; layout is RTL (number left, Arabic word right, e.g. `5.00 : الهدف`).
- LED is the only digit style; the plain-text toggle is dropped.

**Boundary**

Only one stand links per session; custom frame images/GIFs and the idle logo/GIF are deferred.

### F. Per-part resize and rotate

**Player outcome**

The host can grow or shrink the base, the leg, the screen, or the whole stand independently, and turn it to any angle.

**Experience**

- In wand Resize mode, shift-right-clicking a part of the stand (base, neck, or screen) selects it by where the crosshair lands; on overlapping boxes the part nearest the crosshair wins.
- Right-click grows the selected part one ×0.1 step; sneak + left-click shrinks it; chat reports the part and its new size. Per-part range is ×0.1–×5.0, applied independently (screen ×2 grows only the screen).
- Growing a lower part lifts the parts above it so the stand stays assembled.
- Text scales with the screen part, and with the whole stand when the whole stand is scaled.
- `/cb buzzergame size small|medium|large|<scale>` (numeric ×0.1–5.0, optional trailing `setdefault` to persist the spawn default) still scales the whole stand; wand Rotate mode turns the whole stand in yaw 45° steps (8 facings).

**Requirements**

- The stand model is split into base/neck/screen sub-models, each its own display entity, repositioned relative to each other on resize.
- Part scales clamp to ×0.1–5.0; text scale auto-caps to the glass (the 30× arg is accepted but clamped so digits never overflow the screen); a per-part server-side interaction entity tracks each rendered part so select/rotate/link/break land on the part you see.
- The persisted display-entity handles and NBT expand to cover the split parts and both text lines.

**Boundary**

The blueprint-style live cursor-drag preview from the earlier spec is replaced by discrete stepping; a live preview is not part of this scope.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G03 | Buzzer state | A future HUD indicator reads real BuzzerGame session state; G31 does not invent player-visible HUD data. |
| G04 | Chat and hotbar routes | G31 sends all player-facing text through the G04 `Chat.*` helpers and never hand-builds a prefix, glyph, or hotbar colour. |
| G13 | Arabic font and shaping | G31 reuses the bundled `arabtype` font and G13's shaping knowledge for its screen labels; it does not fork a second Arabic font. |
| G20 | Cloud scoreboard | A future global scoreboard reuses the existing Cloudflare Worker vault endpoint rather than new plumbing. |

## Technical Contract

- The buzzer and timer stand are real registered blocks/items in the `customblocks` namespace, not slot-system items, so vanilla `/give` works.
- The timer-stand block renders `INVISIBLE`; its entire visual is server-side `ITEM_DISPLAY`/`TEXT_DISPLAY` entities driven from the block entity, which is what makes live resize and rotate possible.
- The host session is in-memory, keyed by host UUID, never persisted, and destroyed on host logout (which unlinks every linked block).
- Screen Arabic labels are green-tinted bitmap-font glyphs (≤256px each) in the `customblocks:timer_label` provider, baked once from the logical strings via Java2D `TextLayout` (correct shaping/joining). MC's TTF renderer neither shapes Arabic nor leaves pre-shaped glyphs alone — it re-runs bidi and reverses/garbles them (I1) — so the labels are images, not text; PUA codepoints keep them bidi-neutral. The label colour is baked green; `textcolor` tints only the LED digits. Numbers use the `customblocks:led` bitmap font (0–9 and `.` only), redrawn to uniform 7-segment glyphs.
- One timer stand links per session.
- Note-block `SoundEvents` constants use `.value()`; all other sound constants stay bare.
- Resize is applied through display-entity transform scale; part scales stay within a modest cap and text scale clamps to 30×, with the block click-box tracking the rendered size.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Duel / Party multiplayer with ranked reveal | Parked as a later quality-of-life pass; solo ships first. | G31 |
| Reaction-race format + false-start rules | Depends on the parked multiplayer path. | G31 |
| Host-run dramatic `reveal` | Removed from the solo flow; only meaningful with ranked multiplayer. | G31 |
| Global scoreboard synced to the Cloudflare Worker | Cloud plumbing is later polish, not core gameplay. | G31 with G20 |
| Real custom `.ogg` game-show SFX | Vanilla sound combos ship first; sourced audio comes later. | G31 |
| Baked LED-texture digit mode | The text-display LED font covers the current look. | G31 |
| Idle logo/GIF, custom frame image/GIF, named size presets | Fixed default look ships first. | G31 |
| Team mode and custom team assignment | Waits until TEAM mode is actually scoped. | G31 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-18 | Countdown + reaction/precision formats with a host-run ranked reveal | Solo stopwatch: `start <value>` arms a target, the buzzer press cycle drives one result, no reveal. |
| 2026-07-18 | Firjar TTF for the on-screen Arabic | `arabtype` with pre-shaped constants; Firjar cannot render connected Arabic in Minecraft. |
| 2026-07-18 | Pre-shaped constants drawn as MC text in the `arabtype` font | Baked white bitmap-font glyphs (Path B): MC re-runs bidi on any Arabic line and reverses/garbles the pre-shaped glyphs (I1), so labels ship as images, not text. |
| 2026-07-18 | Multiple linked displays per session | One timer stand per session. |
| 2026-07-18 | Whole-stand-only resize with a live drag-preview tool | Per-part resize with discrete wand stepping; whole-stand `size` command kept. |
| 2026-07-07 | A placed admin panel block owns the session | The session is wand-owned and in-memory. |
| 2026-07-07 | Multiblock timer wall | Compact single-block stand rendered by display entities. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_31.md) · [All Groups](README.md)

- [G03 HUD System and ESC Integration](GROUP_03_HUD_ESC.md)
- [G04 Communication](GROUP_04_Communication.md)
- [G13 Arabic](GROUP_13_ARABIC.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- Source: `Active_Projects/TimerChallenge` (reference-only original standalone mod)
- [Pre-template Group 31 snapshot](../archive/group-migration-2026-07-18/GROUP_31_BUZZERGAME.md)
