/**
 * TransparentBgPayload.java — Group 14 / Phase 1c Step 2b.
 *
 * Server-to-client packet carrying the off-atlas background preference: true = transparent
 * (see-through), false = solid black (default). Sent on join and whenever
 * `/cb config transparent` changes it, so the client renderer rebuilds its off-atlas
 * textures accordingly. Mirrors SilentPackPayload (Group 05).
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

public record TransparentBgPayload(boolean transparent) implements CustomPayload {

    public static final CustomPayload.Id<TransparentBgPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "transparent_bg"));

    public static final PacketCodec<PacketByteBuf, TransparentBgPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, TransparentBgPayload::transparent, TransparentBgPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
