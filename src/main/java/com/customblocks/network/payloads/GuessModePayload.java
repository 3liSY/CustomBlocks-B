/**
 * GuessModePayload.java — Group 30 (Guess Mode).
 *
 * Responsibility: server→client packet carrying the set of players currently in guess mode plus, per
 * player, the flagged blocks the holder's own client needs (all-mode flag + the flagged slots and their
 * per-round look slots), and the global default look slot. Broadcast to EVERY client on any change + on
 * join, mirroring {@link com.customblocks.network.HudSync}. Watchers use it to decide the centered pose (a
 * player holding a block flagged FOR THEM); the LOCAL client additionally reads its own record to blank the
 * name (→ "???") + swap in the disguise cube (v3 Phase 1 look, or the bundled "?") for any flagged block.
 *
 * The payload is a compact JSON object (v3 Phase 1):
 *   { "def": &lt;defaultLookSlot|-1&gt;, "p": { "&lt;uuid&gt;": { "all": bool, "looks": { "&lt;slot&gt;": &lt;lookSlot|-1&gt; } } } }
 * Presence of a uuid = that player is in guess mode. {@code all} = blind every custom block they see;
 * {@code looks} keys = flagged slot indices, values = the look slot to draw (-1 = use default → bundled "?").
 * {@code def} = the global default look slot. Reuses the same String codec (1 MB cap) as HudSyncPayload.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API
 * Called by:  GuessSync (build/send), CustomBlocksClient (receive → ClientGuessState).
 */
package com.customblocks.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GuessModePayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<GuessModePayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "guess_mode"));

    public static final PacketCodec<PacketByteBuf, GuessModePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.string(1 << 20), GuessModePayload::json, GuessModePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
