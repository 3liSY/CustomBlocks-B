/**
 * KickPreviewPayload.java — GROUP 04 §G04-5 dev tool. Server-to-client.
 *
 * The hidden {@code /cb debug kick <cause>} command runs on the server, so it cannot open the client
 * kick screen itself. It sends this payload naming the cause; the client builds that cause's CbKickInfo
 * and opens {@link com.customblocks.client.kick.CbKickScreen} WITHOUT a real disconnect, so every kick
 * screen (and Copy Report) can be tested on demand without reproducing a crash.
 *
 * Registered in PayloadRegistrar (S2C), received in CustomBlocksClient.
 *
 * Depends on: CustomPayload + PacketCodec. Called by: DebugCommands (sends), CustomBlocksClient (receives).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record KickPreviewPayload(String cause) implements CustomPayload {

    public static final CustomPayload.Id<KickPreviewPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "kick_preview"));

    public static final PacketCodec<PacketByteBuf, KickPreviewPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, KickPreviewPayload::cause, KickPreviewPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
