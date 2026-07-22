# Group 31 (BuzzerGame) — Fix Plan (deep-search pass, 2026-07-18)

Working spec for the TG31/G31 issue batch. Every issue below is root-caused against the actual code
with file:line evidence, a concrete fix, the files touched, and how to verify. Nothing here is
implemented yet — this is the pre-build agreement.

Decisions locked with the owner:
- **Text approach = Path B** (bake the two Arabic labels as white bitmap-font glyphs; no MC text shaping).
- **IDLE buzzer press = plain stopwatch** (no target line). The `start` command still arms a هدف target.
- **C2 stand break = "slow like before"** (breaks eventually, feels slow/unresponsive — not "never").

---

## Issue index

| ID | Section | Symptom | Severity |
| --- | --- | --- | --- |
| I1 | E (screen) | Arabic reads left-to-right; digits garbled; one glyph is a tofu box | High |
| I2 | E (screen) | **Regression** — text floats in front of / below the screen, not glued | High |
| I3 | D (flow) | A linked buzzer won't start on press; needs `/cb buzzergame start` first | High |
| I4 | C/E (hitbox) | Screen + buzzer hitboxes inaccurate; can't resize the stand easily with the wand | Med |
| I5 | C (feel) | C2 stand break slow/bugged (C1 buzzer instant, C3/C4 particles fine) | Med |
| I6 | C (feel) | C5 place — one-tick flicker/gap before the visual appears | Low |
| I7 | F (wand) | Mode hotbar shows a long jargon sentence; not glanceable | Med |
| I8 | G (FX) | G3 reset sound barely hearable; all three want to sound cooler | Med |

---

## I1 — Arabic reads L→R, number garbled, tofu box

**Symptom.** On-screen labels render disconnected/reversed (read left-to-right), the result number
comes out scrambled (`0.04` split), and one character shows as an empty rectangle.

**Root cause.** The screen uses raw Minecraft TTF text. Labels are stored **pre-shaped, in visual
(L→R) order** as presentation-form constants:
- `TimerDisplayVisual.LABEL_TARGET` / `LABEL_RESULT` — [TimerDisplayVisual.java:57-60](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L57-L60)
- drawn via `lineText()` in the `customblocks:timer_arabic` TTF — [TimerDisplayVisual.java:281-294](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L281-L294)

Minecraft re-runs the Unicode **bidi** algorithm on every rendered line (client side). Arabic
presentation forms are strong-RTL, so MC reverses the already-visual glyphs a **second time** (→ back
to logical order, i.e. it now reads left-to-right) and drags the weak/neutral characters — the `.`
in the number, the `": "` separator — around the RTL run. That reordering fragments the LED-font
digit run and can leave a neutral char without a glyph → the tofu box.

Ruled out: the PNG font is clean. `led_digits.png` is 240×40 (exactly 24px × 10 glyphs), `led_dot.png`
12×40, all glyphs present ([font/led.json](../../../src/main/resources/assets/customblocks/font/led.json)).
So it is not a missing-glyph / slicing bug — it is bidi reordering.

**Fix (Path B — bake labels as bitmap-font glyphs).** Reference exemplar: `ArabicWordRenderer` uses
Java2D `TextLayout`, which shapes + orders Arabic correctly and is the dev-approved quality bar
([ArabicWordRenderer.java:1-30](../../../src/main/java/com/customblocks/arabic/ArabicWordRenderer.java#L1-L30)).

1. Bake two **white** PNGs once (build-time), from the LOGICAL Arabic strings via `TextLayout`
   (cursive recipe, same as the ARABIC WORDS recipe in ArabicWordRenderer):
   - `الهدف`  = U+0627 U+0644 U+0647 U+062F U+0641  → `timer_label_target.png`
   - `النتيجة` = U+0627 U+0644 U+0646 U+062A U+064A U+062C U+0629 → `timer_label_result.png`
2. Add a bitmap font provider `customblocks:timer_label` mapping two Private-Use codepoints to those
   PNGs (e.g. U+E000 = target, U+E001 = result). New file
   `assets/customblocks/font/timer_label.json` + the two PNGs under `textures/font/`.
3. In `lineText()`, replace the label literal with the single PUA glyph in the `timer_label` font,
   keep the number in the `led` font. PUA chars are bidi-neutral (treated as L) → they stay put, and
   a white glyph still tints with the `textcolor` style (so E6 recolor still works).

Because there are only two fixed words, nothing is baked at runtime — the PNGs + font ship in assets
like `led`, served by the existing resource-pack path.

**Files.** `TimerDisplayVisual.java` (lineText + font ids), new `font/timer_label.json`, new
`textures/font/timer_label_target.png` + `..._result.png`. Bake script (throwaway) under
`tools/`.

**Verify.** E1 (labels connected/cursive, correct, not reversed), E2 (both faces), E6 (textcolor
recolors labels too), and the number is never garbled regardless of value.

---

## I2 — Text separated from the screen (regression)

**Symptom.** The target/result text floats in front of and below the black glass instead of sitting
on it.

**Root cause.** The glue geometry constants are placeholder guesses (the code comments say so:
"TUNABLE — expect a couple in-game rounds", [TimerDisplayVisual.java:83-92](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L83-L92)).
Derived truth, validated against the correctly-rendered parts in the owner's screenshot:

- Item-display `none` maps `world = entityPos + (modelPoint − 0.5)·scale`; the parts render right, so
  the convention holds. The screen ITEM_DISPLAY entity sits at `blockY + 0.5`.
- Glass-front center of [timer_display_screen.json](../../../src/main/resources/assets/customblocks/models/block/timer_display_screen.json)
  (screen_front `[3,8.7,9.1]`–`[13,14.8,9.32]`, tilted −22.5° about `[8,8,8.25]`) works out to model
  **(0.5, 0.739, 0.482)** → world **(x+0.5, y+0.739, z+0.482)**.

Two concrete errors in `textNbt()` ([TimerDisplayVisual.java:248-266](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L248-L266)):
1. `TEXT_FORWARD = 0.34` pushes the text to `z+0.84` — **0.36 block south of the glass** (glass is at
   z+0.482). This is the "separated / not touching" regression.
2. `SCREEN_PITCH = 22.5f` but the model tilt is **−22.5°**. Front pitch is applied as `+SCREEN_PITCH`
   ([:252](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L252)) → the text
   plane tilts the wrong way vs the glass.

`faceCenterY` (posY+0.5+0.234 = 0.734) already matches the glass Y (0.739) — leave it.

**Fix.** `TEXT_FORWARD ≈ 0.02`; `SCREEN_PITCH = -22.5f` so front = −22.5, back = +22.5. Keep
`SCREEN_FACE_CENTER`, retune the two `*_LINE_UP` offsets only if the two lines need re-centering after
the forward/pitch fix (change one variable at a time, owner tests between). Optional polish: offset
the two lines along the tilted face's local-up axis (currently Y-only), but that is a small error at
these offsets and can wait.

**Files.** `TimerDisplayVisual.java` (constants only).

**Verify.** E3 (text glued to the black face from both front and back), then E4 line sizing.

---

## I3 — Buzzer won't start without the command

**Symptom.** A buzzer linked to a stand (nothing else) does nothing on press; you must run
`/cb buzzergame start <n>` first.

**Root cause.** The solo cycle has no IDLE→START. `BuzzerSession.onBuzz()` returns `IGNORED` in IDLE
("Arm a target first") — [BuzzerSession.java:210-212](../../../src/main/java/com/customblocks/buzzergame/BuzzerSession.java#L210-L212).
Only `armTarget()` (reached via the `start` command) moves the session to ARMED, so an un-armed press
is dead.

**Fix.** Add an IDLE case to `onBuzz()`: start a **target-less** run — `state = RUNNING`,
`elapsedTicks = 0`, return `START`. The screen already:
- shows elapsed for RUNNING/RESULTS in `resultText()` ([:445-450](../../../src/main/java/com/customblocks/buzzergame/BuzzerSession.java#L445-L450)),
- hides the target line when `hasTarget()` is false ([:435-437](../../../src/main/java/com/customblocks/buzzergame/BuzzerSession.java#L435-L437)),

so a plain stopwatch needs no other change: press 1 = start, press 2 = freeze (RESULTS), press 3 =
back to IDLE (target-less reset, not ARMED). The existing `start` command path is untouched — arming
a هدف still enters the ARMED→RUNNING→RESULTS flow with the target line shown.

One detail: RESULTS press currently goes to ARMED ([:205-208](../../../src/main/java/com/customblocks/buzzergame/BuzzerSession.java#L205-L208)).
For a target-less run, press 3 should return to IDLE (no target) instead. Track whether the run was
armed vs target-less (e.g. `hasTarget` at start, or a boolean) so press 3 lands in the right state.

**Files.** `BuzzerSession.java` (`onBuzz`, small state bookkeeping).

**Verify.** New: link buzzer+stand, no command → press starts count from 0.00, press freezes, press
resets to idle 0.00. Regression: D1–D8 with the `start` command still behave (هدف shown, arm cycle).

---

## I4 — Hitboxes inaccurate; stand hard to resize with the wand

**Symptom.** Screen and buzzer selection boxes feel wrong; selecting/resizing stand parts with the
wand is fiddly.

**Root cause (stand).** `TimerDisplayBlock.shapeFor()` returns a fat, hollow cuboid that does not
match the thin visible stand and ignores per-part scale:
[TimerDisplayBlock.java:93-99](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayBlock.java#L93-L99).
Outline = `min(8, 6·scale)` half-width × full height (12×16×12 at scale 1). It uses
`renderedScale(getScale())` = the **whole** scale only, so growing a single part (leg/screen) never
updates the box.

**Root cause (wand select).** `BuzzerGameWand.selectPart()` picks the part from crosshair height using
bands `BAND_BASE_TOP = 0.34` / `BAND_LEG_TOP = 0.67`
([BuzzerGameWand.java:68-71](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameWand.java#L68-L71),
[:244-254](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameWand.java#L244-L254)). The real
model bands are base 0–2.5px (0–0.156), leg 2.5–9px (0.156–0.5625), screen 8–16px (0.5–1.0). So the
0.34/0.67 split selects the wrong part for most clicks → "can't resize easily".

**Root cause (buzzer).** Actually fine — `SHAPE = (2,0,2)–(14,10,14)` matches the model exactly
([BuzzerBlock.java:56](../../../src/main/java/com/customblocks/buzzergame/BuzzerBlock.java#L56),
[models/block/buzzer.json](../../../src/main/resources/assets/customblocks/models/block/buzzer.json)).
Only looseness is a square box over the domed top. Optional: tighten the top of the shape to the
button footprint (5–11px) if the owner wants it snug.

**Fix.**
- `shapeFor()`: build the outline from the real part columns (base/leg/screen widths + heights) and
  factor in per-part scales (base/leg/screen), not just whole scale, so the box tracks the actual
  rendered stand.
- `selectPart()` bands → ~0.16 (base top) and ~0.56 (leg top) to match the model. Better: compute the
  bands from the same part-height constants used by the visual so they can never drift apart.

**Files.** `TimerDisplayBlock.java` (shapeFor), `BuzzerGameWand.java` (bands), optionally
`BuzzerBlock.java` (snug top).

**Verify.** F1 (shift-RC selects the part under the crosshair reliably), F3 (parts stay assembled and
the highlight box tracks them), and the highlight box hugs the visible stand.

---

## I5 — C2 stand break slow (C1 buzzer instant)

**Symptom.** Buzzer breaks instantly (C1 ✅); the stand breaks slowly/feels bugged (C2), even though
both use identical block settings.

**Root cause.** Both blocks are `strength(0.2f, 6.0f)`, METAL, nonOpaque
([BuzzerGameRegistry.java:66-82](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameRegistry.java#L66-L82)),
so server-side break time is identical. No break interceptor slows it — the only `AttackBlockCallback`
is the wand's, which passes through unless you hold the wand **and** sneak
([BuzzerGameWand.java:261-270](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameWand.java#L261-L270),
registered [CustomBlocksMod.java:242-243](../../../src/main/java/com/customblocks/CustomBlocksMod.java#L242-L243));
the global `PlayerBlockBreakEvents.BEFORE` only guards GuessShowcase
([CustomBlocksMod.java:162-164](../../../src/main/java/com/customblocks/CustomBlocksMod.java#L162-L164)).

So the difference is **feel, not time**: the buzzer is a MODEL block, so you see the crack overlay
progress → reads instant. The stand block is INVISIBLE (the visual is display entities), so while
mining there is no crack feedback on it; combined with the oversized hollow outline (I4) you are often
mining the front face of an empty box floating around the thin stand. Nothing appears to happen until
it pops → reads as "slow/bugged". Break particles still fire at the pop (C3/C4 ✅).

**Fix.** Two parts, both cheap:
1. Tighten the outline to the visible stand (shared with I4) so the crosshair only catches the real
   shape and the highlight aligns with what you see.
2. Lower the stand hardness to a true one-hit (e.g. `strength(0.05f, 6.0f)`), matching the buzzer's
   *perceived* instant break, since there is no crack feedback to sell a 0.2 mine.

**Files.** `BuzzerGameRegistry.java` (stand strength), `TimerDisplayBlock.java` (outline, shared I4).

**Verify.** C2 breaks in ~one hit and feels as instant as C1.

---

## I6 — C5 place flicker

**Symptom.** A one-tick gap between placing the stand and its visual appearing.

**Root cause.** The block is INVISIBLE; its only look is display entities that must be spawned and
then synced to the client. `onPlaced` already spawns them immediately to minimise the gap
([TimerDisplayBlock.java:116-123](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayBlock.java#L116-L123),
[TimerDisplayBlockEntity.ensureSpawnedOnPlace:197-208](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayBlockEntity.java#L197-L208)),
but a freshly spawned display entity renders at its default transform for one frame before the NBT
transform is applied client-side → a brief snap/flicker.

**Fix.** Set the transform + `interpolation`/`teleport_duration` on the entity **before** it enters
the world (the NBT is already built pre-spawn in `spawnPart`/`spawnText`), and confirm
`interpolation_duration = 0` + `teleport_duration = 0` are honored on the spawn packet (they are set
in `baseNbt`, [:308-311](../../../src/main/java/com/customblocks/buzzergame/TimerDisplayVisual.java#L308-L311)).
If a snap remains, seed a `start_interpolation`/`Transformation` so frame 0 already shows the final
pose. Low priority — cosmetic.

**Files.** `TimerDisplayVisual.java` (spawn NBT).

**Verify.** C5 — place a stand, no visible gap/snap.

---

## I7 — Mode hotbar is a long jargon sentence

**Symptom.** Switching wand mode prints a full instructional sentence to the action bar; not
glanceable and looks bad.

**Root cause.** `cycleMode()` sends `"Wand mode: " + label + " — " + hintFor(mode)` to the action bar,
and `hintFor()` is a full how-to sentence
([BuzzerGameWand.java:105-115](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameWand.java#L105-L115),
[:140-147](../../../src/main/java/com/customblocks/buzzergame/BuzzerGameWand.java#L140-L147)).
`Chat.tool` routes to the action bar ([Chat.java:167-169](../../../src/main/java/com/customblocks/command/Chat.java#L167-L169)).

**Fix.** Action bar shows a short, glanceable label only — a glyph + one word per mode, e.g.
`🔗 Link` / `⤢ Resize` / `⟳ Rotate` / `# Digits` (glyph set TBD with the owner, keep to the
red/black/lime brand). Move the how-to hint to a single chat line on switch (or drop it — the modes
are self-evident once labelled). Keep the anti-strobe gate.

**Files.** `BuzzerGameWand.java` (cycleMode + a short-label map).

**Verify.** F — cycle modes; the bar reads clean at a glance.

---

## I8 — G3 reset sound inaudible; all three want to sound cooler

**Symptom.** G1/G2 pass but are plain; G3 (reset) is barely hearable.

**Root cause.** G3 is a single thin, quiet sound: `BLOCK_WOODEN_BUTTON_CLICK_OFF` @ vol 0.8
([BuzzerBlock.fxReset:197-201](../../../src/main/java/com/customblocks/buzzergame/BuzzerBlock.java#L197-L201)).
G1/G2 are two-layer note-block combos but unremarkable
([fxStart:181-186](../../../src/main/java/com/customblocks/buzzergame/BuzzerBlock.java#L181-L186),
[fxStop:189-194](../../../src/main/java/com/customblocks/buzzergame/BuzzerBlock.java#L189-L194)).
Constraint: vanilla sounds + particles only (true custom particles need a client mod — design lock).

**Fix.** Rework all three into layered, distinct, clearly-audible combos (final picks to be tuned by
ear, starting proposals):
- **G1 start** — bright rising: PLING(1.5) + BELL(2.0), add a short `ITEM_TRIDENT_RETURN` swell +
  the green HAPPY_VILLAGER burst.
- **G2 stop** — hard lock: BASEDRUM(0.9) + HAT(1.7), add `BLOCK_ANVIL_LAND` (low vol) for a metallic
  "clunk" + the bigger CRIT burst.
- **G3 reset** — soft but audible: `BLOCK_NOTE_BLOCK_HAT`(1.2) + `ENTITY_ITEM_PICKUP` @ **1.0** (not
  0.8) + the CLOUD puff. Louder and clearer than the current button click.

Keep the note-block `.value()` rule (only `BLOCK_NOTE_BLOCK_*` needs `.value()`; every other
SoundEvents constant is bare — see the existing comment [BuzzerBlock.java:177-178](../../../src/main/java/com/customblocks/buzzergame/BuzzerBlock.java#L177-L178)).

**Files.** `BuzzerBlock.java` (fxStart/fxStop/fxReset).

**Verify.** G1/G2/G3 — each clearly audible and clearly different.

---

## Suggested build order (one-by-one, owner tests between)

1. **I3** press-start (pure logic, no assets) — unblocks solo testing.
2. **I2** glue geometry (constants only) — immediate visual win, owner confirms values.
3. **I1** Path-B baked labels (new font + PNGs + lineText) — the big one.
4. **I4 + I5** hitbox/outline + stand hardness (shared change).
5. **I7** hotbar labels.
6. **I8** sound rework.
7. **I6** place-flicker polish (last, cosmetic).

Each step: build the jar, then update the testing guide status + PROGRESS_LOG per the standing doc
rule; keep the group spec clean.
