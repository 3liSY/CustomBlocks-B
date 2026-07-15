# Cloud Vault — Worker setup (Group 20)

The mod's `/cb vault`, `/cb category share`, and `/cb lore share` talk to a small Cloudflare
**Worker** (not a Pages site). Deploy it once, paste its URL into the mod config, done.
The worker code is [`worker.js`](worker.js) in this folder.

> ⚠️ Minimal test worker: anyone with the URL can upload/download. Keep the URL private until
> the S4 hardening (signing + rate limit) lands. Shared categories/notes (≤5 MB) live in KV and are a
> permanent library. **Backups** are large (40–150+ MB) so they go to **R2** instead — see the R2
> section below — and self-expire after 3 months via a bucket lifecycle rule.

## Deploy (Cloudflare dashboard — no install)

1. **Create the Worker.** dash.cloudflare.com → **Workers & Pages** → **Create** → **Create Worker**.
   Name it `cb-cloud-vault` → **Deploy** (the placeholder one).
2. **Paste the code.** On the worker page → **Edit code** → select-all, delete, paste all of
   `worker.js` → **Deploy**.
3. **Create the KV store.** Left nav → **Storage & Databases** → **KV** → **Create namespace** →
   name it `cb-vault-kv` → **Add**.
4. **Bind KV to the worker.** Worker → **Settings** → **Bindings** (a.k.a. Variables) →
   **Add binding** → **KV namespace** → **Variable name** = `VAULT` (exact, uppercase) →
   choose `cb-vault-kv` → **Deploy**.
5. **Get the URL.** The worker overview shows it, like
   `https://cb-cloud-vault.<your-subdomain>.workers.dev`.
   Open it in a browser — you should see **`CustomBlocks vault OK`**. (If it says
   "no VAULT KV binding", redo step 4.)

## Backups → R2 (large files, direct upload)

Backups are 40–150+ MB — past KV's 25 MB cap **and** past the worker's own 100 MB request-body
limit. So the bytes never go through the worker: the worker hands the mod a one-time signed link
and the mod uploads **straight to R2** (single object up to 5 GB). Do these once.

1. **Turn on R2.** dash.cloudflare.com → **R2** → **Enable**. (Free under 10 GB; Cloudflare may ask
   for a billing card to enable it even though you stay at $0 — that's expected.)
2. **Create the bucket.** **Create bucket** → name `cb-backups` → **Create**.
3. **Auto-delete after 3 months.** Bucket → **Settings** → **Object lifecycle rules** →
   **Add rule** → "delete objects" **90 days** after creation → apply to the whole bucket → **Save**.
   (This is your 3-month expiry — the worker no longer sets it.)
4. **Create an API token** so the worker can sign upload links. R2 overview →
   **Manage R2 API Tokens** → **Create API token** → permission **Object Read & Write** →
   scope to bucket `cb-backups` → **Create**. Copy the three values it shows **once**:
   - **Access Key ID**
   - **Secret Access Key**
   - your **Account ID** (also shown on the R2 overview page, top-right)
5. **Give the worker the four values.** Worker `cb-cloud-vault` → **Settings** →
   **Variables and Secrets** → add each (uppercase, exact):
   | Name | Type | Value |
   |---|---|---|
   | `R2_ACCOUNT_ID` | Text | your account id |
   | `R2_BUCKET` | Text | `cb-backups` |
   | `R2_ACCESS_KEY_ID` | Secret | the access key id |
   | `R2_SECRET_ACCESS_KEY` | Secret | the secret access key |
   Then **Deploy** the worker again so the vars take effect.
6. **Check it.** In game with cloud on: `/cb backup save t` → expect a "synced to cloud" line with a
   code. The file appears in R2 → `cb-backups` → `backups/<code>.zip`.

> Backup size is now limited only by R2 (5 GB per object), not by KV or the worker. No code change
> is needed when your backups grow.

## Point the mod at it

1. Edit `.minecraft\config\customblocks\config.json`:
   ```json
   "vaultEndpoint": "https://cb-cloud-vault.<your-subdomain>.workers.dev"
   ```
   (no trailing slash)
2. In game: `/cb reload`
3. Test: `/cb vault upload g20a` → should return a share code.

## Quick check it works (optional, from a terminal)

```sh
# health
curl https://cb-cloud-vault.<your-subdomain>.workers.dev          # -> CustomBlocks vault OK
# store something, get a code back
curl -X POST --data-binary "hello" https://.../category           # -> e.g. K7P3Q2
# fetch it back
curl https://.../category/K7P3Q2                                  # -> hello
```
