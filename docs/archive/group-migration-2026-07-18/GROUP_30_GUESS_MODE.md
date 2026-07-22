# Group 30 — Guess Mode

> **UI medium audit (2026-07-09):** GuessSettingsScreen confirmed right medium (Screen), flagged for a
> design/UX polish pass.

> **New group created 2026-06-25** from ISSUES.md G30-1 by owner decision.

> 🟢 **v1 BUILT 2026-07-05 (build-green, all gates), NOT in-game.** Toggle · blank name · disguise
> texture/icon · centered 3rd-person pose · held/all scopes · placement · drop · stale-ref fallback.
> Files: `core/GuessModeStore`, `network/GuessSync` + `payloads/GuessModePayload`, `client/ClientGuessState`,
> `client/render/GuessDisguise`, `mixin/BipedArmPoseMixin`, `command/handlers/GuessCommands`,
> `textures/misc/mystery.png`, + seams in `SlotBlock`/`SlotItemRenderer`/`AnimSlotBER`/`HudRenderer`.
> **Deliberately deferred (not bugs):** first-person two-handed pose, chest-GUI disguise picker, atlas
> placed-block disguise. Test plan = `docs/testing/GROUP_30_TESTING_GUIDE.md` (do §A pose FIRST).

> 🔁 **2026-07-05 — v2 command redesign locked, supersedes `scope`/`text`/`disguise` below.** See
> "2026-07-05 — MP test round + v2 command redesign" section further down for the current command
> shape, storage shape, and phased build plan. The v1 command tree/decisions above are history —
> read for context, don't rebuild them as documented; the redesign section is the current target.

## G30-1 · Guess-mode — blind the holder of a custom block (NEW feature)

> 💬 discuss — standalone; command surface (toggle + blank-text) design alongside G04-2; no data-corruption risk

> *"add a setting i can toggle for specific player … when they hold a customblock in their hands, the
> hands both go to the middle for holding the block, and they see all the block names blank or something
> i set it, and they dont see the texture. this is for a guessing game (guess what block is in your hands)."*

**Want** — A per-player "guess mode." While a flagged player **holds a custom block**: they can't see its
**name** (blank, or owner-set text) or its **texture** (shown as a blank/mystery look), and the block is
held with **both hands centered**. Everyone else sees the real block — so others know the answer and the
holder guesses. A party game built on the mod.

**Brand-new feature — nothing like it exists.** Confirmed groundwork:
- Only 3 client mixins today (`HudRenderMixin`, `ClientCommonNetworkHandlerMixin`, `ScreenInvoker`) —
  **no held-item / player-model mixin**, so the pose + texture-hide need a **new client mixin**.
- **No per-player server flag store** anywhere → the toggle is new infra + a sync packet.
- Name / texture / HUD already resolve **client-side** (`SlotBlock.resolveName`, `ClientSlotCache`,
  `HudRenderer`), so blinding *only the holder* is a **local** change on their client — it can't leak the
  hidden state to others, and it touches **no SlotData** (low corruption risk; this is render/UX, not the
  slot rails).

### Developer's decisions (locked via UI 2026-06-24)

- **Holder blind, others see real.** The blinding (blank name + hidden texture) is **local to the
  holder's own client**. Watchers render the block normally — they know the answer.
- **The two-handed centered pose is seen by EVERYONE** (holder's first-person *and* other players'
  third-person view). ← this is the costly part: it means the guess-mode flag must be **synced to all
  clients**, not just the holder, plus a player-model pose override.
- **Blank name is customizable in-game via a command** (default blank). Exact command/UX = brainstorm
  only, not decided.
- **Disguise LOOK is fully owner-customizable in-game (locked 2026-06-24).** The hidden block is **not** a
  fixed "?" — the owner chooses what the blinded holder sees: bundled mystery art, a solid/blank block, or
  **any texture / look they pick**, with "cool customization." The holder's held-item placeholder is a
  configurable setting (parallel to the configurable blank name). Default = a clear mystery look so they know
  they hold *something*. Exact picker UX = brainstorm (ties to the command/screen surface, G04-2).
- **Owner targets a player; it persists** (survives relog). The exact subcommand name/shape is **deferred
  — to be designed alongside the overall command surface** (ties to G04-2).
- **Blind scope = a TOGGLE between both modes (2026-06-24).** Owner-selectable per guess-mode session:
  **(a) held-block-only** (just the block in hands is blanked) OR **(b) all-custom-blocks-the-player-sees**
  (anti-cheat — they can't read a placed copy's name). Build both; expose a switch. *(Verified: name/texture
  resolve client-side, so "all blocks they see" is a local suppression on the holder's client only — no leak
  to others.)*

### Architecture brainstorm (NOT built)

**1. Per-player guess-mode flag (server, persistent).** New small store keyed by player UUID (mirror the
existing id-keyed stores like `LockManager`/`FavoritesManager`, persisted via an atomic JSON write). Set
by the owner's toggle command; survives relog.

**2. Sync the flag to ALL clients** (required because the pose is seen by everyone). A tiny
`GuessModePayload` carrying the set of guess-mode player UUIDs (+ the owner's custom blank text),
broadcast on change and on join. Each client keeps a `ClientGuessState` it can read while rendering any
player.

**3. Client render effects (all gated on the synced flag + "is this a `SlotItem`?"):**
   - **Blank name — holder-only, local.** When the **local player** is in guess mode, suppress the name of
     a held custom block: `SlotItem.getName` / the HUD name / the inventory tooltip return the owner's
     blank text instead. Because it keys on "am *I* the holder & in guess mode," other clients showing the
     same player are unaffected → others see the real name. *(Open: does "all block names blank" mean only
     the held block, or every custom block that player sees while in guess mode? The latter prevents
     cheating by matching a placed copy — see open questions.)*
   - **Hidden texture — holder-only, local.** You can't blank the shared atlas per-player, so instead
     **render the held item with a placeholder model/texture** on the holder's client only. The placeholder is
     **owner-chosen** (see decision): render whichever disguise texture/look the owner configured, defaulting
     to a bundled "mystery" image. New client item-model override for `SlotItem` when the local holder is in
     guess mode. *(Sync the chosen disguise id/text in `GuessModePayload` alongside the blank name.)*
   - **Two-handed centered pose — seen by everyone.** Two sub-pieces, both new mixins:
     - **First-person (holder's arms):** a `HeldItemRenderer` mixin forces the two-handed "filled-map"
       style centered pose when holding a `SlotItem` & the holder is in guess mode.
     - **Third-person (what watchers see):** a `PlayerEntityModel` / `PlayerEntityRenderer` arm-pose mixin
       sets both arms to a centered holding pose for any player who is in guess mode & holding a `SlotItem`
       (read from the synced `ClientGuessState`).

**4. Command surface (DEFERRED — brainstorm with G04-2 / overall commands).** Two commands needed: a
**toggle** (`/cb <verb> <player> [on|off]`, persistent) and a **blank-text setter**. Names/shape open;
the developer wants them designed as part of the whole command layer, not in isolation.

### The careful bits / risks (developer said "be careful")

- **Third-person two-handed pose has no exact vanilla equivalent.** Vanilla's two-handed map pose is
  first-person only; a both-arms-centered *body* pose likely needs a custom arm-pitch override and **must
  be verified in-game** before promising it looks right. This is the highest-uncertainty piece.
- **Texture hide = placeholder swap, not true deletion.** Decide the placeholder look (blank/white vs a
  bundled "?" mystery texture). It only changes on the holder's client.
- **Anti-cheat scope of name-blanking** — held block only vs all custom blocks for that player while in
  guess mode (so they can't read a placed copy's name). Affects how aggressive the local suppression is.
- **Low data risk, contained.** No SlotData mutation, no pack rebuild, no slot recycling — unlike most FX
  items, this can't corrupt blocks. Worst failure is a cosmetic glitch, fixable by toggling off.

### Decisions — all locked (2026-06-25)

- ~~**Name-blank scope**~~ — **resolved (2026-06-24): TOGGLE** between held-block-only and all-custom-blocks-the-player-sees. Both modes built; exposed as a switch.
- **Placeholder texture — fully owner-configurable per session.** The disguise look (what the blinded holder sees instead of the real block) is entirely set by the owner each time — any texture/look they pick, including custom art, Studio-exported images, or a bundled default. No hardcoded "?" required; the owner is in full control. A sensible default (clear "mystery block" bundled art) ships as a fallback when nothing is set.
- **Inventory + hotbar icon also hidden.** While in guess mode holding a custom block, the item icon in the hotbar slot AND inventory grid ALSO shows the mystery/disguise look — not the real block icon. Otherwise the holder could peek their hotbar to cheat. Same disguise texture, same configurability.
- **Command shape: deferred to G04-2 design session.** The toggle command verb + blank-text setter designed as part of the whole command-layer pass.
- **Third-person pose: net-new mixin, needs in-game feasibility confirm before committing.** No held-item/player-model mixin exists today; this is the highest-uncertainty piece. Verify pose looks correct in-game before building the full feature.

### 2026-07-05 — v1 scope locked + architecture re-grounded against real code

**v1 scope = core only.** Toggle, blank name, disguise texture/icon, centered pose. NOT in v1: reveal
animation, title cards, timer HUD, score/streak, hint command, multiplayer rounds, particle aura,
animated disguise — all deferred to later slices (§"100x" brainstorm below still stands as backlog).

**Build order: pose spike FIRST.** Build+verify the third-person arm-pose mixin alone (force the
centered pose on a test player, nothing else wired) before touching store/payload/name/texture. If it
looks wrong in-game, stop and rethink before the rest of G30 exists.

**Architecture correction — name + texture disguise need NO client sync at all; only the pose does.**
Research against real code (`SlotBlock.resolveName`, `SlotItemRenderer`, `HudRenderer`) found:
- `SlotItem.getName`/`resolveName` (`block/SlotBlock.java`) already resolves **per-client, locally** —
  hotbar/inventory/tooltip only ever render the local player's own stack. Blanking the name is a pure
  local check ("am I, the local player, flagged, and is this my held/relevant stack") — **no broadcast
  needed**, contrary to the original doc's implication that a flag might need wider sync for this part.
- Texture disguise reuses the existing `SlotItemRenderer` (`DynamicItemRenderer`) and splits cleanly on
  `ModelTransformationMode`: **GUI + first-person-hand modes** (always "my own item") → disguise, gated
  on the local flag. **Third-person-hand mode** (always "how others see it") → always render real,
  never disguise. This resolves the doc's earlier "how does the renderer know whose hand it is"
  ambiguity for free — no entity-context plumbing needed.
- **Only the arm/pose needs the all-clients broadcast** (new `GuessModePayload`, mirrors the existing
  `HudSync.broadcast` pattern) — because pose is skeletal animation seen by everyone, unlike name/texture.
- Store template confirmed: mirror `FavoritesManager` (`core/FavoritesManager.java`) — per-UUID map,
  atomic tmp-then-`ATOMIC_MOVE` write, `synchronized`, static self-load. New `GuessModeStore` persists
  per player: `enabled`, `disguiseSlotId`, `blankText`, `scopeMode` (held/all). Plus one **global default
  blank-text fallback** in config (not per-player) so new flagged players get sensible text without the
  owner setting it every time.
- Mixin file is `customblocks.mixins.json` (4 existing client mixins, none touch
  `HeldItemRenderer`/`PlayerEntityModel`/`PlayerEntityRenderer` — confirmed still true).

**"All-blocks" anti-cheat scope = 3 hook points, not 1.** Beyond `SlotItem.getName`, the existing
looked-at-block HUD readout (`HudRenderer`) AND the existing block-list chest GUI
(`ListSelection`/`ChestGuiCommands`, reused below for disguise-picking) both leak real names too — all
three get gated when scope = all-blocks.

**Disguise selection — all 3 UX paths, in v1:** (1) hold the desired block + run the command, grabbing
its slot id off the held stack; (2) type the block's id/name directly; (3) a chest-GUI picker reusing
the existing `ListSelection`/`ChestGuiCommands` list infra.

**Command shape — one verb, subcommands, op-only:** `/cb guess <player> on|off`,
`/cb guess <player> text <...>`, `/cb guess <player> scope held|all`, `/cb guess disguise <player> ...`
(all 3 selection paths above). No new permission node — same op tier as other admin commands.

**Placement while disguised — allowed.** The flagged player can place the block; they still see the
disguise/mystery texture on that placed block themselves (the same local per-client render gate extends
to world-block rendering, not just held-item), but everyone else sees the real block. No placement
restriction.

**Offhand — same treatment as main hand.** Guess mode (name/texture/pose) applies to both hands
identically; no exploit where switching hands reveals the real block.

**Death / dropped item — stays disguised until the same player re-holds it.** If the flagged player dies
holding the block, the dropped item entity keeps the mystery look; things resume normally once that same
player picks it back up. (No general "thrown to another player" case designed — narrow, death-drop only.)

**Stale disguise reference — falls back to a bundled default "?" mystery block.** If the owner deletes/
re-slots the block a disguise points at, `disguiseSlotId` silently resolves to a new bundled
question-mark `SlotBlock` shipped by default with the mod, instead of erroring or showing garbage.

### 2026-07-05 — MP test round + v2 command redesign (Round 1 🟢 BUILT, NOT in-game)

> 🟢 **Round 1 built 2026-07-05** (build-green, all gates) — command tree rewritten to
> `on <blockid>` / `on all` / `off <blockid>` / `off all` / bare `off`; `GuessModeStore` is now
> `{all, ids:Set}`; total per-block coverage (held/placed/inventory/hotbar/tooltip/item-frame/F5 +
> looked-at HUD); name hardcoded "???", look hardcoded bundled "?"; `scope`/`text`/`disguise` +
> `guessDefaultBlankText` config removed; arm pose A1/A2 angles retuned. Round 2 (break
> particles/sound/drop through-break) + F3 still deferred. Retest = `GROUP_30_TESTING_GUIDE.md`
> (§A pose FIRST — angles are a guess). Below is the locked design it was built from.

**Context.** First MP test round ran (see TG). Real bugs found: A1/A2 pose off-center + POV-switch
glitch; E1-E3 all-scope not blanking anything. D/F/G "failures" were mistests (plain atlas test
blocks, not off-atlas — retest later with the right block type, not a code bug). H1/H2 (stale
disguise fallback) confused the owner enough that, on discussion, the whole command surface got
reconsidered rather than just re-explained.

**Decision: nuke `scope`, `text`, and `disguise` commands. Replace the whole command surface.**
Owner found 4 separate setup commands (`on/off`, `scope held|all`, `text <name>`, `disguise
held|<id>|clear`) too fiddly and confusing in practice. New shape, locked:

- `/cb guess <player> on <blockid>` — disguise only that one block type for that player.
- `/cb guess <player> on all` — disguise every custom block that player can see.
- `/cb guess <player> off <blockid>` — stop disguising just that one block type.
- `/cb guess <player> off` — bare, clears everything (all block-ids AND all-mode) for that player.
- **No separate `off all`.** Bare `off` is the only full-clear.
- **Block-ids stack.** Running `on <blockid>` multiple times adds to a set — a player can have
  several specific blocks disguised at once without needing `all`. `on all` is independent of the
  stacked set (conceptually a superset), not mutually exclusive with it.
- **Name is hardcoded `"???"`, always.** No `text` command — this also closes old enhancement
  G30-1f (C3) by design, not as a bonus feature.
- **Look is hardcoded to the bundled "?" mystery block, always.** No `disguise` command, no
  per-owner custom look. This also removes the confusing "clear"/fallback surface entirely (H1/H2)
  — there's nothing left to fall back FROM, since there's only ever one look.
- **Store shape must change:** `GuessModeStore` moves from single `disguiseSlotId`/`scopeMode`
  fields per player to a **set of block-ids** + an **all-mode boolean**, per player.

**Decision: coverage must be total — the player must not be able to identify the block by ANY
means while flagged for it.** This is broader than v1's render-only scope. Confirmed in-scope:
held item, world-placed block, inventory/hotbar icon, item frames/display entities, **and item
tooltip text** (hovering must also show "???", not the real name — tooltip was previously assumed
covered by name-blank, needs explicit verification once `text`/`disguise` are gone). Breaking a
disguised block (particles, break sound, dropped item) must **stay disguised through the break**,
not just resolve to real on impact — extends the existing death/drop "stays disguised until the
same player re-holds it" rule (§G30-1, "Death / dropped item") to apply on ordinary breaking too,
not just death.

**Explicitly out of scope: F3 debug screen.** F3's block-id readout comes from the client's own
authoritative block state, not a render layer — spoofing it would need swapping what block is
actually there client-side, a much deeper and riskier change than the rest. Owner decided to drop
it this round; revisit later as its own small project only if it turns out to matter in practice.

**Rollout: phased, two rounds, not one giant batch.**
- **Round 1 (next session):** rewrite the command tree above; update `GuessModeStore` to the
  set+all-mode shape; get held/placed/inventory-icon/tooltip coverage working and confirmed for
  the hardcoded "?" look + "???" name. This also supersedes/replaces the old E1-E3 all-scope bug —
  don't debug the old `scope all` code, it's being torn out and rebuilt as `on all` from scratch.
- **Round 2 (Total Blindness - future session):** The guesser must be completely blind to the block's true identity. This includes spoofing all physical/mechanical leaks:
  - **Hitboxes & Collision:** The disguise must feel physically solid like a 1x1 cube, overriding both `shape` (stairs/slabs) and `noCollision` (passable blocks). The guesser cannot walk through it or feel a stair step.
  - **Light Emission (Glow):** If the real block glows, the chunk will light up. This is a known hard-leak because Minecraft lighting is chunk-based, not client-based. The guesser will see the light. (Needs deep engine mixin to spoof client-side chunk light, parked for Round 3).
  - **Mining Speed:** The block must break at a generic, average speed for the guesser, hiding if the real block is instant-break or obsidian-hard.
  - **Particles:** Punching, breaking, sprinting, or falling on the block must spawn disguise particles, not real textures. (If using the bundled `?` block, spawn custom generic mysterious sparkles).
  - **Sounds:** Hitting, breaking, placing, or walking on the block must play the disguise block's sound. (If using the bundled `?` block, a new custom sound event `customblocks:mystery_break` will play—a cooler, punchy ~1-second version of amethyst breaking).
  - **Drops:** The item dropped upon breaking must stay disguised.
  - Revisit F3 if it turns out to matter.

> ✅ **Round 2 slice 1 — BUILT + PASSED in-game 2026-07-09 (TG §R, MP).** Covers Hitboxes/Collision and
> Particles from the list above:
> - **R1 (hitbox/collision)** — 2 client mixins (`AbstractBlockStateMixin.getOutlineShape` → forces a
>   full cube, outline-only so no server desync; `ParticleDisguiseMixin` cancels break particles) grounded
>   by javap on the YARN-named merged jar. Gate: `GuessDisguise.blinds`.
> - **R2 (particles)** — the holder's own break particles are swapped for a bundled `mystery_break` "?"
>   particle instead of just being suppressed; a 2nd player still sees the real particles. Fires via
>   `ParticleManager`, not `WorldRenderer.processWorldEvent(2001)` (the breaker is excluded from that path).
>   **Scope grew 2026-07-09** — owner wants this pushed further: pair it with the sound stub below **and**
>   add slight randomness/motion to the particle itself (right now it's one fixed effect every time).
> - **R3 (drops)** — turned out `SlotBlock` never had a loot table at all (dropped nothing, disguised or
>   not); added a real `getDroppedStacks` override so **every** custom block drops its own item on a
>   survival break, with the "?" cube shown to a flagged holder. **This is general SlotBlock behavior, not
>   Guess-Mode-specific** — moved to **Group 06 §G06-7** (Block Drops Customization) as its `SELF`-default
>   baseline, confirmed here first. Kept in this doc's history only; future drop-system work happens in G06.
> - **Still open (§R-2, not built):** break/dig sound → `customblocks:mystery_break` stub (needs a
>   `@Redirect`); mining-speed spoof is a client-only fake that desyncs with the server — pending a ruling
>   before building.

- Old enhancement G30-1e (B4, animated on/off transition) stays parked, unrelated to this
  redesign — owner wants to keep discussing it separately, not bundled in.

**Arm pose bug (A1/A2) is a separate, smaller, isolated task** — not part of this redesign. Fix
lives in `BipedArmPoseMixin.java` (angle constants ~line 49-50) + a POV-switch desync to
investigate. Can be done independently, in either order relative to Round 1.

### "100x more advanced + cool" brainstorm — YouTube-quality design (NOT built)

This is for YouTube recordings. Everything about it must look polished, intentional, and exciting on camera.

**Presentation layer (what the guessing player experiences):**
- **Dramatic reveal animation.** When guess mode ENDS (player guesses or time runs out), the real block fades/spins into view with a sound cue + particle burst. Not a simple swap — a cinematic reveal.
- **Custom title card.** `title` + `subtitle` packet shown on guess start: owner-set text ("🎯 What block are you holding?") with custom colour/font style. Big, bold, camera-ready.
- **Timer display.** Optional visible countdown on the holder's HUD (ties G27-1 HUD) — counts down from X seconds, owner-configurable. When it hits zero, the round auto-ends.
- **Score / streak tracker.** Per-session correct/wrong count shown in a HUD brick (ties G27-1). Shows the holder's current streak for YouTube tension.

**Disguise quality (what it looks like in-hand):**
- **Animated mystery texture.** The disguise texture can itself be a GIF or cycle through frames (ties G14-1) — a spinning question mark, a glitchy "???" — not a static image.
- **Particle aura.** A subtle particle effect around the held item while in guess mode (existing `ParticleFx` system) — signals "this is mystery mode" visually.
- **Custom item name with formatting.** The item's name while disguised is fully formatted (colour, bold, animation prefix) — e.g. "§5§l✦ Mystery Block ✦" — set by owner.

**Game-mode enhancements:**
- **Multiplayer rounds.** Owner queues N players, each gets a turn, scores tallied at end. Round-robin or simultaneous (all players in guess mode at once, same block, race to guess first).
- **Audience mode.** Players not in guess mode see a special HUD indicating "they're guessing!" so the stream audience knows who's guessing.
- **Hint system.** Owner can `/cb guesshint <player> <text>` to push a hint to the guessing player's HUD mid-round (without revealing the answer to chat).
- **Auto-reveal after timer.** If the timer runs out with no guess, the real block reveals itself with the dramatic animation + a "Time's up!" title card.
- **Guess submission via command or chat.** `/cb guess <block-name>` as a structured guess (owner can validate it) OR monitor chat for matching block names (simpler, less reliable).

**Open items (still to confirm):**
- Third-person pose feasibility (in-game verify first).
- Command shape (tied to G04-2).
- Timer HUD: ties to G27-1 — design the countdown brick as part of the HUD pass.
- Multiplayer rounds: confirm whether this is v1 or a later slice.

| Fact | Detail |
|---|---|
| Client mixins today | `HudRenderMixin`, `ClientCommonNetworkHandlerMixin`, `ScreenInvoker` — none for held-item/player model |
| Held name source | `SlotBlock.resolveName` → `SlotItem.getName` / HUD / tooltip, all client-side (`SlotBlock.java:64,237`) |
| Per-player flag store | none exists — new infra (model on `LockManager`/`FavoritesManager`) |
| Blinding cost | holder-only = **local** to their client (no leak); cheap + safe |
| Pose cost | seen-by-everyone = flag synced to **all** clients + first- & third-person mixins (the heavy part) |
| Data risk | none — no SlotData mutation, no pack rebuild |

| Group | Scope | Status |
|---|---|---|
| G30 — Guess Mode (NEW) | Both — render/UX; designed for MP party play + YouTube recording | 🔍 designed — core + inventory/hotbar hide + fully-configurable disguise locked 2026-06-25; 3rd-person pose needs in-game feasibility confirm; command shape + timer/rounds = later slices |

**Related:** G04-2 (command surface — toggle + text setter designed with it) · G27-1 (HUD — timer + score bricks) · G14-1 (animated mystery texture via showcase cycle) · G10-1 (render territory) · `HudRenderer` (name suppression path) · `SlotBlock.resolveName` / `ClientSlotCache` · ADR-009 (resolver-seam pattern for client state)
**Touches:** new per-player `GuessModeStore` (server, persistent, mirrors `FavoritesManager`) · new
`GuessModePayload` broadcast-to-all (pose sync ONLY, mirrors `HudSync.broadcast`) + `ClientGuessState` ·
new `HeldItemRenderer` mixin (1st-person pose) · new `PlayerEntityModel`/renderer mixin (3rd-person pose,
reads synced flag for any player) · `SlotItem.getName`/`resolveName` (local name blank, no sync) ·
`SlotItemRenderer` gated on `ModelTransformationMode` (disguise texture, local, no sync) · `HudRenderer` +
`ListSelection`/`ChestGuiCommands` (2 extra leak points blanked in all-blocks scope) · new
`/cb guess`/`/cb guess disguise` subcommand tree, op-only · bundled default "?" mystery `SlotBlock` (stale
-ref fallback) · `customblocks.mixins.json` (new client mixins).
**Deferred to later slices (not v1):** `ParticleFx`/`SoundFx` reveal animation, title/subtitle packet,
timer HUD, score/streak tracker, hint command, multiplayer rounds.
**Verify in-game:** (1) pose spike alone looks correct on a test player (build/verify BEFORE the rest of
G30 exists); (2) holder sees owner-set disguise look + blank name + centered pose (first-person, both
hands); (3) hotbar AND inventory icon also show disguise look; (4) other players see real block + two-
handed pose; (5) all-blocks scope also blanks the looked-at-block HUD and the list-GUI; (6) placing the
block while disguised still shows the disguise to the holder, real block to others; (7) dying holding it
keeps the disguise on the dropped item until the same player re-holds it; (8) toggling off restores
everything instantly; (9) relog keeps the flag; (10) a stale/deleted disguise reference falls back to the
bundled "?" block instead of erroring.

---

## v3 — Customization + camera/content features (2026-07-06, DESIGN ONLY — nothing built)

> **Context.** All 26 multiplayer tests for v2 pass, confirmed in-game 2026-07-06 — Guess Mode itself is
> done. This is the next round: owner wants (1) the "?" look, the pickup/put-down snap, and the pose to
> all be customizable instead of hardcoded, and (2) a batch of new features specifically for recording
> videos/streams of the game — brainstormed at length via UI, several rounds, until it converged. Nothing
> in this section is built yet. Full design below; a handoff brief for a fresh session is in
> `docs/testing/GROUP_30_TESTING_GUIDE.md` planned section / see PROGRESS_LOG for the handoff text.

### v3 issue-ID map (assigned 2026-07-07, revised 2026-07-07 discussion pass — canonical copy in `docs/Information/ID_MAP.md`)

Each build slice below now has a real `G30-n` id so "the remaining v3 work" is trackable one slice at a time.
**Big scope pass 2026-07-07 (discussion-only, nothing built this pass):** owner cut hint ladder, round
timer, timeout-TNT, and boss-tension entirely; killed the `/cb guess pose` command in favor of screen-only
tuning; expanded the pedestal idea into a full **Showcase** feature; and locked a **ship-now vs ship-later**
split (see bottom of this file). Full detail in every slice below.

| Id | Slice (spec §) | Status |
|---|---|---|
| **G30-2** | §1 disguise-look customization + fallback chain | ✅ Phase 1 (id-based) in-game 2026-07-06 · Phase 2 link ⛔reverted |
| **G30-2b** | §1 QuestionMark fallback fix (texture regen + delete old purple `mystery.png`) | 🟡 built 2026-07-08 (regenerated 512×512 sharp/black, `mystery.png` deleted) — pending in-game (TG §P) |
| **G30-3** | §2 smooth pickup/put-down transition | ✅ in-game 2026-07-06 |
| **G30-4** | §3 pose/settings screen (now the unified `/cb guess` mega-screen) | ✅ Pose tab confirmed in-game 2026-07-07 · 🟡 `/cb guess pose` command now actually **removed** + screen drives pose via `GuessPosePayload` (built 2026-07-08, re-confirm TG §N) |
| **G30-5** | §4 manual round-control keybinds | ⬜ not built — **parked for the later "round-control + effects" pass**, spec locked 2026-07-07 |
| **G30-6** | §5 cursed-buzz effect | ⬜ not built — trimmed spec, **parked**, see below |
| **G30-7** | §6 hint ladder | ⛔ **CUT entirely 2026-07-07** — owner decision, see below |
| **G30-8** | §7 stage-zone + shockwave | ⬜ not built — **parked**, spec locked 2026-07-07 |
| **G30-8b** | §8 Showcase (was "pedestal") | 🟡 built 2026-07-08 (real placed block + BER + shared tuning + live Showcase tab), pending in-game (TG §S) |
| **G30-9** | §8 round timer + timeout TNT + boss-tension | ⛔ **CUT entirely 2026-07-07** — owner decision, see below |
| **G30-10** | §9 sound-swap while disguised | ⬜ not built — **parked**, spec locked 2026-07-07 |
| **G30-11** | §10 unified `/cb guess` mega-screen (tabs: Pose · Buzz · Sound · Look · Showcase) | ✅ shell in-game 2026-07-08 (TG §N) · 🟡 Pose + Showcase now LIVE tabs (Buzz/Sound/Look greyed), pending in-game (TG §S) |
| **G30-12** | parked — impostor round | ⛔ parked |

### Key constraint that shapes everything below

**Guessing happens by voice on a Discord call, not in Minecraft.** The mod can never know whether a
spoken guess was right or wrong — there is no chat message or in-game action to detect. So **every round
outcome (correct / wrong / end) must be manually triggered by the owner (OP) on the spot**, via a keybind,
while they're mid-conversation. This is why the control model below is keybind-first, and why nothing here
tries to auto-validate an answer.

### 1. Disguise look — fully customizable (was hardcoded to one bundled "?" cube)

> ✅ **Phase 1 DONE 2026-07-06 (in-game confirmed, SP + MP — `GROUP_30_TESTING_GUIDE.md` §I, all 10 rows).**
> ID-based look only (a look = an existing custom block's id; the inline image/GIF-link convenience is Phase 2). Shipped: `/cb guess
> defaultblock <id>|clear` (global default) + `/cb guess <player> on <blockid> look <lookid>` (per-round
> override) + the round→default→bundled-"?" fallback chain. Look ids are stored in `GuessModeStore`
> (`ids:Set` → `looks:Map<flaggedId,lookId?>` + a global `_default`), resolved to slot indices in `GuessSync`,
> drawn by `GuessDisguise.drawLook(lookSlot)` (reuses `AnimFrameCache`/`StaticFrameCache`; animated looks play
> live). Watchers still see the real block. Also stripped the leftover v2 `[G30-DEBUG]` scaffolding this
> session. Test rows = `GROUP_30_TESTING_GUIDE.md` §I.
>
> ❌ **Phase 2 (paste a raw image/GIF link) REVERTED 2026-07-06** at the owner's request — links are no longer
> part of guess mode. The `defaultblock link <url>` / `look link <url>` sub-commands and their handlers
> (`defaultLookSetLink`, `onIdLink`, `freshLookId`) were stripped from `GuessCommands`; the `ImageDownloader`
> import went with them. The shared `/cb create` rail (`CreationCommands.doCreate` + `AnimCommands`
> `maybeCreateAnimated`'s `postApply`) was left intact — the creation studio still uses it, it was never
> guess-only. Disguise looks are now **existing block ids only** (§I). Build-green after revert.
>
> **Bundled fallback = QuestionMark cube.** The last-resort look (no override, no default) is a bundled
> `textures/misc/questionmark.png` (glossy red "?" on a dark cube, built from a pngimg question-mark image
> at bundle time — not a runtime link). `GuessDisguise.MYSTERY` points at it. The old `mystery.png` is kept
> on disk for easy revert.
>
> 🟡 **G30-2b BUILT 2026-07-08 (build-green, pending in-game — TG §P).** The old `questionmark.png` was
> **64×64** with a **`(35,35,35)` grey** background (the blur + grey-not-black complaints). Regenerated at
> **512×512** with a **sharp red "?" on true `(0,0,0)` black**, and **deleted** the old purple `mystery.png`
> from disk. `GuessDisguise.MYSTERY` already pointed at questionmark.png. A texture-quality fix only — not a
> new block type.

- `/cb guess defaultblock <blockid>` — sets your persistent go-to disguise look. An **existing custom block's
  id only** (reuses its already-baked picture). No link input.
- `/cb guess <player> on <blockid> look <lookid>` — optional per-round override, existing-block id only.
- **Fallback chain:** this round's override → your saved default → the bundled **QuestionMark** cube if you
  never set a default. Never errors, never shows garbage.
- A brand-new picture doesn't need to be a placeable block — `/cb create` it once (never has to be
  placed anywhere) purely so it exists as a reusable id, then point the look at that id.

### 2. Pickup / put-down transition — smooth, not a snap

- Arms glide from the normal pose into the guess-mode pose (and back) over roughly 0.3s instead of
  snapping instantly. Build with a placeholder duration; tune the exact feel after watching it in-game.

### 3. Pose / Settings tab — **G30-4** (✅ Pose tab confirmed in-game 2026-07-07, screen NEEDS REWORK into §10's unified mega-screen)

> 🟡 **`/cb guess pose …` command REMOVED — BUILT 2026-07-08 (build-green, pending in-game — TG §N).** Pose
> tuning is **screen-only** now — no command surface at all. Reason: a command AND a screen editing the same
> value was "two things fighting." `GuessPoseCommands.java` and its `.then(…)` in `GuessCommands` are deleted;
> the `GuessPoseStore` / `GuessSync` / `BipedArmPoseMixin` backend stays. The screen sends the whole six-angle
> pose in one new **`GuessPosePayload`** (client→server, op-checked server-side → `GuessPoseStore.setAll` →
> `GuessSync.broadcast`) instead of building a chat-command string.

Sliders per arm — up/down (pitch), in/out (yaw), twist (roll) — independent per arm (left can sit lower
than right). A live 3D player-dummy preview updates as you drag. The pose is **ONE shared pose applied to
every currently-flagged holder** (not per-player) — every knob is a saved + all-clients-synced value.

**Full knob list — locked 2026-07-07:**
- **Arms (per arm, independent L/R):** up/down (pitch) · in/out (yaw) · twist (roll).
- **Block placement (slice 1b, not yet built):** height · size · distance-out (forward/back) · sideways
  (left/right) · fixed tilt. Distance-out allowed to go **full arm's length** (no tight cap). Size allowed
  a **wide range** (tiny pebble up to an oversized prop).
- **Block animation (slice 1c, not yet built):** idle spin — pick **one** axis style at a time (turntable /
  tumble-forward / free-spin), no stacking multiple styles at once · gentle bob/float · breathing pulse
  (size loop) with **separate speed slider AND separate amount/swing slider** · momentum sway (block
  lags/jiggles on move) — has **both** a plain on/off toggle **and** a tunable strength slider.
- **Body / flair (slice 1c):** head look-down — **tunable slider**, not fixed · sneak/lean posture — a
  **visibly noticeable crouch/lean**, not subtle · glow/outline — **two separate toggles**, one for the
  block, one for the player, independently switchable. Not taken: floating "?" marker (owner declined).
  Particle aura is its own separate later slice, not part of this knob set.

**Screen-level controls (apply to every tab, not just Pose)** — moved to `GROUP_27_SCREENS.md` §G27.26
(2026-07-12): number-entry sliders, symmetry lock, reset/save-as-default, presets dropdown, live 3D dummy,
unsaved-mid-drag popup, sound, theme, access keybind, concurrency, greyed-placeholder-tab pattern. G30 keeps
what each control actually *does* per tab (Pose angles, Buzz tuning, Showcase sliders, etc.) — the pattern
itself is generic Screen chrome, reused wherever a mega-screen has live-preview sliders + presets.

### 4. Manual round control (keybinds) — **G30-5, parked for the later round-control pass**

Three OP-only keybinds, because you're judging the round by ear over Discord, not by anything the mod can
detect. **Locked spec 2026-07-07, not yet built — bundled into the same future pass as §5/§7/§9 since
they're one connected system:**

- **Wrong-buzz** — soft tier. Plays the cursed-buzz effect (§5); round KEEPS GOING. **Unlimited presses**,
  no cap that forces a reveal. Targets **whoever is in the OP's crosshair** at the moment of the press — if
  nobody flagged is in view, it's a **silent no-op** (no message).
- **Correct/reveal** — hard tier. Unlike wrong-buzz, this is an **"end everything" button**: pressing it
  ends **every currently-flagged holder's round at once**, not just whoever's in the crosshair (the one
  deliberate exception to crosshair-targeting, because it's meant as a full wrap-up moment). Each holder
  still gets their own independent shockwave/reveal effect even though they all fire together.
- **Force-end/abort** — ends a round quietly, no effects. Also crosshair-targeted. `/cb guess <player> off`
  (the existing command) stays available too, as the precise typed way to end one specific person's round
  without needing to aim at them.
- Both correct-reveal and force-end fire **instantly, no hold-to-confirm delay** — trusted to the OP's own
  timing since they're mid-conversation.
- These keybinds **only do anything while a round is actually active** (someone flagged). Pressing
  correct/force-end with nobody flagged shows an explicit **"no active round" chat message** rather than
  silently doing nothing.
- **Round state model (multi-holder, locked 2026-07-07):** more than one player CAN be flagged/guessing at
  once. Round *state* (who's flagged, their disguise/look, whether their round is active) is **per-holder
  independent**. Tuning *settings* (pose, buzz feel, sound choice) stay **one shared global value** applied
  to whichever holder(s) are currently flagged — you don't get a different pose per person.
- **Server restart/crash mid-round:** flagged state **persists through restart** — picks back up exactly
  where it left off, same as everything else this mod saves to disk.
- **Holder awareness:** the flagged player gets a **small one-time chat message** the moment they're first
  flagged (something like "You're the mystery block now!") — nothing further after that, no ongoing HUD.
- **OP-side round status:** rather than a bespoke HUD overlay, route this into the existing **`/cb edithud`**
  system as a widget (who's flagged, round state) — don't build a separate always-on corner display.

### 5. Wrong-buzz effect — "cursed buzz" (soft tier) — **G30-6, parked, trimmed spec**

> The picture-flicker part of this effect (flashing the block through random decoy pictures) originally
> reused the hint ladder's decoy pool. **Hint ladder is cut (see §6 below), so the flicker is cut too** —
> owner chose to simplify rather than build a standalone picture-pool just for this. Kept:

- A harsh **full-screen** red static/glitch flash (not edge-only vignette) — sharper/uglier than vanilla's
  damage-flash.
- A discordant buzzer/glitch sound.
- A **real camera shake** (actual camera movement, not a visual-only distort).
- Seen/heard by **everyone nearby** (spectators too, not just the flagged holder or just the OP).
- No accessibility/reduce-flash toggle wanted — full intensity always, small-group-of-friends use case.
- Every piece independently toggleable/tunable (flash colour/intensity/duration, sound choice/volume, jolt
  strength on/off) from the settings screen's Buzz tab, **with the same live dummy-preview pattern as the
  Pose tab** — watch the combo play out on the test dummy before it's live.

### 6. Hint ladder — **CUT ENTIRELY, owner decision 2026-07-07**

> Was: a keybind-advanced ladder (decoy-cycling → silhouette/outline → blur → reveal). Owner found the
> customization surface (reorder/enable/disable per stage) too confusing and not worth the complexity —
> **dropped completely, no simplified version either.** Guessing is fully freeform over Discord voice with
> just the wrong/correct/end keybinds (§4) and no built-in clue system. **Do not rebuild any version of
> this** without a fresh owner decision — id **G30-7 retired**.

### 7. Correct-reveal shockwave — **G30-8, parked, spec locked 2026-07-07**

- Only plays inside a **pre-marked "stage zone"** — marked with **two corner clicks** (WorldEdit-style: click
  one corner, click the opposite corner, zone = the box between). Optional feature — a round works fine
  with no zone marked, this just adds the effect when one exists.
- Effect on reveal: **particle ripple ring expanding outward, plus a light knockback/wobble** on nearby
  entities standing in the zone — **excluding the OP** (the OP triggering it never gets wobbled themselves).
- This marking is unrelated to the Showcase (§8 below) — Showcase does not require a stage zone to exist.

### 8. Showcase (was "pedestal") — **G30-8b, ships now (not parked), full spec locked 2026-07-07**

> Renamed from "pedestal" — owner felt the word didn't land. **Showcase** it is:
> `/cb guess showcase spawn [blockid]` / shift-right-click **directly on** an existing Showcase to delete it,
> or `/cb guess showcase delete`. **Op-only**, same permission tier as the rest of guess mode.
>
> 🟡 **BUILT 2026-07-08 (build-green, all gates, pending in-game — TG §S).** Implemented as a **real placed
> block** (owner's pick — reuses the proven BlockEntity + BER pipeline, free persistence + sync) rather than a
> from-scratch hologram: new `block/GuessShowcaseBlock` (INVISIBLE model, no collision, shift-right-click →
> op removes) + `GuessShowcaseBlockEntity` (holds the shown slot, `markForUpdate` sync) + `GuessShowcaseRegistry`
> (block + BE type, **no item** — command-spawned only). `client/render/GuessShowcaseBER` draws the two layers:
> a spinning picture **core** (the shown slot's baked texture via `GuessDisguise.textureForLook`) + a larger,
> canted, translucent **outer layer** spinning the other way, plus a bob; glow → full-bright, particles →
> end-rod puffs. The look/feel is **one shared tuning** (like the pose) in `core/GuessShowcaseStore`
> (inner-speed · orbit-speed · size · glow · particles), synced via `GuessSync` "showcase" object →
> `ClientGuessState`, edited on the new **Showcase tab** and pushed with `GuessShowcasePayload` (C2S, op-checked).
> Command `command/handlers/GuessShowcaseCommands` (`spawn [id]` / `delete`) attaches under `/cb guess`. Orbit
> distance auto-scales with size (outer layer is a fixed multiple of the core). Assets: `guess_showcase`
> blockstate/model (cube_all → questionmark particle) + lang.

- **Not tied to an active round** — can be spawned any time, purely as a decoration/display piece, as well
  as for showing off the current mystery block during a round.
- **Placement:** spawns **one block above your feet** (like WorldEdit's `//up 1`) — not directly on your
  feet. No collision handling needed beyond that.
- **Content:** if a block id is given (`spawn <blockid>`), shows that block's picture. If **no id is
  given**, chat tells the player **"no block specified"**, then falls back to the bundled **QuestionMark**
  image (same fallback as the disguise system, see G30-2b).
- **Removal:** persists until **manually removed** — no auto-despawn on round end, no timeout. Multiple
  Showcases can exist at once (e.g. one per parallel round) — each is deleted independently by
  shift-right-clicking that specific one (must be aimed directly at it, not "nearest").
- **Visual — full end-crystal-style build, fully tunable, "very cool advanced" per owner:**
  - **Two layers:** an inner spinning core (the picture) + an outer orbiting frame/cage around it, plus a
    gentle bob — matches vanilla End Crystal's structure as the visual reference.
  - **Inner spin speed and outer orbit speed are independently tunable sliders** (not one shared speed).
  - **Orbit distance** (how far the outer layer floats from the core) **auto-scales with the size slider** —
    no separate distance knob to manage.
  - **Resizable** — a size slider scales the whole display (core + outer layer + orbit distance together).
  - **Glow/outline toggle** — off by default, owner-switchable on.
  - **Particles** — customizable, on/off toggle (not forced on or off).
- **Where it lives in the UI:** Showcase is its **own tab** inside the unified `/cb guess` mega-screen
  (§10) — not folded into the Pose tab's animation sliders, since it's a separate object from the held
  disguise block.

### 9. Sound-swap while disguised — **G30-10, parked, spec locked 2026-07-07**

- Replaces **place, break, AND footstep** sounds for a disguised block with a mystery sound (full swap, not
  just place/break) — nothing about the block's audio gives away the real material.
- **Just one bundled mystery sound** (not a list of presets to choose from) — kept deliberately simple.
- **Only active while the round is running** — reverts to the block's real sounds the moment the round
  ends, same behavior as the picture/name disguise.

### 10. Unified `/cb guess` mega-screen — **G30-11, architecture locked 2026-07-07**

> Replaces the earlier idea of a single flat `/cb guess settings` screen. Instead: **one big `/cb guess`
> screen hosting everything as tabs**, with a **persistent tab bar always visible** (click any tab any
> time) — the same pattern already used by `/cb create`'s tabbed screen,
> [`BlockCreationStudioScreen.java`](../../src/main/java/com/customblocks/client/gui/BlockCreationStudioScreen.java)
> — reuse that tab-bar logic rather than reinventing one. Typed subcommands are just shortcuts into a
> specific tab of the SAME screen: `/cb guess settings` opens the mega-screen already focused on the
> Settings/Pose tab, `/cb guess showcase` (if ever given a screen entry point) would open it focused on the
> Showcase tab — but the tab bar is always there, so you can freely click over to any other tab from
> either entry point.
>
> **Final tab lineup (post-cuts):** **Pose** (§3, includes arm/placement/animation/flair sliders) ·
> **Buzz** (§5, cursed-buzz tuning + live dummy preview) · **Sound** (§9, mystery-sound picker) ·
> **Look** (§1, default disguise-look picker) · **Showcase** (§8, the end-crystal-style display tuning).
> Owner's explicit instruction: **keep this organized, not cluttered/scattered** — group related controls
> within each tab rather than a long flat list of sliders.
> 🟡 **Tabbed shell BUILT 2026-07-08 (build-green, pending in-game — TG §N).** `GuessSettingsScreen` now hosts
> the persistent tab bar **Pose · Buzz · Sound · Look · Showcase**; Pose is the live tab (its sliders + live
> dummy carry over unchanged), the other four are greyed placeholders showing a "coming soon" hint on
> hover/click. Reuses the `CbButton.tab` primitive. The placeholder tabs light up as each slice is built.
> 🟡 **Showcase tab now LIVE too (2026-07-08, TG §S)** — clicking it switches the screen to three tuning sliders
> (inner spin · outer orbit · size) + Glow / Particles toggles, pushing `GuessShowcasePayload`. Buzz/Sound/Look
> stay greyed. Both live tabs share the same inline number-entry / nudge / right-click-reset slider behaviour.

### Parked — explicitly NOT in this round

- **Impostor round** (spot-the-odd-one-out among several look-alike blocks). Genuinely a different game
  shape (multiple blocks, no single disguised holder) — owner chose to give it its own future design
  session rather than bolt it onto this pass.

### Ship-now vs ship-later split — locked 2026-07-07

**Shipping now** (buildable next, no further design needed):
1. Disguise system — already built (name-blank, look-swap, pose tab, mega-screen groundwork).
2. **Showcase (§8/G30-8b)** — 🟡 BUILT 2026-07-08 (pending in-game, TG §S). Real placed block + BER + shared
   tuning + live Showcase tab.
3. QuestionMark texture fix (G30-2b) — ✅ in-game 2026-07-08.

**Parked together for one later "round-control + effects" pass** (deliberately bundled — they're one
connected system, don't build them piecemeal):
- Manual keybinds (§4/G30-5)
- Cursed-buzz effect (§5/G30-6)
- Stage zone + shockwave (§7/G30-8)
- Sound-swap (§9/G30-10)
- Default keys for the wrong-buzz/correct-reveal/force-end keybinds are **not yet chosen** — deferred to
  when this pass actually gets built (only the Settings-screen keybind, **K**, has been assigned already).

**Cut entirely, not shipping in any form:**
- Hint ladder (§6/G30-7)
- Round timer, timeout TNT consequence, and boss-tension mode (all under the old §8/G30-9) — all three
  depended on the timer existing, so all three went together.

### Build order note (for whoever picks this up)

Do **not** attempt everything in one sitting (CLAUDE.md: max 5 items per plan). Suggested order given the
split above:
1. QuestionMark texture fix (G30-2b) — quick, isolated.
2. Rework `GuessSettingsScreen` into the unified tabbed `/cb guess` mega-screen (G30-11), reusing
   `BlockCreationStudioScreen`'s tab-bar pattern — this is the container everything else plugs into.
3. Pose tab slice 1b (block placement sliders) then 1c (block animation + body/flair) — finishes what's
   already partway built.
4. Showcase (G30-8b) as its own tab in the mega-screen.
5. *(separate future session)* the bundled round-control pass: keybinds (§4) → cursed-buzz (§5) → stage
   zone/shockwave (§7) → sound-swap (§9), built together since they share the same round-state plumbing.
