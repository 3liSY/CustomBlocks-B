# Fable prompt — Phase 4 of 4: Text Blocks full upgrade + Studio tab

> **Note added 2026-07-02:** Phase 1 of this roadmap was rewritten into a full real-slotblock
> rearchitecture for individual join-letters/numbers (see `docs/archive/
> FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md`). Text Blocks (this phase) are a separate, already-baked
> whole-phrase system — unaffected by that rebuild. No change needed to this prompt.

> Paste everything below the line into a new chat.

---

Read `CLAUDE.md` at the repo root first — those are the hard rules for this project (small steps,
never mark something done without my in-game confirmation, never batch more than a few items — this
phase especially, it's the biggest of the four, so follow the step order strictly).

Then read these design sections in `docs/groups/GROUP_13_ARABIC.md`:
- Search for `G13-21` — Text Blocks (the system/back-end upgrade).
- Search for `G13-22` — the Studio tab (the front-end screen for it).
- Also note: the "Open questions" under G13-21 are marked RESOLVED — read those resolutions, they
  are locked decisions, not open for re-discussion.

**What this is:** today, "Arabic word" blocks are made once and can never be edited again — to
change the text you have to remake the whole block from scratch. This upgrade turns them into a
general "Text Blocks" system (Arabic AND Latin/English, words and numbers) that can be reopened and
retyped/restyled anytime, with much deeper styling options, through a new dedicated screen.

## Locked decisions (do not re-litigate these)

- **Rename:** the system becomes "Text Blocks", not "Arabic word". Keep `/cb arabic word` working as
  an alias to the same thing — don't remove it, just stop treating it as the primary name.
- **Re-editable is the core feature:** persist the actual typed text + colours + style on the block
  itself (not just the finished picture) so it can be reopened and changed later.
- **Old blocks made before this existed have no saved text** — only the picture survives. Their Edit
  button stays LOCKED until the player retypes the text once through the normal flow; from that
  point on it remembers and behaves like every other Text Block. Do NOT try to guess/reconstruct
  their old text from the block's saved name — that was explicitly rejected, a wrong guess with no
  way for the player to notice is worse than just asking them to retype once.
- **Styling scope is the FULL set** — outline colour, font choice, size/spacing, gradient, shadow,
  border. All of it is in scope for this phase. But build and get each one confirmed in-game
  ONE AT A TIME — do not build all six styling features in one batch before testing any of them.
- **Layout:** both multi-line text within one block, AND a phrase spanning multiple blocks, fully
  configurable. The multi-block spanning case reuses the seam fix that's already shipped and
  confirmed for the letter blocks (the flush-to-the-edge rendering) — don't rebuild that, just reuse
  it.
- **Background** should ride the same general background-colour system normal blocks already use,
  not stay a one-off baked-in colour like it is today.

## Build order — follow exactly, jar + in-game confirm after EACH numbered step

1. **Persistence first.** Add the small data record that stores a block's source text + colours +
   style (mirror how animated blocks already store their own data — same pattern, own file). Nothing
   visible changes yet; this just needs to compile and gate-pass.
2. **Re-edit flow using existing UI.** Wire "reopen an existing Text Block → see its stored text →
   change it → re-render the same block" using the current maker flow (no new screen yet). Include
   the legacy-locked-until-retyped behaviour for old blocks from before this step existed.
3. **Rename surface.** `/cb arabic word` becomes an alias; the primary name and any labels become
   "Text Block" / "Text Blocks".
4. **Styling, one axis at a time** (confirm each before starting the next): outline colour → font
   choice → size/spacing → gradient → shadow → border.
5. **Layout.** Multi-line within one block, then a phrase spanning multiple blocks (reusing the
   already-shipped seam fix — verify it actually does look seamless before considering this done).
6. **Background axis.** Make a Text Block's background use the shared background system instead of
   a baked-in colour.
7. **Studio tab (G13-22).** Only after steps 1-6 are confirmed: build the new screen — a "Text" tab
   inside the existing block-creation Studio screen, live preview at the top, text box, mode toggle
   (word block / loose letters / numbers), colour pickers, count field for loose letters, Create
   button. This replaces the old chest-menu flow; retire that flow only once the new tab is
   confirmed working.

**Do NOT:**
- Batch multiple styling features into one build before testing any of them — this is explicitly
  the part of this phase most likely to go wrong if rushed.
- Guess a migration story for old blocks other than the locked one above.
- Move on to any other Arabic feature — this prompt is Text Blocks and its Studio tab only.

Build with `./gradlew.bat build --no-daemon` (needs `JAVA_HOME` set to the JDK 21 path — see
`CLAUDE.md` §6). Green build only means it compiles — never tell me something is done until I've
tested it in-game and said so myself.
