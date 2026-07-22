# UI Medium Guide — chat, chest GUI, or Screen?

Companion to `GUI_DESIGN_GUIDE.md` (that one is *how* to build a good chest GUI once you've picked
one — polish rules, colour language, sound). This one is *which medium to pick in the first place*.
Decide here first, then go build it well over there (or in a future Screen-equivalent guide).

## The 3-question test

Ask in this order — first "yes" wins:

1. **Single fire-and-forget action, nothing to browse or preview?** (a delete, a toggle, a one-shot
   confirm) → **chat command**, with buttons if it needs a follow-up click (`Chat.runButton`/
   `copyButton`/`suggestButton`). No GUI at all.
2. **Just a list to browse + click a slot to run/open something**, and item icons genuinely help
   (ticking multiple blocks, picking one from a grid)? → **chest GUI**. Cheap, reuses
   `ChestMenu`/`Icons`/`GuiFx`, follow `GUI_DESIGN_GUIDE.md`.
3. **Anything with real interactive richness** — sliders, drag, a live preview/dummy, tabs, animation,
   text-entry fields, anything that needs to feel alive while you're using it? → **Screen** (custom
   `Screen` class via `GuiEngine`). A chest grid physically cannot do this well — every attempt to fake
   it (fake sliders out of glass panes, etc.) reads as a workaround, not a feature.

If none of the three clearly fit, that's a sign the feature itself is still fuzzy — go back to what it's
actually supposed to do before picking a medium.

## Current house lean (as of 2026-07-09)

Not a hard rule, but the pattern in every recent decision: **chest GUI is the "old/basic" tier now,
Screen is where new richness goes.** Two sessions in a row chose Screen over chest GUI for things that
used to default to chest:

- `GROUP_04_Communication.md` §G04-4 — `/cb list`/`/cb macro`/`/cb backup` drop their separate `<sub>gui` chest
  commands; the base command opens a full screen directly (Advancements-style). Template gets its own
  screen instead of staying chat-only.
- `GROUP_03_HUD_ESC.md` §G03-2 — the whole HUD/ESC rework is screens + a multi-widget system, nothing
  chest-shaped in it.

Don't read this as "always pick Screen" — question 2 above (the picker/bulk-select case) is still a real,
correct use for chest GUI. `BulkPropertyMenu`/`BulkFilterMenu` (the `GUI_DESIGN_GUIDE.md` reference
implementation) and `/cb listgui`'s tick-to-bulk-select flow are the model: grid + icons + multi-select
is what chest GUI is *for*. The lean is against using chest GUI as a catch-all for anything with a screen
in front of it, which is what `<sub>gui` commands had drifted into.

## Per-group triage table

Filled in as each group gets a real medium decision (not guessed — either tested owner preference or an
explicit ruling like G04-4/G03-2 above). Blank = not decided yet.

| Group | Feature | Medium decided | Where |
|---|---|---|---|
| G03 | HUD widgets + ESC panel | Screen(s) | `GROUP_03_HUD_ESC.md` §G03-2 |
| G04 | list / macro / backup / template / achievements | Screen (was chat+`<sub>gui`) | `GROUP_04_Communication.md` §G04-4 |
| G25 | `/cb listgui` tick-select for bulk ops | **Superseded 2026-07-10** — `listgui`/`blockslist` deleted; `/cb list` opens the G07 Bulk Operations Hub (**Blocks List** tab). The earlier "chest GUI (kept)" ruling no longer holds. | `GROUP_07_BULK_OPERATIONS.md` §G07-3/§G07-5 |
| G21 | Settings Book (config GUI) | Screen — **not built** | `GROUP_21_CONFIG_GUI.md` (top note) |
| G10 | RecolorSliderScreen + EyedropScreen + BgStudioMenu + ColorVariantsMenu + ColorsMenu (hub) + PaletteMenu + GradientPickerMenu + ColorPickBlockMenu | ALL merge into ONE Coloring Screen — **not built** | `GROUP_10_COLOR_IMAGE.md` |
| G13 | ArabicHubMenu, ArabicListMenu, ArabicGroupMenu, WordChoiceMenu, ColorStudioMenu + existing ArabicPreviewScreen/ArabicBrowserScreen | ALL merge into ONE Arabic Screen — **not built** (+ professionally-labeled Colour Variants button) | `GROUP_13_ARABIC.md` |
| G14 | AnimListMenu (animated-blocks picker) | Fold into Block Creation Studio Screen — **not built** | `GROUP_14_ANIMATION_VIDEO.md` |
| G08 | Shape editor (custom bounding-box) + FaceEditorMenu (per-face texture) | Screen (fold into G27 Block Studio) — **not built**, re-confirms an unexecuted 2026-06-21 decision | `GROUP_08_SHAPES.md` §4 |
| G31 | BuzzerGame admin panel (whole thing, incl. target-time field) | Screen — **not built** | `GROUP_31_BUZZERGAME.md` (Admin panel section) |
| G06 | RecolorConfirmMenu, HexRecolorConfirmMenu, OmniMenu (confirm/mode popups) | Chest GUI (kept) — **flagged for design polish pass** | `GROUP_06_TOOLS.md` |
| G06 | HexColorsMenu, CustomColorMenu (hex/colour pickers) | Screen — **not built** | `GROUP_06_TOOLS.md` |
| G16 | ItChestMenu, AuditMenu, ReportMenu, DebugLogMenu (diagnostics family) | One tabbed Screen — **not built** | `GROUP_16_DIAGNOSTICS.md` |
| G16 | FeedbackMenu (FX toggle grid) | Screen (separate from IT) — **not built** | `GROUP_16_DIAGNOSTICS.md` |
| G09 | Backup, Trash, Broken-Blocks, Safety hub (whole group) | Screens — **not built** | `GROUP_09_BACKUP_SAFETY.md` |
| G11 | CategoryListMenu/CategoryBrowserMenu/CategoryEditMenu/CategoryBlockMenu | Retired — already replaced by `CategoryHubScreen` (G27), needs wiring | `GROUP_11_CATEGORY.md` |
| G11 | ExportDashboardMenu (scope/format picker) | Screen — **not built** | `GROUP_11_CATEGORY.md` |
| G18 | NotesMenu (Lore GUI) | Screen — **not built** | `GROUP_18_NOTES_STAGING.md` |
| — | Existing Screens confirmed right medium, but flagged for a design/UX improvement pass (not a medium change): HudEditorScreen (G27, owner: "currently shit") + its 3 sub-popups (HudBrickInspector, HudColorPicker, HudPresetBrowser — same polish flag, they're part of the same editor), VaultConflictScreen (G20), GuessSettingsScreen (G30), ShapeEditorScreen (G27), BlockCreationStudioScreen (G27), RecordOverlayStudioScreen (G29), CategoryHubScreen (G27) | Screen (kept) — **flagged for polish pass** | n/a |
| — | CbScreenTemplate.java (`client/gui/`) — explicitly a dev template, header says "NOT a real screen, do not register/reference" | N/A — not a real feature | n/a |
| G25 | ReIdMenu (change block id) | Screen — **not built** | `GROUP_25_BLOCK_MANAGEMENT_EXTRAS.md` |
| G05 | TextureSizeMenu + RetextureConfirmMenu | Screen — **not built** | `GROUP_05_RESOURCE_PACK.md` |
| — | SearchMenu (/cb search results) | Screen — **not built** | `docs/UI_MEDIUM_GUIDE.md` (no clear owning group; core search feature) |
| G07 | BulkHubMenu, BulkSelectMenu, BulkActionMenu, BulkConfirmMenu (bulk-op flow) **+ BlockListMenu** | One `BulkWorkbenchScreen` — the **Bulk Operations Hub**: 2 LEFT-rail tabs (Blocks List / Bulk Actions), **10 ops** incl. Recolor. All 5 chest menus deleted. 🟢 rebuilt to §G27.22/§G27.22b 2026-07-12 (build-green, not in-game) | `GROUP_07_BULK_OPERATIONS.md` §G07-5 · screens `GROUP_27_SCREENS.md` §G27.22/b |
| G07 | G07-2 Advanced Bulk Recolor Hub (`/cb bulkrecolor`) — 11-tile slider screen, already locked 2026-06-24/25, **not built at all** (previous memory calling this "already Screen, settled" was WRONG — nothing exists in code yet) | Screen, reusing the `RecolorSliderScreen` pattern — now that G10 merges into one Coloring Screen, this should reuse THAT screen, not a 4th separate one | `GROUP_07_BULK_OPERATIONS.md` §G07-2 |
| G21 | StepperMenu, SubChestMenu, ConfigWarnMenu | Fold into Settings Book Screen (already decided) | `GROUP_21_CONFIG_GUI.md` |
| G21 | ConfigScreen (Mod Menu quick-view) — conflicted with Settings Book | Merge into the SAME Settings Book Screen — **one config screen total** | `GROUP_21_CONFIG_GUI.md` |
| G21 | ConfigMenu.java (old flat dashboard, `gui/chest/`) | **Dead code** — GuiRouter's `case CONFIG` routes to SettingsBookMenu, not this. Flag for deletion, not a medium decision. | `GROUP_21_CONFIG_GUI.md` |
| — | MainMenuScreen + BlockEditorScreen (`gui/screens/`, GuiMode ids 0/1) — possible dead/legacy pre-chest screens, still wired but likely superseded by MainMenu/EditorMenu chest (G02) | **Undecided — needs in-game check** whether anything still opens them | `docs/UI_MEDIUM_GUIDE.md` |
| G28 | UndoMenu, HistoryMenu (labeled Group 02 in file headers but actually owned by G28's undo rework) | `UndoHistoryScreen`, 3 tabs (Undo/Redo/Audit Log) — **already locked 2026-06-25, not built** | `GROUP_28_CREATE_STUDIO.md` |
| G02 | MainMenu, EditorMenu, MagicMenu, ConfirmMenu (reverses G02's old chest-only mandate) | Screen — **not built**. *`BlockListMenu` moved out of this row 2026-07-10 — G07 §G07-3 owns and deletes it.* | `GROUP_02_CHEST_GUI.md` |
| G02 | ChestMenu (base infra class, not a feature) | N/A — unaffected | n/a |

**Already Screen, settled, no action:** G02 (Block Studio), G13 (Arabic live-preview), G14 (Animation, owned
by G27), G28 (Undo/History), G07 (Bulk Recolor slider), G29 (Creator Tools overlay).

## ⚠️ Verify against code, not just the group doc

G10 above is a live lesson: the group doc said "chest GUI" but the actual feature (`RecolorSliderScreen`)
had already been built as a Screen — the doc just never got updated. Before triaging any more groups,
check `GuiMode.java` (`gui/GuiMode.java`) and `CustomBlocksClient.java`'s `OpenGuiPayload` switch first —
that's the authoritative list of every Screen that's actually wired today. Cross-reference against
`GuiRouter.java` (`gui/chest/GuiRouter.java`) for the chest-menu equivalent. Don't trust a group doc's
"chest GUI" claim without checking both. (Same lesson as [[feedback_verify_tg_against_source]].)

*(rest of the groups not triaged yet — G01/G17/G22/G20 skipped as backend/no-player-UI. 57 chest menus and
19 Screens exist total; full per-menu inventory not yet done, see handoff.)*
