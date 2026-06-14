# Changelog

All notable changes to CustomBlocks are documented here. This project tracks
progress by **phase milestones** (see CustomBlocks_Engineering_Bible.md §8).

## [Unreleased]

### Fixed
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
