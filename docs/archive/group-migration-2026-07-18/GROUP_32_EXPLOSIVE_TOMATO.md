# Group 32 — Explosive Tomato

> **Status:** Phase A + core Boom built & MP-tested 2026-07-15; owner retest found regressions + locked a rework,
> and that rework is now **BUILT 2026-07-16** (awaiting one owner retest batch — see TG32):
> - **A1** ✅ built — flight billboard flip fixed (renderer UV `v` flipped) + icon unified: inventory/hand and
>   flight all use the **same round 256px sprite** (flat `item/generated` card from the owner's locked art). A true
>   3D tomato **mesh** stays deferred to its own later task (**G32-3D-MODEL**).
> - **A4** ✅ built — airburst/ride lifetime **10s → 20s** (`LIFETIME_TICKS = 400`); detonates in mid-air at 20s if
>   it hits nothing, killing a rider still aboard.
> - **B1** ✅ built — **fuse REMOVED entirely** (the `FUSE` data-tracker + client flash/swell/hiss path are deleted)
>   — instant blast on ANY contact, no tell, no bail window. Crater restore default **15s → 5s**.
> - **B2** ✅ built — `onCollision` now calls `detonate()` **directly and never calls `super.onCollision`**, so the
>   vanilla discard no longer kills the blast before it runs (this same change covers B1). Blast stays power 6 /
>   nuke 20, no fire, and griefing ON. The current jar still lets armour mitigate entity damage; that result is now
>   rejected and B2 is reopened by the infinite-damage owner decision below.
> - **Naming** ✅ built — a thrown tomato now copies the anvil name onto the entity (+ visible), so a named tomato
>   hovers mid-air (B7) and a "nuke"-named one actually triggers the power-20 override (B8). This was never wired.
>
> **Knockback retune (2026-07-17 build pass):** `tomatoBlastKnockback` becomes a tunable **float = 8** (was a
> boolean) — punchier than vanilla power-6 knockback. `tomato.json` schema + `TomatoEntity` updated this pass.
>
> **TG32 retest build (2026-07-17):** the previous owner-locked fixes + the full client-visual slice were BUILT
> + jar'd; newer retest decisions below can require reversals or improvements. The built D5 underwater 3×3
> widen + clump self-heal is now rejected and marked for reversal. D8 no-sneak eat, D10 linear fall-damage, E4 custom ~90s villager-anger decay, E9b
> 30-line self-kill pool (`TomatoDamageSource`), E11 crop restore, and the visual slice (D2–D4 particle
> decals/drips/debris, E2 footprints, E6/E7 shield tint via `ShieldSauceTintMixin`, E8 screen-edge overlay) plus
> a chunky/wet sauce texture (D13). Those visuals exist, but their old appearance is rejected and the approved
> shared sauce redesign below is not built yet. The 3D tomato **mesh** (G32-3D-MODEL) remains deferred.
> **C3 owner decision, 2026-07-17:** tomato punch reflection/deflection is removed entirely; C1/C2 interception stays.
> **D7 owner decision, 2026-07-17:** rain-washing is removed entirely; rain has no special effect on sauce.
> **ROOT SAFETY owner decision, 2026-07-17:** no tomato feature may cause server lag, freezes, kicks, timeouts,
> or crashes. Required restoration is saved and completed safely; optional visual work is reduced or skipped first.
>
> See TG32.
> **Source:** Entirely new feature requested for CustomBlocks-B.

## 🔴 TG32 retest — owner-locked 2026-07-17

Discussion-only session, deep-searched against the actual mod code. The owner-locked decisions below are the **spec**
for this fix pass.

> **✅ BUILT 2026-07-17 (fix pass) — awaiting owner retest.** Every open row below is implemented; nothing is marked
> passed until the owner tests it. What landed:
> - **B2** — real explosion unchanged; a Fabric `AFTER_DAMAGE` hook finishes off any non-exempt survivor with a
>   bypassing lethal blow. The 30 `tomato_N` damage types are tagged `bypasses_armor` + `bypasses_effects`
>   (`data/minecraft/tags/damage_type/…`, merged with vanilla) so armour/resistance/protection can't save them;
>   a stubborn boss falls through to `kill()`. Shield-blockers (AFTER_DAMAGE `blocked`), tamed pets, and
>   creative/spectator/invulnerable are exempt. Radius/terrain/knockback/out-of-range untouched.
> - **Root safety + D1 + E11** — `TomatoCraterManager` rewritten around a per-world `TomatoCraterState`
>   (`PersistentState`): craters persist across restart, restore in a hard **128-block/tick** global batch, never
>   force-load a chunk (a block in an unloaded chunk waits), isolate a throwing position, order **supports first /
>   plants last** (crops carry their snapshot growth stage), and clear sauce or a blast-caused **flowing** fluid on
>   the spot while leaving any player build / source fluid / container in place. First-snapshot-wins retained.
> - **D5** — underwater path lays NO sauce; only a hard-capped red-dust + bubble burst. Clump placement + the
>   10-tick self-heal loop are deleted.
> - **D7 / C3** — rain-wash removed from `SauceManager`; punch deflect/coin-flip/grace removed from `TomatoEntity`
>   (C1/C2 interception intact).
> - **D8** — slurp gate removed: each empty-hand click fills hunger (`add(20,1.0)`) + heals to max
>   (`setHealth(getMaxHealth())`, clamped) and removes one layer; works at full hunger, no sneak.
> - **D11 / D13** — per-blast sauce is bounded (≤64 placements, radius ≤6); one-shot downhill spread then stops;
>   50/chunk FIFO cap retained.
> - **Shared sauce visuals** — new 64x crushed-tomato textures (block + splat/drip/debris); D3 streaks pool ~30s;
>   D2 ~82% face coverage; E2 randomized left/right prints; E6/E8 screen splatter = fresh random ~90% scattered
>   layout for 5s (retimed from 10s, owner-locked 2026-07-18). Shield tint kept sauce-red (true per-fragment patches still a pure-visual TODO).
>
> Statuses + retest steps live in `Group_32_Testing_Guide.md`.

### 🟩🟥 Owner retest results — 2026-07-18

Deep-searched against the actual mod code and reconciled with the owner's in-game retest.

- **Confirmed PASS:** B2 (guaranteed one-shot through armour), D5 (underwater burst-only), D8 (full-hunger no-sneak
  slurp + full heal), D11 (51+ puddles / overlapping restores, no crash), E11 (farm crops restore at growth stage).
  Still passed from 2026-07-17: C1, C2, D9, D12, E1, E5, E10.
- **Confirmed FAIL (reopened):**
  - **D1** — multiple tomatoes, **especially nukes**, leave permanent holes; not everything restores. Owner wants a
    new blast over an old crater to re-own and fully revert the whole area. Root: first-snapshot-wins drops positions
    a later blast re-destroys, and the 128-block/tick budget vs a nuke `snapRadius≈31` restores huge craters slowly/
    incompletely (`TomatoCraterManager.recordCrater`/`tick`/`restoreOne`).
  - **D3** — wall drip is a flat `SAUCE_DRIP` particle, not a face-attached gravity streak; no bottom puddle, no 30s
    decay. Regression, looks bad (`SauceVisuals.spawnSurfaceDecals`).
  - **D13** — ground sauce doesn't look like real sauce and spreads like a static snow layer (`SauceManager`/`SauceBlock`).
  - **E6** — **shielding an incoming tomato does not protect; the blocker is killed.** The guaranteed-kill only exempts
    `AFTER_DAMAGE blocked==true`, but an explosion isn't reliably shield-blocked, so `guaranteeKill` finishes the
    blocker anyway (`TomatoCombat.java:108,142`). Needs an explicit "actively blocking + facing the blast" exemption.
  - **E8** — screen sauce is flat axis-aligned cells on a 28×16 grid (`SauceScreenOverlay`); doesn't run to the bottom
    then decay, "not like the mockup." Needs a real painted sauce texture driven by a drip-then-fade animation.
- **D2 un-runnable:** the blast destroys the very fences/stairs/walls whose sauce cling D2 checks, so the row cannot be
  observed as written — deferred until a non-destructive spawn path exists.
- **Nuke power corrected to 20.0:** `NUKE_POWER = 20.0` is correct and confirmed in-game; the earlier "12.0" in this
  doc was a spec typo, now fixed throughout. Code was right from the start.

### 🔧 Close-out fix plan — owner-locked 2026-07-18 (PLAN ONLY, not yet built)

Every open row + polish + the mesh, fully locked for the build session that closes TG32:

- **Shared sauce material — recreate ORIGINAL, realistic SAUCE only.** Target vibe = the owner's reference (bright red
  glossy sauce dripping on white, irregular runs). That image is **premium stock, not CC0 → reference only, never
  shipped** (spec's own no-photo-theft rule). Ship an **original** hand-painted/procedural **64x** material.
  - **Look = a BLEND (owner-locked 2026-07-18):** bright glossy red runs on top over a **deeper crushed-tomato pulpy
    body** underneath — fine pulp, rare seeds, restrained wet highlights, translucent thin edges.
  - **It must read as realistic tomato SAUCE ONLY — no whole-tomato shapes, blobs, or fruit imagery** (owner-locked
    2026-07-18 — this is the fix for the old "tomatoes stuck to the screen" complaint).
  - Owner **previews + approves each texture** before it is jarred. One shared material feeds **D3, D13, D4, E2,
    E6/E7, E8**; D5 uses its colours only.
- **E8 / E6 screen overlay — repaint + retime.** Replace the 28×16 flat-cell grid (`SauceScreenOverlay`) with the
  approved sauce texture. Sauce **drips in from the TOP edge and gravity-runs downward, bit by bit, fully clearing by
  5 seconds** (owner-locked 2026-07-18 — **overrides the old 10s**). ~90% scattered coverage, readable through gaps.
  A **blocked shield hit (E6) throws the same screen splatter** as a direct hit (E8).
- **E6 shield protection — fix the kill, directional.** Blocking gives complete damage immunity **only when actively
  blocking AND facing the blast** (owner-locked 2026-07-18 — vanilla-style directional; a shield behind you does not
  save you). Root: the guaranteed-kill only exempts `AFTER_DAMAGE blocked==true`, but a tomato explosion is not
  reliably shield-blocked, so `guaranteeKill` still finishes the blocker. Add the explicit facing-block exemption.
  Shield still loses durability; the explosion still hits terrain/mobs/other players.
- **D1 crater restore — BOTH fixes (owner-locked 2026-07-18).**
  - (1) **Re-own overlaps:** a new blast landing in an existing crater's area takes over those positions and restarts
    one clean full revert of the whole overlapping area — **the area's restore timer RESETS to 5s from the newest
    blast** and the region reverts together (no permanent holes on overlap).
  - (2) **Bound the nuke, keep full power:** nuke stays **power 20** (crater size unchanged); only the snapshot radius +
    per-tick restore budget are spread so a huge crater **finishes** restoring — it may take longer, but never lags or
    leaves holes.
  - **Anti-dupe:** every blast over the area (first AND re-owning) has its dropped blocks cleared on restore.
- **D3 wall drip — rebuild** as face-attached shared-sauce streaks that gravity-run (slower than water, faster than
  lava), thin trail, small bottom puddle, decaying at 30s. **Clings to ALL faces — vertical walls, stairs/slabs, AND
  ceiling/overhang undersides** (owner-locked 2026-07-18). (30s timer unchanged.)
- **D13 ground sauce — rebuild** with the shared material; one bounded downhill spread then fully stops; 5s decay.
- **D4 / E2 / E7 polish — recolour only** to the shared material; behavior already passed. E7 shield sauce clears with
  crater restore (5s).
- **G32-3D-MODEL — NOW REQUIRED to close TG32 (owner-locked 2026-07-18, no longer just deferred).** A realistic round
  3D tomato mesh (green calyx, flat-unlit albedo, same in inventory/hand/flight). **Source: search a CC0 low-poly
  tomato, verify the licence permits redistribution to all clients, owner approves before use** (procedural/math-sphere
  stays rejected). This is the one close-out item that is art-blocked.
- **D2 — DROPPED (owner-locked 2026-07-18).** Row removed (the blast destroys the fences/stairs it needs). Audit
  `SauceVisuals` for any **D2-only** face-hugging fence/stair decal code and **delete it**; keep the D3 wall-drip path.
- **Explicitly OUT of TG32 close scope:** the config **GUI** (belongs to Group 27) and **Arabic** localisation (all 60
  death lines stay English until text stops churning).

| # | 🏷️ | Root Cause | Fix |
| --- | --- | --- | --- |
| **B2** | 🔧 reopened damage requirement | The real explosion now fires, but armour/resistance can still let hit entities survive, so the tomato does not one-shot everything it damages. | **Owner-locked 2026-07-17:** every normally damageable entity inside the existing blast damage area is guaranteed to die instantly, regardless of armour or resistance, including the Wither, Ender Dragon, and modded bosses. Blocking shields protect only their blocker and do not cancel the explosion; tamed pets keep full protection. Blast radius, terrain power, creative/spectator behavior, and out-of-range zero damage remain unchanged. |
| **C3** | ❗ removed | Owner removed tomato punch reflection/deflection entirely after the retest. | Remove the punch coin-flip, deflect action, and grace-window code in the next build; C1/C2 interception remains unchanged. |
| **D1** | 🔧 regression | The restore list exists only in memory, so a server crash/restart loses it permanently. Flowing water or sauce can also enter a crater spot and block the original block from returning. | **Owner-locked 2026-07-17:** save every pending crater and its progress to the world and resume after restart. Restoration begins at 5 seconds; a large restore returns in small hard-bounded batches targeting roughly 1–2 seconds total, but may take longer rather than lag the server. Do not force-load unloaded chunks: the crater may wait there and resumes safely when the chunk naturally loads again. Remove sauce/flowing water blocking an original block. Anything deliberately placed by a player stays exactly where placed, including containers/block data; every other destroyed block restores normally. |
| **D2** | 🔧 failed + approved redesign | The current floating splat pictures do not conform to fences, stairs, or walls; the sauce reads like a snow layer sitting beside them. | Use thin face-hugging sauce that follows posts, rails, stair treads/risers/sides, and wall tops/sides. Target roughly **75–85%** irregular coverage, at most **1/16 block** thick. Generate a fresh stable pattern per blast instead of using one fixed layout. |
| **D3** | 🔧 failed + approved redesign | The current wall droplets do not produce convincing face-attached dripping. | **Owner-locked 2026-07-17:** use the approved sauce material for randomized wall-hugging streaks and gravity-pulled drips. They run slower than water but faster than lava, leave thinner trails behind, and form a small puddle at the bottom. The D3 wall trails and their bottom puddle all fully decay after **30 seconds**; this does not change any other sauce timer. |
| **D4** | ✅ passed + visual follow-up | Debris movement passed, but every visible tomato/sauce element now needs the shared approved appearance. | Keep the passed fall/settle behavior unchanged; update only its colour/material to match the approved sauce target and randomize its visible variation. |
| **D5** | 🔧 regression / reversal | Surface sauce clumps glitch, fight water updates, and contribute to severe server lag/crashes. | **Owner-locked 2026-07-17:** remove underwater sauce blocks and the self-heal loop entirely. Underwater/seabed blasts still crater and restore, but show only a small hard-capped burst of red particles + bubbles using the approved sauce colours; it dissolves underwater and never reaches the surface. Excess visual work is dropped immediately instead of queued; tomato visuals must never be allowed to stall or crash the server. |
| **D7** | ❗ removed | The rain test cannot prove whether rain or the matching 5-second cleanup timer removed the sauce. Owner does not want this mechanic retained. | **Owner-locked 2026-07-17:** remove rain-washing entirely. Sauce disappears only through its normal cleanup/restoration rules; rain has no special effect. |
| **D8** | 🎨 design change | Owner wants plain right-click to slurp sauce even when hunger is already full, and wants one slurp to be extremely powerful. | **Owner-locked 2026-07-17:** plain empty-hand right-click always slurps and removes exactly one layer, with no sneak requirement and no full-hunger block. One slurp fills the entire hunger bar and heals the player to their normal maximum health; it never overheals. |
| **D10** | 🎨 design change | Owner wants fall-damage cushioning to scale with puddle depth, not a hard `≥5 layers = full negation` cutoff. | Replace the cutoff with a **linear scale by layer/8** — each layer absorbs proportionally more fall damage (1 layer ≈ 12% less, 8 layers = full negation). |
| **D13** | 🔧 failed + approved redesign | The current sauce texture still does not look like real tomato sauce, and ground spreading reads as a stationary snow-like layer. | **Owner-locked 2026-07-17:** use the shared approved material below. Each ground puddle makes one small, hard-bounded randomized spread into thin irregular patches, strongly favoring downhill, then stops updating completely. The spread sauce keeps the existing **5-second** decay time. No continuous spreading or ongoing work that can lag the server. |
| **E2** | ✅ passed + visual follow-up | The footprint trail works, but its old plain-red splat appearance needs the approved redesign. | Keep the passed alternating trail/fade behavior; render recognizable, differently shaped left/right prints with randomized smears using the shared sauce material. |
| **E6/E7** | E7 ✅ / E6 not verified + approved redesign | E6 damage was not focused in the retest yet, and shields currently receive only a plain red tint. E7 also needs its removal timing aligned with crater restoration. | Blocking a tomato with a shield gives complete damage protection to the blocker only, but the shield still loses durability. It does not cancel the explosion, so terrain, mobs, and other nearby players are still hit normally. The shield gets actual randomized shared-sauce patches, banner patterns remain visible, and the shield sauce clears with crater restoration. E6 also splatters the approved fresh random ~90% sauce across the screen, dripping in bit by bit and fully clearing by 5s (owner-locked 2026-07-18, was 10s). |
| **E8** | ✅ passed + approved redesign | The current screen effect is an edge-only fixed pattern of flat red shapes. | A point-blank direct hit covers about **90% of the screen** with a fresh random arrangement of splashes, smears, holes, and gravity drips. It is scattered rather than a fixed border, remains partly readable through gaps, drips in bit by bit and fully clears by 5s (owner-locked 2026-07-18, was 10s). |
| **E3** | ✅ basic case passed / superseded by B2 | Ordinary hostile mobs already usually die before the stun window can apply. The new B2 decision now requires the one-shot result for every non-exempt damageable entity in range. | Keep E3's tested basic zombie/skeleton result recorded as passed, but remove the expectation of rare survivors: B2's pending guaranteed-kill rework supersedes the stun-survivor path. |
| **E4** | 🔧 regression | Villager trade-price anger "doesn't decay" — it's using **vanilla** gossip decay, which runs on a real-time/day-cycle pace far too slow to observe in a test session. | Replace with a **custom decay**: track the gossip bump per-villager-per-thrower ourselves, linearly decaying it to zero over **~90 seconds**. **Re-hitting the same villager before decay finishes refreshes the timer back to a full 90s** (owner-locked 2026-07-17) — no stacking, one shared timer per villager+thrower pair. |
| **E9** | 🔧 regression | Self-kills always show the generic uncredited line. Vanilla suppresses the `.player` death-message variant whenever attacker == victim, falling back to the plain "was splattered by an explosive tomato" — this is vanilla-inherent behaviour, not a mod bug, but there's no self-kill content to fall back on gracefully. | Add **30 brand-new self-kill-specific lang lines** (`.self`-style keys, distinct text from the existing 30 — self-deprecating tone, e.g. "X blew themselves up with their own tomato" / "X forgot to let go of their own tomato" / "X became a casualty of their own sauce" / "X couldn't outrun their own explosive fruit" — approved direction, owner-locked 2026-07-17), picked uniformly at random. **Covers the ride-airburst self-kill too** (dying to your own tomato while riding it at the 20s mark) — same pool, no separate "went down with the ride" line needed, since the rider is always the owner already. |
| **E11** | 🔧 failed restore + design reversal | The farm does not return completely: crops, tilled soil, or water can be missing/wrong after restoration, and restoration must never crash the server. **Reverses the original E11 owner-lock further down this doc.** | **Owner-locked 2026-07-17:** restore every recorded farm block normally, beginning at 5 seconds. Restore terrain/tilled soil and water first, then every crop at its exact recorded growth stage, even if a player harvested/replanted/changed farm spots during the wait. Large restores use small hard-bounded batches targeting roughly 1–2 seconds total, but safety wins over that target. Save progress. Anything deliberately placed by a player stays exactly where placed, including containers/block data; every other restore spot returns normally. Isolate a bad position so it cannot stop the rest or lag/crash the server. |
| **D6** | 📝 wording only | Test row implied sauce-touches-lava; actual mechanic is the tomato itself detonating instantly on lava contact (per B1 — no fuse, any contact blasts). | Reword the row to "tomato explodes on lava contact," mark passed. No code change. |
| **D11** | ⛔ blocked safety/stress gate | The owner could not complete the test because tomato activity caused severe lag, kicks/timeouts, and a server crash. | Keep the strict 50-puddle-per-chunk FIFO cap, but D11 passes only if 51+ puddles and overlapping restores cause no freeze, kick, timeout, or crash. All per-tick work is hard-bounded; optional visuals are skipped before required restoration is endangered; no unlimited or continuously growing work queue. |
| **SCOPE** | 🎨 shared visual redesign | The sauce visuals were built separately and do not look like one material. | **Owner-approved 2026-07-17:** D2, D3, D4 colouring, D13 appearance, E2, E6/E7, and E8 all use the same sauce material below. D5 uses its colours only. Every blast/hit creates a new randomized pattern; patterns stay stable during their short lifetime instead of flickering or repeating a fixed layout. |
| **SHARED LOOK** | 🎨 owner-approved target | Flat red, ketchup, blood, plastic shine, salsa-sized chunks, and red-gravel noise are rejected. | Use a mostly smooth crushed-tomato purée base with fine pulp, rare tiny pieces/seeds, natural orange-red through rich/dark-red variation, restrained broad wet highlights, and slightly translucent thin edges. Keep the 64x detailed Minecraft-mod look. |

### Root safety contract — owner-locked 2026-07-17

The local client evidence records a timeout at 17:27:51, `Connection reset` at 17:32:32, and another timeout at
17:47:58. It proves the client lost the server connection; the hosting-panel/server log is still needed for the exact
server exception. Code review also found two strong mod-side risks: D5 repeatedly re-created removed underwater sauce
every 10 ticks for up to 60 seconds, and crater restoration attempted every due block in one server tick.

- Remove D5 underwater sauce placement and its self-heal loop entirely. No tomato effect may continuously replace,
  spread, or retry visual blocks.
- Put hard limits on work done by each blast, each chunk, and each server tick. Extra cosmetic particles, decals,
  drips, footprints, screen splashes, or spread attempts are skipped immediately, not saved into a growing queue.
- Required restoration is never dropped. Save the snapshot and progress to the world, restore in bounded batches,
  and resume unfinished work after restart.
- Restoration starts at the existing 5-second point. Large jobs target completion over roughly 1–2 seconds; under
  overload they may take longer instead of freezing the server.
- Do not force unloaded chunks to load for restoration. Keep their saved work; the crater may remain there while the
  chunk is unloaded, then resumes safely when that chunk naturally loads again.
- One broken block position is isolated and recorded; it cannot cancel the remaining restore. Blast-created sauce or
  flowing water may be cleared so the original block can return. Anything deliberately placed by a player stays exactly
  where placed, including containers/block data; every other restore spot returns normally.
- Any tomato-caused server freeze, kick, timeout, crash, lost restore, or permanent hole is an automatic TG32 failure.

### Owner-approved shared sauce target — 2026-07-17

World coating target (D2/D3/D13 and the shared material used by D4/E2/E6/E7):

![Approved sauce world target](assets/tomato/sauce_world_target_approved.png)

Point-blank screen target (E6 shield block and E8 direct hit; one example of a newly randomized arrangement):

![Approved 90-percent randomized screen sauce target](assets/tomato/sauce_screen_target_approved.png)

These are **visual targets, not running-game screenshots**. The material is shared, but placement is freshly randomized
per blast/hit and remains stable until it fades or is cleared.

**Already passed, 2026-07-17, no code changes:** C1 · C2 · D9 · D12 · E1 · E5 · E10

## What this is

A throwable Explosive Tomato combat item. It flies like a snowball, detonates like TNT **instantly on any contact**
(fuse removed, owner-locked 2026-07-15), and leaves behind a slippery "tomato sauce" mess. It is admin-spawned, and it is anti-grief: every block it destroys
comes back.

Two facts to keep in mind while reading, because the original request was written Bukkit-flavoured:

- This is a **Fabric 1.21.1 mod**, not a Bukkit plugin. WorldGuard/Towny do not exist here.
- The mod registers its own blocks and items, so **vanilla clients cannot join anyway** — every player has the mod,
  and client-side rendering is safe to rely on.

## Build order

Three phases. Test in-game and get confirmation before starting the next (CLAUDE.md §2/§4).

| Phase | Name     | What lands                                                                 |
| ----- | -------- | -------------------------------------------------------------------------- |
| **A** | **Fly**  | Entity, renderer, model, throw, riding, dispenser, `/cb tomato` command     |
| **B** | **Boom** | Instant contact detonation (no fuse), TNT explosion, nuke, crater restore, anti-dupe, blast-proofing |
| **D** | **Mess** | Sauce blocks, decals, drips, debris, slipperiness, eating                   |

Then C (interception) and E (combat effects). F and G are parked.

All new code lives in a new self-contained **`com.customblocks.tomato`** package (same shape as `buzzergame`).

## Phase A — Fly

- **Acquisition**: `/cb tomato <targets> [amount]` using vanilla entity selectors, plus a literal `/cb tomato all`.
  Amount defaults to 1. Gated on **vanilla OP level 2** — Fabric has no permission system, so the old
  `customblocks.tomato.give` node does not exist. Also grabbable from the **CB tools creative tab**.
- **Item**: Stacks to 64. Drops safely from broken chests without detonating. Craftable is **parked QoL**
  (eventual recipe: TNT + food).
- **Model — THIS PASS (owner-locked 2026-07-15, A1)**: **unified flat sprite.** Two fixes: (1) the flight billboard
  renders **upside-down** — the renderer's UV `v` coords are inverted (texture bottom maps to the top vertex); swap
  them so it reads right-way-up. (2) inventory/hand currently use an **ugly faceted-cube 16px JSON** — replace
  `models/item/explosive_tomato.json` with a **flat 2D `item/generated`** model pointing at the round **256px** entity
  sprite, so inventory + hand + flight all match. In-hand is a **flat billboard card** (owner accepted 2026-07-15). No
  mesh needed; consistent immediately.
  **Sprite art — OWNER-PROVIDED ASSET, LOCKED 2026-07-15:** the owner's hyper-real tomato PNG (glossy red body,
  water droplets, green calyx + stem; 3531×3451, transparent). Copied into the repo at
  **`docs/groups/assets/tomato/tomato_source.png`** (safe copy of `C:\Users\66664\Desktop\tomato_PNG12567.png`).
  Build session: **downscale to a 256px sprite** (keep transparency, trim/center) and **replace the current
  entity texture** `src/main/resources/assets/customblocks/textures/entity/explosive_tomato.png`, then point the
  flight billboard + `item/generated` inventory/hand model at it so all three surfaces unify. No procedural
  sprite — this asset is final for A1.
- **Model — LATER TASK (deferred 2026-07-15, bug G32-3D-MODEL)**: a **realistic round 3D tomato mesh** with a green
  calyx, rendered the same in inventory/hand/flight (like a shield/spawn-egg). **Adapt a redistributable-CC0 mesh**
  (simplify to game-weight) or, if none is found clean, **hand-model a low-poly tomato** — procedural/math-sphere
  generation is **rejected**. The CC0 licence MUST permit redistribution (the pack ships to every joining player);
  verify before shipping. Needs a custom Fabric 1.21.1 model/renderer — confirm the approach via web search first
  (CLAUDE.md MANDATORY_WEB_SEARCH). This is a separate art-blocked upgrade, NOT required for A1 to pass this pass.
- **Texture (for the later mesh)**: a **flat unlit photoreal albedo** baked onto the model's UVs, **reusing the
  `tomato_gen.py` skin/freckle/calyx colour look** — NOT its current output (which bakes lighting into a 2D sphere; a
  mesh texture must be flat albedo because the game lights the mesh). Real tomato reference for colour, never a copied
  photo. `tomato_gen.py` stays useful as the colour source / 2D-icon tool. 4K stays rejected — detail past ~256 is
  thrown away at the render size.
- **Flight**: Snowball-like arc and gravity. No particle trail (stealthy). Instant throw, no cooldown, spam allowed.
  Inherits player momentum for sprint-jump trick shots. Consumes the stack; free in creative.
- **Airburst**: If it hits nothing, it detonates after **20 seconds** (owner-locked 2026-07-15, was 10s) in mid-air
  rather than flying into unloaded chunks.
- **Riding** *(revised 2026-07-15 — the old "full kamikaze, no dismount" is REVERSED)*: Sneak + throw mounts you.
  **Sneak again and you drop off**, while the tomato flies on without you — vanilla's universal "get off the
  thing" key, same as a horse or boat. Dismount was wired but **regressed on MP** (bugs G32-RIDE-EJECT +
  G32-RIDE-DESYNC): the ride ejects you unprompted after ~1s, and the hotbar stays stuck reading "riding".
  **Dismount is free and unconditional (owner-locked 2026-07-15)** — plain vanilla sneak, no penalty, no
  "fuse already lit so you're stuck" case. If you can sneak, you get off. Target: hold the ride until a 2nd sneak.
  A rider who rides into the blast **without bailing DIES (owner-locked 2026-07-15)** — the blast is lethal. **With
  the fuse removed (B1) there is NO bail window**: contact = instant blast, so the only way to survive is to **sneak
  off before contact**. A rider **still aboard at the 20s airburst also DIES (owner-locked 2026-07-15)** — no
  auto-eject; the mid-air detonation kills them. The ridden tomato gets a speed/distance boost so the ride is a ride.
- **Dispensers**: Fire it at high velocity like a cannon. Dispenser kills credit **nobody** — generic death message,
  no thrower is tracked.

## Phase B — Boom

> 🔒 **Owner-locked blast (2026-07-15) — read before building Phase B.**
> **Power 6.0 normal · `nuke` = 20.0 · knockback ON · no fire · guaranteed instant kill for every non-exempt
> damageable entity inside the existing damage area · griefing ON
> with a 5s crater restore · NO FUSE (instant on contact).** Config-tunable. The short-lived "radius 3 / ~8 dmg /
> survivable / destruction-off" retune is **dropped** — a point-blank thrown tomato now kills.
>
> ⚠️ **No-damage regression root cause (B2, owner-confirmed 2026-07-15 — fails in SP and MP).** `TomatoEntity.onCollision`
> calls `super.onCollision(hitResult)` first; vanilla `ThrownItemEntity.onCollision` **discards the entity** (and sends
> the item-break status byte) before the fuse can tick → `detonate()` never runs on impact → zero damage, no crater
> anywhere. Only the airburst path (no collision, no discard) ever exploded. **Fix: remove the fuse and call
> `detonate()` directly inside `onCollision` BEFORE discard** — this single change fixes B1 (instant contact blast)
> and B2 (no damage) together.
>
> ⚠️ **Blast engine is a REAL vanilla explosion (owner-locked 2026-07-15).** If any hand-rolled `power 0` +
> custom-falloff path remains, drop it: use a real `createExplosion` at **power 6** (`nuke` → 20) with
> **`ExplosionSourceType` set to destroy** — one power drives crater + damage + knockback like TNT. Hook the
> explosion's **affected-blocks list** to feed the restore snapshot + skip blast-proof blocks. The old decoupled
> `tomatoBlastRadius` / `tomatoBlastDamage` keys go away.
>
> Config keys (single-power schema): `tomatoBlastPower` (6.0, `nuke` → 20.0), **`tomatoBlastKnockback` = `8`
> (owner-locked 2026-07-15 — numeric strength, punchier than vanilla power-6; change the key from a boolean to a
> tunable float)**, `tomatoBlastFire` (false), `tomatoRestoreSeconds` (**5**, was 15). **Magic names: `NUKE`
> only (owner-locked 2026-07-15)** — no other named tiers. **Crops/farmland destruction stays Phase E (row E11),
> NOT this pass (owner-locked 2026-07-15).** Still to build this pass: fuse removal + the
> onCollision-before-discard fix, the SPLAT audio, and the griefing/restore layer.

- **Fuse — REMOVED (owner-locked 2026-07-15, B1)**: there is **no fuse at all** — no ~0.5s tell, no TNT hiss, no
  white flash/swell, no bail window. The tomato **detonates instantly on ANY contact** (block, entity, ground), and
  the airburst detonates instantly at 20s. This reverses the earlier "~0.5s tell" decision outright. **Full removal
  (owner-locked 2026-07-15):** delete the `FUSE` data-tracker field **and** the client flash/swell/hiss renderer path
  — no dormant fuse code left behind. (The SPLAT blast audio below is separate and stays.)
- **Power**: **6.0** normal, `nuke` → **20.0** (case-insensitive exact name match). Terrain destruction like TNT
  at that power; crater restores after **5s** (owner-locked 2026-07-15, was 15s).
- **Damage**: **Owner-locked 2026-07-17, superseding the old armour-mitigates rule:** every normally damageable
  entity inside the existing blast damage area dies instantly regardless of armour or resistance, including the
  Wither, Ender Dragon, and modded bosses. Blocking shields
  protect only the blocker and do not cancel the explosion; tamed pets remain fully protected. This does not enlarge the blast or damage anything outside its existing
  range; creative/spectator behavior remains vanilla. Friendly fire stays ON, so an unshielded thrower can die too.
- **Audio**: a CC0 squish SPLAT layered over the vanilla TNT bass boom. The 3 in-repo splat `.ogg`s
  (`tomato_squishsplat_impact`, `tomato_squish_03`, `tomato_squishpop`) are a variation pool — **one is picked at
  random per blast** (owner-locked 2026-07-15). All three live at `assets/customblocks/sounds/`.
- **Custom names**: A name given in an anvil hovers over the tomato in flight, always visible like a named mob.
- **Fire/lava**: Explodes normally, does not burn up.

### Anti-griefing

- Blocks are destroyed like TNT and restoration **begins after 5 seconds** (owner-locked 2026-07-15, was 15s;
  config-tunable). Small jobs may complete immediately; large jobs use hard-bounded batches targeting roughly 1–2
  seconds total. Safety wins over that target, so an overloaded server takes longer instead of freezing.
- Pending snapshots and progress are saved with the world and resume after a crash/restart. Required restore work is
  never discarded. Unloaded chunks are not forced open; the crater may wait there and resumes when the chunk naturally
  loads again. One failed position cannot stop the remaining restore.
- **Anti-dupe**: blocks dropped by the blast are tracked and **deleted from the player's inventory** when the crater
  restores, not merely despawned on the ground. Otherwise every throw is free blocks.
- **Overlapping craters: the FIRST snapshot wins.** A later blast must never record blocks that an active snapshot
  already owns — otherwise the restore writes a crater back as if it were original terrain and leaves permanent
  holes. This is the single most likely correctness bug in this group.
- **Player-placed things survive after restore**: restoration may clear sauce or flowing water caused by the blast.
  Anything deliberately placed by a player stays exactly where placed, including containers/block data; every other
  restore spot returns normally.
- **Blast-proof**: all CB blocks (SlotBlock, guess showcase, buzzer, deleted markers) AND all containers
  (chests/barrels/shulkers) never break. Nothing spills, so there is no inventory-snapshot dupe surface.
  They may end up floating over a crater — accepted.
- **Entities** (item frames, paintings, boats, armor stands, mobs) are destroyed like vanilla TNT and are
  **not** restored. The anti-grief promise covers blocks only.
- Entities trapped in the restoring crater are safely pushed upward.
- Claim protection is **dropped** (WorldGuard/Towny are Bukkit-only). Respecting an installed Fabric protection mod
  is parked QoL.

## Phase D — Mess

Sauce is a **hybrid**, because a block cannot occupy the same space as a fence or stair:

- **Flat ground → a real 8-layer Block** (snow-style). All the gameplay lives here: slipperiness, eating, piston
  pushing, fall-damage negation. It is a normal registry entry, **not** one of the 1028 slots. After placement it
  makes one small hard-bounded randomized spread into thin patches, strongly preferring downhill, then stops all
  spreading work; every resulting ground patch keeps the normal 5-second decay time.
- **Every other surface** (walls, ceilings, fences, stairs, slabs) **→ a thin face-hugging visual coating** that
  follows the real shape instead of sitting beside it as a snow-like layer. Target 75–85% irregular coverage and
  at most 1/16-block thickness. Each blast gets a new stable random pattern. Never spawn hundreds of server entities.

Details:

- **Texture**: 64x detailed Minecraft-mod look, using the owner-approved shared material: mostly smooth crushed
  tomato purée, fine pulp, rare tiny pieces/seeds, natural red variation, restrained wet highlights, and slightly
  translucent thin edges. No ketchup, blood, paint, plastic shine, salsa chunks, or red-gravel noise.
- **Slipperiness**: slipperier than ice (ice is 0.98; sauce ~0.995).
- **Drying**: sauce fades and vanishes after **5 seconds** (owner-locked 2026-07-17, was 15s — now matches
  `tomatoRestoreSeconds` exactly, config-tunable). **Rain-washing is removed entirely; weather has no special effect.**
- **Drips**: randomized face-attached sauce streaks flow downward, slower than water and faster than lava, leave
  thinner trails behind, and form a small puddle at the bottom. The D3 trails + their bottom puddle fully decay after
  **30 seconds**; this timer applies only to D3 and does not change any other sauce lifetime.
- **Debris**: physics tomato chunks keep their passed movement and use matching sauce colours/material variation.
- **Fluids**: underwater/seabed blasts damage terrain normally and show only a small hard-capped burst of red particles + bubbles that dissolves underwater; no sauce blocks or surface clumps. Lava burns sauce off
  instantly with steam.
- **Eating**: plain empty-hand right-click always slurps exactly one puddle layer, even at full hunger; sneak is not
  required. One slurp fills the entire hunger bar and heals the player to their normal maximum health, without
  overhealing.
- **Fall damage**: scales **linearly with layer depth** (owner-locked 2026-07-17, was a hard `≥5 layers` cutoff) —
  each layer absorbs proportionally more (1 layer ≈ 12% less, 8 layers = full negation).
- **Cap**: 50 puddles per chunk (config-tunable); oldest evicts first. Per-blast and per-tick work also has hard
  limits. Optional sauce visuals are skipped rather than queued when those limits are reached.
- **No interop**: sauce and craters are fully independent of every other CB system. CB tools ignore sauce entirely.

## Phase C — Interception

- **Reflection removed (owner-locked 2026-07-17):** punching a mid-air tomato no longer has a special action.
- **Interception**: hit by an arrow or snowball → it explodes immediately like flak. Two tomatoes colliding mid-air
  both explode.

## Phase E — Combat & Entities

- **Slipping**: all entities slide wildly on sauce. Walking out leaves alternating, recognizable left/right prints
  with randomized smears in the shared sauce material; the trail lasts ~5s and fades.
- **Hostile mobs**: guaranteed instant death when caught inside the blast damage area. The old rare-survivor stun
  path is superseded by B2's infinite-damage decision.
- **Villagers**: if their homes are hit they flee in terror, show angry clouds, and raise trade prices — via vanilla
  gossip, so the anger decays naturally rather than being permanent.
- **Tamed pets**: wolves, cats, and parrots take zero explosion damage, but still get sauced and slip.
- **Crops**: farm restoration begins with the crater at 5s. Terrain/tilled soil and water restore first, then every
  crop returns at its exact recorded growth stage, even if a player harvested/replanted/changed farm spots during the
  wait. Large jobs use the root safety batches instead of one heavy server tick. Anything deliberately placed by a
  player stays exactly where placed, including containers/block data; every other restore spot returns normally. One
  bad position cannot stop the rest or lag/crash the server.
- **Shields**: blocking negates all tomato blast damage for the blocker only; the player takes no damage at all, but
  the shield still loses durability. The tomato still explodes normally and can damage terrain, mobs, and other nearby
  players. A blocked point-blank tomato still splatters randomized patches of the shared sauce material across the
  shield while sauced. Banner patterns remain visible underneath. Shield sauce clears with crater restoration.
- **Screen overlay**: a point-blank direct hit (E8) or blocked point-blank hit (E6) covers about **90% of the
  screen** with a new random scattered pattern of splashes, translucent smears, clear holes, and gravity drips. The
  screen sauce drips in bit by bit and fully clears by 5 seconds (owner-locked 2026-07-18, was 10s). It is not a fixed edge border.
- **Death messages**: 30 custom messages, **uniformly random** per death.

### The 30 death messages

1. `<Player> was turned into ketchup by <Thrower>`
2. `<Player> got sauced by <Thrower>`
3. `<Player> was marinara-ed by <Thrower>`
4. `<Player> slipped into the sauce dimension thanks to <Thrower>`
5. `<Player> experienced a high-velocity tomato from <Thrower>`
6. `<Player> was squashed by <Thrower>'s explosive tomato`
7. `<Player> got lost in the sauce, courtesy of <Thrower>`
8. `<Player> became spaghetti topping for <Thrower>`
9. `<Player> took a tomato to the face from <Thrower>`
10. `<Player> was pureed by <Thrower>`
11. `<Player>'s armor couldn't handle <Thrower>'s acidic tomato`
12. `<Player> got completely splattered by <Thrower>`
13. `<Player> was obliterated by <Thrower>'s flying fruit`
14. `<Player> tried to catch <Thrower>'s explosive tomato`
15. `<Player> is now soup, cooked by <Thrower>`
16. `<Player> was violently seasoned by <Thrower>`
17. `<Player> couldn't outrun <Thrower>'s tomato barrage`
18. `<Player> was reduced to red paste by <Thrower>`
19. `<Player> got absolutely juiced by <Thrower>`
20. `<Player> took <Thrower>'s food fight too seriously`
21. `<Player> was blasted into salsa by <Thrower>`
22. `<Player> caught <Thrower>'s explosive produce`
23. `<Player> slipped on <Thrower>'s deadly sauce`
24. `<Player> was sent to the produce aisle by <Thrower>`
25. `<Player> couldn't handle the spice of <Thrower>'s tomato`
26. `<Player> was crushed by <Thrower>'s volatile vegetable`
27. `<Player> got thoroughly drenched by <Thrower>'s tomato`
28. `<Player> became a casualty of <Thrower>'s sauce explosion`
29. `<Player> was blown away by <Thrower>'s red menace`
30. `<Player> got deleted by <Thrower>'s high-speed tomato`

## Configuration

All values live in **`config/customblocks/tomato.json`** — its own file, so the shared CB config schema is untouched.
Config is **server-wide, admin-only, and synced to clients**; it is never a client-local preference (explosion power
must not disagree between client and server).

The admin config **GUI is owned by Group 27** — it folds into the `/cb config` chest→Screen migration (see
`GROUP_27_SCREENS.md` / `Group_27_Testing_Guide.md`), not built as a standalone tomato screen here. This group
only defines the `tomato.json` values themselves.

## Mixins

Owner has pre-approved these targets. Exact target method + injection point must still be stated and approved
before any are written (CLAUDE.md "Mixin Checkmark"):

- Shield renderer — red tint while sauced.
- HUD render — the sauce screen overlay (try reusing the existing `HudRenderMixin` before adding a new one).
- Footprints rendering.

Slipperiness needs **no** mixin — it comes free from `Block.slipperiness`.

## Parked QoL (§G) — do not build until unparked

- Anvil enchants: Infinity (no consumption) and Multishot (throws 3).
- Stats & leaderboards: "Tomatoes Thrown", "Most Players Splattered".
- Respecting an installed Fabric claim/protection mod.
- A crafting recipe (TNT + food), which would end its command/creative-only status.

## Localisation

**English only** for now. The Arabic pass (including all 30 death messages) is deferred until the text stops churning.
