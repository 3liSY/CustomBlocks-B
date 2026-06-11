/**
 * ChatPrefillPayload.java
 *
 * Responsibility: Server-to-client packet that opens the client's chat input with the
 * given text already typed (Group 04 — clicking a command in the /cb help chest GUI
 * pre-fills that command so the player only adds the arguments).
 *
 * Registered in CustomBlocksMod (server), received in CustomBlocksClient (client).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: HelpMenu (sends), CustomBlocksClient (receives)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChatPrefillPayload(String text) implements CustomPayload {

    public static final CustomPayload.Id<ChatPrefillPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "chat_prefill"));

    public static final PacketCodec<PacketByteBuf, ChatPrefillPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, ChatPrefillPayload::text, ChatPrefillPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
