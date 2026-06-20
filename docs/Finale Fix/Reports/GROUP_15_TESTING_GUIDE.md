# 🧪 Group 15 — AI Texture Generation — Testing

> 🟢 Build green = compiles + gates pass. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`
> 🟢 **Built 2026-06-20 — build-green + gates pass, deployed to both mods folders.** Tests below are now live (🔴 until you confirm each in-game).

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Built — awaiting in-game test |
| **Progress** | 🟥🟥🟥🟥🟥🟥🟥🟥 · 0 / 8 passed |
| **Last tested** | — |
| **Jar** | 1.0.0 — AI tab shipped 2026-06-20 (build-green; not yet confirmed in-game) |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST (once built)** | **AI tab in the Block Creation Studio** — `/cb ai <prompt>` opens the studio on a new **AI** tab and generates a texture live on the cube; **Create & Publish** = keep, **Cancel** = discard. Keyless (Pollinations.ai). | §1 |
| ⏳ Not built | Stable Horde fallback · variations grid · `--style` flag · remove legacy AI config fields | §4 |

---

# 🎯 Test now

## §1 · AI tab — describe a block, see it live, keep it

> 💡 **What this proves:** you can type a description, watch the texture appear on the preview cube, roll
> variations, and create a real block from it — **no API key, no chat buttons**. The block only exists once
> you press Create.
>
> 🧰 Needs internet on the server. Open with `/cb ai glowing red crystal`.

① **Open + auto-generate** 🔴 — `/cb ai glowing red crystal`
   ✅ The studio opens on the **AI** tab (sits **before Category** in the left list), the prompt is already
   typed in, and the cube shows a generated texture within a few seconds.
   ❌ Opens on a different tab, no AI tab, or the cube stays grey.

② **Live preview as you type** 🔴 — edit the prompt (e.g. add "mossy")
   ✅ ~0.8s after you stop typing, the cube updates to a new texture. Smooth swap — no flicker, no per-keystroke
   spam, the old texture stays until the new one is ready.

③ **Regenerate rolls a new look** 🟡 — click **Regenerate (↻)**
   ✅ Same prompt, visibly different texture on the cube.

④ **Create keeps it — and matches the preview** 🔴 — give it an id + name (Identity tab) → **Create & Publish**
   ✅ Chat says created; the **block's texture matches what you previewed** (same seed re-fetched at full size).

⑤ **It's a real block** 🔴 — `/cb give <id>` → place it
   ✅ Works like any custom block; texture is the AI image.

⑥ **Cancel = discard** 🔴 — `/cb ai blue tile` → **Cancel / Esc** without creating
   ✅ No block added. `/cb list` shows nothing new.

⑦ **On-screen notice, not chat** 🟡 — open the AI tab, generate, then press **Create & Publish** with the id/name blank
   ✅ A message appears **on the screen** (banner/hint) telling you what's missing — not a chat line.

⑧ **Bare open** 🟡 — `/cb ai` (no prompt)
   ✅ Opens the studio on the AI tab, empty, no request fired.

📋 **Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | `/cb ai <prompt>` opens AI tab + auto-generates |
| 🟥 | ② | live debounced preview, smooth (no flicker/spam) |
| 🟥 | ③ | Regenerate = new look |
| 🟥 | ④ | Create keeps it; block matches the preview |
| 🟥 | ⑤ | created block is real (give + place) |
| 🟥 | ⑥ | Cancel creates nothing |
| 🟥 | ⑦ | missing-spec notice is on-screen, not chat |
| 🟥 | ⑧ | bare `/cb ai` opens empty AI tab |
| — | **0 / 8** | |

↩️ **Undo:** `/cb delete <id>` for any block you kept.

---

# ✅ Confirmed

*(none yet — nothing built)*

---

# 🟡 Polish later

*(none yet)*

---

# ⏳ Not built

| Feature | What it'll do | Spec |
|---|---|---|
| Stable Horde fallback | Anonymous-key provider used when Pollinations fails (POST + async poll) | GROUP_15 §Deferred |
| Variations | Generate several looks, pick one from a grid | GROUP_15 §Deferred |
| `--style` per request | Override the default `aiTextureStyle` inline | GROUP_15 §Deferred |
| Remove legacy AI config | Delete `aiApiKey` / `aiWorkerUrl` / `aiServerToken` / `aiTextureEnabled` (touches config GUI) | GROUP_15 §Deferred |

---

## 🆘 If a test fails
- 🔢 Step number (e.g. ②)
- 👀 What happened vs expected (📸 screenshot helps) — and the **prompt** you used
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`
- 🌐 Can the server reach the internet?

## 🧹 Cleanup
```
/cb delete <any ai test block id>
```
