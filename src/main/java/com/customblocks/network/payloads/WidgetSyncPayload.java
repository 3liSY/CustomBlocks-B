/**
 * WidgetSyncPayload.java — GROUP 03 §G03-2.
 *
 * The ONE server→client signal feeding all seven HUD widgets: active tool mode, macro-recording state,
 * which blocks are locked, a pending incident + its code, the player's 3 favourites, and the op-forced
 * widget set.
 *
 * One payload rather than seven: the widgets are a single overlay that redraws together, the data is
 * tiny, and every one of these facts changes rarely (a macro starts, a block gets locked). Seven packets
 * would be seven registration sites, seven receivers and seven chances for one to fall out of sync.
 *
 * The server stays authoritative (CLAUDE.md §5.8) — the client renders what it is told and computes none
 * of it. In particular the op-forced set is server-only: the whole point is that a player cannot hide a
 * forced widget, which would be trivially defeatable if the client decided it.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API.
 * Called by:  WidgetSync (server, sends), CustomBlocksClient (receives → WidgetSignals).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WidgetSyncPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<WidgetSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "widget_sync"));

    // Locked-id lists can get long on a big server, so give it real room rather than the 32 KB default.
    public static final PacketCodec<PacketByteBuf, WidgetSyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.string(1 << 20), WidgetSyncPayload::json, WidgetSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
