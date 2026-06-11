/**
 * SilentPackPayload.java
 *
 * Responsibility: Server-to-client packet that tells the client whether to silently
 * auto-accept this server's resource-pack prompt (Group 05). Sent on join and whenever
 * `/cb config silentpack` changes the value. The client only goes silent when OUR server
 * tells it to, so the auto-accept never bleeds onto other servers' packs.
 *
 * Registered in CustomBlocksMod (server), received in CustomBlocksClient (client).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: CustomBlocksMod / ConfigCommands (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SilentPackPayload(boolean silent) implements CustomPayload {

    public static final CustomPayload.Id<SilentPackPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "silent_pack"));

    public static final PacketCodec<PacketByteBuf, SilentPackPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, SilentPackPayload::silent, SilentPackPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
