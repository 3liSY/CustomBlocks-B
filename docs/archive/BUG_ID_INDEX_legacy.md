# Bug ID Index

> Cross-group lookup for every Active Bugs row currently open. Bug ID = the test row it lives on
> (per `TESTING_GUIDE_TEMPLATE.md`) — since that's just a row ref (A1, B7, N2...), the **same ID
> can appear in multiple groups**. This file is the one place to disambiguate "which group is B3
> actually in" and to check for an in-file collision before adding a new bug row.
>
> One entry per session as bugs are added/cleared — keep in sync with each group's own
> `# 🐛 Active Bugs` table (this file doesn't replace those, it's a lookup index only).

| Bug ID | Group | Test Row | Short description | Status |
| ------ | ----- | -------- | ------------------ | ------ |
| G03-BULKLOCK-HUD | [G03](GROUP_03_TESTING_GUIDE.md) | A3 | `/cb bulklock` never calls `WidgetSync.pushAll`, so a bulk lock's padlocks don't show until something else pushes (single `/cb lock` does). Surfaced 2026-07-15 building G04 §G | ⚠️ open — 1 line |
| G07-HUB-FILTER | [G07](Group_07_Testing_Guide.md) | — | Hub Console + NL bar still build the dead `category:`/`id:`/… filter words. Chat half confirmed fixed MP 2026-07-15; **Hub half is all that's left** | ❗ open |
| G32-ART-UNWRAP | [G32](Group_32_Testing_Guide.md) | A1 | The tomato ships as a camera-facing **billboard** because both approved PNGs are flat front-view illustrations, not UV unwraps — a 3D model has no texture to wear. Needs art (unwrapped side/back/top), not code | ❔ open — art task |
| A3-screen | [G04](Group_04_Testing_Guide.md) | A3 | The ✎ Edit chip has no screen to open; both candidates rejected. A screen to design, not a bug to fix | ❔ open |
| B4 | [G04](Group_04_Testing_Guide.md) | B4 | `⇪ Copy` fires no sound cue — `COPY_TO_CLIPBOARD` runs no command, so there is no server hook. Vanilla wall | 🧊 wontfix |
| B5 | [G04](Group_04_Testing_Guide.md) | B5 | Rich 3D hover card not built — needs the tooltip-draw mixin (owner pre-approved, cut for scope) | ⏳ deferred |
| B3 | [G05](GROUP_05_TESTING_GUIDE.md) | B3 | SP-only fast create/retexture burst → Corrupt PNG | 🔴 open — known bug |
| B1–B4 | [G12](GROUP_12_TESTING_GUIDE.md) | B1–B4 | Download links leak server host (`httpHost` public) | Open |
| N1 | [G13](GROUP_13_TESTING_GUIDE.md) | N1 | CP1 fixed partition capped `/cb create` at index 800 | Reverted, awaiting in-game confirm |
| N2 | [G13](GROUP_13_TESTING_GUIDE.md) | N2 | CP1 re-classed normal blocks ≥800 as ArabicSlotBlock | Reverted, awaiting in-game confirm |
| T9 | [G14](GROUP_14_TESTING_GUIDE.md) | T9 | Hostile-CDN 403 retry built, never confirmed in-game | 🔴 open — awaiting test |
| T7 | [G14](GROUP_14_TESTING_GUIDE.md) | T7 | Giphy links don't resolve (fix approved, build deferred) | 📝 fix approved · build deferred |
| C1/C4 | [G27](GROUP_27_TESTING_GUIDE.md) | C1/C4 | Multi-box shapes render disjointed in studio 3D cube | Open |
| C1 | [G27](GROUP_27_TESTING_GUIDE.md) | C1 | Link line under block not clickable / hard to see | Open |
| C3 | [G27](GROUP_27_TESTING_GUIDE.md) | C3 | Block-picker chest GUI needs full revamp + search | Open |
| K1 | [G27](GROUP_27_TESTING_GUIDE.md) | K1 | (see GROUP_27_TESTING_GUIDE.md Active Bugs table) | Open |

**Collision note:** `B3` now appears in **G05** (corrupt PNG) *and* as a row ref in other files, and `B4`/`B5`
are G04 rows — all fine, IDs are scoped per file. The cross-group bugs added on 2026-07-15 deliberately use a
**`G##-NAME`** form (`G04-UNDO-DIALECT`, `G03-TOOLCHIP`, `G32-TEXTURE`…) rather than a bare row ref, because
each spans several files and a bare letter+number could not say which. Only suffix a/b when two bugs land on
the **same row in the same file**.

**Closed 2026-07-15:** `C1` (G04 — incidents chest GUI dumps every incident + spams chat). Not fixed — *moved*
whole to **Group 27 §R / §G27.31** as the Diagnostics Screen. It was never a chat bug.

**Fixed 2026-07-15 (build slice 2 — awaiting in-game confirm, G03 §A):** `G03-TOOLCHIP`. The ACTIVE_TOOL widget
is deleted outright (enum entry, `drawActiveTool`, `WidgetSync`'s `tool` property, `WidgetSignals.toolMode` and
its comment, which claimed the server sent `""` when the tool wasn't held — it never did). The padlock is now a
drawn 7×9 sprite, 10px below the crosshair, instead of the 🔒 font glyph.

**Fixed 2026-07-15 (build slice 3 — awaiting in-game confirm, G32 §A/§B):** `G32-DISMOUNT` · `G32-TEXTURE`. The
per-tick sneak force-clear is deleted, so vanilla sneak-dismount just works. Both PNGs are installed (128 item,
256 entity) and the entity is drawn by a new billboard `TomatoEntityRenderer` instead of the item-model renderer.
The core blast is in, hand-rolled (vanilla can't do radius 3 + 8 damage + a survivor from one `power`).

**Fixed 2026-07-15 (build slice 1 — awaiting in-game confirm, G04 §G):** `G04-UNDO-DIALECT` · `G04-VIEW` ·
`G04-COUNT` · `G07-BULK-UNDO`. One pass: `UndoManager.Kind.FLAG` makes lock/favorite undoable (they recorded
nothing before), every `▶ [↩ Undo]` / `▶ [undo]` chip in the mod became `Chat.undoButton()`, the redo line
carries ↩ Undo, `(N left)` became `· N more to undo` (silent at zero), and `Chat.viewButton` + both call sites
are deleted. The `▶ [↩ Undo]` double-glyph turned out to be **9 call sites across 6 files**, not the 2 first
found — all nine fixed together.
