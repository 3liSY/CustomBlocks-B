# Group 32 — Explosive Tomato

**🧊 DEFERRED (owner-locked 2026-07-15):** tomato is **set aside until Group 04 (TG4 + G4) is fully built + tested**. Rework below is locked + documented, ready to build **after** Group 04 lands.  
**🛠️ Queued (not built):** owner retest 2026-07-15 found regressions + locked a rework (discussion-only): A1 flip+unify sprite · A4 20s · B1 fuse REMOVED + 5s restore · B2 no-damage root-caused  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                           |
| --------------- | ------------------------- |
| **Verdict**     | 🛠️ Rework locked 2026-07-15 (discussion pass, **queued to build, none built yet**): A1 = fix upside-down billboard + inventory/hand use the **same round sprite** (true 3D mesh deferred → G32-3D-MODEL); A4 = ride/airburst **20s** (was 10s); B1 = **fuse REMOVED**, instant blast on any contact, crater restore **5s** (was 15s); B2 = no-damage regression root-caused (`super.onCollision` discards before the blast runs) → detonate in `onCollision` before discard, same fix as B1. Blast stays power 6 / nuke 12, no fire, LETHAL (armour mitigates), griefing ON |
| **Progress**    | 🛠️🛠️✅✅✅✅✅ · §A: A2/A3/A5/A6/A7 passed; A1 + A4 queued · §B: B1/B2 queued (fuse-removal + onCollision fix), B3/B4/B5 blocked, B6 parked QoL |
| **Last tested** | 2026-07-15 (MP)           |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

> Rows MUST be sorted by priority, never alphabetically by §: 🎯 → 💔/❔/❗ → 🟡/⚠️ → ✅ → 🛠️ → 🧊 → ⏳.
> The active test-now row is the thing to do today — it must never be buried below history.

| § | What                                                                       | State      |
| --- | -------------------------------------------------------------------------- | ---------- |
| A | **Fly** — Core projectile, model, throw, riding, dispenser, command           | 🛠️ A1 flip+unify + A4 20s queued |
| B | **Boom** — Instant contact blast (no fuse), explosion, nuke, 5s crater restore, anti-dupe, blast-proofing | 🛠️ B1/B2 fix queued 2026-07-15 |
| F | Admin Config GUI — folded into the `/cb config` screen migration              | 🧊 parked   |
| G | QoL — enchants, stats/leaderboards, crafting, protection-mod respect          | 🧊 parked   |
| D | **Mess** — Sauce blocks, decals, drips, debris, slipping, eating              | ⏳ after B  |
| C | Deflect & Interception mechanics                                              | ⏳ planned  |
| E | Combat Effects (stuns, footprints, shield, screen overlay, death messages)    | ⏳ planned  |

🔗 **Related Doc:** [GROUP_32_EXPLOSIVE_TOMATO.md](../groups/GROUP_32_EXPLOSIVE_TOMATO.md)

---

# 🎯 Test now

**A · Fly** — Core Projectile & Throwing. First custom entity in the mod.

💡 The tomato throws snowball-style with no cooldown. Base Phase-A behaviour was "vanish on impact, give up after 10s in the air"; Phase B turns both into the boom — instant blast on contact, and a **20s** airburst (owner-locked 2026-07-15) if it hits nothing.

### 🧰 Setup Required
* Op yourself (`/op <you>`) — the command requires Admin/OP.
* Give yourself the item: `/cb tomato @s 64`, or grab it from the CB tools creative tab.
* A **fresh client** is required — this is the mod's first custom entity, so an old client will not know how to draw it.

| #  | Action                          | Expected Result                                            | SP  | MP  |
| -- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| A1 | `/cb tomato @s 64`, throw one, then look at it **in inventory/hand and in flight** | This pass (owner-locked 2026-07-15): the flight billboard's **upside-down flip is fixed**, and the **inventory/hand icon uses the SAME round sprite as flight** — no more crappy faceted-cube icon; the two match. A true 3D mesh is a **separate later task** (G32-3D-MODEL), not required to pass this row | 🛠️  | 🛠️  |
| A4 | Sneak and throw, then **sneak again**; also throw one at nothing and wait | Mount + ride; the throw-sneak no longer ejects you, **sneaking again drops you off** free/unconditional while the tomato flies on. Ride/airburst lifetime is **20s** (owner-locked 2026-07-15, was 10s); if it hits nothing it **detonates in mid-air** at 20s | 🛠️  | 🛠️  |
| A2 | Throw the tomato                | Snowball-like arc, no cooldown, no particle trail (stealthy)| ✅  | ✅  |
| A3 | Sprint-jump and throw           | Inherits player momentum — flies faster and further         | ✅  | ✅  |
| A5 | Break a chest full of tomatoes  | They drop as ordinary items                                 | ✅  | ✅  |
| A6 | Fire from a Dispenser           | Shoots out at high velocity like a cannon                   | ✅  | ✅  |
| A7 | Run `/cb tomato` as non-op      | Command is refused (OP level 2 gate)                        | ✅  | ✅  |

> ⚠️ **A1 this pass = flip fix + unify sprite (owner-locked 2026-07-15, NOT built yet).** The flight billboard
> currently renders **upside-down** (renderer UV `v` coords inverted — swap them), and inventory/hand use an ugly
> faceted-cube 16px JSON instead of the round 256px sprite. Fix both: correct the flip **and** point inventory/hand
> at the **same round sprite** as flight, so all three surfaces match. See bug **G32-A1-FLIP-ICON**.
> A true **3D tomato mesh** (green calyx; flat photoreal albedo baked onto its UVs; redistributable-CC0 mesh or a
> hand-modelled low-poly one — procedural rejected) is a **deferred later task**, not required for A1 to pass this
> pass. Candidate CC0 sources: Meshy / OpenGameArt / Sketchfab (verify the licence permits redistribution — the pack
> ships to every player). See bug **G32-3D-MODEL**.

## B · Boom — the core blast · 🎯 built 2026-07-15, test-now

> 💡 **FUSE REMOVED (owner-locked 2026-07-15).** No ~0.5s tell, no flash/swell, no bail window — the tomato
> **explodes instantly on ANY contact** (block, entity, ground). **Full removal (owner-locked 2026-07-15):** delete
> the `FUSE` data-tracker field AND the client flash/swell/hiss renderer path — no dormant fuse code. Real vanilla
> explosion, power 6 (nuke = 12), destruction ON, no fire. **Lethal point-blank, armour mitigates** (full iron
> survives). Crater **restores after 5s** (was 15s). A death shows the vanilla explosion line — 30 custom messages
> are Phase E.
> A **rider still aboard at the 20s airburst DIES (owner-locked 2026-07-15)** — no auto-eject; the mid-air
> detonation kills them, same 'no bail window' rule as a contact hit.
>
> ⚠️ **B2 root cause found (2026-07-15):** `TomatoEntity.onCollision` calls `super.onCollision`, and vanilla
> `ThrownItemEntity.onCollision` **discards the entity** (+ sends the item-break status byte) before the fuse can
> tick — so impact detonation never runs → no damage, no crater anywhere. Removing the fuse and detonating
> **directly in `onCollision` before discard** fixes B1 + B2 together.

| #  | Action | Expected Result | SP | MP |
| -- | --- | --- | --- | --- |
| B1 | Throw a tomato at a dirt wall from a few blocks away, then wait 5s | It **detonates the instant it touches anything** — no fuse, no warning — and blasts a crater like TNT (griefing wanted), then the crater **restores after 5s**. CB blocks + containers nearby never break | 🛠️ | 🛠️ |
| B2 | Stand at full health (**no armour**) and detonate one at your feet | Real power-6 explosion: point-blank with no armour is **lethal**; in **full iron you survive**, armour takes heavy durability. (Was giving **zero damage anywhere** — see root cause above) | 🛠️ | 🛠️ |
| B3 | Ride a tomato into a wall | ⛔ **Blocked** until B1/B2 land. Fuse is gone, so there is **no bail window** — riding into anything = **instant death**; you survive only by **sneaking off before contact**. Retest after unblock | 🧊 | 🧊 |
| B4 | Throw one straight up, at nothing | ⛔ **Blocked** until B1/B2 land. Airbursts at **20s** in mid-air (instant, no fuse) instead of flying on forever | 🧊 | 🧊 |
| B5 | Stand well outside the blast (~8+ blocks) and detonate one | ⛔ **Blocked** until B2 damage is confirmed. Then: zero damage/knockback out there; real power-6 reach is larger than the old radius-3 | 🧊 | 🧊 |
| B6 | Edit `tomatoBlastPower` / `tomatoRestoreSeconds` in the config, reload, throw again | **QoL — parked** (owner 2026-07-15). Keys: `tomatoBlastPower` (6, `nuke`=12), `tomatoBlastKnockback`, `tomatoBlastFire` (false), `tomatoRestoreSeconds` (now **5**) | 🧊 | 🧊 |
| B7 | Name a tomato "Test" in an anvil, throw it | **Added to this batch (owner 2026-07-15).** Hovering "Test" name is visible mid-air over the projectile. Already built — testable once B2 damage lands | 🛠️ | 🛠️ |
| B8 | Name a tomato "NUKE", throw it | **Added to this batch.** Power jumps to **12.0** (case-insensitive) — bigger crater + lethal radius than a normal power-6 throw. Already built | 🛠️ | 🛠️ |
| B15 | Blow up an item frame / boat / armor stand | **Added to this batch.** Destroyed like vanilla TNT and **NOT restored** (anti-grief covers blocks only) | 🛠️ | 🛠️ |
| B17 | Break a chest full of tomatoes | **Added to this batch.** They still drop as **safe items** — a broken chest is not a chain reaction (moved from A5) | 🛠️ | 🛠️ |

---

# 🗄️ Archive

<details><summary>⏳ <b>Planned</b> (click to open)</summary>

**B · Boom — the rest of it (fuse, sounds, naming, griefing)**

> ⏳ **The crater half of §B is REOPENED (owner-locked 2026-07-15): griefing is wanted.** Block destruction
> goes back ON — nearby blocks crater like TNT, then the crater **restores after 5s** (owner-locked 2026-07-15, was
> 15s; anti-grief). **B9–B14 and B16 are back in scope** (restore, anti-dupe, blast-proofing). The build shipped
> destruction OFF (`ExplosionSourceType.NONE`); reversing that is the job.
> ⚠️ **Blast = real vanilla explosion, power 6.0 · `nuke` = 12.0 · destruction ON · knockback on · no fire ·
> LETHAL point-blank (but armour mitigates — full iron survives).** The short-lived hand-rolled "radius 3 / ~8 dmg /
> power-0-cosmetic / destruction-off" build is dropped; the current `TomatoEntity.detonate()` gets rewritten to a
> real `createExplosion` at power 6 with the affected-blocks list hooked for restore. **The fuse is DROPPED entirely
> (owner-locked 2026-07-15) — instant blast on contact, no tell/flash/swell.** Only the random **SPLAT audio** below
> is still to build this pass.

| #   | Action                          | Expected Result                                            | SP  | MP  |
| --- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| ~~B2~~ | Watch a tomato land          | ⛔ **DROPPED (owner-locked 2026-07-15).** No fuse tell — the ~0.5s hiss/flash/swell is removed; the tomato detonates **instantly on contact**. (Airburst also detonates instantly at 20s.) | ⛔ | ⛔ |
| B3  | Listen to the explosion         | One of the 3 splat `.ogg`s (**random pick per blast**) layered over a heavy bass TNT boom. Building this pass — vanilla explosion sound today. (Kept — SPLAT audio is separate from the removed fuse) | 🎯  | 🎯  |
| B5  | Throw into fire or lava         | Explodes normally (does not burn up or cook)                | 🟥  | 🟥  |
| B6  | Wear armor and take a hit       | Survives in full iron; armor takes huge durability damage   | 🟥  | 🟥  |
| B7  | Name tomato "Test" and throw    | → **Promoted to Test now (this batch, 2026-07-15).** See live B7 | ⤴ | ⤴ |
| B8  | Name tomato "NUKE" and throw    | → **Promoted to Test now (this batch).** See live B8 | ⤴ | ⤴ |
| B15 | Blow up an item frame / boat    | → **Promoted to Test now (this batch).** See live B15 | ⤴ | ⤴ |
| B17 | Break a chest full of tomatoes  | → **Promoted to Test now (this batch).** See live B17 | ⤴ | ⤴ |
| B9–B14, B16 | Crater restore / anti-dupe / blast-proofing | ✅ **Already built** (`TomatoCraterManager` restore + first-snapshot-wins + anti-dupe; `TomatoBlastBehavior` blast-proof CB blocks + containers). Was blocked only by the B2 no-damage bug; verified this batch via **B1** (crater restores 5s, CB blocks/containers survive) | 🛠️ | 🛠️ |

**D · Mess — Realistic Sauce Physics**

💡 Flat ground gets a real 8-layer sauce block (64x texture, dries in 15s); every other surface gets a client-drawn decal.

| #   | Action                          | Expected Result                                            | SP  | MP  |
| --- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| D1  | Explosion on flat ground        | Chaotic splatters generate; sauce piles up in layers        | 🟥  | 🟥  |
| D2  | Sauce a fence, stair, or wall   | Sauce clings as a decal to every surface shape, not just flat ground | 🟥  | 🟥  |
| D3  | Explosion on a vertical wall    | Sauce drips down slower than water, faster than lava        | 🟥  | 🟥  |
| D4  | Explosion near a wall           | Bouncing/rolling tomato chunk entities spawn and scatter    | 🟥  | 🟥  |
| D5  | Detonate underwater             | Normal terrain damage; sauce clumps float to the surface    | 🟥  | 🟥  |
| D6  | Splatter sauce onto lava        | Sauce burns off instantly with steam                        | 🟥  | 🟥  |
| D7  | Wait 15s, then trigger rain     | Sauce fades in opacity and dries; rain washes it instantly  | 🟥  | 🟥  |
| D8  | Sneak + right-click sauce       | One layer is eaten; soup-tier hunger + saturation restored  | 🟥  | 🟥  |
| D9  | Push sauce with a piston        | Piston moves the sauce block                                | 🟥  | 🟥  |
| D10 | Fall onto a thick sauce puddle  | Player takes exactly 0 fall damage (cushioned)              | 🟥  | 🟥  |
| D11 | Spawn 51 puddles in one chunk   | Caps at 50; oldest puddle vanishes to make room             | 🟥  | 🟥  |
| D12 | Place a CB tool on sauce        | No interop — sauce is inert terrain, CB tools ignore it     | 🟥  | 🟥  |

**C · Deflect & Interception**

| #  | Action                          | Expected Result                                            | SP  | MP  |
| -- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| C1 | Shoot an arrow at mid-air tomato| Tomato explodes immediately mid-air (flak cannon)           | 🟥  | 🟥  |
| C2 | Two players throw at each other | Both tomatoes collide mid-air and explode                   | 🟥  | 🟥  |
| C3 | Punch a mid-air tomato, 10x     | Roughly 50/50: deflects back like a Ghast fireball, or detonates in your face | 🟥  | 🟥  |

**E · Combat Effects**

| #   | Action                          | Expected Result                                            | SP  | MP  |
| --- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| E1  | Walk into a sauce puddle        | Player slides uncontrollably — slipperier than ice          | 🟥  | 🟥  |
| E2  | Walk out of the sauce           | Leaves a red footprint trail that fades after ~5s           | 🟥  | 🟥  |
| E3  | Hit a Zombie with the tomato    | Damage, knockback, frozen ~3s (no AI), then it slips        | 🟥  | 🟥  |
| E4  | Hit a Villager's house          | Villager flees in terror, angry clouds, prices rise then decay | 🟥  | 🟥  |
| E5  | Hit a tamed wolf                | Zero damage; pet gets sauced and slips                      | 🟥  | 🟥  |
| E6  | Player blocks with a shield     | No damage; sauce splatters around; shield renders red-tinted| 🟥  | 🟥  |
| E7  | Block with a banner shield      | Red tint applies, banner pattern still renders underneath   | 🟥  | 🟥  |
| E8  | Player takes a direct hit       | Sauce drips down screen EDGES, center stays clear, fades fast | 🟥  | 🟥  |
| E9  | Player dies to explosion        | Random custom death message from the 30 (e.g. "turned into ketchup") | 🟥  | 🟥  |
| E10 | Die to a dispenser-fired tomato | Generic "was splattered" message — no thrower is credited   | 🟥  | 🟥  |
| E11 | Throw onto farmland crops       | Crops are completely destroyed like standard TNT            | 🟥  | 🟥  |

</details>

<details><summary>🧊 <b>Parked — Admin Config GUI (F)</b> (click to open)</summary>

💡 Values live in `config/customblocks/tomato.json` (server-wide, admin-only, synced). The GUI is folded into the `/cb config` chest→Screen migration, not built here.

| #  | Action                          | Expected Result                                            | SP  | MP  |
| -- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| F1 | Open the tomato config in `/cb config` | Custom screen with toggles/sliders, tabs on the LEFT | 🧊  | 🧊  |
| F2 | Change explosion power to 12    | Next normal (unnamed) tomato explodes at nuke size          | 🧊  | 🧊  |
| F3 | Change sauce decay time         | Sauce visibly decays at the new rate                        | 🧊  | 🧊  |

</details>

<details><summary>🧊 <b>Parked — QoL (G)</b> (click to open)</summary>

**G · QoL** — owner parked these; do not build until unparked.

| #  | Action                          | Expected Result                                            | SP  | MP  |
| -- | ------------------------------- | ---------------------------------------------------------- | --- | --- |
| G1 | Anvil Infinity / Multishot      | Infinity = no consumption, Multishot = throws 3 at once     | 🧊  | 🧊  |
| G2 | Check server stats              | "Tomatoes Thrown" + "Most Players Splattered" are tracked   | 🧊  | 🧊  |
| G3 | Throw into a protected region   | If a Fabric protection mod is installed, its rules are respected | 🧊  | 🧊  |
| G4 | Craft a tomato (TNT + food)     | Recipe works; tomato is no longer command/creative-only     | 🧊  | 🧊  |

</details>

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(When a section hits 100% passed, move its tables here)*

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description      | Status |
| ------ | -------- | ----------------------- | ------ |
| G32-RIDE-EJECT | A4 | **Fix built 2026-07-15** — a short arming window suppresses the throw-sneak that was ejecting the rider; the window closes when they release sneak, so a FRESH sneak then dismounts. Retest A4. | 🟡 |
| G32-RIDE-DESYNC | A4 | **Resolved 2026-07-15** — no persistent "riding" message exists in code any more; the mount now shows a ONE-SHOT hotbar cue that fades on vanilla's own timer and is never re-sent, so it can't stick. Retest A4. | 🟡 |
| G32-BLAST-NODMG | B2 | **Root cause found 2026-07-15, fix NOT built yet.** No damage anywhere (owner confirmed SP **and** MP fail). `TomatoEntity.onCollision` calls `super.onCollision`, and vanilla `ThrownItemEntity.onCollision` **discards the entity** (+ item-break status byte) before the fuse can tick, so `detonate()` never runs on impact. Fix = remove the fuse and `detonate()` **directly in `onCollision` before discard** (covers B1 too). Retest B2 after build. | 🎯 |
| G32-A1-FLIP-ICON | A1 | **Not built yet (owner-locked 2026-07-15).** Two defects: (1) flight billboard renders **upside-down** — renderer UV `v` coords inverted; swap them. (2) inventory/hand use an ugly faceted-cube 16px JSON — replace `models/item/explosive_tomato.json` with a **flat 2D `item/generated`** model pointing at the round **256px** entity sprite, so inventory + hand + flight all match (in-hand is a flat billboard card — owner accepted 2026-07-15). **Sprite art LOCKED — owner-provided asset `C:\Users\66664\Desktop\tomato_PNG12567.png` (3531×3451, transparent, glossy hyper-real): downscale to 256px, unify flight + inventory/hand to it (owner-locked 2026-07-15).** Real 3D mesh stays deferred (G32-3D-MODEL). | 🛠️ |
| G32-3D-MODEL | A1 | **DEFERRED later task (not required for A1 this pass).** A true realistic **3D tomato mesh** (green calyx, same in inventory/hand/flight) needs a mesh asset + flat-albedo UV texture authored/sourced outside the build — redistributable-CC0 mesh or a hand-modelled low-poly one; procedural is rejected. The unified flat sprite (G32-A1-FLIP-ICON) ships as the interim; this is the upgrade. | 🧊 |

---
