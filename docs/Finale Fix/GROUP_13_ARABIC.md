# Group 13 — Arabic System

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 12 (Arabic System) build-verified (with known regressions documented).
>
> **Objective:** Fix all Arabic system regressions: redesign the word command to use an anvil GUI (bypassing Brigadier's ASCII limitation), bundle `arabtype.ttf` in the JAR, bundle `Rockwell Condensed` for English text, and rebuild auto-joining from scratch for horizontal-only letter connections.
>
> **Source issues:** Group O (O1: word command redesign, O2: English font, O3: auto-joining rebuild, O4: arabtype.ttf bundling), Issues 17.18, 17.19, 17.20, 17.21
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / fixes

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| `/cb arabic word` | Took Unicode Arabic text as arg — broken in Minecraft chat | `/cb arabic word <unicodeText> <id> <name>` — Brigadier rejects non-ASCII at position 16 | Redesigned: anvil GUI for text input, choice between single texture block or macro to place letter blocks |
| `arabtype.ttf` | Bundled in JAR | Placed manually at `run/config/customblocks/arabtype.ttf` (system fallback renders wrong shapes) | Bundled in JAR — auto-extracted to `config/customblocks/arabtype.ttf` on first boot |
| Auto-joining | Detected east/west neighbors, swapped to correct contextual form | Not built | Rebuilt from scratch: strict horizontal right-to-left only, no diagonals or T-junctions |
| English font | System SansSerif | System SansSerif | Bundled Rockwell Condensed — used for all non-Arabic text blocks |
| Arabic browser | `ArabicBrowserScreen` (54 slots) | Screen-based | Chest GUI version |
| Letter import | `/cb arabic import` (28 letters) | Working | Preserved |
| Single letter | `/cb arabic letter <name>` | Working | Preserved |
| Arabic text command | `/cb arabic text <color> <text>` | Same Brigadier ASCII issue | Also fixed via anvil GUI |

---

## What this group covers

| Feature | Commands |
|---|---|
| Import all letters | `/cb arabic import` |
| Single letter import | `/cb arabic letter <name>` |
| Word block (anvil flow) | `/cb arabic word <id> <name>` → opens anvil GUI for text |
| Text blocks (anvil flow) | `/cb arabic text <color>` → opens anvil GUI for text |
| Arabic browser | `/cb gui arabic` |
| Letter list | `/cb arabic list` |
| Auto-joining | Automatic on place/break — letters auto-shape isolated/initial/medial/final (O3) |
| Join direction | Omni-Tool → Arabic Direction mode: right-click start, left-click target (per face) (O5) |

---

## Implementation Requirements

### O1 — Word Command Redesign (Issue 17.18)

`/cb arabic word <id> <name>` — does NOT take the Arabic text as a command argument.

Flow:
1. Player runs `/cb arabic word myword MyWord`.
2. An anvil GUI opens with placeholder text "Type or paste Arabic text here".
3. Player types or pastes the Arabic text into the anvil.
4. Player clicks the output slot (confirm).
5. Two choice buttons appear in a chest GUI:
   - "Single texture block" — renders the full word as one block texture.
   - "Place letter blocks" — acts as a macro placing physical letter blocks in a line.

`/cb arabic text <color>` follows the same anvil flow for entering multi-character text.

### O2 — English Font: Rockwell Condensed (Issue 17.21)

**Bundled in JAR** at `assets/customblocks/fonts/RockwellCondensed.ttf`.
- Auto-extracted to `config/customblocks/fonts/RockwellCondensed.ttf` on first boot.
- Used for: all non-Arabic text block rendering.
- **Never** used for Arabic text (which uses `arabtype.ttf`).

### O3 — Auto-Joining Rebuild (Issue 17.20)

> **🔒 LOOK LOCKED 2026-06-18 (developer-approved on real previews).** The auto-join look is the
> **per-letter tile** model: each block draws its OWN contextual form (isolated/initial/medial/final)
> and the connecting bar reaches the tile edges so neighbours meet seamlessly across blocks — proven
> on the dev's own names (علي عبدالله محمد خالد مصطفى لؤي) via `tools/render_preview/InGameMock2.java`
> → `out/INGAME2_*.png`.
> - **Isolated form = the bundled hand-art PNG, used exactly** (no font for isolated). This is hamza,
>   any standalone letter, and any letter stranded by a non-connector (e.g. the lone ا in عبدالله, the
>   ي in لؤي). These are the big/bold curated glyphs the dev wanted.
> - **Connected forms (initial/medial/final) = engine-drawn from arabtype**, but the stroke is **bumped
>   to match the bundled art weight** (`RING ≈ 18/256` black, `WHITE ≈ 7/256` white — the
>   `generate_arabic_letters.py` recipe) so a font letter never looks thinner/lighter than the bundled
>   one beside it. Verified by eye in the Abdullah preview (bundled ا flush against font عبد / لله).
> - **SLICE-the-whole-word approach is REJECTED** (superseded). The per-letter FORM tile (ADR-003) is
>   the locked path. Do not revive slicing.
> - **Texture path = SOLVED.** The make-or-break "no resource-pack reload on texture change" is **PASSED
>   in-game 2026-06-18** (see §P in the testing guide / PROGRESS_LOG) — the live per-block texture path
>   the auto-join builds on is proven. This was the only true blocker; the build can proceed.
> - **Char→art-name mappings — VERIFIED 2026-06-18** against `ArabicLetterMap` + the `ArabicMaker`
>   reconcile: **ح→`ha`, ه→`ha2`, ط→`ta2`** (the earlier note had ح/ه swapped). Reconcile table
>   (`ArabicMaker.java:248-251`): dhal→thal, tah→ta2, dhah→tha2, nun→noon.

**Stuck-isolated letters use the FONT path (corrected 2026-06-19).** A non-connector that is isolated but
**attached** (has a letter neighbour) is drawn through `ArabicTileRenderer` — the **same** engine-drawn path
as the connected forms — so its **stroke, size and crispness match its neighbours exactly** (waw و beside ra
ر reads as one word, same weight). A **lone** isolated letter (no neighbour), the **hotbar/held icon**, and
**fixed-form decoration** (`lockedForm ≥ 0`) keep the **bundled hand-art at full size** — the showpiece, with
nothing to match. *(The earlier "scale the hand-art down" attempt was REJECTED in-game 2026-06-19: a scaled
raster can't match the font neighbours' stroke/size/style — see PROGRESS_LOG.)*

**Reading model (developer-confirmed 2026-06-16).** A word is a row of letter blocks that **stand up
like a sign** and read from the **front, right-to-left** — the first letter you place is the right
end, each next letter goes to its left. Each block auto-shapes itself from its neighbours; the default
single readable front matches the approved design (back-face mirroring is a possible later toggle, not
in this build).

Contextual form per block (0 isolated · 1 initial · 2 medial · 3 final). Stored on the letter
**BlockEntity** and drawn via the live-texture renderer — see **ADR-005** (this supersedes the old
ADR-003 `FORM` blockstate + pack-variant mechanism; ADR-003's joining *rules* still apply):
- **Isolated**: no joining neighbour either side.
- **Initial**: joins on its left (a letter follows it) only → rightmost letter of a run.
- **Final**: joins on its right (a letter precedes it) only → leftmost letter of a run.
- **Medial**: joins on both sides.

Rules:
- On place **and** on break, scan the two in-axis neighbours and re-flow the ≤2 affected blocks
  (bounded updater, sibling of `SlotLighting`). No world-wide scan.
- Joining follows the **full real-Arabic joining table** — every letter and extra form wired to its
  true joining type. The **6 non-connectors** (ا د ذ ر ز و) plus their cousins (آ أ إ, ة, ى, ء) join
  on their **right only**: they take final/isolated, never initial/medial, and the block placed after
  them (to their left) **starts a fresh word**.
- **Numbers never join** (Eastern ٠–٩ + Western 0–9 stay isolated).
- **Only letters join.** A gap or any non-letter block ends the word.
- Connected shapes (initial/medial/final) are **engine-drawn** from `arabtype` (ZWJ), restyled to the
  hand-art (white glyph + black stroke + colour bg), with a `config/customblocks/arabic/` **drop-in
  override** per form. Isolated forms keep using the bundled hand-art.
- **Colour-join** is switchable in `/cb config` (command + GUI). **Default: letters join regardless of
  colour** (each block keeps its own background).
- Direction defaults to horizontal; it is also set **interactively per face** by the OmniTool Arabic
  Direction mode (O5).
- **No diagonals, no T-junction logic.** The لا lam-alef ligature is out of scope for now.

**Direction + faces (developer-confirmed 2026-06-16):**
- **Each letter block remembers its OWN direction + face**, set at placement (or by the O5 tool). So two
  words pointing different ways coexist and re-flow correctly. The join updater reads the block's stored
  axis, not a single global setting. (This adds a `FACING` to the block alongside `FORM` — ADR-003 to be
  amended for it when Pass 4 is built; back-mirror below.)
- **Readable back (option B) — DEFERRED to a post-Pass-4 toggle (2026-06-18), NOT in this build.**
  Planned design: both sides read the word — the back face shows the front glyph **flipped in the model
  via UV** (no extra texture), glyph on front + back, other faces plain background colour (clean sign
  edges). This build ships the **single readable front only** (matches the reading-model note above).
  Revisit once 4a–4e are confirmed in-game.
- **Default when placing without the tool:** the block faces the player, horizontal axis. The O5 tool
  overrides with a custom direction/face (and can go vertical if the two clicks are stacked).
- The **single-texture word block** (the "Single block" maker choice) gets the same readable back.

### O5 — OmniTool "Arabic Direction" mode (Issue 17.20, added 2026-06-16)

A new mode on the existing Omni-Tool (Group 06) sets the writing direction by pointing at blocks
instead of editing config:
- **Right-click** the start block → anchor (and the face you clicked).
- **Left-click** another block → captures the direction from anchor → target, **per face**.
  (Left-click is intercepted while this mode is active so it captures instead of breaking the block.)
- Action does **both**: re-flow the letters already sitting on that line into a joined word, **and**
  remember the direction + face so letter blocks placed next keep joining along it.
- Switched like the other modes (sneak + right-click → Omni-Tool GUI).

### O4 — arabtype.ttf Bundling (Issue 17.19)

**Bundled in JAR** at `assets/customblocks/fonts/arabtype.ttf`.
- Auto-extracted to `config/customblocks/arabtype.ttf` on first boot.
- `ArabicWordRenderer` loads from this path (no more system font fallback).
- If the file is somehow missing: log a clear error and disable Arabic word rendering gracefully.

### Arabic Browser — Chest GUI

`/cb gui arabic` opens a chest GUI browser:
- 54 slots showing all 28 letters + extra forms (initial/medial/final/isolated where applicable).
- Color tabs for filtering (by letter group).
- Click a slot → give that letter block.

---

## Setup

```
/cb arabic import
```

Wait for all 28 letter blocks to import. Then:
```
/cb arabic list
```
Verify 28 letters listed.

---

## Test G13.1 — arabtype.ttf is bundled (not manually placed)

Delete `config/customblocks/arabtype.ttf` if it exists. Restart server.

**Expected:** Server log shows `[CustomBlocks] Extracted arabtype.ttf to config/customblocks/arabtype.ttf.` File present after boot without manual placement.

**Pass:** Font auto-extracted on boot.
**Fail:** Font missing, or server falls back to SansSerif with warning.

---

## Test G13.2 — Arabic letter rendering uses correct font

```
/cb arabic letter alef
```

**Expected:** "Alef" letter block has proper Arabic glyph rendering (correct Arabic letterform, not a box or fallback glyph).

**Pass:** Letter renders as proper Arabic glyph.
**Fail:** Box glyph, question mark, or fallback font visible.

---

## Test G13.3 — `/cb arabic word` opens anvil GUI

```
/cb arabic word testword TestWord
```

**Expected:** An anvil GUI opens with "Type or paste Arabic text here" placeholder. No error about non-ASCII.

**Pass:** Anvil GUI opens.
**Fail:** Brigadier error "incorrect argument at position 16", or no GUI opens.

---

## Test G13.4 — Word → single texture block

In the anvil GUI from G13.3, type or paste a short Arabic word (e.g., "مرحبا"). Click the output slot.

A chest GUI appears with "Single texture block" and "Place letter blocks" options. Click "Single texture block".

**Expected:** A new block "TestWord" is created with the full Arabic word rendered as a texture.

**Pass:** Block created with Arabic text as texture.
**Fail:** Error, no block created, or garbled texture.

---

## Test G13.5 — Auto-joining: isolated letter

Place a single Jeem (ج) letter block in the world with no neighbors.

**Expected:** Block renders with the Jeem **isolated** form.

**Pass:** Isolated form rendered.
**Fail:** Wrong contextual form.

---

## Test G13.6 — Auto-joining: initial + final forms

Place a Ba (ب) letter block directly to the **west (left)** of the Jeem — the word grows leftward,
right-to-left.

**Expected:**
- Jeem (right end) re-evaluates: renders as **initial** form (a letter now follows it on its left).
- Ba (left end) renders as **final** form (a letter precedes it on its right).

**Pass:** Both letters show correct contextual forms.
**Fail:** Forms don't update, or wrong forms shown.

---

## Test G13.7 — Auto-joining: medial form

Place a second Ba (ب) directly to the **west (left)** of the Ba from G13.6. The sequence right-to-left
is now: Jeem — Ba — Ba.

**Expected:**
- Jeem (right end) stays **initial**.
- Ba (center) re-evaluates: renders as **medial** form (joins on both sides).
- Ba (left end) renders as **final**.

**Pass:** All three contextual forms correct.
**Fail:** Center letter stays initial/final instead of medial.

---

## Test G13.8 — Auto-joining: no diagonal connection

Place a letter block diagonally adjacent (e.g., one block northeast) from an existing letter sequence.

**Expected:** The diagonal block does NOT connect to the sequence. It renders as isolated.

**Pass:** Diagonal placement does not trigger joining.
**Fail:** Diagonal block joins the sequence.

---

## Test G13.9 — Rockwell Condensed for English text

```
/cb arabic text #FFFFFF HelloWorld
```

In the anvil GUI, type "Hello". Confirm. Choose "Single texture block".

**Expected:** Block created with "Hello" rendered in Rockwell Condensed (slab-serif, blocky, Minecraft-appropriate aesthetic).

**Pass:** Rockwell Condensed font visible on block texture.
**Fail:** Generic SansSerif or system font used.

---

## Test G13.10 — Arabic browser chest GUI

```
/cb gui arabic
```

**Expected:** Chest GUI opens with 28+ letter slots. Color tabs visible. Click a letter slot → block given.

**Pass:** Chest GUI opens, all letters visible, click-to-give works.
**Fail:** Screen-based UI opens, or GUI empty.

---

## Status & testing

This doc is the **spec**. Step-by-step tests and live pass/fail status live in
`Reports/GROUP_13_TESTING_GUIDE.md` — not here.

Group 13 is complete when all Arabic rendering, the word/colour maker, the browser, font bundling
and (later) auto-joining work in-game.

---

## Cleanup

```
/cb arabic list
```
(Delete individual letter blocks if needed — they have auto-generated IDs like `arabic_alef`, etc.)

---

## Issues reported (rounds 1–2) — what each requires

> Requirements behind each reported issue. Pass/fail status for every one lives in
> `Reports/GROUP_13_TESTING_GUIDE.md` (Tests 1–10).

### Issue 1 — Arabic block rendering
- **1a — Background:** a created word block's background matches the bundled art's true black
  (`#0A0A0A`), not pure `#000000`.
- **1b — Font/style:** generated words match the bundled letters — arabtype glyph with a thick black
  outline, cursively joined.
- **1c — HUD:** looking at a freshly made word block shows the look-at HUD, like every other block.

### Issue 2 — Command merges & overhauls
- **2a — Browser:** `/cb arabic list` opens a clearly marked browser of Arabic letters, Arabic numbers
  and English numbers (English letters coming later): a group picker leading to an aligned grid per
  group, with a black/red/green/yellow colour rail.
- **2b — One command:** there is no `/cb arabic text`; `/cb arabic word <id> <name>` is the single entry
  (text typed in an anvil, colours picked in the GUI).
- **2c — Colour workflow:** after the text, the player customizes the background colour and the letter
  colour before the block is made. A **Render preview** button opens a **live preview screen** showing
  the real rendered word in the chosen colours — a **3D, rotatable** block that looks the part — with
  **no resource-pack reload, no prompt, and no block placed in the world**. **Back** returns to the
  colour menu with the colours still set; **Create** makes the real block.
  - **How it works (new approach, 2026-06-16):** built on the Group 10 live-recolour rail. The preview
    texture is rendered to a throwaway slot's `TextureStore` and served straight over the mod's
    `/tex/<id>` HTTP endpoint — **pack-free** (no `updatePack`, no `ResourcePackSend`). The screen is a
    client `Screen` opened via `OpenGuiPayload`; colour changes round-trip to the server to re-render;
    **Back/close** send `GuiBackPayload` so the server reopens the Color Studio. Live status →
    `Reports/GROUP_13_TESTING_GUIDE.md` §1.

-----

- **2c (SCRAPPED 2026-06-16) — resource-pack force-send + auto-placed world block.** The first attempt
  force-sent the pack refresh to the one player past the GUI hold (`ResourcePackServer.hasBlockingGui`
  → modded local-regen) and **auto-placed a real preview block ~2 ahead**. **Rejected in-game:** the
  texture only appeared after a **resource-pack prompt fired**, it spawned a **whole physical block** in
  the world, and viewing it meant **losing the colour menu** you launched it from. Replaced by the
  pack-free live preview screen above.
- **2d — Defaults:** those colour defaults are editable in the `/cb arabic` Default Colours menu (a
  clean, aligned palette) and pre-fill the maker.

### Issue 3 — No chat prompts from GUIs
A GUI button must never send the player to chat to finish an action — it is handled in-GUI (an anvil for
free text/URLs, a Yes/No confirm GUI for confirmations). Done for the Arabic maker. Remaining GUI→chat
handoffs to convert across the mod:

| # | File | Tile | In-GUI replacement |
|--|--|--|--|
| 1 | `EditorMenu` | Rename block | AnvilPrompt → `/cb rename` |
| 2 | `EditorMenu` | Retexture (URL) | AnvilPrompt (paste URL) |
| 3 | `EditorMenu` | Note | AnvilPrompt |
| 4 | `EditorMenu` | Delete | `ConfirmMenu` (Yes/No) |
| 5 | `SearchMenu` | Search | AnvilPrompt (type query) |
| 6 | `FaceEditorMenu` | Paint face (URL) | AnvilPrompt |
| 7 | `GradientPickerMenu` | Pick A / Pick B | block-picker GUI or AnvilPrompt |
| 8 | `CategoryEditMenu` | Bulk retexture (URL) | AnvilPrompt |
| 9 | `BrokenBlocksMenu` | broken-block action | AnvilPrompt / ConfirmMenu |

Reuse: `AnvilPrompt` (free text/URL), `ConfirmMenu` (Yes/No GUI).

### Issue 4 — Bulk actions bug (lives in Group 07)
Not an Arabic issue. Pre-picked blocks must skip the bulk Step-1 chooser and go to a Yes/No confirm.
Spec + tests live in Group 07 (`GROUP_07_BULK_OPERATIONS.md`, `Reports/GROUP_07_TESTING_GUIDE.md` §A4).

---

## Related & deferred
- **FIX A + FIX B** (clean display names, case-insensitive `/cb give`) → moved to **Group 26**
  (`GROUP_26_NAME_AND_GIVE_FIXES.md`), so Group 13 stays Arabic-only.
- **`/cb search` edits** — wanted later; scope to be defined; not part of Group 13.


---

## Auto-Join Implementation — colour + form + facing (2026-06-19)

**Goal (dev spec):** the existing isolated hand-art letters (ba_red, jeem_green, …) are the blocks
that auto-join; medial/initial/final stay intact and searchable in creative but **do not count
toward slots**; every letter sits centred in its own block; connected hands match the approved
ALL_LETTERS look.

**Data model (all on the one `customblocks:arabic_letter` block + its BlockEntity):**
- `ArabicLetter` (int codepoint) — unchanged.
- `ArabicColor` (string: black/red/green/yellow) — NEW. Drives the tile background and which
  hand-art PNG folder is used.
- `ArabicForm` (int 0..3) — NEW, optional. Present = a **fixed-form** searchable decoration variant
  (lockedForm); absent or -1 = an **auto-join** block whose form follows its neighbours.

**Rendering (ArabicLetterBlockEntityRenderer):** draws effectiveForm (= lockedForm if set, else the
neighbour-computed form) in the block colour.
- ISOLATED + has bundled art -> the hand-art PNG used **exactly** (arabic_art/COLOUR/BASE_COLOUR.png).
- any connected form (or a letter with no bundled art) -> ArabicTileRenderer white glyph on the
  colour background. Backgrounds are sampled from the real art PNGs so the seam is invisible.
- Texture cache key is now letter_form_colour.

**Facing auto-inherit (ArabicLetterBlock.getPlacementState):** (1) explicit OmniTool direction
wins; else (2) inherit a touching **auto-join** letter facing, preferring a neighbour whose word
axis we sit on; else (3) the furnace convention. This is the fix that makes rows reliably join.

**Join flow (ArabicJoinFlow):** fixed-form (locked) blocks are skipped by recompute and ignored as
join neighbours, so decoration variants stay independent.

**Searchable set (CustomBlocksMod.registerArabicJoinTab):** a new "Arabic Letters (Join)" creative
tab lists every bundled letter, in isolated(auto-join)/initial/medial/final, in black/red/green/
yellow. Every entry is the **same** registered block with different custom-data, so this adds **zero**
registrations and costs **zero** slots. Stacks carry a searchable custom name like "jeem . medial . green".

**Build note:** implemented as source edits only; compile + in-game test happen on a JDK machine
(no compiler in the handoff sandbox).

---

## O6 — Naming, virtual IDs & live config labels (2026-06-19, design locked — see ADR-006)

**Brightness fix (prerequisite, coded — not yet confirmed in-game).** The join block's glyph is an
overlay quad drawn by `ArabicLetterBlockEntityRenderer`, lit with the BlockEntity `light` — which the
game samples at the block's **own** (solid, occluded) position → dark → the white glyph rendered grey.
A bundled letter is a normal block lit per-face from the air in front → bright. Fix: sample light at
`pos.offset(facing)` (the air the face points into), used for all four glyph verts. Proof it is
lighting and not the texture: `jeem_black.png` measured = pure white (255,255,255) glyph on (10,10,10);
the same PNG multiplied ×0.45 reproduces the in-game grey (`tools/render_preview/out/DIM_PROOF.png`).

**Naming scheme — isolated is the default, NO form word.**

| Form | Display | Virtual id |
|---|---|---|
| isolated | `Jeem Black` | `Jeem_Black` |
| initial | `Jeem Black Ini` | `Jeem_Black_Ini` |
| medial | `Jeem Black Mid` | `Jeem_Black_Mid` |
| final | `Jeem Black Fin` | `Jeem_Black_Fin` |

- Order = **Letter _ Colour _ Form**; Title-Case letter, digits kept (`Ta2`, `Ha2`); display = clean
  spaces, id = underscores.
- Isolated carries no suffix → a bundled `Jeem Black` and a lone join block read identically (the 224
  confirmed bundled names do **not** change). An auto-join placed block shows its **live** form name.
- Numbers never join → no forms → simple names (`A0 Black`, `E5 Black`).

**Virtual id (Way A).** One registered block + NBT, as today; a helper computes id ⇄ (letter, colour,
form). Zero new registrations, zero slots, no migration. Give / search / list resolve by virtual id.

**Live config labels.** The 3 connected-form words are `/cb config` values (GUI **and** command),
default `Ini` / `Mid` / `Fin` (isolated has none). Names are **computed at display time** (item
`getName` + HUD), never baked → changing one label re-labels every held / placed / stored block
instantly, **no resource-pack reload** (text ≠ texture).

**HUD.** Join blocks wired into the CB HUD so a placed block shows its live name, like bundled letters.

## O7 — Bundled letters → editable config (2026-06-19, design locked — see ADR-006)

The 224 bundled hand-art letters stop being jar-only: **mirrored into config** so the dev can edit /
delete them. Bundled isolated letters keep their current clean names (`Jeem Black`, `A0 Black`) — no
`Iso` suffix. **Delete is recoverable**: the original 224 stay in the jar as a fallback master copy and
a `/cb` restore command brings deleted defaults back.

## Build order (locked — everything, in order, each confirmed in-game before the next)
1. **Brightness fix** (already coded) → build → in-game confirm.
2. **Naming + virtual id + live config form-labels (Ini/Mid/Fin) + HUD on placed blocks.**
3. **Bundled 224 → config mirror** (editable, deletable, recoverable restore).

Per the Golden Rule: preview where visual, build, hand back for in-game confirm. Nothing is DONE until
the dev confirms in-game.

## Isolated floater grounding (shipped 2026-06-19 — see TESTING GUIDE §12)

Isolated auto-join letters are font-drawn at the shared baseline; short ones floated too high. A
two-tier downward nudge runs in `ArabicTileRenderer.render()` on the ISOLATED form only:
- A glyph with a real tail/descender (waw/ra/zay/noon/meem/ya/jeem/ha/lam/ain/maqsura) is left alone.
- A **short** no-descender floater (dal/dhal/ta-marbuta/round-ha/hamza) drops `ISO_DROP = 9%` of tile height.
- A **tall** no-descender letter (alef family) drops a tiny `ISO_TALL_DROP = 3%`.
Detector is shape-driven (descent below baseline ≈ 0 → floater; height < 62% → short). Connected forms
never enter this branch, so kashida bars stay on the baseline. This also **resolves §11** (stuck-isolated
size) — no separate hand-art shrink is needed.

## O8 — Hide / manage the bundled letters (design — brainstormed 2026-06-19, NOT built)

Refines O7's "manage the 224": the dev wants **Hide, not hard delete** — same decluttering, zero fear.
- **Per-block Hide** — a Hide button on each letter in the Arabic browser (click → hidden).
- **Hide all on page** — one action hides every letter in the **current search/filter** view.
- **Master Hide all** — empties the whole visible list at once.
- **Hidden tab** — hidden letters move to a separate Hidden list the dev can reopen anytime.
- **Un-hide** — both **one-by-one** (click in the Hidden tab) and an **un-hide all** button.
- **Recoverable always** — hide only changes visibility; the block + texture are never destroyed; the jar
  masters stay as the ultimate fallback. (Hard delete + custom-colour recolor are **deferred** — Hide
  covers the real need; the per-colour-variant delete and any-custom-colour recolor from O7 wait.)
- Scope unit: a single colour variant (e.g. `Jeem Red`) hides independently of the other colours.

## O9 — Type-a-word, auto-build (design LOCKED 2026-06-19, NOT built)

Type a word, the connected letter blocks place themselves — no placing letter by letter. Reuses the O3
auto-join blocks (`ArabicLetterBlock` + `ArabicJoinFlow`), so each placed block auto-shapes from its
neighbours exactly as a hand-placed row does.

**Trigger (both).** A GUI text box (open a screen, type the word, Build) AND a command
`/cb arabic build <word>`. The GUI text entry reuses the existing anvil prompt; the colour pick reuses the
Color Studio path.

**Placement.** The row builds **on the ground in front of the player**, starting at the block they are
looking at — or ~2 blocks ahead if they are aiming at nothing/sky. It runs **right-to-left** in the
correct auto-join forms (the join brain + facing do the shaping).

**Colour (both modes).** Default = **one colour for the whole word**, picked before building. Optional =
**per-letter colours**. Both offered via the Color Studio path (the 4 set colours + custom/brand). White
script on each block's own background, so a mixed-colour word still reads across the seams.

**Spaces.** A space in the typed text = **an air gap** (one empty block), which ends the join run; the
next word starts fresh on the other side of the gap.

**Numbers.** Digits are placed **inline as non-joining number blocks** (letters still connect around them).
When a word contains digits the player is **asked each time** which style to use — Eastern Arabic-Indic
(`A0–A9`, ٣) or Western (`E0–E9`, 3).

**Collision.** If the target row runs into an existing block, the build **stops before the occupied cell**
and reports how many letters were placed. It **never overwrites** existing blocks.

**Undo.** A whole auto-built word is reversible as **one step**. Undo behaves like the rest of the mod's
undo (every build recorded, a capped stack, undo back through recent builds), with two differences forced
by what it touches: it operates on **placed world blocks** (not the 1028 `SlotData` catalog the core
`UndoManager` handles), and it **persists across relog/restart** (saved to disk). It therefore uses its
**own small saved word-build store**, separate from `core/UndoManager`. Undo removes only the built blocks
**still in place and unchanged** — blocks the player has since broken or replaced by hand are left alone.

**Constraints.** Zero-registration / zero-slot (reuses the single `arabic_letter` block + NBT, like O3) —
never touches the 1028 `SlotBlock`s. A sane max word length / placement cap so a build can never hang.

---

## Round 3 — reported issues + fixes (2026-06-19)

Six issues the dev reported on real in-game screenshots. Decisions locked with the dev this session.

### R3.1 — Stray edge lines on placed letter blocks
Thin lines run along the block edges/seams. *Cause:* the BER paints all six glyph quads at `z = 1.002`
(0.002 **outside** the cube) with no-cull, so every face — including faces buried against a neighbour —
pokes 0.002 past the block boundary; the buried/side quad edges show as lines.
*Fix direction:* stop the overhang — draw at the cube surface and/or cull faces flush against a neighbour
letter block. **Top/bottom/side faces keep their current look** (dev: "only fixing the back"); this is a
geometry fix, not a face-content change.

### R3.2 — `/cb arabic join <letter>` gave 16
Default count was hard-coded `16`. **Fixed → 1** (`ArabicCommands.giveJoin` default).

### R3.3 — Middle-click (pick block) returned a blank item
`ArabicLetterBlock` had no `getPickStack`, so vanilla returned a letter-less `arabic_letter` item (blank
icon + raw `block.customblocks.arabic_letter` key). **Fixed:** `getPickStack` now reads the BlockEntity and
returns a properly stamped stack (letter + colour + form) that names + renders right and re-joins on placement.

### R3.4 — "Place letter blocks" gave the wrong (non-joinable) letters
`ArabicMaker.giveLetterBlocks` handed out the old **static bundled** art blocks (`arabic_<base>_black`).
**Fixed:** it now gives the **joinable** `ArabicLetterBlock` (the "Join" blocks), one per Arabic letter,
and the menu lore is updated (no more "once enabled" — auto-join is live).

### R3.5 — OmniTool "Arabic Direction" mode (O5) — REMOVED
Decision: **remove it.** Placement now auto-inherits facing from adjacent join letters
(`getPlacementState`), so rows join on their own — the manual direction tool is redundant, gives only
chat feedback (feels dead), and overlaps the planned O9 type-a-word build. Removal drops: the `ARABIC`
OmniTool mode, its menu button, the `AttackBlockCallback` left-click intercept, the `preferredFacing`
priority in `getPlacementState`, and the `ArabicDirectionTool` class. (Supersedes O5 above and its §5
testing entry. The `ArabicJoinFlow` re-flow brain is unrelated and stays.)

### R3.6 — Readable back of a placed word — **C-full** (true two-faced sign)
Today every face shows the same tile, so the back reads **flipped + reversed** (mirrored garble). Dev
wants the back to read the **same word, correctly, from behind** (walk around → reads normally both sides).

**The driving fact:** from behind, left/right swap, so a block's back must show its **mirror-partner**
letter, not its own — block at row index `k` shows letter `N-1-k` on its back.

**Studied result that makes it cheap to compute:** a block's **back form equals its partner's front form**,
so a single run-walk that computes every block's front `(letter, form)` array *also* gives the back of every
block for free: `back(k) = front(N-1-k)`.

**Mechanism (locked):**
- On place/break, walk the whole contiguous run once (capped, e.g. ≤64, to keep it from ever hitching),
  build the ordered `(letter, form)` list, and give each block its **front** (own letter+form, as today)
  **and** its **back** (partner letter+form).
- `ArabicLetterBlockEntity` gains `backLetter` + `backForm` (synced like `form`).
- Renderer: **front** face = own tile (unchanged); **back** face = the partner's tile, U-flipped so it
  reads correctly (not mirrored) for a viewer behind. Both tiles already live in the texture cache (keyed
  letter+form+colour) → no new textures.
- **Decisions:** scope = **placed letter rows only** (the single-texture word block is untouched for now);
  **top/bottom/4 sides unchanged** — only the back face changes; a **mixed-colour** word keeps **each
  block's own bg colour** on its back (partner *letter*, this block's colour) — identical for the normal
  single-colour word, no extra data.
- **Edge cases:** break mid-word → two runs, both re-pair · add a letter → whole run re-pairs · lone letter
  → back = same letter, correct · gap/space/number ends the run (pairing resets there, same as joining) ·
  horizontal rows only (vertical went away with the OmniTool).
- **Trade-off (accepted):** place/break re-checks the **whole word**, not just ±2 neighbours (relaxes the
  bounded updater); capped run length keeps it instant and reset-safe.
