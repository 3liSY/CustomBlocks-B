# Group 21 — Config sync backend (`max_blocks` cross-client registry sync)

> **Screen content moved 2026-07-12** — the entire Settings Book spec (decisions D1–D13, layout, settings
> registry, full setting inventory, polish/power features, save/live-apply, build order, acceptance tests
> G21.1–G21.15) now lives in **`GROUP_27_SCREENS.md` §G27.27**. That's the current owner-locked design
> (2026-06-22) for what was `/cb config`'s Settings Book. This doc keeps only what's left: §9 below, the
> `max_blocks` cross-client registry-sync fix — networking/mixin backend, not screen content, unaffected by
> whatever medium the config UI itself ends up in.
>
> **Prerequisite:** Group 02 (Chest GUI) verified.
>
> **Status / test progress lives in** `Reports/GROUP_21_TESTING_GUIDE.md` §E (G21.16–G21.21).

---

## 9. Config sync across clients — `max_blocks` (LOCKED 2026-06-26)

> **Owner-locked approach: BOTH layers** — a **client self-heal net** (Phase A) **and** a **server
> handshake** (Phase B), built to cooperate (§9.6). D7 covers only the *local* "restart required"
> warning when *you* change the value; this section covers propagating it to **other clients** so they
> aren't kicked. Decision made with the owner 2026-06-26 (see D13).
>
> 🔁 **Revised 2026-06-27 (owner): Phase B retired — Phase A is the complete fix, not a net under B.**
> Tracing the join path proved a server handshake **cannot** beat this kick (registry frozen at
> launch + kick fires before any server payload reaches the client) and could only re-introduce the
> raw error if it loses the ordering race. Full reasoning in §9.7 Phase B. Phase A stands alone.

### 9.1 The problem (real incident — 2026-06-26)

`max_blocks` (config key `maxSlots`) sets the **registry size**. The owner raised it on the
server; a client still set to `1124` was kicked at join with the cryptic Fabric error:

```
Received 300 registry entries that are unknown to this client.
The following registry entry namespaces may be related: customblocks
(missing: customblocks:slot_1124, slot_1125, …)
```

The owner expected the client to "just sync." **It cannot** — see the hard constraints.

### 9.2 Hard constraints (verified in source — any fix must obey these)

| Fact | Where | Consequence |
|---|---|---|
| Blocks register in a flat loop `slot_0 … slot_{max-1}` | `SlotManager.registerAll(int max)` (`core/SlotManager.java:54`) | Registry size = `max_blocks`, nothing else (slots.json only paints metadata). |
| `registerAll` runs **once at mod init**, reading the **local** config | `CustomBlocksMod:224`, `CustomBlocksConfigStore:49` | The count is baked at client **launch**. |
| **No** payload carries `maxSlots` | grep: zero sync/packet refs | Today it never propagates at all. |
| Minecraft **freezes registries after init** | engine | You physically cannot add `slot_N` after launch → **≥ 1 client restart is unavoidable** when a client is too low. |
| The kick fires in the **config phase** (Fabric registry sync) | engine, **before** the PLAY-phase `JOIN` hook | The existing on-join config pushes (`CustomBlocksMod.java:279`) are **too late** — the fix must intercept the config phase, not `JOIN`. |
| Direction matters | engine registry-sync | **client ≥ server = OK** (extra client slots are harmless); only **client < server** kicks → self-heal only ever **raises** the client value, never lowers. |

### 9.3 Audit result — the desync surface is narrow (verified 2026-06-26)

Every `Registry.register` site was checked. **`max_blocks` is the only registry sized by data** —
so it is the only thing that can cause this kick:

| Surface | Count source | Kick risk |
|---|---|---|
| `SlotManager` `slot_0…slot_{max}` (block + item) | **`max_blocks`** | ⚠️ **this bug** |
| `ArabicLetterRegistry` | 1 fixed block | none |
| `AnimSlotRegistry` | 1 block-entity type | none |
| `RemovedBlock`, 3 item-group tabs, `ToolItems` | fixed in code | none |

Everything else is either **already live-synced** on join (silent-pack, transparent-bg, colour
hexes, Arabic labels, HUD — `CustomBlocksMod.java:279`) or **server-side only** (`textureSize` is
used server-side to generate the PNGs, then shipped via the pack; `httpHost`/`httpPort` are
local-machine and must **not** sync). No second hard-desync vector exists.

### 9.4 The fix — one shared healer, two callers (LOCKED)

The whole feature converges on **one** helper so the two layers can't fight:

- **`MaxSlotsHealer.ensureAtLeast(int needed)`** *(new file — `SlotManager` is 497/500, full)*
  - If local `max_blocks` ≥ `needed` → **no-op, silent** (idempotent).
  - Else: **atomic raise-only write** of `config.json` (reuse `CustomBlocksConfigStore` temp+rename
    + `clamp(1,8192)`), set a one-shot "restart pending" flag, and show the friendly screen (§9.6 text).
  - **Raise-only + idempotent** is what makes order-independence and no-double-prompt work.

- **Caller A — client self-heal mixin (Phase A).** Extend the config-phase interception (precedent:
  `mixin/ClientCommonNetworkHandlerMixin.java`) to catch the registry-remap failure, read the
  server's highest `customblocks:slot_N` from the sync data, and call `ensureAtLeast(N+1)`.
  **No server change** — heals the owner *and* every friend automatically, even against a server the
  client can't otherwise query.

- ~~**Caller B — server handshake (Phase B).**~~ **RETIRED 2026-06-27** — see §9.7. A server payload
  cannot fire before the config-phase registry kick, so this caller never beats it. Phase A's mixin
  is the sole path.

### 9.5 Locked decisions (was Q1–Q4)

| # | Decision | Locked choice |
|---|---|---|
| Q1 | Transport | **BOTH** — handshake (B) is primary/exact; self-heal mixin (A) is the fallback net. |
| Q2 | Self-heal | **Auto-write**, raise-only, atomic. No manual file editing. |
| Q3 | Safety default | **Keep the shipped default low** (no client bloat); rely on self-heal's one restart. |
| Q4 | Scope | **`max_blocks` first**; build `ConfigSyncPayload` general so future restart-class fields can ride it. |

### 9.6 How the two cooperate — the "work perfectly together" rule

- **Single healer → single write path.** Both callers funnel through `ensureAtLeast`; there is no
  second place that writes `max_blocks`, so no race, no double-write.
- **Idempotent + raise-only → order-free.** Whichever fires first heals; the other sees local ≥
  needed and **stays silent** → exactly one restart prompt (G21.21).
- **Handshake preferred, mixin is the net.** B fires earlier and carries the exact number, so the
  player sees the clean message; A still saves clients hitting an old/other server or a dropped
  handshake packet.
- **One screen, one wording**, both paths: *"This server uses {M} custom blocks; your game was set to
  {N}. We've updated your setting — fully restart Minecraft, then rejoin."*

### 9.7 Build order (phased; each compiles green — Bible §7)

**Phase A — client self-heal (MVP, zero server dependency)**
> ✅ CONFIRMED IN-GAME 2026-06-27 (jar 1.0.0) — `core/MaxSlotsHealer.java` + `mixin/RegistrySyncHealMixin.java` (`@Mixin(RegistrySyncManager.class, remap=false)` HEAD on `checkRemoteRemap`) + `customblocks.mixins.json`. CS-1/2/3 pass (owner + client log); CS-4 (client-higher) not yet run. **v2 fix:** the mixin compares the server slot count against the LIVE registry (`Registries.BLOCK`), not the mutable config — the first build leaked the raw kick on a same-session retry because the heal had already raised the in-memory config while the registry stayed frozen at launch. See TESTING_GUIDE §5 + PROGRESS_LOG 2026-06-27.
1. `MaxSlotsHealer` (new file): raise-only atomic write + restart-pending flag + helper to read it.
2. Client config-phase interception (mixin/hook) on the registry-remap failure → derive `needed` →
   `ensureAtLeast`.
3. Friendly disconnect/restart screen (replaces the raw "registry entries unknown").
   → **OWNER TEST A** (G21.16–G21.19).

**Phase B — server handshake — ❌ RETIRED 2026-06-27 (owner): not building. Phase A is the full fix.**
> Decided after tracing the actual join path against the source. A server handshake **cannot** beat
> this kick, for four concrete reasons:
> 1. **Seats are locked at launch.** The block registry is frozen at mod init (§9.2). No server
>    message can add `slot_N` to a running client → the client *still* needs one restart. Phase A
>    already gives that. Phase B saves zero restarts.
> 2. **The message arrives too late.** Every payload this mod sends goes out on the PLAY-phase `JOIN`
>    hook (`CustomBlocksMod.java:279`), *after* configuration. The kick fires *during* configuration
>    (§9.2). An under-provisioned client is disconnected before `JOIN` — so the locked
>    "`PayloadTypeRegistry.playS2C`" line could never reach the client it was meant to help.
> 3. **The only earlier slot is a race we can lose.** Sending in the configuration phase means
>    cutting in front of Fabric's own registry-sync task. If we lose that ordering race, the **raw
>    kick leaks anyway** → a net *regression* risk over Phase A.
> 4. **Nobody is left to help.** Only a client that already runs CustomBlocks-B can hit this mismatch
>    (the slots are *our* registrations) — and every such client already carries the Phase A mixin.
>    Phase B's only theoretical audience is "a CustomBlocks-B client on a Fabric-API build where the
>    mixin's target moved," which the fragile config-phase handshake wouldn't reliably serve either.
>
> Net: best case Phase B equals Phase A; worst case it re-introduces the bug. Not built.
> `ConfigSyncPayload` as a *general* config broadcaster (replacing the 4 separate `JOIN` payloads) is
> a possible **future, unrelated** cleanup — **not** a `max_blocks` kick fix. G21.20/G21.21 are void.

### 9.8 Reuse + file-size map (read before writing — §3 Research First)

- **New files:** `MaxSlotsHealer` (core), `ConfigSyncPayload` (network/payloads), and likely a
  client-side registry-sync-failure mixin (sibling of `ClientCommonNetworkHandlerMixin`).
- **Touch (have headroom):** `CustomBlocksMod` (401/500 — payload registration + config-phase send),
  `customblocks.mixins.json` (register the new client mixin).
- **DO NOT touch:** `SlotManager` (497/500 — full; all new logic goes in `MaxSlotsHealer`).
- **Reuse:** `CustomBlocksConfigStore` atomic save + `clamp(1,8192)`; the payload register/encode/
  decode pattern of the existing 22 payloads; the config-phase mixin pattern.
- **Acceptance:** **G21.16–G21.21** (§8).

---

> **Golden Rule:** nothing is ✅ done until the owner runs it in-game and confirms.
