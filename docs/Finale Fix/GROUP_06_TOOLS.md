# Group 06 — Tools, Dynamic Glow & Creative Tab

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 7 (Tools) build-verified.
>
> **Objective:** Merge the Lumina Brush and Chisel into a single configurable "Omni-Tool". Add held-block dynamic glow emission. Audit and clean up the dedicated tools creative tab. Restore tool-give shortcuts. Finalize tool consolidation per locked decisions.
>
> **Source issues:** 17.8 (held block dynamic glow), 17.9 (dedicated creative tools tab), 17.10 (Chisel + Lumina unification), Group F (tool-give shortcuts), Q8 (tool consolidation), Decision §7, §8, §9, §C
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group restores / adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Lumina Brush | Cycled glow 0→4→8→12→15 | Working physical item | Merged into Omni-Tool as "Glow Mode" |
| Amethyst Chisel | Cycled hardness presets | Working physical item | Merged into Omni-Tool as "Hardness Mode" |
| Omni-Tool | Existed as configurable multi-mode tool | Missing (two separate tools) | Rebuilt: one item, hot-swappable modes via Shift+RClick |
| Deleter | Right-click to remove block instantly | Working physical item | Kept as separate item (by decision) |
| Tool-give shortcuts | `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle` | Missing — had to use `/give @s customblocks:…` | Restored as `/cb brush` → gives Omni-Tool, etc. |
| Held block glow | Torch-like light emitted when holding glowing block | Missing | Networked client-side mixin: server packet + client light render |
| Creative tools tab | "CustomBlocks Tools" tab existed | Two tabs (Blocks + Tools) | Single "CustomBlocks Tools" tab: Omni-Tool, Deleter, Square, Triangle. NO blocks in tools tab. Sortable via config. |
| Golden Hexagon | Admin tool (physical item) | Framework only | Merged into Omni-Tool as "Admin Mode" — not a separate item |
| Rainbow Rectangle | Area tool (physical item) | Framework only | Merged into Omni-Tool as "Area Mode" |
| Diamond Triangle (Wand) | Eyedrop/wand tool | Framework only | Merged into Omni-Tool as "Eyedrop Mode" |
| Tab icon | `/cb settabicon <url>` | Missing | Restored (see Group 25) |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Omni-Tool | Physical item — Shift+RClick opens mode config chest GUI |
| Omni-Tool modes | Glow, Hardness, Delete, Paint, Area, Eyedrop, Admin |
| Held block glow | Dynamic light emitted from hand when holding glowing custom block |
| Creative tools tab | Clean tab: Omni-Tool, Deleter, Square, Triangle only |
| Tool-give shortcuts | `/cb brush`, `/cb chisel`, `/cb deleter`, `/cb square`, `/cb triangle`, `/cb rectangle`, `/cb hexagon` |
| Mode visual feedback | Omni-Tool changes texture/color per active mode |

---

## Implementation Requirements

### 1. Omni-Tool — Physical Item

- Single item: `customblocks:omni_tool`.
- **Right Click on a block** → performs the active mode's action.
- **Shift + Right Click on a block** → opens the Omni-Tool Config chest GUI.
- The item texture changes dynamically (via model predicates) to reflect the active mode.
- Mode is stored per-player in `config/customblocks/data/magic_items.json`.

**Modes and their behavior:**

| Mode | Icon color hint | Right-click behavior |
|---|---|---|
| Glow | Yellow/gold | Cycle glow: 0→4→8→12→15→0 (Sneak = reverse) |
| Hardness | Gray/iron | Cycle hardness presets: instant→soft→stone→hard→tough→unbreakable |
| Delete | Red | Remove placed custom block instantly (no drop), even unbreakable |
| Paint | Blue | Apply the last-used URL as texture to the clicked block |
| Area | Green | Mark area corners for bulk operations |
| Eyedrop | Purple | Copy texture/attributes from the clicked block to clipboard |
| Admin | Orange | Open admin panel for the clicked block |

### 2. Omni-Tool Config Chest GUI

Opened with Shift+RClick. Contains:
- Mode selector slots (one per mode, click to switch).
- "Configure active mode" slot → opens mode-specific sub-settings.
- "Cycling behavior" slot → customize which values cycle for Glow and Hardness modes.
- "Save default" slot → save current mode as default for this player.

### 3. Held Block Dynamic Glow

Implementation: **Networked Client-Side Mixin** (Decision §7):
1. Server detects when a player is holding a custom block item with `glow > 0`.
2. Server sends a small packet to all nearby clients: "Player X is holding a block with light level Y."
3. Clients render a dynamic light source at the player's hand position using a Mixin.
4. Same effect when placing — light level jumps from hand position to block position.
5. Zero server TPS lag (light rendered client-side only).

### 4. Creative Tools Tab

Tab name: "CustomBlocks Tools"
Contents: Omni-Tool, Deleter Tool, Green Square Marker, Yellow Triangle Marker. Nothing else.
Sort mode configurable in config: `toolTabSort` — `default` (fixed order) or `alphabetical`.

### 5. Tool-Give Shortcuts

All shortcuts give the correct item:

| Command | Item given |
|---|---|
| `/cb brush` | Omni-Tool (Glow mode pre-selected) |
| `/cb chisel` | Omni-Tool (Hardness mode pre-selected) |
| `/cb deleter` | Deleter Tool |
| `/cb square` | Green Square Marker |
| `/cb triangle` | Yellow Triangle Marker |
| `/cb rectangle` | Omni-Tool (Area mode pre-selected) |
| `/cb hexagon` | Omni-Tool (Admin mode pre-selected) |

---

## Setup

```
/cb create g06a GlowBlock
/cb setglow g06a 12
/cb give g06a
```

Place `g06a` nearby. Pick up the block item in your inventory.

---

## Test G06.1 — Omni-Tool give

```
/cb brush
```

**Expected:** Omni-Tool given with Glow mode active. Item name shows current mode (e.g., "Omni-Tool [Glow Mode]").

**Pass:** Item given with correct name and mode.
**Fail:** Wrong item given, error, or missing command.

---

## Test G06.2 — Omni-Tool right-click cycles glow

Hold the Omni-Tool in Glow mode. Right-click on the placed `g06a`.

**Expected:** Glow cycles: `0 → 4 → 8 → 12 → 15 → 0`. Chat or action bar shows current glow level after each click.

**Pass:** Glow increments correctly on each right-click.
**Fail:** Nothing happens, wrong increment, or error.

---

## Test G06.3 — Omni-Tool config chest GUI opens

Hold Omni-Tool. Shift+Right-click on any custom block.

**Expected:** A chest GUI opens with mode selector slots, cycling config, and save default slot.

**Pass:** Chest GUI opens with correct contents.
**Fail:** Nothing happens or error.

---

## Test G06.4 — Omni-Tool mode switch in GUI

In the Omni-Tool config GUI, click the "Hardness" mode slot.

**Expected:** Active mode changes to Hardness. Item texture/name updates to reflect "Hardness Mode". Close GUI and right-click a block — hardness cycles.

**Pass:** Mode switch works, item reflects new mode, right-click cycles hardness.
**Fail:** Mode doesn't change, or right-click still does old mode behavior.

---

## Test G06.5 — Deleter give and use

```
/cb deleter
```

Hold the Deleter. Right-click on the placed `g06a`.

**Expected:** `g06a` is removed instantly. No block drop. Removal is undoable (`/cb undo` restores it).

**Pass:** Block removed instantly. Undo works.
**Fail:** Block not removed, or drop appears.

---

## Test G06.6 — Held block dynamic glow

Hold the `g06a` item (glow = 12) in your main hand. Stand in a dark area (night or underground).

**Expected:** A light level 12 glow radiates from your hand position, illuminating nearby blocks. Other players nearby also see the light.

**Pass:** Dynamic light visible from hand. Other players see it too.
**Fail:** No light emitted from hand.

---

## Test G06.7 — Glow on placement

Hold the `g06a` item. Place it on a wall in a dark area.

**Expected:** As the block is placed, light smoothly transitions from hand position to placed position.

**Pass:** Light follows placement correctly.
**Fail:** No light during/after placement.

---

## Test G06.8 — Creative tools tab contents

Open creative inventory. Navigate to "CustomBlocks Tools" tab.

**Expected:** Tab contains exactly: Omni-Tool, Deleter, Green Square Marker, Yellow Triangle Marker. No custom blocks in this tab.

**Pass:** Only 4 tool items. No blocks.
**Fail:** Old items (Lumina Brush, Chisel as separate items), blocks present, or tab missing.

---

## Test G06.9 — Tool-give shortcuts all work

```
/cb chisel
/cb square
/cb triangle
/cb rectangle
/cb hexagon
```

**Expected:** Each gives the correct item with the correct pre-selected mode.

- `/cb chisel` → Omni-Tool [Hardness Mode]
- `/cb square` → Green Square Marker
- `/cb triangle` → Yellow Triangle Marker
- `/cb rectangle` → Omni-Tool [Area Mode]
- `/cb hexagon` → Omni-Tool [Admin Mode]

**Pass:** All 5 commands give correct items with correct modes.
**Fail:** Any command missing, wrong item, or wrong mode.

---

## Group 06 Verdict

| Test | Description | Result |
|---|---|---|
| G06.1 | Omni-Tool given via `/cb brush` | ⬜ |
| G06.2 | Right-click cycles glow | ⬜ |
| G06.3 | Config chest GUI opens via Shift+RClick | ⬜ |
| G06.4 | Mode switch in GUI works | ⬜ |
| G06.5 | Deleter removes block instantly, undoable | ⬜ |
| G06.6 | Held block emits dynamic glow | ⬜ |
| G06.7 | Glow transitions on placement | ⬜ |
| G06.8 | Creative tools tab has only 4 items | ⬜ |
| G06.9 | All tool-give shortcuts work | ⬜ |

**Group 06 passes when the developer confirms all tools work, glow emits from hand, and the creative tab is clean.**

If anything shows ❌ — paste:
1. The exact command or action
2. What happened vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g06a
```
