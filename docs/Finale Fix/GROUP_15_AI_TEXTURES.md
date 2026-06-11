# Group 15 — AI Texture Generation

> **Prerequisite:** Group 02 (Chest GUI) verified. Internet access available on the server.
>
> **Objective:** Wire the AI texture generator using keyless providers (Pollinations.ai primary, Stable Horde anonymous fallback). Replace the old stub `AiCommandParser` and `AiTextureGenerator`. No API key required.
>
> **Source issues:** 17.22 (AI features are stubs), P3 (keyless AI generator), Decision §D (Pollinations.ai + Stable Horde)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing. These tests require a working internet connection.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| AI texture generation | Existed with API key requirement | `AiTextureGenerator` returns null (stub) | Rebuilt with Pollinations.ai (keyless HTTP GET) |
| AI command | `/cb ai <prompt>` existed | Not wired | Restored |
| Fallback provider | None | None | Stable Horde anonymous key (`0000000000`) |
| API key requirement | Required (blocked usage) | N/A | **No key required** — Pollinations.ai is keyless |
| HuggingFace SDXL | Used previously | N/A | Dropped — requires token + heavy rate limits |
| Config fields | `aiApiKey`, `aiWorkerUrl`, `aiServerToken` | Legacy stubs | Removed/replaced: only `aiMaxVariations` (default 3) and `aiTextureStyle` (default "pixel_art") remain |
| Max variations | N/A | N/A | 3 per request (configurable `aiMaxVariations`) |
| Texture style | N/A | N/A | `aiTextureStyle` — default "pixel_art", configurable |

---

## What this group covers

| Feature | Commands |
|---|---|
| AI generate texture | `/cb ai <prompt>` |
| AI variations | `/cb ai <prompt> --variations 3` |
| AI style override | `/cb ai <prompt> --style realistic` |
| Config | `aiMaxVariations`, `aiTextureStyle` |
| Provider fallback | Automatic (Pollinations → Stable Horde) |

---

## Implementation Requirements

### 1. Provider: Pollinations.ai (Primary)

**Endpoint:** `https://image.pollinations.ai/prompt/{encoded-prompt}?width={size}&height={size}&nologo=true`

- Keyless — no authentication required.
- Request the texture size directly from the URL parameters (width × height = configured `textureSize`, default 256).
- HTTP GET, response is a PNG.
- No rate limit key needed.

### 2. Provider: Stable Horde (Fallback)

**Endpoint:** `https://stablehorde.net/api/v2/generate/async` (POST)

- Anonymous key: `0000000000` (hardcoded, no config needed).
- Used automatically if Pollinations.ai request fails (HTTP error, timeout, or returns non-image).
- Async: poll for completion every 2 seconds (on a daemon thread), then download result.

### 3. `/cb ai <prompt>`

1. Player runs `/cb ai glowing blue stone tile, pixel art`.
2. Server sends request to Pollinations.ai.
3. If Pollinations returns a valid PNG within 15 seconds → create a new block using that texture.
4. If timeout or error → fall back to Stable Horde.
5. Progress feedback in chat: `Generating texture… (this may take a few seconds)`
6. On success: `Texture generated! Block "ai_glowing_blue_stone" created.` with clickable `[preview]` and `[keep]`/`[discard]` buttons.

### 4. Variations

`/cb ai <prompt> --variations 3` generates up to 3 different variations. Each shown as a chest GUI slot with the generated texture. Player picks the one they want.

`aiMaxVariations` config field caps this (default 3).

### 5. Style

`aiTextureStyle` config field (default "pixel_art") is appended to every prompt automatically.
Players can override per-request with `--style realistic`, `--style cartoon`, etc.

### 6. Config Fields — Removed

The following old config fields are removed (no longer needed since no key is required):
- `aiApiKey`
- `aiWorkerUrl`
- `aiServerToken`
- `aiTextureEnabled` (AI is now always enabled — no toggle needed since it requires no key)

New fields:
- `aiMaxVariations` (int, default 3)
- `aiTextureStyle` (string, default "pixel_art")

### 7. Error Handling

| Error | Response |
|---|---|
| Both providers unavailable | `"AI texture generation is currently unavailable. Both providers failed."` |
| Prompt returns non-image | `"The AI returned an unexpected result. Try rephrasing your prompt."` |
| Response > 2MB | Downscale to `textureSize` before storing |
| NSFW content | Pollinations.ai has built-in filtering; if a block is detected as NSFW, discard and message player |

---

## Setup

Ensure internet access. No API keys to configure.

---

## Test G15.1 — `/cb ai` generates a texture

```
/cb ai glowing red crystal block pixel art
```

**Expected:** Chat shows `Generating texture…`. Within 15–30 seconds: `Texture generated! Block "ai_glowing_red_crystal_block" created.` with preview and keep/discard buttons.

**Pass:** Block created with an AI-generated texture matching the prompt style.
**Fail:** Command missing, stuck at "Generating…", or error message.

---

## Test G15.2 — AI block is a real block

After G15.1, click `[keep]`.

```
/cb give ai_glowing_red_crystal_block
```

**Expected:** Block item given. Place it in the world — texture is the AI-generated image.

**Pass:** Block works like any other custom block.
**Fail:** Block missing from registry, or no texture.

---

## Test G15.3 — AI discard removes block

Run `/cb ai simple blue tile` again. When the block is generated, click `[discard]`.

**Expected:** The generated block is NOT added to the registry. No block created.

**Pass:** Discard removes the generated block.
**Fail:** Block remains in registry after discarding.

---

## Test G15.4 — Variations

```
/cb ai stone mossy brick --variations 3
```

**Expected:** Chat shows generating message. After completion, a chest GUI opens with 3 variation slots (3 different generated textures). Clicking one creates a block from that variation.

**Pass:** 3 variations shown. Clicking one creates a block.
**Fail:** Only one texture, or no variations chest GUI.

---

## Test G15.5 — Style override

```
/cb ai forest tree --style realistic
```

**Expected:** Generated texture looks more realistic/photographic, not pixel-art style.

**Pass:** Style override visibly affects the output.
**Fail:** Same pixel art output regardless of style flag.

---

## Test G15.6 — Fallback to Stable Horde

*(This test is difficult to force in normal conditions — skip if unable to simulate Pollinations failure.)*

Temporarily set `aiProviderOverride = stable_horde` in config to force the fallback.

```
/cb ai test fallback
```

**Expected:** Stable Horde anonymous provider is used. Block is generated (may take longer — up to 2 minutes). Chat shows progress updates.

Restore `aiProviderOverride` after test.

**Pass:** Block generated via Stable Horde fallback.
**Fail:** Command fails entirely when Pollinations is unavailable.

---

## Test G15.7 — No API key in config

Check `config/customblocks/data/config.json`.

**Expected:** No `aiApiKey`, `aiWorkerUrl`, or `aiServerToken` fields exist. Only `aiMaxVariations` and `aiTextureStyle` are present.

**Pass:** Config clean, no legacy key fields.
**Fail:** Old key fields still present.

---

## Group 15 Verdict

| Test | Description | Result |
|---|---|---|
| G15.1 | AI generates texture from prompt | ⬜ |
| G15.2 | AI block is a real usable block | ⬜ |
| G15.3 | Discard removes generated block | ⬜ |
| G15.4 | Variations shows 3 options | ⬜ |
| G15.5 | Style override changes output | ⬜ |
| G15.6 | Stable Horde fallback works | ⬜ |
| G15.7 | No API key fields in config | ⬜ |

**Group 15 passes when AI texture generation works without any API key, and the fallback chain is functional.**

If anything shows ❌ — paste:
1. The exact prompt used
2. Error message or behavior
3. Last 20 lines of `latest.log`
4. Network connectivity status (can the server reach the internet?)

---

## Cleanup

```
/cb delete ai_glowing_red_crystal_block
```
(Delete any other AI-generated test blocks by ID.)
