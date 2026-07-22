# Group 12 - Export Dashboard & Marketplace

## Status

| | |
| --- | --- |
| **Verdict** | Offline export and Blueprint logic exist; remote download links, Vault sharing, Marketplace, importfolder, and advanced formats still need work. |
| **Progress** | 🟩🟩🟥🟥🟥🟥🟥🟥🟥🟥 20% |
| **Last tested** | 2026-06-21 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| B | Blueprint item export/import on same server | Built 🎯 | - |
| A | Offline export files and dashboard routes | Built 🎯 | Regression 💔 |
| D | Vault share-code import/export | Planned 📜 | Blocked ‼️ |
| E | Marketplace Screen | Planned 📜 | Blocked ‼️ |
| G | `importfolder` image input overhaul | Designed ⏳ | - |
| C | Export Dashboard Screen conversion | Planned 📜 | - |
| F | Advanced `.litematic`, `.schem`, resource-pack, and NBT formats | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_12_EXPORT_MARKETPLACE.md](../groups/GROUP_12_EXPORT_MARKETPLACE.md)

---

# Active Tests

## 💡 Setup

- Create `g12a` and `g12b`, give each a texture, and assign both to `exporttest`.
- Test local export links on the host and remote export links from another client.
- Keep the G27 Export Dashboard Screen target separate from G12's export file logic.

## A - Offline export files and dashboard routes - Built 🎯

| | |
| --- | --- |
| **Check** | Export routes write the correct files to `config/customblocks/cloud_exports/` and link behavior is honest. |
| **Pass rule** | JSON, PNG, all ZIP, category ZIP, local link, and remote link rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Open `/cb export`. | Export dashboard route opens the current export UI; target Screen conversion is tracked in G27. | 🎯 | 🎯 |
| A2 | Export `g12a` as JSON. | `cloud_exports/g12a.json` exists and contains block metadata. | 🎯 | 🎯 |
| A3 | Export `g12a` as PNG. | `cloud_exports/g12a.png` exists and matches the block texture. | 🎯 | 🎯 |
| A4 | Export all blocks to ZIP. | `all-<stamp>.zip` contains the expected block exports. | 🎯 | 🎯 |
| A5 | Export category `exporttest` to ZIP. | Category ZIP contains only category blocks plus metadata/textures. | 🎯 | 🎯 |
| A6 | Click `[download]` from the host machine. | Local download link works when `httpHost` is local. | 🎯 | ➖ |
| A7 | Click `[download]` from a remote client. | Link does not leak an unreachable host; remote strategy is either local-only wording or Vault URL. | ➖ | 🎯 |

## B - Blueprint item export/import on same server - Built 🎯

| | |
| --- | --- |
| **Check** | Blueprint items move one block by item handoff on the same server. |
| **Pass rule** | Generate, inspect, drop/trade, import, duplicate-id guard, and restart rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Run `/cb exportblock g12a` or use the dashboard Blueprint tile. | Blueprint item is given with texture icon and block metadata. | 🎯 | 🎯 |
| B2 | Give/drop the Blueprint to another player and run `/cb importblock` from hand. | Target player imports the block on the same server. | ➖ | 🎯 |
| B3 | Try importing when the id already exists. | Conflict is handled without overwriting silently. | 🎯 | 🎯 |
| B4 | Restart with a Blueprint item in inventory. | Blueprint still imports correctly after restart. | 🎯 | 🎯 |

## G - `importfolder` image input overhaul - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb importfolder` should import images as blocks and apply matching textures to JSON imports. |
| **Pass rule** | PNG, JPG, GIF, WebP, JSON+PNG pairing, bad file, and naming rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | No code yet; full record lives in `docs/Information/IMAGE_INPUT_OVERHAUL.md`. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Put `chair.png`, `table.jpg`, and `lamp.webp` in an import folder, then run `/cb importfolder`. | Each image becomes a block using filename-derived ids and baked textures. | 🎯 | 🎯 |
| G2 | Put `block.json` and matching `block.png` together. | JSON metadata imports and matching PNG texture is applied. | 🎯 | 🎯 |
| G3 | Include a bad or unsupported file. | Bad file is skipped with a clear report; good files still import. | 🎯 | 🎯 |

## D - Vault share-code import/export - Planned 📜

| | |
| --- | --- |
| **Check** | Single-block share codes upload/download through Vault instead of relying on same-server handoff. |
| **Pass rule** | Upload, code length, import, conflict screen, console conflict, and remote server round-trip pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Needs the Cloud Vault worker/deployment path from G20. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Share `g12a` through Vault. | A 6-8 character code is returned. | 🎯 | 🎯 |
| D2 | Import that code on another server or clean setup. | Block imports with texture and metadata. | 🎯 | 🎯 |
| D3 | Import a conflicting id as a player. | Vault conflict screen opens. | 🎯 | 🎯 |
| D4 | Import a conflicting id from console. | Plain text conflict message appears. | ➖ | 🎯 |

## E - Marketplace Screen - Planned 📜

| | |
| --- | --- |
| **Check** | `/cb market` opens a Screen that browses shared Vault blocks. |
| **Pass rule** | Empty state, browsing, preview, import, search/filter, and error rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Marketplace command/screen is unbuilt and depends on Vault. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Run `/cb market`. | Marketplace Screen opens or honestly reports that Marketplace is unavailable. | 🎯 | 🎯 |
| E2 | Browse shared blocks when Vault has entries. | Entries show preview, owner/source info, and import action. | 🎯 | 🎯 |
| E3 | Import from Marketplace. | Block downloads through the same conflict-safe import path. | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

*(none)*

</details>

<details><summary>💔 <b>Regression</b></summary>

- §A Offline export files and dashboard routes — 💔 `2026-06-21`: Remote download links can be unreachable or host-leaky.

</details>

<details><summary>📜 <b>Planned</b></summary>

*(none)*

</details>

<details><summary>💤 <b>Parked</b></summary>

- §F Advanced `.litematic`, `.schem`, resource-pack, and NBT formats — 💤 `2026-06-14`: Heavy binary formats deferred.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g12a`, `g12b`, category `exporttest`, and temporary export files after testing.
- [ ] Keep Export Dashboard screen issues in G27.
- [ ] Keep Vault deployment/share-code issues linked to G20.
- [ ] Do not advertise advanced binary formats until they are actually built.

</details>
