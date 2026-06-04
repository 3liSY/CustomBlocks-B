# ADR-001: Pre-register a fixed pool of 1028 slot blocks instead of dynamic registry

Date: 2026-06-03
Status: Accepted

## Context
The mod must let players create "new" blocks at runtime with custom textures and
attributes. Minecraft/Fabric block registries are **frozen after mod initialization** —
you cannot safely add new block entries while the game is running. Any mismatch
between the set of registered blocks on the server and on a client produces registry
sync errors and disconnects.

## Decision
At mod init, pre-register a fixed pool of exactly **1028 generic `SlotBlock`
instances** (`slot_0` … `slot_1027`). Each slot is a real, registered block from the
game's point of view. At runtime, a slot is *assigned* a `SlotData` object that
defines its texture(s), attributes, and behaviour. "Creating a block" means claiming
a free slot and assigning it `SlotData`; "deleting" frees the slot. `SlotManager` is
the single source of truth for which slots are free, occupied, or reserved.

## Rationale
- The registry is identical on every client and server, so there are **no registry
  mismatch errors** (Success Criterion in §1) and no restarts are required.
- Runtime changes touch only `SlotData` (data) and resource-pack/texture delivery,
  never the registry itself.
- A flat, immutable `SlotData` model is easy to snapshot, undo/redo, and persist.

## Consequences
- Hard cap of 1028 custom blocks at once (acceptable per the SRS; FR-01-1, NFR-03).
- A small, fixed memory/registration cost for unused slots. Startup time must be
  benchmarked in Phase 1; if slow, registration can be batched (Risk Register).
- Requires `SlotManager` + `SlotDataStore` to track and persist assignments, and a
  texture-delivery pipeline (resource pack / client generation) since the block model
  itself is generic.
