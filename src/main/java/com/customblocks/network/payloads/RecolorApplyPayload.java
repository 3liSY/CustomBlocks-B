/**
 * RecolorApplyPayload.java — Group 10 (live recolour slider) + Group 27 §G27.7 §C3 tone tools.
 *
 * Responsibility: client→server packet sent when the player clicks "Apply" in the live recolour slider.
 * Carries the block id, the HSL shift (hue degrees, saturation factor, lightness factor), and the §C3
 * tone tools (temperature, contrast, shadow lift, highlight drop, one-tap filter id). The server bakes
 * it onto the block (ColorToolService.applyRecolor) — the client only ever previews; the server stays
 * authoritative (CLAUDE.md §5.8). A manual codec is used because the field count (9) exceeds the
 * PacketCodec.tuple overloads.
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/RecolorSliderScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RecolorApplyPayload(String id, float hue, float sat, float light,
                                  float temp, float contrast, float shadowLift, float highlightDrop, int filter)
        implements CustomPayload {

    public static final CustomPayload.Id<RecolorApplyPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "recolor_apply"));

    public static final PacketCodec<PacketByteBuf, RecolorApplyPayload> CODEC = PacketCodec.ofStatic(
            (buf, v) -> {
                buf.writeString(v.id);
                buf.writeFloat(v.hue); buf.writeFloat(v.sat); buf.writeFloat(v.light);
                buf.writeFloat(v.temp); buf.writeFloat(v.contrast);
                buf.writeFloat(v.shadowLift); buf.writeFloat(v.highlightDrop);
                buf.writeVarInt(v.filter);
            },
            buf -> new RecolorApplyPayload(
                    buf.readString(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
