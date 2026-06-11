# Group 02 — Chest GUI · Testing Guide

*One green build proves it compiles — nothing is done until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ confirmed  ·  🟡 polish later  ·  ⏳ not built

---

## Where Group 02 stands

> 🎯 **Needs your test** — the whole chest-GUI system (dashboard, editor, undo/redo, history, RP
> controls). Built + green, 12 review fixes in, **none confirmed in-game yet** → §1
> 🟡 **Polish later** — GUI is "partially complete": a 3rd polish pass + missing-backend GUIs remain
> ⏳ **Coming** — categories / macros / arabic / template / search-picker GUIs (backends not in B yet) → §4

Build first (`gradlew build`); compile errors are still possible until that first build passes.

---

## 🎯 §1 · Chest GUI — test this

**Setup**
```
/cb create g02a ChestGUITest
/cb create g02b EditorTest
```

**1 — Dashboard** — `/cb` (alone) → opens a **6-row chest** titled "CustomBlocks Dashboard" (framed,
Nether-star header). *Broken if:* nothing opens, a full-screen menu opens, or it errors.
Aliases `menu` / `dashboard` / `gui` / `admingui` open the same.

**2 — Editor** — `/cb editor g02a` → chest titled **"CustomBlocks Editor - ID: g02a"**. Click each
action slot (glow / hardness / sound / collision / category / give / rename / retexture / note /
delete) → same chat feedback as the matching `/cb` command; editor stays open / reopens.
*Note:* `/cb gui block <id>` was **removed** — it should say "unknown command" (intended).

**3 — No cursor jump** — click around inside any menu → the mouse must **not** recentre. *(Was the big bug.)*

**4 — Back-stack** — `/cb` → Block List → a block → its editor → **Back** walks back level by level
(not closing, not jumping).

**5 — Undo / Redo / History**
```
/cb setglow g02a 8
/cb setglow g02a 4
/cb undogui
```
→ entries with readable lore (e.g. "Changed glow - g02a"); click one to undo. `/cb redogui` re-applies.
`/cb history` → mutation log (time / player / action / id), entries clickable → open that block's editor.

**6 — Resource-pack controls**
```
/cb rp pause
/cb create g02c PauseTest      → created, but NO pack rebuild while paused
/cb rp resume                  → rebuilds exactly once
/cb sync                       → "Force-syncing … to N client(s)"
/cb unsuppress                 → re-enables the server pack download prompt
```

**7 — `/cb listgui`** → chat list of GUIs, each with a clickable green `[open]`.

| ✓ | Step |
|---|---|
| ⬜ | 1 — `/cb` opens the 6-row dashboard (+ aliases) |
| ⬜ | 2 — `/cb editor g02a` opens the titled editor; every action slot works |
| ⬜ | 3 — clicking inside a menu does **not** recentre the cursor |
| ⬜ | 4 — Back walks the stack correctly |
| ⬜ | 5 — undo/redo browsers + clickable history work |
| ⬜ | 6 — rp pause/resume/sync/unsuppress behave |
| ⬜ | 7 — `/cb listgui` shows clickable `[open]` buttons |

---

## ☑️ §2 · Already working — re-test only if something feels off
*(None confirmed in-game yet — once you verify the §1 rows, move them here.)*

---

## 🟡 §3 · Polishing later (known, not bugs)
- **3rd GUI pass** — refine layout / icons / lore wording; "partially complete" by design.
- **Shape editor** — wiring into `/cb editor` intentionally deferred.
- **History filter** — shift-click "filter by player" not implemented.
- Dead-code warning only: old `GuiCommands.openGui` unused.

---

## ⏳ §4 · Coming next (not built — nothing to test)
| Feature | Why waiting |
|---|---|
| Categories / Macro-list / Arabic / Template GUIs | rebuild once their backends exist in CustomBlocks-B |
| Search-picker, snapshots, recover, bulk-ops, colours, AI, achievements GUIs | not ported (see Appendix) |

> **Appendix — old wired-GUI inventory:** the old build had 81 `open*` workflows (37 command entry
> points). Master audit list lives in the project history / old `GuiManager.java`; new build has 8
> chest GUIs today (Dashboard, Block List, Editor, Undo, Redo, History, Magic Items, Magic-edit).

---

## If a test fails
Send: the step number, what happened vs expected (chest? screen? error? nothing? — screenshot helps),
and the last ~20 lines of `logs/latest.log`.

## Cleanup
```
/cb delete g02a
/cb delete g02b
/cb delete g02c
```
