# Group 10 - Color and Image Tools

> Group 10 owns the image and color rules that turn a source into a clean, editable CustomBlock without losing the player's intended design.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_10.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Image work should be predictable from first upload through later edits. A player can remove or fill a background, resize or export a texture, make color variants, keep palettes, and later change the saved background choice without an irreversible one-time bake.

This Group owns image processing, color algorithms, source validation, and the commands behind color tools. It does not own the common editing Screens, client render-layer registration, resource-pack delivery, recolor-tool interaction, or AI service integration.

## Ownership

| Owns | Does not own |
| --- | --- |
| Background processing and its automatic threshold, palettes, gradient, resize, export, and color variants | Editing Screens, tab layout, live recolor, and eyedrop UI: G27 |
| Stored per-block background setting and re-bake behavior | Cutout render-layer registration: G14 |
| Flattened-checkerboard detection and image safety limits | Pack generation and transport: G05 |
| `/cb colorvariants` command rail and color-family rules | Square/Triangle interaction: G06 |
| Bulk Recolor behavior and its image engines | Bulk selection, confirmation, and workbench behavior: G07 |
| Color styles as a shared creation concern | AI segmentation integration: G15 |

## Direction

Background is one stored per-block attribute with three values: transparent, black, or a color. Every surface writes that same value, then the image is re-baked from source data. The default is black so ordinary art is readable and checkerboard preview images do not become part of the texture.

The player-facing editor is one G27 Coloring Screen, but all image rules remain in G10. Image processing is cautious: reject unreasonable source sizes before mutation, preserve real subject pixels, and use only one pack update for a completed multi-block color operation.

How much of a picture counts as background is the mod's decision, not the player's. There is no strength number anywhere. The mod measures the picture it was actually given and derives its own threshold, and where it cannot decide with confidence it does the safe thing and says so rather than guessing silently.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-15 | `/cb dress` is removed. | Color variants and live recolor cover the useful overlay cases without a redundant command. |
| 2026-06-23 | Color distance uses CIE Lab. | Background matching follows perceptual difference instead of the old YCbCr behavior. |
| 2026-06-24 | Background is one `SlotData.background` attribute: transparent, black, or color. | There is no separate transparency switch or one-time-only fill choice. |
| 2026-06-25 | Black is the default background after removal or flattened-checkerboard detection. | Existing art has a reliable fallback and checkerboard previews never count as intended content. |
| 2026-06-25 | Background choice applies to full and shaped blocks. | Render and generated-model work must handle all supported shape classes. |
| 2026-06-29 | Flattened transparency checkerboards are detected before normal image processing, including `none` mode. | Opaque preview grids become black and a player receives a useful source-image warning. |
| 2026-06-29 | The rejected whole-white background/keyline direction is removed. | Standard processed backgrounds stay plain black unless a stored color choice says otherwise. |
| 2026-06-29 | A color family uses bare `<id>` as its black/base member. | Red, green, and yellow variants use `<id>_red`, `<id>_green`, and `<id>_yellow`. |
| 2026-07-01 | Color-family source downloads have strict size and pixel limits. | Oversized images fail before slots or textures mutate. |
| 2026-07-10 | The lighter bulk-background operation is in scope; the large Bulk Recolor Hub is parked. | The essential stored-background control can ship independently of the eleven-tile hub. |
| 2026-07-12 | Coloring surfaces consolidate into the G27 Coloring Screen. | G10 does not maintain a competing chest-menu editor flow. |
| 2026-07-18 | `/cb bulkrecolor` opens a separate advanced Recoloring Hub. | It does not redirect to the simple hue-shift Recolor action inside `/cb bulk`; every other Hub detail remains Discussion ✏️ for later. |
| 2026-07-18 | The stored background can be changed for one block, and is available through a background tool alongside the other tools. | Exact commands, Screen controls, and tool gestures remain Discussion ✏️ for later. (The chosen-selection and all-block targets were dropped 2026-07-25.) |
| 2026-07-24 | Edit command is `/cb setbg <id> <value>` with a `/cb setbackground` alias. | Matches existing `/cb resize`/`/cb exportpng` brevity pattern; no new command family. |
| 2026-07-24 | The background tool is a new mode on the existing Omni-Tool (sneak+right-click cycle, G06 §F pattern), not a separate item. | No new obtainable item; reuses the tool players already carry. |
| 2026-07-24 | Legacy blocks auto-migrate on first server load after this ships. | Migration takes a full G09 backup snapshot immediately before running, then applies as one undoable batch — chosen over dry-run-only after prior migration incidents; must not silently touch unrelated block state. |
| 2026-07-24 | `/cb setbg` and the Omni-Tool Background mode require the same permission tier as `/cb create`/retexture — no new tier. | Consistent with every other creation-adjacent command. |
| 2026-07-24 | The Studio background field lives inside the future G27 §E Editing tab, alongside Paint/Resize, not as a separate main-Studio section. | Background editing is a post-create edit concern, grouped with the other Editing-tab tools rather than the initial creation flow. |
| 2026-07-24 | One background value applies uniformly across all faces of a shaped/per-face block. | No per-face independent background storage; keeps scope and UI simple. |
| 2026-07-24 | Animated blocks (GIF/WebP) are parked out of background-attribute v1. | Static blocks only for now; animated re-bake-per-frame behavior is deferred, revisit later with G14. |
| 2026-07-24 | `/cb setbg <id> <value>` tab-completes `transparent`, `black`, and all 29 dye-palette names; free hex (`#RRGGBB`) is typed manually, not suggested. | Matches existing suggestion behavior elsewhere in the mod. |
| 2026-07-24 | Color-family size limits gate the source image itself, not just the download. | A stored source re-entering the family rail is re-checked, because it may predate the limits or have arrived through the looser plain-create download cap. |
| 2026-07-24 | A held confirm action names itself when it is replaced and when it runs. | One pending slot per player stays, but silently dropping an earlier held action is not acceptable for a destructive batch. |
| 2026-07-24 | Checkerboard detection covers a dark tone pair as well as a light one, and requires periodicity on both axes. | Dark-themed preview grids are cleaned; striped artwork, which only alternates on one axis, is left alone in both regimes. |
| 2026-07-25 | A baked block texture is always fully opaque. The final snap step composites any leftover alpha onto the fill even when background removal is off. | Slot blocks draw on an alpha-tested cutout layer, where a semi-transparent texel flickers per mip level and reads as a glitched hairline around the subject. Colour variants never showed it because their rail always ended in a fill; the base rail did not. This also matches the existing client-side intent for off-atlas blocks (`OffAtlasImage`: "owner wants them BLACK by default"). Transparent backgrounds must therefore be introduced deliberately with G14 §E, not as leftover alpha. |
| 2026-07-25 | "Background + closed areas" absorbs background-coloured REGIONS, not background-coloured pixels. A region is taken only when it is thick enough to hold a small disk, scaled to the image. | It was a per-pixel colour key over the whole image, so any dark shading inside a subject matched a dark background and was deleted — a chrome logo lost its shadow lines and the repainted background showed through the artwork. Connectivity plus a thickness gate keeps the enclosed-hole case (the counter of a letter "O") while leaving a hairline of subject shading alone. |
| 2026-07-25 | The Stage 1c morphological close is colour-guarded: a pixel plainly far from the background colour is never swallowed by morphology. | The close is not reversible over a subject feature 1-2 px wide — the dilate joins the background across it and the erode cannot reopen it — so thin outlines and small strokes were being deleted from the bake. Near-tolerance pixels stay unguarded, so the original hairline-gap fix is unaffected. |
| 2026-07-25 | Checkerboard tone share is measured over all in-regime border mass split at the two peaks, and the second peak must sit at least two luminance buckets from the first. | A lossy delivery of the same picture (a Google-image JPEG thumbnail) smears each checker tone across neighbouring buckets, which pushed the peak-bucket counts under the share threshold and made the second "peak" the first tone's own ringing. Detection now survives re-compression without loosening neutrality, the L* gap, or the two-axis periodicity test. |
| 2026-07-25 | `transparent` is a valid stored background value but is refused at every write surface until G14 §E registers the cutout render layer. | Storing it earlier would bake alpha the world cannot render, re-creating the hairline artifact the opaque-bake rule exists to remove. |
| 2026-07-25 | Image creation prints only the fetching line and the result line. Source-quality advice notices are removed. | Owner call: the notices read as noise on every create and the behaviour they describe happens anyway. |
| 2026-07-25 | Named colours are a single owner-defined set shared by the colour-variant fills and the colour library. | Two independent tables meant `red` was `#EE3333` in one place and a library entry in another; the owner sets one value per name. |
| 2026-07-25 | The all-block background target is scrapped. `/cb setbg` takes one block; there is no `all` argument, no server-wide re-bake, and no confirm/progress/cancel rail for one. | The test run broke blocks and left an animated block stuttering, and the owner does not want every block sharing one background in the first place. Removed rather than repaired. Selection-based bulk background remains a G27 Bulk Ops question, not a G10 `all` sweep. |
| 2026-07-25 | `/cb tolerance` is to be removed, not re-tuned. The background fill needs a rule that does not depend on a player-set number. | The value reads backwards (low = strictest) and its meaning disagrees between the ΔE mapping and the fill stage; the owner's call is to delete the knob instead of re-deriving it. §E testing is paused until the replacement rule exists. |
| 2026-07-25 | The replacement is a **cascade of five rungs ordered by certainty**, not one cleverer threshold. The first rung whose own evidence test passes decides the mask; a rung that cannot clear its test hands down instead of guessing. | A single estimator has a single failure mode and no way to notice it fired. Rungs let the easy majority of pictures be decided by something that cannot be wrong (the file's own alpha channel) and reserve statistics for pictures that genuinely need it. Each rung states what it assumes, so "why did it do that" always has one answer. |
| 2026-07-25 | Rung order is fixed: **1 authored alpha → 2 known key → 3 boundary-connectivity saliency → 4 threshold-estimator ensemble → 5 colour unmixing for the edge band**. Rung 5 always runs on whatever mask rungs 1-4 produced; it refines the edge, it never decides the mask. | The order is strictly decreasing certainty. Moving a lower rung up would let a guess overrule a fact. Rung 5 is separate because sub-pixel edge quality is a different question from which region is background. |
| 2026-07-25 | Every rung's mask passes the same **degenerate-mask QC gate** before it is accepted: not empty, not the whole frame, not fragmented into many disconnected pieces, and background must actually touch the frame border. Failing any check demotes to the next rung. | These four shapes are what every documented auto-removal failure looks like from the outside. Checking the mask's shape catches a wrong answer without needing to know the right one, and it is what turns "the algorithm was confident and wrong" into "the algorithm handed down". |
| 2026-07-25 | The primary statistical estimator is **minimum-error thresholding (Kittler–Illingworth)**, not Otsu. | Otsu assumes the two sides have equal spread. A picture with a flat background and a busy subject violates that badly, and Otsu answers by cutting into the subject — which is exactly why `MAX_DELTA_E` had to be lowered from 40 to 22 after a near-white subject got eaten. MET models the two spreads separately, so the magic number is deleted with its cause rather than compensated for. |
| 2026-07-25 | Rung 4 is an **ensemble, not a single estimator**: MET, Triangle, Rosin, Li, and weighted-object-variance each propose a threshold, the proposals are z-score normalised and combined by Borda count, and agreement width is the rung's confidence. | The estimators fail on disjoint picture types — Triangle wants a unimodal histogram, MET wants two real modes, Rosin wants a long tail. Any one of them alone is a coin flip on the wrong picture type. Wide disagreement between them is itself the signal that this picture is not decidable statistically, which is what makes the hand-down honest instead of arbitrary. |
| 2026-07-25 | The chosen threshold is the centre of the widest **stability plateau**, not a single computed point. | A threshold that only works at exactly one value is an accident of that picture's noise. Sweeping and keeping the value that survives the widest range makes the same picture, re-saved or re-compressed, land on the same answer — the property the JPEG-thumbnail checkerboard case already proved matters. |
| 2026-07-25 | Colour distance becomes **CIEDE2000**, replacing the plain Euclidean CIE76 currently in `deltaE`. | CIE76 badly overstates distance in saturated blues and understates it in near-neutrals, so one ΔE budget cannot mean the same thing across the palette — a blue background and a grey one need different numbers under CIE76 and the same number under CIEDE2000. A fixed threshold is only defensible once the metric under it is uniform. |
| 2026-07-25 | All compositing moves to **linear light**. Decode sRGB → blend → re-encode, at every site that mixes a pixel with a fill. Alpha is never passed through the transfer curve. | Blending transfer-encoded bytes loses light energy toward the darker operand, which against a black fill draws a dark rim along every anti-aliased edge. That rim is the artifact `RING_DARK_MAX`, `RING_PASSES` and the anti-fringe peel were all built to chase after the fact. Fixing the blend removes the cause, and those compensations come out with it. |
| 2026-07-25 | Edge decisions use **hysteresis double-thresholding**, and thin-structure cleanup uses an **attribute (area) opening on a max-tree**, not a morphological opening. | A single threshold cuts a real edge wherever it momentarily dips; hysteresis keeps a weak pixel that connects to a strong one, so an edge survives its own weak stretch. Morphological opening deletes anything narrower than its structuring element, which is precisely Tux's hair-thin outlines; an area opening judges a component by its total size instead, so a long thin line is kept and a small blob is dropped. |
| 2026-07-25 | The modes collapse to **Auto and Off**. `BgRemove` (edges) and `BgRemove&More` (closed) merge into Auto; `NoBgRemove` stays as Off. **`BgSmart` is deleted outright** — not merged, not kept as a spelling, gone from the cycle, the display names, and tab-complete. Removal mode remains a global setting on `/cb config background`; it does not become per-block. | The removal modes differ only in how aggressively one fixed number was applied, and that number is gone. `BgSmart` was the worst of them — the same code path with the number hard-coded to 35 whenever the player had not set one, dressed up as intelligence it did not have. Auto is the real version of what it pretended to be, so keeping its name around would only invite the question of how it differs. Making mode per-block would be new scope, and Auto is meant to need no per-block choice at all. |
| 2026-07-25 | A stored or typed mode value that is no longer recognised resolves to **Auto**, never silently to Off. | `normalize` currently answers Off for anything unrecognised. After the rip that would turn a server whose config still says `smart` into one that quietly stopped removing backgrounds, with nothing in chat to explain it. Failing toward "still works" is the only safe direction here. **Superseded 2026-07-26**: unrecognised now hard rejects everywhere instead of falling back — see the 2026-07-26 row above. |
| 2026-07-25 | Off means off for a normal bake, but the **recolour rails always run Auto regardless of mode** — colour variants, colour families, and the Arabic coloured sets. | This is already how the mod behaves and it is not optional: `recolorBackground` forces `none` to `edges`, and six separate call sites substitute a tolerance of 30 whenever the real one is 0, each commented "0 would mean no recolour at all". Painting a background a new colour requires knowing where the background is, so with detection genuinely off a variant comes out identical to its base. Written down because the Auto/Off collapse would otherwise read as a promise that Off is always honoured. |
| 2026-07-25 | The per-block tolerance override is deleted rather than ported, and its removal is **not** a behaviour change for most of the mod. | `BlockToleranceStore.effective()` has exactly one caller — `BgStudioSession`. Creation, retexture, faces, colour families, variants, safety re-bakes, temp retexture, and Arabic all read the global value directly. So `/cb tolerance 60 <id>` already sets a number that almost every rail ignores, which is its own defect and removes any argument for preserving it. |
| 2026-07-25 | Checkerboard flattening stays a **pre-pass that runs before the cascade**, in Auto and Off alike, exactly where it runs today. | `CheckerboardDetector.flattenToBlack` is the first line of `process()` and applies even in `none`, because a flattened preview grid is a broken source in every mode. It answers a different question from the cascade — "is this grid real content" rather than "which region is background" — so folding it into a rung would make both harder to reason about. |
| 2026-07-25 | Auto's cost is budgeted **per bake, and bulk rails bake many blocks**. The cascade must not make `/cb retextureall` or a safety re-bake sweep worse than the work it replaces. | Fourteen call sites run the removal path, several of them in loops over every block on the server. A cascade that is affordable once and ruinous a thousand times would ship as a hang, not as a quality win, and the early rungs are cheap precisely so most pictures never reach the expensive one. |
| 2026-07-25 | The single escape hatch is **`/cb bgpick <id> <colour>`** — the player names the background colour, which seeds rung 2 as a known key. No retry command, no strength nudge, no hidden number. | An escape hatch that re-runs the same algorithm and hopes is not an answer. Naming the background supplies the one fact the algorithm was missing, which is why it promotes the picture to the most certain rung that can use it. It takes a colour rather than a clicked pixel because the mod has no surface for clicking inside a source image, and inventing one would be a Screens project; a colour name or hex reuses the picker `/cb setbg` and the fill swatch already have. |
| 2026-07-25 | The opaque-bake snap is unconditional. It must not be gated on any removal setting. | It is currently gated on `tolerance > 0` (`BackgroundRemover:357`), so with removal off the always-opaque rule from earlier today silently does not apply and the cutout-layer hairline comes back. The gate goes out with the number. |
| 2026-07-26 | Rung 2's colour bar, and every "is this the same colour" test that follows from it, is the published average-observer **JND of 2.3 ΔE00** (Mahy, Van Eycken & Oosterlinck 1994). | Rung 2's claim is that the background colour is KNOWN, so its match bar has to be "indistinguishable from the key" rather than a tuned number. A background needing a looser bar than human eyesight is not a known key, and handing that picture down is the correct outcome. |
| 2026-07-26 | Rung 3's two boundary-connectivity thresholds are **read off the measure geometrically, not tuned**: a square flush against one frame edge scores exactly 1, the same square in a corner scores 2, an enclosed region scores 0. Background must reach 2; a sizeable region in the 1-2 band is the hand-down. | It makes "wraps the frame" and "merely rests against an edge" separate, defensible statements instead of a threshold someone picked. The gap between the two derived values IS the clear margin the cascade requires. |
| 2026-07-26 | Rung 4's histogram **starts past the JND**, excluding pixels indistinguishable from the reference tone. | Measuring distance from a colour puts an impulse at zero — every exactly-matching pixel in one bin, often most of the picture. Triangle and Rosin are anchored on the histogram peak, so against that impulse they measure the impulse: both answered "cut at essentially zero" on every baseline picture. Starting past the JND discards no pixel whose classification was ever in doubt. |
| 2026-07-26 | Rung 4's **agreement is judged by consequence, not by how close the numbers look**: the pixel count in the band the surviving proposals span, against the QC area floor. | Requiring five independent estimators to land within a JND of one another is a handful of bins out of 256, which no real picture satisfies — it made the whole rung dead code. A genuine valley is nearly empty however widely the estimators scatter across it, while a histogram with no valley fills the band at once. |
| 2026-07-26 | A `/cb bgpick` colour that fails the QC gate is **refused there and then**; it does not fall through to rungs 3-4. | The player has asserted a fact. Ignoring them and guessing with the lower rungs contradicts the point of the escape hatch, and it produces an unreadable message that lists every rung's complaint at once. |
| 2026-07-26 | Rung 5 hands back the **recovered subject colour** alongside the coverage, not the coverage alone. | A band pixel's stored colour is a mixture that still contains the old background; compositing that onto a new fill lays the old background over the new one and leaves a halo of the original colour tracing every edge. Solving F first replaces the background instead of layering over it. |
| 2026-07-26 | Thin background-coloured slivers at a lossy edge are **left as subject**, and that is the pocket thickness gate working as locked, not a defect to patch. | Traced on the letter-O JPEG: those pixels ARE flagged as background-coloured pockets and are rejected by the thickness gate from 2026-07-25, which exists to stop thin subject shading being eaten. The two cannot both be satisfied by a pixel-level rule; loosening the gate is an owner decision, and three candidate fixes were built and reverted rather than shipped. |
| 2026-07-25 | Behaviour is frozen as **golden images** built from the current jar *before* any rip lands. | Without a before picture, "the new one looks fine" is an opinion. Baselines captured first turn every later change into a visible diff, and the pictures that already have owner verdicts (chrome logo, Tux, letter O, the JPEG thumbnail, the striped artwork) are the ones that matter. |
| 2026-07-26 | The HA4 blue-speckle regression is fixed with a second, looser-tolerance mop-up pass over small leftover isolated foreground fragments, run after despeckle. Rung 2's JND=2.3 key match stays untouched for the main mask, so real subject detail keeps its current protection. | The letter-O bake left scattered blue dots: fragments too large for the despeckle area floor but too close to the key colour to be real subject. Widening the main JND match risks eating real subject detail elsewhere (§H's whole design point); a second pass scoped to small leftover fragments only avoids that trade-off. Built 2026-07-26 as `BgMopUp.sweep`, run from `BgCascade.clean` on rungs 2-4. Neither of its two numbers is new: the size ceiling is `BgQc.areaFloor` — the cascade's existing "too small to be a region" — and the colour bar is twice `BgRungKey.JND`, one JND being where two colours first separate WHEN COMPARED SIDE BY SIDE, which a fragment surrounded by the key colour never is. The fragment is judged on its mean Lab rather than per pixel, because averaging is exactly what cancels the compression noise that created it while a real detail keeps its own colour under the average. Rung 1 is excluded: authored alpha is the author's statement of what is background, and re-judging it by colour would replace a fact with a measurement. |
| 2026-07-28 | Rung 5's mixed band is **grown from the picture, not fixed at 2 px**. It pushes outward one ring at a time while the pixels there are still MOSTLY backdrop (under half covered — a majority, not a bar), the two endpoints are more than a JND apart, and the mixture rebuilds the pixel within 2×JND. Capped at `min(w,h)/150`, never past 8. | A fixed radius states how wide one anti-aliased edge is, and real sources are wider: a cut-out photograph keeps a rim of its old backdrop, an over-sharpened JPEG carries an overshoot ring inside its own outline, a re-compressed grid smears its tones. Measured on `<link 11>` (Jupiter) across the limb: `E0E1E3`, `C4C8CB`, `F3F8FC`, `F9FFFF` — an undershoot hairline then two pixels white to within 3 ΔE00 of the backdrop. The 2 px band reached the first two; the rest baked as the owner's white outline. Growth is by ADJACENCY, so it cannot jump a subject outline into a background-coloured area behind it (the penguin's white belly sits behind a black outline whose pixels are not mixtures, and the ring stops there). |
| 2026-07-28 | Every edge solve must **REBUILD the pixel it claims to explain** — `EdgeMix.explains`, at 2×JND — or the binary decision stands. | A projection onto the line between two colours always returns an answer, including for a pixel that is neither and no mixture of them. A dark keyline between a dark backdrop and a bright subject projects to "all backdrop": without this guard the band growth erases the chrome logo's own outline. It is the single condition that makes a wider band safe. |
| 2026-07-28 | The checkerboard feather's coverage is the **same matting solve as rung 5** (`EdgeMix`), against the nearest grid tone and the nearest ARTWORK colour, both propagated by BFS. The `RAMP_LO`/`RAMP_HI` distance ramp and the `GUARD_LUM`/`GUARD_CH` brightness guard are deleted. | A colour DISTANCE is not a coverage. A red button half covered by a white grid sits far from white, so the ramp called it fully covered, left it opaque and divided the grid colour back out of it — which lightens it. Measured on the share button: boundary pixels baking as `EEDDDD` and `E4A1A1` against a `DB1313` button, the pale outline of HC4/HD2. The artwork colour is taken from the NEAREST artwork, not averaged over a window: an average across the share arrow's white face and its red outline invents a pink that is in neither, and the solve then declines and leaves a pale step. White artwork on a white grid tone is now covered by the endpoints being under a JND apart — the guard's job, done by measurement instead of a constant. |
| 2026-07-28 | Where artwork IS one of the grid's own tones, the grid is identified by **local periodicity** (`CheckerSilhouette`), never by a whole-picture lattice phase: the other tone one cell away, its own tone two cells away, asked separately on each axis. | The owner's football is pure white on a white-and-grey grid, so along its limb there is nothing to measure pixel by pixel and the colour pass took whichever cells looked like grid — a scalloped bite, one per cell, plus pinpricks inside the panels. A global phase cannot fix it because stock previews are scaled: this one's cell boundaries fall at x=22 and again at x=699, a period of 19.9, and a phase fitted from the border came out 3 px off and spared a sliver of every cell. Local periodicity needs no phase at all. |
| 2026-07-28 | Undecided pixels take the **nearest decided verdict by chamfer distance (5-7-11)**, and the boundary is then **straightened by a majority vote over a disk of half a cell**. Only pixels the picture did not positively answer may move. | A 4-connected flood measures city blocks, and city blocks draw a staircase between two coarse patches of evidence — the football's limb with the grid's own tread. The majority vote is mean curvature flow on a binary field: a step half a cell deep is voted away, the arc it sits on is not. Grid evidence is positive and frozen; "subject" from a shared tone is the ABSENCE of evidence and arrives a whole cell at a time, which is what makes the edge step, so those pixels are the ones allowed to straighten. |
| 2026-07-29 | In `CheckerSilhouette`, a grid verdict needs **one axis that keeps the beat and no axis that contradicts it** — silence is not a veto. An axis answering 0 means its probes a cell away landed on nothing solid; an axis answering -1 means the tone REPEATS, which still refuses. | Requiring BOTH axes to beat was the football's white halo. A real grid cell beside the ball hears the checkerboard on the side facing away and nothing on the side facing the ball, so it scored 0 on one axis, fell undecided, and the chamfer fill then handed it to the nearest subject evidence — the ball itself. Measured 2026-07-29 through the real classes: of the 9450 grid pixels the repair spared on `bg_in/9_football.png`, 6738 read one axis beating and the other silent; the ring was one cell wide all round the ball, which is what the owner saw in game. After the change the repair spares 2572 px, and the pixels it exists for — the scalloped bites at the ball's upper limb and the pinpricks inside its white panels — are still spared. Diffed across all twelve baselines: the eight that are not checkerboards are byte-identical, the share button loses 120 px of kept backdrop and the chrome logo 17, both with nothing eaten, and the subscribe banner keeps 16 more white detail pixels. The relaxation cannot reach artwork that alternates in one direction only — the ball's own lit/shaded boundary repeats on the second axis and still answers -1. [G10 §H](GROUP_10_COLOR_IMAGE.md) |
| 2026-07-29 | A solved boundary pixel measured as MORE BACKDROP THAN SUBJECT is backdrop outright, not kept at partial strength — `BgRungUnmix.SNAP_COVERAGE` and the feather's `COVER_FLOOR`, both 0.50. | Owner call after seeing the pre-cascade build side by side: the old mode-plus-tolerance system gave a harder, cleaner edge, and the soft partial-strength fringe the cascade left in its place reads in game as the same white outline. This is NOT the old knob returning. That knob compared EVERY pixel in the picture against a background colour, which is why it erased a white ball on a white grid whole — measured 2026-07-29 by rebuilding it from `89e4f62` and baking the owner's five pictures: it keeps 266803 of the football's 381874 kept pixels (70 %) and 112878 of the share button's 125792 (90 %), deleting the ball's white panels and the share arrow's white face. The snap can only reach a pixel that is already on the mask boundary, already explained by both endpoints, and already measured as under half covered; nothing outside the band and nothing the solve calls mostly-subject can be touched by it. Leftover backdrop after: share 0, penguin 9, Jupiter 54, football 194 of which 168 are white-on-white with nothing to separate, subscribe 131. Tux, the transparent PNG and the stripes are byte-identical. |
| 2026-07-29 | Presence is carried by its own flag, never by a reserved colour value. The grid feather's `near`/`art` arrays used `-1` to mean "nothing carried here", and a packed opaque WHITE pixel IS `-1` as a signed int. | The commonest transparency-grid tone in stock previews is pure white, so the sentinel collided with real data: measured 2026-07-29, 63180 of the share button's 143912 grid pixels and 44080 of the subscribe banner's 95060 are exactly `0xFFFFFFFF`. Every boundary pixel whose nearest grid square was pure white read as "no grid tone here", the feather skipped it, and the mixture baked opaque — 484 pale pixels tracing the share arrow, the outline the owner reported. The same collision applied to white ARTWORK through the `art` array. Fixed with parallel `hasNear`/`hasArt` flags. Pale boundary pixels in the bake: share 618 → 11, subscribe 179 → 111. Nothing else moves — Tux, the transparent PNG and the stripes are byte-identical. |
| 2026-07-29 | Where the mask stopped short and no confirmed background pixel remains within the edge solve's reach, the background endpoint falls back to `BgPlate`, the backdrop estimate the pipeline already builds. | An edge needs a colour for BOTH sides, and a local window cannot supply one where the subject runs off the frame and the only background-side colour left is the anti-aliased ramp itself. Measured on Jupiter, 2026-07-29: along the top of the frame the planet's limb leaves a white ramp 1-3 px deep with no confirmed background above it, so 106 of its 160 leftover pixels failed with "no background within reach" and baked as a white line. The plate answers the same question by push-pull over the whole picture, so it still has a backdrop colour where the window has no sample. Pale boundary pixels on Jupiter: 214 → 95. |
| 2026-07-29 | The edge solve judges a mixture by the residual RELATIVE to the endpoints' own separation (`EdgeMix.MIX_RATIO`, 0.40), not by an absolute ΔE00 alone. The absolute `MIX_TOL` stays as the first test; the ratio is the second. | An absolute bar asks the wrong question. A residual of 8 ΔE00 is a bad fit between colours 10 apart and an excellent one between colours 90 apart, and an anti-aliased edge runs between the most separated pair in the picture, so the bar was tightest exactly where the rung does its work. Lossy sources make it unavoidable: JPEG carries chroma at half resolution, so colour bleeds across a sharp edge and the transition pixels sit off the straight line while still being plainly mixtures. Measured on `bg_in/10_penguin.jpg` row y=450, 2026-07-29: the foot's edge pixels `FFFBE3` and `DEBE81` rebuild 8.1 and 5.5 ΔE00 off against endpoints 60-plus apart, the 4.6 bar refused both, and both baked byte-identical to the source — the owner's white outline. Robust matting judges a sample pair by the same ratio for the same reason (Wang and Cohen, 2007). The value is read off two measured populations rather than chosen: across the twelve baselines the genuine edges are done by about 0.35 (penguin 1458 of 1565 refusals under 0.35; letter O 4552 of 4561 under 0.30) while the pixels the guard exists to refuse mass past 0.50 (transparent PNG 3143 of 5436; chrome logo 2550 of 3690), and 0.40 sits in the empty middle. Verified over every baseline: the transparent PNG, Tux and the football are byte-identical, the chrome logo moves 76 px with no visible change, and the over-erasure count is unchanged at every ratio from 0.10 to 0.60 — the guard against erasing a dark keyline is `separated` plus the coverage clamp, which this does not touch. The pale rim measure falls: penguin 475 → 9, Jupiter 213 → 160, subscribe 475 → 115, chrome JPEG 139 → 44, letter O 1239 → 26. |
| 2026-07-28 | `coversBorder` is asked **before** the silhouette repair, not after. | It answers "was the grid found at all", which is a question about the colour verdict. The repair spares grid that is the artwork's own colour, and near a subject running off the frame that is part of the border ring — letting it move the answer un-declared a grid that had been found perfectly well (measured on the football: ring coverage 76 %, so the alpha hand-off was refused, rung 2 keyed the flattened black instead and ate the ball's black panels). |
| 2026-07-26 | Watermark/logo text sitting close to but outside the background's JND is explicitly out of scope for §H. It is not treated as a cascade bug. | Research confirms same-colour text/watermark removal is a different problem (detection + inpainting), not a background-key match. Chasing it inside the cascade would reopen the exact tolerance-widening risk the mop-up fix above avoids. Logged as its own deferred idea. |
| 2026-07-26 | Open defect: the cascade did not decline on the busy, edge-to-edge 3000×1500 test source. Per the locked rule two rows up, all five rungs failing should mean an unchanged bake and a pointer to `/cb bgpick` — instead the cascade ran to completion (roughly correct bake, ~11s) and never printed the decline message. Not yet root-caused; no rung's QC gate has been checked against this source. | The picture exists precisely to exercise the decline path (Testing Guide §HB15, "too big on purpose"), so a silent full run is a spec violation, not a missing polish item. The connection-drop that accompanied the first attempt is a separate, G05-owned question (server-wide stall vs. this client's link) and is not this row's evidence; this row is about the cascade's own decision, which the retest can isolate by timing and QC-gate logging alone, without needing the network angle resolved first. |
| 2026-07-27 | §HA4 root cause, measured: the mop-up's two numbers were never the fault — its UNIT was. It grouped by connected components of raw foreground, and the leftover background colour is not a component of its own; it is welded to the subject's edge. So the mop-up now runs TWO units over the same size floor and the same colour bar: `absorbDetached` (the original — a whole island judged on its mean Lab) and `absorbFused` (select the near-key foreground pixels FIRST, take connected pieces of those, absorb one that is under the floor and touches the mask). | Measured offline on the real `bg_in/7_letter_o.jpg` through the real cascade classes, 2026-07-27. The old pass spared exactly ZERO fragments on colour while 857 px of background colour survived the bake — every one of them inside a single 89,335 px component, 111× the 800 px floor, which the oversize skip discarded without colour-judging a pixel of it. No value of `BgQc.areaFloor` or the JND multiple could have reached that residue, so widening either would have been the wrong fix aimed at the wrong thing. Golden-diffed across all seven `bg_in` baselines: Tux, the transparent PNG, the chrome JPEG and the stripes changed by 0 px, the chrome logo by 6-16 px, the periodic table by 120 px of 19.8 M; only the letter O moved (≈4.7 k px). Ring residue there fell 235 px → 57 px while the watermark band did not get worse (612 → 542), so nothing was traded for it. A fused piece is absorbed only when it TOUCHES the mask — a fact, not a threshold: welded to the boundary is where the strict flood stopped, floating inside the subject is the subject's own shading, the same distinction `absorbEnclosedPockets` already makes. |
| 2026-07-27 | §HB15 is NOT a broken decline path, and the decline path must not be changed to satisfy it. Rung 2 accepts the periodic table legitimately; the row's expectation is what needs an owner decision. | Every rung was probed against the real `bg_in/5_periodic_table.png` (6000×3300 = 19.8 M px, not the 3000×1500 the row records), 2026-07-27. Rung 1 declines (no alpha). Rung 2 finds a genuinely flat `#FFFFFF` border on all four sides, keys it, and produces a mask covering 42.32 % of the picture that is one connected region reaching the frame — it passes `BgQc` on the merits, and the resulting bake is correct: every element box, its text and its fill survive, only the white between them is replaced. The decline path itself is healthy — a source with no supportable background (random noise) still returns rung 0 with all four decline reasons. What the row actually catches is a SIZE concern wearing a background-confidence costume: 19.8 M px reaches the cascade at all because `ImageLimits` guards only the colour-family rail (`ColorFamilyOps`), not plain `/cb create`. Extending that gate to plain create is the owner's call — it would start refusing large sources that work today — and it also touches the G05 §G allocation question, so it is deliberately not built here. |
| 2026-07-27 | §HA4's blue blobs were never a tolerance problem. Two of them, 57 px and 43 px, are slivers of the letter's own counter pinched off it by a 2-3 px seam; judged alone each is too thin to be a region, so the thickness gate kept them as subject. Pockets are therefore GROUPED across a hairline seam before being judged — components are taken on the pocket set widened by the same hairline bar, while only true pocket pixels are ever absorbed. | Measured offline through the real cascade on `bg_in/7_letter_o.jpg`, 2026-07-27: 85.02 % of the picture is background colour, the edge flood reaches 74.84 %, and 10.18 % is walled off into 310 pockets. The counter (63,735 px) absorbs correctly; the two visible blobs sit 2 px and 3 px from it. No thickness or size threshold could reach them, because the fault was the UNIT being judged, not the bar — the same class of error as the 2026-07-27 mop-up row above. Golden-diffed across all seven baselines: Tux, the chrome logo, the transparent PNG, the periodic table and the stripes are byte-identical. |
| 2026-07-27 | A stock watermark is recognised as background under a TRANSLUCENT WHITE VEIL (`BgVeil`), wired into the mop-up's fused-residue candidate set only. The ΔE bar is untouched; what changes is what the pixel is compared against — the key, or the key mixed toward white in linear light. Two structural guards: the mix may not exceed 0.35, and a near-neutral key disables the test outright. | Measured on the same baseline: the veiled pixels sit at ΔL +3.7, ΔC -5.8, Δhue -6.4°, i.e. 6.3 ΔE00 from the key and hopelessly outside any honest JND — the exact signature of white composited over a colour, which is a geometric fact rather than a tolerance. The mix cap exists because pure white IS on that line, so without it the white letter O and the white stripes would qualify; the measured watermark mixes at 0.10-0.13, far below the cap. The neutral-key guard exists because every grey lies on the line from black to white, which is the chrome logo's entire silver bevel — with a black key the test is off. Wiring it into rung 2's region walk instead was built, measured and REVERTED the same day: it changed the letter O by 0 bytes, because that picture is not decided by rung 2, and it took the periodic table from 4.9 s to 22 s. |
| 2026-07-27 | The mop-up gains a THIRD unit, `absorbStamped`: a watermark's own ink. Units 1 and 2 both ask whether a leftover IS the background colour; ink is not. Four conditions must hold together — the island's every neighbour is background (isolated, not merely touching), it is under `BgQc.areaFloor`, its mean is lighter AND less saturated than the key, and the key is chromatic. | Measured on `bg_in/7_letter_o.jpg`, 2026-07-27: the surviving marks sit 81 % of the way to white and 40.5 ΔE00 from the key, with only 7 % of them within the veil line's bar — no widening of unit 1 or 2 could have reached them, because they are pigment over the background rather than background showing through. ISOLATED rather than TOUCHING is what puts the subject out of reach by construction, and it is also correct on the merits: a stamp printed across the artwork is welded to it, and erasing it would leave a hole. The size floor keeps the white letter O — one component far above it — unable to qualify on any picture, and the chromatic-key guard switches the unit off against a black or grey background, where "lighter and less saturated" describes every silver highlight on the chrome logo. Golden-diffed: all six other baselines byte-identical; strong blue on the letter O falls 296 px → 44 px. |
| 2026-07-27 | Open, deferred to its own session: a smooth GRADIENT background can never reach rung 2, so an entire class of ordinary stock art is decided by the measuring rungs instead of by a fact. | `bg_in/7_letter_o.jpg` fades from `15,116,186` to `31,133,197`, 6.5 ΔE corner to corner. `flatBorderTone` requires all four sides within one JND of a single tone: top 99.9 %, left 99.9 %, bottom 0.0 %, right 0.0 % — so it returns null and rung 2 declines. Three replacements were measured and rejected on 2026-07-27: a bilinear surface fitted from the border covers 34.8 % of the picture against a single tone's 85.0 %, because the fade is not planar and the border does not predict the middle; a neighbour-relative flood covers 75.0 %, because compression noise breaks connectivity across the fade; a running local colour with a 12 px window covers 74.9 %. None is a fix, and none should be retried without new evidence. |
| 2026-07-26 | Only `auto` and `nobackground` are recognised mode values. Every other value — typed on `/cb config background`, stored in the config file, or hit on startup — hard rejects with a logged error. There is no silent fallback to Auto anywhere, including legacy spellings (`BgRemove&More`, `BgSmart`) and startup with a junk/legacy value already on disk. **Supersedes** the 2026-07-25 "unrecognised resolves to Auto, never Off" rule below. | Owner call, 2026-07-26: the earlier fail-safe was meant to stop a server silently losing background removal on an old config; the owner instead wants a hard, visible failure for anything that isn't exactly one of the two live values, at every entry point, so a stale value can never be worked around quietly. Built 2026-07-26: `fromArg` accepts those two strings and nothing else, `normalize` is replaced by `requireMode` which throws, the typed command rejects in chat without writing, and config load throws OUTSIDE its own catch-all (that catch falls back to defaults, which for this field is the very workaround being banned). One value is renamed rather than rejected: the pre-2026-07-26 internal id `none`, which every earlier jar wrote for Off. Renaming the mod's OWN id in place, logged, and rewritten on the next save is not a fallback — the ban is on working around a value the mod never meant — and rejecting it would have bricked startup on the config the shipping jar itself produced. A player who TYPES `none` still gets the hard error. |

## Feature Plan

### A. Core Image and Color Commands

**Player outcome**

Players can make common image changes with predictable output, then find the same capabilities through the unified Coloring Screen.

**Experience**

- `/cb gradient <id1> <id2> <steps>` creates perceptually even intermediate colors as one reversible batch.
- `/cb bgstudio <id>` controls background processing: Auto or Off, and the fill colour. There is no strength control — see §H.
- `/cb palette save/load/list/delete` persists reusable palettes per player.
- `/cb resize <id> <64|128|256>` resamples the source texture deliberately.
- `/cb exportpng <id>` writes a local PNG export without relying on a server-address-leaking chat link.
- `/cb colors` and `/cb customcolor` use the same color library and image rules as the Screen path.

**Requirements**

- Gradient interpolation uses CIE Lab and records one batch history action.
- Background removal decides its own threshold; there is no global or per-block tolerance number for the player to set.
- Palette ownership is per player and persists independently of shared block data.
- Resize and export operate on the current source/baked texture without changing unrelated block attributes.
- Export delivery must not expose a host/IP address or claim a link works when the local file is the only reliable result.

**Boundary**

G10 provides commands and algorithms. G27 provides their consolidated editing surface; G12 owns the export file itself and G20 owns the download route.

### B. Stored Background Attribute

**Player outcome**

A player can choose transparent, black, or a specific background color for a block now and change it later through any supported surface.

**Experience**

- Transparency is genuinely see-through where source alpha exists.
- Black is the default safe fill.
- A color can come from the 29-color library or a free hex value.
- `/cb setbg <id> <value>` (alias `/cb setbackground`), Studio, and the Omni-Tool's Background mode all show and write the same choice.
- Background control is per block. There is no all-block sweep.
- Existing blocks migrate automatically on first load after a G09 backup snapshot, as one undoable batch.

**Requirements**

- `SlotData.background` persists the selected value.
- Re-bake composites to black or the chosen color, or preserves alpha for transparent.
- Slot blocks render on the cutout layer so atlas-backed alpha is visible.
- Full, slab, stair, cross, and other shape models avoid interior-face/culling artifacts with background choices.
- A no-source block is skipped with an honest warning instead of being degraded by a guessed re-bake.
- The single-block command, Studio, and tool routes write the same `SlotData.background` value.
- `/cb setbg` accepts no `all` argument and exposes no server-wide re-bake.


**Boundary**

G10 owns the stored value and re-bake. G14 owns the client cutout-layer registration; G05 owns generated-pack delivery after the changed textures are ready.

### C. Clean Source Processing

**Player outcome**

Transparent-image previews, non-square sources, and harmless edge residue do not turn into ugly checkerboards, bands, or accidental recolor artifacts.

**Experience**

- Flattened checkerboard preview images become a clean black background before normal processing.
- The player is told that the input was a flattened preview and can replace it with the real transparent source.
- A true transparent PNG remains unchanged by checkerboard cleanup.
- Non-square color-variant padding fills with the variant color rather than leaving black bands.
- Any unresolved trapped or off-tolerance dark region is classified from the real input before a destructive recolor rule is added.

**Requirements**

- `CheckerboardDetector` runs before normal background processing and is safe to run more than once.
- Detection requires a strict neutral two-tone checker pattern so ordinary image content is not mistaken for a preview grid.
- Detection covers dark preview grids as well as light ones, because stock sites ship dark-themed transparency checkerboards.
- Striped artwork in checker-like tones stays untouched at either brightness; only a genuine two-axis grid is treated as a preview.
- Component and speck cleanup preserve the significant subject mass while removing small checker residue.
- `ImageProcessor` normalizes non-square sources without transparent padding showing as a black band in a variant.
- `BackgroundRemover` keeps edge-safe behavior until an actual source demonstrates that a closed-region change is safe.

**Boundary**

This work cleans known source artifacts. It does not remove intentional dark design elements merely to make every image recolor identically.

### D. Color Variants and Families

**Player outcome**

One source can create a consistent black, red, green, and yellow block family, with optional extra colors, clear overwrite behavior, and one undo action.

**Experience**

- `/cb colorvariants <id> <displayname> <link>` creates a new family; `/cb variants` is its alias.
- `/cb colorvariants <id>` regenerates color variants from an existing base texture without changing its display name.
- Optional comma-separated colors or `all` extend the default family.
- `/cb colorvariants delete <id>` lists the family and waits for confirmation before removal.
- A future builder Screen (owned entirely by G27, moved 2026-07-24) gathers inputs and calls this same command rail rather than creating a second backend.

**Requirements**

- Base uses bare `<id>`; default variants use `_red`, `_green`, and `_yellow`; a legacy `_black` is removed when deleting a family.
- One download serves the family; the base stores the original upload and variants store their baked PNG sources.
- Existing targets require confirmation and retain non-color attributes such as glow, hardness, sound, collision, category, and favorite state.
- Locked targets are skipped and named in the result.
- The whole create, overwrite, regenerate, or delete family is one history batch and one pack update.
- Source downloads enforce body, image-dimension, and megapixel limits before state mutation.
- The command refreshes HUD/client state and keeps shared help, suggestion, lock, incident, and completion hooks aligned with other creation paths.

**Boundary**

G10 owns the color-family algorithm and command rail. G27 owns the future family Screen, G06 owns Square/Triangle swapping, and G15 owns future animated/AI-heavy extensions.

### E. Bulk Recolor and Color Styles

**Player outcome**

The owner can open a separate advanced Recoloring Hub with `/cb bulkrecolor`. Only that command-to-Hub destination is locked now.

**Experience**

- `/cb bulkrecolor` opens its own advanced Recoloring Hub.
- It is separate from the simple hue-shift Recolor action currently visible inside `/cb bulk`.

**Discussion later — Discussion ✏️**

Target selection and `/cb bulk` Screen integration, the operation set, layout, command arguments, previews, confirmation, safety/workload limits, progress, cancellation/rollback, undo behavior, and every other Hub extra are intentionally not decided yet.

**Requirements**

- Do not infer the final Hub from historical tile lists or from the existing simple hue-shift operation.
- Do not build the advanced Hub until its Discussion ✏️ items are resolved with the owner.

**Boundary**

The advanced Hub is separate future work. It does not delay the ordinary background attribute, and no G07 integration contract is locked before the `/cb bulk` Screen migration is settled.

### F. Open Defects from the 2026-07-25 Test Pass

**Player outcome**

A background or variant colour that the mod says it applied is the colour actually on the block.

**Experience**

- A colour variant repaints the entire background region, including a pocket enclosed by the subject, without the player setting a tolerance.
- `/cb setbg` on a block that is sitting in the trash says so and names `/cb trash restore`, instead of "No block or selection matched".

**Requirements**

- The scope resolver must check the trash before reporting no match, so a deleted-but-recoverable id gets a useful answer.
- Trash restore losing a block's stored source bytes is G06's defect, not G10's; G10's honest "no stored picture" skip is correct behaviour and stays.
- The trapped-background fill must cover a whole connected background region once that region is accepted. A partial repaint that leaves a hard rectangle of the original colour is a failure of the region walk, not of the thickness gate.
- `/cb tolerance` is being removed, so its number is not re-derived. The replacement is §H, and the partial repaint is fixed by §H's region walk, not by the old ΔE mapping.

**Boundary**

This section is repair of already-shipped behaviour. It adds no new command, value, or surface. Its test rows live under the Testing Guide's §E, not its §F; the letters are not aligned between the two documents for this area alone.

### G. Chat Quietness, Colour Hexes, and Upscale Quality

**Player outcome**

Creating a block says what happened and nothing else, the named colours are the owner's chosen shades, and a small source picture bakes as sharply as it can.

**Experience**

- Creating from a link no longer prints the "flattened preview image" notice or the "smaller than the 512px block / it was enlarged" notice. The fetching line and the result line stay.
- Named colours resolve to hex values the owner sets, in both the colour-variant fills and the colour library.
- A source that has to be resampled to the block size bakes as sharp as it can, whether it was enlarged or shrunk. Owner reported softness in both directions: a 447px source enlarged, and a 954×1484 source reduced.

**Requirements**

- Removing a warning must not remove the behaviour behind it: flattened previews are still flattened and small sources are still enlarged; only the chat line goes.
- The colour-variant fills (`CustomBlocksConfig` triangle hexes) and the colour library (`ColorLibrary`) are set from one owner-supplied list so the same colour name is not two different shades.
- Resample sharpening must not introduce halos on flat colour or double edges on line art, and must stay off when the source is already the target size.

**Discussion later — Discussion ✏️**

The hex values themselves. Owner will supply the list; nothing changes until then.

### H. Automatic Background Detection — replacing `/cb tolerance`

**Player outcome**

The player never sets a strength number. They upload a picture and the background comes off correctly. When the mod is not sure, it leaves the picture alone and says why, rather than quietly wrecking it.

**Experience**

- Background handling is Auto or Off, set once on `/cb config background` exactly as the mode is set today. Nothing to tune, nowhere to type a number, and no per-block mode to keep track of.
- That is separate from `SlotData.background`, which is the fill *colour* a block bakes onto and stays per block. Removal decides what comes off; the stored background decides what goes underneath.
- A genuinely transparent PNG is used as-is; the mod does not second-guess a file that already says what its background is.
- A picture with an obvious flat background comes off cleanly whether that background is white, black, a colour, or a preview grid.
- Thin outlines, a letter's enclosed hole, and shading inside a subject all survive.
- If the mod cannot tell subject from background, it does not guess. It says which picture confused it and points at `/cb bgpick <id> <colour>`.
- `/cb bgpick <id> <colour>` names the background colour outright — a palette name or a `#RRGGBB` hex, the same values `/cb setbg` already takes, tab-completed the same way. The block re-bakes immediately, as a certainty rather than a guess.
- The Background Studio's existing fill swatch gains the same action, so the colour can be picked from a swatch or typed into the anvil instead of remembered as a hex.

**The cascade**

Five rungs, ordered by how certain each one can be. The first rung that passes both its own evidence test and the shared QC gate decides the mask. A rung that fails hands down. Rung 5 always runs afterward and only refines the edge.

| # | Rung | What it uses | When it declines |
| --- | --- | --- | --- |
| 1 | Authored alpha | The file's own alpha channel | The file has no meaningful alpha, or its alpha is a single constant |
| 2 | Known key | A background colour named by `/cb bgpick`, or a flat border tone that is unambiguous on all four sides | The named colour is not actually present in the picture, or the keyed region does not reach the frame edge. A `/cb bgpick` colour that matches nothing is refused out loud, not silently ignored |
| 3 | Boundary-connectivity saliency | How much of each region's own perimeter lies on the frame border | No region separates from the rest by a clear margin |
| 4 | Estimator ensemble | MET, Triangle, Rosin, Li, and weighted-object-variance, combined | The estimators disagree too widely, or no stable plateau exists |
| 5 | Colour unmixing | The known background colour, applied along the edge band only | Never — it refines, it does not decide |

If rungs 1-4 all decline, that is the honest outcome: background removal does not run, the picture bakes on the stored fill unchanged, and the player is told to use `/cb bgpick`. A picture that declines is not a broken picture — it is one the mod was not willing to damage on a guess.

`/cb bgpick` is a re-bake of the stored source, not a new upload, so it needs no download and costs one bake. It requires the same permission tier as `/cb setbg` and records one undoable history action, like every other re-bake.

**Requirements**

- The rungs run in the fixed order above. A later rung never overrides an earlier one that passed.
- Every rung's proposed mask passes the same QC gate — non-empty, not the whole frame, not fragmented, and touching the frame border — before it is accepted. A mask failing QC demotes to the next rung and is not partially used.
- Colour distance is CIEDE2000 everywhere a perceptual difference is compared.
- Every blend of a pixel with a fill happens in linear light. Alpha never goes through the transfer curve. This covers both composites in `BackgroundRemover` and the `Graphics2D` fill in `ImageProcessor.fillBackground`, which blends in gamma space by default.
- The dark-ring compensations that exist only to chase the gamma artifact (`RING_DARK_MAX`, `RING_PASSES`, and the anti-fringe peel) are re-evaluated against goldens once the blend is correct, and removed if the artifact they chase is gone. They are not kept "just in case" on top of a fixed blend.
- Thin-structure cleanup uses an area opening judged on component size, never a morphological opening judged on width.
- Edge decisions use hysteresis: a weak edge pixel connected to a strong one is kept.
- No constant in the new path exists without a stated derivation. A number that cannot be explained is a tolerance knob with a different name.
- Removal mode stays a single global value (`CustomBlocksConfig.backgroundMode`). It is not promoted to per-block, and `SlotData.background` keeps meaning the fill colour only.
- ~~`BgRemove` and `BgRemove&More` still parse as Auto~~ — **superseded 2026-07-26**: legacy spellings now hard reject like any other unrecognised value.
- `BgSmart` is removed entirely: no mode constant, no spelling, no display name, no cycle entry, no tab-complete suggestion.
- The two live mode values are `auto` and `nobackground`. An unrecognised value hard rejects with a logged error, at every entry point (command, config load, startup). It must never silently resolve to Auto or Off. (2026-07-26, supersedes the earlier fail-safe-to-Auto rule.)
- The one exception is the pre-2026-07-26 internal id `none`, renamed to `nobackground` on config load with a warning and rewritten on the next save. It is not a spelling `fromArg` accepts, so typing it is still a hard error.
- Thin-structure cleanup is followed by a mop-up pass over foreground fragments below the QC area floor whose MEAN colour sits within twice the JND of the background the rung decided. The mask's own colour match is never widened to catch them, and rung 1 does not run it.
- The opaque-bake snap runs unconditionally, independent of any removal setting.
- Authored alpha must be read correctly: Java's PNG reader silently drops a `tRNS` chunk on non-indexed PNGs, so rung 1 must not conclude "no alpha" from a decode that quietly discarded it.
- A lossy thumbnail of a dark-checker source keeps a dashed residue row after flattening: JPEG compression DC-shifts whole checker cells off both tones and fuses them with the subject's own edge shading, so no pixel-level rule can take the residue without also taking real shadow — five candidate fixes were golden-diffed and rejected on exactly that trade (2026-07-26, the chrome-logo JPEG baseline). The cascade must catch this case by evidence — rung 4's statistics or rung 5's unmixing — or decline to `/cb bgpick`; a widened per-pixel tolerance is not an acceptable fix.
- Behaviour is captured as golden images from the current jar before any code is ripped, and every later change is reviewed as a diff against them.
- `/cb bgpick` operates on the stored source bytes. A block with no stored source gets the same honest skip `/cb setbg` already gives, not a guessed re-bake.
- `/cb bgpick` takes the same permission tier as `/cb setbg`, tab-completes the same colour values, and records one undoable action.
- Checkerboard flattening runs before rung 1 and in both Auto and Off, unchanged from today. The cascade sees an already-flattened source.
- The recolour rails — variants, families, Arabic — run Auto whatever the mode says, matching the existing `none → edges` behaviour in `recolorBackground`. Off is honoured on a normal bake only.
- The cascade is budgeted per bake with the bulk loops in mind: `/cb retextureall`, the safety re-bake sweeps, and the Arabic bootstrap all run it once per block. Cheap rungs resolve the common cases so the expensive rung stays rare, and the bulk rails must not get slower than they are today.
- Removing the per-block store changes nothing outside the Background Studio, because nothing else reads it. Every other rail already uses the global value.

**Rip scope**

`/cb tolerance` is presently registered **twice** — `ConfigCommands` and `ImageToolCommands` both declare the literal. Both go.

| What | Where | Becomes |
| --- | --- | --- |
| `/cb tolerance` command | `ConfigCommands`, `ImageToolCommands` | Removed, both registrations |
| Per-block override store | `BlockToleranceStore` + `config/customblocks/block_tolerances.json` | Deleted; the file is removed on migration, not left orphaned |
| Rename/delete hooks into that store | `SlotManager`, and the bulk rename fan-out that calls every store's `renameId()` alongside favourites and notes | The tolerance call comes out of the fan-out. Renaming a block must not start throwing because one store in the list no longer exists |
| Global default | `CustomBlocksConfig.backgroundTolerance`, `CustomBlocksConfigStore`, `ConfigRegistry`, `ConfigMenu` | Removed; an existing saved value is ignored and dropped |
| The Arabic coloured-set rail | `ArabicSlotBootstrap:222-226` — reads the global mode and tolerance, recolours, then calls `ImageProcessor.fillBackground` | Runs Auto like every other recolour rail. This one matters more than its size suggests: it bakes the 224 bundled art blocks, and its `fillBackground` call is one of the gamma-space sites, so the Arabic sets are currently getting the dark-edge artifact too |
| Strength buttons | `BgStudioMenu`, `BgStudioSession` | Removed from the panel; mode and fill stay |
| `tolerance` parameter | `BackgroundRemover.apply` (both overloads), `recolorBackground`, `snapBackgroundBlack`, `snapBackgroundColor`, and all 14 calling files: `ArabicSlotBootstrap`, `ColorFamilyOps`, `ColorImageCommands`, `CreationCommands`, `FaceCommands`, `RetextureAllCommands`, `SafetyCommands`, `StudioReskin`, `TempRetextureCommands`, `BackgroundService`, `ColorToolService`, `ColorVariantService`, `BgStudioSession`, `BgStudioMenu` | Removed from the signatures, not defaulted to a hidden constant |
| Six hidden `30` fallbacks | `ArabicSlotBootstrap:223`, `ColorFamilyOps:83/189/299`, `ColorVariantService:170/296`, `BgStudioSession:99` — each written as `tol > 0 ? tol : 30` and commented "0 would mean no recolour at all" | All deleted. They exist only because 0 was overloaded to mean both "strictest" and "off"; Auto has no such ambiguity, and the recolour rails simply always run Auto |
| The `tolerance > 0` snap gate | `BackgroundRemover:357` | Deleted. The opaque bake becomes unconditional, which is what the 2026-07-25 always-opaque decision already required |
| Four modes | `BackgroundRemover` (`NONE`/`EDGES`/`CLOSED`/`SMART`, plus `fromArg`, `displayName`, `commandArg`, `next`), `ConfigMenu`'s cycle, `/cb config background` arguments | `auto` and `nobackground`, and nothing else. `SMART` and its `smart`/`ai`/`bgsmart` spellings are deleted. Updated 2026-07-26: `fromArg` no longer accepts `BgRemove`, `BgRemove&More`, `edges`, `closed`, `off` or the old `none` id — every one of them hard rejects, and the player-facing argument for Off becomes `NoBackground` |
| `SMART`'s hidden default | `BackgroundRemover:114` — `effTol = tolerance > 0 ? tolerance : (smart ? 35 : 0)` | Deleted with the mode. This is the clearest example of the knob hiding inside a mode name |

108 references across 27 source files. The parameter is removed from signatures rather than left in place with a default, so nothing can quietly keep passing a number.

Doc-side, when it lands: `All_Groups.md` and `ID_MAP.md` both list `tolerance` in their command inventories. Those files exist to compare against the legacy command set, so the entry is not deleted — it is marked removed, with `bgpick` added. `HelpTopics` and the in-game help text lose the command the same day the command goes, not later.

**Migration**

An existing per-block or global tolerance value is discarded, not translated. There is no number to translate it into, and keeping one would recreate the knob invisibly. The player is not prompted; the blocks re-bake under Auto on their next bake like every other picture.

**Boundary**

G10 owns the cascade and its thresholds. G27 owns the Background Studio panel that loses its strength buttons. The `/cb bgpick` gesture is a G10 command; if it later becomes an Omni-Tool interaction, that surface is G06.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Generated texture delivery | G10 finishes a coherent texture batch, then G05 distributes it through the normal pack contract. |
| G06 | Squares and Triangles | Tool swaps use G10-generated variants without owning image processing. |
| G07 | Future Bulk Recolor selection and routing | Exact integration waits for the `/cb bulk` Screen migration and later owner discussion. |
| G08 | Shapes and face images | G10 prepares images; G08 maps them correctly on shaped and per-face models. |
| G12 | PNG export and folder import | G10 writes the PNG and owns decode/bake/background rules; G12 names the artifact and reuses G10 for `importfolder`. The non-host-leaking download problem moved to G20 on 2026-07-30. |
| G14 | Transparent rendering | G14 registers the cutout render layer required by G10 transparent backgrounds. |
| G15 | AI background removal | G15 supplies any AI/segmentation integration while G10 retains fallback and image rules. |
| G27 | Coloring Screen | G27 provides the single Screen surface; G10 remains the source of image/color logic. |
| G28 | Undo and redo | Image mutations expose clear batch boundaries for history. |

## Technical Contract

- Background matching and color interpolation use CIE Lab where perceptual distance matters. Comparisons of colour difference use CIEDE2000, not Euclidean CIE76, so one difference budget means the same thing across the whole palette instead of being loose in the blues and tight in the neutrals.
- Any operation that mixes a pixel with another colour does it in linear light: decode sRGB, blend, re-encode. Alpha is a coverage fraction and is already linear — it never goes through the transfer curve. This is a correctness rule, not a quality preference; blending encoded bytes darkens edges against a dark fill.
- Background removal derives its own threshold from the image. No player-set, per-block, or global strength value exists anywhere in the pipeline, and no signature carries one with a default.
- The removal cascade is ordered by certainty and every rung's mask is shape-checked before acceptance. A rung that cannot support its answer hands down; the bottom of the cascade is "do not remove and say so", never a guess.
- The always-opaque bake is unconditional and is not gated on the removal mode or on any threshold being non-zero.
- `SlotData.background` is the sole persisted background value: transparent, black, or color.
- Image re-bake reads the stored background; transparent output uses cutout rendering, while black/color output composites during bake.
- `CheckerboardDetector` runs before normal removal/recolor processing and rejects normal source images unless its strict flattened-preview signature matches.
- The signature is tested once per brightness regime, near-white and near-black, because a site draws the preview grid in its own theme. Only the brightness gate differs between the two passes; neutrality, the two-tone border split, the L* gap and the periodicity run are identical inside each, so covering dark grids does not loosen what counts as a checkerboard.
- Periodicity must hold on BOTH axes. A checkerboard is periodic in two dimensions, so accepting a single alternating axis also accepts plain stripes, which are legitimate artwork.
- Color-family source limits live in one validator that every family rail calls, on downloaded and stored bytes alike. Dimensions are read from the image header, never by decoding first, so an oversized source is refused before it reaches heap.
- Plain create/retexture downloads use a separate, looser cap with no pixel limit, so a block's stored source can legitimately be larger than a color family accepts. Family rails must gate on their own limit rather than assuming a stored source already passed one, and the looser create path must not be silently tightened to the family limit.
- `ColorVariantService` is the shared variant naming and recolor rail; family work adds one batch wrapper, not an alternate variant system.
- Color-family processing validates source limits first, respects locks, preserves non-color attributes on overwrite, emits one pack update, and records one history batch.
- Screens collect inputs and call the command/service rail; they do not reproduce image operations in client UI code.
- No `/cb bulkrecolor` selection or `/cb bulk` integration contract is locked until the later discussion is complete.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Advanced `/cb bulkrecolor` behavior | Only the separate Hub destination is locked; selection, controls, operations, safety, and all other extras need later discussion. | G10 with G07 and G27 |
| AI background removal | Requires the G15 service/integration decision and a safe fallback. | G15 with G10 |
| Color Family builder Screen | Needs a final source-input, palette, preview, and placement design. | G27 with G10 |
| Color styles across every creation route | Cross-cutting behavior needs one contract before commands diverge. | G10 |
| Animated (GIF/WebP) background re-bake | Parked out of stored-background v1 (2026-07-24); static blocks only for now. | G10 with G14 |
| Poster, share code, hologram, and animated family extras | Depend on export, Vault, preview, and animation systems. | G12, G15, G20 |
| Watermark/logo detection and removal (text near-background-colour, not a flat key match) | Needs its own detection+inpaint approach; deliberately kept out of the §H cascade to avoid widening the JND match that protects real subject detail. Raised by the HA4 test pass, 2026-07-26. | G10, possibly G15 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-15 | `/cb dress` handled color overlay. | It is removed; variants and live recolor cover the intended use. |
| 2026-06-24 | Transparency was a global toggle or a one-time fill baked into an image. | Each block stores one editable background value. |
| 2026-06-29 | Smart background handling could paint a whole white field or keyline around dark art. | Standard output uses plain black or the stored background value. |
| 2026-07-10 | The full Bulk Recolor Hub was part of the immediate background fix. | Only stored background control proceeds; the large hub is parked. |
| 2026-07-12 | Separate chest menus owned background, palette, variants, and recolor UI. | G27 consolidates those front ends into one Coloring Screen. |
| 2026-07-25 | The player set a 0-100 background-removal strength, globally and per block, and chose between three removal modes. | The number is deleted entirely (§H). The mod derives its own threshold per picture, and the modes are Auto and Off. |
| 2026-07-25 | Colour difference was Euclidean CIE76 and compositing happened on sRGB bytes. | CIEDE2000 for difference, linear light for blending. The dark-edge compensations built on top of the old blend come out with it. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_10.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G07 Bulk Operations](GROUP_07_BULK_OPERATIONS.md)
- [G08 Shapes and Per-Face Textures](GROUP_08_SHAPES.md)
- [G12 Export & Import](GROUP_12_EXPORT_MARKETPLACE.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G15 AI Textures](GROUP_15_AI_TEXTURES.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 10 snapshot](../archive/group-migration-2026-07-18/GROUP_10_COLOR_IMAGE.md)
