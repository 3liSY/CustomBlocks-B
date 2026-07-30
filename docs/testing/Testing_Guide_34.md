# Group 34 - Wheel of Fortune

## Status

| | |
| --- | --- |
| **Verdict** | Wheel face is now ONE baked texture instead of ~850 quads — seams cannot step or flicker, and the back reads mirrored. Re-place the wheel and re-check §A. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-07-24 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Vertical wheel structure (place / face / remove); baked 50-wedge face, front and mirrored back | Built 🎯 | - |
| B | Spin: right-click, 50-icon reroll, 8s deceleration | Built 🎯 | - |
| C | Honest landing (arrow points at winner) | Built 🎯 | - |
| D | Prize popup (giant icon + banner + fireworks), floats above the rim | Built 🎯 | - |
| E | Sound (whir + clacks; win ding+fanfare+firework+levelup) | Built 🎯 | - |
| F | Pool + `/cb wheel` item give | Built 🎯 | - |
| G | Claim prize: right-click floating prize (spinner, 1 item, once) | Built 🎯 | - |

**Original Group:** [GROUP_34_WHEEL_OF_FORTUNE.md](../groups/GROUP_34_WHEEL_OF_FORTUNE.md)

---

# Active Tests

## 💡 Setup

- Get the wheel item: `/cb wheel` (gives a wheel item), or creative tab "Wheel of Fortune".
- Place in an open area: the wheel is 20 blocks across and vertical, so it needs ~10 blocks clear in every direction around the point you place, including above.
- Stand back and face the spot before placing — the wheel turns to face you.
- **Spin** = right-click anywhere on the wheel face or on the center arrow.
- **Claim** = after a spin lands, right-click the floating prize (above the top of the wheel). Only the player who spun gets it — one item, once; the prize stays showing until the next spin.
- **Remove** = left-click the center arrow (the anchor block is invisible and has no collision, so the center arrow is the handle). The wheel item comes back unless you are in creative.

## A - Vertical wheel structure - Built 🎯

| | |
| --- | --- |
| **Check** | Placing the wheel item spawns a ~20-block vertical display-entity wheel facing you; the face is one clean 50-wedge disc from either side; removing the center removes all of it. |
| **Pass rule** | Place + remove cleanly twice with no leftover display entities and no overwritten builds; no stepped, flickering or missing seam anywhere, front or back. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb wheel`, place the item in a clear area | A ~20-wide vertical wheel of coloured wedges spawns around that point, flat face turned toward you, arrow at center | 🎯 | 🎯 |
| A2 | Place while facing a different direction | Wheel faces the new direction (placer yaw) | 🎯 | 🎯 |
| A3 | Left-click the center arrow | Every wheel display entity (face, icons, arrow, popup) disappears; nothing else you built is touched | 🎯 | 🎯 |
| A4 | Place a second wheel elsewhere | Singleton enforced — the old wheel comes down, only the new one stands | 🎯 | 🎯 |
| A5 | Look at the wheel face | 50 wedges, each a different vivid saturated colour (none repeated), no two neighbours close in hue | 🎯 | 🎯 |
| A6 | Walk up to the middle and look straight at it | All 50 wedges converge on ONE point — no hole, no sky, no coloured blob or cap sitting over the centre | 🎯 | 🎯 |
| A7 | Look along the rim and between slices | No sky gaps or slivers anywhere between wedges, edge to edge all the way round; the rim is a clean circle | 🎯 | 🎯 |
| A8 | Look at any single wedge | It is a SOLID filled band of colour, not a row of small dots or dashes | 🎯 | 🎯 |
| A9 | Follow one wedge edge from the rim to the centre, standing close | The edge is ONE straight line the whole way — no stair-steps, notches or zigzag anywhere along it | 🎯 | 🎯 |
| A10 | Stand near the hub and strafe / look around for ~10s | Colours are rock steady; no flickering, twitching or swapping between wedges near the centre | 🎯 | 🎯 |
| A11 | Walk round to the BACK of the wheel | The full face shows from behind too — same wedges, same colours, mirrored like looking at the back of a real wheel; not blank, not see-through | 🎯 | 🎯 |
| A12 | From the back, check any icon against the wedge under it | Each icon still sits on its own wedge, same colour as from the front | 🎯 | 🎯 |

## B - Spin (reroll + deceleration) - Built 🎯

| | |
| --- | --- |
| **Check** | Right-clicking the wheel or arrow re-rolls 50 icons and spins the arrow ~8s with real deceleration, then settles. |
| **Pass rule** | Two spins each show a fresh 50-icon ring and a smooth fast→slow→stop arrow (no instant stop). |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Right-click the wheel face | 50 item icons redraw over the wedges, one per wedge; arrow spins fast then decelerates ~8s and settles | 🎯 | 🎯 |
| B2 | Right-click the center arrow | Same spin as clicking the wheel | 🎯 | 🎯 |
| B3 | Right-click again mid-spin | Ignored; current spin not disturbed | 🎯 | 🎯 |
| B4 | Spin several times, watch the ring | Each spin shows a different random 50 (rerolled from the full pool) | 🎯 | 🎯 |
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

## D - Prize popup - Built 🎯

| | |
| --- | --- |
| **Check** | On land, a giant slow-spinning 3D icon + glowing name banner pop in above the wheel's rim (clear of the arrow and the disc), with a scale animation and fireworks ring; holds until next spin. |
| **Pass rule** | Popup shows the correct item, floats clear above the wheel touching nothing, names it directly above the icon, animates in (not a flat appear), and stays until the next spin overwrites it. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Spin and watch the prize | Giant item icon slowly rotates above the top of the wheel, NOT touching the arrow or the wedges | 🎯 | 🎯 |
| D1b | Read the prize name | Name banner glows (lime outline) directly ABOVE the icon and close to it — not below, not drifting off | 🎯 | 🎯 |
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
| **Check** | `/cb wheel` gives a wheel item; the 50-icon ring only ever draws survival-obtainable vanilla items. |
| **Pass rule** | No command blocks / barriers / spawn eggs ever appear on the ring over many spins; the landed item is handed out ONLY through the claim (§G), never automatically. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | `/cb wheel` | A wheel item is added to your inventory | 🎯 | 🎯 |
| F2 | `/cb wheel list` | Reports the pool size and that 50 are re-rolled onto the ring each spin | 🎯 | 🎯 |
| F3 | Spin ~20 times, watch the ring + landings | Every icon is an obtainable vanilla item (no command block, barrier, spawn egg, etc.) | 🎯 | 🎯 |
| F4 | Land on an item, then don't claim | Result is shown; nothing is auto-given. The item reaches a player only via the claim (§G) | 🎯 | 🎯 |

## G - Claim prize - Built 🎯

| | |
| --- | --- |
| **Check** | After a spin lands, right-clicking the floating prize gives ONE of the won item to the player who spun, once; the popup stays until the next spin. |
| **Pass rule** | The spinner gets exactly one item on the first right-click; a second click and any non-spinner get only a message; a new spin clears the old prize before a fresh one lands. |
| **Pass mark** | ⏳ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Spin, then right-click the floating prize | You (the spinner) receive one of the won item; chat confirms "Claimed ..." | 🎯 | 🎯 |
| G2 | Right-click the same prize again | "already claimed" message; no second item is given | 🎯 | 🎯 |
| G3 | (MP) A different player right-clicks the prize | "only the player who spun can claim" message; they receive nothing | ➖ | 🎯 |
| G4 | With no prize showing, right-click where the prize floats | Wheel spins normally (claim box falls through to spin); no error | 🎯 | 🎯 |
| G5 | Left-click the floating prize | Wheel is NOT removed — only the centre arrow takes it down; you get the "hit the centre arrow" message | 🎯 | 🎯 |

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
