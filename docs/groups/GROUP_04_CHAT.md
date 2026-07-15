# Group 04 — Messages & Command Communication

> 🔒 **2026-07-15 — scope widened: this group owns the HOTBAR too, and the chat chips are settled**
>
> The group used to own chat only. It now owns **everything the mod says to you**, on either surface, because
> the hotbar has exactly the disease chat had (no voice, no colour contract, no routing rule, no gate) and
> splitting "what the mod says" across two groups would mean building the same machinery twice.
>
> **§F · The routing rule (owner-locked):** the **hotbar** is what you did with your **hands** — tool swings,
> right-clicks, mode switches, eyedrop picks, area selects. **Chat** is what you asked for with a **command**.
> Never both. Nothing else decides which surface a message lands on.
>
> **§F · The colour contract:** the call site passes **meaning**, never colour. `Chat.toolSuccess(p, "Glow set",
> "12")` → the label renders green, the value renders **yellow**. Green = worked, red = failed, **light gray `§7`
> = neutral** (owner-locked 2026-07-15, was white — F3; note this is `§7`, NOT the CbFmt 'gray'→`§8` map), yellow =
> the value. The error-line tail (`— /cb unlock x`) is **`§7` too** now, not white (owner-locked 2026-07-15 — it
> shared the old white neutral constant). Four colours, decided
> **once**, inside `Chat`. A `§` code inside a hotbar body is a bug —
> that is exactly how `ColorToolService` ended up hand-picking green/red and `RainbowRectangleItem` went gold.
>
> **§F · Voice:** verb first, ≤6 words, no `[CB]`, no ✔/✖, no trailing period, ids always quoted.
> `Glow set 12` · `Locked "x"` · `Can't edit "x" — /cb unlock x` · `Nothing selected`
>
> **§F · Anti-spam:** one `HotbarSpeaker` per player holds the last text + tick. An identical line inside
> **20 ticks (1.0s)** is swallowed; a *different* line always gets through instantly. This is what stops a held
> tool strobing. Dwell = **vanilla's own hotbar fade** (~5s), not a reimplementation.
>
> **§F · `hotbarRouteGate` (owner-locked 2026-07-15):** HARD build-fail, **hotbar-only** — does NOT touch
> client HUD/overlay rendering. `gradlew build` fails on a raw `sendMessage(…, true)` outside `Chat`, or any `§`
> inside a hotbar body. Server routing is **already clean** (only 1 raw `sendMessage(…,true)` exists, inside
> `Chat`); the real migration is **stripping embedded `§`/`CbFmt` colour from all 61 hotbar call-sites (17 files)**
> — `ColorToolService` (16), `ColorVariantService` (14), `BuzzerGameWand` (10), etc. — and passing (label, value)
> meaning instead so the gate lands green.
>
> **Folded in:** the Group 03 active-tool HUD chip is **deleted** (owner: the Omni-Tool doesn't earn a permanent
> widget). The tool still announces its mode — on the hotbar, once, on switch. That message is §F's problem now.

> 🔒 **2026-07-15 — chat chips, final shape (owner-locked) · §G undo dialect PASSED MP 2026-07-15, folded to history**
>
> - **Only two undo affordances exist in the whole mod: `↩ Undo` and `↪ Redo`.** Both run the real command
>   (`/cb undo` / `/cb redo`). Every other undo-flavoured chip is **wiped**: `BulkFlagCommands`' `▶ [undo]` (it
>   ran the *inverse command*, not the undo stack) and the `[↩ Undo]` label passed to `runButton`, which prepends
>   its own ▶ and rendered `▶ [↩ Undo]`. That double-glyph was **9 call sites across 6 files**, not the 2 first
>   spotted — all nine now call `Chat.undoButton()`.
> - **Lock/unlock/favorite record into `UndoManager`** — bulk as one batch, single as one step. They recorded
>   nothing before, so `/cb undo` silently skipped them and undid whatever came *before* (the trap the
>   2026-07-15 MP run walked into). This needed a new **`Kind.FLAG`**: lock lives in `LockManager` and favorites
>   in `FavoritesManager`, neither is a field on `SlotData`, so the snapshot-restore path every other Kind uses
>   cannot reach them. The op carries `(id, lock|favorite, on, ownerUuid)` — the owner matters because favorites
>   are per-player, and under `undoMode=global` the clicker is not necessarily the favoriter.
> - **The redo line carries `↩ Undo`**, so the round trip loops forever instead of dead-ending.
> - **`⊙ View` is deleted entirely** — it opened `/cb editor`, the chest menu already rejected for Edit. `⊙` now
>   means **Details**, and only Details.
> - **Ids are always quoted** in chat. A block named `-` printed bare and vanished into the sentence.
> - **The `(N left)` counter names its stack in English:** `Undid delete "x" · 1 more to undo`. At zero it prints
>   **nothing** — `(0 left)` read like a failure code on an operation that had just succeeded.

> ⚠️ **2026-07-14 REDISCUSS — read-only chat REVERSED, chat is interactive again**
>
> The 2026-07-12 "chat is strictly read-only" lock is **reopened and replaced**. Owner's current position:
> 1. **Chat is interactive again:** clickable chat stays. Aqua kept as the clickable colour. `⧉ copy` is **scrapped**; `▶ run` and `✎ edit` survive, joined by new action chips. Full spec → §G04-4.
> 2. **Template Feature Extinct:** the `/cb template` command and the entire preset system is deleted. Unchanged.
> 3. **Welcome & Help Extinct in G04:** `/cb welcome` and `/cb help` are no longer chat commands. Fully transferred to **Group 27 (Screens)** and **Group 23 (Onboarding)**. Unchanged.
> 4. **DidYouMean Typo Correction:** remains a "Suggestion", never silently auto-executes. Unchanged.
> 5. **Notifications (Toasts):** still out of Group 04's scope — lives in **Group 27** as the "Gravity Physics / Modern Toast Stack". Unchanged.
> 6. **Error codes:** the short `E-45` code is **kept AND joined by a clickable link** — both appear (§G04-4).
>
> **Group 04 owns the chat/command surface: plain-text system-log routing PLUS the interactive chat layer.**
> Screens, toasts, and the wiki remain in Group 27.

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
  ([CreationCommands.java:228](../src/main/java/com/customblocks/command/handlers/CreationCommands.java))
  — there is **no no-op check**. Running `/cb rename testing testing` twice both report success
  (`Renamed "testing" to "testing".`) even though nothing changed the second time. **Wanted:** when
  the new name already equals the block's current display name, give a friendly "already named that"
  message instead of a fake success — exactly like `/cb reid` already does for ids
  (`"x" is already its id — nothing to change.`). Fix idea: after the lock check, if
  `name.equals(before.displayName())` → `Chat.info(src, "\"" + id + "\" is already named \"" + name
  + "\" — nothing to change.")` and return without recording an undo step.
  *(Reported by developer, screenshot 2026-06-13. Polish only — leave the command working.)*

---

## 🧊 QoL cross-link — Vault chat output → GUI later

`/cb vault codes` + the cloud share commands (Group 20) output to **chat** today. A chest/screen GUI
front-end (the **Vault Hub**, GROUP_20 §G / S7 client screen) is a deferred QoL — no work scheduled,
note only. Tracked across four surfaces: **GROUP_20 §G** ↔ **GROUP_04** (here, chat) ↔ **GROUP_27**
(screens) ↔ **GROUP_02** (chest GUI).

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Message prefix | `CB  Downloading texture…`, `CB  Gave 1 ✓` | Similar terse CB-prefix style | **KEPT on chat as `[CB]`** (developer wants it); removed only from hotbar/action-bar popups. Wording made conversational |
| Success messages | Friendly, complete sentences | Short abbreviations | Full, friendly success sentences |
| Error messages | Plain English explanations | Terse one-liners | Clear explanation + what to do next |
| Major errors | Chat only | Chat only | Chat (for the triggering player) + Incidents log (for admins) |
| DidYouMean | Typo suggestions for wrong commands | Missing | Restored — configurable: smart / always / off |
| Voice modes | 6 selectable tones | N/A | Scrapped entirely — single polished professional tone |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Success messages | All `/cb` command responses |
| Error messages | All `/cb` command errors |
| Major error routing | Chat + Incidents log |
| DidYouMean | Typo correction on unknown subcommands |
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

### 4. Deprecated Features (Moved to G27 / G23)

- `/cb help` is replaced by the 3D Tome Wiki (Group 27 §G27.32).
- `/cb welcome` is replaced by the Cinematic Welcome Video (Group 27 / Group 23).
- `/cb template` is permanently deleted.

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
| G04.8-10 | Help / Welcome features | ➡️ transfrerred to G27 / G23 / Deleted |

**Group 04 passes when the developer confirms all messages are conversational and all help/correction features work in-game.**

If anything shows ❌ — paste:
1. The exact command typed
2. The exact message received
3. What was expected instead

---

## Follow-ups (from in-game test 2026-06-21)

All 10 tests pass. Open polish/rework wanted (group functions, but not "final"):

- **Legacy Help/Welcome/Template features** completely removed/redirected as part of the 2026-07-12 architecture shift.
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

---

## G04-2 · "Did you mean" / command smartness is broken — revamp the whole suggest + help layer

> 🔍 diagnosed — **fix before G25-1**; enables discoverability for every new command added in other clusters

> *"need to revamp and fix the smartness entirely"* — screenshot: typing `/cb anim` →
> *"I don't know 'anim' — did you mean [/cb gui]? Click it to run."*

**Symptom** — Type a slightly-off or old subcommand and the "did you mean" engine suggests **nonsense**.
`/cb anim` → suggests `/cb gui` (no relation). The *correct* answer (`/cb animation`) is never offered,
and `animation` doesn't even appear in `/cb help`. The whole typo-correction + help layer feels dumb.

**Context** — The owner **hard-renamed `/cb anim` → `/cb animation` on 2026-06-22** (`AnimCommands.java:51`
comment), so typing `anim` from habit now misses. That's expected — the real bug is that the smartness
**should** have caught it instantly (`anim` is a literal **prefix** of `animation`, and the matcher even
has a prefix-boost) and didn't.

**Why — the suggestion dictionary is hand-maintained and drifts from the real commands (PRIMARY root).**
`DidYouMean.pickBest` matches the typed word against `HelpTopics.firstTokens()` — a set built **only** from
the hand-written `HelpTopics.CATEGORIES` list (`HelpTopics.java:137`), NOT from the actually-registered
Brigadier command tree. **`animation` was never added to `HelpTopics`**, so:

- it is **not a candidate** → DidYouMean can't suggest it → it picks the closest *other* word instead
  (`gui`, a short word that wins the length tie-break), producing the garbage suggestion;
- it is **missing from `/cb help`** too (same list feeds both) — the command is invisible to users;
- **any** real subcommand someone forgets to hand-add is silently absent from both systems. The class
  header claims "help and typo correction can never drift apart" — true of each other, but **both drift
  from the real command tree**, which is the actual source of truth.

**Secondary root — the fuzzy fallback invents bad matches.** When nothing is genuinely close,
`pickBest` still returns the lowest-scoring candidate within the distance cap (`score = eff*1000 +
c.length()` favors **short** words → `gui`). In `always` mode the cap is distance ≤ 3, wide enough for
unrelated words. So instead of an honest "I don't know," it offers a confident wrong answer.

### Developer's decisions (locked via UI 2026-06-23)

- **Suggest the right command, never garbage** — `anim` must propose `animation`.
- **Never drift again** — suggestions **and** `/cb help` should be built from the **real registered
  commands**, so a missing entry can't happen.
- **Better `/cb help`** — list every real command (`animation` etc. currently absent).
- **Behavior = suggest with click, NOT auto-run** — show "that's wrong, did you mean …?" as a clickable
  line; **keep the full tail/subcommands** in the suggestion (e.g. `/cb anim x ticks 5` →
  `[/cb animation x ticks 5]`). No silent auto-execution.
- **"+ much more brainstorming"** — treat the below as a starting set, not the final scope.

### Fix direction (brainstorm — NOT built)

1. **Feed DidYouMean from the real command tree, not `HelpTopics`.** After `dispatcher.register(root)`
   (`CommandRegistrar.java:123`) the root `LiteralCommandNode` exposes `.getChildren()` — every literal
   child name is an authoritative subcommand. Capture that set (literals only; skip the `subcommand`
   greedy-arg node) and hand it to DidYouMean. Drift becomes structurally impossible; `animation` (and
   every future command) is auto-included → `/cb anim` prefix-matches `animation` with top confidence.
2. **Only ever suggest a *confident* match; otherwise say so honestly.** Prefer prefix/substring hits;
   for fuzzy, gate on **normalized** distance (edits ÷ word length), not an absolute ≤3 that lets tiny
   words like `gui` slip in; drop the blind shortest-word tie-break. No good match → the plain "I don't
   know — try /cb help" (don't manufacture `gui`). Re-examine what `always` mode should still allow.
3. **Keep click-to-run + preserve the tail** — `handleUnknown` already keeps the remainder
   (`DidYouMean.java:54,71`), so once `best` is correct the suggestion is already `/cb animation <tail>`.
   Just confirm it survives the candidate-source swap.
4. **Make `/cb help` complete** — derive (or audit) the help list against the real tree so every
   registered literal is listed. Cheapest robust version: a small build/test gate that **fails if any
   registered subcommand has no help topic**, so the list can't silently fall behind again.
5. **Open (the "+ much more"):** second-level typo help (`/cb category renme` → `rename`); `/cb ` bare →
   show all subcommands.
6. **Suggestion-only aliases — LOCKED IN (owner-locked 2026-07-15):** hard-renamed old names (`anim`) resolve
   as a **clickable suggestion** to the new command (`/cb animation <tail>`), never auto-run, never reviving the
   removed command. Build for every hard-renamed command.

| Fact | Detail |
|---|---|
| DidYouMean candidate source | `HelpTopics.firstTokens()` — hand-list, NOT the real tree (`DidYouMean.java:87`, `HelpTopics.java:137`) |
| `animation` in help/dictionary? | **No** — no topic anywhere in `HelpTopics` → not a candidate, not in `/cb help` |
| Real literal | `animation` (hard-renamed from `anim` 2026-06-22) — `AnimCommands.java:53` + comment `:51` |
| Default mode | `smart` (dist ≤1, ≤2 for ≥5 chars, or prefix) — `CustomBlocksConfig.java:70`, `DidYouMean.java:96` |
| Garbage-match cause | far match still returned; `score = eff*1000 + length` favors short words (`gui`); `always` allows dist ≤3 (`DidYouMean.java:96-101`) |
| Tail preserved already | yes — remainder kept in the suggestion (`DidYouMean.java:54,71`) |
| Authoritative list exists | root `LiteralCommandNode.getChildren()` after `dispatcher.register` (`CommandRegistrar.java:123`) |

| Group | Scope | Status |
|---|---|---|
| G04 — Chat | Both — command parsing, identical SP & MP | 🔍 diagnosed — root = dictionary drift + weak confidence; direction chosen (derive from real tree, suggest-with-click, complete help) |

**Related:** `DidYouMean` (the matcher) · `HelpTopics` (drifting hand-list feeding both help + DYM) ·
`CommandRegistrar` (owns the real registered tree) · CLAUDE.md pitfall "DidYouMean arg shown verbatim"
(already handled — arg is named `subcommand`) · G04-1 (chat/feedback unification — same G04 area)
**Touches:** `DidYouMean` (candidate source + confidence gate) · `CommandRegistrar` (expose registered
literal children to DidYouMean) · `HelpTopics` (complete the list / derive it) · `CustomBlocksConfig.didYouMean`
(mode semantics) · a build/test audit gate (every registered literal has a help topic)
**Verify in-game:** after the fix, `/cb anim` → suggests `/cb animation` (with any tail kept); a genuinely
unknown word → honest "I don't know," never a random command.

---

## G04-3 · Chat message unification — full sweep, style spec locked, build gates planned

> 🔍 diagnosed + designed — spec locked via UI 2026-07-08; **NOT built**; scope = chat wording/routing only
> (G04-2 DidYouMean rewire and `CbActionBar` restyle stay separate, deferred sessions — developer's call)

> *"chat messages are too scattered, some have wrong cb prefix, some are too jargony, some have grey text,
> some are too tight and include information in a bad way and unreadable, i want to unify them all"*

**Symptom** — a full code sweep of every `Chat.*` call site and every command handler found 7 distinct,
concrete classes of drift (on top of the already-diagnosed G04-2 DidYouMean bug above). Every example below
is a real string pulled from the current code, reviewed with the developer via a rendered before/after
artifact on 2026-07-08.

1. **Unbranded lines masquerading as normal output.** ~15 handler files hand-build
   `Chat.PREFIX + "..."` and call `sendFeedback`/`sendMessage` directly instead of `Chat.line()`, so the
   same command family answers with inconsistent branding line-to-line.
   - `AchievementCommands.java:47-48` — branded header, then an unbranded grey body line right under it.
   - `DiagnosticsCommands.java:106` vs `:137` — branded header, unbranded "No matching entries."
   - `ColorImageCommands.java:194` — branded success line, unbranded file path right under it.
   - Also present in: `CategoryCommands.java` (:186, :240-243, :277-280, :336-340), `HistoryCommands.java`
     (:143-147, :179-183), `ConfigCommands.java:366-373`, `ManagementCommands.java` (:83,:120,:123),
     `UtilityCommands.java` (six sites), `MacroCommands.java`,
     `ArabicCommands.java:162`, `BackupCommands.java` (:106-111, :230), `FeedbackCommands.java`,
     `ParticleCommands.java`, `SoundCommands.java`.
2. **"Locked" error worded 4 different ways.**
   - Dominant (14 sites): `"g04a" is locked. Use /cb unlock g04a to edit it.` — e.g.
     `CreationCommands.java:246`.
   - Variant: `'g04a' is locked — /cb unlock g04a first` — `TemplateCommands.java:114`, and
     independently `SlotBlock.java:199` (hotbar) drifted to the same non-canonical wording.
   - Different meaning, different voice: `'g04a' is already locked` — `ManagementCommands.java:67`
     (this is a toggle-state message, not an edit-block error — stays separate).
3. **Grey (`§7`/`§8`) used with no rule.**
   - Whole unbranded line: `AchievementCommands.java:48`.
   - Several facts crammed into one colour-switching line: `GuessCommands.java` `describe()` (used by
     lines 130-266) — state + flag + full id list + label default, all in one string.
   - Hand-built help block, raw colour codes: `BackupCommands.java:106-111`.
5. **Raw Java exceptions leak to chat — breaks the group's own G04.3 test spec**
   ("no raw exception message. Fail: raw exception, terse code, or no message at all."). ~14 sites append
   `e.getMessage()` (or `e.toString()` when null) straight after a friendly sentence:
   `CreationCommands.java:225-230` (`UnknownHostException` after the bad-URL message),
   `CloudCommands.java:147,193` (`"Share failed: " + ex.getMessage()`, `"Download failed: " +
   ex.getMessage()`), `DiagnosticsCommands.java:194` (`"Couldn't write the report: " + e.getMessage()`),
   plus `BackupCommands.java`, `ColorImageCommands.java` (:163,:204,:274), `StudioReskin.java:86-90`,
   `SafetyCommands.java:133-136`, `FaceCommands.java:132-136`, `CategoryCommands.java` (:284,:319).
6. **Hotbar (`Chat.tool()`) has no colour contract.** `Chat.tool` (`Chat.java:35-37`) applies flat white
   and nothing else — every caller invents its own colour/glyph. `ColorToolService.java` uses green/red
   with no glyph; `RainbowRectangleItem.java:66` uses gold, a colour that exists nowhere else in the
   whole mod's palette.
7. **Quote style split project-wide.** Double quotes `"x"` dominant (~40+ sites); single quotes `'x'` in
   `TemplateCommands.java`, `MacroCommands.java`, `ManagementCommands.java`, `ColorToolService.java`,
   `SlotBlock.java`. No rule, just habit per file.

### Developer's decisions (locked via UI 2026-07-08)

- **Quotes** — double quotes `"x"` around every id/name, everywhere. Already the dominant convention;
  least churn.
- **Locked-block error, canonical wording, chat AND hotbar:**
  `"x" is locked. Use /cb unlock x to edit it.` The hotbar version drops `[CB]` and the `✖` glyph but
  keeps the exact same sentence. The "already locked / not locked" toggle-state message
  (`ManagementCommands`) is a different situation and keeps its own separate wording.
- **Grey text** — reserved for a genuinely secondary sub-detail (file path, count) riding on a branded,
  full-colour line. Never the whole message, never several facts crammed together, never un-branded/solo.
- **Exceptions** — player always sees plain English, written per failure type. Never `e.getMessage()` /
  `e.toString()`, never a class name. No "this was logged" note added to the message — the Incidents
  log (G04.4, already built) captures the full detail silently and automatically regardless; saying so
  in chat too is redundant. Simplicity wins.
- **Hotbar colour** — green = success, red = error, white = everything else. No new colours (kills the
  gold outlier). Hotbar stays unbranded — no `[CB]`, no `✔`/`✖` glyph — only chat lines get those.
- **Routing rule** — every player-facing response goes through the shared `Chat` helper
  (`success`/`error`/`info`/`line`/`tool`). No handler hand-builds `Chat.PREFIX + "..."` or calls
  `sendFeedback`/`sendMessage` directly.

### Prevention — stop this from recurring in any future group

- **Build gate `chatRouteGate`** (same family as the existing `mojibakeShield` / `soundGate` /
  `monolithGate` gates): fails the build if any command handler calls Minecraft's raw message-send
  functions instead of going through `Chat`.
- **Build gate: no ad-hoc colour codes** outside `Chat.java`/its button helpers: fails the build if a
  handler hardcodes a `§`-colour.
- This G04-3 spec becomes the reference every future group's chat output is checked against. Once the
  gates exist, new commands in *any* group can't drift even before anyone reviews them by hand.

### Scope for this pass

- **In scope:** all 7 issues above — wording, routing, buttons, grey-text rule, exception messages,
  hotbar colour — plus the two build gates.
- **Out of scope, deferred to their own later sessions (developer's call, 2026-07-08):** G04-2 DidYouMean
  suggestion-dictionary rewire (own bug, own files, diagnosed above); `CbActionBar` GUI colour restyle
  (client-side screens, not chat — separate palette system, see G04-1).

| Group | Scope | Status |
|---|---|---|
| G04 — Chat | Both (SP + MP) — server content + routing; hotbar client-render unaffected (same `Chat.tool` call shape, just a colour rule) | 🔍 diagnosed, spec locked via UI 2026-07-08 — **not built**. Next: implement, then TG retest. |

**Related:** G04-1 (unify chat + hotbar + `CbActionBar` — this pass fully implements the chat-side half) ·
G04-2 (DidYouMean — deferred) · G04.1-G04.10 (existing tests — some will need reworded expected-output
strings to match the new canonical wording) · G04.4 (Incidents log — exception fix routes through it, no
change needed there) · chat-polish backlog above (`/cb rename` no-op — still separate, still open)
**Touches:** `Chat.java` (button helpers get colour+icon+template baked in; `tool()` gets a colour
parameter or typed variants) · every handler file listed under issues 1/3/5/7 above (~20+ files) ·
`build.gradle` (two new verify gates)
**Verify in-game (when built):** every response reads as
one consistent voice; no unbranded lines; locked-error wording matches everywhere including hotbar;
no raw exception text ever appears; hotbar uses only green/red/white; a
deliberately-broken handler (temp test) fails the build via the new gates.

---

## G04-1 · Unify + upgrade all action bars and chat

> 🔍 designed — absorbs G06-1 timing slice; one style spec covers chat + hotbar + `CbActionBar`

> *"all action bars and chat must be unified and upgraded, must be discussed later"*

**Symptom** — Not a bug. A wanted holistic pass: make every piece of feedback (chat lines +
hotbar action-bar popups) feel like one consistent, polished system instead of pieces evolved
separately over time.

**Where it stands today** (already in G04, piecemeal):

- ✅ Chat tone reworked terse → conversational, tested (G04.1–G04.10 pass).
- ✅ Hotbar popups un-branded for a cleaner hotbar (`Chat.tool` dropped the `[CB]` tag, 2026-06-20).
- 🟡 Still open in G04: `/cb help` rework, `/cb welcome` → GUI, `/cb rename x x` no-op polish.

**What's missing** — No single deliberate sweep that unifies + upgrades the *whole* feedback
layer at once. That's this item.

**Three feedback surfaces today (mapped in code):**

| Surface | What | Where | Side |
|---|---|---|---|
| **Chat lines** | branded `[CB]` line + body + glyph (✔/✖) | `Chat.success`/`error`/`info`/`line` (+ rich `runButton`/`copyButton`/`suggestButton`/`hover`) | server |
| **Hotbar popup** | unbranded one-liner over the hotbar | `Chat.tool` → `sendMessage(text, **true**)` | server sends; **vanilla `InGameHud` renders** timing/fade |
| **In-screen button bar** | dockable button strip inside GUIs (G27) | `CbActionBar` (own colours: `GOLD`/`BAR_BG`/`PRIMARY`) | client |

So `Chat` is *almost* the single routing point already — every command goes through it; it owns voice,
colour, glyph, and (on success/error) fires `ParticleFx`+`SoundFx`. The gaps: hotbar **timing/animation**
live in the vanilla action-bar render (client), and `CbActionBar` was styled on its own.

### Developer's decisions (locked via UI 2026-06-24)

- **One shared upgraded helper** — ALL feedback (chat lines + hotbar popups) routes through one place so
  voice / colours / glyphs / sound are defined once; change the style once → everywhere updates.
- **Upgrade = everything + more** — consistent colours + glyphs · better popup **timing** · **animation**
  (fade/slide-in) · **sound cues** per message type · *and keep brainstorming beyond this list (open bucket).*
- **Scope = one big consistency pass** — unify chat + hotbar **and fold in G06-1** (action-bar timing)
  **and restyle `CbActionBar`** to match the same palette/voice. The widest of the three options.

### Brainstorm (architecture — NOT built)

1. **One style spec, two render sides (honest constraint).** A single literal class can't own both: message
   *content* (voice/colour/glyph/sound) is **server-side** (`Chat`), but hotbar *presentation*
   (timing/fade/animation) is **client-side** (the vanilla action-bar overlay). So "one helper" = **one
   `Chat` for content + one client render rule for presentation, both following one shared style spec**
   (colour constants, glyph set, durations). Keep the spec in one referenced place so neither side drifts.
2. **Message *type* as the unit.** Promote the implicit types (success / error / info / tool / line) into a
   small enum or typed methods that each carry their colour + glyph + sound + default duration. Today these
   are scattered string literals (`§a✔`, `§c✖`); centralise them so a restyle is one edit.
3. **Timing + animation (folds in G06-1).** The vanilla hotbar overlay has a fixed fade. Custom timing /
   fade / slide-in needs a **client mixin on the action-bar render** (the existing `HudRenderMixin` is the
   natural home — only 3 client mixins exist, keep it lean). Server sends the text + a duration hint;
   client renders it with the chosen timing/animation. This is the G06-1 piece living here.
4. **Sound cues.** `SoundFx` already fires on success/error server-side — extend to info/tool (and make it
   part of the message type from #2) so every feedback type has a consistent, optional cue.
5. **`CbActionBar` restyle.** Repoint its private colour constants (`GOLD`/`BAR_BG`/`BTN_BG`/`PRIMARY`) at
   the shared palette so the in-screen bar matches chat/hotbar. Pure styling — no behaviour change.
6. **Incremental delivery.** Each slice ships alone: (a) centralise the style spec + message types; (b)
   colour/glyph consistency; (c) sound cues; (d) timing/animation client mixin (G06-1); (e) `CbActionBar`
   palette. Land (a)–(c) first (server-only, cheap), then the client render slices.

### Locked 2026-07-12: The "Ultimate Premium" Notification System

> **Owner confirmed via UI: "everything but more and cooler".**
> *Note: This entire feature set has migrated to Group 27 (Screens). See `GROUP_27_SCREENS.md` §G27.32.*

- **The Modern Toast Stack:** Routine notifications slide in from the top-right corner like modern OS toasts with glassmorphism backgrounds. Multiple notifications stack downwards dynamically.
- **Cinematic Centered Messages:** High-priority alerts stay centered above the hotbar but get smooth cinematic slide-up animations, particle effects, and glowing borders.
- **Micro-Animations:** Success toasts feature a green checkmark that physically pops and scales; errors cause the toast box to gently shake left and right (error buzz).
- **Overload Shader & Audio:** If 15+ events fire at once, the system aggregates them into an "OVERLOAD" mega-toast, a subtle red vignette shader borders the screen for 1 second, and a single, unique bass-boosted "OVERLOAD" sound effect plays to prevent ear-destroying audio spam.
- **Interactive Physics:** Toasts fall off the screen with gravity when they expire. Players can physically flick their mouse cursor at the toasts to "swat" them off the screen prematurely.

| Group | Scope | Status |
|---|---|---|
| G04 — Chat / G27 — Screens | Both (SP + MP) — server content + client render | 💬 discuss later — direction locked (one style spec, everything + G06-1 + CbActionBar restyle); animation/duration taste + open bucket remain; slice incrementally |

---

## G04-4 · Command surface redesign — Interactive Chat (rediscussed)

> 🔍 diagnosed + completely designed — first locked 2026-07-12 (read-only), **reopened and re-locked via UI
> 2026-07-14 (interactive)**; **NOT built**.

The 2026-07-12 read-only decision was reversed by the owner on 2026-07-14: *"full rediscuss but not nuke it
all"*. Chat keeps its clickable layer. What follows supersedes the read-only spec entirely.
**Deep-architecture pass locked 2026-07-14 (second session)** after reading the real code — see the mechanism
section, it changes how chips work and cancels the token-store/TTL plan.

### The clickable button set (locked 2026-07-14)

**Colour:** aqua stays — it's the vanilla convention players already read as "clickable". `Chat.java` already
builds aqua `▶ ⧉ ✎` buttons; this pass repoints them, it does not invent a new style.

| Chip | Action | Notes |
|---|---|---|
| **▶ Run** | `RUN_COMMAND` — executes / re-runs the command on that line | existing `runButton`, kept as-is |
| **✎ Edit** | `RUN_COMMAND` of `/cb edit <id>` → says **"the editor screen is coming later"**, opens nothing | **MERGED (locked 2026-07-14, TG4-MP pass)** — Edit and View were two chips doing the same job, causing confusion. Now ONE chip. It has **no screen to open**: the chest menu was rejected as bad, and the button-grid `BlockEditorScreen` it actually opened was worse (8 of its 11 buttons closed the screen and pre-filled the chat box — the exact chat/screen split G04-4 exists to kill). Both rejected 2026-07-14; the chip answers honestly until a replacement is designed. ⚠️ An earlier revision of this table said the chip ran `/cb editor` (the chest menu) — it never did; that was a doc error |
| **Undo** | `RUN_COMMAND` of the existing `/cb undo` — **no new undo logic**, same stack, same scope | new chip, existing command |
| **↪ Redo** | `RUN_COMMAND` of the existing `/cb redo` — rides an **undo's own result line**, so the round trip is one click each way | new chip 2026-07-14 (TG4-MP A4), existing command |
| ~~View~~ | **MERGED into Edit** — no longer a separate chip | removed 2026-07-14 |
| **⇪ Copy** | copies a **Group 20 vault code** to the clipboard (`COPY_TO_CLIPBOARD`) | renamed from Share 2026-07-14 (TG4-MP A5) — name now matches the action; still the ONLY chip that uses the clipboard, distinct from the scrapped generic `⧉ Copy` chip below |
| ~~⧉ Copy~~ | **SCRAPPED as a generic chip** — no `⧉ Copy` on ordinary messages | the `copyButton` *helper* stays in `Chat.java` (Share uses it, and existing non-chip call sites keep compiling); only the generic chip is gone |
| ~~Dismiss~~ | **SCRAPPED** — no real use case; chat scrolls away on its own | removed |

**Curated per message type.** A message shows only the chips that make sense for it (a create-message gets
View/Edit; a delete-message gets Undo) — never a full row of dead buttons.

### Mechanism — chips are just `RUN_COMMAND` of real commands (locked 2026-07-14)

Vanilla `ClickEvent` can only **run a command / fill the chat box / copy text / open a URL** — it *cannot*
open a custom `Screen` directly. Owner's insight: a chip doesn't need a bespoke system — it just **runs a real
`/cb` command with the right argument**, and the screen-opening commands already ride the existing
server→client screen payload (`CustomBlocksClient` `OpenScreen` switch: `BLOCK_EDITOR`, `MACRO_LIST`, …).

- **No token store, no hidden `/cb click <token>`.** Edit → `/cb edit <id>`; Undo → `/cb undo`; View →
  `/cb <thing> <id>`; error-link → `/cb incidents <code>`. All are real commands with a plain argument.
- **This CANCELS two earlier decisions:** the **7-day TTL** and **disk-persisted click-targets** are dropped —
  they only existed to back a token store. Chips work as long as the target exists; the command validates on
  click and answers honestly ("already undone", "no such block").
- **"Actor-only" → "runs-as-clicker."** The command runs as whoever clicks, on *their own* per-player state.
  Everyone sees the same clickable line; clicking Undo undoes **your** last action, not the original actor's.
  No per-line UUID binding needed.

### Behaviour rules (locked 2026-07-14)

- **Destructive confirm is one click.** Delete/reset show inline Yes/No chips; clicking Yes **runs
  immediately** — no second modal, no timed auto-cancel. The chat line *is* the confirmation.
- **Staleness: resolved by the command on click.** Chips always *look* live; the underlying command reports
  honestly if the target is already gone. No live polling / auto-greying.
- **Sound: distinct cue per action type — vanilla sounds only.** Run / Undo / View / Share each map to a
  fitting **vanilla** sound (no new assets → no `soundGate` risk), still audibly distinct.
- ~~View with no target built yet~~ — **removed 2026-07-14** (TG4-MP A6). View no longer exists as a separate
  chip (merged into Edit, see button table above), and Edit's only current target (`/cb editor`, chest menu)
  is always built, so this fallback never had a real case. Not a feature, don't reintroduce it.

### Flourishes — rendered in a CB overlay layer, NEVER by rewriting vanilla chat (locked 2026-07-14; upgraded 2026-07-15)

Hard rule: **do not rewrite or re-render vanilla chat** — it's load-bearing (wrapping, scroll, history, other
mods) and is the thing most likely to break later. Instead the cool visuals live in a **CustomBlocks overlay
layer** drawn via the existing `HudRenderMixin`, which we fully own. The overlay must **track each chat line's
on-screen position every frame** so chip glow/press and toasts stay aligned as chat scrolls.

**Cooler flourish set — owner-locked 2026-07-15** (owner had called the old floor "not cool enough"):

- **Chip look = glow + hover-lift + press-squish (owner-locked 2026-07-15).** Each clickable chip renders in the
  CB overlay with a soft glow, rises/brightens on hover, and squishes down on click like a real button. Needs
  precise per-frame alignment to the chat line's position.
- **Click feedback = ALL FOUR (owner-locked 2026-07-15):**
  1. **Distinct vanilla sound per action** — Run / Undo / Redo / Copy / Confirm each map to a fitting vanilla
     sound (no new assets → no `soundGate` risk).
  2. **Particle burst at the cursor** — a small themed pop at the mouse on click (green sparkle = success, red =
     destructive).
  3. **Screen-edge flash / shake** — a brief coloured edge-pulse (green confirm) or tiny shake (destructive),
     drawn in the overlay, using the same colour language as the kick screen.
  4. **Floating result toast** — a small toast ("Undone", "Copied") pops from the clicked chip and drifts up +
     fades. This is how a click is confirmed **since the sent chat line itself cannot change** (vanilla limit).
- **Animated confirm.** The flash/shake plays in the **CB overlay layer**, not in the chat line (a sent chat
  line cannot be re-rendered — same limit that killed live-updating chat). The click still runs immediately.
- **Smart context = curate per message type only (owner-locked 2026-07-15).** A message shows only the chips
  that make sense for it (delete→Undo, create→Edit). **No** live re-evaluation / auto-greying; staleness is
  still resolved by the command on click.
- **Hover = glassmorphism info panel (owner-locked 2026-07-15).** Hovering a block id shows a small CB-drawn
  frosted-glass panel in the overlay: the block's **flat texture swatch** on the left, then **bold display
  name**, **gray id** beneath, and a **status row** (🔒 locked / ★ fav / glow level) in the semantic colours —
  matching the toast + kick-screen look. Drawn via `HudRenderMixin`, **no new tooltip mixin**. The old heavy 3D
  hover card stays dropped; this is the cleaner "premium" replacement the owner asked for.
- **Live-updating progress** (rejected for chat) **lives as a G03 HUD widget** in this same overlay layer.

### Error routing (revised)

Since chat is clickable again, major errors show **both**:
- the short code (e.g. `E-45`) as text — pasteable into bug reports, readable without clicking;
- **a clickable link** (`RUN_COMMAND /cb incidents <code>`) that jumps to that entry in the Group 16
  Diagnostics/Incidents GUI.

**New infra:** a **code → incident registry** (maps `E-45` to the full incident) is net-new in this pass. The
Group 16 search-by-code path stays as the fallback.

### Still in scope from the 2026-07-12 pass

- **Template feature deleted** — `/cb template` and the whole preset system, gone.
- **Welcome & Help extinct in G04** — moved to Group 27 / Group 23.
- ~~**Chat Spam Clearing** — `/cb clearlogs`~~ **SCRAPPED (owner-locked 2026-07-15).** Do NOT build
  `/cb clearlogs`; the command is dropped from scope entirely.

### DidYouMean is now a clickable chip too (ties to §G04-2)

Since chat is interactive again, the DidYouMean suggestion is a **clickable chip**: `Did you mean [▶ /cb
animation <tail>]?` — clicking runs the corrected command (full tail preserved). **Never auto-executes.** The
candidate-source + confidence-gate rewrite itself stays specified in §G04-2 (derive from the real registered
command tree, not the hand-list); this pass just makes the *display* a chip.

### Cross-surface combo (ties G04 → G03)

Clicking **View** in chat runs the item's screen command, which can drive the G03 ESC quick-access panel — the
panel jumps/re-scrolls to the clicked item even if it was already open on something else (force-switch). The
G03 Incidents-ping widget reuses the **same** `/cb incidents <code>` jump the chat error-link uses.

### Open bucket (not locked)

- The owner approved the *directions* — game-juice, smarter context, cross-surface combo — and the flourish
  set above, but wants to **keep pushing past them**. Owner's words on the plain utility ideas: *"those
  aren't cool enough, need better."* Treat the flourish list as a floor, not a ceiling.
- **Rejected on technical grounds:** *live-updating chat line* (a message that rewrites itself in place). Moved
  to a G03 HUD widget in the CB overlay layer instead (see Flourishes).

## G04-5 - Every CustomBlocks kick gets one human message

> **Owner discussion 2026-07-15.** Every kick or connection failure caused by CustomBlocks must be
> understandable to a human first, with technical evidence available on demand. The final review must
> show the old and new presentation for every CustomBlocks kick reason.

### Scope and ownership

This covers **every CustomBlocks-caused kick**, regardless of where the underlying failure happens:
registry sync, resource-pack delivery, command or permission handling, texture/media work, networking,
or any future CustomBlocks system. Unrelated vanilla or third-party kicks are outside this contract.

Group 04 owns the player-facing message contract, wording, colors, details view, and copyable report.
The group that owns the underlying failure still owns its actual fix and cause-specific test. For example,
the registry-capacity failure shown in the owner's screenshot remains a Group 21 cause, but its player
message must follow this Group 04 contract.

### One structure for every kick

Every CustomBlocks kick uses the same three-part order:

1. **What happened** - a short, plain explanation.
2. **Why it happened** - the likely cause in normal language.
3. **How to fix it** - concrete steps the player can take.

The main message never leads with a raw exception, registry count, Java class name, or mod jargon.
The player sees the human explanation first. A **Technical Details** control reveals the deeper information
without replacing the human explanation.

### Text color contract

Colors communicate meaning and stay the same for every CustomBlocks kick:

| Content | Color |
| --- | --- |
| Main error title | Red |
| Normal explanation | White |
| Why it happened | Yellow |
| How to fix it | Green |
| Technical Details and Copy Report controls | Light blue |
| Raw log and extra technical text | Gray |

This is semantic color, not decoration. The kick screen must not become a rainbow or color every word.

### Copy Report

The button is named **Copy Report**. It must never be labelled "Copy AI Report". It copies a complete,
pasteable report containing, when available:

- The human summary.
- The raw Minecraft/CustomBlocks error.
- The technical details.
- The error code, when one exists.
- Minecraft and CustomBlocks versions.
- Server type and connection phase.
- The likely cause.
- The recommended fix.
- Relevant recent action and troubleshooting context.

The report is useful when pasted to any helper or bug tracker, but it must not mention AI as part of the
feature wording. The copy action must work from the kick screen even when the player cannot join the world.

### Owner-provided before/after example

**Current raw screen:**

```text
Connection Lost

Received 400 registry entries that are unknown to this client.
This is usually caused by a mismatched mod set between the client and server.
The following registry entry namespaces may be related:
customblocks
```

**Unified screen:**

```text
CustomBlocks could not connect

What happened:
Your client and this server have different CustomBlocks block limits.

Why:
The server has entries that your client does not know about.

How to fix it:
Increase the client limit, restart Minecraft, and join the server again.

[Technical Details] [Copy Report]
```

### Final before/after review

After the kick sweep is complete, the final review must list **every** CustomBlocks kick reason with:

- The old message or screen.
- The new unified message or screen.
- What changed.
- The player-facing fix.
- The technical details available through Details and Copy Report.
- The test result and any remaining issue.

No CustomBlocks kick path is considered unified until it appears in that review and no raw CustomBlocks
kick message remains as the primary player-facing explanation.

### ⚠️ TRUE CODE STATE — deep search 2026-07-15 (read this FIRST)

The repo is a **half-applied §F/G04-3 migration that does NOT compile.** Verified against source:

- **`Chat.java` is the OLD version** — has `success/error/info/line/tool(body)/runButton(3-arg)/copyButton/suggestButton/hover` only. **Missing** the methods the migration already depends on: `raw`, `lockedError`, `toPlayer`, `tool(label,value)`, `toolSuccess` (both a sentence form AND a label+value form — owner-locked), `toolError`, `lockedTool`, `hotbar`, and a **2-arg `runButton(label,command)`**.
- **40 call-sites across 13 files already call the missing methods** (ColorToolService, SlotBlock:227, BuzzerGameWand, GuessShowcaseBlock, AdminPanelBlock, DidYouMean:83-84…). → **build is red until `Chat.java` is finished.**
- **`CbFmt.java` + `HotbarSpeaker.java` are untracked (new).** `CbFmt.TOOL_NEUTRAL` is already `§7` (F3's colour — just unwired into `Chat.tool`).
- **The 4 build gates are already WRITTEN + WIRED** in `verify.gradle`/`build.gradle` (`chatRouteGate`, `chatColourGate`, `hotbarRouteGate`, `helpCoverageGate`). Job = make them **pass**, not build them.
- **`OmniToolItem.useOnBlock` has NO lock check** (F2 confirmed) and still embeds `§e/§7/§f` in `Chat.tool(...)` bodies (not migrated).
- **DidYouMean (G04-2) is code-complete** (real-tree matcher, confidence gate, tiers, chip) **but won't compile** (needs `Chat.raw` + 2-arg `runButton`). Its D1/D2/D3 "passed" marks are **stale**. Once Chat is finished it revives → re-test + add **suggestion-only aliases** (the only feature left).
- **§H kicks: near-greenfield.** Only ONE CB kick exists (`RegistrySyncHealMixin` → `MaxSlotsHealer.restartScreenText`, the registry/`max_slots` desync). No CB mid-game kicks. Build a generic Screen framework + wire that one.

**So the real order:** finish `Chat.java` API → migrate OmniTool (+F2 gate all modes) → wire F3 (`TOOL_NEUTRAL`) → `gradlew build` GREEN (gates pass) → re-test F1–F6 + D1–D3 → add DYM aliases → G04-3 rename no-op → G04-4 chips + flourishes → §H framework. ONE final jar; owner tests everything at the end.

### This pass builds ALL of Group 04 (owner-locked 2026-07-15)

Scope widened again 2026-07-15: the new session finishes **the entire group** in one go —
**F2** (Omni-Tool lock-bypass), **F3** (neutral `§7`), **G04-2 (DidYouMean rewire), G04-3 (chat unification
sweep + wiring the two build gates), G04-4 (interactive chat), and §H/G04-5 (kick unification)** — verifying as
it goes. **Tomato (Group 32) is set aside until TG4 + G4 are fully built and tested** (owner-locked 2026-07-15).

**Build cadence (owner-locked 2026-07-15):** implement each item **one by one, correctly** (not a rushed
all-at-once dump), but deliver **ONE final jar** at the end for a single full Group-04 test batch — not staged
per-slice test jars. Build order sequential, each feature finished + self-checked before the next.

**Sequence for G04-3 gates:** clean all ~80 legacy call sites first, *then* wire `chatRouteGate` +
`chatColourGate` into the build so it lands green.

**Scope edges nailed 2026-07-15:**
- **`/cb rename x x` no-op + small wording nits — FOLDED into the G04-3 sweep** (the sweep already touches
  every handler; add the friendly "already named that" no-op check like `/cb reid` has).
- **`/cb clearlogs` — SCRAPPED** (see above).
- **✎ Edit chip stays "coming later" this batch** — the editor Screen is NOT built now (both old candidates
  rejected; a real one needs its own UX design session). Group 04 is "done" without it; the chip answers
  honestly.
- **A7-hub bug (Hub Console filter) — OUT**, stays Group-07-owned, rides the G07 batch test. Not this batch.

**§H/G04-5 kick unification — build decisions (owner-locked 2026-07-15):**
- **Covers BOTH connect-fail AND mid-game kicks (owner-locked 2026-07-15)** — any CustomBlocks-caused
  disconnect gets the unified screen, whether it fires while joining (registry mismatch) or while already
  playing (mid-session crash/kick).
- **Full custom Screen** — replace vanilla `DisconnectedScreen` via a client mixin. Full control of the
  three-part coloured layout (title red / explanation white / why yellow / fix green / Details+Copy light blue /
  raw gray), a Technical Details expander, and a real **Copy Report** button (never "Copy AI Report"; must work
  from the disconnect screen even when the player cannot join). This adds a client mixin on the disconnect
  screen — flag it against the CLAUDE.md Mixin Checkmark.
- **Hunt EVERY kick path** — deep-search the whole codebase for every place CustomBlocks disconnects/kicks a
  player, plus web research on known Fabric/registry/resource-pack kick causes. Find them ALL, miss none; the
  registry block-limit mismatch (Group 21 screenshot) is only the first documented cause.
- **`/cb debug kick <cause>` dev command** — hidden, force-fires each unified kick screen on demand so every
  cause + the Copy Report can be tested without reproducing a real crash.

| Group | Scope | Status |
|---|---|---|
| G04 — Chat / Command surface | Both (SP + MP) — command routing; chips are `RUN_COMMAND` of real commands; screens are client `Screen`s | 🔍 re-locked 2026-07-14 (interactive chat, deep-architecture pass) — **not built**. |

**Touches:** `Chat.java` (repoint `✎` to editor, per-action vanilla sounds, keep `copyButton` for Share) ·
`DidYouMean` + `CommandRegistrar` + `HelpTopics` (G04-2 rewire) · ~20 handler files + ~80 call sites (G04-3
sweep) · `build.gradle` (wire the two chat gates) · `HudRenderMixin` (CB overlay layer for animation) · a new
narrow tooltip-hover mixin (rich hover card) · a new **code→incident registry** + `/cb clearlogs` command ·
Group 16 Diagnostics (error-link jump) · Group 20 (vault code for Share) · Group 27 (screens the chips open).
**No token store, no click-target persistence — chips are plain commands.**
