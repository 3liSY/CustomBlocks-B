/**
 * HudSyncPayload.java
 *
 * Responsibility: Server-to-client packet that syncs the block index needed by the
 * client-side HUD renderer. Contains a compact JSON map of slotIndex → "id:DisplayName".
 * Sent once on player join so the HUD can identify looked-at custom blocks.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by: CustomBlocksMod (sends on join), ClientSlotCache (receives)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HudSyncPayload(String indexJson) implements CustomPayload {

    public static final CustomPayload.Id<HudSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "hud_sync"));

    public static final PacketCodec<PacketByteBuf, HudSyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, HudSyncPayload::indexJson, HudSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
