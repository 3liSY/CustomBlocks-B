# Group 21 - Config Sync Backend

## Status

| | |
| --- | --- |
| **Verdict** | The `max_blocks` self-heal is confirmed for lower clients; the higher-client no-op row still needs a clean run. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟥🟥🟥 70% |
| **Last tested** | 2026-06-27 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| B | Client-higher no-op check | Built 🎯 | Discussion ✏️ |
| C | Config Screen handoff to G27 | Designed ⏳ | Discussion ✏️ |
| A | `max_blocks` registry self-heal | Done ✅ | - |
| D | Server handshake Phase B | Planned 📜 | Scrapped 👎 |
| E | Old Settings Book ownership in G21 | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_21_CONFIG_GUI.md](../groups/GROUP_21_CONFIG_GUI.md)

---

# Active Tests

## 💡 Setup

- Use two client configurations when possible: one below the server `max_blocks`, one above it.
- This guide tests only the registry-size backend fix.
- `/cb config` Screen content belongs to G27.
- A full Minecraft restart is expected after the client value is raised; hot-adding registry entries is impossible.

## A - `max_blocks` registry self-heal - Done ✅

| | |
| --- | --- |
| **Check** | A client with too few CustomBlocks registry slots is healed to the server count and shown a friendly restart message instead of the raw Fabric kick. |
| **Pass rule** | Lower-client, config write, restart screen, restart/rejoin, same-session retry, and log rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Set client `max_blocks` lower than the server, then join. | The raw unknown-registry error is intercepted. | ➖ | ✅ |
| A2 | Inspect client config after the join attempt. | `max_blocks` is raised atomically to at least the server slot count and is never lowered. | ➖ | ✅ |
| A3 | Read the disconnect/restart message. | It explains server count, old client count, that the setting was updated, and that a full restart is required. | ➖ | ✅ |
| A4 | Restart Minecraft and rejoin. | Client joins cleanly without another prompt. | ➖ | ✅ |
| A5 | Retry joining in the same game session before restart. | The raw kick still does not leak, even though registries remain frozen until restart. | ➖ | ✅ |
| A6 | Check logs. | Log names the detected needed slot count and does not spam repeated heal attempts. | ➖ | ✅ |

## B - Client-higher no-op check - Built 🎯

| | |
| --- | --- |
| **Check** | A client with more slots than the server should join normally and should not be lowered. |
| **Pass rule** | Higher-client, equal-client, no prompt, no write, and log-silence rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | This specific owner row was not run in the 2026-06-27 confirmation. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Set client `max_blocks` higher than the server, then join. | Join succeeds normally; extra local slots are harmless. | ➖ | 🎯 |
| B2 | Inspect client config after join. | Value remains higher and is not lowered to the server value. | ➖ | 🎯 |
| B3 | Set client equal to server and join. | Join succeeds with no self-heal prompt. | ➖ | 🎯 |
| B4 | Check latest log. | No warning, restart prompt, or config write appears. | ➖ | 🎯 |

## C - Config Screen handoff to G27 - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb config` Settings Book work must stay in G27, while G21 remains the backend registry-sync guide. |
| **Pass rule** | G27 owns layout, setting inventory, live apply, restart warnings, and Discord editor surfaces. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Screen behavior is outside G21 by design. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Search G21 for Settings Book UI test ownership. | G21 only references the handoff; active screen tests live in TG27. | ⏳ | ⏳ |
| C2 | Change restart-class fields from the future config Screen. | G27 shows local restart warnings; G21 self-heal still handles other clients joining servers. | ⏳ | ⏳ |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A `max_blocks` registry self-heal — ✅ `2026-06-27`
- §A Friendly restart message — ✅ `2026-06-27`

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

- §D Server handshake Phase B — 👎 `2026-06-27`: Cannot beat the config-phase kick and risks leaking the raw error.
- §E Old Settings Book ownership in G21 — 👎 `2026-07-12`: Moved to G27.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Restore any intentionally edited client `max_blocks` values after testing.
- [ ] Keep config Screen notes in G27.
- [ ] Do not revive Phase B handshake as a registry-kick fix.

</details>
