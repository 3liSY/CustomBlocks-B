# Group 26 - Name, Give & Named-Texture Mirror Fixes

## Status

| | |
| --- | --- |
| **Verdict** | All four fixes are built, but each needs a fresh owner retest before it can return to confirmed. |
| **Progress** | 🟩🟩🟥🟥🟥🟥🟥🟥🟥🟥 20% |
| **Last tested** | 2026-06-20 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Clean display names | Built 🎯 | Discussion ✏️ |
| B | Case-insensitive id resolution | Built 🎯 | Discussion ✏️ |
| C | Named-texture mirror | Built 🎯 | Discussion ✏️ |
| D | Multiplayer display names | Built 🎯 | Discussion ✏️ |

**Original Group:** [GROUP_26_NAME_AND_GIVE_FIXES.md](../groups/GROUP_26_NAME_AND_GIVE_FIXES.md)

---

# Active Tests

## 💡 Setup

- Use a dedicated server for multiplayer display-name rows.
- Use one fresh block with underscores and one mixed-case id.
- Watch `config/customblocks/textures_names/` for mirror tests.
- Treat older June passes as historical only; these rows need fresh owner evidence.

## A - Clean display names - Built 🎯

| | |
| --- | --- |
| **Check** | Display names use spaces and Title Case instead of underscores across migrated and new blocks. |
| **Pass rule** | Boot migration, Arabic bundled names, new create, upload/import, rename, id preservation, and repeated boot rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Fresh retest required after stale status reset. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Boot with legacy underscored display names. | Log reports cleaned count on first migration, then 0 on later boots. | 🎯 | 🎯 |
| A2 | Give a bundled Arabic art block such as `arabic_alef_black`. | Item name shows clean words like `Alef Black`, not underscores. | 🎯 | 🎯 |
| A3 | Create `/cb create g26name Test_black`. | `/cb list`, item, and editor show `Test Black`. | 🎯 | 🎯 |
| A4 | Rename a block with underscores. | Display name is cleaned while id remains unchanged. | 🎯 | 🎯 |
| A5 | Import/upload a block whose source name contains underscores. | Display name uses the same clean naming rule. | 🎯 | 🎯 |
| A6 | Inspect files/ids after name cleanup. | Canonical ids and texture filenames are not changed by display-name cleanup. | 🎯 | 🎯 |

## B - Case-insensitive id resolution - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb give` and other id lookups resolve different capitalization without breaking exact matches. |
| **Pass rule** | Exact, lower, upper, mixed, missing id, duplicate-case tie, and tab-complete rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Fresh retest required after stale status reset. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Create block id `Te`, then run `/cb give Te`. | Exact-case fast path works. | 🎯 | 🎯 |
| B2 | Run `/cb give te` and `/cb give TE`. | Both resolve to the same `Te` block. | 🎯 | 🎯 |
| B3 | Run another existing-id command using different case. | Shared id resolution behaves consistently outside give where applicable. | 🎯 | 🎯 |
| B4 | Run `/cb give zzznope`. | Clear missing-block error appears and no crash occurs. | 🎯 | 🎯 |
| B5 | Create or simulate ids differing only by case if possible. | Tie-break is deterministic and safe. | 🎯 | 🎯 |
| B6 | Check tab completion. | Suggestions keep exact stored casing; fallback only affects resolution. | 🎯 | 🎯 |

## C - Named-texture mirror - Built 🎯

| | |
| --- | --- |
| **Check** | Optional `textures_names/` mirror writes human-readable PNG copies without ever becoming the texture source of truth. |
| **Pass rule** | Enable, status, create, face override, rename, delete, rebuild, off, duplicate names, failure safety, and no-pack-effect rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but never owner-confirmed in-game. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb config mirrornames on`. | Mirror backfills existing textured blocks and reports count. | 🎯 | 🎯 |
| C2 | Inspect `textures_names/`. | Files use clean display names; canonical `textures/slot_N.png` files remain untouched. | 🎯 | 🎯 |
| C3 | Run `/cb config mirrornames`. | Status shows ON/OFF, file count, and folder path. | 🎯 | 🎯 |
| C4 | Create or retexture a block while mirror is on. | A named PNG appears for that block. | 🎯 | 🎯 |
| C5 | Paint a face override. | Mirror writes an additional face-named PNG without overwriting the main file. | 🎯 | 🎯 |
| C6 | Rename the block. | Old mirror filename is removed and new filename appears, with no orphan. | 🎯 | 🎯 |
| C7 | Delete the block. | Its mirror files are removed through the manifest. | 🎯 | 🎯 |
| C8 | Run `/cb config mirrornames rebuild`. | Folder is wiped and regenerated from canonical textures. | 🎯 | 🎯 |
| C9 | Turn mirror off, then create/rename another block. | Existing files remain but new changes do not update the mirror. | 🎯 | 🎯 |
| C10 | Delete or corrupt the mirror folder. | Blocks still render normally because the mirror is write-only. | 🎯 | 🎯 |

## D - Multiplayer display names - Built 🎯

| | |
| --- | --- |
| **Check** | Dedicated-server clients show real custom block names from synced client cache instead of generic fallback text. |
| **Pass rule** | New block item, placed HUD, second client, rejoin, rename live update, singleplayer unchanged, and raw-key rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Fresh dedicated-server retest required after stale status reset. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | On dedicated server, create `d_name` with display name `Royal Banner`. | Hand/hotbar/inventory item shows `Royal Banner`, not `Custom Block`. | ➖ | 🎯 |
| D2 | Place the block and look at it. | HUD/target display shows `Royal Banner`. | ➖ | 🎯 |
| D3 | Have a second player who lacks local data view the item/block. | Second player also sees the real name. | ➖ | 🎯 |
| D4 | Rejoin the server. | Name remains correct after rejoin. | ➖ | 🎯 |
| D5 | Rename to `Royal Flag`. | Item/HUD name updates live without restart. | ➖ | 🎯 |
| D6 | Repeat in singleplayer. | Singleplayer naming remains correct. | 🎯 | ➖ |
| D7 | Inspect for raw fallback. | Never shows `block.customblocks.slot_N`, `Custom Block`, or `Custom Block N`. | 🎯 | 🎯 |

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

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Run `/cb config mirrornames off`.
- [ ] Delete `g26name`, `Te`, `d_name`, and mirror test blocks.
- [ ] Delete `textures_names/` only if the readable mirror is no longer wanted.
- [ ] Keep mirror folder failures from affecting canonical textures.

</details>
