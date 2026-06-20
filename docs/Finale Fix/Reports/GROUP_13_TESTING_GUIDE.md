# 🧪 Group 13 — Arabic System — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | ✅ Round 3 **6 / 6** · auto-join complete · **Build A ✅ · Build B ✅** (chunk-load deadlock fixed, confirmed in-game). Static letters fully retired. Remaining: Hide (§13) · Type-a-word (§14) — 🟡 **deferred, low priority** (not urgent). |
| **Progress** | 🟩🟩🟩🟩🟩🟩 · Round 3: 6 / 6 passed · Build A + B ✅ |
| **Last tested** | 2026-06-20 (§15B Build B — static letters retired, world loads clean) |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| ✅ **Confirmed 2026-06-18** | No-reload proof block — picture changes on click with NO resource-pack reload. PASSED — auto-join texture path proven. | §P |
| ✅ **PASSED — PERFECT** | Text/word stroke — outline 10 (Eng) / 12 (Arabic + numbers); 1-char full, long words solid. | §0 |
| ✅ **Confirmed 2026-06-19** | Render preview — live preview screen: real word in your colours, 3D rotatable block, no pack/prompt/world-block | §1 |
| ✅ Confirmed 2026-06-16 | Arabic Browser (2a) · `text` removed + anvil fit (2b) · Default Colours 6×2 (2d) · bg match (1a) · text render (1b) · HUD (1c) · word cmd · Color Studio (2c) · no chat prompts (3) · bulk pre-pick (4) | §4 |
| ✅ **Confirmed 2026-06-19** | Auto-join letter shapes (O3). *(Omni-Tool direction mode O5 → REMOVED in jar, verify §R3)* | §5 |
| ✅ **Confirmed 2026-06-19** | Brightness fix + clean live names (`Jeem Black` / `…Mid`) + editable form labels + name on placed block (O6) | §6 |
| ✅ **Confirmed 2026-06-19** | All 6 faces render. *(icons → §8 · join is now all-colours → §10)* | §7 |
| ✅ **Confirmed 2026-06-19** | **Icon fix v2** — join item shows real art in hand / inventory / creative. Dev: works. | §8 |
| ✅ **Confirmed 2026-06-19** | **Full-bright glyph** — placed join letter never reads grey/dark regardless of facing or shade | §9 |
| ✅ **Confirmed 2026-06-19** | **Placement flash fix** + **all colours join** (any colour connects) | §10 |
| ✅ **Confirmed 2026-06-19** | **Isolated floaters lowered** — short no-descender letters (dal/dhal/ta-marbuta/round-ha/hamza) nudged DOWN a little; alef tiny; tailed/bowled unchanged | §12 |
| ✅ **Resolved by §12** | **Stuck-isolated size fix** — v1 (scaled hand-art) dropped; isolated is now font-drawn + lowered (§12, confirmed in-game). No separate shrink needed. | §11 |
| ✅ **Confirmed 2026-06-19 (5 of 6)** | **Round 3** — join count 16→1 · pick-block returns the real letter · "Place letter blocks" gives joinable blocks + lore · stray edge lines fixed · Omni Arabic mode REMOVED | §R3 |
| ✅ **Confirmed 2026-06-19** | **Readable back (C-full)** — back reads the same word correctly from behind (dropped the double-flip). | §R3 |
| ✅ **Confirmed 2026-06-19 (core)** | **Retire static letters → one system (Build A)** — `/cb arabic letter`→auto-join ✅, tab "Arabic Letters" ✅. `join` removed; letters no longer auto-created. Old static letters still in creative search → 🟡 Build B deletes them. | §15 |
| ✅ **Confirmed 2026-06-20 (Build B)** | **Delete + reclaim** — boot-migration deletes the 144 static letter slots, air-cleans placed copies as chunks load, reclaims their slots, strips the 144 letter PNGs. Numbers untouched. *(Fixed a chunk-load deadlock that froze world-load at "Preparing spawn area 0%" — air-clean now runs on the world tick.)* | §15B |
| 🟡 **Deferred — low priority** | **Hide / manage letters** — per-block Hide, hide-all-on-page, master hide-all, Hidden tab, un-hide one + all; recoverable (replaces hard delete). Design locked, build parked — not urgent, revisit later. | §13 |
| 🟡 **Deferred — low priority** | **Type-a-word auto-build** — GUI box + command; places a connected row in front of you, RTL, in a colour you pick. Design locked, build parked — not urgent, revisit later. | §14 |

---

# 🎯 Test now

## §15 · Retire static letters → ONE system  (Build A, 2026-06-19)

> 💡 **What changed:** the old static letter blocks are being retired so auto-join is the **only** letter
> system. **Build A** (this jar) does the safe command/menu side — no blocks deleted yet, no world risk.
> **Build B** (next) does the delete + slot reclaim. Numbers (Arabic + English) are untouched throughout.
>
> 🔧 **Status:** 🟢 Build A built + deployed 2026-06-19, jar green 🎯 — test below.

**Test (Build A):**
- ✅ `/cb arabic letter jeem` → gives an **auto-join** jeem (place a few right-to-left → they connect). **CONFIRMED 2026-06-19.**
- ⬜ `/cb arabic letter jeem red 3` → gives **3 red** auto-join jeem (colour + count both work).
- ⬜ `/cb arabic join …` → **command no longer exists** (it was a duplicate of `letter`).
- ✅ Creative inventory → the tab is now named **"Arabic Letters"** (was "Arabic Letters (Join)"). **CONFIRMED 2026-06-19.**
- ⬜ **Numbers still work:** `/cb arabic list` → Arabic Numbers / English Numbers sections still give number blocks.

> 🟡 **Known + expected (→ Build B):** the creative **search** still shows the OLD static letter blocks
> (owner: "the 5 on the left" — the old per-letter static entries). Build A only stops *creating* them and
> redirects the command; the ones from prior boots still linger in slots.json + creative search until
> **Build B deletes them**. This is the dedupe's deletion half — not a bug.

---

## §15B · Delete static letters + reclaim slots  (Build B, 2026-06-19)

> 💡 **What changed:** the 144 OLD static letter blocks (36 letters × 4 colours: black/red/green/yellow) are
> now **deleted for good** on boot. Their slots + space are reclaimed, their textures removed, and any copies
> you already **placed** in the world turn to **air** as you move around (chunks clean on load). The 144 bundled
> letter PNGs are stripped from the jar. **Numbers (A0–A9 + E0–E9, 80 blocks) are NOT touched.** The auto-join
> letters (the "Arabic Letters" tab) are a separate block and are **NOT touched**.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-20.** Also fixed a chunk-load **deadlock** that froze
> world-load at "Preparing spawn area 0%": the placed-copy air-clean called `world.getBlockState` inside the
> chunk-load event → `getChunkBlocking` parked the server thread. Now the scan only queues hits; the air-swap
> runs on the **world tick** (non-blocking chunk getter). Cause/fix → PROGRESS_LOG.
>
> 🛡️ **Safety:** a reclaimed slot can never show a wrong block — freed letter slots are kept out of reuse until
> every other slot is taken, and the placed-copy air-clean runs across restarts (a persisted
> `config/customblocks/retired_slots.json` tracks the retired slots).

**Test (Build B) — ✅ confirmed pass:**
- ✅ **Gone from search:** creative search no longer shows the old static letters (e.g. "Jeem Black" — the "5 on the left"). Only the auto-join **Arabic Letters** tab + numbers remain.
- ✅ `/cb arabic list` → the **letter** entries read 0 present; **Arabic Numbers / English Numbers still give blocks**.
- ✅ **Placed copies vanish:** placed old static letters turn to air as you move near them (chunk-load clean).
- ✅ **Numbers survive:** A0–A9 and E0–E9 (all 4 colours) still placeable and textured.
- ✅ **Auto-join unaffected:** `/cb arabic letter jeem` still works and joins.
- ✅ **Slots reclaimed:** used-slot count drops by ~144 from before.
- ✅ **Idempotent:** restart again → no errors, no re-deletion, everything stable.

> ⚠️ **One-way:** this permanently deletes those blocks (as you asked) and the placed copies. There is no undo.

---

## §R3 · Round 3 — reported-issue fixes  (2026-06-19)

> 💡 **What's here:** the six issues the dev reported. **All six are now coded and in the jar** — install
> `customblocks-1.0.0.jar` and run the checks below. Design + reasons → `GROUP_13_ARABIC.md` → **Round 3**.

**Results (tested 2026-06-19):**
- ✅ **Join count:** `/cb arabic join ba` (no number) gives **1** block, not 16.
- ✅ **Pick block:** place a join letter, **middle-click** it (creative) → you get **that letter** back
  (named e.g. `Ta Red`, real art), **not** a blank `block.customblocks.arabic_letter` item; placing it re-joins.
- ✅ **Place letter blocks:** `/cb arabic word …` → type a word → **Place letter blocks** → gives the
  **joinable** "(Join)" blocks (one per letter), lore reads the new joinable wording.
- ✅ **Edge lines:** single join letter + a row → **no** stray thin lines on edges or seams. Clean.
- ✅ **Omni Arabic mode REMOVED:** sneak+right-click the OmniTool → **no** Arabic button (only Glow /
  Hardness / Area); pointing it at a letter block does nothing. *(Supersedes the §5 OmniTool-direction checks.)*
- ✅ **Readable back (C-full)** — confirmed in-game: a word reads correctly RTL from behind, glyphs not
  backwards. First test had mirrored glyphs (back face was turned 180° **and** mirrored — a double flip);
  fix dropped the mirror (180° turn alone keeps it readable). Lone + connected both read correct both sides.

---

## §P · No-reload proof block  (the make-or-break for auto-join)

> 💡 **What it does:** a throwaway test block that paints itself with a **live in-game picture** instead of
> the resource pack. The whole point is to prove changing a block's picture causes **no reload, no freeze,
> no prompt** — the exact thing that was blocking auto-join.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-18** — clicking changes the block's picture with **no resource-pack reload**. The no-reload live-texture path is proven; the real auto-join is built on it.
>
> **Test (passed):**
> - ✅ Creative → **CustomBlocks "Tools" tab** → grab **Screen Test**. *(or `/give @s customblocks:screen_test`)*
> - ✅ Place it → faces show a coloured picture.
> - ✅ **Right-click** → picture changes instantly, **NO** reload / freeze / prompt.

---

## §1 · Render preview — live preview screen  (Color Studio)

> 💡 **What it does:** in the Color Studio, **Render preview** opens a live preview screen showing the
> real rendered word in your chosen colours — a 3D, rotatable block — with no resource-pack prompt and
> no block placed in the world. Back returns you to the colour menu.
>
> 🔧 **Status:** 🟢 core built + confirmed in-game 2026-06-16 ✅ PASSED (partial): works + looks good, more polish/upgrades planned later.
>
> **Test:** `/cb arabic word <id> <name>` → type text → **Single block** → pick colours → **Render preview**:
> - ⬜ a preview screen opens showing your word in the chosen colours — **no resource-pack prompt**, no block placed in the world.
> - ⬜ the block is 3D and you can rotate it by dragging.
> - ⬜ change a colour on the screen → the preview updates live.
> - ⬜ **Back** returns to the Color Studio with your colours still set; **Create** makes the real block.
>
> **QoL controls (2026-06-16) — ✅ PASSED (partial), polish later:**
> - ✅ **scroll wheel** speeds up / slows down the auto-spin (corner readout shows `spin %`).
> - ✅ **shift + scroll** zooms the cube bigger / smaller (`zoom %`).
> - ✅ **click the cube without dragging** pauses the spin; click again resumes.
> - ✅ **R** resets rotation, speed and zoom to default; **Enter** makes the block (same as Create).

---

# ✅ Confirmed

- **§0 — Text/word stroke — uniform + proportional — ✅ PASSED, PERFECT** 🏆
  - ✅ ① 1-char block = full/bold stroke; long words a bit lighter but still solid — no blob, no hairline.
  - ✅ ② Arabic numbers' black stroke reads the same weight as the letters'.
- **1a — Background matches bundled art** — word background matches the bundled letters. ✅
- **1b — Text rendering** — English readable with open counters, Arabic ح stays open, numbers big + tight. ✅
- **1c — HUD on a new word block** — shows id + name like every block. ✅ ("HUD is fixed")
- **Word command** — `/cb arabic word <id> <name>` makes a joined word block. ✅
- **2a — Arabic Browser** — `/cb arabic list` → group picker → aligned grid per group; the left colour rail switches the variant in place; glyph manage + give-all work. ✅
- **2b — `text` removed + anvil titles** — `/cb arabic text` is rejected (folded into `word`); the maker's anvil titles fit. ✅
- **2c — Color Studio** — customizes background + letter colour in-GUI; creation confirmed "perfect." ✅
- **2d — Default Colours layout** — clean 6×2 two-zone palette; defaults save, persist, and pre-fill the maker. ✅
- **3 — No chat prompts** — the Arabic maker tiles open the GUI directly, no chat handoff. ✅
- **Issue 4 — Bulk pre-pick → Yes/No confirm** — ✅ (full tests in `GROUP_07_TESTING_GUIDE.md` §A4).

---

## §5 · Auto-join letter shapes + direction tool  (O3 / O5)

> 💡 **What it does:** placed Arabic letter blocks **stand up like a sign**, read **front, right-to-left**,
> and auto-shape to **isolated / initial / medial / final** from their neighbours — full real-Arabic rules
> (the one-sided letters ا د ذ ر ز و … end a word; numbers never join). A **new dedicated joinable block**
> with a live in-memory texture — the 1028 existing blocks + 224 static letters are untouched.
> All forms (isolated + connected) are font-drawn at matching weight for one consistent look; kashida hands
> reach the tile seam.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19** — auto-join forms + Omni-Tool direction work.

**Get the blocks:** `/cb arabic join <letter> [count]` — e.g. `/cb arabic join ba`, `join jeem`, `join lam`.
(Tab-completes letter names. These are the NEW joinable blocks, separate from `/cb arabic letter`.)

**Test (place facing yourself, right-to-left — first block = the RIGHT end):**
- ⬜ place one letter → **isolated**. (G13.5)
- ⬜ place a second letter to its **left** → right one becomes **initial** (start), left one **final** (end). (G13.6)
- ⬜ place a third to the left → middle becomes **medial**. (G13.7)
- ⬜ a one-sided letter (`join ra` / `dal` / `waw` …) shows only isolated/final; the block to its **left** starts fresh.
- ⬜ break a middle letter → the two neighbours **re-shape**.
- ⬜ diagonal placement does **not** join → stays isolated. (G13.8)
- ⬜ deep-bowl letters — `join jeem` (ج), `join ha` (ح), `join ya` (ي) — bowl/tail sits **fully inside** the tile, **not cut off** at the bottom (isolated + final).
- ⬜ **Omni-Tool → Arabic mode** (sneak-right-click in air → menu → Arabic): **right-click** a start letter, **left-click** the other end → that line re-faces + reshapes, and letters you place next follow that direction. (O5)
- ⬜ **Facing auto-inherit:** place one letter, then place the next **directly beside it** — the new block takes the same facing as the one it touches, so the two join. (Previously a slightly different facing left them un-joined.)
- ⬜ With an Omni-Tool direction set, the placed block uses the **tool** facing, not the neighbour's.

> ⚠️ **First-look things to tell me (v1):** does each glyph face **you** (not the back)? upright, not mirrored? Does the connected-form **weight match** the isolated art beside it?

---

## §6 · Brightness fix + clean names + live form labels + name on placed block  (O6)

> 💡 **What it does:** four things on the joinable letters — (1) a placed join letter is now **as bright**
> as a bundled letter beside it; (2) names are clean — `Jeem Black`, and connected forms add a word:
> `Jeem Black Ini` / `Mid` / `Fin` (isolated has no word, so a lone block reads just `Jeem Black`);
> (3) those three words (Ini/Mid/Fin) are **editable** with `/cb config arabicforms` and every block
> **re-labels instantly, no reload**; (4) looking at a placed join block shows its **live** name on the
> HUD.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19** — names, live form labels, editable `arabicforms`, HUD all work.

**Test (get blocks: `/cb arabic join jeem 3`):**
- ⬜ **Brightness:** place a join letter right next to a bundled one (`/cb arabic letter jeem`) → they read **equally bright** in daylight *and* in shade.
- ⬜ **Held name:** the join item in your hand / hotbar reads **`Jeem Black`** (not `jeem · black (join)`).
- ⬜ **Creative tab name:** open **Arabic Letters (Join)** → entries read `Jeem Black`, `Jeem Black Ini/Mid/Fin`, in each colour (`Jeem Green`, …).
- ⬜ **Live form name (placed):** place a row right-to-left → look at each block → HUD name updates to match its shape (alone `Jeem Black`; middle `Jeem Black Mid`; ends `…Ini` / `…Fin`).
- ⬜ **Editable labels:** `/cb config arabicforms mid Middle` → every placed/held middle block instantly shows `Jeem Black Middle`, **no resource-pack reload**. `/cb config arabicforms` shows current; `reset` restores Ini/Mid/Fin.

> ⚠️ **Tell me:** any block that still reads grey/dim, or any name still showing the old `·`/`(join)` style (old blocks placed before this jar keep their baked name until re-given — that's expected; re-give to refresh).

---

## §7 · All 6 faces show the letter  (2026-06-19)

> 💡 **What changed:** a placed join letter shows the letter on **all 6 faces**, not just the front.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19** — letter shows on all 6 faces.

**Test (get blocks: `/cb arabic join jeem 3`):**
- ⬜ **All 6 faces:** place one → the letter shows on **every face** (top, bottom, all 4 sides), not just front.

> ⚠️ **Tell me:** any face still blank.

---

## §8 · Icon fix v2 — join item art in hand/inventory  (2026-06-19)

> 💡 **What changed:** the join letter's **inventory / hand icon** now shows the real letter art.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19** — join letters show their real art in inventory / creative / hand.

- ✅ **Icon:** the join item shows the **letter art** in hand / hotbar / creative — no longer solid black.

---

## §9 · Full-bright glyph — no more grey/dark letters  (2026-06-19)

> 💡 **What changed:** a placed join letter is now drawn **full-bright** on every face, so the white glyph
> **never** reads grey or blacks out — no matter which way the block faces or whether it's in shade.
> Before, it borrowed light from the block it faced; facing into a solid block made it dark.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19**.

**Test (get blocks: `/cb arabic join jeem 3`, also a coloured one):**
- ⬜ Place a join letter facing **into a wall / against another block** → the glyph is **still bright white** (this is the case that was dark grey before).
- ⬜ Place letters facing **different directions** → **all** read the same bright white, none grey.
- ⬜ Move into **shade / night** → the glyph **stays bright** (it does **not** dim with the world).
- ⬜ Bright in **all colours** (black / red / green / yellow) — no dark/grey glyph on any.

> ⚠️ **Tell me:** any glyph still grey/dark, or full-bright looks **too** flat next to bundled letters (then we tune it).

---

## §10 · Placement flash fix + all colours join  (2026-06-19)

> 💡 **What changed:**
> 1. **No more black flash** — a freshly placed letter no longer flashes solid black before the glyph shows. The block now renders invisible; the letter is drawn entirely by the glyph renderer.
> 2. **All colours join** — red / green / yellow joinable letters connect just like black; any colour joins any colour. Each block keeps its **own** background colour, white script flowing across the seam.
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19**.

**Test (get blocks: `/cb arabic join jeem 3`, plus coloured ones from the Join tab):**
- ⬜ **No flash:** place a letter → it appears **immediately**, no black square first.
- ⬜ **Colours join:** place e.g. green jeem + red ba + yellow lam in a straight row → they **connect** (initial / medial / final); each keeps its own colour background.
- ⬜ **Mixed:** a green letter beside a black one → they **join** (no colour boundary anymore).
- ⬜ **Either end:** build the row one way, then try the other way → both connect.
- ⬜ **Break middle:** remove a middle letter → the two halves re-shape.
- ⬜ **Fixed-form searchable variants:** in the "Arabic Letters (Join)" tab, type **"medial"**, **"final"**, **"initial"**, or a letter name like **"jeem"** in creative search → those named form variants appear. Place a fixed-form variant on its own → it keeps its chosen shape and does **not** reshape when placed next to other letters.

> ⚠️ **Tell me:** a letter that shows up **invisible** (gone, not black) → send `latest.log`.

---

## §11 · Stuck-isolated letter size fix  ❌ DO NOT TEST v1

> 🔧 **Status:** ✅ **RESOLVED by §12 (2026-06-19)** — v1 (scaled hand-art) dropped; isolated letters are now font-drawn + lowered (§12, confirmed in-game), so they match their neighbours. No separate shrink built.

---

## §12 · Isolated floaters lowered  (built 2026-06-19, jar green 🎯)

> 💡 **What changed:** an **isolated** letter that floated too high now sits a little lower — short
> no-descender letters (dal د, dhal ذ, ta-marbuta ة, round-ha ه, hamza ء) drop a little; alef ا drops a
> tiny bit; letters with a tail/bowl (waw, ra, zay, noon, meem, ya, jeem, ha, lam, ain, maqsura) and all
> connected letters are **unchanged**. (Why/how → PROGRESS_LOG.)
>
> 🔧 **Status:** ✅ **CONFIRMED in-game 2026-06-19** — lone short letters sit a little lower (9%/3%), tailed/connected unchanged. ⚠️ originally needed **Restart MC**;
> re-give blocks made from the old jar.

**Test (get blocks: `/cb arabic join dal`, `join ta_marbuta`, `join alef`, plus a tailed one like `join waw`):**
- ⬜ A lone **dal / dhal / ta-marbuta / round-ha / hamza** sits a touch **lower** than before — grounded a
  little, **not** slammed to the bottom, **not** dead-centre.
- ⬜ A lone **alef** is nudged only a **tiny** bit.
- ⬜ **Tailed/bowled** letters (waw, ra, noon, meem, ya, jeem, ha, lam, ain) look **exactly as before**.
- ⬜ **Connected** letters in a word (initial/medial/final) are **unchanged** — seams still line up.
- ⬜ Word check: build **ذراع** → dal-ish drops, alef tiny, ra + ain stay put, all on one line.

> ⚠️ **Tell me a number:** if any letter is still too high or now too low, say so — the drop is two tunable
> values (short = 9%, alef = 3% of tile height).

---

## §13 · Hide / manage letters  🟡 DEFERRED — low priority (design locked, build parked)

> 🟡 **Deferred 2026-06-20:** nice-to-have, not urgent — revisit after the higher-priority groups. Design stays locked below so it isn't lost.
>
> 💡 **What it will do:** clean up the 224 bundled letters by **hiding** (not deleting) — fully recoverable.
> Design in `GROUP_13_ARABIC.md` → **O8**. Nothing built yet; listed so it isn't lost.
>
> Locked design: per-block **Hide** button in the browser · **Hide all on page** (current search/filter) ·
> master **Hide all** · hidden letters move to a **Hidden tab** · un-hide **one-by-one** and **un-hide all** ·
> always recoverable (hide = visibility only, jar masters never destroyed) · scope = one colour variant.
> Hard delete + custom-colour recolor (from O7) are **deferred** — Hide covers the real need.

## §14 · Type-a-word auto-build  🟡 DEFERRED — low priority (design locked, build parked)

> 🟡 **Deferred 2026-06-20:** nice-to-have, not urgent — revisit after the higher-priority groups. Design stays locked below so it isn't lost.
>
> 💡 **What it will do:** type a word → the connected letter blocks **place themselves** in a row.
> Full spec → `GROUP_13_ARABIC.md` → **O9**. Nothing built yet — test steps below are the plan.
>
> **Locked design:** trigger from **both** a GUI text box **and** `/cb arabic build <word>` · row builds
> **on the ground in front of you, where you look** (~2 ahead if aiming at nothing), **right-to-left**,
> correct auto-join forms · colour = **one colour for the whole word OR per-letter** (Color Studio path) ·
> **space → air gap**, next word fresh · **digits → inline, non-joining**, asked each time Eastern ٣ vs
> Western 3 · **collision → stops** before the blocked cell + tells you how many placed (never overwrites) ·
> **undo = one step, persisted, like the other undos** (own saved word-build store; removes only the still-
> intact built blocks).

**Planned tests (when built):**
- ⬜ `/cb arabic build بيت` → a connected RTL row appears on the ground in front of you, correct forms.
- ⬜ GUI box: type a word → **Build** → same result; colour picked first (whole-word and per-letter both work).
- ⬜ Word with a **space** → an air gap, each side a separate connected word.
- ⬜ Word with **digits** → asked Eastern ٣ vs Western 3; digits sit inline, letters connect around them.
- ⬜ **Collision:** build aimed into an existing block → stops before it, message "placed N", nothing overwritten.
- ⬜ **Undo:** `/cb arabic undo` removes the last word in one step; works again after a relog/restart; a
  block you broke by hand is left alone.

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs the ✅ line (📸 a screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete myid` · delete any test block you made.
