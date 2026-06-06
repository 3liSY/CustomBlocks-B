/**
 * MacroManager.java
 *
 * Responsibility: Record and replay named sequences of /cb commands.
 * A macro is a JSON list of command strings. Recording is per-player (in-memory);
 * playback dispatches each command via the server CommandManager.
 *
 * Storage: config/customblocks/macros/<name>.json
 * Format:  {"name":"<name>","commands":["/cb create ...", ...]}
 *
 * Depends on: Gson, Minecraft server (command dispatch during play)
 * Called by: MacroCommands
 */
package com.customblocks.core;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public final class MacroManager {

    private static final Logger LOG = LoggerFactory.getLogger("CustomBlocks/Macros");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR  = Path.of("config/customblocks/macros");

    private static final Map<UUID, ActiveRecording> RECORDING = new HashMap<>();

    private MacroManager() {}

    // ── Recording ────────────────────────────────────────────────────────────

    public static synchronized boolean startRecording(UUID player, String name) {
        if (RECORDING.containsKey(player)) return false;
        RECORDING.put(player, new ActiveRecording(name, new ArrayList<>()));
        return true;
    }

    public static synchronized boolean addCommand(UUID player, String command) {
        ActiveRecording rec = RECORDING.get(player);
        if (rec == null) return false;
        rec.commands().add(command);
        return true;
    }

    public static synchronized boolean isRecording(UUID player) {
        return RECORDING.containsKey(player);
    }

    /** Stop recording and persist. Returns the macro name, or null if not recording. */
    public static synchronized String stopRecording(UUID player) {
        ActiveRecording rec = RECORDING.remove(player);
        if (rec == null) return null;
        save(rec.name(), rec.commands());
        return rec.name();
    }

    public static synchronized void cancelRecording(UUID player) {
        RECORDING.remove(player);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static void save(String name, List<String> commands) {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            root.addProperty("name", name);
            JsonArray arr = new JsonArray();
            for (String cmd : commands) arr.add(cmd);
            root.add("commands", arr);
            Path file = macroPath(name);
            Path tmp  = file.resolveSibling(sanitize(name) + ".json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to save macro '{}'", name, e);
        }
    }

    /** Load a macro's command list. Returns null if not found. */
    public static List<String> load(String name) {
        try {
            Path file = macroPath(name);
            if (!Files.exists(file)) return null;
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("commands");
            List<String> cmds = new ArrayList<>();
            for (var el : arr) cmds.add(el.getAsString());
            return cmds;
        } catch (Exception e) {
            LOG.error("[CustomBlocks] Failed to load macro '{}'", name, e);
            return null;
        }
    }

    public static boolean delete(String name) {
        try { return Files.deleteIfExists(macroPath(name)); }
        catch (Exception e) { return false; }
    }

    public static List<String> listNames() {
        try {
            if (!Files.isDirectory(DIR)) return Collections.emptyList();
            return Files.list(DIR)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) { return Collections.emptyList(); }
    }

    public static boolean exists(String name) { return Files.exists(macroPath(name)); }

    private static Path macroPath(String name) {
        return DIR.resolve(sanitize(name) + ".json");
    }

    private static String sanitize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    public record ActiveRecording(String name, List<String> commands) {}
}
