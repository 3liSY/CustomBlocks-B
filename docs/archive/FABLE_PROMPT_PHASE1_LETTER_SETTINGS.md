# Fable prompt — Group 13 letters + numbers become real SlotBlocks (full rearchitecture)

> **REWRITTEN 2026-07-02.** The original version of this file (a small "settings sheet" parity
> layer) was built (2 of its 4 steps) and then reverted the same session — the owner clarified the
> real ask was misunderstood. This is a complete replacement, not an edit of that plan. If you have
> any memory of the old version, discard it; nothing in it is current.
>
> This is ONE big, exhaustive build — not a small phased prompt like the rest of this project's
> Group 13 roadmap. It is being handed to a more capable execution agent specifically so it can be
> built in one sitting. That does NOT relax this project's Golden Rule: **nothing is ✅ done until
> the developer runs it in-game and confirms it works.** This file is organized into numbered
> CHECKPOINTS — build one, get a jar, stop and wait for in-game confirmation, then move to the next.
> Do not skip a checkpoint's in-game gate because a later one "obviously depends on it anyway."
>
> Paste everything below the line into a new chat.

---

Read `CLAUDE.md` at the repo root first — those are the hard rules for this project. The ones that
matter most for THIS task specifically:
- No monolith files: any `.java` ≤ 500 lines, command handler ≤ 400 lines. This task adds a
  meaningful amount of new code — split into multiple small classes from the start, don't write one
  giant class and split it later.
- Nothing is done until I've tested it in-game and said so. A green `./gradlew.bat build --no-daemon`
  means it compiles and the gates pass — say exactly that, never more.
- Every class gets a header comment (responsibility, depends-on, called-by).
- Client never mutates server state; server is authoritative.

Then read `docs/groups/GROUP_13_ARABIC.md` → search for `G13-25` for the same decisions in longer
narrative form (useful if anything below is ambiguous), and `HANDOFF_G13_UNIFY.md` at the repo root
for background context. This file is the authoritative spec — if those two disagree with this file,
this file wins (it's the most recent).

---

## 1. The real goal, in the developer's own words

> *"i wanted to make all the arabic auto joining block letters system to be slotblocks and like
> normal, and old system removed, so when i use deleter tool it deletes definition, when i use
> triangles it just does it without any issues, and auto joining still same as is with no bugging it
> or anything."*

Today, `ArabicLetterBlock` (the auto-join letter) is a completely separate system from `SlotBlock`
(every normal custom block): no shared `SlotData`, a live per-frame texture instead of a baked PNG,
and every tool (`Deleter`, the Triangle/Square colour tools, attribute commands) has a special-case
branch — or no branch at all, meaning it silently does nothing — for Arabic letters. That's why
Deleter "bugs out," Triangle does nothing, and colour is stuck at 4 hardcoded options.

**The fix is not a settings layer bolted onto the old system. It's making letters (and numbers,
folded into this same rebuild) real, first-class members of the SAME system normal blocks already
use** — so every tool that already works on a normal block works on a letter automatically, with
zero special-casing, because a join-letter genuinely IS a `SlotBlock` from the type system's
perspective. The old `ArabicLetterBlock` / `ArabicLetterBlockEntity` /
`ArabicLetterBlockEntityRenderer` system is retired once this ships — not kept "just in case."

## 2. Why this is possible without breaking auto-join (read this before objecting to the plan)

The reason letters were kept separate in the first place was real: a `SlotBlock`'s texture is baked
once at creation and never changes shape, but a letter's shape changes live depending on its
neighbours (isolated / initial / initial / medial / final). Baking ONE slot per letter would freeze
its shape forever and kill auto-join.

The actual fix: bake **one real slot per (letter × contextual form)** instead of one per letter, and
make auto-join **swap which registered block is placed** as neighbours change, instead of writing an
internal "form" field on a shared block entity. This is not a novel, risky idea — it is the *exact*
mechanism this codebase already uses, today, proven, for a different feature:

```java
// core/ColorVariantService.swapPlaced() — already does this, live, in the world, no flicker:
world.setBlockState(pos, block.getDefaultState()
        .with(SlotBlock.LIGHT, SlotManager.glowFor(target.index())));
```

That's a Square colour tool swapping a placed block to a DIFFERENT registered `SlotBlock` live, in
response to a player action, and it already works correctly with no visible glitch. Auto-join needs
the same operation, just triggered by `ArabicJoinFlow` instead of a player click, and swapping
between a letter's 4 *form* siblings (same colour) instead of between colour variants.

## 3. Locked decisions — do not re-litigate any of these

1. **Real slot per (letter × form).** 36 letters × 4 contextual forms (isolated / initial / medial /
   final) = **144 base registrations**. Each is a genuine slot: real `SlotData` (glow, hardness,
   sound, collision), a real baked PNG texture (§5), a real `BlockItem`.
2. **Numbers are folded into this same rebuild** (not a later phase). Numbers never grow connecting
   bars — always isolated, so 1 form each: 10 Eastern Arabic-Indic (`a0`-`a9`) + 10 Western
   (`e0`-`e9`) = **20 base registrations**, no sibling-form complexity.
   **Total base slots to pre-bake: 144 + 20 = 164.**
3. **The 224 bundled static Arabic art blocks are RETIRED** (`arabic/ArabicBlockRegistry` /
   `ArabicArt` / `ArabicLetterRetirement`'s existing 144-static-letter retirement, extended). They're
   redundant — a real join-letter now does everything they did, plus joins.
   **The 80 old static number blocks are ALSO retired**, but ONLY after the new auto-join numbers
   exist and are confirmed working in-game — never retire a system before its replacement is proven
   (this sequencing rule is inherited from the original numbers-join design, G13-20, and still
   applies).
4. **Base (black) forms are pre-baked at boot, every time, idempotently.** Not created lazily. All
   164 base registrations exist from the moment the jar loads. Colour variants (anything beyond
   default black) are still created **on demand**, same as every other custom block's colour
   variants work today.
5. **Colour scope = full parity with normal blocks.** The 4 Triangle presets (red/yellow/green/
   black) AND the custom-hex tools (`CustomColorToolItem` / `/cb customcolor`) both work on letters
   and numbers exactly as they do on any normal custom block. Not limited to 4 colours.
6. **Sibling-linked CREATION, independent EDITING.** The first time ANY of a letter's 4 forms gets a
   new colour, all 4 sibling form-slots for that letter+colour are created together, in one action —
   this is a correctness requirement: auto-join swaps between forms of the SAME colour, and if a
   sibling form-slot didn't exist yet the swap would have nothing valid to land on (a visible glitch
   exactly when two letters join — the "bugging out" this whole rebuild exists to prevent). AFTER
   that one-time linked creation, each of the 4 form-slots is independently Deleter/Triangle/
   Square-able — recolouring or deleting one form does not touch its siblings. A word CAN end up
   showing an intentionally mixed colour mid-edit; that's an accepted, deliberate tradeoff, not a bug.
   Numbers have only 1 form, so this rule is moot for them (nothing to link).
7. **Bulk "whole letter" actions exist, but their exact UX is OUT OF SCOPE for this build.** There
   must be a way to act on all 4 forms of a letter+colour at once on purpose (bulk recolour, bulk
   delete), on top of #6's per-form-independent tooling. The developer wants a dedicated future
   conversation to redesign the entire `/cb arabic` command surface, and does not want that
   redesigned here as a side effect of this rebuild. For THIS build: implement the simplest possible
   version — sneak+Triangle / sneak+Square on a placed letter or number applies to all 4 sibling
   forms at once (plain click = single form, per #6) — and leave it there. Do not build a `/cb`
   command form, a GUI, or any other surface for this in this pass; do not redesign `/cb give`,
   `/cb arabic give`, or any existing Arabic command's shape. If in doubt about scope here, build
   less, not more, and flag it for the follow-up conversation instead of guessing.
8. **Slot cap raised to 1448** (`CustomBlocksConfig.maxSlots`, currently 800) — comfortable room for
   the 164 pre-baked base forms (retiring the 224 static letters frees more indices than this costs)
   plus headroom for colour variants created over time, well under the 8192 hard ceiling.
9. **No migration needed.** This is a dev/test world. Existing placed (old-system) letters can be
   broken and re-placed by hand after this ships. Do not build a migration/placement sweeper.
10. **Nothing that currently passes may regress.** Auto-join quality, the readable back face,
    placement facing, naming — all must look and feel exactly as good as they do today (confirmed
    baseline: `docs/testing/GROUP_13_TESTING_GUIDE.md` §B/E/F/G/H/I/J/K/L). Reuse the existing proven
    naming scheme (§4) and texture-generation function (§5) verbatim rather than re-deriving them, so
    there is no room for a subtle pixel/behaviour drift.

## 4. Naming — reuse the existing locked scheme, do not invent a new one

**Letters:** `arabic/ArabicNaming.java` already implements the locked ADR-006 scheme —
`displayName(letter, color, form)` → e.g. `"Jeem Black"` (isolated), `"Jeem Black Mid"` (medial);
`virtualId(letter, color, form)` → e.g. `"Jeem_Black"`, `"Jeem_Black_Mid"`. Use `virtualId(...)` as
the real slot's `SlotData.customId()` and `displayName(...)` as `SlotData.displayName()`, directly,
unmodified.

**Numbers:** `arabic/ArabicArt.java` already implements the scheme used for the (soon-retired)
bundled static numbers — `blockId(glyph, color)` → e.g. `"arabic_a0_black"`; `displayName(glyph,
color)` → e.g. `"A0 Black"`. Use these directly for the new real-slot numbers (numbers have no form
suffix — there's only ever one). `ArabicArt.ALL` already enumerates the Eastern/Western glyph set
(`Group.EASTERN` / `Group.WESTERN`); don't hand-roll the digit list again.

## 5. Texture generation — reuse the existing proven function verbatim

`arabic/ArabicTileRenderer.render(char letter, int form, int fgArgb, int bgArgb)` already produces
PNG bytes for ANY (letter, form, colour) combination — pure Java2D/AWT, no GPU needed, runs
server-side fine. It's the exact function the CURRENT live renderer calls every time it needs a tile
(`client/render/ArabicLetterBlockEntityRenderer.build()`). **Baking a real slot's texture is calling
this exact function once and saving the result** via `TextureStore.save(index, png)` — do not write
a new renderer, do not reimplement glyph shaping.

Exact colour values already locked in `ArabicLetterBlockEntityRenderer.bgArgb()` — reuse verbatim so
there is zero pixel drift from today's look:
- Foreground (the glyph itself): always `WHITE` = `0xFFFFFFFF`.
- Black (default): `0xFF0A0A0A` — **note this is NOT pure `0xFF000000`.** Getting this wrong is a
  visible regression.
- Red: `0xFFFF0000` · Green: `0xFF1E8C1E` · Yellow: `0xFFF0C814`.
- A new custom-hex colour variant: call `render()` again with the custom colour as `bgArgb`.

**Numbers:** the bundled static numbers already have hand-drawn art PNGs (`ArabicArt.resource(g,
color)`, real JAR resources, already exist for all 4 bundled colours). Prefer reusing those bytes
directly for the black/red/green/yellow base + preset variants (matches today's numbers exactly,
zero regression risk) rather than font-rendering numbers through `ArabicWordRenderer`'s numeral
recipe. For a custom-hex number colour, recolour the bundled art PNG's background the same way
`ColorVariantService.createVariant` already recolours any block's background
(`BackgroundRemover.recolorBackground`), not a from-scratch font render.

## 6. Architecture — the new block class

Plain `SlotBlock` has no `FACING` blockstate and no join-flow hooks. Plain `ArabicLetterBlock` has no
real `SlotData`/baked texture. Introduce **one new class, `ArabicSlotBlock extends SlotBlock`**
(package `block`, mirror the header-comment style of the existing block classes):

- Inherits everything `SlotBlock` already does: texture read via `TextureStore` keyed by its slot
  index, `SlotData` glow/hardness/sound/collision reads, and — critically — it satisfies every
  existing `instanceof SlotBlock` check throughout the codebase (Deleter, `ShapeToolItem`,
  `DeleteCommands`, `HudRenderer`, `CustomToolItem`, etc. — grep for `instanceof SlotBlock` to find
  all ~20 call sites). This is what makes tool parity "just work" with minimal new tool-side code:
  most of it is DELETING the old `instanceof ArabicLetterBlock` special-case branches (e.g.
  `item/ShapeToolItem.java`'s `if (clicked instanceof ArabicLetterBlock) { ... }` block), not adding
  new ones.
- Adds the `FACING` blockstate property (copy `ArabicLetterBlock.FACING` / `Properties
  .HORIZONTAL_FACING`) and the placement-orientation logic (`getPlacementState` /
  `joinFacing`/`perpendicularToward` — port from `ArabicLetterBlock` largely as-is, this part already
  works and is confirmed in-game).
- Adds `onPlaced` / `onStateReplaced` hooks that call into the rewritten join-flow (§7) — but ONLY
  when the slot's `SlotData` carries Arabic metadata (§6.1); a normal custom block registered as
  `ArabicSlotBlock`... **wait — do NOT register every normal slot as `ArabicSlotBlock`.** Only the
  164+ letter/number slots get this class; every other index keeps plain `SlotBlock`. This means
  `SlotManager.registerAll(max)`'s per-index loop (`core/SlotManager.java`, currently
  `new SlotBlock(i, settings)` uniformly) needs to pick the class per-index. Since which indices end
  up holding letters/numbers is only known once the pre-bake step (§8) has run, sequence boot as:
  reserve/create the 164 base `SlotData` entries FIRST (claiming their indices, deterministically,
  idempotently), THEN run `registerAll`, consulting a small lookup ("is this index Arabic?") built
  from what was just reserved, to decide `ArabicSlotBlock` vs `SlotBlock` per index. Verify the exact
  current boot order in `CustomBlocksMod.onInitialize` before changing it — `registerAll` currently
  runs, THEN `loadAll`; you likely need to reorder or split registration so Arabic reservation happens
  before the per-index class decision. Think this through carefully and explain your chosen order
  before writing it — this is the trickiest sequencing point in the whole rebuild.

### 6.1 Where Arabic metadata lives

Do not invent a parallel storage file. `core/SlotData.java` already grows one optional field per
phase (e.g. `anim` was added this way — see its header comment and the back-compat constructors).
Add ONE new optional field the same way: a small nullable record, e.g.
`ArabicMeta(String glyphId, int form, String colorKey)` where `glyphId` is the letter's art base
(`arabic/ArabicGlyphs.artBase(char)`, e.g. `"jeem"`) or a number's idBase (e.g. `"a0"`), `form` is
`ArabicJoining.ISOLATED..FINAL` (always `ISOLATED` for numbers), `colorKey` is the bundled colour
name or custom-hex key. `null` for every normal block — zero behaviour change for the other ~1284
slots. Serialize it in `core/SlotDataStore.java` the same way `anim` is serialized (one more optional
JSON object, omitted when null/absent — follow the exact pattern already there, including the
missing-field-defaults-safely rule).

Do not store each slot's 4 sibling indices redundantly (drift risk if one gets deleted/recreated
independently per decision 6). Instead, look up siblings on demand: given a `glyphId` + `colorKey`,
find the 4 (or 1, for numbers) `SlotData` entries whose `ArabicMeta` matches — a simple linear scan
over `SlotManager.assignedSlots()` filtering on `ArabicMeta`, or a small in-memory index rebuilt on
load if that scan proves too slow in practice (unlikely at this scale — a few hundred to low
thousands of slots).

## 7. Join-flow rewrite

`arabic/ArabicJoinFlow.onPlace` / `.onBreak` currently call `ArabicLetterBlockEntity.setForm(int)` to
change a shared block-entity's internal state. Rewrite these to instead **resolve the target sibling
slot** (same `glyphId` + `colorKey`, different `form`) and swap the placed block via
`world.setBlockState(pos, targetBlock.getDefaultState().with(ArabicSlotBlock.FACING, currentFacing)
.with(SlotBlock.LIGHT, SlotManager.glowFor(targetIndex)))` — mirroring
`ColorVariantService.swapPlaced` exactly (§2). If the target sibling doesn't exist yet (shouldn't
happen given decision 6's linked creation, but a placed letter's colour predates this rule, or data
is otherwise inconsistent), fail safely: log it, leave the block as-is, do not crash or silently
corrupt the placement.

The readable-back-face mechanic (mirror partner's glyph on the back face) and the seam-flush
rendering (`z = 1.0` on all faces) currently live in `ArabicLetterBlockEntityRenderer` — a
BlockEntity renderer, which a real `SlotBlock`-family block does not use (its texture is a normal
baked model, not a live BlockEntity draw). Read how `SlotBlock` normally renders (baked model +
resource pack) and figure out how the readable-back-face effect translates to that pipeline — this
is a genuine open technical question, not a solved one. If a faithful port isn't achievable within
the baked-model pipeline without real risk, STOP and ask the developer rather than shipping a
degraded version — the back-face behaviour is `docs/testing/GROUP_13_TESTING_GUIDE.md` §E, already
✅, and must not regress.

## 8. Boot pre-bake pipeline

A new small routine (own class, e.g. `arabic/ArabicSlotBootstrap.java`), called once from
`CustomBlocksMod.onInitialize` after slot registration/load: for each of the 164 base
(glyphId, form, "black") combinations, if no `SlotData` with that `ArabicMeta` already exists, claim
a slot (mirror `SlotManager.createNoSave` — see how the 224 bundled letters already do a batch import
without rewriting `slots.json` per block, `arabic/ArabicBlockRegistry.importArt`, and follow the same
pattern), render its texture (§5), save it, and set its `ArabicMeta`. Save once at the end of the
batch. Idempotent: re-running on a later boot with all 164 already present does nothing.

## 9. Checkpoints — build one, jar, stop for in-game confirmation, then the next

**Checkpoint 1 — Foundation (no visible change).** `SlotData.ArabicMeta` field + `SlotDataStore`
(de)serialization · `ArabicSlotBlock` class (extends `SlotBlock`, adds `FACING` + join hooks that
no-op when `ArabicMeta` is null) · `SlotManager.registerAll` per-index class selection (§6) · raise
`maxSlots` to 1448. Build-green only. **Regression check before moving on:** every EXISTING normal
custom block still behaves identically (place, recolour, glow, delete) — this checkpoint touches
core registration code, so verify nothing broke for the ~1284 non-Arabic slots.

**Checkpoint 2 — Pre-bake.** `ArabicSlotBootstrap` (§8) creates the 164 base slots with correct
`SlotData`/`ArabicMeta`/baked textures on boot. No placement/join wiring yet. Build-green + a simple
sanity check (e.g. `/cb list` shows the new entries with correct names per §4, or `/cb give
Jeem_Black` hands over a real item).

**Checkpoint 3 — Join-flow rewrite (highest-risk step, test thoroughly).** `ArabicJoinFlow` swaps
real blocks (§7) instead of writing a form field. Retire the OLD placement path for NEW letter/number
placements (stop using `ArabicLetterBlock`'s item/give path for anything new — you'll likely need to
decide what `/cb give <letter>` now hands the player; keep it simple and functionally equivalent to
today, remember §3.7 — do not redesign this command). **In-game test:** place a 3+ letter word, watch
it join in real time exactly as smoothly as today, break a middle letter, walk around it, check the
back face. This is where a subtle flicker/regression is most likely to hide — do not rush past it.

**Checkpoint 4 — Tool parity.** Remove the old `instanceof ArabicLetterBlock` special-case branches
now that `instanceof SlotBlock` matches Arabic slots automatically (grep for `ArabicLetterBlock` in
`item/`, `command/handlers/` and audit each). Implement sibling-linked colour creation (decision 6) —
likely a new small service class mirroring `ColorVariantService.createVariant` but creating 4 sibling
slots atomically instead of 1. **In-game test:** Deleter removes a letter/number and its definition
cleanly; Triangle creates a colour variant that immediately works as the word re-joins around it
(all 4 forms exist, not just the one you clicked); Square swaps colour on a placed letter/number.

**Checkpoint 5 — Bulk actions.** Sneak+Triangle / sneak+Square on a letter/number applies to all 4
sibling forms at once (decision 7's minimal scope — nothing more). **In-game test.**

**Checkpoint 6 — Retirement + cleanup.** Retire the 224 bundled static letter blocks (extend the
existing `ArabicLetterRetirement` mechanism — don't build a new one). Retire the 80 old static number
blocks (ONLY now that checkpoint 3's auto-join numbers are confirmed working — decision 3's
sequencing rule). Delete the now-dead old classes (`ArabicLetterBlock`, `ArabicLetterBlockEntity`,
`ArabicLetterBlockEntityRenderer`, `ArabicLetterRegistry`) once nothing references them. **In-game
test — full regression sweep:** every test in `docs/testing/GROUP_13_TESTING_GUIDE.md` §B/E/F/G/H/I/
J/K/L that currently passes must still pass, unchanged, PLUS Deleter/Triangle/Square now work on
letters and numbers with zero special-casing.

## 10. Do NOT

- Redesign the `/cb arabic` give/browse/command surface, or any Arabic `/cb` command's shape —
  explicitly deferred to a future, separate conversation (decision 7). Keep it functionally
  equivalent to today from the player's perspective.
- Touch Text Blocks (G13-21/22, "Arabic word") — a completely separate baked-phrase system.
- Build a migration/placement sweeper — not needed (decision 9).
- Retire the 80 static number blocks before their auto-join replacement is built AND confirmed
  in-game (decision 3 / checkpoint 6's sequencing).
- Skip an in-game confirmation gate because a later checkpoint "obviously needs it anyway."
- Write one large class for all of this — split by responsibility from the start (bootstrap, join
  swap logic, the new block class, sibling-creation service are all naturally separate files).

## 11. Build & verify

```
JAVA_HOME = C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
cd CustomBlocks-B
./gradlew.bat build --no-daemon
```
Green build = compiles + the three gates pass (mojibake, sound, file size). That's it — never tell
me something is done until I've tested it in-game and said so myself.
