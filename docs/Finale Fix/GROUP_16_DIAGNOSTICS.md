# Group 16 — Diagnostics, IT Chest & Admin Tools

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 15 (Diagnostics + Polish) build-verified.
>
> **Objective:** Rework `/cb diag` + `/cb incidents` into an interactive **IT Chest Dashboard** —
> live system health, an incident log, the mutation/history log, and admin controls in one 6-row
> chest GUI. Then layer on auto-fix, admin commands, and per-category particle/sound toggles.
>
> **Source issues:** 17.24 (`/cb diag` + `/cb incidents` rework), P2 (DevConsoleScreen merged into
> the IT Chest — Issue 17.17), R1 (HistoryTracker + mutation log), Group M (audit, cache, screenshot),
> `/cb particles`, `/cb sounds`.

> **Status & test steps live in** `Reports/GROUP_16_TESTING_GUIDE.md`. This spec stays status-free.

---

## Reality check — what already exists (read 2026-06-21)

Before designing, the live code was surveyed. Several spec assumptions were **stale**:

| Area | Spec assumed | Actual state in code |
|---|---|---|
| Mutation / history log | "Missing" | ✅ **Built** — `MutationLog` (who/what/when/blockId) + 6-row paginated `HistoryMenu` (click → editor). Fed centrally by `UndoManager.record`. |
| `/cb confirm` / `/cb cancel` | "New" | ✅ **Built** — `BulkConfirm` (one pending per actor, 60s expiry). Spec wording differs slightly; trivial. |
| `/cb diag` GUI | text → GUI | ⚠️ **Partial** — opens a 3-row `DiagMenu` (slot/history/server gauges + report lore). Not the 6-row dashboard. |
| `/cb incidents` | text → GUI | ⚠️ **Text only** — `IncidentRecorder` stores **time + context + error string only**. No player, no blockId, no severity. |
| Incident recording coverage | — | ✅ Already wired at **~22 sites** (texture download/create/retexture/recolour/backup/pack-rebuild/face/import/trash…). The bad-URL case already fires an incident. |
| Particle effects | togglable | ❌ **The mod emits zero particles anywhere.** Nothing to toggle yet. |
| Server-side screenshot | save PNG | ❌ Impractical on a headless dedicated server (no framebuffer). |

---

## Decisions (2026-06-21, with the owner)

- **Screenshot — DROPPED.** Server-side render is impractical; not worth a client-F2 detour for this group.
- **Particles — add the effects first, then the toggle.** A small particle-FX set is built, *then*
  `/cb particles <category> on|off` controls it. (Own slice, after the dashboard.)
- **Debug Log viewer (DevConsoleScreen merge) — deferred** to a later slice (don't forget it).
- **Auto-fix — its own well-structured slice** (slice 2), not folded into the dashboard.
- **Old 3-row `DiagMenu` — replaced** by the new 6-row IT Chest (one dashboard).
- **Structured incidents — pulled forward into slice 1** so the dashboard's incident row is real from
  day one (player + blockId + severity), wiring **all ~22 record sites**. Only the *auto-fix actions*
  (re-download / restore-from-backup) stay in slice 2.
- **Health row — all 5 gauges**, best-effort (Network Sync = online count + pack SHA, no per-client lag).
- **Severity colour — auto-derived** from the error (throwable / "fail|error" → red, "skip|warn" →
  yellow, else lime). No per-call-site level argument.
- **`/cb incidents`** opens the dashboard (incidents view); `/cb incidents clear` still clears.

---

## Ownership corrections (sweep 2026-06-21)

| Command/feature | Ruling | Source doc |
|---|---|---|
| `particles` / `sounds` / `feedback` | **G16 owns** — merged into the Feedback FX system (slices 4–5). Removed the stale G19 (particles) / G25 (sounds) claims. | SWEEP_INDEX §B |
| `showbrokenblocks` | **G16 owns** (diagnostics) — decision B 2026-06-21. **Moved out of G09**; G09 keeps a cross-ref. Already built (`BrokenBlockScanner` + `BrokenBlocksMenu`, `/cb showbrokenblocks` → `Dest.BROKEN_LIST`); **test spec added** to the G16 testing guide §7 (2026-06-21). | G09 |
| `screenshot` | **DROPPED here.** If ever revived it belongs to **G23** (player experience), not diagnostics. | G23 |
| `incidents` | **G16 owns** the viewer; G04 only *routes* errors into it. | SWEEP_INDEX §A |
| `history` / mutation log | G16 *displays* it in the IT Chest; the block-edit-history **feature** is owned by **G25** (shared surface). | G25 |

---

## Slice plan (build → owner tests in-game → next)

| Slice | Scope | State |
|---|---|---|
| **1 — IT Chest dashboard** | 6-row GUI (health / incidents / mutation / controls) + structured incident model (all ~22 sites). Replaces `DiagMenu`. | ⏳ next |
| **2 — Structured auto-fix** | Click a failed-texture incident → re-download from last URL; broken-block → restore-from-backup / delete; else open editor. | ⏳ |
| **3 — Admin commands** | `/cb audit [player]`, `/cb cache` + `cache clear`, Generate Report (+ Row-6 button). | ⏳ |
| **4 — Particles** | Particle-FX set + `/cb particles <category> on\|off` + Particle FX board. | ✅ verified in-game 2026-06-21 |
| **5 — Feedback FX (merged particle + sound)** | Add the per-category **sound** layer and **merge it with particles** into one Feedback system. Each category keeps **2 flags** (FX + sound); a master toggle flips both, each still settable alone. Expand/collapse chest board. `/cb feedback` / `particles` / `sounds <cat> on\|off`. | ✅ R1+R2 verified in-game 2026-06-21 (R3 centring await) |
| **Later** | Debug Log viewer (latest.log `[CustomBlocks]` filter) folded into Row-6 controls. | 🟢 build-green, await — `DebugLog` + `DebugLogMenu`, IT-Chest Row 6 slot 50 → `Dest.DEBUG_LOG` |
| ✅ already done | mutation log, `/cb confirm` / `/cb cancel` (verify wording in-game). | — |
| ❌ dropped | server-side / client screenshot. | — |

---

## Slice 1 — IT Chest Dashboard (locked design)

**6-row chest GUI, title "IT Chest". `Nav.Dest.DIAG` routes here. `/cb diag` and `/cb incidents` both open it.**

### Row 1 — Live System Health (all 5)
| Slot | Item | Colour meaning | Hover |
|---|---|---|---|
| TPS | Glass pane | Green ≥18 / Yellow 15–17 / Red <15 | Exact TPS |
| Block Registry | Comparator | Green healthy / Red errors | Used vs max slots |
| Network Sync | Redstone dust | Green synced / Yellow approx | Online players + pack SHA *(best-effort, no per-client lag)* |
| Pack Status | Book | Green current / Yellow rebuilding / Red error | Pack size, SHA-1, last rebuild |
| Memory | Barrel | Green <70% / Yellow 70–85% / Red >85% | Heap used/max MB |

### Rows 2–4 — Incident Log (27 slots)
- One wool per incident; **colour auto-derived** from severity (red error / yellow warn / lime info).
- Newest first. Hover: timestamp, player, action/context, error detail.
- Click: open that block's editor if the blockId resolves to an existing block; else show full detail in chat.
- *(Auto-fix actions come in slice 2.)*

### Row 5 — Mutation Log
- Last 9 entries from `MutationLog.recent()` (reuse `HistoryMenu` rendering).
- Hover: timestamp, player, action, blockId. Click → editor if the block still exists.

### Row 6 — Controls (lean for slice 1)
| Slot | Action |
|---|---|
| Refresh | Force-refresh live health (repage in place) |
| Clear Incidents | Clears incident rows only (health + mutation untouched) |
| Back | Back to the previous menu |
| Close | Close the chest |

*(Generate Report + Debug Log buttons arrive with slices 3 / later.)*

### Data model — structured incidents
`IncidentRecorder` gains **severity + blockId + player**; `incidents.json` schema extended; old
entries degrade gracefully (missing fields → "—"). All ~22 `record(...)` call sites updated to pass
the block id + actor where in scope; core services with no player log as **"System"**. Severity is
derived, not passed. Last 100 kept (unchanged), atomic write (unchanged).

---

## Later-slice requirements (reference)

### Slice 2 — Auto-fix
- Missing texture → re-download from the last known URL.
- Broken block (missing `SlotData`) → "Restore from backup" or "Delete broken entry".
- No auto-fix available → open the relevant block editor.

### Slice 3 — Admin commands
- **`/cb audit [player]`** — the mutation log, optionally filtered to one player.
- **`/cb cache`** — texture cache file count + size, resource-pack size + last-built, pending rebuild queue.
- **`/cb cache clear`** — clears in-memory download cache + temp files. Live texture PNGs in
  `config/customblocks/textures/` are **not** cleared.
- **Generate Report** — writes `config/customblocks/data/diag_report.txt` (server info, full health
  snapshot, last 100 incidents, last 50 mutation entries) + a `[download]` chat link.

### Slice 4 — Particles
- Add a particle-FX set first, then `/cb particles <category> on|off`.
- Categories: `success`, `error`, `gui`, `selection`, `bulk_complete`, `rp_regenerate`, `achievement`.
- Config: `particlesEnabled_<category>`.

### Slice 5 — Feedback FX (merged particle + sound) — **locked design 2026-06-21**

**Decision (owner):** don't ship particles and sounds as two separate systems. **Merge** them into
one per-category "Feedback FX". Default behaviour = one switch flips both; but each category stays
**splittable** so you can run particle-only or sound-only.

**Data model — two flags per category, both persisted:**
- Keep `particlesEnabled_<category>` (slice 4, unchanged).
- Add `soundsEnabled_<category>` (new, default all ON), mirrored in `CustomBlocksConfigStore`.
- Helpers: `soundsOn(category)` next to the existing `particlesOn(category)`.
- The "master" toggle is a convenience that writes **both** flags — it is not a third flag.

**Sound engine — `core/SoundFx`** (parallel to `core/ParticleFx`):
- `play(p, category)` (gated by `soundsOn`) / `preview(p, category)` (ignores toggle, for the board).
- One sound per category. Note-block sounds **must** use `.value()` (NFR-12 / `verifySound` gate).
- Provisional palette: success = XP-orb pickup · error = note-block bass (low) · gui = amethyst chime
  · selection = amethyst chime (lower) · bulk_complete = beacon activate · rp_regenerate = amethyst
  chime (soft) · achievement = UI toast / firework.

**Firing — same hubs as particles, now both channels:**
- `Chat.success` / `Chat.error` → also fire `SoundFx.play(p, "success"/"error")`.
- `GuiFx.click` / `GuiFx.select` → route the chime through `SoundFx.play(p, "gui"/"selection")` so the
  per-category sound toggle actually gates it (removes the current hard-coded, un-gated chime).
- The other `GuiFx` cues (open / apply / danger / deny) are **not** categories — left as-is.

**Board — `ParticlesMenu` → `FeedbackMenu` (expand/collapse, one chest):**
- 7 category **master tiles**, collapsed by default. Tile lore shows both child states (FX ● / Snd ●).
- **Left-click master** = toggle BOTH (the merge). **Right-click** = preview BOTH (particle + sound).
- **Shift-click** = expand that category → two sub-tiles (FX on/off, Sound on/off) for independent
  control; shift-click again collapses. Only one category expanded at a time (transient per-player).
- Reached by `/cb particles`, `/cb sounds`, `/cb feedback`, and IT-Chest Row 6 — all the same board.
- **Layout (R3 polish, owner feedback 2026-06-21):** keep the 6-row chest, but the masters are
  **vertically centred**. Master row = row 2 (slots 19-25, cols 1-7); expansion sub-tiles ride rows
  3-4 (`+9` / `+18`); header row 0, footer (Back/Close) row 5. (Master can't go lower — row 3 would
  push the sub-tiles into the footer row.) Horizontal centring unchanged (col 0/8 stay empty).

**Commands:**
- `/cb feedback <category> on|off` = set BOTH flags. `/cb particles <category> on|off` = FX flag only.
  `/cb sounds <category> on|off` = sound flag only. All three tab-complete category + on/off.
- Console (no player): each prints its category state list (sound list / particle list / both).

**Build in two owner-test rounds:** R1 = sound layer fires + merged + `/cb sounds` toggle (board
unchanged). R2 = expand/collapse `FeedbackMenu` + `/cb feedback` master + aliases open the board.

> **Deferred-FX wiring (R4, 2026-06-21):**
> - **`bulk_complete`** → wired. Fires at the end of every bulk op (property / delete / rename), at the
>   `Chat.line` summary site — those use `Chat.line` (no FX), so no double burst.
> - **`rp_regenerate`** → wired to **manual pack reloads only** (owner call): `/cb sync` and `/cb rp
>   resume`. Both used `Chat.success` (generic success FX); switched to new `Chat.successFx(…,
>   "rp_regenerate")` so they fire the pack FX, not a doubled success. Auto-debounced rebuilds stay
>   silent (they'd spam on every create/delete). `/cb reload` is a *data* reload (`SlotManager.reload`),
>   not a pack regen — left on generic success.
> - **`achievement`** → **intentionally preview-only.** No achievement/milestone system exists yet;
>   owner will wire it when that system is built. Effect remains testable via board right-click preview.

---

## Already built — verify only

- **Mutation log:** `MutationLog` + `HistoryMenu`. Slice 1 reuses it for Row 5.
- **Confirm/cancel:** `BulkConfirm` — `/cb confirm` runs the held bulk op, `/cb cancel` discards it,
  "Nothing to confirm/cancel." when none pending. Verify wording in-game; tweak to spec text if wanted.

---

## Dropped

- **Screenshot** (`/cb screenshot`) — server-side render impractical; client-F2 detour not worth it here.
