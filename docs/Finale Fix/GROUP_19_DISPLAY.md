# Group 19 — Showcase & Hologram Display Systems

> **Prerequisite:** Group 02 (Chest GUI) verified. Phase 4 (Texture Pipeline) verified in-game.
>
> **Objective:** Build the Showcase block system (pedestals, glass cases, custom rotation speeds, vanilla item support) and the Hologram preview system (/cb preview + offhand projection for other players).
>
> **Source issues:** Group J (showcase blocks — full revamp with pedestals, glass cases, rotation), Q1 (hologram system — /cb preview + offhand hologram), Decision §I (ambitious features kept in Phase 17)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Showcase blocks | Basic display block with simple rotation | Missing | Full revamp: pedestals, glass cases, custom rotation speeds |
| Showcase vanilla items | Not supported | N/A | New: any vanilla item can be displayed |
| `/cb showcase` | Existed | Missing | Restored + revamped |
| `/cb showcase config` | Basic config | Missing | Full configuration chest GUI |
| Hologram preview | `/cb hologram` stub | Missing (`hologram` in old command list but not built) | New: `/cb preview <url>` → spinning hologram |
| Offhand hologram | Not present | N/A | New: holding a custom block in offhand projects hologram above hand |
| Hologram config | N/A | N/A | `hologramHeight`, `hologramColor` configurable |

---

## What this group covers

| Feature | Commands |
|---|---|
| Create showcase | `/cb showcase <id>` |
| Showcase config | `/cb showcase config <id>` |
| Preview hologram | `/cb preview <url>` |
| List display types | In showcase config chest GUI |
| Hologram config | `hologramHeight`, `hologramColor` in config |

---

## Implementation Requirements

### 1. Showcase Block System

A Showcase is a special placeable entity or block that displays a custom (or vanilla) block/item as a floating 3D model.

**Display types:**
| Type | Description |
|---|---|
| Pedestal | Stone pedestal base, item floats above it |
| Glass Case | Glass box with the item inside, visible from all sides |
| Open Shelf | Simple flat shelf surface |
| Floating | Just the item, spinning in mid-air (no base) |

**Properties (per showcase):**
- `displayType`: pedestal / glass_case / shelf / floating
- `rotationSpeed`: float, 0 = static, 1 = default spin, higher = faster
- `displayItem`: either a custom block ID or a vanilla Minecraft item ID
- `scale`: 0.5–2.0 (item size multiplier)

`/cb showcase <id>` — creates a new showcase displaying the custom block with ID `<id>`.
`/cb showcase <vanilla-item>` — creates a showcase displaying a vanilla item (e.g., `/cb showcase diamond_sword`).

### 2. Showcase Config Chest GUI

`/cb showcase config <showcase-id>` opens a chest GUI with:
- Display type selector (4 slots: pedestal, glass case, shelf, floating)
- Rotation speed slider (5 slots: static, slow, medium, fast, very fast)
- Item to display (click to change via anvil GUI)
- Scale selector
- "Apply" and "Cancel" slots

### 3. Showcase Persistence

Showcases are not standard blocks — they use Minecraft Armor Stands (or Display Entities for MC 1.19.4+) to render the floating item. Their configuration is stored in `config/customblocks/data/display_blocks.json`.

### 4. Hologram Preview System

`/cb preview <url>` — spawns a spinning hologram at the player's location showing a preview of a block using the texture at `<url>`.
- Hologram disappears after 10 seconds or when the player moves more than 5 blocks away.
- Size: 1×1 block. Spins on Y axis.
- Does NOT create a real block — it is preview-only.
- Config: `hologramHeight` (default 1.5 blocks above ground), `hologramColor` (tint overlay, default none).

### 5. Offhand Hologram Projection

When a player holds a custom block item in their **offhand**:
- A small hologram of that block projects approximately 1.5 blocks above the player's head.
- Other nearby players can see it (renders client-side via networked packet).
- The hologram follows the player as they move.
- Disappears when the offhand slot is emptied.

Config: `offhandHologramEnabled` (default true).

---

## Setup

```
/cb create g20a ShowcaseTest https://i.imgur.com/example.png
```

---

## Test G20.1 — Create showcase (pedestal)

```
/cb showcase g20a
```

**Expected:** A pedestal showcase is created at your location. The `g20a` block floats above a pedestal, spinning slowly.

**Pass:** Pedestal visible with spinning block above it.
**Fail:** Error, or nothing appears.

---

## Test G20.2 — Showcase config GUI opens

```
/cb showcase config g20a
```
(Or right-click the showcase)

**Expected:** Chest GUI opens with display type selector, rotation speed, item selector, and scale slots.

**Pass:** GUI opens with all slots.
**Fail:** Command missing or error.

---

## Test G20.3 — Change showcase to glass case

In showcase config, click "Glass Case".

**Expected:** Showcase visual changes to a glass box encasing the block.

**Pass:** Glass case visible in world.
**Fail:** No visual change.

---

## Test G20.4 — Change rotation speed

In showcase config, click "Static" rotation speed.

**Expected:** Block stops spinning.

**Pass:** Block becomes static.
**Fail:** Block still spins.

---

## Test G20.5 — Showcase with vanilla item

```
/cb showcase diamond_sword
```

**Expected:** A showcase created displaying a diamond sword item. No `g20a` required.

**Pass:** Diamond sword displayed in showcase.
**Fail:** Error, or only custom blocks supported.

---

## Test G20.6 — Hologram preview

```
/cb preview https://i.imgur.com/example.png
```

**Expected:** A spinning hologram block appears at your location using the texture from the URL. Disappears after 10 seconds.

**Pass:** Hologram appears and disappears on schedule.
**Fail:** Nothing appears, or hologram persists permanently.

---

## Test G20.7 — Hologram visible to other players

*(Requires a second player — skip if solo.)*

Run `/cb preview` while a second player watches.

**Expected:** Second player sees the hologram spinning at your location.

**Pass:** Hologram visible to others.
**Fail:** Only the command sender sees it.

---

## Test G20.8 — Offhand hologram projection

Give yourself `g20a` and hold it in your offhand.

**Expected:** A hologram of `g20a` projects approximately 1.5 blocks above your head. Other players nearby see it.

**Pass:** Offhand hologram visible above player's head.
**Fail:** No hologram, or only visible to self.

---

## Test G20.9 — Offhand hologram disappears on item removal

Remove `g20a` from your offhand slot.

**Expected:** Hologram disappears immediately.

**Pass:** Hologram gone.
**Fail:** Hologram persists after offhand slot emptied.

---

## Group 19 Verdict

| Test | Description | Result |
|---|---|---|
| G20.1 | Pedestal showcase created | ⬜ |
| G20.2 | Showcase config GUI opens | ⬜ |
| G20.3 | Switch to glass case | ⬜ |
| G20.4 | Rotation speed change to static | ⬜ |
| G20.5 | Showcase with vanilla item | ⬜ |
| G20.6 | Hologram preview appears and fades | ⬜ |
| G20.7 | Hologram visible to other players | ⬜ |
| G20.8 | Offhand hologram projects above player | ⬜ |
| G20.9 | Offhand hologram disappears on empty | ⬜ |

**Group 19 passes when showcases and holograms both work in-game.**

If anything shows ❌ — paste:
1. The exact command or action
2. What appeared vs what was expected
3. Last 20 lines of `latest.log`

---

## Cleanup

```
/cb delete g20a
```
(Break any placed showcase blocks manually)
