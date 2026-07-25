/**
 * PlacedMaskPayload.java — Group 30 (Guess Mode) · §H Placed Mask Mode.
 *
 * Responsibility: server→client packet carrying the block positions the RECEIVING player must see as the
 * bundled "?" cube. The server already filtered the runner's own placements out (PlacedMaskStore.feedFor),
 * so this payload is per-viewer and the client never has to be trusted to hide the answer from itself.
 *
 * Three ops, one wire shape ({@code op, dimCount, [dimId, posCount, packed BlockPos…]}):
 *   • {@link #OP_FULL} — replace everything the client knows (join, toggle, dimension-wide change);
 *   • {@link #OP_ADD}  — one position just became masked (a runner placed a block);
 *   • {@link #OP_DEL}  — one position stopped being masked (that block was broken).
 * Deltas matter: a build can hold thousands of masked positions, and re-sending the whole set on every
 * single placement would be quadratic. Positions are packed {@link net.minecraft.util.math.BlockPos#asLong}
 * and grouped by dimension id, so the client can hold every dimension at once and never needs a re-send when
 * a player walks through a portal.
 *
 * Binary (not the String-JSON codec the other G30 payloads use) precisely because of that size: 8 bytes a
 * position instead of ~20 characters.
 *
 * Depends on: Minecraft CustomPayload + PacketCodec API, fastutil LongList.
 * Called by:  GuessSync (build/send), CustomBlocksClient (receive → ClientPlacedMask).
 */
package com.customblocks.network.payloads;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public record PlacedMaskPayload(byte op, Map<String, LongList> dims) implements CustomPayload {

    /** Replace the client's whole picture with this feed. */
    public static final byte OP_FULL = 0;
    /** Add the carried positions to what the client already has. */
    public static final byte OP_ADD  = 1;
    /** Remove the carried positions from what the client already has. */
    public static final byte OP_DEL  = 2;

    /** Hard ceiling on one packet's positions — comfortably above PlacedMaskStore.MAX_POSITIONS per runner. */
    private static final int MAX_POSITIONS = 200_000;

    public static final CustomPayload.Id<PlacedMaskPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customblocks", "placed_mask"));

    public static final PacketCodec<PacketByteBuf, PlacedMaskPayload> CODEC =
            CustomPayload.codecOf(PlacedMaskPayload::write, PlacedMaskPayload::new);

    /** One-position convenience for the ADD / DEL deltas. */
    public static PlacedMaskPayload one(byte op, String dimId, long pos) {
        LongList list = new LongArrayList(1);
        list.add(pos);
        return new PlacedMaskPayload(op, Map.of(dimId, list));
    }

    private PlacedMaskPayload(PacketByteBuf buf) {
        this(buf.readByte(), read(buf));
    }

    private static Map<String, LongList> read(PacketByteBuf buf) {
        Map<String, LongList> out = new LinkedHashMap<>();
        int dimCount = buf.readVarInt();
        for (int i = 0; i < dimCount; i++) {
            String dim = buf.readString(256);
            int n = Math.min(buf.readVarInt(), MAX_POSITIONS);
            LongList list = new LongArrayList(Math.min(n, 4096));
            for (int j = 0; j < n; j++) list.add(buf.readLong());
            out.put(dim, list);
        }
        return out;
    }

    private void write(PacketByteBuf buf) {
        buf.writeByte(op);
        buf.writeVarInt(dims.size());
        for (Map.Entry<String, LongList> e : dims.entrySet()) {
            buf.writeString(e.getKey(), 256);
            LongList list = e.getValue();
            buf.writeVarInt(list.size());
            for (int i = 0; i < list.size(); i++) buf.writeLong(list.getLong(i));
        }
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
