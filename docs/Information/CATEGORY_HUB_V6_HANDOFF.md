# Category Hub v6 — Implementation Handoff (fresh chat)

> Paste the block below into a new chat to start implementation. Design is LOCKED; the op-audit and every
> ambiguous decision are ALSO LOCKED (2026-07-05) — do not re-litigate or re-ask any of it.

---

## TASK
Rebuild the **Category Hub** screen (`/cb category`) on the locked **v6 design**. This is CustomBlocks-B,
a Fabric mod for Minecraft 1.21.1. The design was locked with the owner on 2026-07-05 after a mockup +
multi-round Q&A. The op-audit and every open design gap were also resolved with the owner on 2026-07-05
(see "DECISIONS LOCKED" below). Your job is to implement — not redesign, not re-ask.

## READ FIRST (in this order)
1. `CLAUDE.md` — operating protocol. The owner is **not a coder**; explain in plain terms, one step at a
   time, and **nothing is "done" until he confirms it in-game** (Golden Rule §2).
2. `docs/groups/GROUP_27_SCREENS.md` → §G27-3, the **"🎨 v6 FULL REDESIGN"** subsection — design contract +
   the "Op audit — DONE" and "Decisions locked with owner 2026-07-05" blocks right under it.
3. `docs/mockups/category_hub_rework_v5.html` — the blessed visual ("Red Ops"). Open it; the ⧉ Zones
   button labels every region. This is the look to match (portable subset only — see below).
4. `docs/testing/TESTING_GUIDE_27.md` → **§L-v6 rows L13–L25** — test checklist, already updated with
   every locked decision baked into the expected results.
5. Current code: `src/main/java/com/customblocks/client/gui/CategoryHubScreen.java` (493/500 lines),
   `CategoryHubModel.java`, `CategoryHubDragDrop.java`.

## LOCKED DESIGN (summary — full detail in §G27-3)
- **Palette:** `#FF0000` red (selected/active) · `#000000` black (bg) · `#40FF00` lime (success only) ·
  `#39E0C8` cyan (live values). **No gold.** Web-only mockup sugar (blur box-shadows, scanline film, CSS
  transitions) does NOT ship — port the rectangles/gradients/borders/spines/pills/text+shadow only.
- **Opens on a HUB OVERVIEW** — nothing selected; right pane = stats dashboard (X categories · Y blocks ·
  default is Z · N loose) + quick New-category.
- **Left rail:** searchable category list; `(uncategorized)` **pinned at TOP as an inbox** (greyed), only
  when it has blocks; New-category input at bottom.
- **Selected category → right pane:** hero header (big colored name + count + ★ badge + underline) · ONE
  toolbar row · BLOCKS list · action bar.
- **Blocks list:** small **thumbnail** + **name only**; a tiny per-row button toggles the internal id
  (**ephemeral**, resets on scroll/reopen); tiny **🔍 → preview popover** beside the row (must animate GIFs
  = F3 fix, **multiple popovers can stay open at once**); **checkboxes** for multi-select.
- **Per-block actions (click a block):** Rename · Delete (confirm, mentions Trash/`/cb undo`) ·
  Lock/Unlock · Duplicate. All four confirmed backend-supported (see op audit below) — just need wiring.
- **Multi-select bulk actions:** Move selected · Lock/Unlock selected · Delete selected — build **all
  three**, not move-only.
- **Move model (REBUILD — current one is broken):** pick→**"Move to…"** popup = reliable primary; tick
  several → **"Move selected to…"**; **drag** onto a left category = bonus. **"Move all blocks to…"**
  empties into a target and **KEEPS** the source (now empty) — deletion is a **separate** explicit action
  (this fixes the old merge confusion). **"Empty to loose"** = one-tap dump to the inbox. A **locked**
  block can still be moved between categories — lock only blocks content edits, never category moves.
- **Category tools:** always Rename (inline) · Colour (16 §-swatches + #hex) · Set default (★ toggle) ·
  Delete (**confirm popup**, blocks kept → inbox). More menu: Lock all · Unlock all. Power tools:
  **Category icon** (a block as its face — plain swatch until picked, no silent auto-pick) ·
  **Hide from browser** (hides only from the Studio's `StudioBlockSelector` picker; hidden category still
  always shows in this hub) · **Duplicate category** (name+settings only, 0 blocks, `_copy`/`_copy2`…
  suffix). (Manual "reorder categories" was declined — do NOT build.)
- **Bulk tools inside a category:** Bulk rename (prefix / find-replace — renames BOTH display name AND
  internal id together, one combined op) · Sort (A–Z / Z–A / colour / **newest**, backed by a new real
  `createdAt` field on `SlotData`) · Quick "Empty to loose" · Select-all.
- **Search:** two labelled searches — categories (top) + blocks-in-category (above BLOCKS).
- **Sound (subtle):** soft UI click on buttons + lime success chime on move/rename/delete-done. Vanilla
  `SoundEvents`, bare `SoundEvent` (only `BLOCK_NOTE_BLOCK_*` needs `.value()`).
- **Undo/Redo:** bulk ops record as one batch into the existing `UndoManager` (same pattern as
  `BulkDuplicateCommands`). Add an in-hub **Undo** button (fires `/cb undo`) + a new **Redo** on top
  (redo doesn't exist yet anywhere — this is new, small, additive).

## OP AUDIT — ALREADY DONE (2026-07-05), do not repeat
Current `CategoryAdminPayload` ops: `create·rename·delete·color·colorhex·default·desc·sort·merge·lock·
unlock·assign`. Every new op v6 needs already has a working engine underneath — only wiring is left:
- `assignMany`/`moveAllKeep`/`emptyToLoose` — new thin bridge cases, loop existing `assign`/
  `SlotManager.setCategory`. `moveAllKeep` = `merge` minus the `CategoryMetadataStore.deleteCategory` call.
- `icon`/`hide`/`duplicate` (category-level) — **no backend yet**, need new `CategoryMetadataStore` fields
  (icon block id, hidden bool) + a copy-settings method.
- `blockRename` → `SlotManager.rename` (exists, same as `/cb reid`).
- `blockDelete` → `DeletionService.delete(server, SlotData, actorUuid)` (exists — snapshots to Trash,
  records Undo; needs actor UUID + `MinecraftServer` threaded from the bridge call).
- `blockDuplicate` → `SlotManager.dupe` (exists, same as `BulkDuplicateCommands`, `_copy`/`_copy2`… suffix).
- `blockLock`/`blockUnlock` → `LockManager.lock`/`unlock` (exists), single-id call.
- `bulkRename` → loops `SlotManager.rename` + every store's `renameId()` (favorites, notes, tolerance,
  locks, drafts, category metadata, display-block manager) per block — same plumbing `/cb reid` already
  exercises safely, just called in a loop with one batched Undo entry.

## FIRST STEP — DO NOT SKIP
**Split first (500-line gate).** `CategoryHubScreen.java` is 493/500 — v6 will not fit. Extract rendering
into a new `CategoryHubView.java` (keep `CategoryHubModel` for data, `CategoryHubDragDrop` for drag; reuse
`CbPopupPicker` for move/bulk pickers, `LocalTexturePreview` for thumbnails/preview). Do this as a
**behavior-identical, build-green** first step and hand back — tell the owner it looks unchanged on
purpose (it's plumbing). Get his in-game "looks the same" confirmation before building any v6 feature.

## THEN BUILD, ZONE BY ZONE (hand back for in-game test after each visible chunk)
Suggested order: overview landing → left rail + pinned inbox → hero header + toolbar → blocks list
(thumbnails + id-toggle + preview popover) → move model (pick/bulk/drag/move-all-keep/empty-to-loose,
incl. bulk lock/unlock/delete) → delete confirm popup → category power tools (icon/hide/duplicate) →
bulk tools (sort incl. newest, bulk rename) → per-block actions (rename/delete/lock/duplicate) → Undo/Redo
button → sound pass.

## BUILD ENV
JDK 21 only (`JAVA_HOME` → 21, `--no-daemon`; machine default Java is different). Gates: `mojibakeShield`,
`soundGate`, `monolithGate`. Build: `./gradlew.bat build`. A green build = compiles + gates pass, NOT
"done" — only the owner's in-game confirmation is done. Keep `docs/testing/TESTING_GUIDE_27.md`
§L-v6 in sync as you go. Branch before committing; never commit unless asked.

## FIRST MESSAGE TO SEND THE OWNER
Confirm the plan in one line, then go straight to the split (no re-audit, no re-asking locked decisions —
they're all above and in `GROUP_27_SCREENS.md` §G27-3). Hand back build-green + "looks unchanged" for
in-game confirm before touching any feature code.
