# Group 23 - Player Experience, Onboarding, and Achievements

> Group 23 gives new players a useful first session and gives every player private, persistent hints and achievement progress without duplicating G27's Screen work.

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_23.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

The first few minutes with CustomBlocks should tell a player what matters without repeating itself or forcing a manual. A Starter Guide, small offline sample set, and one-time contextual hints provide the basics while leaving the player in control.

Achievements turn ordinary progress into a durable personal record. G23 owns the data, milestones, notifications, and dashboard routes. Tutorial, cinematic, and gallery Screens are intentionally owned by G27 so the feature has one visual system.

## Ownership

| Owns | Does not own |
| --- | --- |
| First-join detection, Starter Guide book, sample blocks, and first-use hints | Tutorial/cinematic Screen presentation: G27 |
| Per-player flags, hint history, achievement tracking, and persistence | Achievements gallery Screen layout and interaction: G27 |
| Milestone definitions and unlock notifications | Main dashboard visual framework: G27 |
| Dashboard routes and rotating tip data | Block creation and texture events that supply achievement triggers |

## Direction

On a player's first join, the server identifies that player once, gives the written Starter Guide, and supplies G27 the trigger/flags it needs for the visual onboarding experience. On a truly fresh server, a small set of bundled sample blocks makes the mod immediately understandable even while offline.

Hints and achievements are per-player, persistent, and event-driven. A hint or achievement never repeats after it is saved. The current command and dashboard routes expose the engine now, while G27 upgrades the visual experience by reading the same stable data.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-22 | Starter samples use bundled PNG assets and the normal texture pipeline. | A fresh install works completely offline. |
| 2026-06-22 | First-use hints fire once per player per trigger. | Helpful guidance never becomes repeated chat noise. |
| 2026-06-22 | Achievement work starts with creation and texture milestones. | Feature-specific achievements wait for their underlying Groups. |
| 2026-06-22 | Tutorial and achievements gallery are Screens. | G27 owns their visual design, navigation, and rendering. |
| 2026-06-22 | G23 owns tutorial dismissal flags, triggers, and achievement data. | G27 can present the experience without becoming the data owner. |
| 2026-07-10 | Dashboard visual ownership follows the unified Screen framework. | G23 contributes routes and tips, not another dashboard implementation. |

## Feature Plan

### A. First-Join Foundations

**Player outcome**

A new player gets a concise written starting point, and a fresh server has examples worth inspecting immediately.

**Experience**

- The first join gives a five-page `CustomBlocks Starter Guide` by `CustomBlocks` with create, texture, dashboard, tools, and help routes.
- A player is marked after first-join handling, so the book is not duplicated on later joins.
- A fresh installation creates the bundled Samples category: Glowing Orb, Red Bricks, Neon Grid, Mossy Stone, and Lava Block.
- Operators can remove samples intentionally; restarts never recreate or duplicate existing samples.

**Requirements**

- Record player UUID and first-join/onboarding flags in `players.json`.
- Handle a full inventory safely rather than silently losing the guide.
- Load sample PNGs through the ordinary texture path and assign the documented names/category/glow values.
- Only create samples when the server has no registered CustomBlocks.

**Boundary**

G23 provides the trigger and state for visual onboarding. G27 owns tutorial/cinematic layout, playback, and interaction.

### B. Hints, Tips, and Achievement Engine

**Player outcome**

Useful first actions receive one clear follow-up, while achievements build a private record of progress over time.

**Experience**

- First create explains how to obtain/use the new block; first give and first glow can offer their matching contextual hints.
- Tips rotate when the dashboard is opened and evolve from starter help to more advanced guidance.
- Unlocks notify the player once and retain their original unlock date.
- The current `/cb achievements` route can show real engine data before the G27 gallery is available.

**Requirements**

- Persist hints, unlocks, dates, counts, and progress separately per player UUID.
- Begin with `first_block`, `ten_blocks`, `fifty_blocks`, `hundred_blocks`, and `first_texture` milestones.
- Wire event hooks from create/retexture actions without letting repeated events duplicate unlocks.
- Provide achievement state and progress to G27 through a stable server-side data route.

**Boundary**

The engine owns facts about player progress. G27 owns the trophy-wall gallery and its visual progress presentation.

### C. Visual Handoffs and Routes

**Player outcome**

Onboarding and achievements feel connected to the rest of the mod instead of becoming separate, conflicting UIs.

**Experience**

- A fresh player can be routed from G23 onboarding state into the G27 tutorial/cinematic once it is available.
- Dismissing the visual onboarding saves the flag that stops a repeat.
- The dashboard Achievements route opens the G27 gallery and the Tip route consumes G23 tip data.
- The gallery receives locked/unlocked state, dates, and progress from G23 without guessing them locally.

**Requirements**

- Keep onboarding trigger, dismissal, and temporary player-protection handoff separate from G27 rendering.
- Keep command/dashboard routing compatible with the unified Screen system.
- Do not fork or copy achievement data for a Screen cache that can become stale.

**Boundary**

No chest or duplicate Screen UI is created here. G23 only supplies the server-side behavior and routes the existing interface needs.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Sample textures | Bundled samples use the normal asset pipeline and do not need a special loader format. |
| G06 | First-use tools | Tool-related hints describe the actual G06 behavior and fire only after a successful action. |
| G10 | Colour/image milestones | Future colour and image achievements consume real G10 events. |
| G14 | Animation milestones | Animated-block achievements wait for a dependable G14 completion event. |
| G15 | AI milestone | AI achievement tracking waits for a confirmed G15 generation event. |
| G20 | Sharing milestones | Vault/category-sharing achievements wait for real G20/G12 action events. |
| G27 | Screens and routes | G27 renders onboarding and gallery Screens; G23 supplies flags, data, and routes. |

## Technical Contract

- `players.json` records per-player first-join, tutorial-dismissal, hint, and progress state; achievement persistence retains unlock date and does not re-fire after restart.
- Fresh-install samples are bundled assets loaded through the normal texture pipeline only when no registered block data exists.
- Hint and achievement triggers run after the successful owning action, not merely after command parsing.
- The achievement engine is server authoritative; Screens request/display its stored facts rather than maintain competing progress records.
- G27 receives only the data/actions required for visual onboarding and gallery flow; G23 does not render those Screens.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| AI, GIF, vault, marketplace, gradient, palette, background-removal, shapes, tools, and bulk achievements | Each waits for a stable event from its owning feature. | G23 with owning Group |
| Tutorial cinematic and achievements gallery polish | The visual experience is one G27 Screen system. | G27 |
| More sample blocks | Five curated offline samples are enough for the initial fresh-server experience. | G23 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-22 | G23 could own a tutorial and achievement GUI. | G27 owns the Screen surfaces; G23 owns their data/triggers. |
| 2026-06-22 | Sample content could depend on network texture URLs. | Samples are bundled PNGs and work offline. |
| 2026-07-10 | The achievements dashboard target could be a chest GUI. | Current routes lead toward the unified G27 Screen system. |

</details>

## References

[Dashboard](../testing/00_DASHBOARD.md) · [Testing Guide](../testing/Testing_Guide_23.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G10 Colour and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G15 AI Textures](GROUP_15_AI_TEXTURES.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 23 snapshot](../archive/group-migration-2026-07-18/GROUP_23_PLAYER_EXPERIENCE.md)
