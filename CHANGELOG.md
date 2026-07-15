# Changelog

> What shipped, newest first. One entry per slice.
> **Golden Rule:** 🟢 built ≠ done — nothing is ✅ until the owner confirms it in-game.

**Status key:** ✅ confirmed in-game · 🟢 built, gates green (✋ awaiting in-game test) · 📋 planned · ⛔ reverted

**At a glance (33 slices):** ✅ 8 confirmed · 🟢 23 built / ✋ pending · groups touched: G06 · G07 · G08 · G09 · G10 · G13 · G14 · G16 · G18 · G20 · G25 · G27 · G29 · G30 · G31 · G32

---

## Index — newest first

| Date | Group | Slice | Status |
|---|---|---|---|
| 2026-07-14 | G32 | **Explosive Tomato — Phase A, "Fly"** (§A) — a **throwable tomato**, hand-modelled in 3-D (a round, faceted fruit with a green top, not a flat sprite). Get it with **`/cb tomato @s 64`** — or `/cb tomato all` for everyone — or grab it from the **CB tools tab** (it isn't craftable). It throws like a snowball with **no cooldown**, so you can spam it, and it leaves **no trail** — nobody sees it coming. **Sprint-jump and throw** and it carries your speed, for trick shots. **Sneak + throw and you *ride* it** — faster, flatter, and there is **no getting off**: you go where it goes. **Dispensers fire it** like a cannon, and a chest full of them **breaks safely** — they just drop as items. ⚠️ **It does not explode yet** — it vanishes on impact, and gives up after 10 seconds in the air. The boom, the sauce and the crater are the next phases | 🟢 ✋ |
| 2026-07-14 | G04 | **Bulk commands stop lying · redo chip · Edit says "coming later"** (§A3 · §A4 · §A7) — **you can now type `/cb bulkdelete id1 id2 id3` with plain spaces**, the way you'd expect, on every bulk command (commas and `"quotes"` still work). The old filter words — `category:` `id:` `name:` `favorite:` `locked:` — are **gone from the help text too**: they'd already been removed from the engine, but four commands kept advertising them, so typing one matched **zero blocks** while chat still told you it was valid. Undoing something now gives you a **↪ Redo** chip on the very same line, so delete → undo → redo is one click each way. The **✎ Edit** chip now tells you the editor screen is **coming later** instead of dropping you into a bad menu — the screen it used to open answered "edit" by closing itself and typing half a command into your chat box. ⚠️ **Known:** the Bulk Hub's Console tab still builds the old filter words and will match nothing — fix pending (G07-HUB-FILTER) | 🟢 ✋ |
| 2026-07-14 | G03 | **HUD widgets + ESC quick-access panel** (§G03-2) — **7 new HUD widgets**: the active-tool chip, a **macro-recording banner** that pulses red/white so you can't finish a build still recording, a **small padlock beside the crosshair** (never over it — it must never block your aim) when you look at a locked block, an incidents ping, a favourites strip (Previous · Current · Next), plus a Buzzer timer and Vault toast. Widgets **only show state — you never click them** (there's no cursor while you're playing); each one's action is the command it already had. Press **ESC → CustomBlocks ▸** for a new **Quick-Access Panel**: every block as a spinning 3-D thumbnail, one click to open its editor — and clicking a **⊙ View** chip in chat makes an already-open panel **jump to that block**. Configure everything in the new **Widgets** tab of `/cb edithud`: on/off, drag to position, and a colour picker that opens with **curated swatches** (full RGB is one click deeper). An **op can pin a widget for everyone**, and a pinned widget can't be hidden locally. The Buzzer + Vault widgets have no backend yet, so they're **dev-only** and marked "demo data" — you will never see fake numbers | 🟢 ✋ |
| 2026-07-14 | G04 | **Interactive chat + "did you mean" fixed + one voice everywhere** (§G04-2 · §G04-3 · §G04-4) — messages now carry **clickable chips**, curated per message: creating a block offers **✎ Edit** and **⊙ View**; deleting one offers **↩ Undo**; sharing one offers **⇪ Share** (copies the code). Destructive commands ask **✔ Yes / ✖ No** right in the line, and Yes runs immediately. Clicking a chip runs the same command you'd type, so chips in old chat **never stop working**, and **↩ Undo undoes *your* last action**, not the other player's. **`/cb anim` now suggests `/cb animation`** — it used to suggest `/cb gui`, because the suggestion list was hand-maintained and **67 of 119 commands were missing from it** (including `animation`). Suggestions and `/cb help` now read the real command list, so nothing can go missing again. **Errors never dump a Java exception at you** any more: you get a plain sentence, a short code you can paste into a bug report (`E-45`), and a **⊙ Details** link that opens that exact incident. New **`/cb clearlogs`** clears only *your* `[CB]` lines and leaves player chat alone. Colours are now consistent mod-wide (grey always means "side detail", aqua **only** ever means "clickable") | 🟢 ✋ |
| 2026-07-12 | G07 | **Bulk Operations Hub — rebuilt** (§G27.22 / §G27.22b) — the Hub is now **two tabs**. **Blocks List** shows your blocks as a grid of **slowly-spinning 3-D cubes** with the name and `id:` under each; filter chips (**All · ★ Favorites · Locked · Selected**), a **Sort ▾** (Name · ID · Newest · **Color**), and right-clicking a block opens its details in a panel on the **left**. **Bulk Actions** puts all **ten** operations down the left rail and gives every targeted block its own row: an **include checkbox**, its **old cube → new cube**, and the exact value that will change — locked blocks are greyed and marked "will skip". A **RESULT PREVIEW** panel on the right shows a before→after sample plus a running count. **Execute** confirms in the window, then wipes a red bar across the list as it applies. **NEW: Recolor** — hue-shift every selected block's texture at once, undone by a single `/cb undo`. *(Removed: the old Extra/Health tab, the filter builder, and the per-block Re-ID boxes — Re-ID is now one pattern like `planet_{n}` → `planet_1`, `planet_2`…)* | 🟢 ✋ |
| 2026-07-10 | G07 | **Bulk Workbench Screen** (§G07-3) — the four chest bulk menus and the chest block list are **gone**, replaced by one window. `/cb list` opens a searchable **Browse** tab (left-click ticks, right-click shows a block's details, "Open full editor"); `/cb bulk` (new alias) opens the **Bulk** tab: nine operations down the left, your blocks in the middle, and a live **`old → new` preview** on the right that shows exactly what will change — including which blocks will be skipped, and why — *before* you press Apply. Apply always asks for confirmation **in the window**, never in chat, and the list refreshes in place. **Re-ID** is usable from a GUI for the first time. A `[filter ▾]` narrows the list; when it matches more than you ticked, one button widens the job to all of them. **Fixed:** category and rename text containing a **space** (`red stone`) used to be silently mis-parsed by the old GUI | 🟢 ✋ |
| 2026-07-10 | G31 | **BuzzerGame core-4 build** — one **BuzzerGame Wand** replaces the link wand + resize tool (sneak-cycle **Link · Resize · Rotate · Digit-style**; right-click air flips direction); the **timer stand** now **rotates 5°/click** and shows **LED 7-segment** digits (the wand toggles LED ↔ plain text); right-click a panel opens a branded **admin Screen** (Start/Stop/Reset-with-confirm/Reveal + Mode/Format/Countdown/False-start/Target + link-list unlink) and `/cb buzzergame` trims to `give·start·stop·reset·reveal·help·size`; **Duel/Party** rounds rank everyone (winner + 2nd on screen, full list in chat, disqualified listed separately) in the **lime/red** brand | 🟢 ✋ |
| 2026-07-09 | G04 | **Chat unification slice 2** (G04-3: D7·D6·D1·D3) — every id/name now reads in **double quotes** `"x"`; **hotbar popups** are green = success, red = error, white = neutral (the gold corner-select outlier is gone); **every chat line carries `[CB]`**, including list / help / diagnostics rows that used to slip through unbranded; and **every clickable button is one aqua style** with a per-type icon (**▶** run · **⧉** copy · **✎** fill-in) and a fixed hover ("Click to run: …") — the 8 private button helpers are deleted | 🟢 ✋ |
| 2026-07-09 | G30 | **Guess Mode — Glow scrapped · blocks drop · "?" debris** — the Showcase **Glow toggle is removed** (full-bright never read as a glow in daylight; the tab is now Core spin · Size). **Custom blocks now drop their own item** when broken in survival (they had no loot table, so they dropped nothing). Breaking a **disguised** block as the flagged holder now throws **"?"-textured debris** instead of nothing — the real texture still never leaks, and watchers see the real particles | 🟢 ✋ |
| 2026-07-08 | G30 | **Guess Mode — Showcase display** (G30-8b) — `/cb guess showcase spawn [block]` drops a floating **end-crystal-style display** one block above you: a spinning picture core + an orbiting outer layer, gently bobbing (no id → the "?" cube). Tune it live on the new **Showcase** tab of `/cb guess settings` — inner spin, outer orbit, size, plus **Glow** and **Particles** toggles (one shared look for every display). Remove one by **sneak + right-click** or `/cb guess showcase delete`. Op-only; walk-through; survives world reload | 🟢 ✋ |
| 2026-07-08 | G30 | **Guess Mode — type pose angles by number** — on the Guess Settings **Pose** tab you can now **click a slider's value and type an exact angle** (decimals like `-28.6°`), **scroll** or **↑/↓** to nudge ±1°, and **right-click a slider** to reset just that axis; out-of-range typing is clamped silently. (Also: the tabbed mega-screen rebuild and the sharp red-on-black "?" fallback texture were confirmed in-game.) | 🟢 ✋ |
| 2026-07-07 | G30 | **Guess Mode — tunable pose** (G30-4 slice 1) — the centered two-handed guess pose is now owner-tunable and synced to everyone: `/cb guess pose <left\|right> <updown\|inout\|twist> <degrees>` (+ `show` / `reset`), op-only. Each arm independent; defaults = the previous pose so nothing changes until you tune it. Block-placement/animation knobs + a slider screen with live preview + presets come next | 🟢 ✋ |
| 2026-07-06 | G30 | **Guess Mode — links removed + bundled QuestionMark cube** — reverted the "paste an image/GIF **link**" input (`defaultblock link` / `look link`); disguise looks are **existing block ids only** now. The last-resort fallback (no default, no per-round look) is a new bundled **QuestionMark** cube (glossy red "?") instead of the old purple "?". Smooth pickup/put-down pose transition (K1–K3) confirmed in-game | ⛔+✅ |
| 2026-07-06 | G31 | **BuzzerGame — link wand + first playable round** — link buzzers to a panel with the new **Link Wand** (`/cb buzzergame give wand`); run a **Precision Stop** round (3-2-1-GO, timer counts up on the action-bar, buzz freezes your time) or a **Reaction Race** (fastest after GO wins, configurable false-start rule); host reveals the winner with `/cb buzzergame reveal`. New verbs: `reveal · game · target · countdown · falsestart`. Timer shows near the panel (multiblock screen still to come) | 🟢 ✋ |
| 2026-07-06 | G31 | **BuzzerGame Phase 1 item 2** — admin panel block + `PanelSession` state machine (Idle→Countdown→Running→Results→Finished · Solo/Duel/Party) persisted on the panel; op-only `/cb buzzergame give panel · start · stop · reset · advance · mode · state` (act on the panel you look at) + right-click readout · _(commands to be revamped later — owner feedback)_ | ✅ |
| 2026-07-06 | G31 | **BuzzerGame Phase 1 item 1** — buzzer block (half-dome, `pressed` state) + server-side press detection (log + honk + particle + action-bar, auto-release); `/cb buzzergame give buzzer`, vanilla `/give customblocks:buzzer`, and a dedicated **BuzzerGame** creative tab | ✅ |
| 2026-07-04 | G27 | **Screens step 1** — every full-screen CB popup now wears the locked **red+black** look (#FF0000 red, pure black, lime only on "saved"); Recolor/Shape/HUD editors get the small **movable action bar** instead of the old full-width button strip | 🟢 ✋ |
| 2026-06-30 | [G29](#g29-shorts-overlay) | **Shorts framing overlay** — `F8` capture-invisible 9:16 guide + `F9` borderless helper; exclusive fullscreen sidebar fallback | 🟢 ✋ |
| 2026-06-30 | G10 | **`/cb colorvariants`** on an existing family **overwrites** its picture/colours behind `/cb confirm` — keeps glow/hardness/sound/collision/category/favourite + names (slice 2 piece 2) | ✅ |
| 2026-06-29 | G14 | **Paste-ANY-link fix (link-fixer)** — direct links (imgur) no longer bounce to a webpage; page/share links get their real image pulled out; per-site rules for Google Images/Drive, Dropbox, Twitter/X, GitHub (image-input overhaul **Layer 1**) | 🟢 ✋ |
| 2026-06-29 | G06·G10 | **Fix:** `/cb redo` after undoing a create kept its picture (was purple/black) · **`/cb colorvariants <id>`** rebuilds an existing block's colour family from its own picture (slice 2 piece 1) | ✅ |
| 2026-06-29 | [G10](#g10-cv-create) | **`/cb colorvariants`** — one link → a colour family (`<id>` + `_red`/`_green`/`_yellow`); create form only (Phase 1 slice 1) | ✅ |
| 2026-06-29 | G07·G10 | BgRemove keeps a plain **black** background (keyline + white-flip removed — content is composed on black) | ✅ |
| 2026-06-28 | [G06·G07·G09](#g06-14-slice3) | Bulk delete + broken-blocks GUI cleanup use the shared delete rail (no purple blocks, undoable) | 🟢 ✋ |
| 2026-06-28 | [G20](#g20-vault-codes-share-backup) | Vault code list (`/cb vault codes`) · category Share tile · backup→cloud sync | ✅ codes+tile · 🟡 backup (R2 setup) |
| 2026-06-28 | [G08·G25](#mp-bugfixes-shape-reid) | Bug fixes — shape hitbox on a dedicated server + `/cb reid` case mismatch | ✅ (cross 🟡) |
| 2026-06-27 | [G14](#g14-sharper-resize) | Sharper block pictures — Lanczos resize + sharpen + small-picture warning | 🟢 ✋ |
| 2026-06-27 | [G06](#norejoin-sweep) | NO-REJOIN sweep — undo-after-delete bug fixed; every live edit refreshes all players | ✅ |
| 2026-06-27 | [G06](#g06-14-rail) | Recycle-Bin delete — `/cb delete` + Deleter make markers (slice 2/5) | ✅ |
| 2026-06-27 | [G06](#g06-14-marker) | Recycle-Bin delete — Deleted-marker block + BlockEntity (slice 1/5) | ✅ |
| 2026-06-26 | [G06](#g06-tool-icon-green) | Colour tool icon now brightens to the hex (green→`#10FF01` fix) | ✅ |
| 2026-06-26 | [G06](#g06-recolor-existing) | `/cb config hex` repaints existing blocks (new + old) · colour tools glint | ✅ |
| 2026-06-26 | [G13](#g13-back-mirror) | Arabic back of word stays mirrored after breaking a middle letter | 🟢 ✋ |
| 2026-06-26 | [G06](#g06-tool-icon-tint) | Colour Square/Triangle icons re-tint cleanly to the configured hex | 🟢 ✋ |
| 2026-06-26 | [G06](#g06-config-hex-sync) | `/cb config hex` tool name updates live on dedicated servers | 🟢 ✋ |
| 2026-06-26 | [G13](#g13-arabic-gap) | Arabic letters touch — all six faces flush (gap fix v2) | ✅ |
| 2026-06-26 | [G06](#g06-custom-swap-instant) | Custom-colour Square swaps instant on dedicated servers | ✅ |
| 2026-06-21 | [G20](#g20-cloud-vau lt-s1) | Cloud Vault — share a block by code (S1) | 🟢 ✋ |
| 2026-06-21 | [G14](#g14-all-frames) | Animated blocks keep ALL frames (off-atlas grid) | 🟢 ✋ |
| 2026-06-21 | [G18](#g18-lore-v2) | Lore revamp v2 — single screen, replaces Book GUI | ✅ |
| 2026-06-21 | [G16](#g16-finish-pass) | Finish pass — bulk/pack FX, Debug Log viewer | 🟢 ✋ |
| 2026-06-21 | [G16](#g16-fx-centred) | Feedback FX board centred | 🟢 ✋ |
| 2026-06-20 | [G06](#g06-instant-swap) | Instant colour-Square swaps | 🟢 ✋ |
| 2026-06-20 | [G14](#g14-anim-polish) | Animation polish + render deferral | 🟢 ✋ |
| 2026-06-19 | [G13](#g13-delete-letters) | Delete static Arabic letters + reclaim slots | 🟢 ✋ |

## By group

| Group | Slices |
|---|---|
| **G06 Tools** | [instant colour-Square swaps](#g06-instant-swap) · [custom-colour swaps instant on dedicated](#g06-custom-swap-instant) · [config hex name syncs live](#g06-config-hex-sync) · [tool icons re-tint to hex](#g06-tool-icon-tint) · [config hex repaints existing blocks + tools glint](#g06-recolor-existing) · [Recycle-Bin delete — marker block (slice 1)](#g06-14-marker) · [Recycle-Bin delete — delete rail (slice 2)](#g06-14-rail) · [delete rail slice 3 — bulk + broken cleanup](#g06-14-slice3) · [NO-REJOIN sweep](#norejoin-sweep) |
| **G07 Bulk** | [bulk delete leaves no purple blocks (slice 3)](#g06-14-slice3) |
| **G08 Shapes** | [shape hitbox correct on a dedicated server](#mp-bugfixes-shape-reid) |
| **G09 Safety/Broken** | [broken-blocks cleanup leaves no purple blocks + undoable (slice 3)](#g06-14-slice3) |
| **G10 Color/Image** | [colour family from one link — `/cb colorvariants` (create form)](#g10-cv-create) |
| **G13 Arabic** | [delete static letters + reclaim slots](#g13-delete-letters) · [letters touch — gap fix](#g13-arabic-gap) · [back stays mirrored after breaking a middle letter](#g13-back-mirror) |
| **G14 Render** | [sharper resize + small-picture warning](#g14-sharper-resize) · [all frames (off-atlas grid)](#g14-all-frames) · [animation polish + render deferral](#g14-anim-polish) |
| **G16 Feedback FX** | [finish pass (bulk/pack FX, Debug Log)](#g16-finish-pass) · [board centred](#g16-fx-centred) |
| **G18 Lore** | [revamp v2 — single screen](#g18-lore-v2) |
| **G20 Cloud Vault** | [share a block by code (S1)](#g20-cloud-vault-s1) · [vault code list + Share tile + backup→cloud](#g20-vault-codes-share-backup) |
| **G25 Block Mgmt** | [`/cb reid` works whatever case you type](#mp-bugfixes-shape-reid) |
| **G29 Creator/Capture** | [Shorts framing overlay](#g29-shorts-overlay) |

---

## [Unreleased]

### <a id="g29-shorts-overlay"></a>G29 · Shorts framing overlay · 2026-06-30 · 🟢 built, ✋ awaiting in-game + OBS confirm
- **Added:** `F8` toggles a local Shorts guide over Minecraft: 9:16 center frame, dimmed cut-off sidebars, safe
  margins, subject band, and hollow caption box.
- **Added:** `F9` toggles the Windows borderless helper so the full guide can run as a transparent,
  click-through, always-on-top native window.
- **Capture safety:** the native overlay is marked `WDA_EXCLUDEFROMCAPTURE` through JNA/user32.dll. This still
  needs owner confirmation in OBS and a screenshot before it can be called done.
- **Fallback:** true/exclusive fullscreen closes the native overlay and draws only sidebar markers intended to
  disappear after a 9:16 center crop.
- **Config:** local settings persist in `config/customblocks/data/shorts-overlay-client.json`.

### <a id="g10-cv-create"></a>G10 · `/cb colorvariants` — one link makes a whole colour family · 2026-06-29 · ✅ confirmed in-game
- **Added:** `/cb colorvariants <id> <name> <link>` (also `/cb variants`) — paste **one** image link and it makes a
  matching set of blocks in one go: the normal block **`<id>`**, plus a **red** (`<id>_red`), **green** (`<id>_green`)
  and **yellow** (`<id>_yellow`) version, each with the picture on a coloured background — the same colours the
  Triangle tools use. It downloads the picture once, you get **all four blocks** handed to you, and **one
  `/cb undo`** removes the whole set. The coloured versions are live-swappable in the world with the Square tools,
  just like before.
- **This is the first, lean slice (create only).** Still coming in later updates: remake a family's colours from a
  block it already has, overwrite an existing family (held behind `/cb confirm`), delete a whole family, add extra
  colours or `all`, skip locked blocks, and a "Colour Master" achievement.
- **Heads-up:** if any of the four ids already exist, nothing is created yet (you'll be told which) — overwriting
  comes in the next slice. Pictures with a tricky edge can still show the known black-edge issue (G10-3); that's
  unchanged and improves when G10-3 is fixed.

### <a id="g06-14-slice3"></a>G06 · G07 · G09 · Bulk + broken-blocks delete now leave no purple blocks (slice 3) · 2026-06-28 · 🟢 built, ✋ awaiting in-game
- **Fixed:** deleting blocks in bulk (`/cb bulkdelete`, the Bulk dashboard Delete) used to leave any **placed
  copies** of those blocks as broken/purple blocks in the world until you reloaded the area (walk away + back,
  or rejoin). Now they turn into "Deleted: <name>" markers **instantly**, and the name on screen clears for
  everyone right away — same as deleting one block with `/cb delete`.
- **Fixed:** the **broken-blocks** screen's "Delete selected" (`/cb showbrokenblocks` → tick → Delete) had the
  same problem, and on top of that wasn't undoable. Now it leaves no purple blocks, clears live, and you can
  bring the blocks back with `/cb undo` (they're still in `/cb deletedblocks` too).
- *(Under the hood: both now go through the one shared delete path — `DeletionService` — so every kind of
  delete behaves identically.)*

### <a id="g20-vault-codes-share-backup"></a>G20 · Vault code list, category Share tile & backup→cloud sync · 2026-06-28 · ✅ codes + Share tile confirmed · ✋ backup→cloud built (R2), awaiting R2 setup + test
- **Added ✅:** `/cb vault codes` — a running list of every share code made on the server (block, category, and
  lore shares), newest first, each with a click-to-copy button, so you never lose a code you shared. Saved to
  disk, survives restarts. *(Confirmed in-game 2026-06-28.)*
- **Added ✅:** the category editor's **Share** button now works (was greyed "coming soon") — it uploads the
  category and gives you a code, the same as `/cb category share`. *(Confirmed in-game 2026-06-28.)*
- **Added 🟡:** `/cb backup save` now also sends the backup to your cloud and gives you a code (only when
  cloud sharing is on and your vault URL is set). The local backup always saves first; a cloud hiccup never
  blocks it, and auto-backups stay local. Real backups are large (~150 MB), so they now upload to **R2** cloud
  storage **straight from the mod** (no size limit), not through the small share store. **One-time setup needed**
  before this works — turn on R2, make a bucket, paste 4 values into the worker (steps in
  [cloudflare/SETUP.md](cloudflare/SETUP.md)). Backups self-expire after 3 months; no size cap.
- **Changed:** shared **categories and notes no longer expire** — they're a permanent library now (they used to
  self-delete after 30 days). Applies to shares made *after* you redeploy the worker; codes shared before that
  keep their old 30-day clock.
- **Cleanup:** removed two dead internal methods that never did anything (no player-visible change).

### <a id="mp-bugfixes-shape-reid"></a>G08 · G25 · Bug fixes — shape hitbox on a server + `/cb reid` case · 2026-06-28 · ✅ confirmed in-game (cross hitbox = partial, accepted)
- **Fixed (G08):** on a multiplayer server, a shaped block (carpet/slab/…) showed the right picture but its
  **click/selection box was still a full cube**. Now the box matches the shape for everyone, and updates live
  when you `/cb setshape` (no rejoin). *(Cause: the client read the shape from server-only data that's empty
  on a real server; it now reads the synced shape.)*
- **Fixed (G25):** `/cb reid <old> <new>` failed ("Couldn't change…") when you typed the **old id in a
  different case** than it was stored. It now works whatever case you type, and you can also re-case an id.
- **Fixed (G08/G25, follow-up):** the same case problem hit **every** edit command — `/cb setshape`,
  `setglow`, `sethardness`, `setsound`, `setcollision`, `setcategory`, `setanim`, `rename`, `dupe` all
  errored on a case-mismatched id. Fixed at the root so they work whatever case you type.
- **Fixed (G08, the real shape bug):** shaped blocks had the **wrong hitbox** — a pillar/thin/stairs/pane
  block had a full-cube collision, so it **pushed you out** and you couldn't stand in its open parts; the
  selection box was a full cube too. Now the collision + selection box match the shape exactly, for everyone,
  and update live when you `/cb setshape`. (Cause: the block shape was being cached as "full"; now read live.)
  **✅ Confirmed in-game 2026-06-28** — pillar/thin/stairs/pane no longer push, slab good. **🟡 Cross = partial:**
  walk-through is correct but its selection box is a coarse box not an X — accepted as-is (low-use shape).

### <a id="g14-sharper-resize"></a>G14 · Sharper block pictures — better resize + small-picture warning · 2026-06-27 · 🟢 built, ✋ awaiting in-game
- **Changed:** the mod now enlarges pictures with a sharper method (Lanczos) plus a light sharpen, instead
  of the old soft stretch — so blocks made from a small picture look cleaner and less mushy. Automatic on
  every `/cb create` and `/cb retexture`/`retextureall`, for every player, no setup.
- **Added:** a friendly heads-up in chat when the picture you give is smaller than the block size ("that
  picture is only 128×128px… use a picture at least 512px wide"). It doesn't stop the create — just warns.
- Transparent logos no longer pick up a dark/coloured ring around the edge when resized.
- **Honest limit:** this is **not** AI — it makes edges crisper but adds no new detail. Tiny text from a
  small picture still won't become readable; for that, use a bigger source picture.
- Internal: new pure-Java `ImageResampler` (Lanczos + unsharp, premultiplied alpha); `ImageProcessor` uses
  it in place of Java2D bicubic. No new dependency; runs the same on a dedicated server.

### <a id="norejoin-sweep"></a>G06 · NO-REJOIN sweep — undo-after-delete fixed; changes show live for everyone · 2026-06-27 · ✅ confirmed in-game (MP)
- **Fixed:** `/cb undo` (and `/cb redo`) after deleting a block no longer needs a **rejoin** — the block's
  name and look-HUD come back correctly right away.
- Closed the same "needs a rejoin" lag across the board: changing a block's **glow, hardness, sound,
  collision, category, shape, name/id, or notes** — by command, by tool (Lumina brush, Chisel, Omni-tool),
  in a menu, or in a bulk op — now updates the look-HUD live.
- **Multiplayer:** a change one player makes now shows for **every** player live (was: only the player who
  made it; others had to rejoin).
- Internal: project NO-REJOIN rule written into the code (`HudSync` banner; grep tag `NO-REJOIN`). Bulk
  *delete* still goes the old way — that's the next slice (slice 3).

### <a id="g06-14-rail"></a>G06 · Recycle-Bin delete — `/cb delete` + Deleter make markers (slice 2/5) · 2026-06-27 · ✅ confirmed in-game
- Deleting a custom block now turns **every placed copy** into a grey **`Deleted: <name>`** marker instead
  of the old broken/`(Removed)` block — no more purple blocks, and other players see it without rejoining.
- `/cb delete <id>`, `/cb delete #`, and the red **Deleter** all run through one shared delete rail now
  (`DeletionService`), so they behave identically. `/cb undo` turns the markers back into the real block.
- Held "Deleted Marker" item name is now **green** to match the look-HUD (was red).
- Internal: new `DeletionService` + `MarkerResolver`; the placement sweeper + undo journal now speak markers.

### <a id="g06-14-marker"></a>G06 · Recycle-Bin delete — Deleted-marker block (slice 1/5) · 2026-06-27 · ✅ confirmed in-game
- First piece of the new unified deletion system (replaces the old `(Removed)` block). Adds a dedicated
  **`deleted_marker`** block + BlockEntity: a grey block with a faint **red ✖**, a floating **`Deleted: <name>`**
  tag, and the same name on the look-HUD. Breaks instantly and **drops nothing**. Held item reads "Deleted Marker".
- No delete path uses it yet — that's slice 2 (route `/cb delete` + the Deleter through a shared `DeletionService`).
- **Pending tweak (folding into slice 2's build):** held item name will change from **red → green** to match the look-HUD.

### <a id="g06-tool-icon-green"></a>G06 · Colour tool icon now brightens to the configured hex (green stayed dark) · 2026-06-26 · ✅ confirmed in-game 2026-06-27
- Follow-up to the icon re-tint below. After `/cb config hex green #10FF01`, the **green** Square/Triangle icon
  stayed its dark shipped green even though the name read `[#10FF01]`. The re-tint kept each pixel's **own**
  brightness, and the green art's fill is dark (`#1E8C1E` ≈ 0.55) — so a bright-green target couldn't lift it, and
  green→green has no hue shift either, so nothing moved. Red/yellow looked fine only because their art is already
  bright and their hue shifted. The tint now **normalises** each pixel's brightness against the bundled fill before
  applying the target's, so the fill reaches the exact hex (green icon goes bright `#10FF01`) while outline/shading
  stay relative. Red/yellow now hit their exact hex too.

### <a id="g06-recolor-existing"></a>G06 · `/cb config hex` repaints existing blocks (new + old) · colour tools glint · 2026-06-26 · ✅ confirmed in-game
- Change a colour's hex (`/cb config hex green <#hex>`) and pick **Yes**, and **all** existing `_green` blocks now
  repaint to the new colour — both ones you made recently **and older ones**. Before, only recently-made blocks
  changed (older blocks kept no saved original to regenerate from). Now an older block's background is re-detected
  on the block's own texture and repainted, so it no longer needs the saved original. Designs stay untouched.
- **Tip:** to see the change clearly, pick a **distinctly different** colour (e.g. green → `#1133FF` blue), not a
  near-identical shade.
- All eight preset colour **Squares/Triangles glint** (shimmer) like the custom-colour tools. Glint is drawn on
  **your client**, so update the **client** mod jar (not just the server) to see it, then grab a fresh tool.
- **✅ confirmed in-game 2026-06-26** — tools glint ("tools have glow tint"); `/cb config hex green #10FF01` →
  recolour all → "perfect now". (Green **icon** brightness was the one residual → fixed above, [#g06-tool-icon-green](#g06-tool-icon-green).)

### <a id="g13-back-mirror"></a>G13 · Arabic back of a word stays mirrored after breaking a middle letter · 2026-06-26 · 🟢 built, ✋ pending in-game test
- Break the **middle** letter of an Arabic word and the **back** of the two remaining letters now still reads the
  **same as the front** (e.g. دحل → break ح → both sides read د on the right, ل on the left). Before, the back of
  the survivors read **mirrored**. The front is unchanged — the survivors still go isolated (no connecting bar).
- Bridges only a **single** empty block (the broken letter). Two separate words on one line should be kept **2+**
  empty blocks apart, otherwise they'll mirror-pair across the gap on their back faces.

### <a id="g06-tool-icon-tint"></a>G06 · Colour Square/Triangle icons re-tint cleanly to the configured hex · 2026-06-26 · 🟢 built, ✋ pending in-game test
- After `/cb config hex <colour> <#hex>`, the matching tool's **icon** now actually recolours to the new hex.
  Previously the re-tint kept the bundled art's muted shade (changing red to `#FF0000` looked identical), because
  the tint multiplied the target colour by the art's own saturation. It now uses the target colour directly while
  keeping the outline/shading, so the icon clearly wears the configured colour.

### <a id="g06-config-hex-sync"></a>G06 · `/cb config hex` tool name updates live on dedicated servers · 2026-06-26 · 🟢 built, ✋ pending in-game test
- After `/cb config hex <colour> <#hex>`, the matching Square/Triangle tool **name** (the `[#hex]` shown on the
  item) now updates **immediately for everyone online**, with no need to rejoin — previously a dedicated client
  kept showing the old default hex because the server's value was never sent to it.

### <a id="g13-arabic-gap"></a>G13 · Arabic letters touch — gap fix · 2026-06-26 · ✅ confirmed in-game ("finally the gaps are working")
- Adjacent auto-join Arabic letters no longer show a thin see-through gap between them, and a single letter no
  longer leaks a hairline of sky at its edges. The block stays a full solid cube; **all six faces** now sit flush
  with the block edge (updated 2026-06-26 — the first attempt made only two faces flush, so the gap remained).
- (Replaces an earlier attempt that drew only front+back faces and made the block look hollow — that was reverted.)

### <a id="g06-custom-swap-instant"></a>G06 · Custom-colour swaps instant on dedicated servers · 2026-06-26 · ✅ confirmed in-game
- Swapping a placed block with a `/cb customcolor` Square is now **instant** on a dedicated/LAN server, matching
  the preset colours — no more waiting for the server round-trip, and no flicker of an old/deleted block.

### <a id="g20-cloud-vault-s1"></a>G20 · Cloud Vault — share a whole block by code (S1) · 2026-06-21 · 🟢 built, ✋ pending in-game test
- **Share any block to the cloud and get a short code.** `/cb vault upload <id>` (OP-only) packages the
  block — texture, attributes (glow, hardness, sound, collision, shape, category), per-face paint, and
  animation — and returns a **share code** with a `[copy code]` button. A successful share plays a little
  sound + particle burst and an on-screen **"☁ Uploaded!"** title.
- **Restore it anywhere with the code.** `/cb vault download <code>` recreates the block exactly, **static
  or animated**. Then `/cb give <id>` to get it. If a block with that id already exists, the download is
  skipped for now (an Override / Keep-Both option arrives in the next slice).
- **Master switch `cloudShareEnabled`** (default on). Off → vault commands refuse politely and nothing
  hits the network. No `vaultEndpoint` set → a friendly "not set up yet" message, never a crash.
- ⚙️ Needs your Cloudflare worker URL in `config.json` as `vaultEndpoint` for the real round-trip.

### <a id="g14-all-frames"></a>G14 · Animated blocks keep ALL frames (off-atlas grid) · 2026-06-21 · 🟢 built, ✋ pending in-game test
- **Long GIFs no longer drop frames.** Animation frames are now packed into a square grid texture instead
  of one tall vertical strip, so a clip with hundreds of frames (e.g. 262) keeps every frame at full
  speed. Placed block and the hand/inventory/creative item icon both render off the block atlas (their
  own texture) — full resolution, no atlas down-scale.
- Animated blocks made before this update still play (they keep their old vertical-strip + `.mcmeta`).
- Reverses the planned ADR-012 atlas-revert (which was never built). See `docs/adr/ADR-013`.
- **⚠️ Watch when testing:** the distance "speckle/sparkle" ADR-012 blamed on mipmaps is **not** changed
  by this — the texture is still mipmap-free. Verify a placed animated block from a few blocks away. If it
  shimmers, that's a separate mipmap issue, not this fix.

### <a id="g18-lore-v2"></a>G18 · Lore (REVAMP v2): single-screen, replaces the Book GUI · 2026-06-21 · ✅ confirmed in-game (Rounds 1–6; Round 7 Share/Import still pending a cloud vault)
- **Notes are now "Lore" — the gray hover lines under a block item's name.** `/cb lore <id>` (or the
  Editor menu's **Lore** button) opens a **single screen** (no tabs): your lines listed, a **+ Add line**
  button, an **On/Off** switch, **Share**, and **Close**. OP-only. Everything saves automatically.
- **Each line is typed in an anvil** (≤ 50 chars). Left-click a line to edit it, right-click to delete it.
  Up to 28 lines. `&` colour **and** format codes (`&l` bold, `&n` underline, `&o` italic, …) apply in the
  menu and on the item — and a colour no longer wipes formatting, so codes work in any order.
- **On/Off** shows/hides every line on the item (text stays saved when Off). When On, all lines appear
  under the item name on hover (synced to the client).
- `/cb note <id>` is kept as an **alias**. `/cb lore <id> <text>` quick-adds one line; `/cb lore <id>
  clear` wipes all lines and turns it Off.
- **Share + import unchanged:** Share uploads the lore to the cloud vault → share code in chat; `/cb lore
  import <id> <code>` (or `/cb note import …`) pulls it onto another block (confirm before overwrite).
  *Needs a configured `vaultEndpoint`.*
- **Migration (automatic):** old notes fold into lines — old Hover Tooltip + old Lore (split per line)
  become the lines; it's On if the old tooltip was enabled or the block had lore. **The old To-Do list is
  removed** (the only data loss). Old flat `{id:"text"}` notes become one line.
- Removed the 3-tab Book GUI, the writable-book lore editor (`LoreBook`), and all To-Do code.
- **Post-test fixes (2026-06-21, confirmed in-game):** `&l` and other format codes now stick regardless of
  order — a colour code no longer resets bold/italic (new `core/LoreFormat.java`, used by the item hover and
  the editor's line preview). Removed the **duplicate close button** — the screen had both **Done** and the
  standard **✕**; kept the **✕** (the convention in every other menu).
- Edited: `core/NoteData.java`, `core/BlockNotesManager.java`, `core/LoreFormat.java` (new), `block/SlotBlock.java`,
  `network/HudSync.java`, `client/ClientSlotCache.java`, `client/CustomBlocksClient.java`, `gui/chest/NotesMenu.java`,
  `gui/chest/Icons.java`, `gui/chest/GuiRouter.java`, `gui/chest/EditorMenu.java`, `command/handlers/NoteCommands.java`,
  `command/CommandRegistrar.java`. Deleted: `gui/chest/LoreBook.java`.

### <a id="g16-finish-pass"></a>G16 · Finish pass: bulk/pack FX, Debug Log viewer · 2026-06-21 · 🟢 built, ✋ pending in-game test
- **Bulk operations now play their own "complete" cue.** Finishing a bulk edit / delete / rename plays
  the `bulk_complete` Feedback FX (totem ring + beacon hum) instead of nothing.
- **Manual pack reloads play a pack cue.** `/cb sync` and `/cb rp resume` now play the `rp_regenerate`
  Feedback FX (portal swirl + soft chime) rather than the generic success cue. Ordinary edits still
  rebuild silently; `/cb reload` (data reload) is unchanged.
- **New: Debug Log viewer.** IT Chest (`/cb diag`) → Row 6 → **Debug Log** opens an in-game, read-only,
  paged view of this session's `[CustomBlocks]` log lines (newest first, coloured by severity) — no
  more alt-tabbing to `logs/latest.log`.
- **`achievement` Feedback FX** stays preview-only until a future achievement system can trigger it.
- New files: `core/FeedbackFx.java`, `core/DebugLog.java`, `gui/chest/DebugLogMenu.java`. Edited:
  `Chat`, `BulkCommands`, `ChestGuiCommands`, `ItChestMenu`, `Nav`, `GuiRouter`.

### <a id="g16-fx-centred"></a>G16 · Feedback FX board centred · 2026-06-21 · 🟢 built, ✋ pending in-game test
- **Feedback FX chest tiles now sit in the middle of the chest** instead of hugging the top row.
  Same 6-row board, same controls — just a more balanced layout (owner feedback: felt too top-heavy).
- Edited: `gui/chest/FeedbackMenu.java` (master row 1 → row 2).

### <a id="g06-instant-swap"></a>G06 · Instant colour-Square swaps · 2026-06-20 · 🟢 built, ✋ pending in-game test
- **Colour Squares now swap instantly.** Right-clicking a placed block with a colour Square (the fixed
  red/yellow/green/black Squares and the custom-hex Square) changes it with no visible delay. The swap was
  always instant on the server; the client now paints the result the same tick instead of waiting for the
  server's block-update packet to come back.
- Misses are unchanged: swapping to a variant that doesn't exist still shows "create it with the Triangle
  first" (no flicker), and a Black Square with no `_black` variant still falls back to the base block.
- New: `client/ClientSwapPredictor.java`, `item/ColorSwapTool.java`. Edited: `ShapeToolItem`,
  `CustomColorToolItem`, `ClientSlotCache`, `CustomBlocksClient`. See ADR-009.

### <a id="g14-anim-polish"></a>G14 · Animation polish + render deferral · 2026-06-20 · 🟢 built, ✋ pending in-game test
- **`/cb anim <id>` on a non-animated block** now opens the studio straight on the **Texture** tab (was the
  Identity tab) so you can load a GIF/WebP right away; the chat note is reworded to match.
- **Known / deferred:** GIF/WebP **muffle** (nyan-style speckle) and **earth** crispness + smoothness are
  **deferred to the Phase 1b own-texture renderer** — the vanilla block atlas applies mipmaps to every sprite, so
  no atlas-side setting can make a detailed/text image fully crisp. Tracked in the Group 14 testing guide.
- **Still open (small):** loading a new GIF in the studio resets its speed/loop; an animated block's background
  can't be re-filled without a new image.

### <a id="g13-delete-letters"></a>G13 / Build B · Delete static letters + reclaim slots · 2026-06-19 · 🟢 built, ✋ pending in-game test
- **Removed:** the 144 OLD static Arabic letter blocks (36 letters × 4 colours) are permanently deleted on boot
  (one-time, idempotent migration). They no longer appear in creative search or `/cb arabic list`.
- **World cleanup:** any placed copy of an old static letter is replaced with air as its chunk loads (runs across
  restarts until done).
- **Reclaimed:** the ~144 freed slots return to the pool; freed indices are reused only as a last resort and are
  dropped from the retired set on reuse, so a reused slot can never show or delete a wrong block.
- **Slimmer jar:** the 144 bundled letter PNGs were stripped from `assets/customblocks/arabic_art/` (the 80 number
  PNGs are kept).
- **Untouched:** numbers (A0–A9 + E0–E9, 80 blocks) and the auto-join letter block ("Arabic Letters" tab).
- New files: `core/RetiredSlots.java`, `arabic/ArabicLetterRetirement.java`. Edited: `core/SlotManager.java`
  (`retireSlots`, reuse-safe `nextFreeSlotIndex`), `CustomBlocksMod.java` (boot wire-up).
- New persisted state: `config/customblocks/retired_slots.json`.
