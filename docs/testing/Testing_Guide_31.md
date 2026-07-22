# Group 31 - BuzzerGame

## Status

| | |
| --- | --- |
| **Verdict** | Screen text, stand outline, stopwatch flow, and per-part resize are built and awaiting owner retest; buzzer, wand session, and FX are confirmed. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 37% |
| **Last tested** | 2026-07-19 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| E | Physical timer stand and screen | Built 🎯 | Regression 💔 — screen text rebuilt, retest |
| C | Block fixes and instant feel | Built 🎯 | Regression 💔 — stand outline restored, retest |
| D | Solo stopwatch flow | Built 🎯 | Regression 💔 — retest press flow |
| F | Per-part resize and rotate | Built 🎯 | Regression 💔 — retest resize + rotate |
| G | Press sound and particle FX | Done ✅ | - |
| A | Buzzer block and press detection | Done ✅ | - |
| B | Wand-owned session | Done ✅ | - |
| H | Duel / Party ranked reveal | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_31_BUZZERGAME.md](../groups/GROUP_31_BUZZERGAME.md)

---

# Active Tests

## 💡 Setup

- Use OP or a regular account — no admin gate on any BuzzerGame item.
- Give yourself the gear: `/cb buzzergame give buzzer`, `give display`, `give wand`.
- Place one buzzer and one timer stand, then link both to your wand session (Link mode: right-click the wand in air, then right-click each block).
- **Switch wand jobs with sneak + right-click in air** — it cycles Link → Resize → Rotate, shown on the hotbar. The wand starts in Link. All resize/rotate tests need the matching mode. (The old Digit-style mode is dropped — LED only.)
- Keep tests in build order: block fixes → stopwatch flow → screen → resize → FX.
- Single player is enough; the solo flow needs one buzzer and one stand only.

## C - Block fixes and instant feel - Built 🎯 · Regression 💔

| | |
| --- | --- |
| **Check** | Both blocks break near-instantly, show correct break particles, and the stand's visual appears the moment it is placed. |
| **Pass rule** | Break-speed, break-particle, and place-no-flicker rows pass twice for both blocks. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Break a placed buzzer in survival. | Breaks near-instantly (a couple of hits), not a long metal-mining delay. | ✅ `2026-07-19` | 🎯 |
| C2 | Aim at a placed timer stand, then left-click (attack). | A wireframe outline hugs the visible stand (no more oversized/absent box), and it breaks instantly on one click — not a slow mine on empty space. | 🎯 | 🎯 |
| C3 | Watch the break particles on the buzzer. | Particles match the block, no magenta/black missing-texture. | ✅ `2026-07-19` | 🎯 |
| C4 | Watch the break particles on the timer stand. | Particles match the stand material, no magenta/black. | ✅ `2026-07-19` | 🎯 |
| C5 | Place a timer stand and watch closely. | Stand + screen appear at once; a one-frame flicker is accepted (locked C5/I6) and is not a fail. | 🎯 | 🎯 |

## D - Solo stopwatch flow - Built 🎯 · Regression 💔

| | |
| --- | --- |
| **Check** | `start` requires a target, the buzzer press cycle drives one live result, and `reset` returns to idle. |
| **Pass rule** | Start-validation, arm, press-cycle, format-switch, and reset rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb buzzergame start` with no value. | Rejected with a clear `[CB]` error; nothing arms. | ✅ `2026-07-19` | 🎯 |
| D2 | Run `/cb buzzergame start 0.2` or `start 90`. | Rejected — out of the 0.5–60s range. | ✅ `2026-07-19` | 🎯 |
| D3 | Run `/cb buzzergame start 5` (also try `5s`, `5.5`). | Arms: screen shows `5.00 : الهدف` and `0.00 : النتيجة` (RTL — number left, word right); the clock is idle. | 🎯 | 🎯 |
| D4 | Press the buzzer once (press 1). | `النتيجة` starts climbing live from `0.00` with no perceptible delay. | 🎯 | 🎯 |
| D5 | Press the buzzer again (press 2). | `النتيجة` freezes at the stopped value. | 🎯 | 🎯 |
| D6 | Press the buzzer a third time (press 3). | `النتيجة` clears to `0.00` and is ready for another attempt; `الهدف` stays. | 🎯 | 🎯 |
| D7 | Let a run pass ten seconds before pressing 2. | The result number grows from one integer digit to two at 10s+ (e.g. `9.85` → `12.34`), keeping the dot + hundredths. | 🎯 | 🎯 |
| D8 | Run `/cb buzzergame reset`. | Screen returns to a single idle `0.00`; `الهدف` gone; must `start` again to arm. | 🎯 | 🎯 |
| D9 | Confirm removed commands. | `stop` and `reveal` no longer exist / do nothing meaningful. | 🎯 | 🎯 |

## E - Physical timer stand and screen - Built 🎯 · Regression 💔

| | |
| --- | --- |
| **Check** | The Arabic words render as green image quads (never tofu), the number is the LED font right-aligned to centre, both sit on the tilted glass, and colour/size are customizable. |
| **Pass rule** | Word-shape, both-faces, glued-layout, two-line-size, LED-number, textcolor, and textsize rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Read the `الهدف` / `النتيجة` words after `start 5`. | Both render connected/cursive, correct, and green — **every time** (they are baked image quads now, not a font glyph, so they can't tofu, reverse, or garble). | 🎯 | 🎯 |
| E2 | View the screen from the front and the back. | Both faces show a readable word + number (the word reads correctly from behind, not mirrored). | 🎯 | 🎯 |
| E3 | Look at the layout. | On each line: LED number on the left, blinking colon at centre, Arabic word glued just to its right — all sitting on the tilted black glass, not floating beside/below it. | 🎯 | 🎯 |
| E4 | Compare the two lines. | `النتيجة` line (number + word) is larger than the `الهدف` line. | 🎯 | 🎯 |
| E5 | Look at the numbers. | Numbers render in the glowing LED clock font; default colour `#15FF00`. | ✅ `2026-07-19` | 🎯 |
| E6 | Run `/cb buzzergame textcolor #ff0000` on the stand you're aiming at. | The LED digits + colon (and the ghost-8) recolor to red; the Arabic word stays green (it is a locked-green image, not tinted text). | 🎯 | 🎯 |
| E7 | Run `/cb buzzergame textsize 12`. | The LED digits AND the Arabic word scale up together, auto-capped so neither overflows the glass. | 🎯 | 🎯 |
| E8 | Break the stand or log the host off mid-session. | Stand unlinks and nearby players are warned. | ✅ `2026-07-19` | 🎯 |
| E10 | Watch the LED digits idle. | Clean uniform 7-segment digits with a green bloom, a faint "ghost-8" behind each digit, and the colon blinking ~1 Hz; plain dark glass (no scanlines). | 🎯 | 🎯 |
| E9 | *(removed)* Digit-style toggle is dropped — LED is the only digit style. | — | ➖ | ➖ |

## F - Per-part resize and rotate - Built 🎯 · Regression 💔

| | |
| --- | --- |
| **Check** | Base, neck, and screen each resize independently via the wand (×0.1 step, ×0.1–5.0), the stand stays assembled, and rotate still works. |
| **Pass rule** | Part-select, grow/shrink, reassembly, text-scaling, whole-size, and rotate rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Wand Resize mode: shift-right-click the neck part. | The neck is selected (nearest-crosshair part on overlap); chat reports the part + its size. | 🎯 | 🎯 |
| F2 | Right-click to grow, sneak + left-click to shrink the selected part. | The part steps bigger/smaller by ×0.1 (range ×0.1–5.0); chat reports each step. | 🎯 | 🎯 |
| F3 | Grow the neck, then the base. | Parts above the grown part lift so the stand stays assembled, not clipping/overlapping. | 🎯 | 🎯 |
| F4 | Select and grow the screen part. | The screen and its text scale together. | 🎯 | 🎯 |
| F5 | Run `/cb buzzergame size large`. | The whole stand (all parts + text) scales together. | ✅ `2026-07-19` | 🎯 |
| F6 | Wand Rotate mode: right-click the stand. | The whole stand turns 45° (yaw) per click without being replaced. | 🎯 | 🎯 |
| F7 | Cycle wand mode (sneak + RC air); read the hotbar. | Short, glanceable mode label (`⛓ Link` / `⤢ Resize` / `⟳ Rotate`) — not a long jargon sentence. | 🎯 | 🎯 |
| F8 | Place a fresh stand; then `/cb buzzergame size 2 setdefault` and place another. | The first spawns at ×1.3 (shipped default); after `setdefault`, new stands spawn at the saved size (persists across restart). | 🎯 | 🎯 |

> Note: in Resize mode, before you shift-RC any part the selection is the **whole stand**, so RC-grow / sneak+LC-shrink resize everything (same as `size`). In Rotate mode, right-click **air** flips the turn direction (clockwise ↔ anti-clockwise).

## G - Press sound and particle FX - Done ✅

| | |
| --- | --- |
| **Check** | Each buzzer press in the cycle has a distinct, good-looking sound + particle combo. |
| **Pass rule** | Start, stop, and reset FX rows each pass twice and read as clearly different. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Press 1 (start). | A rising start cue + a green particle burst. | ✅ `2026-07-19` | 🎯 |
| G2 | Press 2 (stop). | A sharp "locked" cue + a bigger burst, clearly different from start. | ✅ `2026-07-19` | 🎯 |
| G3 | Press 3 (reset). | A soft reset click + a small puff. | ✅ `2026-07-19` | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Buzzer block and press detection — ✅ `2026-07-06`
- §B Wand-owned session — ✅ `2026-07-18`
- §G Press sound and particle FX — ✅ `2026-07-19`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §E screen text — 💔 `2026-07-19`: the Arabic words repeatedly failed to render; rebuilt as image quads. Awaiting owner retest. Root cause: `PROGRESS_LOG.md`.
- §C stand outline — 💔 `2026-07-19`: the stand had no outline to aim at; restored to hug the visible parts. Awaiting owner retest. Root cause: `PROGRESS_LOG.md`.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §H Duel / Party ranked reveal — 💤 `2026-07-18`: multiplayer ranking, reaction-race format, false-start rules, and the host-run reveal are a later quality-of-life pass; solo stopwatch ships first.
- Sound/VFX polish, session-local + global scoreboards, and the Cloudflare Worker endpoint — 💤 `2026-07-07`: later polish after core gameplay and display.
- Display polish (custom frame image/GIF, idle logo/GIF, named presets, team assignment) — 💤 `2026-07-07`: later, fixed default look ships first. *(The LED 7-segment digit font shipped in §E; the Digit-style toggle was dropped — LED is the only digit style.)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- `reveal` command — 👎 `2026-07-18`: only meaningful with ranked multiplayer, which is parked.
- `stop` command — 👎 `2026-07-18`: buzzer press 2 already freezes the result, so the command is redundant.
- Action-bar timer readout — 👎 `2026-07-18`: the physical screen is the only readout.
- Firjar TTF for on-screen Arabic — 👎 `2026-07-18`: Minecraft cannot shape Arabic and Firjar lacks the shaped glyphs; `arabtype` with pre-shaped constants is used instead.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove test buzzers, timer stands, and wand-owned sessions.
- [ ] Remove any dormant Duel/Party/reaction/reveal code paths if they are not kept for the parked pass.
- [ ] Clear any Cloudflare test scoreboard entries if a future endpoint is exercised.

</details>
