# PHASE 0 FEATURE COMPARISON

**Purpose:** Compare how the same Phase 0 features are implemented in CustomBlocks-B vs original CustomBlocks.

**Note:** Only Phase 0 features are compared. Phase 6+ features are ignored.

---

## 1. SLOT SYSTEM (1028/800 Pre-registered Blocks)

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Registration Method** | `registerAll(maxSlots)` — pre-registers all blocks at startup | `assign(customId, displayName, texture)` — dynamic assignment on demand | ✅ B pre-allocates; Original allocates on use |
| **Slot Count** | 800 (configurable, default) | 600 (different default) | ⚠️ Different defaults |
| **Block Creation** | `new SlotBlock(index, settings)` in loop | `SlotData.createTrusted(idx, customId, displayName, texture)` | ✅ B is simpler |
| **Storage** | `blocks[]` and `items[]` arrays | `byId` map + `bySlot` map | ✅ B uses arrays; Original uses maps |
| **Texture Handling at P0** | No texture assignment | Async texture file writing to `.dat` file | ✅ B defers textures; Original handles immediately |
| **Duplicate Prevention** | N/A (pre-allocated) | Explicit duplicate check in `assign()` | ✅ Both prevent duplicates |
| **Free Slot Finding** | Not needed (array-based) | `findFreeSlot()` scans `freeSlotIndices` set | ✅ B is more efficient |
| **Luminance Cache** | Dynamic via block properties | Pre-calculated `lightCache[]` array | ✅ Both cache luminance |
| **Persistence on Disk** | Via `SlotDataStore.save()` | Via `loadAll()` which parses JSON + batches deserialization | ✅ Both persist; Original batches loads |

**Verdict:** CustomBlocks-B is **cleaner and simpler** at P0 (pre-allocates without textures). Original is **more complex** (handles textures at P0, which belongs to Phase 4).

---

## 2. TOOL ITEMS REGISTRATION

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Tools Registered** | 3 tools | 12+ tools | 🔴 B is missing tools |
| **Tools in B** | • Lumina Brush<br>• Chisel<br>• Deleter | • Color Squares (Black, Yellow, Green, Red, Custom)<br>• Color Triangles (RGB configurable)<br>• Rainbow Rectangle<br>• Golden Hexagon<br>• Lumina Brush<br>• Amethyst Chisel<br>• Diamond Triangle<br>• Deleter<br>• Tab Icon<br>• Showcase Display | 🔴 B missing decorative tools |
| **Stacking** | maxCount(1) — non-stackable | Varies (most non-stackable) | ✅ Both handle properly |
| **Item Names** | "lumina_brush", "chisel", "deleter" | Varies per item | ✅ Both use proper IDs |
| **Registration Timing** | Phase 0 | Phase 0 | ✅ Both at P0 |
| **Phase Belongs To** | Phase 7 (Tools) | Phase 7 (Tools) | ⚠️ Both register Phase 7 stuff at P0 |

**Verdict:** CustomBlocks-B **has only 3 tools (1/4 of original)**. Original has **12+ tools for creative/decoration**. B is **incomplete** unless decorative items are moved to Phase 7.

---

## 3. CONFIGURATION SYSTEM

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **File Size** | 7 KB | 44 KB | ✅ B is much smaller |
| **Phase 0 Fields** | 4 fields:<br>• maxSlots (800)<br>• httpPort (8123)<br>• textureSize (64)<br>• httpHost ("127.0.0.1") | Same 4 fields:<br>• maxSlots (600)<br>• httpPort (8123)<br>• textureSize (64)<br>• httpHost (set as server IP) | ✅ Both have core P0 fields (different defaults) |
| **Phase 6 Fields (Undo)** | 2 fields:<br>• maxUndoDepth (25)<br>• undoMode ("global") | Same 2 fields | ✅ Both defer to P6 |
| **Phase 11 Fields (HUD)** | 1 field:<br>• hudEnabled (true) | Same field | ✅ Both in config |
| **Phase 13 Fields (AI)** | 2 fields:<br>• aiApiKey ("")<br>• aiTextureEnabled (false) | Multiple fields:<br>• aiApiKey<br>• aiApiProvider<br>• aiMaxVariations<br>• aiTextureStyle<br>• voiceMode | ⚠️ B is minimal; Original is extensive |
| **Phase 14 Fields (Cloud/Discord)** | 2 fields:<br>• vaultEndpoint ("")<br>• discordWebhookUrl ("") | Same 2 fields (plus cloud secret) | ✅ Both minimal |
| **Phase 4 Fields (Image Processing)** | ❌ NOT in P0 config | 10+ fields:<br>• defaultTextureSize<br>• bgRemovalTolerance<br>• bgRemovalUseYcbcr<br>• bgRemovalAutoDetect<br>• shadowThreshold<br>• downloadTimeoutSeconds<br>• maxGifSizeMb | 🔴 **B missing Phase 4 config**<br>✅ Original has it (but premature) |

**Verdict:** CustomBlocks-B **respects phase boundaries** (small, minimal). Original **mixes all phases** (large, bloated). However, **B might be missing Phase 4 image processing config** that should be added when Phase 4 is implemented.

---

## 4. HTTP RESOURCE PACK SERVER

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Startup Method** | `start()` tries ports: configured, then 8124, 8081, 24454, 3000 | `start()` tries ports: configured, then 8081, 24454, 8082, 3000 | ⚠️ Different fallback order |
| **Primary Endpoint** | `/pack.zip` — serves compiled resource pack | `/pack.zip` — serves compiled resource pack | ✅ Identical |
| **404 Response** | "warming up" message | "Pipeline warming up..." message | ✅ Similar error messages |
| **Content-Type** | `application/zip` | `application/zip` with CORS header `Access-Control-Allow-Origin: *` | ⚠️ B might be missing CORS header |
| **Secondary Endpoint** | `/export/` — serves per-block JSON exports | `/exports/` — serves PNG screenshots | ⚠️ Different export types |
| **Path Traversal Protection** | Rejects `..`, `/`, `\` | Rejects `..`, `/`, `\` | ✅ Both protect |
| **Pack Generation** | Queues background build via `PACK_BUILDER` executor | Queues via `updatePack()` | ✅ Both use background threads |
| **Hash Computation** | SHA-1 hash computed after build | SHA-1 hash computed and broadcasted | ✅ Both hash for verification |
| **Client Notification** | `ResourcePackSendS2CPacket` with pack URL, hash, prompt | `ResourcePackSendS2CPacket` with same info | ✅ Identical packet structure |
| **Pack URL Format** | `http://httpHost:activePort/pack.zip` | `http://httpHost:activePort/pack.zip` | ✅ Identical URL format |

**Verdict:** **Essentially identical**. Minor differences: B missing CORS headers (possible), different export endpoint types. Both correctly implement resource pack distribution.

---

## 5. COMMAND REGISTRATION

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Root Command** | `/customblock` alias `/cb` | `/customblock` alias `/cb` | ✅ Identical |
| **Registration System** | `CommandRegistrar.register()` | `CustomBlockCommand` + `CommandRegistrar` | ✅ Both use registrar pattern |
| **Tab Completion** | ✅ Supported | ✅ Supported | ✅ Both have tab completion |
| **Command Handlers** | Not specified at P0 | Delegate to handler classes (CreationCommands, PropertyCommands, etc.) | ⚠️ B's handler structure unclear |
| **Error Handling** | Basic (from logs) | `DidYouMean` suggests corrections for typos | ✅ Original has better error UX |
| **Help System** | `/cb help` available | `HelpRegistry` maps commands to help entries | ✅ Original more structured |

**Verdict:** **Both register `/cb` tree**. Original has **better error handling and help system**. B's handler architecture unclear from logs.

---

## 6. BLOCK LOADING & PERSISTENCE

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Save Timing** | `SlotManager.save()` on shutdown | `SlotManager` saves to JSON asynchronously | ✅ Both save blocks |
| **Save Location** | `SlotDataStore` handles JSON I/O | JSON parsing in `loadAll()` | ✅ Both use JSON |
| **Serialization** | Write-to-temp-then-rename (atomic) | Atomic file operations (mentioned in docs) | ✅ Both atomic |
| **Load Timing** | `SlotManager.loadAll()` at startup | `loadAll()` batches deserialization across ticks | ✅ Both batch loads |
| **Crash Safety** | Atomic ops prevent mid-save corruption | Same atomic ops | ✅ Both crash-safe |

**Verdict:** **Identical approach**. Both use atomic JSON persistence and batch loading.

---

## 7. NETWORK PAYLOAD SYSTEM

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Payload Registry** | `PayloadTypeRegistry.register()` | `ServerPlayNetworking` + custom payload handlers | ✅ Both register payloads |
| **Payloads at P0** | FullSyncPayload, SlotUpdatePayload, ConfigSyncPayload, HudConfigSyncPayload, ChunkedTexturePayload, AnimSettings, SyncRequest | Similar payload set | ✅ Both have core payloads |
| **Client Sync on Join** | `FullSyncPayload` (compressed delta of all 1028 slots) | `FullSyncPayload` (full state) | ✅ Both sync on join |
| **Per-Block Update** | `SlotUpdatePayload` (single-block changes broadcast) | `SlotUpdatePayload` | ✅ Both have per-block updates |
| **HUD Sync** | `HudConfigSyncPayload` | `HudConfigSyncPayload` | ✅ Identical |

**Verdict:** **Identical payload architecture**. Both correctly implement S2C synchronization.

---

## 8. STARTUP SEQUENCE & MANAGER LOADING

| Aspect | CustomBlocks-B | Original CustomBlocks | Key Difference |
|--------|---|---|---|
| **Manager Count at P0** | ~5 managers (SlotManager, OnboardingManager, etc.) | 15+ managers (includes Undo, Favorites, Category, Lock, Notes, Template, Welcome, Trash, Arabic) | ✅ B is lighter |
| **Managers Loaded** | SlotManager ✅<br>ToolItems ✅<br>CommandRegistrar ✅<br>ResourcePackServer ✅<br>OnboardingManager ✅<br>HudSyncPayload ✅ | SlotManager ✅<br>UndoManager ✅<br>PlacementStats ✅<br>AchievementManager ✅<br>FavoritesManager ✅<br>CategoryManager ✅<br>LockManager ✅<br>BlockNotesManager ✅<br>TemplateManager ✅<br>WelcomeManager ✅<br>SnapshotManager ✅<br>TrashManager ✅<br>ArabicBlockRegistry ✅<br>+ more | 🔴 Original loads Phase 6-12 stuff at P0 |
| **OnboardingManager** | ✅ Loaded (Phase ?) | ✅ Loaded | ⚠️ Both load this at P0 |
| **Scope Creep** | Minimal (respects phases) | Massive (all phases at P0) | ✅ **B is architecturally superior** |

**Verdict:** **CustomBlocks-B is dramatically better**. Original loads 15+ managers and features from Phases 6-12 at Phase 0 (scope creep). B only loads essentials.

---

## SUMMARY TABLE: PHASE 0 FEATURE PARITY

| Feature | CustomBlocks-B | Original | Parity | Notes |
|---------|---|---|---|---|
| Slot System (800/1028 blocks) | ✅ Clean, efficient | ✅ Works, texture handling | ✅ Equivalent | B cleaner (no textures at P0) |
| Tool Items (3 tools) | ✅ Registered | ✅ Registered (12+ tools) | 🔴 Missing tools | B has only 1/4 of tools |
| HTTP Server | ✅ Fully working | ✅ Fully working | ✅ Equivalent | Minor CORS difference |
| Config System (14 fields) | ✅ Phase-aware (7 KB) | ✅ All-in-one (44 KB) | ✅ Equivalent | B cleaner, respects phases |
| Command Registration | ✅ Basic | ✅ Full-featured | ⚠️ B basic | Original has better help/error handling |
| Block Persistence | ✅ Atomic JSON | ✅ Atomic JSON | ✅ Equivalent | Identical approach |
| Network Payloads | ✅ Complete | ✅ Complete | ✅ Equivalent | Identical sync architecture |
| Manager Loading (startup) | ✅ Light (5) | ❌ Heavy (15+) | 🔴 Different by design | **B is architecturally superior** |
| Config Scope Creep | ✅ None (7 KB) | ❌ Severe (44 KB) | ✅ B wins | **B respects phase boundaries** |
| File Size Compliance | ✅ Compliant | ❌ Violates (92 KB ImageProcessor) | ✅ B wins | **B follows Royal Directive** |

---

## KEY FINDINGS

### ✅ CustomBlocks-B Strengths (vs Original)
1. **Architectural cleanliness** — respects phase boundaries
2. **Startup performance** — loads only P0-4 managers (5 vs 15+)
3. **Config size** — 7 KB vs 44 KB (respects 300-line limit)
4. **File organization** — no monolithic files (original has 92 KB ImageProcessor)
5. **Follows Royal Directive** — enforces size limits and phase separation

### 🔴 CustomBlocks-B Gaps (vs Original)
1. **Tool items incomplete** — only 3 tools vs 12+ decorative items
2. **Command help system** — no DidYouMean or detailed help registry
3. **Image processing config** — missing Phase 4 config fields (bgRemoval, etc.)
4. **Export endpoint** — uses JSON exports instead of PNG screenshots

### ⚠️ Questions Needing Clarification
1. **OnboardingManager at P0?** — Both load this; belongs to which phase?
2. **CORS headers missing?** — B might need CORS for web resource pack downloads
3. **Tool count intentional?** — Is B's 3 tools deliberate or incomplete?
4. **Phase 4 config deferral** — Should image processing config be added in Phase 4 implementation?

---

## RECOMMENDATIONS

### What to Add Back to CustomBlocks-B from Original
1. **Decorative Tools (Phase 7):**
   - Color Squares (Black, Yellow, Green, Red, Custom)
   - Color Triangles (RGB hex)
   - Rainbow Rectangle
   - Golden Hexagon
   - Diamond Triangle
   - Tab Icon

2. **Command Help System (Phase 3 enhancement):**
   - Implement `DidYouMean` for typo suggestions
   - Create `HelpRegistry` for structured help

3. **HTTP Server Enhancement (Phase 4 prep):**
   - Add CORS headers to `/pack.zip` endpoint
   - Verify `/export/` endpoint serves correct file types

4. **Phase 4 Config Fields (when Phase 4 is implemented):**
   - bgRemovalTolerance
   - bgRemovalUseYcbcr
   - bgRemovalAutoDetect
   - shadowThreshold
   - downloadTimeoutSeconds
   - maxGifSizeMb

### What CustomBlocks-B Already Does Better
1. ✅ Keep lightweight startup (don't load Phase 6+ managers)
2. ✅ Keep config minimal and phase-aware
3. ✅ Keep files compliant with size limits
4. ✅ Keep phase boundaries clear

---

## NEXT STEPS

1. Run `testing_phase0.md` test suite
2. Identify which tests pass/fail
3. Add missing tools and features back from original
4. Maintain CustomBlocks-B's architectural cleanliness
5. Proceed to Phase 1 testing
