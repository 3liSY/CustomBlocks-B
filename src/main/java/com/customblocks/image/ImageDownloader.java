/**
 * ImageDownloader.java
 *
 * Responsibility: Download image bytes from a URL using browser-like headers (so CDNs
 * that block non-browser requests still serve the image) with sensible timeouts.
 *
 * Phase 4: PNG/JPG/GIF via standard HTTP. WebP proxy + retries can be added later.
 *
 * Called by: ImageProcessor / the retexture command.
 */
package com.customblocks.image;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ImageDownloader {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ImageDownloader() {} // static-only

    /** True only for a fetchable http/https link (not data:, file:, blob:, etc.). */
    public static boolean isHttpUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(java.util.Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    /** Download raw image bytes. Throws with a user-friendly message on failure. */
    public static byte[] download(String url) throws Exception {
        if (url == null || url.isBlank()) throw new Exception("No URL given.");
        if (!isHttpUrl(url)) {
            throw new Exception("That's not a web image link. Use a direct http/https URL "
                    + "ending in .png/.jpg/.gif (right-click the image -> Copy Image Address). "
                    + "Inline 'data:' images aren't supported.");
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(url.trim()))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "image/avif,image/webp,image/png,image/*,*/*;q=0.8")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int code = resp.statusCode();
        if (code == 401 || code == 403) {
            throw new Exception("Access denied (" + code + "). Try a direct image link "
                    + "(right-click the image -> Copy Image Address).");
        }
        if (code != 200) throw new Exception("Download failed: HTTP " + code + ".");
        byte[] body = resp.body();
        if (body == null || body.length == 0) throw new Exception("The URL returned no data.");
        return body;
    }
}
