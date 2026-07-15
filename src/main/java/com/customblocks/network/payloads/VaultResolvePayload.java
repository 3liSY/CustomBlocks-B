/**
 * VaultResolvePayload.java — Group 20 §S2 (Cloud Vault conflict screen).
 *
 * Client→server. Sent when the player picks an action on {@link
 * com.customblocks.client.gui.VaultConflictScreen}. The server re-fetches the ZIP by {@code code}
 * (stateless — no pending buffer), then applies the chosen resolution. Server stays authoritative
 * (CLAUDE.md §5.8): the client only states intent + the typed id; the server re-validates everything.
 *
 *   code    — the share code from {@link VaultConflictPayload}; used to re-download the block ZIP.
 *   action  — OVERRIDE (incoming takes the id, local deleted) / KEEP_BOTH (incoming under the typed id,
 *             local untouched) / RENAME_MINE (local re-id'd to the typed id, incoming takes the original).
 *   typedId — the id from the editable box; used by KEEP_BOTH (the incoming's new id) and RENAME_MINE
 *             (the local block's new id). Ignored by OVERRIDE.
 *
 * Registered playC2S in CustomBlocksMod, sent by client/gui/VaultConflictScreen, handled by command/handlers/VaultConflict.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VaultResolvePayload(String code, String action, String typedId) implements CustomPayload {

    /** Action values (kept in sync with command/handlers/VaultConflict). */
    public static final String OVERRIDE = "OVERRIDE";
    public static final String KEEP_BOTH = "KEEP_BOTH";
    public static final String RENAME_MINE = "RENAME_MINE";

    public static final CustomPayload.Id<VaultResolvePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "vault_resolve"));

    public static final PacketCodec<PacketByteBuf, VaultResolvePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, VaultResolvePayload::code,
            PacketCodecs.STRING, VaultResolvePayload::action,
            PacketCodecs.STRING, VaultResolvePayload::typedId,
            VaultResolvePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
