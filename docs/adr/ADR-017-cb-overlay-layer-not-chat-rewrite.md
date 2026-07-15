# ADR-017 — Flourishes render in a CustomBlocks overlay layer; vanilla chat is never rewritten

**Status:** Accepted (2026-07-14) · **Amended 2026-07-14** — see *Amendment: the widget framework was cut*
**Groups:** 04 (§G04-4 flourishes) · 03 (widgets)

---

## Context

Group 04 wants chat to feel alive: an animated flash when a destructive confirm fires, a rich hover card
with a spinning 3D block, live-updating progress on a line.

Every one of those wants to change a chat message **after it has been sent**. Vanilla Minecraft has no API
for that: `ChatHud` can be appended to and cleared, and that is the whole surface. A message, once sent, is
immutable text on a screen.

The tempting fix is to take over chat — re-render it ourselves, so we can animate whatever we like. That
would give us total control. It would also mean owning wrapping, scrollback, history, message signing,
the `overlay` flag, and every other mod that hooks the same screen. Chat is load-bearing, and a chat
rewrite is the single most likely thing in this mod to break silently, later, on someone else's setup.

## Decision

**Never rewrite or re-render vanilla chat. Draw the flourishes next to it, in a layer we own.**

There is a single **CustomBlocks overlay layer** (`CbOverlay`), drawn from the `HudRenderMixin` injection we
already had — the same layer that Group 03's HUD widgets render into.

- **Animated confirm** (`CbFlash`) — a red/lime edge-flash in the overlay layer, not in the chat line.
  The click still runs immediately; the flourish is pure feedback.
- **Live-updating progress** — rejected outright as a chat line, and moved to a **G03 HUD widget** in this
  same layer. That is where a value that changes every tick belongs anyway.
- **Rich 3D hover card** — needs a narrow tooltip-draw mixin (owner pre-approved). Not built in this pass;
  tracked as G04-B1. It will draw in the overlay too, never via vanilla `HoverEvent` (which only supports
  plain text / item / entity).

One client mixin supports this: the existing, pre-approved `HudRenderMixin` (`@Inject` at TAIL of
`InGameHud.render`). A second — `GameMenuScreenMixin` — existed to add the ESC panel button and was
deleted in the amendment below.

`/cb clearlogs` obeys the same rule. It has to remove only `[CB]` lines from one player's chat, which
needs to read back what is on screen — something `ChatHud` will not tell us. Rather than mixin into
`ChatHud` and start poking at vanilla chat internals, the client keeps its own shadow copy of incoming
messages (`CbChatMirror`) and rebuilds the chat box without our lines. No mixin, and vanilla chat's own
data structures are never touched.

## Consequences

- Vanilla chat keeps working: wrapping, scroll, history, signing, other mods. We add zero risk to it.
- Anything visually rich happens **beside** chat, not inside it. A chat line is, and stays, immutable text.
- One layer serves both groups, so G03's widgets and G04's flourishes share a single render entry point and
  cannot fight each other for draw order.
- Accepted limits, written down so nobody re-litigates them: a chat line **cannot** animate, **cannot**
  update in place, and **cannot** host a custom-rendered tooltip. Those are vanilla facts, not gaps in our
  implementation. When one of them is wanted, the answer is a widget in the overlay — not a chat rewrite.

---

## Amendment: the widget framework was cut (2026-07-14)

**The decision above stands unchanged.** The overlay layer is still the one place flourishes render, and
vanilla chat is still never rewritten. What changed is what renders *into* the layer.

Group 03 shipped 7 widgets, a per-player layout store, a Widgets tab in `/cb edithud`, a per-widget colour
picker, an op-force pin, an ESC quick-access panel, and a dev-stub launch flag. The owner's first MP test
rejected almost all of it. Three widgets were worth keeping; the framework built to support them was not.

**Cut:** the Buzzer timer and Vault toast (both `stubBacked` — no backend existed, so they could only render
invented numbers behind `-Dcustomblocks.devStubs=true`, which meant they were untestable in a normal run);
the incidents ping and its blip; the favourites strip; the ESC quick-access panel and its
`GameMenuScreenMixin` doorway; the Widgets tab, the layout store, the colour picker, the op-force pin, and
the never-built HUD-layout share code.

**Kept:** `ACTIVE_TOOL`, `MACRO_BANNER`, `LOCKED_PADLOCK`. All three are pending a look redesign.

Two things worth not rediscovering the hard way:

- **`IncidentRecorder` is not Group 03's and was not touched.** Roughly twenty command handlers write to it.
  What was cut is the HUD *ping* that surfaced it — a note-block blip fired on every logged incident, which
  is a lot of blips. The recorder still records; `/cb incidents` is how you read it. It no longer draws.
- **`WidgetSync` / `WidgetSyncPayload` / `WidgetSignals` survive, slimmed.** They look like widget-framework
  plumbing but they are the server→client transport for the three signals the *surviving* widgets render
  (tool mode, macro-recording, the locked-id set). Deleting them takes A1/A2/A3 down with them.

**Position and colour are no longer player-editable.** With the store and the tab gone, all three widgets
read their geometry and colour straight from `HudWidgetType`. That enum is now the single place the look of
the HUD is decided — which is what makes the pending redesign a one-line change per widget.
