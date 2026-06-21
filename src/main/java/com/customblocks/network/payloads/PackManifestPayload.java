/**
 * PackManifestPayload.java
 *
 * Responsibility: Server-to-client. Ships the dedicated server's pack fingerprint list
 * (path -> sha1) as a GZIP blob of "path\tsha1\n" lines, so the modded client can diff it
 * against its on-disk loose pack and request only the missing/changed files (Group 05,
 * remote/dedicated fix). GZIP keeps a ~4000-file manifest well under the custom-payload size
 * cap (sha1 hex + repeated path prefixes compress hard).
 *
 * Sent only when {@code server.isDedicated()} — the integrated host keeps RegenPackPayload.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  PackSyncService.beginSync (send), ClientPackReceiver (receive).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackManifestPayload(byte[] gz) implements CustomPayload {

    public static final CustomPayload.Id<PackManifestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_manifest"));

    public static final PacketCodec<PacketByteBuf, PackManifestPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BYTE_ARRAY, PackManifestPayload::gz, PackManifestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
