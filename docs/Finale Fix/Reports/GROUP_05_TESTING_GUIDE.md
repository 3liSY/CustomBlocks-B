# Group 05 — Silent Resource Pack Delivery · Testing Guide

*One green build proves it compiles — nothing is done until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ confirmed  ·  🟡 polish later  ·  ⏳ not built

---

## Where Group 05 stands

> ☑️ **Done — verified in-game (2026-06-10).** Textures apply with **no** "download resource pack?"
> dialog; the `silentPack` toggle restores it; rapid edits debounce into one rebuild; textures
> survive a restart. All of G05.1–G05.6 confirmed.

Nothing required to test — listed below for re-verification only.

---

## ☑️ §1 · Confirmed working — re-check only if something feels off

> Have a real direct image URL ready (browser → Copy Image Address). `/cb create g05a SilentPackTest`.

| Feature | Quick check |
|---|---|
| Silent create | `/cb create g05b T <url>` → texture applies, **no dialog** |
| Silent retexture | `/cb retexture g05a <url>` → updates, no dialog |
| Toggle restores dialog | `/cb config silentpack off` → retexture → vanilla dialog returns; `on` to restore |
| Debounce | two retextures within ~1s → **one** `Rebuilding resource pack…` log line |
| Persistence | retexture, restart, rejoin → texture loads, silent (no dialog on join) |
| Config row | `/cb config` → glowing **Silent Pack** row, click toggles in place |

---

## 📌 §2 · Good to know (Minecraft limits, not bugs)
- **Client-side + scoped** — silence only kicks in after *our* server's `SilentPackPayload`; resets on
  disconnect, so **other servers' packs are never auto-accepted**. A player without the mod client-side
  still sees the vanilla dialog.
- **Forced packs always prompt** — a documented MC limit; this mod sends `required=false`, so those
  are the ones silenced.
- **Debounce ~500ms** — deliberately slow edits (>500ms apart) rebuild separately by design.

---

## If a test fails
Send: the test number, whether a dialog appeared (screenshot ideal), and the `logs/latest.log` lines
at the pack push (esp. `Rebuilding resource pack`, `Mixin`, `createConfirmServerResourcePackScreen`).
For the toggle: the `silentPack` value in `config/customblocks/config.json`.

## Cleanup
```
/cb delete g05a
/cb delete g05b
/cb delete g05c
```
