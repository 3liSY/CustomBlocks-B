# Finale Fix — Sweep Index (ownership spine)

> Created 2026-06-21. Purpose: the single source of truth for **which group owns which command/feature**,
> so group docs stop bleeding into each other (e.g. `/cb incidents` showing up in G04 when it belongs to G16).
>
> Base map: `Reports/GROUP_01_AUDIT_REPORT.md` §A (command → group) and §E (group → coverage).
> This file layers the **developer decisions (2026-06-21)** on top and tracks the per-group sweep.
>
> **Sweep = 4 steps per group:** (1) Ownership — foreign commands become cross-refs, not duplicate tests ·
> (2) Reality — read live code, fix stale specs · (3) Verdict — boxes match built+tested state · (4) Follow-ups captured.
> Status key: ⬜ not tested · 🟡 partial · ❌ fail/scrapped · ✅ confirmed in-game.

---

## A. Decision deltas (apply everywhere)

These override the original audit. Any group doc still assuming the old state is STALE and gets corrected on sweep.

| Item | Decision (2026-06-21) | Affects |
|---|---|---|
| `magicitems` | RESTORE | G25 (was "❓ keep or scrap") |
| `editmagicitems` | RESTORE but **full revamp** (not a port) | G25 |
| `script` / `scriptgui` / `run` | **SCRAP** — Macros replace them | G24 |
| `dress` | **DROP** | G10/G19 (already ❌ in G10) |
| `video` / `extract` | **REMOVE** the command (`VideoCommands.java`) | G14 (animation/GIF stays; the video-file command goes) |
| `draft` / `drafts` / `publish` (staging) | **REMOVE entire staging system** | G18 → loses its staging reason; G18 = notes only now |
| `admingui` | **REMOVE** | (new-only command) |
| `incidents` | **KEEP** — owned by G16 | G16 (viewer); G04 only *routes* errors into it |
| Config renames | CONFIRMED correct, in code: `textureSize`, `httpPort`+`httpHost`, `aiTextureEnabled` | G21 |
| `[CB]` chat prefix | **KEEP on chat**; removed only from hotbar popups | G04 |
| Block editor | Screen-based Block Studio Edit tab (§G27.12) — **not yet built; still old chest** | G02 (fail) / G27 |
| `magicitems` / `editmagicitems` | **MOVED G02 → G25** (G25 owns; `editmagicitems` full revamp) | G02, G25 |
| Block-edit `history` | **Feature owned by G25** (decision A); G02 + G16 keep GUI surfaces only | G02, G16, G25 |
| `favorite`/`fav`/`unfavorite` + `recent` | **Feature owned by G25**; **moved out of G17 entirely 2026-06-21** (former tests G17.6–G17.8, G17.12) | G17, G25 |
| `screenshot` | **DROPPED in G16**; if revived → **G23** owns | G16, G23 |
| `particles`/`sounds`/`feedback` | **G16 owns** (merged Feedback FX, built); stripped stale G19/G25 claims | G16, G19, G25 |
| `showbrokenblocks` | **MOVED G09 → G16** (diagnostics, decision B) | G09, G16 |
| `settabicon` | **G25 owns** (decision C); removed from G06 | G06, G25 |
| Macro toggle `on`/`off` | **RENAMED → `/cb macro on`/`off`** (collided with G03 HUD toggle, decision D) | G03, G24 |
| `script`/`scriptgui`/`run` | **SCRAP** (macros replace) | G24 |
| Staging `draft`/`drafts`/`publish`/`stage`/`release`/`staging`/`resume` | **SCRAP entire system**; G18 = notes only | G18 |
| `shapeeditor` screen | **G27 owns the screen** (folded into Studio §G27.11, standalone retired, decision E); G08 owns shape logic. ⚠️ duplicate command registration in code — dedupe | G08, G27 |
| Chat download links | **REMOVE IP/host from every chat download button** (public mod must not leak server address); current `http://<host>:<port>/export/…` links also fail (connection refused). Confirmed call sites: `ResourcePackServer.getPngUrl()` (G10.5 `exportpng`, G12 PNG), `getZipUrl()` (G11.6 `category export`, G12 ZIP). **✅ DELIVERY DECIDED 2026-06-25 (owner): BOTH — (1) host-local link uses `127.0.0.1`/`localhost` (works for the single-player host, leaks no IP); (2) for sharing/remote players, upload to the deployed Cloud Vault (`cb-cloud-vault`) + post the public worker URL. No raw server IP/host ever posted. NOT built — design only.** | G12 (.2–.5), G10 (.5), G11 (.6), anywhere a `[download]` link is posted |

---

## B. Group → owns (corrected coverage)

| Group | Theme | Owns (commands/features) |
|---|---|---|
| G01 | Legacy Audit | (doc only — done) |
| G02 | Chest GUI | `gui`, `menu`, `dashboard`, `editor`, `listgui`, back-stack, undo/redo browser GUIs, `rp pause/resume`, `sync`, `unsuppress`, `history` GUI surface (feature→G25). ~~magicitems~~→G25 |
| G03 | HUD / ESC | `edithud`, `on`/`off` (HUD toggle — macro keeps `/cb macro on/off`), HUD drag editor + HUD config |
| G04 | Chat | message tone, DidYouMean, `help` GUI, `welcome`, error→incidents *routing* |
| G05 | Resource Pack | `resourcepack`, `rp`, silent delivery, RP config fields |
| G06 | Tools | Omni-Tool (`chisel`/`brush`/`deleter`), tool-give shortcuts (`square`/`triangle`/`rectangle`/`hexagon` = item give, NOT shape-create). ~~settabicon~~→G25 |
| G07 | Bulk Ops | `bulk` + all `bulk*`, `confirm`, `bulkgui` |
| G08 | Shapes | shape logic/commands: `setshape`/`addshape`/`removeshape`/`clearshape`/`shapelist`/`shapepreview`/`customtriangle`/`trianglemode`/faces. `shapeeditor` **screen**→G27 (logic stays here) |
| G09 | Backup/Safety | `backup`, `restore`, `safety`, `panic`, `expiry`, `deletedblocks`, `recover`. ~~showbrokenblocks~~→G16 |
| G10 | Color/Image | `colors`, `customcolor`, `gradient`, `palette`, `bgstudio`, bg-removal config (`dress` DROPPED) |
| G11 | Category | `blockscat`, `givecategory`, `importcategory`, `sharecategory`, category GUIs |
| G12 | Export & Import | export rework, `importfolder`, import run recall (`market`/`marketplace` → G20, restore → G09, since 2026-07-30) |
| G13 | Arabic | `arabic` word/letters/auto-join/font |
| G14 | Animation | GIF/animation, `pause`/`resume` of animation; **`video`/`extract` REMOVED** |
| G15 | AI Textures | `ai` command + AI config |
| G16 | Diagnostics | `diag`/`diagnostics`, `cache`, `audit`, `clear`, **`incidents` (viewer)**, **`showbrokenblocks`** (←G09), **`particles`/`sounds`/`feedback`** (merged Feedback FX). `history` displayed here (feature→G25). `screenshot` DROPPED |
| G17 | Regressions | cross-cutting regression checks only (undo/redo/give args/search/delete#). favorite/recent **fully moved → G25** (2026-06-21) |
| G18 | Notes | `note` rework only — **staging system fully REMOVED** |
| G19 | Display | `showcase` (+`config`/`item`/`list`/mgmt/bulk), `preview` (hologram, ←old `hologram` stub), offhand projection. ~~particles~~→G16. *(stale audit overlap removed: faces→G08, text→G13, givedisplayblock→G11, badges→n/a)* |
| G20 | External | `sync`*, `vault` (+`upload`/`download`), cloud/`discord` config |
| G21 | Config GUI | `config`, `settings`, `reset`, session config |
| G22 | Permissions | full permission model |
| G23 | Player Experience | `welcome`, `help`, `achievements`, `show`/`showcase`*, `screenshot` (owns if ever built) |
| G24 | Macros | `macro` (works) + `/cb macro on/off` toggle; `script`/`scriptgui`/`run` SCRAPPED |
| G25 | Block Mgmt Extras | `find`, `reid`, `swapid`/`swapname`, `dupe`/`duplicate`, `setdrop`/`cleardrop`, `settabicon` (←G06), `recent` (←G17), `favorite`/`fav`/`unfavorite` (←G17), `history` feature (←G02), `magicitems`/`editmagicitems`(+revamp, ←G02), editor PNG button. ~~sounds~~→G16 |
| G27 | Screens | Block Studio + screen design system. Owns the **screens** for `shapeeditor` (←G08), `recolor`/`livecolor` (←G10), arabic preview (←G13), HUD templates (←G03) — logic stays with each owner |

\* = contested / appears in two groups — resolve on sweep (see §C).

---

## C. Contested items — RESOLVED (sweep 2026-06-21)

- ✅ **`sync`** — **G02 owns the command** (`/cb sync`, pack push); **G20 owns vault sync** (cloud backup). G05 "sync" was a prose false-positive (no command).
- ✅ **`history`** — **G25 owns the feature** (block-edit mutation log); G02 + G16 keep GUI **surfaces** only (decision A).
- ✅ **`welcome` / `help`** — **G04 owns** both. G23 only references them in help-page prose.
- ✅ **`unsuppress`** — **G02 owns** (pack notifications). Not G04/G16.
- ✅ **`settabicon`** — **G25 owns** (decision C); G06 cross-refs.
- ✅ **`showbrokenblocks`** — **G16 owns** (decision B); G09 cross-refs.
- ✅ **`on`/`off`** — **G03 = HUD toggle**; macro toggle renamed **`/cb macro on/off`** (decision D).
- ✅ **`shapeeditor`** — **screen = G27** (folded into Studio, decision E), **logic = G08**. Code has a duplicate registration to dedupe.
- ✅ **`discord`** — **G20 owns the command/integration**; **G21 owns editing its config fields** via the Config GUI. Both legit, no move.
- ✅ **`showcase`** — **G19 owns it fully** (command + config GUI + rendering), resolved at G19 design-lock 2026-06-21. G23 only cross-refs (`show` player-facing helper, if ever built).

---

## D. Sweep tracker

> **Swept column:** ✅ = full test-sweep done · partial = partial · 📋 = **ownership/duplicate sweep done (doc-only, 2026-06-21)** for a not-yet-built group · — = untouched.

| Group | Tested | Swept | Notes |
|---|---|---|---|
| G01 | ✅ | ✅ | Audit signed off; decisions recorded in report §F |
| G02 | ✅ | ✅ | 8✅ 1🟡(.8 history split) 1❌(.3 editor screen) 1 scrapped(.4); **ownership sweep 2026-06-21: magicitems/editmagicitems→G25, history feature→G25 (G02 keeps GUI surface)** |
| G03 | — | — | not built |
| G04 | ✅ | ✅ | 10✅; prefix spec corrected; help/welcome rework + G04.4 ownership noted |
| G05 | — | — | delivery broken on modded clients |
| G06 | 🟡 | ✅ | 8✅ 1❌(.6 hand glow broken); Omni-Tool full rework + deleter polish; .7/.8 specs corrected; **ownership sweep: settabicon→G25; square/triangle/etc clarified = tool-give (not G08 shapes)** |
| G07 | 🟡 | ✅ | Built slice (.1–.4, .9) all ✅; .5–.8 NOT BUILT; rework wanted on .1/.3/.9 |
| G08 | — | 📋 | not built. **Ownership sweep 2026-06-21: shapeeditor SCREEN→G27 (logic stays G08); duplicate code registration flagged** |
| G09 | — | 📋 | not built. **Ownership sweep 2026-06-21: showbrokenblocks→G16** |
| G10 | ✅ | ✅ | dress DROPPED (confirmed removed in code); G10.5 export link host-leaks (cross-cutting §A); gradient/bgstudio/palette/resize/variants ✅; follow-ups logged |
| G11 | ✅ | ✅ | scattered category cmds unified → `/cb category <action>` (confirmed in code); 15✅; G11.6 export link host-leaks (§A); `bulkretexture` tile = dead (never built); follow-ups logged. **G11-3 design LOCKED 2026-06-30:** Category Forge replacement direction (`/cb create` left Category tab, real category records, multi-category assignment, one main badge, unlimited tree/subcategories, full customization, delete modes, safe migration). Prototype `docs/mockups/category_create_tab_v2.html`; cramped first `/cb create` sample rejected; wide workspace sample built in `StudioCategoryWorkspacePanel`; `/cb category` + `/cb categories` still old and must be rebuilt next. |
| G12 | 🟡 | ✅ | .1✅(spec fixed) .6✅(rework) · .2❌ .3/.4/.5🟡 — all blocked by IP-leaking + broken download links (cross-cutting, see §A) · .7/.8 deferred |
| G13 | ✅ | ✅ | COMPLETE — Round 3 6/6, auto-join, Build A+B, lag/recolour all ✅; 3 micro-checks ✅ 2026-06-21; G13.9 (`arabic text`) scrapped. Deferred low-pri: Hide (§13), Type-a-word (§14) |
| G14 | 🟡 | partial | PARTIAL — **parked by owner 2026-06-21, revisit later**. Part A confirmed; renderer mid-revert (ADR-012). **Sweep: `/cb video`/`/cb extract` standalone commands to be removed (animation/GIF + video-as-source stay) — doc noted, code pending** |
| G15 | — | — | not built |
| G16 | — | 📋 | design only. **Ownership sweep 2026-06-21: gained showbrokenblocks (←G09), owns particles/sounds/feedback (Feedback FX) + incidents viewer; screenshot dropped→G23; displays history (feature G25)** |
| G17 | — | 📋 | not built. **Ownership sweep 2026-06-21: favorite family + recent = regression checks only; feature owned by G25** |
| G18 | — | 📋 | staging SCRAPPED; **design locked 2026-06-21** (interview + 4-slice plan, no code) — Lore=writable book, item-tooltip needs client sync, Share=vault upload+import, OP-only. Spec §🔒 Locked Decisions |
| G19 | — | 📋 | **design LOCKED 2026-06-21** (interview, no code) — Display Entities (BlockDisplay/ItemDisplay), 8-slice plan, OP-only, full customizable showcase + `/cb preview` + offhand (last). Spec §🔒 D1–D20; testing guide created. **G19 owns showcase fully** (resolves §C vs G23). particles→G16. Parked: shop/redstone/proximity, glass-case+shelf |
| G20 | — | 📋 | **design LOCKED 2026-06-21** (interview + 2 feature waves, no code) — Cloud Vault whole-block share (reuse `/category` zip, static+animated), conflict warn+override+**multi-level undo/redo**, backup→cloud sync, HMAC signing + UUID/server-id **control plane** (per-player/server allow-deny/quota/ban/kill, owner-side worker), Discord **fully customizable** (8 events, embeds+texture, toggles/templates/colors/identity/role-ping, chat now+GUI@G21). **32 features greenlit → phased**: G20 core · G20.5 marketplace · G30 community/AI · G31 external creation · G32 owner platform · worker track W1–W8. cloudShareEnabled added; OP-only upload; ship owner vault ON. Hard truth: public jar = url/secret extractable → abuse-resistance not secrecy. Stale config names (`vaultEndpoint` only) + G21.x→G20.x renumber fixed. Spec §🔒 D1–D14 + roadmap; testing guide created (6 slices ⏳). Wave-3 brainstorm parked |
| G21 | — | — | not built |
| G22 | — | — | not built |
| G23 | — | 📋 | not built. **Ownership sweep 2026-06-21: owns welcome/help (G23 refs only) + screenshot (if built)** |
| G24 | — | 📋 | macro works. **Sweep 2026-06-21: script/scriptgui/run SCRAPPED (doc cleaned); toggle renamed `/cb macro on/off`** |
| G25 | — | 📋 | not built. **Ownership sweep 2026-06-21: GAINED magicitems/editmagicitems(←G02), history feature(←G02), favorite family + recent(←G17), settabicon(←G06); LOST sounds→G16. New specs+tests needed when built** |
| G26 | — | — | name/give fixes |
| G27 | 🟡 | partial | PARTIAL — parts built (5 Studio fixes + HUD templates), **parked by owner 2026-06-21, revisit later**. Edit mode/Paint/Recolor fold-in/toasts ⏳ not built |
| G28 | — | — | create studio |
| G29 | — | — | creator tools |
