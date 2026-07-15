/**
 * GuessDisguise.java — Group 30 (Guess Mode). v2 command redesign. CLIENT-SIDE ONLY.
 *
 * The one place that decides WHEN a custom block is disguised for the local guess-mode holder, and DRAWS the
 * disguise cube — shared by {@link SlotItemRenderer} (item icon / hand) and {@link AnimSlotBER} (placed
 * off-atlas world block) so both stay tiny and behave identically.
 *
 * Coverage is TOTAL for a flagged block (v2 redesign): the flagged player must not be able to identify it by
 * ANY means, so every render of a flagged block on THEIR client is disguised — hotbar/inventory icon, their
 * own hand, another player's hand, F5 self-view, a placed copy, a dropped item, an item frame. Gating is
 * purely "does the LOCAL player have this slot flagged" ({@link ClientGuessState#localDisguisesSlot(int)}) —
 * entity-independent, so a WATCHER (not flagged) always sees the real block, and no render-mode check is
 * needed. The disguise look is always the bundled "?" mystery cube; the name is blanked separately to "???".
 *
 * World coverage is now TOTAL regardless of atlas status (every SlotBlock carries this BlockEntity; the "?"
 * cube is drawn slightly INFLATED — see drawCube — so its faces sit just outside a plain atlas block's baked
 * faces at z=0/1 and win the depth test, opaquely covering the real block instead of z-fighting behind it). Item coverage (hand/inventory/frame/dropped) is total for full-cube slots (plain static
 * ones now also get a builtin/entity item model — see ServerPackGenerator — with SlotItemRenderer falling
 * back to the real texture via StaticFrameCache.getIconFallback when not flagged); shaped / per-face blocks
 * still keep their own baked atlas item icon (out of scope), though the NAME still blanks for them regardless
 * (that path is model-independent).
 *
 * Depends on: ClientGuessState, SlotBlock, AnimSlotBER (drawCube), the bundled questionmark.png.
 * Called by:  SlotItemRenderer, AnimSlotBER.
 */
package com.customblocks.client.render;

import com.customblocks.CustomBlocksMod;
import com.customblocks.block.SlotBlock;
import com.customblocks.client.ClientGuessState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class GuessDisguise {

    private GuessDisguise() {} // static-only

    /** Bundled "?" QuestionMark texture — the LAST-resort disguise look (v3 Phase 1 fallback). */
    public static final Identifier MYSTERY = Identifier.of(CustomBlocksMod.MOD_ID, "textures/misc/questionmark.png");

    /** Should this stack's item ICON / HAND render be replaced with the disguise cube for the local holder? */
    public static boolean disguiseItem(ItemStack stack) {
        if (!ClientGuessState.localActive()) return false;
        if (stack == null || !(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof SlotBlock sb))
            return false;
        return ClientGuessState.localDisguisesSlot(sb.getSlotIndex());
    }

    /** Should this placed OFF-ATLAS world block (slot index) be drawn as the disguise cube for the local holder? */
    public static boolean disguiseWorld(int slot) {
        return ClientGuessState.localActive() && ClientGuessState.localDisguisesSlot(slot);
    }

    /**
     * Total Blindness (G30 §R): is this placed block one the LOCAL player has flagged, so its non-visual tells
     * (hitbox outline, break/mining particles) must be spoofed too — not just the texture/name? The gate is the
     * same holder-local predicate as the disguise, so watchers are never affected. Used by the §R client mixins.
     */
    public static boolean blinds(BlockState state) {
        if (state == null || !ClientGuessState.localActive()) return false;
        return state.getBlock() instanceof SlotBlock sb && ClientGuessState.localDisguisesSlot(sb.getSlotIndex());
    }

    /**
     * The look slot to draw for this disguised ITEM stack (v3 Phase 1): per-round override → global default →
     * {@link ClientGuessState#NO_LOOK} (draws the bundled "?"). Call only when {@link #disguiseItem} is true.
     */
    public static int lookSlotForStack(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SlotBlock sb)
            return ClientGuessState.localLookSlot(sb.getSlotIndex());
        return ClientGuessState.NO_LOOK;
    }

    /**
     * Draw the disguise cube (six faces) into the already-posed matrices, inflated ~1.2% about the cube centre
     * so its faces sit just OUTSIDE the unit cube instead of AnimSlotBER's baseline z=0.999 (just INSIDE). This
     * matters ONLY for the placed-in-world disguise over a plain ATLAS block: that block's real cube_all faces
     * are baked at z=0/1, so a face at 0.999 is 0.001 BEHIND them and loses the depth test — the disguise cube
     * would never show. Nudging the faces just past the surface makes them win depth and OPAQUELY cover the real
     * block. Harmless for the item icon (no atlas cube underneath — the 1.2% is invisible).
     *
     * v3 Phase 1: {@code lookSlot} chooses WHICH picture. ≥0 → that slot's baked texture (animated current
     * frame, or its static/off-atlas/icon texture); {@link ClientGuessState#NO_LOOK} or an unresolvable slot →
     * the bundled "?" (questionmark.png). The look is resolved every draw, so an animated look plays live.
     */
    public static void drawLook(MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, int lookSlot) {
        Identifier tex = resolveLookTexture(lookSlot);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(1.012f, 1.012f, 1.012f);
        matrices.translate(-0.5, -0.5, -0.5);
        AnimSlotBER.drawCube(matrices, vc, light, overlay, 0f, 1f, 0f, 1f);
        matrices.pop();
    }

    /**
     * Public look→texture resolver (same rules as the disguise): a look slot's baked full-cube picture
     * (animated current frame / static / atlas icon), or the bundled "?" for {@link ClientGuessState#NO_LOOK}
     * or an unresolvable slot. Reused by the Showcase renderer (G30-8b) so its picture matches the disguise.
     */
    public static Identifier textureForLook(int lookSlot) {
        return resolveLookTexture(lookSlot);
    }

    /** Resolve a look slot to a full-cube texture the same way AnimSlotBER/SlotItemRenderer do; "?" on miss. */
    private static Identifier resolveLookTexture(int lookSlot) {
        if (lookSlot >= 0) {
            AnimFrameCache.Slot s = AnimFrameCache.get(lookSlot);   // animated look → current frame's cell
            if (s != null) return s.prepare(AnimClock.nowMs());
            Identifier st = StaticFrameCache.get(lookSlot);         // static off-atlas look
            if (st != null) return st;
            Identifier icon = StaticFrameCache.getIconFallback(lookSlot); // plain atlas full-cube look (slot_N.png)
            if (icon != null) return icon;
        }
        return MYSTERY;                                            // unset / shaped / unresolvable → bundled "?"
    }
}
