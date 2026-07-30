# Group 19 - Showcase and Hologram Displays

> Group 19 lets operators place persistent, polished block showcases and temporary hologram previews without faking a floating block as a dropped item.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_19.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

Showcases make real CustomBlocks and vanilla content presentable in a world. They persist safely, stay configurable, and have explicit performance limits. Hologram previews let an operator inspect content temporarily before choosing to pin it.

G19 owns showcase commands, stored instances, display-entity behavior, hologram lifecycle, and configuration semantics. G27 owns the showcase configuration Screen.

## Ownership

| Owns | Does not own |
| --- | --- |
| Showcase instances, display entities, persistence, commands, and management | Showcase configuration Screen framework: G27 |
| Hologram preview, pin/unpin, and offhand projection behavior | Animated texture/render implementation: G14 |
| Showcase presets, groups, cap, and gallery logic | Block creation and normal item tools: G06 |
| Display-specific feedback/particles | Vault transport for future config sharing: G20 |

## Direction

Use Minecraft display entities: `BlockDisplay` for custom/vanilla blocks and `ItemDisplay` for vanilla items. Animated custom blocks use the item-render route because a `BlockDisplay` cannot invoke the custom animation renderer. Every saved showcase is an explicit persisted instance with an automatic ID and a server-configurable performance cap.

Build in slices: core persistence first, then Screen controls, multi-content, presets/cap, management, hologram preview, and finally offhand projection. Each slice is independently useful and must not be bundled into one risky build.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-21 | G19 fully owns showcases. | Other Groups may link to showcases but do not create competing behavior. |
| 2026-06-21 | Blocks use `BlockDisplay`; vanilla items use `ItemDisplay`. | A showcase is a real floating block/state, not dropped-item visual trickery. |
| 2026-06-21 | Sky/no-target placement falls back two blocks ahead of the player's view. | Placement never fails merely because no surface is aimed at. |
| 2026-06-21 | Create, edit, remove, and bulk showcase actions are operator-only. | Decorative world controls cannot be used by ordinary players. |
| 2026-06-21 | Showcases persist atomically in `display_blocks.json` and respawn on relevant load. | Restart/chunk load does not lose a placed display. |
| 2026-06-21 | G19 starts with pedestal and floating types only. | Glass cases, shelves, tumble, orbit, and per-showcase glow color remain parked. |
| 2026-06-21 | Right-click cycles content; Shift-right-click opens configuration. | Direct interaction stays simple and removal is never an accidental click. |
| 2026-06-21 | A gallery may hold a chosen list or dynamically include all CustomBlocks. | New blocks join the dynamic gallery without manual list edits. |
| 2026-06-21 | Hologram previews are temporary by default: ten seconds or five blocks of distance. | Preview cannot become an accidental permanent showcase. |
| 2026-06-21 | Offhand hologram is the final slice. | It does not delay persistent showcase and preview behavior. |
| 2026-07-10 | Showcase configuration is a Screen. | G19 does not create a chest configuration UI. |
| 2026-07-30 | G19's command root is `/cb hologram`, not `/cb showcase`. | `showcase` is already taken by G30 as `/cb guess showcase` (the quiz display). G19 keeps the word "showcase" for its feature and data, but the command the owner types is `hologram`, so the two features can never shadow each other in the command tree or in tab-completion. G30's command is unchanged. |

## Feature Plan

### A. Showcase Core

**Player outcome**

An operator can place a persistent pedestal or floating showcase that spins smoothly or remains static.

**Experience**

- `/cb hologram <id>` places at the looked-at surface or the safe sky fallback.
- Scale ranges from compact display through large statue-like presentation.
- Spin supports a true no-spin setting.
- `/cb hologram remove` targets the looked-at showcase or explicit instance ID.
- Restart and chunk load recreate the same saved display.

**Requirements**

- `ShowcaseDataStore` writes atomically under `config/customblocks/data/display_blocks.json`.
- Instance IDs are automatic and stable.
- Stored position, type, rotation, scale, visibility, contents, and configuration restore together.
- Display entity spawn/removal cannot leave orphaned saved data or orphaned world entities.

**Boundary**

Core showcase handles one persisted display instance. It does not include shop, redstone, proximity, or group behavior yet.

### B. Configuration and Content

**Player outcome**

Operators can style a showcase, make it recognizable, and choose one or many display contents.

**Experience**

- Appearance offers glow outline, fullbright, hover bob, scale, label, and aura style.
- Motion offers static/spin and speed.
- Display controls select a list or dynamic all-CustomBlocks gallery, with right-click/manual or automatic cycling.
- A label uses block name by default and can become custom text.
- `/cb hologram item` supplies a furniture-style placer in the Tools tab.

**Requirements**

- G27 Screen tabs organize Appearance, Motion, and Display without duplicating saved data logic.
- Auto-cycle has a minimum 0.1-second interval and is included in performance accounting.
- Animated custom content routes through the appropriate item-render path; vanilla blocks/items use native display entities.
- The current config target is always an explicit looked-at/instance-resolved showcase.

**Boundary**

Configuration updates one persisted instance. It does not silently apply a global style unless a preset/group action is chosen.

### C. Presets, Management, and Limits

**Player outcome**

An operator can reuse a design, find/manage showcases, and avoid placing enough active displays to hurt a server/client.

**Experience**

- Named presets and one server default speed up new showcases.
- List/teleport, locate, edit-nearest, rename, clone, and move make individual management practical.
- Radius actions apply a preset or remove intentionally selected showcases.
- Hide/show retains data; lock prevents configuration edits; groups synchronize selected settings.
- A cap warns before display density, particles, cycling, or animation becomes excessive.

**Requirements**

- Presets and groups are separate persisted records with clear instance references.
- Bulk actions report scope and results and use a deliberate confirmation where destructive.
- Cap calculations account for active display cost, not only raw instance count.
- Future export/share serializes a showcase configuration, never a live world entity identity.

**Boundary**

Management does not turn Showcase into a player economy/shop system.

### D. Hologram Preview and Projection

**Player outcome**

An operator can preview a block or image temporarily, compare two, pin an intentional display, and later project the offhand block.

**Experience**

- `/cb preview <id>` is fast; URL preview uses a temporary safe render/download route.
- Preview disappears after the time/distance rule, unless pinned.
- Preview can target another player or compare two sources side by side.
- Pinned previews become explicit baseless persistent displays; unpin removes them.
- Offhand projection appears above the holder, follows them, is visible to others, and vanishes with an empty offhand.

**Requirements**

- Temporary preview state is isolated from persistent showcase records until Pin.
- Bad URL handling cleans up without leaving a broken display.
- Config stores hologram height/color and offhand enablement separately from showcase instances.
- Projection use is counted within the performance cap.

**Boundary**

Offhand projection comes after Showcase and Preview are reliable; it is not a shortcut around their data/performance model.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Block assets | G19 shows already available block/item assets and never rebuilds packs per display tick. |
| G06 | Tools tab | Showcase placer follows the Tools tab and normal item behavior. |
| G14 | Animated content | G14 supplies the animation renderer; G19 uses the compatible item-display route where required. |
| G16 | Feedback and diagnostics | Showcase aura uses feedback conventions and display failures can become diagnostic incidents. |
| G20 | Config sharing | G20 transports future showcase config codes; G19 validates/applies the data. |
| G27 | Config Screen | G27 owns UI layout; G19 owns every stored display action. |

## Technical Contract

- Showcase records persist atomically under `display_blocks.json`, use automatic instance IDs, and recreate native display entities on server/chunk load.
- Block content maps to `BlockDisplay`; vanilla item content maps to `ItemDisplay`; animated custom blocks use the compatible item-render route.
- Placement resolves a target surface or safe forward fallback before creation.
- Interaction resolves one showcase by entity/instance identity before mutation.
- Temporary previews do not write persistent data until Pin.
- Cap/load accounting includes active effects, auto-cycle, and animated content.
- G27 Screen actions call G19 server services; client UI does not own showcase state.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Shop mode | Needs a separate inventory/economy contract. | G19 with economy owner |
| Redstone/proximity behavior | Follows the core display platform and defined behavior fields. | G19 |
| Glass case, shelf, tumble, orbit | Core pedestal/floating interaction and performance come first. | G19 |
| Per-showcase glow color | One default outline color is sufficient initially. | G19 |
| Offhand projection | Final slice after showcase and preview stability. | G19 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-21 | Showcase could use a dropped-item-like block visual. | Native display entities represent blocks/items correctly. |
| 2026-06-21 | Hologram preview could become permanent without an explicit action. | Preview is temporary; Pin creates the persistent form. |
| 2026-07-10 | Showcase configuration is a chest GUI. | It is a G27 Screen. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_19.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G14 Animation and Video](GROUP_14_ANIMATION_VIDEO.md)
- [G16 Diagnostics and Private Testing](GROUP_16_DIAGNOSTICS.md)
- [G20 External Integrations](GROUP_20_EXTERNAL_INTEGRATIONS.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Pre-template Group 19 snapshot](../archive/group-migration-2026-07-18/GROUP_19_DISPLAY.md)
