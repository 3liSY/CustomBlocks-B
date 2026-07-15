# CustomBlocks-B AI Agent Rules

## MANDATORY INITIALIZATION RULE
**At the absolute beginning of EVERY session, you MUST read the `CLAUDE.md` file in the root directory.**
`CLAUDE.md` contains the supreme, overriding, absolute operating protocol for this workspace. 
**Core Enforcements:**
1. **NO ARTIFACTS EVER**: You are strictly forbidden from creating markdown artifacts (like `implementation_plan.md` or `task.md`).
2. **CHECKMARK REQUIRED**: NEVER jump into coding without receiving the explicit "checkmark to start" from the developer.
3. **UI QUESTIONS ONLY**: ALWAYS use interactive UI options (like `ask_question`) for decisions; no open-ended chat questions.
4. **NO JARGON**: Speak factually and concisely. Do not over-explain modding concepts.
5. **ALWAYS SEARCH THE WEB**: Use web search (Google) proactively to find fixes before guessing.

If you fail to abide by the rules in `CLAUDE.md`, you will destroy the trust of the developer. Read it, respect it, and follow it flawlessly.

The rules in this file are mandatory for all AI agents operating in this workspace.

## Testing Guides Rule

When modifying any file in `docs/testing/GROUP_*_TESTING_GUIDE.md`, you **MUST** first read `docs/testing/TESTING_GUIDE_TEMPLATE.md` and follow it exactly.

**Strict Prohibitions:**
1. **DO NOT** invent new emojis. Use ONLY the symbols defined in the template palette — state: `🟥`, `🟡`, `✅`, `⚠️`, `💔`, `❗`, `🧊`, `🛠️`, `⏳`, `❔`, `🎯`; bar: `🟩`, `🟨`; SP/MP N/A: `➖`.
2. **DO NOT** write paragraphs in the testing guide banner. The banner must be 3 lines maximum.
3. **DO NOT** put implementation details, root causes, or code deep-dives anywhere in the testing guide. Those belong in the Group Document (`GROUP_XX_NAME.md`) or the `PROGRESS_LOG.md`.
4. **DO NOT** write more than ONE single line for the `💡` feature explanation.
5. **DO NOT** put scratchpads or dev notes in testing guides. Docs must be clean.
6. **DO NOT** leave passed test tables in the guide. When a section passes, delete the entire test table and move a 1-line reference to the `✅ Passed Tests History` section at the bottom of the guide.
7. **DO NOT** sort the `## 🗺️ Sections` table alphabetically by `§`. Rows MUST follow priority order: `🎯` → `💔`/`❔`/`❗` → `🟡`/`⚠️` → `✅` → `🛠️` → `🧊` → `⏳`. The active 🎯 test-now row is today's job — it must never sit below already-passed or parked rows.
8. **DO NOT** use a `💬` callout (or any paragraph naming classes/methods/dispatch calls) as a "candidate explanation" for a bug. That is root-cause/implementation detail — it belongs in `PROGRESS_LOG.md`, never in a testing guide (this is prohibition 3, restated: a real instance existed in `GROUP_13_TESTING_GUIDE.md` §A until 2026-07-03, relocated to `PROGRESS_LOG.md`).
9. **DO NOT** use a single `Status` column on a test-row table. Every test-row table MUST have both `SP` (singleplayer/solo world) and `MP` (dedicated/LAN server) columns — a row is only ✅ when both are ✅. Use `➖` when an environment can't run that test.

**Archiving Rule:**
If an entire testing guide is ✅ 100% passed with no pending items, you must move it to `docs/testing/archive/GROUP_XX_TESTING_GUIDE.md` using the file system. Do not leave fully passed guides in the active testing folder.

**Mandatory Verification — not optional, not "looks right":**
After editing ANY `GROUP_*_TESTING_GUIDE.md`, you MUST run this before telling the developer you're done:
```
cd docs/testing && python extra/testing_tools.py health
```
If it prints any `[WARN]` line, your edit broke a rule above — fix it and re-run, don't report done with warnings present.
This is also enforced by a git pre-commit hook (`.git/hooks/pre-commit`) that blocks commits touching testing guides while warnings exist — but do not rely on the hook to catch it for you; run the check yourself as part of the edit, before commit is even considered.

**Every Jar Build Rule — no exceptions:**
After EVERY jar build (`.\gradlew.bat build`), you **MUST** edit the relevant `GROUP_*_TESTING_GUIDE.md` with the actual info of what changed in that build (what was built, what to test, current state). Not later, not "next time" — same turn as the build.
This is enforced by the `tgGate` Gradle task (`build.gradle`), wired into `build` — it fails the build if any `.java` source changed since the last build but no `GROUP_*_TESTING_GUIDE.md` changed since then too. You cannot silently skip this: the build itself will fail and tell you which guide needs updating.
