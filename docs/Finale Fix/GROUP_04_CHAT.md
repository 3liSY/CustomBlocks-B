# Group 04 — Chat Messages & Command Communication

> **Prerequisite:** Group 02 (Chest GUI) verified in-game.
>
> **Objective:** Replace all terse/prefixed chat messages with human-readable, conversational output. Remove the `CB ` prefix style. Ensure error messages include plain-language explanations. Add DidYouMean typo correction and a structured `/cb help` chest GUI.
>
> **Source issues:** 17.2 (chat messages too terse), Q5 (DidYouMean + /cb help GUI), Group I decision (voice modes scrapped → single polished professional tone)
>
> **Rules:** Work through each test in order. Pay attention to message tone — "terse" is a failure even if functionally correct.

---

## Hotbar popups un-branded (2026-06-20, build-green)

`Chat.tool(...)` writes the **action-bar / hotbar** tool popups. As of 2026-06-20 it **no longer prepends
the `[CB]` tag** — the developer wanted a cleaner hotbar (`Chat.java`, the `tool` method only). The `[CB]`
brand stays on **chat** lines: `success` / `error` / `info` / `line` are unchanged, and the `PREFIX`
constant is still used by them. So: chat = branded, hotbar = clean. Cross-cutting; the most visible
beneficiary is the Square colour-swap line (Group 06 §J / Group 13 §16), reworded to
`Swapped to <DisplayName>`. Tests → `GROUP_06_TESTING_GUIDE.md` §J.

---

## Chat-polish backlog (open items)

Small wording issues noticed during testing. Not bugs (the commands work) — message clarity only.

- **`/cb rename` fake-succeeds when the new name equals the current one.**
  `/cb rename <id> <name>` always prints `Renamed "<id>" to "<name>".`
  ([CreationCommands.java:228](../../src/main/java/com/customblocks/command/handlers/CreationCommands.java))
  — there is **no no-op check**. Running `/cb rename testing testing` twice both report success
  (`Renamed "testing" to "testing".`) even though nothing changed the second time. **Wanted:** when
  the new name already equals the block's current display name, give a friendly "already named that"
  message instead of a fake success — exactly like `/cb reid` already does for ids
  (`"x" is already its id — nothing to change.`). Fix idea: after the lock check, if
  `name.equals(before.displayName())` → `Chat.info(src, "\"" + id + "\" is already named \"" + name
  + "\" — nothing to change.")` and return without recording an undo step.
  *(Reported by developer, screenshot 2026-06-13. Polish only — leave the command working.)*

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Message prefix | `CB  Downloading texture…`, `CB  Gave 1 ✓` | Similar terse CB-prefix style | **KEPT on chat as `[CB]`** (developer wants it); removed only from hotbar/action-bar popups. Wording made conversational |
| Success messages | Friendly, complete sentences | Short abbreviations | Full, friendly success sentences |
| Error messages | Plain English explanations | Terse one-liners | Clear explanation + what to do next |
| Major errors | Chat only | Chat only | Chat (for the triggering player) + Incidents log (for admins) |
| DidYouMean | Typo suggestions for wrong commands | Missing | Restored — configurable: smart / always / off |
| `/cb help` | Opened a structured command list GUI | Flooded chat with text | Chest GUI command list |
| Voice modes | 6 selectable tones | N/A | Scrapped entirely — single polished professional tone |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Success messages | All `/cb` command responses |
| Error messages | All `/cb` command errors |
| Major error routing | Chat + Incidents log |
| DidYouMean | Typo correction on unknown subcommands |
| `/cb help` | Chest GUI command browser |
| `/cb welcome` | Welcome message / quick-start |
| Config | `didYouMean` field (smart / always / off) |

---

## Implementation Requirements

### 1. Message Tone Policy

All chat output from CustomBlocks follows one tone: **professional and conversational**. No robotic prefixes. No voice mode toggle.

Examples of correct style:

| Action | Old (terse) | New (correct) |
|---|---|---|
| Create block | `CB  Created 'myblock'` | `Block "myblock" created successfully.` |
| Texture downloading | `CB  Downloading texture…` | `Downloading texture for "myblock"…` |
| Give item | `CB  Gave 1 ✓` | `Gave you 1 × MyBlock.` |
| Lock error | `'myblock' is locked` | `"myblock" is locked. Use /cb unlock myblock to edit it.` |
| Bad URL | `CB  Invalid URL` | `That URL doesn't point to an image. Check the link and try again.` |

### 2. Major Error Routing

When a major system error occurs (texture download failure, pack generation error, import failure, etc.):
- Send a clear explanation to the player who triggered the action.
- Simultaneously write the error to the Incidents log (Group 16) with full context.

This ensures server admins can review errors they didn't see in real time.

### 3. DidYouMean

When a player types an unknown `/cb` subcommand:
- Compute the closest matching known subcommand (Levenshtein distance or similar).
- If a close match is found, suggest it in chat.
- Config field: `didYouMean` — `smart` (default, only suggest when confident), `always`, `off`.

Example:
```
/cb cretae myblock
→ Unknown command "cretae". Did you mean: /cb create?
```

### 4. `/cb help` — Chest GUI

`/cb help` opens a chest GUI with paginated command reference:
- Organized by category: Block Management, Textures, Tools, Bulk, Export, etc.
- Each slot = one command group. Hover for description. Click to pre-fill command in chat.
- No more chat flood.

### 5. `/cb welcome`

`/cb welcome` shows a short welcome/quick-start message with clickable links to the most common first actions (create a block, open the GUI, etc.).

---

## Setup

```
/cb create g04a ChatTest
```

---

## Test G04.1 — Success message style (create)

```
/cb create g04b StyleTest
```

> **CORRECTED 2026-06-21 (developer):** the branded `[CB]` chat prefix is **wanted** and is already
> present — it is NOT a failure. Chat lines stay branded; only the hotbar/action-bar popups are
> un-branded (see top note). The test is about TONE (friendly full sentence), not prefix removal.

**Expected:** A friendly, complete-sentence confirmation carrying the `[CB]` chat prefix. Something like:
`[CB] Block "g04b" ("StyleTest") created.`

**Pass:** Message is a conversational full sentence (with the `[CB]` chat prefix).
**Fail:** Terse abbreviation like `Created g04b` / `Gave 1 ✓` — clipped, robotic wording.

---

## Test G04.2 — Error message style (locked block)

```
/cb lock g04a
/cb setglow g04a 15
```

**Expected:** Error message like:
`"g04a" is locked. Use /cb unlock g04a to edit it.`

**Pass:** Error is a complete English sentence with a suggested action.
**Fail:** Terse one-liner like `'g04a' is locked`.

---

## Test G04.3 — Error message style (bad URL)

```
/cb retexture g04a https://definitely-not-a-valid-image-url.xyz/fake.abc
```

**Expected:** Clear explanation like:
`Could not download a texture from that URL. The link may be broken or doesn't point to an image.`

**Pass:** Human-readable explanation. No raw exception message.
**Fail:** Raw exception, terse code, or no message at all.

---

## Test G04.4 — Major error goes to Incidents log

After G04.3's failed retexture:

Open the Diagnostics/IT Chest (Group 16) and check the Incidents log.

**Expected:** The failed retexture attempt appears as an incident entry with timestamp, player name, action, and error detail.

**Pass:** Incident recorded in log.
**Fail:** Log is empty after the error.

---

## Test G04.5 — DidYouMean triggers on typo

```
/cb cretae g04a
```

**Expected:** Something like:
`Unknown command "cretae". Did you mean: /cb create?`

**Pass:** Suggestion appears in chat.
**Fail:** Generic "unknown command" with no suggestion.

---

## Test G04.6 — DidYouMean doesn't fire on garbage input

```
/cb xyzzy
```

**Expected:** No suggestion (too different from any real command). Message:
`Unknown command. Try /cb help for a list of commands.`

**Pass:** No spurious suggestion.
**Fail:** Suggests a completely unrelated command.

---

## Test G04.7 — DidYouMean can be disabled

Set `didYouMean = off` in config (or via Config chest GUI, Group 21).

```
/cb cretae g04a
```

**Expected:** No suggestion — just the unknown command message.

Restore `didYouMean = smart` after this test.

**Pass:** No suggestion when disabled.
**Fail:** Suggestion still appears despite config being off.

---

## Test G04.8 — `/cb help` opens chest GUI

```
/cb help
```

**Expected:** A chest GUI opens. Commands are organized by category. Each slot has a hover tooltip with description. No chat flood.

**Pass:** Chest GUI opens with organized command list.
**Fail:** Chat flooded with text, or command missing.

---

## Test G04.9 — `/cb help` slot click pre-fills chat

In the `/cb help` chest GUI, click any command slot (e.g., "create").

**Expected:** Chest GUI closes and the command is pre-filled in the chat input bar (e.g., `/cb create `).

**Pass:** Chat bar pre-filled with correct command prefix.
**Fail:** Nothing happens on click, or wrong command pre-filled.

---

## Test G04.10 — `/cb welcome`

```
/cb welcome
```

**Expected:** A short, friendly welcome message with clickable links for: "Create your first block", "Open the dashboard", "Browse help".

**Pass:** Message appears with working clickable links.
**Fail:** Command missing, or plain text with no clickable links.

---

## Group 04 Verdict

| Test | Description | Result |
|---|---|---|
| G04.1 | Create message is conversational (branded `[CB]` chat prefix is CORRECT) | ✅ in-game (2026-06-21) |
| G04.2 | Lock error has complete sentence + suggestion | ✅ in-game (2026-06-21) |
| G04.3 | Bad URL error is human-readable | ✅ in-game (2026-06-21) |
| G04.4 | Major error logged to Incidents | ✅ in-game (2026-06-21) |
| G04.5 | DidYouMean fires on close typo | ✅ in-game (2026-06-21) |
| G04.6 | DidYouMean silent on garbage input | ✅ in-game (2026-06-21) |
| G04.7 | DidYouMean respects config off | ✅ in-game (2026-06-21) |
| G04.8 | `/cb help` opens chest GUI | ✅ in-game (2026-06-21) — wants full rework to a better design (see Follow-ups) |
| G04.9 | Help slot click pre-fills chat | ✅ in-game (2026-06-21) — tied to G04.8 rework |
| G04.10 | `/cb welcome` shows clickable links | ✅ in-game (2026-06-21) — revamp to a screen/GUI later (see Follow-ups) |

**Group 04 passes when the developer confirms all messages are conversational and all help/correction features work in-game.**

If anything shows ❌ — paste:
1. The exact command typed
2. The exact message received
3. What was expected instead

---

## Follow-ups (from in-game test 2026-06-21)

All 10 tests pass. Open polish/rework wanted (group functions, but not "final"):

- **G04.8 / G04.9 — `/cb help` full rework.** Chest help GUI works but developer wants it completely
  redesigned to a better layout. G04.9 (slot click pre-fills chat) is locked in with this rework.
- **G04.10 — `/cb welcome` → screen/GUI.** Works as clickable chat links; developer wants it revamped
  into a screen or GUI. Design discussed later.
- **G04.1 spec corrected** — branded `[CB]` chat prefix is wanted (was wrongly written as "remove
  prefix"). Fixed above.
- **`/cb rename x x` no-op** — fake-success, still open (chat-polish backlog above).

> **Ownership note (G04.4 vs G16):** G04 owns *routing* a major error INTO the incidents log
> (the chat group's job). G16 owns *viewing* incidents (the IT Chest dashboard + `/cb incidents`).
> G04.4 stays here as a "did it get logged?" check; the viewer test lives in G16.

## Cleanup

```
/cb unlock g04a
/cb delete g04a
/cb delete g04b
```
