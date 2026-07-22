# Group 15 - AI Texture Generation

## Status

| | |
| --- | --- |
| **Verdict** | The AI studio tab exists, but it still needs real in-game confirmation and a provider pivot before it is dependable. |
| **Progress** | 🟩🟥🟥🟥🟥🟥🟥🟥🟥🟥 10% |
| **Last tested** | 2026-06-20 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| A | AI studio tab and `/cb ai` entry | Built 🎯 | Discussion ✏️ |
| B | Cloudflare Workers AI provider pivot | Designed ⏳ | Discussion ✏️ |
| C | Recipe picker, draft/final split, and generation log | Designed ⏳ | Parked 💤 |
| D | Post-create refine bar | Planned 📜 | Parked 💤 |
| E | Legacy AI config cleanup | Planned 📜 | Parked 💤 |
| F | Old chat-button AI command flow | Planned 📜 | Scrapped 👎 |

**Original Group:** [GROUP_15_AI_TEXTURES.md](../groups/GROUP_15_AI_TEXTURES.md)

---

# Active Tests

## 💡 Setup

- Use a server with internet access.
- Use `/cb ai` and `/cb ai glowing red crystal`.
- Confirm the AI tab inside the `/cb create` studio, not through chat buttons.
- Create at least one disposable AI block id, then delete it during cleanup.

## A - AI studio tab and `/cb ai` entry - Built 🎯

| | |
| --- | --- |
| **Check** | `/cb ai` opens the Block Creation Studio on the AI tab, previews a generated texture, and only creates a block when the owner publishes it. |
| **Pass rule** | Open, prompt, preview, regenerate, publish, cancel, missing fields, and restart rows pass twice on the target server. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Provider quality and speed still need owner judgement before this can become confirmed. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb ai`. | The studio opens on the AI tab with an empty prompt and no request fired. | 🎯 | 🎯 |
| A2 | Run `/cb ai glowing red crystal`. | The studio opens on the AI tab, the prompt is filled, and generation starts automatically. | 🎯 | 🎯 |
| A3 | Edit the prompt and pause typing. | The preview updates after the debounce without flicker or per-key spam. | 🎯 | 🎯 |
| A4 | Press Regenerate. | The same prompt rolls a visibly different texture. | 🎯 | 🎯 |
| A5 | Fill id/name and press Create & Publish. | Chat confirms creation and the placed block matches the preview result. | 🎯 | 🎯 |
| A6 | Open another AI prompt, then cancel or press Esc. | No block is added and `/cb list` does not show a new id. | 🎯 | 🎯 |
| A7 | Try publishing with missing id or name. | A screen notice explains what is missing; it does not use chat spam. | 🎯 | 🎯 |
| A8 | Restart the game/server after creating an AI block. | The created block reloads with its saved texture and no missing texture warning. | 🎯 | 🎯 |

## B - Cloudflare Workers AI provider pivot - Designed ⏳

| | |
| --- | --- |
| **Check** | The next AI provider should be fast enough and good enough for block textures without pretending the keyless provider is solved. |
| **Pass rule** | Account config, POST fetch, base64 decode, error handling, cache hit, and owner quality rows pass twice after implementation. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Owner wants more discussion before the Cloudflare token/config work is built. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Configure Cloudflare account id and token. | Missing or invalid credentials show one clear screen error and never expose the token. | ⏳ | ⏳ |
| B2 | Generate a texture through Flux Schnell. | The preview appears quickly enough to feel usable and returns a single block-texture result. | ⏳ | ⏳ |
| B3 | Publish the previewed texture. | Create uses the same generated image or a cache hit; it does not ask the provider twice for mismatched art. | ⏳ | ⏳ |
| B4 | Disconnect internet or use a bad token. | The last good preview remains visible and the failure is human-readable. | ⏳ | ⏳ |
| B5 | Compare Cloudflare output with Pollinations output. | Owner can clearly decide whether quality and speed are acceptable. | ⏳ | ⏳ |

## C - Recipe picker, draft/final split, and generation log - Designed ⏳

| | |
| --- | --- |
| **Check** | The AI tab should guide the provider toward block textures instead of random object collages. |
| **Pass rule** | Surface/object recipe, draft, final, log, and stale-response rows pass after the provider pivot. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Parked until the provider is decided. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Choose a surface recipe and generate. | Result favors tileable surface texture framing. | ⏳ | ⏳ |
| C2 | Choose a single-object recipe and generate. | Result favors one centered object without repeated clutter. | ⏳ | ⏳ |
| C3 | Run draft then final quality. | Draft is quick; final is higher quality and keeps the chosen direction. | ⏳ | ⏳ |
| C4 | Open the generation log. | The log explains provider, prompt, seed, timing, and failure reason in copyable text. | ⏳ | ⏳ |

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

- §C Recipe picker, draft/final split, and generation log — 💤 `2026-06-20`: Waiting for provider decision.
- §D Post-create refine bar — 💤 `2026-06-20`: Later editing pass after basic generation is trusted.
- §E Legacy AI config cleanup — 💤 `2026-06-20`: Separate config-screen cleanup after provider direction is final.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- §F Old chat-button AI command flow — 👎 `2026-06-20`: Replaced by the Block Creation Studio AI tab.
- Stable Horde fallback-first plan — 👎 `2026-06-20`: Superseded by the Cloudflare pivot direction.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete any temporary AI test blocks.
- [ ] Remove temporary AI screenshots/log snippets after findings are copied.
- [ ] Keep provider tokens out of reports.
- [ ] Keep screen placement follow-up connected to G27.

</details>
