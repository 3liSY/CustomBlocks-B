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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class ImageDownloader {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Default per-request response timeout (seconds) for ordinary image links. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 20;
    /** Default max response size for image downloads. Larger files are usually source pages/HD photos. */
    private static final int DEFAULT_MAX_BYTES = 25 * 1024 * 1024;

    /** Browser-like UA — most CDNs that block non-browser clients serve images to this. */
    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** Minimal UA fallback. A few hosts (e.g. pngwing via Cloudflare) flag the full
     *  desktop-Chrome string as a scraper and 403 it, yet serve the same file fine to a
     *  plain client. We only use this on a 401/403 retry, never first. */
    private static final String PLAIN_UA = "Mozilla/5.0";

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
        return downloadOnce(url, DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_BYTES);
    }

    /**
     * Like {@link #download(String)} but with a custom per-request timeout, retrying ONCE on failure.
     * Used for AI/Pollinations links: they generate on the fly on the first hit (slow path) and the
     * server occasionally 5xx's that first hit, so one retry meaningfully improves success.
     */
    public static byte[] download(String url, int timeoutSeconds) throws Exception {
        try {
            return downloadOnce(url, timeoutSeconds, DEFAULT_MAX_BYTES);
        } catch (Exception first) {
            return downloadOnce(url, timeoutSeconds, DEFAULT_MAX_BYTES); // retry once; if this also fails, it propagates
        }
    }

    /** Download raw image bytes with a caller-provided hard response cap. */
    public static byte[] download(String url, int timeoutSeconds, int maxBytes) throws Exception {
        return downloadOnce(url, timeoutSeconds, maxBytes);
    }

    private static byte[] downloadOnce(String url, int timeoutSeconds, int maxBytes) throws Exception {
        if (url == null || url.isBlank()) throw new Exception("No URL given.");
        String trimmed = url.trim();
        if (!isHttpUrl(trimmed)) {
            throw new Exception("That's not a web image link. Use a direct http/https URL "
                    + "ending in .png/.jpg/.gif/.webp (right-click the image -> Copy Image Address). "
                    + "Inline 'data:' images aren't supported.");
        }
        // Per-site shortcut: rewrite a known page/share/wrapper link straight to its direct image
        // URL (Google Images, Drive, Dropbox, Twitter/X media, GitHub blob). null = leave as-is.
        String rewritten = LinkResolver.directImageUrl(trimmed);
        String target = (rewritten != null) ? rewritten : trimmed;

        Fetched first = fetch(target, timeoutSeconds, maxBytes);
        // A direct image → done. An HTML page → resolve its og:image and fetch that once.
        if (LinkResolver.looksLikeHtml(first.body(), first.contentType())) {
            String html = new String(first.body(), java.nio.charset.StandardCharsets.UTF_8);
            String media = LinkResolver.resolveImageUrl(html, target);
            if (media == null || !isHttpUrl(media)) {
                throw new Exception("That link is a web page, not an image. Open the image itself, "
                        + "right-click it -> Copy Image Address, and use that direct link.");
            }
            // The resolved image may itself be a known share form (e.g. a Twitter media URL) → normalize.
            String mediaDirect = LinkResolver.directImageUrl(media);
            if (mediaDirect != null) media = mediaDirect;
            Fetched second = fetch(media, timeoutSeconds, maxBytes);
            if (LinkResolver.looksLikeHtml(second.body(), second.contentType())) {
                throw new Exception("Couldn't find a direct image on that page. Right-click the image "
                        + "itself -> Copy Image Address and use that link.");
            }
            return second.body();
        }
        return first.body();
    }

    /**
     * Fetch one URL. Tries the browser UA first; on a 401/403 (some CDNs flag the spoofed
     * desktop-Chrome string as a bot, e.g. pngwing/Cloudflare) it retries ONCE with a plain
     * minimal UA, which those same hosts serve normally. Friendly throw on a real failure.
     */
    private static Fetched fetch(String url, int timeoutSeconds, int maxBytes) throws Exception {
        HttpResponse<InputStream> resp = send(url, timeoutSeconds, CHROME_UA);
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            closeQuietly(resp.body());
            HttpResponse<InputStream> alt = send(url, timeoutSeconds, PLAIN_UA);
            if (alt.statusCode() == 200) resp = alt; // plain UA got through; use it
            else closeQuietly(alt.body());
        }
        int code = resp.statusCode();
        if (code == 401 || code == 403) {
            closeQuietly(resp.body());
            throw new Exception("Access denied (" + code + "). Try a direct image link "
                    + "(right-click the image -> Copy Image Address).");
        }
        if (code != 200) {
            closeQuietly(resp.body());
            throw new Exception("Download failed: HTTP " + code + ".");
        }
        long declared = resp.headers().firstValueAsLong("content-length").orElse(-1);
        if (declared > maxBytes) {
            closeQuietly(resp.body());
            throw new Exception("That image is too large (" + humanSize(declared) + "). Use a smaller direct image.");
        }
        byte[] body = readLimited(resp.body(), maxBytes);
        if (body == null || body.length == 0) throw new Exception("The URL returned no data.");
        String ct = resp.headers().firstValue("content-type").orElse("").toLowerCase(java.util.Locale.ROOT);
        return new Fetched(body, ct);
    }

    /** One HTTP GET with the given User-Agent and our image-preferring Accept. */
    private static HttpResponse<InputStream> send(String url, int timeoutSeconds, String ua) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url.trim()))
                .header("User-Agent", ua)
                // No text/html and no image/avif here: text/html makes hosts like imgur serve their
                // WEBPAGE instead of the picture (the old "couldn't find a direct image" loop), and
                // asking for avif makes sites send avif we can't decode yet. The trailing */*;q=0.8
                // still lets a real page link return HTML so the LinkResolver page-reader can run.
                .header("Accept", "image/png,image/jpeg,image/gif,image/webp,*/*;q=0.8")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
    }

    private static byte[] readLimited(InputStream in, int maxBytes) throws Exception {
        try (InputStream src = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int total = 0;
            int n;
            while ((n = src.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes) {
                    throw new Exception("That image is too large (over " + humanSize(maxBytes)
                            + "). Use a smaller direct image.");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) return;
        try { in.close(); } catch (Exception ignored) {}
    }

    private static String humanSize(long bytes) {
        return (bytes / (1024 * 1024)) + " MB";
    }
}
