/**
 * PackDonePayload.java
 *
 * Responsibility: Server-to-client sentinel. Marks the end of a pack-sync file stream so the
 * modded client knows every requested file has arrived and can do ONE silent resource reload
 * (Group 05, remote/dedicated fix). Carries the server's whole-pack hash so the client can record
 * what it now has applied and skip a no-op resync on rejoin.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  PackSyncService (send, after the last file), ClientPackReceiver (receive -> reload).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackDonePayload(String hash) implements CustomPayload {

    public static final CustomPayload.Id<PackDonePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_done"));

    public static final PacketCodec<PacketByteBuf, PackDonePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, PackDonePayload::hash, PackDonePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
