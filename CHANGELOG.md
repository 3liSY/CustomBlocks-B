# Changelog

All notable changes to CustomBlocks are documented here. This project tracks
progress by **phase milestones** (see CustomBlocks_Engineering_Bible.md §8).

## [Unreleased]

### Added
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
-

### Fixed
-

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
