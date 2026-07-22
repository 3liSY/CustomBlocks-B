# Group 34 - Wheel of Fortune

## Status

| | |
| --- | --- |
| **Verdict** | v2 realism rebuild is built; all six sections await owner in-game test. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-07-22 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Vertical wheel structure (place / face / remove) | Built 🎯 | rebuild |
| B | Spin: right-click, 100-icon reroll, 8s deceleration | Built 🎯 | rebuild |
| C | Honest landing (arrow points at winner) | Built 🎯 | rebuild |
| D | Center popup (giant icon + banner + fireworks) | Built 🎯 | rebuild |
| E | Sound (whir + clacks; win ding+fanfare+firework+levelup) | Built 🎯 | rebuild |
| F | Pool + `/cb wheel` item give | Built 🎯 | rebuild |

**Original Group:** [GROUP_34_WHEEL_OF_FORTUNE.md](../groups/GROUP_34_WHEEL_OF_FORTUNE.md)

---

# Active Tests

## 💡 Setup

- Get the wheel item: `/cb wheel` (gives a wheel item), or creative tab "Wheel of Fortune".
- Place in an open area: the wheel is 20 blocks across and vertical, so it needs ~10 blocks clear in every direction around the point you place, including above.
- Stand back and face the spot before placing — the wheel turns to face you.
- **Spin** = right-click anywhere on the wheel face or on the center arrow.
- **Remove** = left-click the center arrow (the anchor block is invisible and has no collision, so the center arrow is the handle). The wheel item comes back unless you are in creative.

## A - Vertical wheel structure - Built 🎯

| | |
| --- | --- |
| **Check** | Placing the wheel item spawns a ~20-block vertical display-entity wheel facing you; removing the center removes all of it. |
| **Pass rule** | Place + remove cleanly twice with no leftover display entities and no overwritten builds. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb wheel`, place the item in a clear area | A ~20-wide vertical wheel of coloured wedges spawns around that point, flat face turned toward you, arrow at center | 🎯 | 🎯 |
| A2 | Place while facing a different direction | Wheel faces the new direction (placer yaw) | 🎯 | 🎯 |
| A3 | Left-click the center arrow | Every wheel display entity (wedges, icons, arrow, popup) disappears; nothing else you built is touched | 🎯 | 🎯 |
| A4 | Place a second wheel elsewhere | Singleton enforced — the old wheel comes down, only the new one stands | 🎯 | 🎯 |

## B - Spin (reroll + deceleration) - Built 🎯

| | |
| --- | --- |
| **Check** | Right-clicking the wheel or arrow re-rolls 100 icons and spins the arrow ~8s with real deceleration, then settles. |
| **Pass rule** | Two spins each show a fresh 100-icon ring and a smooth fast→slow→stop arrow (no instant stop). |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Right-click the wheel face | 100 item icons redraw over the wedges; arrow spins fast then decelerates ~8s and settles | 🎯 | 🎯 |
| B2 | Right-click the center arrow | Same spin as clicking the wheel | 🎯 | 🎯 |
| B3 | Right-click again mid-spin | Ignored; current spin not disturbed | 🎯 | 🎯 |
| B4 | Spin several times, watch the ring | Each spin shows a different random 100 (rerolled from the full pool) | 🎯 | 🎯 |
| B5 | (MP) Spin with a second player watching | Both see the same reroll + same arrow motion land on the same slice | ➖ | 🎯 |

## C - Honest landing - Built 🎯

| | |
| --- | --- |
| **Check** | The arrow's final angle points exactly at one visible icon, and that icon is the announced winner. |
| **Pass rule** | Over 5 spins the arrow tip visibly points at the same item the popup/chat names, every time. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Spin and read where the arrow stops | Arrow tip aligns with one wedge's icon, not between wedges | 🎯 | 🎯 |
| C2 | Compare arrow slice to the popup and the chat line | Popup + `[CB]` chat line name the exact icon the arrow points at | 🎯 | 🎯 |
| C3 | Spin ~10 times | Landing slice varies (truly random), always a real visible icon | 🎯 | 🎯 |

## D - Center popup - Built 🎯

| | |
| --- | --- |
| **Check** | On land, a giant slow-spinning 3D icon + glowing name banner pop in with a scale animation and fireworks ring; holds until next spin. |
| **Pass rule** | Popup shows the correct item, animates in (not a flat appear), and stays until the next spin overwrites it. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Spin and watch the center | Giant item icon slowly rotates; name banner glows (lime outline) below it | 🎯 | 🎯 |
| D2 | Watch the moment it lands | Icon+name pop in with a scale/bounce overshoot; fireworks ring bursts around it | 🎯 | 🎯 |
| D3 | Wait after landing | Popup holds; does not fade until the next spin | 🎯 | 🎯 |
| D4 | Walk around the wheel | Popup icon+name keep facing you (center billboard) | 🎯 | 🎯 |

## E - Sound - Built 🎯

| | |
| --- | --- |
| **Check** | During the spin: whir loop + peg-clacks that space out as it slows. On land: bell ding + fanfare + firework + level-up. |
| **Pass rule** | Spin audio clearly decelerates with the arrow; win audio is a clear multi-layer payoff. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Spin and listen | Whir + clacks play; clacks start as a rattle and space out into single clacks as the arrow slows | 🎯 | 🎯 |
| E2 | Listen at the landing | Bell ding + fanfare + firework + level-up layer together | 🎯 | 🎯 |

## F - Pool + item give - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb wheel` gives a wheel item; the 100-icon ring only ever draws survival-obtainable vanilla items. |
| **Pass rule** | No command blocks / barriers / spawn eggs ever appear on the ring over many spins; the mod never gives the landed item. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | `/cb wheel` | A wheel item is added to your inventory | 🎯 | 🎯 |
| F2 | `/cb wheel list` | Reports the pool size and that 100 are re-rolled onto the ring each spin | 🎯 | 🎯 |
| F3 | Spin ~20 times, watch the ring + landings | Every icon is an obtainable vanilla item (no command block, barrier, spawn egg, etc.) | 🎯 | 🎯 |
| F4 | Land on an item | Result is shown only; nothing is given to any player | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- v1 flat wool-disc wheel — 👎 `2026-07-23`: instant-stop, pre-picked-fake landing, single reel popup. Superseded by v2; see group doc Superseded Decisions.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Remove temporary commands
- [ ] Remove temporary config
- [ ] Remove temporary screens
- [ ] Remove temporary permissions
- [ ] Remove docs references

</details>
