# Phase 9 Testing Tutorial — Import/Export + Search + Templates

> **Prerequisite:** Phase 8 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify templates, export/import, notes, favorites, drafts, and lock/unlock
> all work correctly end-to-end.
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Templates | Attribute presets existed. Apply behavior similar. | Templates save: glow, hardness, sound, collision, category. **Not texture** (intentional). Apply is fully undoable via `/cb undo`. |
| Export bulk | `exportall` (one command, one format) | `/cb export json` and `/cb export txt` — timestamped files in `exports/` |
| Export single block | `exportblock <id>` | `/cb export <id> config` — saves `exports/<id>.json`. Importable by `importfolder`. |
| Import | `importblock <code>` (share code) | `/cb importfolder` — scans `exports/` folder for JSONs. No share-code import (Phase 17 Group H). |
| Search | Opened a GUI search picker | Text output only. Matches ID, display name, or category (no `SearchIndex.java` — plain string scan). |
| Notes | Similar | `/cb note <id> [text|clear]` — attach text notes to blocks. |
| Favorites | `favorite` + `unfavorite` (separate commands) | `/cb fav <id>` toggles. `/cb favs` lists. No dedicated `unfavorite` (Phase 17 Group N regression). |
| Drafts | Not in old CB | **New.** `/cb draft <id>` marks as WIP. `/cb publish <id>` publishes. `/cb drafts` lists. |
| Lock | Similar | `/cb lock`, `unlock`, `locked` — same function, cleaner implementation. |

---

## What Phase 9 covers

| Feature | Commands |
|---|---|
| Templates | `/cb template save <name> <id>`, `apply <name> <id>`, `list`, `delete <name>` |
| Bulk export | `/cb export json`, `/cb export txt` |
| Single export | `/cb export <id> config` |
| Import folder | `/cb importfolder` |
| Notes | `/cb note <id>`, `/cb note <id> <text>`, `/cb note <id> clear` |
| Favorites | `/cb fav <id>`, `/cb favs` |
| Drafts | `/cb draft <id>`, `/cb publish <id>`, `/cb drafts` |
| Lock | `/cb lock <id>`, `/cb unlock <id>`, `/cb locked` |

---

## Setup

```
/cb create p9a TemplateSource
/cb setglow p9a 8
/cb sethardness p9a 5
/cb setsound p9a metal
/cb setcollision p9a passable
/cb create p9b TemplateTarget
/cb create p9export ExportTest
```

---

## Test 9.1 — Template: save

```
/cb template save mytemplate p9a
```

**Expected:** `Saved "mytemplate" from p9a (glow:8 hard:5.0 metal passable)`

**Pass:** Template saved with correct attributes shown.
**Fail:** Error, or attributes wrong.

---

## Test 9.2 — Template: list

```
/cb template list
```

**Expected:** `1 template(s)` — shows `mytemplate` with its attributes + `[apply →]` and `[x]` buttons.

**Pass:** Template appears with attributes and clickable buttons.
**Fail:** "No templates", or buttons missing.

---

## Test 9.3 — Template: apply

```
/cb template apply mytemplate p9b
```

**Expected:** `Applied "mytemplate" → p9b (glow:8 hard:5.0 metal passable)`

Verify: `/cb list` or check p9b — it should now have glow 8, hardness 5, metal sound, passable collision.

**Pass:** All attributes stamped onto p9b correctly.
**Fail:** Applied but attributes wrong, or error.

---

## Test 9.4 — Template apply is undoable

```
/cb undo
```

**Expected:** `Undid template:mytemplate p9b` — p9b reverts to its pre-template attributes.

**Pass:** Undo reverses the template apply.
**Fail:** Nothing to undo, or wrong block affected.

---

## Test 9.5 — Template: delete

```
/cb template delete mytemplate
```

**Expected:** `Deleted template "mytemplate"`

Then `/cb template list` — "No templates yet."

**Pass:** Template deleted, list empty.
**Fail:** Error, or template still appears.

---

## Test 9.6 — Export all (JSON)

```
/cb export json
```

**Expected:** `Exported X block(s) → blocks-YYYYMMDD-HHMMSS.json`

Check `config/customblocks/exports/` — a timestamped `.json` file should exist.

**Pass:** File created, message shows count.
**Fail:** Error, or file not found.

---

## Test 9.7 — Export single block

```
/cb export p9export config
```

**Expected:** `Saved p9export → exports/p9export.json`

Check `config/customblocks/exports/p9export.json` exists.

**Pass:** Single-block JSON file created.
**Fail:** Error, or file missing.

---

## Test 9.8 — Import folder (round-trip)

1. Export `p9export`:
   ```
   /cb export p9export config
   ```
2. Delete it:
   ```
   /cb delete p9export
   ```
3. Import it back:
   ```
   /cb importfolder
   ```

**Expected:** `Imported 1 block(s): p9export`

Then `/cb list` — `p9export` is back with the same name.

**Pass:** Block round-trips through export → delete → import cleanly.
**Fail:** Import fails, or block comes back with wrong data.

---

## Test 9.9 — Note: set and show

```
/cb create p9note NoteTest
/cb note p9note This block is used for the entrance arch
```
**Expected:** `Note on p9note saved`

```
/cb note p9note
```
**Expected:** `Note on p9note: This block is used for the entrance arch`

**Pass:** Note saved and retrieved correctly.
**Fail:** Note not saved, or wrong text returned.

---

## Test 9.10 — Note: clear

```
/cb note p9note clear
```
**Expected:** `Note cleared from p9note`

Then `/cb note p9note` → "No note for p9note."

**Pass:** Note cleared.
**Fail:** Error, or note still shown after clear.

---

## Test 9.11 — Favorites: add and list

```
/cb fav p9a
```
**Expected:** `Added p9a to favorites ★`

```
/cb favs
```
**Expected:** `1 favorite(s):` — shows p9a with `[give]` and `[unfav]` buttons.

**Pass:** Favorite added and listed with clickable buttons.
**Fail:** Error, or p9a not in list.

---

## Test 9.12 — Favorites: toggle off

```
/cb fav p9a
```
**Expected:** `Removed p9a from favorites`

Then `/cb favs` — empty or p9a gone.

**Pass:** Toggle removes favorite.
**Fail:** Block stays in favorites after second fav.

---

## Test 9.13 — Drafts: mark and list

```
/cb draft p9b
```
**Expected:** `p9b marked as draft. Publish with /cb publish p9b`

```
/cb drafts
```
**Expected:** `1 draft(s):` — shows p9b with `[publish]` button.

`/cb list` — p9b shows `[draft]` tag next to its name.

**Pass:** Draft marked, listed, tag visible in `/cb list`.
**Fail:** Error, or draft tag missing.

---

## Test 9.14 — Drafts: publish

```
/cb publish p9b
```
**Expected:** `p9b published ✔`

Then `/cb drafts` — empty. `/cb list` — p9b has no `[draft]` tag.

**Pass:** Publish removes draft status.
**Fail:** Error, or tag still showing.

---

## Test 9.15 — Lock: blocks modification

```
/cb lock p9a
```
**Expected:** `Locked p9a — use /cb unlock p9a to edit it again`

Now try:
```
/cb setglow p9a 15
```
**Expected red error:** `'p9a' is locked — /cb unlock p9a first`

```
/cb locked
```
**Expected:** Lists p9a with `[unlock]` button.

**Pass:** Locked block rejects edits, shows in `/cb locked`.
**Fail:** Edit goes through despite lock, or locked list empty.

---

## Test 9.16 — Lock: unlock

```
/cb unlock p9a
```
**Expected:** `Unlocked p9a`

Now:
```
/cb setglow p9a 0
```
**Expected:** `Glow p9a → 0` — edit succeeds.

**Pass:** Unlock restores editability.
**Fail:** Edit still blocked after unlock.

---

## Phase 9 Verdict

| Test | Description | Result |
|---|---|---|
| 9.1 | Template save | ✅ |
| 9.2 | Template list with buttons | ✅ |
| 9.3 | Template apply stamps all attributes | ✅ |
| 9.4 | Template apply is undoable | ✅ |
| 9.5 | Template delete | ✅ |
| 9.6 | Export all to JSON | ✅ |
| 9.7 | Export single block | ✅ |
| 9.8 | Import folder round-trip | ✅ |
| 9.9 | Note set and show | ✅ |
| 9.10 | Note clear | ✅ |
| 9.11 | Favorites add and list | ✅ |
| 9.12 | Favorites toggle off | ✅ |
| 9.13 | Draft mark and list | ✅ |
| 9.14 | Draft publish | ✅ |
| 9.15 | Lock blocks modification | ✅ |
| 9.16 | Unlock restores editability | ✅ |

**Phase 9 passes — functionality verified. Known improvements deferred to Phase 17:**

| Item | Phase 17 Issue |
|---|---|
| `/cb note` — barebones, needs GUI wiring and richer UX | 17.12 |
| `/cb fav` should be alias of `favorite`, not replace it | 17.13 |
| Draft/publish — confusing, needs clearer messages + GUI | 17.14 |
| Entire export system — UX rework needed (chat output, formats, workflow) | 17.15 |

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p9a
/cb delete p9b
/cb delete p9note
```
(p9export cleaned up in test 9.8 already — imported back, delete if you want)
