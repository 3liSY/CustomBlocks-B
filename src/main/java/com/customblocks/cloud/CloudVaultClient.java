/**
 * CloudVaultClient.java
 *
 * Responsibility: Upload/download block definitions to/from the Cloudflare Block Vault.
 * Configured via CustomBlocksConfig.vaultEndpoint.
 *
 * Phase 14 stub — methods return null/false until the Cloudflare Worker is set up
 * and vaultEndpoint is filled in config.json.
 *
 * Depends on: CustomBlocksConfig, SlotData
 * Called by: CloudCommands
 */
package com.customblocks.cloud;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.core.SlotData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class CloudVaultClient {

    private CloudVaultClient() {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** Upload a block definition. Returns a share code, or null on failure. */
    public static String upload(SlotData data, byte[] texture) {
        if (!isConfigured()) return null;
        // TODO Phase 14: POST to vaultEndpoint/upload with JSON + texture
        return null;
    }

    /** Download a block definition by share code. Returns the JSON string, or null. */
    public static String download(String shareCode) {
        if (!isConfigured()) return null;
        // TODO Phase 14: GET vaultEndpoint/block/<shareCode>
        return null;
    }

    // ── Group 11 — category sharing ─────────────────────────────────────────────
    //
    // ASSUMED worker contract (confirm against the deployed cb-cloud-vault Worker, then adjust
    // these two methods only):
    //   POST  <vaultEndpoint>/category?name=<category>   body = ZIP bytes (Content-Type: application/zip)
    //         → 2xx, response body = the share code as plain text.
    //   GET   <vaultEndpoint>/category/<code>            → 2xx, response body = the ZIP bytes.
    // Network calls block — callers MUST run these off the server thread.

    /** Upload a category ZIP. Returns the share code, or null on any failure. */
    public static String uploadCategory(String category, byte[] zip) {
        if (!isConfigured() || zip == null || zip.length == 0) return null;
        try {
            URI uri = URI.create(base() + "/category?name=" + enc(category));
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/zip")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(zip))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return null;
            String code = resp.body() == null ? "" : resp.body().trim();
            return code.isEmpty() ? null : code;
        } catch (Exception e) {
            return null;
        }
    }

    /** Download a category ZIP by share code. Returns the bytes, or null on any failure. */
    public static byte[] downloadCategory(String code) {
        if (!isConfigured() || code == null || code.isBlank()) return null;
        try {
            URI uri = URI.create(base() + "/category/" + enc(code.trim()));
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) return null;
            byte[] body = resp.body();
            return (body == null || body.length == 0) ? null : body;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isConfigured() {
        return !CustomBlocksConfig.vaultEndpoint.isEmpty();
    }

    private static String base() {
        return CustomBlocksConfig.vaultEndpoint.trim().replaceAll("/+$", "");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
