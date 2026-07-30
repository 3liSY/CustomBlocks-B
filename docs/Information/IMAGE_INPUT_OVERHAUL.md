# Image Input Overhaul — Master Record

> **Status: 2026-06-29 — investigated + decided in principle, NO code written yet.**
> This is the single, no-info-lost record of the whole discussion. Plain language first;
> exact technical details kept at the bottom so nothing is lost.
> Owning groups: **Group 14** (image engine), **Group 12** (importfolder), **Group 27** (drag-drop).
> Each of those group docs has a short banner pointing here.

---

## 1. The goal (owner's words)

> "Fix all creating issues for all kinds of images and gifs and all… we want a permanent
> fix for all." Plus: **be able to paste ANY link and have it work** — not just the
> "Copy Image Address" link. Clean link, share link, page link, "copy link address" —
> every kind should work. And the mod must do any needed conversion **itself, invisibly** —
> the player never saves-as-PNG or does any extra step. This is a public mod, so it has to
> "just work" for everyone with no per-server setup.

---

## 2. What started this

Player ran `/cb variants sambosa Sambosa https://i.imgur.com/gP98pZX.jpeg` and got:

> "Couldn't get an image from that URL, so the family was NOT created. Couldn't find a
> direct image on that page. Right-click the image itself → Copy Image Address and use that link."

Owner suspected the image was AVIF and the mod couldn't read it. **That turned out to be wrong** — see below.

---

## 3. The real cause (PROVEN with live tests, not a guess)

The imgur link was a **real, working JPEG**. Nothing wrong with the image.

The bug is in the mod's own download code. When the mod asks imgur for the picture, its
request also says *"I accept web-pages too."* Imgur reads that as "a browser is visiting"
and sends back its **webpage** instead of the **image**. The mod then hunts for an image
inside that webpage, loops, and gives up.

Live test results (same URL, only the "what I accept" line changed):

| What the mod says it accepts | What imgur sends back |
|---|---|
| includes `text/html` (current) | the webpage → **fails** ✗ |
| includes `image/avif` but not `text/html` | the JPEG → works (so AVIF was innocent) |
| only normal image types | the JPEG → **works** ✅ |

**One word — `text/html` — is the trigger.** AVIF had nothing to do with it. This is why it
"errors out for too many things": imgur is one of the most common hosts, and **every**
direct imgur link hits this, plus any site that serves a webpage when it senses a browser.

The simple fix is one line (remove `text/html`, and AVIF, from that "accept" line).
**Owner asked to NOT apply it yet — document first, then go.**

---

## 4. Format reality (what the mod can and can't read today)

| Format | Readable now? | Notes |
|---|---|---|
| PNG, JPG, GIF (incl. animated GIF) | ✅ yes | built into Java |
| WebP (still + animated) | ✅ yes | already bundled (TwelveMonkeys library) |
| HEIC / HEIF (iPhone photos) | ❌ no | no decoder in the build |
| AVIF (common web format) | ❌ no | no decoder in the build; this is the hard one |
| Animated PNG (APNG) | ⚠️ partial | only the first frame shows |

---

## 5. The plan — a layered "image input engine"

Goal: paste any link OR drop any file → a block appears, every time, with the mod doing all
conversion invisibly. Three layers, each useful on its own:

### Layer 1 — The link-fixer (free, offline, no dependencies) — START HERE
Accept **any** link, not just a "Copy Image Address" direct link. Two parts:
1. **Stop sending `text/html`** → direct links (like imgur) stop getting bounced to a webpage.
2. **Per-site rules + read-the-page** → when someone pastes a *page* or *share* link, pull the
   real image out of it. Sites to handle: imgur, Twitter/X, Google Images, Discord, Dropbox,
   Google Drive, Pinterest, plus a generic "read the page's preview image" catch-all (the mod
   already does a basic version of this).

This alone fixes the bulk of "won't create" cases — **including the original imgur one** — and
quietly removes most AVIF too, because AVIF is usually a website *choosing* to send it; when we
ask politely for a normal image, the site sends JPG/PNG instead.

### Layer 2 — Bundle the pure-Java decoders that exist (offline, no setup)
- **WebP** — already bundled. Keep.
- **HEIC** — add a pure-Java HEIC decoder so iPhone photos work. (Matters most for dropped/
  imported local files, less for URLs.) **License must be checked before bundling.**

### Layer 3 — Real AVIF support (the genuinely hard format)
AVIF needs a true AV1 decoder. No free, safe, drop-in pure-Java one exists. Two permanent,
fully-automatic options (the player never does anything in either):

- **(A) Bake the decoder into the mod** — ship a small, **sandboxed** AVIF decoder inside the jar
  (a WebAssembly codec run by a pure-Java engine called Chicory). **Best for a public mod:** one
  jar works on every operating system, no internet, no per-server setup, and a bad/hostile AVIF
  **can't crash the server** because it's sandboxed. Trade-offs: more to build, and a little slow
  per image — but decoding already runs in the background, so nobody waits. **Needs a quick
  feasibility test first** (the engine has no "SIMD" speed-ups yet, so we confirm a no-SIMD build
  is fast enough).
- **(B) Convert in the owner's Cloudflare** — the mod hands the odd file to a Cloudflare Worker
  and gets a PNG back. Easier to build, but leans on Cloudflare (free up to ~5,000 images/month,
  small cost past that) and needs internet. Good as an **interim** while (A) is built.

**Rejected:** bundling native decoder programs per operating system — they can crash a public
server and need different files for Windows/Linux/Mac.

---

## 6. The three features and where each lives

| Feature | Plain description | Home group |
|---|---|---|
| **Image engine** | The link-fixer + bundled decoders + bake-in AVIF (everything in §5). Turn any link/file into a usable image. | **Group 14** (already owns "what formats the mod reads" — it's where WebP was added) |
| **importfolder rework** | Today `/cb importfolder` reads `.json` files only and never applies a texture (even a matching `.png` next to it is ignored). Rework so it accepts image files (png/jpg/gif/webp) and makes a block from each (filename → id, bake the picture), and applies textures to `.json` blocks. | **Group 12** (import/export). Note: importfolder has no real spec anywhere yet — give it one here. |
| **Drag-and-drop create** | Drop an image/gif onto the game → a screen pops up with a preview → name it / pick options → Create. Scope = **both** "drop anywhere, screen auto-opens" **and** "drop onto the already-open create screen". | **Group 27** (owns the `/cb create` studio screen) |

**Important catch for drag-drop:** today the create screen only sends the server a **URL**; the
server downloads it. A dropped file is raw bytes on the player's PC with no link, and there is
**no way to send those bytes to the server yet**. So drag-drop needs a new "send the file to the
server in chunks" pipe built first. It's the biggest of the three.

---

## 7. ✅ Decided

1. Root cause of the imgur failure = the `text/html` line, **not** AVIF. Proven.
2. No manual "save as PNG" — all conversion is automatic and invisible, server-side.
3. Build a layered image engine: link-fixer → bundled WebP/HEIC → real AVIF.
4. "Paste ANY link" is a core requirement, not a nice-to-have.
5. For real AVIF, lean toward **(A) bake-in decoder** as the permanent public-safe answer;
   **(B) Cloudflare** allowed only as an interim.
6. Skip native per-OS decoders.
7. Placement: engine → Group 14, importfolder → Group 12, drag-drop → Group 27.
8. Recommended order: (1) link-fixer + WebP/HEIC, (2) bake-in AVIF, (3) importfolder, (4) drag-drop.
9. The one-line `text/html` fix is **approved in principle but on hold** until docs are done and
   the owner says go.

---

## 8. ✅ `importfolder` spec — owner-decided `2026-07-30`

This is the spec §6 said was missing. Locked in a design session; canonical copy lives in
[GROUP_12_EXPORT_MARKETPLACE.md](../groups/GROUP_12_EXPORT_MARKETPLACE.md) §B, tests in TG12 §B.

1. **Deliberate command, not a watcher.** `/cb importfolder` is run on purpose. No folder polling.
   The owner called this "a safe step I need" over full automation.
2. **Preview before commit.** The run shows every file with the id and display name it will get,
   slots this run uses, slots left after, and every problem marked — *then* waits for confirmation.
   Cancel changes nothing on disk or in the world.
3. **Problems are fixed inline.** Each problem file gets its own clickable line: **rename** (opens
   the anvil typing box, validated by `/cb create` rules), **delete the file**, or **ignore**.
4. **Nothing is ever overwritten.** A name clash is a problem line, never a silent replace.
5. **Naming matches `/cb create` exactly.** Safe ids by the same rules; display names get a capital
   first letter on **every** word — `red_brick.png` → `Red Brick`.
6. **Background removal uses the current configured mode/tolerance.** No import-only special case.
7. **Slot cap: fill what fits, list the rest.** Leftovers appear as retryable clickable lines.
8. **Imported source files move to a done folder.** Never deleted, never reprocessed on re-run.
9. **Uncategorised on arrival**, exactly as `/cb create` leaves a block.
10. **One undo entry per run.** The existing batch undo covers it — no new undo system. Individual
    blocks are removed by a normal delete button in the report, not a special path.
11. **Progress is reported** during long runs; no silent freeze.
12. **No file type aborts the run.** Unusable files are named with a reason and skipped.
13. **Animated GIFs are parked** for their own session — frame caps, size caps, and this server's
    GIF crash history need deciding first. Every decision above covers still images only.
14. **A preview *Screen* is G27's**, later and optional. G12 ships the clickable chat flow.
15. **A dedicated import folder**, not the exports folder — files coming in never mix with files
    that went out. Created on first run with a message saying where to drop images. `done/` sits
    inside it.
16. **Admin-only**, like export.
17. **A zip dropped in the import folder is unpacked** and its images imported like loose files.
    No manual unzipping.
18. **Progress reuses the pack-sync top-center panel** (`SyncProgressOverlay`, G05). Needs a
    server→client progress message; that plumbing is part of this job.
19. **An interrupted run resumes by itself.** Everything finished is already in `done/`, so the
    next run sees only the remainder. No separate resume state to persist or corrupt.
20. **A sizeable run asks G09 for a normal backup first.** No backup code of G12's own. Whether
    *every* risky bulk operation should auto-backup is G09's call, parked in TG09.
21. **The last run's report can be recalled** after chat scrolls away, with its per-block delete
    buttons still live (TG12 §C). Same report kept, not a second reporting system.
22. **No restore path in G12.** Proposed and cut the same day: G09 backup/restore is built and
    confirmed in-game, and a second way to get blocks back is clutter.

---

## 8. 🔴 Still open — needs a decision before/at build time

### 🐛 Layer 1 open bugs (owner in-game test 2026-07-02 — DEFERRED, fix later)

Both built but **not closed**. Left for a later pass at owner's request. Full detail in the Group 14 Testing Guide "Active Bugs" table.

- 🔴 **T9 — pngwing / hostile-CDN 403 NOT confirmed.** The 403 retry (browser-UA → plain-UA fallback) is built + deployed but was **never tested in-game** — owner has no pngwing link right now. To close: paste any `w7.pngwing.com/pngs/…png` into `/cb create t9 Pin <url>` → must create with no `403 Access denied`.
- 🔴 **T7 — Giphy page links don't resolve.** Giphy serves no `og:image` in static HTML (JS-rendered), so the page-reader finds nothing (clean error, no crash). Fix **approved but build deferred**: add a `giphy.com` rule to `LinkResolver.directImageUrl` (URL tail id → `https://media.giphy.com/media/<id>/giphy.gif`). Direct `.gif` links already animate fine; only giphy *page* links are the gap.

### Original open decisions

- **AVIF route not final:** bake-in (A) vs Cloudflare (B). Leaning A, but A needs the feasibility
  test (is the no-SIMD WASM decode fast enough in the background?).
- **HEIC library license:** confirm the pure-Java HEIC decoder (Openize.HEIC) is free to bundle in
  a public mod.
- **Exact link-fixer site list:** confirm the final set of sites + the rule for each.
- **importfolder details:** how to turn filenames into ids, handle duplicate/again-existing ids,
  handle gifs in a folder, and whether to keep the old `.json`-definition import alongside the new
  image import.
- **drag-drop UX:** the pop-up screen's exact design — preview, naming, size/options, gif handling —
  not designed yet (owner wants a design pass).
- **drag-drop upload limits:** how big a file/gif to allow and how to chunk it to the server.
- **Cloudflare interim:** do we want AVIF working via Cloudflare immediately, or wait for bake-in?

---

## 9. Technical appendix (for whoever codes it — exact details, so nothing is lost)

**Single download chokepoint:** `src/main/java/com/customblocks/image/ImageDownloader.java`.
- The "accept" line is in `fetch(...)`, around line 95:
  - current: `image/avif,image/webp,image/png,image/*,text/html,*/*;q=0.8`
  - fix: `image/png,image/jpeg,image/gif,image/webp,*/*;q=0.8` (drop `text/html` and `avif`; keep
    `*/*;q=0.8` so page links still return HTML for the page-reader, and other hosts still serve).
  - Verified live: with the fixed line, `https://i.imgur.com/gP98pZX.jpeg` returns the real JPEG (HTTP 200, `image/jpeg`).
- The error string the player saw ("Couldn't find a direct image on that page") is thrown at ~line 81.
- `followRedirects(NORMAL)` currently follows the imgur 302 into the webpage; after the header fix there's no 302.

**Page/link resolver:** `src/main/java/com/customblocks/image/LinkResolver.java` — reads a page's
`og:image` / `twitter:image` meta tags. This is the base of the link-fixer; extend it with per-site
rules (Twitter `?format=jpg&name=large`, Google Images `imgurl=`, Dropbox/Drive direct forms, etc.).

**Static decode:** `src/main/java/com/customblocks/image/ImageProcessor.java` — `toBlockPng(...)` calls
Java's `ImageIO.read`. Returns null on AVIF/HEIC → the "could not read" error. Wire the new decoders
here as a fallback when `ImageIO.read` returns null.

**Animated decode:** `src/main/java/com/customblocks/image/AnimationDecoder.java` — generic multi-frame
reader; gains animated-WebP/etc automatically as ImageIO plugins are added. Runs off-thread already.

**Formats bundled now:** `build.gradle` includes `com.twelvemonkeys.imageio:imageio-webp:3.12.0`
(+ its transitive twelvemonkeys artifacts). HEIC/AVIF would be added the same way (or via Chicory + a
bundled `.wasm` codec).

**importfolder:** `src/main/java/com/customblocks/core/BlockExporter.java` → `importFolder(Path)` at
~line 246. It filters `*.json` only (line 256) and `applyFields(...)` never calls `TextureStore.save`,
so textures are never applied. `importCategoryZip` (~line 288) extracts `.png` to the folder (line 305)
then `importFolder` ignores them. Reuse `ImageProcessor.toBlockPng` / `AnimationDecoder.decode` +
`TextureStore.save` + `ResourcePackServer.updatePack` to bake images.

**drag-drop networking:** `StudioSavePayload` (and the create flow) send a `url` String, not bytes.
There is no client→server texture-byte channel today (existing `PackFilePayload` etc. are
server→client). A new chunked client→server upload payload is required. The OS file-drop is caught
client-side via GLFW's drop callback (`GLFW.glfwSetDropCallback`) on the Minecraft window. The drop
screen builds on `BlockCreationStudioScreen` (Group 27).

**Async:** the create/variant pipelines already run image work on a background daemon thread
(`CreationCommands`, `ColorVariantService`), so a slow AVIF decode won't freeze the server tick.

**Library findings (researched 2026-06-29):**
- WebP: TwelveMonkeys (pure-Java, already in).
- HEIC pure-Java: Openize.HEIC — pure Java, no native, decodes to BufferedImage; HEIC only (no AVIF);
  license = "Openize License" → verify before bundling.
- AVIF: no free pure-Java decoder. JDeli is pure-Java but commercial. NightMonkeys does AVIF/HEIF but
  needs Java 22 + native libheif/libavif on the system → rejected (mod is Java 21; managed hosts can't
  install natives).
- Bake-in route: Chicory (pure-Java WASM runtime, stable 1.0, **no SIMD yet**) + a WASM AVIF codec
  (jSquash/libavif, or libheif.wasm which does both AVIF and HEIC). Sandboxed; cross-platform; offline.
- Cloud route: Cloudflare Images transform, free up to ~5,000 transforms/month.

---

## 10. Sources (researched 2026-06-29)
- TwelveMonkeys (formats): https://github.com/haraldk/TwelveMonkeys
- Openize.HEIC (pure-Java HEIC): https://github.com/openize-com/openize-heic-java
- NightMonkeys (AVIF/HEIF, needs native + Java 22): https://github.com/gotson/NightMonkeys
- JDeli (commercial pure-Java AVIF): https://blog.idrsolutions.com/how-to-read-avif-files-in-java-tutorial/
- Chicory (pure-Java WASM runtime): https://github.com/dylibso/chicory
- jSquash (WASM image codecs): https://github.com/jamsinclair/jSquash
- libheif.wasm (AVIF + HEIC in WASM): https://github.com/discere-os/libheif.wasm
- Cloudflare Images pricing: https://developers.cloudflare.com/images/pricing/
