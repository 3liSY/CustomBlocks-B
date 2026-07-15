<CustomBlocks_AI_Operating_Protocol>
    <SYSTEM_PRIORITY>ABSOLUTE</SYSTEM_PRIORITY>
    
    <DOMAIN_GLOSSARY>
        <TERM name="CustomBlocks">A Fabric mod for Minecraft 1.21.1 being rebuilt from scratch.</TERM>
        <TERM name="SlotData">Immutable state object. Mutate via `.update()` → new snapshot. NEVER mutate in place.</TERM>
        <TERM name="SlotManager">Single source of truth for all 1028 slot assignments.</TERM>
        <TERM name="SlotDataStore">The ONLY class allowed to handle slot disk I/O.</TERM>
        <TERM name="GuiEngine">Primitives for standalone screens.</TERM>
    </DOMAIN_GLOSSARY>

    <LEGACY_CODE_WARNING>
        The `CustomBlocks/` folder is a nested, git-ignored repo from the old project. Reference it ONLY for algorithms. DO NOT copy its file structure or its bugs.
    </LEGACY_CODE_WARNING>

    <CORE_DIRECTIVES>
        <DIRECTIVE id="NO_ARTIFACTS">NEVER create markdown artifacts (`implementation_plan.md`, `task.md`). Plans go in chat.</DIRECTIVE>
        <DIRECTIVE id="THE_CHECKMARK">NEVER write code until the user explicitly types `[x]`, `checkmark`, or `✅`. Casual words like "okay" or "yes" MUST be ignored.</DIRECTIVE>
        <DIRECTIVE id="UI_QUESTIONS_ONLY">ALWAYS use interactive UI options (like `ask_question`) to present options. No open-ended questions.</DIRECTIVE>
        <DIRECTIVE id="NO_JARGON">Speak factually. No robotic fluff. No deep dives unless asked.</DIRECTIVE>
        <DIRECTIVE id="MANDATORY_WEB_SEARCH">If uncertain, you MUST rigorously use web search (Google), specifically searching "Fabric MC 1.21.1 [Class/Issue]".</DIRECTIVE>
        <DIRECTIVE id="PERFECT_IMPORTS">Always use `grep_search` to find exact package paths before writing code.</DIRECTIVE>
    </CORE_DIRECTIVES>

    <WORKFLOW_STATE_MACHINE>
        <STATE name="INTAKE">Read `PROGRESS_LOG.md`. Wait for the user to assign the `GROUP_XX` testing guide. Do not read all 32 guides.</STATE>
        <STATE name="DISCUSS">Propose a plan (max 5 items, ideally 1). Run `<CONFIDENCE_CHECK>`. Wait for the `✅`.</STATE>
        <STATE name="EXECUTE">Write code. Update `PROGRESS_LOG.md` (prepend). Update `GROUP_XX_TESTING_GUIDE.md`.</STATE>
        <STATE name="VERIFY">Output a `<TEST_PLAN>` containing a 2-step list of exactly what to do in Minecraft. STOP and wait for the user.</STATE>
    </WORKFLOW_STATE_MACHINE>

    <SAFETY_PROTOCOLS>
        <PROTOCOL name="CONFIDENCE_CHECK">If you cannot find explicit proof via `grep_search` or web, declare `<STATUS: UNCERTAIN>` and refuse to code until clarified.</PROTOCOL>
        <PROTOCOL name="CRASH_LOOP_SAFETY">If a crash log is provided, you are FORCED to output an `<ANALYSIS>` explaining the root cause, then propose 2 safe options via UI. No instant rewrites.</PROTOCOL>
        <PROTOCOL name="TG_GATE_PANIC">If `gradlew build` fails due to `tgGate`, DO NOT touch Java files. Just update the Testing Guide.</PROTOCOL>
        <PROTOCOL name="CORE_BLAST_RADIUS">If a minor fix requires changing a core system (like `SlotManager`), STOP. Explain the blast radius and get explicit permission.</PROTOCOL>
        <PROTOCOL name="SOFT_PUSHBACK">If the user asks for laggy/unsound code, offer 2 safer alternatives via UI.</PROTOCOL>
    </SAFETY_PROTOCOLS>

    <KNOWN_PITFALLS>
        <PITFALL name="Mojibake">Encoding corruption. Use UTF-8 everywhere. See `TextSanitizer`.</PITFALL>
        <PITFALL name="Note Block Sounds">`BLOCK_NOTE_BLOCK_*` needs `.value()` because they are `RegistryEntry<SoundEvent>`.</PITFALL>
        <PITFALL name="Client-side tool skip">Don't early-return on `world.isClient`; gate on `ServerPlayerEntity`.</PITFALL>
        <PITFALL name="Brigadier Args">Name the arg `"subcommand"`, never `"unknown_cb_tail"`.</PITFALL>
        <PITFALL name="IO Executor">Use `flushSaveForReload()`. NEVER `shutdown()` the IO thread.</PITFALL>
        <PITFALL name="Batch Recolor">Force `edge` mode in batch ops. Never player `full` mode.</PITFALL>
        <PITFALL name="Client Command Root">Give client commands their own top-level root (e.g., `/cblowres`), not `/cb`.</PITFALL>
    </KNOWN_PITFALLS>

    <MODDING_SPECIFICS>
        <RULE>No monoliths: `.java` ≤ 500 lines, command handler ≤ 400, config ≤ 300.</RULE>
        <RULE>Explicitly write backward-compatibility checks in `fromNbt()`.</RULE>
        <RULE>Wrap all packet handling in `server.execute()` or `client.execute()`. No network thread modification.</RULE>
        <RULE>Use Fabric Data Generation API for JSON assets.</RULE>
        <RULE>Before writing Mixins, state the target method, injection point, and wait for a "Mixin Checkmark".</RULE>
        <RULE>STRICT DEPENDENCY BAN: Never add a dependency without proving Vanilla lacks it and getting UI permission.</RULE>
    </MODDING_SPECIFICS>

    <CODE_CLEANUP_RULES>
        <RULE>Touch the exact bug line and NOTHING else. Zero formatting clean up without permission.</RULE>
        <RULE>Never delete existing comments or unrelated code.</RULE>
        <RULE>Use `grep_search` to prove a method is dead before asking to delete it.</RULE>
    </CODE_CLEANUP_RULES>

    <UI_SCREEN_RULES>
        <!-- Mod-wide, owner-approved 2026-07-10 (G07 test run). Apply to EVERY Screen, not just Bulk. -->
        <RULE id="OPAQUE_MODALS">Any modal/popup/dialog MUST fully occlude what's behind it — solid opaque fill (`DIALOG_BG` = `0xFF000000`), never a translucent scrim that lets the screen bleed through. Bleed-through is a bug.</RULE>
        <RULE id="NO_TEXT_OVERLAP">No label, heading, or value may overlap another element or be clipped/truncated. Derive layout from measured control heights (see `BulkOpsView.previewTop()`), never hardcoded offsets. If text must be shortened, it is a layout bug to fix, not to ellipsize.</RULE>
        <RULE id="TABS_ON_LEFT">Screen tabs sit on the LEFT rail, never across the top.</RULE>
    </UI_SCREEN_RULES>

    <TESTING_GUIDE_STRICT_RULES>
        <RULE>ONLY use approved emojis (locked 11-symbol state scale, defined once in `docs/testing/extra/00_GLOSSARY.md`): ✅, 🟡, 🎯, 🟥, ⏳, ⚠️, 💔, ❗, 🧊, 🛠️, ❔. No per-file legend line — every guide points to the glossary instead.</RULE>
        <RULE>DO NOT write paragraphs in the banner. Max 3 lines.</RULE>
        <RULE>Sort rows by priority: 🎯 -> 💔/❗ -> 🟡/⚠️ -> ✅. Do NOT sort alphabetically.</RULE>
        <RULE>Every test table MUST have both SP and MP columns. Single 'Status' column is forbidden.</RULE>
        <RULE>Max ONE sentence explanation per feature (💡). Do NOT put implementation details or root causes in the guide.</RULE>
        <RULE>When a test passes fully, delete the table and move a 1-line reference to the Passed History section.</RULE>
    </TESTING_GUIDE_STRICT_RULES>

    <GIT_AND_DOCUMENTATION>
        <RULE>Conventional Commits only: `feat(scope):`, `fix(scope):`, `docs:`, `refactor:`, `chore:`.</RULE>
        <RULE>NEVER commit unless explicitly requested by the developer.</RULE>
        <RULE>Non-obvious design choices get an ADR in `docs/adr/`.</RULE>
        <RULE>Update `CHANGELOG.md` for any user-facing changes.</RULE>
    </GIT_AND_DOCUMENTATION>
</CustomBlocks_AI_Operating_Protocol>
