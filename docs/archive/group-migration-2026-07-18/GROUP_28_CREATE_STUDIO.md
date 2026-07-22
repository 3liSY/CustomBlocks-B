# Group 28 — Undo System Rework

> **Repurposed 2026-06-25.** Previously a redirect to Group 27 (Block Creation Studio).
> This doc now owns the full undo-system rework spec — G28-1 folded here by owner decision.
>
> **UI medium audit cross-check (2026-07-09):** the `UndoHistoryScreen` decision below (3 tabs, replaces
> UndoMenu + HistoryMenu) predates the whole-mod medium audit but IS the medium decision for those two
> menus — logged into `docs/UI_MEDIUM_GUIDE.md`'s triage table so it isn't missed again.
## G28-1 · Undo + undo GUI barely works for bulk — full rework wanted

> 💬 discuss — do after C4+C5; every new op from those clusters must register undo steps before the GUI rework ships

> *"undo and undogui have tiny problems with bulk actions and their lore in the gui and more"* …
> *"need a way to click a specific thing to show all the blocks that got deleted … looking to ENTIRELY
> REWORK it … it is really bad rn it barely functions"*

**Symptom** — The `/cb undo` command path actually reads fine. The **visual undo/redo menu** is what's
broken, especially for **bulk** actions: rows show `?`, labels are wrong, no block count, and there's
no way to see what a bulk step actually contained. Developer wants a **full rework**, not a patch.

### Confirmed bugs (verified in code — these are real, not cosmetic guesses)

| # | Bug | Where |
|---|---|---|
| 1 | A **bulk (batch) row shows `?`** as its block — `blockId()` returns `"?"` because a BATCH op has `before == null` AND `after == null` (the children hold the data). | `UndoMenu.blockId` |
| 2 | The **"friendly name" map is dead code** — it switches on `setglow`/`sethardness`/`setsound`/`setcollision`/`setcategory`, but the labels actually recorded are `glow`/`hardness`/`sound`/`collision`/`category`. Those cases **never fire** → every one falls to "just capitalize the raw word." The nice "Changed glow" text never shows. | `UndoMenu.friendlyAction` |
| 3 | **Same broken map is copy-pasted** into the audit-log menu — so it's wrong in two places and will keep drifting. | `HistoryMenu.friendlyAction` |
| 4 | The **audit log shows a batch as `×5`** in the Block field (the audit id for a batch is `"×N"`), so it reads "Block: ×5 — block no longer exists." | `HistoryMenu` ← `UndoManager.push` mlId |
| 5 | **Lore undersells bulk** — "undo the last N changes" / "Position #N from the top" counts a whole bulk as ONE change, so a 12-block bulk-delete looks like "1 change." The command path (`HistoryCommands.describe`) gets this right ("(12 blocks)"); the GUI reimplements it wrong. | `UndoMenu` lore |
| 6 | **Redo side has all the same bugs** (same builder, `redo == true`). | `UndoMenu` |

**Root cause of 1–6:** the description logic is **written three times** (`HistoryCommands.describe`,
`UndoMenu`, `HistoryMenu`) and the two GUI copies are the buggy ones. One shared helper kills the
whole class at once.

**Does clicking a bulk row actually WORK (functionally)?** Code path looks correct — `applyInverse`
handles `BATCH` by reverting every child, and clicking row N just calls `undoOnce` N times (a batch =
one step). So it *should* revert correctly; **needs in-game confirm** (developer unsure). Treat as
"looks broken, likely functions" until verified — not a confirmed functional bug yet.

### What the developer wants (locked via UI 2026-06-23)

- **ENTIRE rework**, marked **discuss-later** — "barely functions" today.
- **Drill-in:** click a bulk entry to **expand and see every block it touched** (e.g. all blocks a bulk-delete removed), not just a count.
- Fix the obvious bugs above as part of it: real labels, real **block counts**, no `?` / `×5`, redo too.

### Brainstorm (rework direction)

1. **One shared describe/label helper** (`UndoDescribe`) used by the command report **and** the screen —
   single source of truth, batch-aware, real labels, real counts. This alone erases bugs 1–6.
2. **Honest "what will revert" preview** — hover/lore lists the blocks (or first few + "…and N more") so a
   click is never a surprise; confirm for large bulk reverts.
3. **Redo mirrors undo** from the same component — fixed once, correct on both sides.

### Decisions — all locked (2026-06-25)

- **Bulk undo semantics: all-or-nothing OR one-specific-child.** A bulk step can be reverted as a whole (all N blocks, recommended, no stack risk), OR the developer can pick ONE specific child entry to remove from the batch (the batch shrinks from N → N-1; remaining children stay as-is on the redo stack). No arbitrary partial selection — that would corrupt the redo stack.
- **UI: moved to G27** — full screen spec (`UndoHistoryScreen`, 3 tabs, drill-in, confirm dialog) now lives
  in `GROUP_27_SCREENS.md` §G27.34 (2026-07-12).
- **Persistence: survive restart, capped rolling buffer.** History saved to disk; survives server restart and `/cb reload`. To prevent unbounded pile-up: a **configurable max-steps cap** (default 200 steps per player); when the cap is hit, the oldest entry is trimmed before adding the new one (FIFO rotation). Disk format = compact JSON per-player file; atomic write (temp + rename per standard practice).

### Gap found 2026-07-16 (owner, TG6 §A) — face-paint ops have NO undo/redo at all

> *`/cb paintface foo up <url>` then `/cb undo` → "Nothing to undo."* Owner: wants **all** face-texture-changing
> commands to support undo/redo, this is a regression/gap, not cosmetic.

`/cb paintface` and `/cb clearface` never push an undo step — confirmed via in-game repro (screenshot),
`/cb undo` reports nothing to revert right after a paintface. These commands don't participate in the
`UndoManager.push` pipeline at all yet. **Owner decision:** fold this into G28-1 rather than a standalone
fix — same shared undo engine every other block-mutating command already uses, no separate mechanism.
**Fix direction:** wire `FaceCommands` (paintface/clearface, and any future per-face texture op incl. the
future G06-6 Face-mode transforms) to push a normal undo entry (before/after per-face texture state),
same shape as existing attribute-setter undo steps. Do before/alongside the G28-1 GUI rework so nothing
new ships un-undoable.

| Group | Scope | Status |
|---|---|---|
| G28 — Undo system | Both — server-side undo stack | 🔍 diagnosed 2026-07-16 — not built |

**Related:** TG6 §A (per-face paint, where the gap was found) · G06-6 (Face mode — must also register undo, already locked in its own spec)
**Touches:** `FaceCommands` (paintface/clearface — push undo entry) · `UndoManager` (accept per-face before/after state)

### Architecture additions (extend brainstorm above)

4. **Persistent storage** — new `UndoStore` (one JSON file per player UUID, atomic write). On `UndoManager.push`: append + trim if over cap. On disconnect / reload: write to disk. On login: load from disk.

| Group | Scope | Status |
|---|---|---|
| G28 — Undo System Rework | Both — server state + new G27 full screen | 🔍 designed — all decisions locked 2026-06-25; bugs confirmed in code; ready to build |

**Related:** `/cb undo` command path (the correct reference for labels/counts) · MutationLog audit (shares the buggy map) · G06-2/G06-3 (delete/recycle correctness underneath undo) · G27 screen system (the full-screen browser uses it)
**Touches:** new `UndoDescribe` helper · new `UndoHistoryScreen` (3-tab: Undo/Redo/Audit — replaces `UndoMenu` + `HistoryMenu`) · new `UndoStore` (per-player persistent JSON + FIFO cap) · `HistoryCommands.describe` (route through `UndoDescribe`) · `UndoManager.Op` (children carry detail — already exists) · confirm dialog modal · `CustomBlocksConfig` (+ `undoMaxSteps` + `undoConfirmThreshold` config keys)
**Verify in-game:** (1) undo/redo stack shows correct labels + icons; (2) clicking bulk row drills in to children list; (3) "Undo whole step" reverts all N correctly; (4) "Remove one entry" reverts one and shrinks batch to N-1; (5) confirm dialog appears for large reverts + cancels correctly; (6) history survives server restart; (7) history caps at configured max and oldest trims cleanly.
