# ADR-009: Make colour-Square swaps instant with client-side prediction

Date: 2026-06-20
Status: Accepted

## Context
Right-clicking a placed custom block with a colour Square swaps it to that colour's existing
variant. The swap is server-authoritative: the Square's `useOnBlock` runs on the server and
calls `ColorVariantService.swapPlaced`, which is a single `world.setBlockState` — no image
work, no pack rebuild. It is instant *on the server*.

What the player saw was not instant. The B items do nothing on the client — they swing the
arm, return `SUCCESS`, and wait for the server (`ShapeToolItem`/`CustomColorToolItem`:
`!(getPlayer() instanceof ServerPlayerEntity) → return SUCCESS`). So the visible block change
costs a full network round-trip: click → C2S use packet → server `setBlockState` → block-update
packet back → client re-render. On a server (or even the integrated server) that is the entire
perceived lag.

The old project felt instant because its `ColorSquareItem.useOnBlock` had an
`if (world.isClient)` branch that applied the swap to the client world immediately. B dropped
that branch during the clean-room rewrite — that is the regression.

## Decision
Restore client-side prediction, but as a standalone client listener rather than logic baked
into the common item (which must not reference client-only classes like `ClientSlotCache`).

`client/ClientSwapPredictor` registers a Fabric `UseBlockCallback`. On the client, when the
player right-clicks a `SlotBlock` holding a tool that implements the new common interface
`item/ColorSwapTool` (the Squares — Triangles return `null`), it:
1. resolves the clicked block's synced id from `ClientSlotCache`,
2. computes the target variant id with the server's own math
   (`ColorVariantService.variantId` / `stripColourSuffix`),
3. looks the target up in `ClientSlotCache` (Black Square falls back to the base block),
4. paints it locally: `setBlockState(pos, target.getDefaultState().with(LIGHT, glow),
   NOTIFY_LISTENERS | FORCE_STATE)`,
5. returns `ActionResult.PASS` so the real interaction still goes to the server.

The server still performs the authoritative swap. Because the predicted state mirrors the
server exactly — same target block, same `LIGHT` (glow) value (ADR-002) — the authoritative
packet reconciles with no visible change.

## Rationale
- Client-side prediction is the vanilla-idiomatic way to hide round-trip latency on a
  client-initiated action (block place/break do exactly this). It is the correct fix, not a
  workaround.
- **Shared id math.** The predictor calls `ColorVariantService`'s own static methods, so the
  client target can never drift from the server's. The old project duplicated the resolution
  logic client-side — a drift hazard.
- **Glow-accurate.** B carries glow in the `LIGHT` blockstate property; predicting it avoids a
  relight flash on reconcile. The old branch ignored glow.
- **Predict hits, defer misses.** If the target variant is not in the synced cache, the
  predictor paints nothing and lets the server respond ("create it with the Triangle first").
  No wrong guess, no flicker, and the server's helpful message is preserved.
- **No new pitfall.** The predictor never returns anything but `PASS`, so the item's use-flow
  and the server round-trip are untouched — it does not reintroduce the "client-side skip
  delay on tools" pitfall (CLAUDE.md §7), which was about early-returning `PASS` *instead of*
  the server work, not alongside it.

## Consequences
- A mispredict (stale `ClientSlotCache`) is self-correcting: the authoritative packet overrides
  the client within the same round-trip the player already tolerates today. Worst case equals
  current behaviour, never worse.
- Prediction only runs on a modded client. Vanilla clients on a modded server keep the
  round-trip behaviour (they have no `ClientSlotCache` and no listener) — acceptable.
- `ColorSwapTool` is the seam for any future swap tool: implement it and prediction comes free.
- Arabic-letter recolour (per-BlockEntity colour, `ShapeToolItem.recolorArabicLetter`) is a
  different mechanism and is out of scope here; it can get the same treatment later if needed.
