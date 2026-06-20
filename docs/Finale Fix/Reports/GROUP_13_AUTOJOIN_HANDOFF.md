# Group 13 — Arabic Auto-Join — HANDOFF (updated 2026-06-19 — auto-join COMPLETE; ▶ RETIRE STATIC LETTERS in progress)

> Self-contained so a fresh session (any machine) can continue. Plain-English first, tech below.
> Project rules live in `CLAUDE.md` (read it). Dev is a non-coder — keep replies short, plain, one
> decision at a time, show preview images not walls of text. Golden Rule: nothing is DONE until the dev
> confirms it in-game.

---

## ▶ ACTIVE — CONTINUE HERE: Retire static letters → ONE system (Build B)

> **Read this first.** Owner is moving to a new chat mid-task and wants the SAME work continued (he's
> partnered with me on this — pick up exactly here, don't restart the design). Build env: JDK 21, see §6.

**The goal (owner, 2026-06-19, fully locked via Q&A):** auto-join is now his dream system, so **delete the
144 static letter blocks** and keep auto-join as the ONLY letter system. This resolves the old "Issue 3
dedupe." Locked decisions:
- **KEEP** the 80 NUMBER blocks (Eastern A0–A9 + Western E0–E9 ×4 colours) — they never join, no auto-join
  equivalent. Numbers stay as static slot blocks, untouched.
- **DELETE** the 144 static letter blocks (36 letters ×4 colours).
- **Command:** `/cb arabic letter` is the ONE surviving command (gives auto-join). `/cb arabic join`
  removed as a dupe. Done in Build A.
- **Reclaim** the ~144 freed slots back into the pool.
- **Placed static letters → vanish (air)** as their slots are reclaimed (owner doesn't need them; do NOT
  build the swap-to-auto-join path he originally picked — he revised to "don't need them").
- **Strip** the 144 letter PNGs from the jar (owner has copies on his desktop). KEEP the number PNGs.

**Critical architecture fact (must respect):** placed world blocks are the registry `slot_N` blocks,
textured **by slot index** (`SlotManager.reId` comment, ~line 139). Freeing + reusing a letter's slot index
would make any placed copy show the *new* block's texture (a wrong-block bug). So placed letters must be
air-cleaned BEFORE/as their indices are reclaimed.

### ✅ Build A — DONE + confirmed in-game 2026-06-19 (safe half, no world/slot risk)
- `/cb arabic letter <name> [color] [count]` → auto-join block. `ArabicCommands.giveLetter` rewritten →
  `ArabicLetterBlock.stackFor(ch, color, -1, count)`. ✅ `letter jeem` confirmed.
- `/cb arabic join` removed (dupe); `giveJoin` folded into `giveLetter`.
- Creative tab "Arabic Letters (Join)" → **"Arabic Letters"** (`CustomBlocksMod.registerArabicJoinTab`). ✅ confirmed.
- `ArabicBlockRegistry.importArt` skips `Group.LETTER` (boot creates numbers only).
- Jar built + deployed 22:41. **Owner confirmed `letter jeem` + tab rename.** Still untested (optional):
  `letter jeem red 3` colour+count, `join` gone, numbers-still-work.

### ⏳ Build B — NEXT (destructive; this is what to build)
Owner already saw the leftover: the 144 OLD static letters still show in **creative search** ("the 5 on the
left") because Build A only stopped creating them — prior-boot copies linger in `slots.json`. Build B removes
them. Do in small in-game-confirmed steps:
1. **One-time boot migration** (idempotent, like `migrateDisplayNames`): find every existing SlotData whose
   id matches `arabic_<letter>_<colour>` (use `ArabicArt.ALL` LETTER glyphs × `ArabicArt.COLORS`,
   `ArabicArt.blockId`), record their slot indices into a **persisted "retired letter indices" set**, then
   `SlotManager.removeSilently` each (frees the slot + clears texture; numbers are never matched so they
   survive). *Confirm: old letters gone from creative search + `/cb arabic list`; numbers still there.*
2. **Placed-block air cleanup:** a chunk-load handler (ServerChunkEvents) that, for any `slot_N` in the
   retired set found in the chunk, replaces it with air. Handles unloaded chunks correctly (runs as they
   load). Keep the retired set persisted so the cleanup survives restarts until every chunk has loaded once.
   *Confirm: a previously-placed static letter is gone (air), no wrong-texture block appears.*
3. **Slot reclaim** falls out of step 1 (removeSilently frees the index; `nextFreeSlotIndex` reuses it).
   The retired-set air cleanup (step 2) is what makes reuse safe. *Confirm: `usedSlots` dropped ~144; a new
   `/cb create` works.*
4. **Strip the 144 letter PNGs** from `src/main/resources/assets/customblocks/arabic_art/<colour>/` (the
   36 letter files per colour; keep `a0..a9` + `num_0..num_9`). Shrinks the jar; nothing reads them now
   (auto-join is font-drawn — verified in `ArabicLetterBlockEntityRenderer.build`). *Confirm: build green,
   letters still render (font), numbers fine.*

After Build B: update testing guide §15 (mark Build B) + PROGRESS_LOG. Then the OTHER remaining Group 13
items (O8 Hide, O9 Type-a-word) are still open — see below.

---

## 1. Where we are

**Auto-join is DONE.** All of Round 3 (the 6 reported issues) is **confirmed in-game 2026-06-19**: join
count 16→1, middle-click pick returns the real letter, "Place letter blocks" gives joinable blocks, no
stray edge lines, OmniTool Arabic mode removed, and **readable-back (C-full)** reads the word correctly from
behind. Locked architecture still holds: one block + NBT virtual ids (**Way A**), all forms font-drawn,
direction-agnostic neighbour join, full-bright glyph, all colours join.

- **Readable-back fix (last one):** the back face was turned 180° **and** mirrored (`flipU=true`) — a double
  flip → backwards glyphs. Fix = drop the mirror (`drawBackFace` now `flipU=false`); the 180° turn alone
  keeps it readable, like spinning a sign around. Partner-letter swap in `ArabicJoinFlow` handles reading
  order. File: `client/render/ArabicLetterBlockEntityRenderer.java`.
- **Corners / 90° turns — SCRAPPED (dev, 2026-06-19).** Not building corner joining at all; rows stay
  straight, a turn just starts a fresh word. Do not re-raise.

### What's left in Group 13 (all design-only, not built)
- **O9 — Type-a-word auto-build → NEXT.** Design LOCKED 2026-06-19. Build plan in §2 below.
- **O8 — Hide / manage the 224 bundled letters.** Design locked (testing guide §13). Recoverable hide, not
  delete. After O9.
- **Issue 3 — dedupe "3 blacks / 2 colours".** Still PARTIAL/deferred. Three overlapping letter systems:
  (1) bundled art 224 (`<base>_<colour>` SlotBlocks), (2) the join block (NBT virtual ids), (3) legacy base
  28 (`letter.blockId()`, old `ArabicWordRenderer`, black-only, on disk if `importAll` ever ran). **Decide
  Safe vs Full with the dev first:** Safe = hide the dupes from search/tabs (zero slot risk, nothing
  deleted); Full = retire the old registrations + free slots (cleaner, can break placed old letters, touches
  the 1028-slot system). Round 3 is confirmed now, so this is unblocked whenever the dev wants it.

---

## 2. Build order — O9 Type-a-word (LOCKED 2026-06-19; do in order, in-game confirm between each)

Full spec: `GROUP_13_ARABIC.md` → **O9**. Test steps: testing guide → **§14**. Reuses the O3 join blocks +
`ArabicJoinFlow` — placing a row of join blocks auto-shapes itself, so O9 is mostly the *placement* logic.

**Locked decisions (dev, 2026-06-19):** trigger = GUI text box **and** `/cb arabic build <word>` · row
builds **on the ground in front of you, where you look** (~2 ahead if aiming at nothing), RTL · colour =
**one-colour-whole-word AND per-letter** (Color Studio path) · **space → air gap**, next word fresh ·
**digits → inline non-joining**, **ask each time** Eastern `A0–A9` (٣) vs Western `E0–E9` (3) ·
**collision → stop** before the occupied cell + report "placed N", never overwrite · **undo** = see wrinkle.

**Step 1 — Core build.** `/cb arabic build <word>`: map each char → join letter, place a row on the ground
from the looked-at block (or ~2 ahead), RTL, one colour. Let `ArabicJoinFlow` shape it. Reuse
`ArabicLetterMap`/`ArabicGlyphs` for char→letter and `ArabicCommands.giveJoin`'s block-creation path.
*Confirm:* `build بيت` → a connected RTL row appears, correct forms.

**Step 2 — Spaces + numbers.** Space → leave one empty cell (gap ends the run). Digit → place an inline
number block (non-joining); first digit in a word triggers the **Eastern vs Western** ask (small GUI/chat
prompt). *Confirm:* a word with a space splits into two words; a word with digits asks the style + places
them inline.

**Step 3 — Collision stop.** Before placing each cell, check it's empty; on the first occupied cell, stop
and message "placed N of M". *Confirm:* build into a wall → stops, nothing overwritten.

**Step 4 — Undo (the wrinkle, decided with dev).** Dev wants undo "like the others" **and** persisted across
restart. The core `core/UndoManager` does NOT fit: it tracks the 1028 `SlotData` catalog (not placed world
blocks), is in-memory, and clears on disconnect. So O9 gets its **own small saved word-build store** that
*behaves* like the others (every build recorded, capped stack, undo back through recent builds, one build =
one step) but is **persisted to disk** (survives relog/restart) and records **BlockPos lists** of placed
letters. `/cb arabic undo` removes only the built blocks **still in place + unchanged** (skip any the dev
broke/replaced by hand). *Confirm:* undo clears the last word in one step, works after restart, leaves
hand-edited blocks alone.

**Step 5 — GUI.** Text-box entry (reuse the anvil prompt) + colour pick (reuse the Color Studio path),
including the **per-letter** colour option. *Confirm:* type a word in the box → Build → same result; both
colour modes work.

Constraints unchanged (§3): zero-registration / zero-slot, never touch the 1028 `SlotBlock`s; small steps,
one confirmed before the next; a sane max word length so a build can't hang.

---

## 3. Hard constraints (dev, repeated)
- **Never touch the 1028 `SlotBlock`s or eat slots.** Everything new is zero-registration, zero-slot
  (NBT variants of the single `customblocks:arabic_letter`).
- Preview visual changes as PNGs and get approval **before** any jar.
- "No bugs or problems allowed" — small steps, one confirmed before the next.

---

## 4. The two letter blocks (context, already resolved)

| Block | How you get it | Joins? |
|---|---|---|
| **Bundled letters** (224 curated hand-art) | `/cb arabic letter <name>` or the browser | No — decoration. |
| **Joinable letters** (Pass 4) | `/cb arabic join <name> [count]` | Yes — auto-shapes from neighbours. |

10-second sanity check: `/cb arabic join jeem 3`, place the 3 in a row → they connect.

---

## 5. Key files (under `src/main/java/com/customblocks/`)
- **Renderer (brightness fix lives here):** `client/render/ArabicLetterBlockEntityRenderer.java`.
- **Glyph bitmap:** `arabic/ArabicTileRenderer.java` (connected forms) · bundled isolated art loaded
  from `assets/customblocks/arabic_art/<colour>/<base>_<colour>.png`.
- **Names today (to be replaced by the O6 scheme):** `block/ArabicLetterBlock.java` →
  `displayName(letter, color, lockedForm)` + `stackFor(...)` (currently bakes `CUSTOM_NAME`).
- **Form brain:** `arabic/ArabicJoining.java`. **Re-flow:** `block/ArabicJoinFlow.java`.
- **Placement / FACING (auto-inherit):** `block/ArabicLetterBlock.getPlacementState`.
- **BlockEntity:** `block/ArabicLetterBlockEntity.java` (letter + form + colour).
- **Commands:** `command/handlers/ArabicCommands.java` (`giveLetter` bundled vs `giveJoin` joinable).
- **Char↔art-name map:** `arabic/ArabicGlyphs.java`. **Letter list:** `arabic/ArabicLetterMap.java`.
- **Searchable join tab:** `CustomBlocksMod.registerArabicJoinTab`.
- **Omni-Tool direction (O5): REMOVED** (Round 3 R3.5) — `ArabicDirectionTool` + the AttackBlockCallback
  intercept + the `ARABIC` OmniTool mode are gone; placement auto-inherits facing instead.
- **Readable-back (R3.6):** back face = `drawBackFace` (180° turn, `flipU=false`); partner letter/form set
  by `ArabicJoinFlow.recomputeRun`, carried on `ArabicLetterBlockEntity` (`backLetter`/`backForm`).
- **O9 build (next) will touch:** `command/handlers/ArabicCommands` (new `build` subcommand + `undo`),
  reuse `ArabicLetterMap`/`ArabicGlyphs` + the `giveJoin` block-creation path; new word-build undo store
  (own file, persisted); GUI entry via the existing anvil prompt + Color Studio path.
- **Preview harness (throwaway):** `tools/render_preview/` → PNGs in `out/`.

---

## 6. Build & test (Windows)
```
# JDK 21 (NOT the PATH java, which is 8):
JAVA_HOME = C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
cd CustomBlocks-B
./gradlew.bat build --no-daemon         # gates: verifyFileSize / verifyMojibake / verifySound
# jar  -> build/libs/customblocks-1.0.0.jar
# copy -> %APPDATA%\.minecraft\mods\   then FULLY QUIT Minecraft to desktop and relaunch
```
Preview a glyph change without a jar:
```
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d tools/render_preview tools/render_preview/*.java
"$JAVA_HOME/bin/java"  -cp tools/render_preview <PreviewClass>   # -> out/*.png
```

---

## 7. Next step
Build **O9 Step 1** (core `/cb arabic build <word>` — place the RTL row on the ground in one colour, let
`ArabicJoinFlow` shape it), copy the jar, have the dev confirm in-game. Then Steps 2→5 in order. Nothing is
DONE until the dev confirms in-game.
