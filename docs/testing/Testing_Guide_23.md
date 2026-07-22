# Group 23 - Player Experience, Onboarding & Achievements

## Status

| | |
| --- | --- |
| **Verdict** | Screen-free onboarding and achievement engine are confirmed; sample blocks and G27 screen handoff still need clean coverage. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-06-23 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| E | Sample blocks fresh-install loader | Built 🎯 | Discussion ✏️ |
| F | Tutorial and achievements Screen handoff | Designed ⏳ | Discussion ✏️ |
| A | Achievement and hint engine | Done ✅ | - |
| B | Restart persistence and milestones | Done ✅ | - |
| C | Achievements command and dashboard wiring | Done ✅ | - |
| D | Starter Guide book | Done ✅ | - |
| G | Feature-dependent achievements | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_23_PLAYER_EXPERIENCE.md](../groups/GROUP_23_PLAYER_EXPERIENCE.md)

---

# Active Tests

## 💡 Setup

- Use a fresh player UUID or clear only that player from `players.json`.
- Use one disposable block id such as `g23a`.
- Screen tests for tutorial, cinematic, and achievements gallery belong to G27.
- G23 owns first-join flags, Starter Guide book, sample blocks, hints, achievement tracking, notification data, and dashboard wiring.

## A - Achievement and hint engine - Done ✅

| | |
| --- | --- |
| **Check** | First-use hints and milestone achievements trigger once per player and save correctly. |
| **Pass rule** | First create, repeat create, first give, first glow, first block, first texture, and notification rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | As a fresh player, run `/cb create g23a AchTest`. | First-create hint appears once and explains how to get the block. | ✅ | ✅ |
| A2 | Create a second block. | First-create hint does not repeat. | ✅ | ✅ |
| A3 | Run `/cb give g23a` for the first time. | Give hint appears once if that hint is enabled. | ✅ | ✅ |
| A4 | Run `/cb setglow g23a 8` for the first time. | Glow hint appears once if that hint is enabled. | ✅ | ✅ |
| A5 | Create the player's first block. | Achievement notification fires for first block. | ✅ | ✅ |
| A6 | Apply a URL texture for the first time. | First texture achievement unlocks when the event is wired. | ✅ | ✅ |
| A7 | Re-trigger any unlocked achievement. | It does not unlock twice or duplicate its date. | ✅ | ✅ |

## B - Restart persistence and milestones - Done ✅

| | |
| --- | --- |
| **Check** | Achievement and hint state survives restart, including milestone progress. |
| **Pass rule** | Save, restart, no re-fire, ten-block milestone, progress count, and per-player isolation rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Unlock First Block, restart server, then run `/cb achievements`. | First Block stays unlocked with original date. | ✅ | ✅ |
| B2 | Create another block after restart. | First Block does not re-fire. | ✅ | ✅ |
| B3 | Reach 10 created blocks. | Ten-block milestone unlocks once. | ✅ | ✅ |
| B4 | Log in as a different player. | Other player has separate achievement and hint state. | ✅ | ✅ |
| B5 | Inspect saved data. | `players.json` and achievement data are readable and not reset by restart. | ✅ | ✅ |

## C - Achievements command and dashboard wiring - Done ✅

| | |
| --- | --- |
| **Check** | Current `/cb achievements` output and dashboard slots expose the engine while G27 owns the final gallery Screen. |
| **Pass rule** | Text list, locked/unlocked state, progress, dashboard achievements slot, dashboard tip slot, and rotation rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb achievements`. | Current interim output lists unlocked and locked achievements correctly. | ✅ | ✅ |
| C2 | Check progress count. | It shows the correct unlocked/total ratio for current engine data. | ✅ | ✅ |
| C3 | Open `/cb` dashboard and click Achievements. | Dashboard route reaches the achievements output/screen target. | ✅ | ✅ |
| C4 | Hover/open the Tip slot. | A helpful tip appears. | ✅ | ✅ |
| C5 | Reopen the dashboard repeatedly. | Tip rotates and does not stay frozen on one line. | ✅ | ✅ |

## D - Starter Guide book - Done ✅

| | |
| --- | --- |
| **Check** | New players receive the written Starter Guide once with the correct content. |
| **Pass rule** | First join, title/author, page content, no duplicate, and inventory-full rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Join as a fresh player. | Starter Guide book appears within a few seconds. | ✅ | ✅ |
| D2 | Inspect the book. | Title, author, and five guide pages match the onboarding plan. | ✅ | ✅ |
| D3 | Rejoin after the player is recorded. | Book is not duplicated. | ✅ | ✅ |
| D4 | Join with a full inventory. | Book delivery is handled cleanly or reported without item loss. | ✅ | ✅ |

## E - Sample blocks fresh-install loader - Built 🎯

| | |
| --- | --- |
| **Check** | A fresh install with no registered blocks should create curated offline sample blocks. |
| **Pass rule** | Fresh boot, sample ids, category, textures, glow values, no repeat, and operator deletion rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs clean fresh-server evidence. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Start a clean server with zero block data. | Sample loader creates the sample blocks once. | 🎯 | 🎯 |
| E2 | Open Custom Blocks creative tab or `/cb list`. | Sample blocks are visible and named clearly. | 🎯 | 🎯 |
| E3 | Inspect category list. | Samples are grouped under `Samples`. | 🎯 | 🎯 |
| E4 | Inspect textures and glow. | Bundled PNGs load offline and glow values match the sample plan. | 🎯 | 🎯 |
| E5 | Restart after samples exist. | Loader does not duplicate samples. | 🎯 | 🎯 |
| E6 | Delete samples as operator. | They are removed only by explicit deletion. | 🎯 | 🎯 |

## F - Tutorial and achievements Screen handoff - Designed ⏳

| | |
| --- | --- |
| **Check** | G23 provides flags/data/triggers while G27 owns the actual tutorial, cinematic, and achievements gallery Screens. |
| **Pass rule** | Tutorial flag, first-join trigger, invincibility handoff, gallery data, dashboard route, and no duplicate ownership rows pass in TG27. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen rendering and layout are G27 work. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Fresh player joins after G27 tutorial/cinematic build. | G23 detects first join and triggers the G27 surface. | ⏳ | ⏳ |
| F2 | Dismiss the tutorial/cinematic. | `tutorial_dismissed` or equivalent flag saves and prevents repeat. | ⏳ | ⏳ |
| F3 | During cinematic playback. | Player receives the intended protection/invincibility handoff. | ⏳ | ⏳ |
| F4 | Open G27 achievements gallery. | Gallery reads G23 achievement data accurately. | ⏳ | ⏳ |
| F5 | Click dashboard Achievements after G27 build. | Route opens the G27 gallery Screen. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Achievement and hint engine — ✅ `2026-06-22`
- §B Restart persistence and milestones — ✅ `2026-06-23`
- §C Achievements command and dashboard wiring — ✅ `2026-06-23`
- §D Starter Guide book — ✅ `2026-06-23`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §G Feature-dependent achievements — 💤 `2026-06-22`: AI, GIF, vault, marketplace, gradient, palette, background removal, shapes, tools, and bulk achievements wait for their features.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g23a` and any hint test blocks.
- [ ] Restore any edited player test data after controlled first-join tests.
- [ ] Keep cinematic/tutorial/gallery screen failures in G27.

</details>
