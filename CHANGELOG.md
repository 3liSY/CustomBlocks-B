# Changelog

All notable changes to CustomBlocks are documented here. This project tracks
progress by **phase milestones** (see CustomBlocks_Engineering_Bible.md §8).

## [Unreleased]

### Added
- **Phase 13/16 final push — Video, remaining GUI screens, Mod Menu (Batch 14).**
  - **Video-to-texture** (`/cb video`) — place `.mp4` files in `config/customblocks/videos/`,
    then `/cb video list` to browse them and `/cb video extract <file> <id> <frame>` to bake
    a specific frame as a block texture. Uses jcodec 0.2.5 (pure-Java MP4/H264 decoder,
    bundled in the JAR). Frame extraction runs on a daemon thread; the pack rebuilds and
    prompts clients automatically.
  - **Macro GUI screen** — `/cb gui macros` (or Main Menu → "Macros") opens a dedicated
    `MacroListScreen` with one-click buttons for every macro workflow: view all, record new,
    play, delete, add step, stop recording.
  - **Arabic browser screen** — `/cb gui arabic` (or Main Menu → "Arabic Letters") opens
    `ArabicBrowserScreen` with buttons for import all, import single letter, create word block,
    and list. The "Tip" footer reminds about `arabtype.ttf` placement.
  - **Mod Menu integration** — when Mod Menu is installed, the gear icon on the CustomBlocks
    entry opens the in-game `ConfigScreen`. Mod links (Discord, YouTube) were already present
    in `fabric.mod.json`; the `modmenu` entrypoint now adds the config button.
  - **Main Menu navigation improved** — "Macros" and "Arabic Letters" buttons now open their
    dedicated GUI screens instead of running list commands in chat.

- **Phases 10–15 big push (Batch 13).**
  - **Macro system** — `/cb macro record <name>` starts recording. `/cb macro add <cmd>` appends
    a step. `/cb macro stop` saves. `/cb macro play <name>` replays each step through the server
    dispatcher. `/cb macro list` / `delete` manage saved macros. Persists to
    `config/customblocks/macros/<name>.json`.
  - **Arabic letter system** — `/cb arabic import` creates all 28 base Arabic letter blocks using
    Java2D to render each glyph to a texture. Place `arabtype.ttf` in `config/customblocks/` for
    full Arabic shaping (system fallback used otherwise). `/cb arabic letter <name>` imports a
    single letter; `/cb arabic word <text> <id> <name>` creates a custom Arabic word block.
    `/cb arabic list` shows import status.
  - **Diagnostics** — `/cb diag` shows a live system snapshot (slots used, heap, TPS, pack size,
    HTTP config). `/cb incidents` shows the server incident log; `/cb incidents clear` resets it.
  - **Cloud + Discord stubs** — `/cb vault [upload <id> | download <code>]` and `/cb discord
    [test | status]` guide the user to set `vaultEndpoint` / `discordWebhookUrl` in config.json.
    Actual cloud sync arrives in Phase 14 once the Cloudflare Worker is set up.
  - **AI stubs** — `AiCommandParser` and `AiTextureGenerator` are wired in; they activate when
    `aiApiKey` + `aiTextureEnabled` are set in config.json. Full Phase 13 implementation pending.
  - **In-game GUI (Phase 10)** — `/cb gui` opens a 7-button `MainMenuScreen` hub. `/cb gui block
    <id>` opens `BlockEditorScreen` with attribute-editing buttons. `/cb gui config` opens
    `ConfigScreen` showing live values. All screens open client-side via `OpenGuiPayload`; buttons
    run server commands or pre-fill the chat box.
  - **HUD overlay (Phase 11)** — When looking at a custom block the HUD shows its ID and display
    name in the top-left corner. Block data is synced to the client via `HudSyncPayload` on join.
    Toggle with `/cb edithud`; persisted as `hudEnabled` in config.json.
  - **First-time welcome (OnboardingManager)** — new players receive a clickable quick-start
    message once on first join. Welcomed UUIDs persisted in `config/customblocks/players.json`.
  - **New config fields** — `hudEnabled`, `aiApiKey`, `aiTextureEnabled`, `vaultEndpoint`,
    `discordWebhookUrl`. All default to safe values; existing configs are unaffected.

- **Phase 9 completion — Lock, Notes, Favorites, Drafts (Batch 12).**
  - **`/cb lock <id>` / `/cb unlock <id>`** — prevent any modification of a block until
    explicitly unlocked. Lock status is checked before delete, rename, retexture, all
    attribute setters, template apply, and the Deleter tool. `/cb locked` lists all locked
    blocks with `[unlock]` buttons. Persists to `config/customblocks/locks.json`.
  - **`/cb note <id>`** — view a block's annotation. **`/cb note <id> <text>`** — set it
    (multi-word, no quotes needed). **`/cb note <id> clear`** — remove it. Notes are
    server-wide metadata on the block definition; auto-cleaned when the block is deleted.
    Persists to `config/customblocks/notes.json`.
  - **`/cb fav <id>`** — toggle a block as a personal favorite (per-player). **`/cb favs`**
    lists your favorites with `[give]` and `[unfav]` buttons. Persists to
    `config/customblocks/favorites.json`.
  - **`/cb draft <id>`** / **`/cb publish <id>`** — mark a block as work-in-progress or
    publish it. **`/cb drafts`** lists all drafts with `[publish]` buttons. Persists to
    `config/customblocks/drafts.json`.
  - **`/cb list` and `/cb search`** now show `[locked]` (red) and `[draft]` (grey) status
    tags inline next to each block.
- **UX polish — Deleter visual, export menu, template overhaul (Batch 11).**
  - **Deleter visual:** after deleting a block definition the placed block now shows the
    purple/black "missing texture" checkerboard instead of going transparent/invisible.
  - **`/cb export` redesigned as an action menu.** Bare `/cb export` shows bulk options
    `[.json] [.txt] [to Vault]`. `/cb export <id>` shows `[to Config] [to Vault] [Download]`
    buttons. File paths and copy-path clutter removed. Vault stubs to Phase 14.
    New `[Download]` action writes the JSON and serves it at `http://host:port/export/<id>`
    via a new HTTP endpoint added to `ResourcePackServer`.
  - **`/cb template` overhauled:** list now shows each template's attributes inline
    (glow, hardness, sound, collision, category); empty list explains what templates are;
    save/apply confirmations show the attribute set; new `/cb template delete <name>`;
    tab-complete for template names on `apply` and `delete`.
- **Phase 9 — Export / Import / Templates (Batch 10).**
  - `/cb export <id>` — exports a single block's full metadata to `exports/<id>.json`
    (schema v1: glow, hardness, soundType, collision, category — importable by `importfolder`).
  - `/cb importfolder [path]` — scans a folder for per-block JSON files and creates any
    missing blocks. Skips existing ids; reports created / skipped / failed counts. Defaults to
    `exports/` when no path is given. Triggers a resource-pack rebuild if any blocks were added.
  - `/cb template save <name> <id>` — captures a block's attribute preset (glow, hardness,
    sound, collision, category) as a named template in `config/customblocks/templates/`.
  - `/cb template apply <name> <id>` — stamps a template's attributes onto an existing block;
    undoable via `/cb undo`.
  - `/cb template list` — shows all saved templates with a clickable **[apply]** button.
- **Deleter tool redesigned (Batch 10 fix).**
  - Right-clicking a placed custom block now **deletes its entire definition** (slot data +
    texture + resource-pack rebuild), matching what `/cb delete` does. The physical block in
    the world stays (reverts to the unassigned-slot appearance); `/cb undo` fully restores
    the definition and rebuilds the pack.
  - Previous behaviour (breaking only the placed instance) was wrong — the Deleter is a
    definition-management tool, not a mining shortcut.
- **Undo scope toggle (Batch 9).** `/cb config undomode` cycles undo/redo between
  **server-wide** (one shared history — the new default) and **per-player** (isolated);
  `/cb config` shows the current mode. Switching clears the existing history. Persisted as
  `undoMode` in `config.json`.
- **Phase 8 + 9 — Categories & Search (Batch 8).**
  - `/cb setcategory <id> <name>` — tag a block with a free-form category (`none` clears it).
    `SlotData` gains an immutable `category` field (default uncategorized), persisted in
    `slots.json` (omitted when empty). Undoable like the other attributes.
  - `/cb categories` — lists every category in use with a per-category count and a clickable
    **[list]** button.
  - `/cb search <query>` — finds blocks whose id, display name, or category contains the
    query (case-insensitive); each hit has a clickable **[give]** button.
  - `/cb list` now shows each block's `[category]` tag.
- **Phase 7 — Tools / Items (Batch 7: the 3 attribute tools).**
  - **Lumina Brush** — right-click a custom block to cycle its glow (0 → 4 → 8 → 12 → 15 → 0);
    sneak cycles backward. Refreshes placed copies, persists, and is undoable.
  - **Chisel** — right-click to cycle hardness presets (instant → soft → stone → hard → tough
    → unbreakable); sneak reverses. Persists and is undoable.
  - **Deleter** — right-click to instantly remove a placed custom block (no drop) — works even
    on unbreakable ones. Removes the placed instance only; the definition stays.
  - New `item/` package: `CustomToolItem` base (server-side gated to avoid the old client-skip
    delay) + the 3 tools + `ToolItems` registrar. New **"CustomBlocks Tools"** creative tab;
    tool models reuse vanilla textures (glowstone dust / amethyst shard / barrier).
  - (Color Square/Triangle, Rectangle, Golden Hexagon need the color + region subsystems —
    they land with Phase 8.)
- **Phase 6 — Block Attributes (Batch 6: collision).**
  - `/cb setcollision <id> <solid|passable>` — a **passable** block has an empty collision
    box so entities walk through it, while its outline stays full so you can still target and
    break it. Read **live** via `SlotBlock.getCollisionShape`, so placed blocks update at once.
  - `SlotData` gains an immutable `noCollision` field (default false/solid), persisted in
    `slots.json` (omitted when false). Undoable like the other attributes.
- **Phase 6 — Undo / Redo (Batch 5).**
  - `/cb undo` and `/cb redo` — **per-player** history (FR-06-2): your undo never touches
    another player's blocks. Covers create, delete, rename, dupe, setglow, sethardness,
    setsound. Delete-undo also restores the block's texture; create/delete undo rebuild the
    resource pack, glow undo refreshes placed-block light.
  - New `core/UndoManager` (clean ~150-line per-player stacks of immutable `SlotData`
    snapshots) + `command/handlers/HistoryCommands`. Config adds `maxUndoDepth` (default 25).
  - (Retexture is not yet undoable — that's a later slice.)
- **Phase 6 — Block Attributes (Batch 4, slice 3: sounds).**
  - `/cb setsound <id> <type>` sets the block's break/step/place sound group (stone, wood,
    metal, glass, sand, wool, gravel, snow, dirt, coral, bamboo, nether_brick, ice, honey,
    bone, slime). Read **live** via `getSoundGroup`, so placed blocks update immediately.
  - `SlotData` gains an immutable `soundType` field (default "stone"), persisted in `slots.json`.
- **Phase 6 — Block Attributes (Batch 4, slice 2: hardness).**
  - `/cb sethardness <id> <value>` sets break hardness (negative = unbreakable, 0 = instant,
    1.5 = vanilla stone). Read **live** on every break attempt via `calcBlockBreakingDelta`,
    so placed blocks update instantly with no relight/refresh.
  - Value also accepts **words**: `unbreakable`, `instant`, `stone` (plus tab suggestions),
    not just numbers. Message clarified (no longer implies every value equals "vanilla stone";
    notes that break-time differences show in Survival, since Creative breaks instantly).
  - `SlotData` gains an immutable `hardness` field (default 1.5), persisted in `slots.json`.
- **Phase 6 — Block Attributes (Batch 4, slice 1: glow).**
  - `/cb setglow <id> <0-15>` sets a block's light emission. Light is a real **block-state
    property** (the vanilla `LightBlock` approach), so it genuinely emits — a luminance
    lambda would be frozen at 0 because `getLuminance()` is baked once per state (see ADR-002).
  - New placements inherit the block's configured glow; existing placed blocks near players
    are refreshed immediately (`block/SlotLighting`), and a placed block's light is saved in
    the world so it survives reloads.
  - `SlotData` gains an immutable `glow` field (0–15), persisted in `slots.json` as the
    configured default (older files without the field default to 0).
  - `setglow` accepts any number and caps at 15 (Minecraft's max block light) with a clear
    message, instead of a red out-of-range rejection.
  - New `command/handlers/AttributeCommands` handler (keeps attribute setters out of the
    block-lifecycle handler, per design rule #5).
- **Tab auto-complete** for commands that take an existing block id
  (`delete`, `rename`, `dupe`, `retexture`, `setglow`, `give`) via `BlockSuggestions`.
- **One-command create + texture** (old-project format): `/cb create <id> <name> <url>`
  makes and textures a block in one go; `<id>` and `<id> <name>` still work.
  Multi-word names use quotes (`"Red Heart"`).
- **Phase 0 — Foundation.** Fresh Fabric MDK scaffold for Minecraft 1.21.1
  (Fabric Loom 1.7.4, Java 21):
  - `build.gradle` with the `verifyMojibake` and `verifySound` build gates
    (NFR-11 / NFR-12). Dependencies and SpotBugs are added per-phase, not up front.
  - `fabric.mod.json` + empty `customblocks.mixins.json` (mixin infra ready for Phase 5).
  - Full package structure documented via `package-info.java` for all 21 packages
    (server + client) per §3.
  - Mod + client entrypoints (`CustomBlocksMod`, `CustomBlocksClient`) — load + log.
  - `TextSanitizer` mojibake repair dictionary, ported verbatim from the old project.
  - `LICENSE` / `LICENSE-ar`, `docs/MOJIBAKE_SHIELD.md`, `docs/adr/` (ADR-001 + template),
    `PROGRESS_LOG.md`.
- **Phase 4 — Texture Pipeline (Batch 3).**
  - `image/ImageDownloader` (browser headers, timeouts) + `image/ImageProcessor`
    (bicubic, aspect-preserved square PNG).
  - `core/TextureStore` — per-slot PNG files (atomic), keeping textures out of slots.json.
  - `network/ServerPackGenerator` — builds the resource pack (pack_format 34): textured
    `cube_all` for created blocks; one shared invisible model for empty slots (no more
    "missing model" log spam).
  - `network/ResourcePackServer` — embedded single-endpoint HTTP server (`/pack.zip`),
    SHA-1 hashing, sends the pack to ALL clients (one pack path — avoids the old PACK2 split).
  - `/cb retexture <id> <url>` — downloads/decodes off-thread, then rebuilds + pushes the pack.
  - Creative tab now lists only created blocks; config adds `textureSize` (64) + `httpHost`.
- **Phase 2 — Persistence & Core Commands.**
  - `SlotDataStore` — the sole disk-I/O class for slots; atomic save/load of
    `config/customblocks/slots.json` (design rule #4, NFR-13).
  - `SlotManager` — create / delete / rename / dupe, with load-on-start, save-on-change,
    and `/cb reload`.
  - `/customblock` (alias `/cb`) command tree via `CommandRegistrar` + split handlers
    (`CreationCommands`, `UtilityCommands`): `create, delete, rename, dupe, list, give, reload, export`.
  - `/cb list` ends with clickable **[.json] / [.txt]** export buttons; `/cb export <json|txt>`
    writes the list to `config/customblocks/exports/` (atomic) via `BlockExporter` and offers
    a click-to-copy file path.
  - Default `maxSlots` lowered to **800** for faster startup.
- **Phase 1 — Block Slot System.**
  - `CustomBlocksConfig` — minimal config (maxSlots=1028, httpPort=8123) with atomic
    save to `config/customblocks/config.json`.
  - `SlotData` — immutable record + `.update()` builder (design rule #1).
  - `SlotBlock` + `SlotBlock.SlotItem` — the generic block/item backing each slot.
  - `SlotManager.registerAll()` — pre-registers the 1028-slot pool (ADR-001).
  - Creative tab "CustomBlocks" listing all slot items.
  - (Untextured until Phase 4 — blocks render as the missing-texture cube.)
- **Phase 0 — Hardening & protocol.**
  - `CLAUDE.md` — the operating protocol (golden rule, phase discipline, architecture
    rules, known pitfalls, forbidden behaviors).
  - `verifyFileSize` build gate — fails the build on monolith files (§9.3: 500/400/300
    line caps). The old project had no such enforcement.
  - `.editorconfig` — UTF-8 + consistent formatting across all tools (NFR-11).
  - GitHub Actions CI (`.github/workflows/build.yml`) — builds and runs all gates on push.

### Changed
- `/cb create` name argument is now a quoted string (was greedy) so a URL can follow it.
- **In-game messages now use the old `[CB]` format** (`command/Chat`): one bold `[CB]` prefix,
  a short body, and a ✔/✖ glyph. Removed the long multi-line explanations.

### Fixed
- **Deleter tool now actually removes the block** and only says "Removed" when it did. It used
  `removeBlock` and printed success unconditionally; it now uses `World.breakBlock` (reliable
  client sync + particles, ignores hardness) and checks the result.
- **Tool HUD messages are now readable.** The `[CB]` chat prefix uses black brackets (`§0`)
  that are nearly invisible on the action bar; tool feedback now uses an aqua `HUD_PREFIX`.
- **Hardness mining speed was wrong** (felt inverted / "everything unbreakable"). The mining
  formula now matches vanilla exactly (`getBlockBreakingSpeed / hardness / (canHarvest?30:100)`),
  so lower hardness mines faster and higher mines slower as expected. The default stays 1.5
  (vanilla stone); the earlier "unbreakable default" was this formula bug, not a default change.
- **`data:`/non-web URLs** now give a plain-English error instead of the cryptic
  "invalid URI scheme data", and are rejected **before** the block is created — so a bad
  URL no longer leaves a half-made block (which previously caused a misleading
  "id already exists" on retry).

---

<!--
Template for a released version:

## [0.1.0] - YYYY-MM-DD
### Added
- Phase 1 complete: SlotManager, SlotData, 1028 slots registered
### Changed
-
### Fixed
-
-->
