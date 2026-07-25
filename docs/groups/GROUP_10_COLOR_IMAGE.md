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
| 2026-07-25 | A stored or typed mode value that is no longer recognised resolves to **Auto**, never silently to Off. | `normalize` currently answers Off for anything unrecognised. After the rip that would turn a server whose config still says `smart` into one that quietly stopped removing backgrounds, with nothing in chat to explain it. Failing toward "still works" is the only safe direction here. |
| 2026-07-25 | Off means off for a normal bake, but the **recolour rails always run Auto regardless of mode** — colour variants, colour families, and the Arabic coloured sets. | This is already how the mod behaves and it is not optional: `recolorBackground` forces `none` to `edges`, and six separate call sites substitute a tolerance of 30 whenever the real one is 0, each commented "0 would mean no recolour at all". Painting a background a new colour requires knowing where the background is, so with detection genuinely off a variant comes out identical to its base. Written down because the Auto/Off collapse would otherwise read as a promise that Off is always honoured. |
| 2026-07-25 | The per-block tolerance override is deleted rather than ported, and its removal is **not** a behaviour change for most of the mod. | `BlockToleranceStore.effective()` has exactly one caller — `BgStudioSession`. Creation, retexture, faces, colour families, variants, safety re-bakes, temp retexture, and Arabic all read the global value directly. So `/cb tolerance 60 <id>` already sets a number that almost every rail ignores, which is its own defect and removes any argument for preserving it. |
| 2026-07-25 | Checkerboard flattening stays a **pre-pass that runs before the cascade**, in Auto and Off alike, exactly where it runs today. | `CheckerboardDetector.flattenToBlack` is the first line of `process()` and applies even in `none`, because a flattened preview grid is a broken source in every mode. It answers a different question from the cascade — "is this grid real content" rather than "which region is background" — so folding it into a rung would make both harder to reason about. |
| 2026-07-25 | Auto's cost is budgeted **per bake, and bulk rails bake many blocks**. The cascade must not make `/cb retextureall` or a safety re-bake sweep worse than the work it replaces. | Fourteen call sites run the removal path, several of them in loops over every block on the server. A cascade that is affordable once and ruinous a thousand times would ship as a hang, not as a quality win, and the early rungs are cheap precisely so most pictures never reach the expensive one. |
| 2026-07-25 | The single escape hatch is **`/cb bgpick <id> <colour>`** — the player names the background colour, which seeds rung 2 as a known key. No retry command, no strength nudge, no hidden number. | An escape hatch that re-runs the same algorithm and hopes is not an answer. Naming the background supplies the one fact the algorithm was missing, which is why it promotes the picture to the most certain rung that can use it. It takes a colour rather than a clicked pixel because the mod has no surface for clicking inside a source image, and inventing one would be a Screens project; a colour name or hex reuses the picker `/cb setbg` and the fill swatch already have. |
| 2026-07-25 | The opaque-bake snap is unconditional. It must not be gated on any removal setting. | It is currently gated on `tolerance > 0` (`BackgroundRemover:357`), so with removal off the always-opaque rule from earlier today silently does not apply and the cutout-layer hairline comes back. The gate goes out with the number. |
| 2026-07-25 | Behaviour is frozen as **golden images** built from the current jar *before* any rip lands. | Without a before picture, "the new one looks fine" is an opinion. Baselines captured first turn every later change into a visible diff, and the pictures that already have owner verdicts (chrome logo, Tux, letter O, the JPEG thumbnail, the striped artwork) are the ones that matter. |

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

G10 provides commands and algorithms. G27 provides their consolidated editing surface; G12 owns the shared export/download presentation problem.

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
- `BgRemove` and `BgRemove&More` still parse as Auto, so an existing `/cb config background BgRemove&More` does not become an error.
- `BgSmart` is removed entirely: no mode constant, no spelling, no display name, no cycle entry, no tab-complete suggestion. A config still holding it resolves to Auto.
- An unrecognised mode value resolves to Auto. It must never fall through to Off, which would stop background removal with nothing said.
- The opaque-bake snap runs unconditionally, independent of any removal setting.
- Authored alpha must be read correctly: Java's PNG reader silently drops a `tRNS` chunk on non-indexed PNGs, so rung 1 must not conclude "no alpha" from a decode that quietly discarded it.
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
| Four modes | `BackgroundRemover` (`NONE`/`EDGES`/`CLOSED`/`SMART`, plus `fromArg`, `displayName`, `commandArg`, `next`), `ConfigMenu`'s cycle, `/cb config background` arguments | Auto and Off. `SMART` and its `smart`/`ai`/`bgsmart` spellings are deleted. `fromArg` still accepts `BgRemove` and `BgRemove&More` as Auto so an existing config line does not hard-error, and anything else unrecognised also lands on Auto rather than Off |
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
| G12 | PNG export presentation | G10 writes the export; G12 resolves the safe non-host-leaking download/share experience. |
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
- [G12 Export and Marketplace](GROUP_12_EXPORT_MARKETPLACE.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G15 AI Textures](GROUP_15_AI_TEXTURES.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 10 snapshot](../archive/group-migration-2026-07-18/GROUP_10_COLOR_IMAGE.md)
