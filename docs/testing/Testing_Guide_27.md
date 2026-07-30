# Group 27 - Unified Screens, Create Studio & Editing

## Status

| | |
| --- | --- |
| **Verdict** | Studio baseline exists; new upgrades, shared browser filters, Editing/Text tabs, category Screens, and the wider screen migration still need owner tests or build work. |
| **Progress** | 🟩🟩🟩🟥🟥🟥🟥🟥🟥🟥 30% |
| **Last tested** | 2026-07-12 |
| **Jar** | `customblocks-1.0.0.jar` |

## Sections

| § | Feature | Status | Flags |
| --- | --- | --- | --- |
| Q | Background Studio, palette, and coloring engine flows (from G10) | Built 🎯 | - |
| B | Studio five-fix upgrade set | Built 🎯 | Discussion ✏️ |
| C | HUD templates and shape backgrounds | Built 🎯 | Discussion ✏️ |
| D | Bulk Operations Hub screens | Built 🎯 | Discussion ✏️ |
| E | Studio Editing tab with Paint and Resize | Designed ⏳ | Discussion ✏️ |
| F | Studio Text creation tab | Designed ⏳ | Discussion ✏️ |
| G | Studio Edit Mode and unified editor | Designed ⏳ | Discussion ✏️ |
| H | Recolor and Shape fold-in | Designed ⏳ | Discussion ✏️ |
| I | Screen toasts and unified feedback style | Designed ⏳ | Discussion ✏️ |
| J | Wider screen migration set | Designed ⏳ | Discussion ✏️ |
| L | Shared browser Animated filter and animation route | Designed ⏳ | Discussion ✏️ |
| S | `/cb create` Category workspace (from G11) | Built 🎯 | Polish 🎨 |
| M | Omni-Tool mode-switch Screen | Planned 📜 | Discussion ✏️ |
| N | Backup and Trash Screen migration (from G09) | Planned 📜 | Discussion ✏️ |
| O | Advanced Recolor Hub screen (from G10) | Planned 📜 | Discussion ✏️ |
| P | Color-family builder Screen (from G10) | Planned 📜 | Discussion ✏️ |
| R | CategoryHubScreen routing for category browser (from G11) | Designed ⏳ | - |
| T | Export Dashboard Screen replacement (from G11) | Designed ⏳ | - |
| U | CategoryHubScreen full-parity manager (from G11) | Designed ⏳ | U9-U13 built 🎯 |
| A | Block Creation Studio baseline | Done ✅ | - |
| K | Premium future screen ideas | Planned 📜 | Parked 💤 |

**Original Group:** [GROUP_27_SCREENS.md](../groups/GROUP_27_SCREENS.md)

---

# Active Tests

## 💡 Setup

- Use `/cb create`, `/cb edithud`, `/cb gui macros`, and any screen under test.
- Keep one static PNG block, one transparent PNG, one GIF/animated block, and one shaped block.
- Screen layout must follow the red/black/lime G27 standard.
- Logic owned by other groups stays there; G27 tests the Screen and user flow.
- Text creation uses G13 backend; Editing tab uses G10/G14 texture behavior; Testing Center UI support stays with G16; category Screens (§R/§S/§T) call G11 data and mutation paths.

## A - Block Creation Studio baseline - Done ✅

| | |
| --- | --- |
| **Check** | `/cb create` opens the Block Creation Studio and the original creation path still works. |
| **Pass rule** | Open, identity, texture URL, shape, attributes, category text, create, cancel, dim/help, and command compatibility rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| A1 | Run `/cb create`. | Block Creation Studio opens. | ✅ | ✅ |
| A2 | Fill identity, texture URL, shape, attributes, and category. | Each value carries into the creation payload. | ✅ | ✅ |
| A3 | Press Create & Publish. | A real block is created with chosen values. | ✅ | ✅ |
| A4 | Press Cancel or Esc. | No unwanted block is created. | ✅ | ✅ |
| A5 | Use old `/cb create <id> <name> <url>` form. | Command path still works unchanged. | ✅ | ✅ |

## B - Studio five-fix upgrade set - Built 🎯

| | |
| --- | --- |
| **Check** | The latest Studio fix jar should remove accidental publish, fix layout overlap, preserve transparent-image background color, and improve category controls. |
| **Pass rule** | Enter, hex layout, image background, section checks, hints, category assign/add/rename/color/default/delete, and publish rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but needs owner test pass. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| B1 | Click any field, type, and press Enter. | Enter confirms the field only; it never publishes. | 🎯 | 🎯 |
| B2 | Use the `#RRGGBB` field and Use hex button. | Field and button do not overlap; color applies. | 🎯 | 🎯 |
| B3 | Load transparent PNG, then choose background color. | Image stays; color fills behind transparent parts and publishes correctly. | 🎯 | 🎯 |
| B4 | Clear background with the clear swatch. | Image remains and background clears. | 🎯 | 🎯 |
| B5 | Fill sections and hover attributes. | Tabs show useful complete marks and panels show short hints. | 🎯 | 🎯 |
| B6 | Use Category chips. | Assign, add, rename, color, set default, and confirm-delete act on the intended category. | 🎯 | 🎯 |

## C - HUD templates and shape backgrounds - Built 🎯

| | |
| --- | --- |
| **Check** | HUD editor supports pill/glow/box/plain backgrounds, accent colors, and template-token bricks. |
| **Pass rule** | Default pill, shape picker, accent, old HUD load, template brick, token chips, persistence, and off-block hiding rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Built but needs owner test pass. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| C1 | Run `/cb edithud` and add/reset a brick. | New brick uses rounded pill background with accent stripe. | 🎯 | 🎯 |
| C2 | Cycle shape picker. | Pill, glow box, box, and plain render correctly. | 🎯 | 🎯 |
| C3 | Change accent color. | Stripe/border color updates and persists after relog. | 🎯 | 🎯 |
| C4 | Load an older saved HUD. | It loads safely and applies pill look without moving old bricks. | 🎯 | 🎯 |
| C5 | Add Template brick with `{name} [{id}]`. | It resolves live while aiming at a custom block and hides off-block as designed. | 🎯 | 🎯 |
| C6 | Click token chips. | Tokens insert into the template text box and resolve in-game. | 🎯 | 🎯 |

## D - Bulk Operations Hub screens - Built 🎯

| | |
| --- | --- |
| **Check** | Blocks List and Bulk Actions tabs use the locked screen design while bulk logic stays in G07. You pick blocks by **ticking** them and narrowing the list with **filters** — the old typed "ask" / NL command bar is gone. |
| **Pass rule** | Chrome, search, filter chips, the new **Category ▾** filter, **combined** filters, rotating cube grid, selection, left info panel, history popup, actions rail, result preview, confirm, progress sweep, and undo/redo rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **How to open** | Type `/cb bulk` in chat — opens the Bulk Operations Hub screen directly. |
| **Built (2026-07-20)** | The typed "ask" command bar is gone; targeting is ticking plus filters, and a **Category ▾** filter combines (AND) with the All/Favorites/Locked/Selected chip. |
| **Blocked** | Built to spec but not owner-tested in game. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| D1 | Type `/cb bulk` to open the Hub. | Two left tabs appear: Blocks List and Bulk Actions, no third tab. **No "ask…" text box** anywhere. | 🎯 | 🎯 |
| D2 | Use search, filter chips, sort, and the scrollbar in Blocks List. | Grid updates while the cube spin stays phase-locked. | 🎯 | 🎯 |
| D3 | Left-click and right-click tiles. | Left targets (ticks) the block; right opens the left info panel with hero cube and actions. | 🎯 | 🎯 |
| D4 | Open the History popup. | Opaque popup appears over the dimmed screen and can jump back through steps. | 🎯 | 🎯 |
| D5 | Switch to Bulk Actions and choose each op. | Left action rail, header controls, affects line, row previews, and result preview update. | 🎯 | 🎯 |
| D6 | Execute an operation. | Confirm dialog, red sweep progress, completion feedback, and undo/redo all work. | 🎯 | 🎯 |
| D7 | Click **Category ▾** and pick a category (e.g. red). | The grid shows only blocks in that category; the button shows the picked category. Pick "All categories" to clear it. | 🎯 | 🎯 |
| D8 | Turn on a chip (Locked or Favorites or Selected) **and** a Category together. | The grid shows only blocks matching **both** — e.g. Locked + red = only locked red blocks. | 🎯 | 🎯 |
| D9 | Select blocks via ticks/filters, switch to Bulk Actions, Execute. | Only the ticked blocks are affected — nothing silently acts on the whole list. | 🎯 | 🎯 |
| D10 | Place 3 blocks, tick those 3 in Blocks List, close the screen, then type `/cb setbg selected green`. | All 3 turn green; one `/cb undo` puts all 3 back. Tests that the screen's tick selection survives to a chat command. (Moved from G10's background tests on 2026-07-25; the setbg logic itself stays G10.) | 🎯 | 🎯 |

## E - Studio Editing tab with Paint and Resize - Designed ⏳

| | |
| --- | --- |
| **Check** | `/cb create` should gain an Editing tab that unifies painting and resizing without naming it only Image. |
| **Pass rule** | Tab name, Paint mode, dedicated Resize mode, after-create editing, GIF resize, 2D preview, controls, undo/redo, save/cancel, and status placement rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design needs final build; G10/G14 own image/animation data behavior behind it. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| E1 | Open `/cb create` and select Editing. | Tab is named Editing and lives with the Studio tabs. | ⏳ | ⏳ |
| E2 | Switch between Paint and Resize modes. | They feel connected but each has its own controls; Resize is dedicated and easy to find. | ⏳ | ⏳ |
| E3 | Create a block, reopen it in Edit Mode, and resize. | Editing after creation works, not only during initial create. | ⏳ | ⏳ |
| E4 | Resize a PNG/image. | User can control target size, scale, aspect behavior, crop/fit behavior, and preview before save. | ⏳ | ⏳ |
| E5 | Resize a GIF/animated source. | Frame grid/timing stays valid and G14 animation data remains consistent. | ⏳ | ⏳ |
| E6 | Paint a static image. | Paint tools work for static textures; GIF painting remains unavailable for now. | ⏳ | ⏳ |
| E7 | Move around a locked 2D preview/canvas. | 2D view is movable/zoomable without turning into an uncontrolled 3D spin. | ⏳ | ⏳ |
| E8 | Use `/cb undo` and `/cb redo` after save. | Saved editing operations are undoable/redone through normal history. | ⏳ | ⏳ |
| E9 | Watch the status/progress area. | Status sits in a cool bottom-left or bottom-right placement without clutter. | ⏳ | ⏳ |
| E10 | Cancel or close with unsaved edits. | Clear discard/keep editing behavior appears. | ⏳ | ⏳ |

## F - Studio Text creation tab - Designed ⏳

| | |
| --- | --- |
| **Check** | The old `/cb arabic word <arg> <arg>` choose-one-block menu becomes a proper Studio Text tab owned by G27. |
| **Pass rule** | Command route, tab open, text fields, block choice, color choice, preview, save, old menu removal, G13 backend handoff, and Unicode rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | G13 owns renderer/backend; G27 owns this Screen flow. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| F1 | Run `/cb arabic word <arg> <arg>` and choose one block. | Flow opens the Studio Text tab instead of the old root menu. | ⏳ | ⏳ |
| F2 | Open `/cb create` and select Text. | Text creation is a separate Studio tab/section, name finalization allowed later. | ⏳ | ⏳ |
| F3 | Enter Arabic/Unicode text in a real text box. | Brigadier Unicode limitations are bypassed by Screen input. | ⏳ | ⏳ |
| F4 | Choose color/style/block output. | Preview updates without using the retired Color Studio chest menu. | ⏳ | ⏳ |
| F5 | Publish the text block. | G13 backend creates the right text/Arabic block data. | ⏳ | ⏳ |
| F6 | Try unsupported text/backend state. | Error is human-readable and stays in the screen. | ⏳ | ⏳ |
| F7 | Confirm old G13/root screen references. | Text screen ownership is removed from G13/TG13 and lives here. | ⏳ | ⏳ |

## G - Studio Edit Mode and unified editor - Designed ⏳

| | |
| --- | --- |
| **Check** | Existing blocks should open in the Studio for editing all settings in place. |
| **Pass rule** | Pick existing, `/cb editor` and `/cb editor <id>` routing, lock guard, edit all sections, save changes, save as copy, re-id, draft, and undo/redo rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Exact picker interaction, landing section, Manage actions, and other editor-route extras remain Discussion ✏️ for later. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| G1 | Choose Edit existing from `/cb create`. | Block picker opens and loads selected block into Studio. | ⏳ | ⏳ |
| G2 | Run `/cb editor <id>`. | It opens Studio Edit Mode instead of the old chest editor after cutover. | ⏳ | ⏳ |
| G3 | Edit identity, texture, category, shape, and attributes. | Changes preview safely and save as one operation. | ⏳ | ⏳ |
| G4 | Save as copy. | New block copies settings without overwriting original. | ⏳ | ⏳ |
| G5 | Edit a locked block. | Lock guard prevents unsafe edit and explains what to do. | ⏳ | ⏳ |
| G6 | Run `/cb editor` with no block ID. | The same modern block browser as `/cb list` opens; the removed `/cb listgui` chest route does not return. | ⏳ | ⏳ |
| G7 | Choose a block through the `/cb editor` browser route. | The chosen block opens in Studio Edit Mode through the same route as `/cb editor <id>`. | ⏳ | ⏳ |

## H - Recolor and Shape fold-in - Designed ⏳

> Moved from G08 (2026-07-20): §C shape editor (`/cb shapeeditor`, still a chest GUI) and the advanced face editor both fold in here. This section owns converting them to Studio screens (H6 below). The G08 face-rotation commands are already built and passing; only the live-cube face-picker screen remains.

| | |
| --- | --- |
| **Check** | Standalone recolor and shape screens should fold into Studio sections. |
| **Pass rule** | `/cb recolor`, no-id picker, `livecolor` retirement, live preview, single bake, split preview, presets, eyedrop, per-zone tint, shape route, and old button route rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Requires Studio Edit Mode foundation. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| H1 | Run `/cb recolor <id>`. | Studio opens in Edit Mode focused on Recolor. | ⏳ | ⏳ |
| H2 | Run `/cb recolor` with no id. | Picker opens, then Studio Recolor section opens. | ⏳ | ⏳ |
| H3 | Run `/cb livecolor <id>`. | Old command is retired with no hidden alias if that remains the locked decision. | ⏳ | ⏳ |
| H4 | Drag recolor sliders. | Cube previews live; placed world block updates only once on Save. | ⏳ | ⏳ |
| H5 | Use split preview, presets, eyedrop match, and per-zone tint. | All appear and work inside Recolor section. | ⏳ | ⏳ |
| H6 | Run `/cb shapeeditor <id>`. | Studio opens to Shape section, not standalone ShapeEditorScreen. | ⏳ | ⏳ |
| H7 | Use old chest buttons that referenced recolor/shape. | Buttons relabel/reroute to Studio sections. | ⏳ | ⏳ |

## I - Screen toasts and unified feedback style - Designed ⏳

| | |
| --- | --- |
| **Check** | In-screen feedback should become G27 toasts/action overlays while typed commands still reply in chat. |
| **Pass rule** | Success, error, copy, save, overload, hotbar/action-bar style, sound, typed-command separation, and all-screen coverage rows pass after build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Coordinates with G04 chat/hotbar style and G16 diagnostics error code search. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| I1 | Click Save inside a screen. | Top-right toast appears; chat stays empty. | ⏳ | ⏳ |
| I2 | Trigger a screen validation error. | Red/error toast appears without chat spam. | ⏳ | ⏳ |
| I3 | Run a normal typed command. | Command still replies in chat as before. | ⏳ | ⏳ |
| I4 | Fire many notifications quickly. | They aggregate or handle overload without audio/visual spam. | ⏳ | ⏳ |
| I5 | Compare hotbar/action-bar feedback. | Style and sound cues align with the G27 red/black/lime language. | ⏳ | ⏳ |

## J - Wider screen migration set - Designed ⏳

| | |
| --- | --- |
| **Check** | Screens folded from other groups should follow G27 standards while their behavior remains owned by the original group. |
| **Pass rule** | Macro, Vault Conflict, Vault Hub, Update, Achievements, Tutorial, Backup, Trash, Safety, Record Overlay, Guess settings, Config, Dashboard, Undo/Redo, and History rows pass in their owner TGs. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Each screen waits on its owner group's build slice. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| J1 | Open each migrated Screen. | It uses G27 frame, spacing, keyboard behavior, and modal rules. | ⏳ | ⏳ |
| J2 | Verify owner group logic. | G27 does not steal behavior ownership from G07/G09/G16/G20/G23/G24/G29/G30. | ⏳ | ⏳ |
| J3 | Cut over an old route after confirmation. | Old chest/dead route is removed only after owner confirms replacement. | ⏳ | ⏳ |
| J4 | Check long text config fields. | Real text boxes replace anvil-paste workarounds where appropriate. | ⏳ | ⏳ |

## L - Shared browser Animated filter and animation route - Designed ⏳

| | |
| --- | --- |
| **Check** | Every Screen that browses custom blocks should offer one shared `Animated` filter and `/cb animation` should use that Screen. |
| **Pass rule** | Filters menu, animated-only results, combined filters, shared-browser coverage, command route, Studio handoff, and old-picker retirement rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | - |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| L1 | Open `/cb list`, then open Filters and select `Animated`. | Only GIF and animated WebP blocks remain visible. | ⏳ | ⏳ |
| L2 | Combine `Animated` with Favorites, Locked, a category, or another available filter. | Results satisfy every selected filter. | ⏳ | ⏳ |
| L3 | Type in search while `Animated` is selected. | Search narrows the animated results by block name or ID. | ⏳ | ⏳ |
| L4 | Open another Screen that browses custom blocks. | The same Filters menu and `Animated` behavior are available there. | ⏳ | ⏳ |
| L5 | Run `/cb animation`. | The shared block browser opens with `Animated` already selected. | ⏳ | ⏳ |
| L6 | Choose an animated block from that route. | `/cb create` opens that existing block directly on its Animation tab. | ⏳ | ⏳ |
| L7 | Inspect the available filters. | There is no redundant Static filter; `All` remains the normal view. | ⏳ | ⏳ |
| L8 | Confirm the Screen route after cutover. | The old animated-only chest picker is no longer opened or maintained. | ⏳ | ⏳ |

**Later discussion:** decide the remaining Filters-menu contents and visual layout without changing the approved `Animated` behavior.

## M - Omni-Tool mode-switch Screen - Planned 📜

| | |
| --- | --- |
| **Check** | Reopened 2026-07-20: owner wants a Screen for switching Omni-Tool modes (Glow/Hardness/Face/Copy/Delete), replacing the G06 §F in-hand sneak+right-click cycle. G06 scrapped this on 2026-07-19 as in-hand-only; that decision is now reopened and ownership moves here. |
| **Pass rule** | Not designed yet — layout, trigger (keybind/item use?), and whether §F in-hand cycling is kept as a fallback all need Discussion before build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design not locked. Needs owner discussion: screen trigger, layout (red/black/lime per G27 standard), and whether in-hand cycle (G06 §F) stays as an alt path or is fully replaced. |

*(No test rows yet — design discussion pending.)*

## N - Backup and Trash Screen migration (from G09) - Planned 📜

| | |
| --- | --- |
| **Check** | Moved here 2026-07-23: G09's backup engine is rebuilt and complete at the command/data layer (whole-tree deduped snapshots, kind-based retention, verify, preview/contents, granular restore, cloud pull). The remaining work is the Screen surface, owned by G27. |
| **Pass rule** | Not designed yet — layout and scope need Discussion before build: multi-select delete on the Backup Screen; surfacing the new metadata (kind, reason/note, size, a health dot from `/cb backup verify`, preview/contents/granular-restore actions); a new Trash Screen; and retiring the old backup, trash, and safety chest menus. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design not locked. Needs owner discussion: exact layout/tabs, whether per-block "restore just this block from backup X" is in-scope, and cutover order (Screen accepted before chest removal). G09 keeps persistence/restore/retention semantics. |

**Also folded in from G09 (2026-07-23):**
- **Auto-backup config gate (was TG9 §C4-C6):** `autoBackupTime`, `autoBackupBudgetMB`, and the `/cb config` confirmation gate aren't exposed as testable options today — owner wants them reachable purely in-game, no file editing. Scope as config-screen controls here rather than raw config keys. G09 keeps the interval/keep-count/prune logic itself (already confirmed, TG9 §C1-C3); only the missing config-exposure UI moves here.
- **Preview / contents / granular restore (was TG9 §J):** currently three separate console commands (`preview`, `contents`, scoped `load <name> <item>`) with no screen. Owner does not want a console/file workflow. Scope as preview + per-item restore buttons on the Backup Screen.

*(No test rows yet — design discussion pending.)*

## O - Advanced Recolor Hub screen (from G10) - Planned 📜

> Moved from G10's Advanced Recolor Hub scope (2026-07-24): spec fully locked, nothing built. This is the full 11-tile Recoloring Hub screen (selection/operations/layout/undo), **not** the same thing as the live `/cb bulkrecolor` command — that's a separate single hue-shift op, already built and shipped, and it stays where it is. Command-name overlap between this future Hub and the existing `/cb bulkrecolor` is unresolved — pick the Hub's actual command/route as part of Discussion before build (do not assume it reuses `/cb bulkrecolor`).

| | |
| --- | --- |
| **Check** | A dedicated advanced screen for bulk recolor operations, distinct from G07's existing single hue-shift command. |
| **Pass rule** | Not designed yet — selection, operations, layout, command/route naming, safety, progress/cancel behavior, and undo all need Discussion before build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design not locked. Needs owner discussion: final command/route (must not silently collide with G07's live `/cb bulkrecolor`), screen layout per G27 red/black/lime standard, selection flow, and relationship (if any) to G10's still-undesigned unified `background` attribute. |

*(No test rows yet — design discussion pending.)*

## P - Color-family builder Screen (from G10) - Planned 📜

> Moved from G10's colour-family scope (2026-07-24): the `/cb colorvariants` command/service rail stays in G10 — confirmed built. This is only the future GUI that gathers source/color-set/preview inputs and calls that same command rail (must not duplicate image logic in the screen). The existing colour-variants chest menu only lists families today — a starting point to replace, not a finished builder.

| | |
| --- | --- |
| **Check** | A Screen supplies source, color set, preview, and create/regen/delete flow, calling G10's existing command rail rather than reimplementing it. |
| **Pass rule** | Not designed yet — layout, input flow, overwrite-guard UI, and delete-confirmation UI all need Discussion before build. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | Design not locked. Needs owner discussion: screen trigger/route (from `/cb create` Studio? standalone?), layout per G27 red/black/lime standard, how overwrite-guard and delete-confirm surface in a Screen vs the current chat-confirm flow, and whether `ColorVariantsMenu` is replaced outright or extended. |

*(No test rows yet — design discussion pending.)*

## Q - Background Studio, palette, and coloring engine flows (from G10) - Built 🎯

> Moved from G10's stored-background scope (2026-07-24): whole section requires the Background Studio screen, so ownership moves here entirely. Audited as fully wired — no placeholder or dead buttons.

| | |
| --- | --- |
| **Check** | Background removal, fill color, palette, and color-variant operations all work through the Background Studio screen and its supporting commands. |
| **Pass rule** | Background, fill color, palette persistence, variant creation, and undo rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

**How to open the panel:** type `/cb bgstudio <id>` on a block, or just `/cb bgstudio` to pick a block from a list first.

**Panel buttons, in plain terms:**
- 4 mode tiles at the top: **Keep background** (do nothing), **Remove background** (cuts only the outer edge background), **Remove background + gaps** (also cuts background trapped inside the subject, e.g. inside a letter "O"), **Smart auto** (claims to pick automatically — it does not, it just applies a hidden fixed strength). When G10 §H lands these become **Auto** and **Off**, and Smart auto is deleted outright.
- **Strength +/− 5%** buttons: how loosely a color counts as "background". Being deleted — see the Q3 note below.
- **Fill color**: left-click a color swatch to type a name or hex code in an anvil; right-click resets to black (`#000000`).
- 4 quick-fill slots: these pull from your saved Palette (set up separately via `/cb coloring` → Palette tab). Click one to use that color as fill instantly.
- There is no separate "make color variant" button on this panel — that's the separate `/cb colorvariants` command (G10 §F).

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| Q1 | Open `/cb bgstudio g10a`. Click **Remove background** tile. Right-click the fill color swatch (resets to black). Apply. | Background turns black. Run `/cb undo` — background restores to original. | 🎯 | 🎯 |
| Q2 | Same panel. Left-click the fill color swatch, type `red` (or a hex like `#FF0000`) in the anvil, confirm, apply. | Removed background bakes to red instead of black. | 🎯 | 🎯 |
| Q3 | ~~Tolerance persistence~~ — do not run. `/cb tolerance`, the strength buttons, and the per-block store are all being deleted by [G10 §H](../groups/GROUP_10_COLOR_IMAGE.md). Testing that they persist would be testing something on its way out. This row is replaced by G10 §H2, which checks the buttons are gone. | 💤 | 💤 |
| Q4 | Open `/cb coloring` → Palette tab. Save a color, close, reopen the tab. | Saved color is still there. It also appears as a clickable quick-fill slot in Background Studio (see Q2). | 🎯 | 🎯 |
| Q5 | Run `/cb colorvariants g10a Test <link>` (there's no in-panel button, see note above). | A red/green/yellow variant family is created from `g10a`. `/cb undo` removes it. | 🎯 | 🎯 |

## R - CategoryHubScreen routing for category browser (from G11) - Designed ⏳

> Moved from G11 (2026-07-25): G11 keeps category data, records, and mutation paths; this Screen calls those paths rather than reimplementing them.

| | |
| --- | --- |
| **Check** | Category browser entry points route to `CategoryHubScreen` instead of the old chest menus. |
| **Pass rule** | Categories list, category detail, block row, edit, remove, give, and console fallback pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |
| **Blocked** | CategoryHubScreen exists, but the old `/cb categories` and category browser routing still need migration. |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| R1 | Run `/cb categories` as a player. | CategoryHubScreen opens with all categories, counts, icons, and browse/edit affordances. | 🎯 | 🎯 |
| R2 | Open `testcat`. | The screen shows `g11a`, `g11b`, and `g11c`, with category controls available. | 🎯 | 🎯 |
| R3 | Click a block row. | Give, edit, and remove-from-category actions are available in-screen. | 🎯 | 🎯 |
| R4 | Run the same command from console. | Console receives a text list instead of a screen open attempt. | ➖ | 🎯 |

## S - `/cb create` Category workspace (from G11) - Built 🎯

> Moved from G11 (2026-07-25): G11 keeps category records and mutation rules; this workspace calls them rather than reimplementing them.

| | |
| --- | --- |
| **Check** | The wide Category workspace inside `/cb create` supports the new category system without replacing the current system prematurely. |
| **Pass rule** | Create-tab assignment, category creation, icon/style fields, preview, and migration-safety rows pass twice. |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| S1 | Open `/cb create` and switch to the Category workspace. | Wide workspace appears instead of the rejected cramped panel. | 🎯 | 🎯 |
| S2 | Assign a new block to an existing category from the workspace. | Block saves with the selected main category and appears in category browsing. | 🎯 | 🎯 |
| S3 | Create a new category from the workspace. | Category record is created without deleting or hiding existing categories. | 🎯 | 🎯 |
| S4 | Edit icon/colour/style fields if present. | Category customization persists and exports as category metadata. There is no description field — descriptions were removed `2026-07-28` (TG11 §D). | 🎯 | 🎯 |

## T - Export Dashboard Screen replacement (from G11) - Designed ⏳

> Moved from G11 (2026-07-25): G11 keeps the export ZIP contents and schema, G12 owns the ZIP artifact (TG12 §A) and G20 owns the download link (TG20 §L); this Screen only needs to reach the same export result through a GUI entry point.
>
> **Scope handover (`2026-07-30`):** TG12 is now command-routes-only. Every export surface that opens a screen lives here, including the `/cb export` dashboard entry point and the whole Export Dashboard conversion that TG12 used to carry. A **folder-import preview Screen** also belongs here if it is ever wanted — G12 ships the command with a clickable chat preview and an anvil rename box first, and a screen version is a later G27 upgrade, not a G12 target.

*(No test rows yet — design discussion pending.)*

## U - CategoryHubScreen full-parity manager (from G11) - Designed ⏳

> **Scope handover (`2026-07-28`):** TG11 is now chat-and-typing only. Every category check that needs a screen open lands here — U9-U12 arrived from TG11's clickable-chat rows, and U9-U11 are already **built**, not merely designed, so they can be tested in the same round as TG11 §C and §D.

| | |
| --- | --- |
| **Check** | The Category Hub can do everything the category commands can, without dropping back to chat. |
| **Pass rule** | Every row passes twice (singleplayer and server). |
| **Pass mark** | ✅ `YYYY-MM-DD` |

| # | Action | Expected result | SP | MP |
| --- | --- | --- | --- | --- |
| U1 | Create, rename, colour, and set an icon on a category from the Hub. | All four land without touching chat. (Describe was dropped — descriptions no longer exist, TG11 §D.) | ⏳ | ⏳ |
| U2 | Delete a category from the Hub that has blocks living nowhere else. | The same two-choice prompt as TG11 §C4/§C5, as a dialog rather than chat buttons. | ⏳ | ⏳ |
| U3 | Combine two categories from the Hub. | Blocks end up in the target and the source category is deleted — same result as TG11 §C7. | ⏳ | ⏳ |
| U4 | Drag blocks between categories. | Memberships update; nothing is silently removed from anywhere else. | ⏳ | ⏳ |
| U5 | Multi-select several blocks, then move / remove / give them. | The action applies to the whole selection in one go. | ⏳ | ⏳ |
| U6 | Type in the search box. | The block grid filters as you type, independently of the sort mode. | ⏳ | ⏳ |
| U7 | Give, export, and share a category from the Hub. | Same results as the command paths. | ⏳ | ⏳ |
| U8 | Look for `Uncategorized` in the Hub. | Present, styled dim/italic as a system floor rather than an owner-made category. | ⏳ | ⏳ |
| U9 | Click a category name in a `[CB]` chat line. | The Hub opens focused on that category. *(Moved from TG11 on `2026-07-28` — TG11 checks the chip fires; whether the Hub itself is right is judged here.)* | 🎯 | 🎯 |
| U10 | Click a category name inside `/cb category info` or `/cb category list` output. | Same jump as U9 from a listing line. *(Moved from TG11 on `2026-07-28`; the colour/hover halves stayed in TG11 §C8.)* | 🎯 | 🎯 |
| U11 | Open the Hub with `Uncategorized` holding 0 blocks. | It is still listed, greyed as the system floor. *(Moved from TG11 on `2026-07-28`; the listing/tab-complete halves stayed in TG11 §C9.)* | 🎯 | 🎯 |
| U12 | Look at the detail pane of a selected category. | No Description label, text box, Save button or "saved: …" read-back — descriptions were removed root-and-branch (TG11 §D). BLOCKS moves up into the freed space with no gap left behind. | 🎯 | 🎯 |
| U13 | Rename a category with the Rename button in the chest category editor. | Works, including names with a space. *(Moved from TG11 §D4 on `2026-07-30` — the command-side rename stayed in TG11.)* | 🎯 | 🎯 |

---

# Archive

<details><summary>✅ <b>Confirmed</b></summary>

- §A Block Creation Studio baseline — ✅ `2026-06-21`

</details>

<details><summary>💔 <b>Regression</b></summary>

*(none)*

</details>

<details><summary>📜 <b>Planned</b></summary>

- §N Backup and Trash Screen migration — 📜 `2026-07-23`: G09 backend complete; Screen scope/layout needs owner discussion before build (multi-select delete, backup metadata surface, Trash Screen, chest-menu retirement).
- §O Advanced Recolor Hub screen — 📜 `2026-07-24`: Moved from G10 §G; spec locked, nothing built. Command/route naming still unresolved against G07's live `/cb bulkrecolor` hue-shift op.
- §P Color-family builder Screen — 📜 `2026-07-24`: Moved from G10 §F; command rail stays in G10, only the GUI moved. Layout/trigger/overwrite-guard UI need Discussion before build.

</details>

<details><summary>💤 <b>Parked</b></summary>

- §K Premium future screen ideas — 💤 `2026-07-12`: Physics toasts, 3D Tome wiki, admin editor, cinematic welcome, and rethought error-code experiences are later phases.
- Custom icon set — 💤 `2026-07-04`: Separate future art project.
- Guess Mode screen specifics — 💤 `2026-07-04`: G30 owns game logic and tab content.

</details>

<details><summary>👎 <b>Scrapped</b></summary>

*(none)*

</details>

---

<details><summary>🧨 <b>Cleanup</b></summary>

- [ ] Delete temporary Studio test blocks.
- [ ] Remove temporary HUD layouts created only for tests.
- [ ] Keep G13 backend failures out of G27 unless the Screen routed them incorrectly.
- [ ] Keep image/animation data failures linked to G10/G14 when Editing tab exposes them.
- [ ] Keep category data/export failures linked to G11 unless §R/§S/§T routed them incorrectly.
- [ ] Delete old routes only after owner confirms the replacement Screen in game.
- [ ] Delete `g11a`, `g11b`, `g11c`, and temporary categories after §R/§S testing.

</details>
