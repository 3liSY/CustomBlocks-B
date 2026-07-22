# Handoff — fix TG4 §E and TG5 §D, one by one, no questions (paste this to start a fresh session)

Both are confirmed regressions with a deep-search head start already logged in their Testing Guides.
Fix them one at a time. Do not stop to ask the owner anything — the owner tests everything at the end.
The only checkpoint per item is your own build/compile verify, then move straight to the next.

## Order: §E (TG4) first, then §D (TG5). Do not interleave.

## Read first, in this order
1. `docs/testing/TESTING_GUIDE_04.md` §E row + its Cleanup entry (2026-07-18 deep-search note) —
   current state and what's already ruled out.
2. `docs/groups/GROUP_04_Communication.md` §G04-5 (kick screen spec).
3. `docs/testing/Testing_Guide_05.md` §D row + its Cleanup entry (2026-07-18 deep-search note).
4. `docs/groups/GROUP_05_RESOURCE_PACK.md` §B (Stable Generation, Update, and Resync) — the section
   owning §D's behavior.

## §E — Human CustomBlocks kick screen (real registry-mismatch kick shows the old vanilla screen)

Already ruled out: the `/cb debug kick` preview (`CustomBlocksClient.java`, `KickPreviewPayload`
receiver) calls `mc.setScreen(new CbKickScreen(...))` directly and never touches
`DisconnectedScreenMixin` — so the preview passing proves nothing about the real path.

Real path: `RegistrySyncHealMixin` injects `HEAD` of Fabric API's `RegistrySyncManager.checkRemoteRemap`
(CONFIGURATION-phase, likely off the client render thread), arms `CbKick`, throws `RemapException`.
`DisconnectedScreenMixin` injects `TAIL` of `DisconnectedScreen.init`, `require = 0` (silently no-ops if
it never matches).

Steps:
1. Add temp diagnostic logging: thread name in `CbKick.arm()`/`consume()`, and an unconditional log line
   (before the null check) at the top of `DisconnectedScreenMixin.customblocks$maybeKickScreen`. Build,
   trigger a real registry-mismatch kick (a friend/alt client below the server's `max_blocks`, or
   temporarily lower `localRegisteredSlots()`'s effective count to force it), read the log.
2. From the log, determine: does the mixin injection fire at all? If yes, is `CbKick.consume()` null
   there (timing/thread race) or non-null but the screen swap still doesn't stick (wrong screen class,
   re-entrant `setScreen`, or something re-opening `DisconnectedScreen` after the swap)? If the injection
   never fires, `DisconnectedScreen` is not the class Minecraft opens for a config-phase disconnect —
   find the actual class (check `ClientConfigurationNetworkHandler`/`ClientCommonNetworkHandler`
   disconnect handling in the mapped Fabric/MC sources for this project's version) and retarget the
   mixin at that class instead.
3. Fix whichever cause the log points to. Remove the temp diagnostic logging once fixed (or keep it
   behind a level that doesn't spam normal disconnects — your call, but don't ship raw INFO spam on
   every vanilla disconnect).
4. Verify: build succeeds, and a scripted/dev-forced real kick (not the preview command) shows
   `CbKickScreen`, not vanilla `DisconnectedScreen`. If you can't force a real kick without a second
   client, at minimum confirm via logging that the mixin fires and consumes non-null on the real path.

Locked behavior, do not re-litigate:
- The preview command (`/cb debug kick <cause>`) stays as-is; it's a separate, already-working path.
- Screen text/colours/Technical Details/Copy Report are already correct (E2-E4 passed) — only the
  swap-not-landing bug is in scope. Don't touch `CbKickInfo`/`CbKickScreen` content.

## §D — Fast SP create/retexture burst stability (corrupt PNG race)

Already ruled out: `ResourcePackGenerator.writeLoosePack` already runs under `WRITE_LOCK` and every file
lands via `writeAtomic` (temp file + `ATOMIC_MOVE`) — that fix landed 2026-07-03, the same date this
regression was logged, and the bug still reproduces. The disk-write path is not the remaining cause.

Steps:
1. Read `network/ServerPackGenerator.java` in full (not yet read as of this handoff) — specifically
   `emit()` and whatever it reads from `SlotManager`/`TextureStore` per slot.
2. Look for a data race: two rapid `/cb create`/retexture actions in SP can trigger two `regenerate()`
   calls whose background threads both call `ServerPackGenerator.emit()` — if `emit()` reads mutable
   slot/texture state (e.g. a texture byte array or model field) without synchronization while a second
   edit is writing to that same state, one emit can read a torn/partial buffer and produce a corrupt PNG
   in the pack, independent of the disk-write path already fixed.
3. If confirmed, fix by either (a) synchronizing `emit()`'s read of the mutated fields against the
   mutation path (whatever command handler writes texture bytes on create/retexture), or (b) serializing
   `regenerate()` calls themselves (queue instead of spawning a new thread per call) so `emit()` never
   runs concurrently with a slot mutation or with another `emit()`. Prefer whichever fix matches the
   existing pattern in this file (it already serializes the write pass via `WRITE_LOCK` — extending that
   pattern to cover `emit()` itself is likely the smallest correct fix, but verify against how the
   mutation path actually writes texture data before committing to an approach).
4. Verify: build succeeds, and a scripted rapid double create/retexture burst in SP (script it if no
   in-game access) produces no corrupt PNG and no duplicate reload storm, matching D1/D2's expected
   result rows in `Testing_Guide_05.md`.

Locked behavior, do not re-litigate:
- Live in-place texture swap (§A), skip-redundant-reload (§B), tool resync (§C) are already Built/passed
  in TG5 — don't touch their code paths while fixing §D unless the emit-race fix requires touching
  shared code, in which case re-verify those three still behave (build + read, not a full retest — owner
  retests everything at the end).

## Testing
Owner tests §E and §D together, at the end, once BOTH are fixed and you consider them correct — not
incrementally in-game, and without asking the owner anything mid-session. Your own build/compile pass is
the only per-item checkpoint; move to the next item immediately after it's clean.

## When done
- TG4: leave §E's Sections-table status and Verdict/Progress as-is (still "Built 🎯" + "Regression 💔")
  — do NOT mark it Done or touch the ✅ dates yourself. Add a dated note to §E's row/Cleanup describing
  the fix you made and why, mirroring the existing 2026-07-18 deep-search note's style, so the owner's
  retest has context.
- TG5: same for §D — leave status/progress as owner-decided, add a dated fix note to the Cleanup entry.
- Do not fold either into the Confirmed archive. Only the owner moves a row to ✅ after testing.
