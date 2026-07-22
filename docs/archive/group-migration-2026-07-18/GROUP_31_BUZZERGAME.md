# Group 31 — BuzzerGame (was TimerChallenge)

> **Status:** The session is **wand-owned** — it lives on the host player's wand, not a placed block.
> The wand-owned session (build item 1) is **built**; the stand display is next, coupled to the
> wand-session code since displays link to the wand too. Control is commands only — no GUI/Screen.
> **Source:** ported + rewritten from `Active_Projects/TimerChallenge` (~40% done standalone Fabric mod,
> mod-id `timerchal`, package `com.timerchal`). Treat that repo as reference-only, like old `CustomBlocks/`.
> **Commands:** `/cb buzzergame give buzzer|display|wand`, `start`, `stop`, `reset`, `reveal`,
> `set <key> <value>`, `help`. `set` covers the round settings (mode/format/countdown/falsestart/target
> — exact key names TBD at implementation time, not blocking design). One handler,
> `BuzzerGameCommands.java`, registered from `CommandRegistrar` like every other `*Commands.java`.
> **Registry namespace:** absorbed into CB-B's main namespace (`customblocks:buzzer`, etc), not a separate `buzzergame:` namespace.
> **Item access — buzzer, stand display, wand only:** dedicated BuzzerGame creative-tab
> entries, vanilla `/give @s customblocks:buzzer` (real registered Items, not slot-system SlotItems, so
> `/cb give` doesn't apply), and `/cb buzzergame give <buzzer|display|wand>` as a convenience wrapper.

## What this is

A YouTube-style buzzer/stopwatch minigame built as CB-B blocks: a **buzzer** block players press and
a compact **physical stand display** that shows the countdown (see Physical timer display below). The
**host's wand carries the session** — there's no separate control block. Two game formats:

- **Precision Stop** — timer counts UP visibly (both player and cameraman side), player buzzes once,
  distance to a target time decides the winner. Timer keeps counting past target if nobody buzzes —
  no hard cutoff.
- **Reaction Race** — 3-2-1-GO countdown (length configurable, on/off toggle), first buzz after GO wins.
  False-start (early buzz) rule is configurable: disqualify / time penalty / ignore.

v1 modes: **SOLO, DUEL, PARTY**. TEAM is deferred but buzzer→team assignment is designed to be fully
custom from day one (not colored blocks) — assignment mechanism TBD, revisit when TEAM is actually scoped.

## Session architecture — wand-owned

**The session belongs to the host player via the wand, not to a placed block.**

- **Anchor:** using the wand starts/owns a session for that player. The wand is the anchor — no
  separate control item to give, place, or break.
- **Linking method:** same wand, new target set — click a buzzer or a stand display to link it into
  your session. Order between buzzer/display links doesn't matter.
- **Session lifetime:** dies when the host logs off. No idle timeout, no manual `end` command needed
  beyond just disconnecting — logout is the cleanup trigger.
- **Break/logout handling:** if a linked buzzer or display is broken, OR the host logs off, all linked
  objects for that session auto-unlink and warn remaining players in chat. Same cleanup shape as the
  old break-handling rule, now triggered by logout too.
- **No distance limit** on linking — once linked, works from anywhere loaded.
- **Multiple concurrent hosts** — each host's wand carries its own independent session. No naming
  needed; session identity is simply "whatever's linked to this wand instance." Two hosts running
  duels at once just don't share any linked objects.
- **Mode-based start validation:** SOLO needs 1 buzzer, DUEL needs 2, PARTY needs 2+. `start` refuses
  and states why if unmet — the `start` command enforces it.

## Session control — commands only

Control is commands only: `/cb buzzergame <start|stop|reset|reveal|set|give|help>`. `set` covers the
round settings (mode, format, countdown length, false-start rule, target time) — exact argument shape
(single combined `start` args vs separate `set` calls) is an implementation detail to settle when this
is built, not a design gap.

## Buzzer block

- Physical model: half-dome game-show buzzer (see design mockup — red dome, black base), 1×1 footprint.
- **Multiple size/model variants** planned (low-profile flat, standard dome, tall dome), on top of the
  existing recolorable dome+base (reuses `pressedColor`-style customization already in
  `BuzzerBlockEntity`).
- Anti-spam debounce: **on by default**, configurable off, in addition to existing `lockFirstPress`
  round-lock logic.
- No redstone in/out.
- Optional floating name-tag (text display entity) per buzzer — player name/color, fully
  toggle/customizable, off by default complexity-wise but wanted for filming.
- Placement permission: **anyone** can place/give it, same as before — decorative/gameplay item, no
  admin gate.

## Physical timer display

A **compact standalone stand display** — think a "cooler version of a Rubik's-cube-competition timer":
a small object with an angled screen face on a short dark stand, glowing digits, not a room-sized wall.

- **What it is technically:** a real placeable block (like the buzzer) whose block entity owns a
  linked display entity (block-display/item-display, Minecraft's freely-transformable decorative
  entity type) for the actual visual — this is what makes live resizing/rotation possible without
  rebuilding anything.
- **Both faces readable** — front (player side) and back (camera side) both show the digits.
- **Facing:** faces the direction you were looking when you placed it (sign-style). A right-click
  rotate action lets you fix the angle afterward without breaking and replacing it (later build item).
- **Placement permission: anyone**, same as buzzer.
- **Digit rendering — both techniques get built, switchable per-display in settings:**
  1. Built-in glowing text-display (fast, colorable, uses Minecraft's font). **Build item 1 uses this
     only.**
  2. Custom baked LED/7-segment-style textures per digit, swapped as the number changes — later build
     item, closer match to the reference photo.
- **Item 1 visual spec (locked 2026-07-18):**
  - **Real art from day one** — no placeholder box. Model/texture (angled screen + dark stand) is
    supplied by the developer, not a stand-in.
  - **Default size: Large.** (Before the live resize tool exists.)
  - **Digit color: CB-B brand red/black/lime**, applied now, not deferred to a later color pass.
  - **Digit format:** `0.00` (no left zero-pad) while under 10 seconds. Once the value hits **10.00 or
    higher**, it switches to 4-digit `00.00` (two digits before the point, two after).
  - **Idle state (before any round starts): all zeros** (`0.00`), no idle GIF/logo yet (that toggle is
    still a later item, see below).
- **Resize — a live tool, not a number-entry GUI (later build item, unchanged spec):** hold/use the
  resize tool and **move your cursor to preview the size changing pixel-by-pixel in real time**
  (blueprint/ghost-preview style), plus quick Small/Medium/Large presets to jump to a rough size first.
  Nothing saves while dragging the preview — locks in only on a final confirm click. The interactive
  click-box scales along with the visual.
- **Multiple displays per session are allowed** — link as many stand displays to one wand session as
  you want, e.g. one facing the players and one facing the camera. All linked displays sync the same
  live digits; each display's size/rotation is independent (a placement property, not shared game
  state).
- **Breaking a linked display or host logout** — auto-unlinks and warns remaining players, see Session
  architecture above.
- **Frame customization (custom photo/logo/GIF on the frame)** — fixed default look for now; later
  polish item, not part of item 1.
- Display precision: **hundredths of a second**, see digit format above.
- **Idle-state display toggle (custom GIF/logo instead of all-zeros)** — later item, pulled forward in
  priority once item 1 ships but not part of item 1 itself.

## Sound & VFX

- Real game-show SFX (buzzer honk, countdown ticks, win fanfare) sourced from royalty-free libraries
  (freesound.org / Pixabay Audio), converted to `.ogg`, wired as normal CB sound events.
- Every sound **independently toggleable** via `set`.
- Particle burst on buzzer press; bigger burst/fireworks on winner reveal. Toggleable.
- Announcements (title/actionbar): **near-game broadcast only** by default, radius configurable — not
  server-wide.

## Scoring & persistence

Two scoreboards:

1. **Session-local** — wiped when the session ends (host logout) or by a reset command.
2. **Global** — never auto-wiped.

Global scoreboard syncs to CB-B's **existing Cloudflare Worker** (`cloudflare/worker.js`, Group 20 vault)
via a **new small KV-backed endpoint** — reusing existing cloud infra rather than building new plumbing.
Purpose: pull stats after filming for video reference. **No live OBS overlay** for now (revisit later
if wanted).

Rewards are **cosmetic only** — no item/XP/command payouts on win.

## CustomBlocks crossover (v1)

Ties BuzzerGame into CB-B's actual core feature (image/GIF → block) instead of being a plain,
disconnected minigame:

- **Display frame/background:** the stand display's frame can be any custom image/GIF loaded through
  CB-B's existing image-to-block tool, not just a flat color. **Deferred** — fixed default look ships
  first (item 1).
- **Buzzer skin:** the buzzer dome can be retextured with any photo/logo/face via the same CB-B tool.
- **Idle-screen custom GIF:** before a round starts (session IDLE), a settings toggle on the display
  switches between all-zeros (default, item 1) and a custom GIF/logo — later item.

Deferred for later: win-reveal celebration image/GIF pop.

## Explicitly decided against / deferred

- No redstone integration.
- No separate `/tc` command tree — everything under `/cb buzzergame`.
- Win-reveal celebration custom image/GIF (deferred, not v1).
- Arabic localization: English first, Arabic backfilled later (existing `ar_sa.json` in the old repo is
  reference only, not synced yet).
- Live OBS overlay integration.
- Cross-server global leaderboard merge (only relevant if run on multiple servers later).
- **Extra digit visual styles** (retro LCD / sleek pixel / flip-style) beyond the one default CB-B
  red/black/lime look — later "add more styles" polish pass.
- **Display frame custom image/GIF** (the crossover idea above) — later polish, fixed look ships first.
- **Named size/style presets** for the display (save a full setup, reuse later) — still a documented
  later polish item, not in current scope.
- **Team assignment mechanism** — designed intent stands (fully custom, not colored blocks), concrete
  mechanism deferred until TEAM mode itself is scoped.

## Build order — grouped by what actually depends on what, priority-ordered per the 2026-07-18 session

Test in-game and get developer confirmation before moving to the next item (CLAUDE.md §2/§4). Each
item gets its own checklist rows in
[Group_31_Testing_Guide.md](../testing/Group_31_Testing_Guide.md) as it's built — nothing is ✅ until
the developer confirms it in-game. **One item at a time (CLAUDE.md §4) — do not batch these.**

Precision Stop (Solo), Reveal, Reaction Race, and false-start rules logic is assumed sound and is
replumbed onto the wand session (item 1).

**Next up, in order:**
1. **Wand-owned session** (built): wand is the session anchor (start/own session on use), link
   buzzer/display via wand click, logout-triggers-cleanup (auto-unlink + warn), mode-based start
   validation lives in the `start` command. Precision/Reaction/Reveal logic runs on the wand session.
2. **Command rewrite:** `/cb buzzergame give buzzer|display|wand`, `start`, `stop`, `reset`, `reveal`,
   `set <key> <value>`, `help`. `set` covers mode/format/countdown/false-start/target (exact key syntax
   decided at implementation time).
3. **Stand display — static version:** custom block + block entity + linked block-display entity,
   real art (developer-supplied model/texture), default facing-on-place, both faces show digits,
   text-display digit technique only, **Large** default size, CB-B brand red/black/lime digit color,
   `0.00`→`00.00` format switch at 10s, idle = all zeros, wand-linkable, anyone can place, auto-unlink
   + warn on break/logout.
4. Stand display — live resize tool: S/M/L presets + real-time drag-to-scale preview, confirm-to-lock,
   click-box scales with the visual.
5. Stand display — right-click rotate after placement.
6. Stand display — custom baked LED-texture digit mode + the settings toggle to switch between that
   and the text-display technique.
7. DUEL/PARTY ranked-list reveal (ranking formula, DQ-separated, per-rank chat lines, brand colors) —
   see Reveal flow section below.

**Later polish (deferred, unchanged):** sound/VFX pass, scoreboards (session-local + global +
Cloudflare Worker endpoint), extra digit styles, display frame image/GIF crossover, named
size/style presets, team assignment mechanism, idle custom GIF/logo toggle.

## Reveal flow (game-show pacing for filming)

Display freezes each buzzer's raw stopped time on buzz, but hides who's closest/winner. Host runs
`/cb buzzergame reveal` to show the result — deliberate drama for video, not auto-instant reveal.

**Ranking rule (Duel and Party share the same formula — Duel is just a 2-person Party):**
- Precision Stop → closest-to-target first, worst last.
- Reaction Race → fastest first, slowest last.
- A disqualified (false-started, DQ rule) buzzer is **not** ranked — it's listed separately at the
  bottom as "Disqualified: <name>".
- Chat gets **one line per rank** (`1st: Alex — 4.20s`, `2nd: Sam — 4.55s`, ...).
- The big on-screen title stays just the winner's name (`Alex wins!`); the subtitle adds the runner-up
  (`2nd: Sam`); the rest of the ranked list is chat-only.
- Colors are the CB-B brand (red/black/lime, not gold) for reveal text/titles, matching the rest of the
  mod.
- This is real scoring work (not yet built — Solo-only single-winner exists today), tracked as build
  item **7** above.

> **For the next chat picking this up:** read this whole doc plus
> `docs/testing/Group_31_Testing_Guide.md` first. Build item 1 (wand-owned session) is done; the stand
> display (item 3) is next and unblocks the rest. Do not skip ahead — CLAUDE.md §4
> "max 5 items in any plan, ideally 1" still applies.
