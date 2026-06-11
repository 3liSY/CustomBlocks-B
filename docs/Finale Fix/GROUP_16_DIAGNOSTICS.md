# Group 16 — Diagnostics, IT Chest & Admin Tools

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 15 (Diagnostics + Polish) build-verified.
>
> **Objective:** Rework `/cb diag` and `/cb incidents` into an interactive "IT Chest" Dashboard. Merge the DevConsoleScreen debug log into the IT Chest. Add the Server Mutation Log (history tracker) for admins. Add auto-fix for common incidents. Also covers: cache management, in-game screenshot, and per-category particles/sounds toggles.
>
> **Source issues:** 17.24 (/cb diag + /cb incidents rework), P2 (DevConsoleScreen merged into IT Chest — Issue 17.17), R1 (HistoryTracker + mutation log in IT Chest), Group M (audit, cache, screenshot), `/cb particles`, `/cb sounds`
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| `/cb diag` | Functional but output/layout needed improvement | Working text output | IT Chest GUI: live system health |
| `/cb incidents` | Functional but plain text list | Working text list | IT Chest GUI: Incident Log with colored wool items |
| DevConsoleScreen | Screen-based debug log viewer | Missing entirely (empty `client/gui/`) | Merged into IT Chest as a hover-readable log section |
| History tracker | `/cb history` — mutation log | Missing | Restored as IT Chest section (who edited what, when) |
| Auto-fix | Not present | Not present | New: clicking an incident attempts to auto-fix or opens editor |
| `/cb audit` | Listed server-side action audit | Missing | Restored, shown in IT Chest history section |
| IT Chest sections | N/A | N/A | Live Health (row 1), Incident Log (rows 2–4), Mutation Log (row 5), Controls (row 6) |

---

## What this group covers

| Feature | Commands |
|---|---|
| IT Chest dashboard | `/cb diag` (opens IT Chest GUI) |
| Incident log | `/cb incidents` (opens IT Chest, Incidents section) |
| Clear incidents | `/cb incidents clear` |
| Server audit log | `/cb audit` |
| History log | `/cb history` |
| Generate report | From IT Chest GUI controls |
| Cache status | `/cb cache` |
| Cache clear | `/cb cache clear` |
| Screenshot | `/cb screenshot` |
| Particles toggle | `/cb particles <category> <on\|off>` |
| Sounds toggle | `/cb sounds <category> <on\|off>` |
| Confirm/cancel | `/cb confirm` / `/cb cancel` |

---

## Implementation Requirements

### 1. IT Chest Layout (6-row chest GUI)

**Row 1 — Live System Health:**
| Slot | Item | Color meaning | Hover info |
|---|---|---|---|
| TPS | Glass pane | Green ≥18 TPS / Yellow 15–17 / Red <15 | Exact TPS value |
| Block Registry | Comparator | Green = healthy / Red = errors | Registered vs expected count |
| Network Sync | Redstone dust | Green = all clients synced / Red = sync lag | Client count, pack checksum |
| Pack Status | Book | Green = current / Yellow = rebuilding / Red = error | Pack size, SHA-1, last rebuild time |
| Memory | Barrel | Green <70% / Yellow 70–85% / Red >85% | Heap used/max in MB |

**Rows 2–4 — Incident Log:**
- Each slot = one incident entry.
- Red Wool = Error. Yellow Wool = Warning. Lime Wool = Info.
- Hover: timestamp, player, action, error detail.
- Click: "Auto-fix" (if possible) or "Open editor for this block".
- Most recent incidents first.

**Row 5 — Mutation Log (History):**
- Each slot = one server mutation event (block create, edit, delete, etc.).
- Hover: timestamp, player name, action, block ID.
- Click: opens the block editor for that block (if block still exists).

**Row 6 — Controls:**
| Slot | Action |
|---|---|
| Clear Incidents | Clears all incident entries |
| Generate Report | Writes `config/customblocks/data/diag_report.txt` and provides chat link |
| Refresh | Force-refreshes live health data |
| Debug Log | Opens debug log view (scrollable within same chest GUI) |
| Close | Closes IT Chest |

### 2. Auto-Fix

When clicking an incident slot:
- If the incident is a missing texture → auto-trigger re-download from the last known URL.
- If the incident is a broken block (missing `SlotData`) → offer "Restore from backup" or "Delete broken entry".
- If no auto-fix is available → open the relevant block editor.

### 3. Debug Log Section

The DevConsoleScreen content (mod debug log from `latest.log` filtered to `[CustomBlocks]` lines) is accessible from Row 6 Controls → "Debug Log". Replaces the need for a separate screen.

### 4. History / Mutation Log

Server mutation events written to `config/customblocks/data/` (in memory + periodic flush). Contains: timestamp, player UUID + name, action type, affected block ID, old value, new value.

`/cb audit` shows the same log with player-name filter: `/cb audit <player>`.

### 5. Report Generation

"Generate Report" writes a human-readable `.txt` file with:
- Server info (MC version, mod version, uptime).
- Full live health snapshot.
- All incidents (last 100).
- Last 50 mutation log entries.

---

### 6. Cache Management

`/cb cache` — shows: texture cache file count + size in MB, resource pack size + last built timestamp, pending rebuild queue count.

`/cb cache clear` — clears the in-memory download cache and temp files. Live texture PNG files in `config/customblocks/textures/` are **not** cleared.

### 7. Screenshot

`/cb screenshot` — saves a diagnostic image to `config/customblocks/screenshots/<timestamp>.png` (server-side). If server-side capture is impractical, triggers Minecraft F2 screenshot on the calling player's client.

### 8. Particles Toggle

`/cb particles <category> <on|off>` — enable/disable particle effects per event category.

Categories: `success`, `error`, `gui`, `selection`, `bulk_complete`, `rp_regenerate`, `achievement`

Config stores per-category: `particlesEnabled_<category>`.

### 9. Sounds Toggle

`/cb sounds <category> <on|off>` — enable/disable sound effects per event category. Same categories as particles. Config stores: `soundsEnabled_<category>`.

### 10. Confirm/Cancel

`/cb confirm` — confirms the most recent pending operation for the calling player.
`/cb cancel` — cancels it.

If no operation is pending: `"No pending operation to confirm/cancel."`

---

## Setup

Perform some actions to generate incidents and history entries:
```
/cb create g16a DiagTest
/cb setglow g16a 8
/cb retexture g16a https://definitely-broken-url.invalid/bad.png
```

The bad URL should generate an incident.

---

## Test G16.1 — IT Chest opens

```
/cb diag
```

**Expected:** Chest GUI opens (not text output). Row 1 shows health slots with colored glass panes.

**Pass:** Chest GUI opens with health row.
**Fail:** Text output only, or screen-based UI.

---

## Test G16.2 — Health slots show live data

Hover over the TPS slot.

**Expected:** Tooltip shows exact TPS (e.g., "TPS: 20.0"). Color-coded glass pane (green if healthy).

**Pass:** Hover shows exact TPS value.
**Fail:** No tooltip, or generic item name only.

---

## Test G16.3 — Incident appears in log

After the bad URL retexture from Setup:

In the IT Chest, look at Rows 2–4.

**Expected:** A Red Wool slot appears for the failed texture download. Hover shows: timestamp, player name, "retexture failed: g16a", error detail.

**Pass:** Incident recorded in log.
**Fail:** Log rows empty after the error.

---

## Test G16.4 — Auto-fix: re-download texture

Click the Red Wool incident slot from G16.3. Choose "Auto-fix" (or "Re-download texture" if that's what appears).

**Expected:** Mod attempts to re-download the texture. Since the URL is broken, a new error message appears. (The attempt itself is what matters — auto-fix should fire.)

**Pass:** Auto-fix click triggers the action (success or new error — either is acceptable).
**Fail:** Nothing happens on click.

---

## Test G16.5 — Mutation log shows actions

In the IT Chest, look at Row 5.

**Expected:** Slots show mutation events from Setup: create g16a, setglow g16a 8, retexture g16a attempt.

Hover a slot → shows timestamp, player, action, block ID.

**Pass:** Mutation log populated with events.
**Fail:** Row 5 empty.

---

## Test G16.6 — Clear incidents

In IT Chest Row 6, click "Clear Incidents".

**Expected:** Incident log rows (2–4) are cleared. Health row and mutation log unaffected.

**Pass:** Only incidents cleared.
**Fail:** Clears everything including mutation log, or nothing cleared.

---

## Test G16.7 — Generate report

In IT Chest Row 6, click "Generate Report".

**Expected:** `Report saved → config/customblocks/data/diag_report.txt` with `[download]` chat link.

Check file exists at `config/customblocks/data/diag_report.txt`. Content includes TPS, incidents, mutation log.

**Pass:** File created with full content.
**Fail:** Error or file missing.

---

## Test G16.8 — `/cb incidents` shortcut

```
/cb incidents
```

**Expected:** Opens the IT Chest GUI with focus on the Incidents section (rows 2–4 visible).

**Pass:** IT Chest opens correctly.
**Fail:** Text output only, or different command behavior.

---

## Test G16.9 — Audit command

```
/cb audit
```

**Expected:** IT Chest opens with mutation log filtered to all players. Or text output with mutation events.

```
/cb audit <yourname>
```

**Expected:** Shows only mutations made by you.

**Pass:** Audit shows correct filtered results.
**Fail:** Command missing or no filter.

---

---

## Test G16.10 — Cache status

```
/cb cache
```

**Expected:** Chat summary showing texture cache (N files, MB), resource pack size + last built timestamp, pending rebuild queue count.

**Pass:** Cache info displayed.
**Fail:** Command missing or no output.

---

## Test G16.11 — Cache clear

```
/cb cache clear
```

**Expected:** `Cleared CustomBlocks download cache.` Live texture PNG files still present — only temp/download cache cleared.

**Pass:** Clear confirmed, data files intact.
**Fail:** Command missing, or live textures deleted.

---

## Test G16.12 — Screenshot command

```
/cb screenshot
```

**Expected:** `Screenshot saved → config/customblocks/screenshots/<timestamp>.png` (or client screenshot trigger message).

**Pass:** File created or Minecraft screenshot triggered.
**Fail:** Command missing or error.

---

## Test G16.13 — Particles toggle off

```
/cb particles success off
/cb setglow g16b 5
```

(Create `g16b` first: `/cb create g16b ParticleTest`)

**Expected:** No success particles visible after the setglow. No errors in log.

**Pass:** Particles suppressed.
**Fail:** Particles still fire.

---

## Test G16.14 — Particles toggle back on

```
/cb particles success on
/cb setglow g16b 6
```

**Expected:** Success particles visible again.

**Pass:** Particles restored.
**Fail:** Still no particles.

---

## Test G16.15 — Sounds toggle off

```
/cb sounds gui off
```

Open `/cb` main GUI.

**Expected:** No click/GUI sound when navigating.

**Pass:** GUI sounds silenced.
**Fail:** Sounds still play.

---

## Test G16.16 — Confirm/cancel flow

Trigger a bulk operation requiring confirmation (need ≥10 blocks — use test blocks from Setup or Group 07):

```
/cb bulkproperty all glow 0
```

**Expected:** `This will affect N blocks. Type /cb confirm to proceed, or /cb cancel to abort.`

```
/cb cancel
```

**Expected:** `Pending operation cancelled.` No bulk operation fires.

**Pass:** Cancel works correctly.
**Fail:** Operation proceeds, or cancel missing.

---

## Test G16.17 — Confirm/cancel with no pending operation

```
/cb confirm
```

**Expected:** `No pending operation to confirm.`

**Pass:** Clean message, no error.
**Fail:** Crash or misleading message.

---

## Test G16.18 — Final build verify

```
.\gradlew.bat build
```

**Expected:** BUILD SUCCESSFUL. All 3 gates pass: verifyMojibake, verifySound, verifyFileSize.

**Pass:** Clean build.
**Fail:** Any gate failure. Fix before marking Phase 17 complete.

---

## Group 16 Verdict

| Test | Description | Result |
|---|---|---|
| G16.1 | IT Chest opens as chest GUI | ⬜ |
| G16.2 | Health slots show live data on hover | ⬜ |
| G16.3 | Incident appears in log | ⬜ |
| G16.4 | Auto-fix fires on incident click | ⬜ |
| G16.5 | Mutation log shows server actions | ⬜ |
| G16.6 | Clear incidents only clears incidents | ⬜ |
| G16.7 | Generate report creates text file | ⬜ |
| G16.8 | `/cb incidents` opens IT Chest | ⬜ |
| G16.9 | Audit shows filtered mutation log | ⬜ |
| G16.10 | Cache status displays info | ⬜ |
| G16.11 | Cache clear works, data files safe | ⬜ |
| G16.12 | Screenshot command works | ⬜ |
| G16.13 | Particles toggle off | ⬜ |
| G16.14 | Particles toggle back on | ⬜ |
| G16.15 | Sounds toggle off | ⬜ |
| G16.16 | Confirm/cancel flow works | ⬜ |
| G16.17 | Confirm/cancel with no pending op | ⬜ |
| G16.18 | Final build succeeds — all gates pass | ⬜ |

**Group 16 passes when the IT Chest Dashboard, cache tools, screenshot, particles/sounds toggles, and confirm/cancel all work in-game.**

> **The Golden Rule:** Nothing is ✅ DONE until the developer runs it in-game and confirms it works. A clean build alone is not done.

If anything shows ❌ — paste:
1. The exact action taken
2. What appeared vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g16a
/cb delete g16b
/cb incidents clear
```
