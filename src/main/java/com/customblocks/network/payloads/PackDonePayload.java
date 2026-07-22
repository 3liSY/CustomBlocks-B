/**
 * PackDonePayload.java
 *
 * Responsibility: Server-to-client sentinel for one round of a sync {@code session} (Group 05 §G). Marks
 * that every file the client requested in this round has been streamed, so the client can verify, then
 * either re-request files that failed SHA-256 (a further round in the same session) or, once all are
 * verified, do ONE silent resource reload and acknowledge application with PackAppliedPayload. Carries the
 * server's whole-pack hash so the client can record what it applied and skip a no-op resync on rejoin.
 *
 * Done means "all requested bytes were sent" — NOT "applied". The server only treats a generation as
 * applied when the client returns PackAppliedPayload(ok) (§G10), never on this sentinel alone.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  PackSyncService.sendDone (send, after the last file), ClientPackReceiver.onDone (receive).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackDonePayload(long session, String hash) implements CustomPayload {

    public static final CustomPayload.Id<PackDonePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_done"));

    public static final PacketCodec<PacketByteBuf, PackDonePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, PackDonePayload::session,
            PacketCodecs.STRING,   PackDonePayload::hash,
            PackDonePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
