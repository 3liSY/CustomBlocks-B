/**
 * DiscordWebhook.java
 *
 * Responsibility: Post event notifications to a Discord webhook asynchronously.
 * Opt-in via CustomBlocksConfig.discordWebhookUrl — silently no-ops when not set.
 *
 * Depends on: CustomBlocksConfig, java.net.http
 * Called by: CloudCommands, optionally CreationCommands
 */
package com.customblocks.discord;

import com.customblocks.CustomBlocksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class DiscordWebhook {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Discord");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private DiscordWebhook() {}

    /** Post a plain-text message. No-op if the webhook URL is not configured. */
    public static void post(String message) {
        String url = CustomBlocksConfig.discordWebhookUrl;
        if (url.isEmpty()) return;
        String body = "{\"content\":\"" + escape(message) + "\"}";
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HTTP.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                LOG.warn("[CustomBlocks] Discord webhook failed: {}", e.getMessage());
            }
        });
    }

    public static boolean isConfigured() {
        return !CustomBlocksConfig.discordWebhookUrl.isEmpty();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
