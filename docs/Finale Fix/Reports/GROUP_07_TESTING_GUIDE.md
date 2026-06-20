# 🧪 Group 07 — Bulk Operations — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Partial |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟥🟥🟥🟥 · 17 / 30 passed |
| **Last tested** | 2026-06-13 (§A0·§A1·§A2) |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| ✅ Passed 2026-06-13 (⚠ partial) | **`/cb bulkreid` command** — works; GUI + polish deferred for later | §A2 |
| ✅ Passed 2026-06-13 | **Step 1 → Step 2 GUI — all 8 bulk ops** (edit·delete·rename·category·duplicate·export·lock·favorite). Old dashboard removed. | §A0/§A1 |
| 🎯 Test now **(NEW jar 23:47)** | **Pre-picked bulk skips Step 1** — listgui pick → action → straight to Yes/No confirm (Area 4 bug fix) | §A4 |
| 🎯 Test now | **`/cb listgui` upgrades** — search · tick-many select · bulk-on-selection | §A |
| 🎯 Test now | **Duplicate** + **Export** (dashboard + command) · despeckle v2 · broken-link fix | §B |
| ✅ Passed 2026-06-12 | every core bulk op · Dashboard 2.0 · Bulk Hub · command-twin · tab-complete | §C |
| ⏳ Coming | bulk **recolor(edge)** · bulk **reid** GUI (command in §A2) | §D |

**The ops** — each takes a filter, confirms big batches (clickable), and (except lock/favorite/export) is one `/cb undo` to reverse:

| Op | Command | Dashboard |
|---|---|:--:|
| Edit setting (glow/hardness/sound/collision) | `/cb bulkproperty` | ✅ |
| Delete | `/cb bulkdelete` | ✅ |
| Rename (prefix/suffix/replace) | `/cb bulkrename` | ✅ |
| Lock / Unlock | `/cb bulklock` · `/cb bulkunlock` | ✅ |
| Favorite / Unfavorite | `/cb bulkfavorite` · `/cb bulkunfavorite` | ✅ |
| Move to category | `/cb bulkcategory` | ✅ |
| Duplicate | `/cb bulkduplicate` | 🎯 |
| Export | `/cb bulkexport` | 🎯 |

> 🛠️ **Redesign:** every bulk op now opens a **Step 1 selection GUI** (filter, or hand-pick via the
> listgui + 🟩 green-concrete confirm) → **Step 2 action GUI** (review · the op's controls · confirm).

---

# 🎯 Test now

## §A4 · Pre-picked bulk skips Step 1 → straight to confirm  🎯 Test now

> 🐞 **The bug:** `/cb listgui` → tick blocks → **Bulk actions on N** → pick an op (e.g. Delete) → the wrong menu appeared — the **Step-1 chooser showing "Selected: 0 block(s)"** — your picks were thrown away.
> ✅ **The fix:** when you arrive with blocks already ticked, the Bulk Hub now **remembers them and skips Step 1**. Pick an op → it goes **straight to a Yes/No confirm** on exactly the blocks you ticked.

> 🧰 **Before you start:** `/cb listgui` with a few throwaway blocks (`/cb create z1` … `z3`).

**Try these:**

- **① Pick → hub remembers** 🔴 — tick 2–3 blocks → **Bulk actions on N selected** (slot 52)
  - ✅ **Pass:** the Bulk Hub opens and the top tile reads **"Acting on your N picked block(s)"**; every op tile hint says **"Click → confirm on your picks"**.
  - ❌ **Broken if:** it shows the general hub ("Pick an op → choose blocks → confirm") and loses the count.

- **② Delete → confirm only (no Step-1)** 🔴 — click **Delete** (TNT)
  - ✅ **Pass:** a **Yes/No confirm GUI** opens naming your N blocks — it does **NOT** show the "Selected: 0 block(s)" Step-1 chooser. Confirm → only those blocks delete; `/cb undo` restores them.
  - ❌ **Broken if:** the Step-1 builder appears, or it says 0 selected.

- **③ Param op (Edit a setting) → options → confirm** 🔴 — pick again → **Edit a setting** (comparator)
  - ✅ **Pass:** goes straight to the setting/value options (still on your picks), then confirm; only the picked blocks change.

- **④ Fresh hub still has Step 1** 🔴 — open `/cb bulkgui` directly (no pre-pick)
  - ✅ **Pass:** normal flow — pick an op → **Step 1 choose blocks** → Step 2. The pre-pick path doesn't leak into the normal hub.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | pre-picked hub shows "Acting on your N picked block(s)" |
| 🟥 | ② | Delete → Yes/No confirm, NOT "Selected: 0" Step-1 |
| 🟥 | ③ | param op → options → confirm, still on the picks |
| 🟥 | ④ | a fresh `/cb bulkgui` still uses the normal Step-1 flow |
| — | **0 / 4** | |

---

## §A · `/cb listgui` upgrades — search · multi-select · bulk-on-selection

> 💡 **What it does:** the block list is now a front door to bulk — search it, tick blocks, send
> just those to the hub.
> ⚠️ **In the list now: left-click = tick/untick · right-click = edit** (it used to be left-click = edit).

> 🧰 **Before you start:** `/cb listgui` with ~10 blocks (a mix of categories; lock/favorite a couple).

**Try these:**

- **① Search the list** 🔴 — bottom-row **Search** (compass) → left-click → anvil → type id/name/category → take it
  - ✅ **Pass:** the list shows only matches; paging keeps the search; **right-click Search** clears it; a no-match search shows the placeholder.

- **② Tick-many select** 🔴 — left-click several blocks
  - ✅ **Pass:** each glints green **✔ selected**; **N selected** (slot 47) counts up. Left-click again un-ticks · **right-click** opens the editor (ticks survive) · **Select all shown** (slot 51) ticks the whole filtered list · click **N selected** to clear.

- **③ Bulk on selection** 🔴 — tick 2–3 → **Bulk actions on N selected** (slot 52, chest) → click
  - ✅ **Pass:** the Bulk Hub opens pre-filtered to those blocks (its **Matched** spyglass lists them). Run any op → only the ticked blocks change. **Back** → the list.
  - 🔎 Nothing ticked → gray "Tick some blocks first" (deny sound on click).

- **④ Delete path (one undo)** 🔴 — tick throwaways → Bulk actions → **Delete** → confirm
  - ✅ **Pass:** only those go; `/cb undo` brings them all back in one step.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | search filters by id/name/category, survives paging, clears |
| 🟥 | ② | tick/untick + select-all + clear; right-click still edits |
| 🟥 | ③ | selection feeds the Bulk Hub; only those blocks change |
| 🟥 | ④ | bulk delete on selection is one undoable batch |
| — | **0 / 4** | |

---

## §B · Duplicate · Export · despeckle v2 · broken-link fix

> 🧰 **Before you start:** a couple of textured blocks; a real image URL for the despeckle test.

**Try these:**

- **① Broken-link create makes nothing** 🔴 — `/cb create bad1 Test https://example.com/not-a-real-image.png`
  - ✅ **Pass:** chat says it couldn't get an image and `bad1` is **NOT** created. A good URL after → the block appears only once the image is fetched.

- **② Despeckle v2 (edge-aware fill)** 🟡 — `/cb config background BgRemove` → `/cb create jordan Jordan <logo-url>`
  - ✅ **Pass:** a dark-outlined logo keeps its border **visible** (white fill behind it); bright logos without dark outlines still get a black fill.
  - ⚠️ Wrong fill colour on some image? Send it — this is a one-line tune.

- **③ Bulk duplicate** 🔴 — `/cb create d1 Apple <url>` → `/cb bulkduplicate id:d` → `/cb undo`
  - ✅ **Pass:** `d1_copy` appears with the same texture + settings (not blank); undo removes it. Dashboard: `/cb bulkgui` → Duplicate → filter → Apply.

- **④ Bulk export** 🔴 — `/cb bulkexport all json` · `/cb bulkexport category:fruit txt`
  - ✅ **Pass:** a file lands in the exports folder with that many blocks. Tab `/cb bulkexport all ` → suggests `json`/`txt`. Dashboard: Export → filter → Format toggle → Apply.

- **⑤ Hub has all 8 ops** 🟡 — `/cb bulkgui` → Edit · Rename · Move · Duplicate · Export · Lock · Favorite · Delete, each opening the dashboard in that mode.

---

# ✅ Passed — kept for re-test reference

## ✅ §A2 · `/cb bulkreid` command — change many IDs by a pattern  ✅ Passed 2026-06-13 (⚠ partial)

> ⚠️ **Partial — deferred for later:** the command works in-game. Still to do (parked): the **Step1→Step2 GUI** for reid + a Bulk Hub tile, and a **polish** pass (no-arg currently shows usage). Revisit with recolor.

> 💡 **What it does:** the id-version of `/cb bulkrename`. It changes the **custom id** of many blocks
> at once by a pattern — `prefix`, `suffix`, or `replace` — while keeping each block's slot (so
> textures + already-placed blocks are untouched, no pack rebuild, exactly like single `/cb reid`).
> One `/cb undo` re-ids the whole batch back.

> 🧰 **Before you start:** make a few throwaway blocks with related ids, e.g. `/cb create old_a`,
> `/cb create old_b`, `/cb create old_c`. Lock one (`/cb lock old_c`) to see the skip.

```
/cb bulkreid all prefix new_           → every id gets "new_" in front  (old_a → new_old_a …)
/cb bulkreid id:old_ replace old_ new_ → ids starting old_ become new_  (old_a → new_a …)
/cb bulkreid all suffix _v2            → every id gets "_v2" on the end
```

**Try these:**

- **① Replace across a set** — `/cb bulkreid id:old_ replace old_ new_`
  - ✅ **Pass:** chat *"Re-id'd N block(s)"* (hover lists `old → new` pairs) + **[↩ Undo]**; `/cb list` shows the new ids; the blocks look identical (same texture/slot); locked block skipped.

- **② Undo the batch** — `/cb undo`
  - ✅ **Pass:** all the ids change back in one step.

- **③ Collision is skipped, not clobbered** — with `a` and `b` existing, `/cb bulkreid id:a replace a b`
  - ✅ **Pass:** it refuses to turn `a` into the already-taken `b` — reported as "would collide", `b` untouched.

- **④ Big / all batch confirms** — `/cb bulkreid all suffix _x` on many blocks
  - ✅ **Pass:** asks to confirm in chat (clickable ✔/✖) before changing; ✔ applies as one undoable batch.

- **⑤ Guards** — `/cb bulkreid nope:nope prefix x` (no match) → "No blocks matched"; bad usage → usage line.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | pattern re-id changes matched ids, keeps texture/slot, skips locked |
| 🟥 | ② | one `/cb undo` reverts the whole batch |
| 🟥 | ③ | collisions skipped (no overwrite / no swap) |
| 🟥 | ④ | big/all batch asks to confirm |
| 🟥 | ⑤ | no-match + bad-usage guards |
| — | **0 / 5** | |

---

## §A1 · Step 1 → Step 2 GUI — the other 7 ops  ✅ Passed 2026-06-13

> 💡 **What it does:** edit-a-setting, delete, rename, move-to-category, duplicate, lock and favorite
> now open the same two-step builder export uses. **Step 1 is identical for every op** — only **Step 2** changes per op.

> 🧰 **Before you start:** `/cb bulkgui` opens the Bulk Hub — each tile starts that op's builder. Have ~10 throwaway blocks; pick a small set in Step 1 so mistakes are easy to undo.

**Step 1 is the same everywhere** — quick re-check on any op: opens at "None selected currently"; Option A filter-cycle (6 kinds) or Option B hand-pick + 🟩 green confirm; **Next →** lights on ≥1 match. Below is **Step 2 per op:**

- **① Edit a setting** (`/cb bulkproperty`, blue) — Step 2 = **Setting** chooser (glow/hardness/sound/collision) + **Value** chooser
  - ✅ **Pass:** both cycle (left/right-click); Apply → *"Set <prop>=<val> on N"* + **[↩ Undo]**; only the selected blocks change; `/cb undo` reverts the batch.
- **② Delete** (`/cb bulkdelete`, red) — Step 2 = Review + **⚠ Delete N** (no extra controls)
  - ✅ **Pass:** Apply deletes only the selected (locked skipped); big/all batch asks to confirm in chat; `/cb undo` restores all in one step.
- **③ Rename** (`/cb bulkrename`, yellow) — Step 2 = **Mode** (prefix/suffix/replace) + **Text** anvil
  - ✅ **Pass:** mode cycles; the text tile(s) open an anvil and fill; Apply is gray until the text is set; Apply → *"Renamed N"* + undo; only selected names change.
- **④ Move to category** (`/cb bulkcategory`, cyan) — Step 2 = **Category** anvil (existing names hinted; `none` clears)
  - ✅ **Pass:** anvil sets the target; Apply gray until set; Apply → moves only the selected; `/cb undo` reverts.
- **⑤ Duplicate** (`/cb bulkduplicate`, white) — Step 2 = Review + **✔ Duplicate N**
  - ✅ **Pass:** Apply makes `<id>_copy` blocks for the selected (texture + settings); `/cb undo` removes the copies.
- **⑥ Lock / Unlock** (`/cb bulklock` · `/cb bulkunlock`, green) — Step 2 = **Direction** (lock⇄unlock)
  - ✅ **Pass:** direction flips; Apply → *"Locked/Unlocked N"* with a chat **[undo]**; only selected change.
- **⑦ Favorite / Unfavorite** (`/cb bulkfavorite` · `/cb bulkunfavorite`, magenta) — Step 2 = **Direction** (favorite⇄unfavorite)
  - ✅ **Pass:** same as lock, per-player; only selected change.

> 🔎 **General checks (any op):** **Back** from Step 2 → Step 1 with the selection intact · the frame + header are colour-coded per op · a 0-match selection / missing rename·category text → Apply is gray with a deny sound.

**📋 Scorecard**

| ✓ | # | Op | Proves |
|:--:|:--:|---|---|
| 🟩 | ① | edit | setting+value choosers; applies + undo on selected only |
| 🟩 | ② | delete | deletes selected only; big-batch confirm; one undo restores |
| 🟩 | ③ | rename | mode + anvil text; gated until text set; undo |
| 🟩 | ④ | category | anvil target (none clears); undo |
| 🟩 | ⑤ | duplicate | `<id>_copy` for selected; undo removes copies |
| 🟩 | ⑥ | lock | direction flip; chat-[undo]; selected only |
| 🟩 | ⑦ | favorite | direction flip; per-player; selected only |
| — | **7 / 7** | |

---

## §A0 · Bulk Export — Step 1 → Step 2 GUI  ✅ Passed 2026-06-13

> 💡 **What it does:** `/cb bulkexport` opens a 2-step builder: **Step 1** pick which blocks, **Step 2** review + choose a format + export.

> 🧰 **Before you start:** have ~10 blocks (a mix of categories; favorite/lock a couple). Open with **`/cb bulkexport`** (no args) — or the Bulk Hub's **Export** tile (`/cb bulkgui` → Export).

**Try these:**

- **① Opens at "nothing selected"** — run `/cb bulkexport`
  - ✅ **Pass:** Step 1 opens; header + the Selected spyglass both say **"None selected currently"**; **Next →** is gray and plays a deny sound if clicked.
  - ❌ **Broken if:** it opens the old single-screen dashboard, or Next is already lit.
- **② Option A — filter cycle** — left-click **Option A — Filter** to cycle the kinds (right-click cycles back)
  - ✅ **Pass:** it cycles **All blocks → Category → Favorited → Locked → Name contains → ID starts**; the chosen kind highlights; the Selected spyglass + count update live; **Next →** lights for kinds that match ≥1 block.
- **③ Option A — text kinds** — cycle to **Category** (or Name / ID)
  - ✅ **Pass:** a **↳ value** tile appears → click → anvil → type e.g. a real category → take it → returns to Step 1 with that value filled and the count updated.
- **④ Option B — hand-pick** — click **Option B — Select specific blocks**
  - ✅ **Pass:** the block list opens; tick a few (they glint ✔); a **green block — "Use these N block(s)"** (slot 52, bottom-right) confirms → returns to Step 1 showing **"Picked blocks: N"**; Next lights.
  - 🔎 Nothing ticked → the confirm is gray "Tick some blocks first". **Back** cancels the pick.
- **⑤ Step 2 — review + format + export** — with a selection, click **Next →**
  - ✅ **Pass:** Step 2 shows **🔍 Review** listing the exact block ids; **📄 Format** cycles **all** of json · txt · png · csv · md · html · yaml; **✔ Click to Export** writes the file → chat confirms. The file in the exports folder matches the selection + format.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟩 | ① | opens the new 2-step builder at "None selected"; Next locked |
| 🟩 | ② | filter cycle covers all 6 kinds; count + Next update live |
| 🟩 | ③ | category/name/id reveal the anvil value tile; gate on empty value |
| 🟩 | ④ | hand-pick → green-concrete confirm → "Picked blocks: N" |
| 🟩 | ⑤ | Step 2 review + all 7 formats; export writes only the selected set |
| — | **5 / 5** | |

---

## ✅ §C · Bulk core — passed 2026-06-12

> Confirmed in-game across several rounds. Kept for re-test reference.

**Filters (every op):** `all` · `category:x` · `id:prefix` · `name:text` (`name:text*` = starts-with)
· `favorite:yes/no` · `locked:yes/no` · explicit `id1,id2,…` list.

- ☑️ **Tab-complete** after every bulk subcommand (filters → property → values, even after a comma); plus `/cb arabic letter`, `/cb retextureall`, `/cb video extract`.
- ☑️ **Dashboard 2.0** — colour-coded frame per op (blue Edit / red Delete / yellow Rename…), numbered steps with `»` connectors, ▸ on the current choice, a **Matched** spyglass tile, glint on the current pick, typed **Name contains… / ID starts…** via anvil.
- ☑️ **GUI sounds** — chime on click · XP blip on apply · deep bass on delete/disabled · page-turn on open.
- ☑️ **Lock / Favorite in the dashboard** — the Operation cycle adds them, each with a Lock⇄Unlock / Favorite⇄Unfavorite direction tile.
- ☑️ **Move to category** — `/cb bulkcategory <filter> <cat>` (`none` clears); cyan dashboard op; anvil category; one undo reverts the batch.
- ☑️ **Command twin** — a tile showing the exact command; click → drops it into your chat box (doesn't auto-run).
- ☑️ **Back goes home** — Back from `/cb bulkgui` opens the Main Menu; ✖ Close closes.
- ☑️ **Property edit · delete · batch-undo · clickable confirm · hover-for-list** — all confirmed.

---

## ⏳ §D · Not built

| Feature | What it'll do | Note |
|---|---|---|
| 🎨 Bulk **recolor (edge)** | recolour many blocks' backgrounds to a chosen colour | recolour replaces the texture with no undo — decide: OK, or build texture-undo? |
| 🔁 Bulk **reid** GUI | reassign ids by pattern across many blocks (Step1→Step2 GUI front end for the working command) | riskier — ids key locks/favorites/notes/drafts |

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs expected
- 📄 Last ~20 lines of `.minecraft/logs/latest.log`

## 🧹 Cleanup
`/cb delete r1` · `/cb delete r2` · `/cb delete r3` · `/cb delete d1` · `/cb delete d1_copy` · `/cb delete bad1` · `/cb delete good1` · `/cb delete jordan`
