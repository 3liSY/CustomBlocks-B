# Finale Fix — Progress Log

> Scope: this log documents **only the fixes actually implemented** in the Finale Fix groups
> (Phase 17 — GUI Restoration & Legacy Recovery, Groups 01–25). It is not the whole-project log
> (see the root `PROGRESS_LOG.md` for that). One entry per group that has had fixes landed.
> Newest at the top.

---

## Group 11 · Categories — multi-membership rework, §A built in one pass · 2026-07-25 (🟢 build-green — NOT confirmed in-game)

> Built from the (now consumed) `G11_REWORK_HANDOFF.md` against G11 §B and TG11 §A rows A1–A11.
> Commands only — the chest category menus keep their single-category view, per G11's "commands
> only until G27 Screens exist" decision. Jar builds green (JDK 21); nothing confirmed in-game.

### 1. Membership is a set, not a word on the block
- New `core/CategoryMembershipStore.java`: `blockId → LinkedHashSet<categoryKey>`, persisted to
  `config/customblocks/data/category_membership.json` (atomic write, same shape as the metadata store).
- `uncategorized` is a built-in floor key: `of()` never returns an empty set, `remove()` refuses to
  strip the floor, and dropping a block's last real category lands it there instead of erroring (A2).
- `blocksIn()` skips ids that no longer resolve rather than deleting their rows — a trashed block
  keeps its memberships so `/cb trash restore` is lossless, and a stale row can't surface as a phantom.
- Reid follows: `SlotManager.reId` now calls `renameBlockId` alongside the other id-keyed migrations.

### 2. The key normalizer folds separators
- `key()` = lower-case + every run of spaces/hyphens/underscores folded to one space, shared by both
  stores. This is what makes `arabic letters` resolve `Arabic Letters` **and** makes `arabic-letters`
  a key collision rather than a silent second category (A7). Plain lower-casing could not do both.
- Collision vs. resolve is decided by `collisionFor()`: same key + case-only difference resolves;
  same key + any other difference is refused, naming the existing category.

### 3. The legacy field became a display shadow (owner decision, 2026-07-25)
- `SlotData.category` stays and is restamped on every membership change with the alphabetically-first
  real membership by typed name (`""` when only the floor). Derived, never chosen, never read back as
  truth — so it is not a stored main. It keeps the HUD, Arabic chest menus, exports, blueprint lore,
  Bulk Workbench and the Category Hub readable until G27 moves them onto the membership store.
- Dual-write the other way too: `SlotManager.setCategory` (and `createNoSave`) mirror into the
  membership store with replace semantics, so studio create, template apply, ZIP/vault import, trash
  restore and the Arabic bootstrap all land in the new model without each being reworked.
- **Lock order matters here.** A mutator holds the store's monitor only while editing the map, and
  shadows *after* releasing it — `SlotManager.createNoSave` already runs the other way round
  (SlotManager's monitor held while calling into the store), so shadowing inside the lock would close
  that cycle into a deadlock. There is a banner over the mutation section saying so.

### 4. Undo got `Kind.CATEGORY` — it genuinely doesn't fit MODIFY
- Membership isn't on `SlotData`, so a before/after snapshot pair could only restore the shadow: undo
  would *look* like it worked while the real memberships stayed changed. New `Membership(id, before,
  after)` payload carries both whole sets; `HistoryCommands` restores via `restoreSet`.
- Without this, `/cb setcategory` and `/cb bulkcategory` would have silently lost the undo they have
  today — a regression, not a gap. `Op` gained a 10th field with back-compat constructors, so no
  existing caller changed.

### 5. Commands
- `/cb setcategory <id> <cat>…` **adds** memberships now, several at a time; multi-word names are
  quoted (`"Arabic Letters"`) or the tokenizer would read them as two. `none` still clears (A1).
- New `/cb category create|set|remove`; `delete <name> [category|exclusive|move <target>]` — bare
  delete keeps today's strip-the-category meaning (A3), `exclusive` destroys only blocks with no other
  membership behind `/cb confirm` as one undo batch (A4), `move` re-files first (A5).
- `filter` replaced `sort`: `/cb category filter <cat> <mode>` orders blocks (A8), `/cb category list
  <mode>` orders the category listing — `filter` had to keep A8's literal syntax, so the category half
  needed its own home. Default is alphabetical for both.
- `info <name> [list]` (A11); `give` now hands out every block holding the category as any membership (A10).
- Tab-complete reads the real category records, so an empty category still completes (A9).
- `/cb bulkcategory` made additive to match `setcategory` (owner call — the same word meaning "replace"
  in bulk and "add" singly was the confusing option).

### 6. First-load conversion
- `runFirstLoadConversionIfNeeded()` runs right after `SlotManager.loadAll()`. No backup, no confirm,
  no report — the owner confirmed no category data is in use. Converts any stray legacy word (the 224
  bundled Arabic blocks, mainly) and always writes the file, which is its own "already ran" marker.

### 7. Line gate fallout
- `HistoryCommands.java` hit 414 lines and failed `monolithGate` on the first jar build. Its pure
  formatting half (`describe`/`diff` + helpers) moved to `HistoryDescribe.java`; no behaviour change.

### Files touched
- New: `core/CategoryMembershipStore.java`, `core/CategoryFilters.java`,
  `command/handlers/CategoryMemberCommands.java`, `command/handlers/HistoryDescribe.java`
- Reworked: `core/CategoryService.java`, `core/CategoryMetadataStore.java`, `core/SlotManager.java`,
  `core/UndoManager.java`, `command/handlers/CategoryCommands.java`,
  `command/handlers/AttributeCommands.java`, `command/handlers/BulkCategoryCommands.java`,
  `command/handlers/HistoryCommands.java`, `CustomBlocksMod.java`

### Status
Jar builds green (JDK 21), every gate passing. TG11 §A → `Built 🎯`, all A1–A11 cells left at `🎯`.
NOT confirmed in-game.

### Still open
- Export/import still carries one category per block in code. The **decision** was settled 2026-07-30
  (local export carries every membership, one format only — TG12 §A9); the build has not happened.
  Remote share payloads stay `Discussion ✏️` in TG20 §K.
- Chest category menus untouched; the display shadow is what keeps them readable until G27.

---

## Group 34 · Wheel of Fortune — pre-test tweak pass: vivid+solid face, prize out front, claimable · 2026-07-23 (🟢 build-green — NOT confirmed in-game)

> Owner tweak pass before first in-game test, from two screenshots (drab hollow wheel vs. a vivid filled reference).

### 1. Wedge colours — vivid + more of them
- `WheelRing.WEDGE_COLORS` 4 → **10** saturated concretes (`BLUE, LIME, MAGENTA, ORANGE, LIGHT_BLUE, RED, PURPLE, GREEN, PINK, CYAN`), warm/cool alternating so no two neighbours muddy. 100 ÷ 10 = clean seam. Old set read dark-red / olive-yellow / teal (muddy).

### 2. Solid filled face (no transparent hole behind the arrow)
- Wedges now span centre→rim instead of a hollow `INNER_R=6` band: added `WEDGE_INNER_R=0.5` + `WEDGE_CENTER_R`; `WEDGE_LEN` measures from the 0.5 stub. The stub hides under the arrow pivot.
- `WEDGE_WIDTH` now measured at `OUTER_R` (was `ICON_R`), so slices tile edge-to-edge at the rim (no sky gaps) and overlap inward → gap-free disc. Icon ring radius unchanged.

### 3. Prize moved off the arrow + claimable (overrides the item-F show-only lock)
- `DEPTH_POPUP` 1.10 → **2.00**: the win popup floats 2 blocks out front, clear of the in-plane arrow.
- New `Part.CLAIM` hitbox over the popup (`popupIconPos` shared by icon + box). **Right-click** the prize claims it — middle-click can't (creative-only, client-side, no server packet).
- Claim rules: only the spinner (`spinner` UUID recorded in `startSpin(world, player)`), **1 item**, **once** (`prizeClaimed`); popup stays until next spin. Both persisted in NBT.
- Guarded two regressions the front box introduces: dead-centre right-click still **spins** when no prize is showing (`hasPrize()` fall-through); left-click still **removes** the wheel (CLAIM counts as hub, only RIM left-clicks are redirected).

### Files touched
- `wheel/WheelRing.java`, `wheel/WheelDisplayVisual.java`, `wheel/WheelBlockEntity.java`, `wheel/WheelBlock.java`

### Status
Compiles + jar builds green (JDK 21). New TG §G (claim) + §A5/§D/§F4 rows added. NOT confirmed in-game.

---

## Group 34 · Wheel of Fortune — v2 realism rebuild, §A–§F built in one pass · 2026-07-23 (🟢 build-green — NOT confirmed in-game)

> Full rewrite of the `wheel/` package to the v2 spec locked in the owner's 30-question design pass. The v1
> flat wool disc, its pre-picked-then-faked landing, and the single-reel popup are all gone. Build-green only:
> `gradlew build` with every gate green, `customblocks-1.0.0.jar` produced. Nothing confirmed in-game yet.

- **§A · Structure — real blocks replaced by display entities.** `WheelRing` no longer places wool; it now owns
  the wheel's geometry (a vertical plane spanned by world-up and one horizontal axis, expressed in the display
  entities' own local frame at the placer's yaw) plus the two entity layers: 100 alternating carnival wedges
  (concrete block items squashed into thin radial bars, aimed with a single quaternion about local Z) and 100
  prize icons drawn in their `gui` inventory look on top. `WheelBlock` is now an INVISIBLE, no-collision anchor
  with no `FACING` blockstate property — facing is a precise yaw float on the BlockEntity, not four cardinals,
  and the old property never matched the single-variant blockstate json anyway. **224 display entities per
  wheel** (100 wedges + 100 icons + arrow + 2 popup + 21 click surfaces); only the arrow and popup are pushed
  per tick, wedges are spawned once and never touched.

- **§A · Removal is a tag sweep, not just handle discards.** Because the anchor is invisible and the body is
  entities, a lost UUID handle used to mean permanent debris. Every wheel entity now carries a `cb_wheel`
  command tag and `WheelDisplayVisual.despawnAll` discards the known handles *and* sweeps a 52-block box for
  the tag, so a `/kill`, a crash, or an older build can't leave anything standing. `ensureBuilt` also probes
  that the arrow entity still resolves — `complete()` only proves the handles were recorded, not that the
  entities are alive, so a killed wheel now rebuilds on the next spin instead of staying invisible.

- **§B/§C · Honest landing, by construction.** The spin draws a target slice uniformly at random, then lays an
  easing curve (`1-(1-p)^3` over 160 ticks, 8–12 random full turns) that ends exactly on that slice's centre
  angle. On landing the winner is **read back out of the arrow's final angle** via `WheelRing.sliceAt` — there
  is no separate `winner` field the animation could disagree with, which is what made v1 dishonest. Icons are
  re-rolled from the cached pool every spin by partial Fisher-Yates (no repeats within a spin), so the 100-icon
  perf cap never blocks any of the ~1300 pool items from appearing.

- **§B · Clicking a wall of display entities.** Display entities aren't clickable, so the wheel spawns 21
  INTERACTION surfaces (one 4×4 on the centre pivot, 20 around the rim) tagged with the owning anchor's pos.
  `WheelBlock.onWheelInteract` is wired to Fabric's Use/AttackEntityCallback *after* the BuzzerGame listeners,
  which PASS on anything that isn't a stand part. Right-click any surface spins; left-click the centre takes
  the wheel down and returns the item — needed because the centre INTERACTION covers the invisible anchor, so
  the block itself can no longer be aimed at.

- **§D · Popup animation.** Giant `ITEM_DISPLAY` icon on a centre billboard with a per-tick quaternion about
  local Y (spins inside the camera-facing frame, so D1 "slow 3D turn" and D4 "keeps facing you" both hold), plus
  a `TEXT_DISPLAY` banner glowing in brand lime via `Glowing` + `glow_color_override`. Pop-in is a back-out
  overshoot curve pushed every other tick with `interpolation_duration=2` filling the gap.

- **§E · Audio tracks the curve, not a timer.** `WheelFx` takes a 0–1 speed factor taken from the derivative of
  the same easing curve that drives the arrow, so the whir's pitch/volume and the peg-clacks can't drift out of
  sync with what the eye sees. Clacks fire on peg crossings capped at one per tick: a rattle at full speed that
  spaces itself into single clacks as the arrow settles. Landing layers bell + fanfare + firework + level-up.

- **§F · Commands trimmed.** `/cb wheel` now gives the item (v1's `place` and `spin` leaves are gone — the wheel
  is placed like a block so it can face the placer, and spun by right-click; the command backup stays deferred
  scope). `/cb wheel list` reports the pool. `WheelPool` is unchanged — the existing denylist + spawn-egg filter
  already met the §F contract. Show-only is preserved: the mod never hands the landed item to anyone.

- **Build-gate note.** `verify.gradle`'s `tgGate` matched `Group_*_Testing_Guide.md`, but the testing guides are
  named `Testing_Guide_NN.md`, so the gate could match no file and would fail every build once its marker
  existed. Regex widened to accept the current naming.

---

## Group 08 · Shapes — §F defects F1/F2/F3 fixed (from the 2026-07-22 MP pass) · 2026-07-22 (🟢 build-green — NOT confirmed in-game)

> One jar for the three MP-pass defects in the §J/§B fix plan (group doc §F). §L (F4, vanilla parity)
> stays parked. Build-green only: `compileJava` + every gate green, `customblocks-1.0.0.jar` produced.
> Owner in-game MP confirmation still owed for all three (F3 also needs a facing eyeball — see below).

- **F1 · upside-down stair corners picked the mirror wedge (TG8 J8/J9, 2nd regression).** Cause:
  `StairConnection.compute` classifies the corner (`INNER_LEFT`/`RIGHT`, `OUTER_LEFT`/`RIGHT`) in the base
  NORTH/bottom frame, but `BlockShapes.orient` renders a TOP half as x:180 + y:180 = 180° about Z — which
  preserves Z (front/back = inner vs outer) and mirrors only left↔right. The 2026-07-21 J8 fix only verified
  the left/right-symmetric STRAIGHT stair, so the asymmetric corner wedges landed on the wrong side upside-down.
  Fix: new `topSwap(shape, half)` swaps LEFT↔RIGHT of the chosen corner when `half == TOP` (STRAIGHT passes
  through); front/back detection + the `isDifferentOrientation` guard untouched (Z preserved). Collision reads
  the same stored `StairShape` through the same `orient`, so hitbox == visual for free — no separate branch.

- **F2 · Omni-Tool face rotation did nothing on a shaped block, MP only (TG8 B5).** Cause: `rotateFace`
  (and the undo/redo `FACE_ROTATE` cases in `HistoryCommands`) pushed the pack (`ResourcePackServer.updatePack`)
  but never `HudSync.broadcast`. A full cube bakes rotation into the pushed pack model, so it updated; a §B
  shaped block reads rotation only from the synced packed value (`ClientSlotCache.rot` → `resolveFaceRot`),
  which the pack push never refreshes → the remote client re-meshed with the stale rot. SP worked (reads
  `FaceRotations.packed` in-process). Fix: `HudSync.broadcast(server)` after the rotate and after each
  undo/redo `FACE_ROTATE` restore — `HudSync` already carries the packed rot (line ~94), and the changed
  `ClientSlotCache.rot` makes `geometryDiffers()` trigger the world re-mesh.

- **F3 · plain shaped stair item icon showed the wrong angle (TG8 B6).** Cause: `SlotItemRenderer`/`ShapeIconMesh`
  draw the icon in the base NORTH/bottom frame; the `builtin/entity` display transforms already equal vanilla,
  so scale is right but the presented face is the tall back, not the step. Fix: for a DIRECTIONAL shape (stairs),
  ICON PATH ONLY, pre-rotate the geometry to a fixed presentation facing via the shared `BlockShapes.orientPoint`
  (+ a matching normal rotation), bottom half. Shipped with **`Direction.EAST`** as the first guess per the fix
  plan — the item layer is no-cull so no face drops; the rotation only re-aims the step + lighting. **Needs a
  visual check:** if the step still faces away in-game, change `iconFacing` in `SlotItemRenderer` to
  `Direction.SOUTH`. The placed mesh + hitbox are never touched.

---

## Group 31 · BuzzerGame — third-pass regression fixes (Steps 1–6) · 2026-07-19 (🟢 build-green — NOT confirmed in-game)

> The 2026-07-19 retest found the second pass's screen/hitbox/break/flicker fixes did not hold; re-root-caused
> into `docs/testing/extra/G31_FIX_PLAN.md` + the Group doc's third-pass table. All six agreed steps landed in
> one build. Verified here: both bake tools re-run and their PNGs eyeballed (labels ≤256px + green; LED sheet
> tinted-green preview shows uniform 7-segment + ghost-8 + bloom), the jar packs the new assets, `compileJava`
> + every build gate (monolith/help/hotbar/chat/tg) green. In-game confirmation still owed.

- **Step 1 · label tofu (I1).** Cause: `BakeTimerLabels` baked at `FONT_SIZE=240` → PNGs 351px/360px wide,
  past MC's 256px font-atlas page, so the glyph was dropped and rendered as a tofu box. Fix: the baker now
  AUTO-FITS the font size down (stroke weight scaled with it to keep the approved cursive weight) until the
  widest word + the shared canvas both fit ≤250px → 243×141 / 249×141. Also baked GREEN (locked brand
  `#15FF00`), and `TimerDisplayVisual.lineText` renders the label UNTINTED (white style over the green PNG) so
  `/textcolor` recolors only the LED digits, never the locked-green label. `font/timer_label.json` height/ascent
  moved 10/7 → 11/8 from the bake print (both PNGs share one vertical box → one baseline with the LED line).
  Also reordered `lineText` to the locked RTL layout: LED number LEFT, blinking colon, Arabic label RIGHT
  (`5.00 : الهدف`) — all characters are bidi-neutral/L so MC's bidi keeps code order = visual order.

- **Step 2 · text floated off the tilted glass (I2).** Cause: the forward offset was XZ-only and the two lines
  separated along world-Y, so on the −22.5° face the text drifted off. Fix: `textNbt` now offsets along the
  TRUE face-normal (horizontal × cos(pitch), vertical = −sin(pitch)) and separates the two lines along the
  tilted local-up axis (`f·sin(pitch)`, `cos(pitch)`, …), both derived from the per-face pitch + facing.

- **Step 3 · can't select/resize/rotate/break parts (I4/I5).** Cause: the block's voxel is clamped to the
  0–16px cell, so it never matched the floating/tilted/scaled parts; the invisible block also gave no crack
  feedback → break read as slow. Fix: three server-side INTERACTION entities (base/leg/screen) sized to each
  rendered band via `partBandsPx`, re-fit every resize/rotate, carrying command tags (stand pos + part) that
  survive reload. New `TimerHitbox` owns them (split out to keep `TimerDisplayVisual` under the 500-line gate).
  The stand block's outline is now empty; the wand routes clicks on the part entities through
  `onPartInteract` (Fabric Use/AttackEntityCallback in `CustomBlocksMod`): RC = link/grow/rotate, sneak-RC =
  select THAT exact part, left-click any part = break the whole stand instantly (`world.breakBlock`), sneak-LC
  in Resize = shrink. Overlap → MC's entity pick makes the nearest-crosshair part win. `size/textcolor/textsize`
  now find the stand by raycasting these entities (the block has no outline to raycast).

- **Step 4 · cooler LED.** `led_digits.png` redrawn procedurally (new `tools/BakeLedFont.java`) from one shared
  7-segment map so all ten digits are uniform (the old malformed "5" top bar is fixed). Each glyph bakes three
  layers that all tint with the TEXT_DISPLAY colour, so ghost + bloom follow `/textcolor` with ZERO extra
  entities: GHOST = all seven segments in dark grey (→ faint textcolor "ghost-8" behind the lit digit), BLOOM =
  a blurred grey halo of the lit segments (→ soft glow), LIT = crisp white (→ full textcolor). Dimensions
  unchanged so `font/led.json` still lines up. The colon blinks ~1 Hz via a `blink` flag on `Render` (world
  time `% 20 < 10`) that recolors just the `:` between bright textcolor and a dim shade — same char, no jitter.
  Glass stays plain dark (no scanlines). Bloom is baked into the font rather than a separate backing
  TEXT_DISPLAY layer, to avoid a per-stand entity explosion and to make it follow `/textcolor`.

- **Step 5 · defaults / bounds / digit-mode.** Resize bounds widened `SCALE_MIN/MAX` 0.4–3.0 → 0.1–5.0, step
  0.15 → 0.1; the `size <scale>` arg bound widened to 0.1–5.0. New persisted spawn default
  `CustomBlocksConfig.timerDefaultScale` (shipped ×1.3, clamped 0.1–5.0) drives a freshly placed stand's scale;
  `/cb buzzergame size <n> setdefault` saves it (and applies to the aimed-at stand). The plain-text digit style
  is dropped: the wand's DIGITS mode + the LED↔plain toggle are gone (modes are Link/Resize/Rotate), `Render`
  is always LED, `TimerDisplayBlockEntity.ledDigits` removed. Text-size still accepts the 30× arg but the
  on-screen glyph is capped (`TEXT_SCALE_ONSCREEN_MAX`) so digits never overflow the glass.

- **Step 6 · hotbar label (I7/F7).** Mode cycling already shows a short glyph+word action-bar label
  (`⛓ Link` / `⤢ Resize` / `⟳ Rotate`) with the how-to on one chat line; dropping the DIGITS mode leaves the
  three clean labels. Rotate is now 45° steps (8 facings), yaw-only, whole stand.

---

## Group 31 · BuzzerGame — C→G regression batch (I1–I8) · 2026-07-18 (🟢 build-green — NOT confirmed in-game)

> The 2026-07-18 in-game pass root-caused eight regressions/fails across C/E/F/G (see
> `docs/testing/extra/G31_FIX_PLAN.md`). All eight are now implemented in one pass; the owner batch-tests
> C→G. Verified here: I1 baked-glyph shaping eyeballed on the rendered PNG (correct join/direction), I3 the
> real `BuzzerSession` driven through both press cycles headlessly (all-pass), the jar packs the new font +
> PNGs, and every G04 build gate stays green. I2 glue geometry may want one owner tuning round.

- **I3 · linked buzzer wouldn't start on press (D blocked).** Cause: `onBuzz` had no IDLE case — only
  `armTarget` (the `start` command) moved IDLE→ARMED, so a bare press hit the `default → IGNORED` arm.
  Fix: added IDLE→START (`state=RUNNING`, `elapsedTicks=0`, a new `targetless` flag). A `targetless` run
  shows no هدف line (`hasTarget()` returns false) and press 3 lands back in IDLE, not ARMED; the armed
  (`start`) flow is unchanged (press 3 → ARMED, هدف kept). `targetless` is cleared by `armTarget`/`clearRound`.
  Drove the compiled class through both cycles headlessly (`tools/VerifyI3.java`): all assertions pass.

- **I2 · screen text detached from the glass (massive regression).** Cause: two placeholder constants in
  `TimerDisplayVisual` — `TEXT_FORWARD = 0.34` pushed the line to ~z+0.84, 0.36 block SOUTH of the glass
  (front face at z+0.482); and `SCREEN_PITCH = +22.5` tilted the text plane the wrong way vs the model's
  −22.5° screen. Fix: `TEXT_FORWARD → 0.02` (sit on the glass), `SCREEN_PITCH → −22.5` (front −22.5 / back
  +22.5). Geometry-only; may want one in-game tuning round on the `*_LINE_UP` offsets.

- **I1 · Arabic read L→R, number garbled, one tofu box.** Cause: the labels were stored as pre-shaped
  presentation-form constants drawn as MC text; MC re-runs Unicode bidi on every rendered line, reversing the
  already-visual glyphs (→ reads left-to-right) and dragging the neutral `.`/separator around the RTL run,
  fragmenting the LED digits and leaving a neutral char glyph-less. Fix (Path B, owner-locked): baked
  `الهدف`/`النتيجة` from their LOGICAL strings via Java2D `TextLayout` (RTL, `arabtype.ttf`, the same cursive
  recipe as `ArabicWordRenderer`) into two WHITE transparent-bg PNGs (`tools/BakeTimerLabels.java`), served by
  a new `customblocks:timer_label` bitmap font (`font/timer_label.json`) on bidi-neutral PUA codepoints
  (U+E000 target / U+E001 result). `lineText` now emits the single PUA glyph in that font + the number in the
  LED font — no strong-RTL run, so nothing reorders; white ink still tints with `textcolor`. Shaping/join/
  direction eyeballed on the baked PNGs before shipping.

- **I4 · stand hitbox inaccurate; parts hard to select/resize.** Cause: `shapeFor` returned one fat hollow
  cuboid (full-width over the whole height) built off the WHOLE scale only, and the wand's select bands
  (0.34/0.67) didn't match the model bands. Fix: `shapeFor` now unions the real base/leg/screen columns, each
  tracking whole + per-part scale + the stacking lift via a new shared `TimerDisplayVisual.partBandsPx`; the
  wand's `selectPart` reads the SAME px bands (base 0–2.5, leg 2.5–9, screen 8–16 at scale 1) so the highlight
  box, the click-to-select bands, and the visible stand can never drift apart. Collision uses the base column
  only (don't snag the screen).

- **I5 · stand break felt slow/bugged (buzzer instant).** Cause: identical `strength(0.2f)` on both blocks,
  but the stand is INVISIBLE (its look is display entities) so there's no crack-overlay to sell a 0.2 mine,
  and the old oversized hollow box meant you were often mining empty air. Fix: tightened the outline (shared
  with I4) + dropped the stand to `strength(0.05f, 6.0f)` — a true one-hit, reading as instant like the buzzer.

- **I7 · mode hotbar was a long jargon sentence.** Cause: `cycleMode` sent `"Wand mode: " + label + " — " +
  hintFor(mode)` (a full how-to) to the action bar. Fix: the action bar now shows a short, glanceable label —
  a BMP glyph (unifont-covered, won't tofu like an astral emoji) + one word: `⛓ Link` / `⤢ Resize` /
  `⟳ Rotate` / `# Digits`; the how-to moved to one dim chat line on switch (routed through `CbFmt` per the
  colour gate, not raw §).

- **I8 · G3 reset inaudible; all three FX plain.** Cause: G3 was a lone quiet `WOODEN_BUTTON_CLICK_OFF`@0.8.
  Fix: reworked all three into layered combos — START adds an `ITEM_TRIDENT_RETURN` swell, STOP adds a low
  `BLOCK_ANVIL_LAND` clunk, RESET is now `NOTE_BLOCK_HAT`@1.2 + `ENTITY_ITEM_PICKUP`@vol 1.0 + cloud puff
  (louder, clearly audible, clearly different). Kept the `.value()` rule (only `BLOCK_NOTE_BLOCK_*`).

- **I6 · one-tick place flicker (cosmetic).** Cause: a freshly spawned display entity can render its default
  pose for frame 0 before the NBT transform applies client-side. Fix: the transform is seeded into the spawn
  NBT pre-spawn and `interpolation_duration`/`teleport_duration` are 0; added `start_interpolation = 0` so the
  final pose commits at tick 0. A residual 1-frame client-side snap can't be fully removed server-side (no
  client mod, by design) — needs the owner's eye to confirm.

---

## Group 03 · HUD Overlay — widget framework CUT, 3 widgets kept · 2026-07-14 (🟢 build-green — NOT confirmed in-game)

> First MP test run rejected most of §G03-2. This pass is pure deletion: no new behaviour, nothing to test
> except that the HUD went quiet and the three survivors still render. Redesign is a separate pass.

- **7 widgets → 3.** Kept `ACTIVE_TOOL`, `MACRO_BANNER`, `LOCKED_PADLOCK` (all three still pending a look
  redesign — the owner's word was "unprofessional", and that is tracked as bugs A1-a / A3-a in the TG, not
  fixed here). Cut the Buzzer timer and Vault toast, which were `stubBacked`: no backend ever existed for
  them, so they could only render invented numbers behind `-Dcustomblocks.devStubs=true` — i.e. they were
  untestable in a normal run, which is exactly what the owner hit. Cut the incidents ping, the favourites
  strip, the ESC quick-access panel, the Widgets tab, the per-widget colour picker, the op-force pin, and
  the never-built HUD-layout share code.

- **Deleted (7 files):** `DevStubs`, `HudWidget`, `HudWidgetStore`, `HudWidgetsScreen`, `WidgetColorPicker`,
  `QuickAccessPanel`, `GameMenuScreenMixin` (+ its `customblocks.mixins.json` entry). `HudWidget` went
  beyond the agreed list because with the store and the Widgets tab gone it was a per-player config holder
  with no config source left to fill it — the renderer now reads geometry and colour straight off
  `HudWidgetType`, which makes that enum the one place the HUD's look is decided.

- **`IncidentRecorder` deliberately NOT touched.** It looks like Group 03's, and it is not: ~20 command
  handlers write to it, plus `DiagReport`, `ReportMenu` and `AutoBackup`. What was cut is the HUD *ping*
  that surfaced it — a note-block blip on every logged incident. The recorder still records and
  `/cb incidents` still reads it; it simply never draws on screen again.

- **`WidgetSync` / `WidgetSyncPayload` / `WidgetSignals` survive, slimmed to 3 signals** (tool mode,
  macro-recording, locked-id set). They read like widget-framework plumbing but they are the server→client
  transport the *surviving* widgets render from — deleting them would have taken A1/A2/A3 dark. Dropped the
  incident, buzzer, vault, favourites and op-force fields. `/cb edithud force` is gone from `GuiCommands`.

- **ESC buttons unaffected.** The older, already-confirmed `EscMenuButtons` goes through
  `ScreenEvents.AFTER_INIT` + `ScreenInvoker`, a separate path from the deleted `GameMenuScreenMixin`.

---

## Group 08 · Shapes & Per-Face Textures — §3 faces-on-shapes + face↔shape bug fixed, §E bulkshape built · 2026-07-11 (🟢 build-green — NOT confirmed in-game)

> Two slices landed this pass; nothing ✅ until owner confirms in-game (CLAUDE.md §2). Safe, self-contained
> work only — directional placement/rotation (§G, core + disruptive) NOT touched, still ⏳ planned.

- **§3 per-face textures on non-full shapes → BUILT (fixes the 06-29 face↔shape sync bug at the same root).**
  The bug: paint a face, then `/cb setshape <id> slab` → the painted faces reverted to base. Root cause was
  NOT data loss — face PNGs are stored separately in `TextureStore` and were never wiped. The shaped-block
  model path in `ServerPackGenerator` simply never *looked* at per-face overrides: `shapeModelJson` hardcoded
  the base texture (`#all`) on every box face, unlike the full-cube path `cubeFacesJson` which already checks
  `TextureStore.hasFace` per face. So a shaped block always rendered the base texture, dropping the art.
- **Fix (mirrors the existing cube code, no new systems):** in `ServerPackGenerator.generate` the non-full-shape
  branch now writes each painted face PNG (like the `hasAnyFace` cube branch does) and calls
  `shapeModelJson(shape, i, key)`. `shapeModelJson` declares a `#<face>` texture var for every painted face
  (`base + "_" + face`), and `element(index, box)` points each face at `#<face>` when painted, else `#all`.
  `cross` (billboard) is skipped — single texture only. `full` blocks are unaffected (still `cubeFacesJson`).
- **§E `/cb bulkshape <filter> <shape>` → BUILT** in its own `BulkShapeCommands.java` (BulkCommands was at
  the 400-line handler cap — split per the existing one-file-per-bulk-op pattern; registered in
  `CommandRegistrar` beside the other bulk ops). Reuses the Group 07 rail: `BulkScope` filter → `/cb confirm`
  hold for big/"all" batches → ONE `UndoManager.recordBatch` of `Kind.SHAPE` children (so one `/cb undo`
  reverts the whole batch AND rebuilds the pack) → ONE debounced `ResourcePackServer.updatePack()`. Locked
  and already-that-shape blocks are skipped + reported. Tab-complete via new `BulkSuggestions.SHAPE_ARGS`.
- **Docs:** `testing/GROUP_08_TESTING_GUIDE.md` (§C rows C5/C6 for shaped faces + sync-bug; new §E section
  E1–E4; sections table + banner), this log.

---

## Group 08 · Shapes & Per-Face Textures — owner test round · 2026-06-29 (results + 1 new bug, no code this pass)

> Owner ran the G08 in-game batch. Results recorded in `testing/GROUP_08_TESTING_GUIDE.md`. Docs only — no code changed.

- **C · face textures (commands) → ✅ pass 4/4.** `setface` / `clearface` / `clearallfaces` all correct. Flagged for a
  future **polish** pass (owner note), not a re-test.
- **A · shape commands → ✅ pass (4/5).** A3 `clearshape`, A5 X-ray (no see-through holes) confirmed, on top of the
  06-28 A1/A2/A6. **A4 `shapelist` → ⏳ rework:** flat chat list retired; folds into the **G02 chest-GUI** as a
  clickable, improved menu.
- **⚠️ NEW BUG — face↔shape sync.** Paint a face (`setface` *or* `facechangegui`), then `setshape <id> slab` → the
  painted faces **revert to base texture**. The shape swap drops per-face overrides. Faces alone ✅, shapes alone ✅,
  but face→then→shape loses the art. **Open — fix pending** (likely: `setshape` path must carry/reapply the slot's
  per-face overrides into the shaped model build). Not yet diagnosed in code.
- **B · shape editor GUI → 🟡 partial (2/3).** Clicking shapes works (B2 stairs, B3 reset). **B1 open:** the editor
  **screen preview is stuck on "loading"** — never renders the target block (broken bug); and the whole editor screen
  shell must be **synced to the Screens (G27)** fixes/implementations landing later.
- **D · face editor GUI (`facechangegui`) → ❌ not passed, entire rework.** Will be rebuilt to match + sync with the
  new **G02 chest-GUI** pattern (same destination as A4). Old D rows parked in the guide for reference.

**Open follow-ups (dev):** (1) fix face↔shape sync · (2) fold A4 shapelist + D face-GUI into G02 chest-gui ·
(3) fix B screen "loading" preview + sync editor screen to G27. Nothing owner-testable until those land.
**Docs touched:** `testing/GROUP_08_TESTING_GUIDE.md` (statuses + 2 callouts), this log.

---

## Group 20 · External Integrations — S1 vault block share BUILT · 2026-06-21 (🟢 build-green — NOT confirmed in-game)

> Started G20 (Cloud Vault & Discord), design locked 2026-06-21. Built **S1 — whole-block cloud share**:
> `/cb vault upload <id>` → share code, `/cb vault download <code>` → block restored, static **and**
> animated. `gradlew build` green (verifyFileSize + verifyMojibake + verifySound pass); fresh jar copied
> to `.minecraft\mods\`. **Nothing ✅ until the owner confirms in-game** (CLAUDE.md §2). Pace: one slice,
> hand back to test. Tests: `Reports/GROUP_20_TESTING_GUIDE.md` (S1 → 🟢 build-green).

- **Reality correction that shaped S1:** the spec said "reuse the `/category` zip route" (D3), but
  `BlockExporter.importCategoryZip` restores **definitions only** — it drops bundled textures and never
  carried `AnimData`. A faithful block round-trip (test ② texture, ③ animation) needs more than wiring.
- **New `cloud/VaultBlockCodec`:** packs ONE block into an in-memory ZIP — `block.json` (reuses
  `SlotDataStore`'s exact slots.json shape, incl. anim), `texture.png` (frame strip for animated),
  `source.src`, and any per-face overrides (`face_<f>.png`). `unpack()` allocates a fresh slot, applies
  every field + `setAnim`, writes the texture(s), and reports created/skipped/failed (S1 skips an
  existing id — the S2 conflict GUI handles that). The anim sidecar is rebuilt by `ServerPackGenerator`
  at pack-build, like a locally-made animated block (no `.mcmeta` carried).
- **`SlotDataStore`:** extracted the per-block serializer to public `toJsonObject(SlotData)` (the `save`
  loop now reuses it) and made `animFromJson` public — so the codec shares the on-disk format instead of
  duplicating it (rule #4: slot serialization still owned here).
- **`CloudCommands`:** real `vaultUpload` (OP-gated via `hasPermissionLevel(2)`; `cloudShareEnabled` +
  `vaultEndpoint` gates; off-thread pack→`uploadCategory`; success line keeps `[CB]` brand + `[copy code]`
  + a `/cb vault download` hint; **share juice** = `SoundFx`/`ParticleFx` "success" + on-screen
  `TitleS2CPacket`/`SubtitleS2CPacket`) and `vaultDownload` (off-thread `downloadCategory`→codec→
  `updatePack`, reports restore). Reuses the proven generic `uploadCategory`/`downloadCategory` HTTP.
- **Config:** added `cloudShareEnabled` (default true, D9/D11) to `CustomBlocksConfig` + the store.

**Files:** new `cloud/VaultBlockCodec.java`; edited `core/SlotDataStore.java`,
`command/handlers/CloudCommands.java`, `CustomBlocksConfig.java`, `CustomBlocksConfigStore.java`.
**Deferred within S1 (noted, not blockers):** the `vaultShareEffects` master toggle (share juice already
honors the success sound/particle category toggles); S2 conflict GUI owns the id-clash flow.
**NOT done** — needs the owner's in-game confirm. Worker must answer `POST /category` + `GET /category/<code>`
for the real round-trip; otherwise uploads fail gracefully (no crash).

---

## Group 18 · Lore (REVAMP v2) — CLOSED · 2026-06-21 (✅ confirmed in-game · Round 7 ⏳ deferred)

> Owner ran the testing guide in-game and confirmed **Rounds 1–6 pass**: add/edit/delete lines, On/Off
> toggle, colour **and** format codes (`&l` bold in any order) on the menu + item, restart-survival,
> `/cb note` alias, quick-add, clear, single close button (✕). Two fixes landed this pass and were
> confirmed: the colour-code ordering bug and the duplicate close-button. **Group 18 is closed.**
> **Round 7 (Share/Import) = ⏳ deferred** — it needs `vaultEndpoint`, which only exists once the
> **Discord + cloud-vault** work is built later; the "vault isn't set up yet" message is correct
> behaviour until then, not a bug. Round 7 steps stay parked in the guide, ready for that work.
> Docs only this pass: `Reports/GROUP_18_TESTING_GUIDE.md` status → CLOSED, Round 7 marked ⏳ deferred.

---

## Group 18 · Lore (REVAMP v2) — single-screen Lore BUILT · 2026-06-21 (🟢 build-green — NOT confirmed in-game)

> Built the **REVAMP v2** single-screen Lore feature (the 3-tab Book GUI below was superseded). All 5
> slices landed; `gradlew build` is green (verifyFileSize + verifyMojibake + verifySound pass) and the
> fresh jar is copied to `.minecraft\mods\`. **Nothing ✅ until the owner confirms in-game** (CLAUDE.md §2).
> Tests: `Reports/GROUP_18_TESTING_GUIDE.md` (now marked build-green, 5/5 built). Pace per owner: build all
> 5 to green, one in-game test pass.

- **R-S1 Data:** `NoteData` rewritten to `List<String> lines` + `boolean enabled` (≤28 lines, ≤50 chars/line
  = anvil cap); old `lore/todo/tooltip/tooltipEnabled` removed. `BlockNotesManager` auto-migrates on load:
  old `tooltip` line + old `lore` (split on `\n`) → `lines`; `enabled` = old `tooltipEnabled` OR had-lore;
  **old To-Do dropped** (only data loss — To-Do was cut). Legacy flat `{id:"text"}` → one line. `notes.json`
  now `{lines:[…],enabled:bool}`; share JSON uses the same shape (exportJson/importJson kept).
- **R-S2 Item hover:** extended the already-half-built single-tooltip sync chain to carry **all** lines:
  `SlotBlock` `CLIENT_TOOLTIP_RESOLVER`→`CLIENT_LORE_RESOLVER` (now `IntFunction<List<String>>`),
  `resolveTooltip`→`resolveLore`, `SlotItem.appendTooltip` loops the lines (gray-italic default, `&` colours
  override). `HudSync` syncs a `lore` JSON array (was `tip` string); `ClientSlotCache.Entry.tooltip`→`lore`
  (List); `CustomBlocksClient` resolver updated.
- **R-S3 Menu:** `NotesMenu` rewritten to a **single screen** (no tabs): line grid (left-click=edit anvil,
  right-click=delete), `+ Add line`, On/Off switch, Share, Done. **Deleted `LoreBook.java`** (writable-book
  flow gone) and all To-Do code. `GuiRouter` NOTES dispatch dropped the page arg; `EditorMenu` button
  relabeled Note→**Lore**.
- **R-S4 Commands:** `/cb lore <id>` primary + `/cb note <id>` **alias** (shared Brigadier subtree).
  `/cb lore <id> <text>` = **quick-add one line** (enables on first line); `/cb lore <id> clear` wipes +
  Off; `import` kept (confirm-before-overwrite via BulkConfirm). LoreBook harvest removed from `open`.
- **R-S5 Docs:** this entry + testing guide status (build-green, 5/5) + spec status + CHANGELOG.

**Files changed:** `core/NoteData.java`, `core/BlockNotesManager.java`, `block/SlotBlock.java`,
`network/HudSync.java`, `client/ClientSlotCache.java`, `client/CustomBlocksClient.java`,
`gui/chest/NotesMenu.java`, `gui/chest/GuiRouter.java`, `gui/chest/EditorMenu.java`,
`command/handlers/NoteCommands.java`, `command/CommandRegistrar.java`. **Deleted:** `gui/chest/LoreBook.java`.
**NOT done** — needs the owner's in-game confirm.

---

## Group 18 · Notes (Book GUI) — interview + design locked, 4-slice plan set · 2026-06-21 (📐 design only — no code · ⛔ SUPERSEDED by REVAMP v2 above)

> Owner wants Group 18 started. Surveyed the live note code first, ran a 3-round owner interview, locked
> scope + a 4-slice build order. **Nothing built.** Full spec: `GROUP_18_NOTES_STAGING.md` (🔒 Locked
> Decisions + Slice Plan); tests: `Reports/GROUP_18_TESTING_GUIDE.md`. Staging stays SCRAPPED (§A).

- **Reality corrections (read, not guessed):** note storage is **flat** today — `BlockNotesManager` is
  `Map<String,String>`, `notes.json` flat (one text per id). The 3-field model (Lore / To-Do / Tooltip) is new;
  old flat notes **auto-migrate into Lore**. **Anvil caps ~50 chars** (`AnvilPrompt.MAX_LENGTH`) so 500-char
  Lore can't use anvil → **writable book**. **No item-tooltip injection exists** (`SlotItem` only overrides
  `getName`); on-item hover (G18.4) needs the tooltip **synced to client** like names are
  (`SlotBlock.CLIENT_NAME_RESOLVER`) = heaviest piece. **`CloudVaultClient.upload()` is a stub** (Phase-14
  TODO) → Share must implement it (mirror `uploadCategory`). Notes are **ungated** today. `draft`/`publish`/
  `drafts` are still registered (`ManagementCommands.java:76-88`) = dead staging code to remove.
- **Decisions (owner interview):** Lore = writable book · To-Do = left-click toggle + right-click delete (≤20)
  · color codes `&`→`§` honored in GUI + on tooltip · Tooltip = text + explicit on/off toggle · item-hover
  tooltip **in scope** as its own slice · Share = **vault upload + import** round-trip, import = `/cb note
  import <id> <code>` with **confirm-before-overwrite** · **OP-only** · entry via `/cb note <id>` **+ a Notes
  button in the block Editor menu** · pace = **one slice at a time**, in-game confirm between each.
- **Slice plan:** S1 Lore core (new note model + migration + 3-tab shell + Lore writable book + legacy/clear +
  OP gate + Editor button + remove dead draft/publish) → S2 To-Do tab → S3 Tooltip tab + client sync +
  `SlotItem.appendTooltip` (heaviest) → S4 Share upload + import. G18.5 (restart persistence) verified
  throughout.

**Docs only this pass:** `GROUP_18_NOTES_STAGING.md` (added 🔒 Locked Decisions + Slice Plan; corrected the
input-method line to writable book), `Reports/GROUP_18_TESTING_GUIDE.md` (new — S1 detailed, S2–S4 queued).
**No source changed.** **NOT done** — this is a plan; each slice needs an in-game confirm once built.

---

## Group 17 · Command Regressions — interview + design locked, slice plan set · 2026-06-21 (📐 design only — no code)

> Owner wants Group 17 started. Surveyed the live command code first, ran a 3-round owner interview,
> and locked scope + semantics + a 3-slice build order. **Nothing built.** Full spec:
> `GROUP_17_REGRESSIONS.md` (Locked Decisions block); tests: `Reports/GROUP_17_TESTING_GUIDE.md`.

- **Reality corrections (read, not guessed):** the **search GUI already exists** — `/cb search <query>`
  routes players to a `Nav.Dest.SEARCH` chest (`UtilityCommands.searchGui/search`), so G17.9 is a
  verify-only, not a build. **Export is already extensive** (json/txt/csv/md/html/yaml/png/zip/vault +
  per-id) — the regression "align with G12" row is folded into the Issue 17.15 export-rework session,
  not touched here. `UndoManager` already has per-player/global stacks, `undoSize`, `clearPlayer`, and a
  `MutationLog` audit hook; only the multi-step command layer is missing. **`UtilityCommands` is at
  397/400 lines** → give-args needs a handler split first.
- **Decisions (owner interview):** pace = **one slice at a time**, in-game confirm between each.
  Slice 1 = `undo <N>`/`undo all`/`undo clear`(confirm)/`redo <N>`/`redo all`, **detailed value-bearing**
  multi-undo chat (`glow g17a 12→8`), over-count reverts what's there + reports real number, `undo clear`
  reuses the `BulkConfirm` hold and wipes undo+redo. Slice 2 = give `<amount>` (1–6400, self) +
  `<player>` (**OP/perm-2 only**, recipient notified, overflow → give-what-fits + "inventory full").
  Slice 3 = delete `#` (~6-block raycast → **whole definition**, custom slot blocks only, no confirm,
  undoable). **Deferred:** favorite-primary / unfavorite / recent → **Group 25** (G17 keeps G17.6–8 +
  G17.12 as regression checks); export alignment → Issue 17.15.
- **Slice plan:** 1 undo/redo (`HistoryCommands` + `UndoManager`) → 2 give (split give/search out of
  `UtilityCommands`, then add amount+player) → 3 delete `#` (`CreationCommands` + a server-raycast
  helper). Search GUI = verify-only between slices.

**Docs only this pass:** `GROUP_17_REGRESSIONS.md` (added Locked Decisions & Slice Plan; refined the
undo/redo, give, and delete-shorthand requirement sections), `Reports/GROUP_17_TESTING_GUIDE.md` (new —
slice-1 = 🎯 §1, 5 tests; search = verify-only; slices 2–3 + G25 items listed). **No source changed.**
**NOT done** — this is a plan; each slice needs an in-game confirm once built.

---

## Group 16 · Diagnostics / IT Chest — design locked, slice plan set · 2026-06-21 (📐 design only — no code)

> Owner wants Group 16 started. Surveyed the live code first: the spec was **partly stale**. Ran a 3-round
> Q&A and locked the slice-1 design + a 5-slice roadmap. **Nothing built.** Full spec:
> `GROUP_16_DIAGNOSTICS.md`; tests: `Reports/GROUP_16_TESTING_GUIDE.md`.

- **Reality corrections (read, not guessed):** the **mutation/history log already exists**
  (`MutationLog` + paginated `HistoryMenu`, fed by `UndoManager.record`); **`/cb confirm`/`/cb cancel`
  already exist** (`BulkConfirm`, 60s per-actor hold); `/cb diag` already opens a **3-row** `DiagMenu`
  (not the 6-row dashboard); `/cb incidents` is **text only** (`IncidentRecorder` stores time+context+error
  only — no player/blockId/severity); incidents are **already recorded at ~22 sites** (incl. the bad-URL
  download case → G16.3 data is free); the mod emits **zero particles anywhere**; server-side screenshot
  is impractical on a headless server.
- **Decisions:** screenshot **dropped**; particles = **add FX first, then toggle**; debug-log viewer
  **deferred** (tracked); auto-fix = **its own slice (2)**; old `DiagMenu` **replaced** by the 6-row IT
  Chest; **structured incidents pulled into slice 1** (all ~22 sites get block+player); health row = **all 5**
  best-effort (Network Sync = online count + pack SHA, no per-client lag); severity colour **auto-derived**
  (throwable/"fail|error"→red, "skip|warn"→yellow, else lime); `/cb incidents` **opens the dashboard**.
- **Slice plan:** 1 IT Chest dashboard + structured incident model → 2 auto-fix (re-download / restore-from-
  backup / editor) → 3 admin cmds (`/cb audit [player]`, `/cb cache` + clear, Generate Report) → 4 particles
  (FX then toggle) → 5 sounds toggle. Later: Debug Log viewer. Already done: mutation log, confirm/cancel.
- **Slice 1 locked design:** 6-row chest "IT Chest", `Nav.Dest.DIAG` routes here, `/cb diag` + `/cb incidents`
  open it. Row 1 = 5 health gauges (TPS/Registry/Network/Pack/Memory, colour-coded, exact-value hover);
  Rows 2–4 = incident wool (severity colour, newest first, hover time/player/action/error, click → block
  editor); Row 5 = last 9 `MutationLog` entries (click → editor); Row 6 = Refresh / Clear Incidents / Back /
  Close. Data model: `IncidentRecorder` gains severity+blockId+player, `incidents.json` schema extended,
  old entries degrade to "—", non-player sources log as "System".

**Docs only this pass:** `GROUP_16_DIAGNOSTICS.md` (rewritten — reality check + decisions + slice plan,
status-free), `Reports/GROUP_16_TESTING_GUIDE.md` (new — slice-1 = 🎯 §1, 6 tests). **No source changed.**
**NOT done** — this is a plan; slice 1 needs in-game confirmation once built.

---

## Group 13 · Arabic — retire static letters, one system (Issue 3 dedupe) · 2026-06-19 (🟢 Build A jar green + deployed — in-game pending)

> Owner decision: now that auto-join is his dream system, **delete the 144 static letter blocks** and keep
> auto-join as the ONLY letter system. Resolves the long-standing "Issue 3 dedupe" (three overlapping
> letter systems) by deletion instead of hiding. Locked scope (Q&A 2026-06-19): keep the 80 NUMBER blocks
> (Eastern A0-A9 + Western E0-E9 — never join, no auto-join equivalent); `/cb arabic letter` survives as the
> one command (gives auto-join); reclaim the ~144 freed slots; placed static letters → vanish (air) on
> reclaim (owner doesn't need them); strip the 144 letter PNGs from the jar (owner has copies on desktop).

**Key architecture finding (drove the build split):** placed blocks in the world are the registry `slot_N`
blocks, textured **by slot index** ([SlotManager.reId comment](../../src/main/java/com/customblocks/core/SlotManager.java#L139)).
So freeing + reusing a letter's slot index would make any placed copy show the *new* block's texture (a
wrong-block bug, not a clean break). → split into a safe Build A and a destructive Build B.

**Build A — safe (this jar, no world/slot risk):**
- `/cb arabic letter <name> [color] [count]` now gives the **auto-join** block (was the static-bundled
  giver). Added `[color]` + `[count]`; default black / 1. `ArabicCommands.giveLetter` rewritten to call
  `ArabicLetterBlock.stackFor(ch, color, -1, count)`; old bundled-slot path deleted.
- **`/cb arabic join` removed** — it was a dupe of `letter` after the redirect. `giveJoin` folded into
  `giveLetter`. (Owner: "remove join IF it's a dupe" — it is.)
- Creative tab **"Arabic Letters (Join)" → "Arabic Letters"** (`CustomBlocksMod.registerArabicJoinTab`).
- Boot no longer creates the 144 letters: `ArabicBlockRegistry.importArt` skips `Group.LETTER`
  (numbers-only now). Existing letters from prior boots still linger in slots.json until Build B deletes them.
- Dead imports trimmed (SlotBlock/SlotData/HudSync/ItemStack). compileJava + build green; jar deployed 22:41.

**Build B — destructive (NOT built yet):** delete the 144 letter SlotData, persist a "retired slot index"
set + a chunk-load handler that air-replaces any placed `slot_N` in that set (handles unloaded chunks),
reclaim the freed slots, strip the 144 letter PNGs from `assets/customblocks/arabic_art/`. Gated behind
Build A in-game confirmation.

---

## Group 13 · Arabic Round 3 — 6/6 CONFIRMED in-game; readable-back (C-full) un-mirror fix · 2026-06-19 (✅ all 6 pass — auto-join complete)

> Owner tested Round 3 in-game. **5 of 6 pass, confirmed:** join count 16→1 (R3.2), middle-click pick
> returns the real letter (R3.3), "Place letter blocks" gives joinable blocks + lore (R3.4), no stray edge
> lines (R3.1), OmniTool Arabic mode removed (R3.5). **R3.6 readable-back FAILED:** front read correctly
> (بت) but from behind the word was garble — order was right (ba right / ta left) but every glyph was
> horizontally **mirrored** (backwards letters). Screenshots provided.

- **Cause:** `ArabicLetterBlockEntityRenderer.drawBackFace` turned the back quad 180° about Y **and** set
  `flipU=true` — a double mirror. A 180° turn about the vertical axis already keeps a glyph readable from
  behind (like spinning a sign around); the extra `flipU` re-mirrored it into backwards garble. The
  left/right reading order is handled separately by the partner-letter swap in `ArabicJoinFlow`
  (`back(k) = front(N-1-k)`), so orientation only needed the turn. Confirmed correct by the front face,
  which uses `flipU=false` and reads right; the back is just that quad turned 180°, so it must match.
  Not caught earlier because the shared item-icon draw (`drawGlyphCube`) never shows its back face.
- **Fix:** `drawBackFace` now passes `flipU=false` (turned, not mirrored). One-line behaviour change;
  `flipU` param kept on `face()` for completeness (all callers now pass false). Data path was already
  correct — `backLetter`/`backForm` are written to NBT + the update packet, so the partner tile reaches
  the client; this was purely a back-face orientation bug. Brightness, partner-swap, forms all untouched.
- **Corners / 90° turns: SCRAPPED** (owner, 2026-06-19) — not building corner joining; rows stay straight,
  a turn starts a fresh word. Removed from the group spec + testing guide; noted in the auto-join handoff.
- Files: `client/render/ArabicLetterBlockEntityRenderer.java` (back-face flip + comments). Jar
  `customblocks-1.0.0.jar` rebuilt + deployed 20:49. ✅ **CONFIRMED in-game 2026-06-19** — back reads the
  word correctly from behind (lone + connected). Round 3 complete (6/6); auto-join feature done.

---

## Group 13 · Arabic — isolated floaters nudged DOWN a little (font path) · 2026-06-19 (🟢 jar green, deployed — in-game look pending)

> Owner: isolated letters drew **too high** in their tile. Wanted them lowered **a little** — *not*
> centred, *not* on the floor ("lower a little, not the ground"). Earlier tries (bbox-centre, centroid,
> floor-anchor, dal-only-130%) all rejected as "still floating" or "too far."

- **Two-tier downward nudge on the ISOLATED form only** (`ArabicTileRenderer.render`, right after the
  baseline translate). A glyph with a real tail/descender (waw/ra/zay/noon/meem/ya/jeem/ha/lam/ain/
  maqsura) is left where it sits; a **short** no-descender floater (dal/dhal/ta-marbuta/round-ha/hamza)
  drops `ISO_DROP = 0.09` of tile height; a **tall** no-descender letter (alef family) drops a tiny
  `ISO_TALL_DROP = 0.03`. Detector is shape-driven, not a letter list: `desc = ink below baseline`; if
  `desc > 2%` → leave; else short (`height < 62%`) → big nudge, tall → tiny nudge. Connected forms never
  enter this branch, so kashida bars stay on the baseline and seams still line up.
- **Decided by eye before building** (owner rule). Added `BeforeAfterPreview.isoDrop()` (same formula)
  + `EDGE_CASES.png` (alef/hamza/maqsura/ha/ta-marbuta/ain) + `WORD_EDGES.png` (each edge letter inside
  a real word where it lands isolated — `ذراع` shows dhal-drops / alef-tiny / ra-stays / ain-stays in one
  row). Owner approved the 9%/3% look.
- **Unrelated build break fixed to ship green.** `ArabicLetterItemRenderer` was calling a 4-arg
  `textureFor(letter, form, colour, attached)` — an orphan left from the **deferred §11** "shrink
  stuck-isolated" wiring that was never finished on the entity side (world renderer + `build()` are
  3-arg, no `attached` behaviour exists). Dropped the orphan arg → matches the real 3-arg API. §11 not
  built; isolated is now font-drawn + lowered, which may already cover most of §11 (confirm in-game).
- Files: `arabic/ArabicTileRenderer.java` (nudge + `ISO_DROP`/`ISO_TALL_DROP`),
  `client/render/ArabicLetterItemRenderer.java` (arg fix), `tools/render_preview/BeforeAfterPreview.java`
  (preview only). Jar `customblocks-1.0.0.jar` rebuilt + deployed 16:27.

---

## Group 14 · Phase 1b — the real crisp fix is the OWN-TEXTURE renderer (no atlas size wins); 512px re-enabled for it · 2026-06-19 (🟢 jar green — mockup placed, in-game preview pending)

> Follow-up to the 256px cap below. The owner tested 256 and found fresh blocks **pixelated** (low-res
> close up); raising to 512 brings the **muffle** back. Confirmed on disk: `g14crisp` = 256×256 (blocky),
> `g14pixel` = 512×6144 (muffled). **No atlas size wins** — low = blocky, high = muffled. The block atlas
> is the wrong pipeline for photo/text/video.

- **Real fix (decided with the owner).** Render blocks from their OWN texture like Minecraft maps: a
  per-block `NativeImageBackedTexture` (mipmaps off, per-block nearest/linear filter), drawn by a
  BlockEntityRenderer — the same proven mechanism as the Arabic letters (ADR-005) and the Group 13
  `ScreenTest` spike. Carries full resolution → crisp at any distance, with **zero client video settings**.
- **Mockup shipped (this jar).** Upgraded the isolated `screen_test` block into a gallery: new
  `client/render/ScreenTestImages` builds high-res sharpness cards (text/grid, gradient, resolution
  chart), an animated card, and **the owner's own blocks loaded at full source resolution** (animated
  ones decoded to frames). `ScreenTestBlockEntityRenderer` cycles the gallery with per-example filter +
  world-time animation; `ScreenTestBlockEntity.cycle()` free-runs (the client wraps to the live gallery
  size). Touches none of the 1028 production blocks.
- **In-chat proof.** Rendered the gallery to PNG/GIF (Pillow) and an interactive before/after widget
  (now-vs-new with a distance slider) so the owner could judge the difference without launching MC.
  Owner: "the new are amazing."
- **512px re-enabled for the own-texture path.** `MAX_TEXTURE_SIZE` 256 → **512**; the chest picker +
  command + suggestions add 512 (labeled "needs the new renderer"); `/cb config texturesize` warns when
  >256 that atlas blocks still soften until the rollout. Default stays 256. `sanitizeTextureSize` already
  floors to a power of two (512 → 512). Config still 298 lines (≤300 gate).
- **The catch (flagged to the owner).** `SlotBlock` is a plain `Block` with **no BlockEntity**, so
  production blocks render only via the atlas today. 512 muffles them until the own-texture renderer is
  wired into real blocks — rollout in spec §4 Phase 1b: `SlotBlock` BE + BER → dynamic item renderer →
  on-demand texture cache → make it the default path. **Not built** — needs in-game testing per phase.
- **Docs.** Updated `GROUP_14_ANIMATION_VIDEO.md` (banner, §3, §4 new Phase 1b row, §5) and
  `Reports/GROUP_14_TESTING_GUIDE.md` (§1 now tests the `screen_test` preview). No new ADR this pass
  (owner asked to keep it to the group spec + testing guide + logs).

Files: `client/render/ScreenTestImages` (new) · `client/render/ScreenTestBlockEntityRenderer` (gallery +
filter + animation) · `block/ScreenTestBlockEntity` (free-run cycle) · `CustomBlocksConfig` (cap → 512) ·
`command/handlers/ConfigCommands` (512 + >256 warning) · `gui/chest/TextureSizeMenu` (512 option).

**NOT done** until the owner places `screen_test` in-game and confirms it's crisp. Build-green = compiles + gates only.

---

## Group 14 · Phase 1 REDONE — muffling fix is the 256px atlas cap, Style toggle reverted · 2026-06-19 (🟢 jar green — 256 dev-confirmed, full re-test pending)

> Correction of the previous "Style toggle" entry below. The owner pushed back: the OLD mod's GIF
> blocks were crisp AND animated in inventories, so Path A (mcmeta atlas) is **not** broken — the
> regression had to be elsewhere. Deep-searched the old project and the live atlas log. Found the real
> cause, reverted the misdiagnosed fix, and did it correctly from scratch (owner: "don't copy the old
> buggy project blindly").

- **Real cause (measured, not guessed).** The in-game block-atlas log read `Created: 16384x8192x0` —
  trailing `0` = mip-level count = **mipmaps OFF**. -B let texturesize go to **512**; at 512 a block's
  frame-strip sprite (size × size·frames) blows past MC's **16384px** atlas limit, so Minecraft
  downscales the whole atlas and disables mipmaps for **every** block → the soft/"muffled"/speckled
  look on everything. The old mod never hit this: it hard-capped at 256 + enforced power-of-two. Owner
  set texturesize 256 in-game and confirmed it "feels good" / crisp.
- **The earlier "Style toggle" was a misdiagnosis.** It blamed the bicubic scale filter and added a
  per-block Sharp/Smooth control. That did not fix mipmaps. **Reverted in full:** `AnimData.sharp` +
  `withSharp`, its `SlotDataStore` persistence, the `/cb anim … style` command + Style card row, and the
  `AnimationDecoder.decode(raw,size,sharp)` overload are gone. The `/cb anim` card's **Smooth On/Off**
  row (toggles `interpolate`, the real frame-blend) is restored.
- **Correct fix (clean, from scratch).** (1) `CustomBlocksConfig.MAX_TEXTURE_SIZE = 256` +
  `sanitizeTextureSize` (floors any size to a power of two, 16..256) applied on config load and on
  `/cb config texturesize`; the command arg range and the chest picker drop 512. (2)
  `AnimationDecoder.MAX_STRIP_PX = 8192` + `atlasSafeSize` — a clip long enough that `size·frameCount`
  would overflow the atlas is rendered at a smaller per-frame size (largest power of two that fits), so
  a strip can never blow the atlas; short clips (≤32 frames at 256) are untouched. Frames are always
  bicubic again (the original look).
- **Build gate fallout.** Capping pushed `CustomBlocksConfig.java` to 311 lines (limit 300); compressed
  three verbose field javadocs (texture-size, mirrorNamedTextures, silentPack) back to ~298 — full
  rationale now lives in **ADR-007** instead of inline prose.
- **Docs corrected** (owner asked): `GROUP_14_ANIMATION_VIDEO.md` (status banner, §3, §4 Phase 1 row,
  §5), `Reports/GROUP_14_TESTING_GUIDE.md` (§1 now tests the cap/crispness, not the Style toggle), new
  **`docs/adr/ADR-007-texture-size-atlas-mipmap-cap.md`**, CHANGELOG.

Files: `CustomBlocksConfig` (cap + sanitize + comment trim) · `command/handlers/ConfigCommands`
(arg range + snap message) · `gui/chest/TextureSizeMenu` (drop 512) · `core/AnimData` +
`core/SlotDataStore` (revert sharp) · `command/handlers/AnimCommands` (drop style, restore Smooth row) ·
`image/AnimationDecoder` (single bicubic decode + atlas strip guard). Build GREEN (all 3 gates), jar →
`.minecraft\mods\customblocks-1.0.0.jar`. Test plan: `Reports/GROUP_14_TESTING_GUIDE.md` §1. **NOT done**
until the owner confirms crispness in-game.

---

## Group 13 · Arabic auto-join — placement flash fix + all-colours join · 2026-06-19 (🟢 jar green — in-game pending)

> Dimming ✅ confirmed by dev. This jar: kill the black-flash on placement + reverse to all-colours join.

- **Black flash (deep-searched).** The block's base model is a flat black cube (`cube_all` + `arabic_letter_bg`)
  and `getRenderType` wasn't overridden, so MC drew it the instant you place — before the BlockEntity synced
  the letter (gap = flash; the solid-black 3rd block in the dev's image). Fix: `getRenderType → INVISIBLE`;
  glyph is 100% the BER (chest/sign pattern). Watch: a valid letter showing invisible = texture-build failure.
- **All colours join (Issue 2, decided part).** Removed every colour gate — `ArabicJoinFlow` (recompute +
  letterAt) and `ArabicLetterBlock.getPlacementState` (`blackJoinFacing` → `joinFacing`, colour check gone).
  Any colour joins any colour; each keeps its own bg. Form brain is colour-agnostic.
- **Corners NOT built.** Straight rows join from either end; a 90° turn starts a new word ("Option B"). Option A
  (connect through with a kink) awaits the dev's A/B pick. Build green; jar → mods. Re-test → §10. Detail in root PROGRESS_LOG.

---

## Group 13 · Arabic auto-join — icons confirmed + full-bright glyph · 2026-06-19 (🟢 jar green — brightness in-game pending)

> Icons ✅ confirmed in-game (dev). Then the dev's screenshot showed the brightness bug live.

- **Brightness bug.** A screenshot showed a **dark-grey** green jeem beside a **bright** one — same colour,
  same letter, same texture, so the difference is **lighting**. Cause: the glyph sampled the lightmap at
  `pos.offset(facing)`, which reads dark when the block faces a solid neighbour.
- **Fix (locked v2):** full-bright — `faceLight = LightmapTextureManager.MAX_LIGHT_COORDINATE` on every face,
  so the white letter never dims or blacks out. Removed the unused `WorldRenderer` import. Build green; jar →
  `.minecraft\mods`. Re-test → §9. Tradeoff: the glyph no longer shades with the world. Detail in root PROGRESS_LOG.

---

## Group 13 · Arabic auto-join — icon fix v2 (item model → builtin/entity) · 2026-06-19 (🟢 jar green — in-game pending)

> The earlier "icons fixed" jar was incomplete — the dev tested in-game and join-letter icons were still
> solid black. Fix-pass v2 design session found the real cause; this jar ships the icon piece only.

- **Bug — join item icon solid black.** `ArabicLetterItemRenderer` (Fabric `DynamicItemRenderer`) was correct
  and registered, but Fabric only calls it when the item model is `minecraft:builtin/entity`.
  `models/item/arabic_letter.json` still parented `block/arabic_letter` (→ `cube_all`, flat `arabic_letter_bg`),
  so the flat black cube drew and the renderer never ran.
- **Fix:** item model → `{ "parent": "minecraft:builtin/entity" }` + `display` block copied from
  `minecraft:block/block`. No Java changed. Build green; jar copied to `.minecraft\mods`. Re-test → §8.
- **Locked but NOT built (design):** all-colours join (reverses prior BLACK-only), full-bright glyph,
  direction-agnostic join (axis from neighbours + start-dir on BE), flat-only. Corner A/B decision pending a
  3D mockup (delivered this session). Full detail in the root `PROGRESS_LOG.md` entry + the handoff.

---

## Group 14 · Phase 1 — the Style toggle (fixes the "muffled" look) · 2026-06-19 (🟢 jar green — in-game NOT confirmed)

> Fixes the one open finding from the revamp: animated pixel-art/text looked **soft**. Built as a single
> player-facing **Style** control (the owner rejected exposing Sharp/Smooth/blend as separate jargon
> switches — "combine them perfectly … not overwhelming"). Build GREEN, jar deployed. **NOT done** —
> the owner confirms crisp-vs-soft in-game.

- **Root cause (confirmed in code).** `AnimationDecoder.cropScaleSquare` scaled every frame with
  **bicubic + antialias** — great for photos/video, soft on pixel-art/text. `textureSize` was already
  256, so it was the **filter**, not resolution.
- **The fix — one "Style" control, named by content, not filter.** A normal user knows whether their
  picture is a photo/video or pixel-art/text — not "bicubic" vs "nearest". So the `/cb anim` card's old
  `Smooth [On][Off]` row is **replaced** by one **Style** row:
  - **[Photos & Video]** (default) → bicubic scale **+** frame-blend on (the current look).
  - **[Pixel-art & Text]** → nearest-neighbour scale **+** frame-blend off (crisp pixels/text).
  One pick sets **both** under-the-hood knobs; the scale filter and the motion-blend are never two
  separate switches for the player. New blocks default to **Photos & Video** (owner's call — keeps every
  existing block looking the same).
- **New plumbing — the first PIXEL-editing `/cb anim` edit.** Existing edits (speed/loop/trim) only
  rewrite plain numbers + regenerate the mcmeta. Switching Style changes pixels, so it **re-decodes the
  strip from the stored source** (`TextureStore.loadSource`) with the new filter, off the server thread,
  then saves the strip + sets `sharp`/`interpolate` + one pack rebuild — **never re-downloads**. Speed,
  loop, trim and per-frame timing are all preserved (only the scale pair changes). Blocks with no stored
  source (made before sources were kept) get a clean "remake it to switch" message, not a crash.
- **Persistence.** `AnimData` gains a `boolean sharp` (default false = Photos & Video); `SlotDataStore`
  writes it only when true and reads it back defaulting to false, so old slots load unchanged.

Files: `core/AnimData` (new `sharp` field + `withSharp`) · `core/SlotDataStore` (persist `sharp`) ·
`image/AnimationDecoder` (`decode(raw,size,sharp)` → nearest-neighbour `cropScaleSquare`) ·
`command/handlers/AnimCommands` (Style card row + `style photo|pixel` re-decode rail). Build GREEN (all
3 gates), jar copied to `.minecraft\mods`. Test plan: `Reports/GROUP_14_TESTING_GUIDE.md` §1. **NOT done.**

---

## Group 14 · v2 design revamp — Display Block platform + full roadmap · 2026-06-19 (📐 design only — no code)

> Dev confirmed Part A works in-game ("they work") and flagged that the animated WebP/pixel-art looks a
> little soft. We turned that re-test into a full design session and revamped Group 14 from "GIF editor"
> into the mod's **Display Block platform.** No code this pass — spec + testing guide + logs only.

- **In-game finding — "muffled" animated pixel-art/text.** Root cause: `AnimationDecoder.cropScaleSquare`
  scales every frame with **bicubic + antialias**, which softens pixel-art and text; `textureSize` is
  already 256, so it's the **filter**, not resolution. → **Phase 1** adds a per-block **Sharp**
  (nearest-neighbor) / **Smooth** (bicubic) toggle. (Some softness from Minecraft's own mipmaps remains at distance.)
- **Locked the spine — two render paths.** Path A (pack `.mcmeta`: animates **everywhere**, **cheap**,
  globally **synced**, **can't gate**) vs Path B (client renderer/BlockEntity: **world-only**, **per-block
  cost**, **independent + gateable**). **Auto-perf governs Path B**, not base animation — placing many
  copies of an mcmeta block is ~free.
- **Owner decisions captured** (full list in `GROUP_14_ANIMATION_VIDEO.md` §3): dedicated **Animation tab**
  in `/cb create` with a **live-playing** preview; `/cb anim <id>` → studio **edit-mode**, `/cb anim`
  (empty) → `/cb listgui` → studio; **timeline editor**; **color grading + chroma-key + smart framing**;
  **playback polish** (de-sync / crossfade / ramp / play-once); **Auto perf** config (Always / Distance /
  Auto + off-screen freeze + LOD); **ffmpeg-if-present** universal video; **video wall** (one synced screen
  engine, N×M, auto-remap, fit modes); **live data blocks** (clock / date / countdown / stats / marquee);
  **sync channels**; **auto-emissive**; **trigger playback**; **interactive** right-click; **redstone-reactive
  attribute for ALL blocks** (its own track). License gating **deferred**.
- **10-phase build order set** (`GROUP_14_ANIMATION_VIDEO.md` §4). Phase 2's **studio edit-load path**
  unlocks most later phases; **Auto-perf precedes** the heavy Path-B features; **redstone-for-all can be
  pulled early** (independent of animation).
- **Superseded:** the old chest-GUI anim editor (P1), the jcodec Video Studio (Q6), and the `/cb anim`
  chat card are retired by the screen-based + ffmpeg + Phase-2 design.

Docs: `GROUP_14_ANIMATION_VIDEO.md` (rewritten v2) · `Reports/GROUP_14_TESTING_GUIDE.md` (revamped). **No
source changed.** **NOT done** — this is a plan; every phase needs in-game confirmation as it's built.

---

## Group 14 · Part A — fixes after first in-game test · 2026-06-19 (🟢 jar green — re-test pending)

> ✅ **Dev confirmed in-game:** `/cb create t3st t3st <gif-url>` makes a GIF block that **animates** (the
> core pipeline works). Two issues reported → both fixed this jar.

- **Studio "Load texture" → "URL failed" on GIF/CDN links.** Root cause: the studio screen did a raw
  `ImageIO.read(URI.toURL())` with Java's **default User-Agent**, which Wikimedia/CDNs 403 (the command
  path worked because `ImageDownloader` sends a browser UA + resolves share pages). Fix: new client
  helper `client/gui/StudioTextureLoader` routes the preview through the SAME `ImageDownloader` path +
  `AnimationDecoder.isAnimated`; the screen now badges **"✔ animated — it'll play once placed"**.
  (Publish already reached the animated rail via `CreationStudioBridge → doCreate`, so publishing a GIF
  was always going to work — only the preview was broken.) Extracting the fetch also kept
  `BlockCreationStudioScreen` under the 500-line gate.
- **`/cb anim` was an ugly multi-line chat dump.** Reworked into ONE clean, **clickable** card
  (`AnimCommands.show`): header + stats line + clickable rows — Speed `[5][10][15][20][30][Original]`,
  Loop `[Loop][Bounce][Reverse]`, Smooth `[On][Off]`, Trim — current value highlighted green, each
  button runs the change. Logic reworked too: `AnimData` now keeps the source's **original per-frame
  timing forever** (`frameTimes`) with an explicit `uniformTicks` override, so **[Original]** restores
  real timing and the card shows a true effective-fps. New `/cb anim <id> original` subcommand.
  Persistence migrates the old `frametime` field → `uniformTicks` on read (back-compat).
- **WebP test links** handed to the dev: static `gstatic.com/webp/gallery/1.webp`, animated
  `mathiasbynens.be/demo/animated-webp-supported.webp`.

Files: `client/gui/StudioTextureLoader` (new) · `client/gui/BlockCreationStudioScreen` ·
`core/AnimData` · `core/SlotDataStore` · `network/ServerPackGenerator` · `command/handlers/AnimCommands`.
Build GREEN (all 3 gates). **NOT done** — dev re-tests studio GIF + the new card + webp (see
`GROUP_14_TESTING_GUIDE.md`).

---

## Group 14 · Part A — Animated blocks via commands · 2026-06-19 (🟢 jar green — in-game NOT confirmed)

> ✋ **Golden Rule:** an animated block is DONE only when the dev places one in-game and sees it animate.
> Build is green; that proves it compiles + gates pass, nothing more. Test plan: `GROUP_14_TESTING_GUIDE.md`.

**What this restores/adds.** Animated blocks from a GIF/WebP link, driven by a Minecraft pack
**`.mcmeta`** (vertical frame-strip + sidecar) so they animate **everywhere automatically** — world,
hand, inventory, creative tab, `/cb list`. A live BlockEntity renderer was rejected in the locked design
because it would NOT animate in inventories. Built clean from scratch; old project STUDIED for the GIF
algorithm only (no code copied).

**Architecture (locked design, v1).**
- `core/AnimData` (new, immutable record): frameCount · frametime · loopMode (loop/bounce/reverse) ·
  interpolate · trim in/out · transparency · per-frame `frameTimes[]`. Owns the Loop/Bounce/Reverse
  frame-INDEX ordering (`playback()`); stores **plain numbers only**, never a baked mcmeta string.
- `core/SlotData`: `anim` added as ONE field (10-arg canonical ctor; every `withX` threads it; `withAnim`
  + `isAnimated()`; back-compat ctors default `AnimData.NONE`). `core/SlotDataStore`: optional `"anim"`
  object, omitted for static blocks → restart-persist. `core/SlotManager`: `animFor` · `setAnim` · dupe
  clones anim.
- `image/AnimationDecoder` (new): generic multi-frame `ImageReader` (GIF for sure; animated WebP if the
  plugin exposes frames). GIF disposal(0/1/2/3)+offset compositing, real per-frame delay→ticks
  (`max(1, cs/5)`), center-crop square + normalize each frame to textureSize, vertical strip. Two-pass
  even-sample to ≤256 frames (composites every frame for disposal correctness, snapshots only the kept
  ones). Heap/timeout/dimension guards; off the server thread.
- `network/ServerPackGenerator.emit()`: writes `slot_N.png.mcmeta` for animated slots, regenerated
  **deterministically** from AnimData every build (only when a real strip exists, never the 1×1
  placeholder); forces cube_all. `emit()` is the single source of truth → server-zip + client-loose match.
- `command/handlers/AnimCommands` (new): `maybeCreateAnimated` rail (hooked into
  `CreationCommands.createWithTexture` after download; false → static fallback) + `/cb anim <id>
  ticks|fps|loop|smoothing|trim` (edits numbers → regenerate mcmeta + ONE pack rebuild, no re-download).
- `image/ImageDownloader` + `image/LinkResolver` (new): Tenor/Giphy/Imgur **page** links resolved via
  OpenGraph/Twitter image meta (prefers `.gif`). `build.gradle`: TwelveMonkeys `imageio-webp` 3.12.0 (+
  core/common, all JiJ'd — verified 5 nested jars + WebP `ImageReaderSpi` present); `CustomBlocksMod`
  runs `ImageIO.scanForPlugins()` under the mod classloader so Fabric finds the SPI.
- `CreationCommands.retextureAll` now SKIPS animated slots (toBlockPng would crop the tall strip into one
  square and kill the animation).

**Old-project bugs designed out:** per-frame timing kept as plain numbers + mcmeta rebuilt every build
(old: animMeta-as-string lost timing on save); frames normalized to a uniform square (old: not
normalized); strip strictly size×size·N, frame count never inferred from pixels (old: boundaries
misdetected).

**Not in Part A (queued):** Part B = unified studio Texture-tab editor + filmstrip + crop/pan/zoom +
edit-mode + rebindable keybind (retires `BlockEditorScreen`). Part C = video→animated (frame range).

---

## Group 13 · Pass 4 (auto-join) — No-reload texture: research + PROOF SPIKE · 2026-06-18 (✅ CONFIRMED in-game — no-reload path proven)

> ✅ **2026-06-18 — dev confirmed in-game:** right-clicking the spike block changes its picture with **no
> resource-pack reload**. The make-or-break is settled — the real Pass 4 auto-join is built on this path.
> Next: joining brain → one real letter on the live-texture path → make them join (4a), then direction +
> readable back + Omni-Tool (4b). The `screen_test` spike is retired once the real letter block lands.

**Why this exists:** Pass 4 auto-join (place letter blocks → seamless joined word) has been blocked for
sessions on one thing: every per-block texture change goes through the resource pack, which forces a
client reload. Before building any joining, we had to prove a texture path that changes a block's picture
with **no reload**. Dev chose "solve the prompt first" → this research + spike.

**Research findings (read in live code, not guessed):**
- The "Would you like to download the pack?" **confirm dialog is already auto-suppressed** —
  `ClientCommonNetworkHandlerMixin.customblocks$silentAccept` recognises our pack by its label and
  silent-accepts it (returns no screen). So the dialog is NOT the real blocker.
- The real pain is the **full client resource reload**: any pack content change → rebuild pack →
  `ResourcePackGenerator.regenerate` → `client.reloadResources()` (`ResourcePackGenerator.java:104`).
  That's the multi-second "reloading" hitch, and the most likely trigger of the dev's connection resets.
- The resource-pack route **cannot** avoid that reload (atlas re-stitch is global). Confirmed the mod has
  **zero** dynamic-texture / BlockEntity infrastructure — all 1028 SlotBlocks are pack-textured. The live
  GUI preview (`ArabicPreviewScreen`/`PreviewCube`) is a fake cube of GUI rectangle-fills, not a real
  world-block texture, so it is not a reusable template.

**Decision (see new ADR):** render joined-word blocks with a **BlockEntityRenderer + a live in-memory
`NativeImageBackedTexture`** registered straight into the `TextureManager`, bypassing the resource pack
entirely. Same technique vanilla uses for signs/banners/skulls. Changing a block's picture = swap the
in-memory texture → no pack rebuild, no `reloadResources()`, no dialog, no reset risk, instant.

**Built — an isolated PROOF SPIKE (throwaway, proves only the mechanism):**
- `block/ScreenTestBlock` (BlockEntityProvider; right-click cycles the picture, server-authoritative),
  `block/ScreenTestBlockEntity` (one synced int `variant`; `markForUpdate` → `toUpdatePacket`),
  `block/ScreenTestRegistry` (new ids `customblocks:screen_test` — block + item + BlockEntityType),
  `client/render/ScreenTestBlockEntityRenderer` (builds a distinct 64² bitmap per variant, caches it,
  draws 6 no-cull faces from the live texture).
- Static bundled assets `blockstates/screen_test.json` + `models/block|item/screen_test.json` (base =
  `minecraft:block/smooth_stone`, so it is visible/placeable without the generated pack).
- 3 one-line hooks only: `ScreenTestRegistry.register()` in `CustomBlocksMod.onInitialize`, the block
  added to the Tools creative tab, and `BlockEntityRendererFactories.register(...)` in `CustomBlocksClient`.
- **Touches none** of the SlotBlocks, SlotManager, or the pack generator (RD §4/§8).

**State:** `gradlew build` green (verifyFileSize / verifyMojibake / verifySound all pass); jar deployed to
`.minecraft\mods`. **NOT done** — needs the dev's in-game confirm: place it, right-click, picture changes
instantly with NO reload / freeze / prompt. Test steps in `Reports/GROUP_13_TESTING_GUIDE.md` (top, 🎯).

**Next (only after in-game confirm):** if the no-reload swap holds → build the real Pass 4 on this path —
the joining brain (RTL + non-connectors, which letter is initial/medial/final), neighbour re-flow on
place/break, and feed the **sliced `ArabicWordRenderer`** output (the dev-blessed §0 art) into the BER
instead of the procedural bitmap. If it does NOT hold → diagnose before building further. The spike block
is removed/retired once the real path is in.

---

## Group 27 · G27.8 — Refined "Masterpiece" Revamp Pass · 2026-06-18 (DESIGN ONLY — nothing built yet)

**Status:** Design discussion complete + dev-approved via an **interactive, in-game-accurate HTML mockup**
of the new Live Recolour screen (refined gold-on-black). **Nothing built, nothing tested.** Full spec:
`GROUP_27_SCREENS.md §G27.8`.

**Why:** after G27.7 the dev reviewed every screen and flagged: bottom button strips + scattered controls
look "old / unprofessional," the `[?]` overlay lets the screen behind show through (no scrim), the dim
slider is in the way, the Arabic colour panel is crammed (`+hex/drop/fav/save/load` ≈20px each), the
recolour `Temperature` label overlaps `TONE TOOLS`, per-brick HUD editing is a scattered stacked popup,
and the shape editor is "still partial."

**Root causes confirmed (read, not guessed):** `CbHelpOverlay` + `HudEditorOverlays.drawHelp` draw no
scrim (and the HUD help closes on *any* click); `RecolorSliderScreen.layoutSliders` + `RecolorToneTools.layout`
place the `TONE TOOLS` header (y≈142) and the `Temperature` label (y≈143) on the same line; `CbColorPanel`
collapse box is a 9×9px target; Recolor/Shape/HUD still use vanilla full-width button strips while Arabic
already uses `CbActionBar`.

**Decisions locked (2026-06-18):**
- **Look:** refined gold-on-black (subtle header gradients, faux-rounded corners, soft shadows, hover glow,
  consistent spacing). **Motion:** subtle & smooth + a Reduced-motion toggle.
- **Universal move = Shift+Left-drag** (cube / panels / bar / bricks) — added to hints + help. Layout
  positions remembered **per screen**.
- **Shared kit:** one `[?]` overlay with a full-screen **scrim** + red **[X]** (HUD help folded in); roll
  `CbActionBar` (movable+dockable) onto every screen; cube stage = soft shadow + faint platform; Front/Iso/Top
  view buttons; **gold-only** selection accent.
- **⚙ Settings menu** (new, global, every title bar) replaces the dim slider — holds: dim (Off/Dim/Dark),
  hide-world, UI scale, reduced-motion, master sound + volume, auto-spin default + speed, hint-lines/tooltips
  toggles, confirm-before-discard, snap strength, copy format, compare-tools toggle, reset-positions,
  reset-settings.
- **Sounds (RD §2):** blend amethyst chime (Apply/Save) + note-block (toggles) + subtle clicks/ticks
  (hover/buttons); master on/off + volume.
- **Primary stays open** after Apply/Save (green flash; close via Cancel/Esc/Back) — toggleable.
- **First-run** subtle one-time coachmarks. **Failure UX:** grey fallback **and** Retry (never crash/purple).
- **livecolor:** centered cube + action bar; HSL/TONE grouped collapsible (fixes overlap); compare =
  split-cube **and** hold-to-peek (both, toggleable); GIF animates with play/pause.
- **arabic:** cube centered, panel docks right, both freely Shift-movable; colour panel widened, **all**
  features kept in collapsible sections; `+hex/drop/…` → icon+label; palette-grab from image URL or the
  block's own texture.
- **edithud:** floating mini-toolbar on the selected brick (−/+ size · colour · ⚙ · ✕) **+ corner-drag
  resize**; deep options behind the ⚙ card.
- **shapeeditor:** revamp the named-shape picker (big stage cube + live **auto-spinning** mini-3D on every
  chip, grouped, new look) + a locked **"Custom — soon"** teaser chip; freeform custom-AABB **carve** editor
  still deferred to its own session (new payload + server) and **marked ⏳ "soon" in the testing guide**.
- **Presets & sharing:** gallery of live mini-3D thumbnails + share codes; **search everywhere**.
- **Block extras:** a **glow** (fullbright) toggle previewed live; an **Arabic/RTL** UI option in Settings.
- **Out of scope:** server chest-GUI menus (later); freeform carve (separate session); block FX beyond glow.

**Build approach (when dev says go):** shared kit first (Settings, scrim help, action-bar rollout, cube
stage, mini-toolbar, sounds), then screen-by-screen, build-green at each step, ONE in-game test per screen
(CLAUDE.md §2/§4). **No code written yet.**

**Spec:** `GROUP_27_SCREENS.md §G27.8`. **Tests:** `Reports/GROUP_27_TESTING_GUIDE.md` (custom-shape carve
marked ⏳ "soon").

---

## Group 27 · G27.7 §D + §E5 — Eyedrop polish + HUD centre-name bug · 2026-06-17 (BUILT — build-green, batch)

**§D eyedrop** (`EyedropScreen`): first-time intro popup (persisted `CbScreenPrefs.eyedropIntroSeen`, shows once), permanent hint, shared `CbHelpOverlay`, and a hide-UI toggle (H / `hide` button) that removes the title bar + crosshair so covered pixels are sample-able (§D2). Sample path unchanged; live loupe lives in the panel dropper (§B4), deferred for the full-screen sampler. **§E5 HUD bug** (`HudAnchors.reanchor`): centred variable-width text bricks (display-name) used a left-edge corner anchor → long names drifted right. Now a non-divider brick dropped near the horizontal centre gets the CENTER anchor → grows symmetrically, stays centred. **§E1–E4 NOT built** (restyle/guidance/Advanced fold/drag feel) — `HudEditorScreen` is 494/500, needs splitting first; own session. All gates green; not deployed yet.

---

## Group 27 · G27.7 §C2/§C3 — Recolour controls revamp + tune tools · 2026-06-17 (BUILT — build-green, not yet in-game tested, batch)

**§C2:** plain grey slider bars → real colour-gradient tracks (`client/gui/CbGradSlider.java`): hue rainbow, sat grey→vivid, light black→white, temp cool→warm; bigger knobs + value chips. **§C3:** four tune tools (`client/gui/RecolorToneTools.java`) — Temperature, Contrast, split brightness curve (lift shadows / lower highlights), one-tap filters (Gray/Sepia/Invert/Poster), all per-pixel point ops in `image/CbToneMath.java` shared by preview + bake. `RecolorApplyPayload` grew 4→9 fields → manual `PacketCodec.ofStatic` codec; `ColorToolService.applyRecolor` bakes HSL→tone; undo/reset/dirty unified. Dropped harmony-shift (overlaps Hue). RecolorSliderScreen 335 lines. All gates green; manual codec runtime-unverified until in-game; not deployed (batch).

---

## Group 27 · G27.7 §A4 — Dockable / hideable / smaller action bar · 2026-06-17 (BUILT — build-green, not yet in-game tested, batch)

Reworked the fixed full-width bottom button strip into `client/gui/panel/CbActionBar.java`: smaller custom-drawn buttons, drag-grip to **dock bottom/left/right** (live snap), corner **hide toggle** → thin sliver + show tab, green primary (Create) with `flashPrimary()`. Dock + hidden persist per screen via `CbScreenPrefs` (now whole-object Gson + a per-screen `bars` map; old dim-only files still load). Integrated into `ArabicPreviewScreen` (removed its 7 vanilla bottom buttons + fixed strip). Other frame screens adopt it in their slices. All gates green; not deployed (batch).

---

## Group 27 · G27.7 §B — Floating colour panel + smart-colour system · 2026-06-17 (BUILT — build-green, not yet in-game tested, NOT deployed: batch build)

**Status:** built per §G27.7 §B (locked 2026-06-17), compiling + all gates green (verifyFileSize / verifyMojibake / verifySound). Part of the slice-by-slice batch (dev tests all slices once at the end); **not deployed mid-batch**.

Built **once** as a reusable floating colour panel and wired into `ArabicPreviewScreen` first (Option A — block left, panel right; replaces the old crammed bottom swatch rows). Reused later by recolor (slice 5), the §A4 bottom bar, and the HUD.

- **§B1 movement:** drag-grip header, magnetic edge-snap, collapse-to-header toggle, position + collapsed remembered **per screen** on disk.
- **§B2 palette:** add by hex (or in-panel dropper), remove, drag-reorder, recents row, named saved palettes (`load` = inline list, left-click load / right-click delete).
- **§B3 smart-colour:** contrast guard (live WCAG letter-vs-bg readout), harmony (complementary/triad/analogous) + tint/shade strip, fast keys 1–9, favourites row, hover-swatch hex, right-click edit. (Pull-from-image dropped per spec.)
- **§B4 dropper + loupe:** dropper button freezes the frame once (lag-free), loupe magnifier follows the cursor showing the exact pixel + hex, click samples into the panel without leaving the screen.

**New files:** `image/CbColorTools.java`, `client/gui/panel/CbPaletteStore.java`, `client/gui/panel/CbColorPanel.java`, `client/gui/panel/CbPanelHarmony.java`, `client/gui/panel/CbColorDropper.java` (all ≤500). **Changed:** `ArabicPreviewScreen` (embeds the panel, removed the local swatch rows, routes input through it). Drag-grip uses 3 drawn ASCII bars not the spec's `⠿` glyph (mojibake/font-safe). **Next:** §A4 bar, then slices 5–7, then one deploy + batch test guide.

---

## Group 27 · G27.4 — `HudEditorScreen` redesign: Lego HUD Builder · 2026-06-16 (BUILT — build-green, not yet in-game tested)

**Status:** Built per the locked spec, compiling + all gates green (verifyFileSize / verifyMojibake / verifySound), jar produced. **NOT done** — awaiting the dev's single in-game test pass (testing guide §4). Three flagged deviations (optional `/cb config keybind` chat route not built — vanilla Controls covers rebinding; undo/redo in-session not disk-persisted; per-brick Align stored but renders left). See the root `PROGRESS_LOG.md` top entry for the full file list. Supersedes the original "snap-to-corner buttons" plan with a full rebuild.

**What the dev asked for:** the current `/cb edithud` is "bad" — a fixed 2-line (id+name) box with `−/+` click-spam and an 8-colour cycle. Dev wants a **free-floating, brick-based "Lego" HUD builder**: add/remove/swap/reorder any info line, full per-brick styling, **magnetic snapping**, a **full colour picker**, QoL first. "Build it all, one by one perfectly, then I test once."

**Decisions locked (2026-06-16):**
- HUD = ordered **list of bricks**; each brick = one info line with its own position, anchor, style.
- **Free-floating** layout (dev's explicit choice over single-box / grouped): each brick stores `(offsetX, offsetY, anchor∈{TL,TR,BL,BR,CENTER})`; snapping sets the anchor automatically so bricks hold across resolutions / GUI scale.
- **Magnetic snap** (`HudSnap`): Figma-style smart guides — snap to screen edges / centre / thirds AND to other bricks' edges / centres; cyan guide lines; **Shift** = free; arrow keys nudge (1px / Shift 10px).
- **Full colour picker** (dev's explicit choice): saturation/value square + hue slider + hex/RGB + swatches + recent colours.
- Per-brick controls: show/hide, reorder (z-order), delete, type-swap; inspector = size / colour / bold / shadow / prefix / align.
- Brick catalogue (real data only): ID, Name, Slot#, Coords, Light, Distance, Facing, Custom text, Header, Divider (all free / client-side) + Category, Glow, Hardness, Sound, Shape, Solid (sync-expansion).
- **Deferred — no data in `SlotData`:** Author/credit, Source URL, Texture type, Resolution. Dev chose "ship without them."
- Presets: Minimal / Detailed / Builder. Sound: snap tick / slider click / save chime (Royal Directive §2).
- Group 27 standard frame kept: `0x33` backdrop, gold title bar, `[?]`, two hint lines, bottom action bar `[Undo][Redo] ··· [§aSave] ··· [Copy][Reset][Cancel]`, cancel-confirm, save flash, Ctrl+Z/Y/C/V/Enter/Esc.
- **Backgrounds:** global default behind un-overridden bricks + per-brick override (own colour/opacity or off). No shared box (bricks float). Colour picker gains an **eyedropper** (reuses `EyedropScreen`).
- **Per-brick effects** (optional, off by default): rainbow / pulse / gradient.
- **Brick visibility (smart, by family):** block-info bricks (ID/Name/Slot/Category/Glow/Hardness/Sound/Shape/Solid) show only on-look; world/custom bricks (Coords/Light/Distance/Facing/Custom text/Header/Divider) show always.
- **Panel:** collapsible + see-through (Tab). **Master HUD on/off** switch in the panel. **Fresh default** = Name larger on top, ID smaller beneath (existing saved configs migrate instead).
- **Hover sound** (`HudHoverSound`): edge-triggered look-at feedback — Trigger None/Custom/Any, 17-sound dropdown (None, Bamboo, Stone, Wood, Amethyst, Note Pling, Glass, Bell, Wool, Copper, Sand, Grass, Nether, Froglight, Sculk, Lodestone, Anvil), volume slider 0–100%, **preview-on-pick**.
- **Keybinds** (`CbKeybinds`, client-side per-player, single keys, rebindable): `H` toggle HUD · `Right Shift` CB menu · `Right Ctrl` HUD editor. Also rebindable via `/cb config gui` (`ConfigScreen` Keybinds section) + chat `/cb config keybind <toggle_hud|menu|editor> <key>`. **No server keybind** — Minecraft keybinds are always client-local (corrected a dev misconception).
- **Presets** (`HudPresetStore` + `HudPresetBrowser`): 3 built-ins (Minimal/Detailed/Builder) + `[Save as…]` (name clash → **overwrite-confirm**) + browser with **mini-thumbnail** previews + import/export **both** code-string (clipboard+chat) and `.json` file in `config/customblocks/exports/Hud_Presets/` (`exports/` already exists per `BlockExporter`; subfolder created on first save).

**Bug found while tracing the data path (fix during the rebuild):** `HudSync.sendTo()` joins each slot as `customId + ' ' + displayName`, but `ClientSlotCache.populate()` splits on the **first space** (`indexOf(' ')`) — so a display name containing a space splits at the wrong place, and a one-word name (no space) is dropped entirely. The rebuild replaces the fragile delimited string with a **structured per-slot JSON** object (id, name + sync-brick fields), fixing it.

**`HudConfig` migration:** old saved files (`hudX/hudY/hudColor/…`) auto-map to two bricks (ID + Name) at the saved position — existing users see no regression.

**New / changed files (planned, ≤500-line gate; `HudConfig` ≤300):** `client/hud/HudFieldType.java`, `client/hud/HudField.java`, `client/hud/HudSnap.java`, `client/HudConfig.java` (rewrite), `client/HudRenderer.java` (rewrite), `client/gui/HudEditorScreen.java` (rewrite), `client/gui/hud/HudBrickRow.java`, `client/gui/hud/HudBrickPalette.java`, `client/gui/hud/HudColorPicker.java`, `client/gui/hud/HudBrickInspector.java`, `client/gui/hud/HudPresetBrowser.java`, `client/hud/HudPresetStore.java`, `client/hud/HudHoverSound.java`, `client/CbKeybinds.java`, `network/HudSync.java` (change), `client/ClientSlotCache.java` (change), `gui/screens/ConfigScreen.java` (change — Keybinds section), `command/handlers/ConfigCommands.java` (change — `/cb config keybind`).

**Build order:** data model → renderer → editor UI → snap engine → colour picker → hover sound → keybinds → sync bricks → presets → polish. Green at each step; **one** in-game test at the very end (dev's call).

**Spec:** `GROUP_27_SCREENS.md §G27.4`. **Tests:** `Reports/GROUP_27_TESTING_GUIDE.md §4`.

---

## Group 27 — Unified Screen Design System, Upgrades & Block Creation Studio · 2026-06-16 (DESIGN ONLY — nothing built yet)

**Status:** Design discussion complete. All 6 screens documented (5 upgrades + 1 new studio). Nothing built, nothing tested. Group 28 merged into Group 27 — no separate Group 28 doc exists.

**What this group is:** A unified design language for all CB `Screen` subclasses, per-screen upgrade plans, a new `ShapeEditorScreen`, and a full `BlockCreationStudioScreen` (opened by `/cb create` no-args). Screens are built/upgraded one at a time, in order, with in-game confirmation between each.

**Design decisions locked (2026-06-16):**
- Backdrop `0x33000000` on ALL screens — world always visible behind every screen.
- Title bar: dark strip `0xAA000000` + thin `§6` gold (0xFFFFAA00) 1px bottom border.
- Title format: `§6§l{Name} §7— §f{id}` (with block) or `§6§l{Name}` (without).
- Two hint lines: `§7` screen-specific controls (line 1), `§8` universal shortcuts (line 2).
- Bottom action bar: dark strip + thin `§6` gold 1px top border.
- Button order: `[Undo] [Redo] [Rand] ··· [§aSave] ··· [Copy] [Reset] [Cancel]`.
- Cancel with unsaved changes → in-screen "Discard changes?" overlay with `[Yes, discard]` / `[Keep editing]`.
- Save feedback: primary button flashes green 600ms + professional chat message.
- `[?]` in title bar top-right → in-screen shortcut overlay.
- Undo history persists per block per screen type (disk).
- Universal shortcuts: Ctrl+Z/Y/Shift+Z (undo/redo), Ctrl+C/V (copy/paste), Ctrl+R (randomize), Enter (primary), Esc (cancel), R (reset view on 3D screens), ? (help).

**Screens in scope:**
1. G27.1 `ArabicPreviewScreen` — adopt unified frame, shortcuts, undo
2. G27.2 `RecolorSliderScreen` — flat 2D box → 3D drag-rotate cube + full unification
3. G27.3 `EyedropScreen` — minimal; title bar + hints only
4. G27.4 `HudEditorScreen` — full unification + snap-to-corner buttons
5. G27.5 `ShapeEditorScreen` — new build; 3D cube + wireframe AABB editor + presets + undo
6. G27.6 `BlockCreationStudioScreen` — new build; full professional creation screen (blocked on G27.1–G27.5)

**Block Creation Studio key decisions (G27.6):**
- `/cb create` (no args) → opens `BlockCreationStudioScreen`. All args CLI untouched.
- Sidebar (left) + 3D live preview (center, always visible). World visible behind (`0x33` backdrop).
- Sidebar is a **mode-switching panel stack** with breadcrumb navigation (`‹ Main › Texture › Color`).
- Main sidebar = section rows (Identity / Texture / Shape / Attributes / Organize), each shows current value + ✔/⬜ indicator, click to enter sub-panel.
- `[Start from Template]` + `[Clear All]` at top of sidebar.
- **Nothing leaves the screen.** Color picker, shape editor, eyedrop all happen inside sidebar panels.
- Texture panel: tool tabs [URL] [Color] [AI] [Eyedrop]. Color tab = palette grid + HSL sliders + #hex + eyedrop. AI tab = text prompt + Generate.
- Shape panel: preset grid + inline AABB editor (box list + coordinate nudges + snap + mirror). Cube shows wireframe cage. Same design as G27.5 embedded in sidebar.
- Attributes: drag sliders (Glow 0–15, Hardness 0–10 with named presets), Sound dropdown, Collision/Transparent toggles.
- Cube: auto-spins on open, drag/scroll/shift-scroll/R controls, shows texture + shape + glow aura. Updates live with every change.
- Validation: sidebar scrolls to first problem field, outlines it red, shows error inline.
- Session memory: all fields + last texture cached to disk, restored on next open.
- Action bar: `[Undo] [Redo] ··· [§aDraft] [§aCreate & Publish] ··· [Copy Config] [Cancel]`.
- All Group 27 shortcuts + Tab navigation between fields.

**New files created (design/template only — not compiled into mod yet):**
- `src/main/java/com/customblocks/client/gui/CbScreenTemplate.java` — canonical template all new screens must follow
- `docs/Finale Fix/GROUP_27_SCREENS.md` — full unified spec (G27.1–G27.6, all screen designs)
- `docs/Finale Fix/Reports/GROUP_27_TESTING_GUIDE.md` — in-game test guide (§1–§6, all screens)
- `docs/Finale Fix/GROUP_28_CREATE_STUDIO.md` — redirect only (merged into GROUP_27)

**New files to be created when building:**
`ShapeEditorScreen.java`, `ShapeEditorPayload.java`, `BlockCreationStudioScreen.java`, `studio/StudioSidebar{Main,Identity,Texture,Shape,Attributes,Organize}.java`, `CreateStudioPayload.java`, `StudioSession.java`.

**Build order:** G27.1 → G27.2 → G27.3 → G27.4 → G27.5 → G27.6 (one at a time, in-game confirm between each).

---

## Group 13 — Text/word stroke: uniform outline + proportional-to-glyph "golden rule" · 2026-06-16 (build green, deploy pending, NOT tested)

**Status:** Built green (compile + 3 gates, JDK 21, 26s). **Deploy held** until the developer confirms
Minecraft is fully closed (a hot-swap under a running game is what corrupted the earlier session —
class loads failed with "ZipFile invalid LOC header"). All settings were proven in the headless
`tools/render_preview` harness and **approved by the developer** ("perfect") before any code/build.

**What the developer wanted:** (1) numbers' stroke matches the letters' (felt thinner); (2) consistent
stroke; (3) the "golden rule" — a long word must not get a fat-blob outline; the stroke should scale so a
multi-char block reads like a 1-char block. After previews, the developer also flagged that a pure
1-char-ratio made long words too thin, and picked a **medium** floor.

**Changed in `ArabicWordRenderer` (constants + render math):**
- `LATIN_RING` 6 → **10** (English letters + numbers).
- `ARNUM_OUTLINE` 14 → **12**, `ARNUM_WHITE` auto → **3** — Arabic numbers now read the same stroke as
  Arabic words (both outline 12, white-core 3). Arabic words unchanged (12 / 3).
- **Proportional stroke (golden rule):** stroke scales by `f = min(1, finalSize / refSize)` where
  `refSize` is the first glyph fitted ALONE — so a **1-char block = full stroke** regardless of glyph
  shape (fixes a bug where wide single letters like ب thinned out). Floored at **`STROKE_FLOOR = 0.65`**
  (developer's "medium") so long words stay solid, never hairline. Both the black ring and the white
  core scale by `f`.

**Preview workflow:** tuned in `tools/render_preview/RenderPreview.java`; samples opened as PNGs
(`STROKE_oncolor.png` on teal so the black stroke was visible, `STROKE_FLOOR.png` for the floor pick,
`FINAL_stroke.png` for sign-off). Harness constants mirror the shipped ones.

**Re-test (after deploy + full relaunch):** make a 1-char block and a long word/number of each script
(English, Arabic word, Arabic number) — 1-char full stroke, long words solid-medium, numbers == letters.

---

## Group 13 — Area 2a/2d GUI revamps: Arabic Browser + Color Studio · 2026-06-16 (build green + deployed, NOT tested)

**Status:** Built green (compile + 3 gates, JDK 21, 23s), jar deployed 12:20 AM (7,485,613 B). **Not
confirmed in-game.** Direction picked by the developer (2026-06-16): Browser = "group picker → grid +
colour rail"; Colour menu = "two clean zones, aligned 6×2 palettes". Both reuse existing menus/icons —
no render or slot logic duplicated.

**Bug (developer in-game 2026-06-16):** `/cb arabic list` and the Default Colours / Color Studio menu
were "very randomly made and so overwhelming, gui is bad basically." Root cause: the browser packed all
224 colour variants into one flat scroll; the colour menu wrapped swatches 8-then-4, breaking row
alignment.

**Built this pass:**
- **Arabic Browser is now a two-screen browse** (matches the bulk Step1→Step2 taste the developer liked).
  `ArabicListMenu` rewritten as a **group picker** (Arabic Letters / Arabic Numbers / English Numbers +
  English-Letters-coming-soon, each with its bundled count). New `ArabicGroupMenu` shows ONE group as an
  aligned 8-wide glyph grid (≤40 glyphs → one page, no pagination) with a **colour rail** down the left
  (black/red/green/yellow) — clicking a colour re-renders in place (cursor stays). Glyph click → existing
  `CategoryBlockMenu` (give/edit/remove); footer "Give every block here" → `/cb category give`.
- **Color Studio / Default Colours re-laid-out** (`ColorStudioMenu`): swatches now a tidy **6×2 grid**
  per channel (was 8+4 wrap), with the channel header on col 0 and the Custom #hex tile on col 8 flanking
  each band. All colour/preview/create/save logic unchanged — layout only.

**New files (1):** `gui/chest/ArabicGroupMenu.java`.
**Edited (4):** `gui/chest/ArabicListMenu.java` (rewrite → landing), `gui/chest/ColorStudioMenu.java`
(slot layout), `gui/chest/Nav.java` (+`ARABIC_GROUP` dest), `gui/chest/GuiRouter.java` (route it).

**Re-test:** `/cb arabic list` → pick a group → aligned grid; click a rail colour → variant switches in
place; click a glyph → manage menu; "Give every block here" works. `/cb arabic` → Default Colours → two
clean aligned palette zones; swatches + custom #hex still set bg/letter; Save/Create still work.

---

## Group 13 — Area 2b: `/cb arabic text` removed entirely + anvil titles fixed · 2026-06-16 (build green + deployed, NOT tested)

**Status:** Built green (compile + 3 gates, JDK 21, 25s), jar deployed 12:09 AM (7,482,397 B). **Not
confirmed in-game.** From the developer's 2026-06-16 in-game round (1a ✅ 1b ✅ "3" ✅ "4" ✅ creation
"perfect"); these were the two concrete fails/bugs that needed no design.

- **2b — `/cb arabic text` removed entirely.** It had only been *neutered* (printed "moved to word"),
  so the subcommand still existed. Deleted the `text` literal registration + `textMoved` from
  `ArabicCommands`. Now Brigadier rejects `/cb arabic text` (DidYouMean → `word`); the function lives in
  `/cb arabic word <id> <name>`.
- **Anvil titles clipped outside the box.** Shortened the maker's anvil prompt titles in `ArabicMaker`:
  "Display name for the block" → **"Block name"**, "Block id (letters/numbers)" → **"Block id"**,
  "Type or paste the text" → **"Type the text"**.

**Files:** `command/handlers/ArabicCommands.java`, `arabic/ArabicMaker.java`.

**Still open (design — brainstorm before building):** 2a Arabic Browser revamp (flat scattered scroll →
grouped/aligned) and the Default Colours / Color Studio revamp (scattered swatches → clean layout).
See `GROUP_13_ARABIC.md` Issue 2 (2a / 2d).

**Re-test:** `/cb arabic text` → should be rejected; open `/cb arabic word w MyWord` → anvil titles fit.

---

## Group 07 / Area 4 — Pre-picked bulk skips Step 1 → straight to confirm · 2026-06-15 (✅ CONFIRMED in-game 2026-06-16)

**Status:** ✅ **DONE — developer confirmed in-game 2026-06-16** ("./cb listgui bulk action passes").
Code was written ~10:40 PM (prior session) but **never compiled/deployed** — the live jar was the 8:40 PM
Arabic build. This pass: built clean (green, 3 gates, JDK 21) and deployed.
Jar `customblocks-1.0.0.jar` 11:47 PM (7,482,624 B) → `.minecraft\mods\`.

**Bug (developer, Area 4 of the Arabic round-2 pass):** `/cb listgui` → tick blocks → "Bulk actions on
N" → pick an op → wrong menu appeared — the Step-1 chooser showing **"Selected: 0 block(s)"**; the picks
were discarded.

**Cause:** `BlockListMenu` handed off to `BulkHubMenu`, but the hub's op tile always called
`resetSelection()` → wiped the picks → Step 1 reopened with 0.

**Fix (verified in code, coherent — NOT runtime-tested):**
- `BlockListMenu` "Bulk actions on N" → sets `BulkSession.prePicked = true` + `selMode = "picked"`,
  navigates to `BULK_HUB` (no Step-1).
- `BulkHubMenu`: when `prePicked`, header reads "Acting on your N picked block(s)"; op tile **keeps** the
  selection and routes to `BulkActionMenu` (param ops: property/rename/category/export) or
  `BulkConfirmMenu` (delete/duplicate/lock/favorite) — no `resetSelection()`.
- `BulkSession.prePicked` field; `resetSelection()` clears it; a fresh `/cb bulkgui` resets so the normal
  Step-1 flow is unaffected. `prePicked` honoured in `BulkCommands` too.

**Re-test:** `Reports/GROUP_07_TESTING_GUIDE.md` §A4 (① hub remembers · ② delete → Yes/No, not "0" ·
③ param op → options → confirm · ④ fresh `/cb bulkgui` still has Step 1). Golden Rule: not done until
the developer confirms in-game.

**Note:** Area 4 was the "build 4" half of the developer's instruction; Area 3 (GUI→chat-prompt removal)
remains a documented audit only (`GROUP_13_ARABIC.md` Issue 3 — 9 handoffs listed, not built). Area 2
(command merges 2a–2d) still not built.

---

## Group 13 — Arabic 1b boldness/HQ: now pixel-identical geometry to bundled · 2026-06-15 (build green + deployed, NOT tested)

**Status:** Built, build green (27s), jar redeployed 8:40 PM. **Not confirmed in-game.**

**Feedback (developer):** the round-3 render wasn't bold/high-quality enough vs the bundled blocks; wanted
it "identically generated."

**Cause:** round-3 used a thin centred black stroke + plain white fill — hairline vs the bundled art,
which dilates the glyph TWICE (PIL `stroke_width=18` black, then `=7` white on top).

**Fix:** `ArabicWordRenderer.render` now replicates the exact recipe — black layer = fill + centred stroke
`2*18@256` (dilate 18 each side), white layer = fill + stroke `2*7@256` on top (fatter core) — and renders
at **2× supersampling**, downscaled bicubic. Target reserves the full `2*black` dilation so the OUTLINED
glyph fills ~84%.

**Verified (headless, JDK 21):** generated `٣` ink box = **164×216, centered (128,128)** — byte-for-byte the
same box as the bundled `a3_black.png` (measured both). Geometry now identical; only the originals' faint
white shading differs. PNGs eyeballed bold + crisp.

**Tunable if developer wants heavier/lighter:** constants `18f` (black), `7f` (white), `0.84f` (fill) in
`ArabicWordRenderer.render`.

---

## Group 13 — Arabic 1b size fix (glyph was tiny/high) · 2026-06-15 (build green + deployed, superseded by the entry above)

**Status:** Built, build green (full build 16s), jar redeployed 8:30 PM. **Not confirmed in-game.**

**Bug (developer round 2, screenshot of `٣`):** 1a (bg) passed, but the generated Arabic glyph rendered
**~half size and floated high** vs the bundled block; the English `Te` block looked great. → **Cause:**
`ArabicWordRenderer` auto-fit capped the font point size at `size*0.95`. A single Arabic glyph has small
ink per em, so it needed a larger point size than the cap allowed → clamped → tiny. Latin glyphs have
larger ink/em so never hit the cap (why `Te` was fine).

**Fix:** `ArabicWordRenderer.render` — removed the point-size cap (now `[8, size*8]`), reserve the outline
width in the target, and **refine the fit once against the measured ink** so the OUTLINED glyph fills
~84% of the square (matching the bundled art's MAX_RENDER≈215/256). Added `outlineLongest()` helper.

**Verified before shipping (headless harness, JDK 21):** rendered `٣`/`هلا`/`Te`/mixed at 256² with the
real `arabtype.ttf` — all fill **84%** on the longest axis, centered at (128,128); PNGs eyeballed (big,
bold, outlined, cursively joined). Math + pixels both check out.

**Re-test:** guide Test 2 (compare a generated number/word vs `arabic_a3_black`).

---

## Group 13 — Arabic rendering Area 1: HUD + word-create CONFIRMED; bg/outline awaiting · 2026-06-15

**Status:** Area 1 built + deployed + partially confirmed in-game (round 2). This is Area 1 of a 4-area
pass the developer requested (rendering · commands · chat-prompts · bulk bug), taken one at a time.
Areas 2–4 not started.

**In-game round 2 (developer, 2026-06-15, with screenshots):**
- ✅ **1c HUD** confirmed fixed ("HUD is fixed" — new word block shows `hlo`/`Hlo`).
- ✅ **Word create** confirmed (`/cb arabic word arg arg` made the block, top-right of shot).
- 🎯 **1a background + 1b outline/font** look matched in the screenshot (`هلا`/`هلو` vs bundled `ل`/`ا`) —
  awaiting explicit confirm. Tests 1–2 in the guide.
- Developer (correctly) noted Areas 2–4 still missing: `/cb arabic list` still opens the hub, `/cb arabic`
  GUI lacks the customization/WOW, etc. — all queued.

**Docs expanded this pass (no jar built — docs only):** `GROUP_13_ARABIC.md` now captures the full 4-issue
report (1a/b/c, 2a–d, 3, 4) with cause/fix/status; `Reports/GROUP_13_TESTING_GUIDE.md` rewritten
issue-by-issue — Tests 1–10, each its own numbered section with ✅/🎯/⏳ status (Issue 4's real fix +
tests live in Group 07, cross-referenced).

---

## Group 13 — Arabic rendering: word style now matches bundled art + HUD sync · 2026-06-15 (written, build green)

**Status:** Written, build green (compileJava + all 3 gates, 13s); jar deployed. See the entry above for
the in-game round-2 results.

**Root causes found (verified in code / by sampling the bundled PNGs):**
- **Background mismatch:** generated word blocks used `#000000`; the bundled letter art is `#0A0A0A`
  (sampled RGB 10,10,10, opaque, 256×256). 10-point gap = the visible mismatch.
- **"Font" mismatch:** the bundled letters are hand-rendered PNGs (white glyph + thick black outline
  on `#0A0A0A`), made by the old `generate_arabic_letters.py` from `arabtype.ttf` — the same font the
  word renderer already uses. The words already join cursively (arabtype shaping); only the *style*
  differed (flat white, no outline, pure-black bg).
- **Missing HUD:** the HUD reads `ClientSlotCache`, refreshed only by `HudSync.sendTo()`. The Arabic
  create path never called it, so a new word block's slot never reached the client → no look-at HUD.
  Bundled blocks work because they're synced at join.

**Built this pass:**
- `ArabicWordRenderer.render`: paints with the bundled-art recipe — `#0A0A0A`-honouring bg, thick
  black outline (`~18px @256`) drawn from the shaped glyph outline, then white fill; auto-fit on the
  real ink bounds at ~84% (was 78%). arabtype shaping preserved → words stay joined.
- `ArabicCommands`: word block bg `0xFF000000 → 0xFF0A0A0A`; `colorArgb` aligned to the bundled
  palette (red `#FF0000`, black `#0A0A0A`); `HudSync.sendTo(player)` after word/text/letter creation
  so the look-at HUD appears immediately.

**Re-test:** `/cb arabic word greet Greeting` → type Arabic → Single texture block → compare side-by-side
with a bundled letter (`/cb arabic letter alef`): background + outline + glyph should match; look at the
new block → HUD shows id + name. Outline thickness is tunable if it reads too thick/thin.

---

## Group 05 — Silent Resource Pack: server-side join fix landed, but delivery still BROKEN on modded clients · 2026-06-15 (root cause found, fix pending)

**Status:** Server-side change built + deployed + proven in logs. **Textures still do not load** on a
modded client — real root cause identified; the actual fix is **not built**.

**Built this pass (server side — works, but not enough alone):**
- `ResourcePackServer`: `AWAITING_FIRST_PACK` queue — a player who joins before the async pack build
  finishes is queued and served when the build completes (fixes the join-vs-build race). Cleared in
  `start()`/`forget()`. Per-send logs added: `Sent resource pack to <player> (id …)` and
  `Join before pack ready — <player> queued`.
- `ClientCommonNetworkHandlerMixin`: recognises our pack by its `"CustomBlocks textures"` label and
  silent-accepts it regardless of flag timing (other servers' packs untouched).

**Why it's still broken (proven from the developer's logs/files):** the server now sends correctly
(two `Sent resource pack` lines logged), but the client never applies it — no reload, no download, no
dialog; `server-resource-packs` cache has nothing newer than Jan 2. The mod's own
`client/package-info.java` says *modded clients generate the pack locally instead of downloading the
HTTP pack* — but that **`ResourcePackGenerator` class was never built** in CustomBlocks-B. HTTP push
was only ever the path for vanilla clients. Visible textures come from a stale `resourcepacks/
CustomBlocks` (May 17, old mod) the new mod never updates.

**Fix (NOT built):** build the client-side `ResourcePackGenerator` (recycle old project's proven
version, split for the 500-line gate; pull textures from the existing `/tex/<id>` route), silent
client reload, skip the self-push for modded clients. Keep HTTP push for vanilla friends; fix
`httpHost` for remote delivery separately. Detail in `GROUP_05_RESOURCE_PACK.md` + Reports §3.

---

## Group 12 — Export Dashboard: Blueprint item + download-link/ZIP tidy · 2026-06-14 (written, NOT tested)

**Status:** Written, build green (compile + all 3 gates, 26s), jar copied to `.minecraft\mods\`.
**Not tested in-game.** Scope set with the developer: vault share-code (G12.7), marketplace (G12.8)
and the advanced formats (litematic/.schem/vanilla resource pack) are **deferred — marked partial**,
to revisit at the end. This pass = the offline core: the Blueprint item + closing the small gaps so
G12.1–G12.6 pass from the dashboard.

**Scan first (what already existed, so it was NOT rebuilt):** `/cb export` already opens the chest
dashboard (`Dest.EXPORT_DASHBOARD`); single-block JSON export + a clickable link already worked via
the chat command; category ZIP already worked. The export *system* was kept — only the gaps were
closed.

**Built this pass:**
- **Blueprint item (G12.6) — new offline share path.** `item/Blueprint.java`: a renamed PAPER carrying
  the block's schema-v1 recipe in its `CUSTOM_DATA` component (key `cb_blueprint`) + a readable tooltip
  (id/glow/hardness/sound/category) + glint. Developer chose the simple named-item look over a per-block
  textured icon. `command/handlers/BlueprintCommands.java`: `/cb exportblock <id>` gives the Blueprint;
  `/cb importblock` (held in hand) recreates the block; `/cb importblock <code>` is the deferred vault
  path (graceful "not set up yet" message). Recipe (de)serialise reuses `BlockExporter` — one schema for
  Blueprint, single-JSON and category ZIP.
- **Export All → one ZIP (G12.4).** `BlockExporter.exportAllZip()` → `cloud_exports/all-<stamp>.zip`
  (each block = `<id>.json` + `<id>.png`); `/cb export zip` + an "Export All (ZIP)" dashboard tile, both
  with a clickable `[download]` link.
- **Download links tidied.** Single-block JSON **and** PNG now land in `cloud_exports/` and emit a
  clickable `[download]` link (PNG link was previously broken — the file went to `exports/` while the
  `/png/` route served `cloud_exports/`). New `/zip/` HTTP route + `getZipUrl()` serve bundle ZIPs.
  `/export/` route now reads `cloud_exports/` to match `exportOne`.
- **Category export link (G12.5).** `/cb category export` now gives a clickable `[download]` link
  instead of just a copy-path button (net-zero lines — the file is at the 394/400 gate).
- **Dashboard per-block view** now offers explicit **Download JSON**, **Download PNG** and
  **Generate Blueprint** tiles (the test wording: "click the block → Download JSON").

**New files (3):** `item/Blueprint.java`, `command/handlers/BlueprintCommands.java` (+ register in
`CommandRegistrar`).
**Edited (5):** `core/BlockExporter.java` (toJson public + importJson + exportAllZip + single exports →
cloud_exports), `network/ResourcePackServer.java` (/zip route + getZipUrl + /export reads cloud_exports),
`command/handlers/UtilityCommands.java` (PNG link + `/cb export zip`), `gui/chest/ExportDashboardMenu.java`
(per-block + All tiles), `command/handlers/CategoryCommands.java` (download link).

**Deferred (marked partial, per the developer):** G12.7 share-code, G12.8 marketplace (both need the
Cloudflare Vault Worker deployed — `vaultEndpoint` is empty, no worker in repo), litematic/.schem/vanilla
resource pack, and the share-code/conflict GUI. Note: `[download]` links resolve from `httpHost`
(default `127.0.0.1`) — fine for single-player / same machine; remote friends need a reachable host:port
(server has IP-detect issues + blocked 8080-8081, so links are local-first for now).

**Test:** G12.1 `/cb export` → dashboard; click a block → Download JSON / Download PNG → file in
`cloud_exports/` + working `[download]`; "Export All (ZIP)" → `all-<stamp>.zip` + link; click block →
Generate Blueprint → Blueprint in inventory with the block's tooltip; hold it → `/cb importblock` →
block recreated. Per the Golden Rule, NOT done until confirmed in-game.

---

## M1 / D — `/cb tolerance` strength (0-100) + clickable mode prompt · 2026-06-10 (written, NOT tested)

**Status:** Written, build green (compile + 3 gates), jar in `.minecraft\mods\`. **Not tested in-game.**
Full status board: `Reports/GROUP_06_TRACKER.md`.

**Built (D — from scratch, NOT copied from the old code):**
- `CustomBlocksConfig.backgroundTolerance` (int 0-100, default 30) + load(clamp 0-100)/save.
- `BackgroundRemover.apply(input, mode, tolerance)` — strength is now a parameter: 0-100 maps linearly
  onto CIE-LAB ΔE [0, 40] (one `MAX_DELTA_E` constant); fringe = ΔE + 6. `apply` no-ops when mode is
  none **or** tolerance ≤ 0. `snapBackgroundBlack` takes tolerance and no-ops the same way.
- `/cb tolerance <0-100>` (ConfigCommands): `0` → sets mode none + "turned off" message; `>0` → stores
  the strength and sends a **clickable** chat prompt — `[ Background Only ]` / `[ Background + Enclosed
  Areas ]` (RUN_COMMAND → `/cb config background BgRemove` / `BgRemove&More`). `/cb tolerance` (no arg)
  shows current strength + mode.
- Config GUI background row now shows `Strength: N/100`.

**Comparison vs the OLD tolerance code (as requested):**
- OLD: one raw ΔE int gated behind THREE coupled flags (`bgRemovalEnabled` + `colorToolBackgroundMode`
  + `bgRemovalTolerance`); "tolerance 0 = off" special-cased + duplicated across ~4 near-identical
  methods (`replaceBackground` / `…WithColor` / `…WithFringeTolerance`); a dead `bgRemovalUseYcbcr`
  alternate-distance branch; hardcoded `fringe = tol+15`. The handoff itself flagged the old config
  as "the buggy part."
- NEW: one friendly 0-100 strength → one ΔE mapping; ONE `apply()` (no duplicated variants); a single
  clean no-op guard (mode none OR tolerance ≤ 0); no dead branch; reach chosen by a clickable prompt
  instead of a string the user must memorize; atomic config. Clean separation of **strength**
  (tolerance) vs **reach** (mode) vs **on/off** (tolerance 0).

**Files:** `BackgroundRemover.java`, `CustomBlocksConfig.java`, `ConfigCommands.java`,
`CreationCommands.java`, `ConfigMenu.java`. Per the Golden Rule, NOT done until tested in-game.

**Queued next (approved, NOT built — one tested pass each):** A eyedropper · B despeckle · C preview
(all toggleable in config + `/cb config`) · M2 triangle fills · **M3 hex recolour of the colour
markers + "recolour existing blocks?" GUI prompt + marker-texture hex** · M4 per-face paint. See tracker.

---

## M1 follow-up: bg mode names/commands, true-black fix, config row + chat marked partial · 2026-06-10 (written, NOT tested)

**Chat formatting = PARTIAL (polish later).** Same convention as the GUIs + item lore — chat tone /
formatting works but the developer wants a dedicated polish pass later. Not a regression.

**Background removal — naming + commands (exact mapping requested):**
- none   → menu "No Background Removal"            · arg `NoBgRemove`
- edges  → menu "Background Removal Only"          · arg `BgRemove`
- closed → menu "Background + Closed Areas Removal" · arg `BgRemove&More`

Internal config value stays none/edges/closed; `BackgroundRemover.fromArg / displayName / commandArg /
next` do the mapping. `/cb config background` now takes a greedy arg (so the `&` in `BgRemove&More`
parses) with tab-suggestions. Added an editable Background Removal row to the config GUI
(`ConfigMenu`, slot 26) that cycles the mode.

**True-black fix (3a):** Stage 3 now composites leftover transparency against BLACK (was white → gray
halos); added a post-resize near-black snap (every channel ≤ 24 → #000000) to kill the dark-gray halos
bicubic downscaling leaves around the black background. The remaining confetti-zone grayness is an
*accuracy* issue (the algorithm doesn't recognize decorative confetti as background) — brainstorm pending.

**Files:** `BackgroundRemover.java` (mapping + black composite + snap), `ConfigCommands.java` (args),
`ConfigMenu.java` (row), `CreationCommands.java` (snap wire). Per the Golden Rule, NOT done until tested.

---

## M1 — Background remover (CIE-LAB, 3 modes, black fill) · 2026-06-10 (core slice written, NOT tested)

**Status:** Core slice written, build green (compile + 3 gates, 18s), jar in `.minecraft\mods\`.
**Not tested in-game.**

**Decision (developer):** removed background → **opaque BLACK, exactly like the old version**. Render
stays opaque (slot blocks are solid — confirmed `cube_all`, no `render_type`, no RenderLayer reg), so
no transparency; a removed background is flat black.

**Done this pass (4 changes):**
- NEW `image/BackgroundRemover.java` — clean recode of the old `ImageProcessor.replaceBackground`:
  corner-median sample → CIE-LAB ΔE flood-fill from every border pixel → (CLOSED only) enclosed-area
  pass → 1px anti-fringe dilation → paint background opaque black, white-composite any leftover
  transparency. Modes: `none` / `edges` / `closed`. ΔE tolerance = 12.0 (constant), fringe = +15.
- `CustomBlocksConfig.backgroundMode` ("none" default) + load/save; normalized via `BackgroundRemover`.
- `CreationCommands.applyTexture` — runs `BackgroundRemover.apply(raw, mode)` on the RAW image BEFORE
  `toBlockPng`.
- `/cb config background [none|edges|closed]` setter (`ConfigCommands`), mirrors silentpack/hud.

**Deliberate deviation from the handoff:** the handoff said hook AFTER `toBlockPng`; that runs on the
padded square, and transparent padding would poison corner-sampling on non-square images. So it runs
BEFORE resize, matching the OLD project's order (`replaceBackground` → resize). Bug-free + parity.

**Scope notes / deferred to M1 pass 2:**
- Applies to FUTURE (re)textures only — existing blocks unchanged until retextured.
- Static-image path only (`toBlockPng`); GIF/video untouched.
- Config-GUI row (`gui/chest/ConfigMenu`) + per-mode lore deferred (kept this slice minimal/testable).
- Tolerance 12.0 is a sensible default, NOT the old's exact value (unknown) — tune if too strong/weak.

**Test:** `/cb config background edges` (or `closed`) → create/retexture a block with a solid-colour
background → that background should be solid black on the block; `none` → texture used as-is. Per the
Golden Rule, NOT done until confirmed in-game.

---

## Group 06 — Handoff fixes: Omni air-click, prefix, lore, Magic GUI · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** All 4 handoff items confirmed by the developer.
Build green (compile + all 3 gates); jar copied to `.minecraft\mods\`.

**Done + confirmed:**
- **Omni-Tool sneak-click now works in the air** — `item/OmniToolItem.java`: added a
  `use(World, PlayerEntity, Hand)` override. Sneaking opens the mode GUI in the air / on any
  non-custom block; `useOnBlock` still handles custom blocks (returns SUCCESS, so no double-fire).
- **[CB] prefix unified** to the black-bracket chat format everywhere — `command/Chat.java`
  (`HUD_PREFIX` removed, `Chat.tool()` → `PREFIX`) + `gui/chest/CbChestHandler.java`. Developer's
  accepted trade: black brackets are dim on the dark hotbar bar.
- **Item lore humanized** for every tool/marker — `OmniToolItem`, `DeleterItem`,
  `RainbowRectangleItem`, `ShapeMarkerItem` (all 8 markers), plus new tooltips on `LuminaBrushItem`
  + `ChiselItem` (had none).
- **Magic Items GUI → double chest** — `gui/chest/MagicMenu.java` + `core/MagicItemsManager.java`:
  6 rows, light-blue frame, 3 hand tools on the top interior row, 8 markers in a 4-colour grid
  (each colour's Square directly above its Triangle), header + "Marker Shapes" label + controls;
  enabled items glint, disabled dimmed. 8 markers seeded with enable/disable persistence.

**⚠️ Item lore = PARTIAL (polish later).** Confirmed readable in-game, but marked **partial** —
same as the GUIs — for a dedicated lore polish pass later: tone consistency across all items, more
per-item flavour, and the Shape / Rainbow lore likely wants tightening once their real mechanics
(M2–M4) land. Treat any further lore work as a later pass, not a regression.

**Tab placement:** developer chose to keep two separate tabs. Fabric exposes no API to pin a custom
tab's page or force adjacency, so no tab code changed (registration already orders Blocks→Tools).

---

## Group 06 — Tools: safe fixes pass (textures/lore/GUI/tab) · 2026-06-10

**Status:** Safe fixes written, build green. **Not tested in-game.** Remaining mechanics queued
as careful individual builds (see below).

**Done this pass (low-risk, deterministic):**
- Restored the **old Deleter + Rainbow Rectangle textures** (copied from the old project).
- Redrew the **Omni-Tool texture** — clearer: steel/gold tool head on a rainbow handle, bold
  outline, readable from a distance.
- Simplified **Omni-Tool + Deleter lore** (short, plain words). Shape/Rainbow lore deferred to
  when their real mechanics land (so it isn't written twice).
- **Magic Items menu** seed + mapping updated (lumina_brush/chisel → Omni-Tool / Rainbow
  Rectangle / Deleter). Tools tab icon switched off the removed Lumina Brush → Omni-Tool.
- Confirmed creative tabs register Blocks→Tools (Tools should sit right after Blocks).

**Queued mechanics (need their own careful builds — clean-room rebuild lacks the systems):**
`ImageProcessor` only resizes (no background removal yet); `TextureStore` is one PNG per block
(no per-face / no colour variants). So:
- **M1** CIE-LAB background remover + 3-mode config (foundation).
- **M2** Triangle → fill background with the colour (needs M1).
- **M3** Square colour-variant + Black-square fallback (`blockID_color`, else original).
- **M4** Rainbow Rectangle per-face URL paint (per-face textures — biggest).

---

## Group 06 — Tools (Omni-Tool + shapes + textures) · 2026-06-10

**Status:** Code written, build green (compile + all 3 gates), all 11 textures + models packaged in
the jar. **Not yet tested in-game.** Testing guide: `Reports/GROUP_06_TESTING_GUIDE.md`
(tests G06.2a–h + G06.A–D).

**Corrected Omni composition (developer fix):** Omni-Tool = **Brush (Glow) + Chisel (Hardness) +
Rainbow Rectangle (Area)**. The earlier Eyedrop/Diamond-Triangle mode was a scrapped concept and
was removed. Omni-Tool GUI marked **partial** (polish later).

**Restored tools (textures reused from old project, code from scratch):** 8 colour/shape markers
(Green/Yellow/Red/Black × Square/Triangle) + the Rainbow Rectangle. Deleter kept, new trash texture.

**Textures (`tools/gen_tool_textures.py`):** green/yellow/black square+triangle copied verbatim from
the old project; red_square/red_triangle reproduced with the old project's exact draw algorithm
(no red PNG was captured); new sprites drawn for `rainbow_rectangle`, `deleter` (trash bin) and
`omni_tool` (chisel head + brush bristles + rainbow base). 11 PNGs + 11 models + 9 lang entries.

**New files (5):** `item/OmniToolItem.java`, `core/OmniToolState.java`, `gui/chest/OmniMenu.java`,
`item/RainbowRectangleItem.java`, `item/ShapeMarkerItem.java`, `core/AreaSelection.java` (per-player
two-corner select shared by Rainbow Rectangle + Omni Area). Plus `tools/gen_tool_textures.py`.

**Edited files:** `item/ToolItems.java` (register omni + rainbow + 8 shapes), `ToolCommands.java`
(brush→Omni[Glow], chisel→Omni[Hardness], `/cb omni`, `/cb rectangle`; deleter unchanged + lore),
`CustomBlocksMod.java` (tools tab = Omni, Rainbow Rectangle, Deleter, 8 shapes), `Nav.java` +
`GuiRouter.java` (OMNI dest), `HelpTopics.java` (Tools category), `item/DeleterItem.java` (lore),
plus the 11 item models repointed to `customblocks:item/*` and lang entries.

**Decisions:** sneak+right-click = open config GUI (so glow/hardness cycle forward-only); Area =
self-contained two-corner selector (bulk ops that consume it come later); shape markers are simple
taggers for now; Omni mode shown in item name (no per-mode texture swap yet).

**Remaining:** Step 3 = held-block dynamic glow (networked client light — highest risk, isolated).

---

## Group 05 — Silent Resource Pack Delivery · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** G05.1–G05.6 all pass. Follow-up polish:
texture-applied message is now silent-pack aware (no "accept the prompt" text when silent).
Testing guide: `Reports/GROUP_05_TESTING_GUIDE.md`.

**What it fixes (deep-search findings → fixes):**

| Finding | Before | Fix |
|---|---|---|
| 17.1 | Vanilla "download resource pack?" dialog appeared (`required=false` + prompt) | Client mixin on `ClientCommonNetworkHandler.createConfirmServerResourcePackScreen` takes the vanilla silent add path (`addResourcePack`) instead of building the prompt |
| §E | No `silentPack` config | Field (default true) + `/cb config silentpack` + Config-GUI row; synced to clients via `SilentPackPayload` on join + on change |
| scope | — | Client only silent after OUR server says so; resets on disconnect → never auto-accepts other servers' packs. Forced (`required=true`) packs left to vanilla |
| §debounce | `updatePack` collapsed only concurrent builds | ~500ms time-debounce; rapid edits → one rebuild; added `Rebuilding resource pack…` log line |

**New files (3):** `mixin/ClientCommonNetworkHandlerMixin.java`, `client/SilentPackState.java`,
`network/payloads/SilentPackPayload.java`.

**Edited files (7):** `customblocks.mixins.json` (client mixin), `CustomBlocksConfig.java`
(silentPack), `CustomBlocksMod.java` (register + send on join), `CustomBlocksClient.java`
(receiver + disconnect reset), `ResourcePackServer.java` (debounce + log line),
`ConfigCommands.java` (`/cb config silentpack`), `ConfigMenu.java` (Silent Pack row).

---

## Group 04 — Chat Messages & Command Communication · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** Brand kept (`[CB]` + ✔/✖), wording
upgraded. Testing guide: `Reports/GROUP_04_TESTING_GUIDE.md`.

**What it fixes (deep-search findings → fixes):**

| Finding | Before | Fix |
|---|---|---|
| 17.2 | Terse message bodies (`Created g04b (slot 12)`, `'g04a' is locked`) | Kept `[CB]` tag + ✔/✖ glyphs (the brand); upgraded the wording — bodies rewritten as clear, helpful sentences |
| Q5 | DidYouMean missing entirely (raw Brigadier error on typos) | `DidYouMean.java` — Levenshtein + prefix boost, fallback arg named `subcommand` (LANG1-safe), modes smart/always/off |
| Q5 | `/cb help` missing entirely | Chest GUI browser (9 categories → commands → click pre-fills chat via new `ChatPrefillPayload`) |
| — | `/cb welcome` missing | Added with 3 clickable quick-start actions |
| — | `IncidentRecorder` had zero callers — incidents log could never fill | Wired: texture download failures, import failures, pack rebuild failures |
| — | No `didYouMean` config | Field + `/cb config didyoumean` + click-to-cycle row in the Config chest GUI |

**New files (5):** `command/DidYouMean.java`, `command/handlers/HelpCommands.java`,
`gui/chest/HelpTopics.java`, `gui/chest/HelpMenu.java`, `network/payloads/ChatPrefillPayload.java`.

**Edited files (13):** `Chat.java` (prefix/glyphs removed), `CustomBlocksConfig.java` (didYouMean),
`CommandRegistrar.java` (HelpCommands + fallback last), `ConfigCommands.java` (didyoumean cmd),
`ConfigMenu.java` (cycle row), `CreationCommands.java`, `AttributeCommands.java`,
`UtilityCommands.java`, `ManagementCommands.java`, `TemplateCommands.java`, `VideoCommands.java`,
`ChestGuiCommands.java`, `GuiCommands.java` (message tone), plus `ResourcePackServer.java`
(incident on pack failure), `Nav.java`/`GuiRouter.java` (HELP dest), `CustomBlocksMod.java`/
`CustomBlocksClient.java` (payload registration).

---

## Group 02 — Chest GUI Core Infrastructure · 2026-06-09

**Status:** Code written and statically verified. **Not yet compiled or tested in-game.**

**What it fixes (audit rows / issues closed):**

| Source | Item | Before | Fix |
|---|---|---|---|
| 17.3 | Chest GUI replaced by screens | Screen-based main menu | Server-side chest GUI system; screens removed |
| 17.5 | `/cb editor <id>` shortcut | Missing (only `/cb gui block <id>`) | `/cb editor <id>` added as a direct alias |
| G01 row 41 | `gui` | Screen-based | Opens chest dashboard |
| G01 row 42 | `menu` / `dashboard` | Chest menu missing | Open the chest dashboard (aliases of `/cb`) |
| G01 row 43 | `editor` | Missing | `/cb editor <id>` added |
| G01 row 44 | `listgui` | Missing | Added — chat list of GUIs with clickable `[open]` |
| G01 row 45 | `blocks` (block list GUI) | Chat `list` only | Block list chest GUI added |
| G01 row 46 | `redogui` | Missing | Visual redo browser added |
| G01 row 47 | `undogui` | Missing | Visual undo browser added |
| G01 row 139 | `unsuppress` | Missing | Added |
| G01 row 145 | `history` GUI | Chat undo/redo only | Mutation-log history chest GUI added |
| G01 rows 151/152 | `magicitems` / `editmagicitems` | Missing (open question) | Implemented per the Group 02 spec |
| Group L | `rp pause` / `rp resume` / `sync` | Missing | Added resource-pack pause/resume + force-sync |

**New files (15):**

*Framework — `gui/chest/` (6):*
- `Icons.java` — stained-glass filler panes + item-icon builder (name / lore / glint via `DataComponentTypes`).
- `Nav.java` — per-player back-stack; `Dest` enum (MAIN, BLOCK_LIST, EDITOR, UNDO, REDO, HISTORY, MAGIC, MAGIC_EDIT) + `MenuKey(dest, arg, page)` record.
- `ChestMenu.java` — generic 1–6 row chest container with per-slot click callbacks.
- `CbChestHandler.java` — screen-handler click router (mirrors the proven old `CbScreenHandler`).
- `GuiRouter.java` — open / navigate / repage / back / render, and command delegation (`runCommand`, `runAndReopen`, `promptCommand`, `confirmCommand`) via `executeWithPrefix`.
- `Layout.java` — shared paginated footer (back / prev / page / next / close).

*Menus — `gui/chest/` (6):*
- `MainMenu.java` — the `/cb` dashboard (Block List, Categories, Templates, Macros, Arabic, Diagnostics, Config, Close).
- `BlockListMenu.java` — paginated block list; click a block to open its editor.
- `EditorMenu.java` — per-block editor; slots delegate to give / setglow / sethardness / setsound / setcollision / setcategory / rename / retexture / note / delete.
- `UndoMenu.java` — visual undo/redo browser (click an entry to step to that point).
- `HistoryMenu.java` — server mutation-log view (timestamp / player / action / block ID).
- `MagicMenu.java` — magic-items list and edit mode.

*Core + command (3):*
- `core/MutationLog.java` — in-memory audit log feeding the history GUI.
- `core/MagicItemsManager.java` — magic-item registry + enabled-state.
- `command/handlers/ChestGuiCommands.java` — registers all Group 02 commands.

**Edited files (5):**
- `command/CommandRegistrar.java` — register `ChestGuiCommands` after `GuiCommands`.
- `command/handlers/GuiCommands.java` — removed the old screen `gui` / `admingui` registrations (kept `edithud`).
- `core/UndoManager.java` — added `undoStack(uuid)` / `redoStack(uuid)` reads + a `MutationLog` hook in `push()`.
- `command/handlers/HistoryCommands.java` — added public `undoOnce` / `redoOnce` for the visual menus.
- `network/ResourcePackServer.java` — added `pause` / `resume` / `isPaused` / `syncToAll` / `unsuppress`; `updatePack()` no-ops while paused, `sendToPlayer()` no-ops while suppressed.

**Design note:** every mutating editor action delegates to the existing, tested `/cb` command rather
than re-implementing block logic — so locking, undo recording, dynamic lighting, and chat feedback
all behave identically to typing the command. Editor argument formats were verified against
`AttributeCommands`.

**Verified (static only):**
- File-size gate (§9.3): all files within limits (largest handler `ChestGuiCommands`, 156 / 400 lines).
- Mojibake gate + sound gate: 0 hits.
- Command-literal collision scan: no duplicate literals introduced.
- API cross-check against real source: `SlotManager`, `SlotData`, `SlotBlock`, `ToolItems`, `Chat`,
  `executeWithPrefix`, `DataComponentTypes`, `ClickEvent` / `HoverEvent` constructors — all confirmed.
- Covers tests G02.1–G02.11 (see `GROUP_02_TESTING_GUIDE.md`).

**Known limitations / not verified:**
- Not compiled or run where authored — compile errors remain possible until `gradlew build` on the dev PC.
- `/cb history` does not yet implement the optional shift-click “filter by player” (spec §6).
- `GuiCommands.openGui` is now unused (javac warning only, not an error).
- Golden Rule: none of this is DONE until confirmed in-game.

---

## Group 01 — Legacy Feature Audit · 2026-06-09

**No code fixes** — this group is an audit only. It produced `Reports/GROUP_01_AUDIT_REPORT.md`
(153-row command audit, GUI-workflow gap list, and config-field coverage). Developer sign-off (Test
G01.4) granted, unblocking Group 02. Listed here for completeness; nothing was changed in code.

---

_Groups 03–25: not started — no fixes implemented yet._


---

## Finale Fix — Group 02 reported issues (12 fixes) — IMPLEMENTED (UNCOMPILED, pending in-game test)

### 1. Command registration & logic
- **`/cb` now works.** `CommandRegistrar` registers the `cb` alias with `.redirect(node).executes(ChestGuiCommands::openDashboard)`. A bare redirect does not inherit the target's `executes`, so `/cb` alone used to fail; now it opens the dashboard. Legal because the `cb` node has no children of its own.
- **`/cb gui block <id>` removed.** Only `/cb gui` (dashboard alias) and `/cb editor <id>` remain.
- **`/cb unsuppress` description clarified** — now explains it re-enables the server resource-pack download prompt that a player's "do not show again" had silenced.

### 2. GUI aesthetics
- **Glass-pane framing** via new `Icons.accent()` (light-blue pane). Applied to Main Menu, Editor, Magic Items, and the new Confirm menu.
- **Main Menu rebuilt** as a 6-row "double chest" dashboard (`CustomBlocks Dashboard`) with header + framed rows (was a small 3-row menu).
- **Editor** title fixed to `CustomBlocks Editor - ID: <id>` (was `Edit: <id>`) + accent frame. (Visual shape editor wiring deferred per request.)
- **Magic Items** shrunk from 6 rows to a single row, items centred between Back/controls.

### 3. UX & functionality
- **Cursor reset bug fixed.** `GuiRouter` now refreshes the open screen IN PLACE (`CbChestHandler.refreshWith` + `syncState`) when the new menu has the same rows + title (toggles, paging), instead of reopening (which snapped the cursor to centre). Reopen only happens when navigating to a different screen. Mirrors the old build's `refreshScreen`/`refreshWith` pattern.
- **`/cb history` entries clickable** — clicking opens that block's editor (or notes it no longer exists).
- **`/cb config` confirmation GUI restored** — new `ConfirmMenu.configWarning` (Yes/No) opens before the real config, mirroring the old `openConfigWarningGui` (No → back, Yes → runs config).

### 4. Text / lore / chat
- **Prefix unified** to `Chat.PREFIX` / `Chat.HUD_PREFIX` across GUIs and RP commands (removed stray `[CustomBlocks]` and `§b[CB]` player-facing strings; logger lines unchanged).
- **Undo/Redo/History lore enriched** — friendly action names (e.g. "Changed glow"), block id, position, and an accurate "undo/redo the last N change(s)" hint instead of bare labels like "delete 10".

### Files touched
- `command/CommandRegistrar.java`, `command/handlers/ChestGuiCommands.java`
- `gui/chest/`: `ChestMenu`, `CbChestHandler`, `GuiRouter`, `Nav`, `Icons`, `EditorMenu`, `UndoMenu`, `HistoryMenu`, `MainMenu` (rewrite), `MagicMenu` (rewrite), `ConfirmMenu` (new)

### Status
Static checks pass (no leftover prefixes, no mojibake, all under size gates, braces balanced). NOT compiled (no build in sandbox). Must be verified in-game before being marked done.

### Note on scope
The "restore all old wired chest GUIs" deep-search request was scoped down to the **config confirmation** example only (the one concrete old wired GUI identified: `openConfigWarningGui`). Other old GUIs were inventoried in the testing guide appendix but not mass-recreated.
