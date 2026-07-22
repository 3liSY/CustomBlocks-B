/**
 * PackFilePayload.java
 *
 * Responsibility: Server-to-client. Carries ONE pack file, or one chunk of a large file, to the modded
 * client during a dedicated-server pack sync, tagged with the {@code session} id it belongs to (Group 05
 * §G). Small files arrive as a single chunk (index 0, count 1); a large texture is split into {@code count}
 * chunks the client appends in order to a {@code .part} file then verifies and commits. Chunk size is
 * capped server-side ({@link com.customblocks.network.packsync.PackSyncProtocol#CHUNK_BYTES}); the client
 * rejects any chunk over {@code MAX_CHUNK_BYTES} or belonging to a stale session before allocating.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  PackSyncService.pump (send, credit-paced), ClientPackReceiver.onFile (receive + stream to .part).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackFilePayload(long session, String path, int index, int count, byte[] data) implements CustomPayload {

    public static final CustomPayload.Id<PackFilePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_file"));

    public static final PacketCodec<PacketByteBuf, PackFilePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG,   PackFilePayload::session,
            PacketCodecs.STRING,     PackFilePayload::path,
            PacketCodecs.VAR_INT,    PackFilePayload::index,
            PacketCodecs.VAR_INT,    PackFilePayload::count,
            PacketCodecs.BYTE_ARRAY, PackFilePayload::data,
            PackFilePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
