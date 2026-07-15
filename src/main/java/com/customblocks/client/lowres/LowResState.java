/**
 * LowResState.java — GROUP 05 §E / G05-5 (per-client low-res texture mode). CLIENT-SIDE ONLY.
 *
 * Responsibility: hold + persist the weak-GPU friend's low-res choice, keyed PER SERVER (owner chose
 * "this server only"). Also remembers which (server, size) last wrote the shared loose pack folder and
 * the server-manifest sha1 of every file we currently have shrunk on disk — so a rejoin at the same
 * server + size diffs against the SERVER's 512 fingerprints (not the on-disk shrunk bytes, which would
 * mismatch and re-download everything every join, per the G05-5 spec warning).
 *
 * "off" is the sentinel size {@link #FULL} (512) = no shrink. Absent server entry = off.
 *
 * Depends on: Gson. Called by: LowResClientCommand (set/read), ClientPackReceiver (resolve size at
 *             sync time, commit folder context after a successful apply), CustomBlocksClient (load on init).
 */
package com.customblocks.client.lowres;

import com.customblocks.CustomBlocksMod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class LowResState {

    private LowResState() {} // static-only

    /** Sentinel "off"/base size — the pack's real texture size; nothing above this is shrunk. */
    public static final int FULL = 512;

    private static final String FILE = "config/customblocks/data/lowres.json";
    private static final Gson   GSON = new Gson();

    /** server address -> desired size (256/128). Absent = off (FULL). */
    private static final Map<String, Integer> SERVER_SIZES = new LinkedHashMap<>();

    /** What physically sits in resourcepacks/CustomBlocks right now (that folder is shared across servers). */
    private static String lastFolderServer = "";
    private static int    lastFolderSize   = FULL;
    /** path -> server-512 sha1 for every file we currently have shrunk on disk (fast-rejoin diff basis). */
    private static Map<String, String> sidecar = new LinkedHashMap<>();

    // ── current server ────────────────────────────────────────────────────────
    /** Address of the server this client is connected to, or null (singleplayer / no entry). */
    public static String currentServerKey(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isEmpty()) return info.address;
        return null;
    }

    // ── size lookups ──────────────────────────────────────────────────────────
    public static int sizeFor(String server) {
        if (server == null) return FULL;
        Integer s = SERVER_SIZES.get(server);
        return s == null ? FULL : s;
    }

    /** Set (256/128) or clear (>= FULL = off) the size for a server, then persist. */
    public static void setSize(String server, int size) {
        if (server == null) return;
        if (size >= FULL) SERVER_SIZES.remove(server);
        else              SERVER_SIZES.put(server, size);
        save();
    }

    // ── on-disk folder context ─────────────────────────────────────────────────
    public static String lastFolderServer() { return lastFolderServer; }
    public static int    lastFolderSize()   { return lastFolderSize; }
    public static Map<String, String> sidecar() { return sidecar; }

    /** Record that the loose pack now holds {@code server}'s pack at {@code size}, with these server shas. */
    public static void commitFolder(String server, int size, Map<String, String> serverShas) {
        lastFolderServer = server == null ? "" : server;
        lastFolderSize   = size;
        sidecar = new LinkedHashMap<>(serverShas == null ? Map.of() : serverShas);
        save();
    }

    // ── persistence ─────────────────────────────────────────────────────────────
    public static void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            SERVER_SIZES.clear();
            if (root.has("servers") && root.get("servers").isJsonObject())
                for (var e : root.getAsJsonObject("servers").entrySet())
                    SERVER_SIZES.put(e.getKey(), e.getValue().getAsInt());
            lastFolderServer = str(root, "folderServer", "");
            lastFolderSize   = root.has("folderSize") && !root.get("folderSize").isJsonNull()
                    ? root.get("folderSize").getAsInt() : FULL;
            sidecar = new LinkedHashMap<>();
            if (root.has("sidecar") && root.get("sidecar").isJsonObject())
                for (var e : root.getAsJsonObject("sidecar").entrySet())
                    sidecar.put(e.getKey(), e.getValue().getAsString());
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] low-res state load failed; starting fresh.", e);
        }
    }

    public static void save() {
        try {
            Path file = Path.of(FILE);
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject servers = new JsonObject();
            for (var e : SERVER_SIZES.entrySet()) servers.addProperty(e.getKey(), e.getValue());
            root.add("servers", servers);
            root.addProperty("folderServer", lastFolderServer);
            root.addProperty("folderSize", lastFolderSize);
            JsonObject side = new JsonObject();
            for (var e : sidecar.entrySet()) side.addProperty(e.getKey(), e.getValue());
            root.add("sidecar", side);
            Path tmp = file.resolveSibling("lowres.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            CustomBlocksMod.LOGGER.warn("[CustomBlocks] low-res state save failed.", e);
        }
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }
}
