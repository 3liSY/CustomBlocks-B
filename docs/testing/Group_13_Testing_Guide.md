# Group 13 — Arabic System

**🎯 Active:** O3 — Arabic tab shows checkerboard blocks + a stray real block. Leading theory: stale resource pack (client didn't get the new textures). Owner testing `/cb reload` + relog next.  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                |
| --------------- | ------------------------------ |
| **Verdict**     | 🛑 O3 broken tab — owner to test `/cb reload` + relog, report back · §B/E/F/G/H/I/J/K/L + A1/A2 reverted, stale (pre-07-03) |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 17% (13/76) |
| **Last tested** | 2026-07-04                     |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): ArabicHubMenu/ArabicListMenu/ArabicGroupMenu/WordChoiceMenu/ColorStudioMenu → one Arabic Screen, not built (+ a Colour Variants button, professionally labeled). See `GROUP_13_ARABIC.md`.

## 🗺️ Sections

| § | What                                            | State        |
| --- | ----------------------------------------------- | ------------ |
| O | G13-25 CP4 colours + CP5 old system removed + Arabic tab | 🎯 test now |
| N | G13-25 CP1-fix + CP2 facing + CP3 auto-join (real letter slots) | 🟡 mostly ✅ (N6 partial) |
| M | Colour workflow + live preview                  | 🟡 partial    |
| B | Auto-join flicker on recolour/placement         | 🎯            |
| E | Readable back (two-faced sign)                  | 🎯            |
| F | Auto-join engine (shapes/faces/bright/flash)    | 🎯            |
| G | Recolour placed letter w/ Squares               | 🎯            |
| H | Placement lag + transparent-flash fix           | 🎯            |
| I | Retire static letters → one system              | 🎯            |
| J | Round-3 fixes (count/pick/place)                | 🎯            |
| K | Naming + live form labels                       | 🎯            |
| L | Font + stroke + browser                         | 🎯            |
| A | Deleter on Arabic + seam/gap (shared `CbBlock`) | 🗑️ obsolete (old block removed; new-letter Deleter = N8 ✅) |
| C | Hide / manage letters                           | ⏳ planned    |
| D | Type-a-word auto-build                          | ⏳ planned    |

🔗 **Related Doc:** [GROUP_13_ARABIC.md](../groups/GROUP_13_ARABIC.md)

---

# 🎯 Test now

## O · G13-25 CP4 colours + CP5 one-system + Arabic tab · 🎯

> 💡 **What this build does (2026-07-04, 🟢 build green — NOT in-game yet).** Three things:
> - **Old system NUKED (CP5, = N10):** the whole `customblocks:arabic_letter` NBT block
>   (block/BlockEntity/renderers/join flow/prewarm/recolour predictor/assets) is deleted, and the
>   last static bundled art blocks (the 80 numbers) are retired at boot like the static letters
>   were. Placed OLD letters/numbers VANISH (owner-ruled: air-clean, no migration).
>   `/cb arabic import` is gone too. ONE letter system remains: the real slot blocks.
> - **"Arabic Letters" creative tab repurposed:** it now lists the REAL Arabic slot blocks (every
>   letter form + number, every colour) instead of the old NBT letters. Search still works.
> - **All 4 colours pre-baked (CP4):** every letter form + number now exists in black + red +
>   green + yellow = **656 base slots**, coloured with the EXACT config triangle hexes (not the
>   old bundled-art colours). Letters font-baked; numbers keep the hand-art look with the
>   background recoloured. Changing a config hex re-bakes that coloured set once on next boot.
>   A Square colour-swap on a placed letter keeps its facing/word (BE data is re-stamped).
> 🧰 rebuild jar, boot on the test world; Squares/Triangles; `/cb arabic letter <name> [color]`.
> ⚠️ First boot after this build is SLOW-ish once: 492 coloured slots bake + old numbers retire.

| #  | Action                                                     | Expected Result                                                                | SP | MP |
| --- | ---------------------------------------------------------- | ------------------------------------------------------------------------------ | ----- | ------ |
| O1 | First boot — read the log                                  | `retired 80 static Arabic art block(s)` (once) and `bootstrap (656): 492 created, 164 skipped, 0 failed` | 🎯 | ✅ |
| O2 | Restart — read the log again                               | `bootstrap (656): 0 created, 656 skipped, 0 re-baked, 0 failed`                 | 🎯 | ❓ |
| O3 | Open the "Arabic Letters" creative tab                     | real slot blocks (letters all 4 forms + numbers, 4 colours); icon = black jeem; NO old-style entries | 🎯 | 💔 |
| O4 | Creative search "jeem" / "Red"                             | finds the same new blocks the tab shows                                         | 🎯 | ✅ |
| O5 | A world/area that had OLD placed letters or static numbers | they are GONE (air) — expected, owner-ruled; log shows the air-clean            | 🎯 | ✅ |
| O6 | `/cb arabic letter dal red 3` + place them right-to-left   | gives 3 RED real dal blocks; they join instantly, stay red                      | 🎯 | ⚠️ |
| O7 | Mixed word: red letter next to black letters               | they join into one word; each letter KEEPS its own colour                       | 🎯 | ✅ |
| O8 | Red Square on a placed black letter inside a word          | swaps to the red variant IN PLACE — stays joined, facing/back unchanged         | 🎯 | ⚠️ |
| O9 | Red Triangle on a placed letter                            | "already exists — here's one" (hands over the pre-baked red variant)            | 🎯 | ✅ |
| O10 | Word maker → "Place letter blocks"                        | gives NEW slot letters (place RTL → join)                                       | 🎯 | 🎯 |
| O11 | Numbers: `A0`/`E5` red from the tab                       | hand-art digit look, background = the config red hex                            | 🎯 | 🎯 |
| O12 | Change `triangleredhex` in config → restart               | log shows the red set re-baking once; red letters wear the new hex              | 🎯 | ⏭️ |

> ⚠️ **Known gap (deliberate, still open):** a CUSTOM-hex tool on a letter creates a plain
> non-joining colour variant (no ArabicMeta, no sibling forms) — the decision-6 "sibling-linked
> custom colour creation" is NOT in this build. The 4 preset colours are fully wired.
>
> ✅/⚠️/💔 **owner results, MP, 2026-07-04:** O4/O5/O7/O9 ✅. **O1 ✅** — owner pasted his boot log,
> exact match: `bootstrap (656): 492 created, 164 skipped, 0 failed` + the retire line, both present,
> both correct. **O3 💔 — real, confirmed by owner (2nd look): tab shows checkerboard blocks AND one
> real unrelated block ("Custom Block" / `slot_1113`, 6 components) that isn't Arabic at all.**
> **O6 ⚠️** — two different findings folded into one mark: `dal` genuinely never visually joins
> another `dal` (see note below — expected Arabic behaviour, not new-system-specific, unchanged
> code); separately `ba` DOES join but "a lil buggy" — THAT part is real and open, likely same root
> as N6 (timing/re-flow, not colour-specific). **O8 ⚠️** — swap-in-word works but buggy, open (new
> CP4 code — `ColorVariantService.swapPlaced` + `ArabicSlotJoinFlow.reflowAfterSwap`, prime
> suspect). **O2 ❓** — needs a SECOND boot's log (restart, not first boot) — not tested yet.
> **O10/O11** — owner unsure what these test; explained in chat, re-test when convenient.
> **O12 ⏭️ deliberately skipped** — owner didn't want the risk; deferred, safe to try whenever
> (a config value + restart, easily reverted).
>
> 🔍 **O3 note — dal vs ba is a real Arabic rule, not a bug.** `ArabicJoining.java` (unchanged this
> session) classes 13 letters — alef, dal, dhal, ra, zay, waw + the alef/hamza family — as
> RIGHT-only: they accept a join from the letter before them, but NEVER extend a join to the
> letter after them. Two `dal`s side by side legitimately render ISOLATED, both of them, always —
> same table the OLD system used. Re-test by placing a DUAL letter (ba/jeem/lam/etc.) then a dal
> after it (e.g. "lam dal") — the lam should show a FINAL-side connection into the dal. If that
> ALSO fails to connect, that's a real bug; if it works, `dal` was never broken.
>
> 🔍 **O3 root-cause theory (2026-07-04, unconfirmed) — stale resource pack, not a data bug.** Read
> `registerArabicJoinTab()` (`CustomBlocksMod.java`) — its filter (`if (!d.isArabic()) continue;`)
> is correct; a non-Arabic block cannot get past it in the code as written. The boot log shows the
> bake itself is clean (0 failed). That points at the RESOURCE PACK the client is using being out of
> date: the checkerboard look and the generic "Custom Block" / N-component tooltip are the same
> fallback look normal unassigned slots get when their model/name never reached the client — this is
> the known "long pack debounce → purple blocks" pitfall already listed in `CLAUDE.md` §7, same
> family of bug, not new territory. **Owner's next test (no code change, just try it):**
> 1. In-game, type `/cb reload`.
> 2. Reopen the Arabic Letters tab — did the checkerboards (incl. slot_1113) fix themselves?
> 3. If not: fully quit the game, relaunch, rejoin the SAME world (forces a fresh pack download).
> 4. Check the tab again.
> 5. Report: fixed after step 2? only after step 4? still broken either way?
> That answer decides the real fix — no new code until it comes back.
>
> 🚦 **Gate:** blocked on O3 root cause confirmation (owner's reload/relog test) before calling
> CP5/CP4 confirmed. O6(ba)/O8 buggy-but-passing get investigated together with N6 next (likely one
> shared root).

---

## N · G13-25 CP1-fix + CP2 facing + CP3 auto-join + CP3b instant — real letter slots · 🟡

> 💡 **What this build does.** The 164 base slots (CP1, in-game 2026-07-03: blocks existed ✅ but
> isolated letters wore the WRONG art — the hand-art static look instead of the join-system font
> look) get three things at once:
> - **CP1 fix** — all 4 letter forms are now font-baked (isolated included), matching the old join
>   letters exactly; existing textures self-heal on the first boot (one-time re-bake of all 164).
> - **CP2** — a placed letter faces you (furnace convention / joins a neighbour's word axis) and is
>   readable from BEHIND (back face shows the word's mirror partner). Facing lives in the block's
>   own data, never a blockstate — existing blocks untouched by construction.
> - **CP3** — auto-join is LIVE: placing/breaking letters swaps them between their form-siblings
>   (the proven Square-swap mechanism). New letters join NEW letters; the old system still joins
>   old letters; the two never join each other (old system retires CP5).
> - **CP3b** (🟢 built 2026-07-03, the MP-sweep fix) — the join now happens on YOUR screen the
>   same tick you place/break (the client predicts the exact same result the server computes,
>   like the old system did), and letters are ALWAYS full-bright — no more grey/dim letters, no
>   more missing back mirror while waiting on the server, no more slow joins.
> ⚠️ Numbers stay plain no-facing cubes (correct — they never join). Colours = CP4 (next).
> 🧰 rebuild jar, boot; `/cb give arabic_jeem_iso` etc.; an old-system letter for comparison.
> ⚠️ For N5/N6/N8 use NEW-system letters only (`/cb give arabic_dal_iso`, `arabic_ha_iso`,
> `arabic_lam_iso`, `arabic_meem_iso` — or creative-search "Black" in the Blocks tab). The old
> "Arabic Letters" tab gives OLD-system blocks that never join with the new ones.

| #  | Action                                                     | Expected Result                                                                | SP | MP |
| --- | ---------------------------------------------------------- | ------------------------------------------------------------------------------ | ----- | ------ |
| N1 | First boot — read the log                                  | `bootstrap: 0 created, 0 skipped, 164 re-baked, 0 failed`                       | 🎯 | ✅ |
| N2 | Restart — read the log again                               | `0 created, 164 skipped, 0 re-baked` — heal runs once                           | 🎯 | ✅ |
| N3 | Place `arabic_jeem_iso` alone, old join-jeem beside it     | IDENTICAL look (font glyph, not hand-art); faces you when placed                | 🎯 | ✅ |
| N4 | Walk behind a placed new letter                            | readable from the back (not mirrored)                                           | 🎯 | ✅ |
| N5 | Place 3 new letters in a row (e.g. `/cb give arabic_dal_iso`) | joins INSTANTLY as you place — bars connect, word identical to the old system's | 🎯 | ✅ |
| N6 | Break the middle letter of that word                       | survivors re-flow instantly; back faces still mirror across the gap             | 🎯 | ⚠️ |
| N7 | Two words placed crossing directions                       | only same-direction letters join (perpendicular words stay separate)            | 🎯 | ✅ |
| N8 | Deleter on a placed NEW letter (a `/cb give arabic_…` one)  | DEFINITION deleted (all its placements → purple markers), word re-flows; next boot re-creates the base slot | 🎯 | ✅ |
| N9 | Normal blocks: place/recolour/delete + one animated GIF block | all exactly as before — nothing regressed (renderer path was touched)        | 🎯 | ✅ |
| N10 | ~~OLD letters still join on their own~~                   | SUPERSEDED 2026-07-04: owner ruled "old system needs to be nuked" → done this build (see §O)     | —  | 🗑️ |

> ⚠️ **MP results 2026-07-03 (owner, screenshots) — one root cause for N4/N5/N6.** The letter's
> facing + back-partner data is computed on the SERVER only; the client has no prediction (the old
> system predicted client-side — that's what made it instant). Until the server's data arrives the
> client draws the plain unrotated cube at world brightness → the "dimmer/grey letter" (mixed
> bright/dim in one word), the missing back mirror, and the join feeling slow. One fix (CP3b,
> client prediction + brightness) covers all three.
> **N5/N8 "only jeem works":** the other letters were taken from the OLD "Arabic Letters" creative
> tab — those are OLD-system blocks and never join with (or delete like) the new ones. New letters
> come from `/cb give arabic_<letter>_iso` (dal, ha, lam, meem, …) or creative search "Black" in
> the Blocks tab. Old tab goes away when the old system retires (CP5).
> **N3 note:** owner elaborated 2026-07-03: ONE system survives (new slot blocks); old
> `arabic_letter` system + both old bundled sets removed entirely in CP5, placed old blocks vanish.
>
> ✅ **§N sweep results (owner, MP, 2026-07-04):**
> - **N4 ✅ / N8 ✅** — full pass.
> - **N5 ✅** — "passes like before", **BUT owner note:** *"needs a lil revamp and just upgrading
>   how it corresponds"* → open follow-up; owner explains exactly what corresponds wrong before
>   any code (too vague to build from yet).
> - **N6 ⚠️ partial** — works (correct re-flow) but **isn't instant**; the mid-break re-flow still
>   feels server-paced. Open item: make break re-flow as instant as placement.
> - **N10 →** owner: "old system needs to be nuked" → DONE in the 2026-07-04 build (§O tests it).

---

## A · Deleter on Arabic + seam/gap · 🗑️ obsolete 2026-07-04 (old block removed)

> 🗑️ **This section tested the OLD `arabic_letter` block, which was removed at CP5 (2026-07-04).**
> The A4 regression ("Deleter just bugs out on a letter") dies with it — on the real slot letters
> the Deleter is the NORMAL slot path and passed as **N8 ✅**. Kept for history only.

> 💡 Seam gap sealed (6 faces flush z=1.0) ✅. Deleter on Arabic letters (shared `CbBlock`) **regressed** — reopened, needs revamp.
> 🧰 `/cb deleter`; place a 3+ letter word (حمد) as a wall sign + a couple normal blocks; letter in hand.

| #  | Action                                   | Expected Result                            | SP | MP |
| --- | ---------------------------------------- | ------------------------------------------ | --- | --- |
| A1 | Walk around the word (above/below/sides) | solid cube every angle, no hairline gap    | 🎯 | 🎯 |
| A2 | Break the middle letter (م in حمد)       | neighbours re-shape instantly, bar gone    | 🎯 | 🎯 |

> ⚠️ **A1/A2 reverted to needs-testing 2026-07-05** — stale (were passed before the 07-03 confirm cutoff, needs retest).
| A3 | Look at a letter in hand/inventory       | still a solid letter cube                  | 🎯 | 🎯 |
| A4 | Deleter on a placed Arabic letter        | removed; chat `Deleted <letter>`           | ⚠️ | ⚠️ |
| A5 | Deleter on a middle letter of a word     | gone; neighbours re-flow, no stale bar     | 🎯 | 🎯 |
| A6 | Deleter on a normal/slot block           | wiped, `/cb undo` restores; `Deleted <id>` | 🎯 | 🎯 |
| A7 | Deleter on a locked slot block           | refused w/ lock message; not deleted       | 🎯 | 🎯 |
| A8 | Deleter on air / vanilla block           | nothing happens                            | 🎯 | 🎯 |

> ⚠️ **A4 (06-27):** Deleter on an Arabic letter — owner: *"just breaks in the world, doesn't get deleted as a definition, just bugs out"* (NOT: block stays / ghost left / chat message). Contradicts 06-26 N1–N3 ✅ → regressed since. Reopened — being rebuilt as part of the real-slotblock rearchitecture (see PROGRESS_LOG 2026-07-03). A5–A8 re-test after that build lands.

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | What it'll do                                     | Spec                  |
| --- | ------------------------------------------------- | --------------------- |
| C | Hide / manage letters (recoverable, Hidden tab)   | GROUP_13_ARABIC.md O8 |
| D | Type-a-word auto-build (GUI + `/cb arabic build`) | GROUP_13_ARABIC.md O9 |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **B · Auto-join flicker (place/recolour/re-flow)** (Passed 2026-06-27). Test history moved to [GROUP_13_ARABIC.md](GROUP_13_ARABIC.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · Readable back** (Passed 2026-06-26). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **F · Auto-join engine** (Passed 2026-06-19). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **G · Recolour placed letter w/ Squares** (Passed 2026-06-20). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **H · Placement lag / transparent-flash** (Passed 2026-06-20). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **I · Retire static letters → one system** (Passed 2026-06-20). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **J · Round-3** (Passed 2026-06-19). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **K · Naming + live form labels** (Passed 2026-06-19). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **L · Font + stroke + browser** (Passed 2026-06-19). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| N1 | N1 | CP1 fixed partition capped `/cb create` at index 800 → "every slot is in use" with ~900 free slots (owner world maxSlots=2000). | Reverted, awaiting in-game confirm |
| N2 | N2 | CP1 re-classed normal blocks at index ≥800 as ArabicSlotBlock (stray FACING state). | Reverted, awaiting in-game confirm |

---

