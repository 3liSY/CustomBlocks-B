# Group 31 — BuzzerGame (was TimerChallenge)

> **Status:** Phase 1 items 1-2-3-5-6 built (🟢, awaiting in-game confirm on C/E/F — see
> [GROUP_31_TESTING_GUIDE.md](../testing/GROUP_31_TESTING_GUIDE.md)). **Redesign locked 2026-07-07** after a
> full Q&A session with the developer covering command simplification, the admin panel GUI, ranked
> Duel/Party reveal, and a full replacement of the old "multiblock wall" idea with a compact physical
> stand display. No code written yet for anything in this redesign — see **Build order** below for what's
> next.
> **Source:** ported + rewritten from `Active_Projects/TimerChallenge` (~40% done standalone Fabric mod,
> mod-id `timerchal`, package `com.timerchal`). Treat that repo as reference-only, like old `CustomBlocks/`.
> **Commands (trimmed 2026-07-07):** `/cb buzzergame give buzzer|panel|wand`, `start`, `stop`, `reset`,
> `reveal`, `help`. That's the whole command tree now — `mode`, `game`, `target`, `countdown`, `falsestart`,
> `state`, and the debug-only `advance` stepper are all **removed**; every one of them becomes a click in
> the admin panel GUI instead (see **Admin panel — command trim + GUI** below). One handler,
> `BuzzerGameCommands.java`, registered from `CommandRegistrar` like every other `*Commands.java`.
> **Registry namespace:** absorbed into CB-B's main namespace (`customblocks:buzzer`, etc), not a separate `buzzergame:` namespace.
> **Item access — all three work, no conflict:** dedicated BuzzerGame creative-tab entry, vanilla
> `/give @s customblocks:buzzer` (they're real registered Items, not slot-system SlotItems, so
> `/cb give` doesn't apply), and `/cb buzzergame give <buzzer|panel|wand>` as a convenience wrapper.

## What this is

A YouTube-style buzzer/stopwatch minigame built as CB-B blocks: a **buzzer** block players press,
a compact **physical stand display** that shows the countdown (see Physical timer display below —
replaces the original multiblock-wall idea), and an **admin panel** block the host uses to run the
show. Two game formats:

- **Precision Stop** — timer counts UP visibly (both player and cameraman side), player buzzes once,
  distance to a target time decides the winner. Timer keeps counting past target if nobody buzzes —
  no hard cutoff.
- **Reaction Race** — 3-2-1-GO countdown (length configurable, on/off toggle), first buzz after GO wins.
  False-start (early buzz) rule is configurable: disqualify / time penalty / ignore.

v1 modes: **SOLO, DUEL, PARTY**. TEAM is deferred but buzzer→team assignment is designed to be fully
custom via the admin panel from day one (not colored blocks).

## Session architecture — linking

**Central session object, one per admin panel** (`PanelSession`) is the single source of truth.
Buzzers and screens store only their session's UUID and look everything else up through the session —
not mutual cross-references. This was picked specifically to avoid desync bugs.

- **Linking method:** dedicated link wand item. Click admin panel first, then click each buzzer/screen
  to bind it to that session.
- **Break handling:** if a linked buzzer/screen is broken mid-session, the session auto-removes it from
  its link list and warns the host (on panel/screen), round continues if still playable.
- **Panel = session root:** breaking the admin panel destroys the whole session (in-progress round,
  links, session-local scoreboard). Already-synced global scoreboard entries are unaffected.
- **No distance limit** on linking — once linked, works from anywhere loaded.
- **Multiple sessions run fully independently and concurrently** — no cross-talk between different
  admin panels.
- **Mode-based start validation:** SOLO needs 1 buzzer, DUEL needs 2, PARTY needs 2+. Admin panel
  blocks Start and shows why if unmet.

## Reveal flow (game-show pacing for filming)

Display freezes each buzzer's raw stopped time on buzz, but hides who's closest/winner. Host presses
**Reveal** to show the result — deliberate drama for video, not auto-instant reveal.

**Ranking rule (Duel and Party share the same formula — Duel is just a 2-person Party):**
- Precision Stop → closest-to-target first, worst last.
- Reaction Race → fastest first, slowest last.
- A disqualified (false-started, DQ rule) buzzer is **not** ranked — it's listed separately at the
  bottom as "Disqualified: <name>".
- Chat gets **one line per rank** (`1st: Alex — 4.20s`, `2nd: Sam — 4.55s`, ...).
- The big on-screen title stays just the winner's name (`Alex wins!`); the subtitle adds the runner-up
  (`2nd: Sam`); the rest of the ranked list is chat-only.
- Colors switch from the old gold/green to the CB-B brand (red/black/lime, not gold) for reveal
  text/titles, matching the rest of the mod.
- This is real scoring work (not yet built — Solo-only single-winner exists today), tracked as build
  item **G** below. Lower priority than the physical display, but **not deferred** — still in scope.

## Admin panel — command trim + GUI

> **Screen spec moved 2026-07-12** — full spec (buttons, settings items, link-list sub-screen) now lives
> in `GROUP_27_SCREENS.md` §G27.28. G31 keeps the trimmed command list (top of this doc), round/session
> logic, and everything below (buzzer block, physical timer display, sound/VFX). Built 2026-07-10 per
> `GROUP_31_TESTING_GUIDE.md` line 6 — the old "not built yet" wording above predated that build.

Access: **op / admin-panel-permission only** for all control actions. Regular players can only press
buzzers. Team assignment (v1-ready, TEAM mode itself deferred) stays a documented future addition to the
GUI — not scoped into the current redesign pass.

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

## Physical timer display — replaces the old "multiblock wall" idea entirely (redesigned 2026-07-07)

The original plan was a big resizable multiblock wall. The developer instead wants a **compact
standalone stand display** — think a "cooler version of a Rubik's-cube-competition timer": a small
object with an angled screen face on a short dark stand, glowing digits, not a room-sized wall. This
fully replaces the wall — there is no separate "big wall" mode.

- **What it is technically:** a real placeable block (like the buzzer/panel) whose block entity owns
  a linked display entity (block-display/item-display, Minecraft's freely-transformable decorative
  entity type) for the actual visual — this is what makes live resizing/rotation possible without
  rebuilding anything.
- **Both faces readable** — front (player side) and back (camera side) both show the digits, same
  requirement as the old wall had, just on a compact object instead of a big double-sided wall.
- **Facing:** faces the direction you were looking when you placed it (sign-style). A right-click
  rotate action lets you fix the angle afterward without breaking and replacing it.
- **Digit rendering — both techniques get built, switchable per-display in settings:**
  1. Built-in glowing text-display (fast, colorable, uses Minecraft's font).
  2. Custom baked LED/7-segment-style textures per digit, swapped as the number changes — the closer
     match to the reference photo.
  A settings toggle on the display switches between the two anytime. Only **one visual style** (the
  CB-B default red/black/lime look) ships built for now — the old idea of 4 selectable styles
  (7-segment / retro LCD / sleek pixel / flip-style) becomes a later "add more styles" polish pass, not
  part of this build.
- **Resize — a live tool, not a number-entry GUI:** hold/use the resize tool and **move your cursor to
  preview the size changing pixel-by-pixel in real time** (blueprint/ghost-preview style), plus quick
  Small/Medium/Large presets to jump to a rough size first. Nothing saves while you're dragging the
  preview — it only locks in on a final confirm click. The interactive click-box scales along with the
  visual (a shrunk display has a small click area, a huge one has a big click area), not a fixed hitbox.
- **Multiple displays per panel are allowed** — link as many stand displays to one panel as you want
  (same wand flow as buzzers: click panel, then click the display), e.g. one facing the players and
  one facing the camera. All linked displays sync the same live digits; each display's size/rotation is
  independent (a placement property, not shared game state).
- **Breaking a linked display** behaves like breaking a buzzer: auto-unlinks from the panel and warns
  the host in chat.
- **Frame customization (custom photo/logo/GIF on the frame, from the CustomBlocks crossover idea)** —
  fixed default look for now; the frame image/GIF crossover is a later polish item, not part of this
  build.
- Display precision: **hundredths of a second** (e.g. `47.03s`), unchanged from the original spec.
- **Idle-state display:** a settings toggle picks what shows before a round starts — **default is all
  zeros**, with a custom idle GIF/logo as an alternative option the host can switch to (pulled forward
  from the original "later crossover" idea into a day-one settings toggle, since it's just a toggle on
  top of work already being done).

## Sound & VFX

- Real game-show SFX (buzzer honk, countdown ticks, win fanfare) sourced from royalty-free libraries
  (freesound.org / Pixabay Audio), converted to `.ogg`, wired as normal CB sound events.
- Every sound **independently toggleable** in settings.
- Particle burst on buzzer press; bigger burst/fireworks on winner reveal. Toggleable.
- Announcements (title/actionbar): **near-game broadcast only** by default, radius configurable — not
  server-wide.

## Scoring & persistence

Two scoreboards:

1. **Session-local** — wiped on server restart or by a reset command.
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
  CB-B's existing image-to-block tool, not just a flat color. **Deferred** (see Physical timer display
  section above) — fixed default look ships first.
- **Buzzer skin:** the buzzer dome can be retextured with any photo/logo/face via the same CB-B tool.
- **Idle-screen custom GIF:** before a round starts (session IDLE), a settings toggle on the display
  switches between all-zeros (default) and a custom GIF/logo — pulled forward into day-one scope, see
  Physical timer display section above.

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
- **Named size/style presets** for the display (save a full setup, reuse later) — not re-confirmed in
  the 2026-07-07 redesign session; still a documented later polish item, not in current scope.
- **Team assignment GUI** — designed, still deferred; not scoped into the current admin-panel-GUI pass.

## Build order — grouped by what actually depends on what, priority-ordered per the 2026-07-07 session

Test in-game and get developer confirmation before moving to the next item (CLAUDE.md §2/§4). Each
item gets its own checklist rows in
[GROUP_31_TESTING_GUIDE.md](../testing/GROUP_31_TESTING_GUIDE.md) as it's built — nothing is ✅ until
the developer confirms it in-game. **One item at a time (CLAUDE.md §4) — do not batch these.**

**Already built (🟢, awaiting in-game confirm on C/E/F):** buzzer block + press detection, admin panel +
`PanelSession` state machine, link wand + linking, Precision Stop (Solo), Reveal + Reaction Race +
false-start rules — all still using the **old** command set for now, until the items below land.

**Next up — physical stand display first (developer's stated main feature), then admin panel GUI:**
1. Stand display — static version: custom block + block entity + a linked block-display entity for
   the model, default facing-on-place, both faces show digits, text-display digit technique only
   (fastest path to something real), fixed default size, wand-linkable to a panel (screens re-enabled
   in the link flow once this exists), auto-unlink + warn on break. No resize tool yet.
2. Stand display — live resize tool: S/M/L presets + real-time drag-to-scale preview, confirm-to-lock,
   click-box scales with the visual.
3. Stand display — right-click rotate after placement.
4. Stand display — custom baked LED-texture digit mode + the settings toggle to switch between that
   and the text-display technique.
5. Admin panel GUI shell: right-click panel opens the 4-row branded chest screen; Start/Stop/Reset
   (confirm-if-live)/Reveal buttons wired to the same logic the remote commands use; trim
   `/cb buzzergame` down to `give`/`start`/`stop`/`reset`/`reveal`/`help`.
6. Admin panel GUI settings: Mode/Format/Countdown/False-start cycle items, Target-time
   cycle-or-type-exact, status book item, grey-out + hover-hint on invalid-state buttons.
7. Admin panel GUI link list: linked-count button → auto-numbered list screen → unlink-with-confirm.
8. DUEL/PARTY ranked-list reveal (ranking formula, DQ-separated, per-rank chat lines, brand colors) —
   see Reveal flow section above.

**Later polish (deferred, unchanged from before):** sound/VFX pass, scoreboards (session-local + global
+ Cloudflare Worker endpoint), extra digit styles, display frame image/GIF crossover, named
size/style presets, team assignment GUI.

> **For the next chat picking this up:** read this whole doc plus
> `docs/testing/GROUP_31_TESTING_GUIDE.md` first. Start with item 1 above only. Do not skip ahead —
> CLAUDE.md §4 "max 5 items in any plan, ideally 1" still applies.
