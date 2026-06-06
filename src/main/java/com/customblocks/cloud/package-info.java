/**
 * Cloud Vault sync client. CloudVaultClient uploads and downloads block data to a
 * Cloudflare Worker KV store via share codes. Opt-in via config
 * (cloudShareEnabled / cloudShareUrl). The Worker source is bundled in
 * cloud-vault-worker/.
 */
package com.customblocks.cloud;
