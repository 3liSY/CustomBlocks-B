# Group XX — [Feature Name]

**🎯 Active:** A (0/5) · B (0/3)  
**📦 Jar:** `customblocks-x.x.x.jar`

## 📊 Status

|                 |                           |
| --------------- | ------------------------- |
| **Verdict**     | 🎯 test-now                |
| **Progress**    | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩🟥🟥 · 83% (10/12) |
| **Last tested** | YYYY-MM-DD                |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

> Rows MUST be sorted by priority, never alphabetically by §: 🎯 → 💔/❔/❗ → 🟡/⚠️ → ✅ → 🛠️ → 🧊 → ⏳.
> The active test-now row is the thing to do today — it must never be buried below history.

| § | What                         | State      |
| --- | ---------------------------- | ---------- |
| A | [Component or Feature slice] | 🎯 test-now |
| B | [Another feature slice]      | ⏳ planned  |

🔗 **Related Doc:** [GROUP_XX_NAME.md](../groups/GROUP_XX_NAME.md)

> Related Doc path is relative to `docs/testing/` — spec docs live in `docs/groups/`, one directory over. A bare filename (no `../groups/`) is a broken link.

---

# 🎯 Test now

*(All active tests passed or parked for owner build)*

### 🧰 Setup Required
* `/cb create ...`
* Give yourself X item

**A · [Component Name]**
| #  | Action              | Expected Result            | SP | MP |
| --- | ------------------- | -------------------------- | ----- | ------ |
| A1 | [Exact thing to do] | [What must happen in-game] | 🟥    | 🟥     |

---

# 🗄️ Archive

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

*(Future slices or parked items go here)*

</details>

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

*(When a section hits 100% passed, move its tables here)*

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description      | Status |
| ------ | -------- | ----------------------- | ------ |
| A1     | A1       | It crashed the server. | Open   |

> Bug ID = the test row it belongs to (A1, B7, T9...). If two bugs land on the same row, suffix a/b (A1a, A1b). No BUG-GXX-NNN prefix — see [BUG_ID_INDEX.md](BUG_ID_INDEX.md) for the cross-group lookup/collision list.

---

> 📖 **Legends:**
> **Bar:** 🟩 pass · 🟨 partial · 💔 regression · 🟥 not tested
> **State:** 🟥 not tested · 🟡 partial · ✅ pass · ⚠️ finding · 💔 regression · ❗ blocked · 🧊 parked · 🛠️ polish · ⏳ planned · ❔ needs discussion · 🎯 test-now
> **SP / MP:** every test row carries both columns. SP = singleplayer/solo world, MP = dedicated/LAN server. SP and MP are counted as two STANDALONE slots in the Progress bar (a group with N rows has 2N total slots) — SP passing doesn't require MP to also pass, and vice versa. Use ➖ in a column when that environment cannot run the test (e.g. "2nd player sees it" has no SP case) — ➖ cells don't count toward the total slot count.
