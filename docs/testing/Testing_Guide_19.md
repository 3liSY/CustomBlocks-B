# Group 19 - Showcase & Hologram Display Systems

## Status

| | |
| --- | --- |
| **Verdict** | Showcase and hologram systems are fully designed, but implementation still needs to start with the core slice. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-07-10 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Showcase core | Designed ⏳ | - |
| B | Showcase config Screen | Designed ⏳ | - |
| C | Multi-block, vanilla, animated, and placer support | Designed ⏳ | - |
| D | Presets and performance cap | Designed ⏳ | - |
| E | Showcase management, bulk, and groups | Designed ⏳ | - |
| F | Hologram preview | Designed ⏳ | - |
| G | Offhand hologram projection | Designed ⏳ | - |
| H | Parked display extras | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_19_DISPLAY.md](../groups/GROUP_19_DISPLAY.md)

---

# Active Tests

## 💡 Setup

- Create at least two custom blocks: `g19a` and `g19b`.
- Use OP account for all showcase placement, removal, editing, and bulk work.
- Use MC 1.21.1 Display Entities behavior as the baseline.
- Use G27 for Screen conventions; G19 owns display behavior and data.
- Confirm each slice before moving to the next.

## A - Showcase core - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb showcase <id>` creates a persistent real floating block display with safe placement and removal. |
| **Pass rule** | Placement, sky fallback, static/spin, pedestal/floating, scale, persistence, respawn, remove, OP gate, and storage rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | No showcase code exists yet. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb showcase g19a` while looking at a block surface. | A `BlockDisplay` showcase appears on that surface with an auto instance id. | ⏳ | ⏳ |
| A2 | Look at open sky and run `/cb showcase g19a`. | Showcase spawns about two blocks in front of the player, never as an orphan or error. | ⏳ | ⏳ |
| A3 | Switch between pedestal and floating types. | Pedestal shows a stand; floating has no base. | ⏳ | ⏳ |
| A4 | Set rotation to smooth spin, then static. | Spin interpolates smoothly; static stops completely. | ⏳ | ⏳ |
| A5 | Set scale from 0.5x to about 4x. | Showcase scales cleanly without broken hit/selection behavior. | ⏳ | ⏳ |
| A6 | Restart server and revisit the chunk. | Showcase respawns from `display_blocks.json` with the same config. | ⏳ | ⏳ |
| A7 | Run `/cb showcase remove` while looking at it. | The targeted showcase is removed from world and storage. | ⏳ | ⏳ |
| A8 | Try create/remove as a non-OP account. | Commands are denied cleanly and nothing changes. | ⏳ | ⏳ |

## B - Showcase config Screen - Designed ⏳

| | |
| --- | --- |
| **Check** | Shift-right-click or command opens a Screen with Appearance, Motion, and Display controls. |
| **Pass rule** | Open, tab navigation, knobs, label, aura, in-screen remove, looked-at targeting, and save rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Depends on the G27 Screen system. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Shift-right-click a showcase. | Config Screen opens for the looked-at showcase. | ⏳ | ⏳ |
| B2 | Run `/cb showcase config <id>`. | Same Screen opens by instance id. | ⏳ | ⏳ |
| B3 | Use Appearance controls. | Glow outline, fullbright, hover bob, scale, and particle aura visibly apply. | ⏳ | ⏳ |
| B4 | Use Motion controls. | Static/spin and speed slider update immediately and persist. | ⏳ | ⏳ |
| B5 | Use Display controls. | Label can auto-use block name or custom text and can show through walls if enabled. | ⏳ | ⏳ |
| B6 | Use the in-screen remove button. | It removes the showcase only after an intentional action. | ⏳ | ⏳ |
| B7 | Close and reopen the Screen. | Settings remain saved and match the world display. | ⏳ | ⏳ |

## C - Multi-block, vanilla, animated, and placer support - Designed ⏳

| | |
| --- | --- |
| **Check** | A showcase can display multiple content types and cycle them without abusing dropped-item visuals. |
| **Pass rule** | Multi-list, right-click cycle, auto-cycle, all-custom dynamic, vanilla block, vanilla item, animated item path, and placer rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Animated custom blocks need the item-render path, not `BlockDisplay`. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Add `g19a` and `g19b` to one showcase. | Right-click cycles to the next content entry. | ⏳ | ⏳ |
| C2 | Enable auto-cycle at 0.5s and 0.1s. | Showcase cycles at the configured interval without desyncing. | ⏳ | ⏳ |
| C3 | Set content to all custom blocks. | Existing custom blocks appear, and newly created blocks join dynamically. | ⏳ | ⏳ |
| C4 | Create a showcase for a vanilla block. | It uses `BlockDisplay` and looks like the real blockstate. | ⏳ | ⏳ |
| C5 | Create a showcase for `diamond_sword`. | It uses `ItemDisplay` and does not pretend to be a block. | ⏳ | ⏳ |
| C6 | Add an animated custom block. | It displays through the item-render path so animation can work. | ⏳ | ⏳ |
| C7 | Run `/cb showcase item` or grab the placer from Tools tab. | Placer item creates a showcase like furniture placement. | ⏳ | ⏳ |

## D - Presets and performance cap - Designed ⏳

| | |
| --- | --- |
| **Check** | Showcases have reusable presets, one server default, and a sensible cap to protect performance. |
| **Pass rule** | Save, apply, default, cap warning, cap config, auto-cycle load, and particle load rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Requires the core and config Screen slices first. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Save the current showcase look as a named preset. | Preset appears in the library. | ⏳ | ⏳ |
| D2 | Apply that preset to a different showcase. | The second showcase matches the saved look. | ⏳ | ⏳ |
| D3 | Set a preset as server default. | New showcases start with that default. | ⏳ | ⏳ |
| D4 | Exceed the per-chunk showcase cap. | OP receives a clear warning and the cap behavior matches config. | ⏳ | ⏳ |
| D5 | Use many auto-cycling or particle-heavy showcases. | Cap/load warning accounts for cycling, particles, and animated content. | ⏳ | ⏳ |

## E - Showcase management, bulk, and groups - Designed ⏳

| | |
| --- | --- |
| **Check** | OPs can find, rename, duplicate, move, bulk-edit, and group showcases safely. |
| **Pass rule** | List, teleport, locate, nearest, rename, clone, move, bulk preset, bulk remove, hide/show, lock, group, and export rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Requires stored instance ids and stable targeting. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Run `/cb showcase list`. | List shows instance ids and clickable teleport entries. | ⏳ | ⏳ |
| E2 | Use locate nearby. | Nearby showcases glow-highlight without changing config. | ⏳ | ⏳ |
| E3 | Edit nearest. | The closest showcase opens for editing without needing an id. | ⏳ | ⏳ |
| E4 | Rename and clone one showcase. | Clone copies config while getting a new instance id. | ⏳ | ⏳ |
| E5 | Move/reposition a showcase. | Display and stored position update together. | ⏳ | ⏳ |
| E6 | Apply a preset to all showcases in a radius. | All matching showcases update and report count. | ⏳ | ⏳ |
| E7 | Remove all showcases in a radius. | Removal is intentional, counted, and undo/report friendly. | ⏳ | ⏳ |
| E8 | Hide/show and lock one showcase. | Hidden stays saved; locked blocks edits until unlocked. | ⏳ | ⏳ |
| E9 | Put showcases in a named group and change group settings. | Group members sync the chosen setting. | ⏳ | ⏳ |
| E10 | Export/share one showcase config. | A shareable code recreates the same config elsewhere. | ⏳ | ⏳ |

## F - Hologram preview - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb preview <id|url>` shows temporary or pinned holograms without creating a saved showcase by accident. |
| **Pass rule** | Id, URL, bad URL, timed despawn, distance despawn, pin, unpin, to-player, compare, config, and visibility rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | URL previews require a safe temporary download/render path. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb preview g19a`. | Temporary spinning preview appears quickly and disappears after 10 seconds. | ⏳ | ⏳ |
| F2 | Walk more than 5 blocks away. | Preview disappears early. | ⏳ | ⏳ |
| F3 | Run `/cb preview <image-url>`. | Temp texture downloads/renders and shows as a preview, without creating a permanent block. | ⏳ | ⏳ |
| F4 | Use a bad URL. | Clear error appears and no broken preview remains. | ⏳ | ⏳ |
| F5 | Use `/cb preview pin` and `/cb preview unpin`. | Preview becomes permanent-baseless, then removes cleanly. | ⏳ | ⏳ |
| F6 | Send preview to another player. | Target player sees it; other routing is clear. | ⏳ | ⏳ |
| F7 | Compare two previews side by side. | Both previews appear aligned and labeled enough to compare. | ⏳ | ⏳ |
| F8 | Change `hologramHeight` and `hologramColor`. | Preview height/color follow config. | ⏳ | ⏳ |

## G - Offhand hologram projection - Designed ⏳

| | |
| --- | --- |
| **Check** | Holding a custom block in offhand projects a visible hologram above the player and clears instantly when removed. |
| **Pass rule** | Enable config, hold, follow, other-player visibility, swap, clear, dimension change, and performance rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Last slice after showcase and preview behavior is stable. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Enable offhand holograms and hold `g19a` in offhand. | Hologram appears about 1.5 blocks above the player. | ⏳ | ⏳ |
| G2 | Walk, jump, and turn. | Hologram follows smoothly without leaving duplicates. | ⏳ | ⏳ |
| G3 | Have another player watch. | Other player sees the hologram correctly. | ⏳ | ⏳ |
| G4 | Swap to a different custom block. | Hologram updates to the new block. | ⏳ | ⏳ |
| G5 | Empty offhand. | Hologram disappears immediately. | ⏳ | ⏳ |
| G6 | Change dimension or disconnect. | Old projection cleans up. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §H Shop mode — 💤 `2026-06-21`: Later economy/admin-give discussion.
- §H Redstone control — 💤 `2026-06-21`: Later behavior layer.
- §H Proximity reactions — 💤 `2026-06-21`: Later polish after core performance is known.
- §H Glass case and open shelf types — 💤 `2026-06-21`: Pedestal and floating are first.
- §H Per-showcase glow color — 💤 `2026-06-21`: Default glow color first.
- §H Tilt, tumble, and orbit-all-at-once — 💤 `2026-06-21`: Spin/static and single cycling first.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove any showcase instances created during tests.
- [ ] Delete `g19a` and `g19b` after testing.
- [ ] Clear pinned previews.
- [ ] Remove temporary display data if storage tests fail mid-run.

</details>
