# Group 01 — Legacy Feature Audit · REPORT

> **Status:** Draft for developer sign-off (Test G01.4). Nothing in Groups 02–25 may be marked in-progress until you approve this report.
>
> **Generated against:** old `CustomBlocks` vs new `CustomBlocks-B`, source trees as shipped in `CustomBlocks1.zip`.
>
> **Source issues:** 17.11

---

## 0. Methodology & sources verified

Every status below was assigned by reading the actual source, not by name-matching.

| Source | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Command surface | `command/CustomBlockCommand.java` — **6,784 lines**, monolithic | 14 handler classes under `command/handlers/` + `CommandRegistrar.java` |
| GUI surface | `gui/GuiManager.java` — **10,324 lines**, ~80 `open*` workflows | `gui/GuiEngine.java` + **5** screen classes |
| Systems | 25 × `*Manager.java` | subset rebuilt (Slot, Undo, Lock, Draft, Favorites, BlockNotes, Template, Macro, BlockExporter, TextureStore) |
| Config | `CustomBlocksConfig.java` — 809 lines, ~67 public fields | `CustomBlocksConfig.java` — 139 lines, **11** fields |

**Status vocabulary:** `EXISTS` · `MISSING` · `PARTIAL` · `BROKEN` · `REPLACED` · `SCRAPPED`
**Priority:** **P0** blocks testing · **P1** core UX · **P2** quality of life · **P3** advanced/optional

> ⚠️ **Note on the config-field test (G01.3):** the spec references an old `config.json`, but no such file exists outside `run/`. The old field list below was extracted directly from old `CustomBlocksConfig.java` instead.

---

## A. Command / Feature Audit Table (Test G01.1)

Every subcommand from the canonical old `/cb` reference list, with a status — no blanks.

| # | Command/Feature | Old CB | New CB-B | Restoration Group | Priority |
|---|---|---|---|---|---|
| 1 | `cb` (root) | EXISTS | EXISTS | — | — |
| 2 | `customblock` (alias of `cb`) | EXISTS | EXISTS | — | — |
| 3 | `create` | EXISTS | EXISTS | — (core) | — |
| 4 | `delete` | EXISTS | EXISTS (locked-block guard) | — (core) | — |
| 5 | `dupe` | EXISTS | EXISTS | — (core) | — |
| 6 | `duplicate` (alias) | EXISTS | REPLACED by `dupe` | G25 | P3 |
| 7 | `rename` | EXISTS | EXISTS | — (core) | — |
| 8 | `retexture` | EXISTS | EXISTS | — (core) | — |
| 9 | `give` | EXISTS | EXISTS | — (core) | — |
| 10 | `list` | EXISTS | EXISTS (chat) | — (core) | — |
| 11 | `search` | EXISTS | EXISTS | — (core) | — |
| 12 | `find` (alias of search) | EXISTS | MISSING (alias) | G25 | P2 |
| 13 | `reload` | EXISTS | EXISTS | — (core) | — |
| 14 | `undo` | EXISTS | EXISTS | — (core) | — |
| 15 | `redo` | EXISTS | EXISTS | — (core) | — |
| 16 | `setglow` | EXISTS | EXISTS | — (core) | — |
| 17 | `sethardness` | EXISTS | EXISTS | — (core) | — |
| 18 | `setsound` | EXISTS | EXISTS | — (core) | — |
| 19 | `setcollision` | EXISTS | EXISTS | — (core) | — |
| 20 | `lock` | EXISTS | EXISTS | — (core) | — |
| 21 | `unlock` | EXISTS | EXISTS | — (core) | — |
| 22 | `template` | EXISTS | EXISTS (list/save/apply/delete) | G12 | P2 |
| 23 | `save` (template save) | EXISTS | EXISTS | G12 | P2 |
| 24 | `apply` (template apply) | EXISTS | EXISTS | G12 | P2 |
| 25 | `importfolder` | EXISTS | EXISTS | G12 | P2 |
| 26 | `macro` | EXISTS | EXISTS (record/add/stop/cancel/play/list/delete) | G24 | P2 |
| 27 | `record` (macro record) | EXISTS | EXISTS | G24 | P2 |
| 28 | `add` (macro add / block add) | EXISTS | PARTIAL — macro add only; block-add → `create` | G24 / G25 | P2 |
| 29 | `stop` (macro stop) | EXISTS | EXISTS | G24 | P2 |
| 30 | `pause` | EXISTS | MISSING | G14 | P3 |
| 31 | `resume` | EXISTS | MISSING | G14 | P3 |
| 32 | `script` | EXISTS | MISSING (superseded by macros?) | G24 | P3 |
| 33 | `scriptgui` | EXISTS | MISSING | G24 | P3 |
| 34 | `run` | EXISTS | MISSING | G24 | P3 |
| 35 | `note` | EXISTS | PARTIAL — barebones text only (17.12) | G18 | P2 |
| 36 | `favorite` | EXISTS | PARTIAL — only `fav` alias exists; `favorite` primary missing (17.13) | G25 | P2 |
| 37 | `unfavorite` | EXISTS | REPLACED by `fav` toggle | G25 | P3 |
| 38 | `config` | EXISTS | PARTIAL — only `undomode`; no in-game config GUI (17.23) | G21 | P1 |
| 39 | `settings` (settings GUI) | EXISTS | PARTIAL → folded into `config` | G21 | P1 |
| 40 | `reset` | EXISTS | MISSING | G21 | P3 |
| 41 | `gui` | EXISTS | PARTIAL — screen-based, not chest GUI (17.3) | G02 | P0 |
| 42 | `menu` (main menu) | EXISTS | PARTIAL — chest GUI missing (17.3) | G02 | P1 |
| 43 | `editor` (`/cb editor <id>` shortcut) | EXISTS | MISSING — only `/cb gui block <id>` (17.5) | G02 | P1 |
| 44 | `listgui` | EXISTS | MISSING | G02 | P1 |
| 45 | `blocks` (block list GUI) | EXISTS | PARTIAL — chat `list` only, GUI missing | G02 | P1 |
| 46 | `redogui` | EXISTS | MISSING | G02 | P3 |
| 47 | `undogui` | EXISTS | MISSING | G02 | P3 |
| 48 | `bulkgui` | EXISTS | MISSING | G07 | P2 |
| 49 | `facechangegui` | EXISTS | MISSING | G19 | P2 |
| 50 | `scriptgui` (dup of #33) | EXISTS | MISSING | G24 | P3 |
| 51 | `edithud` | EXISTS | PARTIAL — toggles on/off only; no drag editor (17.4/17.6) | G03 | P1 |
| 52 | `on` | EXISTS | MISSING (toggle) | G03 | P3 |
| 53 | `off` | EXISTS | MISSING (toggle) | G03 | P3 |
| 54 | `resourcepack` | EXISTS | PARTIAL — silent delivery WIP (17.1) | G05 | P1 |
| 55 | `rp` (alias) | EXISTS | PARTIAL → see `resourcepack` | G05 | P2 |
| 56 | `arabic` | EXISTS | PARTIAL/BROKEN — `arabic word` broken (17.18), auto-join missing (17.20), font unbundled (17.19) | G13 | P1 |
| 57 | `text` (text block) | EXISTS | MISSING | G19 | P2 |
| 58 | `ai` | EXISTS | MISSING — `AiCommandParser` stub returns null, no command wired (17.22) | G15 | P2 |
| 59 | `backup` | EXISTS | MISSING — BackupManager not ported | G09 | P1 |
| 60 | `restore` | EXISTS | MISSING | G09 | P1 |
| 61 | `safety` (Safety Center) | EXISTS | MISSING | G09 | P1 |
| 62 | `panic` | EXISTS | MISSING | G09 | P1 |
| 63 | `snapshots` | EXISTS | **SCRAPPED** — merged into `backup` (Decision §C) | G09 | — |
| 64 | `expiry` (trash expiry) | EXISTS | MISSING | G09 | P3 |
| 65 | `deletedblocks` (trash GUI) | EXISTS | MISSING | G09 | P2 |
| 66 | `recover` | EXISTS | MISSING | G09 | P2 |
| 67 | `bulk` | EXISTS | MISSING | G07 | P1 |
| 68 | `bulkblockadd` | EXISTS | MISSING | G07 | P2 |
| 69 | `bulkcolor` | EXISTS | MISSING | G07 | P2 |
| 70 | `bulkdelete` | EXISTS | MISSING | G07 | P1 |
| 71 | `bulkduplicate` | EXISTS | MISSING | G07 | P2 |
| 72 | `bulkexport` | EXISTS | MISSING | G07 | P2 |
| 73 | `bulkfavorite` | EXISTS | MISSING | G07 | P3 |
| 74 | `bulklock` | EXISTS | MISSING | G07 | P2 |
| 75 | `bulkmove` | EXISTS | MISSING | G07 | P2 |
| 76 | `bulkproperty` | EXISTS | MISSING | G07 | P2 |
| 77 | `bulkrecolor` | EXISTS | MISSING | G07 | P2 |
| 78 | `bulkreid` | EXISTS | MISSING | G07 | P2 |
| 79 | `bulkrename` | EXISTS | MISSING | G07 | P2 |
| 80 | `bulkshape` | EXISTS | MISSING | G07 / G08 | P2 |
| 81 | `bulksound` | EXISTS | MISSING | G07 | P2 |
| 82 | `bulkunfavorite` | EXISTS | MISSING | G07 | P3 |
| 83 | `bulkunlock` | EXISTS | MISSING | G07 | P2 |
| 84 | `confirm` | EXISTS | MISSING — bulk/destructive confirm flow | G07 | P3 |
| 85 | `chisel` | EXISTS | REPLACED → Omni-Tool (17.10) | G06 | P1 |
| 86 | `brush` (Lumina) | EXISTS | REPLACED → Omni-Tool (17.10) | G06 | P1 |
| 87 | `deleter` (Deleter Tool) | EXISTS | REPLACED → tools tab item (17.9) | G06 | P1 |
| 88 | `settabicon` | EXISTS | MISSING | G06 / G11 | P3 |
| 89 | `addshape` | EXISTS | MISSING — shapes system not ported | G08 | P2 |
| 90 | `removeshape` | EXISTS | MISSING | G08 | P2 |
| 91 | `clearshape` | EXISTS | MISSING | G08 | P2 |
| 92 | `setshape` | EXISTS | MISSING | G08 | P2 |
| 93 | `shapeeditor` | EXISTS | MISSING | G08 | P2 |
| 94 | `shapelist` | EXISTS | MISSING | G08 | P2 |
| 95 | `shapepreview` | EXISTS | MISSING | G08 | P2 |
| 96 | `triangle` | EXISTS | MISSING | G08 | P2 |
| 97 | `trianglemode` | EXISTS | MISSING | G08 | P2 |
| 98 | `customtriangle` | EXISTS | MISSING | G08 | P2 |
| 99 | `rectangle` | EXISTS | MISSING | G08 | P2 |
| 100 | `square` | EXISTS | MISSING | G08 | P2 |
| 101 | `hexagon` | EXISTS | MISSING | G08 | P2 |
| 102 | `edge` | EXISTS | MISSING | G08 / G10 | P3 |
| 103 | `colors` (Colors Hub) | EXISTS | MISSING | G10 | P2 |
| 104 | `customcolor` | EXISTS | MISSING | G10 | P2 |
| 105 | `gradient` | EXISTS | MISSING | G10 | P2 |
| 106 | `palette` | EXISTS | MISSING | G10 | P2 |
| 107 | `bgstudio` | EXISTS | MISSING | G10 | P2 |
| 108 | `_internal_setglobalbg` | EXISTS | MISSING (internal bg helper) | G10 | P3 |
| 109 | `tolerance` (bg removal) | EXISTS | MISSING | G10 | P3 |
| 110 | `full` (bg removal mode) | EXISTS | MISSING | G10 | P3 |
| 111 | `dress` | EXISTS | MISSING | G19 | P3 |
| 112 | `resize` | EXISTS | MISSING | G10 / G19 | P3 |
| 113 | `exportpng` | EXISTS | MISSING | G10 / G12 | P3 |
| 114 | `setface` | EXISTS | MISSING | G19 | P2 |
| 115 | `clearface` | EXISTS | MISSING | G19 | P2 |
| 116 | `clearallfaces` | EXISTS | MISSING | G19 | P2 |
| 117 | `hologram` | EXISTS | MISSING | G19 | P2 |
| 118 | `givedisplayblock` | EXISTS | MISSING | G19 | P2 |
| 119 | `particles` | EXISTS | MISSING | G19 / G23 | P3 |
| 120 | `sounds` (sounds hub) | EXISTS | PARTIAL — `setsound` only, no hub GUI | G25 | P3 |
| 121 | `export` | EXISTS | PARTIAL — vault path is a stub; needs rework (17.15) | G12 | P1 |
| 122 | `exportall` | EXISTS | REPLACED by `export json/txt` | G12 | P2 |
| 123 | `exportblock` | EXISTS | REPLACED by `export <id>` | G12 | P2 |
| 124 | `exportcategory` | EXISTS | MISSING | G12 | P2 |
| 125 | `import` | EXISTS | PARTIAL — folder/arabic import only | G12 | P2 |
| 126 | `importblock` | EXISTS | PARTIAL → `importfolder` | G12 | P2 |
| 127 | `importcategory` | EXISTS | MISSING | G11 / G12 | P2 |
| 128 | `market` | EXISTS | MISSING | G12 | P2 |
| 129 | `marketplace` | EXISTS | MISSING | G12 | P2 |
| 130 | `sharecategory` | EXISTS | MISSING | G11 / G12 | P2 |
| 131 | `sync` | EXISTS | MISSING — vault sync stub (Phase 14) | G20 | P2 |
| 132 | `blockscat` | EXISTS | PARTIAL — `categories` (chat) only | G11 | P2 |
| 133 | `blockscategory` | EXISTS | PARTIAL → see `blockscat` | G11 | P2 |
| 134 | `givecategory` | EXISTS | MISSING | G11 | P2 |
| 135 | `diagnostics` | EXISTS | REPLACED by `diag`; needs rework (17.24) | G16 | P2 |
| 136 | `cache` (Cache Dashboard) | EXISTS | MISSING | G16 | P3 |
| 137 | `audit` (Audit GUI) | EXISTS | MISSING | G16 | P3 |
| 138 | `showbrokenblocks` | EXISTS | MISSING | G16 | P2 |
| 139 | `unsuppress` | EXISTS | MISSING | G04 / G16 | P3 |
| 140 | `clear` | EXISTS | PARTIAL — only `incidents clear` | G16 | P3 |
| 141 | `reid` | EXISTS | MISSING | G25 | P2 |
| 142 | `swapid` | EXISTS | MISSING | G25 | P2 |
| 143 | `swapname` | EXISTS | MISSING | G25 | P2 |
| 144 | `recent` (Recent GUI) | EXISTS | MISSING (17.11) | G25 | P2 |
| 145 | `history` (history GUI) | EXISTS | PARTIAL — `undo`/`redo` chat only, no GUI | G25 | P2 |
| 146 | `show` / `showcase` | EXISTS | MISSING | G23 | P3 |
| 147 | `screenshot` | EXISTS | MISSING | G23 | P3 |
| 148 | `welcome` (Welcome GUI) | EXISTS | MISSING | G23 | P2 |
| 149 | `achievements` | EXISTS | MISSING | G23 | P3 |
| 150 | `help` | EXISTS | MISSING — no help command/GUI | G23 | P2 |
| 151 | `magicitems` | EXISTS | MISSING — ❓ keep or scrap? (dev decision) | G25 | P3 |
| 152 | `editmagicitems` | EXISTS | MISSING — ❓ keep or scrap? (dev decision) | G25 | P3 |
| 153 | `voice` | EXISTS | **SCRAPPED** — Voice Modes dropped (Decision §I) | — | — |

### New-only commands in CB-B (no old equivalent / renamed)

These exist in CB-B and should be confirmed as intentional, not accidental drift:
`incidents`, `admingui`, `draft` / `publish` / `drafts` (staging), `locked`, `favs`, `categories`, `undomode`, `setcategory`, `video` / `extract`, `vault` (stub), `discord` (stub), `download`, `upload`, `diag`.

---

## B. GUI Workflow Audit (Test G01.2)

Old `GuiManager.java` exposes **~80** chest-GUI workflows. New CB-B ships **5** screen classes — and those are screen-based, which Issue 17.3 says must be replaced by chest GUIs.

| New CB-B screen | Covers old workflow(s) | Status |
|---|---|---|
| `MainMenuScreen` | `openMain`, `openFeatureMenu`, `openMenu` | PARTIAL — wrong paradigm (screen, not chest) → G02 |
| `BlockEditorScreen` | `openEditor`, `openEditorPicker`, `openPropertiesGui` | PARTIAL → G02 |
| `ConfigScreen` | `openConfigGui`, `openConfigWarningGui` | PARTIAL → G21 |
| `MacroListScreen` | `openScriptGui` (macros) | PARTIAL → G24 |
| `ArabicBrowserScreen` | `openArabicBrowser` | PARTIAL → G13 |

**Old GUI workflows with NO equivalent in CB-B (MISSING):**
Achievements, AiGui/AiSuggest, Anim (`openAnimGui`, AnimConfirmAbandon), Assignment/Decision pickers, Audit, BgStudio, BlocksGui, BoxNudgeEditor, BrokenBlocks, all Bulk* (AssignPicker, Delete, Hub, OpPicker, RecolorWizard/Confirm), Cache Dashboard, all Category* GUIs (Browser, Controller, Detail, Editor, IconPicker, Picker, Stats, Subcategory, Delete/Merge pickers), ColorFillMode, ColorPicker, ColorStudio, CustomColorStudio, ColorsHub, DeletedBlocks, DeleterConfirm, FaceChange (Picker/Select/Editor), Favorites, Help (Gui/Category), HexRecolorConfirm, ImportConflict, MagicItems, Maintenance, Market, PaletteGenerator, Recent, Recolor/RecoverGui, RenameTool, ResourceHub, SafetyCenter, Shape editor/preview, ShortInputPrompt, SnapshotsGui (scrapped), Sort menus, SoundMenu, Stats, TabIconPicker, Tools, UndoPicker, Variant, VoicePicker (scrapped), Welcome.

**Verdict:** the entire chest-GUI layer is effectively a greenfield rebuild. Group 02 (Chest GUI) is the keystone that most other groups' GUIs depend on.

---

## C. Config Field Audit (Test G01.3)

Old: ~67 public config fields. New: **11**. Status of every old field:

| Old field | New status | Note / restoration group |
|---|---|---|
| `maxSlots` | EXISTS | — |
| `maxUndoDepth` | EXISTS | — |
| `undoMode` | EXISTS | — |
| `defaultTextureSize` | RENAMED → `textureSize` | confirm rename intentional |
| `aiApiKey` | EXISTS | — (AI engine still stub) |
| `discordWebhookUrl` | EXISTS | — |
| `aiApiProvider` | DROPPED | G15 |
| `aiTextureStyle` | DROPPED | G15 |
| `aiMaxVariations` | DROPPED | G15 |
| `aiServerToken` / `aiWorkerUrl` | DROPPED | G15 / G20 |
| `aiTextureEnabled` (new) | NEW | confirm intentional |
| `vaultEndpoint` (new) | NEW (replaces cloud* set) | G20 |
| `cloudShareUrl` / `cloudShareEnabled` / `cloudPackSecret` | DROPPED | G20 |
| `serverIpOverride` | DROPPED | G05 / G20 |
| `resourcePackPort` | RENAMED → `httpPort` / `httpHost` (new) | confirm | 
| `texturePayloadsPerTick` | DROPPED | G05 |
| `reloadDebounceMs` | DROPPED | G05 |
| `instantClickAggressivenessMs` | DROPPED | G05 (silent RP / auto-click) |
| `downloadTimeoutSeconds` | DROPPED | G05 |
| `hudEnabled` (new) | NEW | G03 — but no layout/scale/color/opacity fields yet |
| `hologramEnabled` / `hologramColor` / `hologramHeight` | DROPPED | G19 |
| `particlesEnabled` / `particleCategories` | DROPPED | G19 / G23 |
| `soundsEnabled` / `soundCategories` | DROPPED | G25 |
| `marketplaceEnabled` | DROPPED | G12 |
| `hideCategoryBadge` / `hideCustomBlockText` | DROPPED | G19 |
| `bgRemovalEnabled` / `bgRemovalAutoDetect` / `bgRemovalUseYcbcr` / `bgRemovalTolerance` / `shadowThreshold` / `hasChosenBgMode` / `colorToolBackgroundMode` | DROPPED | G10 |
| `triangleRedHex` / `triangleGreenHex` / `triangleYellowHex` / `triangleBlackHex` | DROPPED | G08 |
| `autoConnectLetters` | DROPPED | G13 (auto-join, 17.20) |
| `autoSnapshotMinutes` | DROPPED | G09 (backup) |
| `trashRetentionDays` | DROPPED | G09 |
| `bulkConfirmThreshold` | DROPPED | G07 |
| `maxGifSizeMb` / `maxGifStripBytes` | DROPPED | G14 |
| `sessionTimeoutSeconds` | DROPPED | G21 / G22 |
| `didYouMeanMode` | DROPPED | G04 |
| `permissionLevelUse` / `permissionLevelAdmin` / `permissionLevelAdminUser` + all 13 `permissionFallback*` | DROPPED | **G22 (Permissions) — entire permission model is gone** |
| `voiceMode` / `VOICE_MODES` | **SCRAPPED** | Decision §I — only intentional drop |

**Verdict:** only `voiceMode` is an intentional drop. **~50 fields** are unaccounted for and map cleanly onto restoration groups — the **permissions model (G22)** and **backup/safety settings (G09)** are the most consequential losses.

---

## D. Top 10 Priority List

| Rank | Item | Why | Group | Priority |
|---|---|---|---|---|
| 1 | Chest GUI architecture | Keystone — most other GUIs depend on it; current screens are wrong paradigm (17.3) | G02 | P0 |
| 2 | Config in-game GUI | Blocks Phase 14 live testing (Discord/vault) — no way to set keys in-game (17.23) | G21 | P1 |
| 3 | HUD system + drag editor | Overlay + `/cb config hud` absent; only on/off toggle (17.4/17.6) | G03 | P1 |
| 4 | Backup / Restore / Safety / Panic | Data-loss risk — BackupManager + Safety Center not ported | G09 | P1 |
| 5 | Permissions model | Entire `permissionLevel*` / `permissionFallback*` config gone — no access control | G22 | P1 |
| 6 | Bulk operations | ~18 `bulk*` commands all missing — major workflow loss | G07 | P1 |
| 7 | Arabic word/auto-join/font | `arabic word` broken (17.18), auto-join missing (17.20), font unbundled (17.19) | G13 | P1 |
| 8 | Omni-Tool (Chisel+Lumina+Deleter) | Tool ecosystem unification (17.9/17.10) | G06 | P1 |
| 9 | Export/Import rework + Marketplace | Export UX rework (17.15); market/marketplace/share missing | G12 | P1 |
| 10 | Silent resource pack delivery | Auto-click mixin for instant pack apply (17.1) | G05 | P1 |

---

## E. Scope Confirmation (Test G01.4 input)

Every MISSING / PARTIAL / BROKEN item above maps to an existing restoration group:

| Group | Theme | Covers (from this audit) |
|---|---|---|
| G02 | Chest GUI | gui, menu, editor, listgui, blocks, *gui, full GUI rebuild |
| G03 | HUD / ESC | edithud, on/off, HUD editor + config |
| G04 | Chat | unsuppress, didYouMeanMode, message system |
| G05 | Resource Pack | resourcepack, rp, silent delivery, RP config fields |
| G06 | Tools | chisel, brush, deleter, settabicon → Omni-Tool |
| G07 | Bulk Ops | bulk + all bulk*, confirm, bulkgui |
| G08 | Shapes | shapes/triangle/rectangle/etc. + triangle hex config |
| G09 | Backup/Safety | backup, restore, safety, panic, expiry, deletedblocks, recover, snapshots(scrapped) |
| G10 | Color/Image | colors, customcolor, gradient, palette, bgstudio, bg-removal config |
| G11 | Category | blockscat, givecategory, importcategory, sharecategory, category GUIs |
| G12 | Export/Marketplace | export rework, import, market, marketplace, templates |
| G13 | Arabic | arabic word/letters/auto-join/font |
| G14 | Animation/Video | pause, resume, gif config |
| G15 | AI Textures | ai command + AI config fields |
| G16 | Diagnostics | diagnostics rework, cache, audit, showbrokenblocks, clear |
| G17 | Regressions | cross-cutting regressions |
| G18 | Notes/Staging | note rework |
| G19 | Display | faces, hologram, text, particles, displayblock, badges |
| G20 | External Integrations | sync, vault, cloud config |
| G21 | Config GUI | config, settings, reset, session config |
| G22 | Permissions | full permission model restoration |
| G23 | Player Experience | welcome, help, achievements, show/showcase, screenshot |
| G24 | Macros | macro (works), script, run, scriptgui |
| G25 | Block Mgmt Extras | find, reid, swapid/name, recent, history, favorite alias, sounds, magicitems(?) |

**No MISSING/PARTIAL/BROKEN item is left without a home group.**

---

## F. Open questions requiring a developer decision

1. **Magic Items** (`magicitems` / `editmagicitems`) — restore, or scrap like Voice? Not currently assigned a clear feature group.
2. **`script` / `scriptgui` / `run`** — are these intentionally superseded by the Macro system (G24), or should the scripting layer be restored separately?
3. **`dress`** — purpose unclear from old source; confirm intended behavior before assigning.
4. **Config renames** — confirm `defaultTextureSize`→`textureSize`, `resourcePackPort`→`httpPort`/`httpHost`, and the new `aiTextureEnabled` flag are intentional.
5. **New-only commands** (`admingui`, `draft/publish/drafts`, `incidents`) — confirm these are intended additions, not accidental scope creep.

---

## Group 01 Verdict

| Test | Description | Result |
|---|---|---|
| G01.1 | Audit table — all 150+ commands accounted for | ✅ (153 rows, no blanks) |
| G01.2 | GUI workflow gaps identified | ✅ |
| G01.3 | Config field coverage complete | ✅ (1 intentional drop: `voiceMode`) |
| G01.4 | Developer sign-off granted | ⬜ awaiting your review |

**Group 01 passes only when you explicitly approve this audit. No other group may begin implementation until G01.4 is ✅.**

If anything is wrong, reply with: (1) the specific gap, (2) the old feature/command missed, (3) the suggested restoration group.
