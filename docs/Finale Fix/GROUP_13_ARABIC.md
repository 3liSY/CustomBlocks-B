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
| Auto-joining | Automatic on block placement (no command) |

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

Arabic letter blocks connect horizontally, right-to-left only. Rules:
- When a letter block is placed, scan its immediate east and west neighbors.
- If a neighbor is also an Arabic letter block from the same script, determine contextual form:
  - **Isolated**: no neighbors.
  - **Initial**: neighbor to the left (west) only.
  - **Final**: neighbor to the right (east) only.
  - **Medial**: neighbors on both sides.
- Swap the placed block's texture to the correct contextual form.
- Also re-evaluate the neighbor blocks' forms.
- **No diagonals, no vertical connections, no T-junction logic.**
- Implemented as a block placement event listener.

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

Place a single Alef letter block in the world with no neighbors.

**Expected:** Block renders with the Alef **isolated** form.

**Pass:** Isolated form rendered.
**Fail:** Wrong contextual form.

---

## Test G13.6 — Auto-joining: final form

Place a Ba letter block directly to the east (right) of the Alef.

**Expected:**
- Ba renders as **final** form (has connection on its right).
- Alef re-evaluates: renders as **initial** form (has connection on its left).

**Pass:** Both letters show correct contextual forms.
**Fail:** Forms don't update, or wrong forms shown.

---

## Test G13.7 — Auto-joining: medial form

Place a Ta letter block directly to the west (left) of the Alef from G13.6. The sequence is now: Ba — Alef — Ta.

**Expected:**
- Alef (center) renders as **medial** form.
- Ba (rightmost) remains final.
- Ta (leftmost) renders as initial.

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

## Group 13 Verdict

| Test | Description | Result |
|---|---|---|
| G13.1 | arabtype.ttf auto-extracted on boot | ⬜ |
| G13.2 | Letter rendering uses correct Arabic font | ⬜ |
| G13.3 | Word command opens anvil GUI | ⬜ |
| G13.4 | Word → single texture block | ⬜ |
| G13.5 | Isolated letter form correct | ⬜ |
| G13.6 | Two adjacent letters: final + initial forms | ⬜ |
| G13.7 | Three letters: initial + medial + final | ⬜ |
| G13.8 | Diagonal placement does not join | ⬜ |
| G13.9 | English text uses Rockwell Condensed | ⬜ |
| G13.10 | Arabic browser opens as chest GUI | ⬜ |

**Group 13 passes when all Arabic rendering, auto-joining, and font bundling work in-game.**

If anything shows ❌ — paste:
1. The exact command and what appeared
2. Screenshot of the block texture if possible
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb arabic list
```
(Delete individual letter blocks if needed — they have auto-generated IDs like `arabic_alef`, etc.)
