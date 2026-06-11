# Phase 15 Testing Tutorial — Diagnostics + Polish

> **Prerequisite:** Phase 14 skipped (deferred to Phase 17). Creative world open, cheats ON.
>
> **Goal:** Verify `/cb diag` system snapshot and `/cb incidents` log work correctly.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Diagnostics | `/cb diag` existed — showed system state | `/cb diag` — slots, undo mode, texture size, HTTP, HUD, pack size, heap, TPS |
| Incident log | Similar | `IncidentRecorder.java` — appends errors to `incidents.json`, max 100 entries, atomic write |
| HistoryTracker | — | Not built |
| FeedbackHelper | — | Not built (`Chat.java` handles messages instead) |
| SpotBugs | — | Not in build.gradle |
| verifyVoiceCatalog | — | Not built |

---

## What Phase 15 covers (built)

| Feature | Command |
|---|---|
| System snapshot | `/cb diag` |
| Incident log | `/cb incidents` |
| Clear incident log | `/cb incidents clear` |

---

## Test 15.1 — Diagnostics snapshot

```
/cb diag
```

**Expected:** Multi-line output showing:
- `=== CustomBlocks Diagnostics ===`
- `Slots used: N / 800`
- `Undo mode: global` (or per_player)
- `Texture size: 64px`
- `HTTP: 127.0.0.1:8123`
- `HUD enabled: true/false`
- `Resource pack: X KB/MB`
- `Heap: X MB / Y MB`
- `TPS: 20.0`

**Pass:** All lines appear, no crash, values are plausible.
**Fail:** Error, crash, or blank output.

---

## Test 15.2 — Incident log (empty)

```
/cb incidents
```

**Expected:** `Incident log:` header with no entries (or entries from previous errors if any occurred).

**Pass:** Command runs, output shown.
**Fail:** Crash or error.

---

## Test 15.3 — Incidents clear

If any incidents are listed:

```
/cb incidents clear
```

**Expected:** `Incident log cleared.`

Then `/cb incidents` → empty log.

**Pass:** Clear works, log is empty after.
**Fail:** Error, or incidents still shown after clear.

---

## Phase 15 Verdict

| Test | Description | Result |
|---|---|---|
| 15.1 | `/cb diag` shows full system snapshot | ✅ |
| 15.2 | `/cb incidents` shows log | ✅ |
| 15.3 | `/cb incidents clear` empties log | ✅ |

**Phase 15 ✅ — works but both commands need rework. Deferred to Phase 17 (issue 17.24).**

> **Missing from Phase 15 spec:**
> - `HistoryTracker` — not built
> - `FeedbackHelper` — not built (Chat.java used instead)
> - SpotBugs static analysis — not in build.gradle
> - `verifyVoiceCatalog` Gradle task — not built

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure
