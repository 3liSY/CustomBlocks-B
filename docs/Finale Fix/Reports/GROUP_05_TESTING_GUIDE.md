# 🧪 Group 05 — Silent Resource Pack Delivery — Testing

> 🟢 Build green = it compiles.   ✋ Only you, in-game, can say it works.
> 📦 Jar: `.minecraft\mods\customblocks-1.0.0.jar`

**Legend:**  🎯 test now · ✅ confirmed · 🟡 polish later · ⏳ not built

---

## 🗺️ At a glance

| | What | Where |
|:--:|---|:--:|
| ✅ Passed 2026-06-10 | Textures apply with **no** "download pack?" dialog · `silentPack` toggle · debounce · survives restart | §1 |
| 📌 Good to know | Minecraft limits (not bugs) | §2 |

> 👍 **Nothing to test right now** — all of G05.1–G05.6 confirmed. Listed below for re-checks only.

---

# ✅ Passed — re-check only if something feels off

## ✅ §1 · Confirmed working 2026-06-10

> 🧰 **Prep:** a real direct image URL (browser → Copy Image Address) · `/cb create g05a SilentPackTest`.

| 🔎 Feature | Quick check |
|---|---|
| 🤫 Silent create | `/cb create g05b T <url>` → texture applies, **no dialog** |
| 🤫 Silent retexture | `/cb retexture g05a <url>` → updates, no dialog |
| 🔀 Toggle restores dialog | `/cb config silentpack off` → retexture → vanilla dialog returns; `on` to restore |
| ⏱️ Debounce | two retextures within ~1s → **one** `Rebuilding resource pack…` log line |
| 💾 Persistence | retexture → restart → rejoin → texture loads, silent (no dialog on join) |
| ⚙️ Config row | `/cb config` → glowing **Silent Pack** row, click toggles in place |

---

## 📌 §2 · Good to know  *(Minecraft limits, not bugs)*

- 🎯 **Client-side + scoped** — silence only kicks in after *our* server's `SilentPackPayload`; it
  resets on disconnect, so **other servers' packs are never auto-accepted**. A player without the
  mod still sees the vanilla dialog.
- 🔒 **Forced packs always prompt** — a documented MC limit; this mod sends `required=false`, so
  those are the ones it can silence.
- ⏱️ **Debounce ~500ms** — deliberately slow edits (>500ms apart) rebuild separately by design.

---

## 🆘 If a test fails, send me
- 🔢 the test number
- 👀 whether a dialog appeared (📸 screenshot ideal)
- 📄 `logs/latest.log` at the pack push (esp. `Rebuilding resource pack`, `Mixin`, `createConfirmServerResourcePackScreen`)
- ⚙️ for the toggle: the `silentPack` value in `config/customblocks/config.json`

## 🧹 Cleanup
`/cb delete g05a` · `/cb delete g05b` · `/cb delete g05c`
