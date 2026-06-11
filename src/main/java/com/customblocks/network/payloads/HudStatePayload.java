/**
 * HudStatePayload.java
 *
 * Responsibility: Server-to-client packet that tells the client whether the HUD
 * overlay should be shown or hidden. Sent by /cb edithud so the toggle actually
 * reaches the renderer (HudConfig.visible lives client-side; toggling the server
 * config alone has no effect on it).
 *
 * Depends on: CustomPayload, PacketCodecs
 * Called by: GuiCommands.editHud(), CustomBlocksClient (registers receiver)
 */
package com.customblocks.network.payloads;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HudStatePayload(boolean enabled) implements CustomPayload {

    public static final CustomPayload.Id<HudStatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "hud_state"));

    public static final PacketCodec<ByteBuf, HudStatePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, HudStatePayload::enabled, HudStatePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
