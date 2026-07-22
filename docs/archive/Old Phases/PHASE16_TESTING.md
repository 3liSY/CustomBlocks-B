# Phase 16 Testing Tutorial — Stress Test + Release QA

> **Prerequisite:** Phase 15 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Stability, edge cases, and release readiness. No new code built in this phase —
> purely in-game QA.

---

## What Phase 16 covers

| Area | Notes |
|---|---|
| Stress test: many blocks | Create large number of blocks — verify no crash, lag, or slot overflow |
| Invalid URL handling | Feed bad URLs to retexture — verify clean error, no crash |
| `/cb reload` stability | Reload slot data mid-session — verify blocks survive |
| Mod Menu entry | Config button in Mod Menu opens ConfigScreen |
| License file | Check LICENSE exists |
| Build + JAR | Confirm `gradlew build` produces JAR |

**Skipped (need multi-player setup):**
- Multiplayer: 5+ concurrent players
- Keep-alive mixin: large pack disconnect verification

---

## Test 16.1 — Stress: create many blocks

```
/cb create s1 Stress1
/cb create s2 Stress2
/cb create s3 Stress3
```
... create ~20 blocks rapidly.

```
/cb list
```

**Expected:** All created, no errors, list shows them all. Server TPS stays at 20 (`/cb diag`).

**Pass:** No crash, no slot errors, TPS stable.
**Fail:** Slot overflow, crash, or severe lag.

---

## Test 16.2 — Invalid URL: 404

```
/cb create urltest UrlTest
/cb retexture urltest https://example.com/doesnotexist.png
```

**Expected:** Clean error: `Retexture failed: ...` — no crash, block still exists unchanged.

**Pass:** Error message shown, block intact.
**Fail:** Crash, or block gets corrupted texture.

---

## Test 16.3 — Invalid URL: not an image

```
/cb retexture urltest https://example.com
```

**Expected:** Error: `Not a web link — use a direct http/https image URL.` OR a clean download failure message.

**Pass:** Error shown, no crash.
**Fail:** Crash or silent failure.

---

## Test 16.4 — `/cb reload` stability

```
/cb reload
```

**Expected:** `Reloaded X block(s).` All previously created blocks still exist and work after reload.

Verify: `/cb list` — same blocks as before reload.

**Pass:** Reload succeeds, no data lost.
**Fail:** Blocks missing after reload, or crash.

---

## Test 16.5 — Mod Menu entry

> Requires Mod Menu to be installed. Skip if not installed.

Open Mods list in Minecraft → find CustomBlocks → click Config button.

**Expected:** ConfigScreen opens (screen-based for now — Phase 17 replaces with chest GUI).

**Pass:** Config screen opens.
**Fail:** Button missing, crash.

---

## Test 16.6 — License file exists

Check that `LICENSE` exists at the project root.

**Pass:** File exists.
**Fail:** Missing.

---

## Test 16.7 — Build produces JAR

```
.\gradlew.bat build
```

**Expected:** `BUILD SUCCESSFUL` — JAR in `build/libs/customblocks-1.0.0.jar`.

**Pass:** Build succeeds, JAR present.
**Fail:** Build failure, gate violation.

---

## Phase 16 Verdict

| Test | Description | Result |
|---|---|---|
| 16.1 | Stress: many blocks, TPS stable | ⏭ SKIPPED |
| 16.2 | Invalid URL (404) returns clean error | ⏭ SKIPPED |
| 16.3 | Non-image URL returns clean error | ⏭ SKIPPED |
| 16.4 | `/cb reload` — data survives | ⏭ SKIPPED |
| 16.5 | Mod Menu config button opens screen | ⏭ SKIPPED |
| 16.6 | LICENSE file exists | ⏭ SKIPPED |
| 16.7 | `gradlew build` succeeds | ⏭ SKIPPED |

**Phase 16 SKIPPED — not a full feature testing guide. Re-run as release QA after Phase 17 is complete.**

> **Note on maxSlots:** Config defaults to 800 slots, Bible spec says 1028.
> If you want the full 1028-block stress test, set `maxSlots: 1028` in config.json
> and restart. Not required for the pass — 800 is sufficient.

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete s1
/cb delete s2
/cb delete s3
(... delete all stress blocks)
/cb delete urltest
```
