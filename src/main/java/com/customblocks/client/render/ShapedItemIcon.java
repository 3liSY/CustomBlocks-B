/**
 * ShapedItemIcon.java — Group 08 §B (shaped item icon, pack-INDEPENDENT). CLIENT-ONLY.
 *
 * Draws a slot item's icon (hotbar / inventory / hand / dropped / frame) in the slot's CURRENT shape,
 * WITHOUT depending on the resource pack having routed the item to {@code builtin/entity}.
 *
 * Why this exists (the dedicated-server bug, 2026-07-22): the original shaped icon lived only in
 * {@link SlotItemRenderer}, a Fabric {@code DynamicItemRenderer} that fires ONLY when the item's baked
 * model is {@code builtin/entity} — and that model comes from the pack, which on a DEDICATED server is
 * authored by the SERVER. When that routing doesn't reach the client (the exact fragility Group 30 hit
 * and fixed with {@link com.customblocks.mixin.ItemDisguiseMixin}), the DynamicItemRenderer never fires
 * and the item falls back to its plain baked cube — a shaped block showed a FULL CUBE in hand/inventory
 * on a server, while the PLACED block (drawn by the pack-independent {@link DirectionalSlotModel} wrap)
 * still showed the shape. Singleplayer wrote its own pack so it worked there; only remote broke.
 *
 * The fix mirrors Group 30: {@link com.customblocks.mixin.ShapedItemMixin} hooks {@code
 * ItemRenderer.renderItem} at HEAD and calls {@link #tryRender}, which draws the shape client-side from
 * the same {@link com.customblocks.block.BlockShapes} boxes the placed mesh + hitbox use and cancels the
 * vanilla draw. No pack byte is involved, so it can never no-op on a dedicated server.
 *
 * Scope gate — this only takes over the PLAIN shaped case (the one SlotItemRenderer handled in SP):
 *   • the item is a {@link SlotBlock} block item, and NOT guess-disguised (that path stays G30's),
 *   • the slot is not animated (animated icons keep their off-atlas frame path in SlotItemRenderer),
 *   • the shape is non-full and non-cross (a full cube / cross needs no shape draw),
 *   • the slot has NO face rotations and NO per-face textures — a painted/rotated shaped slot keeps its
 *     atlas cube icon (the documented §B trade; its faces can't be drawn from the single base texture).
 * Everything else returns false → vanilla renders normally (full cube, cross billboard, painted cube, or
 * the animated/guess paths through SlotItemRenderer).
 *
 * Depends on: SlotBlock (slot index), SlotGeometryData (synced shape + face rotations), StaticFrameCache
 *             (base texture + the painted-slot gate {@code hasPerFace}), SlotShapeMesh + ShapeIconMesh
 *             (boxes + draw), AnimFrameCache (animated gate), GuessDisguise (guess gate).
 * Called by:  ShapedItemMixin (ItemRenderer.renderItem HEAD).
 */
package com.customblocks.client.render;

import com.customblocks.block.BlockShapes;
import com.customblocks.block.SlotBlock;
import com.customblocks.block.SlotGeometryData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class ShapedItemIcon {

    private ShapedItemIcon() {} // static-only

    // ── TEMP DEBUG (remove after mp diagnosis) ──
    private static final java.util.Set<Integer> DEBUG_SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Reset the once-per-slot debug gate so a resource reload re-captures each slot's verdict on its next draw. */
    public static void resetDebugSeen() { DEBUG_SEEN.clear(); }
    private static void dbgLog(int slot, ModelTransformationMode mode, String verdict, String shape, int faceRot,
                               boolean perFace, boolean boxesOk, boolean worldTex, boolean fallbackTex) {
        com.customblocks.CustomBlocksMod.LOGGER.info(
            "[SHAPED-ICON dbg] slot={} remote={} mode={} -> {} | shape={} faceRot={} perFace={} boxesOk={} worldTex={} fallbackTex={}",
            slot, com.customblocks.block.SlotBlock.CLIENT_REMOTE_SESSION, mode, verdict,
            shape, faceRot, perFace, boxesOk, worldTex, fallbackTex);
    }

    /**
     * Draw {@code stack}'s icon in its slot shape if this is the plain shaped case; return true when it
     * drew (the caller must then cancel the vanilla item render). Framing matches vanilla's own item
     * pipeline (apply the model's display transform for {@code mode}, then translate −0.5) so the shape
     * lands exactly where the normal block icon would, in every mode.
     */
    public static boolean tryRender(ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
                                    MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay,
                                    BakedModel model) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof SlotBlock sb)) return false;

        // Guess Mode owns its own disguise (ItemDisguiseMixin / SlotItemRenderer) — never override it.
        if (GuessDisguise.disguiseItem(stack)) return false;

        int slot = sb.getSlotIndex();

        // ── TEMP DEBUG (mp shaped-icon full-cube, 4th regression) — logs each slot ONCE with every gate
        //    value + the remote-session flag, so an mp test names the exact bail. Remove after diagnosis. ──
        boolean dbg = DEBUG_SEEN.add(slot);

        // Animated slots keep their off-atlas frame icon path (SlotItemRenderer) — don't take those.
        if (AnimFrameCache.get(slot) != null) { if (dbg) dbgLog(slot, mode, "BAIL animated", null, 0, false, false, false, false); return false; }

        String shape = SlotGeometryData.resolveShape(slot, "slot_" + slot);
        if (BlockShapes.isFull(shape) || BlockShapes.isCross(shape)) { if (dbg) dbgLog(slot, mode, "BAIL full/cross", shape, 0, false, false, false, false); return false; }

        // Painted / rotated shaped slots keep their atlas cube icon (the documented §B trade): their per-face
        // art can't be reproduced from the single base texture this path draws with.
        int faceRot = SlotGeometryData.resolveFaceRot(slot);
        if (faceRot != 0) { if (dbg) dbgLog(slot, mode, "BAIL faceRot", shape, faceRot, false, false, false, false); return false; }
        boolean perFace = StaticFrameCache.hasPerFace(slot);
        if (perFace) { if (dbg) dbgLog(slot, mode, "BAIL perFace", shape, faceRot, true, false, false, false); return false; }

        int[][] boxes = SlotShapeMesh.boxesFor(shape, null);
        if (boxes == null) { if (dbg) dbgLog(slot, mode, "BAIL boxes==null", shape, faceRot, false, false, false, false); return false; }

        // Base cube texture (same source SlotItemRenderer's plain path uses): off-atlas world texture if this
        // slot has one, else the plain-cube icon fallback (slot_N.png). No texture → let vanilla draw.
        Identifier tex = StaticFrameCache.get(slot);
        boolean worldTex = tex != null;
        if (tex == null) tex = StaticFrameCache.getIconFallback(slot);
        boolean fallbackTex = !worldTex && tex != null;
        if (tex == null) { if (dbg) dbgLog(slot, mode, "BAIL tex==null", shape, faceRot, false, false, false, false); return false; }
        if (dbg) dbgLog(slot, mode, "DRAW ok", shape, faceRot, false, boxes != null, worldTex, fallbackTex);

        matrices.push();
        // Reproduce vanilla ItemRenderer.renderItem's framing so the shape sits where the real icon would.
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5, -0.5, -0.5);

        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        // A directional shape (stairs) presents its step toward the viewer, like a vanilla stair item — same
        // fixed presentation facing SlotItemRenderer's icon path uses (F3). Non-directional shapes: base frame.
        Direction iconFacing = BlockShapes.isDirectional(shape) ? Direction.EAST : null;
        ShapeIconMesh.drawBoxes(matrices, vc, light, overlay, boxes, iconFacing);

        matrices.pop();
        return true;
    }
}
