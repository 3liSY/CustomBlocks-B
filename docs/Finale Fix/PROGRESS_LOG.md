# Finale Fix — Progress Log

> Scope: this log documents **only the fixes actually implemented** in the Finale Fix groups
> (Phase 17 — GUI Restoration & Legacy Recovery, Groups 01–25). It is not the whole-project log
> (see the root `PROGRESS_LOG.md` for that). One entry per group that has had fixes landed.
> Newest at the top.

---

## M1 / D — `/cb tolerance` strength (0-100) + clickable mode prompt · 2026-06-10 (written, NOT tested)

**Status:** Written, build green (compile + 3 gates), jar in `.minecraft\mods\`. **Not tested in-game.**
Full status board: `Reports/GROUP_06_TRACKER.md`.

**Built (D — from scratch, NOT copied from the old code):**
- `CustomBlocksConfig.backgroundTolerance` (int 0-100, default 30) + load(clamp 0-100)/save.
- `BackgroundRemover.apply(input, mode, tolerance)` — strength is now a parameter: 0-100 maps linearly
  onto CIE-LAB ΔE [0, 40] (one `MAX_DELTA_E` constant); fringe = ΔE + 6. `apply` no-ops when mode is
  none **or** tolerance ≤ 0. `snapBackgroundBlack` takes tolerance and no-ops the same way.
- `/cb tolerance <0-100>` (ConfigCommands): `0` → sets mode none + "turned off" message; `>0` → stores
  the strength and sends a **clickable** chat prompt — `[ Background Only ]` / `[ Background + Enclosed
  Areas ]` (RUN_COMMAND → `/cb config background BgRemove` / `BgRemove&More`). `/cb tolerance` (no arg)
  shows current strength + mode.
- Config GUI background row now shows `Strength: N/100`.

**Comparison vs the OLD tolerance code (as requested):**
- OLD: one raw ΔE int gated behind THREE coupled flags (`bgRemovalEnabled` + `colorToolBackgroundMode`
  + `bgRemovalTolerance`); "tolerance 0 = off" special-cased + duplicated across ~4 near-identical
  methods (`replaceBackground` / `…WithColor` / `…WithFringeTolerance`); a dead `bgRemovalUseYcbcr`
  alternate-distance branch; hardcoded `fringe = tol+15`. The handoff itself flagged the old config
  as "the buggy part."
- NEW: one friendly 0-100 strength → one ΔE mapping; ONE `apply()` (no duplicated variants); a single
  clean no-op guard (mode none OR tolerance ≤ 0); no dead branch; reach chosen by a clickable prompt
  instead of a string the user must memorize; atomic config. Clean separation of **strength**
  (tolerance) vs **reach** (mode) vs **on/off** (tolerance 0).

**Files:** `BackgroundRemover.java`, `CustomBlocksConfig.java`, `ConfigCommands.java`,
`CreationCommands.java`, `ConfigMenu.java`. Per the Golden Rule, NOT done until tested in-game.

**Queued next (approved, NOT built — one tested pass each):** A eyedropper · B despeckle · C preview
(all toggleable in config + `/cb config`) · M2 triangle fills · **M3 hex recolour of the colour
markers + "recolour existing blocks?" GUI prompt + marker-texture hex** · M4 per-face paint. See tracker.

---

## M1 follow-up: bg mode names/commands, true-black fix, config row + chat marked partial · 2026-06-10 (written, NOT tested)

**Chat formatting = PARTIAL (polish later).** Same convention as the GUIs + item lore — chat tone /
formatting works but the developer wants a dedicated polish pass later. Not a regression.

**Background removal — naming + commands (exact mapping requested):**
- none   → menu "No Background Removal"            · arg `NoBgRemove`
- edges  → menu "Background Removal Only"          · arg `BgRemove`
- closed → menu "Background + Closed Areas Removal" · arg `BgRemove&More`

Internal config value stays none/edges/closed; `BackgroundRemover.fromArg / displayName / commandArg /
next` do the mapping. `/cb config background` now takes a greedy arg (so the `&` in `BgRemove&More`
parses) with tab-suggestions. Added an editable Background Removal row to the config GUI
(`ConfigMenu`, slot 26) that cycles the mode.

**True-black fix (3a):** Stage 3 now composites leftover transparency against BLACK (was white → gray
halos); added a post-resize near-black snap (every channel ≤ 24 → #000000) to kill the dark-gray halos
bicubic downscaling leaves around the black background. The remaining confetti-zone grayness is an
*accuracy* issue (the algorithm doesn't recognize decorative confetti as background) — brainstorm pending.

**Files:** `BackgroundRemover.java` (mapping + black composite + snap), `ConfigCommands.java` (args),
`ConfigMenu.java` (row), `CreationCommands.java` (snap wire). Per the Golden Rule, NOT done until tested.

---

## M1 — Background remover (CIE-LAB, 3 modes, black fill) · 2026-06-10 (core slice written, NOT tested)

**Status:** Core slice written, build green (compile + 3 gates, 18s), jar in `.minecraft\mods\`.
**Not tested in-game.**

**Decision (developer):** removed background → **opaque BLACK, exactly like the old version**. Render
stays opaque (slot blocks are solid — confirmed `cube_all`, no `render_type`, no RenderLayer reg), so
no transparency; a removed background is flat black.

**Done this pass (4 changes):**
- NEW `image/BackgroundRemover.java` — clean recode of the old `ImageProcessor.replaceBackground`:
  corner-median sample → CIE-LAB ΔE flood-fill from every border pixel → (CLOSED only) enclosed-area
  pass → 1px anti-fringe dilation → paint background opaque black, white-composite any leftover
  transparency. Modes: `none` / `edges` / `closed`. ΔE tolerance = 12.0 (constant), fringe = +15.
- `CustomBlocksConfig.backgroundMode` ("none" default) + load/save; normalized via `BackgroundRemover`.
- `CreationCommands.applyTexture` — runs `BackgroundRemover.apply(raw, mode)` on the RAW image BEFORE
  `toBlockPng`.
- `/cb config background [none|edges|closed]` setter (`ConfigCommands`), mirrors silentpack/hud.

**Deliberate deviation from the handoff:** the handoff said hook AFTER `toBlockPng`; that runs on the
padded square, and transparent padding would poison corner-sampling on non-square images. So it runs
BEFORE resize, matching the OLD project's order (`replaceBackground` → resize). Bug-free + parity.

**Scope notes / deferred to M1 pass 2:**
- Applies to FUTURE (re)textures only — existing blocks unchanged until retextured.
- Static-image path only (`toBlockPng`); GIF/video untouched.
- Config-GUI row (`gui/chest/ConfigMenu`) + per-mode lore deferred (kept this slice minimal/testable).
- Tolerance 12.0 is a sensible default, NOT the old's exact value (unknown) — tune if too strong/weak.

**Test:** `/cb config background edges` (or `closed`) → create/retexture a block with a solid-colour
background → that background should be solid black on the block; `none` → texture used as-is. Per the
Golden Rule, NOT done until confirmed in-game.

---

## Group 06 — Handoff fixes: Omni air-click, prefix, lore, Magic GUI · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** All 4 handoff items confirmed by the developer.
Build green (compile + all 3 gates); jar copied to `.minecraft\mods\`.

**Done + confirmed:**
- **Omni-Tool sneak-click now works in the air** — `item/OmniToolItem.java`: added a
  `use(World, PlayerEntity, Hand)` override. Sneaking opens the mode GUI in the air / on any
  non-custom block; `useOnBlock` still handles custom blocks (returns SUCCESS, so no double-fire).
- **[CB] prefix unified** to the black-bracket chat format everywhere — `command/Chat.java`
  (`HUD_PREFIX` removed, `Chat.tool()` → `PREFIX`) + `gui/chest/CbChestHandler.java`. Developer's
  accepted trade: black brackets are dim on the dark hotbar bar.
- **Item lore humanized** for every tool/marker — `OmniToolItem`, `DeleterItem`,
  `RainbowRectangleItem`, `ShapeMarkerItem` (all 8 markers), plus new tooltips on `LuminaBrushItem`
  + `ChiselItem` (had none).
- **Magic Items GUI → double chest** — `gui/chest/MagicMenu.java` + `core/MagicItemsManager.java`:
  6 rows, light-blue frame, 3 hand tools on the top interior row, 8 markers in a 4-colour grid
  (each colour's Square directly above its Triangle), header + "Marker Shapes" label + controls;
  enabled items glint, disabled dimmed. 8 markers seeded with enable/disable persistence.

**⚠️ Item lore = PARTIAL (polish later).** Confirmed readable in-game, but marked **partial** —
same as the GUIs — for a dedicated lore polish pass later: tone consistency across all items, more
per-item flavour, and the Shape / Rainbow lore likely wants tightening once their real mechanics
(M2–M4) land. Treat any further lore work as a later pass, not a regression.

**Tab placement:** developer chose to keep two separate tabs. Fabric exposes no API to pin a custom
tab's page or force adjacency, so no tab code changed (registration already orders Blocks→Tools).

---

## Group 06 — Tools: safe fixes pass (textures/lore/GUI/tab) · 2026-06-10

**Status:** Safe fixes written, build green. **Not tested in-game.** Remaining mechanics queued
as careful individual builds (see below).

**Done this pass (low-risk, deterministic):**
- Restored the **old Deleter + Rainbow Rectangle textures** (copied from the old project).
- Redrew the **Omni-Tool texture** — clearer: steel/gold tool head on a rainbow handle, bold
  outline, readable from a distance.
- Simplified **Omni-Tool + Deleter lore** (short, plain words). Shape/Rainbow lore deferred to
  when their real mechanics land (so it isn't written twice).
- **Magic Items menu** seed + mapping updated (lumina_brush/chisel → Omni-Tool / Rainbow
  Rectangle / Deleter). Tools tab icon switched off the removed Lumina Brush → Omni-Tool.
- Confirmed creative tabs register Blocks→Tools (Tools should sit right after Blocks).

**Queued mechanics (need their own careful builds — clean-room rebuild lacks the systems):**
`ImageProcessor` only resizes (no background removal yet); `TextureStore` is one PNG per block
(no per-face / no colour variants). So:
- **M1** CIE-LAB background remover + 3-mode config (foundation).
- **M2** Triangle → fill background with the colour (needs M1).
- **M3** Square colour-variant + Black-square fallback (`blockID_color`, else original).
- **M4** Rainbow Rectangle per-face URL paint (per-face textures — biggest).

---

## Group 06 — Tools (Omni-Tool + shapes + textures) · 2026-06-10

**Status:** Code written, build green (compile + all 3 gates), all 11 textures + models packaged in
the jar. **Not yet tested in-game.** Testing guide: `Reports/GROUP_06_TESTING_GUIDE.md`
(tests G06.2a–h + G06.A–D).

**Corrected Omni composition (developer fix):** Omni-Tool = **Brush (Glow) + Chisel (Hardness) +
Rainbow Rectangle (Area)**. The earlier Eyedrop/Diamond-Triangle mode was a scrapped concept and
was removed. Omni-Tool GUI marked **partial** (polish later).

**Restored tools (textures reused from old project, code from scratch):** 8 colour/shape markers
(Green/Yellow/Red/Black × Square/Triangle) + the Rainbow Rectangle. Deleter kept, new trash texture.

**Textures (`tools/gen_tool_textures.py`):** green/yellow/black square+triangle copied verbatim from
the old project; red_square/red_triangle reproduced with the old project's exact draw algorithm
(no red PNG was captured); new sprites drawn for `rainbow_rectangle`, `deleter` (trash bin) and
`omni_tool` (chisel head + brush bristles + rainbow base). 11 PNGs + 11 models + 9 lang entries.

**New files (5):** `item/OmniToolItem.java`, `core/OmniToolState.java`, `gui/chest/OmniMenu.java`,
`item/RainbowRectangleItem.java`, `item/ShapeMarkerItem.java`, `core/AreaSelection.java` (per-player
two-corner select shared by Rainbow Rectangle + Omni Area). Plus `tools/gen_tool_textures.py`.

**Edited files:** `item/ToolItems.java` (register omni + rainbow + 8 shapes), `ToolCommands.java`
(brush→Omni[Glow], chisel→Omni[Hardness], `/cb omni`, `/cb rectangle`; deleter unchanged + lore),
`CustomBlocksMod.java` (tools tab = Omni, Rainbow Rectangle, Deleter, 8 shapes), `Nav.java` +
`GuiRouter.java` (OMNI dest), `HelpTopics.java` (Tools category), `item/DeleterItem.java` (lore),
plus the 11 item models repointed to `customblocks:item/*` and lang entries.

**Decisions:** sneak+right-click = open config GUI (so glow/hardness cycle forward-only); Area =
self-contained two-corner selector (bulk ops that consume it come later); shape markers are simple
taggers for now; Omni mode shown in item name (no per-mode texture swap yet).

**Remaining:** Step 3 = held-block dynamic glow (networked client light — highest risk, isolated).

---

## Group 05 — Silent Resource Pack Delivery · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** G05.1–G05.6 all pass. Follow-up polish:
texture-applied message is now silent-pack aware (no "accept the prompt" text when silent).
Testing guide: `Reports/GROUP_05_TESTING_GUIDE.md`.

**What it fixes (deep-search findings → fixes):**

| Finding | Before | Fix |
|---|---|---|
| 17.1 | Vanilla "download resource pack?" dialog appeared (`required=false` + prompt) | Client mixin on `ClientCommonNetworkHandler.createConfirmServerResourcePackScreen` takes the vanilla silent add path (`addResourcePack`) instead of building the prompt |
| §E | No `silentPack` config | Field (default true) + `/cb config silentpack` + Config-GUI row; synced to clients via `SilentPackPayload` on join + on change |
| scope | — | Client only silent after OUR server says so; resets on disconnect → never auto-accepts other servers' packs. Forced (`required=true`) packs left to vanilla |
| §debounce | `updatePack` collapsed only concurrent builds | ~500ms time-debounce; rapid edits → one rebuild; added `Rebuilding resource pack…` log line |

**New files (3):** `mixin/ClientCommonNetworkHandlerMixin.java`, `client/SilentPackState.java`,
`network/payloads/SilentPackPayload.java`.

**Edited files (7):** `customblocks.mixins.json` (client mixin), `CustomBlocksConfig.java`
(silentPack), `CustomBlocksMod.java` (register + send on join), `CustomBlocksClient.java`
(receiver + disconnect reset), `ResourcePackServer.java` (debounce + log line),
`ConfigCommands.java` (`/cb config silentpack`), `ConfigMenu.java` (Silent Pack row).

---

## Group 04 — Chat Messages & Command Communication · 2026-06-10

**Status:** ✅ **DONE — verified in-game (2026-06-10).** Brand kept (`[CB]` + ✔/✖), wording
upgraded. Testing guide: `Reports/GROUP_04_TESTING_GUIDE.md`.

**What it fixes (deep-search findings → fixes):**

| Finding | Before | Fix |
|---|---|---|
| 17.2 | Terse message bodies (`Created g04b (slot 12)`, `'g04a' is locked`) | Kept `[CB]` tag + ✔/✖ glyphs (the brand); upgraded the wording — bodies rewritten as clear, helpful sentences |
| Q5 | DidYouMean missing entirely (raw Brigadier error on typos) | `DidYouMean.java` — Levenshtein + prefix boost, fallback arg named `subcommand` (LANG1-safe), modes smart/always/off |
| Q5 | `/cb help` missing entirely | Chest GUI browser (9 categories → commands → click pre-fills chat via new `ChatPrefillPayload`) |
| — | `/cb welcome` missing | Added with 3 clickable quick-start actions |
| — | `IncidentRecorder` had zero callers — incidents log could never fill | Wired: texture download failures, import failures, pack rebuild failures |
| — | No `didYouMean` config | Field + `/cb config didyoumean` + click-to-cycle row in the Config chest GUI |

**New files (5):** `command/DidYouMean.java`, `command/handlers/HelpCommands.java`,
`gui/chest/HelpTopics.java`, `gui/chest/HelpMenu.java`, `network/payloads/ChatPrefillPayload.java`.

**Edited files (13):** `Chat.java` (prefix/glyphs removed), `CustomBlocksConfig.java` (didYouMean),
`CommandRegistrar.java` (HelpCommands + fallback last), `ConfigCommands.java` (didyoumean cmd),
`ConfigMenu.java` (cycle row), `CreationCommands.java`, `AttributeCommands.java`,
`UtilityCommands.java`, `ManagementCommands.java`, `TemplateCommands.java`, `VideoCommands.java`,
`ChestGuiCommands.java`, `GuiCommands.java` (message tone), plus `ResourcePackServer.java`
(incident on pack failure), `Nav.java`/`GuiRouter.java` (HELP dest), `CustomBlocksMod.java`/
`CustomBlocksClient.java` (payload registration).

---

## Group 02 — Chest GUI Core Infrastructure · 2026-06-09

**Status:** Code written and statically verified. **Not yet compiled or tested in-game.**

**What it fixes (audit rows / issues closed):**

| Source | Item | Before | Fix |
|---|---|---|---|
| 17.3 | Chest GUI replaced by screens | Screen-based main menu | Server-side chest GUI system; screens removed |
| 17.5 | `/cb editor <id>` shortcut | Missing (only `/cb gui block <id>`) | `/cb editor <id>` added as a direct alias |
| G01 row 41 | `gui` | Screen-based | Opens chest dashboard |
| G01 row 42 | `menu` / `dashboard` | Chest menu missing | Open the chest dashboard (aliases of `/cb`) |
| G01 row 43 | `editor` | Missing | `/cb editor <id>` added |
| G01 row 44 | `listgui` | Missing | Added — chat list of GUIs with clickable `[open]` |
| G01 row 45 | `blocks` (block list GUI) | Chat `list` only | Block list chest GUI added |
| G01 row 46 | `redogui` | Missing | Visual redo browser added |
| G01 row 47 | `undogui` | Missing | Visual undo browser added |
| G01 row 139 | `unsuppress` | Missing | Added |
| G01 row 145 | `history` GUI | Chat undo/redo only | Mutation-log history chest GUI added |
| G01 rows 151/152 | `magicitems` / `editmagicitems` | Missing (open question) | Implemented per the Group 02 spec |
| Group L | `rp pause` / `rp resume` / `sync` | Missing | Added resource-pack pause/resume + force-sync |

**New files (15):**

*Framework — `gui/chest/` (6):*
- `Icons.java` — stained-glass filler panes + item-icon builder (name / lore / glint via `DataComponentTypes`).
- `Nav.java` — per-player back-stack; `Dest` enum (MAIN, BLOCK_LIST, EDITOR, UNDO, REDO, HISTORY, MAGIC, MAGIC_EDIT) + `MenuKey(dest, arg, page)` record.
- `ChestMenu.java` — generic 1–6 row chest container with per-slot click callbacks.
- `CbChestHandler.java` — screen-handler click router (mirrors the proven old `CbScreenHandler`).
- `GuiRouter.java` — open / navigate / repage / back / render, and command delegation (`runCommand`, `runAndReopen`, `promptCommand`, `confirmCommand`) via `executeWithPrefix`.
- `Layout.java` — shared paginated footer (back / prev / page / next / close).

*Menus — `gui/chest/` (6):*
- `MainMenu.java` — the `/cb` dashboard (Block List, Categories, Templates, Macros, Arabic, Diagnostics, Config, Close).
- `BlockListMenu.java` — paginated block list; click a block to open its editor.
- `EditorMenu.java` — per-block editor; slots delegate to give / setglow / sethardness / setsound / setcollision / setcategory / rename / retexture / note / delete.
- `UndoMenu.java` — visual undo/redo browser (click an entry to step to that point).
- `HistoryMenu.java` — server mutation-log view (timestamp / player / action / block ID).
- `MagicMenu.java` — magic-items list and edit mode.

*Core + command (3):*
- `core/MutationLog.java` — in-memory audit log feeding the history GUI.
- `core/MagicItemsManager.java` — magic-item registry + enabled-state.
- `command/handlers/ChestGuiCommands.java` — registers all Group 02 commands.

**Edited files (5):**
- `command/CommandRegistrar.java` — register `ChestGuiCommands` after `GuiCommands`.
- `command/handlers/GuiCommands.java` — removed the old screen `gui` / `admingui` registrations (kept `edithud`).
- `core/UndoManager.java` — added `undoStack(uuid)` / `redoStack(uuid)` reads + a `MutationLog` hook in `push()`.
- `command/handlers/HistoryCommands.java` — added public `undoOnce` / `redoOnce` for the visual menus.
- `network/ResourcePackServer.java` — added `pause` / `resume` / `isPaused` / `syncToAll` / `unsuppress`; `updatePack()` no-ops while paused, `sendToPlayer()` no-ops while suppressed.

**Design note:** every mutating editor action delegates to the existing, tested `/cb` command rather
than re-implementing block logic — so locking, undo recording, dynamic lighting, and chat feedback
all behave identically to typing the command. Editor argument formats were verified against
`AttributeCommands`.

**Verified (static only):**
- File-size gate (§9.3): all files within limits (largest handler `ChestGuiCommands`, 156 / 400 lines).
- Mojibake gate + sound gate: 0 hits.
- Command-literal collision scan: no duplicate literals introduced.
- API cross-check against real source: `SlotManager`, `SlotData`, `SlotBlock`, `ToolItems`, `Chat`,
  `executeWithPrefix`, `DataComponentTypes`, `ClickEvent` / `HoverEvent` constructors — all confirmed.
- Covers tests G02.1–G02.11 (see `GROUP_02_TESTING_GUIDE.md`).

**Known limitations / not verified:**
- Not compiled or run where authored — compile errors remain possible until `gradlew build` on the dev PC.
- `/cb history` does not yet implement the optional shift-click “filter by player” (spec §6).
- `GuiCommands.openGui` is now unused (javac warning only, not an error).
- Golden Rule: none of this is DONE until confirmed in-game.

---

## Group 01 — Legacy Feature Audit · 2026-06-09

**No code fixes** — this group is an audit only. It produced `Reports/GROUP_01_AUDIT_REPORT.md`
(153-row command audit, GUI-workflow gap list, and config-field coverage). Developer sign-off (Test
G01.4) granted, unblocking Group 02. Listed here for completeness; nothing was changed in code.

---

_Groups 03–25: not started — no fixes implemented yet._


---

## Finale Fix — Group 02 reported issues (12 fixes) — IMPLEMENTED (UNCOMPILED, pending in-game test)

### 1. Command registration & logic
- **`/cb` now works.** `CommandRegistrar` registers the `cb` alias with `.redirect(node).executes(ChestGuiCommands::openDashboard)`. A bare redirect does not inherit the target's `executes`, so `/cb` alone used to fail; now it opens the dashboard. Legal because the `cb` node has no children of its own.
- **`/cb gui block <id>` removed.** Only `/cb gui` (dashboard alias) and `/cb editor <id>` remain.
- **`/cb unsuppress` description clarified** — now explains it re-enables the server resource-pack download prompt that a player's "do not show again" had silenced.

### 2. GUI aesthetics
- **Glass-pane framing** via new `Icons.accent()` (light-blue pane). Applied to Main Menu, Editor, Magic Items, and the new Confirm menu.
- **Main Menu rebuilt** as a 6-row "double chest" dashboard (`CustomBlocks Dashboard`) with header + framed rows (was a small 3-row menu).
- **Editor** title fixed to `CustomBlocks Editor - ID: <id>` (was `Edit: <id>`) + accent frame. (Visual shape editor wiring deferred per request.)
- **Magic Items** shrunk from 6 rows to a single row, items centred between Back/controls.

### 3. UX & functionality
- **Cursor reset bug fixed.** `GuiRouter` now refreshes the open screen IN PLACE (`CbChestHandler.refreshWith` + `syncState`) when the new menu has the same rows + title (toggles, paging), instead of reopening (which snapped the cursor to centre). Reopen only happens when navigating to a different screen. Mirrors the old build's `refreshScreen`/`refreshWith` pattern.
- **`/cb history` entries clickable** — clicking opens that block's editor (or notes it no longer exists).
- **`/cb config` confirmation GUI restored** — new `ConfirmMenu.configWarning` (Yes/No) opens before the real config, mirroring the old `openConfigWarningGui` (No → back, Yes → runs config).

### 4. Text / lore / chat
- **Prefix unified** to `Chat.PREFIX` / `Chat.HUD_PREFIX` across GUIs and RP commands (removed stray `[CustomBlocks]` and `§b[CB]` player-facing strings; logger lines unchanged).
- **Undo/Redo/History lore enriched** — friendly action names (e.g. "Changed glow"), block id, position, and an accurate "undo/redo the last N change(s)" hint instead of bare labels like "delete 10".

### Files touched
- `command/CommandRegistrar.java`, `command/handlers/ChestGuiCommands.java`
- `gui/chest/`: `ChestMenu`, `CbChestHandler`, `GuiRouter`, `Nav`, `Icons`, `EditorMenu`, `UndoMenu`, `HistoryMenu`, `MainMenu` (rewrite), `MagicMenu` (rewrite), `ConfirmMenu` (new)

### Status
Static checks pass (no leftover prefixes, no mojibake, all under size gates, braces balanced). NOT compiled (no build in sandbox). Must be verified in-game before being marked done.

### Note on scope
The "restore all old wired chest GUIs" deep-search request was scoped down to the **config confirmation** example only (the one concrete old wired GUI identified: `openConfigWarningGui`). Other old GUIs were inventoried in the testing guide appendix but not mass-recreated.
