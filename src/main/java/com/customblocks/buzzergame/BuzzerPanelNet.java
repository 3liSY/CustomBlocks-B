/**
 * BuzzerPanelNet.java — Group 31 item 4 (admin panel Screen — server side).
 *
 * Receives {@link BuzzerPanelActionPayload} from the admin Screen, validates op + the target panel, runs
 * the action on that panel's {@link PanelSession} (the same session verbs the commands use), then pushes a
 * fresh snapshot back via {@link OpenGuiPayload} so the open Screen refreshes in place. All world mutation
 * runs on the server thread (CLAUDE.md packet rule). Reveal additionally fires the near-panel broadcast.
 *
 * Depends on: AdminPanelBlockEntity, PanelSession, RoundBroadcast, GuiMode, OpenGuiPayload,
 *             BuzzerPanelActionPayload, ServerPlayNetworking
 * Called by:  CustomBlocksMod.onInitialize (registers this as the payload receiver)
 */
package com.customblocks.buzzergame;

import com.customblocks.command.Chat;
import com.customblocks.command.CbFmt;

import com.customblocks.gui.GuiMode;
import com.customblocks.network.payloads.BuzzerPanelActionPayload;
import com.customblocks.network.payloads.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class BuzzerPanelNet {

    private BuzzerPanelNet() {} // static-only

    /** Payload receiver body: hop to the server thread, then apply. */
    public static void handle(BuzzerPanelActionPayload payload, ServerPlayerEntity player) {
        var server = player.getServer();
        if (server != null) server.execute(() -> apply(payload, player));
    }

    private static void apply(BuzzerPanelActionPayload p, ServerPlayerEntity player) {
        if (!player.hasPermissionLevel(2)) {
            Chat.toPlayer(player, CbFmt.BAD + "Only operators can control the admin panel.");
            return;
        }
        ServerWorld world = player.getServerWorld();
        if (!(world.getBlockEntity(BlockPos.fromLong(p.panelPos())) instanceof AdminPanelBlockEntity panel)) {
            Chat.toPlayer(player, CbFmt.BAD + "That admin panel is gone.");
            return;
        }
        PanelSession s = panel.session();
        PanelSession.Result r = switch (p.action()) {
            case BuzzerPanelActionPayload.START -> s.tryStart();
            case BuzzerPanelActionPayload.STOP -> s.stop();
            case BuzzerPanelActionPayload.RESET -> s.reset();
            case BuzzerPanelActionPayload.REVEAL -> revealWithBroadcast(world, panel);
            case BuzzerPanelActionPayload.MODE -> PanelGui.cycleMode(s);
            case BuzzerPanelActionPayload.FORMAT -> PanelGui.toggleFormat(s);
            case BuzzerPanelActionPayload.COUNTDOWN -> PanelGui.cycleCountdown(s);
            case BuzzerPanelActionPayload.FALSESTART -> PanelGui.cycleFalseStart(s);
            case BuzzerPanelActionPayload.TARGET_PRESET -> PanelGui.cycleTargetPreset(s);
            case BuzzerPanelActionPayload.TARGET_SET -> s.setTargetSeconds(p.arg());
            case BuzzerPanelActionPayload.UNLINK -> s.unlinkBuzzerByIndex((int) Math.round(p.arg()));
            case BuzzerPanelActionPayload.REFRESH -> new PanelSession.Result(true, "");
            default -> new PanelSession.Result(false, "Unknown panel action.");
        };
        panel.markDirty();
        // Only surface failures in chat (guard messages); successes are shown by the Screen refresh + world FX.
        if (!r.ok() && !r.message().isEmpty()) {
            Chat.toPlayer(player, CbFmt.BAD + r.message());
        }
        // Push a fresh snapshot so the open Screen re-renders with the new state.
        ServerPlayNetworking.send(player, new OpenGuiPayload(GuiMode.BUZZER_PANEL.id, PanelGui.guiSnapshot(panel.session(), panel.getPos().asLong())));
    }

    private static PanelSession.Result revealWithBroadcast(ServerWorld world, AdminPanelBlockEntity panel) {
        PanelSession.Result r = panel.session().reveal();
        if (r.ok()) RoundBroadcast.announceReveal(world, panel.getPos(), panel.session());
        return r;
    }
}
