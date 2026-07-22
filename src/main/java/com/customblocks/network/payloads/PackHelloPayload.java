/**
 * PackHelloPayload.java
 *
 * Responsibility: Client-to-server pack-sync compatibility handshake (Group 05 §I1/§I2/§I3). Right after
 * a modded client joins it declares the pack-sync {@code protocol} version it speaks (
 * {@link com.customblocks.network.packsync.PackSyncProtocol#PROTOCOL_VERSION}) and a human {@code build}
 * string (mod version + protocol). The server accepts and starts the selected-variant sync ONLY when the
 * protocol matches its own; otherwise it sends a friendly incompatibility message and logs BOTH distinct
 * builds — so an out-of-date client is told clearly instead of silently misdecoding a newer manifest and
 * missing its textures.
 *
 * Sent once per join, before any manifest/file transfer. A client too old to know this payload never sends
 * it; the server's post-join grace check then delivers the same incompatibility message (§I2).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  CustomBlocksClient (send on JOIN), PackSyncService.onHello (receive → gate the sync).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackHelloPayload(int protocol, String build) implements CustomPayload {

    public static final CustomPayload.Id<PackHelloPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_hello"));

    public static final PacketCodec<PacketByteBuf, PackHelloPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PackHelloPayload::protocol,
            PacketCodecs.STRING,  PackHelloPayload::build,
            PackHelloPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
