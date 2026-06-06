/**
 * OpenGuiPayload.java
 *
 * Responsibility: Server-to-client packet that tells the client which GUI screen
 * to open, plus an optional JSON data string the screen can read.
 *
 * mode  — GuiMode.id value (int) indicating which screen to open.
 * data  — any extra data the screen needs (e.g. a block ID), or "" if unused.
 *
 * Registered in CustomBlocksMod (server), received in CustomBlocksClient (client).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: GuiCommands (sends), CustomBlocksClient (receives)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenGuiPayload(int mode, String data) implements CustomPayload {

    public static final CustomPayload.Id<OpenGuiPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "open_gui"));

    public static final PacketCodec<PacketByteBuf, OpenGuiPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, OpenGuiPayload::mode,
                    PacketCodecs.STRING,  OpenGuiPayload::data,
                    OpenGuiPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
