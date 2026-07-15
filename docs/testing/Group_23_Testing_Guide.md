# Group 23 — Player Experience (Onboarding & Achievements)

**🎯 Active:** None  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                                   |
| --------------- | ------------------------------------------------- |
| **Verdict**     | 🎯 needs retest — B–F reverted to needs-testing (stale, pre-07-03) · A not built · G cross-ref G27 |
| **Progress**    | 🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯🎯 · 0% (0/22)                 |
| **Last tested** | 2026-06-22/23                                     |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-10): achievements gallery is already decided Screen-based (owned by G27
> §G27.16, `GROUP_23_PLAYER_EXPERIENCE.md` §8) — consistent with the mod-wide Screen migration. §D's
> "text list" is the actual interim shipped behavior (not chest GUI); dashboard tabs/slots (§E) belong to
> Group 02's own chest dashboard, out of this group's scope.

## 🗺️ Sections

| § | What                                          | State     |
| --- | --------------------------------------------- | --------- |
| B | Achievement + hint engine                     | 🎯         |
| C | Restart persistence · 10-block milestone      | 🎯         |
| D | `/cb achievements` text list                  | 🎯         |
| E | Dashboard — Tip slot + Achievements slot      | 🎯         |
| F | Starter Guide book on first join              | 🎯         |
| A | Sample blocks on fresh install                | ⏳ planned |
| G | Tutorial screen + Achievements gallery screen | ➡️ G27    |

🔗 **Related Doc:** [GROUP_23_PLAYER_EXPERIENCE.md](../groups/GROUP_23_PLAYER_EXPERIENCE.md)

---

# 🎯 Test now

*(B–F reverted to needs-testing 2026-07-05 — stale pre-07-03 passes, need retest; row-level tests live in [GROUP_23_PLAYER_EXPERIENCE.md](GROUP_23_PLAYER_EXPERIENCE.md))*

---

<details><summary>⏳ <b>Planned / Parked / Notes</b> (click to open)</summary>

**A · Sample blocks**
| # | Action                                                   | Expected Result                                | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Fresh install (no blocks in `slots.json`) → start server | 5 bundled example blocks present and placeable | 🎯 | 🎯 |

> ⚠️ **Known gap (not a G23 bug):** give-hint mentions an offhand hologram preview not yet built → "Coming soon." Wording left as-is until hologram feature (G19) ships.

**G · Tested under G27 (screens cross-ref)**
Screen pieces live in `docs/testing/GROUP_27_TESTING_GUIDE.md`:
- First-join cinematic welcome video (§G27.32)
- Achievements gallery screen — trophy wall + progress bar (§G27.16)

Group 23 owns the **engine data** those screens read. That data now needs retest here (B–C) — see Archive.

</details>

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **B · Achievement + hint engine** (Passed 2026-06-22). Test history moved to [GROUP_23_PLAYER_EXPERIENCE.md](GROUP_23_PLAYER_EXPERIENCE.md).
  ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **C · Restart persistence + 10-block milestone** (Passed 2026-06-23).
  ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **D · `/cb achievements` text list** (Passed 2026-06-23).
  ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **E · Dashboard slots** (Passed 2026-06-23).
  ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).
- **F · Starter Guide book** (Passed 2026-06-23).
  ⚠️ reverted to needs-testing 2026-07-05 — stale (was passed before the 07-03 confirm cutoff, needs retest).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
|        |          |                   |        |

---

