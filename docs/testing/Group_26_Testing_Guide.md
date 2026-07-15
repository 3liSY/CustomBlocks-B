# Group 26 — Name & Give Fixes + Named-Texture Mirror

**🎯 Active:** A (0/14, reverted) · B/C/D reverted (0/6)  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                                        |
| --------------- | ------------------------------------------------------ |
| **Verdict**     | 🎯 A + B/C/D reverted, need retesting (🛠️ A rework + cooler customization also queued) |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/20)                 |
| **Last tested** | 2026-06-29                                             |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                                                | State     |
| --- | ------------------------------------------------------------------- | --------- |
| B | FIX A — clean display names (Title Case, no underscores)            | 🎯         |
| C | FIX B — `/cb give <id>` case-insensitive                            | 🎯         |
| D | FIX D — multiplayer display name (dedicated server)                 | 🎯         |
| A | Named-texture mirror (`textures_names/` · `/cb config mirrornames`) | 🛠️ polish |

🔗 **Related Doc:** [GROUP_26_NAME_AND_GIVE_FIXES.md](../groups/GROUP_26_NAME_AND_GIVE_FIXES.md)

---

# 🎯 Test now

## A · Named-texture mirror · 🛠️ 7/7 (rework queued)

> 💡 Optional human-readable copy of the texture folder: files named by block (`Alef Black.png`).
> 🧰 Need a few textured blocks. Folder to watch: `config\customblocks\textures_names\`

| # | Action                                                  | Expected Result                                                           | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | `/cb config mirrornames on`                             | `textures_names\` fills with PNGs named cleanly; no block changes in-game | 🎯 | 🎯 |
| A2 | `/cb config mirrornames` (no arg)                       | shows `ON`, file count, and folder path                                   | 🎯 | 🎯 |
| A3 | `/cb create mirrortest Mirror_test` + give it a texture | **Mirror Test.png** appears in `textures_names\`                          | 🎯 | 🎯 |
| A4 | `/cb rename mirrortest Renamed_demo`                    | old `Mirror Test.png` gone; `Renamed Demo.png` present                    | 🎯 | 🎯 |
| A5 | `/cb delete mirrortest` (current id)                    | that block's PNG disappears                                               | 🎯 | 🎯 |
| A6 | `/cb config mirrornames rebuild`                        | folder wiped + regenerated from scratch                                   | 🎯 | 🎯 |
| A7 | `/cb config mirrornames off`                            | existing files stay; new creates/renames no longer change the folder      | 🎯 | 🎯 |

> ⚠️ **A1-A7 reverted to needs-testing 2026-07-05** — stale (were passed before the 07-03 confirm cutoff, needs retest).

> 🛠️ **Owner finding (2026-06-29):** works but wants a **rework + improvement pass** — richer/cooler customization options for the named-texture mirror before final sign-off. Functional, not closed.

---

<details><summary>⏳ <b>Planned / Parked</b> (click to open)</summary>

*(none)*

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **B · clean display names** (Passed 2026-06-15). Test history moved to [GROUP_26_NAME_AND_GIVE_FIXES.md](GROUP_26_NAME_AND_GIVE_FIXES.md). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · case-insensitive give** (Passed 2026-06-15). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · multiplayer display name** (Passed 2026-06-20). ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

