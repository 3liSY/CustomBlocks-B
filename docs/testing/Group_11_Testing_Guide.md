# Group 11 — Category System

**🎯 Active:** C/D/E reverted (0/6) · F (0/0)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                  |
| --------------- | -------------------------------- |
| **Verdict**     | 🎯 3 reverted sections need retesting |
| **Progress**    | 🎯🎯🎯🎯🎯🎯 · 0% (0/6) |
| **Last tested** | 2026-06-14                       |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): CategoryListMenu/CategoryBrowserMenu/CategoryEditMenu retired — CategoryHubScreen (G27) already replaces them, needs wiring. ExportDashboardMenu → Screen, not built. See `GROUP_11_CATEGORY.md`.
> Screen spec moved 2026-07-12 → `GROUP_27_SCREENS.md` §G27.29.

## 🗺️ Sections

| § | What                                                  | State     |
| --- | ----------------------------------------------------- | --------- |
| B | Share / Import category by code                       | ❗ blocked |
| C | Unified `/cb category` command                        | 🎯         |
| D | Browser · edit menu · basics                          | 🎯         |
| E | Export — Bulk Choose + Dashboard                      | 🎯         |
| F | Category Forge replacement direction                  | 🛠️ polish |
| A | Lock/Unlock All · Bulk Retexture · Stats · Sort Order | 🧊 parked  |

🔗 **Related Doc:** [GROUP_11_CATEGORY.md](../groups/GROUP_11_CATEGORY.md)

---

# 🎯 Test now

*(Track F needs polishing after in-game test. Current shipped items confirmed.)*

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

| § | What it'll do                                                                                                                                                                | Spec                                                                                                                                            |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| A | Lock/Unlock All · Bulk Retexture · Stats · Sort Order (built, untested)                                                                                                      | —                                                                                                                                               |
| B | Share category (upload → short code) · Import category (download by code; keep/new/uncategorized)                                                                            | needs cb-cloud-vault Worker                                                                                                                     |
| F | `/cb create` Category tab replacement: multiple categories per block, one main badge, advanced in-game customization, tree/subcategories, delete modes, templates, migration | cramped first sample rejected; wide sample built in `StudioCategoryWorkspacePanel`; `/cb category` and `/cb categories` still need the new flow |

</details>

<details><summary>📝 <b>F · Locked replacement direction</b> (click to open)</summary>

**F · Category Forge / `/cb create` Category tab**
- Direction locked 2026-06-30 after prototype iteration.
- The accepted mockup direction is `docs/mockups/category_create_tab_v2.html`.
- It must appear as a left-side Category tab inside `/cb create`, not as a separate random manager screen.
- It should blend Minecraft-style preview/slots with a modern advanced screen.
- It replaces the current category system eventually, but the current verified G11 behavior stays baseline until the new flow is built and tested.
- Core model: multiple categories per block, exactly one visible main category badge, empty categories allowed, unlimited subcategory tree with a warning after 2 sub-levels, parent views include child blocks.
- Customization scope: category icon/source, custom block icon, color, badge, description, templates, sort/reorder, hidden/locked, permissions, sounds, particles, auto-add rules, import/export payload.
- Delete flow must be separated and clear: category only; exclusive blocks only; all shown tree blocks; move blocks then delete.
- Migration must preserve old/current categories and assignments automatically.
- Status after first in-game build: **needs polishing after in-game test**.

</details>

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **C · Unified `/cb category` command** (Passed 2026-06-14). Test history moved to [GROUP_11_CATEGORIES.md](GROUP_11_CATEGORIES.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · Browser · edit menu · basics** (Passed 2026-06-14). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · Export** (Passed 2026-06-14). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

