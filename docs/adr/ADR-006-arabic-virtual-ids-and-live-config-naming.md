# ADR-006: Arabic join blocks — virtual ids + live config-driven names; bundled letters mirrored to config

Date: 2026-06-19
Status: Accepted (design locked with dev 2026-06-19; not yet built)

## Context
The Pass-4 auto-join letters are all **one** registered block (`customblocks:arabic_letter`) with the
letter / colour / form carried as NBT (ADR-005). They worked in-game, but:

1. **Names were ugly.** `displayName()` produced `jeem · black (join)` and `jeem · medial · green` —
   lowercase, romanized art-bases (`ta2`, `ha2`), `·` separators, a trailing `(join)`.
2. **No stable id.** A specific variant (this letter, colour, form) had no clean identifier to give,
   search, or list by — the single registry id `customblocks:arabic_letter` covers all of them.
3. The dev also wants the **224 bundled hand-art letters** (currently jar-only `SlotBlock`s) to become
   **editable / deletable** from config, not locked in the jar.

Hard constraint (dev, repeated): **do not touch the 1028 `SlotBlock`s or eat any slots.** Whatever we
add must be zero-registration, zero-slot.

## Decision

### 1. Virtual ids (Way A), not per-variant registration
Keep the single `customblocks:arabic_letter` block. A new small helper computes a **virtual id** and a
**display name** deterministically from the NBT (letter, colour, form). No new block/item
registrations, no new slots, no migration. Commands / search / the join tab resolve a virtual id back
to an NBT stack.

### 2. Naming scheme — isolated is the default and carries NO suffix
| Form | Display name | Virtual id |
|---|---|---|
| isolated | `Jeem Black` | `Jeem_Black` |
| initial | `Jeem Black Ini` | `Jeem_Black_Ini` |
| medial | `Jeem Black Mid` | `Jeem_Black_Mid` |
| final | `Jeem Black Fin` | `Jeem_Black_Fin` |

- Order = **Letter _ Colour _ Form**. Letter is Title-Cased and keeps digits (`Ta2`, `Ha2`).
- Display = clean spaces, no underscores; id = underscores.
- **Isolated gets no form word.** This unifies a bundled "Jeem Black" with a join block sitting alone —
  both read `Jeem Black`, so the 224 already-confirmed bundled names do **not** change (zero churn).
- An **auto-join** placed block shows its **live** form name (isolated → `Jeem Black`, after a left
  neighbour joins → `Jeem Black Mid`, etc.).
- **Numbers never join** → no forms → they keep simple names (`A0 Black`, `E5 Black`), no form word.

### 3. Live, config-driven form labels
The three connected-form words are **config values**, default `Ini` / `Mid` / `Fin` (isolated has no
label). Editable in `/cb config` (**both** the GUI and the command). Changing a label (e.g. `Ini → Init`)
re-labels **every** block — held, placed, in chests — with no re-stamp pass, because names are
**computed at display time** (overriding the item's `getName` + the block's HUD line), never baked into
the stack. **No resource-pack reload** is needed for a name change — labels touch text only, not
textures.

### 4. Name on a placed block via the CB HUD
Join blocks are wired into the existing CustomBlocks HUD, so looking at a placed join block shows its
live name (e.g. `Jeem Green Mid`), matching the bundled letters.

### 5. Bundled 224 → config mirror, recoverable
The 224 bundled hand-art letters are mirrored into config so the dev can edit / **delete** them. The
original 224 stay in the jar as a **fallback master copy**, and a `/cb` restore command brings deleted
defaults back — **delete is recoverable**, never destructive.

## Rationale
- Virtual ids give clean identity + give/search/list without the cost (registrations, slots, migration)
  of real per-variant blocks — honouring the zero-slot constraint.
- "Isolated = no suffix" is the dev's own insight: it makes bundled and join blocks read identically and
  avoids re-naming the 224 confirmed blocks.
- Computing names at display time is what makes "change one label, everything updates automagically"
  actually work, and it sidesteps a reload because text ≠ texture.
- Mirroring (not moving) the bundled art keeps a safety net, so a mis-click delete is always reversible.

## Consequences
- Adds an Arabic naming/id helper + a small `*Config` block for the 3 form labels (≤300 lines per the
  file-size gate). Item `getName` / HUD now compute names instead of reading a baked `CUSTOM_NAME`.
- The bundled-letter mirror introduces a config-side store for the 224 (separate from `SlotDataStore`
  and the 1028 slots) plus a restore command — its own build step.
- Changing a form label changes the **virtual id** too (`Jeem_Black_Ini → Jeem_Black_Init`); fine
  because ids are computed, not persisted, so nothing dangles.
- Builds on ADR-003 (forms) and ADR-005 (live-texture block). Naming/id is display-layer only; it does
  not alter the join brain or the texture path.
