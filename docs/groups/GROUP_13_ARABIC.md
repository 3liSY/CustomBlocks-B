# Group 13 - Arabic and Text Blocks

> Group 13 makes Arabic letters behave as real CustomBlocks, then builds reusable Arabic and Latin Text Blocks on that reliable foundation.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_13.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Arabic letters and numbers must use the same real SlotBlock contract as normal CustomBlocks: they can join, face correctly, render readable front and back faces, receive ordinary tools and attributes, and survive client/server synchronization without a separate fragile renderer system.

The Group also owns the backend for editable Text Blocks. G27 owns the text-creation and Arabic Screen surfaces that collect player input and drive these services.

## Ownership

| Owns | Does not own |
| --- | --- |
| Arabic slot metadata, forms, joining, facing, colors, and back-face behavior | Arabic/Text Blocks Screen and text-creation presentation: G27 |
| Arabic letter/number bootstrap and retirement of old Arabic block classes | Square/Triangle/Deleter base behavior: G06 |
| Arabic tool/attribute parity and word-row building backend | Pack delivery: G05 |
| Text Block rendering, persistence, and editing backend | General Screen layout and preview chrome: G27 |
| Arabic-specific testing and reconciliation | Undo/redo engine: G28 |

## Direction

Arabic letters and numbers are real SlotBlocks with Arabic metadata, not an NBT-driven special block or a separate live-render system. Letter forms change by swapping to sibling SlotBlocks, with the server authoritative and the client predicting the same accepted join/reflow result to remove visual delay.

The same foundation supports a rebranded Text Block backend for Arabic and Latin text. It stores editable text/style data and renders a normal block texture; the player-facing creator and editor belong in G27.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-02 | Arabic letters and numbers become real SlotBlocks. | Old Arabic-specific block/render/join systems are retired instead of being patched alongside the new path. |
| 2026-07-03 | Arabic forms use stable sibling slot IDs and `ArabicMeta`. | Isolated, initial, medial, and final forms can swap without creating a second registry model. |
| 2026-07-03 | Front/back facing data lives on the shared block-entity path, not a new blockstate property. | Orientation is synchronized without multiplying shared SlotBlock state variants. |
| 2026-07-03 | Auto-join runs server-authoritatively and predicts on the client. | Place, break, recolor, and packet reconciliation should feel immediate in multiplayer. |
| 2026-07-03 | Black, red, green, and yellow Arabic variants are pre-baked from the configured Square hexes. | Standard colors join across a row and react when configured hex colors change. |
| 2026-07-04 | The old `customblocks:arabic_letter` system and static Arabic block sets are retired. | Old placed blocks may clear to air; no parallel legacy path remains. |
| 2026-07-04 | Arabic Letters creative content lists real Arabic slots. | Letter giving and creative selection use the same blocks that join in the world. |
| 2026-07-12 | Arabic menus, browser, word flow, preview, and color choices consolidate into one Arabic Screen. | G13 keeps backend behavior and must not create competing chest or standalone screens. |
| 2026-07-17 | Arabic text creation is part of general Text Blocks. | Arabic and Latin use one editable text-block backend, not separate command-only systems. |
| 2026-07-30 | Number backgrounds are repainted by an exact solve from the bundled black+red pair, never by background detection. The bundled art itself is regenerated on the live Square hexes. | Coloured numbers stop losing their outline to a background guess. `ArabicNumberArt` reads each source background from the art's own corner, so the art can be regenerated without touching code, and a colour whose hex later changes is repainted exactly rather than approximately. Owner-reported 2026-07-30: the numbers surviving the §H cleanup were the visibly broken ones. Cause: `bake` sent the black art through `BackgroundRemover.recolorBackground`, whose detection eats a `#000000` outline sitting against a `#0A0A0A` background and leaves a rim; the bundled coloured art was never read at all. Verified across the full bundled set: repainting onto a set's original hex reproduces that set's file to within rounding, so only the background moves. Green and yellow also moved to the live hexes `#10FF00` / `#EAFF00`, which the old art predated. |
| 2026-07-26 | The retirement hit-list also covers the pre-prefix legacy id shapes — bare `<glyph>_<colour>` and `num_<n>_<colour>`, with no `arabic_` prefix. Owner-approved full retirement: deleted, indices reclaimed through `RetiredSlots`, placed copies swept to air by the existing chunk-load pass. The two letter strays `ha_red` and `sad` go with them. | Proved from the owner's live `slots.json`, 2026-07-26: 656 correct new-scheme blocks, plus 82 orphans the migration cannot see. `ArabicLetterRetirement.retireStaticArt` builds its ids from `ArabicArt.blockId`, which always prefixes `arabic_`, so `a0_black` and `num_0_black` never match and the sweep reports success every boot while leaving them in place. The letters retired correctly because their old ids did carry the prefix; only the 20 numbers (×4 colours) predate it. Visible to the owner as doubled entries — `a0_black` and `arabic_a0_iso` both display "A0 Black" — with `category: null` on the orphan. Owner confirmed no placed copies are worth preserving. |

## Feature Plan

### A. Real Arabic Slots and Join Flow

**Player outcome**

Placed Arabic letters form readable right-to-left words with correct isolated, initial, medial, and final shapes on both faces.

**Experience**

- Creative/search lists real Arabic letter and number slots rather than old legacy entries.
- Letter forms join horizontally and reflow when a neighboring letter is placed, swapped, or removed.
- Back faces preserve a readable word rather than a mirrored or stale result.
- Letters stay bright and consistent across a mixed-color word.
- Numbers remain inline but do not participate in letter joining.

**Requirements**

- `ArabicSlotBootstrap` creates stable Arabic slot definitions with `ArabicMeta` for glyph, form, color, and number behavior.
- `ArabicSlotJoinFlow` walks a bounded run, chooses form siblings, preserves facing/back data, and handles missing siblings safely.
- `ArabicClientView` receives Arabic metadata through `ClientSlotCache` so dedicated clients can make the same local prediction.
- Join updates use the established swap/prediction pattern and reconcile with server state without flicker.
- Letter rendering uses font-baked form textures and a two-faced client renderer; numbers retain their intended visual source.

**Boundary**

Arabic joining is a SlotBlock behavior. It does not create a new block class, registry, or client-only source of truth.

### B. Arabic Tool and Attribute Parity

**Player outcome**

An Arabic slot behaves like every other CustomBlock when a player recolors, deletes, edits an attribute, or uses undo/redo.

**Experience**

- Deleter uses the shared Recycle-Bin deletion path.
- Squares preserve letter facing, back partner, and join flow while switching to the matching configured color.
- Glow, hardness, sound, collision, and other normal attributes apply and refresh normally.
- Custom-color behavior either creates a joining sibling variant or gives an honest unavailable result; it never creates a silently non-joining surprise.

**Requirements**

- Square swaps capture/reapply Arabic facing/back data and reflow the affected run.
- Standard four colors use the pre-baked sibling set; custom hex needs an explicit sibling-linked creation path before being treated as parity.
- Attribute mutation follows normal SlotBlock command/service routes and client refresh contracts.
- G28 receives one clear operation boundary for reversible Arabic mutations.

**Boundary**

G13 supplies Arabic metadata preservation. G06 owns the core tool action and G28 owns history behavior.

### C. Type a Word

**Player outcome**

The player can enter or paste a word and build a connected row of real Arabic slots instead of placing every letter manually.

**Experience**

- The Arabic Screen invokes a word-build action using the entered text.
- Arabic builds right-to-left; spaces create gaps and digits remain inline without joining.
- An occupied target stops cleanly and reports the placed portion.
- One undo removes the entire created word row.

**Requirements**

- Text input bypasses Brigadier's Arabic-character limitation through the in-game Screen/anvil input route.
- The builder maps each glyph to the real Arabic slot family and uses the existing join flow.
- Placement validates world space before each block and records one batch history action.
- The command/Screen route does not revive the retired old Arabic-word block system.

**Boundary**

This feature builds real letter slots. It is separate from a generated Text Block texture.

### D. Text Blocks Backend

**Player outcome**

Arabic and Latin phrases can become editable normal CustomBlocks with stored text and style data rather than a one-time image that cannot be changed.

**Experience**

- The creator renders Arabic with the bundled Arabic font and Latin through the selected non-Arabic font path.
- Existing text can reopen for edit instead of generating a duplicate feature path.
- Text, colors, and later styling persist through restart.
- A preview can update without forcing a full resource-pack send before the player commits.

**Requirements**

- Text data stores content, script/font choice, foreground/background colors, and supported style settings.
- Rendering produces a normal SlotBlock texture and uses the normal pack/HUD lifecycle when committed.
- The preview route remains temporary and pack-free; cancel/back preserves the player's editing context.
- Multi-line, multi-block phrase, outline, gradient, shadow, border, and size work expand only through the same Text Block data model.

**Boundary**

G13 owns rendering and persistence. G27 owns the Text Studio tab and Arabic Screen interaction design.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Generated Arabic/text textures | G13 produces coherent slot textures; G05 distributes committed pack changes. |
| G06 | Tool behavior | G06 tools call ordinary SlotBlock behavior; G13 preserves Arabic metadata during swaps. |
| G10 | Color families and images | Arabic standard colors read configured color values without duplicating image/color infrastructure. |
| G08 | Shape and orientation behavior | Native orientation remains the source for Arabic faces and join data. |
| G27 | Arabic and Text Block Screens | G27 collects input and presents editors; G13 provides all backend mutations and rendering. |
| G28 | History | Word builds, swaps, attributes, and text edits expose normal reversible operation boundaries. |

## Technical Contract

- Real Arabic blocks are SlotBlocks marked by `ArabicMeta`; no legacy Arabic block class or separate registry is retained.
- Forms use stable sibling slot identifiers and are switched through the established placed-block swap path.
- Arabic metadata is synchronized to dedicated clients through the client slot cache so prediction and server reconciliation share the same inputs.
- Facing and back-face information persist through the shared block-entity data path and are restored after swaps/reflow.
- Pre-baked black/red/green/yellow variants use configured Square hex values; custom-hex parity requires sibling-linked variant creation.
- Text Block creation persists editable data, renders through the normal texture pipeline, and keeps preview state separate from committed block state.
- G27 screens call G13 services rather than reimplementing Arabic/text operations in the client.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Custom-hex joining Arabic variants | Requires safe sibling-linked creation and reflow behavior. | G13 with G06 |
| More immediate break reflow | The shared prediction path needs a focused multiplayer timing pass. | G13 |
| Advanced Text Block styles and phrases | Expands after the base editable Text Block model and G27 screen are stable. | G13 with G27 |
| `/cb arabic` command UX revision | Needs a separate owner discussion after Screen consolidation. | G13 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-02 | Arabic letters used a special live-render/NBT block system. | They are real SlotBlocks with Arabic metadata and sibling forms. |
| 2026-07-04 | Static Arabic letters/numbers and the old Arabic creative tab remain alongside the new system. | The old system is retired; the tab lists real Arabic slots. |
| 2026-07-12 | Arabic browser, word choice, color studio, and preview remain separate menus/screens. | One G27 Arabic Screen owns the player flow. |
| 2026-07-17 | Arabic text has its own creation surface. | It is part of the general Text Blocks backend and G27 Text Studio. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_13.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G08 Shapes and Per-Face Textures](GROUP_08_SHAPES.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 13 snapshot](../archive/group-migration-2026-07-18/GROUP_13_ARABIC.md)
