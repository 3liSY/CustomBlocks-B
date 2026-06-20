/**
 * StudioEditPayload.java — Group 14 Phase 2 (Block Creation Studio edit-load).
 *
 * Server→client packet that opens the Block Creation Studio on an EXISTING block, with its current
 * state loaded. Carries:
 *   index — the block's slot index (so the client can read its frame-strip back from the pack).
 *   id    — the block's custom id.
 *   name  — the block's display name.
 *   attrs — a compact "key=value;…" string of the rest (shape/glow/hardness/sound/passable/color/
 *           category + the animation numbers anim=… and animtimes=…), parsed by the studio screen.
 *           A delimited string keeps the codec to 4 fields (PacketCodec.tuple), matching CreateStudioPayload.
 *
 * Registered playS2C in CustomBlocksMod, sent by CreationStudioBridge.openStudioEdit, received in
 * CustomBlocksClient (opens BlockCreationStudioScreen in edit mode).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StudioEditPayload(int index, String id, String name, String attrs) implements CustomPayload {

    public static final CustomPayload.Id<StudioEditPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "studio_edit"));

    public static final PacketCodec<PacketByteBuf, StudioEditPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, StudioEditPayload::index,
                    PacketCodecs.STRING,  StudioEditPayload::id,
                    PacketCodecs.STRING,  StudioEditPayload::name,
                    PacketCodecs.STRING,  StudioEditPayload::attrs,
                    StudioEditPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
