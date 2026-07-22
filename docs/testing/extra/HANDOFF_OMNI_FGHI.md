# Handoff — build the Omni-Tool §F–§I in ONE pass (paste this to start a fresh session)

Build all four Omni-Tool sections of Group 06 TG6 — §F (mode cycle), §G (Face rotate), §H (Delete),
§I (Copy) — as ONE merged tool, one jar. Owner approved **all four in one pass, no questions**. Do NOT
stop to ask; the owner tests in-game at the end. Your only per-step checkpoint is your own build verify.
Everything below is pinned from source (files/lines/signatures verified 2026-07-19). Be precise.

## Read first
1. `docs/testing/Testing_Guide_06.md` — §F rows F1–F5, §G G1–G9, §H H1–H3, §I I1–I6 (the acceptance spec).
2. `docs/testing/extra/Glossary.md` — status/flag rules (mandatory before editing any TG).
3. This file. Then `git grep OMNI` before deleting anything.

## Current state (what exists, what's wrong)
The Omni-Tool is **scaffolded on the SCRAPPED design** (mode-selection Screen, only Glow+Hardness):
- `item/OmniToolItem.java` (151 lines) — sneak+right-click **opens a GUI** (`Nav.Dest.OMNI`); the mode
  `switch` only wires `GLOW` and `HARDNESS`. `GLOW_STEPS={0,4,8,12,15}`, `HARD_STEPS={0,.5,1.5,5,20,-1}`,
  `HARD_LABELS`. Has `cycleGlow`, `cycleHardness`, `applyName(stack,mode)`, `appendTooltip`.
- `core/OmniToolState.java` (100) — `enum Mode{GLOW,HARDNESS}` (label+color, `next()`, `fromName()`);
  persists mode per-player to `config/customblocks/data/omni_tool.json`. `getMode`/`setMode`.
- `gui/chest/OmniMenu.java` — the **scrapped** Screen. `GuiRouter.java:184` `case OMNI -> OmniMenu.build(player)`.
  `Nav.java:25` enum has `OMNI`.
- `command/handlers/ToolCommands.java:33,56-59` — `/cb omni [mode]` already gives the tool and calls
  `OmniToolItem.applyName(stack, active)`. **Keep this.**

The NEW design (spec, locked): **no Screen.** Sneak+right-click **cycles** the 5 modes in fixed order
`Glow → Hardness → Face → Copy → Delete → Glow`, each switch shows a hotbar line + a click sound; plain
right-click runs the current mode. (The Screen was scrapped 2026-07-19 — TG6 Archive 👎.)

---

## Step 1 — OmniToolState: 5 modes + Copy buffer
Add `FACE`, `COPY`, `DELETE` to `enum Mode`, **in cycle order** (`next()` already wraps over `values()`):
```java
GLOW("Glow", CbFmt.VALUE),
HARDNESS("Hardness", CbFmt.DIM),
FACE("Face", CbFmt.VALUE),
COPY("Copy", CbFmt.VALUE),
DELETE("Delete", CbFmt.BAD);   // Delete last (§F fixed order)
```
Add a **per-player, in-memory** copy buffer (Copy is a session loop; it need not persist):
```java
public record Feel(int glow, float hardness, String soundType, boolean noCollision) {}
private static final Map<UUID, Feel> COPY_BUF = new ConcurrentHashMap<>();
public static void setCopy(UUID p, Feel f) { COPY_BUF.put(p, f); }
public static Feel getCopy(UUID p)         { return COPY_BUF.get(p); }
public static void clearCopy(UUID p)       { COPY_BUF.remove(p); }
```
In `setMode(...)`: when the new mode != COPY, `clearCopy(player)` (I3 — the paste prompt/buffer lives
only while in Copy mode). Also `clearCopy` in a disconnect hook if one exists.

## Step 2 — OmniToolItem: in-hand cycle + all 5 mode actions
Rewrite the two sneak branches to **cycle instead of opening the GUI**, and wire all modes.

**Sneak+right-click (both `useOnBlock` sneak branch AND `use()` sneak branch) → cycle:**
```java
Mode next = OmniToolState.getMode(player.getUuid()).next();
OmniToolState.setMode(player.getUuid(), next);
applyName(player.getStackInHand(hand /* or ctx.getHand() */), next); // live item-name update
Chat.toolSuccess(player, "Omni-Tool", next.label);   // hotbar line, routed through Chat (gate)
// click sound — see BuzzerBlock.java for the exact pattern + the .value() rule:
world.playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
        SoundCategory.PLAYERS, 0.6f, 1.4f); // UI_/BLOCK_NOTE_* = RegistryEntry → .value(); bare SoundEvents don't
```
Remove all `GuiRouter.openFresh(... Nav.Dest.OMNI ...)` calls.

**Plain right-click `useOnBlock` — restructure the lock gate, then dispatch 5 modes.** CAUTION: the current
blanket `LockManager.isLocked → refuse` (lines ~75-78) must NOT block Copy's READ (I4). Gate per-mode:
```java
Mode mode = OmniToolState.getMode(player.getUuid());
boolean locked = LockManager.isLocked(d.customId());
if (locked && mode != Mode.COPY) { Chat.lockedTool(player, d.customId()); return ActionResult.SUCCESS; }
switch (mode) {
    case GLOW     -> cycleGlow(player, d);                 // existing
    case HARDNESS -> cycleHardness(player, d);             // existing
    case FACE     -> rotateFace(player, d, ctx.getSide()); // §G — Step 3
    case DELETE   -> ((CbBlock) block).cbDelete(player, ctx.getWorld(), ctx.getBlockPos()); // §H, reuse DeleterItem
    case COPY     -> doCopy(player, d, locked);            // §I — Step 4
}
```
`ctx.getSide()` is the clicked `Direction`; `Direction.getName()` returns exactly `down/up/north/south/
west/east` = `TextureStore.FACES`. `block` is already resolved as a `SlotBlock`; for DELETE cast to `CbBlock`
(SlotBlock implements it — see DeleterItem.java:37,44).

Update `appendTooltip` to list all 5 modes. Keep `applyName` as-is (`"Omni-Tool [X Mode]"`, `mode.color`).

## Step 3 — §G Face rotate (the one new subsystem: native model rotation, non-destructive)
Rotation is a per-face quarter-turn (0..3) rendered by the **vanilla model face `"rotation"`** property —
NO pixel mangling. Painted/shaped blocks already emit standard vanilla model JSON through the pack, so this
just adds a property; delivery to clients rides the existing `updatePack()` → PackSyncService (dedicated) /
local regen (SP), same rail as `/cb paintface`. No custom payload.

**3a. New store `core/FaceRotations.java`** (mirror `MarkerTombstones`/`DeletedSlots` json style). Key by
slot **index** (like `TextureStore` face files). Persist to `config/customblocks/data/face_rotations.json`:
```java
get(int index, String face) -> int 0..3           // 0 when absent
rotateCw(int index, String face) -> int newQ       // set (get+1)%4, save, return newQ
set(int index, String face, int q)                 // for undo/redo restore
boolean hasAny(int index)                          // any face q>0
void clear(int index)                              // all faces — call on delete
```

**3b. Clear on delete.** In `TextureStore.delete(int index)` (line 236, the loop at 252 that deletes all
face files), also `FaceRotations.clear(index)` so a retired/reused slot never inherits stale rotations.

**3c. Pack-gen** (`network/ServerPackGenerator.java`, **currently 472/500 lines — see monolith warning**):
- **`element(index, box)`** (line 399): for each face add `"uv":[0,0,16,16]` and, when
  `FaceRotations.get(index,face) > 0`, `f.addProperty("rotation", q*90)`. This gives **shaped blocks**
  rotation for free (G9). MC requires `rotation ∈ {0,90,180,270}` with a uv present.
- **Full cube** cannot rotate via `cubeFacesJson` (parent `minecraft:block/cube` has fixed elements, no
  per-face element to hold rotation). So add a branch: when `TextureStore.hasAnyFace(i) || FaceRotations.hasAny(i)`,
  and it's a full cube, emit an **explicit-element cube** built from `element(i, {0,0,0,16,16,16})` with the
  same textures map as `cubeFacesJson` (`particle`+`all`→base, painted faces→`base_"_"face`). Keep the plain
  `cubeFacesJson` only for paint-only-no-rotation (unchanged). The block-model dispatch is around lines
  149/159 — extend the trigger to include `FaceRotations.hasAny(i)`.
- Verify `writeFacePngs` still emits every painted face PNG for the new path (a rotated-but-unpainted face
  shows the base texture — no PNG needed; a painted+rotated face uses its override PNG).

**3d. Rotate action** in OmniToolItem:
```java
private static void rotateFace(ServerPlayerEntity p, SlotData d, Direction side) {
    String face = side.getName();
    int oldQ = FaceRotations.get(d.index(), face);
    int newQ = FaceRotations.rotateCw(d.index(), face);
    UndoManager.recordFaceRotate(p.getUuid(), d.index(), face, oldQ, newQ); // Step 3e
    ResourcePackServer.updatePack();  // model changes → clients re-sync (same as paintface)
    Chat.toolSuccess(p, "Face " + face, (newQ * 90) + "°");
}
```
Rotation applies to whatever the face shows (base or painted) — G8. Geometry does NOT rotate — only the
face texture — G1. 4 clicks = 0/90/180/270/0 — G2. Per-face independent (keyed by face) — G3. All placed
copies share the model → all show it — G4. `updatePack` pushes live (G5), json persists (G6).

**3e. Undo/redo (G7).** Rotation lives OUTSIDE `SlotData`, like `Kind.FLAG`. In `UndoManager.java`:
- add `FACE_ROTATE` to `enum Kind`;
- `public record FaceRot(int index, String face, int oldQ, int newQ) {}`;
- carry it on the `Op` (add a nullable `FaceRot` field the same way `Flag flag` was added — keep the old
  constructors delegating so existing callers still compile);
- `recordFaceRotate(UUID p, int index, String face, int oldQ, int newQ)` → push.
In `command/handlers/HistoryCommands.java`: add a `case FACE_ROTATE` to BOTH switches — undo at line 219
(`FaceRotations.set(index,face,oldQ)`), redo at line 260 (`set(...,newQ)`) — and add `FACE_ROTATE` to the
**pack-rebuild condition at line 344** (currently `MODIFY || SHAPE`), so undo/redo rebuilds the model like SHAPE.

## Step 4 — §I Copy (`doCopy`)
```java
private static void doCopy(ServerPlayerEntity p, SlotData d, boolean targetLocked) {
    Feel held = OmniToolState.getCopy(p.getUuid());
    if (held == null) {                       // GRAB (I1) — reading a locked source is allowed (I4)
        OmniToolState.setCopy(p.getUuid(), new Feel(d.glow(), d.hardness(), d.soundType(), d.noCollision()));
        Chat.toolSuccess(p, "Copied feel — click a block to paste"); // persistent-style prompt (I1/I3)
        return;
    }
    if (targetLocked) { Chat.lockedTool(p, d.customId()); return; } // PASTE refused on locked target (I4)
    SlotData before = d;
    SlotManager.setGlow(d.customId(), held.glow());
    SlotManager.setHardness(d.customId(), held.hardness());
    SlotManager.setSoundType(d.customId(), held.soundType());
    SlotData after = SlotManager.setNoCollision(d.customId(), held.noCollision()); // last setter returns fresh snapshot
    UndoManager.recordModify(p.getUuid(), before, after, "copy"); // ONE undo step (I2)
    SlotLighting.applyToPlaced(p.getServer(), d.index(), held.glow()); // glow shows live on placed copies
    HudSync.broadcast(p.getServer());        // feel-stats HUD updates live, no rejoin
    Chat.toolSuccess(p, "Pasted feel onto", d.customId()); // buffer STAYS (I3 — repeatable until mode change)
}
```
Only the 4 feel fields change; look/shape/name/category untouched (I5). Sound is a plain reference, no
support check (I6). Setters: `SlotManager.setGlow/​setHardness/​setSoundType(String)/​setNoCollision(boolean)`
all return the updated `SlotData` (verified 216-234).

## Step 5 — remove the scrapped Screen
`git grep OMNI` first. Delete `gui/chest/OmniMenu.java`; remove `case OMNI ->` at `GuiRouter.java:184`;
remove `OMNI` from the `Nav.Dest` enum (`Nav.java:25`). Confirm no other refs remain.

## Monolith / gate warnings (§9.3: 500 general, 400 command/handlers, 300 *Config)
- **`ServerPackGenerator.java` is 472/500.** The rotated-cube additions WILL likely breach 500. If they do,
  extract the per-face/rotated model builders (`cubeFacesJson`, `shapeModelJson`, `element`, `writeFacePngs`,
  new rotated-cube) into a new `network/FaceModelBuilder.java`. Check `wc -l` before you build.
- **`OmniToolItem.java` is 151.** Five modes + copy + sound may approach 500; if over, move the mode actions
  to a `core/OmniModes.java` helper.
- **hotbarRouteGate:** no `§`/`CbFmt` colour inside a hotbar body — route every hotbar line through a `Chat`
  method (the code above does). **chatRouteGate/chatColourGate:** no raw `sendMessage`/hardcoded `§` — use `Chat`.
- Sound `.value()` rule (memory): `BLOCK_NOTE_BLOCK_*` and the `UI_*` family are RegistryEntry → `.value()`;
  bare `SoundEvent` constants (e.g. `SoundEvents.ITEM_TRIDENT_RETURN`) take no `.value()`. If it won't compile,
  flip `.value()`. Reference: `buzzergame/BuzzerBlock.java:171-194`.

## Build (JDK 21, PowerShell)
```
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat build --no-daemon -q "-Dorg.gradle.java.home=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
```
Green = compiles + all gates (monolithGate/tgGate/chat*/hotbar*/mojibake/sound/fileSize) pass, EXIT 0,
jar → `build/libs/customblocks-1.0.0.jar`. Build-green = compiles only, NOT Done ✅.

## After building
- `Testing_Guide_06.md`: flip §F/§G/§H/§I in the Sections table from `Designed ⏳` to `Built 🎯`
  (per glossary — do NOT mark ✅; the owner confirms in-game). Update the Verdict line. Leave F1–I6 rows unmarked.
- `PROGRESS_LOG.md`: one newest-first entry — files touched, the FaceRotations subsystem, mode cycle,
  Copy buffer, Delete reuse, scrapped-Screen removal, build result. Do NOT mark anything Done.
- Do not write cross-chat memory unprompted.

## Acceptance (what the owner will test)
F1 cycle order Glow→Hardness→Face→Copy→Delete→Glow · F2 hotbar+sound each switch · F3 plain-click cycles
glow/hardness value · F4 no Screen ever · F5 mode survives relog+restart, per-player · G1 face texture turns
90°CW, geometry still · G2 0/90/180/270/0 · G3 per-face independent · G4 all copies match · G5 MP live ·
G6 survives restart · G7 undo/redo · G8 rotates painted face too · G9 works on non-cube shapes · H1 delete
like Deleter · H2 undo · H3 locked refused · I1 grab+prompt · I2 paste all 4 stats, one undo · I3 prompt
persists until mode change · I4 read locked ok / paste locked refused · I5 look/shape/name/category untouched ·
I6 custom sound copies as-is.
