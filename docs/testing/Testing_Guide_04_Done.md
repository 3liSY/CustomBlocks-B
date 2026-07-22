# Group 04 - Messages & Command Communication

## Status

| | |
| --- | --- |
| **Verdict** | All Group 04 communication work is confirmed in-game. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-19 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Baseline chat tone, errors, incidents, and suggestion behavior | Done ✅ | - |
| B | DidYouMean command-tree suggestions | Done ✅ | - |
| C | Chat and hotbar feedback contract | Done ✅ | - |
| D | Interactive chat chips using real commands | Done ✅ | - |
| F | Hotbar routing and hand-action feedback | Done ✅ | - |
| H | `/cb rename x x` honest no-op wording | Done ✅ | - |
| E | Human CustomBlocks kick screen | Done ✅ | - |

**Original Group:** [GROUP_04_Communication.md](../groups/GROUP_04_Communication.md)

---

# Active Tests

*All sections confirmed in-game — nothing left to run. Folded history below.*

*(Scope note: Screen action bars are owned and tested by Group 27, not Group 04.)*

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Baseline chat tone, errors, incidents, and suggestion behavior — ✅ `2026-06-21`
- §B DidYouMean command-tree suggestions — ✅ `2026-07-18`
- §C Chat and hotbar feedback contract — ✅ `2026-07-18`
- §D Interactive chat chips using real commands — ✅ `2026-07-18`
- §F Hotbar routing and hand-action feedback — ✅ `2026-07-18`
- §H `/cb rename x x` honest no-op wording — ✅ `2026-07-18`
- §E Human CustomBlocks kick screen — ✅ `2026-07-19`

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

- Voice mode toggle - 👎 `2026-07-12`: Replaced by one professional tone.
- Generic ordinary-message copy chip - 👎 `2026-07-14`: Real copy actions remain; ordinary messages do not get a copy button.
- Separate View chip - 👎 `2026-07-14`: The honest Edit path replaces it.
- Read-only chat direction - 👎 `2026-07-14`: Interactive chat remains.
- Rich hover card - 👎 `2026-07-15`: Dropped as unnecessary.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [x] §E real registry-mismatch kick — done; owner confirmed E1 in-game 2026-07-19 (a real mismatch showed the unified kick screen with working Copy Report). Detail: `PROGRESS_LOG.md`.
- [ ] Add new CustomBlocks kick causes to §E before calling the feature complete.
- [ ] Keep Help and Welcome testing in Group 27 and Group 23.

</details>
