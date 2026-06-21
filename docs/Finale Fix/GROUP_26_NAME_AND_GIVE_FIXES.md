# Group 26 — Name & Give Fixes + Named-Texture Mirror

> **Prerequisite:** Group 13 (Arabic) Pass 1 + Pass 2 verified in-game.
>
> **Objective:** Two surgical regression fixes pulled out of Group 13 (so it stays Arabic-only),
> plus one self-contained new feature that builds on them:
> (A) display names show clean — spaces + Title Case, no underscores;
> (B) `/cb give <id>` resolves the id case-insensitively;
> (C) an optional, config-toggled `textures_names/` mirror — a human-readable copy of the texture
> folder named by block, **written** by the mod but **never read** by it.
>
> **Source:** Group 13 "Post-Pass-2 in-game feedback" (developer, 2026-06-15) for A + B; developer
> request (2026-06-15) for C.
>
> **Rules:** A and B are surgical — touch only the files named below, do not refactor. C is a new
> feature in new files (write-only, off the critical path). Build order: **A → B → C** (C reuses
> A's clean names). Nothing is DONE until the developer confirms in-game (Golden Rule, CLAUDE.md §2).

---

## Status

| ID | Item | State |
|:--:|---|:--:|
| FIX A | Clean display names (underscores → spaces + Title Case) | ✅ Confirmed in-game 2026-06-15 |
| FIX B | `/cb give <id>` case-insensitive | ✅ Confirmed in-game 2026-06-15 |
| Part C | Named-texture mirror (`textures_names/`) | ✅ Confirmed in-game 2026-06-15 |
| FIX D | Multiplayer display name (block + item read the synced cache, not server-only data) | ⏳ NOT built — root cause confirmed 2026-06-20 |

---

## What this group fixes

| ID | Problem before | Wanted |
|:--:|---|---|
| FIX A | Names render with underscores, e.g. `Test_Black` | `Test Black` — underscores → spaces, each word Title-Cased |
| FIX B | `/cb give Te` and `/cb give te` don't both resolve | Both resolve to the same block (id lookup case-insensitive) |
| Part C | Textures only browsable as opaque `slot_N.png` | Optional `textures_names/<block name>.png` mirror, toggled in config |
| FIX D | On a dedicated server every custom block/item shows the literal **"Custom Block"** (no real name), even after rejoin | The block + item show their real display name on the server, same as singleplayer |

## What this group covers

| Feature | Command / surface |
|---|---|
| Clean display names | Automatic — every create / upload / rename, plus a one-time boot migration |
| Case-insensitive give | `/cb give <id>` (and every other id command) |
| Named-texture mirror | `/cb config mirrornames [on \| off \| rebuild]` |

---

## Implementation Requirements

### FIX A — clean display names ✅ *(regression introduced in Group 13)*

**Problem:** names render with underscores, e.g. `Test_Black`. Cause: `NameCase.titleCase` capitalized
each word but **kept** the `_` — it treated `_` as a word boundary yet appended the char unchanged.

**Want:** a block whose file/id is `Test_black` (any caps) shows **`Test Black`** — underscores become
spaces, each word Title-Cased. Covers the **224 bundled Arabic art blocks** and **all future creation**.

**The fix — two changes:**

1. **One code edit** in `core/NameCase.titleCase`: when the char is `_`, append a `' '` space instead
   of the `_`. Detect the word boundary on the original char *before* the remap.
   - **Blast radius verified:** `titleCase` has exactly **3 callers, all display-name paths** —
     `ArabicArt.displayName`, new-block naming (`SlotManager.create`), and the `withDisplayName` rename
     (`SlotManager.rename`). **None is an id or filename**, so ids/files are untouched.
   - **Do NOT** also edit `arabic/ArabicArt.displayName` — it *calls* `titleCase`, so it inherits the
     fix automatically. A second edit there would be redundant.
2. **One-time boot migration** — the 224 bundled blocks are already persisted with underscore names, so
   the code fix alone only affects *new* derivations. New `SlotManager.migrateDisplayNames()` re-runs
   every loaded name through `titleCase` and persists once if anything changed (idempotent — a no-op on
   every later boot). Routed through `SlotManager` / `SlotDataStore` (design rules #3, #4). Wired in
   `CustomBlocksMod.onInitialize` right after `loadAll()`, before `importArt(false)`; logs the count.

### FIX B — `/cb give <id>` case-insensitive ✅

**Problem:** `UtilityCommands.give` calls `SlotManager.getById(id)`, a plain case-sensitive map get
(`return BY_ID.get(customId);`), so `/cb give te` misses a block whose id is `Te`.

**The fix — one place, zero regression:** add a **case-insensitive fallback** inside
`SlotManager.getById` (and mirror it in `hasId`): try the exact `BY_ID.get(id)` first (fast path,
byte-identical for every existing caller), and only when that returns null, do a case-insensitive scan
of `BY_ID` (new private `findByIdIgnoreCase`). This fixes `/cb give` **and** every other id command.

- **Do NOT** lowercase the `BY_ID` keys or touch the ~20 `BY_ID.put` sites. `getById`/`hasId` have
  **40+ callers**; exact-first-then-fallback keeps all exact matches unchanged (no regression).
- **Tie-break:** if more than one id matches case-insensitively, return the **lowest slot index**
  (deterministic). Ids are unique in practice; this just guarantees stable behavior.
- **Guards:** tab-completion / suggestions stay exact-cased — the fallback only affects resolution.

### FIX D — multiplayer display name ⏳ *(NOT built — root cause confirmed 2026-06-20)*

**Problem (owner, on a dedicated server):** a freshly created block — and its item in hand/inventory — shows
the literal text **"Custom Block"** instead of its real name. It does **not** fix itself after a rejoin.
Singleplayer is always correct.

**Root cause (confirmed from code):** the name is read from the **server-only** `SlotManager`, on the
**client**:

- `SlotBlock.getName()` → `SlotManager.getBySlot(slotKey)` → `d.displayName()`, else `"Custom Block " + index`.
  ([`block/SlotBlock.java`](../../src/main/java/com/customblocks/block/SlotBlock.java) ~line 165)
- `SlotBlock.SlotItem.getName(stack)` → same `SlotManager` read, else `"Custom Block"`. (~line 181)

`getName` runs **client-side** for HUD / hand / inventory display. On a **dedicated** server the client's own
`SlotManager` does **not** contain the server's blocks (its local `slots.json` is stale/empty for them — the
exact split ADR-010 describes for textures), so `getBySlot` returns `null` → the `"Custom Block"` fallback.
Singleplayer/integrated host shares one JVM, so `SlotManager` has the data and the name is right — which is
why this is **multiplayer-only**.

The real name **is** already on the client, in the synced **`ClientSlotCache`**
([`client/ClientSlotCache.java`](../../src/main/java/com/customblocks/client/ClientSlotCache.java)), filled by
`HudSync` → `HudSyncPayload` on join and after every create/rename/delete. `getName` simply never reads it —
so a rejoin (which refreshes `ClientSlotCache`) doesn't help.

**The fix — read the synced cache on the client, keep the server read on the server:**

- On the **client** physical side, resolve the name from `ClientSlotCache.getEntry(slotIndex).name()`
  (fall back to the existing literal only when the cache has no entry yet). On the **server**, keep the
  current `SlotManager` read unchanged.
- **Do NOT** make the common `SlotBlock` / `SlotItem` import a client-only class directly — `ClientSlotCache`
  is `@Environment(CLIENT)`. Use the same seam ADR-009 used for the swap predictor: an env-guarded lookup
  (e.g. `FabricLoader.getEnvironmentType()` / a small client-only name resolver), so the common class never
  hard-references client code. (Mirror of ADR-009's "common item must not reference `ClientSlotCache`" rule.)
- **Blast radius:** only the two `getName` methods in `SlotBlock.java`. No change to ids, the pack, placed
  blocks, or the server's authoritative naming. Arabic letters use their own `ArabicLetterItem.getName`
  (ADR-006) and are not affected.

**Why it stays fixed:** `ClientSlotCache` is *the* channel the server already uses to tell the client about
every block; it is refreshed on join and on every mutation. Reading the name from it means the client always
shows whatever the server last sent — no second source to drift, and it self-heals on the next sync.

> **Relationship to the other multiplayer bugs:** FIX D is one instance of a shared root family — *the
> multiplayer client reading server-only state, or waiting on a round-trip, where singleplayer hid it by
> sharing one JVM.* The Arabic place-flash + slow recolour (`GROUP_13_ARABIC.md` O10/O11) are the same family,
> fixed with the sibling technique (client-side prediction, ADR-009). Documented together so the pattern is
> visible, but each is fixed in its own group.

---

### Part C — Named-Texture Mirror ⏳ *(new feature — depends on FIX A)*

An optional, config-toggled, human-readable copy of the texture folder where every file is named by the
block's in-game name instead of `slot_N`:

```
textures/slot_0.png        ->   textures_names/Neptune Red.png
textures/slot_197_up.png   ->   textures_names/Panda Yellow (up).png       (face override)
textures/slot_1.png        ->   textures_names/Subscribe Red (slot 1).png   (duplicate name)
```

**Iron rule — write-only.** The mod **writes** this folder; it **never reads** it. `slot_N.png` stays
the one true texture, keyed by slot index — block identity (`customblocks:slot_N`), models, the resource
pack, and every already-placed block are untouched. If the mirror breaks or is deleted, blocks are
completely unaffected and no pack rebuild happens.

**Naming:** the clean `Title Case` display name (after FIX A). Sanitised for Windows
(`< > : " / \ | ? *` → `_`). Duplicate names get a `(slot N)` suffix so they never overwrite each other;
face overrides get a `(face)` suffix. **This is why Part C depends on FIX A** — names must be clean
first, or the mirror inherits `Test_Black`.

#### 1. Config flag

`CustomBlocksConfig.mirrorNamedTextures` (boolean, default `false`). One field + one `load` line + one
`save` line, identical to the `hudEnabled` / `silentPack` pattern. (Config.java is 261/300 — fits.)

#### 2. In-game command

A new small handler `command/handlers/MirrorCommands.java`, registered by `CommandRegistrar`.
Deliberately **not** added to `ConfigCommands.java` (374/400 — one more block busts `verifyFileSize`; a
domain-split handler is the §5 rule anyway):

| Command | Does |
|---|---|
| `/cb config mirrornames` | Status — on/off, file count, folder path |
| `/cb config mirrornames on` | Enable **and** immediately backfill every existing block |
| `/cb config mirrornames off` | Stop mirroring (existing files left in place) |
| `/cb config mirrornames rebuild` | Wipe + regenerate the whole folder from truth (fixes drift) |

#### 3. The writer

A new `core/TextureNameMirror.java`, the **sole** owner of `config/customblocks/textures_names/`:

- `syncSlot(index)` — (re)write that block's named PNG(s) from its current name + bytes.
- `removeSlot(index)` — delete that block's named PNG(s).
- `rebuildAll()` — wipe the folder and regenerate from every slot (used by `on` and `rebuild`).
- Atomic writes (temp + move, NFR-13). **Best-effort:** it logs and swallows its own errors so a mirror
  failure can **never** break a block create, retexture, rename, or delete.

#### 4. A tiny manifest

`config/customblocks/data/mirror_index.json`, a `{ slot -> filename }` map. This is what makes it robust:
on rename it knows the *old* filename to delete, on delete it knows exactly what to remove, and collisions
resolve deterministically → **no orphans, no stale files, ever.** Stored under `data/` so the browse
folder stays pure PNGs.

#### 5. Hooks *(only fire when the flag is on)*

- block created / retextured / face painted → `syncSlot(index)` **after** the name is committed
- block renamed (display-name change) → `syncSlot(index)` (rewrites under the new name; the manifest
  deletes the stale file)
- block deleted → `removeSlot(index)`

> **As built (see ADR-004):** rather than editing each command, the hooks live at the two single-writer
> choke points — `TextureStore` (save / saveFace / deleteFace → `syncSlot`; delete → `removeSlot`) and
> `SlotManager` (rename / restoreSnapshot → `syncSlot`). Because `slot_N.png` is the canonical byte store
> for *every* block, this catches every edit path (create, retexture, colour tools, video, gradient, undo,
> Arabic, dupe, trash) with 6 lines and no chance of missing one. In every create flow the name is
> committed before the texture is written, so `syncSlot` never lands nameless.

#### Why it's robust *(not just functional)*

- **Self-healing** — `rebuild` regenerates the whole folder from truth; one command fixes any drift.
- **Zero server risk** — write-only, index-keyed canon untouched, no pack rebuild, no effect on placed blocks.
- **Zero orphans** — the manifest tracks every file, so renames and deletes clean up after themselves.
- **Never breaks a block** — off the critical path and best-effort; a failure logs and is ignored.
- **Clean architecture** — one writer class + one command handler; respects the file-size gates and the
  domain-split rule (§5).

---

## Out of scope *(do NOT build here)*

- The Arabic GUI hub (Pass 5) and `/cb search` edits — stay in / after Group 13.

---

## Testing & sign-off

- **FIX A + FIX B:** ✅ confirmed in-game 2026-06-15. Step-by-step re-test guide:
  `docs/Finale Fix/Reports/GROUP_26_TESTING_GUIDE.md`. Logged in `PROGRESS_LOG.md` + `CHANGELOG.md`.
- **Part C:** 🟢 built + gates pass, jar deployed 2026-06-15. Its test steps are now the 🎯 TEST NOW
  section of `GROUP_26_TESTING_GUIDE.md`. Implementation choice recorded in `docs/adr/ADR-004`. Awaiting
  the developer's in-game run. Golden Rule: nothing is DONE until confirmed in-game.
