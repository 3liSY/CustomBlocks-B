/**
 * AiTextureGenerator.java
 *
 * Responsibility: Generate a texture PNG from a text prompt via an AI image API.
 * Opt-in via CustomBlocksConfig.aiTextureEnabled + aiApiKey.
 *
 * Phase 13 stub — returns null until the API integration is built.
 *
 * Depends on: CustomBlocksConfig
 * Called by: future /cb ai texture command handler
 */
package com.customblocks.ai;

import com.customblocks.CustomBlocksConfig;

public final class AiTextureGenerator {

    private AiTextureGenerator() {}

    /**
     * Generate a PNG texture from the given prompt.
     * Returns null if the feature is not configured or if generation fails.
     */
    public static byte[] generate(String prompt) {
        if (!isConfigured()) return null;
        // TODO Phase 13: call AI image generation API (DALL-E / Stable Diffusion)
        return null;
    }

    public static boolean isConfigured() {
        return CustomBlocksConfig.aiTextureEnabled && !CustomBlocksConfig.aiApiKey.isEmpty();
    }
}
