# Group 22 — Permissions System

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

## Test G24.1 — Non-OP player blocked from edit commands

As a player with no OP and no LuckPerms nodes:

```
/cb create g24a PermTest
```

**Expected:** `"You don't have permission to use this command."`

**Pass:** Command denied with clean message.
**Fail:** Command executes, error message is ugly/stacktrace, or wrong command blocked.

---

## Test G24.2 — Non-OP player can use view commands

As same non-OP player:

```
/cb list
/cb search PermTest
```

**Expected:** Both commands execute (list shows blocks, search shows results). These are "use" tier (OP level 0).

**Pass:** View commands work for non-OP players.
**Fail:** View commands also blocked.

---

## Test G24.3 — LuckPerms node grants permission

*(Skip if LuckPerms not installed.)*

Grant the permission `customblocks.command.create` to the test player via LuckPerms.

```
/lp user <testplayer> permission set customblocks.command.create true
```

As the test player:
```
/cb create g24a PermTest
```

**Expected:** Command succeeds.

**Pass:** Node grants access correctly.
**Fail:** Still denied despite node grant.

---

## Test G24.4 — Vanilla OP fallback (no LuckPerms)

*(Only if LuckPerms is not installed.)*

OP the test player at level 2: `/op <player>` or set OP level via server config.

```
/cb create g24a PermTest
```

**Expected:** Command succeeds (OP level 2 = "edit" tier by default).

**Pass:** Vanilla OP fallback works.
**Fail:** Denied despite OP level 2.

---

## Test G24.5 — Admin tier requires higher level

As OP level 2 player (edit tier only):

```
/cb backup save test
```

**Expected:** Denied — `"You don't have permission to use this command."` (Backup is admin tier, requires OP level 4.)

**Pass:** Admin commands require higher permission.
**Fail:** Admin commands accessible at edit tier.

---

## Test G24.6 — Config tier change takes effect

Via Config GUI (Group 21), Permissions tab: change `permissionTierEdit` from 2 to 0.

As a non-OP player:
```
/cb create g24b PermTest2
```

**Expected:** Command succeeds — edit tier now accessible to all.

Restore `permissionTierEdit = 2` after test.

**Pass:** Tier change applies correctly.
**Fail:** Still denied after tier lowered.

---

## Group 22 Verdict

| Test | Description | Result |
|---|---|---|
| G24.1 | Non-OP blocked from edit commands | ⬜ |
| G24.2 | Non-OP can use view commands | ⬜ |
| G24.3 | LuckPerms node grants access | ⬜ |
| G24.4 | Vanilla OP fallback works | ⬜ |
| G24.5 | Admin tier requires higher OP | ⬜ |
| G24.6 | Config tier change applies | ⬜ |

**Group 22 passes when permission nodes are enforced correctly with both LuckPerms and vanilla OP fallback.**

If anything shows ❌ — paste:
1. The player's OP level and LuckPerms nodes (if any)
2. The exact command tried
3. What happened vs what was expected

---

## Cleanup

```
/cb delete g24a
/cb delete g24b
```
