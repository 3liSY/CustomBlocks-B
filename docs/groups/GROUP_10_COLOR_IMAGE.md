# Group 10 - Color and Image Tools

> Group 10 owns the image and color rules that turn a source into a clean, editable CustomBlock without losing the player's intended design.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_10.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Image work should be predictable from first upload through later edits. A player can remove or fill a background, resize or export a texture, make color variants, keep palettes, and later change the saved background choice without an irreversible one-time bake.

This Group owns image processing, color algorithms, source validation, and the commands behind color tools. It does not own the common editing Screens, client render-layer registration, resource-pack delivery, recolor-tool interaction, or AI service integration.

## Ownership

| Owns | Does not own |
| --- | --- |
| Background processing, tolerance, palettes, gradient, resize, export, and color variants | Editing Screens, tab layout, live recolor, and eyedrop UI: G27 |
| Stored per-block background setting and re-bake behavior | Cutout render-layer registration: G14 |
| Flattened-checkerboard detection and image safety limits | Pack generation and transport: G05 |
| `/cb colorvariants` command rail and color-family rules | Square/Triangle interaction: G06 |
| Bulk Recolor behavior and its image engines | Bulk selection, confirmation, and workbench behavior: G07 |
| Color styles as a shared creation concern | AI segmentation integration: G15 |

## Direction

Background is one stored per-block attribute with three values: transparent, black, or a color. Every surface writes that same value, then the image is re-baked from source data. The default is black so ordinary art is readable and checkerboard preview images do not become part of the texture.

The player-facing editor is one G27 Coloring Screen, but all image rules remain in G10. Image processing is cautious: reject unreasonable source sizes before mutation, preserve real subject pixels, and use only one pack update for a completed multi-block color operation.

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
| 2026-07-18 | The stored background can be changed for one block, a chosen selection, or all blocks, and is available through a background tool alongside the other tools. | Exact commands, Screen controls, selection flow, and tool gestures remain Discussion ✏️ for later. |

## Feature Plan

### A. Core Image and Color Commands

**Player outcome**

Players can make common image changes with predictable output, then find the same capabilities through the unified Coloring Screen.

**Experience**

- `/cb gradient <id1> <id2> <steps>` creates perceptually even intermediate colors as one reversible batch.
- `/cb bgstudio <id>` and `/cb tolerance` control background processing with corners, flood, and no-removal behavior.
- `/cb palette save/load/list/delete` persists reusable palettes per player.
- `/cb resize <id> <64|128|256>` resamples the source texture deliberately.
- `/cb exportpng <id>` writes a local PNG export without relying on a server-address-leaking chat link.
- `/cb colors` and `/cb customcolor` use the same color library and image rules as the Screen path.

**Requirements**

- Gradient interpolation uses CIE Lab and records one batch history action.
- Background tolerance supports clear global and per-block behavior.
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
- Command, Studio, bulk control, and the background tool all show and write the same choice.
- Background control supports one block, a chosen selection, and all blocks without creating separate background systems.
- Existing blocks migrate silently to black unless their existing transparent setting identifies them as transparent.

**Requirements**

- `SlotData.background` persists the selected value.
- Re-bake composites to black or the chosen color, or preserves alpha for transparent.
- Slot blocks render on the cutout layer so atlas-backed alpha is visible.
- Full, slab, stair, cross, and other shape models avoid interior-face/culling artifacts with background choices.
- A no-source block is skipped with an honest warning instead of being degraded by a guessed re-bake.
- Single, selected, all-block, Studio, and tool routes write the same `SlotData.background` value.

**Discussion later — Discussion ✏️**

Exact command names and syntax, suggestions and permissions, selection UI and filters, Studio placement, the background tool's item/name/obtainment and click or sneak gestures, legacy-global migration mechanics, per-face and animation edge cases, heavy-job limits, confirmation, progress, cancellation/rollback, and exact single/bulk undo behavior are intentionally not decided yet.

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
- A future builder Screen gathers inputs and calls this same command rail rather than creating a second backend.

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

- Background matching and color interpolation use CIE Lab where perceptual distance matters.
- `SlotData.background` is the sole persisted background value: transparent, black, or color.
- Image re-bake reads the stored background; transparent output uses cutout rendering, while black/color output composites during bake.
- `CheckerboardDetector` runs before normal removal/recolor processing and rejects normal source images unless its strict flattened-preview signature matches.
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

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_10.md) · [All Groups](README.md)

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
