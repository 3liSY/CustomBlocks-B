# Changelog

## 2026-06-20 (round 2) — Group 14: GIF speed, the WebP muffle, a calmer Animation tab (built — `customblocks-1.0.0.jar`)

- **GIFs play at the right speed now.** The first build sped clips up when it trimmed frames. Fixed — a clip keeps
  its real, natural speed (and "Normal" on the Speed control = exactly the clip's own speed).
- **The muffled/speckled look on animated blocks is gone.** It came from your texture size being **512**, which
  overflowed Minecraft's block atlas. Animated blocks now stay at a safe **256px** (and get **32 frames** instead
  of 16). *Re-create any animated block to pick up the fix.*
- **The Animation tab is much simpler.** No more fps/ticks numbers or wall of text. Just **Speed** (Slower /
  Normal / Faster), **Loop** (Forward / Bounce / Reverse), **Smooth motion** (On/Off), **Trim**, and a moving
  playback bar.
- **`/cb anim` on a block that isn't animated** now tells you so (and still opens the studio so you can add a GIF).
- ⚠️ Build green = compiles + gates pass; **in-game test pending.** Checklist: `docs/Finale Fix/Reports/GROUP_14_TESTING_GUIDE.md` §0.

## 2026-06-20 — Group 14: 3 bug fixes — crisp GIFs, the right `/cb anim` list, a tidy Animation tab (built — `customblocks-1.0.0.jar`)

- **GIFs look crisp now, not blocky.** Long clips used to get crushed to a tiny 32px-per-frame mush. They now
  keep **full resolution** and simply play **fewer frames** when a clip is very long (a short GIF keeps them all).
  *(This is the interim fix — even sharper close-up rendering is still coming as the hybrid renderer.)*
- **`/cb anim` (no id) now opens an "Animated Blocks" list** — only your animated blocks, and clicking one drops
  you straight onto its **Animation tab**. Before it opened the full block list with the wrong click behaviour.
- **The Animation tab is redesigned.** Controls are grouped under clear headers — **Speed · Loop · Smoothing ·
  Trim** — with spacing and a **playback bar** showing the current frame, instead of the old crammed panel.
- ⚠️ Build green = compiles + gates pass; **in-game test pending.** Checklist: `docs/Finale Fix/Reports/GROUP_14_TESTING_GUIDE.md` §0.

## 2026-06-19 — Group 14 Phase 2: Animation tab + live preview + editing animated blocks (built — `customblocks-1.0.0.jar`)

- **New Animation tab in `/cb create`.** Load a GIF/WebP and a dedicated **Animation** tab lights up — with a
  **live-playing preview** of your clip on the spinning block.
- **Tune it visually:** Speed (5/10/15/20/30 fps, or "Original"), Loop / Bounce / Reverse, Smoothing on/off,
  and Trim (cut the start/end frames). The preview updates as you click.
- **`/cb anim <id>` now opens the studio as a FULL editor** of that block — the old text command card is gone.
  **`/cb anim`** (no id) opens the block list to pick one.
- **Edit literally everything from one screen and hit "Save changes":**
  - **Rename the block's id** — and any copies you've already placed in the world keep working (they don't
    vanish or break).
  - **Swap the picture/GIF** — paste a new image link → Load texture → Save. A still image becomes a static
    block; a GIF makes it animate (and a GIF→still swap turns animation back off).
  - **Recolour / reshape / glow / sound / collision / category / name** — all in the same save.
  - **Retune the animation** — speed/loop/trim; your original frame timing is preserved (it never gets wiped).
- ⚠️ Build green = compiles + gates pass; **in-game test pending.** Checklist: `docs/Finale Fix/Reports/GROUP_14_TESTING_GUIDE.md` §1.

## 2026-06-19 — Group 13 Round 3: all 6 join-letter fixes (built — `customblocks-1.0.0.jar`)

- **`/cb arabic join <letter>` now gives 1 block**, not 16. (Add a number for more, e.g. `join ba 8`.)
- **Middle-click (pick block) on a placed join letter** now hands back that exact letter — correct name +
  art — instead of a blank `arabic_letter` item. The picked block re-joins when you place it.
- **"Place letter blocks"** (from `/cb arabic word` → type a word) now gives the **joinable** letter blocks
  that auto-connect, not the old separate static letters. Button text updated to match.
- **No more stray edge lines** on placed letters — the glyph faces sat a hair outside the block, drawing
  thin lines along edges and at the seams between connected letters. Now flush inside; clean.
- **Removed the OmniTool's Arabic Direction mode.** Rows already auto-join the moment you place them in a
  line, so the mode was redundant — the OmniTool menu no longer shows an Arabic button, and the tool does
  nothing to letter blocks (break them with any other item).
- **Readable from behind.** A placed word now reads the **same word correctly from the back** too — walk
  around it and it's right both sides, not the mirrored garble it showed before. Front/top/bottom/sides
  unchanged; only the back face changed. *(First build still mirrored the back glyphs; fixed by dropping a
  double-flip — the back is turned 180°, not mirrored.)*
- ✅ **All 6 confirmed in-game 2026-06-19** — auto-join feature complete. Checklist: `docs/Finale Fix/Reports/GROUP_13_TESTING_GUIDE.md` §R3.

## 2026-06-19 — Group 14: own-texture preview (the real crisp fix); 512px re-enabled

- **The real crisp fix, as a preview you can place.** Capping at 256px only traded muffle for
  pixelation (256 = blocky close up, 512 = atlas-muffled). The fix: draw blocks from their **own
  texture** like Minecraft maps — full resolution, mipmaps off, crisp at any distance, **no client
  settings**.
- **Try it:** place the `screen_test` block (CustomBlocks creative tab, or
  `/give @s customblocks:screen_test`) and right-click to cycle a gallery — sharp text, smooth gradient,
  resolution chart, an animated card, and **your own blocks at full resolution**. Instant, no reload.
- **Texture size can be 512px again** (`/cb config texturesize 512`, the picker, the GUI). ⚠️ 512 only
  looks sharp via the new own-texture renderer; **normal blocks still use the atlas and soften at 512**
  until that renderer is rolled out — keep 256 for now (the command warns you).
- Isolated preview — **does not touch your 1028 blocks.**
- Files: ScreenTestImages (new), ScreenTestBlockEntityRenderer, ScreenTestBlockEntity, CustomBlocksConfig,
  ConfigCommands, TextureSizeMenu. Docs: GROUP_14 spec + testing guide + progress log.

## 2026-06-19 — Group 14: muffling fix (256px atlas cap), Style toggle removed

- **Blocks no longer look soft/"muffled."** Texture size is capped at **256px** (a power of two).
  Above that, the block atlas overflowed Minecraft's size limit and mipmaps switched off for **every**
  block — the cause of the soft look. The size picker now tops out at 256 (16 / 32 / 64 / 128 / 256).
- **Long animated GIFs auto-fit the atlas.** A clip long enough to overflow is rendered at a slightly
  smaller per-frame size (you get a one-line note); short clips are unaffected. Animations can never
  blow the atlas now.
- **Removed the `/cb anim` "Style" (Photos & Video / Pixel-art & Text) button** — it didn't fix the
  muffling and was a misdiagnosis. The card's **Smooth On/Off** control is back.
- **Existing 512px blocks** made before this build stay soft until re-created (animated) or
  `/cb retexture`d (static) at 256.
- Rationale: `docs/adr/ADR-007`. Files: CustomBlocksConfig, ConfigCommands, TextureSizeMenu, AnimData,
  SlotDataStore, AnimCommands, AnimationDecoder.

## 2026-06-19 — Group 13 Arabic auto-join: colour + form + facing + searchable tab

- Joinable letter block now carries a **colour** and an optional **fixed form**; renderer draws
  isolated = hand-art PNG, connected = matching-colour font, cache keyed by letter+form+colour.
- **Facing auto-inherit** on placement so rows actually join (the in-game bug).
- New **"Arabic Letters (Join)"** creative tab: every letter, 4 forms, 4 colours — all NBT variants
  of the single arabic_letter block, so **zero new registrations, zero slots**. Fixed-form variants
  are searchable decoration that never reshape and never drive neighbours.
- Files: ArabicLetterBlock, ArabicLetterBlockEntity, ArabicJoinFlow,
  ArabicLetterBlockEntityRenderer, CustomBlocksMod. Source-only (build on a JDK machine).


All notable changes to CustomBlocks are documented here. This project tracks
progress by **phase milestones** (see CustomBlocks_Engineering_Bible.md §8).

## [Unreleased]

### Changed — Group 27.6 Block Creation Studio · slice 3 "fixes + UI upgrade + Category manager" (📦 deployed 2026-06-18, awaiting in-game test)

> Five upgrades to the studio from your slice-2 feedback. **Build-green is not "done" — this needs your in-game test.** Test steps: testing guide §6 (🎯 the 5 new fixes).

- **Enter no longer publishes** — pressing **Enter** only confirms the field you're typing in; the block is made **only** by the **Create & Publish** button.
- **Background colour instead of "base colour"** — picking a colour (swatch or `#hex`) no longer wipes your image. The colour now fills **behind** the image's transparent areas; a **✖** swatch clears it; a colour with no image still makes a solid block.
- **"Use hex" no longer overlaps its box** — the hex field and button now sit side by side.
- **"Organize" tab is now "Category"** — a full mini category manager: every category shows as a chip (★ = default, colour-tagged ones are tinted); click a chip to put this block in it; **Add** a new one; or, on an existing one, **Rename / Colour / Set Default / Delete** (delete asks to confirm). These edit real server categories (so they affect other blocks too).
- **UI polish** — a green ✔ appears on each left tab once it's filled in; panels sit on subtle cards; hovering Glow / Hardness / Sound / Collision shows a one-line explanation.
- **Not yet:** category **icon** picking, and one block belonging to several categories — later updates.

### Added — Group 27.6 Block Creation Studio · slice 2 "sidebar + sections" (📦 deployed 2026-06-18, awaiting in-game test)

> The studio grew a real left-hand section sidebar. **Build-green is not "done" — this needs your in-game test.**
> Test steps: testing guide §6 (🆕 Slice 2 block).

- **`/cb create` is now a studio with sections** — a left sidebar with **Identity · Texture · Shape · Attributes · Organize** (FX / Behavior / Lore are shown but greyed-out "coming soon"). The open section has a gold marker + breadcrumb.
- **Identity** — Block ID + Display Name + **Auto-ID from name** button.
- **Texture** — paste a URL and Load, **or** pick a base colour (8 swatches or a `#RRGGBB` hex) to make a solid-colour block. The cube preview updates live.
- **Shape** — pick from the 10 shapes (full / slab / stairs / pane / cross / …); the preview cube changes shape live.
- **Attributes** — set glow (0–15), hardness (Soft/Wood/Stone/Iron/Hard), step sound (17 types), and Solid/Passable.
- **Organize** — set a category.
- **Create & Publish applies all of it at once** — the new block is made with your chosen texture/colour, shape, glow, hardness, sound, collision and category in one go, through the mod's existing engines.
- **Not yet:** FX (emissive/pulse/glint), Behavior (gravity/bounce/slippery), and Lore/attribution — those need new block data and come in a later update; the per-pixel colour tools, overlays, and session memory are also later.

### Added — Group 27.6 Block Creation Studio · slice 1 "vertical spine" (📦 deployed 2026-06-18, awaiting in-game test)

> The first working slice of the new one-screen block maker. **Build-green is not "done" — this needs your in-game test.**
> Full spec: `Finale Fix/GROUP_27_SCREENS.md §G27.6` / `§G27.6.X`; test steps: testing guide §6 (🆕 Slice 1 block).

- **`/cb create` with no arguments now opens the Block Creation Studio** — a gold-framed screen with a live spinning 3D preview cube. Type a **Block ID** + **Display Name**, optionally paste a **Texture URL** and click **Load** to see it on the cube, then **Create & Publish** to make a real block. The old `/cb create <id> [name] [url]` typed command is **unchanged**.
- The studio creates the block through the exact same engine the typed command uses, so a bad/duplicate id or broken link is caught and explained in chat (no empty blocks left behind).
- Standard Group 27 frame: world visible behind, `[?]` help, background-dim slider, `Enter` = create, `Esc` = cancel (asks before discarding).

### Added — Group 27.7 screen corrections (📦 deployed 2026-06-18, awaiting in-game test)

> Built slice by slice and deployed as one jar. **Build-green is not "done" — these need your in-game test.**
> Full spec: `Finale Fix/GROUP_27_SCREENS.md §G27.7`; test steps: `Finale Fix/Reports/GROUP_27_TESTING_GUIDE.md`.

- **Smoother 3D screens** — the rotating preview was rebuilt to draw fast (no more lag) and the block no longer disappears while spinning. *(confirmed in-game 2026-06-17)*
- **`[?]` help shows on top**, redesigned with a red **[X]** close, organised shortcut groups, and it no longer vanishes on a stray click.
- **Adjustable background dim** — a slider (top-left of `[?]`) sets how dark the world behind a screen is; sensible dark default; remembered next time. (Eyedrop stays faint.)
- **Tidier Arabic colours** — Background/Letter swatches moved into a clean side panel you can **drag, snap, collapse, and customize**: add (type a hex or use the dropper), remove, drag-reorder, recent colours, named saved palettes, a **contrast warning**, matching-colour + tint suggestions, **number-key 1–9** shortcuts, a **★ favourites** row, hover-to-see-hex, right-click to edit, and a built-in **dropper + magnifier loupe**.
- **Movable, smaller button bar** — the bottom buttons are now a compact bar you can dock to the bottom/left/right edge or hide to a thin tab; takes less space; remembered.
- **`/cb livecolor` with no id** opens a block picker; the colour sliders got a colourful gradient redesign (rainbow hue track, bigger knobs, value chips) plus **temperature, contrast, one-tap filters (grayscale/sepia/invert/posterize), and a brightness curve** (lift shadows / lower highlights).
- **`/cb eyedrop`** now explains itself (first-time popup + a permanent hint) and has a **hide-UI toggle** (press H) so you can sample pixels the top bar covered.
- **`/cb shapeeditor` fixed** — it was an unregistered/duplicated command; now works with no id (block picker → 3D shape screen) or with an id (opens directly).
- **HUD: centered long names stay centered** instead of drifting right (§E5).

### Planned — Group 27.7 remaining (⏳ NOT built)

- **HUD editor look + feel** — a brighter restyle, an intro/guidance + empty-state, advanced controls folded behind one "Advanced" toggle, and clearer brick drag/placement preview. *(its own session — the editor file must be split first)*

### Changed
- **Arabic live preview screen — sharper render + camera QoL (Group 13 §1, confirmed in-game 2026-06-16 — partial).** The 3D preview cube
  now downsamples its texture at a higher grid (56, was 28) with an alpha-weighted area average, so
  the letters read crisp instead of blocky. New live controls: **scroll** = spin speed,
  **shift+scroll** = zoom, **click** (without dragging) = pause/resume the auto-spin, **R** = reset
  view, **Enter** = Create. A small corner readout shows spin %/zoom %. Client-only; no server or
  payload change.

### Fixed
- **Block display names show clean — no more underscores (Group 26 FIX A, confirmed in-game 2026-06-15).**
  Names now render with spaces and Title Case (`Test_black` → **Test Black**); the underscore is no
  longer kept. Applies to new blocks, uploads, and renames, and a one-time boot migration cleans the
  224 bundled Arabic blocks (`Alef_Black` → **Alef Black**) already saved with underscore names.
- **`/cb give <id>` is now case-insensitive (Group 26 FIX B, confirmed in-game 2026-06-15).** If a
  block's id is `Te`, then `/cb give te`, `/cb give Te`, and `/cb give TE` all give the same block.
  Exact matches are unchanged (tried first); the tolerance only kicks in when an exact match misses.
- **Custom-block textures now load on the modded client (host) — confirmed in-game 2026-06-15.**
  A modded client (the world host) was ignoring the server's resource-pack download, so new/edited
  block textures never appeared and only stale textures from an old pack showed. The client now
  builds the pack **locally** from the live block data and silently reloads — textures apply instantly,
  with no "download the resource pack?" dialog. Friends connecting to a real server (vanilla, or modded
  without local data) are a separate next step. Tracked in `Finale Fix/GROUP_05_RESOURCE_PACK.md`.
- **Back button no longer closes command-opened menus** — clicking Back in a GUI you opened
  straight from a command (like `/cb bulkgui`) now takes you to the Main Menu; only ✖ Close
  closes the screen.
- **Background removal: tiny edge pixels no longer block it.** The mask is now despeckled — a 1-px
  morphological close bridges hairline gaps that used to wall off the flood-fill, and tiny stray
  specks are dropped — so backgrounds clear cleanly. The colour tolerance is unchanged.
- **Background fill keeps dark-outlined logos visible** — the removed background is filled white
  (instead of black) when the subject has a dark outline, so e.g. a black-bordered emblem no longer
  disappears into a black fill.
- **A broken image link no longer creates an empty block** — `/cb create <id> <name> <url>` now
  fetches the image first and only creates the block if it succeeds; a bad/non-image link creates
  nothing.
- **`/cb dupe` now copies the texture and settings** — duplicates were coming out blank; a copy is
  now visually identical to the original.

### Changed
- **Coloring redesign (🟢 built, gates pass; ⏳ not in-game verified).**
  - **`/cb colors` → `/cb coloring`** (old name still works); rebuilt as one polished hub for every colour
    tool, plus a **Coloring** tile on the `/cb` dashboard.
  - **`/cb bgstudio` with no id opens a block picker** first; `/cb bgstudio <id>` still opens the studio
    directly. The same picker backs the hub's Background Studio / Colour Variants / Live Recolour.
  - **`/cb tolerance <value> [id]`** — value first. No id sets the **global** default; an id sets just that
    block's strength (saved per-block; the global default is left alone) and re-applies it.
  - **Palette is now a shared colour source** — your saved swatches show up as one-click quick-picks in the
    Background Studio fill picker, the Gradient Builder endpoints, and Custom Colour.
  - **Background Studio polish** — plainer mode names (Keep / Remove background / Remove background + gaps /
    ★ Smart auto), the header shows whether the strength is per-block or global, Apply remembers the block's
    strength, and an **↩ Undo last change** button gives an apply → look → revert loop.
  - **`/cb gradient` with no args now opens the Gradient Builder GUI** (it used to be an error).

### Added
- **Group 26 Part C — browse your textures by name (🟢 built, gates pass; ⏳ not in-game verified).**
  A new optional `/cb config mirrornames on` keeps a human-readable copy of your texture folder at
  `config\customblocks\textures_names\`, where each file is named by the block (`Neptune Red.png`)
  instead of `slot_0.png`. It's **write-only** — the mod writes it but never reads it, so it can never
  affect a block, the resource pack, or anything already placed. `on` backfills every block, `off` stops,
  `rebuild` regenerates from scratch, and a manifest keeps renames/deletes from leaving orphan files.
  Off by default.
- **Group 12 — Export Dashboard: Blueprint item + downloads (🟢 built, gates pass; ⏳ not in-game verified).**
  - **Blueprint item** — `/cb exportblock <id>` gives a tradeable item carrying a block's full recipe;
    hand it to a friend and they run `/cb importblock` (held in hand) to recreate the block. Offline,
    no server needed.
  - **Export All → one ZIP** — `/cb export zip` (and an "Export All (ZIP)" dashboard tile) bundle every
    block (JSON + texture) into `cloud_exports/all-<stamp>.zip` with a clickable `[download]` link.
  - **Clickable downloads everywhere** — single-block JSON/PNG and category ZIP now save to
    `cloud_exports/` and give a working `[download]` link (the PNG link was previously broken).
  - **Deferred (marked partial):** vault share-codes, the marketplace, and litematic/.schem/vanilla
    resource-pack formats — they need the cloud vault deployed; revisiting at the end.
- **Group 10 — colour & image tools, rest of group (🟢 built, gates pass; ⏳ not in-game verified).**
  - **Background Studio:** `/cb bgstudio <id>` — a per-block GUI to re-run background removal on demand
    (None / Background only / Background + enclosed / **Smart**). The first three are the same engine
    `/cb config` already uses; **Smart** is a new *offline* mode (pure local code, no AI download) that keeps
    only the main subject. `/cb tolerance <id> <0-100>` sets the strength and applies it now. Both undoable.
  - **Colour Variants panel:** from `/cb editor <id>` — lighter / darker / vivid / muted / complementary /
    split-complement swatches; click one to spin off a new colour-shifted block (undoable).
  - **Palette:** `/cb palette add/clear/save/load/list/delete` and `/cb colors` (Colour Tools hub) — keep a
    working set of colours and save named palettes, per player.
  - **Export download link:** `/cb exportpng <id>` now adds a clickable **[download]** link that opens the PNG
    in your browser (served over the mod's localhost HTTP server).
  - **Live recolour slider** *(client)* — `/cb livecolor <id>` opens a screen with Hue/Saturation/Lightness
    bars and a live preview; Apply commits to the block (server-side, undoable).
  - **Screen eyedrop** *(client)* — `/cb eyedrop` lets you click any pixel of your Minecraft screen to grab its
    colour straight into your palette.
- **Group 09 — backups, Slice 1 (🟢 built, compiles; ⏳ not in-game verified).** `/cb backup save [name]`
  snapshots your current blocks (slot data + textures + originals + config) into
  `config/customblocks/backups/<name>/`; `/cb backup list` shows them newest-first with timestamp + block
  count. Saves are atomic (built in a temp dir, then renamed) and **only read** live data — they can't
  change or break anything. Auto-names if you omit a name; refuses duplicate or unsafe names.
- **Group 09 — backups, Slice 2: restore (🟢 built, compiles; ⏳ not in-game verified).** `/cb backup
  restore <name>` replaces your current blocks with a saved backup — and **auto-saves your current state
  first** (as `pre-restore-…`), so a restore is itself undoable. It asks for `/cb confirm`. Also:
  `/cb backup delete <name>` (removes a backup; never touches live data), `/cb backup panic` (emergency —
  restore the newest immediately, no confirm), and `/cb recover` (restore the newest, with confirm).
  (Auto-backup, trash browser and migration are coming in later slices.)
- **Group 08 — block shapes & per-face textures (🟢 built, compiles; ⏳ not in-game verified).**
  - **Shapes:** `/cb setshape <id> <shape>`, `/cb clearshape <id>`, `/cb shapelist` — slabs (top/bottom),
    carpet, thin, pane, wall, pillar, stairs, cross, full. The shape drives both the live collision/outline
    and the generated pack model from one source, is undoable, and is refused on locked blocks.
  - **Shape editor GUI:** `/cb shapeeditor <id>` — a chest menu with a button per shape (current one
    glinted) + a "reset to full" tile; every click runs the tested `setshape`/`clearshape` command.
  - **Face editor GUI:** `/cb facechangegui <id>` — a chest menu with one tile per face; left-click pastes
    a URL onto that face, right-click clears it, and a tile clears all faces. Delegates to the existing
    paint/clear commands.
  - **Spec-name aliases:** `/cb setface <id> <face> <url>` (= `paintface`) and `/cb clearallfaces <id>`
    (= `clearface <id> all`), so the Group 08 command names work as written.
  - **Shape preview:** `/cb shapepreview <shape>` — floats a stand-in block in front of you for 5 seconds
    so you can see a shape before applying it; it removes itself. (Op only.)
- **`/cb bulkreid` — change many block IDs by a pattern (✅ command in-game verified 2026-06-13; GUI + polish pending).** The id
  counterpart of `/cb bulkrename`: `/cb bulkreid <filter> prefix <text> | suffix <text> | replace <old>
  <new>` transforms each matched block's custom id while keeping its slot (textures + placed blocks
  untouched, no pack rebuild — like single `/cb reid`). Skips locked / no-change / invalid / colliding
  ids (reported); one `/cb undo` re-ids the whole batch back; big/"all" batches confirm in chat. A GUI
  for it lands once the command passes in-game.
- **Every bulk op now uses the two-step GUI (✅ all 8 in-game verified 2026-06-13).** One shared
  pattern for **all** bulk ops — edit a setting, delete, rename, move to category, duplicate, export,
  lock, favorite. (The old single-screen Bulk Dashboard was removed once the new flow passed.)
  - **Step 1 — choose blocks (shared).** Shows "None selected currently" until you choose. *Option A*:
    a Filter tile that cycles All blocks / Category / Favorited / Locked / Name contains / ID starts
    (the text kinds prompt for a value). *Option B*: "Select specific blocks" → opens the block list to
    tick blocks, then a §agreen-concrete§r tile confirms the picks. §aNext →§r lights once something
    resolves to ≥1 block.
  - **Step 2 — options (per op).** 🔍 Review (the exact blocks) + the op's own controls (setting+value
    / rename mode+text / category / export format / lock·favorite direction; delete & duplicate have
    none) + ✔ Apply. Apply runs the SAME tested `/cb bulk…` command underneath (confirm-guard +
    one-undo batch unchanged).
  - **Reachable from** the Bulk Hub's op tiles, and from each `/cb bulk<op>` with no args.
- **Bulk Hub** — `/cb bulkgui` (and `/cb bulkhub`) open a hub with one tile per bulk operation;
  click one to start that op's two-step builder.
- **Bulk duplicate (built, not yet in-game tested)** — `/cb bulkduplicate <filter>` copies every
  matched block (texture + settings) into new `<id>_copy` blocks; one `/cb undo` removes them all.
- **Bulk export (built, not yet in-game tested)** — `/cb bulkexport <filter> [json|txt]` writes just
  the matched blocks to a file, the filtered version of `/cb export json`.
- **More export formats incl. PNG (✅ in-game verified 2026-06-13 — polish pending)** — exports now
  cover `png`, `csv`, `md`, `html`, `yaml` on top of `json`/`txt`. `/cb export png` saves every
  block's texture as a `.png` image (into `exports/textures-<stamp>/`); `/cb export <id> png` saves
  one block's image; `/cb export csv|md|html|yaml` write the block list in those formats;
  `/cb bulkexport <filter> <format>` does the filtered version of any of them. New formats are
  clickable in `/cb list` and `/cb export`. (Formatting/path polish is a later pass.)
- **`/cb reid` — change a block's id (✅ command verified in-game 2026-06-13; GUI built, not yet tested).**
  - `/cb reid <id> <newId>` changes a block's custom id while **keeping its slot index**, so the
    baked texture and any placed blocks are unaffected (no pack rebuild). Guards: the id exists, the
    new id is non-blank, different, and free; locked blocks are refused.
  - **Migrates every id-keyed reference** — locks, favorites (per-player), notes, drafts; the
    category rides on the slot data. Undoable: `/cb undo` re-ids back (reid is its own inverse).
  - **GUI:** bare `/cb reid` opens a pick-a-block menu → anvil for the new id; `/cb reid <id>` goes
    straight to the anvil; and `/cb editor <id>` gains a **Change ID** button. All three delegate to
    the same tested command.
- **`/cb listgui` upgrades — search, multi-select, bulk-on-selection (built, not yet in-game tested).**
  - **Search** — a Search tile filters the block list by id / name / category (the query survives
    paging; right-click clears it).
  - **Multi-select** — left-click ticks/unticks blocks (**right-click now opens the editor**);
    "N selected", "Select all shown" and a one-click clear live in the footer. Selection is per-player.
  - **Bulk actions on the selection** — hands the ticked ids to the existing Bulk Hub (as a comma
    id-list filter), so delete / lock / favourite / category / duplicate / export / property / rename
    all work on a hand-picked set. New `ListSelection` holds the set; no bulk logic is duplicated.

### Added
- **Group 07 — Bulk Dashboard 2.0 + tab-complete everywhere (✅ verified in-game 2026-06-12).**
  - **Tab-complete after every bulk subcommand** — `/cb bulkproperty`, `bulkdelete`,
    `bulkrename`, `bulklock/unlock`, `bulkfavorite/unfavorite` now suggest filters
    (`all`, live `category:` names, block ids, `locked:`/`favorite:` flags, even after a
    comma in an id list), then property names, then valid values, token by token.
    Also added: `/cb video extract` suggests your .mp4 files, `/cb arabic letter` the 28
    letter names, `/cb retextureall` the standard sizes.
  - **Bulk Dashboard 2.0** — colour-coded frame per operation (blue Edit / red Delete /
    yellow Rename), numbered steps, every choice tile shows its options with a ▸ marker,
    a spyglass **Matched** tile lists the actual blocks the filter hits, and the filter
    picker gained wool-coloured category tiles, a glint on the current pick, and typed
    **Name contains… / ID starts with…** filters via anvil — no chat needed.
  - **GUI sounds** — chime on clicks, XP blip on apply, deep bass on delete/disabled,
    page-turn when the dashboard opens.
  - **Dashboard polish round 2** — two-tone picture frame (darker corners), the chest title
    tints with the operation, » connectors between the steps, and a **Command twin** tile
    showing the exact chat command the screen will run.
  - **`docs/GUI_DESIGN_GUIDE.md`** — the written design language for every future GUI:
    colour meanings, frames, layout grid, icon vocabulary, sound matrix, and a checklist.
  - **Lock & Favorite in the dashboard** — the Operation cycle now also offers **Lock blocks**
    (green frame) and **Favorite blocks** (purple frame), each with a Lock⇄Unlock /
    Favorite⇄Unfavorite direction tile. No typing — pick op, filter, direction, Apply.
  - **Bulk category move** — `/cb bulkcategory <filter> <category>` (use `none` to clear), and a
    matching **Move to category** dashboard op (cyan frame); type the category in an anvil.
    One `/cb undo` reverts the whole batch.
  - **Command twin is clickable** — click the tile to drop its command straight into your chat
    box (edit it or press Enter to run).
- **Group 06 / M4 — paint single block faces from a URL (built, not yet in-game tested).**
  - **Rainbow Rectangle, right-click a face** → chat opens pre-filled with
    `/cb paintface <id> <face> ` — paste an image URL and only that face changes; the other
    five keep the base texture. `/cb clearface <id> <face|all>` resets painted faces.
    Painted faces are stored as files and survive restarts. The area selector moved to
    **sneak + right-click** (the Omni-Tool's Area mode is unchanged).

### Fixed
- **Red Triangle item art** — the bundled red art was lumpy/deformed (an old-project file);
  redrawn from the clean yellow art in the same style (fill + highlight + dark outline).
  Custom-colour tool icons now use the clean shape too, and re-tinted art keeps its
  highlight/outline contrast instead of flattening.
- **Resource-pack reloads no longer interrupt open menus** — a pack push aimed at a player
  who has ANY CustomBlocks menu or anvil prompt open is now held and delivered right after
  they close it (the old "RP building starts while the GUI opens" bug, properly this time —
  the first fix only covered the recolour-confirm prompt).
- **Group 06 — Config-GUI hex editor, rebuild timing fix, `/cb customcolor` Color Studio (built, not yet in-game tested).**
  - **`/cb config` → Variant Colours** — a new glowing entry opens a sub-GUI where each magic
    tool is its dye (red/yellow/green/black); clicking one edits that hex **in an anvil**
    (type `#RRGGBB`, take the output). Same validation, saving and repaint-confirm as the
    chat command.
  - **Fixed:** changing a hex no longer starts the resource-pack rebuild while the
    "recolour existing blocks?" prompt is opening — the rebuild now **waits** until that
    prompt is answered or closed (Yes → one rebuild after the repaint batch; No/close → one
    rebuild right then).
  - **`/cb customcolor` (rebuilt from scratch)** — bare, it opens the **Color Studio**: 29
    ready-made colours (Purple, Pink, Baby Blue…) shown as dyes; click one to receive that
    colour's **magic Square + Triangle pair**, or use **Custom Hex…** to type any `#RRGGBB`
    or colour name in an anvil. `/cb customcolor <hex|name>` gives the pair directly.
    The pair's colour lives in the items themselves (names + icons tinted to match), the
    Triangle creates `*_hex_rrggbb` variants and the Square swaps to them — and tools,
    variants and hexes all survive a restart.
- **Group 06 / M3 hex — the four variant colours are configurable (built, not yet in-game tested).**
  - **`/cb config hex <colour> <#RRGGBB>`** changes what red/yellow/green/black mean: new variants
    use the new colour, the matching Square + Triangle **item art re-tints** automatically, and item
    names show the live hex (e.g. `Red Square [#FF8800]`).
  - When blocks of that colour already exist, a **Yes / Info / No confirm** offers to repaint them —
    a direct colour swap that only touches pixels near the old colour, so designs are safe.
    `/cb recolorvariants <colour> <oldhex>` runs the same repaint by hand.
  - Bare `/cb config hex` shows all four; bad colours/codes are rejected with clear errors.
- **Group 06 / M3 — Squares swap placed blocks between colour variants (built, not yet in-game tested).**
  - **Right-click a placed custom block with a Square** → the block changes in place to that
    colour's existing variant (`vart` + Red Square → the placed block becomes `vart_red`). The
    variant's glow carries over. Squares never create variants — no variant of that colour yet →
    a hotbar error points you to the matching Triangle.
  - **Black Square fallback** — no `_black` variant → the block swaps back to the original base block.
  - The old placeholder "tagged at (x, y, z)" message and the "tagging" lore are gone; Square
    tooltips now explain the swap.
  - **All "marker"/"tagging" wording removed mod-wide** — `ShapeMarkerItem` renamed to
    `ShapeToolItem`, and the Magic Items GUI section label changed from "Marker Shapes — Tag
    your custom blocks by colour" to "Squares & Triangles — Triangles create colour variants,
    Squares swap them".
- **Group 06 / M2 — Triangles create colour variants (✅ in-game verified 2026-06-11).**
  - **Right-click a custom block with a Triangle** → creates a new block whose image background
    is recoloured to that triangle's colour (`mars` + Black Triangle → `mars_black`); the design
    stays, the source block is untouched, and light/hardness/sound carry over. The variant lands
    in your inventory. Using the same triangle again just hands you another one.
  - **Sneak + right-click** opens a Yes / Info / No confirm first.
  - **Configurable colours** — `triangleRedHex` / `triangleYellowHex` / `triangleGreenHex` /
    `triangleBlackHex` in `config.json` (defaults `#EE3333` / `#F0C814` / `#1E8C1E` / `#0A0A0A`).
- **Group 03 — HUD overlay, HUD editor & ESC-menu buttons (written, not yet built/tested).**
  - **HUD overlay** — pointing your crosshair at a CustomBlocks block shows its id and display name
    in a small overlay that hides when you look away.
  - **`/cb edithud` drag editor** — a Lunar-style overlay (`HudEditorScreen`) to drag the HUD to any
    position and adjust scale, text color (palette cycle), background opacity, and ID/Name
    visibility, with Save / Reset / Cancel. The world stays visible; closing without saving reverts.
  - **Persisted HUD settings** — position/scale/color/opacity/show-flags save to
    `config/customblocks/data/hud-config-server.json` (atomic write) and restore on launch.
  - **`/cb config hud`** — `toggle` / `on` / `off` switch the HUD (and a bare read prints status);
    resolves the `/cb config hud on|off` the in-game config screen already referenced.
  - **ESC-menu buttons** — two gray Command-Block-icon buttons below “Leave Game”:
    “CustomBlocks Menu” (opens the dashboard) and “HUD Editor”, added via `ScreenEvents.AFTER_INIT`
    + a `ScreenInvoker` mixin.

### Fixed
- **HUD name line was blank/garbled** — `HudSync` joined id and name with a space while the client
  parser split on a NUL byte; both now use `'\u0000'`.
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
