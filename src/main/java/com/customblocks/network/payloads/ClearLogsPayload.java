/**
 * ClearLogsPayload.java
 *
 * Responsibility: Server-to-client "wipe my [CB] chat lines" signal for /cb clearlogs (Group 04 §G04-4).
 *
 * Chat history lives entirely on the CLIENT — the server cannot reach into someone's chat box and
 * remove lines. So /cb clearlogs is a server command that does nothing but send this, and the client
 * does the actual filtering (see CbChatMirror). It carries no data: "clear mine" is the whole message.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  UtilityCommands (/cb clearlogs, sends), CustomBlocksClient (receives)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClearLogsPayload() implements CustomPayload {

    public static final CustomPayload.Id<ClearLogsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "clear_logs"));

    /** No fields — the packet's existence IS the instruction. */
    public static final PacketCodec<PacketByteBuf, ClearLogsPayload> CODEC =
            PacketCodec.unit(new ClearLogsPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
