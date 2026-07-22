# Group 22 - Permissions System

## Status

| | |
| --- | --- |
| **Verdict** | Permissions are parked with the main rules decided; the full command tier map still needs owner review before build. |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥🟥🟥 0% |
| **Last tested** | 2026-06-22 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | Full command tier map | Planned 📜 | Discussion ✏️ |
| B | Permission engine and denied message | Designed ⏳ | Parked 💤 |
| C | Vanilla OP fallback test slice | Designed ⏳ | Parked 💤 |
| D | LuckPerms / Fabric Permissions API support | Planned 📜 | Parked 💤 |
| E | Permissions config fields | Designed ⏳ | Parked 💤 |
| F | RP and sync admin command restrictions | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_22_PERMISSIONS.md](../groups/GROUP_22_PERMISSIONS.md)

---

# Active Tests

## 💡 Setup

- Resume only after owner reviews the open tier calls.
- Vanilla OP fallback is the required test environment.
- LuckPerms rows are skipped until LuckPerms is installed.
- Commands must stay visible/tab-completable; denial happens inside execution.
- Denied message must be exactly clean and human: `You don't have permission to use this command.`

## A - Full command tier map - Planned 📜

| | |
| --- | --- |
| **Check** | Every top-level `/cb` subcommand must be assigned to use, edit, or admin before mass wiring. |
| **Pass rule** | Owner resolves the eight judgment calls, sample slice matches the map, and no command is left without a tier. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Owner intentionally parked this before ruling on the eight judgment calls. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Review `lock` and `unlock`. | Owner decides admin or edit. | 📜 | 📜 |
| A2 | Review `fav` and favorites commands. | Owner decides use or edit, consistent with G25 ownership. | 📜 | 📜 |
| A3 | Review `undo` and `redo`. | Owner decides edit or use. | 📜 | 📜 |
| A4 | Review `note` and `lore`. | Owner decides edit or use, consistent with G18. | 📜 | 📜 |
| A5 | Review `ai`. | Owner decides edit or admin, consistent with G15. | 📜 | 📜 |
| A6 | Review export/import commands. | Owner decides edit or admin, consistent with G12/G20. | 📜 | 📜 |
| A7 | Review `particles`, `sounds`, and `feedback`. | Owner decides admin or use, consistent with G16. | 📜 | 📜 |
| A8 | Review Arabic commands. | Owner decides edit or use, consistent with G13/G27. | 📜 | 📜 |
| A9 | Generate final tier table. | Every command has one node `customblocks.command.<name>` and one fallback tier. | 📜 | 📜 |

## B - Permission engine and denied message - Designed ⏳

| | |
| --- | --- |
| **Check** | Commands use a shared permission helper that denies inside execution without hiding commands. |
| **Pass rule** | Helper, visible tab-complete, clean denial, no stack trace, no vanilla unknown-command, and line-count rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Parked until tier map is approved. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Type a blocked command as a non-allowed player and press Tab. | Command remains visible and tab-completable. | ⏳ | ⏳ |
| B2 | Execute a blocked edit command. | Clean denied message appears; no stack trace and no unknown-command response. | ⏳ | ⏳ |
| B3 | Execute an allowed use command. | Command runs normally. | ⏳ | ⏳ |
| B4 | Inspect command wiring approach. | Shared helper avoids hundreds of fragile inline checks. | ⏳ | ⏳ |
| B5 | Check logs after denial. | No noisy exception is written for normal denial. | ⏳ | ⏳ |

## C - Vanilla OP fallback test slice - Designed ⏳

| | |
| --- | --- |
| **Check** | The first slice proves use/edit/admin tiers with vanilla OP levels before wiring every command. |
| **Pass rule** | `create` as edit, `list` as use, `backup` as admin, OP-level changes, and config-tier changes pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Build slice only after A is resolved. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Non-OP runs `/cb list`. | Use-tier command works at OP level 0. | ⏳ | ⏳ |
| C2 | Non-OP runs `/cb create g22a PermTest`. | Edit-tier command is denied cleanly. | ⏳ | ⏳ |
| C3 | OP level 2 runs `/cb create g22a PermTest`. | Edit-tier command succeeds. | ⏳ | ⏳ |
| C4 | OP level 2 runs `/cb backup save test`. | Admin-tier command is denied. | ⏳ | ⏳ |
| C5 | OP level 4 runs `/cb backup save test`. | Admin-tier command succeeds. | ⏳ | ⏳ |
| C6 | Lower `permissionTierEdit` to 0. | Non-OP can create until the tier is restored. | ⏳ | ⏳ |

## D - LuckPerms / Fabric Permissions API support - Planned 📜

| | |
| --- | --- |
| **Check** | Fabric Permissions API should allow LuckPerms nodes when the dependency is installed. |
| **Pass rule** | Dependency, node grant, node deny, fallback absence, and node naming rows pass when LuckPerms is available. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Vanilla OP test is first; LuckPerms test waits until the environment has it installed. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Enable Fabric Permissions API dependency. | Build includes permission API without breaking vanilla fallback. | 📜 | 📜 |
| D2 | Grant `customblocks.command.create` through LuckPerms. | Test player can run `/cb create`. | 📜 | 📜 |
| D3 | Revoke the node. | Same player is denied unless vanilla fallback tier allows it. | 📜 | 📜 |
| D4 | Grant one admin node only. | Player gains that exact command without broad accidental admin access. | 📜 | 📜 |

## E - Permissions config fields - Designed ⏳

| | |
| --- | --- |
| **Check** | Permission tiers are configurable as three OP-level fields and later appear in G27/G21 settings UI. |
| **Pass rule** | Defaults, reload, invalid values, live/next-run behavior, and screen handoff rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Config screen surface is outside this parked slice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Inspect default config. | `permissionTierUse=0`, `permissionTierEdit=2`, `permissionTierAdmin=4`. | ⏳ | ⏳ |
| E2 | Change edit tier to 0. | Edit-tier commands follow the new fallback level. | ⏳ | ⏳ |
| E3 | Enter invalid values. | Values clamp or reject cleanly without breaking permissions. | ⏳ | ⏳ |
| E4 | Open future config Screen. | Permissions fields appear under the correct Settings/Permissions section. | ⏳ | ⏳ |

## F - RP and sync admin command restrictions - Planned 📜

| | |
| --- | --- |
| **Check** | Resource-pack control commands that moved here must be admin-tier only. |
| **Pass rule** | `rp pause`, `rp resume`, `sync`, warning title, and non-admin denial rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Depends on the permissions helper and tier map. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Non-admin runs `/cb rp pause` or `/cb rp resume`. | Command is denied cleanly. | 📜 | 📜 |
| F2 | Admin runs `/cb rp pause` and `/cb rp resume`. | Commands work normally. | 📜 | 📜 |
| F3 | Admin runs `/cb sync`. | Server shows a 3-second title warning before pushing the pack. | 📜 | 📜 |
| F4 | Non-admin runs `/cb sync`. | Command is denied cleanly and no pack push starts. | 📜 | 📜 |

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

- §B Permission engine and denied message — 💤 `2026-06-22`: Parked by owner after main decisions.
- §C Vanilla OP fallback test slice — 💤 `2026-06-22`: Waits for final tier map.
- §D LuckPerms / Fabric Permissions API support — 💤 `2026-06-22`: Waits for environment and first slice.
- §E Permissions config fields — 💤 `2026-06-22`: Waits for permissions slice.
- §F RP and sync admin command restrictions — 💤 `2026-06-22`: Waits for helper and tier map.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete `g22a` and `g22b` after future tests.
- [ ] Restore permission tier config to defaults after experiments.
- [ ] Remove temporary LuckPerms grants after node tests.
- [ ] Keep unresolved tier calls in A until owner decides them.

</details>
