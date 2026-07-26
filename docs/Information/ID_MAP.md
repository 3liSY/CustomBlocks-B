# CustomBlocks-B — Master ID Map & Unified Scheme

> **Single source of truth for every issue ID.** Created 2026-06-24.
> One scheme replaces six legacy ones. Nothing is renamed in any other file until its
> group's map below is marked ✅ **applied** — this doc is built first, reviewed, then rolled out.

---

## Why this exists

The project numbered issues **six different ways at once**, so the same problem had three names:

| The same issue | Was called (file 1) | (file 2) | (file 3) |
|---|---|---|---|
| Deleter doesn't delete | `FX-03` (ISSUES.md) | — | — |
| Arabic word command | `O1` (GROUP_13) | `17.18` (All_Groups) | `Group O` |
| HUD config GUI | `17.4` (All_Groups) | `Req §2` | — |

The six legacy schemes: `17.x` · `Group A–R` · `O1–O11` · `P/Q/R` · `R3.x` · `FX-01–26` (+ `G##.N` tests).
This map folds **all** of them into one.

---

## The unified scheme — LOCKED 2026-06-24

**Format: `G<NN>-<n>`** — group number + dash + sequence. Examples: `G06-1`, `G13-4`, `G30-1`.

Rules:
1. **Every issue lives in exactly one group.** Its ID names that group.
2. **One running sequence per group** (`G13-1`, `G13-2`, …). Never reused, never renumbered once assigned.
3. **Dash, not dot** — dot (`G13.1`) already means a *test* in legacy docs; the dash avoids the clash.
4. **Overlapping legacy IDs merge into one new ID.** If `FX-06` and `R3.1` are the same problem, they
   become **one** `G13-n` and both old IDs are listed under "was."
5. **Nothing is lost.** Every legacy ID appears in the "was" column of exactly one new ID (or is marked
   ⛔ removed / ❌ scrapped with the reason).
6. **Tests reuse the issue's ID** — no separate test numbers. `G13-6`'s test = "TG §G13-6."

---

## Decisions locked (2026-06-24)

| # | Decision | Value |
|---|---|---|
| 1 | ID format | `G##-n` (group + dash + sequence) |
| 2 | Scope | **Everything** — Finale Fix folder, group docs, testing guides, PROGRESS_LOG, CHANGELOG, ADR cross-refs |
| 3 | New group | **G30 — Guess Mode** (home for the old `FX-19`) |
| 4 | ADRs | **Stay `ADR-NNN`** — a separate namespace (decisions, not tasks). Issues cross-link to them, never rename them. |
| 5 | Logs (PROGRESS_LOG / CHANGELOG) | _open — owner has an idea, pending_ |

---

## Group backbone (G01–G30)

```
G01 Legacy Audit        G11 Category            G21 Config GUI
G02 Chest GUI           G12 Export/Marketplace  G22 Permissions
G03 HUD / ESC           G13 Arabic              G23 Player Experience
G04 Chat                G14 Animation/Video     G24 Macros
G05 Resource Pack       G15 AI Textures         G25 Block Mgmt Extras
G06 Tools               G16 Diagnostics         G26 Name & Give Fixes
G07 Bulk Operations     G17 Regressions         G27 Screens
G08 Shapes              G18 Notes / Staging     G28 Create Studio
G09 Backup / Safety     G19 Display             G29 Creator Tools
G10 Color / Image       G20 External Integr.    G30 Guess Mode  (NEW)
```

---

## The passport (how the four docs stay synced)

Each issue carries one line that links every doc by the shared ID — **each fact lives in one place,
the others link to it, nothing is copied** (so nothing can drift / contradict):

```
G13-6 · Auto-joining engine
  was: O3, 17.20, tests G13.5–G13.8   ·   Status: 🛠️ built, look-locked
  Decision: ADR-003 + ADR-005   ·   Test: TG §G13-6   ·   Built: 2026-06-18 (texture path only)
```

| Doc | Holds | For G13-6 |
|---|---|---|
| Issue board (group doc) | the task + diagnosis | the spec |
| ADR-NNN | the *why* decision | ADR-003 / ADR-005 |
| Testing guide | the in-game check | TG §G13-6 |
| PROGRESS_LOG | what was done + when | "2026-06-18 …" |
| CHANGELOG | the player-facing note | (on release) |

**`Built:` is filled only after in-game confirmation (Golden Rule — `CLAUDE.md §2`).**

**Test checklist rule:** every `G##-n` appears in its group testing guide's `🔗 Unified-scheme IDs` crosswalk
as status-only until it is built; the **full step/expected/pass-fail checklist is written the moment the item
is implemented** (same pass), never speculatively for unbuilt code.

---

## Status legend

`✅ confirmed in-game` · `🛠️ built / awaiting in-game` · `💬 designed (not built)` ·
`🔍 diagnosed (not built)` · `⛔ removed` · `❌ scrapped` ·
`❔ unverified — build state not yet reconciled (check the group's TG / PROGRESS_LOG before trusting)`

> **Honesty rule:** `❔` is used wherever the source doc did not state a confirmed status. Nothing is
> marked `✅` unless the group doc / PROGRESS_LOG records an in-game confirmation (Golden Rule, `CLAUDE.md §2`).

---

## Roll-out tracker

### Pass 1 — FX board + G13 template (the original roll-out)

These groups had their **FX-board / template** IDs drafted, owner-approved, and written into the real files.

| Group | Map drafted | Owner approved | Applied to files |
|---|---|---|---|
| G04 Chat | ✅ | ✅ | ✅ 2026-06-25 |
| G05 Resource Pack | ✅ | ✅ | ✅ 2026-06-25 |
| G06 Tools | ✅ | ✅ | ✅ 2026-06-25 |
| G07 Bulk Operations | ✅ | ✅ | ✅ 2026-06-25 |
| G10 Color/Image | ✅ | ✅ | ✅ 2026-06-25 |
| G13 Arabic | ✅ | ✅ | ✅ 2026-06-25 |
| G14 Animation/Video | ✅ | ✅ | ✅ 2026-06-25 |
| G25 Block Mgmt Extras | ✅ | ✅ | ✅ 2026-06-25 |
| G27 Screens | ✅ | ✅ | ✅ 2026-06-25 |
| G28 Create Studio | ✅ | ✅ | ✅ 2026-06-25 |
| G29 Creator Tools | ✅ | ✅ | ✅ 2026-06-25 |
| G30 Guess Mode | ✅ | ✅ | ✅ 2026-06-25 |

### Pass 2 — legacy backlog mapped into every group (2026-06-25, this pass)

Every remaining legacy item (`17.x`, `Group A–R`, `P/Q/R`, `Decision §`) now has a `G##-n` ID drafted in the
GROUP MAPS below — **all 30 groups are mapped.** These rows are **drafted only**: not yet owner-approved and
**not yet written into the real files** (that is the next, owner-confirmed step, group by group, per the Method).

| Group | Legacy rows drafted | Owner approved | Applied to files |
|---|---|---|---|
| G01 Legacy Audit | ✅ | ⏳ | ⏳ |
| G02 Chest GUI | ✅ | ⏳ | ⏳ |
| G03 HUD / ESC | ✅ | ⏳ | ⏳ |
| G04 Chat (+legacy) | ✅ | ⏳ | ⏳ |
| G05 Resource Pack (+legacy) | ✅ | ⏳ | ⏳ |
| G06 Tools (+legacy) | ✅ | ⏳ | ⏳ |
| G07 Bulk Operations (+legacy) | ✅ | ⏳ | ⏳ |
| G08 Shapes | ✅ | ⏳ | ⏳ |
| G09 Backup / Safety | ✅ | ⏳ | ⏳ |
| G10 Color/Image (+legacy) | ✅ | ⏳ | ⏳ |
| G11 Category | ✅ | ⏳ | ⏳ |
| G12 Export / Marketplace | ✅ | ⏳ | ⏳ |
| G14 Animation/Video (+legacy) | ✅ | ⏳ | ⏳ |
| G15 AI Textures | ✅ | ⏳ | ⏳ |
| G16 Diagnostics | ✅ | ⏳ | ⏳ |
| G17 Regressions | ✅ | ⏳ | ⏳ |
| G18 Notes / Staging | ✅ | ⏳ | ⏳ |
| G19 Display | ✅ | ⏳ | ⏳ |
| G20 External Integrations | ✅ | ⏳ | ⏳ |
| G21 Config GUI | ✅ | ⏳ | ⏳ |
| G22 Permissions | ✅ | ⏳ | ⏳ |
| G23 Player Experience | ✅ | ⏳ | ⏳ |
| G24 Macros | ✅ | ⏳ | ⏳ |
| G25 Block Mgmt Extras (+legacy) | ✅ | ⏳ | ⏳ |
| G26 Name & Give Fixes | ✅ | ⏳ | ⏳ |
| G27 Screens (+legacy) | ✅ | ⏳ | ⏳ |

> G13/G28/G29/G30 are fully covered by Pass 1 (no extra legacy backlog beyond what's already mapped).

---

## Legacy bucket → group (rollout reference)

Where each legacy bucket folds. `?` = confirm with owner when that group's map is built.

| Legacy | → Group | | Legacy | → Group |
|---|---|---|---|---|
| 17.1 silent RP | G05 | | Group A Bulk | G07 |
| 17.2 chat tone | G04 | | Group B Shapes/faces | G08 |
| 17.3 chest GUI | G02 | | Group C Backup | G09 |
| 17.4 / 17.6 HUD | G03 | | Group D Trash/recovery | G09 ? |
| 17.5 gui editor | G02 / G27 ? | | Group E Color/Image | G10 |
| 17.7 ESC menu | G03 | | Group F Tool-give | G06 |
| 17.8 held glow | G06 ? | | Group G Category | G11 |
| 17.9 tools tab | G06 | | Group H Marketplace | G12 |
| 17.10 omni-tool | G06 | | Group I Voice | ❌ scrapped |
| 17.11 legacy audit | G01 | | Group J Showcase | G19 ? (FX-23) |
| 17.12 note | G18 | | Group K Identity ops | G25 |
| 17.13 fav alias | G17 / G25 ? | | Group L HUD/GUI cmds | G02 / G03 / G27 (split) |
| 17.14 draft/publish | G18 | | Group M Misc | split (find/recent→G25, achievements→G23, script→G24) |
| 17.15 export | G12 | | Group N Regressions | G17 |
| 17.16 AnimBlockScreen | G14 | | Group O Arabic | G13 |
| 17.17 DevConsole | G16 | | P1→G14 P2→G16 P3→G15 P4→G21 P5→G18 P6→G18 P7→G23 | |
| 17.18–21 Arabic | G13 | | Q1→G19 Q2→G10 Q3→G10 Q4→G23 Q5→G04 Q6→G14 Q7→G22 Q8→G06 | |
| 17.22 AI | G15 | | R1→G02/G16 R2 Drops→G06 ? R3→G23 R4→G25 R5→G11/G10 | |
| 17.23 config GUI | G21 | | FX-19 guess | **G30** |
| 17.24 diag | G16 | | FX-26 overlay | G29 |

---

# GROUP MAPS

## G13 — Arabic   ✅ drafted (template)

Every legacy Arabic ID, reconciled into one `G13-n` sequence. Overlaps merged.

### Fonts & setup
| New | Title | was | Status |
|---|---|---|---|
| G13-1 | arabtype.ttf bundled (auto-extract) | O4 · 17.19 · test G13.1 | 🛠️ |
| G13-2 | English font (Rockwell Condensed) | O2 · 17.21 · test G13.9 | ❌ mostly scrapped |

### Maker & browser
| New | Title | was | Status |
|---|---|---|---|
| G13-3 | Word command redesign (anvil flow) | O1 · 17.18 · Issue 2b · tests G13.3/G13.4 | 🛠️ |
| G13-4 | Colour workflow + live preview screen | Issue 2c · 2d | 💬 |
| G13-5 | Arabic browser (chest GUI) | Issue 2a · test G13.10 | 🛠️ |

### Auto-join system
| New | Title | was | Status | Decision |
|---|---|---|---|---|
| G13-6 | Auto-joining engine | O3 · 17.20 · tests G13.5–G13.8 | 🛠️ | ADR-003, ADR-005 |
| G13-7 | Naming + virtual IDs + live config labels | O6 | 💬 | ADR-006 |
| G13-8 | Bundled letters → editable config | O7 | 💬 | ADR-006 |
| G13-9 | Hide / manage bundled letters | O8 | 💬 | |
| G13-10 | Type-a-word auto-build | O9 | 💬 | |
| G13-11 | Placement lag + transparent-flash fix | O10 | 🛠️ partial | |
| G13-12 | Recolour placed letter with Squares | O11 | 🛠️ (server-slow open) | ADR-009 |
| G13-13 | Readable back (two-faced sign) | R3.6 | 💬 | |
| G13-14 | Rendering polish (bg #0A0A0A, weight, HUD) | Issue 1a/1b/1c | mixed | |
| G13-15 | Convert remaining GUI→chat prompts | Issue 3 (9 items) | 🔍 partial | |

### Already fixed (kept for record)
| New | Title | was | Status |
|---|---|---|---|
| G13-16 | join default count → 1 | R3.2 | ✅ in code |
| G13-17 | pick-block stamp | R3.3 | ✅ in code |
| G13-18 | "place letter blocks" gives joinable | R3.4 | ✅ in code |
| — | OmniTool Arabic Direction mode | O5 · R3.5 | ⛔ removed (tombstone) |

### FX board items, folding in
| New | Title | was | Status | Note |
|---|---|---|---|---|
| G13-19 | Shared block contract + seam fix | FX-06 · R3.1 | 🔴 19.4 regressed 06-27 (seam ✅) | cross-group: touches G06; Deleter on Arabic broke again |
| G13-20 | Consolidate to one auto-join (numbers join, retire static) | FX-12 | 💬 | |
| G13-21 | Text Blocks (re-editable + styling) | FX-13 | 🔍 | renamed from "word block" |
| G13-22 | Arabic maker → Studio tab | FX-25 | 💬 | links G13-4, G28 studio |
| G13-23 | Auto-join flicker / transparent-flash on placement + recolour | — (new 06-27) | 🔴 locked fix, not built | tail of G13-12 Cause 2; render-thread only |
| G13-24 | Migrate `setglow` onto the `CbBlock` contract (Arabic letters) | — (was tagged G13-23) | 🔍 designed | renumbered to free G13-23 for the flicker |
| G13-25 | Full settings-sheet parity for letters (glow/hardness/sound/collision/bg, one sheet per letter+colour) | — (new 07-02) | 📝 designed, not built | absorbs G13-24; supersedes G13-23's fix direction; Deleter (G13-19) re-fixed as part of it |

---

## G04 — Chat   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G04-1 | Unify + upgrade all action bars and chat | FX-02 | 💬 designed — direction locked; animation/duration taste open |
| G04-2 | "Did you mean" / command smartness broken — revamp suggest + help GUI | FX-16 · Q5 | 🔍 diagnosed — not built (Q5 `/cb help` GUI merges here) |
| G04-3 | Chat messages too terse — full message-tone rewrite (single polished professional tone) | 17.2 | ❔ |
| — | Voice modes (multiple tone presets) | Group I | ❌ scrapped → folded into the single professional tone (G04-1/G04-3) |

---

## G05 — Resource Pack   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G05-1 | "Needs rejoin" — tools skip client resync + deeper corruption | FX-10 | 🔍 diagnosed — not built |
| G05-2 | Delete-then-create scrambles the new block's NAME + HUD | FX-15 | 💬 redesigned → **G06-14** (Recycle-Bin); old `(Removed)` approach scrapped 2026-06-27 |
| G05-3 | Random face-swap — concurrent client pack writes corrupt loose pack | FX-27 | 🔍 diagnosed — not built |
| G05-4 | Silent pack delivery — no prompt dialog; auto-accept mixin + `silentPack` config toggle | 17.1 · Dec §6 · Dec §E | ❔ |

---

## G06 — Tools   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G06-1 | Action bar lag after Square recolor | FX-01 | 🔍 diagnosed — not built |
| G06-2 | Deleter doesn't delete properly + buggy (MP) | FX-03 | 💬 redesigned → **G06-14** (Recycle-Bin); old `(Removed)` approach ⛔ scrapped 2026-06-27 |
| G06-3 | Delete-then-create scrambles placed blocks (slot recycling) | FX-04 | 💬 redesigned → **G06-14**; reserve-not-retire kills recycling |
| G06-4 | Hex-change system rework — colour tools + variant repaint | FX-09 | 💬 designed → GROUP_06 |
| G06-5 | Color-variant names compound wrong ("Block Yellow (Green)") | FX-11 | 🔍 diagnosed — build with G06-4 |
| G06-6 | Omni-Tool Face mode — 90-degree clicked-face image rotation plus later action backlog | FX-21 | ⏳ designed core; extra actions need discussion → GROUP_06 |
| G06-7 | Block drops customization — per-block drop control | FX-24 | 💬 designed → GROUP_06 |
| G06-8 | Held-block dynamic glow | 17.8 · Dec §7 | ❔ |
| G06-9 | Dedicated creative tools tab | 17.9 · Dec §8 | ❔ |
| G06-10 | Chisel + Lumina unification (omni-tool) | 17.10 · Dec §9 | ❔ |
| G06-11 | Tool-give shortcuts | Group F | ❔ |
| G06-12 | Tool consolidation | Q8 · Dec §C | ❔ |
| G06-13 | Background-removal clean-up (rim + corners + keep shadow) | — (new 06-26) | 🔍 reported — not built |
| G06-14 | Unified Recycle-Bin deletion system (replaces `(Removed)`) | — (new 06-27; absorbs G06-2/G06-3/G05-2 fix + G09-2 trash) | 💬 designed 2026-06-27 — not built |
| G06-15 | In-game marker customization screen (restyle the Deleted marker) | — (new 06-27; deferred QoL of G06-14) | 🧰 deferred QoL — not built (build after G06-14) |

---

## G07 — Bulk Operations   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G07-1 | `/cb bulk` alias → open Bulk Hub | FX-17 | 🔍 diagnosed — ready (1-line alias) |
| G07-2 | `/cb bulkrecolor` — new advanced bulk recolor op | FX-18 | 💬 designed → GROUP_07 |
| G07-3 | Bulk operations system — `bulkdelete/rename/reid/property/export/move/duplicate/lock/unlock/favorite/unfavorite/shape/sound/blockadd` (bulkrecolor → G07-2; bulkshape cross-refs G08) | Group A | ❔ |

---

## G10 — Color / Image   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status | Note |
|---|---|---|---|---|
| G10-1 | Transparent mode + background as controllable attr | FX-05 · FX-08 | 💬 designed → GROUP_10 | MERGED — FX-05 + FX-08 were the same axis |
| G10-2 | Color & image tools — `dress/gradient/colors/customcolor/palette/bgstudio/exportpng/resize` (+ `bgpick`; `tolerance` removed 2026-07-26, G10 §H) | Group E | ❔ | |
| G10-3 | Smart AI background removal | Q2 | ❔ | |
| G10-4 | Live Recolor & Screen Eyedrop | Q3 | ❔ | |
| G10-5 | Colour-variant service (shared recolour engine) | R5 (ColorVariantService) | ❔ | R5 splits — category services → G11 |

---

## G14 — Animation / Video   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G14-1 | Showcase/cycler block — rapidly cycles through custom-block textures | FX-23 | 💬 designed → GROUP_14 |
| G14-2 | Animated / video block engine — off-atlas grid render path + overhaul (ADR-013/ADR-014: retexture-fix + real-clock speed + quality pass) | 17.16 · P1 · Q6 | 🛠️ partial — ADR-014 Step 1+2 ✅ confirmed; Step 3 (mipmaps/pool/frame-cap/LOD) not built |
| — | Animation tab / `AnimBlockScreen` (UI) | (screen of 17.16) | → moved to **G27 §G27.15** (screen consolidation) |

---

## G25 — Block Mgmt Extras   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G25-1 | `#` ("block I'm looking at") in EVERY block command | FX-14 | 🔍 diagnosed — not built |
| G25-2 | Identity ops — `reid` / `swapid` / `swapname` / `duplicate` alias | Group K | ❔ |
| G25-3 | Drop-config service (`DropConfigManager`) | R2 | ❔ — drops feature spec lives in G06-7 |
| G25-4 | `BlockFinder` GUI + export PNG | R4 | ❔ |
| G25-5 | `settabicon` | Group M (settabicon) | ❔ |

---

## G27 — Screens   ✅ FX applied 2026-06-25 · legacy rows drafted (Pass 2)

| New | Title | was | Status |
|---|---|---|---|
| G27-1 | HUD customization — expand on-screen block HUD + universal settings | FX-20 | 💬 designed → GROUP_27 |
| G27-2 | `/cb editor` rework — no-arg block list; block → Studio edit-mode | FX-22 | 💬 designed → GROUP_27 |
| G27-3 | Unified screen design system + upgrade all screens + `ShapeEditorScreen` + `BlockCreationStudioScreen` | group core (§G27.1–G27.14) | 🛠️ partial — built screen-by-screen, owner-confirmed in-game |
| G27-4 | `/cb gui editor` rework (screen side) | 17.5 (screen part) | ❔ — chest-GUI part folds to G02 |
| G27-5 | Screen-side GUI commands (`listgui/menu/undogui/redogui/editor`) | Group L (screen side) | ❔ — chest part folds to G02 |
| G27-6 | Animation tab / timeline editor screen | §G27.15 (absorbs G14 `AnimBlockScreen`) | 💬 designed → GROUP_27 |
| G27-7 | Tutorial screen + Achievements gallery screen | §G27.16 (absorbs G23 screens) | 💬 designed → GROUP_27 |
| G27-8 | Shared block-browser Filters menu + combinable `Animated` filter + `/cb animation` handoff | new 2026-07-18 | ⏳ designed; menu details need discussion → GROUP_27 |

> **Note:** G27 also uses internal section IDs **`§G27.1 … §G27.16`** for its individual screens. Like the
> `G##.N` *test* IDs, these section numbers are a separate axis (a screen-build outline) and are **not**
> renamed to `G27-n` — the `G27-n` rows above are the issue/feature units, the `§G27.N` are the build steps.

---

## G28 — Create Studio   ✅ applied 2026-06-25

| New | Title | was | Status |
|---|---|---|---|
| G28-1 | Undo + undo GUI rework — 3-tab full screen + persistence | FX-07 | 💬 designed → GROUP_28 |

---

## G29 — Creator Tools   ✅ applied 2026-06-25

| New | Title | was | Status |
|---|---|---|---|
| G29-1 | Shorts framing overlay — capture-invisible 9:16 recording guide | FX-26 | 🟡 Build A implemented 2026-06-30; awaiting in-game + OBS confirm |

---

## G30 — Guess Mode   ✅ applied 2026-06-25 · v3 slices added 2026-07-07

| New | Title | was | Status |
|---|---|---|---|
| G30-1 | Guess-mode — blind the holder; owner-configurable disguise (v2 core) | FX-19 | ✅ v2 core in-game 2026-07-06 (TG A–H) |
| G30-2 | v3 §1 — disguise look customization + fallback chain (`defaultblock` / per-round `look`) | — | ✅ Phase 1 id-based in-game 2026-07-06 (TG §I) · Phase 2 link ⛔reverted · sub G30-2b built |
| G30-2b | v3 §1 — fallback "?" texture quality fix (regen 512×512 sharp/black; delete old purple `mystery.png`) | — | ✅ in-game 2026-07-08 (TG §P) |
| G30-3 | v3 §2 — smooth pickup / put-down pose transition (no snap) | — | ✅ in-game 2026-07-06 (TG §K) |
| G30-4 | v3 §3 — pose editor screen (per-arm sliders + live dummy preview + presets) | — | ✅ Pose tab in-game 2026-07-07 (TG §M) · ✅ `/cb guess pose` removed → screen `GuessPosePayload` in-game 2026-07-08 (TG §N) · 🟡 slider number entry (click-to-type / scroll·↑↓ nudge / right-click reset) built 2026-07-08, pending in-game (TG §Q) |
| G30-5 | v3 §4 — manual round-control keybinds (wrong-buzz / correct-reveal / force-end) | — | ⬜ not built |
| G30-6 | v3 §5 — cursed-buzz effect (flicker + red static + glitch sound + jolt), tunable + live preview | — | ⬜ not built |
| G30-7 | v3 §6 — hint ladder (decoy → silhouette → blur → reveal, keybind-advanced, reorderable) | — | ⬜ not built |
| G30-8 | v3 §7 — stage-zone marking + correct-reveal shockwave | — | ⬜ not built (parked, round-control pass) |
| G30-8b | v3 §8 — Showcase: floating end-crystal display (`/cb guess showcase spawn/delete` + live Showcase tab) | — | 🟡 built 2026-07-08 (real placed block + BER + shared tuning), pending in-game (TG §S) |
| G30-9 | v3 §8 — round timer (saved default + override) + timeout TNT consequence + boss-tension toggle | — | ⬜ not built |
| G30-10 | v3 §9 — sound-swap while disguised (bundled preset sounds) | — | ⬜ not built |
| G30-11 | v3 §10 — unified `/cb guess` mega-screen (tabs: Pose · Buzz · Sound · Look · Showcase) | — | ✅ tabbed shell in-game 2026-07-08 (Pose live · rest greyed "coming soon") (TG §N) |
| G30-12 | v3 parked — impostor round (spot-the-odd-one-out) | — | ⛔ parked — own future design session |

---

# GROUP MAPS — Pass 2 (legacy backlog, drafted 2026-06-25)

> One `G##-n` per source-issue token from each group doc's **Source issues** header (bucket-level grain —
> multi-command buckets can be split per-command later if the owner wants). `was` cites the legacy ID.
> Statuses are taken from the group doc / PROGRESS_LOG where stated, else `❔`. **Drafted, not yet applied.**

## G01 — Legacy Audit

| New | Title | was | Status |
|---|---|---|---|
| G01-1 | Legacy feature audit — confirm every old-CB feature has a roadmap home | 17.11 | ✅ audit complete → `Reports/GROUP_01_AUDIT_REPORT.md` |

## G02 — Chest GUI

| New | Title | was | Status |
|---|---|---|---|
| G02-1 | Chest GUI core infrastructure (dashboard / menu framework) | 17.3 | ❔ |
| G02-2 | `/cb gui editor` — chest-GUI editor | 17.5 (chest part) | ❔ — screen part → G27-4 |
| G02-3 | GUI command suite — `listgui/menu/undogui/redogui/history/magicitems/editmagicitems/editor/rp pause·resume/sync/unsuppress` | Group L | ❔ — screen-side → G27-5 |
| G02-4 | GUI regressions — search GUI + config GUI | Group N (GUI part) | ❔ — regression table → G17 |

## G03 — HUD / ESC

| New | Title | was | Status |
|---|---|---|---|
| G03-1 | HUD overlay — block id + name on crosshair | 17.6 | ❔ |
| G03-2 | HUD config system + drag-to-reposition editor (`/cb config hud`, `/cb edithud`) | 17.4 | ❔ |
| G03-3 | ESC / pause-menu integration — two CB buttons | 17.7 | ❔ |

## G08 — Shapes

| New | Title | was | Status |
|---|---|---|---|
| G08-1 | Shape system + per-face textures — `setshape/addshape/removeshape/clearshape/shapeeditor/shapelist/shapepreview/facechangegui/setface/clearface/clearallfaces/bulkshape/customtriangle/trianglemode` | Group B | ❔ — `shapeeditor` screen → G27; `bulkshape` cross-refs G07 |

## G09 — Backup / Safety

| New | Title | was | Status |
|---|---|---|---|
| G09-1 | Backup system | Group C | ❔ |
| G09-2 | Trash & recovery | Group D | 💬 recovery half of **G06-14** Recycle-Bin (restore = fresh slot + heal markers; empty = free slot) 2026-06-27 |
| G09-3 | First-boot migration (`MigrationManager`) | first-boot migration (All_Groups §Server Config Folder Structure) | ❔ |

## G11 — Category

| New | Title | was | Status |
|---|---|---|---|
| G11-1 | Category system — `blocks/blockscat(blockscategory)/blockadd/givecategory/givedisplayblock/exportcategory/sharecategory/importcategory` | Group G | ❔ — share/import cloud transport → G20 |
| G11-2 | Auto-categorize + category display-block services (`AutoCategorizeManager`, `CategoryDisplayBlockManager`) | R5 (category part) | ❔ — R5 colour service → G10-5 |
| G11-3 | Category Forge replacement — real category records, multi-category assignment, one main badge, unlimited tree/subcategories, `/cb create` Category tab, full in-game customization, delete modes, migration | 2026-06-30 owner design lock + old CustomBlocks inspiration | 🟡 cramped first `/cb create` sample rejected; wide workspace sample built in `StudioCategoryWorkspacePanel`; `/cb category` + `/cb categories` still old and must be rebuilt next |

## G12 — Export / Marketplace

| New | Title | was | Status |
|---|---|---|---|
| G12-1 | Universal Export Dashboard — export system rework | 17.15 · Dec §11 | ❔ |
| G12-2 | Marketplace + share/import — `sharecategory/importcategory/exportblock/importblock/market` | Group H | ❔ — cloud transport → G20 |
| G12-3 | Export formats — litematic + schem + standalone vanilla resource pack | Dec §H | ❔ |

## G15 — AI Textures

| New | Title | was | Status |
|---|---|---|---|
| G15-1 | AI texture generation — stubs → real | 17.22 | 🛠️ partial / parked (memory) |
| G15-2 | Keyless AI generator | P3 | 💬 — provider pivot under discussion |
| G15-3 | AI provider | Dec §D (Pollinations.ai) | 💬 — superseded by Cloudflare Flux Schnell pivot (not built) |

## G16 — Diagnostics

| New | Title | was | Status |
|---|---|---|---|
| G16-1 | `/cb diag` + `/cb incidents` rework → IT Chest Dashboard | 17.24 | ⚠️ partial — 3-row `DiagMenu` exists (not 6-row); incidents text-only |
| G16-2 | Debug Log viewer — `DevConsoleScreen` merge into IT Chest | P2 · 17.17 | 💬 deferred slice |
| G16-3 | History / mutation log (`HistoryTracker`/`MutationLog` + `HistoryMenu`) | R1 | 🛠️ built in code — awaiting in-game (R1 GUI regressions → G02) |
| G16-4 | Admin tools — audit, cache, screenshot | Group M (admin part) | screenshot ❌ dropped; audit/cache ❔ |
| G16-5 | Particle / sound toggles (`/cb particles`, `/cb sounds`) | /cb particles · /cb sounds | 💬 — add particle FX first, then the toggle (none emitted today) |

## G17 — Regressions

| New | Title | was | Status |
|---|---|---|---|
| G17-1 | Command regression table — restore regressed commands | Group N | ❔ — GUI regressions also touch G02 |
| G17-2 | `fav` / `favorite` alias | 17.13 | ❔ — legacy table also flagged G25? |

## G18 — Notes / Staging

| New | Title | was | Status |
|---|---|---|---|
| G18-1 | Block note UX | 17.12 | ❔ |
| G18-2 | Note rework into Book GUI | P5 | ❔ |
| G18-3 | Draft / publish + staging | 17.14 · P6 | ❔ — confirm P6 title against group doc |

## G19 — Display

| New | Title | was | Status |
|---|---|---|---|
| G19-1 | Showcase blocks | Group J | ❔ — FX-23 showcase/cycler = G14-1 (cross-ref) |
| G19-2 | Hologram preview — `/cb preview` + offhand | Q1 | ❔ |
| G19-3 | Ambitious display features (kept) | Dec §I | 💬 |

## G20 — External Integrations

| New | Title | was | Status |
|---|---|---|---|
| G20-1 | Whole-block cloud share — upload/download by code | Phase 14 (block vault) · Dec §G | 🔍 the real gap — block upload/download still stubs |
| G20-2 | Discord notification system — embeds, event hooks, toggles | Phase 14 (Discord) · Dec §N | 🔍 stub — plain text only today |
| G20-3 | Block Network — marketplace / cross-server library / community / external creation / owner control plane | owner brainstorm 2026-06-21 (32 features greenlit) | 💬 designed (phased) — nothing built this pass |

## G21 — Config GUI

| New | Title | was | Status |
|---|---|---|---|
| G21-1 | In-game Config GUI ("Settings Book") — tabbed, plain-English, all settings | 17.23 | 💬 designed (D1–D12 locked 2026-06-22) — not built |
| G21-2 | Long text inputs via Anvil GUI | P4 | 💬 designed |

## G22 — Permissions

| New | Title | was | Status |
|---|---|---|---|
| G22-1 | Permissions system — LuckPerms + Fabric Permissions API, vanilla OP fallback | Q7 · Dec §F | 💬 DEFERRED 2026-06-22, no code — owner owes 8 tier rulings (memory) |

## G23 — Player Experience

| New | Title | was | Status |
|---|---|---|---|
| G23-1 | First-join welcome — Starter Guide book + first-join detection | P7 (book part) | 🛠️ engine built, **unwired** — tutorial screen → G27 §G27.16 |
| G23-2 | Sample blocks + `FirstUseHints` + `TipPool` | R3 (player-experience) | 🛠️ engine built, **unwired** — distinct from G13's R3.x Arabic round-3 |
| G23-3 | Achievement engine — milestones first; tracking + title/chat unlock + persistence | Q4 | 🛠️ engine built, **unwired** — gallery screen → G27 §G27.16 |

## G24 — Macros

| New | Title | was | Status |
|---|---|---|---|
| G24-1 | Macro system improvements — chest GUI, step labels, single-undo batch, `/cb macro on·off` toggle | Phase 13 macros | 🛠️ Phase 13 build-verified; improvements ❔ |
| — | `script` / `scriptgui` / `run` | extended-macro idea | ⛔ scrapped (SWEEP_INDEX §A) — macros cover the need |

## G26 — Name & Give Fixes

| New | Title | was | Status |
|---|---|---|---|
| G26-1 | Clean display names (underscores → spaces + Title Case) | FIX A | ✅ confirmed in-game 2026-06-15 |
| G26-2 | `/cb give <id>` case-insensitive | FIX B | ✅ confirmed in-game 2026-06-15 |
| G26-3 | Named-texture mirror (`textures_names/`) | Part C | ✅ confirmed in-game 2026-06-15 |
| G26-4 | Multiplayer display name — block + item read the synced cache | FIX D | ⏳ NOT built — root cause confirmed 2026-06-20 |

---

> **Every group G01–G30 now has a `G##-n` map.** Pass 2 rows are **drafted only** — owner approves, then
> they're written into the real files group-by-group (the additive Path A→B method, code blast radius ≈ 0).
