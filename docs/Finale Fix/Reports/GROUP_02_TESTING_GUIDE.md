# 🧪 Group 02 — Chest GUI — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| 🎯 **TEST NOW** | The whole chest-GUI system — dashboard · editor · undo/redo · history · RP controls (12 review fixes in, **none confirmed yet**) | §1 |
| 🟡 Polish later | 3rd polish pass · shape-editor wiring · history "filter by player" | §2 |
| ⏳ Coming | categories / macros / arabic / template GUIs (backends not in B yet) | §3 |

> ⚙️ Build first (`gradlew build`) — compile errors are possible until that first build passes.

---

# 🎯 Test now

## §1 · Chest GUI

> 💡 **What it does:** the full menu system — open a dashboard, edit a block, browse undo/redo &
> history, and control the resource pack, all from chests.

> 🧰 **Before you start:**
> - `/cb create g02a ChestGUITest`
> - `/cb create g02b EditorTest`

**Try these:**

- **① Dashboard** — `/cb` (alone)
  - ✅ **Pass:** opens a **6-row chest** "CustomBlocks Dashboard" (framed, Nether-star header). Aliases `menu` / `dashboard` / `gui` / `admingui` open the same.
  - ❌ **Broken if:** nothing opens, a full-screen menu opens, or it errors.

- **② Editor** — `/cb editor g02a`
  - ✅ **Pass:** chest "CustomBlocks Editor - ID: g02a"; every action slot (glow / hardness / sound / collision / category / give / **Change ID** / rename / retexture / note / delete) gives the same chat feedback as its `/cb` command; the editor stays open / reopens.
  - 🔎 Note: `/cb gui block <id>` was **removed** → should say "unknown command" (intended).

- **③ No cursor jump** — click around inside any menu
  - ✅ **Pass:** the mouse does **not** recentre. *(This was the big bug.)*

- **④ Back-stack** — `/cb` → Block List → a block → its editor → **Back**
  - ✅ **Pass:** walks back level by level (not closing, not jumping).

- **⑤ Undo / Redo / History**
  `/cb setglow g02a 8` → `/cb setglow g02a 4` → `/cb undogui`
  - ✅ **Pass:** entries with readable lore ("Changed glow - g02a"); click one to undo. `/cb redogui` re-applies. `/cb history` → mutation log (time / player / action / id), entries clickable → open that block's editor.

- **⑥ Resource-pack controls**
  `/cb rp pause` → `/cb create g02c PauseTest` (no rebuild while paused) → `/cb rp resume` (rebuilds once) → `/cb sync` → `/cb unsuppress`
  - ✅ **Pass:** each behaves as labelled — pause holds the rebuild, resume rebuilds exactly once, sync force-pushes, unsuppress re-enables the pack-download prompt.

- **⑦ `/cb listgui`**
  - ✅ **Pass:** the upgraded Block List opens — search · tick-many select · bulk-on-selection (full tests live in **Group 07**).

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | `/cb` opens the 6-row dashboard (+ aliases) |
| ⬜ | ② | `/cb editor g02a` opens the titled editor; every action slot works |
| ⬜ | ③ | clicking inside a menu does **not** recentre the cursor |
| ⬜ | ④ | Back walks the stack correctly |
| ⬜ | ⑤ | undo/redo browsers + clickable history work |
| ⬜ | ⑥ | rp pause/resume/sync/unsuppress behave |
| ⬜ | ⑦ | `/cb listgui` opens the upgraded Block List |

> ↩️ **Undo the test:** `/cb delete g02a` · `/cb delete g02b` · `/cb delete g02c`

---

# 🟡 §2 · Polishing later  *(known, not bugs)*

- 🎨 **3rd GUI pass** — refine layout / icons / lore wording; "partially complete" by design.
- 🔺 **Shape editor** — wiring into `/cb editor` intentionally deferred.
- 👤 **History filter** — shift-click "filter by player" not implemented.
- 🧹 Dead-code warning only: old `GuiCommands.openGui` unused.

---

# ⏳ §3 · Coming next  *(not built — nothing to test)*

| Feature | Why it's waiting |
|---|---|
| Categories / Macro-list / Arabic / Template GUIs | rebuild once their backends exist in CustomBlocks-B |
| Search-picker · snapshots · recover · colours · AI · achievements GUIs | not ported yet (see note) |

> 📚 **Note:** the old build had 81 `open*` workflows (37 command entry points). Today's chest GUIs:
> Dashboard · Block List · Editor · Re-ID · Undo · Redo · History · Magic Items · Magic-edit ·
> Bulk Hub/Dashboard · Config · Help · Diagnostics · Texture-Size · Color Studio.

---

## 🆘 If a test fails, send me
- 🔢 the step number
- 👀 what happened (chest? screen? error? nothing? — 📸 screenshot helps)
- 📄 the last ~20 lines of `logs/latest.log`

## 🧹 Cleanup
`/cb delete g02a` · `/cb delete g02b` · `/cb delete g02c`
