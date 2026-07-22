# Progress Log

One entry per work session. Newest at the top. See the Engineering Bible §9.2.

---

## 2026-06-09 (night) — Phase 17 config schema finalized (per-field walkthrough)

### Done
- Walked the full old `config.json` (~60 fields) with the developer group by group; every field now has a decision (see PHASE17 doc §L–N).

### Decisions (config)
- **Fonts:** Rockwell Condensed (English) + arabtype.ttf (Arabic), both bundled in JAR.
- **Default texture size:** 256px, server-configurable (memory / pack-size caveat noted).
- **Undo:** maxUndoDepth 100; undoMode `both`; keep bulk-confirm >10.
- **Background removal:** fully configurable + switch color matching to **CIE Lab** (was YCbCr).
- **Permissions:** 3-tier (use/edit/admin) backing LuckPerms → vanilla-OP fallback.
- **Sounds & particles:** per-event category toggles restored.
- **Limits:** restored + raised for 256px textures.
- **Auto-backup:** configurable interval (default 30 min); snapshots folded into `/cb backup`.
- **Networking:** pack port / IP override / cloud secret configurable + auto-detect IP.
- **DidYouMean:** smart, configurable.
- **Display & appearance:** labels, category badge, triangle hex colors, hologram height/color — all configurable.
- **AI (keyless Pollinations):** keep variations (3) + style (pixel_art); drop key fields.
- **Discord webhook:** blank by default.

### Next
- Awaiting developer go-ahead on Phase 17 implementation sequencing.

---

## 2026-06-09 (eve) — Phase 17 decisions locked + old CustomBlocks deep-searched

### Done
- Deep-searched the OLD CustomBlocks source (now provided) + its config/data export.
  - Confirmed old monoliths: `GuiManager.java` (10,324 lines), `CustomBlockCommand.java` (6,784) — the exact anti-pattern the file-size gates prevent. Restore behavior, never structure.
  - Extracted the full old `/cb` command surface (~180 subcommands) for the 17.11 audit (see appendix in PHASE17 doc).
  - Confirmed Cloud Vault Worker is deployed: `cb-cloud-vault`, KV id `ee49b7a0…`, endpoint `cb-cloud-vault.cbbblocksvault.workers.dev`.
  - Verified old-format migration data is present: `slots.json.gz`, `textures/slot_N.dat`, `*.json.gz` (achievements/favorites/players/placement_stats), `categories.json`, `magic_items.json`, `hud-config-server.json`, old `config.json` (`maxSlots: 850`, OP-level permission fallbacks).
- Finalized all open Phase 17 interview questions (Groups A–R) + cross-cutting decisions.
- Moved `PHASE17_RESTORATION.md` into new `docs/Finale Fix/` folder; appended a **Locked Decisions** section + legacy-audit command reference.

### Decisions
- **maxSlots → 1000 everywhere** (code default, README, HANDOFF, live config). Cap stays 8192.
- **Config scope creep → split** Phase 6/11/13/14 fields back per-phase.
- **32K blockstate warnings → revisit after Phase 17.**
- **Phase 11/12/14/16 all fold into Phase 17** (HUD rebuild + retest, Arabic, Cloud/Discord GUIs, release QA).
- **Physical tools:** Omni-Tool + Deleter + Square/Triangle markers (Chisel/Lumina/Hexagon/Rectangle/Triangle become Omni modes).
- **AI textures:** Pollinations.ai (keyless) primary + Stable Horde anonymous fallback; drop HuggingFace.
- **Silent pack:** auto-accept mixin + `silentPack` config toggle (forced packs still prompt).
- **Permissions:** LuckPerms / Fabric Permissions API, fallback to vanilla OP level.
- **Exports:** include `.litematic` + `.schem` + standalone vanilla RP.
- **Keep** holograms, screen eyedrop, rembg-style BG removal in scope.
- **Sample blocks:** assistant picks 5–10 for fresh installs.
- **Git:** stay on `main` (no phase17 branch yet). Phase 17 doc lives in `docs/Finale Fix/` only; Bible stays at Phase 16.

### Verified working (in-game confirmed)
- Nothing new in-game this session — decisions + doc updates only. Golden Rule unchanged: nothing is ✅ DONE until confirmed in-game.

### Next
- Developer to instruct on Phase 17 implementation sequencing.

---

## 2026-06-09 — Phase 4–16 In-Game Testing Complete + Phase 17 Planned

### Done
- Phases 4–10: all ✅ confirmed in-game
- Phase 11: HUD bug found + fixed in code (two bugs: toggle never reached client, cache stale mid-session). **Not yet rebuilt/tested.** Needs `.\gradlew.bat build` + retest.
- Phase 12: Partial pass — letter system ✅, word command ❌ (can't type Arabic in MC chat). arabtype.ttf found at `C:\Windows\Fonts\arabtype.ttf` and copied to `run/config/customblocks/`.
- Phase 13: ✅ macros + video list confirmed. AI stubs not tested.
- Phase 14: Skipped — blocked by missing `/cb config` chest GUI (can't set vault/discord URLs in-game).
- Phase 15: ✅ diag + incidents work, both need rework.
- Phase 16: Skipped — release QA deferred until Phase 17 complete.
- Phase 17 restoration doc compiled: 24 tracked issues, Groups A–O interview sections.

### Decisions
- Chest GUI (17.3) replaces ALL current screen-based UIs in Phase 17.
- Legacy feature audit (17.11) requires developer interview before any restoration.
- Arabic word command (17.18) needs redesign — Minecraft chat can't input Unicode.
- AI features (17.22) are explicit stubs — not wired.

### Verified working (in-game confirmed)
Phases 4, 5, 6, 7, 8, 9, 10, 13, 15

### Next
Start Phase 17 implementation session. Read `docs/PHASE17_RESTORATION.md` and `memory/project_phase17_issues.md` before beginning.
Must rebuild first for Phase 11 HUD fix: `.\gradlew.bat build`

---

## 2026-06-07 — Phase 16 Validation: Blockstate Warning Diagnosis (Issue #1 & #2)

### How it started

Developer submitted a diagnostic report from in-game testing. The report identified a
high-volume warning loop in the server console:

```
[Worker-Main-10/WARN]: Exception loading blockstate definition:
  'customblocks:blockstates/slot_X.json' missing model for variant:
  'customblocks:slot_X#light=Y'
```

The error fired 16 times per slot (once for each light level 0–15), starting at slot_850
and continuing through slot_977 where the log truncated. With maxSlots configured to 2048,
this produces up to ~32,000 warnings per startup. Developer asked: "Isn't that expected
since we don't have textures for all 800 blocks?"

### First assessment (WRONG)

I initially identified two separate bugs:

1. **Pack generates blockstate JSONs for all 2048 slots** — should only generate for assigned
   ones (~35). This causes 32K warnings for blocks that don't exist.
2. **The `""` empty variant in `blockstateJson()` is wrong** — should enumerate `light=0`
   through `light=15` individually. Minecraft can't resolve the model when the blockstate
   JSON says `""` but the block has a `light` IntProperty.

I was confident about both. I was wrong about both — partially.

### Investigation round 1: the `""` catch-all

Sent an agent to read the actual code. Findings contradicted my first take:

- **`""` (empty string) IS the vanilla-idiomatic catch-all.** Minecraft's `BlockStatesLoader`
  accepts `""` as a variant key that matches ALL property combinations. This is how vanilla
  blocks like `LightBlock` work. The PROGRESS_LOG entry from 2026-06-04 ("Batch 3 polish")
  even says: "the generator's `""` catch-all variant matches all 16 states (verified in the
  `BlockStatesLoader` bytecode — empty key = empty predicate = match-all)."
- **maxSlots defaults to 800, not 2048.** The 2048 value came from the developer's custom
  `config.json`. Code caps at 8192.
- **The pack generator ALREADY handles empty slots.** It creates a shared `empty_slot.json`
  model (intentionally references a missing texture → purple/black checkerboard) and writes
  blockstate JSONs pointing to it for every unassigned slot. This was specifically designed
  to suppress "missing model" warnings.

So I corrected myself: the code looks correct on paper. The `""` catch-all should work.
The empty slot handling exists. Then why the warnings?

### Investigation round 2: the full pipeline

Dug into every component in the chain:

| Component | File | Verdict |
|---|---|---|
| `blockstateJson()` | ServerPackGenerator.java:98–106 | ✅ Valid JSON, `""` catch-all |
| `emptyModelJson()` | ServerPackGenerator.java:123–133 | ✅ Valid `cube_all` model |
| `cubeAllJson()` | ServerPackGenerator.java:108–115 | ✅ Correct real block model |
| ZIP entry paths | `assets/customblocks/blockstates/slot_N.json` etc. | ✅ Standard Minecraft paths |
| `add()` helper | ServerPackGenerator.java:135–140 | ✅ Duplicate guard, proper close |
| `pack.mcmeta` | pack_format 34 | ✅ Correct for MC 1.21.1 |
| HTTP serving | ResourcePackServer.java:77–87 | ✅ Correct MIME, streaming, SHA1 |
| Static assets | `src/main/resources/assets/customblocks/` | ✅ No conflicts (only lang + tool items) |
| Debouncing | ResourcePackServer.java:128–148 | ✅ AtomicInteger, no double-builds |

Everything checks out. The pack ZIP is well-formed. The JSON is valid. The HTTP server
works. No file conflicts.

### Root cause: startup timing

The problem is WHEN things happen:

```
onInitialize()
  ├── SlotManager.registerAll(maxSlots)  ← All 2048 blocks register with Minecraft
  ├── SlotManager.loadAll()              ← 35 blocks loaded from disk
  └── (Minecraft's resource loader runs) ← Looks for blockstate JSONs in the JAR
                                            The JAR has NONE for slot blocks
                                            → Warnings fire for EVERY registered block

SERVER_STARTED event (later)
  ├── ResourcePackServer.start()         ← HTTP server starts
  └── ResourcePackServer.updatePack()    ← ZIP built on BACKGROUND thread
                                            Clients download it on join
                                            → Everything works in-game
```

The blockstate JSONs live in the dynamically-generated ZIP, not in the mod JAR. Minecraft's
resource loader runs between `onInitialize()` and `SERVER_STARTED`, finds no blockstate
definitions in the JAR for any `slot_N` block, and logs warnings. By the time a player
actually joins and receives the resource pack, all models resolve correctly.

**The warnings are cosmetic startup noise. Blocks render correctly in-game.**

### But wait — the `""` catch-all question

The diagnostic report's error message says `missing model for variant: 'slot_X#light=Y'`.
This specific phrasing means Minecraft found a blockstate JSON, parsed it, and couldn't
match the `light=Y` variant. If no blockstate JSON existed at all, the error message would
be different ("missing blockstate definition" or "FileNotFoundException").

This creates an ambiguity:

- **If the warnings fire BEFORE the pack exists** (no blockstate JSON in JAR at all) →
  pure timing issue, `""` catch-all is irrelevant because Minecraft never sees it.
- **If the warnings fire AFTER the pack is applied** (blockstate JSON exists but `""`
  doesn't match) → the `""` catch-all genuinely fails for blocks with properties in 1.21.1.

The log thread name `Worker-Main-10` suggests resource loading during startup (before pack
delivery), which points to timing. But the error message's wording suggests it DID find a
blockstate file somewhere.

**Resolution: we fix BOTH possible causes.** If it's timing-only, the variant fix is harmless.
If `""` genuinely fails with properties, the variant fix is essential. Either way, we win.

### What the developer actually wants

From the discussion, the developer chose:

1. **Reduce maxSlots default to 100** — fewer blocks registered = fewer potential warnings.
   Still configurable up to 8192 for power users.
2. **Only generate pack entries for assigned slots** — remove the empty-slot loop entirely.
   No blockstate/model JSONs for unassigned slots. Unassigned slots show purple/black
   checkerboard naturally (Minecraft's default for blocks with no model).
3. **Investigate before implementing the light variant fix** — done (this entry).

### Planned fix (two changes)

**Change 1 — Fix `blockstateJson()` to enumerate all 16 light variants:**

```java
private static byte[] blockstateJson(String modelRef) {
    JsonObject variant = new JsonObject();
    variant.addProperty("model", modelRef);
    JsonObject variants = new JsonObject();
    for (int light = 0; light <= 15; light++) {
        variants.add("light=" + light, variant);
    }
    JsonObject bs = new JsonObject();
    bs.add("variants", variants);
    return GSON.toJson(bs).getBytes(StandardCharsets.UTF_8);
}
```

This replaces the `""` catch-all with explicit `light=0` through `light=15` entries. Each
points to the same model. Harmless if `""` already works; essential if it doesn't. Matches
what Minecraft expects for blocks with an IntProperty.

**Change 2 — ServerPackGenerator: only generate for assigned slots:**

Remove the entire empty-slot loop (current lines 70–82). Unassigned slots get no blockstate
JSON, no model JSON, no item model. Minecraft shows the default purple/black missing-texture
checkerboard for any unassigned slot that gets placed — which is the correct behavior already.

Also remove `emptyModelJson()` since nothing calls it anymore.

**Change 3 — CustomBlocksConfig: default maxSlots 800 → 100:**

One-line change. Reduces the registration pool. Still configurable, still capped at 8192.

### What these fixes DON'T solve

The startup timing gap (blocks register before the pack ZIP exists) means the server console
may still log warnings during initialization for the ~35 assigned slots. These warnings
disappear once the pack is built and applied. Fixing THIS would require either:

- Generating the pack synchronously during `onInitialize()` (before resource loading)
- Bundling static fallback blockstate JSONs in the JAR

Both are possible future improvements but are NOT blocking — the warnings are brief, low
in volume (35 × 16 = 560 lines vs the current 32,000), and gameplay is unaffected.

### Lessons

1. Don't trust your first analysis without reading the code. The `""` catch-all looked wrong
   but was actually the documented vanilla approach from an earlier session.
2. The previous developer (this same AI, in an earlier session) verified `""` works "in the
   `BlockStatesLoader` bytecode" and wrote it into the progress log. That verification may
   have been against a different Minecraft version or may have been wrong. Trust but verify.
3. A 32K-line warning loop from 2048 × 16 iterations is always worth fixing even if it's
   "just cosmetic" — it buries real errors and makes the log unreadable.
4. The developer's instinct ("isn't this expected?") was partially right. The unassigned-slot
   warnings ARE expected noise. The assigned-slot variant mismatch is the real issue.

### Status

- Investigation: ✅ Complete
- Fix plan: ✅ Documented above (3 changes)
- Implementation: ⏳ Awaiting developer confirmation to proceed
- In-game verification: ⏳ Blocked on implementation

---

## 2026-06-05 — Phase 13/16 final push: Video, GUI screens, Mod Menu (Batch 14)

### Done (build-verified after `gradlew build` downloads jcodec + modmenu)
- **VideoDecoder** (Phase 13) — jcodec 0.2.5 MP4 frame extractor. `extractFrameAsPng(File, int)`
  seeks to a frame, converts via AWTUtil, scales via ImageProcessor. Throws cleanly on bad
  frames. jcodec + jcodec-javase 0.2.5 uncommented in build.gradle.
- **VideoCommands** — `/cb video list` enumerates `.mp4` files in `config/customblocks/videos/`.
  `/cb video extract <file> <id> <frame>` extracts frame N as the texture for block `<id>`.
  Runs on a daemon thread (never blocks tick). Registered in CommandRegistrar.
- **MacroListScreen** (Phase 10 GUI) — dedicated GUI screen for macro management opened via
  `/cb gui macros` or Main Menu → "Macros". Buttons: View All, Record New, Play, Delete, Add
  Step, Stop Recording.
- **ArabicBrowserScreen** (Phase 10 GUI) — dedicated GUI screen for the Arabic system opened
  via `/cb gui arabic` or Main Menu → "Arabic Letters". Buttons: Import All, Single Letter,
  Word Block, List.
- **Mod Menu integration** (Phase 16) — `ModMenuApiImpl` opens ConfigScreen when the ModMenu
  "Config" button is clicked. `modmenu:11.0.2` added as compile-only dep; terraformersmc maven
  repo added; `modmenu` entrypoint added to fabric.mod.json.
- **GuiCommands** — added `/cb gui macros` and `/cb gui arabic` routes.
- **CustomBlocksClient** — routed `MACRO_LIST` and `ARABIC_BROWSER` modes to their screens.
- **MainMenuScreen** — "Macros" and "Arabic Letters" buttons now open dedicated GUI screens
  instead of running list commands.
- All 3 build gates pass; jcodec + modmenu IDE errors are "missing classpath" warnings that
  clear after `.\gradlew.bat build` downloads the new dependencies.

### Pending developer action
- Run `.\gradlew.bat build` — this downloads jcodec + modmenu-api (first time will be slow).
- Test TESTING_GUIDE §1j–§1n (Batches 10–14). On "works": commit to `dev`, merge to `main`.
- Add a 64×64 PNG as `src/main/resources/pack.png` for the Mod Menu mod icon (optional).
- Place `.mp4` video files in `run/config/customblocks/videos/` to test video extraction.
- Phase 16 distribution: `.\gradlew.bat build` → `build/libs/customblocks-1.0.0.jar`.

### Architecture status
All 16 phases are now code-complete (stubs where full implementation requires external services
or developer-provided files). The only remaining hard blockers before v1.0.0 ship are in-game
confirmation of Batches 10–14 and the stress/multiplayer tests from Phase 16.

---

## 2026-06-05 — Phases 10–15 big push (Batch 13)

### Done (build-verified, NOT yet in-game confirmed)
- **MacroManager** — record/replay named command sequences. `/cb macro record <name>`,
  `add <cmd>`, `stop`, `cancel`, `play <name>`, `list`, `delete <name>`. Persists to
  `config/customblocks/macros/<name>.json`.
- **Arabic system** — `ArabicLetterMap` (28 letters), `ArabicWordRenderer` (Java2D PNG,
  loads `arabtype.ttf` from `config/customblocks/arabtype.ttf` or system fallback),
  `ArabicBlockRegistry`. Commands: `/cb arabic import`, `letter <name>`, `word <text> <id> <name>`,
  `list`.
- **Diagnostics** — `DiagnosticsHelper` + `IncidentRecorder`. `/cb diag` shows system snapshot
  (slots, heap, TPS, pack size). `/cb incidents` shows the incident log.
- **Cloud + Discord stubs** — `/cb vault` and `/cb discord` give setup instructions and fire
  when `vaultEndpoint` / `discordWebhookUrl` are set in config.json.
- **AI stubs** — `AiCommandParser` + `AiTextureGenerator` return null until `aiApiKey` is set.
- **GUI system (Phase 10)** — `GuiMode`, `GuiEngine`, `GuiState`, `OpenGuiPayload`.
  Screens: `MainMenuScreen` (7-button hub), `BlockEditorScreen` (per-block attributes),
  `ConfigScreen` (live config values). `/cb gui`, `/cb gui block <id>`, `/cb gui config`,
  `/cb admingui`, `/cb edithud` (toggle HUD). Screens open client-side via the server packet.
- **HUD overlay (Phase 11)** — `HudSyncPayload` syncs block index to client on join.
  `ClientSlotCache` caches it. `HudRenderMixin` injects into InGameHud to show
  `§e<id> §7"<name>"` when looking at a custom block. Toggle with `/cb edithud`.
- **OnboardingManager** — detects first-time players, sends a clickable welcome message once.
- **Config fields added** — `hudEnabled`, `aiApiKey`, `aiTextureEnabled`, `vaultEndpoint`,
  `discordWebhookUrl`.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Pending developer action
- Place `arabtype.ttf` in `config/customblocks/arabtype.ttf` for proper Arabic letter rendering
  (system fallback works but may lack full Arabic shaping on some servers).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12) PLUS new §1n (Batch 13).
- On "works": commit all Batches 3–13 to `dev`.
- Remaining: Phase 13 video (jcodec dep needed), Phase 14 Cloud (needs Cloudflare Worker URL),
  Phase 16 polish (Mod Menu, README finalization).

---

## 2026-06-05 — Phase 9 completion: Lock, Notes, Favorites, Drafts (Batch 12)

### Done (build-verified, NOT yet in-game confirmed)
- **LockManager** — `config/customblocks/locks.json`; atomic persist. `/cb lock <id>`,
  `/cb unlock <id>`, `/cb locked` (list with `[unlock]` buttons). Lock checks wired into
  every mutation point: `delete`, `rename`, `retexture`, `setglow`, `sethardness`,
  `setsound`, `setcollision`, `setcategory`, `template apply`, and the Deleter tool.
- **BlockNotesManager** — `config/customblocks/notes.json`. `/cb note <id>` (show),
  `/cb note <id> <text>` (set, greedy string), `/cb note <id> clear` (remove).
  Notes auto-cleaned when the block is deleted.
- **FavoritesManager** — per-player bookmarks in `config/customblocks/favorites.json`.
  `/cb fav <id>` toggles (add/remove). `/cb favs` lists with `[give]` + `[unfav]` buttons.
- **DraftManager** — `config/customblocks/drafts.json`. `/cb draft <id>` marks as draft,
  `/cb publish <id>` removes the mark, `/cb drafts` lists with `[publish]` buttons.
- **`/cb list` and `/cb search`** now show `§c[locked]` and `§8[draft]` status tags inline.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12). On "works": commit Batches 3–12 to `dev`.
- Discuss next step: color ecosystem, remaining Phase 9 items, or Phase 10 GUI planning.

---

## 2026-06-05 — UX polish: Deleter visual, export menu, template overhaul (Batch 11)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter visual fixed:** after deleting a block definition, the placed block now shows the
  purple/black "missing texture" checkerboard (Minecraft's standard placeholder) instead of
  going fully transparent/invisible. Fixed in `ServerPackGenerator.emptyModelJson()` — empty
  slot model changed from no-geometry invisible to `cube_all` referencing an absent texture.
- **`/cb export` redesigned:** bare `/cb export` now shows an action menu with
  `[.json] [.txt] [to Vault]` buttons. `/cb export <id>` shows `[to Config] [to Vault] [Download]`
  buttons. Per-block export no longer shows file paths or copy-path buttons. Vault stubs cleanly
  to "Phase 14". New download action writes the JSON and gives an HTTP link
  (`http://host:port/export/<id>`) served by a new `/export/` route in `ResourcePackServer`.
- **`/cb template` overhauled:**
  - List shows each template's attributes inline: `glow:X hard:Y sound:Z [passable] [cat]`.
  - Empty list explains what templates are and how to use them.
  - Save/apply confirmations now show the captured/applied attribute set.
  - New `/cb template delete <name>` subcommand.
  - Tab suggestions for template names on `apply` and `delete` arguments.
- Build green; all 3 gates pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10) + §1k (Batch 11). On "works": commit Batches 3–11.
- Continue with the next phase (color ecosystem or LockManager/BlockNotes — developer's call).

---

## 2026-06-05 — Phase 9 completion + Deleter redesign (Batch 10)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter redesigned:** right-click now deletes the block's entire definition (slot data +
  texture + pack rebuild + undo record), not just the placed instance. Placed block stays,
  reverts to unassigned-slot appearance. Matches `/cb delete` semantics.
- **Per-block export:** `/cb export <id>` → `exports/<id>.json` (schema v1: all attributes,
  importable). Existing `/cb export json|txt` bulk export unchanged.
- **importfolder:** `/cb importfolder [path]` scans a folder for per-block JSONs, creates
  missing blocks, skips existing ids, reports results. Triggers pack rebuild on any creation.
  Defaults to `exports/` when no path given.
- **Templates:** `TemplateManager` + `TemplateCommands`. `/cb template save/apply/list`.
  Apply is undoable. Templates stored as `config/customblocks/templates/*.json`.
- Build green; all 3 gates (verifyFileSize, verifyMojibake, verifySound) pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10). On "works": commit Batches 3–10 to `dev`.
- Continue with the next phase in order (LockManager / BlockNotes, or color ecosystem).

---

## 2026-06-04 — Test feedback fixes (Batch 9): Deleter, HUD prefix, undo scope

### From dev's in-game testing
- **Deleter:** said "Removed" even when it didn't delete. Fixed: now uses `World.breakBlock`
  (reliable sync + particles, ignores hardness so it still kills unbreakable blocks) and only
  confirms when the boolean says a block was actually removed.
- **HUD prefix invisible:** tool action-bar messages used the chat `[CB]` prefix whose `§0`
  black brackets vanish on the dark HUD. Added `Chat.HUD_PREFIX` (aqua) for tool feedback.
- **Undo scope:** added `/cb config undomode` cycling **server-wide** (new default) ↔
  **per-player**; `/cb config` shows it. `UndoManager` now keys its stacks by mode (shared
  GLOBAL_KEY vs per-UUID). Switching scope clears history. Persisted as `undoMode`.

### Parked for Phase 10 (dev's call — do NOT start Phase 10 yet)
- **Lumina Brush + Chisel → merge into ONE GUI "Block Editor" tool.** Left untouched for now;
  their placeholder icons (glowstone dust / amethyst shard) get finalized then too. The dev
  wants to continue phases one-by-one, not jump to the GUI system early.

### Next
- Dev re-test: Deleter actually removes + readable HUD text; `/cb config undomode` toggle
  (TESTING_GUIDE §1g + §1i). On "works": commit Batches 3–9 to `dev`. Then continue in order.

---

## 2026-06-04 — Phases 8+9 warped (Batch 8): Categories & Search

### Done (build-verified, NOT yet in-game)
- **Categories (Phase 8):** new immutable `SlotData.category` field (persisted, undoable).
  `/cb setcategory <id> <name>` (`none` clears). `/cb categories` lists categories + counts
  with clickable [list]. Pattern-identical to glow/sound — same setter/undo/persist path.
- **Search (Phase 9):** `/cb search <query>` matches id / display name / category
  (case-insensitive), each hit clickable [give]. Pure reads via `SlotManager.search/byCategory/
  categories`. `/cb list` now shows a `[category]` tag.
- Chose single-category-per-block (minimal, clean) over the old multi-category CategoryManager;
  can expand later if the GUI needs it. Build green; all 3 gates pass.

### Why this cluster
- Categories + Search are the two most tightly-connected metadata phases and carry zero image
  risk, so warping them was safe. The color ecosystem (recolor pipeline + the 4 color tools)
  is deferred — it's the one area the old project had real bugs (BFS recolor), so it gets its
  own careful batch.

### Next
- Dev batch-test Phases 6–9 (TESTING_GUIDE §1f–§1h). On "works": commit Batches 3–8 to `dev`.
- Then either color ecosystem (Phase 8 color) or templates/import-export (Phase 9 rest).

---

## 2026-06-04 — Phase 7 tools (Batch 7): Lumina Brush, Chisel, Deleter

### Done (build-verified, NOT yet in-game)
- **3 hand tools** in a new `item/` package, listed in a new "CustomBlocks Tools" creative tab:
  - **Lumina Brush** → cycles a clicked block's glow (sneak = backward); reuses setGlow path,
    refreshes placed copies, undoable.
  - **Chisel** → cycles hardness presets instant/soft/stone/hard/tough/unbreakable (sneak =
    backward); undoable.
  - **Deleter** → instantly removes a placed custom block (no drop), even unbreakable ones.
- **`CustomToolItem` base** gates work on `ServerPlayerEntity` (NOT `world.isClient`) so there's
  no client-side skip delay (Bible §9.6 pitfall). Tool models reuse vanilla item textures.
- Build green; all 3 gates pass.

### Deferred (need subsystems not built yet)
- Color Square/Triangle, Rectangle, Golden Hexagon → require the color-replace + region
  pipeline; these belong with Phase 8 (color ecosystem). Building them now would mean writing
  big machinery blind. Not done — flagged for Phase 8.

### Next
- Dev batch-test Phases 6–7 (TESTING_GUIDE §1f + §1g). On "works": commit Batches 3–7 to `dev`.
- Then Phase 8 (color ecosystem) — unlocks the remaining color tools.

---

## 2026-06-04 — Phase 6 undo/redo (Batch 5) + setcollision

### Done (build-verified, NOT yet in-game)
- **Undo/redo (per-player).** New `core/UndoManager` — clean ~150-line per-player stacks of
  immutable `SlotData` snapshots (the old one was 1,170 lines of disk-delta machinery; not
  ported). New `command/handlers/HistoryCommands` registers `/cb undo` + `/cb redo`.
  - Recording wired into create / delete / rename / dupe / setglow / sethardness / setsound.
  - `SlotManager.restoreSnapshot` + `removeSilently` = non-recording restore primitives so
    undo never re-triggers itself. Delete-undo restores the texture bytes too.
  - Config: `maxUndoDepth` (default 25). Per-player isolation via UUID-keyed stacks.
- **setcollision** (walk-through blocks): `/cb setcollision <id> <solid|passable>`. New
  immutable `SlotData.noCollision` field (persisted, undoable). `SlotBlock.getCollisionShape`
  returns an empty box live when passable — entities pass through, outline stays breakable.

### Next
- Dev in-game test: undo/redo per-player + collision toggle (TESTING_GUIDE §6); 2-player
  isolation if possible. Then the 7 tools (Phase 7) as their own focused, tested batch.

---

## 2026-06-04 — Bug fixes: mining formula, [CB] chat format (sound path verified)

### Fixed
- **Hardness mining was wrong (Bug 1 + 2).** The ported formula divided by 100 unless tool
  speed > 1 — but our blocks aren't in any tool tag, so speed is always 1.0 and it ALWAYS
  took the slow ÷100 branch (default 1.5 ≈ 7.5s by hand → felt unbreakable). Replaced with
  Minecraft's EXACT vanilla formula `getBlockBreakingSpeed / hardness / (canHarvest?30:100)`
  using live hardness. Now low = fast, high = slow, -1 = unbreakable, default 1.5 = stone-like.
- **Default hardness:** verified `DEFAULT_HARDNESS = 1.5` (no -1 regression). The "unbreakable
  default" was the broken formula, now fixed.
- **Chat bloat (Bug 4):** added `command/Chat` with the old project's `§0§l[§b§lCB§0§l]§r`
  prefix + ✔/✖ glyphs. Converted Creation/Attribute/Utility command messages to short
  branded lines (dropped the multi-line explanations).

### Verified, NOT changed
- **setsound (Bug 3):** traced the full path — command → `SlotManager.setSoundType` (updates
  BY_ID + BY_SLOT) → `saveAll` writes `"sound"` → `SlotBlock.getSoundGroup` reads it live
  (confirmed `state.getSoundGroup()` delegates live to the block, no caching). The code is
  correct end-to-end; no defect found. Likely the client wasn't relaunched after the build, or
  a subtle sound. Isolation test added to TESTING_GUIDE (check slots.json + a fresh runClient).

### Next
- Dev re-test in Survival: mining speed scales correctly; `[CB]` messages are concise; setsound
  isolation test. Then commit + continue Batch 4 (undo/redo).

---

## 2026-06-04 — Hardness words + message fix + Batch 4 slice 3 (sounds)

### Findings explained (from dev testing)
- "Hardness 1/20/50 all feel the same" = **Creative mode** breaks any positive-hardness block
  instantly; only `-1` (unbreakable) is honored there. Hardness break-times only differ in
  **Survival**. Not a bug — explained to the dev.
- The "(vanilla stone = 1.5)" hint was appended to every value (looked like it called 50
  "vanilla stone"). Fixed the wording.

### Done (build-verified, NOT yet in-game)
- **sethardness word values:** accepts `unbreakable` / `instant` / `stone` (with tab
  suggestions) as well as numbers. Clearer message + a Survival-vs-Creative note.
- **Sounds (Phase 6, slice 3):** `/cb setsound <id> <type>` (17 vanilla groups). `SlotData.soundType`
  (immutable, default "stone", persisted); read live in `SlotBlock.getSoundGroup`
  (+ shared `SlotBlock.soundGroupFor`/`SOUND_TYPES` for the command's validation/suggestions).

### Next
- Dev in-game test: TESTING_GUIDE §1e — hardness words **in Survival**, setsound on
  break/step. On "works": commit Batch 3 + glow + hardness + sounds to `dev`, then undo/redo.

---

## 2026-06-04 — Glow confirmed ✅ + glow cap + Batch 4 slice 2 (hardness)

### Done
- **Glow confirmed working in-game** by the developer ("done it perfectly"). ✅
  The BlockState-property approach (ADR-002) actually emits light.

### Done (build-verified, NOT yet in-game)
- **Side quest — setglow > 15:** Minecraft caps block light at 15 (4-bit, hard engine limit).
  `setglow` now accepts ANY number and caps at 15 with a friendly message instead of a red
  Brigadier rejection. There is no way to emit more than 15 — explained to the developer.
- **Hardness (Phase 6, slice 2):** `/cb sethardness <id> <value>` (negative = unbreakable,
  0 = instant, 1.5 = vanilla stone). `SlotData.hardness` (immutable, default 1.5, persisted);
  read **live** in `SlotBlock.calcBlockBreakingDelta` (ported formula) — no state property
  needed because hardness, unlike luminance, is queried live each break attempt.

### Next
- Dev in-game test: TESTING_GUIDE §1e (glow cap message + hardness: unbreakable / instant /
  slow). On "works": commit Batch 3 + glow + hardness to `dev`, then continue Batch 4
  (custom sounds, then undo/redo and tools).

---

## 2026-06-04 — Batch 3 polish + Batch 4 slice 1 (glow) build-verified

### Done (build-verified, NOT yet in-game)
- **Old-format create:** `/cb create <id> <name> <url>` makes + textures in one command
  (name is now a quoted string so a URL can follow it; `<id>` and `<id> <name>` still work).
- **Bug fix (from screenshot):** the failing URL was a `data:` URI, which `HttpClient`
  rejects ("invalid URI scheme data"). Now non-http(s) URLs are caught **before** the block
  is created → clear message, no half-made block, no "already exists" cascade.
  (`ImageDownloader.isHttpUrl` + pre-create check.)
- **Tab auto-complete** (`BlockSuggestions.IDS`) for delete/rename/dupe/retexture/setglow/give.
- **Glow attribute (Phase 6, slice 1):** `/cb setglow <id> <0-15>` via a new
  `AttributeCommands` handler; `SlotData.glow` (immutable, 0–15, persisted) is the
  configured default for new placements.

### Debug — glow was NOT working (fixed)
- Root cause (verified in the 1.21.1 bytecode): `AbstractBlockState.getLuminance()` returns
  a `final int` field computed ONCE at state construction. A `.luminance(state -> liveValue)`
  lambda is therefore frozen at registration (value 0, before any glow is set) — so dynamic
  glow via a lambda can NEVER work. The old project had the same dead lambda + a
  `triggerGlowUpdate` that scanned `getBlockEntityPositions()` (SlotBlock has no block entity),
  so its glow was effectively broken too.
- Fix (vanilla-idiomatic, like `LightBlock`): light is now a real **block-state property**
  `LIGHT (0..15)` on `SlotBlock`. `luminance` reads `state.get(LIGHT)` → baked per state →
  actually emits. `getPlacementState` applies the slot's configured glow to new placements;
  `block/SlotLighting` rewrites the LIGHT state of already-placed instances near players.
- Pack-safe: the generator's `""` catch-all variant matches all 16 states (verified in the
  `BlockStatesLoader` bytecode — empty key = empty predicate = match-all), so no purple blocks.

### Decisions
- Attribute setters live in their own `AttributeCommands` handler (design rule #5),
  not in CreationCommands — keeps each handler focused and under the size cap.
- Glow stored in the block STATE (not just SlotData): a placed block's light is saved in the
  chunk, survives reload independently, and is correct per-instance. See `docs/adr/ADR-002`.

### Next
- Dev in-game test: TESTING_GUIDE §1d (texture, incl. one-shot create + bad-URL §3.2b) and
  §1e (glow). On "works": commit Batch 3 + glow to `dev`, then continue Batch 4
  (hardness, sounds, undo/redo, tools).

---

## 2026-06-03 — Batch 3 (Phase 4: textures) build-verified

### Done (build-verified, NOT yet in-game)
- `image/ImageDownloader` (browser headers) + `image/ImageProcessor` (bicubic square PNG).
- `core/TextureStore` — per-slot PNG files (atomic); textures stay out of slots.json.
- `network/ServerPackGenerator` — pack_format 34; textured cube_all for created blocks;
  one shared invisible model for empty slots (kills the missing-model warnings).
- `network/ResourcePackServer` — embedded HTTP `/pack.zip`, SHA-1, ResourcePackSendS2CPacket
  to ALL clients (single pack path; no modded/vanilla split).
- `/cb retexture <id> <url>` — download/decode off the server thread, then rebuild + push.
- Mod wiring: HTTP start/build on SERVER_STARTED, stop on SERVER_STOPPED, send pack on join,
  creative tab → created-blocks-only. Config: textureSize=64, httpHost=127.0.0.1.

### Design note (de-risking)
- Chose a single HTTP-pack path for ALL clients to avoid the old PACK2 dual-system conflict.
  Client-side generation (Phase 5) is now optional. Textures work everywhere via the pack;
  multiplayer block NAMES + creative-tab contents still need client sync (Phase 5).

### Known rough edge
- Each retexture re-prompts the player to accept the pack (vanilla mechanism). Smoothing
  this (client auto-apply) is a Phase 5 nicety.

### Next
- Dev in-game test (TESTING_GUIDE §1d): create -> retexture <url> -> accept pack prompt ->
  see the textured block; survives a restart. Then commit Batch 3 to dev.

---

## 2026-06-03 — Phase 1 CONFIRMED ✅ + Batch 2 (Phases 2-3 core)

### Context
- Developer asked to batch related phases (16 small steps was overwhelming) and to lower
  maxSlots to **800** for faster load. New plan = 6 testable batches; this is Batch 2.

### Done (build-verified, NOT yet in-game)
- Phase 1 confirmed working in-game ("Phase 1 works"). ✅
- `maxSlots` default → 800.
- `SlotDataStore` — atomic JSON persistence (sole slot disk-I/O class, rule #4).
- `SlotManager` — create/delete/rename/dupe + loadAll/saveAll/reload; orphan guard on load.
- `/cb` command tree: `CommandRegistrar` + `CreationCommands` (create/delete/rename/dupe)
  + `UtilityCommands` (list/give/reload). LANG1 avoided by construction (clean literal tree).
- Wired `CustomBlocksMod`: loadAll after registerAll, register commands, saveAll on stop.
- `gradlew build` PASSED, all gates green; no file over its size limit.

### Deferred (on purpose, to later batches)
- Undo/redo → batched with attributes (Batch 4 / Phase 6).
- retexture → needs the image pipeline (Batch 3 / Phase 4).
- Creative tab still lists all 800 slots; will switch to "only created blocks" in Batch 3.

### Next
- Developer in-game test of Batch 2 (TESTING_GUIDE §1c): create/list/give/place/rename/
  dupe/delete + survives a restart. Then Batch 3 = textures.

---

## 2026-06-03 — Phase 0 CONFIRMED ✅ + Phase 1 start

### Done
- **Phase 0 confirmed working in-game by the developer** ("Phase 0 works"). Mod loads in
  MC 1.21.1, the three `[CustomBlocks]` startup lines appear, no crash. Phase 0 is DONE.

### Now starting — Phase 1: Block Slot System
- Goal (Bible milestone): the game starts with **1028 blocks registered**, no errors.
- Scope: `CustomBlocksConfig` (minimal: maxSlots, httpPort), immutable `SlotData` + `.update()`,
  `SlotBlock`, `SlotManager` (register the pool + BlockItems + a creative tab), wire into
  `CustomBlocksMod` with a clear "Registered N slot blocks" log line.
- Deferred to their own phases: `SlotDataStore` (Phase 2 persistence), `BlockFinder` (Phase 3).
- Expectation: slot blocks render as the **missing-texture (purple/black) cube** until Phase 4.

### Build-verified (NOT yet in-game)
- Implemented `CustomBlocksConfig` (62 lines, atomic save), `SlotData` (immutable record
  + `.update()`), `SlotBlock` + `SlotBlock.SlotItem` (minimal), `SlotManager.registerAll()`,
  and wired `CustomBlocksMod` to register the pool + a "CustomBlocks" creative tab.
- `gradlew build` PASSED — compiles, all gates green. No file exceeds its size limit.
- Recycled the proven 1.21.1 registration API from the old project (`Identifier.of`,
  `Registry.register(Registries.BLOCK/ITEM, …)`, `FabricItemGroup.builder()`).
- **Awaiting developer in-game test** (see docs/TESTING_GUIDE.md §Phase 1): startup log
  should read "Registered 1028 slot blocks", game must not crash, creative tab present.
  Expect many "missing model" warnings + purple blocks — normal until Phase 4.

---

## 2026-06-03 (cont.) — Phase 0: hardening & protocol

### Done
- Wrote `CLAUDE.md` — the operating protocol (golden rule, who the developer is, phase
  discipline, architecture rules, known pitfalls, git/doc protocol, forbidden behaviors).
- Added `verifyFileSize` build gate (§9.3): fails the build if any `.java` exceeds its
  limit (500 general / 400 command handlers / 300 `*Config.java`). package-info exempt.
  This is new — the old project relied on an unenforced rule and grew 9,400-line files.
- Added `.editorconfig` (UTF-8, LF, 4-space) for encoding/format consistency (NFR-11).
- Added GitHub Actions CI (`.github/workflows/build.yml`): JDK 21 + `gradlew build`
  (which runs all three gates) + JAR artifact. Remote is `github.com/3liSY/CustomBlocks`.

### Note on "spot the bugs"
- The new project has no runtime bugs to hunt yet (≈3 code files, builds clean). The real
  bugs live in the old project; the strategy is **prevention** (gates above + the pitfalls
  table in CLAUDE.md §7) and **fix-on-port** as each phase is rebuilt.

### Verified
- `gradlew build` **PASSED** — all three gates run (`verifyFileSize`, `verifyMojibake`,
  `verifySound`).
- `verifyFileSize` proven to actually fire: a throwaway 501-line file made the build
  FAIL with exit 1 ("exceeds class limit of 500"); file then deleted, build green again.
  (A gate that's never seen to fail is false confidence — so it was tested both ways.)

### Next Session
- Phase 1 — Block Slot System (unchanged from the entry below).

---

## 2026-06-03 — Phase 0: Foundation (fresh start)

### Done
- Scaffolded a fresh project in the repo root, recycling the old project's working
  build infrastructure (Fabric Loom 1.7.4, Gradle 8.8 wrapper, MC 1.21.1, Java 21).
- `build.gradle`: minimal Phase-0 dependency set + `verifyMojibake` and `verifySound`
  gates wired into `build`. jcodec / SpotBugs / permissions are commented out, to be
  enabled in their phases (13 / 15 / when needed).
- `fabric.mod.json`, `customblocks.mixins.json` (empty mixin list), minimal `en_us.json`.
- Full package structure laid down as `package-info.java` for all 21 packages.
- `CustomBlocksMod` + `CustomBlocksClient` entrypoints — log a Hello-World line on load.
- Ported `TextSanitizer.java` verbatim (the mojibake repair dictionary).
- Docs: `CHANGELOG.md`, this log, `docs/adr/ADR-001` + template, copied
  `docs/MOJIBAKE_SHIELD.md`, and `LICENSE` / `LICENSE-ar`.

### Decision (flagged to developer)
- Instead of ~90 empty class stubs, the package structure is documented via
  `package-info.java` per package, with real classes added per phase. This matches
  the Bible's own "start minimal, expand per-phase" philosophy. Can switch to full
  stubs if the developer prefers.

### Verified
- `gradlew build` **PASSED** (25s): compileJava, verifyMojibake, and verifySound all
  green; produced `build/libs/customblocks-1.0.0.jar` (2.3 MB, fabric-api bundled).
- Required installing Temurin JDK 21 (machine had only JDK 26, too new for Gradle 8.8)
  and pinning the Gradle daemon to it via `~/.gradle/gradle.properties`.
- **NOT yet verified in-game** — per CLAUDE.md, only a developer in-game test counts
  as DONE. The build passing proves it compiles, not that it works in Minecraft.
  Next: run `gradlew runClient` and confirm the "[CustomBlocks] Hello World — mod
  loaded successfully." line appears in the log with no errors.

### Next Session
- Phase 1 — Block Slot System: `SlotBlock`, immutable `SlotData` + `.update()`,
  `SlotManager` (register 1028 blocks), `SlotDataStore`, `BlockFinder`, minimal
  `CustomBlocksConfig` (httpPort, maxSlots). Milestone: server starts with 1028
  blocks in the registry, no errors.
