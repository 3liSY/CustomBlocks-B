/**
 * SauceHitPayload.java — Group 32 (Explosive Tomato) Phase E, E8 (screen overlay).
 *
 * Server-to-client one-shot: "you were caught in a tomato blast, at this intensity (0..1)". The server sends it
 * to every player inside the blast radius (see {@code SauceVisuals.signalDirectHits}); the client starts/refreshes
 * the screen-edge sauce drip overlay ({@code SauceScreenOverlay}), which paints the EDGES of the screen — centre
 * stays clear — and dries fast. Intensity scales how heavy the overlay starts (closer = heavier).
 *
 * Depends on: CustomPayload, PacketCodecs
 * Called by: SauceVisuals.signalDirectHits (send), PayloadRegistrar (register), CustomBlocksClient (receiver)
 */
package com.customblocks.network.payloads;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SauceHitPayload(float intensity) implements CustomPayload {

    public static final CustomPayload.Id<SauceHitPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "sauce_hit"));

    public static final PacketCodec<ByteBuf, SauceHitPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.FLOAT, SauceHitPayload::intensity, SauceHitPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
