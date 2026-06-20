# 🧪 Group 25 — Block Management Extras — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Partial |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 · 8 / 11 passed |
| **Last tested** | 2026-06-13 (§2) |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **Reid GUI** — pick-a-block menu · single-arg anvil · Change-ID button in `/cb editor` | §1 |
| ✅ Passed 2026-06-13 | `/cb reid <id> <newId>` command — id change · state migration · undo | §2 |
| ⏳ Coming | swapid · swapname · duplicate alias · custom drops · Block Finder · HD PNG export · settabicon | §3 |

---

# 🎯 Test now

## §1 · Reid GUI

> 💡 **What it does:** change a block's id from a menu instead of typing both ids — pick a block,
> type the new id in an anvil. Every path runs the already-passed `/cb reid` command underneath.

> 🧰 **Before you start:**
> - `/cb create demo Demo`
> - `/cb create demo2 Demo2`
> - `/cb lock demo2`   *(so you can test the locked-block refusal)*

**Try these:**

- **① No-arg picker** 🔴
  `/cb reid`
  - ✅ **Pass:** a "Re-ID - pick a block" chest opens → click a block → anvil pre-filled with its id → take a new id → `Changed id <old> to <new>.`, and `/cb list` shows it with the texture intact.
  - ❌ **Broken if:** the texture changes, or the slot number moves.

- **② Single-arg → straight to anvil** 🔴
  `/cb reid demo`
  - ✅ **Pass:** the anvil opens for `demo`; take a new id → same confirmation.
  - 🔎 Also: `/cb reid typo` → error, **no anvil** · `/cb reid demo2` (locked) → refused, **no anvil**.

- **③ Change ID from the editor** 🔴
  `/cb editor demo`
  - ✅ **Pass:** click **Change ID** (anvil icon, left of Rename) → the same anvil. **Esc** → back to the editor, nothing changed.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | No-arg → picker → anvil re-ids, keeps texture & slot |
| 🟥 | ② | Single-arg opens the anvil; typo + locked refuse up front |
| 🟥 | ③ | The editor's Change-ID button reaches the same flow |
| — | **0 / 3** | |

> ↩️ **Undo the test:** `/cb undo` reverses any re-id.

---

# ✅ Passed — kept for re-test reference

## ✅ §2 · `/cb reid <id> <newId>` command — 8/8 passed 2026-06-13

> Confirmed in-game by the developer 2026-06-13. Kept so you can re-check after future changes.

**The run:**
- 🧱 make it: `/cb create reidtest ReidTest`
- 🔗 attach state: `/cb fav reidtest` · `/cb lock reidtest` · `/cb note reidtest hello`
- 🔒 locked guard: `/cb lock reidtest` → `/cb reid reidtest x` → refused ("unlock first")
- ✏️ rename: `/cb unlock reidtest` → `/cb reid reidtest newname` → "Changed id reidtest to newname"
- 👀 check: `/cb list` → `newname`, same slot #, texture intact
- 🪢 state follows: `/cb favs` · `/cb locked` · `/cb note newname` all point at `newname`
- ↩️ undo/redo: `/cb undo` → `reidtest` · `/cb redo` → `newname`
- 🚫 conflict: `/cb reid newname existingId` → "already taken"

✅ **Proves:** the id change keeps the slot + texture, migrates locks / favorites / notes / drafts, and is undoable.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟩 | 1 | locked guard refuses |
| 🟩 | 2 | rename changes the id |
| 🟩 | 3 | slot # and texture intact |
| 🟩 | 4 | favorites follow the new id |
| 🟩 | 5 | locks follow the new id |
| 🟩 | 6 | notes follow the new id |
| 🟩 | 7 | undo/redo work |
| 🟩 | 8 | conflict blocked |
| — | **8 / 8** | |

---

# ⏳ Not built

| Feature | What it'll do |
|---|---|
| `/cb swapid <a> <b>` | swap two blocks' ids |
| `/cb swapname <a> <b>` | swap two blocks' display names |
| duplicate alias | a friendlier alias for `/cb dupe` |
| custom drops | what a block drops when broken |
| Block Finder GUI | locate placed blocks in the world |
| HD PNG export from editor | one-click full-res texture export |
| `/cb settabicon` | set the creative-tab icon |

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs what you expected (📸 a screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete demo` · `/cb delete demo2` · `/cb delete newname`
