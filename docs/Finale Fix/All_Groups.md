# Phase 17 — GUI Restoration, Tool Unification & Legacy Recovery

## Objective

Restore the original chest-based GUI system, recover all missing legacy features, unify the tool ecosystem into a single configurable system, and fix all regressions introduced during modernization.

## Background

The previous CustomBlocks chest GUI system was more intuitive, reliable, and feature-complete. During later development, portions of the GUI infrastructure were replaced with custom screen-based interfaces, resulting in broken workflows and missing functionality. Additionally, numerous legacy commands and systems appear to have been silently dropped during the rebuild.

---

## Issue Log

| # | Issue | Evidence / Source |
|---|---|---|
| 17.1 | No pack prompt dialog — want instant silent RP reload like old CustomBlocks | Screenshot: vanilla "Would you like to download" dialog during Phase 4 testing |
| 17.2 | Chat messages too terse (CB prefix style) — want old human/conversational messages back | Screenshots: `CB  Downloading texture…`, `CB  Gave 1 ✓` |
| 17.3 | Chest GUI replaced by screens — screens do not satisfy; restore original chest GUI system | Screenshot: screen-based main menu with Block List / Categories / Templates / Macros / Arabic Letters / Diagnostics |
| 17.4 | HUD has no config system — `/cb edithud` only toggles on/off; old CB had a drag-to-reposition HUD layout editor (`OpenHudEditorPayload`). `/cb config hud` never existed — the full HUD config GUI needs building. | Confirmed in-game during Phase 5 testing; command source audited Phase 10 |
| 17.5 | `/cb gui editor` is wrong command — correct is `/cb gui block <id>`. But the old CB `/cb editor <id>` shortcut is missing. Add `/cb editor <id>` as a direct alias. | Confirmed in-game during Phase 5 testing; source audited Phase 10 |
| 17.6 | HUD system absent — HUD overlay does not exist; needs full restoration | Confirmed in-game during Phase 5 testing |
| 17.7 | ESC key — pressing ESC should offer CustomBlocks menu access (button or intercept) | Requested UX improvement |
| 17.8 | Held block glow — holding a block with glow > 0 should emit dynamic light from hand (like torch mods). Same on placement. | Requested QoL during Phase 6 testing |
| 17.9 | Dedicated Creative Tools Tab — tools tab needs audit/improvement for discoverability and future-proofing | Raised during Phase 7 review |
| 17.10 | Chisel + Lumina unification — two separate tools create unnecessary complexity; merge into one configurable tool system | Raised during Phase 7 review |
| 17.11 | Legacy feature audit — comprehensive investigation of features missing vs old CustomBlocks | Known missing: `/cb snapshots` (replaced by `/cb backup`). Full scope TBD by audit. |
| 17.12 | `/cb note` system improvement — current is barebones text storage; wire to future chest GUI, better formatting, richer UX | Raised during Phase 9 testing |
| 17.13 | `/cb fav` should be an alias of `favorite` — `favorite` must also exist as the primary command (not just `fav`) | Raised during Phase 9 testing |
| 17.14 | Draft/publish system — confusing to players; needs clearer explanation, better chat messages, and GUI integration | Raised during Phase 9 testing |
| 17.15 | Export system rework — entire export UX needs redesign: chat output, formats, what gets shown, workflow; needs full discussion | Raised during Phase 9 testing |
| 17.16 | AnimBlockScreen missing — animation/GIF block editor screen not built; `client/gui/` is empty | Discovered during Phase 11 source audit |
| 17.17 | DevConsoleScreen missing — in-game debug log viewer not built | Discovered during Phase 11 source audit |
| 17.18 | `/cb arabic word` broken — command takes Arabic Unicode as first arg but Minecraft chat cannot input Arabic characters; Brigadier rejects non-ASCII input. Command needs redesign (e.g. compose word from letter names, or use clipboard paste flow). | Phase 12 in-game test — screenshot shows Brigadier error at position 16 |
| 17.19 | `arabtype.ttf` not bundled — mod uses system `SansSerif` fallback which lacks proper Arabic shaping; letters render as unshaped glyphs. Font must be placed at `config/customblocks/arabtype.ttf` manually. Will be auto-placed from PC when found. | Phase 12 testing |
| 17.20 | Auto-joining not built — placing Arabic letter blocks next to each other should detect east/west neighbors and swap to the correct contextual form (initial/medial/final/isolated). Not implemented. | Phase 12 source audit |
| 17.21 | English font for word/letter blocks — what English font (if any) should be used for non-Arabic block text? Needs developer decision. | Phase 12 interview question |
| 17.22 | AI features are stubs — `AiCommandParser` and `AiTextureGenerator` both return null; no `/cb ai` command wired. Full API integration needed. | Phase 13 source audit |
| 17.23 | `/cb config` needs chest GUI — no in-game way to set `vaultEndpoint`, `discordWebhookUrl`, AI keys, or other config values without editing `config.json` manually. Blocks full Phase 14 testing (Discord live test, vault upload/download). | Phase 14 testing |
| 17.24 | `/cb diag` + `/cb incidents` need full rework — currently functional but output, layout, and UX need improvement. Needs dedicated discussion. | Phase 15 in-game testing |

---

## Requirements

### 1. Restore Original Chest GUI Architecture (Issue 17.3)

Reintroduce the classic chest-based GUI framework used in the original CustomBlocks project.

* **DECISION:** The main GUIs will be accessed via `/cb`, `/cb menu`, `/cb dashboard`, and `/cb gui`. Other commands will have their own GUIs.
* The visual theme will be the old "stained glass style" but modernized, cleaner, and more polished.
* Eliminate all screen-based UIs and replace them with Chest GUIs.

---

### 2. Restore HUD System (Issues 17.4, 17.6)

* `/cb config hud` must function correctly.
* HUD overlay shows block ID + display name when crosshair is on a custom block.
* **DECISION:** Build a drag-to-reposition HUD Editor with full customization options (Color, Scale, Position, and Background Opacity).

---

### 3. Restore GUI Editor Access (Issue 17.5)

* `/cb gui editor` must be a valid command.
* Opens the block editor GUI (chest-based, not screen-based).

---

### 4. ESC Menu Integration (Issue 17.7)

* **DECISION:** Inject two standard gray buttons equally below the vanilla "Leave Game" button: one for "CustomBlocks Menu" and one for "HUD Editor".
* Both buttons will use easily editable Command Block icons.

---

### 5. Chat Message System (Issue 17.2)

* Error messages: clear, plain-language explanation.
* Success messages: friendly, conversational.
* **DECISION:** Major system errors will be shown directly to the player who triggered them, AND simultaneously written to the "Incidents GUI" log for server admins to review later.

---

### 6. Silent Resource Pack Delivery (Issue 17.1)

* Pack applies instantly on texture change with no player dialog or click required.
* **DECISION:** Use an "Auto-Click Mixin" to invisibly intercept and accept the vanilla pack prompt. This provides 100% stability of the vanilla engine while remaining completely silent to the player.

---

### 7. Held Block Dynamic Glow (Issue 17.8)

* Holding a custom block with `glow > 0` emits dynamic light from the player's hand.
* Light level matches the block's configured glow value.
* Same effect applies when placing the block.
* **DECISION:** Use a "Networked Client-Side" Mixin approach. The server sends a small packet indicating a player is holding a glowing item, and the clients render the light locally via Mixin. This ensures everyone sees the light, but causes zero server TPS lag and requires no external mod dependencies.

---

### 8. Dedicated Creative Tools Tab (Issue 17.9)

A dedicated creative inventory tab exclusively for CustomBlocks tools.

* **DECISION:** The tab will contain ONLY the core tools (Omni-Tool, Deleter Tool, Green Square, Yellow Triangle, etc.). It will NOT contain blueprints or generated blocks.
* The organization/sorting behavior will be completely configurable in the config with switchable modes (e.g., category grouping vs alphabetical).

---

### 9. Chisel + Lumina Unification (The Omni-Tool) (Issue 17.10)

Merge the Chisel and Lumina Brush into a single unified tool system called the "Omni-Tool".

**Configuration access:**
* Shift + Right Click on a block opens the tool configuration GUI (chest-based).
* The tool item will visually change texture/color depending on the active mode (Glow, Hardness, Delete, Paint, etc.) so the player always knows what it does.

**Within the GUI, the user can:**
* Select active tool behavior.
* Configure interaction modes and adjust cycling behavior and ranges.
* Save preferred configurations.
* Customize default right-click action profiles.

---

### 10. Legacy Feature Audit & Restoration (Issue 17.11)

A dedicated investigation must be performed before any restoration work begins.

**Investigation scope — compare current implementation against old CustomBlocks and identify:**
* Missing commands.
* Missing GUIs.
* Missing systems.
* Missing workflows.
* Missing utilities.
* Missing editor functionality.
* Missing configuration options.
* Features replaced by inferior alternatives.

**Known missing example:** `/cb snapshots` — previously existed, now absent or non-functional.

**Current commands that DO exist** (for reference when doing the audit):
```
/cb create, delete, rename, dupe, retexture
/cb setglow, sethardness, setsound, setcollision, setcategory
/cb undo, redo
/cb lock, unlock, locked
/cb note, fav, favs, draft, publish, drafts
/cb list, give, reload, search, categories
/cb export (json, txt, vault, <id>), importfolder
/cb template
/cb macro
/cb arabic
/cb diag, incidents
/cb cloud
/cb gui
/cb video
/cb config
```

**Output required from audit:**
* Full table: Existing / Missing / Partial / Broken / Replaced-by-inferior.
* Restoration priority per item — determined by developer interview (see below).

**Restoration policy:**
Any legacy feature that provided meaningful functionality should be evaluated and restored. The objective is not just parity — it is recovering functionality that was unintentionally lost.

---

### 11. Universal Export Dashboard (Issue 17.15)

**Server folder:** `config/customblocks/cloud_exports/` — `CloudVaultClient.java` must read/write this folder for vault exports. Old export files (hash-named JSONs + PNGs) already live there from the old jar.

The export system must be redesigned into a robust Chest GUI with advanced options:
* **Bulk Actions:** Export all blocks to a ZIP, Export a specific Category, or Vault Sync.
* **Single Block Actions:** Select a block to open a sub-menu to: Download PNG only, Download JSON only, Download .csv, Download raw .nbt, Export full block (ZIP), Generate a physical Blueprint item, or Share a short-code in chat.
* **Advanced Exports:** Support exporting as a standalone Vanilla Resource Pack (so non-modded players can use the blocks via CustomModelData), and generating `.litematic` or `.schem` files for mapmakers.
* **DECISION:** Add a localhost HTTP server route that generates clickable chat links to instantly download exported files, eliminating the need to dig through config folders.

---

### 12. Diagnostics & Incidents GUI (Issue 17.24)

The `/cb diag` and `/cb incidents` commands must be reworked into an interactive "IT Chest" Dashboard:
* **Live System Health:** Top row features items representing TPS (color-coded glass pane), Block Registry Status, and Network Sync. Hovering provides exact metrics.
* **The Incident Log:** Middle rows list errors (Red Wool) and warnings (Yellow Wool).
* **Auto-Fix:** Clicking an incident attempts to fix the issue or opens the relevant editor.
* **Controls:** Options to Clear Log or Generate a full `.txt` report to the server files.

---

## 🔴 NEEDS DEVELOPER INTERVIEW — Legacy Feature Decisions

> ✅ **INTERVIEW COMPLETE (2026-06-09).** Every group below is decided — see each group's **DECISION** line plus the consolidated **Locked Decisions** section at the end of this document. The build-gate hold is lifted; implementation may begin once the developer confirms sequencing.

The full old command list was audited from `CustomBlocks/src/main/java/com/customblocks/command/CustomBlockCommand.java`. Every feature below exists in the old project and is missing or broken in new CustomBlocks-B. Developer must decide what to restore.

---

### Group A — Bulk Operations

Commands: `bulkdelete`, `bulkrename`, `bulkreid`, `bulkproperty`, `bulkexport`, `bulkmove`, `bulkduplicate`, `bulklock`, `bulkunlock`, `bulkfavorite`, `bulkunfavorite`, `bulkshape`, `bulksound`, `bulk` / `bulkgui`, `bulkblockadd`, `bulkrecolor`

These allowed operating on many blocks at once — delete 10 blocks, rename all blocks with a prefix, recolor an entire category, etc.

**DECISION:** Restore all bulk operations.

---

### Group B — Shape System

Commands: `setshape`, `addshape`, `removeshape`, `clearshape`, `shapeeditor`, `shapelist`, `shapepreview`, `facechangegui`, `setface`, `clearface`, `clearallfaces`, `bulkshape`, `customtriangle`, `trianglemode`

Old CustomBlocks supported custom block shapes (slab, thin, carpet, wall, pane, stairs, cross, etc.) and per-face textures (different image on each face of the block).

**DECISION:** Restore both the shape system and per-face textures, but it must be cleaned and without junk or issues.

---

### Group C — Backup System

Commands: `backup create/list/restore/delete/expiry`, `safety`, `recover`, `panic`

- **Backup:** point-in-time saves of all block definitions + automated timed backups with expiry policies.
- **Panic/Recover:** emergency rollback system.

**DECISION:** Build a unified `/cb backup` system. No `/cb snapshots` — snapshots are merged into backup. Macros remain separate in `config/customblocks/macros/`.
* **CRITICAL NOTE:** The old snapshot system was completely unreliable and broken (e.g., reverting to broken states on restart). The entire system must be redesigned and rebuilt from the ground up with careful planning. Do not patch the old code.
* **Storage:** All backups saved to `config/customblocks/backups/` AND synced to the CustomBlocks Vault (Cloud).
* **Commands:** `/cb backup save [name]`, `/cb backup list`, `/cb backup restore <name>`, `/cb backup delete <name>`, `/cb backup panic`

---

### Group D — Trash & Recovery

Commands: `deletedblocks`, `showbrokenblocks`, `recover`

- **deletedblocks:** browse recently deleted blocks (trash can). Restore them from the GUI.
- **showbrokenblocks:** list blocks that have placement issues or missing textures.

**DECISION:** Restore both trash browser and broken-blocks report. The Trash will feature an adjustable auto-delete timer (e.g., 30 days) AND the ability to "pin" items to keep them forever.

---

### Group E — Color & Image Tools

Commands: `dress`, `gradient`, `colors`, `customcolor`, `palette`, `bgstudio`, `tolerance`, `exportpng`, `resize`

- **dress:** apply a color overlay pattern on top of an existing texture.
- **gradient:** generate a series of intermediate blocks between two colors.
- **palette:** per-player color palette management.
- **bgstudio:** background removal studio GUI.
- **tolerance:** set background removal tolerance.
- **exportpng:** export a block's texture as a PNG file.
- **resize:** resize a block's texture resolution (e.g. 64→128px).

**DECISION:** Restore all color and image tools.

---

### Group F — Tool-Give Shortcuts

Commands: `brush`, `chisel`, `deleter`, `square`, `triangle`, `rectangle`, `hexagon`

These gave the player the corresponding tool item directly. Currently you must use `/give @s customblocks:lumina_brush` etc.

**DECISION:** Restore quick give. Lumina and Chisel are resolved — they become **Omni-Tool modes** (see Locked Decisions §C), not separate give shortcuts.

---

### Group G — Category System (Full)

Commands: `blocks`, `blockscat`/`blockscategory`, `blockadd`, `givecategory`, `givedisplayblock`, `exportcategory`, `sharecategory`, `importcategory`

Old had a full category browser GUI, per-category give-all, display blocks showing a category icon, category export/import/share by code.

**DECISION:** Restore the full category system (browsers, display blocks, etc.).

---

### Group H — Marketplace & Sharing

Commands: `sharecategory`, `importcategory`, `exportblock`, `importblock`, `market`

Share individual blocks or whole categories via short codes. Import shared blocks from others.

**DECISION:** Restore both sharing and the marketplace.

---

### Group I — Voice Modes

Command: `voice <mode>` — modes: `friendly`, `professional`, `royal`, `minimal`, `arabic`, `silly`

Changes the tone/style of all chat messages from the mod.

**DECISION:** Scrap Voice Modes entirely to save development time and technical debt. Instead, use a single, highly polished, professional conversational tone for all standard mod messages.

---

### Group J — Showcase Blocks

Command: `showcase`, `showcase config`

Special display blocks that show a 3D rotating preview of a custom block. Used for decoration/display.

**DECISION:** Revamp completely. Include pedestals, glass cases, custom rotation speeds, and support for displaying any vanilla item in addition to custom blocks.

---

### Group K — Block Identity Operations

Commands: `reid`, `swapid`, `swapname`, `duplicate` (alias for dupe), `resume`

- **reid:** rename a block's ID without changing its display name.
- **swapid / swapname:** swap the IDs or names of two blocks.
- **resume:** resume an interrupted draft session.

**DECISION:** Restore all identity operations (reid, swapid, swapname, resume).

---

### Group L — HUD & GUI Commands (beyond 17.3–17.6)

Commands: `edithud`, `listgui`, `help`, `welcome`, `menu`, `magicitems`, `editmagicitems`, `editor`, `history`, `undogui`, `redogui`, `scriptgui`, `resourcepack`/`rp`, `rp pause`, `rp resume`, `sync`, `unsuppress`

- **edithud:** opens a drag-to-reposition HUD layout editor.
- **editor:** opens the block editor GUI for a specific block.
- **history:** shows undo/redo history in chat or GUI.
- **undogui / redogui:** GUI-based undo/redo browser.
- **magicitems / editmagicitems:** special item management system.
- **rp pause / rp resume:** pause/resume resource pack updates during batch operations.
- **sync:** force-sync the resource pack to all clients.

**DECISION:** Restore all of these GUI/HUD commands.

---

### Group M — Misc Commands

Commands: `recent`, `find`, `settabicon`, `bgstudio`, `cache`, `audit`, `screenshot`, `achievements`, `script`/`scriptgui`, `market`, `tolerance`

- **recent:** show recently used blocks.
- **find:** find all placed copies of a block in the world.
- **settabicon:** set a custom URL image as the tab icon.
- **achievements:** in-game achievement system for CustomBlocks usage.
- **script / scriptgui:** extended macro system with GUI.

**DECISION:** Restore all misc commands. Perform a massive overhaul on `bgstudio` (image background removal), `find` and `recent` (tracking tools), `settabicon`, and the `achievements` system, as these were known to be buggy in the old version.

---

### Group N — Partially Broken in New CB-B (known regressions)

These exist in new CB-B but are worse than the old version:

| Command | Old behavior | New behavior | Gap |
|---|---|---|---|
| `undo` | Supports `undo clear`, `undo <N>` (undo multiple) | Only single undo | Missing multi-undo and clear |
| `redo` | Supports `redo <N>` | Only single redo | Missing multi-redo |
| `give` | `give <id> <amount> <player>` | `give <id>` only | Missing amount and player args |
| `favorite` | `favorite` + dedicated `unfavorite` | `fav` only (toggle) | Name changed, no dedicated unfavorite |
| `search` | Opened a searchable GUI | Text output only | GUI replaced by inferior text |
| `config` | Opened full settings GUI | Reduced, no GUI | GUI replaced |
| `delete` | `delete #` shortcut (delete what you're looking at) | `delete <id>` only | Missing # shortcut |
| `export` | `exportall`, `exportcategory`, `list export csv` | Different structure | Some export paths missing |

**DECISION:** Restore all regressions (Undo/Redo, Give, Search, Favorite, Config, Export, Delete, and more).

---

### Group O — Arabic System Decisions

Issues discovered during Phase 12 in-game testing. All need developer input before fixing.

**O1 — `/cb arabic word` command redesign (Issue 17.18)**

Current command: `/cb arabic word <unicodeText> <id> <displayName>`
Problem: Minecraft chat cannot accept Arabic Unicode input. Brigadier also rejects it.

Options being considered:
- Compose word from letter names: `/cb arabic word alef-ba-ta myword MyWord`
- Accept only English text (for word-as-texture use case, not Arabic text)
- Clipboard paste flow (GUI-based)

**DECISION:** Use an anvil-based flow (e.g., custom Anvil GUI) to allow typing or pasting text for word blocks. Give players the choice between generating a single texture block, or acting as a macro to place multiple physical letter blocks in the world.

---

**O2 — English font for text/word blocks (Issue 17.21)**

The renderer uses Java2D to draw text onto a block texture. For English text blocks, what font should be used?
- System default (`SansSerif`)
- A bundled custom font (which one?)
- No English text blocks — Arabic only

**DECISION:** Bundle **Rockwell Condensed**. This slab-serif font fits the blocky Minecraft aesthetic perfectly while remaining premium and modern.

---

**O3 — Auto-joining restoration (Issue 17.20)**

Old CB detected east/west neighbors and automatically swapped Arabic letter blocks to their correct contextual form (initial, medial, final, isolated). Not built in new CB-B.

**DECISION:** Build auto-joining from scratch for perfect stability. **Scrap** the 3D/diagonal connecting idea. Keep it realistic: letters only connect in a strict, straight horizontal line from right to left, just like written Arabic. Any T-junctions or vertical placements will break the connection.

---

**O4 — arabtype.ttf placement (Issue 17.19)**

Font found at `C:\Windows\Fonts\arabtype.ttf` and copied to `run/config/customblocks/arabtype.ttf`. This is the local dev run directory — if you ever wipe it, re-copy. In production the font must be placed manually by server owners.

**DECISION:** Bundle the font in the JAR.

---

### Group P — Deferred Subsystems (Deep Search Discoveries)

**P1 — AnimBlockScreen (Issue 17.16)**
**DECISION:** Restore the old system that accepts both `.gif` and pre-sliced `.png` files, but upgrade it significantly to eliminate past bugs.

**P2 — DevConsoleScreen (Issue 17.17)**
**DECISION:** Do not restore as a separate screen. Merge the debug logs directly into the new "IT Chest" (Diagnostics GUI) so admins can read logs by hovering over items.

**P3 — AI Features (Issue 17.22)**
**DECISION (updated 2026-06-09):** Use a **keyless** generator — **Pollinations.ai** primary (HTTP GET, request texture size directly), **Stable Horde** anonymous fallback. HuggingFace SDXL is dropped (now requires a token + heavy rate limits). See Locked Decisions §D.

**P4 — Config Inputs (Issue 17.23)**
**DECISION:** Long strings like AI Keys or Discord Webhooks will be input by clicking the Config Chest slot, which opens an Anvil GUI to paste the text safely.

**P5 — Note System (Issue 17.12)**
**DECISION:** Rework into a comprehensive Book GUI with three tabs: a Lore page, a To-Do checklist, and a Hover Tooltip toggle. Also include a new feature allowing players to "Share" their notes.

**P6 — Draft/Publish System (Issue 17.14)**
**DECISION:** Rename the system to the "Staging Area". Add clear in-GUI explanations that this area exists to prevent severe server Resource Pack lag during heavy block editing.

**P7 — First-Join Welcome (Test 14.1)**
**DECISION:** When a new player joins, automatically give them a physical "CustomBlocks Starter Guide" book in their inventory AND open a pop-up Screen GUI tutorial thanking them for installing the mod.

### Group Q — Post-v1.0 & Missed Features (Final Deep Search)

**Q1 — Hologram Previews**
**DECISION:** Build a full holographic projection system. ` /cb preview <url>` spawns a spinning hologram to preview a block before creating it. Holding a custom block in your offhand will project a hologram above your hand for other players to see.

**Q2 — Smart AI Background Removal**
**DECISION:** Integrate a local/cloud AI segmentation model (like `rembg`) for perfect subject cutouts. Additionally, restore and improve the original three modes: "Corners Only", "Corners + Enclosed Areas" (flood-fill trapped colors), and "None".

**Q3 — Live Recolor & Screen Eyedrop**
**DECISION:** Build an advanced Color Picker GUI. Include a "Screen Eyedrop" feature to sample any pixel on the user's monitor, and a Live Recolor slider that shows the block updating in real-time.

**Q4 — The Achievement System**
**DECISION:** Build from scratch. Add an "Achievements" tab to the Master Chest GUI that tracks block creation milestones, sharing, and color mastery.

**Q5 — Command Help & Error Correction**
**DECISION:** Restore `DidYouMean` to suggest corrections for typo commands. Rework `/cb help` to open a structured Chest GUI command list instead of flooding the chat.

**Q6 — Video Extraction (`/cb video`)**
**DECISION:** Wire the `jcodec` MP4 frame extractor into a "Video Studio" Chest GUI. It will list all `.mp4` files and allow 1-click animated block creation.

**Q7 — Permissions System (LuckPerms)**
**DECISION:** Restore the Fabric Permissions API / LuckPerms integration. Add strict permission nodes for every command (e.g., `customblocks.command.create`) so server owners can restrict features by rank.

**Q8 — Missing Tools Consolidation**
**DECISION:** The "Golden Hexagon" (Admin tool), "Rainbow Rectangle" (Area tool), and "Diamond Triangle" (Eyedrop/Wand tool) will NOT be physical items. They will be merged into the new "Omni-Tool" as hot-swappable modes to prevent inventory bloat.

### Group R — The Absolute Final Discoveries (Deep File Tree Scan)

**R1 — HistoryTracker & Placement Stats**
**DECISION:** Restore `/cb history` as both a text command and a "Server Mutation Log" inside the IT Chest GUI so admins can track who edited blocks. Restore `PlacementStats` to track who placed which custom blocks and where.

**R2 — DropConfigManager (Custom Drops)**
**DECISION:** Restore the ability for custom blocks to drop different items when broken. Add a "Custom Drop" slot in the Block Editor GUI so players can easily assign drops (e.g., dropping diamonds).

**R3 — SampleBlocksLoader & FirstUseHints**
**DECISION:** When the mod is installed on a server for the very first time, pre-load 5-10 cool example blocks (like "Test Glowing Orb") so the creative tab isn't empty. Combine `FirstUseHints` and `TipPool` to provide helpful hints in the GUI.

**R4 — Block Finder GUI & Export PNGs**
**DECISION:** Restore `BlockFinder` (`/cb find`) but upgrade it into a GUI that scans nearby chunks and highlights placed custom blocks. Also, add an "Export PNG Screenshot" button to the Block Editor GUI to save HD renders.

**R5 — Minor Subsystems (Auto-Categorize & Color Variants)**
**DECISION:** Restore `AutoCategorizeManager` to automatically guess a block's category based on its name/texture. Restore `CategoryDisplayBlockManager` so users can set a custom block as the icon for a category. Restore `ColorVariantService` to generate algorithmic color variations of a block in the Color Tools GUI.

---

## Success Criteria

Phase 17 is complete only when all of the following are confirmed in-game by the developer:

* Chest-based GUI system fully restored — no screen-based interfaces (Issue 17.3).
* HUD system functional — shows block info, toggleable, configurable (Issue 17.6).
* `/cb config hud` works (Issue 17.4).
* `/cb gui editor` works (Issue 17.5).
* ESC menu integration implemented (Issue 17.7).
* Chat messages are human-readable and conversational (Issue 17.2).
* Resource pack applies silently — no player dialog ever shown (Issue 17.1).
* Held-block dynamic glow working (Issue 17.8).
* Dedicated tools creative tab clean and organized (Issue 17.9).
* Chisel + Lumina merged into unified configurable tool with chest GUI (Issue 17.10).
* Legacy audit complete + all meaningful missing features restored (Issue 17.11).
* Developer confirms every item in-game. Nothing is ✅ DONE until then.

---

## Server Config Folder Structure (post-cleanup)

All JSON/GZ data files were moved to `config/customblocks/data/` during the pre-Phase-17 cleanup (2026-06-09).

### Path constants every class must use

| File | Correct path | Class that owns it |
|------|-------------|-------------------|
| Block slot data | `config/customblocks/data/slots.json` | `SlotDataStore` |
| Mod settings | `config/customblocks/data/config.json` | `CustomBlocksConfig` |
| Categories | `config/customblocks/data/categories.json` | Phase 17 CategoryManager |
| Favorites | `config/customblocks/data/favorites.json` | `FavoritesManager` |
| HUD config | `config/customblocks/data/hud-config-server.json` | `HudConfig` |
| Locks | `config/customblocks/data/locks.json` | `LockManager` |
| Notes | `config/customblocks/data/notes.json` | `BlockNotesManager` |
| Achievements | `config/customblocks/data/achievements.json` | Phase 17 AchievementsManager |
| Placement stats | `config/customblocks/data/placement_stats.json` | Phase 17 PlacementStats |
| Players | `config/customblocks/data/players.json` | Phase 17 PlayerManager |
| Templates | `config/customblocks/data/templates.json` | `TemplateManager` |
| Display blocks | `config/customblocks/data/display_blocks.json` | Phase 17 DisplayBlockManager |
| Drop config | `config/customblocks/data/drop_config.json` | Phase 17 DropConfigManager |
| Auto rules | `config/customblocks/data/auto_rules.json` | Phase 17 AutoRulesManager |
| Magic items | `config/customblocks/data/magic_items.json` | Phase 17 MagicItemsManager |
| Textures | `config/customblocks/textures/slot_N.png` | `TextureStore` |
| Backups | `config/customblocks/backups/` | Phase 17 BackupManager |
| Cloud exports | `config/customblocks/cloud_exports/` | `CloudVaultClient` |
| Macros | `config/customblocks/macros/` | `MacroManager` |
| Import staging | `config/customblocks/import/` | Phase 17 ImportManager |

### First-boot migration (runs once, Phase 17)

The server currently has old-format data. On first boot with new jar, a `MigrationManager` must:

1. **Slots** — read `data/slots.json.gz` (gzip, old `"slots"` key, `"lightLevel"` field) → convert to new format (`"blocks"` key, `"glow"` field) → save as `data/slots.json` → delete `slots.json.gz`
2. **Config** — `data/config.json` already exists. `CustomBlocksConfig` must be updated to read from `data/config.json` instead of root `config.json`
3. **Textures** — rename all `textures/slot_N.dat` → `slot_N.png` in place (they are identical PNG bytes, just wrong extension). `TextureStore` falls back to `.dat` if `.png` missing during migration window.
4. **Categories** — read old `data/categories.json` (separate file with rich objects + assignments map) → merge category string into each block's `SlotData` → handled by Phase 17 CategoryManager

### maxSlots
Default is **1000 everywhere** — code default in `CustomBlocksConfig.java`, README, HANDOFF, and the live `config.json` (locked by developer decision 2026-06-09; supersedes the old 800/1028/2048/850 values). Code cap stays 8192. Old data tops out at block index **687** (old `maxSlots` was 850). Verify clean registration at startup with 1000.

---

## Phase Status

**Current Status: Decisions finalized (2026-06-09) — ready to implement, awaiting developer go-ahead on sequencing.**

Note: Phases 1–7 passed in-game. Tests 5.4 and 5.8 deferred here as known regressions. Phase 17 is scoped as the final polish + restoration pass after all 16 main phases pass.

---

## ✅ Locked Decisions — Developer Interview Complete (2026-06-09)

This section finalizes every open question in this document plus the phase-routing
decisions made during the 2026-06-09 review. **Where anything here conflicts with
older text above, this section wins.**

### A. Global / cross-cutting
1. **Slot capacity → 1000 everywhere.** Code default (`CustomBlocksConfig`), README, HANDOFF, this doc, and the live `config.json`. Was: README 1028, code/doc 800, live config 2048, old config 850. Cap stays 8192. Verify clean startup registration at 1000.
2. **Config scope creep → split per phase.** Move Phase 6/11/13/14 fields out of the Phase 0 config block into their owning phases.
3. **32K blockstate warnings → revisit AFTER Phase 17** (harmless launch-timing noise; see PROGRESS_LOG 2026-06-07).
4. **Git → stay on `main` for now.** No `phase17` branch until the developer says so.
5. **Docs location → `docs/Finale Fix/`.** This Phase 17 document is the sole file in that folder. The Engineering Bible stays capped at Phase 16.
6. **Decision tracking → PROGRESS_LOG + this doc**, per issue.

### B. Phase routing — everything folds into Phase 17
- **Phase 11 (HUD):** fix is coded; Phase 17 owns the final rebuild + in-game verification.
- **Phase 12 (Arabic):** word command + bundled font handled here (see Group O).
- **Phase 14 (Cloud/Discord/Onboarding):** redone here with proper chest GUIs, not stubs.
- **Phase 16 (Release QA):** redone/completed here after restoration.

### C. Tooling (confirms 17.10 / Q8)
- **Physical items kept:** Omni-Tool, Deleter, Square marker, Triangle marker. Chisel, Lumina, Hexagon, Rectangle, and Triangle collapse into **Omni-Tool modes**.

### D. AI textures (resolves 17.22 / P3)
- **No API key.** Primary: **Pollinations.ai** (keyless HTTP GET, request texture size directly). Automatic fallback: **Stable Horde** (anonymous key `0000000000`). HuggingFace SDXL dropped (now needs a token + heavy rate limits).

### E. Silent resource pack (resolves 17.1 / Req §6)
- **Auto-accept mixin + `silentPack` config toggle.** Server-forced (`required=true`) packs still show a prompt, so they do not achieve silence. The mixin auto-confirms the vanilla pack prompt; the toggle lets an owner restore it. Safe because the mod ships client+server and is pinned to MC 1.21.1.

### F. Permissions (resolves Q7)
- Restore **LuckPerms / Fabric Permissions API** with per-command nodes. **Fallback when absent → vanilla OP level** (mirrors the old `permissionFallback*` OP-level scheme in the old config.json).

### G. Cloud Vault / Marketplace
- **Already deployed.** Worker `cb-cloud-vault`, KV id `ee49b7a0d7584b539b1c986bdf8428ee`, endpoint `cb-cloud-vault.cbbblocksvault.workers.dev`. Marketplace, share-codes, and cloud backups all ride this. Config keys: `cloudShareEnabled`, `cloudShareUrl`.

### H. Exports (confirms 17.15 / Group K)
- In scope: **.litematic + .schem + standalone vanilla resource pack** (CustomModelData), alongside PNG/JSON/CSV/ZIP and localhost download links.

### I. Ambitious features — all KEPT in Phase 17
- Holograms (`/cb preview` + offhand projection), Screen Eyedrop color picker, and rembg-style AI background removal (plus the three classic BG modes).

### J. Sample blocks (resolves R3)
- **Assistant picks 5–10 good starter blocks** for fresh installs.

### K. Migration data (confirmed available)
Old-format data was provided and verified present, validating the first-boot MigrationManager scope:
- `slots.json.gz` (gzip; old `slots` key, `lightLevel` field, embedded `tabIconTexture` base64)
- `textures/slot_N.dat` (PNG bytes, wrong extension)
- `.json.gz`: achievements, favorites, players, placement_stats
- `categories.json` (rich objects + assignment map), `magic_items.json`, `hud-config-server.json`
- old `config.json` (`maxSlots: 850`, OP-level permission fallbacks, `voiceMode: arabic` → scrapped)

---

## 📚 Legacy Audit Reference — Old Command Surface

Old project confirmed **monolithic**: `GuiManager.java` 10,324 lines; `CustomBlockCommand.java` 6,784 lines — the exact anti-pattern the rebuild's file-size gates prevent. **Restore behavior, never structure.**

Full old `/cb` subcommand set extracted from `CustomBlockCommand.java` (reference for the 17.11 audit):

```
_internal_setglobalbg achievements add addshape ai apply arabic audit backup bgstudio
blockadd blocks blockscat blockscategory brush bulk bulkblockadd bulkcolor bulkdelete
bulkduplicate bulkexport bulkfavorite bulkgui bulklock bulkmove bulkproperty bulkrecolor
bulkreid bulkrename bulkshape bulksound bulkunfavorite bulkunlock cache cb chisel clear
clearallfaces clearface clearshape colors config confirm create customblock customcolor
customtriangle delete deletedblocks deleter diagnostics dress dupe duplicate edge edithud
editmagicitems editor expiry export exportall exportblock exportcategory exportpng
facechangegui favorite find full give givecategory givedisplayblock gradient gui help
hexagon history hologram import importblock importcategory importfolder list listgui lock
macro magicitems market marketplace menu note off on palette panic particles pause recent
record recover rectangle redo redogui reid reload remove removeshape rename reset resize
resourcepack restore resume retexture rp run safety save screenshot script scriptgui search
setcollision setface setglow sethardness setshape setsound settabicon settings shapeeditor
shapelist shapepreview sharecategory show showbrokenblocks showcase snapshots sounds square
stop swapid swapname sync template text tolerance triangle trianglemode undo undogui
unfavorite unlock unsuppress voice welcome
```

---

## ✅ Locked Decisions — Round 2 (2026-06-09)

### L. Fonts & config defaults
- **Fonts:** English/Latin text blocks → **Rockwell Condensed**; Arabic letters/words → **arabtype.ttf** (Arabic-capable). Both bundled in the JAR. Rockwell is never used for Arabic.
- **Default texture size → 256px** (up from old 128 / current 64) for higher fidelity. ⚠️ At up to 1000 slots this materially increases generated resource-pack size + client download + VRAM, so keep it **server-configurable** (owners can lower to 128/64).
- **maxUndoDepth → 100** (old value; clamp cap stays 1000).
- **Discord webhook → blank by default** — do NOT ship the personal webhook found in the old config.json.
- **Full config schema:** under per-group review (Batch 1+ in PROGRESS_LOG). `voiceMode` dropped (voice scrapped); AI key fields reworked for keyless Pollinations.

---

## ✅ Locked Decisions — Config Batch 1 (2026-06-09)

### M. Config Batch 1 results
- **Background removal:** fully configurable (all knobs exposed in-game) AND switch the color-distance method to **CIE Lab** (perceptually accurate; replaces the old `bgRemovalUseYcbcr` YCbCr default) — per prior old-version agreement.
- **Permissions:** **3-tier** scheme in config (use / edit / admin) instead of the old 12-action granular set; still backs the LuckPerms → vanilla-OP fallback.
- **Sounds & particles:** restore **per-event category toggles** (success, error, gui, selection, bulk_complete, rp_regenerate, achievement) for both sounds and particles.
- **Limits & performance:** restore all, but **raise** byte/size limits to suit 256px textures (`maxGifSizeMb`, `maxGifStripBytes`, `texturePayloadsPerTick`, etc.).
- **Auto-backup timer:** keep a timed auto-backup with a **configurable interval** (default 30 min); snapshots are folded into `/cb backup`.

---

## ✅ Locked Decisions — Config Batch 2 (2026-06-09)

### N. Config Batch 2 results
- **Undo:** `undoMode = both` (per-player + server-wide history); keep the “confirm before bulk affecting >10 blocks” guard (`bulkConfirmThreshold`).
- **Networking:** keep `resourcePackPort` (8080), `serverIpOverride`, and `cloudPackSecret` configurable, **plus auto-detect server IP** when no override is set.
- **DidYouMean (typo correction):** default **smart**, configurable (smart / always / off).
- **Display & appearance:** expose all as configurable — `hideCustomBlockText`, `hideCategoryBadge`, the four triangle-marker hex colors, and hologram `height`/`color`.
- **AI options (keyless Pollinations):** keep both `aiMaxVariations` (default 3) and `aiTextureStyle` (default "pixel_art"); legacy key fields (`aiApiKey`/`aiWorkerUrl`/`aiServerToken`) become unused/removed since no key is required.

### Config coverage
Every field in the old `config.json` is now resolved — slots, textures, bg-removal, limits, undo, permissions, networking, sounds/particles, display, triangle colors, snapshots/backup, AI, marketplace, discord, hologram, and Arabic auto-connect. `voiceMode` is the only dropped field (voice scrapped).
