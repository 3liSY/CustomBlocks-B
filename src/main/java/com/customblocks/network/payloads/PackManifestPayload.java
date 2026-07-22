/**
 * PackManifestPayload.java
 *
 * Responsibility: Server-to-client. Opens one dedicated pack-sync SESSION ({@code session} id, §G5) and
 * ships that session's pack fingerprint list as a GZIP blob of "path\tsha256\tsize\n" lines (Group 05 §G),
 * so the modded client can diff it against its on-disk loose pack, size its disk preflight, and request
 * only the missing/changed files. GZIP keeps a large manifest well under the custom-payload size cap.
 *
 * The {@code session} id ties every later PackFile/PackDone the server sends and every PackRequest/PackAck/
 * PackApplied the client returns to THIS manifest, so a superseded transfer (a resolution change or a new
 * generation mid-sync) can be ignored by id instead of corrupting the current one.
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

public record PackManifestPayload(long session, byte[] gz) implements CustomPayload {

    public static final CustomPayload.Id<PackManifestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_manifest"));

    public static final PacketCodec<PacketByteBuf, PackManifestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG,   PackManifestPayload::session,
            PacketCodecs.BYTE_ARRAY, PackManifestPayload::gz,
            PackManifestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
