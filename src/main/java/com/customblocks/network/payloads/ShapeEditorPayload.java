/**
 * ShapeEditorPayload.java — Group 27 §G27.5 (Shape Editor screen).
 *
 * Client→server packet sent when the player clicks "Save" in the Shape Editor. Carries the block id
 * plus the chosen shape name (one of BlockShapes.names()). The SERVER applies it through the same
 * ShapeCommands rail the /cb setshape command uses (validate → SlotManager.setShape → undo → pack
 * rebuild) — the client only previews; the server stays authoritative (CLAUDE.md §5.8).
 *
 * Registered playC2S in CustomBlocksMod (server receiver), sent by client/gui/ShapeEditorScreen.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShapeEditorPayload(String id, String shape) implements CustomPayload {

    public static final CustomPayload.Id<ShapeEditorPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "shape_editor"));

    public static final PacketCodec<PacketByteBuf, ShapeEditorPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, ShapeEditorPayload::id,
                    PacketCodecs.STRING, ShapeEditorPayload::shape,
                    ShapeEditorPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
