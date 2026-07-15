# Group 04 — Messages & Command Communication

**🎯 Active:** **BATCH = FINISH ALL OF GROUP 04 (owner-locked 2026-07-15).** One session builds every unbuilt
Group-04 item: **F2** lock-bypass fix · **F3** neutral `§7` · **G04-2** DidYouMean rewire · **G04-3** chat
unification sweep + 2 build gates (~80 call sites) · **G04-4** interactive clickable chat redesign · **§H/G04-5**
kick unification as a **full custom Screen** + `/cb debug kick <cause>` dev command. **Tomato (Group 32) is set
ASIDE until TG4 + G4 are fully done + tested** (owner-locked 2026-07-15).  
**📦 Jar:** `customblocks-1.0.0.jar`

## 📊 Status

|                 |                                   |
| --------------- | --------------------------------- |
| **Verdict**     | 🛠️ **Batch = finish all of Group 04 (owner-locked 2026-07-15).** ⚠️ **DEEP-SEARCH 2026-07-15: the tree does NOT compile** — a prior session half-migrated §F/G04-3 (40 call-sites in 13 files already call `Chat.toolSuccess/toolError/lockedTool/hotbar/raw`, but `Chat.java` has none of them; `CbFmt`+`HotbarSpeaker` are untracked). So **§F is UNBUILT** and **F1/F4/F5/F6 never "passed MP"** (impossible — no build). DidYouMean (G04-2) is code-complete but **also won't compile** (same missing `Chat.raw`/2-arg `runButton`) → its D1/D2/D3 marks are **stale/untested**. Build order: finish `Chat.java` API → migrate OmniTool + gate F2 → wire F3 → compile GREEN → then G04-2 aliases · G04-3 gate-pass + rename no-op · G04-4 interactive chat + flourishes · §H generic kick framework. Tomato deferred until TG4+G4 done. · ✅ §G undo passed MP · ✅ §A passed MP · §C → Group 27 · §B cut |
| **Progress**    | 🎯🎯🎯🎯🎯🎯 §F all unbuilt (F1–F6) · G04-2 code-complete but blocked · G04-3 gates wired, need pass · G04-4 new · §H new · ✅ §A/§G history |
| **Last tested** | 2026-07-15 (MP)                   |

> 💡 **Confused by symbols or terms?** See [00_GLOSSARY.md](extra/00_GLOSSARY.md)

## 🗺️ Sections

| § | What                                                        | State       |
| --- | ----------------------------------------------------------- | ----------- |
| F | **Hotbar unification** — one voice, one colour contract, anti-spam, route gate | 🎯 UNBUILT — tree doesn't compile; finish `Chat.java` API + migrate all call-sites (2026-07-15) |
| G | **Undo dialect** — one chip, one command; lock/fav become undoable; ⊙ View deleted | ✅ passed MP 2026-07-15, folded |
| A | Chat chips                                                   | ✅ all passed MP 2026-07-15 |
| C | Errors + admin                                               | ✅ moved to Group 27 §R (Diagnostics Screen) |
| D | DidYouMean (G04-2)                                           | 🎯 code-complete but WON'T COMPILE (needs `Chat.raw`+2-arg `runButton`); D1/D2/D3 marks stale → re-test + add aliases |
| H | Every CustomBlocks kick — human message + Copy Report         | 🛠️ IN THIS BATCH (owner-locked 2026-07-15) — build G04-5 kick unification + `/cb debug kick <cause>` dev command |
| E | Build gates                                                  | dev-only, no test rows |

🔗 **Related Doc:** [GROUP_04_CHAT.md](../groups/GROUP_04_CHAT.md) · **ADRs:** [ADR-016](../adr/ADR-016-chat-chips-are-run-command.md), [ADR-017](../adr/ADR-017-cb-overlay-layer-not-chat-rewrite.md)

---

# 🎯 Test now

### 🧰 Setup Required
* `/cb create c1 Test` — the block every F row acts on
* The Omni-Tool in hand for F4/F5

## F · Hotbar unification · 🟡 built; F2/F3 fixes queued 2026-07-15

> 💡 The rule: the **hotbar** is what you did with your **hands**; **chat** is what you asked for with a
> **command**. Never both.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| F2 | Use a tool on a locked block (try **every** Omni mode: Glow, Hardness, Area) | hotbar reads `Can't edit "x" — /cb unlock x` — label **red**, value **yellow**, tail **light gray `§7`**; the block is NOT edited or selected in any mode | 🟥 | 🟥 |
| F3 | A tool reports a neutral fact (e.g. nothing selected) | hotbar reads `Nothing selected` in **light gray `§7`** (owner-locked 2026-07-15 — light gray, NOT the codebase's current `§8` dark-gray 'gray' mapping, and not white). F2's error tail goes `§7` too | 🛠️ | 🛠️ |
| F1 | Use any tool that succeeds (e.g. set glow with the Omni-Tool) | hotbar reads `Glow set 12` — label **green**, value **yellow**, no `[CB]`, no ✔ | 🎯 | 🎯 |
| F4 | Hold a tool and swing it fast, repeatedly | the same line does **not** strobe — an identical message inside 20 ticks (1s) is swallowed; a *different* message always gets through instantly | 🎯 | 🎯 |
| F5 | Cycle the Omni-Tool's mode | the new mode appears **once** on the hotbar, then fades. No chat line, no permanent widget | 🎯 | 🎯 |
| F6 | Run any `/cb` command | it answers in **chat only** — never duplicated onto the hotbar | 🎯 | 🎯 |

> ⚠️ **`hotbarRouteGate` = HARD build-fail, WIRED 2026-07-15.** `gradlew build` fails on any raw
> `sendMessage(…, true)` outside `Chat`, or any `§`/`CbFmt` colour inside a hotbar body. All 68 hotbar
> call-sites (19 files) are migrated off embedded colour onto `(label, value)` meaning; anti-spam swallows an
> identical line within **20 ticks (1.0s)** (a different line passes instantly), dwell uses **vanilla's own
> hotbar fade** (~5s). `Chat.tool(p, label)` = white · `tool(p, label, value)` = white + yellow value ·
> `toolSuccess(…)` = green + yellow (F1) · `toolError(p, label, value[, tail])` = red + yellow + white tail (F2).

## E · Chat unification + build gates

> 💡 Not a player-testable section — no SP/MP rows. `chatRouteGate` + `chatColourGate` + `hotbarRouteGate` +
> `helpCoverageGate` run automatically in `gradlew build`; status: 🟢 green (§F build, 2026-07-15).

---

## H - Every CustomBlocks kick - human message + Copy Report (🛠️ IN THIS BATCH — owner-locked 2026-07-15)

> 🎯 **In the batch (owner-locked 2026-07-15) — GENERIC FRAMEWORK + wire the 1 real kick.**
> ⚠️ **DEEP-SEARCH 2026-07-15:** the codebase has **exactly ONE** CustomBlocks-caused kick — the registry /
> `max_slots` desync, and it **already** shows a friendly Text via `RegistrySyncHealMixin` →
> `MaxSlotsHealer.restartScreenText`. There are **ZERO** CB code paths that kick mid-game. So "hunt every path"
> realistically yields one (plus possibly a resource-pack failure).
> **Build (owner-locked 2026-07-15):** a **GENERIC** custom kick Screen (replace vanilla `DisconnectedScreen`
> via client mixin; 3-part coloured layout + Technical Details expander + real **Copy Report** button; covers
> connect-fail AND mid-game shape even though no CB mid-game kick exists yet), then **wire the one real kick
> (registry) through it** + any resource-pack failure found. Future CB kicks plug in free. Plus the hidden
> **`/cb debug kick <cause>`** dev command to force-fire each screen for testing.

This section tests the Group 04 user-facing contract from `GROUP_04_CHAT.md` §G04-5. It covers every
kick or connection failure caused by CustomBlocks, regardless of the underlying feature. Cause-specific
tests remain in the source group's guide; TG4 verifies that the player-facing result is always unified.

Every case must use **What happened -> Why it happened -> How to fix it**. The main message is human
and plain. Technical Details is optional. The copy control is named **Copy Report** and never mentions AI.

| # | Action | Expected Result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Trigger a representative CustomBlocks kick or connection failure. | The player sees one consistent message with What happened, Why, and How to fix it; no raw error leads the screen. | ⏳ | ⏳ |
| H2 | Reproduce the registry mismatch shown in the owner's screenshot. | The player sees a human explanation about the client/server CustomBlocks block-limit mismatch, restart guidance, Technical Details, and Copy Report. | ➖ | ⏳ |
| H3 | Run the complete matrix of CustomBlocks kick causes from each owning group. | Every CustomBlocks-caused kick uses the same three-part structure; no cause is left with the old raw presentation. | ⏳ | ⏳ |
| H4 | Inspect the unified kick screen. | The title is red, normal explanation white, reason yellow, fix green, Details/Copy Report light blue, and raw technical text gray. | ⏳ | ⏳ |
| H5 | Open Technical Details. | The original raw error and deeper diagnostic context are available without replacing the human explanation. | ⏳ | ⏳ |
| H6 | Press Copy Report from the kick screen. | A complete report is copied, including the human summary, raw error, technical details, error code when available, versions, server type, connection phase, likely cause, fix, and relevant troubleshooting context. | ⏳ | ⏳ |
| H7 | Read the copied report and button labels. | The control is named Copy Report; the report contains no feature wording about AI and is ready to paste into a bug report or helper conversation. | ⏳ | ⏳ |
| H8 | Trigger different CustomBlocks kick causes one after another. | The voice, layout, colors, Details behavior, and report fields remain consistent; no raw exception or class name becomes the headline. | ⏳ | ⏳ |
| H9 | Complete the final kick-unification review. | The before/after review lists every CustomBlocks kick reason with the old screen, new screen, change, player fix, available technical details, and test result. | ➖ | ➖ |

### H2 reference example

**Old:** `Received 400 registry entries that are unknown to this client.`

**New:** `Your client and this server have different CustomBlocks block limits. The server has entries
that your client does not know about. Increase the client limit, restart Minecraft, and join again.`

The full raw message remains available under Technical Details and in Copy Report.

# 🗄️ Archive

<details><summary>✅ <b>Passed Tests History</b> (click to open)</summary>

- **G04.1–G04.7** (Passed in-game 2026-06-21) — message tone, error style, Incidents routing, DidYouMean fire/silence/config-off.
- **G04.8–G04.10** — Help / Welcome features transferred to Group 27 / Group 23, or deleted.
- **G1–G6** (Passed in-game MP 2026-07-15) — undo dialect: one `↩ Undo → /cb undo` everywhere; bulk lock/fav undoable; `/cb lock` success line distinct from its error; redo round-trips; empty stack shows no `(0 left)`; create line carries ✎ Edit only, ⊙ View gone.
- **A1** (Passed in-game 2026-07-14) — create success line carries only the merged Edit chip, aqua.
- **A4** (Passed in-game 2026-07-14) — Undo chip behaves exactly as `/cb undo`. The redo chip the owner
  asked for in the same pass was built 2026-07-14 and is now row **A4b** above, awaiting its own test.
- **A3** (Passed in-game MP 2026-07-15) — Edit chip says "coming later" and opens nothing, as designed.
- **A4b** (Passed in-game MP 2026-07-15) — the undo line carries ↪ Redo and it re-deletes.
- **A7** (Passed in-game MP 2026-07-15) — `/cb bulkdelete c1 c2 c3` deletes exactly those three, no second confirm.
- **A7b** (Passed in-game MP 2026-07-15) — chat-side filter words gone; `category:red` matches nothing. Hub half stays open as A7-hub.
- **A5** (Passed in-game 2026-07-14) — vault code lands on clipboard.
- **C3** (Passed in-game 2026-07-14) — block names stay pure white.
- **D1** (Passed in-game 2026-07-14) — DidYouMean suggests the real command as a clickable chip.
- **D2** (Passed in-game 2026-07-14) — honest "I don't know," never `/cb gui`.
- **D3** (Passed in-game 2026-07-14) — `/cb help` lists every registered command.

</details>

<details><summary>🗑️ <b>Deleted Tests</b> (click to open)</summary>

- **"Verify NO clickable buttons appear in chat"** — deleted 2026-07-14. Chat is interactive again.
- **"Error Code instead of clickable link"** — replaced by C1: the code **and** the link appear together.
- **Token store / 7-day TTL / disk-persisted click-targets** — dropped once chips became plain `RUN_COMMAND`.
- **§C entirely (C1 — incidents chest GUI)** — moved 2026-07-15 to **Group 27 §R · Diagnostics Screen**. It was
  never a chat bug: the chest GUI dumps every incident and spams chat. The fix is a Screen, so it lives with
  the screens. `⊙ Details` will open it. Group 16 keeps `IncidentRecorder`.
- **⊙ View chip** — deleted 2026-07-15, owner-locked. See bug G04-VIEW.
- **§B entirely (chip rules B1–B6)** — cut 2026-07-15, owner-locked ("weird and pointless"). They were assertions about `RUN_COMMAND` chips (who-runs-as, expiry, stale clicks, sound, hover), not features. B1/B2/B3 are self-evident once a chip = a plain command; B4 was a vanilla dead-end (⇪ Copy runs no command → no sound hook); **B5 hover tooltip is dropped, not deferred**.

</details>

---

# 🐛 Active Bugs

*Log test failures here immediately. Once fixed, clear the row.*

| Bug ID | Test Row | Issue Description | Status |
| ------ | -------- | ----------------- | ------ |
| F2-OMNILOCK | F2 | The **Omni-Tool skips the lock check entirely** — `OmniToolItem.useOnBlock` dispatches Glow/Hardness/Area with no `LockManager.isLocked` gate, so it edits a locked block and fires a green `toolSuccess` (owner saw `Hardness egypt → soft` on a locked block). Fix: gate before mode dispatch, emit `Chat.lockedTool` (red/yellow/`§7`). **Gate ALL modes incl Area-mark (owner-locked 2026-07-15)** — Glow, Hardness AND Area: nothing touches or selects a locked block. | 🟥 |
| F3-GRAYNEUTRAL | F3 | Neutral hotbar line is **white**; owner wants **light gray `§7`** (owner-locked 2026-07-15 — NOT `§8`; the CbFmt 'gray'→`§8` map is the wrong shade for this). Point `Chat.tool` neutral at `§7`; F2's error tail goes `§7` too (owner-agreed). | 🛠️ |
| A7-hub | — | The Hub's Console tab and NL bar still **build** `category:`/`id:`/`name:`/`favorite:`/`locked:` strings and send them to a server that no longer resolves them, so a Console filter silently matches nothing. Chat side confirmed fixed MP 2026-07-15 (A7b); the Hub half is held until the G07 batch test runs. See G07 TG. | ❗ |
| G03-BULKLOCK-HUD | — | `/cb bulklock` never calls `WidgetSync.pushAll`, so a bulk lock's padlocks don't appear until something else pushes — single `/cb lock` does push. Surfaced while building §G; belongs with the padlock (G03 §G03-1), not here. | ⚠️ |
| A3-screen | A3 | The Edit chip has no screen to open. Both candidates were rejected (the chest menu, and the button-grid that kicked you back to the chat box), so `/cb edit` says "coming later" until a replacement is designed. Not a regression — nothing to fix, a screen to design. | ❔ |

---
