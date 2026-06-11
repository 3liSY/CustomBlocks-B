# Phase 13 Testing Tutorial — Macros + Video + AI

> **Prerequisite:** Phase 12 partial pass confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify macro record/replay and video frame extraction work. AI features are
> explicit stubs — nothing to test there.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Macros | Macro system existed. Record/replay sequences of /cb commands. | Same: `MacroManager.java` + `MacroCommands.java`. GUI via `/cb gui macros` (MacroListScreen). |
| Video | Video frame extraction existed | `/cb video extract <file> <id> <frame>` — place MP4 in `config/customblocks/videos/`. jcodec pure-Java decoder. |
| AI command parser | Not in old CB | Stub — `AiCommandParser.java` returns null. No `/cb ai` command wired up. Phase 17 scope. |
| AI texture generator | Not in old CB | Stub — `AiTextureGenerator.java` returns null. No command wired up. Phase 17 scope. |

---

## What Phase 13 covers (built)

| Feature | Commands |
|---|---|
| Macro record | `/cb macro record <name>` |
| Macro add step | `/cb macro add <cmd>` |
| Macro stop | `/cb macro stop` |
| Macro cancel | `/cb macro cancel` |
| Macro play | `/cb macro play <name>` |
| Macro list | `/cb macro list` |
| Macro delete | `/cb macro delete <name>` |
| Video list | `/cb video` or `/cb video list` |
| Video extract | `/cb video extract <filename> <id> <frame>` |

**Not testable (stubs):**
- AI command parser — no command registered, returns null
- AI texture generator — no command registered, returns null

---

## Setup

```
/cb create p13a MacroTestA
/cb create p13b MacroTestB
```

---

## Test 13.1 — Macro: record and add steps

```
/cb macro record myglow
```

**Expected:** `Recording macro 'myglow'. Add steps with /cb macro add <cmd>. Finish with /cb macro stop.`

```
/cb macro add cb setglow p13a 15
/cb macro add cb setglow p13b 8
```

**Expected:** Each `Added: /cb setglow ...` confirmation.

**Pass:** Recording started, steps added.
**Fail:** Error on record, or "not recording" on add.

---

## Test 13.2 — Macro: stop and persist

```
/cb macro stop
```

**Expected:** `Macro 'myglow' saved. Play with /cb macro play myglow`

Check `config/customblocks/macros/myglow.json` exists.

**Pass:** Stop saves the macro.
**Fail:** "Not recording anything", or file not created.

---

## Test 13.3 — Macro: list with buttons

```
/cb macro list
```

**Expected:** `1 macro(s):` — shows `myglow` with `[play]` and `[delete]` buttons.

**Pass:** Macro listed with clickable buttons.
**Fail:** "No macros saved."

---

## Test 13.4 — Macro: play executes steps

First reset the glow so we can see the change:

```
/cb setglow p13a 0
/cb setglow p13b 0
```

Then:

```
/cb macro play myglow
```

**Expected:**
```
Playing 'myglow' (2 step(s))...
Done: 2/2 succeeded.
```

Verify: `/cb list` — p13a has glow 15, p13b has glow 8.

**Pass:** Both blocks updated to their correct glow values.
**Fail:** "No macro named", errors on steps, or glow unchanged.

---

## Test 13.5 — Macro: cancel (discard recording)

```
/cb macro record throwaway
/cb macro add cb setglow p13a 5
/cb macro cancel
```

**Expected:** `Recording cancelled.`

Check `/cb macro list` — `throwaway` does NOT appear.

**Pass:** Cancel discards without saving.
**Fail:** `throwaway` appears in list.

---

## Test 13.6 — Macro: delete

```
/cb macro delete myglow
```

**Expected:** `Deleted macro 'myglow'.`

Then `/cb macro list` — empty.

**Pass:** Macro deleted.
**Fail:** "No macro named", or macro still shows.

---

## Test 13.7 — Video: list (no files)

```
/cb video list
```

**Expected:**
```
No .mp4 files found in config/customblocks/videos/
Place MP4 files there, then use: /cb video extract <file> <id> <frame>
```

**Pass:** Command works, helpful message shown.
**Fail:** Error or crash.

---

## Test 13.8 — Video: extract (requires MP4)

> **Skip if you don't have an MP4 handy.** Place any `.mp4` at
> `config/customblocks/videos/test.mp4` to run this test.

```
/cb video list
```

**Expected:** `test` listed with file size.

```
/cb video extract test p13a 0
```

**Expected:** `Extracting frame 0 from test…` → then `Frame 0 applied to p13a — accept the pack prompt`

**Pass:** Frame extracted and applied as texture to p13a.
**Fail:** "File not found", jcodec error, or texture not applied.

---

## Phase 13 Verdict

| Test | Description | Result |
|---|---|---|
| 13.1 | Macro record + add steps | ✅ |
| 13.2 | Macro stop + persist to file | ✅ |
| 13.3 | Macro list with [play]/[delete] buttons | ✅ |
| 13.4 | Macro play executes steps correctly | ✅ |
| 13.5 | Macro cancel discards without saving | ✅ |
| 13.6 | Macro delete | ✅ |
| 13.7 | Video list (no files — message shown) | ✅ |
| 13.8 | Video extract frame from MP4 | ⬜ (untested — no MP4 available) |

**Phase 13 ✅ — confirmed in-game. Deferred to Phase 17:**

| Item | Phase 17 ref |
|---|---|
| Macro list/management screen → needs chest GUI | 17.3 |
| Video browse/extract → needs chest GUI workflow | 17.3 |
| AI command parser — stub, needs full API integration | 17.22 |
| AI texture generator — stub, needs full API integration | 17.22 |

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p13a
/cb delete p13b
```
