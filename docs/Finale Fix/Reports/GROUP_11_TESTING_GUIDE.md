# 🧪 Group 11 — Category System — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`
> ✅ **G11.1–G11.17 confirmed in-game 2026-06-14** (basics §7 + the whole round-1 overhaul §1–§5).
> ✅ **Round 2 (§R2–§R4) confirmed in-game 2026-06-14** — anvil inputs, unified `/cb category`, Export "Bulk Choose".
> ➡️ The resource-pack join-prompt bug is **NOT a Group 11 item** — it lives in **GROUP_05_TESTING_GUIDE §3** now.

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟢 All pass |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 · 40 / 40 passed |
| **Last tested** | 2026-06-14 |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## What changed in ROUND 2 (read first)

- **Anvil instead of chat:** CategoryEditMenu **Rename / Merge / Description** now open an anvil to type in. **Bulk Retexture stays on chat** on purpose — image URLs are too long for an anvil.
- **Unified `/cb category <action>`:** the scattered `renamecategory` / `mergecategory` / `categorydesc` / `givecategory` / `exportcategory` / `sharecategory` / `importcategory` commands are **GONE**, folded into one base command (rename · merge · delete · color · desc · icon · sort · lock · unlock · give · export · share · import · info · list · edit). `/cb setcategory` and `/cb categories` are unchanged.
- **Export Dashboard:** "Per Selection" is now **"Bulk Choose"** (opens the block list to tick blocks, then export them). Per Block / Per Category / Bulk Choose now show the **same seven format tiles** as All Blocks.

> ℹ️ Round 1 (below, §1–§5) is kept for re-test reference. Note the round-1 wording mentions the old `/cb renamecategory` etc. — those are now `/cb category rename …`. The GUI tiles do the same thing.

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| ✅ Passed 2026-06-14 | **Anvil inputs** — Rename / Merge / Description open an anvil (not chat) | §R2 |
| ✅ Passed 2026-06-14 | **Unified `/cb category`** — rename/merge/delete/color/desc/… + old commands gone | §R3 |
| ✅ Passed 2026-06-14 | **Export "Bulk Choose"** + same 7 formats on every scope | §R4 |
| ✅ Passed 2026-06-14 | round-1 overhaul: browse/edit tiles · CategoryEditMenu · Export GUI · removed cmds | §1–§5 |
| ✅ Passed 2026-06-14 | basics: browser GUI · slot sub-menu · categories list · give · setcategory · export ZIP | §7 |
| ➡️ Moved out | resource-pack join prompt bug → **GROUP_05_TESTING_GUIDE §3** | — |
| 🟡 Polish later | Lock/Unlock All · Bulk Retexture (cmd not built) · Stats · Sort Order | §6 |
| ⏳ Coming | **Share / Import** category by code — waits on the cb-cloud-vault Worker | §6 |

> 👍 **Nothing to test right now in Group 11** — all of round 2 passed. The §R sections below are kept for re-test reference.

---

## 🧰 Setup (run once)

```
/cb create g11a CategoryTestBlock1
/cb create g11b CategoryTestBlock2
/cb create g11c CategoryTestBlock3
/cb setcategory g11a testcat
/cb setcategory g11b testcat
/cb setcategory g11c testcat
```

---

# ✅ Passed — round 2 (confirmed in-game 2026-06-14)

> Anvil inputs, the unified `/cb category` command, and the Export "Bulk Choose" rework all passed in-game. Kept for re-test reference.

## §R2 · Anvil inputs (no more chat typing)

> 💡 **What it does:** Rename / Merge / Description in the CategoryEditMenu now open an **anvil** to type in.

> 🧰 **Before you start:** `/cb category edit testcat` (or right-click testcat in `/cb categories`).

**Try these:**

- **① Rename via anvil** — click **Rename Category** → an anvil opens pre-filled with `testcat` → type a new name → take the result.
  - ✅ **Pass:** an anvil opened (not chat); the category renames; the edit menu reopens on the new name.
- **② Description via anvil** — click **Description** → type text in the anvil → take it.
  - ✅ **Pass:** anvil opened; the description saves and shows in the tile/browser header.
- **③ Merge via anvil** — click **Merge Into…** → type a target category → take it.
  - ✅ **Pass:** anvil opened; blocks move; you land back on the category list.
- **④ Bulk Retexture stays chat** — click **Bulk Retexture**.
  - ✅ **Pass:** this one still opens a **chat** prompt (URLs are too long for an anvil).

**📋 Scorecard** — ✅ ① rename anvil · ✅ ② desc anvil · ✅ ③ merge anvil · ✅ ④ retexture still chat

---

## §R3 · Unified `/cb category <action>`

> 💡 **What it does:** one base command replaces all the scattered category commands.

**Try these:**

- **① Old commands are gone** — `/cb renamecategory testcat foo`, `/cb mergecategory a b`, `/cb categorydesc testcat hi`, `/cb givecategory testcat`, `/cb exportcategory testcat`.
  - ✅ **Pass:** every one is an unknown command now.
- **② rename / merge / delete** — `/cb category rename testcat blockstest`, then `/cb category merge othercat blockstest`, then `/cb category delete blockstest`.
  - ✅ **Pass:** rename updates all blocks; merge empties+removes the source; delete uncategorizes (keeps blocks).
- **③ color / desc / icon / sort** — `/cb category color blockstest aqua`, `/cb category desc blockstest A test`, `/cb category icon blockstest g11a`, `/cb category sort blockstest custom`.
  - ✅ **Pass:** name tints aqua in `/cb categories`; description shows; icon becomes g11a; sort flips to Custom.
- **④ lock / unlock / give / export** — `/cb category lock blockstest`, `/cb category unlock blockstest`, `/cb category give blockstest`, `/cb category export blockstest`.
  - ✅ **Pass:** all blocks lock then unlock; you get one of each; a ZIP is written with a copy-path button.
- **⑤ info / list / edit** — `/cb category info blockstest`, `/cb category list`, `/cb category edit blockstest`.
  - ✅ **Pass:** info prints a text summary; list opens the browser GUI; edit opens the CategoryEditMenu.
- **⑥ Tab-complete** — type `/cb category ` and press Tab.
  - ✅ **Pass:** the actions are suggested; `color` suggests colour words; `sort` suggests alpha/custom.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | old scattered commands removed |
| ✅ | ② | rename / merge / delete work |
| ✅ | ③ | color / desc / icon / sort work |
| ✅ | ④ | lock / unlock / give / export work |
| ✅ | ⑤ | info / list / edit work |
| ✅ | ⑥ | tab-completion suggests actions + values |
| — | **6 / 6** | |

---

## §R4 · Export Dashboard — Bulk Choose + standard formats

> 💡 **What it does:** "Per Selection" is now **Bulk Choose** (pick blocks from the list), and every scope shows the same seven format tiles as "All Blocks".

> 🧰 **Before you start:** `/cb export` (as a player).

**Try these:**

- **① Bulk Choose opens the list** — click **Bulk Choose**.
  - ✅ **Pass:** the block list opens; left-click ticks blocks (✔). A green **Export these N block(s)** tile appears at the bottom-right once ≥1 is ticked.
- **② Confirm → formats** — tick 2–3 blocks → click **Export these N block(s)**.
  - ✅ **Pass:** you return to the dashboard showing seven format tiles (.json/.txt/.csv/.md/.html/.yaml/PNG). Click one → it exports just the picked blocks.
- **③ Per Block has all formats** — back to scopes → **Per Block** → pick a block.
  - ✅ **Pass:** the same seven format tiles appear.
- **④ Per Category has all formats** — back to scopes → **Per Category** → pick a category.
  - ✅ **Pass:** the seven format tiles appear, **plus** a "Category ZIP" tile.
- **⑤ All Blocks unchanged** — back to scopes → **All Blocks**.
  - ✅ **Pass:** the same seven-tile screen as before.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | Bulk Choose opens the tickable list |
| ✅ | ② | confirm returns to formats; exports the picked set |
| ✅ | ③ | Per Block shows all 7 formats |
| ✅ | ④ | Per Category shows all 7 formats + ZIP |
| ✅ | ⑤ | All Blocks still correct |
| — | **5 / 5** | |

> ↩️ **Undo the test:** exports are written files in the exports folder — delete them if you like.

---

# ✅ Passed — round 1 (confirmed in-game 2026-06-14)

> The category overhaul below passed in-game. Kept for re-test reference. Remember: the §3 wording shows the old `/cb renamecategory` etc. — those are now `/cb category rename …` (§R3).

## §1 · Category tiles — browse vs edit

> 💡 **What it does:** the `/cb categories` overview now splits each tile into two actions — left-click browses, right-click opens the new edit menu.

**Try these:**

- **① Tile lore** — `/cb categories`, then hover the **testcat** tile.
  - ✅ **Pass:** lore shows the block count, the description (if any), and the two lines **"Left-click to browse"** + **"Right-click to edit."**
- **② Left-click browses** — left-click **testcat**.
  - ✅ **Pass:** the category **browser** opens (g11a, g11b, g11c) — same as before.
- **③ Right-click edits** — right-click **testcat**.
  - ✅ **Pass:** the **CategoryEditMenu** opens.
  - ❌ **Broken if:** right-click opens the browser, or nothing happens.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | tiles show browse + edit hint lore |
| ✅ | ② | left-click → browser |
| ✅ | ③ | right-click → CategoryEditMenu |
| — | **3 / 3** | |

---

## §2 · CategoryEditMenu — Display Block · Color Tag · Description · Delete

> 💡 **What it does:** the single edit hub for a category.

> 🧰 **Before you start:** right-click **testcat** in `/cb categories` to open the edit menu.

**Try these:**

- **① All tiles present** — look at the menu.
  - ✅ **Pass:** you see Display Block, Rename, Merge, Export, Share *(greyed "coming soon")*, Lock/Unlock All, Bulk Retexture, Stats, Color Tag, Description, Sort Order, Delete.
- **② Display Block picker** — click **Display Block** → pick a block in the picker.
  - ✅ **Pass:** back in `/cb categories`, the **testcat** icon is now that block's item.
- **③ Color Tag cycles** — click **Color Tag** a few times.
  - ✅ **Pass:** each click changes the tag colour; in `/cb categories` the **testcat name text** tints to match.
  - ❌ **Broken if:** the *icon* changes colour instead of the name text.
- **④ Description** — click **Description** → type `My test category` in the anvil.
  - ✅ **Pass:** the description shows in the browser header and in the tile lore on `/cb categories`.
- **⑤ Delete (with confirm)** — click **Delete** → confirm in the sub-menu.
  - ✅ **Pass:** "testcat" disappears from `/cb categories`, but g11a/b/c **still exist** (now uncategorized). They are **not** deleted.
  - ❌ **Broken if:** the blocks themselves vanish, or the category deletes with no confirm step.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | edit menu shows all tiles |
| ✅ | ② | Display Block tile sets the category icon |
| ✅ | ③ | Color Tag tints the name text (not the icon) |
| ✅ | ④ | Description tile sets the header text |
| ✅ | ⑤ | Delete uncategorizes blocks (keeps them) after a confirm |
| — | **5 / 5** | |

> ↩️ **Undo the test:** if you deleted testcat, re-run the Setup block to rebuild it.

---

## §3 · Text commands — rename · merge · description

> 💡 **What it does:** the same rename / merge / describe actions as the edit tiles, but as plain commands.

> 🧰 **Before you start:** testcat with g11a/b/c (re-run Setup if you deleted it in §2).

**Try these:**

- **① Rename** — `/cb renamecategory testcat blockstest`
  - ✅ **Pass:** `/cb categories` now shows **blockstest** (not testcat); g11a/b/c report category blockstest.
- **② Merge** — `/cb create g11f MergeTest` → `/cb setcategory g11f othercat` → `/cb mergecategory othercat blockstest`
  - ✅ **Pass:** g11f moves into **blockstest**; **othercat** disappears from `/cb categories`.
  - ❌ **Broken if:** othercat stays, or only some blocks move.
- **③ Description** — `/cb categorydesc blockstest A merged test category`
  - ✅ **Pass:** that text shows in the browser header / tile lore for blockstest.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | renamecategory moves every member block |
| ✅ | ② | mergecategory empties + removes the source |
| ✅ | ③ | categorydesc sets the description |
| — | **3 / 3** | |

---

## §4 · Export Dashboard GUI

> 💡 **What it does:** `/cb export` with no args now opens a chest GUI for players. Phase 1 = pick a **scope**; clicking a scope redraws the same GUI with the **format** choices.

**Try these:**

- **① Open it** — `/cb export` (as a player, no args).
  - ✅ **Pass:** an Export Dashboard chest opens with scope tiles: **Per Block · Per Category · All Blocks · Per Selection**.
- **② Scope → format redraw** — click **All Blocks**.
  - ✅ **Pass:** the same GUI redraws in place showing format tiles.
- **③ Back** — click the **← Back** tile.
  - ✅ **Pass:** returns to the scope tiles.
- **④ Per Category / Per Block pickers** — click **Per Category** (and **Per Block**).
  - ✅ **Pass:** a category (or block) picker appears; choosing one leads to its format tiles.
- **⑤ Console still gets text** — run `/cb export` from the **server console**.
  - ✅ **Pass:** the console prints the text export output — unchanged from before.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | player gets the dashboard GUI, not chat text |
| ✅ | ② | picking a scope redraws with format tiles |
| ✅ | ③ | Back returns to scope selection |
| ✅ | ④ | Per Category / Per Block pickers work |
| ✅ | ⑤ | console output still text |
| — | **5 / 5** | |

---

## §5 · Removed commands are gone

**Try these:**

- **① setdisplayblock** — `/cb setdisplayblock blockstest g11a`
  - ✅ **Pass:** unknown-command / usage error.
- **② cleardisplayblock** — `/cb cleardisplayblock blockstest`
  - ✅ **Pass:** unknown-command error.
- **③ autocategorize** — `/cb autocategorize g11a`
  - ✅ **Pass:** unknown-command error. *(Auto-categorize still appears as a hint when you `/cb create` a block.)*

**📋 Scorecard** — ✅ ① setdisplayblock gone · ✅ ② cleardisplayblock gone · ✅ ③ autocategorize command gone

> 💡 **Sanity check ④** — `/cb create g11g RedBrickWall` (no URL): chat should still offer a one-click **[Add] / [Edit]** category hint.

---

# ✅ Passed — kept for re-test reference

## ✅ §7 · Category basics — passed 2026-06-14

> Confirmed in-game by the developer 2026-06-14. Kept so you can re-check after the overhaul.

| ✓ | # | Test | Proves |
|:--:|:--:|---|---|
| ✅ | G11.1 | `/cb categories` → click a tile | category browser opens as a **chest GUI** |
| ✅ | G11.2 | click a block slot in the browser | sub-menu opens (Give / Edit / Remove) |
| ✅ | G11.3 | `/cb categories` | category overview list (console still prints text) |
| ✅ | G11.4 | `/cb givecategory testcat` | gives one of every block in the category |
| ✅ | G11.5 | `/cb setcategory g11d testcat` | adds a block to a category |
| ✅ | G11.6 | browser **Export** tile | writes a ZIP file + chat link |
| — | **6 / 6** | |

> ⚠️ The overhaul changed the **browser action row** — the old "Set icon" tile is now **Edit Category**. If you re-test G11.1/G11.2, expect that tile to read **Edit Category**.

---

# 🟡 Polish later

- **Lock / Unlock All** tile — locks/unlocks every block in the category. Built, untested.
- **Bulk Retexture** tile — re-apply one URL to all blocks. Built, untested.
- **Stats** tile — block count, total texture size, locked count, oldest/newest. Built, untested.
- **Sort Order** tile — custom display order; default = alphabetical by block name. Built, untested.

# ⏳ Not built

| Feature | What it'll do |
|---|---|
| **Share category** | upload a category → short share code *(needs the cb-cloud-vault Worker deployed)* |
| **Import category** | download by code; a GUI asks: keep original category / new category / leave uncategorized |

> The **Share** tile in CategoryEditMenu is intentionally greyed "coming soon" until the vault Worker is live.

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs what you expected (📸 a screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete g11a` · `/cb delete g11b` · `/cb delete g11c` · `/cb delete g11f` · `/cb delete g11g`
*(any block left uncategorized by a Delete test still shows in `/cb listgui` — delete from there too)*
