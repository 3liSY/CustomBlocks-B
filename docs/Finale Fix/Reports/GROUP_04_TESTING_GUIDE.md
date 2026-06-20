# 🧪 Group 04 — Chat Messages & Command Communication — Testing

> 🟢 Build green = compiles. ✋ Only in-game confirms it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:** 🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🚦 Status

| | |
|---|---|
| **Verdict** | 🟢 All pass |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩 · 9 / 9 passed |
| **Last tested** | 2026-06-10 |
| **Jar** | 1.0.0 |
| **Tester** | — |

---

## 🗺️ At a glance

| | What | § |
|:--:|---|:--:|
| ✅ Passed 2026-06-10 | Upgraded wording · DidYouMean · `/cb help` GUI · `/cb welcome` · incident routing | §1 |
| 🟡 Polish later | chat-formatting pass · `/cb rename` "already named" message · short bodies on low-traffic commands | §2 |

> 👍 **Nothing to test right now** — Group 04 is confirmed. Listed below for re-checks only.

---

# ✅ Passed — re-check only if something feels off

## ✅ §1 · Confirmed working 2026-06-10

| 🔎 Feature | Quick check |
|---|---|
| ✏️ Upgraded wording | `/cb create g04b StyleTest` → `[CB] Block "g04b" ("StyleTest") created successfully. ✔` |
| ⛔ Error tone | locked block → `[CB] "g04a" is locked. Use /cb unlock g04a to edit it. ✖` |
| 🎁 Give wording | `/cb give g04a` → `[CB] Gave you 1 × ChatTest. ✔` *(name, not id)* |
| 🌐 Bad-URL error | retexture a fake URL → human-readable `Couldn't get a texture…`, no stack trace |
| 📒 Incident routing | after that failure → `/cb incidents` lists it (time / id / player / url) |
| 🔤 DidYouMean | `/cb cretae g04a` → clickable `[/cb create g04a]` · `/cb xyzzy` → no guess, offers `/cb help` |
| ⚙️ DidYouMean config | `/cb config didyoumean off` silences it; `/cb config` has a click-to-cycle Typo Correction row |
| 📖 `/cb help` | chest GUI, 9 categories → command list → click pre-fills chat |
| 👋 `/cb welcome` | greeting + 3 clickable buttons (create / dashboard / help) |

---

## 🟡 §2 · Polish later

- 💬 **Chat formatting pass** — a dedicated formatting polish is queued.
- ✏️ **`/cb rename` "already named" message** — renaming a block to the name it already has fake-succeeds (`Renamed "x" to "x"`); should say "already named that" instead.
- 🗣️ **Tone is a first pass** — all 163 message sites carry the brand; some low-traffic commands (templates, macros, arabic, cloud) still read short.
- 🔤 **DidYouMean matches the first word only** (the game handles a bad second word) — same as the old project.
- 🖥️ **Chat pre-fill needs the mod client-side**; a console `/cb help` prints a text list.

---

## 🆘 If a test fails

- 🔢 Step number
- 👀 What happened vs expected (📸 screenshot helps for the help GUI / config row / pre-filled chat)
- 📄 Last ~20 lines of `.minecraft\logs\latest.log`

## 🧹 Cleanup
`/cb unlock g04a` · `/cb delete g04a` · `/cb delete g04b` · `/cb incidents clear`
