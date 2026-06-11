# CustomBlocks — Developer Handoff

**Date:** 2026-06-06  
**Branch:** `dev` (commit `8ccd5b5`)  
**Build status:** BUILD SUCCESSFUL — all 3 gates green (verifyFileSize · verifyMojibake · verifySound)  
**Phase status:** All 16 phases code-complete. **In-game testing pending.**

---

## What this mod does

CustomBlocks is a Fabric mod for Minecraft 1.21.1. It lets a server admin (or any player with permission) create up to 800 custom blocks by pasting an image/GIF URL. Blocks render the image as their texture, work in-game like normal blocks, survive server restarts, and replicate to all connected clients automatically via an embedded HTTP resource-pack server.

---

## Immediate next step — test in-game

Before anything else, the mod needs in-game confirmation. Nothing is ✅ done until you test it.

```
.\gradlew.bat runClient
```

Work through [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md) top-to-bottom:

| Section | Covers | Status |
|---|---|---|
| §1j Batch 10 | Per-block export, importfolder, templates | ⬜ |
| §1k Batch 11 | UX polish, export menu, template overhaul | ⬜ |
| §1l Batch 12 | Lock, Notes, Favorites, Drafts | ⬜ |
| §1n Batch 13 | Macros, Arabic, GUI, HUD, diagnostics, cloud stubs | ⬜ |

Batches 0–9 were already confirmed working (see TESTING_GUIDE for ✅ marks).

---

## Key commands reference

```
/cb create <id> [name] [url]      — create a block (with optional texture)
/cb retexture <id> <url>          — apply a new texture
/cb give <id>                     — put the block in your hand
/cb list                          — show all created blocks
/cb delete <id>                   — remove a block definition
/cb rename <id> <name>            — rename display name
/cb dupe <id> <newId>             — duplicate a block

/cb setglow <id> <0-15>           — set light emission
/cb sethardness <id> <value>      — stone / unbreakable / instant / 5 / 20 / 50
/cb setsound <id> <sound>         — wood/stone/metal/glass/dirt/gravel/wool
/cb setcollision <id> solid|passable

/cb undo / /cb redo               — undo/redo per-player or server-wide
/cb config undomode [perplayer|serverwide]

/cb lock <id> / /cb unlock <id>   — prevent modification
/cb note <id> [text|clear]        — attach a note to a block
/cb fav <id>                      — toggle favorite; /cb favs to list
/cb draft <id> / /cb publish <id> — mark as WIP or publish

/cb setcategory <id> <cat>        — tag a block with a category
/cb categories                    — list categories with counts
/cb search <query>                — search by id, name, or category

/cb template save <name> <id>     — save attributes as a template
/cb template apply <name> <id>    — apply template to a block
/cb template list / delete <name>

/cb export [id]                   — export block(s) to JSON/TXT
/cb importfolder [path]           — import from exports/ folder

/cb macro record <name>           — start recording a macro
/cb macro add <cmd>               — append a command step
/cb macro stop / cancel           — finish or discard recording
/cb macro play <name>             — replay all steps
/cb macro list / delete <name>

/cb arabic import                 — create all 28 Arabic letter blocks
/cb arabic letter <name>          — create one letter block
/cb arabic word <text> <id> <name>— create a word-image block
/cb arabic list                   — list imported Arabic blocks

/cb video list                    — list .mp4 files in config/customblocks/videos/
/cb video extract <file> <id> <n> — bake frame N from video as block texture

/cb gui                           — open main GUI menu
/cb gui block <id>                — open block attribute editor
/cb gui config                    — open config screen
/cb gui macros                    — open macro manager screen
/cb gui arabic                    — open Arabic browser screen
/cb edithud                       — toggle HUD block-info overlay

/cb diag                          — system snapshot (slots, heap, TPS)
/cb incidents / /cb incidents clear

/cb vault upload <id>             — (stub) cloud vault upload
/cb vault download <code>         — (stub) cloud vault download
/cb discord test / status         — (stub) Discord webhook test

/cb reload                        — reload all blocks from disk
```

---

## Architecture at a glance

```
src/main/java/com/customblocks/
├── CustomBlocksMod.java          ← server entrypoint
├── CustomBlocksConfig.java       ← config.json, all settings
├── block/SlotBlock.java          ← the 800 registered slot blocks
├── core/
│   ├── SlotManager.java          ← SINGLE SOURCE OF TRUTH for all slot state
│   ├── SlotData.java             ← immutable record; mutate via .update()
│   ├── SlotDataStore.java        ← ONLY class that touches slots.json
│   ├── TextureStore.java         ← read/write PNG bytes per slot
│   ├── UndoManager.java          ← per-player or server-wide history
│   ├── TemplateManager.java      ← attribute templates
│   ├── MacroManager.java         ← command sequence recording/replay
│   ├── LockManager.java          ← lock/unlock block definitions
│   ├── BlockNotesManager.java    ← per-block text annotations
│   ├── FavoritesManager.java     ← per-player bookmarks
│   ├── DraftManager.java         ← draft/publish workflow
│   ├── DiagnosticsHelper.java    ← system snapshot
│   ├── IncidentRecorder.java     ← incident log
│   └── OnboardingManager.java    ← first-join welcome
├── command/
│   ├── CommandRegistrar.java     ← builds the /cb tree
│   └── handlers/                 ← one file per domain (≤400 lines each)
├── image/                        ← download + decode + resize images
├── arabic/                       ← 28-letter Arabic block system
├── video/VideoDecoder.java       ← jcodec MP4 frame extractor
├── network/                      ← HTTP pack server + payloads
├── gui/                          ← GUI screens (≤500 lines each)
├── client/                       ← HUD, slot cache, client entrypoint
├── modmenu/ModMenuApiImpl.java   ← Mod Menu config button
├── ai/                           ← AI stubs (activate with aiApiKey)
├── cloud/                        ← Cloud vault stub
└── discord/                      ← Discord webhook
```

**Non-negotiable architecture rules:**
- `SlotData` is always immutable — mutate via `.update()` / `.withX()`
- `SlotManager` is the only authority on slot state
- `SlotDataStore` is the only class that reads/writes `slots.json`
- All file writes are atomic (write-temp + rename)
- No file over 500 lines (400 for handlers, 300 for Config)

---

## Config fields (config/customblocks/config.json)

| Field | Default | Purpose |
|---|---|---|
| `maxSlots` | `800` | Number of registered slot blocks |
| `httpPort` | `8123` | Resource-pack HTTP server port |
| `textureSize` | `64` | Texture resolution (16/32/64/128) |
| `maxUndoDepth` | `50` | Undo history limit |
| `undoMode` | `"server-wide"` | `"server-wide"` or `"per-player"` |
| `httpHost` | `""` | Override host for pack URL (leave blank for auto) |
| `hudEnabled` | `true` | HUD overlay toggle |
| `aiApiKey` | `""` | Anthropic API key — enables AI features when set |
| `aiTextureEnabled` | `false` | Enable AI texture generation |
| `vaultEndpoint` | `""` | Cloudflare Worker URL — enables cloud sync |
| `discordWebhookUrl` | `""` | Discord webhook — enables notifications |

---

## Optional developer setup

| Task | What to do |
|---|---|
| Arabic fonts | Place `arabtype.ttf` in `run/config/customblocks/arabtype.ttf` |
| Video extraction | Place `.mp4` files in `run/config/customblocks/videos/` |
| Cloud sync | Deploy Cloudflare Worker, set `vaultEndpoint` in config.json |
| Discord alerts | Create a Discord webhook, set `discordWebhookUrl` in config.json |
| AI features | Set `aiApiKey` (Anthropic) and `aiTextureEnabled: true` in config.json |
| Mod Menu icon | Add a 64×64 PNG as `src/main/resources/pack.png` |

---

## Build & run

```powershell
# Build (downloads deps first run, ~30s; subsequent builds ~5s)
.\gradlew.bat build

# Launch dev client (downloads MC assets first run, ~2min)
.\gradlew.bat runClient

# Launch dedicated dev server
.\gradlew.bat runServer

# Final distributable JAR
build/libs/customblocks-1.0.0.jar
```

**JDK requirement:** Temurin JDK 21 only. The machine default is JDK 26 which Gradle 8.8 cannot use. JDK 21 is pinned via `C:\Users\POTATO\.gradle\gradle.properties`. In IntelliJ, set Gradle JVM → 21.

---

## What's left before v1.0.0 ship

1. **In-game testing** — work through TESTING_GUIDE §1j–§1n; fix any failures found.
2. **Stress test** (Phase 16) — create 800 blocks simultaneously, confirm no registry errors.
3. **Multiplayer** (Phase 16) — test with 5+ concurrent players if possible.
4. **Commit to `main`** — once all tests pass, merge `dev` → `main` and tag `v1.0.0`.
5. **Upload JAR** — `build/libs/customblocks-1.0.0.jar` to Modrinth/CurseForge.

---

## Known architecture decisions (ADRs)

- [ADR-001](docs/adr/) — clean-room fresh start (no monolith copy from old project)
- [ADR-002](docs/adr/ADR-002-glow-as-blockstate-property.md) — glow implemented as BlockState property (MC bakes luminance once per state; dynamic attributes need a state property)

---

## Repo layout

```
CustomBlocks/               ← THIS project (fresh start)
├── CLAUDE.md               ← AI assistant operating protocol
├── PROGRESS_LOG.md         ← session log (newest on top)
├── CHANGELOG.md            ← user-facing change history
├── CustomBlocks_Engineering_Bible.md  ← single source of truth for design
├── docs/
│   ├── TESTING_GUIDE.md    ← per-phase test checklists
│   ├── MOJIBAKE_SHIELD.md  ← encoding safety docs
│   └── adr/                ← architecture decision records
└── src/

CustomBlocks/ (subfolder)   ← OLD project (reference only, git-ignored)
                               Read it to recycle algorithms; do not copy structure
```

---

*Generated 2026-06-06. Source of truth: `PROGRESS_LOG.md` + `TESTING_GUIDE.md`.*
