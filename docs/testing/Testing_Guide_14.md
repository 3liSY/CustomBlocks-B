# Group 14 - Animation, Video & Display Blocks

## Status

| | |
| --- | --- |
| **Verdict** | Basic animated blocks are confirmed; GIF retexture safety, render quality, source recovery, and display-block features remain open. |
| **Progress** | 🟩🟩🟥🟥🟥🟥🟥🟥🟥🟥 20% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| E | Source Wall review workflow | Built 🎯 | Discussion ✏️ |
| C | Animation-aware retexture rail | Designed ⏳ | - |
| D | Off-atlas quality and speed renderer rewrite | Designed ⏳ | - |
| F | Cutout render layer for G10 background attribute | Designed ⏳ | - |
| G | Showcase / cycler block | Designed ⏳ | - |
| A | Animated block basics | Done ✅ | - |
| H | Universal video import | Planned 📜 | Parked 💤 |
| I | Redstone-reactive and live-data display blocks | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_14_ANIMATION_VIDEO.md](../groups/GROUP_14_ANIMATION_VIDEO.md)

---

# Active Tests

## 💡 Setup

- Use one static image block and one GIF/WebP animated block.
- Test retexture on both SP and a dedicated server.
- Keep G27 screen/command routing issues in TG27; this guide tests engine/data/render behavior.

## C - Animation-aware retexture rail - Designed ⏳

| | |
| --- | --- |
| **Check** | Retexturing with a GIF/WebP must update texture and `AnimData` together so blocks never render scrambled or stale. |
| **Pass rule** | Static-to-GIF, GIF-to-static, GIF-to-GIF, bad source, restart, and MP viewer rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Rows become runnable after `applyTexture` mirrors the create path for animated sources. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Retexture a static block with a GIF URL. | Block becomes animated with correct frames, speed, and no scrambled grid. | 🎯 | 🎯 |
| C2 | Retexture an animated block with a static PNG/JPG. | Animation data clears and block becomes a normal static texture. | 🎯 | 🎯 |
| C3 | Retexture an animated block with a different GIF/WebP. | Texture grid, frame count, timing, and playback data all match the new source. | 🎯 | 🎯 |
| C4 | Retexture with a bad or unsupported source. | Old texture/animation remains intact and the error is human-readable. | 🎯 | 🎯 |
| C5 | Restart after each conversion. | Saved texture and animation state reload correctly. | 🎯 | 🎯 |

## D - Off-atlas quality and speed renderer rewrite - Designed ⏳

| | |
| --- | --- |
| **Check** | Animated placed blocks should render at source quality and speed without atlas muffle, garble, or 20 TPS frame stepping. |
| **Pass rule** | Quality, speed, loop, bounce, reverse, memory sharing, restart, and MP rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Rows become runnable after the current-frame GPU upload path and robust animation data guard are built. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Place a high-quality GIF block close to the camera. | Text/details are crisp and no atlas muffle/speckle is visible. | 🎯 | 🎯 |
| D2 | Use a fast GIF source. | Playback timing matches source more closely than 20 TPS frame stepping. | 🎯 | 🎯 |
| D3 | Test loop, bounce, and reverse. | Playback modes render correctly in world. | 🎯 | 🎯 |
| D4 | Place many copies of the same animated id. | Copies share the same decoded/render resource and stay performant. | 🎯 | 🎯 |
| D5 | Restart and revisit the blocks. | Animated blocks remain visible, textured, and correctly timed. | 🎯 | 🎯 |

## E - Source Wall review workflow - Built 🎯

| | |
| --- | --- |
| **Check** | Source Wall helps review and repair old blurry source images without touching owner-built structures. |
| **Pass rule** | Create wall, skip variants, retexture one, undo one, teardown, and MP visibility rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but awaiting in-game confirmation. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Run the Source Wall command/workflow. | Wall places only review targets and skips variants/Arabic/generated blocks. | 🎯 | 🎯 |
| E2 | Pick one wall block and replace its source. | Existing `/cb retexture` rail updates that block and is undoable. | 🎯 | 🎯 |
| E3 | Tear down the Source Wall. | Only the wall placements created by the workflow are removed. | 🎯 | 🎯 |
| E4 | Reopen after restart. | Review state and already-fixed blocks remain correct. | 🎯 | 🎯 |

## F - Cutout render layer for G10 background attribute - Designed ⏳

| | |
| --- | --- |
| **Check** | Slot blocks render on the cutout layer when G10 background transparency requires it. |
| **Pass rule** | Transparent, black default, shaped-block, and existing-solid rows pass twice with G10. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | G10 owns the background attribute; G14 owns only the render-layer registration. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Set a block background to transparent after G10 build. | Transparent pixels show through correctly. | 🎯 | 🎯 |
| F2 | Inspect a normal black-background block. | Existing solid blocks still look black, not unexpectedly see-through. | 🎯 | 🎯 |
| F3 | Test a shaped transparent block. | Cutout layer does not create broken interior artifacts. | 🎯 | 🎯 |

## G - Showcase / cycler block - Designed ⏳

| | |
| --- | --- |
| **Check** | A normal SlotBlock with showcase data cycles through a filtered set of block textures in sync for all clients. |
| **Pass rule** | Place, filter, random, sync-all, animated member, inventory icon, and performance rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Rows become runnable after `ShowcaseData`, renderer integration, and item icon cycling are built. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Create or toggle a showcase block. | Placed block cycles through the chosen filtered pool. | 🎯 | 🎯 |
| G2 | Set random mode. | Random sequence behaves as designed without desyncing between clients when sync-all is selected. | 🎯 | 🎯 |
| G3 | Include an animated member in the pool. | Animated member plays while it is the active showcase entry. | 🎯 | 🎯 |
| G4 | Inspect the item in inventory/hotbar. | Inventory icon cycles too if item-render hook is built. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Animated block basics — ✅ `2026-06-19`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §H Universal video import — 💤 `2026-07-09`: Needs ffmpeg/fallback plan and later build.
- §I Redstone-reactive and live-data display blocks — 💤 `2026-07-09`: Later display-platform phases.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- G14-owned animation UI spec — 👎 `2026-07-09`: Moved to G27.
- Standalone `/cb video` and `/cb video extract` commands — 👎 `2026-07-09`: Dropped in favor of animation hub direction.
- Old ScreenTest prototype cluster — 👎 `2026-06-20`: Superseded by proper render architecture.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete temporary animated/static test blocks after testing.
- [ ] Keep animation screen findings in G27.
- [ ] Keep G10 background transparency failures cross-linked to G10 and G14.
- [ ] Do not revive old atlas-toggle or ScreenTest prototype directions.

</details>
