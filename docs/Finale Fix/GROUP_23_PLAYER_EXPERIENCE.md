# Group 23 — Player Experience: Onboarding & Achievements

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 04 (Chat Messages) verified.
>
> **Objective:** Build the complete first-time player experience (starter guide book, tutorial screen, sample blocks, contextual hints) and the achievement system (milestone tracking, achievements tab in GUI, TipPool rotating hints). Both systems share `players.json` and TipPool — they are tested together.
>
> **Source issues:** P7 (first-join welcome book + tutorial screen), R3 (SampleBlocksLoader + FirstUseHints + TipPool), Q4 (Achievement System)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group adds

### Onboarding

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| First-join welcome | `OnboardingManager` + `WelcomeManager` — simple message | Stub | Physical Starter Guide book + pop-up tutorial screen |
| Sample blocks | `SampleBlocksLoader` — loaded presets on fresh install | Stub | 5–10 curated example blocks |
| FirstUseHints | `FirstUseHints.java` existed | Stub | Contextual one-time hints wired to specific commands |
| TipPool | `TipPool.java` existed | Stub | Rotating tips in dashboard + after commands |

### Achievements

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| Achievements | Existed — `/cb achievements` | `AchievementsManager` stub | Built from scratch |
| GUI | Screen-based | Missing | Chest GUI tab in main dashboard |
| Notification | Toast on unlock | Missing | Title text + chat message with `[View]` link |
| Persistence | Per-player | N/A | `config/customblocks/data/achievements.json` |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Achievements GUI | `/cb achievements` |
| Achievement tab | Bottom row of `/cb` main dashboard |
| Tip slot | Bottom row of `/cb` main dashboard |
| First-join book | Auto-given to new players |
| Tutorial screen | Auto-opens on first join |
| Sample blocks | Auto-loaded on fresh install |
| FirstUseHints | Fires once after specific commands |
| Player tracking | `config/customblocks/data/players.json` |

---

## Implementation Requirements

### 1. First-Join Detection

`PlayerManager` checks `config/customblocks/data/players.json` on join. If UUID not present → first join. Mark UUID. Runs once per player, ever.

### 2. Starter Guide Book

Given on first join — a written book item:
- **Title:** "CustomBlocks Starter Guide" | **Author:** "CustomBlocks"
- Page 1: Welcome + what the mod does
- Page 2: "Your first block: `/cb create <id> <name>` → texture with `/cb retexture <id> <url>`"
- Page 3: "Open the dashboard: `/cb`"
- Page 4: "Get your tools: `/cb brush` for glow, `/cb deleter` for removing"
- Page 5: "Need help? `/cb help` anytime"

### 3. Tutorial Screen

Opens immediately after the book is given. Full-screen overlay (Screen GUI — exception to the chest-GUI rule, justified for welcoming first experience).

5 pages with "Next" / "Back" / "Dismiss" buttons. Each page has a clickable button pre-filling a key command in chat. Dismissed state stored in `players.json` — never re-opens after dismiss.

### 4. Sample Blocks (Fresh Install)

On first boot with 0 registered blocks, `SampleBlocksLoader` creates:

| ID | Name | Description |
|---|---|---|
| `sample_glowing_orb` | Glowing Orb | Pink/purple texture, glow 8 |
| `sample_bricks_red` | Red Bricks | Classic red brick pattern |
| `sample_neon_grid` | Neon Grid | Cyberpunk neon grid |
| `sample_stone_mossy` | Mossy Stone | Mossy cobblestone variation |
| `sample_lava_glow` | Lava Block | Lava-like texture, glow 12 |

Category: "Samples". Only removed if explicitly deleted by an operator.

### 5. FirstUseHints

One-time contextual hints per player (stored in `players.json`):

| Trigger | Hint shown |
|---|---|
| First `/cb create` | "Give yourself the block with `/cb give <id>` or find it in the Custom Blocks creative tab." |
| First `/cb give` | "Hold the block in your offhand for a hologram preview." |
| First `/cb setglow` | "Holding a glowing block in your hand emits dynamic light." |

Each hint fires once per player only.

### 6. Achievement Definitions

Tracked per-player UUID. Key achievements:

**Creation:** `first_block` (1 block), `ten_blocks` (10), `fifty_blocks` (50), `hundred_blocks` (100)

**Texture:** `first_texture` (apply URL), `ai_texture` (AI generate), `animated_block` (GIF block)

**Sharing:** `first_share` (vault share), `marketplace_import` (import from market), `category_share` (share category)

**Color/Image:** `gradient_created`, `palette_saved`, `bg_removal`

**Mastery:** `all_shapes` (every shape used), `all_tools` (every Omni-Tool mode), `bulk_master` (bulk op on 50+ blocks)

### 7. Achievement Unlock Notification

When unlocked:
- Title text above hotbar: "Achievement Unlocked: [Name]"
- Chat: `You unlocked "Name"! [View]` → opens achievements GUI.

### 8. Achievements GUI

`/cb achievements` — chest GUI:
- Locked: gray glass pane with name + "???" description.
- Unlocked: achievement icon + name + description + unlock date on hover.
- Bottom row: progress slot showing "X of Y unlocked".

**Tab in main dashboard:** "Achievements" slot in bottom row of `/cb`. Click → opens achievements GUI.

### 9. TipPool in Dashboard

Bottom row of `/cb` main dashboard has a "Tip" slot. Hover shows a rotating helpful tip. Tip rotates each time the dashboard is opened. Sourced from `FirstUseHints` early-game → advanced tips after milestone achievements.

---

## Setup — Onboarding Tests

Log in with a fresh player account (never joined before), OR wipe `players.json` to simulate a fresh join.

## Setup — Achievement Tests

```
/cb create g25a AchTest
```

---

## Test G25.1 — Starter Guide book given on first join

Log in as a new player.

**Expected:** Within 3 seconds: "CustomBlocks Starter Guide" written book in inventory. Chat shows welcome message.

**Pass:** Book in inventory with correct title and 5 pages.
**Fail:** No book, or wrong content.

---

## Test G25.2 — Tutorial screen opens

Immediately after G25.1.

**Expected:** Full-screen tutorial overlay opens. First page visible. "Next" and "Dismiss" buttons present.

**Pass:** Tutorial screen opens automatically.
**Fail:** No tutorial screen.

---

## Test G25.3 — Tutorial navigation and buttons

Click "Next" through all 5 pages. Click command buttons on each page.

**Expected:** Pages advance. Buttons pre-fill `/cb create`, `/cb`, `/cb brush`, etc. in chat.

**Pass:** All pages accessible, buttons work.
**Fail:** Navigation broken or buttons wrong.

---

## Test G25.4 — Tutorial does not re-show

Dismiss tutorial. Log out and back in.

**Expected:** Tutorial does NOT re-open.

**Pass:** Shown only once.
**Fail:** Re-opens every login.

---

## Test G25.5 — Sample blocks on fresh install

Start a completely fresh server (0 block data). Open creative → CustomBlocks tab.

**Expected:** 5 sample blocks visible. `/cb categories` shows "Samples".

**Pass:** Samples present on first boot.
**Fail:** Tab empty.

---

## Test G25.6 — FirstUseHint after first create

As the new player, run:
```
/cb create g25hint HintTest
```

**Expected:** After success: hint fires — "Give yourself the block with `/cb give g25hint` or find it in the Custom Blocks creative tab."

**Pass:** Contextual hint appears.
**Fail:** No hint.

---

## Test G25.7 — Hint fires only once

```
/cb create g25hint2 HintTest2
```

**Expected:** No hint this time.

**Pass:** Hint does not repeat.
**Fail:** Same hint fires again.

---

## Test G25.8 — First Block achievement fires

Creating `g25a` from Setup (or `g25hint` if this is the player's true first block):

**Expected:** Title text "Achievement Unlocked: First Block" above hotbar. Chat message with `[View]` link.

**Pass:** Achievement notification fires.
**Fail:** No notification.

---

## Test G25.9 — Achievements GUI

```
/cb achievements
```

**Expected:** Chest GUI. "First Block" shown unlocked with icon + unlock date on hover. Locked achievements shown as gray panes.

**Pass:** GUI shows correct locked/unlocked states.
**Fail:** Command missing, or all locked despite G25.8.

---

## Test G25.10 — Achievement tab in dashboard

```
/cb
```

**Expected:** Main dashboard has "Achievements" slot in bottom row. Click → opens achievements GUI.

**Pass:** Slot present, navigation works.
**Fail:** No achievements slot.

---

## Test G25.11 — Progress bar in achievements GUI

In `/cb achievements`, bottom row.

**Expected:** Progress slot showing "X of Y achievements unlocked."

**Pass:** Progress displayed.
**Fail:** No progress indicator.

---

## Test G25.12 — TipPool hint in dashboard

```
/cb
```

**Expected:** Bottom row has "Tip" slot. Hover → helpful tip. Open dashboard again → tip may differ.

**Pass:** Tip slot visible, rotates.
**Fail:** No tip slot, or always same tip.

---

## Test G25.13 — Achievement and hint data persists

Restart server.

- `/cb achievements` → "First Block" still unlocked with original date.
- Open dashboard → tip slot present.

**Pass:** All data persisted.
**Fail:** Reset after restart.

---

## Group 23 Verdict

| Test | Description | Result |
|---|---|---|
| G25.1 | Starter Guide book given on first join | ⬜ |
| G25.2 | Tutorial screen opens automatically | ⬜ |
| G25.3 | Tutorial pages + buttons work | ⬜ |
| G25.4 | Tutorial shown only once | ⬜ |
| G25.5 | Sample blocks on fresh install | ⬜ |
| G25.6 | FirstUseHint fires after first create | ⬜ |
| G25.7 | Hint fires only once | ⬜ |
| G25.8 | First Block achievement notification | ⬜ |
| G25.9 | Achievements GUI shows correct states | ⬜ |
| G25.10 | Achievement tab in main dashboard | ⬜ |
| G25.11 | Progress bar shows completion ratio | ⬜ |
| G25.12 | TipPool hint in dashboard rotates | ⬜ |
| G25.13 | All data persists after restart | ⬜ |

**Group 23 passes when the full first-time experience and achievement system both work in-game.**

If anything shows ❌ — paste:
1. Whether the player UUID was in `players.json` before the test
2. What appeared vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g25a
/cb delete g25hint
/cb delete g25hint2
```
