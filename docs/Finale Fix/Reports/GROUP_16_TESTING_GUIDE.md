# 🧪 Group 16 — Diagnostics & IT Chest — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟢 Confirmed (owner, 2026-06-21): slices 1, 2, 3, 4, + 5 R1/R2/**R3 centred**/**R4 bulk_complete + achievement-preview** · **showbrokenblocks** (§7). `rp_regenerate` FX **removed entirely** (owner: pointless — firing it on a manual pack reload is useless). **Build-green, awaiting in-game (one test round):** the **advanced Debug Log** (§6 — copy-to-clipboard, severity filter, issue summary). Nothing else left to build except the `achievement` *live* trigger (needs owner's achievement system; preview already confirmed). |
| **Progress** | 🟩🟩🟩🟩🟩🟩 · all built · only the advanced Debug Log awaits a test; `achievement` live trigger deferred to owner |
| **Last tested** | 2026-06-21 (owner) |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **Advanced Debug Log** — copy-to-clipboard · severity filter · issue summary · glint | §6 |
| ✅ Confirmed | IT Chest dashboard · auto-fix · admin cmds (slice 3) · particles · Feedback FX R1+R2+R3+R4 (bulk_complete + achievement preview) · showbrokenblocks | §1·§5·§7 |
| ✅ Already working | `/cb confirm` · `/cb cancel` (reuse — verify wording) | §2 |
| 🗑️ Removed | `rp_regenerate` FX (owner: pointless on a manual reload) | §5 |
| ❌ Dropped | ~~screenshot~~ | — |
| ⏳ Owner to build later | `achievement` *live* trigger (needs an achievement system; preview confirmed) | §5 |

---

# 🎯 Test now

## §1 · IT Chest dashboard

> 💡 **What it does:** `/cb diag` (and `/cb incidents`) open one 6-row chest — Row 1 live health,
> Rows 2–4 incidents (coloured wool), Row 5 recent mutations, Row 6 controls. Replaces the old 3-row menu.

> 🧰 **Before you start** — make a block and force one incident:
> - `/cb create g16a DiagTest`
> - `/cb setglow g16a 8`
> - `/cb retexture g16a https://definitely-broken-url.invalid/bad.png`   *(the bad URL logs an incident)*

**Try these:**

- **① Dashboard opens** 🔴 — `/cb diag`
  - ✅ **Pass:** a 6-row chest opens (not text). Row 1 shows colour-coded health items.
  - ❌ **Broken if:** text output only, or the old 3-row menu.

- **② Health hover is live** 🔴 — hover the **TPS** and **Memory** slots
  - ✅ **Pass:** tooltips show exact values (e.g. `TPS: 20.0`, `Heap: NNN / NNN MB`), colour matches the value.

- **③ Incident appears** 🔴 — look at Rows 2–4
  - ✅ **Pass:** a **red wool** for the failed `g16a` download. Hover shows time, your name, the action, the error.
  - ❌ **Broken if:** rows empty after the bad URL.

- **④ Incident click → re-download (slice 2)** 🔴 — click the red `g16a` incident
  - ✅ **Pass:** it re-runs the download from the **stored URL** — no retyping. With the bad test URL it
    fails again (expected — that proves it fired); a real image URL fixes the block.
  - 💡 An incident with **no** stored URL (e.g. a create/face failure) opens that block's editor instead.

- **⑤ Mutation row** 🔴 — look at Row 5
  - ✅ **Pass:** recent actions show (create / setglow / retexture g16a). Hover = time, player, action, block. Click → editor.

- **⑥ Clear + Refresh** 🟡 — Row 6 → **Clear Incidents**, then **Refresh**
  - ✅ **Pass:** Rows 2–4 clear; health + mutation rows untouched. Refresh re-reads health in place.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ✅ | ① | `/cb diag` opens the 6-row dashboard |
| ✅ | ② | Health gauges show live, colour-coded values |
| ✅ | ③ | Failed download is logged as a structured incident |
| ✅ | ④ | Incident click re-downloads from its stored URL (slice 2) |
| ✅ | ⑤ | Mutation row reads the history log |
| ✅ | ⑥ | Clear hits incidents only; Refresh re-reads health |
| — | **6 / 6 — confirmed 2026-06-21** | |

> ↩️ **Undo the test:** `/cb delete g16a` · then Row 6 → **Clear Incidents** (or `/cb incidents clear`).

---

# ✅ Already working — verify only

## §2 · `/cb confirm` / `/cb cancel`

> 💡 Built earlier (`BulkConfirm`). Trigger a bulk op that needs confirmation, then:
> - `/cb cancel` → `Nothing to cancel.` becomes a real "Cancelled: …" when an op is pending.
> - `/cb confirm` with nothing pending → `Nothing to confirm.`
>
> Spec wants "No pending operation to confirm." — say the word if you want the wording matched exactly.

---

# 📋 Feature ledger

| Feature | State | Slice |
|---|---|---|
| `/cb audit [player]` | ✅ **Confirmed (2026-06-21)** (3a→GUI) — paged **chest** of the mutation log; `/cb audit <name>` filters (name tab-completes) + a **Show all** button clears it | 3a |
| `/cb cache` | ✅ **Confirmed (2026-06-21)** (3a) — readout: textures/sources/exports size, pack size + last-built, pending rebuild | 3a |
| `/cb cache clear` | ✅ **Confirmed (2026-06-21)** (3b) — clears `cloud_exports/` + `*.tmp` (keeps live PNGs/pack/sources/backups) | 3b |
| Generate Report | ✅ **Confirmed (2026-06-21)** (3b→GUI) — `/cb report` chest: **Generate Report** writes `data/diag_report.txt` + posts a [download] link; **Get Download Link** re-posts it. IT Chest Row-6 → Diagnostic Report opens the same screen | 3b |
| `/cb particles <cat> on\|off` | ✅ **Confirmed (2026-06-21)** (4) — particle FX set + board. All **6** categories fire live (success/error/gui/selection + bulk_complete); `achievement` is preview-only. `rp_regenerate` **removed** | 4 |
| `/cb feedback` · `/cb sounds` · `/cb particles` | ✅ **R1+R2+R3+R4 confirmed (2026-06-21)** (5) — **merged Feedback FX**: per-category sound layer + merged with particles. Master flips both; each splittable; centred board; bulk_complete fires; achievement preview confirmed. See §5 | 5 |
| Debug Log viewer | 🟢 **Advanced rebuild — build-green, await** — IT Chest Row 6 → **Debug Log**: paged read-only `[CustomBlocks]` lines from `logs/latest.log`, newest first, severity colour/glint, **issue summary tile**, **severity filter**, **click-to-copy** + **copy-filtered**. See §6 | later |
| `/cb showbrokenblocks` | ✅ **Confirmed (2026-06-21)** (G09, G16 owns spec) — lists missing-texture blocks, bulk re-bake from saved source, select-all / delete-to-trash. See §7 | — |
| ~~`/cb screenshot`~~ | **Dropped** — impractical on a dedicated server | — |

---

## §5 · Feedback FX (merged particle + sound) — ✅ R1–R4 confirmed (`rp_regenerate` removed)

> 💡 **What it does:** particles (slice 4) and a new **sound** layer are merged into one per-category
> Feedback system. Each category has 2 switches (FX + Sound). A master flips both; you can also set
> each alone. Built in two rounds — test R1 first, then R2.

### Round 1 — sound exists + merged firing (board unchanged) — ✅ confirmed in-game 2026-06-21

> 🧰 Stand in the open so you can hear + see cues.

- **① Success makes a sound** 🔴 — run a command that succeeds (e.g. `/cb create g16snd Snd`)
  - ✅ **Pass:** green particle ring **and** a sound (XP-orb pickup) together.
  - ❌ **Broken if:** particle but silence (or the reverse).
- **② Error makes a sound** 🔴 — run a failing command (e.g. `/cb create g16snd Snd` again — id taken)
  - ✅ **Pass:** angry puff **and** a low note-block bass buzz.
- **③ Menu click makes a sound** 🔴 — `/cb diag`, click any tile
  - ✅ **Pass:** enchant glyphs **and** a chime.
- **④ Sound-only toggle splits the channel** 🔴 — `/cb sounds error off`, then fail a command again
  - ✅ **Pass:** still see the angry puff, but **no** buzz. `/cb sounds error on` → buzz returns.
  - 👀 Particle and sound toggle **independently** — that's the merge being splittable.
- **⑤ Persists** 🔴 — toggle a couple off, restart server, `/cb sounds` (console) or re-check
  - ✅ **Pass:** states stayed.

### Round 2 — expand/collapse Feedback board — ✅ confirmed in-game 2026-06-21 (incl. R3 centring)

- **① Board opens** ✅ — `/cb feedback` (also `/cb particles`, `/cb sounds`, IT Chest Row 6)
  - ✅ **Pass:** one chest, 6 category master tiles; lore shows FX ● / Snd ● state.
- **①ᵇ Tiles are centred** ✅ — look at where the 6 tiles sit (R3 polish 2026-06-21)
  - ✅ **Pass:** master row sits in the **vertical middle**, not hugging the top; one
    blank row above it, Back/Close on the bottom row. Expanding a tile still drops its 2 sub-tiles
    directly below without hitting the footer.
- **② Master toggles both** 🔴 — left-click a category tile
  - ✅ **Pass:** both FX + Sound for it flip together (lore updates, saved).
- **③ Expand splits it** 🔴 — shift-click a category tile
  - ✅ **Pass:** it expands into two sub-tiles (FX on/off, Sound on/off); set each alone; shift-click
    again collapses.
- **④ Preview** 🔴 — right-click a category tile
  - ✅ **Pass:** plays that category's particle **and** sound, ignoring its toggle.

### Round 4 — deferred FX wired to real events — ✅ confirmed in-game 2026-06-21 (`rp_regenerate` removed)

> 💡 `bulk_complete` now fires on real bulk ops. `achievement` stays **preview-only** on purpose (no
> achievement system yet). `rp_regenerate` was **removed entirely** (owner: firing it on a manual pack
> reload is pointless) — manual reloads (`/cb sync`, `/cb rp resume`) just use the normal success cue.

- **① bulk_complete fires** ✅ — run a bulk op, e.g. `/cb bulkproperty category:all glow 5` (or
  `/cb bulkdelete` / `/cb bulkrename`), confirm if prompted
  - ✅ **Pass:** when the batch finishes you get the **totem ring** particle + **beacon hum** sound.
  - ❌ **Broken if:** silent, or you get the plain success ring instead of the totem ring.
  - 👀 Toggle test: `/cb particles bulk_complete off` → only the sound on the next bulk op; `/cb sounds
    bulk_complete off` → only the particle.
- **② achievement stays preview-only** ✅ — `/cb feedback`, right-click the Achievement tile
  - ✅ **Pass:** firework column + toast ding play on preview. No live event fires it yet (by design).

---

## §6 · Debug Log viewer (advanced) — 🎯 TEST NOW — 🟢 build-green, awaiting in-game

> 💡 **What it does:** an in-game window onto this session's `[CustomBlocks]` log lines, so you don't
> have to open `logs/latest.log` in a text editor. Now upgraded: a **summary tile** with per-severity
> counts, a **severity filter** to see only issues, **click-to-copy** any line, and **copy-filtered**
> for the whole view. Read-only, newest first.
>
> 🗺️ **Layout:** Row 1 = summary tile (centre) + frame · Rows 2–5 = log lines (36/page) · Row 6 =
> footer (Back · **Filter** · **Copy filtered** · Prev/Page/Next · Close).

- **① Opens from the IT Chest** 🔴 — `/cb diag` → Row 6 → **Debug Log** (bookshelf icon, slot 50)
  - ✅ **Pass:** a 6-row chest titled `Debug Log · All` opens. Log lines fill rows 2–5, newest at the
    top-left of that block; row 1 shows the summary tile.
  - ❌ **Broken if:** empty when the log clearly has `[CustomBlocks]` lines, or it doesn't open.
- **② Summary tile** 🔴 — hover the tile in the middle of Row 1
  - ✅ **Pass:** lore lists **N error(s) · M warning(s) · K info** + "X shown · Y total". It **glints**
    when there's at least one error/warning. Left-click it → jumps to **Issues only** (and back to
    **All** when already on a problem view).
- **③ Severity filter** 🔴 — Row 6, the **Hopper** button ("Filter: …")
  - ✅ **Pass:** each click cycles **All → Issues only → Errors → Warnings → Info**; the title and the
    "X shown of Y" count update, and only matching lines remain. Prev/Next keep the active filter.
  - 💡 With nothing of that severity, you get a "No lines match: …" tile instead of a blank grid.
- **④ Lines render by severity** 🔴 — look at the tiles
  - ✅ **Pass:** INFO = paper (grey), WARN = yellow wool, ERROR = red wool. ERROR/WARN tiles **glint** so
    issues pop. Title = the message; hover shows the full raw line wrapped over several rows.
- **⑤ Click-to-copy a line** 🔴 — left-click any log tile
  - ✅ **Pass:** a chat line appears with a **`[Copy]`** button; clicking that button copies the **full
    raw line** to your clipboard (paste anywhere to confirm). *(A chest click can't reach the clipboard
    directly — that's why it routes through a one-click chat button.)*
- **⑥ Copy filtered** 🟡 — Row 6, the **Writable Book** button ("Copy filtered")
  - ✅ **Pass:** posts a **`[Copy N lines]`** chat button; clicking copies every currently-shown line
    (one per row) to the clipboard. N matches the filter's "shown" count.
- **⑦ Force an entry, filter to Errors** 🟡 — `/cb retexture <id> https://bad.invalid/x.png`, re-open,
  set the filter to **Errors** (or **Issues only**)
  - ✅ **Pass:** the fresh failed-download line shows at the top; the summary error count went up by one.

---

## §7 · `/cb showbrokenblocks` — ✅ built (G09), G16 owns the spec — awaiting in-game

> 💡 **What it does:** lists blocks whose baked texture file is missing (they render purple) and
> bulk-fixes them by re-baking from each block's saved source image. Opens from `/cb showbrokenblocks`
> or the Safety dashboard (`/cb safety` → broken-blocks **[open]**).

> 🧰 **Make a broken block:** create one, then remove its baked texture so it scans as broken — the
> simplest repeatable way is a bad retexture that wipes the bake. If you can't force one, just confirm
> the **empty state** (① below) reads correctly.

- **① Opens / empty state** 🔴 — `/cb showbrokenblocks`
  - ✅ **Pass (none broken):** chest titled `Broken Blocks · 0`, a green "No broken blocks" tile.
  - ✅ **Pass (some broken):** title shows the count; one **red wool** per broken block (slot + "Saved
    image: yes/none" in lore).
- **② Tick + fix selected** 🔴 — left-click some tiles, then footer slot 46 **Fix N selected**
  - ✅ **Pass:** ticked blocks with a saved image re-bake (texture returns, no longer purple); ones
    without a saved image are skipped. Report refreshes.
- **③ Fix just one** 🔴 — right-click a tile
  - ✅ **Pass:** with a saved image → it re-bakes immediately. With none → it pre-fills
    `/cb retexture <id> ` for you to paste a URL.
- **④ Select all / clear** 🟡 — slot 51 **Select all broken**, slot 47 to clear
  - ✅ **Pass:** all broken tiles tick / untick across pages; the summary count updates.
- **⑤ Delete selected** 🟡 — tick some, slot 52 **Delete N selected** → confirm
  - ✅ **Pass:** routes to a confirm screen; confirming sends them to the **trash** (restorable), not a
    hard delete.

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs what you expected (📸 a screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup

`/cb delete g16a` · `/cb incidents clear`
