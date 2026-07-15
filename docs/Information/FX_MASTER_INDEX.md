# CustomBlocks-B — Master Issue Register

> **Permanent home for the issue register, conflict log, and build roadmap.**
> All issues folded into Group docs. This file = the index + cross-cutting records.
> ID scheme: `G##-n` (group number + dash + sequence). See `docs/Information/ID_MAP.md`.

---

## Complete Issue Register

| Cluster | ID | Issue / Feature | Group doc | Scope | Status |
|---|---|---|---|---|---|
| **C1 · Slot Reuse + Sync** | G06-1 | Action bar lag after Square recolor | GROUP_06 | Both | 🔍 diagnosed — not built |
| | G06-2 | Deleter doesn't delete properly + buggy (MP) | GROUP_06 | Both | 🔍 diagnosed — not built |
| | G06-3 | Delete-then-create scrambles placed blocks (slot recycling) | GROUP_06 | Both | 🔍 diagnosed — not built |
| | G05-1 | "Needs rejoin" — tools skip client resync + deeper corruption | GROUP_05 | Both | 🔍 diagnosed — not built |
| | G05-2 | Delete-then-create scrambles the new block's NAME + HUD | GROUP_05 | Both | 🔍 diagnosed — not built |
| | G05-4 | Square-swap FLICKER on dedicated server — `ClientSwapPredictor` reads the client's STALE local `SlotManager` (its "remote = empty" assumption was disproven by C1 step 4: a remote client loads its OWN `slots.json`), so it paints an old/deleted block for the round-trip before the server snaps it to the correct variant. Same `CLIENT_REMOTE_SESSION` staleness root as the step-4 names fix; predictor never got the guard. SP/LAN-host unaffected (local registry is live truth). | GROUP_06 | Both | 🔍 diagnosed 06-26 — fix = gate predictor on `CLIENT_REMOTE_SESSION` |
| **C2 · Command Layer** | G25-1 | `#` ("block I'm looking at") in EVERY block command | GROUP_25 | Both | 🔍 diagnosed — not built |
| | G04-2 | "Did you mean" / command smartness broken — revamp suggest + help | GROUP_04 | Both | 🔍 diagnosed — not built |
| **C3 · Feedback + Chat** | G04-1 | Unify + upgrade all action bars and chat | GROUP_04 / GROUP_27 | Both | 🔍 designed — direction locked; animation/duration taste open |
| **C4 · Background + Color Engine** | G10-1 | Transparent mode + background as controllable attr — ONE FEATURE (was FX-05+08) | GROUP_10 + GROUP_14 | Both | 🔍 designed → GROUP_10 |
| | G06-4 | Hex-change system rework — colour tools + variant repaint | GROUP_06 | Both | 🔍 designed → GROUP_06 |
| | G06-5 | Color-variant names compound wrong ("Block Yellow (Green)") | GROUP_06 | Both | 🟢 BUILT 06-26 standalone (3 steps: de-bracket name + nearest-preset name + hex-free id `_magenta` + 1-time boot migration of old blocks; shared `ColorLibrary.nearestName` now exists for G06-4 to reuse) — see GROUP_06 §N, awaiting batch test |
| | G07-2 | `/cb bulkrecolor` — new advanced bulk recolor op | GROUP_07 | Both | 🔍 designed → GROUP_07 |
| **C5 · Arabic** | G13-19 | Unify SlotBlocks + Arabic auto-join (shared contract + seam fix) | GROUP_13 | Both | 🔍 designed → GROUP_13 |
| | G13-20 | Consolidate Arabic — one auto-join system + bug fixes | GROUP_13 | Both | 🔍 designed → GROUP_13 |
| | G13-21 | Word-block system — keep & upgrade "100x" + rebrand to Text Blocks | GROUP_13 | Both | 🔍 diagnosed — co-design with G13-22 |
| | G13-22 | Arabic maker → Studio "Text" tab — unified creation screen | GROUP_13 | Both | 🔍 designed → GROUP_13 |
| **C6 · Undo** | G28-1 | Undo + undo GUI rework — 3-tab full screen + persistence | GROUP_28 | Both | 🔍 designed → GROUP_28 |
| **C7 · Studio + Editor** | G07-1 | `/cb bulk` alias → open Bulk Hub | GROUP_07 | Both | 🔍 diagnosed — ready (1-line alias) |
| | G27-2 | `/cb editor` rework — no-arg block list; block → Studio edit-mode | GROUP_27 | Both | 🔍 designed → GROUP_27 |
| | G06-7 | Block drops customization — per-block drop control | GROUP_06 | Both | 🔍 designed → GROUP_06 |
| | G09-1 | Trash GUI (`/cb deletedblocks`) needs upgrading — weak UX; also add a cleanup path for the orphan blank "Custom Block" items/placements that delete leaves behind | GROUP_09 | Both | 🗒️ reported 06-26 — backlog (not designed) |
| **C8 · New Features** | G30-1 | Guess-mode — blind the holder; owner-configurable disguise | GROUP_30 | Both | 🔍 designed → GROUP_30 |
| | G27-1 | HUD customization — expand on-screen block HUD + universal settings | GROUP_27 | Both | 🔍 designed → GROUP_27 |
| | G06-6 | Per-face actions — Omni-Tool "Face" mode (rotate/mirror/copy) | GROUP_06 | Both | 🔍 designed → GROUP_06 |
| | G14-1 | Showcase/cycler block — rapidly cycles through custom-block textures | GROUP_14 | Both | 🔍 designed → GROUP_14 |
| | G29-1 | Shorts framing overlay — capture-invisible 9:16 recording guide | GROUP_29 | Client-only | 🟡 Build A implemented 2026-06-30; awaiting in-game + OBS confirm |
| **C9 · Pack Integrity** | G05-3 | Random face-swap — concurrent client pack writes corrupt loose pack | GROUP_05 | Client-only | 🔍 diagnosed → GROUP_05 |

---

## 🗺️ Build Roadmap — Clusters & Order

| # | Cluster | Issues | State | Notes |
|---|---|---|---|---|
| **C1** | **Slot Reuse + Sync** — slot recycling, resync, tool parity | G06-1, G06-2, G06-3, G05-1, G05-2 | 🔍 diagnosed | Fix G06-2+G06-3 together (shared reuse-guard); G06-1+G05-2 share `AfterEdit.broadcast`; G05-1 is the deeper resync |
| **C2** | **Command Layer** — `#` everywhere + DidYouMean revamp | G25-1, G04-2 | 🔍 diagnosed | G04-2 first (real command tree); G25-1 uses that tree for tab-complete |
| **C3** | **Feedback + Chat** — unified style spec + upgrade | G04-1 | 🔍 designed | Absorbs G06-1 timing slice; ship incrementally (style spec → colours → sound → timing → animation) |
| **C4** | **Background + Color Engine** — `background` attr + re-bake primitive | G10-1, G06-4, G06-5, G07-2 | 🔍 designed | G10-1 first (bg attr + cutout layer) → G06-4 (re-bake engine, build G06-5 with it) → G07-2 (bulk hub) |
| **C5** | **Arabic** — shared contract, auto-join revamp, Text Studio tab | G13-19, G13-20, G13-21, G13-22 | 🔍 designed | G13-19 first → G13-20 → G13-21+G13-22 co-design in one session |
| **C6** | **Undo** — full rework, 3-tab screen, persistence | G28-1 | 🔍 designed | Do after C4+C5 (all new ops from those clusters register undo steps); then build the full screen |
| **C7** | **Studio + Editor** — `/cb editor` rework + block drops | G07-1, G27-2, G06-7 | 🔍 designed | G07-1 trivial (1-line alias, ship anytime); G27-2 first → G06-7 (Drops tab reuses Studio infra) |
| **C8** | **New Features** — all independent, pick any order | G30-1, G27-1, G06-6, G14-1, G29-1 | mixed: G29-1 built/pending, others designed | All standalone; no mandatory sequence |
| **C9** | **Pack Integrity** — concurrent client pack write race | G05-3 | 🔍 diagnosed | Standalone; 2 changes, 1 file; fix any time (high priority — causes random face-swap in-game) |

**Key cross-cluster gates:**
- All new MODIFY ops (G10-1, G06-4, G07-2, G06-6, G06-7) must register undo steps → gates on G28-1 being built (or at least the `UndoDescribe` helper existing)
- G25-1 (`#` everywhere) must be wired into every new block command as it's built (not retrofitted)
- G06-5 (variant naming) builds alongside G06-4 (shared `nearestName` resolver)
- G13-21+G13-22 co-design: one Arabic session

---

## 🔁 Duplicate & Conflict Register

> Every cross-issue overlap/conflict resolved with the owner. All 16 rows resolved as of 2026-06-25.

| # | Issue | Conflicts with | Type | What overlaps | Difference / why distinct | Resolution | Status |
|---|---|---|---|---|---|---|---|
| 1 | G13-21 (word/text blocks) | G13-22 (Arabic Studio tab) | overlap | Both touch word-block maker UI + `ArabicWordRenderer` live preview | G13-21 = system/back-end (rename, persist `TextData`, styling); G13-22 = Studio front-end tab | Split: G13-21 owns system, G13-22 owns screen; **build together** one Arabic session | ✅ resolved (2026-06-24) |
| 2 | G13-21 (word/text blocks) | G13-20 (Arabic consolidate) | overlap | Both use `ArabicMaker` flow + `WordChoiceMenu` | G13-20 = auto-join letters/numbers; G13-21 = single textured text block. Different render paths | Keep distinct render paths; share maker entry; co-design G13-20/G13-21/G13-22 one session | ✅ resolved (2026-06-24) |
| 3 | G13-21 (word/text blocks) | G10-1 (background axis) | overlap | Word block bg colour baked into PNG = same "background" attribute G10-1 stores | G13-21 should consume shared `background` attribute | Fold G13-21 bg onto G10-1 `background` field once it lands | ✅ resolved (2026-06-24) |
| 4 | G13-22 (Arabic Studio tab) | G27-2 (editor → Studio tab) | overlap | Both add a new `Section`/tab to `BlockCreationStudioScreen` via same panel + `openStudioEdit` infra | G13-22 = Arabic-maker tab; G27-2 = Edit/Manage tab. Different content, same mechanic | TWO SEPARATE tabs, two purposes — NOT merged. Each keeps its own panel | ✅ resolved (2026-06-24) |
| 5 | G13-20 (Arabic consolidate) | G13-19 (SlotBlocks+Arabic unify) | overlap | Both = auto-join revamp: join/re-flow correctness + hairline seam fix | G13-19 = shared-contract + seam direction; G13-20 = consolidation + numbers auto-join + retire static | Do G13-19 first as G13-20 step 1 (one revamp serves both); gated on in-game seam repro | ✅ resolved (2026-06-24) |
| 6 | G10-1 (transparency + bg — merged) | — | supersedes/merge | Previously two separate items (transparency + bg colour); need same new `SlotData` field + per-block setter | None left — they are the SAME axis (`background = {transparent\|black\|colour}`) | **MERGED into one feature:** one `background` attribute, cutout render layer, black default, 4 synced surfaces | ✅ resolved (2026-06-24) |
| 7 | G06-4 (hex rework) | G10-1 (background attribute) | overlap | "Robust repaint = re-bake-from-source" is same engine G10-1 bg system uses | G06-4 = 4 fixed colour tools + variant repaint; G10-1 = bg attribute. Shared re-bake primitive | Build ONE re-bake-from-source engine; both consume it | ✅ resolved (2026-06-24) |
| 8 | G07-2 (bulkrecolor) | G10-1 + G06-4 (recolour engine) | overlap | Bulkrecolor loops per-block recolour over `BulkScope`; its background tile IS G10-1's bulk-bg | G07-2 = bulk hub/menu; G10-1/G06-4 = single-block engine | G07-2 reuses G10-1/G06-4 engine + Group-07 framework; only menu is new | ✅ resolved (2026-06-24) |
| 9 | G06-5 (variant naming) | G06-4 (hex tools) | overlap | G06-5's name-clean + nearest-name applies to variants G06-4 creates/repaints | G06-5 = naming; G06-4 = colour/repaint pipeline | Apply G06-5 `nearestName` + strip in G06-4 variant paths; build together | ✅ resolved (2026-06-24) |
| 10 | G06-7 (block drops) | G27-2 (editor → Studio tab) | overlap | "Advanced Drops screen" must be a Studio surface sharing `SlotData.drops` state | G06-7 = drop system + screen; G27-2 = Studio/editor tab framework | Drops = its OWN Studio Section tab (`Section.DROPS`) BESIDE Attributes tab; distinct from Manage tab | ✅ resolved (2026-06-24) |
| 11 | G06-7 (block drops) | G07-2 (bulk framework) | overlap | A bulk "set drops" op would loop over `BulkScope` like every other bulk | G06-7 = per-block drops; G07-2 = bulk menu/framework | Add bulk-drops op to G07-2 framework; reuse, don't rebuild bulk | ✅ resolved (2026-06-24) |
| 12 | G04-1 (feedback unify) | G27-1 (HUD expansion) | overlap | Both define on-screen text style (colour/glyph/animation); should read as one visual voice | G04-1 = chat/hotbar/button-bar feedback; G27-1 = block-info HUD bricks | Share ONE palette + glyph + animation spec; G27-1 bricks and G04-1 messages reference it, don't redefine | ✅ resolved (2026-06-24) |
| 13 | G28-1 (undo rework) | G10-1/G06-7/G06-6/G06-4 (new ops) | overlap | Every new MODIFY op must register an undo step | G28-1 = undo system; others = ops that feed it | **All new ops plug into G28-1's shared `UndoDescribe` helper** — nothing ships un-undoable | ✅ resolved (2026-06-24) |
| 14 | G13-19 (shared `CbBlock` contract) | G06-4 (tools special-casing SlotBlock vs Arabic) | overlap | Tools branch `instanceof SlotBlock` vs Arabic today; G13-19's contract removes that | G13-19 = interface; G06-4 = consumer | **Migrate each tool onto the contract as it's built** (incremental) | ✅ resolved (2026-06-24) |
| 15 | G25-1 (`#` in all commands) | G06-7/G06-6/G07-2 (new commands) | overlap | New block commands must accept `#` like G25-1 standardises | G25-1 folded into G25; its `BlockRefArgumentType` is the contract | **All new block commands accept `#` from the start** (built-in, not retrofitted) | ✅ resolved (2026-06-24) |
| 16 | G29-1 (framing overlay) | G27-1 (in-frame cinematic HUD preset) | overlap | Both are recording aids | G29-1 = guide **hidden FROM** recording; G27-1 preset = clean HUD **shown IN** recording. Opposite mechanisms | **Keep BOTH, distinct features**; build separately | ✅ resolved (2026-06-24) |

---

## Status Key

**Status:** 🆕 new · 💬 discuss later · 🔍 diagnosed/designed · 🛠️ building · ✅ done (in-game confirmed)
**Scope:** SP · MP · Both

> All 27 issues folded into their Group docs as of 2026-06-25. This file = permanent index only.
> For full spec + decisions + verify checklist, read the Group doc listed in the table above.

## ⚠️ Build-time requirement

Before building any issue:
1. **Add its test checklist to `docs/TESTING_GUIDE.md`** (per CLAUDE.md §6). Nothing is ✅ done until the developer runs that checklist in-game and confirms. An issue without a TESTING_GUIDE entry cannot be marked done.
2. **Deep-search the Group docs + code** to confirm the work isn't already built/planned. Three outcomes: already exists (don't rebuild) · partly exists (split) · truly new (proceed). Any overlap goes in the Conflict Register above before building.
3. **All new block commands must accept `#` from the start** (G25-1 contract) — not retrofitted later.
4. **All new MODIFY ops must register an undo step** (G28-1 `UndoDescribe` helper) — nothing ships un-undoable.
