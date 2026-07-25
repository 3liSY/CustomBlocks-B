# Group 14 - Animation, Video, and Display Blocks

> Group 14 turns CustomBlocks into crisp, safe display blocks: animated images, later video and live data, and reliable rendering that does not trade quality for a hidden memory problem.

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_14.md) · [All Groups](README.md)

[Direction](#direction) · [Decisions](#locked-decisions) · [Plan](#feature-plan) · [Connections](#cross-group-contracts) · [History](#superseded-decisions)

---

## Purpose

An animated or display block must preserve its source timing, look crisp when placed, remain valid after retexture and restart, and share rendering work between repeated copies of the same block. The platform also provides the future foundation for video walls, live data, triggers, and showcase displays.

G14 owns rendering/data engines, source recovery, animation-aware texture mutation, and display behavior. G27 owns all animation/video/editor Screen design and routing.

## Ownership

| Owns | Does not own |
| --- | --- |
| Animation decoding, `AnimData`, renderer/cache behavior, timing, and performance limits | Animation Studio, timeline, and animation hub Screens: G27 |
| Animation-aware retexture and static/animated conversion | Pack delivery to clients: G05 |
| Source Wall and source-quality recovery workflow | Image decoding/source validation rules: G10 and Information guide |
| Display-block engine: showcase, video wall, live data, reactive behavior | General tools and block mutation: G06 |
| Cutout render-layer registration for G10 background transparency | Stored background attribute and re-bake policy: G10 |

## Direction

Placed animated/display blocks use an owned client texture path with one current frame per distinct block ID, millisecond timing, and bounded cache/pool behavior. This avoids atlas blur, hard 20 TPS frame stepping, and a full GPU texture grid for every frame. Hand/inventory representation stays a separate renderer decision rather than being confused with world-block quality.

The animation UI is one G27 Studio flow. G14 exposes clean data and mutation services to it; it does not retain chest editors, a separate `/cb video` command surface, or prototype renderer clusters.

## Locked Decisions

| Date | Decision | Effect |
| --- | --- | --- |
| 2026-06-20 | Old ScreenTest/prototype renderer clusters are retired. | New display rendering uses a dedicated, maintainable cache and block-entity renderer path. |
| 2026-06-21 | Retexture must recognize animated sources and update `AnimData` with texture state. | Static-to-animation, animation-to-static, and animation-to-animation cannot leave stale frame metadata. |
| 2026-06-21 | Placed animation uses current-frame texture upload with bounded resources. | Copies of one ID share work; frame count does not force every frame into GPU memory. |
| 2026-06-21 | Animation timing uses wall-clock milliseconds. | Fast source playback is no longer limited to Minecraft tick stepping. |
| 2026-06-21 | `/cb animation` is the single animation front door. | Old `/cb anim`, `/cb video`, and typed standalone editor paths do not become new UI surfaces. |
| 2026-06-25 | Showcase cycles a logical filtered pool in sync where selected. | A wall/display system is a renderer/data feature, not a pack rebuild loop. |
| 2026-06-28 | Image resize uses premultiplied-alpha Lanczos with guarded sharpening. | Small sources improve without introducing bright ringing or transparent-edge halos. |
| 2026-07-09 | Animation/editor Screen content belongs to G27. | G14 retains only engine, command routing, and data contracts. |
| 2026-07-12 | Video import, video walls, live data, and reactive behavior are later phases. | The core animation reliability path is not delayed by the wider display platform. |
| 2026-07-18 | G14 exposes saved animated/static identity to G27's shared block-browser data. | The `Animated` Screen filter recognizes GIF and animated WebP blocks without inspecting filenames. |
| 2026-07-18 | Bare `/cb animation` routes through G27's shared browser with `Animated` selected. | Choosing a result opens that block in `/cb create` on the Animation tab; the old chest picker can retire after cutover. |

## Feature Plan

### A. Animation-Aware Retexture

**Player outcome**

Retexturing a block with a GIF/WebP or a still image changes its behavior cleanly, with no garbled faces or stale playback state.

**Experience**

- A static block retextured from an animated source becomes animated.
- An animated block retextured from a still becomes a normal static block.
- Replacing one animation with another updates frame count, timing, loop data, and texture together.
- An invalid source leaves the prior valid state intact and explains the problem clearly.

**Requirements**

- The retexture rail detects animation using the same source path as create.
- `AnimData` is created, replaced, or cleared atomically with texture files and pack metadata.
- A failed decode cannot partially replace a slot texture or leave a false animated flag.
- Restart serialization restores the matching image and animation metadata.

**Boundary**

This is data correctness before visual polish. It does not require a Screen redesign.

### B. Crisp, Bounded World Rendering

**Player outcome**

Placed animated blocks look close to their source and play at the intended pace without consuming GPU memory per frame or per placed copy.

**Experience**

- Close world views are crisp rather than atlas-muffled.
- Fast animations follow millisecond timing instead of a 20-frame-per-second ceiling.
- Loop, bounce, reverse, and later play-once behavior use the same animation data model.
- Many copies of one animated ID share decoded/rendered resources.
- Off-screen and distant work can pause or reduce under the approved performance governor.

**Requirements**

- `AnimFrameCache` maintains one owned current-frame texture per active animated slot, not a full frame grid on the GPU.
- `AnimSlotBER` renders the placed block from the owned texture path and respects block shape, face overrides, and glow where supported.
- Cache eviction/pooling bounds GPU and RAM use by active/visible distinct IDs.
- Animation timing derives from elapsed milliseconds and saved `AnimData` rather than game ticks alone.
- Client renderer failures fall back safely without corrupting server block data.

**Boundary**

This guarantees placed-world rendering. Item/hand renderer scope is a separate decision and must not be implied by a world-only block entity renderer.

### C. Source Quality and Recovery

**Player outcome**

Old blurry sources can be reviewed safely, while all newly created/retextured images receive the best safe resize path available in the mod.

**Experience**

- Small input images receive a non-blocking quality warning.
- Resize preserves sharp edges and clean alpha without white rings or dark fringes.
- Source Wall shows only review targets and never touches player builds outside the temporary review placements.
- A creator can re-source one target, undo it, and tear down only the wall created by the workflow.

**Requirements**

- Resampling uses premultiplied-alpha Lanczos and applies light sharpening only when enlarging.
- Anti-ringing and anti-fringe clamps preserve subject pixels while preventing generated halos.
- Source Wall skips generated, Arabic, and simple recolor variants that do not need original-source review.
- The workflow records only its own temporary placements and persists enough review state to resume safely.
- General link/file support follows `docs/Information/IMAGE_INPUT_OVERHAUL.md` rather than a parallel downloader design.

**Boundary**

The renderer improves supplied images; it cannot invent detail that a tiny source never contained.

### D. Display Platform Extensions

**Player outcome**

Future display blocks can cycle a collection, show video or live data, and react to the world without rebuilding a resource pack for every frame.

**Experience**

- Showcase cycles a configured filtered set, optionally in a synchronized order.
- Video import accepts supported animated/video sources through a single decode pipeline.
- A video wall presents one logical screen across tiles with fit behavior and a single performance budget unit.
- Live data, redstone triggers, right-click controls, channels, and emissive behavior build on the same per-block display data.

**Requirements**

- Showcase data is persisted separately from ordinary block texture data and renderer state syncs only the needed selection/timing information.
- Video decoding checks for the optional ffmpeg route and uses supported animated-image fallback when absent.
- Auto performance mode accounts for active display IDs, visibility, distance, and client frame budget.
- Reactive behavior uses explicit `SlotData` fields and server neighbor/update hooks, not client-only guessed state.

**Boundary**

These are later platform slices. They must not compromise the core retexture and animation renderer path.

### E. Cutout Rendering for Backgrounds

**Player outcome**

When G10 enables a transparent background, alpha really shows through while ordinary black-background blocks remain visually stable.

**Experience**

- Transparent pixels render through on supported custom blocks.
- Existing black-filled blocks do not unexpectedly become transparent.
- Shaped blocks preserve clean interior/edge behavior.

**Requirements**

- G14 registers slot blocks on the correct cutout render layer in the client initializer.
- The change works with G10's stored per-block background value and G08 shape models.
- No new independent transparency setting is created here.

**Boundary**

G14 only supplies render-layer registration. G10 owns background data and texture re-bake decisions.

**Dependency state**

G10's half of this contract is in place: `SlotData.background` stores the choice and `BackgroundService` re-bakes it, with `black` and colour values working. `transparent` parses and is a valid stored value, but `/cb setbg` refuses it and reports that the render layer is missing, because no cutout registration exists for slot blocks yet and `SlotBlock` is not non-opaque. This section is therefore the sole remaining blocker for G10 §B's transparent option. Note that the client already flattens alpha onto black for off-atlas blocks (`OffAtlasImage`); that behavior has to be reconciled with a real transparent option rather than left to fight it.

## Cross-Group Contracts

| Group | Connection | Promise |
| --- | --- | --- |
| G05 | Pack delivery | G14 produces coherent texture/data changes; G05 delivers generated resources without duplicating renderer work. |
| G06 | Tool mutation | Tools trigger normal texture/attribute changes and do not bypass animation state updates. |
| G08 | Shapes | Display rendering honors supported block shapes and face mapping. |
| G10 | Images and transparency | G10 owns source/background rules; G14 supplies renderer and cutout-layer needs. |
| G15 | AI/video-adjacent work | Any AI-heavy image/video process stays separately gated. |
| G27 | Animation screens | G27 owns animation hub, timeline, previews, and layout; G14 owns engine data and actions. |

## Technical Contract

- Texture mutation treats image bytes and `AnimData` as one transaction.
- A placed animation renderer uses an owned current-frame texture and millisecond playback clock, shared per distinct slot ID.
- Resource limits are based on active/visible display IDs, never raw placed-copy count or total source-frame count alone.
- Static and animated rendering paths identify their state from saved slot data and fail without corrupting it.
- Source processing uses the shared image-input plan and guarded Lanczos pipeline.
- Showcase/video/live data state is explicit data, not a succession of server-side pack rebuilds.
- G27 screens call G14 engine routes; no chest editor or retired command becomes a second source of truth.

## Deferred Scope

<details><summary>Future ideas outside this Group's current plan</summary>

| Idea | Why it is deferred | Owner if revived |
| --- | --- | --- |
| Universal video import | Needs optional ffmpeg behavior and robust animated-image fallback. | G14 |
| Video wall | Needs multiblock mapping, fit behavior, and Auto-performance protection. | G14 with G27 |
| Live data and reactive displays | Needs defined data sources, triggers, and performance boundaries. | G14 |
| Custom item renderer everywhere | Requires a separate inventory/hand rendering decision. | G14 |

</details>

## Superseded Decisions

<details><summary>Historical decisions kept only so old work does not return</summary>

| Date | Old direction | Current direction |
| --- | --- | --- |
| 2026-06-20 | ScreenTest and mixed prototype renderer clusters were a foundation. | They are retired for a dedicated cache and renderer system. |
| 2026-06-21 | Retexture treated every source as a still image. | Retexture updates animation and texture state together. |
| 2026-07-09 | G14 owns animation editor layout and multiple menu surfaces. | G27 owns the one animation Studio flow. |
| 2026-07-09 | `/cb anim` and standalone video commands are the public workflow. | `/cb animation` is the single Screen hub. |

</details>

## References

[Dashboard](../testing/Dashboard.md) · [Testing Guide](../testing/Testing_Guide_14.md) · [All Groups](README.md)

- [G05 Resource Pack Delivery](GROUP_05_RESOURCE_PACK.md)
- [G06 Tools and Block Interaction](GROUP_06_TOOLS.md)
- [G08 Shapes and Per-Face Textures](GROUP_08_SHAPES.md)
- [G10 Color and Image Tools](GROUP_10_COLOR_IMAGE.md)
- [G15 AI Textures](GROUP_15_AI_TEXTURES.md)
- [G27 Screens](GROUP_27_SCREENS.md)
- [Image Input Overhaul](../Information/IMAGE_INPUT_OVERHAUL.md)
- [Pre-template Group 14 snapshot](../archive/group-migration-2026-07-18/GROUP_14_ANIMATION_VIDEO.md)
