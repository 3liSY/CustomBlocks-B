/**
 * LinkResolver.java
 *
 * Responsibility: Turn a "share page" link (Tenor / Giphy / Imgur / Discord / almost any site)
 * into the DIRECT image URL by reading the page's OpenGraph / Twitter image meta tags. This is what
 * makes pasting the link you actually copied from a browser work — most of those links are HTML
 * pages, not images. Pure regex over the HTML (no parser dependency); resolves relative URLs against
 * the page URL; prefers a ".gif" candidate so an animation isn't downgraded to a static preview.
 *
 * Depends on: nothing (pure string work).
 * Called by:  ImageDownloader (after a fetch that turned out to be an HTML page).
 */
package com.customblocks.image;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkResolver {

    private LinkResolver() {} // static-only

    // <meta property="og:image" content="..."> / name="twitter:image" — attribute order varies, so
    // we capture the tag then pull property/name + content out of it independently.
    private static final Pattern META_TAG = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROP =
            Pattern.compile("(?:property|name)\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT =
            Pattern.compile("content\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_IMAGE_SRC = Pattern.compile(
            "<link\\b[^>]*rel\\s*=\\s*[\"']image_src[\"'][^>]*href\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);

    /** Image meta keys we trust, best first. */
    private static final List<String> IMAGE_KEYS = List.of(
            "og:image:secure_url", "og:image:url", "og:image", "twitter:image:src", "twitter:image");

    /** Quick check: does this look like an HTML page rather than raw image bytes? */
    public static boolean looksLikeHtml(byte[] body, String contentType) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/html")) return true;
        if (body == null || body.length == 0) return false;
        // Sniff the first non-whitespace bytes for an HTML opener.
        int i = 0;
        while (i < body.length && i < 64 && Character.isWhitespace(body[i])) i++;
        String head = new String(body, i, Math.min(body.length - i, 256), StandardCharsets.US_ASCII)
                .toLowerCase(Locale.ROOT);
        return head.startsWith("<!doctype html") || head.startsWith("<html")
                || head.startsWith("<head") || head.startsWith("<meta") || head.startsWith("<!--");
    }

    /**
     * Extract the best direct image URL from an HTML page, resolved absolute against {@code pageUrl}.
     * Returns null when the page advertises no image. Prefers a candidate ending in {@code .gif} so a
     * Tenor/Giphy page resolves to the animated file, not a static thumbnail.
     */
    public static String resolveImageUrl(String html, String pageUrl) {
        if (html == null || html.isBlank()) return null;
        List<String> candidates = new ArrayList<>();

        Matcher tags = META_TAG.matcher(html);
        // Collect by trusted-key priority: walk keys outer, tags inner, so order reflects IMAGE_KEYS.
        // (Simple two-pass: gather all (key,content) first, then pick.)
        List<String[]> metas = new ArrayList<>();
        while (tags.find()) {
            String tag = tags.group();
            Matcher p = PROP.matcher(tag);
            Matcher c = CONTENT.matcher(tag);
            if (p.find() && c.find()) {
                metas.add(new String[]{p.group(1).toLowerCase(Locale.ROOT), c.group(1)});
            }
        }
        for (String key : IMAGE_KEYS) {
            for (String[] m : metas) {
                if (key.equals(m[0])) candidates.add(m[1]);
            }
        }
        Matcher link = LINK_IMAGE_SRC.matcher(html);
        if (link.find()) candidates.add(link.group(1));

        if (candidates.isEmpty()) return null;

        // Prefer an animated .gif among the candidates; else take the first (highest-priority) one.
        String best = null;
        for (String c : candidates) {
            String abs = absolutize(decodeEntities(c), pageUrl);
            if (abs == null) continue;
            if (best == null) best = abs;
            if (stripQuery(abs).toLowerCase(Locale.ROOT).endsWith(".gif")) return abs;
        }
        return best;
    }

    private static String stripQuery(String url) {
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }

    /** Decode the handful of HTML entities that show up in meta content URLs. */
    private static String decodeEntities(String s) {
        return s.replace("&amp;", "&").replace("&#38;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'").trim();
    }

    /** Resolve {@code ref} (possibly relative or protocol-relative) against the page URL. */
    private static String absolutize(String ref, String pageUrl) {
        if (ref == null || ref.isBlank()) return null;
        try {
            if (ref.startsWith("//")) {
                String scheme = URI.create(pageUrl).getScheme();
                return (scheme == null ? "https" : scheme) + ":" + ref;
            }
            if (ref.startsWith("http://") || ref.startsWith("https://")) return ref;
            return URI.create(pageUrl).resolve(ref).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
