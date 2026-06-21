# 🧪 Group 17 — Command Regressions — Testing Guide

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Slices 1–3 built — build-green 2026-06-21. Slice 1 confirmed in-game; **Slices 2–3 awaiting your in-game test**. |
| **Progress** | 🟩🟨🟨 · 3 slices built · 1 / 3 confirmed |
| **Last tested** | Slice 1 (undo/redo) — passed |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| ✅ Confirmed | **Slice 1 — multi-undo / undo all / undo clear / multi-redo** | §1 |
| 🎯 **TEST NOW** | **Slice 2 — give `<amount>` / give `<amount> <player>`** | §3 |
| 🎯 **TEST NOW** | **Slice 3 — delete `#` (looked-at custom block)** | §4 |
| ✅ Verify only | Search GUI (already opens a chest) | §2 |
| ⏳ G25 | favorite (primary) · unfavorite · recent → owned by **Group 25** | §5 |
| ⏳ 17.15 | export structure alignment → own session | §5 |

> 🧰 **One-time setup for this whole session:**
> - `/cb create g17a UndoTest`
> - `/cb create g17b GiveTest`
> - `/cb create g17c SearchTest1` · `/cb create g17d SearchTest2`
> - `/cb setglow g17a 4` · `/cb setglow g17a 8` · `/cb setglow g17a 12`

---

# ✅ Confirmed (re-test if you like)

## §1 · Slice 1 — Undo / Redo upgrade
> 💡 `/cb undo` / `/cb redo` are multi-step: `undo <N>`, `undo all`, `undo clear` (confirm),
> `redo <N>`, `redo all`. Multi-undo prints a value-bearing list (`glow g17a 12→8`).

- **① Multi-undo** — `/cb undo 3` → 3 reverted with values, glow back to **0**.
- **② Multi-redo** — `/cb redo 3` → glow back to **12**.
- **③ undo all / redo all** — empties / replays the stack.
- **④ Over-count safe** — `/cb undo 99` → undoes what's there, reports the real number.
- **⑤ undo clear** — `/cb setglow g17a 5` then `/cb undo clear` → `[confirm]` prompt, clears on click.

---

# 🎯 Test now

## §3 · Slice 2 — Give with amount / player
> 💡 `/cb give <id>` (1 to you) · `/cb give <id> <amount>` (1–6400 to you) ·
> `/cb give <id> <amount> <player>` (to an online player, **OP-only**). Overflow is reported,
> never dropped. Recipient is notified.

> 🧰 Uses `g17b` (GiveTest) from setup.

**Try these:**

- **① Give 1 still works** 🔴 — `/cb give g17b`
  - ✅ **Pass:** 1 × GiveTest enters your inventory, `Gave you 1 × GiveTest.`
  - ❌ **Broken if:** error, or "unexpected argument".

- **② Give an amount** 🔴 — `/cb give g17b 5`
  - ✅ **Pass:** 5 in inventory, `Gave you 5 × GiveTest.`
  - ❌ **Broken if:** only 1 given, or error.

- **③ Amount tab-completes** 🟡 — type `/cb give g17b ` then press Tab
  - ✅ **Pass:** suggests `1 / 16 / 32 / 64`.

- **④ Overflow is reported, not dropped** 🔴 — fill your inventory (or nearly), then `/cb give g17b 6400`
  - ✅ **Pass:** gives only what fits, message like `Gave 12 × GiveTest (6388 didn't fit — inventory full).` No items drop on the floor.
  - ❌ **Broken if:** items spill onto the ground, or it claims it gave all 6400.

- **⑤ Give to another player (OP)** 🔴 *(needs a 2nd online player — skip if solo)* — `/cb give g17b 2 <name>`
  - ✅ **Pass:** they receive 2 and see `You received 2 × GiveTest from <you>.`; you see `Gave 2 × GiveTest to <name>.`
  - ❌ **Broken if:** items go to you, or the other player isn't notified.

- **⑥ Non-OP can't give to others** 🟡 *(test from a non-OP account)* — `/cb give g17b 2 <name>`
  - ✅ **Pass:** `Only operators can give blocks to other players.` — self-give still works for them.

- **⑦ Offline name → clear error** 🟡 — `/cb give g17b 2 NotOnline123`
  - ✅ **Pass:** `No online player named "NotOnline123". They must be online to receive blocks.`

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | `give <id>` still gives 1 |
| 🟥 | ② | `give <id> <amount>` gives N |
| 🟥 | ④ | Overflow reported, nothing dropped |
| 🟥 | ⑤ | `give <id> <amount> <player>` delivers + notifies |
| 🟨 | ③⑥⑦ | tab-complete · OP gate · offline error |
| — | **0 / 4** | (red rows) |

---

## §4 · Slice 3 — Delete `#` (looked-at block)
> 💡 `/cb delete #` deletes the **custom block you're looking at** (~6-block reach) — the whole
> definition, same as `/cb delete <id>`. **No confirm** — recover a mis-aim with `/cb undo`.
> Custom slot blocks only.

> 🧰 Place a `g17b` block in the world first: `/cb give g17b`, then place it and look at it.

**Try these:**

- **① Delete the block you're aiming at** 🔴 — look at the placed `g17b`, then `/cb delete #`
  - ✅ **Pass:** `Deleted "g17b" (targeted block). Undo with /cb undo.` The definition is gone (`/cb list` no longer shows g17b).
  - ❌ **Broken if:** `unexpected argument #`, or the wrong block is deleted.

- **② Undo brings it back** 🔴 — `/cb undo`
  - ✅ **Pass:** g17b is restored (texture + all), back in `/cb list`.

- **③ Aiming at a vanilla block → clear error** 🔴 — look at dirt/stone, `/cb delete #`
  - ✅ **Pass:** `The block you're looking at isn't a custom block.` Nothing is deleted.
  - ❌ **Broken if:** the vanilla block vanishes, or a confusing error.

- **④ Delete by id still works** 🟡 — `/cb delete g17c`
  - ✅ **Pass:** `Block "g17c" deleted.` (unchanged behaviour).

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | `delete #` removes the targeted custom block |
| 🟥 | ② | It's undoable |
| 🟥 | ③ | Vanilla/air → clear "not a custom block" error |
| 🟨 | ④ | `delete <id>` unchanged |
| — | **0 / 3** | (red rows) |

> ↩️ After ④, re-create if needed: `/cb create g17c SearchTest1`

---

# ✅ Verify only

## §2 · Search GUI (G17.9)

> 💡 `/cb search <query>` already routes players to a chest GUI (`Nav.Dest.SEARCH`) — no build
> needed, just confirm it behaves. Uses `g17c` / `g17d` from setup.

- **① Search opens a chest** 🔴 — `/cb search SearchTest`
  - ✅ **Pass:** a chest GUI opens with the matching blocks as slots; clicking a slot opens that block's editor.
  - ❌ **Broken if:** text-only output, or no results found.

---

# ⏳ Not built (owned elsewhere)

## §5 · Deferred

| Feature | Why not here | Owner |
|---|---|---|
| `favorite` (primary) · `unfavorite` · `recent` | favorites/recent feature set | **Group 25** (G17 = regression check only) |
| Export structure alignment | dedicated export-rework discussion | **Issue 17.15** |

---

## 🆘 If a test fails
- 🔢 Step number
- 👀 What happened vs expected (📸 screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb delete g17a` · `/cb delete g17b` · `/cb delete g17c` · `/cb delete g17d`
(some may already be gone from the delete-# tests — that's fine)
