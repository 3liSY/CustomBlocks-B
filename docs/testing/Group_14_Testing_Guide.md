# Group 14 — Animation, Video & Display Blocks

**🎯 Active:** T (0/18, reverted) · A (0/12) · B (0/12) · C (0/16) · Q-fixes (0/10) · D (0/14) · O (0/8) · R (0/12) · S (0/12) · I/J/K/L/M/N reverted (0/12)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                |
| --------------- | ------------------------------ |
| **Verdict**     | 🟡 partial — §T + §I/J/K/L/M/N reverted, stale (pre-07-03) |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 10% (14/140) |
| **Last tested** | 2026-07-02                     |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): AnimListMenu folds into Block Creation Studio Screen, not built. See `GROUP_14_ANIMATION_VIDEO.md`.

## 🗺️ Sections

| § | What                                                             | State      |
| --- | ---------------------------------------------------------------- | ---------- |
| T | Paste-ANY-link fix — link-fixer (image-input **Layer 1**)        | 🎯 test-now |
| A | Texture quality 512 (blur fix) — *Slice 1b*                      | 🎯 test-now |
| B | `/cb retextureall` upgrades GIFs too — *Slice 1c*                | 🎯 test-now |
| C | retextureall safety backup + stop blur — *Slice 1d*              | 🎯 test-now |
| D | Animation tab + full `/cb anim` editor — *Phase 2*               | 🎯 test-now |
| O | `/cb sourcelist` — audit                                         | 🎯 test-now |
| R | Source Wall — temporary review tool (`/cb sourcewall`)           | 🎯 test-now |
| S | `/cb tempretexture` — batch-apply sourced logos + per-block undo | 🎯 test-now |
| Q | Sharper resize (Lanczos+sharpen) + small-source warning          | 🟡 partial  |
| I | per-frame upload + VRAM pool — *Slice 1*                         | 🎯          |
| J | retexture spot-fix — *Step 1*                                    | 🎯          |
| K | real-clock speed — *Step 2*                                      | 🎯          |
| L | retexture undo — *Step 2b*                                       | 🎯          |
| M | animated GIF/WebP blocks everywhere — *Part A*                   | 🎯          |
| N | `/cb anim` animated-only list — *Fix 2*                          | 🎯          |
| E | mipmaps (custom render layer) — *Slice 2*                        | 🧊 parked   |
| F | sharpness + LOD — *Slice 3*                                      | ⏳ planned  |
| G | timeline editor · video import · video wall · live-data          | ⏳ planned  |
| H | Showcase / cycler block                                          | ⏳ planned  |
| P | Re-source blurry bases (~242) — *plan*                           | ⏳ planned  |

🔗 **Related Doc:** [GROUP_14_ANIMATION_VIDEO.md](../groups/GROUP_14_ANIMATION_VIDEO.md)

---

# 🎯 Test now

## T · Paste-ANY-link fix — link-fixer · 🎯 0/9 (reverted 2026-07-05)

> 💡 **Layer 1 of the image-input overhaul.** The mod resolves direct page/share links automatically.
> 🧰 Just `/cb create <id> <name> <url>` and the sample links below.
> ⚠️ **T1-T6/T8 reverted to needs-testing 2026-07-05** — was "Owner-tested 2026-07-02", now stale (before the 07-03 confirm cutoff, needs retest). T7 = giphy gap (⚠️ finding, see Active Bugs). T9 = pending (no link).

| # | Action                                                                                                    | Expected Result                                                         | SP | MP |
| --- | --- | --- | --- | --- |
| T1 | **The exact link that used to fail:** `/cb create t1 Test https://i.imgur.com/gP98pZX.jpeg`               | block **creates** — no "Couldn't find a direct image on that page"      | 🎯 | 🎯 |
| T2 | **A plain page link** (not a direct image): `/cb create t2 Cat https://en.wikipedia.org/wiki/Cat`         | block creates from the page's preview image                             | 🎯 ¹ | 🎯 ¹ |
| T3 | **Google Images:** search Google Images, click a picture, copy its link, `/cb create t3 Test <that link>` | block creates from the real image, not Google's page                    | 🎯 | 🎯 |
| T4 | **Twitter/X:** right-click an image in a tweet → Copy image address, `/cb create t4 Test <that link>`     | block creates at full size (not a tiny thumbnail)                       | 🎯 | 🎯 |
| T5 | **Google Drive / Dropbox:** your own normal "Copy link" share link to an image                            | block creates from the shared file                                      | 🎯 | 🎯 |
| T6 | **GitHub:** any `github.com/USER/REPO/blob/…/something.png` **page** link                                 | block creates from the raw file                                         | 🎯 | 🎯 |
| T7 | **A GIF page link** (Tenor or Giphy): open a GIF, copy the page link                                      | block creates and **animates**                                          | ⚠️ giphy | ⚠️ giphy |
| T8 | **Login-walled site** (Instagram/Facebook post link)                                                      | a clear message ("that link is a web page, not an image"), **no crash** | 🎯 | 🎯 |
| T9 | **Hostile CDN (pngwing & similar):** `/cb create t9 Pin <pngwing URL>`                                    | block **creates** — no `403 Access denied`                              | 🔴 **PENDING — test later** (see Active Bug T9-pngwing) | 🔴 **PENDING — test later** (see Active Bug T9-pngwing) |

> ¹ **T2 link-resolution passes** (block created from the page image). The ragged white cutout is a **separate** background-removal issue → tracked in [GROUP_10_TESTING_GUIDE.md](GROUP_10_TESTING_GUIDE.md) §BG-X, not a §T failure.
>
> **Proven working links (owner, 2026-07-02):**
> - T3 `https://www.google.com/imgres?imgurl=https%3A%2F%2Fupload.wikimedia.org%2Fwikipedia%2Fcommons%2F3%2F3a%2FCat03.jpg&imgrefurl=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FCat`
> - T4 `https://pbs.twimg.com/media/HMJXy0Pb0AAalJd?format=jpg&name=small` (rule forced `name=large` → full size)
> - T5 own Google Drive `…/file/d/<ID>/view?usp=sharing`
> - T6 `https://github.com/twitter/twemoji/blob/master/assets/72x72/1f431.png`
> - T8 `https://www.instagram.com/p/…` → friendly error, no crash (this is the pass)

## A · Texture quality 512 (blur fix) · 🎯 0/6

> 💡 Default texture size → **512**. NEW blocks crisp at 512.
> 🧰 On an existing world: `/cb config texturesize 512` → `/cb retextureall 512`. Use source images ≥512px.

| # | Action                                                                                      | Expected Result                       | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | After `config texturesize 512` + `retextureall 512`, view a **static image** block close up | clearly **sharper** than before       | 🎯 | 🎯 |
| A2 | Create a **new** static block (img)                                                         | crisp at 512, not soft                | 🎯 | 🎯 |
| A3 | A **re-created GIF** block (short clip)                                                     | crisp at 512                          | 🎯 | 🎯 |
| A4 | `/cb config texturesize` + the GUI picker                                                   | no "512 softens" warning              | 🎯 | 🎯 |
| A5 | **Per-face / shaped** blocks (still atlas) at 512                                           | don't look **worse**; flag if they do | 🎯 | 🎯 |
| A6 | Repeat in singleplayer **and** on a server                                                  | same result both                      | 🎯 | 🎯 |

## B · `/cb retextureall` upgrades GIFs too · 🎯 0/6

> 💡 One command upgrades static **and** animated. Keeps speed/loop/smoothing/trim.
> 🧰 An existing GIF block made at 256 + a long GIF (>~64 frames).

| # | Action                                                                | Expected Result                                 | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | GIF block made at 256 → `config texturesize 512` → `retextureall 512` | GIF crisp at 512 **without** recreating it      | 🎯 | 🎯 |
| B2 | That GIF after upgrade                                                | still plays — same speed/loop/smoothing/trim    | 🎯 | 🎯 |
| B3 | Read the chat summary                                                 | reports GIF count re-baked; no-original skipped | 🎯 | 🎯 |
| B4 | A **long** GIF (>~64 frames) after upgrade                            | plays correctly, not scrambled                  | 🎯 | 🎯 |
| B5 | Static blocks in the same run                                         | still upgrade (no regression to A1)             | 🎯 | 🎯 |
| B6 | Repeat in singleplayer **and** on a server                            | same result both                                | 🎯 | 🎯 |

## C · retextureall: safety backup first + stop the blur · 🎯 0/8

> 💡 Snapshots `textures + sources + slots` into a `pre-retextureall-…` backup **before** touching anything. **Leaves a block as-is** instead of upscaling when it has no saved original.
> 🧰 A live world with existing source-less blocks.

| # | Action                                                                | Expected Result                                              | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb retextureall 512`, read the summary                          | counts read "re-rendered … / left as-is" — **no "upscaled"** | 🎯 | 🎯 |
| C2 | A block with **no saved original** after the run                      | **not blurrier** — left exactly as it was                    | 🎯 | 🎯 |
| C3 | Run the `/cb backup restore pre-retextureall-…` line from the summary | every block returns to its pre-retexture state               | 🎯 | 🎯 |
| C4 | `/cb backup list` right after the run                                 | a fresh `pre-retextureall-…` backup is listed                | 🎯 | 🎯 |
| C5 | A block **with** a saved original                                     | still re-renders sharper                                     | 🎯 | 🎯 |
| C6 | `/cb retextureall 128` (go smaller)                                   | "resized smaller" count non-zero; blocks shrink cleanly      | 🎯 | 🎯 |
| C7 | `/cb config texturesize 512` message                                  | no longer says "(animated blocks: re-create them)"           | 🎯 | 🎯 |
| C8 | Repeat in singleplayer **and** on a server                            | same result both                                             | 🎯 | 🎯 |

## Q-fix · white edge halo (anti-ringing clamp) · 🎯 0/2

> 💡 Sharp resize overshoot clamped.
> 🧰 A watermark/dark-background picture that showed the white ring before.

| # | Action                                                        | Expected Result                                        | SP | MP |
| --- | --- | --- | --- | --- |
| Q7 | `/cb retexture <id> <moon-on-dark picture URL>`, look at edge | **no white outline / glow ring** hugging the moon edge | 🎯 | 🎯 |
| Q8 | Compare a block re-baked now vs one from before this fix      | still **as sharp** as §Q was — only the ring is gone   | 🎯 | 🎯 |

## Q-fix2 · pale halo ring (anti-fringe peel) · 🎯 0/3

> 💡 Peels the anti-aliased grey halo ring on photo subjects against white backgrounds.
> 🧰 A subject **on a white/light background**, and the tolerance slider.

| # | Action                                                            | Expected Result                                                 | SP | MP |
| --- | --- | --- | --- | --- |
| Q9  | `/cb retexture <id> <moon-on-WHITE picture URL>`, look at rim     | **no pale/white halo ring** hugging the edge                    | 🎯 | 🎯 |
| Q10 | Set `/cb config backgroundtolerance 20`, re-bake the same picture | ring **still gone**, **and** the subject body is **not** eaten  | 🎯 | 🎯 |
| Q11 | Re-bake a **flat light-grey logo** (crisp edge, no feather)       | logo edge **unchanged** — peel does NOT shave a crisp pale edge | 🎯 | 🎯 |

> ⚠️ **STILL OPEN (NOT fixed) — "white blob beside the moon" at low tolerance.** A pocket of near-white background that is cut off from the frame edge can be left **unremoved as a white blob** next to the subject.
> ⚠️ **Separate finding — "watermark leak".** A **watermarked** stock photo bakes with gray watermark text leaking on the black background because the removal is colour-based.
> 🔗 **Same root cause as [GROUP_10 §BG-X](GROUP_10_TESTING_GUIDE.md#bg-x--%EF%B8%8F-investigation--photo-bg-removal-leaves-ragged-white-blob--logged-2026-07-02):** colour-based (ΔE flood-fill) background removal can't cleanly separate a subject from a non-uniform/shaded same-colour-family background. Fix any one of these three symptoms with the same approach — don't treat them as separate bugs.

## D · Animation tab + full `/cb anim` editor · 🎯 0/7

> 💡 `/cb anim <id>` re-opens any animated block as a full studio.
> 🧰 `/cb create`; a GIF link; place a test block first.

| # | Action                                                 | Expected Result                                        | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Load a GIF → click the (now white) Animation tab       | preview cube **plays the animation live**              | 🎯 | 🎯 |
| D2 | Change Speed / Loop / Smooth / Trim                    | the live preview reacts to each                        | 🎯 | 🎯 |
| D3 | `/cb anim <id>` on an existing block → tune → Save     | opens playing real frames; saved values persist        | 🎯 | 🎯 |
| D4 | Identity tab → change the **id** → Save changes        | `/cb anim <new-id>` works; old id gone from `/cb list` | 🎯 | 🎯 |
| D5 | Texture tab → paste a **different** link → Load → Save | block shows the new image (static↔animated both ways)  | 🎯 | 🎯 |
| D6 | Set background colour / shape / glow → Save changes    | all apply together                                     | 🎯 | 🎯 |
| D7 | `/cb anim` (no id)                                     | the block list opens to pick one                       | 🎯 | 🎯 |

## O · `/cb sourcelist` — which blocks can be re-made sharper · 🎯 0/4

> 💡 Read-only audit tool for HD upgrades.
> 🧰 A live world with your real blocks.

| # | Action                                                                         | Expected Result                             | SP | MP |
| --- | --- | --- | --- | --- |
| O1 | Run `/cb sourcelist`                                                           | chat: "Scanning N block(s)…" then a tally   | 🎯 | 🎯 |
| O2 | Click the **[click to copy the file path]** in chat, open that file in Notepad | a readable report; 3 labelled buckets       | 🎯 | 🎯 |
| O3 | Check the **auto-ready** + **link-only** sections                              | link-only rows show the web link under them | 🎯 | 🎯 |
| O4 | Confirm nothing changed in-game                                                | no blocks re-textured / moved / deleted     | 🎯 | 🎯 |

## R · Source Wall — temporary review-and-fix tool · 🎯 0/6

> 💡 `/cb sourcewall` lays every blurry base in a grid in front of you for easy HD patching.
> 🧰 Stand in an OPEN flat area (creative).

| # | Action                                                                    | Expected Result                                           | SP | MP |
| --- | --- | --- | --- | --- |
| R1 | `/cb sourcewall` in an open area                                          | a grid of blocks appears; each has a floating id tag      | 🎯 | 🎯 |
| R2 | Eyeball the set + read the chat breakdown                                 | black/transparent bases + gifs; no arabic/colour variants | 🎯 | 🎯 |
| R3 | `/cb retexture <id> <url>` on one wall block                              | updates **live** in place; tag unchanged                  | 🎯 | 🎯 |
| R4 | Place your OWN block touching the wall, then `/cb sourcewall clear`       | wall blocks + labels gone; **your block stays**           | 🎯 | 🎯 |
| R5 | `/cb sourcewall` again after a clear                                      | builds fresh — no leftover labels                         | 🎯 | 🎯 |
| R6 | (restart safety) `/cb sourcewall`, restart server, `/cb sourcewall clear` | still removes the wall after a restart                    | 🎯 | 🎯 |

## S · `/cb tempretexture` — batch-apply sourced logos · 🎯 0/6

> 💡 Applies 38 sourced HD logos automatically.
> 🧰 OP (perm 2).

| # | Action                                                          | Expected Result                                                 | SP | MP |
| --- | --- | --- | --- | --- |
| S1 | `/cb tempretexture all`                                         | the listed brand blocks (e.g. `youtube`) now show their HD logo | 🎯 | 🎯 |
| S2 | Read the chat summary                                           | reports any failed/skipped counts; no crash                     | 🎯 | 🎯 |
| S3 | `/cb tempretexture undo twitter` on a single block              | that block reverts to **exactly** its original                  | 🎯 | 🎯 |
| S4 | `/cb tempretexture undo all`                                    | every block the batch changed goes back                         | 🎯 | 🎯 |
| S5 | `/cb tempretexture all` again, then `/cb backup restore <name>` | the named pre-batch backup also works                           | 🎯 | 🎯 |
| S6 | `/cb tempretexture clear`                                       | chat confirms snapshots cleared; undo no longer works           | 🎯 | 🎯 |

---

<details><summary>⏳ <b>Planned / Parked / Notes</b> (click to open)</summary>

- **E · Mipmaps:** 🧊 Deferred (owner 06-27 "come back later") — far blocks sparkle; polish not bug.
- **P · Re-source blurry bases (~242):** ⏳ Planned. Fixes up-close softness. Fix = local AI upscale or name-search HD download -> write new `sources/*.src` only; re-run recolor so variants follow.

| § | Feature                      | What it'll do                                           | Spec               |
| --- | ---------------------------- | ------------------------------------------------------- | ------------------ |
| F | sharpness + LOD              | as sharp as a static block; long GIFs keep all frames   | ANIMATION_VIDEO §7 |
| G | timeline editor / video wall | timeline editor · auto-perf · video import · video wall | ANIMATION_VIDEO    |
| H | Showcase / cycler block      | —                                                       | —                  |

</details>

<details><summary>🗄️ <b>History — superseded / moved</b> (click to open)</summary>

- **§5 Phase 1b own-texture renderer** (Slices A–D) — superseded by the off-atlas GRID renderer.
- **§6 Phase 1c off-atlas static** — 🔴 FAILED 2026-06-20; folded into the off-atlas grid.
- **Bucket-1 studio polish** — owner test 2026-06-22 did **not** pass → moved to `GROUP_27_TESTING_GUIDE.md`.
- **Atlas-revert** (ADR-012) — abandoned, never shipped; off-atlas is the owner-blessed design.

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **Q · Sharper resize (Lanczos+sharpen)** (Passed 2026-06-28). Test history moved to [GROUP_14_ANIMATION_VIDEO.md](GROUP_14_ANIMATION_VIDEO.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **I · per-frame upload + VRAM pool** (Passed 2026-06-27). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **J · retexture spot-fix** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **K · real-clock speed** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **L · retexture undo** (Passed 2026-06-21). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **M · animated GIF/WebP blocks everywhere** (Passed 2026-06-19). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **N · `/cb anim` animated-only list** (Passed 2026-06-20). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| 🔴 T9 | T9 | **PENDING — TEST LATER.** Hostile-CDN 403 retry (browser UA → plain-UA fallback) is **built + deployed but NEVER confirmed in-game.** Owner has no pngwing link right now (deferred 2026-07-02). To close: paste any `w7.pngwing.com/pngs/…png` link into `/cb create t9 Pin <url>` → must create, no `403 Access denied`. Fix code: `ImageDownloader.fetch` ([2026-06-30 UA-fallback](../../PROGRESS_LOG.md)). | 🔴 open — awaiting test |
| T7 | T7 | Giphy page links don't resolve to the GIF. Verified 2026-07-02: giphy pages serve **no** `og:image`/`twitter:image`/`image_src` in static HTML (JS-rendered), so `LinkResolver.resolveImageUrl` correctly finds nothing → clean "web page, not an image" error (no crash). **APPROVED FIX (build deferred by owner 2026-07-02 — DOCUMENT ONLY, do NOT build yet):** add a `giphy.com` rule to `LinkResolver.directImageUrl` — last URL segment after `-` is the id (`cat-JIX9t2j0ZTN9S` → `JIX9t2j0ZTN9S`) → rewrite to `https://media.giphy.com/media/<id>/giphy.gif`. Tenor likely still works via og:image (untested). | 📝 fix approved · build deferred |

---

