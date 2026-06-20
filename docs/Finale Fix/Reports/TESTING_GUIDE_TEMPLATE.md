# 🧪 Testing Guide — Template

> **Copy the skeleton below for every group's testing guide.**
> 🎯 Goal: developer eye lands on **what to test right now** in seconds —
> calm, scannable, never a wall of text.

---

## 📋 Rules

- 🚦 **Status block first.** Fill Verdict + Progress dots after every test run.
- 🗺️ **At a glance second.** Map table follows Status — 🎯 TEST NOW always on top, each row links its section.
- 🔽 **Status order:** 🎯 Test now → ✅ Confirmed → 🟡 Polish later → ⏳ Not built. Never bury today's task below history.
- 🚫 **No bug reports here.** One-line `💡 Tests:` only. All cause/fix history → **PROGRESS_LOG**.
- 💡 **Soft section open.** One-line what-it-tests + 🧰 Before you start only if prep is needed.
- 🟦 **One test = one bullet.** Circled number · severity (🔴 blocker / 🟡 minor) · title · `command` · ✅ Pass. Add ❌ Broken if only when failure isn't obvious.
- 📋 **Scorecard + ↩️ Undo** at end of every section that changed something.
- 🤝 **Honest > impressive.** Built ≠ done. Partials → 🟡 in both the map and Polish section.

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 📐 Skeleton

```
# 🧪 Group NN — <Name> — Testing Guide

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🔴 Not tested · 🟡 Partial · 🟢 All pass |
| **Progress** | 🟥🟥🟥 · 0 / <N> passed |
| **Last tested** | — |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **<the new thing>** | §1 |
| ✅ Confirmed <date> | <feature> · <feature> | §2 |
| 🟡 Polish later | <partial item> | §3 |
| ⏳ Not built | <queued> · <queued> | §4 |

---

# 🎯 Test now

## §1 · <thing to test>
> 💡 <what this checks>

🧰 `<prep command or item>` *(skip if none)*

① **<title>** 🔴 — `<command>`
   ✅ <expected result>

② **<title>** 🟡 — `<command>`
   ✅ <expected result>
   ❌ <failure symptom>

📋 **Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| 🟥 | ① | <claim> |
| 🟥 | ② | <claim> |
| — | **0 / 2** | |

↩️ **Undo:** `<reset commands>`

---

# ✅ Confirmed

## §2 · <feature> — confirmed <date>

> 💡 <one-line re-check note>

---

# 🟡 Polish later

- <item> — <why it's parked>

---

# ⏳ Not built

| Feature | What it'll do | Spec |
|---|---|---|
| <x> | <y> | <group doc> |

---

## 🆘 If a test fails
- 🔢 Step number
- 👀 What happened vs expected (📸 screenshot helps)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`<commands to remove test blocks>`
```
