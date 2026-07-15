# Fable prompt — Phase 3 of 4: type-a-word auto-build

> ⚠️ **CORRECTED 2026-07-02.** Two changes from the original version of this prompt:
> 1. **Part A (G13-9, hide/manage the 224 bundled letters) is CANCELLED, not just deferred.** Those
>    224 bundled static blocks are retired entirely by the Phase 1 rebuild
>    (`docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md`, §G13-25 decision 3) — a real join-letter
>    now does everything they did, plus joins. There is nothing left to hide. Do not build Part A.
> 2. **Part B (G13-10) now depends on Phase 1 having shipped first.** It originally said "reuses the
>    existing single auto-join letter block, don't touch the 1028 normal slot blocks or use up any
>    of their slots" — that's no longer true. After Phase 1, join-letters ARE real slot blocks, so
>    building a word necessarily places real slot blocks (Phase 1 raised the slot cap to 1448
>    specifically to make room for this). Do not run this prompt until Phase 1 is built AND
>    confirmed in-game.

> Paste everything below the line into a new chat.

---

Read `CLAUDE.md` at the repo root first — those are the hard rules for this project (small steps,
never mark something done without my in-game confirmation, never batch more than a few items).

**Before you build anything:** confirm Phase 1 (`docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md`)
is actually built and in-game confirmed — check `PROGRESS_LOG.md`'s top entries and
`docs/testing/GROUP_13_TESTING_GUIDE.md` §A. If it isn't, stop and tell me instead of building on
top of something unfinished.

Then read `docs/groups/GROUP_13_ARABIC.md` → search for `G13-10` (type-a-word auto-build). It's
fully designed already. Do not redesign it — if something in the design looks wrong once you're in
the code (especially anything that assumed the OLD non-slotblock letter system), ask me, don't
improvise around it.

## G13-10: type-a-word auto-build

Type or paste a word, the connected letter blocks place themselves in the world — no placing letter
by letter. Reuses the real slot-block join-letters and join logic Phase 1 built; this is purely
about *placement*, not a new letter system.

Build in this order, jar + in-game confirm after each step:
1. Core build: `/cb arabic build <word>` places a right-to-left row of joining letters on the ground
   in front of the player (where they're looking, or ~2 blocks ahead if aiming at nothing/sky). One
   colour, picked before building.
2. Spaces + numbers: a space in the typed word leaves one empty block (ends that word's join run,
   next word starts fresh past the gap). A digit places inline as a non-joining number block; the
   first time a word has a digit, ask which number style — Eastern Arabic-Indic or Western digits.
3. Collision handling: if the row runs into an existing block, stop before that block and tell the
   player how many letters got placed. Never overwrite an existing block.
4. Undo: the whole built word undoes as one step, and undo survives a relog/restart (it needs its
   own small persisted record of what was placed — the existing core undo system tracks a different
   kind of data and doesn't fit here, don't force it to). Undo only removes blocks that are still
   there and unchanged — leave alone anything the player broke or replaced by hand since.
5. GUI: a text-box screen (reuse the existing anvil-style text prompt) plus a colour picker (reuse
   the existing Color Studio flow), including the option to set each letter's colour individually
   instead of one colour for the whole word.

**Do NOT:**
- Register any NEW slot blocks for this — every letter/number slot this places should already exist
  from Phase 1's pre-bake (base forms) or its on-demand colour-creation path (non-default colours).
  This phase only calls placement/join code, it doesn't add to the real-slot catalogue.
- Let a build run forever — put a sane maximum word length on it.
- Move on to a different phase of Group 13.
- Redesign the `/cb arabic` give/browse/command surface — that's explicitly deferred to its own
  future conversation (see Phase 1's spec). Use whatever give/place mechanism Phase 1 actually built.

Build with `./gradlew.bat build --no-daemon` (needs `JAVA_HOME` set to the JDK 21 path — see
`CLAUDE.md` §6). Green build only means it compiles — never tell me something is done until I've
tested it in-game and said so myself.
