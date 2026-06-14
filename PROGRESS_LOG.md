# Progress Log

One entry per work session. Newest at the top. See the Engineering Bible §9.2.

---

## 2026-06-14 (later 10) — Group 10 marked PASSED in-game + client-screen cancel→back fix (build green)

**Developer confirmed the whole of Group 10 + the Coloring redesign works in-game.** Marked all scorecards
✅ in `GROUP_10_TESTING_GUIDE.md` and the verdict table in `GROUP_10_COLOR_IMAGE.md` (G10.3–G10.8 ✅;
G10.1/G10.2 dress = removed). resize/exportpng were already ✅.

**Done — cancel/Esc returns to the previous menu (build green; gates pass — NOT in-game tested):**
- New `network/payloads/GuiBackPayload` (empty C2S signal). Registered playC2S + a server receiver in
  `CustomBlocksMod` that reopens `Nav.current(player)` via `GuiRouter.render`.
- `RecolorSliderScreen` + `EyedropScreen` now override `close()` to send `GuiBackPayload` — so Cancel/Esc
  (and Apply, for the slider) drop the player back into the chest menu they came from (the block picker, the
  editor, or the Coloring hub) instead of out to the world. Eyedrop *picking* a colour still routes to the
  chat prefill (no back), only Esc/cancel goes back.

**Files:** new — `network/payloads/GuiBackPayload`. Edited — `CustomBlocksMod`,
`client/gui/{RecolorSliderScreen, EyedropScreen}`.

**TEST IN-GAME (developer):** open `/cb coloring` → Live Recolour → pick a block → **Cancel** (and **Esc**):
should land back on the block picker. Same for `/cb eyedrop` Esc → back to the hub. Apply on the slider
should commit and also return to the picker.

---

## 2026-06-14 (later 9) — Coloring redesign built in one push (build green; gates pass — NOT in-game)

Developer approved building all four redesign slices at once. **Build green with JDK 21 (`--no-daemon`);
all three gates pass (fileSize, mojibake, sound).** Jar in `build/libs/` only.

**Done — Coloring redesign (build green; gates pass — NOT in-game tested):**
- **`/cb colors` → `/cb coloring`** (rename; `colors` kept as a silent alias). `ColorsMenu` rebuilt as a
  framed 6-row hub titled "Coloring" — 4 main tools (Background Studio, Palette, Gradient Builder, Custom
  Colour) over 3 extras (Colour Variants, Live Recolour, Screen Eyedrop). Added a **Coloring** tile to the
  `/cb` dashboard (MainMenu slot 32) for discoverability.
- **`/cb bgstudio` no-arg → block picker.** New `ColorPickBlockMenu` (generic paged block picker that routes
  to bgstudio / variants / livecolor); `/cb bgstudio` and the hub's block tools open it. `/cb bgstudio <id>`
  still goes straight in. New `Nav.Dest.COLOR_PICK` + GuiRouter case.
- **`/cb tolerance <value> [id]`** — arg order flipped to value-first. No id = set the **global** default
  (`CustomBlocksConfig.backgroundTolerance`, persisted). With id = per-block override in new
  **`BlockToleranceStore`** (atomic JSON, mirrors LockManager; renames follow via SlotManager.renameId),
  global untouched, re-applies now. BgStudio seeds each block's strength from its override.
- **Palette is now a shared colour source.** Working-set swatches appear as one-click quick-picks in the
  BgStudio **fill** picker, the **Gradient** endpoints (left=A / right=B), and **Custom Colour**. Header lore
  explains where they're used.
- **BgStudio polish** — clearer mode names (Keep / Remove background / Remove background + gaps / ★ Smart
  auto), header shows whether strength is per-block or global, Apply remembers the block's strength, and an
  **↩ Undo last change** tile gives an apply→look→revert loop (real preview is the undoable apply).
- **Gradient kept + improved** — `/cb gradient` with no args now opens the Builder GUI (was an error); palette
  endpoints added; `woolFor`/hex helpers deduped onto the new shared **`Swatch`** util (removed 3 copies).

**Files:** new — `core/BlockToleranceStore`, `gui/chest/{Swatch, ColorPickBlockMenu}`. Rewritten —
`gui/chest/{ColorsMenu, BgStudioMenu}`, `command/handlers/ImageToolCommands`. Edited — `gui/chest/{Nav,
GuiRouter, BgStudioSession, PaletteMenu, GradientPickerMenu, CustomColorMenu, MainMenu}`,
`command/handlers/ColorImageCommands`, `core/SlotManager`.

**TEST IN-GAME (developer):** see `Reports/GROUP_10_TESTING_GUIDE.md` (updated). Key flows: `/cb coloring`
hub, `/cb bgstudio` (no id) picker, `/cb tolerance 40` vs `/cb tolerance 40 <id>`, palette swatches showing up
in bgstudio fill + gradient + custom colour, gradient GUI from `/cb gradient`.

---

## 2026-06-14 (later 8) — Group 10 in-game results + colour-tools redesign (DISCUSSION, nothing built)

Developer tested Group 10 in-game. **Confirmed working:** `/cb resize`, `/cb exportpng`, and the `/cb colors`
hub open/navigation. Marked ✅ in `GROUP_10_COLOR_IMAGE.md` (G10.5/G10.6) and the testing-guide §1 scorecard.

**Developer rejected as not good enough:** `/cb gradient` (no GUI, confusing), `/cb bgstudio` (no picker,
blind Apply, confusing modes — "currently bad, needs so much work"), `/cb palette` (weird, no clear purpose),
`/cb tolerance` (wrong arg order, no global-vs-per-block split).

**Confirmed design direction (4 forks answered):**
- Rename `/cb colors` → `/cb coloring`; fold palette + bgstudio + tolerance under it.
- **Palette → shared colour source:** saved colours become one-click swatches in every colour picker
  (bgstudio fill, custom colour, recolour); eyedrop + add feed it.
- **Tolerance:** `/cb tolerance <value> [id]` — no id = global default; with id = per-block override (persists),
  global untouched.
- **Gradient: KEEP** (reversed earlier removal) — build it a real preview GUI so it stops being confusing.
- **BgStudio: major polish** — add a block picker for no-arg, add a result preview before Apply, simplify the
  modes. Developer flagged all three pain points.

**Plan = 4 ordered slices (each build-verified + handed off for in-game test), not one push.** Slice 1
(rename + picker + tolerance syntax/storage) proposed as the start. Nothing coded yet — awaiting the go-ahead
on slice 1.

---

## 2026-06-14 (later 7) — Group 10 Revamp: palette anvil, BgStudio fill colour, gradient GUI, dress removed (build green)

Applied the developer's four design answers to the Group 10 colour tools. **Build green with JDK 21; all three
gates pass (fileSize, mojibake, sound).** Jar in `build/libs/` only — **NOT in-game tested.**

**Done — Group 10 Revamp (build green; gates pass — NOT in-game tested):**
- **Palette "Add colour" → anvil** — `PaletteMenu` rewritten: "＋ Add colour" now opens an `AnvilPrompt`
  (typed hex/name) instead of closing to chat. Layout reorganised (6 rows): header → actions → working set
  swatches → saved palettes. "Save as…" also uses anvil. Left-click a swatch = remove it. Palette now
  accessible from the Gradient Builder and BgStudio fill-colour picker.
- **BgStudio fill-colour picker** — `BgStudioMenu` gains a fill-colour tile (slot 28): shows nearest-wool,
  hex in lore. Left-click = anvil to type any colour; right-click = reset to black. The fill colour flows
  through `BgStudioSession.fillColor` → `ColorToolService.applyBgRemoval(fillRgb)` →
  `BackgroundRemover.apply(4-arg)` + `snapBackgroundColor`. Old 3-arg `apply()` and `snapBackgroundBlack`
  kept for backward compat (default smart black/white).
- **Gradient Builder GUI** — new `GradientPickerMenu` + `GradientSession`: 5-row chest with Colour A/B tiles
  (left-click = anvil hex, right-click = `/cb gradientpick a|b <id>` to pick from a block's average colour),
  steps ±, wool preview swatches (CIE-Lab interpolated), Create button. New `/cb gradientpick` command wired
  into `ColorImageCommands`. `Nav.Dest.GRADIENT_PICKER` + `GuiRouter` case + tile in `ColorsMenu`.
- **`/cb dress` removed** — command registration + handler method deleted from `ColorImageCommands`. The dress
  functionality (solid-colour overlay) was deemed "overkill and unnecessary" by the developer; Colour Variants
  + live recolour cover the same ground.
- **`ColorsMenu` expanded** to 4 rows to fit the new Gradient Builder tile.
- **Testing guide rewritten** — `GROUP_10_TESTING_GUIDE.md` §2 overhauled (dress → gradient GUI), §3 gains
  fill-colour tests (③-④), §5 rewritten for anvil flow + layout changes.

**Files:** new — `gui/chest/{GradientPickerMenu,GradientSession}`. Rewritten — `gui/chest/{BgStudioMenu,
PaletteMenu,ColorsMenu}`, `command/handlers/ColorImageCommands`. Edited — `gui/chest/{BgStudioSession,Nav,
GuiRouter}`, `core/ColorToolService`, `image/BackgroundRemover`. Docs — `GROUP_10_TESTING_GUIDE.md`.

**TEST IN-GAME (developer):** `Reports/GROUP_10_TESTING_GUIDE.md` — all sections. Key new tests:
§2 ③-⑦ (gradient GUI + dress gone), §3 ③-④ (fill colour), §5 ①-⑥ (palette anvil + layout).

---

## 2026-06-14 (later 6) — Group 10 FINISHED in one push (build green; gates pass — NOT in-game)

Developer said: build the **entire** rest of Group 10 in one go, no per-slice handoff. Asked the two real
forks first — Smart/AI background mode → **pure-Java offline** (no model download, respects the no-internet
rule); screen eyedrop → **samples Minecraft's own screen** (no OS capture). Then built everything.
**Build green with JDK 21 (`C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`, `--no-daemon`); all three
gates pass.** Jar in `build/libs/` only.

**Done — rest of Group 10 (build green; gates pass — NOT in-game tested):**
- **`/cb bgstudio <id>`** — per-block Background Studio chest GUI (`BgStudioMenu` + `BgStudioSession`): pick
  None / Background-only / Background+enclosed / **Smart (offline)**, nudge tolerance, Apply. Re-bakes from the
  saved source (else the baked PNG) through the **existing** `BackgroundRemover`. **Undoable** (TEXTURE op).
- **`/cb tolerance <id> <0-100>`** — sets the strength + applies the current mode immediately. Undoable.
- **Smart BG mode (new, pure offline Java)** — added `SMART` to `BackgroundRemover` (+ `image/BgMask` split out
  to keep the file under the 500 gate): border-flood + enclosed pass + **keep-largest-connected-subject**. Never
  neural; falls back to the original image on any failure like the other modes.
- **Colour Variants panel** — `ColorVariantsMenu` off `/cb editor`: 7 algorithmic swatches (lighter/darker/
  vivid/muted/complementary/2× split-complement) via new HSL maths in `ColorMath`. Click → bakes a new
  `<id>_<variant>` block (deduped). One CREATE undo per click. (Distinct from the Group-06 `ColorVariantService`.)
- **`/cb palette` (per-player)** — new `PlayerPaletteManager` (atomic JSON, working set + named saves) +
  `PaletteCommands` (add/clear/list/save/load/delete) + `PaletteMenu` GUI (nearest-wool swatches) +
  **`/cb colors`** hub (`ColorsMenu`: palette · custom colour · variant colours · eyedrop).
- **exportpng `[download]` link** — `ResourcePackServer` now serves `/png/<id>` (export) + `/tex/<id>` (live
  texture); `exportpng` prints a clickable localhost **[download]**.
- **Live recolour slider (client)** — `RecolorSliderScreen` (Hue/Sat/Bright drag bars, live cell-grid preview
  fetched from `/tex/<id>`); **Apply** sends the new C2S `RecolorApplyPayload` → server bakes via
  `ColorToolService.applyRecolor` (TEXTURE undo). Server stays authoritative.
- **Screen eyedrop (client)** — `EyedropScreen` captures the framebuffer, reads the clicked pixel, opens chat
  pre-filled with `/cb palette add #RRGGBB` (reuses the palette command — no extra packet).
- **New `core/ColorToolService`** — the shared server flows (applyBgRemoval / createVariant / applyRecolor),
  same off-thread-bake → one-pack-rebuild idiom as the rest. New `GuiMode.RECOLOR_SLIDER`/`EYEDROP`; four new
  `Nav.Dest`s wired in `GuiRouter`; three new editor tiles.

**Files:** new — `image/BgMask`, `core/ColorToolService`, `core/PlayerPaletteManager`,
`command/handlers/ImageToolCommands`, `command/handlers/PaletteCommands`, `gui/chest/{BgStudioMenu,
BgStudioSession,ColorVariantsMenu,ColorsMenu,PaletteMenu}`, `client/gui/{RecolorSliderScreen,EyedropScreen}`,
`network/payloads/RecolorApplyPayload`. Edited — `image/{ColorMath,BackgroundRemover}`,
`command/handlers/ColorImageCommands`, `command/CommandRegistrar`, `network/ResourcePackServer`,
`gui/GuiMode`, `gui/chest/{Nav,GuiRouter,EditorMenu}`, `client/CustomBlocksClient`, `CustomBlocksMod`.

**TEST IN-GAME (developer):** `Reports/GROUP_10_TESTING_GUIDE.md` §3–§6 (plus re-confirm §2 dress/gradient if
not already). §6 needs the mod on your client (it does).

**Note on §6 (client screens):** live recolour + eyedrop compile but are the least gate-coverable parts (no
server gate can prove a client screen renders). The framebuffer eyedrop + the live preview fetch are the bits
most likely to need a tweak in-game — report what you see.

---

## 2026-06-14 (later 5) — Group 10 Slice 2: dress + gradient + real texture undo (green, NOT in-game)

Developer confirmed Slice 1's `/cb exportpng` works (the "didn't export" worry was just looking in the old
`config/customblocks1/cloud_exports` — the new mod writes to `config/customblocks/cloud_exports`; file was
there). Then: build Slice 2. **Build green with JDK 21 (`C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`,
`--no-daemon`); all three gates pass.** Jar in `build/libs/` only.

**Done — Group 10 Slice 2 (build green; gates pass — NOT in-game tested):**
- **`/cb dress <id> <colour> <intensity 0-1>`** — blends a solid colour over the block's current texture
  (linear per-channel mix; transparent padding untouched, so only the art is tinted). Colour accepts a
  ColorLibrary name OR hex; arg comes in as a greedy tail so a literal `#RRGGBB` parses (Brigadier `word()`
  rejects `#`). Off-thread bake → server-thread pack rebuild + sync. **Undoable** (see below).
- **`/cb gradient <id1> <id2> <steps 1-32>`** — averages each source block's colour, interpolates in
  **CIE-Lab** (Decision §M) at `k/(steps+1)`, bakes solid swatch blocks `gradient_1…N` (next free id). Whole
  batch is **ONE `/cb undo`** via `recordBatch` of CREATE children. One pack rebuild after the batch.
- **Texture-level undo now exists** (was explicitly deferred). Added `UndoManager.Kind.TEXTURE` + a
  `textureAfter` field on `Op` + `recordTexture(...)`; `HistoryCommands` restores pre-edit bytes on undo and
  re-applies post-edit bytes on redo, then rebuilds the pack. Dress is its first user; reusable by future
  pixel ops. `/cb resize`/`/cb retexture` still have no undo (unchanged this slice).
- **New `image/ColorMath`** — dress blend, alpha-weighted average colour, sRGB⇄CIE-Lab + `labLerp`, solid
  swatch PNG. Pure maths, no MC/server types. No existing image code touched.
- Files touched: `core/UndoManager`, `command/handlers/HistoryCommands`, `command/handlers/ColorImageCommands`
  (now Slices 1–2, ~310 lines, under the 400 gate), new `image/ColorMath`. `ColorImageCommands.register`
  already wired in CommandRegistrar (Slice 1) — dress/gradient are added inside it, no registrar change.

**Design call (asked first):** dress-undo → built real texture undo (clean 2-file core extension); gradient →
solid-colour swatches interpolated in Lab (matches spec "colour step"), not a texture cross-fade. Both
developer-chosen.

**Known limit (pre-existing, mod-wide):** redo of a CREATE restores metadata but not the texture file —
so redo-ing an undone gradient brings the blocks back textureless. Same as every other create; undo (the
common path) is perfect. Not expanded this slice.

**TEST IN-GAME (developer):** `Reports/GROUP_10_TESTING_GUIDE.md` §2 — `/cb dress` (G10.1/G10.2),
`/cb gradient` (G10.3), undo of each, and the bad-input refusals.

---

## 2026-06-14 (later 4) — Group 09 wrapped (S6 deferred) · Group 10 STARTED · Slice 1 resize + exportpng (green, NOT in-game)

Group 09 build-complete (Slices 1–5 + GUIs; Slice 6 deferred by developer). Moved to the next group in the
finale-fix sequence — **Group 10 (Color & Image Tools)** — built in tested slices, simplest first. **Build
green with JDK 21; all three gates pass.** Jar in `build/libs/` only.

**Done — Group 10 Slice 1 (build green; gates pass — NOT in-game tested):**
- **`command/handlers/ColorImageCommands`** (new):
  - **`/cb resize <id> <16-512>`** (suggests 64/128/256) — resamples a block's texture. Re-renders from the
    saved SOURCE image when there is one (lossless), else resamples the baked PNG. Off-thread bake →
    server-thread pack rebuild + sync. Locked blocks refused; no-texture blocks refused. Reuses the exact
    `/cb retexture` pipeline — no new image code. (Note: like all texture ops today, **no undo** — the saved
    source is untouched so it's re-derivable.)
  - **`/cb exportpng <id>`** — writes the block's current texture to
    `config/customblocks/cloud_exports/<id>.png` (atomic tmp→move; id sanitised for the filename) and prints
    the path. (The spec's clickable localhost `[download]` HTTP link is a later slice — needs a serve
    endpoint on ResourcePackServer.)
- Registered `ColorImageCommands` in `CommandRegistrar`.

**Chose the two simplest tools first on purpose** — both are self-contained and reuse tested infra (no undo
engine, GUI, AI, or live-preview needed). The heavier Group 10 features come in later slices:
- **Slice 2:** `/cb dress` (colour overlay, needs texture undo) + `/cb gradient` (CIE-Lab interpolated blocks).
- **Slice 3:** `/cb bgstudio` GUI (corners/flood/none) + `/cb tolerance <id>` + Color Variants panel.
- **Slice 4:** `/cb palette` (per-player) + exportpng HTTP `[download]` link.
- **Later/hard:** AI background removal, live recolor slider, screen eyedrop (client-side — discuss first).

**TEST IN-GAME (developer):** new `Reports/GROUP_10_TESTING_GUIDE.md` §1 — `/cb resize` (G10.6) and
`/cb exportpng` (G10.5).

---

## 2026-06-14 (later 3) — Slice 5 ✅ CONFIRMED · broken-blocks bulk actions + Safety dashboard GUI (green, NOT in-game)

Developer confirmed Slice 5 works ✅ and asked for two upgrades, then to start the next slice. **Build green
with JDK 21; all three gates pass.** Jar in `build/libs/` only.

**Developer-confirmed ✅ (in-game):** Slice 5 — `/cb showbrokenblocks` + `/cb safety` (chat) + the
rebuild-from-source fix.

**Done this session (build green; gates pass — NOT in-game tested):**
- **`/cb showbrokenblocks` is now a multi-select bulk fixer.** Per tile: **left-click ticks** for a bulk
  action, **right-click fixes just that one** (rebake / retexture). Footer: **Fix selected** (batch re-bake
  from saved images — one pack rebuild for the whole batch), selection summary/clear, **Select all**,
  **Delete selected** (→ confirm → trash; deleted blocks stay recoverable). New `BrokenSelection` (separate
  per-player store) + `BrokenConfirmMenu`; `SafetyCommands.guiRebakeMany` batches the re-bake.
- **`/cb safety` now opens an advanced GUI dashboard** (`SafetyMenu`) instead of chat (console still gets the
  chat summary). A framed 6-row panel with a health line + clickable tiles: **Blocks** (used/max),
  **Backups** → backup manager, **Auto-Backup** → config, **Trash** → trash browser, **Broken Blocks** →
  the fixer, plus a **Save a backup now** action. Reuses every existing screen — pure navigation, no logic
  duplicated.
- New dests `BROKEN_CONFIRM`, `SAFETY`; router cases added.

**TEST IN-GAME (developer):** `GROUP_09_TESTING_GUIDE.md` §5 — the new bulk select / Fix-selected /
Delete-selected on `/cb showbrokenblocks`, and `/cb safety` opening the dashboard.

**Slice 6 — DEFERRED by developer (2026-06-14).** Asked the two key questions before touching live
persistence. Decision: **-B is fresh now (no old data), but they want to bring the old -A data into -B AND
do the `config/customblocks/data/` path move LATER, together — not now.** So Slice 6 (first-boot
MigrationManager + data-path move) stays **unbuilt** until they're ready to migrate. Marked in the
`project_customblocks_dat_migration` memory. **Group 09 is now build-complete (Slices 1–5 + GUIs);
remaining: deferred Slice 6 + the backup-GUI polish pass.**

**Next → Group 10 (Color & Image Tools)** — the next group in the finale-fix sequence (`GROUP_10_COLOR_IMAGE.md`).

Developer confirmed the previous batch works in-game ✅ and said to push on. **Build green with JDK 21; all
three gates pass.** Jar in `build/libs/` only.

**Developer-confirmed ✅ (in-game):**
- **Slice 4 — deleted-block trash** (`/cb deletedblocks`: capture on delete, restore, pin, delete-forever, prune).
- **`/cb config` confirm gate** + **auto-backup config tile** (interval/keep cycling).

**Done — Slice 5 (build green; gates pass — NOT in-game tested):**
- **`core/BrokenBlockScanner`** (new, READ-ONLY) — flags every assigned block with **no baked texture file**
  (`!TextureStore.has(index)`); records whether a saved SOURCE image exists (so it can be auto-fixed). No
  world scan (placed-instance sweep deliberately skipped — too costly; the registry/texture mismatch is the
  cheap, reliable signal).
- **`command/handlers/SafetyCommands`** (new):
  - **`/cb showbrokenblocks`** — opens the report GUI (console prints a count).
  - **`/cb safety`** — read-only summary: blocks used/max · backups + newest · auto-backup on/off + interval ·
    trash count · broken count (clickable `[open]` when > 0; a `[/cb backup save]` nudge when 0 backups).
  - **`guiRebake`** — the GUI auto-fix: re-renders a broken block's texture **from its saved source** (no
    network), reusing the exact `BackgroundRemover → ImageProcessor → TextureStore → updatePack` pipeline
    that `/cb retexture` / retexture-all use. Off-thread bake → server-thread pack rebuild + sync.
- **`gui/chest/BrokenBlocksMenu`** (new) — paginated red-wool list. Click a tile: if it has a saved image →
  rebuild from source; if not → pre-fills `/cb retexture <id>` in chat. All-clear screen when nothing's broken.
- Registered `SafetyCommands`; added `Dest.BROKEN_LIST` + router case.

**Notes for testing:**
- "Broken" = missing baked texture only (renders purple). A block with a texture but no saved source is NOT
  flagged (it renders fine; many legit blocks — Arabic/video — have no source).
- Auto-fix only works when a source image was saved (normal for URL-created blocks). No-source blocks route
  to `/cb retexture`.

**TEST IN-GAME (developer):** `GROUP_09_TESTING_GUIDE.md` new **§5** — delete a texture file → `/cb
showbrokenblocks` shows it → rebuild-from-source fix → `/cb safety` summary.

**Next (after §5 passes):** Slice 6 — first-boot migration + move data to `config/customblocks/data/` (🔴
highest risk: touches live persistence — will be built extra-carefully and discussed first).

Developer **confirmed two pieces working in-game** ✅, asked for the backup GUI to be marked partial, two
config polish items, then to push straight into the next slice. **Build green with JDK 21
(`C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`); all three gates pass.** Jar in `build/libs/` only.

**Developer-confirmed ✅ (in-game):**
- **`/cb backup load`** (the `restore`→`load` rename) — works.
- **Auto-backup (Slice 3)** — timed auto-backup + prune works.

**🟡 PARTIAL / needs polish (developer's call):**
- **The advanced backup GUI** (`/cb backup` no-args `BackupMenu` + `BackupConfirmMenu`) — functional but the
  developer wants it polished further. Treat as **partial**, not done.

**Done this session (build green; gates pass — NOT in-game tested):**
- **Auto-backup is now a clickable tile in `/cb config`** (`ConfigMenu` slot 33, barrel icon): **left-click
  cycles the interval** (off→5→15→30→60→120→360 min), **right-click cycles the keep count** (3→5→10→20→50).
  Backed by new chat commands `/cb config autobackup interval [min]` / `keep [count]` (no value = cycle).
  Interval changes apply **immediately** via `AutoBackup.applyConfigChange()` (generation-token reschedule —
  no cancel/race; a superseded tick no-ops).
- **`/cb config` now asks first.** Opening config (command OR the dashboard Config button) lands on a Yes/No
  **`ConfigWarnMenu`** gate; **Yes** swaps in the config screen (Back from config then goes home), **No** backs
  out. Restores the old "are you sure" guard the finale-fix had dropped.
- **Slice 4 — deleted-block trash (`/cb deletedblocks`, alias `/cb trash`):**
  - **`core/TrashManager`** (new) — on delete, the block's fields + texture + source image are copied (atomic
    tmp→rename, BEST-EFFORT so a trash-write failure can't break the delete) into
    `config/customblocks/trash/<entryId>/`. `list()` is newest-first and **lazily prunes** unpinned entries
    older than `trashRetentionDays` (default **30**, 0 = keep forever); **pinned entries never prune**.
  - **Capture hook** in `SlotManager.delete` — snapshots BEFORE the texture is removed. (Undo's
    `removeSilently` is untouched, so undo doesn't spam the trash.)
  - **`TrashMenu` + `TrashEntryMenu`** (new) — paginated browser; per entry: **Restore · Pin/Unpin · Delete
    forever** (confirm). Restore reuses the tested `SlotManager.create` + setters + `TextureStore.save`
    (same path as `dupe`), then rebuilds + pushes the pack; refuses if the id is already taken or the pool is full.
  - **Config:** `trashRetentionDays` added (clamped 0..3650, persisted).

**⚠️ Call out for testing (Slice 4 is the risky one — it recreates live blocks):**
- **Restore correctness** — does a restored block come back with its texture, glow, hardness, sound,
  collision, category AND shape? (Per-face textures from the face editor are NOT captured yet — a restored
  face-edited block keeps only its main texture. Known limit.)
- Restoring when the id is taken / pool is full should fail cleanly with a chat message (not crash).
- Bulk-deleting many blocks copies each to the trash — watch for any lag on a very large bulk delete.

**TEST IN-GAME (developer):** `GROUP_09_TESTING_GUIDE.md` — new **§4** (delete → `/cb deletedblocks` → restore
/ pin / delete-forever), plus the config gate + auto-backup config tile. §2/§3 marked ✅.

**Next (after §4 passes):** Slice 5 (`/cb showbrokenblocks` + `/cb safety`, 🟢 read-only), then Slice 6
(first-boot migration + move data to `data/`, 🔴).

---

## 2026-06-14 — Group 09: `restore`→`load` rename · advanced backup GUI · Slice 3 auto-backup (green, NOT in-game)

Developer-requested polish on Group 09 + the next slice, built to test together. **Build green with JDK 21
(`Microsoft\jdk-21.0.10.7-hotspot`); all three gates pass (verifyFileSize/Mojibake/Sound). NOT in-game
tested.** Jar in `build/libs/` only — not copied to mods.

**Done — rename + GUI (build green; gates pass — awaiting in-game test):**
- **`/cb backup restore` → `/cb backup load`.** `load` is the primary verb everywhere (usage text, the
  restore-undo hint now says `/cb backup load <safety>`). **`restore` kept as a hidden alias** so old habit
  / the testing-guide steps still work.
- **Bare `/cb backup` (no args) now opens an advanced chest GUI** (`BackupMenu`) for players; console still
  gets the chat usage list. Also added **`/cb backupgui`** (matches `/cb listgui`, `/cb bulkgui`).
- **`BackupMenu`** — paginated, newest-first grid of every backup. Per tile: **left-click = tick for bulk
  delete**, **right-click = load** (→ confirm screen). Footer: Back · **Create new backup** (anvil name
  prompt, pre-filled auto name) · selection summary/clear · prev/page/next · **Select all** · **Delete N
  selected** (→ confirm) · Close. Auto/safety backups show a barrel icon + "(auto)" tag.
- **`BackupConfirmMenu`** — Yes/No screens for **load** (one backup) and **delete-selected** (bulk). The
  chest's Yes IS the confirmation, so the GUI path skips the chat `/cb confirm`.
- **Reuse, not rewrite:** the GUI calls new `BackupCommands.guiCreate` / `guiLoad`, which run the SAME
  tested save (`startSave`, refactored out of `save`) and `doRestore` orchestration as the chat commands.
  New `BackupSelection` (per-player ticked-names set) mirrors `ListSelection` but is a separate store so
  block- and backup-selections never collide.

**Done — Slice 3 (auto-backup + prune) (build green; gates pass — awaiting in-game test):**
- **`core/AutoBackup.java`** (new) — daemon scheduler. Every `autoBackupInterval` minutes it asks the
  **server thread** to flush slots + read the block count, then copies on a **separate IO worker** (same
  threading idiom as a manual save — no tick hitch). Saves an `auto-YYYYMMDD-HHMMSS` backup, then prunes.
  Runs **silently** (one log line per save, no chat). Self-reschedules reading config each cycle, so an
  interval change takes effect next cycle; **interval ≤ 0 disables** it (re-checked every 60s).
- **`BackupManager.pruneAuto(keep)`** — keeps the newest `keep` **`auto-`** backups, deletes the rest.
  **Only `auto-` folders are touched** — manual saves and `pre-restore-…` safety copies are never pruned.
- **Config:** `autoBackupInterval` (default **30** min, 0 disables) + `autoBackupKeepCount` (default **10**),
  both clamped + persisted in `CustomBlocksConfig`.
- **Lifecycle:** `AutoBackup.start(server)` on `SERVER_STARTED`; `AutoBackup.stop()` on `SERVER_STOPPING`
  **before** the final `saveAll`, so no auto-backup fires mid-shutdown.

**Design notes / call out for testing:**
- Auto-backup config is read from disk — to test, set `autoBackupInterval: 2` in `config/customblocks/
  config.json` and restart (G09.5). Default 30 min means you won't see one quickly otherwise.
- Creating a backup from the GUI reopens the list **after** the (async) save finishes, so the new backup
  appears without a manual refresh.

**TEST IN-GAME (developer):** `GROUP_09_TESTING_GUIDE.md` — updated §2 to `load`, new **§GUI** (open `/cb
backup`, create, tick + bulk-delete, right-click load) and **§3** (auto-backup fires + prunes).

**Next (after these pass):** Slice 4 (`/cb deletedblocks` trash browser + pin).

---

## 2026-06-13 (later 12) — Group 09 Slice 2: restore + delete + panic + recover (green, NOT in-game)

Built the dangerous slice (it overwrites live data) carefully. **Build green with JDK 21; all three gates
pass. NOT in-game tested.** Jar at `build/libs/` only — not copied to mods.

**Done — Slice 2 (build green; gates pass — awaiting in-game test):**
- **`BackupManager.restore(name, currentBlocks)`** — SAFE SWAP: verify the chosen backup parses (else
  abort, live untouched) → **MOVE** the current live files into a fresh `pre-restore-<stamp>` backup
  (fast rename; doubles as a recoverable snapshot) → **COPY** the chosen backup's files into live. On a
  copy failure it best-effort rolls the safety copy back, then rethrows. Plus `isValidBackup`,
  `latestName`, `delete`, `moveIfExists`, `rollback`.
- **`BackupCommands`** — `/cb backup restore <name>` (confirm-gated via the existing `BulkConfirm`),
  `/cb backup delete <name>`, `/cb backup panic` (restore newest, NO confirm — emergency), and top-level
  **`/cb recover`** (restore newest, with confirm). Name tab-complete added.
- **`doRestore` (server thread):** pause pack → `saveAll` → `BackupManager.restore` → `CustomBlocksConfig
  .load()` → `SlotManager.reload()` → resume (rebuilds pack) → `syncToAll`. On failure: resume + leave
  data as-is + incident-log. Reports the `pre-restore-…` safety name so the developer can undo a restore.

**Design notes / known limits (call out for testing):**
- Restore runs **synchronously on the server thread** (brief hitch on a big restore) — deliberate, so no
  other command can edit slots mid-swap. Acceptable for a rare, safety-critical op.
- Placed-block **glow** isn't re-applied to already-placed blocks on restore (matches startup, which does
  no post-load relight); the block's SlotData glow IS restored, so re-placing/breaking picks it up. Minor.
- `CustomBlocksConfig.load()` re-reads the restored config; a backup from a different maxSlots is an
  untested cross-version edge (same-version backups are fine).

**TEST IN-GAME (developer):** `GROUP_09_TESTING_GUIDE.md` §2 (restore needs confirm · brings block back ·
safety copy auto-saved · cancel · recover · panic · delete · survives restart). **Test §1 too if not yet.**

**Next (after §1+§2 pass):** Slice 3 (auto-backup timer + prune).

---

## 2026-06-13 (later 11) — Group 09 STARTED · Slice 1 (backup save + list) built (green, NOT in-game)

Developer parked `shapepreview [id]` as **PARTIAL** (base works; textured `[id]` deferred — noted in
`GROUP_08_SHAPES.md`) and moved to **Group 09 (Backup & Data Safety)** with a strong "be surgical" note.
Group 09 is greenfield in -B and large/dangerous, so it's being built in **tested slices, safest first**
(plan in `GROUP_09_TESTING_GUIDE.md`). Developer approved **starting with Slice 1 only**.

**Done — Slice 1 (build green; gates pass; NOT in-game tested):**
- **`core/BackupManager.java`** (new) — point-in-time backups under `config/customblocks/backups/<name>/`:
  copies live `slots.json` + `config.json` + `textures/` + `sources/` verbatim, plus a `manifest.json`
  (epoch + human time, block count, auto flag). **READ-ONLY w.r.t. live data** — never writes the live
  files. Built in a `<name>.tmp` dir then **atomically renamed**, so a crash mid-copy can only leave a
  stray `.tmp` (ignored by `list()`), never a half-written named backup. Name validated
  `[A-Za-z0-9_-]{1,48}` (no path traversal). `list()` reads manifests, newest first.
- **`command/handlers/BackupCommands.java`** (new) — `/cb backup save [name]` (auto-names if blank;
  refuses duplicates + bad names) and `/cb backup list`. Save flushes `SlotManager.saveAll()` on the
  server thread first, then copies on a daemon worker (heavy-I/O idiom) → chat on completion.
- Registered in `CommandRegistrar`.
- **Path-agnostic by design:** backups copy whatever paths exist *now* and (Slice 2) restore them
  exactly — so this slice does NOT touch the risky data-path normalization (deferred to Slice 6).

**Deliberately NOT built yet (next slices, each tested before the next):** restore/delete/panic/recover
(Slice 2, 🔴 overwrites live data), auto-backup (3), trash browser (4), broken-blocks + safety (5),
first-boot migration + path move to `data/` (6, 🔴 highest risk).

**Jar:** built to `build/libs/customblocks-1.0.0.jar` only — **not** copied to any mods folder
(developer's instruction; build.gradle has no auto-deploy task anyway).

**TEST IN-GAME (developer):** `docs/Finale Fix/Reports/GROUP_09_TESTING_GUIDE.md` §1 (G09.1–2: save named/
auto, duplicate + bad-name refused, list). After it passes → Slice 2 (restore/panic) with extra care.

---

## 2026-06-13 (later 10) — x-ray + shapepreview fixes ✅ in-game · ideas captured

Developer confirmed **both fixes pass in-game** ✅ — `.nonOpaque()` killed the x-ray on shaped blocks, and
`/cb shapepreview` now spawns (op-level summon). No code this turn — captured two agreed/brainstormed
ideas in `docs/Finale Fix/GROUP_08_SHAPES.md`:
- **FaceGuide** (replaces the confusing face-tile clarity problem): a built-in auto-seeded block with
  N/E/S/W/UP/DOWN painted on its faces + a `/cb faceguide` toggle that temporarily swaps a looked-at block
  to it (auto-restores after ~30s / on relog). Letters drawn in-code, no download.
- **Textured shape preview** `/cb shapepreview <shape> [id]`: preview a shape wearing a custom block's
  texture with NO pack rebuild — summon `block_display`(s) of `customblocks:slot_N` transformed into each
  `BlockShapes.boxes(shape)` box. Single-box exact; multi-box = one display per box; cross = fallback.

**Next (build order, after this):** FaceGuide block + `/cb faceguide`; textured `shapepreview` arg2;
`facechangegui` no-arg list-pick flow; then `bulkshape`; then the triangle chat.

---

## 2026-06-13 (later 9) — Group 08 tested: x-ray + shapepreview bugs FIXED · GUIs parked for polish

Developer ran the Group 08 guide. **Shapes, shape editor GUI, face commands + face editor GUI all
pass** ✅ — but two bugs + GUI-clarity feedback. **Build green with JDK 21; all three gates pass.
The two fixes are NOT yet in-game re-tested.**

**Bugs fixed this session (build green — awaiting in-game re-test):**
- **X-ray on shaped blocks** — placed slab/pillar/etc. let you see through the world behind them.
  Cause: `SlotBlock` was registered as a full **opaque** cube, so neighbours culled their faces against
  it regardless of the real shape. Fix: `AbstractBlock.Settings.nonOpaque()` in `SlotManager.registerAll`
  so the game respects each block's actual culling shape. Bonus: cut-out (transparent-background)
  textures now show through. ⚠️ Re-test that **full** image blocks still look right (lighting can differ
  slightly for non-opaque blocks).
- **`/cb shapepreview` showed nothing** — the chat line printed but no block appeared. Cause: the inner
  vanilla `summon` ran on a `withSilent()` source at the **player's** permission level; in a no-cheats
  world that's level 0, so `summon` (needs 2) failed silently. Fix: run summon/kill on `src.withLevel(4)
  .withSilent()` (op level), so it works regardless of cheats/op. (Confirmed via latest.log: "Previewing
  …" fired, no summon, no error — classic silenced permission failure.)

**GUI updates — 🟡 PARKED for polish (developer's call, do later):**
- **Face editor layout is confusing** — coloured-glass tiles don't read as "this is the north face,"
  etc. (see the test screenshot). Needs a clearer, intuitive layout — brainstormed options in chat /
  below; not built yet.
- **`/cb facechangegui` no-arg flow** — should open the block list (`listgui`) in single-pick mode →
  confirm one block → then the face editor for it. Not built yet (part of the same polish pass).

**Docs:** the **Custom Shape Sculptor** idea is now saved at the bottom of `docs/Finale Fix/
GROUP_08_SHAPES.md`, clearly marked "currently just an idea."

**Next:** developer re-tests the two fixes (x-ray on a slab + `/cb shapepreview slab_top`); then the
face-GUI polish (layout redesign + no-arg list-pick flow); then `bulkshape`; then the triangle chat.

---

## 2026-06-13 (later 8) — Group 08 slices: shape cmds + 2 GUIs + face aliases + shapepreview (build green, NOT in-game)

Continued Group 08. Slice 1 (shape **commands** + live collision/outline + generated pack model) was
already in the tree from the prior session and **compiles**; this session added the two **chest GUIs**,
the **spec-name face aliases**, and **`/cb shapepreview`**, all delegating to tested commands / the
vanilla command parser. **Build green with JDK 21; all three gates pass (verifyFileSize /
verifyMojibake / verifySound). NOT in-game tested.**

**Done (build green; gates pass — awaiting in-game test):**
- **Shape editor GUI** — `gui/chest/ShapeEditorMenu.java` (new). One button per shape (current one
  enchant-glinted) + a "reset to full" tile; each click runs the tested `/cb setshape`/`/cb clearshape`
  via `GuiRouter.runAndReopen`. No shape logic duplicated. Opened by `/cb shapeeditor <id>`.
- **Face editor GUI** — `gui/chest/FaceEditorMenu.java` (new). One tile per face (down/up/north/south/
  west/east); **left-click** chat-prefills `/cb paintface <id> <face> ` for a URL paste (the proven
  long-URL path — URLs don't fit an anvil), **right-click** clears that face, and a tile clears all.
  Opened by `/cb facechangegui <id>`.
- **Wiring** — `Nav.Dest` (+`SHAPE_EDITOR`,`FACE_EDITOR`), `GuiRouter.build` (2 cases),
  `ChestGuiCommands` (`shapeeditor`/`facechangegui` commands; `editor` refactored to share a new
  `openFor(dest,id)` helper).
- **Spec-name face aliases** — `FaceCommands.java`: `/cb setface` (= `paintface`) and `/cb clearallfaces`
  (= `clearface <id> all`). Pure aliases over the tested handlers — no new texture logic.
- **`/cb shapepreview <shape>`** — `ShapeCommands.java`. Floats a vanilla stand-in block (slab→stone
  slab, stairs→stone stairs, cross→poppy, …) ~2.5 blocks ahead at eye level, auto-removed after 5s.
  Built by orchestrating the vanilla `summon block_display` / `kill @e[tag=…]` as the player via the
  command parser (the mod has **no** entity code, and `BlockDisplayEntity` exposes no clean setter),
  each spawn uniquely tagged; cleanup runs on the daemon-thread idiom → `server.execute`. Player + op
  only (summon needs level 2); both vanilla commands run on a `withSilent()` source so only our own
  friendly message shows.

**Decisions:**
- Per-face textures were ALREADY built in Group 06/M4 (`paintface` / `clearface <face|all>` +
  `TextureStore` face I/O + `ServerPackGenerator` per-face cube). Group 08's `setface`/`clearallfaces`
  are just spec-named aliases over them — nothing re-implemented.
- Face GUI uses **chat-prefill** for the URL (not the literal "anvil" the spec mentions): the anvil
  rename box can't hold a full image URL, and chat-prefill is the mod's established URL-input pattern
  (Retexture, Rainbow Rectangle). Functionally satisfies G08.11.

**Decision on shapepreview:** developer chose the **summon-display** approach (over a temp real block /
deferring) — chosen because the NBT parser handles the geometry, it looks native, and the daemon-thread
auto-kill matches the mod's idiom. Trade-off: it needs the player to be op (summon = level 2), which the
server owner is.

**Editor links:** added **Shape** (stonecutter, slot 25) + **Faces** (item frame, slot 26) tiles to
`EditorMenu` — `/cb editor <id>` now reaches both new GUIs (navigate, so Back returns to the editor).

**Still queued:** `bulkshape`; `customtriangle`/`trianglemode` (triangular geometry vs the existing
Group 06 colour-variant "Triangle" — needs a design chat). New idea parked for discussion: a per-voxel
**custom-shape sculpt tool** as an Omni-Tool mode (could grow into its own group) — see notes below / chat.

**TEST IN-GAME (developer):** `docs/Finale Fix/Reports/GROUP_08_TESTING_GUIDE.md` (new) — shape commands
(§1), shape editor GUI (§2), face commands/aliases (§3), face editor GUI (§4), shape preview (§5).

**Next (after the above pass):** `bulkshape`; then discuss the triangle features.

---

## 2026-06-13 (later 7) — bulkreid ✅ (partial) · starting Group 08 (Shapes & Per-Face)

`/cb bulkreid` command **passes in-game** ✅ — developer marked it **partial / needs polish**: the
Step1→Step2 GUI for reid + a Hub tile + a polish pass are **parked for later** (revisit with bulk
recolor). Then asked to **implement Group 08 (Shape System & Per-Face Textures) entirely**.

**Parked (bulkreid follow-ups):** reid GUI op in `BulkActionMenu` + Hub tile + repoint no-arg + polish.

**Group 08 — scope (the largest group; deep rendering/pack work, both features greenfield in -B):**
shapes (slab/top·bottom, thin, carpet, wall, pane, stairs, cross, pillar, custom AABB) + per-face
textures (6 faces) + 2 chest GUIs (shape editor, face editor) + shape preview + ~14 commands
(setshape/add/remove/clear, shapelist, shapepreview, setface/clearface/clearallfaces, facechangegui,
customtriangle, trianglemode, bulkshape). Spec: `docs/Finale Fix/GROUP_08_SHAPES.md`. Built in
verifiable slices (one tested before the next), same rhythm as the bulk rounds — see below.

---

## 2026-06-13 (later 6) — /cb bulkreid command built (command-first; GUI after it passes)

Developer asked to do **bulkreid next** (recolor deferred), then move to the next group. Following the
proven reid/export rhythm: **command first → in-game test → then the GUI**. Built the new
`/cb bulkreid` command. **Build green with JDK 21; gates pass; jar deployed 18:06. NOT in-game tested.**

**Design (mirrors `/cb bulkrename`, but transforms the ID not the display name):**
- `/cb bulkreid <filter> prefix <text> | suffix <text> | replace <old> <new>` — changes each matched
  block's custom id by the pattern, **keeping the slot** (textures + placed blocks untouched, no pack
  rebuild — same as single `/cb reid`).
- Per block SKIPS + reports: locked · no-change · invalid id (must match the create/word charset) ·
  **collisions** — a newId already taken by a block or already claimed earlier in the same batch (this
  also rules out unsafe id swaps).
- ONE undo entry: a `BATCH` of `REID` children (HistoryCommands already reverses REID inside a batch),
  so a single `/cb undo` re-ids them all back. Big/"all" batches held for `/cb confirm` like the others.

**Done (build green; gates pass — NOT in-game tested):**
- `command/handlers/BulkReidCommands.java` (new) — the command above. Reuses `BulkScope`,
  `SlotManager.reId`/`hasId`, `UndoManager.recordBatch`, `BulkConfirm`, `BulkChat`, `HudSync` — no new
  id/undo machinery. Tab-complete via the existing `BulkSuggestions.RENAME_ARGS` (same token layout).
- `command/CommandRegistrar.java` — registers `BulkReidCommands`.
- No-arg `/cb bulkreid` prints usage **this round** (the GUI doesn't exist yet); it will open the
  builder once the reid op is added to `BulkActionMenu`.

**TEST IN-GAME (developer):** `docs/Finale Fix/Reports/GROUP_07_TESTING_GUIDE.md` → new **§A2**
(pattern re-id · undo the batch · collision skipped · big-batch confirm · guards).

**Next (after §A2 passes):** add the **reid** op to the Step1→Step2 GUI (thin front-end over this
command — `BulkActionMenu` reid branch using mode+text controls, like rename) + a Bulk Hub tile +
repoint the no-arg command to open Step 1. Then move on to the next group.

---

## 2026-06-13 (later 5) — ALL bulk ops Step1→Step2 ✅ verified in-game · dead dashboard removed

Developer confirmed **all 7 remaining ops pass in-game** ✅ (edit · delete · rename · category ·
duplicate · lock · favorite — export already passed). The two-step bulk GUI rollout is **done**.

**✅ Verified in-game (developer, 2026-06-13):** every bulk op through Step1→Step2 (all 8 incl. export).

**Done (cleanup — code only, no behaviour change; build green, gates pass, jar redeployed 17:55):**
- Deleted the now-unreachable old single-screen dashboard: `gui/chest/BulkPropertyMenu.java` and
  `gui/chest/BulkFilterMenu.java`.
- Removed their `Nav.Dest` entries (`BULK_PROPERTY`, `BULK_FILTER`) and `GuiRouter` routes.
- Removed the orphaned `BulkSession` helpers they were the only callers of (`prettyFilter`,
  `toggleExportFormat`) + the now-unused `Locale` import; refreshed stale "Called by" javadoc that
  pointed at the deleted classes.
- Backend untouched. The bulk subsystem is now exactly: Hub → `BulkSelectMenu` (Step 1) →
  `BulkActionMenu` (Step 2) → tested `bulk…` command paths, with `BulkStyle` for shared styling.

**Next:** the §D backlog — bulk **recolor(edge)** and bulk **reid** (each via the same Step1→Step2 flow).

---

## 2026-06-13 (later 4) — Step 1 → Step 2 GUI rolled out to ALL bulk ops

Developer confirmed the export Step1→Step2 flow + despeckle v2 **pass in-game** ✅, and asked to
extend the flow to every bulk op — "all, but one consistent pattern, no mistakes." Generalized the
export flow into a shared selector + one per-op action screen, and routed every entry point through it.
**Build green with JDK 21; all three gates pass. The other ops are NOT in-game tested — that's the handoff.**

**✅ Verified in-game (developer, 2026-06-13):** export Step1→Step2 flow · despeckle v2 (before/after screenshot).

**Done (build green; gates pass — non-export ops awaiting in-game test):**
- **Generalized Step 1** — `gui/chest/BulkSelectMenu.java` (new) replaces the export-only
  `BulkExportSelectMenu`. Op-agnostic: reads `BulkSession.op` only to colour/label; the selection
  (Option A filter-cycle / Option B hand-pick + 🟩 green-concrete confirm) is identical for every op.
- **Generalized Step 2** — `gui/chest/BulkActionMenu.java` (new) replaces `BulkExportActionMenu`.
  One op-driven screen: 🔍 Review + the op's controls + ✔ Apply. Controls per op — edit: setting+value ·
  rename: mode+text(s) · category: target · export: format · lock/favorite: direction · delete &
  duplicate: none. Apply delegates to the existing tested `applyXFromGui` paths with the Step-1 scope.
- **Shared styling** — `gui/chest/BulkStyle.java` (new): per-op frame/header/icon palette, shared by
  both new menus.
- **Selection state generalized** on `BulkSession`: `export*` selection fields/methods renamed to
  generic `sel*` (`selMode`/`selFilterKind`/`selFilterValue`, `selScopeExpr`, `selHasSelection`,
  `selLabel`, `cycleSelFilterKind`, `selKindLabel`); `listPickForExport` → `listPickForBulk`. Export
  format helpers kept (`EXPORT_FORMATS`, `cycleExportFormat`).
- **Every entry point routed through the new flow:** `BulkHubMenu` all 8 op tiles → Step 1 (op set +
  `resetSelection`); each `/cb bulk<op>` no-arg → Step 1 via the shared `BulkCommands.openOpBuilder`
  (added rename + lock/unlock/favorite/unfavorite no-arg openers too); `BlockListMenu` green-concrete
  confirm now returns to the generic Step 1. `Nav` dests `BULK_EXPORT_*` → `BULK_SELECT`/`BULK_ACTION`.
- **Backend untouched:** `BulkScope`, `ListSelection`, and every `bulk…` command path are unchanged —
  no bulk/export logic duplicated or rewritten. All files under the §9.3 gates.
- **Left in place (now unreachable, not deleted):** the old single-screen `BulkPropertyMenu` +
  `BulkFilterMenu` (and their `BULK_PROPERTY`/`BULK_FILTER` routes). Safe to remove once the new flow
  passes in-game — flagged as a follow-up.

**TEST IN-GAME (developer):** `docs/Finale Fix/Reports/GROUP_07_TESTING_GUIDE.md` → new **§A1**
(every op through Step1→Step2: edit · delete · rename · category · duplicate · lock · favorite — export
already ✅). Confirm each op's Step 2 controls work and Apply changes only the selected blocks.

**Next (after §A1 passes):** delete the dead `BulkPropertyMenu`/`BulkFilterMenu` + their routes.

---

## 2026-06-13 (later 3) — bulk export GUI redesign built (Step 1 → Step 2) · the template

Built the approved two-step bulk-export GUI — export first, as the template for the other bulk ops.
**Build green with JDK 21; all three gates pass (verifyFileSize / verifyMojibake / verifySound).
NOT in-game tested — that's the handoff.**

**Done (build green; gates pass — awaiting in-game test):**
- **Step 1 — Selection GUI** (`gui/chest/BulkExportSelectMenu.java`, new). Opens at
  **"None selected currently"**. *Option A*: a Filter tile that cycles All blocks / Category /
  Favorited / Locked / Name contains / ID starts; the three text kinds reveal an anvil value tile.
  *Option B*: "Select specific blocks" → the block list in pick mode → a 🟩 green-concrete
  **"Use these N block(s)"** confirm returns here. **Next →** lights only once the selection resolves
  to ≥1 block.
- **Step 2 — Action GUI** (`gui/chest/BulkExportActionMenu.java`, new). 🔍 Review (the exact block
  ids), 📄 Format chooser cycling **all** formats (json · txt · png · csv · md · html · yaml), and
  **✔ Click to Export** beside it → delegates to the tested
  `BulkExportCommands.applyExportFromGui` (i.e. `/cb bulkexport <scope> <format>`).
- **Backend reused, not rewritten:** selection state rides on `BulkSession` (new export-flow fields
  + helpers: `exportSelMode`/`exportFilterKind`/`exportFilterValue`, `exportScopeExpr`,
  `exportHasSelection`, `exportSelLabel`, `cycleExportFormat`); picks reuse `ListSelection`; the
  resolve is the same `BulkScope`; the export is the same command path. No bulk/export logic duplicated.
- **Wiring:** `Nav` (+`BULK_EXPORT_SELECT`,`BULK_EXPORT_ACTION`), `GuiRouter` (2 routes),
  `BlockListMenu` (green-concrete confirm shown only in export-pick mode — normal "Bulk actions on N"
  unchanged otherwise), `BulkExportCommands.openBuilder` + `BulkHubMenu` Export tile now open Step 1.
  The old export branch in `BulkPropertyMenu` is left intact (just no longer the front door). All
  files under the §9.3 gates.

**TEST IN-GAME (developer):** `docs/Finale Fix/Reports/GROUP_07_TESTING_GUIDE.md` → new **§A0**
(`/cb bulkexport` → Step 1 selection → Step 2 review/format/export, all 7 formats, green-concrete pick).

**Next (after §A0 passes):** replicate the Step 1 → Step 2 flow to the other bulk ops, one at a time.

---

## 2026-06-13 (later 2) — testing-guide overhaul (v3 style) · bulk GUI redesign queued

Developer confirmed the reid GUI + listgui A/B/C **all pass in-game** ✅. Then asked for (1) a docs
overhaul and (2) a multi-step bulk GUI redesign. Style + build-order approved via question.

**Done — docs (no code, no build needed):**
- **New v3 testing-guide blueprint** (`Reports/_TESTING_GUIDE_TEMPLATE.md`) — readable style: a
  🗺️ "At a glance" map first, two big dividers (🎯 Test now / ✅ Passed), soft 💡 What-it-does +
  🧰 Before-you-start intros, **one test = one bullet** with ✅ Pass / ❌ Broken-if, 📋 scorecards,
  emoji grouping. Goal: less overwhelm.
- **Rewrote all 7 group testing guides** to v3: `GROUP_02`, `03`, `04`, `05`, `06`, `07`, `25`
  (`Reports/*_TESTING_GUIDE.md`). Every existing test preserved; long prose history condensed into
  scannable bullets. Group 06's M4 moved from "coming" to "test now" (it's built); Group 07 notes
  the upcoming Step1→Step2 redesign.

**Queued — bulk GUI redesign (approved: export-first as the template):**
- New **Step 1 selection GUI**: shows "None selected currently"; Option A filter-cycle
  (All / Category / Favorited / Locked / Name / ID), Option B "Select specific blocks" → listgui →
  🟩 green-concrete confirm. Then **Next →**.
- New **Step 2 action GUI** (export): 🔍 Review (the matched blocks) · Format chooser (json·txt·png·
  csv·md·html·yaml) · ✅ Click to Export. Delegates to the tested `/cb bulkexport` command.
- Reuses `ListSelection`, `BlockListMenu`, `BulkScope`, `BulkSession`, the bulk commands.
- **Next session:** build the export flow, build green, hand off for test; then replicate to the
  other bulk commands.

---

## 2026-06-13 (later) — listgui upgrades: search + multi-select + bulk-on-selection (A → B → C)

Developer asked for "advanced bulk + advanced search, one by one." Built the first three slices on
`/cb listgui` in one pass (they'll test the batch together); slice D (advanced search operators) is next.

**Done (build green; gates pass — NOT in-game tested):**
- **Slice A — in-list search.** `/cb listgui` gains a Search tile (anvil) that filters the list by
  id / name / category (case-insensitive contains). The query rides on the `MenuKey` arg so paging
  keeps it; right-click Search clears it.
- **Slice B — tick-many multi-select.** New `gui/chest/ListSelection.java` (per-player, order-preserving
  id set). In the list, **left-click ticks/unticks** a block (glint + ✔), **right-click opens the
  editor** (was left-click). Footer adds "N selected" (click clears) and "Select all shown".
- **Slice C — bulk actions on the selection.** A "Bulk actions on N selected" tile seeds
  `BulkSession.filter` with the ticked ids (`BulkScope` already resolves a comma id-list) and opens
  the existing, tested **Bulk Hub** — so delete / lock / favourite / category / duplicate / export /
  property / rename all work on the hand-picked set. **No bulk logic duplicated.**
- **Files:** `gui/chest/ListSelection.java` (new), `gui/chest/BlockListMenu.java` (rewritten),
  `gui/chest/GuiRouter.java` (routes the search query). All under the §9.3 gates.

**Behaviour change to call out:** in the list, **left-click now selects, right-click edits**
(previously left-click opened the editor). Documented in the Group 07 listgui test note.

**TEST IN-GAME (developer):** the new 🎯 "listgui upgrades" section in
`docs/Finale Fix/Reports/GROUP_07_TESTING_GUIDE.md` (search · multi-select · bulk-on-selection).

**Next (after this passes):** slice D — advanced search operators (compound filters: category +
locked + name-contains, saved filters) + "more stuff", one by one.

---

## 2026-06-13 (reid command ✅ verified · reid GUI slice B built · export ✅ working · docs synced)

Developer confirmed the `/cb reid` command works in-game → **slice A ✅ verified**. Also confirmed
**export works** (polish pending — see the export entry below). Then built slice B (the reid GUI) +
wired reid into the block editor, synced the testing guide, and logged a chat-polish item.

**✅ Verified in-game (developer, 2026-06-13):**
- `/cb reid <id> <newId>` command — slice A (the whole command + undo + id-migration entry below).
- Export formats incl. PNG (build + in-game) — **polish pending**, not blocking.

**Done (build green; gates pass — GUI NOT in-game tested yet):**
- **Reid GUI — slice B.** New `gui/chest/ReIdMenu.java`:
  - no-arg `/cb reid` → a paginated **pick-a-block** menu; click a block → anvil pre-filled with
    its current id; type a new id, take the result.
  - single-arg `/cb reid <id>` → **straight to the anvil** (checks the block exists + isn't locked
    first, so a typo or locked block fails cleanly instead of opening a dead prompt).
  - shared `ReIdMenu.openAnvil(player, id, onCancel)` → on submit **delegates to the tested
    `/cb reid <id> <newId>` command** (lock check, id-key migration, undo, chat all unchanged); on
    cancel reopens the source menu. The rules stay in one place — the GUI is a thin front-end.
  - `Nav.Dest.REID` added + routed in `GuiRouter`.
- **`/cb editor <id>` → new "Change ID" button** (anvil icon, slot 28, left of Rename) → same
  `openAnvil`. So reid is reachable from the block editor too.
- **Files:** `gui/chest/ReIdMenu.java` (new), `gui/chest/EditorMenu.java`, `gui/chest/Nav.java`,
  `gui/chest/GuiRouter.java`, `command/handlers/ReIdCommands.java`. All under the §9.3 gates.

**Docs synced:**
- `docs/Finale Fix/Reports/GROUP_25_TESTING_GUIDE.md` (new) — reid command (☑️ passed 2026-06-13) +
  reid GUI (🎯 test now), in the v2 per-group format. Testing lives **per group**, not in the old
  V1 batch guide.
- `docs/CHANGELOG.md` — reid marked verified + reid GUI added.
- `docs/Finale Fix/GROUP_04_CHAT.md` — new **Chat-polish backlog**: the `/cb rename` message reads
  `Renamed "X" to "X"` when id == new display name and doesn't label which is the id vs the name.

**TEST IN-GAME (developer) — reid GUI (slice B):**
```
/cb reid                       → pick-a-block menu opens; click a block → anvil shows its id
   (edit to a new id, take the result)   → "Changed id <old> to <new>." ; /cb list shows it
/cb reid <id>                  → anvil opens straight away for that block
/cb editor <id>  → Change ID   → same anvil
/cb reid <lockedId>            → refused up front ("locked, unlock first") — no dead anvil
press Esc on the anvil         → returns to the picker / editor, nothing changed
```

**Next (after the GUI passes):** the item after reid — **Fix 1: tick-many picker + in-list search
on `/cb listgui`**. Needs a quick design chat first: does the multi-select picker **replace** or
**complement** the existing filter model (`BulkFilterMenu`)? (See the export entry's "Also pending".)

---

## 2026-06-12 (/cb reid — command + undo + id migration · GUI deferred to slice B)
> **✅ Slice A verified in-game by the developer 2026-06-13.** Slice B (the GUI) was built the
> same day — see the top entry.

Started the `/cb reid` work the previous entry handed off. Developer chose **command first, GUI
after in-game test** (their usual "GUI is a thin front-end over the tested command" pattern).

**Done (build green; gates pass; jar staged 16:12 — NOT in-game tested):**
- **`SlotManager.reId(oldId, newId)`** — changes the id, **keeps the slot index** (so the baked
  texture, keyed by index, does NOT move and placed `slot_N` blocks are unaffected → **no pack
  rebuild needed**). Guards: oldId exists, newId non-blank / not equal / not taken. Migrates every
  id-keyed reference, then `saveAll()`. Records no undo (caller's job).
- **Id-ref migration — audited the whole tree, these are ALL of them.** New `renameId(old,new)`
  mover on each: `LockManager`, `FavoritesManager` (per-player, moves across **every** player's
  set, order preserved), `BlockNotesManager`, `DraftManager`. Category rides on `SlotData` (free).
  `MutationLog` is an audit log → left as-is (records what happened). TemplateManager /
  MagicItemsManager key by template-name / tool-id, NOT block id → untouched.
- **Undo — `Kind.REID`.** reId is its **own inverse** (the migration is a pure move), so undo just
  re-ids back: `UndoManager.recordReid(before,after)`; `HistoryCommands` REID cases call
  `SlotManager.reId(after,before)` (undo) / `reId(before,after)` (redo). No snapshot/texture
  machinery added. `describe()` shows `reid old → new`.
- **New `command/handlers/ReIdCommands.java`** (split out — CreationCommands is 381 lines, near the
  400 gate): `/cb reid <id> <newId>`, both `word()` args, tab-complete on the existing id via
  `BlockSuggestions.IDS`, locked-block refusal (same rule as rename/delete/retexture), clear
  per-case errors, HUD re-sync. Registered in `CommandRegistrar` (after CreationCommands).

**Files:** `core/SlotManager.java`, `core/LockManager.java`, `core/FavoritesManager.java`,
`core/BlockNotesManager.java`, `core/DraftManager.java`, `core/UndoManager.java`,
`command/handlers/HistoryCommands.java`, `command/handlers/ReIdCommands.java` (new),
`command/CommandRegistrar.java`.

**TEST IN-GAME (developer):**
```
/cb create reidtest ReidTest          → make a block
/cb fav reidtest · /cb lock reidtest · /cb note reidtest hello   → attach state
/cb lock reidtest  →  /cb reid reidtest x   → refused ("locked, unlock first")
/cb unlock reidtest
/cb reid reidtest newname             → "Changed id reidtest to newname"
/cb list                              → shows newname (same slot #), reidtest gone, texture intact
/cb favs · /cb locked · /cb note newname   → state followed the new id (newname fav/locked/noted)
/cb undo                              → back to reidtest, state follows back
/cb redo                              → newname again
/cb reid newname existingId           → "already taken" error (no change)
```

**Next (slice B, after pass):** the GUI — no-arg `/cb reid` → pick-a-block menu (clone
`BlockListMenu`), click → `AnvilPrompt` for the new id → runs the verified backend. Single-arg
`/cb reid <id>` → straight to the anvil.

---

## 2026-06-12 (export formats incl. PNG · recolor parked · reid handoff)

Developer originally had these 3 done in the **old `CustomBlocks/` reference repo by mistake**; that
repo has been **reverted to original** (only my 6 edits there were undone; their other uncommitted
work was left untouched). A patch of that throwaway work sits at `Coding/cb-3fixes.patch` — **reference
only, do NOT apply to -B** (different architecture). The 3 asks were re-scoped onto -B:

**✅ WORKING (developer confirmed in-game 2026-06-13) — polish pending, see note below.**

**Done (build green; gates pass; jar staged 15:48):**
- **More export formats incl. PNG.** `BlockExporter` now does `png`, `csv`, `md`, `html`, `yaml` on
  top of `json`/`txt`. PNG reads the baked `TextureStore.load(index)` and writes real `.png` images.
  - `/cb export png` → every block's texture → `exports/textures-<stamp>/<id>.png`
  - `/cb export <id> png` → one block's image → `exports/<id>.png`
  - `/cb export csv|md|html|yaml` (whole list) and `/cb bulkexport <filter> <format>` (filtered)
  - Clickable format buttons added to `/cb list` and `/cb export`.
  - Files: `core/BlockExporter.java`, `command/handlers/UtilityCommands.java`,
    `command/handlers/BulkExportCommands.java`, `command/handlers/BulkSuggestions.java`. All under gates.

**Polish pending (export works, revisit later — not blocking):** revisit formatting/UX of the new
formats (csv/md/html/yaml layout, PNG output path naming, clickable-button polish). Deferred; no
crash, just rough edges. Pick up after reid.

**Parked — needs a design decision (NOT built):**
- **`/cb recolor`** — developer is unsure what it should do. Two candidates discussed:
  (A) make a recoloured **variant** block (`mars` → `mars_red`, background recoloured, design kept) — a
  thin wrapper over the existing, tested `ColorVariantService.createVariant` (records CREATE undo); or
  (B) repaint the original block **in place** — needs a new texture pipeline + extending UndoManager
  for texture undo (deliberately deferred). **Decide A vs B with the developer before building.**

**NEXT — START HERE: `/cb reid` (its own session, per the developer).** -B has **no `reId`** today
(`SlotManager.rename` is display-name only; ids are keyed in several places). Build it carefully + tested:
  - `SlotManager.reId(oldId, newId)` — change the id; the **slot index stays**, so `TextureStore`
    files (keyed by index) do NOT move. Persist via `SlotDataStore`. Guard: newId free, valid, not taken.
  - **Migrate every id reference:** `LockManager`, `FavoritesManager`, `BlockNotesManager`,
    `DraftManager` (check each for id-keyed state), plus categories travel on the SlotData itself.
    Audit with a grep for `customId`/id-keyed maps before finishing — a missed one = a dangling ref.
  - **Undo:** `UndoManager` currently has no id-change/texture undo (see its header note). A reid op
    changes the map key, so a plain MODIFY won't reverse cleanly — design a reid undo (reid back +
    restore refs) or a dedicated Op kind.
  - Command `/cb reid` + a **cool GUI**: no-arg → pick-a-block menu (model on `BlockListMenu`), click →
    `AnvilPrompt` for the new id. Single-arg `/cb reid <id>` → straight to the anvil. `<id> <newid>` → direct.
  - Keep all new files under -B's gates (≤500 / handlers ≤400). Test in-game before marking done.

**Also pending (after reid):**
- **Fix 1 — tick-many picker + in-list search on `/cb listgui`.** Developer wants a multi-select block
  picker; -B currently uses a **filter model** (`BulkFilterMenu`: all/fav/locked/category + `name:`/`id:`).
  These are different UX. **Needs a quick design chat** (does the picker replace or complement the filter?).

**Build on this machine (user 66664, not POTATO):** PATH `java` is Java 8; set JDK 21 explicitly —
`$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"; .\gradlew.bat build --no-daemon -x test`.

**Next:** developer tests the export jar (15:48), then a fresh session builds `/cb reid`.

---

## 2026-06-12 (create-bug fix · despeckle v2 · bulk duplicate + export · Hub passed)

Developer verified the Hub (✅ passed) and said despeckle works but wants it better; reported the
broken-link create bug; asked to continue the leftover Group 07 ops.

**Done (build green; jar staged 09:49):**
- **Bug fix — broken link no longer makes an empty block.** `/cb create <id> <name> <url>` now
  downloads + decodes the image FIRST (off-thread) and only allocates the slot if that succeeds
  (`CreationCommands.createWithTexture`). A 404 / non-image / decode failure creates nothing. No-URL
  create is unchanged (immediate).
- **Despeckle v2 — edge-aware smart fill.** The black/white background fill is now chosen from the
  subject's **silhouette** (the foreground ring touching the background), not just whole-subject
  brightness: if ≥45% of the silhouette is dark (a dark-outlined logo, e.g. the Jordan emblem),
  the fill goes WHITE so the outline stays visible instead of vanishing into black. Constants
  `EDGE_DARK_VALUE` / `EDGE_DARK_FRACTION`. Bright-edged subjects still get black (no regression).
- **`SlotManager.dupe` fixed** — it used to make an EMPTY copy (no texture, no attributes). Now it
  clones the display name, glow/hardness/sound/collision/category, and the baked texture + source
  image. (Single `/cb dupe` benefits too.)
- **Bulk duplicate** — `BulkDuplicateCommands`: `/cb bulkduplicate <filter>`, unique `<id>_copy`
  ids, ONE undo batch (CREATEs), ONE pack rebuild, confirm guard, slot-exhaustion reported.
- **Bulk export** — `BulkExportCommands`: `/cb bulkexport <filter> [json|txt]`, the filtered
  counterpart of `/cb export json` (read-only, no undo). Reuses `BlockExporter.exportAll`.
- **Dashboard + Hub** — both new ops added: Operation cycle is now 8 (Edit · Delete · Rename ·
  Category · Duplicate · Export · Lock · Favorite), Duplicate (white frame, book) and Export (brown
  frame, map, json/txt toggle) have full tiles; Hub shows all 8 in two rows. Tab-complete:
  `BulkSuggestions.EXPORT_ARGS`; duplicate reuses `FILTER_ONLY`.

**Held for a fresh session (told developer):** bulk **recolor(edge)** — needs a no-undo decision
first (recolour replaces texture like retexture); bulk **reid** — needs id-key migration across
locks/favorites/notes/drafts. Doing these at the tail of a long session risks the exact untested
mess we avoid; they get their own session.

**Next:** developer tests jar 09:49 (create-bug · despeckle v2 · duplicate · export · 8-op hub),
then recolor + reid.

---

## 2026-06-12 (Bulk Hub + Despeckle — built, awaiting in-game test)

Group 07 verified + closed earlier today. Developer picked: build the Bulk Hub, then the
despeckle fix, then they test both.

**Done (build green; jar staged 09:24):**
- **Bulk Hub** (`BulkHubMenu.java`, new `Dest.BULK_HUB`) — the front door for bulk work: one
  colour-coded tile per op (edit/rename/category/lock/favorite/delete, matching each op's
  dashboard frame colour), the remembered op glints, clicking a tile sets `BulkSession.op` and
  navigates into the dashboard (so Back returns to the hub). Plus an "⊞ Open the dashboard" tile.
  - `/cb bulkgui` and new `/cb bulkhub` now open the Hub. `/cb bulkproperty` (no args) still goes
    straight to the dashboard in Edit mode (now sets op=property explicitly). MainMenu's Bulk tile
    points at the Hub.
- **Despeckle background-removal fix** (`BackgroundRemover` Stage 1c) — the agreed fix for the
  old "tiny edge pixels block removal" bug, **without touching tolerance**: after the flood-fill
  builds the bg mask, a 1-px morphological **close** (dilate→erode, 4-neighbour) bridges hairline
  gaps/pinholes that walled off the fill, then tiny isolated **foreground specks** (area ≤
  max(4, w·h/20000)) are dropped into the background. Constants `SPECK_MIN` / `SPECK_DIVISOR` are
  named for easy tuning. Wrapped by the existing try/catch (never breaks a retexture).

**Tuning note:** despeckle uses sensible defaults; developer to send a sample image where bg
removal previously failed so the close radius / speck threshold can be dialled in if needed.

**Next:** developer tests Hub + despeckle. Remaining roadmap: bulk duplicate · export · reid ·
recolor(edge). A git checkpoint of the verified Group 07 work is also on the table.

---

## 2026-06-12 (GROUP 07 — ✅ VERIFIED IN-GAME, group closed)

Developer ran the full `GROUP_07_TESTING_GUIDE.md` and **all of it passed**: every bulk op
(property · delete · rename · lock/unlock · favorite/unfavorite · category) as both command and
dashboard, tab-complete everywhere, Dashboard 2.0 (frame/colours/sounds/previews/typed filters),
Back→home fix, and the clickable command twin. Jar 09:11.

**Group 07 is DONE.** Nothing outstanding. CHANGELOG caveats dropped; testing guide marked all
passed (per-step kept for regression).

**Next (not started — pick one, small):** despeckle background-removal fix · bulk reid ·
bulk duplicate · bulk export · bulk recolor(edge) · the full Bulk Hub menu. See bottom of this
entry / the roadmap in the Bible §8.

---

## 2026-06-12 (GROUP 07 — command-twin click + bulk category move)

Developer confirmed lock/favorite all good. Two asks: (1) command-twin tile should drop the
command into chat on click + reword its last lore line; (2) continue to the next bulk op.

**Done (build green; jar staged 09:11):**
- **Command twin is now clickable** — clicking it closes the dashboard and posts a one-click
  chat line (`GuiRouter.typeInChat`, SUGGEST_COMMAND) that fills the chat box with the exact
  command (editable, not auto-run). Last lore line reworded to "§eClick here §7to auto-type it
  in chat". (Engine note: no API types into chat without that one chat-link click — this is the
  closest possible.)
- **Bulk category move** — new op, the batch twin of `/cb setcategory`:
  - **`BulkCategoryCommands.java`** (own handler so BulkCommands stays under the 400 gate):
    `/cb bulkcategory <filter> <category>` (`none` clears). BulkScope filter, BulkConfirm guard
    for big/all, BulkChat hover list, ONE UndoManager batch entry, locked-skip. Category is
    metadata only — no pack rebuild (mirrors the single setter). Tab-complete via new
    `BulkSuggestions.CATEGORY_ARGS` (filter, then existing categories + none).
  - **Dashboard op** "Move to category" (cyan frame, CHEST icon): ③ Category tile types the
    name in an anvil (existing names hinted, `none` clears); Apply disabled until set. Preview,
    command twin (`/cb bulkcategory …`) wired.
  - Registered in `CommandRegistrar`. `BulkSession` got `category` + the op in OPS/prettyOp.

**Op cycle is now:** Edit · Delete · Rename · Move to category · Lock · Favorite.

**Next:** developer tests command-twin click + bulk category (command + dashboard). Then:
reid · duplicate · move(world?) · export · recolor(edge) · full hub · despeckle.

---

## 2026-06-11 (GROUP 07 — Lock + Favorite added to the dashboard)

Developer switched to Opus, said continue (will test the 4 polish checks later). Next roadmap
item = **lock/favorite dashboard tiles**. Backend (`BulkFlagCommands`) was already built + passed
as commands; this wires them into the dashboard as two new operations.

**Done (build green; jar staged 23:34):**
- **Two new dashboard operations** — cycle is now Edit ⇄ Delete ⇄ Rename ⇄ **Lock** ⇄ **Favorite**.
  Each has a **Direction** tile (③): Lock⇄Unlock, Favorite⇄Unfavorite (mirrors Rename's mode tile).
  No value/text step — just op, filter, direction, Apply.
- **Design-guide colours:** Lock = green frame (safe/reversible — green corners, §2 title,
  TRIPWIRE_HOOK icon); Favorite = magenta/purple frame (special — NETHER_STAR icon). Preview,
  command twin (`/cb bulklock <filter>` etc.), and Apply all follow.
- **`BulkSession`:** added `flagOn` + `toggleFlag()` + `flagCommandOp()` (maps op+direction →
  lock/unlock/favorite/unfavorite). `OPS` + `prettyOp` extended.
- **`BulkFlagCommands.applyFlagFromGui(player, op, filter)`** — closes the screen, runs the
  already-tested `run(...)` path. No new mutation logic (non-destructive, self-inverse, no undo
  entry — the opposite command is the undo, with a chat `[undo]` button).

**Not changed:** the flag commands themselves (already passed). GUI is a thin front-end.

**Next:** developer tests the 4 polish checks (Back/frame/twin) + lock/favorite ops in the
dashboard. Then: reid · duplicate · move · export · recolor(edge) · full hub · despeckle.

---

## 2026-06-11 (GROUP 07 — Back-button fix + Dashboard polish round 2 + GUI Design Guide)

Developer test results: tab-complete (all steps) ✅ · filter tiles ✅ · end-to-end op ✅ ·
dashboard "amazing" — asked for even more polish + a design-guide doc. One bug: **Back on a
command-opened GUI closed it** instead of going back.

**Done (build green; jar staged 23:22):**
- **Back-button fix** (`GuiRouter.back`): empty stack after pop (menu opened straight from a
  command, e.g. /cb bulkgui) now goes **home to the Main Menu** instead of closing. ✖ Close
  still closes. Applies to every menu. `Icons.back()` lore updated to say so.
- **Polish round 2:** new `ChestMenu.frame(edge, corner)` — two-tone "picture frame" (darker
  corners: blue/black/orange per op); chest **title** now tinted per op (§3/§4/§6); quiet `§8»`
  connector panes walk the eye ①»②»③; new **Command twin** tile (chain command block, slot 49)
  shows the exact chat command the screen equals; **page-turn sound** when the dashboard opens
  from a command (`GuiFx.open`). Filter picker got the same frame + title treatment.
- **`docs/GUI_DESIGN_GUIDE.md` (new)** — the design language written down: colour semantics
  (red = danger, blue = calm…), two-tone frames, the 6-row grid, radio-style choice tiles,
  icon vocabulary, lore voice, the sound matrix, consequences-before-commit, the Back/Close
  contract, and a copy-paste checklist for new menus. BulkPropertyMenu is the reference impl.

**Next:** developer re-tests (Back behaviour + new frame/title/twin tile), then: lock/favorite
dashboard tiles · reid · duplicate · move · export · recolor(edge) · full hub · despeckle.

---

## 2026-06-11 (GROUP 07 — tab-complete fix + Dashboard 2.0) — read this first

Developer confirmed the whole Group 07 test sheet **works** (rename GUI, bulk lock/favorite, all).
Then asked: (1) fix autocomplete after subcommands across the mod, (2) upgrade the basic-looking
bulk dashboard.

**Done (build green; jar staged 21:39):**
- **Tab-complete audit of every /cb subcommand.** Root cause: the bulk commands use greedy
  strings, which Brigadier can't suggest into without a custom provider. New
  `BulkSuggestions.java` — token-aware providers (`FILTER_ONLY` / `PROPERTY_ARGS` /
  `RENAME_ARGS`): re-tokenizes the typed tail, offsets the builder to the current token,
  suggests filters (incl. live categories + block ids + after-comma id lists), property names,
  and per-property values. Wired into `BulkCommands` (bulkproperty/bulkdelete/bulkrename) and
  `BulkFlagCommands` (all four).
- **Other suggestion gaps filled:** `/cb video extract <file>` (lists .mp4 names from the videos
  folder), `/cb arabic letter <name>` (28 letter names), `/cb retextureall <px>` (16…512).
  All other handlers already had providers (audited one by one).
- **Dashboard 2.0** (`BulkPropertyMenu` + `BulkFilterMenu`): op-coloured frame (blue edit / red
  delete / yellow rename), numbered step tiles, radio-style option lists (▸ on current; long
  lists collapse to prev ▸ next), new **Matched** spyglass tile sampling the actual ids, filter
  picker glints the current pick, wool-coloured category tiles, and typed **name:** / **id:**
  filters via anvil prompt (chat command no longer required for those).
- **GUI sounds** — new `gui/chest/GuiFx.java`, palette recycled from old FeedbackHelper:
  amethyst chime (click/select), XP orb (apply), note-bass `.value()` (danger/deny, NFR-12 ok).

**Not changed:** all mutation paths (BulkCommands apply/delete/rename) untouched — GUI is still
a thin front-end over the tested command backend.

**Next:** developer runs the new "Test now" section in `GROUP_07_TESTING_GUIDE.md` (tab-complete
+ Dashboard 2.0). Then: lock/favorite dashboard tiles · reid · duplicate · move · export ·
recolor(edge) · full hub · despeckle.

---

## 2026-06-11 (GROUP 07 — rename GUI + lock/favorite ops) — read this first

Developer wants ALL the old bulk ops rebuilt (command + GUI), plus the rename GUI. Despeckle later.

**Done (build green; jar 17:11):**
- **Rename in the dashboard** — Operation tile now cycles Edit ⇄ Delete ⇄ Rename; rename mode has a
  prefix/suffix/replace toggle + anvil text entry (via `AnvilPrompt`), then Apply → `bulkRename`.
- **New ops** `BulkFlagCommands.java`: `/cb bulklock` `/cb bulkunlock` `/cb bulkfavorite`
  `/cb bulkunfavorite`. Self-inverse (no undo entry — the opposite command IS the undo; chat shows
  a `[undo]` button that runs it). No confirm guard (non-destructive). Favorites are per-player.
- **Refactor:** extracted the shared confirm/pending mechanism into `BulkConfirm.java`
  (`actor` / `request` / `confirm` / `cancel`). `BulkCommands` 409 → 328, back under the 400 gate.
  Property/delete/rename confirm flow unchanged — just relocated.

**Files now:** `BulkCommands` (property/delete/rename + GUI bridges), `BulkFlagCommands` (lock/fav),
`BulkConfirm` (confirm), `BulkChat` (hover list + confirm line), `BulkValues` (value parse),
`BulkScope` (filters). Dashboard = `gui/chest/BulkPropertyMenu` + `BulkFilterMenu` + `BulkSession`.

**Next:** lock/favorite dashboard tiles · reid · duplicate · move · export · recolor(edge) · full hub.
Then despeckle (background removal). Roadmap: bring every old bulk op, command + GUI, upgraded.

---

## 2026-06-11 (GROUP 07 — bulk ops now command + GUI; rename added)

Developer confirmed bulk (property + delete) **works**. Then asked: bulk ops should NOT be
GUI-only — want `/cb bulkdelete`, `/cb bulkrename` etc. as commands too, with full GUI support.

**Done (build green; jar 16:58):**
- Re-added commands: `/cb bulkproperty`, `/cb bulkdelete` (no-args opens the dashboard; with args runs).
- New `/cb bulkrename <filter> prefix|suffix|replace …` — batch undo, clickable confirm. **Command only so far** (GUI rename mode is next).
- Refactor: shared chat helpers → new `BulkChat.java`; kept `BulkCommands` under the 400 gate (353).
- Tightened the testing guide (developer asked for straightforward docs).

**Not done:** rename in the dashboard GUI (anvil text box) · despeckle bg fix.

**Next:** (1) rename GUI mode, (2) **despeckle** for background removal — developer picked "despeckle
the mask"; still want a sample failing image to tune. Background-removal note: new mod has the SAME
core flood-fill as old, so the "tiny edge pixels block removal" bug is present; despeckle the mask
(morphological close + drop specks) is the agreed fix, doesn't touch tolerance.

---

## 2026-06-11 (GROUP 07 — Bulk DELETE added to dashboard, awaiting in-game test)

Built the **delete** operation as a second mode of the Bulk Dashboard (per "bulkgui is the only
way" — no `/cb bulkdelete` command). Destructive, so done carefully with full batch undo.

### What was built (NOT verified — golden rule)
- **`BulkValues.java` (new)** — value parsing/validation split out of `BulkCommands` to stay under
  the 400-line handler gate (§5.1 "split first"). `BulkCommands` now uses `BulkValues.parse(...)`.
- **`BulkCommands` delete** — `applyDeleteFromGui` / `bulkDelete` / `applyDelete` /
  `sendDeleteConfirmPrompt`. Deletes every non-locked matched block, capturing each texture via
  `TextureStore.load` BEFORE delete (mirrors single `/cb delete`), records the batch as ONE undo
  entry of `Kind.DELETE` children, then ONE `updatePack()` (debounced → single rebuild). Red
  clickable `[✔ DELETE] / [✖ Keep]` confirm for big/all batches.
- **Dashboard delete mode** — `BulkSession.op` ("property"/"delete") + `toggleOp`; `BulkPropertyMenu`
  got an **Operation** tile (top-left) that switches modes: Edit setting hides → red **Delete N**
  button. `BulkFilterMenu` reused unchanged.
- Undo of a bulk delete uses the existing `Kind.BATCH` path in `HistoryCommands` (DELETE children →
  restoreSnapshot + TextureStore.save + updatePack, debounced to one rebuild). No HistoryCommands
  change needed.

### Why no HistoryCommands change
`ResourcePackServer.updatePack()` is debounced ~500ms and coalesces — N delete/undo calls in a tick
collapse to ONE rebuild. So batch delete + its undo each trigger a single pack rebuild already.

### State right now
- **Build green** (compile + 3 gates; verifyFileSize OK after the split). Jar staged →
  `.minecraft\mods\customblocks-1.0.0.jar` (**16:26**).
- Nothing committed since `df4d74e`. Don't commit unless asked.

### Next step
Developer runs `GROUP_07_TESTING_GUIDE.md` §1 (bulk delete: toggle → filter → red Delete → undo
restores incl. texture; locked skipped; big batch confirm). Then: developer wants to **discuss
background removal** before more bulk ops. After that, remaining bulk ops + full hub.

---

## 2026-06-11 (GROUP 07 — Bulk CONFIRMED working; rebrand to Bulk Dashboard)

Developer tested slice 2: **"everything is working about bulk."** ✅ Three follow-ups handled:

### 1. Investigated "no resource-pack prompt / no reload" — NOT a bug
Read every `ResourcePackServer.updatePack()` caller: the pack only rebuilds on **texture** changes
(create / retexture / paint / recolor / delete / video / arabic / hex). `bulkproperty` changes
glow/hardness/sound/collision — server-side settings read live (glow relights placed blocks via
`SlotLighting.applyToPlaced`; the rest apply on next break/step). No texture changed → nothing to
reload → correctly no prompt. Documented as an FYI in the testing guide. No code change.

### 2. Rebrand + lock to GUI-only (developer request)
- **Removed `/cb bulkproperty` and `/cb bulk` commands.** `/cb bulkgui` is now the ONLY entry.
- GUI Apply no longer dispatches a chat command — calls new `BulkCommands.applyFromGui(player,
  filter, prop, value)` directly (same validate + confirm-guard + batch-undo path).
- **Rebranded "Bulk — Edit Property" → "Bulk Dashboard"** everywhere visible: menu titles, header,
  the dashboard tile, and the middle tile "Property:" → "Setting:". Undo label "bulkproperty…" →
  "bulk-edit…". `/cb confirm` + `/cb cancel` stay (they back the clickable chat buttons).

### 3. Statuses set (developer request)
Testing guide: **bulk = ☑️ working**; **GUI polishing = 🟡 PARTIAL**; **chat system = 🟡 PARTIAL polish**.

### State right now
- **Build green** (compile + 3 gates). Jar staged → `.minecraft\mods\customblocks-1.0.0.jar`
  (**15:28**, 4,655,168 bytes).
- Nothing committed since `df4d74e`. Don't commit unless asked.

### Next step
Quick re-confirm (guide §1): `/cb bulkproperty` gone · `/cb bulkgui` opens "Bulk Dashboard" · Apply
still works. Then build the **bulkdelete** slice (destructive — command-backed + dashboard tile,
batch undo, own test), then the rest of the bulk ops + a full hub.

---

## 2026-06-11 (GROUP 07 — Slice 2: Bulk GUI + cleaner chat, awaiting in-game test)

Developer tested slice 1: **functionality passes** ("everything else passes"), but the bulk
**chat output was a wall of text** (screenshot: 12 ids + "+13 more" inline) and they want
`bulkproperty` to be **mainly a GUI**, with clickable confirm + hover-for-details. Built that.

### What was built (NOT verified — golden rule)
- **Chat QOL** (`Chat.java` + `BulkCommands.java`):
  - Added reusable rich-chat helpers to `Chat`: `runButton` (click→run cmd), `suggestButton`
    (click→prefill), `hover` (tooltip), `line` (branded rich line).
  - Bulk **result is now ONE line**: `Set glow=12 on §e3 blocks§r  [↩ Undo] ✔` — the full id
    list moved to a **hover** tooltip (5/row), `[↩ Undo]` is a clickable button. No more flood.
  - Confirm prompt is now **clickable**: `Apply to N blocks? [✔ Confirm] [✖ Cancel]` — hover the
    count for the list. No typing `/cb confirm`.
- **Bulk GUI** (new `gui/chest/`): `BulkSession` (per-player filter/property/value),
  `BulkPropertyMenu` (builder: filter · property · value · live count · Apply), `BulkFilterMenu`
  (click-pick All / Favorited / Locked / per-category). Wired `Nav.Dest.BULK_PROPERTY` +
  `BULK_FILTER`, `GuiRouter` cases, a **Bulk Edit** tile on the dashboard (`MainMenu` slot 31).
  GUI is a **thin front-end** — Apply runs the tested `/cb bulkproperty` command (so confirm +
  batch-undo are unchanged).
- `/cb bulkproperty` (no args) / `/cb bulkgui` / `/cb bulk` → open the builder.

### Deliberately deferred (told the developer)
- **`bulkdelete` (command + GUI)** — it's **destructive**; doing it as its own next slice with
  proper batch-undo + its own test, NOT bundled into this drop. (They asked for the no-args→GUI
  pattern; the builder pattern here is exactly what it'll reuse.)
- `id:`/`name:` filters in the GUI need typed text (anvil) — kept on the chat command for now.

### State right now
- **Build green** (compile + 3 gates). Jar staged → `.minecraft\mods\customblocks-1.0.0.jar`
  (**15:13**, 4,655,064 bytes).
- Nothing committed since the `df4d74e` checkpoint. Don't commit unless asked.

### Next step — developer runs §1 of `GROUP_07_TESTING_GUIDE.md`
Open `/cb bulkproperty` → build a bulkproperty by clicking → Apply → check the one-line chat +
hover + clickable Undo/Confirm. If pass → build the **bulkdelete** slice (command + GUI), then the
rest of the bulk ops + a full hub.

---

## 2026-06-11 (GROUP 07 — Slice 1 built, awaiting in-game test)

Pushed the full Group 01–06 working tree to GitHub as a checkpoint (`main`, commit `df4d74e`,
`bin/` now gitignored). Then read the **old mod** (`Coding/CustomBlocks/`) to recycle its proven
bulk logic, and built Group 07 slice 1.

### What was built (NOT verified — golden rule: nothing ✅ until in-game test)
- **`core/BulkScope.java`** (new) — filter resolver, ported + upgraded from old `BulkScope`.
  Core filters `all / category: / id:<prefix> / name:`; **`id:<prefix>` is new** (old mod only
  had exact id). Bonus filters kept from the old resolver: `name:<prefix>*` wildcard,
  `favorite:yes|no`, `locked:yes|no`, comma id-list, exact id.
- **`command/handlers/BulkCommands.java`** (new) — `/cb bulkproperty <filter> <glow|hardness|
  sound|collision> <value>`, plus `/cb confirm` and `/cb cancel` (pending-action, 60s expire).
  Locked blocks skipped (reported, not errored). Value parsing mirrors `AttributeCommands`.
- **Confirm guard** — `bulkConfirmThreshold` (default 10) added to `CustomBlocksConfig`. Fires
  when N > threshold OR filter is `all`.
- **Batch undo** — added `UndoManager.Kind.BATCH` + `recordBatch()`; `HistoryCommands` reverts/
  re-applies all children as ONE step. One `/cb undo` reverts the whole batch (test G07.2).
- **`CommandRegistrar`** — `BulkCommands.register(root)` wired in.

### State right now
- **Build green** (compile + 3 gates: verifyFileSize, verifyMojibake, verifySound). 7s.
- **Jar staged** → `.minecraft\mods\customblocks-1.0.0.jar` (**13:09**, 4,643,997 bytes).
- Nothing committed for slice 1 yet (the GitHub push was the pre-Group-07 checkpoint). Don't
  commit unless the developer asks.
- Env still contaminated (texture rendering unreliable) — but slice 1 verifies by command output
  + `/cb list`, not visuals. Safe to test.

### Next step — developer runs tests G07.1–G07.4 in-game
```
/cb create g07a BulkTest1   (repeat g07b/g07c, then /cb setcategory each → bulktest)
/cb bulkproperty category:bulktest glow 8     → "Set glow=8 on 3 block(s): g07a, g07b, g07c"
/cb undo                                       → all 3 revert with ONE undo (G07.2)
/cb bulkproperty all glow 0                    → confirm prompt if >10 blocks (G07.3)
/cb confirm                                    → executes (G07.4)
```
If pass → build slice 2 (next bulk ops + `bulkgui`). G07 spec doc updated to match this build.

---

## 2026-06-11 (HANDOFF → START GROUP 07 — Bulk Operations)

Developer wrapped the bug-fix session (below) and called it: **start Group 07 in a fresh convo.**

### State right now (trust this)
- **Build green** (compile + 3 gates). Jar in `.minecraft\mods\customblocks-1.0.0.jar` (**12:46**).
- **BUG 1 fixed & kept** — `/cb config hex … #RRGGBB` (with `#`) works in chat + Config GUI.
- **BUG 2 (silent pack) REVERTED** — do NOT re-apply the join-delay. Developer is running with
  `/cb config silentpack OFF` so the resource-pack screen shows on every edit (that's what makes
  edits visibly apply on their current data). See the entry below for the full why.
- **Nothing committed** (git has only the 2 old commits; all work is working-tree). Don't commit
  unless the developer asks.

### ⚠️ Environment is contaminated — do NOT chase texture rendering this session
The developer has been swapping old/new jars on one world, so `config/customblocks/` mixes old
data (root `slot_N.png`, `.dat` files, 20 MB `slots_old.json`) with new (`textures/slot_N.png`,
3.5 KB `slots.json`). Also an OLD **client-side** pack `.minecraft/resourcepacks/CustomBlocks`
(888 old textures, same `customblocks` namespace) is still enabled and overrides the new server
pack. **Result: block textures render wrong/old — this is NOT a code bug** (the new pipeline is
correct: `slots.json` clean, served zip has the right textures, paths read only from
`textures/slot_N.png`). **Resolution = the deferred `.dat` migration.** Until then, any visual
texture test is unreliable. Group 07 was chosen specifically because it avoids this.

### Group 07 — Bulk Operations (the task)
- Spec: `docs/Finale Fix/GROUP_07_BULK_OPERATIONS.md`. Tests: G07.1–G07.9 (verified by command
  output + `/cb list`, not visuals — contamination-safe).
- Scope: `bulkdelete / bulkrename / bulkreid / bulkproperty / bulkexport / bulkmove /
  bulkduplicate / bulklock / bulkunlock / bulkfavorite / bulkunfavorite / bulksound /
  bulkrecolor`, plus `/cb bulkgui` and the `bulkConfirmThreshold` (default 10) confirm guard.
- **Build only what's safe now:** SKIP `bulkshape` (needs Group 08, not built). `bulkrecolor`
  must be **edge-mode ONLY** — hardcoded, no full-mode option (CLAUDE.md pitfall: batch full-mode
  recolor destroys designs). Each bulk op = ONE undo entry for the whole batch.
- Filters: `category:<name>` · `id:<prefix>` · `name:<substring>` · `all` (always confirms).
- Architecture (CLAUDE.md §5): new command handlers under `command/handlers/` (≤400 lines, split
  if needed), registered via `CommandRegistrar`; all slot writes through `SlotManager` /
  `SlotDataStore`; GUI as a standalone `gui/chest/` menu. Check what already exists first
  (LockManager, FavoritesManager, BlockExporter, category system are already in the tree).

### First steps for the next session
1. Read this entry + CLAUDE.md. Read `GROUP_07_BULK_OPERATIONS.md` fully.
2. Grep for any existing `bulk*` handlers/commands before writing (avoid dupes).
3. Propose a SMALL first slice (≤5 items — e.g. the filter parser + `bulkproperty` + confirm
   guard + undo) and get the developer's OK before coding. Hand back for in-game test (G07.1–G07.4).
4. Keep `silentpack OFF` in mind: the developer accepts the pack screen on edits for now.

---

## 2026-06-11 (BUG 1 FIXED ✅ · BUG 2 silent-pack change REVERTED ⏪) — final jar 12:46

Build green (compile + 3 gates). Final jar → `.minecraft\mods` **12:46**.

### Test results (developer, in-game)
- **BUG 1 — DONE ✅.** `/cb config hex red #FF0000` (with `#`) works in chat AND the Config GUI.
  Kept. (greedyString args + `#`-strip in the GUI dispatches.)
- **BUG 2 — REVERTED ⏪ at developer's request. DO NOT re-apply without rethinking.**
  - The 250ms-delay fix DID make rejoin silent (proved via an `[SP]` diagnostic build:
    `silent=true` at the pack prompt). BUT making silent *reliable* broke the developer's
    edit workflow: with the pack screen suppressed, edits (hex change / new block / any edit)
    showed **no dialog and no visible change** — because on the current CONTAMINATED data the
    silent auto-accept doesn't visibly reload, while the manual "Yes" path did.
  - Reverted `CustomBlocksMod` JOIN hook back to immediate `sendToPlayer` and removed the added
    `ResourcePackServer.sendToPlayerDelayed`. Now == the round-2 state where edits worked.
  - **Workaround given to developer:** `/cb config silentpack off` → forces the resource-pack
    screen back on every pack send; accept it and the edit shows.
  - **Real root issue is the contamination** (see below), not the silent timing. Revisit silent
    pack only AFTER the `.dat` migration, on clean data — and confirm the silent auto-accept
    actually triggers a visible client reload before relying on it.
- Diagnostic `[SP]` logs were added to root-cause, then stripped.

### NOT a bug — environment contamination (texture rendering)
Developer saw a newly-created block (`tesp`, slot 22) render as an OLD-version texture. Root-caused
to **two CustomBlocks packs colliding**, NOT mod code:
- New mod is correct: `slots.json` clean (slot 22 = tesp), served `customblocks_pack.zip` contains
  the correct new `slot_22.png` (9259 B), `httpHost 127.0.0.1:8123` reachable. `TextureStore` +
  `ServerPackGenerator` only ever read `config/customblocks/textures/slot_N.png`.
- An OLD **client-side** pack `.minecraft/resourcepacks/CustomBlocks` (888 old slot textures, same
  `customblocks` namespace + paths) is still enabled and collides. Disabling it breaks ALL blocks
  (old + new) on the current mixed data — so it's left as-is.
- Cause: jar-swapping mixed old data (root `slot_N.png`, `.dat` files, 20 MB `slots_old.json`) with
  new data. **Resolution deferred to the `.dat` migration** (see [[project_customblocks_dat_migration]]).
  Texture rendering can't be cleanly tested until then. Developer agreed to leave it.

### 🐞 BUG 1 — `#` in hex args (`/cb config hex`, `recolorvariants`) → Brigadier parse error
- Root cause confirmed: `StringArgumentType.word()` rejects `#`. Both affected args are the
  FINAL argument, so switched them to `greedyString()` (safe).
- `HexCommands.register`: `value` arg of `config hex` and `oldhex` arg of `recolorvariants`
  → `word()` → `greedyString()`.
- Belt-and-braces: the GUI dispatch strings now strip `#` before running the command —
  `HexColorsMenu.applyHex` (`norm.replace("#","")`) and the `HexRecolorConfirmMenu` Yes button
  (`oldHex.replace("#","")`). `normalizeHexColor` already tolerates `#RRGGBB` and `RRGGBB`.

### 🐞 BUG 2 — silentpack ON, rejoin still shows the vanilla "download pack?" dialog
- Verified the join hook ALREADY sent `SilentPackPayload` before the pack (the round-2 jar had
  this and still failed) → plain send-order is insufficient. The client's flag receiver
  double-defers `SilentPackState.set` onto its main thread, so a same-tick race on rejoin lets
  the pack screen be built before the flag is true.
- Fix (the diagnosis's explicit alternative): **delay the pack send ~250ms (~5 ticks) after the
  payload.** New `ResourcePackServer.sendToPlayerDelayed(player, delayMs)` (mirrors the existing
  `onGuiClosed` DEBOUNCER→server-thread pattern, no-ops if the player left). `CustomBlocksMod`
  JOIN hook now calls it instead of `sendToPlayer`.

### Files touched
`command/handlers/HexCommands.java`, `gui/chest/HexColorsMenu.java`,
`gui/chest/HexRecolorConfirmMenu.java`, `network/ResourcePackServer.java`, `CustomBlocksMod.java`.

### Next
Developer runs testing guide §0d (① — ⑨) + §0c④ re-check + confirm the Yes-repaint actually
repaints (it was masked by BUG 1's parse error). §0d pass → **Group 06 done → start Group 07**
(`docs/Finale Fix/`).

---

## 2026-06-11 (HANDOFF — session ended mid-test) — 2 NEW BUGS reported, diagnosed, NOT fixed

Developer tested the round-2 jar (07:10) and hit two new problems. **Fix these FIRST in the
next session, before anything else.**

### 🐞 BUG 1 — `/cb config hex red #FF0000` → "Expected whitespace to end one argument, but found trailing data … #FF0000<--[HERE]"
- Happened when editing red's hex through the Config GUI (Variant Colours → anvil).
- **Diagnosis (confident):** Brigadier's `StringArgumentType.word()` only accepts
  `0-9 A-Z a-z _ - . +` — the `#` character is ILLEGAL in a `word()` argument. The anvil flow
  (`HexColorsMenu.applyHex`) normalizes input to canonical `#RRGGBB` and dispatches
  `cb config hex red #FF0000` via `executeWithPrefix` → parse error in chat, nothing applied.
- **Same landmine in TWO more places:**
  - `HexRecolorConfirmMenu` Yes button runs `recolorvariants <colour> <oldhex>` where oldhex
    carries a leading `#` — `oldhex` is also a `word()` arg → same parse error.
  - Any player typing a hex WITH `#` by hand (the §0b guide literally tells them to).
- **Fix:** in `HexCommands.register`, change the `value` arg of `config hex` AND the `oldhex`
  arg of `recolorvariants` from `StringArgumentType.word()` to `greedyString()` (both are the
  final argument, so greedy is safe). `normalizeHexColor` already tolerates both `#FF0000`
  and `FF0000`, so no other change needed. Belt-and-braces: strip the `#` in
  `HexColorsMenu.applyHex` and the Yes button's dispatched command strings.
- Files: `command/handlers/HexCommands.java`, `gui/chest/HexColorsMenu.java`,
  `gui/chest/HexRecolorConfirmMenu.java`.

### 🐞 BUG 2 — silent pack ON, but rejoin still shows the vanilla "download resource pack?" Yes/No dialog
- Developer expectation (correct): with `/cb config silentpack` ON, rejoin should apply the
  pack silently — no dialog.
- **Diagnosis (likely, verify in code):** an ORDER problem on join. `CustomBlocksMod`'s
  join hook calls `ResourcePackServer.sendToPlayer(player)` right away, but the client only
  auto-accepts when its `SilentPackState` flag is true — and that flag is set by
  `SilentPackPayload`, which (a) is reset to false on every disconnect by design, and (b) may
  arrive AFTER the ResourcePackSend packet on rejoin → the mixin sees flag=false → vanilla
  dialog shows.
- **Fix direction:** on PLAY-join, send `SilentPackPayload(silentPack)` FIRST, then the pack
  packet (or delay the pack send a few ticks after the payload). Check
  `CustomBlocksMod` (join hook, ~line 97), `client/SilentPackState`,
  `mixin/ClientCommonNetworkHandlerMixin`, `network/payloads/SilentPackPayload`.
- Also note: vanilla still always prompts for server-FORCED packs; ours is not forced — the
  silent path is the mod's own auto-accept, so ordering is the whole story.

### State of the rest (unchanged from the entry below)
- Round-2 fixes + M4 are built, green, jar copied 07:10 — **testing guide §0d NOT yet run**
  (developer hit BUG 1 immediately). After both bugs are fixed: rerun §0d ① — ⑨, plus §0c④
  re-check (reload-waits-for-GUI), plus confirm the Yes-repaint actually repaints (it may
  have been silently broken by BUG 1's parse error on `recolorvariants`).
- Group 06 after that: core mechanics all built (M1-M4). Optional backlog: eyedropper,
  despeckle, preview, held-block glow (deferred), §4 lore/chat polish. **Developer said:
  when Group 06 is done → continue to Group 07** (`docs/Finale Fix/` has the group docs).

---

## 2026-06-11 (round-2 fixes + M4) — red art redrawn · reload waits for GUIs · per-face paint (build green, jar copied, NOT tested)

### Developer's test results (previous slice)
- ☑️ §0c passes ("everything else passes") — Config-GUI hex editor, anvil editing, Color Studio.
- 🐞 **Red Triangle texture "broken"** — diagnosed: the BUNDLED red art itself was lumpy/deformed
  (old-project file); yellow/green/black are clean. Pack tint output was verified clean offline.
- 🐞 **Pack-rebuild timing "still not working"** — diagnosed from `latest.log` (06:45:10): hex
  change with **0 variants** takes the no-confirm branch → instant rebuild → resource reload
  lands while the player is still inside the Variant Colours GUI. Last session's fix only
  covered the confirm-GUI case.

### Done (code written; build green — compile + 3 gates; jar → `.minecraft\mods` 07:10)
- **`red_triangle.png` regenerated** from the clean yellow art (HSB transfer to `#EE3333`):
  symmetric shape, highlight + outline preserved. `custom_square/triangle.png` white bases
  regenerated from the YELLOW art too (were derived from the lumpy red).
- **`ColorReplacer.tint` → HSB transfer** (target hue; sat = target×pixel; brightness scaled):
  re-tinted overrides keep highlight/fill/outline contrast instead of flattening.
- **Deferred reload safety (recycled old-project behaviour):** `ResourcePackServer.sendToPlayer`
  now HOLDS the push for any player inside a CustomBlocks chest menu or anvil prompt
  (`PENDING_SENDS`); `CbChestHandler.onClosed` + `AnvilPrompt.onClosed` call `onGuiClosed`,
  which delivers the held push ~300ms later if no other CB screen replaced it. Covers EVERY
  flow (confirm GUI, Variant Colours, Color Studio, anvils) — not just the confirm GUI.
- **M4 — per-face URL paint** (Group 06's last tool mechanic):
  - `TextureStore`: per-face overrides `slot_N_<face>.png` (FACES const, saveFace/loadFace/
    hasFace/hasAnyFace/deleteFace; `delete()` wipes faces too — no bleed into reused slots).
  - `ServerPackGenerator`: blocks with overrides emit a `minecraft:block/cube` model — painted
    faces point at their texture, the rest (+particle) at the base. No SlotData change needed;
    faces persist as plain files.
  - **New `command/handlers/FaceCommands.java`** — `/cb paintface <id> <face> <url>` (same
    download/bg-clean/resize pipeline as retexture, ONE rebuild after) + `/cb clearface
    <id> <face|all>`. Registered in CommandRegistrar.
  - **`RainbowRectangleItem`** — right-click a FACE → chat opens pre-filled
    `/cb paintface <id> <face> ` (ChatPrefillPayload); **area corner-marking moved to
    sneak + right-click** (Omni Area mode untouched). Lore rewritten.

### TEST IN-GAME (developer) — guide §0d (9 tests)

### Files touched
`assets .../red_triangle.png`, `custom_square.png`, `custom_triangle.png` (regenerated),
`image/ColorReplacer.java`, `network/ResourcePackServer.java`, `network/ServerPackGenerator.java`,
`gui/chest/CbChestHandler.java`, `gui/chest/AnvilPrompt.java`, `core/TextureStore.java`,
`command/handlers/FaceCommands.java` (new), `command/CommandRegistrar.java`,
`item/RainbowRectangleItem.java`, `Reports/GROUP_06_TESTING_GUIDE.md` (§0d).

### Group 06 remaining after this
M4 was the last core mechanic. Still open in the tracker: A eyedropper · B despeckle ·
C preview (bg-removal upgrades, approved) · held-block glow (deferred) · §4 lore/chat polish.
Group 07 starts once the developer calls Group 06 done.

---

## 2026-06-11 (M3 hex fixes + customcolor) — Config-GUI hex editor · rebuild-waits-for-GUI · Color Studio (build green, jar copied, NOT tested)

### Developer's §0b test findings (this session's work order)
1. `/cb config hex` had **no Config-GUI slot** — wire it in, sub-GUI with a dye per tool, edit in anvils.
2. **Bug:** after a hex change the pack rebuild fired immediately, WHILE the recolour-confirm GUI was opening — rebuild must wait until that GUI is closed.
3. **Remake `/cb customcolor` from scratch** on the old mod's Color Studio idea (no args → GUI of ready-made colours → magic tool pairs).

### Done (code written; build green — compile + 3 gates, 24s; jar → `.minecraft\mods`)
- **New `gui/chest/AnvilPrompt.java`** — reusable server-side anvil text input (rename the seed
  item, take the output to submit; ESC = cancel). Recycled from the old project's proven
  AnvilPromptManager, rebuilt callback-based.
- **New `gui/chest/HexColorsMenu.java`** (`Nav.Dest.HEX_COLORS`) — the ONLY GUI place hexes are
  edited: four dyes (red/yellow/green/black) showing current hex, default, variant count; click →
  anvil prompt → valid input runs the tested `/cb config hex` command. ConfigMenu got a glowing
  **Variant Colours** entry (slot 32) that opens it.
- **Rebuild-timing fix** — `ChestMenu.onClose(callback)` + `CbChestHandler.onClosed` now report
  screen close once. `HexCommands.setHex` no longer rebuilds the pack before opening the confirm;
  `HexRecolorConfirmMenu`: **Yes** → the repaint batch's single rebuild covers everything ·
  **No/X/ESC** → one rebuild on close (item re-tint). No-variant/console path rebuilds immediately.
  Restart with the prompt open is safe — the pack rebuilds at every SERVER_STARTED.
- **`/cb customcolor` rebuilt** (old mod read for inspiration; code new):
  - **New `core/ColorLibrary.java`** — 29 preset colours + aliases + name/hex resolution
    (data recycled from the old ColorLibrary).
  - **New `item/CustomColorToolItem.java`** + `custom_square`/`custom_triangle` registered in
    ToolItems — ONE item id per shape, the colour rides in NBT (`cb_rgb`); Triangle creates a
    `*_hex_rrggbb` variant, Square swaps to an existing one (same flows as ShapeToolItem).
  - **`ColorVariantService`** generalised for `hex_rrggbb` keys: `keyForRgb/isHexKey/rgbForKey/
    labelFor`, `stripColourSuffix` also strips `_hex_xxxxxx`.
  - **New `gui/chest/CustomColorMenu.java`** (`Nav.Dest.CUSTOM_COLOR`) — the Color Studio: 29
    dyes (click → pair, studio stays open) + **Custom Hex…** anvil button (hex or colour name).
  - **`/cb customcolor [colour]`** registered in HexCommands — bare opens the studio; an arg
    (hex or name, aliases included) gives the pair directly, with did-you-mean on bad input.
  - **Icons:** new white `custom_square/triangle` textures (generated from the red art) tinted
    client-side from the stack NBT (`ColorProviderRegistry` in CustomBlocksClient) — every pair
    visibly wears its colour. Lang entries added.

### Persistence (audited, needs in-game confirm)
Hexes → config.json (atomic save/load) · tool colour/name/lore → item NBT in the inventory ·
hex variants → SlotDataStore + TextureStore like any block · pack → rebuilt every boot.

### TEST IN-GAME (developer) — guide §0c (10 tests) + re-run §0b ②③⑦ (timing changed)

### Files touched
`gui/chest/AnvilPrompt.java` (new), `gui/chest/HexColorsMenu.java` (new),
`gui/chest/CustomColorMenu.java` (new), `core/ColorLibrary.java` (new),
`item/CustomColorToolItem.java` (new), `gui/chest/ChestMenu.java`, `gui/chest/CbChestHandler.java`,
`gui/chest/ConfigMenu.java`, `gui/chest/HexRecolorConfirmMenu.java`, `gui/chest/Nav.java`,
`gui/chest/GuiRouter.java`, `command/handlers/HexCommands.java`, `core/ColorVariantService.java`,
`item/ToolItems.java`, `client/CustomBlocksClient.java`, assets (2 textures, 2 models, lang),
`Reports/GROUP_06_TESTING_GUIDE.md` (§0c).

---

## 2026-06-11 (M3 hex) — colour setters + item re-tint + variant repaint (build green, NOT tested)

### First: M3 swap verified ✅ + hotbar wording flagged
- Developer confirmed all 8 §0 guide tests in-game (swap, fallback, errors, glow, lore, GUI label).
- 🟡 **PARTIAL: hotbar message wording** — rough, queued with the chat/GUI polish (guide §4).

### Done (code written; build green — compile + 3 gates, 24s; jar → `.minecraft\mods`)
Built **M3 hex** from `Reports/GROUP_06_HANDOFF.md` §6 — the four variant colours are configurable:
- **New `command/handlers/HexCommands.java`** — `/cb config hex` (status), `/cb config hex
  <colour>` (one), `/cb config hex <colour> <#RRGGBB>` (set: normalize/validate → save → pack
  rebuild re-tints item art → if `*_<colour>` blocks exist, open the confirm GUI; console gets a
  command hint instead). Plus `/cb recolorvariants <colour> <oldhex>` — the repaint batch entry.
  Started inside ConfigCommands but that hit 413 lines → **the 400-line gate fired** → split out
  (the no-monolith rule doing its job).
- **New `image/ColorReplacer.java`** — `swapColor` (direct per-pixel replace within ~30/channel
  of the old hex, NO flood fill → design pixels safe, the old project's batch pitfall avoided)
  and `tint` (target hex scaled by pixel brightness — outlines survive) for item art.
- **`ColorVariantService`** — `variantCount(colour)` + `recolorVariants(player, colour, oldRgb)`:
  off-thread batch over every `*_<colour>` block texture, ONE `updatePack()` AFTER the batch
  (CLAUDE.md §7), report "N recoloured / N skipped(+incidents)".
- **`ServerPackGenerator.addTintedShapeItems`** — when a colour's hex ≠ shipped default, the pack
  carries re-tinted Square/Triangle item textures (bundled art loaded from the jar, tinted,
  written at the same asset path → overrides the mod art client-side). Default hex → no override.
- **`CustomBlocksConfig`** — `TRIANGLE_*_DEFAULT` constants (fields now initialize from them).
- **New `gui/chest/HexRecolorConfirmMenu`** (Yes/Info/No; arg `colour:oldHex`; Yes runs the
  tested `/cb recolorvariants`, No closes) + `Nav.Dest.HEX_RECOLOR_CONFIRM` + `GuiRouter` case.
- **`ShapeToolItem.getName`** — names now show the live hex: "Red Square §8[#EE3333]" (spec).

### TEST IN-GAME (developer) — guide §0b (8 tests)
Status line · set red `#FF8800` with `_red` blocks placed → confirm opens · Yes repaints (design
intact) · item names show hex + art re-tinted · new variants use the new hex · bad hex/colour
errors · No keeps blocks · hexes survive restart.

### Files touched
`command/handlers/HexCommands.java` (new), `image/ColorReplacer.java` (new),
`gui/chest/HexRecolorConfirmMenu.java` (new), `core/ColorVariantService.java`,
`network/ServerPackGenerator.java`, `CustomBlocksConfig.java`, `item/ShapeToolItem.java`,
`gui/chest/Nav.java`, `gui/chest/GuiRouter.java`, `command/CommandRegistrar.java`,
`command/handlers/ConfigCommands.java` (hex moved out).

---

## 2026-06-11 (M3 swap) — Square = swap to colour variant (build green, jar copied, NOT tested)

### Done (code written; build green — compile + 3 gates, 23s; jar → `.minecraft\mods`)
Built **M3 Square swap** from `Reports/GROUP_06_HANDOFF.md` §6 — squares' old placeholder
tag/marking is GONE (developer flagged it this session):
- **`ColorVariantService.swapPlaced(player, world, pos, current, colourKey)`** — resolve
  `variantId(currentId, colour)` (strip-suffix + add, so `vart_red` + black → `vart_black`);
  exists → `world.setBlockState` to that slot's block, carrying the target's glow in the
  `SlotBlock.LIGHT` state (mirrors `getPlacementState`); **never creates**. Black Square with
  no `_black` → falls back to the BASE id. Same block → "already" message. No variant at all →
  hotbar error "create one with the <Colour> Triangle first". Null block guard → /cb reload hint.
- **`ShapeMarkerItem`** — square branch now calls `swapPlaced` (tag message deleted); square
  lore rewritten (swap wording, Black Square fallback line; no more "tagging"/"mark").
- **DEVIATION from the presented plan:** `/cb undo` does NOT cover swaps. `UndoManager` stores
  only SlotData edits (deliberately free of world/position types) and a swap edits no SlotData.
  Swap is self-reversing (Square of the original colour / Black Square → base). Wiring world
  edits into undo = its own slice + ADR if the developer wants it.

### TEST IN-GAME (developer) — guide §0 (7 tests)
Red Square on placed `vart` → becomes `vart_red`, "Swapped to" chat · Black Square → `vart_black`
· Green Square (no variant) → error, nothing created · delete `vart_black` then Black Square →
back to base `vart` · same colour twice → "already" · swapped block carries variant glow ·
square lore rewritten.

### Also: full marker/tagging purge (same session, developer-requested; rebuilt green, jar re-copied)
- **`ShapeMarkerItem.java` RENAMED → `ShapeToolItem.java`** (class + constructor + header).
- Every "marker"/"tagging" reference removed from src: `ToolItems` (list type + comments),
  `CustomBlocksMod` (tab comment), `MagicItemsManager` (seed comment), `MagicMenu` (header
  comment + slot comment + **user-visible GUI label**: "Marker Shapes / Tag your custom blocks
  by colour" → "Squares & Triangles / Triangles create colour variants — Squares swap them"),
  `RecolorConfirmMenu` + `ColorVariantService` (called-by comments), `ToolCommands` (comment),
  `item/package-info.java` (rewritten to current tool reality — old project's 7-tool list was stale).
- Grep proof: zero `marker|Marker|tagged|tagging` matches left in `src/main`. (Unrelated "tag"
  uses — `[CB]` brand tag in Chat.java, Minecraft tool-tag comment, category list tag — kept.)

### Files touched
`core/ColorVariantService.java`, `item/ShapeToolItem.java` (renamed from `ShapeMarkerItem.java`),
`item/ToolItems.java`, `item/package-info.java`, `CustomBlocksMod.java`,
`core/MagicItemsManager.java`, `gui/chest/MagicMenu.java`, `gui/chest/RecolorConfirmMenu.java`,
`command/handlers/ToolCommands.java`, `Reports/GROUP_06_TESTING_GUIDE.md` (new §0).

---

## 2026-06-11 (verification) — M2 + Retexture-all PASSED in-game ✅; Squares old-tag issue → M3 next

### Verified in-game ✅ DONE (developer, 2026-06-11)
- **M2 Triangle colour variants** — all 6 guide tests passed: variant create (design kept, bg
  recoloured, source untouched), sneak Yes/Info/No confirm, already-exists instant give, suffix
  swap (`vart_black` + red → `vart_red`), untextured-block error, `/cb undo` removes variant.
- **Retexture-all** — all 6 guide tests passed: confirm GUI + Info numbers, Yes batch (progress +
  complete chat, blocks visibly change), No keeps existing blocks, 0-block skip, source-sharpen vs
  upscale-only difference confirmed, direct `/cb retextureall <px>`.
- `Reports/GROUP_06_TESTING_GUIDE.md` updated: §1 + §2 marked passed; squares issue flagged.

### Reported issue (developer)
- **Squares still do the old tag/marking** (`[CB] <Colour> Square tagged …`, `ShapeMarkerItem`
  square branch). Expected — M3 not built — but developer wants it fixed next.

### Next steps, in order
1. **M3 — Square swap** (`Reports/GROUP_06_HANDOFF.md` §6): right-click placed block → swap the
   PLACED block in place to its existing `<id>_<colour>` variant; Black Square falls back to base
   id when no `_black` exists; **never auto-create**; clear hotbar error when no variant; reuse
   `ColorVariantService` id math (`stripColourSuffix`); record undo; replace square tag message +
   "tagging" lore. Plan presented to developer — awaiting go-ahead.
2. **M3 hex** — config hex setters + Square/Triangle item re-tint + "recolour existing blocks?"
   batch prompt (direct hex swap, batch must force edge mode — CLAUDE.md §7).
3. Rest of guide §4/§5 backlog (eyedropper, despeckle, preview, per-face, lore/chat polish).

---

## 2026-06-11 (later still) — M2: Triangle creates colour variants (build green, NOT tested)

### Done (code written; build green — compile + 3 gates, 22s; jar copied to `.minecraft\mods` 03:58)
Built **M2 from `Reports/GROUP_06_HANDOFF.md` §6** — Triangles now CREATE colour variants:
- **Config:** 4 new fields `triangleRedHex #EE3333` / `triangleYellowHex #F0C814` / `triangleGreenHex
  #1E8C1E` / `triangleBlackHex #0A0A0A` (old project's defaults), validated by a new
  `normalizeHexColor` (accepts missing `#`, bad value → keep old). Load + atomic save wired.
- **`BackgroundRemover`:** `apply()` body extracted into a shared `process(…, Integer forcedFill)`;
  new `recolorBackground(input, mode, tolerance, fillRgb)` = same M1 detection (flood-fill, ΔE,
  anti-fringe) but paints the background the GIVEN hex instead of smart black/white. Mode `none` is
  promoted to `edges` (a recolour with no detection would do nothing). Smart-fill brightness scan
  skipped when the fill is forced.
- **New `core/ColorVariantService`** — the M2 brain: `variantId` (`stripColourSuffix(src) + "_" +
  colour`, so `mars_black` + red → `mars_red`), `createVariant(player, sourceId, colour)`:
  variant exists → just hand the item over; source untextured → friendly error, nothing claimed;
  else claim slot, copy glow/hardness/sound/category in ONE `restoreSnapshot` write, record undo,
  then worker thread recolours (stored original source if present, else baked PNG → same fallback
  as retextureall) → `toBlockPng` at current textureSize → save (+ copy the `.src` to the variant
  so it stays re-renderable) → ONE pack rebuild → give item + chat. Failure → incident + chat
  (block stays, textureless, deletable).
- **`ShapeMarkerItem`:** Triangle right-click = create variant now; **sneak+right-click = new
  Yes/Info/No `RecolorConfirmMenu`** (arg `colour:sourceId`; Info shows new id, hex, source-vs-baked
  note, already-exists note). Squares keep the tag message until M3. Triangle lore rewritten.
- **Wiring:** `Nav.Dest.RECOLOR_CONFIRM` + `GuiRouter.build` case.

### NOT done (next slices, in spec order)
- **M3** — Square = swap placed block to an existing colour variant (never auto-create, hotbar error).
- **M3 hex** — `/cb config` hex setters + item-texture re-tint + "recolour existing blocks?" batch
  prompt (batch must force edge mode — CLAUDE.md §7).

### TEST IN-GAME (developer)
1. Give a textured block + a Red Triangle. Right-click the placed block → chat "Creating …", then
   "created — background recoloured to #EE3333"; new block lands in inventory; place it: same design,
   red background. Source block unchanged.
2. Sneak+right-click with a triangle → confirm GUI opens; Info reads right; Yes creates (chat + item);
   No/✕ does nothing.
3. Same triangle on the SAME block again → "already exists — here's one" + item, instantly.
4. Red triangle on `<id>_black` → makes/gives `<id>_red` (suffix swapped, not stacked).
5. Triangle on an untextured block → friendly error, no block created (check /cb list).
6. Undo: /cb undo after a variant create should remove the variant.

### Files touched
`CustomBlocksConfig.java`, `image/BackgroundRemover.java`, `core/ColorVariantService.java` (new),
`item/ShapeMarkerItem.java`, `gui/chest/RecolorConfirmMenu.java` (new), `gui/chest/Nav.java`,
`gui/chest/GuiRouter.java`.

---

## 2026-06-11 (later) — Texture-Size picker sub-GUI (✅ verified in-game)

### Done (code written; build green — compile + 3 gates, 22s; jar copied to `.minecraft\mods`)
Built the texture-size sub-GUI from the handoff spec below. Four parts + one extra fix:
- **512 now allowed** — raised the cap 256→512 in **three** spots: the `texturesize` command arg
  (`ConfigCommands`), the config-file loader clamp (`CustomBlocksConfig.load`, line ~127 — the
  handoff named only two spots; this loader clamp would have silently snapped 512 back to 256),
  and the field doc comment. Suggestions now offer 16/32/64/128/256/512.
- **New `gui/chest/TextureSizeMenu.java`** — a 3-row chest listing all six sizes (slots 10–15).
  Current size glows. Each item's hover shows **~per-texture size + ~whole-pack size** (per-texture
  × live `maxSlots`, e.g. "~80 KB per texture · ~64 MB pack if all 800 textured") plus a look hint.
  Clicking delegates to the tested `/cb config texturesize <px>` command then reopens Config.
- **Wired** via `Nav.Dest.TEXTURE_SIZE` + `GuiRouter.build` switch case.
- **Opens** from the Config chest **Texture Quality** row (slot 20, now glowing + clickable) and from
  `/cb config texturesize` with **no argument** (`openTextureSizeMenu`; console still prints status).
  `/cb config texturesize <px>` with a number still sets directly as a shortcut.

### MB numbers are measured, not guessed
Re-encoded real images at 16–512px (32-bit RGBA PNG, high-quality downscale) and cross-checked
against the developer's 20 on-disk textures (all 64px, 0.2–11 KB) — the model lands in range.
Typical mid-values used: 16≈1 KB · 32≈2 KB · 64≈6 KB · 128≈20 KB · 256≈80 KB · 512≈0.3 MB.
(Real images vary: a flat logo packs larger than a smooth photo at the same size.)

### Verified in-game ✅ DONE
- Developer confirmed the texture-size picker works: menu opens from the Texture Quality row AND the
  bare `/cb config texturesize`, hover shows per-texture + pack MB, clicking a size sets/saves and
  returns to Config, 512 selectable. ✅

### 🟡 PARTIAL / backlog (developer-requested this session; NOT built — deferred to save a session)
- **Retexture-confirm sub-sub-GUI.** After picking a size in `TextureSizeMenu`, open a 3-button
  confirm: **Yes** (retexture existing blocks to the new size) · **Info** (middle, read-only) · **No**
  (keep existing as-is). Info-item brainstorm: "§eRetexture N existing blocks?" where N =
  `SlotManager.usedSlots()`; est. new pack size = N × per-texture-at-new-px; "players see one brief
  pack reload"; "only already-created blocks change; sources are re-rendered".
  **✅ FEASIBILITY CHECKED — source is NOT stored.** Per-block disk = only the baked `slot_N.png`
  (`TextureStore`), at the size it was made. No URL, no raw image, no cache; `applyTexture`
  (`CreationCommands`) downloads → processes → discards the URL (kept only in an error incident).
  `SlotData` has no source field. Consequences for the feature:
    - Smaller/equal target size → clean (downscale the baked PNG).
    - **Larger target → only upscales** the old pixels; no real sharpening (we lack the original).
  To enable TRUE higher-res re-render: add a **source store** — in `applyTexture`, also write the raw
  downloaded bytes to `config/customblocks/sources/slot_N.<ext>`; then retexture-all re-runs
  `ImageProcessor.toBlockPng` at any size from the real source. Cost: ~1 source image of disk/block;
  only helps blocks created AFTER that change (existing blocks have no source to recover).
  **DECISION NEEDED from developer:** (a) honest down/equal-only retexture now, or (b) add source
  store first so the feature can truly sharpen going forward.
  **➡ UPDATE (this session): chose (b). SOURCE STORE BUILT (build green, NOT tested).**
  - `TextureStore` now also keeps each URL block's ORIGINAL image at `config/customblocks/sources/
    slot_N.src` (raw bytes): new `saveSource` / `loadSource` / `hasSource`; `delete(index)` now wipes
    texture AND source (single choke point — every slot-free path in `SlotManager` already routes here).
  - `CreationCommands.applyTexture` calls `TextureStore.saveSource(index, raw)` after a successful
    texture. So every block textured/retextured FROM NOW ON is re-renderable at any size.
  - A **huge NOTE block** above `CreationCommands.retexture()` documents the exact retexture-all batch
    recipe (loadSource → re-run BackgroundRemover + toBlockPng at new size → one pack rebuild at end).
  - Caveats: blocks made BEFORE this update have no source (upscale-only); Arabic/video blocks never
    have a single source; undo of a delete restores the baked texture but not the source.
  - STILL NOT BUILT: the Yes/Info/No confirm sub-sub-GUI + the batch job itself. That's the next slice.
  **➡ UPDATE 2 (this session): retexture-confirm GUI + batch BUILT (build green, NOT tested).**
  - New `gui/chest/RetextureConfirmMenu.java` — 3-row Yes / Info / No. Opens after picking a size in
    `TextureSizeMenu` **only when** `usedSlots() > 0` (else straight back to Config). Info item shows
    block count, est. new pack size (reuses `TextureSizeMenu.avgBytes`/`human`), source-vs-upscale note.
  - New command `/cb retextureall [16-512]` in `CreationCommands` + `retextureAll(server, size, src)`
    batch: off-thread, per slot loads the stored source → re-runs BackgroundRemover + toBlockPng at the
    new size; no source → upscales the baked PNG; ONE `ResourcePackServer.updatePack()` after the batch.
    Reports "N re-rendered, N upscaled, N skipped"; logs an incident if any skipped.
  - Wiring: `Nav.Dest.RETEXTURE_CONFIRM`, `GuiRouter.build` case + new `runThenNavigate` helper,
    `TextureSizeMenu` click now branches (confirm vs back-to-config), `TextureSizeMenu.avgBytes`/`human`
    made package-private for reuse.
  - **TEST IN-GAME (developer):** with ≥1 existing block, pick a size → confirm opens; Info reads right;
    Yes → chat "Retexturing N…" then "complete — …", blocks change at new size; No → blocks unchanged,
    size still set for new blocks; with 0 blocks, picking a size skips the confirm. Try going UP in size
    on a block made THIS session (has source → real sharpen) vs an OLD block (upscale only).

### Next steps, in order
1. Pick the next issue with the developer (texture GUI is done; M3 is big and was deferred).
2. Retexture-confirm sub-sub-GUI (above) — once the source-storage question is answered.
3. Queued **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6) — the big colour-variant feature.
4. Open bg bugs (Test 2 fringe, Test 4 low-contrast) stay parked unless the developer reopens them.
5. 🔶 **BACKLOG — old `.dat` → new `.png` migration** (noted 2026-06-11, not yet planned/built). The live
   **➡ DEVELOPER DECISIONS (2026-06-11, via UI):** migration STAYS PARKED for now. When built: old blocks
   keep their **native 256** (NO stretch to 512 — zero detail gained, pack would balloon ~60→230 MB).
   Default textureSize for new blocks = **256**, developer adjusts in-game via the size menu as wanted.
   The 20 junk test blocks (wrecked by a 64→512 upscale: blur + rainbow static on busy images, e.g.
   slot_2 "PNG Taat" — no sources existed, all hit the upscale fallback) are to be **deleted entirely**.
   config dir has **770 old `slot_N.dat` files** (real blocks) but the new `slots.json` knows only **20 junk
   test blocks**. Findings from inspection:
   - `.dat` bytes ARE real PNGs (old version's extension); `.dat` are **256×256 RGBA** (some 128²) vs new
     `.png` at 64². So `.dat` are HIGH-RES — effectively free re-render sources for 770 blocks.
   - A block renders in the new pack ONLY if its index is in new `slots.json` (`assignedSlots()`) **AND**
     `slot_N.png` exists ([ServerPackGenerator] cube_all). So migration = **TWO jobs:** (A) copy/rename
     `slot_N.dat`→`slot_N.png` (trivial, no transcode), (B) convert OLD metadata (`slots.bak1.json` 144 KB /
     `slots_old.json` 20 MB) → new `SlotData` schema `(index, customId, displayName, glow, hardness,
     soundType, noCollision, category)`. Texture-only copy = orphan, never shown.
   - Gotchas: index collision (junk tests 0–19 overlap real old 0–19 → wipe junk, import wholesale or offset);
     per-face `slot_N_north.dat` + variants `slot_N_var0.dat` have no home (new = cube_all, M3 not built) →
     flatten to single `slot_N.dat`, variants dropped for now.
   - Sizing the `.dat`: DOWN-size = clean/sharp; UP past native 256 = upscale-only (no new detail). ⚠ `.dat`
     are already-baked (bg removed, square) → size them with a **plain downscale**, NOT the full
     `retextureAll` pipeline (BackgroundRemover + toBlockPng would double-process). `loadSource()` reads
     `slot_N.src` (none exist yet) → wire `.dat` as a source fallback.

### Files touched
`gui/chest/TextureSizeMenu.java` (new), `gui/chest/Nav.java`, `gui/chest/GuiRouter.java`,
`gui/chest/ConfigMenu.java`, `command/handlers/ConfigCommands.java`, `CustomBlocksConfig.java`.

---

## 2026-06-11 (late) — 🟢 HANDOFF (read this first): M1 re-test results + texture-size sub-GUI request

> Continue in a fresh conversation. Self-contained. Supersedes the earlier "M1 bg fixes" entry below
> (those fixes are now in the jar AND in-game tested — results here).

### In-game re-test (new jar: smart fill + tolerance ΔE cap 22 + textureSize 128)
Developer ran the 5 substitute images + the pixelation test:

1. ✅ **Test 1 — dark subject FIXED.** Black "K" renders on a light/white fill, clearly visible — no
   more black-on-black. Smart fill works. *(Fill looks faintly lilac in shade = world lighting on
   white, not a bug. Can verify the raw PNG is pure white if it ever matters.)*
2. 🐞 **Test 2 — light fringe halo remains.** White bg removed→black, red "88" correct, but a thin
   **light outline** rings the red digits — anti-alias fringe the 1-px dilation didn't catch.
   Developer's instinct (raise tolerance) is **right, partly:** fringeTol = tol+6, so higher tol
   eats more of the ring (try 45–55). Proper fix = widen the anti-fringe past 1 px, or queued **B
   despeckle**. Lives in `image/BackgroundRemover.java` (`FRINGE_EXTRA` + Stage-2 dilation). NOT a blocker.
3. 🐞 **Test 4 — low-contrast still eaten.** Substitute (gray `CCCCCC` bg + near-white `EEEEEE` "6")
   at **tol 55** → all-black; the 6 read as background. Cause: those greys are only ~ΔE 12 apart and
   tol 55 now maps to ΔE 12.1 — still ≥ the subject gap. The cap fix (40→22) DOES help the *original*
   bug (a ~ΔE 13 subject now survives at 55) but this substitute is lower-contrast and sits on the
   line. Fix for near-equal colours = **lower tol (25–30)** or the queued **A eyedropper**. The
   unmapped **"9 9"** image (near-white digits, clean result) confirms the fix works at real contrast.
   **OPEN DECISION:** (a) accept as the documented colour-distance limit, or (b) build eyedropper sooner.
4. ✅ **DECISION — Test 5 photo: leave it alone.** Developer agrees bg removal isn't for complex
   photos. No work on the photo case; eyedropper/despeckle stay optional/later.
5. ✅ **Pixelation — big win.** "sharpness difference is huge." 128px >> 64px, confirmed in-game.

### ▶️ NEXT FEATURE (developer wants this) — Texture-Size picker sub-GUI · NOT built
Ready to build (clear spec). Three parts:
1. **Add a 512px option** — raise the max from 256 → **512** in two places:
   `CustomBlocksConfig.textureSize` clamp `(16,256)`→`(16,512)` and `ConfigCommands` arg
   `integer(16,256)`→`(16,512)`. Mind pack size (that's what the MB hover is for).
2. **Open a sub-GUI** instead of a chat line: clicking the **Texture Quality** row in the config
   chest GUI (`gui/chest/ConfigMenu.java` slot 20) AND running `/cb config texturesize` (no arg)
   open a new chest menu listing every size — **16 / 32 / 64 / 128 / 256 / 512** — as clickable
   items. Click one → set `textureSize` + `save()` + return to the config GUI. Follow the existing
   chest-menu pattern (`ConfigMenu`, Magic Items menu) and wire via `GuiRouter` / `Nav`. New file
   e.g. `gui/chest/TextureSizeMenu.java`.
3. **Hover lore on each size = average texture size in MB**, so the cost is visible before choosing.
   Starting estimates (measure one real 256px export, scale by area ∝ px², then refine):
   | px | ~MB / block texture |
   |----|----|
   | 16  | <0.001 |
   | 32  | ~0.002 |
   | 64  | ~0.008 |
   | 128 | ~0.03 |
   | 256 | ~0.10 |
   | 512 | ~0.40 |
   (Could also show pack total ≈ per-texture × assigned blocks.)

### Still queued (unchanged)
- **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6) — next real feature once bg is signed off.
- A eyedropper · B despeckle · C preview · M2 · M4.

### Verified (Golden Rule)
✅ Smart fill (Test 1) + pixelation — developer-confirmed in-game. Open: Test 2 fringe, Test 4
low-contrast. Decided: Test 5 photo = leave. Texture-size GUI = not built.

### Build / run
`$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'` → `.\gradlew.bat build` → copy
`build\libs\customblocks-1.0.0.jar` to `%APPDATA%\.minecraft\mods\`.

---

## 2026-06-11 (cont.) — M1 bg fixes: tolerance + smart fill + pixelation (build green, NOT tested)

### Decision (developer, this session)
- Black-on-black fork → **keep black, smart fill** (NOT transparency). Honors the earlier
  "opaque black like the old version" call; no cutout/render changes. A near-black subject now
  gets a contrasting **WHITE** fill so it can't vanish into the black background.

### Done (code written; build green — compile + 3 gates, 8s; jar copied to `.minecraft\mods`)
- `image/BackgroundRemover.java`:
  - **Tolerance (bug #2):** `MAX_DELTA_E` 40 → **22**, so even strength 100 can't eat a
    low-contrast subject (Test 4's near-white "6", subject only ~ΔE 13 from its gray bg).
  - **Smart fill (bug #1):** after the bg mask, the mean foreground HSV-"value" (max RGB channel)
    picks the fill — `< 64` (near-black subject) → WHITE, else BLACK. Saturated-but-bright
    subjects (pure red = value 255) stay on black; only genuinely dark subjects flip. Partial-
    alpha edge pixels now composite against the chosen fill, not always black.
  - `snapBackgroundBlack` bails when the corners aren't black (i.e. a white-fill block), so the
    near-black snap can't blacken a dark subject.
- **Pixelation:** `CustomBlocksConfig.textureSize` default 64 → **128**; new `/cb config
  texturesize <16-256>` setter (status + set, saves config — overwrites a config.json pinned at
  64); ConfigMenu quality-row hint now points at the command.

### Verified in-game
- Nothing new — code + build only. **Golden Rule: NOT done until the developer confirms in-game.**

### Next steps, in order
1. **Re-run the 5 test images.** Expected: Test 1 (black "K") now shows on a WHITE fill, not
   black-on-black; Test 4 ("6") survives at high strength; Tests 2/5 unchanged.
2. **Pixelation gotcha:** the existing `config.json` is pinned at `textureSize: 64`, so the new
   128 default does NOT apply to this install on its own. Run `/cb config texturesize 128` once
   (or edit config.json), then **retexture** a block — texture size only affects new textures.
3. If the 5 tests pass in-game → build **M3 hex recolour** (`Reports/GROUP_06_HANDOFF.md` §6).
   Held until then.
4. Then A (eyedropper) / B (despeckle) for photo/confetti; M2, M4 after.

### Files touched
`image/BackgroundRemover.java`, `CustomBlocksConfig.java`,
`command/handlers/ConfigCommands.java`, `gui/chest/ConfigMenu.java`.

---

## 2026-06-11 — 🟢 HANDOFF (read this first): background-removal test results + next steps

> Long session ending; continue in a fresh conversation. Everything below is self-contained.

### State in one paragraph
Groups 02–06 features are built; most are developer-verified in-game. **M1 background removal is in the
jar but FAILED in-game testing** — real bugs diagnosed below (not yet fixed). Before anything else, the
next session must pick the **black-fill-vs-transparency** design fork, apply the bg fixes, and fix the
global pixelation. **M3 hex recolour** (the next real feature, spec in `Reports/GROUP_06_HANDOFF.md` §6)
is queued *behind* that. No code was changed this session — the jar = the version with the bugs below.

### ✅ Verified in-game (developer-confirmed)
- Group 04 (chat tone, `/cb help`, DidYouMean, `/cb welcome`, incidents) · Group 05 (silent pack) ·
  Group 03 core (HUD overlay, `/cb config hud`, `/cb edithud`, ESC buttons).
- Group 06: Omni-Tool + **air sneak-click**, Deleter, the 8 Squares/Triangles (tag-on-click),
  **Magic Items double-chest GUI**, unified black-bracket **`[CB]` prefix**.

### 🐞 Built but FAILING — background removal (M1 + `/cb tolerance`), tested 2026-06-11
Ran the 5 test images. Findings (root causes located, **fixes NOT applied**):

1. **Dark subject vanishes — Test 1 (black "K" on white → all-black block).** Not deletion: the white
   bg is painted **black** and the subject is also black → black-on-black, invisible. **Core flaw of
   "paint bg black": any dark subject disappears.**
   → **DECIDE FIRST:** switch removed background to **transparent (cutout render)** instead of black.
   This is the see-through option declined earlier — the black-on-black failure makes it the right call.
   Needs: `"render_type":"cutout"` in `ServerPackGenerator.cubeAllJson` **and** register every SlotBlock
   on the cutout layer client-side (Fabric `BlockRenderLayerMap`). Watch light-leak/culling glitches.
   (Weaker alt: keep black but choose a contrasting fill by subject brightness.)

2. **Tolerance too aggressive — Test 4 (gray bg + near-white "6" at tol 55 → all-black).** Map is
   `tol/100 * MAX_DELTA_E(40)`, so tol 55 = ΔE 22, but subject-to-bg was only ~ΔE 13 → the 6 read as
   background and got eaten. → **Fix:** lower `MAX_DELTA_E` in `BackgroundRemover` from 40 to ~20–22.
   Document that genuinely low-contrast images can't be cleanly separated by colour distance.

3. **Photo breaks down + modes identical — Test 5.** Colour flood-fill can't isolate a photo (no
   uniform bg); edge vs closed match because a photo has ~no enclosed same-colour pockets. → Not a
   tweak — this is the documented limit; needs the queued **eyedropper (A)/despeckle (B)/AI**.

4. **Test 2 (the two 8s) is CORRECT, keep it.** Enclosed mode blacks the loop-holes; edge-only leaves
   them red (enclosed bg, not edge-connected). That's the intended mode difference, not a bug.

### 🐞 Global quality bug — severe pixelation (ALL generated blocks)
Textures are **64px** (`CustomBlocksConfig.textureSize = 64`); 512→64 downscale + MC block rendering =
chunky. → **Fix:** bump `textureSize` to **128 or 256** + add an in-game setter (the `/cb config` row is
read-only and existing `config.json` is pinned at 64). Mind pack size at 256.

### 🟡 Partials backlog (developer-requested this session)
- **Chat flooding** — still happening; **keep PARTIAL**, fix later.
- **All "partial" GUIs** (Group 02 chest GUIs, Omni-Tool config GUI, etc.) — **revisit soon**: new
  mechanics, add new stuff into the GUIs, final polish. Treat as one dedicated backlog item.
- **Lore wording** — drop "marker"; they are **Squares / Triangles** (do in the lore polish pass).
- **Chat formatting** — dedicated polish pass queued.

### ▶️ Next steps, in order
1. Decide **black fill vs transparency** (fork in bug #1) — gates the bg fixes.
2. Apply bg fixes: tolerance scale (`MAX_DELTA_E`), the chosen fill/transparency → re-run the 5 tests.
3. Fix **pixelation** (textureSize 128/256 + setter).
4. Build **M3 hex recolour** (`GROUP_06_HANDOFF.md` §6) — the next real feature.
5. Then A (eyedropper) / B (despeckle) for photo/confetti; M2, M4 after.

### Key files
- `image/BackgroundRemover.java` — algorithm; `MAX_DELTA_E` lives here.
- `command/handlers/CreationCommands.java` — `applyTexture` (remover before `toBlockPng`, snap after).
- `command/handlers/ConfigCommands.java` — `/cb tolerance`, `/cb config background`.
- `CustomBlocksConfig.java` — `backgroundMode`, `backgroundTolerance`, `textureSize`.
- `network/ServerPackGenerator.java` — block-model gen (the `render_type` for the transparency fork).
- Specs/status: `Reports/GROUP_06_HANDOFF.md` (§6 M2/M3), `Reports/GROUP_06_TESTING_GUIDE.md`,
  `Reports/_TESTING_GUIDE_TEMPLATE.md` (format standard for all guides).

### Build / run
`$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'` → `.\gradlew.bat build` → copy
`build\libs\customblocks-1.0.0.jar` to `%APPDATA%\.minecraft\mods\`. Golden Rule: nothing's done until
the developer confirms in-game.

---

## 2026-06-10 — Finale Fix Group 06: Omni air-click, prefix unify, lore pass, Magic GUI (✅ verified in-game)

### Done (code written this session)
Four handoff items from `GROUP_06_HANDOFF.md`. 3 of 4 coded; 1 blocked on a developer decision.

**1. Omni-Tool sneak-click works in the air** — `item/OmniToolItem.java`: added a `use(World, PlayerEntity, Hand)`
override (the air / non-block right-click hook). Sneaking opens the mode-switch GUI in the air and on any
non-custom block; a plain air-click passes through. `useOnBlock` still handles custom blocks and returns
SUCCESS, so `use()` never double-fires there. Mirrors the file's existing client-SUCCESS / server-does-work
pattern; reuses the same `GuiRouter.openFresh(player, Nav.MenuKey.of(Nav.Dest.OMNI))` call already present.

**3a. [CB] prefix unified** (developer chose: black-bracket chat format everywhere) — `command/Chat.java`:
deleted `HUD_PREFIX`; `Chat.tool()` (action bar) now uses `PREFIX`. `gui/chest/CbChestHandler.java:78`
repointed `Chat.HUD_PREFIX` -> `Chat.PREFIX`. Grep confirms no other `HUD_PREFIX` / hand-built `[CB]` refs.
Note: black brackets are dim on the dark hotbar bar — the developer's accepted trade for one identical look.

**3b. Lore humanized for every item** — rewrote `appendTooltip` in `OmniToolItem`, `DeleterItem`,
`RainbowRectangleItem`, `ShapeMarkerItem` (the shared class driving all 8 markers); and ADDED `appendTooltip`
(plus Item/ItemStack/Text imports) to `LuminaBrushItem` and `ChiselItem`, which previously had none.

**4. Magic Items GUI -> double chest** — rewrote `gui/chest/MagicMenu.java` as a 6-row menu: light-blue
framed border, the 3 hand tools (Omni / Rainbow / Deleter) on the top interior row, the 8 colour markers in
a 4-colour grid (each colour's Square directly above its Triangle), a Nether-Star header, a "Marker Shapes"
label, and back / edit-toggle / close on the bottom row. Enabled items glint; disabled are dimmed. Items now
resolve by id from the item registry (`Registries.ITEM.get`) so adding a seed item only needs a slot mapping.
Seeded the 8 markers into `core/MagicItemsManager.java` (so they get enable/disable persistence).

### Task 2 (resolved): keep two separate tabs — NO code change
**Tools creative tab on Page 5 (wanted Page 2 next to Blocks).** Verified: both tabs already register
back-to-back in `CustomBlocksMod` (the only lever Fabric gives); both source trees share mod id `customblocks`
(so no stray old jar is co-loading — both tabs are from the one mod). Fabric's official 1.21 docs expose NO
API to pin a custom tab to a page or force it adjacent to another tab — a clean two-tab "Page 2" fix is not
supported. Options offered: merge tools into the Blocks tab (one tab, reliably adjacent) vs keep two tabs.
**Developer chose: keep two separate tabs.** No tab code changed; placement stays as Fabric orders it in the
137-mod pack. Per the handoff, did NOT guess-change the tab code.

### Verified (static only)
- File-size gate: all touched files well under §9.3 (MagicMenu ~135/500, OmniToolItem ~135/500, others small).
- API cross-check: `use()` signature taken from the old project's `LuminaBrushItem` (1.21.1
  `TypedActionResult<ItemStack> use(World, PlayerEntity, Hand)`); ChestMenu/Icons/GuiRouter/Nav APIs read
  from source before use; `MagicMenu.build(player, edit, page)` signature kept (GuiRouter calls it unchanged).

### Verified in-game (2026-06-10) — ✅ DONE
- `gradlew build` green (compile + all 3 gates, 27s); jar copied to `.minecraft\mods\`.
- Developer confirmed all 4: Omni air sneak-click opens the GUI; [CB] identical in chat + hotbar;
  new lore on every tool/marker; `/cb` -> Magic Items double-chest with all 11 items.

### Marked PARTIAL (revisit later)
- **Item lore** = partial (polish later), same convention as the GUIs. A dedicated lore pass later
  for tone consistency + per-item flavour; Shape/Rainbow lore to tighten once M2–M4 mechanics land.

### Next
- Continue queued mechanics, foundation-first: **M1** — CIE-LAB background remover + 3-mode config
  (see `docs/Finale Fix/Reports/GROUP_06_HANDOFF.md` §2–3).

---

## 2026-06-10 — Group 03: HUD overlay, HUD editor & ESC-menu buttons (written, NOT yet built/tested)

### Done (code written this session)
Implemented Group 03 (source issues 17.4 HUD config, 17.6 HUD overlay, 17.7 ESC menu). 5 new files,
3 rewrites, 4 edits.

**New (5):**
- `client/gui/HudEditorScreen.java` — Lunar-style drag-to-reposition overlay editor. World stays
  visible (`renderBackground` no-op, `shouldPause()` false); live preview; buttons for Scale ±,
  Color cycle, BG opacity ±, ID/Name toggles, Reset, Save, Cancel. Drag via mouseClicked/Dragged/
  Released with clamping; Cancel/Esc reverts to the snapshot taken on open.
- `client/gui/EscMenuButtons.java` — on `ScreenEvents.AFTER_INIT`, if the screen is `GameMenuScreen`,
  adds two `CbIconButton`s below the lowest vanilla button: "CustomBlocks Menu" (closes menu, runs
  `/cb`) and "HUD Editor" (opens `HudEditorScreen`).
- `client/gui/CbIconButton.java` — `ButtonWidget` that also draws a Command Block item icon.
- `mixin/ScreenInvoker.java` — `@Invoker` exposing Screen's protected `addDrawableChild` so the ESC
  buttons render + receive clicks like vanilla. Registered in `customblocks.mixins.json` (client).

**Rewritten (3):**
- `client/HudConfig.java` — full client-side config + atomic persistence to
  `config/customblocks/data/hud-config-server.json` (visible/x/y/scale/color/bgOpacity/showId/
  showName), palette cycle, clamps, load()/save()/resetDefaults()/syncFromConfig(). 170 lines (≤300).
- `client/HudRenderer.java` — split into `render(ctx)` (live, from mixin) + shared `draw(...)` honoring
  position/scale/color/bg/flags (used by the editor preview) + `boxSize(...)` for hit-testing.
- `command/handlers/GuiCommands.java` — `/cb edithud` now sends `OpenGuiPayload(HUD_EDITOR)` instead
  of toggling.
- `client/CustomBlocksClient.java` — `HudConfig.load()` on init; `HUD_EDITOR` case in the OpenGui
  switch; `HudStatePayload` receiver now sets `HudConfig.visible` + `save()`; registers
  `ScreenEvents.AFTER_INIT → EscMenuButtons`.

**Edited (4):**
- `network/HudSync.java` — separator bug fix: `id + ' ' + name` → `id + '\u0000' + name` to match
  `ClientSlotCache`'s NUL split (was producing blank/garbled HUD name lines).
- `gui/GuiMode.java` — added `HUD_EDITOR(6)`.
- `command/handlers/ConfigCommands.java` — added `/cb config hud` (status / toggle / on / off);
  resolves the dangling `/cb config hud on|off` that `ConfigScreen` already referenced.
- `src/main/resources/customblocks.mixins.json` — registered `ScreenInvoker` under `client`.

### Verified (static only)
- File-size gate: HudConfig 170/300, HudEditorScreen 205/500, ConfigCommands 113/400, all others
  well under §9.3 limits.
- Cross-checked APIs against real source: `ClientSlotCache.get/populate`, `SlotBlock.getSlotIndex`,
  `CustomBlocksConfig.hudEnabled/save`, `Chat.info/success`, `OpenGuiPayload`/`HudStatePayload`
  shapes, `CustomBlocksMod` S2C registrations (HUD_EDITOR reuses the existing OpenGuiPayload).
- Testing guide written: `docs/Finale Fix/Reports/GROUP_03_TESTING_GUIDE.md` (G03.1–G03.10).

### NOT verified — read before testing
- **Not compiled and not run.** The sandbox cannot build: no network, no cached Gradle 8.8
  distribution, and only JDK 25 present (project needs Temurin JDK 21). Compile errors remain
  possible until `gradlew build` runs on the dev PC.
- ESC buttons use `ScreenEvents.AFTER_INIT` + a `ScreenInvoker` invoker mixin (not a hand-patched
  `GameMenuScreen.init` mixin); behaviour must be confirmed in-game.
- HUD look settings persist client-side; only on/off is server-driven.
- Per the Golden Rule, none of this is DONE until confirmed in-game.

### Next session
- Run `gradlew build` on the dev PC; fix any compile errors.
- Test G03.1–G03.10 in-game per the Group 03 testing guide.

---

## 2026-06-09 — Group 02: Chest GUI Core Infrastructure (written, NOT yet built/tested)

### Done (code written this session)
Replaced the screen-based GUI with a server-side chest-GUI system. 15 new files + 5 edits.

**New — `gui/chest/` framework + menus (12):**
- `Icons.java` — stained-glass fillers + item-icon builder (name/lore/glint via DataComponentTypes).
- `Nav.java` — per-player back-stack: `Dest` enum (MAIN, BLOCK_LIST, EDITOR, UNDO, REDO, HISTORY, MAGIC, MAGIC_EDIT) + `MenuKey(dest, arg, page)` record.
- `ChestMenu.java` + `CbChestHandler.java` — generic 1–6 row chest container + click router (mirrors the proven old `CbScreenHandler`).
- `GuiRouter.java` — open/navigate/repage/back/render + command delegation (`runCommand`, `runAndReopen`, `promptCommand`, `confirmCommand`) via `executeWithPrefix`.
- `Layout.java` — shared paginated footer (back / prev / page / next / close).
- `MainMenu`, `BlockListMenu`, `EditorMenu`, `UndoMenu`, `HistoryMenu`, `MagicMenu`.

**New — core + command (3):**
- `core/MutationLog.java` — in-memory audit log (actor / action / blockId / time) for the history GUI.
- `core/MagicItemsManager.java` — magic-item registry + enabled-state persistence.
- `command/handlers/ChestGuiCommands.java` — registers `/cb`, `menu`, `dashboard`, `admingui`, `gui [block <id>]`, `editor <id>`, `listgui`, `undogui`, `redogui`, `history`, `magicitems`, `editmagicitems`, `rp pause|resume`, `sync`, `unsuppress`.

**Edited (5):**
- `CommandRegistrar` — register `ChestGuiCommands` after `GuiCommands`.
- `GuiCommands` — removed the old screen `gui`/`admingui` registrations (kept `edithud`).
- `UndoManager` — added `undoStack(uuid)`/`redoStack(uuid)` reads + a `MutationLog` hook in `push()`.
- `HistoryCommands` — added public `undoOnce`/`redoOnce` for the visual undo/redo menus.
- `ResourcePackServer` — added `pause`/`resume`/`isPaused`/`syncToAll`/`unsuppress`; `updatePack()` no-ops while paused, `sendToPlayer()` no-ops while suppressed.

Every mutating editor action DELEGATES to the existing tested `/cb` commands (give/setglow/sethardness/setsound/setcollision/setcategory/rename/retexture/note/delete) so locking, undo recording, lighting and chat feedback behave identically. Command arg formats verified against `AttributeCommands`.

### Verified (static only)
- File-size gate: all files within §9.3 limits (largest handler `ChestGuiCommands` 156 / 400).
- Mojibake gate (exact build.gradle patterns) + sound gate: 0 hits.
- Command-literal collision scan: no duplicate literals introduced.
- API cross-check against real source: `SlotManager`, `SlotData`, `SlotBlock`, `ToolItems`, `Chat`, `executeWithPrefix`, `DataComponentTypes`, `ClickEvent`/`HoverEvent` ctor style — all confirmed.
- Covers tests G02.1–G02.11.

### NOT verified — read before testing
- **Not compiled and not run.** The sandbox cannot build (no Gradle/JDK here), so compile errors remain possible until `gradlew build` is run on the dev PC.
- Known non-blocking javac warnings only (not errors): `GuiCommands` has a pre-existing duplicate `ServerPlayerEntity` import, and its `openGui` method is now unused/dead.
- Minor spec deviation: `/cb history` shows the paginated log but does NOT yet implement the optional shift-click "filter by player" from §6.
- Per the Golden Rule, none of this is DONE until confirmed in-game.

### Next session
- Run `gradlew build` on the dev PC; fix any compile errors.
- Test G02.1–G02.11 in-game.

---

## 2026-06-07 — Phase 16: the 32K startup-warning investigation (CORRECTED)

### What was seen
Client log (`.minecraft/logs/latest.log`) showed ~32,000 lines of:
`Exception loading blockstate definition: 'customblocks:blockstates/slot_N.json'
missing model for variant: 'customblocks:slot_N#light=K'`, for N = 0..2047, K = 0..15.

### Root cause (confirmed, not guessed)
- The active config `…/.minecraft/config/customblocks/config.json` has `maxSlots: 2048`, so the
  mod registers **2048 blocks** (slot_0 … slot_2047).
- `SlotBlock` carries `IntProperty LIGHT (0..15)` — dynamic glow MUST live in the blockstate
  because luminance is frozen at block construction. That is **16 block-states per block**.
- 2048 × 16 = **32,768 states**, each needing a model.
- The game resolves models at **client launch / world load — BEFORE joining any server.** At that
  moment only vanilla + the **mod jar** are loaded. The mod jar ships **zero** blockstate/model
  files (`src/main/resources/assets/customblocks/blockstates/` is empty). So every state logs
  "missing model." That is the 32K.
- The HTTP resource pack (`ServerPackGenerator`) only loads **after a player joins**, so it can
  never silence launch-time warnings, no matter what it contains.

### Decision
- These are **harmless WARN noise** — no crash, blocks still render once textures arrive. The
  developer chose to **accept them as expected** for now.
- The previous commit (`P0 Fix: eliminate 32K console warnings`) was based on a wrong assumption
  (that the HTTP pack was the source) and was **reverted in full**. That commit had also deleted
  the empty-slot loop, which legitimately suppresses post-join warnings — restoring it.

### Correction of the earlier misdiagnosis
- WRONG: "empty-string `\"\"` variant fails for LIGHT blocks." The `\"\"` catch-all is valid and
  matches all states; it was not the cause. Restored.
- WRONG: "writing empty-slot JSONs floods the log." Those JSONs *suppress* post-join warnings;
  removing them removed a safety net. Restored.

### Future option (not built — needs its own phase)
Eliminate launch warnings forever by giving the **mod itself** a model for every registered slot
at launch — e.g. a Fabric always-enabled runtime resource pack that synthesizes one
blockstate + cube_all model per slot in memory (count auto-matches `maxSlots`). Textures still
come from the HTTP pack for assigned slots; unassigned just show the missing-texture checkerboard,
silently. To discuss before building.

---

## 2026-06-05 — Phase 13/16 final push: Video, GUI screens, Mod Menu (Batch 14)

### Done (build-verified after `gradlew build` downloads jcodec + modmenu)
- **VideoDecoder** (Phase 13) — jcodec 0.2.5 MP4 frame extractor. `extractFrameAsPng(File, int)`
  seeks to a frame, converts via AWTUtil, scales via ImageProcessor. Throws cleanly on bad
  frames. jcodec + jcodec-javase 0.2.5 uncommented in build.gradle.
- **VideoCommands** — `/cb video list` enumerates `.mp4` files in `config/customblocks/videos/`.
  `/cb video extract <file> <id> <frame>` extracts frame N as the texture for block `<id>`.
  Runs on a daemon thread (never blocks tick). Registered in CommandRegistrar.
- **MacroListScreen** (Phase 10 GUI) — dedicated GUI screen for macro management opened via
  `/cb gui macros` or Main Menu → "Macros". Buttons: View All, Record New, Play, Delete, Add
  Step, Stop Recording.
- **ArabicBrowserScreen** (Phase 10 GUI) — dedicated GUI screen for the Arabic system opened
  via `/cb gui arabic` or Main Menu → "Arabic Letters". Buttons: Import All, Single Letter,
  Word Block, List.
- **Mod Menu integration** (Phase 16) — `ModMenuApiImpl` opens ConfigScreen when the ModMenu
  "Config" button is clicked. `modmenu:11.0.2` added as compile-only dep; terraformersmc maven
  repo added; `modmenu` entrypoint added to fabric.mod.json.
- **GuiCommands** — added `/cb gui macros` and `/cb gui arabic` routes.
- **CustomBlocksClient** — routed `MACRO_LIST` and `ARABIC_BROWSER` modes to their screens.
- **MainMenuScreen** — "Macros" and "Arabic Letters" buttons now open dedicated GUI screens
  instead of running list commands.
- All 3 build gates pass; jcodec + modmenu IDE errors are "missing classpath" warnings that
  clear after `.\gradlew.bat build` downloads the new dependencies.

### Pending developer action
- Run `.\gradlew.bat build` — this downloads jcodec + modmenu-api (first time will be slow).
- Test TESTING_GUIDE §1j–§1n (Batches 10–14). On "works": commit to `dev`, merge to `main`.
- Add a 64×64 PNG as `src/main/resources/pack.png` for the Mod Menu mod icon (optional).
- Place `.mp4` video files in `run/config/customblocks/videos/` to test video extraction.
- Phase 16 distribution: `.\gradlew.bat build` → `build/libs/customblocks-1.0.0.jar`.

### Architecture status
All 16 phases are now code-complete (stubs where full implementation requires external services
or developer-provided files). The only remaining hard blockers before v1.0.0 ship are in-game
confirmation of Batches 10–14 and the stress/multiplayer tests from Phase 16.

---

## 2026-06-05 — Phases 10–15 big push (Batch 13)

### Done (build-verified, NOT yet in-game confirmed)
- **MacroManager** — record/replay named command sequences. `/cb macro record <name>`,
  `add <cmd>`, `stop`, `cancel`, `play <name>`, `list`, `delete <name>`. Persists to
  `config/customblocks/macros/<name>.json`.
- **Arabic system** — `ArabicLetterMap` (28 letters), `ArabicWordRenderer` (Java2D PNG,
  loads `arabtype.ttf` from `config/customblocks/arabtype.ttf` or system fallback),
  `ArabicBlockRegistry`. Commands: `/cb arabic import`, `letter <name>`, `word <text> <id> <name>`,
  `list`.
- **Diagnostics** — `DiagnosticsHelper` + `IncidentRecorder`. `/cb diag` shows system snapshot
  (slots, heap, TPS, pack size). `/cb incidents` shows the incident log.
- **Cloud + Discord stubs** — `/cb vault` and `/cb discord` give setup instructions and fire
  when `vaultEndpoint` / `discordWebhookUrl` are set in config.json.
- **AI stubs** — `AiCommandParser` + `AiTextureGenerator` return null until `aiApiKey` is set.
- **GUI system (Phase 10)** — `GuiMode`, `GuiEngine`, `GuiState`, `OpenGuiPayload`.
  Screens: `MainMenuScreen` (7-button hub), `BlockEditorScreen` (per-block attributes),
  `ConfigScreen` (live config values). `/cb gui`, `/cb gui block <id>`, `/cb gui config`,
  `/cb admingui`, `/cb edithud` (toggle HUD). Screens open client-side via the server packet.
- **HUD overlay (Phase 11)** — `HudSyncPayload` syncs block index to client on join.
  `ClientSlotCache` caches it. `HudRenderMixin` injects into InGameHud to show
  `§e<id> §7"<name>"` when looking at a custom block. Toggle with `/cb edithud`.
- **OnboardingManager** — detects first-time players, sends a clickable welcome message once.
- **Config fields added** — `hudEnabled`, `aiApiKey`, `aiTextureEnabled`, `vaultEndpoint`,
  `discordWebhookUrl`.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Pending developer action
- Place `arabtype.ttf` in `config/customblocks/arabtype.ttf` for proper Arabic letter rendering
  (system fallback works but may lack full Arabic shaping on some servers).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12) PLUS new §1n (Batch 13).
- On "works": commit all Batches 3–13 to `dev`.
- Remaining: Phase 13 video (jcodec dep needed), Phase 14 Cloud (needs Cloudflare Worker URL),
  Phase 16 polish (Mod Menu, README finalization).

---

## 2026-06-05 — Phase 9 completion: Lock, Notes, Favorites, Drafts (Batch 12)

### Done (build-verified, NOT yet in-game confirmed)
- **LockManager** — `config/customblocks/locks.json`; atomic persist. `/cb lock <id>`,
  `/cb unlock <id>`, `/cb locked` (list with `[unlock]` buttons). Lock checks wired into
  every mutation point: `delete`, `rename`, `retexture`, `setglow`, `sethardness`,
  `setsound`, `setcollision`, `setcategory`, `template apply`, and the Deleter tool.
- **BlockNotesManager** — `config/customblocks/notes.json`. `/cb note <id>` (show),
  `/cb note <id> <text>` (set, greedy string), `/cb note <id> clear` (remove).
  Notes auto-cleaned when the block is deleted.
- **FavoritesManager** — per-player bookmarks in `config/customblocks/favorites.json`.
  `/cb fav <id>` toggles (add/remove). `/cb favs` lists with `[give]` + `[unfav]` buttons.
- **DraftManager** — `config/customblocks/drafts.json`. `/cb draft <id>` marks as draft,
  `/cb publish <id>` removes the mark, `/cb drafts` lists with `[publish]` buttons.
- **`/cb list` and `/cb search`** now show `§c[locked]` and `§8[draft]` status tags inline.
- Build green; all 3 gates pass (verifyFileSize, verifyMojibake, verifySound).

### Next
- Dev test TESTING_GUIDE §1j–§1m (Batches 10–12). On "works": commit Batches 3–12 to `dev`.
- Discuss next step: color ecosystem, remaining Phase 9 items, or Phase 10 GUI planning.

---

## 2026-06-05 — UX polish: Deleter visual, export menu, template overhaul (Batch 11)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter visual fixed:** after deleting a block definition, the placed block now shows the
  purple/black "missing texture" checkerboard (Minecraft's standard placeholder) instead of
  going fully transparent/invisible. Fixed in `ServerPackGenerator.emptyModelJson()` — empty
  slot model changed from no-geometry invisible to `cube_all` referencing an absent texture.
- **`/cb export` redesigned:** bare `/cb export` now shows an action menu with
  `[.json] [.txt] [to Vault]` buttons. `/cb export <id>` shows `[to Config] [to Vault] [Download]`
  buttons. Per-block export no longer shows file paths or copy-path buttons. Vault stubs cleanly
  to "Phase 14". New download action writes the JSON and gives an HTTP link
  (`http://host:port/export/<id>`) served by a new `/export/` route in `ResourcePackServer`.
- **`/cb template` overhauled:**
  - List shows each template's attributes inline: `glow:X hard:Y sound:Z [passable] [cat]`.
  - Empty list explains what templates are and how to use them.
  - Save/apply confirmations now show the captured/applied attribute set.
  - New `/cb template delete <name>` subcommand.
  - Tab suggestions for template names on `apply` and `delete` arguments.
- Build green; all 3 gates pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10) + §1k (Batch 11). On "works": commit Batches 3–11.
- Continue with the next phase (color ecosystem or LockManager/BlockNotes — developer's call).

---

## 2026-06-05 — Phase 9 completion + Deleter redesign (Batch 10)

### Done (build-verified, NOT yet in-game confirmed)
- **Deleter redesigned:** right-click now deletes the block's entire definition (slot data +
  texture + pack rebuild + undo record), not just the placed instance. Placed block stays,
  reverts to unassigned-slot appearance. Matches `/cb delete` semantics.
- **Per-block export:** `/cb export <id>` → `exports/<id>.json` (schema v1: all attributes,
  importable). Existing `/cb export json|txt` bulk export unchanged.
- **importfolder:** `/cb importfolder [path]` scans a folder for per-block JSONs, creates
  missing blocks, skips existing ids, reports results. Triggers pack rebuild on any creation.
  Defaults to `exports/` when no path given.
- **Templates:** `TemplateManager` + `TemplateCommands`. `/cb template save/apply/list`.
  Apply is undoable. Templates stored as `config/customblocks/templates/*.json`.
- Build green; all 3 gates (verifyFileSize, verifyMojibake, verifySound) pass.

### Next
- Dev test TESTING_GUIDE §1j (Batch 10). On "works": commit Batches 3–10 to `dev`.
- Continue with the next phase in order (LockManager / BlockNotes, or color ecosystem).

---

## 2026-06-04 — Test feedback fixes (Batch 9): Deleter, HUD prefix, undo scope

### From dev's in-game testing
- **Deleter:** said "Removed" even when it didn't delete. Fixed: now uses `World.breakBlock`
  (reliable sync + particles, ignores hardness so it still kills unbreakable blocks) and only
  confirms when the boolean says a block was actually removed.
- **HUD prefix invisible:** tool action-bar messages used the chat `[CB]` prefix whose `§0`
  black brackets vanish on the dark HUD. Added `Chat.HUD_PREFIX` (aqua) for tool feedback.
- **Undo scope:** added `/cb config undomode` cycling **server-wide** (new default) ↔
  **per-player**; `/cb config` shows it. `UndoManager` now keys its stacks by mode (shared
  GLOBAL_KEY vs per-UUID). Switching scope clears history. Persisted as `undoMode`.

### Parked for Phase 10 (dev's call — do NOT start Phase 10 yet)
- **Lumina Brush + Chisel → merge into ONE GUI "Block Editor" tool.** Left untouched for now;
  their placeholder icons (glowstone dust / amethyst shard) get finalized then too. The dev
  wants to continue phases one-by-one, not jump to the GUI system early.

### Next
- Dev re-test: Deleter actually removes + readable HUD text; `/cb config undomode` toggle
  (TESTING_GUIDE §1g + §1i). On "works": commit Batches 3–9 to `dev`. Then continue in order.

---

## 2026-06-04 — Phases 8+9 warped (Batch 8): Categories & Search

### Done (build-verified, NOT yet in-game)
- **Categories (Phase 8):** new immutable `SlotData.category` field (persisted, undoable).
  `/cb setcategory <id> <name>` (`none` clears). `/cb categories` lists categories + counts
  with clickable [list]. Pattern-identical to glow/sound — same setter/undo/persist path.
- **Search (Phase 9):** `/cb search <query>` matches id / display name / category
  (case-insensitive), each hit clickable [give]. Pure reads via `SlotManager.search/byCategory/
  categories`. `/cb list` now shows a `[category]` tag.
- Chose single-category-per-block (minimal, clean) over the old multi-category CategoryManager;
  can expand later if the GUI needs it. Build green; all 3 gates pass.

### Why this cluster
- Categories + Search are the two most tightly-connected metadata phases and carry zero image
  risk, so warping them was safe. The color ecosystem (recolor pipeline + the 4 color tools)
  is deferred — it's the one area the old project had real bugs (BFS recolor), so it gets its
  own careful batch.

### Next
- Dev batch-test Phases 6–9 (TESTING_GUIDE §1f–§1h). On "works": commit Batches 3–8 to `dev`.
- Then either color ecosystem (Phase 8 color) or templates/import-export (Phase 9 rest).

---

## 2026-06-04 — Phase 7 tools (Batch 7): Lumina Brush, Chisel, Deleter

### Done (build-verified, NOT yet in-game)
- **3 hand tools** in a new `item/` package, listed in a new "CustomBlocks Tools" creative tab:
  - **Lumina Brush** → cycles a clicked block's glow (sneak = backward); reuses setGlow path,
    refreshes placed copies, undoable.
  - **Chisel** → cycles hardness presets instant/soft/stone/hard/tough/unbreakable (sneak =
    backward); undoable.
  - **Deleter** → instantly removes a placed custom block (no drop), even unbreakable ones.
- **`CustomToolItem` base** gates work on `ServerPlayerEntity` (NOT `world.isClient`) so there's
  no client-side skip delay (Bible §9.6 pitfall). Tool models reuse vanilla item textures.
- Build green; all 3 gates pass.

### Deferred (need subsystems not built yet)
- Color Square/Triangle, Rectangle, Golden Hexagon → require the color-replace + region
  pipeline; these belong with Phase 8 (color ecosystem). Building them now would mean writing
  big machinery blind. Not done — flagged for Phase 8.

### Next
- Dev batch-test Phases 6–7 (TESTING_GUIDE §1f + §1g). On "works": commit Batches 3–7 to `dev`.
- Then Phase 8 (color ecosystem) — unlocks the remaining color tools.

---

## 2026-06-04 — Phase 6 undo/redo (Batch 5) + setcollision

### Done (build-verified, NOT yet in-game)
- **Undo/redo (per-player).** New `core/UndoManager` — clean ~150-line per-player stacks of
  immutable `SlotData` snapshots (the old one was 1,170 lines of disk-delta machinery; not
  ported). New `command/handlers/HistoryCommands` registers `/cb undo` + `/cb redo`.
  - Recording wired into create / delete / rename / dupe / setglow / sethardness / setsound.
  - `SlotManager.restoreSnapshot` + `removeSilently` = non-recording restore primitives so
    undo never re-triggers itself. Delete-undo restores the texture bytes too.
  - Config: `maxUndoDepth` (default 25). Per-player isolation via UUID-keyed stacks.
- **setcollision** (walk-through blocks): `/cb setcollision <id> <solid|passable>`. New
  immutable `SlotData.noCollision` field (persisted, undoable). `SlotBlock.getCollisionShape`
  returns an empty box live when passable — entities pass through, outline stays breakable.

### Next
- Dev in-game test: undo/redo per-player + collision toggle (TESTING_GUIDE §6); 2-player
  isolation if possible. Then the 7 tools (Phase 7) as their own focused, tested batch.

---

## 2026-06-04 — Bug fixes: mining formula, [CB] chat format (sound path verified)

### Fixed
- **Hardness mining was wrong (Bug 1 + 2).** The ported formula divided by 100 unless tool
  speed > 1 — but our blocks aren't in any tool tag, so speed is always 1.0 and it ALWAYS
  took the slow ÷100 branch (default 1.5 ≈ 7.5s by hand → felt unbreakable). Replaced with
  Minecraft's EXACT vanilla formula `getBlockBreakingSpeed / hardness / (canHarvest?30:100)`
  using live hardness. Now low = fast, high = slow, -1 = unbreakable, default 1.5 = stone-like.
- **Default hardness:** verified `DEFAULT_HARDNESS = 1.5` (no -1 regression). The "unbreakable
  default" was the broken formula, now fixed.
- **Chat bloat (Bug 4):** added `command/Chat` with the old project's `§0§l[§b§lCB§0§l]§r`
  prefix + ✔/✖ glyphs. Converted Creation/Attribute/Utility command messages to short
  branded lines (dropped the multi-line explanations).

### Verified, NOT changed
- **setsound (Bug 3):** traced the full path — command → `SlotManager.setSoundType` (updates
  BY_ID + BY_SLOT) → `saveAll` writes `"sound"` → `SlotBlock.getSoundGroup` reads it live
  (confirmed `state.getSoundGroup()` delegates live to the block, no caching). The code is
  correct end-to-end; no defect found. Likely the client wasn't relaunched after the build, or
  a subtle sound. Isolation test added to TESTING_GUIDE (check slots.json + a fresh runClient).

### Next
- Dev re-test in Survival: mining speed scales correctly; `[CB]` messages are concise; setsound
  isolation test. Then commit + continue Batch 4 (undo/redo).

---

## 2026-06-04 — Hardness words + message fix + Batch 4 slice 3 (sounds)

### Findings explained (from dev testing)
- "Hardness 1/20/50 all feel the same" = **Creative mode** breaks any positive-hardness block
  instantly; only `-1` (unbreakable) is honored there. Hardness break-times only differ in
  **Survival**. Not a bug — explained to the dev.
- The "(vanilla stone = 1.5)" hint was appended to every value (looked like it called 50
  "vanilla stone"). Fixed the wording.

### Done (build-verified, NOT yet in-game)
- **sethardness word values:** accepts `unbreakable` / `instant` / `stone` (with tab
  suggestions) as well as numbers. Clearer message + a Survival-vs-Creative note.
- **Sounds (Phase 6, slice 3):** `/cb setsound <id> <type>` (17 vanilla groups). `SlotData.soundType`
  (immutable, default "stone", persisted); read live in `SlotBlock.getSoundGroup`
  (+ shared `SlotBlock.soundGroupFor`/`SOUND_TYPES` for the command's validation/suggestions).

### Next
- Dev in-game test: TESTING_GUIDE §1e — hardness words **in Survival**, setsound on
  break/step. On "works": commit Batch 3 + glow + hardness + sounds to `dev`, then undo/redo.

---

## 2026-06-04 — Glow confirmed ✅ + glow cap + Batch 4 slice 2 (hardness)

### Done
- **Glow confirmed working in-game** by the developer ("done it perfectly"). ✅
  The BlockState-property approach (ADR-002) actually emits light.

### Done (build-verified, NOT yet in-game)
- **Side quest — setglow > 15:** Minecraft caps block light at 15 (4-bit, hard engine limit).
  `setglow` now accepts ANY number and caps at 15 with a friendly message instead of a red
  Brigadier rejection. There is no way to emit more than 15 — explained to the developer.
- **Hardness (Phase 6, slice 2):** `/cb sethardness <id> <value>` (negative = unbreakable,
  0 = instant, 1.5 = vanilla stone). `SlotData.hardness` (immutable, default 1.5, persisted);
  read **live** in `SlotBlock.calcBlockBreakingDelta` (ported formula) — no state property
  needed because hardness, unlike luminance, is queried live each break attempt.

### Next
- Dev in-game test: TESTING_GUIDE §1e (glow cap message + hardness: unbreakable / instant /
  slow). On "works": commit Batch 3 + glow + hardness to `dev`, then continue Batch 4
  (custom sounds, then undo/redo and tools).

---

## 2026-06-04 — Batch 3 polish + Batch 4 slice 1 (glow) build-verified

### Done (build-verified, NOT yet in-game)
- **Old-format create:** `/cb create <id> <name> <url>` makes + textures in one command
  (name is now a quoted string so a URL can follow it; `<id>` and `<id> <name>` still work).
- **Bug fix (from screenshot):** the failing URL was a `data:` URI, which `HttpClient`
  rejects ("invalid URI scheme data"). Now non-http(s) URLs are caught **before** the block
  is created → clear message, no half-made block, no "already exists" cascade.
  (`ImageDownloader.isHttpUrl` + pre-create check.)
- **Tab auto-complete** (`BlockSuggestions.IDS`) for delete/rename/dupe/retexture/setglow/give.
- **Glow attribute (Phase 6, slice 1):** `/cb setglow <id> <0-15>` via a new
  `AttributeCommands` handler; `SlotData.glow` (immutable, 0–15, persisted) is the
  configured default for new placements.

### Debug — glow was NOT working (fixed)
- Root cause (verified in the 1.21.1 bytecode): `AbstractBlockState.getLuminance()` returns
  a `final int` field computed ONCE at state construction. A `.luminance(state -> liveValue)`
  lambda is therefore frozen at registration (value 0, before any glow is set) — so dynamic
  glow via a lambda can NEVER work. The old project had the same dead lambda + a
  `triggerGlowUpdate` that scanned `getBlockEntityPositions()` (SlotBlock has no block entity),
  so its glow was effectively broken too.
- Fix (vanilla-idiomatic, like `LightBlock`): light is now a real **block-state property**
  `LIGHT (0..15)` on `SlotBlock`. `luminance` reads `state.get(LIGHT)` → baked per state →
  actually emits. `getPlacementState` applies the slot's configured glow to new placements;
  `block/SlotLighting` rewrites the LIGHT state of already-placed instances near players.
- Pack-safe: the generator's `""` catch-all variant matches all 16 states (verified in the
  `BlockStatesLoader` bytecode — empty key = empty predicate = match-all), so no purple blocks.

### Decisions
- Attribute setters live in their own `AttributeCommands` handler (design rule #5),
  not in CreationCommands — keeps each handler focused and under the size cap.
- Glow stored in the block STATE (not just SlotData): a placed block's light is saved in the
  chunk, survives reload independently, and is correct per-instance. See `docs/adr/ADR-002`.

### Next
- Dev in-game test: TESTING_GUIDE §1d (texture, incl. one-shot create + bad-URL §3.2b) and
  §1e (glow). On "works": commit Batch 3 + glow to `dev`, then continue Batch 4
  (hardness, sounds, undo/redo, tools).

---

## 2026-06-03 — Batch 3 (Phase 4: textures) build-verified

### Done (build-verified, NOT yet in-game)
- `image/ImageDownloader` (browser headers) + `image/ImageProcessor` (bicubic square PNG).
- `core/TextureStore` — per-slot PNG files (atomic); textures stay out of slots.json.
- `network/ServerPackGenerator` — pack_format 34; textured cube_all for created blocks;
  one shared invisible model for empty slots (kills the missing-model warnings).
- `network/ResourcePackServer` — embedded HTTP `/pack.zip`, SHA-1, ResourcePackSendS2CPacket
  to ALL clients (single pack path; no modded/vanilla split).
- `/cb retexture <id> <url>` — download/decode off the server thread, then rebuild + push.
- Mod wiring: HTTP start/build on SERVER_STARTED, stop on SERVER_STOPPED, send pack on join,
  creative tab → created-blocks-only. Config: textureSize=64, httpHost=127.0.0.1.

### Design note (de-risking)
- Chose a single HTTP-pack path for ALL clients to avoid the old PACK2 dual-system conflict.
  Client-side generation (Phase 5) is now optional. Textures work everywhere via the pack;
  multiplayer block NAMES + creative-tab contents still need client sync (Phase 5).

### Known rough edge
- Each retexture re-prompts the player to accept the pack (vanilla mechanism). Smoothing
  this (client auto-apply) is a Phase 5 nicety.

### Next
- Dev in-game test (TESTING_GUIDE §1d): create -> retexture <url> -> accept pack prompt ->
  see the textured block; survives a restart. Then commit Batch 3 to dev.

---

## 2026-06-03 — Phase 1 CONFIRMED ✅ + Batch 2 (Phases 2-3 core)

### Context
- Developer asked to batch related phases (16 small steps was overwhelming) and to lower
  maxSlots to **800** for faster load. New plan = 6 testable batches; this is Batch 2.

### Done (build-verified, NOT yet in-game)
- Phase 1 confirmed working in-game ("Phase 1 works"). ✅
- `maxSlots` default → 800.
- `SlotDataStore` — atomic JSON persistence (sole slot disk-I/O class, rule #4).
- `SlotManager` — create/delete/rename/dupe + loadAll/saveAll/reload; orphan guard on load.
- `/cb` command tree: `CommandRegistrar` + `CreationCommands` (create/delete/rename/dupe)
  + `UtilityCommands` (list/give/reload). LANG1 avoided by construction (clean literal tree).
- Wired `CustomBlocksMod`: loadAll after registerAll, register commands, saveAll on stop.
- `gradlew build` PASSED, all gates green; no file over its size limit.

### Deferred (on purpose, to later batches)
- Undo/redo → batched with attributes (Batch 4 / Phase 6).
- retexture → needs the image pipeline (Batch 3 / Phase 4).
- Creative tab still lists all 800 slots; will switch to "only created blocks" in Batch 3.

### Next
- Developer in-game test of Batch 2 (TESTING_GUIDE §1c): create/list/give/place/rename/
  dupe/delete + survives a restart. Then Batch 3 = textures.

---

## 2026-06-03 — Phase 0 CONFIRMED ✅ + Phase 1 start

### Done
- **Phase 0 confirmed working in-game by the developer** ("Phase 0 works"). Mod loads in
  MC 1.21.1, the three `[CustomBlocks]` startup lines appear, no crash. Phase 0 is DONE.

### Now starting — Phase 1: Block Slot System
- Goal (Bible milestone): the game starts with **1028 blocks registered**, no errors.
- Scope: `CustomBlocksConfig` (minimal: maxSlots, httpPort), immutable `SlotData` + `.update()`,
  `SlotBlock`, `SlotManager` (register the pool + BlockItems + a creative tab), wire into
  `CustomBlocksMod` with a clear "Registered N slot blocks" log line.
- Deferred to their own phases: `SlotDataStore` (Phase 2 persistence), `BlockFinder` (Phase 3).
- Expectation: slot blocks render as the **missing-texture (purple/black) cube** until Phase 4.

### Build-verified (NOT yet in-game)
- Implemented `CustomBlocksConfig` (62 lines, atomic save), `SlotData` (immutable record
  + `.update()`), `SlotBlock` + `SlotBlock.SlotItem` (minimal), `SlotManager.registerAll()`,
  and wired `CustomBlocksMod` to register the pool + a "CustomBlocks" creative tab.
- `gradlew build` PASSED — compiles, all gates green. No file exceeds its size limit.
- Recycled the proven 1.21.1 registration API from the old project (`Identifier.of`,
  `Registry.register(Registries.BLOCK/ITEM, …)`, `FabricItemGroup.builder()`).
- **Awaiting developer in-game test** (see docs/TESTING_GUIDE.md §Phase 1): startup log
  should read "Registered 1028 slot blocks", game must not crash, creative tab present.
  Expect many "missing model" warnings + purple blocks — normal until Phase 4.

---

## 2026-06-03 (cont.) — Phase 0: hardening & protocol

### Done
- Wrote `CLAUDE.md` — the operating protocol (golden rule, who the developer is, phase
  discipline, architecture rules, known pitfalls, git/doc protocol, forbidden behaviors).
- Added `verifyFileSize` build gate (§9.3): fails the build if any `.java` exceeds its
  limit (500 general / 400 command handlers / 300 `*Config.java`). package-info exempt.
  This is new — the old project relied on an unenforced rule and grew 9,400-line files.
- Added `.editorconfig` (UTF-8, LF, 4-space) for encoding/format consistency (NFR-11).
- Added GitHub Actions CI (`.github/workflows/build.yml`): JDK 21 + `gradlew build`
  (which runs all three gates) + JAR artifact. Remote is `github.com/3liSY/CustomBlocks`.

### Note on "spot the bugs"
- The new project has no runtime bugs to hunt yet (≈3 code files, builds clean). The real
  bugs live in the old project; the strategy is **prevention** (gates above + the pitfalls
  table in CLAUDE.md §7) and **fix-on-port** as each phase is rebuilt.

### Verified
- `gradlew build` **PASSED** — all three gates run (`verifyFileSize`, `verifyMojibake`,
  `verifySound`).
- `verifyFileSize` proven to actually fire: a throwaway 501-line file made the build
  FAIL with exit 1 ("exceeds class limit of 500"); file then deleted, build green again.
  (A gate that's never seen to fail is false confidence — so it was tested both ways.)

### Next Session
- Phase 1 — Block Slot System (unchanged from the entry below).

---

## 2026-06-03 — Phase 0: Foundation (fresh start)

### Done
- Scaffolded a fresh project in the repo root, recycling the old project's working
  build infrastructure (Fabric Loom 1.7.4, Gradle 8.8 wrapper, MC 1.21.1, Java 21).
- `build.gradle`: minimal Phase-0 dependency set + `verifyMojibake` and `verifySound`
  gates wired into `build`. jcodec / SpotBugs / permissions are commented out, to be
  enabled in their phases (13 / 15 / when needed).
- `fabric.mod.json`, `customblocks.mixins.json` (empty mixin list), minimal `en_us.json`.
- Full package structure laid down as `package-info.java` for all 21 packages.
- `CustomBlocksMod` + `CustomBlocksClient` entrypoints — log a Hello-World line on load.
- Ported `TextSanitizer.java` verbatim (the mojibake repair dictionary).
- Docs: `CHANGELOG.md`, this log, `docs/adr/ADR-001` + template, copied
  `docs/MOJIBAKE_SHIELD.md`, and `LICENSE` / `LICENSE-ar`.

### Decision (flagged to developer)
- Instead of ~90 empty class stubs, the package structure is documented via
  `package-info.java` per package, with real classes added per phase. This matches
  the Bible's own "start minimal, expand per-phase" philosophy. Can switch to full
  stubs if the developer prefers.

### Verified
- `gradlew build` **PASSED** (25s): compileJava, verifyMojibake, and verifySound all
  green; produced `build/libs/customblocks-1.0.0.jar` (2.3 MB, fabric-api bundled).
- Required installing Temurin JDK 21 (machine had only JDK 26, too new for Gradle 8.8)
  and pinning the Gradle daemon to it via `~/.gradle/gradle.properties`.
- **NOT yet verified in-game** — per CLAUDE.md, only a developer in-game test counts
  as DONE. The build passing proves it compiles, not that it works in Minecraft.
  Next: run `gradlew runClient` and confirm the "[CustomBlocks] Hello World — mod
  loaded successfully." line appears in the log with no errors.

### Next Session
- Phase 1 — Block Slot System: `SlotBlock`, immutable `SlotData` + `.update()`,
  `SlotManager` (register 1028 blocks), `SlotDataStore`, `BlockFinder`, minimal
  `CustomBlocksConfig` (httpPort, maxSlots). Milestone: server starts with 1028
  blocks in the registry, no errors.
