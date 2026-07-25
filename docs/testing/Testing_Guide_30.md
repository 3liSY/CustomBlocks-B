# Group 30 - Guess Mode

## Status

| | |
| --- | --- |
| **Verdict** | All earlier Guess Mode work is confirmed; Placed Mask Mode is now built and needs its first in-game run. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟥🟥 80% |
| **Last tested** | 2026-07-09 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| H | Placed Mask Mode | Built 🎯 | - |
| A | Core guess disguise coverage | Done ✅ | - |
| B | Pose and mega-screen tabs | Done ✅ | - |
| C | QuestionMark, fallback, and custom look | Done ✅ | - |
| D | Guess Showcase display | Done ✅ | - |
| E | Total Blindness physical coverage | Done ✅ | - |
| F/G | Round-control, effects, and future Guess Mode expansions | Planned 📜 | Parked 💤 (out of scope, not counted) |

**Original Group:** [GROUP_30_GUESS_MODE.md](../groups/GROUP_30_GUESS_MODE.md)

---

## H - Placed Mask Mode - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb guess placed` makes blocks the runner places show as QuestionMark to every other player, while the runner keeps seeing the truth. |
| **Pass rule** | Toggle on, toggle off, watcher view, runner view, second runner, look-at HUD, break, and restart rows pass twice. |

💡 Needs two clients: the runner and at least one watcher. A third client is needed for H5.

💡 If the runner builds with Arabic letters, watch H1 while a word re-flows its forms: the mask must stay on those positions and never flash the real letters to the watcher.

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Run `/cb guess placed`, then place a custom block. | Chat confirms the mode is on; the watcher sees QuestionMark where the runner sees the real block. | 🎯 | 🎯 |
| H2 | Place a custom block that was already in the world before the command. | Blocks placed before the mode was turned on stay real for everyone. | 🎯 | 🎯 |
| H3 | Run `/cb guess placed` again. | Chat confirms the mode is off, says how many blocks were revealed, and every block masked by that runner becomes real for everyone. | 🎯 | 🎯 |
| H4 | With the mode on, have the watcher place their own custom block. | The watcher's own placements are never masked. | 🎯 | 🎯 |
| H5 | Have a second player run `/cb guess placed` and place a block. | Each runner sees only their own placements real and sees the other runner's placements as QuestionMark. | 🎯 | 🎯 |
| H6 | As the watcher, look at a masked block and read the HUD. | Look-at name and HUD show QuestionMark, not the real block name. | 🎯 | 🎯 |
| H7 | As the watcher, break a masked block, then pick the drop up. | Break particles, break sound, and the item on the ground all show QuestionMark; once it is in the inventory it is a normal stack with its real name. | 🎯 | 🎯 |
| H8 | Restart the server, then rejoin as runner and watcher. | Masked positions and the runner's mode state survive the restart. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

### A - Core guess disguise coverage - ✅ `2026-07-09`

| | |
| --- | --- |
| **Check** | Guess Mode hides selected custom blocks from the flagged player while watchers still see the truth. |
| **Pass rule** | On one id, on all, off one id, bare off, held, offhand, placed, inventory, hotbar, tooltip, item frame, watcher view, and persistence rows pass twice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb guess <player> on <blockid>`. | That block type is disguised for the target player. | ✅ | ✅ |
| A2 | Run `/cb guess <player> on all`. | Every custom block is disguised for the target player. | ✅ | ✅ |
| A3 | Run `/cb guess <player> off <blockid>`. | Only that block id stops being disguised. | ✅ | ✅ |
| A4 | Run `/cb guess <player> off`. | All guess state clears for that player. | ✅ | ✅ |
| A5 | Hold the disguised block in main hand and offhand. | Name, icon, tooltip, and held render show the mystery state. | ✅ | ✅ |
| A6 | Look at a placed disguised block. | HUD/name/render are disguised for the guesser. | ✅ | ✅ |
| A7 | Put the disguised block in an item frame or inventory. | It remains disguised for the guesser. | ✅ | ✅ |
| A8 | Have a watcher view the same block/player. | Watcher sees the real block and real name. | ✅ | ✅ |
| A9 | Restart or relog while flagged. | Guess state persists as designed. | ✅ | ✅ |

### B - Pose and mega-screen tabs - ✅ `2026-07-08`

| | |
| --- | --- |
| **Check** | Guess Mode pose tuning and the unified `/cb guess` mega-screen work without command/screen conflict. |
| **Pass rule** | Pose tab, arm sliders, shared pose sync, removed pose command, tab bar, placeholder tabs, Showcase tab entry, and slider controls pass twice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Open `/cb guess` or settings route. | Mega-screen opens with persistent tabs. | ✅ | ✅ |
| B2 | Use Pose tab sliders. | Left/right arm pitch, yaw, and roll update the live dummy and real flagged holder. | ✅ | ✅ |
| B3 | Save pose values. | One shared pose applies to all flagged holders. | ✅ | ✅ |
| B4 | Try removed `/cb guess pose` command. | Command surface is gone; screen is the only pose editor. | ✅ | ✅ |
| B5 | Click Buzz, Sound, Look, and Showcase tabs. | Built tabs work; unavailable tabs show clear coming-soon state. | ✅ | ✅ |
| B6 | Use number-entry sliders and reset/default behavior. | G27 shared slider chrome works for Guess Mode tabs. | ✅ | ✅ |

### C - QuestionMark, fallback, and custom look - ✅ `2026-07-09`

| | |
| --- | --- |
| **Check** | Guess disguise look uses existing block ids and falls back to the bundled QuestionMark cube safely. |
| **Pass rule** | Bundled fallback, high-res black/red texture, default block, per-block look override, deleted look fallback, no link commands, and animated look rows pass twice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Use guess mode with no default or override. | Bundled QuestionMark cube appears, not a missing texture. | ✅ | ✅ |
| C2 | Inspect the fallback. | It is sharp red on true black and not the old blurry grey/purple asset. | ✅ | ✅ |
| C3 | Run `/cb guess defaultblock <blockid>`. | Existing block id becomes persistent default look. | ✅ | ✅ |
| C4 | Run `/cb guess <player> on <blockid> look <lookid>`. | Per-round look override appears for the guesser only. | ✅ | ✅ |
| C5 | Delete the look block or use stale reference. | Fallback chain returns to QuestionMark safely. | ✅ | ✅ |
| C6 | Try old link-based look commands. | Link input is gone and cannot create guess-only image rails. | ✅ | ✅ |

### D - Guess Showcase display - ✅ `2026-07-09`

| | |
| --- | --- |
| **Check** | Guess Showcase is a placed display block with tunable end-crystal-style visuals. |
| **Pass rule** | Spawn with id, spawn without id, placement, multiple showcases, shift-right-click delete, command delete, persistence, tuning, particles, glow, and tab controls pass twice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb guess showcase spawn <blockid>`. | Showcase spawns one block above the player and shows that block. | ✅ | ✅ |
| D2 | Run `/cb guess showcase spawn` with no id. | Chat says no block specified and uses QuestionMark fallback. | ✅ | ✅ |
| D3 | Spawn multiple showcases. | Each persists and is removed independently. | ✅ | ✅ |
| D4 | Shift-right-click one showcase as OP. | That exact showcase deletes. | ✅ | ✅ |
| D5 | Use `/cb guess showcase delete`. | Targeted showcase deletes through command path. | ✅ | ✅ |
| D6 | Adjust Showcase tab sliders/toggles. | Inner spin, outer orbit, size, glow, and particles update and sync. | ✅ | ✅ |
| D7 | Restart server. | Showcase block/entity persists. | ✅ | ✅ |

### E - Total Blindness physical coverage - ✅ `2026-07-09`

| | |
| --- | --- |
| **Check** | The guesser cannot infer identity through physical/render leaks that are in current scope. |
| **Pass rule** | Hitbox, collision, mining speed, particles, break/place visibility, dropped item, F5, and watcher separation rows pass twice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Touch or stand on a disguised block with unusual shape/collision. | Guesser experiences the mystery cube behavior where implemented. | ✅ | ✅ |
| E2 | Mine/break a disguised block. | Mining feel and break particles do not reveal the true block within current scope. | ✅ | ✅ |
| E3 | Place and break while flagged. | The guesser continues seeing the disguise through placement/break flow. | ✅ | ✅ |
| E4 | Drop/pick up the item as the same flagged player. | It stays disguised until guess state clears. | ✅ | ✅ |
| E5 | Switch to F5/third person. | Guesser view does not reveal the real held block. | ✅ | ✅ |
| E6 | Watch from another client. | Watcher still sees the real block. | ✅ | ✅ |

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §F/G Round-control keybinds/effects and future Guess Mode expansions (impostor round, other variants) — 💤 `2026-07-07`: Combined, out of scope, need separate future design pass.
- §E Future glow/light spoofing — 💤 `2026-07-09`: Parked as a deeper future concern, not an active bug.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Clear guess state with `/cb guess <player> off`.
- [ ] Delete temporary look/test blocks.
- [ ] Remove temporary showcases.
- [ ] Toggle `/cb guess placed` off for every runner so no masked blocks are left behind.
- [ ] Keep parked round-control ideas grouped together.

</details>
