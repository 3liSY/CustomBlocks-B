# 🧪 Group 26 — Name & Give Fixes + Named-Texture Mirror — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟡 Partial |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 · 3 / 10 passed |
| **Last tested** | 2026-06-15 (§2·§3) |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **Part C** — `textures_names/` named-texture mirror (`/cb config mirrornames`) | §1 |
| ✅ Passed 2026-06-15 | **FIX A** clean display names · **FIX B** case-insensitive `/cb give` | §2 · §3 |

---

# 🎯 Test now

## §1 · Named-texture mirror

> 💡 **What it does:** an **optional** human-readable copy of your texture folder where each file is
> named by the block (`Neptune Red.png`) instead of `slot_0.png`. **Write-only** — the mod writes it,
> never reads it, so it can't affect a single block. Off by default.

> 🧰 **Before you start:**
> - A couple of real textured blocks, e.g. `/cb give arabic_alef_black` and one of your own.
> - Folder to watch: `config\customblocks\textures_names\`

**Try these:**

- **① Turn it on (backfills everything)** 🔴
  `/cb config mirrornames on`
  - ✅ **Pass:** chat says `wrote N file(s)`, and the `textures_names\` folder fills with PNGs named like **Alef Black.png** — one per textured block. The original `slot_N.png` files in `textures\` are untouched.
  - ❌ **Broken if:** a block's texture changes or vanishes in-game (the mirror must never touch real blocks).

- **② Status** 🟡
  `/cb config mirrornames`
  - ✅ **Pass:** shows `ON`, the file count, and the folder path.

- **③ A new block appears in the mirror** 🔴
  `/cb create mirrortest Mirror_test` *(then give it a texture if your create flow needs a URL)*
  - ✅ **Pass:** **Mirror Test.png** shows up in `textures_names\` (clean name).

- **④ Rename re-files it (no orphan)** 🔴
  `/cb rename mirrortest Renamed_demo`
  - ✅ **Pass:** the old `Mirror Test.png` is **gone**, `Renamed Demo.png` is there — no leftover.

- **⑤ Delete removes it** 🔴
  `/cb delete mirrortest` *(use the current id)*
  - ✅ **Pass:** that block's PNG disappears from `textures_names\`.

- **⑥ Self-heal** 🟡
  `/cb config mirrornames rebuild`
  - ✅ **Pass:** folder is wiped + regenerated from scratch; count matches your textured blocks.

- **⑦ Turn it off** 🟡
  `/cb config mirrornames off`
  - ✅ **Pass:** existing files stay, but new creates/renames no longer change the folder.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | `on` backfills; real `slot_N.png` + blocks untouched |
| 🟥 | ② | Status shows on/off, count, path |
| 🟥 | ③ | New block mirrors with its clean name |
| 🟥 | ④ | Rename re-files, leaves no orphan |
| 🟥 | ⑤ | Delete removes the mirrored file |
| 🟥 | ⑥ | `rebuild` regenerates the whole folder |
| 🟥 | ⑦ | `off` stops mirroring, keeps existing files |
| — | **0 / 7** | |

> ↩️ **Undo the test:** `/cb config mirrornames off` · delete `config\customblocks\textures_names\` if you don't want it · `/cb delete` any test blocks you made.

---

# ✅ Passed — kept for re-test reference

## ✅ §2 · FIX A — clean display names — passed 2026-06-15

> 💡 **What it does:** names show with **spaces + Title Case**, never underscores (`Test_black` → **Test Black**). New blocks, uploads, renames, and the 224 bundled Arabic blocks all read clean.

**The run:**
- 🔢 boot log: `[CustomBlocks] Cleaned N legacy display name(s) (underscores -> spaces).` (N ≈ 224 first boot; 0 after — it already ran).
- 🧱 old block: `/cb give arabic_alef_black` → name shows **Alef Black**, not `Alef_Black`.
- 🆕 new create: `/cb create g26name Test_black` → `/cb list` shows **Test Black**.

✅ **Proves:** the one-time migration cleans saved names, and the casing rule cleans every new create, upload, and rename.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟩 | 1 | boot migration cleans old underscored names |
| 🟩 | 2 | new creates use Title Case (no underscores) |
| — | **2 / 2** | |

## ✅ §3 · FIX B — `/cb give <id>` case-insensitive — passed 2026-06-15

> 💡 **What it does:** you no longer have to match the id's exact capitalization. If the id is `Te`, then `te`, `Te`, and `TE` all give the same block. Exact matches still win first.

**The run:**
- 🧱 make it: `/cb create Te GiveTest`
- 🔡 any casing: `/cb give te` · `/cb give TE` · `/cb give Te` → all give **GiveTest**
- 🚫 real miss: `/cb give zzznope` → `There's no block called "zzznope"…`, no crash

✅ **Proves:** the case-insensitive fallback resolves a different-case id while the exact-match fast path stays unchanged.

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟩 | 1 | any capitalisation of the id finds the block |
| — | **1 / 1** | |

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs what you expected (📸 a screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb config mirrornames off` · `/cb delete g26name` · `/cb delete Te` · delete any leftover test blocks
