# Handoff — apply the TG6 (Group 06) fixes (paste this to start a fresh session)

The owner tested Group 06 on 2026-07-19. Marking is already done in `Testing_Guide_06.md`
(§A/§B folded ✅; D1–D4/D6 ✅; K1–K4 ✅; D5/K5 flagged 🟡). A previous session deep-searched the
three open problems and located root causes but was told **not** to apply the fixes — that is YOUR job.

Apply the fixes below one at a time. Verify each with a build/compile. Do **not** stop to ask the
owner questions — the owner tests everything in-game at the end. The only per-item checkpoint is your
own build verify, then move to the next.

## Read first
1. `docs/testing/Testing_Guide_06.md` — §C/§D/§K rows + D5/K5 flags.
2. `docs/testing/extra/Glossary.md` — status/flag/archive rules (mandatory before editing any TG).
3. `PROGRESS_LOG.md` `## G06-C` (line ~33) and the two 2026-06-26 A/B/C-batch entries (~4415–4466) —
   the §C history and the "can't pin from source, need server logs" conclusion.
4. `docs/groups/GROUP_06_TOOLS.md` — G06-5 (customcolor snap-to-nearest), G06-14 (recycle bin).

---

## Fix ① — naming: custom-colour tool reads "#AC1155 Triangle" not "Crimson Triangle"

**Where:** `src/main/java/com/customblocks/item/CustomColorToolItem.java`, `createStack(...)` line ~71.

**Root cause:** the tool label uses `ColorLibrary.nameForHex(hex)` — an EXACT preset-hex match — and
falls back to the raw hex when the colour isn't one of the 29 presets. But the BLOCK it creates names
itself via `nearestName()` (→ "Crimson"), so tool and block disagree. `#AC1155` snaps to Crimson
everywhere except the tool label.

**Fix (1 line):** label the tool with the nearest preset name, matching the G06-5 snap-to-nearest design:
```java
// old:
String label = ColorLibrary.nameForHex(hex) != null ? ColorLibrary.nameForHex(hex) : hex;
// new:
String label = ColorLibrary.nearestName(rgb);
```
Result: `Crimson Triangle [#AC1155]` (grey `[#hex]` suffix unchanged). Apply to both Square + Triangle
(same `createStack`). Sanity-check `HexCommands.giveCustom` chat wording (line ~121) uses `nameForHex`
too — same cosmetic mismatch; change to `nearestName(...)` there for consistency if desired.

## Fix ② — D5/K5 restore: markers stuck generic "(Deleted)", never heal (issue G06-12 / §K)

**Where:** `core/MarkerTombstones.java` (add remove) + `command/handlers/TrashCommands.java` `doRestore`.

**Root cause:** `DeletedPlacementSweeper.resolveMarker` (line ~195) blanks a marker to generic
"(Deleted)" when `MarkerTombstones.contains(id)`. `MarkerTombstones` (marker_tombstones.json) is
**append-only — no remove(), never cleared, persisted forever.** So once an id is Emptied (D6), a later
re-create → delete → **restore** of that same id (e.g. reusing the name `vart`) hits the stale
tombstone and stamps the fresh markers generic instead of healing them. The "Deleted: name → (Deleted)"
degrade the owner saw is the tombstone signature.

**Fix:**
1. Add to `MarkerTombstones`:
   ```java
   /** Un-tombstone a customId — a restore brought a live block of this id back; its markers may heal again. */
   public static synchronized void remove(String customId) {
       if (customId != null && EMPTIED.remove(customId.toLowerCase())) save();
   }
   ```
2. In `TrashCommands.doRestore`, after the block is live again (right by the existing
   `MarkerResolver.forget(index)` call, line ~116), add:
   ```java
   MarkerTombstones.remove(e.customId());
   ```

**CRITICAL — do NOT also clear on `SlotManager.create`.** D6 (passing) requires a same-name *create*
to NOT revive emptied markers. Only a deliberate **restore** should un-tombstone. Clearing on create
would regress D6.

**Note for the owner retest:** if D5 fails even with NO prior Empty of that id, the code path is correct
(`resolveMarker` heals by customId) — that would be a separate runtime issue; capture it then.

## Fix ③ — §C repaint: NOT a source fix yet — needs server logs

The owner's "the blocks got fucked a lot" for §C is the C-repaint / C-icon path. PROGRESS_LOG is
explicit (lines ~4415, ~4429, ~4456): the C-name hex-sync was already fixed (`ColorHexSyncPayload`),
but **`recolorvariants` (the confirm's "Yes") and the icon re-tint (`ServerPackGenerator.addTintedShapeItems`)
are RUNTIME bugs that cannot be pinned from source.** They run server-side; their `G06-C` diagnostic
lines live on the MCServerHost server (yoyoo.mcsh.io), NOT the client `latest.log`.

**Do not guess a code change here.** The jar already carries temporary `G06-C`-tagged logging in
`HexCommands.setHex`, `ColorVariantService.recolorVariants`, and `ServerPackGenerator.addTintedShapeItems`.

**Collect the logs on SP first (safe, local).** On singleplayer the integrated server runs the same
server-side code in-process, so the `G06-C` lines land in the owner's own `.minecraft/logs/latest.log`
— no MCServerHost pull needed, and a throwaway test world risks no real blocks. Repro: new SP world →
create a block, retexture it, make a `_red` variant (red Triangle) → `/cb config hex red #FF8800` →
press Yes → read `G06-C` lines from `latest.log`. Only if the bug proves dedicated-only does the owner
need the remote server log. Once the lines are in hand, the repaint fix is targetable. Leave the
temporary `G06-C` logs in place until §C is pinned.

---

## After applying ① and ②
- Build green (JDK 21: `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`, `--no-daemon`;
  jar → `build/libs/customblocks-1.0.0.jar`). Build-green = compiles only, NOT Done ✅.
- Update `Testing_Guide_06.md` per glossary: keep D5/K5 flagged until the owner re-confirms
  in-game; record the fix state in the Cleanup/notes, not as ✅.
- Log the work in `PROGRESS_LOG.md` (root cause + files + build result) — do not mark anything Done.
- For §C: add/keep a Cleanup item saying it's blocked on server-side `G06-C` logs.
