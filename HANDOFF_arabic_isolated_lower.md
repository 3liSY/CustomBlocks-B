# Handoff — Group 13 Arabic: lower floaty isolated letters

**Date:** 2026-06-19. **Repo:** `Coding/CustomBlocks-B/` (active). Build = JDK 21 (see below).

## The one task left
Isolated Arabic letters (the auto-join letter block, ISOLATED form) render **a little too high** in
their tile. Dev wants them **nudged DOWN a little bit** so they look natural in a word.

- **NOT** centered, **NOT** anchored to the floor/ground. Just a *small* drop. Dev was explicit and
  frustrated about this: "lower them a little bit down not at the ground."
- Applies to **short, no-descender letters that float**: dal د, dhal ذ, ta-marbuta ة, and similar
  compact letters ("and relevant ones").
- Letters with a tail/descender (waw و, ra ر, zay ز, noon ن, meem م, ya ي, jeem ج, ha ح, lam ل) already
  sit fine — **do NOT move them**. Connected tiles (initial/medial/final, the ones with kashida hands)
  **do NOT move** either — their bars must stay on the baseline so joins line up.

## What's already decided / done (don't re-litigate)
1. **Isolated letters use the FONT, not bundled art.** Same font/stroke/size/baseline as the connected
   letters — the ONLY difference is no connecting hands. Dev said "yes" to this. It is ALREADY how the
   runtime works: `ArabicTileRenderer.render(letter, ISOLATED, …)` draws the glyph with no kashida.
2. The renderer routing is done: `ArabicLetterBlockEntityRenderer.build()` sends **all** forms (incl.
   isolated) through `ArabicTileRenderer`. (These 3 files are new/untracked, see below.)
3. Lowering amount tuning is the ONLY open question. Approaches tried and **rejected by dev**:
   - bbox-center (100%) → "still floating."
   - centroid-center → "still floating up."
   - floor-anchor (bottom to descender line) → "not at the ground fuck" (too far).
   - dal-only @130% bbox was close-ish but dev wants ALL floaty letters lowered, just a *little*.

## Next step (keep it SIMPLE — see dev notes)
Pick a **small** lowering for floaty isolated letters and get it approved on ONE before/after image,
then bake + build. Suggested starting point: lower a no-descender isolated letter by a *small fixed
fraction of tile height* (try **~8–10%**), OR ~30–40% of the way toward center (NOT 100%). Show
`داود` (dal floats) and `مكة` (ta-marbuta floats) before/after at that amount, let dev tweak the number.

"Floaty" = letter whose ink has little/no descent below the baseline. Easiest detector: measure the
glyph's descent (ink below baseline); if ~0, it's a floater → nudge it down; if it has real descent,
leave it. (Tailed letters self-exclude.)

### Where to bake it (runtime)
`src/main/java/com/customblocks/arabic/ArabicTileRenderer.java`, in `render(...)`, right after:
```java
Shape outline = AffineTransform.getTranslateInstance(tx, baseY).createTransformedShape(raw);
```
If `form == ArabicJoining.ISOLATED`, add a small downward shift to the outline BEFORE the kashida/Area
work (isolated has no kashida so it's safe). Scale the nudge by the working size like `fs`/`baseY` do
(`* h/(float)H`). Don't touch connected forms.

## Preview harness (decide by eye BEFORE building — dev rule)
`tools/render_preview/`. Standalone mirrors of the runtime math; needs JDK 21.

Build + run the simple before/after (best one to iterate on):
```
export JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview \
  src/main/java/com/customblocks/arabic/ArabicJoining.java \
  tools/render_preview/RenderPreview.java tools/render_preview/BeforeAfterPreview.java
"$JAVA_HOME/bin/java" -cp tools/render_preview BeforeAfterPreview
# opens out/BEFORE_AFTER.png — currently set to FLOOR-anchor (too far). Change the `fix` block in
# BeforeAfterPreview.tile() to a SMALL nudge and re-run.
```
The `fix` block to edit is in `BeforeAfterPreview.java` `tile(...)` — currently floor-anchors; replace
with the small nudge. `RenderPreview.java` holds the shared math + locked stroke constants. Other
throwaway previews from this session (ignore unless useful): WawFixPreview, WordIsoPreview,
WordLowerPreview, AllIsoPreview, AllIsoV2Preview, StressIsoPreview.

## Build + deploy (after dev approves the look)
```
export JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
"$JAVA_HOME/bin/../bin/gradlew" ...   # use the documented command:
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"; & "<repo>\gradlew.bat" -p "<repo>" build -x test --no-daemon
```
Then DEPLOY (dev expects this every build):
`Copy-Item -Force CustomBlocks-B/build/libs/customblocks-1.0.0.jar  C:\Users\66664\AppData\Roaming\.minecraft\mods\customblocks-1.0.0.jar`

Note: deployed jar at last check (14:33) did NOT include the 14:53 entity-renderer edit, so the dev's
in-game shot was older code. A fresh build will sync it.

## Key files
- `arabic/ArabicTileRenderer.java` — per-letter tile draw (shared metric FS/BASE_Y; kashida for connected;
  isolated = no hands). **← add the nudge here.** Has an unused `isolatedScale()` (dead, ignore/remove).
- `arabic/ArabicJoining.java` — pure joining table → form (ISOLATED/INITIAL/MEDIAL/FINAL). Used by previews.
- `client/render/ArabicLetterBlockEntityRenderer.java` — routes all forms → ArabicTileRenderer; 6-face cube.
- `block/ArabicLetterBlockEntity.java` — letter/form/color/attached state.
- These three are **untracked** (new this session) — commit them once the look is locked.

## DEV NOTES (read these)
- Dev is a **non-coder**, aesthetics-obsessed, **gets overwhelmed FAST**. Keep replies SHORT. Show **ONE**
  simple preview, not grids of 28 letters or 3-way comparisons. One small question at a time.
- This session over-walked it (too many options) and the dev got frustrated. The fix is **small**: nudge
  the floaty isolated letters down a little. Do that, show it once, ship it.
