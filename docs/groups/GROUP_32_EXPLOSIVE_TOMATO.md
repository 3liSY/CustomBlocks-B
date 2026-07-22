# Group 32 - Explosive Tomato

> Group 32 gives operators a ridiculous, powerful tomato projectile whose real explosion, temporary crater recovery, sauce gameplay, and visual chaos never take priority over server stability or player world safety.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_32.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Explosive Tomato is a server-wide toy with a real vanilla-style blast, a temporary tomato-sauce aftermath, and deliberately over-the-top combat feedback. It must still be safe to use: crater recovery, farm recovery, and anti-duplication cannot be sacrificed for spectacle, and no sauce/decal/particle task may freeze, kick, time out, or crash a server.

G32 owns tomato projectile/gameplay rules, persistent crater restoration, sauce behavior, combat exceptions, and tomato configuration. G27 owns any administrative Screen surface; it does not own explosion, restoration, or visual-effect semantics.

## Ownership

| Owns | Does not own |
| --- | --- |
| Tomato projectile, throw/ride/dispenser/command behavior, explosion power, and interception | General block creation or CustomBlock texture system |
| Crater snapshots, anti-dupe, restoration batching, sauce blocks/decals, and tomato combat effects | Backup and restore system for normal server data: G09 |
| Server-wide `tomato.json` schema and client synchronization | Tomato configuration Screen layout: G27 |
| Tomato-specific HUD/shield/footprint visual contracts | Shared HUD framework and diagnostics infrastructure |

## Direction

An Explosive Tomato is a single round sprite in inventory, hands, and flight. It detonates instantly on contact, uses a real vanilla explosion, restores its destroyed terrain after a short delay, and leaves a temporary, bounded tomato-sauce aftermath. A specially named `nuke` tomato raises blast power without changing any safety rule.

The root rule is simple: required restoration is durable and complete; optional visual work is capped and disposable. If a server is under pressure, particles, decals, sauce spread, and other cosmetic work are reduced or skipped before crater recovery, player safety, or the game thread is put at risk.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-07-15 | Tomato uses a real vanilla explosion with instant contact detonation and no fuse. | There is no fake power-zero blast, warning hiss, or bail window. |
| 2026-07-15 | Normal blast power is 6; anvil name `nuke` selects power 20. | Nuke strength is an explicit case-insensitive name override, not a general tier system. |
| 2026-07-17 | Every normally damageable in-range entity dies despite armour/resistance, including bosses. | Blast radius does not grow; shield blockers, tamed pets, and normal invulnerable/creative/spectator cases remain exempt. |
| 2026-07-17 | Crater snapshots/progress persist with the world and restore in bounded batches after five seconds. | Restart/crash cannot leave a temporary crater permanently missing. |
| 2026-07-17 | First overlapping crater snapshot wins; player-placed blocks/containers survive restoration. | A later blast cannot overwrite an active crater's original terrain or duplicate inventory contents. |
| 2026-07-17 | Underwater sauce and self-healing sauce loops are removed. | Underwater blasts use only a small capped sauce-coloured particle/bubble burst. |
| 2026-07-17 | Rain washing and punch reflection/deflection are removed. | Weather and punching do not create hidden alternate tomato behavior. |
| 2026-07-17 | Ground sauce is a short-lived real layered block; visual work is bounded by per-blast, per-chunk, and per-tick caps. | Sauce gameplay remains real without continuous spread queues or server overload. |
| 2026-07-17 | Empty-hand right-click always slurps one layer, fills hunger, and heals to normal maximum. | No sneak requirement or full-hunger refusal exists. |
| 2026-07-17 | Fall cushioning scales linearly by sauce depth. | Eight layers fully absorb fall damage; shallower puddles provide proportional protection. |
| 2026-07-18 | Shared sauce material is real photographed ketchup/sauce reference imagery (chroma-keyed, de-fringed) with stable fresh random patterns. | Superseded same day by mockup review: blood/plastic/red-gravel appearance and fixed repeated splats are still rejected, but the glossy real-ketchup look (not smooth crushed-puree) is the approved material everywhere sauce appears — block texture included. |
| 2026-07-18 | Point-blank/shield screen sauce is a scattered readable pattern that clears fully after five seconds. | Screen feedback is not a fixed edge border and cannot linger. |
| 2026-07-18 | Generic D2 coating of fences/stairs/walls is dropped because the blast destroys those targets. | Ground sauce and defined wall-drip behavior remain; unused D2-only coating code must not return. |
| 2026-07-18 | Owner reviewed and approved static/animated mockups for E8 screen splash, D3 wall drip, and D13 ground puddle; see "Mockup-Locked Parameters" under §C. | Implementation must match the approved look, material, and timing exactly, not an approximation. |
| 2026-07-18 | Wall-drip (D3) total lifetime is ten seconds, not thirty. | Owner corrected the draft number during mockup review; ten seconds is final. |

## Feature Plan

### A. Flight and Detonation

**Player outcome**

An operator can throw, ride, dispense, or command-give a tomato that looks consistent and detonates immediately when it touches a valid target.

**Experience**

- Inventory, held item, and projectile use the same round tomato sprite; a true 3D mesh is not implied by the current item art.
- `/cb tomato` supplies the item within its configured permission route; dispensers and riding use the same projectile identity.
- A named tomato displays its name in flight; `nuke` selects the higher blast power.
- Flight/contact is immediate, and an untouched airburst/ride lifetime ends in a mid-air detonation at twenty seconds.
- Arrow/snowball interception and tomato-to-tomato collision both cause immediate explosion.

**Requirements**

- `TomatoEntity` owns flight life, name propagation, collision, and detonation; collision must call its detonation path directly rather than allowing vanilla discard to cancel the blast.
- Render/model paths use one shared sprite asset/UV orientation across item and flight views.
- Explosion uses normal power 6 or explicit `nuke` power 20, no fire, and configured knockback strength.
- A random CC0 squish/splat sound from the bundled pool layers with the blast without adding unbounded audio work.

**Boundary**

Flight is not a generic projectile framework. Tomato does not gain crafting, enchantment, or multi-shot behavior until those separately parked decisions are reopened.

### B. Blast, Damage, and Durable Crater Recovery

**Player outcome**

The tomato feels dangerous in the moment but does not permanently destroy ordinary terrain or lose recovery work when a server restarts.

**Experience**

- A normal damageable target within the existing explosion damage area dies instantly regardless of armour/resistance; one blocker with a shield is protected while surrounding terrain/entities still receive the explosion.
- Tamed wolves, cats, and parrots take zero tomato blast damage but can still receive non-damage sauce effects.
- Terrain restoration begins after five seconds. Small craters return quickly; larger jobs complete steadily without freezing the server.
- Crops, tilled soil, water, and exact crop growth stage return in safe dependency order. Deliberately placed player blocks/containers stay untouched.

**Requirements**

- Use a real explosion and then a post-damage lethal completion path for non-exempt in-range survivors; do not enlarge the damage area to achieve the one-shot rule.
- Record crater snapshots in world-persistent `TomatoCraterState`, including progress, and resume safely after restart/reload.
- Restore in a global hard-bounded batch, never force-load chunks, and let unloaded positions wait until natural chunk load.
- Restore support terrain/soil/source water before crops; isolate a failing position so it cannot stop the rest of the job.
- First snapshot wins across overlapping craters. Clear blast-created sauce or flowing fluid that blocks original terrain, but keep player-placed blocks, source fluids, containers, and block data.
- Delete/neutralize blast drops on restore to prevent duplication; CustomBlocks blocks and containers are blast-proof and do not spill.
- Push entities trapped in a restoring crater safely upward.

**Boundary**

Crater recovery restores destroyed block state, not entities. Item frames, paintings, boats, armor stands, and mobs follow normal explosion behavior and are not reconstructed.

### C. Sauce, Decals, and Visual Safety

**Player outcome**

Tomato aftermath reads as temporary crushed sauce with tactile gameplay, not as a laggy permanent red layer.

**Experience**

- Flat ground receives an 8-layer sauce block that is very slippery, slurpable, and cushions falls proportionally to depth.
- Each puddle makes one small downhill-favoured irregular spread, then stops all spreading work and fades on its normal five-second timer.
- Wall drips use face-attached gravity-pulled streaks, thinner trails, and a small bottom puddle; their dedicated lifetime is ten seconds.
- Debris and left/right footprints use the same sauce material with fresh randomized variation.
- Underwater/seabed explosions show only a small capped red-particle/bubble burst; lava burns sauce instantly with steam.
- Point-blank direct or shielded hits place a fresh scattered sauce overlay with readable gaps and gravity drips that clear by five seconds.

**Requirements**

- Use the shared real-photo ketchup/sauce material (see Mockup-Locked Parameters below): chroma-keyed and de-fringed from real reference photography, glossy wet highlights, natural deep-red variation, drip-shaped edges. This supersedes the earlier smooth-crushed-puree texture description.
- A blast's random pattern remains stable for its short life; no flickering or fixed repeated template is acceptable.
- Enforce per-blast placement limit, radius cap, fifty-puddle-per-chunk FIFO cap, and hard per-tick work limits. Drop optional visual work rather than queue it.
- Surface sauce uses real ground-layer gameplay. Do not revive generic D2 fence/stair/wall coating that cannot survive the explosion target; keep only explicitly supported wall-drip visual behavior.
- Remove underwater sauce blocks and any recurring self-heal loop. Rain has no special cleanup effect.
- Shield rendering keeps banner pattern visible beneath randomized sauce patches and clears along with crater restoration.

**Mockup-Locked Parameters (owner-approved 2026-07-18)**

These three visuals were mocked up and approved outside the codebase before any implementation existed. The implementation must match them exactly — do not approximate, simplify, or "close enough" these numbers.

- *E8 screen splash* — Built only from real photographed ketchup/sauce reference images (no procedural/synthetic texture generation). Source photos are chroma-keyed clean (no white-background halo/fringe) and de-fringed. To avoid a "stamped/pasted" look from repeating the same few source photos, each placed piece uses a random small irregular sub-crop of a source photo (not the whole photo silhouette repeated), plus a soft edge feather ramp so no rectangular seam is visible. One static composite per hit — no intro/reveal animation, no drip-growth animation — sauce is at full opacity immediately on hit, covering roughly 90% of the screen in a random (non-templated) scattered layout, then simple-fades out, fully cleared by 5s.
- *D3 wall drip* — A real-photo splat composite lands on the wall face instantly (no intro). Individual drip streaks (thin real-photo strand crops, tapered) trickle downward at independently staggered random start times and durations across roughly the first 0-4.5s — this must NOT be a single uniform top-down wipe; real drips start and lengthen at different, uncoordinated moments. A small pooled puddle forms at the wall base once the first streak(s) that reach the floor arrive. Total lifetime is 10s: streaks are done trickling by ~4.5s, hold static/wet until 8s, then fade out fully by 10s.
- *D13 ground puddle* — Uses exactly one source sauce photo (not a mix of several). Shape is a Minecraft-water-bucket-style blocky flood fill outward from the impact block: a per-cell fluid "level" starts at maximum at the source block and decreases outward (mirroring vanilla flowing-liquid spread logic), producing one downhill-favoured irregular spread that runs once and then permanently stops (no re-spread/re-trigger). A small scorched/dirt crater sits directly under the impact block. Total lifetime is 5s: the flood finishes spreading by ~1.1s, holds, then fades out fully between 4s and 5s.

**Boundary**

Sauce is tomato-only. CustomBlocks tools do not edit it, and no continuous fluid-like spread/simulation is permitted.

### D. Combat, Villagers, and Feedback

**Player outcome**

Tomato combat has clear exceptions and memorable feedback without hidden behavior or permanent penalties.

**Experience**

- A shield blocks all tomato damage for its blocker and loses durability, but does not cancel the world blast or protect nearby targets.
- Villager trade anger from a hit decays linearly to zero over roughly ninety seconds; a new hit refreshes one timer rather than stacking forever.
- Sauce makes all entities slide and leaves alternating recognizable footprints after they leave a puddle.
- Death messages choose uniformly from attacker-specific or self-kill-specific pools; dispenser kills use their generic text.

**Requirements**

- Track custom villager anger per villager/thrower with one refreshable decay timer rather than relying on slow vanilla gossip decay.
- Screen/shield/footprint visual mixins remain client presentation only and do not change server damage truth.
- Self-kill selection covers airburst/ride self-death through the same self pool, without reusing a generic uncredited line.
- All direct-hit/shield visual work respects the same bounded/lifetime rule as sauce decals.

**Boundary**

Combat exceptions are intentionally tomato-specific. They do not change vanilla shield, pet, villager, or death-message behavior outside tomato damage sources.

### E. Configuration and Administration

**Player outcome**

Server owners can tune tomato behavior consistently for every player without a client/server mismatch.

**Experience**

- Tomato settings are server-wide and administrator-controlled, never an individual client preference.
- The future configuration route appears within G27 configuration Screens rather than as a disconnected tomato UI.
- Temporary test changes can be restored through one documented `tomato.json` location.

**Requirements**

- Keep settings in `config/customblocks/tomato.json`, including blast power, nuke power, restore delay, knockback, cap/limit values, and enabled visual/gameplay tuning.
- Validate, persist, and synchronize config before clients render values that must agree with server behavior.
- Configuration UI calls G32 config services and does not own a second schema.

**Boundary**

Tomato configuration does not modify shared CustomBlocks config schema unnecessarily and does not replace G22 permission policy.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Assets | Tomato item/projectile/sauce assets use normal resource delivery and do not alter CustomBlock slot assets. |
| G09 | Recovery safety | Tomato's short crater state complements, never replaces, normal backups/recovery. |
| G16 | Diagnostics | Lag, timeout, crash, or missing-restoration events are diagnostic incidents with useful evidence. |
| G22 | Permissions | Tomato command/config/push actions use approved server authorization. |
| G27 | Configuration Screen | G27 presents tomato configuration; G32 owns every schema field and server rule. |

## Technical Contract

- `TomatoEntity` uses one immediate-detonation path, explicit twenty-second airburst/ride limit, real explosion powers, and name propagation.
- Guaranteed tomato kill occurs only for otherwise normally damageable in-range entities after ordinary blast handling; shields, tamed pets, and ordinary invulnerability exemptions remain intact.
- `TomatoCraterState` persists snapshots/progress per world, first-snapshot-wins, restores in hard bounded batches, never force-loads chunks, and isolates a bad restore position.
- Restoration prioritizes original required world data over all tomato cosmetics and preserves player-placed structures/containers/source fluid according to the recorded change policy.
- Sauce work has hard per-blast/per-chunk/per-tick limits; optional particles/decals/spread are skipped before the server risks a freeze, kick, timeout, or crash.
- Ground-layer sauce owns gameplay; decals/drips/overlays are bounded visuals with independent cleanup timers and shared stable randomized material.
- Tomato config is server authoritative in `tomato.json` and synchronized to clients that need visual settings.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| True 3D tomato mesh | Current sprite consistency is the complete visual requirement; mesh needs a separate asset task. | G32 3D model work |
| Infinity/Multishot, crafting recipe, stats, and leaderboards | They expand availability/progression after core safety and feedback are stable. | G32 QoL work |
| Fabric claim/protection-mod compatibility | It needs a concrete Fabric integration contract rather than a guessed generic hook. | G32 compatibility work |
| Arabic localization | Tomato text remains English while feature wording is still changing. | G13/localization work |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-07-15 | Tomato could use a fuse or a non-real visual explosion. | Any valid contact detonates immediately through a real explosion. |
| 2026-07-17 | Armour/resistance could leave a tomato-damaged target alive. | Non-exempt in-range normally damageable targets are guaranteed to die. |
| 2026-07-17 | Crater recovery could be memory-only or unblockably delayed by fluid. | Persistent bounded restore resumes after restart and clears only blast-created obstructions. |
| 2026-07-17 | Underwater sauce, rain wash, and punch deflection were active mechanics. | They are removed; underwater uses a capped particle burst and rain/punch have no special tomato action. |
| 2026-07-17 | Sauce required sneak/full hunger room and used a hard fall-damage threshold. | One empty-hand slurp works at full hunger and fall protection scales linearly with depth. |
| 2026-07-18 | Sauce could be fixed red splats or generic face coating across destroyed structures. | It uses shared stable random sauce material; unsupported D2 coating stays removed. |
| 2026-07-18 | Screen sauce could be an edge-only pattern lasting ten seconds. | Direct/shield impact uses a scattered readable five-second pattern. |
| 2026-07-18 | Shared sauce material was described as smooth crushed-tomato puree (pulp/seeds, restrained highlights, ketchup look rejected). | Owner mockup review same day approved the real-photo glossy ketchup material instead (see Mockup-Locked Parameters); puree description no longer applies anywhere, including the block texture. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_32.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G09 Backup and Recovery](GROUP_09_BACKUP_SAFETY.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G22 Permissions](GROUP_22_PERMISSIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 32 snapshot](../archive/group-migration-2026-07-18/GROUP_32_EXPLOSIVE_TOMATO.md)
