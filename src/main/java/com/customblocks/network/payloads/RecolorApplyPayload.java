/**
 * RecolorApplyPayload.java — Group 10 (live recolour slider).
 *
 * Responsibility: client→server packet sent when the player clicks "Apply" in the live recolour
 * slider. Carries the block id plus the chosen HSL shift (hue degrees, saturation factor, lightness
 * factor). The server bakes it onto the block (ColorToolService.applyRecolor) — the client only ever
 * previews; the server stays authoritative (CLAUDE.md §5.8).
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/RecolorSliderScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RecolorApplyPayload(String id, float hue, float sat, float light) implements CustomPayload {

    public static final CustomPayload.Id<RecolorApplyPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "recolor_apply"));

    public static final PacketCodec<PacketByteBuf, RecolorApplyPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, RecolorApplyPayload::id,
                    PacketCodecs.FLOAT,  RecolorApplyPayload::hue,
                    PacketCodecs.FLOAT,  RecolorApplyPayload::sat,
                    PacketCodecs.FLOAT,  RecolorApplyPayload::light,
                    RecolorApplyPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
