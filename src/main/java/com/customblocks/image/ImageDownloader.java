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

    /** Default per-request response timeout (seconds) for ordinary image links. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 20;

    private ImageDownloader() {} // static-only

    /** True only for a fetchable http/https link (not data:, file:, blob:, etc.). */
    public static boolean isHttpUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(java.util.Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    /** One fetched response: the raw bytes plus the server's Content-Type (lower-cased, may be ""). */
    private record Fetched(byte[] body, String contentType) {}

    /**
     * Download raw image bytes. If the link is actually an HTML page (e.g. a Tenor/Giphy/Imgur share
     * page — what you normally copy from a browser), this reads the page's OpenGraph image meta and
     * re-fetches the real image ONCE. Throws with a user-friendly message on failure.
     */
    public static byte[] download(String url) throws Exception {
        return downloadOnce(url, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Like {@link #download(String)} but with a custom per-request timeout, retrying ONCE on failure.
     * Used for AI/Pollinations links: they generate on the fly on the first hit (slow path) and the
     * server occasionally 5xx's that first hit, so one retry meaningfully improves success.
     */
    public static byte[] download(String url, int timeoutSeconds) throws Exception {
        try {
            return downloadOnce(url, timeoutSeconds);
        } catch (Exception first) {
            return downloadOnce(url, timeoutSeconds); // retry once; if this also fails, it propagates
        }
    }

    private static byte[] downloadOnce(String url, int timeoutSeconds) throws Exception {
        if (url == null || url.isBlank()) throw new Exception("No URL given.");
        if (!isHttpUrl(url)) {
            throw new Exception("That's not a web image link. Use a direct http/https URL "
                    + "ending in .png/.jpg/.gif/.webp (right-click the image -> Copy Image Address). "
                    + "Inline 'data:' images aren't supported.");
        }
        Fetched first = fetch(url, timeoutSeconds);
        // A direct image → done. An HTML page → resolve its og:image and fetch that once.
        if (LinkResolver.looksLikeHtml(first.body(), first.contentType())) {
            String html = new String(first.body(), java.nio.charset.StandardCharsets.UTF_8);
            String media = LinkResolver.resolveImageUrl(html, url.trim());
            if (media == null || !isHttpUrl(media)) {
                throw new Exception("That link is a web page, not an image. Open the image itself, "
                        + "right-click it -> Copy Image Address, and use that direct link.");
            }
            Fetched second = fetch(media, timeoutSeconds);
            if (LinkResolver.looksLikeHtml(second.body(), second.contentType())) {
                throw new Exception("Couldn't find a direct image on that page. Right-click the image "
                        + "itself -> Copy Image Address and use that link.");
            }
            return second.body();
        }
        return first.body();
    }

    /** Perform one HTTP GET with browser-like headers; throws a friendly message on a bad status. */
    private static Fetched fetch(String url, int timeoutSeconds) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url.trim()))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "image/avif,image/webp,image/png,image/*,text/html,*/*;q=0.8")
                .timeout(Duration.ofSeconds(timeoutSeconds))
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
        String ct = resp.headers().firstValue("content-type").orElse("").toLowerCase(java.util.Locale.ROOT);
        return new Fetched(body, ct);
    }
}
