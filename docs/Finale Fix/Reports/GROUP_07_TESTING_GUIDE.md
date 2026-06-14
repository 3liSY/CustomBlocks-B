# 🧪 Group 07 — Bulk Operations — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| ✅ Passed 2026-06-13 (⚠ partial) | **`/cb bulkreid` command** — works; GUI + polish deferred for later | §A2 |
| ✅ Passed 2026-06-13 | **Step 1 → Step 2 GUI — all 8 bulk ops** (edit·delete·rename·category·duplicate·export·lock·favorite). Old dashboard removed. | §A0/§A1 |
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
> Export passed in-game (§A0); the other 7 ops are now built on the same shared flow — test in §A1.

---

# 🎯 Test now

## §A2 · `/cb bulkreid` command — change many IDs by a pattern  ✅ Passed 2026-06-13 (⚠ partial)

> ⚠️ **Partial — deferred for later:** the command works in-game. Still to do (parked): the **Step1→Step2
> GUI** for reid + a Bulk Hub tile, and a **polish** pass (no-arg currently shows usage). Revisit with recolor.

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
  - ✅ **Pass:** chat *"Re-id'd N block(s)"* (hover lists `old → new` pairs) + **[↩ Undo]**; `/cb list` shows the new ids; the blocks look identical (same texture/slot); locked block skipped (shown in the count).
- **② Undo the batch** — `/cb undo`
  - ✅ **Pass:** all the ids change back in one step.
- **③ Collision is skipped, not clobbered** — with `a` and `b` existing, `/cb bulkreid id:a replace a b`
  - ✅ **Pass:** it refuses to turn `a` into the already-taken `b` — reported as "would collide", `b` untouched (no overwrite, no swap).
- **④ Big / all batch confirms** — `/cb bulkreid all suffix _x` on many blocks
  - ✅ **Pass:** asks to confirm in chat (clickable ✔/✖) before changing; ✔ applies as one undoable batch.
- **⑤ Guards** — `/cb bulkreid nope:nope prefix x` (no match) → "No blocks matched"; bad usage → usage line.

> ⚙️ **How it works:** reuses `BulkScope` for the filter, the tested `SlotManager.reId` per block, and
> records the batch as `REID` undo children (same path single `/cb reid` uses). Skips locked, no-change,
> invalid-id, and collision cases. No new id/undo machinery.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | pattern re-id changes matched ids, keeps texture/slot, skips locked |
| ⬜ | ② | one `/cb undo` reverts the whole batch |
| ⬜ | ③ | collisions skipped (no overwrite / no swap) |
| ⬜ | ④ | big/all batch asks to confirm |
| ⬜ | ⑤ | no-match + bad-usage guards |

> ⏭️ **After this passes:** add the **reid** op to the Step 1 → Step 2 GUI (a thin front-end over this
> command, like the other ops) + a Bulk Hub tile.

---

## §A1 · Step 1 → Step 2 GUI — the other 7 ops  ✅ Passed 2026-06-13  *(same flow as export)*

> 💡 **What it does:** edit-a-setting, delete, rename, move-to-category, duplicate, lock and favorite
> now open the **same** two-step builder export uses. **Step 1 is identical for every op** (you already
> tested it for export) — only **Step 2** changes per op. Apply runs the same tested `/cb bulk…`
> command underneath, so the confirm-on-big-batch + one-`/cb undo` behaviour is unchanged.

> 🧰 **Before you start:** `/cb bulkgui` opens the Bulk Hub — each tile starts that op's builder. (Each
> `/cb bulk<op>` with no args opens it too.) Have ~10 throwaway blocks; pick a small set in Step 1 so
> mistakes are easy to undo.

**Step 1 is the same everywhere** — quick re-check on any op: opens at "None selected currently";
Option A filter-cycle (6 kinds) or Option B hand-pick + 🟩 green confirm; **Next →** lights on ≥1 match.
Below is **Step 2 per op** (open via the Hub, pick a tiny selection, then Next →):

- **① Edit a setting** (`/cb bulkproperty`, blue) — Step 2 = **Setting** chooser (glow/hardness/sound/collision) + **Value** chooser
  - ✅ **Pass:** both cycle (left/right-click); Apply → *"Set <prop>=<val> on N"* + **[↩ Undo]**; only the selected blocks change; `/cb undo` reverts the batch.
- **② Delete** (`/cb bulkdelete`, red) — Step 2 = Review + **⚠ Delete N** (no extra controls)
  - ✅ **Pass:** Apply deletes only the selected (locked skipped); big/all batch asks to confirm in chat; `/cb undo` restores all in one step.
- **③ Rename** (`/cb bulkrename`, yellow) — Step 2 = **Mode** (prefix/suffix/replace) + **Text** anvil (+ **Replace with** anvil in replace mode)
  - ✅ **Pass:** mode cycles; the text tile(s) open an anvil and fill; Apply is gray until the text is set; Apply → *"Renamed N"* + undo; only selected names change.
- **④ Move to category** (`/cb bulkcategory`, cyan) — Step 2 = **Category** anvil (existing names hinted; `none` clears)
  - ✅ **Pass:** anvil sets the target; Apply gray until set; Apply → moves only the selected; `/cb undo` reverts.
- **⑤ Duplicate** (`/cb bulkduplicate`, white) — Step 2 = Review + **✔ Duplicate N** (no extra controls)
  - ✅ **Pass:** Apply makes `<id>_copy` blocks for the selected (texture + settings); `/cb undo` removes the copies.
- **⑥ Lock / Unlock** (`/cb bulklock` · `/cb bulkunlock`, green) — Step 2 = **Direction** (lock⇄unlock)
  - ✅ **Pass:** direction flips; Apply → *"Locked/Unlocked N"* with a chat **[undo]** (the opposite op); only selected change.
- **⑦ Favorite / Unfavorite** (`/cb bulkfavorite` · `/cb bulkunfavorite`, magenta) — Step 2 = **Direction** (favorite⇄unfavorite)
  - ✅ **Pass:** same as lock, per-player; only selected change.

> ⚙️ **How it works:** one shared `BulkSelectMenu` (Step 1) feeds one op-driven `BulkActionMenu`
> (Step 2). Step 2's Apply passes the Step-1 scope (a `BulkScope` filter string, or the ticked ids as a
> comma-list) to the same `applyXFromGui` command path each op already used. No bulk logic was rewritten.

> 🔎 **General checks (any op):** **Back** from Step 2 → Step 1 with the selection intact · the frame +
> header are colour-coded per op · a 0-match selection / missing rename·category text → Apply is gray
> with a deny sound.

**📋 Scorecard**

| ✓ | # | Op | Proves |
|:--:|:--:|---|---|
| ⬜ | ① | edit | setting+value choosers; applies + undo on selected only |
| ⬜ | ② | delete | deletes selected only; big-batch confirm; one undo restores |
| ⬜ | ③ | rename | mode + anvil text; gated until text set; undo |
| ⬜ | ④ | category | anvil target (none clears); undo |
| ⬜ | ⑤ | duplicate | `<id>_copy` for selected; undo removes copies |
| ⬜ | ⑥ | lock | direction flip; chat-[undo]; selected only |
| ⬜ | ⑦ | favorite | direction flip; per-player; selected only |

---

## §A0 · Bulk Export — Step 1 → Step 2 GUI  ✅ Passed 2026-06-13  *(the redesign template)*

> 💡 **What it does:** `/cb bulkexport` no longer opens the old dashboard. It opens a 2-step builder:
> **Step 1** pick which blocks (a filter rule *or* hand-pick them), **Step 2** review + choose a format
> + export. Underneath it runs the same tested `/cb bulkexport` command — only the front door changed.

> 🧰 **Before you start:** have ~10 blocks (a mix of categories; favorite/lock a couple so those
> filters have something to match). Open with **`/cb bulkexport`** (no args) — or the Bulk Hub's
> **Export** tile (`/cb bulkgui` → Export).

**Try these:**

- **① Opens at "nothing selected"** — run `/cb bulkexport`
  - ✅ **Pass:** Step 1 opens; header + the Selected spyglass both say **"None selected currently"**; **Next →** is gray and plays a deny sound if clicked.
  - ❌ **Broken if:** it opens the old single-screen dashboard, or Next is already lit.
- **② Option A — filter cycle** — left-click **Option A — Filter** to cycle the kinds (right-click cycles back)
  - ✅ **Pass:** it cycles **All blocks → Category → Favorited → Locked → Name contains → ID starts**; the chosen kind highlights; the Selected spyglass + count update live; **Next →** lights for kinds that match ≥1 block.
- **③ Option A — text kinds** — cycle to **Category** (or Name / ID)
  - ✅ **Pass:** a **↳ value** tile (name-tag) appears → click → anvil → type e.g. a real category → take it → returns to Step 1 with that value filled and the count updated.
  - 🔎 With the value still blank, Next stays gray ("type text first" sense).
- **④ Option B — hand-pick** — click **Option B — Select specific blocks**
  - ✅ **Pass:** the block list opens; tick a few (they glint ✔); a **green block — "Use these N block(s)"** (slot 52, bottom-right) confirms → returns to Step 1 showing **"Picked blocks: N"**; Next lights.
  - 🔎 Nothing ticked → the confirm is gray "Tick some blocks first" (deny on click). **Back** (no confirm) cancels the pick.
- **⑤ Step 2 — review + format + export** — with a selection, click **Next →**
  - ✅ **Pass:** Step 2 shows **🔍 Review** listing the exact block ids; **📄 Format** cycles **all** of json · txt · png · csv · md · html · yaml; **✔ Click to Export** (next to Format) writes the file → chat: *"Exported N block(s) → exports/…"* (png → *"Exported N texture PNG(s)…"*). The file in `config/customblocks/exports/` matches the selection + format.
  - **↩ Change selection** / **Back** → returns to Step 1 with the selection intact.
  - ❌ **Broken if:** the export hits blocks you didn't select, or a format is missing.

> ⚙️ **How it works:** Step 1 stores the choice on `BulkSession` (a `BulkScope` filter string, or the
> ticked ids as a comma-list); Step 2's export tile passes that straight to the already-tested
> `/cb bulkexport <scope> <format>` path. No export logic was rewritten.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | opens the new 2-step builder at "None selected"; Next locked |
| ⬜ | ② | filter cycle covers all 6 kinds; count + Next update live |
| ⬜ | ③ | category/name/id reveal the anvil value tile; gate on empty value |
| ⬜ | ④ | hand-pick → green-concrete confirm → "Picked blocks: N" |
| ⬜ | ⑤ | Step 2 review + all 7 formats; export writes only the selected set |

---

## §A · `/cb listgui` upgrades — search · multi-select · bulk-on-selection  *(jar 13:18)*

> 💡 **What it does:** the block list is now a front door to bulk — search it, tick blocks, send
> just those to the hub.
> ⚠️ **In the list now: left-click = tick/untick · right-click = edit** (it used to be left-click = edit).

> 🧰 **Before you start:** `/cb listgui` with ~10 blocks (a mix of categories; lock/favorite a couple).

**Try these:**

- **① Search the list** — bottom-row **Search** (compass) → left-click → anvil → type id/name/category → take it
  - ✅ **Pass:** the list shows only matches; paging keeps the search; **right-click Search** clears it; a no-match search shows the placeholder.
- **② Tick-many select** — left-click several blocks
  - ✅ **Pass:** each glints green **✔ selected**; **N selected** (slot 47) counts up. Left-click again un-ticks · **right-click** opens the editor (ticks survive) · **Select all shown** (slot 51) ticks the whole filtered list · click **N selected** to clear.
- **③ Bulk on selection** — tick 2–3 → **Bulk actions on N selected** (slot 52, chest) → click
  - ✅ **Pass:** the Bulk Hub opens pre-filtered to those blocks (its **Matched** spyglass lists them). Run any op → only the ticked blocks change. **Back** → the list.
  - 🔎 Nothing ticked → gray "Tick some blocks first" (deny sound on click).
- **④ Delete path (one undo)** — tick throwaways → Bulk actions → **Delete** → confirm
  - ✅ **Pass:** only those go; `/cb undo` brings them all back in one step.

> ⚙️ **How it works:** the ticked ids seed the Bulk Hub's filter as a comma id-list (`BulkScope`
> resolves it), so every existing bulk op runs on the hand-picked set — no new bulk logic. Ticks
> persist until you clear them or restart.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | search filters by id/name/category, survives paging, clears |
| ⬜ | ② | tick/untick + select-all + clear; right-click still edits |
| ⬜ | ③ | selection feeds the Bulk Hub; only those blocks change |
| ⬜ | ④ | bulk delete on selection is one undoable batch |

---

## §B · Duplicate · Export · despeckle v2 · broken-link fix  *(jar 09:49)*

> 🧰 **Before you start:** a couple of textured blocks; a real image URL for the despeckle test.

**Try these:**

- **① Broken-link create makes nothing** — `/cb create bad1 Test https://example.com/not-a-real-image.png`
  - ✅ **Pass:** chat says it couldn't get an image and `bad1` is **NOT** created. A good URL after → the block appears only once the image is fetched.
- **② Despeckle v2 (edge-aware fill)** — `/cb config background BgRemove` → `/cb create jordan Jordan <logo-url>`
  - ✅ **Pass:** a dark-outlined logo keeps its border **visible** (white fill behind it); bright logos without dark outlines still get a black fill.
  - ⚠️ Wrong fill colour on some image? Send it — `EDGE_DARK_FRACTION` is a one-line tune.
- **③ Bulk duplicate** — `/cb create d1 Apple <url>` → `/cb bulkduplicate id:d` → `/cb undo`
  - ✅ **Pass:** `d1_copy` appears with the same texture + settings (not blank); undo removes it. Dashboard: `/cb bulkgui` → Duplicate → filter → Apply.
- **④ Bulk export** — `/cb bulkexport all json` · `/cb bulkexport category:fruit txt`
  - ✅ **Pass:** a file lands in `config/customblocks/exports/` with that many blocks. Tab `/cb bulkexport all ` → suggests `json`/`txt`. Dashboard: Export → filter → Format toggle → Apply.
- **⑤ Hub has all 8 ops** — `/cb bulkgui` → Edit · Rename · Move · Duplicate · Export · Lock · Favorite · Delete, each opening the dashboard in that mode.

---

# ✅ Passed — kept for re-test reference

## ✅ §C · Bulk core — passed 2026-06-12

> Confirmed in-game across several rounds (jars 21:39 → 09:11). Kept for re-test reference.

**Filters (every op):** `all` · `category:x` · `id:prefix` · `name:text` (`name:text*` = starts-with)
· `favorite:yes/no` · `locked:yes/no` · explicit `id1,id2,…` list.

- ☑️ **Tab-complete** after every bulk subcommand (filters → property → values, even after a comma); plus `/cb arabic letter`, `/cb retextureall`, `/cb video extract`.
- ☑️ **Dashboard 2.0** — colour-coded frame per op (blue Edit / red Delete / yellow Rename…), numbered steps with `»` connectors, ▸ on the current choice, a **Matched** spyglass tile, glint on the current pick, typed **Name contains… / ID starts…** via anvil.
- ☑️ **GUI sounds** — chime on click · XP blip on apply · deep bass on delete/disabled · page-turn on open.
- ☑️ **Lock / Favorite in the dashboard** — the Operation cycle adds them, each with a Lock⇄Unlock / Favorite⇄Unfavorite direction tile.
- ☑️ **Move to category** — `/cb bulkcategory <filter> <cat>` (`none` clears); cyan dashboard op; anvil category; one undo reverts the batch.
- ☑️ **Command twin** — a tile showing the exact command; click → drops it into your chat box (doesn't auto-run).
- ☑️ **Back goes home** — Back from `/cb bulkgui` opens the Main Menu; ✖ Close closes.
- ☑️ **Property edit · delete · batch-undo · clickable confirm · hover-for-list** — all confirmed. Setting edits don't reload the pack (correct); delete does.

---

## ⏳ §D · Coming next  *(not built — nothing to test)*

| Feature | What it'll do | Note |
|---|---|---|
| 🛠️ **Step1→Step2 GUI redesign** | every bulk command → selection GUI (filter or hand-pick + 🟩 green-concrete confirm) → action GUI (review · format chooser · confirm) | **Export first** as the template, then the rest |
| 🎨 Bulk **recolor (edge)** | recolour many blocks' backgrounds to a chosen colour | recolour replaces the texture with no undo (like `/cb retexture`) — decide: OK, or build texture-undo? Forces *edge* mode in batches. |
| 🔁 Bulk **reid** | reassign ids by pattern across many blocks | riskier — ids key locks/favorites/notes/drafts, so it must migrate all of those. Single-block `/cb reid` is done (Group 25). |

---

## 🆘 If a test fails, send me
- 🔢 the step number · 👀 what happened vs expected · 📄 last ~20 lines of `.minecraft/logs/latest.log`

## 🧹 Cleanup
`/cb delete r1` · `/cb delete r2` · `/cb delete r3` · `/cb delete d1` · `/cb delete d1_copy` · `/cb delete bad1` · `/cb delete good1` · `/cb delete jordan`
