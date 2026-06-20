# Group 15 — AI Texture Generation

> ## ⏸️ STATUS: PARTIAL — PARKED (2026-06-20)
>
> **Priority:** This is a **cool bonus feature, NOT a backbone of the mod.** Lower priority than the
> core block/studio/legacy-restore work. Come back to it later.
>
> **What works:** AI tab UI is built end-to-end; the 20s fetch timeout that caused "couldn't generate"
> is fixed (AI fetch now 60s + retry + WARN log — shipped this session, awaiting in-game confirm).
>
> **Why parked:** the **keyless Pollinations.ai provider is not good enough** — slow (free shared queue,
> 2–40s, unpredictable) and low quality / wrong framing (object collages, cartoon look). Prompt-recipe +
> model tuning helped (see the browser mockup `docs/mockups/ai_tab_mockup.html`) but the provider ceiling
> remains.
>
> **Provider pivot (decided, needs more discussion before building):** move OFF keyless Pollinations TO
> **Cloudflare Workers AI — Flux Schnell** (`@cf/black-forest-labs/flux-1-schnell`). Fast (~1–2s),
> reliable edge infra, free tier (10k neurons/day, no card). Cost: a **free API key** (account ID + token
> in config) — this **breaks the "keyless" goal** in Decision §D below, accepted as the trade for speed/quality.
> Runner-up if needed: Google Gemini "Nano Banana" (`gemini-2.5-flash-image`, ~500/day free). **Owner will
> use Cloudflare but wants to discuss more before implementation.**
>
> **Still TODO when resumed:** (1) finalize Cloudflare decision; (2) add a POST+Bearer+base64 fetch path
> (current `ImageDownloader` is GET-only); (3) port the mockup UX into the Java AI tab — recipe picker
> (surface vs single-object, fixes the "many cats" repeat), draft/Final-HQ split, generation log;
> (4) the post-create **refine bar** ("add a red background"); (5) drop legacy key fields. Speed plan:
> unify preview+create to one size so Create is a cache hit (no second render).

> **Prerequisite:** Group 27 §G27.6 (Block Creation Studio) working. Internet access on the server.
>
> **Objective:** Generate block textures from a text description, **inside the Block Creation Studio**
> as a new **AI** tab. Keyless — uses Pollinations.ai (HTTP GET, no account, no API key). The texture
> previews **live** on the studio's 3D cube; the block is only created when the player hits
> **Create & Publish** (so an unwanted result is simply not kept).
>
> **Source issues:** 17.22 (AI features are stubs), P3 (keyless AI generator), Decision §D (Pollinations.ai).
>
> **Design owner decisions (2026-06-20):** AI is a studio tab, not a chat command flow. Live (debounced)
> generation. AI tab sits **before Category**. `/cb ai <prompt>` opens the studio on that tab and generates
> immediately. No auto-fill of id/name — an **on-screen** notice (not chat) tells the player what's missing.

---

## How this differs from the original stub plan

The first draft of this group was a chat command (`/cb ai <prompt>` → keep/discard buttons → variations
chest GUI → Stable Horde fallback). That is **superseded**. The block creator already has a live-preview
studio (Group 27) and a URL→block rail (Group 04/10) that re-fetches any image URL. AI generation rides
on both: the AI tab turns a prompt into a Pollinations **URL**, the cube previews it, and the existing
**Create & Publish** packet creates the block from that same URL. This removes nearly all the new
server code the stub plan needed.

| Area | Old CustomBlocks | This Group (v1) |
|---|---|---|
| Where AI lives | `/cb ai` chat command | **AI tab** in the Block Creation Studio (`/cb create`) |
| Provider | API-key service (DALL·E / SDXL) | **Pollinations.ai** — keyless HTTP GET, returns a PNG |
| API key | Required | **None** |
| Preview | None | **Live** on the studio cube as you type (debounced) |
| Keep / discard | Chat buttons | **Create & Publish** = keep · **Cancel** = discard (nothing made until Create) |
| Block creation | New code path | **Reuses** `CreateStudioPayload` → `createFromStudio` → `doCreate` (server re-fetches the URL) |
| Config | `aiApiKey`, `aiWorkerUrl`, `aiServerToken`, `aiTextureEnabled` | Adds `aiTextureStyle` (default `pixel_art`). Legacy key fields stay for now (removed in a later slice). |

---

## Provider — Pollinations.ai

**Endpoint:** `https://image.pollinations.ai/prompt/{encoded-prompt}?width={px}&height={px}&nologo=true&seed={seed}`

- Keyless. HTTP GET. Response is a PNG.
- `width`/`height` = the requested texture size.
- `seed` makes the result **deterministic**: the same prompt + seed returns the same image, so the studio
  preview matches the block that Create later re-fetches.
- Fetched through the existing `ImageDownloader` (browser User-Agent, redirects, timeouts) — no new HTTP code.

---

## The AI tab

A new left-sidebar section in `BlockCreationStudioScreen`, placed **before Category**.

1. **Prompt box** — a text field: *"describe the block… e.g. glowing red crystal"*.
2. **Live generation** — ~0.8s after the player stops typing, the tab builds the Pollinations URL and loads
   it onto the cube via the existing `StudioTextureLoader.load(url)`. Details under *Live behaviour* below.
3. **Regenerate (↻)** — bumps the seed to roll a different look for the same prompt.
4. **Generating badge** — a quiet "generating…" indicator reuses the cube's existing status-badge slot.
5. **On-screen notices** (not chat) — empty prompt → a hint in the tab; pressing **Create & Publish** with no
   id/name → an in-screen banner (replaces the old chat warning).

### Prompt enhancement

Every prompt is sent as:

```
<player text>, seamless tileable block texture, <aiTextureStyle>
```

`aiTextureStyle` (new config, default `pixel_art`) is appended so results read as real block textures. The
player never has to type the boilerplate.

### Live behaviour — "smooth, not spammy"

- **Debounce:** fire only ~0.8s after typing stops, and only for prompts ≥ 3 characters.
- **Deterministic seed:** seed derives from the prompt text, so pausing on the same words doesn't re-roll a
  different image each debounce. Regenerate changes the seed deliberately.
- **Stale-guard:** each generation carries a request id; a slow earlier response can't overwrite a newer one.
- **No flicker:** the cube keeps the current texture until the new one fully decodes, then swaps.
- **Preview size:** previews fetch small (256px) for speed. **Create re-fetches at the real `textureSize`**,
  so the published block is full quality. Same seed → same image, just larger.

### `/cb ai [prompt]`

- `/cb ai` → opens the studio on the **AI** tab, empty.
- `/cb ai glowing red crystal` → opens on the AI tab with the prompt pre-filled and the **first generation
  already running**.
- Implemented as a small handler that opens the studio (like `CreationStudioBridge.openStudio`) and carries
  the target tab + prompt to the client.

### Keep / discard

No separate buttons — the studio already has them:

- **Create & Publish** = keep. Sends `CreateStudioPayload(id, name, pollinationsUrl, attrs)`; the server
  re-fetches the seeded URL and creates the block on the shared rail.
- **Cancel / Esc** = discard. Nothing was created (the texture only ever existed as a client preview + a URL).

---

## Config

| Field | Type | Default | Note |
|---|---|---|---|
| `aiTextureStyle` | string | `pixel_art` | Appended to every prompt. |

Legacy `aiApiKey`, `aiWorkerUrl`, `aiServerToken`, `aiTextureEnabled` are **left in place for v1** and removed
in a later slice (they touch the config GUI — see *Deferred*).

---

## Error handling

| Situation | Response |
|---|---|
| Empty / too-short prompt | On-screen hint in the AI tab; no request fired. |
| Provider returns a non-image / fails | Cube keeps the last good texture; badge shows "couldn't generate — try rephrasing". |
| No internet | Same as above; the existing `ImageDownloader` error surfaces as a failed badge. |
| Create with no id/name | On-screen banner ("give it an id and a name first"); nothing sent. |

---

## Deferred to later slices (NOT in v1)

| Item | What it'll add | Why deferred |
|---|---|---|
| Stable Horde fallback | Anonymous-key POST + async polling when Pollinations fails | New HTTP + polling code; v1 proves the core first. |
| Variations | Generate N looks, pick one in a grid | Needs a multi-preview surface; bigger UI. |
| `--style` per-request | Override `aiTextureStyle` inline | Config default covers v1. |
| Remove legacy AI config fields | Delete `aiApiKey` / `aiWorkerUrl` / `aiServerToken` / `aiTextureEnabled` | Touches `ConfigMenu`; do as its own safe pass (G15.7). |

---

## Touch list (for the build)

- `command/handlers/AiCommands.java` (new) — `/cb ai [prompt]`, registered in `CommandRegistrar`.
- Open-GUI path — carry "open studio on AI tab + prompt" to the client (extend `OpenGuiPayload` use / `GuiMode`).
- `client/gui/BlockCreationStudioScreen.java` — new `Section.AI` (before Category), prompt field + Regenerate,
  live-debounce + stale-guard, on-screen notices. Mind the §9.3 500-line gate (split helper out if needed).
- `ai/AiTextureGenerator.java` — repurposed from the null stub into the Pollinations **URL builder**
  (prompt + style + size + seed). No key check.
- `CustomBlocksConfig.java` — add `aiTextureStyle`.

Tests + scorecards live in `Reports/GROUP_15_TESTING_GUIDE.md` (this spec stays status-free).
