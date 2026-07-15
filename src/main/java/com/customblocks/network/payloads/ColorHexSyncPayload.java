/**
 * ColorHexSyncPayload.java — Group 06 / M3 hex (G06-C fix).
 *
 * Server-to-client packet carrying the four colour-variant hexes (red / yellow / green / black).
 * Sent on join and whenever `/cb config hex <colour> <#RRGGBB>` changes one, so the client's
 * in-memory CustomBlocksConfig hex fields match the server's. Without this, a dedicated client
 * kept its OWN (default) hex, so the Square/Triangle tool name (ShapeToolItem.getName, rendered
 * client-side) showed the stale "[#EE3333]" no matter what the server was set to. Mirrors
 * TransparentBgPayload (Group 14).
 *
 * Registered in CustomBlocksMod (server), received in CustomBlocksClient (client).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: CustomBlocksMod / HexCommands (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ColorHexSyncPayload(String red, String yellow, String green, String black)
        implements CustomPayload {

    public static final CustomPayload.Id<ColorHexSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "color_hex_sync"));

    public static final PacketCodec<PacketByteBuf, ColorHexSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ColorHexSyncPayload::red,
            PacketCodecs.STRING, ColorHexSyncPayload::yellow,
            PacketCodecs.STRING, ColorHexSyncPayload::green,
            PacketCodecs.STRING, ColorHexSyncPayload::black,
            ColorHexSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
