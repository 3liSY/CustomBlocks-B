# Group 22 — Permissions System

> ## ⏸️ DEFERRED 2026-06-22 — come back later (owner's call)
>
> Not started. Spec was read, design questions were answered, then the owner chose to **park
> this group and resume later**. Everything below in this banner is **already decided** — when we
> resume, do NOT re-ask; pick up at "Resume here".
>
> **Decisions locked (owner-confirmed 2026-06-22):**
> 1. **Rollout = slice first, then roll out.** Build engine + config + denied message, wire a small
>    test slice (`create`=edit, `list`=use, `backup`=admin), owner confirms in-game, THEN wire all.
> 2. **Tier mapping = walk the full list together.** Proposed map below is owner-reviewable, not yet
>    confirmed. Owner stopped before ruling on the 8 judgment calls.
> 3. **Denied UX = clean message, command stays visible.** `"You don't have permission to use this
>    command."` via an **in-execute check** (NOT `.requires()`, which would hide the node and give
>    vanilla "Unknown command"). Commands stay tab-completable for all; they just refuse to run.
> 4. **Test env = vanilla OP only (no LuckPerms).** Verify the 3-tier OP fallback + config tier
>    changes. Skip Test G22.3 (LuckPerms node) until/unless LuckPerms is installed.
>
> **Build facts found:** `fabric-permissions-api` is present but **commented out** in `build.gradle`
> (~line 57: `me.lucko:fabric-permissions-api:0.3.1`) — uncomment to enable. Group 21 Config GUI
> already has a `Cat.PERMISSIONS` tab with a "coming with Group 22" placeholder
> (`ConfigRegistry.java` ~line 334) — the 3 tier fields slot in there. Handlers each build literals
> inline (`root.then(CommandManager.literal("x")...)`) across ~55 files — in-execute checks need a
> shared helper so we don't edit hundreds of `executes`.
>
> ### Resume here — proposed tier map (owner reviews, then we build)
> One node per top-level subcommand: `customblocks.command.<name>`. Fallback OP: use=0, edit=2, admin=4.
>
> - **🟢 USE (OP 0):** list · search · categories · help · welcome · give · favs · locked ·
>   shapelist · shapepreview · menu · dashboard · listgui · blockslist
> - **🟡 EDIT (OP 2):** create · rename · dupe · retexture · delete · reid · setglow · sethardness ·
>   setsound · setcollision · setcategory · setshape · clearshape · shapeeditor · paintface · setface ·
>   clearface · clearallfaces · animation · resize · exportpng · gradient · gradientpick · bgstudio ·
>   tolerance · coloring · colors · livecolor · eyedrop · customcolor · recolorvariants · omni · brush ·
>   chisel · rectangle · deleter · template · palette · category · gui · editor · edithud · facechangegui ·
>   magicitems · editmagicitems
> - **🔴 ADMIN (OP 4):** all bulk* (bulkgui bulkhub bulkproperty bulkdelete bulkrename bulkcategory
>   bulkduplicate bulkexport bulkreid bulklock bulkunlock bulkfavorite bulkunfavorite) · confirm · cancel ·
>   config · backup · backupgui · recover · safety · showbrokenblocks · trash · deletedblocks · diag ·
>   incidents · audit · cache · report · reload · rp · sync · unsuppress · admingui · lock · unlock ·
>   vault · discord
>
> **8 open judgment calls (owner had NOT ruled when deferred):**
> 1. lock/unlock → admin (guess) or edit?
> 2. fav → use (guess) or edit?
> 3. undo/redo → edit (guess) or use?
> 4. note/lore → edit (guess) or use?
> 5. ai → edit (guess) or admin?
> 6. export/exportblock/importblock/importfolder → edit (guess) or admin?
> 7. particles/sounds/feedback → admin (guess) or use?
> 8. arabic → edit (guess) or use?

---

> **Prerequisite:** Group 02 (Chest GUI) verified. LuckPerms (or Fabric Permissions API) installed on test server (or vanilla OP fallback test only).
>
> **Objective:** Restore the LuckPerms / Fabric Permissions API integration with per-command permission nodes. When LuckPerms is absent, fall back to vanilla OP level checks using a 3-tier scheme.
>
> **Source issues:** Q7 (Permissions System), Decision §F (LuckPerms + Fabric Permissions API, vanilla OP fallback)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing. Test both with and without LuckPerms if possible.

---

## What this group restores

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| LuckPerms integration | Existed with per-command nodes | Missing — all commands OP-only or unrestricted | Restored |
| Permission nodes | `customblocks.command.create` etc. | Not present | Fully wired per-command nodes |
| 3-tier fallback | Old had granular OP-level scheme | No fallback at all | 3-tier: use / edit / admin → vanilla OP levels |
| Config | `permissionFallback*` OP-level fields | Not present | 3-tier config in Permissions tab (Group 21) |

---

## What this group covers

| Feature | Area |
|---|---|
| Permission nodes | One node per `/cb` command |
| 3-tier fallback | use = OP 0, edit = OP 2, admin = OP 4 |
| LuckPerms auto-detect | If present, use it; if absent, use vanilla OP |
| Config | Permission tier settings via Group 21 Config GUI |

---

## Implementation Requirements

### 1. Permission Architecture

CustomBlocks uses the **Fabric Permissions API** (`me.lucko:fabric-permissions-api`) as the abstraction layer:
- If LuckPerms is installed: LuckPerms nodes are evaluated automatically through the API.
- If LuckPerms is absent: Fabric Permissions API falls back to vanilla OP level checks.

This means zero extra code paths — one check handles both cases.

### 2. Permission Nodes

All nodes follow the pattern `customblocks.command.<subcommand>`.

**Tier assignment:**

| Tier | Description | Default vanilla OP level | Example nodes |
|---|---|---|---|
| `use` | View, browse, give, search, favs | OP level 0 (all players) | `customblocks.command.list`, `.give`, `.search`, `.favs`, `.help` |
| `edit` | Create, delete, modify, import/export | OP level 2 | `customblocks.command.create`, `.delete`, `.setglow`, `.retexture`, `.template` |
| `admin` | Bulk ops, config, backup, diagnostics | OP level 4 | `customblocks.command.bulkdelete`, `.config`, `.backup`, `.diag`, `.lock` |

### 3. Fallback Scheme — 3 Tiers in Config

Config fields (in Permissions tab via Group 21):
| Field | Default | Description |
|---|---|---|
| `permissionTierUse` | 0 | Vanilla OP level for "use" tier commands |
| `permissionTierEdit` | 2 | Vanilla OP level for "edit" tier commands |
| `permissionTierAdmin` | 4 | Vanilla OP level for "admin" tier commands |

Server ops can lower these (e.g., set `permissionTierEdit = 0` to allow all players to create blocks).

### 4. Denied Command Message

When a player runs a command they don't have permission for:
`"You don't have permission to use this command."`

No stack trace. No exception. Clean message.

---

## Setup

*(With LuckPerms — skip to vanilla OP tests if LuckPerms not available.)*

Create a test player account (or use `/lp user` commands) with no extra permissions.

---

## Test G22.1 — Non-OP player blocked from edit commands

As a player with no OP and no LuckPerms nodes:

```
/cb create g22a PermTest
```

**Expected:** `"You don't have permission to use this command."`

**Pass:** Command denied with clean message.
**Fail:** Command executes, error message is ugly/stacktrace, or wrong command blocked.

---

## Test G22.2 — Non-OP player can use view commands

As same non-OP player:

```
/cb list
/cb search PermTest
```

**Expected:** Both commands execute (list shows blocks, search shows results). These are "use" tier (OP level 0).

**Pass:** View commands work for non-OP players.
**Fail:** View commands also blocked.

---

## Test G22.3 — LuckPerms node grants permission

*(Skip if LuckPerms not installed.)*

Grant the permission `customblocks.command.create` to the test player via LuckPerms.

```
/lp user <testplayer> permission set customblocks.command.create true
```

As the test player:
```
/cb create g22a PermTest
```

**Expected:** Command succeeds.

**Pass:** Node grants access correctly.
**Fail:** Still denied despite node grant.

---

## Test G22.4 — Vanilla OP fallback (no LuckPerms)

*(Only if LuckPerms is not installed.)*

OP the test player at level 2: `/op <player>` or set OP level via server config.

```
/cb create g22a PermTest
```

**Expected:** Command succeeds (OP level 2 = "edit" tier by default).

**Pass:** Vanilla OP fallback works.
**Fail:** Denied despite OP level 2.

---

## Test G22.5 — Admin tier requires higher level

As OP level 2 player (edit tier only):

```
/cb backup save test
```

**Expected:** Denied — `"You don't have permission to use this command."` (Backup is admin tier, requires OP level 4.)

**Pass:** Admin commands require higher permission.
**Fail:** Admin commands accessible at edit tier.

---

## Test G22.6 — Config tier change takes effect

Via Config GUI (Group 21), Permissions tab: change `permissionTierEdit` from 2 to 0.

As a non-OP player:
```
/cb create g22b PermTest2
```

**Expected:** Command succeeds — edit tier now accessible to all.

Restore `permissionTierEdit = 2` after test.

**Pass:** Tier change applies correctly.
**Fail:** Still denied after tier lowered.

---

## Group 22 Verdict

| Test | Description | Result |
|---|---|---|
| G22.1 | Non-OP blocked from edit commands | ⬜ |
| G22.2 | Non-OP can use view commands | ⬜ |
| G22.3 | LuckPerms node grants access | ⬜ |
| G22.4 | Vanilla OP fallback works | ⬜ |
| G22.5 | Admin tier requires higher OP | ⬜ |
| G22.6 | Config tier change applies | ⬜ |

**Group 22 passes when permission nodes are enforced correctly with both LuckPerms and vanilla OP fallback.**

If anything shows ❌ — paste:
1. The player's OP level and LuckPerms nodes (if any)
2. The exact command tried
3. What happened vs what was expected

---

## Migrated Commands (from Group 02 Extinction)

With the deprecation of Group 02, the following administrative commands have been fully transferred to Group 22's scope.
- **`/cb rp pause` & `/cb rp resume`**: Explicitly restricted to Server OPs (Admin Tier) only. 
- **`/cb sync`**: When pushing the resource pack to all connected clients, the server will display a 3-second on-screen title announcement warning players of the incoming lag spike before pushing the pack.

---

## Cleanup

```
/cb delete g22a
/cb delete g22b
```
