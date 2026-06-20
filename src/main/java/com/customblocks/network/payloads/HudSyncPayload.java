/**
 * HudSyncPayload.java
 *
 * Responsibility: Server-to-client packet that syncs the block index needed by the
 * client-side HUD renderer. Contains a compact JSON map of slotIndex → a structured object
 * {id, name, cat, glow, hard, sound, shape, pass} (Group 27 §G27.4; see HudSync /
 * ClientSlotCache). Sent on join + after slot mutations so the HUD can identify looked-at
 * custom blocks and render the sync-expansion bricks. The String codec is unchanged (1 MB).
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

    // PacketCodecs.STRING = string(32767) — blows up once the server has hundreds of blocks.
    // 1 << 20 = 1 MB, matching Fabric's custom-payload size cap.
    public static final PacketCodec<PacketByteBuf, HudSyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.string(1 << 20), HudSyncPayload::indexJson, HudSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
