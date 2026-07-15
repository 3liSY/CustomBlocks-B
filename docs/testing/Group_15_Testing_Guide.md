# Group 15 — AI Texture Generation

**🎯 Active:** A (0/16)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                 |
| --------------- | ------------------------------- |
| **Verdict**     | 🟡 built — awaiting in-game test |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/16)    |
| **Last tested** | —                               |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                                           | State      |
| --- | -------------------------------------------------------------- | ---------- |
| A | AI tab — describe + generate + keep                            | 🎯 test-now |
| B | Stable Horde fallback · Variations · `--style` · remove legacy | ⏳ planned  |

🔗 **Related Doc:** [GROUP_15_AI_TEXTURES.md](../groups/GROUP_15_AI_TEXTURES.md) *(Assuming standard naming)*

---

# 🎯 Test now

## A · AI tab — describe a block, see it live, keep it · 🎯 0/8

> 💡 No API key. Open with `/cb ai glowing red crystal`. Needs internet on the server.
> 📌 AI is **partial / parked** — provider pivot Pollinations → Cloudflare Flux Schnell under discussion (not built). A is the shipped Pollinations path.

| # | Action                                                               | Expected Result                                                             | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb ai glowing red crystal`                                         | studio opens on **AI** tab; prompt pre-filled; cube shows generated texture | 🎯 | 🎯 |
| A2 | Edit the prompt (e.g. add "mossy")                                   | cube updates ~0.8 s after you stop typing; smooth swap, no flicker          | 🎯 | 🎯 |
| A3 | Click **Regenerate (↻)**                                             | same prompt → visibly different texture on the cube                         | 🎯 | 🎯 |
| A4 | Add id + name (Identity tab) → **Create & Publish**                  | chat says created; block texture matches what you previewed                 | 🎯 | 🎯 |
| A5 | `/cb give <id>` → place it                                           | works like any custom block; texture is the AI image                        | 🎯 | 🎯 |
| A6 | `/cb ai blue tile` → **Cancel / Esc** without creating               | no block added; `/cb list` shows nothing new                                | 🎯 | 🎯 |
| A7 | Open AI tab, generate, press **Create & Publish** with id/name blank | message appears **on screen** (banner/hint) — not a chat line               | 🎯 | 🎯 |
| A8 | `/cb ai` (no prompt)                                                 | studio opens on AI tab, empty, no request fired                             | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | Feature                 | What it'll do                                                                                 | Spec               |
| --- | ----------------------- | --------------------------------------------------------------------------------------------- | ------------------ |
| B | Stable Horde fallback   | anonymous-key provider when Pollinations fails (POST + async poll)                            | GROUP_15 §Deferred |
| B | Variations              | generate several looks, pick one from a grid                                                  | GROUP_15 §Deferred |
| B | `--style` per request   | override the default `aiTextureStyle` inline                                                  | GROUP_15 §Deferred |
| B | Remove legacy AI config | delete `aiApiKey` / `aiWorkerUrl` / `aiServerToken` / `aiTextureEnabled` (touches config GUI) | GROUP_15 §Deferred |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(No sections fully passed yet)*

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

