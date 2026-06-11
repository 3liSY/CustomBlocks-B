# 🧱 CustomBlocks — Engineering Bible
> **Version:** 2.0.0 | **Status:** Planning | **Author:** 3liSY / SrbGamer  
> *This document is the single source of truth for the entire project lifecycle.*

---

## 📋 Table of Contents
1. [Project Charter](#1-project-charter)
2. [Software Requirements Specification (SRS)](#2-software-requirements-specification)
3. [System Architecture](#3-system-architecture)
4. [Client Architecture](#4-client-architecture)
5. [Network Architecture](#5-network-architecture)
6. [Image Processing Pipeline](#6-image-processing-pipeline)
7. [Configuration System](#7-configuration-system)
8. [Development Roadmap (Phases)](#8-development-roadmap)
9. [Engineering Protocols](#9-engineering-protocols)
10. [Risk Register](#10-risk-register)
11. [Templates & Appendix](#11-templates--appendix)

---

## 1. Project Charter

### What We're Building
A Minecraft Fabric mod (MC 1.21.1) that lets players and server admins create custom-textured blocks in real-time using image URLs — no restarts, no manual resource pack downloads, no client-side installs required.

### Why From Scratch
- Clean, well-documented codebase from day 0
- Full ownership and understanding of every system
- No inherited tech debt or undocumented decisions
- Every architectural choice will be logged in this document
- The old codebase grew to ~2MB of Java with a 623KB GUI monolith and 414KB command monolith — we will not repeat those mistakes

### Goals
| Goal | Description |
|---|---|
| **Primary** | Functional mod that delivers the full feature set described below |
| **Secondary** | Fully documented codebase with decision logs at every major step |
| **Tertiary** | Clean architecture that can be extended or maintained by any future developer |

### Success Criteria
- [ ] Players can create a custom block with a URL in under 10 seconds
- [ ] Texture updates propagate to all connected clients without restarts
- [ ] Supports up to 1028 unique custom blocks simultaneously
- [ ] Zero registry mismatch errors on server startup
- [ ] All commands in the `/cb` tree work as specified
- [ ] Undo/Redo works per-player without affecting others
- [ ] Cloud sync of block data via Cloudflare Worker
- [ ] Arabic letter/word system fully operational
- [ ] AI texture generation and command parsing functional
- [ ] Video-to-texture pipeline working
- [ ] Macro recording and playback works end-to-end

### Out of Scope (v1.0)
- Forge / NeoForge support
- Bedrock Edition
- Cross-version compatibility (only 1.21.1)
- Paid/premium tiers
- Mobile clients
- Full permission system (planned for v1.1 — Fabric Permissions API / LuckPerms)
- Achievement system (post-v1.0)
- Hologram preview system (post-v1.0)
- Showcase blocks (post-v1.0)
- Voice/Personality chat system (post-v1.0)

---

## 2. Software Requirements Specification

### 2.1 Functional Requirements

#### FR-01 — Block Slot System
| ID | Requirement |
|---|---|
| FR-01-1 | The mod SHALL pre-register exactly 1028 generic `SlotBlock` instances at game startup |
| FR-01-2 | Each slot SHALL be assignable to a `SlotData` object at runtime |
| FR-01-3 | `SlotData` SHALL be immutable; all mutations create a new snapshot via `.update()` |
| FR-01-4 | The `SlotManager` SHALL track which slots are free, occupied, or reserved |

#### FR-02 — Texture Delivery Pipeline
| ID | Requirement |
|---|---|
| FR-02-1 | The mod SHALL accept any valid image URL as a texture source |
| FR-02-2 | `ServerPackGenerator` SHALL generate a ZIP resource pack in memory (no disk writes) |
| FR-02-3 | `ResourcePackServer` SHALL run an embedded HTTP server on a configurable port |
| FR-02-4 | The server SHALL push a native Minecraft Resource Pack packet to all clients on update |
| FR-02-5 | The client SHALL auto-apply the resource pack with no user interaction |
| FR-02-6 | GIF animations SHALL be supported via Minecraft's native animation format (MCMETA) |
| FR-02-7 | Modded clients SHALL receive textures via `ConfigSyncPayload` + client-side generation (bypassing HTTP pack for modded) |
| FR-02-8 | Large textures SHALL be split via `ChunkedTexturePayload` when HTTP delivery is unavailable |

#### FR-03 — Per-Face Texturing
| ID | Requirement |
|---|---|
| FR-03-1 | Each block face (top, bottom, north, south, east, west) SHALL support an independent texture URL |
| FR-03-2 | A single URL MAY be applied to all faces simultaneously as a shorthand |

#### FR-04 — Block Attributes
| ID | Requirement |
|---|---|
| FR-04-1 | Light emission SHALL be configurable from 0 to 15 |
| FR-04-2 | Break hardness SHALL be configurable per block |
| FR-04-3 | Custom place, break, and step sounds SHALL be assignable per block |
| FR-04-4 | Collision shape SHALL be configurable via multi-part bounding boxes |
| FR-04-5 | Drop behavior SHALL be configurable per block via `DropConfigManager` |

#### FR-05 — Command System (`/customblock` or `/cb`)
All commands listed below are required. Priority groupings:

**P0 (Core — must work first):**
- `create`, `delete`, `retexture`, `list`, `gui`

**P1 (Standard — required for v1.0):**
- `rename`, `dupe`, `reid`, `setglow`, `sethardness`, `setsound`, `give`, `undo`, `redo`
- `reload`, `config`

**P2 (Advanced — required for v1.0):**
- `setshape`, `addshape`, `removeshape`, `saveshape`, `loadshape`, `setcollision`
- `export`, `importfolder`, `admingui`, `recent`
- `colors` — open color hub GUI
- `tolerance` — configure background removal sensitivity
- `snapshots` — open snapshot manager GUI
- `panic` — emergency 2-step rollback to last snapshot
- `edithud` — open HUD drag editor
- `arabic` — open Arabic letter browser / import / text commands
- `macro` — record, play, list, delete macros
- `deleter` — manage deleter tool
- `license` — display license info

#### FR-06 — Undo / Redo Engine
| ID | Requirement |
|---|---|
| FR-06-1 | Every block state change SHALL be recorded in the acting player's history stack |
| FR-06-2 | Undo/Redo history SHALL be **per-player** and completely isolated |
| FR-06-3 | History SHALL persist for the duration of the server session |
| FR-06-4 | History SHALL be capped per-player to prevent memory leaks |

#### FR-07 — In-Game GUI
| ID | Requirement |
|---|---|
| FR-07-1 | GUI SHALL support contexts: MAIN_MENU, BLOCK_EDITOR, TEXTURE_PICKER, SHAPE_EDITOR, COLORS_HUB, SNAPSHOT_MANAGER, CATEGORY_BROWSER, ARABIC_BROWSER, MACRO_MANAGER, IMPORT_CONFLICT, UNDO_PICKER |
| FR-07-2 | Navigation history SHALL use an `ArrayDeque` back-stack |
| FR-07-3 | GUI SHALL include: buttons, sliders, text inputs, color pickers, sorting, pagination |
| FR-07-4 | Color picker SHALL support HSV/RGB modes |
| FR-07-5 | GUI state SHALL be serializable via `GuiState` snapshots |
| FR-07-6 | Anvil-based text input SHALL be available for naming/renaming via `AnvilPromptManager` |

#### FR-08 — Tools (Items)
| Item | Behavior |
|---|---|
| Lumina Brush | Right-click block → cycle glow level |
| Amethyst Chisel | Right-click block → alter hardness/shape |
| Deleter | Right-click block → safely remove + clear metadata (shift = instant) |
| Color Square | Dynamic color change on block interaction |
| Color Triangle | Precision marking and metadata tagging |
| Golden Hexagon | Admin-only advanced block manipulation |
| Rectangle Tool | Click two corners → define rectangular textured area |

#### FR-09 — Import / Export
| ID | Requirement |
|---|---|
| FR-09-1 | `export <id>` SHALL save block metadata to a JSON file |
| FR-09-2 | `importfolder <path>` SHALL load all `.json` exports from a directory in batch |
| FR-09-3 | Export format SHALL be human-readable and version-tagged |
| FR-09-4 | Import conflicts SHALL be handled with a GUI showing resolution options |

#### FR-10 — Search & Filtering
| ID | Requirement |
|---|---|
| FR-10-1 | `SearchIndex` SHALL index all registered custom blocks |
| FR-10-2 | `SearchFilter` SHALL support filtering by name, color, category, attributes |
| FR-10-3 | Search SHALL return results in real-time as the user types (no lag) |
| FR-10-4 | Sorting SHALL support multiple modes via `SortMode` (alphabetical, date, color, etc.) |

#### FR-11 — Image Processing
| ID | Requirement |
|---|---|
| FR-11-1 | Background removal SHALL support three modes: `corners_only`, `corners_and_trapped`, `none` |
| FR-11-2 | Tolerance SHALL be configurable (0–100 scale) per the config system |
| FR-11-3 | Image downloads SHALL send browser-like headers (User-Agent, Accept, Referer) to bypass CDN hotlink protection |
| FR-11-4 | Images SHALL be resized using bicubic interpolation (no pixelation) |
| FR-11-5 | Invalid/unreachable URLs SHALL fail gracefully with a user-friendly error message |
| FR-11-6 | Color detection SHALL analyze the dominant color of images for automatic tinting |
| FR-11-7 | Color recoloring SHALL support per-pixel replacement with configurable tolerance |

#### FR-12 — Category System
| ID | Requirement |
|---|---|
| FR-12-1 | Blocks SHALL be organizable into named categories |
| FR-12-2 | Auto-categorize SHALL suggest categories based on block properties |
| FR-12-3 | Categories SHALL support custom display blocks as icons |
| FR-12-4 | Category management SHALL have a full GUI (create, rename, delete, merge, sort) |

#### FR-13 — Snapshot & Backup System
| ID | Requirement |
|---|---|
| FR-13-1 | Snapshots SHALL capture the full state of ALL slots (compressed JSON) |
| FR-13-2 | Snapshots SHALL be saved to `config/customblocks/snapshots/` |
| FR-13-3 | Auto-snapshots SHALL be taken at a configurable interval |
| FR-13-4 | Manual snapshots SHALL support custom naming (not just timestamps) |
| FR-13-5 | Snapshots SHALL be lockable to prevent auto-overwrite |
| FR-13-6 | Snapshot restore SHALL correctly persist the selected snapshot across restarts |
| FR-13-7 | `/cb panic` SHALL perform emergency 2-step rollback |
| FR-13-8 | Maximum 20 snapshots stored at any time |
| FR-13-9 | `BackupManager` SHALL maintain independent automatic backups separate from snapshots |

#### FR-14 — Color Tools Ecosystem
| ID | Requirement |
|---|---|
| FR-14-1 | `ColorDetection` SHALL analyze block textures and determine color families |
| FR-14-2 | `ColorVariantService` SHALL manage color variant relationships between blocks |
| FR-14-3 | `ColorNames` SHALL provide human-readable color names from hex values |
| FR-14-4 | `PlayerPaletteManager` SHALL save per-player favorite color palettes |
| FR-14-5 | Color tools SHALL feel instant (no client-side skip delay) |
| FR-14-6 | Hex change wizard SHALL support batch recoloring of existing blocks |

#### FR-15 — Template System
| ID | Requirement |
|---|---|
| FR-15-1 | Block configurations SHALL be saveable as reusable templates |
| FR-15-2 | Templates SHALL be loadable to create new blocks with pre-set attributes |

#### FR-16 — Trash / Recycle Bin
| ID | Requirement |
|---|---|
| FR-16-1 | Deleted blocks SHALL go to a trash/recycle bin instead of permanent deletion |
| FR-16-2 | Trash SHALL support recovery of deleted blocks |
| FR-16-3 | Trash SHALL auto-purge after a configurable retention period |

#### FR-17 — Block Management
| ID | Requirement |
|---|---|
| FR-17-1 | Blocks SHALL support locking to prevent modification via `LockManager` |
| FR-17-2 | Blocks SHALL support per-block text notes/annotations via `BlockNotesManager` |
| FR-17-3 | Favorites SHALL allow players to bookmark blocks via `FavoritesManager` |
| FR-17-4 | Drafts SHALL allow work-in-progress blocks before publishing via `DraftManager` |

#### FR-18 — HUD Overlay System
| ID | Requirement |
|---|---|
| FR-18-1 | An in-game HUD SHALL display block info when looking at a custom block |
| FR-18-2 | HUD layout SHALL be editable via a drag editor (`/cb edithud`) |
| FR-18-3 | HUD config SHALL sync from server to client |

#### FR-19 — Arabic Letter & Word Block System
| ID | Requirement |
|---|---|
| FR-19-1 | `/cb arabic import <path>` SHALL batch-import letter PNG sets from color folders |
| FR-19-2 | `/cb arabic` SHALL open a 54-slot browser GUI with color tabs and letter grid |
| FR-19-3 | `/cb arabic give <letter> <color>` SHALL give a specific letter block |
| FR-19-4 | `/cb arabic text <color> <text>` SHALL create blocks for each character |
| FR-19-5 | Word block generator SHALL render Arabic text using bundled `arabtype.ttf` with configurable size/outline/background |
| FR-19-6 | Auto-joining SHALL detect neighboring letter blocks and swap to correct form (initial/medial/final/isolated) based on Arabic joining rules |

#### FR-20 — AI Features
| ID | Requirement |
|---|---|
| FR-20-1 | `AiCommandParser` SHALL parse natural-language player input into valid `/cb` commands |
| FR-20-2 | `AiTextureGenerator` SHALL generate block textures from text descriptions (requires external API key, opt-in only) |

#### FR-21 — Video-to-Texture
| ID | Requirement |
|---|---|
| FR-21-1 | The mod SHALL decode MP4/H264 video files via jcodec |
| FR-21-2 | Video frames SHALL be extractable as texture sequences for animated blocks |

#### FR-22 — Macro System
| ID | Requirement |
|---|---|
| FR-22-1 | Players SHALL be able to record sequences of `/cb` commands as named macros |
| FR-22-2 | Macros SHALL be replayable with a single command |
| FR-22-3 | Macro management SHALL support list, delete, and rename operations |

#### FR-23 — Cloud Vault Sync
| ID | Requirement |
|---|---|
| FR-23-1 | Block data SHALL be uploadable to a Cloudflare Worker KV store |
| FR-23-2 | Block data SHALL be downloadable from Cloud Vault on another server using a share code |
| FR-23-3 | Cloud sync SHALL be opt-in via config (`cloudShareEnabled`, `cloudShareUrl`) |
| FR-23-4 | The Cloudflare Worker source SHALL be bundled in `cloud-vault-worker/` |

#### FR-24 — Discord Integration
| ID | Requirement |
|---|---|
| FR-24-1 | Configurable webhook notifications SHALL be sendable to Discord channels |
| FR-24-2 | Discord integration SHALL be opt-in via config |

#### FR-25 — Onboarding & Help
| ID | Requirement |
|---|---|
| FR-25-1 | First-time users SHALL see contextual hints via `FirstUseHints` |
| FR-25-2 | `TipPool` SHALL provide rotating gameplay tips |
| FR-25-3 | `HelpRegistry` SHALL map every command to a help entry |
| FR-25-4 | `DidYouMean` SHALL suggest corrections for mistyped commands |
| FR-25-5 | Sample blocks SHALL be loadable for new servers via `SampleBlocksLoader` |

#### FR-26 — Diagnostics
| ID | Requirement |
|---|---|
| FR-26-1 | `DiagnosticsHelper` SHALL collect system state for debugging |
| FR-26-2 | `IncidentRecorder` SHALL log errors with context for post-mortem analysis |

#### FR-27 — Bulk Operations
| ID | Requirement |
|---|---|
| FR-27-1 | `BulkScope` SHALL define scoped batch operations on multiple blocks |
| FR-27-2 | Batch recoloring, retexturing, and deletion SHALL be supported |

#### FR-28 — Text Sanitization
| ID | Requirement |
|---|---|
| FR-28-1 | `TextSanitizer` SHALL detect and repair mojibake (CP1252 → UTF-8 corruption) in block names and metadata |
| FR-28-2 | Build SHALL fail if mojibake sequences are detected in source files (Gradle verification task) |

---

### 2.2 Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Block creation SHALL propagate to clients in under 3 seconds on a LAN server |
| NFR-02 | Performance | GUI SHALL render at full game FPS with no dropped frames |
| NFR-03 | Scalability | System SHALL handle 1028 custom blocks without registry errors |
| NFR-04 | Reliability | Server SHALL NOT crash if a texture URL is invalid or unreachable |
| NFR-05 | Security | The embedded HTTP server SHALL only serve the resource pack ZIP (no directory traversal) |
| NFR-06 | Persistence | All `SlotData` SHALL survive server restarts (saved to disk on change) |
| NFR-07 | Compatibility | Mod SHALL only target Fabric loader on MC 1.21.1 |
| NFR-08 | Maintainability | Every class SHALL have a header comment explaining its responsibility |
| NFR-09 | Observability | All major operations SHALL emit structured log lines (prefixed `[CustomBlocks]`) |
| NFR-10 | Stability | Keep-alive mixins SHALL prevent client/server timeouts during large pack downloads |
| NFR-11 | Encoding | All source files SHALL be UTF-8 with no BOM. Mojibake verification SHALL run on every build |
| NFR-12 | Sound Safety | `BLOCK_NOTE_BLOCK_*` constants SHALL always use `.value()` — verified by Gradle task |
| NFR-13 | File Safety | Atomic file writes SHALL prevent data corruption from mid-save crashes |

---

## 3. System Architecture

### Package Tree (Server Side)
```
com.customblocks
├── CustomBlocksMod.java              ← ModInitializer: registers blocks, items, commands, payloads
├── CustomBlocksConfig.java           ← Config loader/saver (grows per-phase)
├── BlockFinder.java                  ← Utility: find registered blocks optimally
├── TextSanitizer.java                ← Mojibake detection & repair dictionary
│
├── core/                             ← Heart of the system
│   ├── SlotManager.java              ← Single point of truth for all 1028 slot assignments
│   ├── SlotData.java                 ← Immutable block state model + .update() builder
│   ├── SlotDataStore.java            ← [NEW] All disk I/O for slot data (JSON read/write)
│   ├── UndoManager.java              ← Per-player undo/redo stacks
│   ├── SnapshotManager.java          ← Full-state snapshots (save/restore/lock)
│   ├── BackupManager.java            ← Automatic backups (independent from snapshots)
│   ├── CategoryManager.java          ← Block organization by categories
│   ├── Category.java                 ← Category data model
│   ├── AutoCategorizeManager.java    ← Suggest categories based on block properties
│   ├── CategoryDisplayBlockManager.java ← Custom display blocks as category icons
│   ├── ColorDetection.java           ← Analyze dominant color of textures
│   ├── ColorVariantService.java      ← Manage color variant relationships
│   ├── ColorNames.java               ← Hex → human-readable color name
│   ├── PlayerPaletteManager.java     ← Per-player saved color palettes
│   ├── TemplateManager.java          ← Save/load block templates
│   ├── MacroManager.java             ← Record/replay command sequences
│   ├── TrashManager.java             ← Soft-delete recycle bin with recovery
│   ├── LockManager.java              ← Lock blocks from modification
│   ├── BlockNotesManager.java        ← Per-block text annotations
│   ├── FavoritesManager.java         ← Bookmark favorite blocks
│   ├── DraftManager.java             ← Work-in-progress block drafts
│   ├── DropConfigManager.java        ← Per-block drop behavior config
│   ├── SearchIndex.java              ← Real-time block search index
│   ├── SearchFilter.java             ← Advanced search with filters
│   ├── HistoryTracker.java           ← Action history beyond undo/redo
│   ├── BulkScope.java                ← Scoped batch block operations
│   ├── DiagnosticsHelper.java        ← System state collection for debugging
│   ├── IncidentRecorder.java         ← Error logging with context
│   ├── SampleBlocksLoader.java       ← Load sample blocks for new servers
│   ├── FirstUseHints.java            ← First-time user contextual hints
│   ├── TipPool.java                  ← Rotating gameplay tips
│   ├── OnboardingManager.java        ← New user onboarding flow
│   └── WelcomeManager.java           ← Welcome screen state
│
├── block/
│   └── SlotBlock.java                ← The actual Minecraft Block subclass
│
├── command/
│   ├── CommandRegistrar.java         ← [NEW] Registers the /cb command tree (delegates to handlers)
│   ├── handlers/                     ← [NEW] One handler class per command group
│   │   ├── CreationCommands.java     ← create, delete, retexture, rename, dupe, reid
│   │   ├── PropertyCommands.java     ← setglow, sethardness, setsound, setcollision
│   │   ├── ShapeCommands.java        ← setshape, addshape, removeshape, saveshape, loadshape
│   │   ├── HistoryCommands.java      ← undo, redo, snapshots, panic
│   │   ├── UtilityCommands.java      ← give, list, export, importfolder, reload, config
│   │   ├── GuiCommands.java          ← gui, admingui, edithud, colors, recent
│   │   ├── ArabicCommands.java       ← arabic import/gui/give/text/word
│   │   ├── MacroCommands.java        ← macro record/play/list/delete
│   │   └── AdminCommands.java        ← license, deleter, cloud sync commands
│   ├── DidYouMean.java               ← Suggest corrections for mistyped commands
│   └── HelpRegistry.java             ← Map every command to a help entry
│
├── item/
│   ├── LuminaBrushItem.java          ← Paint glow levels
│   ├── AmethystChiselItem.java       ← Alter hardness/shape
│   ├── DeleterItem.java              ← Safely remove blocks (shift = instant)
│   ├── ColorSquareItem.java          ← Dynamic color change on interaction
│   ├── ColorTriangleItem.java        ← Precision marking and metadata
│   ├── GoldenHexagonItem.java        ← Admin advanced manipulation
│   └── RectangleToolItem.java        ← Define rectangular textured areas
│
├── gui/
│   ├── GuiEngine.java                ← [NEW] Core render loop + widget primitives
│   ├── GuiMode.java                  ← Context enum (all screen types)
│   ├── GuiState.java                 ← Serializable state snapshot for back-stack
│   ├── GuiLayout.java                ← Layout constants and slot mappings
│   ├── SortMode.java                 ← Block list sorting options
│   ├── AnvilPromptManager.java       ← In-game text input via anvil UI
│   ├── CbScreenHandler.java          ← Custom ScreenHandler for chest-based GUIs
│   ├── ChatHelper.java               ← Formatted chat message utilities
│   ├── FeedbackHelper.java           ← Rich in-game feedback messages
│   ├── ColorLibrary.java             ← Color presets and selection matrix
│   ├── ColorPickerHelper.java        ← HSV/RGB color picker logic
│   └── screens/                      ← [NEW] One class per screen (NO monolith)
│       ├── MainMenuScreen.java       ← Overview of features
│       ├── BlockEditorScreen.java     ← Deep editing of a specific block
│       ├── TexturePickerScreen.java   ← Selecting/previewing URLs
│       ├── ShapeEditorScreen.java     ← Modifying bounding boxes
│       ├── ColorsHubScreen.java       ← Color management hub
│       ├── CategoryBrowserScreen.java ← Browse/manage categories
│       ├── SnapshotManagerScreen.java ← View/restore/lock snapshots
│       ├── ArabicBrowserScreen.java   ← Arabic letter/word browser
│       ├── MacroManagerScreen.java    ← Macro management
│       ├── UndoPickerScreen.java      ← Visual undo history picker
│       ├── ImportConflictScreen.java  ← Resolve import conflicts
│       ├── ConfigScreen.java          ← In-game config editor
│       └── AdminScreen.java          ← Server admin panel
│
├── image/                            ← [NEW] Standalone image processing (was 94KB in root)
│   ├── ImageProcessor.java           ← Download, decode, resize, background removal
│   ├── BackgroundRemover.java        ← [NEW] Extracted: corner/trapped/smart BG removal
│   ├── ColorReplacer.java            ← [NEW] Extracted: per-pixel color replacement
│   └── ImageDownloader.java          ← [NEW] Extracted: URL download with browser headers
│
├── network/
│   ├── NetworkManager.java           ← Coordinates all payload registration & dispatch
│   ├── ServerPackGenerator.java      ← Builds ZIP resource pack in memory
│   ├── ResourcePackServer.java       ← Embedded HTTP server (single endpoint)
│   ├── payloads/                     ← [NEW] Organized payload definitions
│   │   ├── FullSyncPayload.java      ← Send all 1028 slots on client join
│   │   ├── SlotUpdatePayload.java    ← Broadcast single slot change
│   │   ├── ConfigSyncPayload.java    ← Sync server config to client
│   │   ├── HudConfigSyncPayload.java ← Sync HUD settings
│   │   ├── ChunkedTexturePayload.java ← Fallback chunked texture transfer
│   │   ├── AnimSettingsPayload.java   ← Animation settings
│   │   ├── SyncRequestPayload.java   ← Client requests re-sync
│   │   ├── SyncCompletePayload.java  ← Server confirms sync done
│   │   ├── OpenAnimGuiPayload.java   ← Trigger anim GUI on client
│   │   ├── OpenHudEditorPayload.java ← Trigger HUD editor on client
│   │   └── RpPausePayload.java       ← Pause resource pack pushes
│   └── sync/
│       └── TextureQueue.java         ← Queue texture updates to avoid flooding
│
├── arabic/
│   ├── ArabicBlockRegistry.java      ← Maps (letter, color) → block ID, persists to JSON
│   ├── ArabicLetterMap.java          ← Letter → Unicode data + joining rules
│   └── ArabicWordRenderer.java       ← [NEW] Java2D Arabic text rendering with arabtype.ttf
│
├── ai/                               ← [NEW] Extracted from old root + assistant/
│   ├── AiCommandParser.java          ← Natural-language → /cb commands
│   └── AiTextureGenerator.java       ← AI-generated textures (opt-in, API key required)
│
├── video/                            ← [NEW] Video-to-texture pipeline
│   └── VideoDecoder.java             ← jcodec MP4/H264 → texture frames
│
├── cloud/                            ← [NEW] Cloud sync client-side logic
│   └── CloudVaultClient.java         ← Upload/download blocks to Cloudflare Worker
│
├── discord/                          ← [NEW] Extracted from old root
│   └── DiscordWebhook.java           ← Send notifications to Discord channels
│
└── mixin/
    ├── ClientKeepAliveMixin.java     ← Prevent client timeout during large pack download
    └── ServerKeepAliveMixin.java     ← Prevent server timeout during pack operations
```

### Package Tree (Client Side)
```
com.customblocks.client
├── CustomBlocksClient.java           ← ClientModInitializer: registers screens, payload receivers, textures
├── ResourcePackGenerator.java        ← Client-side texture generation (for modded clients)
├── HudConfig.java                    ← HUD overlay position/visibility settings
├── gui/
│   ├── HudEditorScreen.java          ← Drag editor for HUD overlay layout
│   ├── AnimBlockScreen.java          ← GIF/animation texture editor
│   └── DevConsoleScreen.java         ← Admin debug log viewer
└── texture/
    └── TextureCache.java             ← Client-side texture caching
```

### Data Flow (Texture Update)
```
Player Input (URL)
      │
      ▼
CommandRegistrar → CreationCommands.create()
      │
      ▼
ImageProcessor.download(url)  ←── downloads with browser headers
      │
      ├──► BackgroundRemover.remove(image, mode, tolerance)
      │
      ▼
SlotManager.assign(slotId, newSlotData)
      │
      ├──► UndoManager.push(playerId, previousState)
      │
      ├──► SlotDataStore.save(slotId, newSlotData)
      │
      ├──► SearchIndex.reindex(slotId)
      │
      ├──► ColorDetection.analyze(texture) → cache color family
      │
      ▼
ServerPackGenerator.build()  ←── generates ZIP in memory
      │
      ▼
ResourcePackServer.mount(zipBytes)
      │
      ├──► Vanilla clients: Server sends ResourcePack packet → HTTP download
      │
      ├──► Modded clients: ConfigSyncPayload → client ResourcePackGenerator
      │
      ▼
Client receives textures → chunk re-render
```

### Key Design Rules
1. **`SlotData` is always immutable.** Use `.update()` to get a modified copy. Never mutate in place.
2. **`SlotManager` is the single point of truth.** Nothing else should modify slot assignments directly.
3. **The HTTP server only serves one file:** `/customblocks_pack.zip`. Period.
4. **All disk I/O goes through `SlotDataStore`.** No other class reads/writes slot data to files.
5. **GUI state is always serialized before navigation.** Never assume the GUI knows its own history.
6. **No monolith files.** If a file exceeds ~500 lines, it must be split. The old 623KB `GuiManager` and 414KB `CustomBlockCommand` MUST NOT happen again.
7. **Command handlers are split by domain.** One handler class per command group, registered via `CommandRegistrar`.
8. **Screen classes are standalone.** Each GUI screen is its own class in `gui/screens/`. The `GuiEngine` provides shared primitives.
9. **Atomic file writes.** All persistence uses write-to-temp + rename to prevent half-written files from corrupting worlds.
10. **Client code never touches server state.** Client-side prediction is allowed but the server is always authoritative.

---

## 4. Client Architecture

> The old project had 86KB of client code with no documentation. This section prevents that.

### `CustomBlocksClient.java` (ClientModInitializer)
**Responsibilities:**
- Register client-side payload receivers (all payloads from `network/payloads/`)
- Register client GUI screens (`HudEditorScreen`, `AnimBlockScreen`, `DevConsoleScreen`)
- Initialize `TextureCache`
- Pre-warm `cachedColorFamily` on `SlotData` when receiving sync payloads
- Trigger `ResourcePackGenerator` for modded client texture generation

### Client-Side Resource Pack Generation
Modded clients (those with the mod installed) bypass the HTTP resource pack entirely:
1. Server sends `ConfigSyncPayload` with texture data
2. Client's `ResourcePackGenerator` generates textures locally from config
3. This avoids the PACK2 issue where HTTP pack guard blocks modded clients

### HUD Overlay
- `HudConfig.java` stores position/visibility/style settings
- `HudConfigSyncPayload` syncs from server
- `HudEditorScreen` provides a drag editor (Lunar-style)
- Renders block info (name, ID, texture, attributes) when looking at a custom block

### Keep-Alive Mixins
**Why these exist:** Large resource pack downloads can take several seconds. Minecraft's default keep-alive timeout will disconnect the player during this time.
- `ClientKeepAliveMixin` — extends client-side timeout tolerance during pack operations
- `ServerKeepAliveMixin` — extends server-side grace period

---

## 5. Network Architecture

### Payload Fleet

| Payload | Direction | Purpose |
|---|---|---|
| `FullSyncPayload` | Server → Client | Compressed delta of all 1028 slots on player join |
| `SlotUpdatePayload` | Server → Client | Single-slot change broadcast |
| `ConfigSyncPayload` | Server → Client | Sync config values (hex colors, settings) |
| `HudConfigSyncPayload` | Server → Client | Sync HUD overlay settings |
| `ChunkedTexturePayload` | Server → Client | Fallback: stream texture bytes in chunks when HTTP unavailable |
| `AnimSettingsPayload` | Server → Client | Animation settings for MCMETA-based GIFs |
| `SyncRequestPayload` | Client → Server | Client requests a re-sync |
| `SyncCompletePayload` | Server → Client | Confirms sync is finished |
| `OpenAnimGuiPayload` | Server → Client | Tell client to open animation editor |
| `OpenHudEditorPayload` | Server → Client | Tell client to open HUD editor |
| `RpPausePayload` | Server → Client | Pause resource pack pushes during batch operations |

### `TextureQueue` (Debouncing)
When multiple block changes happen rapidly (e.g., bulk import), `TextureQueue` batches them and triggers a single pack rebuild after a short debounce window. This prevents flooding clients with redundant resource pack downloads.

### HTTP Resource Pack Flow
```
ServerPackGenerator.build()
      │ generates ZIP containing:
      │   ├── pack.mcmeta
      │   ├── assets/customblocks/models/block/slot_X.json  (per used slot)
      │   ├── assets/customblocks/textures/block/slot_X.png  (per used slot)
      │   └── assets/customblocks/textures/block/slot_X.png.mcmeta  (if animated)
      │
      ▼
ResourcePackServer.mount(zipBytes)
      │
      ▼
Server sends ResourcePack packet with URL:
  http://<server-ip>:<port>/customblocks_pack.zip
      │
      ▼
Client HTTP GET → 200 OK → native engine reload → textures applied
```

**Critical rules for `ResourcePackServer`:**
- Only serves one endpoint: `/customblocks_pack.zip`
- No directory traversal — reject any other path
- Modded client guard: do NOT send HTTP pack to modded clients (they generate locally)
- Port must be configurable via config

---

## 6. Image Processing Pipeline

> The old project had a single 94KB `ImageProcessor.java`. We split it into focused modules.

### `ImageDownloader.java`
- Downloads images from URLs
- Sends browser-like headers: `User-Agent` (Chrome), `Accept`, `Referer`
- Handles 401/403 with user-friendly error messages ("right-click → copy image address")
- Supports WebP via URL rewriting to `wsrv.nl` proxy
- Timeout handling with graceful failure

### `ImageProcessor.java`
- Central coordinator: download → process → return bytes
- Bicubic interpolation for resizing (no nearest-neighbor pixelation)
- Pad-to-square for non-square images
- Convert to 128×128 PNG for block faces

### `BackgroundRemover.java`
Three modes:
1. **`corners_only`** — Sample corner pixels, flood-fill from edges
2. **`corners_and_trapped`** — Same + fill enclosed holes of the same color
3. **`none`** — Skip background removal entirely

Tolerance is configurable (0–100). `tolerance <= 0` removes nothing. Manual tolerance always wins over auto-detect.

### `ColorReplacer.java`
- Per-pixel color replacement with threshold matching
- Direct hex-swap mode (compare pixel against stored hex, threshold configurable)
- Used by hex change wizard for batch recoloring
- **Rule:** Never use player-mode "full" recoloring in batch operations (destroys designs). Always use edge mode.

---

## 7. Configuration System

> Start minimal. Expand per-phase. Every new config option must be documented here first.

### Phase 1 Config (Foundation)
```json
{
  "httpPort": 8123,
  "maxSlots": 1028
}
```

### Phase 4 Config (Textures)
```json
{
  "httpPort": 8123,
  "maxSlots": 1028,
  "bgRemovalMode": "corners_only",
  "bgRemovalTolerance": 30,
  "bgRemovalAutoDetect": false
}
```

### Phase 8+ Config (Full)
```json
{
  "httpPort": 8123,
  "maxSlots": 1028,
  "bgRemovalMode": "corners_only",
  "bgRemovalTolerance": 30,
  "bgRemovalAutoDetect": false,
  "snapshotIntervalMinutes": 30,
  "maxSnapshots": 20,
  "cloudShareEnabled": false,
  "cloudShareUrl": "",
  "discordWebhookUrl": "",
  "discordWebhookEnabled": false,
  "triangleRedHex": "#EE3333",
  "triangleGreenHex": "#33EE33",
  "triangleBlueHex": "#3333EE",
  "squareHex": "#3399FF",
  "arabicAutoJoin": true,
  "aiTextureEnabled": false,
  "aiTextureApiKey": ""
}
```

### Config Design Rules
- Config file: `config/customblocks/config.json`
- Config class grows incrementally — only add fields when the feature is being built
- All config fields have sensible defaults
- `/cb config` opens an in-game GUI editor
- Config changes SHALL NOT require a server restart unless explicitly noted
- Config sync to clients via `ConfigSyncPayload`

---

## 8. Development Roadmap

### Phase 0 — Foundation (Week 1)
> Goal: Dev environment running, empty mod loads in Minecraft.

- [ ] Set up Java 21 + IntelliJ IDEA
- [ ] Set up Fabric MDK (Mod Development Kit) with Loom 1.7.4
- [ ] Initialize Git repo with `.gitignore` for Minecraft mod projects
- [ ] Create `CHANGELOG.md` and this `Engineering Bible`
- [ ] Verify mod loads in MC 1.21.1 with a "Hello World" log message
- [ ] Set up full package structure (all packages, empty classes with header comments)
- [ ] Set up Gradle verification tasks: `verifyMojibake`, `verifySound`
- [ ] Add `TextSanitizer.java` with mojibake repair dictionary
- [ ] Add `fabric.mod.json` with correct metadata (name, author, license, links)
- [ ] Add `LICENSE` and `LICENSE-ar` files

**Milestone:** `gradle runClient` launches Minecraft with mod loaded, no errors. All verification tasks pass.

---

### Phase 1 — Block Slot System (Week 2)
> Goal: 1028 slots registered. No registry errors.

- [ ] Implement `SlotBlock.java` (extends Block, holds slot ID)
- [ ] Implement `SlotData.java` (immutable record/class + `.update()` builder)
- [ ] Implement `SlotManager.java` (register 1028 blocks at mod init)
- [ ] Implement `SlotDataStore.java` (JSON read/write with atomic file operations)
- [ ] Implement `BlockFinder.java`
- [ ] Minimal `CustomBlocksConfig.java` (httpPort, maxSlots)
- [ ] Verify: server starts with 1028 blocks in registry, no crashes

**Milestone:** Server starts with 1028 blocks in registry. No errors.

---

### Phase 2 — Persistence & Safety (Week 2-3)
> Goal: Block data survives server restarts. Backups work.

- [ ] Wire `SlotDataStore` ← `SlotManager` (auto-save on assign)
- [ ] On server start: load all saved `SlotData` → restore to `SlotManager`
- [ ] Handle corrupted/missing files gracefully (log warning, skip)
- [ ] Implement `BackupManager.java` (automatic backups)
- [ ] Implement `SnapshotManager.java` (full-state snapshots, naming, locking)
- [ ] Implement `TrashManager.java` (soft-delete recycle bin)

**Milestone:** Create a block, restart server, block still exists. Backups and snapshots save correctly.

---

### Phase 3 — Core Commands (Week 3-4)
> Goal: P0 + P1 commands work end-to-end.

- [ ] Implement `CommandRegistrar.java` (registers `/customblock` tree with `/cb` alias)
- [ ] Implement `handlers/CreationCommands.java` (create, delete, rename, dupe, reid)
- [ ] Implement `handlers/PropertyCommands.java` (setglow, sethardness, setsound)
- [ ] Implement `handlers/HistoryCommands.java` (undo, redo, snapshots, panic)
- [ ] Implement `handlers/UtilityCommands.java` (give, list, reload, config)
- [ ] Implement `DidYouMean.java` (command suggestions — use `"subcommand"` as arg name, NOT `"unknown_cb_tail"`)
- [ ] Implement `HelpRegistry.java`
- [ ] Add tab-completion for block IDs

**Milestone:** `create`, `delete`, `list`, `undo`, `redo`, `rename`, `dupe`, `setglow` all work correctly in-game.

---

### Phase 4 — Texture Pipeline (Week 4-5)
> Goal: Paste a URL → block changes texture for everyone.

- [ ] Implement `image/ImageDownloader.java` (URL download with browser headers)
- [ ] Implement `image/ImageProcessor.java` (coordinator: download → resize → return PNG bytes)
- [ ] Implement `image/BackgroundRemover.java` (3 modes + tolerance)
- [ ] Implement `image/ColorReplacer.java` (per-pixel color swap)
- [ ] Implement `network/ServerPackGenerator.java` (JSON model + texture → ZIP bytes)
- [ ] Implement `network/ResourcePackServer.java` (embedded HTTP, single endpoint, port from config)
- [ ] Wire `SlotManager` → generator → HTTP server → client packet
- [ ] Implement `handlers/CreationCommands.retexture()`
- [ ] Implement `network/sync/TextureQueue.java` (debounce rapid changes)
- [ ] Add config fields: `bgRemovalMode`, `bgRemovalTolerance`, `bgRemovalAutoDetect`
- [ ] Test with a real image URL on a LAN server

**Milestone:** `retexture <id> <url>` visually updates the block for all players.

---

### Phase 5 — Client Architecture (Week 5-6)
> Goal: Full client-side pipeline works. Modded + vanilla clients both receive textures.

- [ ] Implement `client/CustomBlocksClient.java` (ClientModInitializer)
- [ ] Implement `client/ResourcePackGenerator.java` (client-side texture generation)
- [ ] Implement `client/texture/TextureCache.java`
- [ ] Implement all payload classes in `network/payloads/`
- [ ] Implement `NetworkManager.java` (payload registration + dispatch)
- [ ] Implement modded client detection + ConfigSyncPayload path
- [ ] Implement `ChunkedTexturePayload` fallback
- [ ] Implement `mixin/ClientKeepAliveMixin.java`
- [ ] Implement `mixin/ServerKeepAliveMixin.java`
- [ ] Test: vanilla client connects and sees textures
- [ ] Test: modded client connects and sees textures

**Milestone:** Both vanilla and modded clients see correct textures. No purple/black blocks. No disconnects during large packs.

---

### Phase 6 — Undo/Redo + Block Attributes (Week 6-7)
> Goal: Full per-player history + attribute control.

- [ ] Implement `UndoManager.java` (per-player stacks, capped history)
- [ ] Hook into `SlotManager.assign()` to capture previous state
- [ ] Implement `undo` and `redo` commands (already registered in Phase 3)
- [ ] Implement shape commands: `setshape`, `addshape`, `removeshape`, `saveshape`, `loadshape`, `setcollision`
- [ ] Custom sounds registry
- [ ] Implement `DropConfigManager.java`
- [ ] Test with 2 players making simultaneous changes

**Milestone:** Player A's undo does not affect Player B's blocks. Shapes and sounds work.

---

### Phase 7 — Tools / Items (Week 7-8)
> Goal: All 7 custom items work.

- [ ] Register all 7 items in the creative tab
- [ ] Implement `LuminaBrushItem.java` (glow cycling)
- [ ] Implement `AmethystChiselItem.java` (hardness/shape)
- [ ] Implement `DeleterItem.java` (safe delete, shift = instant, trash integration)
- [ ] Implement `ColorSquareItem.java` (dynamic recoloring)
- [ ] Implement `ColorTriangleItem.java` (precision marking, BFS recolor, enclosed holes)
- [ ] Implement `GoldenHexagonItem.java` (admin manipulation)
- [ ] Implement `RectangleToolItem.java` (two-corner rectangular texturing)
- [ ] Ensure NO client-side skip delay on tool use (instant feel)
- [ ] Wire tools to `TrashManager` for soft-delete

**Milestone:** All 7 tools interact correctly with custom blocks. Instant response.

---

### Phase 8 — Color Ecosystem + Categories (Week 8-9)
> Goal: Full color management and block organization.

- [ ] Implement `ColorDetection.java` (dominant color analysis)
- [ ] Implement `ColorVariantService.java` (variant relationships)
- [ ] Implement `ColorNames.java` (hex → name)
- [ ] Implement `PlayerPaletteManager.java` (per-player palettes)
- [ ] Implement `CategoryManager.java` + `Category.java`
- [ ] Implement `AutoCategorizeManager.java`
- [ ] Implement `CategoryDisplayBlockManager.java`
- [ ] Implement hex change wizard (batch recoloring with edge-mode)
- [ ] Implement `BulkScope.java` (batch operations)

**Milestone:** Blocks auto-categorize. Color detection works. Batch recoloring doesn't destroy designs.

---

### Phase 9 — Import/Export + Search + Templates (Week 9-10)
> Goal: Blocks can be shared, searched, templated.

- [ ] Implement `handlers/UtilityCommands.export()` + `importfolder()`
- [ ] Implement `ImportConflictScreen.java` (GUI for conflict resolution)
- [ ] Define export JSON schema (version-tagged) — see Templates section
- [ ] Implement `SearchIndex.java` + `SearchFilter.java` + `SortMode.java`
- [ ] Implement `TemplateManager.java`
- [ ] Implement `LockManager.java`
- [ ] Implement `BlockNotesManager.java`
- [ ] Implement `FavoritesManager.java`
- [ ] Implement `DraftManager.java`

**Milestone:** Export a block, delete it, import it back — identical result. Search is instant. Templates work.

---

### Phase 10 — GUI System (Week 10-13)
> Goal: Full in-game visual editor. NO monolith files.

- [ ] Implement `GuiEngine.java` (immediate-mode render loop + widget primitives)
- [ ] Implement `GuiState.java` + `ArrayDeque` back-stack
- [ ] Implement `GuiMode.java` (all context enums)
- [ ] Implement `CbScreenHandler.java` (custom ScreenHandler)
- [ ] Implement `AnvilPromptManager.java` (text input via anvil)
- [ ] Implement `ColorLibrary.java` + `ColorPickerHelper.java`
- [ ] Build `screens/MainMenuScreen.java`
- [ ] Build `screens/BlockEditorScreen.java`
- [ ] Build `screens/TexturePickerScreen.java`
- [ ] Build `screens/ShapeEditorScreen.java`
- [ ] Build `screens/ColorsHubScreen.java`
- [ ] Build `screens/CategoryBrowserScreen.java`
- [ ] Build `screens/SnapshotManagerScreen.java`
- [ ] Build `screens/UndoPickerScreen.java`
- [ ] Build `screens/ImportConflictScreen.java`
- [ ] Build `screens/ConfigScreen.java`
- [ ] Build `screens/AdminScreen.java`
- [ ] Wire `gui` and `admingui` commands

**File size rule:** Each screen class MUST stay under 500 lines. If it grows beyond that, extract widgets into `gui/widgets/`.

**Milestone:** Full block creation and editing workflow possible without typing commands.

---

### Phase 11 — HUD + Client Screens (Week 13-14)
> Goal: In-game overlay and client-only editors work.

- [ ] Implement `client/HudConfig.java` (position/visibility/style)
- [ ] Implement `client/gui/HudEditorScreen.java` (drag editor)
- [ ] Implement `client/gui/AnimBlockScreen.java` (animation editor)
- [ ] Implement `client/gui/DevConsoleScreen.java` (debug viewer)
- [ ] Wire `edithud` command → payload → client screen
- [ ] HUD renders block info when looking at a custom block

**Milestone:** `/cb edithud` opens a Lunar-style drag editor. HUD shows block info correctly.

---

### Phase 12 — Arabic System (Week 14-15)
> Goal: Full Arabic letter/word block system.

- [ ] Implement `arabic/ArabicLetterMap.java` (letter data + joining rules)
- [ ] Implement `arabic/ArabicBlockRegistry.java` (persist to JSON)
- [ ] Implement `arabic/ArabicWordRenderer.java` (Java2D + arabtype.ttf)
- [ ] Implement `handlers/ArabicCommands.java` (import, gui, give, text, word)
- [ ] Build `screens/ArabicBrowserScreen.java`
- [ ] Implement auto-joining: detect east/west neighbors, swap forms
- [ ] Bundle `arabtype.ttf` font

**Milestone:** Import Arabic letters → browse in GUI → place → auto-join works.

---

### Phase 13 — AI + Video + Macros (Week 15-16)
> Goal: Advanced features operational.

- [ ] Implement `ai/AiCommandParser.java` (natural-language → commands)
- [ ] Implement `ai/AiTextureGenerator.java` (opt-in, API key required)
- [ ] Implement `video/VideoDecoder.java` (jcodec MP4/H264 → frames)
- [ ] Implement `core/MacroManager.java` (record/replay/list/delete)
- [ ] Implement `handlers/MacroCommands.java`
- [ ] Build `screens/MacroManagerScreen.java`

**Milestone:** AI parses "make me a glowing red block" correctly. Video frames extract. Macros record and replay.

---

### Phase 14 — Cloud + Discord + Onboarding (Week 16-17)
> Goal: Cloud sync, Discord notifications, and new user experience.

- [ ] Set up `cloud-vault-worker/` (Cloudflare Worker + KV store)
- [ ] Implement `cloud/CloudVaultClient.java` (upload/download)
- [ ] Implement cloud share commands
- [ ] Implement `discord/DiscordWebhook.java` (opt-in webhooks)
- [ ] Implement `core/OnboardingManager.java` + `WelcomeManager.java`
- [ ] Implement `core/FirstUseHints.java`
- [ ] Implement `core/TipPool.java`
- [ ] Implement `core/SampleBlocksLoader.java`
- [ ] Add config fields for cloud and Discord

**Milestone:** Share a block on Server A → import on Server B via cloud code. Discord notifications fire.

---

### Phase 15 — Diagnostics + Polish (Week 17-18)
> Goal: Debugging tools, incident tracking, final polish.

- [ ] Implement `core/DiagnosticsHelper.java`
- [ ] Implement `core/IncidentRecorder.java`
- [ ] Implement `core/HistoryTracker.java`
- [ ] Implement `FeedbackHelper.java` (rich in-game messages)
- [ ] Add SpotBugs static analysis (strict mode — `ignoreFailures = false`)
- [ ] Add `verifyVoiceCatalog` Gradle task (if voice system added post-v1.0)

**Milestone:** Diagnostics collect system state. Incidents are logged with context.

---

### Phase 16 — Testing + Release (Week 18-19)
- [ ] Stress test: 1028 blocks simultaneously
- [ ] Multiplayer test: 5+ concurrent players
- [ ] Invalid URL handling (timeout, 404, non-image)
- [ ] Performance profiling (no lag during GUI, no FPS drops)
- [ ] Keep-alive mixin verification (no disconnects on large packs)
- [ ] Full README + documentation pass
- [ ] `reload` command stability test
- [ ] License file finalization
- [ ] Mod Menu entry (icon, description, links)
- [ ] Build and distribute JAR

**Milestone:** Stable, shippable v1.0.0 release.

---

## 9. Engineering Protocols

### 9.1 Git Strategy

#### Branch Naming
```
main              ← always stable, always working
dev               ← active development (default merge target)
feature/<name>    ← new feature (e.g. feature/slot-manager)
fix/<name>        ← bug fix (e.g. fix/http-server-crash)
phase/<number>    ← per-phase branch (e.g. phase/4-texture-pipeline)
```

#### Commit Message Format (Conventional Commits)
```
<type>(<scope>): <short description>

Types:
  feat     → new feature
  fix      → bug fix
  docs     → documentation only
  refactor → code change with no behavior change
  test     → test additions
  chore    → build/tooling changes

Examples:
  feat(slot-manager): implement .update() immutable builder
  fix(http-server): handle null ZIP on first request
  docs(engineering-bible): add phase 4 milestone
  refactor(undo-manager): replace LinkedList with ArrayDeque
```

#### Merge Rules
- Never commit directly to `main`
- `dev` → `main` only when a full Phase milestone is reached
- Write a CHANGELOG entry before every merge to `main`
- **NEVER leave a dirty worktree.** Commit or stash before ending a session.

---

### 9.2 Documentation Protocol

#### Every New Class Gets a Header
```java
/**
 * SlotManager.java
 *
 * Responsibility: Single point of truth for all 1028 block slot assignments.
 * Manages lifecycle: free → occupied → freed.
 *
 * Depends on: SlotData, SlotDataStore, UndoManager, ServerPackGenerator
 * Called by:  CommandRegistrar handlers, GuiEngine, Items
 *
 * ADR Reference: ADR-001 (Why pre-registration instead of dynamic registry)
 */
```

#### Architecture Decision Records (ADRs)
When you make a non-obvious design choice, document it:

```
ADR-XXX: <Title>
Date: YYYY-MM-DD
Status: Accepted / Superseded / Deprecated

Context:
  What problem were we solving?

Decision:
  What did we choose to do?

Rationale:
  Why this over the alternatives?

Consequences:
  What are the trade-offs?
```

Store all ADRs in `/docs/adr/` folder.

#### Progress Log
Maintain a `PROGRESS_LOG.md` — one entry per work session:
```
## 2025-XX-XX
### Done
- Implemented SlotData immutability + .update() builder
- All tests pass for Phase 1

### Blockers
- Fabric's block registration API unclear for 1.21.1 — need to check docs

### Next Session
- Start SlotDataStore persistence
```

---

### 9.3 File Size Rules

| File Type | Maximum Lines | Action if Exceeded |
|---|---|---|
| Any `.java` class | 500 lines | Split into focused sub-classes or extract to a new package |
| Any screen class | 500 lines | Extract widgets into `gui/widgets/` |
| Command handler | 400 lines | Split into sub-handlers |
| Config class | 300 lines | Group into sections or nested config objects |

**Rationale:** The old project's `GuiManager.java` grew to 623KB (est. 15,000+ lines) and `CustomBlockCommand.java` to 414KB (est. 10,000+ lines). Both became unmaintainable nightmares. This will not happen again.

---

### 9.4 Problem-Solving Protocol

When you hit a bug or blocker, follow this exact process:

```
STEP 1 — REPRODUCE
  Can you make it happen again?
  Write down: what you did, what you expected, what happened.
  If you can't reproduce it → log it and move on.

STEP 2 — ISOLATE
  What is the SMALLEST piece of code that triggers the issue?
  Comment out everything unrelated. Narrow it down.

STEP 3 — HYPOTHESIZE
  Write 2-3 possible causes (don't debug randomly).
  "I think it's X because..."

STEP 4 — TEST ONE HYPOTHESIS AT A TIME
  Change one thing. Observe. Don't change 3 things at once.

STEP 5 — FIX
  Apply the fix. Confirm it solves the issue.
  Confirm it doesn't break anything else.

STEP 6 — DOCUMENT
  Write a commit: fix(<scope>): <what was wrong and why>
  If it was non-obvious, add a comment in the code explaining WHY the fix works.
  If it was an architectural mistake, write an ADR.
```

#### When You're Genuinely Stuck (30-min rule)
If you've been stuck for 30 minutes:
1. Write down exactly what you've tried (forces clarity)
2. Search the Fabric Discord / GitHub issues first
3. Ask for help with: what you tried, what you expected, what happened, minimal reproducer

---

### 9.5 Definition of Done

A feature is DONE when:
- [ ] It works as specified in the SRS
- [ ] Edge cases are handled (invalid input, null, network failure)
- [ ] It's been tested manually in-game
- [ ] The class has a proper header comment
- [ ] A commit has been written with the correct format
- [ ] CHANGELOG has been updated if it's user-facing
- [ ] The file stays under the line limit (§9.3)

---

### 9.6 Known Pitfalls (Lessons from the Old Project)

These are real bugs that happened. Learn from them. Don't repeat them.

| Pitfall | What Happened | Prevention |
|---|---|---|
| **Mojibake** | CP1252 → UTF-8 corruption broke block names | `TextSanitizer` + `verifyMojibake` Gradle task |
| **SoundEvents.value()** | `BLOCK_NOTE_BLOCK_*` are `RegistryEntry<SoundEvent>`, not `SoundEvent` — silent crashes | `verifySound` Gradle task |
| **Client-side skip** | `if (world.isClient) return PASS` in tool `useOnBlock()` caused noticeable delay | Gate via `if (!(player instanceof ServerPlayerEntity))` instead |
| **Dirty worktree** | Uncommitted changes lost across sessions | Commit or stash before every session end |
| **DidYouMean arg name** | Argument named `"unknown_cb_tail"` showed verbatim in action bar | Name it `"subcommand"` |
| **IO executor shutdown** | `IO_EXECUTOR.shutdown()` in save killed the IO thread permanently | Use `flushSaveForReload()` without shutdown |
| **Batch recolor mode** | Player PLAYER_MODE "full" destroyed block designs in batch recolor | Always force edge mode for batch operations |
| **ConfigSync timing** | `ConfigSyncPayload` fired before batch → stale textures | Move `broadcastConfigSync` to AFTER batch completion |
| **Pack debounce** | 30-second debounce timer delayed pack rebuild → 30s purple blocks | Make debounce window short (500ms) or configurable |
| **Snapshot auto-restore** | Wrong snapshot loaded on startup — selected snapshot not persisted to disk | Persist selected snapshot ID to disk, not just memory |

---

## 10. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Fabric API changes break registration logic | Medium | High | Pin exact Fabric API version. Review release notes before updates. |
| Embedded HTTP server blocked by server firewalls | High | High | Make port configurable. Document the port requirement clearly. |
| GIF animation not rendering | Medium | Low | Minecraft supports animated textures via `.mcmeta` — test early in Phase 4. |
| Memory leak from ZIP generation on every update | Medium | High | Generate ZIP lazily; cache and invalidate only on actual slot changes. Use `TextureQueue` debouncing. |
| Per-face texture model JSON generation is complex | Medium | Medium | Prototype this in Phase 4 before anything else. |
| GUI causes FPS drops | Medium | High | Profile the render loop in isolation before wiring it to game data. |
| `importfolder` importing corrupted JSON crashes server | High | High | Wrap all import parsing in try-catch with graceful skip + log. |
| 1028 pre-registered blocks causes slow server startup | Low | Medium | Benchmark startup time in Phase 1. If slow, lazy-register in batches. |
| File size creep (monolith files) | High | High | Enforce §9.3 file size rules. Code review every PR for line count. |
| Client disconnect during large pack download | Medium | High | Keep-alive mixins (Phase 5). |
| Mojibake corruption in localization | Medium | Medium | `verifyMojibake` Gradle task runs on every build. |
| AI features requiring API keys fail silently | Medium | Low | All AI features opt-in with clear error messages when key missing. |
| Cloud vault worker deployment confusion | Medium | Low | Bundle `cloud-vault-worker/` with step-by-step README. |

---

## 11. Templates & Appendix

### CHANGELOG.md Template
```markdown
# Changelog

## [Unreleased]
### Added
- 

## [0.1.0] - YYYY-MM-DD
### Added
- Phase 1 complete: SlotManager, SlotData, 1028 slots registered

### Changed
- 

### Fixed
- 
```

### Export File Schema (v1)
```json
{
  "schema_version": 1,
  "exported_at": "2025-01-01T00:00:00Z",
  "block_id": "my_block",
  "display_name": "My Custom Block",
  "textures": {
    "all": "https://example.com/texture.png",
    "top": null,
    "bottom": null,
    "north": null,
    "south": null,
    "east": null,
    "west": null
  },
  "attributes": {
    "glow": 0,
    "hardness": 1.5,
    "sound": "minecraft:block.stone"
  },
  "shapes": [],
  "category": null,
  "notes": null,
  "locked": false,
  "tags": []
}
```

### ADR Template File
```
# ADR-001: Title Here

Date: YYYY-MM-DD  
Status: Accepted

## Context
What problem were we trying to solve?

## Decision
What did we decide to do?

## Rationale
Why this approach over alternatives?

## Consequences
What are the trade-offs?
```

### fabric.mod.json Reference
```json
{
  "schemaVersion": 1,
  "id": "customblocks",
  "version": "1.0.0",
  "name": "CustomBlocks",
  "description": "Make any image or gif a working block :)",
  "authors": ["3liSY / SrbGamer"],
  "license": "All Rights Reserved",
  "icon": "assets/customblocks/icon.png",
  "contact": {
    "homepage": "https://modrinth.com/mod/customblocks"
  },
  "environment": "*",
  "entrypoints": {
    "main":   ["com.customblocks.CustomBlocksMod"],
    "client": ["com.customblocks.client.CustomBlocksClient"]
  },
  "mixins": ["customblocks.mixins.json"],
  "depends": {
    "fabricloader": ">=0.16.0",
    "fabric-api":   "*",
    "minecraft":    "~1.21.1",
    "java":         ">=21"
  },
  "suggests": {
    "fabric-permissions-api-v0": "*",
    "modmenu": "*"
  },
  "custom": {
    "modmenu": {
      "links": {
        "modmenu.discord": "https://discord.gg/stormygang",
        "customblocks.youtube": "https://www.youtube.com/@SrbGamerr"
      }
    }
  }
}
```

### Dependencies Reference
```groovy
dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings  "net.fabricmc:yarn:1.21.1+build.3:v2"
    modImplementation "net.fabricmc:fabric-loader:0.16.9"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.104.0+1.21.1"

    // Video-to-texture (jcodec — pure-Java MP4/H264 decoder)
    include implementation("org.jcodec:jcodec:0.2.5")
    include implementation("org.jcodec:jcodec-javase:0.2.5")

    // SpotBugs annotations (compileOnly — not shipped)
    compileOnly 'com.github.spotbugs:spotbugs-annotations:4.8.6'
}
```

### Post-v1.0 Features (Planned for Later)
| Feature | Notes |
|---|---|
| Full permission system | Fabric Permissions API / LuckPerms integration, granular per-command nodes |
| Achievement system | In-game achievement tracking for block creation milestones |
| Hologram preview | Floating holographic block previews |
| Showcase blocks | Display blocks in a showcase format |
| Voice/Personality system | 6 chat voice styles (friendly, pro, royal, minimal, arabic, silly) |
| Diamond Triangle tool | Extra color tool variant |
| Smart BG removal | AI-powered background detection with learning feedback loop |
| Screen eyedrop | Sample any screen pixel for color picking |
| Live recolor preview | Real-time preview in GUI during recoloring |

---

*Last updated: See Git log*  
*Owner: 3liSY / SrbGamer*  
*Next review: After Phase 4 completion*
