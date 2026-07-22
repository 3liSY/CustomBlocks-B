/**
 * PackAckPayload.java
 *
 * Responsibility: Client-to-server flow-control ack (Group 05 §G1/§G4). As the modded client writes each
 * received chunk to its {@code .part} file — freeing that staging memory — it tells the server how many
 * {@code bytes} it consumed for the given {@code session}. The server adds those bytes back to that
 * session's send credit (capped so outstanding data never exceeds MAX_INFLIGHT), which paces the stream to
 * the client's real write rate and keeps in-flight data bounded under latency/loss without a false timeout.
 *
 * Acks for a superseded session id are ignored, so a cancelled transfer can't credit the new one.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  ClientPackReceiver.onFile (send, per chunk), PackSyncService.onAck (receive, replenish credit).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackAckPayload(long session, long bytes) implements CustomPayload {

    public static final CustomPayload.Id<PackAckPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_ack"));

    public static final PacketCodec<PacketByteBuf, PackAckPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, PackAckPayload::session,
            PacketCodecs.VAR_LONG, PackAckPayload::bytes,
            PackAckPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
