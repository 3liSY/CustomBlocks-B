# Fable prompt — Phase 2 of 4: numbers join like letters, retire the old static numbers

> ⚠️ **ABSORBED, 2026-07-02 — DO NOT RUN THIS PROMPT.** Numbers-joining is now built as PART OF
> Phase 1's rebuild (`docs/archive/FABLE_PROMPT_PHASE1_LETTER_SETTINGS.md`), not as a separate later
> phase — the owner folded it in once Phase 1 pivoted to a real-slotblock rearchitecture (same
> mechanism serves both letters and numbers, numbers are simpler since they never grow connecting
> bars). Run Phase 1's prompt instead; it retires the 80 old static number blocks as part of its own
> checkpoints. This file is kept only so nobody re-derives the same plan from scratch later.

> Paste everything below the line into a new chat.

---

Read `CLAUDE.md` at the repo root first — those are the hard rules for this project (small steps,
never mark something done without my in-game confirmation, never batch more than a few items).

Then read the design section in `docs/groups/GROUP_13_ARABIC.md` → search for `G13-20`.

**Before you build anything:** that design section's "step 1" (fixing render bugs — ghost faces on
top/bottom, stale connecting bars on break, back-face mirror) reads as NOT done in the doc, but I
believe it actually already IS done through later work that isn't reflected there — the letter-seam
fix and the readable-back-face are both confirmed passed in
`docs/testing/GROUP_13_TESTING_GUIDE.md` (sections A1/A2 and E). **Check the current code yourself**
(`client/render/ArabicLetterBlockEntityRenderer.java` and `block/ArabicLetterBlock.onStateReplaced`)
against what the design doc's "Bug 1/2/3" describe, and against the testing guide's passed sections,
before deciding whether step 1 needs any work at all. Tell me what you find before touching code —
don't assume the design doc is current, and don't assume it's stale either. Verify.

**The actual goal (once step 1 is confirmed either way):** today, numbers are separate static blocks
that never join together. Letters auto-join into words. Make numbers join the same way letters do —
except numbers always stay in their own separate (isolated) shape, no connecting bars between them
(that's linguistically correct, don't add bars to numbers). Once that works and I've confirmed it
in-game, remove the old static number blocks (80 of them) since they're replaced.

**Do this in order, one step at a time. Build the jar and tell me to test in-game after EACH step:**

1. (See "before you build anything" above — confirm step 1's real status first.)
2. Extend auto-join to numbers — same block family as the joining letters, but numbers never grow
   connecting bars, always isolated form.
3. ONLY after step 2 is confirmed working in-game: retire the 80 old static number blocks
   (`arabic/ArabicLetterRetirement.java` already does this for the old static letters — extend the
   same mechanism to numbers, don't build a new one).
4. Remove the old legacy number-creation code paths (`ArabicBlockRegistry.importAll` /
   `importLetter` — check what's actually still called before deleting anything).

**Do NOT:**
- Retire the static numbers before their auto-join replacement exists and is confirmed working —
  hard sequencing rule, never skip it.
- Touch the letter joining logic itself, the settings-sheet work (that's a different phase), or
  anything already confirmed passing in the testing guide.
- Move on to a different phase of Group 13.

Build with `./gradlew.bat build --no-daemon` (needs `JAVA_HOME` set to the JDK 21 path — see
`CLAUDE.md` §6). Green build only means it compiles — never tell me something is done until I've
tested it in-game and said so myself.
