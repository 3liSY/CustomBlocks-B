/**
 * ArabicLabelsPayload.java — Group 13 / O6.
 *
 * Server-to-client packet carrying the three live Arabic join form labels (initial / medial / final).
 * Sent on player join and whenever /cb config arabicforms changes a value, so a label edit updates
 * every client's held / placed / stored block names instantly with no resource-pack reload. The
 * client stores them in ArabicLabels and resets to defaults on disconnect (so another server's
 * labels never bleed across), exactly like SilentPackPayload / SilentPackState (Group 05).
 *
 * Registered in CustomBlocksMod (server S2C), received in CustomBlocksClient (client).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: CustomBlocksMod / ArabicFormCommands (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ArabicLabelsPayload(String ini, String mid, String fin) implements CustomPayload {

    public static final CustomPayload.Id<ArabicLabelsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "arabic_labels"));

    public static final PacketCodec<PacketByteBuf, ArabicLabelsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ArabicLabelsPayload::ini,
            PacketCodecs.STRING, ArabicLabelsPayload::mid,
            PacketCodecs.STRING, ArabicLabelsPayload::fin,
            ArabicLabelsPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
