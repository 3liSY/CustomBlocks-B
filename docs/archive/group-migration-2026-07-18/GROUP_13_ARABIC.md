# Group 13 — Arabic System

## UI medium audit (2026-07-09)

ArabicHubMenu, ArabicListMenu, ArabicGroupMenu, WordChoiceMenu, ColorStudioMenu → **all fold into one
Arabic Screen, not built.** Owner confirmed the two ALREADY-BUILT screens (`ArabicPreviewScreen`,
`ArabicBrowserScreen`) merge into this same Screen too — one unified Arabic Screen total, not two extra
standalone ones sitting next to it. Owner also wants a professionally-labeled button inside this Screen
that launches Colour Variants (`/cb colorvariants`) — not shown as a raw command, styled like a real
feature button.

## 🗺️ Remaining-work roadmap (locked 2026-07-02, REVISED 2026-07-02) — 3 phases

> Owner's plan: hand each phase to a fresh session/prompt, in-game confirm before the next starts
> (CLAUDE.md §4 — no batching). This is the order. Don't reorder without asking the owner first.

> **Correction (same day):** an original draft had a Phase 2 ("placement + recolour smoothness on
> servers", §G13-11 item 4 + §G13-12 fix) that turned out to already be built and confirmed in-game
> 2026-06-27 — removed, phases renumbered 1-4.
>
> **Second, bigger correction (same day, later):** what was Phase 1 ("letters get a settings-sheet
> layer, NOT real slot blocks") was built (steps 1-2) then reverted — the owner clarified the real
> ask: letters become REAL slot blocks, old system removed, numbers folded into the same rebuild. See
> §G13-25 (fully rewritten) for the whole story. This is now ONE big rebuild handed to a more capable
> execution agent as a single exhaustive spec, not a small phased prompt like the others — so the old
> "Phase 1" and "Phase 2" rows below are MERGED into one Phase 1. Phase 3 (hide/auto-build) is
> corrected: its Hide/manage half is cancelled outright (the 224 static blocks it was about no longer
> exist once §G13-25 ships). Phase 4 (Text Blocks) is unaffected — separate system.

| # | Phase | Covers | Design location |
|---|---|---|---|
| 1 | Letters AND numbers become real SlotBlocks, old live-render system + the 224 bundled static blocks retired | full Deleter/Triangle/Square parity with zero special-casing, pre-baked base forms, full colour parity (presets + custom hex), auto-join via proven block-swap — one big rebuild, one exhaustive spec, not phased | §G13-25 (absorbs old Phase 1 + Phase 2 / §G13-20) |
| 2 | Type-a-word auto-build | type/paste a word and the connected row builds itself on the ground, using the real slot letters from Phase 1 (Hide/manage half of the old Phase 3 is CANCELLED — see §G13-25 decision 3) | §G13-10 |
| 3 | Text Blocks full upgrade + Studio tab | rebrand "Arabic word" → general re-editable "Text Blocks" (Arabic + Latin), full styling (outline/font/size/gradient/shadow/border, one at a time), multi-line + multi-block phrases, new Studio tab front-end | §G13-21 · §G13-22 |

Everything else in Group 13 not listed above is either already ✅ confirmed in-game (see
`docs/testing/GROUP_13_TESTING_GUIDE.md`) or is cross-group backlog that isn't Arabic-specific
(G13-15's remaining 9 GUI→chat conversions apply mod-wide, not just here).

---

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
| Arabic browser | `ArabicBrowserScreen` (54 slots) | Screen-based | Merges into the one unified Arabic Screen (see audit note above) |
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

### G13-3 · Word Command Redesign  (was O1 · 17.18)

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

### G13-2 · English Font: Rockwell Condensed  (was O2 · 17.21)

**Bundled in JAR** at `assets/customblocks/fonts/RockwellCondensed.ttf`.
- Auto-extracted to `config/customblocks/fonts/RockwellCondensed.ttf` on first boot.
- Used for: all non-Arabic text block rendering.
- **Never** used for Arabic text (which uses `arabtype.ttf`).

### G13-6 · Auto-Joining Rebuild  (was O3 · 17.20)

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

### O5 — OmniTool "Arabic Direction" mode (Issue 17.20, added 2026-06-16)  (⛔ no G13-n — removed, see R3.5)

A new mode on the existing Omni-Tool (Group 06) sets the writing direction by pointing at blocks
instead of editing config:
- **Right-click** the start block → anchor (and the face you clicked).
- **Left-click** another block → captures the direction from anchor → target, **per face**.
  (Left-click is intercepted while this mode is active so it captures instead of breaking the block.)
- Action does **both**: re-flow the letters already sitting on that line into a joined word, **and**
  remember the direction + face so letter blocks placed next keep joining along it.
- Switched like the other modes (sneak + right-click → Omni-Tool GUI).

### G13-1 · arabtype.ttf Bundling  (was O4 · 17.19)

**Bundled in JAR** at `assets/customblocks/fonts/arabtype.ttf`.
- Auto-extracted to `config/customblocks/arabtype.ttf` on first boot.
- `ArabicWordRenderer` loads from this path (no more system font fallback).
- If the file is somehow missing: log a clear error and disable Arabic word rendering gracefully.

### Arabic Browser — Screen (merges into the unified Arabic Screen)

`/cb gui arabic` opens the unified Arabic Screen (see audit note above; not built):
- 54 rows/tiles showing all 28 letters + extra forms (initial/medial/final/isolated where applicable).
- Color tabs for filtering (by letter group).
- Click a row → give that letter block.

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

## Test G13.9 — Rockwell Condensed for English text ❌ SCRAPPED (2026-06-21)

> **SCRAPPED:** `/cb arabic text` was **removed entirely** (Area 2b, confirmed 2026-06-16). This test no
> longer applies. English text-block rendering, if revisited, moves under the word maker — not `arabic text`.

~~`/cb arabic text #FFFFFF HelloWorld` → Rockwell Condensed on block texture.~~

---

## Test G13.10 — Arabic browser Screen ⏳ (blocked — unified Arabic Screen not built)

> ⚠️ **UPDATED 2026-07-10:** browser moved into the unified Arabic Screen (audit note above). The old
> chest GUI is now the FAIL, not the Pass — reverse of the original spec.

```
/cb gui arabic
```

**Expected:** Unified Arabic Screen opens with 28+ letter rows. Color tabs visible. Click a letter row → block given.

**Pass:** Screen opens, all letters visible, click-to-give works.
**Fail:** Old chest GUI opens, or Screen empty.

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

### G13-14 · Arabic block rendering  (was Issue 1)
- **1a — Background:** a created word block's background matches the bundled art's true black
  (`#0A0A0A`), not pure `#000000`.
- **1b — Font/style:** generated words match the bundled letters — arabtype glyph with a thick black
  outline, cursively joined.
- **1c — HUD:** looking at a freshly made word block shows the look-at HUD, like every other block.

### Issue 2 — Command merges & overhauls  (→ G13-3 / G13-4 / G13-5, see sub-items)
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

### G13-15 · No chat prompts from GUIs  (was Issue 3)
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

### Issue 4 — Bulk actions bug (lives in Group 07)  (→ G07, not G13)
Not an Arabic issue. Pre-picked blocks must skip the bulk Step-1 chooser and go to a Yes/No confirm.
Spec + tests live in Group 07 (`GROUP_07_BULK_OPERATIONS.md`; bulk-op behaviour → `GROUP_07_TESTING_GUIDE.md` §B, Hub screen rows → `GROUP_27_TESTING_GUIDE.md` §T).

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

## G13-7 · Naming, virtual IDs & live config labels (2026-06-19, design locked — see ADR-006)  (was O6)

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

## G13-8 · Bundled letters → editable config (2026-06-19, design locked — see ADR-006)  (was O7)

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

## G13-9 · Hide / manage the bundled letters (design — brainstormed 2026-06-19, NOT built)  (was O8)

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

## G13-10 · Type-a-word, auto-build (design LOCKED 2026-06-19, NOT built)  (was O9)

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

### G13-19 · Stray edge lines on placed letter blocks  (was R3.1 · G13-19)
Thin lines run along the block edges/seams. *Cause:* the BER paints all six glyph quads at `z = 1.002`
(0.002 **outside** the cube) with no-cull, so every face — including faces buried against a neighbour —
pokes 0.002 past the block boundary; the buried/side quad edges show as lines.
*Fix direction:* stop the overhang — draw at the cube surface and/or cull faces flush against a neighbour
letter block. **Top/bottom/side faces keep their current look** (dev: "only fixing the back"); this is a
geometry fix, not a face-content change.

### G13-16 · `/cb arabic join <letter>` gave 16  (was R3.2)
Default count was hard-coded `16`. **Fixed → 1** (`ArabicCommands.giveJoin` default).

### G13-17 · Middle-click (pick block) returned a blank item  (was R3.3)
`ArabicLetterBlock` had no `getPickStack`, so vanilla returned a letter-less `arabic_letter` item (blank
icon + raw `block.customblocks.arabic_letter` key). **Fixed:** `getPickStack` now reads the BlockEntity and
returns a properly stamped stack (letter + colour + form) that names + renders right and re-joins on placement.

### G13-18 · "Place letter blocks" gave the wrong (non-joinable) letters  (was R3.4)
`ArabicMaker.giveLetterBlocks` handed out the old **static bundled** art blocks (`arabic_<base>_black`).
**Fixed:** it now gives the **joinable** `ArabicLetterBlock` (the "Join" blocks), one per Arabic letter,
and the menu lore is updated (no more "once enabled" — auto-join is live).

### R3.5 — OmniTool "Arabic Direction" mode (O5) — REMOVED  (⛔ no G13-n)
Decision: **remove it.** Placement now auto-inherits facing from adjacent join letters
(`getPlacementState`), so rows join on their own — the manual direction tool is redundant, gives only
chat feedback (feels dead), and overlaps the planned O9 type-a-word build. Removal drops: the `ARABIC`
OmniTool mode, its menu button, the `AttackBlockCallback` left-click intercept, the `preferredFacing`
priority in `getPlacementState`, and the `ArabicDirectionTool` class. (Supersedes O5 above and its §5
testing entry. The `ArabicJoinFlow` re-flow brain is unrelated and stays.)

### G13-13 · Readable back of a placed word — **C-full** (true two-faced sign)  (was R3.6)
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

---

## G13-11 · Placement lag + transparent flash on a server (investigated 2026-06-20, NOT built)  (was O10)

Dev report (on a real server): placing auto-join letters **lags**, and the block shows a **transparent
flash** for a moment before the glyph appears; "a bit buggy." Deep investigation found **two independent
root causes**, both client-side render (unrelated to the server texture/resource-pack path).

### Cause 1 — transparent flash (block invisible until the letter reaches the client)
The block is `BlockRenderType.INVISIBLE` (`ArabicLetterBlock.getRenderType`) — it draws nothing itself; the
glyph is 100 % the BlockEntityRenderer, which returns early while the **client** BlockEntity still has
`letter == 0` (`ArabicLetterBlockEntityRenderer.render` line ~88). The client only learns the letter from a
BlockEntity sync packet, and two things delay/drop it:
- **Network round-trip.** Singleplayer is instant; on a server the letter only arrives one round-trip after
  placement → that gap is the visible transparent window.
- **Missing-sync bug.** `onPlaced` sets the letter with `markDirty()` only (marks for *save*, not client
  sync), then `ArabicJoinFlow` syncs **only blocks whose form changed** (`recomputeRun`, `if (changed)
  be.sync()`). A **lone / first** letter computes all-default values → `changed == false` → **`be.sync()`
  never fires** → the client stays at `letter == 0` (transparent) until a neighbour is placed (re-flow
  changes its form) or the chunk reloads. This is a real correctness gap, not just latency.

### Cause 2 — lag spike (heavy texture build on the render thread)
`textureFor()` builds each `(letter, form, colour)` tile **lazily and synchronously on the render thread**,
the first frame that combo is seen. With `textureSize = 256` and `SS = 4` the AWT work runs at **1024×1024**:
font outline + `Area` boolean ops (add/intersect) + double stroke fill+draw + bicubic downscale + a
**PNG encode → decode round-trip** (`ImageIO.write` then `NativeImage.read`) + GPU upload — all inside one
frame = a stall. Building a word re-flows the run, changing several letters' forms → several new cache keys
→ several builds back-to-back = compounding hitches. First letter ever also pays `ensureMetrics()` (28
letters × 4 forms of `TextLayout` outlines) on the render thread.
**Regression note:** the OLD project pre-warmed textures on daemon threads (`cb-color-prewarm`); the -B
rewrite went fully lazy-synchronous and dropped that.

### Perfect fix (design — to discuss before building)
1. **`onPlaced` → unconditional `be.sync()`** after stamping letter/colour/form. Fixes the missing-sync gap
   and shrinks the flash to one tick. *(core, smallest, safe)*
2. **Async texture build.** On a cache miss, build the `NativeImage` on a background thread and register it
   on the render thread via `MinecraftClient.execute()`; return null that frame (block blank ~1 frame, no
   stall). Drop the PNG encode/decode — write pixels straight into the `NativeImage`. *(core — kills the freeze)*
3. **Prewarm** the tiles of the letter items the player is carrying (hotbar + offhand), all 4 forms, on a
   daemon thread each client tick, porting the old mod's approach. ✅ **BUILT 2026-06-20 (build-green,
   awaiting in-game)** — `ArabicLetterBlockEntityRenderer.prewarm` + `client/render/ArabicPrewarm` tick.
4. **Optional zero-flash:** client stamps the BE letter from the held stack during predicted placement, so
   the glyph shows with no round-trip at all. *(advanced)*

Items 1 + 2 remove the reported symptoms; 3 + 4 are polish. **Not built — Golden Rule: nothing is DONE
until confirmed in-game.**

### In-game result on the server (owner, 2026-06-20) → item 4 is now REQUIRED, not optional

Parts 1–3 are in the jar (unconditional `be.sync()`, async off-thread build, hotbar/offhand prewarm).
**Owner re-tested on the dedicated server: the letter still flashes transparent for a split second on
placement, then appears.** That residual flash is the **network round-trip itself** — Parts 1–3 cannot
remove it, because on a server the client only learns the letter *after* the place packet round-trips back,
and the INVISIBLE block draws nothing in that gap. Singleplayer has no round-trip, so it never showed there.

**The fix is item 4 — client-side placement prediction** (the same technique normal blocks already use for
colour swaps, ADR-009 / `ClientSwapPredictor`):

- On the **client**, hook block placement (Fabric `UseBlockCallback`, or a place-side mixin/predictor) for
  `ArabicLetterBlock`. When the player places a letter, **immediately stamp the client's
  `ArabicLetterBlockEntity` letter + colour + lockedForm from the held stack's NBT** (the same NBT
  `onPlaced` reads on the server), so the glyph draws on frame one — before any server packet.
- The server still runs `onPlaced` + `ArabicJoinFlow` authoritatively; its sync packet reconciles with what
  the client already drew (same letter/colour from the same stack data → no visible change).
- Pair with **prewarm** (item 3, already shipped) so the predicted glyph's tile is already cached → no build
  hitch behind the predicted placement either.
- **Keep the architecture (owner decision 2026-06-20): auto-join stays live-texture/BlockEntity (ADR-005);
  we add prediction, we do NOT convert letters to slots.**

This is the **same client-prediction technique** O11's recolour fix needs (below) and the same family as
GROUP_26 FIX D (multiplayer client reading/awaiting server state).

> ✅ **RESOLVED — item 4 IS built** (`ArabicLetterBlock.onPlaced`, client-side branch stamps the predicted
> BlockEntity from the held stack the same tick, mirroring `ClientSwapPredictor`/ADR-009) and, combined
> with G13-23's fallback-render fix, **confirmed in-game 2026-06-27** ("it is now perfect" — flash gone
> on place, recolour, and re-flow; testing guide §B, moved to Passed). This section was stale — leaving
> it in place for the history, but there is nothing left to build here.

---

## G13-12 · Recolour a placed auto-join letter with the Squares (2026-06-20, build-green)  (was O11)

The coloured **Squares** (Group 06) recolour a placed **auto-join** letter to their colour — exactly the
way they swap colours on the 1028 SlotBlocks, so the same tool covers both. Before, a Square ignored a
letter entirely (it only matched `SlotBlock`).

- **Colour ONLY.** The letter's colour is per-block data on its `ArabicLetterBlockEntity` (the `color`
  field), so the Square mutates **only** that field and `sync()`s it. It never calls `setBlockState` and
  never re-runs `ArabicJoinFlow` — so **FACING, contextual form and neighbour joins stay exactly as
  placed.** Walking around a letter (any angle) and recolouring it cannot re-orient it or re-join it; this
  was the developer's explicit worry.
- **Instant, no pack rebuild.** The four Square colours (green / yellow / red / black) are the four bundled
  letter colours 1:1, and the renderer already builds one tile per `(letter, form, colour)`; the new colour
  syncs to the client and the tile rebuilds live — same no-reload path as auto-join itself (ADR-005).
- **Triangles** still do nothing on a letter (there is no slot variant to create — Triangles are a SlotBlock
  concept). Clicking with the same colour the letter already is → an "Already <name>" action-bar line.
- **Naming** of the action-bar feedback comes from `ArabicNaming.displayName(letter, colour, form)` →
  e.g. "Ba Green" (isolated) / "Ba Green Mid".

Touches: `item/ShapeToolItem.java` (the letter branch + `recolorArabicLetter`). The hotbar wording itself
is Group 04 / 06 (see those docs). Tests → TESTING GUIDE §16.

### In-game result on the server (owner, 2026-06-20) → recolour is NOT instant on a server

§16 claims "instant," and it **is** on the integrated/singleplayer host. **Owner re-tested on the dedicated
server: recolouring a letter is still slow — noticeably laggier than recolouring a normal custom block,
which is instant.** Two compounding causes, both server-only:

1. **No client-side prediction.** `ShapeToolItem.recolorArabicLetter` is **server-only** (gated on
   `ServerPlayerEntity`): it mutates the BlockEntity `color` then `sync()`s. The visible change therefore
   costs a full round-trip (click → C2S use → server mutate → BE update packet → client redraw). Normal
   blocks feel instant precisely because ADR-009 added prediction for *them* — and that ADR explicitly left
   Arabic recolour "out of scope … later." This is that "later."
2. **Texture rebuild on the new colour.** The renderer keys tiles by `(letter, form, colour)`
   (`ArabicLetterBlockEntityRenderer.textureFor`). A new colour is a **cache miss** → the tile builds
   off-thread → the glyph is blank for a few frames before the new colour shows. **Prewarm currently warms
   the 4 forms of only the placed colour, not the other colours** → every recolour pays a cold build.

**The fix (same family as O10 item 4 + ADR-009):**

- **Client-predict the recolour.** Extend the `ColorSwapTool` / `ClientSwapPredictor` seam (ADR-009) so that
  right-clicking an `ArabicLetterBlock` with a Square **applies the new colour to the client
  `ArabicLetterBlockEntity` immediately** (then `PASS` so the server still does the authoritative recolour).
  Instant on the server, reconciles with no flicker.
- **Prewarm all four colours**, not just the placed one — warm `(letter, every form, every Square colour)`
  for letters the player carries / is looking at, so the predicted recolour's tile is already built (no
  blank-frame gap).
- **Keep the architecture** (owner decision 2026-06-20): stays live-texture/BlockEntity (ADR-005) + add
  prediction; do **not** convert letters to slots.

> ✅ **RESOLVED — the client-predict fix IS built** (`client/ClientArabicRecolorPredictor`, registered in
> `CustomBlocksClient`, mirrors ADR-009's `ClientSwapPredictor`: paints the colour on the client
> BlockEntity the same tick and warms that colour's tiles). Combined with G13-23's fallback-render fix,
> **confirmed in-game 2026-06-27** ("it is now perfect"). The only thing NOT done is the fully-optional
> "prewarm all four Square colours ahead of time" polish (owner: deferred, flicker already perfect
> without it — see G13-23's "build 2"). This section was stale — nothing required left to build here.

---

## G13-23 · Auto-join flicker / transparent-flash on placement + recolour (NEW 2026-06-27) — LOCKED fix, not built

> 🔴 Owner report 2026-06-27: placed / recoloured auto-join letters **flash see-through (nothing) for a split
> second** before the glyph appears — on **placement** and on **Square recolour**, "sometimes". Code-verified.
> This is the unfinished tail of **G13-12 Cause 2** (cold tile build), promoted to its own ID.

**Root cause (confirmed in source).** The block is `BlockRenderType.INVISIBLE` — the glyph is 100 % drawn by
`ArabicLetterBlockEntityRenderer`. `render()` early-returns when `textureFor(letter, form, colour)` returns
`null`, and `textureFor` returns `null` on a **cache miss** while it builds the tile **off-thread** (the O10
lag fix — building inline froze the frame, so async is deliberate). Until the build + GPU upload land (a few
frames) the cube draws **nothing → see-through**. "Sometimes" = only a **cold** `(letter, form, colour)` key;
a previously-shown combo is cached → no flash.

**Why recolour flashes every time.** `ClientArabicRecolorPredictor` flips `be.setColor(newColour)` the **same
tick** it kicks the new-colour build (`prewarm`). The colour key changes instantly but its tile isn't ready
for several frames → a guaranteed transparent window. Prewarm only *races* the painter; a synchronous build
is banned (it stalls).

**LOCKED fix (owner 2026-06-27: "second option … fast and colour instantly as I click with Square, make it
perfect"):** never draw nothing, and make recolour seamless —

1. **Recolour = keep the old glyph until the new one is ready (zero blank frame).** Don't flip the *rendered*
   colour on click. Render the **last cached tile** while the new colour builds; swap to the new colour only
   the frame its tile is cached. The click still feels instant — the letter never disappears, it just changes
   colour the instant the paint lands (≈ a couple frames), no see-through gap. (Still predict the colour on the
   BE for the server round-trip; only the *rendered* colour is gated on tile-readiness.)
2. **Universal placeholder for genuine cold draws (placement / brand-new combo).** When there is **no** cached
   tile to fall back to, draw an **opaque solid colour-bg cube** (the letter's bg colour, a trivial 1×1 sync
   texture) instead of early-returning — a solid coloured cube for those few frames, then the glyph pops in.
   Never see-through.
3. **Aggressive prewarm** stays / extends (all four Square colours for held + looked-at letters) so the swap
   tile is usually already cached → step 1 swaps with no perceptible delay.

**Touches (when built):** `client/render/ArabicLetterBlockEntityRenderer` (fallback-render instead of
early-return; track last-good tile), `client/ClientArabicRecolorPredictor` (gate rendered colour on
readiness), maybe `block/ArabicLetterBlockEntity` (a "displayed colour" vs "logical colour"). **Render-thread
only — no server / protocol change.** Supersedes the open tail of G13-12.

**Build 1 (parts 1+2 — never see-through) ✅ CONFIRMED IN-GAME 2026-06-27 ("it is now perfect"); build 2 (all-four-colour prewarm) optional polish, not started. Status/tests live in the testing guide (§B) per doc rule.**

---

## G13-21 · Word-block system — keep & upgrade "100x" + rebrand

> 🔍 diagnosed — **co-design with G13-22 in one Arabic session**; do G13-20 first; G13-21 = system/back-end, G13-22 = Studio tab front-end

> *"those are a different cool newly created system why remove them? infact i wanna upgrade them even
> more and make their system name better and overall 100x better"*

**Status: 🔍 diagnosed (2026-06-24).** A kept, valued feature the developer wants **expanded + rebranded**,
not removed (distinct from G13-20). Deep-dive done — the four big gaps below are real, read from code.

**What it actually is (mapped from code):**

| Piece | Reality |
|---|---|
| `ArabicWordRenderer.render(text, textColor, bgColor)` | A **general text→PNG renderer** (Java2D). Auto-picks recipe **by script**: Latin words/numbers · Arabic words · Arabic numbers. Locked dev-approved recipes (font/outline/dilation/size/tracking, 2026-06-15/16). 2× supersample → crisp. **Not Arabic-only.** |
| `ArabicBlockRegistry.importWord(text, id, name, textColor, bgColor[, rebuildPack])` | Renders, then **creates a normal `SlotBlock`** with that PNG (or re-textures if id exists). A "word block" **is a slot block** with a generated texture — it already rides the whole slot/pack/HUD/tool system. |
| Flow (`ArabicMaker`) | `/cb arabic word <id> <name>` (or GUI Name→ID) → text anvil → `WordChoiceMenu` → Color Studio → **live preview** (pack-free `/tex/<id>` + `ARABIC_PREVIEW` client screen, re-render on colour change) → `finalizeWord` → `importWord` + give + `HudSync`. |
| State | `ArabicWordSession` (per-UUID): name, id, text, bgArgb, letterArgb. **Transient — cleared after create.** |

**The four real gaps (the "100x" + rebrand targets):**

1. **Mis-named / mis-filed.** The renderer handles **Latin words & numbers** too — "Arabic word" badly
   undersells it. The developer's "better system name" = promote it to a general **Text/Label block**
   maker (Arabic just one script it supports). The `/cb arabic word` door undersells it too.
2. **Not re-editable (biggest gap).** The source `text` (and colours) live **only** in the transient
   `ArabicWordSession`, discarded on create. `SlotData` stores **no** text/colours, so you **cannot
   reopen a word block and change its text** — you remake it from scratch. Persisting the source text +
   style on the block is what makes it a real, living system.
3. **Thin styling.** Only `letterArgb` + `bgArgb` are exposed; the **outline is hard-coded black**
   (`ArabicWordRenderer`), font/size/tracking/recipe are all **LOCKED**, and it's **single-line only**
   (one centred `TextLayout`, no wrap/multi-line, no phrase-across-blocks).
4. **bg is baked into the PNG** → same "background" axis as G10-1; a word block's bg should ride
   the shared `background` attribute, not a one-off baked colour.

### Developer's decisions (locked via UI 2026-06-24)

- **System name = "Text Blocks"** — promote from "Arabic word" to a general text-block maker (Arabic +
  Latin, words + numbers). Keep `/cb arabic word` as an alias.
- **Re-editable = YES** — persist the source text + colours + style on the block (`TextData` on
  `SlotData`) so a Text block can be **reopened and retyped/restyled**, re-rendering the same slot. This
  is the core "100x." Needs a new `SlotData` field + one-time migration.
- **Expose ALL styling axes** — outline colour (not just black) · font pick · size/spacing · effects
  (gradient / shadow / border). All four are wanted.
- **Layout = fully configurable, many options** — both **multi-line within a block** AND **a phrase
  spanning multiple blocks**, "changeable and configurable fully" by the owner. Build the renderer +
  `TextData` so layout is data-driven, not a fixed mode.

### Brainstorm (upgrade direction — NOT built)

1. **Rebrand to the general "Text Block" system** — one maker for Latin + Arabic, words + numbers, script
   auto-detected exactly as the renderer already does. Keep `/cb arabic word` as an alias.
2. **Persist the source on `SlotData`** — a small immutable `TextData {text, textColor, bgColor, style…}`
   (same pattern as `AnimData`). This unlocks **re-editing** (reopen → change text/colour → re-render the
   same slot), reproducibility, and a future "edit text" tool. The single load-bearing upgrade.
3. **Expose styling** (incrementally): outline colour (not just black), font pick, size/tracking, and the
   bigger ones — **multi-line / wrap** and **text effects** (gradient/shadow/border). Each is an additive
   field on `TextData` + a renderer branch.
4. **Reuse the proven live-preview rail** — the pack-free `/tex/<id>` + `ARABIC_PREVIEW` screen already
   works; the upgraded maker (and G13-22's Studio tab) render on it.
5. **Fold the background axis** — a Text block's bg reads the G10-1 `background` attribute instead of
   a baked one-off, so transparency/bg-colour tools work on it like any block.

### Open questions — RESOLVED 2026-07-02

- **Migration — LOCKED: legacy-locked-until-retyped.** An old word block (made before `TextData`
  existed) has no saved source text — only the baked picture survives. Its Edit button stays locked
  ("type it once to enable editing") until the owner retypes the text once through the normal maker
  flow; from that point on it has a real `TextData` and edits normally forever after. **Do NOT**
  attempt to guess/reconstruct old text from the block's saved name — rejected, guesses could be wrong
  with no way for the owner to notice.
- **Effects depth — LOCKED: full set, built one at a time.** Outline colour, font pick, size/spacing,
  gradient, shadow, border are **all** in scope for G13-21 — not scoped down to a "basics only" cut.
  Build order: ship and in-game-confirm one styling axis before starting the next (CLAUDE.md §4 — no
  batching). See the roadmap phase order at the top of this doc for where this sits overall.
- **Multi-block continuous render — RESOLVED, no new work needed.** G13-19's seam fix (z-flush at the
  cube surface) is **already built and confirmed in-game 2026-06-27** (testing guide §A1/A2, "seam gap
  sealed, 6 faces flush z=1.0 ✅"). A spanned phrase across multiple Text Blocks reuses that same
  already-shipped fix — no separate pick, no rebuild.
- **G13-22 split — CONFIRMED.** G13-21 = system/back-end (rename, re-edit, styling, layout).
  G13-22 = the Studio tab front-end for it. Build together, back-end first (see roadmap Phase 5).

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic → **Text Blocks** (rebrand) | Both — server render + slot/pack; preview is client | 🔍 diagnosed — decisions locked (Text Blocks · re-edit · all styling · multi-line + multi-block, fully configurable); migration + G13-22 co-design open |

**Related:** **G13-22** (Studio Arabic tab — the front-end for this; build together) · **G13-20** (auto-join letters/numbers — shares the maker flow & `WordChoiceMenu`) · **G10-1 / G10-1** (background axis — fold bg in) · `ArabicWordRenderer` · `importWord` · `ArabicMaker` / `ArabicWordSession` · `/tex/<id>` live-preview rail
**Touches (when built):** `SlotData` (+`TextData` field for re-edit/style) · `ArabicWordRenderer` (expose outline colour/font/size/multi-line/effects) · `ArabicBlockRegistry.importWord` (persist + re-render from stored `TextData`) · `ArabicMaker` (reopen/edit path) · rename surface (`/cb arabic word` → general text-block command + alias) · G13-22 Studio tab

---

## G13-19 · Unify SlotBlocks + Arabic Auto-Join (Shared Contract + Seam Fix)

> 🔴 **Part A Deleter REGRESSED 2026-06-27 (19.4)** — Part A shipped the `CbBlock` contract + Deleter on Arabic (in-game 06-26: N1/N2/N3 ✅). Owner retest 2026-06-27: Deleter on an Arabic auto-join letter now FAILS — *"just breaks in the world, doesn't get deleted as a definition, just bugs out."* Something regressed since 06-26. **Superseded 2026-07-02** — this whole section (letters as a separate render system from SlotBlocks) is being replaced by a real-slotblock rearchitecture (see §G13-25 below, fully rewritten). The Deleter fix lands as part of that rebuild, not as a standalone patch. Re-verify the Deleter→`CbBlock.cbDelete` dispatch (`DeleterItem`) + that `cbDelete` runs for `arabic_letter` and that `world.removeBlock` actually removes it (auto-join letters have no shared slot definition — each is its own placement, so "doesn't delete the definition" likely means the block isn't being removed / routing broke). Part B (seam/gaps) ✅ CONFIRMED 06-27 (19.1/19.2). Remaining tool migrations (attrs/lock/`delete #`) still 🔍 designed.
>
> 🔍 designed — **do first in C5**; shared `CbBlock` contract unlocks G13-20 + G13-22; seam fix verified in code, confirm in-game when building
>
> **Progress:** `block/CbBlock.java` interface added (lean: `cbDelete` only, grows per migrated tool). `SlotBlock` + `ArabicLetterBlock` implement it; `DeleterItem` routes through it → Deleter now works on Arabic letter blocks (was `instanceof SlotBlock`-blind). **Still open:** the `z=0.999` seam fix (Part B — needs in-game repro to pick a/b/c), `/cb delete #` on Arabic, and migrating the other tools (`setglow`=G13-24, recolor). *(G13-23 is now the auto-join flicker fix — see its section.)*

> *"i want to unify the system of slotblocks and arabicautojoining blocks because they keep having conflicts with each other … tiny gaps between arabic blocks form, which is unprofessional"*

### What's broken

**Tool disparity (confirmed in code):** `DeleteCommands.java:89` checks `instanceof SlotBlock`, returns null for Arabic letter blocks → delete silently fails on them. Most tools have the same `instanceof SlotBlock` gate. Arabic auto-join blocks are invisible to the tool layer.

**Word blocks are fine** — they ARE SlotBlocks (baked into slots), atlas renderer, all tools work normally. Issue is only with `ArabicLetterBlock` (the auto-join block).

**The z=0.999 seam (confirmed in code):** `ArabicLetterBlockEntityRenderer.java:178` — `float z = 0.999f` draws each face just inside the cube surface to kill overhang lines. Side effect: adjacent letter blocks have a hairline recessed outline at their borders. Also confirmed in `AnimSlotBER.java:91`. Verify exact visual in-game before picking the fix.

### ✅ All decisions (locked 2026-06-23, 2026-06-25)

- **NOT a true merge** — keep two render systems separate (ADR-005 deliberate split). Unify = shared rules + shared contract only.
- **Shared `CbBlock` interface:** both `SlotBlock` and `ArabicLetterBlock` implement it. Methods: `displayName()`, `color()`, `recolor()`, `delete()`, `isTransparent()`, `kind()`. Every tool/command/GUI routes through the interface instead of `instanceof SlotBlock`. Incremental: add interface, migrate one tool at a time (start with Deleter + Square recolor — they already special-case both).
- **Seam fix:** verify in-game during build. Candidate fixes: (a) draw front/word-axis face flush at z=1.0 while keeping inset on non-join faces; (b) bleed joining bar past tile edge to cover seam; (c) continuous word rendering (one texture sliced across blocks — biggest change). Pick after in-game repro.
- **Static number blocks (80 remaining):** stay on atlas (they don't join); auto-deleted when G13-20 ships auto-join numbers.

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic | Both — tool behavior + render seam | 🟡 Part A build-green 2026-06-26 (contract + Deleter, awaiting §N in-game); Part B seam fix + tool migrations pending |

**Related:** ADR-005 (deliberate split) · G13-20 (auto-join revamp; do after this) · G06-2/G06-3 (Deleter/recycler — first tools to migrate onto contract) · G06 Tools
**Touches:** new `CbBlock` interface · `SlotBlock` + `ArabicLetterBlock` (implement it) · tool/command/GUI call-sites (route through interface, remove `instanceof SlotBlock` gates) · `ArabicLetterBlockEntityRenderer` (z-inset seam fix — method TBD after in-game verify)
**Verify in-game:** Deleter works on Arabic letter blocks; Square recolor works on Arabic letter blocks; seam/hairline gap is gone between adjacent letters.

---

## G13-20 · Consolidate Arabic — One Auto-Join System + Join/Render Bug Fixes

> 🔍 designed — do G13-19 first (shared contract + seam fix); then this. ⚠️ sequence: revamp join → add number auto-join → THEN retire static numbers

> *"there are bundled letters and numbers … i want to remove entirely blocks that dont auto join … the existing auto joining system needs a revamp"*

### ✅ All decisions (locked 2026-06-23, 2026-06-25)

- **One unified auto-join system for letters AND numbers.** Numbers = always isolated form (no kashida connecting bars — linguistically correct). Appearance customization (mirror/orient options) designed during build session when developer can describe exactly what they want in-game.
- **Retire everything that doesn't auto-join** — air-clean placed copies (developer has art backed up).
- **Sequencing (hard rule):** build number auto-join FIRST → THEN retire static number blocks (auto-deleted). Never remove static numbers before their replacement exists.
- **Auto-join revamp required** — letters join wrong + render buggy (see confirmed bugs below).

### ✅ Confirmed bugs + fixes (from in-game screenshots, 2026-06-25)

**Bug 1 — Ghost render on top/bottom faces:**
The BER renders on ALL visible faces including top/bottom, not just the reading faces. This causes a second faded ghost rendering visible when looking at blocks from certain angles.
**Fix:** Only render on north face + south face. Stop drawing on top/bottom/east/west faces of each block.

**Bug 2 — Back face shows unmirrored glyphs:**
From the back of a word, letter glyphs appear as their own backwards reflection. Example: حمد placed facing south — from the north (back), ح is on the left, د on the right, but each letter's individual glyph is NOT mirrored, so they look like incorrect reversed shapes.
**Fix:** When rendering the north face (back), flip the UV coordinates horizontally for each glyph quad. Mirroring a ح initial form gives a visually correct ح when read from behind. Works for all letters including non-joining (اك etc.).

**Bug 3 — Breaking a letter from any face leaves stale connecting bars:**
When a middle letter (e.g. م in حمد) is broken, the remaining neighbors (ح, د) keep their old contextual forms. The connecting kashida bars drawn by adjacent blocks don't update → a ghost of the broken letter's connection lingers.
**Fix:** Server-side: on ANY block-break event on an `ArabicLetterBlock` (regardless of which face was hit), force re-calculation of ALL adjacent Arabic letter blocks' contextual forms and push the updated form to each neighbor immediately.

### Build order (within G13-20)

1. Fix the join/render bugs above (Bug 1-3) — this is the recording-quality fix and the foundation.
2. Extend auto-join to numbers (one unified `arabic_glyph` block: letters join, numbers stay isolated).
3. THEN retire the 80 static number blocks (extend `ArabicLetterRetirement` to number ids, auto-runs on update).
4. Remove legacy letter paths (`importAll` / `importLetter` via `ArabicLetterMap`) — kills duplicate source.

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic | Both — render + content migration | 🟡 **Bug 1 (ghost faces) + Bug 3 (stale bars) build-green 2026-06-26** (render batch, awaiting in-game §O O2/O3). Bug 2 (back-face mirror) **NOT changed** — likely fixed by the face cleanup; §O O4 verifies, targeted follow-up only if still wrong. Steps 2-4 (number auto-join → retire static numbers → drop legacy paths) still 🔍 designed |

**Related:** G13-19 (shared contract + seam fix — do first) · G13-21 (word blocks — kept, NOT removed) · G13-22 (Arabic Studio tab — builds on this) · `ArabicLetterRetirement` (retire mechanism to extend to numbers) · `ArabicBlockRegistry` / `ArabicArt` (static content to retire)
**Touches:** `ArabicLetterBlockEntityRenderer` (Bug 1: face filter; Bug 2: north-face UV flip) · `ArabicJoinFlow` (Bug 3: forced neighbor recalc on break) · unified `arabic_glyph` block (numbers) · `ArabicTileRenderer` (isolated number tiles) · `ArabicLetterRetirement` (extend to numbers, sequenced) · retire `ArabicBlockRegistry.importArt` + `importAll`/`importLetter`
**Verify in-game:** no ghost render on top/bottom of letter blocks; back face of word reads correctly (letters mirrored, RTL readable); breaking a letter from any face instantly updates all neighbors; number auto-join places correctly (isolated, side-by-side); static numbers gone after retirement; no legacy Arabic creation paths remain.

---

## G13-22 · Text Studio Tab — Unified Arabic + English Text Creation Screen

> 🔍 designed — replaces ALL chest-menu Arabic flows; do G13-20 first (auto-join revamp must exist before the tab surfaces it); co-design with G13-21 (word-block upgrade)

> *"when i type /cb arabic word <arg> <arg> then choose 1 single block, i want this entire menu reworked into an actual screen, that is linked to '/cb create' as a separate tab"*

### What's broken with the current flow

`/cb arabic word` requires hidden positional args (`<id> <name>`) before opening anything — completely unfriendly, fails silently with no guidance. `/cb arabic letter <name>` requires knowing the internal glyph name. The chest-menu word flow is multi-step (anvil → WordChoiceMenu → Color Studio) with no live preview. This entire surface is replaced by the new tab.

### ✅ All decisions (locked 2026-06-24, 2026-06-25)

- **Tab name:** "Text" — covers both Arabic and English/Latin, not just Arabic.
- **Location:** left tab list in `BlockCreationStudioScreen` (same `Section` system as TEXTURE/SHAPE/ANIMATION/AI/CATEGORY tabs). NOT a separate window.
- **Scope (both languages, confirmed in code):** `ArabicWordRenderer` already handles Arabic (arabtype.ttf) AND Latin/English (RockwellCondensed.ttf). v1 tab exposes both equally. English text blocks and Arabic word blocks use the same render pipeline.
- **Layout (Option A — preview top, controls below):**
  - Large live preview panel at the top
  - Text input field below it
  - Mode toggle (Word Block / Loose Letters / Numbers) below input
  - Colour pickers (BG + letter colour) below mode
  - CREATE button at bottom
- **Live preview:** server-renders via existing `ArabicMaker.renderPreview` + `/tex/<id>` rail (fonts are server-side — client-side render would break on dedicated). Debounced on keystroke — re-renders on text/colour change. Reuses the proven `ARABIC_PREVIEW` path, surfaced inside the tab.
- **Loose letters CREATE behaviour:** letters go to inventory (same as current `/cb arabic letter`). Tab includes a count field per letter so you can request e.g. 3× ح and 1× م in one click.
- **Word block CREATE:** delegates to `ArabicMaker.finalizeWord` / `importWord` (unchanged backend).
- **Retires the chest flow:** `WordChoiceMenu` + chest Color-Studio steps folded into the tab. `/cb arabic word` and `/cb arabic letter` commands will eventually re-route to open the tab instead.
- **`/cb arabic` subcommand:** gets merged/changed later (noted; not blocked on G13-22 build).

### Architecture (code-grounded, NOT built)

- New `Section.TEXT` enum value in `BlockCreationStudioScreen` + panel class `StudioTextPanel` (mirror `StudioAnimPanel`/`StudioAiPanel` pattern, own file, under 500-line gate).
- A `default → "coming soon"` placeholder branch already exists in `StudioSections` — the tab slots in cleanly.
- Edit-mode entry (`CreationStudioBridge.openStudioEdit` → `StudioEditPayload`) and create/save rails (`createFromStudio` / `saveFromStudio`) already built and server-authoritative.
- `ArabicWordSession` kept only if needed for server-side hand-off; otherwise replaced by `StudioState`.

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic / Text (Studio surface) | Both — client screen + server render/create; dedicated-safe via existing preview rail | 🔍 designed — all decisions locked 2026-06-25; ready to build after G13-20 |

**Related:** G13-20 (auto-join revamp — must exist before tab surfaces letters/numbers) · G13-21 (word-block upgrade — tab is its front-end) · G27-2 (Studio-tab pattern — same `Section` infra) · `ArabicWordRenderer` / `ArabicMaker` / `importWord` (backend to reuse) · `WordChoiceMenu` / `ArabicWordSession` / Color Studio (chest flow to retire) · `ColorLibrary` (colour pickers)
**Touches:** `BlockCreationStudioScreen` + `StudioSections` (new `Section.TEXT` + panel) · new `StudioTextPanel` (text input + mode toggle + colour pickers + live preview + count field) · client dynamic-texture preview from `ArabicWordRenderer` bytes via `/tex/<id>` · `ArabicCommands` (re-route `/cb arabic word` and `/cb arabic letter` to open the tab) · retire `WordChoiceMenu` + chest Color-Studio steps
**Verify in-game:** `/cb arabic` (or Studio → Text tab) opens the left-tab screen; typing Arabic and English shows live preview; word block CREATE makes the correct block; loose-letter CREATE gives correct count to inventory; numbers mode works; old chest word-maker no longer appears.

---

## G13-23 · Attributes (setglow / etc.) don't apply to Arabic auto-join blocks

> 🆕 reported in-game 2026-06-25 (owner) — NOT investigated yet. Surfaced testing G06 §K row 4.

> *"when i try to setglow on an arabic autojoining block, doesnt change, i think other attributes too… BUT NORMAL COLORED VARIANTS WORKED FOR SETGLOW AND RECOLOR WITH SQUARE."*

**Symptom** — `/cb setglow <arabicId> <n>` on a placed **Arabic auto-join letter** block does nothing
(no glow change). Owner suspects other attributes (hardness / sound / collision) are affected too, but
didn't enumerate. **Normal blocks AND coloured variants work fine** for setglow + Square recolour — so
the gap is specific to the Arabic letter block path.

**Why (hypothesis — verify in code before building):** Arabic letters are a separate block type
(`ArabicLetterBlock` + `ArabicLetterBlockEntity`), not the `SlotBlock` that reads luminance/hardness from
`SlotData`. The `/cb setglow` handler mutates `SlotManager` slot data + the `SlotBlock` `LIGHT` state;
an `ArabicLetterBlock` likely (a) isn't resolved/mutated by that path, or (b) doesn't read glow/attrs
from `SlotData` at all. Need to confirm: does `SlotManager.getById(arabicId)` resolve a letter block, and
does `ArabicLetterBlock` honour `glowFor`/hardness/etc.?

**Fix direction (TBD after investigation):** either route attribute commands through a shared block
contract so letters honour glow/hardness/sound/collision like SlotBlocks (ties to **G13-19** shared
`CbBlock` contract), or give `ArabicLetterBlock` its own attribute storage + read path.

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic | Both — attribute commands are server-side | 🆕 reported — needs code investigation |

**Related:** G13-19 (shared block contract — the clean place to make letters honour attributes) · G06 §K row 4 (where it surfaced) · `SlotManager.setGlow`/`setHardness`/etc. · `ArabicLetterBlock` / `ArabicLetterBlockEntity` · `SlotBlock` luminance/shape read path
**Touches (to confirm):** the `/cb setglow` (+ sibling attribute) handler's block resolution · `ArabicLetterBlock` attribute read · possibly the G13-19 contract
**Verify in-game:** `/cb setglow <arabicId> 12` lights the placed letter; hardness/sound/collision likewise apply to a letter block; normal blocks + variants still work.

---

## G13-25 · Letters become REAL SlotBlocks (full rearchitecture — restarted DATA-ONLY after the partition regression; CP1 🟢 built 2026-07-03)

> 📝 **REVERSAL, locked 2026-07-02.** The 2026-07-02 "settings sheet" design (kept in the archive
> note at the bottom of this section for history) was built (steps 1+2) and then **reverted the same
> day** — the owner clarified the real ask was misunderstood. This section replaces it entirely. Do
> not build the old sheet design; it is dead.
>
> *"i wanted to make all the arabic auto joining block letters system to be slotblocks and like
> normal, and old system removed, so when i use deleter tool it deletes definition, when i use
> triangles it just does it without any issues, and auto joining still same as is with no bugging it
> or anything."*

**Build state (started 2026-07-02 · spec = `docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md` · each checkpoint gates the next on in-game confirmation):**

| CP | What | Status |
|---|---|---|
| 1 | 164 base (black) letter/number slots pre-baked + registered at boot as normal flagged SlotBlocks (`ArabicSlotBootstrap`: data-only `ArabicMeta`, normal-pool allocation, idempotent by stable id `arabic_<glyph>_<form>` with fixed iso/ini/mid/fin tokens); dead `ArabicSlotBlock` subclass (the FACING hazard) DELETED; `SlotManager.createArabicNoSave` added | 🟡 in-game 2026-07-03: blocks exist, no regressions — BUT isolated letters wore the WRONG art (bundled hand-art; the live join system font-draws EVERY form, `ArabicLetterBlockEntityRenderer.build()`). **v2 fix built same day:** all 4 letter forms font-baked, `BAKE_VERSION` sidecar re-bakes existing textures once at boot (numbers keep hand-art). Re-test in the §N sweep |
| 2 | Letter facing + readable back face: facing + mirror-partner slot stored on the shared `AnimSlotBlockEntity` NBT (server-stamped, packet-synced — NO blockstate property); Arabic slots' pack model switched to the invisible off-atlas marker so `AnimSlotBER` owns their faces (`ArabicSlotFaces` — port of the confirmed old-renderer geometry: flush z=1.0, full-bright, back = 180° turn no flip) | 🟢 built 2026-07-03, awaiting in-game — TG §N |
| 3 | Auto-join re-flow: `ArabicSlotJoinFlow` (server-only port of the confirmed `ArabicJoinFlow` whole-run walk, MAX_RUN 64, single-air-gap back-mirror bridge) — form change = sibling form-slot SWAP via the proven `swapPlaced` pattern (`setBlockState` + LIGHT); hooks on `SlotBlock.onPlaced`/`onStateReplaced`, data-gated on `ArabicMeta`, re-entrancy-guarded; missing sibling degrades (keeps form) | ⚠️ MP-tested 2026-07-03: joins/re-flows CORRECT but not instant, mixed bright/dim letters in one word, back-mirror not applied — one root: server-only, no client prediction (old system predicted client-side). → CP3b |
| 3b | **Instant feel (the MP fixes):** ✅/⚠️ **owner-tested MP 2026-07-04:** N4 back-mirror ✅, N5 place-join ✅ (owner note: wants a small "how it corresponds" revamp — open, needs his explanation first), N8 Deleter-on-new-letter ✅, **N6 break-re-flow ⚠️ works but NOT instant** (still open — the mid-break path feels server-paced). Detail:  the SAME `ArabicSlotJoinFlow` now runs on BOTH sides — server authoritative, client as instant prediction the tick a letter is placed/broken (the old system's proven dual-side pattern: `ArabicLetterBlock.onPlaced` ran `ArabicJoinFlow` on both sides). Client swaps use `NOTIFY_LISTENERS\|FORCE_STATE` + client BE facing/back stamps — the SHIPPED `ClientSwapPredictor` mechanism (ADR-009), chosen over the earlier "transient render state, never client setBlockState" sketch because that exact setBlockState prediction is already owner-confirmed for Square swaps (identical result on both sides = packets reconcile with no visible change). Remote-server clients know each slot's ArabicMeta via the new HudSync `"ar"` tuple → `ClientSlotCache.Entry.arabic` → `ArabicClientView` seam (`ArabicSlotJoinFlow.CLIENT_VIEW`, common code can't import client classes; SlotBlock's hook gate is session-aware too or remote prediction would never fire). `inFlow` guard now per-thread (ThreadLocal — SP runs server + client flows on two threads). Plus: Arabic LETTERS always draw full-bright in `AnimSlotBER`'s plain-cube fallback (kills the dim/grey letter; numbers keep world light) | see the ✅/⚠️ sweep results at the row start — remaining: N6 instant-fix + N5 revamp |
| 4 | Colours — **AMENDED by owner 2026-07-03 (UI answers, supersedes decision 4's "on-demand only"):** ALL FOUR colours pre-baked at boot — every letter×form and number exists in black+red+green+yellow = **656 base slots**, coloured with the **exact CONFIG hexes** (`triangleRedHex`/`GreenHex`/`YellowHex`), NOT the old bundled-art colours (art red `0xFFFF0000`/green `0xFF1E8C1E`/yellow `0xFFF0C814` are DEAD as block colours). Letters bake via `ArabicTileRenderer` with config-hex bg; numbers keep the hand-art LOOK (black art's bg recoloured to the config hex). Ids = standard variant suffix `arabic_<glyph>_<form>_<red|green|yellow>`. Custom-hex still on-demand via tools with sibling-linked creation (decision 6 stays for customs). Config hex change → coloured set re-bakes (recolorVariants/BAKE_VERSION rail). + tool parity sweep + Square-swap-on-letter facing re-stamp | 🟢 **built 2026-07-04** (green, NOT in-game — TG §O): 656 pre-baked (letters font-baked on config-hex bg, numbers = black hand-art bg-recoloured via the proven BackgroundRemover/ImageProcessor variant path); hex sidecar re-bakes a coloured set once when its config hex changes; Square-swap now captures + re-stamps the letter's BE facing/back and re-flows the run (`ArabicSlotJoinFlow.reflowAfterSwap`). ⚠️ NOT built: decision-6 sibling-linked CUSTOM-hex creation (custom tool on a letter still makes a plain non-joining variant) — open sliver |
| 5 | **ONE SYSTEM ONLY (owner 2026-07-03: "its a mess, i want only 1 bundled to survive"):** remove the old `customblocks:arabic_letter` NBT system ENTIRELY (block/BE/renderers/join-flow/prewarm/old creative tab) + retire the 224 static letters AND 80 static numbers. **Placed old blocks VANISH** (owner ruled: air-clean is fine, "i havent used them yet" — no migration sweeper). Bundled art PNGs stay in the jar ONLY as the numbers' bake source, never as blocks. Full regression sweep after | 🟢 **built 2026-07-04** (green, NOT in-game — TG §O; owner 2026-07-04: "old system needs to be nuked"): deleted block/BE/item/registry/renderers/old ArabicJoinFlow/prewarm/recolour-predictor + arabic_letter model+blockstate assets; `/cb arabic import` + `ArabicBlockRegistry.importArt` removed; static NUMBER art blocks (80) retired at boot via the Build-B rail (letters were already); placed old blocks vanish (unregistered → air, owner-ruled). "Arabic Letters" tab REPURPOSED to list the real Arabic slots (was the owner's "own creative menu tab" ask); `/cb arabic letter` + word-maker "Place letter blocks" now give real slots (colour arg → "_&lt;colour&gt;" variant) |

> 📌 Bulk sneak+Triangle/Square + the `/cb arabic` command surface (decision 7) follow AFTER CP5, in
> the owner's separate "revamp `/cb arabic`" discussion — not part of these checkpoints.

> 💔 **CP1 regression + boundary problem (2026-07-03).** CP1 assumed normal custom blocks only ever
> occupy indices 0–799, so it reserved 800+ for Arabic as a fixed compile-time boundary. The owner's
> real world runs **maxSlots=2000 with ~1100 blocks assigned** — normal blocks already live past 800.
> The fixed boundary therefore (a) capped `/cb create` at 800 → "every slot is in use" with ~900 free
> slots, and (b) re-classed live normal blocks at index ≥800 as `ArabicSlotBlock`. Reverted.
>
> The lesson: the Arabic pool **must never overlap any index a normal block can occupy**, and the
> per-index class must stay a compile-time constant (the client/server heal path fixes slot *count*
> only, not per-index *class*). A fixed *low* boundary can't satisfy both.
>
> ✅ **Redesign LOCKED (2026-07-03, owner approved): DATA-ONLY.** No partition, no subclass, no
> FACING blockstate. An Arabic letter/number is a plain `SlotBlock` whose `SlotData` carries
> `ArabicMeta`, allocated from the same free pool as any normal block — so it is impossible for it
> to occupy an index differently from a normal block, on any world. Orientation (CP2) lives in the
> shared slot BlockEntity's NBT, read by the off-atlas renderer (Group 14 / ADR-013 machinery) —
> zero blockstate change, zero class divergence. `ArabicSlotBlock.java` deleted.

**Decision 1 (was locked, now REVERSED):** letters WILL become real registered `SlotBlock`-family
blocks with real baked textures and real `SlotData` — not a parallel settings layer, not a live
per-frame render kept separate "for safety." The earlier reasoning (a slot's texture is fixed at
bake time, a letter's shape changes live per neighbour, so baking would kill auto-join) is real, but
solvable: bake ONE real slot per (letter × contextual form) instead of one slot per letter, and swap
which registered block is placed as neighbours change — using the exact block-swap mechanism already
proven safe in this codebase (`core/ColorVariantService.swapPlaced`, which already does
`world.setBlockState(pos, otherRegisteredBlock.getDefaultState()...)` live, in the world, with no
flicker, for every normal custom block's Square colour-swap today).

> 📌 **OWNER AMENDMENTS 2026-07-03 (AskUserQuestion UI, after the §N MP sweep) — these override the
> matching 2026-07-02 decisions below:**
> 1. **Build order:** CP3b instant-fix FIRST (prediction + brightness + back mirror), owner tests;
>    then CP4 colours; then CP5 removal. Two small builds beat one big one.
> 2. **Placed old letters/numbers VANISH at CP5** (air-clean, no migration) — overrides nothing
>    (decision 9 already said no migration) but now explicitly re-confirmed for the real server:
>    owner: "vanish if that is better and doesnt need more coding cz i havent used them yet".
> 3. **All 4 colours PRE-BAKED at boot (656 base slots)** — overrides decision 4's black-only
>    pre-bake. Colours use the EXACT config triangle hexes, not the old bundled-art colours
>    (owner: "only 1 bundled to survive with the ones that has the exact hexes of red grren yellow
>    in config, not old ones"). Decision 6 (sibling-linked on-demand creation) now applies only to
>    CUSTOM hex colours.
> 4. **Numbers keep the hand-art look**; the bundled art PNGs survive in the jar ONLY as bake
>    source. The old static art BLOCKS (all 224 letters + 80 numbers) and the whole
>    `customblocks:arabic_letter` NBT system are removed at CP5 ("old system removed and removed
>    from bundle entirely").

### ✅ All decisions locked this session (2026-07-02, extensive back-and-forth — do not re-litigate)

1. **Real slot per (letter × form).** 36 letters × 4 contextual forms (isolated/initial/medial/final)
   = 144 base registrations. Each is a genuine `SlotManager`-style slot: real `SlotData` (glow,
   hardness, sound, collision), a real baked PNG texture, a real `BlockItem`. Auto-join no longer
   writes an internal `form` field — it swaps the placed block to the sibling form's registered
   block, same colour, via the proven `setBlockState` swap pattern.
2. **Numbers folded into this same rebuild, now** (was going to be a separate later phase — no
   longer). Numbers never grow connecting bars (linguistically correct, always isolated) so they need
   only 1 form each: 10 Eastern (`a0`-`a9`) + 10 Western (`e0`-`e9`) = 20 base registrations, no
   sibling-form complexity at all.
   **Base slot total: 144 + 20 = 164**, pre-baked (see decision 4).
3. **The 224 bundled static Arabic art blocks (`ArabicBlockRegistry`, non-joining, already real
   SlotBlocks) are RETIRED.** They're redundant once join-letters have full colour/tool parity — a
   real join-letter now does everything they did, plus joins. One unified letter system. (This also
   cancels G13-9 "hide/manage the 224 bundled letters" below — nothing is left to hide.)
4. **Base (black) forms are PRE-BAKED at boot**, not created lazily. All 164 base registrations
   exist from the moment the jar loads, guaranteed, every time — "pre bake all of them, but I want it
   perfectly, no issues" (owner). Colour variants (anything beyond the default black) are still
   created **on demand**, exactly like every other custom block's colour variants already work
   (`ColorVariantService.createVariant` / the Triangle tool) — pre-baking is only for the base set,
   not for an unbounded space of possible colours.
5. **Colour scope = full parity with normal blocks.** Triangle presets (red/yellow/green/black) AND
   the custom-hex tools (`CustomColorToolItem`) both work on letters exactly as they do on any normal
   custom block — "keep it how it is now, BUT can be coloured with customs etc, literally how normal
   slotblocks are built" (owner). Not limited to the 4 bundled colours.
6. **Sibling-linked CREATION, independent EDITING.** The first time any form of a letter gets a new
   colour (Triangle/hex), all 4 sibling form-slots of that letter+colour are created together, in one
   action — this is a correctness requirement, not a preference: auto-join swaps a placed letter
   between forms of the SAME colour as neighbours change, and if a sibling form-slot didn't exist yet
   the swap would have nothing correct to land on (a visible glitch at the exact moment two letters
   join — the "bugging out" the owner explicitly ruled out). After that one-time linked creation, each
   of the 4 form-slots is independently Deleter/Triangle/Square-able — recolouring or deleting one
   form does NOT touch its siblings. Owner explicitly chose independent-after-creation over
   permanently-linked, understanding a word could show a mid-recolour as intentionally mixed colours.
   **Confirmed 2026-07-02.**
7. **Bulk "whole letter" convenience — BOTH triggers, exact UX still open.** On top of #6's
   per-form-independent tooling, there must ALSO be a way to act on all 4 forms of one letter+colour
   at once on purpose (bulk recolour, bulk delete). Owner wants **both** a sneak+Triangle/Square
   modifier (mirrors how sneak already means something different depending on context elsewhere in
   the mod) AND a `/cb` command form. **The precise command/UX shape is still open — owner wants a
   dedicated follow-up discussion to "revamp the entire `/cb arabic` command section," not bundled
   into this build.** Until that discussion happens, the give/place/browse command surface (`/cb
   give <letter>`, the held-item NBT-stamp flow, etc.) stays functionally equivalent to today from
   the player's perspective — the underlying data becomes real slots, but do not redesign the command
   UX as part of this build. That redesign is separate, future, out of scope here.
8. **Slot cap raised to 1448** (`CustomBlocksConfig.maxSlots`), as part of this plan — comfortable
   room for the 164 pre-baked base forms (retiring the 224 static ones frees more than that costs)
   plus a large amount of future colour-variant headroom, well under the 8192 hard ceiling.
9. **No migration needed.** This is a dev/test world — old placed letters (the current live-render
   `ArabicLetterBlock`) can just be broken and re-placed by hand once the new system ships. No
   migration sweeper required.
10. **This is one big rebuild, not a phased small-step roadmap** — explicitly NOT split into several
    small Fable prompts like the rest of Group 13. It goes to a more capable execution agent
    ("Fable max ultracode") as ONE exhaustive, self-contained build spec, written to be buildable in
    one sitting with internal checkpoints still gated on in-game confirmation (CLAUDE.md's Golden
    Rule doesn't relax just because the executor is more capable). The spec lives at
    `docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md` (filename kept from the earlier, smaller,
    now-superseded plan — content is completely rewritten).

### Technical grounding (confirmed in code this session, de-risks the plan)

- **Texture generation is already colour-parameterized and PNG-based, not a mystery live-GPU
  process.** `arabic/ArabicTileRenderer.render(char letter, int form, int fgArgb, int bgArgb)`
  already produces PNG bytes for ANY (letter, form, colour) combination — it's what the CURRENT
  live renderer calls every time it needs a tile (`client/render/ArabicLetterBlockEntityRenderer
  .build()`). Baking a real slot's texture is calling this exact function once (server-side, pure
  Java2D/AWT, no GPU needed) and saving the result via `TextureStore.save(index, png)` — the same
  function the live system already trusts for pixel-perfect output. Foreground is always
  `WHITE` (`0xFFFFFFFF`); today's 4 bundled backgrounds are black `0xFF0A0A0A` (NOT pure
  `0x000000` — note the exact value for zero-regression), red `0xFFFF0000`, green `0xFF1E8C1E`,
  yellow `0xFFF0C814` (`bgArgb()` in that same renderer file). A new colour variant is just calling
  `render()` again with a different `bgArgb`.
- **Block-swap auto-join is already a proven, shipped pattern.** `core/ColorVariantService
  .swapPlaced()` already does `world.setBlockState(pos, block.getDefaultState()...)` live, on a
  placed block, in response to a player action (Square recolour) — with no flicker, already
  confirmed working in-game. The new join-flow reuses this exact mechanism instead of the old
  `ArabicLetterBlockEntity.setForm()` field write, triggered automatically by `ArabicJoinFlow`
  instead of by a player click.
- **Naming already has a locked scheme (ADR-006) — reuse it directly, do not invent a new one.**
  `arabic/ArabicNaming.displayName(letter, color, form)` / `.virtualId(...)` already produce
  "Jeem Black" / "Jeem_Black_Mid" etc. for every (letter, colour, form) combination. Use these as
  the real slot's `SlotData.displayName()` / `customId()` directly. Numbers reuse the existing
  `ArabicArt.blockId(g, color)` / `.displayName(g, color)` scheme (no form suffix — numbers have
  only one form).
- ~~**New block class needed.**~~ **SUPERSEDED 2026-07-03 — this bullet caused the CP1 partition
  regression.** A subclass with a `FACING` blockstate forces a per-index class boundary between
  client and server, which broke the owner's maxSlots=2000 world. The locked design is DATA-ONLY:
  plain `SlotBlock` + `ArabicMeta` on `SlotData`; facing in the shared slot BlockEntity NBT (the
  off-atlas renderer reads it); join re-flow triggered without block-class hooks (CP3 wires it via
  the placement/break paths that already exist for every slot block). Do NOT reintroduce a subclass.

### Explicitly OUT of scope for this build

- **The `/cb arabic` give/browse/command UX redesign** (decision 7) — deferred to its own future
  conversation. Keep today's command shape working, backed by real slots underneath.
- **Text Blocks** (G13-21/22, "Arabic word" rebrand) — a completely separate baked-phrase system,
  untouched by this rearchitecture.
- **G13-9** (hide/manage the 224 bundled letters) — cancelled outright by decision 3 (retirement),
  not deferred.
- **G13-10** (type-a-word auto-build) — depends on this rebuild landing first (it places real
  join-letters); revisit its prompt once this ships. See `FABLE_PROMPT_PHASE3_HIDE_AND_AUTOBUILD.md`
  for the correction notice.

| Group | Scope | Status |
|---|---|---|
| G13 — Arabic | Full rearchitecture — new real-slot block class + pre-bake pipeline + join-flow swap + retire old system | 📝 designed 2026-07-02 (extensive discussion, locked), nothing built. Full build spec: `docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md` |

**Related:** G13-19 (Deleter regression this absorbs and properly fixes) · G13-23 (attribute gap this
absorbs) · G13-9 (cancelled by this) · G13-10 (blocked on this landing first) · ADR-005 (the "keep
letters a separate render path" decision this partially reverses — the render path is still separate
in the sense of being pre-baked art rather than a URL upload, but it is no longer separate from the
SlotBlock/SlotData/tooling system) · `core/ColorVariantService.swapPlaced` (the proven swap mechanism
being reused) · `arabic/ArabicTileRenderer.render` (the proven texture-generation function being
reused) · `arabic/ArabicNaming` (ADR-006 naming scheme being reused)
**Verify in-game:** the full spec's internal checkpoints (see the Fable prompt file); final check —
every existing G13 passed test (testing guide §B/E/F/G/H/I/J/K/L) still passes unchanged, PLUS
Deleter/Triangle/Square now work on letters with zero special-casing.

<details><summary>🗄️ Archive: the earlier "settings sheet" design (2026-07-02, superseded same day)</summary>

The first pass at this problem proposed a parallel `ArabicLetterStyle` settings-sheet layer that kept
`ArabicLetterBlock` as a separate live-rendered, non-`SlotBlock` system (explicitly ruling out real
SlotBlocks). Steps 1 (sheet + storage) and 2 (Deleter wired through it) were actually built and
build-green, then reverted the same session once the owner clarified the real ask was full real-slot
parity, not a parity *layer*. No trace of that design should be treated as current — this whole
section (the rewrite above) is the only current design.

</details>
