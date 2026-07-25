# Group 11 - Category System

## Status

| | |
| --- | --- |
| **Verdict** | Category commands are confirmed; the multi-category rework is built, command-only, and awaiting in-game testing. |
| **Progress** | 🟩🟩🟩🟩🟩🟥🟥🟥🟥🟥 50% |
| **Last tested** | 2026-06-14 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Multi-category records and delete safety | Built 🎯 | - |
| B | Unified category commands and baseline category editing | Done ✅ | - |
| C | Category Bulk Retexture tile | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md)

---

# Active Tests

## 💡 Setup

- Create `g11a`, `g11b`, and `g11c`.
- Assign all three to `testcat` with `/cb setcategory`.
- Keep one extra category and one extra block ready for merge/delete checks.
- §A needs a block in two categories at once, an otherwise-uncategorized block, and a category with a sub-category-free flat sibling for merge testing.

## A - Multi-category records and delete safety - Built 🎯

| | |
| --- | --- |
| **Check** | A block can hold several equal categories with no stored main; `Uncategorized` is the unremovable floor; delete offers exactly 3 modes; merge, key collision, filter, tab-complete, `give`, and `info` behave as specified. |
| **Pass rule** | All rows pass twice. Full specification lives in [GROUP_11_CATEGORY.md §B](../groups/GROUP_11_CATEGORY.md); this table is the test plan only. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

**Watch while testing:** old surfaces (HUD, Arabic chest menus, exports, blueprint lore, Bulk Workbench, Category Hub) show one category per block, not the full set. That is the legacy display shadow, by decision — expect the first name alphabetically, not a bug.

Commands: names with spaces are quoted where a mode word follows, e.g. `/cb category info "Arabic Letters" list`. Category-listing order is `/cb category list <mode>`; block order inside one is `/cb category filter <cat> <mode>`.

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb setcategory g11a arabic shop`. | `g11a` gains both memberships; neither is a stored main. | 🎯 | 🎯 |
| A2 | `/cb category remove g11a arabic` on a block only in `arabic`. | `g11a` falls to `Uncategorized`, not an error. | 🎯 | 🎯 |
| A3 | `/cb category delete arabic` on a category with blocks. | Only the `arabic` membership is stripped from each block; other memberships and the blocks themselves are untouched. | 🎯 | 🎯 |
| A4 | `/cb category delete shop exclusive`, then `/cb confirm`. | Only blocks with no other membership are deleted, as one undoable batch. | 🎯 | 🎯 |
| A5 | `/cb category delete shop move arabic`. | Blocks move to the target category before `shop` is removed. | 🎯 | 🎯 |
| A6 | `/cb category merge arabic shop` where a block is in both already. | Merge succeeds; the block ends up in `shop` once, no error. | 🎯 | 🎯 |
| A7 | `/cb category create Arabic Letters`, then `/cb category create arabic-letters`. | Second create is rejected, naming the existing category. | 🎯 | 🎯 |
| A8 | `/cb category filter shop newest`. | Blocks inside `shop` list newest-to-oldest; no argument still gives alphabetical. | 🎯 | 🎯 |
| A9 | Tab-complete a category argument on any `/cb category` or `/cb setcategory` verb. | Suggestions match existing category names only. | 🎯 | 🎯 |
| A10 | `/cb category give shop` where a block has `shop` as one of several memberships. | The block is given, regardless of which membership is "first". | 🎯 | 🎯 |
| A11 | `/cb category info shop` then `/cb category info shop list`. | First shows count/icon/colour/description; second adds the block names. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §B Unified category commands and baseline category editing — ✅ `2026-06-14`

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

- §C Category Bulk Retexture tile — 👎 `2026-07-25`: Tile removed from `CategoryEditMenu`; no bulk retexture flow will be built.
- Scattered `renamecategory` / `mergecategory` / `categorydesc` commands — 👎 `2026-06-14`: Replaced by `/cb category <action>`.
- Old chest category browser target — 👎 `2026-07-09`: Replaced by CategoryHubScreen target.
- Rejected cramped `/cb create` Category tab sample — 👎 `2026-06-30`: Superseded by the wide workspace sample.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g11a`, `g11b`, `g11c`, and temporary categories after testing.
- [ ] Remove stale references to scattered category commands from active docs.
- [ ] Do not re-add a Bulk Retexture tile or a `/cb bulkretexture` route to any category surface.
- [ ] Keep Vault share/import findings linked to G20 (TG20 §K).
- [ ] Keep export ZIP/download-link findings linked to G12 (TG12 §A).
- [ ] Keep CategoryHub/Create-workspace/Export Dashboard screen findings linked to G27 (TG27 §R, §S, §T).

</details>
