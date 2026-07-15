/**
 * VersionHandshakePayload.java
 *
 * Responsibility: Server-to-client packet carrying THIS server's CustomBlocks version + the info a
 * client needs to auto-update itself (GROUP_20 §CS3 / §K). Sent once on join. The client compares
 * the version against its own (read from fabric.mod.json); if the client is older AND
 * {@code autoUpdateEnabled} is true it downloads {@code downloadUrl}, verifies {@code sha256}, and
 * swaps its jar. If auto-update is off, or the client is newer, it only shows a warning toast.
 *
 * {@code downloadUrl}/{@code sha256} are empty strings when the server has no packaged jar to serve
 * (e.g. a dev run) — the client treats empty as "no download available" and falls back to a toast.
 *
 * Registered S2C in CustomBlocksMod, received in CustomBlocksClient → UpdateController.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: CustomBlocksMod JOIN handler (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VersionHandshakePayload(String serverVersion, String downloadUrl, String sha256,
                                      boolean autoUpdateEnabled) implements CustomPayload {

    public static final CustomPayload.Id<VersionHandshakePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "version_handshake"));

    public static final PacketCodec<PacketByteBuf, VersionHandshakePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, VersionHandshakePayload::serverVersion,
            PacketCodecs.STRING, VersionHandshakePayload::downloadUrl,
            PacketCodecs.STRING, VersionHandshakePayload::sha256,
            PacketCodecs.BOOL,   VersionHandshakePayload::autoUpdateEnabled,
            VersionHandshakePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
