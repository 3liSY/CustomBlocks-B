# Testing Guide — Format Standard (v2)

> **Copy this skeleton for every group's testing guide.** Goal: the developer's eye lands on what to
> test **right now**; everything else is scannable, never a dump. (v1 established with Group 06;
> v2 upgrade — status table + ①② test blocks + scorecard — established 2026-06-11, see
> `GROUP_06_TESTING_GUIDE.md` for a live example.)

**Rules**
- The **"Where Group NN stands" status table** is the first thing in the file: one row per status
  (🎯 / ☑️ / 🟡 / ⏳), TEST NOW always the top row, each row linking its section.
- Two big `#` dividers split the file: `# 🎯 TEST NOW` (the work) and
  `# ☑️ PASSED — kept for re-test reference` (the history). New stuff goes under TEST NOW;
  when the developer confirms it, mark the section ☑️, tick its scorecard, move it below the
  PASSED divider.
- Every TEST NOW section opens with **"In one line:"** (what the feature does, plain words) and a
  🧰 **"Before you start"** quote (everything to prepare, so no test dies mid-run on a missing block).
- Each test is its own `### ① <title>` block (circled numbers ①②③…): the command in a code
  block, then ONE bold **Expect:** line. Add *Broken if:* only when the failure mode isn't obvious.
- End each TEST NOW section with a **Scorecard** table (`✓ | # | Proves`) using the same ①②…
  markers, and a ↩️ **reset tip** if the tests changed any state.
- **No fake test steps for unbuilt features** — they live in the ⏳ Coming table only.
- Keep status honest — built ≠ done (Golden Rule). Partials get 🟡 in BOTH the status table and §Polish.
- Keep it short. Deep history goes to the progress log, not this file.

**Legend:**  🎯 test now  ·  ☑️ confirmed in-game  ·  🟡 works, polish later  ·  ⏳ not built yet

---

### Skeleton (copy below this line)

```
# Group NN — Testing Guide

*One green build proves it compiles — nothing is done until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ confirmed  ·  🟡 polish later  ·  ⏳ not built

---

## Where Group NN stands

| | What | Where |
|---|---|---|
| 🎯 **TEST NOW** | **<the new thing>.** Build green, jar in `.minecraft\mods`, **never run in-game.** | **§1** |
| ☑️ Passed <date> | <feature> (n/n) · <feature> (n/n) | §2 · §3 |
| ☑️ Working | <verified features, dot-separated> | §4 |
| 🟡 Polish later | <partial items, dot-separated> | §5 |
| ⏳ Coming | <queued items, dot-separated> | §6 |

---

# 🎯 TEST NOW

## §1 · <thing to test>

**In one line:** <what it does, one sentence, plain words>.

> 🧰 **Before you start:** <blocks to place, items to grab, commands to run once>.

---

### ① <short test title>
​```
<command>
​```
**Expect:** <result>. *Broken if:* <symptom — only when not obvious>.

### ② <short test title>
<action, no command needed>
**Expect:** <result>.

---

### Scorecard

| ✓ | # | Proves |
|---|---|---|
| ⬜ | ① | <one-line claim this test proves> |
| ⬜ | ② | <…> |

> ↩️ **Put it back anytime:** <reset commands, if the tests changed state>.

---

# ☑️ PASSED — kept for re-test reference

## ☑️ §2 · <passed feature> — ALL n TESTS PASSED <date>

> ✅ Confirmed in-game on <date>. Kept for re-test reference.

<the section, unchanged, scorecard ticked ☑️>

---

## ☑️ §4 · Already working — re-test only if something feels off

| Feature | Quick check |
|---|---|

## 🟡 §5 · Polishing later (known, not bugs)
- <item> — <why later>

## ⏳ §6 · Coming next (not built — nothing to test)

| | Feature | What it'll do |
|---|---|---|

## If a test fails
Send: the step number, what happened vs expected (screenshot helps), last ~20 lines of `latest.log`.

## Cleanup
​```
<cleanup commands>
​```
```
