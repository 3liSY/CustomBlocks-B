# Group 15 - AI Texture Generation

> Group 15 lets a creator explore generated block textures inside the Creation Studio, while keeping provider quality, cost, privacy, and published results honest.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_15.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

AI generation should feel like a creative Studio tool, not a chat command that creates unwanted blocks. A prompt produces a preview; only Create & Publish commits a normal CustomBlock. The provider must be fast and controllable enough for the owner before AI is presented as dependable.

G15 owns provider integration, prompt/preview behavior, provider-safe configuration, and generation metadata. G27 owns the AI tab's shared Studio surface and layout.

## Ownership

| Owns | Does not own |
| --- | --- |
| AI provider requests, prompt recipe, seed/cache behavior, and generation errors | Creation Studio Screen framework and layout: G27 |
| Provider credentials and secret-safe configuration | Normal image decode/bake pipeline: G10 |
| Generated preview-to-publish contract | Final block creation and pack delivery: G05/G27 |
| Provider comparison and quality acceptance | General post-create image editing: G10/G27 |

## Direction

AI lives in the Creation Studio AI tab. A debounced prompt request updates the preview without creating a block; Regenerate deliberately changes the result; Cancel leaves no artifact. Publish uses the ordinary Studio creation rail so generated blocks are normal, persistent CustomBlocks.

The keyless Pollinations provider is not the final provider direction because its speed and quality are not dependable enough. The next candidate is Cloudflare Workers AI with Flux Schnell, using owner-controlled credentials and one cached generated result for preview and publish.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-20 | AI generation is a Creation Studio tab, not a chat keep/discard flow. | Preview, publish, cancel, and error feedback stay in the Studio. |
| 2026-06-20 | No block exists until Create & Publish. | Cancel/Esc discards the preview with no registry or pack mutation. |
| 2026-06-20 | Prompt changes debounce and stale responses cannot replace newer work. | The preview remains smooth rather than generating on every keypress. |
| 2026-06-20 | Pollinations is not sufficient as the dependable provider. | A provider pivot is required before AI is promoted beyond a bonus feature. |
| 2026-06-20 | Cloudflare Workers AI with Flux Schnell is the leading pivot candidate. | A free account/token tradeoff is accepted only after the owner finalizes the discussion. |
| 2026-06-20 | Preview and publish should reuse one generated result or cache hit. | Publish does not request mismatched art a second time. |
| 2026-06-20 | Provider tokens never appear in chat, reports, logs, or client-visible error text. | Secret handling remains server/config only. |

## Feature Plan

### A. Studio AI Generation

**Player outcome**

The player can describe a texture, see a stable preview, try another seed, and publish only the result they choose.

**Experience**

- `/cb ai` opens the Creation Studio on the AI tab; an optional prompt starts the first preview.
- Prompt text debounces, preserves the prior good preview until a new image is ready, and ignores stale responses.
- Regenerate rolls a deliberate new seed.
- Missing ID/name, short prompt, offline state, and provider failure show a concise in-screen result.
- Create & Publish uses the normal Studio creation path; Cancel creates nothing.

**Requirements**

- Prompt recipes add texture framing/style guidance without making the player type boilerplate.
- Preview request IDs, seed, source reference, and status are tracked per active Studio session.
- A failed request cannot clear the last good preview or create partial block data.
- Published output records the same generated image/cache result seen in preview.

**Boundary**

G15 creates an image source. G27 and the normal create rail remain responsible for Studio interaction and block creation.

### B. Provider Pivot and Secret Safety

**Player outcome**

Generation becomes quick and usable enough for real creative work without leaking the owner's credentials.

**Experience**

- Provider configuration gives a clear screen error for missing/invalid credentials.
- Cloudflare results arrive quickly enough for live preview and retain a predictable single-texture framing.
- Network/provider failure keeps the last good preview and gives a human-readable reason.
- A provider comparison supports the owner's quality decision rather than silently changing results.

**Requirements**

- The Cloudflare path uses authenticated POST, base64/image decode, timeout, retry, and bounded error handling.
- Account ID/token are server-side secrets and are redacted from every user-facing/reportable message.
- Preview and publish use a generation cache keyed by provider, effective prompt, settings, and seed.
- Provider limits, availability, and response validation fail before any block mutation.

**Boundary**

AI provider setup is optional creator infrastructure. It cannot be required for ordinary texture creation or degrade it when unavailable.

### C. Guided Generation and Refinement

**Player outcome**

After the provider is trusted, creators can guide AI toward useful block art instead of random collage-like images.

**Experience**

- Recipe choices distinguish tileable surfaces from centered single objects.
- Draft and final quality modes balance speed and detail.
- A generation log gives copyable provider, effective prompt, seed, duration, and failure summary without secrets.
- A later refine bar can apply a requested visual change to a committed result.

**Requirements**

- Recipe, quality, seed, and generation history remain session data until publish.
- Refinement produces a new explicit revision rather than overwriting a block invisibly.
- Variations use the same provider/cache/preview contract as the initial prompt.

**Boundary**

These enhancements wait for a provider that meets the basic speed and quality bar.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Published texture delivery | A committed generated image follows the normal pack/client delivery contract. |
| G10 | Image pipeline | Generated responses enter the same validation, resize, and background processing paths as normal sources. |
| G14 | Display/video ideas | G15 does not become a substitute for G14 animation/video decoding. |
| G27 | AI Studio tab | G27 hosts the interaction; G15 provides provider/session state and actions. |
| G28 | Revisions and history | Published/refined results expose clear reversible creation/edit boundaries. |

## Technical Contract

- AI preview is session-scoped and never writes a block/texture slot before publish.
- Each request carries a generation identifier; only the newest matching response may update the preview.
- Provider configuration and tokens are server-side secrets and never serialized to client payloads or logs.
- Publish uses a cached/generated image reference through the normal Studio create service, not a private alternate creation rail.
- Provider responses are bounded, decoded/validated as images, and pass through standard image safety handling before commit.
- G27 UI requests G15 provider actions; it does not construct raw provider requests itself.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Cloudflare provider build | Owner must finalize the provider/config discussion first. | G15 |
| Recipe picker, draft/final, and generation log | Waits for the dependable provider path. | G15 with G27 |
| Post-create refine bar | Follows a trusted base generation flow. | G15 with G10/G27 |
| Legacy AI config cleanup | Needs a separate safe config migration after provider direction settles. | G15 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-20 | AI uses chat keep/discard buttons and variations menus. | AI lives in the Creation Studio tab. |
| 2026-06-20 | Keyless Pollinations is the dependable production provider. | It is a prototype; provider pivot is required. |
| 2026-06-20 | Publish re-fetches a separate generated image. | Preview and publish reuse the same result/cache key. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_15.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [G28 Create Studio](GROUP_28_CREATE_STUDIO.md)
- [Pre-template Group 15 snapshot](../archive/group-migration-2026-07-18/GROUP_15_AI_TEXTURES.md)
