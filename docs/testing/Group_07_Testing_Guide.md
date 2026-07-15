# Group 07 — Bulk Operations

**🎯 Active:** B — 8 rows untested (2/10 passed) · ❗ Hub Console filter bug open
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                   |
| --------------- | --------------------------------- |
| **Verdict**     | 🎯 8 rows need testing (§B) — A, C already passed · ❗ Hub Console still speaks the dead filter language |
| **Progress**    | ✅✅🟡🟡🟡🎯🎯🎯🎯🎯 · 20% (2/10 passed) |
| **Last tested** | 2026-07-13 (SP)                   |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> This guide covers chat bulk commands (§B) and routing/settings (§A, §C). Bulk-op behaviour and the Hub screens now live in `GROUP_27_TESTING_GUIDE.md` §T.
>
> Full design + build history: [GROUP_07_BULK_OPERATIONS.md](../groups/GROUP_07_BULK_OPERATIONS.md)

## 🗺️ Sections

| § | What | State |
| --- | --- | --- |
| B | Chat bulk commands | 🎯 8 untested — id-only, spaces added 2026-07-14 |
| A | Routing — `/cb list` · `/cb bulk` · MainMenu | ✅ passed, see history |
| C | `/cb setall <setting> <value>` | ✅ passed, see history (🛠️ Set All Screen polish later) |

🔗 **Related Doc:** [GROUP_07_BULK_OPERATIONS.md](../groups/GROUP_07_BULK_OPERATIONS.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create g07a …` through `g07c`, all `/cb setcategory … bulktest` — for §B

## B · Chat bulk commands · 🎯 5 untested (trimmed to id-only, 2026-07-13)

> 💡 Every `/cb bulk…` command still runs from chat, with its `[✔ Confirm]` guard.
> ⚠️ `category:`/`id:`/`name:`/`favorite:`/`locked:` filters and the AND/OR/NOT combinator are **removed** from `BulkScope`. Chat bulk commands take `all` or an **id list** — plain spaces (`id1 id2 id3`, added 2026-07-14), commas, or quotes. Picking blocks by category/name/etc. is a Hub-only job now (Blocks List ticks).
> ❗ The Hub's Console tab still *builds* the removed filter words — see bug **G07-HUB-FILTER** below. Test §B from chat, not from the Console, until that's fixed.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| B7 | `/cb bulkrename g07a g07b prefix old_` | renames both — the id list ends at the `prefix` keyword | 🟡 | 🟡 |
| B6 | `/cb bulkdelete g07a g07b g07c` (plain spaces) | deletes exactly those three — the space-separated id list parses | ✅ | ✅ |
| B8 | `/cb bulkdelete category:bulktest` | "No blocks matched" — the filter words resolve to nothing, loudly | ✅ | ✅ |
| B1 | `/cb bulkdelete "g07a","g07b"` → confirm | "Deleted N", hover id list, **`↩ Undo`** (the old `▶ [↩ Undo]` is being wiped — see G04-UNDO-DIALECT); placed → markers | 🎯 | 🎯 |
| B2 | `/cb undo` right after B1 | blocks + world markers return in one step | 🎯 | 🎯 |
| B3 | run any op on **2+ blocks** (or `all`) | clickable confirm at 2+; nothing runs until click/`/cb confirm` | 🎯 | 🎯 |
| B4 | wait 60s past a confirm, then `/cb confirm` | says expired; nothing runs | 🎯 | 🎯 |
| B5 | `/cb bulklock "id1" "id2"` (quoted multi-id) | locks exactly those ids | 🎯 | ➖ |

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

| § | What it was | Spec | Tested / Outcome |
| --- | --- | --- | --- |
| A | Routing — `/cb menu` slots, export pick mode, `/cb help` bulk listing | — | ✅ 2026-07-13 |
| C | `/cb setall` — glow/shape/category/sound, backup pruning, Set All Screen | — | ✅ 2026-07-13 · 🛠️ Set All Screen layout wants a polish pass later |

</details>

<details><summary>🗂️ Retired sections (click to open)</summary>

Old §E/§F/§C5 tested chest menus that no longer exist (replaced by the Hub screen). §E's real items moved:
create-bad-url → Group 06, background-removal → Group 10, duplicate behavior → G27 §T-d.

Bulk-op behaviour (Edit/Recolor/Rename/Move/Duplicate/Re-ID/Lock/Favorite/Export/Delete) and the Hub
screens (Blocks List + Bulk Actions) both moved to `GROUP_27_TESTING_GUIDE.md` §T — same screen, no
reason to keep it split across two guides.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| G07-HUB-FILTER | — | The Hub Console tab + NL bar still build `category:`/`id:`/`name:`/`favorite:`/`locked:` expressions and send them to a `BulkScope` that stopped resolving them on 2026-07-13, so a Console filter silently matches zero blocks. Fix = strip the filter builder + AND/OR/NOT from the Hub (Console picks by tickbox). **The chat half is now confirmed fixed in MP** (2026-07-15: B6/B8 passed, and TG4 A7/A7b passed) — the **Hub half is all that's left of this bug**. | ❗ |
| G07-BULK-UNDO | B1 | ✅ **Fixed 2026-07-15** (build slice 1, with G04-UNDO-DIALECT) — awaiting in-game confirm on **G04 §G**. Bulk lock/unlock/favorite now record one `UndoManager` batch of `Kind.FLAG` children, and every bulk chip is the real `↩ Undo`. Clear this row once G04 §G passes. | 🟡 |
| G07-DEAD | `core/IdReferenceRegistry.java` | Health-scan only, zero callers (Health tab already cut) — confirmed dead | 🛠️ owner-approved delete, pending actual removal |

> ⚠️ **Moved out of G07:** the `glow > 0` place-lag (light appears late) is filed under **Group 06** as
> **G06-GLOW-LAG**. See that TG.

---

