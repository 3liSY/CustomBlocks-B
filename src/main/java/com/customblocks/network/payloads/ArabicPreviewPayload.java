/**
 * ArabicPreviewPayload.java — Group 13 (live Arabic word preview screen).
 *
 * Responsibility: client→server packet from the live preview screen. Two actions:
 *   action 0 (COLOUR) — the player picked a new letter/background colour on the screen; the server
 *                       re-renders the throwaway preview texture (pack-free) and refreshes the screen.
 *   action 1 (CREATE) — the player clicked Create; the server makes the real word block.
 * Colours are opaque ARGB ints. The server stays authoritative (CLAUDE.md §5.8): the client only
 * previews + requests; the block is created server-side.
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/ArabicPreviewScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ArabicPreviewPayload(int action, int letterArgb, int bgArgb) implements CustomPayload {

    public static final int ACTION_COLOUR = 0;
    public static final int ACTION_CREATE = 1;

    public static final CustomPayload.Id<ArabicPreviewPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "arabic_preview"));

    public static final PacketCodec<PacketByteBuf, ArabicPreviewPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, ArabicPreviewPayload::action,
                    PacketCodecs.INTEGER, ArabicPreviewPayload::letterArgb,
                    PacketCodecs.INTEGER, ArabicPreviewPayload::bgArgb,
                    ArabicPreviewPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
