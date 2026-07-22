# Group 03 - HUD System & ESC Integration

## Status

| | |
| --- | --- |
| **Verdict** | All in-scope HUD/ESC features confirmed; bulk-lock padlock live-push fix confirmed in-game. |
| **Progress** | 🟩🟩🟩🟩🟩🟩🟩🟩🟩🟩 100% |
| **Last tested** | 2026-07-18 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| B | Bulk-lock padlock widget sync | Done ✅ | - |
| A | ESC pause-menu quick-access buttons | Done ✅ | - |
| D | Silent HUD after active-tool chip deletion | Done ✅ | - |
| E | Locked-block padlock sprite under crosshair | Done ✅ | - |
| F | Macro recording banner | Done ✅ | - |
| G | Incidents ping widget | Planned 📜 | Parked 💤 |
| H | Vault-upload progress toast widget | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_03_HUD_ESC.md](../groups/GROUP_03_HUD_ESC.md)

---

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §B Bulk-lock padlock widget sync — ✅ `2026-07-18`
- §A ESC pause-menu quick-access buttons — ✅ `2026-07-18`
- §D Silent HUD after active-tool chip deletion — ✅ `2026-07-15`
- §E Locked-block padlock sprite under crosshair — ✅ `2026-07-15`
- §F Macro recording banner — ✅ `2026-07-15`

</details>

<details><summary>💔 <b>Regression</b></summary>

</details>

<details><summary>📜 <b>Planned</b></summary>

</details>

<details><summary>💤 <b>Parked</b></summary>

- §G Incidents ping widget — 💤 `2026-07-18`: Owner still wants it eventually (quality-of-life, not urgent). Excluded from the progress bar until unparked.
- §H Vault-upload progress toast widget — 💤 `2026-07-18`: Owner still wants it eventually (quality-of-life, not urgent). Excluded from the progress bar until unparked.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

- Global widget-visibility server setting — 👎 `2026-07-18`: Owner does not want it; no real use case.
- Active-tool HUD chip — 👎 `2026-07-15`: Deleted by owner decision; hotbar feedback belongs to G04.
- Context-aware shortcuts and extra minor widgets — 👎 `2026-07-14`: Cut during the scope trim.
- Buzzer Game score/timer widget — 👎 `2026-07-18`: Owner does not want it.
- Favorites quick-bar widget — 👎 `2026-07-18`: Owner does not want it; favorites stay command/keybind only.
- Spinning 3D block-preview thumbnails on ESC buttons — 👎 `2026-07-18`: Owner is keeping the plain pause-menu buttons.
- Shareable HUD layout codes — 👎 `2026-07-18`: Owner does not want layout sharing; layouts stay local per-player.

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Keep old HUD persistence and cancel-editor checks out of G03 if they are covered by G27.
- [ ] Do not recreate the active-tool HUD chip.

</details>
