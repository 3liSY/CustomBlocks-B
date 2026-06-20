/**
 * RegenPackPayload.java
 *
 * Responsibility: Server-to-client signal telling a MODDED client to (re)generate its local
 * resource pack and silently reload, instead of downloading the HTTP pack (Group 05 fix,
 * 2026-06-15). Carries the server's current pack hash so the client can skip a regen when it
 * already has that exact pack applied. Vanilla clients never receive this (they can't decode
 * the channel) — they keep getting the HTTP push.
 *
 * Registered in CustomBlocksMod (server S2C), received in CustomBlocksClient (client) which
 * calls ResourcePackGenerator.regenerate(client, hash).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: ResourcePackServer.sendToPlayer (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RegenPackPayload(String hash) implements CustomPayload {

    public static final CustomPayload.Id<RegenPackPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "regen_pack"));

    public static final PacketCodec<PacketByteBuf, RegenPackPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, RegenPackPayload::hash, RegenPackPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
