/**
 * OnboardingManager.java
 *
 * Responsibility: Detect first-time players and send a welcome message with
 * quick-start tips. The set of welcomed UUIDs is persisted to players.json
 * so the message fires exactly once per player.
 *
 * Call onPlayerJoin(player) from CustomBlocksMod on the JOIN event.
 *
 * Depends on: Gson, Chat, standard Java
 * Called by: CustomBlocksMod (ServerPlayConnectionEvents.JOIN)
 */
package com.customblocks.core;

import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public final class OnboardingManager {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR  = Path.of("config/customblocks");
    private static final Path FILE = DIR.resolve("players.json");
    private static final Set<String> WELCOMED = new HashSet<>();

    static { load(); }

    private OnboardingManager() {}

    /** Call on every player join. Sends welcome message once per UUID. */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        String uuid = player.getUuidAsString();
        synchronized (WELCOMED) {
            if (WELCOMED.contains(uuid)) return;
            WELCOMED.add(uuid);
            save();
        }
        // Defer so the message appears after the join announcement
        player.getServer().execute(() -> sendWelcome(player));
    }

    private static void sendWelcome(ServerPlayerEntity p) {
        p.sendMessage(Text.literal("\n§6§lWelcome to §eCustomBlocks§6§l!"), false);
        p.sendMessage(Text.literal("§7Turn any image URL into a working block:"), false);
        p.sendMessage(suggest("  §a/cb create §7<id> <name> <url>", "/cb create "), false);
        p.sendMessage(suggest("  §a/cb list", "/cb list"), false);
        p.sendMessage(suggest("  §a/cb give §7<id>", "/cb give "), false);
        p.sendMessage(Text.literal("§7Open the GUI with §a/cb gui§7, or type §a/cb help§7."), false);
    }

    private static MutableText suggest(String label, String cmd) {
        return Text.literal(label).styled(s -> s
                .withColor(Formatting.WHITE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Click to fill command"))));
    }

    private static synchronized void load() {
        try {
            if (!Files.exists(FILE)) return;
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("welcomed")) {
                for (var el : root.getAsJsonArray("welcomed")) WELCOMED.add(el.getAsString());
            }
        } catch (Exception e) {
            LOG.warn("[CustomBlocks] Could not load players.json: {}", e.getMessage());
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            WELCOMED.forEach(arr::add);
            root.add("welcomed", arr);
            Path tmp = DIR.resolve("players.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to save players.json", e);
        }
    }
}
