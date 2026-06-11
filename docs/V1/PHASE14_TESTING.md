# Phase 14 Testing Tutorial — Cloud + Discord + Onboarding

> **Prerequisite:** Phase 13 ✅ confirmed. Creative world open, cheats ON.
>
> **Goal:** Verify onboarding welcome, vault/discord status commands, and Discord test
> (if a webhook is configured).

---

## What changed from old CustomBlocks → new

| Area | Old CustomBlocks | New CustomBlocks-B |
|---|---|---|
| Onboarding | Welcome message on first join | `OnboardingManager.java` — fires once per UUID, persists to `players.json` |
| Cloud vault | Share blocks via short codes | `CloudVaultClient.java` — **stub**. Commands show status but upload/download say "coming soon". Requires Cloudflare Worker setup. |
| Discord | Not in old CB | `DiscordWebhook.java` — real HTTP. No-ops if `discordWebhookUrl` not set in config. |
| WelcomeManager | — | Not built |
| FirstUseHints / TipPool | — | Not built |
| SampleBlocksLoader | — | Not built |

---

## What Phase 14 covers (built)

| Feature | How to test |
|---|---|
| First-join welcome message | Delete `config/customblocks/players.json`, relog |
| `/cb vault` — config status | Run without configuring anything |
| `/cb vault upload <id>` | Shows stub message |
| `/cb vault download <code>` | Shows stub message |
| `/cb discord` — config status | Run without configuring anything |
| `/cb discord test` | Requires `discordWebhookUrl` in config (optional) |

---

## Test 14.1 — First-join welcome message

Delete the players file so onboarding fires again:

```
/cb diag
```

Note your game directory, then close the game and delete:
`config/customblocks/players.json`

Relaunch and open the world.

**Expected:** On join, a welcome message appears:
```
Welcome to CustomBlocks!
Turn any image URL into a working block:
  /cb create <id> <name> <url>   [clickable]
  /cb list                        [clickable]
  /cb give <id>                   [clickable]
Open the GUI with /cb gui, or type /cb help.
```

**Pass:** Message appears exactly once on first join. Does NOT appear on subsequent relogs.
**Fail:** No message, or appears every time.

---

## Test 14.2 — Vault status (unconfigured)

```
/cb vault
```

**Expected:** `Block Vault is not configured. Set vaultEndpoint in config.json to enable.`

**Pass:** Message shown, no crash.
**Fail:** Error or crash.

---

## Test 14.3 — Vault upload stub

```
/cb create p14test VaultTest
/cb vault upload p14test
```

**Expected:** `Block Vault not configured — set vaultEndpoint in config.json.`

**Pass:** Command runs, returns config instructions.
**Fail:** Crash or unexpected behavior.

---

## Test 14.4 — Discord status (unconfigured)

```
/cb discord
```

**Expected:** `Discord not configured. Set discordWebhookUrl in config.json.`

**Pass:** Status message shown.
**Fail:** Error or crash.

---

## Test 14.5 — Discord test (optional — skip if no webhook URL)

> Only run this if you have a Discord webhook URL. Set it in
> `config/customblocks/config.json` under `discordWebhookUrl`.

```
/cb discord test
```

**Expected:** `Test message sent to Discord.` — and the message appears in the Discord channel.

**Pass:** Webhook fires and message appears in Discord.
**Fail:** No message in Discord, or error in chat.

---

## Phase 14 Verdict

| Test | Description | Result |
|---|---|---|
| 14.1 | First-join welcome fires once | ⏭ SKIPPED — deferred to Phase 17 |
| 14.2 | `/cb vault` shows unconfigured status | ⏭ SKIPPED — deferred to Phase 17 |
| 14.3 | Vault upload returns config instructions | ⏭ SKIPPED — deferred to Phase 17 |
| 14.4 | `/cb discord` shows unconfigured status | ⏭ SKIPPED — deferred to Phase 17 |
| 14.5 | Discord test sends webhook | ⛔ BLOCKED — needs Phase 17 `/cb config` GUI to set webhook URL in-game |

**Phase 14 — PARTIAL. Tests 14.1–14.4 testable now. Full vault + Discord blocked by missing config GUI.**

> **Blocked by Phase 17:**
> - Setting `vaultEndpoint` and `discordWebhookUrl` requires manually editing `config.json` — no in-game GUI exists
> - Phase 17 issue 17.23: `/cb config` needs a chest GUI that lets players set all config values in-game
> - Re-test full Phase 14 (vault upload/download, Discord live test) after Phase 17 config GUI is built
>
> **Missing from Phase 14 spec:**
> - `CloudVaultClient` upload/download — stubs, Cloudflare Worker not set up
> - `WelcomeManager`, `FirstUseHints`, `TipPool`, `SampleBlocksLoader` — not built

If anything shows ❌ — paste:
1. The exact command typed
2. What you expected vs what happened
3. Last 20 lines of `latest.log` at failure

---

## Cleanup after testing

```
/cb delete p14test
```
