/**
 * VaultConflict.java — Group 20 §S2 (Cloud Vault conflict screen, server side).
 *
 * When {@code /cb vault download <code>} restores a block whose id already exists locally, this opens a
 * client conflict screen (instead of S1's "delete it first" line) and applies the player's choice.
 *
 *   open(...)    — peek the downloaded ZIP off-thread (build the incoming preview), then send
 *                  {@link VaultConflictPayload} so the client shows BOTH blocks. No per-player buffer is
 *                  kept: the share code rides the packet, so resolve re-fetches the ZIP statelessly.
 *   resolve(...) — re-download by code off-thread, then apply on the server thread:
 *                    OVERRIDE     — delete the local block, import the incoming under that id.
 *                    KEEP_BOTH    — import the incoming under the typed id; local untouched.
 *                    RENAME_MINE  — re-id the local block to the typed id; import incoming under the original.
 *                  Every mutation reuses the proven SlotManager rails and records to UndoManager so a plain
 *                  /cb undo reverts it (compound actions use an ordered recordBatch — undo removes the
 *                  incoming first, freeing the id, then restores/renames the local block). Authoritative
 *                  server (CLAUDE.md §5.8): the client only states intent; everything is re-validated here.
 *
 * Depends on: VaultBlockCodec, CloudVaultClient, SlotManager, TextureStore, UndoManager, ResourcePackServer,
 *             SoundFx, ParticleFx, HudSync, Chat, VaultConflictPayload, VaultResolvePayload
 * Called by:  CloudCommands (open), CustomBlocksMod C2S receiver (resolve)
 */
package com.customblocks.command.handlers;

import com.customblocks.command.CbFmt;
import com.customblocks.CustomBlocksConfig;
import com.customblocks.cloud.CloudVaultClient;
import com.customblocks.cloud.VaultBlockCodec;
import com.customblocks.command.Chat;
import com.customblocks.core.BlockExporter.ImportResult;
import com.customblocks.core.ParticleFx;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;
import com.customblocks.core.SoundFx;
import com.customblocks.core.TextureStore;
import com.customblocks.core.UndoManager;
import com.customblocks.network.HudSync;
import com.customblocks.network.ResourcePackServer;
import com.customblocks.network.payloads.VaultConflictPayload;
import com.customblocks.network.payloads.VaultResolvePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public final class VaultConflict {

    private VaultConflict() {} // static-only

    /** Ids accept the same charset as /cb create (Brigadier word()): letters, digits, _ . + - */
    private static final String ID_CHARS = "[A-Za-z0-9_.+\\-]+";

    // ── Open the conflict screen ───────────────────────────────────────────────

    /**
     * Send the conflict screen to {@code player} for a downloaded ZIP whose {@code incomingId} clashes
     * with a local block. The image work (preview build) runs off-thread; the packet is sent back on the
     * server thread.
     */
    public static void open(ServerPlayerEntity player, String code, byte[] zip, String incomingId) {
        if (player == null) return;
        SlotData local = SlotManager.getById(incomingId);
        if (local == null) return; // clash vanished; nothing to resolve
        MinecraftServer server = player.server;
        new Thread(() -> {
            VaultBlockCodec.Peek peek = VaultBlockCodec.peek(zip);
            String youStats = VaultBlockCodec.statsOf(local);
            String inStats = peek != null ? peek.stats() : "";
            String suggest = suggestFreeId(incomingId);
            String meta = "you=" + youStats + "|in=" + inStats + "|suggest=" + suggest;
            byte[] preview = (peek != null && peek.preview() != null) ? peek.preview() : new byte[0];
            server.execute(() -> ServerPlayNetworking.send(player,
                    new VaultConflictPayload(code, incomingId, meta, preview)));
        }, "cb-vault-conflict-open").start();
    }

    // ── Apply the player's choice ──────────────────────────────────────────────

    /** Re-fetch the ZIP by code off-thread, then apply the chosen action on the server thread. */
    public static void resolve(ServerPlayerEntity player, String code, String action, String typedId) {
        if (player == null) return;
        MinecraftServer server = player.server;
        ServerCommandSource src = player.getCommandSource();
        if (!CustomBlocksConfig.cloudShareEnabled) {
            Chat.error(src, "Cloud sharing is disabled. Enable it in /cb config (cloudShareEnabled).");
            return;
        }
        Chat.info(src, "Resolving the download…");
        new Thread(() -> {
            byte[] zip = CloudVaultClient.downloadCategory(code);
            if (zip == null) {
                server.execute(() -> Chat.error(src, "Couldn't fetch that block again — try /cb vault download once more."));
                return;
            }
            VaultBlockCodec.Peek peek = VaultBlockCodec.peek(zip);
            if (peek == null) {
                server.execute(() -> Chat.error(src, "That block share looks corrupt — nothing was changed."));
                return;
            }
            String inId = peek.id();
            server.execute(() -> apply(player, src, zip, action, inId, typedId));
        }, "cb-vault-conflict-resolve").start();
    }

    private static void apply(ServerPlayerEntity player, ServerCommandSource src, byte[] zip,
                              String action, String inId, String typedId) {
        UUID who = player.getUuid();
        switch (action == null ? "" : action) {
            case VaultResolvePayload.OVERRIDE -> {
                SlotData old = SlotManager.getById(inId);
                if (old == null) { importFresh(src, who, zip); return; } // clash gone → plain import
                byte[] oldTex = TextureStore.load(old.index());
                SlotManager.delete(inId);
                ImportResult r = VaultBlockCodec.unpackAs(zip, inId);
                if (r.created().isEmpty()) { // import failed → put the old block back, lose nothing
                    SlotManager.restoreSnapshot(old);
                    if (oldTex != null) TextureStore.save(old.index(), oldTex);
                    ResourcePackServer.updatePack();
                    Chat.error(src, "Override failed: " + joinFailed(r) + " — your block was kept.");
                    return;
                }
                SlotData created = SlotManager.getById(inId);
                ResourcePackServer.updatePack();
                // Undo order matters: remove the incoming first (frees the id), THEN restore the old block.
                UndoManager.recordBatch(who, List.of(
                        new UndoManager.Op(UndoManager.Kind.CREATE, null, created, null, "vault import"),
                        new UndoManager.Op(UndoManager.Kind.DELETE, old, null, oldTex, "vault remove")),
                        "vault override " + inId);
                done(player, src, inId, "Replaced " + CbFmt.VALUE + inId + CbFmt.OK + " with the downloaded block.");
            }
            case VaultResolvePayload.KEEP_BOTH -> {
                String err = validateNew(typedId, inId);
                if (err != null) { Chat.error(src, err); return; }
                ImportResult r = VaultBlockCodec.unpackAs(zip, typedId);
                if (r.created().isEmpty()) { Chat.error(src, "Couldn't import: " + joinFailed(r)); return; }
                SlotData created = SlotManager.getById(typedId);
                ResourcePackServer.updatePack();
                UndoManager.recordCreate(who, created);
                done(player, src, typedId, "Kept both — the downloaded block was saved as " + CbFmt.VALUE + typedId + CbFmt.OK + ".");
            }
            case VaultResolvePayload.RENAME_MINE -> {
                SlotData old = SlotManager.getById(inId);
                if (old == null) { importFresh(src, who, zip); return; } // clash gone → plain import
                String err = validateNew(typedId, inId);
                if (err != null) { Chat.error(src, err); return; }
                SlotData after = SlotManager.reId(inId, typedId);
                if (after == null) { Chat.error(src, "Couldn't rename your block to \"" + typedId + "\"."); return; }
                ImportResult r = VaultBlockCodec.unpackAs(zip, inId);
                if (r.created().isEmpty()) { // import failed → undo the rename, lose nothing
                    SlotManager.reId(typedId, inId);
                    ResourcePackServer.updatePack();
                    Chat.error(src, "Couldn't import: " + joinFailed(r) + " — your block kept its id.");
                    return;
                }
                SlotData created = SlotManager.getById(inId);
                ResourcePackServer.updatePack();
                // Undo order: remove the incoming first (frees the original id), THEN rename the local block back.
                UndoManager.recordBatch(who, List.of(
                        new UndoManager.Op(UndoManager.Kind.CREATE, null, created, null, "vault import"),
                        new UndoManager.Op(UndoManager.Kind.REID, old, after, null, "reid")),
                        "vault keep both (rename mine) " + inId);
                done(player, src, inId, "Your block is now " + CbFmt.VALUE + typedId + CbFmt.OK + "; the downloaded block took " + CbFmt.VALUE + inId + CbFmt.OK + ".");
            }
            default -> Chat.error(src, "Unknown conflict action — nothing was changed.");
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Plain import (used when the clash has vanished by the time the player chooses). */
    private static void importFresh(ServerCommandSource src, UUID who, byte[] zip) {
        ImportResult r = VaultBlockCodec.unpack(zip);
        if (r.created().isEmpty()) { Chat.error(src, "Couldn't import: " + joinFailed(r)); return; }
        ResourcePackServer.updatePack();
        String id = r.created().get(0);
        UndoManager.recordCreate(who, SlotManager.getById(id));
        Chat.success(src, "Downloaded " + CbFmt.VALUE + id + CbFmt.OK + " — use " + CbFmt.BODY + "/cb give " + id + CbFmt.OK + " to get it.");
    }

    /** Validate a typed id for KEEP_BOTH / RENAME_MINE. Returns an error message, or null if it's fine. */
    private static String validateNew(String id, String inId) {
        if (id == null || id.isBlank()) return "Type a new id in the box first.";
        if (!id.matches(ID_CHARS)) return "Ids can only use letters, numbers, and _ . + - (got \"" + id + "\").";
        if (id.equals(inId)) return "Pick a different id from \"" + inId + "\".";
        if (SlotManager.hasId(id)) return "\"" + id + "\" is already taken — pick another.";
        return null;
    }

    /** First free id of the form {@code base2, base3, …} (the conflict screen's pre-fill). */
    private static String suggestFreeId(String base) {
        for (int n = 2; n < 1000; n++) {
            String cand = base + n;
            if (!SlotManager.hasId(cand)) return cand;
        }
        return base + System.currentTimeMillis();
    }

    private static String joinFailed(ImportResult r) {
        return r.failed().isEmpty() ? "no free slot" : String.join(", ", r.failed());
    }

    /** Success feedback: confirm line + give hint + sound/particle/title juice + refresh the client cache. */
    private static void done(ServerPlayerEntity player, ServerCommandSource src, String id, String msg) {
        Chat.success(src, msg + " Use " + CbFmt.BODY + "/cb give " + id + CbFmt.OK + " to hold it. Undo with " + CbFmt.BODY + "/cb undo" + CbFmt.OK + ".");
        SoundFx.play(player, "success");
        ParticleFx.play(player, "success");
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(CbFmt.OK + "✔ Resolved")));
        HudSync.broadcast(player.getServer()); // NO-REJOIN: resolved block shows live for all players (was actor-only)
    }
}
