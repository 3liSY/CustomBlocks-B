/**
 * FirstUseHints.java
 *
 * Responsibility: Fire a one-time contextual hint the first time a player uses a given command
 * (Group 23 §5 / G25.6-7). Each hint fires exactly once per player, ever — the fired-hint set is
 * persisted to config/customblocks/data/hints.json with an atomic write so it survives restarts.
 *
 * Depends on: Gson, Minecraft text API.
 * Called by:  (future wiring) the create / give / setglow command handlers, after success.
 */
package com.customblocks.core.onboarding;

import com.customblocks.command.Chat;

import com.customblocks.command.CbFmt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public final class FirstUseHints {

    private static final Logger LOG  = LoggerFactory.getLogger("CustomBlocks");
    private static final Gson   GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   DIR  = Path.of("config/customblocks/data");
    private static final Path   FILE = DIR.resolve("hints.json");

    /** "uuid|hintKey" entries that have already fired. Guarded by LOCK. */
    private static final Set<String> FIRED = new HashSet<>();
    private static final Object LOCK = new Object();

    static { load(); }

    private FirstUseHints() {}

    private static final String CREATE  = "create";
    private static final String GIVE    = "give";
    private static final String SETGLOW = "setglow";

    /** First /cb create — tell the player how to obtain the block they just made. */
    public static void onFirstCreate(ServerPlayerEntity player, String id) {
        fireOnce(player, CREATE, Text.literal(
                CbFmt.DIM + "Tip: give yourself the block with " + CbFmt.OK + "/cb give " + id
                        + CbFmt.DIM + " or find it in the Custom Blocks creative tab."));
    }

    /** First /cb give — tell the player about the offhand hologram preview. */
    public static void onFirstGive(ServerPlayerEntity player) {
        fireOnce(player, GIVE, Text.literal(
                CbFmt.DIM + "Tip: hold the block in your offhand for a hologram preview."));
    }

    /** First /cb setglow — tell the player held glowing blocks emit light. */
    public static void onFirstSetglow(ServerPlayerEntity player) {
        fireOnce(player, SETGLOW, Text.literal(
                CbFmt.DIM + "Tip: holding a glowing block in your hand emits dynamic light."));
    }

    /** Send {@code message} the first time {@code hintKey} fires for this player; else no-op. */
    private static void fireOnce(ServerPlayerEntity player, String hintKey, Text message) {
        String entry = player.getUuidAsString() + "|" + hintKey;
        synchronized (LOCK) {
            if (FIRED.contains(entry)) return;
            FIRED.add(entry);
            save();
        }
        Chat.toPlayer(player, message);
    }

    // ---- persistence ----------------------------------------------------------------

    private static void load() {
        synchronized (LOCK) {
            try {
                if (!Files.exists(FILE)) return;
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("fired")) {
                    for (var el : root.getAsJsonArray("fired")) FIRED.add(el.getAsString());
                }
            } catch (Exception e) {
                LOG.warn("[CustomBlocks] Could not load hints.json: {}", e.getMessage());
            }
        }
    }

    /** Atomic write. Called under LOCK by fireOnce. */
    private static void save() {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            FIRED.forEach(arr::add);
            root.add("fired", arr);
            Path tmp = DIR.resolve("hints.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to save hints.json", e);
        }
    }
}
