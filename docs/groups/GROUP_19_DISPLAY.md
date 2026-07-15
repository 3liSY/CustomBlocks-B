# Group 19 — Showcase & Hologram Display Systems

> **Prerequisite:** Group 02 (Chest GUI) verified. Texture pipeline (TextureStore / resource-pack render) working (G10/G13/G14 all rely on it). Display Entities are native to MC 1.21.1.
>
> **Objective:** A **highly customizable** Showcase system — real floating blocks on configurable stands — plus a Hologram preview system (`/cb preview`) and, last, offhand hologram projection.
>
> **Source issues:** Group J (showcase blocks), Q1 (hologram `/cb preview` + offhand), Decision §I (ambitious features kept).
>
> **Rules:** Build **one slice at a time**, in order. Owner confirms each in-game before the next (CLAUDE.md §2/§4). Nothing is ✅ until the owner confirms in-game.
>
> ⚠️ **UI medium audit (2026-07-10):** the showcase config UI is Screen-based, not chest GUI, per the
> mod-wide Screen migration. G19 is entirely unbuilt, so this is a clean target — no legacy chest code to
> migrate away from. §S2/G19.6 below and its "multi-tab chest GUI" wording describe the pre-migration design;
> build it as a Screen instead.

---

## 🔒 Locked Decisions (owner interview 2026-06-21)

> Surveyed the live code first, then a multi-round owner interview. **Nothing built this pass — design only.**
> These decisions override anything below them. Reality corrections are read from code, not guessed.

**Reality corrections (read, not guessed):**
- **No display/showcase code exists.** No `showcase`/`DisplayEntity`/`display_blocks.json`/`givedisplayblock` in `src`. G19 is **fully new** — no port, no stub to revive.
- **Render path = Display Entities (MC 1.21.1 native).** Real blocks → **`BlockDisplay`** (shows the true blockstate as a full 3D block — owner's "block as its block state floating", **not** a dropped item). Vanilla non-block items (e.g. `diamond_sword`) → **`ItemDisplay`**. Both are server-side, so "visible to other players" (old test G20.7) is **free**, not extra work.
- **Animated/GIF blocks won't show via `BlockDisplay`.** Animated blocks render through a custom BER (`AnimSlotBER`) / `SlotItemRenderer` (G14, off-atlas). `BlockDisplay` renders the static block model only and **never invokes a BER**. So an *animated* showcase must use the **item-render path** (`ItemDisplay` of the animated item model), not `BlockDisplay`. Flagged on slice S3.
- **Texture source = `TextureStore`** (`core/TextureStore` — `load(i)`, `has(i)`, `hasAnyFace(i)`, `FACES`). `/cb preview <id>` just spawns a `BlockDisplay` of an existing block (already rendered in the pack). `/cb preview <url>` is the heavy path — it must **download + render a temp texture** (reuse the create pipeline), so it can be slow / fail on a bad URL.
- **Creative tabs exist:** `CUSTOM_BLOCKS_TAB` ("blocks") and **`CUSTOM_TOOLS_TAB` ("tools")** (`CustomBlocksMod.java:59-65`). The **placer item belongs in the Tools tab**.
- **Commands** split by domain in `command/handlers/*`, registered by `CommandRegistrar`. New handler(s) needed (e.g. `ShowcaseCommands`, `PreviewCommands`), each ≤ 400 lines (§9.3 gate).
- **Disk I/O** follows the store pattern (atomic write-temp + rename). New `display_blocks.json` gets its own store (e.g. `ShowcaseDataStore`), mirroring `SlotDataStore`.

**Locked decisions:**

| # | Decision |
|---|---|
| D1 | **G19 owns showcase fully** — command + config GUI + rendering. Resolves the SWEEP §C contest: G23 only cross-refs. |
| D2 | **Render = `BlockDisplay` for blocks (custom + vanilla), `ItemDisplay` for vanilla items.** Real floating block, never a dropped-item look. |
| D3 | **Placement:** spawn on the looked-at surface; if aiming at sky / no block in range → **2 blocks in front of the head** along the look vector (never errors, never orphaned). |
| D4 | **OP-only** for all placing / removing / editing. |
| D5 | **Persists** to `config/customblocks/data/display_blocks.json` (atomic); **survives restart**; respawns the display entity on chunk/world load. Auto-assigned instance ids; config/remove target = the showcase you look at, or by id. |
| D6 | **Display types this group: pedestal + floating only.** Glass case + open shelf = parked. |
| D7 | **Rotation:** smooth interpolated **spin**, speed slider that **includes a static (no-spin)** setting. (Tumble / multi-axis = parked.) |
| D8 | **Scale:** 0.5× up to **~4× (giant statues)**. |
| D9 | **Cool knobs:** glow outline (default colour, per-showcase colour parked) · fullbright (shines in the dark) · hover bob (floats up/down) · floating label (**auto = block name, editable**, shows through walls) · **particle aura (style picked per showcase**: sparkle / embers / enchant / portal). |
| D10 | **Multi-block:** a showcase holds **1 or more** blocks. **Right-click = cycle** to the next. Optional **auto-cycle** at an owner-set interval (**min 0.1s**). Content = a chosen list **or** "all custom blocks" (**dynamic** — new blocks join the gallery). (Orbit-all-at-once = parked.) |
| D11 | **Controls:** Right-click = cycle next (single-block = no-op) · **Shift-right-click = config GUI** · **Remove = a button in the config GUI + `/cb showcase remove`** (looked-at or by id). No accidental removal. |
| D12 | **Config GUI = multi-page tabs (Screen-based):** Appearance / Motion / Display. |
| D13 | **Presets:** a named **library** + one **auto-default** applied to every new showcase (server-wide, OP-managed). |
| D14 | **Placer item:** `/cb showcase <id>` spawns directly at aim; **`/cb showcase item`** gives a furniture-style placer; the placer is also in the **Tools creative tab** (and other entry points). |
| D15 | **Management:** `/cb showcase list` (click → teleport) · locate (glow-highlight nearby) · edit nearest · rename · duplicate/clone · move/reposition · export/share config as a code. |
| D16 | **Bulk / group:** apply preset to all in radius / remove all in radius · hide-show toggle (temporary, no delete) · lock (freeze config) · groups (name a set, sync settings). |
| D17 | **Hologram `/cb preview <id\|url>`** — temporary spin (default **10s** OR move **>5 blocks** away). Visible to others (free). `<id>` fast; `<url>` downloads+renders. Extras: **pin** (`/cb preview pin` → permanent baseless display) · **unpin** (`/cb preview unpin`) · **to a player** (`@player`) · **compare two** side by side. Config: `hologramHeight` (1.5), `hologramColor` (none). |
| D18 | **Offhand hologram = LAST slice (S8).** Hold a custom block in offhand → hologram ~1.5 blocks above head, follows the player, visible to others, vanishes when offhand emptied. Config: `offhandHologramEnabled` (true). |
| D19 | **Performance cap:** sensible default (per chunk) with a warning; OP-configurable. Auto-cycle/particles/animated all count toward load. |
| D20 | **Pace = one slice at a time**, in-game confirm between each (CLAUDE.md §4). |

**Owner-approved defaults (flag if wrong):** glow default colour = white · "all custom blocks" gallery = dynamic · preview = 10s / 5-block · instance ids auto · config targets the looked-at showcase.

---

## ⛔ Parked — discuss later, NOT built in G19

| Item | Note |
|---|---|
| **Shop mode** | Click showcase to buy/receive the block (free admin-give or priced economy). Flagship, revisit. |
| **Redstone control** | Power → spin/visible; unpowered → freeze/hide. |
| **Proximity react** | Idle when alone; spins faster + glows when a player approaches. |
| Glass case + open shelf types | Only pedestal + floating in G19 (D6). |
| Per-showcase glow colour | Single default colour for now (D9). |
| Tilt / lean · tumble rotation · orbit-all-at-once | Parked (D7/D10). |

---

## 🧱 Slice Plan (build order — confirm each in-game before the next)

| Slice | Scope | Tests |
|---|---|---|
| **S1 — Showcase core** | `/cb showcase <id>` spawns a `BlockDisplay` at aim (sky → 2 ahead) · smooth spin + static · **pedestal + floating** · scale incl. giant · persistence (`display_blocks.json`, atomic, survive restart, respawn on load) · `/cb showcase remove` · OP gate · auto instance ids | G19.1–G19.5 |
| **S2 — Config GUI** | Multi-page tabs (Appearance / Motion / Display): type · rotation speed + static · scale · glow outline · fullbright · hover bob · label (auto name/editable) · particle aura (style pick) · **Shift-right-click opens** · in-GUI remove button | G19.6–G19.9 |
| **S3 — Multi-block + vanilla + animated + placer** | Cycle on right-click · auto-cycle (≥0.1s) · chosen list **or** "all custom blocks" · vanilla items (`ItemDisplay`) · **animated blocks via item-render path** (reality caveat above) · placer item (`/cb showcase item` + Tools tab) | G19.10–G19.14 |
| **S4 — Presets + cap** | Named preset library · auto-default for new showcases · performance cap (default + OP-configurable) | G19.15–G19.16 |
| **S5 — Management (single)** | `/cb showcase list` + click-teleport · locate (glow-highlight) · edit nearest · rename · duplicate/clone · move/reposition | G19.17–G19.20 |
| **S6 — Bulk / group** | Apply preset to all in radius / remove all in radius · hide-show toggle · lock · groups (sync settings) | G19.21–G19.24 |
| **S7 — Hologram preview** | `/cb preview <id\|url>` temporary (10s / 5-block) · `@player` · compare two · pin / unpin · `hologramHeight` / `hologramColor` | G19.25–G19.29 |
| **S8 — Offhand projection** *(hardest, last)* | Offhand custom block → hologram above head, follows player, visible to others, vanishes on empty · `offhandHologramEnabled` | G19.30–G19.31 |

> **Each slice ends `🟢 build-green` only — `✅ done` needs the owner's in-game confirm (CLAUDE.md §2).**
> This is a large group by design (owner wants the full vision). It ships incrementally; we never build the whole stack at once.

---

## What this group adds

| Area | Old CustomBlocks | This Group |
|---|---|---|
| Showcase blocks | Basic display block, simple rotation | Full revamp: real floating block (`BlockDisplay`), pedestal + floating, smooth spin/static, scale to 4×, glow/fullbright/bob/label/aura |
| Showcase content | Custom only | Custom blocks + vanilla blocks + vanilla items + animated blocks; multi-block cycle gallery ("all custom blocks") |
| Showcase mgmt | None | list/teleport, locate, rename, clone, move, bulk-in-radius, hide/show, lock, groups, presets, export/share |
| `/cb showcase` / `config` | Existed (basic) | Restored + revamped + multi-tab config GUI + placer item (Tools tab) |
| Hologram preview | `hologram` stub (never built) | `/cb preview <id\|url>`, pin/unpin, to-player, compare |
| Offhand hologram | Not present | New (last slice) |

---

## What this group covers (commands)

| Feature | Command |
|---|---|
| Create showcase | `/cb showcase <id>` (custom/vanilla block) · `/cb showcase <vanilla-item>` |
| Placer item | `/cb showcase item` (+ Tools creative tab) |
| Config | Shift-right-click a showcase · `/cb showcase config <id>` |
| Remove | in-GUI button · `/cb showcase remove` (looked-at or `<id>`) |
| Manage | `/cb showcase list` · `rename` · `clone` · `move` · `lock` · `hide`/`show` · `group` · `export`/`import` |
| Bulk | `/cb showcase bulk <preset\|remove> <radius>` |
| Hologram | `/cb preview <id\|url>` · `pin` · `unpin` · `<id\|url> @player` · `compare <a> <b>` |
| Storage | `config/customblocks/data/display_blocks.json` |
| Config fields | `hologramHeight`, `hologramColor`, `offhandHologramEnabled`, showcase cap |

---

## Setup

```
/cb create g19a ShowcaseTest https://i.imgur.com/example.png
/cb create g19b ShowcaseTest2
```

---

## Tests

> Numbering is grouped by slice. Old `G20.x` numbering is **retired** (this is Group 19). Tests are ⏳ until their slice is built.

### S1 — Showcase core
- **G19.1 — Create showcase (pedestal):** `/cb showcase g19a` → the `g19a` block floats above a pedestal, spinning smoothly. **Fail:** error / nothing.
- **G19.2 — Floating type:** set type to floating → block spins in mid-air, no base.
- **G19.3 — Aim at sky:** look up at open sky, `/cb showcase g19a` → it spawns ~2 blocks in front of your head (no error, not orphaned).
- **G19.4 — Remove:** `/cb showcase remove` while looking at it → showcase gone.
- **G19.5 — Persist across restart:** restart server → the showcase is still there, still spinning.

### S2 — Config GUI
- **G19.6 — Config opens:** **shift-right-click** the showcase (or `/cb showcase config g19a`) → multi-tab Screen (Appearance / Motion / Display).
- **G19.7 — Static rotation:** Motion tab → set speed to **static** → block stops spinning.
- **G19.8 — Cool knobs:** Appearance tab → toggle glow outline, fullbright, hover bob; set giant scale; pick a particle aura → each visibly applies.
- **G19.9 — Label:** enable label → block's name floats above it (visible through walls); edit it → custom text shows.

### S3 — Multi-block + vanilla + animated + placer
- **G19.10 — Vanilla item:** `/cb showcase diamond_sword` → a diamond sword displays (item form). No `g19a` needed.
- **G19.11 — Multi-block cycle:** add `g19a` + `g19b` to one showcase → right-click cycles between them.
- **G19.12 — Auto-cycle:** set auto-cycle to 0.5s → it flips automatically; set 0.1s → faster.
- **G19.13 — All-custom gallery:** set content to "all custom blocks" → it cycles through every created block; create a new one → it joins.
- **G19.14 — Placer item:** `/cb showcase item` (or grab from the **Tools** creative tab) → place it like furniture → becomes a showcase.

### S4 — Presets + cap
- **G19.15 — Preset save/apply:** save a look as a named preset → apply to another showcase → it matches.
- **G19.16 — Default + cap:** set a preset as default → new showcases start with it. Exceed the cap → a warning appears.

### S5 — Management
- **G19.17 — List + teleport:** `/cb showcase list` → click an entry → teleported to it.
- **G19.18 — Locate:** locate → all nearby showcases glow-highlight.
- **G19.19 — Rename + clone:** rename one; clone it → the copy has the same config.
- **G19.20 — Move + edit nearest:** move a showcase to a new spot; edit-nearest changes the closest one without an id.

### S6 — Bulk / group
- **G19.21 — Bulk apply:** apply a preset to all showcases in a radius → all update.
- **G19.22 — Bulk remove:** remove all in a radius → all gone.
- **G19.23 — Hide/show + lock:** hide a showcase (still saved) → show it back; lock one → its config can't be changed.
- **G19.24 — Groups:** group several → change a group setting → all in the group update together.

### S7 — Hologram preview
- **G19.25 — Preview by id:** `/cb preview g19a` → temporary spinning hologram; disappears after 10s.
- **G19.26 — Preview by url:** `/cb preview <url>` → downloads + shows; bad url → a clear error, no crash.
- **G19.27 — Move-away despawn:** `/cb preview g19a`, walk >5 blocks → it vanishes early.
- **G19.28 — Pin / unpin:** `/cb preview pin` → it stays; `/cb preview unpin` → gone.
- **G19.29 — To-player + compare:** `/cb preview g19a @player` → that player sees it; `/cb preview compare g19a g19b` → two side by side.

### S8 — Offhand projection
- **G19.30 — Offhand shows:** hold `g19a` in offhand → hologram ~1.5 blocks above your head; another player sees it.
- **G19.31 — Offhand clears:** empty the offhand slot → hologram disappears immediately.

---

## Group 19 Verdict

| Slice | Tests | Result |
|---|---|---|
| S1 — Core | G19.1–G19.5 | ⏳ |
| S2 — Config GUI | G19.6–G19.9 | ⏳ |
| S3 — Multi/vanilla/animated/placer | G19.10–G19.14 | ⏳ |
| S4 — Presets + cap | G19.15–G19.16 | ⏳ |
| S5 — Management | G19.17–G19.20 | ⏳ |
| S6 — Bulk / group | G19.21–G19.24 | ⏳ |
| S7 — Hologram preview | G19.25–G19.29 | ⏳ |
| S8 — Offhand projection | G19.30–G19.31 | ⏳ |

**Group 19 passes when showcases and holograms both work in-game, slice by slice.**

If anything shows ❌ — paste: (1) the exact command/action, (2) what appeared vs expected, (3) last ~20 lines of `latest.log`.

---

## Cleanup

```
/cb showcase remove        (look at each, or /cb showcase bulk remove <radius>)
/cb delete g19a
/cb delete g19b
```
