# Group 30 — Guess Mode

**🎯 Active:** §S closed out; §R slice 1 (R1-R3) all passed — only §R-2 (sound stub + mining-speed) left open  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                                                                          |
| --------------- | ---------------------------------------------------------------------------------------- |
| **Verdict**     | ✅ Core A–Q (89/89). ✅ §S Showcase passed, closed. ✅ §R slice 1 (R1/R2/R3) passed — R3 moved to G06 §G06-7. Only §R-2 (sound + speed) left open. |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅⏳⏳ |
| **Last tested** | 2026-07-09 MP (S1/S5/S6/S7/S8/S13/R1/R2/R3 ✅ all passed) |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ Screen content moved 2026-07-12: GuessSettingsScreen's shared slider/preset/dummy-preview chrome →
> `GROUP_27_SCREENS.md` §G27.26. G30 keeps round-state/buzzer/showcase game logic and each tab's own content.

## 🗺️ Sections

| § | What                                                                | State |
| --- | ------------------------------------------------------------------- | ----- |
| S | Showcase — floating end-crystal display (spawn/delete + live tuning tab) | ✅ passed, closed |
| Q | Pose-tab sliders — number entry · scroll/↑↓ nudge · right-click reset | ✅     |
| N | Guess Settings screen — tabs + pose control (rebuilt)               | ✅     |
| P | Fallback "?" disguise texture (regenerated sharp + black)           | ✅     |
| A | Centered two-handed pose (seen by everyone)                         | ✅     |
| B | `/cb guess` command tree + persist + op-gate                        | ✅     |
| C | Blank name "???" (holder only)                                      | ✅     |
| D | Disguise "?" — icon / inventory / hand / frame / F5                 | ✅     |
| E | All-mode (`on all`)                                                 | ✅     |
| F | Placement while disguised                                           | ✅     |
| G | Death / drop keeps the disguise                                     | ✅     |
| H | Stacking ids + partial clear                                        | ✅     |
| I | Custom disguise look (`defaultblock` + per-round `look` + fallback) | ✅     |
| K | Smooth pickup / put-down transition                                 | ✅     |
| M | Guess Settings screen — Pose tab (sliders + live dummy + presets)   | ✅     |
| ⏳ | Round-control pass (keybinds / buzz / sound)                        | ⏳ parked |
| R | Total Blindness (Round 2 — hitboxes, particles, sounds, speed)      | ✅ slice 1 passed (R1/R2/R3); §R-2 sound/speed ⏳ |

🔗 **Related Doc:** [GROUP_30_GUESS_MODE.md](../groups/GROUP_30_GUESS_MODE.md)

---

# 🎯 Test now

### 🧰 Setup
* Op on a **solo world (F5)** is enough for most rows; a **2nd player** for the "applies to everyone" rows.
* Make a disguise block: `/cb create guessme <image>`; give it to the target: `/cb give guessme 1 <target>`.
* Flag the target: `/cb guess <target> on guessme`. Open the screen: `/cb guess settings` (op only), Pose tab.

**R · Total Blindness — §R-2 (sound + mining-speed)** *(slice 1 — R1/R2/R3 — passed 2026-07-09, see Archive; this is the only open row left)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| R4 | *(follow-up, not built)* Mine / break a flagged block — **sound** | Break/dig sound becomes a generic **"?" stub**, not the real material | ⏳ | ⏳ |
| R5 | *(follow-up, not built)* **Mining speed** of a flagged block | Pending a ruling — a client-only speed spoof desyncs with the server (see doc) | ⏳ | ⏳ |

> 💡 **R-2 scope grew 2026-07-09:** owner wants R2's fix taken further — add the sound stub above **and** a touch of randomness/motion to the "?" particle itself (right now it's one fixed particle every time; a little variance so it doesn't look too uniform).

---

# 🗄️ Archive

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

Full specs in [GROUP_30_GUESS_MODE.md](../groups/GROUP_30_GUESS_MODE.md).

* **Total Blindness (Round 2)** — slice 1 ✅ **passed** (§R, see Passed Tests History): selection outline forced to a full cube; break/mining particles suppressed for the holder; drops stay disguised. Follow-up (§R-2, ⏳, scope grew 2026-07-09): break/dig sound → `customblocks:mystery_break` stub (vanilla amethyst) **+ slight particle variance on the "?" debris**; mining-speed pending a desync ruling.
* **Round-control pass (parked together)** — manual keybinds (wrong-buzz / correct-reveal / force-end), cursed-buzz effect, stage-zone shockwave, sound-swap. Built as one connected system later.
* **Cut for good** — hint ladder, round timer / timeout-TNT / boss-tension.
* **Later polish** — first-person two-handed pose; per-face-picture blocks keep their real hotbar icon.
* **Own future session** — Impostor round (spot-the-odd-one-out).

</details>

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

Core A–H (v2) + custom look §I + smooth transition §K confirmed in-game **2026-07-06** (singleplayer + 2-player). Guess Settings Pose tab §M confirmed **2026-07-07**. Mega-screen rebuild §N + fallback "?" texture §P confirmed **2026-07-08**. Command shape: `on <id>` (stacks), `on all`, `off <id>`, `off all`, bare `off` (clears all). Name is always "???"; look resolves per-round override → global default → bundled "?".

**S · Showcase — floating end-crystal display** *(passed + closed 2026-07-09, MP; SP spot-checked)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| S6 | Open the **Showcase** tab | **No Glow toggle exists** anywhere — the tab has only Core spin · Size · Reset; the display always uses normal world light | 🟡 | ✅ |
| S1 | `/cb guess showcase spawn guessme` (op) | A floating display spawns **one block above your feet**: a **single** spinning picture core, gently bobbing — **no** outer ghost layer | 🟡 | ✅ |
| S8 | **Left-click / mine** the display, then **sneak + right-click** it | Left-click does **nothing** (can't be destroyed); sneak+right-click removes it ("Showcase removed") | 🟡 | ✅ |
| S13 | Open the **Showcase** tab | Shows a **live in-screen spinning "?" preview** that reacts live to Core spin / Size (like the Pose dummy) | 🟡 | ✅ |
| S5 | Showcase tab → drag **Core spin / Size** | On release the world display updates **live**; only **two** sliders (outer-orbit + Glow both gone) | 🟡 | ✅ |
| S7 | Open the Showcase tab; mine near a display | **No** Particles toggle and **no** end-rod particles anywhere (both removed) | 🟡 | ✅ |
| S2 | `/cb guess showcase spawn` (no id) | Chat says **"no block specified"**; the display shows the bundled **"?"** cube | 🟥 | ✅ |
| S3 | A **2nd player** looks at the display | Sees the **same** spinning display (it's not holder-only) | ➖ | ✅ |
| S4 | Walk into the display | You **pass through** — no collision | 🟥 | ✅ |
| S9 | `/cb guess showcase delete` (looking at / standing near one) | The targeted / nearest Showcase is removed | 🟥 | ✅ |
| S10 | Spawn **two** showcases, sneak+right-click just one | Only **that** one is removed; the other stays | 🟥 | ✅ |
| S11 | A **non-op** runs `/cb guess showcase spawn` | **Refused** (op-gated) | 🟥 | ✅ |
| S12 | Spawn one, then **reload the world / relog** | The Showcase **persists** (it's a real placed block) | 🟥 | ✅ |

> 💡 **S1/S5:** the outer orbit layer + its slider were removed — one core, two sliders (Core spin · Size).
> 💡 **S6:** the Glow toggle was scrapped end-to-end (screen, sync, store, renderer) — there is nothing left to toggle.
> 💡 **S8:** left-click / mining a Showcase is cancelled server-side; only shift-RC or the command removes it.

**R · Total Blindness (slice 1) — hitbox + particles** *(passed 2026-07-09; ⚠️ R3 is a general SlotBlock behavior, not Guess-Mode-specific — moved to [Group 06 §G06-7](GROUP_06_TESTING_GUIDE.md), kept here only as the historical pass record)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| R1 | Flag a **shaped** (non-full-cube) block; the target aims at a placed one | The selection **outline is a full 1×1 cube**, not the block's real shape | 🟡 | ✅ |
| R2 | The target **mines / breaks** a flagged placed block | **"?"-textured** debris flies (never the real texture); a 2nd player still sees the **real** particles | 🟡 | ✅ |
| R3 | Break **any** custom block (flagged or not) in **survival** | It **drops its own item**; for a flagged holder the dropped item still shows the **"?"** cube. Creative = no drop (vanilla) — *(moved to G06 §G06-7, general block behavior)* | 🟡 | ✅ |

> 💡 **R:** hitbox outline + break/mining particles are plugged this slice; sound stub + particle variance are the next micro-slice (§R-2, above).
> 💡 **R2:** the holder's real break particles are swapped for a bundled `mystery_break` "?" particle, so debris shows without leaking.
> 💡 **R3:** custom blocks had no loot table, so they dropped nothing; they now drop their own item on a survival break — this is Group 06's SELF-default drop mode (§G06-7), confirmed here first.

**Q · Pose-tab sliders — number entry + nudge + reset** *(2026-07-08)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| Q1 | **Click the value number** to the right of a slider | Turns into an **editable field** (accent border + caret) | ✅ | ✅ |
| Q2 | Type a value (e.g. `-45.5`) and press **Enter** | Slider + dummy jump to it; flagged target's real pose updates for everyone | ✅ | ✅ |
| Q3 | Type an **out-of-range** value (`999`) → Enter | Silently **clamped** to the slider's max — no error flash | ✅ | ✅ |
| Q4 | With a value field focused, press **↑ / ↓** | Nudges the value **±1°** live | ✅ | ✅ |
| Q5 | **Scroll** the wheel over a slider (or its value) | Nudges **±1°** and applies live | ✅ | ✅ |
| Q6 | **Right-click** one slider | Only **that axis** resets to default; others unchanged | ✅ | ✅ |
| Q7 | Start typing, then press **Esc** | Edit **cancels**, value unchanged, screen stays open | ✅ | ✅ |
| Q8 | Start typing, then **click another slider / close** | The typed value **commits** (isn't lost) | ✅ | ✅ |

**N · Guess Settings screen — tabs + pose control** *(rebuilt into the tabbed mega-screen; `/cb guess pose` command removed)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| N1 | `/cb guess settings` (op) | Opens on the **Pose** tab; the tab bar reads **Pose · Buzz · Sound · Look · Showcase** | ✅ | ✅ |
| N2 | **Click** a greyed tab (Buzz / Sound / Look / Showcase) | Shows a **"coming soon"** line; stays on the Pose tab | ✅ | ✅ |
| N3 | **Hover** a greyed tab | A small **"coming soon"** tooltip shows by the cursor | ✅ | ✅ |
| N4 | Drag an arm slider (flagged target in the world) | Dummy arm moves live; on **release** the target's real pose updates for everyone | ✅ | ✅ |
| N5 | Click a preset (**Cradle / Present / Low**) or **Reset** | Sliders + dummy + everyone's real pose jump to it | ✅ | ✅ |
| N6 | Type `/cb guess pose ...` in chat | **Command no longer exists** — pose is screen-only now | ✅ | ✅ |

**P · Fallback "?" disguise texture** *(regenerated 512×512 sharp red on true black; old purple removed)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| P1 | Flag a block with **no default look set** → hold it | The "?" disguise is a **sharp red mark on a solid black cube** — not blurry, not grey | ✅ | ✅ |
| P2 | `/cb guess defaultblock clear`, then re-flag and hold | Shows the **"?" cube** — never the old purple texture | ✅ | ✅ |

**A · Centered two-handed pose (seen by everyone)**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb guess <target> on guessme`; target holds it; a 2nd player looks | Both hands meet at the **centre** of the chest | ➖ | ✅ |
| A2 | Check your OWN body in **F5** while flagged + holding | Same centered two-handed pose | ✅ | ✅ |
| A3 | Hold a non-flagged item, or run `off` | Arms return to normal **live** (no relog) | ✅ | ✅ |

**B · Command tree + persistence + op-gate**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | `/cb guess <target> on guessme` | Chat confirms; takes effect immediately | ✅ | ✅ |
| B2 | `/cb guess <target>` (no sub-arg) | Prints status: `ON · all=no · blocks=[guessme]` | ✅ | ✅ |
| B3 | Target **relogs** while flagged | Still in guess mode after reconnect | ➖ | ✅ |
| B4 | `/cb guess <target> off` (bare) | Chat confirms; blinding + pose stop live | ✅ | ✅ |
| B5 | A **non-op** runs the command | Refused | ✅ | ✅ |

**C · Blank name — holder only**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Target holds `guessme` → hotbar name + tooltip | Both read **"???"** | ✅ | ✅ |
| C2 | A 2nd player holds / looks at the same block | Sees the **real** name | ➖ | ✅ |
| C3 | Target also holds a different, non-flagged block | Only `guessme` reads "???" | ✅ | ✅ |
| C4 | `/cb guess <target> on guess2`, hold each | **Both** read "???" — ids stack | ✅ | ✅ |

**D · Disguise "?" — icon / inventory / hand**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Target holds `guessme` — hotbar icon + first-person hand | Both show the **"?"** cube | ✅ | ✅ |
| D2 | Open inventory | The flagged block's grid icon also shows the **"?"** cube | ✅ | ✅ |
| D3 | Put it in an **item frame**, or view as a **dropped item** | Shows the **"?"** cube to the holder | ✅ | ✅ |
| D4 | Target in **F5** looks at their own hand | Also shows the **"?"** cube | ✅ | ✅ |
| D5 | 2nd player watches the target's hand | Sees the **real** block | ➖ | ✅ |

**E · All-mode (`on all`)**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | `on all`; target looks at a placed custom block → HUD | HUD id/name show **"???"** | ✅ | ✅ |
| E2 | Open inventory with several custom blocks | **Every** custom block shows "???" + "?" | ✅ | ✅ |
| E3 | `off all` (keeps specific ids) | All-mode blocks real again; specific ids stay disguised | ✅ | ✅ |

**F · Placement while disguised**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Target **places** `guessme` | Placed block shows the **"?"** cube to the target | ✅ | ✅ |
| F2 | 2nd player looks at that block | Sees the **real** block | ➖ | ✅ |

**G · Death / drop keeps the disguise**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Target dies / drops the block → look at the drop | Still shows the **"?"** to the target; 2nd player sees real | ✅ | ✅ |
| G2 | Target picks it back up | Disguised again in hand | ✅ | ✅ |

**H · Stacking + partial clear**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | `on guessme` then `on guess2`; run status | Lists **both**; both disguise | ✅ | ✅ |
| H2 | `off guessme` (leave guess2); then bare `off` | After first: only guess2 disguised; after bare: everything real | ✅ | ✅ |

**I · Custom disguise look** *(look resolves per-round override → global default → bundled "?")*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| I1 | `/cb guess defaultblock lookblock`; then flag the target | Target sees `guessme` wearing **lookblock's picture** | ✅ | ✅ |
| I2 | `on guessme look otherblock` | Sees `guessme` as **otherblock**, overriding the default | ✅ | ✅ |
| I3 | With default + override both set, check the held block | Shows **otherblock** (override beats default) | ✅ | ✅ |
| I4 | `defaultblock clear`; `off`; then flag again (no look) | Sees the **bundled "?" cube** | ✅ | ✅ |
| I5 | Use an **animated GIF** block as the look | The disguise cube **plays the GIF live** | ✅ | ✅ |
| I6 | `/cb guess <target>` status after I2 | Prints `blocks=[guessme→otherblock]` and `default look=…` | ✅ | ✅ |
| I7 | **Delete** the look block while in use | Falls back gracefully (→ override, else "?") — no crash | ✅ | ✅ |
| I8 | Set default + per-round look, then target **relogs** | Both survive the relog | ➖ | ✅ |
| I9 | 2nd player looks at the disguised target | Always sees the **real** `guessme` — look swap is holder-only | ➖ | ✅ |
| I10 | `/cb reid lookblock newlook` | Default/override auto-follow the rename; no fallback to "?" | ✅ | ✅ |

**K · Smooth pickup / put-down transition**
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| K1 | Flagged target equips the flagged block | Arms **glide** into the centre hold over ~0.3s (no snap) | ✅ | ✅ |
| K2 | Switch to a non-flagged item (or run `off`) | Arms **glide back** to normal (no pop) | ✅ | ✅ |
| K3 | Rapidly swap the flagged block in/out | The glide **reverses smoothly** mid-transition | ✅ | ✅ |

**M · Guess Settings screen — Pose tab** *(2026-07-07; re-confirm via §N after the mega-screen rebuild)*
| #  | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| M1 | `/cb guess settings` (op) | Opens on the Pose tab: six sliders left, live dummy right | ✅ | ✅ |
| M2 | Drag **Right arm — up/down** | The dummy's right arm moves live | ✅ | ✅ |
| M3 | Release the slider (flagged target present) | The target's real pose updates (saved + synced) | ✅ | ✅ |
| M4 | Click a preset | Sliders + dummy + real pose jump to it | ✅ | ✅ |
| M5 | Click **Reset** | Back to the default centered hold | ✅ | ✅ |
| M6 | Drag a **left**-arm slider only | Only the left arm changes — arms stay independent | ✅ | ✅ |
| M7 | Close with **Done** / Esc | Preview stops; body uses the saved pose | ✅ | ✅ |
| M8 | A **non-op** runs `/cb guess settings` | Refused (op-gated server-side) | ✅ | ✅ |

</details>

<details><summary>🗄️ <b>Test history</b> (click to open)</summary>

* **2026-07-08:** mega-screen rebuild + pose-command removal (N1–N6), sharp/black fallback "?" texture (P1–P2), and pose-slider number entry (Q1–Q8) all confirmed in-game (SP + MP). Core A–Q = 89/89. Showcase §S in progress.
* **2026-07-07:** Guess Settings Pose tab (M1–M8) confirmed in-game.
* **2026-07-06:** all core A–H (SP + MP), custom look §I, and smooth transition §K confirmed. Group core 65/65.
* **2026-07-05:** first MP pass — command tree + name-blank worked; pose and "?" look fixed after.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

