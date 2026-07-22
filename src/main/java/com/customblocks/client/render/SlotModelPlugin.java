/**
 * SlotModelPlugin.java — Group 08 §J + §B. CLIENT-ONLY.
 *
 * Registers a Fabric model-loading plugin that wraps every numbered slot's baked BLOCK model in
 * {@link DirectionalSlotModel}, which draws the slot's current shape (§B) and rotates it to the placement
 * (§J) during the chunk rebuild.
 *
 * §B widened this gate. Before, only a slot whose current shape was directional got wrapped, because the
 * pack had already baked the right model for every other shape — but that is exactly what made a shape
 * change need a pack rebuild + reload prompt. Now the pack emits ONE shape-independent (full-cube) model
 * per static slot and the wrap supplies the geometry, so the wrap must be present whatever the shape is,
 * including a slot that is full today and becomes a slab with no reload in between. That also removes the
 * old "shape data not populated at first bake" edge: the wrap no longer decides anything at bake time.
 *
 * Wrapping a full cube costs nothing at render time: {@link DirectionalSlotModel} forwards straight to the
 * wrapped model when the shape is full, which is the overwhelming majority of blocks.
 *
 * Item models are left untouched (the inventory variant is skipped) — an icon must not rotate with a
 * placement, and the pack picks the icon model itself.
 *
 * Depends on: DirectionalSlotModel (the wrapper).
 * Called by:  CustomBlocksClient.onInitializeClient.
 */
package com.customblocks.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class SlotModelPlugin {

    private static final String SLOT_PREFIX = "slot_";

    private SlotModelPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(ctx -> ctx.modifyModelAfterBake().register((model, bakeCtx) -> {
            if (model == null || model instanceof DirectionalSlotModel) return model; // nothing / already wrapped

            ModelIdentifier top = bakeCtx.topLevelId();
            if (top == null) return model;                                             // resource-id model
            if (ModelIdentifier.INVENTORY_VARIANT.equals(top.variant())) return model;  // item icon stays as baked

            Identifier id = top.id();
            if (id == null || !"customblocks".equals(id.getNamespace())) return model;
            String path = id.getPath();
            if (path == null || !path.startsWith(SLOT_PREFIX)) return model;

            int slot;
            try {
                slot = Integer.parseInt(path.substring(SLOT_PREFIX.length()));
            } catch (NumberFormatException e) {
                return model;                                                          // not a numbered slot block
            }

            return new DirectionalSlotModel(model, slot);
        }));
    }
}
