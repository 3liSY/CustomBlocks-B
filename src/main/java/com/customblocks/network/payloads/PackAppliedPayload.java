/**
 * PackAppliedPayload.java
 *
 * Responsibility: Client-to-server terminal ack for a sync {@code session} (Group 05 §G10/§G11). The client
 * sends it ONLY after every requested file has been SHA-256 verified and the silent resource reload has
 * actually completed: {@code ok=true} with the applied whole-pack {@code hash} when the pack loaded, or
 * {@code ok=false} when the reload failed or dropped the pack. The server advances its per-player applied
 * state and logs "applied gen N" only on {@code ok=true}; on {@code ok=false} it records an application
 * failure and keeps the previous verified generation recoverable. The server never infers application from
 * the file stream finishing — only from this message.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  ClientPackReceiver (send, after reload), PackSyncService.onApplied (receive).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackAppliedPayload(long session, String hash, boolean ok) implements CustomPayload {

    public static final CustomPayload.Id<PackAppliedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_applied"));

    public static final PacketCodec<PacketByteBuf, PackAppliedPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, PackAppliedPayload::session,
            PacketCodecs.STRING,   PackAppliedPayload::hash,
            PacketCodecs.BOOL,     PackAppliedPayload::ok,
            PackAppliedPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
