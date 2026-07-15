/**
 * BulkSnapshot.java — Group 07 §G07-3 (Bulk Workbench Screen — server side).
 *
 * Builds the small JSON blob the Bulk Workbench opens on, and sends it as an OpenGuiPayload.
 *
 *   {"tab":"browse|bulk|pick", "locked":[ids…], "fav":[ids…]}
 *
 * Deliberately tiny. Every field the Screen needs to draw a row (id, name, category, glow, hardness,
 * sound) is ALREADY on the client in ClientSlotCache, so the snapshot carries only the two things that
 * cache lacks: the global lock set and the RUNNING PLAYER's favorites.
 *
 * Why not put these on HudSync's shared index instead: HudSync.broadcast() builds ONE JSON blob for every
 * player, so a per-player "fav" field baked into it would leak player A's favorites onto player B's screen.
 * Sending them per-player, on open and after each Apply, sidesteps that by construction and touches no core
 * class (no HudSyncPayload / ClientSlotCache.Entry / HudRenderer change). Owner decision, 2026-07-10.
 *
 * Depends on: LockManager, FavoritesManager, GuiMode, OpenGuiPayload, Gson
 * Called by:  BulkNet (re-push after an op), BulkCommands / ChestGuiCommands / UtilityCommands (open)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.Chat;
import com.customblocks.core.FavoritesManager;
import com.customblocks.core.LockManager;
import com.customblocks.core.UndoManager;
import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BulkSnapshot {

    private BulkSnapshot() {} // static-only

    /** The Browse tab — a searchable list of every block. What /cb list opens for a player. */
    public static final String TAB_BROWSE = "browse";
    /** The Console tab. Bare {@code /cb bulk} opens it on the 3×3 op-picker grid; {@code "bulk:<opKey>"} lands
     *  straight on one op (see {@link #openForOp}) — §G07-4 A5a. */
    public static final String TAB_BULK = "bulk";
    /** Pick mode — Browse with a "Use these" bar, for Group 12's Export Dashboard hand-off. */
    public static final String TAB_PICK = "pick";

    /** Open the Bulk Workbench on {@code tab}, switching to it if the Screen is already open. */
    public static void open(ServerPlayerEntity player, String tab) {
        BulkResult.clear(player.getUuid()); // an explicit open must not pop a stale chat-path result modal (X3)
        BulkToast.clear(player.getUuid());  // …nor a stale undo/redo toast (§G07-4)
        send(player, tab == null || tab.isBlank() ? TAB_BROWSE : tab);
    }

    /**
     * Command / chest-tile entry point. Closes a chest menu first so the server and client agree about what's
     * open, then opens the Workbench.
     *
     * The guard matters: {@code closeHandledScreen()} makes the client run {@code setScreen(null)}, which would
     * also close an already-open Workbench — so a second {@code /cb bulk} would rebuild the Screen from scratch
     * and silently throw away the player's ticked selection. Only close when a real chest handler is up.
     */
    public static int openFor(ServerCommandSource src, String tab) {
        if (src.getEntity() instanceof ServerPlayerEntity p) {
            if (p.currentScreenHandler != p.playerScreenHandler) p.closeHandledScreen();
            open(p, tab);
            return 1;
        }
        Chat.error(src, "Open the Bulk Operations Hub in-game as a player.");
        return 0;
    }

    /** Chest-tile entry point: the player is definitely inside a chest menu, so always close it. */
    public static void openFromChest(ServerPlayerEntity player, String tab) {
        player.closeHandledScreen();
        open(player, tab);
    }

    /**
     * Open the Console landing straight on one op — what a NAMED bulk* command does (e.g. /cb bulkreid), so it
     * skips the op-picker grid that bare /cb bulk shows. {@code opKey} ∈ property/rename/category/duplicate/
     * export/lock/favorite/reid/delete; the client maps it back to a rail op (BulkOpSpec.railForKey).
     */
    public static int openForOp(ServerCommandSource src, String opKey) {
        return openFor(src, TAB_BULK + ":" + opKey);
    }

    /**
     * Re-render an already-open Workbench in place after an op. Sends a BLANK tab, which the Screen reads as
     * "keep whatever tab the player is on" — an Apply on the Bulk tab must not throw them back to Browse.
     */
    public static void push(ServerPlayerEntity player) { send(player, ""); }

    /**
     * Live-refresh EVERY player who has the Hub open, not just the one who applied (§G07-4 MP ruling). Each
     * recipient gets their OWN snapshot (their favorites, never another player's), and the blank tab means
     * "refresh only" — a player who has the Hub closed ignores it (see BulkWorkbenchScreen.wantsOpen), so this
     * broadcast can never pop the Hub open on a bystander. One tiny blob per online player; harmless at scale.
     */
    public static void pushAll(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) send(p, "");
    }

    private static void send(ServerPlayerEntity player, String tab) {
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.BULK_WORKBENCH.id, build(player, tab)));
    }

    /** Build the snapshot JSON: the tab (blank = keep), the global lock set, this player's favorites. */
    public static String build(ServerPlayerEntity player, String tab) {
        JsonObject root = new JsonObject();
        root.addProperty("tab", tab == null ? "" : tab);
        root.add("locked", array(LockManager.list()));
        root.add("fav", array(FavoritesManager.list(player.getUuid())));
        String result = BulkResult.consume(player.getUuid()); // X3: relay the actor's last op result into the Hub (once)
        if (result != null) root.addProperty("result", result);
        String toast = BulkToast.consume(player.getUuid());   // §G07-4: relay the actor's last undo/redo toast (once)
        if (toast != null) root.addProperty("toast", toast);
        // §G07-4 history panel — this player's undo/redo stacks, most-recent first, as friendly labels.
        root.add("undo", labels(UndoManager.undoStack(player.getUuid())));
        root.add("redo", labels(UndoManager.redoStack(player.getUuid())));
        return root.toString();
    }

    private static JsonArray array(java.util.List<String> ids) {
        JsonArray a = new JsonArray();
        if (ids != null) for (String id : ids) a.add(id);
        return a;
    }

    private static JsonArray labels(java.util.List<UndoManager.Op> ops) {
        JsonArray a = new JsonArray();
        if (ops != null) for (UndoManager.Op op : ops) a.add(BulkNet.prettyLabel(op));
        return a;
    }
}
