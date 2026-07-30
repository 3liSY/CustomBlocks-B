/**
 * PayloadRegistrar.java
 *
 * Responsibility: registers every S2C/C2S network payload type and their global receivers.
 * Split out of CustomBlocksMod.onInitialize() to keep that file under the §9.3 500-line cap
 * (pure move, no behavior change).
 *
 * Depends on: all com.customblocks.network.payloads.* payload types
 * Called by:  CustomBlocksMod.onInitialize()
 */
package com.customblocks;

import com.customblocks.network.payloads.ArabicLabelsPayload;
import com.customblocks.network.payloads.ChatPrefillPayload;
import com.customblocks.network.payloads.GuiBackPayload;
import com.customblocks.network.payloads.HudStatePayload;
import com.customblocks.network.payloads.HudSyncPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import com.customblocks.network.payloads.RecolorApplyPayload;
import com.customblocks.network.payloads.RegenPackPayload;
import com.customblocks.network.payloads.SilentPackPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

final class PayloadRegistrar {

    private PayloadRegistrar() {}

    static void registerAll() {
        // Register server→client payloads (Phase 10/11)
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID,   OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.ID,   HudSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HudStatePayload.ID,  HudStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatPrefillPayload.ID, ChatPrefillPayload.CODEC); // Group 04
        PayloadTypeRegistry.playS2C().register(SilentPackPayload.ID, SilentPackPayload.CODEC);   // Group 05
        PayloadTypeRegistry.playS2C().register(                                                  // Group 04 §G04-5 dev
                com.customblocks.network.payloads.KickPreviewPayload.ID,
                com.customblocks.network.payloads.KickPreviewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(                                                  // Group 03 §G03-2
                com.customblocks.network.payloads.WidgetSyncPayload.ID,
                com.customblocks.network.payloads.WidgetSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(                                                  // Group 14 Phase 1c Step 2b
                com.customblocks.network.payloads.TransparentBgPayload.ID,
                com.customblocks.network.payloads.TransparentBgPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(                                                  // Group 06 / M3 hex (G06-C)
                com.customblocks.network.payloads.ColorHexSyncPayload.ID,
                com.customblocks.network.payloads.ColorHexSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RegenPackPayload.ID,  RegenPackPayload.CODEC);    // Group 05 — modded local regen (integrated host)
        PayloadTypeRegistry.playS2C().register(                                                  // Group 12 §B — folder-import progress
                com.customblocks.network.payloads.ImportProgressPayload.ID,
                com.customblocks.network.payloads.ImportProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(                                                  // Group 20 §K — auto-update handshake
                com.customblocks.network.payloads.VersionHandshakePayload.ID,
                com.customblocks.network.payloads.VersionHandshakePayload.CODEC);
        // Group 05 — remote/dedicated file-level pack sync (modded client on a real server).
        PayloadTypeRegistry.playS2C().register(
                com.customblocks.network.payloads.PackManifestPayload.ID,
                com.customblocks.network.payloads.PackManifestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                com.customblocks.network.payloads.PackFilePayload.ID,
                com.customblocks.network.payloads.PackFilePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                com.customblocks.network.payloads.PackDonePayload.ID,
                com.customblocks.network.payloads.PackDonePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.PackRequestPayload.ID,
                com.customblocks.network.payloads.PackRequestPayload.CODEC);
        // Group 05 §G — client flow-control ack (credit) + terminal application result.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.PackAckPayload.ID,
                com.customblocks.network.payloads.PackAckPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.PackAppliedPayload.ID,
                com.customblocks.network.payloads.PackAppliedPayload.CODEC);
        // Group 05 §I — client compatibility handshake (protocol + build).
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.PackHelloPayload.ID,
                com.customblocks.network.payloads.PackHelloPayload.CODEC);
        // Modded client (dedicated server) asks for the pack files it lacks → queue them for streaming.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackRequestPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() ->
                            com.customblocks.network.packsync.PackSyncService.onRequest(player, payload.session(), payload.gz()));
                });
        // §G1/§G4: client returns send credit as it writes each chunk → the server may stream more.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackAckPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() ->
                            com.customblocks.network.packsync.PackSyncService.onAck(player, payload.session(), payload.bytes()));
                });
        // §G10/§G11: client reports the reload result → server marks applied (ok) or records a failure.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackAppliedPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() ->
                            com.customblocks.network.packsync.PackSyncService.onApplied(player, payload.session(), payload.hash(), payload.ok()));
                });
        // §I1-3: client declares its pack-sync protocol → server accepts + syncs, or rejects clearly.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.PackHelloPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() ->
                            com.customblocks.network.packsync.PackSyncService.onHello(player, payload.protocol(), payload.build()));
                });
        PayloadTypeRegistry.playS2C().register(                                                    // Group 32 — Explosive Tomato E8
                com.customblocks.network.payloads.SauceHitPayload.ID,
                com.customblocks.network.payloads.SauceHitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArabicLabelsPayload.ID, ArabicLabelsPayload.CODEC); // Group 13 / O6
        PayloadTypeRegistry.playS2C().register(                                                    // Group 30 — Guess Mode
                com.customblocks.network.payloads.GuessModePayload.ID,
                com.customblocks.network.payloads.GuessModePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(                                                    // Group 30 §H — Placed Mask
                com.customblocks.network.payloads.PlacedMaskPayload.ID,
                com.customblocks.network.payloads.PlacedMaskPayload.CODEC);
        // Group 30 · G30-4: client→server pose from the Guess Settings screen (replaces the removed
        // /cb guess pose command). Op-checked; writes GuessPoseStore then re-broadcasts so it applies live.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.GuessPosePayload.ID,
                com.customblocks.network.payloads.GuessPosePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.GuessPosePayload.ID, (payload, context) -> {
                    var player = context.player();
                    float[] v = payload.radians();
                    if (v == null) return;
                    player.server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;              // server stays authoritative (§5.8)
                        com.customblocks.core.GuessPoseStore.setAll(v[0], v[1], v[2], v[3], v[4], v[5]);
                        com.customblocks.network.GuessSync.broadcast(player.server);
                    });
                });
        // Group 30 · G30-8b: client→server showcase tuning from the Guess Settings screen's Showcase tab.
        // Op-checked; writes GuessShowcaseStore then re-broadcasts so every placed Showcase updates live.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.GuessShowcasePayload.ID,
                com.customblocks.network.payloads.GuessShowcasePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.GuessShowcasePayload.ID, (payload, context) -> {
                    var player = context.player();
                    com.customblocks.network.payloads.GuessShowcasePayload.Parsed v = payload.parse();
                    if (v == null) return;
                    player.server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;              // server stays authoritative (§5.8)
                        com.customblocks.core.GuessShowcaseStore.setAll(v.inner(), v.size());
                        com.customblocks.network.GuessSync.broadcast(player.server);
                    });
                });
        PayloadTypeRegistry.playS2C().register(                                                    // Group 14 Phase 2 — studio edit-load
                com.customblocks.network.payloads.StudioEditPayload.ID,
                com.customblocks.network.payloads.StudioEditPayload.CODEC);
        // Group 10: client→server live-recolour Apply. Server bakes; client only previews.
        PayloadTypeRegistry.playC2S().register(RecolorApplyPayload.ID, RecolorApplyPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                RecolorApplyPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.core.ColorToolService.applyRecolor(
                            player, payload.id(), payload.hue(), payload.sat(), payload.light(),
                            payload.temp(), payload.contrast(), payload.shadowLift(), payload.highlightDrop(), payload.filter()));
                });

        // Group 10: client→server "go back" — cancelling a colour client screen reopens the menu
        // the player came from (Nav.current), instead of dropping them to the world.
        PayloadTypeRegistry.playC2S().register(GuiBackPayload.ID, GuiBackPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                GuiBackPayload.ID, (payload, context) -> {
                    var player = context.player();
                    com.customblocks.gui.chest.Nav.MenuKey prev =
                            com.customblocks.gui.chest.Nav.current(player.getUuid());
                    if (prev != null) com.customblocks.gui.chest.GuiRouter.render(player, prev);
                });

        // Group 13: client→server actions from the live Arabic preview screen — change the preview
        // colours (re-render pack-free) or Create the real block. Server stays authoritative.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.ArabicPreviewPayload.ID,
                com.customblocks.network.payloads.ArabicPreviewPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ArabicPreviewPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> {
                        if (payload.action() == com.customblocks.network.payloads.ArabicPreviewPayload.ACTION_CREATE)
                            com.customblocks.arabic.ArabicMaker.finalizeFromPreview(player);
                        else
                            com.customblocks.arabic.ArabicMaker.updatePreviewColours(
                                    player, payload.letterArgb(), payload.bgArgb());
                    });
                });

        // Group 27 §G27.5: client→server Shape Editor save. Server applies via the /cb setshape rail.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.ShapeEditorPayload.ID,
                com.customblocks.network.payloads.ShapeEditorPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.ShapeEditorPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.ShapeCommands
                            .applyFromEditor(player, payload.id(), payload.shape()));
                });

        // Group 27 §G27.6: client→server Block Creation Studio "Create & Publish". Server creates via
        // the same CreationCommands rail /cb create uses (validate id → SlotManager.create → texture).
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.CreateStudioPayload.ID,
                com.customblocks.network.payloads.CreateStudioPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.CreateStudioPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CreationStudioBridge
                            .createFromStudio(player, payload.id(), payload.name(), payload.url(), payload.attrs()));
                });

        // Group 20 §S2: Cloud Vault conflict screen. S2C opens the screen (the incoming block's preview
        // rides the packet); C2S applies the player's choice (the server re-fetches by code + re-validates
        // everything — client never mutates server state, §5.8).
        PayloadTypeRegistry.playS2C().register(
                com.customblocks.network.payloads.VaultConflictPayload.ID,
                com.customblocks.network.payloads.VaultConflictPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.VaultResolvePayload.ID,
                com.customblocks.network.payloads.VaultResolvePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.VaultResolvePayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.VaultConflict
                            .resolve(player, payload.code(), payload.action(), payload.typedId()));
                });

        // Group 14 Phase 2: client→server studio "Save changes" (edit mode). Applies attrs + merges the
        // animation knobs onto the block's existing AnimData (per-frame timing preserved). Authoritative.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.StudioSavePayload.ID,
                com.customblocks.network.payloads.StudioSavePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.StudioSavePayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CreationStudioBridge
                            .saveFromStudio(player, payload.origId(), payload.id(), payload.name(),
                                    payload.url(), payload.attrs()));
                });

        // Group 07 §G07-3: client→server bulk op from the Bulk Workbench Screen. BulkNet validates the op,
        // re-resolves the scope server-side, and calls the real bulk handler with structured args.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.BulkActionPayload.ID,
                com.customblocks.network.payloads.BulkActionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.BulkActionPayload.ID, (payload, context) ->
                        com.customblocks.command.handlers.BulkNet.handle(payload, context.player()));

        // Group 09 §G09-A4: client→server backup action from the Backup Screen. BackupCommands runs the
        // real create/restore/delete/rename/protect rail server-side, then re-pushes the refreshed screen.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.BackupActionPayload.ID,
                com.customblocks.network.payloads.BackupActionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.BackupActionPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.BackupScreenCommands
                            .handleScreenAction(player, payload.action(), payload.name(), payload.arg()));
                });

        // Group 07: client→server Set All from the Set All Screen. Runs the same validated auto-backup +
        // apply-to-all rail as /cb setall <setting> <value>.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.SetAllActionPayload.ID,
                com.customblocks.network.payloads.SetAllActionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.SetAllActionPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.SetAllCommands
                            .handleScreenAction(player, payload.setting(), payload.value()));
                });

        // Group 27 §G27.6: client→server category management from the studio's Category tab
        // (rename / delete / colour / set-default). Applied through the existing category rails.
        PayloadTypeRegistry.playC2S().register(
                com.customblocks.network.payloads.CategoryAdminPayload.ID,
                com.customblocks.network.payloads.CategoryAdminPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.customblocks.network.payloads.CategoryAdminPayload.ID, (payload, context) -> {
                    var player = context.player();
                    player.server.execute(() -> com.customblocks.command.handlers.CategoryAdminBridge
                            .handle(player, payload.op(), payload.cat(), payload.arg()));
                });
    }
}
