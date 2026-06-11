# Group 04 — Chat Messages & Command Communication · Testing Guide

*One green build proves it compiles — nothing is done until you confirm it in-game.*

**Legend:**  🎯 test now  ·  ☑️ confirmed  ·  🟡 polish later  ·  ⏳ not built

---

## Where Group 04 stands

> ☑️ **Done — verified in-game (2026-06-10).** Upgraded message wording (brand `[CB]` + ✔/✖ kept),
> DidYouMean typo correction, `/cb help` GUI, `/cb welcome`, incident routing — all confirmed.
> 🟡 **Polish later** — chat *formatting* polish pass (your call this chat); some low-traffic command
> bodies still read short.

Nothing required to test — listed below for re-verification only.

---

## ☑️ §1 · Confirmed working — re-check only if something feels off

| Feature | Quick check |
|---|---|
| Upgraded wording | `/cb create g04b StyleTest` → `[CB] Block "g04b" ("StyleTest") created successfully. ✔` |
| Error tone | locked block → `[CB] "g04a" is locked. Use /cb unlock g04a to edit it. ✖` |
| Give wording | `/cb give g04a` → `[CB] Gave you 1 × ChatTest. ✔` (name, not id) |
| Bad-URL error | retexture a fake URL → human-readable `Couldn't get a texture…`, no stack trace |
| Incident routing | after that failure, `/cb incidents` lists it (time / id / player / url) |
| DidYouMean | `/cb cretae g04a` → clickable `[/cb create g04a]`; `/cb xyzzy` → no guess, offers `/cb help` |
| DidYouMean config | `/cb config didyoumean off` silences it; `/cb config` has a click-to-cycle Typo Correction row |
| `/cb help` | chest GUI, 9 categories → command list → click pre-fills chat |
| `/cb welcome` | greeting + 3 clickable buttons (create / dashboard / help) |

---

## 🟡 §2 · Polishing later (known, not bugs)
- **Chat formatting polish** — a dedicated formatting pass is queued (this chat).
- **Tone is a first pass** — all 163 message sites carry the brand; some low-traffic commands
  (templates, macros, arabic, cloud) still have shorter bodies worth friendlier wording.
- **DidYouMean matches the first word only** (Brigadier handles a bad second word with its own usage
  error) — same as the old project.
- **Chat pre-fill needs the mod on the client** (client packet); console `/cb help` prints a text list.

---

## If a test fails
Send: the test (e.g. DidYouMean), exact message vs expected (screenshot for help GUI / config row /
pre-filled chat), and `logs/latest.log` lines naming `DidYouMean` / `HelpMenu` / `ChatPrefillPayload` /
`IncidentRecorder`. For incidents: `config/customblocks/incidents.json`.

## Cleanup
```
/cb unlock g04a
/cb delete g04a
/cb delete g04b
/cb incidents clear
```
