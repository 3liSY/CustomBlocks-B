# CLAUDE.md — CustomBlocks Operating Protocol

> Auto-loaded every session. This is not background reading — it is how we work.
> If anything here conflicts with a clever idea you just had, this wins.

---

## 1. What this project is

CustomBlocks is a **Fabric mod for Minecraft 1.21.1** that turns image/GIF URLs into
working, fully-attributed blocks in real time. We are doing a **clean-room fresh start**:
rebuilding the mod from scratch, recycling proven logic from the old project but **never**
its structure.

- **Single source of truth for the design:** `CustomBlocks_Engineering_Bible.md` (root).
- **Where we are right now:** `PROGRESS_LOG.md` (root). Read its top entry first, every session.
- **Old project (reference only):** `CustomBlocks/` — its own nested git repo, git-ignored.
  Read it to recycle *algorithms*. Do **not** copy its file structure or its bugs.

---

## 2. THE GOLDEN RULE — read it twice

> **Nothing is ✅ DONE until the developer runs it in-game and confirms it works.**
>
> **Nothing is ✅ DONE until the developer runs it in-game and confirms it works.**

- Build passes → **NOT done.**
- Code looks correct → **NOT done.**
- "I verified the logic" → **NOT done.**
- Developer says "works" / sends a screenshot → ✅ **done.**

You cannot launch their server or see their game. A green `gradlew build` proves the code
**compiles and the gates pass** — nothing more. Say exactly that, never more.

---

## 3. Who you're working with

The developer is **not a programmer**. They have spent months building this mod with AI and
have been burned repeatedly by assistants that made huge plans, marked everything "done",
and shipped broken code. They are tired of that. Your job is to be **reliable, not impressive.**

- When they say "idk" → they're overwhelmed. Narrow to one question.
- When they say "it's broken" / "you broke it" → find the last good state, offer to revert,
  don't defend the change.
- No theatrical language. No "Celestial Architect" nonsense. Plain, factual, calm.

---

## 4. How we work — phase discipline

We follow the Engineering Bible roadmap (§8) **one phase at a time**, in order:

- **Phase 0 — Foundation ✅** (scaffold, build, gates) — build-verified; awaiting in-game confirm.
- **Phase 1 — Block Slot System** (next): SlotBlock, immutable SlotData + `.update()`,
  SlotManager (1028 blocks), SlotDataStore, BlockFinder, minimal CustomBlocksConfig.
- …through Phase 16 (testing + release).

Rules:
- **Finish and verify the current phase before starting the next.** No skipping ahead.
- **Max 5 items in any plan. Ideally 1.** A big plan is not competence — it's not listening.
- **Start minimal, expand per-phase** (Bible §7). Add a dependency, config field, or class
  only when the feature using it is being built.
- After each chunk: update `PROGRESS_LOG.md`, update `CHANGELOG.md` if user-facing, and stop
  for the developer to test before piling on more.

---

## 5. Architecture rules — non-negotiable (Bible §3, §9.3)

1. **No monoliths.** The old `GuiManager` hit ~9,400 lines and `CustomBlockCommand` ~6,300.
   Never again. The `verifyFileSize` build gate enforces this:
   - any `.java` ≤ **500** lines · command handler ≤ **400** · `*Config.java` ≤ **300**.
   - If you're about to exceed a limit, **split first**, then continue.
2. **`SlotData` is always immutable.** Mutate via `.update()` → new snapshot. Never in place.
3. **`SlotManager` is the single source of truth** for all 1028 slot assignments. Nothing else
   mutates slot state directly.
4. **All slot disk I/O goes through `SlotDataStore`.** No other class reads/writes slot files.
5. **Commands split by domain** → `command/handlers/*`, registered by `CommandRegistrar`.
6. **Screens are standalone** → `gui/screens/*`, sharing primitives from `GuiEngine`.
7. **Atomic file writes** everywhere (write-temp + rename) so a mid-save crash can't corrupt data.
8. **Client never mutates server state.** Server is always authoritative.
9. **Every class gets a header comment** (responsibility, depends-on, called-by) per NFR-08.
10. **Non-obvious decisions get an ADR** in `docs/adr/`.

---

## 6. The build (must stay green)

- **Java:** Temurin **JDK 21** only. This machine's default is JDK 26, which Gradle 8.8
  cannot run under (`major version 70`). The Gradle daemon is pinned to JDK 21 via
  `C:\Users\POTATO\.gradle\gradle.properties`. In IntelliJ, set the Gradle JVM to 21.
- **Build:** `.\gradlew.bat build` — compiles, then runs the gates:
  - `verifyMojibake` — fails on CP1252→UTF-8 corruption (NFR-11). See `docs/MOJIBAKE_SHIELD.md`.
  - `verifySound` — fails on `SoundEvents.BLOCK_NOTE_BLOCK_*` used without `.value()` (NFR-12).
  - `verifyFileSize` — fails on monolith files (§9.3).
- **In-game (developer):** `.\gradlew.bat runClient` → look for `[CustomBlocks] …` log lines.
  Per-phase test checklists live in **`docs/TESTING_GUIDE.md`** — add the current phase's
  tests there as it's built, and have the developer run them before marking it done.
- All source files are **UTF-8, no BOM**. `.editorconfig` enforces formatting.

---

## 7. Known pitfalls — do NOT reintroduce (Bible §9.6)

| Pitfall | Prevention |
|---|---|
| Mojibake (encoding corruption) | UTF-8 everywhere; `verifyMojibake` gate; `TextSanitizer`. |
| `BLOCK_NOTE_BLOCK_*` without `.value()` | They are `RegistryEntry<SoundEvent>`; `verifySound` gate. |
| Client-side skip delay on tools | Don't early-return on `world.isClient`; gate on `ServerPlayerEntity`. |
| `DidYouMean` arg shown verbatim | Name the Brigadier arg `"subcommand"`, never `"unknown_cb_tail"`. |
| IO executor shutdown on reload | Use `flushSaveForReload()` — never `shutdown()` the IO thread. |
| Batch recolor destroying designs | Force **edge mode** in batch ops; never player "full" mode. |
| ConfigSync firing before batch ends | Broadcast `ConfigSyncPayload` **after** batch completes. |
| Long pack debounce → purple blocks | Keep the debounce window short (~500ms). |
| Snapshot restoring wrong state | Persist the selected snapshot **ID to disk**, not just memory. |
| Dirty worktree losing work | Commit or stash before ending a session (commit only when asked). |

---

## 8. Documentation & git protocol

- **`PROGRESS_LOG.md`** — one entry per session: Done / Decisions / Verified / Next. Newest on top.
- **`CHANGELOG.md`** — update for anything user-facing.
- **ADRs** in `docs/adr/` for non-obvious design choices (template provided).
- **Commits** (Conventional Commits): `feat(scope):`, `fix(scope):`, `docs:`, `refactor:`, `chore:`.
  - **Never commit unless the developer asks.** Never commit directly to `main` — branch first.
  - List every change in the commit. The developer can't test what they don't know changed.

---

## 9. Forbidden behaviors (absolute)

| ❌ Never | Why |
|---|---|
| Mark ✅ DONE without the developer's in-game confirmation | The lie that broke trust before. |
| Plan more than 5 items at once | Big plans = big untested messes. |
| "While I was at it, I also changed X" | Every unasked change is a potential new crash. |
| Say "I think" / "probably" about code behavior | Read the file. State facts. |
| Create a monolith / exceed file-size limits | The original disaster. The gate will fail you. |
| Theatrical language | It wastes space and trust. |
| Build before confirming what the developer wants | Ask first when unsure. |
| Call a plan a fix | A plan is not a fix. Tested working code is. |

---

## 10. Start-of-session checklist

1. Read the top entry of `PROGRESS_LOG.md` — what's the current phase and state?
2. State plainly: what's verified working, what's not, and the single next step.
3. Confirm the developer agrees on that next step **before** writing code.
4. Keep it to one phase, small steps, and hand back for in-game testing.
