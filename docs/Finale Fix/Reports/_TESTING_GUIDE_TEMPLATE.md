# 🧪 Testing Guide — Blueprint (v3)

> **Copy the skeleton below for every group's testing guide.**
> 🎯 Goal: the developer's eye lands on **what to test right now** in seconds — everything else is
> calm, scannable, and never a wall of text.
>
> *(v1 = Group 06 · v2 = status table + ①② blocks · **v3 = bullets + emoji grouping + soft intros**,
> set 2026-06-13. Look at `GROUP_25_TESTING_GUIDE.md` for a live v3 example.)*

---

## 🎨 The style in 6 rules

- 🗺️ **Open with a one-glance map.** The first thing in the file is the **At a glance** table —
  one row per status, 🎯 TEST NOW always on top, each row pointing to its section.
- ✂️ **Two halves, one big divider each:** `# 🎯 Test now` (the work) and `# ✅ Passed` (the
  history). New stuff lands under *Test now*; once you confirm it, tick its scorecard and move it
  down to *Passed*.
- 💡 **Start every section soft:** a one-line **What it does** + a 🧰 **Before you start** bullet
  list (everything to prep, so no test dies mid-run on a missing block).
- 🟦 **One test = one bullet.** Circled number + title, the command in `code`, then a ✅ **Pass**
  line (add ❌ **Broken if** only when the failure isn't obvious). No dense paragraphs.
- 📋 **End each section with a Scorecard** (`✓ | # | Proves`) and a ↩️ **Undo the test** line if it
  changed anything.
- 🤝 **Honest > impressive.** Built ≠ done (Golden Rule). Partials are 🟡 in *both* the map and the
  Polish section. Deep history goes to the progress log, not here.

**Legend:**  🎯 test now · ✅ confirmed in-game · 🟡 works, polish later · ⏳ not built yet

---

## 📐 Skeleton — copy everything below this line

```
# 🧪 Group NN — <Name> — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| 🎯 **TEST NOW** | **<the new thing>** | §1 |
| ✅ Passed <date> | <feature> · <feature> | §2 |
| 🟡 Polish later | <partial item> | §3 |
| ⏳ Coming | <queued> · <queued> | §4 |

---

# 🎯 Test now

## §1 · <thing to test>

> 💡 **What it does:** <one plain sentence>.

> 🧰 **Before you start:**
> - <block to place / item to grab>
> - <command to run once>

**Try these:**

- **① <short title>**
  `<command>`
  - ✅ **Pass:** <result>
  - ❌ **Broken if:** <symptom>   ← *(only when not obvious)*

- **② <short title>** — <action, no command needed>
  - ✅ **Pass:** <result>

**📋 Scorecard**

| ✓ | # | Proves |
|:--:|:--:|---|
| ⬜ | ① | <one-line claim> |
| ⬜ | ② | <…> |

> ↩️ **Undo the test:** `<reset commands>`

---

# ✅ Passed — kept for re-test reference

## ✅ §2 · <feature> — passed <date>

> Confirmed in-game <date>. Kept so you can re-check after future changes.

<the section, scorecard ticked ✅>

---

# 🟡 Polishing later  *(known, not bugs)*

- <item> — <why it's parked>

# ⏳ Coming next  *(not built — nothing to test yet)*

| Feature | What it'll do |
|---|---|
| <x> | <y> |

---

## 🆘 If a test fails, send me
- 🔢 the step number
- 👀 what happened vs what you expected (📸 a screenshot helps)
- 📄 the last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`<commands to remove the test blocks>`
```
