# Group 21 — In-Game Config GUI

> **Prerequisite:** Group 02 (Chest GUI) verified.
>
> **Objective:** Build a complete in-game Config chest GUI that exposes all server settings without requiring manual `config.json` editing. Long text inputs (API keys, webhook URLs, IP overrides) use an Anvil GUI for safe pasting. All config fields are accessible in-game.
>
> **Source issues:** 17.23 (no in-game way to set vaultEndpoint, discordWebhookUrl, AI keys, etc.), P4 (config inputs via Anvil GUI)
>
> **Rules:** Work through each test in order. Stop and report failure before continuing.

---

## What this group adds

| Area | Old CustomBlocks | New CustomBlocks-B | This Group |
|---|---|---|---|
| Config GUI | `/cb config` opened a full settings GUI | `/cb config` reduced/no GUI | Full chest GUI with all fields |
| Text field input | Anvil GUI for long strings | Not present | Anvil GUI for: webhook URL, cloud endpoint, AI style, IP override |
| Config categories | Grouped settings by area | Flat list | Categorized: General, Textures, HUD, Tools, Network, Backup, AI, Discord, Advanced |
| Live reload | Not always applied | Not present | Config changes apply live (no restart needed where possible) |
| Config regression | Group N mentions this as a regression | Reduced config | Fully restored |

---

## What this group covers

| Feature | Commands / Area |
|---|---|
| Config GUI | `/cb config` |
| Category navigation | Chest GUI tabs |
| Text field input | Anvil GUI for string fields |
| Live apply | Changes take effect immediately |
| Config file path | `config/customblocks/data/config.json` |

---

## Implementation Requirements

### 1. Config Chest GUI Layout

`/cb config` opens a 6-row chest GUI with category tabs in the top row:

| Tab | Slot icon | Contains |
|---|---|---|
| General | Compass | `maxSlots`, `textureSize`, `silentPack`, `didYouMean`, `autoCategorizeEnabled` |
| HUD | Spyglass | `hudEnabled`, `hudX`, `hudY`, `hudScale`, `hudColor`, `hudBgOpacity` |
| Tools | Iron Sword | `toolTabSort`, `offhandHologramEnabled`, `hologramHeight`, `hologramColor` |
| Network | Redstone | `resourcePackPort`, `serverIpOverride`, `cloudShareEnabled`, `cloudShareUrl`, `cloudPackSecret` |
| Backup | Chest | `autoBackupInterval`, `autoBackupKeepCount`, `trashRetentionDays` |
| AI | Blaze Powder | `aiMaxVariations`, `aiTextureStyle`, `aiProviderOverride` |
| Discord | Paper | `discordWebhookUrl`, `discordNotify*` event toggles |
| Permissions | Shield | Permission tier settings (3-tier: use / edit / admin) |
| Advanced | Command Block | `bulkConfirmThreshold`, `maxUndoDepth`, `texturePayloadsPerTick`, etc. |

### 2. Config Slot Types

Each config field is one chest slot with:
- **Slot item**: visually represents the current value (e.g., glow level shown as glowstone).
- **Hover tooltip**: field name, current value, brief description.
- **Click behavior**:
  - Boolean → toggle immediately.
  - Number → opens anvil GUI with current value pre-filled. Player edits number, confirms.
  - String → opens anvil GUI for text input (URL, key, etc.).
  - Enum → cycles through allowed values on click.

### 3. Anvil GUI for Text Inputs

For long string fields (`discordWebhookUrl`, `cloudShareUrl`, `serverIpOverride`, `aiTextureStyle`):
1. Clicking the slot opens an Anvil GUI.
2. The current value is pre-filled in the rename field.
3. Player types or pastes the new value.
4. Player clicks the output slot (renamed item) to confirm.
5. Config field updated immediately.

### 4. Live Apply

Changes apply immediately to the running server where possible:
- `silentPack`, `hudEnabled`, `didYouMean` — live.
- `resourcePackPort` — requires restart (shows warning: "Restart required to change port").
- `maxSlots` — requires restart (shows warning).
- All other fields — live.

### 5. Config Save

Every change triggers an atomic write to `config/customblocks/data/config.json` (write-temp + rename).

### 6. Group 21 as Prerequisite

Group 20 (External Integrations) instructs setting the webhook and cloud vault URL via this Config GUI. Group 15 (AI) references `aiTextureStyle`. All depend on this group being complete.

---

## Setup

No blocks needed — this group tests the config system directly.

---

## Test G23.1 — Config GUI opens as chest GUI

```
/cb config
```

**Expected:** A chest GUI opens with category tabs in the top row: General, HUD, Tools, Network, Backup, AI, Discord, Permissions, Advanced.

**Pass:** Chest GUI opens with category tabs.
**Fail:** Text output only, screen-based UI, or missing categories.

---

## Test G23.2 — Navigate to a category

Click the "HUD" tab.

**Expected:** Content area shows HUD-related config slots: hudEnabled, hudScale, hudColor, etc. Each slot has a hover tooltip with field name and current value.

**Pass:** HUD slots visible with tooltips.
**Fail:** Same content regardless of tab, or no tooltips.

---

## Test G23.3 — Toggle boolean field

In the HUD tab, click the `hudEnabled` slot.

**Expected:** Slot visual changes (e.g., green → red). HUD is disabled in-game. Clicking again re-enables it. Change saves to config.json immediately.

**Pass:** Toggle works, applies live, saves to file.
**Fail:** Click does nothing, or file not updated.

---

## Test G23.4 — Edit number field via anvil

In the HUD tab, click the `hudScale` slot.

**Expected:** Anvil GUI opens with current scale value (e.g., "1.0") pre-filled. Change to "1.5" and confirm.

**Pass:** Anvil opens, value accepted, HUD scale updates live.
**Fail:** Anvil doesn't open, or value not saved.

---

## Test G23.5 — Set Discord webhook via GUI

Navigate to "Discord" tab. Click the `discordWebhookUrl` slot.

**Expected:** Anvil GUI opens. Paste or type a Discord webhook URL. Confirm.

**Pass:** Webhook URL saved to config. `/cb discord status` shows it as configured.
**Fail:** Anvil doesn't open, or URL not saved.

---

## Test G23.6 — Set cloud URL via GUI

Navigate to "Network" tab. Click the `cloudShareUrl` slot.

**Expected:** Anvil GUI opens with current URL pre-filled (`cb-cloud-vault.cbbblocksvault.workers.dev`). Confirming without change keeps it.

**Pass:** Anvil opens, current value shown, can be changed.
**Fail:** Anvil doesn't open.

---

## Test G23.7 — Config persists after restart

Change `hudScale` to 1.3 via the Config GUI. Restart the server.

Open `/cb config` → HUD tab.

**Expected:** `hudScale` slot shows 1.3 — the value persisted.

**Pass:** Config value persisted.
**Fail:** Config reset to default.

---

## Test G23.8 — Restart-required warning

Navigate to "Network" tab. Click `resourcePackPort`.

**Expected:** Anvil opens for input. After confirming a new value, chat shows: `"Port change requires a server restart to take effect."`

**Pass:** Warning shown.
**Fail:** No warning, or port change applied without restart message.

---

## Group 21 Verdict

| Test | Description | Result |
|---|---|---|
| G23.1 | Config GUI opens with category tabs | ⬜ |
| G23.2 | Category navigation shows correct fields | ⬜ |
| G23.3 | Boolean toggle works live | ⬜ |
| G23.4 | Number field via anvil | ⬜ |
| G23.5 | Discord webhook set via GUI | ⬜ |
| G23.6 | Cloud URL editable | ⬜ |
| G23.7 | Config persists after restart | ⬜ |
| G23.8 | Restart-required warning shown | ⬜ |

**Group 21 passes when all config fields are accessible in-game and changes persist correctly.**

If anything shows ❌ — paste:
1. The tab and field clicked
2. What appeared vs what was expected
3. Content of `config/customblocks/data/config.json` if the value didn't save
