# Changelog

Newest first. "Built" means code-complete and gates-green; ✋ items still need in-game confirmation
(nothing is DONE until the owner confirms in-game).

## [Unreleased]

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
