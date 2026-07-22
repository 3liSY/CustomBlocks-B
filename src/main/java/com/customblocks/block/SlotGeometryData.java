/**
 * SlotGeometryData.java — Group 08 §B/§J.
 *
 * The two pieces of slot data the GEOMETRY path needs — the block's shape and its per-face quarter-turns
 * — resolved for whichever side is asking. Split out of {@link SlotBlock} so that class stays under the
 * no-monolith size limit, the same way {@link DirectionalPlacement} was, and because these two now travel
 * together: §B builds both the collision boxes and the drawn mesh from them.
 *
 * Server JVM, singleplayer, and a LAN host read the live in-process data ({@link SlotManager} /
 * {@link com.customblocks.core.FaceRotations}). A REMOTE client's SlotManager is empty (G05 lesson), so it
 * skips it and reads the value synced through ClientSlotCache; the client entrypoint installs the two
 * resolvers below, which stay null on a server JVM.
 *
 * Why face rotations need a seam at all: before §B they existed only as {@code uv}+{@code rotation} baked
 * into the pack's model JSON. §B draws non-full shapes on the client from {@link BlockShapes} boxes and
 * never reads that JSON, so without this a shaped block would silently lose its G06 §G rotations on a
 * dedicated server.
 *
 * Depends on: SlotManager + SlotData (live truth), FaceRotations (live truth), SlotBlock.CLIENT_REMOTE_SESSION.
 * Called by:  SlotBlock (outline/collision), StairConnection (the "is a stair" gate),
 *             DirectionalSlotModel (the runtime mesh), CustomBlocksClient (installs the resolvers).
 */
package com.customblocks.block;

import com.customblocks.core.FaceRotations;
import com.customblocks.core.SlotData;
import com.customblocks.core.SlotManager;

import java.util.function.IntFunction;

public final class SlotGeometryData {

    private SlotGeometryData() {} // static-only

    /** Client seam for the block SHAPE (slotIndex → shape name, or null). Null on a server JVM. */
    public static volatile IntFunction<String> CLIENT_SHAPE_RESOLVER = null;

    /** Client seam for the PACKED face rotations (slotIndex → packed value, or null). Null on a server JVM. */
    public static volatile IntFunction<Integer> CLIENT_FACEROT_RESOLVER = null;

    /**
     * A slot's shape, for outline/collision and for the runtime mesh. On a dedicated server the client's
     * SlotManager is empty, so without the synced fallback the hitbox stayed a full cube even when the
     * block was a carpet or a slab.
     */
    public static String resolveShape(int slotIndex, String slotKey) {
        if (!SlotBlock.CLIENT_REMOTE_SESSION) {
            SlotData d = SlotManager.getBySlot(slotKey);
            if (d != null) return d.shape();
        }
        IntFunction<String> resolver = CLIENT_SHAPE_RESOLVER;
        if (resolver != null) {
            String cached = resolver.apply(slotIndex);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        return SlotData.DEFAULT_SHAPE;
    }

    /** A slot's six face quarter-turns, packed 2-bits-per-face in Direction order (see FaceRotations). */
    public static int resolveFaceRot(int slotIndex) {
        if (!SlotBlock.CLIENT_REMOTE_SESSION) return FaceRotations.packed(slotIndex);
        IntFunction<Integer> resolver = CLIENT_FACEROT_RESOLVER;
        if (resolver != null) {
            Integer packed = resolver.apply(slotIndex);
            if (packed != null) return packed;
        }
        return 0;
    }
}
