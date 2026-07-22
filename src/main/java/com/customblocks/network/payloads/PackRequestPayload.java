/**
 * PackRequestPayload.java
 *
 * Responsibility: Client-to-server. After diffing the manifest, the modded client asks the dedicated
 * server for exactly the files it is missing or that changed — a GZIP blob of newline-separated
 * pack-relative paths, tagged with the {@code session} id it belongs to (Group 05 §G). The server
 * validates the session, bounds-checks and gunzips the list, queues only manifest-present paths, and
 * replies with one credit-paced PackFilePayload stream then PackDonePayload. The client may send a
 * further request in the same session to re-fetch a file that failed SHA-256 (§G7 bounded retry).
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  ClientPackReceiver (send), PackSyncService.onRequest (receive, queues the files).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PackRequestPayload(long session, byte[] gz) implements CustomPayload {

    public static final CustomPayload.Id<PackRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "pack_request"));

    public static final PacketCodec<PacketByteBuf, PackRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG,   PackRequestPayload::session,
            PacketCodecs.BYTE_ARRAY, PackRequestPayload::gz,
            PackRequestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
