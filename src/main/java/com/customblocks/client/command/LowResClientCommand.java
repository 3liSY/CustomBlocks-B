/**
 * LowResClientCommand.java — GROUP 05 §E / G05-5 (per-client low-res texture mode). CLIENT-SIDE ONLY.
 *
 * Responsibility: register the CLIENT-side "/cblowres [256|128|off]" command so a NON-OP player on a
 * weak GPU can shrink the CustomBlocks pack for himself. It is registered through Fabric's
 * ClientCommandManager (no permission gate), a separate dispatcher from the op-gated server "/cb" tree.
 *
 * IMPORTANT — its own root, NOT a "/cb" subcommand: Fabric's client dispatcher OWNS whatever root
 * literal it registers. If this were "/cb lowres", the client would own the entire "/cb" literal and
 * reject every OTHER "/cb ..." (guess, list, give…) with "Incorrect argument for command" BEFORE it
 * ever reaches the server — Brigadier's getRelevantNodes finds no client match and the resulting
 * dispatcherUnknownArgument is NOT one Fabric forwards on. So this command lives under its own
 * "/cblowres" root; the server "/cb" tree is left completely untouched. No server, and no other
 * player, is affected.
 *
 * Depends on: LowResState (persist the choice), ClientPackReceiver (re-pull + shrink + reload).
 * Called by:  CustomBlocksClient.onInitializeClient (register()).
 */
package com.customblocks.client.command;

import com.customblocks.client.lowres.LowResState;
import com.customblocks.client.packsync.ClientPackReceiver;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public final class LowResClientCommand {

    private LowResClientCommand() {} // static-only

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(literal("cblowres")
                        .executes(ctx -> status(ctx.getSource()))
                        .then(literal("256").executes(ctx -> apply(ctx.getSource(), 256)))
                        .then(literal("128").executes(ctx -> apply(ctx.getSource(), 128)))
                        .then(literal("off").executes(ctx -> apply(ctx.getSource(), LowResState.FULL)))));
    }

    private static int status(FabricClientCommandSource src) {
        String server = LowResState.currentServerKey(src.getClient());
        if (server == null) { needServer(src); return 0; }
        int size = LowResState.sizeFor(server);
        src.sendFeedback(Text.literal("§b[CustomBlocks] low-res is "
                + (size >= LowResState.FULL ? "OFF (full 512px)" : size + "px")
                + " on this server. Use §f/cblowres 256§b | §f128§b | §foff§b."));
        return 1;
    }

    private static int apply(FabricClientCommandSource src, int size) {
        MinecraftClient client = src.getClient();
        String server = LowResState.currentServerKey(client);
        if (server == null) { needServer(src); return 0; }
        LowResState.setSize(server, size);
        if (!ClientPackReceiver.applyLowRes(client, size)) {
            src.sendFeedback(Text.literal("§e[CustomBlocks] No CustomBlocks pack from this server yet — "
                    + "wait a moment after joining, then run it again."));
            return 0;
        }
        src.sendFeedback(Text.literal(size >= LowResState.FULL
                ? "§b[CustomBlocks] Restoring full 512px pack… (large — may lag while it re-downloads)."
                : "§b[CustomBlocks] Applying " + size + "px low-res pack… no rejoin needed."));
        return 1;
    }

    private static void needServer(FabricClientCommandSource src) {
        src.sendFeedback(Text.literal("§e[CustomBlocks] /cblowres only works while connected to a server."));
    }
}
