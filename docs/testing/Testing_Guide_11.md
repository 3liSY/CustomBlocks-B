# Group 11 - Category System

## Status

| | |
| --- | --- |
| **Verdict** | Category commands are confirmed; the new CategoryHub screen routing, create-tab workspace, export link, and Vault share/import still need work. |
| **Progress** | 🟩🟩🟩🟩🟩🟥🟥🟥🟥🟥 50% |
| **Last tested** | 2026-06-14 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| D | Category export ZIP download link | Built 🎯 | Regression 💔 |
| E | Category share/import through Vault | Planned 📜 | Blocked ‼️ |
| F | Category Bulk Retexture tile | Planned 📜 | Blocked ‼️ |
| C | `/cb create` Category workspace | Built 🎯 | Polish 🎨 |
| B | CategoryHubScreen routing for category browser | Designed ⏳ | - |
| G | Export Dashboard Screen replacement | Designed ⏳ | - |
| H | Full category rework and migration model | Designed ⏳ | - |
| A | Unified category commands and baseline category editing | Done ✅ | - |

**Original Group:** [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md)

---

# Active Tests

## 💡 Setup

- Create `g11a`, `g11b`, and `g11c`.
- Assign all three to `testcat` with `/cb setcategory`.
- Keep one extra category and one extra block ready for merge/delete/export checks.

## B - CategoryHubScreen routing for category browser - Designed ⏳

| | |
| --- | --- |
| **Check** | Category browser entry points route to `CategoryHubScreen` instead of the old chest menus. |
| **Pass rule** | Categories list, category detail, block row, edit, remove, give, and console fallback pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | CategoryHubScreen exists, but the old `/cb categories` and category browser routing still need migration. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Run `/cb categories` as a player. | CategoryHubScreen opens with all categories, counts, icons, and browse/edit affordances. | 🎯 | 🎯 |
| B2 | Open `testcat`. | The screen shows `g11a`, `g11b`, and `g11c`, with category controls available. | 🎯 | 🎯 |
| B3 | Click a block row. | Give, edit, and remove-from-category actions are available in-screen. | 🎯 | 🎯 |
| B4 | Run the same command from console. | Console receives a text list instead of a screen open attempt. | ➖ | 🎯 |

## C - `/cb create` Category workspace - Built 🎯

| | |
| --- | --- |
| **Check** | The wide Category workspace inside `/cb create` supports the new category system without replacing the current system prematurely. |
| **Pass rule** | Create-tab assignment, category creation, icon/style fields, preview, and migration-safety rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Open `/cb create` and switch to the Category workspace. | Wide workspace appears instead of the rejected cramped panel. | 🎯 | 🎯 |
| C2 | Assign a new block to an existing category from the workspace. | Block saves with the selected main category and appears in category browsing. | 🎯 | 🎯 |
| C3 | Create a new category from the workspace. | Category record is created without deleting or hiding existing categories. | 🎯 | 🎯 |
| C4 | Edit icon/color/description-style fields if present. | Category customization persists and exports as category metadata. | 🎯 | 🎯 |

## D - Category export ZIP download link - Built 🎯

| | |
| --- | --- |
| **Check** | Category export writes correct files and the download link is reachable without leaking the wrong host. |
| **Pass rule** | ZIP contents, chat link, host URL, console behavior, and MP download rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Run `/cb category export testcat`. | ZIP contains category metadata, assigned blocks, textures, and JSON. | 🎯 | 🎯 |
| D2 | Click the `[download]` link. | Link reaches the exported ZIP from the correct server address. | 🎯 | 🎯 |
| D3 | Export from the GUI/screen flow. | Same export result as the command. | 🎯 | 🎯 |

## F - Category Bulk Retexture tile - Planned 📜

| | |
| --- | --- |
| **Check** | The category Bulk Retexture tile must either call a real command or be removed until one exists. |
| **Pass rule** | Tile cannot point at missing `/cb bulkretexture`; build-or-remove decision is verified in-game. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | No `bulkretexture` literal exists today. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Click Bulk Retexture in the category UI. | Either a real working flow opens or the tile is absent/disabled with honest wording. | 🎯 | 🎯 |
| F2 | Try `/cb bulkretexture` manually. | If still unbuilt, it is not advertised as a working feature. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Unified category commands and baseline category editing — ✅ `2026-06-14`

</details>

<details><summary>💔 <b>Regression</b></summary>

- §D Category export ZIP download link — 💔 `2026-07-10`: Link host can be wrong or unreachable.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

*(none)*

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Scattered `renamecategory` / `mergecategory` / `categorydesc` commands — 👎 `2026-06-14`: Replaced by `/cb category <action>`.
- Old chest category browser target — 👎 `2026-07-09`: Replaced by CategoryHubScreen target.
- Rejected cramped `/cb create` Category tab sample — 👎 `2026-06-30`: Superseded by the wide workspace sample.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g11a`, `g11b`, `g11c`, and temporary categories after testing.
- [ ] Remove stale references to scattered category commands from active docs.
- [ ] Keep Vault share/import findings linked to G20.
- [ ] Keep Create Studio host-surface issues linked to G27.

</details>
