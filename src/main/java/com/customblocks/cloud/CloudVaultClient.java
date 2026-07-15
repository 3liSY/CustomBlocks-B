/**
 * CloudVaultClient.java
 *
 * Responsibility: Upload/download categories and notes to/from the Cloudflare Block Vault.
 * Configured via CustomBlocksConfig.vaultEndpoint. Network calls block — callers run them off-thread.
 *
 * Depends on: CustomBlocksConfig
 * Called by: CloudCommands, CategoryCommands, NoteCommands, VaultConflict
 */
package com.customblocks.cloud;

import com.customblocks.CustomBlocksConfig;

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

    // ── Group 18 / S4 — note sharing ────────────────────────────────────────────
    //
    // ASSUMED worker contract (mirrors the category one; confirm against the deployed Worker):
    //   POST  <vaultEndpoint>/note            body = note JSON (Content-Type: application/json)
    //         → 2xx, response body = the share code as plain text.
    //   GET   <vaultEndpoint>/note/<code>     → 2xx, response body = the note JSON.
    // Network calls block — callers MUST run these off the server thread.

    /** Upload one note (JSON). Returns the share code, or null on any failure. */
    public static String uploadNote(String json) {
        if (!isConfigured() || json == null || json.isEmpty()) return null;
        try {
            URI uri = URI.create(base() + "/note");
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return null;
            String code = resp.body() == null ? "" : resp.body().trim();
            return code.isEmpty() ? null : code;
        } catch (Exception e) {
            return null;
        }
    }

    /** Download one note (JSON) by share code. Returns the JSON, or null on any failure. */
    public static String downloadNote(String code) {
        if (!isConfigured() || code == null || code.isBlank()) return null;
        try {
            URI uri = URI.create(base() + "/note/" + enc(code.trim()));
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return null;
            String body = resp.body();
            return (body == null || body.isBlank()) ? null : body;
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

    // ── Group 20 / §C — backup sync (two-step, direct to R2) ─────────────────────
    //
    // Backups are 40–150+ MB, well past KV's 25 MB cap AND past the worker's own
    // 100 MB request-body limit, so the bytes must NOT pass through the worker.
    // Worker contract (cb-cloud-vault):
    //   1. POST <vaultEndpoint>/backup?name=<name>
    //         → 2xx, body = JSON { "code": "<share code>", "url": "<presigned R2 PUT url>" }
    //   2. PUT  <url>   body = ZIP bytes        → 2xx (the upload lands straight in R2)
    // Network calls block — callers MUST run this off the server thread.

    /** Upload a backup ZIP straight to R2 via a worker-issued link. Returns the code, or null on failure. */
    public static String uploadBackup(String name, byte[] zip) {
        if (!isConfigured() || zip == null || zip.length == 0) return null;
        try {
            // Step 1 — ask the worker for a one-time upload URL + share code.
            URI initUri = URI.create(base() + "/backup?name=" + enc(name));
            HttpRequest initReq = HttpRequest.newBuilder(initUri)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> initResp = HTTP.send(initReq, HttpResponse.BodyHandlers.ofString());
            if (initResp.statusCode() / 100 != 2) return null;
            String body = initResp.body() == null ? "" : initResp.body();
            String code = jsonField(body, "code");
            String url  = jsonField(body, "url");
            if (code == null || code.isEmpty() || url == null || url.isEmpty()) return null;

            // Step 2 — upload the zip bytes directly to R2 (bypasses the worker size limit).
            HttpRequest putReq = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(10)) // 150 MB over a home connection is slow
                    .header("Content-Type", "application/zip")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(zip))
                    .build();
            HttpResponse<Void> putResp = HTTP.send(putReq, HttpResponse.BodyHandlers.discarding());
            if (putResp.statusCode() / 100 != 2) return null;

            return code;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pull one string field out of a flat JSON object without a JSON library.
     * Safe here because the worker's values (a 6-char code and a presigned URL)
     * never contain a double-quote, so first-quote..next-quote is the whole value.
     */
    private static String jsonField(String body, String field) {
        if (body == null) return null;
        String key = "\"" + field + "\"";
        int k = body.indexOf(key);
        if (k < 0) return null;
        int colon = body.indexOf(':', k + key.length());
        if (colon < 0) return null;
        int q1 = body.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = body.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return body.substring(q1 + 1, q2);
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
