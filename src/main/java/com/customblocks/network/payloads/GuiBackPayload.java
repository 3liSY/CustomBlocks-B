/**
 * GuiBackPayload.java — Group 10 (Coloring), client→server "go back one menu".
 *
 * Responsibility: sent when the player cancels or presses Esc on one of the colour client screens
 * (live recolour slider, screen eyedrop). The server reopens whatever chest menu they came from
 * (Nav.current) so cancelling drops them back into the GUI a step ago instead of out to the world.
 * Carries no data — it's a pure "reopen my previous menu" signal; the server stays authoritative.
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/{RecolorSliderScreen,
 * EyedropScreen}.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GuiBackPayload() implements CustomPayload {

    public static final CustomPayload.Id<GuiBackPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "gui_back"));

    public static final PacketCodec<PacketByteBuf, GuiBackPayload> CODEC =
            PacketCodec.unit(new GuiBackPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
