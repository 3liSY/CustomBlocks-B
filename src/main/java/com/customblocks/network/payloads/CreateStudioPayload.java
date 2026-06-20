/**
 * CreateStudioPayload.java — Group 27 §G27.6 (Block Creation Studio).
 *
 * Client→server packet sent when the player clicks "Create & Publish". Carries:
 *   id    — the block id (server normalises + validates).
 *   name  — display name ("" = default to id).
 *   url   — image/GIF URL ("" = none; ignored when attrs carries a colour).
 *   attrs — compact "key=value;…" string of the rest (shape/glow/hardness/sound/passable/category/color),
 *           parsed by CreationStudioBridge. A delimited string keeps the codec to 4 fields (PacketCodec.tuple)
 *           and makes adding a setting later a content-only change — no codec/registration churn.
 *
 * The SERVER creates the block via the SAME CreationCommands rail the /cb create CLI uses, then applies
 * the attrs through the existing SlotManager setters (setShape/setGlow/…). Authoritative server (CLAUDE.md §5.8).
 *
 * Registered playC2S in CustomBlocksMod, sent by client/gui/BlockCreationStudioScreen.
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CreateStudioPayload(String id, String name, String url, String attrs) implements CustomPayload {

    public static final CustomPayload.Id<CreateStudioPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "create_studio"));

    public static final PacketCodec<PacketByteBuf, CreateStudioPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, CreateStudioPayload::id,
                    PacketCodecs.STRING, CreateStudioPayload::name,
                    PacketCodecs.STRING, CreateStudioPayload::url,
                    PacketCodecs.STRING, CreateStudioPayload::attrs,
                    CreateStudioPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
