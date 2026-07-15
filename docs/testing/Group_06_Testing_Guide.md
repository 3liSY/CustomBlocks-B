# Group 06 — Tools, Colour Variants & Per-Face Paint

**🎯 Active:** A (0/14) · B (0/44) · C (0/11) · D/M/N/O/RC reverted (0/10)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | 🎯 36 rows + 5 reverted sections need (re)testing |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/79) |
| **Last tested** | 2026-06-29                      |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): RecolorConfirmMenu/HexRecolorConfirmMenu/OmniMenu kept as chest GUI (design polish flagged). HexColorsMenu + CustomColorMenu → Screen, not built. See `GROUP_06_TOOLS.md`.
>
> ⚠️ **2026-07-10:** G06-4 (hex-change system full rework — sync payload, live-tinted tool icons, robust re-bake repaint, tool rename/lore, gradient/tone-mix tool) is about to be built, alongside G10-1. §B below (`/cb config hex`) tests today's BROKEN behavior — new rows land once G06-4 is built (per checklist-after-build rule), replacing/extending §B, not deleted first. See `GROUP_06_TOOLS.md` §G06-4.

## 🗺️ Sections

| §  | What                                   | State              |
| --- | -------------------------------------- | ------------------ |
| A  | Per-face paint (Rainbow Rectangle)     | 🎯 0/7              |
| G  | Held-block dynamic glow                | ❔ needs discussion |
| H  | Dedicated creative tools tab           | ❔ needs discussion |
| I  | Chisel + Lumina unification            | ❔ needs discussion |
| J  | Tool-give shortcuts                    | ❔ needs discussion |
| K  | Tool consolidation                     | ❔ needs discussion |
| L  | Background-removal clean-up            | ❔ needs discussion |
| B  | Hex system rework                      | 🟡 2/22             |
| C  | Live resync — no rejoin                | 🟡 0/6              |
| D  | Unified Recycle-Bin delete (Slice 1-2) | 🎯                  |
| M  | Instant Square swaps                   | 🎯 0/7             |
| N  | Variant names + cleaner hotbar         | 🎯                  |
| O  | Core: Square swap · Triangle variants  | 🎯                  |
| RC | `/cb redo` after a create              | 🎯                  |
| D  | Unified Recycle-Bin delete (Slice 3-5) | ⏳ planned          |
| E  | Per-face Omni-Tool "Face" mode         | ⏳ planned          |
| F  | Block drops customization (SELF-default baseline ✅, rest ⏳) | 🟡 baseline confirmed |

🔗 **Related Doc:** [GROUP_06_TOOLS.md](../groups/GROUP_06_TOOLS.md)

---

# 🎯 Test now

## A · Per-face paint · 🎯 0/7

> 💡 Rainbow Rectangle paints one face of a block from an image URL.
> 🧰 place a textured block `foo`; `/cb rectangle` (or `/cb` → Magic Items → Rainbow Rectangle).

| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Right-click `foo`'s **north** face → paste a URL | chat prefills `/cb paintface foo north `; only north changes on reload | 🎯 | 🎯 |
| A2 | Paint **up**, then `/cb paintface foo east <url>` | each face keeps its own image; unpainted stay base | 🎯 | 🎯 |
| A3 | `/cb clearface foo north` → `/cb clearface foo all` | north resets; then all reset; none left → "foo has no painted faces" | 🎯 | 🎯 |
| A4 | Sneak + right-click two blocks | "Corner 1 set…" then "Area selected …" | 🎯 | 🎯 |
| A5 | Bad face · broken URL · missing block | the matching error each time | 🎯 | 🎯 |
| A6 | Restart with `foo` painted | painted faces survive | 🎯 | 🎯 |
| A7 | Delete painted `foo`, create a new block | no face art bleeds onto the new block | 🎯 | 🎯 |

## B · Hex system rework · 🟡 2/22

> 💡 `/cb config hex red #FF8800` makes "red" mean orange, syncing variants and items.
> 🧰 a placed `vart_red` + a `_green` variant + Squares/Triangles.

| #   | Action                                            | Expected Result                                  | SP | MP |
| --- | ------------------------------------------------- | ------------------------------------------------ | ----- | --- |
| B1  | `/cb config hex`                                  | all four on one line                             | 🎯    | 🎯     |
| B2  | `/cb config hex red #FF8800`                      | Yes/Info/No chest opens                          | 🎯    | 🎯     |
| B3  | click **Yes**                                     | `vart_red` bg orange after reload, design intact | 🎯    | 🎯     |
| B4  | check Red Square/Triangle **NAME** (no rejoin)    | reads `… [#FF8800]` live                         | 🎯    | 🎯     |
| B5  | check Red Square/Triangle **ICON** (after reload) | clean orange                                     | 🎯    | 🎯     |
| B6  | `/cb config hex green #10FF01`                    | bright green                                     | 🎯    | 🎯     |
| B7  | Red Triangle on a different block                 | orange background                                | 🎯    | 🎯     |
| B8  | `red banana` · `purple #112233`                   | expected errors                                  | 🎯    | 🎯     |
| B9  | `green #00FFAA` → **No**                          | new ones use #00FFAA                             | 🎯    | 🎯     |
| B10 | restart, `/cb config hex`                         | still shows custom hexes                         | 🎯    | 🎯     |
| B11 | `/cb config` → Variant Colours (red dye)          | sub-GUI: four dyes                               | 🎯    | 🎯     |
| B12 | Red dye → anvil "Set Red hex" → `#FF8800`         | same flow as command                             | 🎯    | 🎯     |
| B13 | `banana` · then **ESC**                           | back to editor                                   | 🎯    | 🎯     |
| B14 | with `_red` blocks: `/cb config hex red #22AAFF`  | Yes/Info/No opens **without** a reload first     | 🎯    | 🎯     |
| B15 | `/cb customcolor`                                 | 6-row studio, 29 colours                         | 🎯    | 🎯     |
| B16 | click **Purple**                                  | "Gave you Purple Square + Triangle", stays open  | 🎯    | 🎯     |
| B17 | Custom Hex… → `#FF1493`; `lavender`; `banana`     | pair lands; lavender works; banana errors        | 🎯    | 🎯     |
| B18 | Triangle on `foo`; then its Square                | Square swaps; no variant → "make one first"      | 🎯    | 🎯     |
| B19 | `/cb customcolor pink` · `#00CED1` · `banana`     | expected                                         | 🎯    | 🎯     |
| B20 | restart                                           | tools keep colour/name/tint                      | 🎯    | 🎯     |
| B21 | Look at the Red Triangle icon (moved from A8)     | clean symmetric triangle                         | 🎯    | 🎯     |
| B22 | Set red's hex in anvil **with a CB menu open** (moved from A9) | pack doesn't reload until you close everything | 🎯    | 🎯     |

## C · Live resync — no rejoin · 🎯 0/6 (reverted 2026-07-05, was C1–C3/C6 server-confirmed — stale, needs retest)

> 💡 Tools/commands broadcast the slot cache to every client so names/HUDs appear without rejoin.
> 🧰 a placed `vart`; Squares/Triangles + Deleter.

| #  | Action                                                | Expected Result                               | SP | MP |
| --- | ----------------------------------------------------- | --------------------------------------------- | ----- | --- |
| C1 | custom-colour `vart` → look at the new block          | HUD shows immediately                         | 🎯    | 🎯     |
| C2 | make a fresh block, place, look                       | texture appears ~1s                           | 🎯    | 🎯     |
| C3 | delete a block, keep crosshair on the leftover        | HUD/name clears live                          | 🎯    | 🎯     |
| C4 | rapidly create→delete→create, look at the last        | last block's texture + HUD correct live       | 🎯    | 🎯     |
| C5 | **LAN/2nd player:** host makes a variant, other looks | other player sees new name/HUD without rejoin | ➖    | 🎯     |
| C6 | remote server: hold/rename a block                    | item name/lore match HUD/server               | 🎯    | 🎯     |

---

## M · Instant Square swap feedback · 🎯 0/7 (retest after implementation)

> 💡 This is a Group 06 timing test, not a Group 04 wording test. The expected text stays exactly as the
> existing tool text. The client-side block prediction and the hotbar result must happen on the same click;
> the later server confirmation must not duplicate the line.
> 🧰 one normal block with a different existing colour variant, one custom-hex Square, one Arabic SlotBlock,
> and a dedicated server for MP rows.

| # | Action | Expected Result | SP | MP |
|---|---|---|---|---|
| M1 | Use a fixed-colour Square on a normal block whose target variant exists | the block changes colour and `Swapped to <display name>` appears at the same moment; the line appears once | 🎯 | 🎯 |
| M2 | Use a custom-hex Square on a normal block whose target variant exists | the custom Square uses the same instant one-time result; no special or delayed path | 🎯 | 🎯 |
| M3 | Use a Square on a block that is already that colour | the existing `Already <display name>.` line appears immediately and once; wording and punctuation are unchanged | 🎯 | 🎯 |
| M4 | Repeat M3 with a custom-hex Square | the same existing `Already <display name>.` behavior is instant and one-time | 🎯 | 🎯 |
| M5 | Run M1 and M3 on a dedicated server with a second player watching | the acting player's block and message are immediate; the second player receives the authoritative block state normally; no duplicate message appears | ➖ | 🎯 |
| M6 | Use fixed and custom Squares on an Arabic SlotBlock | the Arabic block recolours immediately and uses the same one-time existing result text; its facing/join data stays intact | 🎯 | 🎯 |
| M7 | Inspect the client/server confirmation after any successful Square action | the server remains authoritative, but its matching confirmation is silent to the player; there is never a second identical hotbar line | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| Slice | What                                                                                                            | Test when built                                                                  |
| ----- | --------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| 3     | route bulk delete + GUI delete + broken-cleanup through the same rail                                           | bulk delete → markers everywhere, no purple blocks, others update without rejoin |
| 4     | Trash reserve/restore — reserve slot while in Trash; Restore re-creates on a fresh slot + heals markers by name | delete → restore → markers turn back; restart → still restores                   |
| 5     | Empty frees the slot + tombstones markers; remove forever chunk-scanner + no-reuse leak                         | empty → number reusable; emptied markers don't revive on same-name create        |

| §  | What it'd do                                                                             | Spec                               |
| --- | ---------------------------------------------------------------------------------------- | ---------------------------------- |
| D+ | In-game **marker customization** screen — restyle the Deleted marker (deferred QoL of D) | `G06-15` — deferred, build after D |
| E  | Per-face Omni-Tool "Face" mode — rotate / mirror / copy a face                           | GROUP_06                           |
| F  | Block drops customization — per-block drop control (full config screen, NONE mode, XP, tool-tier) | `G06-7` — SELF-default baseline ✅ confirmed 2026-07-09 (via G30 §R3), rest still ⏳ |
| G  | Held-block dynamic glow                                                                  |                                    |
| H  | Dedicated creative tools tab                                                             |                                    |
| I  | Chisel + Lumina unification (omni-tool)                                                  |                                    |
| J  | Tool-give shortcuts                                                                      |                                    |
| K  | Tool consolidation                                                                       |                                    |
| L  | Background-removal clean-up — rim + corners → black, keep drop-shadow (🔍 reported 06-26) |                                    |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **D · Unified Recycle-Bin delete (Slice 1-2)** (Passed 2026-06-27). Test history moved to [GROUP_06_TOOLS.md](GROUP_06_TOOLS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **M · Instant Square swaps** (Passed). Test history moved to [GROUP_06_TOOLS.md](GROUP_06_TOOLS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **N · Variant names + cleaner hotbar** (Passed). Test history moved to [GROUP_06_TOOLS.md](GROUP_06_TOOLS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **O · Core: Square swap · Triangle variants** (Passed). Test history moved to [GROUP_06_TOOLS.md](GROUP_06_TOOLS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **RC · `/cb redo` after a create** (Passed). Test history moved to [GROUP_06_TOOLS.md](GROUP_06_TOOLS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **RC · redo after a create** (Passed 2026-06-29).
- **F · Block drops — SELF-default baseline** (Passed 2026-07-09, MP, confirmed via Group 30 §R3). Rest of `G06-7` still ⏳ — see [GROUP_06_TOOLS.md](../groups/GROUP_06_TOOLS.md) §G06-7.
- **M · Instant Square swaps** (Passed 2026-06-26).
- **N · Variant names + cleaner hotbar** (Passed 2026-06-26).
- **O · Core: Square swap · Triangle variants** (Passed 2026-06-11).

</details>

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | --- |
| G06-GLOW-LAG | (glow place) | Placing a block with `glow > 0` — its **light appears late, well after the block is placed** (found in G07 test 2026-07-10, moved here as owner). Likely the new-placement path in `SlotBlock.getPlacementState` doesn't force a client light re-check the way `SlotLighting` does for already-placed blocks. Mirror the already-fixed recolor "squares latency" fix. Needs repro + discussion. | ⚠️ finding |

---
