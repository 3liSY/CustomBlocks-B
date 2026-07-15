# Group 05 — Silent Resource Pack Delivery

**⚠️ Known bug (open, SP only):** B3 — a fast SP create/retexture burst can still fire multiple reloads + `Corrupt PNG`. MP is unaffected. Only unclosed item.  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                |
| --------------- | ------------------------------ |
| **Verdict**     | ✅ 12/13 pass — B3 (SP only) open, everything else good |
| **Progress**    | ✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅⚠️ · 96% (25/26 pass · 1 known bug) |
| **Last tested** | 2026-07-03                     |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)
>
> ⚠️ UI medium audit (2026-07-09): TextureSizeMenu + RetextureConfirmMenu → Screen, not built. See `GROUP_05_RESOURCE_PACK.md`.
>
> ⏳ Group reopened 2026-07-15: § G (G05-6) live in-place texture swap — DESIGN only, awaiting owner ✅ to build.

## 🗺️ Sections

| § | What                                | State |
| --- | ----------------------------------- | ----- |
| G | Live in-place texture swap — no full reload on a pixel-only edit | ⏳ DESIGN — awaiting ✅ to build (G05-6) |
| H | Skip redundant first-join reload (SP) — persist applied hash | ⏳ DESIGN — awaiting ✅ to build (G05-7) |
| F | Emit atlas guard + rogue slot_1281/1650 repair | 🟢 0/3 — built 2026-07-06, awaiting in-game (G05-5b) |
| E | Per-client low-res mode (weak-GPU fix) | 🟢 0/6 — built 2026-07-06, awaiting in-game (G05-5) |
| B | Silent create / retexture / restart | 🟡 5/6 (B3 ⚠️ known bug) |
| A | Face-swap on fast regens            | ✅ 3/3 |
| C | Modded-client local pack generation | ✅     |

🔗 **Related Doc:** [GROUP_05_RESOURCE_PACK.md](../groups/GROUP_05_RESOURCE_PACK.md)

---

# ⚠️ Open bug — B3 (SP only, everything else in Group 05 ✅)

## B · Silent delivery · 🟡 5/6 — B3 open (SP only) 2026-07-03

> 💡 Client should apply pack silently without a reload dialog.
> 🧰 `/cb create g05a SilentPackTest`

| # | Action                         | Expected Result                              | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | `/cb create g05b T <url>`      | texture applies, **no dialog**               | ✅ | ✅ |
| B2 | `/cb retexture g05a <url>`     | updates, no dialog                           | ✅ | ✅ |
| B3 | two fast image ops (create/retexture) | one silent reload, zero `Corrupt PNG` — see Active Bugs | ⚠️ | ✅ |
| B4 | retexture → restart → rejoin   | texture loads on rejoin, no dialog           | ✅ | ✅ |
| B5 | `/cb config` → Silent Pack row | glowing row, click toggles in place          | ✅ | ✅ |
| B6 | the always-silent behaviour    | mod always keeps pack silent                 | ✅ | ✅ |

---

<details><summary>📌 <b>Minecraft limits (not bugs)</b> (click to open)</summary>

- **Client-side + scoped** — silence only kicks in after *our* server's signal; resets on disconnect; other servers' packs never auto-accepted.
- **Forced packs always prompt** — Minecraft limit; this mod sends `required=false`, so ours are the ones it can silence.
- **Debounce ~500ms** — slow edits (>500ms apart) rebuild separately by design.

</details>

---

# ⏳ Design — G (G05-6, live in-place texture swap — NOT built, awaiting ✅)

## G · Live in-place texture swap · ⏳ DESIGN 2026-07-15 — no code yet, awaiting owner ✅

> 💡 A pixel-only edit should push the new pixels straight to the GPU and skip the full `reloadResources()` freeze; structural edits (new block / shape change) still take one reload. Full design in [G05-6](../groups/GROUP_05_RESOURCE_PACK.md#g05-6--live-in-place-texture-swap--kill-the-full-reload-on-a-pixel-only-edit--the-reload-freezes-every-edit-pain--design--awaiting--to-build).
> 🛠️ Phased (coverage corrected 2026-07-15 by code sweep): Phase 1 = off-atlas WORLD blocks (animated + Arabic) + ALL item icons. Phase 2 = atlas WORLD blocks (the common static block, per-face, shapes). The common static block's in-world look needs Phase 2, NOT Phase 1.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | **P1** edit an ANIMATED or ARABIC block's texture | placed block updates **instantly** — no freeze, no reload log line | ⏳ | ⏳ |
| G2 | **P1** edit ANY block, check its hand/inventory ICON | icon updates instantly (all icons are off-atlas) | ⏳ | ⏳ |
| G3 | rapid burst of pixel edits | each lands, no stall, zero `Corrupt PNG` (obsoletes B3 for pixel edits) | ⏳ | ⏳ |
| G4 | create a NEW block, or change a block's SHAPE | still one silent reload (structural — expected, not a bug) | ⏳ | ⏳ |
| G5 | **P2** edit a COMMON static / per-face / shape block, look at the PLACED block | its atlas sprite updates live, no full re-stitch stall | ⏳ | ⏳ |

---

# ⏳ Design — H (G05-7, skip redundant first-join reload — NOT built, awaiting ✅)

## H · Skip redundant first-join reload (SP/integrated) · ⏳ DESIGN 2026-07-15 — no code yet, awaiting owner ✅

> 💡 On a singleplayer session's first world-open the game reloads once even when nothing changed since last session; persisting the applied pack hash to disk lets it skip that when the on-disk pack is already correct. Full design in [G05-7](../groups/GROUP_05_RESOURCE_PACK.md#g05-7--skip-the-redundant-first-join-reload-singleplayerintegrated--persist-the-applied-hash--design--awaiting--to-build).
> 🛠️ Vanilla BOOT reload (all packs at launch) is out of scope — not removable.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | launch, open the same SP world with NO changes since last session | NO reload overlay; all blocks present + correct textures | ⏳ | ➖ |
| H2 | create/edit a block, relaunch, open world | ONE reload (data changed — expected); the edit is visible | ⏳ | ➖ |
| H3 | delete `.cbpackhash` (or the loose folder), open world | forces a full reload; nothing missing afterward | ⏳ | ➖ |
| H4 | join a dedicated server | unchanged — still uses the manifest-diff path (already skips when current) | ➖ | ⏳ |

---

# 🟢 Built — E (G05-5, awaiting in-game confirm)

## E · Per-client low-res mode (weak-GPU atlas-overflow fix) · 🟢 0/6 — built 2026-07-06, NOT yet confirmed in-game

> 💡 Weak-GPU client opts into shrunk textures so its block atlas fits; everyone else keeps 512. See [G05-5](../groups/GROUP_05_RESOURCE_PACK.md#g05-5--per-client-low-res-texture-mode--weak-gpu-resource-reload-failed-atlas-overflow).
> 🧰 Needs the weak-GPU friend (e.g. GT 730) on the dedicated server. Non-op is fine — the command is client-side.
> 🧰 Command: `/cblowres 256` · `/cblowres 128` · `/cblowres off` · bare `/cblowres` = status. No rejoin needed.
> ⚙️ Built behaviour: shrinks every texture PNG (static + off-atlas GIF/Arabic sheets) to fit the chosen size, aspect-preserved; per-server setting; re-pull is diffed against the server's 512 shas so rejoin is cheap; **256 auto-steps down to 128** if the reload still drops the pack.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | weak-GPU friend runs `/cblowres 256` (no rejoin) | NO "resource reload failed"; custom blocks render; chat "low-res 256px applied" | ➖ | 🎯 |
| E2 | if 256 still overflows | mod AUTO-tries 128 (chat says so); client loads, blocks render — or a manual `/cblowres 128` | ➖ | 🎯 |
| E3 | `/cblowres off` | re-pulls full 512 + reloads; blocks render (capable client) | ➖ | 🎯 |
| E4 | relaunch after E1 | setting persists; still low-res, no needless full re-download | ➖ | 🎯 |
| E5 | look at GIF / Arabic blocks in low-res | present + animating, just softer — no out-of-VRAM crash | ➖ | 🎯 |
| E6 | other player + owner (capable GPU) | still see full 512, unaffected | ➖ | 🎯 |

---

# 🟢 Built — F (G05-5b atlas guard, awaiting in-game confirm)

## F · Emit oversized-texture guard + rogue slot_1281/1650 repair · 🟢 0/3 — built 2026-07-06, NOT yet confirmed in-game

> 💡 No emitted block texture may exceed `textureSize` (default 512). Two stale 4088px files in the owner's loose pack (slot_1281 = Ha Red Mid, slot_1650 = Sad Yellow Fin) were repaired to the correct 512 store versions; the emit path now downscales any oversized static texture so it can't recur.
> 🧰 Both blocks are Arabic tiles already placed/creatable; a full pack regen happens on any create/retexture or rejoin.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | look at slot_1281 (Ha Red Mid) + slot_1650 (Sad Yellow Fin) in-world | render correctly, same as before — no purple/missing texture | 🎯 | 🎯 |
| F2 | trigger a pack regen (any `/cb create` or `/cb retexture`), then re-check the loose pack | no `slot_*.png` in `resourcepacks/CustomBlocks/.../block/` exceeds 512×512 | 🎯 | ➖ |
| F3 | general VRAM/atlas feel after regen | no atlas-stitch warnings in the log; weak-GPU pressure reduced | 🎯 | 🎯 |

---

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

### A · Face-swap on fast regens · ✅ 3/3 — passed 2026-07-03

> 💡 Serialized + atomic writes prevent two fast edits from clobbering each other's PNG/model files.
> 🧰 `/cb create g05c FaceA <url1>` · `/cb create g05d FaceB <url2>`

| # | Action                                                          | Expected Result                                               | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | retexture both back-to-back fast, ~5×                           | each block shows **its own last-set texture** — no cross-swap | ✅ | ✅ |
| A2 | during that burst, watch faces                                  | no purple / blank / half-loaded face                          | ✅ | ✅ |
| A3 | also `/cb retexture g05c <url1>` twice within ~1s; repeat burst | still no swap, still stable                                   | ✅ | ✅ |

- **C · Modded-client local pack** (Passed 2026-06-15). Test history moved to [GROUP_05_SILENT_PACK.md](GROUP_05_SILENT_PACK.md).

---

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| B3 | B3 | SP only. Fast create/retexture burst can fire multiple reloads + `Corrupt PNG`. Not fixed by: debounce (reverted, broke MP), image-op counter, atomic loose-writer + write lock. MP unaffected. Full narrative in PROGRESS_LOG. | 🔴 open — known bug |

---

