# Group 32 — Explosive Tomato

> **Status:** Phase A + core Boom built & MP-tested 2026-07-15; owner retest found regressions + locked a rework
> (2026-07-15, discussion-only pass — none of the below is built yet):
> - **A1** → fix the upside-down flight billboard **and unify the icon**: inventory/hand use the **same round flat
>   sprite** as flight this pass. A true 3D tomato **mesh** is deferred to its own later task (**G32-3D-MODEL**), not
>   this pass.
> - **A4** → airburst/ride lifetime **10s → 20s**; detonates in mid-air at 20s if it hits nothing.
> - **B1** → **fuse REMOVED entirely** — instant blast on ANY contact, no ~0.5s tell, no flash/swell, no bail window.
>   Crater restore **15s → 5s**.
> - **B2** → no-damage regression root-caused: `onCollision` calls `super.onCollision`, which discards the entity
>   before the fuse can tick, so `detonate()` never runs. Fix = detonate **directly in `onCollision` before discard**
>   (this same fix covers B1). Blast stays power 6 / nuke 12, no fire, LETHAL (armour mitigates), griefing ON.
>
> See TG32.
> **Source:** Entirely new feature requested for CustomBlocks-B.

## What this is

A throwable Explosive Tomato combat item. It flies like a snowball, detonates like TNT **instantly on any contact**
(fuse removed, owner-locked 2026-07-15), and leaves behind a slippery "tomato sauce" mess. It is admin-spawned, and it is anti-grief: every block it destroys
comes back.

Two facts to keep in mind while reading, because the original request was written Bukkit-flavoured:

- This is a **Fabric 1.21.1 mod**, not a Bukkit plugin. WorldGuard/Towny do not exist here.
- The mod registers its own blocks and items, so **vanilla clients cannot join anyway** — every player has the mod,
  and client-side rendering is safe to rely on.

## Build order

Three phases. Test in-game and get confirmation before starting the next (CLAUDE.md §2/§4).

| Phase | Name     | What lands                                                                 |
| ----- | -------- | -------------------------------------------------------------------------- |
| **A** | **Fly**  | Entity, renderer, model, throw, riding, dispenser, `/cb tomato` command     |
| **B** | **Boom** | Instant contact detonation (no fuse), TNT explosion, nuke, crater restore, anti-dupe, blast-proofing |
| **D** | **Mess** | Sauce blocks, decals, drips, debris, slipperiness, eating                   |

Then C (deflect & interception) and E (combat effects). F and G are parked.

All new code lives in a new self-contained **`com.customblocks.tomato`** package (same shape as `buzzergame`).

## Phase A — Fly

- **Acquisition**: `/cb tomato <targets> [amount]` using vanilla entity selectors, plus a literal `/cb tomato all`.
  Amount defaults to 1. Gated on **vanilla OP level 2** — Fabric has no permission system, so the old
  `customblocks.tomato.give` node does not exist. Also grabbable from the **CB tools creative tab**.
- **Item**: Stacks to 64. Drops safely from broken chests without detonating. Craftable is **parked QoL**
  (eventual recipe: TNT + food).
- **Model — THIS PASS (owner-locked 2026-07-15, A1)**: **unified flat sprite.** Two fixes: (1) the flight billboard
  renders **upside-down** — the renderer's UV `v` coords are inverted (texture bottom maps to the top vertex); swap
  them so it reads right-way-up. (2) inventory/hand currently use an **ugly faceted-cube 16px JSON** — replace
  `models/item/explosive_tomato.json` with a **flat 2D `item/generated`** model pointing at the round **256px** entity
  sprite, so inventory + hand + flight all match. In-hand is a **flat billboard card** (owner accepted 2026-07-15). No
  mesh needed; consistent immediately.
  **Sprite art — OWNER-PROVIDED ASSET, LOCKED 2026-07-15:** the owner's hyper-real tomato PNG (glossy red body,
  water droplets, green calyx + stem; 3531×3451, transparent). Copied into the repo at
  **`docs/groups/assets/tomato/tomato_source.png`** (safe copy of `C:\Users\66664\Desktop\tomato_PNG12567.png`).
  Build session: **downscale to a 256px sprite** (keep transparency, trim/center) and **replace the current
  entity texture** `src/main/resources/assets/customblocks/textures/entity/explosive_tomato.png`, then point the
  flight billboard + `item/generated` inventory/hand model at it so all three surfaces unify. No procedural
  sprite — this asset is final for A1.
- **Model — LATER TASK (deferred 2026-07-15, bug G32-3D-MODEL)**: a **realistic round 3D tomato mesh** with a green
  calyx, rendered the same in inventory/hand/flight (like a shield/spawn-egg). **Adapt a redistributable-CC0 mesh**
  (simplify to game-weight) or, if none is found clean, **hand-model a low-poly tomato** — procedural/math-sphere
  generation is **rejected**. The CC0 licence MUST permit redistribution (the pack ships to every joining player);
  verify before shipping. Needs a custom Fabric 1.21.1 model/renderer — confirm the approach via web search first
  (CLAUDE.md MANDATORY_WEB_SEARCH). This is a separate art-blocked upgrade, NOT required for A1 to pass this pass.
- **Texture (for the later mesh)**: a **flat unlit photoreal albedo** baked onto the model's UVs, **reusing the
  `tomato_gen.py` skin/freckle/calyx colour look** — NOT its current output (which bakes lighting into a 2D sphere; a
  mesh texture must be flat albedo because the game lights the mesh). Real tomato reference for colour, never a copied
  photo. `tomato_gen.py` stays useful as the colour source / 2D-icon tool. 4K stays rejected — detail past ~256 is
  thrown away at the render size.
- **Flight**: Snowball-like arc and gravity. No particle trail (stealthy). Instant throw, no cooldown, spam allowed.
  Inherits player momentum for sprint-jump trick shots. Consumes the stack; free in creative.
- **Airburst**: If it hits nothing, it detonates after **20 seconds** (owner-locked 2026-07-15, was 10s) in mid-air
  rather than flying into unloaded chunks.
- **Riding** *(revised 2026-07-15 — the old "full kamikaze, no dismount" is REVERSED)*: Sneak + throw mounts you.
  **Sneak again and you drop off**, while the tomato flies on without you — vanilla's universal "get off the
  thing" key, same as a horse or boat. Dismount was wired but **regressed on MP** (bugs G32-RIDE-EJECT +
  G32-RIDE-DESYNC): the ride ejects you unprompted after ~1s, and the hotbar stays stuck reading "riding".
  **Dismount is free and unconditional (owner-locked 2026-07-15)** — plain vanilla sneak, no penalty, no
  "fuse already lit so you're stuck" case. If you can sneak, you get off. Target: hold the ride until a 2nd sneak.
  A rider who rides into the blast **without bailing DIES (owner-locked 2026-07-15)** — the blast is lethal. **With
  the fuse removed (B1) there is NO bail window**: contact = instant blast, so the only way to survive is to **sneak
  off before contact**. A rider **still aboard at the 20s airburst also DIES (owner-locked 2026-07-15)** — no
  auto-eject; the mid-air detonation kills them. The ridden tomato gets a speed/distance boost so the ride is a ride.
- **Dispensers**: Fire it at high velocity like a cannon. Dispenser kills credit **nobody** — generic death message,
  no thrower is tracked.

## Phase B — Boom

> 🔒 **Owner-locked blast (2026-07-15) — read before building Phase B.**
> **Power 6.0 normal · `nuke` = 12.0 · knockback ON · no fire · LETHAL point-blank (armour mitigates) · griefing ON
> with a 5s crater restore · NO FUSE (instant on contact).** Config-tunable. The short-lived "radius 3 / ~8 dmg /
> survivable / destruction-off" retune is **dropped** — a point-blank thrown tomato now kills.
>
> ⚠️ **No-damage regression root cause (B2, owner-confirmed 2026-07-15 — fails in SP and MP).** `TomatoEntity.onCollision`
> calls `super.onCollision(hitResult)` first; vanilla `ThrownItemEntity.onCollision` **discards the entity** (and sends
> the item-break status byte) before the fuse can tick → `detonate()` never runs on impact → zero damage, no crater
> anywhere. Only the airburst path (no collision, no discard) ever exploded. **Fix: remove the fuse and call
> `detonate()` directly inside `onCollision` BEFORE discard** — this single change fixes B1 (instant contact blast)
> and B2 (no damage) together.
>
> ⚠️ **Blast engine is a REAL vanilla explosion (owner-locked 2026-07-15).** If any hand-rolled `power 0` +
> custom-falloff path remains, drop it: use a real `createExplosion` at **power 6** (`nuke` → 12) with
> **`ExplosionSourceType` set to destroy** — one power drives crater + damage + knockback like TNT. Hook the
> explosion's **affected-blocks list** to feed the restore snapshot + skip blast-proof blocks. The old decoupled
> `tomatoBlastRadius` / `tomatoBlastDamage` keys go away.
>
> Config keys (single-power schema): `tomatoBlastPower` (6.0, `nuke` → 12.0), **`tomatoBlastKnockback` = `8`
> (owner-locked 2026-07-15 — numeric strength, punchier than vanilla power-6; change the key from a boolean to a
> tunable float)**, `tomatoBlastFire` (false), `tomatoRestoreSeconds` (**5**, was 15). **Magic names: `NUKE`
> only (owner-locked 2026-07-15)** — no other named tiers. **Crops/farmland destruction stays Phase E (row E11),
> NOT this pass (owner-locked 2026-07-15).** Still to build this pass: fuse removal + the
> onCollision-before-discard fix, the SPLAT audio, and the griefing/restore layer.

- **Fuse — REMOVED (owner-locked 2026-07-15, B1)**: there is **no fuse at all** — no ~0.5s tell, no TNT hiss, no
  white flash/swell, no bail window. The tomato **detonates instantly on ANY contact** (block, entity, ground), and
  the airburst detonates instantly at 20s. This reverses the earlier "~0.5s tell" decision outright. **Full removal
  (owner-locked 2026-07-15):** delete the `FUSE` data-tracker field **and** the client flash/swell/hiss renderer path
  — no dormant fuse code left behind. (The SPLAT blast audio below is separate and stays.)
- **Power**: **6.0** normal, `nuke` → **12.0** (case-insensitive exact name match). Terrain destruction like TNT
  at that power; crater restores after **5s** (owner-locked 2026-07-15, was 15s).
- **Damage**: Blast only — no bonus damage for a direct hit. **Lethal point-blank with no armour**, but **armour
  mitigates** (owner-locked 2026-07-15): a player in **full iron survives** a point-blank blast, with heavy armour
  durability loss (acidity). A real vanilla explosion already runs damage through armour, so this comes for free.
  Friendly fire is ON (it hurts the thrower). A death **this pass** shows the plain **vanilla explosion death line**;
  the 30 custom death messages are Phase E, not now (owner-locked 2026-07-15).
- **Audio**: a CC0 squish SPLAT layered over the vanilla TNT bass boom. The 3 in-repo splat `.ogg`s
  (`tomato_squishsplat_impact`, `tomato_squish_03`, `tomato_squishpop`) are a variation pool — **one is picked at
  random per blast** (owner-locked 2026-07-15). All three live at `assets/customblocks/sounds/`.
- **Custom names**: A name given in an anvil hovers over the tomato in flight, always visible like a named mob.
- **Fire/lava**: Explodes normally, does not burn up.

### Anti-griefing

- Blocks are destroyed like TNT and **restore after 5 seconds** (owner-locked 2026-07-15, was 15s; config-tunable). The crater snapshot is held
  **in memory only** — it is intentionally lost on server restart.
- **Anti-dupe**: blocks dropped by the blast are tracked and **deleted from the player's inventory** when the crater
  restores, not merely despawned on the ground. Otherwise every throw is free blocks.
- **Overlapping craters: the FIRST snapshot wins.** A later blast must never record blocks that an active snapshot
  already owns — otherwise the restore writes a crater back as if it were original terrain and leaves permanent
  holes. This is the single most likely correctness bug in this group.
- **Restore skips occupied spots**: anything a player built in the hole is never overwritten.
- **Blast-proof**: all CB blocks (SlotBlock, guess showcase, buzzer, deleted markers) AND all containers
  (chests/barrels/shulkers) never break. Nothing spills, so there is no inventory-snapshot dupe surface.
  They may end up floating over a crater — accepted.
- **Entities** (item frames, paintings, boats, armor stands, mobs) are destroyed like vanilla TNT and are
  **not** restored. The anti-grief promise covers blocks only.
- Entities trapped in the restoring crater are safely pushed upward.
- Claim protection is **dropped** (WorldGuard/Towny are Bukkit-only). Respecting an installed Fabric protection mod
  is parked QoL.

## Phase D — Mess

Sauce is a **hybrid**, because a block cannot occupy the same space as a fence or stair:

- **Flat ground → a real 8-layer Block** (snow-style). All the gameplay lives here: slipperiness, eating, piston
  pushing, fall-damage negation. It is a normal registry entry, **not** one of the 1028 slots.
- **Every other surface** (walls, ceilings, fences, stairs, slabs) **→ a client-side decal**, visual only.
  The server sends **one "splat here" packet per blast** and each client draws its own decals. Never spawn hundreds
  of server entities.

Details:

- **Texture**: 64x, uniform deep red. A deliberate style clash with 16x vanilla; owner accepted this.
- **Slipperiness**: slipperier than ice (ice is 0.98; sauce ~0.995).
- **Drying**: sauce fades and vanishes after **15 seconds** (config-tunable). Rain instantly washes away exposed sauce.
- **Drips**: sauce on walls flows downward, slower than water and faster than lava.
- **Debris**: physics tomato chunks that roll, bounce, and squish.
- **Fluids**: underwater blasts damage terrain normally and create floating clumps that rise; lava burns sauce off
  instantly with steam.
- **Eating**: sneak + right-click a puddle to eat it — soup-tier food (6 hunger, high saturation), consumes one layer.
- **Fall damage**: thick puddles fully negate it.
- **Cap**: 50 puddles per chunk (config-tunable); oldest evicts first.
- **No interop**: sauce and craters are fully independent of every other CB system. CB tools ignore sauce entirely.

## Phase C — Deflect & Interception

- **Punching a mid-air tomato**: a **50/50** coin flip — either it deflects perfectly back at the thrower like a
  Ghast fireball, or it detonates in your face.
- **Interception**: hit by an arrow or snowball → it explodes immediately like flak. Two tomatoes colliding mid-air
  both explode.

## Phase E — Combat & Entities

- **Slipping**: all entities slide wildly on sauce. Walking out leaves a red footprint trail that lasts ~5s and fades.
- **Hostile mobs**: full damage, knockback, then **completely stunned for ~3s** (Slowness 255 + AI disabled), then
  they slip.
- **Villagers**: if their homes are hit they flee in terror, show angry clouds, and raise trade prices — via vanilla
  gossip, so the anger decays naturally rather than being permanent.
- **Tamed pets**: wolves, cats, and parrots take zero explosion damage, but still get sauced and slip.
- **Crops**: farmland crops are destroyed like TNT.
- **Shields**: blocking negates the blast damage, but sauce still splatters around you, and the shield gets a
  **red tint overlay** while sauced (a renderer mixin — not a custom shield model, so banner shields keep working).
- **Screen overlay**: a direct hit paints red sauce dripping down the **edges** of your screen, center stays clear,
  then dries fast. Deliberately not a blinding pumpkin-blur.
- **Death messages**: 30 custom messages, **uniformly random** per death.

### The 30 death messages

1. `<Player> was turned into ketchup by <Thrower>`
2. `<Player> got sauced by <Thrower>`
3. `<Player> was marinara-ed by <Thrower>`
4. `<Player> slipped into the sauce dimension thanks to <Thrower>`
5. `<Player> experienced a high-velocity tomato from <Thrower>`
6. `<Player> was squashed by <Thrower>'s explosive tomato`
7. `<Player> got lost in the sauce, courtesy of <Thrower>`
8. `<Player> became spaghetti topping for <Thrower>`
9. `<Player> took a tomato to the face from <Thrower>`
10. `<Player> was pureed by <Thrower>`
11. `<Player>'s armor couldn't handle <Thrower>'s acidic tomato`
12. `<Player> got completely splattered by <Thrower>`
13. `<Player> was obliterated by <Thrower>'s flying fruit`
14. `<Player> tried to catch <Thrower>'s explosive tomato`
15. `<Player> is now soup, cooked by <Thrower>`
16. `<Player> was violently seasoned by <Thrower>`
17. `<Player> couldn't outrun <Thrower>'s tomato barrage`
18. `<Player> was reduced to red paste by <Thrower>`
19. `<Player> got absolutely juiced by <Thrower>`
20. `<Player> took <Thrower>'s food fight too seriously`
21. `<Player> was blasted into salsa by <Thrower>`
22. `<Player> caught <Thrower>'s explosive produce`
23. `<Player> slipped on <Thrower>'s deadly sauce`
24. `<Player> was sent to the produce aisle by <Thrower>`
25. `<Player> couldn't handle the spice of <Thrower>'s tomato`
26. `<Player> was crushed by <Thrower>'s volatile vegetable`
27. `<Player> got thoroughly drenched by <Thrower>'s tomato`
28. `<Player> became a casualty of <Thrower>'s sauce explosion`
29. `<Player> was blown away by <Thrower>'s red menace`
30. `<Player> got deleted by <Thrower>'s high-speed tomato`

## Configuration

All values live in **`config/customblocks/tomato.json`** — its own file, so the shared CB config schema is untouched.
Config is **server-wide, admin-only, and synced to clients**; it is never a client-local preference (explosion power
must not disagree between client and server).

The admin config **GUI is parked** (§F): it gets folded into the `/cb config` chest→Screen migration rather than
being built as a standalone screen here.

## Mixins

Owner has pre-approved these targets. Exact target method + injection point must still be stated and approved
before any are written (CLAUDE.md "Mixin Checkmark"):

- Shield renderer — red tint while sauced.
- HUD render — the sauce screen overlay (try reusing the existing `HudRenderMixin` before adding a new one).
- Footprints rendering.

Slipperiness needs **no** mixin — it comes free from `Block.slipperiness`.

## Parked QoL (§G) — do not build until unparked

- Anvil enchants: Infinity (no consumption) and Multishot (throws 3).
- Stats & leaderboards: "Tomatoes Thrown", "Most Players Splattered".
- Respecting an installed Fabric claim/protection mod.
- A crafting recipe (TNT + food), which would end its command/creative-only status.

## Localisation

**English only** for now. The Arabic pass (including all 30 death messages) is deferred until the text stops churning.
