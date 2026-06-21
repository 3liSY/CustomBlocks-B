/**
 * PackFilePayload.java
 *
 * Responsibility: Server-to-client. Carries ONE pack file, or one chunk of a large file, to the
 * modded client during a dedicated-server pack sync (Group 05, remote/dedicated fix). Small files
 * (most JSON models/blockstates and tiny PNGs) arrive as a single chunk (index 0, count 1); a
 * large texture/anim strip is split into {@code count} chunks the client buffers in order then
 * writes once. Chunk size is capped server-side well under the custom-payload limit.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  PackSyncService (send, throttled per tick), ClientPackReceiver (receive + buffer).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackFilePayload(String path, int index, int count, byte[] data) implements CustomPayload {

    public static final CustomPayload.Id<PackFilePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_file"));

    public static final PacketCodec<PacketByteBuf, PackFilePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,     PackFilePayload::path,
            PacketCodecs.VAR_INT,    PackFilePayload::index,
            PacketCodecs.VAR_INT,    PackFilePayload::count,
            PacketCodecs.BYTE_ARRAY, PackFilePayload::data,
            PackFilePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
