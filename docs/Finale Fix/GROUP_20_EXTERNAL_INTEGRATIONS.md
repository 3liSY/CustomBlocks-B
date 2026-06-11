# Group 20 — External Integrations: Cloud Vault & Discord

> **Prerequisite:** Group 02 (Chest GUI) verified. Group 21 (Config GUI) verified (so webhook and vault URL can be set in-game). Internet access available on server.
>
> **Objective:** Wire the deployed cloud vault (Cloudflare Worker) for block upload/download and cloud backups. Wire the Discord webhook for real-time event notifications. Both require internet access and are configured via the Config GUI.
>
> **Source issues:** Phase 14 (Cloud Vault + Discord stubs), Group H (cloud sharing backend), Decision §G (vault already deployed), Decision §N (Discord webhook blank by default)
>
> **Deployed cloud infrastructure:**
> - Worker: `cb-cloud-vault`
> - KV namespace ID: `ee49b7a0d7584b539b1c986bdf8428ee`
> - Endpoint: `cb-cloud-vault.cbbblocksvault.workers.dev`
> - Config keys: `cloudShareEnabled`, `cloudShareUrl`
>
> **Rules:** Work through each test in order. Stop and report failure before continuing. These tests require a working internet connection. Discord tests require a real webhook URL.

---

## What this group restores / adds

### Cloud Vault

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| Vault upload | `/cb vault upload <id>` (stub) | Not functional | Fully wired to deployed Worker |
| Vault download | `/cb vault download <code>` (stub) | Not functional | Fully wired |
| Cloud backup sync | Not present | Not present | `/cb backup save` optionally syncs to vault |
| Share code infrastructure | Not present | Not present | Used by Group 11 (sharecategory) and Group 12 (exportblock) |

### Discord

| Area | Old CB | New CB-B | This Group |
|---|---|---|---|
| Webhook notifications | Existed — fired on key events | `DiscordWebhook.java` stub | Fully wired to all events |
| `/cb discord test` | Sent test message | Stub | Restored |
| `/cb discord status` | Showed config | Stub | Restored |
| Default webhook | Shipped personal URL | N/A | **Blank by default** — never ships a hardcoded URL |
| Event toggles | Per-event toggles | Not wired | Restored |

---

## What this group covers

| Feature | Commands |
|---|---|
| Upload block | `/cb vault upload <id>` |
| Download block | `/cb vault download <code>` |
| Cloud backup | From `/cb backup save` |
| Discord test | `/cb discord test` |
| Discord status | `/cb discord status` |
| Config | `cloudShareEnabled`, `cloudShareUrl`, `discordWebhookUrl`, `discordNotify*` |

---

## Implementation Requirements

### 1. CloudVaultClient

Runs all HTTP on a daemon thread — never blocks the server thread.

**Upload:** `POST https://cb-cloud-vault.cbbblocksvault.workers.dev/upload`
- Body: JSON + base64-encoded texture PNG.
- Response: `{ "code": "XXXXXX" }` — 6–8 char share code.

**Download:** `GET .../download/<code>`
- Response: JSON + base64-encoded texture PNG.

### 2. Cloud Vault Config

| Field | Default | Description |
|---|---|---|
| `cloudShareEnabled` | true | Enable/disable vault features |
| `cloudShareUrl` | `cb-cloud-vault.cbbblocksvault.workers.dev` | Worker endpoint |
| `cloudPackSecret` | (empty) | Optional HMAC for private instances |

### 3. Conflict Resolution on Download

If downloaded block ID already exists locally: Import Conflict chest GUI opens — "Keep existing", "Overwrite", "Rename incoming".

### 4. Cloud Backup Sync

When `/cb backup save [name]` is run with `cloudShareEnabled = true`: backup is uploaded to vault asynchronously after local save. Message includes `(synced to cloud)` on success.

### 5. DiscordWebhook

HTTP POST to configured webhook URL. Runs async on daemon thread.

**Notification events** (all default `true` when a webhook is set):

| Event | Config field | Fires when |
|---|---|---|
| Block created | `discordNotifyCreate` | `/cb create` succeeds |
| Block deleted | `discordNotifyDelete` | `/cb delete` succeeds |
| Bulk operation | `discordNotifyBulk` | Any bulk command completes |
| Error | `discordNotifyError` | Incident-level error |
| Backup saved | `discordNotifyBackup` | `/cb backup save` completes |
| Server start | `discordNotifyStartup` | Mod initializes |

### 6. Discord Default Config

`discordWebhookUrl` is **blank by default**. No hardcoded or personal URL ever ships in source or config. Server admins configure their own.

### 7. `/cb discord test`

Sends: "CustomBlocks webhook test — connection successful." + server name, block count, uptime.

If no webhook configured: `"No Discord webhook set. Configure it in /cb config → Discord."`

### 8. `/cb discord status`

Shows in chat: configured yes/no, partial URL (first 30 chars + `...`), active event toggles, last notification timestamp.

### 9. No-Webhook Graceful Silence

When `discordWebhookUrl` is blank: all Discord code paths skip silently. Zero errors in `latest.log`.

---

## Setup — Cloud Vault

Ensure `cloudShareEnabled = true` and `cloudShareUrl = cb-cloud-vault.cbbblocksvault.workers.dev` in config (via Group 21 Config GUI).

```
/cb create g21a CloudTest https://i.imgur.com/example.png
/cb setglow g21a 6
```

## Setup — Discord

Set a Discord webhook URL via Config GUI → Discord tab, or directly in `config/customblocks/data/config.json`:
```json
"discordWebhookUrl": "https://discord.com/api/webhooks/YOUR_WEBHOOK_HERE"
```

---

## Test G21.1 — Upload block to vault

```
/cb vault upload g21a
```

**Expected:** `Block "g21a" uploaded — share code: XXXXXX`

Note the code.

**Pass:** Code returned in chat.
**Fail:** Error, timeout, or "vault not configured".

---

## Test G21.2 — Download by code

```
/cb delete g21a
/cb vault download XXXXXX
```

**Expected:** `Downloaded block "g21a" (code: XXXXXX).` Block restored with glow 6 and original texture.

**Pass:** Block restored with correct attributes.
**Fail:** Error, or block restored with wrong data.

---

## Test G21.3 — Conflict resolution on download

```
/cb create g21a AnotherBlock
/cb vault download XXXXXX
```

**Expected:** Import Conflict chest GUI — "Keep existing", "Overwrite", "Rename incoming".

**Pass:** Conflict GUI opens.
**Fail:** Silently overwrites.

---

## Test G21.4 — Cloud backup sync

```
/cb backup save cloud-test
```

**Expected:** `Backup "cloud-test" saved. (synced to cloud)`

**Pass:** Both local backup and cloud sync confirmed.
**Fail:** Local only, no cloud sync message.

---

## Test G21.5 — Cloud disabled gracefully

Set `cloudShareEnabled = false`. Reload.

```
/cb vault upload g21a
```

**Expected:** `"Cloud sharing is disabled. Enable it in /cb config."`

No crash. Restore `cloudShareEnabled = true`.

**Pass:** Graceful message.
**Fail:** Crash or null pointer.

---

## Test G21.6 — Discord test message

```
/cb discord test
```

**Expected:** `Discord test message sent.` AND a message appears in your Discord channel: "CustomBlocks webhook test — connection successful."

**Pass:** Message in Discord.
**Fail:** Error or no Discord message.

---

## Test G21.7 — Discord status

```
/cb discord status
```

**Expected:** Shows: configured yes, partial URL, active event list, last notification timestamp.

**Pass:** Correct status.
**Fail:** "not configured" when it is, or command missing.

---

## Test G21.8 — Discord block create notification

```
/cb create g21b DiscordTest https://i.imgur.com/example.png
```

**Expected:** Discord embed: "[CustomBlocks] Block Created — g21b / DiscordTest / Player: `<yourname>`"

**Pass:** Embed in Discord with correct fields.
**Fail:** No notification.

---

## Test G21.9 — Discord error notification

```
/cb create g21c ErrorTest
/cb retexture g21c https://broken-url.invalid/bad.png
```

**Expected:** Error embed in Discord: "[CustomBlocks] Error — retexture failed for g21c"

**Pass:** Error notification fires.
**Fail:** No Discord message for errors.

---

## Test G21.10 — No webhook = graceful silence

Set `discordWebhookUrl` to blank. Perform a block operation. Check `latest.log` — no Discord errors.

Restore webhook after test.

**Pass:** No errors when webhook is blank.
**Fail:** Errors in log.

---

## Test G21.11 — Default config has blank webhook

Check `config/customblocks/data/config.json` default or source code.

**Expected:** `discordWebhookUrl` is `""` or absent. No hardcoded URL anywhere.

**Pass:** Blank by default.
**Fail:** A real URL found in default config or source.

---

## Group 20 Verdict

| Test | Description | Result |
|---|---|---|
| G21.1 | Upload returns share code | ⬜ |
| G21.2 | Download by code restores block | ⬜ |
| G21.3 | Conflict GUI on duplicate ID | ⬜ |
| G21.4 | Backup save syncs to cloud | ⬜ |
| G21.5 | Cloud disabled shows graceful message | ⬜ |
| G21.6 | Discord test message reaches Discord | ⬜ |
| G21.7 | Discord status shows correct info | ⬜ |
| G21.8 | Block create notification fires | ⬜ |
| G21.9 | Error notification fires | ⬜ |
| G21.10 | No webhook = graceful silence | ⬜ |
| G21.11 | Default config has blank webhook | ⬜ |

**Group 20 passes when cloud vault and Discord both work correctly in-game.**

If anything shows ❌ — paste:
1. The exact command
2. Error or missing notification
3. Last 20 lines of `latest.log`
4. Network connectivity status

---

## Cleanup

```
/cb delete g21a
/cb delete g21b
/cb delete g21c
/cb backup delete cloud-test
```
