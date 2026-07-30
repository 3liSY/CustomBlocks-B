/**
 * ImportProgressPayload.java — server→client progress for a folder-import run (Group 12 §B rule 18).
 *
 * The owner chose to REUSE the pack-sync top-center panel rather than grow a second progress look, and
 * that panel is client-side, so the server needs a way to tell it how far along an import is. This is
 * that message and nothing more: a done/total pair plus a short label.
 *
 * Deliberately additive — it feeds {@code SyncProgressOverlay} alongside the pack-sync counters and does
 * not touch the §G transport those counters come from.
 *
 * {@code total <= 0} means "the run is over, hide the panel".
 *
 * Registered in PayloadRegistrar (S2C), received in CustomBlocksClient.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  network/ImportProgress (send), CustomBlocksClient (receive)
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ImportProgressPayload(int done, int total, String label) implements CustomPayload {

    public static final CustomPayload.Id<ImportProgressPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "import_progress"));

    public static final PacketCodec<PacketByteBuf, ImportProgressPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,   ImportProgressPayload::done,
            PacketCodecs.VAR_INT,   ImportProgressPayload::total,
            PacketCodecs.STRING,    ImportProgressPayload::label,
            ImportProgressPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
