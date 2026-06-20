# ADR-004: Named-texture mirror hooks at the single-writer choke points (not per-command)

Date: 2026-06-15
Status: Accepted

## Context

Group 26 Part C adds an optional, write-only `textures_names/` folder — a human-readable copy of every
block's texture named by its display name (`Neptune Red.png`) instead of `slot_N.png`. The mirror must
stay in sync with the blocks: it has to update whenever a block is **created, retextured, face-painted,
face-cleared, renamed, or deleted**, and on undo/redo.

The Part C spec enumerated the trigger points as a list of *command handlers* to edit: `CreationCommands`,
the retexture flow, `FaceCommands`, the name-edit path, and `SlotManager` delete.

While implementing, a grep showed `TextureStore.save(...)` is actually called from **~20 sites** — not
just the four the spec named, but also `ColorImageCommands`, `ColorToolService`, `ColorVariantService`,
`VideoCommands`, `GradientPickerMenu`, `HistoryCommands` (undo/redo), `SafetyCommands`,
`ArabicBlockRegistry`, `SlotManager.dupe`, and `TrashCommands` (restore). `slot_N.png` is the canonical
on-disk byte store for **every** block, including Arabic art. Editing each command site would (a) miss
several real edit paths (colour tools, video, gradient, undo), and (b) scatter mirror calls across a dozen
files, each a place to forget the hook in future work.

## Decision

Hook the mirror at the **single-writer choke points** instead of at each command:

- **`TextureStore`** — the sole reader/writer of texture bytes (per its own header + design rule for
  texture I/O): `save` / `saveFace` / `deleteFace` → `TextureNameMirror.syncSlot(index)`;
  `delete` → `TextureNameMirror.removeSlot(index)`.
- **`SlotManager`** — the single source of truth for slot identity (design rule #3): `rename` and
  `restoreSnapshot` → `syncSlot(index)`, covering name changes and undo/redo, which do **not** write bytes.

Six call sites in two existing files. `TextureNameMirror` (new, in `core`) owns the folder + a
`mirror_index.json` manifest, is flag-gated on `CustomBlocksConfig.mirrorNamedTextures` (off by default),
and swallows its own errors.

## Rationale

- **Can't-miss coverage.** Every texture edit already funnels through `TextureStore`; every name/identity
  change funnels through `SlotManager`. Hooking the funnels catches all current paths *and* any future
  command that uses the canonical writers — without that command having to remember to call the mirror.
- **Respects the architecture rules** (§5 #3/#4: `SlotManager` and the I/O stores are the choke points)
  rather than working around them.
- **Smaller blast radius** than the per-command plan: 6 lines in 2 files vs. edits scattered across ~10
  handlers, several of which the spec's list omitted entirely.
- **Safe ordering.** In every create flow the `SlotData` (and its name) is committed *before* the texture
  is written, so by the time `TextureStore.save` fires the hook, `SlotManager.getBySlot` returns the real
  name — the spec's "first write must not land nameless" requirement holds for free.

## Consequences

- **Trade-off: occasional redundant syncs.** A full delete fires `syncSlot` from the per-face delete loop
  (only when face overrides exist) and then `removeSlot`; the redundant calls are cheap, idempotent, and
  guarded by the flag. A bulk retexture *while the mirror is on* re-mirrors each block (O(n) file writes);
  acceptable since the mirror is opt-in and `rebuild` exists to batch-regenerate.
- **`TextureStore` now depends on `TextureNameMirror`** (same `core` package). The dependency is one-way
  and the mirror never calls back into `TextureStore.save`, so there is no recursion.
- **Self-healing.** Because the manifest tracks every file, renames/deletes clean up their old files, and
  `/cb config mirrornames rebuild` regenerates the whole folder from truth — any drift is one command away
  from fixed.
