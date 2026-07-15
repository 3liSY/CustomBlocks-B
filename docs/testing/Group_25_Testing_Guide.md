# Group 25 — Block Management Extras

**🎯 Active:** None  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                   |
| --------------- | --------------------------------- |
| **Verdict**     | 🎯 needs retest — A, C reverted to needs-testing (stale, pre-07-03) |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/26)                 |
| **Last tested** | 2026-06-28                        |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): ReIdMenu is built (chest menu, `ReIdMenu.java`) but still targets a Screen migration per the mod-wide Screen rule — migration itself not built. See `GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md`.

## 🗺️ Sections

| § | What                                                                                  | State     |
| --- | ------------------------------------------------------------------------------------- | --------- |
| A | Reid GUI — picker · single-arg anvil · Change-ID editor button                        | 🎯         |
| C | `/cb reid <id> <newId>` command — id change · state migration · undo                  | 🎯         |
| B | swapid · swapname · dupe alias · custom drops · Block Finder · HD export · settabicon | ⏳ planned |
| D | Target Macros (`#` and `!`) — looked-at and held block targets                        | ⏳ planned |

🔗 **Related Doc:** [GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md](../groups/GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md)

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

**B · Future ops**
| § | What it'll do                                                 | Spec             |
| --- | ------------------------------------------------------------- | ---------------- |
| B | `/cb swapid <a> <b>` — swap two blocks' ids                   | GROUP_25         |
| B | `/cb swapname <a> <b>` — swap two blocks' display names       | GROUP_25         |
| B | duplicate alias — friendlier alias for `/cb dupe`             | GROUP_25         |
| B | custom drops — per-block drop when broken                     | GROUP_06 (G06-7) — SELF-default baseline ✅ confirmed 2026-07-09, rest ⏳ |
| B | Block Finder GUI — locate placed blocks in the world          | GROUP_25         |
| B | HD PNG export from editor — one-click full-res texture export | GROUP_25         |
| B | `/cb settabicon` — set the creative-tab icon                  | GROUP_25         |
| D | Target Macros (`#` and `!`) — looked-at and held block targets| GROUP_25         |

**D · Target Macros (Planned)**
| § | What it'll do                                                 | Spec             |
| --- | ------------------------------------------------------------- | ---------------- |
| D | `#` macro — targets block at crosshair (10 block reach)       | GROUP_25         |
| D | `!` macro — targets block in hand (main/offhand)              | GROUP_25         |
| D | Piercing — `#` ignores water, lava, and entities              | GROUP_25         |
| D | Item Frames — `#` reads custom block item inside frame        | GROUP_25         |
| D | Commands — works on retexture, delete, shape, color, etc      | GROUP_25         |

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **A · Reid GUI** (Passed 2026-06-28). Test history moved to [GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md](GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · `/cb reid <id> <newId>` command** (Passed 2026-06-13). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C9/C10 · case-fix confirmed** (Passed 2026-06-28). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

