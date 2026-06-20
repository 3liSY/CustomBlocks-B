/**
 * StudioSavePayload.java — Group 14 Phase 2 (Block Creation Studio "Save changes").
 *
 * Client→server packet sent when the studio is in EDIT mode and the player clicks "Save changes".
 * Group 14 "edit everything": the studio is a full editor, so this carries the whole block, not just
 * the animation knobs. Fields:
 *   origId — the id the block had when the studio opened (the lookup key; never changes mid-edit).
 *   id     — the (possibly RENAMED) id; when it differs from origId the server does a safe SlotManager.reId
 *            (placed blocks are the registry slot_N, so a rename never orphans them).
 *   name   — its (possibly changed) display name.
 *   url    — a NEW image/GIF url to re-skin the block with ("" = keep the current texture). Kept as its
 *            own field (not folded into attrs) because a url can contain ';' and '=' which the attrs
 *            parser splits on. A new url RE-BAKES the slot: animated→strip+fresh AnimData, static→clear anim.
 *   attrs — a compact "key=value;…" string: shape/glow/hardness/sound/passable/category/color plus the
 *           animation KNOBS anim=… . When NO new url is sent the knobs merge onto the EXISTING AnimData so
 *           the per-frame source timing is never wiped (designs out the old "timing lost on save" bug).
 *
 * Registered playC2S in CustomBlocksMod, sent by BlockCreationStudioScreen, applied by
 * CreationStudioBridge.saveFromStudio. Server-authoritative (CLAUDE.md §5.8).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StudioSavePayload(String origId, String id, String name, String url, String attrs) implements CustomPayload {

    public static final CustomPayload.Id<StudioSavePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "studio_save"));

    public static final PacketCodec<PacketByteBuf, StudioSavePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, StudioSavePayload::origId,
                    PacketCodecs.STRING, StudioSavePayload::id,
                    PacketCodecs.STRING, StudioSavePayload::name,
                    PacketCodecs.STRING, StudioSavePayload::url,
                    PacketCodecs.STRING, StudioSavePayload::attrs,
                    StudioSavePayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
