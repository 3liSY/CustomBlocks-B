# Group 30 - Guess Mode and Showcase

> Group 30 lets an operator run a voice-led block guessing game: the holder sees a disguised CustomBlock while everyone else sees the real answer and the shared guessing pose.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_30.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Guess Mode makes a CustomBlock party game possible without changing the real block, its asset, or its placed data. A flagged player is blinded locally: they see a disguise instead of the real name/texture. Other players still see the real block, while the shared pose signals that the player is holding a mystery block.

The game is judged through voice conversation, so the mod never pretends it can hear a correct answer. G30 owns held-player disguise/pose state, manual future round control, the Showcase display, and the Guess Screen behavior. It does not own general rendering assets, HUD architecture, or Screen primitives.

## Ownership

| Owns | Does not own |
| --- | --- |
| Per-player Guess Mode state, commands, local holder disguise, pose sync, and fallback chain | CustomBlock data, texture generation, and resource-pack rebuilds |
| Guess-specific pose/look/showcase configuration and manual future round effects | Shared Screen frame/slider primitives: G27 |
| `GuessShowcaseBlock` data/renderer/tuning and its command lifecycle | General persistent world display platform: G19 |
| Voice-led round state and operator triggers | Automatic answer recognition, timers, or generic game framework |
| Placed Mask Mode state, its command, and its per-placement mask records | Vanilla block rendering, block protection, and any claim/region system |

## Direction

Guess Mode treats secrecy as a local client-render effect. The flagged holder sees a mystery look, hidden name, hotbar/inventory disguise, and matching placed view. Watchers keep the real item/block appearance. Only the pose needs all-client synchronization because every viewer must see the flagged holder present the block with both arms.

The same render seam runs in both directions. Guess Mode blinds one flagged holder while watchers keep the truth; Placed Mask Mode inverts that and blinds every watcher while one runner keeps the truth. Both resolve to the same bundled QuestionMark asset and neither one changes stored block data.

All configuration is organized under one `/cb guess` Screen with a persistent tab bar. Pose and Showcase use the same shared settings model. Look, Buzz, and Sound have defined destinations rather than scattered commands; visual Screen mechanics follow G27 while G30 validates the actual game settings and actions.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-24 | Holder is blinded locally; other players see the real block. | Guessing cannot leak the answer through the holder's own UI while watchers retain the answer. |
| 2026-06-24 | Guess scope offers held-only and all-custom-blocks-visible modes. | An operator can choose ordinary play or stronger anti-cheat suppression. |
| 2026-06-25 | Both hands, hotbar, inventory, and the holder's placed view follow the disguise. | Switching hand or opening inventory does not reveal the answer. |
| 2026-07-05 | Commands use `/cb guess` subcommands with operator access. | Core controls are player on/off, text, scope, per-round look, and default look. |
| 2026-07-05 | Disguise looks are existing CustomBlock IDs only. | Raw image/GIF links are not part of Guess Mode; new art is created through normal Studio flow first. |
| 2026-07-05 | Look resolution is per-round override, then global default, then bundled QuestionMark. | A missing/deleted configured look never breaks guessing or shows garbage. |
| 2026-07-06 | Pose is shared/global and configured through the Guess Screen, not a parallel pose command. | Every flagged holder uses the same saved pose and clients receive one synchronized value. |
| 2026-07-07 | Guesses are judged manually by operator controls. | Voice-call play never relies on fictitious in-game answer detection. |
| 2026-07-07 | Hint ladder, timer, timeout TNT, and boss tension are cut. | They must not return as implied core game behavior. |
| 2026-07-07 | Round controls, buzz, stage shockwave, and sound swap stay one later connected pass. | Shared round-state/effects work is not built piecemeal. |
| 2026-07-07 | Showcase is a persistent command-spawned block separate from active rounds. | It can decorate a game space and remains until an operator removes that exact instance. |
| 2026-07-24 | Guess Mode gains a second, inverted mode: Placed Mask Mode hides the runner's own placements from everyone else. | The Group now owns two disguise directions and both must share one look resolver and one render seam. |
| 2026-07-24 | Placed Mask Mode masks only CustomBlocks placed while the mode is on, tracked per placement. | Vanilla blocks and blocks placed before the toggle are never touched, so the mask can be undone exactly. |
| 2026-07-24 | Only the runner sees through their own mask; other operators do not. | Authorization controls who may run the command, never who may see the answer. |
| 2026-07-24 | Toggling the command off unmasks every block that runner masked. | The mode is fully reversible and never leaves permanent masked blocks in the world. |
| 2026-07-24 | Placed Mask Mode always uses the bundled QuestionMark and never a configurable look. | It adds no look commands, no Guess Screen tab, and no new fallback chain. |

## Feature Plan

### A. Holder Disguise and Shared Pose

**Player outcome**

A chosen player holds a mystery block without seeing the real answer, while everyone else sees the answer and a clear two-handed game pose.

**Experience**

- `/cb guess <player> on|off` controls persistent player participation; a one-time friendly message tells a newly flagged holder what happened.
- The holder's name, tooltip, HUD, hotbar/inventory icon, hands, and placed view use their configured disguise according to held/all scope.
- Watchers see the real item/block, but every viewer sees the flagged holder's shared centered two-hand pose.
- Disguise applies to main hand and offhand; a death-dropped item retains the mystery appearance until the same flagged player holds it again.
- Turning Guess Mode off restores normal visuals immediately.

**Requirements**

- `GuessModeStore` persists enabled state, scope, blank text, and look reference per player with atomic storage.
- Local name/texture suppression checks only the local flagged holder; it does not broadcast the secret just to blind that player.
- `GuessSync` broadcasts the participant/pose state needed for first- and third-person pose rendering across clients.
- Held-item render routing disguises local GUI/first-person paths and preserves real third-person watcher rendering.
- All-scope suppression covers held naming, looked-at HUD output, and block-list selection leak points.
- A pose spike/implementation uses dedicated client mixins and must keep normal player rendering untouched outside flagged CustomBlock holding.

**Boundary**

Guess Mode never mutates `SlotData`, deletes texture assets, or rebuilds a pack merely to hide a block. It is a render/state feature, not an alternate block type.

### B. Looks, Pose, and Guess Screen

**Player outcome**

An operator can make the mystery presentation feel deliberate without navigating multiple conflicting command systems.

**Experience**

- `/cb guess defaultblock <id>|clear` sets the reusable default look; per-round `look <id>` can override it.
- The fallback chain is per-round look, default look, then the bundled sharp red-on-black QuestionMark look.
- `/cb guess` opens one tabbed settings Screen: Pose, Buzz, Sound, Look, and Showcase. Tabs remain organized by their purpose rather than a long flat control list.
- Pose controls use live 3D preview, independent arm pitch/yaw/roll, saved presets/defaults, and screen-only apply/reset behavior.
- Future placement, block animation, body/flair, Buzz, Sound, and Look controls share the same UI conventions but only invoke their own G30 data paths.

**Requirements**

- Look IDs resolve to existing custom block slots and reuse static/animated caches; missing IDs fall back silently to bundled QuestionMark.
- QuestionMark asset stays sharp, high-resolution, true black, and independent of the removed legacy `mystery.png` look.
- Pose configuration sends one validated client-to-server payload, saves one shared setting, and broadcasts it to clients; it is not converted into chat-command strings.
- G27 provides tab bar, sliders, placeholder treatment, number entry, dirty-change handling, accessibility, and Screen feedback. G30 owns each tab's setting meaning and server validation.

**Boundary**

Raw image/GIF-link looks remain outside Guess Mode. The owner can make a reusable look through Studio/G10/G14 first, then choose its block ID here.

### C. Guess Showcase

**Player outcome**

An operator can place a polished persistent mystery showcase for a round or as a decoration without tying it to a player flag.

**Experience**

- `/cb guess showcase spawn [blockid]` places one block above the operator; no ID gives a clear message and the QuestionMark fallback.
- Multiple Showcases can coexist; an operator shift-right-clicks the exact instance or uses delete to remove it.
- The display has a spinning picture core, counter-rotating outer layer, gentle bob, scalable size, optional glow, and optional particles.
- Showcase tuning lives on its own Guess Screen tab and affects the shared presentation rather than hiding inside pose controls.

**Requirements**

- Use a command-spawned no-item `GuessShowcaseBlock` plus block entity/renderer/registry so world persistence and client sync are explicit.
- Store shown look slot and call `markForUpdate` on changes; use the existing disguise look resolver/cache for core texture rendering.
- Save shared inner-speed, outer-orbit speed, size, glow, and particle tuning in `GuessShowcaseStore` and validate/broadcast updates through a dedicated operator-checked payload.
- Orbit distance scales with size; deletion targets one directly selected Showcase rather than an arbitrary nearest display.

**Boundary**

This is a Guess Mode prop. G19 owns the broader showcase/display platform, and a Showcase does not create/stop/manage an active guessing round.

### D. Future Manual Round-Control Pass

**Player outcome**

When the next game-effects pass is built, an operator can react to voice-call outcomes instantly without asking the mod to understand speech.

**Experience**

- Wrong buzz targets the flagged holder in the operator's crosshair, is repeatable, and otherwise silently does nothing.
- Correct/reveal ends every active holder round at once; force-end quietly ends one crosshair target. Both execute immediately for the trusted operator.
- A no-active-round use of correct/force-end states that plainly.
- Wrong buzz is a full-screen red glitch flash, discordant sound, and real camera shake for nearby viewers; its settings have live preview.
- Optional correct-reveal shockwave runs only inside a marked stage zone and excludes the triggering operator.
- Sound swap replaces place, break, and footstep material clues only while a round is active.

**Requirements**

- Round state remains independent per flagged holder and persists restart; shared tuning remains global.
- Operator keybinds are permission checked server-side and send no action until a valid target/round condition exists.
- Stage zones use two-corner selection and affect only their explicit box.
- The future HUD status is a G03 HUD widget, not a parallel always-on Guess Mode overlay.

**Boundary**

This entire section is one future cohesive pass. Hint stages, countdown/timer, timeout TNT, and boss tension are not part of it.

### H. Placed Mask Mode

This section is lettered `H` so it matches its Testing Guide section. It is the inverted counterpart of section A: A blinds one flagged holder, H blinds everyone except one runner.

**Player outcome**

An operator turns on one command, builds normally, and every block they place appears as a QuestionMark to every other player while they themselves keep seeing the real build. Turning the command off restores the whole build for everyone at once.

**Experience**

- `/cb guess placed` toggles the mode for the player who ran it; running it again turns it off and immediately reveals every block that runner masked.
- While the mode is on, each CustomBlock that runner places is recorded and rendered as the bundled QuestionMark for all other players.
- The runner always sees their own placements as the real block, including the real name and HUD text.
- Blocks placed before the toggle, blocks placed by other players, and all vanilla blocks are never masked.
- For other players the mask also covers the look-at HUD name, the break particles, the break sound, and the item dropped when a masked block is broken.
- The runner's held item, hotbar, and inventory are never disguised; the mask begins at placement.
- Two players may run the mode at the same time and stay independent: each sees only their own placements as real and sees the other runner's placements as QuestionMark.
- Mode state and masked positions survive relog and server restart, so a build stays hidden across sessions until the runner toggles the mode off.

**Requirements**

- A persistent `PlacedMaskStore` holds, per runner, the enabled flag and the set of masked block positions, saved atomically with the same storage discipline as `GuessModeStore`.
- Masking is decided per placement on the server, not recomputed from block type, so the record stays exact and reversible.
- The client render decision is `viewer is the recording runner -> real block, otherwise QuestionMark`; operator or admin permission never grants see-through.
- Masked positions and mode state sync to clients through the existing `GuessSync` route rather than a second parallel channel.
- Breaking a masked block drops the real item, and the mask is applied to the resulting item entity for non-runner viewers only; the record for that position is released on break.
- The dropped item carries the mask as an invisible `custom_data` runner marker, applied only in the ground and item-frame render modes and removed the moment the item enters any inventory, so a picked-up stack is byte-identical to a normal one.
- Look resolution is fixed to the bundled QuestionMark; the per-round and default-look chain of section B is not consulted.
- The stored position set is capped, and reaching the cap reports a clear message instead of silently dropping masks.

**Boundary**

Placed Mask Mode does not mask vanilla blocks, does not disguise held or inventory items, does not add a Guess Screen tab, and does not create or judge a round. It is an independent build-hiding toggle that shares section A's render seam and section C's QuestionMark asset.

Two limits come with that shared seam and are accepted rather than worked around here. The mask is painted by the placed-block renderer, so it stops at the same block-entity render distance every off-atlas custom block already stops at — a viewer far enough away sees the real block, exactly as an animated block far enough away stops animating. And only the BREAK sound is neutralised: the repeated hit sound while a watcher is mining still comes from the real block's material, the same gap section A left open when it deferred sound work.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G03 | HUD | Guess status uses a normal optional HUD widget rather than a duplicate overlay. |
| G04 | Commands and feedback | Guess command errors and toggle messages use the unified human wording. |
| G10 | Looks and colour/image assets | Guess selects already-created block looks and does not download raw image links. |
| G13 | Arabic blocks | Arabic render/data remains separate; Guess only handles safe CustomBlock disguise routes. |
| G14 | Animated looks | Animated configured looks reuse existing animation cache/render behavior. |
| G19 | Displays | G30 Showcase remains a dedicated guessing prop and does not duplicate G19's general display management. |
| G22 | Permissions | Guess controls, Showcase, Placed Mask Mode, and future keybind actions use the operator/admin authorization route for who may run them, never for who may see through a disguise. |
| G27 | Guess Screen | G27 owns Screen primitives/tabs; G30 owns pose/look/showcase/buzz/sound data and actions. |

## Technical Contract

- Persistent `GuessModeStore` holds participant IDs and per-player enabled/scope/blank/look state atomically; a global default look/blank setting supplies sensible fallback.
- Local holder suppression is isolated to local client name/HUD/GUI/first-person render paths; only pose and shared settings are broadcast to all clients.
- Dedicated held-item/player-model render seams apply the centered pose only while a synced flagged player holds a CustomBlock, then restore normal animation.
- Look resolution is `round override -> default block ID -> bundled QuestionMark`; raw external URLs are never fetched by Guess Mode.
- Showcase uses its own persisted block entity plus server-authoritative settings payload and a renderer that consumes cached look textures.
- Future manual effects operate from explicit server round state and operator actions, never from inferred chat/voice answers.
- Placed Mask Mode stores explicit block positions per runner so the mask is exactly reversible; it never infers the mask from block type or ownership at render time.
- The Placed Mask render predicate is viewer identity only. Permission decides who may run the command; it never decides who may see through a mask.
- The viewer filter runs on the SERVER: each client is sent only the positions it must hide, with the runner's own placements already removed, so no client is ever trusted to hide the answer from itself.
- Placed Mask positions travel as per-placement and per-break deltas on the Guess sync route; only a join or a mode toggle costs a full re-send, so hiding a large build never re-sends the whole set per block.
- Placed Mask positions are keyed by dimension and packed, and the store's disk write is debounced onto the server tick because its mutations happen per block placement rather than per command.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Manual keybinds, buzz, stage shockwave, and mystery sound swap | They share round state/effects and are one future pass. | G30 |
| Impostor/odd-one-out game | It is a different multi-block game shape, not a holder disguise variation. | Future game design |
| Raw image/GIF look links | Guess look selection is intentionally existing block IDs only. | G10/G14 creation flow |
| Hint ladder, timer, timeout TNT, and boss tension | They were explicitly cut and need a new owner decision to return. | G30 only with new decision |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-25 | Guess commands used older scope/text/disguise shapes. | One operator `/cb guess` subcommand family owns core controls. |
| 2026-07-05 | Guess Mode could accept raw image/GIF links as disguise looks. | Looks are existing block IDs only with a bundled fallback chain. |
| 2026-07-06 | Pose could be configured through a separate pose command. | Pose is screen-only and sends one validated settings payload. |
| 2026-07-07 | Hint ladder, timer, timeout TNT, and boss tension were future core ideas. | They are cut and must not be rebuilt silently. |
| 2026-07-07 | Pedestal described the mystery display. | The feature is named Showcase and is a persistent block-entity prop. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_30.md) · [All Groups](README.md)

- [G03 HUD and Escape](GROUP_03_HUD_ESC.md)
- [G04 Communication](GROUP_04_Communication.md)
- [G10 Colour and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G13 Arabic and Text Blocks](GROUP_13_ARABIC.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G19 Showcase and Hologram Displays](GROUP_19_DISPLAY.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 30 snapshot](../archive/group-migration-2026-07-18/GROUP_30_GUESS_MODE.md)
