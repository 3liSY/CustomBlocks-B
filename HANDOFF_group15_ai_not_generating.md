# HANDOFF — Group 15 AI tab "couldn't generate" (NOT a prompt/understanding problem)

**Date:** 2026-06-20
**Status:** ✅ Timeout fix SHIPPED (AI fetch 60s + retry + WARN log; awaiting in-game confirm). But the
deeper outcome: **Group 15 is now PARTIAL / PARKED** — the keyless Pollinations provider is too slow/low
quality. **Decision: pivot to Cloudflare Workers AI (Flux Schnell), pending more discussion.** Group 15 is
a **bonus feature, not a backbone** — revisit later. See `docs/Finale Fix/GROUP_15_AI_TEXTURES.md` status
banner + browser mockup `docs/mockups/ai_tab_mockup.html`. The original timeout analysis below stands.

---

**Original handoff (timeout root cause):** AI tab is built and the UI works end-to-end. Image generation
failed in-game with *"couldn't generate — try rephrasing."* **Root cause: the HTTP timeout was too short
for Pollinations.** Fixed this session.

---

## What the owner saw

`/cb ai glowing red crystal` → studio opens on the **AI** tab, prompt pre-filled, but the cube stays grey
and the badge says **"couldn't generate — try rephrasing."** Every prompt does this. It looks like the AI
"isn't understanding or doing anything."

## What is actually happening

**There is no "understanding" step in the mod, and nothing is broken in the prompt path.** The flow is:

1. The AI tab URL-encodes the words into a Pollinations.ai link
   (`AiTextureGenerator.buildUrl`) — e.g.
   `https://image.pollinations.ai/prompt/glowing%20red%20crystal,%20seamless%20tileable%20block%20texture,%20pixel_art?width=256&height=256&nologo=true&seed=...`
2. `StudioAiPanel` fetches that link off-thread via `StudioTextureLoader.load` → `ImageDownloader.download`.
3. If the fetch fails for **any** reason, `load()` returns `null` and the badge shows
   "couldn't generate." It cannot tell *why* it failed (see "No diagnostics" below).

So "not understanding" really means **"the image never came back, so there was nothing to show."** The
actual image model runs on Pollinations' servers, not in the mod.

## Root cause — the 20-second request timeout

`image/ImageDownloader.java` sets a **20-second** per-request timeout:

```java
.timeout(Duration.ofSeconds(20))   // in fetch(...)
```

Pollinations generates the image **on the fly** on the first request for a given prompt+seed+size (a cache
*miss*). That takes time. Measured today from this machine with the exact URL + the same browser User-Agent
the mod uses:

| Request | Result | Time |
|---|---|---|
| `glowing red crystal …` (fresh) | HTTP 200, `image/jpeg`, valid JPEG | first hit was a cache **Miss** |
| `mossy ancient stone brick …` (fresh, random seed) | HTTP 200, `image/jpeg`, 16.9 KB | **18.5 s** |

18.5 s is **right under** the 20 s limit on a good run. The mod uses a **deterministic seed per prompt**, so
the **first** generation of any prompt is always a fresh miss = always the slow path. With any extra latency
(server load, the owner's connection, a busy Pollinations) it crosses 20 s, the Java HTTP client throws a
timeout, `download()` throws, `load()` returns `null`, and the badge says "couldn't generate." That matches
"it never works."

**Evidence the endpoint/URL/encoding are all correct:** a plain `curl` of the exact URL with the mod's
browser User-Agent returns **HTTP 200, Content-Type `image/jpeg`, real JPEG bytes** (`FF D8 …`). Nothing is
wrong with the link, the prompt encoding, the params, or Pollinations itself — only the timeout.

## ⚠️ The same timeout hits "Create & Publish" too

When a preview eventually succeeds, **Create & Publish re-fetches at 512px** (the preview is 256px). Pollinations
treats a different size as a **different image** → another fresh, slow generation → **the server-side create
fetch also runs through `ImageDownloader`'s 20 s timeout** (`CreationCommands.createWithTexture`). So even after
fixing the preview, Create can still time out and create nothing. **Any fix must cover both the client preview
fetch and the server create fetch.**

## Recommended fix (needs one small decision)

**Option A (recommended): give AI its own longer-timeout fetch (~60 s); leave the global 20 s alone.**
- Add a timeout-aware path in `ImageDownloader` (e.g. `download(url, timeoutSeconds)`), used by the AI preview
  (`StudioTextureLoader` → `StudioAiPanel`) and the AI create path. Keeps normal bad-URL feedback fast (20 s)
  everywhere else.
- While generating, show a state like **"generating… can take ~30s"** instead of failing instantly, so a slow
  (but working) generation doesn't read as an error.

**Option B (simpler, blunter): bump `ImageDownloader`'s request timeout to ~60 s globally.**
- One line, but it slows the error feedback for genuinely-broken URLs across the *whole* mod.

**Either way, also:**
- **Retry once** on failure — Pollinations occasionally 5xx's the first hit.
- **Add diagnostics first** (see below) and confirm in `latest.log` that the live failure is a timeout before
  shipping the fix, so we're certain.

## No diagnostics today (do this first)

`StudioTextureLoader.load` and `StudioAiPanel.fire` **swallow the exception** — a timeout, a 404, and
no-internet all surface as the same grey badge. Before/with the fix, log the URL + the real exception message
(at least at WARN) in the AI fetch path so the in-game failure reason is visible. That turns this from "high
confidence" into "confirmed."

## Files involved

| File | Role in the bug / fix |
|---|---|
| `image/ImageDownloader.java` | The 20 s `.timeout(...)` — the actual cause. Add a timeout-aware overload (Option A) or raise it (Option B). |
| `client/gui/StudioTextureLoader.java` | Calls `download()`; swallows the exception (no diagnostics). |
| `client/gui/StudioAiPanel.java` | AI preview fetch; could use the longer-timeout path + a "still generating" state + log the failure. |
| `command/handlers/CreationCommands.java` (`createWithTexture`) | Server-side Create re-fetch — **also** uses the 20 s timeout; must use the longer path for AI/Pollinations urls. |
| `ai/AiTextureGenerator.java` | URL builder — **correct**, no change needed. |

## Notes

- Pollinations returns **JPEG**, not PNG (the spec assumed PNG). Harmless — `ImageIO` reads both — but worth
  knowing when reasoning about the bytes.
- Same prompt+seed at 256 vs 512 is technically two different cache entries, so preview and the published block
  are generated separately; they should look alike but may differ in fine detail (the already-documented
  tradeoff). Not part of this bug.

## Done this session

- Removed the on-screen line **"Powered by Pollinations.ai — no API key needed"** from the AI tab
  (`StudioAiPanel.render`). Rebuilt green (all gates) and redeployed `customblocks-1.0.0.jar` to both mods
  folders (~11:04).
- **Not** changed: the timeout. That's this handoff's job — it needs the Option A/B decision above.
