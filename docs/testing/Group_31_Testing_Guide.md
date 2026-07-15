# Group 31 — BuzzerGame

**🎯 Test now:** W · D · D2 · D3 · D4 · K · C · E · F · G — all 5 core items built 2026-07-10 · **✅ Passed:** A · B
**📦 Jar:** `customblocks-1.0.0.jar`

> ✅ UI medium audit (2026-07-09) **resolved 2026-07-10:** the admin panel is now a full custom Screen (§K).
> Screen spec moved 2026-07-12 → `GROUP_27_SCREENS.md` §G27.28. §K test rows (K1–K10) stay here.

## 📊 Status

|                 |                                          |
| --------------- | ---------------------------------------- |
| **Verdict**     | 🎯 test-now — all 5 core items built (W · D · D2 · D3 · D4 · K · C · E · F · G) |
| **Progress**    | ✅✅🟡🟡🟡🟡🟡🟡🟡🟡 · 2/12 confirmed · W·C·D·D2·D3·D4·K·E·F·G built ⏳ test |
| **Last tested** | 2026-07-06 (§A + §B ✅ SP+MP); 2026-07-10 core-4 build: wand · rotate · LED · admin Screen · Duel/Party |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

> Rows MUST be sorted by priority, never alphabetically by §: 🎯 → 💔/❔/❗ → 🟡/⚠️ → ✅ → 🛠️ → 🧊 → ⏳.

| §   | What                                                        | State      |
| --- | ------------------------------------------------------------ | ---------- |
| W   | Central wand — one tool, sneak-cycled modes: Link + Resize + Rotate + Digit-style | 🎯 test-now |
| D   | Physical timer stand — static stand + live glowing digits (redesign item 1) | 🎯 test-now |
| D2  | Timer stand — live resize (wand Resize mode + presets + scaling hitbox) (redesign item 2) | 🎯 test-now |
| D3  | Timer stand — 5° rotate (wand Rotate mode) (redesign item 3)   | 🎯 test-now |
| D4  | Timer stand — LED 7-segment digits + wand Digit-style toggle (redesign item 4) | 🎯 test-now |
| C   | Link wand + linking flow                                       | 🎯 test-now |
| E   | Precision Stop mode end-to-end (SOLO)                          | 🎯 test-now |
| F   | Reveal flow + Reaction Race + false-start rules                | 🎯 test-now |
| K   | Admin panel Screen + trimmed commands (redesign items 5-7)     | 🎯 test-now |
| G   | Duel / Party multi-buzzer scoring + ranked reveal              | 🎯 test-now |
| A   | Buzzer block + press detection                                | ✅ pass     |
| B   | Admin panel + PanelSession state machine                      | ✅ pass     |
| H   | Sound / VFX pass                                                | ⏳ planned  |
| I   | Scoreboards (session-local, global + Cloudflare endpoint)       | ⏳ planned  |
| J   | Extra digit styles, display frame image/GIF, named presets, name tags, polish | ⏳ planned  |

🔗 **Related Doc:** [GROUP_31_BUZZERGAME.md](../groups/GROUP_31_BUZZERGAME.md)

---

# 🎯 Test now

### 🧰 Setup Required
* Get the gear: `/cb buzzergame give buzzer | panel | display | wand` (or the BuzzerGame creative tab, or
  `/give @s customblocks:buzzer` / `:admin_panel` / `:timer_display` / `:buzzergame_wand`).
* **One BuzzerGame Wand does everything now** (it replaced the old separate Link Wand + Resize Tool).
  **Sneak + right-click air** cycles its mode (Link → Resize → Rotate → Digit-style; action bar shows which).
  **Right-click air, no sneak** flips the current mode's direction (Resize BIGGER ↔ SMALLER, Rotate CW ↔
  anti-CW). **Right-click a block** applies the mode.
* Place **1 admin panel** + **1 buzzer**. Wand actions + panel control are **op-only**.
* **Right-click the admin panel (op) → the branded BuzzerGame Screen** (Start/Stop/Reset/Reveal +
  Mode/Format/Countdown/False-start/Target + link list). The round can also be driven remotely by
  `/cb buzzergame start | stop | reset | reveal`. Live timer / countdown / winner show as an **action-bar
  line + big titles near the panel** (~48 blocks) and on any linked timer stand. **You need the mod
  client-side** to see the Screen; vanilla clients fall back to the commands.

**W · Central wand** — one tool for setup (`/cb buzzergame give wand`); **op-only**. Replaces the old Link Wand + Resize Tool.
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| W1 | Hold the wand, **sneak + right-click air** (repeat) | Action bar cycles "Wand mode: **Link**" → "**Resize**" → "**Rotate**" → "**Digit style**" → back to Link | 🟥  | 🟥  |
| W2 | In **Resize** mode, right-click air (no sneak), repeat | Action bar toggles "Resize direction: **BIGGER**" ↔ "**SMALLER**" | 🟥  | 🟥  |
| W3 | In **Link** mode: right-click a panel, then a buzzer, then a stand | Panel selected → "Linked buzzer…" → "Linked timer stand…" (old link-wand flow) | 🟥  | 🟥  |
| W4 | In **Resize** mode: right-click a stand a few times, flip direction, again | Stand grows / shrinks one notch per click (old resize-tool behaviour) | 🟥  | 🟥  |
| W5 | Wrong mode: in **Resize** mode, right-click a panel or buzzer | Yellow nudge "Switch to Link mode to …"; no action taken | 🟥  | 🟥  |
| W6 | Non-op holds the wand, clicks a stand or panel | Denied: "Only operators can use the BuzzerGame wand." | 🟥  | 🟥  |
| W7 | `/give @s customblocks:link_wand` and `:resize_tool` | Both fail — unknown item (retired; only `customblocks:buzzergame_wand` exists now) | 🟥  | 🟥  |

**C · Linking flow** (now the wand's **Link mode** — the default) — bind buzzers to a panel's session
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| C1 | With the wand (Link mode), right-click the admin panel (op) | "Panel selected. Now right-click buzzers / timer stands to link them to it." | 🟥  | 🟥  |
| C2 | With the wand, right-click a buzzer            | "Linked buzzer to panel (1 linked)." Then right-click the panel (empty hand) → the Screen's status shows **Linked 1 buzzer(s)** | 🟥  | 🟥  |
| C3 | Try to link with no panel selected (fresh wand) | "Select an admin panel first — right-click one with the wand." | 🟥  | 🟥  |
| C4 | Start a round, then try to link another buzzer | Refused: "Reset the round to Idle before changing its links." | 🟥  | 🟥  |
| C5 | Break a linked buzzer                          | Nearby warning "A linked buzzer was removed."; panel readout drops the count | 🟥  | 🟥  |
| C6 | Non-op right-clicks the panel with the wand    | Denied (op-only) | 🟥  | 🟥  |
| C7 | Break the admin panel, then press the buzzer   | Session gone → buzzer falls back to the standalone demo press (honk + `[BuzzerGame] Buzz!`) | 🟥  | 🟥  |

**E · Precision Stop (SOLO)** — setup: link 1 buzzer, then in the panel Screen set **Mode Solo · Format Precision · Target 5.00s**
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| E1 | Screen **Start** (or `/cb buzzergame start`) | Near the panel: 3-2-1-GO titles + sounds, then action-bar **"Time X.XXs (target 5.00s)"** counting up | 🟥  | 🟥  |
| E2 | Press the buzzer once          | Chime; "Buzzed! Time locked in — waiting for the host to reveal"; action-bar → "Buzzed! Host: reveal". **Winner NOT shown yet** | 🟥  | 🟥  |
| E3 | Screen **Reveal** (or `/cb buzzergame reveal`) | Big **"<you> wins!"** title + "Stopped X.XXs - target 5.00s - off by Z.ZZs"; round → **Finished** | 🟥  | 🟥  |
| E4 | Screen: **Reset** → **Target** 3s → **Start**, then DON'T buzz | Target reads 3.00s; timer **keeps counting past 3.00s** (no hard cutoff) | 🟥  | 🟥  |
| E5 | Screen: **Reset** → **Countdown** Off → **Start** | No 3-2-1 — the timer starts counting immediately | 🟥  | 🟥  |

**F · Reveal + Reaction Race + false-start** — setup: link 1 buzzer, then in the Screen set **Mode Solo · Format Reaction**
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| F1 | **Start**; buzz **after** GO; then **Reveal** | Action-bar "GO! X.XXs"; buzz records reaction; reveal shows "Reaction X.XXs" | 🟥  | 🟥  |
| F2 | Screen False-start → **DQ**; Start; buzz **during** the 3-2-1; Reveal | "False start — disqualified!"; reveal → "No valid result / Everyone false-started" | 🟥  | 🟥  |
| F3 | False-start → **Ignore**; Start; buzz during 3-2-1, then buzz after GO | Early press: "Too early! Press ignored"; the post-GO buzz records normally | 🟥  | 🟥  |
| F4 | False-start → **Penalty**; Start; buzz during 3-2-1, then buzz after GO | Early: "A time penalty will apply"; reveal shows the reaction "(with penalty)" | 🟥  | 🟥  |
| F5 | Screen: Reset → **Countdown** 5 → Start | Countdown runs 5-4-3-2-1-GO (length changed) | 🟥  | 🟥  |

**K · Admin panel Screen + trimmed commands** — right-click a panel (op) to open it
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| K1 | Right-click the panel (op, empty hand) | Branded red/black Screen opens: title "BuzzerGame Panel — <state>", status readout, control + settings buttons | 🟥  | 🟥  |
| K2 | Click Mode / Format / Countdown / False-start (while Idle) | Each cycles its value; the Screen refreshes with the new value | 🟥  | 🟥  |
| K3 | Click **Target** (Idle); then **Type exact…**, enter 7.5, OK/Enter | Target cycles presets 3/5/10/15/30/60; the type box sets an exact 7.50s | 🟥  | 🟥  |
| K4 | Start a round, reopen the Screen | Mode/Format/Countdown/False-start/Target/Start are **greyed**; hover explains "Already running — stop or reset first" | 🟥  | 🟥  |
| K5 | On a **live** round click **Reset** | Button flips to "Confirm?"; a second click resets. (Idle/Finished reset is instant) | 🟥  | 🟥  |
| K6 | The **Reveal** button | Greyed until the round reaches **Results** (everyone buzzed), then lights up | 🟥  | 🟥  |
| K7 | Click **Buzzers ▸ N** → a buzzer row → click it again | Expands the list; first click arms "Unlink #k?", second unlinks; count drops (Idle only) | 🟥  | 🟥  |
| K8 | Non-op right-clicks the panel | Nothing opens; any action denied "Only operators can control the admin panel." | 🟥  | 🟥  |
| K9 | Remote `/cb buzzergame reset` on a **live** round | Asks "run again within 5s to confirm"; a second reset within 5s resets | 🟥  | 🟥  |
| K10 | `/cb buzzergame help`; try an old verb | Lists give/start/stop/reset/reveal/size; `mode`/`game`/`target`/`countdown`/`falsestart`/`state`/`advance` no longer exist | 🟥  | 🟥  |

**G · Duel / Party ranked reveal** — link **2+ buzzers**, then set **Mode Duel** (or **Party**) in the Screen
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| G1 | Mode **Duel** with only 1 buzzer linked → **Start** | Blocked: "needs 2 buzzer(s), 1 linked …" | 🟥  | 🟥  |
| G2 | 2 buzzers, Duel · Precision · Start; two players each buzz once; **Reveal** | Title **"<closest> wins!"**, subtitle **"2nd: <other>"**; chat: "1st: … — X.XXs (off …)", "2nd: …" | 🟥  | 🟥  |
| G3 | **Party**, 3 buzzers, Reaction, Start; all three buzz after GO; Reveal | Ranked fastest→slowest, one chat line each; title = winner, subtitle = 2nd | 🟥  | 🟥  |
| G4 | Reaction · False-start **DQ**; one player false-starts, another buzzes after GO; Reveal | DQ'd player **not** ranked — listed "**Disqualified: <name>**" (red) at the bottom; the valid buzz wins | 🟥  | 🟥  |
| G5 | Everyone false-starts (DQ rule) | Title "No valid result / Everyone false-started"; each shown "Disqualified: <name>" | 🟥  | 🟥  |
| G6 | Reveal colours | Winner title + "1st" line are **lime (#40FF00)**, DQ lines **red** — not the old gold/green | 🟥  | 🟥  |

**D · Timer stand (static)** — get it: `/cb buzzergame give display` (or the BuzzerGame tab / `/give @s customblocks:timer_display`)
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| D1 | Place the timer stand          | Dark stand (base + leg + angled red-framed screen) appears; screen faces you; both faces show a glowing green **0.00**; no crash | 🟥  | 🟥  |
| D2 | Place a few facing different ways | Screen faces the way you were looking (toward you) each time | 🟥  | 🟥  |
| D3 | Wand: click the panel, then the stand | "Linked timer stand to panel (1 stand(s))."; panel readout shows **1 screen(s)** | 🟥  | 🟥  |
| D4 | Start a round on that panel    | Both faces count up live in green, matching the action-bar time | 🟥  | 🟥  |
| D5 | Buzz, then `reveal`            | Digits **freeze** on the stopped time (don't keep counting) | 🟥  | 🟥  |
| D6 | `reset` the panel to Idle      | Stand returns to dim **0.00** | 🟥  | 🟥  |
| D7 | Break the linked stand         | Stand model **and** both digit panels vanish (no leftover floating text); "A linked timer stand was removed."; panel screen count drops | 🟥  | 🟥  |
| D8 | Leave + rejoin the world       | Stand still there, digits still show, **no duplicate** stand/text | 🟥  | 🟥  |

> ⚙️ **First-look tune (expected):** the stand's size, which way it faces, or which side the digits read from are single-number constants in `TimerDisplayVisual.java` (`STAND_BASE_SCALE`, `*_YAW_OFFSET`, `TEXT_*`). If D1/D2 look off (stand backwards, digits facing inward, too big/small), that's a 1-line adjust — tell me what's wrong and I'll dial it.

**D2 · Timer stand resize** — via the wand's **Resize mode** (sneak+right-click air until "Wand mode: Resize")
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| D2a | In Resize mode, right-click a stand (repeat) | Stand grows one notch per click; action-bar shows "Timer stand size: 1.15× …"; stops at max | 🟥  | 🟥  |
| D2b | Right-click **air** (no sneak) to flip to SMALLER, then right-click the stand | Direction flips to **SMALLER**; stand now shrinks each click down to min | 🟥  | 🟥  |
| D2c | Grow it big, then small — try to break it each time | Hitbox tracks the size: big stand = big aim/break area, tiny stand = tiny area | 🟥  | 🟥  |
| D2d | Look at a stand, `/cb buzzergame size small` / `medium` / `large` | Jumps straight to that preset size | 🟥  | 🟥  |
| D2e | `/cb buzzergame size 2.0`      | Sets that exact size | 🟥  | 🟥  |
| D2f | Resize while a round runs      | Digits stay centered on both faces at every size; keep counting | 🟥  | 🟥  |
| D2g | Resize, then leave + rejoin    | Size persists; no duplicate stand/text | 🟥  | 🟥  |

**D3 · Timer stand rotate (5°)** — via the wand's **Rotate mode** (sneak+right-click air until "Wand mode: Rotate")
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| D3a | In Rotate mode, right-click a stand (repeat) | Stand **and** both digit faces turn 5° each click; action-bar "Timer stand angle: N°" | 🟥  | 🟥  |
| D3b | Right-click air (no sneak) to flip to ANTI-CLOCKWISE, then right-click the stand | Turn direction flips; stand now turns the other way, 5°/click | 🟥  | 🟥  |
| D3c | Turn to a diagonal (~45°), read both faces | Front + camera faces both readable at the angle; digits stay centred | 🟥  | 🟥  |
| D3d | Rotate while a round is running | Digits keep counting on both faces at the new angle | 🟥  | 🟥  |
| D3e | Rotate, then leave + rejoin the world | Angle persists; no duplicate stand/text | 🟥  | 🟥  |
| D3f | Rotate a stand placed **before** this build | It reseeds from its facing, then turns normally (no jump to 0°) | 🟥  | 🟥  |

**D4 · LED 7-segment digits** — **default** on new stands; the wand's **Digit-style mode** flips LED ↔ plain text
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| D4a | Place a fresh timer stand      | Digits read as green **7-segment LED** (blocky segments), not the rounded plain font | 🟥  | 🟥  |
| D4b | Wand **Digit-style** mode, right-click the stand | Action bar "Timer digits: **Plain text**"; digits switch to the normal font | 🟥  | 🟥  |
| D4c | Right-click the stand again    | "Timer digits: **LED 7-segment**"; digits switch back to segments | 🟥  | 🟥  |
| D4d | Toggle to LED, then run a round | Both faces count up in LED segments; freeze on buzz, same as plain | 🟥  | 🟥  |
| D4e | Pick a style, then leave + rejoin | Chosen style persists (LED or plain); no duplicate stand/text | 🟥  | 🟥  |
| D4f | View from a **vanilla** client (no CB-B mod installed) | Falls back to plain digits — the LED font ships in the mod jar only (**expected**, not a bug) | ➖  | 🟥  |

> 🧊 **Deferred to polish (not in this build):** the continuous mouse-drag "ghost preview + confirm-to-lock"
> resize. It needs client-side cursor input (scroll/drag), which this feature deliberately avoids to stay
> server-only. The tool's click-to-step + presets do the same job reliably; drag-preview is a later add.

> 🔧 **Superseded 2026-07-07:** the command-UX complaint above is now a full locked redesign, not just a
> backlog note — see **§K** below and the rewritten
> [GROUP_31_BUZZERGAME.md](../groups/GROUP_31_BUZZERGAME.md) (Admin panel — command trim + GUI section).
> `mode`/`game`/`target`/`countdown`/`falsestart`/`state`/`advance` are all being **removed**, not just
> reworked — once §K lands, the E/F test rows below (which still use those verbs) get rewritten to match.

---

# 🗄️ Archive

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

> **C · linking, E · Precision Stop, F · Reveal/Reaction** are now built — see **🎯 Test now** above.
> **2026-07-07 redesign:** §K (admin panel GUI + trimmed commands) and §D (physical stand display,
> replacing the old multiblock-wall plan) are both fully spec'd in
> [GROUP_31_BUZZERGAME.md](../groups/GROUP_31_BUZZERGAME.md) — build order there says **physical display
> first**, then the admin panel GUI. Screen-linking (part of C's original scope) stays hidden until §D's
> first slice (item 1 below) exists.

**D · Physical stand display** (replaces the old multiblock wall — build order items 1-4 in the group
doc, **priority 1**)
- Place the stand display block → renders a compact angled-screen model on a stand, both front and
  back faces show digits
- Faces the direction you were looking when placed; right-click rotates it afterward
- Text-display digit technique works first; a later settings toggle switches to custom baked
  LED-texture digits
- Live resize tool: S/M/L presets + real-time drag-to-scale preview (nothing saved until a final
  confirm click); the click-box scales along with the visual size
- Link with the wand same as buzzers (click panel, then click the display); multiple displays can
  link to one panel and all show the same synced digits
- Breaking a linked display auto-unlinks it from the panel + warns the host, same as a buzzer
- Idle-state settings toggle: default all-zeros vs a custom idle GIF/logo

**K · Admin panel GUI + trimmed commands** (build order items 5-7 in the group doc, priority 2)
- `/cb buzzergame` only offers `give`/`start`/`stop`/`reset`/`reveal`/`help` — `mode`/`game`/`target`/
  `countdown`/`falsestart`/`state`/`advance` all gone
- Start/Stop/Reset/Reveal work as remote commands (no aiming at the panel needed)
- Right-click panel (op) opens a 4-row red/black/lime branded chest screen, title
  `BuzzerGame Panel — <state>` that live-updates
- Start/Stop/Reset/Reveal buttons in the screen match the remote commands' behavior exactly
- Reset on a live round (Countdown/Running/Results) needs click-again-to-confirm; Idle/Finished Reset
  is instant, both in the GUI and via the remote command
- Reveal button always visible, greyed out until Results
- Mode/Format/Countdown/False-start items left-click-cycle through their choices; only apply while Idle,
  greyed + hover-explained otherwise
- Target-time item: left-click cycles presets (3/5/10/15/30/60s), right-click opens a type-exact box
- Status book item shows the full state/mode/rules summary
- Linked-count item reads "Buzzers: N (screens coming later)"; click → auto-numbered list
  (Buzzer #1/#2/...) → click one → click-again-to-confirm → unlinks

**G · DUEL / PARTY multi-buzzer scoring + ranked reveal**
- DUEL requires 2 buzzers linked to start, blocked below that
- PARTY requires 2+ buzzers
- Reveal ranks by closest-to-target (Precision) or fastest (Reaction) — same formula for Duel and Party
- Disqualified (false-started) buzzers are NOT ranked — listed separately at the bottom as "Disqualified"
- Chat shows one line per rank (`1st: ... `, `2nd: ...`); big title shows winner name, subtitle shows 2nd place
- Reveal colors use the CB-B red/black/lime brand, not the old gold/green

**H · Sound / VFX pass**
- Buzzer honk, countdown ticks, win fanfare all play and are each independently toggleable
- Particle burst on press and on reveal, toggleable
- Title/actionbar announcement only reaches players near the game, not server-wide, by default

**I · Scoreboards**
- Session-local scoreboard wipes on restart / reset command
- Global scoreboard persists across restart
- Global scoreboard entry appears in Cloudflare KV via new endpoint after a completed round

**J · Extra digit styles, display frame image/GIF, named presets, name tags, polish**
- Additional digit visual styles (retro LCD / sleek pixel / flip-style) selectable, on top of the one
  default CB-B look
- Display frame accepts a custom photo/logo/GIF via CB-B's image-to-block tool
- Named size/style preset save/load works across sessions
- Floating name tag toggle shows/hides player name above buzzer correctly

</details>

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

**A · Buzzer block + press detection** — ✅ confirmed in-game 2026-07-06 (SP + MP)
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| A1 | Place buzzer block             | Block places, red half-dome model renders, no crash        | ✅  | ✅  |
| A2 | Right-click / press buzzer     | Server log `[CustomBlocks] Buzzer pressed at … by … (id=…)`; dome drops + `[BuzzerGame] Buzz!` action-bar + chime/particles; dome pops back after ~1s | ✅  | ✅  |

**B · Admin panel + PanelSession state machine** — ✅ confirmed in-game 2026-07-06 (SP + MP)
*(command-driven; no chest GUI yet — commands to be revamped later, see Backlog above)*
| #  | Action                        | Expected Result                                         | SP  | MP  |
| -- | ------------------------------ | --------------------------------------------------------- | --- | --- |
| B1 | Place panel, right-click it (op) | Readout: State **Idle**, Mode **Solo**, Linked **0/0**   | ✅  | ✅  |
| B2 | Look at panel, `/cb buzzergame start` | Blocked: needs 1 buzzer(s), 0 linked (no linking yet)     | ✅  | ✅  |
| B3 | `/cb buzzergame advance` ×4      | Walks Idle → Counting down → Running → Results → Finished  | ✅  | ✅  |
| B4 | `mode duel` then `start`; then `reset` | Mode switches (needs 2+); start blocked; reset → Idle     | ✅  | ✅  |
| B5 | Non-op runs a control command    | Denied (op-only)                                          | ✅  | ✅  |
| B6 | Set non-Idle state, reload, right-click | State persisted across reload                            | ✅  | ✅  |

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ------------------ | ------ |

---

