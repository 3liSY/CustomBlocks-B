# Group 11 - Category System

## Status

| | |
| --- | --- |
| **Verdict** | All sections (A, B, C, D) confirmed in-game. Hub-side category work lives in TG27 §U. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-30 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | A block in several categories, and safe deleting | Done ✅ | - |
| B | One `/cb category` command family | Done ✅ | - |
| C | Category commands, chat replies, and Tab suggestions | Done ✅ | - |
| D | Renaming, and descriptions removed | Done ✅ | - |

**Original Group:** [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md)

---

# Active Tests

*(none — every section is confirmed and archived below.)*

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A A block in several categories, and safe deleting — ✅ `2026-07-27`
- §B One `/cb category` command family — ✅ `2026-06-14`
- §C Category commands, chat replies, and Tab suggestions — ✅ `2026-07-30`
- §D Renaming, and descriptions removed — ✅ `2026-07-30`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none currently)* — every item below was fixed, rebuilt and re-confirmed. Kept so the same mistakes are not repeated.

- **Category name colour** — found `2026-07-27`: a colour code typed into a name was saved into the name itself and printed back raw, and a custom colour picked in the Hub never reached chat. Fixed the same day, re-confirmed in §C8.
- **Tab suggestions mixed two things** — found `2026-07-27`: order words and category names came back in one list with nothing marking which was which. Fixed and re-confirmed ✅ `2026-07-27`.
- **First round of §A failures** — found `2026-07-26`: quoted names broke the lookup, `none` kept being suggested after unrelated text, deleting recorded no undo step, and the sort word was still `alpha`. All fixed the same day; details in `PROGRESS_LOG.md`.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §A former rows A4-A6 (`delete … exclusive`, `delete … move <target>`, `merge`) — 💤 `2026-07-26`: replaced by the two-button delete and by `combine`. Rows removed and §A renumbered `2026-07-27`.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Category descriptions — 👎 `2026-07-28`: clutter on every screen and reply that carried them. Removed from the commands, saved data, chat, chest menus and Category Hub. No group inherits them.
- §D5 (old description data left behind) — 👎 `2026-07-30`: no category ever had a description saved on this install, so there was nothing to check.

</details>

**🚚 Moved out**

- §D4 rename from the chest category editor → [TG27 §U13](Testing_Guide_27.md) on `2026-07-30`.
- Clickable-chat rows that need the Hub open → [TG27 §U9-§U11](Testing_Guide_27.md) on `2026-07-28`; their colour, hover and Tab halves became §C8 and §C9.

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g11a`, `g11b`, `g11c`, and the temporary categories.
- [ ] Remove leftover mentions of the old separate category commands from active docs.
- [ ] Do not add a Bulk Retexture button or a `/cb bulkretexture` command to any category screen.
- [ ] Do not bring back quoted category names, order words typed after the name, `merge`, or the old `exclusive` / `move` delete words.
- [ ] Do not put a second confirm step behind the red delete button — clicking it is the confirmation.
- [ ] Do not bring category descriptions back in any form.
- [ ] Do not invent a second joining word for renaming; `into` is shared with `combine` on purpose.
- [ ] Vault share/import problems belong to G20 (TG20 §K).
- [ ] Export ZIP contents belong to G12 (TG12 §A); download-link problems belong to G20 (TG20 §L) as of 2026-07-30.
- [ ] Category Hub, Create-workspace and Export Dashboard screen problems belong to G27 (TG27 §R, §S, §T, §U).

</details>
