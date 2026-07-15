/**
 * VaultConflictPayload.java — Group 20 §S2 (Cloud Vault conflict screen).
 *
 * Server→client. Sent when {@code /cb vault download <code>} restores a block whose id already exists
 * locally: instead of a "delete it first" chat line, the client opens {@link
 * com.customblocks.client.gui.VaultConflictScreen} showing BOTH blocks so the player picks how to resolve it.
 *
 *   code        — the share code; echoed back in {@link VaultResolvePayload} so the server can re-fetch
 *                 the ZIP statelessly (no per-player pending buffer to leak).
 *   incomingId  — the clashing id (the incoming block's own id).
 *   meta        — compact "you=<stats>|in=<stats>|suggest=<freeId>" string. Each <stats> is the codec's
 *                 "key=val;…" form (incl. frames= and, for "you", idx= so the client can read the local
 *                 block's frames back from the active resource pack). A delimited string keeps the codec
 *                 to 4 fields and makes adding a field later a content-only change (same idea as
 *                 CreateStudioPayload.attrs).
 *   preview     — a small vertical 64px frame strip PNG of the INCOMING block (the local one is read from
 *                 the pack), so the incoming texture shows live with no resource-pack rebuild.
 *
 * Registered playS2C in CustomBlocksMod, sent by command/handlers/VaultConflict, received by client/CustomBlocksClient.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VaultConflictPayload(String code, String incomingId, String meta, byte[] preview) implements CustomPayload {

    public static final CustomPayload.Id<VaultConflictPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "vault_conflict"));

    public static final PacketCodec<PacketByteBuf, VaultConflictPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,     VaultConflictPayload::code,
            PacketCodecs.STRING,     VaultConflictPayload::incomingId,
            PacketCodecs.STRING,     VaultConflictPayload::meta,
            PacketCodecs.BYTE_ARRAY, VaultConflictPayload::preview,
            VaultConflictPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
