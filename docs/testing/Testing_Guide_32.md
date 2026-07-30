# Group 32 - Explosive Tomato

## Status

| | |
| --- | --- |
| **Verdict** | Tomato fly, boom, and interception are confirmed; mess/combat still have five regression rows (D1/D3/D13/E6/E8) plus parked mesh/QoL work. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-07-18 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| B | Boom: instant blast, nuke, knockback, guaranteed one-shot | Built 🎯 | - |
| D | Mess: crater restore, sauce block, decals, eating, fall cushion | Built 🎯 | Regression 💔 |
| E | Combat and entities: pets, villagers, shields, screen, death messages | Built 🎯 | Regression 💔 |
| A | Fly core: throw, ride, airburst, dispenser, command | Done ✅ | - |
| C | Interception: arrow and tomato collision | Done ✅ | - |
| C(3D) | Realistic 3D tomato mesh | Designed ⏳ | Deferred 💤 (sprite is the complete visual requirement) |
| G | Config screen and QoL | Planned 📜 | Parked 💤 |
| H | Compatibility and Arabic localisation | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_32_EXPLOSIVE_TOMATO.md](../groups/GROUP_32_EXPLOSIVE_TOMATO.md)

---

# Active Tests

## 💡 Setup

- Give tomatoes with `/cb tomato <targets> [amount]` or `/cb tomato all [amount]`; OP level 2+ is required.
- Use a disposable world for crater, nuke, sauce, shield, and screen-overlay rows.
- Current config path: `config/customblocks/tomato.json`; default blast power 6, restore 5 seconds, fire false.
- Treat any tomato-caused freeze, kick, timeout, crash, lost restore, or permanent hole as a failed test.

## D - Mess: crater restore and sauce - Built 🎯

| | |
| --- | --- |
| **Check** | Anti-grief crater restore, ground sauce gameplay, decals, drips, debris, eating, and fall cushion stay bounded and safe. |
| **Pass rule** | Every active D row passes in game without lag, timeout, crash, lost restore, or permanent holes. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Throw multiple tomatoes or nukes over one area, then wait for restore. | Every crater restores, including overlapping blasts; no permanent holes remain. | 💔 | 💔 |
| D3 | Blast a vertical wall and watch the drip. | Face-attached sauce streaks trickle down at staggered times (not one uniform wipe), leave a thin trail, form a small bottom puddle, and fully decay by 10s. | 💔 | 💔 |
| D4 | Blast and watch flying tomato chunks. | Debris falls and settles using the shared sauce material. | 🎨 | 🎨 |
| D5 | Blast underwater or on a seabed. | No sauce blocks appear; only a small capped red-dust and bubble burst dissolves while crater restore still works. | ✅ | ✅ |
| D6 | Throw onto lava. | Tomato detonates on lava contact. | ✅ | ✅ |
| D7 | Rain on sauce. | No rain-washing mechanic exists; this removed behavior stays gone. | ➖ | ➖ |
| D8 | Empty-hand right-click a sauce puddle at full hunger, no sneak. | One layer is slurped; hunger fills and health heals to normal max without overheal. | ✅ | ✅ |
| D9 | Blast CustomBlocks blocks and containers. | They are blast-proof and never spill. | ✅ | ✅ |
| D10 | Fall onto sauce at different depths. | Fall damage reduction scales linearly by layer depth; eight layers fully absorb it. | ✅ | ✅ |
| D11 | Spawn 51+ puddles in a chunk and overlap restores. | Oldest puddle evicts at the cap; no freeze, kick, timeout, or crash occurs. | ✅ | ✅ |
| D12 | Walk on sauce. | Sauce is slipperier than ice. | ✅ | ✅ |
| D13 | Blast flat ground and watch puddle spread. | Ground sauce looks like crushed tomato material, spreads once downhill, then stops and decays. | 💔 | 💔 |

## E - Combat and entities - Built 🎯

| | |
| --- | --- |
| **Check** | Pets, villagers, shields, footprints, screen overlay, and death messages match the owner-locked behavior. |
| **Pass rule** | Every active E row passes in game and shield/screen sauce no longer regress. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Walk onto sauce. | All entities slide wildly. | ✅ | ✅ |
| E2 | Walk out of sauce. | Alternating recognizable left/right prints fade after about 5 seconds. | 🎨 | 🎨 |
| E3 | Catch an edge-of-blast hostile. | Guaranteed-kill behavior supersedes rare survivor stun; ordinary hostile test remains safe. | ✅ | ✅ |
| E4 | Hit a villager home and watch trade prices. | Custom anger bump decays to zero over about 90 seconds and refreshes on re-hit. | ✅ | ✅ |
| E5 | Blast a tamed wolf, cat, or parrot. | Pet takes zero blast damage, but still gets sauced and slips. | ✅ | ✅ |
| E6 | Block an incoming tomato with a shield. | Blocker takes no damage, shield durability changes, explosion still affects terrain/others, and screen splatters. | 💔 | 💔 |
| E7 | Look at the shield after blocking. | Randomized sauce patches remain while banner stays visible, then clear with crater restore. | 🎨 | 🎨 |
| E8 | Take a point-blank direct hit. | About 90 percent of the screen gets fresh random sauce with gaps and gravity-drip shapes, fully present the instant the hit lands (no drip-in animation), then fully clears by 5s. | 💔 | 💔 |
| E9 | Die to your own or another player's tomato. | One of 30 credited lines or 30 self-kill lines appears uniformly; dispenser uses generic text. | ✅ | ✅ |
| E10 | Die to a dispenser tomato. | Generic uncredited dispenser death message appears. | ✅ | ✅ |
| E11 | Blast a farm, then wait for restore. | Terrain, water, soil, and crops restore at exact growth stage while player-placed blocks stay. | ✅ | ✅ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Fly core — ✅ `2026-07-18`
- §C Interception — ✅ `2026-07-17`
- §B Boom rows B1-B6 — ✅ `2026-07-18`
- §D Rows D5, D6, D8, D9, D10, D11, D12 — ✅ `2026-07-18`
- §E Rows E1, E4, E5, E9, E10, E11 — ✅ `2026-07-18`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §D D1, D3, D13 — 💔 `2026-07-18`: crater restore, wall drip, and ground sauce failed owner retest. Every tomato/sauce source file was rewritten `2026-07-23`, after that retest; awaiting a fresh D1/D3/D13 run.
- §E E6, E8 — 💔 `2026-07-18`: shield protection and screen sauce failed owner retest. Same `2026-07-23` rewrite covers the shield-blocker exemption; awaiting a fresh E6/E8 run.

</details>

<details><summary>📜 <b>Planned</b></summary>

- §D/E close-out fixes — 📜 `2026-07-18`: The locked build details live in the Group 32 close-out plan.

</details>


<details><summary>💤 <b>Parked</b></summary>

- §G Config GUI and QoL — 💤 `2026-07-18`: GUI ownership belongs with Group 27.
- §H Compatibility and Arabic localisation — 💤 `2026-07-18`: Deferred until tomato text stops changing.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §D7 Rain washing — 👎 `2026-07-17`: Owner removed rain-specific sauce washing.
- §C3 Punch reflection — 👎 `2026-07-17`: Owner removed tomato punch reflection and deflection.
- §D2 Fence/stair sauce-cling test — 👎 `2026-07-18`: Dropped — the blast destroys the very surfaces the row needs. Any D2-only face-hugging decal code is to be deleted (keep the D3 wall-drip path).

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove test tomatoes/items after tests.
- [ ] Use a disposable world for crater/sauce tests.
- [ ] Reset any `tomato.json` values changed during tests.

</details>
