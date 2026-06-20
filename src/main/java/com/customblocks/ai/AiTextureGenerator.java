/**
 * AiTextureGenerator.java — Group 15 (AI Texture Generation).
 *
 * Responsibility: turn a player's text prompt into a keyless Pollinations.ai image URL. NOT a network
 * class — it only builds the URL string; the existing {@link com.customblocks.image.ImageDownloader}
 * (via StudioTextureLoader on the client, and CreationStudioBridge on the server) actually fetches it.
 *
 * The prompt is auto-enhanced with ", seamless tileable block texture, <aiTextureStyle>" so the result
 * reads as a real block texture. The seed makes the image deterministic — the studio previews a small
 * (PREVIEW_SIZE) version, and "Create & Publish" re-fetches the SAME seed at CREATE_SIZE for full quality.
 *
 * Depends on: CustomBlocksConfig (aiTextureStyle).
 * Called by: StudioAiPanel (preview + create URLs).
 */
package com.customblocks.ai;

import com.customblocks.CustomBlocksConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AiTextureGenerator {

    private AiTextureGenerator() {} // static-only

    /** Live-preview fetch size (small = fast). The cube downsamples anyway, so this need not be large. */
    public static final int PREVIEW_SIZE = 256;

    /** Create re-fetch size. 512 ≥ any server textureSize (MAX_TEXTURE_SIZE), so the published block is
     *  full quality regardless of the server's configured size; the server then resizes to its own size. */
    public static final int CREATE_SIZE = 512;

    /** Per-request fetch timeout (seconds) for AI images. Pollinations generates the image on the FIRST
     *  hit for a given prompt+seed+size (a cache MISS), which routinely takes ~15–20s+. The normal 20s
     *  download timeout is too tight, so AI fetches (preview + create) use this longer one instead. */
    public static final int FETCH_TIMEOUT_SECONDS = 60;

    private static final String BASE = "https://image.pollinations.ai/prompt/";

    /** True if {@code url} is a Pollinations AI image link — so the fetch should use {@link #FETCH_TIMEOUT_SECONDS}. */
    public static boolean isAiUrl(String url) {
        return url != null && url.startsWith(BASE);
    }

    /**
     * Build the Pollinations URL for {@code prompt} at {@code sizePx}px square with {@code seed}.
     * Same prompt + seed → the same image, so the preview matches what Create re-fetches.
     */
    public static String buildUrl(String prompt, int sizePx, long seed) {
        String enhanced = (prompt == null ? "" : prompt.trim())
                + ", seamless tileable block texture, " + style();
        // Encode as a single path segment: URLEncoder uses '+' for spaces; a path wants %20.
        String enc = URLEncoder.encode(enhanced, StandardCharsets.UTF_8).replace("+", "%20");
        return BASE + enc + "?width=" + sizePx + "&height=" + sizePx + "&nologo=true&seed=" + seed;
    }

    /** The configured style suffix (default "pixel_art" when unset/blank). */
    private static String style() {
        String s = CustomBlocksConfig.aiTextureStyle;
        return (s == null || s.isBlank()) ? "pixel_art" : s.trim();
    }
}
