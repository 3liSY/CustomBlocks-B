# ADR-003: Arabic contextual forms as a BlockState property; hybrid (hand-art + engine-drawn) textures

Date: 2026-06-15
Status: Accepted — **DISPLAY MECHANISM SUPERSEDED BY [ADR-005](ADR-005-auto-join-dynamic-texture-rendering.md) (2026-06-18)**

> ⚠️ **Superseded (display only).** Decision parts **1 & 2** below — contextual form as a `SlotBlock`
> `FORM` blockstate property and a `ServerPackGenerator` 4-variant pack branch + `TextureStore` per-form
> textures — are **replaced by ADR-005**: forms are stored on a **letter BlockEntity** and drawn from a
> **live in-memory texture** (no pack rebuild, no reload). Pass 4 also uses a **new dedicated joinable
> letter block**, not the shared `SlotBlock` (reverses the "no new block class" argument in part 1).
> **Still valid:** the *joining rules* (which letters connect, RTL, the 6 non-connectors + cousins,
> numbers never join), part 3 (hybrid hand-art + engine-drawn forms / override folder), and part 4
> (colour = background). See ADR-005 for the current display path.

## Context

Group 13 must make placed Arabic **letter blocks auto-join**: a letter shows its
**isolated / initial / medial / final** form depending on its horizontal neighbours, and the
neighbours re-evaluate when a block is placed or broken (Issue 17.20 / O3). The developer also
wants the join direction **fully customizable at runtime** (axis E–W / N–S / follow-facing, flip
start end, an optional vertical mode, and a per-block manual override).

Two facts about the existing codebase shape the decision:

1. **Blocks are pre-registered (ADR-001).** All `maxSlots` blocks are identical generic
   `SlotBlock`s registered at boot; a slot only *becomes* an Arabic letter at runtime via import.
   So a letter-specific `Block` subclass is not available at registration time.
2. **Glow already proved the pattern (ADR-002).** Per-placement render state that must change live
   is stored as a real `BlockState` property set in `getPlacementState`, with a bounded
   neighbour-rescan updater (`SlotLighting`). ADR-002 explicitly names this "the template for future
   per-block attributes that must affect rendering/behaviour live."

Separately, the developer supplied a **hand-drawn glyph set** (256² RGBA: white glyph, thick black
stroke, solid colour background; 4 colours = 4 backgrounds) covering 28 letters + extra forms
(hamza, ta_marbuta, alef variants, maqsura, waw/ya hamza) + two number sets (Eastern ٠–٩, Western
0–9). The set is **isolated forms only** — it has no initial/medial/final shapes — and its 4 colours
are baked in (stroke and background are both black in the BLACK set, so an arbitrary-hex re-tint of
that set can't separate glyph from background cleanly).

## Decision

**1. Contextual form = a `BlockState` IntProperty on `SlotBlock`** (mirror ADR-002 glow):
- `SlotBlock.FORM = IntProperty.of("form", 0, 3)` — 0 isolated, 1 initial, 2 medial, 3 final.
- `getPlacementState` scans the two horizontal neighbours (per the live join-direction setting),
  determines this block's form, and bakes it into the placed state.
- A local, bounded updater (a small sibling of `SlotLighting`) re-evaluates the ≤2 affected
  neighbours on place and on break. No world-wide scan.
- The 6 non-connecting letters (alef, dal, dhal/thal, ra, zay, waw) only ever take isolated/final.
- Non-letter blocks carry the property too (pre-registration forces this) but never leave form 0;
  their pack model is the `""` catch-all, exactly as glow's 16 states already are.

This keeps the FORM count to 4 (×16 light) and requires **no new block class** and **no world
block-swapping** (the alternative — sibling slots per form — would consume ~100 slots and need
place/break block replacement; rejected as higher blast radius).

**2. Generator emits a 4-variant blockstate only for letter slots.** `ServerPackGenerator` learns one
new branch: when a slot is an Arabic letter, write a `form=0..3` blockstate → 4 form models → 4 form
textures (`slot_X_form{0..3}.png`). Every other slot is unchanged (`""` catch-all). `TextureStore`
gains per-form variants alongside the existing per-face ones (`loadForm`/`hasForm`, mirroring
`loadFace`/`hasFace`). `SlotData` gains an Arabic-letter marker (the letter key) so the generator and
the join logic can recognise letter slots.

**3. Hybrid texture pipeline (art where it exists, draw where it doesn't):**
- **Isolated letters, extra forms, numbers** → the developer's hand-drawn PNGs, used directly
  (bundled in the JAR, auto-extracted; overridable from config).
- **Connected forms (initial/medial/final)** → engine-drawn: shape from the bundled Arabic font
  (`arabtype.ttf`, correct contextual glyph via ZWJ), restyled to match the art (white fill + black
  stroke at the sample-measured thickness + the block's background colour). Chosen for **correct
  joining on the first attempt**; visual polish is iterative via override (below).
- **Custom words / text** (anvil flow) → engine-drawn in the same style (whole word shaped by the
  font in one texture).
- **Per-form drop-in override** → a `config/customblocks/arabic/` folder; any `<name>.png` dropped
  there replaces that exact letter/form/number/word texture on reload. The ultimate control and the
  path to perfect any connected form the font renders imperfectly.

**4. Colour = background, reusing the existing colour system.** The glyph stays white + black stroke;
the *background* carries the colour. Arabic colour variants are ordinary
`ColorVariantService` variants (`arabic_alef_red`, …, or `…_hex_rrggbb`), so the live hex link is
already solved: changing `triangleRedHex` and running the existing `recolorVariants` repaints every
`*_red` block old→new hex (per-pixel, no flood — CLAUDE.md §7). Default is white-on-black; full
control (29-colour library + arbitrary hex, optional glyph colour) layers on top.

## Rationale

- Reuses two proven, in-repo systems (ADR-002 state-property + `SlotLighting`; `ColorVariantService`
  recolour/live-link) instead of inventing parallel machinery — surgical, low blast radius.
- Works within pre-registration (ADR-001): the property lives on the generic block; only the
  generator and join logic special-case letter slots, and only at pack-build / placement time.
- The hybrid pipeline honours the developer's hand-art exactly where the art exists (isolated,
  numbers, extras) and guarantees correct joining where it doesn't, with a drop-in override as the
  quality escape hatch — so "correct first attempt" and "keep my style" are both satisfiable.

## Consequences

- `SlotBlock` state count rises 16 → 64 (light × form) for **every** block. Block states are cheap;
  the pack is unaffected for non-letters (catch-all variant), exactly as ADR-002 accepted for glow.
- `ServerPackGenerator` gains one letter-only branch and `TextureStore`/`SlotData` gain small
  additions — the only shared-infra edits in Group 13. Contained and documented here.
- Connected forms come from the font initially, so a few letters may read more "font-like" than the
  hand-drawn isolated glyph until a hand-made PNG is dropped in. Accepted, with the override path.
- Asset name reconciliation needed: `ArabicLetterMap` uses dhal/tah/dhah/nun; the art set uses
  thal/ta2/tha2/noon. A single mapping table resolves it (engineering detail, not a design choice).
- Join direction is a live config setting; the default (fixed East→West, horizontal) matches the
  Group 13 spec so out-of-the-box behaviour is the documented one.
