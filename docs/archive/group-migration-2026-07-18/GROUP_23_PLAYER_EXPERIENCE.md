# Group 23 — Player Experience: Onboarding & Achievements

> ### ⚠️ SCOPE SPLIT (2026-06-22) — screens moved to Group 27
> Owner decided the **two `Screen`-based pieces of this group fold into the Screens group (Group 27)**,
> to be built/migrated there under the unified screen standard with synced + cool features:
> - **Tutorial screen** (first-join) → **Group 27 §G27.16** (single source of truth for its design).
> - **Achievements gallery screen** (`/cb achievements`) → **Group 27 §G27.16**.
>
> **Group 23 keeps the screen-free parts** (build here): first-join detection, Starter Guide **book**,
> **sample blocks**, **FirstUseHints**, **TipPool**, the achievement **engine** (tracking + title/chat
> unlock notification + persistence), and the **chest** dashboard wiring (Achievements tab + Tip slot
> that *open/feed* the G27 screen). Detailed specs for the 2 screens live in G27 only — **not duplicated here.**
>
> **Achievement scope decision (2026-06-22):** build **milestones first** (`first_block`, `ten_blocks`,
> `fifty_blocks`, `hundred_blocks`, `first_texture`) — they hook cleanly into create/retexture. The
> feature-dependent achievements (AI, GIF, vault, marketplace, gradient, palette, bg-removal, shapes,
> tools, bulk) are stubbed as locked and wired later as each feature is confirmed.
>
> **Sample-texture decision (2026-06-22):** the 5 sample blocks ship as **bundled PNGs** inside the mod
> assets (works fully offline), fed through the normal texture pipeline.
>
> ⚠️ **UI medium audit (2026-07-10):** the achievements gallery is already decided Screen-based (§8 below,
> 2026-06-22) — consistent with the mod-wide Screen migration, no change needed there. What's actually
> shipped today (per G25.9's in-game result) is a **plain chat text list**, not a chest GUI — the "Chest
> GUI" wording in Test G25.9's body below is stale/inaccurate on two counts (not the real interim behavior,
> and not the eventual Screen target). Fixed below. The dashboard "Achievements"/"Tip" tabs/slots are
> Group 02's own chest dashboard (out of this group's scope) — G23 only wires a tab that opens the G27
> screen, it doesn't own that dashboard's medium.

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 04 (Chat Messages) verified.
>
> **Objective:** Build the screen-free first-time player experience (Starter Guide book, sample blocks,
> contextual hints, rotating tips) and the achievement engine (milestone tracking, title/chat unlock
> notification, persistence) plus its chest dashboard wiring. The tutorial screen and achievements
> gallery screen are built in Group 27 (§G27.16). Both systems share `players.json` and TipPool.
>
> **Source issues:** P7 (first-join welcome book + tutorial screen → screen part in G27), R3
> (SampleBlocksLoader + FirstUseHints + TipPool), Q4 (Achievement System)
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
| GUI | Screen-based | Missing | Gallery = Screen (G27 §G27.16); dashboard tab (Group 02's chest dashboard) opens it |
| Notification | Toast on unlock | Missing | Modern Toast Notification |
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

### 3. Cinematic Welcome Video & Tutorial → moved to Group 27 §G27.32 / §G27.16

**Built in the Screens group (Group 27).** A 10-second unskippable cinematic hype video plays on first join, granting 100% invincibility to the player while playing (designed in **`GROUP_27_SCREENS.md` §G27.32**). The legacy 5-page tutorial screen is merged/handled via G27 as well.

Group 23's only job for it: set/read the `tutorial_dismissed` flag in `players.json`, ensure invincibility during playback, and trigger the cinematic on first join (after the book is given). The Screen and Video renderer are G27.

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
- A Modern Toast Notification slides down: "Achievement Unlocked: [Name]" (Group 27 Physics Toast).

### 8. Achievements gallery → moved to Group 27 §G27.16

**The `/cb achievements` gallery is now a full `Screen`** (owner chose Screen over chest, 2026-06-22) —
trophy-wall grid, locked/unlocked states, unlock dates, progress bar. Full design lives in
**`GROUP_27_SCREENS.md` §G27.16** — not duplicated here.

**Stays in Group 23 (chest):** the **"Achievements" tab** in the bottom row of the `/cb` dashboard
(`MainMenu.java`). Clicking it sends the player to the G27 gallery screen (`OpenGuiPayload`). The
engine (§6) provides the locked/unlocked + progress data the screen reads.

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

**Expected:** Modern Toast Notification slides down showing "Achievement Unlocked: First Block".

**Pass:** Achievement notification fires.
**Fail:** No notification.

> **Result 2026-06-22:** ✅ PASS (in-game) — Notification engine wired at `CreationCommands.onCreated`.

---

## Test G25.9 — Achievements list/gallery ➡️ real target is the G27 §G27.16 Screen; interim behavior is a chat text list

```
/cb achievements
```

**Expected (interim, actually shipped):** a chat text list. "First Block" shown unlocked with unlock date; locked achievements shown as locked entries.
**Expected (target, not built):** the G27 §G27.16 gallery Screen — trophy-wall grid, locked/unlocked states, unlock dates, progress bar.

**Pass:** Correct locked/unlocked states shown (chat list today; Screen once G27 §G27.16 lands).
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

> **Result 2026-06-22 (partial):** ✅ In-session save confirmed — making a 2nd block did NOT re-fire the
> "First Block" achievement (proves `achievements.json` saved the unlock). Full restart-persistence test still pending.

---

## Group 23 Verdict

| Test | Description | Result |
|---|---|---|
| G25.1 | Starter Guide book given on first join | ✅ 2026-06-23 (in-game — 5-page book on first join) |
| — | `/cb achievements` text list + progress | ✅ 2026-06-23 (in-game) |
| G25.2 | Tutorial screen opens automatically | ➡️ G27 §G27.16 (screen) |
| G25.3 | Tutorial pages + buttons work | ➡️ G27 §G27.16 (screen) |
| G25.4 | Tutorial shown only once | ➡️ G27 §G27.16 (screen) |
| G25.5 | Sample blocks on fresh install | ⬜ |
| G25.6 | FirstUseHint fires after first create | ✅ 2026-06-22 (in-game) |
| G25.7 | Hint fires only once | ✅ 2026-06-22 (in-game) |
| G25.8 | First Block achievement notification | ✅ 2026-06-22 (in-game) |
| — | 10-block milestone "Getting Started" fires at 10 | ✅ 2026-06-23 (in-game) |
| G25.9 | Achievements gallery shows correct states | ➡️ G27 §G27.16 (screen) |
| G25.10 | Achievement tab in dashboard (slot 50) | ✅ 2026-06-23 (in-game — slot opens `/cb achievements` text list; gallery screen = G27) |
| G25.11 | Progress bar shows completion ratio | ➡️ G27 §G27.16 (in gallery screen) |
| G25.12 | TipPool hint in dashboard rotates (slot 48) | ✅ 2026-06-23 (in-game — rotates each reopen) |
| G25.13 | All data persists after restart | ✅ 2026-06-23 (in-game — restart confirmed, no re-fire) |

> **Full testing guide:** `Reports/GROUP_23_TESTING_GUIDE.md` (engine tests, marked + scored like the other groups).
> Rows marked ➡️ are the screen pieces — their tests live in `Reports/GROUP_27_TESTING_GUIDE.md`
> (§ "G27.16 — Onboarding + Achievements screens"). G25.8/G25.13 still cover the **engine** data the
> gallery reads; G25.10 covers the **chest tab** that opens the gallery.

**Group 23 passes when the screen-free onboarding + achievement engine work in-game.** The two screen
pieces pass under Group 27.

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
