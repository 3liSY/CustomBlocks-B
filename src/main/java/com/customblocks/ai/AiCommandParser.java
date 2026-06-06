/**
 * AiCommandParser.java
 *
 * Responsibility: Parse natural-language input into /cb command sequences using an
 * external AI API. Configured via CustomBlocksConfig.aiApiKey.
 *
 * Phase 13 stub — returns null until an API key is set and the integration is built.
 *
 * Depends on: CustomBlocksConfig
 * Called by: future /cb ai command handler
 */
package com.customblocks.ai;

import com.customblocks.CustomBlocksConfig;

import java.util.List;

public final class AiCommandParser {

    private AiCommandParser() {}

    /**
     * Parse natural-language input and return a list of /cb commands to execute.
     * Returns null if the feature is not configured.
     */
    public static List<String> parse(String input) {
        if (CustomBlocksConfig.aiApiKey.isEmpty()) return null;
        // TODO Phase 13: call AI API with input, parse response into command list
        return null;
    }

    public static boolean isConfigured() {
        return !CustomBlocksConfig.aiApiKey.isEmpty();
    }
}
