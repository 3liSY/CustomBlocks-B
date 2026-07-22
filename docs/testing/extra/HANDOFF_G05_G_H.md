# Handoff — build TG5 §G then §H (paste this to start a fresh session)

Design is fully locked (2026-07-16). No more discussion needed — build it.

## Read first, in this order
1. `docs/groups/GROUP_05_RESOURCE_PACK.md` §G05-6 (all of it, including "Locked decisions" at the top) —
   this is the live in-place texture swap spec.
2. Same file §G05-7 (live in-place... no — skip-redundant-first-join-reload spec, including its "Locked
   decisions").
3. `docs/testing/Testing_Guide_05.md` — current TG5 state, verify §G/§H rows still say "LOCKED, not
   yet green-lit" (if it now says built/passed, STOP and re-read what changed before continuing).

## Order: build G completely first, then H. Do not interleave.

## §G — Live in-place texture swap
Before writing any code, deep-search and verify against the CURRENT source (the design doc cites specific
line numbers from 2026-07-15/16 — the file may have moved since):
- `client/ResourcePackGenerator.java` — the `regenerate → writeLoosePack → applyReload → reloadResources()`
  chokepoint. Confirm line numbers still match the doc's citations before trusting them.
- `client/render/StaticFrameCache.java` — confirm it still discards its `NativeImageBackedTexture` ref
  (doc cites `:102-105`); this needs to change to KEEP the ref.
- `client/render/AnimFrameCache.java` — confirm `Slot` still holds `frameTex` + `grid`, and the
  `blitCell`+`upload()` pattern the doc wants you to reuse for Phase 1.
- `network/ServerPackGenerator.java:100-165` — re-verify the model-selection table in the doc (which slot
  kinds are atlas vs off-atlas) against current code; this table was already corrected once (2026-07-15)
  after being wrong in an earlier draft — don't trust it blindly, re-derive it from source.
- For Phase 2: confirm the `SpriteContents.upload` signature and package-private-ness against the actual
  yarn 1.21.1 mappings in this project (the doc says an accessor/invoker Mixin is needed) before writing
  the Mixin.

Build order within G: Phase 1 (off-atlas: animated + Arabic world blocks + all item icons) fully working
and verified with no regression to §A/§B/§C/§E/§F, THEN Phase 2 (atlas sprite upload for common
static/per-face/shape blocks).

Locked behavior to implement (do not re-litigate):
- B3 fix scope: only retexture-only bursts on EXISTING blocks are fixed. `/cb create` bursts still go
  through the old full-reload path — document that as a known remaining edge, don't claim it's fixed.
- Live-upload failure → silently fall back to the existing full `reloadResources()` path. No player-facing
  message. One `LOGGER.warn` line server-side with the slot id. Must never crash.
- Multiplayer: the live pixel update broadcasts to EVERY connected client, not just the editor.
- `/cb undo` on a live-swapped retexture live-reverts instantly via the same path in reverse, not a full
  reload.

## §H — Skip redundant first-join reload
Build only after §G is stable and verified. Deep-search against current source before coding:
- `client/ResourcePackGenerator.java:46` (`lastAppliedHash` static volatile) and `:56` (the guard) — confirm
  line numbers.
- `client/packsync/ClientPackReceiver.java:238-243` (`finalizeDone`, the dedicated-server path this mirrors).
- Confirm where per-world save-folder data can be written client-side in this MC/Fabric version (need a
  per-world path, not the global `resourcepacks/CustomBlocks/` folder the original draft sketched).

Locked behavior to implement (do not re-litigate):
- Hash sidecar file is PER-WORLD (inside that world's save folder), not global.
- Never trust the hash blindly: missing, corrupt/unreadable, mismatched pack folder (empty/no
  `pack.mcmeta`), or a mismatched mod version → treat as "no hash," force a real reload.
- Sidecar write must be atomic (temp file + rename) — reuse the G05-3 atomic loose-writer pattern. A
  mid-write crash must never leave a corrupt file that gets misread as valid, and the write path itself
  must never be able to crash the game.
- Bundle the mod's version string into what's checked — a mod jar update forces one reload even if the
  pack-content hash still matches.
- Never delete the sidecar file during `deleteStale` / pack-wipe passes.

## Testing
Owner tests §G and §H together, at the end, once BOTH are fully built and you consider them correct — not
incrementally in-game. Use the Verify/Expected-Result rows already written in TG5 §G/§H and
GROUP_05_RESOURCE_PACK.md as your own pre-handoff checklist (SP + MP, both phases, undo, failure fallback,
mod-version bump, corrupt hash file) before declaring it ready for the owner's pass.

## When done
Update `docs/testing/Testing_Guide_05.md` §G/§H rows from "LOCKED — not yet green-lit" to
"🟢 built, awaiting owner confirm" (mirroring how §E/§F looked before their pass) and update
`GROUP_05_RESOURCE_PACK.md`'s "Reload state" fact rows accordingly. Do NOT mark passed yourself — that's the
owner's call after they test.
