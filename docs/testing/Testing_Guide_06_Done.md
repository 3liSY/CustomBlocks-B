# Group 06 — Tools, Colour Variants & Per-Face Paint

## Status

| | |
| --- | --- |
| **Verdict** | All in-scope Group 06 work is confirmed in-game; §E scrapped. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-20 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Square visual prediction and clean hotbar wording | Done ✅ | - |
| B | Immediate square action-bar timing | Done ✅ | - |
| C | Round-2 tools, hex editor, and per-face paint commands | Done ✅ | - |
| D | Recycle-bin deletion system and no-rejoin refresh | Done ✅ | - |
| F | Omni-Tool in-hand mode switching (sneak+right-click cycle) | Done ✅ | - |
| G | Omni-Tool Face mode (rotate-only) | Done ✅ | - |
| H | Omni-Tool Delete mode (instant, like the red Deleter) | Done ✅ | - |
| I | Omni-Tool Copy mode (grab 4 feel-stats → click to paste) | Done ✅ | - |
| J | Triangle variants, square swaps, and source-store baseline | Done ✅ | - |
| K | Live-block deletion safeguard and boot self-heal | Done ✅ | - |
| E | Background-removal rim, corners, and shadow cleanup | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_06_TOOLS.md](../groups/GROUP_06_TOOLS.md)

---

# Active Tests

*All sections confirmed in-game — nothing left to run. Folded history below.*

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Square visual prediction and clean hotbar wording — ✅ `2026-07-19`
- §B Immediate square action-bar timing — ✅ `2026-07-19`
- §C Round-2 tools, hex editor, and per-face paint commands — ✅ `2026-07-20`
- §D Recycle-bin deletion system and no-rejoin refresh — ✅ `2026-07-20`
- §F Omni-Tool in-hand mode switching — ✅ `2026-07-20`
- §G Omni-Tool Face mode (rotate-only) — ✅ `2026-07-20`
- §H Omni-Tool Delete mode — ✅ `2026-07-20`
- §I Omni-Tool Copy mode (incl. §I3 persistent paste-prompt) — ✅ `2026-07-20`
- §J Triangle variants, square swaps, and source-store baseline — ✅ `2026-06-11`
- §K Live-block deletion safeguard and boot self-heal — ✅ `2026-07-20`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- Old `.dat` migration — 💤 `2026-06-11`

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §E Background-removal rim, corners, and shadow cleanup — 👎 `2026-07-20`: owner no longer recalls the source PNG or repro; dropped for good.
- Old `(Removed)` deletion mechanism — 👎 `2026-06-27`
- Separate held Golden Hexagon admin tool — 👎 `2026-06-21`
- Separate Rainbow Rectangle area identity — 👎 `2026-06-21`
- Held/dropped-item hand glow — 👎 `2026-07-18` (only placed-block glow remains; covered by §A row A4)
- Rename / edit the CB tool items — 👎 `2026-07-18`
- Omni Paint mode — 👎 `2026-07-18`
- Omni Admin mode — 👎 `2026-07-18`
- Omni Face-action backlog (mirror/copy/swap/clone/zoom/tint…) — 👎 `2026-07-18`
- Omni mode-selection Screen — 👎 `2026-07-19`, **reopened `2026-07-20`** → moved to G27 (see TG27 §M); §F in-hand cycling stays until that Screen replaces it.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `vart`, `vart_green`, `vart_black`, custom-hex variants, and painted-face blocks after testing.
- [ ] Keep square wording controlled by G04 but square timing controlled by this TG.
- [ ] Keep trash restore/empty behavior inside G06; only full-server backups cross-link to G09.
- [x] **§I3 paste-prompt fix built + confirmed ✅ `2026-07-20`.** `OmniToolItem.tickCopyPrompts` (END_SERVER_TICK) re-sends the standing "click a block to paste" line every 30 ticks while the player holds the Omni-Tool in Copy mode with a live clipboard, refreshing the hotbar dwell before it fades.
- [ ] **Custom-colour naming fix built `2026-07-19`:** tool labels via `nearestName(rgb)` (e.g. `Crimson Triangle [#AC1155]`) matching the block's snap-to-nearest, not raw hex. Confirm in-game.
- [ ] **Temporary `G06-C` logging still in place** across `HexCommands`, `ColorVariantService`, `ServerPackGenerator`, etc. Remove once the owner confirms §C repaint in-game.

Resolved fixes (D5/K5 restore-heal, §C hex repaint) are folded into the Confirmed archive; root-cause detail lives in `PROGRESS_LOG.md` (G06-C).

</details>
