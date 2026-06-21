# Changelog

Newest first. "Built" means code-complete and gates-green; ✋ items still need in-game confirmation
(nothing is DONE until the owner confirms in-game).

## [Unreleased]

### Group 16 — finish pass: bulk/pack FX, Debug Log viewer (2026-06-21) — 🟢 built, ✋ pending in-game test
- **Bulk operations now play their own "complete" cue.** Finishing a bulk edit / delete / rename plays
  the `bulk_complete` Feedback FX (totem ring + beacon hum) instead of nothing.
- **Manual pack reloads play a pack cue.** `/cb sync` and `/cb rp resume` now play the `rp_regenerate`
  Feedback FX (portal swirl + soft chime) rather than the generic success cue. Ordinary edits still
  rebuild silently; `/cb reload` (data reload) is unchanged.
- **New: Debug Log viewer.** IT Chest (`/cb diag`) → Row 6 → **Debug Log** opens an in-game, read-only,
  paged view of this session's `[CustomBlocks]` log lines (newest first, coloured by severity) — no
  more alt-tabbing to `logs/latest.log`.
- **`achievement` Feedback FX** stays preview-only until a future achievement system can trigger it.
- New files: `core/FeedbackFx.java`, `core/DebugLog.java`, `gui/chest/DebugLogMenu.java`. Edited:
  `Chat`, `BulkCommands`, `ChestGuiCommands`, `ItChestMenu`, `Nav`, `GuiRouter`.

### Group 16 — Feedback FX board centred (2026-06-21) — 🟢 built, ✋ pending in-game test
- **Feedback FX chest tiles now sit in the middle of the chest** instead of hugging the top row.
  Same 6-row board, same controls — just a more balanced layout (owner feedback: felt too top-heavy).
- Edited: `gui/chest/FeedbackMenu.java` (master row 1 → row 2).

### Group 06 — instant colour-Square swaps (2026-06-20) — 🟢 built, ✋ pending in-game test
- **Colour Squares now swap instantly.** Right-clicking a placed block with a colour Square (the fixed
  red/yellow/green/black Squares and the custom-hex Square) changes it with no visible delay. The swap was
  always instant on the server; the client now paints the result the same tick instead of waiting for the
  server's block-update packet to come back.
- Misses are unchanged: swapping to a variant that doesn't exist still shows "create it with the Triangle
  first" (no flicker), and a Black Square with no `_black` variant still falls back to the base block.
- New: `client/ClientSwapPredictor.java`, `item/ColorSwapTool.java`. Edited: `ShapeToolItem`,
  `CustomColorToolItem`, `ClientSlotCache`, `CustomBlocksClient`. See ADR-009.

### Group 14 — animation polish + render deferral (2026-06-20) — 🟢 built, ✋ pending in-game test
- **`/cb anim <id>` on a non-animated block** now opens the studio straight on the **Texture** tab (was the
  Identity tab) so you can load a GIF/WebP right away; the chat note is reworded to match.
- **Known / deferred:** GIF/WebP **muffle** (nyan-style speckle) and **earth** crispness + smoothness are
  **deferred to the Phase 1b own-texture renderer** — the vanilla block atlas applies mipmaps to every sprite, so
  no atlas-side setting can make a detailed/text image fully crisp. Tracked in the Group 14 testing guide.
- **Still open (small):** loading a new GIF in the studio resets its speed/loop; an animated block's background
  can't be re-filled without a new image.

### Group 13 / Build B — delete static letters + reclaim slots (2026-06-19) — 🟢 built, ✋ pending in-game test
- **Removed:** the 144 OLD static Arabic letter blocks (36 letters × 4 colours) are permanently deleted on boot
  (one-time, idempotent migration). They no longer appear in creative search or `/cb arabic list`.
- **World cleanup:** any placed copy of an old static letter is replaced with air as its chunk loads (runs across
  restarts until done).
- **Reclaimed:** the ~144 freed slots return to the pool; freed indices are reused only as a last resort and are
  dropped from the retired set on reuse, so a reused slot can never show or delete a wrong block.
- **Slimmer jar:** the 144 bundled letter PNGs were stripped from `assets/customblocks/arabic_art/` (the 80 number
  PNGs are kept).
- **Untouched:** numbers (A0–A9 + E0–E9, 80 blocks) and the auto-join letter block ("Arabic Letters" tab).
- New files: `core/RetiredSlots.java`, `arabic/ArabicLetterRetirement.java`. Edited: `core/SlotManager.java`
  (`retireSlots`, reuse-safe `nextFreeSlotIndex`), `CustomBlocksMod.java` (boot wire-up).
- New persisted state: `config/customblocks/retired_slots.json`.
